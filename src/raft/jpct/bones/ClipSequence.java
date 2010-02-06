package raft.jpct.bones;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.threed.jpct.Animation;

/** <p>
 * A sequence of {@link Clip}s. A ClipSequence is analogue of jPCT's {@link Animation} sequence.
 * A ClipSequence can be assigned to a {@link Skinned3D} and the object can be 
 * directly animated</p>.
 * 
 * @see Skinned3D#setClipSequence(ClipSequence)
 * @see Skinned3D#animateSkin(float)
 * @see Skinned3D#animateSkin(float, int)
 * */
public class ClipSequence implements java.io.Serializable, Iterable<Clip> {
	private static final long serialVersionUID = 1L;
	
	private Clip[] clips;
	private float[] times;

	/**
	 * <p>Creates a ClipSequence out of given clips. All clips should be bound to same skeleton. 
	 * If not, consider using {@link #merge(ClipSequence...)}</p>
	 * 
	 * @see #merge(ClipSequence...)
	 * */
	public ClipSequence(Clip... clips) {
		this(Arrays.asList(clips));
	}
	
	/**
	 * <p>Same as {@link #ClipSequence(Clip...)} but uses a List instead of array.</p>
	 * 
	 * @see #ClipSequence(Clip...)
	 * */
	public ClipSequence(List<Clip> clips) {
		if (clips.isEmpty())
			throw new IllegalArgumentException("no clips");
		
		this.clips = clips.toArray(new Clip[clips.size()]);
		checkSameSkeleton();
		updateTimes();
	}
	
	/** Returns number of clips */
	public int getSize() {
		return clips.length;
	}
	
	/** Returns the specified clip */
	public Clip getClip(int index) {
		return clips[index];
	}
	
	/** return total time of clips in seconds */
	public float getTime() {
		return times[times.length -1];
	}
	
	/** return the skeleton this clip is related to. */
	public Skeleton getSkeleton() {
		return clips[0].getSkeleton();
	}
	
	public void addClip(Clip clip) {
		if (clip.getSkeleton() != getSkeleton()) 
			throw new IllegalArgumentException("Clip has a different skeleton!");
		
		Clip[] newClips = new Clip[clips.length + 1];
		System.arraycopy(clips, 0, newClips, 0, clips.length);
		newClips[clips.length] = clip;
		this.clips = newClips;
		updateTimes();
	}
	
	/** 
	 * <p>applies the clip which corresponds to given seconds. 
	 * given seconds should be in [0,time] range, otherwise clamped.</p>
	 * */
	public void animate(final float seconds, Skeleton.Pose pose) {
		// figure out what frames we are between and by how much
		final int lastClip = clips.length - 1;
		if (seconds < 0 || clips.length == 1) {
			clips[0].applyTo(seconds, pose);
		} else if (seconds >= times[lastClip+1]) {
			clips[lastClip].applyTo(clips[lastClip].getTime(), pose);
		} else {
			int clipIndex = 0;

			for (int i = times.length-1; i > 0; i--) {
				if (times[i] < seconds) {
					clipIndex = i;
					break;
				}
			}
			final float clipTime = seconds - times[clipIndex];
			clips[clipIndex].applyTo(clipTime, pose);
		}
	}
	
	/** throws an exception if all clips does not have same skeleton. */
	private void checkSameSkeleton() {
		Skeleton lastSkeleton = null;
		
		for (Clip clip : clips) {
			if (lastSkeleton == null)
				lastSkeleton = clip.getSkeleton();
			
			if (clip.getSkeleton() != lastSkeleton)
				throw new IllegalArgumentException("all clips should have same Skeleton");
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

	
	/** 
	 * <p>Merges many <code>ClipSequence<code>s into one. This method
	 * does not require all ClipSequences share the same {@link Skeleton}
	 * but skeletons are <i>almost</i> identical.</p>
	 * 
	 * <p>If many ClipSequences are loaded from different files, their
	 * skeleton objects will be different even if they are identical.
	 * This method is meant to help such cases.</p>
	 * 
	 * <p>This method always uses the skeleton of first sequence.</p>
	 * 
	 *  @see SkinnedGroup#mergeSkin(raft.jpct.bones.SkinnedGroup...)
	 *  @see Skinned3D#mergeSkin(Skinned3D...)
	 * */
	public static ClipSequence merge(ClipSequence... sequences) {
		if (sequences.length == 0)
			throw new IllegalArgumentException("no sequences");
		
		// check skeletons match
		for (int i = 0; i < sequences.length; i++) {
			for (int j = i + 1; j < sequences.length; j++) {
				sequences[i].getSkeleton().checkAlmostEqual(sequences[j].getSkeleton());
			}
		}
		// this is the skeleton we will use
		Skeleton skeleton = sequences[0].getSkeleton();
		
		List<Clip> clips = new LinkedList<Clip>();
		
		for (ClipSequence sequence : sequences) {
			for (Clip clip : sequence.clips) {
				clips.add(new Clip(skeleton, clip));
			}
		}
		return new ClipSequence(clips);
	}

	public Iterator<Clip> iterator() {
		return Arrays.asList(clips).iterator();
	}
	
	
}
