package com.turboio.games.vampires.audio;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;

import java.util.HashMap;
import java.util.Map;

public class Sound {
    private final AssetManager assetManager;
    private AudioNode backgroundMusic;
    private final Map<String, AudioNode> loopingSounds = new HashMap<>();

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

    public void playSound(String path, float volume) {
        AudioNode sound = new AudioNode(assetManager, path, AudioData.DataType.Buffer);
        sound.setPositional(false);
        sound.setLooping(false);
        sound.setVolume(volume);
        sound.playInstance();
    }

    public void startLoop(String path, String id, float volume) {
        if (loopingSounds.containsKey(id)) {
            return; // Already playing
        }
        AudioNode loop = new AudioNode(assetManager, path, AudioData.DataType.Stream);
        loop.setPositional(false);
        loop.setLooping(true);
        loop.setVolume(volume);
        loopingSounds.put(id, loop);
        loop.play();
    }

    public void stopLoop(String id) {
        AudioNode loop = loopingSounds.remove(id);
        if (loop != null) {
            loop.stop();
        }
    }

    public void stopAllSounds() {
        stopMusic();
        for (AudioNode loop : loopingSounds.values()) {
            loop.stop();
        }
        loopingSounds.clear();
    }
}
