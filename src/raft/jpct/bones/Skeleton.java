/**
 * Some of the classes in this file are adapted or inspired from Ardor3D. 
 * Such classes are indicated in class javadocs. 
 * 
 * Modification and redistribution of them may be subject to Ardor3D's license: 
 * http://www.ardor3d.com/LICENSE
 */
package raft.jpct.bones;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Iterator;

import com.threed.jpct.Logger;
import com.threed.jpct.Matrix;


/**
 * <p>A collection of {@link Joint}'s which constitute a hierarchy. A Skeleton can be shared among
 * {@link SkeletonPose}s.</p>
 * 
 * <p>This class is originally adapted from <a href="http://www.ardor3d.com">Ardor3D.</a></p>
 */
public class Skeleton implements java.io.Serializable, Iterable<Joint> {
	private static final long serialVersionUID = 1L;
	
    /** Maximum number of joints per vertex. */
    public static final int MAX_JOINTS_PER_VERTEX = 4;
	
    final Joint[] joints;
    
    final Matrix transform = new Matrix();
	
    /** <p>Creates a new Skeleton out of given joints. Joint's indices must match their position in array
     * and array should be ordered such that, parent comes first.</p> */
    public Skeleton(Joint[] joints) {
    	this.joints = new Joint[joints.length];
    	
    	for (int i = 0; i < joints.length; i++) {
    		Joint joint = joints[i];
    		this.joints[i] = joint;
    		
    		if (i != joint.index)
    			throw new IllegalArgumentException("joint index does not match its position in array. position: " 
    					+ i + ", index: " + joint.index);
    		
    		if (joint.hasParent()) {
        		int parent = joint.parentIndex;
        		if (parent < 0 || parent >= joints.length) 
        			throw new IllegalArgumentException("parent of joint " + i + " is out of range: " + parent);
    		}
    	}
		if (Logger.getLogLevel() == Logger.LL_VERBOSE) 
			printJoints();
    }
    
	/** Returns number of joints */
	public int getNumberOfJoints() {
		return joints.length;
	}
	
	/** Returns the {@link Joint} by index. */
	public Joint getJoint(int index) {
		return joints[index];
	}	
	
	/** Returns the transform which is finally applied to joints. 
	 * Can be used to rotate or scale the skeleton (and poses of it) */
	public Matrix getTransform() {
		return transform;
	}
	
    /** <p>Returns an iterator of joints</p> */
	public Iterator<Joint> iterator() {
		return Arrays.asList(joints).iterator();
	}
	
	
    /**
     * <p>Finds the joint by name. First found one is returned.</p>
     * 
     * @param jointName name of the joint to locate. Case sensitive.
     * @return the joint if found, or null if not.
     */
    public Joint findJointByName(final String jointName) {
        for (Joint joint : joints) {
            if (jointName.equals(joint.name)) 
                return joint;
        }
        return null;
    }
	
	void checkAlmostEqual(Skeleton other) {
		if (joints.length != other.joints.length)
			throw new IllegalArgumentException("Number of joints differ!");
		
		for (int i = 0; i < joints.length; i++) {
			if (joints[i].parentIndex != other.joints[i].parentIndex)
				throw new IllegalArgumentException("Joint parents differ");
		}
	}
	
	private void printJoints() {
		System.out.println("-- total " + joints.length + " joint(s) --");
		for (Joint joint : joints) {
			System.out.println(MessageFormat.format("{0} name: {1}, parent: {2}", 
					joint.index, joint.name, ((joint.hasParent() ? String.valueOf(joint.parentIndex) : ""))));
		}
		System.out.println("-- --");
	}

}
