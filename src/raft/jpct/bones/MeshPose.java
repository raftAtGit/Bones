package raft.jpct.bones;

import com.threed.jpct.SimpleVector;

/**
 * <p>A a pose of mesh vertices. Vertex positions in pose are defined as offsets to
 * their original positions.</p>
 * 
 * <p>This class is originally adapted from <a href="http://www.jmonkeyengine.com" target="_blank">jME.</a></p>
 */
public class MeshPose implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

    private final String name;

    private final SimpleVector[] offsets;
    private final int[] indices;
    
    private static final SimpleVector tmpVector = new SimpleVector();
    

    /** Creates a new MeshPose out of given  data. */
	public MeshPose(String name, SimpleVector[] offsets, int[] indices) {
		this(name, indices.length);
		
		if (offsets.length != indices.length)
            throw new IllegalArgumentException("Offsets and indices must be same length!");

        for (int i = 0; i < indices.length; i++) {
        	this.indices[i] = indices[i];
        	this.offsets[i] = new SimpleVector(offsets[i]);
        }
	}
	
	MeshPose(com.jmex.model.ogrexml.anim.Pose pose) {
		this(pose.getName(), pose.getIndices().length);

		for (int i = 0; i < indices.length; i++) {
        	this.indices[i] = pose.getIndices()[i];
        	this.offsets[i] = SkinHelper.getVector(pose.getOffsets()[i]);
		}
	}

	private MeshPose(String name, int length) {
		this.name = name;
		this.offsets = new SimpleVector[length];
		this.indices = new int[length];
	}

	/** returns name of this pose. */
	public String getName() {
		return name;
	}

	/** Applies this pose to given object. */
	void apply(float poseWeight, Animated3D target) { 
		SimpleVector[] vertices = target.getDestinationMesh(); 
		
        for (int i = 0; i < indices.length; i++){
        	SimpleVector offset = offsets[i];
            int vertIndex = indices[i];

            tmpVector.set(offset);
            tmpVector.scalarMul(poseWeight);

            vertices[vertIndex].add(tmpVector);
        }
	}
	
	
}