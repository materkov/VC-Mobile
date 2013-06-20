package com.example.vcmobile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;
import android.app.Activity;
import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.vcmobile.core.VCVideo;
import com.example.vcmobile.core.VideoUtils;

public class VideoListElementAdapter extends BaseAdapter {
	Activity activity; 
	VCVideo data[] = null;
	private LayoutInflater inflater;
	public ImageLoader imageLoader; 
	private String searchCriterion;
	
	public VideoListElementAdapter(Activity activity, VCVideo[] data, String searchCriterion) {
		this.activity = activity;
		this.data = data;
		this.searchCriterion = searchCriterion;

		inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		imageLoader = new ImageLoader(activity.getApplicationContext());
	}
	
	public int getCount() {
		return data.length;
	}
	
	public long getItemId(int position) {
		return position;
	}
	
	public Object getItem(int position) {
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View vi = convertView;
		if(convertView == null)
			vi = inflater.inflate(R.layout.video_list_row, null);
		
		VCVideo currentVideo = data[position];
		vi.setTag(data[position]);
		
		Assert.assertTrue(currentVideo.isCorrect());

		// Название
		TextView text = (TextView)vi.findViewById(R.id.currentDownloadTitle);
		if (searchCriterion != null) {
			// Подсветить
			String replaceString = "(?i)" + Pattern.quote(searchCriterion);
			Pattern p = Pattern.compile("(" + replaceString + ")");
			
			Matcher m = p.matcher(currentVideo.video.title);
			StringBuffer s = new StringBuffer();
			while (m.find())
				m.appendReplacement(s, "<font color=\"red\">" + m.group(1) + "</font>");
			
			m.appendTail(s);

			text.setText(Html.fromHtml(s.toString()));
		}
		else {
			text.setText(currentVideo.video.title);
		}
		
		// Длительность
		text = (TextView)vi.findViewById(R.id.txtLength);
		text.setText(VideoUtils.GetFormatedLength(currentVideo.video.length));
		
		// Прсмотры
		if (currentVideo.videoShort.views == 0)
			vi.findViewById(R.id.viewsLayout).setVisibility(View.INVISIBLE);
		else {
			vi.findViewById(R.id.viewsLayout).setVisibility(View.VISIBLE);
			TextView viewsCounter = (TextView)vi.findViewById(R.id.viewsCounter);
			viewsCounter.setText(Integer.toString(currentVideo.videoShort.views));
		}
		
		// Лайки
		if (currentVideo.videoShort.likes == 0) {
			vi.findViewById(R.id.likesLayout).setVisibility(View.INVISIBLE);
		}
		else {
			vi.findViewById(R.id.likesLayout).setVisibility(View.VISIBLE);
			TextView likesCounter = (TextView)vi.findViewById(R.id.likesCounter);
			likesCounter.setText(Integer.toString(currentVideo.videoShort.likes));
			
			// Если я НЕ лайкнул эту видяшку, установить альфу
			if (!currentVideo.videoShort.ilike) 
			{
				final int ALPHA = 100;
				ImageView pic = (ImageView)vi.findViewById(R.id.likePic);
				pic.setAlpha(ALPHA);
				
				likesCounter.setTextColor(likesCounter.getTextColors().withAlpha(ALPHA));
				likesCounter.setHintTextColor(likesCounter.getHintTextColors().withAlpha(ALPHA));
				likesCounter.setLinkTextColor(likesCounter.getLinkTextColors().withAlpha(ALPHA));
			}
		}
		
		// Превьюшка
		ImageView image = (ImageView)vi.findViewById(R.id.imgIcon);
		imageLoader.DisplayImage(data[position].video.id + ".jpg", image);
		
		// Тык на видео
		vi.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				v.setSelected(true);
				VCVideo video = (VCVideo)v.getTag();
				VideoUtils.StartVideoPageActivity(activity, video.video, true);
				activity.overridePendingTransition(R.anim.right_in, R.anim.left_out);
			}});
		
		
		return vi;
	}
}