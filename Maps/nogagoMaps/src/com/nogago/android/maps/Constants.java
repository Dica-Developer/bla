package com.nogago.android.maps;

public class Constants {
	
	public static final String NOMINATIM_SEARCH_URL = "http://www.nogago.com/nominatim/search";
	public static final String ROUTING_SERVICE_URL = "http://route.nogago.com/gosmore.php";
	public static final String POI_SEARCH_URL = "http://www.nogago.com/poi/search";
	public static final float POI_SEARCH_DEVIATION = 0.0001f;
	public static final float POI_SEARCH_THRESHOLD = 0.1f;
	public static final double POI_PERIMETER_LIMIT = 10000.0;
	
	public static final String NOGAGO_MAP_MANAGER_COMPONENT = "com.nogago.android.download";
	public static final String NOGAGO_MAP_DOWNLOADER_ACTIVITY = "com.nogago.android.download.ui.SplashActivity";
	public static final String TRACKS_DOWNLOAD_URL = "market://search?q=pname:com.nogago.android.tracks";
	
	public static final String STRING_LONGITUDE = "longitude";
	public static final String STRING_LATITUDE = "latitude";
	
	public static final String POITYPES_ASSET = "poi-type.xml";
	
	public static final String BASEMAP_NAME_DIR = "basemap";
	public static final String BASEMAP_NAME_PREFIX = "World_Basemap";
	public static final String BASEMAP_NAME_EXT = ".obf";
	public static final String BASEMAP_NAME = BASEMAP_NAME_PREFIX + BASEMAP_NAME_EXT;
	
	public static final String APP_PACKAGE_NAME = "com.nogago.android.maps";
	
	public static final int MAX_LOADED_WAYPOINTS_POINTS = 10000;
	
	public static final String MYTRACKS_SERVICE_PACKAGE = "com.nogago.android.tracks";
	public static final String MYTRACKS_SERVICE_CLASS = "com.nogago.android.tracks.services.TrackRecordingService";
	public static final long BAD_MYTRACKS_TRACK_ID = -1L;
	
	//nogago Map Downloader
	public final static String STORAGE_PATH = "/nogago/";
	public final static String TEMP_PATH = STORAGE_PATH + "temp/";
	public final static String POI_PATH = STORAGE_PATH + "POI/";
	public final static String NOGAGO_STORAGE_URL = "https://download.nogago.com/latest/";
	public final static String NOGAGO_CONTOURS_STORAGE_URL = "https://download.nogago.com/contours/";
	public final static String NOGAGO_MAP_URL = NOGAGO_STORAGE_URL + "maps/";
	public final static String NOGAGO_POI_URL = NOGAGO_STORAGE_URL + "pois/";
	public final static String MAP_FILE_EXTENSION = ".obf";
	public final static String POLY_FILE_EXTENSION = ".poly";
	public final static String POI_FILE_EXTENSION = ".poi.odb";
	public final static String STRING_LONGITUDE_POSTFIX = "longitude";
	public final static String STRING_LATITUDE_POSTFIX = "latitude";
	public final static int HTTP_CODE_401 = 401;
	public final static String URL_ENCODING = "UTF-8";
	public final static int BAD_OBF_FILE_THRESHOLD = 10000;
	
	public final static String NOGAGO_REGISTER_URL = "https://www.nogago.com/userRegister/index";
	
}
