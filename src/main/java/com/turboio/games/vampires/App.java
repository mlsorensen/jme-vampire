package com.turboio.games.vampires;

import com.jme3.app.SimpleApplication;
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.GuiGlobals;
import com.turboio.games.vampires.states.StartScreenAppState;

/**
 * The main entry point for the game.
 */
public class App extends SimpleApplication {

    public static void main(String[] args) {
        App app = new App();
        AppSettings settings = new AppSettings(true);
        settings.setWidth(1600);
        settings.setHeight(1200);
        settings.setTitle("Vampires");
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // setup camera for 2D games
        cam.setParallelProjection(true);
        cam.setLocation(new Vector3f(0, 0, 0.5f));
        getFlyByCamera().setEnabled(false);

        // We start the game by attaching the start screen app state
        stateManager.attach(new StartScreenAppState());
    }

    @Override
    public void destroy() {
        System.exit(0);
    }
}
