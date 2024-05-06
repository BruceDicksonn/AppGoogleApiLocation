# AppGoogleApiLocation
This project is a basic guide for we can be learn to work with a location service in android native apps.

<h2>Manifest.xml:</h2>

    <p>
        All permissions below are very important when we want to work with capturing user location:
        <code>
        <pre>
            &lt;android:name="android.permission.ACCESS_COARSE_LOCATION"&gt;
            &lt;uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"&gt;
            &lt;uses-permission android:name="android.permission.FOREGROUND_SERVICE" &gt;
            &lt;uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"&gt;
            &lt;uses-permission android:name="android.permission.POST_NOTIFICATIONS"&gt;
        </pre>
        </code>
    </p>
        
    <p>It is necessary to make the service explicit in the manifest file as well</p>
        <code>
        <pre>
        ...
                &lt;service
                    android:name=".LocationService"
                    android:exported="false"&gt;    
        ...	
        </pre>
        </code>	
    
    
    <h2>General information:</h2>

        <ol type="I" >
            <li>
                <p>In addition to providing permissions in the manifest, we must check whether these permissions have been given so that we can work with the necessary instances. If the user has not allowed them, request them.</p>
    
                <code>
                <pre>
                    private boolean requestLocationPermissions() {
                        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION,}, 0);
                            return false;
            
                        }
            
                        checkPermissionLocation = true;
                        return true;
                    }
                </pre>
                </code>
            </li>
            <li>
                <p>Can be important check if service is running or no, This prevents the service from starting more than once during the app's execution during lifecycle changes</p>
                <code>
                <pre>
                    private boolean checkRunningService(String serviceClassName) {
                        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                        for (ActivityManager.RunningServiceInfo info : manager.getRunningServices(Integer.MAX_VALUE)) {
                            if (info.service.getClassName().equals(serviceClassName)) {
                                return true;
                            }
                        }
            
                        return false;	
                    }
                </pre>
                </code>                
            </li>
            <li>
                <p>
                    In Android API´s 10 or current versions it is necessary to show a notification that indicates
                    to the user that a service is running in the background. This is an approach that we must adopt so 
                    that our service works on any version of Android.
                    The practice of having a global application context also allows us to have a reference to an active 
                    activity (usually main) so that we can connect it to the service.
                </p>
            </li>
        </ol>
        
    <h2>General location service concepts:</h2>	
    
        <p>
            Now we will talk about some important examples that we should keep in mind when building our Service class.
        </p>
        
        <ul>
            <li>
                <p>
                    <strong>currentLocation</strong>: guarantees us a global instance Location referring to
                    the user's current location, for which we will define the values when the callback 
                    sends the response;
                </p>
            </li>
            <li>
                <p>
                    <strong>fusedLocationProviderClient</strong>: instance of FusedLocationProviderClient class of 
                    com.google.android.gms.location package. Its usefulness is to connect the client (user device) 
                    to the location API. This is the most important class when it comes to location services;
                </p>
                <p>
                    <code>
                        <pre>fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);</pre>
                    </code>

                    The example above shows how we initialize our client, see how our Context instance is 
                    so important in the service. We start the client using LocalizationServices class holder 
                    of the all services/providers related to location;

                </p>			
            </li>
            <li>
                <p>
                    <strong>locationRequest</strong>: represent how we make requests to the API, as it dictates how requests will be made, how long, etc...
                </p>	
                <p>
                    <code>
                        <pre>locationRequest = new LocationRequest.Builder(1000)
                            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                            .build();
                        </pre>
                    </code>
                </p>
                    
            </li>
            <li>
                <p>
                    <strong>locationCallback</strong>: This is the callback that holds the locations received by the api,
                    its implementation is of paramount importance when we want to stay
                    continually checking the user's location;
                </p>		
            </li>
            <li>
                <p>
                    <strong>notificationID and channel_ID</strong>: Its usefulness is to meet the requirements established
                    by Google, which states that devices with Android 10 or more must display a notification that indicates
                    to the user that the service is running in the background.
                </p>		
            </li>
            <li>
                <p>
                    <strong>locationSettings</strong>: serve to verify that all permissions and settings granted by the user
                    meet the standard we desire. (high precision and allows foreground capture). 
                    This way, we can check if they are valid and if they are not, we send a proper exception;
                </p>	
                <p>
                    <code>
                        <pre>
                            locationSettings = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build();

                            SettingsClient settingsClient = LocationServices.getSettingsClient(context);
                            Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(locationSettings);
                    
                            task.addOnSuccessListener((Activity) context, locationSettingsResponse -> {
                    
                                Toast.makeText(context, "All settings are satisfied. The client can be initialized to request the user's location .", Toast.LENGTH_LONG).show();
                            }).addOnFailureListener((Activity) context, exception -> {
                                if (exception instanceof ResolvableApiException) {
                                    
                                    // If the settings are not satisfied, we can fix this by showing a dialog box to the user        
                                    try {
                    
                                        /**
                                            Create a Resolvable instance of ResolvableApiException to show the dialog,
                                            call the instance's startResolutionForResult() function and check the result in the OnActivityResult() method
                                         **/
                                        ResolvableApiException resolvable = (ResolvableApiException) exception;
                                        resolvable.startResolutionForResult((Activity) context, 0);
                    
                                    } catch (IntentSender.SendIntentException sendEx) {
                    
                                    }
                                }
                            });

                        </pre>
                    </code>
                </p>            
            </li>
            <li>
                <p>
                    Some methods and structure that we should follow:
                    <code>
                        <pre>
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
                        </pre>
                    </code>
                </p>
            </li>	
        </ul>
