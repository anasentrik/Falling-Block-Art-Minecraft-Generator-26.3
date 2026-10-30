package org.xet.experiments.builder.algorithm.height_map;

import org.xet.experiments.builder.algorithm.color.ImageColorEnum;

import java.awt.Color;
import java.util.PriorityQueue;

/**
 * Image-aware: the wave starts at the edges of the image and has a hard time crossing colour borders,
 * so it flows around the shapes in the picture (background first) before it fills them.
 */
public class OutlineGenerator extends GeneratorHeightMap {
    private static final int[] DX = {1, -1, 0, 0};
    private static final int[] DY = {0, 0, 1, -1};
    private static final double EDGE_PENALTY = 40;

    @Override
    public int[][] getHeightMap(int width, int height) {
        return generate(width, height, null).heightMap();
    }

    @Override
    public FallPlan generate(int width, int height, ImageColorEnum[][] colors) {
        double[][] distance = new double[width][height];
        int[][] depth = new int[width][height];
        boolean[][] done = new boolean[width][height];
        PriorityQueue<double[]> queue = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                distance[x][y] = Double.MAX_VALUE;
                if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
                    distance[x][y] = 0;
                    queue.add(new double[]{0, x, y});
                }
            }
        }

        while (!queue.isEmpty()) {
            double[] entry = queue.poll();
            int x = (int) entry[1];
            int y = (int) entry[2];
            if (done[x][y]) {
                continue;
            }
            done[x][y] = true;

            for (int d = 0; d < 4; d++) {
                int nx = x + DX[d];
                int ny = y + DY[d];
                if (!HeightMapUtils.isValid(nx, ny, width, height) || done[nx][ny]) {
                    continue;
                }
                double next = distance[x][y] + 1 + EDGE_PENALTY * colorDifference(colors, x, y, nx, ny);
                if (next < distance[nx][ny]) {
                    distance[nx][ny] = next;
                    depth[nx][ny] = depth[x][y] + 1;
                    queue.add(new double[]{next, nx, ny});
                }
            }
        }

        // falls in order of the weighted distance: the wave really stalls at colour borders
        return new FallPlan(depth, distance);
    }

    /** 0 = same colour, 1 = black vs white */
    private static double colorDifference(ImageColorEnum[][] colors, int x1, int y1, int x2, int y2) {
        if (colors == null) {
            return 0;
        }
        Color a = colors[x1][y1].getColor();
        Color b = colors[x2][y2].getColor();
        int dr = a.getRed() - b.getRed();
        int dg = a.getGreen() - b.getGreen();
        int db = a.getBlue() - b.getBlue();
        return Math.sqrt(dr * dr + dg * dg + db * db) / 441.7;
    }

    @Override
    public String getName() {
        return "outline";
    }
}
