package raft.jpct.bones.util;

import java.io.File;
import java.util.Locale;

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
}
