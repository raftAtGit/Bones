package raft.jpct.bones;

import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.SimpleVector;


/** 
 * <p>Place holder for mesh coordinates.</p>
 * 
 * @author hakan eryargi (r a f t)
 */
public class MeshData implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	
	final float[] coordinates;
	final float[] uvs;
	final int[] indices;
	
	/** <p>Creates a new MeshData out of given data. Arrays are copied to ensure encapsulation. 
	 * Coordinates array must be given while others may be null.</p>
	 * 
	 * @see Object3D#Object3D(float[], float[], int[], int)
	  */
	public MeshData(float[] coordinates, float[] uvs, int[] indices) {
		if (indices == null) {
			if ((coordinates.length % 9) != 0)
				throw new IllegalArgumentException("coordinates length should be a multiple of 9 for non-indexed geometry");
		} else {
			if ((coordinates.length % 3) != 0)
				throw new IllegalArgumentException("coordinates length should be a multiple of 3");
			
			for (int index : indices) {
				if ((index + 1) * 3 > coordinates.length) 
					throw new IllegalArgumentException("index: " + index + ", no corresponding vertex");
				if ((uvs != null) && ((index + 1) * 2 > uvs.length)) 
					throw new IllegalArgumentException("index: " + index + ", no corresponding UV");
			}
		}
		
		this.coordinates = new float[coordinates.length];
		this.uvs = (uvs == null) ? null : new float[uvs.length];
		this.indices = (indices == null) ? null : new int[indices.length];
		
		System.arraycopy(coordinates, 0, this.coordinates, 0, coordinates.length);
		if (uvs != null)
			System.arraycopy(uvs, 0, this.uvs, 0, uvs.length);
		if (indices != null)
			System.arraycopy(indices, 0, this.indices, 0, indices.length);
	}
	
//	private MeshData(ObjectInputStream in) throws IOException {
//		this.coordinates = BonesIO.readFloatArray(in);
//		this.uvs = BonesIO.readFloatArray(in);
//		this.indices = BonesIO.readIntArray(in);
//	} 
	
	
	boolean isEmpty() {
		return (coordinates.length == 0);
	}
	
	void applyTransform(Matrix transform) {
		SimpleVector v = new SimpleVector();
		
		for (int i = 0; i < coordinates.length; i += 3) {
			v.set(coordinates[i], coordinates[i+1], coordinates[i+2]);
			v.matMul(transform);
			
			coordinates[i] = v.x;
			coordinates[i+1] = v.y;
			coordinates[i+2] = v.z;
		}
	}
	
//	static MeshData readFromStream(java.io.ObjectInputStream in) throws IOException {
//		if (in.readInt() == BonesIO.NULL)
//			return null;
//		return new MeshData(in);
//	}
//	
//	static void writeToStream(MeshData object, java.io.ObjectOutputStream out) throws IOException {
//		if (object == null) {
//			out.writeInt(BonesIO.NULL);
//		} else {
//			out.writeInt(BonesIO.NON_NULL);
//			
//			BonesIO.writeFloatArray(out, object.coordinates);
//			BonesIO.writeFloatArray(out, object.uvs);
//			BonesIO.writeIntArray(out, object.indices);
//		}
//	}
	
	

}