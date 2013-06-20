package com.example.vcmobile;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;

import com.actionbarsherlock.view.MenuItem;
import com.example.vcmobile.core.Utils;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

public class VideoUploadActivity extends SlidingFragmentActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_upload);
		Utils.InitUI(this, "Загрузить видео");
		Utils.SetActionBarBackButton(this);
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
	
	private static final int ACTION_TAKE_VIDEO = 3;
	private static final int ACTION_VIEW_FS = 1;

	public void onFSSelected(View v) {
		Intent intent = new Intent(this, FileSelectActivity.class);
		startActivityForResult(intent, ACTION_VIEW_FS);
	}
	
	private void StartUpload(String type, String path) {
		// Показать окошко, где надо задать параметры
		Intent intent = new Intent(this, VideoEditActivity.class)
			.putExtra("type", 1)
			.putExtra(type, path);
		
		startActivity(intent);
		finish();
	}
		
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ACTION_VIEW_FS && data != null) {
			String path = data.getStringExtra("path");
			StartUpload("path", path);
		}
		else if (requestCode == ACTION_TAKE_VIDEO && resultCode == RESULT_OK) {
			Uri videoUri = data.getData();
			StartUpload("uri", videoUri.toString());
		}
		else
			super.onActivityResult(requestCode, resultCode, data);
	}
	
	/** Check if this device has a camera */
	private boolean checkCameraHardware() {
		return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
	}

	public void onCameraClick(View v) {
		if (checkCameraHardware()) {
			Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
			startActivityForResult(takeVideoIntent, ACTION_TAKE_VIDEO);
		}
		else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			
			builder.setMessage("На вашем устройстве нет камеры.")
				   .setPositiveButton("OK", new DialogInterface.OnClickListener() {
					   public void onClick(DialogInterface dialog, int id) {
					   }
				   })
				   .create()
				   .show();
		}
	}
	
	public void onYoutubeClick(View v) {
		Intent intent = new Intent(this, YouTubeUploadActivity.class);
		startActivity(intent);
		finish();
	}
}
