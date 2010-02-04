package raft.bones.demo.boy;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import raft.bones.demo.AbstractDemo;
import raft.bones.demo.CameraOrbitController;
import raft.bones.demo.ControlsPanel;
import raft.jpct.bones.Channel;
import raft.jpct.bones.Clip;
import raft.jpct.bones.ClipSequence;
import raft.jpct.bones.Quaternion;
import raft.jpct.bones.Skeleton;
import raft.jpct.bones.SkinIO;
import raft.jpct.bones.Skinned3D;
import raft.jpct.bones.SkinnedGroup;

import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.threed.jpct.Logger;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;

public class BoyDemo extends AbstractDemo {

	private SkinnedGroup skinnedGroup;
    private Skeleton.Pose currentPose;
    private Skeleton.Debugger skeletonDebugger;
	
    private ClipSequence clipSequence;
	
	private int animationSequence = 0;
	private float animationSpeed = 1f;
	private float animationIndex = 0f;
    
	private CameraOrbitController cameraController;
	
	public BoyDemo() {
	}

	@Override
	protected String getName() {
		return "Skeleton Demo";
	}
	
	@Override
	protected void initialize() throws Exception {
		this.skinnedGroup = loadArdorSkinGroup();

		Texture texture = new Texture(getClass().getResourceAsStream("body_flipped.jpg"));
		TextureManager.getInstance().addTexture("boy", texture);
		
		world.setAmbientLight(255, 255, 255);

		for (Skinned3D o : skinnedGroup) {
			o.setTexture("boy");
			o.build();
			o.discardSkeletonMesh();
		}
		skinnedGroup.addToWorld(world);
		
		// all SkinnedObject3D share the same pose 
		this.currentPose = skinnedGroup.get(0).getCurrentPose();

		appendTempClip();
		
		
		this.skeletonDebugger = new Skeleton.Debugger(skinnedGroup.get(0).getCurrentPose(), 10f);
		skeletonDebugger.addToWorld(world);
		
		this.clipSequence = skinnedGroup.getClipSequence();
		
		// skeleton is oriented upside down, rotate it
		//currentPose.getSkeleton().getTransform().rotateX((float)-Math.PI/2);

        cameraController = new CameraOrbitController(world.getCamera());
        cameraController.cameraTarget.set(0, 50, 0);
        cameraController.cameraRadius = 200;
		
		Rectangle bounds;
		
    	AnimationControlPanel animationPanel = new AnimationControlPanel();
		renderPanel.add(animationPanel);
		bounds = new Rectangle(animationPanel.getPreferredSize());
		bounds.translate(size.width - bounds.width - 10, 10);
		animationPanel.setBounds(bounds);
		
		ControlsPanel controlsPanel = new ControlsPanel(skinnedGroup, skeletonDebugger);
		renderPanel.add(controlsPanel);
		bounds = new Rectangle(controlsPanel.getPreferredSize());
		bounds.translate(10, 10);
		controlsPanel.setBounds(bounds);
		
		
		renderPanel.addKeyListener(cameraController);
		renderPanel.addKeyListener(controlsPanel);
		
        update(0); // update once to reflect changes visible in first scene
	}

	private void appendTempClip() {
		List<Channel> channels = new LinkedList<Channel>();
		for (Skeleton.Joint joint : currentPose.getSkeleton()) {
			
			Skeleton.Joint parent = joint.hasParent() ? currentPose.getSkeleton().getJoint(joint.getParentIndex()) : null;
			
			float[] times = {1,2,3};
			SimpleVector[] translations = {new SimpleVector(), new SimpleVector(), new SimpleVector()}; 
			Quaternion[] rotations = {new Quaternion(), new Quaternion().rotateY(0.5f), new Quaternion()}; 
			SimpleVector[] scales = {new SimpleVector(1,1,1), new SimpleVector(1,1,1), new SimpleVector(1,1,1)};
			
			for (SimpleVector t : translations) {
				t.matMul(joint.getBindPose());
				if (joint.hasParent()) {
					t.matMul(parent.getInverseBindPose());
				}
			}
			for (Quaternion q : rotations) {
				q.rotate(joint.getBindPose());
				if (joint.hasParent()) {
					q.rotate(parent.getInverseBindPose());
				}
			}
			
			
			channels.add(new Channel(joint.getIndex(), times, translations, rotations, scales));
		}
		Clip clip = new Clip(currentPose.getSkeleton(), channels.toArray(new Channel[channels.size()]));
		ClipSequence sequence = new ClipSequence(clip); 
		//skinnedGroup.setClipSequence(ClipSequence.merge(skinnedGroup.getClipSequence(), sequence));
		//skinnedGroup.setClipSequence(sequence);
	}

	@Override
	protected void update(long deltaTime) {
		cameraController.placeCamera();
		
		float clipTime = (animationSequence == 0) 
			? clipSequence.getTime() // whole animation 
			: clipSequence.getClip(animationSequence - 1).getTime(); // single clip 

		animationIndex += deltaTime * animationSpeed / clipTime / 1000;
		while (animationIndex > 1)
			animationIndex -= 1;
			
		skinnedGroup.animateSkin(animationIndex, animationSequence);
		
		currentPose.setToBindPose();
		currentPose.updateTransforms();
        skinnedGroup.applyPose();
		
        skeletonDebugger.update(currentPose);
		
//        for (int i = 0; i < currentPose.getSkeleton().getNumberOfJoints(); i++) {
//        	Skeleton.Joint joint = currentPose.getSkeleton().getJoint(i); 
//            System.out.println(joint.getName() + "\t:" + currentPose.getGlobal(i).getTranslation());
//        }
        
	}


	private SkinnedGroup loadArdorSkinGroup() {
		
		ColladaImporter colladaImporter = new ColladaImporter().loadTextures(false);

		SkinnedGroup g1 = SkinIO.loadColladaSkin(colladaImporter.load("/raft/bones/demo/boy/Box01.dae"), 1f);
		
//		SkinnedGroup g1 = SkinIO.loadArdorSkinGroup(colladaImporter.load("/raft/bones/demo/boy/man_walk.DAE"));
//		SkinnedGroup g2 = SkinIO.loadArdorSkinGroup(colladaImporter.load("/raft/bones/demo/skeleton/skeleton.walk.dae"));
//		SkinnedGroup g3 = SkinIO.loadArdorSkinGroup(colladaImporter.load("/raft/bones/demo/skeleton/skeleton.run.dae"));
//		
//		return SkinnedGroup.mergeSkin(g1, g2, g3);
		return g1;
	}
	
	@SuppressWarnings("serial")
	private class AnimationControlPanel extends JPanel {
		
		private AnimationControlPanel() {
			setLayout(new BorderLayout());
			setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			
			Hashtable<Integer, JComponent> labels = new Hashtable<Integer, JComponent>();
			labels.put(0, new JLabel("Stop"));
			labels.put(100, new JLabel("Normal"));
			labels.put(300, new JLabel("Fast"));
			
			final JSlider speedSlider = new JSlider(JSlider.HORIZONTAL, 0, 300, 100);
			speedSlider.setPaintLabels(true);
			speedSlider.setLabelTable(labels);
			speedSlider.setPaintTicks(true);
			speedSlider.setMajorTickSpacing(100);
			
			speedSlider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					animationSpeed = speedSlider.getValue() / 100f;
				}
			});
			
			ButtonGroup animationGroup = new ButtonGroup();
			
			JPanel animationPanel = new JPanel();
			animationPanel.setBorder(BorderFactory.createTitledBorder("Animation"));
			animationPanel.setLayout(new BoxLayout(animationPanel, BoxLayout.X_AXIS));
			
			addAnimationButton("All", animationGroup, animationPanel, 0, true);
			addAnimationButton("Punch", animationGroup, animationPanel, 1, false);
			addAnimationButton("Walk", animationGroup, animationPanel, 2, false);
			addAnimationButton("Run", animationGroup, animationPanel, 3, false);
			
			add(speedSlider, BorderLayout.SOUTH);
			add(animationPanel, BorderLayout.CENTER);
		}
		
		private void addAnimationButton(String text, ButtonGroup group, JComponent container, final int animation, boolean selected) {
			JRadioButton button = new JRadioButton(text);
			group.add(button);
			container.add(button);
			group.setSelected(button.getModel(), selected);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					animationSequence = animation;
					animationIndex = 0;
				}
			});
		}
	}
	
	public static void main(String[] args) throws Exception {
		Logger.setOnError(Logger.ON_ERROR_THROW_EXCEPTION);
		new BoyDemo().loop();
	}
}
