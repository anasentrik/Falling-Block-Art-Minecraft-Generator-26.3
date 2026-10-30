package org.xet.experiments.builder.algorithm.height_map;

import org.xet.experiments.builder.algorithm.color.ImageColorEnum;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * maze: the walls of a random maze collapse first (as a ripple from the centre), so the maze stays standing,
 * then it is eaten away along its corridors (one path at a time, backtracking at dead ends).
 * dissolve: every pixel falls at a random moment, like the image dissolving.
 */
public class MazeGenerator extends PatternGenerator {
    private final boolean dissolve;

    public MazeGenerator(String name, boolean dissolve) {
        // only the order matters here, so keep the build low
        super(name, true);
        this.dissolve = dissolve;
    }

    @Override
    protected double[][] fallTime(int width, int height, ImageColorEnum[][] colors) {
        Random random = new Random();
        double[][] field = new double[width][height];

        if (dissolve) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    field[x][y] = random.nextDouble();
                }
            }
            return field;
        }

        // maze cells live on even coordinates, the pixels between them are walls or passages
        int cellsX = (width + 1) / 2;
        int cellsY = (height + 1) / 2;
        boolean[][] visited = new boolean[cellsX][cellsY];
        for (double[] column : field) {
            Arrays.fill(column, -1);
        }

        int step = 0;
        Deque<int[]> stack = new ArrayDeque<>();
        int startX = cellsX / 2;
        int startY = cellsY / 2;
        visited[startX][startY] = true;
        field[startX * 2][startY * 2] = step++;
        stack.push(new int[]{startX, startY});

        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!stack.isEmpty()) {
            int[] cell = stack.peek();
            List<int[]> options = new ArrayList<>();
            for (int[] dir : directions) {
                int nx = cell[0] + dir[0];
                int ny = cell[1] + dir[1];
                if (nx >= 0 && ny >= 0 && nx < cellsX && ny < cellsY && !visited[nx][ny]) {
                    options.add(new int[]{nx, ny});
                }
            }
            if (options.isEmpty()) {
                stack.pop();
                continue;
            }
            Collections.shuffle(options, random);
            int[] next = options.get(0);
            visited[next[0]][next[1]] = true;
            // passage pixel first, then the cell behind it
            field[cell[0] + next[0]][cell[1] + next[1]] = step++;
            field[next[0] * 2][next[1] * 2] = step++;
            stack.push(next);
        }

        // walls (and the odd last row/column) fall first, closest to the centre first; the corridors come after them
        // so every corridor pixel can hang on a wall next to it, which keeps the build low
        double cx = (width - 1) / 2.0;
        double cy = (height - 1) / 2.0;
        double wallsEnd = Math.hypot(cx, cy) + 1;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                field[x][y] = field[x][y] < 0 ? Math.hypot(x - cx, y - cy) : wallsEnd + field[x][y];
            }
        }

        return field;
    }
}
