package dev.identra.app.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import android.location.Location;
import java.util.Map;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.TilesOverlay;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.identra.app.BuildConfig;
import dev.identra.app.R;
import dev.identra.app.x.core.database.A_CODE;
import dev.identra.app.x.core.hook.AppXpPacket;
import dev.identra.app.x.core.hook.AssignmentPacket;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;
import dev.identra.app.x.ui.core.UserClientAppContext;

/**
 * Location tab: search a place (Photon, Nominatim fallback), pick it on an OpenStreetMap map,
 * and set it as the spoofed location for every app or one app. See {@link IdentraLocation}
 * for the setting keys.
 */
public class IdentraLocationFragment extends Fragment {
    private static final String TAG = "Identra.UiLocationTab";
    private static final long DEBOUNCE_MS = 400;
    /** House numbers appear on the OSM standard tiles from z18. */
    private static final double DEFAULT_ZOOM = 18.0;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService net = Executors.newSingleThreadExecutor();
    private int searchSeq = 0;

    private MapView map;
    private Marker selMarker, activeMarker, meMarker;
    private View myProgress;
    private MaterialButton myButton;
    private IdentraRealLocation.Request myRequest;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private EditText search;
    private View results, resultsProgress, selCard, targetCard;
    private TextView resultsMessage, selName, selCoords, targetText;
    private ImageView targetIcon;
    private MaterialButton selStar, selApp;
    private final IdentraListAdapter resultsAdapter = new IdentraListAdapter();
    private LinearLayout activeBox, recentBox, advancedBox;
    private ChipGroup favs;
    private boolean advancedOpen = false;

    private IdentraPlaces.Place selected;
    private IdentraLocation.Value activeGlobal;

    // set when the tab was opened to choose a location for one app
    private String targetPkg;
    private int targetUid;

    private final Runnable debounced = this::runSearch;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissionResult);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        configureOsmdroid(requireContext());
        View v = inflater.inflate(R.layout.fragment_identra_location, container, false);
        search = v.findViewById(R.id.locSearch);
        results = v.findViewById(R.id.locResults);
        resultsProgress = v.findViewById(R.id.locResultsProgress);
        resultsMessage = v.findViewById(R.id.locResultsMessage);
        selCard = v.findViewById(R.id.locSelCard);
        selName = v.findViewById(R.id.locSelName);
        selCoords = v.findViewById(R.id.locSelCoords);
        selStar = v.findViewById(R.id.locSelStar);
        selApp = v.findViewById(R.id.locSelApp);
        targetCard = v.findViewById(R.id.locTargetCard);
        targetText = v.findViewById(R.id.locTargetText);
        targetIcon = v.findViewById(R.id.locTargetIcon);
        activeBox = v.findViewById(R.id.locActive);
        recentBox = v.findViewById(R.id.locRecent);
        advancedBox = v.findViewById(R.id.locAdvancedBox);
        favs = v.findViewById(R.id.locFavs);

        RecyclerView rl = v.findViewById(R.id.locResultsList);
        rl.setLayoutManager(new LinearLayoutManager(requireContext()));
        rl.setAdapter(resultsAdapter);

        setupMap(v);
        setupSearch();

        v.findViewById(R.id.locSelAll).setOnClickListener(x -> { if (selected != null) applyGlobal(selected); });
        selApp.setOnClickListener(x -> { if (selected != null) chooseAppFor(selected); });
        selStar.setOnClickListener(x -> toggleFavourite(selected));
        v.findViewById(R.id.locRecenter).setOnClickListener(x -> recenter());
        myButton = v.findViewById(R.id.locMyLocation);
        myProgress = v.findViewById(R.id.locMyProgress);
        myButton.setOnClickListener(x -> onMyLocation());
        v.findViewById(R.id.locTargetCancel).setOnClickListener(x -> clearTarget());
        v.findViewById(R.id.locRecentClear).setOnClickListener(x -> {
            IdentraPlaces.clearRecent(requireContext());
            renderRecent();
        });

        renderFavourites();
        renderRecent();
        renderAdvanced();
        renderTarget();
        return v;
    }

    // ---- map -------------------------------------------------------------------

    private static boolean osmConfigured = false;

    static void configureOsmdroid(Context context) {
        if (osmConfigured) return;
        try {
            Context app = context.getApplicationContext();
            org.osmdroid.config.IConfigurationProvider cfg = org.osmdroid.config.Configuration.getInstance();
            cfg.load(app, app.getSharedPreferences("identra_osmdroid", Context.MODE_PRIVATE));
            // 0.5.1: User-Agent names the app and its version (OSM tile usage policy); osmdroid sends no Referer
            cfg.setUserAgentValue(BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME + " (Android)");
            File base = new File(app.getCacheDir(), "osmdroid");
            // 0.5.1: drop the caches of earlier tile sources (0.5.0 CARTO cached "API key required" tiles)
            for (String old : IdentraMapTiles.OLD_CACHE_DIRS) deleteRecursively(new File(base, old));
            File tiles = new File(base, IdentraMapTiles.CACHE_DIR);
            //noinspection ResultOfMethodCallIgnored
            tiles.mkdirs();
            cfg.setOsmdroidBasePath(base);
            cfg.setOsmdroidTileCache(tiles);
            osmConfigured = true;
        } catch (Throwable t) {
            Log.e(TAG, "osmdroid config failed: " + t);
        }
    }

    private static void deleteRecursively(File f) {
        if (f == null || !f.exists()) return;
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteRecursively(k);
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }

    private void setupMap(View v) {
        map = v.findViewById(R.id.locMap);
        // 0.5.1: OpenStreetMap standard tiles (keyless; CARTO now needs an API key). Night mode inverts them.
        map.setTileSource(IdentraMapTiles.standard());
        if (isNight()) map.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);
        map.setMultiTouchControls(true);
        map.setTilesScaledToDpi(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        map.setMinZoomLevel(2.5);
        map.setMaxZoomLevel((double) IdentraMapTiles.MAX_ZOOM);
        map.setVerticalMapRepetitionEnabled(false);
        map.getController().setZoom(3.0);
        map.getController().setCenter(new GeoPoint(30.0, 10.0));

        // let the map own its gestures inside the scrolling page
        map.setOnTouchListener((view, e) -> {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_MOVE)
                view.getParent().requestDisallowInterceptTouchEvent(true);
            else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL)
                view.getParent().requestDisallowInterceptTouchEvent(false);
            return false;
        });

        map.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                hideResults();
                select(droppedPin(p.getLatitude(), p.getLongitude()), false, false);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                hideResults();
                select(droppedPin(p.getLatitude(), p.getLongitude()), false, true);
                return true;
            }
        }));

        CopyrightOverlay copyright = new CopyrightOverlay(requireContext());
        copyright.setTextColor(ContextCompat.getColor(requireContext(), R.color.identra_on_surface_variant));
        map.getOverlays().add(copyright);
    }

    private boolean isNight() {
        int m = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return m == Configuration.UI_MODE_NIGHT_YES;
    }

    private IdentraPlaces.Place droppedPin(double lat, double lon) {
        return new IdentraPlaces.Place(getString(R.string.identra_loc_dropped_pin), null,
                IdentraLocation.clampLat(lat), IdentraLocation.wrapLon(lon));
    }

    private void placeSelectedMarker(IdentraPlaces.Place p) {
        GeoPoint gp = new GeoPoint(p.lat, p.lon);
        if (selMarker == null) {
            selMarker = new Marker(map);
            selMarker.setIcon(drawable(R.drawable.ic_identra_map_pin));
            selMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            selMarker.setInfoWindow(null);
            selMarker.setDraggable(true);
            selMarker.setOnMarkerClickListener((m, mv) -> { showPlaceSheet(selected); return true; });
            selMarker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
                @Override public void onMarkerDrag(Marker marker) { }
                @Override public void onMarkerDragStart(Marker marker) { marker.setAlpha(0.7f); }
                @Override public void onMarkerDragEnd(Marker marker) {
                    marker.setAlpha(1f);
                    GeoPoint g = marker.getPosition();
                    select(droppedPin(g.getLatitude(), g.getLongitude()), false, false);
                }
            });
            map.getOverlays().add(selMarker);
        }
        selMarker.setPosition(gp);
        map.invalidate();
    }

    private void placeActiveMarker() {
        if (map == null) return;
        if (activeGlobal == null || !activeGlobal.set) {
            if (activeMarker != null) { map.getOverlays().remove(activeMarker); activeMarker = null; map.invalidate(); }
            return;
        }
        if (activeMarker == null) {
            activeMarker = new Marker(map);
            activeMarker.setIcon(drawable(R.drawable.ic_identra_map_active));
            activeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            activeMarker.setInfoWindow(null);
            activeMarker.setOnMarkerClickListener((m, mv) -> {
                snack(getString(R.string.identra_loc_active_marker, activeLabel()));
                return true;
            });
            // below the selection pin
            int idx = selMarker == null ? map.getOverlays().size() : Math.max(0, map.getOverlays().indexOf(selMarker));
            map.getOverlays().add(idx, activeMarker);
        }
        activeMarker.setPosition(new GeoPoint(activeGlobal.lat, activeGlobal.lon));
        map.invalidate();
    }

    private Drawable drawable(int res) { return ContextCompat.getDrawable(requireContext(), res); }

    private void moveTo(double lat, double lon, boolean animate) {
        GeoPoint gp = new GeoPoint(lat, lon);
        double zoom = Math.max(map.getZoomLevelDouble(), DEFAULT_ZOOM);
        if (animate) map.getController().animateTo(gp, zoom, 600L);
        else { map.getController().setZoom(zoom); map.getController().setCenter(gp); }
    }

    /** Shows the selected place, else the spoofed (active) location. Never the phone's position. */
    private void recenter() {
        if (selected != null) moveTo(selected.lat, selected.lon, true);
        else if (activeGlobal != null && activeGlobal.set) {
            moveTo(activeGlobal.lat, activeGlobal.lon, true);
            snack(getString(R.string.identra_loc_showing_spoofed, activeLabel()));
        } else snack(getString(R.string.identra_loc_nothing_selected));
    }

    // ---- real "my location" (0.4.0) ------------------------------------------------

    private void onMyLocation() {
        if (!isAdded()) return;
        if (!IdentraRealLocation.hasPermission(requireContext())) {
            permissionLauncher.launch(new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION});
            return;
        }
        fetchMyLocation();
    }

    private void onPermissionResult(Map<String, Boolean> result) {
        if (!isAdded()) return;
        if (IdentraRealLocation.hasPermission(requireContext())) {
            fetchMyLocation();
            return;
        }
        boolean askAgain = shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)
                || shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        View v = getView();
        if (v == null) return;
        Snackbar sb = Snackbar.make(v, R.string.identra_loc_my_denied, Snackbar.LENGTH_LONG);
        if (!askAgain) {
            sb.setAction(R.string.identra_loc_my_open_settings, x -> {
                try {
                    android.content.Intent i = new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.fromParts("package", requireContext().getPackageName(), null));
                    startActivity(i);
                } catch (Exception ignored) { }
            });
        }
        sb.show();
    }

    private void fetchMyLocation() {
        if (myRequest != null) myRequest.cancel();
        setMyBusy(true);
        myRequest = IdentraRealLocation.fetch(requireContext(), new IdentraRealLocation.Callback() {
            @Override
            public void onLocation(Location l, long ageMs, boolean stale) {
                myRequest = null;
                if (!isAdded() || map == null) return;
                setMyBusy(false);
                showMe(l, ageMs, stale);
            }

            @Override
            public void onError(int error) {
                myRequest = null;
                if (!isAdded()) return;
                setMyBusy(false);
                int msg = error == IdentraRealLocation.ERROR_PERMISSION ? R.string.identra_loc_my_denied
                        : error == IdentraRealLocation.ERROR_DISABLED ? R.string.identra_loc_my_off
                        : R.string.identra_loc_my_no_fix;
                snack(getString(msg));
            }
        });
    }

    private void setMyBusy(boolean busy) {
        if (myProgress != null) myProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
        if (myButton != null) myButton.setEnabled(!busy);
        if (busy) snack(getString(R.string.identra_loc_my_finding));
    }

    private void showMe(Location l, long ageMs, boolean stale) {
        GeoPoint gp = new GeoPoint(l.getLatitude(), l.getLongitude());
        if (meMarker == null) {
            meMarker = new Marker(map);
            meMarker.setIcon(drawable(R.drawable.ic_identra_map_me));
            meMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            meMarker.setInfoWindow(null);
            meMarker.setOnMarkerClickListener((m, mv) -> {
                GeoPoint p = m.getPosition();
                select(new IdentraPlaces.Place(getString(R.string.identra_loc_my_place), null,
                        p.getLatitude(), p.getLongitude()), false, true);
                return true;
            });
            map.getOverlays().add(meMarker);
        }
        meMarker.setPosition(gp);
        moveTo(l.getLatitude(), l.getLongitude(), true);
        map.invalidate();

        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.identra_loc_my_found, Math.round(l.hasAccuracy() ? l.getAccuracy() : 0)));
        if (stale) sb.append(" · ").append(getString(R.string.identra_loc_my_old, Math.max(1, ageMs / 60000L)));
        if (IdentraRealLocation.isMock(l)) sb.append(" · ").append(getString(R.string.identra_loc_my_mock));
        if (selfSpoofed()) sb.append(" · ").append(getString(R.string.identra_loc_my_self_hooked));
        snack(sb.toString());
    }

    /** True when the user assigned Spoof.Location hooks to Identra itself (then the reading may be spoofed). */
    private boolean selfSpoofed() {
        try {
            IdentraData.Snapshot s = IdentraData.cached();
            AppXpPacket self = IdentraData.findApp(s, BuildConfig.APPLICATION_ID, android.os.Process.myUid());
            if (self == null) return false;
            for (String g : IdentraData.assignedGroups(s, self))
                if (g != null && g.toLowerCase(Locale.ROOT).startsWith("spoof.location")) return true;
        } catch (Throwable ignored) { }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null && !isHidden()) map.onResume();
        refreshActive();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (map == null) return;
        if (hidden) {
            map.onPause();
            hideKeyboard();
        } else {
            map.onResume();
            refreshActive();
            renderFavourites();
            renderRecent();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        main.removeCallbacks(debounced);
        if (myRequest != null) { myRequest.cancel(); myRequest = null; }
        if (map != null) map.onDetach();
        map = null;
        selMarker = null;
        activeMarker = null;
        meMarker = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        net.shutdownNow();
    }

    // ---- search ------------------------------------------------------------------

    private void setupSearch() {
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                main.removeCallbacks(debounced);
                String q = s == null ? "" : s.toString().trim();
                if (q.isEmpty()) { searchSeq++; hideResults(); return; }
                main.postDelayed(debounced, DEBOUNCE_MS);
            }
        });
        search.setOnEditorActionListener((tv, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                main.removeCallbacks(debounced);
                runSearch();
                hideKeyboard();
                return true;
            }
            return false;
        });
        search.setOnFocusChangeListener((view, has) -> {
            if (has && resultsAdapter.getItemCount() > 0 && search.getText() != null && search.getText().length() > 0)
                results.setVisibility(View.VISIBLE);
        });
    }

    private void runSearch() {
        if (!isAdded()) return;
        final String q = search.getText() == null ? "" : search.getText().toString().trim();
        if (q.isEmpty()) { hideResults(); return; }
        final int seq = ++searchSeq;

        double[] pair = IdentraLocation.parsePair(q);
        if (pair != null) {
            IdentraPlaces.Place p = new IdentraPlaces.Place(getString(R.string.identra_loc_coordinates), null, pair[0], pair[1]);
            List<IdentraPlaces.Place> one = new ArrayList<>();
            one.add(p);
            showResults(one, null);
            return;
        }
        if (q.length() < 2) { hideResults(); return; }

        results.setVisibility(View.VISIBLE);
        resultsProgress.setVisibility(View.VISIBLE);
        net.submit(() -> {
            List<IdentraPlaces.Place> found = null;
            String error = null;
            try {
                found = IdentraGeocoder.search(q);
            } catch (Throwable t) {
                Log.w(TAG, "Search failed: " + t);
                error = t.getMessage();
            }
            final List<IdentraPlaces.Place> fFound = found;
            final String fError = error;
            main.post(() -> {
                if (!isAdded() || seq != searchSeq) return;
                resultsProgress.setVisibility(View.GONE);
                if (fFound == null) showResults(null, getString(R.string.identra_loc_search_failed));
                else if (fFound.isEmpty()) showResults(null, getString(R.string.identra_loc_search_none, q));
                else showResults(fFound, null);
                if (fError != null) Log.d(TAG, "error=" + fError);
            });
        });
    }

    private void showResults(List<IdentraPlaces.Place> places, String message) {
        results.setVisibility(View.VISIBLE);
        resultsProgress.setVisibility(View.GONE);
        resultsMessage.setVisibility(message == null ? View.GONE : View.VISIBLE);
        resultsMessage.setText(message);
        List<IdentraListAdapter.Item> items = new ArrayList<>();
        if (places != null) {
            for (final IdentraPlaces.Place p : places) {
                String sub = p.detail == null || p.detail.isEmpty() ? IdentraPlaces.shortCoords(p.lat, p.lon) : p.detail;
                IdentraListAdapter.Item it = IdentraListAdapter.Item.row(R.drawable.ic_identra_location, p.name, sub)
                        .click(x -> {
                            hideKeyboard();
                            hideResults();
                            select(p, true, true);
                        });
                it.chevron = false;
                items.add(it);
            }
        }
        resultsAdapter.submit(items);
    }

    private void hideResults() {
        if (results != null) results.setVisibility(View.GONE);
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
            search.clearFocus();
        } catch (Throwable ignored) { }
    }

    // ---- selection ---------------------------------------------------------------------

    private void select(IdentraPlaces.Place p, boolean move, boolean sheet) {
        if (p == null || map == null) return;
        selected = p;
        placeSelectedMarker(p);
        if (move) moveTo(p.lat, p.lon, true);
        selName.setText(p.name);
        selCoords.setText(p.detail == null || p.detail.isEmpty() ? p.coords() : p.detail + "\n" + p.coords());
        if (selCard.getVisibility() != View.VISIBLE) {
            selCard.setAlpha(0f);
            selCard.setVisibility(View.VISIBLE);
            selCard.animate().alpha(1f).setDuration(200).start();
        }
        updateStar();
        renderTarget();
        if (sheet) showPlaceSheet(p);
    }

    private void updateStar() {
        if (selected == null) return;
        boolean fav = IdentraPlaces.isFavourite(requireContext(), selected);
        selStar.setIconResource(fav ? R.drawable.ic_identra_star : R.drawable.ic_identra_star_outline);
    }

    private void toggleFavourite(IdentraPlaces.Place p) {
        if (p == null) return;
        Context c = requireContext();
        if (IdentraPlaces.isFavourite(c, p)) {
            IdentraPlaces.removeFavourite(c, p);
            snack(getString(R.string.identra_loc_unstarred, p.name));
        } else {
            IdentraPlaces.addFavourite(c, p);
            snack(getString(R.string.identra_loc_starred, p.name));
        }
        updateStar();
        renderFavourites();
    }

    private void showPlaceSheet(final IdentraPlaces.Place p) {
        if (p == null || !isAdded()) return;
        final IdentraSheet sheet = new IdentraSheet(requireContext())
                .title(p.name)
                .sub(p.detail);
        sheet.field(getString(R.string.identra_loc_latitude), IdentraLocation.format(p.lat));
        sheet.field(getString(R.string.identra_loc_longitude), IdentraLocation.format(p.lon));
        if (jitterOn()) sheet.text(getString(R.string.identra_loc_jitter_note, jitterMeters()));
        final AppXpPacket target = targetApp();
        if (target != null) {
            sheet.action(IdentraSheet.STYLE_FILLED, getString(R.string.identra_loc_set_for, IdentraData.labelOf(target)), x -> applyApp(target, p), true);
            sheet.action(IdentraSheet.STYLE_TONAL, getString(R.string.identra_loc_set_all), x -> applyGlobal(p), true);
        } else {
            sheet.action(IdentraSheet.STYLE_FILLED, getString(R.string.identra_loc_set_all), x -> applyGlobal(p), true);
            sheet.action(IdentraSheet.STYLE_TONAL, getString(R.string.identra_loc_set_app), x -> chooseAppFor(p), true);
        }
        boolean fav = IdentraPlaces.isFavourite(requireContext(), p);
        sheet.action(IdentraSheet.STYLE_TEXT, getString(fav ? R.string.identra_loc_unfavourite : R.string.identra_loc_favourite),
                x -> toggleFavourite(p), true);
        sheet.show();
    }

    private void chooseAppFor(final IdentraPlaces.Place p) {
        IdentraAppPicker.show(requireContext(), getString(R.string.identra_loc_pick_app), app -> applyApp(app, p));
    }

    // ---- apply -----------------------------------------------------------------------

    private String mode() { return IdentraPlaces.prefs(requireContext()).getString(IdentraPlaces.KEY_MODE, IdentraLocation.TYPE_SET); }
    private String accuracy() { return IdentraPlaces.prefs(requireContext()).getString(IdentraPlaces.KEY_ACCURACY, ""); }
    private boolean jitterOn() { return IdentraPlaces.prefs(requireContext()).getBoolean(IdentraPlaces.KEY_JITTER_ON, false); }
    private int jitterMeters() { return IdentraPlaces.prefs(requireContext()).getInt(IdentraPlaces.KEY_JITTER_M, 50); }

    private double[] finalCoords(IdentraPlaces.Place p) {
        if (jitterOn()) return IdentraLocation.jitter(p.lat, p.lon, jitterMeters());
        return new double[]{p.lat, p.lon};
    }

    private void applyGlobal(final IdentraPlaces.Place p) {
        final Context ctx = requireContext().getApplicationContext();
        final double[] c = finalCoords(p);
        final String mode = mode(), acc = accuracy();
        IdentraData.EXECUTOR.submit(() -> {
            A_CODE code;
            try { code = IdentraLocation.setGlobal(ctx, c[0], c[1], mode, acc); }
            catch (Throwable t) { Log.e(TAG, "Set global failed: " + t); code = A_CODE.FAILED; }
            final A_CODE fc = code;
            main.post(() -> {
                if (!isAdded()) return;
                if (A_CODE.isSuccessful(fc)) {
                    IdentraPlaces.Place applied = new IdentraPlaces.Place(p.name, p.detail, c[0], c[1]);
                    IdentraPlaces.addRecent(ctx, p);
                    IdentraPlaces.rememberApplied(ctx, null, 0, applied);
                    snack(getString(R.string.identra_loc_set_all_done, p.name));
                    renderRecent();
                    refreshActive();
                } else {
                    snack(getString(R.string.identra_change_failed, String.valueOf(fc)));
                }
            });
        });
    }

    private void applyApp(final AppXpPacket app, final IdentraPlaces.Place p) {
        final Context ctx = requireContext().getApplicationContext();
        final double[] c = finalCoords(p);
        final String mode = mode(), acc = accuracy();
        final boolean kill = app.forceStop;
        IdentraData.EXECUTOR.submit(() -> {
            A_CODE code;
            try { code = IdentraLocation.setForApp(ctx, app.uid, app.packageName, c[0], c[1], mode, acc, kill); }
            catch (Throwable t) { Log.e(TAG, "Set app failed: " + t); code = A_CODE.FAILED; }
            final A_CODE fc = code;
            main.post(() -> {
                if (!isAdded()) return;
                if (A_CODE.isSuccessful(fc)) {
                    IdentraPlaces.addRecent(ctx, p);
                    IdentraPlaces.rememberApplied(ctx, app.packageName, app.uid, new IdentraPlaces.Place(p.name, p.detail, c[0], c[1]));
                    renderRecent();
                    refreshActive();
                    offerHooks(app, getString(R.string.identra_loc_set_app_done, p.name, IdentraData.labelOf(app)));
                    if (targetPkg != null && targetPkg.equals(app.packageName) && targetUid == app.uid) clearTarget();
                } else {
                    snack(getString(R.string.identra_change_failed, String.valueOf(fc)));
                }
            });
        });
    }

    /** The location only reaches an app with the Spoof.Location hooks; offer to turn them on. */
    private void offerHooks(final AppXpPacket app, String message) {
        IdentraData.Snapshot s = IdentraData.cached();
        final List<XHook> hooks = new ArrayList<>();
        boolean on = false;
        if (s != null) {
            for (XHook h : IdentraData.availableHooks(s, app))
                if (h.group != null && h.group.equalsIgnoreCase(IdentraLocation.GROUP)) hooks.add(h);
            synchronized (app.assignments) {
                for (AssignmentPacket a : app.assignments) {
                    String g = IdentraData.groupOfAssignment(s, a);
                    if (g != null && g.equalsIgnoreCase(IdentraLocation.GROUP)) { on = true; break; }
                }
            }
        }
        View root = getView();
        if (root == null) return;
        if (on || hooks.isEmpty()) {
            Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
            return;
        }
        Snackbar.make(root, getString(R.string.identra_loc_hooks_off, IdentraData.labelOf(app)), Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.identra_loc_turn_on, x -> IdentraData.assign(requireContext(), app, hooks, code -> main.post(() -> {
                    if (isAdded()) snack(A_CODE.isSuccessful(code) ? getString(R.string.identra_loc_hooks_on, IdentraData.labelOf(app))
                            : getString(R.string.identra_change_failed, String.valueOf(code)));
                })))
                .show();
    }

    // ---- active location -------------------------------------------------------------

    private String activeLabel() {
        if (activeGlobal == null || !activeGlobal.set) return getString(R.string.identra_loc_not_set);
        String n = IdentraPlaces.nameFor(requireContext(), null, 0, activeGlobal.lat, activeGlobal.lon);
        return n != null ? n : activeGlobal.coords();
    }

    private static final class Override_ {
        AppXpPacket app;
        IdentraLocation.Value value;
    }

    private void refreshActive() {
        if (!isAdded()) return;
        final Context ctx = requireContext().getApplicationContext();
        IdentraData.ensure(ctx, s -> {
            final List<AppXpPacket> prot = IdentraData.protectedApps(s);
            IdentraData.EXECUTOR.submit(() -> {
                final IdentraLocation.Value g = IdentraLocation.readGlobal(ctx);
                final List<Override_> overrides = new ArrayList<>();
                for (AppXpPacket a : prot) {
                    IdentraLocation.Value v = IdentraLocation.readForApp(ctx, a.uid, a.packageName);
                    if (v != null) {
                        Override_ o = new Override_();
                        o.app = a;
                        o.value = v;
                        overrides.add(o);
                    }
                }
                main.post(() -> {
                    if (!isAdded() || getView() == null) return;
                    activeGlobal = g;
                    renderActive(overrides);
                    placeActiveMarker();
                    if (selected == null && g.set && map != null && map.getZoomLevelDouble() < 5) moveTo(g.lat, g.lon, false);
                });
            });
        });
    }

    private void renderActive(List<Override_> overrides) {
        activeBox.removeAllViews();
        final Context c = requireContext();
        String gSub;
        if (activeGlobal != null && activeGlobal.set) {
            String n = IdentraPlaces.nameFor(c, null, 0, activeGlobal.lat, activeGlobal.lon);
            gSub = (n != null ? n + " · " : "") + IdentraPlaces.shortCoords(activeGlobal.lat, activeGlobal.lon) + modeSuffix(activeGlobal);
        } else {
            gSub = getString(R.string.identra_loc_not_set_desc);
        }
        IdentraRows.row(activeBox, R.drawable.ic_identra_globe, getString(R.string.identra_loc_all_apps), gSub,
                activeGlobal != null && activeGlobal.set ? (View.OnClickListener) x -> moveTo(activeGlobal.lat, activeGlobal.lon, true) : null);
        for (final Override_ o : overrides) {
            String n = IdentraPlaces.nameFor(c, o.app.packageName, o.app.uid, o.value.lat, o.value.lon);
            String sub = (n != null ? n + " · " : "") + IdentraPlaces.shortCoords(o.value.lat, o.value.lon) + modeSuffix(o.value);
            View row = IdentraRows.row(activeBox, 0, IdentraData.labelOf(o.app), sub, x -> showOverrideSheet(o));
            ImageView icon = row.findViewById(R.id.identraRowIcon);
            icon.setVisibility(View.VISIBLE);
            if (o.app.icon <= 0) icon.setImageResource(android.R.drawable.sym_def_app_icon);
            else UserClientAppContext.attachIcon(c, IdentraRows.dp(c, 24), icon, o.app.packageName, o.app.icon);
        }
    }

    private String modeSuffix(IdentraLocation.Value v) {
        return IdentraLocation.TYPE_COARSE.equalsIgnoreCase(v.type) ? " · " + getString(R.string.identra_loc_mode_coarse) : "";
    }

    private void showOverrideSheet(final Override_ o) {
        IdentraSheet sheet = new IdentraSheet(requireContext())
                .title(IdentraData.labelOf(o.app))
                .sub(getString(R.string.identra_loc_override_desc));
        sheet.field(getString(R.string.identra_loc_latitude), IdentraLocation.format(o.value.lat));
        sheet.field(getString(R.string.identra_loc_longitude), IdentraLocation.format(o.value.lon));
        sheet.field(getString(R.string.identra_loc_mode), o.value.type);
        sheet.field(getString(R.string.identra_loc_accuracy), o.value.accuracy);
        sheet.action(IdentraSheet.STYLE_TONAL, getString(R.string.identra_loc_show_on_map), x -> moveTo(o.value.lat, o.value.lon, true), true);
        sheet.action(IdentraSheet.STYLE_TEXT, getString(R.string.identra_loc_remove_override), x -> removeOverride(o.app), true);
        sheet.show();
    }

    private void removeOverride(final AppXpPacket app) {
        final Context ctx = requireContext().getApplicationContext();
        final boolean kill = app.forceStop;
        IdentraData.EXECUTOR.submit(() -> {
            A_CODE code;
            try { code = IdentraLocation.clearForApp(ctx, app.uid, app.packageName, kill); }
            catch (Throwable t) { code = A_CODE.FAILED; }
            final A_CODE fc = code;
            main.post(() -> {
                if (!isAdded()) return;
                if (A_CODE.isSuccessful(fc)) {
                    IdentraPlaces.forgetApplied(ctx, app.packageName, app.uid);
                    snack(getString(R.string.identra_loc_override_removed, IdentraData.labelOf(app)));
                } else {
                    snack(getString(R.string.identra_change_failed, String.valueOf(fc)));
                }
                refreshActive();
            });
        });
    }

    // ---- favourites / recent -----------------------------------------------------------

    private void renderFavourites() {
        if (favs == null || !isAdded()) return;
        favs.removeAllViews();
        List<IdentraPlaces.Place> list = IdentraPlaces.favourites(requireContext());
        View root = getView();
        if (root != null) root.findViewById(R.id.locFavsEmpty).setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        for (final IdentraPlaces.Place p : list) {
            Chip chip = (Chip) LayoutInflater.from(requireContext()).inflate(R.layout.item_identra_place_chip, favs, false);
            chip.setText(p.name);
            chip.setOnClickListener(x -> select(p, true, true));
            chip.setOnLongClickListener(x -> { editFavourite(p); return true; });
            favs.addView(chip);
        }
    }

    private void editFavourite(final IdentraPlaces.Place p) {
        final IdentraSheet sheet = new IdentraSheet(requireContext()).title(getString(R.string.identra_loc_edit_favourite)).sub(p.coords());
        final TextInputEditText name = sheet.input(getString(R.string.identra_loc_name), p.name, false);
        sheet.action(IdentraSheet.STYLE_FILLED, getString(R.string.identra_save), x -> {
            String n = name.getText() == null ? "" : name.getText().toString().trim();
            if (!n.isEmpty()) IdentraPlaces.renameFavourite(requireContext(), p, n);
            renderFavourites();
        }, true);
        sheet.action(IdentraSheet.STYLE_TEXT, getString(R.string.identra_delete), x -> {
            IdentraPlaces.removeFavourite(requireContext(), p);
            renderFavourites();
            updateStar();
        }, true);
        sheet.show();
    }

    private void renderRecent() {
        if (recentBox == null || !isAdded()) return;
        recentBox.removeAllViews();
        List<IdentraPlaces.Place> list = IdentraPlaces.recent(requireContext());
        View root = getView();
        if (root != null) {
            root.findViewById(R.id.locRecentCard).setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
            root.findViewById(R.id.locRecentEmpty).setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            root.findViewById(R.id.locRecentClear).setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        }
        for (final IdentraPlaces.Place p : list) {
            String sub = (p.detail == null || p.detail.isEmpty() ? "" : p.detail + " · ") + IdentraPlaces.shortCoords(p.lat, p.lon);
            IdentraRows.row(recentBox, R.drawable.ic_identra_history, p.name, sub, x -> select(p, true, true));
        }
    }

    // ---- advanced ----------------------------------------------------------------------

    private void renderAdvanced() {
        advancedBox.removeAllViews();
        final Context c = requireContext();
        View head = IdentraRows.row(advancedBox, R.drawable.ic_identra_tune, getString(R.string.identra_section_advanced),
                getString(R.string.identra_loc_advanced_desc), x -> { advancedOpen = !advancedOpen; renderAdvanced(); });
        head.findViewById(R.id.identraRowChevron).setRotation(advancedOpen ? 90f : 0f);
        if (!advancedOpen) return;

        View modeView = LayoutInflater.from(c).inflate(R.layout.item_identra_loc_mode, advancedBox, false);
        ChipGroup modes = modeView.findViewById(R.id.locModeChips);
        modes.check(IdentraLocation.TYPE_COARSE.equals(mode()) ? R.id.locModeCoarse : R.id.locModeExact);
        modes.setOnCheckedStateChangeListener((g, ids) -> {
            if (ids.isEmpty()) return;
            String m = ids.get(0) == R.id.locModeCoarse ? IdentraLocation.TYPE_COARSE : IdentraLocation.TYPE_SET;
            IdentraPlaces.prefs(c).edit().putString(IdentraPlaces.KEY_MODE, m).apply();
        });
        advancedBox.addView(modeView);

        final EditText acc = addNumberField(getString(R.string.identra_loc_accuracy_hint), accuracy());
        acc.addTextChangedListener(simpleWatcher(t -> IdentraPlaces.prefs(c).edit().putString(IdentraPlaces.KEY_ACCURACY, t).apply()));

        MaterialSwitch jitter = IdentraRows.switchRow(advancedBox, 0, getString(R.string.identra_loc_jitter),
                getString(R.string.identra_loc_jitter_desc), jitterOn(), (b, on) -> {
                    IdentraPlaces.prefs(c).edit().putBoolean(IdentraPlaces.KEY_JITTER_ON, on).apply();
                    renderAdvanced();
                });
        if (jitterOn()) {
            final EditText meters = addNumberField(getString(R.string.identra_loc_jitter_hint), String.valueOf(jitterMeters()));
            meters.addTextChangedListener(simpleWatcher(t -> {
                int m;
                try { m = Integer.parseInt(t.trim()); } catch (Exception e) { m = 0; }
                IdentraPlaces.prefs(c).edit().putInt(IdentraPlaces.KEY_JITTER_M, Math.max(0, Math.min(m, 100000))).apply();
            }));
        }
        if (jitter == null) Log.w(TAG, "no jitter switch");
    }

    private EditText addNumberField(String hint, String value) {
        View field = LayoutInflater.from(requireContext()).inflate(R.layout.item_identra_text_field, advancedBox, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int m = IdentraRows.dp(requireContext(), 16);
        lp.setMargins(m, IdentraRows.dp(requireContext(), 4), m, IdentraRows.dp(requireContext(), 8));
        field.setLayoutParams(lp);
        ((com.google.android.material.textfield.TextInputLayout) field).setHint(hint);
        EditText et = field.findViewById(R.id.identraFieldEdit);
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setText(value);
        advancedBox.addView(field);
        return et;
    }

    private interface TextChanged { void on(String t); }

    private static TextWatcher simpleWatcher(final TextChanged l) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { l.on(s == null ? "" : s.toString()); }
        };
    }

    // ---- target app (opened from an app's detail screen) ------------------------------

    /** Opens the tab in "choose for this app" mode. */
    public void setTarget(String pkg, int uid) {
        this.targetPkg = pkg;
        this.targetUid = uid;
        if (getView() != null) renderTarget();
    }

    private AppXpPacket targetApp() {
        if (targetPkg == null) return null;
        return IdentraData.findApp(IdentraData.cached(), targetPkg, targetUid);
    }

    private void clearTarget() {
        targetPkg = null;
        renderTarget();
    }

    private void renderTarget() {
        if (targetCard == null) return;
        final AppXpPacket app = targetApp();
        if (targetPkg != null && app == null) {
            IdentraData.ensure(requireContext(), s -> { if (isAdded() && IdentraData.findApp(s, targetPkg, targetUid) != null) renderTarget(); });
        }
        if (app == null) {
            targetCard.setVisibility(View.GONE);
            selApp.setText(R.string.identra_loc_set_app);
            selApp.setOnClickListener(x -> { if (selected != null) chooseAppFor(selected); });
            return;
        }
        targetCard.setVisibility(View.VISIBLE);
        targetText.setText(getString(R.string.identra_loc_target, IdentraData.labelOf(app)));
        if (app.icon <= 0) targetIcon.setImageResource(android.R.drawable.sym_def_app_icon);
        else UserClientAppContext.attachIcon(requireContext(), IdentraRows.dp(requireContext(), 32), targetIcon, app.packageName, app.icon);
        selApp.setText(getString(R.string.identra_loc_set_for, IdentraData.labelOf(app)));
        selApp.setOnClickListener(x -> { if (selected != null) applyApp(app, selected); });
    }

    private void snack(String text) {
        View v = getView();
        if (v != null) Snackbar.make(v, text, Snackbar.LENGTH_LONG).show();
    }
}
