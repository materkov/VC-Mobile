package com.example.vcmobile.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import junit.framework.Assert;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;

import com.example.vcmobile.DownloadsActivity;
import com.example.vcmobile.VideoPageActivity;

import de.greenrobot.event.EventBus;

public class DownloadService extends NetService {
	public final static int NOTIFICATION_ID = 432843; // random number
	
	public final static String BROADCAST_ABORT_ACTION = "com.example.vcmobile.download.abort";
	public final static String BROADCAST_PAUSE_ACTION = "com.example.vcmobile.download.pause";
	public final static String BROADCAST_RESUME_ACTION = "com.example.vcmobile.download.resume";
	
	public final static String BROADCAST_SERVICE_FINISHED_ACTION = "com.example.vcmobile.download.progress.finish";
	public final static String BROADCAST_SERVICE_ACTION = "com.example.vcmobile.download";

	private EventBus bus;
	
	public DownloadService() {
		super("DownloadService", NOTIFICATION_ID,
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
		public int videoId;
		public int videoSize;
		public boolean isDoing;
		
		public Progress(boolean isDoing, int progress, int videoId, int videoSize) {
			this.isDoing = isDoing;
			this.progress = progress;
			this.videoId = videoId;
			this.videoSize = videoSize;
		}
	}
	
	public static class Finished {
		public int videoId;
		
		public Finished(int videoId) {
			this.videoId = videoId;
		}
	}
	
	@Override
	protected void DoPause() {
		if (isAborted) bus.postSticky(new Progress(false, progress, videoId, videoSize));
	}
	
	@Override
	protected void DoAbort() {
		bus.removeStickyEvent(Progress.class);
		//abortForeground();
		bus.post(new Finished(videoId));
	}
	
	@Override
	protected void DoResume() {
		if (isAborted) bus.postSticky(new Progress(true, progress, videoId, videoSize));
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		videoId = intent.getIntExtra("id", -1);
		videoTitle = intent.getStringExtra("title");
		
		Assert.assertNotNull(videoTitle);
		Assert.assertTrue(videoId > 0);
		
		VCLog.write("DownloadService started. videoId = [" + videoId + "]");
		
		String title = "Загружается \"" + videoTitle + "\"";
		RegisterBroadcast();
		
		// ReportStarted
		if (!isAborted) bus.postSticky(new Progress(true, 0, videoId, videoSize));
		ShowStartNotification(title, DownloadsActivity.class, title);
		
		try {
			DoLoading();
			
			Bundle b = new Bundle();
			b.putInt("id", videoId);
			b.putString("title", videoTitle);
			
			ShowFinishNotification("Загрузка завершена", 
					videoTitle,
					"Видео загружено.", VideoPageActivity.class, b);
		}
		catch (DownloadException ex) {
			VCLog.write(ex, "DownloadService DownloadException!");
			
			ShowFinishNotification("Ошибка при звгрузке видео", 
					videoTitle,
					"Не удалось загрузить видео.", DownloadsActivity.class, null);
		}
		catch (DownloadAbortedException ex) {
			VCLog.write(ex, "DownloadService ABORTED! videoId = [" + videoId + "]");
			// Если прервано, протсо выходим по-тихому
		}
		finally {
			//ReportFinished();
			bus.removeStickyEvent(Progress.class);
			bus.post(new Finished(videoId));
			
			UnregisterBroadcast();
			VCLog.write("DownloadService finished. videoId = [" + videoId + "]");
		}
	}
	
	private String videoTitle;
	private int videoId;
	
	private final int BUFFER_SIZE = 1024*128;	// 128 кб - Размер 1 части
	
	public void DoLoading() throws DownloadException, DownloadAbortedException {
		NetUtils utils = new NetUtils(VkApp.GetUserID(this));
		
		// Получаем размер
		videoSize = utils.GetVideoSize(videoId);
		
		VCLog.write("DownloadService. Size = " + videoSize);

		if (videoSize <= 0) throw new DownloadException();
		
		
		final String LOADING_CACHE_DIR = Environment.getExternalStorageDirectory() + "/VCMobile/DownloadTmp/";
		
		FileOutputStream output = null;
		File f = null;
		
		try {
			// Файл куда будем писать
			f = new File(LOADING_CACHE_DIR + videoId + ".tmp");
			f.createNewFile();
			
			VCLog.write("DownloadService. Downloading to " + f.getAbsolutePath());

			output = new FileOutputStream(f.getAbsolutePath());
			int parts = (int)Math.ceil((double)videoSize / (double)BUFFER_SIZE);
			
			for (int i = 0; i < parts; i++) {
				double progress = (double)i / (double)parts * 100.0;
				NotifyProgress((int)Math.round(progress));
				if (!isAborted)
					bus.postSticky(new Progress(!isPaused, (int)Math.round(progress), videoId, videoSize));
				
				// Ждем, если стоим на паузе
				synchronized (this) {
					while (isPaused) { 
						try {
							wait();
						}
						catch (InterruptedException ex)	{ }
					}
					
					if (isAborted) throw new DownloadAbortedException();
				}
				
				// Читаем порцию
				int bufSize = BUFFER_SIZE;
				if (i == (parts - 1)) {
					// На последней итерации размер будет меньше
					bufSize = videoSize % BUFFER_SIZE;
				}
				
				byte[] buffer = utils.GetVideoPart(videoId, i*BUFFER_SIZE, bufSize);
				if (buffer == null) {
					//throw new DownloadException();
					// Не получилось прочитать часть. что делать?
					// Поставить на паузу?
					VCLog.write("DownloadService. Error loading part #" + i + ", buffer size = " + bufSize);
					VCLog.write("Pausing service...");
					
					Pause();	// Чтоб уведомлени послались!
					//synchronized (this) { isPaused = true; }
					
					// Отправим уведомление, что приостановлено все
					//Toast.makeText(this, "При загрузке видео произошла ошибка, ставим на паузу", Toast.LENGTH_SHORT).show();
					ShowNotification("Ошибка загруки видео \"" + videoTitle + "\"",
							videoTitle,
							"Произошла ошибка. Поставлено на паузу.",
							DownloadsActivity.class,
							null);

					i--;	// Чтобы ту же часть еще раз
					continue;
				}
				
				// Дописываем
				output.write(buffer);
			}
		
			output.close();
			//ReportProgress(99);
			if (!isAborted)
				bus.postSticky(new Progress(true, 99, videoId, videoSize));
			
			// Переименовываем файлик
			File fNew = new File(Environment.getExternalStorageDirectory() + "/VCMobile/VideoCache/" + videoId);
			f.renameTo(fNew);
			
			// Записываем в список загрузок
			VideoLibDownloads lib = new VideoLibDownloads();
			lib.Init();
			lib.videoDownloadsList.add((Integer)videoId);
			lib.SaveLists();
			
			VCLog.write("DownloadService. Renamed to" + fNew.getAbsolutePath());
		} 
		catch (DownloadAbortedException ex) {
			VCLog.write("DownloadService. DoDownload. DownloadAbortedException!");
			
			// Зачищаем все что накачали
			try {
				output.close();
			} catch (IOException e) {
				// Если не получилось закрыть - ну, видимо, ничего не делаем....
			}
			
			if (f != null)
				f.delete();
			
			throw ex; 
		} 
		catch (IOException e) {
			// Все ошибки с сетью у нас просто возвращают null, исключений НЕ выкидывают!
			// Значит, если попали сюда, что-то действительно серьезное.
			VCLog.write("DownloadService. DoDownload. IOException!");
			
			// Зачищаем все что накачали
			try {
				if (output != null)
					output.close();
			} catch (IOException ex) {
				// Если не получилось закрыть - ну, видимо, ничего не делаем....
			}
			
			if (f != null)
				f.delete();

			throw new DownloadException();
		}
	}
	
	// Произошла ошибка при загрузке какой-то части, потом можно будет возобновить процедуру.
	static class DownloadException extends Exception {}
	static class DownloadAbortedException extends Exception { }
}