package raft.jpct.bones;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import com.ardor3d.extension.animation.skeletal.JointChannel;
import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.jmex.model.ogrexml.OgreEntityNode;
import com.jmex.model.ogrexml.anim.MeshAnimationController;
import com.jmex.model.ogrexml.anim.OgreMesh;
import com.threed.jpct.Logger;

/** 
 * <p>Contains static loader and saver methods.</p> 
 * 
 * @author hakan eryargi (r a f t)
 * */
public class BonesIO {

	private static final String HEADER_GROUP = "Bones-Group";
	private static final String HEADER_OBJECT = "Bones-Object";
	private static final short VERSION = 2;
	
	/** can not be instantiated */
	private BonesIO() {}
	
	
	/** 
	 * <p>Constructs a {@link AnimatedGroup} out of Ardor's skinData.</p>
	 * 
	 * @param colladaStorage Ardor collada storage
	 * @param scale the scale. not used at the moment.
	 * 
	 * @see AnimatedGroup
	 * */
	public static AnimatedGroup loadCollada(ColladaStorage colladaStorage, float scale) { 
		if (colladaStorage.getSkins().isEmpty())
			throw new IllegalArgumentException("ColladaStorage contains no skins.");
		
		if (scale != 1f)
			Logger.log("Scale is not supported at the moment, ignoring", Logger.WARNING);
		
		Skeleton skeleton = new Skeleton(findUniqueArdorSkeleton(colladaStorage));
		
		SkeletonPose currentPose = new SkeletonPose(skeleton);
		currentPose.updateTransforms();
		
		List<Animated3D> objects = new LinkedList<Animated3D>();

		for (com.ardor3d.extension.model.collada.jdom.data.SkinData skinData : colladaStorage.getSkins()) {
			for (SkinnedMesh sm : skinData.getSkins()) {
				SkinData skin = new SkinData(sm);
				MeshData mesh = new MeshData(sm);
				Animated3D skinnedObject = new Animated3D(mesh, skin, currentPose);
				objects.add(skinnedObject);
			}
		}
		
		AnimatedGroup group = new AnimatedGroup(objects.toArray(new Animated3D[objects.size()]));
		
		List<JointChannel> jointChannels = colladaStorage.getJointChannels();
		if ((jointChannels != null && !jointChannels.isEmpty())) {
			SkinClip clip = new SkinClip(skeleton, jointChannels);
			group.setSkinClipSequence(new SkinClipSequence(clip));
			Logger.log("Created one animation clip", Logger.MESSAGE);
		}
		return group;
	}
	
	/** 
	 * <p>Saves given skin group to given stream.</p>
	 * @see AnimatedGroup 
	 * */
	public static void saveGroup(AnimatedGroup group, OutputStream out) throws IOException {
		ObjectOutputStream oout = new ObjectOutputStream(out);
		writeHeader(oout, HEADER_GROUP);
		group.writeToStream(oout);
		oout.flush();
	}
	
	/** 
	 * <p>Saves given {@link Animated3D} to given stream.</p> 
	 * @see Animated3D 
	 * */
	public static void saveObject(Animated3D object, OutputStream out) throws IOException {
		ObjectOutputStream oout = new ObjectOutputStream(out);
		writeHeader(oout, HEADER_OBJECT);
		object.writeToStream(oout);
		oout.flush();
	}
	
	/** 
	 * <p>Loads an array of {@link Animated3D}'s from given stream. Objects should be saved to stream
	 * via {@link #saveObject(Animated3D, OutputStream)}</p>
	 *  
	 * @see Animated3D
	 * @see #saveObject(Animated3D, OutputStream) 
	 * */
	public static Animated3D loadObject(InputStream in) throws IOException, ClassNotFoundException {
		ObjectInputStream oin = new ObjectInputStream(in);
		readHeader(oin, HEADER_OBJECT);
		return new Animated3D(oin);
	}
	
	/** 
	 * <p>Loads a {@link AnimatedGroup}'s from given stream. Group should be saved to stream
	 * via {@link #saveGroup(AnimatedGroup, OutputStream)}</p>
	 *  
	 * @see AnimatedGroup
	 * @see #saveGroup(AnimatedGroup, OutputStream) 
	 * */
	public static AnimatedGroup loadGroup(InputStream in) throws IOException, ClassNotFoundException {
		ObjectInputStream oin = new ObjectInputStream(in);
		readHeader(oin, HEADER_GROUP);
		return new AnimatedGroup(oin);
	}

	/** 
	 * <p>Constructs a {@link AnimatedGroup} out of jME OGRE data.</p>
	 * 
	 * @param node jME OgreEntityNode
	 * @param scale the scale. not used at the moment.
	 * */
	public static AnimatedGroup loadOgre(OgreEntityNode node, float scale) throws IOException {
		if (node.getControllerCount() == 0)
			throw new IllegalArgumentException("No controller found in OgreEntityNode. Means there is no skeleton or pose animation!");
		
		if (scale != 1f)
			Logger.log("Scale is not supported at the moment, ignoring", Logger.WARNING);
		
		MeshAnimationController controller = (MeshAnimationController) node.getController(0);

		Skeleton skeleton = null;
		SkeletonPose currentPose = null;
		
		if (controller.getSkeleton() != null) {
			skeleton = new Skeleton(controller.getSkeleton());
			
			currentPose = new SkeletonPose(skeleton);
			currentPose.updateTransforms();
		}
		
		
		List<Animated3D> list = new LinkedList<Animated3D>();
		
		int index = 0;
		for (OgreMesh ogreMesh : controller.getMeshList()) {
			SkinData skin = (skeleton == null) ? null : new SkinData(ogreMesh);
			MeshData mesh = new MeshData(ogreMesh);

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
				skeletonClips.add(new SkinClip(skeleton, boneAnim));
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
					poseChannels.add(new MeshChannel(poseTrack)); 
					
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

	private static void writeHeader(java.io.ObjectOutputStream out, String header) throws IOException {
		out.writeUTF(header);
		out.writeShort(VERSION);
	}
	
	private static void readHeader(java.io.ObjectInputStream in, String requiredHeader) throws IOException {
		String header = in.readUTF();
		if (!requiredHeader.equals(header))
			throw new IOException("Invalid header: " + header);
		short version = in.readShort();
		if (VERSION != version)
			throw new IOException("Version mismatch. Current version: " + VERSION + ", stream version: " + version);
	}

}
