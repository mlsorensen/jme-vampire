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
        
        // Configure camera to see screen coordinates for rootNode elements
        float width = settings.getWidth();
        float height = settings.getHeight();
        cam.setFrustum(1, 1000, 0, width, 0, height);
        cam.setLocation(new Vector3f(width / 2f, height / 2f, 10f));
        
        getFlyByCamera().setEnabled(false);
        setDisplayStatView(SHOW_STATS);
        setDisplayFps(SHOW_STATS);

        // Apply bloom filter to both viewports for complete coverage
        FilterPostProcessor guiFpp = new FilterPostProcessor(assetManager);
        BloomFilter guiBloom = new BloomFilter(BloomFilter.GlowMode.Objects);
        guiBloom.setBloomIntensity(2.5f);
        guiBloom.setExposurePower(2.0f);
        guiFpp.addFilter(guiBloom);
        guiViewPort.addProcessor(guiFpp);
        
        FilterPostProcessor mainFpp = new FilterPostProcessor(assetManager);
        BloomFilter mainBloom = new BloomFilter(BloomFilter.GlowMode.Objects);
        mainBloom.setBloomIntensity(2.5f);
        mainBloom.setExposurePower(2.0f);
        mainFpp.addFilter(mainBloom);
        viewPort.addProcessor(mainFpp);

        stateManager.attach(new StartScreenAppState());
        inputManager.setCursorVisible(true);
    }

    @Override
    public void destroy() {
        System.exit(0);
    }
}
