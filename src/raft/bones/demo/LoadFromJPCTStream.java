package raft.bones.demo;

import java.io.FileInputStream;

import raft.jpct.bones.SkinnedGroup;
import raft.jpct.bones.SkinIO;

import com.threed.jpct.IRenderer;

/** load seymour demo from a pre-saved jPCT stream. this demo requires only jPCT in classpath. */
public class LoadFromJPCTStream {
	
	private static SkinnedGroup loadGroupFromStream() throws Exception {
		String fileName = "src/raft/bones/demo/group.ser";
		FileInputStream fis = new FileInputStream(fileName);
		try {
			SkinnedGroup group = SkinIO.loadGroup(fis);
			System.out.println("loaded group from " + fileName);
			return group;
		} finally {
			fis.close();
		}
	}


	
	public static void main(String[] args) throws Exception {
		SkinnedGroup skinnedGroup = loadGroupFromStream();
		
		SeymourDemo seymour = new SeymourDemo(skinnedGroup);
		System.gc();
		seymour.loop();
		
		seymour.frameBuffer.disableRenderer(IRenderer.RENDERER_OPENGL);
		seymour.frameBuffer.dispose();
		System.exit(0);		
	}

}
