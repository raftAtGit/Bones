package bones.samples;

import java.awt.Color;
import java.io.File;
import java.net.URL;

import raft.jpct.bones.Animated3D;
import raft.jpct.bones.AnimatedGroup;
import raft.jpct.bones.BonesImporter;
import raft.jpct.bones.Quaternion;
import raft.jpct.bones.SkeletonDebugger;

import com.jmex.model.ogrexml.OgreEntityNode;
import com.jmex.model.ogrexml.OgreLoader;
import com.threed.jpct.Logger;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;

public class FacialAnimationSample extends AbstractSkinSample {

	@Override
	protected SkeletonDebugger createSkeletonDebugger() throws Exception {
		return null;
	}

	@Override
	protected AnimatedGroup createAnimatedGroup() throws Exception {
		Logger.setLogLevel(Logger.LL_VERBOSE);
		Logger.setOnError(Logger.ON_ERROR_THROW_EXCEPTION);
		
		File meshFile = new File("./samples/data/facial/facial.mesh.xml");
		
		URL url = meshFile.toURI().toURL();
		
		OgreLoader loader = new OgreLoader();
		OgreEntityNode node = loader.loadModel(url);

		// face in ogre file is upside down, so rotate around x axis
		AnimatedGroup group = BonesImporter.importOgre(node, 5f, new Quaternion().rotateX((float)Math.PI));
		//AnimatedGroup group = BonesIO.loadGroup(new FileInputStream("samples/data/facial/facial.group.bones"));
		
		TextureManager.getInstance().addTexture("eyes", new Texture(8, 8, Color.WHITE));
		TextureManager.getInstance().addTexture("teeth", new Texture(8, 8, Color.WHITE));
		TextureManager.getInstance().addTexture("glasses", new Texture(8, 8, Color.YELLOW));
		TextureManager.getInstance().addTexture("head", new Texture("./samples/data/facial/Dr_Bunsen_Head.jpg"));
		
		group.get(0).setTexture("eyes");
		group.get(1).setTexture("glasses");
		group.get(3).setTexture("head");
		group.get(4).setTexture("teeth");
		
		group.get(1).setTransparency(0);
		
		for (Animated3D m : group) {
			m.build();
		}
		
		return group;
	}
	
	@Override
	protected void initialize() throws Exception {
		super.initialize();
		
		world.setAmbientLight(100, 100, 100);
		
        update(0); // update once to reflect changes visible in first scene

        //cameraController.cameraAngle = 0f;
		autoAdjustCamera();
	}

	@Override
	protected String getName() {
		return getClass().getName();
	}
	
	public static void main(String[] args) throws Exception {
		new FacialAnimationSample().loop();
	}
	

}
