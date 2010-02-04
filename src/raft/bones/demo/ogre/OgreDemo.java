package raft.bones.demo.ogre;

import java.awt.Dimension;
import java.io.File;
import java.net.URL;

import raft.bones.demo.AbstractSkinDemo;
import raft.jpct.bones.Skeleton;
import raft.jpct.bones.SkinIO;
import raft.jpct.bones.Skinned3D;
import raft.jpct.bones.SkinnedGroup;
import raft.jpct.bones.Skeleton.Debugger;

import com.jmex.model.ogrexml.OgreEntityNode;
import com.jmex.model.ogrexml.OgreLoader;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;

public class OgreDemo extends AbstractSkinDemo {

	public OgreDemo() {
		super(new Dimension(1024, 768));
	}
	
	@Override
	protected String getName() {
		return getClass().getName();
	}

	@Override
	protected Debugger createSkeletonDebugger() throws Exception {
		return new Skeleton.Debugger(skinnedGroup.get(0).getCurrentPose(), 10f, (short)0);
	}

	@Override
	protected SkinnedGroup createSkinnedGroup() throws Exception {
		URL ninjaUrl = getClass().getResource("/jmetest/data/model/ogrexml/ninja.mesh.xml");
		URL cyberHumanoidUrl = new File("D:/raft/eclipse/ardor/skeletal/SkeletalExample/Model Examples/Humanoid/Cube.001.mesh.xml").toURI().toURL();
		URL cyberWormyUrl = new File("D:/raft/eclipse/ardor/skeletal/SkeletalExample/Model Examples/Worm Thingy/Cube.mesh.xml").toURI().toURL();
		URL boxUrl = new File("C:/Users/raft/Documents/3dsMaxDesign/export/Box01.mesh.xml").toURI().toURL();
		
		OgreLoader loader = new OgreLoader();
		OgreEntityNode node = loader.loadModel(ninjaUrl, null);

		SkinnedGroup skinnedGroup = SkinIO.loadOgreSkin(node, 1f);

		Texture texture = new Texture(getClass().getResourceAsStream("/jmetest/data/model/ogrexml/nskingr.jpg"));
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
		
        cameraController.cameraTarget.set(0, -85, 0);
        cameraController.cameraRadius = 300;
		
		// skeleton is oriented upside down, rotate it
		currentPose.getSkeleton().getTransform().rotateX((float)Math.PI);
        update(0); // update once to reflect changes visible in first scene
	}
	
	public static void main(String[] args) throws Exception {
		new OgreDemo().loop();
	}

}
