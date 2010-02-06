package raft.jpct.bones.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;


/** 
 * <p>Utility class to parse command line arguments passed to main method.</p>
 *  
 * @author hakan eryargi (r a f t)
 */
public class ComLineArgs implements Serializable {
	private static final long serialVersionUID = 1L;
	
    private String[] args = null;
    private List<String> argList = null; // map not used since it is not ordered.
    private List<String> unConsumed = null;
    
    /** Creates new ComLineArgs */
    public ComLineArgs(String[] args, int offset, int length) {
        this.argList = new ArrayList<String>(length); 
        for (int i = 0; i < length; i++)
        	argList.add(args[offset + i]);
        this.unConsumed = new ArrayList<String>(argList);
        this.args = new String[length];
        System.arraycopy(args, offset, this.args, 0, length);
    }
    
    /** Creates new ComLineArgs */
    public ComLineArgs(String[] args) {
        this(args, 0, args.length);
    }
    
    /** Creates new ComLineArgs */
    public ComLineArgs(String[] args, int offset) {
        this(args, offset, args.length - offset);
    }
    
    public boolean isEmpty() { return argList.isEmpty(); }
    
    public boolean containsArg(String tag) {
        unConsumed.remove(tag);
        return argList.contains(tag);
    }
    
    /** returns index'th argument */
    public String getArg(int index) throws NoSuchElementException {
        if (args.length <= index)
            throw new NoSuchElementException("index: " + index);
        
        String result = args[index];
        unConsumed.remove(result);
        return result;
    }
    
    /** returns index'th argument for tag. */
    public String getArg(String tag, int index) throws NoSuchElementException {
        if (! argList.contains(tag))
            throw new NoSuchElementException("tag [" + tag + "] not found");
        
        int tagIndex = argList.indexOf(tag);
        if (argList.size() > tagIndex + index + 1) {
            String result = argList.get(tagIndex + index + 1);
            unConsumed.remove(tag);
            unConsumed.remove(result);
            return result;
        }  else {
            throw new NoSuchElementException("argument [" + index + "] for [" + tag + "] not found.");
        }
    }
    
    /** returns first argument for tag. */
    public String getArg(String tag) throws NoSuchElementException {
        return getArg(tag, 0);
    }
    
    /** returns true still exist any unconsumed argument */
    public boolean isUnconsumed() {
        return (! unConsumed.isEmpty());
    }
    
    /** returns the unconsumed arguments */
    public List<String> getUnconsumed() {
        return unConsumed;
    }
    
    /** returns the original arguments */
    public String[] getArgs() {
        return args;
    }
}