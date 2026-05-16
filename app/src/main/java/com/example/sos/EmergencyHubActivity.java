package com.example.sos;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class EmergencyHubActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private TextView currentLocationText;
    private MaterialButton btnHospitals, btnPolice, btnFire, btnGas, btnAmbulance, btnBloodBank, btnPharmacy, btnDisaster;
    private MaterialToolbar toolbar;
    private Location lastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_hub);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        currentLocationText = findViewById(R.id.currentLocationText);
        
        // Initialize Buttons
        btnHospitals = findViewById(R.id.btnHospitals);
        btnPolice = findViewById(R.id.btnPolice);
        btnFire = findViewById(R.id.btnFire);
        btnGas = findViewById(R.id.btnGas);
        btnAmbulance = findViewById(R.id.btnAmbulance);
        btnBloodBank = findViewById(R.id.btnBloodBank);
        btnPharmacy = findViewById(R.id.btnPharmacy);
        btnDisaster = findViewById(R.id.btnDisaster);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        getCurrentLocation();

        // Set Click Listeners
        btnHospitals.setOnClickListener(v -> searchNearby("hospitals"));
        btnPolice.setOnClickListener(v -> searchNearby("police stations"));
        btnFire.setOnClickListener(v -> searchNearby("fire station"));
        btnGas.setOnClickListener(v -> searchNearby("gas leak repair"));
        btnAmbulance.setOnClickListener(v -> searchNearby("ambulance service"));
        btnBloodBank.setOnClickListener(v -> searchNearby("blood bank"));
        btnPharmacy.setOnClickListener(v -> searchNearby("pharmacy"));
        btnDisaster.setOnClickListener(v -> searchNearby("disaster management office"));
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    lastLocation = location;
                    currentLocationText.setText("Lat: " + String.format("%.4f", location.getLatitude()) + 
                                               ", Lon: " + String.format("%.4f", location.getLongitude()));
                } else {
                    currentLocationText.setText("Location not available. Try turning on GPS.");
                }
            }
        });
    }

    private void searchNearby(String query) {
        Uri gmmIntentUri;
        if (lastLocation != null) {
            gmmIntentUri = Uri.parse("geo:" + lastLocation.getLatitude() + "," + lastLocation.getLongitude() + "?q=" + query);
        } else {
            gmmIntentUri = Uri.parse("geo:0,0?q=" + query);
        }
        
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Fallback if Google Maps app is not installed
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=" + query));
            startActivity(webIntent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}