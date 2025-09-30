package com.turboio.games.vampires.perimeter;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SparkEffect {
    
    private static class Spark {
        Geometry geometry;
        float age;
        float lifetime;
        float startSize;
        float endSize;
        Vector3f position;
        Vector3f velocity;
        
        Spark(Geometry geometry, Vector3f position, Vector3f velocity, float lifetime, float startSize, float endSize) {
            this.geometry = geometry;
            this.position = position.clone();
            this.velocity = velocity;
            this.lifetime = lifetime;
            this.startSize = startSize;
            this.endSize = endSize;
            this.age = 0;
        }
    }
    
    private final Node parentNode;
    private final AssetManager assetManager;
    private final List<Spark> activeSparks = new ArrayList<>();
    private final Material sparkMaterial;
    
    public SparkEffect(Node parentNode, AssetManager assetManager) {
        this.parentNode = parentNode;
        this.assetManager = assetManager;
        
        // Create a reusable material for all sparks
        sparkMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        sparkMaterial.setColor("Color", new ColorRGBA(1f, 0.6f, 0.2f, 1f));
        sparkMaterial.setColor("GlowColor", new ColorRGBA(1f, 0.4f, 0.1f, 1f));
        sparkMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Additive);
        sparkMaterial.getAdditionalRenderState().setDepthTest(false);
        sparkMaterial.getAdditionalRenderState().setDepthWrite(false);
    }
    
    public void emitSparks(Vector3f position, int count) {
        for (int i = 0; i < count; i++) {
            emitSpark(position);
        }
    }
    
    private void emitSpark(Vector3f position) {
        // Quick, small sparks
        float lifetime = 0.15f + (float) Math.random() * 0.15f;
        float startSize = 3f + (float) Math.random() * 3f;
        float endSize = startSize * 0.5f;
        
        // Random velocity in all directions
        float angle = (float) (Math.random() * Math.PI * 2);
        float speed = 20f + (float) Math.random() * 30f;
        Vector3f velocity = new Vector3f(
            (float) Math.cos(angle) * speed,
            (float) Math.sin(angle) * speed,
            0
        );
        
        // Create a small quad for the spark
        Quad quad = new Quad(1, 1);
        Geometry sparkGeom = new Geometry("Spark", quad);
        sparkGeom.setMaterial(sparkMaterial.clone());
        sparkGeom.setQueueBucket(RenderQueue.Bucket.Gui);
        
        // Position it centered on the spark location
        sparkGeom.setLocalTranslation(position.x - startSize / 2, position.y - startSize / 2, 3.8f);
        sparkGeom.setLocalScale(startSize, startSize, 1);
        
        parentNode.attachChild(sparkGeom);
        
        Spark spark = new Spark(sparkGeom, position, velocity, lifetime, startSize, endSize);
        activeSparks.add(spark);
    }
    
    public void update(float tpf) {
        Iterator<Spark> iterator = activeSparks.iterator();
        while (iterator.hasNext()) {
            Spark spark = iterator.next();
            spark.age += tpf;
            
            if (spark.age >= spark.lifetime) {
                // Remove dead spark
                spark.geometry.removeFromParent();
                iterator.remove();
            } else {
                // Update spark appearance
                float progress = spark.age / spark.lifetime;
                
                // Fade out quickly
                float alpha = 1f - progress;
                
                // Shrink as it fades
                float size = spark.startSize + (spark.endSize - spark.startSize) * progress;
                
                // Move spark based on velocity
                spark.position.x += spark.velocity.x * tpf;
                spark.position.y += spark.velocity.y * tpf;
                
                // Update color with fade
                Material mat = spark.geometry.getMaterial();
                ColorRGBA color = new ColorRGBA(1f, 0.6f - progress * 0.4f, 0.2f - progress * 0.1f, alpha);
                mat.setColor("Color", color);
                
                ColorRGBA glowColor = new ColorRGBA(1f, 0.4f, 0.1f, alpha);
                mat.setColor("GlowColor", glowColor);
                
                // Update size and position
                spark.geometry.setLocalScale(size, size, 1);
                spark.geometry.setLocalTranslation(
                    spark.position.x - size / 2,
                    spark.position.y - size / 2,
                    3.8f
                );
            }
        }
    }
    
    public void clear() {
        for (Spark spark : activeSparks) {
            spark.geometry.removeFromParent();
        }
        activeSparks.clear();
    }
}
