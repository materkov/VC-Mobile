package com.example.vcmobile;

import java.lang.ref.WeakReference;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.view.MenuItem;
import com.example.vcmobile.core.NetUtils;
import com.example.vcmobile.core.Utils;
import com.example.vcmobile.core.VkApp;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

public class YouTubeUploadActivity extends SlidingFragmentActivity {
	private SendTask asyncTask;
	private ProgressDialog pd;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_you_tube_upload);
		Utils.InitUI(this, "Скачать с YouTube");
		Utils.SetActionBarBackButton(this);
		
		asyncTask = (SendTask)getLastCustomNonConfigurationInstance();
		if (asyncTask != null && asyncTask.getStatus() != AsyncTask.Status.FINISHED) {
			asyncTask.activity = new WeakReference<YouTubeUploadActivity>(this);
			pd = ProgressDialog.show(this, "", "Идет загрузка ...", true, false);
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (asyncTask != null) asyncTask.activity = null;
	}
	
	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		return asyncTask;
	}
	
	public void onOKClick(View v) {
		// Hide keyboard
		InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
		
		// Создать asyncTask
		asyncTask = new SendTask();
		asyncTask.activity = new WeakReference<YouTubeUploadActivity>(this);
		asyncTask.userId = VkApp.GetUserID(this);

		EditText link = (EditText)findViewById(R.id.teYouTubeLink);
		asyncTask.link = link.getText().toString();
		
		asyncTask.execute();
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			onBackPressed();
			break;
		}

		return true;
	}
	

	private static class SendTask extends AsyncTask<Void, Void, Void> {
		public WeakReference<YouTubeUploadActivity> activity;
		public int userId;
		public String link;
		
		@Override
		protected void onPreExecute() {
			activity.get().pd = ProgressDialog.show(activity.get(), "", "Идет загрузка ...", true, false);
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			NetUtils utils = new NetUtils(userId);
			utils.UploadYouTube(link);
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if (activity != null && activity.get() != null) {
				Toast.makeText(activity.get(), "Видео будет загружено на сервер", Toast.LENGTH_SHORT).show();
				activity.get().onBackPressed();
			}
		}
		
	}
}
