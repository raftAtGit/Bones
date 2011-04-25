/**
 * Some of the classes in this file are adapted or inspired from Ardor3D. 
 * Such classes are indicated in class javadocs. 
 * 
 * Modification and redistribution of them may be subject to Ardor3D's license: 
 * http://www.ardor3d.com/LICENSE
 */
package raft.jpct.bones;

import java.io.IOException;
import java.util.Arrays;

import com.threed.jpct.Matrix;
import com.threed.jpct.SimpleVector;

/** 
 * <p>Skeletal animation data related to a single {@link Joint}.</p>
 * 
 * <p>Channel data is in Joint's local space and directly applied to local transform
 * of {@link SkeletonPose} related to Joint.
 * </p>
 * 
 * <p>This class is originally adapted from <a href="http://www.ardor3d.com">Ardor3D.</a></p>
 * 
 * @see SkinClip
 * @see Joint
 * */
public class JointChannel implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	final int jointIndex;
	
	private final float[] times;
	private final Quaternion[] rotations;
	private final SimpleVector[] translations;
    private final SimpleVector[] scales;
	
    private transient Quaternion tmpRotation = new Quaternion();
    private transient SimpleVector tmpTranslation = new SimpleVector();
    private transient SimpleVector tmpScale = new SimpleVector();
    private transient Matrix tmpScaleMatrix = new Matrix();
    
    /**
     * <p>Creates a new JointChannel out of given data. The arrays must be same length.<p>
     * 
     * @param jointIndex index of {@link Joint} in {@link Skeleton} this channel is related to 
     * */
    public JointChannel(int jointIndex, float[] times, SimpleVector[] translations, 
    		Quaternion[] rotations, SimpleVector[] scales) {

    	this(jointIndex, times.length);
    	
        if (rotations.length != times.length || translations.length != times.length || scales.length != times.length) {
            throw new IllegalArgumentException("All provided arrays must be same length!");
        }
        
        for (int i = 0; i < times.length; i++) {
        	this.times[i] = times[i];
        	this.translations[i] = new SimpleVector(translations[i]);
        	this.rotations[i] = new Quaternion(rotations[i]);
        	this.scales[i] = new SimpleVector(scales[i]);
        }
        validateTimes();
    }
    
	private JointChannel(int jointIndex, int length) {
		if (jointIndex < 0)
			throw new IllegalArgumentException("jointIndex: " + jointIndex);
    	if (length < 1)
    		throw new IllegalArgumentException("length: " + length); 
		
		this.jointIndex = jointIndex; 
		this.times = new float[length];
		this.rotations = new Quaternion[length];
		this.translations = new SimpleVector[length];
		this.scales = new SimpleVector[length];
	}
	
//	private JointChannel(ObjectInputStream in) throws IOException {
//		this.jointIndex = in.readShort();
//		this.times = BonesIO.readFloatArray(in);
//		this.rotations = BonesIO.readQuaternionArray(in);
//		this.translations = BonesIO.readSimpleVectorArray(in);
//		this.scales = BonesIO.readSimpleVectorArray(in);
//	}

	/** returns the index of joint this channel is related to. */
	public int getJointIndex() {
		return jointIndex;
	}
	
	/** returns length of this channel in seconds */
	public float getTime() {
		return times[times.length - 1];
	}
	
	/** returns number of samples in this channel */
	public int getLength() {
		return times.length;
	}
	
	/** 
	 * applies channel data to given matrix. given seconds should be in [0,time] range, otherwise clamped.  
	 * */
	void applyTo(final float seconds, final Matrix target) {
		// figure out what frames we are between and by how much
		final int lastFrame = times.length - 1;
		if (seconds <= 0 || times.length == 1) {
			applyTo(0, target);
		} else if (seconds >= times[lastFrame]) {
			applyTo(lastFrame, target);
		} else {
			int startFrame = times.length - 2;

			for (int i = times.length - 2; i >= 0 ; i--) {
				if (times[i] < seconds) {
					startFrame = i;
					break;
				}
			}
			final float progressPercent = (seconds - times[startFrame])
					/ (times[startFrame + 1] - times[startFrame]);

			applyTo(startFrame, progressPercent, target);
		}
	}
	
	/** applies channel data to given matrix. index should be [0,length) range otherwise 
	 * it will be clamped
	 * */
    void applyTo(int sampleIndex, Matrix target) {
    	sampleIndex = SkinHelper.clamp(0, times.length-1, sampleIndex);
    	applyToMatrix(rotations[sampleIndex], translations[sampleIndex], scales[sampleIndex], target);
    }
    
    
	/** 
	 * applies interpolated channel data to given matrix. 
	 * index should be [0,length-1) range otherwise 
	 * an ArrayIndexOutOfBoundsException will be thrown
	 *  
	 * @throws ArrayIndexOutOfBoundsException */
    void applyTo(final int sampleIndex, final float progressPercent, final Matrix target) {
        // shortcut
        if (progressPercent == 0.0f) {
            applyTo(sampleIndex, target);
            return;
        } else if (progressPercent == 1.0f) {
            applyTo(sampleIndex + 1, target);
            return;
        }

        // apply linear interpolation
        tmpRotation.slerp(rotations[sampleIndex], rotations[sampleIndex + 1], progressPercent);
        SkinHelper.interpolate(translations[sampleIndex], translations[sampleIndex + 1], tmpTranslation, progressPercent);
        SkinHelper.interpolate(scales[sampleIndex], scales[sampleIndex + 1], tmpScale, progressPercent);

        applyToMatrix(tmpRotation, tmpTranslation, tmpScale, target);
    }
    
    private void applyToMatrix(Quaternion rotation, SimpleVector translation, SimpleVector scale, Matrix target) {
    	target.setIdentity();
    	rotation.setRotation(target);
    	SkinHelper.setTranslation(target, translation);
    	//target.matMul(SkinHelper.getScaleMatrix(this.scale));

    	if ((scale.x != 1) || (scale.y != 1) || (scale.z != 1)) {
	    	tmpScaleMatrix.set(0, 0, scale.x);
	    	tmpScaleMatrix.set(1, 1, scale.y);
	    	tmpScaleMatrix.set(2, 2, scale.z);
	    	// TODO correct ?
	    	target.matMul(tmpScaleMatrix);
	    	//System.out.println(scale);
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

	/** rotates channel data. this method should be called before skeleton itself is rotated.
	 * the conversion done here cannot be combined with a scaling, so this method
	 * differs from its variants in {@link MeshPose} and {@link Skeleton}. */
	void rotate(Skeleton skeleton, Quaternion rotation) {
		Matrix transform = rotation.getRotationMatrix();
		
		Joint joint = skeleton.getJoint(jointIndex);
		Joint parent = joint.hasParent() ? skeleton.getJoint(joint.getParentIndex()) : null;
		
		for (int i = 0; i < times.length; i++) {
			Matrix frame = new Matrix();
			rotations[i].setRotation(frame);
			frame.translate(translations[i]);
			
			// take to object space
			if (joint.hasParent())
				frame.matMul(parent.bindPose);
			
			// do transform
			frame.matMul(transform);

			// take back to local space in parent's new space
			if (joint.hasParent()) {
				Matrix bindPose = new Matrix(parent.bindPose);
				bindPose.matMul(transform);
				frame.matMul(bindPose.invert());
			}
		
			rotations[i] = new Quaternion(frame);
			translations[i] = frame.getTranslation();
		}
	}

	/** scales channel data. ie: scales translation data */
	void scale(float scale) {
		for (int i = 0; i < times.length; i++) {
			translations[i].scalarMul(scale);
		}		
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		
	    tmpRotation = new Quaternion();
	    tmpTranslation = new SimpleVector();
	    tmpScale = new SimpleVector();
	    tmpScaleMatrix = new Matrix();
	}
	
//	static JointChannel readFromStream(java.io.ObjectInputStream in) throws IOException {
//		if (in.readInt() == BonesIO.NULL)
//			return null;
//		return new JointChannel(in);
//	}
//	
//	static void writeToStream(JointChannel object, java.io.ObjectOutputStream out) throws IOException {
//		if (object == null) {
//			out.writeInt(BonesIO.NULL);
//		} else {
//			out.writeInt(BonesIO.NON_NULL);
//			
//			out.writeShort(object.jointIndex);
//			BonesIO.writeFloatArray(out, object.times);
//			BonesIO.writeQuaternionArray(out, object.rotations);
//			BonesIO.writeSimpleVectorArray(out, object.translations);
//			BonesIO.writeSimpleVectorArray(out, object.scales);
//			
//		}
//	}
	
}
