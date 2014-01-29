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

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Handler.Callback;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_settings:
				// Start up the SettingsActivity class to manage app preferences
				Intent intent = new Intent();
				intent.setClass(getApplicationContext(), SettingsActivity.class);
				startActivity(intent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private static GPSTracker gpsTracker;
	private static Handler textViewUpdateHandler;
	
	public static GPSTracker getGPSTracker() {
		return gpsTracker;
	}
	
	private String deviceName = null;
	private TextView textView;
	private StringBuilder textViewContents;
	
	public void sendMessageToTextView(String messageString) {
		Message message = new Message();
		message.obj = messageString;
		getTextViewUpdateHandler().sendMessage(message);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle bundle) {
		bundle.putString("textViewContents", textViewContents.toString());
		bundle.putString("start_stop_gps_btn_text",((Button)findViewById(R.id.start_stop_gps_btn)).getText().toString());
		bundle.putString("deviceName", this.deviceName);
	}
	
	@Override
	protected void onPause() {
		System.out.println("onPause()");
		super.onPause();
		if (textView != null) {
			textViewContents = new StringBuilder(textView.getText());
		}
	}
	
	@Override
	protected void onResume() {
		System.out.println("onResume()");
		super.onResume();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTitle(R.string.app_title);
		setContentView(R.layout.activity_main);
		textViewContents = new StringBuilder();
		if (savedInstanceState != null) {
			textViewContents.append(savedInstanceState.getString("textViewContents"));
			this.deviceName = savedInstanceState.getString("deviceName");
		}
		
		textViewUpdateHandler = new Handler(new Callback() {
			public boolean handleMessage(Message msg) {
				String text = (String)msg.obj;
				appendText(text);
				return true;
			};
		});
		
		if (gpsTracker == null) {
			SharedPreferences preferences = getSharedPreferences("com.bughousedb.GPSTracker_preferences",MODE_PRIVATE);
			String deviceNameKey = getString(R.string.device_name);
			this.deviceName = preferences.getString(deviceNameKey, "Android Device");
			
			gpsTracker = new GPSTracker();
			gpsTracker.setUploadUrl(preferences.getString(getString(R.string.upload_url), null));
			gpsTracker.setUploadFrequency(Integer.parseInt(preferences.getString(getString(R.string.upload_interval), "15000")));
			gpsTracker.setMainActivity(this);
			gpsTracker.init();
			gpsTracker.start();
		}
		
		gpsTracker.setMainActivity(this);
		
		textView = (TextView)findViewById(R.id.textView1);
		textView.setMovementMethod(new ScrollingMovementMethod());
		if (textViewContents != null) {
			textView.setText(textViewContents.toString());
		}
		
		((Button)findViewById(R.id.clear_log_btn)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				textView.setText("");
				textViewContents.delete(0,textViewContents.length());
				if (gpsTracker.getLastKnownLocation() != null) {
					appendText(String.format("Last Known Location:\n%s\n",Utils.locationToString(gpsTracker.getLastKnownLocation())));
				}
			}
		});
		
		
		((Button)findViewById(R.id.close_app_btn)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				System.out.println("GPS Tracker exiting now");
				System.exit(0);
			}
		});
		
		final Button _btn = (Button)findViewById(R.id.start_stop_gps_btn);
		if (savedInstanceState != null) {
			_btn.setText(savedInstanceState.getString("start_stop_gps_btn_text"));
		}
		_btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				final String start_gps = getString(R.string.start_gps);
				final String stop_gps = getString(R.string.stop_gps);
				
				if (_btn.getText().equals(stop_gps)) {
					gpsTracker.stop();
					_btn.setText(start_gps);
					sendMessageToTextView("GPS Tracker Service has stopped\n");
				} else if (_btn.getText().equals(start_gps)) {
					gpsTracker.start();
					_btn.setText(stop_gps);
					sendMessageToTextView("GPS Tracker Service has started\n");
				}
			}
		});
	}
	
	@Override
	public void onLowMemory() {
		System.out.println("onLowMemory()");
		super.onLowMemory();
		gpsTracker.cleanup();
	}

	private void appendText(String text) {
		if (textView != null) {
			textView.append(text);
			
			if (textViewContents != null) {
				textViewContents.append(text);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public static Handler getTextViewUpdateHandler() {
		return textViewUpdateHandler;
	}

	public static void setTextViewUpdateHandler(Handler textViewUpdateHandler) {
		MainActivity.textViewUpdateHandler = textViewUpdateHandler;
	}

	public TextView getTextView() {
		return textView;
	}

	public String getDeviceName() {
		return deviceName;
	}
}
