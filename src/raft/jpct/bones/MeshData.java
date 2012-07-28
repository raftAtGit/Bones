package raft.jpct.bones;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.threed.jpct.Logger;
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
	
	private static final boolean COMPACT_ARRAYS = false;
	
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

			if (COMPACT_ARRAYS) {
			
				Set<Integer> set = new TreeSet<Integer>();
				for (int index : indices) {
					set.add(index);
				}
	//			System.out.println(set.size() + ": "  + set);
				
				if (coordinates.length > set.size() * 3) {
					// TODO maybe use a ratio to decide sparseness
	
					Logger.log("indexed geometry is sparse. remapping indices to compact arrays", Logger.WARNING);
					
					Map<Integer, Integer> map = new TreeMap<Integer, Integer>();
					
					int position = 0;
					for (Integer index : set) {
						map.put(index, position);
						position++;
					}
	
					float[] newCoordinates = new float[set.size() * 3];
					float[] newUvs = (uvs == null) ? null : new float[set.size() * 2];
					
					for (int i = 0; i < indices.length; i++) {
						int oldIndex = indices[i];
						int newIndex = map.get(oldIndex);
						
						indices[i] = newIndex;
						
						newCoordinates[newIndex*3] = coordinates[oldIndex*3];
						newCoordinates[newIndex*3 + 1] = coordinates[oldIndex*3 + 1];
						newCoordinates[newIndex*3 + 2] = coordinates[oldIndex*3 + 2];
						
						if (newUvs != null) {
							newUvs[newIndex*2] = uvs[oldIndex*2];
							newUvs[newIndex*2 + 1] = uvs[oldIndex*2 + 1];
						}
					}
					
					Logger.log("remapped indices, size reduced from " + coordinates.length/3 + " to " + newCoordinates.length/3, Logger.MESSAGE);
	
					coordinates = newCoordinates;
					uvs = newUvs;
					
				} // end remapping
				
			} // if COMPACT_ARRAYS
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