package com.ingres;

import com.ingres.discovery.PathUtilities;
import com.ingres.util.CharUtil;
import com.ingres.util.IICharsetMap;
import com.ingres.util.Intl;
import com.ingres.util.Logging;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalIIapi extends IIapiBase {
	private static boolean isWindows;
	private static boolean isLinux;
	private IIapi.SysInfo sysInfo = null;

	static {
		String osName = System.getProperty("os.name").toLowerCase();
		isWindows = osName.startsWith("windows");
		isLinux = osName.startsWith("linux");
		if (isWindows) {
			//loodLibrary加载路径及顺序？
			System.loadLibrary("iitoolsjni");
		} else {
			System.loadLibrary("iitoolsjni.1");
		}
		
		//初始化iiapi native call 
		InitAPI();
		
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
			}
		}));
	}

	public LocalIIapi() {
		this(SessionAuth.getAuth());
	}

	public LocalIIapi(SessionAuth sessionAuth) {
		this.sessionAuth = sessionAuth;
		this.Instance = getToolsAPIInstance();
		if (this.Instance == 0L) {
			throw new IllegalStateException(Intl.getString(IIapi.pkg,
					"iiapi.toolsAPIError"));
		}
		
		try {
			Charset nativeCharset = Charset.defaultCharset();
			//Charset nativeCharset = Charset.forName("gbk");
			
			this.decoderForNativeChars = nativeCharset.newDecoder();
			this.decoderForNativeChars
					.onUnmappableCharacter(CodingErrorAction.REPORT);
			this.decoderForNativeChars
					.onMalformedInput(CodingErrorAction.REPORT);

			this.encoderForNativeChars = nativeCharset.newEncoder();
			this.encoderForNativeChars
					.onUnmappableCharacter(CodingErrorAction.REPORT);
			this.encoderForNativeChars
					.onMalformedInput(CodingErrorAction.REPORT);
			
			//@modify zhengxb 获取ingres charset name
			String ingresName = bootstrapCharsetName();
			String javaCharsetName = IICharsetMap.getJavaNameForIngresName(ingresName);
			Charset ingresCharset = Charset.forName(javaCharsetName);

			this.decoderForIngresChars = ingresCharset.newDecoder();
			this.decoderForIngresChars
					.onUnmappableCharacter(CodingErrorAction.REPORT);
			this.decoderForIngresChars
					.onMalformedInput(CodingErrorAction.REPORT);

			this.altDecoderForIngresChars = ingresCharset.newDecoder();
			this.altDecoderForIngresChars
					.onUnmappableCharacter(CodingErrorAction.REPLACE);
			this.altDecoderForIngresChars
					.onMalformedInput(CodingErrorAction.REPLACE);
			this.altDecoderForIngresChars.replaceWith(this.decoderReplacement);

			this.encoderForIngresChars = ingresCharset.newEncoder();
			this.encoderForIngresChars
					.onUnmappableCharacter(CodingErrorAction.REPORT);
			this.encoderForIngresChars
					.onMalformedInput(CodingErrorAction.REPORT);

			Charset utf8Charset = Charset.forName("UTF-8");

			this.decoderForUTF8Chars = utf8Charset.newDecoder();
			this.encoderForUTF8Chars = utf8Charset.newEncoder();
		} catch (IIapi.Exception e) {
			unloadInstallation();
			throw e;
		}
	}

	private long Instance = 0L;
	private CharsetEncoder encoderForNativeChars;
	private CharsetDecoder decoderForNativeChars;
	private CharsetEncoder encoderForIngresChars;
	private CharsetDecoder decoderForIngresChars;
	private CharsetEncoder encoderForUTF8Chars;
	private CharsetDecoder decoderForUTF8Chars;
	private CharsetDecoder altDecoderForIngresChars;
	private String decoderReplacement = "●";
	private IIapi.Exception decodingError = null;
	private IIapi.Exception lastDecodingError = null;
	private CharacterCodingException lastCCE = null;
	private int cceCount = 0;
	private SessionAuth sessionAuth;

	private byte[] encodeNativeBytes(byte[] b) {
		try {
			return this.encoderForNativeChars.encode(
					this.decoderForUTF8Chars.decode(ByteBuffer.wrap(b)))
					.array();
		} catch (CharacterCodingException cce) {
			throw new IIapi.Exception(String.format("%1$s: %2$s", new Object[] {
					cce.getClass().getName(), cce.getMessage() }));
		}
	}

	private byte[] encodeNativeChars(String s) {
		try {
			return this.encoderForNativeChars.encode(CharBuffer.wrap(s))
					.array();
		} catch (CharacterCodingException cce) {
			throw new IIapi.Exception(String.format("%1$s: %2$s", new Object[] {
					cce.getClass().getName(), cce.getMessage() }));
		}
	}

	private String decodeNativeChars(byte[] ba) {
		try {
			return this.decoderForNativeChars.decode(ByteBuffer.wrap(ba))
					.toString();
		} catch (CharacterCodingException cce) {
			throw new IIapi.Exception(String.format("%1$s: %2$s", new Object[] {
					cce.getClass().getName(), cce.getMessage() }));
		}
	}

	private byte[] encodeIngresBytes(byte[] b) {
		try {
			return this.encoderForIngresChars.encode(
					this.decoderForUTF8Chars.decode(ByteBuffer.wrap(b)))
					.array();
		} catch (CharacterCodingException cce) {
			throw new IIapi.Exception(String.format("%1$s: %2$s", new Object[] {
					cce.getClass().getName(), cce.getMessage() }));
		}
	}

	private byte[] encodeIngresChars(String s) {
		try {
			return this.encoderForIngresChars.encode(CharBuffer.wrap(s))
					.array();
		} catch (CharacterCodingException cce) {
			throw new IIapi.Exception(String.format("%1$s: %2$s", new Object[] {
					cce.getClass().getName(), cce.getMessage() }));
		}
	}

	private void chainDecodingError(CharacterCodingException cce, int count) {
		String countMsg;
		if (count == 0) {
			countMsg = Intl.getString(IIapi.pkg, "iiapi.noTimes");
		} else {
			if (count == 1) {
				countMsg = Intl.getString(IIapi.pkg, "iiapi.oneTime");
			} else {
				countMsg = Intl.formatString(IIapi.pkg, "iiapi.manyTimes",
						new Object[] { Integer.valueOf(count) });
			}
		}
		String msg = null;
		if ((cce instanceof MalformedInputException)) {
			msg = Intl.formatString(IIapi.pkg, "iiapi.malformedInput",
					new Object[] { this.decoderForIngresChars.charset().name(),
							this.decoderReplacement, countMsg });
		} else if ((cce instanceof UnmappableCharacterException)) {
			msg = Intl.formatString(IIapi.pkg, "iiapi.unmappableChar",
					new Object[] { this.decoderForIngresChars.charset().name(),
							this.decoderReplacement, countMsg });
		} else {
			msg = String.format("%1$s: %2$s (%3$s)", new Object[] {
					cce.getClass().getName(), cce.getMessage(), countMsg });
		}
		IIapi.Exception error = new IIapi.Exception(msg);
		if (this.decodingError == null) {
			this.decodingError = (this.lastDecodingError = error);
		} else {
			this.lastDecodingError.setNextException(error);
			this.lastDecodingError = error;
		}
	}

	private String decodeIngresChars(byte[] ba) {
		try {
			return this.decoderForIngresChars.decode(ByteBuffer.wrap(ba))
					.toString();
		} catch (CharacterCodingException cce) {
			if ((this.lastCCE != null)
					&& (this.lastCCE.getClass() != cce.getClass())) {
				chainDecodingError(this.lastCCE, this.cceCount);
			}
			this.lastCCE = cce;
			this.cceCount += 1;
			try {
				return this.altDecoderForIngresChars
						.decode(ByteBuffer.wrap(ba)).toString();
			} catch (CharacterCodingException cce2) {
				throw new IIapi.Exception(String.format(
						"%1$s: %2$s",
						new Object[] { cce2.getClass().getName(),
								cce2.getMessage() }));
			}
		}
	}
	
	//是否为远程
	public boolean isRemote() {
		return false;
	}

	public SessionAuth.AuthType getAuthType() {
		return this.sessionAuth.getAuthType();
	}

	public void unloadInstallation() {
		for (UtilityProcessHandle processHandle : this.instanceOwnedUtilityHandles) {
			UtilityProcess utilityProcess = (UtilityProcess) processRegistry
					.remove(processHandle);
			if (utilityProcess != null) {
				utilityProcess.release();
				Logging.Debug(
						"UtilityProcessHandle released during unload: %1$s",
						new Object[] { processHandle });
			} else {
				Logging.Debug(
						"UtilityProcessHandle not found during unload: %1$s",
						new Object[] { processHandle });
			}
		}
		for (FileHandle fileHandle : this.instanceOwnedFileHandles) {
			FileResource fileResource = (FileResource) fileRegistry
					.remove(fileHandle);
			if (fileResource != null) {
				fileResource.release();
				Logging.Debug("FileHandle released during unload: %1$s",
						new Object[] { fileHandle });
			} else {
				Logging.Debug("FileHandle not found during unload: %1$s",
						new Object[] { fileHandle });
			}
		}
		nativeUnloadInstallation();
	}
	
	/**
	 * 连接命名服务器
	 */
	public ConnectionHandle connectNameServer(String target, String user,
			byte[] password, int iTimeout, String effectiveUser) {
		boolean readOnly = false;
		if ((user != null) && (!user.isEmpty()) && (effectiveUser != null)
				&& (!effectiveUser.isEmpty())) {
			throw new IIapi.Exception("%iiapi.nameServerUsers");
		}
		String sessionUser = this.sessionAuth.getUID();
		// switch
		// (2.$SwitchMap$com$ingres$SessionAuth$AuthType[this.sessionAuth.getAuthType().ordinal()])
		switch (this.sessionAuth.getAuthType().ordinal()) {
		case 1:
			if ((effectiveUser != null) && (!effectiveUser.isEmpty())) {
				user = effectiveUser;
				password = null;
			}
			break;
		case 2:
			if ((user == null) || (user.isEmpty())) {
				boolean netAdmin = (checkPrivilege(sessionUser,
						IIapi.Privileges.NET_ADMIN))
						|| (checkPrivilege(sessionUser,
								IIapi.Privileges.TRUSTED));
				if ((effectiveUser == null) || (effectiveUser.isEmpty())) {
					user = sessionUser;
				} else {
					if (!netAdmin) {
						throw new IIapi.Exception(Intl.formatString(IIapi.pkg,
								"iiapi.needNetAdmin",
								new Object[] { sessionUser }));
					}
					user = effectiveUser;
				}
				password = null;
				readOnly = !netAdmin;
			}
			break;
		case 3:
			if ((user == null) || (user.isEmpty())) {
				if ((effectiveUser != null) && (!effectiveUser.isEmpty())) {
					if ((!checkPrivilege(sessionUser,
							IIapi.Privileges.NET_ADMIN))
							&& (!checkPrivilege(sessionUser,
									IIapi.Privileges.TRUSTED))) {
						throw new IIapi.Exception(Intl.formatString(IIapi.pkg,
								"iiapi.needNetAdmin",
								new Object[] { sessionUser }));
					}
					user = effectiveUser;
					password = null;
				} else {
					user = sessionUser;
					password = this.sessionAuth.getPWD();
				}
			}
			break;
		}
		ConnectionHandle handle = connect(true, target, user, password,
				iTimeout, null);
		connectionInfo.put(handle, new ConnectionType(true, readOnly));
		return handle;
	}

	private static class ConnectionType {
		public boolean nameServer;
		public boolean readOnly;

		public ConnectionType(boolean nameServer, boolean readOnly) {
			this.nameServer = nameServer;
			this.readOnly = readOnly;
		}
	}

	private static ConcurrentMap<ConnectionHandle, ConnectionType> connectionInfo = new ConcurrentHashMap();
	
	/**
	 * 连接数据库
	 */
	public ConnectionHandle connectDatabase(String target, String user,
			byte[] password, int iTimeout, ConnectOptions options) {
		String sessionUser = this.sessionAuth.getUID();
		// switch
		// (2.$SwitchMap$com$ingres$SessionAuth$AuthType[this.sessionAuth.getAuthType().ordinal()])
		switch (this.sessionAuth.getAuthType().ordinal()) {
		case 1:
			break;
		case 2:
			if ((user == null) || (user.isEmpty())) {
				checkDbAccess(target);
				if (options == null) {
					options = new ConnectOptions(sessionUser, null, null,
							CharUtil.getUtf8String(this.sessionAuth
									.getDBMSPWD()));
				} else if ((options.effectiveUser == null)
						|| (options.effectiveUser.isEmpty())) {
					options = new ConnectOptions(sessionUser, options.group,
							options.role,
							options.dbmsPassword != null ? options.dbmsPassword
									: CharUtil.getUtf8String(this.sessionAuth
											.getDBMSPWD()));
				} else if (checkUserPriv(32768) == 0) {
					throw new IIapi.Exception(
							Intl.formatString(IIapi.pkg, "iiapi.needPrivilege",
									new Object[] { sessionUser }));
				}
			}
			break;
		case 3:
			if ((user == null) || (user.isEmpty())) {
				user = sessionUser;
				password = this.sessionAuth.getPWD();
				byte[] dbmsPassword = this.sessionAuth.getDBMSPWD();
				if (dbmsPassword != null) {
					if (options == null) {
						options = new ConnectOptions(user, null, null,
								CharUtil.getUtf8String(dbmsPassword));
					} else if (options.dbmsPassword == null) {
						options = new ConnectOptions(user, options.group,
								options.role,
								CharUtil.getUtf8String(dbmsPassword));
					}
				}
			}
			break;
		}
		
		//连接数据库 jni call
		ConnectionHandle handle = connect(false, target, user, password,
				iTimeout, options);
		connectionInfo.put(handle, new ConnectionType(false, false));
		return handle;
	}

	public void abort(ConnectionHandle connHandle) {
		connectionInfo.remove(connHandle);
		nativeAbort(connHandle);
	}
	
	/**
	 * 断开数据库连接
	 */
	public void disconnect(ConnectionHandle connHandle) {
		connectionInfo.remove(connHandle);
		nativeDisconnect(connHandle);
	}

	@Deprecated
	public StatementHandle query(ConnectionHandle connHandle,
			TransactionHandle tranHandle, String query, boolean bNeedParms) {
		throw new IIapi.Exception("%iiapi.invalidConn");
	}
	
	/**
	 * 执行数据库查询，调用jni函数接口
	 * @param connHandle
	 * @param tranHandle
	 * @param query
	 * @param bNeedParms
	 * @param stmtHandle
	 */
	private void query(ConnectionHandle connHandle,
			TransactionHandle tranHandle, String query, boolean bNeedParms,
			StatementHandle stmtHandle) {
		ConnectionType connType = (ConnectionType) connectionInfo
				.get(connHandle);
		if (connType == null) {
			throw new IIapi.Exception("%iiapi.invalidConn");
		}
		if ((connType.nameServer) && (connType.readOnly)) {
			if (!query.startsWith("show ")) {
				throw new IIapi.Exception("%iiapi.needPassModify");
			}
		}
		nativeQuery(connHandle, tranHandle, stmtHandle, query, bNeedParms);
	}
	
	/**
	 * 执行语句
	 */
	public IIapi.QueryInfo executeStatement(ConnectionHandle connHandle,
			TransactionHandle tranHandle, String statement,
			IIapi.Descriptor[] descriptors, IIapi.DataValue[] datavalues) {
		boolean params = false;
		if (descriptors == null) {
			if (datavalues != null) {
				throw new IllegalArgumentException(Intl.getString(IIapi.pkg,
						"iiapi.nullDescArray"));
			}
		} else {
			if (datavalues == null) {
				throw new IllegalArgumentException(Intl.getString(IIapi.pkg,
						"iiapi.nullDataArray"));
			}
			if (descriptors.length != datavalues.length) {
				throw new IllegalArgumentException(Intl.getString(IIapi.pkg,
						"iiapi.arrayLengthDiff"));
			}
			params = descriptors.length > 0;
		}
		StatementHandle stmtHandle = new StatementHandle();
		query(connHandle, tranHandle, statement, params, stmtHandle);
		if (params) {
			try {
				putParameters(stmtHandle, descriptors, datavalues);
			} catch (IIapi.Exception ex) {
				try {
					cancel(stmtHandle);
				} catch (IIapi.Exception ignore) {
				}
				try {
					close(stmtHandle);
				} catch (IIapi.Exception ignore) {
				}
				throw ex;
			}
		}
		IIapi.QueryInfo queryInfo = null;
		try {
			queryInfo = getQueryInfo(stmtHandle);
		} catch (IIapi.Exception ex) {
			try {
				cancel(stmtHandle);
			} catch (IIapi.Exception ignore) {
			}
			try {
				close(stmtHandle);
			} catch (IIapi.Exception ignore) {
			}
			throw ex;
		}
		close(stmtHandle);
		return queryInfo;
	}
	
	/**
	 * 执行数据库查询
	 */
	public IIapi.Descriptor[] executeQuery(ConnectionHandle connHandle,
			TransactionHandle tranHandle, StatementHandle stmtHandle,
			String query, IIapi.Descriptor[] descriptors,
			IIapi.DataValue[] datavalues) {
		boolean params = false;
		if (descriptors == null) {
			if (datavalues != null) {
				throw new IllegalArgumentException(Intl.getString(IIapi.pkg,
						"iiapi.nullDescArray"));
			}
		} else {
			if (datavalues == null) {
				throw new IllegalArgumentException(Intl.getString(IIapi.pkg,
						"iiapi.nullDataArray"));
			}
			if (descriptors.length != datavalues.length) {
				throw new IllegalArgumentException(Intl.getString(IIapi.pkg,
						"iiapi.arrayLengthDiff"));
			}
			params = descriptors.length > 0;
		}
		query(connHandle, tranHandle, query, params, stmtHandle);
		if (params) {
			try {
				putParameters(stmtHandle, descriptors, datavalues);
			} catch (IIapi.Exception ex) {
				try {
					cancel(stmtHandle);
				} catch (IIapi.Exception ignore) {
				}
				try {
					close(stmtHandle);
				} catch (IIapi.Exception ignore) {
				}
				throw ex;
			}
		}
		IIapi.Descriptor[] resultDescriptors = null;
		try {
			resultDescriptors = getDescriptors(stmtHandle);
		} catch (IIapi.Exception ex) {
			try {
				cancel(stmtHandle);
			} catch (IIapi.Exception ignore) {
			}
			try {
				close(stmtHandle);
			} catch (IIapi.Exception ignore) {
			}
			throw ex;
		}
		if (resultDescriptors.length == 0) {
			try {
				getQueryInfo(stmtHandle);
			} catch (IIapi.Exception ex) {
				try {
					cancel(stmtHandle);
				} catch (IIapi.Exception ignore) {
				}
				try {
					close(stmtHandle);
				} catch (IIapi.Exception ignore) {
				}
				throw ex;
			}
			close(stmtHandle);
		}
		return resultDescriptors;
	}

	public IIapi.QueryInfo endQuery(StatementHandle stmtHandle) {
		IIapi.QueryInfo queryInfo = null;
		try {
			queryInfo = getQueryInfo(stmtHandle);
		} catch (IIapi.Exception ex) {
			try {
				cancel(stmtHandle);
			} catch (IIapi.Exception ignore) {
			}
			try {
				close(stmtHandle);
			} catch (IIapi.Exception ignore) {
			}
			throw ex;
		}
		close(stmtHandle);
		return queryInfo;
	}
	
	/**
	 * 获取列信息
	 */
	public boolean getColumns(StatementHandle stmtHandle,
			IIapi.Descriptor[] desc, IIapi.DataValue[] data) {
		IIapi.DataValue[][] rows = new IIapi.DataValue[1][];
		rows[0] = data;
		return getRows(stmtHandle, desc, rows) > 0;
	}
	
	/**
	 * 获取所有行
	 */
	public int getRows(StatementHandle stmtHandle, IIapi.Descriptor[] desc,
			IIapi.DataValue[][] rows, boolean close) {
		int rowCount = getRows(stmtHandle, desc, rows);
		if ((rowCount < rows.length) && (close)) {
			try {
				getQueryInfo(stmtHandle);
			} catch (IIapi.Exception ex) {
				try {
					cancel(stmtHandle);
				} catch (IIapi.Exception ignore) {
				}
			}
			try {
				close(stmtHandle);
			} catch (IIapi.Exception ignore) {
			}
		}
		return rowCount;
	}

	public IIapi.Exception getDecodingError() {
		if (this.lastCCE != null) {
			chainDecodingError(this.lastCCE, this.cceCount);
			this.lastCCE = null;
			this.cceCount = 0;
		}
		return this.decodingError;
	}

	public void resetDecodingError() {
		this.decodingError = (this.lastDecodingError = null);
		this.lastCCE = null;
		this.cceCount = 0;
	}

	public void setDecodingReplacement(String replace) {
		this.decoderReplacement = replace;
		this.altDecoderForIngresChars.replaceWith(this.decoderReplacement);
	}

	public String IDname() {
		// switch
		// (2.$SwitchMap$com$ingres$SessionAuth$AuthType[this.sessionAuth.getAuthType().ordinal()])
		switch (this.sessionAuth.getAuthType().ordinal()) {
		case 1:
			break;
		case 2:
		case 3:
			return this.sessionAuth.getUID();
		}
		return nativeIDname();
	}

	public String IDnameServer() {
		return nativeIDname();
	}
	
	/**
	 * 获取系统信息
	 */
	public IIapi.SysInfo getSysInfo() {
		if (this.sysInfo == null) {
			this.sysInfo = new IIapi.SysInfo();
			
			//获取数据库安装路径
			this.sysInfo.IIinstallation = getEnv("II_INSTALLATION");
			if ((this.sysInfo.IIinstallation == null)
					|| (this.sysInfo.IIinstallation.isEmpty())) {
				throw new IIapi.Exception("%iiapi.notDefInstID");
			}
			
			//获取II_SYSTEM路径
			this.sysInfo.IIsystem = getEnv("II_SYSTEM");
			if ((this.sysInfo.IIsystem == null)
					|| (this.sysInfo.IIsystem.isEmpty())) {
				throw new IIapi.Exception("%iiapi.notDefIISystem");
			}
			try {
				this.sysInfo.IIsystem = new File(this.sysInfo.IIsystem)
						.getCanonicalPath();
			} catch (IOException ignore) {
			}
			this.sysInfo.hostName = GChostname(false);
			try {
				this.sysInfo.fullyQualifiedHostName = GChostname(true);
			} catch (IIapi.Exception ignore) {
			}
			if ((this.sysInfo.hostName.equalsIgnoreCase("localhost"))
					&& (this.sysInfo.fullyQualifiedHostName != null)) {
				int ix = this.sysInfo.fullyQualifiedHostName.indexOf('.');
				if (ix > 0) {
					this.sysInfo.hostName = this.sysInfo.fullyQualifiedHostName
							.substring(0, ix);
				}
			}
			this.sysInfo.version = getVersionInfo();
			
			//获取操作系统信息
			this.sysInfo.osName = System.getProperty("os.name");
			this.sysInfo.osVersion = System.getProperty("os.version");
		}
		return this.sysInfo;
	}

	public boolean isClientFullyAuthorized() {
		SessionAuth.AuthType authType = this.sessionAuth.getAuthType();
		return ((authType == SessionAuth.AuthType.PROCESS_UID) && (!this.sessionAuth
				.isNotFullyTrusted()))
				|| (authType == SessionAuth.AuthType.CLIENT_PWD);
	}

	private void checkAuthorizedForConfigChanges() {
		// switch
		// (2.$SwitchMap$com$ingres$SessionAuth$AuthType[this.sessionAuth.getAuthType().ordinal()])
		switch (this.sessionAuth.getAuthType().ordinal()) {
		case 1:
			break;
		case 2:
		case 3:
			if (!checkPrivilege(this.sessionAuth.getUID(),
					IIapi.Privileges.TRUSTED)) {
				throw new IIapi.Exception(Intl.formatString(IIapi.pkg,
						"iiapi.needAdminConfig",
						new Object[] { this.sessionAuth.getUID() }));
			}
			break;
		}
	}

	public void PMinsert(ContextHandle ctxHandle, String pmkey, String value) {
		checkAuthorizedForConfigChanges();

		nativePMinsert(ctxHandle, pmkey, value);
	}

	public void PMdelete(ContextHandle ctxHandle, String pmkey) {
		checkAuthorizedForConfigChanges();

		nativePMdelete(ctxHandle, pmkey);
	}

	public void PMwrite(ContextHandle ctxHandle, String path) {
		checkAuthorizedForConfigChanges();

		nativePMwrite(ctxHandle, path);
	}

	public void CRsetPMval(ContextHandle ctxHandle, String key, String value) {
		checkAuthorizedForConfigChanges();

		nativeCRsetPMval(ctxHandle, key, value);
	}

	public boolean isTransactionLogConfigured(int whichLog, String nodename) {
		String[] paths = LGgetLogPaths(whichLog, nodename);
		if (paths.length == 0) {
			return false;
		}
		for (String path : paths) {
			File file = new File(path);
			if ((!file.exists()) || (!file.canWrite())) {
				return false;
			}
		}
		return true;
	}

	private static final ConcurrentMap<FileHandle, FileResource> fileRegistry = new ConcurrentHashMap();
	private final HashSet<FileHandle> instanceOwnedFileHandles = new HashSet();
	private static final int DB_GLOBAL = 1;
	private static final int DB_ACCESS = 2048;
	private static final int U_CREATEDB = 1;
	private static final int U_SECURITY = 32768;

	private FileHandle newFileHandle(FileResource f) {
		FileHandle handleObj = null;
		long handleValue = 0L;
		FileResource previousFileResource = null;
		if (f != null) {
			do {
				handleValue = secureRandom.nextLong();
				handleObj = new FileHandle(handleValue);
				previousFileResource = (FileResource) fileRegistry.putIfAbsent(
						handleObj, f);
			} while (previousFileResource != null);
			this.instanceOwnedFileHandles.add(handleObj);

			Logging.Debug("FileHandle created:  %1$s",
					new Object[] { handleObj });
		}
		return handleObj;
	}

	private static class FileResource {
		File f;
		InputStream is;
		String name;
		long pos;

		public FileResource(String name) throws FileNotFoundException {
			this.name = name;
			this.f = new File(name);
			if ((this.f.exists()) && (this.f.isFile()) && (this.f.canRead())) {
				try {
					this.is = new FileInputStream(this.f);
				} catch (IOException ioe) {
					throw new IIapi.Exception(String.format(
							"%1$s: %2$s",
							new Object[] { ioe.getClass().getName(),
									ioe.getMessage() }));
				}
				this.pos = 0L;
			} else {
				throw new FileNotFoundException(name);
			}
		}

		public void release() {
			if (this.is != null) {
				try {
					this.is.close();
				} catch (IOException ignore) {
				}
				this.is = null;
			}
			this.pos = 0L;
		}

		public int read(long offset, byte[] buf) {
			try {
				if (offset == this.pos) {
					if ((this.is != null) && (this.is.available() > 0)) {
						int len = this.is.read(buf);
						this.pos += len;
						return len;
					}
					return -1;
				}
				if (this.is != null) {
					this.is.close();
					this.is = new FileInputStream(this.f);
					this.is.skip(offset);
					this.pos = offset;
					if (this.is.available() > 0) {
						int len = this.is.read(buf);
						this.pos += len;
						return len;
					}
				}
				return -1;
			} catch (IOException ioe) {
				throw new IIapi.Exception(String.format(
						"%1$s: %2$s",
						new Object[] { ioe.getClass().getName(),
								ioe.getMessage() }));
			}
		}
	}

	public FileHandle FSopenFile(String name) {
		try {
			return newFileHandle(new FileResource(name));
		} catch (FileNotFoundException fnfe) {
		}
		return null;
	}

	public int FSreadFile(FileHandle handle, long offset, byte[] buffer) {
		FileResource fileResource = (FileResource) fileRegistry.get(handle);
		if (fileResource != null) {
			return fileResource.read(offset, buffer);
		}
		return -1;
	}

	public void FScloseFile(FileHandle handle) {
		if (this.instanceOwnedFileHandles.remove(handle)) {
			FileResource fileResource = (FileResource) fileRegistry
					.remove(handle);
			if (fileResource != null) {
				fileResource.release();
				Logging.Debug("FileHandle released: %1$s",
						new Object[] { handle });
			} else {
				Logging.Debug("FileHandle missing from global registry: %1$s",
						new Object[] { handle });
			}
		} else {
			throw new IIapi.Exception("%iiapi.badFileHandle");
		}
	}
	
	/**
	 * 对外接口，创建数据库
	 */
	public UtilityProcessHandle UTcreateDatabase(String name,
			CreateDBOptions options) {
		String safeEffUser = options == null ? null
				: safeString(options.effUser);
		// switch
		// (2.$SwitchMap$com$ingres$SessionAuth$AuthType[this.sessionAuth.getAuthType().ordinal()])
		switch (this.sessionAuth.getAuthType().ordinal()) {
		case 1:
			break;
		case 2:
		case 3:
			int privs = checkUserPriv(32769);
			if ((privs & 0x1) == 0) {
				throw new IIapi.Exception(Intl.formatString(IIapi.pkg,
						"iiapi.needCreateDB",
						new Object[] { this.sessionAuth.getUID() }));
			}
			if ((safeEffUser == null) || (safeEffUser.isEmpty())) {
				safeEffUser = CharUtil.delimitIdentifier(
						this.sessionAuth.getUID(), false);
			} else if ((privs & 0x8000) == 0) {
				throw new IIapi.Exception(Intl.formatString(IIapi.pkg,
						"iiapi.needPrivilege",
						new Object[] { this.sessionAuth.getUID() }));
			}
			break;
		}
		this.dbAccessInfo = null;

		return createDatabase(name, options, safeEffUser);
	}
	
	/**
	 * 创建数据库
	 * @param name
	 * @param options
	 * @param safeEffUser
	 * @return
	 */
	private UtilityProcessHandle createDatabase(String name,
			CreateDBOptions options, String safeEffUser) {
		if (options == null) {
			options = new CreateDBOptions();
		}
		List<String> command = new ArrayList();
		
		//获取createdb命令的绝对路径
		command.add(fullPathOf("createdb"));
		if (options.serverClass != null) {
			command.add(String.format("%1$s/%2$s", new Object[] {
					safeString(name), options.serverClass.getServerClass() }));
		} else {
			command.add(safeString(name));
		}
		if ((options.cdbname != null) && (!options.cdbname.isEmpty())) {
			command.add(safeString(options.cdbname));
		}
		if ((options.databaseLoc != null) && (!options.databaseLoc.isEmpty())) {
			command.add(escapeForCmdLine("-d" + safeString(options.databaseLoc)));
		}
		if ((options.checkpointLoc != null)
				&& (!options.checkpointLoc.isEmpty())) {
			command.add(escapeForCmdLine("-c"
					+ safeString(options.checkpointLoc)));
		}
		if ((options.journalLoc != null) && (!options.journalLoc.isEmpty())) {
			command.add(escapeForCmdLine("-j" + safeString(options.journalLoc)));
		}
		if ((options.dumpLoc != null) && (!options.dumpLoc.isEmpty())) {
			command.add(escapeForCmdLine("-b" + safeString(options.dumpLoc)));
		}
		if ((options.workLoc != null) && (!options.workLoc.isEmpty())) {
			command.add(escapeForCmdLine("-w" + safeString(options.workLoc)));
		}
		if ((options.catalogProducts != null)
				&& (options.catalogProducts.size() != 0)) {
			command.add("-f");
			for (CatalogProduct cp : options.catalogProducts) {
				command.add(cp.toString());
			}
		}
		if (options.unicode) {
			String collation = options.collationName != null ? options.collationName
					: "";
			if (options.normalization == 0) {
				command.add("-i" + safeString(collation));
			} else if (options.normalization == 1) {
				command.add("-n" + safeString(collation));
			}
		} else if ((options.collationName != null)
				&& (!options.collationName.isEmpty())) {
			command.add("-l" + safeString(options.collationName));
		}
		if (options.privateDB) {
			command.add("-p");
		}
		if ((safeEffUser != null) && (!safeEffUser.isEmpty())) {
			command.add(escapeForCmdLine("-u" + safeEffUser));
		}
		if ((options.password != null) && (!options.password.isEmpty())) {
			command.add(escapeForCmdLine("-P" + safeString(options.password)));
		}
		if ((options.readOnlyLoc != null) && (!options.readOnlyLoc.isEmpty())) {
			command.add(escapeForCmdLine("-r" + safeString(options.readOnlyLoc)));
		}
		if (options.pageSize != 0) {
			command.add(String.format("-page_size=%1$d",
					new Object[] { Integer.valueOf(options.pageSize) }));
		}
		if (options.alwaysLogged) {
			command.add("-m");
		}
		return launchUtilityAsync(command);
	}
	
	/**
	 * 对外接口，销毁数据库
	 */
	public UtilityProcessHandle UTdestroyDatabase(String name,
			DestroyOptions options) {
		String safeEffUser = options == null ? null
				: safeString(options.effUser);
		// switch
		// (2.$SwitchMap$com$ingres$SessionAuth$AuthType[this.sessionAuth.getAuthType().ordinal()])
		switch (this.sessionAuth.getAuthType().ordinal()) {
		case 1:
			break;
		case 2:
		case 3:
			if ((safeEffUser == null) || (safeEffUser.isEmpty())) {
				safeEffUser = CharUtil.delimitIdentifier(
						this.sessionAuth.getUID(), false);
			} else if (checkUserPriv(32768) == 0) {
				throw new IIapi.Exception(Intl.formatString(IIapi.pkg,
						"iiapi.needPrivilege",
						new Object[] { this.sessionAuth.getUID() }));
			}
			break;
		}
		this.dbAccessInfo = null;

		return destroyDatabase(name, options, safeEffUser);
	}

	private UtilityProcessHandle destroyDatabase(String name,
			DestroyOptions options, String safeEffUser) {
		if (options == null) {
			options = new DestroyOptions();
		}
		List<String> command = new ArrayList();
		command.add(fullPathOf("destroydb"));
		if (options.serverClass != null) {
			command.add(String.format("%1$s/%2$s", new Object[] {
					safeString(name), options.serverClass.getServerClass() }));
		} else {
			command.add(safeString(name));
		}
		if (options.abortIfInUse) {
			command.add("-l");
		}
		if ((safeEffUser != null) && (!safeEffUser.isEmpty())) {
			command.add(escapeForCmdLine("-u" + safeEffUser));
		}
		if ((options.password != null) && (!options.password.isEmpty())) {
			command.add(escapeForCmdLine("-P" + safeString(options.password)));
		}
		return launchUtilityAsync(command);
	}

	public UtilityProcessHandle UTstartIngres(StartOptions options) {
		// switch
		// (2.$SwitchMap$com$ingres$SessionAuth$AuthType[this.sessionAuth.getAuthType().ordinal()])
		switch (this.sessionAuth.getAuthType().ordinal()) {
		case 1:
			if (!checkPrivilege(this.sessionAuth.getUID(),
					IIapi.Privileges.SERVER_CONTROL)) {
				throw new IIapi.Exception("%iiapi.serverControlStart");
			}
			break;
		case 2:
		case 3:
			if (!checkPrivilege(this.sessionAuth.getUID(),
					IIapi.Privileges.SERVER_CONTROL)) {
				throw new IIapi.Exception("%iiapi.serverControlStart");
			}
			break;
		}
		return startIngres(options);
	}

	private UtilityProcessHandle startIngres(StartOptions options) {
		if (options == null) {
			options = new StartOptions();
		}
		List<String> command = new ArrayList();
		command.add(fullPathOf("ingstart"));
		if (options.asService) {
			command.add("-service");
		}
		if (options.server != null) {
			StringBuilder sb = new StringBuilder("-");
			sb.append(options.server.getStartupFlag());
			if ((options.configName != null) && (!options.configName.isEmpty())) {
				sb.append("=");
				sb.append(safeString(options.configName));
			}
			command.add(sb.toString());
		}
		if (options.allClusterNodes) {
			command.add("-cluster");
		}
		if ((options.clusterNodeName != null)
				&& (!options.clusterNodeName.isEmpty())) {
			command.add("-node");
			command.add(safeString(options.clusterNodeName));
		}
		return launchUtilityAsync(command);
	}

	public UtilityProcessHandle UTstopIngres(StopOptions options) {
		switch (this.sessionAuth.getAuthType().ordinal()) {
		case 1:
			if (!checkPrivilege(this.sessionAuth.getUID(),
					IIapi.Privileges.SERVER_CONTROL)) {
				throw new IIapi.Exception("%iiapi.serverControlStop");
			}
			break;
		case 2:
		case 3:
			if (!checkPrivilege(this.sessionAuth.getUID(),
					IIapi.Privileges.SERVER_CONTROL)) {
				throw new IIapi.Exception("%iiapi.serverControlStop");
			}
			break;
		}
		return stopIngres(options);
	}

	private UtilityProcessHandle stopIngres(StopOptions options) {
		if (options == null) {
			options = new StopOptions();
		}
		List<String> command = new ArrayList();
		command.add(fullPathOf("ingstop"));
		if (options.asService) {
			command.add("-service");
		}
		if (options.server != null) {
			StringBuilder sb = new StringBuilder("-");
			sb.append(options.server.getStartupFlag());
			if ((options.connectId != null) && (!options.connectId.isEmpty())) {
				sb.append("=");
				sb.append(safeConnectId(options.connectId));
			}
			command.add(sb.toString());
		}
		if (options.f) {
			command.add("-f");
		}
		if (options.timeout > 0) {
			command.add(String.format("-timeout=%d",
					new Object[] { Integer.valueOf(options.timeout) }));
		}
		if (options.kill) {
			command.add("-kill");
		}
		if (options.force) {
			command.add("-force");
		}
		if (options.immediate) {
			command.add("-immediate");
		}
		if (options.show) {
			command.add("-show");
		}
		if (options.allClusterNodes) {
			command.add("-cluster");
		}
		if ((options.clusterNodeName != null)
				&& (!options.clusterNodeName.isEmpty())) {
			command.add("-node");
			command.add(safeString(options.clusterNodeName));
		}
		return launchUtilityAsync(command);
	}

	private class WildcardFilenameFilter implements FilenameFilter {
		private Pattern namePattern;

		public WildcardFilenameFilter(String name, String ext) {
			StringBuilder buf = new StringBuilder(((name == null ? 1
					: name.length()) + (ext == null ? 1 : ext.length())) * 2);

			buf.append('^');
			if ((name != null) && (!name.isEmpty())) {
				for (int i = 0; i < name.length(); i++) {
					char ch = name.charAt(i);
					if (ch == '*') {
						buf.append(".*");
					} else if (ch == '?') {
						buf.append('.');
					} else {
						buf.append(ch);
					}
				}
			}
			if ((ext != null) && (!ext.isEmpty())) {
				buf.append("\\.");
				for (int i = 0; i < ext.length(); i++) {
					char ch = ext.charAt(i);
					if (ch == '*') {
						buf.append(".*");
					} else if (ch == '?') {
						buf.append('.');
					} else {
						buf.append(ch);
					}
				}
			}
			buf.append('$');
			this.namePattern = Pattern.compile(buf.toString());
		}

		public boolean accept(File dir, String name) {
			File f = new File(dir, name);
			if ((f.exists()) && (f.isFile())) {
				return this.namePattern.matcher(name).matches();
			}
			return false;
		}
	}

	private String[] getFileList(String dir, String name, String ext) {
		File directory = new File(getSysInfo().IIsystem, dir);
		return directory.list(new WildcardFilenameFilter(name, ext));
	}

	public String[] FSgetFileList(String dir, String name, String ext) {
		verifyAuthorizationForUtilityLaunch();

		return getFileList(dir, name, ext);
	}

	public String[] FSgetNonUnicodeCollations() {
		String[] files = getFileList("ingres/files/collation", "*", "dsc");
		String[] outputNames = new String[files.length];
		for (int i = 0; i < files.length; i++) {
			outputNames[i] = files[i].replace(".dsc", "");
		}
		return outputNames;
	}

	public String[] FSgetUnicodeCollations() {
		String[] files = getFileList("ingres/files/collation", "*", "uce");
		String[] outputNames = new String[files.length];
		for (int i = 0; i < files.length; i++) {
			outputNames[i] = files[i].replace(".uce", "");
		}
		return outputNames;
	}

	public String[] FSgetLogFiles() {
		return new File(getLogFilesDirectory())
				.list(new WildcardFilenameFilter("*", "log"));
	}

	private static class DBAccess {
		public String dbName;

		public static enum AccessType {
			DEFAULT, GRANTED, DENIED;

			private AccessType() {
			}
		}

		public boolean owner = false;
		public boolean isPublic = false;
		public AccessType access = AccessType.DEFAULT;

		public DBAccess(String dbName) {
			this.dbName = dbName;
		}
	}

	private Hashtable<String, DBAccess> dbAccessInfo = null;

	private void checkDbAccess(String target) {
		String dbName = target.trim().toLowerCase();
		DBAccess dbAccess;
		if ((this.dbAccessInfo == null)
				|| ((dbAccess = (DBAccess) this.dbAccessInfo.get(dbName)) == null)) {
			loadDbAccess();

			dbAccess = (DBAccess) this.dbAccessInfo.get(dbName);
			if (dbAccess == null) {
				throw new IIapi.Exception(Intl.formatString(IIapi.pkg,
						"iiapi.accessUnknownDB", new Object[] {
								this.sessionAuth.getUID(), target }));
			}
		}
		if ((dbAccess.owner)
				|| (dbAccess.access == LocalIIapi.DBAccess.AccessType.GRANTED)) {
			return;
		}
		if (dbAccess.isPublic) {
			if (dbAccess.access != LocalIIapi.DBAccess.AccessType.DENIED) {
				return;
			}
			throw new IIapi.Exception(Intl.formatString(IIapi.pkg,
					"iiapi.accessDBDenied",
					new Object[] { this.sessionAuth.getUID(), target }));
		}
		throw new IIapi.Exception(Intl.formatString(IIapi.pkg,
				"iiapi.accessDBPrivate",
				new Object[] { this.sessionAuth.getUID(), target }));
	}

	private void loadDbAccess() {
		String uid = this.sessionAuth.getUID().trim().toLowerCase();
		this.dbAccessInfo = new Hashtable();

		ConnectionHandle conn = connect(false, "iidbdb", null, null, -1, null);
		TransactionHandle tran = new TransactionHandle();

		StatementHandle stmt = new StatementHandle();
		nativeQuery(
				conn,
				tran,
				stmt,
				"select database_name, database_owner, access from iidatabase_info",
				false);

		IIapi.Descriptor[] desc = getDescriptors(stmt);
		if ((desc != null) && (desc.length >= 2)) {
			IIapi.DataValue[] values = new IIapi.DataValue[desc.length];
			while (getColumns(stmt, desc, values)) {
				if ((values[0].value != null)
						&& ((values[0].value instanceof String))) {
					String dbName = ((String) values[0].value).trim()
							.toLowerCase();
					if ((values[1].value != null)
							&& ((values[1].value instanceof String))) {
						String owner = ((String) values[1].value).trim()
								.toLowerCase();
						if ((values[2].value != null)
								&& ((values[2].value instanceof Number))) {
							int access = ((Number) values[2].value).intValue();
							if (this.dbAccessInfo.get(dbName) == null) {
								DBAccess dbAccess = new DBAccess(dbName);
								if (uid.equals(owner)) {
									dbAccess.owner = true;
								}
								if ((access & 0x1) != 0) {
									dbAccess.isPublic = true;
								}
								this.dbAccessInfo.put(dbName, dbAccess);
							}
						}
					}
				}
			}
		}
		close(stmt);

		nativeQuery(
				conn,
				tran,
				stmt,
				"select dbname, grantee, control, flags from iidbpriv where gtype = 0",
				false);
		desc = getDescriptors(stmt);
		if ((desc != null) && (desc.length >= 4)) {
			IIapi.DataValue[] values = new IIapi.DataValue[desc.length];
			while (getColumns(stmt, desc, values)) {
				if ((values[0].value != null)
						&& ((values[0].value instanceof String))) {
					String dbName = ((String) values[0].value).trim()
							.toLowerCase();
					if ((values[1].value != null)
							&& ((values[1].value instanceof String))) {
						String grantee = ((String) values[1].value).trim()
								.toLowerCase();
						if ((values[2].value != null)
								&& ((values[2].value instanceof Number))) {
							int control = ((Number) values[2].value).intValue();
							if ((values[3].value != null)
									&& ((values[3].value instanceof Number))) {
								int flags = ((Number) values[3].value)
										.intValue();

								DBAccess dbAccess = (DBAccess) this.dbAccessInfo
										.get(dbName);
								if (dbAccess == null) {
									dbAccess = new DBAccess(dbName);
									this.dbAccessInfo.put(dbName, dbAccess);
								}
								if (((control & 0x800) != 0)
										&& (uid.equals(grantee))) {
									dbAccess.access = ((flags & 0x800) != 0 ? LocalIIapi.DBAccess.AccessType.GRANTED
											: LocalIIapi.DBAccess.AccessType.DENIED);
								}
							}
						}
					}
				}
			}
		}
		close(stmt);
		commit(tran);
		nativeDisconnect(conn);
	}

	private int checkUserPriv(int priv) {
		String uid = this.sessionAuth.getUID().trim().toLowerCase();
		int clientPrivs = 0;

		ConnectionHandle conn = connect(false, "iidbdb", null, null, -1, null);
		TransactionHandle tran = new TransactionHandle();
		StatementHandle stmt = new StatementHandle();
		nativeQuery(conn, tran, stmt,
				"select user_name, internal_status from iiusers", false);
		IIapi.Descriptor[] desc = getDescriptors(stmt);
		if ((desc != null) && (desc.length >= 2)) {
			IIapi.DataValue[] values = new IIapi.DataValue[desc.length];
			while (getColumns(stmt, desc, values)) {
				if ((values[0].value != null)
						&& ((values[0].value instanceof String))) {
					String name = ((String) values[0].value).trim()
							.toLowerCase();
					if ((values[1].value != null)
							&& ((values[1].value instanceof Number))) {
						int privs = ((Number) values[1].value).intValue();
						if (uid.equals(name)) {
							clientPrivs |= privs & priv;
						}
					}
				}
			}
		}
		close(stmt);
		commit(tran);
		nativeDisconnect(conn);
		return clientPrivs;
	}

	private static final SecureRandom secureRandom = new SecureRandom();
	private static final ConcurrentMap<UtilityProcessHandle, UtilityProcess> processRegistry = new ConcurrentHashMap();
	private final HashSet<UtilityProcessHandle> instanceOwnedUtilityHandles = new HashSet();

	private static final class UtilityProcess {
		public final Process process;
		public final BufferedReader processOutput;
		public final BufferedWriter processInput;
		public final LocalIIapi.OptionsFile optionsFile;

		public UtilityProcess(Process process, BufferedReader processOutput,
				BufferedWriter processInput, LocalIIapi.OptionsFile optionsFile) {
			this.process = process;
			this.processOutput = processOutput;
			this.processInput = processInput;
			this.optionsFile = optionsFile;
		}

		public void release() {
			if (this.optionsFile != null) {
				this.optionsFile.release();
			}
			if (this.processOutput != null) {
				try {
					this.processOutput.close();
				} catch (IOException ioe) {
					Logging.Except(ioe);
				}
			}
			if (this.processInput != null) {
				try {
					this.processInput.close();
				} catch (IOException ioe) {
					Logging.Except(ioe);
				}
			}
		}
	}

	private static final class OptionsFile {
		public final File optionsFile;
		public final BufferedWriter writer;

		public OptionsFile(IIapi iiapi) {
			try {
				File tempdir = new File(iiapi.getEnv("II_TEMPORARY"));
				this.optionsFile = File.createTempFile("IIapi_utility_",
						".txt", tempdir);
				Logging.Debug("OptionsFile created: '%1$s'",
						new Object[] { this.optionsFile.getPath() });
				this.writer = new BufferedWriter(new FileWriter(
						this.optionsFile));
			} catch (IOException ioe) {
				throw new IIapi.Exception(String.format(
						"%1$s: %2$s",
						new Object[] { ioe.getClass().getName(),
								ioe.getMessage() }));
			}
		}

		public String getName() {
			return this.optionsFile.getPath();
		}

		public void add(String text) {
			try {
				this.writer.write(text);
				this.writer.newLine();
			} catch (IOException ioe) {
				throw new IIapi.Exception(String.format(
						"%1$s: %2$s",
						new Object[] { ioe.getClass().getName(),
								ioe.getMessage() }));
			}
		}

		public void close() {
			try {
				this.writer.flush();
				this.writer.close();
			} catch (IOException ioe) {
				throw new IIapi.Exception(String.format(
						"%1$s: %2$s",
						new Object[] { ioe.getClass().getName(),
								ioe.getMessage() }));
			}
		}

		public void release() {
			try {
				this.writer.close();
				this.optionsFile.delete();
				Logging.Debug("OptionsFile deleted: '%1$s'",
						new Object[] { this.optionsFile.getPath() });
			} catch (IOException ioe) {
				Logging.Except(ioe);
			}
		}
	}

	private UtilityProcessHandle newUtilityProcessHandle(
			UtilityProcess utilityProcess) {
		UtilityProcessHandle handleObj = null;
		long handleValue = 0L;
		UtilityProcess previousUtilityProcess = null;
		do {
			handleValue = secureRandom.nextLong();
			handleObj = new UtilityProcessHandle(handleValue);
			previousUtilityProcess = (UtilityProcess) processRegistry
					.putIfAbsent(handleObj, utilityProcess);
		} while (previousUtilityProcess != null);
		this.instanceOwnedUtilityHandles.add(handleObj);

		Logging.Debug("UtilityProcessHandle created:  %1$s",
				new Object[] { handleObj });
		return handleObj;
	}
	
	/**
	 * 
	 * @param command
	 * @return
	 */
	private String fullPathOf(String command) {
		File exeFile = PathUtilities.getUtilityPath(new File(getSysInfo().IIsystem), command);
		if (exeFile != null) {
			return exeFile.getPath();
		}
		throw new IIapi.Exception(Intl.formatString(IIapi.pkg, "iiapi.utilityNotFound", new Object[] { command }));
	}
	
	/**
	 * 执行命令，例createdb
	 * @param command
	 * @param cwd
	 * @param optionsFile
	 * @param allowInput
	 * @return
	 */
	private UtilityProcessHandle launchUtilityAsync(List<String> command,
			String cwd, OptionsFile optionsFile, boolean allowInput) {
		try {
			ProcessBuilder pb = new ProcessBuilder(command);
			pb.redirectErrorStream(true);
			if (Logging.isLevelEnabled(5)) {
				StringBuilder sb = new StringBuilder(String.format(
						"launchUtilityAsync[cwd=%1$s]: ", new Object[] { cwd }));
				for (String s : pb.command()) {
					if (((s.indexOf(' ') >= 0) || (s.indexOf('\t') >= 0))
							&& (s.charAt(0) != '"')) {
						sb.append("\"").append(s).append("\" ");
					} else {
						sb.append(s).append(" ");
					}
				}
				Logging.Debug("%1$s", new Object[] { sb.toString() });
			}
			
			//? cwd 代表什么呢？
			if ((cwd != null) && (!cwd.isEmpty())) {
				pb.directory(new File(cwd));
			}
			
			//linux特殊处理
			if (isLinux) {
				try {
					Map<String, String> env = pb.environment();
					env.remove("LD_PRELOAD");
				} catch (IIapi.Exception e) {
					Logging.Info("Error removing LD_PRELOAD: %1$s",
							new Object[] { e.getMessage() });
				}
			}
			Process process = pb.start();
			BufferedReader processOutput = new BufferedReader(
					new InputStreamReader(process.getInputStream()));
			BufferedWriter processInput = null;
			if (allowInput) {
				processInput = new BufferedWriter(new OutputStreamWriter(
						process.getOutputStream()));
			}
			UtilityProcess utilityProcess = new UtilityProcess(process,
					processOutput, processInput, optionsFile);
			return newUtilityProcessHandle(utilityProcess);
		} catch (IOException ioe) {
			throw new IIapi.Exception(String.format("%1$s: %2$s", new Object[] {
					ioe.getClass().getName(), ioe.getMessage() }));
		}
	}

	private UtilityProcessHandle launchUtilityAsync(List<String> command) {
		return launchUtilityAsync(command, null, null, true);
	}

	private void verifyAuthorizationForUtilityLaunch() {
		if (!isClientFullyAuthorized()) {
			throw new IIapi.Exception("%iiapi.utilityAsOwner");
		}
	}

	private String safeString(String s) {
		if ((!isClientFullyAuthorized()) && (s != null) && (!s.isEmpty())
				&& (!safeCommandLinePattern.matcher(s).matches())) {
			throw new IIapi.Exception(Intl.formatString(IIapi.pkg,
					"iiapi.utilityUnsafeString", new Object[] { s }));
		}
		return s;
	}

	private static Pattern safeCommandLinePattern = Pattern
			.compile("[\\p{L}\\p{N}_][\\p{L}\\p{N}_#@$\\.]*|\"[\\p{L}\\p{N}_#@$\\.]+\"");

	private String safeConnectId(String s) {
		if ((!isClientFullyAuthorized()) && (s != null) && (!s.isEmpty())
				&& (!safeConnectIdPattern.matcher(s).matches())) {
			throw new IIapi.Exception(Intl.formatString(IIapi.pkg,
					"iiapi.ingstopUnsafeString", new Object[] { s }));
		}
		return s;
	}

	private static Pattern safeConnectIdPattern = Pattern
			.compile("[\\p{Alnum}][\\p{Alnum}\\\\]*");

	private String join(String[] items, String itemDelimiter) {
		StringBuilder sb = new StringBuilder();
		if (items != null) {
			for (String item : items) {
				if (item != null) {
					if (sb.length() > 0) {
						sb.append(itemDelimiter);
					}
					sb.append(item);
				}
			}
		}
		return sb.toString();
	}

	private String escapeForCmdLine(String arg) {
		if (isWindows) {
			return CharUtil.windowsEscapeForCmdLine(arg);
		}
		return arg;
	}

	public void UTwriteProcessInput(UtilityProcessHandle processHandle,
			String text) {
		UtilityProcess utilityProcess = (UtilityProcess) processRegistry
				.get(processHandle);
		if (utilityProcess == null) {
			throw new IIapi.Exception("%iiapi.invalidProcHandle");
		}
		if (utilityProcess.processInput == null) {
			throw new IIapi.Exception("%iiapi.cannotInput");
		}
		try {
			utilityProcess.processInput.write(text);
			utilityProcess.processInput.newLine();
			utilityProcess.processInput.flush();
		} catch (IOException ioe) {
			throw new IIapi.Exception(String.format("%1$s: %2$s", new Object[] {
					ioe.getClass().getName(), ioe.getMessage() }));
		}
	}

	public String UTreadProcessOutput(UtilityProcessHandle processHandle,
			int maxLength, boolean blocking) {
		UtilityProcess utilityProcess = (UtilityProcess) processRegistry
				.get(processHandle);
		if (utilityProcess == null) {
			throw new IIapi.Exception("%iiapi.invalidProcHandle");
		}
		try {
			String text = "";
			int length = 0;
			if ((this.reusableBuffer == null)
					|| (this.reusableBuffer.length < maxLength)) {
				this.reusableBuffer = new char[maxLength];
			}
			if ((blocking) || (utilityProcess.processOutput.ready())) {
				length = utilityProcess.processOutput.read(this.reusableBuffer,
						0, maxLength);
			}
			if (length > 0) {
				text = new String(this.reusableBuffer, 0, length);
			} else if (length >= 0) {
			}
			return null;
		} catch (IOException ioe) {
			throw new IIapi.Exception(String.format("%1$s: %2$s", new Object[] {
					ioe.getClass().getName(), ioe.getMessage() }));
		}
	}

	private char[] reusableBuffer = null;

	public int UTgetProcessExitValue(UtilityProcessHandle processHandle) {
		UtilityProcess utilityProcess = (UtilityProcess) processRegistry
				.get(processHandle);
		if (utilityProcess == null) {
			throw new IIapi.Exception("%iiapi.invalidProcHandle");
		}
		int retval = 2147483647;
		try {
			retval = utilityProcess.process.exitValue();
		} catch (IllegalThreadStateException ignore) {
		}
		return retval;
	}

	public int UTwaitForProcessExitValue(UtilityProcessHandle processHandle) {
		UtilityProcess utilityProcess = (UtilityProcess) processRegistry
				.get(processHandle);
		if (utilityProcess == null) {
			throw new IIapi.Exception("%iiapi.invalidProcHandle");
		}
		int retval = -2147483648;
		try {
			retval = utilityProcess.process.waitFor();
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		return retval;
	}

	public void UTkillProcess(UtilityProcessHandle processHandle) {
		UtilityProcess utilityProcess = (UtilityProcess) processRegistry
				.get(processHandle);
		if (utilityProcess == null) {
			throw new IIapi.Exception("%iiapi.invalidProcHandle");
		}
		utilityProcess.process.destroy();
	}

	public void UTreleaseProcessHandle(UtilityProcessHandle processHandle) {
		if (this.instanceOwnedUtilityHandles.remove(processHandle)) {
			UtilityProcess utilityProcess = (UtilityProcess) processRegistry
					.remove(processHandle);
			if (utilityProcess != null) {
				utilityProcess.release();
				Logging.Debug("UtilityProcessHandle released: %1$s",
						new Object[] { processHandle });
			} else {
				Logging.Debug(
						"UtilityProcessHandle missing from global registry: %1$s",
						new Object[] { processHandle });
			}
		} else {
			throw new IIapi.Exception("%invalidProcHandle2");
		}
	}
	
	/**
	 * 数据库备份
	 */
	public UtilityProcessHandle UTbackupDatabase(String name,
			BackupOptions options) {
		if (options == null) {
			options = new BackupOptions();
		}
		verifyAuthorizationForUtilityLaunch();

		List<String> command = new ArrayList();

		command.add(fullPathOf("dmfjsp"));
		command.add("ckpdb");
		if (options.serverClass != null) {
			command.add(String.format("%1$s/%2$s", new Object[] { name,
					options.serverClass.getServerClass() }));
		} else {
			command.add(name);
		}
		if (options.destroyAllPrevious) {
			command.add("-d");
		}
		if (options.enableJournaling) {
			command.add("+j");
		}
		if (options.disableJournaling) {
			command.add("-j");
		}
		if (options.lockDatabase) {
			command.add("-l");
		}
		if (options.locationsAtOnce > 1) {
			command.add(String.format("#m%1$d",
					new Object[] { Integer.valueOf(options.locationsAtOnce) }));
		}
		if ((options.tapeDevices != null) && (options.tapeDevices.length != 0)) {
			command.add("-m" + join(options.tapeDevices, ","));
		}
		if ((options.tableNames != null) && (options.tableNames.length != 0)) {
			command.add(escapeForCmdLine("-table="
					+ join(options.tableNames, ",")));
		}
		if (options.verbose) {
			command.add("-v");
		}
		if (options.wait) {
			command.add("+w");
		}
		if ((options.timeout != null) && (!options.timeout.isEmpty())) {
			command.add("-timeout=" + options.timeout);
		}
		if ((options.effUser != null) && (!options.effUser.isEmpty())) {
			command.add(escapeForCmdLine("-u" + options.effUser));
		}
		return launchUtilityAsync(command);
	}

	public UtilityProcessHandle UTrestoreDatabase(String name,
			RestoreOptions options) {
		if (options == null) {
			options = new RestoreOptions();
		}
		verifyAuthorizationForUtilityLaunch();

		List<String> command = new ArrayList();

		command.add(fullPathOf("dmfjsp"));
		command.add("rolldb");
		if (options.serverClass != null) {
			command.add(String.format("%1$s/%2$s", new Object[] { name,
					options.serverClass.getServerClass() }));
		} else {
			command.add(name);
		}
		if (!options.useCheckpoint) {
			command.add("-c");
		}
		if (options.checkpointNumber >= 0) {
			if (options.checkpointNumber > 0) {
				command.add(String.format("#c%1$d", new Object[] { Integer
						.valueOf(options.checkpointNumber) }));
			} else {
				command.add("#c");
			}
		}
		if (!options.useJournal) {
			command.add("-j");
		}
		if ((options.tapeDevices != null) && (options.tapeDevices.length != 0)) {
			command.add("-m" + join(options.tapeDevices, ","));
		}
		if ((options.effUser != null) && (!options.effUser.isEmpty())) {
			command.add(escapeForCmdLine("-u" + options.effUser));
		}
		if (options.locationsAtOnce > 1) {
			command.add(String.format("#m%1$d",
					new Object[] { Integer.valueOf(options.locationsAtOnce) }));
		}
		if (options.verbose) {
			command.add("-v");
		}
		if (options.wait) {
			command.add("+w");
		}
		if ((options.beginDate != null) && (!options.beginDate.isEmpty())) {
			command.add("-b" + options.beginDate);
		}
		if ((options.endDate != null) && (!options.endDate.isEmpty())) {
			command.add("-e" + options.endDate);
		}
		if (options.incremental) {
			command.add("-incremental");
		}
		if (options.noRollback) {
			command.add("-norollback");
		}
		if ((options.tableNames != null) && (options.tableNames.length != 0)) {
			command.add(escapeForCmdLine("-table="
					+ join(options.tableNames, ",")));
		}
		if (options.noSecondaryIndex) {
			command.add("-nosecondary_index");
		}
		if (options.forceJournaling) {
			command.add("#f");
		}
		if (options.printStatistics) {
			command.add("-statistics");
		}
		if (options.ignoreErrors) {
			command.add("-ignore");
		}
		if (options.continueOnError) {
			command.add("-on_error_continue");
		}
		if (options.promptOnError) {
			command.add("-on_error_prompt");
		}
		if (options.relocate) {
			command.add("-relocate");
		}
		if ((options.oldLocations != null)
				&& (options.oldLocations.length != 0)) {
			command.add(escapeForCmdLine("-location="
					+ join(options.oldLocations, ",")));
		}
		if ((options.newLocations != null)
				&& (options.newLocations.length != 0)) {
			command.add(escapeForCmdLine("-new_location="
					+ join(options.newLocations, ",")));
		}
		if (options.dmfCacheSize >= 0) {
			command.add("-dmf_cache_size=" + options.dmfCacheSize);
		}
		if (options.dmfCacheSize4k >= 0) {
			command.add("-dmf_cache_size_4k=" + options.dmfCacheSize4k);
		}
		if (options.dmfCacheSize8k >= 0) {
			command.add("-dmf_cache_size_8k=" + options.dmfCacheSize8k);
		}
		if (options.dmfCacheSize16k >= 0) {
			command.add("-dmf_cache_size_16k=" + options.dmfCacheSize16k);
		}
		if (options.dmfCacheSize32k >= 0) {
			command.add("-dmf_cache_size_32k=" + options.dmfCacheSize32k);
		}
		if (options.dmfCacheSize64k >= 0) {
			command.add("-dmf_cache_size_64k=" + options.dmfCacheSize64k);
		}
		return launchUtilityAsync(command);
	}
	
	/**
	 * 优化数据库操作 OptimizeDB
	 */
	public UtilityProcessHandle UToptimizeDatabase(String name,
			OptimizeOptions options) {
		if (options == null) {
			options = new OptimizeOptions();
		}
		verifyAuthorizationForUtilityLaunch();

		List<String> command = new ArrayList();
		command.add(fullPathOf("optimizedb"));
		if ((options.zf != null) && (!options.zf.isEmpty())) {
			command.add("-zf");
			command.add(options.zf);
			return launchUtilityAsync(command, options.cwd, null, true);
		}
		OptionsFile optionsFile = new OptionsFile(this);
		command.add("-zf");
		command.add(optionsFile.getName());
		if ((options.effUser != null) && (!options.effUser.isEmpty())) {
			optionsFile.add("-u" + options.effUser);
		}
		if (options.sqlOpts != null) {
			for (String opt : options.sqlOpts) {
				optionsFile.add(opt);
			}
		}
		if ((options.iFilename != null) && (!options.iFilename.isEmpty())) {
			optionsFile.add("-i " + options.iFilename);
		}
		if ((options.oFilename != null) && (!options.oFilename.isEmpty())) {
			optionsFile.add("-o " + options.oFilename);
		}
		if (options.zc) {
			optionsFile.add("-zc");
		}
		if (options.zcpk) {
			optionsFile.add("-zcpk");
		}
		if (options.zdn) {
			optionsFile.add("-zdn");
		}
		if (options.ze) {
			optionsFile.add("-ze");
		}
		if (options.zfq) {
			optionsFile.add("-zfq");
		}
		if (options.zh) {
			optionsFile.add("-zh");
		}
		if (options.zhex) {
			optionsFile.add("-zhex");
		}
		if (options.zk) {
			optionsFile.add("-zk");
		}
		if (options.zlr) {
			optionsFile.add("-zlr");
		}
		if (options.zns) {
			optionsFile.add("-zns");
		}
		if (options.znt) {
			optionsFile.add("-znt");
		}
		if (options.zn >= 0) {
			optionsFile.add(String.format("-zn%1$d",
					new Object[] { Integer.valueOf(options.zn) }));
		}
		if (options.zp) {
			optionsFile.add("-zp");
		}
		if (options.zr >= 0) {
			optionsFile.add(String.format("-zr%1$d",
					new Object[] { Integer.valueOf(options.zr) }));
		}
		if (options.zs >= 0.0D) {
			optionsFile.add(String.format("-zs%1$f",
					new Object[] { Double.valueOf(options.zs) }));
		}
		if (options.zss >= 0.0D) {
			optionsFile.add(String.format("-zss%1$f",
					new Object[] { Double.valueOf(options.zss) }));
		}
		if (options.zu >= 0) {
			optionsFile.add(String.format("-zu%1$d",
					new Object[] { Integer.valueOf(options.zu) }));
		}
		if (options.zv) {
			optionsFile.add("-zv");
		}
		if (options.zw) {
			optionsFile.add("-zw");
		}
		if (options.zx) {
			optionsFile.add("-zx");
		}
		if (options.serverClass != null) {
			optionsFile.add(String.format("%1$s/%2$s", new Object[] { name,
					options.serverClass.getServerClass() }));
		} else {
			optionsFile.add(name);
		}
		if ((options.rTables != null) && (options.rTables.length != 0)) {
			for (OptimizeOptions.TableAndCols tabAndCols : options.rTables) {
				if ((tabAndCols != null) && (tabAndCols.rTable != null)) {
					optionsFile.add("-r" + tabAndCols.rTable);
					if (tabAndCols.aCols != null) {
						for (String aCol : tabAndCols.aCols) {
							if ((aCol != null) && (!aCol.isEmpty())) {
								optionsFile.add("-a" + aCol);
							}
						}
					}
				}
			}
		}
		if ((options.xrTables != null) && (options.xrTables.length != 0)) {
			for (String xrTable : options.xrTables) {
				if ((xrTable != null) && (!xrTable.isEmpty())) {
					optionsFile.add("-xr" + xrTable);
				}
			}
		}
		optionsFile.close();
		
		return launchUtilityAsync(command, options.cwd, optionsFile, true);
	}

	public UtilityProcessHandle UTsysmodDatabase(String name,
			SysmodOptions options) {
		if (options == null) {
			options = new SysmodOptions();
		}
		verifyAuthorizationForUtilityLaunch();

		List<String> command = new ArrayList();
		command.add(fullPathOf("sysmod"));
		if (options.serverClass != null) {
			command.add(String.format("%1$s/%2$s", new Object[] { name,
					options.serverClass.getServerClass() }));
		} else {
			command.add(name);
		}
		if (options.tables != null) {
			for (String table : options.tables) {
				if ((table != null) && (!table.isEmpty())) {
					command.add(escapeForCmdLine(table));
				}
			}
		}
		if ((options.catalogProducts != null)
				&& (options.catalogProducts.size() != 0)) {
			command.add("-f");
			for (CatalogProduct cp : options.catalogProducts) {
				command.add(cp.toString());
			}
		}
		if (options.newPageSize > 0) {
			command.add(String.format("-page_size=%1$d",
					new Object[] { Integer.valueOf(options.newPageSize) }));
		}
		if (options.wait) {
			command.add("+w");
		}
		return launchUtilityAsync(command);
	}

	public UtilityProcessHandle UTusermodDatabase(String name,
			UsermodOptions options) {
		if (options == null) {
			options = new UsermodOptions();
		}
		verifyAuthorizationForUtilityLaunch();

		List<String> command = new ArrayList();
		command.add(fullPathOf("usermod"));
		if (options.serverClass != null) {
			command.add(String.format("%1$s/%2$s", new Object[] { name,
					options.serverClass.getServerClass() }));
		} else {
			command.add(name);
		}
		if ((options.effUser != null) && (!options.effUser.isEmpty())) {
			command.add(escapeForCmdLine("-u" + options.effUser));
		}
		if (options.tables != null) {
			for (String table : options.tables) {
				if ((table != null) && (!table.isEmpty())) {
					command.add(escapeForCmdLine(table));
				}
			}
		}
		if (options.online) {
			command.add("-online");
		}
		if (options.noint) {
			command.add("-noint");
		}
		if (options.repmod) {
			command.add("-repmod");
			if (options.repmodWait) {
				command.add("+w");
			}
		}
		return launchUtilityAsync(command);
	}

	public UtilityProcessHandle UTvwLoad(String name, VWLoadOptions options) {
		if (options == null) {
			options = new VWLoadOptions();
		}
		verifyAuthorizationForUtilityLaunch();

		List<String> command = new ArrayList();
		command.add(fullPathOf("vwload"));
		if ((options.attributes != null) && (!options.attributes.isEmpty())) {
			command.add("-a");
			command.add(escapeForCmdLine(options.attributes));
		}
		if (options.header) {
			command.add("--header");
		}
		if ((options.fdelim != null) && (!options.fdelim.isEmpty())) {
			command.add("-f");
			command.add(escapeForCmdLine(options.fdelim));
		}
		if ((options.rdelim != null) && (!options.rdelim.isEmpty())) {
			command.add("-r");
			command.add(escapeForCmdLine(options.rdelim));
		}
		if ((options.quote != null) && (!options.quote.isEmpty())) {
			command.add("-q");
			command.add(escapeForCmdLine(options.quote));
		}
		if ((options.escape != null) && (!options.escape.isEmpty())) {
			command.add("-e");
			command.add(escapeForCmdLine(options.escape));
		}
		if ((options.dateFormat != null) && (!options.dateFormat.isEmpty())) {
			command.add("-d");
			command.add(options.dateFormat);
		}
		if ((options.nullValue != null) && (!options.nullValue.isEmpty())) {
			command.add("-n");
			command.add(escapeForCmdLine(options.nullValue));
		}
		if (options.ignoreFirst) {
			command.add("--ignfirst");
		}
		if (options.ignoreLast) {
			command.add("--ignlast");
		}
		if ((options.charset != null) && (!options.charset.isEmpty())) {
			command.add("--charset");
			command.add(options.charset);
		}
		if ((options.substitute != null) && (!options.substitute.isEmpty())) {
			command.add("--substitute");
			command.add(escapeForCmdLine(options.substitute));
		}
		if (options.skip > 0) {
			command.add("-s");
			command.add(Integer.toString(options.skip));
		}
		if (options.errCount > 0) {
			command.add("-x");
			command.add(Integer.toString(options.errCount));
		}
		if (options.verbose) {
			command.add("-v");
		}
		if (options.rollback) {
			command.add("--rollback");
		}
		if ((options.effUser != null) && (!options.effUser.isEmpty())) {
			command.add("-u");
			command.add(escapeForCmdLine(options.effUser));
		}
		if ((options.log != null) && (!options.log.isEmpty())) {
			command.add("-l");
			command.add(options.log);
		}
		if ((options.tablename != null) && (!options.tablename.isEmpty())) {
			command.add("-t");
			command.add(escapeForCmdLine(options.tablename));
		}
		if (options.serverClass != null) {
			command.add(String.format("%1$s/%2$s", new Object[] { name,
					options.serverClass.getServerClass() }));
		} else {
			command.add(name);
		}
		if (options.filenames != null) {
			for (String filename : options.filenames) {
				if ((filename != null) && (!filename.isEmpty())) {
					command.add(filename);
				}
			}
		}
		return launchUtilityAsync(command, options.cwd, null, true);
	}

	public UtilityProcessHandle UTalterDatabase(String name,
			AlterOptions options) {
		if (options == null) {
			options = new AlterOptions();
		}
		verifyAuthorizationForUtilityLaunch();

		List<String> command = new ArrayList();

		command.add(fullPathOf("dmfjsp"));
		command.add("alterdb");
		if (options.disableJournaling) {
			command.add("-disable_journaling");
		}
		if (options.deleteOldestCheckpoint) {
			command.add("-delete_oldest_ckp");
		}
		if (options.initJournalBlocks >= 0) {
			command.add(String
					.format("-init_jnl_blocks=%1$d", new Object[] { Integer
							.valueOf(options.initJournalBlocks) }));
		}
		if (options.journalBlockSize >= 0) {
			command.add(String.format("-jnl_block_size=%1$d",
					new Object[] { Integer.valueOf(options.journalBlockSize) }));
		}
		if (options.nextJournalFile) {
			command.add("-next_jnl_file");
		}
		if (options.targetJournalBlocks >= 0) {
			command.add(String
					.format("-target_jnl_blocks=%1$d", new Object[] { Integer
							.valueOf(options.targetJournalBlocks) }));
		}
		if (options.deleteInvalidCheckpoints) {
			command.add("-delete_invalid_ckp");
		}
		if (options.normalization != null) {
			String collation = options.unicodeCollationName == null ? ""
					: options.unicodeCollationName;
			if (options.normalization == AlterOptions.NormalizationForm.C) {
				command.add("-i" + collation);
			} else if (options.normalization == AlterOptions.NormalizationForm.D) {
				command.add("-n" + collation);
			}
		}
		if (options.keep >= 0) {
			command.add(String.format("-keep=%1$d",
					new Object[] { Integer.valueOf(options.keep) }));
		}
		if (options.disableMVCC) {
			command.add("-disable_mvcc");
		}
		if (options.enableMVCC) {
			command.add("-enable_mvcc");
		}
		if (options.enableMustlog) {
			command.add("-enable_mustlog");
		}
		if (options.disableMustlog) {
			command.add("-disable_mustlog");
		}
		if (options.verbose) {
			command.add("-verbose");
		}
		if (options.serverClass != null) {
			command.add(String.format("%1$s/%2$s", new Object[] { name,
					options.serverClass.getServerClass() }));
		} else {
			command.add(name);
		}
		return launchUtilityAsync(command);
	}

	public UtilityProcessHandle UTextendDatabase(String name,
			ExtendOptions options) {
		if (options == null) {
			options = new ExtendOptions();
		}
		verifyAuthorizationForUtilityLaunch();

		List<String> command = new ArrayList();
		command.add(fullPathOf("extenddb"));
		if ((options.location != null) && (!options.location.isEmpty())) {
			command.add(escapeForCmdLine("-l" + options.location));
		}
		if ((options.effUser != null) && (!options.effUser.isEmpty())) {
			command.add(escapeForCmdLine("-u" + options.effUser));
		}
		if (options.noDB) {
			command.add("-nodb");
		} else if ((options.dbnames == null) || (options.dbnames.length == 0)) {
			if (options.serverClass != null) {
				command.add(String.format("%1$s/%2$s", new Object[] { name,
						options.serverClass.getServerClass() }));
			} else {
				command.add(name);
			}
		} else {
			for (String db : options.dbnames) {
				if ((db != null) && (!db.isEmpty())) {
					command.add(db);
				}
			}
		}
		if ((options.areaDirectory != null)
				&& (!options.areaDirectory.isEmpty())) {
			command.add("-a" + options.areaDirectory);
		}
		if ((options.locationUsages != null)
				&& (options.locationUsages.size() != 0)) {
			StringBuilder sb = new StringBuilder();
			boolean firstTime = true;
			for (ExtendOptions.Usage usage : options.locationUsages) {
				if (firstTime) {
					sb.append("-U").append(usage);
					firstTime = false;
				} else {
					sb.append(",").append(usage);
				}
			}
			command.add(sb.toString());
		}
		if (options.rawPct >= 0) {
			command.add(String.format("-r%1$d",
					new Object[] { Integer.valueOf(options.rawPct) }));
		}
		if (options.drop) {
			command.add("-drop");
		}
		if (options.alter) {
			command.add("-alter");
		}
		return launchUtilityAsync(command, options.cwd, null, true);
	}

	public UtilityProcessHandle UTverifyDatabase(String name,
			VerifyOptions options) {
		if (options == null) {
			options = new VerifyOptions();
		}
		verifyAuthorizationForUtilityLaunch();

		List<String> command = new ArrayList();
		command.add(fullPathOf("verifydb"));
		if (options.mode != null) {
			command.add("-m" + options.mode);
		}
		if (options.scope != null) {
			command.add("-s" + options.scope);
			if (options.scope == VerifyOptions.Scope.DBNAME) {
				if ((options.dbnames == null) || (options.dbnames.length == 0)) {
					if (options.serverClass != null) {
						command.add(String.format("%1$s/%2$s", new Object[] {
								name, options.serverClass.getServerClass() }));
					} else {
						command.add(name);
					}
				} else {
					command.add(join(options.dbnames, " "));
				}
			}
		}
		if (options.operation != null) {
			command.add("-o" + options.operation);
			if ((options.operation == VerifyOptions.Operation.DROP_TABLE)
					|| (options.operation == VerifyOptions.Operation.TABLE)
					|| (options.operation == VerifyOptions.Operation.XTABLE)) {
				if ((options.tablenames != null)
						&& (options.tablenames.length != 0)) {
					command.add(escapeForCmdLine(join(options.tablenames, " ")));
				}
			}
		}
		if (options.noLog) {
			command.add("-n");
		}
		if ((options.logfile != null) && (!options.logfile.isEmpty())) {
			command.add("-lf" + options.logfile);
		}
		if (options.verbose) {
			command.add("-v");
		}
		if ((options.effUser != null) && (!options.effUser.isEmpty())) {
			command.add(escapeForCmdLine("-u" + options.effUser));
		}
		return launchUtilityAsync(command);
	}

	public UtilityProcessHandle UTstatdumpDatabase(String name,
			StatdumpOptions options) {
		if (options == null) {
			options = new StatdumpOptions();
		}
		verifyAuthorizationForUtilityLaunch();

		List<String> command = new ArrayList();
		command.add(fullPathOf("statdump"));
		if ((options.zf != null) && (!options.zf.isEmpty())) {
			command.add("-zf");
			command.add(options.zf);
			return launchUtilityAsync(command, options.cwd, null, true);
		}
		OptionsFile optionsFile = new OptionsFile(this);
		command.add("-zf");
		command.add(optionsFile.getName());
		if ((options.effUser != null) && (!options.effUser.isEmpty())) {
			optionsFile.add("-u" + options.effUser);
		}
		if (options.sqlOpts != null) {
			for (String opt : options.sqlOpts) {
				if ((opt != null) && (!opt.isEmpty())) {
					optionsFile.add(opt);
				}
			}
		}
		if (options.zc) {
			optionsFile.add("-zc");
		}
		if (options.zcpk) {
			optionsFile.add("-zcpk");
		}
		if (options.zdl) {
			optionsFile.add("-zdl");
		}
		if (options.zhex) {
			optionsFile.add("-zhex");
		}
		if (options.zn >= 0) {
			optionsFile.add(String.format("-zn%1$d",
					new Object[] { Integer.valueOf(options.zn) }));
		}
		if (options.zq) {
			optionsFile.add("-zq");
		}
		if ((options.oFilename != null) && (!options.oFilename.isEmpty())) {
			optionsFile.add("-o " + options.oFilename);
		}
		if (options.serverClass != null) {
			optionsFile.add(String.format("%1$s/%2$s", new Object[] { name,
					options.serverClass.getServerClass() }));
		} else {
			optionsFile.add(name);
		}
		if ((options.rTables != null) && (options.rTables.length != 0)) {
			for (StatdumpOptions.TableAndCols tabAndCols : options.rTables) {
				if ((tabAndCols != null) && (tabAndCols.rTable != null)) {
					optionsFile.add("-r" + tabAndCols.rTable);
					if ((tabAndCols.aCols != null)
							&& (tabAndCols.aCols.length != 0)) {
						for (String aCol : tabAndCols.aCols) {
							if ((aCol != null) && (!aCol.isEmpty())) {
								optionsFile.add("-a" + aCol);
							}
						}
					}
				}
			}
		}
		if ((options.xrTables != null) && (options.xrTables.length != 0)) {
			for (String xrTable : options.xrTables) {
				if ((xrTable != null) && (!xrTable.isEmpty())) {
					optionsFile.add("-xr" + xrTable);
				}
			}
		}
		optionsFile.close();

		return launchUtilityAsync(command, options.cwd, optionsFile, true);
	}

	public UtilityProcessHandle UTcopyDatabase(String name, CopyOptions options) {
		if (options == null) {
			options = new CopyOptions();
		}
		verifyAuthorizationForUtilityLaunch();

		List<String> command = new ArrayList();
		command.add(fullPathOf("copydb"));
		if ((options.paramFile != null) && (!options.paramFile.isEmpty())) {
			command.add("-param_file=" + options.paramFile);
			return launchUtilityAsync(command, options.cwd, null, true);
		}
		OptionsFile optionsFile = new OptionsFile(this);
		command.add("-param_file=" + optionsFile.getName());
		if (options.serverClass != null) {
			optionsFile.add(String.format("%1$s/%2$s", new Object[] { name,
					options.serverClass.getServerClass() }));
		} else {
			optionsFile.add(name);
		}
		if (options.printableData) {
			optionsFile.add("-c");
		}
		if ((options.effUser != null) && (!options.effUser.isEmpty())) {
			optionsFile.add("-u" + options.effUser);
		}
		if ((options.groupID != null) && (!options.groupID.isEmpty())) {
			optionsFile.add("-G" + options.groupID);
		}
		if (options.groupTableIndexes) {
			optionsFile.add("-group_tab_idx");
		}
		if (options.parallel) {
			optionsFile.add("-parallel");
		}
		if (options.journal) {
			optionsFile.add("-journal");
		}
		if (options.promptForPassword) {
			command.add("-P");
		}
		if ((options.source != null) && (!options.source.isEmpty())) {
			optionsFile.add("-source=" + options.source);
		}
		if ((options.dest != null) && (!options.dest.isEmpty())) {
			optionsFile.add("-dest=" + options.dest);
		}
		if ((options.outputDirectory != null)
				&& (!options.outputDirectory.isEmpty())) {
			optionsFile.add("-d" + options.outputDirectory);
		}
		if (options.withTables) {
			optionsFile.add("-with_tables");
		}
		if (options.withModify) {
			optionsFile.add("-with_modify");
		}
		if (options.noDependencyCheck) {
			optionsFile.add("-nodependency_check");
		}
		if (options.withData) {
			optionsFile.add("-with_data");
		}
		if (options.all) {
			optionsFile.add("-all");
		}
		if (options.orderCCM) {
			optionsFile.add("-order_ccm");
		}
		if (options.withIndex) {
			optionsFile.add("-with_index");
		}
		if (options.withConstraints) {
			optionsFile.add("-with_constr");
		}
		if (options.withViews) {
			optionsFile.add("-with_views");
		}
		if (options.withSynonyms) {
			optionsFile.add("-with_synonyms");
		}
		if (options.withEvents) {
			optionsFile.add("-with_events");
		}
		if (options.withProcedures) {
			optionsFile.add("-with_proc");
		}
		if (options.withRegistration) {
			optionsFile.add("-with_reg");
		}
		if (options.withRules) {
			optionsFile.add("-with_rules");
		}
		if (options.withAlarms) {
			optionsFile.add("-with_alarms");
		}
		if (options.withComments) {
			optionsFile.add("-with_comments");
		}
		if (options.withRoles) {
			optionsFile.add("-with_roles");
		}
		if (options.withSequences) {
			optionsFile.add("-with_sequences");
		}
		if (options.noSequences) {
			optionsFile.add("-no_seq");
		}
		if (options.withPermits) {
			optionsFile.add("-with_permits");
		}
		if (options.addDrop) {
			optionsFile.add("-add_drop");
		}
		if ((options.infile != null) && (!options.infile.isEmpty())) {
			optionsFile.add("-infile=" + options.infile);
		}
		if ((options.outfile != null) && (!options.outfile.isEmpty())) {
			optionsFile.add("-outfile=" + options.outfile);
		}
		if (options.relpath) {
			optionsFile.add("-relpath");
		}
		if (options.noint) {
			optionsFile.add("-noint");
		}
		if (options.noLoc) {
			optionsFile.add("-no_loc");
		}
		if (options.noPerm) {
			optionsFile.add("-no_perm");
		}
		if (options.noPersist) {
			optionsFile.add("-no_persist");
		}
		if (options.noRepMod) {
			optionsFile.add("-no_repmod");
		}
		if (options.noRep) {
			optionsFile.add("-no_rep");
		}
		if (options.noLogging) {
			optionsFile.add("-nologging");
		}
		if (options.online) {
			optionsFile.add("-online");
		}
		if (options.tables != null) {
			for (String table : options.tables) {
				if ((table != null) && (!table.isEmpty())) {
					optionsFile.add(table);
				}
			}
		}
		optionsFile.close();

		return launchUtilityAsync(command, options.cwd, optionsFile, true);
	}

	public UtilityProcessHandle UTunloadDatabase(String name,
			UnloadOptions options) {
		if (options == null) {
			options = new UnloadOptions();
		}
		verifyAuthorizationForUtilityLaunch();

		List<String> command = new ArrayList();
		command.add(fullPathOf("unloaddb"));
		if (options.serverClass != null) {
			command.add(String.format("%1$s/%2$s", new Object[] { name,
					options.serverClass.getServerClass() }));
		} else {
			command.add(name);
		}
		if (options.printableData) {
			command.add("-c");
		}
		if ((options.outputDirectory != null)
				&& (!options.outputDirectory.isEmpty())) {
			command.add("-d" + options.outputDirectory);
		}
		if ((options.source != null) && (!options.source.isEmpty())) {
			command.add("-source=" + options.source);
		}
		if ((options.dest != null) && (!options.dest.isEmpty())) {
			command.add("-dest=" + options.dest);
		}
		if (options.promptForPassword) {
			command.add("-P");
		}
		if ((options.effUser != null) && (!options.effUser.isEmpty())) {
			command.add(escapeForCmdLine("-u" + options.effUser));
		}
		if ((options.groupID != null) && (!options.groupID.isEmpty())) {
			command.add(escapeForCmdLine("-G" + options.groupID));
		}
		if (options.parallel) {
			command.add("-parallel");
		}
		if (options.journal) {
			command.add("-journal");
		}
		if (options.withSequences) {
			command.add("-with_sequences");
		}
		if (options.groupTableIndexes) {
			command.add("-group_tab_idx");
		}
		if (options.noRep) {
			command.add("-no_rep");
		}
		if (options.noLogging) {
			command.add("-nologging");
		}
		return launchUtilityAsync(command, options.cwd, null, true);
	}

	public UtilityProcessHandle UTauditDatabase(String name,
			AuditOptions options) {
		if (options == null) {
			options = new AuditOptions();
		}
		verifyAuthorizationForUtilityLaunch();

		List<String> command = new ArrayList();

		command.add(fullPathOf("dmfjsp"));
		command.add("auditdb");
		if (options.serverClass != null) {
			command.add(String.format("%1$s/%2$s", new Object[] { name,
					options.serverClass.getServerClass() }));
		} else {
			command.add(name);
		}
		if (options.systemCatalogs) {
			command.add("-a");
		}
		if (options.all) {
			command.add("-all");
		}
		if ((options.tableNames != null) && (options.tableNames.length != 0)) {
			command.add(escapeForCmdLine("-table="
					+ join(options.tableNames, ",")));
		}
		if (options.fileNames != null) {
			if (options.fileNames.length > 0) {
				command.add("-file=" + join(options.fileNames, ","));
			} else {
				command.add("-file");
			}
		}
		if ((options.beginDate != null) && (!options.beginDate.isEmpty())) {
			command.add("-b" + options.beginDate);
		}
		if ((options.endDate != null) && (!options.endDate.isEmpty())) {
			command.add("-e" + options.endDate);
		}
		if (options.checkpointNumber > 0) {
			command.add(String.format("#c%1$d",
					new Object[] { Integer.valueOf(options.checkpointNumber) }));
		}
		if ((options.iUsername != null) && (!options.iUsername.isEmpty())) {
			command.add(escapeForCmdLine("-i" + options.iUsername));
		}
		if (options.inconsistent) {
			command.add("-inconsistent");
		}
		if (options.wait) {
			command.add("-wait");
		}
		if ((options.effUser != null) && (!options.effUser.isEmpty())) {
			command.add(escapeForCmdLine("-u" + options.effUser));
		}
		return launchUtilityAsync(command, options.cwd, null, true);
	}
	
	/**
	 * InfoDB 操作
	 */
	public UtilityProcessHandle UTinfoDatabase(String name, InfoOptions options) {
		if (options == null) {
			options = new InfoOptions();
		}
		verifyAuthorizationForUtilityLaunch();

		List<String> command = new ArrayList();

		command.add(fullPathOf("dmfjsp"));
		command.add("infodb");
		if (name != null) {
			if (options.serverClass != null) {
				command.add(String.format("%1$s/%2$s", new Object[] { name,
						options.serverClass.getServerClass() }));
			} else {
				command.add(name);
			}
		}
		if (options.checkpointNumber >= 0) {
			if (options.checkpointNumber > 0) {
				command.add(String.format("#c%1$d", new Object[] { Integer
						.valueOf(options.checkpointNumber) }));
			} else {
				command.add("#c");
			}
		}
		if ((options.effUser != null) && (!options.effUser.isEmpty())) {
			command.add(escapeForCmdLine("-u" + options.effUser));
		}
		return launchUtilityAsync(command);
	}

	public UtilityProcessHandle UTvwinfoDatabase(String name,
			VWInfoOptions options) {
		if (options == null) {
			options = new VWInfoOptions();
		}
		verifyAuthorizationForUtilityLaunch();

		List<String> command = new ArrayList();
		if (options.launchIIvwinfo) {
			command.add(fullPathOf("iivwinfo"));
		} else {
			command.add(fullPathOf("vwinfo"));
		}
		if (options.stats) {
			command.add("--stats");
		}
		if (options.config) {
			command.add("--config");
		}
		if (options.tableBlockUse) {
			command.add("--table_block_use");
		}
		if (options.columnBlockUse) {
			command.add("--column_block_use");
		}
		if (options.openTransactions) {
			command.add("--open_transactions");
		}
		if ((options.effUser != null) && (!options.effUser.isEmpty())) {
			command.add("--user");
			command.add(escapeForCmdLine(options.effUser));
		}
		if (options.verbose) {
			command.add("--verbose");
		}
		if ((options.tablename != null) && (!options.tablename.isEmpty())) {
			command.add("--table");
			command.add(escapeForCmdLine(options.tablename));
		}
		if (options.serverClass != null) {
			command.add(String.format("%1$s/%2$s", new Object[] { name,
					options.serverClass.getServerClass() }));
		} else {
			command.add(name);
		}
		return launchUtilityAsync(command);
	}

	private static native void InitAPI();

	private static native void TermAPI();
	
	/**
	 * 分配一个ToolsAPI实例, Toolsapi DLL的外部进入点
	 * @return
	 */
	private static native long getToolsAPIInstance();

	private native String bootstrapCharsetName();

	private native void nativeUnloadInstallation();

	public native TransactionHandle autoCommitOn(
			ConnectionHandle paramConnectionHandle);

	public native void autoCommitOff(TransactionHandle paramTransactionHandle);

	public native void commit(TransactionHandle paramTransactionHandle);
	
	//回滚操作
	public native void rollback(TransactionHandle paramTransactionHandle);
	
	//
	private native ConnectionHandle connect(boolean paramBoolean,
			String paramString1, String paramString2, byte[] paramArrayOfByte,
			int paramInt, ConnectOptions paramConnectOptions);
	
	//断开数据库连接 jni call
	private native void nativeDisconnect(ConnectionHandle paramConnectionHandle);

	private native void nativeAbort(ConnectionHandle paramConnectionHandle);
	
	//执行数据库查询　jni call
	private native void nativeQuery(ConnectionHandle paramConnectionHandle,
			TransactionHandle paramTransactionHandle,
			StatementHandle paramStatementHandle, String paramString,
			boolean paramBoolean);
	
	//执行数据库存储过程, jni call
	public native StatementHandle executeProcedure(
			ConnectionHandle paramConnectionHandle,
			TransactionHandle paramTransactionHandle,
			IIapi.Descriptor[] paramArrayOfDescriptor,
			IIapi.DataValue[] paramArrayOfDataValue);

	public native void putParameters(StatementHandle paramStatementHandle,
			IIapi.Descriptor[] paramArrayOfDescriptor,
			IIapi.DataValue[] paramArrayOfDataValue);

	public native void cancel(StatementHandle paramStatementHandle);

	public native void close(StatementHandle paramStatementHandle);

	public native IIapi.Descriptor[] getDescriptors(
			StatementHandle paramStatementHandle);
	
	//获取所有行 jni call
	private native int getRows(StatementHandle paramStatementHandle,
			IIapi.Descriptor[] paramArrayOfDescriptor,
			IIapi.DataValue[][] paramArrayOfDataValue);

	public native IIapi.QueryInfo getQueryInfo(
			StatementHandle paramStatementHandle);

	public native String getEnv(String paramString);

	public native Map<String, String> NMsymbols();

	private native String nativeIDname();

	public native String IDname_service();

	public native IIapi.VersionInfo getVersionInfo();

	public native String getVersionString();

	public native IIapi.VersionInfo getAPIVersionInfo();

	public native String getAPIVersionString();

	public native void elevationRequired();

	public native void elevationRequiredWarning();

	public native boolean isElevationRequired();

	public native boolean checkPrivilege(String paramString,
			IIapi.Privileges paramPrivileges);

	public native void GCusrpwd(String paramString, byte[] paramArrayOfByte)
			throws IIapi.Exception;

	public native String GChostname(boolean paramBoolean);

	public native int GCtcpIpPort(String paramString, int paramInt);

	public native String CMgetCharsetName();

	public native String CMgetStdCharsetName();

	public native ContextHandle PMinit();

	public native String PMexpToRegExp(ContextHandle paramContextHandle,
			String paramString);

	public native void PMfree(ContextHandle paramContextHandle);

	public native String PMget(ContextHandle paramContextHandle,
			String paramString);

	public native String PMgetDefault(ContextHandle paramContextHandle,
			int paramInt);

	public native String PMhost(ContextHandle paramContextHandle);

	public native void PMload(ContextHandle paramContextHandle,
			String paramString);

	public native int PMnumElem(ContextHandle paramContextHandle,
			String paramString);

	public native void PMrestrict(ContextHandle paramContextHandle,
			String paramString);

	public native Map<String, String> PMscan(ContextHandle paramContextHandle,
			String paramString);

	public native void PMsetDefault(ContextHandle paramContextHandle,
			int paramInt, String paramString);

	public native void PMlowerOn(ContextHandle paramContextHandle);

	private native void nativePMinsert(ContextHandle paramContextHandle,
			String paramString1, String paramString2);

	private native void nativePMdelete(ContextHandle paramContextHandle,
			String paramString);

	private native void nativePMwrite(ContextHandle paramContextHandle,
			String paramString);

	private native void nativeCRsetPMval(ContextHandle paramContextHandle,
			String paramString1, String paramString2);

	public native String[] LGgetLogPaths(int paramInt, String paramString);
}
