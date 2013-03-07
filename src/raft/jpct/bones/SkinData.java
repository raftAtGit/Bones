package raft.jpct.bones;



/** 
 * <p>Skin contains information how a {@link Animated3D} is deformed with respect to {@link SkeletonPose}.</p>
 * 
 * <p>This class is adapted from <a href="http://www.ardor3d.com">Ardor3D.</a></p>
 *  */
public class SkinData implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	
	// TODO separate max joints for ardor and jME to be safe in the future
	
	final float[][] weights;
	final short[][] jointIndices;
	
	/** <p>Creates a new SkinData out of given information. The arrays are copied.</p>
	 * 
	 * @param weights how much each vertex in mesh is effected by corresponding skeleton joints 
	 * @param jointIndices which joints effect which vertices   
	 * */
	public SkinData(float[][] weights, short[][] jointIndices) {
		this.weights = copyWeights(weights);
		this.jointIndices = copyIndices(jointIndices);
		
//		this.weights = new float[weights.length][];
//		this.jointIndices = new short[jointIndices.length][];
//		
//		for (int i = 0; i < weights.length; i++) {
//			this.weights[i] = new float[weights[i].length];
//			System.arraycopy(weights[i], 0, this.weights[i], 0, weights[i].length);
//		}
//		for (int i = 0; i < jointIndices.length; i++) {
//			this.jointIndices[i] = new short[jointIndices[i].length];
//			System.arraycopy(jointIndices[i], 0, this.jointIndices[i], 0, jointIndices[i].length);
//		}
	}
	
//	private SkinData(ObjectInputStream in) throws IOException {
//		this.weights = BonesIO.readFloat2Array(in);
//		this.jointIndices = BonesIO.readShort2Array(in);
//	} 
	
	/** Returns a deep copy of this SkinData. */
	@Override
	public SkinData clone() {
		return new SkinData(this.weights, this.jointIndices); 
	}
	
	/** Returns a copy of weights array. */
	public float[][] getWeights() {
		return copyWeights(weights);
	}

	/** Returns a copy of joint indices array. */
	public short[][] getJointIndices() {
		return copyIndices(jointIndices);
	}

	void checkAlmostEqual(SkinData other) {
		if (weights.length != other.weights.length)
			throw new IllegalArgumentException("Number of vertices differ!");
	}

//	static SkinData readFromStream(java.io.ObjectInputStream in) throws IOException {
//		if (in.readInt() == BonesIO.NULL)
//			return null;
//		return new SkinData(in);
//	}
//	
//	static void writeToStream(SkinData object, java.io.ObjectOutputStream out) throws IOException {
//		if (object == null) {
//			out.writeInt(BonesIO.NULL);
//		} else {
//			out.writeInt(BonesIO.NON_NULL);
//			
//			BonesIO.writeFloat2Array(out, object.weights);
//			BonesIO.writeShort2Array(out, object.jointIndices);
//		}
//	}
	
	private static float[][] copyWeights(float[][] weights) {
		float[][] copy = new float[weights.length][];
		
		for (int i = 0; i < weights.length; i++) {
			copy[i] = new float[weights[i].length];
			System.arraycopy(weights[i], 0, copy[i], 0, weights[i].length);
		}
		
		return copy;
	} 

	private static short[][] copyIndices(short[][] indices) {
		short[][] copy = new short[indices.length][];
		
		for (int i = 0; i < indices.length; i++) {
			copy[i] = new short[indices[i].length];
			System.arraycopy(indices[i], 0, copy[i], 0, indices[i].length);
		}
		
		return copy;
	} 
	
	
}