package bones.samples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import raft.jpct.bones.Animated3D;
import raft.jpct.bones.AnimatedGroup;
import raft.jpct.bones.BonesImporter;
import raft.jpct.bones.PoseClip;
import raft.jpct.bones.PoseClipSequence;
import raft.jpct.bones.Quaternion;
import raft.jpct.bones.SkeletonDebugger;
import raft.jpct.bones.SkeletonPose;
import raft.jpct.bones.SkinClip;
import raft.jpct.bones.SkinClipSequence;

import com.jmex.model.ogrexml.OgreEntityNode;
import com.jmex.model.ogrexml.OgreLoader;
import com.threed.jpct.Camera;
import com.threed.jpct.Config;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Interact2D;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;

public class AnimationBlendSample extends AbstractSample {

	private static final int SKIN_BIND_POSE = -1;
	private static final int SKIN_ANIMATE_ALL = 0;
	
	private static final boolean ANIM_SKIN = true;
	private static final boolean ANIM_POSE = false;
	
	private AnimatedGroup animatedGroup;
	private SkeletonPose currentPose;
	private SkeletonDebugger skeletonDebugger;
	
	private SkinClipSequence skinClipSequence;
	private PoseClipSequence poseClipSequence;
	
	private boolean animate = true;
	private int skinAnimationSequence = SKIN_BIND_POSE;
	private float skinAnimationSpeed = 1f;
	private float skinAnimationIndex = 0f;
    
	private final Map<Integer, PoseState> poseAnimStates = new TreeMap<Integer, PoseState>();
	
	private boolean showMesh = true;
	private boolean showSkeleton = false;
	
	private boolean hasSkinAnimation = false;
	private boolean hasPoseAnimation = false;
	
	private CameraOrbitController cameraController;
	
	protected AnimatedGroup createAnimatedGroup() throws Exception {
		// we only specify the mesh file, skeleton file automatically loaded, and should be in same directory.  
		URL meshUrl = new File("./samples/data/blend/cylinder.mesh.xml").toURI().toURL();
//		URL meshUrl = new File("/home/raft/tmp2/bones_dog/dog.mesh.xml").toURI().toURL();
		
		OgreLoader loader = new OgreLoader();
		OgreEntityNode node = loader.loadModel(meshUrl);

		AnimatedGroup group = BonesImporter.importOgre(node, 2f, new Quaternion().rotateX((float)Math.PI));
		// we will use blending, so we need to disable auto applying 
		group.setAutoApplyAnimation(false);

		TextureManager.getInstance().addTexture("stone", new Texture("./samples/data/blend/stone.jpg"));
		
		for (Animated3D o : group) {
			o.setTexture("stone");
			o.build();
			o.discardMeshData();
		}
		return group;
	}

	@Override
	protected void initialize() throws Exception {
		world.setAmbientLight(255, 255, 255);
		
		this.animatedGroup = createAnimatedGroup();
		
		this.skinClipSequence = animatedGroup.getSkinClipSequence();
		this.poseClipSequence = animatedGroup.getPoseClipSequence();
		
		this.hasSkinAnimation = (skinClipSequence != null) && (skinClipSequence.getSize() != 0);
		this.hasPoseAnimation = (poseClipSequence != null) && (poseClipSequence.getSize() != 0);
		
		this.skinAnimationSequence = hasSkinAnimation ? -1 : 0;
		
		animatedGroup.addToWorld(world);
		
		// all SkinnedObject3D share the same pose 
		this.currentPose = animatedGroup.get(0).getSkeletonPose();
		
		this.skeletonDebugger = new SkeletonDebugger(animatedGroup.get(0).getSkeletonPose(), 5f, 2f);
		if (skeletonDebugger != null) {
			skeletonDebugger.addToWorld(world);
			skeletonDebugger.setVisibility(showSkeleton);
		}
		
        cameraController = new CameraOrbitController(world.getCamera());
        float[] bb = calcBoundingBox();
        float height = bb[3] - bb[2];
        cameraController.dragMovePerPixel = height / frameBuffer.getOutputHeight();
        Config.farPlane = Math.max(height * 100, Config.farPlane);
		
		for (Animated3D o : animatedGroup) {
			o.setVisibility(showMesh);
		}

		renderPanel.addKeyListener(cameraController);
		renderPanel.addMouseListener(cameraController);
		renderPanel.addMouseMotionListener(cameraController);
		renderPanel.addMouseWheelListener(cameraController);
		
		createGUI();
		
        update(0); // update once to reflect changes visible in first scene
		
		autoAdjustCamera();
	}
	
	@Override
	protected void update(long deltaTime) {
		cameraController.placeCamera();
		
		if (!animate)
			return;
		if (!hasPoseAnimation && !hasSkinAnimation)
			return;
		
		
		for (PoseState state : poseAnimStates.values()) {
			if (!state.enabled)
				continue;
			
			float clipTime = getClipTime(state.clipNo, ANIM_POSE); 
			state.animationIndex += deltaTime * state.speed / clipTime / 1000;
			while (state.animationIndex > 1) {
				state.animationIndex -= 1;
			}
			
			animatedGroup.animatePose(state.animationIndex, state.clipNo);
		}
		
		
		switch (skinAnimationSequence) {
			case SKIN_BIND_POSE:
				currentPose.setToBindPose();
				currentPose.updateTransforms();
				animatedGroup.applySkeletonPose();
				break;
			default:
				float clipTime = getClipTime(skinAnimationSequence, ANIM_SKIN); 
				skinAnimationIndex += deltaTime * skinAnimationSpeed / clipTime / 1000;
				while (skinAnimationIndex > 1) {
					skinAnimationIndex -= 1;
				}
				
				animatedGroup.animateSkin(skinAnimationIndex, skinAnimationSequence);
				break;
		}
		 		
		animatedGroup.applyAnimation();
		skeletonDebugger.update(currentPose);
		
	}
	
	private float getClipTime(int sequence, boolean skinAnim) {
		if (skinAnim) {
			return (sequence == 0) 
				? skinClipSequence.getTime() // whole animation 
				: skinClipSequence.getClip(sequence - 1).getTime(); // single clip 
		} else {
			return (sequence == 0) 
				? poseClipSequence.getTime() // whole animation 
				: poseClipSequence.getClip(sequence - 1).getTime(); // single clip 
		}
	}
	
	
	
	/** adjusts camera based on current mesh of skinned group. 
	 * camera looks at mid point of height and placed at a distance 
	 * such that group height occupies 2/3 of screen height. */
	private void autoAdjustCamera() {
		float[] bb = calcBoundingBox();
		float groupHeight = bb[3] - bb[2];
        cameraController.cameraRadius = calcDistance(world.getCamera(), frameBuffer, 
        		frameBuffer.getOutputHeight() / 1.5f , groupHeight);
        cameraController.minCameraRadius = groupHeight / 10f;
        cameraController.cameraTarget.y = (bb[3] + bb[2]) / 2; 
        cameraController.placeCamera();
	}

	
	/** calculates and returns whole bounding box of skinned group */
	private float[] calcBoundingBox() {
		float[] box = null;
		
		for (Animated3D skin : animatedGroup) {
			float[] skinBB = skin.getMesh().getBoundingBox();
			
			if (box == null) {
				box = skinBB;
			} else {
				// x
				box[0] = Math.min(box[0], skinBB[0]);
				box[1] = Math.max(box[1], skinBB[1]);
				// y
				box[2] = Math.min(box[2], skinBB[2]);
				box[3] = Math.max(box[3], skinBB[3]);
				// z
				box[4] = Math.min(box[4], skinBB[4]);
				box[5] = Math.max(box[5], skinBB[5]);
			}
		}
		return box;
	}
	
    /** 
     * calculates a camera distance to make object look height pixels on screen 
     * @author EgonOlsen 
     * */
    private float calcDistance(Camera c, FrameBuffer buffer, float height, float objectHeight) {
        float h = height / 2f;
        float os = objectHeight / 2f;

        Camera cam = new Camera();
        cam.setFOV(c.getFOV());
        SimpleVector p1 = Interact2D.project3D2D(cam, buffer, new SimpleVector(0f, os, 1f));
        switch (buffer.getSamplingMode()) {
            case FrameBuffer.SAMPLINGMODE_OGUS:     p1.y /= 0.5f; break;
            case FrameBuffer.SAMPLINGMODE_NORMAL:   break;
            case FrameBuffer.SAMPLINGMODE_OGSS_FAST:p1.y /= 1.5f; break;
            case FrameBuffer.SAMPLINGMODE_OGSS:     p1.y /= 2f; break;
            default:
                throw new AssertionError("sampling: " + buffer.getSamplingMode());
        }
        float y1 = p1.y - buffer.getMiddleY();
        float z = (1f/h) * y1;

        return z;
    }
	
	private void toggleVisible(int index) {
		if ((animatedGroup.getSize() > 1) && (index < animatedGroup.getSize()))
			animatedGroup.get(index).setVisibility(!animatedGroup.get(index).getVisibility());
	}
    
	private void createGUI() {
		Rectangle bounds;
		
    	AnimationControlPanel animationPanel = new AnimationControlPanel();
		renderPanel.add(animationPanel);
		bounds = new Rectangle(animationPanel.getPreferredSize());
		bounds.translate(size.width - bounds.width - 10, 10);
		animationPanel.setBounds(bounds);
		
        
		ControlsPanel controlsPanel = new ControlsPanel();
		renderPanel.add(controlsPanel);
		bounds = new Rectangle(controlsPanel.getPreferredSize());
		bounds.translate(10, 10);
		controlsPanel.setBounds(bounds);
	}

	@SuppressWarnings("serial")
	protected class ControlsPanel extends JPanel {
		
		protected ControlsPanel() {
			
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			
			add(createLabel("Arrow keys, A,Z to move camera", Color.RED));
			
			final JCheckBox showSkeletonCheckBox = new JCheckBox("Show skeleton (s)", showSkeleton);
			showSkeletonCheckBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					showSkeleton = showSkeletonCheckBox.isSelected();
					skeletonDebugger.setVisibility(showSkeleton);
				}
			});
			add(showSkeletonCheckBox);
			
			final JCheckBox showMeshCheckBox = new JCheckBox("Show mesh (m)", showMesh);
			showMeshCheckBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					showMesh = showMeshCheckBox.isSelected();
					for (Animated3D o : animatedGroup) {
						o.setVisibility(showMesh);
					}
				}
			});
			add(showMeshCheckBox);
			
			final JCheckBox textureCheckBox = new JCheckBox("Draw textures", drawTextures);
			textureCheckBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					drawTextures = textureCheckBox.isSelected();
				}
			});
			add(textureCheckBox);
			
			final JCheckBox wireframeCheckBox = new JCheckBox("Draw wireframe", drawWireFrame);
			wireframeCheckBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					drawWireFrame = wireframeCheckBox.isSelected();
				}
			});
			add(wireframeCheckBox);
			
			if (animatedGroup.getSize() > 1) {
				JPanel subMeshPanel = new JPanel();
				subMeshPanel.setBorder(BorderFactory.createTitledBorder("SubMesh"));
				subMeshPanel.setLayout(new BoxLayout(subMeshPanel, BoxLayout.Y_AXIS));

				for (int skin = 0; skin < animatedGroup.getSize(); skin++) {
					final int skinNo = skin;
					final JCheckBox subMeshCheckBox = new JCheckBox("Show submesh (" + skin + ")", true);
					subMeshCheckBox.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							toggleVisible(skinNo);
						}
					});
					subMeshPanel.add(subMeshCheckBox);
				}
				add(subMeshPanel);
			}
		}
		
		protected JLabel createLabel(String text, Color color) {
			JLabel label = new JLabel(text);
			label.setForeground(color);
			return label;
		}
	}
    
	
	@SuppressWarnings("serial")
	protected class AnimationControlPanel extends JPanel {
		
		protected AnimationControlPanel() {
			setLayout(new BorderLayout());
			setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			
			if (!hasSkinAnimation && !hasPoseAnimation) {
				return;
			}
			
			final JCheckBox animateCheckBox = new JCheckBox("Animate", animate);
			animateCheckBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					animate = animateCheckBox.isSelected();
				}
			});
			
			add(animateCheckBox, BorderLayout.NORTH);

			ButtonGroup animationGroup = new ButtonGroup();
			
			if (hasSkinAnimation) {
				addSkinAnimations(animationGroup);
			}
			
			if (hasPoseAnimation) {
				addPoseAnimations();
			}
			
		}

		protected void addSkinAnimations(ButtonGroup animationGroup) {

			
			JPanel skinPanel = new JPanel(new BorderLayout());
			skinPanel.setBorder(BorderFactory.createTitledBorder("Skin Animation"));

			JPanel skinAnimsPanel = new JPanel();
			skinAnimsPanel.setLayout(new BoxLayout(skinAnimsPanel, BoxLayout.Y_AXIS));
			
			final JSlider speedSlider = new JSlider(JSlider.HORIZONTAL, 0, 300, 100);
			speedSlider.setPaintTicks(true);
			speedSlider.setMajorTickSpacing(100);
			
			speedSlider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					skinAnimationSpeed = speedSlider.getValue() / 100f;
				}
			});
			
			
			addSkinAnimationButton("None <BindPose>", animationGroup, skinAnimsPanel, SKIN_BIND_POSE, true);
			addSkinAnimationButton("All", animationGroup, skinAnimsPanel, SKIN_ANIMATE_ALL, false);
			
			int clipNo = 1;
			for (SkinClip clip : skinClipSequence) {
				String text = clipNo + " " + ((clip.getName() == null) ? 
						"<no name>" : clip.getName());
				addSkinAnimationButton(text, animationGroup, skinAnimsPanel, clipNo, false);
				clipNo++;
			}
			
			skinPanel.add(speedSlider, BorderLayout.NORTH);
			skinPanel.add(skinAnimsPanel, BorderLayout.CENTER);
			
			add(skinPanel, BorderLayout.CENTER);
		}
		
		protected void addSkinAnimationButton(String text, ButtonGroup group, JComponent container, final int animation, boolean selected) {
			JRadioButton button = new JRadioButton(text);
			group.add(button);
			container.add(button);
			group.setSelected(button.getModel(), selected);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					skinAnimationSequence = animation;
					skinAnimationIndex = 0;
				}
			});
		}
		
		protected void addPoseAnimations() {
			
			JPanel animationPanel = new JPanel();
			animationPanel.setBorder(BorderFactory.createTitledBorder("Pose Animation"));
			animationPanel.setLayout(new BoxLayout(animationPanel, BoxLayout.Y_AXIS));

			int clipNo = 1;
			for (PoseClip clip : poseClipSequence) {
				PoseState poseState = new PoseState(clipNo);
				poseAnimStates.put(clipNo, poseState);
				
				String text = clipNo + " " + ((clip.getName() == null) ? 
						"<no name>" : clip.getName());
				addPoseAnimationButton(text, animationPanel, poseState, false);
				clipNo++;
			}
			add(animationPanel, BorderLayout.SOUTH);
		}
		
		protected void addPoseAnimationButton(String text, JComponent container, final PoseState poseState, boolean selected) {
			JPanel panel = new JPanel(new BorderLayout());
			
			final JCheckBox button = new JCheckBox(text);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					poseState.enabled = button.isSelected();
				}
			});
			
			final JSlider speedSlider = new JSlider(JSlider.HORIZONTAL, 0, 300, 100);
			speedSlider.setPaintTicks(true);
			speedSlider.setMajorTickSpacing(100);
			
			speedSlider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					poseState.speed = speedSlider.getValue() / 100f;
				}
			});
			
			panel.add(button, BorderLayout.NORTH);
			panel.add(speedSlider, BorderLayout.SOUTH);
			
			container.add(panel);
		}
	}

	protected static class PoseState {
		final int clipNo;
		float animationIndex = 0;
		boolean enabled = false;
		float speed = 1f;
		
		PoseState(int clipNo) {
			this.clipNo = clipNo;
		}
	}
	
	public static void main(String[] args) throws Exception {
		new AnimationBlendSample().loop();
	}

	

}
