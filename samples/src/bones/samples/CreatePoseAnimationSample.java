package bones.samples;

import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.SimpleVector;

import raft.jpct.bones.Animated3D;
import raft.jpct.bones.AnimatedGroup;
import raft.jpct.bones.MeshChannel;
import raft.jpct.bones.MeshPose;
import raft.jpct.bones.PoseClip;
import raft.jpct.bones.PoseClipSequence;
import raft.jpct.bones.PoseFrame;
import raft.jpct.bones.SkeletonDebugger;

/**
 * 
 * Demonstrates programatically creating a pose animation.
 * 
 * @author  hakan eryargi (r a f t)
 */
public class CreatePoseAnimationSample extends AbstractSkinSample {

	@Override
	protected AnimatedGroup createAnimatedGroup() throws Exception {
		MeshPose pose1 = new MeshPose("1", new SimpleVector[] { new SimpleVector(0, 2, 0)}, new int[] {0});
		PoseFrame frame1 = new PoseFrame(new MeshPose[] {}, new float[] {});
		PoseFrame frame2 = new PoseFrame(new MeshPose[] {pose1}, new float[] {1f});
		
		MeshChannel channel = new MeshChannel(0, new PoseFrame[] {frame1, frame2, frame1}, new float[] {0f, 1f, 2f});
		PoseClip clip = new PoseClip(1, channel);
		PoseClipSequence clipSequence = new PoseClipSequence(clip);
		
		Object3D cube = Primitives.getBox(1, 1);
		Animated3D animated = new Animated3D(cube, null, null);
		AnimatedGroup group = new AnimatedGroup(animated);
		
		group.setPoseClipSequence(clipSequence);
		return group;
	}

	@Override
	protected SkeletonDebugger createSkeletonDebugger() throws Exception {
		return null;
	}
	
	@Override
	protected void initialize() throws Exception {
		super.initialize();
		cameraController.cameraRadius = 10;
	}

	public static void main(String[] args) throws Exception {
		new CreatePoseAnimationSample().loop();
	}
}
