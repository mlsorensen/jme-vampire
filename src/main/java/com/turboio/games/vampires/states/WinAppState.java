package com.turboio.games.vampires.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.scene.Node;

public class WinAppState extends BaseAppState implements ActionListener {
    private SimpleApplication app;
    private Node guiNode;
    private final double score;
    private final double percentage;

    public WinAppState(double score, double percentage) {
        this.score = score;
        this.percentage = percentage;
    }

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        this.guiNode = new Node("WinScreenGui");

        BitmapFont guiFont = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        BitmapText winText = new BitmapText(guiFont, false);
        winText.setSize(guiFont.getCharSet().getRenderedSize() * 4);
        winText.setText("YOU WIN");
        winText.setLocalTranslation(
                (app.getCamera().getWidth() - winText.getLineWidth()) / 2,
                app.getCamera().getHeight() * 0.75f,
                0
        );
        guiNode.attachChild(winText);

        BitmapText scoreText = new BitmapText(guiFont, false);
        scoreText.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        scoreText.setText(String.format("Score: %.0f (%.2f%% captured)", score, percentage));
        scoreText.setLocalTranslation(
                (app.getCamera().getWidth() - scoreText.getLineWidth()) / 2,
                winText.getLocalTranslation().y - winText.getLineHeight() * 2,
                0
        );
        guiNode.attachChild(scoreText);

        BitmapText restartText = new BitmapText(guiFont, false);
        restartText.setSize(guiFont.getCharSet().getRenderedSize());
        restartText.setText("Press Enter to play again");
        restartText.setLocalTranslation(
                (app.getCamera().getWidth() - restartText.getLineWidth()) / 2,
                restartText.getLineHeight() * 2,
                0
        );
        guiNode.attachChild(restartText);
    }

    @Override
    protected void onEnable() {
        app.getGuiNode().attachChild(guiNode);
        InputManager inputManager = app.getInputManager();
        inputManager.addMapping("return", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(this, "return");
    }

    @Override
    protected void onDisable() {
        app.getGuiNode().detachChild(guiNode);
        InputManager inputManager = app.getInputManager();
        inputManager.deleteMapping("return");
        inputManager.removeListener(this);
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("return") && isPressed) {
            app.getStateManager().detach(this);
            app.getStateManager().attach(new GameAppState());
        }
    }

    @Override
    protected void cleanup(Application app) {}
}
