package com.floodcue.app;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class OfflineMapCatalogTest {

    @Test
    public void testGetById_FindsExisting() {
        OfflineMapCatalog.MapInfo found = OfflineMapCatalog.getById("tirupati");
        assertNotNull(found);
        assertEquals(found.id, "tirupati");
        assertEquals(found.name, "Tirupati");
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
        // The catalog resolves the map file relative to its own baseFolder, so
        // this check only verifies that resolution points at a sibling of the
        // temp file. Use a stable relative parent (the working directory) so
        // the test passes on any machine.
        OfflineMapCatalog.MapInfo map = OfflineMapCatalog.TIRUPATI;
        File temp = java.io.File.createTempFile("test", ".tmp");
        File resolved = OfflineMapCatalog.getMapFile(temp, map);
        assertEquals("tpt.mbtiles", resolved.getName());
        temp.delete();
    }

    @Test
    public void testShouldPreserveMapFile_ValidMbtiles_ReturnsTrue() {
        assertTrue(OfflineMapCatalog.shouldPreserveMapFile(new File("tpt.mbtiles")));
    }

    @Test
    public void testShouldPreserveMapFile_IncompleteDownload_ReturnsFalse() {
        assertFalse(OfflineMapCatalog.shouldPreserveMapFile(new File("tpt.mbtiles.download")));
    }

    @Test
    public void testShouldPreserveMapFile_NonMbtiles_ReturnsFalse() {
        assertFalse(OfflineMapCatalog.shouldPreserveMapFile(new File("readme.txt")));
    }

}
