package com.ingres;

//import com.ingres.annotations.DefaultValue;
//import com.ingres.annotations.Scriptable;
import java.io.Serializable;

public final class BackupOptions
  implements Serializable
{
  private static final long serialVersionUID = -8777701266567784448L;

  //@Scriptable
  public ServerClass serverClass = null;

  //@Scriptable
  public String effUser = null;

  //@Scriptable
  public boolean destroyAllPrevious = false;

  //@Scriptable
  public boolean enableJournaling = false;

  //@Scriptable
  public boolean disableJournaling = false;

  //@Scriptable
  public boolean lockDatabase = false;

  //@Scriptable
  //@DefaultValue("-1")
  public int locationsAtOnce = -1;

  public String[] tapeDevices = null;

  //@Scriptable
  public String[] tableNames = null;

  //@Scriptable
  public boolean verbose = false;

  //@Scriptable
  public boolean wait = false;

  //@Scriptable
  public String timeout = null;
}