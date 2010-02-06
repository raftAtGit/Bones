package raft.jpct.bones.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import raft.jpct.bones.SkinIO;
import raft.jpct.bones.SkinnedGroup;

import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;
import com.threed.jpct.Logger;

/** 
 * <p>Utility class to import Collada skins via Ardor3D's collada loader. 
 * Can also be used as a command line tool.</p>
 *  
 * @author hakan eryargi (r a f t)
 */
public class ArdorColladaImporter {
	
	private final File outFile;
	private final List<File> inputFiles;
	private final float scale;
	
	public ArdorColladaImporter(File outFile, List<File> inputFiles, float scale) {
		if (inputFiles.isEmpty())
			throw new IllegalArgumentException("No input files");
		
		this.outFile = outFile;
		this.inputFiles = inputFiles;
		this.scale  = scale;
	}

	public void run() throws Exception {
		final SkinnedGroup group = loadGroup();
		
		if (outFile != null) {
			if (outFile.isDirectory())
				throw new IllegalArgumentException("Out file is a directory: " + outFile);
			Helper.createParentDirs(outFile);
			
			FileOutputStream fos = new FileOutputStream(outFile);
			try {
				SkinIO.saveGroup(group, fos);
				Logger.log("Saved bones-group to " + outFile, Logger.MESSAGE);
			} finally {
				fos.close();
			}
		}
	}
	
	private SkinnedGroup loadGroup() throws Exception { 
		if (inputFiles.size() == 1) {
			return loadGroup(inputFiles.get(0));
		} else {
			List<SkinnedGroup> groups = new LinkedList<SkinnedGroup>();
			for (File input : inputFiles) {
				groups.add(loadGroup(input));
			}
			return SkinnedGroup.mergeSkin(groups.toArray(new SkinnedGroup[groups.size()]));
		}
	}
	
	private SkinnedGroup loadGroup(File colladaFile) throws Exception {
		URI uri = colladaFile.toURI();
		
        final SimpleResourceLocator resLocater = new SimpleResourceLocator(uri.resolve("./"));
        ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_MODEL, resLocater);
        
		try {
			ColladaImporter colladaImporter = new ColladaImporter().loadTextures(false);
			ColladaStorage colladaStorage = colladaImporter.load(uri.toString());
			
			return SkinIO.loadColladaSkin(colladaStorage, scale);
		} finally {
			ResourceLocatorTool.removeResourceLocator(ResourceLocatorTool.TYPE_MODEL, resLocater);
		}
	}

	private static void printUsage(PrintStream ps) {
        ps.println("usage: ArdorColladaImporter [options] -in <collada file> [collada file...]");
        ps.println("options:");
        ps.println("    -out <destination file>                     : destination file to write");
        ps.println("    -scale <scale>                              : loading scale, default 1");
        ps.println("    -h | -help                                  : print help");
        ps.println("    -log <logLevel: VERBOSE*|WARNING|ERROR>     : set log level");
    }

	
	public static void main(String[] args) throws Exception {
		ComLineArgs comLine = new ComLineArgs(args);
		
        if (comLine.containsArg("-h") || comLine.containsArg("-help")) {
            printUsage(System.out);
            System.exit(0);
        }
        
        if (comLine.containsArg("-log"))
        	Helper.setLogLevel(comLine.getArg("-log"));

        List<File> inputFiles = new LinkedList<File>();
        inputFiles.add(new File(comLine.getArg("-in")));
        
        for (int i = 1;; i++) {
        	try {
        		inputFiles.add(new File(comLine.getArg("-in", i)));
        	} catch (NoSuchElementException e) {
        		break;
        	}
        }
        
        File outFile = comLine.containsArg("-out") ? new File(comLine.getArg("-out")) : null;
        float scale = comLine.containsArg("-scale") ? Float.parseFloat(comLine.getArg("-scale")) : 1f;
        
        if (comLine.isUnconsumed())
            throw new IllegalArgumentException("Unknown args: " + comLine.getUnconsumed());
        
        new ArdorColladaImporter(outFile, inputFiles, scale).run();
        
	}
}
