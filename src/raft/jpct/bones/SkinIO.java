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
import com.ardor3d.extension.model.collada.jdom.data.SkinData;
import com.jmex.model.ogrexml.OgreEntityNode;
import com.jmex.model.ogrexml.anim.MeshAnimationController;
import com.jmex.model.ogrexml.anim.OgreMesh;
import com.threed.jpct.Logger;

/** 
 * <p>Contains skin loading and saving methods.</p> 
 * 
 * @author hakan eryargi (r a f t)
 * */
public class SkinIO {

	private static final String HEADER_GROUP = "Bones-Group";
	private static final String HEADER_OBJECT = "Bones-Object";
	private static final short VERSION = 1;
	
	/** can not be instantiated */
	private SkinIO() {}
	
	
	/** 
	 * <p>Constructs a {@link SkinnedGroup} out of Ardor's skinData.</p>
	 * 
	 * @param colladaStorage Ardor collada storage
	 * @param scale the scale. not used at the moment.
	 * 
	 * @see SkinnedGroup
	 * */
	public static SkinnedGroup loadColladaSkin(ColladaStorage colladaStorage, float scale) {
		if (colladaStorage.getSkins().isEmpty())
			throw new IllegalArgumentException("ColladaStorage contains no skins.");
		
		Skeleton skeleton = new Skeleton(findUniqueArdorSkeleton(colladaStorage));
		
		Skeleton.Pose currentPose = new Skeleton.Pose(skeleton);
		currentPose.updateTransforms();
		
		List<Skinned3D> objects = new LinkedList<Skinned3D>();

		for (SkinData skinData : colladaStorage.getSkins()) {
			for (SkinnedMesh sm : skinData.getSkins()) {
				Skeleton.Skin skin = new Skeleton.Skin(sm);
				Skeleton.Mesh mesh = new Skeleton.Mesh(sm);
				Skinned3D skinnedObject = new Skinned3D(mesh, skin, currentPose);
				objects.add(skinnedObject);
			}
		}
		
		SkinnedGroup group = new SkinnedGroup(objects.toArray(new Skinned3D[objects.size()]));
		
		List<JointChannel> jointChannels = colladaStorage.getJointChannels();
		if ((jointChannels != null && !jointChannels.isEmpty())) {
			Clip clip = new Clip(skeleton, jointChannels);
			group.setClipSequence(new ClipSequence(clip));
		}
		return group;
	}
	
	/** 
	 * <p>Saves given skin group to given stream.</p>
	 * @see SkinnedGroup 
	 * */
	public static void saveGroup(SkinnedGroup group, OutputStream out) throws IOException {
		ObjectOutputStream oout = new ObjectOutputStream(out);
		writeHeader(oout, HEADER_GROUP);
		group.writeToStream(oout);
		oout.flush();
	}
	
	/** 
	 * <p>Saves given {@link Skinned3D} to given stream.</p> 
	 * @see Skinned3D 
	 * */
	public static void saveObject(Skinned3D object, OutputStream out) throws IOException {
		ObjectOutputStream oout = new ObjectOutputStream(out);
		writeHeader(oout, HEADER_OBJECT);
		object.writeToStream(oout);
		oout.flush();
	}
	
	/** 
	 * <p>Loads an array of {@link Skinned3D}'s from given stream. Objects should be saved to stream
	 * via {@link #saveObject(Skinned3D, OutputStream)}</p>
	 *  
	 * @see Skinned3D
	 * @see #saveObject(Skinned3D, OutputStream) 
	 * */
	public static Skinned3D loadObject(InputStream in) throws IOException, ClassNotFoundException {
		ObjectInputStream oin = new ObjectInputStream(in);
		readHeader(oin, HEADER_OBJECT);
		return new Skinned3D(oin);
	}
	
	/** 
	 * <p>Loads a {@link SkinnedGroup}'s from given stream. Group should be saved to stream
	 * via {@link #saveGroup(SkinnedGroup, OutputStream)}</p>
	 *  
	 * @see SkinnedGroup
	 * @see #saveGroup(SkinnedGroup, OutputStream) 
	 * */
	public static SkinnedGroup loadGroup(InputStream in) throws IOException, ClassNotFoundException {
		ObjectInputStream oin = new ObjectInputStream(in);
		readHeader(oin, HEADER_GROUP);
		return new SkinnedGroup(oin);
	}

	/** 
	 * <p>Constructs a {@link SkinnedGroup} out of jME OGRE data.</p>
	 * 
	 * @param node jME OgreEntityNode
	 * @param scale the scale. not used at the moment.
	 * */
	public static SkinnedGroup loadOgreSkin(OgreEntityNode node, float scale) throws IOException {
		if (node.getControllerCount() == 0)
			throw new IllegalArgumentException("No controller found in OgreEntityNode. Means there is no skeleton!");
		
		MeshAnimationController controller = (MeshAnimationController) node.getController(0);

		Skeleton skeleton = new Skeleton(controller.getSkeleton());
		
		Skeleton.Pose currentPose = new Skeleton.Pose(skeleton);
		currentPose.updateTransforms();
		
		List<Skinned3D> list = new LinkedList<Skinned3D>();
		
		for (OgreMesh ogreMesh : controller.getMeshList()) {
			Skeleton.Skin skin = new Skeleton.Skin(ogreMesh);
			Skeleton.Mesh mesh = new Skeleton.Mesh(ogreMesh);

			Skinned3D skinnedObject = new Skinned3D(mesh, skin, currentPose);
			list.add(skinnedObject);
		}
		
		Skinned3D[] skinnedObjects = list.toArray(new Skinned3D[list.size()]);
		
		List<Clip> clips = new LinkedList<Clip>();
		
		for (com.jmex.model.ogrexml.anim.Animation anim : controller.getAnimations()) {
			if (!anim.hasBoneAnimation()) {
				Logger.log("skipping none bone animation " + anim.getName(), Logger.WARNING);
				continue;
			}
			com.jmex.model.ogrexml.anim.BoneAnimation boneAnim = anim.getBoneAnimation();
			clips.add(new Clip(skeleton, boneAnim));
		}
		
		if (clips.isEmpty()) {
			return new SkinnedGroup(skinnedObjects);
		} else {
			return new SkinnedGroup(skinnedObjects, new ClipSequence(clips));
		}
	}
	
	private static com.ardor3d.extension.animation.skeletal.Skeleton findUniqueArdorSkeleton(ColladaStorage colladaStorage) {
		if (colladaStorage.getSkins().isEmpty())
			throw new IllegalArgumentException("ColladaStorage contains no skins.");
		
		com.ardor3d.extension.animation.skeletal.Skeleton skeleton = null;
		
		for (SkinData skin : colladaStorage.getSkins()) {
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
