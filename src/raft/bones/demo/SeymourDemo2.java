/**
 * Some of the classes in this file are adapted or inspired from Ardor3D. 
 * Such classes are indicated in class javadocs. 
 * 
 * Modification and redistribution of them may be subject to Ardor3D's license: 
 * http://www.ardor3d.com/LICENSE
 */
package raft.bones.demo;

import java.awt.Rectangle;

import raft.jpct.bones.SkinnedGroup;
import raft.jpct.bones.Quaternion;
import raft.jpct.bones.Skeleton;
import raft.jpct.bones.SkinIO;
import raft.jpct.bones.Skinned3D;

import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.extension.model.collada.jdom.data.SkinData;
import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;

/** 
 * <p></p>
 * 
 * <p>This class is adapted from <a href="http://www.ardor3d.com">Ardor3D.</a></p>
 * */
public class SeymourDemo2 extends AbstractDemo {
	
	private SkinnedGroup skinnedGroup;
    private Skeleton.Pose currentPose;
    private Skeleton.Debugger skeletonDebugger;
    private Object3D ballSphere; 
	
	private CameraOrbitController cameraController;
	
	private long totalTime = 0;
	
	
	public SeymourDemo2() {
	}
	
	private SkinnedGroup loadArdorSkinGroup() {
		
		ColladaImporter colladaImporter = new ColladaImporter().loadTextures(false);
		ColladaStorage colladaStorage = colladaImporter.load("/raft/bones/demo/Seymour.dae");
		
		return SkinIO.loadColladaSkin(colladaStorage, 1f);
	}
	
	
	@Override
	protected void initialize() throws Exception {
		this.skinnedGroup = loadArdorSkinGroup();

		Texture texture = new Texture(getClass().getResourceAsStream("boy_10_flipped.png"));
		TextureManager.getInstance().addTexture("seymour", texture);
		
		world.setAmbientLight(255, 255, 255);

		for (Skinned3D o : skinnedGroup) {
			o.setTexture("seymour");
			o.build();
			o.discardSkeletonMesh();
		}
		skinnedGroup.addToWorld(world);
		
		// all SkinnedObject3D share the same pose 
		this.currentPose = skinnedGroup.get(0).getCurrentPose();
		
		// seymour is oriented for GL coordinates, rotate it for jPCT
		currentPose.getSkeleton().getTransform().rotateX((float)Math.PI);
		
		this.skeletonDebugger = new Skeleton.Debugger(currentPose);
		skeletonDebugger.addToWorld(world);
		
        this.ballSphere = Primitives.getSphere(10, 0.5f);
        ballSphere.build();
        world.addObject(ballSphere);
        
        cameraController = new CameraOrbitController(world.getCamera());
        cameraController.cameraTarget.set(0, -5, 0);
        cameraController.cameraRadius = 20;
        
		ControlsPanel controlsPanel = new ControlsPanel(skinnedGroup, skeletonDebugger);
		renderPanel.add(controlsPanel);
		Rectangle bounds = new Rectangle(controlsPanel.getPreferredSize());
		bounds.translate(10, 10);
		controlsPanel.setBounds(bounds);
		
		
		renderPanel.addKeyListener(cameraController);
		renderPanel.addKeyListener(controlsPanel);
		
        
        update(0); // update once to reflect changes visible in first scene
	}
	
	@Override
	protected void update(long deltaTime) {
		totalTime += deltaTime;
		updateBallLocation();
		
		//stretchNeck();
		
        currentPose.updateTransforms();
        skeletonDebugger.update(currentPose);
        skinnedGroup.applyPose();
        
		cameraController.placeCamera();
	}
	
	private void stretchNeck() {
		currentPose.setToBindPose();
		
		final short jointIndex = 13; 
		final Skeleton.Joint neckJoint = currentPose.getSkeleton().getJoint(jointIndex);
		
        //final Matrix[] globals = currentPose.getGlobalJointTransforms();
        final short parentIndex = neckJoint.getParentIndex();
		
        SimpleVector boneDirection = currentPose.getGlobal(neckJoint.getIndex()).getTranslation().calcSub(
        		currentPose.getGlobal(parentIndex).getTranslation()).normalize();
        boneDirection.scalarMul(2f);
        
        boneDirection.rotate(neckJoint.getInverseBindPose());
        currentPose.getLocal(jointIndex).translate(boneDirection);
	}

    private void updateBallLocation() {
		float seconds = totalTime / 1000f;
		
		SimpleVector ballPos = new SimpleVector(Math.sin(seconds) * 5, Math.cos(seconds) * 5 + 10, 5);
        
        //ballPos.set(-5, 15, 5);
		
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
    
    private void targetJoint(Skeleton.Pose pose, int jointIndex, SimpleVector bindPoseDirection,
    		SimpleVector targetPos, final float targetStrength) {
    	
        //final Matrix[] globalTransforms = pose.getGlobalJointTransforms();

        final short parentIndex = pose.getSkeleton().getJoint(jointIndex).getParentIndex();

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

	@Override
	protected String getName() {
		return "Bones - Precedural Animation Demo";
	}

	public static void main(String[] args) throws Exception {
		new SeymourDemo2().loop();
	}
}
