package com.example.vcmobile.core;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.net.ParseException;

// Низкоуровнеый штуки для работы с сервером
public class NetUtils {
	// -------------------------------------------------------------------------------
	// КОНСТАНТЫ СЕРВЕРА
	// -------------------------------------------------------------------------------
	//final public static String URL_SERVER = "http://192.168.0.13";		// Сервак
	//final public static String URL_SERVER = "http://ite.azurewebsites.net";		// Сервак
	//final public static String URL_SERVER = "http://ite.netai.net";		// Сервак
	final public static String URL_SERVER = "http://ite.cloudapp.net";		// Сервак
	//final public static String URL_SERVER = "http://212.192.93.156";		// Сервак
	
	final public static String URL_VIDEOS = URL_SERVER + "/videos/";
	
	final public static int UPLOAD_PART_SIZE = 128*1024;		// Размер одного кусочка (128 килобайт)
	
	private HttpClient httpClient;
	private String userId;
	
	public NetUtils(int userId) {
		httpClient = new DefaultHttpClient();
		this.userId = Integer.toString(userId);
	}
	
	/*
	 * Взять список всех видео, 
	 * Вернуть пары videoId-version
	 * null в случае ошибки
	 */
	public SortedMap<Integer, Integer> GetList() {
		try {
			String url = URL_SERVER + "/api/get_list.php";
			
			// Формируем http post запрос
			HttpPost httppost = new HttpPost(url);
			
			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("uid",		new StringBody(userId));
			//reqEntity.addPart("type",	new StringBody(type));
			httppost.setEntity(reqEntity);
			
			// Выполняем запрос
			HttpResponse response = httpClient.execute(httppost);
			if (response == null) {
				VCLog.write("NetUtils.GetList() response == null"); 
				return null;
			}
			
			HttpEntity resEntity = response.getEntity();
			if (resEntity == null) {
				VCLog.write("NetUtils.GetList() resEntity == null"); 
				return null;
			}
			
			String RESULT = EntityUtils.toString(resEntity);
			return ParseVideoList(RESULT);
		}
		catch (IOException ex) {
			VCLog.write(ex, "NetUtils.GetList() IOException"); 
			return null;
		}
		catch (ParseException ex) {
			VCLog.write(ex, "NetUtils.GetList() ParseException"); 
			return null;
		}
	}
	
	private SortedMap<Integer, Integer> ParseVideoList(String list) {
		SortedMap<Integer, Integer> videos = new TreeMap<Integer, Integer>();
		
		try {
			String pairs[] = list.split(":");
			for (String pair : pairs) {
				String spair[] = pair.split(" ");
				
				int videoId = Integer.parseInt(spair[0]);
				int versionId = Integer.parseInt(spair[1]);
				
				videos.put(videoId, versionId);
			}
			
			return videos;
		}
		catch (Exception ex) {
			VCLog.write(ex, "NetUtils.GetList.ParseVideoList Exception treying parse [" + list + "]."); 
			return null;
		}
	}
	
	/*
	 * Вернeт список моих видео
	 * 
	 * Возвращает массив ID, которые мои
	 * При неудаче - null
	 */
	public String[] GetMyVideos() {
		try {
			String url = URL_SERVER + "/api/get_my.php";
			
			// Формируем http post запрос
			HttpPost httppost = new HttpPost(url);
			
			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("uid",	 new StringBody(userId));
			httppost.setEntity(reqEntity);
			
			// Выполняем запрос
			HttpResponse response = httpClient.execute(httppost);
			if (response == null) {
				VCLog.write("NetUtils.GetMyVideos() response == null"); 
				return null;
			}
			
			HttpEntity resEntity = response.getEntity();
			if (resEntity == null) {
				VCLog.write("NetUtils.GetMyVideos() resEntity == null"); 
				return null;
			}
			
			String RESULT = EntityUtils.toString(resEntity);
			return RESULT.split(" ");
		}
		catch (IOException ex) {
			VCLog.write(ex, "NetUtils.GetMyVideos() IOException"); 
			return null;
		}
		catch (ParseException ex) {
			VCLog.write(ex, "NetUtils.GetMyVideos() ParseException"); 
			return null;
		}
	}
	
	/*
	 * Вернуть размер видео с именем file
	 * При неудаче -1
	 */
	public int GetVideoSize(int videoId) {
		HttpURLConnection conn = null;
		try {
			URL url = new URL(URL_VIDEOS + Integer.toString(videoId));

			conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("HEAD");
			conn.getInputStream();
			return conn.getContentLength();
		} 
		catch (IOException e) {
			return -1;
		}
		finally {
			if (conn != null)
				conn.disconnect();
		}
	}
	
	/*
	 * Скачать частичку видео name
	 * Начиная с offset и размером size
	 */
	public byte[] GetVideoPart(int videoId, int offset, int size) {
		try {
			//String url = URL_SERVER + "/api/video_get.php?id=" + id + "&ext=" + ext + "&offset=" + offset + "&size=" + size;
			String url = URL_SERVER + "/videos/" + videoId;

			// Формируем http запрос
			HttpGet request = new HttpGet(url);
			request.addHeader("Range", "bytes=" + offset + "-" + Integer.toString(offset+size-1));
			
			HttpResponse response = httpClient.execute(request);
			if (response == null) {
				VCLog.write("NetUtils.GetVideoPart() response == null"); 
				return null;
			}
			
			HttpEntity resEntity = response.getEntity();
			if (resEntity == null) {
				VCLog.write("NetUtils.GetVideoPart() resEntity == null"); 
				return null;
			}
			
			byte[] output = EntityUtils.toByteArray(resEntity);
			return output;
		}
		catch (IOException e) {
			return null;
		}
	}
	
	/*
	 * Начинаем выгрузку на сервер, загружаем описание
	 *	desc	JSON описание
	 *	size	Размер в байтах
	 * 
	 * Возвращает ID, в случае неудачи -1
	 */
	public int UploadVideoBegin(String desc, long size) {
		try {
			String url = URL_SERVER + "/api/video_upload_begin.php";
			
			// Формируем http post запрос
			HttpPost httppost = new HttpPost(url);
			
			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("uid",	 new StringBody(userId));
			reqEntity.addPart("size",	 new StringBody(Long.toString(size)));
			reqEntity.addPart("desc",	 new StringBody(desc, Charset.defaultCharset()));
			httppost.setEntity(reqEntity);
			
			// Выполняем запрос
			HttpResponse response = httpClient.execute(httppost);
			if (response == null) {
				VCLog.write("NetUtils.UploadVideoBegin() response == null"); 
				return -1;
			}
			
			HttpEntity resEntity = response.getEntity();
			if (resEntity == null) {
				VCLog.write("NetUtils.UploadVideoBegin() resEntity == null"); 
				return -1;
			}
			
			String RESULT = EntityUtils.toString(resEntity);
			
			Pattern pattern = Pattern.compile("^OK(\\d+)$");
			Matcher m = pattern.matcher(RESULT);
			if (!m.find()) {
				VCLog.write("NetUtils.UploadVideoBegin() m.find() == false, RESULT=[" + RESULT + "]."); 
				return -1;
			}
			
			String videoIdStr = m.group(1);
			
			try {
				int id = Integer.parseInt(videoIdStr);
				return id;
			}
			catch (NumberFormatException ex) {
				VCLog.write(ex, "NetUtils.UploadVideoBegin() NumberFormatException"); 
				return -1;
			}
		}
		catch (IOException ex) {
			VCLog.write(ex, "NetUtils.UploadVideoBegin() IOException"); 
			return -1;
		}
		catch (IllegalStateException ex) {
			VCLog.write(ex, "NetUtils.UploadVideoBegin() IllegalStateException"); 
			return -1;
		}
	}
	
	/*
	 * Начинаем выгрузку на сервер, загружаем описание
	 *	query	Строка запроса (строка вида 3 5 6)
	 *	
	 * Возвращает JSON-строку (массив) либо null
	 */
	public String GetVideoDesc(String query) {
		try {
			String url = URL_SERVER + "/api/video_get_info_list.php";

			// Формируем http post запрос
			HttpPost httppost = new HttpPost(url);
			
			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("vids", new StringBody(query));
			httppost.setEntity(reqEntity);
			
			// Выполняем запрос
			HttpResponse response = httpClient.execute(httppost);
			if (response == null) {
				VCLog.write("NetUtils.GetVideoDesc() response == null"); 
				return null;
			}
			
			HttpEntity resEntity = response.getEntity();
			if (resEntity == null) {
				VCLog.write("NetUtils.GetVideoDesc() resEntity == null"); 
				return null;
			}
			
			String RESULT = EntityUtils.toString(resEntity, "UTF-8");
			return RESULT;
		}
		catch (IOException ex) {
			VCLog.write(ex, "NetUtils.GetVideoDesc() IOException == null"); 
			return null;
		}
		catch (ParseException ex) {
			VCLog.write(ex, "NetUtils.GetVideoDesc() ParseException == null"); 
			return null;
		}
	}

	/*
	 * Получить короткие описание (лайки + прсомотры)
	 * query	Строка запроса (строка вида 3 5 6)
	 *	
	 * Возвращает JSON-строку (массив) либо null
	 */
	public String GetVideoShortDesc(String query) {
		try {
			String url = URL_SERVER + "/api/video_get_info_list_short.php";

			// Формируем http post запрос
			HttpPost httppost = new HttpPost(url);
			
			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("vids", new StringBody(query));
			reqEntity.addPart("uid", new StringBody(userId));
			httppost.setEntity(reqEntity);
			
			// Выполняем запрос
			HttpResponse response = httpClient.execute(httppost);
			if (response == null) {
				VCLog.write("NetUtils.GetVideoShortDesc() response == null"); 
				return null;
			}
			
			HttpEntity resEntity = response.getEntity();
			if (resEntity == null) {
				VCLog.write("NetUtils.GetVideoShortDesc() resEntity == null"); 
				return null;
			}
			
			String RESULT = EntityUtils.toString(resEntity);
			return RESULT;
		}
		catch (IOException ex) {
			VCLog.write(ex, "NetUtils.GetVideoShortDesc() IOException");
			return null;
		}
		catch (ParseException ex) {
			VCLog.write(ex, "NetUtils.GetVideoShortDesc() ParseException"); 
			return null;
		}
	}
	
	/*
	 * Загрузить часть видео на сервер
	 * 
	 * В случае успеха = True
	 */
	public boolean UploadVideoPart(int videoId, int partId, byte[] content) {
		final String url = URL_SERVER + "/api/video_upload.php";
		final String DEFAULT_FILE_NAME = "LALALA";	// Не сервере он все равно не нужен
		
		try {
			HttpPost httppost = new HttpPost(url);
			
			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("uid",	 new StringBody(userId));
			reqEntity.addPart("vid",	 new StringBody(Integer.toString(videoId)));
			reqEntity.addPart("part",	 new StringBody(Integer.toString(partId)));
			reqEntity.addPart("content", new ByteArrayBody(content, DEFAULT_FILE_NAME));
			httppost.setEntity(reqEntity);
			
			// Выполняем запрос
			HttpResponse response = httpClient.execute(httppost);
			if (response == null) {
				VCLog.write("NetUtils.UploadVideoPart() response == null"); 
				return false;
			}
			
			HttpEntity resEntity = response.getEntity();
			if (resEntity == null) {
				VCLog.write("NetUtils.UploadVideoPart() resEntity == null"); 
				return false;
			}
			
			String RESULT = EntityUtils.toString(resEntity);
			
			if (RESULT.equals("OK"))
				return true;
			else {
				VCLog.write("NetUtils.UploadVideoPart() Returned [" + RESULT + "].");
				return false;
			}
		}
		catch (IOException ex) {
			VCLog.write(ex, "NetUtils.UploadVideoPart() IOException");
			return false;
		}
		catch (ParseException ex) {
			VCLog.write(ex, "NetUtils.UploadVideoPart() ParseException"); 
			return false;
		}
	}
	
	/*
	 * Склеить части видоса videoId
	 * 
	 * Возвращает true в случае успеха
	 */
	public boolean MergeVideoFile(int videoId) {
		final String finishUrl = URL_SERVER + "/api/video_upload_finish.php";
		
		try {
			// Объединить все части в одну
			
			HttpPost httppost = new HttpPost(finishUrl);
			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("vid", new StringBody(Integer.toString(videoId)));
			httppost.setEntity(reqEntity);
			
			// Выполняем запрос
			HttpResponse response = httpClient.execute(httppost);
			if (response == null) {
				VCLog.write("NetUtils.MergeVideoFile() response == null"); 
				return false;
			}
			
			HttpEntity resEntity = response.getEntity();
			if (resEntity == null) {
				VCLog.write("NetUtils.MergeVideoFile() resEntity == null"); 
				return false;
			}
			
			String RESULT = EntityUtils.toString(resEntity);
			if (RESULT.equals("OK"))
				return true;
			else {
				VCLog.write("NetUtils.MergeVideoFile() FAILED! Returned [" + RESULT + "].");
				return false;
			}
		}
		catch (IOException ex) {
			VCLog.write(ex, "NetUtils.MergeVideoFile() IOException");
			return false;
		}
		catch (ParseException ex) {
			VCLog.write(ex, "NetUtils.MergeVideoFile() ParseException");
			return false;
		}
	}
	
	/*
	 * Уведичить на 1 счетчик просмотров видео videoId
	 */
	public boolean IncrementVideoView(int videoId) {
		final String url = URL_SERVER + "/api/video_add_view.php";
		
		try {
			HttpPost httppost = new HttpPost(url);
			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("vid", new StringBody(Integer.toString(videoId)));
			httppost.setEntity(reqEntity);
			
			// Выполняем запрос
			HttpResponse response = httpClient.execute(httppost);
			if (response == null) {
				VCLog.write("NetUtils.IncrementVideoView() response == null"); 
				return false;
			}
			
			HttpEntity resEntity = response.getEntity();
			if (resEntity == null) {
				VCLog.write("NetUtils.IncrementVideoView() resEntity == null"); 
				return false;
			}
			
			String RESULT = EntityUtils.toString(resEntity);
			if (RESULT.equals("OK"))
				return true;
			else {
				VCLog.write("NetUtils.IncrementVideoView() FAILED! Returned [" + RESULT + "].");
				return false;
			}
		}
		catch (IOException ex) {
			VCLog.write(ex, "NetUtils.IncrementVideoView() IOException");
			return false;
		}
		catch (ParseException ex) {
			VCLog.write(ex, "NetUtils.IncrementVideoView() ParseException");
			return false;
		}
	}
	
	/*
	 * Поставить лайк видео videoId
	 */
	public boolean AddLike(int videoId) {
		final String url = URL_SERVER + "/api/video_add_like.php";
		
		try {
			HttpPost httppost = new HttpPost(url);
			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("vid", new StringBody(Integer.toString(videoId)));
			reqEntity.addPart("uid", new StringBody(userId));
			httppost.setEntity(reqEntity);
			
			// Выполняем запрос
			HttpResponse response = httpClient.execute(httppost);
			if (response == null) {
				VCLog.write("NetUtils.AddLike() response == null"); 
				return false;
			}
			
			HttpEntity resEntity = response.getEntity();
			if (resEntity == null) {
				VCLog.write("NetUtils.AddLike() resEntity == null"); 
				return false;
			}
			
			String RESULT = EntityUtils.toString(resEntity);
			if (RESULT.equals("OK"))
				return true;
			else {
				VCLog.write("NetUtils.AddLike() FAILED! Returned [" + RESULT + "].");
				return false;
			}
		}
		catch (IOException ex) {
			VCLog.write(ex, "NetUtils.AddLike() IOException");
			return false;
		}
		catch (ParseException ex) {
			VCLog.write(ex, "NetUtils.AddLike() ParseException");
			return false;
		}
	}
	
	/*
	 * Убрать лайк видео videoId
	 */
	public boolean DeleteLike(int videoId) {
		final String url = URL_SERVER + "/api/video_delete_like.php";
		
		try {
			HttpPost httppost = new HttpPost(url);
			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("vid", new StringBody(Integer.toString(videoId)));
			reqEntity.addPart("uid", new StringBody(userId));
			httppost.setEntity(reqEntity);
			
			// Выполняем запрос
			HttpResponse response = httpClient.execute(httppost);
			if (response == null) {
				VCLog.write("NetUtils.DeleteLike() response == null"); 
				return false;
			}
			
			HttpEntity resEntity = response.getEntity();
			if (resEntity == null) {
				VCLog.write("NetUtils.DeleteLike() resEntity == null"); 
				return false;
			}
			
			String RESULT = EntityUtils.toString(resEntity);
			if (RESULT.equals("OK"))
				return true;
			else {
				VCLog.write("NetUtils.DeleteLike() FAILED! Returned [" + RESULT + "].");
				return false;
			}
		}
		catch (IOException ex) {
			VCLog.write(ex, "NetUtils.DeleteLike() IOException");
			return false;
		}
		catch (ParseException ex) {
			VCLog.write(ex, "NetUtils.DeleteLike() ParseException");
			return false;
		}
	}
	
	/*
	 * Добавить videoId в мои видео
	 */
	public boolean AddToMyVideos(int videoId) {
		final String url = URL_SERVER + "/api/video_add_my.php";
		
		try {
			HttpPost httppost = new HttpPost(url);
			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("vid", new StringBody(Integer.toString(videoId)));
			reqEntity.addPart("uid", new StringBody(userId));
			httppost.setEntity(reqEntity);
			
			// Выполняем запрос
			HttpResponse response = httpClient.execute(httppost);
			if (response == null) {
				VCLog.write("NetUtils.AddToMyVideos() response == null"); 
				return false;
			}
			
			HttpEntity resEntity = response.getEntity();
			if (resEntity == null) {
				VCLog.write("NetUtils.AddToMyVideos() resEntity == null"); 
				return false;
			}
			
			String RESULT = EntityUtils.toString(resEntity);
			if (RESULT.equals("OK"))
				return true;
			else {
				VCLog.write("NetUtils.AddToMyVideos() FAILED! Returned [" + RESULT + "].");
				return false;
			}
		}
		catch (IOException ex) {
			VCLog.write(ex, "NetUtils.AddToMyVideos() IOException");
			return false;
		}
		catch (ParseException ex) {
			VCLog.write(ex, "NetUtils.AddToMyVideos() ParseException");
			return false;
		}
	}
	
	/*
	 * Удалить videoId из моих видео
	 */
	public boolean DeleteFromMyVideos(int videoId) {
		final String url = URL_SERVER + "/api/video_delete_my.php";
		
		try {
			HttpPost httppost = new HttpPost(url);
			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("vid", new StringBody(Integer.toString(videoId)));
			reqEntity.addPart("uid", new StringBody(userId));
			httppost.setEntity(reqEntity);
			
			// Выполняем запрос
			HttpResponse response = httpClient.execute(httppost);
			if (response == null) {
				VCLog.write("NetUtils.DeleteFromMyVideos() response == null"); 
				return false;
			}
			
			HttpEntity resEntity = response.getEntity();
			if (resEntity == null) {
				VCLog.write("NetUtils.DeleteFromMyVideos() resEntity == null"); 
				return false;
			}
			
			String RESULT = EntityUtils.toString(resEntity);
			if (RESULT.equals("OK"))
				return true;
			else {
				VCLog.write("NetUtils.DeleteFromMyVideos() FAILED! Returned [" + RESULT + "].");
				return false;
			}
		}
		catch (IOException ex) {
			VCLog.write(ex, "NetUtils.DeleteFromMyVideos() IOException");
			return false;
		}
		catch (ParseException ex) {
			VCLog.write(ex, "NetUtils.DeleteFromMyVideos() ParseException");
			return false;
		}
	}
	
	/*
	 * Удалить videoId из каталога (ПОЛНОСТЬЮ!)
	 */
	public boolean DeleteVideo(int videoId) {
		final String url = URL_SERVER + "/api/video_delete.php";
		
		try {
			HttpPost httppost = new HttpPost(url);
			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("vid", new StringBody(Integer.toString(videoId)));
			httppost.setEntity(reqEntity);
			
			// Выполняем запрос
			HttpResponse response = httpClient.execute(httppost);
			if (response == null) {
				VCLog.write("NetUtils.DeleteVideo() response == null"); 
				return false;
			}
			
			HttpEntity resEntity = response.getEntity();
			if (resEntity == null) {
				VCLog.write("NetUtils.DeleteVideo() resEntity == null"); 
				return false;
			}
			
			String RESULT = EntityUtils.toString(resEntity);
			if (RESULT.equals("OK"))
				return true;
			else {
				VCLog.write("NetUtils.DeleteVideo() FAILED! Returned [" + RESULT + "].");
				return false;
			}
		}
		catch (IOException ex) {
			VCLog.write(ex, "NetUtils.DeleteVideo() IOException");
			return false;
		}
		catch (ParseException ex) {
			VCLog.write(ex, "NetUtils.DeleteVideo() ParseException");
			return false;
		}
	}
	
	/*
	 * Обновить videoId 
	 */
	public boolean UpdateVideoDesc(String desc) {
		final String url = URL_SERVER + "/api/video_update.php";
		
		try {
			HttpPost httppost = new HttpPost(url);
			MultipartEntity reqEntity = new MultipartEntity();
			//reqEntity.addPart("vid", new StringBody(Integer.toString(videoId)));
			reqEntity.addPart("desc", new StringBody(desc, Charset.defaultCharset()));
			httppost.setEntity(reqEntity);
			
			
			// Выполняем запрос
			HttpResponse response = httpClient.execute(httppost);
			if (response == null) {
				VCLog.write("NetUtils.UpdateVideoDesc() response == null"); 
				return false;
			}
			
			HttpEntity resEntity = response.getEntity();
			if (resEntity == null) {
				VCLog.write("NetUtils.UpdateVideoDesc() resEntity == null"); 
				return false;
			}
			
			String RESULT = EntityUtils.toString(resEntity);
			if (RESULT.equals("OK"))
				return true;
			else {
				VCLog.write("NetUtils.UpdateVideoDesc() FAILED! Returned [" + RESULT + "].");
				return false;
			}
		}
		catch (IOException ex) {
			VCLog.write(ex, "NetUtils.UpdateVideoDesc() IOException");
			return false;
		}
		catch (ParseException ex) {
			VCLog.write(ex, "NetUtils.UpdateVideoDesc() ParseException");
			return false;
		}
	}
	
	/*
	 * Отпарвить email на addr об видео videoId
	 */
	public boolean SendEmail(String addr, int videoId) {
		final String url = URL_SERVER + "/api/send_email.php";
		
		try {
			HttpPost httppost = new HttpPost(url);
			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("addr", new StringBody(addr, Charset.defaultCharset()));
			reqEntity.addPart("vid", new StringBody(Integer.toString(videoId)));
			reqEntity.addPart("uid", new StringBody(userId));
			httppost.setEntity(reqEntity);
			
			
			// Выполняем запрос
			HttpResponse response = httpClient.execute(httppost);
			if (response == null) {
				VCLog.write("NetUtils.SendEmail() response == null"); 
				return false;
			}
			
			HttpEntity resEntity = response.getEntity();
			if (resEntity == null) {
				VCLog.write("NetUtils.SendEmail() resEntity == null"); 
				return false;
			}
			
			String RESULT = EntityUtils.toString(resEntity);
			if (RESULT.equals("OK"))
				return true;
			else {
				VCLog.write("NetUtils.SendEmail() FAILED! Returned [" + RESULT + "].");
				return false;
			}
		}
		catch (IOException ex) {
			VCLog.write(ex, "NetUtils.SendEmail() IOException");
			return false;
		}
		catch (ParseException ex) {
			VCLog.write(ex, "NetUtils.SendEmail() ParseException");
			return false;
		}
	}
	
	/*
	 * Скачать видос с YouTube по ссылке link
	 */
	public boolean UploadYouTube(String link) {
		final String url = URL_SERVER + "/api/upload_youtube.php";
		
		try {
			HttpPost httppost = new HttpPost(url);
			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("link", new StringBody(link, Charset.defaultCharset()));
			reqEntity.addPart("uid", new StringBody(userId));
			httppost.setEntity(reqEntity);
			
			
			// Выполняем запрос
			HttpResponse response = httpClient.execute(httppost);
			if (response == null) {
				VCLog.write("NetUtils.UploadYouTube() response == null"); 
				return false;
			}
			
			HttpEntity resEntity = response.getEntity();
			if (resEntity == null) {
				VCLog.write("NetUtils.UploadYouTube() resEntity == null"); 
				return false;
			}
			
			String RESULT = EntityUtils.toString(resEntity);
			if (RESULT.equals("OK"))
				return true;
			else {
				VCLog.write("NetUtils.UploadYouTube() FAILED! Returned [" + RESULT + "].");
				return false;
			}
		}
		catch (IOException ex) {
			VCLog.write(ex, "NetUtils.UploadYouTube() IOException");
			return false;
		}
		catch (ParseException ex) {
			VCLog.write(ex, "NetUtils.UploadYouTube() ParseException");
			return false;
		}
	}
	
	/*
	 * Проверить, находится ли видос на декодировании
	 */
	public boolean IsVideoEncoding(int videoId) {
		final String url = URL_SERVER + "/api/video_is_encoding.php";
		
		try {
			HttpPost httppost = new HttpPost(url);
			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("vid", new StringBody(Integer.toString(videoId)));
			httppost.setEntity(reqEntity);
			
			
			// Выполняем запрос
			HttpResponse response = httpClient.execute(httppost);
			if (response == null) {
				VCLog.write("NetUtils.IsVideoEncoding() response == null"); 
				return false;
			}
			
			HttpEntity resEntity = response.getEntity();
			if (resEntity == null) {
				VCLog.write("NetUtils.IsVideoEncoding() resEntity == null"); 
				return false;
			}
			
			String RESULT = EntityUtils.toString(resEntity);
			if (RESULT.equals("YES"))
				return true;
			else {
				return false;
			}
		}
		catch (IOException ex) {
			VCLog.write(ex, "NetUtils.IsVideoEncoding() IOException");
			return false;
		}
		catch (ParseException ex) {
			VCLog.write(ex, "NetUtils.IsVideoEncoding() ParseException");
			return false;
		}
	}
	
	/*
	 * Получить супер короткую инфо об 1 видео
	 */
	public VCVideoShortJSON GetVideoShortInfo(int videoId) {
		final String url = URL_SERVER + "/api/video_short_info.php";
		
		try {
			HttpPost httppost = new HttpPost(url);
			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("vid", new StringBody(Integer.toString(videoId)));
			reqEntity.addPart("uid", new StringBody(userId));
			httppost.setEntity(reqEntity);
			
			
			// Выполняем запрос
			HttpResponse response = httpClient.execute(httppost);
			if (response == null) {
				VCLog.write("NetUtils.GetVideoShortInfo() response == null"); 
				return null;
			}
			
			HttpEntity resEntity = response.getEntity();
			if (resEntity == null) {
				VCLog.write("NetUtils.GetVideoShortInfo() resEntity == null"); 
				return null;
			}
			
			String RESULT = EntityUtils.toString(resEntity);
			try {
				String str[] = RESULT.split(":");
				if (str.length != 3)
					return null;
				
				VCVideoShortJSON desc = new VCVideoShortJSON();
				desc.id = videoId;
				desc.views = Integer.parseInt(str[0]);
				desc.likes = Integer.parseInt(str[1]);
				int ilike = Integer.parseInt(str[2]);
				if (ilike == 0)
					desc.ilike = false;
				else if (ilike == 1)
					desc.ilike = true;
				else
					return null;
				
				return desc;
			}
			catch (NumberFormatException ex) {
				return null;
			}
		}
		catch (IOException ex) {
			VCLog.write(ex, "NetUtils.GetVideoShortInfo() IOException");
			return null;
		}
		catch (ParseException ex) {
			VCLog.write(ex, "NetUtils.GetVideoShortInfo() ParseException");
			return null;
		}
	}
}
