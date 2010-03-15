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
		if ((coordinates.length % 3) != 0)
			throw new IllegalArgumentException("coordinates length should be a multiple of 3");
		
		this.coordinates = new float[coordinates.length];
		this.uvs = (uvs == null) ? null : new float[uvs.length];
		this.indices = (indices == null) ? null : new int[indices.length];
		
		System.arraycopy(coordinates, 0, this.coordinates, 0, coordinates.length);
		if (uvs != null)
			System.arraycopy(uvs, 0, this.uvs, 0, uvs.length);
		if (indices != null)
			System.arraycopy(indices, 0, this.indices, 0, indices.length);
	}
	
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

}