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
import com.jme3.material.RenderState;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import com.turboio.games.vampires.audio.Sound;
import com.turboio.games.vampires.controls.PlayerControl;
import com.turboio.games.vampires.perimeter.Perimeter;
import com.turboio.games.vampires.perimeter.PerimeterManager;
import com.turboio.games.vampires.perimeter.PerimeterRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameAppState extends BaseAppState implements ActionListener {
    private SimpleApplication app;
    private Picture background;
    private Spatial player;
    private Spatial enemy;
    private Node perimeterGeoms;
    private Geometry dayField;
    private Geometry drawingPathGeom;

    private List<Perimeter> perimeters;
    private PerimeterManager perimeterManager;
    private PerimeterRenderer perimeterRenderer;
    private double originalPerimeterArea;
    private double currentPerimeterArea;
    private double score;
    private BitmapText scoreText;
    private BitmapText percentageText;
    private Sound sound;

    private final String[] MAPPINGS = new String[]{"left", "right", "up", "down", "return"};

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        this.perimeterManager = new PerimeterManager();
        this.perimeterRenderer = new PerimeterRenderer(app.getAssetManager(), app.getCamera().getWidth(), app.getCamera().getHeight());

        // Layer 0: Background (night field - captured areas)
        background = new Picture("Background");
        background.setImage(app.getAssetManager(), "Textures/field_night.png", true);
        background.setWidth(app.getCamera().getWidth());
        background.setHeight(app.getCamera().getHeight());
        background.setLocalTranslation(0, 0, 0);

        // --- Data Setup ---
        List<Vector3f> initialVertices = new ArrayList<>();
        float inset = 64f;
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();
        initialVertices.add(new Vector3f(inset, inset, 0));
        initialVertices.add(new Vector3f(screenWidth - inset, inset, 0));
        initialVertices.add(new Vector3f(screenWidth - inset, screenHeight - inset - 50, 0));
        initialVertices.add(new Vector3f(inset, screenHeight - inset - 50, 0));
        Perimeter initialPerimeter = new Perimeter(initialVertices);
        this.perimeters = new ArrayList<>();
        this.perimeters.add(initialPerimeter);
        this.originalPerimeterArea = initialPerimeter.getArea();
        this.currentPerimeterArea = originalPerimeterArea;
        this.score = 0;

        // Create initial visuals
        perimeterGeoms = new Node("PerimeterGeometries");
        Geometry initialPerimeterLine = perimeterRenderer.createPerimeterLine(initialPerimeter.getVertices());
        perimeterGeoms.attachChild(initialPerimeterLine);
        dayField = perimeterRenderer.createDayField(initialPerimeter.getVertices());

        // Drawing Path (initially empty)
        drawingPathGeom = perimeterRenderer.createDrawingPathLine();

        // Layer 3: Player
        player = getSpatial("player");
        player.setUserData("alive", true);
        player.setLocalTranslation(initialPerimeter.getVertices().get(0).add(0, 0, 3));
        player.setQueueBucket(RenderQueue.Bucket.Gui);

        // Layer 3: Enemy
        enemy = getSpatial("human");
        Random rand = new Random();
        float enemyWidth = 64f;
        float enemyHeight = 64f;
        float spawnX = inset + rand.nextFloat() * (screenWidth - inset * 2 - enemyWidth);
        float spawnY = inset + rand.nextFloat() * (screenHeight - inset * 2 - enemyHeight);
        enemy.setLocalTranslation(spawnX, spawnY, 3);
        enemy.setQueueBucket(RenderQueue.Bucket.Gui);

        // Add control to the player
        player.addControl(new PlayerControl(initialPerimeter));


        // UI
        BitmapFont guiFont = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        scoreText = new BitmapText(guiFont, false);
        scoreText.setSize(72);
        scoreText.setLocalTranslation(10, app.getCamera().getHeight() - 10, 5);
        percentageText = new BitmapText(guiFont, false);
        percentageText.setSize(72);
        percentageText.setLocalTranslation(app.getCamera().getWidth() - 450, app.getCamera().getHeight() - 10, 5);

        sound = new Sound(app.getAssetManager());
    }

    @Override
    protected void onEnable() {
        app.getGuiNode().attachChild(background);
        app.getGuiNode().attachChild(player);
        app.getGuiNode().attachChild(enemy);
        app.getGuiNode().attachChild(perimeterGeoms);
        app.getGuiNode().attachChild(drawingPathGeom);
        app.getGuiNode().attachChild(dayField);
        app.getGuiNode().attachChild(scoreText);
        app.getGuiNode().attachChild(percentageText);
        sound.playMusic();

        InputManager inputManager = app.getInputManager();
        inputManager.addMapping("left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("return", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(this, MAPPINGS);
    }

    @Override
    protected void onDisable() {
        sound.stopMusic();
        app.getGuiNode().detachAllChildren();

        InputManager inputManager = app.getInputManager();
        for (String mapping : MAPPINGS) {
            if (inputManager.hasMapping(mapping)) {
                inputManager.deleteMapping(mapping);
            }
        }
        inputManager.removeListener(this);
    }

    @Override
    public void update(float tpf) {
        PlayerControl control = player.getControl(PlayerControl.class);
        if (control == null) return;

        if (control.wasCollisionDetected()) {
            Perimeter lastPerimeter = perimeters.get(perimeters.size() - 1);
            Perimeter newPerimeter = perimeterManager.calculateNewPerimeter(lastPerimeter, control, enemy.getLocalTranslation());
            perimeters.add(newPerimeter);

            double lastPerimeterArea = currentPerimeterArea;
            currentPerimeterArea = newPerimeter.getArea();
            double capturedArea = lastPerimeterArea - currentPerimeterArea;
            score += capturedArea * (capturedArea / lastPerimeterArea);

            // Add new line visual
            Geometry newPerimeterLine = perimeterRenderer.createPerimeterLine(newPerimeter.getVertices());
            perimeterGeoms.attachChild(newPerimeterLine);

            // Update shading overlay
            if (dayField != null) {
                dayField.removeFromParent();
            }
            dayField = perimeterRenderer.createDayField(newPerimeter.getVertices());
            app.getGuiNode().attachChild(dayField);

            control.finalizeCollision(newPerimeter);

            if (currentPerimeterArea / originalPerimeterArea <= 0.1) {
                double percentage = (1 - (currentPerimeterArea / originalPerimeterArea)) * 100;
                app.getStateManager().detach(this);
                app.getStateManager().attach(new WinAppState(score, percentage));
            }
        }

        // Update the visual representation of the drawing path
        perimeterRenderer.updateDrawingPathVisuals(drawingPathGeom, control.getVisualDrawingPath());

        double percentage = (1 - (currentPerimeterArea / originalPerimeterArea)) * 100;
        scoreText.setText("Score: " + (int) score);
        percentageText.setText(String.format("%.2f%%", percentage));
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if ((Boolean) player.getUserData("alive")) {
            PlayerControl control = player.getControl(PlayerControl.class);
            if (control != null) {
                switch (name) {
                    case "up": control.up = isPressed; break;
                    case "down": control.down = isPressed; break;
                    case "left": control.left = isPressed; break;
                    case "right": control.right = isPressed; break;
                }
            }
        }
    }

    private Spatial getSpatial(String name) {
        Node node = new Node(name);
        Picture pic = new Picture(name);
        Texture2D tex = (Texture2D) app.getAssetManager().loadTexture("Textures/" + name + ".png");
        pic.setTexture(app.getAssetManager(), tex, true);

        float width = tex.getImage().getWidth();
        float height = tex.getImage().getHeight();
        pic.setWidth(width);
        pic.setHeight(height);
        pic.move(-width / 2f, -height / 2f, 0);

        Material picMat = new Material(app.getAssetManager(), "Common/MatDefs/Gui/Gui.j3md");
        picMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.AlphaAdditive);
        node.setMaterial(picMat);

        node.setUserData("radius", width / 2);
        node.attachChild(pic);
        return node;
    }

    @Override
    protected void cleanup(Application app) {}
}
