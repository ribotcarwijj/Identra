package dev.identra;

/**
 * Identra namespaces. Identra can be installed beside the module it was derived
 * from, so every channel the two would otherwise share has its own name here:
 * the SettingsProvider command prefixes, the on-device database folder prefix
 * and the VXP authority (${applicationId}.vxp in the manifest).
 */
public final class IdentraNs {
    private IdentraNs() { }

    /** Settings.call() handler for hook/settings commands. */
    public static final String CMD_LUA = "identra";
    /** Settings.call() handler for mock/property commands. */
    public static final String CMD_MOCK = "identra_mock";
    /**
     * Version of the app <-> system_server command protocol. getVersion returns this,
     * NOT the APK versionCode, so a normal app update does not need a soft reboot.
     * Bump ONLY when the system_server side (command handlers, packets) changes
     * incompatibly; then a soft reboot is really needed.
     * <p>
     * 0.5.1: numbered from 100. Builds before 0.1.4 answered getVersion with their APK versionCode
     * (the phone's system_server still ran one answering 3), which is >= any small protocol number,
     * so an outdated system_server went unnoticed. Every versionCode those builds could answer is
     * far below 100, so "0 < answer < PROTOCOL" now reliably means "system_server runs older
     * Identra code" (see IdentraHookProvider.isOutdated).
     * <p>
     * 0.6.1: frozen at 100. The protocol only grows backward compatibly (new commands are optional and
     * the app copes when an older system part does not know them). Which build runs in system_server
     * is asked with the identraBuild command, and an older one is hot-reloaded, never restarted.
     */
    public static final int PROTOCOL = 100;

    /**
     * 0.5.0: the one hook collection Identra ships. Every hook id is "Identra.<name>"; the
     * collection setting is forced to this value and older installs are migrated on start
     * (see dev.identra.app.x.core.database.CollectionMigration).
     */
    public static final String COLLECTION = "Identra";

    /** Database folder prefix under /data/misc. */
    public static final String DB_DIR_PREFIX = "identra-";
}
