/*
 * Copyright (C) 2014 Loren A. Baker. All rights reserved.
 *
 */

package com.lbconsulting.android.location;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.lbconsulting.about.About;
import com.lbconsulting.android.location.R.string;

public class MainActivity extends FragmentActivity implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener,
		com.google.android.gms.location.LocationListener {

	// A request to connect to Location Services
	private LocationRequest mLocationRequest;

	// Stores the current instantiation of the location client in this object
	private LocationClient mLocationClient;

	// Handles to UI views
	private TextView mLatLng;
	private TextView mAddress;
	private ProgressBar mProgressBar;
	private TextView mConnectionStatus;

	// Handle to SharedPreferences and editor for this app
	SharedPreferences mPrefs;
	SharedPreferences.Editor mEditor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Get handles to the UI view objects
		mLatLng = (TextView) findViewById(R.id.lat_lng);
		mAddress = (TextView) findViewById(R.id.address);
		mProgressBar = (ProgressBar) findViewById(R.id.address_progress);
		mConnectionStatus = (TextView) findViewById(R.id.text_connection_status);

		// Create a new global location parameters object
		mLocationRequest = LocationRequest.create();

		// Use high accuracy
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

		/*
		 * Create a new location client, using the enclosing class to
		 * handle callbacks.
		 */
		mLocationClient = new LocationClient(this, this, this);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_settings:
				// Toast.makeText(this, "action_settings", Toast.LENGTH_SHORT).show();
				startActivity(new Intent(this, PreferencesActivity.class));
				return true;

			case R.id.action_about:

				Resources res = getResources();
				String aboutText = res.getString(string.dialogAbout_aboutText);
				String copyrightText = res.getString(string.copyright_text);
				String okButtonText = res.getString(string.btnOK_text);
				About.show(this, aboutText, copyrightText, okButtonText);
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}

	}

	@Override
	public void onStop() {
		// After disconnect() is called, the client is considered "dead".
		mLocationClient.disconnect();

		super.onStop();
	}

	@Override
	public void onStart() {

		super.onStart();

		/*
		 * Connect the client. Don't re-start any requests here;
		 * instead, wait for onResume()
		 */
		try {
			mLocationClient.connect();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/*
	 * Handle results returned to this Activity by other Activities started with
	 * startActivityForResult(). In particular, the method onConnectionFailed() in
	 * LocationUpdateRemover and LocationUpdateRequester may call startResolutionForResult() to
	 * start an Activity that handles Google Play services problems. The result of this
	 * call returns here, to onActivityResult.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

		// Choose what to do based on the request code
		switch (requestCode) {

		// If the request code matches the code sent in onConnectionFailed
			case LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST:

				switch (resultCode) {
				// If Google Play services resolved the problem
					case Activity.RESULT_OK:

						// Log the result
						Log.d(LocationUtils.APPTAG, getString(R.string.resolved));

						// Display the result
						// mConnectionState.setText(R.string.connected);
						mConnectionStatus.setText(R.string.resolved);
						break;

					// If any other result was returned by Google Play services
					default:
						// Log the result
						Log.d(LocationUtils.APPTAG, getString(R.string.no_resolution));

						// Display the result
						// mConnectionState.setText(R.string.disconnected);
						mConnectionStatus.setText(R.string.no_resolution);

						break;
				}
				break;

			// If any other request code was received
			default:
				// Report that this Activity received an unknown requestCode
				Log.d(LocationUtils.APPTAG,
						getString(R.string.unknown_activity_request_code, requestCode));

				break;
		}
	}

	/**
	 * Verify that Google Play services is available before making a request.
	 *
	 * @return true if Google Play services is available, otherwise false
	 */
	private boolean servicesConnected() {

		// Check that Google Play services is available
		int resultCode =
				GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {
			// In debug mode, log the status
			Log.d(LocationUtils.APPTAG, getString(R.string.play_services_available));

			// Continue
			return true;

		} else {
			// Google Play services was not available for some reason
			// Display an error dialog
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
			if (dialog != null) {
				ErrorDialogFragment errorFragment = new ErrorDialogFragment();
				errorFragment.setDialog(dialog);
				errorFragment.show(getSupportFragmentManager(), LocationUtils.APPTAG);
			}
			return false;
		}
	}

	/**
	 * Invoked by the "Get Location" button.
	 *
	 * Calls getLastLocation() to get the current location
	 *
	 * @param v The view object associated with this method, in this case a Button.
	 */
	public void getLocation(View v) {

		// If Google Play Services is available
		if (servicesConnected()) {
			// Get the current location
			Location currentLocation = mLocationClient.getLastLocation();

			// Display the current location in the UI
			mLatLng.setText(LocationUtils.getLatLng(this, currentLocation));
			mAddress.setText("");

			getAddress(currentLocation);
		}
	}

	public void showLocation(View v) {
		Location currentLocation = null;
		Uri geoLocation = null;
		if (servicesConnected()) {
			// Get the current location
			currentLocation = mLocationClient.getLastLocation();
			if (currentLocation != null) {
				geoLocation = getGeoLocation(currentLocation);
			}

			if (geoLocation != null) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(geoLocation);
				if (intent.resolveActivity(getPackageManager()) != null) {
					startActivity(intent);
				}
			}
		}
	}

	public void emailLocation(View v) {

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		Resources res = getResources();
		String emailRecipents = sharedPrefs.getString(res.getString(R.string.email_recipients_key),
				res.getString(R.string.email_recipients_default));

		StringBuilder sb = new StringBuilder();
		sb.append(mLatLng.getText().toString())
				.append("\n")
				.append(mAddress.getText().toString());

		Intent intent = new Intent(Intent.ACTION_SENDTO);
		intent.setType("message/rfc822");
		intent.putExtra(Intent.EXTRA_SUBJECT, "Location: ");
		intent.setData(Uri.parse("mailto:")); // only email apps should handle this
		if (!emailRecipents.equals(res.getString(R.string.email_recipients_default))) {
			intent.putExtra(Intent.EXTRA_EMAIL, new String[] { emailRecipents });
		}

		intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
		Intent mailer = Intent.createChooser(intent, null);
		startActivity(mailer);
	}

	private Uri getGeoLocation(Location currentLocation) {
		StringBuilder geoLocation = new StringBuilder();
		geoLocation.append("geo:0,0?q=")
				.append(currentLocation.getLatitude())
				.append(",")
				.append(currentLocation.getLongitude());

		return Uri.parse(geoLocation.toString());
	}

	public void getAddress(Location currentLocation) {

		// In Gingerbread and later, use Geocoder.isPresent() to see if a geocoder is available.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD && !Geocoder.isPresent()) {
			// No geocoder is present. Issue an error message
			Toast.makeText(this, R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
			return;
		}

		if (servicesConnected()) {

			ConnectivityManager cm =
					(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

			NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
			boolean isConnectedToInternet = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
			if (isConnectedToInternet) {

				// Turn the progress bar indicator on
				mProgressBar.setVisibility(View.VISIBLE);

				// Start the background task
				(new MainActivity.GetAddressTask(this)).execute(currentLocation);
			} else {
				// no Internet service
				mAddress.setText("N/A (No Internet service available.)");
			}
		}
	}

	/*
	 * Called by Location Services when the request to connect the
	 * client finishes successfully. At this point, you can
	 * request the current location or start periodic updates
	 */
	@Override
	public void onConnected(Bundle bundle) {
		mConnectionStatus.setText(R.string.connected);
	}

	/*
	 * Called by Location Services if the connection to the
	 * location client drops because of an error.
	 */
	@Override
	public void onDisconnected() {
		mConnectionStatus.setText(R.string.disconnected);
	}

	/*
	 * Called by Location Services if the attempt to
	 * Location Services fails.
	 */
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {

		/*
		 * Google Play services can resolve some errors it detects.
		 * If the error has a resolution, try sending an Intent to
		 * start a Google Play services activity that can resolve
		 * error.
		 */
		if (connectionResult.hasResolution()) {
			try {

				// Start an Activity that tries to resolve the error
				connectionResult.startResolutionForResult(
						this, LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

				/*
				* Thrown if Google Play services canceled the original
				* PendingIntent
				*/

			} catch (IntentSender.SendIntentException e) {

				// Log the error
				e.printStackTrace();
			}
		} else {

			// If no resolution is available, display a dialog to the user with the error.
			showErrorDialog(connectionResult.getErrorCode());
		}
	}

	/**
	 * Report location updates to the UI.
	 *
	 * @param location The updated location.
	 */
	@Override
	public void onLocationChanged(Location location) {

		// Report to the UI that the location was updated
		mConnectionStatus.setText(R.string.location_updated);

		// In the UI, set the latitude and longitude to the value received
		mLatLng.setText(LocationUtils.getLatLng(this, location));
	}

	/**
	 * An AsyncTask that calls getFromLocation() in the background.
	 * The class uses the following generic types:
	 * Location - A {@link android.location.Location} object containing the current location,
	 *            passed as the input parameter to doInBackground()
	 * Void     - indicates that progress units are not used by this subclass
	 * String   - An address passed to onPostExecute()
	 */
	protected class GetAddressTask extends AsyncTask<Location, Void, String> {

		// Store the context passed to the AsyncTask when the system instantiates it.
		Context localContext;

		// Constructor called by the system to instantiate the task
		public GetAddressTask(Context context) {

			// Required by the semantics of AsyncTask
			super();

			// Set a Context for the background task
			localContext = context;
		}

		/**
		 * Get a geocoding service instance, pass latitude and longitude to it, format the returned
		 * address, and return the address to the UI thread.
		 */
		@Override
		protected String doInBackground(Location... params) {
			/*
			 * Get a new geocoding service instance, set for localized addresses. This example uses
			 * android.location.Geocoder, but other geocoders that conform to address standards
			 * can also be used.
			 */
			Geocoder geocoder = new Geocoder(localContext, Locale.getDefault());

			// Get the current location from the input parameter list
			Location location = params[0];

			// Create a list to contain the result address
			List<Address> addresses = null;

			// Try to get an address for the current location. Catch IO or network problems.
			try {

				/*
				 * Call the synchronous getFromLocation() method with the latitude and
				 * longitude of the current location. Return at most 1 address.
				 */
				addresses = geocoder.getFromLocation(location.getLatitude(),
						location.getLongitude(), 1
						);

				// Catch network or other I/O problems.
			} catch (IOException exception1) {

				// Log an error and return an error message
				Log.e(LocationUtils.APPTAG, getString(R.string.IO_Exception_getFromLocation));

				// print the stack trace
				exception1.printStackTrace();

				// Return an error message
				return (getString(R.string.IO_Exception_getFromLocation));

				// Catch incorrect latitude or longitude values
			} catch (IllegalArgumentException exception2) {

				// Construct a message containing the invalid arguments
				String errorString = getString(
						R.string.illegal_argument_exception,
						location.getLatitude(),
						location.getLongitude()
						);
				// Log the error and print the stack trace
				Log.e(LocationUtils.APPTAG, errorString);
				exception2.printStackTrace();

				//
				return errorString;
			}
			// If the reverse geocode returned an address
			if (addresses != null && addresses.size() > 0) {

				// Get the first address
				Address address = addresses.get(0);
				String street = LocationUtils.EMPTY_STRING;
				String city = LocationUtils.EMPTY_STRING;
				String state = LocationUtils.EMPTY_STRING;
				String zipCode = LocationUtils.EMPTY_STRING;
				String countryName = LocationUtils.EMPTY_STRING;

				// Format the address
				StringBuilder addressText = new StringBuilder();
				// If there's a street address, add it
				street = address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : "";
				// Locality is usually a city
				city = address.getLocality();
				// AdminArea is usually a state
				state = address.getAdminArea();
				zipCode = address.getPostalCode();
				countryName = address.getCountryName();
				String lineSep = System.getProperty("line.separator");

				if (!street.isEmpty()) {
					addressText.append(street).append(lineSep);
				}
				if (!city.isEmpty()) {
					addressText.append(city);
				}
				if (!state.isEmpty()) {
					addressText.append(", ").append(state);
				}
				if (!zipCode.isEmpty()) {
					addressText.append(" ").append(zipCode);
				}
				if (!countryName.isEmpty()) {
					addressText.append(lineSep).append(countryName);
				}

				// Return the address text
				return addressText.toString();

				// If there aren't any addresses, post a message
			} else {
				return getString(R.string.no_address_found);
			}
		}

		/**
		 * A method that's called once doInBackground() completes. Set the text of the
		 * UI element that displays the address. This method runs on the UI thread.
		 */
		@Override
		protected void onPostExecute(String address) {

			// Turn off the progress bar
			mProgressBar.setVisibility(View.GONE);

			// Set the address in the UI
			mAddress.setText(address);
		}
	}

	/**
	 * Show a dialog returned by Google Play services for the
	 * connection error code
	 *
	 * @param errorCode An error code returned from onConnectionFailed
	 */
	private void showErrorDialog(int errorCode) {

		// Get the error dialog from Google Play services
		Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
				errorCode,
				this,
				LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

		// If Google Play services can provide an error dialog
		if (errorDialog != null) {

			// Create a new DialogFragment in which to show the error dialog
			ErrorDialogFragment errorFragment = new ErrorDialogFragment();

			// Set the dialog in the DialogFragment
			errorFragment.setDialog(errorDialog);

			// Show the error dialog in the DialogFragment
			errorFragment.show(getSupportFragmentManager(), LocationUtils.APPTAG);
		}
	}

	/**
	 * Define a DialogFragment to display the error dialog generated in
	 * showErrorDialog.
	 */
	public static class ErrorDialogFragment extends DialogFragment {

		// Global field to contain the error dialog
		private Dialog mDialog;

		/**
		 * Default constructor. Sets the dialog field to null
		 */
		public ErrorDialogFragment() {
			super();
			mDialog = null;
		}

		/**
		 * Set the dialog to display
		 *
		 * @param dialog An error dialog
		 */
		public void setDialog(Dialog dialog) {
			mDialog = dialog;
		}

		/*
		 * This method must return a Dialog to the DialogFragment.
		 */
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return mDialog;
		}
	}

}
