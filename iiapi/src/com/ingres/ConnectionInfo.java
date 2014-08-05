package com.ingres;

import com.ingres.discovery.Discovery;
import com.ingres.discovery.Discovery.Info;
import com.ingres.util.CharUtil;
import com.ingres.util.Environment;
import com.ingres.util.Logging;
import java.util.Date;
import java.util.List;

public class ConnectionInfo
{
  public static List<Discovery.Info> localInstalls = null;
  private Discovery.Info dInfo;
  private InstallationName installationName;
  private boolean local;
  private boolean single = false;

  private boolean firstUpdate = true;

  private String userName = null;

  private byte[] password = null;

  private byte[] dbmsPassword = null;

  private AuthenticationType authenticationType = AuthenticationType.CURRENT_USER;

  private SessionAuth sessionAuth = null;

  private Date date = null;

  public ConnectionInfo()
  {
    this.local = true;
    this.dInfo = null;
  }

  public ConnectionInfo(String name)
  {
    this.installationName = new InstallationName(name);
    this.local = isLocalMachine();
    this.userName = Environment.currentUser();
  }

  public ConnectionInfo(Discovery.Info dInfo)
  {
    this(dInfo.toShortName());
    this.dInfo = dInfo;
    this.userName = Environment.currentUser();
  }

  public ConnectionInfo(InstallationName name, boolean local, String userName, AuthenticationType type)
  {
    this.installationName = name;
    this.local = local;
    this.userName = userName;
    this.authenticationType = type;
  }

  public InstallationName getInstallationName()
  {
    return this.installationName;
  }

  public Discovery.Info getDiscoveryInfo()
  {
    return this.dInfo;
  }

  public void setDiscoveryInfo(Discovery.Info dInfo)
  {
    this.dInfo = dInfo;
  }

  public boolean getLocal()
  {
    return this.local;
  }

  public void setLocal(boolean local)
  {
    this.local = local;
  }

  public void setSingle(boolean single)
  {
    this.single = single;
  }

  public String getUserName()
  {
    return this.userName;
  }

  public void setUserName(String userName)
  {
    this.userName = userName;
    this.sessionAuth = null;
  }

  public byte[] getPassword()
  {
    return this.password;
  }

  public void setPassword(byte[] password)
  {
    this.password = ((password != null) && (password.length == 0) ? null : password);
    this.sessionAuth = null;
  }

  public byte[] getDBMSPassword()
  {
    return this.dbmsPassword;
  }

  public void setDBMSPassword(byte[] dbmsPassword)
  {
    this.dbmsPassword = ((dbmsPassword != null) && (dbmsPassword.length == 0) ? null : dbmsPassword);
    this.sessionAuth = null;
  }

  public AuthenticationType getAuthenticationType()
  {
    return this.authenticationType;
  }

  public void setAuthenticationType(AuthenticationType authenticationType)
  {
    this.authenticationType = authenticationType;
  }

	public SessionAuth getSessionAuth() {
		if (this.sessionAuth == null) {
			// switch
			// (1.$SwitchMap$com$ingres$AuthenticationType[this.authenticationType.ordinal()])
			// {
			switch (this.authenticationType.ordinal()) {
			case 1:
				this.sessionAuth = SessionAuth.getAuth();
				break;
			case 2:
				if (this.dbmsPassword != null)
					this.sessionAuth = SessionAuth.getAuth(this.userName,
							this.password, this.dbmsPassword);
				else
					this.sessionAuth = SessionAuth.getAuth(this.userName,
							this.password);
				break;
			}
		}
		return this.sessionAuth;
	}

  public Date getDate()
  {
    return this.date;
  }

  public void setDate(Date date)
  {
    this.date = date;
  }

  public ServerType getServerType()
  {
    return this.local ? ServerType.LOCAL : ServerType.REMOTE;
  }

  public boolean isLocalMachine()
  {
    return this.installationName.isLocalMachine();
  }

  public String toString()
  {
    StringBuilder builder = new StringBuilder();

    builder.append(String.format("Installation: %s\n", new Object[] { getInstallationName() }));
    builder.append(String.format("User Name: %s\n", new Object[] { getUserName() }));
    builder.append(String.format("Authentication Type: %s\n", new Object[] { getAuthenticationType() == AuthenticationType.ANOTHER_USER ? "Another User" : "Current User" }));

    return builder.toString();
  }

  public boolean equals(Object obj)
  {
    boolean equal = false;
    if ((obj instanceof ConnectionInfo)) {
      ConnectionInfo info = (ConnectionInfo)obj;
      equal = (this.installationName.equals(info.installationName)) && (this.local == info.local) && (((this.userName == null) && (info.userName == null)) || ((this.userName != null) && (this.userName.equals(info.userName)) && (((this.password == null) && (info.password == null)) || ((this.password != null) && (this.password.equals(info.password)) && (((this.dbmsPassword == null) && (info.dbmsPassword == null)) || ((this.dbmsPassword != null) && (this.dbmsPassword.equals(info.dbmsPassword)) && (this.authenticationType.equals(info.authenticationType))))))));
    }

    return equal;
  }

  public int hashCode()
  {
    return this.installationName.hashCode() ^ Boolean.valueOf(this.local).hashCode() ^ (this.userName == null ? 0 : this.userName.hashCode()) ^ (this.password == null ? 0 : this.password.hashCode()) ^ (this.dbmsPassword == null ? 0 : this.dbmsPassword.hashCode()) ^ this.authenticationType.hashCode();
  }

  public boolean isSameInstallation(ConnectionInfo info)
  {
    return (this.installationName.equals(info.installationName)) && (this.local == info.local) && (((this.userName == null) && (info.userName == null)) || ((this.userName != null) && (this.userName.equals(info.userName)) && (this.authenticationType.equals(info.authenticationType))));
  }

  public String getDisplayName()
  {
    return this.installationName.getDisplayName(this.local, this.single);
  }

  public String toSerializableForm()
  {
    StringBuilder buf = new StringBuilder();
    CharUtil.appendToCSV(this.installationName.toString(), buf);
    CharUtil.appendToCSV(Boolean.toString(this.local), buf);
    CharUtil.appendToCSV(this.authenticationType.toString(), buf);
    CharUtil.appendToCSV(this.userName, buf);
    buf.append(this.dInfo.toSerializableForm());
    return buf.toString();
  }

  public static ConnectionInfo fromSerializableForm(String serial)
  {
    StringBuilder buf = new StringBuilder(serial);
    InstallationName iName = new InstallationName(CharUtil.getFromCSV(buf));
    boolean local = Boolean.parseBoolean(CharUtil.getFromCSV(buf));
    AuthenticationType type = AuthenticationType.valueOf(CharUtil.getFromCSV(buf));
    String user = CharUtil.getFromCSV(buf);
    ConnectionInfo info = new ConnectionInfo(iName, local, user, type);
    info.dInfo = Discovery.Info.fromSerializableForm(buf.toString());
    return info;
  }

  public static List<Discovery.Info> getLocalInstallations(boolean refresh)
  {
    if ((localInstalls == null ^ refresh)) {
      localInstalls = Discovery.getLocalInstallations();
    }
    return localInstalls;
  }

  public Status findInstallation(boolean refresh)
    throws Throwable
  {
    List<Info> installs = null;
    InstallationName iName = this.installationName;
    String hostName = "local";

    if (this.local) {
      installs = getLocalInstallations(refresh);
    }
    else {
      hostName = iName.getServerName();
      installs = Discovery.getInstallations(hostName);
    }

    if ((installs == null) || (installs.size() == 0)) {
      return Status.NO_INSTALLS;
    }

    String installationCode = iName.getInstallationCode();

    if (installs.size() == 1) {
      Discovery.Info di = (Discovery.Info)installs.get(0);
      String foundInstallationCode = di.info.IIinstallation;

      if ((installationCode == null) || (installationCode.isEmpty()) || (installationCode.equalsIgnoreCase(foundInstallationCode)))
      {
        this.dInfo = di;
        return Status.OK;
      }

      return Status.NOT_FOUND_SINGLE;
    }

    if ((installationCode == null) || (installationCode.isEmpty()))
    {
      return Status.EMPTY_CODE;
    }

    for (Discovery.Info di : installs) {
      try {
        if (di.info.IIinstallation.equalsIgnoreCase(installationCode)) {
          this.dInfo = di;
          return Status.OK;
        }
      }
      catch (Throwable ex) {
        throw ex;
      }

    }

    return Status.NOT_FOUND_MULTIPLE;
  }

  public boolean update()
  {
    Discovery.Info oldInfo = this.dInfo;
    try {
      Status status = findInstallation(true);
      if (status == Status.OK)
      {
        if (((oldInfo == null) && (this.dInfo != null)) || (!oldInfo.deepEquals(this.dInfo))) {
          return true;
        }

        boolean ret = this.firstUpdate;
        this.firstUpdate = (!this.firstUpdate);
        return ret;
      }
    }
    catch (Throwable ex)
    {
      Logging.Except("ConnectionInfo.update error", ex);
    }

    return false;
  }

  public static enum Status
  {
    OK, 

    NO_INSTALLS, 

    EMPTY_CODE, 

    NOT_FOUND_SINGLE, 

    NOT_FOUND_MULTIPLE;
  }
}