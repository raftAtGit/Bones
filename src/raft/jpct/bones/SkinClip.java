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
 * 
 * @see JointChannel
 * */
public class SkinClip implements java.io.Serializable, Iterable<JointChannel> {
	private static final long serialVersionUID = 1L;
	
	private Skeleton skeleton;
	private final JointChannel[] channels;
	private float maxTime = 0;
	private int size = 0;
	private String name = null;
	
	/**
	 * <p>Creates a new SkinClip.</p>
	 *  
	 * @param skeleton the skeleton this clip is related to
	 * @param channels the channels   
	 * */
	public SkinClip(Skeleton skeleton, JointChannel... channels) {
		this(skeleton);
		
		for (JointChannel channel : channels) {
			addChannel(channel);
		}
	}
	
	/**
	 * <p>Creates a new SkinClip. Same as {@link #SkinClip(Skeleton, JointChannel...)} but uses a List instead of an array.</p>
	 *  
	 * @param skeleton the skeleton this clip is related to
	 * @param channels the channels   
	 * */
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
	
//	private SkinClip(ObjectInputStream in) throws IOException, ClassNotFoundException {
//		this.skeleton = (Skeleton) in.readObject();
//		this.maxTime = in.readFloat();
//		this.size = in.readInt();
//		this.name = BonesIO.readString(in);
//		
//		int length = in.readInt();
//		this.channels = new JointChannel[length];
//		for (int i = 0; i < length; i++) {
//			channels[i] = JointChannel.readFromStream(in); 
//		}
//	}

	/** returns time of this clip in seconds. time of clip is the time of the longest channel */
    public float getTime() {
        return maxTime;
    }

    /** 
     * <p>Adds a new channel.</p>
     * 
     * @throws IllegalStateException if there is already a channel related to joint
     * */
    public void addChannel(JointChannel channel) {
    	if (channels[channel.jointIndex] != null)
    		throw new IllegalStateException("there is already a channel for joint " + channel.jointIndex);
    	
    	channels[channel.jointIndex] = channel;
    	size++;
    	if (channel.getTime() > maxTime)
    		maxTime = channel.getTime();
    }

    /** 
     * <p>Removes a channel.</p> */
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
    
    /** <p>Applies channels in this clip to given {@link SkeletonPose}</p>  */
    public void applyTo(float time, SkeletonPose pose) {
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
    
	/** Returns string representation. */
    @Override
    public String toString() {
        return "SkinClip [channel count=" + size + ", max time=" + maxTime + "]";
    }

	void replaceSkeleton(Skeleton skeleton) {
		this.skeleton.checkAlmostEqual(skeleton);
		this.skeleton = skeleton;
	}
	
//	static SkinClip readFromStream(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
//		if (in.readInt() == BonesIO.NULL)
//			return null;
//		return new SkinClip(in);
//	}
//	
//	static void writeToStream(SkinClip object, java.io.ObjectOutputStream out) throws IOException {
//		if (object == null) {
//			out.writeInt(BonesIO.NULL);
//		} else {
//			out.writeInt(BonesIO.NON_NULL);
//			
//			out.writeObject(object.skeleton);
//			out.writeFloat(object.maxTime);
//			out.writeInt(object.size);
//			
//			BonesIO.writeString(out, object.name);
//			
//			out.writeInt(object.channels.length);
//			for (JointChannel channel : object.channels) {
//				JointChannel.writeToStream(channel, out);
//			}
//		}
//	}
	
}
