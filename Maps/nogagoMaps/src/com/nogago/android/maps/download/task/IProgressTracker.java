package com.nogago.android.maps.download.task;


public interface IProgressTracker {
	
	
    /** Updates progress with just a message */
    void setProgressMessage(String message);
    
    /** Updates progress with a percentage */
    void onProgress(int progressPercentage);
    
    
    /** Notifies about task completeness */
    void onComplete();
}