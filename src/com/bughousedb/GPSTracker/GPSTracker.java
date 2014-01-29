/** 
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 John Nahlen (john.nahlen@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * */
package com.bughousedb.GPSTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class GPSTracker implements Service {
	private Location lastUploadedLocation;
	private Location lastLocation;
	private List<Location> locationHistory;
	private Timer timer;
	private boolean gotFirstFix;
	private MainActivity mainActivity;
	private String uploadUrl;
	private int uploadFrequency;
	private ServiceStatus serviceStatus;
	private List<LocationCallback> locationChangedCallbacks;
	private LocationListener locationListener;
	private LocationManager locationManager;
	private String locationProvider;
	
	private void gotFirstFix() {
		gotFirstFix = true;
		TimerTask task = new TimerTask() {
			public void run() {
				Location location = lastLocation;
				
				if (lastUploadedLocation != null) {
					boolean areLocationsTheSame = !Utils.areLocationsDifferent(location, lastUploadedLocation);
					if (areLocationsTheSame) {
						return;
					}
				}
				
				if (location == null) {
					return;
				}
				
				String postbody = mainActivity.getDeviceName() + "," + (location.getTime()/1000) + "," + Utils.round(location.getLatitude()) + "," + Utils.round(location.getLongitude()) + "," + location.getAltitude() + "," + location.getSpeed() + "," + location.getAccuracy() + "," + location.getBearing() + "," + "\n";
				mainActivity.sendMessageToTextView("Uploading Coordinates Now\n");
				System.out.println("Calling URL: " + getUploadUrl());
				Utils.uploadToServer(mainActivity, getUploadUrl(), "coords="+postbody);
				lastUploadedLocation = location;
			}
		};
		
		timer.scheduleAtFixedRate(task, 0, getUploadFrequency());
	}
	
	// Called when the activity's onLowMemory() method is fired
	public void cleanup() {
		locationHistory.clear();
	}
	
	public Location getLastKnownLocation() {
		return lastLocation;
	}
	
	public void setUploadUrl(String url) {
		this.uploadUrl = url;
	}
	
	public String getUploadUrl() {
		return this.uploadUrl;
	}
	
	public void init() {
		serviceStatus = ServiceStatus.INITIALIZED;
		locationHistory = new ArrayList<Location>();
		timer = new Timer();
		gotFirstFix = false;
		locationChangedCallbacks = new ArrayList<LocationCallback>();
		locationListener = new LocationListener() {
			public void onStatusChanged(String provider, int status, Bundle extras) {
				System.out.println(String.format("Received notification of status changed: %s %s",provider,extras.getString("satellites")));
			}
			
			public void onProviderEnabled(String provider) {
				System.out.println(String.format("Received notification that location provider %s has been enabled",provider));
			}
			
			public void onProviderDisabled(String provider) {
				System.out.println(String.format("Received notification that location provider %s has been disabled",provider));
			}
			
			public void onLocationChanged(Location location) {
				for(LocationCallback locationCallback : locationChangedCallbacks) {
					locationCallback.setLocation(location);
					locationCallback.run();
				}
			}
		};
		mainActivity.sendMessageToTextView("Please wait while a GPS fix is acquired...\n");
		locationManager = (LocationManager)mainActivity.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
		locationProvider = LocationManager.GPS_PROVIDER;
		
//		if (!locationManager.isProviderEnabled(locationProvider)) {
//			List<String> providers = locationManager.getAllProviders();
//			for(String name : providers) {
//				if (locationManager.isProviderEnabled(name)) {
//					LocationProvider _locationProvider = locationManager.getProvider(name);
//					if (!_locationProvider.hasMonetaryCost()) {
//						locationProvider = _locationProvider.getName();
//					}
//				}
//			}
//		}
		
		System.out.println(String.format("Using locationProvider=%s",locationProvider));
		locationChangedCallbacks.add(new LocationCallback() {
			public void run() {
				Location location = getLocation();
				if (location == null) return;
				if (lastLocation != null && !Utils.areLocationsDifferent(location, lastLocation)) {
					return;
				}
				
				String str = Utils.locationToString(location);
				mainActivity.sendMessageToTextView(str);
				locationHistory.add(location);
				lastLocation = location;
				if (!gotFirstFix) {
					gotFirstFix();
				}
			}
		});
	}
	
	public void start() {
		if (serviceStatus == ServiceStatus.STARTED) {
			return;
		}
		
		System.out.println("GPS Tracker Service has been started");
		locationManager.requestLocationUpdates(locationProvider, 0, 0, locationListener);
		serviceStatus = ServiceStatus.STARTED;
	}
	
	public void stop() {
		if (serviceStatus == ServiceStatus.STOPPED) {
			return;
		}
		
		System.out.println("GPS Tracker Service has been stopped");
		locationManager.removeUpdates(locationListener);
		serviceStatus = ServiceStatus.STOPPED;
	}

	public MainActivity getMainActivity() {
		return mainActivity;
	}

	public void setMainActivity(MainActivity mainActivity) {
		this.mainActivity = mainActivity;
	}

	public int getUploadFrequency() {
		return uploadFrequency;
	}

	public void setUploadFrequency(int uploadFrequency) {
		this.uploadFrequency = uploadFrequency;
	}
}
