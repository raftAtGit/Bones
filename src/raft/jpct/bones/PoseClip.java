package raft.jpct.bones;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.threed.jpct.Animation;


/**
 * <p>A single animation in pose animation. May be related to single or multiple objects. 
 * A PoseClip consists of {@link MeshChannel}s. There is at most one MeshChannel for each {@link Animated3D} in object group.
 * A PoseClip is analogue of sub sequence in jPCT's {@link Animation} except a pose clip can span multiple objects.</p>
 * 
 * 
 * @author hakan eryargi (r a f t)
 */
public class PoseClip implements java.io.Serializable, Iterable<MeshChannel>  {
	private static final long serialVersionUID = 1L;
	
	private final MeshChannel[] channels;
	private float maxTime = 0;
	private int size = 0;
	private String name = null;
	
	public PoseClip(int groupSize, MeshChannel... channels) {
		this(groupSize);
		
		for (MeshChannel channel : channels) {
			addChannel(channel);
		}
	}
	
	public PoseClip(int groupSize, List<MeshChannel> channels) {
		this(groupSize);
		
		for (MeshChannel channel : channels) {
			addChannel(channel);
		}
	}
	
	private PoseClip(int groupSize) {
		this.channels = new MeshChannel[groupSize];
	} 
	
	/** returns time of this clip in seconds. time of clip is the time of the longest channel */
    public float getTime() {
        return maxTime;
    }

    public void addChannel(MeshChannel channel) {
    	if (channels[channel.objectIndex] != null)
    		throw new IllegalStateException("there is already a channel for joint " + channel.objectIndex);
    	
    	channels[channel.objectIndex] = channel;
    	size++;
    	if (channel.getTime() > maxTime)
    		maxTime = channel.getTime();
    }

    public boolean removeChannel(MeshChannel channel) {
    	if (channels[channel.objectIndex] == channel) {
    		channels[channel.objectIndex] = null;
    		size--;
    		updateTime();
    		return true;
    	}
    	return false;
    }
    /** applies channels in this clip to given {@link SkeletonPose}  */
    void applyTo(float time, AnimatedGroup targetGroup, float weight) {
    	time = SkinHelper.clamp(0f, maxTime, time);
    	
    	for (MeshChannel channel : channels) {
        	if (channel == null)
        		continue;
        	Animated3D target = targetGroup.get(channel.objectIndex);
        	channel.applyTo(time, target, weight);
    	}
    }
    
    /** applies channels in this clip to given {@link SkeletonPose}  */
    void applyTo(float time, Animated3D target, float weight) {
    	MeshChannel channel = channels[target.getIndex()];
    	if (channel == null)
    		return;
    	time = SkinHelper.clamp(0f, maxTime, time);
    	channel.applyTo(time, target, weight);
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
    	
        for (MeshChannel channel : channels) {
        	if (channel == null)
        		continue;
        	float max = channel.getTime();
            if (max > maxTime) {
                maxTime = max;
            }
        }
    }
    
    /** <p>Returns an iterator of channels. Note some channels may be null.</p> */
	public Iterator<MeshChannel> iterator() {
		return Arrays.asList(channels).iterator();
	}
    
    @Override
    public String toString() {
        return "PoseClip [channel count=" + size + ", max time=" + maxTime + "]";
    }

}
