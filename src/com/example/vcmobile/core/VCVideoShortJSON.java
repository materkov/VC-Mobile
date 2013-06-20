package com.example.vcmobile.core;

import java.io.Serializable;

// Тут только лайки и просмотры
public class VCVideoShortJSON implements Serializable {
	public int id;
	public int likes;
	public int views;
	public boolean ilike;
	
	@Override
	public boolean equals(Object otherObj) {
		if (otherObj == null) return false;
		if (!(otherObj instanceof VCVideoShortJSON)) return false;
		
		VCVideoShortJSON other = (VCVideoShortJSON)otherObj;
		
		return this.id == other.id &&
				this.likes == other.likes &&
				this.views == other.views &&
				this.ilike == other.ilike;
	}
}