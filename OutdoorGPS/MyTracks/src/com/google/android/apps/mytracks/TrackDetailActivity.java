/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.apps.mytracks;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TrackDataHub;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.WaypointCreationRequest;
import com.google.android.apps.mytracks.fragments.ChartFragment;
import com.google.android.apps.mytracks.fragments.DeleteOneTrackDialogFragment;
import com.google.android.apps.mytracks.fragments.DeleteOneTrackDialogFragment.DeleteOneTrackCaller;
import com.google.android.apps.mytracks.fragments.FrequencyDialogFragment;
import com.google.android.apps.mytracks.fragments.InstallEarthDialogFragment;
import com.google.android.apps.mytracks.fragments.InstallMapsDialogFragment;
import com.google.android.apps.mytracks.fragments.MapFragment;
import com.google.android.apps.mytracks.fragments.StatsFragment;
import com.google.android.apps.mytracks.io.file.SaveActivity;
import com.google.android.apps.mytracks.io.file.TrackWriterFactory.TrackFileFormat;
import com.google.android.apps.mytracks.io.sendtogoogle.GPXUploadTask;
import com.google.android.apps.mytracks.io.sendtogoogle.UploadTaskException;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.settings.SettingsActivity;
import com.google.android.apps.mytracks.util.AnalyticsUtils;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.TrackRecordingServiceConnectionUtils;
import com.nogago.android.task.AsyncTaskManager;
import com.nogago.android.task.OnTaskCompleteListener;
import com.nogago.bb10.tracks.R;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;

import java.util.List;

/**
 * An activity to show the track detail.
 * 
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public class TrackDetailActivity extends AbstractMyTracksActivity implements DeleteOneTrackCaller,
    OnTaskCompleteListener {

  public static final String EXTRA_TRACK_ID = "track_id";
  public static final String EXTRA_MARKER_ID = "marker_id";

  private static final String TAG = TrackDetailActivity.class.getSimpleName();
  private static final String CURRENT_TAB_TAG_KEY = "current_tab_tag_key";

  // The following are set in onCreate
  private TrackRecordingServiceConnection trackRecordingServiceConnection;
  private TrackDataHub trackDataHub;
  private View mapViewContainer;
  private TabHost tabHost;
  private TabManager tabManager;
  private TrackController trackController;

  // From intent
  public long trackId;
  private long markerId;

  // Preferences
  private long recordingTrackId;
  private boolean recordingTrackPaused;

  private MenuItem insertMarkerMenuItem;
  private MenuItem playNogagoMenuItem;
  private MenuItem playEarthMenuItem;
  private MenuItem shareMenuItem;
  private MenuItem sendGoogleMenuItem;
  private MenuItem saveMenuItem;
  private MenuItem voiceFrequencyMenuItem;
  private MenuItem splitFrequencyMenuItem;
  private ImageButton markerImageButton;
  private ImageButton recordImageButton;

  private final Runnable bindChangedCallback = new Runnable() {
    @Override
    public void run() {
      // After binding changes (is available), update the total time in
      // trackController.
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          trackController.update(trackId == recordingTrackId, recordingTrackPaused);
        }
      });
    }
  };

  /*
   * Note that sharedPreferenceChangeListener cannot be an anonymous inner
   * class. Anonymous inner class will get garbage collected.
   */
  private final OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
      // Note that key can be null
      if (key == null
          || key.equals(PreferencesUtils.getKey(TrackDetailActivity.this,
              R.string.recording_track_id_key))) {
        recordingTrackId = PreferencesUtils.getLong(TrackDetailActivity.this,
            R.string.recording_track_id_key);
      }
      if (key == null
          || key.equals(PreferencesUtils.getKey(TrackDetailActivity.this,
              R.string.recording_track_paused_key))) {
        recordingTrackPaused = PreferencesUtils.getBoolean(TrackDetailActivity.this,
            R.string.recording_track_paused_key, PreferencesUtils.RECORDING_TRACK_PAUSED_DEFAULT);
      }
      if (key != null) {
        boolean isRecording = trackId == recordingTrackId;
        updateMenuItems(isRecording, recordingTrackPaused);
        trackController.update(isRecording, recordingTrackPaused);
      }
    }
  };

  private final OnClickListener recordListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      if (recordingTrackPaused) {
        // Paused -> Resume
        AnalyticsUtils.sendPageViews(TrackDetailActivity.this, "/action/resume_track");
        updateMenuItems(true, false);
        TrackRecordingServiceConnectionUtils.resumeTrack(trackRecordingServiceConnection);
        trackController.update(true, false);
      } else {
        // Recording -> Paused
        AnalyticsUtils.sendPageViews(TrackDetailActivity.this, "/action/pause_track");
        updateMenuItems(true, true);
        TrackRecordingServiceConnectionUtils.pauseTrack(trackRecordingServiceConnection);
        trackController.update(true, true);
      }
    }
  };

  private final OnClickListener stopListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      AnalyticsUtils.sendPageViews(TrackDetailActivity.this, "/action/stop_recording");
      updateMenuItems(false, true);
      TrackRecordingServiceConnectionUtils.stopRecording(TrackDetailActivity.this,
          trackRecordingServiceConnection, true);
    }
  };

  private final OnClickListener markerListener = new OnClickListener() {
    @Override
    public void onClick(View v) {

      if (trackId == recordingTrackId) {
        // Recording
        insertMarkerAction();
      } else {
        sendTrackToNogago();
      }
    }
  };

  /**
   * We are not displaying driving directions. Just an arbitrary track that is
   * not associated to any licensed mapping data. Therefore it should be okay to
   * return false here and still comply with the terms of service.
   */
  // @Override
  protected boolean isRouteDisplayed() {
    return false;
  }

  /**
   * We are displaying a location. This needs to return true in order to comply
   * with the terms of service.
   */
  // @Override
  protected boolean isLocationDisplayed() {
    return true;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    handleIntent(getIntent());
    SharedPreferences sharedPreferences = getSharedPreferences(Constants.SETTINGS_NAME,
        Context.MODE_PRIVATE);
    sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    sharedPreferenceChangeListener.onSharedPreferenceChanged(sharedPreferences, null);

    trackRecordingServiceConnection = new TrackRecordingServiceConnection(this, bindChangedCallback);
    trackDataHub = TrackDataHub.newInstance(this);

    mapViewContainer = getLayoutInflater().inflate(R.layout.map, null);
    ApiAdapterFactory.getApiAdapter().disableHardwareAccelerated(mapViewContainer);

    tabHost = (TabHost) findViewById(android.R.id.tabhost);
    tabHost.setup();
    tabManager = new TabManager(this, tabHost, R.id.realtabcontent);
    TabSpec chartTabSpec = tabHost.newTabSpec(ChartFragment.CHART_FRAGMENT_TAG).setIndicator(
        getString(R.string.track_detail_chart_tab),
        getResources().getDrawable(R.drawable.tab_chart));
    TabSpec mapTabSpec = tabHost.newTabSpec(MapFragment.MAP_FRAGMENT_TAG).setIndicator(
        getString(R.string.track_detail_map_tab), getResources().getDrawable(R.drawable.tab_map));
    TabSpec statsTabSpec = tabHost.newTabSpec(StatsFragment.STATS_FRAGMENT_TAG).setIndicator(
        getString(R.string.track_detail_stats_tab),
        getResources().getDrawable(R.drawable.tab_stats));
    tabManager.addTab(statsTabSpec, StatsFragment.class, null); // 0 Stats
    tabManager.addTab(chartTabSpec, ChartFragment.class, null); // 1 Chart
    tabManager.addTab(mapTabSpec, MapFragment.class, null); // 2 Map
    if (savedInstanceState != null) {
      tabHost.setCurrentTabByTag(savedInstanceState.getString(CURRENT_TAB_TAG_KEY));
    }

    // Setup Action Bar

    ImageButton backButton = (ImageButton) findViewById(R.id.listBtnBarBack);
    if (backButton != null)
      backButton.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          TrackDetailActivity.this.finish();
        }
      });

    // Buttons 3 managed by Track Controller

    recordImageButton = (ImageButton) findViewById(R.id.listBtnBarRecord);
    if (recordImageButton != null)
      recordImageButton.setOnClickListener(recordListener);

    markerImageButton = (ImageButton) findViewById(R.id.listBtnBarMarker);
    if (markerImageButton != null)
      markerImageButton.setOnClickListener(markerListener);

    ImageButton moreButton = (ImageButton) findViewById(R.id.listBtnBarMore);
    if (moreButton != null)
      moreButton.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          openOptionsMenu();
        }
      });

    trackController = new TrackController(this, trackRecordingServiceConnection, false,
        recordListener, stopListener);
    showMarker();

  }

  @Override
  protected int getLayoutResId() {
    return R.layout.track_detail;
  }

  @Override
  protected boolean hideTitle() {
    return false;
  }

  @Override
  protected void onStart() {
    super.onStart();
    trackDataHub.start();
  }

  @Override
  protected void onPause() {
    super.onPause();
    trackController.stop();
  }

  @Override
  protected void onResume() {
    super.onResume();
    trackDataHub.loadTrack(trackId);
    TrackRecordingServiceConnectionUtils.resumeConnection(this, trackRecordingServiceConnection);
    trackController.update(trackId == recordingTrackId, recordingTrackPaused);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(CURRENT_TAB_TAG_KEY, tabHost.getCurrentTabTag());
  }

  @Override
  protected void onStop() {
    super.onStop();
    trackDataHub.stop();
    AnalyticsUtils.dispatch();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    trackRecordingServiceConnection.unbind();
  }

  @Override
  protected void onHomeSelected() {
    Intent intent = IntentUtils.newIntent(this, TrackListActivity.class);
    startActivity(intent);
    finish();
  }

  @Override
  public void onNewIntent(Intent intent) {
    setIntent(intent);
    handleIntent(intent);
    showMarker();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.track_detail, menu);
    String fileTypes[] = getResources().getStringArray(R.array.file_types);
    menu.findItem(R.id.track_detail_save_gpx).setTitle(
        getString(R.string.menu_save_format, fileTypes[0]));
    menu.findItem(R.id.track_detail_save_kml).setTitle(
        getString(R.string.menu_save_format, fileTypes[1]));
    menu.findItem(R.id.track_detail_save_csv).setTitle(
        getString(R.string.menu_save_format, fileTypes[2]));
    // menu.findItem(R.id.track_detail_save_tcx).setTitle(getString(R.string.menu_save_format,
    // fileTypes[3]));

    insertMarkerMenuItem = menu.findItem(R.id.track_detail_insert_marker);
    playNogagoMenuItem = menu.findItem(R.id.track_detail_play);
    playEarthMenuItem = menu.findItem(R.id.track_detail_earth_play); // Not Supported
    if(Constants.IS_BLACKBERRY) menu.removeItem(R.id.track_detail_earth_play);                                                         // on
    // BB
    // shareMenuItem = menu.findItem(R.id.track_detail_share);
    sendGoogleMenuItem = menu.findItem(R.id.track_detail_send_nogago);
    saveMenuItem = menu.findItem(R.id.track_detail_mail);
    voiceFrequencyMenuItem = menu.findItem(R.id.track_detail_voice_frequency);
    splitFrequencyMenuItem = menu.findItem(R.id.track_detail_split_frequency);

    updateMenuItems(trackId == recordingTrackId, recordingTrackPaused);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    String sensorTypeValueNone = getString(R.string.sensor_type_value_none);
    boolean showSensorState = !sensorTypeValueNone.equals(PreferencesUtils.getString(this,
        R.string.sensor_type_key, sensorTypeValueNone));
    menu.findItem(R.id.track_detail_sensor_state).setVisible(showSensorState);
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent intent;
    switch (item.getItemId()) {

      case R.id.track_detail_insert_marker:
        insertMarkerAction();
        return true;
      case R.id.track_detail_earth_play:
        if (isEarthInstalled()) {
          AnalyticsUtils.sendPageViews(this, "/action/play");
          intent = IntentUtils.newIntent(this, SaveActivity.class)
              .putExtra(SaveActivity.EXTRA_TRACK_ID, trackId)
              .putExtra(SaveActivity.EXTRA_TRACK_FILE_FORMAT, (Parcelable) TrackFileFormat.KML)
              .putExtra(SaveActivity.EXTRA_PLAY_TRACK, true);
          startActivity(intent);
        } else {
          Fragment fragment = getSupportFragmentManager().findFragmentByTag(
              InstallEarthDialogFragment.INSTALL_EARTH_DIALOG_TAG);
          if (fragment == null) {
            InstallEarthDialogFragment.newInstance().show(getSupportFragmentManager(),
                InstallEarthDialogFragment.INSTALL_EARTH_DIALOG_TAG);
          }
        }
        return true;
      case R.id.track_detail_play:
        if (!TabManager.isMapsInstalled(this)) {
          Fragment fragment = getSupportFragmentManager().findFragmentByTag(
              InstallMapsDialogFragment.INSTALL_MAPS_DIALOG_TAG);
          if (fragment == null) {
            InstallMapsDialogFragment.newInstance().show(getSupportFragmentManager(),
                InstallMapsDialogFragment.INSTALL_MAPS_DIALOG_TAG);
          }
        } else {
          // Open Maps with command to navigate
          if (Constants.IS_BLACKBERRY) {
            Toast.makeText(this, R.string.track_detail_maps_blackberry_msg, Toast.LENGTH_LONG);
          }
          // TODO Need to save the track first
          intent =  IntentUtils.newIntent(this, SaveActivity.class)
              .putExtra(SaveActivity.EXTRA_TRACK_ID, trackId)
              .putExtra(SaveActivity.EXTRA_TRACK_FILE_FORMAT, (Parcelable) TrackFileFormat.GPX )
              .putExtra(SaveActivity.EXTRA_FOLLOW_TRACK, true);
          // Then automatically opens nogago Maps based on the extras
          startActivity(intent);
        }
        return true;
      case R.id.track_detail_markers:
        intent = IntentUtils.newIntent(this, MarkerListActivity.class).putExtra(
            MarkerListActivity.EXTRA_TRACK_ID, trackId);
        startActivity(intent);
        return true;
      case R.id.track_detail_voice_frequency:
        FrequencyDialogFragment.newInstance(R.string.voice_frequency_key,
            PreferencesUtils.VOICE_FREQUENCY_DEFAULT, R.string.settings_voice_frequency_title)
            .show(getSupportFragmentManager(), FrequencyDialogFragment.FREQUENCY_DIALOG_TAG);
        return true;
      case R.id.track_detail_split_frequency:
        FrequencyDialogFragment.newInstance(R.string.split_frequency_key,
            PreferencesUtils.SPLIT_FREQUENCY_DEFAULT, R.string.settings_split_frequency_title)
            .show(getSupportFragmentManager(), FrequencyDialogFragment.FREQUENCY_DIALOG_TAG);
        return true;
      case R.id.track_detail_send_nogago:
        sendTrackToNogago();
        return true;
      case R.id.track_detail_save_gpx:
        startSaveActivity(TrackFileFormat.GPX);
        return true;
      case R.id.track_detail_save_kml:
        startSaveActivity(TrackFileFormat.KML);
        return true;
      case R.id.track_detail_save_csv:
        startSaveActivity(TrackFileFormat.CSV);
        return true;
        /*
         * case R.id.track_detail_save_tcx:
         * startSaveActivity(TrackFileFormat.TCX); return true;
         */
      case R.id.track_detail_edit:
        intent = IntentUtils.newIntent(this, TrackEditActivity.class).putExtra(
            TrackEditActivity.EXTRA_TRACK_ID, trackId);
        startActivity(intent);
        return true;
      case R.id.track_detail_delete:
        DeleteOneTrackDialogFragment.newInstance(trackId).show(getSupportFragmentManager(),
            DeleteOneTrackDialogFragment.DELETE_ONE_TRACK_DIALOG_TAG);
        return true;
      case R.id.track_detail_sensor_state:
        intent = IntentUtils.newIntent(this, SensorStateActivity.class);
        startActivity(intent);
        return true;
      case R.id.track_detail_settings:
        intent = IntentUtils.newIntent(this, SettingsActivity.class);
        startActivity(intent);
        return true;
      case R.id.track_detail_help:
        intent = IntentUtils.newIntent(this, HelpActivity.class);
        startActivity(intent);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  /*
   * public void uploadTrackAction() { AnalyticsUtils.sendPageViews(this,
   * "/action/send_nogago"); ChooseUploadServiceDialogFragment.newInstance(new
   * SendRequest(trackId)).show( getSupportFragmentManager(),
   * ChooseUploadServiceDialogFragment.CHOOSE_UPLOAD_SERVICE_DIALOG_TAG); }
   */

  private void insertMarkerAction() {
    // Recording
    Intent intent;
    AnalyticsUtils.sendPageViews(this, "/action/insert_marker");
    intent = IntentUtils.newIntent(this, MarkerEditActivity.class).putExtra(
        MarkerEditActivity.EXTRA_TRACK_ID, trackId);
    startActivity(intent);
  }

  @Override
  public boolean onTrackballEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      if (trackId == recordingTrackId && !recordingTrackPaused) {
        TrackRecordingServiceConnectionUtils.addMarker(this, trackRecordingServiceConnection,
            WaypointCreationRequest.DEFAULT_WAYPOINT);
        return true;
      }
    }
    return super.onTrackballEvent(event);
  }

  @Override
  public TrackRecordingServiceConnection getTrackRecordingServiceConnection() {
    return trackRecordingServiceConnection;
  }

  /**
   * Gets the map view container.
   */
  public View getMapViewContainer() {
    return mapViewContainer;
  }

  /**
   * Gets the {@link TrackDataHub}.
   */
  public TrackDataHub getTrackDataHub() {
    return trackDataHub;
  }

  /**
   * Handles the data in the intent.
   */
  private void handleIntent(Intent intent) {
    trackId = intent.getLongExtra(EXTRA_TRACK_ID, -1L);
    markerId = intent.getLongExtra(EXTRA_MARKER_ID, -1L);
    if (markerId != -1L) {
      Waypoint waypoint = MyTracksProviderUtils.Factory.get(this).getWaypoint(markerId);
      if (waypoint == null) {
        exit();
        return;
      }
      trackId = waypoint.getTrackId();
    }
    if (trackId == -1L) {
      exit();
      return;
    }
  }

  /**
   * Exists and returns to {@link TrackListActivity}.
   */
  private void exit() {
    Intent newIntent = IntentUtils.newIntent(this, TrackListActivity.class);
    startActivity(newIntent);
    finish();
  }

  /**
   * Shows marker.
   */
  private void showMarker() {
    if (markerId != -1L) {
      MapFragment mapFragmet = (MapFragment) getSupportFragmentManager().findFragmentByTag(
          MapFragment.MAP_FRAGMENT_TAG);
      if (mapFragmet != null) {
        tabHost.setCurrentTabByTag(MapFragment.MAP_FRAGMENT_TAG);
        mapFragmet.showMarker(trackId, markerId);
      } else {
        Log.e(TAG, "MapFragment is null");
      }
    }
  }

  /**
   * Updates the menu items.
   * 
   * @param isRecording true if recording
   */
  private void updateMenuItems(boolean isRecording, boolean isPaused) {

    if (insertMarkerMenuItem != null) {
      insertMarkerMenuItem.setVisible(isRecording && !isPaused);
    }
    if (playNogagoMenuItem != null) {
      playNogagoMenuItem.setVisible(!isRecording);
    }
    if (playEarthMenuItem != null) {
      playNogagoMenuItem.setVisible(!isRecording);
    }
    if (shareMenuItem != null) {
      shareMenuItem.setVisible(!isRecording);
    }
    if (sendGoogleMenuItem != null) {
      sendGoogleMenuItem.setVisible(!isRecording);
    }

    if (markerImageButton != null) {
      markerImageButton.setImageResource(isRecording ? R.drawable.ic_marker : R.drawable.ic_upload);
      markerImageButton.setContentDescription(getString(isRecording ? R.string.icon_marker
          : R.string.icon_upload));
    }
    if (recordImageButton != null) {
      recordImageButton.setImageResource(isRecording && !isPaused ? R.drawable.ic_pause
          : R.drawable.ic_record);
      recordImageButton
          .setContentDescription(getString(isRecording && !isPaused ? R.string.icon_pause_recording
              : R.string.icon_record_track));
    }
    if (saveMenuItem != null) {
      saveMenuItem.setVisible(!isRecording);
    }
    if (voiceFrequencyMenuItem != null) {
      voiceFrequencyMenuItem.setVisible(isRecording);
    }
    if (splitFrequencyMenuItem != null) {
      splitFrequencyMenuItem.setVisible(isRecording);
    }

    String title;
    if (isRecording) {
      title = getString(isPaused ? R.string.generic_paused : R.string.generic_recording);
    } else {
      Track track = MyTracksProviderUtils.Factory.get(this).getTrack(trackId);
      title = track != null ? track.getName() : getString(R.string.my_tracks_app_name);
    }
    setTitle(title);
  }

  /**
   * Starts the {@link SaveActivity} to save a track.
   * 
   * @param trackFileFormat the track file format
   */
  private void startSaveActivity(TrackFileFormat trackFileFormat) {
    AnalyticsUtils.sendPageViews(this, "/action/save");
    if (FileUtils.isSdCardAvailable()) {
      Intent intent = IntentUtils.newIntent(this, SaveActivity.class)
          .putExtra(SaveActivity.EXTRA_TRACK_ID, trackId)
          .putExtra(SaveActivity.EXTRA_MAIL_TRACK, true)
          .putExtra(SaveActivity.EXTRA_TRACK_FILE_FORMAT, (Parcelable) trackFileFormat);
      startActivity(intent);
    } else {
      Toast.makeText(this, getString(R.string.sd_card_error_no_storage), Toast.LENGTH_SHORT).show();
    }
  }

  /**
   * Returns true if Google Earth app is installed.
   */
  private boolean isEarthInstalled() {
    List<ResolveInfo> infos = getPackageManager().queryIntentActivities(
        new Intent().setType(SaveActivity.GOOGLE_EARTH_KML_MIME_TYPE),
        PackageManager.MATCH_DEFAULT_ONLY);
    for (ResolveInfo info : infos) {
      if (info.activityInfo != null && info.activityInfo.packageName != null
          && info.activityInfo.packageName.equals(SaveActivity.GOOGLE_EARTH_PACKAGE)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if Google Earth app is installed.
   */
  private boolean isMapsInstalled() {
    try {
      getPackageManager().getActivityInfo(Constants.MAPS_COMPONENT, PackageManager.GET_META_DATA);
      return true;
    } catch (NameNotFoundException nnfe) {
      return false;
    }
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    switch (keyCode) {
      case KeyEvent.KEYCODE_M:
        if (trackId == recordingTrackId) {
          // Recording
          insertMarkerAction();
        }
        break;
      case KeyEvent.KEYCODE_H:
        if (tabHost.getCurrentTab() == 1) {
          ChartFragment f = (ChartFragment) tabManager.getCurrentFragment();
          f.scrollLeft();
        }
        break;
      case KeyEvent.KEYCODE_K:
        if (tabHost.getCurrentTab() == 1) {
          ChartFragment f = (ChartFragment) tabManager.getCurrentFragment();
          f.scrollRight();
        }
        break;
      case KeyEvent.KEYCODE_0:
        if (tabHost.getCurrentTab() == 2) {
          MapFragment f = (MapFragment) tabManager.getCurrentFragment();
          f.showMyLocation();
        }
        break;

      case KeyEvent.KEYCODE_Q:
        tabHost.setCurrentTab(0);
        break;

      case KeyEvent.KEYCODE_SPACE:
        if ((tabHost.getCurrentTab() == 1)
            && PreferencesUtils.getBoolean(this, R.string.settings_mapsprovider, true)) {
          // Use nogago Maps
        } else {
          tabHost.setCurrentTab(tabHost.getCurrentTab() + 1);
        }
        break;

      case KeyEvent.KEYCODE_I:
      case KeyEvent.KEYCODE_VOLUME_DOWN:
        if (tabHost.getCurrentTab() == 0) {
          Toast.makeText(getApplicationContext(), "Cannot zoom in on statistics",
              Toast.LENGTH_SHORT).show();
        } else if (tabHost.getCurrentTab() == 1) {
          ChartFragment f = (ChartFragment) tabManager.getCurrentFragment();
          f.zoomIn();
          // Toast.makeText(getApplicationContext(), "Currently at " +
          // f.getClass().toString(), Toast.LENGTH_SHORT).show();
        } else if (tabHost.getCurrentTab() == 2) {
          MapFragment f = (MapFragment) tabManager.getCurrentFragment();
          f.zoomIn();
          // Toast.makeText(getApplicationContext(), "Currently at " +
          // f.getClass().toString(), Toast.LENGTH_SHORT).show();
        }
        break;
      case KeyEvent.KEYCODE_O:
      case KeyEvent.KEYCODE_VOLUME_UP:
        if (tabHost.getCurrentTab() == 0) {
          Toast.makeText(getApplicationContext(), "Cannot zoom in on statistics",
              Toast.LENGTH_SHORT).show();
        } else if (tabHost.getCurrentTab() == 1) {
          ChartFragment f = (ChartFragment) tabManager.getCurrentFragment();
          f.zoomOut();
          // Toast.makeText(getApplicationContext(), "Currently at " +
          // f.getClass().toString(), Toast.LENGTH_SHORT).show();
        } else if (tabHost.getCurrentTab() == 2) {
          MapFragment f = (MapFragment) tabManager.getCurrentFragment();
          f.zoomOut();
          // Toast.makeText(getApplicationContext(), "Currently at " +
          // f.getClass().toString(), Toast.LENGTH_SHORT).show();
        }
        break;
      case KeyEvent.KEYCODE_P:
        if (recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT) {
          if (recordingTrackPaused) {
            // Paused -> Resume
            Toast.makeText(getApplicationContext(), "Resumed Recording", Toast.LENGTH_LONG).show();
            AnalyticsUtils.sendPageViews(TrackDetailActivity.this, "/action/resume_track");
            updateMenuItems(true, false);
            TrackRecordingServiceConnectionUtils.resumeTrack(trackRecordingServiceConnection);
            trackController.update(true, false);
          } else {
            // Recording -> Paused
            Toast.makeText(getApplicationContext(), "Paused Recording", Toast.LENGTH_LONG).show();
            AnalyticsUtils.sendPageViews(TrackDetailActivity.this, "/action/pause_track");
            updateMenuItems(true, true);
            TrackRecordingServiceConnectionUtils.pauseTrack(trackRecordingServiceConnection);
            trackController.update(true, true);
          }
        } else {
          Toast.makeText(getApplicationContext(), "Cannot pause track that is not recorded.",
              Toast.LENGTH_LONG).show();
        }
        break;

      case KeyEvent.KEYCODE_S:
        AnalyticsUtils.sendPageViews(TrackDetailActivity.this, "/action/stop_recording");
        updateMenuItems(false, true);
        TrackRecordingServiceConnectionUtils.stopRecording(TrackDetailActivity.this,
            trackRecordingServiceConnection, true);
        break;

      case KeyEvent.KEYCODE_A:
        Intent intent = IntentUtils.newIntent(this, SettingsActivity.class);
        startActivity(intent);
        break;

    }

    return super.onKeyUp(keyCode, event);
  }

  // Upload to nogago

  private boolean isOnline() {
    ConnectivityManager cm = (ConnectivityManager) getSystemService(TrackDetailActivity.this.CONNECTIVITY_SERVICE);
    if (cm == null)
      return false;
    NetworkInfo ni = cm.getActiveNetworkInfo();
    if (ni == null)
      return false;
    return ni.isConnectedOrConnecting();
  }

  private String getUserName() {
    return PreferencesUtils.getString(TrackDetailActivity.this, R.string.user_name, "");
  }

  private String getPassword() {
    return PreferencesUtils.getString(TrackDetailActivity.this, R.string.user_password, "");
  }

  private void sendTrackToNogago() {
    if (isOnline()) {
      if (getUserName() != null && (getUserName().length() > 0)
          && getUserName().compareTo("username") != 0) {
        // Create manager and set this activity as context and listener
        AsyncTaskManager mAsyncTaskManager = new AsyncTaskManager(this, this, "TEST", false);
        mAsyncTaskManager.handleRetainedTask(getLastNonConfigurationInstance());
        GPXUploadTask task = new GPXUploadTask(this, getString(R.string.upload_progressbar_title),
            getUserName(), getPassword(), trackId);
        mAsyncTaskManager.setupTask(task);
      } else {
        android.content.DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            Intent settings = new Intent(TrackDetailActivity.this, SettingsActivity.class);
            startActivity(settings);
          }
        };
        showAlertDialog(this, getString(R.string.wrong_credential),
            getString(R.string.error_username), listener);
      }

    } else {
      Toast.makeText(TrackDetailActivity.this, R.string.error_network, Toast.LENGTH_LONG).show();
    }
  }

  private void showAlertDialog(Context context, String title, String message,
      android.content.DialogInterface.OnClickListener listener) {
    Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(title);
    builder.setIcon(android.R.drawable.ic_dialog_info);
    builder.setMessage(message);
    builder.setNeutralButton(R.string.ok_button, listener);
    builder.show();
  }

  @Override
  public void onTaskComplete(AsyncTask task, Object result) {
    Toast.makeText(this, getString(R.string.successfully_uploaded_track), Toast.LENGTH_SHORT)
        .show();

  }

  @Override
  public void onTaskCancel(AsyncTask task) {
    Toast.makeText(this, getString(R.string.sd_card_canceled), Toast.LENGTH_SHORT).show();

  }

  @Override
  public void onTaskError(AsyncTask task, Exception error) {
    if (error instanceof UploadTaskException
        && ((UploadTaskException) error).getId() == UploadTaskException.CREDENTIALS_WRONG) {
      android.content.DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          Intent settings = new Intent(TrackDetailActivity.this, SettingsActivity.class);
          startActivity(settings);
        }
      };
      showAlertDialog(this, getString(R.string.wrong_credential),
          getString(R.string.error_username), listener);
    } else if (error instanceof UploadTaskException
        && ((UploadTaskException) error).getId() == UploadTaskException.UNABLE_TO_CONNECT) {

      Toast.makeText(TrackDetailActivity.this, R.string.error_network, Toast.LENGTH_LONG).show();

    } else {
      Toast.makeText(this, getString(R.string.sd_card_canceled), Toast.LENGTH_SHORT).show();
    }
  }
}