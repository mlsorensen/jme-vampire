package com.turboio.games.vampires.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.style.BaseStyles;

public class StartScreenAppState extends BaseAppState {

    private Container window;

    @Override
    protected void initialize(Application app) {
        // Initialize Lemur GUI
        GuiGlobals.initialize(app);

        // Load the 'glass' style
        BaseStyles.loadGlassStyle();

        // Set 'glass' as the default style
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");

        // Create a simple container for our elements
        window = new Container();
        ((SimpleApplication) app).getGuiNode().attachChild(window);

        // Create a 'Start' button
        Button startButton = window.addChild(new Button("Start"));
        startButton.setFontSize(24f);
        startButton.addClickCommands((Command<Button>) source -> startGame());

        // Center the window
        window.setLocalTranslation(
            (app.getCamera().getWidth() - window.getPreferredSize().x) / 2,
            (app.getCamera().getHeight() + window.getPreferredSize().y) / 2,
            0
        );
    }

    private void startGame() {
        setEnabled(false);
        getStateManager().attach(new GameAppState());
    }

    @Override
    protected void cleanup(Application app) {
        // Detach the Lemur window
        window.removeFromParent();
    }

    @Override
    protected void onEnable() {
        if (window != null) {
            window.setCullHint(Node.CullHint.Inherit);
        }
    }

    @Override
    protected void onDisable() {
        if (window != null) {
            window.setCullHint(Node.CullHint.Always);
        }
        // We detach this state, so the next time it's attached, initialize will be called again.
        getStateManager().detach(this);
    }
}
