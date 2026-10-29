package com.floodcue.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class OfflineMapsActivity extends FloodCueActivity {

    private Button btnTirupati;
    private Button btnNellore;
    private ProgressBar downloadProgress;
    private TextView downloadPercent;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean downloadInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_maps);

        TextView buttonBackToHome = findViewById(R.id.buttonBackToHome);
        btnTirupati = findViewById(R.id.btnTirupati);
        btnNellore = findViewById(R.id.btnNellore);
        downloadProgress = findViewById(R.id.downloadProgress);
        downloadPercent = findViewById(R.id.downloadPercent);

        buttonBackToHome.setOnClickListener(v -> finish());
        btnTirupati.setOnClickListener(
                v -> selectMap(OfflineMapCatalog.TIRUPATI)
        );
        btnNellore.setOnClickListener(
                v -> selectMap(OfflineMapCatalog.NELLORE)
        );
    }

    private void selectMap(OfflineMapCatalog.MapInfo map) {
        if (downloadInProgress) {
            return;
        }

        File destination = OfflineMapCatalog.getMapFile(this, map);

        if (OfflineMapCatalog.isDownloaded(this, map)) {
            openMap(map);
            return;
        }

        downloadMap(map, destination);
    }

    private void downloadMap(OfflineMapCatalog.MapInfo map, File destination) {
        downloadInProgress = true;
        setButtonsEnabled(false);
        setDownloadProgressVisible(true);
        downloadProgress.setIndeterminate(true);
        downloadProgress.setProgress(0);
        downloadPercent.setText(
                getString(R.string.offline_map_connecting, map.name)
        );

        // Use the central download method in OfflineMapCatalog which
        // writes a .download file and renames it to the final .mbtiles
        // only when the download is complete and verified. The progress
        // of the download is reported back on the main thread via a
        // Handler.
        if (!OfflineMapCatalog.download(this, map, new OfflineMapCatalog.DownloadListener() {
            @Override
            public void onProgress(long downloadedBytes, long totalBytes) {
                mainHandler.post(() -> updateDownloadProgress(map, downloadedBytes, totalBytes));
            }

            @Override
            public void onComplete() {
                mainHandler.post(() -> {
                    downloadInProgress = false;
                    setButtonsEnabled(true);
                    setDownloadProgressVisible(false);
                    openMap(map);
                });
            }

            @Override
            public void onFailed() {
                mainHandler.post(() -> {
                    downloadInProgress = false;
                    setButtonsEnabled(true);
                    setDownloadProgressVisible(false);
                    Toast.makeText(
                            OfflineMapsActivity.this,
                            getString(R.string.offline_map_download_failed, map.name, "Download failed"),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        })) {
            String message = getString(
                    R.string.offline_map_download_failed,
                    map.name,
                    "Cannot start download"
            );
            mainHandler.post(() -> {
                downloadInProgress = false;
                setButtonsEnabled(true);
                setDownloadProgressVisible(false);
                Toast.makeText(
                        OfflineMapsActivity.this,
                        message,
                        Toast.LENGTH_LONG
                ).show();
            });
        }
    }

    private void moveDownloadedFile(File source, File destination)
            throws IOException {
        if (destination.exists() && !destination.delete()) {
            throw new IOException("Could not replace the previous map file.");
        }

        if (source.renameTo(destination)) {
            return;
        }

        try (FileInputStream input = new FileInputStream(source);
             FileOutputStream output = new FileOutputStream(destination)) {

            byte[] buffer = new byte[32 * 1024];
            int count;

            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }

            output.flush();
            output.getFD().sync();
        }

        if (!source.delete()) {
            source.deleteOnExit();
        }
    }

    private void openMap(OfflineMapCatalog.MapInfo map) {
        Intent intent = new Intent(this, OfflineMapViewActivity.class);
        intent.putExtra(OfflineMapCatalog.EXTRA_MAP_ID, map.id);
        startActivity(intent);
    }

    private void setButtonsEnabled(boolean enabled) {
        btnTirupati.setEnabled(enabled);
        btnNellore.setEnabled(enabled);
    }

    private void setDownloadProgressVisible(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        downloadProgress.setVisibility(visibility);
        downloadPercent.setVisibility(visibility);
    }

    private void updateDownloadProgress(
            OfflineMapCatalog.MapInfo map,
            long downloadedBytes,
            long totalBytes
    ) {
        if (totalBytes <= 0) {
            downloadProgress.setIndeterminate(true);
            downloadPercent.setText(getString(R.string.offline_map_downloading, map.name));
            return;
        }

        int percent = (int) Math.min(100, (downloadedBytes * 100L) / totalBytes);
        downloadProgress.setIndeterminate(false);
        downloadProgress.setProgress(percent);
        downloadPercent.setText(getString(
                R.string.offline_map_downloading_percent,
                map.name,
                percent
        ));
    }
}
