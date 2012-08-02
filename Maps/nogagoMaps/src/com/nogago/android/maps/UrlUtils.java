package com.nogago.android.maps;


public class UrlUtils {
	
	public static String getMapUrl(int mapId){
		return Constants.NOGAGO_MAP_URL + mapId + Constants.MAP_FILE_EXTENSION;
	}
	
	public static String getPoiUrl(int poiId){
		return Constants.NOGAGO_POI_URL + poiId + Constants.POI_FILE_EXTENSION;
	}

}
