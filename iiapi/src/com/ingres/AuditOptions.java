package com.ingres;

//import com.ingres.annotations.DefaultValue;
//import com.ingres.annotations.Scriptable;
import java.io.Serializable;

public final class AuditOptions
  implements Serializable
{
  private static final long serialVersionUID = 8944713015766235326L;

  //@Scriptable
  public String cwd = null;

  //@Scriptable
  public ServerClass serverClass = null;

  //@Scriptable
  public String effUser = null;

  //@Scriptable
  public boolean systemCatalogs = false;

  //@Scriptable
  public boolean all = false;

  //@Scriptable
  public String[] tableNames = null;

  //@Scriptable
  public String[] fileNames = null;

  //@Scriptable
  public String beginDate = null;

  //@Scriptable
  public String endDate = null;

  //@Scriptable
  //@DefaultValue("-1")
  public int checkpointNumber = -1;

  //@Scriptable
  public String iUsername = null;

  //@Scriptable
  public boolean inconsistent = false;

  //@Scriptable
  public boolean wait = false;
}