package raft.jpct.bones;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.threed.jpct.Matrix;
import com.threed.jpct.SimpleVector;

/** 
 * <p>Contains static helper methods.</p> 
 * */
class SkinHelper {
	
	private SkinHelper() {}
	
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
	public static Matrix getScaleMatrix(float scale) {
		return getScaleMatrix(scale, scale, scale);
	}
	public static Matrix getScaleMatrix(float scaleX, float scaleY, float scaleZ) {
		Matrix m = new Matrix();
		m.set(0, 0, scaleX);
		m.set(1, 1, scaleY);
		m.set(2, 2, scaleZ);
		return m;
	}
	
}
