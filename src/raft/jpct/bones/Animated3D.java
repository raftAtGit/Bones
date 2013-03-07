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
import com.threed.jpct.Matrix;
import com.threed.jpct.Mesh;
import com.threed.jpct.Object3D;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.TextureManager;

/** 
 * <p>An {@link Object3D} which can be animated via skeletal or pose animation.</p> 
 * 
 * <p>{@link #animateSkin(float, int)} and {@link #animatePose(float, int)} methods are analogues of
 * {@link Object3D#animate(float, int)} method. They calculate a new Mesh and apply it if "auto apply animation"
 * is enabled (It's enabled by default). If you need animation blending, you should disable "auto apply animation",
 * first perform pose animation(s) then perform skin animation and finally call {@link #applyAnimation()}.</p>
 * 
 * <p>Once constructed, Animated3D sets an {@link IVertexController} on its mesh which
 * modifies the {@link Mesh} to perform animation. So if you set another IVertexController
 * on its mesh, you will break animation.</p>
 * 
 * <p>Animated objects often consist of sub-objects and in such cases manipulating them in an {@link AnimatedGroup}
 * is much easier and convenient. Most of the methods in this class also exist in AnimatedGroup to allow bulk operations.
 * For skin animations with many (more than one) sub objects, animating via AnimatedGroup also performs better since
 * {@link SkeletonPose} calculations are done once for whole group.</p>
 * 
 * <p>Skeletal animation updates vertex normals for proper lighting effects. However pose animation does not update normals
 * since it is simply too expensive. Pose animations are typically used for small deformations (like facial animations) 
 * so this wont be a problem for most cases.</p>
 * 
 * <p>Skeletal animation part of this class is adapted from <a href="http://www.ardor3d.com">Ardor3D.</a></p>
 * 
 * @see AnimatedGroup
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
	
	private Skeleton skeleton;
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
	private transient SimpleVector[] sourceNormals;
	private transient SimpleVector[] destNormals;
	
    private SimpleVector vertexSum = new SimpleVector();
    private SimpleVector vertexTemp = new SimpleVector();
	
    private SimpleVector normalSum = new SimpleVector();
    private SimpleVector normalTemp = new SimpleVector();
    
	private boolean autoApplyAnimation = true;
	
	// TODO maybe add a method createAnimationSequence() to create jPCT mesh animation sequence

	/** 
	 * <p>Same as {@link #Animated3D(Animated3D, boolean)  Animated3D(Object3D, MESH_REUSE)}</p>
	 * 
	 * @see #Animated3D(Animated3D, boolean)
	 */
	public Animated3D(Animated3D object) {
		this(object, MESH_REUSE);
	}
	
	/**
	 * <p>Behaves same as {@link Object3D#Object3D(Object3D, boolean) Object3D(Object3D, reuseMesh)}. 
	 * In addition copies animation data.</p> 
	 * 
	 * <p>Note: If master object has skin animation, its mesh must be in bind pose, 
	 * otherwise created object will have garbled animation.</p>
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
		this.autoApplyAnimation = object.autoApplyAnimation;
		this.index = object.index;

		attachVertexController();
	}

	/** Creates a Animated3D out of given information. */
	public Animated3D(MeshData meshData, SkinData skin, SkeletonPose currentPose) {
		super(meshData.coordinates, meshData.uvs, meshData.indices, TextureManager.TEXTURE_NOTFOUND);
//		super(meshData.coordinates, meshData.uvs, createIndices(meshData), TextureManager.TEXTURE_NOTFOUND);
		
		this.skeleton = (currentPose == null) ? null : currentPose.skeleton;
		this.currentPose  = currentPose;
		this.skin = skin; 
		this.meshData = meshData;

		attachVertexController();
	}
	
//	private static int[] createIndices(MeshData meshData) {
//		if (meshData.indices != null)
//			return meshData.indices;
//		System.out.println(meshData.coordinates.length);
//		int[] indices = new int[meshData.coordinates.length/9 * 3];
//		for (int i = 0; i < indices.length; i++) {
//			indices[i] = i;
//		}
//		return indices;
//	}

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
//		this(MeshData.readFromStream(in), SkinData.readFromStream(in), SkeletonPose.readFromStream(in));
		this.index = in.readInt();
		setName((String)in.readObject());
		this.skinClipSequence = (SkinClipSequence) in.readObject();
//		this.skinClipSequence = SkinClipSequence.readFromStream(in);
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

	void replaceSkeleton(Skeleton skeleton) {
		this.skeleton.checkAlmostEqual(skeleton);
		this.skeleton = skeleton;
		setSkeletonPose(new SkeletonPose(skeleton));
		
		if (skinClipSequence != null) {
			for (SkinClip clip : skinClipSequence) {
				clip.replaceSkeleton(skeleton);
			}
		}
	}
	
	/** Returns skin data if any. This method always returns a copy of SkinData so modifying it has no effect. */
	public SkinData getSkinData() {
		return (skin == null) ? null : skin.clone();
	}
	
	/** If this object is called via Ardor3D's or jME's loader, calling this method saves some memory. */
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
	
	/** Applies animation to mesh. */
	public void applyAnimation() {
		vertexController.updateMesh();
		touch();
		destMeshDirty = true;
	}
	
	/** <p>Animates this object using assigned {@link SkinClipSequence}. 
	 * Updates current {@link SkeletonPose}, applies current pose by calling
	 * {@link #applySkeletonPose()} and if "auto apply animation" is enabled calls {@link #applyAnimation()} </p>
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
		applySkeletonPose();
		
		if (autoApplyAnimation)
			applyAnimation();
	}
	
	/** Same as {@link #animatePose(float, int, float) animatePose(float, int, 1)} */
	public void animatePose(float index, int sequence) {
		animatePose(index, sequence, 1f);
	}
	
	/** <p>Animates this object using assigned {@link PoseClipSequence}.
	 * If "auto apply animation" is enabled calls {@link #applyAnimation()}</p>
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
	 * @see PoseClipSequence
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
	 * <p>Sets the {@link PoseClipSequence} of this object.</p>
	 * 
	 * <p>This method is analogue of {@link Object3D#setAnimationSequence(com.threed.jpct.Animation)}.</p>
	 * */
	public void setPoseClipSequence(PoseClipSequence poseClipSequence) {
		this.poseClipSequence = poseClipSequence;
	}

	/** <p>Returns the assigned {@link PoseClipSequence} if any.</p> */
	public PoseClipSequence getPoseClipSequence() {
		return poseClipSequence;
	}

	/** <p>Returns the assigned {@link SkinClipSequence} if any.</p> */
	public SkinClipSequence getSkinClipSequence() {
		return skinClipSequence;
	}
	
	/** 
	 * <p>Sets the {@link SkinClipSequence} of this object. The skeleton of SkinClipSequence must be the same of
	 * this object.</p>
	 * 
	 * <p>This method is analogue of {@link Object3D#setAnimationSequence(com.threed.jpct.Animation)}.</p>
	 * 
	 * @see #mergeAnimations(Animated3D...)
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
	 * <p>Clones this object. Behaves same as {@link #Animated3D(Animated3D, boolean) Animated3D(Animated3D, MESH_REUSE)}.</p>
	 *
	 *  @see #Animated3D(Animated3D, boolean)
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
			throw new IllegalStateException("this object does not contain mesh data. did you called discardMeshData() ?");

//		MeshData.writeToStream(meshData, out);
//		SkinData.writeToStream(skin, out);
//		SkeletonPose.writeToStream(currentPose, out);
		out.writeObject(meshData);
		out.writeObject(skin);
		out.writeObject(currentPose);
		out.writeInt(index);
		out.writeObject(getName());
		out.writeObject(skinClipSequence);
//		SkinClipSequence.writeToStream(skinClipSequence, out);
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

		sourceNormals = vertexController.getSourceNormals();
		destNormals = vertexController.getDestinationNormals();
	}
	
	/** Applies skin animation to internal copy of mesh. Actual mesh is not updated yet. 
	 * Call {@link #applyAnimation()} to update mesh.  */
	public void applySkeletonPose() {
        
        SimpleVector[] destMesh = this.destMesh;
        SimpleVector[] destNormals = this.destNormals;
        
        // if pose animation is applied, destination vertices are already initialized based on source and offseted, so use them  
        SimpleVector[] sourceMesh = !destMeshDirty ? destMesh : this.sourceMesh;
        //SimpleVector[] sourceNormals = !destMeshDirty ? destNormals : this.sourceNormals;
        SimpleVector[] sourceNormals = this.sourceNormals;
        
        SimpleVector vertexTemp = this.vertexTemp;
        SimpleVector vertexSum = this.vertexSum;
        
        SimpleVector normalTemp = this.normalTemp;
        SimpleVector normalSum = this.normalSum;
        
        float[][] skinWeights = skin.weights;
        short[][] skinJointIndices = skin.jointIndices;
        
        // Cycle through each vertex
        int end = sourceMesh.length;
        for (int i = 0; i < end; i++) {
            // zero out our sum var
            vertexSum.x = 0f;
            vertexSum.y = 0f;
            vertexSum.z = 0f;
            
            normalSum.x = 0f;
            normalSum.y = 0f;
            normalSum.z = 0f;

            // pull in joint data
            final float[] weights = skinWeights[i];
            final short[] jointIndices = skinJointIndices[i];

            SimpleVector sourceMesh_i = sourceMesh[i];
            SimpleVector sourceNormals_i = sourceNormals[i];
            Matrix[] currentPosePalette = currentPose.palette;
            
            for (int j = 0; j < Skeleton.MAX_JOINTS_PER_VERTEX; j++) {
                
            	final float weights_j = weights[j];
            	
            	if (weights_j == 0) {
                    continue;
                }

                Matrix mat = currentPosePalette[jointIndices[j]];
                // -- vertices --
                vertexTemp.x = sourceMesh_i.x;
                vertexTemp.y = sourceMesh_i.y;
                vertexTemp.z = sourceMesh_i.z;

                // Multiply our vertex by the matrix pallete entry
                vertexTemp.matMul(mat);
                
                // Sum, weighted.
                vertexTemp.x *= weights_j;
                vertexTemp.y *= weights_j;
                vertexTemp.z *= weights_j;
                
                vertexSum.x += vertexTemp.x;
                vertexSum.y += vertexTemp.y;
                vertexSum.z += vertexTemp.z;
                
                // -- normals --
                normalTemp.x = sourceNormals_i.x;
                normalTemp.y = sourceNormals_i.y;
                normalTemp.z = sourceNormals_i.z;

                // Multiply our vertex by the matrix pallete entry
                normalTemp.rotate(mat);
                
                // Sum, weighted.
                normalTemp.x *= weights_j;
                normalTemp.y *= weights_j;
                normalTemp.z *= weights_j;
                
                normalSum.x += normalTemp.x;
                normalSum.y += normalTemp.y;
                normalSum.z += normalTemp.z;
            }

            // Store sum into meshData
            destMesh[i].set(vertexSum);
            destNormals[i].set(normalSum);
            
        } // for vertices
        
		destMeshDirty = true;
	}

//	/** Applies skin animation to internal copy of mesh. Actual mesh is not updated yet. 
//	 * Call {@link #applyAnimation()} to update mesh.  */
//	public void applySkeletonPose() {
//        
//        SimpleVector[] destMesh = this.destMesh;
//        SimpleVector[] destNormals = this.destNormals;
//        
//        // if pose animation is applied, destination vertices are already initialized based on source and offseted, so use them  
//        SimpleVector[] sourceMesh = !destMeshDirty ? destMesh : this.sourceMesh;
//        //SimpleVector[] sourceNormals = !destMeshDirty ? destNormals : this.sourceNormals;
//        SimpleVector[] sourceNormals = this.sourceNormals;
//        
//        SimpleVector vertexTemp = this.vertexTemp;
//        SimpleVector vertexSum = this.vertexSum;
//        
//        SimpleVector normalTemp = this.normalTemp;
//        SimpleVector normalSum = this.normalSum;
//        
//        // Cycle through each vertex
//        for (int i = 0; i < sourceMesh.length; i++) {
//            // zero out our sum var
//            vertexSum.set(0f, 0f, 0f);
//            normalSum.set(0f, 0f, 0f);
//
//            // pull in joint data
//            final float[] weights = skin.weights[i];
//            final short[] jointIndices = skin.jointIndices[i];
//
//            // for each joint where the weight != 0
//            for (int j = 0; j < Skeleton.MAX_JOINTS_PER_VERTEX; j++) {
//                if (weights[j] == 0) {
//                    continue;
//                }
//
//                final int jointIndex = jointIndices[j];
//                // -- vertices --
//                vertexTemp.set(sourceMesh[i]);
//
//                // Multiply our vertex by the matrix pallete entry
//                vertexTemp.matMul(currentPose.palette[jointIndex]);
//                
//                // Sum, weighted.
//                vertexTemp.scalarMul(weights[j]);
//                vertexSum.add(vertexTemp);
//
//                
//                // -- normals --
//                normalTemp.set(sourceNormals[i]);
//
//                // Multiply our vertex by the matrix pallete entry
//                normalTemp.rotate(currentPose.palette[jointIndex]);
//                
//                // Sum, weighted.
//                normalTemp.scalarMul(weights[j]);
//                normalSum.add(normalTemp);
//                
//            }
//
//            // Store sum into meshData
//            destMesh[i].set(vertexSum);
//            destNormals[i].set(normalSum);
//            
//        } // for vertices
//        
//		destMeshDirty = true;
//	}
	
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
	 *  @see AnimatedGroup#mergeAnimations(raft.jpct.bones.AnimatedGroup...)
	 *  @see SkinClipSequence#merge(SkinClipSequence...)
	 * 
	 * */
	public static Animated3D mergeAnimations(Animated3D... objects) {
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
