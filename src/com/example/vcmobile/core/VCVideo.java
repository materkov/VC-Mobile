package com.example.vcmobile.core;

import java.io.Serializable;

public class VCVideo implements Serializable {
	public VCVideoJSON video;
	public VCVideoShortJSON videoShort;
	public int version;
	public boolean isMy;	// Лежит ли в списке моих видео
	
	public VCVideo(VCVideoJSON _video, VCVideoShortJSON _videoShort, int _version) {
		video = _video;
		videoShort = _videoShort;
		version = _version;
		isMy = false;
	}
	
	public boolean isCorrect() {
		return video != null && video.isCorrect() && videoShort != null;
	}
}