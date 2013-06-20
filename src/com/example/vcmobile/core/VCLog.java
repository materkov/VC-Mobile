package com.example.vcmobile.core;

public class VCLog {
	private static final String LogTag = "VCMobile";
	private static final boolean writeDebug = true;
	
	public static void write(String msg) {
		if (writeDebug)
			android.util.Log.d(LogTag, msg);
	}
	
	public static void write(Exception ex, String msg) {
		if (writeDebug) {
			android.util.Log.d(LogTag, msg, ex);
		}
	}
}
