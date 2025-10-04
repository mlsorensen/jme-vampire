package com.turboio.games.vampires.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.MatParam;
import com.jme3.ui.Picture;
import com.turboio.games.vampires.story.SlideConfig;

import java.util.List;
import java.util.logging.Logger;

public class SlideshowAppState extends BaseAppState implements ActionListener {

    public interface SlideshowListener {
        void onSlideshowFinished();
    }

    private static final Logger logger = Logger.getLogger(SlideshowAppState.class.getName());
    private static final String NEXT_SLIDE = "NextSlide";

    private final List<SlideConfig> slides;
    private final SlideshowListener listener;

    private SimpleApplication app;
    private Node root;
    private Geometry backgroundQuad;
    private Picture backgroundImage;
    private BitmapText text;

    private int currentIndex = -1;
    private float slideTimer = 0f;
    private float fadeDuration = 0.5f;
    private enum SlideState { FADING_IN, DISPLAYING, FADING_OUT }
    private SlideState state = SlideState.FADING_IN;

    public SlideshowAppState(List<SlideConfig> slides, SlideshowListener listener) {
        this.slides = slides;
        this.listener = listener;
    }

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        root = new Node("SlideshowRoot");
        this.app.getGuiNode().attachChild(root);

        createBackgroundElements();
        createTextElement();

        // Register key input for advancing slides
        InputManager inputManager = this.app.getInputManager();
        inputManager.addMapping(NEXT_SLIDE, new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(this, NEXT_SLIDE);

        advanceSlide();
    }

    private void createBackgroundElements() {
        float width = app.getCamera().getWidth();
        float height = app.getCamera().getHeight();

        Quad quad = new Quad(width, height);
        backgroundQuad = new Geometry("SlideBackground", quad);
        backgroundQuad.setLocalTranslation(0, 0, 0);
        Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", ColorRGBA.Black);
        bgMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        backgroundQuad.setMaterial(bgMat);
        backgroundQuad.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Gui);
        root.attachChild(backgroundQuad);

        backgroundImage = new Picture("SlideImage");
        backgroundImage.setWidth(width);
        backgroundImage.setHeight(height);
        backgroundImage.setLocalTranslation(0, 0, 0.1f);
        backgroundImage.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Gui);
    }

    private void createTextElement() {
        BitmapFont font = app.getAssetManager().loadFont("Font/Metal_Mania/MetalMania32.fnt");
        text = new BitmapText(font, false);
        text.setSize(font.getCharSet().getRenderedSize() * 1.25f);
        text.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Gui);
        text.setBox(new Rectangle(0, 0, app.getCamera().getWidth(), text.getLineHeight() * 2.5f));
        text.setAlignment(BitmapFont.Align.Center);
        text.setLocalTranslation(0, 0, 0.2f);
        root.attachChild(text);
    }

    @Override
    public void update(float tpf) {
        if (slides == null || slides.isEmpty()) {
            finish();
            return;
        }

        slideTimer += tpf;
        SlideConfig current = slides.get(currentIndex);

        switch (state) {
            case FADING_IN:
                float alphaIn = Math.min(1f, slideTimer / fadeDuration);
                setAlpha(alphaIn);
                if (alphaIn >= 1f) {
                    state = SlideState.DISPLAYING;
                    slideTimer = 0f;
                }
                break;
            case DISPLAYING:
                setAlpha(1f);
                if (slideTimer >= current.getDuration()) {
                    state = SlideState.FADING_OUT;
                    slideTimer = 0f;
                }
                break;
            case FADING_OUT:
                float alphaOut = Math.max(0f, 1f - (slideTimer / fadeDuration));
                setAlpha(alphaOut);
                if (alphaOut <= 0f) {
                    advanceSlide();
                }
                break;
        }
    }

    private void setAlpha(float alpha) {
        Material bgMat = backgroundQuad.getMaterial();
        MatParam colorParam = bgMat.getParam("Color");
        ColorRGBA color = colorParam != null ? ((ColorRGBA) colorParam.getValue()) : ColorRGBA.Black;
        bgMat.setColor("Color", new ColorRGBA(color.r, color.g, color.b, alpha));
        text.setAlpha(alpha);
        if (backgroundImage.getParent() != null && backgroundImage.getMaterial() != null) {
            MatParam imgColorParam = backgroundImage.getMaterial().getParam("Color");
            ColorRGBA base = imgColorParam != null ? ((ColorRGBA) imgColorParam.getValue()) : ColorRGBA.White;
            backgroundImage.getMaterial().setColor("Color", new ColorRGBA(base.r, base.g, base.b, alpha));
        }
    }

    private void advanceSlide() {
        currentIndex++;
        if (currentIndex >= slides.size()) {
            finish();
            return;
        }
        SlideConfig config = slides.get(currentIndex);
        applySlide(config);
        slideTimer = 0f;
        state = SlideState.FADING_IN;
    }

    private void applySlide(SlideConfig slide) {
        setAlpha(0f);

        if (slide.getBackgroundImage() != null) {
            try {
                backgroundImage.setImage(app.getAssetManager(), slide.getBackgroundImage(), true);
                backgroundImage.getMaterial().getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
                if (backgroundImage.getParent() == null) {
                    root.attachChild(backgroundImage);
                }
            } catch (com.jme3.asset.AssetNotFoundException ex) {
                logger.warning("Missing slide background image: " + slide.getBackgroundImage() + " - using solid color.");
                if (backgroundImage.getParent() != null) {
                    backgroundImage.removeFromParent();
                }
                Material bgMat = backgroundQuad.getMaterial();
                bgMat.setColor("Color", parseColor(slide.getBackgroundColor()));
            }
        } else {
            if (backgroundImage.getParent() != null) {
                backgroundImage.removeFromParent();
            }
            Material bgMat = backgroundQuad.getMaterial();
            ColorRGBA color = parseColor(slide.getBackgroundColor());
            bgMat.setColor("Color", color);
        }

        String slideText = slide.getText() != null ? slide.getText() : "";
        text.setText(slideText);

        if (text.getLineWidth() > app.getCamera().getWidth()) {
            int middle = slideText.length() / 2;
            int splitPoint = slideText.lastIndexOf(' ', middle);
            if (splitPoint == -1) {
                splitPoint = slideText.indexOf(' ', middle);
            }

            if (splitPoint != -1) {
                String wrappedText = slideText.substring(0, splitPoint).trim() + "\n" + slideText.substring(splitPoint).trim();
                text.setText(wrappedText);
            }
        }

        centerText();
    }

    private void centerText() {
        float y;
        if (text.getText().contains("\n")) {
            y = (app.getCamera().getHeight() * 0.33f);
        } else {
            y = (app.getCamera().getHeight() * 0.33f) + text.getLineHeight();
        }
        text.setLocalTranslation(0, y, 0.2f);
    }

    private ColorRGBA parseColor(String hex) {
        if (hex == null) {
            return ColorRGBA.Black;
        }
        try {
            int color = (int) Long.parseLong(hex.replace("#", ""), 16);
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;
            return new ColorRGBA(r, g, b, 1f);
        } catch (NumberFormatException e) {
            return ColorRGBA.Black;
        }
    }

    private void finish() {
        if (root != null) {
            root.removeFromParent();
        }
        getStateManager().detach(this);
        if (listener != null) {
            listener.onSlideshowFinished();
        }
    }

    @Override
    protected void cleanup(Application app) {
        if (root != null) {
            root.removeFromParent();
        }
        
        // Clean up input mapping
        InputManager inputManager = this.app.getInputManager();
        if (inputManager.hasMapping(NEXT_SLIDE)) {
            inputManager.deleteMapping(NEXT_SLIDE);
        }
        inputManager.removeListener(this);
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals(NEXT_SLIDE) && !isPressed) {
            // Skip to next slide immediately when Space is pressed
            advanceSlide();
        }
    }
}
