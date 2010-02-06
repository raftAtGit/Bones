package bones.samples;

import java.awt.Dimension;
import java.io.File;
import java.net.URI;

import raft.jpct.bones.Skeleton;
import raft.jpct.bones.SkinIO;
import raft.jpct.bones.Skinned3D;
import raft.jpct.bones.SkinnedGroup;
import raft.jpct.bones.Skeleton.Debugger;

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
	protected Debugger createSkeletonDebugger() throws Exception {
		return new Skeleton.Debugger(skinnedGroup.get(0).getCurrentPose(), 10f);
	}

	@Override
	protected SkinnedGroup createSkinnedGroup() throws Exception {
		File colladaFile = new File("./samples/data/seymour/Seymour.dae");
		URI uri = colladaFile.toURI();
		
        final SimpleResourceLocator resLocater = new SimpleResourceLocator(uri.resolve("./"));
        ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_MODEL, resLocater);
        
        SkinnedGroup skinnedGroup;
		try {
			ColladaImporter colladaImporter = new ColladaImporter().loadTextures(false);
			ColladaStorage colladaStorage = colladaImporter.load(uri.toString());
			
			skinnedGroup = SkinIO.loadColladaSkin(colladaStorage, 1f);
		} finally {
			ResourceLocatorTool.removeResourceLocator(ResourceLocatorTool.TYPE_MODEL, resLocater);
		}

		Texture texture = new Texture("./samples/data/seymour/seymour_flipped.png");
		TextureManager.getInstance().addTexture("seymour", texture);
		
		for (Skinned3D o : skinnedGroup) {
			o.setTexture("seymour");
			o.build();
			o.discardSkeletonMesh();
		}
		return skinnedGroup;
	}

	
	@Override
	protected void initialize() throws Exception {
		super.initialize();
		
		world.setAmbientLight(255, 255, 255);
		
		// skeleton is oriented upside down, rotate it
		currentPose.getSkeleton().getTransform().rotateX((float)Math.PI);
		
        update(0); // update once to reflect changes visible in first scene

		autoAdjustCamera();
	}
	
	public static void main(String[] args) throws Exception {
		new ColladaSample().loop();
	}

}
