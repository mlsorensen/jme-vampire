package com.turboio.games.vampires.audio;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;

public class Sound {
    private final AssetManager assetManager;
    private AudioNode backgroundMusic;

    public Sound(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public void playMusic(String path) {
        stopMusic();
        backgroundMusic = new AudioNode(assetManager, path, AudioData.DataType.Stream);
        backgroundMusic.setPositional(false);
        backgroundMusic.setReverbEnabled(false);
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(1);
        backgroundMusic.play();
    }

    public void stopMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic = null;
        }
    }
}
