package bones.samples;

import java.io.File;
import java.net.URL;

import raft.jpct.bones.Animated3D;
import raft.jpct.bones.AnimatedGroup;
import raft.jpct.bones.BonesIO;
import raft.jpct.bones.BonesImporter;
import raft.jpct.bones.SkeletonDebugger;

import com.jmex.model.ogrexml.OgreEntityNode;
import com.jmex.model.ogrexml.OgreLoader;

public class AnimationBlendSample extends AbstractSkinSample {

	@Override
	protected AnimatedGroup createAnimatedGroup() throws Exception {
		// we only specify the mesh file, skeleton file automatically loaded, and should be in same directory.  
		URL ninjaUrl = new File("./samples/data/blend/Box_morph.mesh.xml").toURI().toURL();
		
		OgreLoader loader = new OgreLoader();
		OgreEntityNode node = loader.loadModel(ninjaUrl);

		AnimatedGroup skinnedGroup = BonesImporter.importOgre(node, 1f);

		for (Animated3D o : skinnedGroup) {
			o.build();
			o.discardMeshData();
		}
		return skinnedGroup;
	}

	@Override
	protected SkeletonDebugger createSkeletonDebugger() throws Exception {
		return new SkeletonDebugger(animatedGroup.get(0).getSkeletonPose(), 10f);
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
		new AnimationBlendSample().loop();
	}
	

}
