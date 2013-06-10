package bones.samples;

import java.awt.Dimension;
import java.io.File;
import java.net.URL;

import raft.jpct.bones.Animated3D;
import raft.jpct.bones.AnimatedGroup;
import raft.jpct.bones.BonesImporter;
import raft.jpct.bones.Quaternion;
import raft.jpct.bones.SkeletonDebugger;

import com.jmex.model.ogrexml.OgreEntityNode;
import com.jmex.model.ogrexml.OgreLoader;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;

/**
 * <p>Demonstrates loading an Ogre3D skin.</p>
 *  
 * @author hakan eryargi (r a f t)
 * */
public class OgreSample extends AbstractSkinSample {

	public OgreSample() {
		super(new Dimension(1024, 768));
	}
	
	@Override
	protected SkeletonDebugger createSkeletonDebugger() throws Exception {
		// in this file, root joint always remain in origin which doesn't look so good.
		// so we tell debugger to ignore root joint (0)
		return new SkeletonDebugger(animatedGroup.get(0).getSkeletonPose(), 10f, 0.2f, 0);
	}

	@Override
	protected AnimatedGroup createAnimatedGroup() throws Exception {
		// we only specify the mesh file, skeleton file automatically loaded, and should be in same directory.  
		URL meshUrl = new File("./samples/data/ninja/ninja.mesh.xml").toURI().toURL();
		
		
		OgreLoader loader = new OgreLoader();
		OgreEntityNode node = loader.loadModel(meshUrl);

		// data in ogre file is upside down, so rotate around x axis
		AnimatedGroup animatedGroup = BonesImporter.importOgre(node, 2f, new Quaternion().rotateX((float)Math.PI));
	
		Texture texture = new Texture("./samples/data/ninja/nskingr.jpg");
		TextureManager.getInstance().addTexture("ninja", texture);
		
		for (Animated3D o : animatedGroup) {
			o.setTexture("ninja");
			o.build();
			o.discardMeshData();
		}
		return animatedGroup;
	}

	
	@Override
	protected void initialize() throws Exception {
		//animate = false;
		super.initialize();
		
		world.setAmbientLight(255, 255, 255);

        update(0); // update once to reflect changes visible in first scene

        // ninja is facing positive Z axis, so place camera accordingly
        cameraController.cameraAngle = 0f;
		autoAdjustCamera();
	}
	
//	@Override
//	protected void update(long deltaTime) {
//		cameraController.placeCamera();
//	}

	public static void main(String[] args) throws Exception {
		new OgreSample().loop();
	}

}
