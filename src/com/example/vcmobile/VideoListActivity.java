package com.example.vcmobile;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.example.vcmobile.core.SuggestionProvider;
import com.example.vcmobile.core.Utils;
import com.example.vcmobile.core.VCLog;
import com.example.vcmobile.core.VCVideo;
import com.example.vcmobile.core.VideoLib;
import com.example.vcmobile.core.VkApp;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

public class VideoListActivity extends SlidingFragmentActivity {
	private VideoLib videoLib;
	private LoadListTask asyncTask;
	
	private int sortCriterion = 0;
	private String searchCriterion = null;
	
	// Фильтры
	private String	filterDirector = null;
	private String	filterActor = null;
	private int	filterCountry = -1;
	private int	filterJenre = -1;
	
	private BroadcastReceiver br;
	public final static String UPDATE_BROADCAST = "com.example.vcmobile.listupdate";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		VCLog.write("VideoListActivity. onCreate");
		
		setContentView(R.layout.activity_video_list);
		Utils.InitUI(this, "Каталог");
		Utils.SetActionBarBackButton(this);
		
		SetupTabFragments();	// Вкладки
		
		// Проверяем тип Intent
		if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) { 
			// Берем строку запроса из экстры
			String query = getIntent().getStringExtra(SearchManager.QUERY);

			// Выполняем поиск
			searchCriterion = query;
			
			// Установить заголовок
			Utils.SetTitle(this, "Поиск: " + searchCriterion);
			
			// Записываем в историю
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, SuggestionProvider.AUTHORITY, SuggestionProvider.MODE);
			suggestions.saveRecentQuery(searchCriterion, null);
		}
		
		// Есть ли флажок синхронизировать в интенте?
		Bundle bundle = getIntent().getExtras();
		boolean needSyncIntent = (bundle != null) && bundle.containsKey("needSync");
		getIntent().removeExtra("needSync");
		
		VCLog.write("needSyncIntent = " + needSyncIntent);
		
		// Есть ли сохраненный прогресс?
		if (savedInstanceState != null && savedInstanceState.containsKey("progress")) {
			TextView tv = (TextView)findViewById(R.id.listProgress);
			tv.setText(savedInstanceState.getString("progress"));
		}
		
		// Есть ли сохраненный кртиерий?
		if (savedInstanceState != null) {
			sortCriterion = savedInstanceState.getInt("sortCriterion");
		}
		
		// Восстанавливаем AsyncTask
		asyncTask = (LoadListTask)getLastCustomNonConfigurationInstance();
		if (asyncTask != null) {
			asyncTask.activity = new WeakReference<VideoListActivity>(this);
			
			VCLog.write("asyncTask != null");
			
			if (asyncTask.getStatus() == Status.FINISHED) {
				// Уже закончен
				HideProgressAnimation();
				videoLib = asyncTask.savedLib;
				SetupAdapters();
				
				VCLog.write("asyncTask.getStatus() == Status.FINISHED");
			}
			else {
				// Еще в процессе
				ShowProgressAnimation();
				
				VCLog.write("asyncTask.getStatus() != Status.FINISHED");
			}
		}
		else {
			// Нет предыдцщего таска
			asyncTask = new LoadListTask(this);
			asyncTask.execute(needSyncIntent);
			ShowProgressAnimation();
			
			VCLog.write("asyncTask == null");
		}
		
		// Слушаем подсказки что надо перегрузить библиотеку (локально!)
		br = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (videoLib == null)
					return;
				
				if (intent.getExtras() == null) {
					// Если параметров нет, просто перегружаем все!
					videoLib.Init();
					SetupAdapters();
					
					return;
				}
				
				if (intent.getExtras().containsKey("set_views")) {
					int views = intent.getIntExtra("set_views", 0);
					int vid = intent.getIntExtra("id", 0);
					VCVideo video = videoLib.videosList.get(vid);
					if (video != null)
						video.videoShort.views = views;
					
					adapterALL.notifyDataSetChanged();
					adapterMY.notifyDataSetChanged();
				}
				
				if (intent.getExtras().containsKey("set_likes")) {
					int likes = intent.getIntExtra("set_likes", 0);
					int vid = intent.getIntExtra("id", 0);
					VCVideo video = videoLib.videosList.get(vid);
					if (video != null)
						video.videoShort.likes = likes;
					
					adapterALL.notifyDataSetChanged();
					adapterMY.notifyDataSetChanged();
				}
				
				if (intent.getExtras().containsKey("set_ilike")) {
					boolean ilike = intent.getBooleanExtra("set_ilike", false);
					int vid = intent.getIntExtra("id", 0);
					
					VCVideo video = videoLib.videosList.get(vid);
					if (video != null)
						video.videoShort.ilike = ilike;
					
					adapterALL.notifyDataSetChanged();
					adapterMY.notifyDataSetChanged();
				}
				
				if (intent.getExtras().containsKey("add_my_list")) {
					int videoId = intent.getIntExtra("add_my_list", 0);
					VCVideo video = videoLib.videosList.get(videoId);
					if (video != null) {
						video.isMy = true;
					}
					
					// Тут не получится вызвать notifyDataSetChanged
					// Т.к. изменяться не толко даннеы, но и их количество (в адаптере adapterMY)
					// Поэтому надо заново создавать адаптеры, ничего не подклаешь :(
					SetupAdapters();
				}
				
				if (intent.getExtras().containsKey("del_my_list")) {
					int videoId = intent.getIntExtra("del_my_list", 0);
					VCVideo video = videoLib.videosList.get(videoId);
					if (video != null) {
						video.isMy = false;
					}
					
					// Тут не получится вызвать notifyDataSetChanged
					// Т.к. изменяться не толко даннеы, но и их количество (в адаптере adapterMY)
					// Поэтому надо заново создавать адаптеры, ничего не подклаешь :(
					SetupAdapters();
				}
			}
		};
		registerReceiver(br, new IntentFilter(UPDATE_BROADCAST));
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		clearListSelection();
	}
	
	private void clearListSelection() {
		// Снимаем выделение со всех списков
		ListView v = (ListView)findViewById(R.id.listViewALL);
		if (v != null)
			v.setItemChecked(-1, true);

		v = (ListView)findViewById(R.id.listViewMY);
		if (v != null)
			v.setItemChecked(-1, true);
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		
		VCLog.write("VideoListActivity. onRestart");

		if (videoLib == null && asyncTask != null && asyncTask.getStatus() == Status.FINISHED) {
			// Это значит что при окончании обновления - активити была не активна
			videoLib = asyncTask.savedLib;
			SetupAdapters();
			HideProgressAnimation();
			
			VCLog.write("VideoListActivity. Recieved result, but UI not updated yet.");
		}
	}
	
	@Override
	protected void onDestroy() {
		VCLog.write("VideoListActivity. onDestroy");
		
		unregisterReceiver(br);

		if (asyncTask != null) asyncTask.activity = null;
		super.onDestroy();
	}
	
	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		return asyncTask;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("progress", (String)((TextView)findViewById(R.id.listProgress)).getText());
		outState.putInt("sortCriterion", sortCriterion);
		
		super.onSaveInstanceState(outState);
	}

	
	
	// Синхронизировать
	public void Synch() {
		VCLog.write("VideoListActivity. Synch");
		if (asyncTask == null || asyncTask.getStatus() == Status.FINISHED) {
			VCLog.write("VideoListActivity. Synchrinizing...");
			
			asyncTask = new LoadListTask(this);
			asyncTask.execute(true);
			ShowProgressAnimation();
			videoLib = null;
		}
		else
			VCLog.write("VideoListActivity. Don't need to synch.");
	}
	
	private void ShowProgressAnimation() {
		// Скрыть основной контент
		View replaceView = findViewById(R.id.replaceView);
		replaceView.setVisibility(View.GONE);
		
		// Показать анимашку
		Animation hyperspaceJump = AnimationUtils.loadAnimation(this, R.anim.progress_animation);
		ImageView v = (ImageView)findViewById(R.id.placeholderAnim);
		v.startAnimation(hyperspaceJump);

		findViewById(R.id.placeholderView).setVisibility(View.VISIBLE);
	}
	
	private void HideProgressAnimation() {
		// Показать основной контент
		View replaceView = findViewById(R.id.replaceView);
		replaceView.setVisibility(View.VISIBLE);
		
		// Скрыть анимашку
		ImageView v = (ImageView)findViewById(R.id.placeholderAnim);
		v.clearAnimation();
		
		findViewById(R.id.placeholderView).setVisibility(View.GONE);
	}
	
	private VCTabListener tabListener;
	private VideoListElementAdapter adapterALL, adapterMY;
	
	private static class DefaultSort implements Comparator<VCVideo> {
		@Override
		public int compare(VCVideo arg0, VCVideo arg1) {
			Integer d1 = arg0.video.createDate;
			Integer d2 = arg1.video.createDate;
			return d2.compareTo(d1);	// inverse порядок
		}
	}
	
	private static class TitleSort implements Comparator<VCVideo> {
		@Override
		public int compare(VCVideo arg0, VCVideo arg1) {
			return arg0.video.title.compareToIgnoreCase(arg1.video.title);
		}
	}
	
	private static class YearSort implements Comparator<VCVideo> {
		@Override
		public int compare(VCVideo arg0, VCVideo arg1) {
			Integer d1 = arg0.video.year;
			Integer d2 = arg1.video.year;
			return d2.compareTo(d1);	// inverse порядок
		}
	}
	
	private static class LikesSort implements Comparator<VCVideo> {
		@Override
		public int compare(VCVideo arg0, VCVideo arg1) {
			Integer d1 = arg0.videoShort.likes;
			Integer d2 = arg1.videoShort.likes;
			return d2.compareTo(d1);	// inverse порядок
		}
	}
	
	private static class ViewsSort implements Comparator<VCVideo> {
		@Override
		public int compare(VCVideo arg0, VCVideo arg1) {
			Integer d1 = arg0.videoShort.views;
			Integer d2 = arg1.videoShort.views;
			return d2.compareTo(d1);	// inverse порядок
		}
	}
	
	private boolean isSearchMatch(VCVideo video) {
		if (searchCriterion == null)
			return true;
		
		String nameLower = video.video.title.toLowerCase();
		String qLower = searchCriterion.toLowerCase();
		
		return nameLower.contains(qLower);
	}
	
	private boolean isFilterMatch(VCVideo video) {
		if (filterActor != null) {
			if (!video.video.actors.toLowerCase().contains(filterActor.toLowerCase())) return false;
		}
		
		if (filterDirector != null) {
			if (!video.video.directors.toLowerCase().contains(filterDirector.toLowerCase())) return false;
		}
		
		if (filterJenre != -1) {
			boolean present = false;
			for (int jenre : video.video.jenres)
				if (jenre == filterJenre) {
					present = true;
					break;
				}
			
			if (!present) return false;
		}
		
		if (filterCountry != -1) {
			boolean present = false;
			for (int country : video.video.countries)
				if (country == filterCountry) {
					present = true;
					break;
				}
			
			if (!present) return false;
		}
		
		return true;
	}
	
	private void SetupAdapters() {
		if (videoLib == null) {
			// Если библиотека пуста, значит, была ошибка.
			adapterALL = new VideoListElementAdapter(this, new VCVideo[0], null);
			adapterMY = new VideoListElementAdapter(this, new VCVideo[0], null);
			
			tabListener.fAll.SetAdapter(adapterALL);
			tabListener.fMy.SetAdapter(adapterMY);
			
			return;
		}
		
		ArrayList<VCVideo> dataALLList = new ArrayList<VCVideo>();
		ArrayList<VCVideo> dataMYList = new ArrayList<VCVideo>();
		
		for (VCVideo entry : videoLib.videosList.values()) {
			if (!isSearchMatch(entry) || !isFilterMatch(entry)) continue;
			
			dataALLList.add(entry);

			if (entry.isMy)
				dataMYList.add(entry);
		}
		
		// Сортировка
		switch (sortCriterion) {
		case 0:
			Collections.sort(dataALLList, new DefaultSort());
			Collections.sort(dataMYList, new DefaultSort());
			break;
		case 1:
			Collections.sort(dataALLList, new TitleSort());
			Collections.sort(dataMYList, new TitleSort());
			break;
		case 2:
			Collections.sort(dataALLList, new YearSort());
			Collections.sort(dataMYList, new YearSort());
			break;
		case 3:
			Collections.sort(dataALLList, new ViewsSort());
			Collections.sort(dataMYList, new ViewsSort());
			break;
		case 4:
			Collections.sort(dataALLList, new LikesSort());
			Collections.sort(dataMYList, new LikesSort());
			break;
		}
		
		VCVideo[] dataALL = dataALLList.toArray(new VCVideo[0]);
		VCVideo[] dataMY = dataMYList.toArray(new VCVideo[0]);

		// Устанавливаем адаптеры
		adapterALL = new VideoListElementAdapter(this, dataALL, searchCriterion);
		adapterMY = new VideoListElementAdapter(this, dataMY, searchCriterion);
		
		tabListener.fAll.SetSearchMode(searchCriterion != null || hasFilter());
		tabListener.fAll.SetAdapter(adapterALL);
		
		tabListener.fMy.SetSearchMode(searchCriterion != null || hasFilter());
		tabListener.fMy.SetAdapter(adapterMY);
	}
	
	private void SetupTabFragments() {
		// Устанавливаем вкладки
		ActionBar bar = getSupportActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		//bar.removeAllTabs();

		ActionBar.Tab tab1 = bar.newTab();
		ActionBar.Tab tab2 = bar.newTab();
		tab1.setText(" ОБЩЕЕ ");
		tab2.setText(" МОЕ ");
		tabListener = new VCTabListener();
		tab1.setTabListener(tabListener);
		tab2.setTabListener(tabListener);
		bar.addTab(tab1);
		bar.addTab(tab2);
	}
	
	private boolean hasFilter() {
		return filterDirector != null || filterActor != null || 
				filterCountry != -1 || filterJenre != -1; 
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Поиск
		menu.add(Menu.NONE, 3, Menu.NONE, "Поиск")
			.setIcon(R.drawable.search)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		
		// Сортировка
		SubMenu m = menu.addSubMenu("Сортировка");
		m.add(Menu.NONE, 500, Menu.NONE, "По дате добавления");
		m.add(Menu.NONE, 501, Menu.NONE, "По названию");
		m.add(Menu.NONE, 502, Menu.NONE, "По году");
		m.add(Menu.NONE, 503, Menu.NONE, "По числу просмотров");
		m.add(Menu.NONE, 504, Menu.NONE, "По числу лайков");
		m.add(Menu.NONE, 505, Menu.NONE, "Отфильтровать...");

		
		MenuItem subMenu1Item = m.getItem();
		int icon = R.drawable.sort_default;
		switch (sortCriterion) {
		case 1:
			icon = R.drawable.sort_name;
			break;
		case 2:
			icon = R.drawable.sort_year;
			break;
		case 3:
			icon = R.drawable.sort_views;
			break;
		case 4:
			icon = R.drawable.sort_likes;
			break;
		default:
			break;
		}
		subMenu1Item.setIcon(icon);
		subMenu1Item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		
		// Фильтр 
		if (hasFilter()) {
			menu.add(Menu.NONE, 4, Menu.NONE, "Фильтр")
				.setIcon(R.drawable.filter_use)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		}

		Utils.SetupAppMenu(menu);
		
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, com.actionbarsherlock.view.MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			if (searchCriterion == null)
				getSlidingMenu().toggle();
			else
				onBackPressed();
			break;
			
		case 3:
			// Поиск
			onSearchRequested();
			break;
			
		case 4:
		case 505:
			// Фильтры
			Intent intent = new Intent(this, VideoFilterActivity.class);
			intent.putExtra("actor", filterActor == null ? "" : filterActor);
			intent.putExtra("director", filterDirector == null ? "" : filterDirector);
			intent.putExtra("jenre", filterJenre);
			intent.putExtra("country", filterCountry);
			
			startActivityForResult(intent, 1);
			break;
			
		// Сортировки:
		case 500:
			if (sortCriterion != 0) {
				sortCriterion = 0;
				SetupAdapters();
				supportInvalidateOptionsMenu();
			}
			break;
			
		case 501:
			if (sortCriterion != 1) {
				sortCriterion = 1;
				SetupAdapters();
				supportInvalidateOptionsMenu();
			}
			break;
			
		case 502:
			if (sortCriterion != 2) {
				sortCriterion = 2;
				SetupAdapters();
				supportInvalidateOptionsMenu();
			}
			break;
			
		case 503:
			if (sortCriterion != 3) {
				sortCriterion = 3;
				SetupAdapters();
				supportInvalidateOptionsMenu();
			}
			break;
			
		case 504:
			if (sortCriterion != 4) {
				sortCriterion = 4;
				SetupAdapters();
				supportInvalidateOptionsMenu();
			}
			break;
			
		case Utils.ABOUT_MENU_ID:
			Utils.ShowAbout(this);
			break;
			
		case Utils.EXIT_MENU_ID:
			finish();
			break;
		}

		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data == null) {
			// nothing
		}
		else {
			// Получить результат от фильтра
			filterActor = data.getStringExtra("actor");
			filterDirector = data.getStringExtra("director");
			filterJenre = data.getIntExtra("jenre", -1);
			filterCountry = data.getIntExtra("country", -1);
			
			if (filterActor != null && filterActor.equals("")) 
				filterActor = null;
			if (filterDirector != null && filterDirector.equals("")) 
				filterDirector = null;
			
			// Может измениться количество элементов, так что
			// перестраиваем все заново
			SetupAdapters();
			supportInvalidateOptionsMenu();
		}
	}
	
	private class VCTabListener implements ActionBar.TabListener
	{
		VideoListFragmentAll fAll;
		VideoListFragmentMy fMy;
		
		public VCTabListener() {
			fAll = new VideoListFragmentAll();
			fMy = new VideoListFragmentMy();
		}
		
		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			if (tab.getPosition() == 0) {
				ft.replace(R.id.replaceView, fAll);
			} 
			else {
				ft.replace(R.id.replaceView, fMy);
			}
		}
		
		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		}
		
		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}
	}
	
	static class LoadListTask extends AsyncTask<Object, Integer, Boolean> {
		private WeakReference<VideoListActivity> activity;
		private boolean needSync;
		private int userId;
		private VideoLib savedLib = null;

		public LoadListTask(VideoListActivity _activity) {
			this.activity = new WeakReference<VideoListActivity>(_activity);
			userId = VkApp.GetUserID(_activity);
		}
		
		@Override
		protected Boolean doInBackground(Object... params) {
			needSync = (Boolean)params[0];

			ReportProgress(1);
			VCLog.write("LoadListTask.doInBackground. Report 1");
			
			// Загружаем списки
			VideoLib lib = new VideoLib();
			boolean success = lib.Init();

			// Если ошибки при загрузке списка с телефона, грузим заново с сервера
			if (!success) needSync = true;
			
			ReportProgress(2);
			VCLog.write("LoadListTask.doInBackground. Report 2. lib.Init() == " + success);
			
			if (needSync) {
				// Если надо синхронизоваться, грузим с сервера
				try {
					lib.Synchronize(userId);
				}
				catch (VideoLib.LibSynchException ex) {
					// Т.е. просто будет возвращена только что загруженная с телефона библиотека
					VCLog.write(ex, "LoadListTask.doInBackground. LibSynchException");

					if (lib.isCorrect())
						savedLib = lib;
					else {
						// В процессе загрузки какие-то ошибки
						savedLib = new VideoLib();
						savedLib.Init();
					}
					
					
					return false;
				}

				ReportProgress(3);
				VCLog.write("LoadListTask.doInBackground. Report 3.");
				
				lib.SaveLists();
				
				// Сохраняем дату синхронизации
				if (activity != null && activity.get() != null) {
					SharedPreferences settings = activity.get().getSharedPreferences(VideoLib.INFO_FILENAME, Context.MODE_PRIVATE);
					SharedPreferences.Editor _editor = settings.edit();
					_editor.putLong(VideoLib.LAST_SYNCH_DATE, System.currentTimeMillis());
					_editor.commit();
					
					VCLog.write("LoadListTask.doInBackground. Saved LAST_SYNCH_DATE.");
				}
			}
			ReportProgress(4);
			
			if (needSync && activity != null && activity.get() != null) {
				// Если надо синхронизоваться, заожно запросим инфу из вконтакта (вдруг изменилась)
				boolean result = VkApp.UpdateInfo(activity.get());
				
				VCLog.write("LoadListTask.doInBackground. Updated VK info. Result = " + result);
			}
			
			ReportProgress(5);
			savedLib = lib;
			
			return true;
		}
		
		private void ReportProgress(int progress) {
			if (needSync) {
				// публиковать прогресс будет только если нет принудительного обновления
				// т.к. в жтом случае все происходит быстро, и прогресс получается очень дерганный
				// Лучше пусть там так и будет написана старая надпись.
				publishProgress(progress);
			}
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			
			if (activity != null && activity.get() != null) {
				TextView tv = (TextView)activity.get().findViewById(R.id.listProgress);
				tv.setText("Идет загрузка (" + Integer.toString(values[0]) + "/5) ...");
			}
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			
			if (activity != null && activity.get() != null) {
				activity.get().videoLib = savedLib;
				activity.get().HideProgressAnimation();
				activity.get().SetupAdapters();
				
				if (result) {
					if (needSync)
						Toast.makeText(activity.get(), "Каталог синхронизирован.", Toast.LENGTH_SHORT).show();
				}
				else
					Toast.makeText(activity.get(), "Ошибка синхронизации с сервером.", Toast.LENGTH_SHORT).show();
				
				VCLog.write("LoadListTask.onPostExecute. Doing...., Result = " + result);
			}
			else {
				// Если активити уже нету, значит результат просто теряется....
				VCLog.write("LoadListTask.onPostExecute. Doing nothing...., because no context");
			}

		}
	}
}