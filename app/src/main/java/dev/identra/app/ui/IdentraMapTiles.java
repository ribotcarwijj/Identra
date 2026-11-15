package dev.identra.app.ui;

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.util.MapTileIndex;

/**
 * Location map tiles.
 * <p>
 * 0.5.1: OpenStreetMap standard tiles (https://tile.openstreetmap.org/{z}/{x}/{y}.png), keyless,
 * street names everywhere and house numbers from z18. Requests carry a User-Agent naming the app
 * and its version (set in IdentraLocationFragment.configureOsmdroid), as the OSM tile usage policy
 * asks; no Referer is sent. Night mode inverts the tiles (osmdroid colour filter).
 * <p>
 * 0.5.0 used CARTO Voyager / Dark Matter (basemaps.cartocdn.com). CARTO now answers every keyless
 * request on all of its hosts (a-d.basemaps.cartocdn.com and the old fastly hosts) with HTTP 200
 * image/png watermark tiles reading "API KEY REQUIRED" (ETag "wm-…-light"/"wm-…-dark"), so it
 * can no longer be used without a CARTO account key. Checked from the phone 2026-11-15.
 */
public final class IdentraMapTiles extends OnlineTileSourceBase {
    public static final String COPYRIGHT = "© OpenStreetMap contributors";
    /** tile.openstreetmap.org serves z0..z19. */
    public static final int MAX_ZOOM = 19;
    /** Cache folder (under cacheDir/osmdroid) for these tiles; older folders are deleted on start. */
    public static final String CACHE_DIR = "tiles-osm";
    /** Cache folders of earlier tile sources (0.5.0 CARTO, whose cache holds "API key" tiles). */
    public static final String[] OLD_CACHE_DIRS = {"tiles-carto", "tiles"};

    public static IdentraMapTiles standard() { return new IdentraMapTiles(); }

    private IdentraMapTiles() {
        super("IdentraOsmStandard", 0, MAX_ZOOM, 256, ".png",
                new String[]{"https://tile.openstreetmap.org/"}, COPYRIGHT);
    }

    @Override
    public String getTileURLString(long pMapTileIndex) {
        return getBaseUrl() + MapTileIndex.getZoom(pMapTileIndex) + "/" + MapTileIndex.getX(pMapTileIndex) + "/"
                + MapTileIndex.getY(pMapTileIndex) + mImageFilenameEnding;
    }
}
