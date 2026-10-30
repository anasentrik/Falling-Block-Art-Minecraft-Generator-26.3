package org.xet.experiments.builder.algorithm.height_map;

import org.xet.experiments.builder.algorithm.color.ImageColorEnum;

import java.util.Random;

/**
 * One wave that sweeps across the image.
 * sine: left to right with a wavy front, curtain: top to bottom with drips that run ahead like a melting curtain.
 */
public class SweepGenerator extends PatternGenerator {
    private final boolean curtain;

    public SweepGenerator(String name, boolean curtain) {
        super(name, false);
        this.curtain = curtain;
    }

    @Override
    protected double[][] fallTime(int width, int height, ImageColorEnum[][] colors) {
        double[][] field = new double[width][height];

        if (curtain) {
            // smooth random drip length per column
            Random random = new Random();
            double[] drip = new double[width];
            double[] knots = new double[width / 4 + 2];
            for (int i = 0; i < knots.length; i++) {
                knots[i] = random.nextDouble();
            }
            for (int x = 0; x < width; x++) {
                double position = x / 4.0;
                int i = (int) position;
                double t = position - i;
                t = t * t * (3 - 2 * t);
                drip[x] = (knots[i] * (1 - t) + knots[i + 1] * t) * height * 0.35;
            }
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    field[x][y] = y - drip[x];
                }
            }
        } else {
            double amplitude = Math.max(2, width / 8.0);
            double period = Math.max(8, height / 2.0);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    field[x][y] = x + amplitude * Math.sin(2 * Math.PI * y / period);
                }
            }
        }

        return field;
    }
}
