package com.example.vcmobile;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.example.vcmobile.core.DownloadService;
import com.example.vcmobile.core.UploadService;
import com.example.vcmobile.core.Utils;
import com.example.vcmobile.core.VCVideo;
import com.example.vcmobile.core.VCVideoJSON;
import com.example.vcmobile.core.VideoLib;
import com.example.vcmobile.core.VideoLibDownloads;
import com.example.vcmobile.core.VideoUtils;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

import de.greenrobot.event.EventBus;

public class DownloadsActivity extends SlidingFragmentActivity {
	//private VideoLib lib;
	
	private boolean initedD = false, initedU = false;
	private boolean isDoingD = true, isDoingU = true;
	 
	private View headerViewD;
	private View headerViewU;
	
	private VCVideo currentDownload;
	
	private ImageLoader imageLoader;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_downloads);
		
		Utils.InitUI(this, "Загрузки");
		Utils.SetActionBarBackButton(this);
		
		imageLoader = new ImageLoader(getApplicationContext());
		
		headerViewU = SetupHeader("Выгрузка", 
				UploadService.BROADCAST_PAUSE_ACTION, 
				UploadService.BROADCAST_RESUME_ACTION, 
				UploadService.BROADCAST_ABORT_ACTION);
		
		headerViewD = SetupHeader("Загрузка",
				DownloadService.BROADCAST_PAUSE_ACTION, 
				DownloadService.BROADCAST_RESUME_ACTION, 
				DownloadService.BROADCAST_ABORT_ACTION);
		
		EventBus.getDefault().registerSticky(this);
		UpdateAdapter();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		EventBus.getDefault().unregister(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		
		// Снимаем выделение со всех
		ListView v = (ListView)findViewById(R.id.listViewDownloads);
		if (v != null)
			v.setItemChecked(-1, true);
	}

	// Слушаем Download service
	public void onEventMainThread(DownloadService.Finished e) {
		// Закончили загрузку
		headerViewD.findViewById(R.id.currentListHeadRow).setVisibility(View.GONE);
		initedD = false;
		
		currentDownload = null;
		
		UpdateAdapter();
	}
	
	public void onEventMainThread(DownloadService.Progress e){  
		final ImageView pauseImage = (ImageView)headerViewD.findViewById(R.id.pauseListHeadButton);
		
		boolean firstShow = false;
		if (!initedD) {
			// Начальаная установка
			// Получаем библиотеку
			VideoLib lib = new VideoLib();
			lib.Init();
			VCVideo video = lib.videosList.get(e.videoId);
			
			// Имя
			TextView t = (TextView)headerViewD.findViewById(R.id.currentListHeadTitle);
			t.setText(video.video.title);
			
			// Картинка
			ImageView image = (ImageView)headerViewD.findViewById(R.id.imgIcon);
			imageLoader.DisplayImage(video.video.id + ".jpg", image);
			
			// Длительность
			TextView length = (TextView)headerViewD.findViewById(R.id.txtLength);
			length.setText(VideoUtils.GetFormatedLength(video.video.length));

			headerViewD.findViewById(R.id.currentListHeadRow).setVisibility(View.VISIBLE);
			
			isDoingD = true;
			initedD = true;
			firstShow = true;
			
			currentDownload = video;
		}

		// Состояние
		boolean isDoingCurrent = e.isDoing;
		if (isDoingCurrent != isDoingD) {
			// Если изменилось состояние по сравнению с предыдущим
			isDoingD = isDoingCurrent;
			
			if (isDoingCurrent) {
				// Поставить кнопку пауза
				pauseImage.setImageResource(R.drawable.pause_button);
				pauseImage.setTag(Boolean.valueOf(true));
			}
			else {
				// Поставить кнопку возобновить
				pauseImage.setImageResource(R.drawable.resume_button);
				pauseImage.setTag(Boolean.valueOf(false));
			}
		}
		
		// Обновляем прогресс только если не на паузе, либо 1 раз
		if (isDoingCurrent || firstShow) {		
			// Прогресс
			ProgressBar pb = (ProgressBar)headerViewD.findViewById(R.id.currentListHeadProgressBar);
			pb.setIndeterminate(false);
			pb.setProgress(e.progress);
			
			// Размер
			double mbSize = e.videoSize;
			mbSize = mbSize / 1024.0 / 1024.0;
			mbSize = Math.round(mbSize * 100.0) / 100.0;
			
			double current =  mbSize * ((double)e.progress / 100.0);
			current = Math.round(current * 100.0) / 100.0;
			
			TextView t = (TextView)headerViewD.findViewById(R.id.currentListHeadSize);
			t.setText(current + " / " + mbSize + " МБ (" + e.progress + "%)");
		}
	} 
	
	// Слушаем Upload service
	public void onEventMainThread(UploadService.Finished e) {
		// Закончили загрузку
		headerViewU.findViewById(R.id.currentListHeadRow).setVisibility(View.GONE);
		initedU = false;
		
		UpdateAdapter();
	}
	
	public void onEventMainThread(UploadService.Progress e){  
		final ImageView pauseImage = (ImageView)headerViewU.findViewById(R.id.pauseListHeadButton);
		
		boolean firstShow = false;
		if (!initedU) {
			// Начальаная установка
			
			// Имя
			TextView t = (TextView)headerViewU.findViewById(R.id.currentListHeadTitle);
			t.setText(e.name);
			
			// Картинка
			ImageView image = (ImageView)headerViewU.findViewById(R.id.imgIcon);
			image.setScaleType(ScaleType.FIT_CENTER);
			image.setImageResource(R.drawable.uploading);
			
			// Длительность
			TextView length = (TextView)headerViewU.findViewById(R.id.txtLength);
			length.setVisibility(View.GONE);
			
			headerViewU.findViewById(R.id.currentListHeadRow).setVisibility(View.VISIBLE);
			
			isDoingU = true;
			initedU = true;
			firstShow = true;
		}

		// Состояние
		boolean isDoingCurrent = e.isDoing;
		if (isDoingCurrent != isDoingU) {
			// Если изменилось состояние по сравнению с предыдущим
			isDoingU = isDoingCurrent;
			
			if (isDoingCurrent) {
				// Поставить кнопку пауза
				pauseImage.setImageResource(R.drawable.pause_button);
				pauseImage.setTag(Boolean.valueOf(true));
			}
			else {
				// Поставить кнопку возобновить
				pauseImage.setImageResource(R.drawable.resume_button);
				pauseImage.setTag(Boolean.valueOf(false));
			}
		}
		
		// Обновляем прогресс только если не на паузе, либо 1 раз
		if (isDoingCurrent || firstShow) {		
			// Прогресс
			ProgressBar pb = (ProgressBar)headerViewU.findViewById(R.id.currentListHeadProgressBar);
			pb.setIndeterminate(false);
			pb.setProgress(e.progress);
			
			// Размер
			double mbSize = e.videoSize;
			mbSize = mbSize / 1024.0 / 1024.0;
			mbSize = Math.round(mbSize * 100.0) / 100.0;
			
			double current =  mbSize * ((double)e.progress / 100.0);
			current = Math.round(current * 100.0) / 100.0;
			
			TextView t = (TextView)headerViewU.findViewById(R.id.currentListHeadSize);
			t.setText(current + " / " + mbSize + " МБ (" + e.progress + "%)");
		}
	}
	
	private View SetupHeader(final String name, final String pauseAction, final String resumeAction, final String abortAction) {
		final ListView v = (ListView)findViewById(R.id.listViewDownloads);
		
		final View headerView = getLayoutInflater().inflate(R.layout.current_download_head_row, null);
		v.addHeaderView(headerView, null, false);
		headerView.findViewById(R.id.currentListHeadRow).setVisibility(View.GONE);
		
		
		// Onclick
		//View inner = headerView.findViewById(R.id.currentListHeadRow);
		//inner.setClickable(true);
		headerView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (currentDownload != null)
					VideoUtils.StartVideoPageActivity(DownloadsActivity.this, currentDownload.video, false);
			}
		});

		// Delete button
		ImageView image = (ImageView)headerView.findViewById(R.id.deleteListHeadButton);
		image.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendBroadcast(new Intent(abortAction));
				
				// Скрываем хедер из вида
				// !!!! Это нужно для нормальной быстрой рекции на кнопку
				headerView.findViewById(R.id.currentListHeadRow).setVisibility(View.GONE);
				
				Toast.makeText(DownloadsActivity.this, name + " удалена", Toast.LENGTH_SHORT).show();
			}
		});
		
		// Pause button
		final ImageView pauseImage = (ImageView)headerView.findViewById(R.id.pauseListHeadButton);
		OnClickListener l = new OnClickListener() {
			@Override
			public void onClick(View v) {
				Boolean isDoing = (Boolean)v.getTag();

				if (isDoing) {
					sendBroadcast(new Intent(pauseAction));
					
					Toast.makeText(DownloadsActivity.this, name + " приостановлена", Toast.LENGTH_SHORT).show();
				}
				else {
					sendBroadcast(new Intent(resumeAction));
					
					Toast.makeText(DownloadsActivity.this, name + " возобновлена", Toast.LENGTH_SHORT).show();
				}
			}
		};
		pauseImage.setOnClickListener(l);
		pauseImage.setTag(Boolean.valueOf(true));
		
		return headerView;
	}
	
	// Обновить адаптер
	private void UpdateAdapter() {
		ArrayList<VCVideoJSON> data = new ArrayList<VCVideoJSON>();
		
		VideoLibDownloads libDownloads = new VideoLibDownloads();
		libDownloads.Init();
		
		VideoLib lib = new VideoLib();
		lib.Init();
		
		// Смотрим, какие видео есть
		for (Integer id : libDownloads.videoDownloadsList) {
			VCVideo video = lib.videosList.get(id);
			if (video != null)
				data.add(video.video);
		}
		
		DownloadsListElementAdapter adapter = new DownloadsListElementAdapter(this, data, libDownloads);
		ListView v = (ListView)findViewById(R.id.listViewDownloads);
		v.setAdapter(adapter);
		
		// Надпись поверх что загруженных видео нету
		TextView tv = (TextView)findViewById(R.id.downloadsTextView);
		tv.setVisibility(data.size() == 0 ? View.VISIBLE : View.GONE);
	}
	
	private void DoDeleteAll() {
		VideoLibDownloads libDownloads = new VideoLibDownloads();
		libDownloads.Init();
		libDownloads.videoDownloadsList.clear();
		libDownloads.SaveLists();
		
		UpdateAdapter();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		menu.add(Menu.NONE, 3, Menu.NONE, "Удалить все")
			.setIcon(R.drawable.cancel_all)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, com.actionbarsherlock.view.MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			onBackPressed();
			break;
		case 3:
			//DELETE ALL;
			new AlertDialog.Builder(this)
					.setTitle("Удалить все")
					.setMessage("Вы уверены, что хотите очистить список загрузок?")
					.setPositiveButton("Да",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,	int which) {
									DoDeleteAll();
								}
							})
					.setNegativeButton("Нет",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,	int which) {
									// do nothing
								}
							}).show();
			
			break;
		}

		return true;
	}
}