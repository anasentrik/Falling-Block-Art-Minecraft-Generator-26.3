package org.xet.experiments.builder.algorithm.height_map;

import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * The same kind of generation as {@link WaveNoiseGenerator}, but fast enough for big images.
 *
 * The rules are the same: a flood from the centre that always continues at the cell with the highest Perlin noise,
 * visits the neighbours in random order and accepts a new height with a probability that drops once about
 * {@value #MAX_DUPLICATES} cells share that height. The old version needed ~10 s for 96x96 because every time its queue ran
 * dry it scanned the whole image and computed the noise of every candidate again. Here the noise is computed once and
 * the cells next to the flooded area are kept in a queue of their own, so everything is O(n log n).
 */
public class WaveNoiseFastGenerator extends GeneratorHeightMap {
    private static final int[] DX = {0, 0, -1, 1};
    private static final int[] DY = {-1, 1, 0, 0};
    private static final int MAX_DUPLICATES = 10;
    private static final int MAX_ITERATIONS_MULTIPLIER = 3;

    private final String name;
    private final double noiseScale;

    /** @param noiseScale 0.15 = blobs of about 6 cells (same as wave_noise), smaller = bigger blobs */
    public WaveNoiseFastGenerator(String name, double noiseScale) {
        this.name = name;
        this.noiseScale = noiseScale;
    }

    @Override
    public int[][] getHeightMap(int width, int height) {
        return new Flood(width, height, noiseScale).run();
    }

    @Override
    public String getName() {
        return name;
    }

    private static final class Flood {
        private final int width;
        private final int height;
        private final int total;
        private final Random random = new Random();
        private final double[] noise;
        private final int[] heights;
        private final int[] heightCount;
        private final boolean[] inFrontier;
        private final PriorityQueue<Integer> queue;
        private final PriorityQueue<Integer> frontier;
        // pairs (flooded cell, empty neighbour), decides how many cells a new wave takes
        private int frontierEdges = 0;
        private int filled = 0;

        Flood(int width, int height, double noiseScale) {
            this.width = width;
            this.height = height;
            this.total = width * height;
            this.noise = new double[total];
            this.heights = new int[total];
            this.heightCount = new int[total + 2];
            this.inFrontier = new boolean[total];
            Arrays.fill(heights, -1);

            WaveNoiseGenerator.PerlinNoise perlin = new WaveNoiseGenerator.PerlinNoise(random.nextInt());
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    noise[x * height + y] = perlin.perlin(x * noiseScale, y * noiseScale);
                }
            }

            // highest noise first
            this.queue = new PriorityQueue<>((a, b) -> Double.compare(noise[b], noise[a]));
            this.frontier = new PriorityQueue<>((a, b) -> Double.compare(noise[b], noise[a]));
        }

        int[][] run() {
            int start = (width / 2) * height + height / 2;
            fill(start, 0);
            queue.add(start);

            int maxIterations = total * MAX_ITERATIONS_MULTIPLIER;
            int iterations = 0;
            int[] dirs = {0, 1, 2, 3};

            while (filled < total && iterations++ < maxIterations) {
                if (queue.isEmpty()) {
                    spawnNewWave();
                }

                Integer current = queue.poll();
                if (current == null) {
                    continue;
                }

                // random order of the neighbours
                for (int i = 3; i > 0; i--) {
                    int j = random.nextInt(i + 1);
                    int tmp = dirs[i];
                    dirs[i] = dirs[j];
                    dirs[j] = tmp;
                }

                int cx = current / height;
                int cy = current % height;
                for (int dir : dirs) {
                    int nx = cx + DX[dir];
                    int ny = cy + DY[dir];
                    if (nx < 0 || ny < 0 || nx >= width || ny >= height) {
                        continue;
                    }
                    int neighbour = nx * height + ny;
                    if (heights[neighbour] != -1) {
                        continue;
                    }

                    int newHeight = heights[current] + 1;
                    // a height that is already crowded gets accepted less and less often
                    double probability = Math.max(0.3, 1.0 - heightCount[newHeight] / (double) MAX_DUPLICATES);
                    if (random.nextDouble() > probability) {
                        continue;
                    }

                    fill(neighbour, newHeight);
                    queue.add(neighbour);
                }
            }

            if (filled < total) {
                completeUnfilled();
            }

            int[][] result = new int[width][height];
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    result[x][y] = heights[x * height + y];
                }
            }
            return result;
        }

        private void fill(int cell, int value) {
            heights[cell] = value;
            heightCount[value]++;
            filled++;

            int cx = cell / height;
            int cy = cell % height;
            for (int d = 0; d < 4; d++) {
                int nx = cx + DX[d];
                int ny = cy + DY[d];
                if (nx < 0 || ny < 0 || nx >= width || ny >= height) {
                    continue;
                }
                int neighbour = nx * height + ny;
                if (heights[neighbour] != -1) {
                    frontierEdges--;
                } else {
                    frontierEdges++;
                    if (!inFrontier[neighbour]) {
                        inFrontier[neighbour] = true;
                        frontier.add(neighbour);
                    }
                }
            }
        }

        /**
         * The queue ran dry: start over at the cells next to the flooded area that have the highest noise.
         * Like in wave_noise, every pair (flooded cell, empty neighbour) is one candidate and the best
         * max(1, candidates / 5) candidates (at most 10) are looked at; a cell takes the height of the first
         * flooded neighbour in reading order (left, up, down, right) and is skipped if that height is crowded.
         */
        private void spawnNewWave() {
            int slots = Math.min(10, Math.max(1, frontierEdges / 5));

            // choose first, fill afterwards: the candidates are a snapshot of the situation before this wave
            java.util.List<int[]> chosen = new java.util.ArrayList<>();
            while (slots > 0 && !frontier.isEmpty()) {
                int cell = frontier.poll();
                if (heights[cell] != -1) {
                    continue;
                }
                slots -= floodedNeighbours(cell);
                chosen.add(new int[]{cell, firstNeighbourHeight(cell) + 1});
            }

            for (int[] candidate : chosen) {
                if (heightCount[candidate[1]] < MAX_DUPLICATES * 2) {
                    fill(candidate[0], candidate[1]);
                    queue.add(candidate[0]);
                } else {
                    // stays available for the next wave
                    frontier.add(candidate[0]);
                }
            }
        }

        private int floodedNeighbours(int cell) {
            int cx = cell / height;
            int cy = cell % height;
            int count = 0;
            for (int d = 0; d < 4; d++) {
                int nx = cx + DX[d];
                int ny = cy + DY[d];
                if (nx >= 0 && ny >= 0 && nx < width && ny < height && heights[nx * height + ny] != -1) {
                    count++;
                }
            }
            return count;
        }

        /** Height of the first flooded neighbour in reading order: left, up, down, right. */
        private int firstNeighbourHeight(int cell) {
            int cx = cell / height;
            int cy = cell % height;
            int[][] order = {{-1, 0}, {0, -1}, {0, 1}, {1, 0}};
            for (int[] step : order) {
                int nx = cx + step[0];
                int ny = cy + step[1];
                if (nx >= 0 && ny >= 0 && nx < width && ny < height && heights[nx * height + ny] != -1) {
                    return heights[nx * height + ny];
                }
            }
            throw new IllegalStateException("cell without flooded neighbour in the frontier");
        }

        /** Emergency fill for the cells that are still empty: one higher than the lowest neighbour. */
        private void completeUnfilled() {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int cell = x * height + y;
                    if (heights[cell] != -1) {
                        continue;
                    }

                    int minHeight = Integer.MAX_VALUE;
                    for (int d = 0; d < 4; d++) {
                        int nx = x + DX[d];
                        int ny = y + DY[d];
                        if (nx >= 0 && ny >= 0 && nx < width && ny < height && heights[nx * height + ny] != -1) {
                            minHeight = Math.min(minHeight, heights[nx * height + ny]);
                        }
                    }
                    // a cell without any flooded neighbour starts on the ground
                    fill(cell, minHeight == Integer.MAX_VALUE ? 0 : minHeight + 1);
                }
            }
        }
    }
}
