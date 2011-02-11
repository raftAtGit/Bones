package bones.samples;

import java.awt.Dimension;
import java.io.File;
import java.net.URI;

import raft.jpct.bones.Animated3D;
import raft.jpct.bones.AnimatedGroup;
import raft.jpct.bones.BonesImporter;
import raft.jpct.bones.Quaternion;
import raft.jpct.bones.SkeletonDebugger;

import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;

/**
 * <p>Demonstrates loading an Collada skin.</p>
 *  
 * @author hakan eryargi (r a f t)
 * */
public class ColladaSample extends AbstractSkinSample {

	public ColladaSample() {
		super(new Dimension(1024, 768));
	}
	
	@Override
	protected String getName() {
		return getClass().getName();
	}

	@Override
	protected SkeletonDebugger createSkeletonDebugger() throws Exception {
		return new SkeletonDebugger(animatedGroup.get(0).getSkeletonPose());
	}

	@Override
	protected AnimatedGroup createAnimatedGroup() throws Exception {
		File colladaFile = new File("./samples/data/seymour/Seymour.dae");
		//File colladaFile = new File("/home/raft/tmp/java/ardor/ardor3d-examples/src/main/resources/com/ardor3d/example/media/models/collada/skeleton/skeleton.run.dae");
		URI uri = colladaFile.toURI();
		
        final SimpleResourceLocator resLocater = new SimpleResourceLocator(uri.resolve("./"));
        ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_MODEL, resLocater);
        
        AnimatedGroup skinnedGroup;
		try {
			ColladaImporter colladaImporter = new ColladaImporter().loadTextures(false);
			ColladaStorage colladaStorage = colladaImporter.load(uri.toString());
			
			skinnedGroup = BonesImporter.importCollada(colladaStorage, 1f, new Quaternion().rotateX((float)Math.PI));
		} finally {
			ResourceLocatorTool.removeResourceLocator(ResourceLocatorTool.TYPE_MODEL, resLocater);
		}

		Texture texture = new Texture("./samples/data/seymour/seymour.png");
		TextureManager.getInstance().addTexture("seymour", texture);
		
		for (Animated3D o : skinnedGroup) {
			o.setTexture("seymour");
			o.build();
			o.discardMeshData();
		}
		return skinnedGroup;
	}

	
	@Override
	protected void initialize() throws Exception {
		super.initialize();
		
		world.setAmbientLight(255, 255, 255);
		
        update(0); // update once to reflect changes visible in first scene

		autoAdjustCamera();
	}
	
	public static void main(String[] args) throws Exception {
		new ColladaSample().loop();
	}

}
