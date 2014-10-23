/*
 * Copyright (C) 2014 Loren A. Baker. All rights reserved.
 *
 */
package com.lbconsulting.android.location;

import android.content.Context;
import android.location.Location;

/**
 * Defines app-wide constants and utilities
 */
public final class LocationUtils {

	// Debugging tag for the application
	public static final String APPTAG = "Location";

	/*
	 * Define a request code to send to Google Play services
	 * This code is returned in Activity.onActivityResult
	 */
	public final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

	// Create an empty string for initializing strings
	public static final String EMPTY_STRING = new String();

	/**
	 * Get the latitude and longitude from the Location object returned by
	 * Location Services.
	 *
	 * @param currentLocation A Location object containing the current location
	 * @return The latitude and longitude of the current location, or null if no
	 * location is available.
	 */
	public static String getLatLng(Context context, Location currentLocation) {
		// If the location is valid
		if (currentLocation != null) {

			double latitude = currentLocation.getLatitude();
			String latitudeSuffix = EMPTY_STRING;
			if (latitude > 0) {
				latitudeSuffix = " N";
			} else {
				latitudeSuffix = " S";
				latitude = Math.abs(latitude);
			}

			double longitude = currentLocation.getLongitude();
			String longitudeSuffix = EMPTY_STRING;
			if (longitude > 0) {
				longitudeSuffix = " E";
			} else {
				longitudeSuffix = " W";
				longitude = Math.abs(longitude);
			}

			// Return the latitude and longitude as strings

			StringBuilder sb = new StringBuilder();
			sb
					.append(latitude).append(latitudeSuffix)
					.append(", ")
					.append(longitude).append(longitudeSuffix);

			return sb.toString();

		} else {

			// Otherwise, return the empty string
			return EMPTY_STRING;
		}
	}
}
