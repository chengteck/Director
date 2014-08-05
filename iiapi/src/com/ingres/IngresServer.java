package com.ingres;

import java.util.EnumSet;

public enum IngresServer {
	INGRES("dbms", "iidbms"), IINMSVR("dbms", "iigcn"), IUSVR("dbms", "dmfrcp"), COMSVR(
			"gcc", "iigcc"), DASVR("gcd", "iigcd"), BRIDGE("gcb", "iigcb"), NMSVR(
			"dbms", "iigcn"), DMFACP("", "dmfacp"), CLIENT("", "client"), STAR(
			"star", "iistar"), RMCMD("rmcmd"), DB2UDB("db2udb"), ORACLE(
			"oracle"), SYBASE("sybase"), INFORMIX("informix"), MSSQL("mssql"), RDB(
			"rdb"), RMS("rms"), ICESVR("icesvr"), JDBC("jdbc"), MGMTSVR(
			"mgmtsvr");

	private String startupName;
	private String startupFlag;
	public static final String DEFAULT = "(default)";

	private IngresServer(String startupName) {
		this.startupName = startupName;
		this.startupFlag = null;
	}

	private IngresServer(String startupName, String startupFlag) {
		this.startupName = startupName;
		this.startupFlag = startupFlag;
	}

	public static EnumSet<IngresServer> gatewayServerSet() {
		return EnumSet.range(DB2UDB, RMS);
	}

	public String getStartupName() {
		return this.startupName;
	}

	public String getStartupFlag() {
		if (this.startupFlag == null) {
			return this.startupName;
		}
		return this.startupFlag;
	}

	public static IngresServer findEntry(String startupName)
   {
	 //todo after :
     for (IngresServer is : IngresServer.values()) {
       if (is.startupName.equalsIgnoreCase(startupName)) {
         return is;
       }
     }
     throw new IllegalArgumentException(String.format("No IngresServer entry corresponding to \"%1$s\" startup name.", new Object[] { startupName }));
   }
}
