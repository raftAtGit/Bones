package raft.jpct.bones;

/**
 * <p>A key frame in pose animation. PoseFrame is defined as an list of {@link MeshPose}s and their respective weights.</p>
 * 
 * <p>This class is originally adapted from <a href="http://www.jmonkeyengine.com" target="_blank">jME.</a></p>
 */
public class PoseFrame implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	
    final MeshPose[] poses;
    final float[] weights;
	
    public PoseFrame(MeshPose[] poses, float[] weights) {
    	this(poses.length);
    	
		if (poses.length != weights.length)
            throw new IllegalArgumentException("Poses and weights must be same length!");

        for (int i = 0; i < poses.length; i++) {
        	this.poses[i] = poses[i];
        	this.weights[i] = weights[i];
        }
    	
    }
    
    private PoseFrame(int length) {
    	this.poses = new MeshPose[length];
    	this.weights = new float[length];
    }
}
