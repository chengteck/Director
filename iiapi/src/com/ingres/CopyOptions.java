package com.ingres;

//import com.ingres.annotations.Scriptable;
import java.io.Serializable;

public final class CopyOptions implements Serializable {
	private static final long serialVersionUID = 1228450130273294238L;
	//@Scriptable
	public String cwd = null;
	//@Scriptable
	public String paramFile = null;
	//@Scriptable
	public ServerClass serverClass = null;
	//@Scriptable
	public boolean printableData = false;
	//@Scriptable
	public String effUser = null;
	//@Scriptable
	public String groupID = null;
	//@Scriptable
	public boolean groupTableIndexes = false;
	//@Scriptable
	public boolean parallel = false;
	//@Scriptable
	public boolean journal = false;
	//@Scriptable
	public boolean promptForPassword = false;
	//@Scriptable
	public String source = null;
	//@Scriptable
	public String dest = null;
	//@Scriptable
	public String outputDirectory = null;
	//@Scriptable
	public boolean withTables = false;
	//@Scriptable
	public boolean withModify = false;
	//@Scriptable
	public boolean noDependencyCheck = false;
	//@Scriptable
	public boolean withData = false;
	//@Scriptable
	public boolean all = false;
	//@Scriptable
	public boolean orderCCM = false;
	//@Scriptable
	public boolean withIndex = false;
	//@Scriptable
	public boolean withConstraints = false;
	//@Scriptable
	public boolean withViews = false;
	//@Scriptable
	public boolean withSynonyms = false;
	//@Scriptable
	public boolean withEvents = false;
	//@Scriptable
	public boolean withProcedures = false;
	//@Scriptable
	public boolean withRegistration = false;
	//@Scriptable
	public boolean withRules = false;
	//@Scriptable
	public boolean withAlarms = false;
	//@Scriptable
	public boolean withComments = false;
	//@Scriptable
	public boolean withRoles = false;
	//@Scriptable
	public boolean withSequences = false;
	//@Scriptable
	public boolean noSequences = false;
	//@Scriptable
	public boolean withPermits = false;
	//@Scriptable
	public boolean addDrop = false;
	//@Scriptable
	public String infile = null;
	//@Scriptable
	public String outfile = null;
	//@Scriptable
	public boolean relpath = false;
	//@Scriptable
	public boolean noint = false;
	//@Scriptable
	public boolean noLoc = false;
	//@Scriptable
	public boolean noPerm = false;
	//@Scriptable
	public boolean noPersist = false;
	//@Scriptable
	public boolean noRepMod = false;
	//@Scriptable
	public boolean noRep = false;
	//@Scriptable
	public boolean noLogging = false;
	//@Scriptable
	public boolean online = false;
	//@Scriptable
	public String[] tables = null;
}
