package com.atomjack.sdscan;

import android.util.Log;

public class Logger
{
  private static final String TAG = "SDScan";

  public static void i(String format, Object ... args)
  {
    String msg = String.format(format, args);
    Log.i(TAG, msg);
  }

  public static void d(String format, Object ... args)
  {
    String msg = String.format(format, args);
    Log.d(TAG, msg);
  }

  public static void w(String format, Object ... args)
  {
    String msg = String.format(format, args);
    Log.w(TAG, msg);
  }

  public static void e(String format, Object ... args)
  {
    String msg = String.format(format, args);
    Log.e(TAG, msg);
  }
}
