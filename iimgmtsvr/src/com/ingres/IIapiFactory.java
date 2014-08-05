package com.ingres;

import com.ingres.discovery.Discovery;
import com.ingres.discovery.Discovery.Info;
import com.ingres.util.Intl;
import com.ingres.util.Logging;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;

public class IIapiFactory {

	private static String getConnectMessage(ConnectionInfo ci,
			String messageKey, String exceptionMsg) {
		String name = ci.getDisplayName();
		int port = ci.getDiscoveryInfo().remoteCmdPort;
		String message = Intl.getString(IIapi.pkg, messageKey);
		return Intl.formatString(IIapi.pkg, "factory.msgInstancePort",
				new Object[] { message, name, Integer.valueOf(port),
						exceptionMsg });
	}

	public static IIapi getInstance(ConnectionInfo ci) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, ShortBufferException, IllegalBlockSizeException, BadPaddingException
   {
     IIapi localInst = null;
     if (ci.getServerType() == ServerType.LOCAL)
     {
       File localPath = Discovery.getOurIISystem();
       if (localPath != null) {
         try
         {
           localInst = new LocalIIapi(ci.getSessionAuth());
         }
         catch (Throwable e)
         {
           Logging.Except("Loading local installation", e);
         }
       }
       Discovery.Info dInfo = ci.getDiscoveryInfo();
       if (dInfo == null) {
         return localInst;
       }
       if (localInst != null)
       {
         IIapi.SysInfo localInfo = localInst.getSysInfo();
         if (dInfo.info.equals(localInfo)) {
           return localInst;
         }
         localInst.unloadInstallation();
       }
     }
     RemoteIIapi remoteInst = new RemoteIIapi(ci);
     do
     {
       try
       {
         if (remoteInst.connectToRemoteServer()) {
           return remoteInst;
         }
       }
       catch (IIapi.Exception apiex)
       {
         Logging.Except("Doing connect to remote instance: ", apiex);
         remoteInst.unloadInstallation();
         throw apiex;
       }
       catch (ConnectException ce)
       {
         do
         {
           remoteInst.unloadInstallation();
         } while (ci.update());
         Logging.Except("Could not connect to remote instance: ", ce);
         String msg = getConnectMessage(ci, "factory.couldNotConnect", ce.getMessage());
         throw new IIapi.Exception(0, 0, ce.getMessage(), msg, null);
       }
       catch (IOException ioe)
       {
         remoteInst.unloadInstallation();
     Logging.Except("Connecting to remote instance: ", ioe);
     String errorMsg = ioe.getMessage();
     if ((ioe instanceof UnknownHostException)) {
       errorMsg = Intl.formatString(IIapi.pkg, "factory.unknownHost", new Object[] { errorMsg });
     }
     String msg = getConnectMessage(ci, "factory.errorConnect", errorMsg);
     throw new IIapi.Exception(0, 0, errorMsg, msg, null);
       }
     } while (ci.update());
	  /*Logging.Except("Connecting to remote instance: ", ioe);
		String errorMsg = ioe.getMessage();
		if ((ioe instanceof UnknownHostException)) {
		errorMsg = Intl.formatString(IIapi.pkg, "factory.unknownHost", new Object[] { errorMsg });
		}
		String msg = getConnectMessage(ci, "factory.errorConnect", errorMsg);
		throw new IIapi.Exception(0, 0, errorMsg, msg, null);*/

     remoteInst.unloadInstallation();
     return null;
   }
}
