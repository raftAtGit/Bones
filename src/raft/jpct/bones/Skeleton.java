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
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
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
    public static final int MAX_JOINTS_PER_VERTEX = SkinnedMesh.MAX_JOINTS_PER_VERTEX;
	
    final Joint[] joints;
    
    final Matrix transform = new Matrix();
	
	Skeleton(com.ardor3d.extension.animation.skeletal.Skeleton skeleton) {
		this.joints = new Joint[skeleton.getJoints().length];
		
		for (int i = 0; i < joints.length; i++) {
			this.joints[i] = new Joint(skeleton.getJoints()[i]);
		}
		Logger.log("Skeleton created out of Ardor3D skeleton", Logger.MESSAGE);
		if (Logger.getLogLevel() == Logger.LL_VERBOSE) 
			printJoints();
	}

	Skeleton(com.jmex.model.ogrexml.anim.Skeleton skeleton) {
		this.joints = new Joint[skeleton.getBoneCount()];
		
		// it's not guaranteed bones are sorted for hierarchy 
		// so we first create a map of joint indices
		Map<com.jmex.model.ogrexml.anim.Bone, Short> map =
			new IdentityHashMap<com.jmex.model.ogrexml.anim.Bone, Short>();

		for (int i = 0; i < joints.length; i++) {
			map.put(skeleton.getBone(i), (short)i);
		}
		
		// we cannot change original ordering of joints since channels use target joint index
		for (int i = 0; i < joints.length; i++) {
			this.joints[i] = new Joint(map, skeleton.getBone(i), (short) i);
		}
		
		Logger.log(MessageFormat.format("Skeleton created out of jME OGRE skeleton, {0} joints", joints.length), Logger.MESSAGE);
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
