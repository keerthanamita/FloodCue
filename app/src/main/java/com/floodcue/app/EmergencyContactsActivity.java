package com.floodcue.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EmergencyContactsActivity extends FloodCueActivity {

    private static final int CALL_PHONE_PERMISSION_REQUEST = 1001;

    private String pendingPhoneNumber;

    private static final EmergencyService[] EMERGENCY_SERVICES = {
            new EmergencyService(
                    R.string.emergency_service_national,
                    "112",
                    R.string.emergency_service_national_desc
            ),
            new EmergencyService(
                    R.string.emergency_service_police,
                    "100",
                    R.string.emergency_service_police_desc
            ),
            new EmergencyService(
                    R.string.emergency_service_fire,
                    "101",
                    R.string.emergency_service_fire_desc
            ),
            new EmergencyService(
                    R.string.emergency_service_ambulance,
                    "108",
                    R.string.emergency_service_ambulance_desc
            ),
            new EmergencyService(
                    R.string.emergency_service_ndrf,
                    "9711077372",
                    R.string.emergency_service_ndrf_desc
            ),
            new EmergencyService(
                    R.string.emergency_service_calamity,
                    "1070",
                    R.string.emergency_service_calamity_desc
            ),
            new EmergencyService(
                    R.string.emergency_service_highway,
                    "1033",
                    R.string.emergency_service_highway_desc
            ),
            new EmergencyService(
                    R.string.emergency_service_child,
                    "1098",
                    R.string.emergency_service_child_desc
            )
    };

    private LinearLayout personalContactsList;
    private TextView personalContactsEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contacts);

        TextView buttonBackToHome = findViewById(R.id.buttonBackToHome);
        buttonBackToHome.setOnClickListener(v -> finish());

        personalContactsList = findViewById(R.id.personalContactsList);
        personalContactsEmpty = findViewById(R.id.personalContactsEmpty);

        bindEmergencyServices();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindPersonalContacts();
    }

    private void bindPersonalContacts() {
        personalContactsList.removeAllViews();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showPersonalEmpty(getString(R.string.emergency_contacts_login_required));
            return;
        }

        UserProfile profile = ProfileStorage.getProfile(this, user.getUid());
        if (profile == null) {
            showPersonalEmpty(getString(R.string.emergency_contacts_personal_empty));
            return;
        }

        int added = 0;
        added += addPersonalContactIfPresent(
                profile.primary_contact_name,
                profile.primary_contact_relationship,
                profile.primary_contact_phone,
                profile.primary_contact_alt_phone
        );
        added += addPersonalContactIfPresent(
                profile.second_contact_name,
                profile.second_contact_relationship,
                profile.second_contact_phone,
                profile.second_contact_alt_phone
        );
        added += addPersonalContactIfPresent(
                profile.third_contact_name,
                profile.third_contact_relationship,
                profile.third_contact_phone,
                profile.third_contact_alt_phone
        );

        if (added == 0) {
            showPersonalEmpty(getString(R.string.emergency_contacts_personal_empty));
            return;
        }

        personalContactsEmpty.setVisibility(View.GONE);
        personalContactsList.setVisibility(View.VISIBLE);
    }

    private int addPersonalContactIfPresent(
            String name,
            String relationship,
            String phone,
            String alternatePhone
    ) {
        if (isBlank(name) || isBlank(phone)) {
            return 0;
        }

        StringBuilder details = new StringBuilder();
        if (!isBlank(relationship)) {
            details.append(relationship).append("\n");
        }
        details.append(phone);
        if (!isBlank(alternatePhone)) {
            details.append("\n")
                    .append(getString(R.string.emergency_contacts_alternate, alternatePhone));
        }

        addCallRow(personalContactsList, name, details.toString(), phone);
        return 1;
    }

    private void showPersonalEmpty(String message) {
        personalContactsList.setVisibility(View.GONE);
        personalContactsEmpty.setText(message);
        personalContactsEmpty.setVisibility(View.VISIBLE);
    }

    private void bindEmergencyServices() {
        LinearLayout servicesList = findViewById(R.id.emergencyServicesList);
        servicesList.removeAllViews();

        for (EmergencyService service : EMERGENCY_SERVICES) {
            String details = service.number + " • " + getString(service.descriptionRes);
            addCallRow(servicesList, getString(service.titleRes), details, service.number);
        }
    }

    private void addCallRow(
            LinearLayout container,
            String name,
            String details,
            String phoneNumber
    ) {
        if (container.getChildCount() > 0) {
            View divider = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
            );
            divider.setLayoutParams(params);
            divider.setBackgroundColor(getColor(R.color.divider_color));
            container.addView(divider);
        }

        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_call_row, container, false);

        TextView nameView = row.findViewById(R.id.callRowName);
        TextView detailsView = row.findViewById(R.id.callRowDetails);
        Button callButton = row.findViewById(R.id.callRowButton);

        nameView.setText(name);
        detailsView.setText(details);
        callButton.setOnClickListener(v -> makeCall(phoneNumber));

        container.addView(row);
    }

    private void makeCall(String phoneNumber) {
        String normalized = normalizePhoneNumber(phoneNumber);
        if (isBlank(normalized)) {
            Toast.makeText(
                    this,
                    R.string.emergency_contacts_invalid_number,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
        ) != PackageManager.PERMISSION_GRANTED) {
            pendingPhoneNumber = normalized;
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    CALL_PHONE_PERMISSION_REQUEST
            );
            return;
        }

        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + normalized));

        try {
            startActivity(callIntent);
        } catch (SecurityException exception) {
            Toast.makeText(
                    this,
                    R.string.emergency_contacts_permission_required,
                    Toast.LENGTH_SHORT
            ).show();
        } catch (Exception exception) {
            Toast.makeText(
                    this,
                    R.string.emergency_contacts_unable_to_call,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != CALL_PHONE_PERMISSION_REQUEST) {
            return;
        }

        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && !isBlank(pendingPhoneNumber)) {
            String phoneNumber = pendingPhoneNumber;
            pendingPhoneNumber = null;
            makeCall(phoneNumber);
            return;
        }

        pendingPhoneNumber = null;
        Toast.makeText(
                this,
                R.string.emergency_contacts_permission_required,
                Toast.LENGTH_LONG
        ).show();
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }

        String number = phoneNumber.trim()
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");

        if (number.startsWith("+")) {
            return number;
        }

        if (number.matches("[6-9][0-9]{9}")) {
            return "+91" + number;
        }

        return number;
    }

    private boolean isBlank(String value) {
        return value == null
                || value.trim().isEmpty()
                || "null".equalsIgnoreCase(value.trim());
    }

    private static final class EmergencyService {
        final int titleRes;
        final String number;
        final int descriptionRes;

        EmergencyService(int titleRes, String number, int descriptionRes) {
            this.titleRes = titleRes;
            this.number = number;
            this.descriptionRes = descriptionRes;
        }
    }
}
