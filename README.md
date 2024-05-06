# AppGoogleApiLocation
This project is a basic guide for we can be learn to work with a location service in android native apps.

## Manifest.xml

All permissions below are very important when we want to work with capturing user location:
```bash
  <android:name="android.permission.ACCESS_COARSE_LOCATION">
  <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION">
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE">
  <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS">
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS">
```

It is necessary to make the service explicit in the manifest file as well:
```bash
  <service android:name=".LocationService" android:exported="false>       
````

*******

## General information:

1. In addition to providing permissions in the manifest, we must check whether these permissions have been given so that we can work with the necessary instances. If the user has not allowed them, request them.

```bash

private boolean requestLocationPermissions() {
    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION,}, 0);
      return false;

  }

  checkPermissionLocation = true;
  return true;
}
```

2. Can be important check if service is running or no, This prevents the service from starting more than once during the app's execution during lifecycle changes
````bash
private boolean checkRunningService(String serviceClassName) {
    ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
    for (ActivityManager.RunningServiceInfo info : manager.getRunningServices(Integer.MAX_VALUE)) {
        if (info.service.getClassName().equals(serviceClassName)) {
            return true;
        }
    }

    return false;	
}
````
3.  In Android API´s 10 or current versions it is necessary to show a notification that indicates
    to the user that a service is running in the background. This is an approach that we must adopt so
    that our service works on any version of Android.
    The practice of having a global application context also allows us to have a reference to an active
    activity (usually main) so that we can connect it to the service.

## General location service concepts:

Now we will talk about some important examples that we should keep in mind when building our Service class.

1. **currentLocation**: guarantees us a global instance Location referring to the user's current location, for which we will define the values when the callback
   sends the response;

2. **fusedLocationProviderClient**: instance of FusedLocationProviderClient class of com.google.android.gms.location package. Its usefulness is to connect the client (user device) to the location API. This is the most important class when it comes to location services;
```bash
fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
```
The example above shows how we initialize our client, see how our Context instance is
so important in the service. We start the client using LocalizationServices class holder
of the all services/providers related to location;

3. **locationRequest**: represent how we make requests to the API, as it dictates how requests will be made, how long, etc...
````bash
locationRequest = new LocationRequest.Builder(1000)
                            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                            .build();
````
4.**locationCallback**: This is the callback that holds the locations received by the api, its implementation is of paramount importance when we want to stay                    continually checking the user's location;
````bash
    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            for (Location location : locationResult.getLocations()) {

                currentLocation = location;

                double latitude = currentLocation.getLatitude();
                double longitude = currentLocation.getLongitude();

                MainActivity.listLocations.add(String.format("%f, %f", latitude, longitude));
                MainActivity.refreshListLocation();

                Log.e("Location Service", String.format("Latitude: %f Longitude: %f", latitude, longitude));
            }
        }
    };  
````
5.**notificationID and channel_ID**: Its usefulness is to meet the requirements established by Google, which states that devices with Android 10 or more must display a notification that indicates to the user that the service is running in the background.

6.**locationSettings**: serve to verify that all permissions and settings granted by the user meet the standard we desire. (high precision and allows foreground capture). This way, we can check if they are valid and if they are not, we send a proper exception;

````bash
locationSettings = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build();

SettingsClient settingsClient = LocationServices.getSettingsClient(context);
Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(locationSettings);

task.addOnSuccessListener(context, locationSettingsResponse -> {

    Toast.makeText(context, "All settings are satisfied. The client can be initialized to request the user's location .", Toast.LENGTH_LONG).show();

}).addOnFailureListener(context, exception -> {

    if (exception instanceof ResolvableApiException) {
        
        // If the settings are not satisfied, we can fix this by showing a dialog box to the user        
        try {

            //Create a Resolvable instance of ResolvableApiException to show the dialog,
            //call the instances startResolutionForResult() function and check the result in the OnActivityResult() method

            ResolvableApiException resolvable = (ResolvableApiException) exception;
            resolvable.startResolutionForResult(context, 0);

        } catch (IntentSender.SendIntentException sendEx) {

        }
    }
});
````

7.**Some methods and structure od Service class that we should follow:**
````bash

@Override
public int onStartCommand(Intent intent, int flags, int startId) {
    startService();
    return START_NOT_STICKY;
}

private void startService() {
    stopService();

    Notification notification = createNotification();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // This notification allows/indicates the service to continue running in the background
        startForeground(notification_id, notification, 0);
    }

    //starts GPS point capture monitoring
    fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
    );

    Log.e("LocationService", "Starting location service.");

}                            

private void stopService() {
    fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    Log.e("LocationService", "Closing location service.");
}                            
````