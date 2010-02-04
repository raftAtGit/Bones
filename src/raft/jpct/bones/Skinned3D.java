/**
 * Some of the classes in this file are adapted or inspired from Ardor3D. 
 * Such classes are indicated in class javadocs. 
 * 
 * Modification and redistribution of them may be subject to Ardor3D's license: 
 * http://www.ardor3d.com/LICENSE
 */
package raft.jpct.bones;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.threed.jpct.GenericVertexController;
import com.threed.jpct.IVertexController;
import com.threed.jpct.Mesh;
import com.threed.jpct.Object3D;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.TextureManager;

/** 
 * <p>An {@link Object3D} which can be animated via skeleton animation.</p> 
 * 
 * <p>Once constructed, Skinned3D sets a {@link IVertexController} on its mesh which
 * deforms the {@link Mesh} to perform animation. So if you set another IVertexController
 * on its mesh, you will break skeletal animation.</p>
 * 
 * <p>This class is adapted from <a href="http://www.ardor3d.com">Ardor3D.</a></p>
 * 
 * @see SkinIO#loadSkinnedObjects(java.io.InputStream)
 * */
public class Skinned3D extends Object3D implements Cloneable {
	private static final long serialVersionUID = 1L;

	public static final boolean MESH_REUSE = true;
	public static final boolean MESH_DONT_REUSE = false;
	
	final Skeleton skeleton;
	final Skeleton.Skin skin;
	private Skeleton.Pose currentPose;
	private Skeleton.Mesh meshData;
	
	private ClipSequence clipSequence;
	
	private final SkinVertexController skinVertexController = new SkinVertexController();
	
	// TODO maybe add a method createAnimationSequence() to create jPCT mesh animation sequence

	/** 
	 * <p>Same as {@link #Skinned3D(Skinned3D, boolean)  Object3D(Object3D, true)}</p>
	 */
	public Skinned3D(Skinned3D object) {
		this(object, MESH_REUSE);
	}
	
	/**
	 * <p>Behaves same as {@link Object3D#Object3D(Object3D, boolean) Object3D(Object3D, reuseMesh)}. 
	 * In addition copies skin information.</p> 
	 */
	public Skinned3D(Skinned3D object, boolean reuseMesh) {
		super(object, reuseMesh);
		this.skeleton = object.skeleton;
		this.skin = object.skin;
		this.currentPose = object.currentPose;
		this.meshData = object.meshData;
		this.clipSequence = object.clipSequence;

		attachVertexController();
	}

	/** Creates a Skinned3D out of given information. */
	Skinned3D(Skeleton.Mesh meshData, Skeleton.Skin skin, Skeleton.Pose currentPose) {
		super(meshData.coordinates, meshData.uvs, meshData.indices, TextureManager.TEXTURE_NOTFOUND);
		
		this.skeleton = currentPose.skeleton;
		this.currentPose  = currentPose;
		this.skin = skin; 
		this.meshData = meshData;

		attachVertexController();
	}

	/** 
	 * Creates a Skinned3D by re-loading information previously saved to stream. 
	 * @see #writeToStream(java.io.ObjectOutputStream) */
	Skinned3D(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		this((Skeleton.Mesh) in.readObject(), (Skeleton.Skin) in.readObject(), (Skeleton.Pose) in.readObject());
		this.clipSequence = (ClipSequence) in.readObject();
	}
	
	/** Returns the skeleton pose used for animation. */
	public Skeleton.Pose getCurrentPose() {
		return currentPose;
	}

	/** Sets skeleton pose used for animation. */
	public void setCurrentPose(Skeleton.Pose currentPose) {
		if (currentPose.skeleton != this.skeleton)
			throw new IllegalArgumentException("pose belongs to another skeleton");
		this.currentPose = currentPose;
	}
	
	/** Returns the Skeleton this object is bound to. */
	public Skeleton getSkeleton() {
		return skeleton;
	}

	/** Applies current pose by applying vertex controller */
	public void applyPose() {
		getMesh().applyVertexController();
	}

	/** If this object is called via Ardor3D's loader, calling this method saves some memory. */
	public void discardSkeletonMesh() {
		meshData = null;
	}
	
	/** 
	 * <p>Animates this object using assigned {@link ClipSequence}.
	 * Updates curentPose and calls {@link #applyPose()}</p>
	 * 
	 * <p>Note, if many skinned objects share the same pose,
	 * updating current pose for each of them is a waste. Consider
	 * using {@link SkinnedGroup#animateSkin(float)} 
	 * </p>
	 * 
	 * <p>This method behaves similar to {@link Object3D#animate(float)}.</p>
	 * 
	 * @see ClipSequence
	 * @see Object3D#animate(float)
	 * @throws NullPointerException if clipSequence is null
	 * */
	public void animateSkin(float index) {
		clipSequence.animate(index * clipSequence.getTime(), currentPose);
		currentPose.updateTransforms();
		applyPose();
	}
	
	/** <p>Animates this object using assigned {@link ClipSequence}. 
	 * Updates curentPose and calls {@link #applyPose()}</p>
	 * 
	 * <p>Note, if many skinned objects share the same pose,
	 * updating current pose for each of them is a waste. Consider
	 * using {@link SkinnedGroup#animateSkin(float, int)} 
	 * </p>
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
			Clip clip = clipSequence.getClip(sequence); 
			clip.applyTo(index * clip.getTime(), currentPose);
			currentPose.updateTransforms();
			applyPose();
		}
	}
	
	/** <p>Returns the assigned ClipSequence if any.</p> */
	public ClipSequence getClipSequence() {
		return clipSequence;
	}

	/** 
	 * <p>Sets the {@link ClipSequence} of this object. The skeleton of ClipSequence must be the same of
	 * this object.</p>
	 * 
	 * <p>This method is analogue of {@link Object3D#setAnimationSequence(com.threed.jpct.Animation)}.</p>
	 * 
	 * @see #mergeSkin(Skinned3D...)
	 * @throws IllegalArgumentException if given ClipSequence has a different {@link Skeleton} 
	 * */
	public void setClipSequence(ClipSequence clipSequence) {
		if ((clipSequence != null) && (clipSequence.getSkeleton() != this.skeleton)) 
			throw new IllegalArgumentException("ClipSequence's skeleton is different from this object's skeleton");
		 
		this.clipSequence = clipSequence;
	}

	/**
	 * <p>Clones this object. Behaves same as {@link Object3D#cloneObject()} and copies
	 * skinning information in addition.</p>
	 * 
	 *  @see Object3D#cloneObject()
	 * */
	@Override
	public Skinned3D cloneObject() {
		return new Skinned3D(this, MESH_REUSE); 
	}
	
	/** 
	 * <p>Writes mesh and skinning information to stream. This method is different
	 * from serializing whole object.</p>
	 * */
	void writeToStream(java.io.ObjectOutputStream out) throws IOException {
		if (meshData == null)
			throw new IllegalStateException("this object does not contain skeleton mesh data. did you called discardSkeletonMesh() ?");
		
		out.writeObject(meshData);
		out.writeObject(skin);
		out.writeObject(currentPose);
		out.writeObject(clipSequence);
	}
	
	/** 
	 * <p>Attaches the {@link IVertexController}. First calls {@link #calcNormals()} to avoid
	 * warning setting vertex controller on an object with no normals.</p> 
	 * */
	private void attachVertexController() {
		// we call calcNormals here to avoid warning setting vertex controller on an object with no normals 
		calcNormals();
		getMesh().setVertexController(skinVertexController, IVertexController.PRESERVE_SOURCE_MESH);
	}
	
	/** 
	 * <p>Merge many <code>Skinned3D</code>s into one. This method
	 * does not require all Skinned3Ds share the same {@link Skeleton}
	 * but skeletons are <i>almost</i> identical.</p> 
	 * 
	 * <p>If many Skinned3Ds are loaded from different files, their
	 * skeleton objects will be different even if they are identical.
	 * This method is meant to help such cases.</p>
	 * 
	 * <p>This method always uses the skeleton of first object.</p>
	 * 
	 *  @see SkinnedGroup#mergeSkin(raft.jpct.bones.SkinnedGroup...)
	 *  @see ClipSequence#merge(ClipSequence...)
	 * 
	 * */
	public static Skinned3D mergeSkin(Skinned3D... objects) {
		if (objects.length == 0) 
			throw new IllegalArgumentException("No objects!");
		
		// check skeletons match
		for (int i = 0; i < objects.length; i++) {
			for (int j = i + 1; j < objects.length; j++) {
				objects[i].skeleton.checkAlmostEqual(objects[j].skeleton);
				objects[i].skin.checkAlmostEqual(objects[j].skin);
			}
		}
		Skinned3D merged = new Skinned3D(objects[0], MESH_DONT_REUSE);
		
		List<ClipSequence> sequences = new LinkedList<ClipSequence>();
		for (Skinned3D object : objects) {
			if (object.clipSequence != null)
				sequences.add(object.clipSequence);
		}
		if (!sequences.isEmpty()) {
			ClipSequence mergedSequence = ClipSequence.merge(sequences.toArray(new ClipSequence[sequences.size()]));
			merged.setClipSequence(mergedSequence);
		}
		return merged;
	} 
	
	
	
	/** 
	 * <p>The {@link IVertexController} which deforms the mesh according to current pose.</p> 
	 * 
	 * <p>This class is adapted from <a href="http://www.ardor3d.com">Ardor3D.</a></p>
	 * */
	private class SkinVertexController extends GenericVertexController {
		private static final long serialVersionUID = 1L;
		
        private SimpleVector vertexSum = new SimpleVector();
        private SimpleVector temp = new SimpleVector();

		public void apply() {
            
            // Get a handle to the source and dest vertices buffers
            SimpleVector[] bindVerts = getSourceMesh();
            SimpleVector[] storeVerts = getDestinationMesh();
            
            // Cycle through each vertex
            for (int i = 0; i < bindVerts.length; i++) {
                // zero out our sum var
                vertexSum.set(0f, 0f, 0f);

                // pull in joint data
                final float[] weights = skin.weights[i];
                final short[] jointIndices = skin.jointIndices[i];

                // for each joint where the weight != 0
                for (int j = 0; j < Skeleton.MAX_JOINTS_PER_VERTEX; j++) {
                    if (weights[j] == 0) {
                        continue;
                    }

                    final int jointIndex = jointIndices[j];
                    temp.set(bindVerts[i]);

                    // Multiply our vertex by the matrix pallete entry
                    temp.matMul(currentPose.palette[jointIndex]);
                    
                    // Sum, weighted.
                    temp.scalarMul(weights[j]);
                    vertexSum.add(temp);
                }

                // Store sum into _meshData
                storeVerts[i].set(vertexSum);
                
            } // for vertices
		
		}
	}
	
}
