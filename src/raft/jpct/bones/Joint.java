package raft.jpct.bones;

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
	
	final Matrix inverseBindPose;
	final int index;
	final int parentIndex;
	final String name;
	
	public Joint(Matrix inverseBindPose, int index, int parentIndex, String name) {
		if (parentIndex < 0 && parentIndex != NO_PARENT)
			throw new IllegalArgumentException("parent index: " + parentIndex);
		if (index == parentIndex)
			throw new IllegalArgumentException("joint cannot be parent of itself");
		if (index < parentIndex)
			throw new IllegalArgumentException("parent index should be less than joint index. " +
					"o/w a joint array cannot be ordered such that parent comes first");
		
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