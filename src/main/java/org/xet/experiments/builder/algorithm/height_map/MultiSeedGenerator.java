package org.xet.experiments.builder.algorithm.height_map;

import org.xet.experiments.builder.algorithm.color.ImageColorEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Several round waves at the same time that run into each other.
 * raindrops: random drop points, corners: the four corners, tiles: a small wave in every 8x8 tile.
 */
public class MultiSeedGenerator extends PatternGenerator {
    public enum Mode { RAINDROPS, CORNERS, TILES }

    private static final int TILE_SIZE = 8;

    private final Mode mode;

    public MultiSeedGenerator(String name, Mode mode) {
        super(name, false);
        this.mode = mode;
    }

    @Override
    protected double[][] fallTime(int width, int height, ImageColorEnum[][] colors) {
        double[][] field = new double[width][height];

        if (mode == Mode.TILES) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    double cx = (x / TILE_SIZE) * TILE_SIZE + (Math.min(TILE_SIZE, width - (x / TILE_SIZE) * TILE_SIZE) - 1) / 2.0;
                    double cy = (y / TILE_SIZE) * TILE_SIZE + (Math.min(TILE_SIZE, height - (y / TILE_SIZE) * TILE_SIZE) - 1) / 2.0;
                    field[x][y] = Math.hypot(x - cx, y - cy);
                }
            }
            return field;
        }

        List<double[]> seeds = new ArrayList<>();
        if (mode == Mode.CORNERS) {
            seeds.add(new double[]{0, 0});
            seeds.add(new double[]{width - 1, 0});
            seeds.add(new double[]{0, height - 1});
            seeds.add(new double[]{width - 1, height - 1});
        } else {
            Random random = new Random();
            int count = Math.max(3, Math.min(12, width * height / 400));
            for (int i = 0; i < count; i++) {
                seeds.add(new double[]{random.nextInt(width), random.nextInt(height)});
            }
        }

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double best = Double.MAX_VALUE;
                for (double[] seed : seeds) {
                    best = Math.min(best, Math.hypot(x - seed[0], y - seed[1]));
                }
                field[x][y] = best;
            }
        }

        return field;
    }
}
