package raft.jpct.bones;


import com.threed.jpct.Matrix;

/** 
 * <p>A pose of {@link Skeleton} {@link Joint Joint}'s. Poses are used to deform (animate) {@link Animated3D}'s.
 * Many Poses can share the same <code>Skeleton</code>.</p>
 * 
 * <p>This class is adapted from <a href="http://www.ardor3d.com">Ardor3D.</a></p>
 * */
public class SkeletonPose implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	
	final Skeleton skeleton; 
	/** transforms in object space */
	final Matrix[] globals;
	/** transforms in joint space */
	final Matrix[] locals;
	/** the transform which can directly be applied to a mesh vertex */
	final Matrix[] palette;
	
	
	/** Creates a new Pose for given {@link Skeleton}. */
	public SkeletonPose(Skeleton skeleton) {
		this.skeleton = skeleton;
		
        final int jointCount = skeleton.joints.length;

        this.locals = createNMatrices(jointCount);
        this.globals = createNMatrices(jointCount);
        this.palette = createNMatrices(jointCount);

        // start off in bind pose.
        setToBindPose();
	}
	
//	private SkeletonPose(ObjectInputStream in) throws IOException, ClassNotFoundException {
////		this(Skeleton.readFromStream(in)); 
//		this((Skeleton)in.readObject()); 
//	}

	/** Returns the {@link Skeleton} this pose is related to. */
	public Skeleton getSkeleton() {
		return skeleton;
	}

	/** Returns the joint transform in local space (relative to its parent).
	 * 
	 * @param index the joint index */
	public Matrix getLocal(int index) {
		return locals[index];
	}

	/** Returns the joint transform in object space.
	 *  
	 * @param index the joint index */
	public Matrix getGlobal(int index) {
		return globals[index];
	}

	/** Returns the joint transform in bind-pose space. The returned matrix can be applied
	 * to a vertex of mesh in bind-pose to skin it according to this pose.
	 *   
	 * @param index the joint index */
	public Matrix getPalette(int index) {
		return palette[index];
	}

	/** Updates transforms to reset to bind-pose. */
	public void setToBindPose() {
        // go through our local transforms
        for (int i = 0; i < locals.length; i++) {
            // inverse of inverseBindPose = bindPose :)
            locals[i].setTo(skeleton.joints[i].bindPose);

            // At this point we are in model space, so we need to remove our parent's transform (if we have one.)
            if (skeleton.joints[i].hasParent()) {
	            final int parentIndex = skeleton.joints[i].getParentIndex();
                // We remove the parent's transform simply by multiplying by its inverse bind pose. Done! :)
                locals[i].matMul(skeleton.joints[parentIndex].inverseBindPose);
            }
        }
	}
	
    /**
     * Updates the global and palette transforms based on current local transforms.
     * This method should be called before calling {@link Animated3D#applySkeletonPose()} 
     * if pose is modified. 
     */
    public void updateTransforms() {
    	
        // we go in update array order, which ensures parent global transforms are updated before child.
        for (int index = 0; index < skeleton.joints.length; index++) {

            // find our parent
            if (skeleton.joints[index].hasParent()) {
	            final int parentIndex = skeleton.joints[index].getParentIndex();
                // we have a parent, so take us from local->parent->model space by multiplying 
	            // by parent's local->model space transform.
	            globals[index].setTo(locals[index]);
	            globals[index].matMul(globals[parentIndex]);
	            
            } else {
                // no parent so just set global to the local transform
                globals[index].setTo(locals[index]);
            }

            // at this point we have a local->model space transform for this joint, for skinning we multiply this by the
            // joint's inverse bind pose (joint->model space, inverted). This gives us a transform that can take a
            // vertex from bind pose (model space) to current pose (model space).
            palette[index].setTo(skeleton.joints[index].inverseBindPose);
            palette[index].matMul(globals[index]);
        }
    }
    
    /** Returns a new {@link SkeletonPose} sharing same skeleton with this.
     * Created SkeletonPose is initially in bind pose. */
    @Override
    public SkeletonPose clone() {
    	return new SkeletonPose(skeleton);
    }
    
//	static SkeletonPose readFromStream(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
//		if (in.readInt() == BonesIO.NULL)
//			return null;
//		return new SkeletonPose(in);
//	}
//	
//	static void writeToStream(SkeletonPose object, java.io.ObjectOutputStream out) throws IOException {
//		if (object == null) {
//			out.writeInt(BonesIO.NULL);
//		} else {
//			out.writeInt(BonesIO.NON_NULL);
//			// write skeleton as object since it's shared among other objects
//			out.writeObject(object.skeleton);
////			Skeleton.writeToStream(object.skeleton, out);
//		}
//	}
    
	private static Matrix[] createNMatrices(int length) {
		Matrix[] result = new Matrix[length];
        for (int i = 0; i < length; i++) {
        	result[i] = new Matrix();
        }
        return result;
	}

}