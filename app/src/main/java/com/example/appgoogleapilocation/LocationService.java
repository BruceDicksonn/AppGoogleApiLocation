/**
 * Algumas informações importantes:
 **/


package com.example.appgoogleapilocation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.FragmentManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;

@SuppressLint("MissingPermission")
public class LocationService extends Service {

    Context context = MainActivity.globalContext;

    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationSettingsRequest locationSettings;

    /**
     * This is the callback that holds the locations received by the api,
     * its implementation is of paramount importance when we want to stay
     * continually checking the user's location;
     * <p>
     * Esse é o callback que detém as localizações recebidas pela api,
     * sua implementação é de suma importancia quando queremos ficar
     * checando continuamente a localização do usuário;
     **/
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

                Log.e("ServicoLocalizacao", String.format("Latitude: %f Longitude: %f", latitude, longitude));
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        /**

         We start the client using LocalizationServices class holder of the all
         services/providers related to location

         Iniciamos o client usando a classe LocationServices detentora de todos
         os servicos/providers relacionados a localização.
         * **/
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);

        /**

         locationRequest is an important item, as it dictates how requests will be made, how long, etc...

         O request é um item importante, pois ele é quem dita como as requisições serão
         feitas, de quanto em quanto tempo e etc ...
         * **/
        locationRequest = new LocationRequest.Builder(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .build();

        initializeConfigurationLocation();

    }

    private void initializeConfigurationLocation() {

        /**
         *
         Receive location settings

         These settings serve to verify that all permissions and settings granted by the user
         meet the standard we desire. (high precision and allows foreground capture)

         This way, we can check if they are valid and if they are not, we send a proper exception.

         Receber configuracoes de localização

         Essas configurações servem para verificarmos se todas as permissões e configuracoes
         dadas pelo usuário atendem ao padrão que queremos. (alta precisão e permitir captura em segundo plano)

         Assim, conseguimos checar se as mesmas estão válidas e se não estiverem, enviamos uma
         exception adequada;
         * **/
        locationSettings = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build();

        SettingsClient settingsClient = LocationServices.getSettingsClient(context);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(locationSettings);

        task.addOnSuccessListener((Activity) context, locationSettingsResponse -> {

            Toast.makeText(context, "All settings are satisfied. The client can be initialized to request the user's location .", Toast.LENGTH_LONG).show();
        }).addOnFailureListener((Activity) context, exception -> {
            if (exception instanceof ResolvableApiException) {
                /**

                 If the settings are not satisfied, we can fix this by showing a dialog box to the user

                 Caso as configurações não tiverem sido satisfeitas, podemos consertar isso
                 mostrando uma caixa de diálogo ao usuário.

                **/

                try {

                    /**
                     *

                        Create a Resolvable instance of ResolvableApiException to show the dialog,
                        call the instance's startResolutionForResult() function and check the result in the OnActivityResult() method

                        Crie uma instância resolvable de ResolvableApiException.
                        Para mostrar o diálogo, chame a funcção startResolutionForResult() de resolvable
                        e verifique o resultado em OnActivityResult();

                     **/
                    ResolvableApiException resolvable = (ResolvableApiException) exception;
                    resolvable.startResolutionForResult((Activity) context, 0);

                } catch (IntentSender.SendIntentException sendEx) {

                }
            }
        });

    }


    int notification_id = 9999;
    String channel_id = "LocationService";

    private void createNotificationChannel() {

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel notificationChannel;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(channel_id, "Location Service", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(notificationChannel);
        }

    }

    private Notification createNotification() {

        createNotificationChannel();
        Notification notification = null;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            notification = new Notification.Builder(context, channel_id)
                    .setContentTitle("Location Service")
                    .setContentText("We´re receiving your current location. Please, don´t turn off the gps.")
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .build();

            return notification;
        }

        notification = new Notification.Builder(context)
                .setContentTitle("Location Service")
                .setContentText("We´re receiving your current location. Please, don´t turn off the gps.")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(Notification.PRIORITY_HIGH)
                .build();

        return notification;
    }

    @SuppressLint("ForegroundServiceType")
    private void startService() {
        stopService();

        Notification notification = createNotification();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notification_id, notification, 0);
        }

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startService();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService();
    }

}
