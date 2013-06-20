package com.example.vcmobile.core;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;

import android.os.Environment;

public class VideoLibDownloads {
	public LinkedList<Integer> videoDownloadsList;		// Список всех скачанных видео
	private final static String libDownloadsPath = Environment.getExternalStorageDirectory() + "/VCMobile/DBCache/VideoLibDownloads.bin";
	
	public synchronized boolean Init() {
		// Загрузить описание из файла
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(libDownloadsPath));
			videoDownloadsList = (LinkedList<Integer>)ois.readObject();
			ois.close();
			
			return true;
		}
		catch (Exception ex) {
			videoDownloadsList = new LinkedList<Integer>();
			return false;
		}
	}
	
	public boolean SaveLists() {
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new FileOutputStream(libDownloadsPath));
			
			oos.writeObject(videoDownloadsList);
			oos.flush();
			
			return true;
		}
		catch (IOException ex) {
			VCLog.write(ex, "VideoLib._SaveList. IOException. path = [" + libDownloadsPath + "].");
			return false;
		}
		finally {
			try {
				if (oos != null) oos.close();
			} 
			catch (IOException e) {
			}
		}
	}
}
