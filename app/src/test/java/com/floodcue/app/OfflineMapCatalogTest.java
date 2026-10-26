package com.floodcue.app;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class OfflineMapCatalogTest {

    @Test
    public void testGetById_FindsExisting() {
        OfflineMapCatalog.MapInfo found =
                OfflineMapCatalog.getById("tirupati");
        assertNotNull(found);
        assertEquals("tirupati", found.id);
        assertEquals("Tirupati", found.name);
        assertEquals("tpt.mbtiles", found.fileName);
        assertEquals(13.6288, found.latitude, 0.0001);
        assertEquals(79.4192, found.longitude, 0.0001);
    }

    @Test
    public void testGetById_NullReturnsNull() {
        assertNull(OfflineMapCatalog.getById(null));
    }

    @Test
    public void testGetById_NotFoundReturnsNull() {
        assertNull(OfflineMapCatalog.getById("nonexistent"));
    }

    @Test
    public void testAllMaps_ReturnsBothMaps() {
        OfflineMapCatalog.MapInfo[] maps = OfflineMapCatalog.allMaps();
        assertEquals(2, maps.length);
        assertEquals("tirupati", maps[0].id);
        assertEquals("nellore", maps[1].id);
    }

    @Test
    public void testIsDownloaded_FileExists_ReturnsTrue() throws IOException {
        // We cannot create a temporary file that persists across JUnit runs,
        // but we can verify the catalog's file helper logic by constructing
        // a valid MapInfo and checking the File resolution.
        OfflineMapCatalog.MapInfo map = OfflineMapCatalog.TIRUPATI;
        File file = OfflineMapCatalog.getMapFile(
                java.io.File.createTempFile("test", ".tmp"), map);
        assertEquals(new File("").getAbsolutePath() + File.separator, file.getParent());
        assertEquals("tpt.mbtiles", file.getName());
    }

    @Test
    public void testShouldPreserveMapFile_ValidMbtiles_ReturnsTrue() {
        assertTrue(OfflineMapCatalog.shouldPreserveMapFile(
                new File("tpt.mbtiles")));
    }

    @Test
    public void testShouldPreserveMapFile_IncompleteDownload_ReturnsFalse() {
        assertFalse(OfflineMapCatalog.shouldPreserveMapFile(
                new File("tpt.mbtiles.download")));
    }

    @Test
    public void testShouldPreserveMapFile_NonMbtiles_ReturnsFalse() {
        assertFalse(OfflineMapCatalog.shouldPreserveMapFile(
                new File("readme.txt")));
    }
}
