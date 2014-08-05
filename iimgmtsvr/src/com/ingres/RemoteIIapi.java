package com.ingres;

import com.ingres.server.APIFunction;
import com.ingres.server.Parameter;
import com.ingres.server.Parameter.IO;
import com.ingres.server.RemoteCommand;
import com.ingres.server.RemoteConnection;
import com.ingres.util.Logging;

import java.io.IOException;
import java.io.PrintStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;

public class RemoteIIapi extends IIapiBase {
	private static Logging logger = new Logging(RemoteIIapi.class);
	private ConnectionInfo ci = null;
	private RemoteConnection conn = null;

	public RemoteIIapi(ConnectionInfo ci) {
		this.ci = ci;
	}

	public boolean isRemote() {
		return true;
	}

	public ConnectionInfo getConnectionInfo() {
		return this.ci;
	}

	public RemoteConnection getRemoteConnection() {
		return this.conn;
	}

	public boolean connectToRemoteServer() throws IOException, IIapi.Exception, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
		return connectToRemoteServer(false);
	}

	public boolean connectToRemoteServer(boolean command) throws IOException,
			IIapi.Exception, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
		if (this.conn == null) {
			this.conn = new RemoteConnection(this.ci);
		}
		return this.conn.connect(command ? RemoteCommand.COMMAND_SIGNATURE
				: RemoteCommand.SIGNATURE);
	}

	public void unloadInstallation() {
		if (this.conn != null) {
			if (this.conn.isConnected()) 
			{
				//@modify zhengxb add try catch block
				try {
					this.conn.disconnect();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			this.conn = null;
		}
	}

	public int getInstallationID() {
		return this.conn.getInstallationID();
	}

	public SessionAuth.AuthType getAuthType() {
		Parameter ret = APIFunction.GET_AUTH_TYPE.call(this.conn,
				new Parameter[0]);
		return ret.getAuthTypeValue();
	}

	public void abort(ConnectionHandle connHandle) {
		APIFunction.ABORT.call(this.conn, new Parameter[] { new Parameter(0,
				ConnectionHandle.class, Parameter.IO.INOUT, connHandle) });
	}

	public TransactionHandle autoCommitOn(ConnectionHandle connHandle) {
		Parameter ret = APIFunction.AUTO_COMMIT_ON.call(this.conn,
				new Parameter[] { new Parameter(0, ConnectionHandle.class,
						Parameter.IO.INPUT, connHandle) });

		return ret.getTransactionHandleValue();
	}

	public void autoCommitOff(TransactionHandle tranHandle) {
		APIFunction.AUTO_COMMIT_OFF.call(this.conn,
				new Parameter[] { new Parameter(0, TransactionHandle.class,
						Parameter.IO.INOUT, tranHandle) });
	}

	public void commit(TransactionHandle tranHandle) {
		APIFunction.COMMIT.call(this.conn, new Parameter[] { new Parameter(0,
				TransactionHandle.class, Parameter.IO.INOUT, tranHandle) });
	}

	public void rollback(TransactionHandle tranHandle) {
		APIFunction.ROLLBACK.call(this.conn, new Parameter[] { new Parameter(0,
				TransactionHandle.class, Parameter.IO.INOUT, tranHandle) });
	}

	public ConnectionHandle connectNameServer(String target, String user,
			byte[] password, int iTimeout, String effectiveUser) {
		Parameter ret = APIFunction.CONNECT_NAME_SERVER
				.call(this.conn,
						new Parameter[] {
								new Parameter(0, String.class,
										Parameter.IO.INPUT, target),
								new Parameter(1, String.class,
										Parameter.IO.INPUT, user),
								new Parameter(2, Parameter.BYTE_ARRAY_CLASS,
										Parameter.IO.INPUT, password),
								new Parameter(3, Integer.class,
										Parameter.IO.INPUT, Integer
												.valueOf(iTimeout)),
								new Parameter(4, String.class,
										Parameter.IO.INPUT, effectiveUser) });

		return ret.getConnectionHandleValue();
	}

	public ConnectionHandle connectDatabase(String target, String user,
			byte[] password, int iTimeout, ConnectOptions options) {
		Parameter ret = APIFunction.CONNECT_DATABASE
				.call(this.conn,
						new Parameter[] {
								new Parameter(0, String.class,
										Parameter.IO.INPUT, target),
								new Parameter(1, String.class,
										Parameter.IO.INPUT, user),
								new Parameter(2, Parameter.BYTE_ARRAY_CLASS,
										Parameter.IO.INPUT, password),
								new Parameter(3, Integer.class,
										Parameter.IO.INPUT, Integer
												.valueOf(iTimeout)),
								new Parameter(4, ConnectOptions.class,
										Parameter.IO.INPUT, options) });

		return ret.getConnectionHandleValue();
	}

	public void disconnect(ConnectionHandle connHandle) {
		APIFunction.DISCONNECT.call(this.conn, new Parameter[] { new Parameter(
				0, ConnectionHandle.class, Parameter.IO.INOUT, connHandle) });
	}

	@Deprecated
	public StatementHandle query(ConnectionHandle connHandle,
			TransactionHandle tranHandle, String query, boolean bNeedParms) {
		throw new IIapi.Exception("%iiapi.invalidConn");
	}

	public IIapi.QueryInfo executeStatement(ConnectionHandle connHandle,
			TransactionHandle tranHandle, String statement,
			IIapi.Descriptor[] descriptors, IIapi.DataValue[] datavalues) {
		Parameter ret = APIFunction.EXECUTE_STATEMENT.call(this.conn,
				new Parameter[] {
						new Parameter(0, ConnectionHandle.class,
								Parameter.IO.INPUT, connHandle),
						new Parameter(1, TransactionHandle.class,
								Parameter.IO.INOUT, tranHandle),
						new Parameter(2, String.class, Parameter.IO.INPUT,
								statement),
						new Parameter(3, Parameter.DESC_ARRAY_CLASS,
								Parameter.IO.INPUT, descriptors),
						new Parameter(4, Parameter.DATA_ARRAY_CLASS,
								Parameter.IO.INPUT, datavalues) });

		return ret.getQueryInfoValue();
	}

	public IIapi.Descriptor[] executeQuery(ConnectionHandle connHandle,
			TransactionHandle tranHandle, StatementHandle stmtHandle,
			String query, IIapi.Descriptor[] descriptors,
			IIapi.DataValue[] datavalues) {
		Parameter ret = APIFunction.EXECUTE_QUERY.call(this.conn,
				new Parameter[] {
						new Parameter(0, ConnectionHandle.class,
								Parameter.IO.INPUT, connHandle),
						new Parameter(1, TransactionHandle.class,
								Parameter.IO.INOUT, tranHandle),
						new Parameter(2, StatementHandle.class,
								Parameter.IO.OUTPUT, stmtHandle),
						new Parameter(3, String.class, Parameter.IO.INPUT,
								query),
						new Parameter(4, Parameter.DESC_ARRAY_CLASS,
								Parameter.IO.INPUT, descriptors),
						new Parameter(5, Parameter.DATA_ARRAY_CLASS,
								Parameter.IO.INPUT, datavalues) });

		return ret.getDescArrayValue();
	}

	public IIapi.QueryInfo endQuery(StatementHandle stmtHandle) {
		Parameter ret = APIFunction.END_QUERY.call(this.conn,
				new Parameter[] { new Parameter(0, StatementHandle.class,
						Parameter.IO.INPUT, stmtHandle) });

		return ret.getQueryInfoValue();
	}

	public StatementHandle executeProcedure(ConnectionHandle connHandle,
			TransactionHandle tranHandle, IIapi.Descriptor[] descriptors,
			IIapi.DataValue[] datavalues) {
		Parameter ret = APIFunction.EXECUTE_PROCEDURE.call(this.conn,
				new Parameter[] {
						new Parameter(0, ConnectionHandle.class,
								Parameter.IO.INPUT, connHandle),
						new Parameter(1, TransactionHandle.class,
								Parameter.IO.INOUT, tranHandle),
						new Parameter(2, Parameter.DESC_ARRAY_CLASS,
								Parameter.IO.INPUT, descriptors),
						new Parameter(3, Parameter.DATA_ARRAY_CLASS,
								Parameter.IO.INPUT, datavalues) });

		return ret.getStatementHandleValue();
	}

	public void putParameters(StatementHandle stmtHandle,
			IIapi.Descriptor[] descriptors, IIapi.DataValue[] datavalues) {
		APIFunction.PUT_PARAMETERS.call(this.conn, new Parameter[] {
				new Parameter(0, StatementHandle.class, Parameter.IO.INPUT,
						stmtHandle),
				new Parameter(1, Parameter.DESC_ARRAY_CLASS,
						Parameter.IO.INPUT, descriptors),
				new Parameter(2, Parameter.DATA_ARRAY_CLASS,
						Parameter.IO.INPUT, datavalues) });
	}

	public void cancel(StatementHandle stmtHandle) {
		APIFunction.CANCEL.call(this.conn, new Parameter[] { new Parameter(0,
				StatementHandle.class, Parameter.IO.INPUT, stmtHandle) });
	}

	public void close(StatementHandle stmtHandle) {
		APIFunction.CLOSE.call(this.conn, new Parameter[] { new Parameter(0,
				StatementHandle.class, Parameter.IO.INOUT, stmtHandle) });
	}

	public IIapi.Descriptor[] getDescriptors(StatementHandle stmtHandle) {
		Parameter ret = APIFunction.GET_DESCRIPTORS.call(this.conn,
				new Parameter[] { new Parameter(0, StatementHandle.class,
						Parameter.IO.INPUT, stmtHandle) });

		return ret.getDescArrayValue();
	}

	public boolean getColumns(StatementHandle stmtHandle,
			IIapi.Descriptor[] desc, IIapi.DataValue[] data) {
		Parameter ret = APIFunction.GET_COLUMNS.call(this.conn,
				new Parameter[] {
						new Parameter(0, StatementHandle.class,
								Parameter.IO.INPUT, stmtHandle),
						new Parameter(1, Parameter.DESC_ARRAY_CLASS,
								Parameter.IO.INPUT, desc),
						new Parameter(2, Parameter.DATA_ARRAY_CLASS,
								Parameter.IO.OUTPUT, data) });

		return ret.getBooleanValue();
	}

	public int getRows(StatementHandle stmtHandle, IIapi.Descriptor[] desc,
			IIapi.DataValue[][] rows, boolean bClose) {
		Parameter ret = APIFunction.GET_ROWS.call(this.conn,
				new Parameter[] {
						new Parameter(0, StatementHandle.class,
								Parameter.IO.INPUT, stmtHandle),
						new Parameter(1, Parameter.DESC_ARRAY_CLASS,
								Parameter.IO.INPUT, desc),
						new Parameter(2, Parameter.DATA_2D_ARRAY_CLASS,
								Parameter.IO.OUTPUT, rows),
						new Parameter(3, Boolean.class, Parameter.IO.INPUT,
								Boolean.valueOf(bClose)) });

		return ret.getIntValue();
	}

	public IIapi.QueryInfo getQueryInfo(StatementHandle stmtHandle) {
		Parameter ret = APIFunction.GET_QUERYINFO.call(this.conn,
				new Parameter[] { new Parameter(0, StatementHandle.class,
						Parameter.IO.INPUT, stmtHandle) });

		return ret.getQueryInfoValue();
	}

	public IIapi.Exception getDecodingError() {
		Parameter ret = APIFunction.GET_DECODING_ERROR.call(this.conn,
				new Parameter[0]);
		return ret.getIIapiExceptionValue();
	}

	public void resetDecodingError() {
		APIFunction.RESET_DECODING_ERROR.call(this.conn, new Parameter[0]);
	}

	public void setDecodingReplacement(String replace) {
		APIFunction.SET_DECODING_REPLACEMENT.call(this.conn,
				new Parameter[] { new Parameter(0, String.class,
						Parameter.IO.INPUT, replace) });
	}

	public String getEnv(String symbol) {
		Parameter ret = APIFunction.GET_ENV.call(this.conn,
				new Parameter[] { new Parameter(0, String.class,
						Parameter.IO.INPUT, symbol) });

		return ret.getStringValue();
	}

	public Map<String, String> NMsymbols() {
		Parameter ret = APIFunction.NM_SYMBOLS
				.call(this.conn, new Parameter[0]);
		return ret.getMapStringStringValue();
	}

	public String IDname() {
		Parameter ret = APIFunction.ID_NAME.call(this.conn, new Parameter[0]);
		return ret.getStringValue();
	}

	public String IDnameServer() {
		Parameter ret = APIFunction.ID_NAME_SERVER.call(this.conn,
				new Parameter[0]);
		return ret.getStringValue();
	}

	public String IDname_service() {
		Parameter ret = APIFunction.ID_NAME_SERVICE.call(this.conn,
				new Parameter[0]);
		return ret.getStringValue();
	}

	public IIapi.VersionInfo getVersionInfo() {
		Parameter ret = APIFunction.GET_VERSION_INFO.call(this.conn,
				new Parameter[0]);
		return ret.getVersionInfoValue();
	}

	public String getVersionString() {
		Parameter ret = APIFunction.GET_VERSION_STRING.call(this.conn,
				new Parameter[0]);
		return ret.getStringValue();
	}

	public IIapi.VersionInfo getAPIVersionInfo() {
		Parameter ret = APIFunction.GET_API_VERSION_INFO.call(this.conn,
				new Parameter[0]);
		return ret.getVersionInfoValue();
	}

	public String getAPIVersionString() {
		Parameter ret = APIFunction.GET_API_VERSION_STRING.call(this.conn,
				new Parameter[0]);
		return ret.getStringValue();
	}

	public IIapi.SysInfo getSysInfo() {
		Parameter ret = APIFunction.GET_SYS_INFO.call(this.conn,
				new Parameter[0]);
		return ret.getSysInfoValue();
	}

	public void elevationRequired() {
		Parameter ret = APIFunction.IS_ELEVATION_REQUIRED.call(this.conn,
				new Parameter[0]);
		if (ret.getBooleanValue()) {
			System.out
					.println("Access Denied as you do not have sufficient privileges.");
			System.out
					.println("You must invoke this utility running in elevated mode.");

			System.exit(740);
		}
	}

	public void elevationRequiredWarning() {
		Parameter ret = APIFunction.IS_ELEVATION_REQUIRED.call(this.conn,
				new Parameter[0]);
		if (ret.getBooleanValue()) {
			System.out
					.println("Access Denied as you do not have sufficient privileges.");
			System.out
					.println("You must invoke this utility running in elevated mode.");
		}
	}

	public boolean isElevationRequired() {
		Parameter ret = APIFunction.IS_ELEVATION_REQUIRED.call(this.conn,
				new Parameter[0]);
		return ret.getBooleanValue();
	}

	public boolean checkPrivilege(String user, IIapi.Privileges priv) {
		Parameter ret = APIFunction.CHECK_PRIVILEGE
				.call(this.conn,
						new Parameter[] {
								new Parameter(0, String.class,
										Parameter.IO.INPUT, user),
								new Parameter(1, IIapi.Privileges.class,
										Parameter.IO.INPUT, priv) });

		return ret.getBooleanValue();
	}

	public boolean isClientFullyAuthorized() {
		Parameter ret = APIFunction.IS_CLIENT_FULLY_AUTHORIZED.call(this.conn,
				new Parameter[0]);
		return ret.getBooleanValue();
	}

	public void GCusrpwd(String user, byte[] password) throws IIapi.Exception {
		throw new IIapi.Exception(
				"Password validation not supported via remote function execution!");
	}

	public String GChostname(boolean fullyQualified) {
		Parameter ret = APIFunction.HOST_NAME.call(this.conn,
				new Parameter[] { new Parameter(0, Boolean.class,
						Parameter.IO.INPUT, Boolean.valueOf(fullyQualified)) });

		return ret.getStringValue();
	}

	public int GCtcpIpPort(String input, int subport) {
		Parameter ret = APIFunction.TCP_IP_PORT.call(this.conn,
				new Parameter[] {
						new Parameter(0, String.class, Parameter.IO.INPUT,
								input),
						new Parameter(1, Integer.class, Parameter.IO.INPUT,
								Integer.valueOf(subport)) });

		return ret.getIntValue();
	}

	public String CMgetCharsetName() {
		Parameter ret = APIFunction.GET_CHARSET_NAME.call(this.conn,
				new Parameter[0]);
		return ret.getStringValue();
	}

	public String CMgetStdCharsetName() {
		Parameter ret = APIFunction.GET_STD_CHARSET_NAME.call(this.conn,
				new Parameter[0]);
		return ret.getStringValue();
	}

	public ContextHandle PMinit() {
		Parameter ret = APIFunction.PM_INIT.call(this.conn, new Parameter[0]);
		return ret.getContextHandleValue();
	}

	public void PMdelete(ContextHandle ctxHandle, String pmkey) {
		APIFunction.PM_DELETE.call(this.conn, new Parameter[] {
				new Parameter(0, ContextHandle.class, Parameter.IO.INPUT,
						ctxHandle),
				new Parameter(1, String.class, Parameter.IO.INPUT, pmkey) });
	}

	public String PMexpToRegExp(ContextHandle ctxHandle, String exp) {
		Parameter ret = APIFunction.PM_EXP_TO_REG_EXP
				.call(this.conn,
						new Parameter[] {
								new Parameter(0, ContextHandle.class,
										Parameter.IO.INPUT, ctxHandle),
								new Parameter(1, String.class,
										Parameter.IO.INPUT, exp) });

		return ret.getStringValue();
	}

	public void PMfree(ContextHandle ctxHandle) {
		APIFunction.PM_FREE.call(this.conn, new Parameter[] { new Parameter(0,
				ContextHandle.class, Parameter.IO.INOUT, ctxHandle) });
	}

	public String PMget(ContextHandle ctxHandle, String pmkey) {
		Parameter ret = APIFunction.PM_GET.call(this.conn, new Parameter[] {
				new Parameter(0, ContextHandle.class, Parameter.IO.INPUT,
						ctxHandle),
				new Parameter(1, String.class, Parameter.IO.INPUT, pmkey) });

		return ret.getStringValue();
	}

	public String PMgetDefault(ContextHandle ctxHandle, int idx) {
		Parameter ret = APIFunction.PM_GET_DEFAULT.call(this.conn,
				new Parameter[] {
						new Parameter(0, ContextHandle.class,
								Parameter.IO.INPUT, ctxHandle),
						new Parameter(1, Integer.class, Parameter.IO.INPUT,
								Integer.valueOf(idx)) });

		return ret.getStringValue();
	}

	public String PMhost(ContextHandle ctxHandle) {
		Parameter ret = APIFunction.PM_HOST.call(this.conn,
				new Parameter[] { new Parameter(0, ContextHandle.class,
						Parameter.IO.INPUT, ctxHandle) });

		return ret.getStringValue();
	}

	public void PMinsert(ContextHandle ctxHandle, String pmkey, String value) {
		APIFunction.PM_INSERT.call(this.conn, new Parameter[] {
				new Parameter(0, ContextHandle.class, Parameter.IO.INPUT,
						ctxHandle),
				new Parameter(1, String.class, Parameter.IO.INPUT, pmkey),
				new Parameter(2, String.class, Parameter.IO.INPUT, value) });
	}

	public void PMload(ContextHandle ctxHandle, String path) {
		APIFunction.PM_LOAD.call(this.conn, new Parameter[] {
				new Parameter(0, ContextHandle.class, Parameter.IO.INPUT,
						ctxHandle),
				new Parameter(1, String.class, Parameter.IO.INPUT, path) });
	}

	public int PMnumElem(ContextHandle ctxHandle, String pmkey) {
		Parameter ret = APIFunction.PM_NUM_ELEM.call(this.conn,
				new Parameter[] {
						new Parameter(0, ContextHandle.class,
								Parameter.IO.INPUT, ctxHandle),
						new Parameter(1, String.class, Parameter.IO.INPUT,
								pmkey) });

		return ret.getIntValue();
	}

	public void PMrestrict(ContextHandle ctxHandle, String value) {
		APIFunction.PM_RESTRICT.call(this.conn, new Parameter[] {
				new Parameter(0, ContextHandle.class, Parameter.IO.INPUT,
						ctxHandle),
				new Parameter(1, String.class, Parameter.IO.INPUT, value) });
	}

	public Map<String, String> PMscan(ContextHandle ctxHandle, String regexp) {
		Parameter ret = APIFunction.PM_SCAN.call(this.conn, new Parameter[] {
				new Parameter(0, ContextHandle.class, Parameter.IO.INPUT,
						ctxHandle),
				new Parameter(1, String.class, Parameter.IO.INPUT, regexp) });

		return ret.getMapStringStringValue();
	}

	public void PMsetDefault(ContextHandle ctxHandle, int idx, String value) {
		APIFunction.PM_SET_DEFAULT.call(this.conn,
				new Parameter[] {
						new Parameter(0, ContextHandle.class,
								Parameter.IO.INPUT, ctxHandle),
						new Parameter(1, Integer.class, Parameter.IO.INPUT,
								Integer.valueOf(idx)),
						new Parameter(2, String.class, Parameter.IO.INPUT,
								value) });
	}

	public void PMwrite(ContextHandle ctxHandle, String path) {
		APIFunction.PM_WRITE.call(this.conn, new Parameter[] {
				new Parameter(0, ContextHandle.class, Parameter.IO.INPUT,
						ctxHandle),
				new Parameter(1, String.class, Parameter.IO.INPUT, path) });
	}

	public void PMlowerOn(ContextHandle ctxHandle) {
		APIFunction.PM_LOWER_ON.call(this.conn,
				new Parameter[] { new Parameter(0, ContextHandle.class,
						Parameter.IO.INPUT, ctxHandle) });
	}

	public void CRsetPMval(ContextHandle ctxHandle, String key, String value) {
		APIFunction.CR_SET_PMVAL.call(this.conn, new Parameter[] {
				new Parameter(0, ContextHandle.class, Parameter.IO.INPUT,
						ctxHandle),
				new Parameter(1, String.class, Parameter.IO.INPUT, key),
				new Parameter(2, String.class, Parameter.IO.INPUT, value) });
	}

	public String[] LGgetLogPaths(int whichLog, String nodename) {
		Parameter ret = APIFunction.LG_GET_LOG_PATHS.call(this.conn,
				new Parameter[] {
						new Parameter(0, Integer.class, Parameter.IO.INPUT,
								Integer.valueOf(whichLog)),
						new Parameter(1, String.class, Parameter.IO.INPUT,
								nodename) });

		return ret.getStringArrayValue();
	}

	public boolean isTransactionLogConfigured(int whichLog, String nodename) {
		Parameter ret = APIFunction.IS_LOG_CONFIGURED.call(this.conn,
				new Parameter[] {
						new Parameter(0, Integer.class, Parameter.IO.INPUT,
								Integer.valueOf(whichLog)),
						new Parameter(1, String.class, Parameter.IO.INPUT,
								nodename) });

		return ret.getBooleanValue();
	}

	public FileHandle FSopenFile(String name) {
		Parameter ret = APIFunction.FS_OPEN_FILE.call(this.conn,
				new Parameter[] { new Parameter(0, String.class,
						Parameter.IO.INPUT, name) });

		return ret.getFileHandleValue();
	}

	public int FSreadFile(FileHandle handle, long offset, byte[] buffer) {
		Parameter ret = APIFunction.FS_READ_FILE.call(
				this.conn,
				new Parameter[] {
						new Parameter(0, FileHandle.class, Parameter.IO.INPUT,
								handle),
						new Parameter(1, Long.class, Parameter.IO.INPUT, Long
								.valueOf(offset)),
						new Parameter(2, Parameter.BYTE_ARRAY_CLASS,
								Parameter.IO.OUTPUT, buffer) });

		return ret.getIntValue();
	}

	public void FScloseFile(FileHandle handle) {
		APIFunction.FS_CLOSE_FILE.call(this.conn,
				new Parameter[] { new Parameter(0, FileHandle.class,
						Parameter.IO.INPUT, handle) });
	}

	public UtilityProcessHandle UTcreateDatabase(String name,
			CreateDBOptions options) {
		Parameter ret = APIFunction.UT_CREATE_DATABASE
				.call(this.conn,
						new Parameter[] {
								new Parameter(0, String.class,
										Parameter.IO.INPUT, name),
								new Parameter(1, CreateDBOptions.class,
										Parameter.IO.INPUT, options) });

		return ret.getUtilityProcessHandleValue();
	}

	public UtilityProcessHandle UTdestroyDatabase(String name,
			DestroyOptions options) {
		Parameter ret = APIFunction.UT_DESTROY_DATABASE
				.call(this.conn,
						new Parameter[] {
								new Parameter(0, String.class,
										Parameter.IO.INPUT, name),
								new Parameter(1, DestroyOptions.class,
										Parameter.IO.INPUT, options) });

		return ret.getUtilityProcessHandleValue();
	}

	public UtilityProcessHandle UTstartIngres(StartOptions options) {
		Parameter ret = APIFunction.UT_START_INGRES.call(this.conn,
				new Parameter[] { new Parameter(0, StartOptions.class,
						Parameter.IO.INPUT, options) });

		return ret.getUtilityProcessHandleValue();
	}

	public UtilityProcessHandle UTstopIngres(StopOptions options) {
		Parameter ret = APIFunction.UT_STOP_INGRES.call(this.conn,
				new Parameter[] { new Parameter(0, StopOptions.class,
						Parameter.IO.INPUT, options) });

		return ret.getUtilityProcessHandleValue();
	}

	public String[] FSgetFileList(String dir, String name, String ext) {
		Parameter ret = APIFunction.GET_FILE_LIST
				.call(this.conn,
						new Parameter[] {
								new Parameter(0, String.class,
										Parameter.IO.INPUT, dir),
								new Parameter(1, String.class,
										Parameter.IO.INPUT, name),
								new Parameter(2, String.class,
										Parameter.IO.INPUT, ext) });

		return ret.getStringArrayValue();
	}

	public String[] FSgetNonUnicodeCollations() {
		Parameter ret = APIFunction.FS_GET_NON_UNICODE_COLLATIONS.call(
				this.conn, new Parameter[0]);
		return ret.getStringArrayValue();
	}

	public String[] FSgetUnicodeCollations() {
		Parameter ret = APIFunction.FS_GET_UNICODE_COLLATIONS.call(this.conn,
				new Parameter[0]);
		return ret.getStringArrayValue();
	}

	public String[] FSgetLogFiles() {
		Parameter ret = APIFunction.FS_GET_LOG_FILES.call(this.conn,
				new Parameter[0]);
		return ret.getStringArrayValue();
	}

	public void UTwriteProcessInput(UtilityProcessHandle processHandle,
			String text) {
		APIFunction.UT_WRITE_PROCESS_INPUT.call(this.conn, new Parameter[] {
				new Parameter(0, UtilityProcessHandle.class,
						Parameter.IO.INPUT, processHandle),
				new Parameter(1, String.class, Parameter.IO.INPUT, text) });
	}

	public String UTreadProcessOutput(UtilityProcessHandle processHandle,
			int maxLength, boolean blocking) {
		Parameter ret = APIFunction.UT_READ_PROCESS_OUTPUT.call(this.conn,
				new Parameter[] {
						new Parameter(0, UtilityProcessHandle.class,
								Parameter.IO.INPUT, processHandle),
						new Parameter(1, Integer.class, Parameter.IO.INPUT,
								Integer.valueOf(maxLength)),
						new Parameter(2, Boolean.class, Parameter.IO.INPUT,
								Boolean.valueOf(blocking)) });

		return ret.getStringValue();
	}

	public int UTgetProcessExitValue(UtilityProcessHandle processHandle) {
		Parameter ret = APIFunction.UT_GET_PROCESS_EXIT_VALUE.call(this.conn,
				new Parameter[] { new Parameter(0, UtilityProcessHandle.class,
						Parameter.IO.INPUT, processHandle) });

		return ret.getIntValue();
	}

	public int UTwaitForProcessExitValue(UtilityProcessHandle processHandle) {
		Parameter ret = APIFunction.UT_WAIT_FOR_PROCESS_EXIT_VALUE.call(
				this.conn, new Parameter[] { new Parameter(0,
						UtilityProcessHandle.class, Parameter.IO.INPUT,
						processHandle) });

		return ret.getIntValue();
	}

	public void UTkillProcess(UtilityProcessHandle processHandle) {
		APIFunction.UT_KILL_PROCESS.call(this.conn,
				new Parameter[] { new Parameter(0, UtilityProcessHandle.class,
						Parameter.IO.INPUT, processHandle) });
	}

	public void UTreleaseProcessHandle(UtilityProcessHandle processHandle) {
		APIFunction.UT_RELEASE_PROCESS_HANDLE.call(this.conn,
				new Parameter[] { new Parameter(0, UtilityProcessHandle.class,
						Parameter.IO.INPUT, processHandle) });
	}

	public UtilityProcessHandle UTbackupDatabase(String name,
			BackupOptions options) {
		Parameter ret = APIFunction.UT_BACKUP_DATABASE
				.call(this.conn,
						new Parameter[] {
								new Parameter(0, String.class,
										Parameter.IO.INPUT, name),
								new Parameter(1, BackupOptions.class,
										Parameter.IO.INPUT, options) });

		return ret.getUtilityProcessHandleValue();
	}

	public UtilityProcessHandle UTrestoreDatabase(String name,
			RestoreOptions options) {
		Parameter ret = APIFunction.UT_RESTORE_DATABASE
				.call(this.conn,
						new Parameter[] {
								new Parameter(0, String.class,
										Parameter.IO.INPUT, name),
								new Parameter(1, RestoreOptions.class,
										Parameter.IO.INPUT, options) });

		return ret.getUtilityProcessHandleValue();
	}

	public UtilityProcessHandle UToptimizeDatabase(String name,
			OptimizeOptions options) {
		Parameter ret = APIFunction.UT_OPTIMIZE_DATABASE
				.call(this.conn,
						new Parameter[] {
								new Parameter(0, String.class,
										Parameter.IO.INPUT, name),
								new Parameter(1, OptimizeOptions.class,
										Parameter.IO.INPUT, options) });

		return ret.getUtilityProcessHandleValue();
	}

	public UtilityProcessHandle UTsysmodDatabase(String name,
			SysmodOptions options) {
		Parameter ret = APIFunction.UT_SYSMOD_DATABASE
				.call(this.conn,
						new Parameter[] {
								new Parameter(0, String.class,
										Parameter.IO.INPUT, name),
								new Parameter(1, SysmodOptions.class,
										Parameter.IO.INPUT, options) });

		return ret.getUtilityProcessHandleValue();
	}

	public UtilityProcessHandle UTusermodDatabase(String name,
			UsermodOptions options) {
		Parameter ret = APIFunction.UT_USERMOD_DATABASE
				.call(this.conn,
						new Parameter[] {
								new Parameter(0, String.class,
										Parameter.IO.INPUT, name),
								new Parameter(1, UsermodOptions.class,
										Parameter.IO.INPUT, options) });

		return ret.getUtilityProcessHandleValue();
	}

	public UtilityProcessHandle UTvwLoad(String name, VWLoadOptions options) {
		Parameter ret = APIFunction.UT_VWLOAD.call(this.conn, new Parameter[] {
				new Parameter(0, String.class, Parameter.IO.INPUT, name),
				new Parameter(1, VWLoadOptions.class, Parameter.IO.INPUT,
						options) });

		return ret.getUtilityProcessHandleValue();
	}

	public UtilityProcessHandle UTalterDatabase(String name,
			AlterOptions options) {
		Parameter ret = APIFunction.UT_ALTER_DATABASE
				.call(this.conn,
						new Parameter[] {
								new Parameter(0, String.class,
										Parameter.IO.INPUT, name),
								new Parameter(1, AlterOptions.class,
										Parameter.IO.INPUT, options) });

		return ret.getUtilityProcessHandleValue();
	}

	public UtilityProcessHandle UTextendDatabase(String name,
			ExtendOptions options) {
		Parameter ret = APIFunction.UT_EXTEND_DATABASE
				.call(this.conn,
						new Parameter[] {
								new Parameter(0, String.class,
										Parameter.IO.INPUT, name),
								new Parameter(1, ExtendOptions.class,
										Parameter.IO.INPUT, options) });

		return ret.getUtilityProcessHandleValue();
	}

	public UtilityProcessHandle UTverifyDatabase(String name,
			VerifyOptions options) {
		Parameter ret = APIFunction.UT_VERIFY_DATABASE
				.call(this.conn,
						new Parameter[] {
								new Parameter(0, String.class,
										Parameter.IO.INPUT, name),
								new Parameter(1, VerifyOptions.class,
										Parameter.IO.INPUT, options) });

		return ret.getUtilityProcessHandleValue();
	}

	public UtilityProcessHandle UTstatdumpDatabase(String name,
			StatdumpOptions options) {
		Parameter ret = APIFunction.UT_STATDUMP_DATABASE
				.call(this.conn,
						new Parameter[] {
								new Parameter(0, String.class,
										Parameter.IO.INPUT, name),
								new Parameter(1, StatdumpOptions.class,
										Parameter.IO.INPUT, options) });

		return ret.getUtilityProcessHandleValue();
	}

	public UtilityProcessHandle UTcopyDatabase(String name, CopyOptions options) {
		Parameter ret = APIFunction.UT_COPY_DATABASE
				.call(this.conn,
						new Parameter[] {
								new Parameter(0, String.class,
										Parameter.IO.INPUT, name),
								new Parameter(1, CopyOptions.class,
										Parameter.IO.INPUT, options) });

		return ret.getUtilityProcessHandleValue();
	}

	public UtilityProcessHandle UTunloadDatabase(String name,
			UnloadOptions options) {
		Parameter ret = APIFunction.UT_UNLOAD_DATABASE
				.call(this.conn,
						new Parameter[] {
								new Parameter(0, String.class,
										Parameter.IO.INPUT, name),
								new Parameter(1, UnloadOptions.class,
										Parameter.IO.INPUT, options) });

		return ret.getUtilityProcessHandleValue();
	}

	public UtilityProcessHandle UTauditDatabase(String name,
			AuditOptions options) {
		Parameter ret = APIFunction.UT_AUDIT_DATABASE
				.call(this.conn,
						new Parameter[] {
								new Parameter(0, String.class,
										Parameter.IO.INPUT, name),
								new Parameter(1, AuditOptions.class,
										Parameter.IO.INPUT, options) });

		return ret.getUtilityProcessHandleValue();
	}

	public UtilityProcessHandle UTinfoDatabase(String name, InfoOptions options) {
		Parameter ret = APIFunction.UT_INFO_DATABASE
				.call(this.conn,
						new Parameter[] {
								new Parameter(0, String.class,
										Parameter.IO.INPUT, name),
								new Parameter(1, InfoOptions.class,
										Parameter.IO.INPUT, options) });

		return ret.getUtilityProcessHandleValue();
	}

	public UtilityProcessHandle UTvwinfoDatabase(String name,
			VWInfoOptions options) {
		Parameter ret = APIFunction.UT_VWINFO_DATABASE
				.call(this.conn,
						new Parameter[] {
								new Parameter(0, String.class,
										Parameter.IO.INPUT, name),
								new Parameter(1, VWInfoOptions.class,
										Parameter.IO.INPUT, options) });

		return ret.getUtilityProcessHandleValue();
	}

	public int terminateServer(String user, int flags) {
		Parameter ret = APIFunction.TERMINATE_SERVER
				.call(this.conn,
						new Parameter[] {
								new Parameter(0, String.class,
										Parameter.IO.INPUT, user),
								new Parameter(1, Integer.class,
										Parameter.IO.INPUT, Integer
												.valueOf(flags)) });

		return ret.getIntValue();
	}
}
