package com.example.vcmobile.core;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.example.vcmobile.AuthActivity;
import com.example.vcmobile.MenuFragment;
import com.example.vcmobile.R;
import com.example.vcmobile.VideoListActivity;
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.SlidingMenu.OnClosedListener;
import com.slidingmenu.lib.SlidingMenu.OnOpenListener;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

public class Utils {
	public static void InitUI(final SlidingFragmentActivity a, String actionBarTitle) {
		// Боковое меню
		a.setBehindContentView(R.layout.activity_menu);
		
		a.getSupportFragmentManager()
		.beginTransaction()
		.replace(R.id.menu_frame, new MenuFragment())
		.commit();
		
		//a.getSlidingMenu().setOnOpenedListener(new MenuOnOpenListener(a));
		SetupMenu(a);
		a.getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		a.getSlidingMenu().setBehindOffset(70);
		a.getSlidingMenu().setShadowDrawable(R.drawable.shadow);
		a.getSlidingMenu().setShadowWidth(10);
		a.getSlidingMenu().setBehindScrollScale(0.0f);
		a.getSlidingMenu().setFadeEnabled(false);
		
		a.getSlidingMenu().setOnClosedListener(new OnClosedListener() {
			
			@Override
			public void onClosed() {
				// TODO Auto-generated method stub
				// Снимаем выделение со всех
				ListView v = (ListView)a.findViewById(android.R.id.list);
				if (v != null)
					v.setItemChecked(-1, true);

			}
		});
		
		a.getSlidingMenu().setOnOpenListener(new OnOpenListener() {
			
			@Override
			public void onOpen() {
				// TODO Auto-generated method stub
				SetupDate(a);
			}
		});
		
		// Action Bar
		ActionBar ab = a.getSupportActionBar();
		ab.setTitle(actionBarTitle);
	}
	
	public static void SetTitle(SlidingFragmentActivity a, String title) {
		ActionBar ab = a.getSupportActionBar();
		ab.setTitle(title);
	}
	
	private static void SetupMenu(final SlidingFragmentActivity context) {
		SharedPreferences settings = context.getSharedPreferences(VkApp.VK_SESSION_FILE_NAME, Context.MODE_PRIVATE);
		SharedPreferences libSettings = context.getSharedPreferences(VideoLib.INFO_FILENAME, Context.MODE_PRIVATE);
		
		// Avatar
		ImageView avatarImageView = (ImageView)context.findViewById(R.id.avatarImageView);
		Bitmap b = BitmapFactory.decodeFile(VkApp.AVATAR_FILE_NAME);
		if (b != null)
			avatarImageView.setImageBitmap(b);
		else {
			// Если нету аватара, оставим черный квадрат
		}
		
		// Name
		TextView nameTextView = (TextView)context.findViewById(R.id.userNameTextView);
		nameTextView.setText(settings.getString("VkUserName", ""));
		
		// OnClick для шапки
		View header = context.findViewById(R.id.menuHeaderLayout);
		header.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				context.startActivity(new Intent(context, VideoListActivity.class));
			}
		});
		
		// Last synchronization
		SetupDate(context);
	}
	
	private static void SetupDate(SlidingFragmentActivity context) {
		SharedPreferences libSettings = context.getSharedPreferences(VideoLib.INFO_FILENAME, Context.MODE_PRIVATE);
		Long dateMillis = libSettings.getLong(VideoLib.LAST_SYNCH_DATE, 1);
		SimpleDateFormat dateFormat1 = new SimpleDateFormat("d ");
		SimpleDateFormat dateFormat2 = new SimpleDateFormat(" в H:mm");
		String[] month = {"янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек"};
		Date date = new Date(dateMillis);
		
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
		
		
		TextView lastSynchTextView = (TextView)context.findViewById(R.id.LastSynchTextView);
		lastSynchTextView.setText("Обновлено " + dateStr);
	}
	
	public static void SetActionBarBackButton(SlidingFragmentActivity a) {
		ActionBar ab = a.getSupportActionBar();
		if (ab != null)
			ab.setDisplayHomeAsUpEnabled(true);
		
	}
	
	public static void InitHiddenUI(SlidingFragmentActivity a) {
		a.setBehindContentView(R.layout.activity_menu);
		
		// Скрыть UI
		ActionBar bar = a.getSupportActionBar(); 
		if (bar != null) bar.hide();
		
		// Запретить вызывать меню жестом
		a.getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_NONE);
	}
	
	public final static int ABOUT_MENU_ID = 545;
	public final static int EXIT_MENU_ID = 546;
	
	public static void SetupAppMenu(Menu menu) {
		menu.add(Menu.NONE, ABOUT_MENU_ID, Menu.NONE, "О программе")
			.setIcon(R.drawable.menu_info)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
	
		menu.add(Menu.NONE, EXIT_MENU_ID, Menu.NONE, "Выход")
			.setIcon(R.drawable.menu_close)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
	}
	
	public static void ShowAbout(SlidingFragmentActivity context) {
		String version = getVersion(context);
		int build = getVersionCode(context);
		
		new AlertDialog.Builder(context)
				.setTitle("VC Mobile")
				.setIcon(R.drawable.logo32)
				.setMessage("Видеокаталог.\nite.cloudapp.net\nВерсия " + version + ", сборка " + build + "\n\n\u00A9 2013. Матерков Максим, Хаукка Станислав.")
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
					}
				})
				.create().
				show();
	}
	
	public static int getVersionCode(Context context) {
		int v = 0;
		try {
			v = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
		} 
		catch (NameNotFoundException e) {
		}
		
		return v;
	}
	
	public static String getVersion(Context context) {
		String v = "";
		try {
			v = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
		} 
		catch (NameNotFoundException e) {
		}
		
		return v;
	}
	
	public static void MakeDirs() {
		File dir = new File(Environment.getExternalStorageDirectory() + "/VCMobile/");
		dir.mkdirs();
		
		dir = new File(Environment.getExternalStorageDirectory() + "/VCMobile/PreviewCache/");
		dir.mkdirs();
		
		dir = new File(Environment.getExternalStorageDirectory() + "/VCMobile/DBCache/");
		dir.mkdirs();
		
		dir = new File(Environment.getExternalStorageDirectory() + "/VCMobile/VideoCache/");
		dir.mkdirs();
		
		dir = new File(Environment.getExternalStorageDirectory() + "/VCMobile/DownloadTmp/");
		dir.mkdirs();
		
		dir = new File(Environment.getExternalStorageDirectory() + "/VCMobile/UploadTmp/");
		dir.mkdirs();
	}
	
	public static void Logout(Context context) {
		VkApp.Logout(context);
		
		ClearDirs();
	}
	
	public static void authFail(SlidingFragmentActivity activity) {
		VkApp.Logout(activity);
		
		Intent intent = new Intent(activity, AuthActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		
		activity.startActivity(intent);
		activity.finish();
	}
	
	// Стереть все
	private static void ClearDirs() {
		ClearDir(new File(Environment.getExternalStorageDirectory() + "/VCMobile/PreviewCache/"));
		ClearDir(new File(Environment.getExternalStorageDirectory() + "/VCMobile/DBCache/"));
		//ClearDir(new File(Environment.getExternalStorageDirectory() + "/VCMobile/VideoCache/"));
		ClearDir(new File(Environment.getExternalStorageDirectory() + "/VCMobile/DownloadTmp/"));
		ClearDir(new File(Environment.getExternalStorageDirectory() + "/VCMobile/UploadTmp/"));
	}
	
	private static void ClearDir(File dir) {
		String[] children = dir.list();
		for (int i = 0; i < children.length; i++) {
			new File(dir, children[i]).delete();
		}
	}
}
