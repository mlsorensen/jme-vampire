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
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

public class LevelSummaryAppState extends BaseAppState implements ActionListener {
    private final String title;
    private final double score;
    private final double percentage;
    private final boolean showStats;
    private final Runnable onDismiss;

    private SimpleApplication app;
    private Node guiNode;

    public LevelSummaryAppState(String title, double score, double percentage, Runnable onDismiss) {
        this(title, score, percentage, true, onDismiss);
    }

    public LevelSummaryAppState(String title, double score, double percentage, boolean showStats, Runnable onDismiss) {
        this.title = title;
        this.score = score;
        this.percentage = percentage;
        this.showStats = showStats;
        this.onDismiss = onDismiss;
    }

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        this.guiNode = new Node("LevelSummaryGui");

        // Create black background to cover the level
        float width = app.getCamera().getWidth();
        float height = app.getCamera().getHeight();
        Quad quad = new Quad(width, height);
        Geometry background = new Geometry("SummaryBackground", quad);
        background.setLocalTranslation(0, 0, 0);
        Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", ColorRGBA.Black);
        background.setMaterial(bgMat);
        background.setQueueBucket(RenderQueue.Bucket.Gui);
        guiNode.attachChild(background);

        BitmapFont largeFont = app.getAssetManager().loadFont("Font/Metal_Mania/MetalMania72.fnt");
        BitmapFont smallFont = app.getAssetManager().loadFont("Font/Metal_Mania/MetalMania32.fnt");
        configureFontTexture(largeFont);
        configureFontTexture(smallFont);

        BitmapText titleText = new BitmapText(largeFont);
        titleText.setSize(largeFont.getCharSet().getRenderedSize() * 1.5f);
        titleText.setText(title);
        titleText.setLocalTranslation(
                (app.getCamera().getWidth() - titleText.getLineWidth()) / 2,
                app.getCamera().getHeight() * 0.75f,
                0
        );
        guiNode.attachChild(titleText);

        if (showStats) {
            BitmapText scoreText = new BitmapText(smallFont);
            scoreText.setSize(smallFont.getCharSet().getRenderedSize() * 1.5f);
            scoreText.setText(String.format("Score: %.0f (%.2f%% captured)", score, percentage));
            scoreText.setLocalTranslation(
                    (app.getCamera().getWidth() - scoreText.getLineWidth()) / 2,
                    titleText.getLocalTranslation().y - titleText.getLineHeight() * 2,
                    0
            );
            guiNode.attachChild(scoreText);
        }

        BitmapText continueText = new BitmapText(smallFont);
        continueText.setSize(smallFont.getCharSet().getRenderedSize());
        continueText.setText("Press Enter to continue");
        continueText.setLocalTranslation(
                (app.getCamera().getWidth() - continueText.getLineWidth()) / 2,
                continueText.getLineHeight() * 2,
                0
        );
        guiNode.attachChild(continueText);
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
        if (inputManager.hasMapping("return")) {
            inputManager.deleteMapping("return");
        }
        inputManager.removeListener(this);
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if ("return".equals(name) && isPressed) {
            app.getStateManager().detach(this);
            if (onDismiss != null) {
                onDismiss.run();
            }
        }
    }

    @Override
    protected void cleanup(Application app) {}

    private void configureFontTexture(BitmapFont font) {
        for (int i = 0; i < font.getPageSize(); i++) {
            Material pageMat = font.getPage(i);
            Texture tex = pageMat.getTextureParam("ColorMap").getTextureValue();
            if (tex != null) {
                tex.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
                tex.setMagFilter(Texture.MagFilter.Nearest);
            }
        }
    }
}
