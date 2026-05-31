package com.bananarepublic.ui;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;

public final class AudioEngine {

    public enum Sfx {
        CLICK       ("/audio/sfx/click.wav"),
        ERROR       ("/audio/sfx/error.wav"),
        BUILD       ("/audio/sfx/build.mp3"),
        DICE        ("/audio/sfx/dice.mp3"),
        TRADE       ("/audio/sfx/trade.wav"),
        ACHIEVEMENT ("/audio/sfx/achievement.wav"),
        NIMON_UNGU  ("/audio/sfx/nimon_ungu.wav"),
        DAGGER      ("/audio/sfx/dagger.wav");

        final String path;
        Sfx(String path) { this.path = path; }
    }

    private static final String BGM_MENU   = "/audio/bgm/main_menu.mp3";
    private static final String BGM_GAME_1 = "/audio/bgm/game_1.mp3";
    private static final String BGM_GAME_2 = "/audio/bgm/game_2.mp3";

    private static final AudioEngine INSTANCE = new AudioEngine();
    public static AudioEngine get() { return INSTANCE; }

    private MediaPlayer bgmPlayer;
    private boolean gameBgmToggle = false;
    private double bgmVolume = 0.45;
    private double sfxVolume = 0.80;
    private boolean muted = false;

    private AudioEngine() {}

    // ── BGM ──────────────────────────────────────────────────────────────────

    public void playMenuBgm() {
        runOnFx(() -> switchBgm(BGM_MENU, true));
    }

    public void playGameBgm() {
        runOnFx(() -> {
            gameBgmToggle = false;
            switchBgm(BGM_GAME_1, false);
        });
    }

    public void stopBgm() {
        runOnFx(() -> {
            if (bgmPlayer != null) {
                bgmPlayer.stop();
                bgmPlayer.dispose();
                bgmPlayer = null;
            }
        });
    }

    private void switchBgm(String resource, boolean loop) {
        // Must run on FX thread
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer.dispose();
            bgmPlayer = null;
        }
        Media media = loadMedia(resource);
        if (media == null) return;

        MediaPlayer player = new MediaPlayer(media);
        player.setVolume(muted ? 0 : bgmVolume);
        player.setOnError(() -> System.err.println(
                "[AudioEngine] BGM error: " + player.getError()));
        if (loop) {
            player.setCycleCount(MediaPlayer.INDEFINITE);
        } else {
            player.setOnEndOfMedia(this::onGameTrackEnd);
        }
        bgmPlayer = player;
        player.play();
    }

    private void onGameTrackEnd() {
        gameBgmToggle = !gameBgmToggle;
        switchBgm(gameBgmToggle ? BGM_GAME_2 : BGM_GAME_1, false);
    }

    // ── SFX ──────────────────────────────────────────────────────────────────

    public void playSfx(Sfx sfx) {
        playSfx(sfx, -1);
    }

    // stopAfterMillis <= 0 means play to end
    public void playSfx(Sfx sfx, double stopAfterMillis) {
        if (muted) return;
        runOnFx(() -> {
            Media media = loadMedia(sfx.path);
            if (media == null) {
                System.err.println("[AudioEngine] SFX not found: " + sfx.path);
                return;
            }
            MediaPlayer player = new MediaPlayer(media);
            player.setVolume(sfxVolume);
            if (stopAfterMillis > 0) {
                player.setStopTime(javafx.util.Duration.millis(stopAfterMillis));
            }
            player.setOnError(() -> System.err.println(
                    "[AudioEngine] SFX error (" + sfx.path + "): " + player.getError()));
            player.setOnEndOfMedia(() -> runOnFx(player::dispose));
            player.setOnStopped(() -> runOnFx(player::dispose));
            player.play();
        });
    }

    // ── Volume / Mute ─────────────────────────────────────────────────────────

    public void setBgmVolume(double v) {
        bgmVolume = clamp(v);
        runOnFx(() -> {
            if (bgmPlayer != null && !muted) bgmPlayer.setVolume(bgmVolume);
        });
    }

    public void setSfxVolume(double v) {
        sfxVolume = clamp(v);
    }

    public void setMuted(boolean m) {
        muted = m;
        runOnFx(() -> {
            if (bgmPlayer != null) bgmPlayer.setVolume(muted ? 0 : bgmVolume);
        });
    }

    public boolean isMuted()       { return muted; }
    public double getBgmVolume()   { return bgmVolume; }
    public double getSfxVolume()   { return sfxVolume; }

    // ── Util ──────────────────────────────────────────────────────────────────

    private static void runOnFx(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    private static Media loadMedia(String resource) {
        URL url = AudioEngine.class.getResource(resource);
        if (url == null) {
            System.err.println("[AudioEngine] Resource not found: " + resource);
            return null;
        }
        try {
            return new Media(url.toExternalForm());
        } catch (Exception e) {
            System.err.println("[AudioEngine] Failed to load media: " + resource + " — " + e.getMessage());
            return null;
        }
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
