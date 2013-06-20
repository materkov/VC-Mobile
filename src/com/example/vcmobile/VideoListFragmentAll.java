package com.example.vcmobile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

public class VideoListFragmentAll extends SherlockFragment {
	private View view;
	private ListAdapter adapter;
	private boolean isSearchMode = false;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		view = inflater.inflate(R.layout.fragment_list_all, container, false);
		
		SetAdapter(adapter);
		return view;
	}
	
	public void SetAdapter(ListAdapter a) {
		adapter = a;
		
		if (view != null) {
			// Только если уже создан
			ListView listView = (ListView)view.findViewById(R.id.listViewALL);
			listView.setAdapter(a);
			
			TextView tv = (TextView)view.findViewById(R.id.fragmentAllPlaceholder);
			tv.setText(isSearchMode ? "Ничего не найдено." : "Видео нет.");
			
			if (a == null || a.isEmpty())
				tv.setVisibility(View.VISIBLE);
			else
				tv.setVisibility(View.GONE);
		}
	}
	
	public void SetSearchMode(boolean isSearchMode) {
		this.isSearchMode = isSearchMode;
	}
}