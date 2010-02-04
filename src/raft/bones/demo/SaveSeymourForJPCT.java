package raft.bones.demo;

import java.io.FileOutputStream;

import raft.jpct.bones.SkinIO;
import raft.jpct.bones.SkinnedGroup;

import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;

/** loads seymour data via Ardor3d's loader and saves in a jPCT stream. */
public class SaveSeymourForJPCT {
	
	public static void main(String[] args) throws Exception {
		
		ColladaImporter colladaImporter = new ColladaImporter().loadTextures(false);
		ColladaStorage colladaStorage = colladaImporter.load("/raft/bones/demo/Seymour.dae");
		
		SkinnedGroup group = SkinIO.loadColladaSkin(colladaStorage, 1f);

		String fileName = "src/raft/bones/demo/group.ser";
		FileOutputStream fos = new FileOutputStream(fileName);
		try {
			SkinIO.saveGroup(group, fos);
			System.out.println("saved group to " + fileName);
		} finally {
			fos.close();
		}
		
	}
}
