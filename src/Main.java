/** Abdullahi S
 *
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class Main extends JPanel {
    private final int WIDTH = 800;
    private final int HEIGHT = 600;
    private BufferedImage image;
    private double lightX = 1;
    private double lightY = 1;
    private double lightZ = -0.5;

    public Main() {
        image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        renderScene();
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                updateLightPosition(e.getX(), e.getY());
            }
            @Override
            public void mouseMoved(MouseEvent e) {
                updateLightPosition(e.getX(), e.getY());
            }
        });
    }

    private void updateLightPosition(int mouseX, int mouseY) {
        lightX = (mouseX - WIDTH / 2.0) / (WIDTH / 2.0);
        lightY = (mouseY - HEIGHT / 2.0) / (HEIGHT / 2.0);
        renderScene();
        repaint();
    }

    private void renderScene() {
        int maxDepth = 3;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                double nx = (x - WIDTH / 2.0) / (WIDTH / 2.0);
                double ny = (y - HEIGHT / 2.0) / (HEIGHT / 2.0);
                Vector3 origin = new Vector3(0, 0, 0);
                Vector3 direction = new Vector3(nx, ny, -1).normalize();
                Color color = traceRay(origin, direction, maxDepth);
                image.setRGB(x, y, color.getRGB());
            }
        }
    }

    private Color traceRay(Vector3 origin, Vector3 direction, int depth) {
        Intersection closest = null;
        Intersection sp = intersectSphere(origin, direction, new Vector3(0, 0, -1), 0.5, new Color(200, 50, 50), 0.3);
        if (sp != null) {
            closest = sp;
        }
        Intersection floorInt = intersectPlane(origin, direction, new Vector3(0, -0.7, 0), new Vector3(0, 1, 0), new Color(100, 100, 100), 0.2);
        if (floorInt != null && (closest == null || floorInt.t < closest.t)) {
            closest = floorInt;
        }
        Intersection ceilingInt = intersectPlane(origin, direction, new Vector3(0, 0.7, 0), new Vector3(0, -1, 0), new Color(150, 150, 150), 0.2);
        if (ceilingInt != null && (closest == null || ceilingInt.t < closest.t)) {
            closest = ceilingInt;
        }
        Intersection leftWallInt = intersectPlane(origin, direction, new Vector3(-1, 0, 0), new Vector3(1, 0, 0), new Color(50, 50, 200), 0.2);
        if (leftWallInt != null && (closest == null || leftWallInt.t < closest.t)) {
            closest = leftWallInt;
        }
        Intersection rightWallInt = intersectPlane(origin, direction, new Vector3(1, 0, 0), new Vector3(-1, 0, 0), new Color(50, 50, 200), 0.2);
        if (rightWallInt != null && (closest == null || rightWallInt.t < closest.t)) {
            closest = rightWallInt;
        }
        Intersection backWallInt = intersectPlane(origin, direction, new Vector3(0, 0, -3), new Vector3(0, 0, 1), new Color(50, 200, 50), 0.2);
        if (backWallInt != null && (closest == null || backWallInt.t < closest.t)) {
            closest = backWallInt;
        }
        if (closest == null) {
            return Color.BLACK;
        }
        Vector3 hitPoint = origin.add(direction.multiply(closest.t));
        Vector3 lightPos = new Vector3(lightX, lightY, lightZ);
        Vector3 toLight = lightPos.subtract(hitPoint).normalize();
        double dot = Math.max(0, closest.normal.dot(toLight));
        double brightness = Math.pow(dot, 2.0);
        Color baseColor = closest.color;
        int r = clamp((int)(baseColor.getRed() * brightness));
        int g = clamp((int)(baseColor.getGreen() * brightness));
        int b = clamp((int)(baseColor.getBlue() * brightness));
        Color localColor = new Color(r, g, b);
        double reflectivity = closest.reflectivity;
        Color reflectedColor = Color.BLACK;
        if (depth > 0 && reflectivity > 0) {
            Vector3 reflectDir = direction.subtract(closest.normal.multiply(2 * direction.dot(closest.normal))).normalize();
            Vector3 newOrigin = hitPoint.add(closest.normal.multiply(1e-4));
            reflectedColor = traceRay(newOrigin, reflectDir, depth - 1);
        }
        int finalR = clamp((int)((1 - reflectivity) * localColor.getRed() + reflectivity * reflectedColor.getRed()));
        int finalG = clamp((int)((1 - reflectivity) * localColor.getGreen() + reflectivity * reflectedColor.getGreen()));
        int finalB = clamp((int)((1 - reflectivity) * localColor.getBlue() + reflectivity * reflectedColor.getBlue()));
        return new Color(finalR, finalG, finalB);
    }

    private class Intersection {
        double t;
        Vector3 point;
        Vector3 normal;
        Color color;
        double reflectivity;
    }

    private Intersection intersectSphere(Vector3 origin, Vector3 direction, Vector3 center, double radius, Color color, double reflectivity) {
        Vector3 oc = origin.subtract(center);
        double a = direction.dot(direction);
        double b = 2.0 * oc.dot(direction);
        double c = oc.dot(oc) - radius * radius;
        double discriminant = b * b - 4 * a * c;
        if (discriminant < 0) return null;
        double t = (-b - Math.sqrt(discriminant)) / (2 * a);
        if (t < 0) t = (-b + Math.sqrt(discriminant)) / (2 * a);
        if (t < 0) return null;
        Intersection inter = new Intersection();
        inter.t = t;
        inter.point = origin.add(direction.multiply(t));
        inter.normal = inter.point.subtract(center).normalize();
        inter.color = color;
        inter.reflectivity = reflectivity;
        return inter;
    }

    private Intersection intersectPlane(Vector3 origin, Vector3 direction, Vector3 pointOnPlane, Vector3 normal, Color color, double reflectivity) {
        double denom = direction.dot(normal);
        if (Math.abs(denom) < 1e-6) return null;
        double t = (pointOnPlane.subtract(origin)).dot(normal) / denom;
        if (t < 0) return null;
        Intersection inter = new Intersection();
        inter.t = t;
        inter.point = origin.add(direction.multiply(t));
        inter.normal = normal;
        inter.color = color;
        inter.reflectivity = reflectivity;
        return inter;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static class Vector3 {
        double x, y, z;
        Vector3(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        Vector3 add(Vector3 v) {
            return new Vector3(x + v.x, y + v.y, z + v.z);
        }
        Vector3 subtract(Vector3 v) {
            return new Vector3(x - v.x, y - v.y, z - v.z);
        }
        Vector3 multiply(double scalar) {
            return new Vector3(x * scalar, y * scalar, z * scalar);
        }
        double dot(Vector3 v) {
            return x * v.x + y * v.y + z * v.z;
        }
        Vector3 normalize() {
            double len = Math.sqrt(x * x + y * y + z * z);
            return new Vector3(x / len, y / len, z / len);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, this);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("rayt racing test");
        Main panel = new Main();
        frame.add(panel);
        frame.setSize(panel.WIDTH, panel.HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
