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
 * A Clip consists of {@link Channel}s. There is at most one Channel for each joint in Skeleton.
 * A Clip is analogue of sub sequence in jPCT's {@link Animation}.</p>
 * 
 * <p>This class is adapted from <a href="http://www.ardor3d.com">Ardor3D.</a></p>
 * */
public class Clip implements java.io.Serializable, Iterable<Channel> {
	private static final long serialVersionUID = 1L;
	
	private Skeleton skeleton;
	private final Channel[] channels;
	private float maxTime = 0;
	private int size = 0;
	private String name = null;
	
	public Clip(Skeleton skeleton, Channel... channels) {
		this(skeleton);
		
		for (Channel channel : channels) {
			addChannel(channel);
		}
	}
	
	Clip(Skeleton skeleton, List<com.ardor3d.extension.animation.skeletal.JointChannel> jointChannels) {
		this(skeleton);
		
		for (com.ardor3d.extension.animation.skeletal.JointChannel jointChannel : jointChannels) {
			addChannel(new Channel(jointChannel));
		}
	}
	
	Clip(Skeleton skeleton, com.jmex.model.ogrexml.anim.BoneAnimation boneAnimation) {
		this(skeleton);
		setName(boneAnimation.getName());
		
		for (com.jmex.model.ogrexml.anim.BoneTrack track : boneAnimation.getTracks()) {
			addChannel(new Channel(track, skeleton));
		}
	}
	
	Clip(Skeleton skeleton, Clip other) {
		this(skeleton);
		
		for (Channel channel : other.channels) {
			if (channel != null)
				addChannel(channel);
		}
	}
	
	
	private Clip(Skeleton skeleton) {
		this.skeleton = skeleton;
		this.channels = new Channel[skeleton.getNumberOfJoints()];
	}
	
	/** returns time of this clip in seconds. time of clip is the time of the longest channel */
    public float getTime() {
        return maxTime;
    }

    public void addChannel(Channel channel) {
    	if (channels[channel.jointIndex] != null)
    		throw new IllegalStateException("there is already a channel for joint " + channel.jointIndex);
    	
    	channels[channel.jointIndex] = channel;
    	size++;
    	if (channel.getTime() > maxTime)
    		maxTime = channel.getTime();
    }

    public boolean removeChannel(Channel channel) {
    	if (channels[channel.jointIndex] == channel) {
    		channels[channel.jointIndex] = null;
    		size--;
    		updateTime();
    		return true;
    	}
    	return false;
    }
    
	public Skeleton getSkeleton() {
		return skeleton;
	}
    
    /** applies channels in this clip to given {@link Skeleton.Pose}  */
    public void applyTo(float time, Skeleton.Pose pose) {
    	if (skeleton != pose.skeleton)
    		throw new IllegalArgumentException("pose belongs to another skeleton");
    	
    	time = SkinHelper.clamp(0f, maxTime, time);
    	
    	for (Channel channel : channels) {
        	if (channel == null)
        		continue;
        	channel.applyTo(time, pose.getLocal(channel.jointIndex));
    	}
    }
    
    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private void updateTime() {
    	maxTime = 0;
    	
        for (Channel channel : channels) {
        	if (channel == null)
        		continue;
        	float max = channel.getTime();
            if (max > maxTime) {
                maxTime = max;
            }
        }
    }
    
    /** <p>returns an iterator of channels. note some channels may be null.</p> */
	public Iterator<Channel> iterator() {
		return Arrays.asList(channels).iterator();
	}
    
    @Override
    public String toString() {
        return "Clip [channel count=" + size + ", max time=" + maxTime + "]";
    }

}
