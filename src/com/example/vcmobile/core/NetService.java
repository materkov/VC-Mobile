package com.example.vcmobile.core;

import java.util.Random;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.example.vcmobile.R;

abstract public class NetService extends IntentService {

	public NetService(String name, int notificationId, 
			String ServiceAction, String ServiceActionFinished,
			String ActionAbort, String ActionPause, String ActionResume) {
		super(name);
		serviceName = name;
		this.notificationId = notificationId;
		
		this.ServiceAction = ServiceAction;
		this.ServiceActionFinishedd = ServiceActionFinished;
		
		this.ActionAbort = ActionAbort;
		this.ActionPause = ActionPause;
		this.ActionResume = ActionResume;
	}
	
	private String serviceName;
	
	private Notification mainNotif;
	private NotificationManager notificationManager;
	private int notificationId;
	
	private String ServiceAction, ServiceActionFinishedd;
	private String ActionAbort, ActionPause, ActionResume;
	
	protected boolean isPaused = false;		// Сервис может быть приостановлен
	protected boolean isAborted = false;		// Сигнал что надо остановиться
	
	//private Intent serviceIntent;		// Sticky intent, будет вещаться пока работает сервис
	private Object serviceEvent;
	/*
	 * progress, int 0..100	
	 * isDoing true - ok, false - paused
	 *					
	 */
	protected int videoSize;
	protected int progress;
	
	abstract protected void DoPause();
	abstract protected void DoResume();
	abstract protected void DoAbort();
	
	synchronized protected void Pause() {
		isPaused = true;
		
		DoPause();
	}
	
	synchronized protected void Resume() {
		isPaused = false;
		notify();
		
		DoResume();
	}
	
	synchronized protected void Abort() {
		isAborted = true;
		
		// Если аборт был при паузе, из нее надо выйти
		isPaused = false;
		notify();
		
		DoAbort();
	}
	
	private BroadcastReceiver br;
	
	protected void RegisterBroadcast() {
		// Сервис будет слушать 3 действия
		br = new BroadcastReceiver() {
			// действия при получении сообщений
			public void onReceive(Context context, Intent intent) {
				VCLog.write(serviceName + ". BroadcastReceiver.OnRecieve, action = " + intent.getAction());
				
				if (intent.getAction().equals(ActionAbort))
					Abort();
				else if(intent.getAction().equals(ActionPause))
					Pause();
				else if(intent.getAction().equals(ActionResume))
					Resume();
			}
		};
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(ActionAbort);
		filter.addAction(ActionPause);
		filter.addAction(ActionResume);
		
		registerReceiver(br, filter);
	}
	
	protected void UnregisterBroadcast() {
		unregisterReceiver(br);
	}
	
	protected void NotifyProgress(int progress) {
		this.progress = progress;
		
		// Update in notification
		mainNotif.contentView.setProgressBar(R.id.notificationProgressBar, 100, progress, false);
		mainNotif.contentView.setTextViewText(R.id.tvNotificationProgressPercent, Integer.toString(progress) + "%");
		notificationManager.notify(notificationId, mainNotif); 
	}
	
	// Показать прогресс бар в области уведомлений
	protected void ShowStartNotification(String shortTitle, Class<?> startClass, String progressText) {
		mainNotif = new Notification(R.drawable.logo64, shortTitle, System.currentTimeMillis());

		// Активиити, которая будет запускаться при тыке на уведомдение
		Intent intent = new Intent(this, startClass);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
		
		// Что будет в самом уведомлении
		mainNotif.setLatestEventInfo(this, "", "", pi);
		mainNotif.flags |= Notification.FLAG_NO_CLEAR;
		mainNotif.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.notification);
		mainNotif.contentView.setProgressBar(R.id.notificationProgressBar, 100, 0, false);
		mainNotif.contentView.setTextViewText(R.id.tvNotificationProgress, progressText);
		//mainNotif.contentView.setInt(R.layout.notification, "setBackgroundResource", R.color.Azure);
		
		notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

		// Дабы наш сервис андроид не вздумал завершить при нехватке памяти
		startForeground(notificationId, mainNotif);
	}
	
	// Показать уведомление об окончании загрузки
	protected void ShowFinishNotification(String shortTitle, String title, String description, Class<?> startClass, Bundle extras) {
		// 1-я часть
		Notification notif = new Notification(R.drawable.logo64, shortTitle, System.currentTimeMillis());
		
		// 3-я часть
		Intent intent = new Intent(this, startClass);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		if (extras != null)
			intent.putExtras(extras);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
		
		// 2-я часть
		notif.setLatestEventInfo(this, title, description, pIntent);
		
		// ставим флаг, чтобы уведомление пропало после нажатия
		notif.flags |= Notification.FLAG_AUTO_CANCEL;
		
		Random r = new Random();
		
		// отправляем
		NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		nm.notify(r.nextInt(100000), notif);
		
		stopForeground(true);
	}
	
	protected void ShowNotification(String shortTitle, String title, String description, Class<?> startClass, Bundle extras) {
		// 1-я часть
		Notification notif = new Notification(R.drawable.logo64, shortTitle, System.currentTimeMillis());
		
		// 3-я часть
		Intent intent = new Intent(this, startClass);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		if (extras != null)
			intent.putExtras(extras);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
		
		// 2-я часть
		notif.setLatestEventInfo(this, title, description, pIntent);
		
		// ставим флаг, чтобы уведомление пропало после нажатия
		notif.flags |= Notification.FLAG_AUTO_CANCEL;
		
		Random r = new Random();
		
		// отправляем
		NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		nm.notify(r.nextInt(100000), notif);
	}
	
	protected void abortForeground() {
		stopForeground(true);
	}
}
