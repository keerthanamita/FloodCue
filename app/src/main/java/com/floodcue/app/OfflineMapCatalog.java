package com.floodcue.app;

import android.content.Context;

import java.io.File;

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

    public static File getMapFile(Context context, MapInfo map) {
        return new File(context.getFilesDir(), map.fileName);
    }

    public static boolean isDownloaded(Context context, MapInfo map) {
        File file = getMapFile(context, map);
        return file.isFile() && file.length() > 0;
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
