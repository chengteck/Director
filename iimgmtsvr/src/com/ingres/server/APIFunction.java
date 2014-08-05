 package com.ingres.server;
 
 import com.ingres.AlterOptions;
import com.ingres.AuditOptions;
import com.ingres.BackupOptions;
import com.ingres.ConnectOptions;
import com.ingres.ConnectionHandle;
import com.ingres.ContextHandle;
import com.ingres.CopyOptions;
import com.ingres.CreateDBOptions;
import com.ingres.DestroyOptions;
import com.ingres.ExtendOptions;
import com.ingres.FileHandle;
import com.ingres.IIapi;
import com.ingres.IIapi.DataValue;
import com.ingres.IIapi.Descriptor;
import com.ingres.IIapi.Exception;
import com.ingres.IIapi.Privileges;
import com.ingres.IIapi.QueryInfo;
import com.ingres.IIapi.SysInfo;
import com.ingres.IIapi.VersionInfo;
import com.ingres.InfoOptions;
import com.ingres.OptimizeOptions;
import com.ingres.RestoreOptions;
import com.ingres.SessionAuth;
import com.ingres.SessionAuth.AuthType;
import com.ingres.StartOptions;
import com.ingres.StatdumpOptions;
import com.ingres.StatementHandle;
import com.ingres.StopOptions;
import com.ingres.SysmodOptions;
import com.ingres.TransactionHandle;
import com.ingres.UnloadOptions;
import com.ingres.UsermodOptions;
import com.ingres.UtilityProcessHandle;
import com.ingres.VWInfoOptions;
import com.ingres.VWLoadOptions;
import com.ingres.VerifyOptions;
import com.ingres.discovery.Discovery.Info;
import com.ingres.exception.PrivilegeException;
import com.ingres.util.Intl;
import com.ingres.util.Logging;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
 
public enum APIFunction
{
 ABORT(1, (short)0, "abort", new Parameter(Void.TYPE), new Parameter[] { new Parameter(0, ConnectionHandle.class, "connHandle", Parameter.IO.INOUT) }),  AUTO_COMMIT_ON(2, (short)0, "autoCommitOn", new Parameter(TransactionHandle.class), new Parameter[] { new Parameter(0, ConnectionHandle.class, "connHandle") }),  AUTO_COMMIT_OFF(3, (short)0, "autoCommitOff", new Parameter(Void.TYPE), new Parameter[] { new Parameter(0, TransactionHandle.class, "tranHandle", Parameter.IO.INOUT) }),  COMMIT(4, (short)0, "commit", new Parameter(Void.TYPE), new Parameter[] { new Parameter(0, TransactionHandle.class, "tranHandle", Parameter.IO.INOUT) }),  ROLLBACK(5, (short)0, "rollback", new Parameter(Void.TYPE), new Parameter[] { new Parameter(0, TransactionHandle.class, "tranHandle", Parameter.IO.INOUT) }),  CONNECT_NAME_SERVER(6, (short)1, "connectNameServer", new Parameter(ConnectionHandle.class), new Parameter[] { new Parameter(0, String.class, "target"), new Parameter(1, String.class, "user"), new Parameter(2, Parameter.BYTE_ARRAY_CLASS, "password"), new Parameter(3, Integer.class, "iTimeout"), new Parameter(4, String.class, "effectiveUser") }),  CONNECT_DATABASE(7, (short)1, "connectDatabase", new Parameter(ConnectionHandle.class), new Parameter[] { new Parameter(0, String.class, "target"), new Parameter(1, String.class, "user"), new Parameter(2, Parameter.BYTE_ARRAY_CLASS, "password"), new Parameter(3, Integer.class, "iTimeout"), new Parameter(4, ConnectOptions.class, "options") }),  DISCONNECT(8, (short)0, "disconnect", new Parameter(Void.TYPE), new Parameter[] { new Parameter(0, ConnectionHandle.class, "connHandle", Parameter.IO.INOUT) }),  EXECUTE_PROCEDURE(10, (short)0, "executeProcedure", new Parameter(StatementHandle.class), new Parameter[] { new Parameter(0, ConnectionHandle.class, "connHandle"), new Parameter(1, TransactionHandle.class, "tranHandle", Parameter.IO.INOUT), new Parameter(2, Parameter.DESC_ARRAY_CLASS, "descriptors"), new Parameter(3, Parameter.DATA_ARRAY_CLASS, "datavalues", Parameter.IO.INPUT) }),  PUT_PARAMETERS(11, (short)0, "putParameters", new Parameter(Void.TYPE), new Parameter[] { new Parameter(0, StatementHandle.class, "stmtHandle"), new Parameter(1, Parameter.DESC_ARRAY_CLASS, "descriptors"), new Parameter(2, Parameter.DATA_ARRAY_CLASS, "datavalues") }),  CANCEL(12, (short)0, "cancel", new Parameter(Void.TYPE), new Parameter[] { new Parameter(0, StatementHandle.class, "stmtHandle") }),  CLOSE(13, (short)0, "close", new Parameter(Void.TYPE), new Parameter[] { new Parameter(0, StatementHandle.class, "stmtHandle", Parameter.IO.INOUT) }),  GET_DESCRIPTORS(14, (short)0, "getDescriptors", new Parameter(Parameter.DESC_ARRAY_CLASS), new Parameter[] { new Parameter(0, StatementHandle.class, "stmtHandle") }),  GET_COLUMNS(15, (short)0, "getColumns", new Parameter(Boolean.class), new Parameter[] { new Parameter(0, StatementHandle.class, "stmtHandle"), new Parameter(1, Parameter.DESC_ARRAY_CLASS, "desc"), new Parameter(2, Parameter.DATA_ARRAY_CLASS, "data", Parameter.IO.OUTPUT) }),  GET_QUERYINFO(16, (short)0, "getQueryInfo", new Parameter(IIapi.QueryInfo.class), new Parameter[] { new Parameter(0, StatementHandle.class, "stmtHandle") }),  EXECUTE_STATEMENT(17, (short)0, "executeStatement", new Parameter(IIapi.QueryInfo.class), new Parameter[] { new Parameter(0, ConnectionHandle.class, "connHandle"), new Parameter(1, TransactionHandle.class, "tranHandle", Parameter.IO.INOUT), new Parameter(2, String.class, "statement"), new Parameter(3, Parameter.DESC_ARRAY_CLASS, "descriptors"), new Parameter(4, Parameter.DATA_ARRAY_CLASS, "datavalues") }),  EXECUTE_QUERY(18, (short)0, "executeQuery", new Parameter(Parameter.DESC_ARRAY_CLASS), new Parameter[] { new Parameter(0, ConnectionHandle.class, "connHandle"), new Parameter(1, TransactionHandle.class, "tranHandle", Parameter.IO.INOUT), new Parameter(2, StatementHandle.class, "stmtHandle", Parameter.IO.OUTPUT), new Parameter(3, String.class, "query"), new Parameter(4, Parameter.DESC_ARRAY_CLASS, "descriptors"), new Parameter(5, Parameter.DATA_ARRAY_CLASS, "datavalues") }),  END_QUERY(19, (short)0, "endQuery", new Parameter(IIapi.QueryInfo.class), new Parameter[] { new Parameter(0, StatementHandle.class, "stmtHandle") }),  GET_ROWS(20, (short)0, "getRows", new Parameter(Integer.class), new Parameter[] { new Parameter(0, StatementHandle.class, "stmtHandle"), new Parameter(1, Parameter.DESC_ARRAY_CLASS, "descriptors"), new Parameter(2, Parameter.DATA_2D_ARRAY_CLASS, "rows", Parameter.IO.OUTPUT), new Parameter(3, Boolean.class, "close") }),  GET_DECODING_ERROR(21, (short)0, "getDecodingError", new Parameter(IIapi.Exception.class), new Parameter[0]),  RESET_DECODING_ERROR(22, (short)0, "resetDecodingError", new Parameter(Void.TYPE), new Parameter[0]),  SET_DECODING_REPLACEMENT(23, (short)0, "setDecodingReplacement", new Parameter(Void.TYPE), new Parameter[] { new Parameter(0, String.class, "replace") }),  GET_ENV(24, (short)0, "getEnv", new Parameter(String.class), new Parameter[] { new Parameter(0, String.class, "symbol") }),  NM_SYMBOLS(26, (short)0, "NMsymbols", new Parameter(Map.class), new Parameter[0]),  ID_NAME(27, (short)0, "IDname", new Parameter(String.class), new Parameter[0]),  ID_NAME_SERVICE(28, (short)0, "IDname_service", new Parameter(String.class), new Parameter[0]),  GET_VERSION_INFO(29, (short)0, "getVersionInfo", new Parameter(IIapi.VersionInfo.class), new Parameter[0]),  GET_VERSION_STRING(30, (short)0, "getVersionString", new Parameter(String.class), new Parameter[0]),  GET_API_VERSION_INFO(31, (short)0, "getAPIVersionInfo", new Parameter(IIapi.VersionInfo.class), new Parameter[0]),  GET_API_VERSION_STRING(32, (short)0, "getAPIVersionString", new Parameter(String.class), new Parameter[0]),  GET_SYS_INFO(33, (short)0, "getSysInfo", new Parameter(IIapi.SysInfo.class), new Parameter[0]),  GET_AUTH_TYPE(34, (short)0, "getAuthType", new Parameter(SessionAuth.AuthType.class), new Parameter[0]),  IS_ELEVATION_REQUIRED(48, (short)0, "isElevationRequired", new Parameter(Boolean.class), new Parameter[0]),  CHECK_PRIVILEGE(49, (short)0, "checkPrivilege", new Parameter(Boolean.class), new Parameter[] { new Parameter(0, String.class, "user"), new Parameter(1, IIapi.Privileges.class, "priv") }),  TCP_IP_PORT(50, (short)0, "GCtcpIpPort", new Parameter(Integer.class), new Parameter[] { new Parameter(0, String.class, "input"), new Parameter(1, Integer.class, "subport") }),  GET_CHARSET_NAME(51, (short)0, "CMgetCharsetName", new Parameter(String.class), new Parameter[0]),  GET_STD_CHARSET_NAME(52, (short)0, "CMgetStdCharsetName", new Parameter(String.class), new Parameter[0]),  HOST_NAME(53, (short)0, "GChostname", new Parameter(String.class), new Parameter[] { new Parameter(0, Boolean.class, "fullyQualified") }),  LG_GET_LOG_PATHS(54, (short)0, "LGgetLogPaths", new Parameter(Parameter.STRING_ARRAY_CLASS), new Parameter[] { new Parameter(0, Integer.class, "whichLog"), new Parameter(1, String.class, "nodename") }),  IS_LOG_CONFIGURED(55, (short)0, "isTransactionLogConfigured", new Parameter(Boolean.class), new Parameter[] { new Parameter(0, Integer.class, "whichLog"), new Parameter(1, String.class, "nodename") }),  FS_OPEN_FILE(56, (short)0, "FSopenFile", new Parameter(FileHandle.class), new Parameter[] { new Parameter(0, String.class, "name") }),  FS_READ_FILE(57, (short)0, "FSreadFile", new Parameter(Integer.class), new Parameter[] { new Parameter(0, FileHandle.class, "handle"), new Parameter(1, Long.class, "offset"), new Parameter(2, Parameter.BYTE_ARRAY_CLASS, "buffer", Parameter.IO.OUTPUT) }),  FS_CLOSE_FILE(58, (short)0, "FScloseFile", new Parameter(Void.TYPE), new Parameter[] { new Parameter(0, FileHandle.class, "handle") }),  PM_INIT(64, (short)0, "PMinit", new Parameter(ContextHandle.class), new Parameter[0]),  PM_DELETE(65, (short)0, "PMdelete", new Parameter(Void.TYPE), new Parameter[] { new Parameter(0, ContextHandle.class, "ctxHandle"), new Parameter(1, String.class, "pmkey") }),  PM_EXP_TO_REG_EXP(66, (short)0, "PMexpToRegExp", new Parameter(String.class), new Parameter[] { new Parameter(0, ContextHandle.class, "ctxHandle"), new Parameter(1, String.class, "exp") }),  PM_FREE(67, (short)0, "PMfree", new Parameter(Void.TYPE), new Parameter[] { new Parameter(0, ContextHandle.class, "ctxHandle", Parameter.IO.INOUT) }),  PM_GET(68, (short)0, "PMget", new Parameter(String.class), new Parameter[] { new Parameter(0, ContextHandle.class, "ctxHandle"), new Parameter(1, String.class, "pmkey") }),  PM_GET_DEFAULT(69, (short)0, "PMgetDefault", new Parameter(String.class), new Parameter[] { new Parameter(0, ContextHandle.class, "ctxHandle"), new Parameter(1, Integer.class, "pmkey") }),  PM_HOST(70, (short)0, "PMhost", new Parameter(String.class), new Parameter[] { new Parameter(0, ContextHandle.class, "ctxHandle") }),  PM_INSERT(71, (short)0, "PMinsert", new Parameter(Void.TYPE), new Parameter[] { new Parameter(0, ContextHandle.class, "ctxHandle"), new Parameter(1, String.class, "pmkey"), new Parameter(2, String.class, "value") }),  PM_LOAD(72, (short)0, "PMload", new Parameter(Void.TYPE), new Parameter[] { new Parameter(0, ContextHandle.class, "ctxHandle"), new Parameter(1, String.class, "path") }),  PM_NUM_ELEM(73, (short)0, "PMnumElem", new Parameter(Integer.class), new Parameter[] { new Parameter(0, ContextHandle.class, "ctxHandle"), new Parameter(1, String.class, "pmkey") }),  PM_RESTRICT(74, (short)0, "PMrestrict", new Parameter(Void.TYPE), new Parameter[] { new Parameter(0, ContextHandle.class, "ctxHandle"), new Parameter(1, String.class, "value") }),  PM_SCAN(75, (short)0, "PMscan", new Parameter(Map.class), new Parameter[] { new Parameter(0, ContextHandle.class, "ctxHandle"), new Parameter(1, String.class, "regexp") }),  PM_SET_DEFAULT(76, (short)0, "PMsetDefault", new Parameter(Void.TYPE), new Parameter[] { new Parameter(0, ContextHandle.class, "ctxHandle"), new Parameter(1, Integer.class, "idx"), new Parameter(2, String.class, "value") }),  PM_WRITE(77, (short)0, "PMwrite", new Parameter(Void.TYPE), new Parameter[] { new Parameter(0, ContextHandle.class, "ctxHandle"), new Parameter(1, String.class, "path") }),  PM_LOWER_ON(78, (short)0, "PMlowerOn", new Parameter(Void.TYPE), new Parameter[] { new Parameter(0, ContextHandle.class, "ctxHandle") }),  CR_SET_PMVAL(79, (short)0, "CRsetPMval", new Parameter(Void.TYPE), new Parameter[] { new Parameter(0, ContextHandle.class, "ctxHandle"), new Parameter(1, String.class, "key"), new Parameter(2, String.class, "value") }),  UT_CREATE_DATABASE(80, (short)0, "UTcreateDatabase", new Parameter(UtilityProcessHandle.class), new Parameter[] { new Parameter(0, String.class, "name"), new Parameter(1, CreateDBOptions.class, "options") }),  UT_DESTROY_DATABASE(81, (short)0, "UTdestroyDatabase", new Parameter(UtilityProcessHandle.class), new Parameter[] { new Parameter(0, String.class, "name"), new Parameter(1, DestroyOptions.class, "options") }),  UT_START_INGRES(82, (short)0, "UTstartIngres", new Parameter(UtilityProcessHandle.class), new Parameter[] { new Parameter(0, StartOptions.class, "options") }),  UT_STOP_INGRES(83, (short)0, "UTstopIngres", new Parameter(UtilityProcessHandle.class), new Parameter[] { new Parameter(0, StopOptions.class, "options") }),  GET_FILE_LIST(96, (short)0, "FSgetFileList", new Parameter(Parameter.STRING_ARRAY_CLASS), new Parameter[] { new Parameter(0, String.class, "dir"), new Parameter(1, String.class, "name"), new Parameter(2, String.class, "ext") }),  FS_GET_NON_UNICODE_COLLATIONS(97, (short)0, "FSgetNonUnicodeCollations", new Parameter(Parameter.STRING_ARRAY_CLASS), new Parameter[0]),  FS_GET_UNICODE_COLLATIONS(98, (short)0, "FSgetUnicodeCollations", new Parameter(Parameter.STRING_ARRAY_CLASS), new Parameter[0]),  FS_GET_LOG_FILES(99, (short)0, "FSgetLogFiles", new Parameter(Parameter.STRING_ARRAY_CLASS), new Parameter[0]),  UT_WRITE_PROCESS_INPUT(114, (short)0, "UTwriteProcessInput", new Parameter(Void.TYPE), new Parameter[] { new Parameter(0, UtilityProcessHandle.class, "processHandle"), new Parameter(1, String.class, "text") }),  UT_READ_PROCESS_OUTPUT(115, (short)0, "UTreadProcessOutput", new Parameter(String.class), new Parameter[] { new Parameter(0, UtilityProcessHandle.class, "processHandle"), new Parameter(1, Integer.class, "maxLength"), new Parameter(2, Boolean.class, "blocking") }),  UT_GET_PROCESS_EXIT_VALUE(116, (short)0, "UTgetProcessExitValue", new Parameter(Integer.class), new Parameter[] { new Parameter(0, UtilityProcessHandle.class, "processHandle") }),  UT_WAIT_FOR_PROCESS_EXIT_VALUE(117, (short)0, "UTwaitForProcessExitValue", new Parameter(Integer.class), new Parameter[] { new Parameter(0, UtilityProcessHandle.class, "processHandle") }),  UT_RELEASE_PROCESS_HANDLE(118, (short)0, "UTreleaseProcessHandle", new Parameter(Void.TYPE), new Parameter[] { new Parameter(0, UtilityProcessHandle.class, "processHandle") }),  UT_BACKUP_DATABASE(119, (short)0, "UTbackupDatabase", new Parameter(UtilityProcessHandle.class), new Parameter[] { new Parameter(0, String.class, "name"), new Parameter(1, BackupOptions.class, "options") }),  UT_RESTORE_DATABASE(120, (short)0, "UTrestoreDatabase", new Parameter(UtilityProcessHandle.class), new Parameter[] { new Parameter(0, String.class, "name"), new Parameter(1, RestoreOptions.class, "options") }),  UT_OPTIMIZE_DATABASE(121, (short)0, "UToptimizeDatabase", new Parameter(UtilityProcessHandle.class), new Parameter[] { new Parameter(0, String.class, "name"), new Parameter(1, OptimizeOptions.class, "options") }),  UT_SYSMOD_DATABASE(122, (short)0, "UTsysmodDatabase", new Parameter(UtilityProcessHandle.class), new Parameter[] { new Parameter(0, String.class, "name"), new Parameter(1, SysmodOptions.class, "options") }),  UT_USERMOD_DATABASE(123, (short)0, "UTusermodDatabase", new Parameter(UtilityProcessHandle.class), new Parameter[] { new Parameter(0, String.class, "name"), new Parameter(1, UsermodOptions.class, "options") }),  UT_VWLOAD(124, (short)1, "UTvwLoad", new Parameter(UtilityProcessHandle.class), new Parameter[] { new Parameter(0, String.class, "name"), new Parameter(1, VWLoadOptions.class, "options") }),  UT_ALTER_DATABASE(125, (short)0, "UTalterDatabase", new Parameter(UtilityProcessHandle.class), new Parameter[] { new Parameter(0, String.class, "name"), new Parameter(1, AlterOptions.class, "options") }),  UT_EXTEND_DATABASE(126, (short)0, "UTextendDatabase", new Parameter(UtilityProcessHandle.class), new Parameter[] { new Parameter(0, String.class, "name"), new Parameter(1, ExtendOptions.class, "options") }),  UT_VERIFY_DATABASE(127, (short)0, "UTverifyDatabase", new Parameter(UtilityProcessHandle.class), new Parameter[] { new Parameter(0, String.class, "name"), new Parameter(1, VerifyOptions.class, "options") }),  UT_STATDUMP_DATABASE(128, (short)0, "UTstatdumpDatabase", new Parameter(UtilityProcessHandle.class), new Parameter[] { new Parameter(0, String.class, "name"), new Parameter(1, StatdumpOptions.class, "options") }),  UT_COPY_DATABASE(129, (short)0, "UTcopyDatabase", new Parameter(UtilityProcessHandle.class), new Parameter[] { new Parameter(0, String.class, "name"), new Parameter(1, CopyOptions.class, "options") }),  UT_UNLOAD_DATABASE(130, (short)0, "UTunloadDatabase", new Parameter(UtilityProcessHandle.class), new Parameter[] { new Parameter(0, String.class, "name"), new Parameter(1, UnloadOptions.class, "options") }),  UT_AUDIT_DATABASE(131, (short)0, "UTauditDatabase", new Parameter(UtilityProcessHandle.class), new Parameter[] { new Parameter(0, String.class, "name"), new Parameter(1, AuditOptions.class, "options") }),  UT_INFO_DATABASE(132, (short)0, "UTinfoDatabase", new Parameter(UtilityProcessHandle.class), new Parameter[] { new Parameter(0, String.class, "name"), new Parameter(1, InfoOptions.class, "options") }),  UT_VWINFO_DATABASE(133, (short)1, "UTvwinfoDatabase", new Parameter(UtilityProcessHandle.class), new Parameter[] { new Parameter(0, String.class, "name"), new Parameter(1, VWInfoOptions.class, "options") }),  ID_NAME_SERVER(134, (short)0, "IDnameServer", new Parameter(String.class), new Parameter[0]),  IS_CLIENT_FULLY_AUTHORIZED(135, (short)0, "isClientFullyAuthorized", new Parameter(Boolean.class), new Parameter[0]),  UT_KILL_PROCESS(136, (short)0, "UTkillProcess", new Parameter(Void.TYPE), new Parameter[] { new Parameter(0, UtilityProcessHandle.class, "processHandle") }),  TERMINATE_SERVER(34661, (short)1, "TerminateServer", new Parameter(Integer.class), new Parameter[] { new Parameter(0, String.class, "sUser"), new Parameter(1, Integer.class, "iFlags") });
 
 private final int id; //命令id
 private final short version; //命令版本
 private final String name; //命令名称
 private final Parameter formalReturn;
 private final Parameter[] formalParameters;
 private static Logging logger;
 private static HashMap<Integer, APIFunction> fcns;
 
 /**
  * 
  * @param id      表示命令id
  * @param version
  * @param name
  * @param ret
  * @param params
  */
 private APIFunction(int id, short version, String name, Parameter ret, Parameter... params)
 {
   this.id = id;
   this.version = version;
   this.name = name;
   this.formalReturn = ret;
   this.formalParameters = params;
 }
 
 public String displayName()
 {
   return this.name;
 }
 
 public Parameter[] getFormalParameters()
 {
   return this.formalParameters;
 }
 
 /**
  * todo　这里的具体实现在哪里？
  * @param paramIIapi
  * @param paramArrayOfParameter
  * @return
  */
 public  Parameter invoke(IIapi paramIIapi, Parameter[] paramArrayOfParameter){
	 
	 return null;
 }
 
   public Parameter call(RemoteConnection dest, Parameter... actualParams) 
   {
     Parameter ret = null;
     RuntimeException ex = null;
     try
     {
       dest.lock();
       try
       {
         if (dest.getInstallationID() == 0)
         {
           logger.debug("Trying to reconnect after function failure...", new Object[0]);
           dest.reconnect();
         }
         logger.debug("%1$s.call(...)", new Object[] { this.name });
         if (actualParams.length != this.formalParameters.length) {
           throw new ParameterException("Length", Intl.formatString(RemoteCommand.pkg, "function.paramLengthMismatch", new Object[] { Integer.valueOf(actualParams.length), Integer.valueOf(this.formalParameters.length), this.name }));
         }
         ObjectInputStream is = dest.getInputStream();
         ObjectOutputStream os = dest.getOutputStream();
         
         int protoLvl = dest.getProtocolLevel();
         
         os.reset();
         os.write(RemoteCommand.SIGNATURE);
         os.writeInt(7); //代表func操作
         os.writeInt(dest.getInstallationID());
         os.writeInt(this.id); //funcID
         os.writeShort(this.version); //funcVersion
         os.writeInt(actualParams.length); //func 参数
         for (int i = 0; i < actualParams.length; i++)
         {
           Parameter actual = actualParams[i];
           if (!this.formalParameters[i].check(actual)) {
             throw new ParameterException(this.formalParameters[i].toString(), Intl.formatString(RemoteCommand.pkg, "function.parameterMismatch", new Object[] { actual, this.formalParameters[i] }));
           }
           actual.write(os, Parameter.IO.INPUT, protoLvl);
         }
         os.flush(); //这里会block么？
         
         byte[] sig = new byte[4];
         is.readFully(sig);
         if (!Arrays.equals(sig, RemoteCommand.SIGNATURE))
         {
           logger.warn("Received invalid response signature %1$s", new Object[] { Arrays.toString(sig) });
           ex = new IIapi.Exception(Intl.getString(RemoteCommand.pkg, "function.invalidResponseSig"));
         }
         else
         {
           int exceptCount = 0;
           int rqstID = is.readInt();
           switch (rqstID)
           {
           case 0: 
             exceptCount = is.readInt();
             if (exceptCount == 0) 
             {
               ex = new IIapi.Exception(Intl.getString(RemoteCommand.pkg, "function.unexpectedClose"));
             } 
             else 
             {
               ex = (RuntimeException)is.readObject();
             }
             logger.warn("Exception: %1$s", new Object[] { ex.toString() });
             break;
           case 8: 
        	 //对应7 funcResponse
             exceptCount = is.readInt();
             logger.debug("Exception count = %1$d", new Object[] { Integer.valueOf(exceptCount) });
             if (exceptCount != 0)
             {
               ex = (RuntimeException)is.readObject();
               logger.debug("Exception: %1$s", new Object[] { ex.toString() });
               if (protoLvl > 1) {
                 for (Parameter p : actualParams) {
                   if (p.dir == Parameter.IO.INOUT) {
                     p.read(is, Parameter.IO.OUTPUT, protoLvl);
                   } else if ((protoLvl > 2) && 
                     (p.dir == Parameter.IO.OUTPUT)) {
                     p.read(is, Parameter.IO.OUTPUT, protoLvl);
                   }
                 }
               }
             }
             else
             {
               logger.debug("Reading the OUTPUT and INOUT parameters...", new Object[0]);
               for (Parameter p : actualParams) {
                 p.read(is, Parameter.IO.OUTPUT, protoLvl);
               }
               logger.debug("Reading the return value...", new Object[0]);
               ret = this.formalReturn.clone();
               ret.read(is, Parameter.IO.OUTPUT, protoLvl);
             }
             logger.debug("Done with %1$s.call(...)", new Object[] { this.name });
             break;
           default: 
             ex = new IIapi.Exception(Intl.getString(RemoteCommand.pkg, "function.unknownResponseID"));
             logger.warn("Exception: %1$s", new Object[] { ex.toString() });
           }
         }
       } catch (ClassNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (ParameterException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (InstantiationException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IllegalAccessException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
       finally
       {
         dest.unlock();
       }
     }
     catch (SocketException se)
     {
       try
       {
         for (;;)
         {
           logger.debug("Trying to reset connection to '%1$s' during '%2$s' function call...", new Object[] { dest.getDiscoveryInfo().toShortName(), this.name });
           if (!dest.reset()) {
             break;
           }
         }
       }
       catch (Exception ex2)
       {
         logger.except("During reconnect", ex2);
       } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
       logger.debug("Connection reset didn't work, so throwing the original exception.", new Object[0]);
       throw new ParameterException(Intl.formatString(RemoteCommand.pkg, "function.socketError", new Object[] { this.name, dest.getDiscoveryInfo().toShortName() }), se);
     }
     catch (IOException ioe)
     {
       try
       {
         for (;;)
         {
           logger.debug("Trying to reset connection to '%1$s' during '%2$s' function call...", new Object[] { dest.getDiscoveryInfo().toShortName(), this.name });
           if (!dest.reset()) {
             break;
           }
         }
       }
       catch (Exception ex2)
       {
         logger.except("During reconnect", ex2);
       } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
       logger.debug("Connection reset didn't work, so throwing the original exception.", new Object[0]);
       throw new ParameterException(Intl.formatString(RemoteCommand.pkg, "function.socketError", new Object[] { this.name, dest.getDiscoveryInfo().toShortName() }), ioe);
     }
     catch (Exception e)
     {
       String msg = e.getMessage();
       if ((msg == null) || (msg.isEmpty())) {
         msg = e.getClass().getSimpleName();
       }
       throw new ParameterException(Intl.formatString(RemoteCommand.pkg, "function.otherError", new Object[] { this.name, msg }), e);
     }
     if (ex != null)
     {
       if ((ex instanceof IllegalArgumentException)) {
         try
         {
           dest.disconnect();
         }
         catch (Exception ignore) {
        	 
         } 
         catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
       }
       throw ex;
     }
     return ret;
   }
   
   public static APIFunction getFunction(int id, short version)
     throws IllegalArgumentException
   {
     Integer k = Integer.valueOf(id);
     if (fcns.containsKey(k))
     {
       APIFunction fcn = (APIFunction)fcns.get(k);
       if (fcn.version == version) {
         return fcn;
       }
       if (fcn.version < version) {
         throw new IllegalArgumentException(Intl.formatString(RemoteCommand.pkg, "function.upgradeServerFunction", new Object[] { fcn.name }));
       }
       throw new IllegalArgumentException(Intl.formatString(RemoteCommand.pkg, "function.upgradeClientFunction", new Object[] { fcn.name }));
     }
     throw new IllegalArgumentException(Intl.formatString(RemoteCommand.pkg, "function.functionNotFound", new Object[] { Integer.valueOf(id) }));
   }
   
   static
   {
	 //构造功能函数字典　k=f.id value=f
     fcns = new HashMap<Integer, APIFunction>();
     
     logger = new Logging("APIFunction");
     for (APIFunction f : values()) //values()函数返回枚举所有值
     {
       APIFunction p = (APIFunction)fcns.put(Integer.valueOf(f.id), f);
       if (p != null) {
         throw new IllegalArgumentException(Intl.formatString(RemoteCommand.pkg, "function.duplicateID", new Object[] { Integer.valueOf(f.id), f.name }));
       }
     }
   }
 }



