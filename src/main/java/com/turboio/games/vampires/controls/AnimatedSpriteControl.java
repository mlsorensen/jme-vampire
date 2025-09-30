package com.turboio.games.vampires.controls;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Vector2f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Image;
import com.jme3.ui.Picture;
import com.jme3.util.BufferUtils;

public class AnimatedSpriteControl extends AbstractControl {
    
    public enum Direction {
        UP, DOWN, LEFT, RIGHT, IDLE
    }
    
    private final AssetManager assetManager;
    private final Picture spritePicture;
    
    // Individual frame textures extracted from sprite sheets
    private Texture2D[] upFrames;
    private Texture2D[] downFrames;
    private Texture2D[] sideFrames;
    private Texture2D[] sideFramesFlipped;  // Horizontally flipped side frames
    
    // Animation settings
    private Direction currentDirection = Direction.DOWN;
    private int currentFrame = 0;
    private float frameTime = 0;
    private float frameDuration = 0.15f; // Time per frame
    
    // Sprite sheet configurations
    private int upFrameCount = 4;
    private int downFrameCount = 4;
    private int sideFrameCount = 2;
    private int upColumns = 2;
    private int upRows = 2;
    private int downColumns = 2;
    private int downRows = 2;
    private int sideColumns = 1;
    private int sideRows = 2;
    
    private float spriteWidth = 64f;  // Each sprite cell is 64x64
    private float spriteHeight = 64f;
    
    public AnimatedSpriteControl(AssetManager assetManager, String basePath) {
        this.assetManager = assetManager;
        
        // Load sprite sheets and extract individual frames
        try {
            Texture2D upSheet = (Texture2D) assetManager.loadTexture(basePath + "-up.png");
            Texture2D downSheet = (Texture2D) assetManager.loadTexture(basePath + "-down.png");
            Texture2D sideSheet = (Texture2D) assetManager.loadTexture(basePath + "-side.png");
            
            upFrames = extractFrames(upSheet, upColumns, upRows);
            downFrames = extractFrames(downSheet, downColumns, downRows);
            sideFrames = extractFrames(sideSheet, sideColumns, sideRows);
            
            // Create horizontally flipped versions of side frames for left movement
            sideFramesFlipped = new Texture2D[sideFrames.length];
            for (int i = 0; i < sideFrames.length; i++) {
                sideFramesFlipped[i] = flipTextureHorizontally(sideFrames[i]);
            }
            
            System.out.println("Loaded and extracted victor sprite frames successfully");
        } catch (Exception e) {
            System.err.println("Failed to load victor sprite textures: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Create Picture for 2D GUI rendering
        spritePicture = new Picture("AnimatedSprite");
        spritePicture.setTexture(assetManager, downFrames[0], true);
        spritePicture.setWidth(spriteWidth);
        spritePicture.setHeight(spriteHeight);
        
        // Center the sprite
        spritePicture.move(-spriteWidth / 2, -spriteHeight / 2, 0);
        
        System.out.println("AnimatedSpriteControl initialized");
    }
    
    private Texture2D[] extractFrames(Texture2D spriteSheet, int columns, int rows) {
        Image sheet = spriteSheet.getImage();
        int frameWidth = sheet.getWidth() / columns;
        int frameHeight = sheet.getHeight() / rows;
        int frameCount = columns * rows;
        
        Texture2D[] frames = new Texture2D[frameCount];
        
        for (int i = 0; i < frameCount; i++) {
            int col = i % columns;
            int row = i / columns;
            
            // Extract frame from sprite sheet
            Image frameImage = extractSubImage(sheet, col * frameWidth, row * frameHeight, frameWidth, frameHeight);
            frames[i] = new Texture2D(frameImage);
            frames[i].setMagFilter(Texture.MagFilter.Nearest);
            frames[i].setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        }
        
        return frames;
    }
    
    private Image extractSubImage(Image source, int x, int y, int width, int height) {
        // Create a new image with just the frame we want
        java.nio.ByteBuffer sourceData = source.getData(0);
        java.nio.ByteBuffer frameData = BufferUtils.createByteBuffer(width * height * 4);
        
        int bytesPerPixel = 4; // RGBA
        int sourceWidth = source.getWidth();
        
        // Copy row by row from source to frame buffer
        byte[] rowBuffer = new byte[width * bytesPerPixel];
        for (int row = 0; row < height; row++) {
            int sourcePos = ((y + row) * sourceWidth + x) * bytesPerPixel;
            sourceData.position(sourcePos);
            sourceData.get(rowBuffer, 0, width * bytesPerPixel);
            frameData.put(rowBuffer);
        }
        
        frameData.rewind();
        return new Image(source.getFormat(), width, height, frameData, source.getColorSpace());
    }
    
    private Texture2D flipTextureHorizontally(Texture2D source) {
        Image sourceImage = source.getImage();
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        
        java.nio.ByteBuffer sourceData = sourceImage.getData(0);
        java.nio.ByteBuffer flippedData = BufferUtils.createByteBuffer(width * height * 4);
        
        int bytesPerPixel = 4; // RGBA
        byte[] pixelBuffer = new byte[bytesPerPixel];
        
        // Flip each row horizontally
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Read pixel from source at (x, y)
                int sourcePos = (y * width + x) * bytesPerPixel;
                sourceData.position(sourcePos);
                sourceData.get(pixelBuffer, 0, bytesPerPixel);
                
                // Write pixel to flipped position (width - 1 - x, y)
                int flippedX = width - 1 - x;
                int flippedPos = (y * width + flippedX) * bytesPerPixel;
                flippedData.position(flippedPos);
                flippedData.put(pixelBuffer);
            }
        }
        
        flippedData.rewind();
        Image flippedImage = new Image(sourceImage.getFormat(), width, height, flippedData, sourceImage.getColorSpace());
        
        Texture2D flippedTexture = new Texture2D(flippedImage);
        flippedTexture.setMagFilter(Texture.MagFilter.Nearest);
        flippedTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        
        return flippedTexture;
    }
    
    public void setDirection(Direction direction) {
        if (this.currentDirection != direction) {
            this.currentDirection = direction;
            this.currentFrame = 0;
            this.frameTime = 0;
            
            // Update to show first frame of new direction
            updateFrameTexture();
        }
    }
    
    private void updateFrameTexture() {
        Texture2D[] frames;
        switch (currentDirection) {
            case UP:
                frames = upFrames;
                break;
            case DOWN:
                frames = downFrames;
                break;
            case LEFT:
                frames = sideFramesFlipped;  // Use flipped frames for left
                break;
            case RIGHT:
                frames = sideFrames;
                break;
            case IDLE:
            default:
                frames = downFrames;
                break;
        }
        
        if (frames != null && currentFrame < frames.length) {
            spritePicture.setTexture(assetManager, frames[currentFrame], true);
        }
    }
    
    public Geometry getGeometry() {
        return spritePicture;
    }
    
    @Override
    protected void controlUpdate(float tpf) {
        if (currentDirection == Direction.IDLE) {
            return;
        }
        
        frameTime += tpf;
        if (frameTime >= frameDuration) {
            frameTime -= frameDuration;
            
            // Advance frame
            int maxFrames = getFrameCount();
            currentFrame = (currentFrame + 1) % maxFrames;
            updateFrameTexture();
        }
    }
    
    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // Nothing to do here
    }
    
    private int getFrameCount() {
        switch (currentDirection) {
            case UP:
                return upFrameCount;
            case DOWN:
                return downFrameCount;
            case LEFT:
            case RIGHT:
                return sideFrameCount;
            default:
                return 1;
        }
    }
    
}
