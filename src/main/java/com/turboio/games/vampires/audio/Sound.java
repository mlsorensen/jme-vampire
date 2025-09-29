package com.turboio.games.vampires.audio;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;

public class Sound {
    private AudioNode backgroundMusic;

    public Sound(AssetManager assetManager) {
        loadSounds(assetManager);
    }

    private void loadSounds(AssetManager assetManager) {
        backgroundMusic = new AudioNode(assetManager, "Audio/vampires.wav", AudioData.DataType.Buffer);
        backgroundMusic.setPositional(false);
        backgroundMusic.setReverbEnabled(false);
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(1);
    }

    public void playMusic() {
        backgroundMusic.play();
    }

    public void stopMusic() {
        backgroundMusic.stop();
    }
}
