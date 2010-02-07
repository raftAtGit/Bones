package raft.jpct.bones;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import com.threed.jpct.Object3D;
import com.threed.jpct.World;

/** Helper class to group {@link Skinned3D}'s and manipulate them easily. */
public class SkinnedGroup implements java.io.Serializable, Iterable<Skinned3D>, Cloneable {
	private static final long serialVersionUID = 1L;

	/** Re-use (share) the mesh. */
	public static final boolean MESH_REUSE = true;
	/** Use a separate mesh. */
	public static final boolean MESH_DONT_REUSE = false;
	
	private final Object3D root = Object3D.createDummyObj();
	private Skinned3D[] objects;
	
	private ClipSequence clipSequence;

	/**
	 * Creates a new SkinnedGroup out of given objects. All objects must have the same {@link Skeleton}.
	 * 
	 *  @throws IllegalArgumentException if object array is empty or they have different skeletons.
	 *  @see Skinned3D#mergeSkin(Skinned3D...)
	 * */
	public SkinnedGroup(Skinned3D[] objects) {
		if (objects.length == 0)
			throw new IllegalArgumentException("No objects");
		
		this.objects = new Skinned3D[objects.length];
		System.arraycopy(objects, 0, this.objects, 0, objects.length);
		
		checkSameSkeleton();
		
		for (Skinned3D so : objects) {
			root.addChild(so);
		}
	}
	
	SkinnedGroup(Skinned3D[] objects, ClipSequence clipSequence) {
		this(objects);
		setClipSequence(clipSequence);
	}

	SkinnedGroup(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		int count = in.readInt();
		this.objects = new Skinned3D[count];
		for (int i = 0; i < count; i++) {
			objects[i] = new Skinned3D(in);
			root.addChild(objects[i]);
		}
		this.clipSequence = (ClipSequence) in.readObject();
	}
	

	/** returns root object that all skinned objects are added as child */
	public Object3D getRoot() {
		return root;
	}
	
	/** Returns the specified object */
	public Skinned3D get(int index) {
		return objects[index];
	}
	
	/** Adds all skinned objects to world. 
	 * @see World#addObject(Object3D) */
	public void addToWorld(World world) {
		for (Skinned3D so : objects) {
			world.addObject(so);
		}
	}
	
	/** <p>Returns the assigned ClipSequence if any.</p> */
	public ClipSequence getClipSequence() {
		return clipSequence;
	}

	/** 
	 * <p>Sets the {@link ClipSequence} of this group. The skeleton of ClipSequence must be the same of
	 * this group.</p>
	 * 
	 * <p>This method is analogue of {@link Object3D#setAnimationSequence(com.threed.jpct.Animation)}.</p>
	 * 
	 * @see #mergeSkin(SkinnedGroup...)
	 * @throws IllegalArgumentException if given ClipSequence has a different {@link Skeleton} 
	 * */
	public void setClipSequence(ClipSequence clipSequence) {
		if ((clipSequence != null) && (clipSequence.getSkeleton() != objects[0].skeleton)) 
			throw new IllegalArgumentException("ClipSequence's skeleton is different from this group's skeleton");
		 
		this.clipSequence = clipSequence;
		
		for (Skinned3D so : objects)
			so.setClipSequence(clipSequence);
	}
	
	/** 
	 * <p>Animates this object group using assigned {@link ClipSequence}. 
	 * Updates curentPose once and calls {@link Skinned3D#applyPose()} on each of objects.</p>
	 * 
	 * <p>This method behaves similar to {@link Object3D#animate(float)}.</p> 
	 * 
	 * @see ClipSequence
	 * @see Object3D#animate(float, int)
	 * @throws NullPointerException if clipSequence is null
	 * */
	public void animateSkin(float index) {
		clipSequence.animate(index * clipSequence.getTime(), objects[0].getCurrentPose());
		objects[0].getCurrentPose().updateTransforms();
		applyPose();
	}
	
	/** 
	 * <p>Animates this object group using assigned {@link ClipSequence}. 
	 * Updates curentPose once and calls {@link Skinned3D#applyPose()} on each of objects.</p>
	 * 
	 * <p>This method behaves similar to {@link Object3D#animate(float, int)}.</p> 
	 * 
	 * @see ClipSequence
	 * @see Object3D#animate(float, int)
	 * @throws NullPointerException if clipSequence is null
	 * */
	public void animateSkin(float index, int sequence) {
		if (sequence == 0) {
			animateSkin(index);
		} else {
			Clip clip = clipSequence.getClip(sequence - 1); 
			clip.applyTo(index * clip.getTime(), objects[0].getCurrentPose());
			objects[0].getCurrentPose().updateTransforms();
			applyPose();
		}
	}
	
	/** 
	 * <p>calls {@link Skinned3D#applyPose()} on each of objects.</p>
	 * 
	 * @see Skinned3D#applyPose()
	 * */
	public void applyPose() {
		for (Skinned3D so : objects) {
			so.applyPose();
		}
	}
	
	/** Returns an iterator of skinned objects. */
	public Iterator<Skinned3D> iterator() {
		return Arrays.asList(objects).iterator();
	}
	
	void writeToStream(java.io.ObjectOutputStream out) throws IOException {
		out.writeInt(objects.length);
		
		for (Skinned3D so : objects) {
			so.writeToStream(out);
		}
		out.writeObject(clipSequence);
	}
	
	/** returns number of objects in this group */
	public int getSize() {
		return objects.length;
	}
	
	/** throws an exception if all clips does not have same skeleton. */
	private void checkSameSkeleton() {
		Skeleton lastSkeleton = null;
		
		for (Skinned3D o : objects) {
			if (lastSkeleton == null)
				lastSkeleton = o.getSkeleton();
			
			if (o.getSkeleton() != lastSkeleton)
				throw new IllegalArgumentException("all objects should have same Skeleton");
		}
	}
	
	
	/** 
	 * <p>Merge many <code>SkinnedGroup</code>s into one. This method
	 * does not require all SkinnedGroups share the same {@link Skeleton}
	 * but skeletons are <i>almost</i> identical.</p> 
	 * 
	 * <p>If many SkinnedGroups are loaded from different files, their
	 * skeleton objects will be different even if they are identical.
	 * This method is meant to help such cases.</p>
	 * 
	 * <p>This method always uses the skeleton of first group.</p>
	 * 
	 *  @see Skinned3D#mergeSkin(Skinned3D...)
	 *  @see ClipSequence#merge(ClipSequence...)
	 * 
	 * */
	public static SkinnedGroup mergeSkin(SkinnedGroup... groups) {
		if (groups.length == 0)
			throw new IllegalArgumentException("no groups");
		
		int numObjects = groups[0].objects.length;
		for (SkinnedGroup group : groups) {
			if (numObjects != group.objects.length)
				throw new IllegalArgumentException("number of objects differ in groups.");
		}
		
		Skinned3D[] merged = new Skinned3D[numObjects];
		Skinned3D[] objects = new Skinned3D[groups.length];
		
		for (int i = 0; i < numObjects; i++) {
			for (int j = 0; j < groups.length; j++) {
				objects[j] = groups[j].get(i);
			}
			merged[i] = Skinned3D.mergeSkin(objects);
		}
		return new SkinnedGroup(merged, merged[0].getClipSequence());
	}
	
	/** 
	 * Same as clone(MESH_REUSE) 
	 * @see SkinnedGroup#clone(boolean) 
	 * */
	@Override
	public SkinnedGroup clone() {
		return clone(MESH_REUSE);
	}

	/** 
	 * <p>Clones this object group. {@link Skeleton}, {@link Skeleton.Pose} and {@link ClipSequence} will be shared. 
	 * If they mesh is reused, the clone and master will inherit animations from each other.</p>
	 * 
	 * @see Skinned3D#Skinned3D(Skinned3D, boolean)
	 * @see #MESH_REUSE 
	 * @see #MESH_DONT_REUSE 
	 * */
	public SkinnedGroup clone(boolean reuseMesh) {
		Skinned3D[] clones = new Skinned3D[objects.length];
		for (int i = 0; i < clones.length; i++) {
			clones[i] = new Skinned3D(objects[i], reuseMesh);
		}
		return new SkinnedGroup(clones, clipSequence);
	}
}