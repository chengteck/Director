package com.ingres.discovery;

import com.ingres.ConnectionInfo;
import com.ingres.IIapi;
import com.ingres.IIapi.SysInfo;
import com.ingres.IIapi.VersionInfo;
import com.ingres.LocalIIapi;
import com.ingres.RemoteIIapi;
import com.ingres.exception.ConnectionException;
import com.ingres.server.RemoteCommand;
import com.ingres.server.ServerProperties;
import com.ingres.server.ServerProperties.Property;
import com.ingres.util.ClientStatistics;
import com.ingres.util.Environment;
import com.ingres.util.Intl;
import com.ingres.util.Logging;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.net.ServerSocketFactory;

public class DiscoverIngres
{
	private static List<Discovery.Info>	installations		= new ArrayList<Discovery.Info>();
	private static boolean				fixupMaster			= false;
	private static final byte[]			HTTP_GET			= { 71, 69, 84, 32 };
	private static final byte[]			HTTP_POST			= { 80, 79, 83, 84 };
	private static final byte[]			HTTP_HEAD			= { 72, 69, 65, 68 };
	private static final byte[]			HTTP_PUT			= { 80, 85, 84, 32 };
	private static final Pattern		instIdPattern		= Pattern.compile("[a-zA-Z][a-zA-Z0-9]"); //regular expression
	private static boolean				markedToTerminate	= false;
	private static boolean				endOfLife			= false;
	private static volatile boolean		notTheMaster		= false;
	private static volatile Thread		remoteThread		= null;
	private static Logging				logger				= null;
	private static IIapi				inst				= null;
	private static Discovery.Info		dInfo				= new Discovery.Info();
	private static long					ourPID				= 0L;
	private static boolean				verbose			 	= false;
	private static boolean				respondToHTTP;
	
	
	private static enum PollState
	{
		NORMAL, MASTER_TERMINATED, REMOTE_TERMINATED;

		private PollState()
		{
		}
	}

	private static boolean addInstallation(Discovery.Info newInfo)
	{
		int ix = installations.indexOf(newInfo);
		if (ix < 0)
		{
			installations.add(newInfo);
			logInstallationInfo("Adding new installation", newInfo);
			return true;
		}
		Discovery.Info oldInfo = (Discovery.Info) installations.get(ix);
		if ((oldInfo.remoteCmdPort == newInfo.remoteCmdPort)
				&& (oldInfo.isMaster == newInfo.isMaster))
		{
			return false;
		}
		installations.set(ix, newInfo);
		logInstallationInfo("Updating info for installation", newInfo);
		return true;
	}

	private static boolean removeInstallation(Discovery.Info info)
	{
		int existingIndex = installations.indexOf(info);
		if (existingIndex >= 0)
		{
			Discovery.Info existingInfo = (Discovery.Info) installations
					.get(existingIndex);
			if (existingInfo.remoteCmdPort == info.remoteCmdPort)
			{
				installations.remove(existingIndex);
				logInstallationInfo("Removing installation", existingInfo);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 
	 * @param info
	 * @param inst
	 * @param kill
	 * @return
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws ShortBufferException 
	 * @throws InvalidKeySpecException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws com.ingres.IIapi.Exception 
	 * @throws InvalidKeyException 
	 */
	private static int shutdownOurRemoteServer(Discovery.Info info, IIapi inst,
			boolean kill) throws InvalidKeyException, com.ingres.IIapi.Exception, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, ShortBufferException, IllegalBlockSizeException, BadPaddingException
	{
		logger.debug(
				"Client: shutdownOurRemoteServer: trying to connect on port %1$d",
				new Object[] { Integer.valueOf(info.remoteCmdPort) });
		int count = -1;
		RemoteIIapi remInst = new RemoteIIapi(new ConnectionInfo(info));
		try
		{
			if (remInst.connectToRemoteServer(true))
			{
				count = remInst.terminateServer(inst.IDname(), kill ? 1 : 0);
				if (count >= 1)
				{
					count--;
				}
			}
			remInst.unloadInstallation();
		} catch (ConnectException ce)
		{
			logger.warn("Couldn't connect to remote server: %1$s",
					new Object[] { ce.getLocalizedMessage() });
		} catch (IOException ioe)
		{
			throw new RuntimeException(ioe);
		}
		return count;
	}

	private static void printInstallationInfo(Discovery.Info info,
			boolean isCurrent, boolean afterFirst)
	{
		if (afterFirst)
		{
			System.out.println();
		}
		Intl.outFormat(
				Discovery.pkg,
				"discover.listInstall",
				new Object[] {
						info.info.IIinstallation,
						isCurrent ? Intl.getString(Discovery.pkg,
								"discover.current") : "",
						(info.isMaster) && (info.remoteCmdPort > 0) ? Intl
								.getString(Discovery.pkg, "discover.master")
								: "" });

		Intl.outFormat(Discovery.pkg, "discover.listIISystem",
				new Object[] { info.info.IIsystem });
		Intl.outFormat(Discovery.pkg, "discover.listVersion",
				new Object[] { info.info.version.toString() });
		Intl.outFormat(Discovery.pkg,
				info.remoteCmdPort > 0 ? "discover.listPortNumber"
						: "discover.listPortNone", new Object[] { Integer
						.valueOf(info.remoteCmdPort) });
		if (verbose)
		{
			Intl.outFormat(Discovery.pkg, "discover.listHostName",
					new Object[] { info.info.hostName });
			Intl.outFormat(Discovery.pkg, "discover.listHostFully",
					new Object[] { info.info.fullyQualifiedHostName });
			Intl.outFormat(Discovery.pkg, "discover.listHostAddr",
					new Object[] { info.hostAddress });
		}
	}

	private static void logInstallationInfo(String msg, Discovery.Info info)
	{
		logger.info(
				"%1$s:%n Installation code = %2$s %3$s%n\t II_SYSTEM = %4$s%n\t   version = %5$s%n      command port = %6$s%n",
				new Object[] {
						msg,
						info.info.IIinstallation,
						(info.isMaster) && (info.remoteCmdPort > 0) ? "(master)"
								: "",
						info.info.IIsystem,
						info.info.version.toString(),
						info.remoteCmdPort > 0 ? Integer
								.toString(info.remoteCmdPort) : "----" });
	}

	private static void waitForRemoteThreadDeath()
	{
		try
		{
			logger.debug("Waiting for remote thread %1$s to finish",
					new Object[] { remoteThread });
			if (remoteThread != null)
			{
				remoteThread.join(); //TODO
			}
		} catch (InterruptedException ie)
		{
			Logging.Except("Interrupted waiting to join remote command thread",
					ie);
		}
		logger.debug("Remote thread %1$s completed",
				new Object[] { remoteThread });
	}

	private static void reportException(Exception e, String error)
	{
		String message = e.getMessage();
		if (message == null)
		{
			message = e.getClass().getSimpleName();
		}
		System.err
				.format("%1$s: %2$s%n",
						new Object[] { Intl.getKeyString(Discovery.pkg, error),
								message });
		e.printStackTrace(System.err);
	}

	/**
	 * Find Instance
	 * 
	 * @return
	 */
	private static boolean findOurselves()
	{
		try
		{
			//初始化LocalIIapi
			inst = new LocalIIapi();
		} 
		catch (Exception e)
		{
			reportException(e, "%discover.errConnectLocal");
			return false;
		}

		// Can not find the instance, return
		if (inst == null)
		{
			Intl.outPrintln(Discovery.pkg, "discover.cannotConnect");
			return false;
		}

		// if find the instance and connect, get the sysinfo
		try
		{
			dInfo.info = inst.getSysInfo(); //获取系统信息
		} catch (Exception e2)
		{
			reportException(e2, "%discover.errSysInfo");
			return false;
		}
		dInfo.isMaster = true;

		//
		File ourIIsystem = Discovery.getOurIISystem();
		if (ourIIsystem == null)
		{
			Intl.outPrintln(Discovery.pkg, "discover.IIsystem");
			return false;
		}

		File instIIsystem = new File(dInfo.info.IIsystem);
		if (!instIIsystem.equals(ourIIsystem))
		{
			Intl.outFormat(
					Discovery.pkg,
					"discover.pathMismatch",
					new Object[] { ourIIsystem.getPath(),
							instIIsystem.getPath() });

			return false;
		}
		return true;
	}

	/**
	 * perform shutdown server action
	 * @param kill
	 * @return
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws ShortBufferException 
	 * @throws InvalidKeySpecException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws com.ingres.IIapi.Exception 
	 * @throws InvalidKeyException 
	 */
	private static int doShutdown(boolean kill) throws InvalidKeyException, com.ingres.IIapi.Exception, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, ShortBufferException, IllegalBlockSizeException, BadPaddingException
	{
		int count = -1;
		if (Discovery.serverIsRunning() > 0L)
		{
			for (Discovery.Info info : Discovery.getLocalInstallations())
			{
				if (info.equals(dInfo))
				{
					if (info.isMaster)
					{
						logger.debug("Client: shutting down the master server (ourselves)", new Object[0]);
						count = Discovery.shutdownServer(kill);
						break;
					}
					count = shutdownOurRemoteServer(info, inst, kill);

					break;
				}
			}
		} 
		else
		{
			dInfo.remoteCmdPort = RemoteCommand.getActualListenPort(inst);
			if (dInfo.remoteCmdPort > 0)
			{
				count = shutdownOurRemoteServer(dInfo, inst, kill);
			}
		}
		return count;
	}

	private static void doList(String[] args)
	{
		boolean anyFound = false;
		boolean afterFirst = false;
		for (int i = 1; i < args.length; i++)
		{
			if ((args[i] != null) && (!args[i].isEmpty()))
			{
				if (anyFound)
				{
					System.out.println();
				}
				anyFound = true;
				afterFirst = false;
				Intl.outFormat(Discovery.pkg, "discover.listHeadRemote",
						new Object[] { args[i] });
				try
				{
					for (Discovery.Info info : Discovery
							.getInstallations(args[i]))
					{
						printInstallationInfo(info, false, afterFirst);
						afterFirst = true;
					}
				} catch (ConnectionException ce)
				{
					Intl.outFormat(Discovery.pkg, "discover.errorConnect",
							new Object[] { args[i], ce.getMessage() });
				}
			}
		}
		if (!anyFound)
		{
			Intl.outPrintln(Discovery.pkg, "discover.listHeadLocal");
			boolean foundCurrent = false;
			if (Discovery.serverIsRunning() > 0L)
			{
				for (Discovery.Info info : Discovery.getLocalInstallations())
				{
					boolean isCurrent = info.equals(dInfo);
					if (isCurrent)
					{
						foundCurrent = true;
					}
					printInstallationInfo(info, isCurrent, afterFirst);
					afterFirst = true;
				}
			}
			if (!foundCurrent)
			{
				dInfo.remoteCmdPort = RemoteCommand.getActualListenPort(inst);
				printInstallationInfo(dInfo, true, afterFirst);
			}
		}
	}

	private static void doHelp()
	{
		Intl.printHelp(Discovery.pkg, "discover");
	}
    
	/**
	 * 
	 * @param args
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws ShortBufferException 
	 * @throws InvalidKeySpecException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws com.ingres.IIapi.Exception 
	 * @throws InvalidKeyException 
	 */
	private static void processClientCommands(String[] args) throws InvalidKeyException, com.ingres.IIapi.Exception, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, ShortBufferException, IllegalBlockSizeException, BadPaddingException
	{
		List<String> argList = new ArrayList<String>();
		for (String arg : args)
		{
			if (arg.startsWith("--"))
			{
				arg = arg.substring(2);
			} 
			else if ((arg.startsWith("-")) || (arg.startsWith("/")))
			{
				arg = arg.substring(1);
			}
			
			if (arg.equalsIgnoreCase("verbose"))
			{
				verbose = true;
			} 
			else
			{
				argList.add(arg);
			}
		}
		
		//閸涙垝鎶ら崝銊ㄧ槤閿涘苯宓嗛幍褑顢戞禒锟芥稊鍫熸惙娴ｆ粣绱濇俊淇eatedb
		String cmdVerb = (String) argList.get(0);
		logger.debug("Client: processing '%1$s' command...", new Object[] { cmdVerb });
		
		//婢跺嫮鎮�"shutdown"閹达拷"stop"閸涙垝鎶�
		if ((cmdVerb.equalsIgnoreCase("shutdown")) || (cmdVerb.equalsIgnoreCase("stop")))
		{
			int count = doShutdown(false);
			if (count < 0)
			{
				Intl.outFormat(Discovery.pkg, "discover.cannotFindStop",
						new Object[] { dInfo.info.IIinstallation });

				System.exit(2);
			} 
			else
			{
				Intl.outPrintln(Discovery.pkg, "discover.stopping");
				if (count != 0)
				{
					if (count == 1)
					{
						Intl.outPrintln(Discovery.pkg, "discover.oneConnected");
					} 
					else if (count > 1)
					{
						Intl.outFormat(Discovery.pkg, "discover.manyConnected",
								new Object[] { Integer.valueOf(count) });
					}
					Intl.outPrintln(Discovery.pkg, "discover.useKill");
					System.exit(1);
				}
			}
		} 
		else if (cmdVerb.equalsIgnoreCase("kill")) //婢跺嫮鎮�"kill"閸涙垝鎶�
		{
			doShutdown(true);
			Intl.outPrintln(Discovery.pkg, "discover.killed");
		} 
		else if (cmdVerb.equalsIgnoreCase("list")) //婢跺嫮鎮�"list"閸涙垝鎶�
		{
			doList((String[]) argList.toArray(args));
		} 
		else if (cmdVerb.equalsIgnoreCase("isrunning")) 
		{
			long pid = Discovery.serverIsRunning();
			logger.debug("The master discovery server PID = %1$d", new Object[] { Long.valueOf(pid) });
			boolean isRunning = pid > 0L;
			String id = pid == 1L ? "<unknown>" : String.valueOf(pid);
			System.out.format("%1$s\t%2$s%n", new Object[] { isRunning ? "true" : "false", id });
			System.exit(isRunning ? 0 : 1);
		} 
		else if (cmdVerb.equalsIgnoreCase("isremoterunning"))
		{
			int remotePort = RemoteCommand.getActualListenPort(inst);
			logger.debug("Our remote command port = %1$d", new Object[] { Integer.valueOf(remotePort) });
			System.out.println(remotePort > 0 ? "true" : "false");
			System.exit(remotePort > 0 ? 0 : 1);
		} 
		else if (cmdVerb.equalsIgnoreCase("start")) //handle "start" command
		{
			int remotePort = RemoteCommand.getActualListenPort(inst);
			if (remotePort <= 0)
			{
				Intl.outPrintln(Discovery.pkg, "discover.startBackground");
				File mgmtsvrPath = PathUtilities.getUtilityPath(new File(dInfo.info.IIsystem), "iimgmtsvr");
				if (mgmtsvrPath == null)
				{
					Intl.errPrintln(Discovery.pkg, "discover.serverPath");
				} 
				else
				{
					ProcessBuilder pb = new ProcessBuilder(new String[] {mgmtsvrPath.getPath(), dInfo.info.IIinstallation, "mgmtsvr" });
					try
					{
						pb.start();
					} 
					catch (IOException ioe)
					{
						Intl.errFormat(Discovery.pkg, "discover.exceptionStart", new Object[] { ioe.getMessage() });
					}
				}
			} 
			else
			{
				Intl.outPrintln(Discovery.pkg, "discover.alreadyRunning");
				dInfo.remoteCmdPort = remotePort;
				printInstallationInfo(dInfo, true, false);
			}
		} 
		else if (cmdVerb.equalsIgnoreCase("version"))
		{
			System.out.println(inst.getAPIVersionString());
		} 
		else if ((cmdVerb.equalsIgnoreCase("help")) || (cmdVerb.equals("?")))
		{
			doHelp();
		} 
		else
		{
			Intl.outFormat(Discovery.pkg, "discover.unknownCommand",
					new Object[] { cmdVerb });
			doHelp();
		}
	}
	
	
	private static PollState pollMasterServer()
	{
		double value = Math.random();
		long millis = (long) (value * 120000.0D + 60000.0D);
		boolean interrupted = false;
		try
		{
			Thread.sleep(millis);
		} catch (InterruptedException ie)
		{
			logger.debug("pollMasterServer sleep was interrupted",
					new Object[0]);
			interrupted = true;
		}
		if ((remoteThread != null)
				&& ((!RemoteCommand.isRunning()) || (RemoteCommand
						.isMarkedToTerminate())))
		{
			return PollState.REMOTE_TERMINATED;
		}
		if (interrupted)
		{
			return PollState.MASTER_TERMINATED;
		}
		List<Discovery.Info> currentInstallations = Discovery
				.getLocalInstallations();
		if ((currentInstallations == null) || (currentInstallations.isEmpty()))
		{
			return PollState.MASTER_TERMINATED;
		}
		installations = currentInstallations;

		boolean needRegistration = false;
		int ourIndex = installations.indexOf(dInfo);
		if (ourIndex < 0)
		{
			needRegistration = true;
		} else
		{
			Discovery.Info info = (Discovery.Info) installations.get(ourIndex);
			if (info.remoteCmdPort != dInfo.remoteCmdPort)
			{
				needRegistration = true;
			}
		}
		if (needRegistration)
		{
			dInfo.isMaster = false;
			Discovery.registerInstallation(dInfo);
		}
		return PollState.NORMAL;
	}

	private static void redoMasterList(Discovery.Info info)
	{
		Discovery.Info oldMaster = null;
		boolean foundOurselves = false;
		for (Discovery.Info di : installations)
		{
			if (di.equals(info))
			{
				di.isMaster = true;
				foundOurselves = true;
			} else if (di.isMaster)
			{
				oldMaster = di;
			} else
			{
				di.isMaster = false;
			}
		}
		if (oldMaster != null)
		{
			removeInstallation(oldMaster);
		}
		if (!foundOurselves)
		{
			info.isMaster = true;
			addInstallation(info);
		}
	}
	
	/**
	 * 娴ｆ粈璐熼張宥呭鏉╂劘顢�
	 * @param servsock
	 * @return
	 */
	private static boolean runAsServer(final ServerSocket servsock)//@modify the param to final
	{
		new Thread(new Runnable() {
			public void run()
			{
				long pid = Discovery.serverIsRunning();
				DiscoverIngres.logger.debug("background master check thread, received pid=%1$d, ourPID=%2$d",
								new Object[] { Long.valueOf(pid), Long.valueOf(DiscoverIngres.ourPID) });

				if (pid != DiscoverIngres.ourPID)
				{
					//TODO:
					
					//DiscoverIngres.access$202(true);
					DiscoverIngres.logger.debug("background master check thread, pids did not agree, closing server socket", new Object[0]);
					try
					{
						//this.varservsock.close();
						servsock.close();
					} catch (IOException ignore)
					{
						
					}
				}
			}
		}).start();

		while ((!endOfLife) && (!notTheMaster))
		{
			try
			{
				logger.debug("Waiting for client request...", new Object[0]);
				//閹恒儱褰堢�广垺鍩涚粩顖濈箾閹恒儴顕Ч锟�
				Socket sock = servsock.accept();
				logger.debug("Accepted connection from: %1$s",
						new Object[] { sock.getRemoteSocketAddress() });

				InputStream inputStream = sock.getInputStream();
				DataInputStream is = new DataInputStream(inputStream);
				OutputStream outputStream = sock.getOutputStream();
				DataOutputStream os = new DataOutputStream(outputStream);

				byte[] sig = new byte[4];
				is.readFully(sig);
				logger.debug("Received incoming signature %1$s",
						new Object[] { Arrays.toString(sig) });
				if (Arrays.equals(sig, Discovery.SIGNATURE))
				{
					String cmd = null;
					try
					{
						while ((cmd = is.readUTF()) != null)
						{
							logger.debug("Command = %1$s", new Object[] { cmd });
							if (cmd.equalsIgnoreCase("Register"))
							{
								Discovery.Info newInfo = Discovery.Info
										.readObject(is);

								newInfo.isMaster = false;
								addInstallation(newInfo);
								newInfo = null;
							} else if (cmd.equalsIgnoreCase("Unregister"))
							{
								Discovery.Info newInfo = Discovery.Info
										.readObject(is);
								removeInstallation(newInfo);
								newInfo = null;
							} else if (cmd.equalsIgnoreCase("Request"))
							{
								//鏉╂柨娲栫�瑰顥婄�圭偘绶ラ崚妤勩�冮崚鏉款吂閹撮顏�
								logger.debug(
										"Sending list of %1$d installation(s) to client...",
										new Object[] { Integer
												.valueOf(installations.size()) });
								for (Discovery.Info info : installations)
								{
									info.write(os);
								}
								os.writeUTF("");
								os.flush();
								logger.debug(
										"Finished sending installation list.",
										new Object[0]);
							} else if ((cmd.equalsIgnoreCase("Shutdown"))
									|| (cmd.equalsIgnoreCase("Kill")))
							{
								logger.debug(
										"Setting 'terminate' flag for remote command server",
										new Object[0]);
								int flags = 0;
								if (cmd.equalsIgnoreCase("Kill"))
								{
									flags = 1;
								}
								int count = RemoteCommand.setTerminate(flags);
								markedToTerminate = true;

								servsock.setSoTimeout(1000);

								logger.debug(
										"At termination there are %1$d clients connected.",
										new Object[] { Integer.valueOf(count) });
								os.writeInt(count);
								os.flush();
							} else if (cmd.equalsIgnoreCase("IsRunning"))
							{
								logger.debug(
										"IS_RUNNING sending master server PID = %1$d",
										new Object[] { Long.valueOf(ourPID) });
								os.writeLong(ourPID);
								os.flush();
							} else
							{
								if (cmd.equalsIgnoreCase("Close"))
								{
									logger.debug("Closing connection...",
											new Object[0]);
									break;
								}
								logger.error("Command '%1$s' not recognized!",
										new Object[] { cmd });
							}
						}
					} catch (IOException ioe2)
					{
						logger.except("While receiving commands", ioe2);
					}
					os.close();
					is.close();
				} else
				{
					if ((Arrays.equals(sig, HTTP_GET))
							|| (Arrays.equals(sig, HTTP_PUT))
							|| (Arrays.equals(sig, HTTP_POST))
							|| (Arrays.equals(sig, HTTP_HEAD)))
					{
						is.skip(is.available());
						if (respondToHTTP)
						{
							logger.debug("Sending HTTP response", new Object[0]);

							os.writeBytes("HTTP/1.0 200 OK\r\nServer: ");
							String hostName = dInfo.info.fullyQualifiedHostName == null ? dInfo.info.hostName
									: dInfo.info.fullyQualifiedHostName;

							os.writeBytes(hostName);
							String title = Intl.formatString(Discovery.pkg,
									"discover.listHeadRemote",
									new Object[] { hostName });
							os.writeBytes("\r\n\r\n<!DOCTYPE html><html><head><title>");
							os.writeBytes(title);
							os.writeBytes("</title><meta http-equiv=\"refresh\" content=\"300\"/>");
							os.writeBytes("<style type=\"text/css\"><!-- ");
							os.writeBytes("H2 {color:blue} table { border-collapse: collapse; width: 100% } ");
							os.writeBytes("table, td, th { border: 2px solid blue; padding 4px; font-family:\"Trebuchet MS\", Arial, Helvetica, sans-serif; } ");
							os.writeBytes("th { background-color:#00bfff; color: black } ");
							os.writeBytes("tr.alt td { background-color: #87cefa } ");
							os.writeBytes(" --></style></head><body><h2>");
							os.writeBytes(title);
							os.writeBytes("</h2>");
							os.writeBytes("<table border=\"1\" cellpadding=\"4\"><tr><th>");
							os.writeBytes(Intl.getString(Discovery.pkg,
									"discover.httpInstance"));
							os.writeBytes("</th><th>");
							os.writeBytes(Intl.getString(Discovery.pkg,
									"discover.httpIISystem"));
							os.writeBytes("</th><th>");
							os.writeBytes(Intl.getString(Discovery.pkg,
									"discover.httpVersion"));
							os.writeBytes("</th><th>");
							os.writeBytes(Intl.getString(Discovery.pkg,
									"discover.httpPort"));
							os.writeBytes("</th><th>");
							os.writeBytes(Intl.getString(Discovery.pkg,
									"discover.httpMaster"));
							os.writeBytes("</th></tr>");
							boolean alt = false;
							for (Discovery.Info info : installations)
							{
								os.writeBytes(alt ? "<tr class=\"alt\"><td>"
										: "<tr><td>");
								os.writeBytes(info.info.IIinstallation);
								os.writeBytes("</td><td>");
								os.writeBytes(info.info.IIsystem);
								os.writeBytes("</td><td>");
								os.writeBytes(info.info.version.toString());
								os.writeBytes("</td><td>");
								os.writeBytes(Integer
										.toString(info.remoteCmdPort));
								os.writeBytes("</td><td>");
								os.writeBytes(info.isMaster ? Intl
										.getString(Discovery.pkg,
												"discover.httpMasterYes")
										: "---");
								os.writeBytes("</td></tr>");
								alt = !alt;
							}
							os.writeBytes("</table></body></html>");
						} else
						{
							logger.info(
									"Returning HTTP \"Bad Request\" response",
									new Object[0]);
							os.writeBytes("HTTP/1.0 400 Bad Request\r\n\r\n");
						}
						os.flush();
						os.close();
					} else
					{
						logger.error("Unknown signature received: %1$s",
								new Object[] { Arrays.toString(sig) });
					}
					sig = null;
					is.close();
				}
			} catch (SocketTimeoutException ste)
			{
				if ((markedToTerminate) && (remoteThread != null)
						&& (!RemoteCommand.isRunning()))
				{
					logger.debug("Setting 'endOfLife' to shut ourselves down",
							new Object[0]);
					endOfLife = true;
				}
			} catch (IOException ioe)
			{
				logger.except("I/O Exception receiving commands", ioe);
			}
		}
		try
		{
			servsock.close();
		} catch (Exception ignore)
		{
		}
		logger.debug("End of life reached for master server, shutting down.",
				new Object[0]);

		return endOfLife;
	}

	private static void signalMasterShutdown()
	{
		for (Discovery.Info info : installations)
		{
			if (!info.equals(dInfo))
			{
				logger.debug(
						"Sending master shutdown signal to installation '%1$s' on port %2$d",
						new Object[] { info.info.IIinstallation,
								Integer.valueOf(info.remoteCmdPort) });
				if (RemoteCommand.signalMasterShutdown(info.remoteCmdPort))
				{
					break;
				}
			}
		}
	}
	
	/**
	 * 程序入口main()函数
	 * @param args
	 * @throws IOException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws ShortBufferException 
	 * @throws InvalidKeySpecException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws com.ingres.IIapi.Exception 
	 * @throws InvalidKeyException 
	 */
	public static void main(String[] args) throws IOException, InvalidKeyException, com.ingres.IIapi.Exception, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, ShortBufferException, IllegalBlockSizeException, BadPaddingException
	{	
		Locale locale = Locale.getDefault();
		Intl.initResources(IIapi.pkg, locale);
		Intl.initResources(Discovery.pkg, locale);
		Intl.initResources(RemoteCommand.pkg, locale);
		
		//加载toolapijni.dll，初始化inst
		if (!findOurselves())
		{
			System.exit(1);
		}
		
		Map<String, String> symbols = inst.NMsymbols(); //This is a native method.
		try
		{
			InputStream ris = DiscoverIngres.class.getClassLoader()
					.getResourceAsStream("com/ingres/discovery/logging.properties");
			Logging.readConfiguration(ris, symbols);
			ris.close();
		} catch (IOException ioe1)
		{
			Intl.errFormat(Discovery.pkg, "discover.configLogging",
					new Object[] { ioe1.getMessage() });
			System.exit(2);
		}
		logger = new Logging("discovery");


		int numToSkip = 0;
		if (args.length > 0)
		{
			while (numToSkip < args.length)
			{
				Matcher m = instIdPattern.matcher(args[numToSkip]); 
				if (m.matches())
				{
					numToSkip++;
				} 
				else
				{
					if ((!args[numToSkip].equalsIgnoreCase("mgmtsvr")) && (!args[numToSkip].equalsIgnoreCase("iimgmtsvr")))
					{
						break;
					}
					numToSkip++;
				}
			}
		}
		
		ourPID = Environment.getProcessID();
		if (args.length > numToSkip)
		{
			processClientCommands((String[]) Arrays.copyOfRange(args, numToSkip, args.length));
			return;
		}
		int remotePort = RemoteCommand.getActualListenPort(inst);
		if (remotePort > 0)
		{
			Intl.outPrintln(Discovery.pkg, "discover.alreadyRunning");
			return;
		}
		
		if (!ServerProperties.load(symbols))
		{
			return;
		}
		respondToHTTP = ServerProperties.getBooleanValue(ServerProperties.Property.RespondToHTTP, true);

		Intl.outPrintln(Discovery.pkg, "discover.startAsServer");

        //创建守护线程，守护线程负责维护BlockingQueue<Thread>线程队列
		logger.debug("Trying to start RemoteCommand thread...", new Object[0]);
		remoteThread = RemoteCommand.startThread(inst, dInfo, Thread.currentThread());

		ServerSocketFactory factory = ServerSocketFactory.getDefault();
		ServerSocket servsock = null;
		for (;;)
		{
			boolean weAreFirst = true;

			logger.info("Running in server mode, process id = %1$d", new Object[] { Long.valueOf(ourPID) });

			long masterServerPID = Discovery.serverIsRunning();
			if ((masterServerPID >= 0L) && (masterServerPID != ourPID))
			{
				logger.debug("Found that discovery server is already running.", new Object[0]);
				weAreFirst = false;
			} 
			else
			{
				try
				{
					logger.debug("Trying to listen on main port %1$d...", new Object[] { Integer.valueOf(16902) });
					
					//閸掓稑缂撻張宥呭閸ｃ劎顏琒ocket, 閻╂垵鎯夐崚棰佸瘜缁旑垰褰�16902
					servsock = factory.createServerSocket(16902);
					weAreFirst = servsock.isBound();//閼惧嘲褰囬惄鎴濇儔閻樿埖锟斤拷
				} catch (IOException ioe)
				{
					weAreFirst = false;
				}
			}
			
			//閺堫亣鍏橀幋鎰閻╂垵鎯�16902缁旑垰褰�
			if (!weAreFirst)
			{
				//缁旑垰褰涚悮顐㈠窗閻㈩煉绱濇径姘嚋鐎瑰顥婄�圭偘绶ラ惃鍕剰閸愶拷
				logger.info(
						"Server listen port is in use -- Registering ourselves and becoming a remote server only", new Object[0]);
				
				//閺嶅洨銇氭稉娲姜娑撶粯婀囬崝鈽呯礉娑撶粯婀囬崝鈥茶礋缁楊兛绔存稉顏勭暔鐟佸懎鐤勬笟瀣剁礋
				dInfo.isMaster = false;
				Discovery.registerInstallation(dInfo);

				installations = Discovery.getLocalInstallations();

				PollState state = PollState.NORMAL;
				do
				{
					state = pollMasterServer();
				} 
				while (state == PollState.NORMAL);
				
				if (state == PollState.MASTER_TERMINATED)
				{
					fixupMaster = true;
				} 
				else
				{
					waitForRemoteThreadDeath();

					Discovery.unregisterInstallation(dInfo);

					logger.debug("RemoteCommand thread has finished, terminating ourselves...", new Object[0]);
					return;
				}
			} 
			else
			{
				//閹存劕濮涢惄鎴濇儔16902缁旑垰褰�
				dInfo.isMaster = true;
				addInstallation(dInfo);
				if (fixupMaster)
				{
					redoMasterList(dInfo);
					fixupMaster = false;
				}
				
				if (runAsServer(servsock))
				{
					break;
				}
				notTheMaster = false;
			}
		}
		if (ClientStatistics.currentClientCount() != 0)
		{
			waitForRemoteThreadDeath();
		} 
		else
		{
			RemoteCommand.cleanup();
		}
		signalMasterShutdown();
	}
}