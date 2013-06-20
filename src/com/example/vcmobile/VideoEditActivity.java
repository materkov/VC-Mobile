package com.example.vcmobile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Iterator;

import junit.framework.Assert;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vcmobile.core.NetUtils;
import com.example.vcmobile.core.UploadService;
import com.example.vcmobile.core.Utils;
import com.example.vcmobile.core.VCVideo;
import com.example.vcmobile.core.VCVideoJSON;
import com.example.vcmobile.core.VideoLib;
import com.example.vcmobile.core.VideoUtils;
import com.google.gson.Gson;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

public class VideoEditActivity extends SlidingFragmentActivity {
	private int type;		// 1 - new, 2 - edit
	private int videoId;
	private String path, uriS;
	
	//private 
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		type = getIntent().getIntExtra("type", -1);
		videoId = getIntent().getIntExtra("id", -1);
		path = getIntent().getStringExtra("path");
		uriS = getIntent().getStringExtra("uri");
		
		Assert.assertTrue(type == 1 || type == 2);
		Assert.assertTrue(path != null || uriS != null || type == 2);
		
		setContentView(R.layout.activity_video_edit);
		Utils.InitUI(this, (type == 1) ? "Добавить видео" : "Редактировать видео");
		Utils.SetActionBarBackButton(this);
		
		// Текст кнопки ОК
		Button okBtn = (Button)findViewById(R.id.editOKButton);
		okBtn.setText((type == 1) ? "Добавить" : "Обновить");
		
		if (type == 2) {
			Assert.assertTrue(videoId >= 0);
			
			// Если редактируем, загрузить инфу 
			VideoLib videoLib = new VideoLib();
			videoLib.Init();
			VCVideo video = videoLib.videosList.get(videoId);
			
			// И раскидать по полям:
			EditText teName = (EditText)findViewById(R.id.teName);
			teName.setText(video.video.title);
			
			EditText teDesc = (EditText)findViewById(R.id.teDesc);
			teDesc.setText(video.video.desc);
			
			EditText teYear = (EditText)findViewById(R.id.teYear);
			teYear.setText(Integer.toString(video.video.year));
			
			EditText teDirectors = (EditText)findViewById(R.id.teDirectors);
			teDirectors.setText(video.video.directors);
			
			EditText teActors = (EditText)findViewById(R.id.teActors);
			teActors.setText(video.video.actors);
			
			// Жанры
			TextView tveJenresHidden = (TextView)findViewById(R.id.tveJenresHidden);
			if (tveJenresHidden.getText().equals("")) {
				StringBuilder str = new StringBuilder();
				StringBuilder strH = new StringBuilder();
				
				for (int jenre : video.video.jenres) {
					strH.append(jenre);
					strH.append(" ");
					
					str.append(VideoUtils.GetJenreById(jenre) + "\n");
				}
				
				if (str.length() > 0)
					str.deleteCharAt(str.length()-1);
				if (strH.length() > 0)
					strH.deleteCharAt(strH.length()-1);
				
				tveJenresHidden.setText(strH.toString());
				
				TextView tveJenres = (TextView)findViewById(R.id.tveJenres);
				tveJenres.setText(str.toString());
				
				if (str.toString().equals(""))
					tveJenres.setText("(ничего не выбрано)");
			}
			
			// Страны
			TextView tveCountriesHidden = (TextView)findViewById(R.id.tveCountriesHidden);
			if (tveCountriesHidden.getText().equals("")) {
				StringBuilder str = new StringBuilder();
				StringBuilder strH = new StringBuilder();
				
				for (int country : video.video.countries) {
					strH.append(country);
					strH.append(" ");
					
					str.append(VideoUtils.GetCountryById(country) + "\n");
				}
				
				if (str.length() > 0)
					str.deleteCharAt(str.length()-1);
				if (strH.length() > 0)
					strH.deleteCharAt(strH.length()-1);
				
				tveCountriesHidden.setText(strH.toString());
				
				TextView tveCountries = (TextView)findViewById(R.id.tveCountries);
				tveCountries.setText(str.toString());
				
				if (str.toString().equals(""))
					tveCountries.setText("(ничего не выбрано)");
			}
			
			// Приватность
			CheckBox isPublic = (CheckBox)findViewById(R.id.publicCheckbox);
			isPublic.setChecked(!video.video.isPrivate);
			
			// Сркыть инфу о размере и формате
			View v = findViewById(R.id.newFileInfoLayout);
			v.setVisibility(View.GONE);
		}
		else {
			// Новое видео
			double size = 0;
			
			if (path != null) {
				// путь задан через path
				File f = new File(path);
				size = f.length();
			}
			else {
				// Путь задан через uri
				Uri uri = Uri.parse(uriS);
				try {
					AssetFileDescriptor as = getContentResolver().openAssetFileDescriptor(uri, "r");
					size = as.getLength();
				}
				catch (FileNotFoundException ex) {
				}
			}
			
			// Размер
			size = size / 1024 / 1024;	// В мегабайты
			size = Math.round(size*10.0) / 10.0;
			
			TextView tvSize = (TextView)findViewById(R.id.tvSize);
			tvSize.setText(Double.toString(size) + " МБ");
			
			// Имя
			SharedPreferences settings = getSharedPreferences("LastMyVideoId", Context.MODE_PRIVATE);
			int id = settings.getInt("id", 1);
			EditText teName = (EditText)findViewById(R.id.teName);
			teName.setText("Мое видео " + id);
			
			SharedPreferences.Editor _editor = settings.edit();
			_editor.putInt("id", id+1);
			_editor.commit();
		}
	}
	
	
	public void onJenresEditClick(View v) {
		final HashSet<Integer> selected = new HashSet<Integer>();
		
		TextView tv = (TextView)findViewById(R.id.tveJenresHidden);
		String str = tv.getText().toString();
		boolean start[] = new boolean[VideoUtils.jenres.length];
		if (str.length() > 0)
			for(String s : str.split(" ")) {
				int pos = Integer.parseInt(s);
				start[pos] = true;
				selected.add(pos);
			}
		
		new AlertDialog.Builder(this)
			.setTitle("Выберите жанры")
			.setMultiChoiceItems(VideoUtils.jenres, start,
				new DialogInterface.OnMultiChoiceClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which, boolean isChecked) {
						if (isChecked)
							selected.add(which);
						else
							selected.remove(which);
					}
				})
			.setPositiveButton("OK", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						StringBuilder str = new StringBuilder();
						StringBuilder strH = new StringBuilder();
						
						Iterator<Integer> iterator = selected.iterator(); 
						while (iterator.hasNext()){
							int jenre = iterator.next();
							
							str.append(VideoUtils.GetJenreById(jenre) + "\n");
							strH.append(jenre + " ");
						}
						
						if (str.length() > 0)
							str.deleteCharAt(str.length()-1);
						if (strH.length() > 0)
							strH.deleteCharAt(strH.length()-1);
						
						TextView tv = (TextView)findViewById(R.id.tveJenres);
						tv.setText(str.toString());
						
						if (str.toString().equals(""))
							tv.setText("(ничего не выбрано)");
						
						tv = (TextView)findViewById(R.id.tveJenresHidden);
						tv.setText(strH.toString());
					}
				})
			.create()
			.show();
	}
	
	public void onJenresClearButton(View v) {
		TextView t = (TextView)findViewById(R.id.tveJenres);
		t.setText("(ничего не выбрано)");
		
		t = (TextView)findViewById(R.id.tveJenresHidden);
		t.setText("");
	}
	
	public void onCountriesEditClick(View v) {
		final HashSet<Integer> selected = new HashSet<Integer>();
		
		TextView tv = (TextView)findViewById(R.id.tveCountriesHidden);
		String str = tv.getText().toString();
		boolean start[] = new boolean[VideoUtils.countries.length];
		if (str.length() > 0)
			for(String s : str.split(" ")) {
				int pos = Integer.parseInt(s);
				start[pos] = true;
				selected.add(pos);
			}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Выберите страны");
			builder.setMultiChoiceItems(VideoUtils.countries, start,
				new DialogInterface.OnMultiChoiceClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which, boolean isChecked) {
						if (isChecked)
							selected.add(which);
						else
							selected.remove(which);
					}
				});
			builder.setPositiveButton("OK", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						StringBuilder str = new StringBuilder();
						StringBuilder strH = new StringBuilder();
						
						Iterator<Integer> iterator = selected.iterator(); 
						while (iterator.hasNext()){
							int country = iterator.next();
							
							str.append(VideoUtils.GetCountryById(country) + "\n");
							strH.append(country + " ");
						}
						
						if (str.length() > 0)
							str.deleteCharAt(str.length()-1);
						if (strH.length() > 0)
							strH.deleteCharAt(strH.length()-1);
						
						TextView tv = (TextView)findViewById(R.id.tveCountries);
						tv.setText(str.toString());
						
						if (str.toString().equals(""))
							tv.setText("(ничего не выбрано)");
						
						tv = (TextView)findViewById(R.id.tveCountriesHidden);
						tv.setText(strH.toString());
					}
				});
			AlertDialog dialog = builder.create();
			dialog.getListView().setFastScrollEnabled(true);
			dialog.show();
	}
	
	public void onCountriesClearButton(View v) {
		TextView t = (TextView)findViewById(R.id.tveCountries);
		t.setText("(ничего не выбрано)");
		
		t = (TextView)findViewById(R.id.tveCountriesHidden);
		t.setText("");
	}
	

	@Override
	public boolean onMenuItemSelected(int featureId, com.actionbarsherlock.view.MenuItem item) {
		int itemId = item.getItemId();
		
		switch (itemId) {
			case android.R.id.home:
				onBackPressed();
				break;
		}

		return true;
	}
	
	public void onMoreClick(View v) {
		// Показать доп. инфо
		View view = findViewById(R.id.otherInfoLayout);
		view.setVisibility(View.VISIBLE);
		
		// Скрыть саму кнопку
		v.setVisibility(View.GONE);
	}
	
	private void BuildParams(VCVideoJSON video) {
		EditText teName = (EditText)findViewById(R.id.teName);
		video.title = teName.getText().toString();
				
		EditText teDesc = (EditText)findViewById(R.id.teDesc);
		video.desc = teDesc.getText().toString();
		
		EditText teYear = (EditText)findViewById(R.id.teYear);
		try {
			video.year = Integer.parseInt(teYear.getText().toString());
		}
		catch (NumberFormatException ex) {
			video.year = 0;
		}
		
		EditText teDirectors = (EditText)findViewById(R.id.teDirectors);
		video.directors = teDirectors.getText().toString();
		
		EditText teActors = (EditText)findViewById(R.id.teActors);
		video.actors = teActors.getText().toString();
		
		// Жанры
		TextView tveJenresHidden = (TextView)findViewById(R.id.tveJenresHidden);
		String jenresStr = tveJenresHidden.getText().toString();
		if (jenresStr.length() > 0) {
			String jenres[] = jenresStr.split(" ");
			video.jenres = new int[jenres.length];
			
			int i = 0;
			for (String jenre : jenres) {
				int jenreId = Integer.parseInt(jenre);
				video.jenres[i] = jenreId;
				i++;
			}
		}
		else {
			video.jenres = new int[0];
		}
		
		// Страны
		TextView tveCountriesHidden = (TextView)findViewById(R.id.tveCountriesHidden);
		String countriesStr = tveCountriesHidden.getText().toString();
		if (countriesStr.length() > 0) {
			String countries[] = countriesStr.split(" ");
			video.countries = new int[countries.length];
			
			int i = 0;
			for (String country : countries) {
				int countryId = Integer.parseInt(country);
				video.countries[i] = countryId;
				i++;
			}
		}
		else {
			video.countries = new int[0];
		}
		
		// Приватность
		CheckBox isPublic = (CheckBox)findViewById(R.id.publicCheckbox);
		video.isPrivate = !isPublic.isChecked();
	}
	
	public void onOKClick(View v) {
		if (type == 1) {
			// Добавить
			VCVideoJSON video = new VCVideoJSON();
			BuildParams(video);
			
			// Разрещение - особо
			/*
			if (uriS != null) {
				// Берем разрешение
				video.ext = VideoUtils.GetExtensionByUri(this, Uri.parse(uriS));
			}
			else {
				video.ext = VideoUtils.GetExtensionByName(path);
			}*/
			
			// Строим JSON-описание
			Gson gson = new Gson();
			String jsonStr = gson.toJson(video);
			
			Intent intent = new Intent(this, UploadService.class)
				.putExtra("name", video.title)
				.putExtra("desc", jsonStr);
			
			if (uriS != null)
				intent.putExtra("uri", uriS);
			else
				intent.putExtra("path", path);
	
			startService(intent);
			Toast.makeText(this, "Видео \"" + video.title + "\" выгружается на сервер ...", Toast.LENGTH_SHORT).show();
			
			startActivity(new Intent(this, DownloadsActivity.class));
			finish();
		}
		else {
			// Редактировать
			
			// Составляем JSON-объект
			VideoLib videoLib = new VideoLib();
			videoLib.Init();
			VCVideo video = videoLib.videosList.get(videoId);
			Assert.assertNotNull(video);
			Assert.assertTrue(video.isCorrect());
			
			BuildParams(video.video);
			
			// Сохраняем локально
			videoLib.SaveLists();
			
			// Отправляем на сервер
			new UploadInfoTask().execute(video);
			
			// Покаываем всплывающее сообщение
			Toast.makeText(this, "Информация обновлена.", Toast.LENGTH_SHORT).show();
			
			// Броадкаст
			sendBroadcast(new Intent(VideoListActivity.UPDATE_BROADCAST));
			
			// Переходим на страницу видео
			VideoUtils.StartVideoPageActivity(this, video.video, false);
			finish();
		}
	}
	
	private static class UploadInfoTask extends AsyncTask<VCVideo, Void, Void> {

		@Override
		protected Void doInBackground(VCVideo... params) {
			VCVideo video = params[0];
			
			Gson gson = new Gson();
			String desc = gson.toJson(video.video);
			
			NetUtils utils = new NetUtils(0);
			utils.UpdateVideoDesc(desc);
			return null;
		}
		
	}
}
