package raft.jpct.bones;


/** 
 * <p>Skin contains information how a {@link Animated3D} is deformed with respect to {@link SkeletonPose}.</p>
 * 
 * <p>This class is adapted from <a href="http://www.ardor3d.com">Ardor3D.</a></p>
 *  */
class SkinData implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	
	// TODO separate max joints for ardor and jME to be safe in the future
	
	final float[][] weights;
	final short[][] jointIndices;
	
	public SkinData(float[][] weights, short[][] jointIndices) {
		this.weights = new float[weights.length][];
		this.jointIndices = new short[jointIndices.length][];
		
		for (int i = 0; i < weights.length; i++) {
			this.weights[i] = new float[weights[i].length];
			System.arraycopy(weights[i], 0, this.weights[i], 0, weights[i].length);
		}
		for (int i = 0; i < jointIndices.length; i++) {
			this.jointIndices[i] = new short[jointIndices[i].length];
			System.arraycopy(jointIndices[i], 0, this.jointIndices[i], 0, jointIndices[i].length);
		}
	}
	
	void checkAlmostEqual(SkinData other) {
		if (weights.length != other.weights.length)
			throw new IllegalArgumentException("Number of vertices differ!");
	}
	
}