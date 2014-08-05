package com.ingres.discovery;

import com.ingres.IIapi;
import com.ingres.IIapi.SysInfo;
import com.ingres.IIapi.VersionInfo;
import com.ingres.exception.ConnectionException;
import com.ingres.util.CharUtil;
import com.ingres.util.Intl;
import com.ingres.util.Logging;
import com.ingres.util.NetworkUtil;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Discovery 监测安装实例信息；远程管理远程实例；
 * @author ingres
 */
public class Discovery {
	public static final int SERVER_LISTEN_PORT = 16902; //iimgmtsvr服务监听端口
	public static final String REGISTER = "Register";
	public static final String UNREGISTER = "Unregister";
	public static final String REQUEST = "Request";
	public static final String SHUTDOWN = "Shutdown";
	public static final String KILL = "Kill";
	public static final String IS_RUNNING = "IsRunning";
	public static final String CLOSE = "Close";
	private static final int LOCAL_TIMEOUT = 500;
	private static final int REMOTE_TIMEOUT = 2000;
	public static final Package pkg = Discovery.class.getPackage();

//-----------------------------------------------------------------------------------------
	
	/**
	 * 
	 * @author Ingres
	 * @comment zhengxb
	 */
	public static class Info {
		public IIapi.SysInfo info = null;
		public int remoteCmdPort = -1;
		public boolean isMaster = false; //第一个iimgmtsvr实例作为主服务
		public String hostAddress = null;

		public boolean equals(Object o) {
			if ((o != null) && ((o instanceof Info))) {
				Info di = (Info) o;
				if (di.info.equals(this.info)) {
					return true;
				}
			}
			return false;
		}

		public boolean deepEquals(Object o) {
			return (equals(o))
					&& (this.remoteCmdPort == ((Info) o).remoteCmdPort);
		}

		public int hashCode() {
			return this.info.hashCode();
		}

		public String toString() {
			return this.info.toString();
		}

		public String toShortName() {
			return String.format("%1$s/%2$s", new Object[] {
					this.info.hostName, this.info.IIinstallation });
		}

		public void write(DataOutputStream os) throws IOException {
			os.writeUTF(this.info.IIinstallation);
			os.writeUTF(this.info.IIsystem);
			os.writeUTF(this.info.hostName);
			if (this.info.fullyQualifiedHostName == null) {
				os.writeUTF("");
			} else {
				os.writeUTF(this.info.fullyQualifiedHostName);
			}
			os.writeUTF(this.info.version.toString());
			os.writeInt(this.remoteCmdPort);
			os.writeBoolean(this.isMaster);
		}

		public static Info readObject(DataInputStream is) throws IOException {
			String inst = is.readUTF();
			if ((inst == null) || (inst.isEmpty())) {
				return null;
			}
			Info newInfo = new Info();
			newInfo.info = new IIapi.SysInfo();
			newInfo.info.IIinstallation = inst;
			newInfo.info.IIsystem = is.readUTF();
			newInfo.info.hostName = is.readUTF();
			newInfo.info.fullyQualifiedHostName = is.readUTF();
			if (newInfo.info.fullyQualifiedHostName.isEmpty()) {
				newInfo.info.fullyQualifiedHostName = null;
			}
			newInfo.info.version = new IIapi.VersionInfo(is.readUTF());
			newInfo.remoteCmdPort = is.readInt();
			newInfo.isMaster = is.readBoolean();
			return newInfo;
		}

		public String toSerializableForm() {
			StringBuilder buf = new StringBuilder();
			CharUtil.appendToCSV(this.info.IIinstallation, buf);
			CharUtil.appendToCSV(this.info.IIsystem, buf);
			CharUtil.appendToCSV(this.info.hostName, buf);
			CharUtil.appendToCSV(this.info.fullyQualifiedHostName, buf);
			CharUtil.appendToCSV(this.hostAddress, buf);
			CharUtil.appendToCSV(this.info.version.toString(), buf);
			CharUtil.quoteForCSV(String.valueOf(this.remoteCmdPort), buf);
			return buf.toString();
		}

		public static Info fromSerializableForm(String serial) {
			StringBuilder buf = new StringBuilder(serial);
			Info newInfo = new Info();
			newInfo.info = new IIapi.SysInfo();
			newInfo.info.IIinstallation = CharUtil.getFromCSV(buf);
			newInfo.info.IIsystem = CharUtil.getFromCSV(buf);
			newInfo.info.hostName = CharUtil.getFromCSV(buf);
			newInfo.info.fullyQualifiedHostName = CharUtil.getFromCSV(buf);
			newInfo.hostAddress = CharUtil.getFromCSV(buf);
			newInfo.info.version = new IIapi.VersionInfo(
					CharUtil.getFromCSV(buf));
			newInfo.remoteCmdPort = Integer.valueOf(CharUtil.getFromCSV(buf))
					.intValue();
			return newInfo;
		}
		
		public boolean isLocalConnection() {
			if (this.hostAddress != null) {
				return NetworkUtil.isLocalMachine(this.hostAddress);
			}
			if (this.info.fullyQualifiedHostName != null) {
				return NetworkUtil.isLocalMachine(this.info.fullyQualifiedHostName);
			}
			return NetworkUtil.isLocalMachine(this.info.hostName);
		}
	}

//--------------------------------------------------------------------------------------------
	
	public static final byte[] SIGNATURE = { 53, 94, 36, -13 };
	
	/**
	 * 停止Server
	 * @param kill
	 * @return
	 */
	public static int shutdownServer(boolean kill) {
		int count = -1;
		try {
			Socket sock = NetworkUtil.localConnect(16902, 500, 500);
			OutputStream outputStream = sock.getOutputStream();
			DataOutputStream os = new DataOutputStream(outputStream);
			InputStream inputStream = sock.getInputStream();
			DataInputStream is = new DataInputStream(inputStream);
			os.write(SIGNATURE);
			os.writeUTF(kill ? "Kill" : "Shutdown");
			os.flush();
			count = is.readInt();
			os.writeUTF("Close");
			os.flush();
			is.close();
			os.close();
		} catch (IOException ioe) {
			String msg = String.format(
					"I/O Exception sending '%1$s' command: ",
					new Object[] { kill ? "Kill" : "Shutdown" });
			Logging.Except(msg, ioe);
		}
		return count;
	}

	public static List<Info> getLocalInstallations() {
		try {
			return getInstallations();
		} catch (ConnectionException cex) {
			Logging.Except("Getting installations list", cex);
		}
		return new ArrayList();
	}
	
	/**
	 * 获取本地安装实例
	 * @return
	 * @throws ConnectionException
	 */
	public static List<Info> getInstallations() throws ConnectionException {
		return getInstallations("localhost");
	}
	
	/**
	 * 获取安装实例
	 * @param remoteHostName
	 * @return
	 * @throws ConnectionException
	 */
	public static List<Info> getInstallations(String remoteHostName)
			throws ConnectionException {
		try {
			List<Info> installs = new ArrayList();
			Socket sock = NetworkUtil.isLocalMachine(remoteHostName) ? NetworkUtil
					.localConnect(16902, 500, 500) : NetworkUtil.connect(
					remoteHostName, 16902, 2000, 2000);

			InetAddress addr = sock.getInetAddress();
			String hostAddress = addr.getHostAddress();
			OutputStream outputStream = sock.getOutputStream();
			DataOutputStream os = new DataOutputStream(outputStream);
			InputStream inputStream = sock.getInputStream();
			DataInputStream is = new DataInputStream(inputStream);
			os.write(SIGNATURE);
			os.writeUTF("Request");
			os.flush();
			for (;;) {
				Info dInfo = Info.readObject(is);
				if (dInfo == null) {
					break;
				}
				dInfo.hostAddress = hostAddress;
				installs.add(dInfo);
			}
			os.writeUTF("Close");
			os.flush();
			is.close();
			os.close();
			return installs;
		} catch (UnknownHostException uhe) {
			throw new ConnectionException(Intl.formatString(pkg,
					"discovery.getInstallationsHost",
					new Object[] { remoteHostName }), uhe);
		} catch (IOException ioe) {
			throw new ConnectionException(Intl.formatString(pkg,
					"discovery.getInstallations", new Object[] {
							remoteHostName, ioe }), ioe);
		}
	}
	
	/**
	 * 
	 * @param dInfo
	 */
	public static void registerInstallation(Info dInfo) {
		try {
			Socket sock = NetworkUtil.localConnect(16902, 500, 500);
			OutputStream outputStream = sock.getOutputStream();
			DataOutputStream os = new DataOutputStream(outputStream);
			os.write(SIGNATURE);
			os.writeUTF("Register");
			dInfo.write(os);
			os.writeUTF("Close");
			os.flush();
			os.close();
		} catch (IOException ioe) {
			String msg = String.format(
					"I/O Exception sending '%1$s' command: ",
					new Object[] { "Register" });
			Logging.Except(msg, ioe);
		}
	}
	
	public static void unregisterInstallation(Info dInfo) {
		try {
			Socket sock = NetworkUtil.localConnect(16902, 500, 500);
			OutputStream outputStream = sock.getOutputStream();
			DataOutputStream os = new DataOutputStream(outputStream);
			os.write(SIGNATURE);
			os.writeUTF("Unregister");
			dInfo.write(os);
			os.writeUTF("Close");
			os.flush();
			os.close();
		} catch (IOException ioe) {
			String msg = String.format(
					"I/O Exception sending '%1$s' command: ",
					new Object[] { "Unregister" });
			Logging.Except(msg, ioe);
		}
	}
    
	/**
	 * 服务是否正在运行
	 * @return
	 */
	public static long serverIsRunning() {
		long pid = -1L;
		Socket sock = null;
		try {
			//监听16902端口的socket
			sock = NetworkUtil.localConnect(16902, 500, 1000);
			if (sock.isConnected()) {
				OutputStream outputStream = sock.getOutputStream();
				DataOutputStream os = new DataOutputStream(outputStream);
				InputStream inputStream = sock.getInputStream();
				DataInputStream is = new DataInputStream(inputStream);

				os.write(SIGNATURE);
				os.writeUTF("IsRunning");
				os.writeUTF("Close");
				os.flush();
				pid = is.readLong();
				is.close();
				os.close();
				sock = null;
			}
			return pid;
		} 
		catch (SocketTimeoutException ste) {
			Logging.Debug(
					"SocketTimeoutException during 'serverIsRunning', server is not running.",
					new Object[0]);
		} 
		catch (EOFException ee) {
			Logging.Debug(
					"EOF exception during 'serverIsRunning', probably an old server.",
					new Object[0]);

			pid = 1L;
		} 
		catch (IOException ioe) {
			Logging.Debug(
					"Other I/O exception during 'serverIsRunning', should never happen: %1$s",
					new Object[] { ioe.getMessage() });
		} 
		finally 
		{
			if (sock != null)
			{
				try
				{
					sock.close();
				} 
				catch (IOException ignore) 
				{
				}
			}
			return pid;
		}
	}

	/**
	 * 获取IISystem路径
	 * 
	 * @return
	 */
	public static File getOurIISystem() {
		File ourIIsystem = PathUtilities.getIISystem();
		if (ourIIsystem == null) {
			String path = System.getenv("II_SYSTEM");
			if (path != null) {
				ourIIsystem = new File(path);
			}
		}
		if (ourIIsystem != null) {
			if (!ourIIsystem.exists()) {
				ourIIsystem = null;
			} else {
				try {
					ourIIsystem = ourIIsystem.getCanonicalFile();
				} catch (IOException ignore) {
				}
			}
		}
		return ourIIsystem;
	}
}
