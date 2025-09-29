package com.turboio.games.vampires;

import com.jme3.app.SimpleApplication;
import com.jme3.math.Vector3f;
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

        stateManager.attach(new StartScreenAppState());
        inputManager.setCursorVisible(true);
    }

    @Override
    public void destroy() {
        System.exit(0);
    }
}
