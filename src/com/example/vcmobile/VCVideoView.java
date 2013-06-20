package com.example.vcmobile;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

public class VCVideoView extends VideoView {
	public VCVideoView(Context context) {
		super(context);
	}

	public VCVideoView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public VCVideoView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	private boolean _isPlaying = false;
	
	public void setPlayPauseListener(PlayPauseListener listener) {
		mListener = listener;
	}
	
	@Override
	public void pause() {
		_isPlaying = false;

		if (mListener != null) {
			mListener.onPause();
		}

		super.pause();
	}
	
	@Override
	public void start() {
		_isPlaying = true;
		
		if (mListener != null) {
			mListener.onPlay();
		}
		
		super.start();
	}
	
	private PlayPauseListener mListener;
	
	interface PlayPauseListener {
		void onPlay();
		void onPause();
	}
	
	@Override
	public boolean isPlaying() {
		return _isPlaying;
	}
}
