package dev.nxnative.android;

import android.content.Context;
import android.content.SharedPreferences;

/** The launcher's own settings (the on-screen controls keep theirs in TouchControls): the overlay over the game, frame generation. */
final class Prefs {
    private Prefs() {}

    /**
     * A settings file, read again whenever another process has written it. The launcher and the game (GameActivity, in its own process :game) each keep the file in memory:
     * with MODE_PRIVATE a change made in the game's panel stayed unseen by the launcher, whose next write put its old copy back over it, so in-game settings came back
     * as they were. MODE_MULTI_PROCESS reloads the file on each getSharedPreferences when it changed on disk (deprecated, still honoured), so every access goes through
     * here, and writes use commit(): written before the call returns, so Stop (which ends the game's process) cannot drop one still queued by apply().
     */
    @SuppressWarnings("deprecation")
    static SharedPreferences open(Context c, String name) { return c.getSharedPreferences(name, Context.MODE_MULTI_PROCESS); }
    static SharedPreferences of(Context c) { return open(c, "launcher"); }

    /** The overlay over the game (master switch: the frame rate and the other items top right, Overlay; the build tag top left is always shown); on by default. DM_OVERLAY=0 / 1 in env.txt overrides it for a run. */
    static boolean overlay(Context c) { return of(c).getBoolean("overlay", true); }
    static void overlay(Context c, boolean on) { of(c).edit().putBoolean("overlay", on).commit(); }

    /** One item of the overlay (Overlay.ITEMS); all on by default, shown only while the overlay is on */
    static boolean overlayItem(Context c, String key) { return of(c).getBoolean("overlay_" + key, true); }
    static void overlayItem(Context c, String key, boolean on) { of(c).edit().putBoolean("overlay_" + key, on).commit(); }

    /** The game's window size (Game resolution): DM_WINDOW_SIZE for the host (loader.c window_apply), "1280x720" (720p, the default), "960x540" (540p), "640x360" (360p) or a custom size WxH. DM_WINDOW_SIZE in env.txt overrides it for a run.
     *  A game whose profile has graphics_scale rows gets the graphics scale and operation mode that give the size instead (Profile.resolutionEnv). */
    static final String[] WINDOWS = {"1280x720", "960x540", "640x360"},
            WINDOW_NAMES = {"720p (1280x720)", "540p (960x540)", "360p (640x360)"},
            WINDOW_SHORT = {"720p", "540p", "360p"};

    static boolean isCustomWindow(String v) {
        return v != null && v.matches("\\d+x\\d+") && !java.util.Arrays.asList(WINDOWS).contains(v);
    }

    /** A window size's name in the lists, its short name for a settings row (short); the value itself when it is none of WINDOWS (an env.txt-style size or custom) */
    static String windowName(String v, boolean shortName) {
        int i = java.util.Arrays.asList(WINDOWS).indexOf(v);
        if (i >= 0) return shortName ? WINDOW_SHORT[i] : WINDOW_NAMES[i];
        if (isCustomWindow(v)) return shortName ? "Custom (" + v + ")" : "Custom (" + v + ")";
        return v != null ? v : "";
    }
    static String window(Context c) {
        String v = of(c).getString("window", WINDOWS[0]);
        if ("1920x1080".equals(v) || "original".equals(v)) {
            v = WINDOWS[0];
            window(c, v);
        }
        return v;
    }
    static void window(Context c, String v) { of(c).edit().putString("window", v).commit(); }

    // ---- A game's own settings (its Manage sheet › Game settings, the in-game panel), stored per title ID as "<key>@<title ID>"; a game without its own value
    // takes the default (the main screen's Settings, global). DKB with nothing of its own set runs exactly as before ----
    static String gameKey(String key, String titleId) { return key + "@" + titleId; }

    /** The game's window size: its own choice, else the default (defaultWindow) */
    static String gameWindow(Context c, String titleId) {
        String v = of(c).getString(gameKey("window", titleId), defaultWindow(c, titleId));
        if ("1920x1080".equals(v) || "original".equals(v)) {
            v = defaultWindow(c, titleId);
            gameWindow(c, titleId, v);
        }
        return v;
    }
    /** A game's default window size: Phone 720p in Low RAM mode, else the main screen's when the player chose one there, else its profile's android_window, else 720p */
    static String defaultWindow(Context c, String titleId) {
        if (gameLowRam(c, titleId)) return "1280x720";
        if (of(c).contains("window")) return window(c);
        Profile p = Profile.of(titleId);
        return p != null && java.util.Arrays.asList(WINDOWS).contains(p.window) ? p.window : WINDOWS[0];
    }
    /** null: back to the default */
    static void gameWindow(Context c, String titleId, String v) { put(c, gameKey("window", titleId), v); }
    static boolean ownWindow(Context c, String titleId) { return of(c).contains(gameKey("window", titleId)); }

    /** Whether this phone has <= 8GB physical RAM (threshold 10.5 GiB covers 8GB devices with RAM Plus / Memory Extension enabled) */
    static boolean isLowRamDevice(Context c) {
        android.app.ActivityManager am = (android.app.ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;
        android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.totalMem <= 11274289152L; // 10.5 GiB
    }

    /** Low RAM mode (optimized for <= 8GB RAM devices like Snapdragon 870): aggressive trim, 16MB slabs, prefault off, 720p default */
    static boolean lowRam(Context c) { return of(c).getBoolean("low_ram", isLowRamDevice(c)); }
    static void lowRam(Context c, boolean on) { of(c).edit().putBoolean("low_ram", on).commit(); }

    static boolean gameLowRam(Context c, String titleId) {
        return of(c).getBoolean(gameKey("low_ram", titleId), lowRam(c));
    }
    static void gameLowRam(Context c, String titleId, Boolean v) {
        if (v == null) of(c).edit().remove(gameKey("low_ram", titleId)).commit();
        else of(c).edit().putBoolean(gameKey("low_ram", titleId), v).commit();
    }
    static boolean ownLowRam(Context c, String titleId) { return of(c).contains(gameKey("low_ram", titleId)); }

    /**
     * The frame rate cap (the in-game panel): 60 or 30 fps. DM_FPS_TARGET for the host at start, and Native.fpsCapSet while the game runs.
     */
    static final int[] FPS_CAPS = {60, 30};
    /** A stored cap as one of FPS_CAPS: 30 or 60 */
    static int capOf(int v) {
        if (v == 30) return 30;
        return 60;
    }
    static int fpsCap(Context c) { return capOf(of(c).getInt("fps_cap", 60)); }
    static void fpsCap(Context c, int v) { of(c).edit().putInt("fps_cap", v).commit(); }
    /** The game's cap: its own (set in its Game settings or its in-game panel), else the default */
    static int fpsCap(Context c, String titleId) { return capOf(of(c).getInt(gameKey("fps_cap", titleId), fpsCap(c))); }

    /**
     * Whether the host found that this game picks its own frame rate (its cap status reports no rate it can hold: Native.fpsCapStatus rates 0), as the last run
     * saw it: its Game settings and its in-game panel then show no cap choices, which would do nothing. Written by GameActivity and the panel while the game runs.
     */
    static boolean ownRate(Context c, String titleId) { return of(c).getBoolean(gameKey("own_rate", titleId), false); }
    static void ownRate(Context c, String titleId, boolean v) { if (ownRate(c, titleId) != v) of(c).edit().putBoolean(gameKey("own_rate", titleId), v).commit(); }
    /** null: back to the default */
    static void fpsCap(Context c, String titleId, Integer v) {
        if (v == null) of(c).edit().remove(gameKey("fps_cap", titleId)).commit();
        else of(c).edit().putInt(gameKey("fps_cap", titleId), v).commit();
    }
    static boolean ownFpsCap(Context c, String titleId) { return of(c).contains(gameKey("fps_cap", titleId)); }

    /** Pause on return (per game, on by default): the game's screen coming back from the background shows it paused, suspended until a tap (GameActivity.onStart) */
    static boolean pauseOnReturn(Context c, String titleId) { return of(c).getBoolean(gameKey("pause_return", titleId), true); }
    static void pauseOnReturn(Context c, String titleId, boolean on) { of(c).edit().putBoolean(gameKey("pause_return", titleId), on).commit(); }

    /**
     * The game's own environment lines (KEY=VALUE, one a line; its Game settings), given to the host after the launcher's settings and env.txt, so they win for
     * this game; env-&lt;title ID&gt;.txt in the app's folder (adb) comes after them. "" = none
     */
    /** GPU timing (Game settings): "segment" or "submission" passed to the host as DM_NVN_GPU_TS, null for the game's default (its profile's gpu_timestamps) */
    static String gpuTs(Context c, String titleId) { return of(c).getString(gameKey("gpu_ts", titleId), null); }
    static void gpuTs(Context c, String titleId, String mode) { put(c, gameKey("gpu_ts", titleId), mode); }

    static String gameEnv(Context c, String titleId) { return of(c).getString(gameKey("env", titleId), ""); }
    static void gameEnv(Context c, String titleId, String text) { put(c, gameKey("env", titleId), text == null || text.trim().isEmpty() ? null : text.trim()); }

    private static void put(Context c, String key, String v) {
        if (v == null) of(c).edit().remove(key).commit();
        else of(c).edit().putString(key, v).commit();
    }

    /**
     * Folders the user picked (main screen, Folders…), as /storage paths; null = the default. Kept in the app's preferences, so an APK update keeps them (an
     * uninstall does not). "pack_<title ID>": the game's pack folder (pack.tsv and shaders/; default the app's packs/<id> when it holds one, else
     * <game>/../<id>-work/android-pack, Pack.find; the older single-game "pack" key is still read when it holds a pack of that title, Folders.packFor);
     * "save_<title ID>": the game's save folder, given to the host as DM_SAVE (default the app's saves/<id>, AppStorage.save).
     */
    static String folder(Context c, String key) { return of(c).getString("folder_" + key, null); }
    static void folder(Context c, String key, String path) { of(c).edit().putString("folder_" + key, path).commit(); }

    /**
     * The default save folder of versions up to 0.1.0, on shared storage: <Switch2 root>/saves/<id> for the play app, saves-test/<id> for the others, the root
     * being the folder that holds the game folder or the .nup. The default is now the app's own saves/<id> (AppStorage.save); a save found here is copied in once
     * (SaveMigrate, GameActivity.saveDir), and this folder is never changed
     */
    static java.io.File oldSaveDefault(java.io.File game, String id) {
        return new java.io.File(game.getAbsoluteFile().getParentFile(), (BuildConfig.NX_PLAY ? "saves/" : "saves-test/") + id);
    }

    /** The old default save folder whose save was copied into app storage (SaveMigrate), null when none was. Written by the game's process (GameActivity) */
    static String migratedFrom(Context c, String titleId) { return of(c).getString(gameKey("save_migrated", titleId), null); }
    static void migratedFrom(Context c, String titleId, String path) { put(c, gameKey("save_migrated", titleId), path); }

    /** Where a game's save backups go (SaveStore): <Switch2 root>/save-backups/<id>, the root being the game folder's parent, for the play and the test app alike
     *  (each backup's manifest says which save folder it came from) */
    static java.io.File backupsDir(java.io.File game, String id) {
        return new java.io.File(game.getAbsoluteFile().getParentFile(), "save-backups/" + id);
    }

    /**
     * Which build a log or a screenshot came from. Play app: "Banana Native 0.1 · 6a6448d" (the version up to its "-", the commit);
     * test app: "Banana Native test · int@6a6448d" (the branch without "android-", the commit). "+dirty" follows the commit when the
     * tree had changes. The play app adds " · official" when the release key signed the APK, else " · unofficial build" (Signer). The main screen shows it and it
     * is the first line of every game log ("[nxlaunch] build ...").
     */
    static String buildTag(Context ctx) { return Signer.tag(buildTag(), BuildConfig.NX_PLAY, signers(ctx)); }

    private static String[] signers;

    /** The SHA-256 of each certificate that signed this APK (its current signers, not past ones of a rotated key), read once; empty when it cannot be read */
    static synchronized String[] signers(Context ctx) {
        if (signers != null) return signers;
        try {
            android.content.pm.SigningInfo si = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES).signingInfo;
            android.content.pm.Signature[] sig = si == null ? new android.content.pm.Signature[0] : si.getApkContentsSigners();
            String[] h = new String[sig.length];
            for (int i = 0; i < sig.length; i++) h[i] = Signer.sha256(sig[i].toByteArray());
            signers = h;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            signers = new String[0];
        }
        return signers;
    }

    /** Which key signed this APK, for logs and reports: the signer's SHA-256, first 16 hex digits */
    static String signerId(Context ctx) { return Signer.shortId(signers(ctx)); }

    /** The build identifier alone (Signer.buildId): "launcher-2@4a15067", "0.1 · 6a6448d". Home shows it under its title (the build tag would not fit a phone's width) */
    static String buildId() {
        return Signer.buildId(BuildConfig.NX_PLAY, BuildConfig.VERSION_NAME, BuildConfig.NX_COMMIT, BuildConfig.NX_DIRTY, BuildConfig.NX_BRANCH);
    }

    private static String buildTag() { return Signer.buildTag(BuildConfig.NX_PLAY, buildId()); }
}
