package com.example.vcmobile;

import java.lang.ref.WeakReference;

import junit.framework.Assert;
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

public class SendEmailActivity extends SlidingFragmentActivity {
	private int videoId;
	private SendTask asyncTask;
	private ProgressDialog pd;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_send_email);
		Utils.InitUI(this, "Отправить на почту");
		Utils.SetActionBarBackButton(this);
		
		Assert.assertNotNull(getIntent().getExtras());
		Assert.assertTrue(getIntent().getExtras().containsKey("id"));
		
		videoId = getIntent().getIntExtra("id", 0);
		asyncTask = (SendTask)getLastCustomNonConfigurationInstance();
		if (asyncTask != null && asyncTask.getStatus() != AsyncTask.Status.FINISHED) {
			asyncTask.activity = new WeakReference<SendEmailActivity>(this);
			pd = ProgressDialog.show(this, "", "Идет отправка ...", true, false);
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
		asyncTask.activity = new WeakReference<SendEmailActivity>(this);
		asyncTask.userId = VkApp.GetUserID(this);
		asyncTask.videoId = videoId;
		
		EditText email = (EditText)findViewById(R.id.teEmail);
		asyncTask.email = email.getText().toString();
		
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
		public WeakReference<SendEmailActivity> activity;
		public int userId, videoId;
		public String email;
		
		@Override
		protected void onPreExecute() {
			activity.get().pd = ProgressDialog.show(activity.get(), "", "Идет отправка ...", true, false);
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			NetUtils utils = new NetUtils(userId);
			utils.SendEmail(email, videoId);
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if (activity != null && activity.get() != null) {
				Toast.makeText(activity.get(), "Видео отправлено на почту", Toast.LENGTH_SHORT).show();
				activity.get().onBackPressed();
			}
		}
		
	}
}
