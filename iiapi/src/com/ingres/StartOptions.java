package com.ingres;

//import com.ingres.annotations.Scriptable;
import java.io.Serializable;

public final class StartOptions implements Serializable {
	private static final long serialVersionUID = 817162577623611894L;
	//@Scriptable
	public boolean asService = false;
	//@Scriptable
	public IngresServer server = null;
	//@Scriptable
	public String configName = null;
	//@Scriptable
	public boolean allClusterNodes = false;
	//@Scriptable
	public String clusterNodeName = null;
}
