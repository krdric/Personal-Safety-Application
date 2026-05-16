package com.example.sos;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.content.IntentSender;
import android.location.LocationManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.color.DynamicColors;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity implements SensorEventListener {

    CardView registerContact, editMessage, sosguid, helpline, showContact, Info, btnSosService, fakeCall, emergencyRooms;
    FusedLocationProviderClient fusedLocationClient;
    DatabaseHelper db;
    String myLocation = "";
    
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTime;
    private static final int SHAKE_THRESHOLD = 20;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivitiesIfAvailable(getApplication());
        setContentView(R.layout.activity_home);
        registerContact = findViewById(R.id.registerContact);
        editMessage = findViewById(R.id.editMessage);
        btnSosService = findViewById(R.id.btnSosService);
        sosguid = findViewById(R.id.sosguid);
        helpline = findViewById(R.id.helpline);
        Info = findViewById(R.id.Info);
        showContact = findViewById(R.id.showContact);
        fakeCall = findViewById(R.id.fakeCall);
        emergencyRooms = findViewById(R.id.emergencyRooms);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = new DatabaseHelper(this);

        btnSosService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendSosMessage();
            }
        });

        fakeCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(HomeActivity.this, "Fake call will start in 5 seconds", Toast.LENGTH_SHORT).show();
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(HomeActivity.this, FakeCallActivity.class);
                        startActivity(intent);
                    }
                }, 5000); // 5 seconds delay
            }
        });

        registerContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, RegisterNumberActivity.class);
                startActivity(intent);
            }
        });

        editMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, EditMessageActivity.class);
                startActivity(intent);
            }
        });

        sosguid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeActivity.this, Guide.class);
                startActivity(intent);
            }
        });

        helpline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeActivity.this, SosCall.class);
                startActivity(intent);
            }
        });

        Info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeActivity.this, Instructions.class);
                startActivity(intent);
            }
        });

        showContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeActivity.this, ShowContact.class);
                startActivity(intent);
            }
        });

        emergencyRooms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, EmergencyHubActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            double acceleration = Math.sqrt(x * x + y * y + z * z);
            long currentTime = System.currentTimeMillis();

            if (acceleration > SHAKE_THRESHOLD) {
                if (currentTime - lastShakeTime > 5000) { // 5-second debouncing
                    lastShakeTime = currentTime;
                    if (vibrator != null) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vibrator.vibrate(500);
                        }
                    }
                    Toast.makeText(this, "Shake detected! Sending SOS...", Toast.LENGTH_SHORT).show();
                    sendSosMessage();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendSosMessage();
            } else {
                Toast.makeText(this, "Permissions are required for SOS to work!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void sendSosMessage() {
        if (db.count() == 0) {
            Toast.makeText(this, "Please register at least one contact first!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, RegisterNumberActivity.class));
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS}, 100);
            return;
        }

        // Check if location is enabled
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGpsEnabled && !isNetworkEnabled) {
            enableLocationSettings();
            return;
        }

        fetchLocationAndSendSms();
    }

    private void enableLocationSettings() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(HomeActivity.this, 1001);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            if (resultCode == RESULT_OK) {
                // User enabled location, try sending SOS again
                fetchLocationAndSendSms();
            } else {
                Toast.makeText(this, "Location is required for SOS accuracy", Toast.LENGTH_SHORT).show();
                // Send anyway with whatever we have or fallback
                fetchLocationAndSendSms();
            }
        }
    }

    private void fetchLocationAndSendSms() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Fetching location...", Toast.LENGTH_SHORT).show();

        // Try getting last known location immediately as a quick fallback
        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location lastKnown) {
                // Now try to get a fresh current location
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener(new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location freshLocation) {
                                if (freshLocation != null) {
                                    myLocation = "https://maps.google.com/maps?q=" + freshLocation.getLatitude() + "," + freshLocation.getLongitude();
                                } else if (lastKnown != null) {
                                    myLocation = "https://maps.google.com/maps?q=" + lastKnown.getLatitude() + "," + lastKnown.getLongitude() + " (Last Known)";
                                } else {
                                    myLocation = "Location not available. Please ensure GPS is ON and you are in an open area.";
                                }
                                dispatchSms(myLocation);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                if (lastKnown != null) {
                                    myLocation = "https://maps.google.com/maps?q=" + lastKnown.getLatitude() + "," + lastKnown.getLongitude() + " (Last Known)";
                                } else {
                                    myLocation = "Location error: " + e.getMessage();
                                }
                                dispatchSms(myLocation);
                            }
                        });
            }
        });
    }

    // Remove the unused requestFreshLocation method to keep code clean if it's not needed after the improved logic above


    private void dispatchSms(String locationStr) {
        ArrayList<ContactModel> list = db.fetchData();
        SharedPreferences sp = getSharedPreferences("message", MODE_PRIVATE);
        String customMsg = sp.getString("msg", null);
        SmsManager smsManager = SmsManager.getDefault();

        for (ContactModel contact : list) {
            String message;
            if (customMsg != null && !customMsg.isEmpty()) {
                message = "Emergency! " + customMsg + "\nMy Location: " + locationStr;
            } else {
                message = "I am in DANGER! I need help immediately. My Location: " + locationStr;
            }

            try {
                smsManager.sendTextMessage(contact.getNumber(), null, message, null, null);
                Toast.makeText(this, "SOS Message sent to " + contact.getName(), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to send SMS to " + contact.getName(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
