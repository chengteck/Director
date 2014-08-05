package com.ingres;

//import com.ingres.annotations.DefaultValue;
//import com.ingres.annotations.Scriptable;
import java.io.Serializable;

public final class InfoOptions implements Serializable {
	private static final long serialVersionUID = 91473139349316967L;
	//@Scriptable
	public ServerClass serverClass = null;
	//@Scriptable
	public String effUser = null;
	//@Scriptable
	//@DefaultValue("-1")
	public int checkpointNumber = -1;
}