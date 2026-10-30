package org.xet.experiments.builder.algorithm.height_map;

import org.xet.experiments.builder.algorithm.color.ImageColorEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * A wave from the centre whose front has the outline of a shape (heart, star, hexagon, flower) that keeps growing.
 */
public class ShapeWaveGenerator extends PatternGenerator {
    private static final int ANGLE_STEPS = 1440;

    // distance from the centre to the outline for every angle, largest = 1
    private final double[] radiusByAngle = new double[ANGLE_STEPS];

    /** @param outline polygon around (0, 0); x to the right, y down (north of the image is up) */
    public ShapeWaveGenerator(String name, List<double[]> outline) {
        super(name, false);

        double max = 0;
        for (int i = 0; i < ANGLE_STEPS; i++) {
            double angle = 2 * Math.PI * i / ANGLE_STEPS;
            radiusByAngle[i] = rayDistance(outline, Math.cos(angle), Math.sin(angle));
            max = Math.max(max, radiusByAngle[i]);
        }
        double[] raw = radiusByAngle.clone();
        for (int i = 0; i < ANGLE_STEPS; i++) {
            // a ray that exactly hits a vertex can miss the outline, take the neighbours instead
            double neighbours = Math.min(raw[(i + ANGLE_STEPS - 1) % ANGLE_STEPS], raw[(i + 1) % ANGLE_STEPS]);
            radiusByAngle[i] = Math.max(Math.max(raw[i], neighbours) / max, 0.05);
        }
    }

    @Override
    protected double[][] fallTime(int width, int height, ImageColorEnum[][] colors) {
        return HeightMapUtils.fieldFromCenter(width, height, (dx, dy) -> {
            double angle = Math.atan2(dy, dx);
            if (angle < 0) {
                angle += 2 * Math.PI;
            }
            int index = (int) Math.round(angle / (2 * Math.PI) * ANGLE_STEPS) % ANGLE_STEPS;
            return Math.hypot(dx, dy) / radiusByAngle[index];
        });
    }

    /** Farthest point where the ray from (0, 0) in direction (dirX, dirY) crosses the polygon. */
    private static double rayDistance(List<double[]> polygon, double dirX, double dirY) {
        double best = 0;
        for (int i = 0; i < polygon.size(); i++) {
            double[] a = polygon.get(i);
            double[] b = polygon.get((i + 1) % polygon.size());
            double ex = b[0] - a[0];
            double ey = b[1] - a[1];
            double denominator = dirX * ey - dirY * ex;
            if (Math.abs(denominator) < 1e-12) {
                continue;
            }
            double t = (a[0] * ey - a[1] * ex) / denominator;
            double u = (a[0] * dirY - a[1] * dirX) / denominator;
            if (t > 0 && u >= 0 && u <= 1) {
                best = Math.max(best, t);
            }
        }
        return best;
    }

    /** Regular polygon with a corner pointing up. */
    public static List<double[]> regular(int corners) {
        List<double[]> points = new ArrayList<>();
        for (int i = 0; i < corners; i++) {
            double angle = -Math.PI / 2 + 2 * Math.PI * i / corners;
            points.add(new double[]{Math.cos(angle), Math.sin(angle)});
        }
        return points;
    }

    /** Cog wheel with flat teeth. */
    public static List<double[]> gear(int teeth) {
        List<double[]> points = new ArrayList<>();
        double step = 2 * Math.PI / teeth;
        for (int i = 0; i < teeth; i++) {
            double a = i * step;
            double[][] corners = {{-0.36, 0.78}, {-0.2, 1}, {0.2, 1}, {0.36, 0.78}};
            for (double[] corner : corners) {
                points.add(new double[]{corner[1] * Math.cos(a + corner[0] * step), corner[1] * Math.sin(a + corner[0] * step)});
            }
        }
        return points;
    }

    /** Plus sign. */
    public static List<double[]> cross() {
        double[][] corners = {{-0.3, -1}, {0.3, -1}, {0.3, -0.3}, {1, -0.3}, {1, 0.3}, {0.3, 0.3},
                {0.3, 1}, {-0.3, 1}, {-0.3, 0.3}, {-1, 0.3}, {-1, -0.3}, {-0.3, -0.3}};
        List<double[]> points = new ArrayList<>();
        for (double[] corner : corners) {
            points.add(corner);
        }
        return points;
    }

    /** The butterfly curve (wings on both sides, antennae on top). */
    public static List<double[]> butterfly() {
        List<double[]> points = new ArrayList<>();
        int steps = 1440;
        for (int i = 0; i < steps; i++) {
            double t = 12 * Math.PI * i / steps;
            double r = Math.exp(Math.sin(t)) - 2 * Math.cos(4 * t) + Math.pow(Math.sin((2 * t - Math.PI) / 24), 5);
            points.add(new double[]{r * Math.sin(t), -r * Math.cos(t)});
        }
        return points;
    }

    /** Rose with the given number of lobes; {@code floor} is how deep the gaps between the lobes are (0 = to the centre). */
    public static List<double[]> rose(int lobes, double floor) {
        List<double[]> points = new ArrayList<>();
        for (int i = 0; i < 720; i++) {
            double angle = 2 * Math.PI * i / 720;
            double radius = floor + (1 - floor) * Math.pow(Math.abs(Math.cos(lobes * angle / 2)), 0.7);
            points.add(new double[]{radius * Math.cos(angle), radius * Math.sin(angle)});
        }
        return points;
    }

    public static List<double[]> heart() {
        List<double[]> points = new ArrayList<>();
        for (int i = 0; i < 360; i++) {
            double t = 2 * Math.PI * i / 360;
            double x = 16 * Math.pow(Math.sin(t), 3);
            double y = 13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t);
            // shift so the wave starts between the two lobes and flip y (image y points down)
            points.add(new double[]{x, -(y - 2)});
        }
        return points;
    }

    public static List<double[]> star(int spikes, double innerRadius) {
        List<double[]> points = new ArrayList<>();
        for (int i = 0; i < spikes * 2; i++) {
            double radius = i % 2 == 0 ? 1 : innerRadius;
            double angle = -Math.PI / 2 + Math.PI * i / spikes;
            points.add(new double[]{radius * Math.cos(angle), radius * Math.sin(angle)});
        }
        return points;
    }

    public static List<double[]> flower(int petals) {
        List<double[]> points = new ArrayList<>();
        for (int i = 0; i < 360; i++) {
            double angle = 2 * Math.PI * i / 360;
            double radius = 0.6 + 0.4 * Math.abs(Math.cos(petals * angle / 2));
            points.add(new double[]{radius * Math.cos(angle), radius * Math.sin(angle)});
        }
        return points;
    }
}
