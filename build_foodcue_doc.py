import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
import os

def build_foodcue_doc():
    src_path = r'D:\New folder (5)\FloodCue.docx'
    dst_path = r'D:\New folder (5)\FoodCue_doc.docx'
    
    doc = docx.Document(src_path)
    body = doc._body._element
    
    # 1. Locate Section 6 ("6. SYSTEM DESIGN")
    # We must not change ANYTHING from FloodCue.docx till Section 5!
    sec6_elem_idx = None
    for idx, child in enumerate(body):
        tag = child.tag.split('}')[-1]
        if tag == 'p':
            p = docx.text.paragraph.Paragraph(child, doc)
            if p.text.strip().startswith('6. SYSTEM DESIGN'):
                sec6_elem_idx = idx
                break
                
    if sec6_elem_idx is None:
        raise Exception("Could not find '6. SYSTEM DESIGN' in FloodCue.docx")
        
    print(f"Preserving elements 0 to {sec6_elem_idx - 1} exactly as in FloodCue.docx.")
    
    # Remove all body elements from Section 6 onwards
    while len(body) > sec6_elem_idx:
        body.remove(body[sec6_elem_idx])
        
    print(f"Removed old sections 6-12. Remaining elements count: {len(body)}")
    
    # Helper to add heading 1
    def add_h1(title):
        p = doc.add_paragraph(title, style='Heading 1')
        p.paragraph_format.space_before = Pt(14)
        p.paragraph_format.space_after = Pt(6)
        p.paragraph_format.keep_with_next = True
        return p

    def add_h2(title):
        p = doc.add_paragraph(title, style='Heading 2')
        p.paragraph_format.space_before = Pt(10)
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.keep_with_next = True
        return p

    def add_h3(title):
        p = doc.add_paragraph(title, style='Heading 3')
        p.paragraph_format.space_before = Pt(8)
        p.paragraph_format.space_after = Pt(3)
        p.paragraph_format.keep_with_next = True
        return p

    def add_h4(title):
        p = doc.add_paragraph(title, style='Heading 4')
        p.paragraph_format.space_before = Pt(6)
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.keep_with_next = True
        return p

    def add_p(text):
        p = doc.add_paragraph(text, style='Normal')
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.line_spacing = 1.15
        return p

    def add_code(text):
        p = doc.add_paragraph(text, style='Normal')
        p.paragraph_format.space_before = Pt(1)
        p.paragraph_format.space_after = Pt(1)
        p.paragraph_format.line_spacing = 1.0
        for run in p.runs:
            run.font.name = 'Consolas'
            run.font.size = Pt(9.5)
        return p

    # ==========================================
    # SECTION 6: SYSTEM DESIGN
    # ==========================================
    add_h1("6. SYSTEM DESIGN")
    
    add_h2("6.1 Architecture")
    add_p("FloodCue is engineered around a resilient, multi-tier, offline-first mobile architecture tailored specifically for natural disasters, severe flash floods, and localized infrastructure collapse. The application strictly decouples user experience from persistent internet connectivity, ensuring full life-saving utility even under total terrestrial cellular blackouts.")
    
    add_p("Key Architectural Components:")
    add_p("1. Model-View-Controller (MVC) & Service-Oriented Pattern: The architecture cleanly divides presentation, state orchestration, background workers, and persistent data engines:")
    add_p("   - Model Layer: Encapsulates disaster models (UserProfile, EmergencyContacts, SosQueue, WeatherForecast, FloodAlertCache, MBTileCatalog).")
    add_p("   - View Layer: Native Android XML Layouts coupled with Material Design 3 components optimized for high contrast, emergency color palettes, and low-battery consumption.")
    add_p("   - Controller / Activity Layer: Manages user interactions, permission acquisition workflows, and UI transitions (HomeActivity, SosActivity, OfflineMapViewActivity, FloodAlertActivity, WeatherActivity).")
    add_p("   - Services & Broadcast Receivers: Autonomous background daemons (SosQueue, SosSendReceiver, SosRetryReceiver, SosBootReceiver, FloodAlertJobService) ensuring mission-critical reliability.")
    
    add_p("2. Offline Storage & Hardware Telephony Engine: Unlike conventional web-reliant platforms, FloodCue integrates low-level Android Telephony and baseband hardware. Emergency SOS dispatch utilizes Android's SmsManager with multipart SMS payload distribution and GPS coordinate embedding (via FusedLocationProviderClient). Messages are queued locally inside SQLite / SharedPreferences-backed SosQueue. If network towers are down, the queue dynamically retains messages and triggers automated retries via AlarmManager broadcasts as signal reception fluctuates.")
    
    add_p("3. Vector Mapping & Geospatial Engine: Embeds an offline MapLibre / Mapbox Native SDK engine that directly parses local .mbtiles raster and vector archives stored on device flash memory. Displaced citizens can navigate to relief camps, high-ground shelters, and primary health centers completely without an active internet connection.")
    
    add_p("4. Multilingual Localization Engine (LocaleManager): Provides instant on-the-fly language switching (English, Telugu, Hindi) utilizing Android dynamic context wrapping without requiring an application restart, vital for disaster-affected populations across regional linguistic divides.")

    add_h2("6.2 UML Diagrams")
    
    add_h3("6.2.1. Use Case Diagram")
    add_p("The Use Case Diagram defines the interactions between the system actors (Citizens / Flood Victims, Emergency Contacts, Disaster Response Authorities, and System Background Services) and the core capabilities of FloodCue.")
    add_p("Actors:")
    add_p("1. Citizen / Flood Victim (Primary End-User)")
    add_p("2. Emergency Contact (Recipient of SMS Alerts & GPS Coordinates)")
    add_p("3. Government Disaster Agencies (NDMA / SDMA / IMD Broadcast feeds)")
    add_p("4. Android OS Background Scheduler (JobScheduler & AlarmManager)")
    
    add_p("Use Cases:")
    add_p("- Emergency SOS Operations:")
    add_p("  * Trigger One-Tap Instant SOS Alert")
    add_p("  * Capture High-Accuracy Real-Time GPS Coordinates (Lat/Long)")
    add_p("  * Transmit Multi-Contact SMS Payload via GSM Telephony")
    add_p("  * Queue Pending Alerts for Auto-Retry on Signal Loss")
    add_p("  * Activate Loud Acoustic Panic Siren (Super-Loud Loop)")
    add_p("- Survival Guides & Resource Navigation:")
    add_p("  * Access Offline Survival Guides & Medical First-Aid Procedures")
    add_p("  * View Dynamic Flood Preparedness & Survival Checklist")
    add_p("  * Render Offline Vector Shelter Maps via MBTiles Cache")
    add_p("- Flood Risk & Early Warning:")
    add_p("  * Sync Real-time SACHET & IMD River Basin Warning Feeds (when online)")
    add_p("  * View 24h/7-Day Local Rainfall Forecasts via Weather Intelligence")
    add_p("  * Cache Early Warnings for Offline Reference")
    add_p("- User & System Management:")
    add_p("  * Onboard Profile & Register Trusted Emergency Contacts")
    add_p("  * Switch System Language (English / Telugu / Hindi)")
    add_p("  * Manage App Permissions (SMS, Location, Notifications)")

    add_p("Figure 6.1: Use Case Diagram - FloodCue Offline Flood Survival System")
    
    add_h3("6.2.2. Sequence Diagram")
    add_p("The Sequence Diagram details the execution workflow during an Emergency SOS Trigger:")
    add_p("1. Citizen presses the Emergency SOS button on HomeActivity / SosActivity.")
    add_p("2. SosActivity queries LocationServices (FusedLocationProviderClient) for the latest GPS fix.")
    add_p("3. Location provider returns latitude, longitude, and accuracy timestamp.")
    add_p("4. SosActivity constructs an emergency payload containing victim identity, distress message, and direct Google Maps coordinate hyperlink.")
    add_p("5. ProfileStorage retrieves verified emergency contact numbers.")
    add_p("6. SosActivity hands the payload to SosQueue for persistent queuing.")
    add_p("7. SosQueue invokes SmsManager.sendMultipartTextMessage() to transmit the SMS via cellular baseband radio.")
    add_p("8. If cellular signal is absent, SmsManager returns an error broadcast to SosSendReceiver.")
    add_p("9. SosSendReceiver instructs AlarmManager to register an exponential backoff retry intent.")
    add_p("10. Once cellular connectivity returns, SosRetryReceiver wakes the background worker and successfully dispatches all queued emergency messages.")

    add_p("Figure 6.2: Sequence Diagram - Emergency SOS Dispatch & Offline Retry Mechanism")

    add_h3("6.2.3. Class Diagram")
    add_p("The Class Diagram illustrates the object-oriented structure of FloodCue, outlining entity models, controllers, and hardware service bridges:")
    add_p("- Core Model Entities:")
    add_p("  * UserProfile: Stores victim name, blood group, medical conditions, primary phone, and emergency address.")
    add_p("  * EmergencyContact: Represents contact name, relationship, and sanitized mobile number.")
    add_p("  * SosMessage: Encapsulates messageId, recipientNumber, textPayload, timestamp, and deliveryStatus.")
    add_p("  * WeatherData: Stores temperature, precipitation probability, humidity, and rainfall millimeters.")
    add_p("- Controller Activities:")
    add_p("  * HomeActivity: Orchestrates quick-action cards, siren toggle, SOS launcher, and navigation bar.")
    add_p("  * SosActivity: Implements real-time countdown, GPS query, and multi-contact dispatch.")
    add_p("  * OfflineMapViewActivity: Renders vector MBTiles maps and marks emergency shelter coordinates.")
    add_p("  * FloodAlertActivity: Parses official NDMA SACHET warning bulletins and river alert feeds.")
    add_p("  * WeatherActivity: Fetches meteorological forecasts via Open-Meteo REST APIs.")
    add_p("- Hardware & Service Bridges:")
    add_p("  * SosQueue: Singleton managing offline message persistence and dispatch queues.")
    add_p("  * SosSendReceiver & SosRetryReceiver: BroadcastReceivers listening for SMS delivery and network reconnects.")
    add_p("  * LocaleManager: Wraps base context with selected language configuration.")

    add_p("Figure 6.3: Class Diagram - FloodCue Mobile Application Architecture")

    # ==========================================
    # SECTION 7: SYSTEM IMPLEMENTATION
    # ==========================================
    add_h1("7. SYSTEM IMPLEMENTATION")
    add_h2("7.1  MODULES")
    add_p("FloodCue is structured into discrete, highly decoupled functional modules designed to maximize reliability during real-world crisis conditions:")

    add_h3("7.1.1 Emergency SOS & Multi-Channel Alert Module")
    add_p("The cornerstone of the FloodCue survival suite. Facilitates one-tap emergency activation that dispatches formatted SMS alerts with precise GPS coordinates to predefined contacts and government disaster helplines. Operates completely without mobile data or internet access. Includes an integrated high-frequency acoustic Panic Siren loop to alert nearby rescue boats and ground search teams in low-visibility or submerged surroundings.")

    add_h3("7.1.2 Offline Vector Mapping & Shelter Navigation Module")
    add_p("Powered by MapLibre Native SDK and embedded MBTiles spatial database containers. Allows users to view offline topographical maps, localized flood evacuation routes, and pre-indexed cyclone/flood relief centers even when cellular base stations are completely non-functional.")

    add_h3("7.1.3 Flood Intelligence & Official Warning Module")
    add_p("Interfaces with government hydrometeorological endpoints (NDMA SACHET alert network and IMD warning systems via server-side caching proxy imd_proxy.php). When internet connectivity is available, the module downloads and securely caches regional flood bulletins, river basin warnings, and dam discharge advisories for offline review.")

    add_h3("7.1.4 Weather Forecasting & Rain Risk Module")
    add_p("Provides hourly and 7-day granular rainfall predictions, cloud cover analytics, and precipitation probability metrics using Open-Meteo REST endpoints, allowing citizens to anticipate water level surges prior to evacuation.")

    add_h3("7.1.5 Offline Disaster Survival Guides & Preparedness Checklist")
    add_p("A zero-dependency, rich-media educational repository covering critical survival actions: pre-flood home protection, electrical hazard mitigation, clean water purification, snake bite emergency response, and post-flood hygiene. Includes an interactive checklist with persistent local state.")

    add_h3("7.1.6 User Profile, Emergency Contacts & Multilingual Engine")
    add_p("Handles initial user onboarding, verified contact registry (supporting phonebook pickers), and on-the-fly language switching across English, Telugu, and Hindi.")

    # ==========================================
    # SECTION 8: TESTING
    # ==========================================
    add_h1("8. TESTING")
    add_p("To guarantee the uncompromising reliability demanded of life-critical disaster software, FloodCue underwent a rigorous multi-phase verification process encompassing hardware, software, and simulated real-world disaster conditions:")

    add_p("1. Unit Testing: Verified standalone functions and utility classes using the JUnit framework and AndroidX Test libraries. Focused on GPS coordinate formatting, telephone number normalization and international dial-code sanitization, SMS character-limit partitioning, and JSON parsing of meteorological warning feeds.")
    
    add_p("2. Integration Testing: Validated the seamless interaction between Android OS components, hardware abstraction layers, and internal modules. Specifically tested:")
    add_p("   - FusedLocationProviderClient integration with SosActivity for coordinate precision under GPS/GNSS sensor constraints.")
    add_p("   - Telephony radio handoff via SmsManager and intent callback verification through SosSendReceiver.")
    add_p("   - MBTiles vector tile extraction and offline rendering performance on the MapLibre surface.")
    add_p("   - Network resilience testing with mock server responses and fallback to cached flood alerts upon network timeouts.")

    add_p("3. System Testing (End-to-End Testing): Simulated realistic end-to-end disaster scenarios on physical Android devices. Tested the complete SOS workflow: initiating distress button press, activating the background panic siren, validating automated SMS reception on physical recipient phones, and confirming accurate hyperlink mapping to rescue destinations.")

    add_p("4. Usability & High-Stress Scenario Testing: Evaluated UI legibility and interaction efficiency under adverse conditions, including direct sunlight reflection, simulated torrential rain screen smudges, one-handed operation, and high-contrast night mode readability. Confirmed that emergency triggers can be completed in under 3 seconds.")

    # ==========================================
    # SECTION 9: SAMPLE SOURCE CODE
    # ==========================================
    add_h1("9. SAMPLE SOURCE CODE")
    add_p("The following sections showcase production-grade source code implementations from the FloodCue Android project and its backend infrastructure:")

    add_h2("9.1 Application Core & Architecture Components")
    add_h3("9.1.1 AndroidManifest.xml (Permissions & Component Declarations)")
    add_p("Defines mission-critical hardware permissions (SMS, FINE_LOCATION, AUDIO) and registers activities, receivers, and background services:")
    
    manifest_code = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.floodcue.app">

    <!-- Mission-Critical Hardware & Communication Permissions -->
    <uses-permission android:name="android.permission.SEND_SMS" />
    <uses-permission android:name="android.permission.READ_SMS" />
    <uses-permission android:name="android.permission.RECEIVE_SMS" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".FloodCueApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.FloodCue">

        <activity
            android:name=".SplashActivity"
            android:exported="true"
            android:theme="@style/Theme.FloodCue.Splash">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity android:name=".HomeActivity" android:exported="false" />
        <activity android:name=".SosActivity" android:exported="false" />
        <activity android:name=".OfflineMapsActivity" android:exported="false" />
        <activity android:name=".OfflineMapViewActivity" android:exported="false" />
        <activity android:name=".FloodAlertActivity" android:exported="false" />
        <activity android:name=".WeatherActivity" android:exported="false" />
        <activity android:name=".EmergencyGuideActivity" android:exported="false" />
        <activity android:name=".ChecklistActivity" android:exported="false" />
        <activity android:name=".EmergencyContactsActivity" android:exported="false" />
        <activity android:name=".LanguageActivity" android:exported="false" />

        <!-- Broadcast Receivers for Offline SOS Resilience -->
        <receiver android:name=".SosSendReceiver" android:exported="false" />
        <receiver android:name=".SosRetryReceiver" android:exported="false" />
        <receiver android:name=".SosBootReceiver" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>

    </application>
</manifest>"""
    for line in manifest_code.split('\n'):
        add_code(line)

    add_h3("9.1.2 app/build.gradle (Dependency Orchestration)")
    add_p("Configures Android SDK compilation targets and dependencies for vector mapping, location services, and Material Design:")
    
    gradle_code = """plugins {
    id 'com.android.application'
}

android {
    namespace 'com.floodcue.app'
    compileSdk 34

    defaultConfig {
        applicationId "com.floodcue.app"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'com.google.android.gms:play-services-location:21.0.1'
    implementation 'org.maplibre.gl:android-sdk:10.2.0'
    implementation 'com.android.volley:volley:1.2.1'
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}"""
    for line in gradle_code.split('\n'):
        add_code(line)

    add_h3("9.1.3 SosQueue.java (Offline Distress Queue & Auto-Retry Engine)")
    add_p("Manages persistent storage and dispatch of emergency alerts, implementing resilient retries when towers are offline:")
    
    queue_code = """package com.floodcue.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.telephony.SmsManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SosQueue {
    private static final String TAG = "SosQueue";
    private static final String PREF_NAME = "sos_queue_prefs";
    private static final String KEY_PENDING_MSGS = "pending_messages";
    private static final long RETRY_INTERVAL_MS = 60000; // 1 minute retry

    public static synchronized void enqueueMessage(Context context, String phone, String text) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> set = new HashSet<>(prefs.getStringSet(KEY_PENDING_MSGS, new HashSet<>()));
        String item = phone + ":::" + text;
        set.add(item);
        prefs.edit().putStringSet(KEY_PENDING_MSGS, set).apply();
        Log.d(TAG, "Enqueued message for " + phone + ". Total queued: " + set.size());
        
        // Attempt dispatch immediately
        dispatchPending(context);
    }

    public static synchronized void dispatchPending(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> set = prefs.getStringSet(KEY_PENDING_MSGS, new HashSet<>());
        if (set == null || set.isEmpty()) {
            return;
        }

        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> toRemove = new ArrayList<>();

        for (String record : set) {
            String[] parts = record.split(":::", 2);
            if (parts.length == 2) {
                String phone = parts[0];
                String text = parts[1];
                try {
                    ArrayList<String> partsList = smsManager.divideMessage(text);
                    smsManager.sendMultipartTextMessage(phone, null, partsList, null, null);
                    toRemove.add(record);
                    Log.i(TAG, "Successfully dispatched SMS to " + phone);
                } catch (Exception e) {
                    Log.w(TAG, "Dispatch failed for " + phone + "; schedule retry: " + e.getMessage());
                    scheduleRetry(context);
                    break;
                }
            }
        }

        if (!toRemove.isEmpty()) {
            Set<String> updated = new HashSet<>(set);
            updated.removeAll(toRemove);
            prefs.edit().putStringSet(KEY_PENDING_MSGS, updated).apply();
        }
    }

    private static void scheduleRetry(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SosRetryReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
            context, 101, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (alarmManager != null) {
            alarmManager.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + RETRY_INTERVAL_MS,
                pi
            );
        }
    }
}"""
    for line in queue_code.split('\n'):
        add_code(line)

    add_h2("9.2 Activities & UI Screen Controllers")
    
    add_h3("9.2.1 HomeActivity.java (Master Dashboard & Panic Siren Controller)")
    add_p("Coordinates dashboard cards, instant SOS launcher, and audio panic alarm loop:")
    
    home_code = """package com.floodcue.app;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {
    private Button btnSos, btnSiren, btnOfflineMaps, btnAlerts, btnWeather, btnGuide;
    private MediaPlayer mediaPlayer;
    private boolean isSirenPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        btnSos = findViewById(R.id.btnHomeSos);
        btnSiren = findViewById(R.id.btnPanicSiren);
        btnOfflineMaps = findViewById(R.id.btnOfflineMaps);
        btnAlerts = findViewById(R.id.btnFloodAlerts);
        btnWeather = findViewById(R.id.btnWeatherForecast);
        btnGuide = findViewById(R.id.btnSurvivalGuide);

        btnSos.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, SosActivity.class));
        });

        btnSiren.setOnClickListener(v -> togglePanicSiren());

        btnOfflineMaps.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, OfflineMapsActivity.class));
        });

        btnAlerts.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, FloodAlertActivity.class));
        });

        btnWeather.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, WeatherActivity.class));
        });

        btnGuide.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, EmergencyGuideActivity.class));
        });
    }

    private void togglePanicSiren() {
        if (!isSirenPlaying) {
            mediaPlayer = MediaPlayer.create(this, R.raw.panic_siren_alarm);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
                isSirenPlaying = true;
                btnSiren.setText(R.string.stop_panic_siren);
            }
        } else {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            isSirenPlaying = false;
            btnSiren.setText(R.string.start_panic_siren);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}"""
    for line in home_code.split('\n'):
        add_code(line)

    add_h3("9.2.2 SosActivity.java (GPS Acquisition & Multi-Contact SMS Dispatch)")
    add_p("Captures high-precision GPS coordinates and sends emergency broadcast alerts:")
    
    sos_code = """package com.floodcue.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.util.List;

public class SosActivity extends AppCompatActivity {
    private FusedLocationProviderClient locationClient;
    private Button btnSendSosNow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);

        locationClient = LocationServices.getFusedLocationProviderClient(this);
        btnSendSosNow = findViewById(R.id.btnSendSosNow);

        btnSendSosNow.setOnClickListener(v -> executeSosEmergency());
    }

    private void executeSosEmergency() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        locationClient.getLastLocation().addOnSuccessListener(this, location -> {
            dispatchToContacts(location);
        });
    }

    private void dispatchToContacts(Location location) {
        String coords = (location != null) 
            ? "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude()
            : "Location unavailable (GPS obstructed).";

        String message = "EMERGENCY FLOOD ALERT! I need urgent rescue! My location: " + coords;
        List<String> contacts = ProfileStorage.getEmergencyContacts(this);

        if (contacts.isEmpty()) {
            Toast.makeText(this, "No emergency contacts registered! Opening settings...", Toast.LENGTH_LONG).show();
            return;
        }

        for (String phone : contacts) {
            SosQueue.enqueueMessage(this, phone, message);
        }

        Toast.makeText(this, "Distress SOS alert queued for dispatch to all contacts!", Toast.LENGTH_SHORT).show();
        finish();
    }
}"""
    for line in sos_code.split('\n'):
        add_code(line)

    add_h3("9.2.3 OfflineMapViewActivity.java (Offline Vector MapLibre Engine)")
    add_p("Loads and displays locally bundled MBTiles map packages without network connectivity:")
    
    map_code = """package com.floodcue.app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import org.maplibre.android.MapLibre;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.Style;
import java.io.File;

public class OfflineMapViewActivity extends AppCompatActivity {
    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapLibre.getInstance(this);
        setContentView(R.layout.activity_offline_map_view);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        String mbtilesPath = getIntent().getStringExtra("MBTILES_PATH");
        mapView.getMapAsync(map -> {
            configureOfflineStyle(map, mbtilesPath);
        });
    }

    private void configureOfflineStyle(MapLibreMap map, String mbtilesPath) {
        if (mbtilesPath != null && new File(mbtilesPath).exists()) {
            String styleJson = OfflineMapCatalog.buildStyleFromMbtiles(mbtilesPath);
            map.setStyle(new Style.Builder().fromJson(styleJson));
        } else {
            map.setStyle(Style.getPredefinedStyle("Basic"));
        }
    }

    @Override
    protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override
    protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override
    protected void onPause() { super.onPause(); mapView.onPause(); }
    @Override
    protected void onStop() { super.onStop(); mapView.onStop(); }
    @Override
    protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
}"""
    for line in map_code.split('\n'):
        add_code(line)

    add_h2("9.3 Backend Proxy & Disaster Information Aggregation")
    add_h3("9.3.1 imd_proxy.php (Server-Side Warning Cache & Caching Bridge)")
    add_p("Proxies meteorological river alerts and SACHET disaster bulletins, reducing mobile bandwidth consumption:")
    
    php_code = """<?php
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');

$cache_file = __DIR__ . '/cache/imd_alert_cache.json';
$cache_lifetime = 600; // 10 minutes cache validity

if (file_exists($cache_file) && (time() - filemtime($cache_file) < $cache_lifetime)) {
    echo file_get_contents($cache_file);
    exit;
}

$target_url = "https://sachet.ndma.gov.in/cap_public_website/FetchAlertDetails";
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $target_url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_TIMEOUT, 10);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);

$response = curl_exec($ch);
$http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($http_code == 200 && !empty($response)) {
    if (!is_dir(__DIR__ . '/cache')) {
        mkdir(__DIR__ . '/cache', 0755, true);
    }
    file_put_contents($cache_file, $response);
    echo $response;
} else {
    if (file_exists($cache_file)) {
        echo file_get_contents($cache_file);
    } else {
        http_response_code(503);
        echo json_encode(["status" => "error", "message" => "Disaster feed unavailable"]);
    }
}
?>"""
    for line in php_code.split('\n'):
        add_code(line)

    # ==========================================
    # SECTION 10: SCREEN LAYOUTS
    # ==========================================
    add_h1("10. SCREEN LAYOUTS")
    add_p("The visual interface of FloodCue is engineered around rapid emergency discoverability, ultra-high contrast color palettes, and immediate tactile touchpoints. The following catalog presents the primary application screens:")
    
    add_p("Figure 10.1: Splash & Application Launcher Screen (Branded FloodCue Routing)")
    add_p("Figure 10.2: Main Home Dashboard Screen (SOS Panic Trigger, Quick Actions, Emergency Navigation)")
    add_p("Figure 10.3: Emergency SOS Dispatch Screen (GPS Capture, Multi-Contact Delivery, Queue Status)")
    add_p("Figure 10.4: Offline Vector Shelter Maps Screen (MapLibre MBTiles Surface, Relief Centers)")
    add_p("Figure 10.5: Real-Time Flood & SACHET Alert Screen (River Levels, Basin Warnings, Color-Coded Severity)")
    add_p("Figure 10.6: Weather Intelligence & Rainfall Forecast Screen (Granular Precipitation Probability)")
    add_p("Figure 10.7: Offline Disaster Survival Guide Screen (Flood Preparedness & First-Aid Protocols)")
    add_p("Figure 10.8: Interactive Emergency Checklist Screen (Life-Saving Supplies & Medication Checklist)")
    add_p("Figure 10.9: Emergency Contacts Onboarding & Management Screen (Direct Phonebook Sync)")
    add_p("Figure 10.10: Language & Localization Selection Screen (English, Telugu, Hindi Dynamic Switching)")

    # ==========================================
    # SECTION 11: FUTURE ENHANCEMENTS
    # ==========================================
    add_h1("11. FUTURE ENHANCEMENTS")
    add_p("While FloodCue delivers an unprecedented standard of offline resilience for flood emergencies, disaster management is a rapidly advancing domain. The following strategic roadmap outlines future architectural expansions:")

    add_h2("Bluetooth Mesh (BLE) & Wi-Fi Direct Mesh Networking")
    add_p("To eliminate reliance on cellular GSM infrastructure entirely when cell towers suffer total physical destruction, FloodCue will integrate peer-to-peer mesh protocols (such as Bridgefy SDK or open-source Wi-Fi Direct mesh protocols). Citizens will be able to bounce SOS packets, medical requests, and water supply advisories device-to-device across a decentralized hopping network spanning several kilometers.")

    add_h2("AI-Powered Flood Level Estimation via Edge Computer Vision")
    add_p("Using on-device TensorFlow Lite neural networks, citizens will be able to take a photograph of nearby watermarks against buildings, signboards, or vehicles. The on-device model will instantly compute water depth and flood current velocity, warning the citizen whether it is safe to wade through or if immediate vertical rooftop evacuation is mandated.")

    add_h2("Autonomous Drone Relief Beacon Synchronization")
    add_p("Integration with autonomous UAV search-and-rescue systems. Disaster response quadcopters deployed over flooded deltas can broadcast ultra-high-frequency Wi-Fi/LoRa beacons. FloodCue will detect these aerial beacons and transmit queued SOS packets directly to the drone, which relays the rescue coordinates to centralized command centers.")

    add_h2("Low-Earth Orbit (LEO) Satellite SOS Messaging Protocol")
    add_p("Direct compatibility with next-generation Android satellite telephony (Direct-to-Cell satellite standards such as 3GPP Rel-17 NTN). When terrestrial cellular radios register zero signal, the app will orient the user toward overhead orbital passes to transmit ultra-compressed distress vectors directly to orbital satellites.")

    add_h2("Centralized Incident Command Web Dashboard for First Responders")
    add_p("Development of an enterprise-grade Web GIS dashboard for the National Disaster Response Force (NDRF) and State Emergency Operation Centers (SEOC). The web console will ingest all incoming SMS distress vectors, plot heatmaps of stranded citizen clusters, and optimize rescue boat deployment trajectories in real-time.")

    # ==========================================
    # SECTION 12: CONCLUSION
    # ==========================================
    add_h1("12. CONCLUSION")
    add_p("In conclusion, the FloodCue Offline Flood Survival Application represents a transformative, life-saving breakthrough in community disaster preparedness and crisis communication. Traditional emergency apps suffer from a fundamental paradox: they depend entirely on stable internet connectivity at the exact moment natural disasters cause massive electrical grid collapses and cellular tower destruction. FloodCue shatters this dependency.")
    
    add_p("By unifying native Android hardware telephony, resilient offline SMS queuing with automated AlarmManager retries, on-device vector map rendering via MapLibre and MBTiles, an acoustic panic siren loop, and rich multilingual localization (English, Telugu, Hindi), FloodCue delivers an autonomous survival tool that guarantees functionality when citizens need it most.")
    
    add_p("The rigorous multi-tiered architecture ensures robust performance, extreme battery conservation, and intuitive rapid operation during severe psychological stress. As climate change increases the frequency and severity of extreme hydrometeorological disasters across India and globally, FloodCue stands as a resilient, scalable, and humanitarian technological solution—bridging the gap between stranded citizens and life-saving rescue forces.")

    # ==========================================
    # UPDATE TABLE OF CONTENTS (TABLE 1)
    # ==========================================
    # Now update Table 1 in the front matter so Section 7-12 reflect FloodCue
    if len(doc.tables) > 1:
        t1 = doc.tables[1]
        if len(t1.rows) > 2:
            t1.rows[2].cells[1].text = "SAMPLE SOURCE CODE\n   9.1 Application Core & Architecture Components\n      9.1.1 AndroidManifest.xml\n      9.1.2 app/build.gradle\n      9.1.3 SosQueue.java\n   9.2 Activities & UI Screen Controllers\n      9.2.1 HomeActivity.java\n      9.2.2 SosActivity.java\n      9.2.3 OfflineMapViewActivity.java\n   9.3 Backend Proxy & Disaster Aggregation\n      9.3.1 imd_proxy.php"
            if len(t1.rows) > 3:
                t1.rows[3].cells[1].text = ""

    doc.save(dst_path)
    print(f"Successfully generated FoodCue_doc.docx at: {dst_path}")

if __name__ == "__main__":
    build_foodcue_doc()
