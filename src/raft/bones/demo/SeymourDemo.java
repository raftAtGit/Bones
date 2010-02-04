package raft.bones.demo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import raft.jpct.bones.SkinnedGroup;
import raft.jpct.bones.Quaternion;
import raft.jpct.bones.Skeleton;
import raft.jpct.bones.Skinned3D;

import com.threed.jpct.Camera;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.IRenderer;
import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;

public class SeymourDemo {
	
	private static final long UPDATE_PERIOD = 25; // milliseconds 
	
	private static final SimpleVector CAMERA_TARGET = new SimpleVector(0, -5, 0);
	private static final float CAMERA_ROTATION_SPEED = 0.1f;

	private final SkinnedGroup skinnedGroup;
    private final Skeleton.Pose currentPose;
    private final Skeleton.Debugger skeletonDebugger;
    private final Object3D ballSphere; 
	
	private final World world;
	private final RenderPanel renderPanel;
	final FrameBuffer frameBuffer;
	
    private boolean showMesh = true;
    private boolean showSkeleton = false;
    private boolean drawWireFrame = false;
    
    private float cameraAngle = (float)-Math.PI/2;
	private float cameraRadius = 20f;
	
	private boolean cameraMovingUp = false;
	private boolean cameraMovingDown = false;
	private boolean cameraMovingIn = false;
	private boolean cameraMovingOut = false;
	private boolean cameraTurningLeft = false;
	private boolean cameraTurningRight = false;
	
	private long lastTime = System.currentTimeMillis();
	private long passedTime = 0;
	private long totalTime = 0;
	
	
	public SeymourDemo(SkinnedGroup skinnedGroup) {
		this.skinnedGroup = skinnedGroup;

		Texture texture = new Texture(SeymourDemo.class.getResourceAsStream("boy_10_flipped.png"));
		TextureManager.getInstance().addTexture("seymour", texture);
		
		this.world = new World();
		
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
		skeletonDebugger.setVisibility(showSkeleton);
		
        this.ballSphere = Primitives.getSphere(10, 0.5f);
        ballSphere.build();
        world.addObject(ballSphere);
        
        placeCamera();
        update(); // update once to reflect changes visible in first scene
		
		this.frameBuffer = new FrameBuffer(800, 600, FrameBuffer.SAMPLINGMODE_NORMAL);
		frameBuffer.enableRenderer(IRenderer.RENDERER_SOFTWARE);
        
		this.renderPanel = new RenderPanel();
		renderPanel.setPreferredSize(new Dimension(frameBuffer.getOutputWidth(), frameBuffer.getOutputHeight()));
        
        JFrame frame = new JFrame("seymour - skeleton animation demo");
        frame.setResizable(false);
        frame.add(renderPanel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
		
	}
	public void loop() throws Exception {
		lastTime = System.currentTimeMillis();
		
    	while (true) {
	        SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					long now = System.currentTimeMillis();
					passedTime += now - lastTime;
					lastTime = now;
		
					while (passedTime  > UPDATE_PERIOD) {
						update();
						passedTime -= UPDATE_PERIOD;
					}
	        
					display();
					renderPanel.paintImmediately(renderPanel.getBounds());
					
		    		long end = System.currentTimeMillis();
		            if (end - now < 20) {
		            	try {
		            		Thread.sleep(20 - end + now);
		            	} catch (InterruptedException e) {}
		            } else {
		            	Thread.yield();
		            }
				}
			});
    	}
	}
	
	private void update() {
		totalTime += UPDATE_PERIOD;
		updateBallLocation();
		
		//stretchNeck();
		
        currentPose.updateTransforms();
        skeletonDebugger.update(currentPose);
        skinnedGroup.applyPose();
        
		placeCamera();
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

	private void display() {
		frameBuffer.clear();
        world.renderScene(frameBuffer);
        world.draw(frameBuffer);
        
        if (drawWireFrame)
        	world.drawWireframe(frameBuffer, Color.GREEN);
	}
	
    private void placeCamera() {
    	if (cameraMovingUp)
			CAMERA_TARGET.y -= 0.5f;
    	if (cameraMovingDown)
			CAMERA_TARGET.y += 0.5f;
    	if (cameraMovingIn)
			cameraRadius = Math.max(cameraRadius - 0.5f, 3f);
    	if (cameraMovingOut)
			cameraRadius += 0.5f;
    	if (cameraTurningRight)
    		cameraAngle += CAMERA_ROTATION_SPEED;
    	if (cameraTurningLeft)
    		cameraAngle -= CAMERA_ROTATION_SPEED;
    	
        Camera camera = world.getCamera();
        float camX = (float) Math.cos(cameraAngle) * cameraRadius;
        float camZ = (float) Math.sin(cameraAngle) * cameraRadius;
        camera.setPosition(camX, CAMERA_TARGET.y, camZ);
        camera.lookAt(CAMERA_TARGET);
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
        final Matrix inverseNeckBindGlobalTransform = pose.getSkeleton().getJoint(jointIndex).getInverseBindPose();
        final Matrix neckBindGlobalTransform = inverseNeckBindGlobalTransform.invert();

        // Get a vector representing forward direction in neck space, use inverse to take from world -> neck space.
        SimpleVector forwardDirection = new SimpleVector(bindPoseDirection);
        forwardDirection.rotate(inverseNeckBindGlobalTransform);

        // Get a vector representing a direction to target point in neck space.
        SimpleVector targetDirection = targetPos.calcSub(pose.getGlobal(jointIndex).getTranslation()).normalize();
        targetDirection.rotate(inverseNeckBindGlobalTransform);

        // Calculate a rotation to go from one direction to the other and set that rotation on a blank transform.
        Quaternion quat = new Quaternion();
        quat.fromVectorToVector(forwardDirection, targetDirection);
        quat.slerp(Quaternion.IDENTITY, quat, targetStrength);

        final Matrix subGlobal = quat.getRotationMatrix();
        
        // now remove the global/world transform of the neck's parent bone, leaving us with just the local transform of
        // neck + rotation.
        subGlobal.matMul(neckBindGlobalTransform);
        subGlobal.matMul(pose.getSkeleton().getJoint(parentIndex).getInverseBindPose());

        // set that as the neck's transform
        pose.getLocal(jointIndex).setTo(subGlobal);
    }

	@SuppressWarnings("serial")
	class RenderPanel extends JPanel {
    	RenderPanel() {
    		setFocusable(true);
    		
    		addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					switch (e.getKeyCode()) {
						case KeyEvent.VK_ESCAPE:
							System.exit(0);
							break;
						case KeyEvent.VK_W:
							drawWireFrame = !drawWireFrame;
							break;
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
						case KeyEvent.VK_UP:
							cameraMovingUp = true;
							break;
						case KeyEvent.VK_DOWN:
							cameraMovingDown = true;
							break;
						case KeyEvent.VK_RIGHT:
							cameraTurningRight = true;
							break;
						case KeyEvent.VK_LEFT:
							cameraTurningLeft = true;
							break;
						case KeyEvent.VK_A:
							cameraMovingIn = true;
							break;
						case KeyEvent.VK_Z:
							cameraMovingOut = true;
							break;
					}
				}

				@Override
				public void keyReleased(KeyEvent e) {
					switch (e.getKeyCode()) {
						case KeyEvent.VK_UP:
							cameraMovingUp = false;
							break;
						case KeyEvent.VK_DOWN:
							cameraMovingDown = false;
							break;
						case KeyEvent.VK_RIGHT:
							cameraTurningRight = false;
							break;
						case KeyEvent.VK_LEFT:
							cameraTurningLeft = false;
							break;
						case KeyEvent.VK_A:
							cameraMovingIn = false;
							break;
						case KeyEvent.VK_Z:
							cameraMovingOut = false;
							break;
					}
				}
    		});
    	}
    	
        public void paintComponent(Graphics g) {
            frameBuffer.display(g);
            g.setColor(Color.WHITE);
            
            int x = 10; int y = 20;
            g.drawString("arrow keys, a,z to move camera", x, y+=20);
            g.drawString("vis list: " + world.getVisibilityList().getSize(), x, y+=20);
            g.drawString("w: toggle wireframe " + (drawWireFrame ? "(visible)" : "(hidden)"), x, y+=20);
            g.drawString("s: toggle skeleton " + (showSkeleton ? "(visible)" : "(hidden)"), x, y+=20);
            g.drawString("m: toggle mesh " + (showMesh ? "(visible)" : "(hidden)"), x, y+=20);
            
        }
    }
	
}
