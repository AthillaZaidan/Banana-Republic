package com.bananarepublic.ui;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

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
    private boolean gameBgmToggle = false; // alternates game_1 / game_2
    private double bgmVolume = 0.45;
    private double sfxVolume = 0.80;
    private boolean muted = false;

    private AudioEngine() {}

    // ── BGM ──────────────────────────────────────────────────────────────────

    public void playMenuBgm() {
        switchBgm(BGM_MENU, true);
    }

    public void playGameBgm() {
        gameBgmToggle = false;
        switchBgm(BGM_GAME_1, false);
    }

    public void stopBgm() {
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer.dispose();
            bgmPlayer = null;
        }
    }

    private void switchBgm(String resource, boolean loop) {
        stopBgm();
        Media media = loadMedia(resource);
        if (media == null) return;

        bgmPlayer = new MediaPlayer(media);
        bgmPlayer.setVolume(muted ? 0 : bgmVolume);
        if (loop) {
            bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        } else {
            bgmPlayer.setOnEndOfMedia(this::onGameTrackEnd);
        }
        bgmPlayer.play();
    }

    private void onGameTrackEnd() {
        // Alternate between game_1 and game_2 seamlessly
        gameBgmToggle = !gameBgmToggle;
        switchBgm(gameBgmToggle ? BGM_GAME_2 : BGM_GAME_1, false);
    }

    // ── SFX ──────────────────────────────────────────────────────────────────

    public void playSfx(Sfx sfx) {
        if (muted) return;
        Media media = loadMedia(sfx.path);
        if (media == null) return;
        MediaPlayer player = new MediaPlayer(media);
        player.setVolume(sfxVolume);
        // Dispose after playback — fire and forget
        player.setOnEndOfMedia(player::dispose);
        player.setOnError(player::dispose);
        player.play();
    }

    // ── Volume / Mute ─────────────────────────────────────────────────────────

    public void setBgmVolume(double v) {
        bgmVolume = clamp(v);
        if (bgmPlayer != null && !muted) bgmPlayer.setVolume(bgmVolume);
    }

    public void setSfxVolume(double v) {
        sfxVolume = clamp(v);
    }

    public void setMuted(boolean m) {
        muted = m;
        if (bgmPlayer != null) bgmPlayer.setVolume(muted ? 0 : bgmVolume);
    }

    public boolean isMuted() { return muted; }
    public double getBgmVolume() { return bgmVolume; }
    public double getSfxVolume() { return sfxVolume; }

    // ── Util ──────────────────────────────────────────────────────────────────

    private static Media loadMedia(String resource) {
        URL url = AudioEngine.class.getResource(resource);
        if (url == null) return null;
        try {
            return new Media(url.toExternalForm());
        } catch (Exception e) {
            return null;
        }
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
