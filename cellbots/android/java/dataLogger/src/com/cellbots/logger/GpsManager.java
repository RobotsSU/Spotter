/*
 * Copyright (C) 2011 Google Inc.
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

package com.cellbots.logger;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.util.Iterator;

/**
 * Simplifies handling of the GPS to only focus on what we want to log.
 *
 * @author clchen@google.com (Charles L. Chen)
 */
public class GpsManager {
    /**
     * Callback interface to be implemented by the calling Context.
     */
    public interface GpsManagerListener {
        public void onGpsLocationUpdate(long time, float accuracy, double latitude,
                double longitude, double altitude, float bearing, float speed);

        public void onGpsStatusUpdate(
                long time, int maxSatellites, int actualSatellites, int timeToFirstFix);

        public void onGpsNmeaUpdate(long time, String nmeaString);
    }

    /**
     * We need to keep track of all of these listeners so we can de-register
     * them later.
     */
    private GpsStatus.Listener gpsStatusListener;

    private GpsStatus.NmeaListener nmeaListener;

    private LocationListener locationListener;

    private LocationManager locationManager;

    private Context parentCtx;

    private GpsManagerListener mCallbackListener;

    public GpsManager(Context ctx, GpsManagerListener listener) {
        parentCtx = ctx;
        mCallbackListener = listener;
        locationManager = (LocationManager) parentCtx.getSystemService(Context.LOCATION_SERVICE);
        gpsStatusListener = new MyGpsStatusListener();
        nmeaListener = new MyNmeaListener();
        locationListener = new MyLocationListener();

        locationManager.addGpsStatusListener(gpsStatusListener);
        locationManager.addNmeaListener(nmeaListener);
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    public void shutdown() {
        try {
            locationManager.removeGpsStatusListener(gpsStatusListener);
            locationManager.removeNmeaListener(nmeaListener);
            locationManager.removeUpdates(locationListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class MyLocationListener implements LocationListener {
        public void onLocationChanged(Location location) {
            mCallbackListener.onGpsLocationUpdate(location.getTime(), location.getAccuracy(),
                    location.getLatitude(), location.getLongitude(), location.getAltitude(),
                    location.getBearing(), location.getSpeed());
        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

    private class MyGpsStatusListener implements GpsStatus.Listener {
        public void onGpsStatusChanged(int event) {
            LocationManager locationManager = (LocationManager) parentCtx.getSystemService(
                    Context.LOCATION_SERVICE);
            // Request a new GpsStatus object instead of having one filled in.
            GpsStatus status = locationManager.getGpsStatus(null);
            int satelliteCount = 0;
            Iterator<GpsSatellite> it = status.getSatellites().iterator();
            while (it.hasNext()) {
                it.next();
                satelliteCount++;
            }
            mCallbackListener.onGpsStatusUpdate(System.currentTimeMillis(),
                    status.getMaxSatellites(), satelliteCount, status.getTimeToFirstFix());
        }
    }

    private class MyNmeaListener implements GpsStatus.NmeaListener {
        public void onNmeaReceived(long timestamp, String nmea) {
            mCallbackListener.onGpsNmeaUpdate(timestamp, nmea);
        }
    }

}
