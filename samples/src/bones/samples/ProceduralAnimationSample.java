/**
 * Some of the classes in this file are adapted or inspired from Ardor3D. 
 * Such classes are indicated in class javadocs. 
 * 
 * Modification and redistribution of them may be subject to Ardor3D's license: 
 * http://www.ardor3d.com/LICENSE
 */
package bones.samples;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import raft.jpct.bones.Animated3D;
import raft.jpct.bones.AnimatedGroup;
import raft.jpct.bones.BonesImporter;
import raft.jpct.bones.Joint;
import raft.jpct.bones.Quaternion;
import raft.jpct.bones.SkeletonDebugger;
import raft.jpct.bones.SkeletonPose;

import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;
import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;

/** 
 * <p>Demonstrates how to programatically pose {@link Joint joint}s to follow an external object.</p>
 * 
 * <p>This class is adapted from <a href="http://www.ardor3d.com">Ardor3D.</a></p>
 * */
public class ProceduralAnimationSample extends AbstractSample {
	
	private AnimatedGroup skinnedGroup;
    private SkeletonPose currentPose;
    private SkeletonDebugger skeletonDebugger;
    private Object3D ballSphere; 
	
	private CameraOrbitController cameraController;
	
	private long totalTime = 0;
	
	protected boolean showMesh = true;
	protected boolean showSkeleton = false;
	
	
	public ProceduralAnimationSample() {
	}
	
    
	@Override
	protected String getName() {
		return getClass().getName();
	}

	
	@Override
	protected void initialize() throws Exception {
		File colladaFile = new File("./samples/data/seymour/Seymour.dae");
		URI uri = colladaFile.toURI();
		
        final SimpleResourceLocator resLocater = new SimpleResourceLocator(uri.resolve("./"));
        ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_MODEL, resLocater);
        
		try {
			ColladaImporter colladaImporter = new ColladaImporter().loadTextures(false);
			ColladaStorage colladaStorage = colladaImporter.load(uri.toString());
			
			this.skinnedGroup = BonesImporter.importCollada(colladaStorage, 1f);
		} finally {
			ResourceLocatorTool.removeResourceLocator(ResourceLocatorTool.TYPE_MODEL, resLocater);
		}

		Texture texture = new Texture("./samples/data/seymour/seymour_flipped.png");
		TextureManager.getInstance().addTexture("seymour", texture);
		
		world.setAmbientLight(255, 255, 255);

		for (Animated3D o : skinnedGroup) {
			o.setTexture("seymour");
			o.build();
			o.discardMeshData();
		}
		skinnedGroup.addToWorld(world);
		
		// all SkinnedObject3D share the same pose 
		this.currentPose = skinnedGroup.get(0).getSkeletonPose();
		
		// seymour is oriented for GL coordinates, rotate it for jPCT
		currentPose.getSkeleton().getTransform().rotateX((float)Math.PI);
		
		this.skeletonDebugger = new SkeletonDebugger(currentPose);
		skeletonDebugger.addToWorld(world);
		skeletonDebugger.setVisibility(showSkeleton);
		
        this.ballSphere = Primitives.getSphere(10, 0.5f);
        ballSphere.build();
        world.addObject(ballSphere);
        
        cameraController = new CameraOrbitController(world.getCamera());
        cameraController.cameraTarget.set(0, -5, 0);
        cameraController.cameraRadius = 20;
        
		ControlsPanel controlsPanel = new ControlsPanel();
		renderPanel.add(controlsPanel);
		Rectangle bounds = new Rectangle(controlsPanel.getPreferredSize());
		bounds.translate(10, 10);
		controlsPanel.setBounds(bounds);
		
		
		renderPanel.addKeyListener(cameraController);
        
        update(0); // update once to reflect changes visible in first scene
	}
	
	@Override
	protected void update(long deltaTime) {
		totalTime += deltaTime;
		updateBallLocation();
		
		//stretchNeck();
		
        currentPose.updateTransforms();
        skeletonDebugger.update(currentPose);
        skinnedGroup.applySkeletonPose();
        
		cameraController.placeCamera();
	}
	
	
	private void stretchNeck() {
		currentPose.setToBindPose();
		
		final int jointIndex = 13; 
		final Joint neckJoint = currentPose.getSkeleton().getJoint(jointIndex);
		
        //final Matrix[] globals = currentPose.getGlobalJointTransforms();
        final int parentIndex = neckJoint.getParentIndex();
		
        SimpleVector boneDirection = currentPose.getGlobal(neckJoint.getIndex()).getTranslation().calcSub(
        		currentPose.getGlobal(parentIndex).getTranslation()).normalize();
        boneDirection.scalarMul(2f);
        
        boneDirection.rotate(neckJoint.getInverseBindPose());
        currentPose.getLocal(jointIndex).translate(boneDirection);
	}

    private void updateBallLocation() {
		float seconds = totalTime / 1000f;
		
		SimpleVector ballPos = new SimpleVector(Math.sin(seconds) * 5, Math.cos(seconds) * 5 + 10, 5);
        
         //Neck
         targetJoint(currentPose, 13, new SimpleVector(0, 0, 1), ballPos, 1.0f);
        
         // Right arm
         targetJoint(currentPose, 10, new SimpleVector(-1, 0, 0), ballPos, 0.4f);
         targetJoint(currentPose, 11, new SimpleVector(-1, 0, 0), ballPos, 0.6f);
         targetJoint(currentPose, 12, new SimpleVector(-1, 0, 0), ballPos, 0.5f);
        
         // Left arm
         targetJoint(currentPose, 7, new SimpleVector(1, 0, 0), ballPos, 0.15f);
         targetJoint(currentPose, 8, new SimpleVector(1, 0, 0), ballPos, 0.15f);
        
         // Waist
         targetJoint(currentPose, 5, new SimpleVector(0, 1, 0), ballPos, 0.1f);
         
         // all above demo calculations assume GL coordinates of Seymour. 
         // i take the easy way, leave them as they are, and finally transform to new location for rendering 
         ballPos.matMul(currentPose.getSkeleton().getTransform());
         ballSphere.translate(ballPos.calcSub(ballSphere.getTranslation()));
    }
    
    private void targetJoint(SkeletonPose pose, int jointIndex, SimpleVector bindPoseDirection,
    		SimpleVector targetPos, final float targetStrength) {
    	
        //final Matrix[] globalTransforms = pose.getGlobalJointTransforms();

        final int parentIndex = pose.getSkeleton().getJoint(jointIndex).getParentIndex();

        // neckBindGlobalTransform is the neck bone -> model space transform. essentially, it is the world transform of
        // the neck bone in bind pose.
        final Matrix jointInverseBindPose = pose.getSkeleton().getJoint(jointIndex).getInverseBindPose();
        final Matrix jointBindPose = jointInverseBindPose.invert();

        // Get a vector representing forward direction in neck space, use inverse to take from world -> neck space.
        SimpleVector forwardDirection = new SimpleVector(bindPoseDirection);
        forwardDirection.rotate(jointInverseBindPose);

        // Get a vector representing a direction to target point in neck space.
        SimpleVector targetDirection = targetPos.calcSub(pose.getGlobal(jointIndex).getTranslation()).normalize();
        targetDirection.rotate(jointInverseBindPose);

        // Calculate a rotation to go from one direction to the other and set that rotation on a blank transform.
        Quaternion quat = new Quaternion();
        quat.fromVectorToVector(forwardDirection, targetDirection);
        quat.slerp(Quaternion.IDENTITY, quat, targetStrength);

        final Matrix subGlobal = quat.getRotationMatrix();
        
        // now remove the global/world transform of the neck's parent bone, leaving us with just the local transform of
        // neck + rotation.
        subGlobal.matMul(jointBindPose);
        subGlobal.matMul(pose.getSkeleton().getJoint(parentIndex).getInverseBindPose());

        // set that as the neck's transform
        pose.getLocal(jointIndex).setTo(subGlobal);
    }

	protected void toggleVisible(int index) {
		if ((skinnedGroup.getSize() > 1) && (index < skinnedGroup.getSize()))
			skinnedGroup.get(index).setVisibility(!skinnedGroup.get(index).getVisibility());
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
					for (Animated3D o : skinnedGroup) {
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
	
	public static void main(String[] args) throws Exception {
		new ProceduralAnimationSample().loop();
	}
}
