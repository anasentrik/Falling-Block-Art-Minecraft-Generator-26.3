package org.xet.experiments.builder.algorithm.height_map;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * Helpers shared by the height map generators.
 *
 * A height map is only buildable if every cell with height h > 0 has a 4-neighbour with height h - 1
 * (the block hangs on a wall torch attached to that neighbour). The height is also the order in which
 * the blocks fall by default, but a generator can also describe "when" every pixel should fall and let
 * {@link #fromFallTime(double[][], boolean)} build a valid height map that respects that order.
 */
public final class HeightMapUtils {
    /** Highest Y a build may reach. Above ~1000 falling blocks drop as items. */
    public static final int MAX_BUILD_Y = 999;

    private static final int[] DX = {1, -1, 0, 0};
    private static final int[] DY = {0, 0, 1, -1};

    private HeightMapUtils() {
    }

    /**
     * Turns a "fall time" field (lower = falls earlier) into a plan: the pixels fall exactly in the order of the field,
     * the heights form a valid torch structure in which every pixel hangs on a neighbour that falls before it.
     * Cells with the lowest fall time (and local minima) become seeds standing on a torch on the ground.
     *
     * @param lowBuild true: keep the build as low as possible, false: shape the build after the pattern as well
     */
    public static FallPlan fromFallTime(double[][] fallTime, boolean lowBuild) {
        int width = fallTime.length;
        int height = fallTime[0].length;

        double min = Double.MAX_VALUE;
        for (double[] column : fallTime) {
            for (double value : column) {
                min = Math.min(min, value);
            }
        }
        double[][] shifted = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                shifted[x][y] = fallTime[x][y] - min;
            }
        }

        if (lowBuild) {
            return new FallPlan(grow(shifted, 1.0, true), shifted);
        }

        // pass 1: find the seeds, pass 2: scale the field so it is reachable from them (a cell can never be lower
        // than its grid distance to a seed) and build the final map
        int[][] firstPass = grow(shifted, 1.0, false);
        int[][] distance = distanceFromSeeds(firstPass);

        double scale = 1.0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (shifted[x][y] > 1e-9) {
                    scale = Math.max(scale, distance[x][y] / shifted[x][y]);
                }
            }
        }
        scale = Math.min(scale, 4.0);

        return new FallPlan(grow(shifted, scale, false), shifted);
    }

    /**
     * Grows a spanning forest in order of the fall time. Every cell hangs on an already grown neighbour (so it never
     * falls before its support): the lowest one, or the one that brings it closest to its scaled fall time.
     */
    private static int[][] grow(double[][] target, double scale, boolean lowest) {
        int width = target.length;
        int height = target[0].length;
        int[][] result = new int[width][height];
        for (int[] column : result) {
            Arrays.fill(column, -1);
        }

        // entries: {target, hasFilledNeighbour ? 0 : 1, x, y} - cells next to the filled area go first on ties,
        // so plateaus grow from their border instead of spawning new seeds
        PriorityQueue<double[]> queue = new PriorityQueue<>((a, b) -> {
            int cmp = Double.compare(a[0], b[0]);
            return cmp != 0 ? cmp : Double.compare(a[1], b[1]);
        });
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                queue.add(new double[]{target[x][y], 1, x, y});
            }
        }

        while (!queue.isEmpty()) {
            double[] entry = queue.poll();
            int x = (int) entry[2];
            int y = (int) entry[3];
            if (result[x][y] != -1) {
                continue;
            }

            double wanted = target[x][y] * scale;
            int best = -1;
            for (int d = 0; d < 4; d++) {
                int nx = x + DX[d];
                int ny = y + DY[d];
                if (!isValid(nx, ny, width, height) || result[nx][ny] == -1) {
                    continue;
                }
                int candidate = result[nx][ny] + 1;
                boolean better = lowest
                        ? best == -1 || candidate < best
                        : best == -1 || Math.abs(candidate - wanted) < Math.abs(best - wanted)
                                || (Math.abs(candidate - wanted) == Math.abs(best - wanted) && candidate < best);
                if (better) {
                    best = candidate;
                }
            }
            // the very first cells to fall all stand on the ground
            result[x][y] = best == -1 || target[x][y] <= 1e-9 ? 0 : best;

            for (int d = 0; d < 4; d++) {
                int nx = x + DX[d];
                int ny = y + DY[d];
                if (isValid(nx, ny, width, height) && result[nx][ny] == -1) {
                    queue.add(new double[]{target[nx][ny], 0, nx, ny});
                }
            }
        }

        return result;
    }

    /** Grid (BFS) distance of every cell to the nearest cell with height 0. */
    private static int[][] distanceFromSeeds(int[][] heightMap) {
        int width = heightMap.length;
        int height = heightMap[0].length;
        int[][] distance = new int[width][height];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (heightMap[x][y] == 0) {
                    queue.add(new int[]{x, y});
                } else {
                    distance[x][y] = -1;
                }
            }
        }
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            for (int d = 0; d < 4; d++) {
                int nx = c[0] + DX[d];
                int ny = c[1] + DY[d];
                if (isValid(nx, ny, width, height) && distance[nx][ny] == -1) {
                    distance[nx][ny] = distance[c[0]][c[1]] + 1;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
        return distance;
    }

    /** Fills the field with f(dx, dy), where dx/dy are measured from the centre of the image. */
    public static double[][] fieldFromCenter(int width, int height, CenterField f) {
        double cx = (width - 1) / 2.0;
        double cy = (height - 1) / 2.0;
        double[][] field = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                field[x][y] = f.apply(x - cx, y - cy);
            }
        }
        return field;
    }

    public static int maxHeight(int[][] heightMap) {
        int max = 0;
        for (int[] column : heightMap) {
            for (int value : column) {
                max = Math.max(max, value);
            }
        }
        return max;
    }

    /** True if every block has a supporting neighbour one step lower. */
    public static boolean isBuildable(int[][] heightMap) {
        int width = heightMap.length;
        int height = heightMap[0].length;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int value = heightMap[x][y];
                if (value < 0) {
                    return false;
                }
                if (value == 0) {
                    continue;
                }
                boolean supported = false;
                for (int d = 0; d < 4 && !supported; d++) {
                    int nx = x + DX[d];
                    int ny = y + DY[d];
                    supported = isValid(nx, ny, width, height) && heightMap[nx][ny] == value - 1;
                }
                if (!supported) {
                    return false;
                }
            }
        }
        return true;
    }

    static boolean isValid(int x, int y, int width, int height) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    @FunctionalInterface
    public interface CenterField {
        double apply(double dx, double dy);
    }
}
