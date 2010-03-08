package raft.jpct.bones;

import java.io.IOException;
import java.nio.IntBuffer;
import java.text.MessageFormat;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.ardor3d.extension.animation.skeletal.TransformData;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Transform;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.jmex.model.ogrexml.OgreEntityNode;
import com.jmex.model.ogrexml.anim.MeshAnimationController;
import com.jmex.model.ogrexml.anim.OgreMesh;
import com.threed.jpct.Logger;
import com.threed.jpct.Matrix;
import com.threed.jpct.SimpleVector;

/** 
 * <p>Contains static importer methods to convert jME and Ardor format to Bones format.</p> 
 * 
 * @author hakan eryargi (r a f t)
 * */
public class BonesImporter {

	/** can not be instantiated */
	private BonesImporter() {}
	
	/** 
	 * <p>Constructs a {@link AnimatedGroup} out of Ardor's skinData.</p>
	 * 
	 * @param colladaStorage Ardor collada storage
	 * @param scale the scale. not used at the moment.
	 * 
	 * @see AnimatedGroup
	 * */
	public static AnimatedGroup importCollada(ColladaStorage colladaStorage, float scale) { 
		if (colladaStorage.getSkins().isEmpty())
			throw new IllegalArgumentException("ColladaStorage contains no skins.");
		
		if (scale != 1f)
			Logger.log("Scale is not supported at the moment, ignoring", Logger.WARNING);
		
		Skeleton skeleton = convertArdorSkeleton(findUniqueArdorSkeleton(colladaStorage));
		
		SkeletonPose currentPose = new SkeletonPose(skeleton);
		currentPose.updateTransforms();
		
		List<Animated3D> objects = new LinkedList<Animated3D>();

		for (com.ardor3d.extension.model.collada.jdom.data.SkinData skinData : colladaStorage.getSkins()) {
			for (SkinnedMesh sm : skinData.getSkins()) {
				SkinData skin = convertArdorSkinData(sm);
				MeshData mesh = convertArdorMeshData(sm);
				Animated3D skinnedObject = new Animated3D(mesh, skin, currentPose);
				objects.add(skinnedObject);
			}
		}
		
		AnimatedGroup group = new AnimatedGroup(objects.toArray(new Animated3D[objects.size()]));
		
		List<com.ardor3d.extension.animation.skeletal.JointChannel> jointChannels = colladaStorage.getJointChannels();
		if ((jointChannels != null && !jointChannels.isEmpty())) {
			SkinClip clip = convertArdorSkinClip(skeleton, jointChannels);
			group.setSkinClipSequence(new SkinClipSequence(clip));
			Logger.log("Created one animation clip", Logger.MESSAGE);
		}
		return group;
	}

	
	/** 
	 * <p>Constructs a {@link AnimatedGroup} out of jME OGRE data.</p>
	 * 
	 * @param node jME OgreEntityNode
	 * @param scale the scale. not used at the moment.
	 * */
	public static AnimatedGroup importOgre(OgreEntityNode node, float scale) throws IOException {
		if (node.getControllerCount() == 0)
			throw new IllegalArgumentException("No controller found in OgreEntityNode. Means there is no skeleton or pose animation!");
		
		if (scale != 1f)
			Logger.log("Scale is not supported at the moment, ignoring", Logger.WARNING);
		
		MeshAnimationController controller = (MeshAnimationController) node.getController(0);

		Skeleton skeleton = null;
		SkeletonPose currentPose = null;
		
		if (controller.getSkeleton() != null) {
			skeleton = convertJMESkeleton(controller.getSkeleton());
			
			currentPose = new SkeletonPose(skeleton);
			currentPose.updateTransforms();
		}
		
		
		List<Animated3D> list = new LinkedList<Animated3D>();
		
		int index = 0;
		for (OgreMesh ogreMesh : controller.getMeshList()) {
			SkinData skin = (skeleton == null) ? null : convertJMESkinData(ogreMesh);
			MeshData mesh = convertJMEMeshData(ogreMesh);

			Animated3D skinnedObject = new Animated3D(mesh, skin, currentPose);
			skinnedObject.setIndex(index++);
			list.add(skinnedObject);
		}
		
		Animated3D[] skinnedObjects = list.toArray(new Animated3D[list.size()]);
		
		List<SkinClip> skeletonClips = new LinkedList<SkinClip>();
		List<PoseClip> poseClips = new LinkedList<PoseClip>();
		//Map<Skinned3D, List<PoseClip>> poseClips = new IdentityHashMap<Skinned3D, List<PoseClip>>();
		
		for (com.jmex.model.ogrexml.anim.Animation anim : controller.getAnimations()) {
			if (anim.hasBoneAnimation()) {
				if (skeleton == null)
					throw new IllegalStateException("Skeleton is null but controller has Bone animation!");
				
				com.jmex.model.ogrexml.anim.BoneAnimation boneAnim = anim.getBoneAnimation();
				skeletonClips.add(convertJMESkinClip(skeleton, boneAnim));
				Logger.log("Created skeleton animation clip: " + boneAnim.getName(), Logger.MESSAGE);
			}
			
			if (anim.hasMeshAnimation()) {
				com.jmex.model.ogrexml.anim.MeshAnimation meshAnim = anim.getMeshAnimation();
				List<MeshChannel> poseChannels = new LinkedList<MeshChannel>();
				
				for (com.jmex.model.ogrexml.anim.Track track : meshAnim.getTracks()) {
					if (!(track instanceof com.jmex.model.ogrexml.anim.PoseTrack)) {
						Logger.log("skipping none pose track " + track.getClass(), Logger.WARNING);
						continue;
					}
					com.jmex.model.ogrexml.anim.PoseTrack poseTrack = (com.jmex.model.ogrexml.anim.PoseTrack) track;
					poseChannels.add(convertJMEMeshChannel(poseTrack)); 
					
				}
				if (poseChannels.isEmpty()) {
					Logger.log("No pose tracks in mesh animation '" + meshAnim.getName() + "', skipping completely", Logger.WARNING);
					continue;
				}
				PoseClip poseClip = new PoseClip(skinnedObjects.length, poseChannels);
				poseClip.setName(meshAnim.getName());
				poseClips.add(poseClip);
				
				Logger.log("Created pose animation clip: " + meshAnim.getName(), Logger.MESSAGE);
				
			}			
		}
		
		AnimatedGroup group = new AnimatedGroup(skinnedObjects);
		if (!skeletonClips.isEmpty()) 
			group.setSkinClipSequence(new SkinClipSequence(skeletonClips));
		if (!poseClips.isEmpty()) 
			group.setPoseClipSequence(new PoseClipSequence(poseClips));
		
		return group;
	}
	
	private static com.ardor3d.extension.animation.skeletal.Skeleton findUniqueArdorSkeleton(ColladaStorage colladaStorage) {
		if (colladaStorage.getSkins().isEmpty())
			throw new IllegalArgumentException("ColladaStorage contains no skins.");
		
		com.ardor3d.extension.animation.skeletal.Skeleton skeleton = null;
		
		for (com.ardor3d.extension.model.collada.jdom.data.SkinData skin : colladaStorage.getSkins()) {
			if (skeleton == null)
				skeleton = skin.getPose().getSkeleton();
			if (skeleton != skin.getPose().getSkeleton())
				throw new IllegalArgumentException("There are more than one skeletons in ColladaStorage.");
		}
		assert (skeleton != null);
		return skeleton;
	}

	private static Skeleton convertArdorSkeleton(com.ardor3d.extension.animation.skeletal.Skeleton skeleton) {
		Joint[] joints = new Joint[skeleton.getJoints().length];
		
		for (int i = 0; i < joints.length; i++) {
			joints[i] = convertArdorJoint(skeleton.getJoints()[i]);
		}
		Skeleton result = new Skeleton(joints);
		Logger.log("Skeleton created out of Ardor3D skeleton", Logger.MESSAGE);
		return result;
	}

	private static Joint convertArdorJoint(com.ardor3d.extension.animation.skeletal.Joint joint) {
		Matrix inverseBindPose = getMatrix(joint.getInverseBindPose());
		int parentIndex = (joint.getParentIndex() == com.ardor3d.extension.animation.skeletal.Joint.NO_PARENT) 
				? Joint.NO_PARENT : joint.getParentIndex();
		return new Joint(inverseBindPose, joint.getIndex(), parentIndex, joint.getName());
	}
	
	
	private static Skeleton convertJMESkeleton(com.jmex.model.ogrexml.anim.Skeleton skeleton) {
		Joint[] joints = new Joint[skeleton.getBoneCount()];
		
		// it's not guaranteed bones are sorted for hierarchy 
		// so we first create a map of joint indices
		Map<com.jmex.model.ogrexml.anim.Bone, Integer> map =
			new IdentityHashMap<com.jmex.model.ogrexml.anim.Bone, Integer>();

		for (int i = 0; i < joints.length; i++) {
			map.put(skeleton.getBone(i), i);
		}
		
		// we cannot change original ordering of joints since channels use target joint index
		for (int i = 0; i < joints.length; i++) {
			joints[i] = convertJMEJoint(map, skeleton.getBone(i), i);
		}
		
		Skeleton result = new Skeleton(joints);
		Logger.log(MessageFormat.format("Skeleton created out of jME OGRE skeleton, {0} joints", joints.length), Logger.MESSAGE);
		return result;
	}
	
	private static Joint convertJMEJoint(Map<com.jmex.model.ogrexml.anim.Bone, Integer> parentMap, 
			com.jmex.model.ogrexml.anim.Bone bone, int index) {
		
		com.jmex.model.ogrexml.anim.Bone root = bone;
		while (root.getParent() != null) {
			root = root.getParent();
		}
		
		com.jme.math.Vector3f tx = bone.getWorldBindInversePos();
		com.jme.math.Quaternion rot = bone.getWorldBindInverseRot();
		// due to jME's OGRE loading, root rotation is baked into all bind poses, remove it  
		tx = root.getWorldBindInverseRot().mult(tx);

		Matrix inverseBindPose = convertQuaternion(rot).getRotationMatrix();
		inverseBindPose.translate(tx.x, tx.y, tx.z);
		
		int parentIndex = (bone.getParent() == null) ? Joint.NO_PARENT  
				: parentMap.get(bone.getParent());
		
		return new Joint(inverseBindPose, index, parentIndex, bone.getName());
		
	}

	private static MeshData convertArdorMeshData(SkinnedMesh mesh) {
		final float[] coordinates = SkinHelper.asArray(mesh.getMeshData().getVertexBuffer()); 
		final float[] uvs = SkinHelper.asArray(mesh.getMeshData().getTextureBuffer(0));
		
		IntBuffer indexBuffer = mesh.getMeshData().getIndexBuffer();
		final int[] indices = (indexBuffer == null) ? null : SkinHelper.asArray(indexBuffer);
		
		return new MeshData(coordinates, uvs, indices);
	}
	
	private static MeshData convertJMEMeshData(OgreMesh mesh) {
		final float[] coordinates = SkinHelper.asArray(mesh.getVertexBuffer());
		final float[] uvs;
		if (mesh.getTextureCoords().isEmpty()) {
			Logger.log("Mesh has no texture coodinates", Logger.WARNING);
			uvs = null;
		} else {
			uvs = SkinHelper.asArray(mesh.getTextureCoords().get(0).coords);
		}
		
		IntBuffer indexBuffer = mesh.getIndexBuffer();
		final int[] indices = (indexBuffer == null) ? null : SkinHelper.asArray(indexBuffer);
		
		return new MeshData(coordinates, uvs, indices);
	}
	

    /**
     * <p>Creates a new Channel out of Ardor3D's JointChannel.<p> 
     * */
	private static JointChannel convertArdorJointChannel(com.ardor3d.extension.animation.skeletal.JointChannel jointChannel) {
		int jointIndex = parseJointIndex(jointChannel); 
		int length = jointChannel.getLength();

		float[] times = new float[length];
		Quaternion[] rotations = new Quaternion[length];
		SimpleVector[] translations = new SimpleVector[length];
		SimpleVector[] scales = new SimpleVector[length];
		
		TransformData tmpTransform = new TransformData(); 
		for (int sampleIndex = 0; sampleIndex < length; sampleIndex++) {
			jointChannel.setCurrentSample(sampleIndex, tmpTransform);
			
			times[sampleIndex] = jointChannel.getTime(sampleIndex);
			rotations[sampleIndex] = convertQuaternion(tmpTransform.getRotation());
			translations[sampleIndex] = convertArdorVector(tmpTransform.getTranslation());
			scales[sampleIndex] = convertArdorVector(tmpTransform.getScale());
		}
		JointChannel result = new JointChannel(jointIndex, times, translations, rotations, scales);
//		Logger.log(MessageFormat.format("JointChannel created out of Ardor JointChannel, {0} keys", length), Logger.MESSAGE);
		return result;
	}
	
    /**
     * <p>Creates a new Channel out of jME OGRE BoneTrack. Skeleton is used
     * for transforming track data into joint local space.<p> 
     * */
	private static JointChannel convertJMEJointChannel(com.jmex.model.ogrexml.anim.BoneTrack track, Skeleton skeleton) {
		int jointIndex = track.getTargetBoneIndex();
		int length = track.getTimes().length;

		float[] times = new float[length];
		Quaternion[] rotations = new Quaternion[length];
		SimpleVector[] translations = new SimpleVector[length];
		SimpleVector[] scales = new SimpleVector[length];
		
		// jME OGRE tracks are relative to joints, we need to take them to joint local space
		Joint joint = skeleton.getJoint(jointIndex);
		Joint parentJoint = joint.hasParent() ? 
				skeleton.getJoint(joint.getParentIndex()) : null;

		for (int sampleIndex = 0; sampleIndex < length; sampleIndex++) {
			times[sampleIndex] = track.getTimes()[sampleIndex];
			// there is no scale information in jME OGRE implementation
			scales[sampleIndex] = new SimpleVector(1f, 1f, 1f); 
			
			Matrix m = convertQuaternion(track.getRotations()[sampleIndex]).getRotationMatrix();
			com.jme.math.Vector3f tx = track.getTranslations()[sampleIndex];
			m.translate(tx.x, tx.y, tx.z);

			m.matMul(joint.getBindPose()); // -> take to joint object space
			if (joint.hasParent()) {
				// remove parent transform -> take to joint local space 
				m.matMul(parentJoint.getInverseBindPose());
			}
			rotations[sampleIndex] = new Quaternion(m);
			translations[sampleIndex] = m.getTranslation();
		}
		JointChannel result = new JointChannel(jointIndex, times, translations, rotations, scales);
		//Logger.log(MessageFormat.format("JointChannel created out of jME BoneTrack, {0} keys", length), Logger.MESSAGE);
		return result;
	}
	
	private static SkinClip convertArdorSkinClip(Skeleton skeleton, 
			List<com.ardor3d.extension.animation.skeletal.JointChannel> jointChannels) {
		
		List<JointChannel> channels = new LinkedList<JointChannel>();
		for (com.ardor3d.extension.animation.skeletal.JointChannel jointChannel : jointChannels) {
			channels.add(convertArdorJointChannel(jointChannel));
		}
		SkinClip result = new SkinClip(skeleton, channels);
		//Logger.log(MessageFormat.format("SkinClip created out of Ardor JointChannel's, {0} channels", channels.size()), Logger.MESSAGE);
		return result;
	}
	
	private static SkinClip convertJMESkinClip(Skeleton skeleton, com.jmex.model.ogrexml.anim.BoneAnimation boneAnimation) {
		
		List<JointChannel> channels = new LinkedList<JointChannel>();
		for (com.jmex.model.ogrexml.anim.BoneTrack track : boneAnimation.getTracks()) {
			channels.add(convertJMEJointChannel(track, skeleton));
		}
		SkinClip result = new SkinClip(skeleton, channels);
		result.setName(boneAnimation.getName());
//		Logger.log(MessageFormat.format("SkinClip created out of jME BoneAnimation, {0} channels, name: {1}", 
//				channels.size(), result.getName()), Logger.MESSAGE);
		return result;
	}
	
	
    private static short parseJointIndex(com.ardor3d.extension.animation.skeletal.JointChannel jointChannel) {
    	String prefix = com.ardor3d.extension.animation.skeletal.JointChannel.JOINT_CHANNEL_NAME;
    	return Short.parseShort(jointChannel.getChannelName().substring(prefix.length()));
    }
	
    private static SkinData convertArdorSkinData(SkinnedMesh skinnedMesh) {
    	float[][] weights = SkinHelper.asArray(skinnedMesh.getWeights(), Skeleton.MAX_JOINTS_PER_VERTEX);
    	short[][] jointIndices = SkinHelper.asArray(skinnedMesh.getJointIndices(), Skeleton.MAX_JOINTS_PER_VERTEX); 
		return new SkinData(weights, jointIndices);
				
	}

    private static SkinData convertJMESkinData(OgreMesh ogreMesh) {
    	float[][] weights = SkinHelper.asArray(ogreMesh.getWeightBuffer().getWeights(), Skeleton.MAX_JOINTS_PER_VERTEX);
    	short[][] jointIndices = SkinHelper.asShortArray(ogreMesh.getWeightBuffer().getIndexes(), Skeleton.MAX_JOINTS_PER_VERTEX); 
		return new SkinData(weights, jointIndices);
	}
	
    private static MeshChannel convertJMEMeshChannel(com.jmex.model.ogrexml.anim.PoseTrack poseTrack) {
    	int length = poseTrack.getTimes().length;
    	
    	PoseFrame[] frames = new PoseFrame[length];
    	float[] times = new float[length];
    	
    	Map<com.jmex.model.ogrexml.anim.Pose, MeshPose> poseCache = 
    		new IdentityHashMap<com.jmex.model.ogrexml.anim.Pose, MeshPose>();
    	
    	for (int i = 0; i < times.length; i++) {
    		
    		com.jmex.model.ogrexml.anim.PoseTrack.PoseFrame jmeFrame = poseTrack.getFrames()[i];
    		
    		MeshPose[] framePoses = new MeshPose[jmeFrame.getPoses().length];
    		
    		for (int j = 0; j < framePoses.length; j++) {
    			com.jmex.model.ogrexml.anim.Pose jmePose = jmeFrame.getPoses()[j];
    			MeshPose pose = poseCache.get(jmePose);
    			if (pose == null) {
    				pose = convertJMEMeshPose(jmePose);
    				poseCache.put(jmePose, pose);
    			}
    			framePoses[j] = pose;
    		}
    		
    		times[i] = poseTrack.getTimes()[i];
    		frames[i] = new PoseFrame(framePoses, jmeFrame.getWeights());
    	}
    	
    	return new MeshChannel(poseTrack.getTargetMeshIndex(), frames, times);
    }
    
    private static MeshPose convertJMEMeshPose(com.jmex.model.ogrexml.anim.Pose pose) {
		int length = pose.getIndices().length;
		int[] indices = new int[length];
		SimpleVector[] offsets = new SimpleVector[length];
		
		for (int i = 0; i < indices.length; i++) {
        	indices[i] = pose.getIndices()[i];
        	offsets[i] = convertJMEVector(pose.getOffsets()[i]);
		}
		return new MeshPose(pose.getName(), offsets, indices);
	}

	
	/** converts a transform matrix to a jPCT Matrix. rotation and translation information is retrieved. */
	public static Matrix convertArdorMatrix(Matrix4 m4) {
		Matrix m = new Matrix();
		
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				m.set(j, i, m4.getValuef(i, j));
			}
		}
		m.translate(m4.getValuef(0, 3), m4.getValuef(1, 3), m4.getValuef(2, 3));
		return m;
	}
	
	
	/** converts a transform to a jPCT Matrix. rotation and translation information is retrieved. */
	public static Matrix getMatrix(ReadOnlyTransform transform) {
		return convertArdorMatrix(transform.getHomogeneousMatrix(null));
	}
	
	/** converts Ardor3D Vector3 to jPCT SimpleVector */
	public static SimpleVector convertArdorVector(com.ardor3d.math.Vector3 vector3) {
		return new SimpleVector(vector3.getXf(), vector3.getYf(), vector3.getZf());
	}
	
	/** converts jME Vector3f to jPCT SimpleVector */
	public static SimpleVector convertJMEVector(com.jme.math.Vector3f vector3) {
		return new SimpleVector(vector3.x, vector3.y, vector3.z);
	}
	
    /**
     * Constructs a new quaternion from ardor quaternion
     */
	public static Quaternion convertQuaternion(com.ardor3d.math.Quaternion quat) {
		return new Quaternion(quat.getXf(), quat.getYf(), quat.getZf(), quat.getWf());
    }

    /**
     * Constructs a new quaternion from jME quaternion
     */
	public static Quaternion convertQuaternion(com.jme.math.Quaternion quat) {
		return new Quaternion(quat.x, quat.y, quat.z, quat.w);
    }
	
	/** creates an ardor Transform out of given jPCT matrix */
	public static Transform getTransform(Matrix m) {
		Transform t = new Transform();
		
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				((Matrix3)t.getMatrix()).setValue(i, j, m.get(j, i));
			}
		}
		
		SimpleVector translation = m.getTranslation();
		t.translate(translation.x, translation.y, translation.z);
		
		return t;
	}

}

