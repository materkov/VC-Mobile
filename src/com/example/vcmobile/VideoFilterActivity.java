package com.example.vcmobile;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.example.vcmobile.core.Utils;
import com.example.vcmobile.core.VideoUtils;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

public class VideoFilterActivity extends SlidingFragmentActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_video_filter);
		Utils.InitUI(this, "Установить фильтр");
		Utils.SetActionBarBackButton(this);
		
		// Режиссер
		// Режиссер
		EditText et = (EditText)findViewById(R.id.editText1);
		et.setText(getIntent().getStringExtra("director"));
		
		// Актер
		et = (EditText)findViewById(R.id.editText2);
		et.setText(getIntent().getStringExtra("actor"));
			
		// Страны
		Spinner spinner1 = (Spinner) findViewById(R.id.spinner1);
		List<String> list = new ArrayList<String>(VideoUtils.countries.length + 1);
		list.add("Любая страна");
		for (int i = 0; i < VideoUtils.countries.length; i++)
			list.add(VideoUtils.countries[i]);

		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner1.setAdapter(dataAdapter);
		
		spinner1.setSelection(getIntent().getIntExtra("country", -1) + 1);
		
		// Жанры
		Spinner spinner2 = (Spinner) findViewById(R.id.spinner2);
		list = new ArrayList<String>(VideoUtils.jenres.length + 1);
		list.add("Любой жанр");
		for (int i = 0; i < VideoUtils.jenres.length; i++)
			list.add(VideoUtils.jenres[i]);

		dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner2.setAdapter(dataAdapter);
		
		spinner2.setSelection(getIntent().getIntExtra("jenre", -1) + 1);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		return true;
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

	public void onClearClick(View v) {
		// Жанры
		Spinner spinner = (Spinner) findViewById(R.id.spinner2);
		spinner.setSelection(0);
		
		// Страны
		spinner = (Spinner)findViewById(R.id.spinner1);
		spinner.setSelection(0);
		
		// Режиссер
		EditText et = (EditText)findViewById(R.id.editText1);
		et.setText("");
		
		// Актер
		et = (EditText)findViewById(R.id.editText2);
		et.setText("");
		
		onOKClick(v);
	}
	
	public void onOKClick(View v) {
		Intent intent = new Intent();
		
		EditText director = (EditText)findViewById(R.id.editText1);
		intent.putExtra("director", director.getText().toString());
		
		EditText actor = (EditText)findViewById(R.id.editText2);
		intent.putExtra("actor", actor.getText().toString());
		
		Spinner country = (Spinner)findViewById(R.id.spinner1);
		intent.putExtra("country", country.getSelectedItemPosition() - 1);
		
		Spinner jenre = (Spinner)findViewById(R.id.spinner2);
		intent.putExtra("jenre", jenre.getSelectedItemPosition() - 1);
		
		setResult(RESULT_OK, intent);
		finish();
	}
}
