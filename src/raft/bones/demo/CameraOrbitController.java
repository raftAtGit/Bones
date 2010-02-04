package raft.bones.demo;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import com.threed.jpct.Camera;
import com.threed.jpct.SimpleVector;

public class CameraOrbitController extends KeyAdapter {
	
	public final SimpleVector cameraTarget = new SimpleVector(0, 0, 0);
    public float cameraAngle = 0f;
	public float cameraRadius = 20f;
	public float cameraRotationSpeed = 0.1f;
	
	private boolean cameraMovingUp = false;
	private boolean cameraMovingDown = false;
	private boolean cameraMovingIn = false;
	private boolean cameraMovingOut = false;
	private boolean cameraTurningLeft = false;
	private boolean cameraTurningRight = false;
	
	private Camera camera;
	
	public CameraOrbitController(Camera camera) {
		this.camera = camera;
	}
	
	@Override
	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
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
	
    public void placeCamera() {
    	if (cameraMovingUp)
			cameraTarget.y -= 0.5f;
    	if (cameraMovingDown)
			cameraTarget.y += 0.5f;
    	if (cameraMovingIn)
			cameraRadius = Math.max(cameraRadius - 0.5f, 3f);
    	if (cameraMovingOut)
			cameraRadius += 0.5f;
    	if (cameraTurningRight)
    		cameraAngle += cameraRotationSpeed;
    	if (cameraTurningLeft)
    		cameraAngle -= cameraRotationSpeed;
    	
        float camX = (float) Math.cos(cameraAngle) * cameraRadius;
        float camZ = (float) Math.sin(cameraAngle) * cameraRadius;
        
        SimpleVector camPos = new SimpleVector(camX, 0, camZ);
        camPos.add(cameraTarget);
        camera.setPosition(camPos);
        camera.lookAt(cameraTarget);
	}
	

}
