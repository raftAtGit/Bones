package raft.jpct.bones;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/** 
 * <p>Contains static loader and saver methods.</p> 
 * 
 * @author hakan eryargi (r a f t)
 * */
public class BonesIO {

	private static final String HEADER_GROUP = "Bones-Group";
	private static final String HEADER_OBJECT = "Bones-Object";
	private static final short VERSION = 2;
	
	/** can not be instantiated */
	private BonesIO() {}
	
	/** 
	 * <p>Saves given skin group to given stream.</p>
	 * @see AnimatedGroup 
	 * */
	public static void saveGroup(AnimatedGroup group, OutputStream out) throws IOException {
		ObjectOutputStream oout = new ObjectOutputStream(out);
		writeHeader(oout, HEADER_GROUP);
		group.writeToStream(oout);
		oout.flush();
	}
	
	/** 
	 * <p>Saves given {@link Animated3D} to given stream.</p> 
	 * @see Animated3D 
	 * */
	public static void saveObject(Animated3D object, OutputStream out) throws IOException {
		ObjectOutputStream oout = new ObjectOutputStream(out);
		writeHeader(oout, HEADER_OBJECT);
		object.writeToStream(oout);
		oout.flush();
	}
	
	/** 
	 * <p>Loads an {@link Animated3D} from given stream. Object should be saved to stream
	 * via {@link #saveObject(Animated3D, OutputStream)}</p>
	 *  
	 * @see Animated3D
	 * @see #saveObject(Animated3D, OutputStream) 
	 * */
	public static Animated3D loadObject(InputStream in) throws IOException, ClassNotFoundException {
		ObjectInputStream oin = new ObjectInputStream(in);
		readHeader(oin, HEADER_OBJECT);
		return new Animated3D(oin);
	}
	
	/** 
	 * <p>Loads a {@link AnimatedGroup}'s from given stream. Group should be saved to stream
	 * via {@link #saveGroup(AnimatedGroup, OutputStream)}</p>
	 *  
	 * @see AnimatedGroup
	 * @see #saveGroup(AnimatedGroup, OutputStream) 
	 * */
	public static AnimatedGroup loadGroup(InputStream in) throws IOException, ClassNotFoundException {
		ObjectInputStream oin = new ObjectInputStream(in);
		readHeader(oin, HEADER_GROUP);
		return new AnimatedGroup(oin);
	}

	private static void writeHeader(java.io.ObjectOutputStream out, String header) throws IOException {
		out.writeUTF(header);
		out.writeShort(VERSION);
	}
	
	private static void readHeader(java.io.ObjectInputStream in, String requiredHeader) throws IOException {
		String header = in.readUTF();
		if (!requiredHeader.equals(header))
			throw new IOException("Invalid header: " + header);
		short version = in.readShort();
		if (VERSION != version)
			throw new IOException("Version mismatch. Current version: " + VERSION + ", stream version: " + version);
	}

}
