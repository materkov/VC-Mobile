package com.example.vcmobile.core;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.SortedMap;

import android.os.Environment;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class VideoLib {
	public HashMap<Integer, VCVideo> videosList;	// Список всех видео (id - описание)
	private final static String libPath = Environment.getExternalStorageDirectory() + "/VCMobile/DBCache/VideoLib.bin";
	
	public final static String INFO_FILENAME = "VideoLibSettings";
	public final static String LAST_SYNCH_DATE = "LastSynch";
	
	public synchronized boolean Init() {
		// Загрузить описание из файла
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(libPath));
			videosList = (HashMap<Integer, VCVideo>)ois.readObject();
			ois.close();
			
			if (!isCorrect()) {
				VCLog.write("VideoLib.Init: WARNING! isCorrect==false, creating new enmpty catalog!!!");
				videosList = new HashMap<Integer, VCVideo>();
				return false;
			}
			
			return true;
		}
		catch (Exception ex) {
			videosList = new HashMap<Integer, VCVideo>();
			return false;
		}
	}
	
	public boolean isCorrect() {
		for (VCVideo video : videosList.values()) {
			if (!video.isCorrect())
				return false;
		}
		
		return true;
	}
	
	public void Synchronize(int userId) throws LibSynchException {
		NetUtils utils = new NetUtils(userId);
		
		// Запрашиваем список всех видео
		SortedMap<Integer, Integer> serverList = utils.GetList();
		if (serverList == null) {
			VCLog.write("VideoLib.Synchronize. serverList == null");
			throw new LibSynchException();
		}
		
		// Формируем список того, что надо обновить
		Gson gson = new Gson();
		StringBuilder query = new StringBuilder();

		for (Integer videoId : serverList.keySet()) {
			if (!videosList.containsKey(videoId)) {
				// Смотрим, если id нету еще - добавляем
				query.append(videoId);
				query.append(' ');
			}
			else {
				// Если есть, но версия не та - обновляем
				if (videosList.get(videoId).version != serverList.get(videoId)) {
					query.append(videoId);
					query.append(' ');
				}
			}
		}
		
		if (query.length() != 0) {
			// Удалить последний пробел
			query.deleteCharAt(query.length()-1);
			
			// Делаем запрос всех описаний (разом)
			
			VCVideoJSON array[] = null;
			
			try {
				String arrayStr = utils.GetVideoDesc(query.toString());
				if (arrayStr == null) {
					VCLog.write("VideoLib.Synchronize. arrayStr == null");
					throw new LibSynchException();
				}
				
				array = gson.fromJson(arrayStr, VCVideoJSON[].class);
			}
			catch (JsonSyntaxException ex) {
				VCLog.write(ex, "VideoLib.Synchronize JsonSyntaxException1");
				throw new LibSynchException();
			}
			
			// Разбираем кждый объект
			for (int i = 0; i < array.length; i++) {
				if (!array[i].isCorrect()) {
					// Проверим каждый объект на корректность после парсинга GSON'ом
					VCLog.write("VideoLib.Synchronize isCorrect == false!");
					throw new LibSynchException();
				}
					
				int videoId = array[i].id;
				int ver = serverList.get(videoId);
				
				if (videosList.containsKey(videoId)) {
					// Если у нас уже есть видео с таким ID, обновляем описание и версию
					VCVideo video = videosList.get(videoId);
					video.version = ver;
					video.video = array[i];
				}
				else {
					// Если нет, добавляем новое видео
					videosList.put(array[i].id, new VCVideo(array[i], null, ver));
				}
			}
		}


		// Если у нас есть id которого уже нет на серваке - удаляем
		LinkedList<Integer> toDel = new LinkedList<Integer>();
		query.delete(0, query.length());	// Паралельно будем строить запрос на лайки, их запрашиваем у всех
		for (Integer videoId : videosList.keySet()) {
			if (!serverList.containsKey(videoId))
				toDel.add(videoId);
			else {
				// Те, которые не надо удалять - на те будем запрашивать
				query.append(videoId);
				query.append(' ');
			}
		}
		
		for (Integer videoId : toDel) {
			videosList.remove(videoId);
		}
		
		// Запрашиваем лайки+прсомотры для всех
		if (query.length() != 0) {
			query.deleteCharAt(query.length() - 1);
			
			String arrayStr = utils.GetVideoShortDesc(query.toString());
			if (arrayStr == null) {
				VCLog.write("VideoLib.Synchronize 2 arrayStr == null");
				throw new LibSynchException();
			}
			
			VCVideoShortJSON array[] = null;
			try {
				array = gson.fromJson(arrayStr, VCVideoShortJSON[].class);
			}
			catch (JsonSyntaxException ex) {
				VCLog.write(ex, "VideoLib.Synchronize JsonSyntaxException2");
				throw new LibSynchException();
			}
			
			for (VCVideoShortJSON shortVideo : array) {
				VCVideo video = videosList.get(shortVideo.id);
				if (video != null)
					videosList.get(shortVideo.id).videoShort = shortVideo;
				else
					throw new LibSynchException();
			}
		}
		
		// Запрашиваем список моих видео
		String[] myVideos = utils.GetMyVideos();
		if (myVideos == null) {
			VCLog.write("myVideos == null");
			throw new LibSynchException();
		}
		
		// Сначала установим всей коллекции что не мое, 
		// паралельно проверми корректность всех
		for (VCVideo video : videosList.values()) {
			if (!video.isCorrect())
				throw new LibSynchException();
			
			video.isMy = false;
		}
		
		try {
			for (String myVideo : myVideos) {
				VCVideo video = videosList.get(Integer.parseInt(myVideo));
				if (video != null) {
					video.isMy = true;
				}
				else
					VCLog.write("VideoLib.Synchronize. WARNING! Recieved id from server = " + myVideo + " and can't find it here!!!!!!!!!");
			}
		}
		catch (NumberFormatException ex) {
			VCLog.write(ex, "VideoLib.Synchronize. NumberFormatException. My Videos.");
			throw new LibSynchException();
		}
	}
	
	// Сохраняет списки на телефон
	public synchronized boolean SaveLists() {
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new FileOutputStream(libPath));
			
			oos.writeObject(videosList);
			oos.flush();
			
			return true;
		}
		catch (IOException ex) {
			VCLog.write(ex, "VideoLib._SaveList. IOException. path = [" + libPath + "].");
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
	
	public static class LibSynchException extends Exception { };
}
