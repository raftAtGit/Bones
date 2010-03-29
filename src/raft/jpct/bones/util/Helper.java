package raft.jpct.bones.util;

import java.io.File;
import java.util.Locale;
import java.util.regex.Pattern;

import raft.jpct.bones.Quaternion;

import com.threed.jpct.Logger;

/** 
 * <p>Contains static utility methods.</p>
 *  
 * @author hakan eryargi (r a f t)
 */
class Helper {

	static final String LL_VERBOSE = "VERBOSE"; 
	static final String LL_WARNING = "WARNING"; 
	static final String LL_ERROR = "ERROR"; 

	private static final Pattern ROTATION_PATTERN = Pattern.compile("([xXyYzZ]\\d+)(,[xXyYzZ]\\d+)*");
	
	/** sets log level of jPCT {@link Logger} from a string. */
	static void setLogLevel(String level) {
		level = level.toUpperCase(Locale.ENGLISH);
		
		if (LL_VERBOSE.equals(level)) {
			Logger.setLogLevel(Logger.LL_VERBOSE);
		} else if (LL_WARNING.equals(level)) {
			Logger.setLogLevel(Logger.LL_ERRORS_AND_WARNINGS);
		} else if (LL_ERROR.equals(level)) {
			Logger.setLogLevel(Logger.LL_ONLY_ERRORS);
		} else {
			throw new IllegalArgumentException("unknown log level: " + level);
		}  
	}
	
	static void createParentDirs(File file) {
        File dir = file.getParentFile();
        if (dir != null && !dir.exists() && !dir.mkdirs())
                throw new IllegalStateException("Couldnt create " + dir);
	}

	static Quaternion parseRotation(String s) {
		if (!ROTATION_PATTERN.matcher(s).matches())
			throw new IllegalArgumentException("Invalid rotation string: " + s);
		
		Quaternion rotation = new Quaternion();
		for (String part : s.split(",")) {
			//System.out.println(part);
			float angle = (float) Math.toRadians(Double.parseDouble(part.substring(1)));
			
			switch (part.charAt(0)) {
				case 'x': 
				case 'X':
					rotation.rotateX(angle);
					break;
				case 'y': 
				case 'Y':
					rotation.rotateY(angle);
					break;
				case 'z': 
				case 'Z':
					rotation.rotateZ(angle);
					break;
				default:
					throw new IllegalArgumentException("unknown rotation axis: " + part.charAt(0));
			}
		}
		return rotation;
	}
	
	public static void main(String[] args) {
		System.out.println(parseRotation(args[0]));
	}
}
