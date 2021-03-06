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

package com.google.android.apps.mytracks.fragments;

import com.google.android.apps.mytracks.MapOverlay;
import com.google.android.apps.mytracks.TrackDetailActivity;
import com.google.android.apps.mytracks.content.TrackDataHub;
import com.google.android.apps.mytracks.content.TrackDataListener;
import com.google.android.apps.mytracks.content.TrackDataType;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.apps.mytracks.util.GeoRect;
import com.google.android.apps.mytracks.util.GoogleLocationUtils;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.nogago.android.apps.tracks.content.AnnotatedXYTileSource;
import com.nogago.android.apps.tracks.content.MyTracksProviderUtils;
import com.nogago.android.apps.tracks.content.MyTracksProviderUtils.Factory;
import com.nogago.android.apps.tracks.content.TileSourceFactory;
import com.nogago.android.apps.tracks.content.Track;
import com.nogago.android.apps.tracks.content.Waypoint;
import com.nogago.bb10.tracks.R;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.EnumSet;
import java.util.List;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;

/**
 * A fragment to display map to the user.
 * 
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public class MapFragment extends Fragment implements View.OnTouchListener, View.OnClickListener,
    TrackDataListener {

  public static final String MAP_FRAGMENT_TAG = "mapFragment";

  private static final String CURRENT_LOCATION_KEY = "current_location_key";
  private static final String KEEP_MY_LOCATION_VISIBLE_KEY = "keep_my_location_visible_key";

  private TrackDataHub trackDataHub;

  // True to keep my location visible.
  private boolean keepMyLocationVisible;

  // True show satelliteMap instead
  private boolean satelliteMap = false;

  // True to zoom to my location. Only apply when keepMyLocationVisible is true.
  private boolean zoomToMyLocation;

  // The track id of the marker to show.
  private long markerTrackId;

  // The marker id to show
  private long markerId;

  // The current selected track id. Set in onSelectedTrackChanged.
  private long currentSelectedTrackId;

  // The current location. Set in onCurrentLocationChanged.
  private Location currentLocation;

  // UI elements
  private View mapViewContainer;
  private MapView mapView;
  private MapOverlay mapOverlay;
  private ImageButton myLocationImageButton;
  private ImageButton myMapModeImageButton;
  private TextView messageTextView;
  private TextView attributionTextView;

  protected ResourceProxy mResourceProxy;

  private CompassOverlay mCompassOverlay;

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    // mResourceProxy = new
    // ResourceProxyImpl(inflater.getContext().getApplicationContext());
    mapView = new MapView(inflater.getContext(), 256, new DefaultResourceProxyImpl(inflater
        .getContext().getApplicationContext()));
    mapView.setUseSafeCanvas(true);

    mapViewContainer = ((TrackDetailActivity) getActivity()).getMapViewContainer();
    FrameLayout fl = (FrameLayout) mapViewContainer.findViewById(R.id.map_container);
    fl.addView(mapView);
    // MOD
    mapOverlay = new MapOverlay(getActivity());

    List<Overlay> overlays = mapView.getOverlays();
    overlays.clear();
    overlays.add(mapOverlay);

    mapView.requestFocus();
    mapView.setOnTouchListener(this);
    mapView.getController().setZoom(15);
    mapView.getController().setCenter(new GeoPoint(48.7725, 8.385556));
    mapView.setBuiltInZoomControls(true);
    myLocationImageButton = (ImageButton) mapViewContainer.findViewById(R.id.map_my_location);
    myLocationImageButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        showMyLocation();
      }
    });

    myMapModeImageButton = (ImageButton) mapViewContainer.findViewById(R.id.map_mode);
    myMapModeImageButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        setSatellite(!isSatellite());
      }
    });
    //
    messageTextView = (TextView) mapViewContainer.findViewById(R.id.map_message);
    attributionTextView = (TextView) mapViewContainer.findViewById(R.id.map_copyright);
    // map_copyright
    ApiAdapterFactory.getApiAdapter().invalidMenu(getActivity());
    setSatellite(isSatellite());
    return mapViewContainer;
    // return mapView;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    if (savedInstanceState != null) {
      keepMyLocationVisible = savedInstanceState.getBoolean(KEEP_MY_LOCATION_VISIBLE_KEY, false);
      currentLocation = (Location) savedInstanceState.getParcelable(CURRENT_LOCATION_KEY);
      if (currentLocation != null) {
        updateCurrentLocation();
      }
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    resumeTrackDataHub();
    if (currentLocation != null) {
      updateCurrentLocation();
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(KEEP_MY_LOCATION_VISIBLE_KEY, keepMyLocationVisible);
    if (currentLocation != null) {
      outState.putParcelable(CURRENT_LOCATION_KEY, currentLocation);
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    pauseTrackDataHub();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    ViewGroup parentViewGroup = (ViewGroup) mapViewContainer.getParent();
    if (parentViewGroup != null) {
      parentViewGroup.removeView(mapViewContainer);
    }
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflator) {
    // menuInflator.inflate(R.menu.map, menu);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    /*
     * int titleId = R.string.menu_satellite_mode; if (mapView != null) {
     * titleId = isSatellite() ? R.string.menu_map_mode :
     * R.string.menu_satellite_mode; }
     * menu.findItem(R.id.map_satellite_mode).setTitle(titleId);
     */
    super.onPrepareOptionsMenu(menu);
  }

  private boolean isSatellite() {
    // TODO Auto-generated method stub
    return satelliteMap;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem menuItem) {
    if (mapView != null && menuItem.getItemId() == R.id.map_satellite_mode) {
      setSatellite(!isSatellite());

      return true;
    }
    return super.onOptionsItemSelected(menuItem);
  }

  // TODO

  private void setSatellite(boolean b) {
    String v = PreferencesUtils.getString(getActivity(),
        R.string.settings_tile_source_key, "DRIVING");
    AnnotatedXYTileSource t = (AnnotatedXYTileSource) (b ? TileSourceFactory.SATELLITE
        : TileSourceFactory.getTileSource(v));
    mapView.setTileSource(t);
    if(attributionTextView !=null && (t.getAnnotation() != null) ) attributionTextView.setText(t.getAnnotation());
    satelliteMap = b;

  }

  /**
   * Shows my location.
   */
  public void showMyLocation() {
    updateTrackDataHub();
    // keepMyLocationVisible = true;
    zoomToMyLocation = true;
    if (currentLocation != null) {
      updateCurrentLocation();
    }
  }

  /**
   * Shows the marker.
   * 
   * @param id the marker id
   */
  private void showMarker(long id) {
    MyTracksProviderUtils MyTracksProviderUtils = Factory.get(getActivity());
    Waypoint waypoint = MyTracksProviderUtils.getWaypoint(id);
    if (waypoint != null && waypoint.getLocation() != null) {
      keepMyLocationVisible = false;
      GeoPoint center = new GeoPoint((int) (waypoint.getLocation().getLatitude() * 1E6),
          (int) (waypoint.getLocation().getLongitude() * 1E6));
      mapView.getController().setCenter(center);
      mapView.getController().setZoom(mapView.getMaxZoomLevel());
      mapView.invalidate();
    }
  }

  /**
   * Shows the marker.
   * 
   * @param trackId the track id
   * @param id the marker id
   */
  public void showMarker(long trackId, long id) {
    /*
     * Synchronize to prevent race condition in changing markerTrackId and
     * markerId variables.
     */
    synchronized (this) {
      if (trackId == currentSelectedTrackId) {
        showMarker(id);
        markerTrackId = -1L;
        markerId = -1L;
        return;
      }
      markerTrackId = trackId;
      markerId = id;
    }
  }

  @Override
  public boolean onTouch(View view, MotionEvent event) {
    // keepMyLocationVisible = false;
    if (keepMyLocationVisible && event.getAction() == MotionEvent.ACTION_MOVE) {
      // if (!isVisible(currentLocation)) {
      /*
       * Only set to false when no longer visible. Thus can keep showing the
       * current location with the next location update.
       */
      keepMyLocationVisible = false;
      // }
    }
    return false;
  }

  @Override
  public void onClick(View v) {
    if (v == messageTextView) {
      Intent intent = GoogleLocationUtils.isAvailable(getActivity()) ? new Intent(
          GoogleLocationUtils.ACTION_GOOGLE_LOCATION_SETTINGS) : new Intent(
          Settings.ACTION_LOCATION_SOURCE_SETTINGS);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(intent);
    }
  }

  @Override
  public void onLocationStateChanged(LocationState state) {
    if (isResumed()) {
      final String message;
      final boolean isGpsDisabled;
      if (!isSelectedTrackRecording()) {
        message = null;
        isGpsDisabled = false;
      } else {
        switch (state) {
          case DISABLED:
            String setting = getString(GoogleLocationUtils.isAvailable(getActivity()) ? R.string.gps_google_location_settings
                : R.string.gps_location_access);
            message = getString(R.string.gps_disabled, setting);
            isGpsDisabled = true;
            currentLocation = null;
            updateCurrentLocation();
            break;
          case NO_FIX:
            message = getString(R.string.gps_wait_for_signal);
            isGpsDisabled = false;
            break;
          case BAD_FIX:
            message = getString(R.string.gps_wait_for_better_signal);
            isGpsDisabled = false;
            break;
          case GOOD_FIX:
            message = null;
            isGpsDisabled = false;
            break;
          default:
            throw new IllegalArgumentException("Unexpected state: " + state);
        }
      }
      getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          if (isResumed()) {
            if (message == null) {
              messageTextView.setVisibility(View.GONE);
              return;
            }
            messageTextView.setText(message);
            messageTextView.setVisibility(View.VISIBLE);
            if (isGpsDisabled) {
              Toast.makeText(getActivity(), R.string.gps_not_found, Toast.LENGTH_LONG).show();

              // Click to show the location source settings
              messageTextView.setOnClickListener(MapFragment.this);
            } else {
              messageTextView.setOnClickListener(null);
            }
          }
        }
      });
    }
  }

  @Override
  public void onLocationChanged(Location location) {
    if (isResumed()) {
      currentLocation = location;
      updateCurrentLocation();
    }
  }

  @Override
  public void onHeadingChanged(double heading) {
    if (isResumed()) {
      if (mapOverlay.setHeading((float) heading)) {
        mapView.postInvalidate();
      }
    }
  }

  @Override
  public void onSelectedTrackChanged(final Track track) {
    if (isResumed()) {
      getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          if (isResumed()) {
            boolean hasTrack = track != null;
            mapOverlay.setTrackDrawingEnabled(hasTrack);

            if (hasTrack) {
              synchronized (this) {
                /*
                 * Synchronize to prevent race condition in changing
                 * markerTrackId and markerId variables.
                 */
                currentSelectedTrackId = track.getId();
                updateMap(track);
              }
              mapOverlay.setShowEndMarker(!isSelectedTrackRecording());
            }
            mapView.invalidate();
          }
        }
      });
    }
  }

  @Override
  public void onTrackUpdated(Track track) {
    // We don't care.
  }

  @Override
  public void clearTrackPoints() {
    if (isResumed()) {
      mapOverlay.clearPoints();
    }
  }

  @Override
  public void onSampledInTrackPoint(Location location) {
    if (isResumed()) {
      mapOverlay.addLocation(location);
    }
  }

  @Override
  public void onSampledOutTrackPoint(Location location) {
    // We don't care.
  }

  @Override
  public void onSegmentSplit(Location location) {
    if (isResumed()) {
      mapOverlay.addSegmentSplit();
    }
  }

  @Override
  public void onNewTrackPointsDone() {
    if (isResumed()) {
      mapView.postInvalidate();
    }
  }

  @Override
  public void clearWaypoints() {
    if (isResumed()) {
      mapOverlay.clearWaypoints();
    }
  }

  @Override
  public void onNewWaypoint(Waypoint waypoint) {
    if (isResumed() && waypoint != null && LocationUtils.isValidLocation(waypoint.getLocation())) {
      // TODO: Optimize locking inside addWaypoint
      mapOverlay.addWaypoint(waypoint);
    }
  }

  @Override
  public void onNewWaypointsDone() {
    if (isResumed()) {
      mapView.postInvalidate();
    }
  }

  @Override
  public boolean onMetricUnitsChanged(boolean metric) {
    // We don't care.
    return false;
  }

  @Override
  public boolean onReportSpeedChanged(boolean reportSpeed) {
    // We don't care.
    return false;
  }

  @Override
  public boolean onMinRecordingDistanceChanged(int minRecordingDistance) {
    // We don't care.
    return false;
  }

  /**
   * Resumes the trackDataHub. Needs to be synchronized because trackDataHub can
   * be accessed by multiple threads.
   */
  private synchronized void resumeTrackDataHub() {
    trackDataHub = ((TrackDetailActivity) getActivity()).getTrackDataHub();
    trackDataHub.registerTrackDataListener(this, EnumSet.of(TrackDataType.SELECTED_TRACK,
        TrackDataType.WAYPOINTS_TABLE, TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE,
        TrackDataType.LOCATION, TrackDataType.HEADING));
  }

  /**
   * Pauses the trackDataHub. Needs to be synchronized because trackDataHub can
   * be accessed by multiple threads.
   */
  private synchronized void pauseTrackDataHub() {
    trackDataHub.unregisterTrackDataListener(this);
    trackDataHub = null;
  }

  /**
   * Updates the trackDataHub. Needs to be synchronized because trackDataHub can
   * be accessed by multiple threads.
   */
  private synchronized void updateTrackDataHub() {
    if (trackDataHub != null) {
      trackDataHub.forceUpdateLocation();
    }
  }

  /**
   * Returns true if the selected track is recording. Needs to be synchronized
   * because trackDataHub can be accessed by multiple threads.
   */
  private synchronized boolean isSelectedTrackRecording() {
    return trackDataHub != null && trackDataHub.isSelectedTrackRecording();
  }

  /**
   * Updates the map by either zooming to the requested marker or showing the
   * track.
   * 
   * @param track the track
   */
  private void updateMap(Track track) {
    if (track.getId() == markerTrackId) {
      // Show the marker
      showMarker(markerId);

      markerTrackId = -1L;
      markerId = -1L;
    } else {
      // Show the track
      showTrack(track);
    }
  }

  /**
   * Returns true if the location is visible.
   * 
   * @param location the location
   */
  private boolean isVisible(Location location) {
    if (location == null || mapView == null) {
      return false;
    }
    IGeoPoint mapCenter = mapView.getMapCenter();
    int latitudeSpan = mapView.getLatitudeSpan();
    int longitudeSpan = mapView.getLongitudeSpan();

    /*
     * The bottom of the mapView is obscured by the zoom controls, subtract its
     * height from the visible area.
     */
    IGeoPoint zoomControlBottom = mapView.getProjection().fromPixels(0, mapView.getHeight());
    IGeoPoint zoomControlTop = mapView.getProjection().fromPixels(0, mapView.getHeight());
    int zoomControlMargin = Math.abs(zoomControlTop.getLatitudeE6()
        - zoomControlBottom.getLatitudeE6());
    GeoRect geoRect = new GeoRect(mapCenter, latitudeSpan, longitudeSpan);
    geoRect.top += zoomControlMargin;

    GeoPoint geoPoint = LocationUtils.getGeoPoint(location);
    return geoRect.contains(geoPoint);
  }

  /**
   * Updates the current location and centers it if necessary.
   */
  private void updateCurrentLocation() {
    if (mapOverlay == null || mapView == null) {
      return;
    }

    mapOverlay.setMyLocation(currentLocation);
    mapView.postInvalidate();

    if (currentLocation != null && keepMyLocationVisible && !isVisible(currentLocation)) {
      GeoPoint geoPoint = LocationUtils.getGeoPoint(currentLocation);
      MapController mapController = mapView.getController();
      mapController.animateTo(geoPoint);
      mapController.setCenter(geoPoint);
      if (zoomToMyLocation) {
        // Only zoom in the first time we show the location.
        zoomToMyLocation = false;
        if (mapView.getZoomLevel() < mapView.getMaxZoomLevel()) {
          mapController.setZoom(mapView.getMaxZoomLevel());
        }
      }
    }
  }

  /**
   * Shows the track.
   * 
   * @param track the track
   */
  private void showTrack(Track track) {
    if (mapView == null || track == null || track.getNumberOfPoints() < 2) {
      return;
    }

    TripStatistics tripStatistics = track.getTripStatistics();
    int bottom = tripStatistics.getBottom();
    int left = tripStatistics.getLeft();
    int latitudeSpanE6 = tripStatistics.getTop() - bottom;
    int longitudeSpanE6 = tripStatistics.getRight() - left;
    if (latitudeSpanE6 > 0 && latitudeSpanE6 < 180E6 && longitudeSpanE6 > 0
        && longitudeSpanE6 < 360E6) {
      keepMyLocationVisible = false;
      GeoPoint center = new GeoPoint(bottom + latitudeSpanE6 / 2, left + longitudeSpanE6 / 2);
      if (LocationUtils.isValidGeoPoint(center)) {
        mapView.getController().setCenter(center);
        mapView.getController().zoomToSpan(latitudeSpanE6, longitudeSpanE6);
      }
    }
  }

  public void zoomIn() {
    mapView.getController().zoomIn();
  }

  public void zoomOut() {
    mapView.getController().zoomOut();
  }

}
