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
import com.jme3.texture.Texture;
import com.turboio.games.vampires.audio.Sound;
import com.turboio.games.vampires.controls.PlayerControl;
import com.turboio.games.vampires.controls.WanderingEnemyControl;
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

    private final Random random = new Random();
    private WanderingEnemyControl enemyControl;
    private float enemySpeed = 200f;
    private boolean gameOver = false;

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
        float enemyWidth = 64f;
        float enemyHeight = 64f;
        float spawnX = inset + random.nextFloat() * (screenWidth - inset * 2 - enemyWidth);
        float spawnY = inset + random.nextFloat() * (screenHeight - inset * 2 - enemyHeight);
        enemy.setLocalTranslation(spawnX, spawnY, 3);
        enemy.setQueueBucket(RenderQueue.Bucket.Gui);

        enemyControl = new WanderingEnemyControl(initialPerimeter, enemySpeed);
        enemy.addControl(enemyControl);

        // Add control to the player
        player.addControl(new PlayerControl(initialPerimeter, perimeterManager));


        // UI
        BitmapFont guiFont = app.getAssetManager().loadFont("Font/Metal_Mania/MetalMania72.fnt");
        for (int i = 0; i < guiFont.getPageSize(); i++) {
            Material pageMat = guiFont.getPage(i);
            Texture tex = pageMat.getTextureParam("ColorMap").getTextureValue();
            if (tex != null) {
                tex.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
                tex.setMagFilter(Texture.MagFilter.Nearest);
            }
        }
        scoreText = new BitmapText(guiFont);
        scoreText.setSize(72);
        scoreText.setLocalTranslation(10, app.getCamera().getHeight() - 10, 5);
        percentageText = new BitmapText(guiFont);
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
        if (!(Boolean) player.getUserData("alive")) {
            return;
        }

        if (enemyControl != null) {
            enemyControl.setPerimeter(perimeters.get(perimeters.size() - 1));
        }
        checkCollisions(control);

        if (gameOver) {
            return;
        }

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
                endGame("YOU WIN", percentage);
                return;
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

    private void updateEnemyMovement(float tpf) {
        directionTimer -= tpf;
        if (directionTimer <= 0f) {
            enemyVelocity = randomDirection();
            directionTimer = nextDirectionInterval();
        }

        Vector3f currentPos = enemy.getLocalTranslation();
        Vector3f nextPos = currentPos.add(enemyVelocity.mult(enemySpeed * tpf));
        Perimeter currentPerimeter = perimeters.get(perimeters.size() - 1);

        if (!currentPerimeter.contains(nextPos)) {
            enemyVelocity = randomDirection();
            directionTimer = nextDirectionInterval();
            return;
        }

        enemy.setLocalTranslation(nextPos);
    }

    private Vector3f randomDirection() {
        Vector3f dir = new Vector3f(random.nextFloat() * 2f - 1f, random.nextFloat() * 2f - 1f, 0);
        if (dir.lengthSquared() == 0) {
            dir.set(1, 0, 0);
        }
        return dir.normalizeLocal();
    }

    private float nextDirectionInterval() {
        return 1.5f + random.nextFloat() * 2f;
    }

    private void checkCollisions(PlayerControl control) {
        Vector3f playerPos = player.getLocalTranslation();
        Vector3f enemyPos = enemy.getLocalTranslation();
        float playerRadius = ((Number) player.getUserData("radius")).floatValue();
        float enemyRadius = ((Number) enemy.getUserData("radius")).floatValue();

        if (playerPos.distance(enemyPos) <= playerRadius + enemyRadius) {
            handlePlayerDeath();
            return;
        }

        if (control.isDrawing()) {
            List<Vector3f> path = control.getVisualDrawingPath();
            if (path != null && !path.isEmpty()) {
                if (isCircleIntersectingPolyline(enemyPos, enemyRadius, path)) {
                    handlePlayerDeath();
                }
            }
        }
    }

    private boolean isCircleIntersectingPolyline(Vector3f center, float radius, List<Vector3f> polyline) {
        for (int i = 0; i < polyline.size() - 1; i++) {
            Vector3f a = polyline.get(i);
            Vector3f b = polyline.get(i + 1);
            float dist = distanceToSegment(center, a, b);
            if (dist <= radius) {
                return true;
            }
        }
        return false;
    }

    private float distanceToSegment(Vector3f point, Vector3f start, Vector3f end) {
        Vector3f seg = end.subtract(start);
        Vector3f toPoint = point.subtract(start);
        float segLenSq = seg.x * seg.x + seg.y * seg.y;
        if (segLenSq == 0f) {
            return point.distance(start);
        }
        float t = Math.max(0f, Math.min(1f, (toPoint.x * seg.x + toPoint.y * seg.y) / segLenSq));
        Vector3f projection = start.add(seg.mult(t));
        return projection.distance(point);
    }

    private void handlePlayerDeath() {
        if (gameOver) {
            return;
        }
        player.setUserData("alive", false);
        player.getControl(PlayerControl.class).reset();
        double percentage = (1 - (currentPerimeterArea / originalPerimeterArea)) * 100;
        endGame("YOU LOSE", percentage);
    }

    private void endGame(String title, double percentage) {
        if (gameOver) {
            return;
        }
        gameOver = true;
        app.getStateManager().detach(this);
        app.getStateManager().attach(new EndGameAppState(title, score, percentage));
    }
}
