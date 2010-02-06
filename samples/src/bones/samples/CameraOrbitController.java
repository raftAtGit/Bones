package bones.samples;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import com.threed.jpct.Camera;
import com.threed.jpct.SimpleVector;

/** 
 * <p>Utility class to rotate {@link Camera} around a point with a fixed radius. 
 * Both orbit center and radius can be adjusted. This class can be added as a {@link KeyListener} to
 * {@link RenderPanel} to position camera according to key events.</p>
 * 
 * @author hakan eryargi (r a f t)
 * */
public class CameraOrbitController extends KeyAdapter {
	
	public final SimpleVector cameraTarget = new SimpleVector(0, 0, 0);
	/** the angle with respect to positive Z axis. initial value is PI so looking down to positive Z axis. */
    public float cameraAngle = (float)(Math.PI);
	public float cameraRadius = 20f;
	public float cameraRotationSpeed = 0.1f;
	public float minCameraRadius = 3f;
	public float cameraMoveStepSize = 0.5f;
	
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
			cameraTarget.y -= cameraMoveStepSize;
    	if (cameraMovingDown)
			cameraTarget.y += cameraMoveStepSize;
    	if (cameraMovingIn)
			cameraRadius = Math.max(cameraRadius - cameraMoveStepSize, minCameraRadius);
    	if (cameraMovingOut)
			cameraRadius += cameraMoveStepSize;
    	if (cameraTurningRight)
    		cameraAngle += cameraRotationSpeed;
    	if (cameraTurningLeft)
    		cameraAngle -= cameraRotationSpeed;
    	
        float camX = (float) Math.sin(cameraAngle) * cameraRadius;
        float camZ = (float) Math.cos(cameraAngle) * cameraRadius;
        
        SimpleVector camPos = new SimpleVector(camX, 0, camZ);
        camPos.add(cameraTarget);
        camera.setPosition(camPos);
        camera.lookAt(cameraTarget);
	}
	

}
