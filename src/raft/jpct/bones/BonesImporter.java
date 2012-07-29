package raft.jpct.bones;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.IntBuffer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import com.jme.scene.TexCoords;
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
	 * <p>Constructs an {@link AnimatedGroup} out of Ardor's skinData.</p>
	 * 
	 * @param colladaStorage Ardor collada storage
	 * @param scale the scaling applied
	 * @param rotation the rotation applied to whole system. maybe null
	 * 
	 * @see AnimatedGroup
	 * */
	public static AnimatedGroup importCollada(ColladaStorage colladaStorage, float scale, Quaternion rotation) { 
		if (colladaStorage.getSkins().isEmpty())
			throw new IllegalArgumentException("ColladaStorage contains no skins.");
		
		if (scale == 0)
			throw new IllegalArgumentException("scale: " + scale);
		
		Matrix transform = null;
		if ((scale != 1) || (rotation != null)) {
			transform = new Matrix();
			if (rotation != null)
				rotation.setRotation(transform);
			if (scale != 1) 
				transform.matMul(SkinHelper.getScaleMatrix(scale));
		}
		
		Skeleton skeleton = convertArdorSkeleton(findUniqueArdorSkeleton(colladaStorage));
		
		SkeletonPose currentPose = new SkeletonPose(skeleton);
		currentPose.updateTransforms();
		
		List<Animated3D> objects = new LinkedList<Animated3D>();

		for (com.ardor3d.extension.model.collada.jdom.data.SkinData skinData : colladaStorage.getSkins()) {
			for (SkinnedMesh sm : skinData.getSkins()) {
				SkinData skin = convertArdorSkinData(sm);
				MeshData mesh = convertArdorMeshData(sm);
				
				if (transform != null)
					mesh.applyTransform(transform);
				
				Animated3D skinnedObject = new Animated3D(mesh, skin, currentPose);
				skinnedObject.setName(sm.getName());
				objects.add(skinnedObject);
			}
		}
		
		AnimatedGroup group = new AnimatedGroup(objects.toArray(new Animated3D[objects.size()]));
		
		// there is no example of collada file with multiple animations
		List<com.ardor3d.extension.animation.skeletal.JointChannel> jointChannels = colladaStorage.getJointChannels();
		if ((jointChannels != null && !jointChannels.isEmpty())) {
			SkinClip skinClip = convertArdorSkinClip(skeleton, jointChannels);
			group.setSkinClipSequence(new SkinClipSequence(skinClip));
			
			if (rotation != null) {
				for (JointChannel channel : skinClip) {
					if (channel != null) 
						channel.rotate(skeleton, rotation);
				}
			}
			if (scale != 1) {
				for (JointChannel channel : skinClip) {
					if (channel != null) { 
						channel.scale(scale);
					}
				}
			}
			Logger.log("Created one animation clip", Logger.MESSAGE);
		}
		
		// finally rotate/scale skeleton. this should be done after jointChannels are rotated/scaled
		if ((skeleton != null) && (transform != null)) {
			if (rotation != null)
				skeleton.rotate(rotation);
			if (scale != 1)
				skeleton.scale(scale);
			
			currentPose.setToBindPose();
			currentPose.updateTransforms();
		}
		
		return group;
	}

	
	/** 
	 * <p>Constructs an {@link AnimatedGroup} out of jME OGRE data.</p>
	 * 
	 * @param node jME OgreEntityNode
	 * @param scale the scale
	 * @param rotation the rotation applied to whole system. maybe null
	 * */
	public static AnimatedGroup importOgre(OgreEntityNode node, float scale, Quaternion rotation) throws IOException {
		if (node.getControllerCount() == 0)
			throw new IllegalArgumentException("No controller found in OgreEntityNode. Means there is no skeleton or pose animation!");
		
		if (scale == 0)
			throw new IllegalArgumentException("scale: " + scale);
		
		Matrix transform = null;
		if ((scale != 1) || (rotation != null)) {
			transform = new Matrix();
			if (rotation != null)
				rotation.setRotation(transform);
			if (scale != 1) 
				transform.matMul(SkinHelper.getScaleMatrix(scale, scale, scale));
		}
		
		MeshAnimationController controller = (MeshAnimationController) node.getController(0);

		Skeleton skeleton = null;
		SkeletonPose currentPose = null;
		int[] jointOrder = null;
		
		if (controller.getSkeleton() != null) {
			com.jmex.model.ogrexml.anim.Skeleton jmeSkeleton = controller.getSkeleton();
			jmeSkeleton.reset();
			
			List<com.jmex.model.ogrexml.anim.Bone> jmeBones = new ArrayList<com.jmex.model.ogrexml.anim.Bone>();
			for (int i = 0; i < jmeSkeleton.getBoneCount(); i++) {
				jmeBones.add(jmeSkeleton.getBone(i));
			}
			// sort bones such that parents always comes before 
			Collections.sort(jmeBones, new Comparator<com.jmex.model.ogrexml.anim.Bone>() {
				public int compare(com.jmex.model.ogrexml.anim.Bone b1, com.jmex.model.ogrexml.anim.Bone b2) {
					return getDepth(b1) - getDepth(b2);
				}
				
				private int getDepth(com.jmex.model.ogrexml.anim.Bone b) {
					com.jmex.model.ogrexml.anim.Bone parent = b.getParent();
					int depth = 0;
					while (parent != null) {
						depth++;
						parent = parent.getParent();
					}
					return depth;
				}
			});
			assert (ordered(jmeBones));
			jointOrder = new int[jmeBones.size()];
			for (int i = 0; i < jointOrder.length; i++) {
				jointOrder[i] = jmeSkeleton.getBoneIndex(jmeBones.get(i));
			}
			
			skeleton = convertJMESkeleton(jmeSkeleton, jointOrder);

			currentPose = new SkeletonPose(skeleton);
			currentPose.updateTransforms();
		}
		
		List<Animated3D> list = new LinkedList<Animated3D>();
		
		int index = 0;
		for (OgreMesh ogreMesh : controller.getMeshList()) {
			
			SkinData skin = (skeleton == null) ? null : convertJMESkinData(ogreMesh, jointOrder);
			MeshData mesh = convertJMEMeshData(ogreMesh);
			
			//saveMeshData(mesh, "C:/tmp/bones_tmp/mesh.ser");


			if (transform != null)
				mesh.applyTransform(transform);

			Animated3D skinnedObject = new Animated3D(mesh, skin, currentPose);
			skinnedObject.setName(ogreMesh.getName());
			skinnedObject.setIndex(index++);
			list.add(skinnedObject);
		}
		
		Animated3D[] objects = list.toArray(new Animated3D[list.size()]);
		
		List<SkinClip> skinClips = new LinkedList<SkinClip>();
		List<PoseClip> poseClips = new LinkedList<PoseClip>();
		
    	// first iterate through all pose animations to collect poses
    	Map<com.jmex.model.ogrexml.anim.Pose, MeshPose> poseCache = createJMEPoseCache(controller, transform);
    			new IdentityHashMap<com.jmex.model.ogrexml.anim.Pose, MeshPose>();
		
		for (com.jmex.model.ogrexml.anim.Animation anim : controller.getAnimations()) {
			if (anim.hasBoneAnimation()) {
				if (skeleton == null)
					throw new IllegalStateException("Skeleton is null but controller has Bone animation!");
				
				com.jmex.model.ogrexml.anim.BoneAnimation boneAnim = anim.getBoneAnimation();
				SkinClip skinClip = convertJMESkinClip(skeleton, boneAnim, jointOrder); 
				skinClips.add(skinClip);

				if (rotation != null) {
					for (JointChannel channel : skinClip) {
						if (channel != null) 
							channel.rotate(skeleton, rotation);
					}
				}
				if (scale != 1) {
					for (JointChannel channel : skinClip) {
						if (channel != null) 
							channel.scale(scale);
					}
				}
				Logger.log("Created skeleton animation clip: " + boneAnim.getName(), Logger.MESSAGE);
			}
			
			if (anim.hasMeshAnimation()) {
				com.jmex.model.ogrexml.anim.MeshAnimation meshAnim = anim.getMeshAnimation();
				List<MeshChannel> meshChannels = new LinkedList<MeshChannel>();
				
				for (com.jmex.model.ogrexml.anim.Track track : meshAnim.getTracks()) {
					if (!(track instanceof com.jmex.model.ogrexml.anim.PoseTrack)) {
						Logger.log("skipping none pose track " + track.getClass(), Logger.WARNING);
						continue;
					}
					com.jmex.model.ogrexml.anim.PoseTrack poseTrack = (com.jmex.model.ogrexml.anim.PoseTrack) track;
					if (poseTrack.getTimes().length == 0) {
						Logger.log("No frames in pose track for submesh " + poseTrack.getTargetMeshIndex() + ", skipping", Logger.WARNING);
						continue;
					}
					
					MeshChannel meshChannel = convertJMEMeshChannel(poseCache, poseTrack); 
					meshChannels.add(meshChannel); 
				}
				
				if (meshChannels.isEmpty()) {
					Logger.log("No pose tracks in mesh animation '" + meshAnim.getName() + "', skipping completely", Logger.WARNING);
					continue;
				}
				PoseClip poseClip = new PoseClip(objects.length, meshChannels);
				poseClip.setName(meshAnim.getName());
				poseClips.add(poseClip);
				
				Logger.log("Created pose animation clip: " + meshAnim.getName(), Logger.MESSAGE);
			}			
		}
		
		// finally rotate/scale skeleton. this should be done after jointChannels are rotated/scaled
		if ((skeleton != null) && (transform != null)) {
			if (rotation != null)
				skeleton.rotate(rotation);
			if (scale != 1)
				skeleton.scale(scale);
			
			currentPose.setToBindPose();
			currentPose.updateTransforms();
		}
		
		AnimatedGroup group = new AnimatedGroup(objects);
		if (!skinClips.isEmpty()) 
			group.setSkinClipSequence(new SkinClipSequence(skinClips));
		if (!poseClips.isEmpty()) 
			group.setPoseClipSequence(new PoseClipSequence(poseClips));
		
		return group;
	}
	
	private static boolean ordered(List<com.jmex.model.ogrexml.anim.Bone> bones) {
		for (int i = 0; i < bones.size(); i++) {
			com.jmex.model.ogrexml.anim.Bone bone = bones.get(i);
			com.jmex.model.ogrexml.anim.Bone parent = bone.getParent();
			
			while (parent != null) {
				int parentIndex = bones.indexOf(parent);
				assert (parentIndex != -1);
				if (parentIndex > i)
					return false;
				parent = parent.getParent();
			}
		}
		return true;
	} 
	
	private static Map<com.jmex.model.ogrexml.anim.Pose, MeshPose> createJMEPoseCache(MeshAnimationController controller, Matrix transform) {
    	Map<com.jmex.model.ogrexml.anim.Pose, MeshPose> poseCache = 
    		new IdentityHashMap<com.jmex.model.ogrexml.anim.Pose, MeshPose>();

		// first iterate through all pose animations to collect poses
		for (com.jmex.model.ogrexml.anim.Animation anim : controller.getAnimations()) {
			if (anim.hasMeshAnimation()) {
				com.jmex.model.ogrexml.anim.MeshAnimation meshAnim = anim.getMeshAnimation();
				
				for (com.jmex.model.ogrexml.anim.Track track : meshAnim.getTracks()) {
					if (!(track instanceof com.jmex.model.ogrexml.anim.PoseTrack)) {
						continue;
					}
					com.jmex.model.ogrexml.anim.PoseTrack poseTrack = (com.jmex.model.ogrexml.anim.PoseTrack) track;
					for (com.jmex.model.ogrexml.anim.PoseTrack.PoseFrame poseFrame : poseTrack.getFrames()) {
						for (com.jmex.model.ogrexml.anim.Pose jmePose : poseFrame.getPoses()) {
			    			MeshPose pose = poseCache.get(jmePose);
			    			if (pose == null) {
			    				pose = convertJMEMeshPose(jmePose);
			    				poseCache.put(jmePose, pose);
		
								if (transform != null)
									pose.applyTransform(transform);
			    			}
						}
					}
				}
			}			
		}
		return poseCache;
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
	
	
	private static Skeleton convertJMESkeleton(com.jmex.model.ogrexml.anim.Skeleton jmeSkeleton, int[] jointOrder) {
		Joint[] joints = new Joint[jmeSkeleton.getBoneCount()];
		
		for (int i = 0; i < joints.length; i++) {
			com.jmex.model.ogrexml.anim.Bone bone = jmeSkeleton.getBone(jointOrder[i]);
			
			final int parentIndex; 
			if (bone.getParent() == null) {
				parentIndex = Joint.NO_PARENT;  
			} else {
				int jmeParentIndex = jmeSkeleton.getBoneIndex(bone.getParent());
				parentIndex = indexOf(jointOrder, jmeParentIndex);
				assert (parentIndex != -1);
			}
			joints[i] = convertJMEJoint(joints, bone, i, parentIndex);
		}
		
		Skeleton skeleton = new Skeleton(joints);
		Logger.log(MessageFormat.format("Skeleton created out of jME OGRE skeleton, {0} joints", joints.length), Logger.MESSAGE);
		
		return skeleton;
	}
	
	private static Joint convertJMEJoint(Joint[] joints, 
			com.jmex.model.ogrexml.anim.Bone bone, int index, int parentIndex) {
		
		// local = bindPose x parentInvertBindPose
		// -> invertBindPose = (local x parentBindPose) -1
		
		com.jme.math.Vector3f tx = bone.getInitialPos();
		com.jme.math.Quaternion rot = bone.getInitialRot();
		
//		Matrix local = (bone.getParent() == null) ? new Matrix() : convertQuaternion(rot).getRotationMatrix(); 
		
		Matrix local = convertQuaternion(rot).getRotationMatrix();
		local.translate(tx.x, tx.y, tx.z);
		
		if (bone.getParent() != null) {
			local.matMul(joints[parentIndex].bindPose);
		}
		Matrix invertBindPose = local.invert();
		
		return new Joint(invertBindPose, index, parentIndex, bone.getName());
	}

	private static MeshData convertArdorMeshData(SkinnedMesh mesh) {
		final float[] coordinates = SkinHelper.asArray(mesh.getMeshData().getVertexBuffer()); 
		final float[] uvs = SkinHelper.asArray(mesh.getMeshData().getTextureBuffer(0));
		for (int i = 1; i < uvs.length; i += 2)
			uvs[i] = 1 - uvs[i];
		
		IntBuffer indexBuffer = mesh.getMeshData().getIndexBuffer();
		final int[] indices = (indexBuffer == null) ? null : SkinHelper.asArray(indexBuffer);
		
		return new MeshData(coordinates, uvs, indices);
	}
	
	private static MeshData convertJMEMeshData(OgreMesh mesh) {
		final float[] coordinates = SkinHelper.asArray(mesh.getVertexBuffer());
		float[] uvs = null;
		if ((mesh.getTextureCoords() == null) || mesh.getTextureCoords().isEmpty()) {
			Logger.log("Mesh has no texture coodinates", Logger.WARNING);
			uvs = null;
		} else {
			for (TexCoords texCoords : mesh.getTextureCoords()) {
				if (texCoords == null) {
					Logger.log("skipping null TexCoords", Logger.WARNING);
					continue;
				}
				uvs = SkinHelper.asArray(texCoords.coords);
			}
			if (uvs == null) {
				Logger.log("all " +  mesh.getTextureCoords().size() + " TexCoord(s) are null", Logger.WARNING);
			}
			//uvs = SkinHelper.asArray(mesh.getTextureCoords().get(0).coords);
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
     * @param jointOrder 
     * */
	private static JointChannel convertJMEJointChannel(com.jmex.model.ogrexml.anim.BoneTrack track, Skeleton skeleton, int[] jointOrder) {
		int jointIndex = indexOf(jointOrder, track.getTargetBoneIndex());
		int length = track.getTimes().length;

		float[] times = new float[length];
		Quaternion[] rotations = new Quaternion[length];
		SimpleVector[] translations = new SimpleVector[length];
		SimpleVector[] scales = new SimpleVector[length];
		
		// jME OGRE tracks are relative to joints, we need to take them to joint local space
		Joint joint = skeleton.getJoint(jointIndex);
		Joint parentJoint = joint.hasParent() ? 
				skeleton.getJoint(joint.getParentIndex()) : null;

		// there is no scale information in jME OGRE implementation
		final SimpleVector noScale = new SimpleVector(1f, 1f, 1f);
		
		for (int sampleIndex = 0; sampleIndex < length; sampleIndex++) {
			times[sampleIndex] = track.getTimes()[sampleIndex];
			scales[sampleIndex] = noScale; 
			
			Matrix m = convertQuaternion(track.getRotations()[sampleIndex]).getRotationMatrix();
			com.jme.math.Vector3f tx = track.getTranslations()[sampleIndex];

			m.matMul(joint.getBindPose()); // -> take to joint object space
			m.translate(tx.x, tx.y, tx.z);
			
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
	
	private static SkinClip convertJMESkinClip(Skeleton skeleton, com.jmex.model.ogrexml.anim.BoneAnimation boneAnimation, int[] jointOrder) {
		
		List<JointChannel> channels = new LinkedList<JointChannel>();
		for (com.jmex.model.ogrexml.anim.BoneTrack track : boneAnimation.getTracks()) {
			channels.add(convertJMEJointChannel(track, skeleton, jointOrder));
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

    private static SkinData convertJMESkinData(OgreMesh ogreMesh, int[] jointOrder) {
    	float[][] weights = SkinHelper.asArray(ogreMesh.getWeightBuffer().getWeights(), Skeleton.MAX_JOINTS_PER_VERTEX);
    	short[][] jointIndices = SkinHelper.asShortArray(ogreMesh.getWeightBuffer().getIndexes(), Skeleton.MAX_JOINTS_PER_VERTEX); 
    	
    	for (int i = 0; i < weights.length; i++) {
    		for (int j = 0; j < weights[i].length; j++) {
    			if (weights[i][j] == 0)
    				continue;
    			jointIndices[i][j] = (short)indexOf(jointOrder, jointIndices[i][j]);
    		}
    	}
    	
		return new SkinData(weights, jointIndices);
	}
	
    private static MeshChannel convertJMEMeshChannel(Map<com.jmex.model.ogrexml.anim.Pose, MeshPose> poseCache, com.jmex.model.ogrexml.anim.PoseTrack poseTrack) {
    	int length = poseTrack.getTimes().length;
    	
    	PoseFrame[] frames = new PoseFrame[length];
    	float[] times = new float[length];
    	
    	for (int i = 0; i < times.length; i++) {
    		
    		com.jmex.model.ogrexml.anim.PoseTrack.PoseFrame jmeFrame = poseTrack.getFrames()[i];
    		
    		MeshPose[] framePoses = new MeshPose[jmeFrame.getPoses().length];
    		
    		for (int j = 0; j < framePoses.length; j++) {
    			com.jmex.model.ogrexml.anim.Pose jmePose = jmeFrame.getPoses()[j];
    			MeshPose pose = poseCache.get(jmePose);
    			if (pose == null) 
    				throw new AssertionError("couldnt get MeshPose from jME PoseCache");
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
    private static Matrix convertArdorMatrix(Matrix4 m4) {
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
    private static Matrix getMatrix(ReadOnlyTransform transform) {
		return convertArdorMatrix(transform.getHomogeneousMatrix(null));
	}
	
	/** converts Ardor3D Vector3 to jPCT SimpleVector */
    private static SimpleVector convertArdorVector(com.ardor3d.math.Vector3 vector3) {
		return new SimpleVector(vector3.getXf(), vector3.getYf(), vector3.getZf());
	}
	
	/** converts jME Vector3f to jPCT SimpleVector */
    private static SimpleVector convertJMEVector(com.jme.math.Vector3f vector3) {
		return new SimpleVector(vector3.x, vector3.y, vector3.z);
	}
	
    /**
     * Constructs a new quaternion from ardor quaternion
     */
    private static Quaternion convertQuaternion(com.ardor3d.math.Quaternion quat) {
		return new Quaternion(quat.getXf(), quat.getYf(), quat.getZf(), quat.getWf());
    }

    /**
     * Constructs a new quaternion from jME quaternion
     */
    private static Quaternion convertQuaternion(com.jme.math.Quaternion quat) {
		return new Quaternion(quat.x, quat.y, quat.z, quat.w);
    }
	
	/** creates an ardor Transform out of given jPCT matrix */
    private static Transform getTransform(Matrix m) {
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
    
    private static int indexOf(int[] array, int value) {
    	for (int i = 0; i < array.length; i++) {
    		if (array[i] == value)
    			return i;
    	}
    	return -1;
    }

    private static void saveMeshData(MeshData meshData, String file) throws IOException {
    	FileOutputStream fos = new FileOutputStream(file);
    	try {
    		ObjectOutputStream out = new ObjectOutputStream(fos);
    		out.writeObject(meshData.coordinates);
    		out.writeObject(meshData.uvs);
    		out.writeObject(meshData.indices);
    		out.flush();
    		out.close();
    		System.out.println("saved mesh data to: " + file);
    	} finally {
    		fos.close();
    	}
    }
}

