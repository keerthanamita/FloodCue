package com.floodcue.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.location.LocationComponent;
import org.maplibre.android.location.LocationComponentActivationOptions;
import org.maplibre.android.location.modes.CameraMode;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.expressions.Expression;
import org.maplibre.android.style.layers.FillLayer;
import org.maplibre.android.style.layers.LineLayer;
import org.maplibre.android.style.layers.SymbolLayer;

import java.io.File;

import static org.maplibre.android.style.layers.PropertyFactory.fillColor;
import static org.maplibre.android.style.layers.PropertyFactory.fillOpacity;
import static org.maplibre.android.style.layers.PropertyFactory.lineColor;
import static org.maplibre.android.style.layers.PropertyFactory.lineWidth;
import static org.maplibre.android.style.layers.PropertyFactory.textColor;
import static org.maplibre.android.style.layers.PropertyFactory.textField;
import static org.maplibre.android.style.layers.PropertyFactory.textFont;
import static org.maplibre.android.style.layers.PropertyFactory.textHaloColor;
import static org.maplibre.android.style.layers.PropertyFactory.textHaloWidth;
import static org.maplibre.android.style.layers.PropertyFactory.textSize;

public class OfflineMapViewActivity extends FloodCueActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private MapView mapView;
    private TextView statusText;
    private OfflineMapCatalog.MapInfo selectedMap;
    private MapLibreMap activeMap;
    private Style activeStyle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String mapId = getIntent().getStringExtra(OfflineMapCatalog.EXTRA_MAP_ID);
        selectedMap = OfflineMapCatalog.getById(mapId);

        if (selectedMap == null
                || !OfflineMapCatalog.isDownloaded(this, selectedMap)) {
            Toast.makeText(
                    this,
                    R.string.offline_map_missing,
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        MapLibre.getInstance(getApplicationContext());
        setContentView(R.layout.activity_offline_map_view);

        TextView buttonBackToCatalog = findViewById(R.id.buttonBackToCatalog);
        buttonBackToCatalog.setText(
                getString(R.string.offline_map_back_title, selectedMap.name)
        );
        buttonBackToCatalog.setOnClickListener(v -> finish());

        statusText = findViewById(R.id.statusText);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        File mapFile = OfflineMapCatalog.getMapFile(this, selectedMap);
        statusText.setText(
                getString(R.string.offline_map_opening, selectedMap.name)
        );
        statusText.setVisibility(View.VISIBLE);

        mapView.getMapAsync(mapLibreMap ->
                configureMap(mapLibreMap, selectedMap, mapFile)
        );
    }

    private void configureMap(
            MapLibreMap mapLibreMap,
            OfflineMapCatalog.MapInfo map,
            File mapFile
    ) {
        mapLibreMap.setCameraPosition(
                new CameraPosition.Builder()
                        .target(new LatLng(map.latitude, map.longitude))
                        .zoom(map.zoom)
                        .build()
        );

        String mbtilesUrl = "mbtiles://" + mapFile.getAbsolutePath();
        Style.Builder builder =
                new Style.Builder().fromJson(makeStyleJson(mbtilesUrl));

        mapLibreMap.setStyle(builder, style -> {
            try {
                activeMap = mapLibreMap;
                activeStyle = style;
                addLayers(style);
                enableOrRequestLocation();

                statusText.setText(
                        getString(R.string.offline_map_loaded, map.name)
                );
                statusText.setVisibility(View.VISIBLE);
                statusText.postDelayed(() -> {
                    if (!isFinishing() && selectedMap == map) {
                        statusText.setVisibility(View.GONE);
                    }
                }, 3500);

            } catch (Exception e) {
                showError(getString(R.string.offline_map_style_error, e.getMessage()));
            }
        });
    }

    private String makeStyleJson(String sourceUrl) {
        String escapedSourceUrl = sourceUrl
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");

        return "{"
                + "\"version\":8,"
                + "\"name\":\"Offline District Map\","
                + "\"glyphs\":\"asset://font/{fontstack}/{range}.pbf\","
                + "\"sources\":{"
                + "\"offline\":{"
                + "\"type\":\"vector\","
                + "\"url\":\"" + escapedSourceUrl + "\""
                + "}"
                + "},"
                + "\"layers\":["
                + "{"
                + "\"id\":\"background\","
                + "\"type\":\"background\","
                + "\"paint\":{\"background-color\":\"#e9eee9\"}"
                + "}"
                + "]"
                + "}";
    }

    private void addLayers(@NonNull Style style) {
        addFill(style, "landcover", "landcover", "#dce8d5", 0.65f);
        addFill(style, "landuse", "landuse", "#d5e5c9", 0.48f);
        addFill(style, "park", "park", "#b9d9a8", 0.70f);
        addFill(style, "water", "water", "#a9d5e8", 0.95f);
        addFill(style, "building", "building", "#d5cfc5", 0.75f);

        addLine(style, "waterway", "waterway", "#8fc9e3", 1.2f);
        addLine(
                style, "transportation-minor",
                "transportation", "#ffffff", 1.0f
        );
        addLine(
                style, "transportation-major",
                "transportation", "#f4c98a", 2.2f
        );
        addLine(style, "boundary", "boundary", "#b78aa6", 1.0f);

        addLabelLayer(style, "place-labels", "place", 12f);
        addLabelLayer(style, "road-labels", "transportation_name", 11f);
        addLabelLayer(style, "water-labels", "water_name", 11f);
        addLabelLayer(style, "poi-labels", "poi", 10f);
    }

    private void addFill(
            Style style,
            String id,
            String sourceLayer,
            String color,
            float opacity
    ) {
        FillLayer layer = new FillLayer(id, "offline");
        layer.setSourceLayer(sourceLayer);
        layer.setProperties(
                fillColor(color),
                fillOpacity(opacity)
        );
        style.addLayer(layer);
    }

    private void addLine(
            Style style,
            String id,
            String sourceLayer,
            String color,
            float width
    ) {
        LineLayer layer = new LineLayer(id, "offline");
        layer.setSourceLayer(sourceLayer);
        layer.setProperties(
                lineColor(color),
                lineWidth(width)
        );
        style.addLayer(layer);
    }

    private void addLabelLayer(
            Style style,
            String id,
            String sourceLayer,
            float size
    ) {
        SymbolLayer layer = new SymbolLayer(id, "offline");
        layer.setSourceLayer(sourceLayer);
        layer.setProperties(
                textField(Expression.get("name:latin")),
                textFont(new String[]{"Noto Sans Regular"}),
                textSize(size),
                textColor("#34434d"),
                textHaloColor("#ffffff"),
                textHaloWidth(1.2f)
        );
        style.addLayer(layer);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void enableOrRequestLocation() {
        if (hasLocationPermission()) {
            enableUserLocation();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST
            );
        }
    }

    private void enableUserLocation() {
        if (activeMap == null || activeStyle == null) {
            return;
        }

        // Check location permission immediately before accessing location APIs.
        if (!hasLocationPermission()) {
            return;
        }

        try {
            LocationComponent component =
                    activeMap.getLocationComponent();

            if (!component.isLocationComponentActivated()) {
                LocationComponentActivationOptions options =
                        LocationComponentActivationOptions
                                .builder(this, activeStyle)
                                .build();

                component.activateLocationComponent(options);
            }

            // Recheck permission before enabling location updates.
            if (!hasLocationPermission()) {
                return;
            }

            component.setLocationComponentEnabled(true);
            component.setCameraMode(CameraMode.NONE);

        } catch (SecurityException e) {
            // Location permission may have been revoked.
            Toast.makeText(
                    this,
                    getString(R.string.offline_map_location_required),
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {
            showError(
                    getString(R.string.offline_map_location_enable_failed, e.getMessage())
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults
        );

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (hasLocationPermission()) {
                enableUserLocation();
            } else {
                Toast.makeText(
                        this,
                        R.string.offline_map_location_denied,
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private void showError(String message) {
        statusText.setText(message);
        statusText.setVisibility(View.VISIBLE);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mapView != null) {
            mapView.onStop();
        }
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    @Override
    protected void onDestroy() {
        if (mapView != null) {
            mapView.onDestroy();
        }
        super.onDestroy();
    }
}
