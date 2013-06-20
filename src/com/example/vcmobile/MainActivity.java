package com.example.vcmobile;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import com.example.vcmobile.core.Utils;
import com.example.vcmobile.core.VkApp;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

public class MainActivity extends SlidingFragmentActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		Utils.InitHiddenUI(this);
		
		// Проверить, есть ли SD карта вообще
		boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
		if (!isSDPresent) {
			new AlertDialog.Builder(this)
					.setMessage("На вашем телефоне нет SD карты. Приложение не может работать без нее :(")
					.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,	int id) {
									finish();
								}
							})
					.create()
					.show();
			
			return;
		}
		
		// Создаем директории на SD карте для нашего приложения (если нету)
		Utils.MakeDirs();
		
		SharedPreferences settings = getSharedPreferences(VkApp.VK_SESSION_FILE_NAME, Context.MODE_PRIVATE);
		if (settings.contains("VkAccessToken")) {
			// Юзер уже авторизован, сразу идем на страницу авторизации, не показываем ничего на этой активити
			//startActivity(new Intent(this, AuthActivity.class));

			// Сразу идем в каталог!
			startActivity(new Intent(this, VideoListActivity.class));
			finish();
		}
	}
	
	public void onAuthClick(View w) {
		startActivity(new Intent(this, AuthActivity.class));
		finish();
	}
}