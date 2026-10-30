package org.xet.experiments.builder.algorithm.height_map;

import org.xet.experiments.builder.algorithm.color.ImageColorEnum;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Fall time fields: every method returns, for every pixel, the moment it falls (lower = earlier).
 * Only the order matters, not the scale. See {@link LambdaPattern} and {@link HeightMapUtils#fromFallTime}.
 */
public final class Patterns {
    private static final int[] DX = {1, -1, 0, 0};
    private static final int[] DY = {0, 0, 1, -1};

    private Patterns() {
    }

    private static double size(int width, int height) {
        return Math.max(width, height);
    }

    // ---------------------------------------------------------------- sweeps and waves

    /** Clockwise sweep around the centre, starting at the top. With several arms the blades sweep at the same time. */
    public static double[][] radar(int width, int height, int arms) {
        double scale = size(width, height);
        return HeightMapUtils.fieldFromCenter(width, height, (dx, dy) -> {
            double turn = Math.atan2(dx, -dy) / (2 * Math.PI);
            if (turn < 0) {
                turn += 1;
            }
            double phase = turn * arms;
            return (phase - Math.floor(phase)) * scale;
        });
    }

    /** Two foci: ellipses around them (distance sum) or hyperbolas (distance difference). */
    public static double[][] focus(int width, int height, boolean hyperbola) {
        double focus = size(width, height) * 0.22;
        boolean horizontal = width >= height;
        return HeightMapUtils.fieldFromCenter(width, height, (dx, dy) -> {
            double a = horizontal ? Math.hypot(dx - focus, dy) : Math.hypot(dx, dy - focus);
            double b = horizontal ? Math.hypot(dx + focus, dy) : Math.hypot(dx, dy + focus);
            return hyperbola ? Math.abs(a - b) * 2 : a + b;
        });
    }

    /** Contour lines of smooth noise, like water running off a landscape. */
    public static double[][] contour(int width, int height) {
        WaveNoiseGenerator.PerlinNoise noise = new WaveNoiseGenerator.PerlinNoise(new Random().nextInt());
        double scale = 1.0 / Math.max(6, size(width, height) / 6.0);
        double amplitude = size(width, height) * 1.5;
        double[][] field = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double n = noise.perlin(x * scale + 0.37, y * scale + 0.71)
                        + 0.5 * noise.perlin(x * scale * 2 + 5.3, y * scale * 2 + 1.9);
                field[x][y] = n * amplitude;
            }
        }
        return field;
    }

    /** Shards: the image breaks into random pieces, every piece falls (as a small ripple) after the other. */
    public static double[][] voronoi(int width, int height) {
        Random random = new Random();
        int count = Math.max(4, width * height / 140);
        double[][] seeds = new double[count][2];
        for (double[] seed : seeds) {
            seed[0] = random.nextDouble() * width;
            seed[1] = random.nextDouble() * height;
        }
        List<Integer> turn = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            turn.add(i);
        }
        Collections.shuffle(turn, random);

        double spread = size(width, height) * 2.5;
        double[][] field = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int nearest = 0;
                double best = Double.MAX_VALUE;
                for (int i = 0; i < count; i++) {
                    double d = Math.hypot(x - seeds[i][0], y - seeds[i][1]);
                    if (d < best) {
                        best = d;
                        nearest = i;
                    }
                }
                field[x][y] = turn.get(nearest) / (double) count * spread + best * 0.8;
            }
        }
        return field;
    }

    /** Several round waves that start at random moments, like the first drops of a storm. */
    public static double[][] storm(int width, int height) {
        Random random = new Random();
        int count = Math.max(4, width * height / 120);
        double[][] drops = new double[count][3];
        for (double[] drop : drops) {
            drop[0] = random.nextDouble() * width;
            drop[1] = random.nextDouble() * height;
            drop[2] = random.nextDouble() * size(width, height) * 1.2;
        }
        double[][] field = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double best = Double.MAX_VALUE;
                for (double[] drop : drops) {
                    best = Math.min(best, drop[2] + Math.hypot(x - drop[0], y - drop[1]));
                }
                field[x][y] = best;
            }
        }
        return field;
    }

    /** Straight wave fronts from random directions that run through the image and meet. */
    public static double[][] crossfire(int width, int height) {
        Random random = new Random();
        int fronts = 3;
        double[] cos = new double[fronts];
        double[] sin = new double[fronts];
        double[] start = new double[fronts];
        double[] origin = new double[fronts];
        for (int k = 0; k < fronts; k++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            cos[k] = Math.cos(angle);
            sin[k] = Math.sin(angle);
            start[k] = random.nextDouble() * size(width, height) * 0.8;
            origin[k] = Math.min(Math.min(0, (width - 1) * cos[k]), Math.min((height - 1) * sin[k], (width - 1) * cos[k] + (height - 1) * sin[k]));
            origin[k] = Math.min(origin[k], 0);
        }
        double[][] field = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double best = Double.MAX_VALUE;
                for (int k = 0; k < fronts; k++) {
                    best = Math.min(best, x * cos[k] + y * sin[k] - origin[k] + start[k]);
                }
                field[x][y] = best;
            }
        }
        return field;
    }

    /** Wavy sea: a sweep from the top with two layers of waves on the front. */
    public static double[][] ocean(int width, int height) {
        Random random = new Random();
        double p1 = random.nextDouble() * 2 * Math.PI;
        double p2 = random.nextDouble() * 2 * Math.PI;
        double a1 = height * 0.10;
        double a2 = height * 0.05;
        double l1 = Math.max(6, width / 5.0);
        double l2 = Math.max(3, width / 11.0);
        double[][] field = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                field[x][y] = y + a1 * Math.sin(x / l1 + p1) + a2 * Math.sin(x / l2 + p2);
            }
        }
        return field;
    }

    /** V shaped front that runs from the top centre down, like a zipper being pulled. */
    public static double[][] chevron(int width, int height) {
        double cx = (width - 1) / 2.0;
        double[][] field = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                field[x][y] = y + Math.abs(x - cx) * 0.7;
            }
        }
        return field;
    }

    // ---------------------------------------------------------------- noise and interference

    /** Classic demo-scene plasma: the sum of a few sine waves, the pixels fall along its flowing contour lines. */
    public static double[][] plasma(int width, int height) {
        Random random = new Random();
        double k = 2 * Math.PI / (size(width, height) / 2.0);
        double[] phase = new double[4];
        for (int i = 0; i < phase.length; i++) {
            phase[i] = random.nextDouble() * 2 * Math.PI;
        }
        double cx = (width - 1) / 2.0;
        double cy = (height - 1) / 2.0;
        double[][] field = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                field[x][y] = Math.sin(x * k + phase[0])
                        + Math.sin(y * k * 1.3 + phase[1])
                        + Math.sin((x + y) * k * 0.8 + phase[2])
                        + Math.sin(Math.hypot(x - cx, y - cy) * k * 1.1 + phase[3]);
            }
        }
        return field;
    }

    /** Three sets of rings on top of each other (moire): the pixels fall along the interference pattern. */
    public static double[][] moire(int width, int height) {
        Random random = new Random();
        double k = 2 * Math.PI / Math.max(4, size(width, height) / 14.0);
        double[][] sources = new double[3][2];
        for (double[] source : sources) {
            source[0] = random.nextDouble() * width;
            source[1] = random.nextDouble() * height;
        }
        double[][] field = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double sum = 0;
                for (double[] source : sources) {
                    sum += Math.cos(Math.hypot(x - source[0], y - source[1]) * k);
                }
                field[x][y] = sum;
            }
        }
        return field;
    }

    /** A drop of ink in water: a ripple from the centre whose front is pushed around by noise. */
    public static double[][] inkdrop(int width, int height) {
        WaveNoiseGenerator.PerlinNoise noise = new WaveNoiseGenerator.PerlinNoise(new Random().nextInt());
        double scale = 1.0 / Math.max(6, size(width, height) / 8.0);
        double amplitude = size(width, height) * 0.35;
        return HeightMapUtils.fieldFromCenter(width, height, (dx, dy) -> {
            double n = noise.perlin(dx * scale + 40.3, dy * scale + 17.9) + 0.5 * noise.perlin(dx * scale * 2 + 3.1, dy * scale * 2 + 9.7);
            return Math.hypot(dx, dy) + n * amplitude;
        });
    }

    /** Noise folded eight times around the centre, so the ripple front is a symmetrical kaleidoscope. */
    public static double[][] kaleidoscope(int width, int height) {
        WaveNoiseGenerator.PerlinNoise noise = new WaveNoiseGenerator.PerlinNoise(new Random().nextInt());
        double scale = 1.0 / Math.max(6, size(width, height) / 9.0);
        double amplitude = size(width, height) * 0.3;
        return HeightMapUtils.fieldFromCenter(width, height, (dx, dy) -> {
            double ax = Math.abs(dx);
            double ay = Math.abs(dy);
            if (ay > ax) {
                double swap = ax;
                ax = ay;
                ay = swap;
            }
            double n = noise.perlin(ax * scale + 12.7, ay * scale + 5.3) + 0.5 * noise.perlin(ax * scale * 2 + 7.9, ay * scale * 2 + 1.1);
            return Math.hypot(dx, dy) * 0.8 + n * amplitude;
        });
    }

    /** Digital rain: every column starts at its own moment and falls from the top at its own speed. */
    public static double[][] matrix(int width, int height) {
        Random random = new Random();
        double[][] field = new double[width][height];
        for (int x = 0; x < width; x++) {
            double delay = random.nextDouble() * height * 1.5;
            double speed = 0.6 + random.nextDouble() * 1.0;
            for (int y = 0; y < height; y++) {
                field[x][y] = delay + y / speed;
            }
        }
        return field;
    }

    // ---------------------------------------------------------------- rows, columns, rings

    /** Venetian blinds: every third row falls first (top to bottom), then the rows in between. */
    public static double[][] blinds(int width, int height) {
        double[][] field = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                field[x][y] = (y % 3) * (double) height + y;
            }
        }
        return field;
    }

    /** Chessboard: first all the dark squares as a ripple from the centre, then all the light squares. */
    public static double[][] checker(int width, int height) {
        double cx = (width - 1) / 2.0;
        double cy = (height - 1) / 2.0;
        double phase = Math.hypot(cx, cy) + 2;
        double[][] field = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                field[x][y] = ((x + y) & 1) * phase + Math.hypot(x - cx, y - cy);
            }
        }
        return field;
    }

    /** Even columns fall from the top to the bottom while the odd columns fall from the bottom to the top. */
    public static double[][] weave(int width, int height) {
        double[][] field = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                field[x][y] = x % 2 == 0 ? y : height - 1 - y;
            }
        }
        return field;
    }

    /** Rings of a ripple in two beats: all the even rings, then all the odd rings. */
    public static double[][] pulse(int width, int height) {
        double phase = Math.hypot(width, height) / 2 + 2;
        return HeightMapUtils.fieldFromCenter(width, height, (dx, dy) -> {
            double r = Math.hypot(dx, dy);
            return ((int) Math.floor(r) % 2) * phase + r;
        });
    }

    /** The square spiral path of {@code v4}, but without the tall build. */
    public static double[][] spiral(int width, int height, boolean inward) {
        double[][] field = new double[width][height];
        int minRow = 0;
        int maxRow = width - 1;
        int minCol = 0;
        int maxCol = height - 1;
        int num = 0;

        while (minRow <= maxRow && minCol <= maxCol) {
            for (int j = minCol; j <= maxCol; j++) {
                field[minRow][j] = num++;
            }
            minRow++;

            for (int i = minRow; i <= maxRow; i++) {
                field[i][maxCol] = num++;
            }
            maxCol--;

            if (minRow <= maxRow) {
                for (int j = maxCol; j >= minCol; j--) {
                    field[maxRow][j] = num++;
                }
                maxRow--;
            }

            if (minCol <= maxCol) {
                for (int i = maxRow; i >= minRow; i--) {
                    field[i][minCol] = num++;
                }
                minCol++;
            }
        }

        if (!inward) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    field[x][y] = num - 1 - field[x][y];
                }
            }
        }
        return field;
    }

    /** Bands of random thickness that fall at random moments and sweep in a random direction (a glitching screen). */
    public static double[][] glitch(int width, int height) {
        Random random = new Random();
        double[][] field = new double[width][height];
        int y = 0;
        while (y < height) {
            int band = 1 + random.nextInt(5);
            double delay = random.nextDouble() * 3 * height;
            boolean fromLeft = random.nextBoolean();
            for (int yy = y; yy < Math.min(height, y + band); yy++) {
                for (int x = 0; x < width; x++) {
                    field[x][yy] = delay + (fromLeft ? x : width - 1 - x) * 0.6;
                }
            }
            y += band;
        }
        return field;
    }

    /** Seeds of a sunflower (golden angle) fall in order: spiral arms that wind outwards. */
    public static double[][] sunflower(int width, int height) {
        double c = Math.sqrt(6 / Math.PI);
        double radius = Math.hypot(width, height) / 2 + 3;
        int count = (int) Math.ceil((radius / c) * (radius / c)) + 1;
        double golden = Math.PI * (3 - Math.sqrt(5));
        double[] px = new double[count];
        double[] py = new double[count];
        for (int i = 0; i < count; i++) {
            double r = c * Math.sqrt(i);
            px[i] = r * Math.cos(i * golden);
            py[i] = r * Math.sin(i * golden);
        }

        return HeightMapUtils.fieldFromCenter(width, height, (dx, dy) -> {
            int guess = (int) ((dx * dx + dy * dy) / (c * c));
            int span = (int) (4 * Math.sqrt(Math.max(guess, 1))) + 12;
            int best = Math.max(0, Math.min(count - 1, guess));
            double bestDistance = Double.MAX_VALUE;
            for (int i = Math.max(0, guess - span); i <= Math.min(count - 1, guess + span); i++) {
                double d = (dx - px[i]) * (dx - px[i]) + (dy - py[i]) * (dy - py[i]);
                if (d < bestDistance) {
                    bestDistance = d;
                    best = i;
                }
            }
            return best;
        });
    }

    // ---------------------------------------------------------------- growth and fractals

    /** Growth like mould or moss: a random free neighbour of the grown area falls next. */
    public static double[][] mold(int width, int height) {
        Random random = new Random();
        double[][] field = new double[width][height];
        boolean[][] done = new boolean[width][height];
        List<int[]> frontier = new ArrayList<>();

        int sx = width / 2;
        int sy = height / 2;
        done[sx][sy] = true;
        addFreeNeighbours(frontier, done, sx, sy, width, height);

        int step = 1;
        while (!frontier.isEmpty()) {
            int index = random.nextInt(frontier.size());
            int[] cell = frontier.get(index);
            frontier.set(index, frontier.get(frontier.size() - 1));
            frontier.remove(frontier.size() - 1);
            if (done[cell[0]][cell[1]]) {
                continue;
            }
            done[cell[0]][cell[1]] = true;
            field[cell[0]][cell[1]] = step++;
            addFreeNeighbours(frontier, done, cell[0], cell[1], width, height);
        }
        return field;
    }

    /** One long winding snake (random depth first walk) that fills the whole image. */
    public static double[][] serpent(int width, int height) {
        Random random = new Random();
        double[][] field = new double[width][height];
        boolean[][] done = new boolean[width][height];
        Deque<int[]> stack = new ArrayDeque<>();

        int sx = width / 2;
        int sy = height / 2;
        done[sx][sy] = true;
        stack.push(new int[]{sx, sy});

        int step = 1;
        while (!stack.isEmpty()) {
            int[] cell = stack.peek();
            List<int[]> options = new ArrayList<>();
            for (int d = 0; d < 4; d++) {
                int nx = cell[0] + DX[d];
                int ny = cell[1] + DY[d];
                if (HeightMapUtils.isValid(nx, ny, width, height) && !done[nx][ny]) {
                    options.add(new int[]{nx, ny});
                }
            }
            if (options.isEmpty()) {
                stack.pop();
                continue;
            }
            int[] next = options.get(random.nextInt(options.size()));
            done[next[0]][next[1]] = true;
            field[next[0]][next[1]] = step++;
            stack.push(next);
        }
        return field;
    }

    /** Sierpinski triangle first (top left to bottom right), then the holes of the fractal level by level. */
    public static double[][] sierpinski(int width, int height) {
        double level = width + height + 1;
        double[][] field = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                field[x][y] = Integer.bitCount(x & ~y) * level + (x + y) * 0.5;
            }
        }
        return field;
    }

    /** Sierpinski carpet first (from the centre outwards), then the holes of the fractal level by level. */
    public static double[][] carpet(int width, int height) {
        double cx = (width - 1) / 2.0;
        double cy = (height - 1) / 2.0;
        double level = Math.hypot(width, height) + 1;
        double[][] field = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int a = x;
                int b = y;
                int holes = 0;
                while (a > 0 || b > 0) {
                    if (a % 3 == 1 && b % 3 == 1) {
                        holes++;
                    }
                    a /= 3;
                    b /= 3;
                }
                field[x][y] = holes * level + Math.hypot(x - cx, y - cy);
            }
        }
        return field;
    }

    /** Cellular automaton "rule 30" growing down from the top centre; its chaotic triangle falls first. */
    public static double[][] rule30(int width, int height) {
        double[][] field = new double[width][height];
        boolean[] row = new boolean[width];
        row[width / 2] = true;
        for (int y = 0; y < height; y++) {
            boolean[] next = new boolean[width];
            for (int x = 0; x < width; x++) {
                field[x][y] = row[x] ? y : y + height;
                boolean left = x > 0 && row[x - 1];
                boolean right = x < width - 1 && row[x + 1];
                next[x] = left ^ (row[x] || right);
            }
            row = next;
        }
        return field;
    }

    private static void addFreeNeighbours(List<int[]> frontier, boolean[][] done, int x, int y, int width, int height) {
        for (int d = 0; d < 4; d++) {
            int nx = x + DX[d];
            int ny = y + DY[d];
            if (HeightMapUtils.isValid(nx, ny, width, height) && !done[nx][ny]) {
                frontier.add(new int[]{nx, ny});
            }
        }
    }

    // ---------------------------------------------------------------- image aware

    private static double[][] neutral(int width, int height) {
        return HeightMapUtils.fieldFromCenter(width, height, Math::hypot);
    }

    private static double luminance(ImageColorEnum color) {
        Color c = color.getColor();
        return 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
    }

    private static double colorDistance(ImageColorEnum a, ImageColorEnum b) {
        Color c1 = a.getColor();
        Color c2 = b.getColor();
        int dr = c1.getRed() - c2.getRed();
        int dg = c1.getGreen() - c2.getGreen();
        int db = c1.getBlue() - c2.getBlue();
        return Math.sqrt(dr * dr + dg * dg + db * db) / 441.7;
    }

    /** The dark blocks of the picture fall first (or the light ones), the image flows from shadow to highlight. */
    public static double[][] brightness(int width, int height, ImageColorEnum[][] colors, boolean darkFirst) {
        if (colors == null) {
            return neutral(width, height);
        }
        double[][] field = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double lum = luminance(colors[x][y]);
                field[x][y] = darkFirst ? lum : 255 - lum;
            }
        }
        return field;
    }

    /** Colours fall in rainbow order; the grey blocks fall before all the colours, from dark to light. */
    public static double[][] hue(int width, int height, ImageColorEnum[][] colors) {
        if (colors == null) {
            return neutral(width, height);
        }
        double[][] field = new double[width][height];
        float[] hsb = new float[3];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color c = colors[x][y].getColor();
                Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsb);
                field[x][y] = hsb[1] < 0.12 ? hsb[2] - 1 : hsb[0];
            }
        }
        return field;
    }

    /** The edges of the objects in the picture fall first, the flat areas inside them follow like a sketch collapsing. */
    public static double[][] edges(int width, int height, ImageColorEnum[][] colors) {
        if (colors == null) {
            return neutral(width, height);
        }
        double[][] field = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double edge = 0;
                for (int d = 0; d < 4; d++) {
                    int nx = x + DX[d];
                    int ny = y + DY[d];
                    if (HeightMapUtils.isValid(nx, ny, width, height)) {
                        edge = Math.max(edge, colorDistance(colors[x][y], colors[nx][ny]));
                    }
                }
                field[x][y] = 1 - edge;
            }
        }
        return field;
    }

    /** Every area of one colour is an island: the biggest island falls first, every island as a ripple from its corner. */
    public static double[][] islands(int width, int height, ImageColorEnum[][] colors) {
        if (colors == null) {
            return neutral(width, height);
        }
        int[][] distance = new int[width][height];
        int[][] island = new int[width][height];
        for (int[] column : island) {
            java.util.Arrays.fill(column, -1);
        }
        List<Integer> sizes = new ArrayList<>();
        List<Integer> reach = new ArrayList<>();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (island[x][y] != -1) {
                    continue;
                }
                int id = sizes.size();
                int count = 0;
                int far = 0;
                Deque<int[]> queue = new ArrayDeque<>();
                island[x][y] = id;
                queue.add(new int[]{x, y});
                while (!queue.isEmpty()) {
                    int[] cell = queue.poll();
                    count++;
                    far = Math.max(far, distance[cell[0]][cell[1]]);
                    for (int d = 0; d < 4; d++) {
                        int nx = cell[0] + DX[d];
                        int ny = cell[1] + DY[d];
                        if (HeightMapUtils.isValid(nx, ny, width, height) && island[nx][ny] == -1
                                && colors[nx][ny] == colors[x][y]) {
                            island[nx][ny] = id;
                            distance[nx][ny] = distance[cell[0]][cell[1]] + 1;
                            queue.add(new int[]{nx, ny});
                        }
                    }
                }
                sizes.add(count);
                reach.add(far);
            }
        }

        // rank of every island by size, biggest first
        Integer[] byRank = new Integer[sizes.size()];
        for (int i = 0; i < byRank.length; i++) {
            byRank[i] = i;
        }
        java.util.Arrays.sort(byRank, (a, b) -> Integer.compare(sizes.get(b), sizes.get(a)));
        int[] rank = new int[byRank.length];
        for (int i = 0; i < byRank.length; i++) {
            rank[byRank[i]] = i;
        }

        double[][] field = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int id = island[x][y];
                field[x][y] = rank[id] + distance[x][y] / (reach.get(id) + 1.0);
            }
        }
        return field;
    }
}
