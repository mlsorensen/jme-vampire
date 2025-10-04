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
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import com.turboio.games.vampires.audio.Sound;
import com.turboio.games.vampires.controls.AnimatedSpriteControl;
import com.turboio.games.vampires.controls.EnemyMovementControl;
import com.turboio.games.vampires.controls.PlayerControl;
import com.turboio.games.vampires.controls.WanderingEnemyControl;
import com.turboio.games.vampires.level.EnemyConfig;
import com.turboio.games.vampires.level.LevelConfig;
import com.turboio.games.vampires.perimeter.Perimeter;
import com.turboio.games.vampires.perimeter.PerimeterManager;
import com.turboio.games.vampires.perimeter.PerimeterRenderer;
import com.turboio.games.vampires.perimeter.SparkEffect;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class LevelAppState extends BaseAppState implements ActionListener {
    public enum LevelOutcome { WIN, LOSE }

    public interface LevelResultListener {
        void onLevelCompleted(LevelOutcome outcome, double score, double percentage);
    }

    private final LevelConfig config;
    private final LevelResultListener resultListener;

    private SimpleApplication app;
    private Picture background;
    private final List<Spatial> enemies = new ArrayList<>();
    private final List<EnemyMovementControl> enemyControls = new ArrayList<>();
    private Node perimeterGeoms;
    private Geometry dayField;
    private Geometry drawingPathGeom;
    private SparkEffect sparkEffect;

    private List<Perimeter> perimeters;
    private PerimeterManager perimeterManager;
    private PerimeterRenderer perimeterRenderer;
    private double originalPerimeterArea;
    private double currentPerimeterArea;
    private double score;
    private BitmapText scoreText;
    private BitmapText percentageText;
    private Sound sound;
    private float pulseTimer = 0;

    private final String[] MAPPINGS = new String[]{"left", "right", "up", "down", "return", "draw"};

    private Spatial player;
    private boolean gameOver = false;

    public LevelAppState(LevelConfig config) {
        this(config, null);
    }

    public LevelAppState(LevelConfig config, LevelResultListener listener) {
        this.config = config;
        this.resultListener = listener;
    }

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        this.perimeterManager = new PerimeterManager();
        this.perimeterRenderer = new PerimeterRenderer(app.getAssetManager(), app.getCamera().getWidth(), app.getCamera().getHeight());

        setupBackground();
        setupPerimeter();
        setupPlayer();
        setupEnemies();
        setupUI();
        sound = new Sound(app.getAssetManager());
        if (config.getMusic() != null) {
            sound.playMusic(config.getMusic());
        }
        gameOver = false;
    }

    private void setupBackground() {
        background = new Picture("Background");
        String backgroundPath = config.getBackgroundImage() != null ? config.getBackgroundImage() : "Textures/field_night.png";
        background.setImage(app.getAssetManager(), backgroundPath, true);
        background.setWidth(app.getCamera().getWidth());
        background.setHeight(app.getCamera().getHeight());
        background.setLocalTranslation(0, 0, 0);
    }

    private void setupPerimeter() {
        List<Vector3f> vertices = new ArrayList<>();
        if (config.getPerimeterVertices() != null && !config.getPerimeterVertices().isEmpty()) {
            config.getPerimeterVertices().forEach(v -> vertices.add(new Vector3f(v.getX(), v.getY(), 0)));
        } else {
            float inset = 64f;
            float screenWidth = app.getCamera().getWidth();
            float screenHeight = app.getCamera().getHeight();
            vertices.add(new Vector3f(inset, inset, 0));
            vertices.add(new Vector3f(screenWidth - inset, inset, 0));
            vertices.add(new Vector3f(screenWidth - inset, screenHeight - inset - 50, 0));
            vertices.add(new Vector3f(inset, screenHeight - inset - 50, 0));
        }
        Perimeter initialPerimeter = new Perimeter(vertices);
        this.perimeters = new ArrayList<>();
        this.perimeters.add(initialPerimeter);
        this.originalPerimeterArea = initialPerimeter.getArea();
        this.currentPerimeterArea = originalPerimeterArea;
        this.score = 0;

        perimeterGeoms = new Node("PerimeterGeometries");
        perimeterRenderer.setForegroundTexture(config.getForegroundImage() != null ? config.getForegroundImage() : "Textures/field.png");
        Geometry initialPerimeterLine = perimeterRenderer.createPerimeterLine(initialPerimeter.getVertices());
        perimeterGeoms.attachChild(initialPerimeterLine);
        dayField = perimeterRenderer.createDayField(initialPerimeter.getVertices());
        drawingPathGeom = perimeterRenderer.createDrawingPathLine();
        sparkEffect = new SparkEffect(app.getGuiNode(), app.getAssetManager());
    }

    private void setupPlayer() {
        // Default to victor, can override with "player" or other sprite in level config
        String playerName = config.getPlayerSprite() != null ? config.getPlayerSprite() : "victor";
        
        if (playerName.equals("victor")) {
            player = createAnimatedSprite("Textures/sprites/victor");
        } else {
            player = createSprite(playerName);
        }
        
        player.setUserData("alive", true);
        player.setLocalTranslation(perimeters.get(0).getVertices().get(0).add(0, 0, 4f));
        player.setQueueBucket(RenderQueue.Bucket.Gui);
        player.addControl(new PlayerControl(perimeters.get(0), perimeterManager));
    }

    private void setupEnemies() {
        enemies.clear();
        enemyControls.clear();
        if (config.getEnemies() != null) {
            for (EnemyConfig enemyConfig : config.getEnemies()) {
                Spatial enemySpatial = createSprite(enemyConfig.getSprite() != null ? enemyConfig.getSprite() : "human");
                float radius = enemyConfig.getRadius() > 0 ? enemyConfig.getRadius() : ((Number) enemySpatial.getUserData("radius")).floatValue();
                enemySpatial.setUserData("radius", radius);
                enemySpatial.setLocalTranslation(enemyConfig.getSpawnX(), enemyConfig.getSpawnY(), 4f);
                enemySpatial.setQueueBucket(RenderQueue.Bucket.Gui);

                EnemyMovementControl control = createMovementControl(enemyConfig);
                enemySpatial.addControl(control);

                enemies.add(enemySpatial);
                enemyControls.add(control);
            }
        }
    }

    private EnemyMovementControl createMovementControl(EnemyConfig config) {
        float speed = config.getSpeed() > 0 ? config.getSpeed() : 200f;
        String movementClass = config.getMovementClass();
        if (movementClass == null) {
            return new WanderingEnemyControl(perimeters.get(0), speed);
        }
        try {
            Class<?> clazz = Class.forName(movementClass);
            return (EnemyMovementControl) clazz.getConstructor(Perimeter.class, float.class).newInstance(perimeters.get(0), speed);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException("Could not create movement control: " + movementClass, e);
        }
    }

    private void setupUI() {
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
    }

    @Override
    protected void onEnable() {
        app.getGuiNode().attachChild(background);
        System.out.println("Attaching player to guiNode: " + player.getName() + " at " + player.getLocalTranslation());
        System.out.println("Player has " + ((Node)player).getChildren().size() + " children");
        app.getGuiNode().attachChild(player);
        for (Spatial enemy : enemies) {
            app.getGuiNode().attachChild(enemy);
        }
        app.getGuiNode().attachChild(perimeterGeoms);
        app.getGuiNode().attachChild(dayField);
        app.getGuiNode().attachChild(drawingPathGeom);
        app.getGuiNode().attachChild(scoreText);
        app.getGuiNode().attachChild(percentageText);

        InputManager inputManager = app.getInputManager();
        inputManager.addMapping("left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("return", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("draw", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(this, MAPPINGS);
    }

    @Override
    protected void onDisable() {
        if (sound != null) {
            sound.stopAllSounds();
        }
        cleanupScene();

        InputManager inputManager = app.getInputManager();
        for (String mapping : MAPPINGS) {
            if (inputManager.hasMapping(mapping)) {
                inputManager.deleteMapping(mapping);
            }
        }
        inputManager.removeListener(this);
    }

    @Override
    protected void cleanup(Application app) {
        cleanupScene();
    }

    @Override
    public void update(float tpf) {
        PlayerControl control = player.getControl(PlayerControl.class);
        if (control == null) return;
        if (!(Boolean) player.getUserData("alive")) {
            return;
        }

        handleDrawingSounds(control);

        for (int i = 0; i < enemyControls.size(); i++) {
            enemyControls.get(i).setPerimeter(perimeters.get(perimeters.size() - 1));
        }
        checkCollisions(control);

        if (gameOver) {
            return;
        }

        if (control.wasCollisionDetected()) {
            Perimeter lastPerimeter = perimeters.get(perimeters.size() - 1);
            Perimeter newPerimeter = perimeterManager.calculateNewPerimeter(lastPerimeter, control, enemies.isEmpty() ? player.getLocalTranslation() : enemies.get(0).getLocalTranslation());
            perimeters.add(newPerimeter);

            double lastPerimeterArea = currentPerimeterArea;
            currentPerimeterArea = newPerimeter.getArea();
            double capturedArea = lastPerimeterArea - currentPerimeterArea;
            score += 5 * capturedArea * (capturedArea / originalPerimeterArea);

            Geometry newPerimeterLine = perimeterRenderer.createPerimeterLine(newPerimeter.getVertices());
            perimeterGeoms.attachChild(newPerimeterLine);

            if (dayField != null) {
                dayField.removeFromParent();
            }
            dayField = perimeterRenderer.createDayField(newPerimeter.getVertices());
            app.getGuiNode().attachChild(dayField);

            control.finalizeCollision(newPerimeter);

            if (currentPerimeterArea / originalPerimeterArea <= 0.1) {
                double percentage = (1 - (currentPerimeterArea / originalPerimeterArea)) * 100;
                endLevel(LevelOutcome.WIN, percentage);
                return;
            }
        }

        perimeterRenderer.updateDrawingPathVisuals(drawingPathGeom, control.getVisualDrawingPath());
        updateDrawingPathSparks(control.getVisualDrawingPath());
        sparkEffect.update(tpf);

        pulseTimer += tpf * 12f;
        float pulse = (float) (Math.sin(pulseTimer) * 0.45 + 0.55);
        if (drawingPathGeom != null) {
            Material mat = drawingPathGeom.getMaterial();
            ColorRGBA baseGlow = new ColorRGBA(1f, 0.15f, 0.15f, 1f);
            mat.setColor("GlowColor", baseGlow.mult(pulse));
        }

        double percentage = (1 - (currentPerimeterArea / originalPerimeterArea)) * 100;
        scoreText.setText(String.format("Score: %.0f",score));
        percentageText.setText(String.format("%.2f%%", percentage));
        percentageText.setLocalTranslation(app.getCamera().getWidth() - percentageText.getLineWidth() - 10, app.getCamera().getHeight() - 10, 5);
    }

    private void handleDrawingSounds(PlayerControl control) {
        switch (control.getDrawingState()) {
            case STARTING_DRAW:
                sound.playSound("Audio/cut-start.ogg", 0.5f);
                sound.startLoop("Audio/cut-active.ogg", "drawing", 0.3f);
                control.advanceDrawingState();
                break;
            case ENDING_DRAW:
                sound.stopLoop("drawing");
                sound.playSound("Audio/cut-end.ogg", 0.5f);
                control.advanceDrawingState();
                break;
        }
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
                    case "draw":
                        if (!isPressed) { // On key release
                            control.toggleDrawing();
                        }
                        break;
                }
            }
        }
    }

    private Spatial createSprite(String name) {
        Node node = new Node(name);
        String texturePath = "Textures/" + name + ".png";
        Texture2D tex = (Texture2D) app.getAssetManager().loadTexture(texturePath);
        Picture pic = new Picture(name);
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
    
    private Spatial createAnimatedSprite(String basePath) {
        Node node = new Node("AnimatedPlayer");
        AnimatedSpriteControl animControl = new AnimatedSpriteControl(app.getAssetManager(), basePath);
        node.attachChild(animControl.getGeometry());
        node.addControl(animControl);
        node.setUserData("radius", 32f); // Half of 64px sprite
        return node;
    }

    private void checkCollisions(PlayerControl control) {
        // Victor is only vulnerable when drawing a path
        if (!control.isDrawing()) {
            return;
        }
        
        Vector3f playerPos = player.getLocalTranslation();
        float playerRadius = ((Number) player.getUserData("radius")).floatValue();

        for (Spatial enemy : enemies) {
            Vector3f enemyPos = enemy.getLocalTranslation();
            float enemyRadius = ((Number) enemy.getUserData("radius")).floatValue();

            // Check direct collision with enemy
            if (playerPos.distance(enemyPos) <= playerRadius + enemyRadius) {
                handlePlayerDeath();
                return;
            }

            // Check if drawing path intersects enemy
            List<Vector3f> path = control.getVisualDrawingPath();
            if (path != null && !path.isEmpty()) {
                if (isCircleIntersectingPolyline(enemyPos, enemyRadius, path)) {
                    handlePlayerDeath();
                    return;
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
        PlayerControl control = player.getControl(PlayerControl.class);
        if (control != null) {
            control.reset();
        }
        double percentage = (1 - (currentPerimeterArea / originalPerimeterArea)) * 100;
        endLevel(LevelOutcome.LOSE, percentage);
    }

    private void endLevel(LevelOutcome outcome, double percentage) {
        if (gameOver) {
            return;
        }
        gameOver = true;
        if (sound != null) {
            sound.stopAllSounds();
        }
        cleanupScene();
        app.getStateManager().detach(this);
        if (resultListener != null) {
            resultListener.onLevelCompleted(outcome, score, percentage);
        } else {
            String title = outcome == LevelOutcome.WIN ? "YOU WIN" : "YOU LOSE";
            app.getStateManager().attach(new LevelSummaryAppState(title, score, percentage, null));
        }
    }

    private void cleanupScene() {
        if (background != null) {
            background.removeFromParent();
        }
        if (player != null) {
            player.removeFromParent();
        }
        for (Spatial enemy : enemies) {
            enemy.removeFromParent();
        }
        enemies.clear();
        enemyControls.clear();
        if (perimeterGeoms != null) {
            perimeterGeoms.removeFromParent();
            perimeterGeoms.detachAllChildren();
        }
        if (dayField != null) {
            dayField.removeFromParent();
            dayField = null;
        }
        if (drawingPathGeom != null) {
            drawingPathGeom.removeFromParent();
        }
        if (sparkEffect != null) {
            sparkEffect.clear();
        }
        if (scoreText != null) {
            scoreText.removeFromParent();
        }
        if (percentageText != null) {
            percentageText.removeFromParent();
        }
    }

    private void updateDrawingPathSparks(List<Vector3f> path) {
        if (sparkEffect == null) {
            return;
        }

        if (path == null || path.size() < 2) {
            return;
        }

        // Emit a few sparks at the tip of the drawing path
        Vector3f tip = path.get(path.size() - 1);
        sparkEffect.emitSparks(tip, 2);
    }
}
