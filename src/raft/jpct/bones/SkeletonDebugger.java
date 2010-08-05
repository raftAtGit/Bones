package raft.jpct.bones;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.World;

/** 
 * <p>Helper class to visually represent a {@link SkeletonPose}.</p> 
 * 
 * <p>This class is adapted from <a href="http://www.ardor3d.com">Ardor3D.</a></p>
 */
public class SkeletonDebugger {
	private static final long serialVersionUID = 1L;
	
	/** Minimum length of a bone. Sometimes joints may overlap,
	 * resulting bones will not have a length of zero but this value. */
	public static float minBoneLength = 0.01f;
	
	/** Default value of bone scale. */
	public static final float DEFAULT_BONE_SCALE = 10f;
	/** Default value of joint scale. */
	public static final float DEFAULT_JOINT_SCALE = 0.3f;
	
	private final Skeleton skeleton;
    private final Object3D[] jointObjects; 
    private final Object3D[] boneObjects; 
	private final float boneScale;
	private final float jointScale;
	
    private Matrix tempMatrix = new Matrix();

    /** 
     * Creates a new Debugger with {@link #DEFAULT_BONE_SCALE} and {@link #DEFAULT_JOINT_SCALE}
     *  
     * @param pose the pose this debugger represents
     * @param ignoreJoints these joints and the bones associated with them will not be displayed. 
     * */
	public SkeletonDebugger(SkeletonPose pose, int... ignoreJoints) {
		this(pose, DEFAULT_BONE_SCALE, DEFAULT_JOINT_SCALE);
	}
	
    /** 
     * Creates a new Debugger with given scale.
     * 
     * @param pose the pose this debugger represents
     * @param boneScale scale of bone  
     * @param ignoreJoints these joints and the bones associated with them will not be displayed.
     * 
     *  @see Primitives#getPyramide(float, float)
     * */
	public SkeletonDebugger(SkeletonPose pose, float boneScale, float jointScale, int... ignoreJoints) {
		this.skeleton = pose.skeleton;
		this.boneScale = boneScale;
		this.jointScale = jointScale;
		
		this.jointObjects = new Object3D[skeleton.joints.length];
		this.boneObjects = new Object3D[skeleton.joints.length];

	    BitSet ignoredJoints = new BitSet();
		for (int joint : ignoreJoints) {
			ignoredJoints.set(joint);
		}
		
        for (int i = 0, max = skeleton.joints.length; i < max; i++) {
        	if (ignoredJoints.get(i))
        		continue;
        		
            jointObjects[i] = createJoint(pose.globals[i]);
            
            if (skeleton.joints[i].hasParent()) {
	            final int parentIndex = skeleton.joints[i].getParentIndex();
	            if (!ignoredJoints.get(parentIndex))
	            	boneObjects[i] = createBone(pose.globals[parentIndex], pose.globals[i]);
            }
        }
	}

	/** Returns the {@link Object3D}s which represent bones. Some of them may be null. */
	public List<Object3D> getBoneObjects() {
		return Arrays.asList(boneObjects);
	}
	
	/** Returns the {@link Object3D}s which represent joints. Bones are virtual links
	 * between a joint and its parent (if any) 
	 * Some of them may be null. */
	public List<Object3D> getJointObjects() {
		return Arrays.asList(jointObjects);
	}
	
	/** Updates bone and joint objects according to given pose. */
	public void update(SkeletonPose pose) {
		if (pose.skeleton != this.skeleton)
			throw new IllegalArgumentException("pose does not belong to this debugger's skeleton");
		
        final Matrix[] globals = pose.globals;

        for (int i = 0, max = skeleton.joints.length; i < max; i++) {
            Object3D joint = jointObjects[i];
            if (joint == null)
            	continue;
            
            updateJoint(joint, globals[i]);
            
            final int parentIndex = skeleton.joints[i].getParentIndex();
            if (parentIndex != Joint.NO_PARENT) {
                Object3D bone = boneObjects[i];
	            if (bone == null)
	            	continue;
                updateBone(bone, globals[parentIndex], globals[i]);
            }
        }
    }
    
	
    private Object3D createJoint(Matrix jntTransform) {

    	Object3D joint = Primitives.getBox(jointScale, 1f);
    	
    	joint.getRotationMatrix().setTo(jntTransform);
    	SkinHelper.clearTranslation(joint.getRotationMatrix());
    	joint.translate(jntTransform.getTranslation());
    	
    	joint.build();
    	return joint;
    }
    
    private Object3D createBone(Matrix start, Matrix end) {
    	Object3D bone = createPyramide(start.getTranslation(), end.getTranslation(), boneScale);
    	return bone;
    }
	
    private void updateJoint(Object3D joint, Matrix jntTransform) {
    	tempMatrix.setTo(jntTransform);
    	
    	joint.getRotationMatrix().setTo(tempMatrix);
    	SkinHelper.clearTranslation(joint.getRotationMatrix());
    	joint.translate(tempMatrix.getTranslation().calcSub(joint.getTranslation()));
    }
    
    private void updateBone(Object3D bone, final Matrix start, final Matrix end) {
    	SimpleVector from = start.getTranslation();
    	SimpleVector to = end.getTranslation();

		SimpleVector direction = to.calcSub(from);
		
		bone.setScale(1f);
		bone.getRotationMatrix().setTo(direction.getRotationMatrix());
		bone.translate(from.calcSub(bone.getTranslation()));
		bone.setScale(Math.max(minBoneLength, direction.length()));
    }

    /** Adds all joint and bone objects to given world. */
	public void addToWorld(World world) {
		for (Object3D joint : jointObjects) {
			if (joint != null) world.addObject(joint);
		}
		for (Object3D bone : boneObjects) {
			if (bone != null)
				world.addObject(bone);
		}
	}
	
    /** Sets visibility of joint and bone objects. */
	public void setVisibility(boolean visible) {
		for (Object3D joint : jointObjects) {
			if (joint != null) joint.setVisibility(visible);
		}
		for (Object3D bone : boneObjects) {
			if (bone!= null) bone.setVisibility(visible);
		}
	}
	
	/** creates a pyramide object where its bottom center is placed at <i>from</i> vector and top corner
	 * is at <i>to</i> vector. */
	private static Object3D createPyramide(SimpleVector from, SimpleVector to, float scale) {
		SimpleVector direction = to.calcSub(from); 
		float height = 1f;
		Object3D p = Primitives.getPyramide(height/scale/2, scale);

		// move pyramide such that it's bottom center is at origin
		p.translate(0, -height/2, 0);
		p.translateMesh();
		p.getTranslationMatrix().setIdentity();
		
		// make pyramid looking down to positive Z axis
		p.rotateX((float)Math.PI/2);
		p.rotateMesh();
		
		// set direction
		p.getRotationMatrix().setTo(direction.getRotationMatrix());
		
		p.translate(from);
		
		p.build();
		// build modifies rotation pivot, so set it again
		p.setRotationPivot(SimpleVector.ORIGIN);
		p.setScale(Math.max(minBoneLength, direction.length()));
		
		return p;
	}
	
}
