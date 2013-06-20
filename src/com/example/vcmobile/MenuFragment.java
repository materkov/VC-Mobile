package com.example.vcmobile;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.vcmobile.core.Utils;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

public class MenuFragment extends ListFragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.list, null);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		String[] items = {"Каталог", "Добавить видео", "Синхронизировать", "Загрузки", "Выйти из системы"};
		ArrayAdapter<String> colorAdapter = new ArrayAdapter<String>(getActivity(), 
				R.layout.menu_list_row, R.id.text1, items);
		setListAdapter(colorAdapter);
	}

	@Override
	public void onListItemClick(ListView lv, View v, int position, long id) {
		if (position == 0) {
			// Каталог
			if (!(getActivity() instanceof VideoListActivity)) {
				startActivity(new Intent(getActivity(), VideoListActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				getActivity().finish();
			}
		} 
		else if (position == 1) {
			// Добавить видео
			if (!(getActivity() instanceof VideoUploadActivity)) {
				startActivity(new Intent(getActivity(), VideoUploadActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
			}
		}
		else if (position == 2) {
			// Синхронизировать
			if (getActivity().getClass() == VideoListActivity.class) {
				// Если уже в этой активити
				((VideoListActivity)getActivity()).Synch();
			}
			else {
				// Создаем активити
				Intent intent = new Intent(getActivity(), VideoListActivity.class);
				Bundle b = new Bundle();
				b.putBoolean("needSync", true);
				intent.putExtras(b);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				
				getActivity().startActivity(intent);
				getActivity().finish();
			}
		}
		else if (position == 3) {
			// Загрузки
			if (!(getActivity() instanceof DownloadsActivity)) {
				Intent intent = new Intent(getActivity(), DownloadsActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				getActivity().startActivity(intent);
				//getActivity().finish();
			}
		}
		else if (position == 4) {
			new AlertDialog.Builder(getActivity())
				.setTitle("Выход")
				.setMessage("Вы уверены, что хотите выйти?")
				.setPositiveButton("Да", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) { 
						// Выйти
						Utils.Logout(getActivity());
						
						startActivity(new Intent(getActivity(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
						getActivity().finish();
					}
				 })
				 .setNegativeButton("Нет", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) { 
						// do nothing
					}
				 })
				 .show();
			
			return;
		}
		
		// Скрыть меню после тычка
		SlidingFragmentActivity activity = (SlidingFragmentActivity)getActivity();
		activity.getSlidingMenu().showContent();
	}
}
