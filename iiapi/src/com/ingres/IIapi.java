package com.ingres;

import com.ingres.util.Intl;
import com.ingres.util.Logging;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
 
 public abstract interface IIapi
 {
   public static final Package pkg = IIapi.class.getPackage();
   public static final short HNDL_TYPE = -1;
   public static final short DTE_TYPE = 3;
   public static final short DATE_TYPE = 4;
   public static final short MNY_TYPE = 5;
   public static final short TMWO_TYPE = 6;
   public static final short TMTZ_TYPE = 7;
   public static final short TIME_TYPE = 8;
   public static final short TSWO_TYPE = 9;
   public static final short DEC_TYPE = 10;
   public static final short LOGKEY_TYPE = 11;
   public static final short TABKEY_TYPE = 12;
   public static final short TSTZ_TYPE = 18;
   public static final short TS_TYPE = 19;
   public static final short CHA_TYPE = 20;
   public static final short VCH_TYPE = 21;
   public static final short LVCH_TYPE = 22;
   public static final short BYTE_TYPE = 23;
   public static final short VBYTE_TYPE = 24;
   public static final short LBYTE_TYPE = 25;
   public static final short NCHA_TYPE = 26;
   public static final short NVCH_TYPE = 27;
   public static final short LNVCH_TYPE = 28;
   public static final short LNLOC_TYPE = 29;
   public static final short INT_TYPE = 30;
   public static final short FLT_TYPE = 31;
   public static final short CHR_TYPE = 32;
   public static final short INTYM_TYPE = 33;
   public static final short INTDS_TYPE = 34;
   public static final short LBLOC_TYPE = 35;
   public static final short LCLOC_TYPE = 36;
   public static final short TXT_TYPE = 37;
   public static final short BOOL_TYPE = 38;
   public static final short LTXT_TYPE = 41;
   public static final short GEOM_TYPE = 56;
   public static final short POINT_TYPE = 57;
   public static final short MPOINT_TYPE = 58;
   public static final short LINE_TYPE = 59;
   public static final short MLINE_TYPE = 61;
   public static final short POLY_TYPE = 62;
   public static final short MPOLY_TYPE = 63;
   public static final short NBR_TYPE = 64;
   public static final short GEOMC_TYPE = 65;
   public static final short COL_TUPLE = 0;
   public static final short COL_PROCBYREFPARM = 1;
   public static final short COL_PROCPARM = 2;
   public static final short COL_SVCPARM = 3;
   public static final short COL_QPARM = 4;
   public static final short COL_PROCGTTPARM = 5;
   public static final short COL_PROCINPARM = 2;
   public static final short COL_PROCOUTPARM = 6;
   public static final short COL_PROCINOUTPARM = 1;
   public static final int ST_SUCCESS = 0;
   public static final int ST_MESSAGE = 1;
   public static final int ST_WARNING = 2;
   public static final int ST_NO_DATA = 3;
   public static final int ST_ERROR = 4;
   public static final int ST_FAILURE = 5;
   public static final int ST_NOT_INITIALIZED = 6;
   public static final int ST_INVALID_HANDLE = 7;
   public static final int ST_OUT_OF_MEMORY = 8;
   public static final int GQF_FAIL = 1;
   public static final int GQF_ALL_UPDATED = 2;
   public static final int GQF_NULLS_REMOVED = 4;
   public static final int GQF_UNKNOWN_REPEAT_QUERY = 8;
   public static final int GQF_END_OF_DATA = 16;
   public static final int GQF_CONTINUE = 32;
   public static final int GQF_INVALID_STATEMENT = 64;
   public static final int GQF_TRANSACTION_INACTIVE = 128;
   public static final int GQF_OBJECT_KEY = 256;
   public static final int GQF_TABLE_KEY = 512;
   public static final int GQF_NEW_EFFECTIVE_USER = 1024;
   public static final int GQF_FLUSH_QUERY_ID = 2048;
   public static final int GQF_ILLEGAL_XACT_STMT = 4096;
   public static final int GQ_ROW_COUNT = 1;
   public static final int GQ_CURSOR = 2;
   public static final int GQ_PROCEDURE_RET = 4;
   public static final int GQ_PROCEDURE_ID = 8;
   public static final int GQ_REPEAT_QUERY_ID = 16;
   public static final int GQ_TABLE_KEY = 32;
   public static final int GQ_OBJECT_KEY = 64;
   public static final int GQ_ROW_STATUS = 128;
   public static final int LG_PRIMARY_TRANS_LOG = 1;
   public static final int LG_DUAL_TRANS_LOG = 2;
   
   public abstract void unloadInstallation();
   
   public abstract boolean isRemote();
   
   public abstract SessionAuth.AuthType getAuthType();
   
   public abstract TransactionHandle autoCommitOn(ConnectionHandle paramConnectionHandle);
   
   public abstract void autoCommitOff(TransactionHandle paramTransactionHandle);
   
   public abstract void commit(TransactionHandle paramTransactionHandle);
   
   public abstract void rollback(TransactionHandle paramTransactionHandle);
   
   public abstract ConnectionHandle connectNameServer(String paramString1, String paramString2, byte[] paramArrayOfByte, int paramInt);
   
   public abstract ConnectionHandle connectNameServer(String paramString1, String paramString2, byte[] paramArrayOfByte, int paramInt, String paramString3);
   
   public abstract ConnectionHandle connectDatabase(String paramString1, String paramString2, byte[] paramArrayOfByte, int paramInt);
   
   public abstract ConnectionHandle connectDatabase(String paramString1, String paramString2, byte[] paramArrayOfByte, int paramInt, ConnectOptions paramConnectOptions);
   
   public abstract void abort(ConnectionHandle paramConnectionHandle);
   
   public abstract void disconnect(ConnectionHandle paramConnectionHandle);
   
   @Deprecated
   public abstract StatementHandle query(ConnectionHandle paramConnectionHandle, TransactionHandle paramTransactionHandle, String paramString, boolean paramBoolean);
   
   public abstract QueryInfo executeStatement(ConnectionHandle paramConnectionHandle, TransactionHandle paramTransactionHandle, String paramString, Descriptor[] paramArrayOfDescriptor, DataValue[] paramArrayOfDataValue);
   
   public abstract Descriptor[] executeQuery(ConnectionHandle paramConnectionHandle, TransactionHandle paramTransactionHandle, StatementHandle paramStatementHandle, String paramString, Descriptor[] paramArrayOfDescriptor, DataValue[] paramArrayOfDataValue);
   
   public abstract QueryInfo endQuery(StatementHandle paramStatementHandle);
   
   public abstract StatementHandle executeProcedure(ConnectionHandle paramConnectionHandle, TransactionHandle paramTransactionHandle, Descriptor[] paramArrayOfDescriptor, DataValue[] paramArrayOfDataValue);
   
   public abstract StatementHandle executeProcedure(ConnectionHandle paramConnectionHandle, TransactionHandle paramTransactionHandle, String paramString1, String paramString2, Descriptor[] paramArrayOfDescriptor, DataValue[] paramArrayOfDataValue);
   
   public abstract void putParameters(StatementHandle paramStatementHandle, Descriptor[] paramArrayOfDescriptor, DataValue[] paramArrayOfDataValue);
   
   public abstract void cancel(StatementHandle paramStatementHandle);
   
   public abstract void close(StatementHandle paramStatementHandle);
   
   public abstract Descriptor[] getDescriptors(StatementHandle paramStatementHandle);
   
   public abstract boolean getColumns(StatementHandle paramStatementHandle, Descriptor[] paramArrayOfDescriptor, DataValue[] paramArrayOfDataValue);
   
   public abstract int getRows(StatementHandle paramStatementHandle, Descriptor[] paramArrayOfDescriptor, DataValue[][] paramArrayOfDataValue, boolean paramBoolean);
   
   public abstract QueryInfo getQueryInfo(StatementHandle paramStatementHandle);
   
   public abstract Exception getDecodingError();
   
   public abstract void resetDecodingError();
   
   public abstract void setDecodingReplacement(String paramString);
   
   public abstract String getEnv(String paramString);
   
   public abstract Map<String, String> NMsymbols();
   
   public abstract String IDname();
   
   public abstract String IDname_service();
   
   public abstract String IDnameServer();
   
   public abstract VersionInfo getVersionInfo();
   
   public abstract String getVersionString();
   
   public abstract VersionInfo getAPIVersionInfo();
   
   public abstract String getAPIVersionString();
   
   public abstract SysInfo getSysInfo();
   
   public abstract void elevationRequired();
   
   public abstract void elevationRequiredWarning();
   
   public abstract boolean isElevationRequired();
   
   public abstract boolean checkPrivilege(String paramString, Privileges paramPrivileges);
   
   public abstract boolean isClientFullyAuthorized();
   
   public abstract void GCusrpwd(String paramString, byte[] paramArrayOfByte)
     throws IIapi.Exception;
   
   public abstract String GChostname(boolean paramBoolean);
   
   public abstract int GCtcpIpPort(String paramString, int paramInt);
   
   public abstract String CMgetCharsetName();
   
   public abstract String CMgetStdCharsetName();
   
   public abstract ContextHandle PMinit();
   
   public abstract String PMexpToRegExp(ContextHandle paramContextHandle, String paramString);
   
   public abstract void PMfree(ContextHandle paramContextHandle);
   
   public abstract String PMget(ContextHandle paramContextHandle, String paramString);
   
   public abstract String PMgetDefault(ContextHandle paramContextHandle, int paramInt);
   
   public abstract String PMhost(ContextHandle paramContextHandle);
   
   public abstract void PMload(ContextHandle paramContextHandle, String paramString);
   
   public abstract void PMload(ContextHandle paramContextHandle, File paramFile);
   
   public abstract int PMnumElem(ContextHandle paramContextHandle, String paramString);
   
   public abstract void PMrestrict(ContextHandle paramContextHandle, String paramString);
   
   public abstract Map<String, String> PMscan(ContextHandle paramContextHandle, String paramString);
   
   public abstract void PMsetDefault(ContextHandle paramContextHandle, int paramInt, String paramString);
   
   public abstract void PMwrite(ContextHandle paramContextHandle, File paramFile);
   
   public abstract void PMlowerOn(ContextHandle paramContextHandle);
   
   public abstract void PMinsert(ContextHandle paramContextHandle, String paramString1, String paramString2);
   
   public abstract void PMdelete(ContextHandle paramContextHandle, String paramString);
   
   public abstract void PMwrite(ContextHandle paramContextHandle, String paramString);
   
   public abstract void CRsetPMval(ContextHandle paramContextHandle, String paramString1, String paramString2);
   
   public abstract String[] LGgetLogPaths(int paramInt, String paramString);
   
   public abstract boolean isTransactionLogConfigured(int paramInt, String paramString);
   
   public abstract FileHandle FSopenFile(String paramString);
   
   public abstract int FSreadFile(FileHandle paramFileHandle, long paramLong, byte[] paramArrayOfByte);
   
   public abstract void FScloseFile(FileHandle paramFileHandle);
   
   public abstract UtilityProcessHandle UTcreateDatabase(String paramString, CreateDBOptions paramCreateDBOptions);
   
   public abstract UtilityProcessHandle UTdestroyDatabase(String paramString, DestroyOptions paramDestroyOptions);
   
   public abstract UtilityProcessHandle UTstartIngres(StartOptions paramStartOptions);
   
   public abstract UtilityProcessHandle UTstopIngres(StopOptions paramStopOptions);
   
   public abstract String[] FSgetFileList(String paramString1, String paramString2, String paramString3);
   
   public abstract String[] FSgetNonUnicodeCollations();
   
   public abstract String[] FSgetUnicodeCollations();
   
   public abstract String[] FSgetLogFiles();
   
   public abstract void UTwriteProcessInput(UtilityProcessHandle paramUtilityProcessHandle, String paramString);
   
   public abstract String UTreadProcessOutput(UtilityProcessHandle paramUtilityProcessHandle, int paramInt, boolean paramBoolean);
   
   public abstract int UTgetProcessExitValue(UtilityProcessHandle paramUtilityProcessHandle);
   
   public abstract int UTwaitForProcessExitValue(UtilityProcessHandle paramUtilityProcessHandle);
   
   public abstract void UTkillProcess(UtilityProcessHandle paramUtilityProcessHandle);
   
   public abstract void UTreleaseProcessHandle(UtilityProcessHandle paramUtilityProcessHandle);
   
   public abstract UtilityProcessHandle UTbackupDatabase(String paramString, BackupOptions paramBackupOptions);
   
   public abstract UtilityProcessHandle UTrestoreDatabase(String paramString, RestoreOptions paramRestoreOptions);
   
   public abstract UtilityProcessHandle UToptimizeDatabase(String paramString, OptimizeOptions paramOptimizeOptions);
   
   public abstract UtilityProcessHandle UTsysmodDatabase(String paramString, SysmodOptions paramSysmodOptions);
   
   public abstract UtilityProcessHandle UTusermodDatabase(String paramString, UsermodOptions paramUsermodOptions);
   
   public abstract UtilityProcessHandle UTvwLoad(String paramString, VWLoadOptions paramVWLoadOptions);
   
   public abstract UtilityProcessHandle UTalterDatabase(String paramString, AlterOptions paramAlterOptions);
   
   public abstract UtilityProcessHandle UTextendDatabase(String paramString, ExtendOptions paramExtendOptions);
   
   public abstract UtilityProcessHandle UTverifyDatabase(String paramString, VerifyOptions paramVerifyOptions);
   
   public abstract UtilityProcessHandle UTstatdumpDatabase(String paramString, StatdumpOptions paramStatdumpOptions);
   
   public abstract UtilityProcessHandle UTcopyDatabase(String paramString, CopyOptions paramCopyOptions);
   
   public abstract UtilityProcessHandle UTunloadDatabase(String paramString, UnloadOptions paramUnloadOptions);
   
   public abstract UtilityProcessHandle UTauditDatabase(String paramString, AuditOptions paramAuditOptions);
   
   public abstract UtilityProcessHandle UTinfoDatabase(String paramString, InfoOptions paramInfoOptions);
   
   public abstract UtilityProcessHandle UTvwinfoDatabase(String paramString, VWInfoOptions paramVWInfoOptions);
   
   public abstract String getLogFilesDirectory();
   
   public static enum InstallType
   {
     INGRES("II", "Ingres"),  VECTORWISE("VW", "Vectorwise");
     
     private String prefix;
     private String displayName;
     
     private InstallType(String prefix, String displayName)
     {
       this.prefix = prefix;
       this.displayName = displayName;
     }
     
     public String getPrefix()
     {
       return this.prefix;
     }
     
     public String getDisplayName()
     {
       return this.displayName;
     }
     
     public static InstallType findType(String type)
     {
       // "InstallType.values()" is not confirmed!!!
       for (InstallType it : InstallType.values()) {
         if (it.prefix.equalsIgnoreCase(type)) {
           return it;
         }
       }
       throw new IllegalArgumentException(Intl.formatString(IIapi.pkg, "iiapi.badInstallType", new Object[] { type }));
     }
   }
   
   public static class VersionInfo implements Serializable
   {
     private static final long serialVersionUID = -5446238918920215931L;
     private static final Pattern versionPattern = Pattern.compile("^(\\S\\S)\\s+((\\d+)\\.(\\d+)\\.(\\d+)\\s+\\(([^/]+)/(\\d+)\\)).*");
     private int majorVersion = -1;
     private int minorVersion = -1;
     private int revision = -1;
     private String platform = "unknown";
     private int build = -1;
     private String releaseString = "unknown";
     private IIapi.InstallType type = IIapi.InstallType.INGRES;
     
     public VersionInfo() {}
     
     public VersionInfo(String versionString)
     {
       Matcher m = versionPattern.matcher(versionString);
       if ((m.matches()) && (m.groupCount() == 7))
       {
         this.type = IIapi.InstallType.findType(m.group(1));
         this.releaseString = m.group(2);
         this.majorVersion = Integer.parseInt(m.group(3));
         this.minorVersion = Integer.parseInt(m.group(4));
         this.revision = Integer.parseInt(m.group(5));
         this.platform = m.group(6);
         this.build = Integer.parseInt(m.group(7));
       }
     }
     
     public boolean equals(Object o)
     {
       if ((o != null) && ((o instanceof VersionInfo)))
       {
         VersionInfo vi = (VersionInfo)o;
         if ((vi.majorVersion == this.majorVersion) && (vi.minorVersion == this.minorVersion) && (vi.revision == this.revision) && (vi.platform.equals(this.platform)) && (vi.build == this.build) && (vi.releaseString.equals(this.releaseString)) && (vi.type.equals(this.type))) {
           return true;
         }
       }
       return false;
     }
     
     public int hashCode()
     {
       return this.majorVersion ^ this.minorVersion ^ this.revision ^ this.platform.hashCode() ^ this.build ^ this.releaseString.hashCode() ^ this.type.hashCode();
     }
     
     public int getMajorVersion()
     {
       return this.majorVersion;
     }
     
     public int getMinorVersion()
     {
       return this.minorVersion;
     }
     
     public int getRevision()
     {
       return this.revision;
     }
     
     public String getPlatform()
     {
       return this.platform;
     }
     
     public int getBuild()
     {
       return this.build;
     }
     
     public String getReleaseString()
     {
       return this.releaseString;
     }
     
     public IIapi.InstallType getInstallType()
     {
       return this.type;
     }
     
     public boolean isVW()
     {
       return this.type.equals(IIapi.InstallType.VECTORWISE);
     }
     
     public String toString()
     {
       return String.format("%1$s %2$s", new Object[] { this.type.getPrefix(), this.releaseString });
     }
   }
   
   /**
    * 实例系统信息
    * @author zxbing
    *
    */
   public static class SysInfo implements Serializable
   {
     private static final long serialVersionUID = 7713691575056692996L;
     public String IIinstallation = null;
     public String IIsystem = null;
     public String hostName = null;
     public String fullyQualifiedHostName = null;
     public IIapi.VersionInfo version = null;
     public String osName = null;
     public String osVersion = null;
     
     public boolean equals(Object o)
     {
       if ((o != null) && ((o instanceof SysInfo)))
       {
         SysInfo si = (SysInfo)o;
         if ((si.IIinstallation.equals(this.IIinstallation)) && (si.IIsystem.equals(this.IIsystem)) && (si.hostName.equals(this.hostName)) && (((si.fullyQualifiedHostName == null) && (this.fullyQualifiedHostName == null)) || ((si.fullyQualifiedHostName != null) && (this.fullyQualifiedHostName != null) && (si.fullyQualifiedHostName.equals(this.fullyQualifiedHostName))))) {
           return true;
         }
       }
       return false;
     }
     
     public int hashCode()
     {
       return this.IIinstallation.hashCode() ^ this.IIsystem.hashCode() ^ this.hostName.hashCode() ^ (this.fullyQualifiedHostName == null ? 0 : this.fullyQualifiedHostName.hashCode());
     }
     
     public String toString()
     {
       return String.format("%1$s (%2$s %3$s - %4$s)", new Object[] { this.hostName, this.version.getInstallType().getDisplayName(), this.IIinstallation, this.version.getReleaseString() });
     }
   }
   
   public static enum Privileges
   {
     SERVER_CONTROL,  NET_ADMIN,  MONITOR,  TRUSTED;
     
     private Privileges() {}
     
     public static EnumSet<Privileges> getPrivilegesFromCsvString(String str)
     {
       EnumSet<Privileges> result = EnumSet.noneOf(Privileges.class);
       if ((str != null) && (!str.isEmpty()))
       {
         String[] parts = str.split("\\,\\s*");
         for (String part : parts)
         {
           Privileges privilege = valueOf(part);
           if (privilege != null) {
             result.add(privilege);
           }
         }
       }
       return result;
     }
   }
   
   public static class Descriptor implements Serializable
   {
     private static final long serialVersionUID = 7179248204709235756L;
     public short dataType = 0;
     public boolean nullable = false;
     public short length = 0;
     public short precision = 0;
     public short scale = 0;
     public short columnType = 0;
     public String columnName = null;
     
     public Descriptor() {}
     
     public Descriptor(short dataType, boolean nullable, int length, int precision, int scale, short columnType, String columnName)
     {
       this.dataType = dataType;
       this.nullable = nullable;
       this.length = ((short)length);
       this.precision = ((short)precision);
       this.scale = ((short)scale);
       this.columnType = columnType;
       this.columnName = columnName;
     }
     
     public Descriptor(short dataType, boolean nullable, int length, short columnType, String columnName)
     {
       this.dataType = dataType;
       this.nullable = nullable;
       this.length = ((short)length);
       this.columnType = columnType;
       this.columnName = columnName;
     }
   }
   
   public static class DataValue implements Serializable
   {
     private static final long serialVersionUID = 1214345327429590516L;
     public boolean isnull = true;
     public int length = 0;
     public Object value = null;
     
     public DataValue() {}
     
     public DataValue(Object value)
     {
       this.value = value;
     }
     
     private void readObject(ObjectInputStream stream)
       throws IOException, ClassNotFoundException
     {
       this.isnull = stream.readBoolean();
       this.length = stream.readInt();
       this.value = stream.readObject();
       Logging.Debug("IIapi.DataValue.readObject => isnull %1$b, length %2$d, value '%3$s'", new Object[] { Boolean.valueOf(this.isnull), Integer.valueOf(this.length), this.value });
     }
     
     private void writeObject(ObjectOutputStream stream)
       throws IOException
     {
       Logging.Debug("IIapi.DataValue.writeObject => isnull %1$b, length %2$d, value '%3$s'", new Object[] { Boolean.valueOf(this.isnull), Integer.valueOf(this.length), this.value });
       
       stream.writeBoolean(this.isnull);
       stream.writeInt(this.length);
       stream.writeObject(this.value);
     }
   }
   
   public static class QueryInfo implements Serializable
   {
     private static final long serialVersionUID = -410571709867464044L;
     public long flags = 0L;
     public long mask = 0L;
     public long rowCount = 0L;
     public boolean readOnly = false;
     public long procReturn = 0L;
     public long procHandle = 0L;
     public long repeatQueryHandle = 0L;
     public String tableKey = null;
     public String objectKey = null;
     public long cursorType = 0L;
     public long rowStatus = 0L;
     public long rowPosition = 0L;
   }
   
   public static class Exception extends RuntimeException implements Serializable
   {
     private static final long serialVersionUID = -7153377757102186382L;
     private int type;
     private int code;
     private String source;
     private Exception next;
     
     public Exception(String message)
     {
       super();
     }
     
     public Exception(int type, int code, String source, String message, Exception next)
     {
       super();
       this.type = type;
       this.code = code;
       this.source = source;
       this.next = next;
     }
     
     public int getType()
     {
       return this.type;
     }
     
     public int getCode()
     {
       return this.code;
     }
     
     public String getSource()
     {
       return this.source;
     }
     
     public Exception getNextException()
     {
       return this.next;
     }
     
     public void setNextException(Exception next)
     {
       this.next = next;
     }
     
     public String toString()
     {
       return Intl.formatString(IIapi.pkg, "iiapi.exception", new Object[] { Integer.valueOf(this.type), Integer.valueOf(this.code), this.source, getMessage() });
     }
   }
 }
