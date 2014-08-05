package com.ingres;

//import com.ingres.annotations.DefaultValue;
//import com.ingres.annotations.Scriptable;
import java.io.Serializable;

public final class StatdumpOptions implements Serializable {
	private static final long serialVersionUID = 2865504392478578224L;

	public static final class TableAndCols implements Serializable {
		private static final long serialVersionUID = 3464177817371570536L;
		public final String rTable;
		public final String[] aCols;

		public TableAndCols(String rTable, String[] aCols) {
			this.rTable = rTable;
			this.aCols = aCols;
		}
	}

	//@Scriptable
	public String cwd = null;
	//@Scriptable
	public String zf = null;
	//@Scriptable
	public ServerClass serverClass = null;
	//@Scriptable
	public String effUser = null;
	//@Scriptable
	public String[] sqlOpts = null;
	//@Scriptable
	public boolean zc = false;
	//@Scriptable
	public boolean zcpk = false;
	//@Scriptable
	public boolean zdl = false;
	//@Scriptable
	public boolean zhex = false;
	//@Scriptable
	//@DefaultValue("-1")
	public int zn = -1;
	//@Scriptable
	public boolean zq = false;
	//@Scriptable
	public String oFilename = null;
	public TableAndCols[] rTables = null;
	//@Scriptable
	public String[] xrTables = null;
}
