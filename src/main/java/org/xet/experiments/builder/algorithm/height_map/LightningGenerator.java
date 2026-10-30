package org.xet.experiments.builder.algorithm.height_map;

import org.xet.experiments.builder.algorithm.color.ImageColorEnum;

import java.util.PriorityQueue;
import java.util.Arrays;
import java.util.Random;

/**
 * Cracks shoot out from the centre like lightning along random thin channels, the areas between them follow later.
 */
public class LightningGenerator extends PatternGenerator {
    private static final int[] DX = {1, -1, 0, 0};
    private static final int[] DY = {0, 0, 1, -1};
    private static final double NOISE_SCALE = 0.09;
    private static final double SLOW_COST = 30;

    public LightningGenerator() {
        super("lightning", false);
    }

    @Override
    protected double[][] fallTime(int width, int height, ImageColorEnum[][] colors) {
        // cheap channels where the noise crosses zero, expensive everywhere else
        WaveNoiseGenerator.PerlinNoise noise = new WaveNoiseGenerator.PerlinNoise(new Random().nextInt());
        double[][] cost = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double channel = Math.min(1, Math.abs(noise.perlin(x * NOISE_SCALE + 0.5, y * NOISE_SCALE + 0.5)) * 6);
                cost[x][y] = 1 + SLOW_COST * channel;
            }
        }

        double[][] distance = new double[width][height];
        for (double[] column : distance) {
            Arrays.fill(column, Double.MAX_VALUE);
        }
        int sx = width / 2;
        int sy = height / 2;
        distance[sx][sy] = 0;
        PriorityQueue<double[]> queue = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));
        queue.add(new double[]{0, sx, sy});

        while (!queue.isEmpty()) {
            double[] entry = queue.poll();
            int x = (int) entry[1];
            int y = (int) entry[2];
            if (entry[0] > distance[x][y]) {
                continue;
            }
            for (int d = 0; d < 4; d++) {
                int nx = x + DX[d];
                int ny = y + DY[d];
                if (!HeightMapUtils.isValid(nx, ny, width, height)) {
                    continue;
                }
                double next = distance[x][y] + cost[nx][ny];
                if (next < distance[nx][ny]) {
                    distance[nx][ny] = next;
                    queue.add(new double[]{next, nx, ny});
                }
            }
        }

        return distance;
    }
}
