package com.example.vcmobile;

import java.io.File;
import java.io.FileFilter;
import java.util.Comparator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vcmobile.core.Utils;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

public class FileSelectActivity extends SlidingFragmentActivity	 {
	private final static String SESSION_FILE = "FileSelect"; 
	
	private SharedPreferences settings;
	   
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_file_select);
		Utils.InitUI(this, "Выберите файл");
		Utils.SetActionBarBackButton(this);
		
		settings = getSharedPreferences(SESSION_FILE, Context.MODE_PRIVATE);
		String lastDir = settings.getString("LastDir", Environment.getExternalStorageDirectory().getAbsolutePath());
		
		File startDir = new File(lastDir);
		final TextView pathTextView = (TextView)findViewById(R.id.pathTextView);
		pathTextView.setText(startDir.getAbsolutePath());
		  
		final ListView lv = (ListView)findViewById(R.id.fileListView);
		lv.setAdapter(CreateAdapter(startDir));
		
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				File f = (File)parent.getItemAtPosition(pos);
				if (f.isFile())
					onFileSelected(f);
				else {
					pathTextView.setText(f.getAbsolutePath());
					view.setEnabled(true);
				
					lv.setAdapter(CreateAdapter(f));
					
					// Записываем последнюю директорию
					SharedPreferences.Editor _editor = settings.edit();
					_editor.putString("LastDir", f.getAbsolutePath());
					_editor.commit();
				}
			}
		});
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
	
	private boolean isVideoFile(File f) {
		return f.getName().endsWith("webm") ||
			f.getName().endsWith("flv") ||
			f.getName().endsWith("mp4") ||
			f.getName().endsWith("3gp");
	}
	
	private void onFileSelected(File f) {
		if (isVideoFile(f)) {
			Intent data = new Intent();
			data.putExtra("path", f.getAbsolutePath());
			setResult(1, data);
			finish();
		}
		else {
			Toast.makeText(this, "Такой формат не поддерживается!", Toast.LENGTH_SHORT).show();
		}
	}
	   
	private MySimpleArrayAdapter CreateAdapter(File parent) {
		File[] files = parent.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if (pathname.isHidden())
					return false;
				
				return true;
			}
		});
		if (files == null)
			files = new File[0];
		MySimpleArrayAdapter adapter = new MySimpleArrayAdapter(this, files);
		
		adapter.sort(new Comparator<File>() {
			@Override
			public int compare(File f1, File f2) {
				if (f1.isDirectory() && f2.isDirectory())
					return f1.getName().compareToIgnoreCase(f2.getName());
				else if (f1.isDirectory() && !f2.isDirectory())
					return -1;
				else if (!f1.isDirectory() && f2.isDirectory())
					return 1;
				else
					return f1.getName().compareToIgnoreCase(f2.getName());
			}
		});
		
		return adapter;
	}
	
	public class MySimpleArrayAdapter extends ArrayAdapter<File> {
		private final Context context;
		private final File[] values;

		public MySimpleArrayAdapter(Context context, File[] values) {
			super(context, R.layout.file_list_row, values);
			this.context = context;
			this.values = values;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View rowView = inflater.inflate(R.layout.file_list_row, parent, false);
			
			TextView textView = (TextView) rowView.findViewById(R.id.label);
			textView.setText(values[position].getName());
			if (values[position].isDirectory())
				textView.setTypeface(null, Typeface.ITALIC);
			else if (isVideoFile(values[position]))
				textView.setTypeface(null, Typeface.BOLD);
			
			ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
			if (values[position].isDirectory())
				imageView.setImageResource(R.drawable.ic_osdialogs_dir);
			
			return rowView;
		}
	}
	
	public void onUpClick(View v) {
		final ListView lv = (ListView)findViewById(R.id.fileListView);
		final TextView pathTextView = (TextView)findViewById(R.id.pathTextView);
		File f = new File((String)pathTextView.getText());
		File parent = f.getParentFile();
		if (parent != null) {
			pathTextView.setText(parent.getAbsolutePath());
			lv.setAdapter(CreateAdapter(parent));
		}
	}
}
