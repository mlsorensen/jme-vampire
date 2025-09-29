package com.turboio.games.vampires.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.scene.Node;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.style.BaseStyles;
import com.turboio.games.vampires.story.StoryLoader;

public class StartScreenAppState extends BaseAppState {

    private Container window;

    @Override
    protected void initialize(Application app) {
        GuiGlobals.initialize(app);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");

        window = new Container();
        ((SimpleApplication) app).getGuiNode().attachChild(window);

        Button startButton = window.addChild(new Button("Start"));
        startButton.setFontSize(24f);
        startButton.addClickCommands((Command<Button>) source -> startGame());

        window.setLocalTranslation(
            (app.getCamera().getWidth() - window.getPreferredSize().x) / 2,
            (app.getCamera().getHeight() + window.getPreferredSize().y) / 2,
            0
        );
    }

    private void startGame() {
        setEnabled(false);
        getStateManager().attach(new StoryAppState("storylines/storyline1.json"));
        getStateManager().detach(this);
    }

    @Override
    protected void cleanup(Application app) {
        if (window != null) {
            window.removeFromParent();
        }
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
    }
}
