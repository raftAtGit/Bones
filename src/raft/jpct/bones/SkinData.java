package raft.jpct.bones;

import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.jmex.model.ogrexml.anim.OgreMesh;

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
	
	SkinData(SkinnedMesh skinnedMesh) {
		this(SkinHelper.asArray(skinnedMesh.getWeights(), Skeleton.MAX_JOINTS_PER_VERTEX), 
				SkinHelper.asArray(skinnedMesh.getJointIndices(), Skeleton.MAX_JOINTS_PER_VERTEX));
	}

	SkinData(OgreMesh ogreMesh) {
		this(SkinHelper.asArray(ogreMesh.getWeightBuffer().getWeights(), Skeleton.MAX_JOINTS_PER_VERTEX),
				SkinHelper.asShortArray(ogreMesh.getWeightBuffer().getIndexes(), Skeleton.MAX_JOINTS_PER_VERTEX));
	}
	
	
	SkinData(float[][] weights, short[][] jointIndices) {
		this.weights = weights;
		this.jointIndices = jointIndices;
	}

	
	void checkAlmostEqual(SkinData other) {
		if (weights.length != other.weights.length)
			throw new IllegalArgumentException("Number of vertices differ!");
	}
	
}