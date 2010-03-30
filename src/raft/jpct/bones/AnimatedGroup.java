package raft.jpct.bones;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import com.threed.jpct.Object3D;
import com.threed.jpct.World;

/** 
 * Helper class to group {@link Animated3D}'s and manipulate them easily.
 *  
 * @author hakan eryargi (r a f t)
 */
public class AnimatedGroup implements java.io.Serializable, Iterable<Animated3D>, Cloneable {
	private static final long serialVersionUID = 1L;

	/** Re-use (share) the mesh. */
	public static final boolean MESH_REUSE = true;
	/** Use a separate mesh. */
	public static final boolean MESH_DONT_REUSE = false;
	
	private final Object3D root = Object3D.createDummyObj();
	private Animated3D[] objects;
	
	private SkinClipSequence skinClipSequence;
	private PoseClipSequence poseClipSequence;

	private boolean autoApplyAnimation = true;
	
	/**
	 * Creates a new AnimatedGroup out of given objects. All objects must have the same {@link Skeleton}.
	 * 
	 *  @throws IllegalArgumentException if object array is empty or they have different skeletons.
	 *  @see Animated3D#mergeSkin(Animated3D...)
	 * */
	public AnimatedGroup(Animated3D... objects) {
		if (objects.length == 0)
			throw new IllegalArgumentException("No objects");
		
		this.objects = new Animated3D[objects.length];
		System.arraycopy(objects, 0, this.objects, 0, objects.length);
		
		checkSameSkeleton();
		
		for (Animated3D so : objects) {
			root.addChild(so);
		}
	}
	
	AnimatedGroup(Animated3D[] objects, SkinClipSequence clipSequence, PoseClipSequence poseClipSequence) {
		this(objects);
		setSkinClipSequence(clipSequence);
		setPoseClipSequence(poseClipSequence);
	}

	AnimatedGroup(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		int count = in.readInt();
		this.objects = new Animated3D[count];
		for (int i = 0; i < count; i++) {
			objects[i] = new Animated3D(in);
			root.addChild(objects[i]);
		}
		this.skinClipSequence = (SkinClipSequence) in.readObject();
		this.poseClipSequence = (PoseClipSequence) in.readObject();
	}
	

	/** Returns root object that all animated objects are added as child. Root object is a dummy object, 
	 * it isn't added to world. To translate/rotate the whole group, it's enough to apply translation/rotation to root object. */
	public Object3D getRoot() {
		return root;
	}
	
	/** Returns the specified object */
	public Animated3D get(int index) {
		return objects[index];
	}
	
	/** Adds all animated objects to world. 
	 * @see World#addObject(Object3D) */
	public void addToWorld(World world) {
		for (Animated3D so : objects) {
			world.addObject(so);
		}
	}
	
	/** <p>Returns the assigned ClipSequence if any.</p> */
	public SkinClipSequence getSkinClipSequence() {
		return skinClipSequence;
	}

	/** 
	 * <p>Sets the {@link SkinClipSequence} of this group. The skeleton of ClipSequence must be the same of
	 * this group.</p>
	 * 
	 * <p>This method is analogue of {@link Object3D#setAnimationSequence(com.threed.jpct.Animation)}.</p>
	 * 
	 * @see #mergeSkin(AnimatedGroup...)
	 * @throws IllegalArgumentException if given ClipSequence has a different {@link Skeleton} 
	 * */
	public void setSkinClipSequence(SkinClipSequence clipSequence) {
		if ((clipSequence != null) && (clipSequence.getSkeleton() != objects[0].skeleton)) 
			throw new IllegalArgumentException("SkinClipSequence's skeleton is different from this group's skeleton");
		 
		this.skinClipSequence = clipSequence;
		
		for (Animated3D so : objects)
			so.setSkinClipSequence(clipSequence);
	}
	
	/** <p>Returns the assigned ClipSequence if any.</p> */
	public PoseClipSequence getPoseClipSequence() {
		return poseClipSequence;
	}

	/** 
	 * <p>Sets the {@link SkinClipSequence} of this group. The skeleton of ClipSequence must be the same of
	 * this group.</p>
	 * 
	 * <p>This method is analogue of {@link Object3D#setAnimationSequence(com.threed.jpct.Animation)}.</p>
	 * 
	 * @see #mergeSkin(AnimatedGroup...)
	 * @throws IllegalArgumentException if given ClipSequence has a different {@link Skeleton} 
	 * */
	public void setPoseClipSequence(PoseClipSequence poseClipSequence) {
		this.poseClipSequence = poseClipSequence;
		
		for (Animated3D so : objects)
			so.setPoseClipSequence(poseClipSequence);
	}
	
	/** 
	 * <p>Animates this object group using assigned {@link SkinClipSequence}. 
	 * Updates curentPose once and if "auto apply animation" is enabled calls 
	 * {@link Animated3D#applySkeletonPose()} on each of objects.</p>
	 * 
	 * <p>This method behaves similar to {@link Object3D#animate(float, int)}.</p> 
	 * 
	 * @see SkinClipSequence
	 * @see Animated3D#animateSkin(float, int)
	 * @see #setAutoApplyAnimation(boolean)
	 * @see Object3D#animate(float, int)
	 * */
	public void animateSkin(float index, int sequence) {
		if (skinClipSequence == null)
			return;
		
		SkeletonPose currentPose = objects[0].getSkeletonPose();
		if (sequence == 0) {
			skinClipSequence.animate(index * skinClipSequence.getTime(), currentPose);
		} else {
			SkinClip clip = skinClipSequence.getClip(sequence - 1); 
			clip.applyTo(index * clip.getTime(), currentPose);
		}
		currentPose.updateTransforms();
		
		for (Animated3D so : objects) 
			so.applySkeletonPose();
		
		if (autoApplyAnimation)
			applyAnimation();
	}
	
	/** 
	 * <p>calls {@link Animated3D#applySkeletonPose()} on each of objects.</p>
	 * 
	 * @see Animated3D#applySkeletonPose()
	 * */
	public void applySkeletonPose() {
		for (Animated3D so : objects) {
			so.applySkeletonPose();
		}
	}
	
	/** Returns if animateXX methods are automatically applied.
	 *  
	 * @see #animatePose(float, int)
	 * @see #animateSkin(float, int) */
	public boolean isAutoApplyAnimation() {
		return autoApplyAnimation;
	}

	/** 
	 * <p>Sets if animateXX methods are automatically applied. Also calls {@link Animated3D#setAutoApplyAnimation setAutoApplyAnimation}
	 * on each of objects. Default is true.</p>
	 * 
	 * <p>To enable animation blending automatic applying must be disabled.</p>
	 *  
	 * @see #animatePose(float, int)
	 * @see #animateSkin(float, int) */
	public void setAutoApplyAnimation(boolean autoApplyAnimation) {
		this.autoApplyAnimation = autoApplyAnimation;
		for (Animated3D so : objects) {
			so.setAutoApplyAnimation(autoApplyAnimation);
		}
	}

	/** 
	 * <p>Calls {@link Animated3D#applyAnimation() applyAnimation()} on each of objects.</p>
	 * 
	 * @see Animated3D#applyAnimation()
	 * */
	public void applyAnimation() {
		for (Animated3D so : objects) {
			so.applyAnimation();
		}
	}
	
	/** 
	 * <p>calls {@link Animated3D#resetAnimation() resetAnimation()} on each of objects.</p>
	 * 
	 * @see Animated3D#resetAnimation()
	 * */
	public void resetAnimation() {
		for (Animated3D o : objects) {
			o.resetAnimation();
		}
	}
	
	/** Same as {@link #animatePose(float, int, float) animatePose(float, int, 1)} */
	public void animatePose(float index, int sequence) {
		animatePose(index, sequence, 1f);
	}
	
	/** <p>Animates this group using assigned {@link PoseClipSequence}. 
	 * Updates curentPose and if "auto apply animation" is enabled calls {@link #applySkeletonPose()} </p>
	 * 
	 * <p>Pose animations are cumulative if "auto apply animation" is disabled. 
	 * Each call to this method cancels previous skin animation.</p>
	 * 
	 * @param sequence the number of {@link PoseClip} in {@link PoseClipSequence}. 1 is the first sequence. 
	 * 			0 means whole {@link PoseClipSequence}
	 * @param index time index   
	 * @param weight how much animation will be applied. 1 means as it is
	 * 
	 * @see SkinClipSequence
	 * @see Object3D#animate(float, int)
	 * @see #setAutoApplyAnimation(boolean)
	 * */
	public void animatePose(float index, int sequence, float weight) {
		if (poseClipSequence == null)
			return;
		
		for (Animated3D o : objects) {
			o.animatePoseDontApply(index, sequence, weight);
		}
		if (autoApplyAnimation)
			applyAnimation();
	}
	
	
	/** Returns an iterator of {@link Animated3D} objects. */
	public Iterator<Animated3D> iterator() {
		return Arrays.asList(objects).iterator();
	}
	
	void writeToStream(java.io.ObjectOutputStream out) throws IOException {
		out.writeInt(objects.length);
		
		for (Animated3D so : objects) {
			so.writeToStream(out);
		}
		out.writeObject(skinClipSequence);
		out.writeObject(poseClipSequence);
	}
	
	/** returns number of objects in this group */
	public int getSize() {
		return objects.length;
	}
	
	/** throws an exception if all objects does not have same skeleton. */
	private void checkSameSkeleton() {
		Skeleton lastSkeleton = null;
		
		for (Animated3D o : objects) {
			if (lastSkeleton == null)
				lastSkeleton = o.getSkeleton();
			
			if (o.getSkeleton() != lastSkeleton)
				throw new IllegalArgumentException("all objects should have same Skeleton");
		}
	}
	
	
	/** 
	 * <p>Merge many <code>AnimatedGroup</code>s into one. This method
	 * does not require all AnimatedGroups share the same {@link Skeleton}
	 * but skeletons are <i>almost</i> identical.</p> 
	 * 
	 * <p>If many AnimatedGroups are loaded from different files, their
	 * skeleton objects will be different even if they are identical.
	 * This method is meant to help such cases.</p>
	 * 
	 * <p>This method always uses the skeleton of first group.</p>
	 * 
	 *  @see Animated3D#mergeSkin(Animated3D...)
	 *  @see SkinClipSequence#merge(SkinClipSequence...)
	 * 
	 * */
	public static AnimatedGroup mergeSkin(AnimatedGroup... groups) {
		if (groups.length == 0)
			throw new IllegalArgumentException("no groups");
		
		int numObjects = groups[0].objects.length;
		for (AnimatedGroup group : groups) {
			if (numObjects != group.objects.length)
				throw new IllegalArgumentException("number of objects differ in groups.");
		}
		
		Animated3D[] merged = new Animated3D[numObjects];
		Animated3D[] objects = new Animated3D[groups.length];
		
		for (int i = 0; i < numObjects; i++) {
			for (int j = 0; j < groups.length; j++) {
				objects[j] = groups[j].get(i);
			}
			merged[i] = Animated3D.mergeSkin(objects);
		}
		return new AnimatedGroup(merged, merged[0].getSkinClipSequence(), merged[0].getPoseClipSequence());
	}
	
	/** 
	 * Same as clone(MESH_REUSE) 
	 * @see AnimatedGroup#clone(boolean) 
	 * */
	@Override
	public AnimatedGroup clone() {
		return clone(MESH_REUSE);
	}

	/** 
	 * <p>Clones this object group. {@link Skeleton}, {@link SkeletonPose}, {@link SkinClipSequence} 
	 * {@link PoseClipSequence} will be shared. 
	 * If the mesh is reused, the clone and master will inherit animations from each other.</p>
	 * 
	 * @see Animated3D#Animated3D(Animated3D, boolean)
	 * @see #MESH_REUSE 
	 * @see #MESH_DONT_REUSE 
	 * */
	public AnimatedGroup clone(boolean reuseMesh) {
		Animated3D[] clones = new Animated3D[objects.length];
		for (int i = 0; i < clones.length; i++) {
			clones[i] = new Animated3D(objects[i], reuseMesh);
		}
		return new AnimatedGroup(clones, skinClipSequence, poseClipSequence);
	}
}