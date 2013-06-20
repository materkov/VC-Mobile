package com.example.vcmobile;

import android.app.Activity;
import android.content.Context;
import android.view.KeyEvent;
import android.widget.MediaController;

public class VCMediaController extends MediaController {
	public VCMediaController(Context context) {
		super(context);
	}
	
	/*
	 * Единственное зачем нужно переопределятьк онтроллер - 
	 * при проигрывании видео надо было 2 раза нажать "назад" чтобы действительнов ыйти назад.
	 * Тут это исправляется
	 */
	@Override
	public boolean dispatchKeyEvent(KeyEvent event)
	{
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
			((Activity)getContext()).finish();
		
		return super.dispatchKeyEvent(event);
	}
	
	private boolean forceHide = false;
	
	// Принудить скрыть все
	public void SetForceHide(boolean forceHide) {
		if (forceHide) super.hide();

		this.forceHide = forceHide;
	}
	
	@Override
	public void show() {
		if (!forceHide)
			super.show();
	}
	
	@Override
	public void show(int timeout) {
		if (!forceHide)
			super.show(timeout);
	}
	
	@Override
	public void hide() {
		if (!forceHide)
			super.hide();
	}
}
