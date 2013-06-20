package com.example.vcmobile.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Random;

import junit.framework.Assert;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import com.example.vcmobile.DownloadsActivity;
import com.example.vcmobile.ImageLoader;
import com.example.vcmobile.VideoListActivity;
import com.example.vcmobile.VideoPageActivity;

import de.greenrobot.event.EventBus;

public class UploadService extends NetService {
	public final static int NOTIFICATION_ID = 147326; // random number
	
	public final static String BROADCAST_ABORT_ACTION = "com.example.vcmobile.upload.abort";
	public final static String BROADCAST_PAUSE_ACTION = "com.example.vcmobile.upload.pause";
	public final static String BROADCAST_RESUME_ACTION = "com.example.vcmobile.upload.resume";
	
	public final static String BROADCAST_SERVICE_FINISHED_ACTION = "com.example.vcmobile.upload.progress.finish";
	public final static String BROADCAST_SERVICE_ACTION = "com.example.vcmobile.upload";
	
	private EventBus bus;
	
	public UploadService() {
		super("UploadService", NOTIFICATION_ID,
				BROADCAST_SERVICE_ACTION,
				BROADCAST_SERVICE_FINISHED_ACTION,
				BROADCAST_ABORT_ACTION,
				BROADCAST_PAUSE_ACTION, 
				BROADCAST_RESUME_ACTION);
		
		bus = EventBus.getDefault();
	}
	
	// Class for transfering
	public static class Progress {
		public int progress;
		public String name;
		public int videoSize;
		public boolean isDoing;
		
		public Progress(boolean isDoing, int progress, String name, int videoSize) {
			this.isDoing = isDoing;
			this.progress = progress;
			this.name = name;
			this.videoSize = videoSize;
		}
	}
		
	public static class Finished { }
	
	private String name;
	private NetUtils utils;
	private int videoId;
	private String videoDesc;
	
	@Override
	protected void onHandleIntent(Intent intent) {
		String uriStr = intent.getStringExtra("uri");
		String path = intent.getStringExtra("path");
		name = intent.getStringExtra("name");
		videoDesc = intent.getStringExtra("desc");
		
		utils = new NetUtils(VkApp.GetUserID(this));
		
		Assert.assertTrue((uriStr != null) || (path != null));
		Assert.assertNotNull(name);
		
		VCLog.write("UploadService started. Uploading uriStr = [" + uriStr + "], path = [" + path + "].");
		
		String title = "Выгрузка \"" + name + "\"";
		RegisterBroadcast();
		
		//ReportStarted
		if (!isAborted) bus.postSticky(new Progress(true, 0, name, 0));
		ShowStartNotification(title, DownloadsActivity.class, title);
		
		try {
			Random rand = new Random();
			String SDPath = null;
	
			// Копируем на SD карту
			SDPath = Environment.getExternalStorageDirectory()
					+ "/VCMobile/UploadTmp/" + rand.nextInt(Integer.MAX_VALUE);

			if (uriStr != null) {
				VCLog.write("UploadService. Copying frim uri to [" + SDPath + "]");
				
				try {
					Uri uri = Uri.parse(uriStr);
					InputStream is = getContentResolver().openInputStream(uri);
					String type = getContentResolver().getType(uri);
					OutputStream os = new FileOutputStream(SDPath);
					int length = ImageLoader.CopyStream(is, os);
					if (!isAborted) bus.postSticky(new Progress(true, 0, name, length));
					is.close();
					os.close();
				} catch (FileNotFoundException e) {
					throw new FileMovingException();
				} catch (IOException e) {
					throw new FileMovingException();
				}
			}
			else {
				File fOld = new File(path);
				File fNew = new File(SDPath);
				
				VCLog.write("UploadService. Moving from path to [" + SDPath + "]");
				fOld.renameTo(fNew);
				
				if (!isAborted) bus.postSticky(new Progress(true, 0, name, (int)fOld.length()));
			}
   
			// Выгрузка
			VCLog.write("UploadService. Starting uploading parts");
			DoUploading(SDPath);
			
			// Объединить все части в одну
			VCLog.write("UploadService. Merging parts");
			utils.MergeVideoFile(videoId);
			
			
			if (uriStr != null) {
				// Удалить временный файл если с камеры
				File f = new File(SDPath);
				f.delete();
			}
			else {
				// Переместить обратно если с карты
				File fOld = new File(path);
				File fNew = new File(SDPath);
				
				fNew.renameTo(fOld);
			}
			 
			if (!isAborted)	bus.postSticky(new Progress(true, 99, name, videoSize));
			
			// Заносим локально в список видео на телефоне (синхронизируем с сервером, в фоне все это)
			VideoLib lib = new VideoLib();
			lib.Init();
			try {
				lib.Synchronize(VkApp.GetUserID(this));
				lib.SaveLists();
			}
			catch (VideoLib.LibSynchException ex) {
				VCLog.write(ex, "UploadService.onHandleIntent(). LibSynchException WARNING!");
			}
			
			// Отсылаем броадкаст списку что надо обновится
			sendBroadcast(new Intent(VideoListActivity.UPDATE_BROADCAST));
			
			Bundle b = new Bundle();
			b.putInt("id", videoId);
			b.putString("title", name);
			
			ShowFinishNotification("Видео \"" + name + "\" выгружено на сервер", 
					name,
					"Видео выгружено.", VideoPageActivity.class, b);
		}
		catch (UploadErrorException ex) {
			VCLog.write("UploadService. UploadErrorException");
			
			ShowFinishNotification("Ошибка при выгрузке видео", 
					name,
					"Не удалось выгрузить видео.", DownloadsActivity.class, null);
		}
		catch (FileMovingException ex) {
			VCLog.write("UploadService. FileMovingException");
			
			ShowFinishNotification("Ошибка при выгрузке видео", 
					name,
					"Не удалось выгрузить видео.", DownloadsActivity.class, null);
		}
		catch (UploadAbortedException ex) {
			VCLog.write("UploadService ABORTED! videoName = [" + uriStr + "]");
			
			// Если прервано, протсо выходим по-тихому
		}
		finally {
			bus.removeStickyEvent(Progress.class);
			bus.post(new Finished());
			
			UnregisterBroadcast();
			VCLog.write("UploadService finished. uriStr = [" + uriStr + "], path = [" + path + "].");
		}
	}
	  
	private void DoUploading(String filePath) throws UploadErrorException, UploadAbortedException {
		RandomAccessFile file = null;
		
		try {
			// Открываем файл
			file = new RandomAccessFile(filePath, "r");
			videoSize = (int)file.length();
			int num_parts = (int)Math.ceil((double)file.length() / (double)NetUtils.UPLOAD_PART_SIZE);
			
			// Получаем ID
			videoId = utils.UploadVideoBegin(videoDesc, file.length());
			if (videoId == -1)
				throw new UploadErrorException();
			
			for (int i = 0; i < num_parts; i++) {
				double progress = (double)i / (double)(num_parts+1) * 100.0;
				NotifyProgress((int)Math.round(progress));
				//ReportProgress((int)Math.round(progress));
				if (!isAborted)
					bus.postSticky(new Progress(!isPaused, (int)Math.round(progress), name, videoSize));
				
				// Ждем, если стоим на паузе
				synchronized (this) {
					while (isPaused) { 
						try {
							wait();
						}
						catch (InterruptedException ex)	{ }
					}
					
					if (isAborted) throw new UploadAbortedException();
				}
				
				// Читаем кусок файла
				long offset = i*NetUtils.UPLOAD_PART_SIZE;
				file.seek(offset);
				
				int bufSize = 0;
				
				if (i == num_parts-1)
					//bufSize = (int)file.length() - i*NetUtils.UPLOAD_PART_SIZE; // На последней части надо уменьшенный размер
					bufSize = (int)file.length() % NetUtils.UPLOAD_PART_SIZE;
				else
					bufSize = NetUtils.UPLOAD_PART_SIZE;
				
				byte buffer[] = new byte[bufSize];
				int res = file.read(buffer);
				
				boolean result = utils.UploadVideoPart(videoId, i, buffer);
				if (!result) {
					VCLog.write("UploadService. Error uploading part #" + i + ", buffer size = " + bufSize);
					VCLog.write("Pausing service...");
					
					Pause();	// Чтоб уведомлени послались!
					//synchronized (this) { isPaused = true; }
					
					ShowNotification("Ошибка вызагруки видео \"" + name + "\"",
							name,
							"Произошла ошибка. Поставлено на паузу.",
							DownloadsActivity.class,
							null);
	
					i--;	// Чтобы ту же часть еще раз
					continue;
				}
			}

		}
		catch (FileNotFoundException ex) {
			throw new UploadErrorException();
		}
		catch (IOException ex) {
			throw new UploadErrorException();
		}
		finally {
			try {
				if (file != null) file.close();
			} 
			catch (IOException e) {
			}
		}
	}
	
	static class UploadAbortedException extends Exception { }
	static class UploadErrorException extends Exception { }
	static class FileMovingException extends Exception { }
	
	@Override
	protected void DoPause() {
		if (!isAborted)	bus.postSticky(new Progress(false, progress, name, videoSize));
	}

	@Override
	protected void DoResume() {
		if (!isAborted) bus.postSticky(new Progress(true, progress, name, videoSize));
	}

	@Override
	protected void DoAbort() {
		bus.removeStickyEvent(Progress.class);
		//abortForeground();
		bus.post(new Finished());
	}
}
