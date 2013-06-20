package com.example.vcmobile.core;

import java.io.Serializable;

import android.graphics.Bitmap;

public class VCVideoJSON implements Serializable {
	public int id;
	public int author;	// id чела, который выложил видео
	public String title;
	public int year;
	public int length;	// Seconds
	
	public int createDate;	// UNIX time, Дата создания
	
	public boolean isPrivate;	// Является ли видео приватным	
	
	public int[] countries;
	public int[] jenres;
	
	public String directors;
	public String actors;
	
	public String desc;
	
	public transient Bitmap bitmap;	// transient - не передается с сервера
	
	public VCVideoJSON() {
		title = "";
		desc = "";
		countries = new int[0];
		jenres = new int[0];
		directors = "";
		actors = "";
		isPrivate = false;
	}
	
	public boolean isCorrect() {
		return id > 0 && author > 0 && title != null && desc != null &&
				countries != null && jenres != null && 
				directors != null && actors != null;
	}
}
