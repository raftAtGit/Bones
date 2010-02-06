package bones.samples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.util.Hashtable;

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

import raft.jpct.bones.Clip;
import raft.jpct.bones.ClipSequence;
import raft.jpct.bones.Skeleton;
import raft.jpct.bones.Skinned3D;
import raft.jpct.bones.SkinnedGroup;

import com.threed.jpct.Camera;
import com.threed.jpct.Config;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Interact2D;
import com.threed.jpct.SimpleVector;

/** 
 * <p>Makes it easier to load skin and animation and display animation controls. 
 * This class also has support to initially auto position camera and later move it with mouse.</p>
 * 
 * @author hakan eryargi (r a f t)
 * */
public abstract class AbstractSkinSample extends AbstractSample {

	protected SkinnedGroup skinnedGroup;
	protected Skeleton.Pose currentPose;
	protected Skeleton.Debugger skeletonDebugger;
	
	protected ClipSequence clipSequence;
	
	protected int animationSequence = -1; // bind pose
	protected float animationSpeed = 1f;
	protected float animationIndex = 0f;
    
	protected boolean showMesh = true;
	protected boolean showSkeleton = false;
	
	protected CameraOrbitController cameraController;
	
	protected AbstractSkinSample() {}

	protected AbstractSkinSample(Dimension size) {
		super(size);
	}
	
	protected abstract SkinnedGroup createSkinnedGroup() throws Exception;

	protected abstract Skeleton.Debugger createSkeletonDebugger() throws Exception;
	
	protected float dragTurnAnglePerPixel = (float) (Math.PI / 256);
	protected float dragMovePerPixel = 1f;
	protected float cameraMovePerWheelClick = 1.1f;
	
	private Point dragStartPoint = null;
	private float cameraAngleAtDragStart = 0f;
	private float cameraHeightAtDragStart = 0f;
	
	@Override
	protected void initialize() throws Exception {
		this.skinnedGroup = createSkinnedGroup();
		
		skinnedGroup.addToWorld(world);
		
		// all SkinnedObject3D share the same pose 
		this.currentPose = skinnedGroup.get(0).getCurrentPose();
		
		this.skeletonDebugger = createSkeletonDebugger();
		skeletonDebugger.addToWorld(world);
		
		this.clipSequence = skinnedGroup.getClipSequence();
		
        cameraController = new CameraOrbitController(world.getCamera());
        float[] bb = calcBoundingBox();
        float height = bb[3] - bb[2];
        dragMovePerPixel = height / frameBuffer.getOutputHeight();
        Config.farPlane = Math.max(height * 100, Config.farPlane);
		
		for (Skinned3D o : skinnedGroup) {
			o.setVisibility(showMesh);
		}
		skeletonDebugger.setVisibility(showSkeleton);

		renderPanel.addKeyListener(cameraController);
		
		renderPanel.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				switch (e.getKeyCode()) {
					case KeyEvent.VK_S:
						showSkeleton = !showSkeleton;
						skeletonDebugger.setVisibility(showSkeleton);
						break;
					case KeyEvent.VK_M:
						showMesh = !showMesh;
						for (Skinned3D o : skinnedGroup) {
							o.setVisibility(showMesh);
						}
						break;
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

		renderPanel.addMouseListener(new MouseAdapter() {
			@Override
		    public void mousePressed(MouseEvent event) {
		        dragStartPoint = event.getPoint();
		        cameraAngleAtDragStart = cameraController.cameraAngle;
		        cameraHeightAtDragStart = cameraController.cameraTarget.y;
		    }
		});
		
		renderPanel.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
		    public void mouseDragged(MouseEvent event) {
		        cameraController.cameraAngle = cameraAngleAtDragStart 
		        	+ (event.getPoint().x - dragStartPoint.x) * dragTurnAnglePerPixel;
		        cameraController.cameraTarget.y = cameraHeightAtDragStart 
	        		- (event.getPoint().y - dragStartPoint.y) * dragMovePerPixel;
		    }
		});
		
		renderPanel.addMouseWheelListener(new MouseAdapter() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				int clicks = e.getWheelRotation();
				for (int i = 0; i < Math.abs(clicks); i++) {
					if (clicks > 0) 
						cameraController.cameraRadius *= cameraMovePerWheelClick;
					else 
						cameraController.cameraRadius /= cameraMovePerWheelClick;
				}
				cameraController.cameraRadius = Math.max(cameraController.minCameraRadius, cameraController.cameraRadius);
			}
		});
		
		createGUI();
		
        update(0); // update once to reflect changes visible in first scene
		
	}
	
	/** adjusts camera based on current mesh of skinned group. 
	 * camera looks at mid point of height and placed at a distance 
	 * such that group height occupies 2/3 of screen height. */
	protected void autoAdjustCamera() {
		float[] bb = calcBoundingBox();
		float groupHeight = bb[3] - bb[2];
        cameraController.cameraRadius = calcDistance(world.getCamera(), frameBuffer, 
        		frameBuffer.getOutputHeight() / 1.5f , groupHeight);
        cameraController.minCameraRadius = groupHeight / 10f;
        cameraController.cameraTarget.y = (bb[3] + bb[2]) / 2; 
        cameraController.placeCamera();
	}

	/** calculates and returns whole bounding box of skinned group */
	protected float[] calcBoundingBox() {
		float[] box = null;
		
		for (Skinned3D skin : skinnedGroup) {
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
    protected float calcDistance(Camera c, FrameBuffer buffer, float height, float objectHeight) {
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
	
	protected void createGUI() {
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

	@Override
	protected void update(long deltaTime) {
		cameraController.placeCamera();
		
		if (animationSequence < 0) {
			currentPose.setToBindPose();
			currentPose.updateTransforms();
			skinnedGroup.applyPose();
			
		} else {
			
			float clipTime = (animationSequence == 0) 
				? clipSequence.getTime() // whole animation 
				: clipSequence.getClip(animationSequence - 1).getTime(); // single clip 

			animationIndex += deltaTime * animationSpeed / clipTime / 1000;
			while (animationIndex > 1)
				animationIndex -= 1;
				
			skinnedGroup.animateSkin(animationIndex, animationSequence);
		}
		
		
        skeletonDebugger.update(currentPose);
	}
	
	protected void toggleVisible(int index) {
		if ((skinnedGroup.getSize() > 1) && (index < skinnedGroup.getSize()))
			skinnedGroup.get(index).setVisibility(!skinnedGroup.get(index).getVisibility());
	}
	
	
	
	@SuppressWarnings("serial")
	protected class AnimationControlPanel extends JPanel {
		
		protected AnimationControlPanel() {
			setLayout(new BorderLayout());
			setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			
			if ((clipSequence == null) || (clipSequence.getSize() == 0)) {
				add(new JLabel("No animations"), BorderLayout.CENTER);
				return;
			}
			
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
			animationPanel.setLayout(new BoxLayout(animationPanel, BoxLayout.Y_AXIS));
			
			addAnimationButton("None <BindPose>", animationGroup, animationPanel, -1, true);
			addAnimationButton("All", animationGroup, animationPanel, 0, false);
			
			int clipNo = 1;
			for (Clip clip : clipSequence) {
				String text = clipNo + " " + ((clip.getName() == null) ? 
						"<no name>" : clip.getName());
				addAnimationButton(text, animationGroup, animationPanel, clipNo, false);
				clipNo++;
			}
			add(speedSlider, BorderLayout.NORTH);
			add(animationPanel, BorderLayout.CENTER);
		}
		
		protected void addAnimationButton(String text, ButtonGroup group, JComponent container, final int animation, boolean selected) {
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
					for (Skinned3D o : skinnedGroup) {
						o.setVisibility(showMesh);
					}
				}
			});
			add(showMeshCheckBox);
			
			final JCheckBox wireframeCheckBox = new JCheckBox("Draw wireframe", drawWireFrame);
			wireframeCheckBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					drawWireFrame = wireframeCheckBox.isSelected();
				}
			});
			add(wireframeCheckBox);
			
			if (skinnedGroup.getSize() > 1) {
				JPanel subMeshPanel = new JPanel();
				subMeshPanel.setBorder(BorderFactory.createTitledBorder("SubMesh"));
				subMeshPanel.setLayout(new BoxLayout(subMeshPanel, BoxLayout.Y_AXIS));

				for (int skin = 0; skin < skinnedGroup.getSize(); skin++) {
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
}
