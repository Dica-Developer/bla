/*
 * Copyright 2011 Google Inc.
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

package com.google.android.apps.mytracks.content;

import static com.nogago.android.tracks.Constants.TAG;

import android.util.Log;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages register/unregister {@link TrackDataListener} and keeping the state
 * for each registered listener.
 * 
 * @author Rodrigo Damazio
 */
public class TrackDataManager {

  // Map of listener to its state
  private final Map<TrackDataListener, ListenerState>
      listenerToStateMap = new HashMap<TrackDataListener, ListenerState>();

  // Map of track data type to listeners
  private final Map<TrackDataType, Set<TrackDataListener>>
      typeToListenersMap = new EnumMap<TrackDataType, Set<TrackDataListener>>(TrackDataType.class);

  public TrackDataManager() {
    for (TrackDataType trackDataType : TrackDataType.values()) {
      typeToListenersMap.put(trackDataType, new LinkedHashSet<TrackDataListener>());
    }
  }

  /**
   * Registers a listener.
   * 
   * @param listener the listener
   * @param trackDataTypes the track data types the listener is interested
   */
  public ListenerState registerListener(
      TrackDataListener listener, EnumSet<TrackDataType> trackDataTypes) {
    if (listenerToStateMap.containsKey(listener)) {
      throw new IllegalStateException("Listener is already registered");
    }

    ListenerState listenerState = new ListenerState(listener, trackDataTypes);
    listenerToStateMap.put(listener, listenerState);

    for (TrackDataType trackDataType : trackDataTypes) {
      typeToListenersMap.get(trackDataType).add(listener);
    }

    return listenerState;
  }

  /**
   * Unregisters a listener.
   * 
   * @param listener the listener
   */
  public void unregisterListener(TrackDataListener listener) {
    ListenerState removed = listenerToStateMap.remove(listener);
    if (removed == null) {
      Log.w(TAG, "Tried to unregister a listener that is not registered.");
      return;
    }

    // Remove the listener from the typeToListenersMap
    for (TrackDataType trackDataType : removed.getTrackDataTypes()) {
      typeToListenersMap.get(trackDataType).remove(listener);
    }
  }

  /**
   * Gets the number of {@link TrackDataListener}.
   */
  public int getNumberOfListeners() {
    return listenerToStateMap.size();
  }

  /**
   * Gets the track listener state.
   * 
   * @param listener the listener
   */
  public ListenerState getListenerState(TrackDataListener listener) {
    return listenerToStateMap.get(listener);
  }

  /**
   * Gets the listeners for a {@link TrackDataType}.
   * 
   * @param type the type
   */
  public Set<TrackDataListener> getListeners(TrackDataType type) {
    return typeToListenersMap.get(type);
  }

  /**
   * Gets all the registered {@link TrackDataType}.
   */
  public EnumSet<TrackDataType> getRegisteredTrackDataTypes() {
    EnumSet<TrackDataType> types = EnumSet.noneOf(TrackDataType.class);
    for (ListenerState registration : this.listenerToStateMap.values()) {
      types.addAll(registration.getTrackDataTypes());
    }
    return types;
  }
}
