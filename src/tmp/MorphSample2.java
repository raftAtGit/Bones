package tmp;

import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;

import raft.jpct.bones.Animated3D;
import raft.jpct.bones.AnimatedGroup;
import raft.jpct.bones.BonesImporter;
import bones.samples.AbstractSample;
import bones.samples.CameraOrbitController;

import com.jmex.model.ogrexml.OgreEntityNode;
import com.jmex.model.ogrexml.OgreLoader;
import com.threed.jpct.Logger;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;

public class MorphSample2 extends AbstractSample {

	private AnimatedGroup group;
	private CameraOrbitController cameraController;
	
	private long totalTime = 0;
	private float animLength = -100;
	private float seconds;
	
	@Override
	protected String getName() {
		return getClass().getName();
	}

	@Override
	protected void initialize() throws Exception {
		Logger.setLogLevel(Logger.LL_VERBOSE);
		Logger.setOnError(Logger.ON_ERROR_THROW_EXCEPTION);
		
		//File meshFile = new File("/home/raft/projects/karga-workspace/Temp/data/ogre/facial.mesh.xml");
		File meshFile = new File("/home/raft/projects/karga-workspace/Temp/data/ogre/Box_morph.mesh.xml");
		//File meshFile = new File("/home/raft/projects/karga-workspace/Temp/data/ogre/morph1.mesh.xml");
		
		URL url = meshFile.toURI().toURL();
		
		OgreLoader loader = new OgreLoader();
		OgreEntityNode node = loader.loadModel(url);

		TextureManager.getInstance().addTexture("eyes", new Texture(8, 8, Color.WHITE));
		TextureManager.getInstance().addTexture("teeth", new Texture(8, 8, Color.WHITE));
		TextureManager.getInstance().addTexture("glasses", new Texture(8, 8, Color.YELLOW));
		TextureManager.getInstance().addTexture("head", new Texture("/home/raft/projects/karga-workspace/Temp/data/ogre/Dr_Bunsen_Head.jpg"));
		
		this.group = BonesImporter.importOgre(node, 2f, null);
		group.addToWorld(world);
		for (Animated3D m : group) {
			//world.addObject(m);
//			if (m.getMeshChannel() != null && m.getMeshChannel().getTime() > animLength) {
//				animLength = m.getMeshChannel().getTime();
//				System.out.println("animTime: " + animLength);
//			}
			if (m.getPoseClipSequence() != null && m.getPoseClipSequence().getTime() > animLength) {
				animLength = m.getPoseClipSequence().getTime();
				System.out.println("animTime: " + animLength);
			}
		}
		
		group.setAutoApplyAnimation(false);
		
		//animLength = 2.5f;
//		group.get(0).setTexture("eyes");
//		group.get(1).setTexture("glasses");
//		group.get(3).setTexture("head");
//		group.get(4).setTexture("teeth");
//		
//		group.get(1).setTransparency(0);

		
		world.setAmbientLight(100, 100, 100);
		//world.addLight(new SimpleVector(0, 0, -400), Color.LIGHT_GRAY);
		world.buildAllObjects();
		
		for (Animated3D m : group) {
			m.setRotationPivot(SimpleVector.ORIGIN);
			m.rotateX((float)Math.PI);
		}
        cameraController = new CameraOrbitController(world.getCamera());
        cameraController.cameraTarget.set(0, -20, 0);
        cameraController.cameraRadius = 100;
        //cameraController.cameraAngle = 0; 
        
		renderPanel.addKeyListener(cameraController);
        
		renderPanel.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				switch (e.getKeyCode()) {
					case KeyEvent.VK_0:
					case KeyEvent.VK_1:
					case KeyEvent.VK_2:
					case KeyEvent.VK_3:
					case KeyEvent.VK_4:
					case KeyEvent.VK_5:
					case KeyEvent.VK_6:
					case KeyEvent.VK_7:
					case KeyEvent.VK_8:
					case KeyEvent.VK_9:
						toggleVisible(e.getKeyCode() - KeyEvent.VK_0);
						break;
				}
			}
		});
		
        update(0); // update once to reflect changes visible in first scene
		
	}

	protected void toggleVisible(int index) {
		if ((group.getSize() > 1) && (index < group.getSize()))
			group.get(index).setVisibility(!group.get(index).getVisibility());
	}
	
	@Override
	protected void update(long deltaTime) {
		totalTime += deltaTime;
        seconds += deltaTime / 1000f;
		
		if (seconds > animLength)
			seconds -= animLength;
		
//		for (Animated3D m : group) {
//			m.animatePose(seconds / animLength, 0);
//			m.animateSkin(seconds / animLength, 0);
//			m.applyAnimation();
//		}
		
			group.animatePose(seconds / animLength, 0);
			//group.animatePose(seconds / animLength, 0, 1);
			group.animateSkin(seconds / animLength, 0);
			group.applyAnimation();
			
		cameraController.placeCamera();
	}
	
	public static void main(String[] args) throws Exception {
		new MorphSample2().loop();
	}

}
