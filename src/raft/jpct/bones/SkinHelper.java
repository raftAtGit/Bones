package raft.jpct.bones;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Transform;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.threed.jpct.Matrix;
import com.threed.jpct.SimpleVector;

/** 
 * <p>Contains static helper methods.</p> 
 * */
class SkinHelper {
	
	private SkinHelper() {}
	
	/** converts a transform matrix to a jPCT Matrix. rotation and translation information is retrieved. */
	public static Matrix getMatrix(Matrix4 m4) {
		Matrix m = new Matrix();
		
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				m.set(j, i, m4.getValuef(i, j));
			}
		}
		m.translate(m4.getValuef(0, 3), m4.getValuef(1, 3), m4.getValuef(2, 3));
		return m;
	}
	
	
	/** converts a transform to a jPCT Matrix. rotation and translation information is retrieved. */
	public static Matrix getMatrix(ReadOnlyTransform transform) {
		return getMatrix(transform.getHomogeneousMatrix(null));
	}
	
	/** clears translation information in given matrix */
	public static void clearTranslation(Matrix m) {
    	m.set(3, 0, 0);
    	m.set(3, 1, 0);
    	m.set(3, 2, 0);
	}
	
	/** sets translation information in given matrix to given vector */
	public static void setTranslation(Matrix m, SimpleVector v) {
    	m.set(3, 0, v.x);
    	m.set(3, 1, v.y);
    	m.set(3, 2, v.z);
	}
	
	/** converts a FloatBuffer to an array */
	public static float[] asArray(FloatBuffer buffer) {
        if (buffer.hasArray()) {
        	return buffer.array(); 
        } else {
        	float[] data = new float[buffer.limit()];
        	buffer.rewind();
        	buffer.get(data);
        	return data;
        } 
	}
	
	/** converts a FloatBuffer to an array */
	public static byte[] asArray(ByteBuffer buffer) {
        if (buffer.hasArray()) {
        	return buffer.array(); 
        } else {
        	byte[] data = new byte[buffer.limit()];
        	buffer.rewind();
        	buffer.get(data);
        	return data;
        } 
	}
	
	/** converts a FloatBuffer to an array */
	public static float[][] asArray(FloatBuffer buffer, int subArrayLength) {
		if (subArrayLength < 1)
			throw new IllegalArgumentException("subArrayLength: " + subArrayLength);
		float[] array = asArray(buffer);
		float[][] result = new float[array.length / subArrayLength][subArrayLength];
		for (int i = 0; i < result.length; i++) {
			int baseIndex = i * subArrayLength;
			for (int j = 0; j < subArrayLength; j++) {
				result[i][j] = array[baseIndex + j];
			}
		}
		return result;
	}
	
	/** converts an IntBuffer to an array */
	public static int[] asArray(IntBuffer buffer) {
        if (buffer.hasArray()) {
        	return buffer.array(); 
        } else {
        	int[] data = new int[buffer.limit()];
        	buffer.rewind();
        	buffer.get(data);
        	return data;
        } 
	}
	
	/** converts a ShortBuffer to an array */
	public static short[] asArray(ShortBuffer buffer) {
        if (buffer.hasArray()) {
        	return buffer.array(); 
        } else {
        	short[] data = new short[buffer.limit()];
        	buffer.rewind();
        	buffer.get(data);
        	return data;
        } 
	}
	
	/** converts a ShortBuffer to an array of arrays */
	public static short[][] asArray(ShortBuffer buffer, int subArrayLength) {
		if (subArrayLength < 1)
			throw new IllegalArgumentException("subArrayLength: " + subArrayLength);
		short[] array = asArray(buffer);
		short[][] result = new short[array.length / subArrayLength][subArrayLength];
		for (int i = 0; i < result.length; i++) {
			int baseIndex = i * subArrayLength;
			for (int j = 0; j < subArrayLength; j++) {
				result[i][j] = array[baseIndex + j];
			}
		}
		return result;
	}

	/** converts a ShortBuffer to an array of arrays */
	public static short[][] asShortArray(ByteBuffer buffer, int subArrayLength) {
		if (subArrayLength < 1)
			throw new IllegalArgumentException("subArrayLength: " + subArrayLength);
		byte[] array = asArray(buffer);
		short[][] result = new short[array.length / subArrayLength][subArrayLength];
		for (int i = 0; i < result.length; i++) {
			int baseIndex = i * subArrayLength;
			for (int j = 0; j < subArrayLength; j++) {
				result[i][j] = (short)array[baseIndex + j];
			}
		}
		return result;
	}
	
	/** returns a rotation matrix to rotate given <i>from</i> vector to <i>to</i> vector */
	public static Matrix getRotationFor(SimpleVector from, SimpleVector to) {
		Matrix rotation = from.getRotationMatrix().invert3x3();
		rotation.matMul(to.getRotationMatrix());
		return rotation;
	}
	
	/** creates an ardor Transform out of given jPCT matrix */
	public static Transform getTransform(Matrix m) {
		Transform t = new Transform();
		
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				((Matrix3)t.getMatrix()).setValue(i, j, m.get(j, i));
			}
		}
		
		SimpleVector translation = m.getTranslation();
		t.translate(translation.x, translation.y, translation.z);
		
		return t;
	}

	/** converts Ardor3D Vector3 to jPCT SimpleVector */
	public static SimpleVector getVector(com.ardor3d.math.Vector3 vector3) {
		return new SimpleVector(vector3.getXf(), vector3.getYf(), vector3.getZf());
	}
	
	/** converts jME Vector3f to jPCT SimpleVector */
	public static SimpleVector getVector(com.jme.math.Vector3f vector3) {
		return new SimpleVector(vector3.x, vector3.y, vector3.z);
	}
	
    /** interpolates one and two and sets result in given result vector.
     *@param weight how much vector two should contribute to result */
    public static final SimpleVector interpolate(SimpleVector one, SimpleVector two, SimpleVector result, float weight) {
    	if (result == null)
    		result = new SimpleVector();
    	
        float x = one.x * (1-weight) + two.x * weight;
        float y = one.y * (1-weight) + two.y * weight;
        float z = one.z * (1-weight) + two.z * weight;

        result.set(x, y, z);
        return result;
    }

    /** interpolates one and two and returns a new created vector */
    public static final SimpleVector interpolate(SimpleVector one, SimpleVector two, float weight) {
        return interpolate(one, two, new SimpleVector(), weight);
    }

	public static float clamp(float min, float max, float value) {
		if (min > max)
			throw new IllegalArgumentException(min + " !<= " + max);
		if (value < min)
			return min;
		if (value > max)
			return max;
		return value;
	}
	
	public static int clamp(int min, int max, int value) {
		if (min > max)
			throw new IllegalArgumentException(min + " !<= " + max);
		if (value < min)
			return min;
		if (value > max)
			return max;
		return value;
	}
	
    /**
     * Constructs a new quaternion from ardor quaternion
     */
	public static Quaternion convertQuaternion(com.ardor3d.math.Quaternion quat) {
		return new Quaternion(quat.getXf(), quat.getYf(), quat.getZf(), quat.getWf());
    }

    /**
     * Constructs a new quaternion from jME quaternion
     */
	public static Quaternion convertQuaternion(com.jme.math.Quaternion quat) {
		return new Quaternion(quat.x, quat.y, quat.z, quat.w);
    }
	
}
