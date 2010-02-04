/**
 * Some of the classes in this file are adapted or inspired from Ardor3D. 
 * Such classes are indicated in class javadocs. 
 * 
 * Modification and redistribution of them may be subject to Ardor3D's license: 
 * http://www.ardor3d.com/LICENSE
 */
package raft.jpct.bones;

import java.nio.IntBuffer;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.jmex.model.ogrexml.anim.OgreMesh;
import com.threed.jpct.Logger;
import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.World;


/**
 * <p>A collection of {@link Joint}'s which constitute a hierarchy. A Skeleton can be shared among
 * {@link Skeleton.Pose}s.</p>
 * 
 * <p>This class is originally adapted from <a href="http://www.ardor3d.com">Ardor3D.</a></p>
 */
public class Skeleton implements java.io.Serializable, Iterable<Skeleton.Joint> {
	private static final long serialVersionUID = 1L;
	
    /** Maximum number of joints per vertex. */
    public static final int MAX_JOINTS_PER_VERTEX = SkinnedMesh.MAX_JOINTS_PER_VERTEX;
	
    private final Joint[] joints;
    
    private final Matrix transform = new Matrix();
	
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
	
	public Joint getJoint(int index) {
		return joints[index];
	}	
	
	public Matrix getTransform() {
		return transform;
	}
	
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
			System.out.println(MessageFormat.format("{0} name: {1} parent: {2}", 
					joint.index, joint.name, ((joint.hasParent() ? String.valueOf(joint.parentIndex) : ""))));
		}
		System.out.println("-- --");
	}

	
	/** 
	 * <p>A Joint in a {@link Skeleton}. A Joint essentially consists of an <i>Invert Bind Pose</i> matrix and a parent id.</p>
	 * 
	 * <p>This class is adapted from <a href="http://www.ardor3d.com">Ardor3D.</a></p>
	 * */
	public static class Joint implements java.io.Serializable {
		private static final long serialVersionUID = 1L;
		
	    /** Root node ID */
	    public static final short NO_PARENT = Short.MIN_VALUE;
		
		private Matrix inverseBindPose;
		private final short index;
		private final short parentIndex;
		private final String name;
		
		Joint(com.ardor3d.extension.animation.skeletal.Joint joint) {
			this(SkinHelper.getMatrix(joint.getInverseBindPose()), joint.getIndex(), 
					joint.getParentIndex(), joint.getName());
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
		
		public short getIndex() {
			return index;
		}
		
		public short getParentIndex() {
			return parentIndex;
		}

		public String getName() {
			return name;
		}
		
		public boolean hasParent() {
			return (parentIndex != NO_PARENT);
		}
	}
	
	/** 
	 * <p>A pose of {@link Skeleton} {@link Joint Joint}'s. Poses are used to deform (animate) {@link Skinned3D}'s.
	 * Many Poses can share the same <code>Skeleton</code>.</p>
	 * 
	 * <p>This class is adapted from <a href="http://www.ardor3d.com">Ardor3D.</a></p>
	 * */
	public static class Pose implements java.io.Serializable {
		private static final long serialVersionUID = 1L;
		
		final Skeleton skeleton; 
		/** transforms in object space */
		final Matrix[] globals;
		/** transforms in joint space */
		final Matrix[] locals;
		/** the transform which can directly be applied to a mesh vertex */
		final Matrix[] palette;
		
		
		/** Creates a new Pose for given {@link Skeleton}. */
		public Pose(Skeleton skeleton) {
			this.skeleton = skeleton;
			
	        final int jointCount = skeleton.joints.length;

	        this.locals = createNMatrices(jointCount);
	        this.globals = createNMatrices(jointCount);
	        this.palette = createNMatrices(jointCount);

	        // start off in bind pose.
	        setToBindPose();
		}
		
		/** Returns the {@link Skeleton} this pose is related to. */
		public Skeleton getSkeleton() {
			return skeleton;
		}

		/** Returns the joint transform in joint space.
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
	            locals[i] = skeleton.joints[i].inverseBindPose.invert();

	            // At this point we are in model space, so we need to remove our parent's transform (if we have one.)
	            if (skeleton.joints[i].hasParent()) {
		            final short parentIndex = skeleton.joints[i].getParentIndex();
	                // We remove the parent's transform simply by multiplying by its inverse bind pose. Done! :)
	                locals[i].matMul(skeleton.joints[parentIndex].inverseBindPose);
	            }
	        }
		}
		
	    /**
	     * Updates the global and palette transforms based on current local transforms.
	     * This method should be called before calling {@link Skinned3D#applyPose()} 
	     * if pose is modified. 
	     */
	    public void updateTransforms() {
	    	
	        // we go in update array order, which ensures parent global transforms are updated before child.
	        for (int index = 0; index < skeleton.joints.length; index++) {

	            // find our parent
	            if (skeleton.joints[index].hasParent()) {
		            final short parentIndex = skeleton.joints[index].getParentIndex();
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

	            if (!skeleton.transform.isIdentity()) {
	            	palette[index].matMul(skeleton.transform);
	            }
	        }
	    }
	    
		private static Matrix[] createNMatrices(int length) {
			Matrix[] result = new Matrix[length];
	        for (int i = 0; i < length; i++) {
	        	result[i] = new Matrix();
	        }
	        return result;
		}

	}

	/** 
	 * <p>Skin contains information how a {@link Skinned3D} is deformed with respect to {@link Pose}.</p>
	 * 
	 * <p>This class is adapted from <a href="http://www.ardor3d.com">Ardor3D.</a></p>
	 *  */
	static class Skin implements java.io.Serializable {
		private static final long serialVersionUID = 1L;
		
		// TODO separate max joints for ardor and jME to be safe in the future
		
		final float[][] weights;
		final short[][] jointIndices;
		
		Skin(SkinnedMesh skinnedMesh) {
			this(SkinHelper.asArray(skinnedMesh.getWeights(), MAX_JOINTS_PER_VERTEX), 
					SkinHelper.asArray(skinnedMesh.getJointIndices(), MAX_JOINTS_PER_VERTEX));
		}

		Skin(OgreMesh ogreMesh) {
			this(SkinHelper.asArray(ogreMesh.getWeightBuffer().getWeights(), MAX_JOINTS_PER_VERTEX),
					SkinHelper.asShortArray(ogreMesh.getWeightBuffer().getIndexes(), MAX_JOINTS_PER_VERTEX));
		}
		
		
		Skin(float[][] weights, short[][] jointIndices) {
			this.weights = weights;
			this.jointIndices = jointIndices;
		}

		
		void checkAlmostEqual(Skin other) {
			if (weights.length != other.weights.length)
				throw new IllegalArgumentException("Number of vertices differ!");
		}
		
	}
	
	/** 
	 * <p>Helper class to visually represent a {@link Skeleton}.</p> 
	 * 
	 * <p>This class is adapted from <a href="http://www.ardor3d.com">Ardor3D.</a></p>
	 */
	public static class Debugger {
		private static final long serialVersionUID = 1L;
		
		public static float minBoneLength = 0.01f;
		
		private final Skeleton skeleton;
	    private final Object3D[] jointObjects; 
	    private final Object3D[] boneObjects; 
		private final float boneScale;
		
	    private Matrix tempMatrix = new Matrix();

		public Debugger(Skeleton.Pose pose, short... ignoreJoints) {
			this(pose, 3f);
		}
		
		public Debugger(Skeleton.Pose pose, float boneScale, short... ignoreJoints) {
			this.skeleton = pose.skeleton;
			this.boneScale = boneScale;
			
			this.jointObjects = new Object3D[skeleton.joints.length];
			this.boneObjects = new Object3D[skeleton.joints.length];

		    BitSet ignoredJoints = new BitSet();
			for (short joint : ignoreJoints) {
				ignoredJoints.set(joint);
			}
			
	        for (int i = 0, max = skeleton.joints.length; i < max; i++) {
	        	if (ignoredJoints.get(i))
	        		continue;
	        		
	            jointObjects[i] = createJoint(pose.globals[i]);
	            
	            if (skeleton.joints[i].hasParent()) {
		            final short parentIndex = skeleton.joints[i].getParentIndex();
		            if (!ignoredJoints.get(parentIndex))
		            	boneObjects[i] = createBone(pose.globals[parentIndex], pose.globals[i]);
	            }
	        }
		}

		public List<Object3D> getBoneObjects() {
			return Arrays.asList(boneObjects);
		}
		
		public List<Object3D> getJointObjects() {
			return Arrays.asList(jointObjects);
		}
		
		public void update(Skeleton.Pose pose) {
			if (pose.skeleton != this.skeleton)
				throw new IllegalArgumentException("pose does not belong to this debugger's skeleton");
			
	        final Matrix[] globals = pose.globals;

	        for (int i = 0, max = skeleton.joints.length; i < max; i++) {
	            Object3D joint = jointObjects[i];
	            if (joint == null)
	            	continue;
	            
	            updateJoint(joint, globals[i]);
	            
	            final short parentIndex = skeleton.joints[i].getParentIndex();
	            if (parentIndex != Joint.NO_PARENT) {
	                Object3D bone = boneObjects[i];
		            if (bone == null)
		            	continue;
	                updateBone(bone, globals[parentIndex], globals[i]);
	            }
	        }
	    }
	    
		
	    private Object3D createJoint(Matrix jntTransform) {

	    	Object3D joint = Primitives.getBox(0.3f, 1f);
	    	
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
	    	
	    	if (!skeleton.transform.isIdentity()) {
	    		tempMatrix.matMul(skeleton.transform);
	    	}
	    	
	    	joint.getRotationMatrix().setTo(tempMatrix);
	    	SkinHelper.clearTranslation(joint.getRotationMatrix());
	    	joint.translate(tempMatrix.getTranslation().calcSub(joint.getTranslation()));
	    }
	    
	    private void updateBone(Object3D bone, final Matrix start, final Matrix end) {
	    	SimpleVector from = start.getTranslation();
	    	SimpleVector to = end.getTranslation();

	    	if (!skeleton.transform.isIdentity()) {
	    		from.matMul(skeleton.transform);
	    		to.matMul(skeleton.transform);
	    	}
	    	
			SimpleVector direction = to.calcSub(from);
			
			bone.setScale(1f);
			bone.getRotationMatrix().setTo(direction.getRotationMatrix());
			bone.translate(from.calcSub(bone.getTranslation()));
			bone.setScale(Math.max(minBoneLength, direction.length()));
	    }

		public void addToWorld(World world) {
			for (Object3D joint : jointObjects) {
				if (joint != null) world.addObject(joint);
			}
			for (Object3D bone : boneObjects) {
				if (bone != null)
					world.addObject(bone);
			}
		}
		
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
	
	/** place holder for mesh coordinates */
	static class Mesh implements java.io.Serializable {
		private static final long serialVersionUID = 1L;
		
		final float[] coordinates;
		final float[] uvs;
		final int[] indices;
		
		Mesh(SkinnedMesh mesh) {
			this.coordinates = SkinHelper.asArray(mesh.getMeshData().getVertexBuffer()); 
			this.uvs = SkinHelper.asArray(mesh.getMeshData().getTextureBuffer(0));
			
			IntBuffer indexBuffer = mesh.getMeshData().getIndexBuffer();
			this.indices = (indexBuffer == null) ? null : SkinHelper.asArray(indexBuffer);
		}
		
		Mesh(OgreMesh mesh) {
			this.coordinates = SkinHelper.asArray(mesh.getVertexBuffer());
			if (mesh.getTextureCoords().isEmpty()) {
				Logger.log("Mesh has no texture coodinates", Logger.WARNING);
				this.uvs = null;
			} else {
				this.uvs = SkinHelper.asArray(mesh.getTextureCoords().get(0).coords);
			}
			
			IntBuffer indexBuffer = mesh.getIndexBuffer();
			this.indices = (indexBuffer == null) ? null : SkinHelper.asArray(indexBuffer);
		}
		
		boolean isEmpty() {
			return (coordinates.length == 0);
		}
	}

}
