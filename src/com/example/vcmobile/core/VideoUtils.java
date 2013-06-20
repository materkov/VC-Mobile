package com.example.vcmobile.core;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;

import com.example.vcmobile.VideoPageActivity;

public class VideoUtils {
	final public static String jenres[] = {
			"аниме", // 0
			"биографический",
			"боевик",
			"вестерн",
			"военный",
			"детектив",
			"детский",
			"документальный",
			"драма",
			"исторический",
			"кинокомикс",
			"комедия",
			"концерт",
			"короткометражный",
			"криминал",
			"мелодрама",
			"мистика",
			"музыка",
			"мультфильм",
			"мюзикл",
			"научный",
			"приключения",
			"реалити-шоу",
			"семейный",
			"спорт",
			"ток-шоу",
			"триллер",
			"ужасы",
			"фантастика",
			"фильм-нуар",
			"фэнтези",
			"эротика"
		};
	
	public static String GetJenreById(int id) {
		if (id >= 0 && id < jenres.length)
			return jenres[id];
		else
			return "";
	}
	
	final public static String countries[] = {
			"Абхазия", // 0
			"Австралия",
			"Австрия",
			"Азербайджан",
			"Албания",
			"Алжир",
			"Американское Самоа",
			"Ангилья",
			"Ангола",
			"Андорра",
			"Антарктида",
			"Антигуа и Барбуда",
			"Аргентина",
			"Армения",
			"Аруба",
			"Афганистан",
			"Багамы",
			"Бангладеш",
			"Барбадос",
			"Бахрейн",
			"Беларусь",
			"Белиз",
			"Бельгия",
			"Бенин",
			"Бермуды",
			"Болгария",
			"Боливия, Многонациональное Государство",
			"Бонайре, Саба и Синт-Эстатиус",
			"Босния и Герцеговина",
			"Ботсвана",
			"Бразилия",
			"Британская территория в Индийском океане",
			"Бруней-Даруссалам",
			"Буркина-Фасо",
			"Бурунди",
			"Бутан",
			"Вануату",
			"Венгрия",
			"Венесуэла",
			"Виргинские острова, Британские",
			"Виргинские острова, США",
			"Вьетнам",
			"Габон",
			"Гаити",
			"Гайана",
			"Гамбия",
			"Гана",
			"Гваделупа",
			"Гватемала",
			"Гвинея",
			"Гвинея-Бисау",
			"Германия",
			"Гернси",
			"Гибралтар",
			"Гондурас",
			"Гонконг",
			"Гренада",
			"Гренландия",
			"Греция",
			"Грузия",
			"Гуам",
			"Дания",
			"Джерси",
			"Джибути",
			"Доминика",
			"Доминиканская Республика",
			"Египет",
			"Замбия",
			"Западная Сахара",
			"Зимбабве",
			"Израиль",
			"Индия",
			"Индонезия",
			"Иордания",
			"Ирак",
			"Иран, Исламская Республика",
			"Ирландия",
			"Исландия",
			"Испания",
			"Италия",
			"Йемен",
			"Кабо-Верде",
			"Казахстан",
			"Камбоджа",
			"Камерун",
			"Канада",
			"Катар",
			"Кения",
			"Кипр",
			"Киргизия",
			"Кирибати",
			"Китай",
			"Кокосовые (Килинг) острова",
			"Колумбия",
			"Коморы",
			"Конго",
			"Конго, Демократическая Республика",
			"Корея, Народно-Демократическая Республика",
			"Корея, Республика",
			"Коста-Рика",
			"Кот",
			"Куба",
			"Кувейт",
			"Кюрасао",
			"Лаос",
			"Латвия",
			"Лесото",
			"Ливан",
			"Ливийская Арабская Джамахирия",
			"Либерия",
			"Лихтенштейн",
			"Литва",
			"Люксембург",
			"Маврикий",
			"Мавритания",
			"Мадагаскар",
			"Майотта",
			"Макао",
			"Малави",
			"Малайзия",
			"Мали",
			"Малые Тихоокеанские отдаленные острова Соединенных Штатов",
			"Мальдивы",
			"Мальта",
			"Марокко",
			"Мартиника",
			"Маршалловы острова",
			"Мексика",
			"Микронезия, Федеративные Штаты",
			"Мозамбик",
			"Молдова, Республика",
			"Монако",
			"Монголия",
			"Монтсеррат",
			"Мьянма",
			"Намибия",
			"Науру",
			"Непал",
			"Нигер",
			"Нигерия",
			"Нидерланды",
			"Никарагуа",
			"Ниуэ",
			"Новая",
			"Новая",
			"Норвегия",
			"Объединенные Арабские Эмираты",
			"Оман",
			"Остров Буве",
			"Остров Мэн",
			"Остров Норфолк",
			"Остров Рождества",
			"Остров Херд и острова Макдональд",
			"Острова Кайман",
			"Острова Кука",
			"Острова Теркс и Кайкос",
			"Пакистан",
			"Палау",
			"Палестинская территория, оккупированная",
			"Панама",
			"Папский Престол (Государство — город Ватикан)",
			"Папуа-Новая Гвинея",
			"Парагвай",
			"Перу",
			"Питкерн",
			"Польша",
			"Португалия",
			"Пуэрто-Рико",
			"Республика Македония",
			"Реюньон",
			"Россия",
			"Руанда",
			"Румыния",
			"Самоа",
			"Сан-Марино",
			"Сан-Томе и Принсипи",
			"Саудовская Аравия",
			"Свазиленд",
			"Святая Елена, Остров вознесения, Тристан-да-Кунья",
			"Северные Марианские острова",
			"Сен-Бартельми",
			"Сен-Мартен",
			"Сенегал",
			"Сент-Винсент и Гренадины",
			"Сент-Китс и Невис",
			"Сент-Люсия",
			"Сент-Пьер и Микелон",
			"Сербия",
			"Сейшелы",
			"Сингапур",
			"Синт-Мартен",
			"Сирийская Арабская Республика",
			"Словакия",
			"Словения",
			"Соединенное Королевство",
			"Соединенные Штаты",
			"Соломоновы острова",
			"Сомали",
			"Судан",
			"Суринам",
			"Сьерра-Леоне",
			"Таджикистан",
			"Таиланд",
			"Тайвань (Китай)",
			"Танзания, Объединенная Республика",
			"Тимор-Лесте",
			"Того",
			"Токелау",
			"Тонга",
			"Тринидад и Тобаго",
			"Тувалу",
			"Тунис",
			"Туркмения",
			"Турция",
			"Уганда",
			"Узбекистан",
			"Украина",
			"Уоллис и Футуна",
			"Уругвай",
			"Фарерские острова",
			"Фиджи",
			"Филиппины",
			"Финляндия",
			"Фолклендские острова (Мальвинские)",
			"Франция",
			"Французская Гвиана",
			"Французская Полинезия",
			"Французские Южные территории",
			"Хорватия",
			"Центрально-Африканская Республика",
			"Чад",
			"Черногория",
			"Чешская Республика",
			"Чили",
			"Швейцария",
			"Швеция",
			"Шпицберген и Ян Майен",
			"Шри-Ланка",
			"Эквадор",
			"Экваториальная Гвинея",
			"Эландские",
			"Эль-Сальвадор",
			"Эритрея",
			"Эстония",
			"Эфиопия",
			"Южная Африка",
			"Южная Джорджия и Южные Сандвичевы острова",
			"Южная Осетия",
			"Южный Судан",
			"Ямайка",
			"Япония"
		};
	
	public static String GetCountryById(int id) {
		if (id >= 0 && id < countries.length)
			return countries[id];
		else
			return "";
	}
	
	public static String GetPreviewPath(int videoId) {
		return NetUtils.URL_SERVER + "/videos_preview/" + videoId + ".jpg";
	}
	
	public static String GetPreviewPathLocal(int videoId) {
		return Environment.getExternalStorageDirectory() + "/VCMobile/PreviewCache/" + videoId + ".jpg";
	}
	
	public static final String VIDEO_CACHE_PATH = Environment.getExternalStorageDirectory() + "/VCMobile/VideoCache/";
	
	public static String GetPath(int id) {
		// Если есть в кэше
		String localPath = VIDEO_CACHE_PATH + Integer.toString(id); 
		File f = new File(localPath);
		
		if (f.exists())
			return localPath;
		else
			return NetUtils.URL_SERVER + "/videos/" + Integer.toString(id);
	}
	
	public static boolean isDownloaded(VCVideo video) {
		String localPath = VIDEO_CACHE_PATH + Integer.toString(video.video.id); 
		File f = new File(localPath);
		
		return f.exists();
	}
	
	public static boolean isDownloaded(int id) {
		String localPath = VIDEO_CACHE_PATH + Integer.toString(id); 
		File f = new File(localPath);
		
		return f.exists();
	}
	
	public static long GetCacheVideoSize(int id) {
		// Если есть в кэше
		String localPath = VIDEO_CACHE_PATH + Integer.toString(id);
		File f = new File(localPath);

		if (f.exists())
			return f.length();
		else
			return 0;
	}
	
	public static void DeleteFromCache(int id) {
		String localPath = VIDEO_CACHE_PATH + Integer.toString(id); 
		File f = new File(localPath);
		
		if (f.exists())
			f.delete();
	}
	
	public static void StartVideoPageActivity(Activity context, VCVideoJSON video, boolean fromList) {
		Intent intent = new Intent(context, VideoPageActivity.class);
		Bundle b = new Bundle();
		b.putInt("id", video.id);
		b.putString("title", video.title);
		b.putBoolean("fromList", fromList);
		intent.putExtras(b);
		
		context.startActivity(intent);
		
	}
	
	// Возвращает длительность length красиво отформатированную (для отображения на превьюшке)
	public static String GetFormatedLength(int length) {
		Integer minutes = length / 60;
		Integer seconds = length % 60;
		String txtLength;
		
		if (length >= 60*60) {
			Integer hours = minutes / 60;
			minutes = minutes - hours*60;
			txtLength = hours + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds); 
		}
		else {
			txtLength = minutes + ":" + String.format("%02d", seconds); ;
		}

		return txtLength;
	}
}