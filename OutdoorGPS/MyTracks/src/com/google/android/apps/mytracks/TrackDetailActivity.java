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

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.apps.mytracks.content.TrackDataHub;
import com.google.android.apps.mytracks.fragments.ChartFragment;
import com.google.android.apps.mytracks.fragments.ChooseActivityDialogFragment;
import com.google.android.apps.mytracks.fragments.DeleteOneTrackDialogFragment;
import com.google.android.apps.mytracks.fragments.DeleteOneTrackDialogFragment.DeleteOneTrackCaller;
import com.google.android.apps.mytracks.fragments.FrequencyDialogFragment;
import com.google.android.apps.mytracks.fragments.InstallEarthDialogFragment;
import com.google.android.apps.mytracks.fragments.InstallMapsDialogFragment;
import com.google.android.apps.mytracks.fragments.MapFragment;
import com.google.android.apps.mytracks.fragments.StatsFragment;
import com.google.android.apps.mytracks.io.file.SaveActivity;
import com.google.android.apps.mytracks.io.file.TrackWriterFactory.TrackFileFormat;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.settings.SettingsActivity;
import com.google.android.apps.mytracks.util.AnalyticsUtils;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.apps.mytracks.util.Constants;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.TrackRecordingServiceConnectionUtils;
import com.nogago.android.apps.tracks.content.MyTracksProviderUtils;
import com.nogago.android.apps.tracks.content.Track;
import com.nogago.android.apps.tracks.content.Waypoint;
import com.nogago.android.apps.tracks.content.WaypointCreationRequest;
import com.nogago.bb10.tracks.R;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
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
public class TrackDetailActivity extends AbstractMyTracksActivity implements DeleteOneTrackCaller {

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
        AnalyticsUtils.sendPageViews(TrackDetailActivity.this, "/action/detail/resume_track");
        updateMenuItems(true, false);
        TrackRecordingServiceConnectionUtils.resumeTrack(trackRecordingServiceConnection);
        trackController.update(true, false);
      } else {
        // Recording -> Paused
        AnalyticsUtils.sendPageViews(TrackDetailActivity.this, "/action/detail/pause_track");
        updateMenuItems(true, true);
        TrackRecordingServiceConnectionUtils.pauseTrack(trackRecordingServiceConnection);
        trackController.update(true, true);
      }
    }
  };

  private final OnClickListener stopListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      AnalyticsUtils.sendPageViews(TrackDetailActivity.this, "/action/detail/stop_recording");
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
        // Share Track
        shareTrackAction();
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
    AnalyticsUtils.sendPageViews(this, this.getLocalClassName() + "/create");
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
    EasyTracker.getInstance(this).activityStart(this); // Add this method.
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
    EasyTracker.getInstance(this).activityStop(this);
    trackDataHub.stop();
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
    menu.findItem(R.id.track_detail_mail_gpx).setTitle(
        getString(R.string.menu_save_format, fileTypes[0]));
    menu.findItem(R.id.track_detail_mail_kml).setTitle(
        getString(R.string.menu_save_format, fileTypes[1]));
    menu.findItem(R.id.track_detail_mail_csv).setTitle(
        getString(R.string.menu_save_format, fileTypes[2]));
    // menu.findItem(R.id.track_detail_save_tcx).setTitle(getString(R.string.menu_save_format,
    // fileTypes[3]));

    insertMarkerMenuItem = menu.findItem(R.id.track_detail_insert_marker);
    playNogagoMenuItem = menu.findItem(R.id.track_detail_play);
    playEarthMenuItem = menu.findItem(R.id.track_detail_earth_play); // Not
                                                                     // Supported
    if (Constants.IS_BLACKBERRY)
      menu.removeItem(R.id.track_detail_earth_play); // on
    shareMenuItem = menu.findItem(R.id.track_detail_share);
    saveMenuItem = menu.findItem(R.id.track_detail_mail);
    voiceFrequencyMenuItem = menu.findItem(R.id.track_detail_voice_frequency);
    splitFrequencyMenuItem = menu.findItem(R.id.track_detail_split_frequency);

    if (!Constants.isOnline(TrackDetailActivity.this)) {
      // Alternative would be to hide them
      menu.removeItem(R.id.track_detail_share);
      menu.removeItem(R.id.track_detail_earth_play);
    }
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
        AnalyticsUtils.sendPageViews(this, "/action/detail/markerInsert");
        insertMarkerAction();
        return true;
      case R.id.track_detail_share:
        shareTrackAction();
        return true;
      case R.id.track_detail_earth_play:
        AnalyticsUtils.sendPageViews(this, "/action/detail/earth");
        if (isEarthInstalled()) {
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
        AnalyticsUtils.sendPageViews(this, "/action/detail/navigateTrack");
        if (!TabManager.isMapsInstalled(this)) {
          Fragment fragment = getSupportFragmentManager().findFragmentByTag(
              InstallMapsDialogFragment.INSTALL_MAPS_DIALOG_TAG);
          if (fragment == null) {
            InstallMapsDialogFragment.newInstance().show(getSupportFragmentManager(),
                InstallMapsDialogFragment.INSTALL_MAPS_DIALOG_TAG);
          }
        } else {
          // Open Maps with command to navigate
          intent = IntentUtils.newIntent(this, SaveActivity.class)
              .putExtra(SaveActivity.EXTRA_TRACK_ID, trackId)
              .putExtra(SaveActivity.EXTRA_TRACK_FILE_FORMAT, (Parcelable) TrackFileFormat.GPXM)
              .putExtra(SaveActivity.EXTRA_FOLLOW_TRACK, true);
          // Then automatically opens nogago Maps based on the extras
          startActivity(intent);
        }
        return true;
      case R.id.track_detail_markers:
        AnalyticsUtils.sendPageViews(this, "/action/detail/marker/Detail");
        intent = IntentUtils.newIntent(this, MarkerListActivity.class).putExtra(
            MarkerListActivity.EXTRA_TRACK_ID, trackId);
        startActivity(intent);
        return true;
      case R.id.track_detail_voice_frequency:
        AnalyticsUtils.sendPageViews(this, "/action/detail/preferences/voice_frequency");
        FrequencyDialogFragment.newInstance(R.string.voice_frequency_key,
            PreferencesUtils.VOICE_FREQUENCY_DEFAULT, R.string.settings_voice_frequency_title)
            .show(getSupportFragmentManager(), FrequencyDialogFragment.FREQUENCY_DIALOG_TAG);
        return true;
      case R.id.track_detail_split_frequency:
        AnalyticsUtils.sendPageViews(this, "/action/detail/preferences/split_frequency");
        FrequencyDialogFragment.newInstance(R.string.split_frequency_key,
            PreferencesUtils.SPLIT_FREQUENCY_DEFAULT, R.string.settings_split_frequency_title)
            .show(getSupportFragmentManager(), FrequencyDialogFragment.FREQUENCY_DIALOG_TAG);
        return true;
      case R.id.track_detail_mail_gpx:
        AnalyticsUtils.sendPageViews(this, "/action/detail/mail/gpx");
        startSaveActivity(TrackFileFormat.GPX);
        return true;
      case R.id.track_detail_mail_kml:
        AnalyticsUtils.sendPageViews(this, "/action/detail/mail/kml");
        startSaveActivity(TrackFileFormat.KML);
        return true;
      case R.id.track_detail_mail_csv:
        AnalyticsUtils.sendPageViews(this, "/action/detail/mail/csv");
        startSaveActivity(TrackFileFormat.CSV);
        return true;
        /*
         * case R.id.track_detail_save_tcx:
         * startSaveActivity(TrackFileFormat.TCX); return true;
         */
      case R.id.track_detail_edit:
        AnalyticsUtils.sendPageViews(this, "/action/detail/edit_track");
        intent = IntentUtils.newIntent(this, TrackEditActivity.class).putExtra(
            TrackEditActivity.EXTRA_TRACK_ID, trackId);
        startActivity(intent);
        return true;
      case R.id.track_detail_delete:
        AnalyticsUtils.sendPageViews(this, "/action/detail/delete_track");
        DeleteOneTrackDialogFragment.newInstance(trackId).show(getSupportFragmentManager(),
            DeleteOneTrackDialogFragment.DELETE_ONE_TRACK_DIALOG_TAG);
        return true;
      case R.id.track_detail_sensor_state:
        AnalyticsUtils.sendPageViews(this, "/action/detail/sensor_state");
        intent = IntentUtils.newIntent(this, SensorStateActivity.class);
        startActivity(intent);
        return true;
      case R.id.track_detail_settings:
        AnalyticsUtils.sendPageViews(this, "/action/detail/settings");
        intent = IntentUtils.newIntent(this, SettingsActivity.class);
        startActivity(intent);
        return true;
      case R.id.track_detail_help:
        AnalyticsUtils.sendPageViews(this, "/action/detail/help");
        intent = IntentUtils.newIntent(this, HelpActivity.class);
        startActivity(intent);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void shareTrackAction() {
    AnalyticsUtils.sendPageViews(this, "/action/detail/share");
    String mapId = MyTracksProviderUtils.Factory.get(this).getTrack(trackId).getMapId(); // URL
                                                                                         // falls
                                                                                         // schon
                                                                                         // existent
    ChooseActivityDialogFragment.newInstance(trackId, (mapId.length() == 0 ? null : mapId)).show(
        getSupportFragmentManager(), ChooseActivityDialogFragment.CHOOSE_ACTIVITY_DIALOG_TAG);
  }

  private void insertMarkerAction() {
    // Recording
    Intent intent;
    AnalyticsUtils.sendPageViews(this, "/action/detail/insert_marker");
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
      if (!Constants.IS_BLACKBERRY)
        playNogagoMenuItem.setVisible(!isRecording);
    }
    if (shareMenuItem != null) {
        shareMenuItem.setVisible(!isRecording);
    }

    if (markerImageButton != null) {
      markerImageButton.setImageResource(isRecording ? R.drawable.ic_marker : R.drawable.ic_upload);
      markerImageButton.setContentDescription(getString(isRecording ? R.string.icon_marker
          : R.string.menu_edit));
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
    AnalyticsUtils.sendPageViews(this, "/action/detail/save");
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
            AnalyticsUtils.sendPageViews(TrackDetailActivity.this, "/action/detail/resume_track");
            updateMenuItems(true, false);
            TrackRecordingServiceConnectionUtils.resumeTrack(trackRecordingServiceConnection);
            trackController.update(true, false);
          } else {
            // Recording -> Paused
            Toast.makeText(getApplicationContext(), "Paused Recording", Toast.LENGTH_LONG).show();
            AnalyticsUtils.sendPageViews(TrackDetailActivity.this, "/action/detail/pause_track");
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
        AnalyticsUtils.sendPageViews(TrackDetailActivity.this, "/action/detail/stop_recording");
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

}