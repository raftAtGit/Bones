package raft.jpct.bones;

import java.util.Map;

import com.threed.jpct.Matrix;

/** 
 * <p>A Joint in a {@link Skeleton}. A Joint essentially consists of an <i>Invert Bind Pose</i> matrix and a parent id.</p>
 * 
 * <p>This class is adapted from <a href="http://www.ardor3d.com">Ardor3D.</a></p>
 * */
public class Joint implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	
    /** Parent index of a joint which has no parent */
    public static final int NO_PARENT = -1;
	
	Matrix inverseBindPose;
	final int index;
	final int parentIndex;
	final String name;
	
	Joint(com.ardor3d.extension.animation.skeletal.Joint joint) {
		this(SkinHelper.getMatrix(joint.getInverseBindPose()), joint.getIndex(), 
				(joint.getParentIndex() == com.ardor3d.extension.animation.skeletal.Joint.NO_PARENT) ? NO_PARENT : joint.getParentIndex(), 
						joint.getName());
	}
	
	Joint(Map<com.jmex.model.ogrexml.anim.Bone, Short> map, 
			com.jmex.model.ogrexml.anim.Bone bone, short index) {
		
		com.jmex.model.ogrexml.anim.Bone root = bone;
		while (root.getParent() != null) {
			root = root.getParent();
		}
		
		this.index = index;
		this.name = bone.getName();
		
		com.jme.math.Vector3f tx = bone.getWorldBindInversePos();
		com.jme.math.Quaternion rot = bone.getWorldBindInverseRot();
		// due to jME's OGRE loading, root rotation is baked into all bind poses, remove it  
		tx = root.getWorldBindInverseRot().mult(tx);

		this.inverseBindPose = new Quaternion(rot).getRotationMatrix();
		inverseBindPose.translate(tx.x, tx.y, tx.z);
		
		this.parentIndex = (bone.getParent() == null) ? NO_PARENT 
				: map.get(bone.getParent());
	}

	Joint(Matrix inverseBindPose, short index, short parentIndex, String name) {
		this.inverseBindPose = inverseBindPose;
		this.index = index;
		this.parentIndex = parentIndex;
		this.name = name;
	}
	
	/** Returns a copy of invertBindPose matrix */
	public Matrix getInverseBindPose() {
		return inverseBindPose.cloneMatrix();
	}

	/** Returns invert of invertBindPose matrix. 
	 * This method does not use cache, always calculates invert of invertBindPose matrix. */
	public Matrix getBindPose() {
		return inverseBindPose.invert();
	}
	
	/** Returns the index of this joint. */
	public int getIndex() {
		return index;
	}
	
	/** Returns the index of parent of this joint. Or {@link #NO_PARENT} if this joint has no parent. */
	public int getParentIndex() {
		return parentIndex;
	}

	/** Returns name of this joint. */
	public String getName() {
		return name;
	}
	
	/** Returns true if this joint has a parent. */
	public boolean hasParent() {
		return (parentIndex != NO_PARENT);
	}
}