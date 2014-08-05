package com.ingres;

//import com.ingres.annotations.DefaultValue;
//import com.ingres.annotations.Scriptable;
import java.io.Serializable;

public final class AlterOptions implements Serializable
{
  private static final long serialVersionUID = 1602428118063468043L;

  //@Scriptable
  public ServerClass serverClass = null;

  //@Scriptable
  public boolean disableJournaling = false;

  //@Scriptable
  public boolean deleteOldestCheckpoint = false;

  //@Scriptable
  //@DefaultValue("-1")
  public int initJournalBlocks = -1;

  //@Scriptable
  //@DefaultValue("-1")
  public int journalBlockSize = -1;

  //@Scriptable
  public boolean nextJournalFile = false;

  //@Scriptable
  //@DefaultValue("-1")
  public int targetJournalBlocks = -1;

  //@Scriptable
  //@DefaultValue("-1")
  public boolean deleteInvalidCheckpoints = false;

  public NormalizationForm normalization = null;

  public String unicodeCollationName = null;

  //@Scriptable
  //@DefaultValue("-1")
  public int keep = -1;

  public boolean disableMVCC = false;

  //@Scriptable
  public boolean enableMVCC = false;

  //@Scriptable
  public boolean enableMustlog = false;

  //@Scriptable
  public boolean disableMustlog = false;

  //@Scriptable
  public boolean verbose = false;

  public static enum NormalizationForm
  {
    C, 

    D;
  }
}