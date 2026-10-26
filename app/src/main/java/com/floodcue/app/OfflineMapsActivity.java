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

        File temporaryFile = new File(
                getFilesDir(),
                map.fileName + ".download"
        );

        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                if (temporaryFile.exists() && !temporaryFile.delete()) {
                    throw new IOException(
                            "Could not remove the previous incomplete download."
                    );
                }

                connection = (HttpURLConnection)
                        new URL(map.url).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(30000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("Accept-Encoding", "identity");
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException("Server returned HTTP " + responseCode);
                }

                long totalBytes = connection.getContentLengthLong();

                mainHandler.post(() -> {
                    downloadProgress.setIndeterminate(totalBytes <= 0);
                    if (totalBytes > 0) {
                        downloadPercent.setText(
                                getString(
                                        R.string.offline_map_downloading_percent,
                                        map.name,
                                        0
                                )
                        );
                    } else {
                        downloadPercent.setText(
                                getString(
                                        R.string.offline_map_downloading,
                                        map.name
                                )
                        );
                    }
                });

                long downloadedBytes = 0;
                long lastUpdateTime = 0;

                try (InputStream input = connection.getInputStream();
                     FileOutputStream output =
                             new FileOutputStream(temporaryFile)) {

                    byte[] buffer = new byte[32 * 1024];
                    int count;

                    while ((count = input.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                        downloadedBytes += count;

                        long now = System.currentTimeMillis();
                        if (now - lastUpdateTime >= 150 && totalBytes > 0) {
                            int percent = (int) Math.min(
                                    100,
                                    downloadedBytes * 100L / totalBytes
                            );

                            mainHandler.post(() -> {
                                downloadProgress.setIndeterminate(false);
                                downloadProgress.setProgress(percent);
                                downloadPercent.setText(
                                        getString(
                                                R.string.offline_map_downloading_percent,
                                                map.name,
                                                percent
                                        )
                                );
                            });

                            lastUpdateTime = now;
                        }
                    }

                    output.flush();
                    output.getFD().sync();
                }

                if (downloadedBytes == 0
                        || !temporaryFile.isFile()
                        || temporaryFile.length() == 0) {
                    throw new IOException("The downloaded file is empty.");
                }

                moveDownloadedFile(temporaryFile, destination);
                mainHandler.post(() -> {
                    downloadInProgress = false;
                    setButtonsEnabled(true);
                    setDownloadProgressVisible(false);
                    openMap(map);
                });

            } catch (Exception e) {
                if (temporaryFile.exists()) {
                    temporaryFile.delete();
                }

                String message = e.getMessage() == null
                        ? "Unknown download error"
                        : e.getMessage();

                mainHandler.post(() -> {
                    downloadInProgress = false;
                    setButtonsEnabled(true);
                    setDownloadProgressVisible(false);
                    Toast.makeText(
                            OfflineMapsActivity.this,
                            getString(
                                    R.string.offline_map_download_failed,
                                    map.name,
                                    message
                            ),
                            Toast.LENGTH_LONG
                    ).show();
                });

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
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
}
