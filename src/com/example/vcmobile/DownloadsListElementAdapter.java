package com.example.vcmobile;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vcmobile.core.VCVideoJSON;
import com.example.vcmobile.core.VideoLibDownloads;
import com.example.vcmobile.core.VideoUtils;

public class DownloadsListElementAdapter extends BaseAdapter {
	Activity activity; 
	//int layoutResourceId;
	ArrayList<VCVideoJSON> data = null;
	private VideoLibDownloads lib;
	private LayoutInflater inflater;
	public ImageLoader imageLoader; 
	
	public DownloadsListElementAdapter(Activity activity, ArrayList<VCVideoJSON> data, VideoLibDownloads lib) {
		this.activity = activity;
		this.data = data;
		this.lib = lib;
		
		inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		imageLoader = new ImageLoader(activity.getApplicationContext());
	}
	
	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View vi = convertView;
		if(convertView == null)
			vi = inflater.inflate(R.layout.downloads_list_row, null);
		
		final VCVideoJSON currentVideo = data.get(position);
		
		vi.setTag(currentVideo);

		// Название
		TextView text = (TextView)vi.findViewById(R.id.currentDownloadTitle);;
		text.setText(currentVideo.title);
		
		// Длительность
		text = (TextView)vi.findViewById(R.id.txtLength);
		Integer minutes = currentVideo.length / 60;
		Integer seconds = currentVideo.length % 60;
		String txtLength;
		
		if (currentVideo.length >= 60*60) {
			Integer hours = minutes / 60;
			minutes = minutes - hours*60;
			txtLength = hours + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds); 
		}
		else {
			txtLength = minutes + ":" + String.format("%02d", seconds); ;
		}
		text.setText(txtLength);
		
		// Превьюшка
		ImageView image = (ImageView)vi.findViewById(R.id.imgIcon);
		imageLoader.DisplayImage(currentVideo.id + ".jpg", image);
		
		// Размер
		text = (TextView)vi.findViewById(R.id.downloadSize);
		long sizeL = VideoUtils.GetCacheVideoSize(currentVideo.id);
		double size = sizeL;
		size = size / 1024.0 / 1024.0;	//Mb
		size = Math.round(size * 100.0) / 100.0;
		text.setText(Double.toString(size) + " МБ");
		
		// Тык на видео
		vi.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				v.setSelected(true);
				VCVideoJSON video = (VCVideoJSON)v.getTag();
				
				VideoUtils.StartVideoPageActivity(activity, video, false);
			}});
		
		// DELETE BUTTON
		image = (ImageView)vi.findViewById(R.id.deleteDownloadsButton);
		image.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//activity.sendBroadcast(new Intent(DownloadService.BROADCAST_ABORT_ACTION));
				//VideoUtils.DeleteFromCache(currentVideo.id, currentVideo.ext);
				Object obj = (Integer)currentVideo.id;	// Если прост опередать интережр, то он удалит элемент в этой позиции!
				lib.videoDownloadsList.remove(obj);
				lib.SaveLists();
				
				v.setVisibility(View.GONE);
				
				Toast.makeText(activity, "Загрузка удалена", Toast.LENGTH_SHORT).show();
			}
		});
   
		return vi;
	}

}
