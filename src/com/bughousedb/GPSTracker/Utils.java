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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.location.Location;

public class Utils {
	public static double round(double value) {
		return round(value,6);
	}
	
	public static double round(double value,int precision) {
		return new BigDecimal(value).setScale(precision, RoundingMode.CEILING).doubleValue();
	}
	
	public static String locationToString(Location location) {
		return SimpleDateFormat.getTimeInstance().format(new Date(location.getTime())) + " " + Utils.round(location.getLatitude()) + ", " + Utils.round(location.getLongitude()) + " " + location.getAccuracy() + " " + location.getSpeed() + "\n";
	}
	
	public static boolean uploadToServer(MainActivity mainActivity,String url,String postBody) {
		String charset = "UTF-8";
		try {
			URLConnection connection = new URL(url).openConnection();
			connection.setDoOutput(true); // Triggers POST.
			connection.setRequestProperty("Accept-Charset", charset);
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);
			OutputStream output = null;
			try {
				output = connection.getOutputStream();
				output.write(postBody.getBytes(charset));
			} finally {
				if (output != null) {
					output.close();
				}
			}
			try {
				InputStream response = connection.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(response),8192);
				while(reader.ready()) {
					System.out.println(reader.readLine());
				}
			} catch(Exception e) {
				e.printStackTrace(System.err);
				mainActivity.sendMessageToTextView("Could not fetch response: " + e + "\n");
			}
			return true;
		} catch(Exception e) {
			e.printStackTrace(System.err);
			mainActivity.sendMessageToTextView("Could not upload coordinates: " + e + "\n");
			return false;
		}
	}
	
	public static boolean areLocationsDifferent(Location loc1,Location loc2) {
		int precision = 5;
		//boolean accuracyDiff = loc1.getAccuracy() != loc2.getAccuracy();
		boolean latDiff = round(loc1.getLatitude(),precision) != round(loc2.getLatitude(),precision);
		boolean longDiff = round(loc1.getLongitude(),precision) != round(loc2.getLongitude(),precision);
		return latDiff || longDiff;
	}
}
