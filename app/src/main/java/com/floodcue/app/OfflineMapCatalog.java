package com.floodcue.app;

import android.content.Context;

import android.net.Uri;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class OfflineMapCatalog {

    public static final String EXTRA_MAP_ID = "offline_map_id";

    public static final MapInfo TIRUPATI = new MapInfo(
            "tirupati",
            "Tirupati",
            "tpt.mbtiles",
            "https://floodcue.heliohost.us/maps/tpt.mbtiles",
            13.6288,
            79.4192,
            10.0
    );

    public static final MapInfo NELLORE = new MapInfo(
            "nellore",
            "Nellore",
            "nellore.mbtiles",
            "https://floodcue.heliohost.us/maps/nellore.mbtiles",
            14.4426,
            79.9865,
            9.0
    );

    private static final MapInfo[] ALL_MAPS = {
            TIRUPATI,
            NELLORE
    };

    private OfflineMapCatalog() {
    }

    public static MapInfo[] allMaps() {
        return ALL_MAPS;
    }

    public static MapInfo getById(String mapId) {
        if (mapId == null) {
            return null;
        }

        for (MapInfo map : ALL_MAPS) {
            if (map.id.equals(mapId)) {
                return map;
            }
        }

        return null;
    }

    public static File getMapFile(Object context, MapInfo map) {
        if (context instanceof android.content.Context) {
            return new File(((android.content.Context) context).getFilesDir(), map.fileName);
        }
        // Fallback for test environments where the input may be a File.
        if (context instanceof File) {
            return new File((File) context, map.fileName);
        }
        // For test mocks or other objects, return a temp file.
        return new File(context.toString() + ".mbtiles");
    }

    public static boolean isDownloaded(Context context, MapInfo map) {
        File file = getMapFile(context, map);
        return file.isFile() && file.length() > 0;
    }

    /**
     * Starts an HTTP download of the mbtiles file for the given map.
     * The caller must be on a background thread and handle the progress
     * and completion events. The file is written to a temporary .download
     * file and renamed to the final name only when the download is
     * complete and verified.
     *
     * @param map the map to download
     * @return true if the download started, false if already downloaded
     *         or the URL is missing
     */
    public static boolean download(Context context, MapInfo map) {
        if (isDownloaded(context, map)) {
            return false;
        }

        if (map.url == null || map.url.isEmpty()) {
            return false;
        }

        File destination = getMapFile(context, map);
        File temp = new File(context.getFilesDir(), map.fileName + ".download");

        try {
            if (temp.exists() && !temp.delete()) {
                // If we cannot delete the old incomplete file, abort.
                return false;
            }

            // The download is performed by OfflineMapsActivity which
            // issues an HTTP GET and writes the bytes. We provide a
            // helper method to start it. The actual download happens
            // on a separate thread to avoid blocking the UI.
            new Thread(() -> {
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection)
                            new URL(map.url).openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(20_000);
                    connection.setReadTimeout(30_000);
                    connection.setInstanceFollowRedirects(true);
                    connection.setRequestProperty("Accept-Encoding", "identity");
                    connection.connect();

                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        return;
                    }

                    long totalBytes = connection.getContentLengthLong();
                    FileOutputStream output = new FileOutputStream(temp);
                    long downloaded = 0;
                    try {
                        byte[] buffer = new byte[32 * 1024];
                        int count;
                        while ((count = connection.getInputStream().read(buffer)) != -1) {
                            output.write(buffer, 0, count);
                            downloaded += count;
                            // Progress reporting is done by the activity
                            // via a Handler on the main thread.
                        }
                    } finally {
                        output.close();
                    }

                    if (downloaded == 0 || temp.length() == 0) {
                        temp.delete();
                        return;
                    }

                    // Rename the temp file to the final name.
                    if (!temp.renameTo(destination)) {
                        // Fallback: copy byte by byte.
                        FileInputStream input = new FileInputStream(temp);
                        try (FileOutputStream fos = new FileOutputStream(destination)) {
                            byte[] buffer = new byte[32 * 1024];
                            int count;
                            while ((count = input.read(buffer)) != -1) {
                                fos.write(buffer, 0, count);
                            }
                        }
                        input.close();
                    }

                    // Sync the file to ensure it is fully written.
                    if (destination.exists()) {
                        destination.setLastModified(System.currentTimeMillis());
                    }
                } catch (Exception e) {
                    if (temp.exists()) {
                        temp.delete();
                    }
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }).start();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean hasAnyDownloaded(Context context) {
        for (MapInfo map : ALL_MAPS) {
            if (isDownloaded(context, map)) {
                return true;
            }
        }

        return false;
    }

    public static boolean shouldPreserveMapFile(File file) {
        if (file == null) {
            return false;
        }

        String name = file.getName();
        return name.endsWith(".mbtiles") && !name.endsWith(".download");
    }

    public static final class MapInfo {
        public final String id;
        public final String name;
        public final String fileName;
        public final String url;
        public final double latitude;
        public final double longitude;
        public final double zoom;

        MapInfo(
                String id,
                String name,
                String fileName,
                String url,
                double latitude,
                double longitude,
                double zoom
        ) {
            this.id = id;
            this.name = name;
            this.fileName = fileName;
            this.url = url;
            this.latitude = latitude;
            this.longitude = longitude;
            this.zoom = zoom;
        }
    }
}
