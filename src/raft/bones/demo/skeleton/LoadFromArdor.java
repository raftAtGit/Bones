package raft.bones.demo.skeleton;

import raft.jpct.bones.SkinnedGroup;
import raft.jpct.bones.SkinIO;

import com.ardor3d.extension.model.collada.jdom.ColladaImporter;

/** load seymour demo by using Ardor3d's collada loader. this demo requires some ardor3d classes and their dependencies and jPCT in classpath. */
public class LoadFromArdor {
	
	static SkinnedGroup loadArdorSkinGroup() {
		
		ColladaImporter colladaImporter = new ColladaImporter().loadTextures(false);
		
		SkinnedGroup g1 = SkinIO.loadColladaSkin(colladaImporter.load("/raft/bones/demo/skeleton/skeleton.punch.dae"), 1f);
		SkinnedGroup g2 = SkinIO.loadColladaSkin(colladaImporter.load("/raft/bones/demo/skeleton/skeleton.walk.dae"), 1f);
		SkinnedGroup g3 = SkinIO.loadColladaSkin(colladaImporter.load("/raft/bones/demo/skeleton/skeleton.run.dae"), 1f);
		
		return SkinnedGroup.mergeSkin(g1, g2, g3);
	}
	
	
	public static void main(String[] args) throws Exception {
		//Logger.setOnError(Logger.ON_ERROR_THROW_EXCEPTION);
		SkinnedGroup skinnedGroup = loadArdorSkinGroup();
		
		SkeletonDemo demo = new SkeletonDemo();
		System.gc();
		demo.loop();
		
		System.exit(0);		
	}

}
