package raft.jpct.bones.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import raft.jpct.bones.BonesIO;
import raft.jpct.bones.AnimatedGroup;
import raft.jpct.bones.BonesImporter;
import raft.jpct.bones.Quaternion;

import com.jmex.model.ogrexml.OgreEntityNode;
import com.jmex.model.ogrexml.OgreLoader;
import com.threed.jpct.Logger;

/** 
 * <p>Utility class to import Ogre3D skins via jME's ogrexml loader. 
 * Can also be used as a command line tool. Use the script in scripts folder to use from command line.</p>
 *  
 * @author hakan eryargi (r a f t)
 */
public class JMEOgreImporter {
	
	private final File outFile;
	private final List<File> inputFiles;
	private final float scale;
	private final Quaternion rotation;
//	private boolean mergeAnimations = true;
	
	/** 
	 * Creates a new importer with given parameters.
	 * 
	 * @param outFile destination file to write bones group. May be null.
	 * @param inputFiles list of Ogre3D mesh xml files. There must be at least one. If there are many,
	 * 		their skeletons must match. 
	 * @param scale the scaling of group
	 * @param rotation the rotation applied while loading 
	 * 
	 * @see BonesImporter#importOgre(OgreEntityNode, float, Quaternion)
	 * @see AnimatedGroup#mergeAnimations(AnimatedGroup...)
	 * */
	public JMEOgreImporter(File outFile, List<File> inputFiles, float scale, Quaternion rotation) {
		
		if (inputFiles.isEmpty())
			throw new IllegalArgumentException("No input files");
		
		this.outFile = outFile;
		this.inputFiles = inputFiles;
		this.scale  = scale;
		this.rotation = rotation;
	}
	
	/** Sets if merge animations. By default animations are merged if many input files are given. Otherwise groups are merged.
	 *  
	 * @param mergeAnimations merge animations or groups
	 * @return this for chaining */
//	public JMEOgreImporter setMergeAnimations(boolean mergeAnimations) {
//		this.mergeAnimations = mergeAnimations;
//		return this;
//	}

	/** Executes the importer. */
	public void run() throws Exception {
		final AnimatedGroup group = loadGroup();
		
		if (outFile != null) {
			if (outFile.isDirectory())
				throw new IllegalArgumentException("Out file is a directory: " + outFile);
			Helper.createParentDirs(outFile);
			
			FileOutputStream fos = new FileOutputStream(outFile);
			try {
				BonesIO.saveGroup(group, fos);
				Logger.log("Saved bones-group to " + outFile, Logger.MESSAGE);
			} finally {
				fos.close();
			}
		}
	}
	
	private AnimatedGroup loadGroup() throws Exception { 
		if (inputFiles.size() == 1) {
			return loadGroup(inputFiles.get(0));
		} else {
			List<AnimatedGroup> groups = new LinkedList<AnimatedGroup>();
			for (File input : inputFiles) {
				groups.add(loadGroup(input));
			}
			Logger.log("Merging animations in multiple input files", Logger.MESSAGE);
			return AnimatedGroup.mergeAnimations(groups.toArray(new AnimatedGroup[groups.size()]));
//			if (mergeAnimations) {
//				Logger.log("Merging animations in multiple input files", Logger.MESSAGE);
//				return AnimatedGroup.mergeAnimations(groups.toArray(new AnimatedGroup[groups.size()]));
//			} else {
//				Logger.log("Merging groups in multiple input files", Logger.MESSAGE);
//				return AnimatedGroup.mergeGroups(groups.toArray(new AnimatedGroup[groups.size()]));
//			}
		}
	}
	
	private AnimatedGroup loadGroup(File meshFile) throws Exception {
		URL url = meshFile.toURI().toURL();
		
		OgreLoader loader = new OgreLoader();
		OgreEntityNode node = loader.loadModel(url);

		AnimatedGroup group = BonesImporter.importOgre(node, scale, rotation);
		
		int skinAnims = (group.getSkinClipSequence() == null) ? 0 : group.getSkinClipSequence().getSize();
		int poseAnims = (group.getPoseClipSequence() == null) ? 0 : group.getPoseClipSequence().getSize();
		
		Logger.log(MessageFormat.format("Loaded ogre file: {0}, scale: {1}, rotation: {2}\n" +
				"\t{3} sub objects, {4} skin animation(s), {5} pose animation(s)", 
				meshFile, scale, rotation, group.getSize(), skinAnims, poseAnims), Logger.MESSAGE);
		
		return group;
	}

	private static void printUsage(PrintStream ps) {
        ps.println("usage: JMEOgreImporter [options] -in <ogre.mesh.xml> [ogre.mesh.xml...]");
        ps.println("options:");
        ps.println("    -out <destination file>                         : destination file to write");
        ps.println("    -scale <scale>                                  : loading scale, default 1");
        ps.println("    -rotation <<x|y|zdegrees>[,x|y|zdegrees]...>    : loading rotation, default none (sample: x180,y180)");
//        ps.println("    -mergeGroups                                    : merge groups if many input files are given. by default animations are merged");
        ps.println("    -h | -help                                      : print help");
        ps.println("    -log <logLevel: VERBOSE*|WARNING|ERROR>         : set log level");
    }

	
	/** Command line entry method. */
	public static void main(String[] args) throws Exception {
		ComLineArgs comLine = new ComLineArgs(args);
		
        if (comLine.isEmpty() || comLine.containsArg("-h") || comLine.containsArg("-help")) {
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
        Quaternion rotation = comLine.containsArg("-rotation") ? 
        		Helper.parseRotation(comLine.getArg("-rotation")) : null; 
        
//        boolean mergeAnimations = !comLine.containsArg("-mergeGroups");
        
        if (comLine.isUnconsumed())
            throw new IllegalArgumentException("Unknown args: " + comLine.getUnconsumed());
        
//        new JMEOgreImporter(outFile, inputFiles, scale, rotation).setMergeAnimations(mergeAnimations).run();
        new JMEOgreImporter(outFile, inputFiles, scale, rotation).run();
        
	}
}
