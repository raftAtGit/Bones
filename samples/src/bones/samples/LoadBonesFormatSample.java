package bones.samples;

import java.awt.Dimension;
import java.io.FileInputStream;

import raft.jpct.bones.Skeleton;
import raft.jpct.bones.SkinIO;
import raft.jpct.bones.Skinned3D;
import raft.jpct.bones.SkinnedGroup;
import raft.jpct.bones.Skeleton.Debugger;

import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;

/**
 * <p>Demonstrates loading skin in Bones format.</p>
 *  
 * @see SkinIO#loadGroup(java.io.InputStream)
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
	protected Debugger createSkeletonDebugger() throws Exception {
		// in this file, root joint always remain in origin which doesn't look so good.
		// so we tell debugger to ignore root joint (0)
		return new Skeleton.Debugger(skinnedGroup.get(0).getCurrentPose(), 10f, (short)0);
	}

	@Override
	protected SkinnedGroup createSkinnedGroup() throws Exception {
		//return loadSeymour();
		return loadNinja();
	}
	
	private SkinnedGroup loadNinja() throws Exception {
		FileInputStream fis = new FileInputStream("./samples/data/ninja/ninja.group.bones");
		try {
			SkinnedGroup skinnedGroup = SkinIO.loadGroup(fis);
	
			Texture texture = new Texture("./samples/data/ninja/nskingr.jpg");
			TextureManager.getInstance().addTexture("ninja", texture);
			
			for (Skinned3D o : skinnedGroup) {
				o.setTexture("ninja");
				o.build();
				o.discardSkeletonMesh();
			}
			return skinnedGroup;
		} finally {
			fis.close();
		}
	}

	private SkinnedGroup loadSeymour() throws Exception {
		FileInputStream fis = new FileInputStream("./samples/data/seymour/seymour.group.bones");
		try {
			SkinnedGroup skinnedGroup = SkinIO.loadGroup(fis);
	
			Texture texture = new Texture("./samples/data/seymour/seymour_flipped.png");
			TextureManager.getInstance().addTexture("ninja", texture);
			
			for (Skinned3D o : skinnedGroup) {
				o.setTexture("ninja");
				o.build();
				o.discardSkeletonMesh();
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
		
		// skeleton is oriented upside down, rotate it
		currentPose.getSkeleton().getTransform().rotateX((float)Math.PI);
		
        update(0); // update once to reflect changes visible in first scene

        // ninja is facing positive Z axis, so place camera accordingly
        cameraController.cameraAngle = 0f;
		autoAdjustCamera();
	}
	
	public static void main(String[] args) throws Exception {
		new LoadBonesFormatSample().loop();
	}

}
