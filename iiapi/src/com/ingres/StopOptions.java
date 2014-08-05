package com.ingres;

//import com.ingres.annotations.Scriptable;
import java.io.Serializable;

public final class StopOptions implements Serializable {
	private static final long serialVersionUID = -997739135770765737L;
	//@Scriptable
	public boolean asService = false;
	//@Scriptable
	public IngresServer server = null;
	//@Scriptable
	public String connectId = null;
	//@Scriptable
	public boolean f = false;
	//@Scriptable
	public int timeout = 0;
	//@Scriptable
	public boolean kill = false;
	//@Scriptable
	public boolean force = false;
	//@Scriptable
	public boolean immediate = false;
	//@Scriptable
	public boolean show = false;
	//@Scriptable
	public boolean allClusterNodes = false;
	//@Scriptable
	public String clusterNodeName = null;
}
