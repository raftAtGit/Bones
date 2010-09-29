package bones.samples;

import java.awt.Dimension;
import java.io.FileInputStream;

import raft.jpct.bones.Animated3D;
import raft.jpct.bones.AnimatedGroup;
import raft.jpct.bones.BonesIO;
import raft.jpct.bones.SkeletonDebugger;

import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;

/**
 * <p>Demonstrates loading skin in Bones format. 
 * This sample can be run only with bones.jar and jpct.jar in classpath.</p>
 *  
 * @see BonesIO#loadGroup(java.io.InputStream)
 * @author hakan eryargi (r a f t)
 * */
public class LoadBonesFormatSample extends AbstractSkinSample {

	public LoadBonesFormatSample() {
		super(new Dimension(1024, 768));
	}
	
	@Override
	protected String getName() {
		return getClass().getName();
	}

	@Override
	protected SkeletonDebugger createSkeletonDebugger() throws Exception {
		// in ninja file, root joint always remain in origin which doesn't look so good.
		// so we tell debugger to ignore root joint (0)
		return new SkeletonDebugger(animatedGroup.get(0).getSkeletonPose(), 10f, (short)0);
	}

	@Override
	protected AnimatedGroup createAnimatedGroup() throws Exception {
		//return loadSeymour();
		return loadNinja();
	}
	
	private AnimatedGroup loadNinja() throws Exception {
		FileInputStream fis = new FileInputStream("./samples/data/ninja/ninja.group.bones");
		try {
			AnimatedGroup skinnedGroup = BonesIO.loadGroup(fis);
	
			Texture texture = new Texture("./samples/data/ninja/nskingr.jpg");
			TextureManager.getInstance().addTexture("ninja", texture);
			
			for (Animated3D o : skinnedGroup) {
				o.setTexture("ninja");
				o.build();
				o.discardMeshData();
			}
			return skinnedGroup;
		} finally {
			fis.close();
		}
	}

	private AnimatedGroup loadSeymour() throws Exception {
		FileInputStream fis = new FileInputStream("./samples/data/seymour/seymour.group.bones");
		try {
			AnimatedGroup skinnedGroup = BonesIO.loadGroup(fis);
	
			Texture texture = new Texture("./samples/data/seymour/seymour.png");
			TextureManager.getInstance().addTexture("ninja", texture);
			
			for (Animated3D o : skinnedGroup) {
				o.setTexture("ninja");
				o.build();
				o.discardMeshData();
			}
			return skinnedGroup;
		} finally {
			fis.close();
		}
	}
	
	@Override
	protected void initialize() throws Exception {
		super.initialize();
		
		world.setAmbientLight(255, 255, 255);
		
        update(0); // update once to reflect changes visible in first scene

        // ninja is facing positive Z axis, so place camera accordingly
        cameraController.cameraAngle = 0f;
		autoAdjustCamera();
	}
	
	public static void main(String[] args) throws Exception {
		new LoadBonesFormatSample().loop();
	}

}
