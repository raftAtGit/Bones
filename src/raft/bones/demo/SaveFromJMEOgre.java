package raft.bones.demo;

import java.io.FileOutputStream;
import java.net.URL;

import raft.jpct.bones.SkinIO;
import raft.jpct.bones.SkinnedGroup;

import com.jmex.model.ogrexml.OgreEntityNode;
import com.jmex.model.ogrexml.OgreLoader;

/** load seymour demo by using Ardor3d's collada loader. this demo requires some ardor3d classes and their dependencies and jPCT in classpath. */
public class SaveFromJMEOgre {
	
	
	public static void main(String[] args) throws Exception {
		URL url = SaveFromJMEOgre.class.getResource("/jmetest/data/model/ogrexml/ninja.mesh.xml");
		
		OgreLoader loader = new OgreLoader();
		OgreEntityNode node = loader.loadModel(url, null);

		SkinnedGroup group = SkinIO.loadOgreSkin(node, 1f);

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
