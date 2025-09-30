package com.turboio.games.vampires.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapText;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;

public class StartScreenAppState extends BaseAppState implements ActionListener {

    private static final String START_GAME = "StartGame";
    private Node guiNode;
    private BitmapText startText;
    private Picture background;

    @Override
    protected void initialize(Application app) {
        guiNode = ((SimpleApplication) app).getGuiNode();

        // Background
        background = new Picture("Start Screen Background");
        background.setImage(app.getAssetManager(), "Textures/startscreen.png", true);
        background.setWidth(app.getCamera().getWidth());
        background.setHeight(app.getCamera().getHeight());
        guiNode.attachChild(background);


        startText = new BitmapText(app.getAssetManager().loadFont("Font/Metal_Mania/MetalMania72.fnt"));
        startText.setText("Press Space to Start");
        startText.setLocalTranslation(
                (app.getCamera().getWidth() - startText.getLineWidth()) / 2,
                startText.getLineHeight() + 20f,
                0
        );
        guiNode.attachChild(startText);

        InputManager inputManager = app.getInputManager();
        inputManager.addMapping(START_GAME, new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(this, START_GAME);
    }

    private void startGame() {
        setEnabled(false);
        getStateManager().attach(new StoryAppState("storylines/foo/story.json"));
        getStateManager().detach(this);
    }

    @Override
    protected void cleanup(Application app) {
        guiNode.detachChild(startText);
        guiNode.detachChild(background);
        InputManager inputManager = app.getInputManager();
        if (inputManager.hasMapping(START_GAME)) {
            inputManager.deleteMapping(START_GAME);
        }
        inputManager.removeListener(this);
    }

    @Override
    protected void onEnable() {
        if (startText != null) {
            startText.setCullHint(Node.CullHint.Inherit);
        }
        if (background != null) {
            background.setCullHint(Node.CullHint.Inherit);
        }
    }

    @Override
    protected void onDisable() {
        if (startText != null) {
            startText.setCullHint(Node.CullHint.Always);
        }
        if (background != null) {
            background.setCullHint(Node.CullHint.Always);
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals(START_GAME) && !isPressed) {
            startGame();
        }
    }
}
