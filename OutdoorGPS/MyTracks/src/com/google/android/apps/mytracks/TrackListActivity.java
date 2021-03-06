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
import com.google.android.apps.mytracks.content.TrackDataListener;
import com.google.android.apps.mytracks.content.TrackDataType;
import com.google.android.apps.mytracks.fragments.CheckUnitsDialogFragment;
import com.google.android.apps.mytracks.fragments.DeleteAllTrackDialogFragment;
import com.google.android.apps.mytracks.fragments.DeleteOneTrackDialogFragment;
import com.google.android.apps.mytracks.fragments.DeleteOneTrackDialogFragment.DeleteOneTrackCaller;
import com.google.android.apps.mytracks.fragments.MarketDialogFragment;
import com.google.android.apps.mytracks.fragments.ReviewDialogFragment;
import com.google.android.apps.mytracks.fragments.WelcomeDialogFragment;
import com.google.android.apps.mytracks.io.backup.BackupActivity;
import com.google.android.apps.mytracks.io.file.SaveActivity;
import com.google.android.apps.mytracks.io.file.TrackWriterFactory.TrackFileFormat;
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.settings.SettingsActivity;
import com.google.android.apps.mytracks.util.AnalyticsUtils;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.apps.mytracks.util.Constants;
import com.google.android.apps.mytracks.util.ContextualActionModeCallback;
import com.google.android.apps.mytracks.util.EulaUtils;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.ListItemUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.apps.mytracks.util.TrackIconUtils;
import com.google.android.apps.mytracks.util.TrackRecordingServiceConnectionUtils;
import com.nogago.android.apps.tracks.content.Track;
import com.nogago.android.apps.tracks.content.TracksColumns;
import com.nogago.android.apps.tracks.content.Waypoint;
import com.nogago.bb10.tracks.R;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.ResourceCursorAdapter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.EnumSet;

/**
 * An activity displaying a list of tracks.
 * 
 * @author Leif Hendrik Wilden
 */
public class TrackListActivity extends FragmentActivity implements DeleteOneTrackCaller {

  private static final String TAG = TrackListActivity.class.getSimpleName();
  private static final String START_GPS_KEY = "start_gps_key";
  private static boolean started = false;

  private static final String[] PROJECTION = new String[] { TracksColumns._ID, TracksColumns.NAME,
      TracksColumns.DESCRIPTION, TracksColumns.CATEGORY, TracksColumns.STARTTIME,
      TracksColumns.TOTALDISTANCE, TracksColumns.TOTALTIME, TracksColumns.ICON };

  // Callback when the trackRecordingServiceConnection binding changes.
  private final Runnable bindChangedCallback = new Runnable() {
    @Override
    public void run() {
      // After binding changes (is available), update the total time in
      // trackController.
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          trackController.update(recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT,
              recordingTrackPaused);
        }
      });

      if (!startNewRecording) {
        return;
      }

      ITrackRecordingService service = trackRecordingServiceConnection.getServiceIfBound();
      if (service == null) {
        Log.d(TAG, "service not available to start a new recording");
        return;
      }
      try {
        recordingTrackId = service.startNewTrack();
        startNewRecording = false;
        Intent intent = IntentUtils.newIntent(TrackListActivity.this, TrackDetailActivity.class)
            .putExtra(TrackDetailActivity.EXTRA_TRACK_ID, recordingTrackId);
        startActivity(intent);
        Toast.makeText(TrackListActivity.this, R.string.track_list_record_success,
            Toast.LENGTH_SHORT).show();
      } catch (Exception e) {
        Toast.makeText(TrackListActivity.this, R.string.track_list_record_error, Toast.LENGTH_LONG)
            .show();
        Log.e(TAG, "Unable to start a new recording.", e);
      }
    }
  };

  /*
   * Note that sharedPreferenceChangeListenr cannot be an anonymous inner class.
   * Anonymous inner class will get garbage collected.
   */
  private final OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
      if (key == null
          || key.equals(PreferencesUtils.getKey(TrackListActivity.this, R.string.metric_units_key))) {
        metricUnits = PreferencesUtils.getBoolean(TrackListActivity.this,
            R.string.metric_units_key, PreferencesUtils.METRIC_UNITS_DEFAULT);
        if (key != null) {
          resourceCursorAdapter.notifyDataSetChanged();
          return;
        }
      }
      if (key == null
          || key.equals(PreferencesUtils.getKey(TrackListActivity.this,
              R.string.recording_track_id_key))) {
        recordingTrackId = PreferencesUtils.getLong(TrackListActivity.this,
            R.string.recording_track_id_key);
        if (key != null) {
          boolean isRecording = recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
          if (isRecording) {
            trackRecordingServiceConnection.startAndBind();
          }
          updateMenuItems(isRecording);
          resourceCursorAdapter.notifyDataSetChanged();
          trackController.update(isRecording, recordingTrackPaused);
          return;
        }
      }
      if (key == null
          || key.equals(PreferencesUtils.getKey(TrackListActivity.this,
              R.string.recording_track_paused_key))) {
        recordingTrackPaused = PreferencesUtils.getBoolean(TrackListActivity.this,
            R.string.recording_track_paused_key, PreferencesUtils.RECORDING_TRACK_PAUSED_DEFAULT);
        if (key != null) {
          boolean success = true;
          try {
            resourceCursorAdapter.notifyDataSetChanged();
          } catch (android.database.StaleDataException e) {
            success = false;
          }
          if (success)
            trackController.update(recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT,
                recordingTrackPaused);
          return;
        }
      }
    }
  };

  // Callback when an item is selected in the contextual action mode
  private final ContextualActionModeCallback contextualActionModeCallback = new ContextualActionModeCallback() {
    @Override
    public boolean onClick(int itemId, int position, long id) {
      return handleContextItem(itemId, id);
    }
  };

  private final OnClickListener recordListener = new OnClickListener() {
    public void onClick(View v) {
      if (recordingTrackId == PreferencesUtils.RECORDING_TRACK_ID_DEFAULT) {
        // Not recording -> Recording
        AnalyticsUtils.sendPageViews(TrackListActivity.this, "/action/record_track");
        startGps = false;
        handleStartGps();
        updateMenuItems(true);
        startRecording();
      } else {
        if (recordingTrackPaused) {
          // Paused -> Resume
          AnalyticsUtils.sendPageViews(TrackListActivity.this, "/action/resume_track");
          updateMenuItems(true);
          TrackRecordingServiceConnectionUtils.resumeTrack(trackRecordingServiceConnection);
          trackController.update(true, false);
        } else {
          // Recording -> Paused
          AnalyticsUtils.sendPageViews(TrackListActivity.this, "/action/pause_track");
          updateMenuItems(true);
          TrackRecordingServiceConnectionUtils.pauseTrack(trackRecordingServiceConnection);
          trackController.update(true, true);
        }
      }
    }
  };

  private final OnClickListener stopListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      AnalyticsUtils.sendPageViews(TrackListActivity.this, "/action/stop_recording");
      updateMenuItems(false);
      TrackRecordingServiceConnectionUtils.stopRecording(TrackListActivity.this,
          trackRecordingServiceConnection, true);
    }
  };

  private final TrackDataListener trackDataListener = new TrackDataListener() {
    @Override
    public void onTrackUpdated(Track track) {
      // Ignore
    }

    @Override
    public void onSelectedTrackChanged(Track track) {
      // Ignore
    }

    @Override
    public void onSegmentSplit(Location location) {
      // Ignore
    }

    @Override
    public void onSampledOutTrackPoint(Location location) {
      // Ignore
    }

    @Override
    public void onSampledInTrackPoint(Location location) {
      // Ignore
    }

    @Override
    public boolean onReportSpeedChanged(boolean reportSpeed) {
      return false;
    }

    @Override
    public void onNewWaypointsDone() {
      // Ignore
    }

    @Override
    public void onNewWaypoint(Waypoint waypoint) {
      // Ignore
    }

    @Override
    public void onNewTrackPointsDone() {
      // Ignore
    }

    @Override
    public boolean onMinRecordingDistanceChanged(int minRecordingDistance) {
      return false;
    }

    @Override
    public boolean onMetricUnitsChanged(boolean isMetricUnits) {
      return false;
    }

    @Override
    public void onLocationStateChanged(LocationState locationState) {
      // Ignore
    }

    @Override
    public void onLocationChanged(Location location) {
      // Ignore
    }

    @Override
    public void onHeadingChanged(double heading) {
      // Ignore
    }

    @Override
    public void clearWaypoints() {
      // Ignore
    }

    @Override
    public void clearTrackPoints() {
      // Ignore
    }
  };

  // The following are set in onCreate
  private TrackRecordingServiceConnection trackRecordingServiceConnection;
  private TrackController trackController;
  private ListView listView;
  private ResourceCursorAdapter resourceCursorAdapter;
  private TrackDataHub trackDataHub;

  // Preferences
  private boolean metricUnits;
  private long recordingTrackId;
  private boolean recordingTrackPaused;

  // Menu items
  private MenuItem searchMenuItem;
  private MenuItem startGpsMenuItem;
  private MenuItem importMenuItem;
  private MenuItem saveAllMenuItem;
  private MenuItem deleteAllMenuItem;

  private boolean startNewRecording = false; // true to start a new recording
  private boolean startGps = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setVolumeControlStream(TextToSpeech.Engine.DEFAULT_STREAM);
    setContentView(R.layout.track_list);

    AnalyticsUtils.sendPageViews(this, this.getLocalClassName() + "/create");

    ApiAdapterFactory.getApiAdapter().hideActionBar(this);

    Display display = getWindowManager().getDefaultDisplay();
    boolean devicesZ = display.getWidth() > 720 || display.getHeight() > 720;
    if (devicesZ) {
      // Disable the Keyboard help link
      View v = findViewById(R.id.help_keyboard_q);
      if (v != null)
        v.setVisibility(View.GONE);
      v = findViewById(R.id.help_keyboard_a);
      if (v != null)
        v.setVisibility(View.GONE);
    }
    trackRecordingServiceConnection = new TrackRecordingServiceConnection(this, bindChangedCallback);

    SharedPreferences sharedPreferences = getSharedPreferences(Constants.SETTINGS_NAME,
        Context.MODE_PRIVATE);
    sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    sharedPreferenceChangeListener.onSharedPreferenceChanged(sharedPreferences, null);

    trackController = new TrackController(this, trackRecordingServiceConnection, true,
        recordListener, stopListener);

    // START MOD
    ImageButton helpButton = (ImageButton) findViewById(R.id.listBtnBarHelp);
    if (helpButton != null)
      helpButton.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          Intent intent = IntentUtils.newIntent(TrackListActivity.this, HelpActivity.class);
          startActivity(intent);
        }
      });
    /*
     * Record = Pause and Stop managed by track controller ImageButton
     * recordButton = (ImageButton) findViewById(R.id.listBtnBarRecord);
     * recordButton.setOnClickListener(recordListener); ImageButton stopButton =
     * (ImageButton) findViewById(R.id.listBtnBarStop);
     * stopButton.setOnClickListener(stopListener);
     */
    ImageButton searchButton = (ImageButton) findViewById(R.id.listBtnBarSearch);
    if (searchButton != null)
      searchButton.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {

          onSearchRequested();
        }
      });
    ImageButton settingsButton = (ImageButton) findViewById(R.id.listBtnBarSettings);
    if (settingsButton != null)
      settingsButton.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          Intent intent = IntentUtils.newIntent(TrackListActivity.this, SettingsActivity.class);
          startActivity(intent);
        }
      });

    // END MOD
    listView = (ListView) findViewById(R.id.track_list);
    listView.setEmptyView(findViewById(R.id.track_list_empty_view));
    listView.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = IntentUtils.newIntent(TrackListActivity.this, TrackDetailActivity.class)
            .putExtra(TrackDetailActivity.EXTRA_TRACK_ID, id);
        startActivity(intent);
      }
    });
    resourceCursorAdapter = new ResourceCursorAdapter(this, R.layout.list_item, null, 0) {
      @Override
      public void bindView(View view, Context context, Cursor cursor) {
        int idIndex = cursor.getColumnIndex(TracksColumns._ID);
        int iconIndex = cursor.getColumnIndex(TracksColumns.ICON);
        int nameIndex = cursor.getColumnIndex(TracksColumns.NAME);
        int categoryIndex = cursor.getColumnIndex(TracksColumns.CATEGORY);
        int totalTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.TOTALTIME);
        int totalDistanceIndex = cursor.getColumnIndexOrThrow(TracksColumns.TOTALDISTANCE);
        int startTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.STARTTIME);
        int descriptionIndex = cursor.getColumnIndex(TracksColumns.DESCRIPTION);

        boolean isRecording = cursor.getLong(idIndex) == recordingTrackId;
        int iconId = TrackIconUtils.getIconDrawable(cursor.getString(iconIndex));
        String name = cursor.getString(nameIndex);
        String totalTime = StringUtils.formatElapsedTime(cursor.getLong(totalTimeIndex));
        String totalDistance = StringUtils.formatDistance(TrackListActivity.this,
            cursor.getDouble(totalDistanceIndex), metricUnits);
        long startTime = cursor.getLong(startTimeIndex);
        String startTimeDisplay = StringUtils.formatDateTime(context, startTime).equals(name) ? null
            : StringUtils.formatRelativeDateTime(context, startTime);

        ListItemUtils.setListItem(TrackListActivity.this, view, isRecording, recordingTrackPaused,
            iconId, R.string.icon_track, name, cursor.getString(categoryIndex), totalTime,
            totalDistance, startTimeDisplay, cursor.getString(descriptionIndex));
      }
    };
    listView.setAdapter(resourceCursorAdapter);
    ApiAdapterFactory.getApiAdapter().configureListViewContextualMenu(this, listView,
        contextualActionModeCallback);

    getSupportLoaderManager().initLoader(0, null, new LoaderCallbacks<Cursor>() {
      @Override
      public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new CursorLoader(TrackListActivity.this, TracksColumns.CONTENT_URI, PROJECTION,
            null, null, TracksColumns._ID + " DESC");
      }

      @Override
      public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        resourceCursorAdapter.swapCursor(cursor);
      }

      @Override
      public void onLoaderReset(Loader<Cursor> loader) {
        resourceCursorAdapter.swapCursor(null);
      }
    });
    trackDataHub = TrackDataHub.newInstance(this);
    if (savedInstanceState != null) {
      startGps = savedInstanceState.getBoolean(START_GPS_KEY);
    } // Test repeated messaging
    if (!started)
      showStartupDialogs();
  }

  @Override
  protected void onStart() {
    super.onStart();
    trackDataHub.start();
    EasyTracker.getInstance(this).activityStart(this); // Add this method.
  }

  @Override
  protected void onPause() {
    super.onPause();
    trackController.stop();
    trackDataHub.unregisterTrackDataListener(trackDataListener);
  }

  @Override
  protected void onResume() {
    super.onResume();
    TrackRecordingServiceConnectionUtils.resumeConnection(this, trackRecordingServiceConnection);
    trackController.update(recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT,
        recordingTrackPaused);
    handleStartGps();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(START_GPS_KEY, startGps);
  }

  @Override
  protected void onStop() {
    super.onStop();
    trackDataHub.stop();
    EasyTracker.getInstance(this).activityStop(this); // Add this method.
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    trackRecordingServiceConnection.unbind();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.track_list, menu);
    String fileTypes[] = getResources().getStringArray(R.array.file_types);
    menu.findItem(R.id.track_list_save_all_gpx).setTitle(
        getString(R.string.menu_save_format, fileTypes[0]));
    menu.findItem(R.id.track_list_save_all_kml).setTitle(
        getString(R.string.menu_save_format, fileTypes[1]));
    menu.findItem(R.id.track_list_save_all_csv).setTitle(
        getString(R.string.menu_save_format, fileTypes[2]));
    // menu.findItem(R.id.track_list_save_all_tcx)
    // .setTitle(getString(R.string.menu_save_format, fileTypes[3]));

    // searchMenuItem = menu.findItem(R.id.track_list_search);
    startGpsMenuItem = menu.findItem(R.id.track_list_start_gps);
    importMenuItem = menu.findItem(R.id.track_list_import);
    saveAllMenuItem = menu.findItem(R.id.track_list_save_all);
    deleteAllMenuItem = menu.findItem(R.id.track_list_delete_all);

    ApiAdapterFactory.getApiAdapter().configureSearchWidget(this, searchMenuItem);
    updateMenuItems(recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent intent;
    switch (item.getItemId()) {
    /*
     * case R.id.track_list_search: return
     * ApiAdapterFactory.getApiAdapter().handleSearchMenuSelection(this);
     */
      case R.id.track_list_start_gps:
        startGps = !startGps;
        handleStartGps();
        updateMenuItems(recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT);
        return true;
      case R.id.track_list_import:
        AnalyticsUtils.sendPageViews(this, "/action/import");
        String msg = String.format(getResources().getString(R.string.dlg_import), Constants.IS_BLACKBERRY ? FileUtils
            .buildExternalDirectoryPath("gpx").toString().replace("/mnt/sdcard", "/misc/android") :  FileUtils
            .buildExternalDirectoryPath("gpx").toString()  );
        Builder builder = new AlertDialog.Builder(TrackListActivity.this);
        builder.setMessage(msg).setNeutralButton(getString(android.R.string.cancel), null);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            Intent myintent = IntentUtils.newIntent(TrackListActivity.this, ImportActivity.class)
                .putExtra(ImportActivity.EXTRA_IMPORT_ALL, true);
            startActivity(myintent);
          }
        });
        builder.show();
        return true;
      case R.id.track_list_save_all_gpx:
        startSaveActivity(TrackFileFormat.GPX);
        return true;
      case R.id.track_list_save_all_kml:
        startSaveActivity(TrackFileFormat.KML);
        return true;
      case R.id.track_list_save_all_csv:
        startSaveActivity(TrackFileFormat.CSV);
        return true;
        // case R.id.track_list_save_all_tcx:
        // startSaveActivity(TrackFileFormat.TCX);
        // return true;
      case R.id.track_list_delete_all:
        new DeleteAllTrackDialogFragment().show(getSupportFragmentManager(),
            DeleteAllTrackDialogFragment.DELETE_ALL_TRACK_DIALOG_TAG);
        return true;
      case R.id.track_list_aggregated_statistics:
        intent = IntentUtils.newIntent(this, AggregatedStatsActivity.class);
        startActivity(intent);
        return true;
      case R.id.track_list_settings:
        intent = IntentUtils.newIntent(this, SettingsActivity.class);
        startActivity(intent);
        return true;
        /*
         * case R.id.track_list_help: intent = IntentUtils.newIntent(this,
         * HelpActivity.class); startActivity(intent); return true;
         */
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    getMenuInflater().inflate(R.menu.list_context_menu, menu);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    if (handleContextItem(item.getItemId(), ((AdapterContextMenuInfo) item.getMenuInfo()).id)) {
      return true;
    }
    return super.onContextItemSelected(item);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    // T,B, Space are already mapped by BB on Q10!
    Intent intent;
    boolean isRecording = recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
    switch (keyCode) {
      case KeyEvent.KEYCODE_SEARCH:
        if (ApiAdapterFactory.getApiAdapter().handleSearchKey(searchMenuItem)) {
          return true;
        }
        break;
      case KeyEvent.KEYCODE_H:
        // Help
        intent = IntentUtils.newIntent(TrackListActivity.this, HelpActivity.class);
        startActivity(intent);
        break;
      case KeyEvent.KEYCODE_N:
        if (!isRecording) {
          // Not recording -> Recording
          AnalyticsUtils.sendPageViews(TrackListActivity.this, "/action/record_track");
          startGps = false;
          handleStartGps();
          updateMenuItems(true);
          startRecording();
        }
        break;
      case KeyEvent.KEYCODE_P:
        if (isRecording) {
          if (recordingTrackPaused) {
            // Paused -> Resume
            AnalyticsUtils.sendPageViews(TrackListActivity.this, "/action/resume_track");
            updateMenuItems(true);
            TrackRecordingServiceConnectionUtils.resumeTrack(trackRecordingServiceConnection);
            trackController.update(true, false);
          } else {
            // Recording -> Paused
            AnalyticsUtils.sendPageViews(TrackListActivity.this, "/action/pause_track");
            updateMenuItems(true);
            TrackRecordingServiceConnectionUtils.pauseTrack(trackRecordingServiceConnection);
            trackController.update(true, true);
          }
        } else {
          Toast.makeText(getApplicationContext(), "Cannot pause track that is not recorded.",
              Toast.LENGTH_LONG).show();
        }
        break;
      case KeyEvent.KEYCODE_S:
        if (isRecording) {

          // Stop Recording
          updateMenuItems(false);
          TrackRecordingServiceConnectionUtils.stopRecording(TrackListActivity.this,
              trackRecordingServiceConnection, true);
        } else {
          // Search
          ApiAdapterFactory.getApiAdapter().handleSearchMenuSelection(this);
        }
        break;
      case KeyEvent.KEYCODE_A:
        intent = IntentUtils.newIntent(this, SettingsActivity.class);
        startActivity(intent);
        break;

    }

    return super.onKeyUp(keyCode, event);
  }

  @Override
  public TrackRecordingServiceConnection getTrackRecordingServiceConnection() {
    return trackRecordingServiceConnection;
  }

  /**
   * Shows start up dialogs.
   */
  public void showStartupDialogs() {
    setupDirectories();
    /*
     * if (!EulaUtils.getAcceptEula(this)) { } else
     */if (EulaUtils.getShowWelcome(this)) {
      Fragment fragment = getSupportFragmentManager().findFragmentByTag(
          WelcomeDialogFragment.WELCOME_DIALOG_TAG);
      if (fragment == null) {
        new WelcomeDialogFragment().show(getSupportFragmentManager(),
            WelcomeDialogFragment.WELCOME_DIALOG_TAG);
      }
    } else if (EulaUtils.getShowCheckUnits(this)) {
      Fragment fragment = getSupportFragmentManager().findFragmentByTag(
          CheckUnitsDialogFragment.CHECK_UNITS_DIALOG_TAG);
      if (fragment == null) {
        new CheckUnitsDialogFragment().show(getSupportFragmentManager(),
            CheckUnitsDialogFragment.CHECK_UNITS_DIALOG_TAG);
      }
    } else if (!Constants.IS_BLACKBERRY && EulaUtils.getShowReview(this) && EulaUtils.getAppStart(this) > 7) {
      // Ask For Review at 7th start, continue bugging the user
      Fragment fragment = getSupportFragmentManager().findFragmentByTag(
          ReviewDialogFragment.REVIEW_DIALOG_TAG);
      if (fragment == null) {
        ReviewDialogFragment.newInstance(false).show(getSupportFragmentManager(),
            ReviewDialogFragment.REVIEW_DIALOG_TAG);
      }
    } else if (!Constants.IS_BLACKBERRY && (EulaUtils.getAppStart(this) % 10) == 9) {
      // Show our other apps every tenth start with starting with the ninth
      // start
      Fragment fragment = getSupportFragmentManager().findFragmentByTag(
          MarketDialogFragment.MARKET_DIALOG_TAG);
      if (fragment == null) {
        MarketDialogFragment.newInstance(false).show(getSupportFragmentManager(),
            MarketDialogFragment.MARKET_DIALOG_TAG);
      }
    } else if ((EulaUtils.getAppStart(this) % 10) == 0) {
      // Perform a backup at every tenth app start.
      Intent intent = IntentUtils.newIntent(this, BackupActivity.class);
      startActivity(intent);
    } else {
      /*
       * Before the welcome sequence, the empty view is not visible so that it
       * doesn't show through.
       */
      findViewById(R.id.track_list_empty_view).setVisibility(View.VISIBLE);
    }
    checkPriorExceptions(false);
    started = true;
  }

  /*
   * Checks whether previous runs of the app had an exception.
   */

  private void checkPriorExceptions(boolean firstTime) {
    final File file = new File(FileUtils.buildExternalDirectoryPath("error.log"));
    if (file != null && file.exists() && file.length() > 0) {
      String msg = getString(R.string.previous_run_crashed);
      Builder builder = new AlertDialog.Builder(TrackListActivity.this);
      // User says no
      builder.setMessage(msg).setNeutralButton(getString(R.string.donot_send_report),
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              // Delete Exceptions File when user presses Ignore
              if (!file.delete())
                Toast.makeText(getApplicationContext(), "Exceptions file not deleted",
                    Toast.LENGTH_LONG).show();
            }
          });
      // User says yes
      builder.setPositiveButton(R.string.send_report, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          Intent intent = new Intent(Intent.ACTION_SEND);
          intent.putExtra(Intent.EXTRA_EMAIL, new String[] { Constants.BUGS_MAIL }); //$NON-NLS-1$
          intent.setType("vnd.android.cursor.dir/email"); //$NON-NLS-1$
          intent.putExtra(Intent.EXTRA_SUBJECT, "nogago Tracks bug"); //$NON-NLS-1$
          StringBuilder text = new StringBuilder();
          text.append("\nDevice : ").append(Build.DEVICE); //$NON-NLS-1$
          text.append("\nBrand : ").append(Build.BRAND); //$NON-NLS-1$
          text.append("\nModel : ").append(Build.MODEL); //$NON-NLS-1$
          text.append("\nProduct : ").append("Tracks"); //$NON-NLS-1$
          text.append("\nBuild : ").append(Build.DISPLAY); //$NON-NLS-1$
          text.append("\nVersion : ").append(Build.VERSION.RELEASE); //$NON-NLS-1$
          text.append("\nApp Starts : ").append(EulaUtils.getAppStart(TrackListActivity.this)); //$NON-NLS-1$

          try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (info != null) {
              text.append("\nApk Version : ").append(info.versionName).append(" ").append(info.versionCode); //$NON-NLS-1$ //$NON-NLS-2$
            }
          } catch (NameNotFoundException e) {}

          try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while (br.read() != -1) {
              if ((line = br.readLine()) != null) {
                text.append(line);
              }
            }
            br.close();
            fr.close();
          } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Error reading exceptions file!",
                Toast.LENGTH_LONG).show();
          }
          intent.putExtra(Intent.EXTRA_TEXT, text.toString());
          startActivity(Intent.createChooser(intent, getString(R.string.send_report)));

          if (!file.delete())
            Toast.makeText(getApplicationContext(), "Exceptions file not deleted",
                Toast.LENGTH_LONG).show();
        }

      });
      builder.show();
    }
  }

  /**
   * Creates the directories to exchange track files
   */
  private void setupDirectories() {
    if (FileUtils.isSdCardAvailable()) {
      // create a File object for the parent directory
      File directory = new File(FileUtils.buildExternalDirectoryPath("gpx"));
      // have the object build the directory structure, if needed.
      directory.mkdirs();
      directory = new File(FileUtils.buildExternalDirectoryPath("backups"));
      // have the object build the directory structure, if needed.
      directory.mkdirs();
      directory = new File(FileUtils.buildExternalDirectoryPath("kml"));
      // have the object build the directory structure, if needed.
      directory.mkdirs();
      directory = new File(FileUtils.buildExternalDirectoryPath("csv"));
      // have the object build the directory structure, if needed.
      directory.mkdirs();
    }

  }

  /**
   * Updates the menu items.
   * 
   * @param isRecording true if recording
   */
  private void updateMenuItems(boolean isRecording) {
    if (startGpsMenuItem != null) {
      startGpsMenuItem.setIcon(startGps ? R.drawable.menu_stop_gps : R.drawable.menu_start_gps);
      startGpsMenuItem.setTitle(startGps ? R.string.menu_stop_gps : R.string.menu_start_gps);
    }
    if (importMenuItem != null) {
      importMenuItem.setVisible(!isRecording);
    }
    if (saveAllMenuItem != null) {
      saveAllMenuItem.setVisible(!isRecording);
    }
    if (deleteAllMenuItem != null) {
      deleteAllMenuItem.setVisible(!isRecording);
    }
  }

  /**
   * Starts a new recording.
   */
  private void startRecording() {
    startNewRecording = true;
    trackRecordingServiceConnection.startAndBind();

    /*
     * If the binding has happened, then invoke the callback to start a new
     * recording. If the binding hasn't happened, then invoking the callback
     * will have no effect. But when the binding occurs, the callback will get
     * invoked.
     */
    bindChangedCallback.run();
  }

  /**
   * Starts the {@link SaveActivity} to save all tracks.
   * 
   * @param trackFileFormat the track file format
   */
  private void startSaveActivity(TrackFileFormat trackFileFormat) {
    AnalyticsUtils.sendPageViews(this, "/action/save_all");
    Intent intent = IntentUtils.newIntent(this, SaveActivity.class).putExtra(
        SaveActivity.EXTRA_TRACK_FILE_FORMAT, (Parcelable) trackFileFormat);
    startActivity(intent);
  }

  /**
   * Handles a context item selection.
   * 
   * @param itemId the menu item id
   * @param trackId the track id
   * @return true if handled.
   */
  private boolean handleContextItem(int itemId, long trackId) {
    Intent intent;
    switch (itemId) {
      case R.id.list_context_menu_show_on_map:
        intent = IntentUtils.newIntent(this, TrackDetailActivity.class).putExtra(
            TrackDetailActivity.EXTRA_TRACK_ID, trackId);
        startActivity(intent);
        return true;
      case R.id.list_context_menu_edit:
        intent = IntentUtils.newIntent(this, TrackEditActivity.class).putExtra(
            TrackEditActivity.EXTRA_TRACK_ID, trackId);
        startActivity(intent);
        return true;
      case R.id.list_context_menu_delete:
        DeleteOneTrackDialogFragment.newInstance(trackId).show(getSupportFragmentManager(),
            DeleteOneTrackDialogFragment.DELETE_ONE_TRACK_DIALOG_TAG);
        return true;
      default:
        return false;
    }
  }

  /**
   * Handles starting gps.
   */
  private void handleStartGps() {
    if (startGps) {
      trackDataHub.registerTrackDataListener(trackDataListener, EnumSet.of(TrackDataType.LOCATION));
    } else {
      trackDataHub.unregisterTrackDataListener(trackDataListener);
    }
  }

}
