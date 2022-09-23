/**
 * Copyright 2017 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cmrlabs.tracker.FetchLocation;


import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

import com.cmrlabs.tracker.R;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DateFormat;
import java.util.Date;

class Utils {
    private Context context;

    static final String KEY_REQUESTING_LOCATION_UPDATES = "requesting_locaction_updates";

    /**
     * Returns true if requesting location updates, otherwise returns false.
     *
     * @param context The {@link Context}.
     */
    static boolean requestingLocationUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_LOCATION_UPDATES, false);
    }

    /**
     * Stores the location updates state in SharedPreferences.
     *
     * @param requestingLocationUpdates The location updates state.
     */
    static void setRequestingLocationUpdates(Context context, boolean requestingLocationUpdates) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_LOCATION_UPDATES, requestingLocationUpdates)
                .apply();
    }

    /**
     * Returns the {@code location} object as a human readable string.
     *
     * @param location The {@link Location}.
     */
    static String getLocationText(Location location) {
        Context context = MyApplication.getAppContext();
        String busId;
        SharedPreferences pref = context.getSharedPreferences("A", Context.MODE_PRIVATE);
        busId = pref.getString("busID", "0");
        FirebaseDatabase mFirebaseInstance = FirebaseDatabase.getInstance();
        //mFirebaseInstance.getReference("bus_tracking").child("service_no").child(busId).child("origin_lat").setValue("12.972442");
        //mFirebaseInstance.getReference("bus_tracking").child("service_no").child(busId).child("origin_lon").setValue("77.580643");
        mFirebaseInstance.getReference("bus_tracking").child("service_no").child(busId).child("live_lat").setValue(location.getLatitude());
        mFirebaseInstance.getReference("bus_tracking").child("service_no").child(busId).child("live_lon").setValue(location.getLongitude());
        //mFirebaseInstance.getReference("bus_tracking").child("service_no").child(busId).child("destination_lat").setValue("14.442599");
        //mFirebaseInstance.getReference("bus_tracking").child("service_no").child(busId).child("destination_lon").setValue("79.986458");

        return location == null ? "Unknown location" :
                "(" + location.getLatitude() + ", " + location.getLongitude() + ")";
    }

    static String getLocationTitle(Context context) {
        return context.getString(R.string.location_updated,
                DateFormat.getDateTimeInstance().format(new Date()));
    }

    public Utils(Context context) {
        this.context = context;
    }

}
