package raft.jpct.bones;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.threed.jpct.Animation;

/** 
 * <p>
 * A sequence of {@link PoseClip}s. A PoseClipSequence is analogue of jPCT's {@link Animation} sequence 
 * except PoseClipSequence can span multiple objects.
 * A PoseClipSequence can be assigned to a {@link Animated3D} and the object can be 
 * directly animated</p>.
 * 
 * @see Animated3D#setPoseClipSequence(PoseClipSequence)
 * @see Animated3D#animatePose(float, int)
 * 
 * @author hakan eryargi (r a f t)
 * */
public class PoseClipSequence implements java.io.Serializable, Iterable<PoseClip> {
	private static final long serialVersionUID = 1L;

	private PoseClip[] clips;
	private float[] times;

	/**
	 * <p>Creates a PoseClipSequence out of given clips.</p>
	 * */
	public PoseClipSequence(PoseClip... clips) {
		this(Arrays.asList(clips));
	}
	
	/**
	 * <p>Same as {@link #PoseClipSequence(PoseClip...)} but uses a List instead of array.</p>
	 * 
	 * @see #PoseClipSequence(PoseClip...)
	 * */
	public PoseClipSequence(List<PoseClip> clips) {
		if (clips.isEmpty())
			throw new IllegalArgumentException("no clips");
		
		this.clips = clips.toArray(new PoseClip[clips.size()]);
		updateTimes();
	}
	
	/** Returns number of clips */
	public int getSize() {
		return clips.length;
	}
	
	/** Returns the specified clip */
	public PoseClip getClip(int index) {
		return clips[index];
	}
	
	/** return total time of clips in seconds */
	public float getTime() {
		return times[times.length -1];
	}
	
	public void addClip(PoseClip clip) {
		PoseClip[] newClips = new PoseClip[clips.length + 1];
		System.arraycopy(clips, 0, newClips, 0, clips.length);
		newClips[clips.length] = clip;
		this.clips = newClips;
		updateTimes();
	}
	
	void animate(float seconds, AnimatedGroup targetGroup) {
		// figure out what frames we are between and by how much
		final int lastClip = clips.length - 1;
		if (seconds < 0 || clips.length == 1) {
			clips[0].applyTo(seconds, targetGroup, 1f);
		} else if (seconds >= times[lastClip+1]) {
			clips[lastClip].applyTo(clips[lastClip].getTime(), targetGroup, 1f);
		} else {
			int clipIndex = 0;

			for (int i = times.length-1; i > 0; i--) {
				if (times[i] < seconds) {
					clipIndex = i;
					break;
				}
			}
			final float clipTime = seconds - times[clipIndex];
			clips[clipIndex].applyTo(clipTime, targetGroup, 1f);
		}
	}
	
	/** 
	 * <p>applies the clip which corresponds to given seconds. 
	 * given seconds should be in [0,time] range, otherwise clamped.</p>
	 * */
	void animate(final float seconds, Animated3D target, float weight) {
		// figure out what frames we are between and by how much
		final int lastClip = clips.length - 1;
		if (seconds < 0 || clips.length == 1) {
			clips[0].applyTo(seconds, target, weight);
		} else if (seconds >= times[lastClip+1]) {
			clips[lastClip].applyTo(clips[lastClip].getTime(), target, weight);
		} else {
			int clipIndex = 0;

			for (int i = times.length-1; i > 0; i--) {
				if (times[i] < seconds) {
					clipIndex = i;
					break;
				}
			}
			final float clipTime = seconds - times[clipIndex];
			clips[clipIndex].applyTo(clipTime, target, weight);
		}
	}
	
	/** creates and populates times array from scratch. */
	private void updateTimes() {
		this.times = new float[clips.length + 1];
		
		float cumulativeTime = 0f;
		for (int i = 0; i < clips.length; i++) {
			times[i] = cumulativeTime;
			cumulativeTime += clips[i].getTime();
		}
		times[clips.length] = cumulativeTime;
	}

	
    /** <p>Returns an iterator of {@link PoseClip}s.</p> */
	public Iterator<PoseClip> iterator() {
		return Arrays.asList(clips).iterator();
	}
	
	
}
