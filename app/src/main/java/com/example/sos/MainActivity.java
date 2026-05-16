package com.example.sos;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 11;
    private static final int REQUEST_CHECK_SETTING = 10;
    private DatabaseHelper databaseHelper;
    private String[] permissions = {
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CALL_PHONE
    };

    private ImageView statusIcon;
    private TextView statusText;
    private MaterialButton startButton, stopButton;
    private MaterialCardView statusCard;
    private boolean isServiceActive = false;
    private MaterialToolbar appBar;

    @Override
    protected void onStart() {
        super.onStart();
        databaseHelper = new DatabaseHelper(this);
        if (databaseHelper.count() == 0) {
            Intent intent = new Intent(this, RegisterNumberActivity.class);
            startActivity(intent);
            Toast.makeText(this, "Register at least one contact", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isServiceActive = isMyServiceRunning(ServiceMine.class);
        updateServiceStatus();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        appBar = findViewById(R.id.topAppBar);
        setSupportActionBar(appBar);
        
        initializeViews();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("MYID", "CHANNELFOREGROUND", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager m = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (m != null) m.createNotificationChannel(channel);
        }
    }

    private void initializeViews() {
        statusIcon = findViewById(R.id.statusIcon);
        statusText = findViewById(R.id.statusText);
        startButton = findViewById(R.id.start);
        stopButton = findViewById(R.id.stop);
        statusCard = findViewById(R.id.statusCard);
    }

    public void stopServiceV(View view) {
        Intent notificationIntent = new Intent(this, ServiceMine.class);
        notificationIntent.setAction("STOP");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(notificationIntent);
        } else {
            stopService(notificationIntent);
        }
        Snackbar.make(findViewById(android.R.id.content), "Service Stopped!", Snackbar.LENGTH_LONG).show();
        isServiceActive = false;
        updateServiceStatus();
    }

    public void startServiceV(View view) {
        List<String> permissionList = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(permission);
            }
        }

        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[0]), REQUEST_CODE);
        } else {
            checkLocationSettingsAndStart();
        }
    }

    private void checkLocationSettingsAndStart() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!isGpsEnabled) {
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build();
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest).setAlwaysShow(true);
            SettingsClient client = LocationServices.getSettingsClient(this);
            Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

            task.addOnFailureListener(this, e -> {
                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTING);
                    } catch (IntentSender.SendIntentException ex) {
                        Log.e("MainActivity", "Error starting location settings resolution", ex);
                    }
                }
            });
            
            task.addOnSuccessListener(this, locationSettingsResponse -> startMonitoringService());
        } else {
            startMonitoringService();
        }
    }

    private void startMonitoringService() {
        Intent notificationIntent = new Intent(this, ServiceMine.class);
        notificationIntent.setAction("START");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(notificationIntent);
        } else {
            startService(notificationIntent);
        }
        Snackbar.make(findViewById(android.R.id.content), "Service Started!", Snackbar.LENGTH_LONG).show();
        isServiceActive = true;
        updateServiceStatus();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTING && resultCode == RESULT_OK) {
            startMonitoringService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            boolean allGranted = true;
            for (int res : grantResults) { if (res != PackageManager.PERMISSION_GRANTED) allGranted = false; }
            if (allGranted) {
                checkLocationSettingsAndStart();
            } else {
                showPermissionDeniedDialog();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("This app requires Location, SMS, and Phone permissions to protect you. Please enable them in settings.")
                .setPositiveButton("Settings", (dialog, which) -> openAppSettings())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void updateServiceStatus() {
        TypedValue containerColor = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, containerColor, true);
        TypedValue onContainerColor = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnPrimaryContainer, onContainerColor, true);
        TypedValue surfaceVariant = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceVariant, surfaceVariant, true);
        TypedValue onSurfaceVariant = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, onSurfaceVariant, true);

        if (isServiceActive) {
            statusCard.setCardBackgroundColor(ContextCompat.getColor(this, containerColor.resourceId));
            statusIcon.setImageResource(R.drawable.safety_check);
            statusIcon.setColorFilter(ContextCompat.getColor(this, onContainerColor.resourceId));
            statusText.setText("Service Active");
            statusText.setTextColor(ContextCompat.getColor(this, onContainerColor.resourceId));
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        } else {
            statusCard.setCardBackgroundColor(ContextCompat.getColor(this, surfaceVariant.resourceId));
            statusIcon.setImageResource(R.drawable.safety_check_off);
            statusIcon.setColorFilter(ContextCompat.getColor(this, onSurfaceVariant.resourceId));
            statusText.setText("Service Inactive");
            statusText.setTextColor(ContextCompat.getColor(this, onSurfaceVariant.resourceId));
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
