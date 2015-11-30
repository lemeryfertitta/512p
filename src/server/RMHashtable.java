// -------------------------------// Adapted from Kevin T. Manley// CSE 593// -------------------------------package server;import java.util.*;import java.io.File;import java.io.FileInputStream;import java.io.FileNotFoundException;import java.io.FileOutputStream;import java.io.IOException;import java.io.ObjectInputStream;import java.io.ObjectOutputStream;import middle.ServerName;// A specialization of Hashtable with some extra diagnostics.public class RMHashtable extends Hashtable {    public RMHashtable() {      super();    }    public String toString() {        String s = "RMHashtable { \n";        Object key = null;        for (Enumeration e = keys(); e.hasMoreElements();) {            key = e.nextElement();            String value = (String) get(key);            s = s + "  [key = " + key + "] " + value + "\n";        }        s = s + "}";        return s;    }    public void dump() {        System.out.println(toString());    }		public synchronized void save(ServerName rmName, boolean committed)	{		try {			FileOutputStream fos = new FileOutputStream(committed ? rmName.name()+"_committed.ht" : rmName.name()+"_uncommitted.ht");			ObjectOutputStream oos = new ObjectOutputStream(fos);			oos.writeObject(this);			oos.close();		} catch (IOException e) {			// TODO Auto-generated catch block			e.printStackTrace();		}			}		public static RMHashtable load(ServerName rmName, boolean committed)	{		System.out.println("Loading hashtable.");		RMHashtable table = new RMHashtable();		try {						// ref : http://www.mkyong.com/java/how-to-read-file-in-java-fileinputstream/			FileInputStream fis = new FileInputStream(new File(rmName.name()+ (committed ? "_committed.ht" : "_uncommitted.ht")));						// ref : http://www.tutorialspoint.com/java/io/objectinputstream_readobject.htm            ObjectInputStream ois = new ObjectInputStream(fis);			try {				table = (RMHashtable) ois.readObject();			} catch (ClassNotFoundException e) {				e.printStackTrace();			}			fis.close();					} catch (IOException e)		{ // does not exist, so create.			System.out.println("No hashtable found on disk - creating new file.");			table.save(rmName, committed);		}		return table;	}		public static RMHashtable loadCommittedVersion(ServerName rmName)	{		return load(rmName, true);	}		public static RMHashtable loadUncommittedVersion(ServerName rmName)	{		return load(rmName, false);	}}