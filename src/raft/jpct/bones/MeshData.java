package raft.jpct.bones;

import java.nio.IntBuffer;

import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.jmex.model.ogrexml.anim.OgreMesh;
import com.threed.jpct.Logger;

/** place holder for mesh coordinates */
public class MeshData implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	
	final float[] coordinates;
	final float[] uvs;
	final int[] indices;
	
	public MeshData(float[] coordinates, float[] uvs, int[] indices) {
		this.coordinates = coordinates;
		this.uvs = uvs;
		this.indices = indices;
	}
	
	MeshData(SkinnedMesh mesh) {
		this.coordinates = SkinHelper.asArray(mesh.getMeshData().getVertexBuffer()); 
		this.uvs = SkinHelper.asArray(mesh.getMeshData().getTextureBuffer(0));
		
		IntBuffer indexBuffer = mesh.getMeshData().getIndexBuffer();
		this.indices = (indexBuffer == null) ? null : SkinHelper.asArray(indexBuffer);
	}
	
	MeshData(OgreMesh mesh) {
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