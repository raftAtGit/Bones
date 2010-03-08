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
 * <p>An {@link Object3D} which can be animated via skeletal animation or pose animation.</p> 
 * 
 * <p>Once constructed, Skinned3D sets a {@link IVertexController} on its mesh which
 * deforms the {@link Mesh} to perform animation. So if you set another IVertexController
 * on its mesh, you will break skeletal animation.</p>
 * 
 * <p>Skeletal animation part of this class is adapted from <a href="http://www.ardor3d.com">Ardor3D.</a></p>
 * 
 * @see BonesIO#loadObject(java.io.InputStream)
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
	 * In addition copies skin information.</p>
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
		this.index = object.index;

		attachVertexController();
	}

	/** Creates a Skinned3D out of given information. */
	public Animated3D(MeshData meshData, SkinData skin, SkeletonPose currentPose) {
		super(meshData.coordinates, meshData.uvs, meshData.indices, TextureManager.TEXTURE_NOTFOUND);
		
		this.skeleton = (currentPose == null) ? null : currentPose.skeleton;
		this.currentPose  = currentPose;
		this.skin = skin; 
		this.meshData = meshData;

		attachVertexController();
	}

	/** Creates a Skinned3D out of given information. */
	public Animated3D(Object3D object, SkinData skin, SkeletonPose currentPose) {
		super(object, MESH_DONT_REUSE);
		
		this.skeleton = (currentPose == null) ? null : currentPose.skeleton;
		this.currentPose  = currentPose;
		this.skin = skin; 

		attachVertexController();
	}
	
	/** 
	 * Creates a Skinned3D by re-loading information previously saved to stream. 
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
	
	public boolean isAutoApplyAnimation() {
		return autoApplyAnimation;
	}

	public void setAutoApplyAnimation(boolean autoApplyAnimation) {
		this.autoApplyAnimation = autoApplyAnimation;
	}

	public void applyAnimation() {
		vertexController.updateMesh();
		touch();
		destMeshDirty = true;
	}
	
	/** <p>Animates this object using assigned {@link SkinClipSequence}. 
	 * Updates curentPose and calls {@link #applySkeletonPose()}</p>
	 * 
	 * <p>Note, if many skinned objects share the same pose,
	 * updating current pose for each of them is a waste. Consider
	 * using {@link AnimatedGroup#animateSkin(float, int)} 
	 * </p>
	 * 
	 * <p>This method behaves similar to {@link Object3D#animate(float, int)}.</p> 
	 * 
	 * @see SkinClipSequence
	 * @see Object3D#animate(float, int)
	 * @throws NullPointerException if clipSequence is null
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
	public void animatePose(float index, int sequence) {
		animatePose(index, sequence, 1f);
	}
	
	/** <p>Animates this object using assigned {@link SkinClipSequence}. 
	 * Updates curentPose and calls {@link #applySkeletonPose()}</p>
	 * 
	 * <p>Note, if many skinned objects share the same pose,
	 * updating current pose for each of them is a waste. Consider
	 * using {@link AnimatedGroup#animateSkin(float, int)} 
	 * </p>
	 * 
	 * <p>This method behaves similar to {@link Object3D#animate(float, int)}.</p> 
	 * 
	 * @see SkinClipSequence
	 * @see Object3D#animate(float, int)
	 * @throws NullPointerException if clipSequence is null
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
            for (int i = 0; i < sourceMesh.length; i++) {
            	destMesh[i].set(sourceMesh[i]);
            }
            destMeshDirty = false;
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
	 * skinning information in addition.</p>
	 * 
	 *  @see Object3D#cloneObject()
	 * */
	@Override
	public Animated3D cloneObject() {
		return new Animated3D(this, MESH_REUSE); 
	}
	
//	public void addPoseBlend(int sequence, float seconds) {
//		skinVertexController.addPose(seconds, poseClipSequence.getClip(sequence-1));
//	}
//	
//	static class SkinBlend {
//		final int sequence; 
//		final float seconds;
//		
//		SkinBlend(int sequence, float seconds) {
//			this.sequence = sequence;
//			this.seconds = seconds;
//		}
//	}
//	
//	private List<SkinBlend> skinBlends = new LinkedList<SkinBlend>();
//	
//	public void addSkinBlend(int sequence, float seconds) {
//		//skinVertexController.addSkin(seconds, clipSequence.getClip(sequence-1));
//		skinBlends.add(new SkinBlend(sequence, seconds));
//	}
//
//	public void animateBlend() {
//		skinVertexController.applyPoseAnimation = skinVertexController.lastPoseIndex != 0;
//		
//		skinVertexController.applySkinAnimation = !skinBlends.isEmpty();
//		if (!skinBlends.isEmpty()) {
//			if (skinBlends.size() == 1) {
//				SkinBlend skinBlend = skinBlends.get(0);
//				clipSequence.getClip(skinBlend.sequence-1).applyTo(skinBlend.seconds, currentPose);
//			} else {
//				Matrix[][] locals = new Matrix[skeleton.getNumberOfJoints()][skinBlends.size()];
//				for (int i = 0; i < skinBlends.size(); i++) {
//					SkinBlend skinBlend = skinBlends.get(i);
//					clipSequence.getClip(skinBlend.sequence-1).applyTo(skinBlend.seconds, currentPose);
//					
//					for (int j = 0; j < skeleton.getNumberOfJoints(); j++) {
//						locals[j][i] = currentPose.getLocal(j).cloneMatrix();
//					}
//				}
//				for (int i = 0; i < skeleton.getNumberOfJoints(); i++) {
//					SimpleVector tx = new SimpleVector();
//					SimpleVector scale = new SimpleVector();
//					Quaternion rot = new Quaternion();
//					
//					for (int j = 0; j < skinBlends.size(); j++) {
//						Matrix m = locals[i][j];
//						tx.add(m.getTranslation());
//						//scale.add(SkinHelper.);
//						rot.add(new Quaternion(m));
//					}
//					rot.scalarMul(1f/skinBlends.size());
//					tx.scalarMul(1f/skinBlends.size());
//					Matrix m = new Matrix();
//					rot.setRotation(m);
//					m.translate(tx);
//					currentPose.locals[i].setTo(m);
//				}
//			}
//		}
//		
//		currentPose.updateTransforms();
//		
//		skinBlends.clear();
//		getMesh().applyVertexController();
//	}
	
	
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
		
		if (sourceMesh == null) {
			sourceMesh = vertexController.getSourceMesh();
			destMesh = vertexController.getDestinationMesh();
		}
	}
	
	void applySkinAnimation() {
        
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
		
		List<SkinClipSequence> sequences = new LinkedList<SkinClipSequence>();
		for (Animated3D object : objects) {
			if (object.skinClipSequence != null)
				sequences.add(object.skinClipSequence);
		}
		if (!sequences.isEmpty()) {
			SkinClipSequence mergedSequence = SkinClipSequence.merge(sequences.toArray(new SkinClipSequence[sequences.size()]));
			merged.setSkinClipSequence(mergedSequence);
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
