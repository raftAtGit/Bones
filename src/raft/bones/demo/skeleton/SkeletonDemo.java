package raft.bones.demo.skeleton;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

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
import raft.jpct.bones.ClipSequence;
import raft.jpct.bones.SkinnedGroup;
import raft.jpct.bones.Skeleton;
import raft.jpct.bones.SkinIO;
import raft.jpct.bones.Skinned3D;

import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;

public class SkeletonDemo extends AbstractDemo {

	private SkinnedGroup skinnedGroup;
    private Skeleton.Pose currentPose;
    private Skeleton.Debugger skeletonDebugger;
	
    private ClipSequence clipSequence;
	
	private int animationSequence = 0;
	private float animationSpeed = 1f;
	private float animationIndex = 0f;
    
	private CameraOrbitController cameraController;
	
	public SkeletonDemo() {
	}

	@Override
	protected String getName() {
		return "Skeleton Demo";
	}
	
	@Override
	protected void initialize() throws Exception {
		this.skinnedGroup = loadArdorSkinGroup();

		Texture texture = new Texture(getClass().getResourceAsStream("skeleton_flipped.png"));
		TextureManager.getInstance().addTexture("skeleton", texture);
		
		world.setAmbientLight(255, 255, 255);

		for (Skinned3D o : skinnedGroup) {
			o.setTexture("skeleton");
			o.build();
			o.discardSkeletonMesh();
		}
		skinnedGroup.addToWorld(world);
		
		// all SkinnedObject3D share the same pose 
		this.currentPose = skinnedGroup.get(0).getCurrentPose();
		
		this.skeletonDebugger = new Skeleton.Debugger(skinnedGroup.get(0).getCurrentPose(), 10f);
		skeletonDebugger.addToWorld(world);
		
		this.clipSequence = skinnedGroup.getClipSequence();
		
		// skeleton is oriented upside down, rotate it
		currentPose.getSkeleton().getTransform().rotateX((float)Math.PI);

        cameraController = new CameraOrbitController(world.getCamera());
        cameraController.cameraTarget.set(0, -45, 0);
        cameraController.cameraRadius = 120;
		
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
        skeletonDebugger.update(currentPose);
	}


	private SkinnedGroup loadArdorSkinGroup() {
		
		ColladaImporter colladaImporter = new ColladaImporter().loadTextures(false);
		
		SkinnedGroup g1 = SkinIO.loadColladaSkin(colladaImporter.load("/raft/bones/demo/skeleton/skeleton.punch.dae"), 1f);
		SkinnedGroup g2 = SkinIO.loadColladaSkin(colladaImporter.load("/raft/bones/demo/skeleton/skeleton.walk.dae"), 1f);
		SkinnedGroup g3 = SkinIO.loadColladaSkin(colladaImporter.load("/raft/bones/demo/skeleton/skeleton.run.dae"), 1f);
		
		return SkinnedGroup.mergeSkin(g1, g2, g3);
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
		new SkeletonDemo().loop();
	}
}
