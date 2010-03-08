/**
 * Some of the classes in this file are adapted or inspired from Ardor3D. 
 * Such classes are indicated in class javadocs. 
 * 
 * Modification and redistribution of them may be subject to Ardor3D's license: 
 * http://www.ardor3d.com/LICENSE
 */
package raft.jpct.bones;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.threed.jpct.Animation;

/** 
 * <p>A single animation for a {@link Skeleton}. 
 * A Clip consists of {@link JointChannel}s. There is at most one Channel for each joint in Skeleton.
 * A Clip is analogue of sub sequence in jPCT's {@link Animation}.</p>
 * 
 * <p>This class is adapted from <a href="http://www.ardor3d.com">Ardor3D.</a></p>
 * */
public class SkinClip implements java.io.Serializable, Iterable<JointChannel> {
	private static final long serialVersionUID = 1L;
	
	private Skeleton skeleton;
	private final JointChannel[] channels;
	private float maxTime = 0;
	private int size = 0;
	private String name = null;
	
	public SkinClip(Skeleton skeleton, JointChannel... channels) {
		this(skeleton);
		
		for (JointChannel channel : channels) {
			addChannel(channel);
		}
	}
	
	public SkinClip(Skeleton skeleton, List<JointChannel> channels) {
		this(skeleton);
		
		for (JointChannel channel : channels) {
			addChannel(channel);
		}
	}
	
	SkinClip(Skeleton skeleton, SkinClip other) {
		this(skeleton);
		
		for (JointChannel channel : other.channels) {
			if (channel != null)
				addChannel(channel);
		}
	}
	
	private SkinClip(Skeleton skeleton) {
		this.skeleton = skeleton;
		this.channels = new JointChannel[skeleton.getNumberOfJoints()];
	}
	
	/** returns time of this clip in seconds. time of clip is the time of the longest channel */
    public float getTime() {
        return maxTime;
    }

    public void addChannel(JointChannel channel) {
    	if (channels[channel.jointIndex] != null)
    		throw new IllegalStateException("there is already a channel for joint " + channel.jointIndex);
    	
    	channels[channel.jointIndex] = channel;
    	size++;
    	if (channel.getTime() > maxTime)
    		maxTime = channel.getTime();
    }

    public boolean removeChannel(JointChannel channel) {
    	if (channels[channel.jointIndex] == channel) {
    		channels[channel.jointIndex] = null;
    		size--;
    		updateTime();
    		return true;
    	}
    	return false;
    }
    
    /** Returns the {@link Skeleton} this clip is related to. */
	public Skeleton getSkeleton() {
		return skeleton;
	}
    
    /** applies channels in this clip to given {@link SkeletonPose}  */
    void applyTo(float time, SkeletonPose pose) {
    	if (skeleton != pose.skeleton)
    		throw new IllegalArgumentException("pose belongs to another skeleton");
    	
    	time = SkinHelper.clamp(0f, maxTime, time);
    	
    	for (JointChannel channel : channels) {
        	if (channel == null)
        		continue;
        	channel.applyTo(time, pose.getLocal(channel.jointIndex));
    	}
    }
    
    /** Returns name of this clip. May be null */
    public String getName() {
		return name;
	}

    /** Sets name of this clip. */
	public void setName(String name) {
		this.name = name;
	}

	private void updateTime() {
    	maxTime = 0;
    	
        for (JointChannel channel : channels) {
        	if (channel == null)
        		continue;
        	float max = channel.getTime();
            if (max > maxTime) {
                maxTime = max;
            }
        }
    }
    
    /** <p>Returns an iterator of channels. Note some channels may be null.</p> */
	public Iterator<JointChannel> iterator() {
		return Arrays.asList(channels).iterator();
	}
    
    @Override
    public String toString() {
        return "Clip [channel count=" + size + ", max time=" + maxTime + "]";
    }

}
