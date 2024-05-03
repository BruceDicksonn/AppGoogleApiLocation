package com.example.appgoogleapilocation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityManagerCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    boolean checkPermissionLocation = false;
    public static Context globalContext;
    Intent intentService;

    public static List<String> listLocations = new ArrayList();
    static ArrayAdapter<String> adapter;
    static Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        globalContext = this;

        toolbar = ((Toolbar) findViewById(R.id.toolbar));
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listLocations);
        ((ListView) findViewById(R.id.listLocation)).setAdapter(adapter);
        adapter.notifyDataSetChanged();

        // function invoked when we want to open the location in the Google Maps App
        ((ListView) findViewById(R.id.listLocation)).setOnItemClickListener((parent, view, position, id) -> {
            showOpenMapsDialog(listLocations.get(position));
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (requestLocationPermissions() && !checkRunningService(LocationService.class.getName())) {
            startServices();
        }

    }

    private boolean checkRunningService(String serviceClassName) {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo info : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (info.service.getClassName().equals(serviceClassName)) {
                return true;
            }
        }

        return false;
    }

    private boolean requestLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION,}, 0);
            return false;

        }

        checkPermissionLocation = true;
        return true;
    }

    private void startServices() {

        intentService = new Intent(this, LocationService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intentService);
            return;
        }

        startService(intentService);

    }

    public static void refreshListLocation() {
        adapter.notifyDataSetChanged();
        toolbar.setTitle(String.format("Captured points: %d", listLocations.size()));
    }

    private void showOpenMapsDialog(String coordenates) {
        new AlertDialog.Builder(this)
                .setTitle("Go to maps")
                .setMessage("Do you wish to see your current location ?")
                .setIcon(android.R.drawable.ic_menu_mylocation)
                .setPositiveButton("Open Maps", (dialog, which) -> {
                    /** how to send coordenates to maps **/
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("geo:%s?z=15", coordenates)));
                    startActivity(intent);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                })
                .create()
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(intentService);
    }
}