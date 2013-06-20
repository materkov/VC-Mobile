package com.example.vcmobile;

import java.lang.ref.WeakReference;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.app.ActionBar;
import com.example.vcmobile.core.VCLog;
import com.example.vcmobile.core.VkApp;
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

public class AuthActivity extends SlidingFragmentActivity {
	private ProgressDialog mSpinner;
	private boolean isFirstTime = false;
	private WebView webView;
	
	private GetInfoTask asyncTask;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_auth);
		setBehindContentView(R.layout.activity_menu);
		
		// Запретить вызывать меню жестом
		getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_NONE);
		
		ActionBar bar = getSupportActionBar();
		if (bar != null)
			bar.hide();
		
		mSpinner = ProgressDialog.show(this, "", "Авторизация VK ...", true, false);
		
		webView = (WebView)findViewById(R.id.webView1);
		
		
		webView.setWebViewClient(new VkWebViewClient());
		//webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setDefaultTextEncodingName("utf-8");
		webView.getSettings().setSavePassword(false);
		webView.getSettings().setSaveFormData(false);
		webView.loadUrl(VkApp.OAUTH_AUTHORIZE_URL);
		
		// Retain task
		asyncTask = (GetInfoTask)getLastCustomNonConfigurationInstance();
		if (asyncTask != null)
			asyncTask.activity = new WeakReference<AuthActivity>(this);
	}
	
	@Override
	protected void onDestroy() {
		if (asyncTask != null) 
			asyncTask.activity = null;
		
		mSpinner.dismiss();
		
		super.onDestroy();
	}
	
	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		return asyncTask;
	}
	
	private void AuthSuccess(String url) {
		mSpinner.setMessage("Загрузка информации...");

		isFirstTime = !VkApp.haveSavedSession(this);
		
		// Получить данные о юзере
		asyncTask = new GetInfoTask();
		asyncTask.activity = new WeakReference<AuthActivity>(this);
		asyncTask.execute(url);
	}
	
	private void ErrorRecieved(String desc) {
		webView.setVisibility(View.INVISIBLE);
		
		VCLog.write("AuthActivity.ErrorRecieved. desc = [" + desc + "].");
		
		// Use the Builder class for convenient dialog construction
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		builder.setMessage("Не удалось подключиться к серверу ВКонтакте.")
			   .setPositiveButton("OK", new DialogInterface.OnClickListener() {
				   public void onClick(DialogInterface dialog, int id) {
					   finish();
				   }
			   })
			   .create()
			   .show();
	}
	
	class GetInfoTask extends AsyncTask {

		public WeakReference<AuthActivity> activity;
		
		@Override
		protected Object doInBackground(Object... arg) {
			String url = (String)arg[0];
			
			return VkApp.Login(AuthActivity.this, url);
		}
		
		@Override
		protected void onPostExecute(Object result) {
			if ((Boolean)result) {
				Intent intent = new Intent(AuthActivity.this, VideoListActivity.class);
				
				if (isFirstTime) {
					intent.putExtra("needSync", true);
				}
				
				// перейти в каталог
				startActivity(intent);
				finish();
			}
			else {
				if (activity == null || activity.get() == null) return;
				
				AlertDialog.Builder builder = new AlertDialog.Builder(activity.get());
				
				builder.setMessage("Ошибка авторизации ВКонтакте.")
				   .setPositiveButton("OK", new DialogInterface.OnClickListener() {
					   public void onClick(DialogInterface dialog, int id) {
						   finish();
					   }
				   })
				   .create()
				   .show();
			}
		}
	}
	
	
	class VkWebViewClient extends WebViewClient {
		
		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
			// i.e. - NO INTENNET CONNECTION!
			super.onReceivedError(view, errorCode, description, failingUrl);
			ErrorRecieved(description);
			//VkDialog.this.dismiss();
		}
		
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			super.onPageStarted(view, url, favicon);
			
			Log.d("MyTag", "URL="+url);
			
			if( url.contains("error") ) {
				//VkDialog.this.dismiss();
				return;
			}
			else if(url.contains("access_token")) {
				// Скрыть противную страничку
				WebView webView = (WebView)findViewById(R.id.webView1);
				webView.setVisibility(View.INVISIBLE);
				
				AuthSuccess(url);
				return;
			}
			
			//if (!mSpinner.isShowing())
			mSpinner.show();
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			if (!url.contains("access_token"))
				mSpinner.dismiss();
		}
	}

}


