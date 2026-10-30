package org.xet.experiments.builder.algorithm.height_map;

/**
 * Result of a generator.
 *
 * @param heightMap how high every pixel is built (must be buildable, see {@link HeightMapUtils#isBuildable(int[][])})
 * @param fallOrder when every pixel falls during /image collapse (lower = earlier). A pixel must never fall before the
 *                  neighbour it hangs on (the one with height - 1).
 */
public record FallPlan(int[][] heightMap, double[][] fallOrder) {
    /**
     * Same heights, but every fall time gets a random offset of up to {@code fraction} of the whole range, so a tidy
     * pattern becomes a bit ragged. A block never falls before the earliest block it hangs on.
     */
    public FallPlan jittered(java.util.Random random, double fraction) {
        int width = heightMap.length;
        int height = heightMap[0].length;

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double[] column : fallOrder) {
            for (double value : column) {
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
        }
        double range = max - min;
        if (range <= 0) {
            return this;
        }

        double[][] noisy = new double[width][height];
        java.util.List<java.util.List<int[]>> levels = new java.util.ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                noisy[x][y] = fallOrder[x][y] + (random.nextDouble() * 2 - 1) * fraction * range;
                while (levels.size() <= heightMap[x][y]) {
                    levels.add(new java.util.ArrayList<>());
                }
                levels.get(heightMap[x][y]).add(new int[]{x, y});
            }
        }

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        for (int level = 1; level < levels.size(); level++) {
            for (int[] cell : levels.get(level)) {
                double earliestSupport = Double.MAX_VALUE;
                for (int d = 0; d < 4; d++) {
                    int nx = cell[0] + dx[d];
                    int ny = cell[1] + dy[d];
                    if (nx >= 0 && ny >= 0 && nx < width && ny < height && heightMap[nx][ny] == level - 1) {
                        earliestSupport = Math.min(earliestSupport, noisy[nx][ny]);
                    }
                }
                if (earliestSupport != Double.MAX_VALUE && noisy[cell[0]][cell[1]] < earliestSupport) {
                    noisy[cell[0]][cell[1]] = earliestSupport;
                }
            }
        }
        return new FallPlan(heightMap, noisy);
    }

    /** Plan for generators that only define heights: blocks fall from the lowest to the highest. */
    public static FallPlan byHeight(int[][] heightMap) {
        double[][] order = new double[heightMap.length][heightMap[0].length];
        for (int x = 0; x < heightMap.length; x++) {
            for (int y = 0; y < heightMap[0].length; y++) {
                order[x][y] = heightMap[x][y];
            }
        }
        return new FallPlan(heightMap, order);
    }
}
