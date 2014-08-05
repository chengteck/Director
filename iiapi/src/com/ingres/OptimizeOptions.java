package com.ingres;

//import com.ingres.annotations.DefaultValue;
//import com.ingres.annotations.Scriptable;
import java.io.Serializable;

public final class OptimizeOptions implements Serializable {
	private static final long serialVersionUID = 9022238770857397066L;

	public static final class TableAndCols implements Serializable {
		private static final long serialVersionUID = 8063082565154829804L;
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
	public String iFilename = null;
	//@Scriptable
	public String oFilename = null;
	//@Scriptable
	public boolean zc = false;
	//@Scriptable
	public boolean zcpk = false;
	//@Scriptable
	public boolean zdn = false;
	//@Scriptable
	public boolean ze = false;
	//@Scriptable
	public boolean zfq = false;
	//@Scriptable
	public boolean zh = false;
	//@Scriptable
	public boolean zhex = false;
	//@Scriptable
	public boolean zk = false;
	//@Scriptable
	public boolean zlr = false;
	//@Scriptable
	public boolean zns = false;
	//@Scriptable
	public boolean znt = false;
	//@Scriptable
	//@DefaultValue("-1")
	public int zn = -1;
	//@Scriptable
	public boolean zp = false;
	//@Scriptable
	//@DefaultValue("-1")
	public int zr = -1;
	//@Scriptable
	//@DefaultValue("-1.0d")
	public double zs = -1.0D;
	//@Scriptable
	//@DefaultValue("-1.0d")
	public double zss = -1.0D;
	//@Scriptable
	//@DefaultValue("-1")
	public int zu = -1;
	//@Scriptable
	public boolean zv = false;
	//@Scriptable
	public boolean zw = false;
	//@Scriptable
	public boolean zx = false;
	public TableAndCols[] rTables = null;
	//@Scriptable
	public String[] xrTables = null;
}
