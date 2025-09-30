package com.turboio.games.vampires;

import com.jme3.app.SimpleApplication;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.system.AppSettings;
import com.turboio.games.vampires.states.StartScreenAppState;

public class App extends SimpleApplication {

    private static final boolean SHOW_STATS = false;

    public static void main(String[] args) {
        App app = new App();
        AppSettings settings = new AppSettings(true);
        settings.setWidth(1600);
        settings.setHeight(1200);
        settings.setTitle("Vampires");
        settings.setAudioRenderer(AppSettings.LWJGL_OPENAL);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        cam.setParallelProjection(true);
        cam.setLocation(new Vector3f(0, 0, 0.5f));
        getFlyByCamera().setEnabled(false);
        setDisplayStatView(SHOW_STATS);
        setDisplayFps(SHOW_STATS);

        // Apply bloom filter to GUI viewport for glow effects
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
        bloom.setBloomIntensity(2.5f);
        bloom.setExposurePower(2.0f);
        fpp.addFilter(bloom);
        guiViewPort.addProcessor(fpp);

        stateManager.attach(new StartScreenAppState());
        inputManager.setCursorVisible(true);
    }

    @Override
    public void destroy() {
        System.exit(0);
    }
}
