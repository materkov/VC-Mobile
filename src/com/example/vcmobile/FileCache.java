package com.example.vcmobile;

import java.io.File;

import android.content.Context;

public class FileCache {
	private File cacheDir;
	
	public FileCache(Context context){
		//Find the dir to save cached images
		cacheDir = new File(android.os.Environment.getExternalStorageDirectory(), "VCMobile/PreviewCache/");
		
		if (!cacheDir.exists())
			cacheDir.mkdirs();
	}
	
	public File getFile(String url){
		File f = new File(cacheDir, url);
		return f;
	}
	
	public void clear(){
		File[] files = cacheDir.listFiles();
		if (files == null)
			return;
		
		for (File f : files)
			f.delete();
	}
}
