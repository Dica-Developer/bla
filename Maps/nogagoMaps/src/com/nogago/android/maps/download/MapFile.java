package com.nogago.android.maps.download;

import java.io.File;

import com.nogago.android.maps.Constants;

import android.os.Environment;


/**
 * A POJO for representing the map files on SD Card
 * @author Android
 *
 */
public class MapFile {

	String fullPath;
	String poiPath;
	String fullName;
	String name;
	String part;
	int mapId;
	
	
	/** Counter part of encoding at Area.getMapFileName() */
	public MapFile(File f) {
	
		fullName = f.getName();
		fullPath = f.getAbsolutePath();
		String parts[] = fullName.split("[;\\.]");
		name = parts[0].replace('_', ' ');
		part = parts[1];
		mapId = new Integer(parts[2]).intValue();
		poiPath = Environment.getExternalStorageDirectory().toString()
					+ Constants.POI_PATH + fullName.substring(0, fullName.lastIndexOf(".")) + Constants.POI_FILE_EXTENSION;
		
		}
	
	public boolean equals(Object o) {
		return (o instanceof MapFile && ((MapFile) o).mapId==mapId) ;
	}
	
	public String getName() {
		return name;
	}
	
	public String getFullPath(){
		return this.fullPath;
	}
	
	public String toString() {
		String title = getName() + " (" + part +")";
		return title;
	}
	
	public int getMapId(){
		return this.mapId;
	}
	
	public String getPoiPath(){
		return this.poiPath;
	}
}
