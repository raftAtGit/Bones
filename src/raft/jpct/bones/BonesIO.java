package raft.jpct.bones;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import com.threed.jpct.Logger;

/** 
 * <p>Contains static loader and saver methods.</p> 
 * 
 * @author hakan eryargi (r a f t)
 * */
public class BonesIO {

	private static final String HEADER_GROUP = "Bones-Group";
	private static final String HEADER_OBJECT = "Bones-Object";
	private static final short VERSION = 3;
	
	static final int NULL = -1;
	static final int NON_NULL = 0;
	
	/** can not be instantiated */
	private BonesIO() {}
	
	/** 
	 * <p>Saves given skin group to given stream.</p>
	 * @see AnimatedGroup 
	 * */
	public static void saveGroup(AnimatedGroup group, OutputStream out) throws IOException {
		ObjectOutputStream oout = new ObjectOutputStream(out);
		writeHeader(oout, HEADER_GROUP);
		group.writeToStream(oout);
		oout.flush();
	}
	
	/** 
	 * <p>Saves given {@link Animated3D} to given stream.</p> 
	 * @see Animated3D 
	 * */
	public static void saveObject(Animated3D object, OutputStream out) throws IOException {
		ObjectOutputStream oout = new ObjectOutputStream(out);
		writeHeader(oout, HEADER_OBJECT);
		object.writeToStream(oout);
		oout.flush();
	}
	
	/** 
	 * <p>Loads an {@link Animated3D} from given stream. Object should be saved to stream
	 * via {@link #saveObject(Animated3D, OutputStream)}</p>
	 *  
	 * @see Animated3D
	 * @see #saveObject(Animated3D, OutputStream) 
	 * */
	public static Animated3D loadObject(InputStream in) throws IOException, ClassNotFoundException {
		if (!(in instanceof BufferedInputStream)) {
			Logger.log("Wrapping input stream in a BufferedInputStream", Logger.MESSAGE);
			in = new BufferedInputStream(in);
		}
		ObjectInputStream oin = new ObjectInputStream(in);
		readHeader(oin, HEADER_OBJECT);
		return new Animated3D(oin);
	}
	
	/** 
	 * <p>Loads an {@link AnimatedGroup} from given stream. Group should be saved to stream
	 * via {@link #saveGroup(AnimatedGroup, OutputStream)}</p>
	 *  
	 * @see AnimatedGroup
	 * @see #saveGroup(AnimatedGroup, OutputStream) 
	 * */
	public static AnimatedGroup loadGroup(InputStream in) throws IOException, ClassNotFoundException {
		if (!(in instanceof BufferedInputStream)) {
			Logger.log("Wrapping input stream in a BufferedInputStream", Logger.MESSAGE);
			in = new BufferedInputStream(in);
		}
		ObjectInputStream oin = new ObjectInputStream(in);
		readHeader(oin, HEADER_GROUP);
		return new AnimatedGroup(oin);
	}

	private static void writeHeader(java.io.ObjectOutputStream out, String header) throws IOException {
		out.writeUTF(header);
		out.writeShort(VERSION);
	}
	
	private static void readHeader(java.io.ObjectInputStream in, String requiredHeader) throws IOException {
		String header = in.readUTF();
		if (!requiredHeader.equals(header))
			throw new IOException("Invalid header: " + header);
		short version = in.readShort();
		if (VERSION != version)
			throw new IOException("Version mismatch. Current version: " + VERSION + ", stream version: " + version);
	}

	
	
//	static void writeFloatArray(ObjectOutputStream out, float[] array) throws IOException {
//		if (array == null) {
//			out.writeInt(NULL);
//		} else {
//			out.writeInt(array.length);
//			for (float val : array) {
//				out.writeFloat(val);
//			}
//		}
//	}
//
//	static void writeFloat2Array(ObjectOutputStream out, float[][] array) throws IOException {
//		if (array == null) {
//			out.writeInt(NULL);
//		} else {
//			out.writeInt(array.length);
//			for (float[] subarray : array) {
//				writeFloatArray(out, subarray);
//			}
//		}
//	}
//	
//	static void writeIntArray(ObjectOutputStream out, int[] array) throws IOException {
//		if (array == null) {
//			out.writeInt(NULL);
//		} else {
//			out.writeInt(array.length);
//			for (int val : array) {
//				out.writeInt(val);
//			}
//		}
//	}
//	
//	static int[] readIntArray(ObjectInputStream in) throws IOException {
//		int size = in.readInt();
//		if (size == NULL)
//			return null;
//		
//		int[] array = new int[size];
//		for (int i = 0; i < size; i++) {
//			array[i] = in.readInt();
//		}
//		return array;
//	}
//	
//	
//	static void writeShortArray(ObjectOutputStream out, short[] array) throws IOException {
//		if (array == null) {
//			out.writeInt(NULL);
//		} else {
//			out.writeInt(array.length);
//			for (short val : array) {
//				out.writeShort(val);
//			}
//		}
//	}
//	
//	static void writeShort2Array(ObjectOutputStream out, short[][] array) throws IOException {
//		if (array == null) {
//			out.writeInt(NULL);
//		} else {
//			out.writeInt(array.length);
//			for (short[] subarray : array) {
//				writeShortArray(out, subarray);
//			}
//		}
//	}
//	
//	static void writeMatrix(ObjectOutputStream out, Matrix matrix) throws IOException {
//		if (matrix == null) {
//			out.writeInt(NULL);
//		} else {
//			out.writeInt(NON_NULL);
//			float[] dump = matrix.getDump();
//			for (float val : dump) {
//				out.writeFloat(val);
//			}
//		}
//	}
//	
//	static Matrix readMatrix(ObjectInputStream in) throws IOException {
//		if (in.readInt() == NULL)
//			return null;
//		
//		float[] dump = new float[16];
//		for (int i = 0; i < 16; i++) {
//			dump[i] = in.readFloat();
//		}
//		Matrix matrix = new Matrix();
//		matrix.setDump(dump);
//		return matrix;
//	}
//	
//	static float[] readFloatArray(ObjectInputStream in) throws IOException {
//		int size = in.readInt();
//		if (size == NULL)
//			return null;
//		
//		float[] array = new float[size];
//		for (int i = 0; i < size; i++) {
//			array[i] = in.readFloat();
//		}
//		return array;
//	}
//	
//	static float[][] readFloat2Array(ObjectInputStream in) throws IOException {
//		int size = in.readInt();
//		if (size == NULL)
//			return null;
//		
//		float[][] array = new float[size][];
//		for (int i = 0; i < size; i++) {
//			array[i] = readFloatArray(in);
//		}
//		return array;
//	}
//	
//	static short[] readShortArray(ObjectInputStream in) throws IOException {
//		int size = in.readInt();
//		if (size == NULL)
//			return null;
//		
//		short[] array = new short[size];
//		for (int i = 0; i < size; i++) {
//			array[i] = in.readShort();
//		}
//		return array;
//	}
//	
//	static short[][] readShort2Array(ObjectInputStream in) throws IOException {
//		int size = in.readInt();
//		if (size == NULL)
//			return null;
//		
//		short[][] array = new short[size][];
//		for (int i = 0; i < size; i++) {
//			array[i] = readShortArray(in);
//		}
//		return array;
//	}
//
//	static void writeString(ObjectOutputStream out, String s) throws IOException {
//		if (s == null) {
//			out.writeInt(NULL);
//		} else {
//			out.writeInt(s.length());
//			
//			for (char c : s.toCharArray()) {
//				out.writeChar(c);
//			}
//		}
//	}
//	
//	static String readString(ObjectInputStream in) throws IOException {
//		int size = in.readInt();
//		if (size == NULL)
//			return null;
//		
//		char[] chars = new char[size];
//		for (int i = 0; i < size; i++) {
//			chars[i] = in.readChar();
//		}
//		return new String(chars);
//	}
//
//	static void writeSimpleVectorArray(ObjectOutputStream out, SimpleVector[] array) throws IOException {
//		if (array == null) {
//			out.writeInt(NULL);
//		} else {
//			out.writeInt(array.length);
//			for (SimpleVector vector : array) {
//				writeSimpleVector(out, vector);
//			}
//		}
//	}
//	
//	static SimpleVector[] readSimpleVectorArray(ObjectInputStream in) throws IOException {
//		int size = in.readInt();
//		if (size == NULL)
//			return null;
//		
//		SimpleVector[] array = new SimpleVector[size];
//		for (int i = 0; i < size; i++) {
//			array[i] = readSimpleVector(in);
//		}
//		return array;
//	}
//	
//	static void writeSimpleVector(ObjectOutputStream out, SimpleVector vector) throws IOException {
//		if (vector == null) {
//			out.writeInt(NULL);
//		} else {
//			out.writeInt(NON_NULL);
//			out.writeFloat(vector.x);
//			out.writeFloat(vector.y);
//			out.writeFloat(vector.z);
//		}
//	}
//	
//	static SimpleVector readSimpleVector(ObjectInputStream in) throws IOException {
//		if (in.readInt() == NULL)
//			return null;
//		
//		return new SimpleVector(in.readFloat(), in.readFloat(), in.readFloat());
//	}
//	
//	static void writeQuaternionArray(ObjectOutputStream out, Quaternion[] array) throws IOException {
//		if (array == null) {
//			out.writeInt(NULL);
//		} else {
//			out.writeInt(array.length);
//			for (Quaternion quat : array) {
//				writeQuaternion(out, quat);
//			}
//		}
//	}
//	
//	static Quaternion[] readQuaternionArray(ObjectInputStream in) throws IOException {
//		int size = in.readInt();
//		if (size == NULL)
//			return null;
//		
//		Quaternion[] array = new Quaternion[size];
//		for (int i = 0; i < size; i++) {
//			array[i] = readQuaternion(in);
//		}
//		return array;
//	}
//	
//	static void writeQuaternion(ObjectOutputStream out, Quaternion quat) throws IOException {
//		if (quat == null) {
//			out.writeInt(NULL);
//		} else {
//			out.writeInt(NON_NULL);
//			out.writeFloat(quat.x);
//			out.writeFloat(quat.y);
//			out.writeFloat(quat.z);
//			out.writeFloat(quat.w);
//		}
//	}
//	
//	static Quaternion readQuaternion(ObjectInputStream in) throws IOException {
//		if (in.readInt() == NULL)
//			return null;
//		
//		return new Quaternion(in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat());
//	}
//	
}
