package raft.bones.demo;

import raft.jpct.bones.SkinIO;
import raft.jpct.bones.SkinnedGroup;

import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.threed.jpct.IRenderer;

/** load seymour demo by using Ardor3d's collada loader. this demo requires some ardor3d classes and their dependencies and jPCT in classpath. */
public class LoadFromArdor {
	
	private static SkinnedGroup loadArdorSkinGroup() {
		
		ColladaImporter colladaImporter = new ColladaImporter().loadTextures(false);
		ColladaStorage colladaStorage = colladaImporter.load("/raft/bones/demo/Seymour.dae");
		return SkinIO.loadColladaSkin(colladaStorage, 1f);
	}
	
	
	public static void main(String[] args) throws Exception {
		SkinnedGroup skinnedGroup = loadArdorSkinGroup();
		
		SeymourDemo seymour = new SeymourDemo(skinnedGroup);
		System.gc();
		seymour.loop();
		
		seymour.frameBuffer.disableRenderer(IRenderer.RENDERER_OPENGL);
		seymour.frameBuffer.dispose();
		System.exit(0);		
	}

}
