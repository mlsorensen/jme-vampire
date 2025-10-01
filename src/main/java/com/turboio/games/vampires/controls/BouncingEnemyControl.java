package com.turboio.games.vampires.controls;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import com.turboio.games.vampires.perimeter.Perimeter;

import java.util.List;
import java.util.Random;

public class BouncingEnemyControl extends AbstractControl implements EnemyMovementControl {

    private final Random random = new Random();

    private float speed;
    private final Vector3f velocity = new Vector3f();
    private Perimeter perimeter;

    public BouncingEnemyControl(Perimeter perimeter, float speed) {
        this.perimeter = perimeter;
        this.speed = speed;
        chooseNewDirection();
    }

    @Override
    public void setPerimeter(Perimeter perimeter) {
        this.perimeter = perimeter;
    }

    @Override
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (!isEnabled() || spatial == null || perimeter == null) {
            return;
        }

        final float originalZ = spatial.getLocalTranslation().z;
        Vector3f currentPos = spatial.getLocalTranslation();
        Vector3f step = velocity.mult(speed * tpf);
        Vector3f nextPos = currentPos.add(step);

        float radius = ((Number) spatial.getUserData("radius")).floatValue();

        List<Vector3f> vertices = perimeter.getVertices();
        int n = vertices.size();
        Vector3f wallToReflect = null;
        Vector3f wallA = null;
        Vector3f wallB = null;
        boolean collision = false;

        // Use the original logic to find the wall to bounce off of.
        float minDistance = Float.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            Vector3f a = vertices.get(i);
            Vector3f b = vertices.get((i + 1) % n);
            float dist = distanceToSegment(nextPos, a, b);
            if (dist < radius) {
                collision = true;
                if (dist < minDistance) {
                    minDistance = dist;
                    wallToReflect = b.subtract(a);
                    wallA = a;
                    wallB = b;
                }
            }
        }

        if (collision) {
            if (wallToReflect != null && wallToReflect.lengthSquared() > 0.0001f) {
                // 1. Calculate the wall normal.
                Vector3f wallNormal = new Vector3f(-wallToReflect.y, wallToReflect.x, 0).normalizeLocal();

                // 2. Ensure the normal points inwards.
                Vector3f projection = projectOnSegment(nextPos, wallA, wallB);
                Vector3f testPoint = projection.add(wallNormal.mult(0.1f));
                if (!perimeter.contains(testPoint)) {
                    wallNormal.negateLocal();
                }

                // 3. Reflect velocity (the part that worked well).
                float dot = velocity.dot(wallNormal);
                velocity.subtractLocal(wallNormal.mult(2 * dot));

                // 4. TARGETED FIX: Correct the position to prevent tunneling.
                // Set position to the point of impact, pushed back by the radius.
                nextPos = projection.add(wallNormal.mult(radius));
                nextPos.z = originalZ; // Preserve Z-coordinate
            }
        }

        spatial.setLocalTranslation(nextPos);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}

    private void chooseNewDirection() {
        do {
            velocity.set(random.nextFloat() * 2f - 1f, random.nextFloat() * 2f - 1f, 0f);
        } while (velocity.lengthSquared() == 0f);
        velocity.normalizeLocal();
    }

    private Vector3f projectOnSegment(Vector3f point, Vector3f start, Vector3f end) {
        Vector3f seg = end.subtract(start);
        Vector3f toPoint = point.subtract(start);
        float segLenSq = seg.lengthSquared();
        if (segLenSq == 0f) {
            return start.clone();
        }
        float t = Math.max(0f, Math.min(1f, toPoint.dot(seg) / segLenSq));
        return start.add(seg.mult(t));
    }

    private float distanceToSegment(Vector3f point, Vector3f start, Vector3f end) {
        return point.distance(projectOnSegment(point, start, end));
    }
}
