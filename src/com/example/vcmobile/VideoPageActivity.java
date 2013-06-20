package com.example.vcmobile;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import junit.framework.Assert;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.example.vcmobile.VCVideoView.PlayPauseListener;
import com.example.vcmobile.core.DownloadService;
import com.example.vcmobile.core.NetUtils;
import com.example.vcmobile.core.Utils;
import com.example.vcmobile.core.VCLog;
import com.example.vcmobile.core.VCVideo;
import com.example.vcmobile.core.VCVideoShortJSON;
import com.example.vcmobile.core.VideoLib;
import com.example.vcmobile.core.VideoUtils;
import com.example.vcmobile.core.VkApp;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

import de.greenrobot.event.EventBus;

public class VideoPageActivity extends SlidingFragmentActivity {
	// From intent:
	private int videoId;
	private String videoTitle;
	
	private enum PageState {
		NONE,				// = 0
		CREATED_NORMAL,
		PLAYING,
		PAUSED,
		FINISHED,
		FINISHED_RECREATED
	};
	
	private PageState state = PageState.NONE;
	private int seekPosition = 0;
	
	private boolean fromList = false;
	private VCVideo video;
	
	private WeakReference<LoadInfoTask> infoTask;
	private WeakReference<LoadPreviewTask> previewTask;
	
	// Статус загрузки (для изменения состояния download-button)
	private enum DownloadState {
		NotLoaded,
		Loading,
		Loaded
	};
	private DownloadState downloadState = DownloadState.NotLoaded;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (savedInstanceState == null) {
			// Создаем первый раз (пришли из каталога)
			videoId = getIntent().getIntExtra("id", 0);
			videoTitle = getIntent().getStringExtra("title");
			
			fromList = getIntent().getBooleanExtra("fromList", false);
			
			Assert.assertTrue(videoId != 0 && videoTitle != null);
			
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
				state = PageState.PLAYING;
			else
				state = PageState.CREATED_NORMAL;
		}
		else { 
			// Восстанавливаем переменные
			videoId = savedInstanceState.getInt("vid");
			videoTitle = savedInstanceState.getString("title");
			
			seekPosition = savedInstanceState.getInt("pos");
			state = PageState.values()[savedInstanceState.getInt("state")];
			fromList = savedInstanceState.getBoolean("fromList");
			
			if (state == PageState.FINISHED)
				state = PageState.FINISHED_RECREATED;
			
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
					&& state == PageState.CREATED_NORMAL) {
				state = PageState.PLAYING;
				
				// Зарегистрировать просмотр (это при повороте)
				IncrenentViewTask task = new IncrenentViewTask();
				task.isLand = true;
				task.execute();
			}
		}
		
		Assert.assertTrue(state != PageState.NONE);
		
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			SetupLandscape(savedInstanceState);
		}
		else {
			super.onCreate(savedInstanceState); // BUG FIX! Должен быть именно здесь!
			SetupNormal();
		}
	}
	
	// Listening from Download service
	public void onEventMainThread(DownloadService.Progress e) {
		if (e.videoId != videoId) return; // Не наше видео
		
		if (downloadState != DownloadState.Loading) {
			// Сменить иконку на прогресс
			ImageView iv = (ImageView)findViewById(R.id.pageDownloadButton);
			iv.setImageResource(R.drawable.loading_black24);
			iv.setPadding(8, 8, 8, 8);
			
			// Показать анимашку
			Animation hyperspaceJump = AnimationUtils.loadAnimation(this, R.anim.progress_animation);
			iv.startAnimation(hyperspaceJump);
			
			downloadState = DownloadState.Loading;
			
			// Прогресс бар показать
			ProgressBar pb = (ProgressBar)findViewById(R.id.progressBarVPage);
			pb.setVisibility(View.VISIBLE);
			if (e.progress == 0) pb.setIndeterminate(true);
			
			return;
		}
		
		// Обновить прогресс бар
		ProgressBar pb = (ProgressBar)findViewById(R.id.progressBarVPage);
		pb.setIndeterminate(false);
		pb.setProgress(e.progress);
	}
	
	public void onEventMainThread(DownloadService.Finished e) {
		if (e.videoId != videoId) return; // Не наше видео
		
		// Финиш может быть и неудачным!
		ImageView iv = (ImageView)findViewById(R.id.pageDownloadButton);
		iv.clearAnimation();
		iv.setPadding(0, 0, 0, 0);
		
		if (VideoUtils.isDownloaded(videoId)) {
			// Сменить иконку на финиш
			iv.setImageResource(R.drawable.video_download_ok);
			
			downloadState = DownloadState.Loaded;
		}
		else {
			// Сменить иконку на обычную
			iv.setImageResource(R.drawable.video_download_button);
			
			downloadState = DownloadState.NotLoaded;
		}
		
		// Прогресс бар скрыть
		ProgressBar pb = (ProgressBar)findViewById(R.id.progressBarVPage);
		pb.setVisibility(View.GONE);
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		
		if (state == PageState.PLAYING || state == PageState.PAUSED) {
			VideoView myVideoView = (VideoView)findViewById(R.id.videoView1);
			myVideoView.seekTo(seekPosition);
			myVideoView.pause();
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (infoTask != null && infoTask.get() != null) {
			infoTask.get().activity = null;
		}
		if (previewTask != null && previewTask.get() != null) {
			previewTask.get().activity = null;
		}
		
		EventBus.getDefault().unregister(this);
	}
	
	// Создать активити при нормальной ориентации
	private void SetupNormal() {
		// Создать полный UI
		setContentView(R.layout.activity_video_page);
		Utils.InitUI(this, videoTitle);
		Utils.SetActionBarBackButton(this);
		
		// Название видоса
		TextView titleT = (TextView)findViewById(R.id.titleTextView);
		titleT.setText(videoTitle);
		
		// Прогресс слушать будем
		SetupDownloadButton();
		EventBus.getDefault().registerSticky(this);
		
		// Загружаем всю инфу полностью, она тут будет отображаться, никуда не деться :(
		infoTask = new WeakReference<LoadInfoTask>(new LoadInfoTask());
		infoTask.get().activity = new WeakReference<VideoPageActivity>(this);
		infoTask.get().execute(videoId, VkApp.GetUserID(this));
		
		// Анимашку пока инфа загружается
		ShowInfoProgressAnimation();
		
		if (state == PageState.CREATED_NORMAL) {
			// В этом состоянии нам нужна превьюшка
			previewTask = new WeakReference<LoadPreviewTask>(new LoadPreviewTask());
			previewTask.get().activity = new WeakReference<VideoPageActivity>(this);
			previewTask.get().execute(videoId);
			
			// Пока загружается ПРЕВЬЮШКА, включить анимашку
			// Здесь анимашка именно по поводу загрузки превьюшки, а не видяшки!
			ShowPreviewProgressAnimation();
		}
		else if (state == PageState.PAUSED) {
			SetupPlayer();
			PlayerSeek(seekPosition);
		}
		else if (state == PageState.PLAYING) {
			SetupPlayer();
			PlayerSeek(seekPosition);
			PlayVideo();
		}
		else if (state == PageState.FINISHED_RECREATED) {
			ShowRecratedReplayButton();
		}
	}
	
	// Поменять иконку на зугружено, если надо
	private void SetupDownloadButton() {
		if (VideoUtils.isDownloaded(videoId)) {
			// Уже загружен
			ImageView iv = (ImageView)findViewById(R.id.pageDownloadButton);
			iv.setImageResource(R.drawable.video_download_ok);
			
			downloadState = DownloadState.Loaded;
		}
	}
		
	// Создать активити при landscape ориентации
	private void SetupLandscape(Bundle savedInstanceState) {
		// Убрать все (сделать фулскрин)
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		// BUG FIX!!!!! Должен быть именно здесь!
		super.onCreate(savedInstanceState);
		
		// Скрыть soft buttons для андроид 4 и выше
		//getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
		
		setContentView(R.layout.activity_video_page);
		Utils.InitHiddenUI(this);
		
		// Вперед! Показать видео и спозиционировать
		if (state == PageState.PAUSED) {
			SetupPlayer();
			PlayerSeek(seekPosition);
		}
		else if (state == PageState.PLAYING) {
			SetupPlayer();
			PlayerSeek(seekPosition);
			PlayVideo();
		}
		else if (state == PageState.FINISHED_RECREATED) {
			ShowRecratedReplayButton();
		}
	}
	
	// Отправить на стену - кнопка
	public void onWallPostClick(View v) {
		new WallPostTask().execute(this);
		
		Toast.makeText(this, "Видео отправлено на вашу страницу ВКонтакте", Toast.LENGTH_SHORT).show();
	}
	
	class WallPostTask extends AsyncTask<SlidingFragmentActivity, Void, Void> {
		@Override
		protected Void doInBackground(SlidingFragmentActivity... arg) {
			if (!VkApp.postToWall(VideoPageActivity.this, videoId, videoTitle)) {
				Utils.authFail(arg[0]);
			}
			
			return null;
		}
	}
	
	private class IncrenentViewTask extends AsyncTask<Void, Void, Void> {
		public boolean isLand;
		
		@Override
		protected void onPreExecute() {
			if (isLand) return;
			
			// Обновить счетчик на странице
			TextView tv = (TextView)findViewById(R.id.viewsTextView);
			if (tv != null) {
				int prev = Integer.parseInt(tv.getText().toString());
				tv.setText(Integer.toString(prev + 1));
			}
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			// На сервер инкремент
			NetUtils utils = new NetUtils(0);	// 0 is UserID!!!
			utils.IncrementVideoView(videoId);
			
			// Сохраняем у себя инкремент
			VideoLib lib = new VideoLib();
			lib.Init();
			VCVideo video = lib.videosList.get(videoId);
			if (video != null) {
				video.videoShort.views++;
				lib.SaveLists();
				
				// Броадкаст
				sendBroadcast(new Intent(VideoListActivity.UPDATE_BROADCAST)
					.putExtra("set_views", video.videoShort.views)
					.putExtra("id", video.videoShort.id));
			}
			else
				VCLog.write("WARNIGN! IncrenentViewTask video==null");
		
			return null;
		}
	}
	
	private class AddLikeTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			// На сервере добавляем
			NetUtils utils = new NetUtils(VkApp.GetUserID(VideoPageActivity.this));
			utils.AddLike(videoId);
			
			// Сохраняем у себя
			VideoLib lib = new VideoLib();
			lib.Init();
			VCVideo video = lib.videosList.get(videoId);
			if (video != null) {
				video.videoShort.likes++;
				video.videoShort.ilike = true;
				lib.SaveLists();
				
				// Броадкаст
				sendBroadcast(new Intent(VideoListActivity.UPDATE_BROADCAST)
					.putExtra("set_likes", video.videoShort.likes)
					.putExtra("set_ilike", video.videoShort.ilike)
					.putExtra("id", video.videoShort.id));
			}
			else
				VCLog.write("WARNING! AddLikeTask video==null");
			
			return null;
		}
	}
	
	private class DeleteLikeTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			// На сервер
			NetUtils utils = new NetUtils(VkApp.GetUserID(VideoPageActivity.this));
			utils.DeleteLike(videoId);
			
			// Сохраняем у себя
			VideoLib lib = new VideoLib();
			lib.Init();
			VCVideo video = lib.videosList.get(videoId);
			if (video != null) {
				video.videoShort.likes--;
				video.videoShort.ilike = false;
				lib.SaveLists();
				
				// Броадкаст
				sendBroadcast(new Intent(VideoListActivity.UPDATE_BROADCAST)
					.putExtra("set_likes", video.videoShort.likes)
					.putExtra("set_ilike", video.videoShort.ilike)
					.putExtra("id", video.videoShort.id));
			}
			else
				VCLog.write("WARNING! DeleteLikeTask video==null");
			
			
			
			return null;
		}
	}
	
	private class AddToMyVideosTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			// На сервере добавляем
			NetUtils utils = new NetUtils(VkApp.GetUserID(VideoPageActivity.this));
			utils.AddToMyVideos(videoId);
			
			// Сохраняем у себя
			VideoLib lib = new VideoLib();
			lib.Init();
			VCVideo video = lib.videosList.get(videoId);
			if (video != null) {
				video.isMy = true;
				VideoPageActivity.this.video.isMy = true;	// Видео самой странички
				lib.SaveLists();
			}
			
			// Броадкаст
			sendBroadcast(new Intent(VideoListActivity.UPDATE_BROADCAST)
				.putExtra("add_my_list", videoId));
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			supportInvalidateOptionsMenu(); // Перестроить меню
		}
	}
	
	private class DeleteFromMyVideosTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			// На сервере добавляем
			NetUtils utils = new NetUtils(VkApp.GetUserID(VideoPageActivity.this));
			utils.DeleteFromMyVideos(videoId);
			
			// Сохраняем у себя
			VideoLib lib = new VideoLib();
			lib.Init();
			VCVideo video = lib.videosList.get(videoId);
			if (video != null) {
				video.isMy = false;
				VideoPageActivity.this.video.isMy = false;	// Видео самой странички
				lib.SaveLists();
			}
			
			// Броадкаст
			sendBroadcast(new Intent(VideoListActivity.UPDATE_BROADCAST)
				.putExtra("del_my_list", videoId));
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			supportInvalidateOptionsMenu(); // Перестроить меню
		}
	}
	
	public void onProgressBarClick(View v) {
		startActivity(new Intent(this, DownloadsActivity.class));
	}
	
	// Тык на скачать
	public void onDownloadClick(View v) {
		if (downloadState == DownloadState.NotLoaded) {
			Intent intent = new Intent(this, DownloadService.class)
				.putExtra("id", videoId)
				.putExtra("title", videoTitle);
		
			startService(intent);
			
			Toast.makeText(this, "Началась загрузка видео", Toast.LENGTH_SHORT).show();
		}
		else {
			startActivity(new Intent(this, DownloadsActivity.class));
		}
	}
	
	public void onEmailSendClick(View w) {
		startActivity(new Intent(this, SendEmailActivity.class).putExtra("id", videoId));
	}
	
	public void onLikeClick(View v) {
		if (!video.videoShort.ilike) {
			// Увеличивам число в TextView
			TextView tv = (TextView)findViewById(R.id.likesTextView);
			Integer likes = Integer.parseInt(tv.getText().toString());
			tv.setText(Integer.toString(likes+1));

			// Меняем картинку
			ImageView iv = (ImageView)findViewById(R.id.likesButton);
			iv.setImageResource(R.drawable.like_p);
			
			// Сохраняем лайк
			new AddLikeTask().execute();
			video.videoShort.likes++;
			
			Toast.makeText(this, "Вам понравилось это видео.", Toast.LENGTH_SHORT).show();
		}
		else {
			// Уже лайкнуто мною
			// TextView
			TextView tv = (TextView)findViewById(R.id.likesTextView);
			Integer likes = Integer.parseInt(tv.getText().toString());
			tv.setText(Integer.toString(likes-1));
			
			// Меняем картинку
			ImageView iv = (ImageView)findViewById(R.id.likesButton);
			iv.setImageResource(R.drawable.like);
			
			// Сохраняем анлайк
			new DeleteLikeTask().execute();
			video.videoShort.likes--;
			
			Toast.makeText(this, "Вам больше не нравится это видео.", Toast.LENGTH_SHORT).show();

		}
		
		video.videoShort.ilike = !video.videoShort.ilike;
	}
	
	// Перемотать
	private void PlayerSeek(int seekTo) {
		VideoView myVideoView = (VideoView)findViewById(R.id.videoView1);
		myVideoView.seekTo(seekTo);
	}
	
	// Показать анимашку вместо превьюшки
	private void ShowPreviewProgressAnimation() {
		ImageView myImageView = (ImageView)findViewById(R.id.previewImageView);
		myImageView.setImageResource(R.drawable.loading32);
		myImageView.setScaleType(ScaleType.CENTER);
		
		Animation hyperspaceJump = AnimationUtils.loadAnimation(VideoPageActivity.this, R.anim.progress_animation);
		myImageView.startAnimation(hyperspaceJump);
	}
	
	// Скрыть анимацию на превьюшке
	private void HidePreviewProgressAnimation() {
		// Убираем анимашку - стираем анимацию из изображения
		ImageView myImageView = (ImageView)findViewById(R.id.previewImageView);
		myImageView.clearAnimation();
		myImageView.setScaleType(ScaleType.FIT_CENTER);
	}
	
	// Показать прогрессбар на странице с инфой
	private void ShowInfoProgressAnimation() {
		// Показываем анимашку
		Animation hyperspaceJump = AnimationUtils.loadAnimation(VideoPageActivity.this, R.anim.progress_animation);
		ImageView v = (ImageView)findViewById(R.id.otherInfoPlaceholder);
		v.startAnimation(hyperspaceJump);
		
		// Скрываем данные
		findViewById(R.id.otherInfoView).setVisibility(View.GONE);
	}
	
	// Скрыть прогрессбар со страницы с с инфой
	private void HideInfoProgressAnimation() {
		// Скрываем анимашку
		ImageView p = (ImageView)findViewById(R.id.otherInfoPlaceholder);
		p.clearAnimation();
		p.setVisibility(View.GONE);
		
		// Делаем видимыми данные
		findViewById(R.id.otherInfoView).setVisibility(View.VISIBLE);
	}
	
	private void ShowRecratedReplayButton() {
		// Показываем replay кнопку
		ImageView replay = (ImageView)findViewById(R.id.videoReplayImageView);
		replay.setVisibility(View.VISIBLE);
		
		replay.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				seekPosition = 0;
				SetupPlayer();
				PlayVideo();
				state = PageState.PLAYING;
				
				HideReplayButton();
			}
		});
	}
	
	private void HideReplayButton() {
		findViewById(R.id.videoReplayImageView).setVisibility(View.GONE);
	}
	
	// Запустить воспроизведение
	private void PlayVideo() {
		VCVideoView videoView = (VCVideoView)findViewById(R.id.videoView1);
		videoView.requestFocus();
		videoView.start();
	}
	
	private class CheckEncoded extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {
			NetUtils utils = new NetUtils(0);
			return utils.IsVideoEncoding(videoId);
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			if (!result) {
				DoSetupPlayer();
			}
			else {
				// Если видео еще декодируется, то показать ошибку
				findViewById(R.id.previewImageView).setVisibility(View.GONE);
				findViewById(R.id.videoView1).setVisibility(View.GONE);
				
				// Показываем текст с ошибкой
				TextView errorTV = (TextView)findViewById(R.id.videoErrorTextView);
				errorTV.setText("Это видео сейчас декодируется. Попробуйте позже.");
				errorTV.setVisibility(View.VISIBLE);
				
				// Скрываем анимашку
				HidePreviewProgressAnimation();
			}
		}
	}
	
	private void SetupPlayer() {
		if (VideoUtils.isDownloaded(videoId)) {
			// Если загружено, грущим как обычно
			DoSetupPlayer();
		}
		else {
			// Если в сети, сначала проверить что уже перекодировано
			ShowPreviewProgressAnimation();
			new CheckEncoded().execute();
		}
	}
	
	// Настроить плеер
	private void DoSetupPlayer() {
		final VCVideoView videoView = (VCVideoView)findViewById(R.id.videoView1);
		
		// Ввидео начинает "готовится" с момента установки URI, а не с момента комнады старт
		// Поэтому сразу показываем анимашку
		ShowPreviewProgressAnimation();
		
		// Установить источник
		Uri uri = Uri.parse(VideoUtils.GetPath(videoId));
		videoView.setVideoURI(uri);
		
		// Создаем Controller
		final VCMediaController mc = new VCMediaController(this);
		videoView.setMediaController(mc);
		mc.setAnchorView(findViewById(R.id.VideoFrameLayout));
		
		// Действия, когда видео подготовилось и готово начаться
		videoView.setOnPreparedListener(new OnPreparedListener() {
			public void onPrepared(MediaPlayer mp) {
				// Совсем убрать превьюшку
				HidePreviewProgressAnimation();
				findViewById(R.id.previewImageView).setVisibility(View.GONE);
				
				mc.show(3000);
			}
		});
		
		// Действия при нажатии Play/pause
		videoView.setPlayPauseListener(new PlayPauseListener() {
			@Override
			public void onPlay() {
				state = PageState.PLAYING;
			}
			
			@Override
			public void onPause() {
				state = PageState.PAUSED;
			}
		});
		
		// Действия, если ошибочка вышла
		videoView.setOnErrorListener(new OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				// Убираем превьюшку и видео
				HidePreviewProgressAnimation();
				findViewById(R.id.previewImageView).setVisibility(View.GONE);
				findViewById(R.id.videoView1).setVisibility(View.GONE);
				
				// Показываем текст с ошибкой
				findViewById(R.id.videoErrorTextView).setVisibility(View.VISIBLE);
				
				return true;
			}
		});
		
		// Действия, когда видео закончилось
		videoView.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				state = PageState.FINISHED;
				mc.hide();
				mc.SetForceHide(true);
				
				// Показываем replay кнопку
				ImageView replay = (ImageView)findViewById(R.id.videoReplayImageView);
				replay.setVisibility(View.VISIBLE);
				
				replay.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						seekPosition = 0;
						videoView.seekTo(0);
						videoView.start();
						
						mc.SetForceHide(false);
						
						mc.show(3000);
						
						state = PageState.PLAYING;
						
						HideReplayButton();
					}
				});
			}
		});
	}

	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Если video == null, просто ничего не доабвляем, инфа еще не загружена
		int myID = VkApp.GetUserID(this);
		
		if (video != null && video.isMy && video.video.author == myID) {
			// Я - автор видоса
			menu.add(Menu.NONE, 3, Menu.NONE, "Редактировать")
				.setIcon(R.drawable.edit)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
			
			menu.add(Menu.NONE, 4, Menu.NONE, "Удалить")
				.setIcon(R.drawable.cancel_all)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		}
		else if (video != null && video.isMy) {
			menu.add(Menu.NONE, 7, Menu.NONE, "Удалить из моих")
				.setIcon(R.drawable.cancel_all)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		}
		else if (video != null) {
			menu.add(Menu.NONE, 10, Menu.NONE, "Добавить в мои")
				.setIcon(R.drawable.add)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		}
		
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
			// EDIT
			Intent i = new Intent(this, VideoEditActivity.class);
			i.putExtra("type", 2); // edit mode
			i.putExtra("id", videoId);
			startActivity(i);
			finish();
			break;

		case 4:
			// DELETE
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			
			builder.setMessage("Вы действительно хотите совсем удалить это видео из каталога?")
				   .setPositiveButton("Да", new DialogInterface.OnClickListener() {
					   public void onClick(DialogInterface dialog, int id) {
							// Удалить на сервере
							new DeleteTask().execute(videoId);
							
							// Удаляем из коллекции
							VideoLib lib = new VideoLib();
							lib.Init();
							lib.videosList.remove(videoId);
							lib.SaveLists();
							
							// Броадкаст
							sendBroadcast(new Intent(VideoListActivity.UPDATE_BROADCAST));

							// переходим на экран каталога
							startActivity(new Intent(VideoPageActivity.this, VideoListActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
							finish();
						}
				   })
				   .setNegativeButton("Нет", new DialogInterface.OnClickListener() {
					   public void onClick(DialogInterface dialog, int id) {
						   // Делаем ничего
					   }
				   })
				   .create()
				   .show();
			break;
			
		case 7:
			// Удалить из моих видео
			Toast.makeText(this, "Эыто видео удалено из моих видео.", Toast.LENGTH_SHORT).show();
			new DeleteFromMyVideosTask().execute();
			
			break;
			
		case 10:
			// Добавить в мои видео
			Toast.makeText(this, "Это видео добавлено в мои видео.", Toast.LENGTH_SHORT).show();
			new AddToMyVideosTask().execute();
			
			break;
		}

		return true;
	}
	
	private static class DeleteTask extends AsyncTask<Integer, Void, Void> {
		@Override
		protected Void doInBackground(Integer... arg) {
			NetUtils utils = new NetUtils(0);
			utils.DeleteVideo(arg[0]);
			
			return null;
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		VideoView mVideoView = (VideoView)findViewById(R.id.videoView1);
		
		// From intent:
		outState.putInt("vid", videoId);
		outState.putString("title", videoTitle);
		outState.putBoolean("fromList", fromList);
		
		seekPosition = Math.max(mVideoView.getCurrentPosition(), seekPosition); 
		outState.putInt("pos", seekPosition);
		outState.putInt("state", state.ordinal());
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		
		if (fromList)
			overridePendingTransition(R.anim.right_out, R.anim.left_in);
	}
	
	static class LoadPreviewTask extends AsyncTask<Integer, Void, Bitmap> {
		public WeakReference<VideoPageActivity> activity;
		
		@Override
		protected Bitmap doInBackground(Integer... params) {
			int videoId = params[0];
			
			String previewPath = VideoUtils.GetPreviewPathLocal(videoId);
			return BitmapFactory.decodeFile(previewPath);
		}
		
		@Override
		protected void onPostExecute(Bitmap result) {
			if (activity != null && activity.get() != null) {
				// Убираем анимашку
				activity.get().HidePreviewProgressAnimation();
				
				// Ставим превьшку на это место
				ImageView previewImageView = (ImageView)activity.get().findViewById(R.id.previewImageView);
				if (result != null)
					previewImageView.setImageBitmap(result);
				else {
					// Если по каким-то причинам не загрузилась превьюшка 
					// (например не успела локально загрузится на активити каталога)
					// тогда показываем просто черный цвет
					previewImageView.setImageResource(R.color.Black);
				}
				
				// Включаем треугольничек (кнопка Play)
				final View playButton = activity.get().findViewById(R.id.playImageView);
				playButton.setVisibility(View.VISIBLE);
				
				// Тык на превтюшку от видео, тут надо запустить видос
				previewImageView.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						if (activity != null && activity.get() != null) {
							// Убираем треугольник
							playButton.setVisibility(View.GONE);
							
							activity.get().SetupPlayer();
							activity.get().PlayVideo();
							activity.get().state = PageState.PLAYING;
							
							// Регистрируем просмотр на сервере!
							IncrenentViewTask incTask = activity.get().new IncrenentViewTask();
							incTask.isLand = false;
							incTask.execute();
						}
					}});
			}
		}
	}
	
	static class LoadInfoTask extends AsyncTask<Integer, Void, VCVideo> {
		public WeakReference<VideoPageActivity> activity;
		public boolean needUpdate = false;
		
		@Override
		protected VCVideo doInBackground(Integer... params) {
			int id = params[0];
			int userId = params[1];
			
			// Грузим данные о конкретном видосе
			VideoLib lib = new VideoLib();
			lib.Init();
			
			VCVideo video = lib.videosList.get(id);
			Assert.assertNotNull(video);
			
			// Запрашиваем короткую инфу
			NetUtils utils = new NetUtils(userId);
			VCVideoShortJSON shortDesc = utils.GetVideoShortInfo(id);
			
			if (shortDesc == null) {
				// Если какая-то ошибка при загрузке, оставим старый
			}
			else {
				// Если отличается от текущего положения дел, пора обновляться!
				if (!video.videoShort.equals(shortDesc)) {
					video.videoShort = shortDesc;
					lib.SaveLists();
					
					needUpdate = true;
				}
			}
			
			return video;
		}
		
		private String FormatDate(int timestamp) {
			Date date = new Date((long)timestamp * 1000);
			
			SimpleDateFormat dateFormat1 = new SimpleDateFormat("d ");
			SimpleDateFormat dateFormat2 = new SimpleDateFormat(" в H:mm");
			String[] month = {"января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"};
			String dateStr = dateFormat1.format(date) + month[date.getMonth()] + dateFormat2.format(date);
			
			// Yesterday
			Calendar c1 = Calendar.getInstance();
			c1.add(Calendar.DATE, -1);
			
			// Out date
			Calendar c2 = Calendar.getInstance();
			c2.setTime(date); // your date
			if (c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
					  && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)) {
				// Yesterday
				dateStr = "вчера" + dateFormat2.format(date);
			}
			
			Calendar c3 = Calendar.getInstance();
			if (c3.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
					  && c3.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)) {
				// Today
				dateStr = "сегодня" + dateFormat2.format(date);
			}
			
			return dateStr;
		}
		
		@Override
		protected void onPostExecute(VCVideo result) {
			if (activity != null && activity.get() != null) {
				Assert.assertNotNull(result);
				Assert.assertNotNull(result.videoShort);
				Assert.assertTrue(result.video.isCorrect());
				
				activity.get().video = result;
				
				// Броадкаст
				if (needUpdate) {
					Intent intent = new Intent(VideoListActivity.UPDATE_BROADCAST)
						.putExtra("id", result.videoShort.id)
						.putExtra("set_likes", result.videoShort.likes)
						.putExtra("set_views", result.videoShort.views)
						.putExtra("set_ilike", result.videoShort.ilike);
					
					activity.get().sendBroadcast(intent);
				}
				
				// Установить данные
				// Приватное?
				activity.get().findViewById(R.id.lockImageView).setVisibility(
						result.video.isPrivate ? View.VISIBLE : View.GONE);
				
				// Лайки
				TextView likes = (TextView)activity.get().findViewById(R.id.likesTextView);
				likes.setText(Integer.toString(result.videoShort.likes));
				
				// Если уже лайкнуто мной, поменять картинку
				if (result.videoShort.ilike) {
					ImageView likeImg = (ImageView)activity.get().findViewById(R.id.likesButton);
					likeImg.setImageResource(R.drawable.like_p);
				}
				
				// Просмотры
				TextView views = (TextView)activity.get().findViewById(R.id.viewsTextView);
				views.setText(Integer.toString(result.videoShort.views));
				
				// Дата добавления
				TextView createDate = (TextView)activity.get().findViewById(R.id.tvCreateDate);
				createDate.setText("Добавлено " + FormatDate(result.video.createDate));
				
				// Жанры
				StringBuilder builder = new StringBuilder();
				for (int jenre : result.video.jenres) {
					builder.append(VideoUtils.GetJenreById(jenre));
					builder.append(", ");
				}
				
				TextView jenres = (TextView)activity.get().findViewById(R.id.tvJenres);
				
				if (builder.length() > 1) {
					builder.deleteCharAt(builder.length()-1);
					builder.deleteCharAt(builder.length()-1);
					
					jenres.setText(builder.toString());
				}
				else {
					jenres.setVisibility(View.GONE);
				}
				
				
				// Страны и год
				if (builder.length() > 0)
					builder.delete(0, builder.length());
				
				for (int country : result.video.countries) {
					builder.append(VideoUtils.GetCountryById(country));
					builder.append(", ");
				}
				
				if (builder.length() > 1) {
					builder.deleteCharAt(builder.length()-1);
					builder.deleteCharAt(builder.length()-1);
				}
				
				if (result.video.year > 0) {
					if (builder.length() > 0)
						builder.append(", " + result.video.year);
					else
						builder.append(result.video.year);
					
					builder.append(" год.");
				}
				
				TextView year = (TextView)activity.get().findViewById(R.id.tvYear);
				if (builder.length() > 0)
					year.setText(builder.toString());
				else
					year.setVisibility(View.GONE);
				
				// Описание
				TextView desc = (TextView)activity.get().findViewById(R.id.tvDesc);
				if (result.video.desc.length() != 0)
					desc.setText(result.video.desc);
				else
					desc.setVisibility(View.GONE);
				
				// Режиссеры
				TextView directors = (TextView)activity.get().findViewById(R.id.tvDirectors);
				if (result.video.directors.length() != 0)
					directors.setText(result.video.directors);
				else {
					activity.get().findViewById(R.id.directorsLayout).setVisibility(View.GONE);
				}
				
				// Актеры
				TextView actors = (TextView)activity.get().findViewById(R.id.tvActors);
				if (result.video.actors.length() != 0)
					actors.setText(result.video.actors);
				else
					activity.get().findViewById(R.id.actorsLayout).setVisibility(View.GONE);
				
				// Скрываем анимашку
				activity.get().HideInfoProgressAnimation();
				
				// Установить меню
				activity.get().supportInvalidateOptionsMenu();
			}
		}
	}
}
