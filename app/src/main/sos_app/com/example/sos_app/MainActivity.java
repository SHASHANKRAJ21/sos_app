package com.example.sos_app;
import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "MainActivity";  // For logging
    private final List<String> contactList = new ArrayList<>();
    private int currentContactIndex = 0;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    private LocationManager locationManager;
    private String currentLocation = "Location not available";
    private String locationLink = "";
    private TextView statusText;
    private boolean isCallActive = false;
    private final Handler handler = new Handler(); // For delaying the next call

    // MediaRecorder setup
    private MediaRecorder mediaRecorder;
    private String audioFilePath = null;
    private boolean isLocationAvailable = false;  // Track if location is available

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button sosButton = findViewById(R.id.sosButton);
        Button settingsButton = findViewById(R.id.settingsButton);
        statusText = findViewById(R.id.statusText);

        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        phoneStateListener = new CustomPhoneStateListener();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Request location updates
        if (checkPermissions()) {
            requestLocationUpdates();
        } else {
            requestPermissions();
        }

        // Email setup
        String email = "aryantomar.tomar12@gmail.com"; // Your Gmail
        String password = "byhqcpowryoxaxtq"; // Your Gmail app-specific password
        MailSender mailSender = new MailSender(email, password);

        sosButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                loadContacts();
                currentContactIndex = 0;
                startRecordingAudio();  // Start recording audio
                waitForLocationAndSendSOS(mailSender);  // Send SOS message only after location is available
                callNextContact();  // Start the sequential calling process
            } else {
                requestPermissions();
            }
        });

        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    // Method to start recording audio
    private void startRecordingAudio() {
        try {
            File audioFile = new File(getExternalFilesDir(null), "sos_audio.3gp");
            audioFilePath = audioFile.getAbsolutePath();

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioFilePath);

            mediaRecorder.prepare();
            mediaRecorder.start();

            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to stop recording audio
    private void stopRecordingAudio() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.SEND_SMS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.RECORD_AUDIO
        }, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdates();
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadContacts() {
        SharedPreferences prefs = getSharedPreferences("contacts_prefs", MODE_PRIVATE);
        Set<String> savedContacts = prefs.getStringSet("contacts_key", new HashSet<>());
        contactList.clear();
        contactList.addAll(savedContacts);
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Request both GPS and Network location updates for fallback
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 5, locationListener);
    }

    // Location listener to handle updates from both providers
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            currentLocation = "Lat: " + latitude + ", Lon: " + longitude;
            locationLink = "http://maps.google.com/maps?q=" + latitude + "," + longitude;
            statusText.setText(getString(R.string.current_location, currentLocation));
            isLocationAvailable = true;  // Location is now available
            Log.d(TAG, "Location updated: " + currentLocation);  // Log the location
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {
            Toast.makeText(MainActivity.this, "Location provider disabled", Toast.LENGTH_SHORT).show();
        }
    };

    // Wait for location to be available before sending SOS
    private void waitForLocationAndSendSOS(MailSender mailSender) {
        if (!isLocationAvailable) {
            Log.d(TAG, "Waiting for location...");
            handler.postDelayed(() -> waitForLocationAndSendSOS(mailSender), 1000);  // Check every second
        } else {
            sendSOSMessage(mailSender);  // Send SOS after location is available
        }
    }

    private void sendSOSMessage(MailSender mailSender) {
        SmsManager smsManager = SmsManager.getDefault();
        String message = "SOS! I need help. My location is: " + currentLocation + "\n" + locationLink;

        for (String contact : contactList) {
            String[] contactDetails = contact.split(": ");
            smsManager.sendTextMessage(contactDetails[1], null, message, null, null);
        }

        // Send email after recording finishes
        String recipient = "shashank286raj@gmail.com";  // Emergency contact's email
        String subject = "SOS Alert!";
        String messageBody = "SOS! I need help. My location is: " + currentLocation + "\n" + locationLink;
        handler.postDelayed(() -> {
            stopRecordingAudio();  // Stop recording after delay
            new SendMailTask(recipient, subject, messageBody, audioFilePath, mailSender).execute();
        }, 50000);  // Record for 50 seconds and then send the email
    }

    private void callNextContact() {
        if (currentContactIndex < contactList.size() && !isCallActive) {
            String contact = contactList.get(currentContactIndex);
            String[] contactDetails = contact.split(": ");
            String phoneNumber = contactDetails[1];

            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                startActivity(callIntent);
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
                isCallActive = true;
            }
        }
    }

    private class CustomPhoneStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, @NonNull String phoneNumber) {
            super.onCallStateChanged(state, phoneNumber);

            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    // Call is ringing, do nothing
                    break;

                case TelephonyManager.CALL_STATE_OFFHOOK:
                    // Call is ongoing, wait for it to end
                    isCallActive = true;
                    break;

                case TelephonyManager.CALL_STATE_IDLE:
                    // Call ended, proceed to the next contact after a delay
                    isCallActive = false;
                    currentContactIndex++;
                    handler.postDelayed(() -> {
                        if (currentContactIndex < contactList.size()) {
                            callNextContact();  // Call next contact after 3 seconds
                        } else {
                            Log.d("MainActivity", "All contacts have been called.");
                        }
                    }, 3000);
                    break;

                default:
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        // Remove location updates when app is destroyed
        locationManager.removeUpdates(locationListener);
    }
}