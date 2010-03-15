package raft.jpct.bones;

import java.util.Arrays;

/** 
 * <p>Pose animation data related to a single mesh. A PoseChannel consists of a
 * series of {@link PoseFrame}s and their respective times.</p>
 *
 * @author hakan eryargi (r a f t)
 */
public class MeshChannel implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	
	final int objectIndex;
	
    private final PoseFrame[] frames;
    private final float[] times;
	
    /**
     * <p>Creates a new MeshChannel out of given data. The arrays must be same length.<p>
     * 
     * @param objectIndex index of {@link Animated3D} in {@link AnimatedGroup} this channel is related to 
     * */
    public MeshChannel(int objectIndex, PoseFrame[] frames, float[] times) {
    	this(objectIndex, times.length);
    	
    	if (times.length != frames.length)
    		throw new IllegalArgumentException("The arrays must be same length");
    	
        for (int i = 0; i < times.length; i++) {
        	this.frames[i] = frames[i];
        	this.times[i] = times[i];
        }
        validateTimes();
    }
    
    private MeshChannel(int objectIndex, int length) {
		if (objectIndex < 0)
			throw new IllegalArgumentException("jointIndex: " + objectIndex);
    	if (length < 1)
    		throw new IllegalArgumentException("length: " + length); 
    	
    	this.objectIndex = objectIndex;
    	this.frames = new PoseFrame[length];
    	this.times = new float[length];
    }
    
	/** returns the index of object in group this channel is related to. */
	public int getObjectIndex() {
		return objectIndex;
	}
    
	/** returns length of this channel in seconds */
	public float getTime() {
		return times[times.length - 1];
	}
	
	/** returns number of samples in this channel */
	public int getLength() {
		return times.length;
	}
	
	void applyTo(float seconds, Animated3D target, float weight) {
		// figure out what frames we are between and by how much
		final int lastFrame = times.length - 1;
		if (seconds <= 0 || times.length == 1) {
			applyFrame(0, weight, target);
		} else if (seconds >= times[lastFrame]) {
			applyFrame(lastFrame, weight, target);
		} else {
			int startFrame = times.length - 2;

			for (int i = times.length - 2; i >= 0 ; i--) {
				if (times[i] < seconds) {
					startFrame = i;
					break;
				}
			}
            int endFrame = startFrame + 1;
            float blend = (seconds - times[startFrame]) / (times[endFrame] - times[startFrame]);
            
            applyFrame(startFrame, weight*(1-blend), target);
            applyFrame(endFrame, weight*blend, target);
		}
	}
	
    void applyFrame(int frameIndex, float weight, Animated3D target){
        PoseFrame frame = frames[frameIndex];

        for (int i = 0; i < frame.poses.length; i++){
            MeshPose pose = frame.poses[i];
            float poseWeight = frame.weights[i] * weight;

            pose.apply(poseWeight, target);
        }
    }
	
    /** check time values are valid */
	private void validateTimes() {
		float last = -1f;
		for (float time : times) {
			if (time < 0)
				throw new IllegalArgumentException("Negative time: " + time);
			if (time < last)
				throw new IllegalArgumentException("Time values not incremental: " + time + " > " + last 
						+ "\n" + Arrays.toString(times));
			last = time;
		}
	}

}
