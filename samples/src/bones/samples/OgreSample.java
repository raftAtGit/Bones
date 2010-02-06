package bones.samples;

import java.awt.Dimension;
import java.io.File;
import java.net.URL;

import raft.jpct.bones.Skeleton;
import raft.jpct.bones.SkinIO;
import raft.jpct.bones.Skinned3D;
import raft.jpct.bones.SkinnedGroup;
import raft.jpct.bones.Skeleton.Debugger;


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
	protected String getName() {
		return getClass().getName();
	}

	@Override
	protected Debugger createSkeletonDebugger() throws Exception {
		// in this file, root joint always remain in origin which doesn't look so good.
		// so we tell debugger to ignore root joint (0)
		return new Skeleton.Debugger(skinnedGroup.get(0).getCurrentPose(), 10f, (short)0);
	}

	@Override
	protected SkinnedGroup createSkinnedGroup() throws Exception {
		// we only specify the mesh file, skeleton file automatically loaded, and should be in same directory.  
		URL ninjaUrl = new File("./samples/data/ninja/ninja.mesh.xml").toURI().toURL();
		
		OgreLoader loader = new OgreLoader();
		OgreEntityNode node = loader.loadModel(ninjaUrl);

		SkinnedGroup skinnedGroup = SkinIO.loadOgreSkin(node, 1f);

		Texture texture = new Texture("./samples/data/ninja/nskingr.jpg");
		TextureManager.getInstance().addTexture("ninja", texture);
		
		for (Skinned3D o : skinnedGroup) {
			o.setTexture("ninja");
			o.build();
			o.discardSkeletonMesh();
		}
		return skinnedGroup;
	}

	
	@Override
	protected void initialize() throws Exception {
		super.initialize();
		
		world.setAmbientLight(255, 255, 255);
		
		// skeleton is oriented upside down, rotate it
		currentPose.getSkeleton().getTransform().rotateX((float)Math.PI);
		
        update(0); // update once to reflect changes visible in first scene

        // ninja is facing positive Z axis, so place camera accordingly
        cameraController.cameraAngle = 0f;
		autoAdjustCamera();
	}
	
	public static void main(String[] args) throws Exception {
		new OgreSample().loop();
	}

}
