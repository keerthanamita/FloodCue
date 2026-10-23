
package com.floodcue.app;

import org.json.JSONException;
import org.json.JSONObject;

public class UserProfile {

    public String app_language = "en";
    public String survival_language = "en";

    public String full_name = "";
    public String blood_group = "";
    public String allergies = "";
    public String other_allergies = "";
    public String medications = "";
    public String medical_notes = "";

    public String primary_contact_name = "";
    public String primary_contact_relationship = "";
    public String primary_contact_phone = "";
    public String primary_contact_alt_phone = "";

    public String second_contact_name = "";
    public String second_contact_relationship = "";
    public String second_contact_phone = "";
    public String second_contact_alt_phone = "";

    public String third_contact_name = "";
    public String third_contact_relationship = "";
    public String third_contact_phone = "";
    public String third_contact_alt_phone = "";

    public String special_assistance = "";
    public String country = "";
    public String state = "";
    public String district = "";

    public boolean setup_completed = false;

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("app_language", app_language);
        json.put("survival_language", survival_language);
        json.put("full_name", full_name);
        json.put("blood_group", blood_group);
        json.put("allergies", allergies);
        json.put("other_allergies", other_allergies);
        json.put("medications", medications);
        json.put("medical_notes", medical_notes);

        json.put("primary_contact_name", primary_contact_name);
        json.put("primary_contact_relationship", primary_contact_relationship);
        json.put("primary_contact_phone", primary_contact_phone);
        json.put("primary_contact_alt_phone", primary_contact_alt_phone);

        json.put("second_contact_name", second_contact_name);
        json.put("second_contact_relationship", second_contact_relationship);
        json.put("second_contact_phone", second_contact_phone);
        json.put("second_contact_alt_phone", second_contact_alt_phone);

        json.put("third_contact_name", third_contact_name);
        json.put("third_contact_relationship", third_contact_relationship);
        json.put("third_contact_phone", third_contact_phone);
        json.put("third_contact_alt_phone", third_contact_alt_phone);

        json.put("special_assistance", special_assistance);
        json.put("country", country);
        json.put("state", state);
        json.put("district", district);

        json.put("setup_completed", setup_completed);

        return json;
    }

    public static UserProfile fromJson(JSONObject json) {
        UserProfile p = new UserProfile();

        p.app_language = getString(json, "app_language", "en");
        p.survival_language = getString(json, "survival_language", "en");

        p.full_name = getString(json, "full_name", "");
        p.blood_group = getString(json, "blood_group", "");
        p.allergies = getString(json, "allergies", "");
        p.other_allergies = getString(json, "other_allergies", "");
        p.medications = getString(json, "medications", "");
        p.medical_notes = getString(json, "medical_notes", "");

        p.primary_contact_name = getString(json, "primary_contact_name", "");
        p.primary_contact_relationship = getString(json, "primary_contact_relationship", "");
        p.primary_contact_phone = getString(json, "primary_contact_phone", "");
        p.primary_contact_alt_phone = getString(json, "primary_contact_alt_phone", "");

        p.second_contact_name = getString(json, "second_contact_name", "");
        p.second_contact_relationship = getString(json, "second_contact_relationship", "");
        p.second_contact_phone = getString(json, "second_contact_phone", "");
        p.second_contact_alt_phone = getString(json, "second_contact_alt_phone", "");

        p.third_contact_name = getString(json, "third_contact_name", "");
        p.third_contact_relationship = getString(json, "third_contact_relationship", "");
        p.third_contact_phone = getString(json, "third_contact_phone", "");
        p.third_contact_alt_phone = getString(json, "third_contact_alt_phone", "");

        p.special_assistance = getString(json, "special_assistance", "");
        p.country = getString(json, "country", "");
        p.state = getString(json, "state", "");
        p.district = getString(json, "district", "");

        p.setup_completed = json.optBoolean("setup_completed", false);

        return p;
    }

    private static String getString(
            JSONObject json,
            String key,
            String defaultValue
    ) {
        String value = json.optString(key, defaultValue);
        return "null".equals(value) ? defaultValue : value;
    }
}
