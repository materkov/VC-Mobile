package com.example.vcmobile.core;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.example.vcmobile.ImageLoader;

public abstract class VkApp {
	// constants for OAUTH AUTHORIZE in Vkontakte
	public static final String CALLBACK_URL = "https://oauth.vk.com/blank.html";
	public static final String APP_ID = "3548652";
	public static final String PERMISSIONS = Integer.toString(8192 + 65536); // Работа со стеной + оффлайн доступ
	public static final String OAUTH_AUTHORIZE_URL = "https://oauth.vk.com/oauth/authorize?client_id=" + APP_ID + "&scope=" + PERMISSIONS + "&redirect_uri=" + CALLBACK_URL + "&display=touch&response_type=token";
	private static final String VK_API_URL = "https://api.vk.com/method/";
	
	public static final String VK_SESSION_FILE_NAME = "VkSession";
	public static final String AVATAR_FILE_NAME = Environment.getExternalStorageDirectory() + "/VCMobile/avatar";

	// parse vkontakte JSON response
	private static boolean checkResponse(JSONObject jsonObj) {
		return !jsonObj.has("error");
	}
	
	private static boolean checkResponse(String jsonObj) {
		try {
			JSONObject jsobObj = new JSONObject(jsonObj);
			return checkResponse(jsobObj);
		} 
		catch (JSONException e) {
			return false;
		}
	}
	
	public static boolean postToWall(Context context, int videoId, String videoTitle) {
		SharedPreferences settings = context.getSharedPreferences(VK_SESSION_FILE_NAME, Context.MODE_PRIVATE);
		String accessToken = settings.getString("VkAccessToken", "");
		
		//set request uri params
		String message = videoTitle;
		String videoUrl = NetUtils.URL_SERVER + "/video.php?id=" + videoId;
		String url = "";
		
		VCLog.write("VK postToWall, message = [" + message + "], url = [" + videoUrl + "].");
		
		try {
			url = VK_API_URL + "wall.post?message=" + URLEncoder.encode(message, "UTF-8") + "&attachments=" + videoUrl + "&access_token=" + accessToken;

			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(url);
			HttpResponse response = client.execute(request);

			HttpEntity entity = response.getEntity();
			String responseText = EntityUtils.toString(entity);
			
			VCLog.write("VK postToWall. Result = " + responseText);
			
			return checkResponse(responseText);
		} 
		catch (UnsupportedEncodingException e) {
			VCLog.write(e, "VK postToWall. UnsupportedEncodingException");
			return true;
		}
		catch (IOException ex) {
			VCLog.write(ex, "VK postToWall. IOException");
			return true;
		}
	}
	
	private static boolean isAccessTokenValid(Context context) {
		SharedPreferences settings = context.getSharedPreferences(VK_SESSION_FILE_NAME, Context.MODE_PRIVATE);
		String accessToken = settings.getString("VkAccessToken", "");
		
		VCLog.write("VK isAccessTokenValid.");
		
		try {
			String url = VK_API_URL + "execute?code=return true;&access_token=" + accessToken;

			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(url);
			HttpResponse response = client.execute(request);

			HttpEntity entity = response.getEntity();
			String responseText = EntityUtils.toString(entity);
			
			JSONObject jsobObj = new JSONObject(responseText);
			boolean isValid = checkResponse(jsobObj);
			
			VCLog.write("VK isAccessTokenValid. Result = " + isValid);
			
			return isValid;
		} catch (JSONException e) {
			return true;
		}
		catch (UnsupportedEncodingException e) {
			return true;
		}
		catch (IOException ex) {
			return true;
		}

	}
	
	private static String GetUserInfoAndAvatar(String userId) {
		// Можно запросить без access_token
		String url = VK_API_URL + "users.get?uids=" + userId + "&fields=photo_50";
		
		try {
			VCLog.write("VKAPI. GetUserInfoAndAvatar(String), url = [" + url + "].");
			
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(url);
			HttpResponse httpResponse = client.execute(request);
			HttpEntity httpEntity = httpResponse.getEntity();
	
			String responseText = EntityUtils.toString(httpEntity);
			VCLog.write("VKAPI. GetUserInfoAndAvatar, Response = [" + responseText + "].");
			
			JSONObject jsobObj = new JSONObject(responseText);
			if (!checkResponse(jsobObj))
				return "";
			
			JSONArray array = jsobObj.getJSONArray("response");
			JSONObject jUser = array.getJSONObject(0);
			String userName = jUser.getString("first_name") + " " + jUser.getString("last_name");
			String avatarUrl = jUser.getString("photo_50");
			
			// Сохранить аватар
			URL imgUrl = new URL(avatarUrl);
			HttpURLConnection connection = (HttpURLConnection)imgUrl.openConnection();
			connection.setDoInput(true);
			connection.connect();
			InputStream input = connection.getInputStream();
			FileOutputStream os = new FileOutputStream(AVATAR_FILE_NAME);
			ImageLoader.CopyStream(input, os);
			os.close();

			return userName;
		}
		catch (JSONException e) {
			VCLog.write(e, "VKAPI. GetUserInfoAndAvatar(String), JSONException ");
		}
		catch(ClientProtocolException ex){
			VCLog.write(ex, "VKAPI. GetUserInfoAndAvatar(String), ClientProtocolException ");
		}
		catch(IOException ex){
			VCLog.write(ex, "VKAPI. GetUserInfoAndAvatar(String), IOException ");
		}
		
		return null;
	}
	
	public static boolean haveSavedSession(Context context) {
		SharedPreferences settings = context.getSharedPreferences(VK_SESSION_FILE_NAME, Context.MODE_PRIVATE);
		return settings.contains("VkUserName");
	}
	
	public static boolean UpdateInfo(Context context) {
		// Обновить имя + аватарку
		SharedPreferences settings = context.getSharedPreferences(VK_SESSION_FILE_NAME, Context.MODE_PRIVATE);
		int VKuserId = settings.getInt("VkUserId", 0);
		Assert.assertTrue(VKuserId > 0);
		
		String userName = GetUserInfoAndAvatar(Integer.toString(VKuserId));
		if (userName == null || userName.equals("")) {
			VCLog.write("VKApi.UpdateInfo, GetUserInfo() error");
			return false;
		}
		else {
			SharedPreferences.Editor _editor = settings.edit();
			_editor.putString("VkUserName", userName);
			_editor.commit();
			
			return true;
		}
	}
	
	// Авторизация по строке url
	public static boolean Login(Context context, String url) {
		SharedPreferences settings = context.getSharedPreferences(VK_SESSION_FILE_NAME, Context.MODE_PRIVATE);
		VCLog.write("VKApi.Login(), url = [" + url + "].");
			
		Pattern p = Pattern.compile("#access_token=(\\w*)&expires_in=(\\d*)&user_id=(\\d*)");
		Matcher m = p.matcher(url);
		
		if (m.find()) {
			String access_token = m.group(1);
			String expires_in = m.group(2);
			String user_id = m.group(3);
			
			String userName;
			if (settings.contains("VkUserName")) {
				// Если уже есть сохраненные настройки
				userName = settings.getString("VkUserName", "");
			}
			else {
				userName = GetUserInfoAndAvatar(user_id);
			}
			
			// Сохраняем новые данные
			SaveVkSession(context, access_token, Integer.parseInt(expires_in), Integer.parseInt(user_id), userName);
			
			return true;
		}
		else {
			VCLog.write("VKApi.Login(), Incorrect url!");
			return false;
		}
	}
	
	private static void SaveVkSession(Context context, String accessToken, int expires, int userId, String userName) {
		SharedPreferences settings = context.getSharedPreferences(VK_SESSION_FILE_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor _editor = settings.edit();
		
		_editor.putString("VkAccessToken", accessToken);
		_editor.putString("VkUserName", userName);
		_editor.putInt("VkExpiresIn", expires);
		_editor.putInt("VkUserId", userId);
		_editor.putLong("VkAccessTime", System.currentTimeMillis());
		_editor.commit();
	}
	
	public static void Logout(Context context) {
		SharedPreferences settings = context.getSharedPreferences(VK_SESSION_FILE_NAME, Context.MODE_PRIVATE);
		
		CookieSyncManager.createInstance(context);
		CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.removeAllCookie();
		
		SharedPreferences.Editor _editor = settings.edit();
		_editor.clear();
		_editor.commit();
		
		VCLog.write("VkApp.Logout.");
	}
	
	public static int GetUserID(Context context) {
		SharedPreferences settings = context.getSharedPreferences(VK_SESSION_FILE_NAME, Context.MODE_PRIVATE);
		return settings.getInt("VkUserId", 0);
	}
}
