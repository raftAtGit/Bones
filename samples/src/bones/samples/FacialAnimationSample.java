package bones.samples;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

import raft.jpct.bones.Animated3D;
import raft.jpct.bones.AnimatedGroup;
import raft.jpct.bones.BonesIO;
import raft.jpct.bones.SkeletonDebugger;

import com.jmex.model.ogrexml.OgreEntityNode;
import com.jmex.model.ogrexml.OgreLoader;
import com.threed.jpct.Logger;
import com.threed.jpct.SimpleVector;
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
		
		File meshFile = new File("/home/raft/projects/karga-workspace/Temp/data/ogre/facial.mesh.xml");
		
		URL url = meshFile.toURI().toURL();
		
		OgreLoader loader = new OgreLoader();
		OgreEntityNode node = loader.loadModel(url);

		//AnimatedGroup group = BonesIO.loadOgre(node, 1f);
		AnimatedGroup group = BonesIO.loadGroup(new FileInputStream("samples/data/facial/facial.group.bones"));
		
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
			m.setRotationPivot(SimpleVector.ORIGIN);
			m.rotateX((float)Math.PI);
		}
		
		return group;
	}
	
	@Override
	protected void initialize() throws Exception {
		super.blendEnabled = false;
		super.initialize();
		
		world.setAmbientLight(100, 100, 100);
		
        update(0); // update once to reflect changes visible in first scene

        //cameraController.cameraAngle = 0f;
		autoAdjustCamera();
	}

	// we rotated group around X axis, so we need to adjust bounding box calculation accordingly
	@Override
	protected float[] calcBoundingBox() {
		float[] box = super.calcBoundingBox();
		
		float minY = -box[3];
		float maxY = -box[2];
		box[2] = minY;
		box[3] = maxY;
		
		return box;
	}
	
	@Override
	protected String getName() {
		return getClass().getName();
	}
	
	public static void main(String[] args) throws Exception {
		new FacialAnimationSample().loop();
	}
	

}
