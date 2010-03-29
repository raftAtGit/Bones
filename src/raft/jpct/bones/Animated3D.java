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
 * <p>An {@link Object3D} which can be animated via skeletal or pose animation.</p> 
 * 
 * <p>Once constructed, Animated3D sets an {@link IVertexController} on its mesh which
 * modifies the {@link Mesh} to perform animation. So if you set another IVertexController
 * on its mesh, you will break animation.</p>
 * 
 * <p>Skeletal animation part of this class is adapted from <a href="http://www.ardor3d.com">Ardor3D.</a></p>
 * 
 * @see BonesIO#loadObject(java.io.InputStream)
 * 
 * @author hakan eryargi (r a f t)
 * */
public class Animated3D extends Object3D implements Cloneable {
	private static final long serialVersionUID = 1L;

	/** Re-use (share) the mesh. */
	public static final boolean MESH_REUSE = true;
	/** Use a separate mesh. */
	public static final boolean MESH_DONT_REUSE = false;
	
	final Skeleton skeleton;
	final SkinData skin;
	private SkeletonPose currentPose;
	private MeshData meshData;
	
	private SkinClipSequence skinClipSequence;
	private PoseClipSequence poseClipSequence;
	
	private final VertexController vertexController = new VertexController();
	
	private int index;
	private boolean destMeshDirty = false;
	private transient SimpleVector[] sourceMesh;
	private transient SimpleVector[] destMesh;
	
    private SimpleVector vertexSum = new SimpleVector();
    private SimpleVector temp = new SimpleVector();
	
	private boolean autoApplyAnimation = true;
	
	// TODO maybe add a method createAnimationSequence() to create jPCT mesh animation sequence

	/** 
	 * <p>Same as {@link #Animated3D(Animated3D, boolean)  Object3D(Object3D, true)}</p>
	 */
	public Animated3D(Animated3D object) {
		this(object, MESH_REUSE);
	}
	
	/**
	 * <p>Behaves same as {@link Object3D#Object3D(Object3D, boolean) Object3D(Object3D, reuseMesh)}. 
	 * In addition copies animation data in addition.</p>
	 *  
	 * @see #MESH_REUSE 
	 * @see #MESH_DONT_REUSE 
	 */
	public Animated3D(Animated3D object, boolean reuseMesh) {
		super(object, reuseMesh);
		this.skeleton = object.skeleton;
		this.skin = object.skin;
		this.currentPose = object.currentPose;
		this.meshData = object.meshData;
		this.skinClipSequence = object.skinClipSequence;
		this.poseClipSequence = object.poseClipSequence;
		this.index = object.index;

		attachVertexController();
	}

	/** Creates a Animated3D out of given information. */
	public Animated3D(MeshData meshData, SkinData skin, SkeletonPose currentPose) {
		super(meshData.coordinates, meshData.uvs, meshData.indices, TextureManager.TEXTURE_NOTFOUND);
		
		this.skeleton = (currentPose == null) ? null : currentPose.skeleton;
		this.currentPose  = currentPose;
		this.skin = skin; 
		this.meshData = meshData;

		attachVertexController();
	}

	/** Creates a Animated3D out of given information. */
	public Animated3D(Object3D object, SkinData skin, SkeletonPose currentPose) {
		super(object, MESH_DONT_REUSE);
		
		this.skeleton = (currentPose == null) ? null : currentPose.skeleton;
		this.currentPose  = currentPose;
		this.skin = skin; 

		attachVertexController();
	}
	
	/** 
	 * Creates a Animated3D by re-loading information previously saved to stream. 
	 * @see #writeToStream(java.io.ObjectOutputStream) */
	Animated3D(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		this((MeshData) in.readObject(), (SkinData) in.readObject(), (SkeletonPose) in.readObject());
		this.index = in.readInt();
		this.skinClipSequence = (SkinClipSequence) in.readObject();
		this.poseClipSequence = (PoseClipSequence) in.readObject();
	}
	
	/** Returns the skeleton pose used for animation. */
	public SkeletonPose getSkeletonPose() {
		return currentPose;
	}

	/** Sets skeleton pose used for animation. */
	public void setSkeletonPose(SkeletonPose currentPose) {
		if (currentPose.skeleton != this.skeleton)
			throw new IllegalArgumentException("pose belongs to another skeleton");
		this.currentPose = currentPose;
	}
	
	/** Returns the Skeleton this object is bound to. */
	public Skeleton getSkeleton() {
		return skeleton;
	}

	/** Applies current pose by applying vertex controller */
	public void applySkeletonPose() {
		if (skeleton == null)
			throw new IllegalStateException("This object has no skeleton");
		
		applySkinAnimation();
		applyAnimation();
	}
	
	/** If this object is called via Ardor3D's loader, calling this method saves some memory. */
	public void discardMeshData() {
		meshData = null;
	}
	
	/** Returns if animateXX methods are automatically applied.
	 *  
	 * @see #animatePose(float, int)
	 * @see #animateSkin(float, int) */
	public boolean isAutoApplyAnimation() {
		return autoApplyAnimation;
	}

	/** <p>Sets if animateXX methods are automatically applied. Default is true.</p>
	 *  
	 * <p>To enable animation blending automatic applying must be disabled.</p>
	 *  
	 * @see #animatePose(float, int)
	 * @see #animateSkin(float, int) */
	public void setAutoApplyAnimation(boolean autoApplyAnimation) {
		this.autoApplyAnimation = autoApplyAnimation;
	}

	/** Clears all animation state to initial position of Mesh.  */
	public void resetAnimation() {
        for (int i = 0; i < sourceMesh.length; i++) {
        	destMesh[i].set(sourceMesh[i]);
        }
        destMeshDirty = false;
	}
	
	// TODO hold a flag if an animation if actually done. then apply animation if flag is set
	// this will increase performance for groups with pose animations 
	/** Applies animation to mesh. */
	public void applyAnimation() {
		vertexController.updateMesh();
		touch();
		destMeshDirty = true;
	}
	
	/** <p>Animates this object using assigned {@link SkinClipSequence}. 
	 * Updates current SkeletonPose and if "auto apply animation" is enabled calls {@link #applySkeletonPose()} </p>
	 * 
	 * <p>Skin animations are not cumulative. Each call to this method cancels previous skin animation.</p>
	 * 
	 * <p>Note, if many animated objects share the same pose,
	 * updating current pose for each of them is a waste. Consider
	 * using {@link AnimatedGroup#animateSkin(float, int)} 
	 * </p>
	 * 
	 * <p>This method behaves similar to {@link Object3D#animate(float, int)}.</p> 
	 * 
	 * @param sequence the number of {@link SkinClip} in {@link SkinClipSequence}. 1 is the first sequence. 
	 * 			0 means whole {@link SkinClipSequence}
	 * @param index time index   
	 * 
	 * @see SkinClipSequence
	 * @see Object3D#animate(float, int)
	 * @see #setAutoApplyAnimation(boolean)
	 * */
	public void animateSkin(float index, int sequence) {
		if (skinClipSequence == null)
			return;
		
		if (sequence == 0) {
			skinClipSequence.animate(index * skinClipSequence.getTime(), currentPose);
		} else {
			SkinClip clip = skinClipSequence.getClip(sequence - 1); 
			clip.applyTo(index * clip.getTime(), currentPose);
		}
		currentPose.updateTransforms();
		applySkinAnimation();
		
		if (autoApplyAnimation)
			applySkeletonPose();
	}
	
	/** Same as {@link #animatePose(float, int, float) animatePose(float, int, 1)} */
	public void animatePose(float index, int sequence) {
		animatePose(index, sequence, 1f);
	}
	
	/** <p>Animates this object using assigned {@link PoseClipSequence}. 
	 * Updates curentPose and if "auto apply animation" is enabled calls {@link #applySkeletonPose()} </p>
	 * 
	 * <p>Pose animations are cumulative if "auto apply animation" is disabled. 
	 * Each call to this method cancels previous skin animation.</p>
	 * 
	 * <p>This method behaves similar to {@link Object3D#animate(float, int)}.</p> 
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
		animatePoseDontApply(index, sequence, weight);
		if (autoApplyAnimation)
			applyAnimation();
	}
	
	void animatePoseDontApply(float index, int sequence, float weight) {
		if (poseClipSequence == null)
			return;
		
        // first reset to initial position
		if (destMeshDirty) {
			resetAnimation();
		}
		if (sequence == 0) {
			poseClipSequence.animate(index * poseClipSequence.getTime(), this, weight);
		} else {
			PoseClip clip = poseClipSequence.getClip(sequence - 1); 
			clip.applyTo(index * clip.getTime(), this, weight);
		}
	}
	

	/** 
	 * <p>Sets the {@link SkinClipSequence} of this object. The skeleton of ClipSequence must be the same of
	 * this object.</p>
	 * 
	 * <p>This method is analogue of {@link Object3D#setAnimationSequence(com.threed.jpct.Animation)}.</p>
	 * 
	 * @see #mergeSkin(Animated3D...)
	 * @throws IllegalArgumentException if given ClipSequence has a different {@link Skeleton} 
	 * */
	public void setPoseClipSequence(PoseClipSequence poseClipSequence) {
		this.poseClipSequence = poseClipSequence;
	}

	/** <p>Returns the assigned ClipSequence if any.</p> */
	public PoseClipSequence getPoseClipSequence() {
		return poseClipSequence;
	}

	/** <p>Returns the assigned ClipSequence if any.</p> */
	public SkinClipSequence getSkinClipSequence() {
		return skinClipSequence;
	}
	
	/** 
	 * <p>Sets the {@link SkinClipSequence} of this object. The skeleton of ClipSequence must be the same of
	 * this object.</p>
	 * 
	 * <p>This method is analogue of {@link Object3D#setAnimationSequence(com.threed.jpct.Animation)}.</p>
	 * 
	 * @see #mergeSkin(Animated3D...)
	 * @throws IllegalArgumentException if given ClipSequence has a different {@link Skeleton} 
	 * */
	public void setSkinClipSequence(SkinClipSequence clipSequence) {
		if (clipSequence != null) {
			if (skeleton == null)
				throw new IllegalStateException("This object has no skeleton");
			
			if (clipSequence.getSkeleton() != this.skeleton) 
				throw new IllegalArgumentException("ClipSequence's skeleton is different from this object's skeleton");
		}
		 
		this.skinClipSequence = clipSequence;
	}

	/**
	 * <p>Clones this object. Behaves same as {@link Object3D#cloneObject()} and copies
	 * animation data in addition.</p>
	 * 
	 *  @see Object3D#cloneObject()
	 * */
	@Override
	public Animated3D cloneObject() {
		return new Animated3D(this, MESH_REUSE); 
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
		out.writeInt(index);
		out.writeObject(skinClipSequence);
		out.writeObject(poseClipSequence);
	}
	
	SimpleVector[] getDestinationMesh() {
		return destMesh;
	}
	
	/** returns the index in group */
	public int getIndex() {
		return index;
	}

	/** sets the index in group */
	void setIndex(int index) {
		this.index = index;
	}

	/** 
	 * <p>Attaches the {@link IVertexController}. First calls {@link #calcNormals()} to avoid
	 * warning setting vertex controller on an object with no normals.</p> 
	 * */
	private void attachVertexController() {
		// we call calcNormals here to avoid warning setting vertex controller on an object with no normals 
		calcNormals();
		getMesh().setVertexController(vertexController, IVertexController.PRESERVE_SOURCE_MESH);
		
		sourceMesh = vertexController.getSourceMesh();
		destMesh = vertexController.getDestinationMesh();
	}
	
	/** applies skin animation to internal copy of mesh. actual mesh is not updated yet. */
	public void applySkinAnimation() {
        
        SimpleVector[] dest = destMesh;
        // if pose animation is applied, destination vertices are already initialized based on source and offseted, so use them  
        SimpleVector[] source = !destMeshDirty ? dest : sourceMesh;
        
        // Cycle through each vertex
        for (int i = 0; i < source.length; i++) {
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
                temp.set(source[i]);

                // Multiply our vertex by the matrix pallete entry
                temp.matMul(currentPose.palette[jointIndex]);
                
                // Sum, weighted.
                temp.scalarMul(weights[j]);
                vertexSum.add(temp);
            }

            // Store sum into _meshData
            dest[i].set(vertexSum);
            
        } // for vertices
        
		destMeshDirty = true;
	}
	
	/** 
	 * <p>Merge many <code>Animated3D</code>s into one. This method
	 * does not require all Animated3Ds share the same {@link Skeleton}
	 * but skeletons are <i>almost</i> identical.</p> 
	 * 
	 * <p>If many Animated3Ds are loaded from different files, their
	 * skeleton objects will be different even if they are identical.
	 * This method is meant to help such cases.</p>
	 * 
	 * <p>This method always uses the skeleton of first object.</p>
	 * 
	 *  @see AnimatedGroup#mergeSkin(raft.jpct.bones.AnimatedGroup...)
	 *  @see SkinClipSequence#merge(SkinClipSequence...)
	 * 
	 * */
	public static Animated3D mergeSkin(Animated3D... objects) {
		if (objects.length == 0) 
			throw new IllegalArgumentException("No objects!");
		
		// check skeletons match
		for (int i = 0; i < objects.length; i++) {
			for (int j = i + 1; j < objects.length; j++) {
				objects[i].skeleton.checkAlmostEqual(objects[j].skeleton);
				objects[i].skin.checkAlmostEqual(objects[j].skin);
			}
		}
		Animated3D merged = new Animated3D(objects[0], MESH_DONT_REUSE);
		
		List<SkinClipSequence> skinSequences = new LinkedList<SkinClipSequence>();
		for (Animated3D object : objects) {
			if (object.skinClipSequence != null)
				skinSequences.add(object.skinClipSequence);
		}
		if (!skinSequences.isEmpty()) {
			SkinClipSequence mergedSequence = SkinClipSequence.merge(
					skinSequences.toArray(new SkinClipSequence[skinSequences.size()]));
			merged.setSkinClipSequence(mergedSequence);
		}
		
		List<PoseClip> poseClips = new LinkedList<PoseClip>();
		for (Animated3D object : objects) {
			if (object.poseClipSequence != null) {
				for (PoseClip clip : object.poseClipSequence) 
					poseClips.add(clip);
			}
		}
		if (!poseClips.isEmpty()) {
			PoseClipSequence mergedSequence = new PoseClipSequence(poseClips);
			merged.setPoseClipSequence(mergedSequence);
		}
		
		return merged;
	} 
	
	
	/** we just use this to get mesh data */
	private class VertexController extends GenericVertexController {
		private static final long serialVersionUID = 1L;
		
		public void apply() {
		}
	}
	
}
