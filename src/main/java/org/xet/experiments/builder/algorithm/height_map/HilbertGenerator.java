package org.xet.experiments.builder.algorithm.height_map;

import org.xet.experiments.builder.algorithm.color.ImageColorEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * One single line that winds through the whole image along a Hilbert curve (generalised to any rectangle).
 * Only the falling order follows the curve, the build itself stays low, so it works for big images too.
 */
public class HilbertGenerator extends PatternGenerator {
    public HilbertGenerator() {
        // the path is only the falling order, the build itself stays low
        super("hilbert", true);
    }

    @Override
    protected double[][] fallTime(int width, int height, ImageColorEnum[][] colors) {
        List<int[]> path = new ArrayList<>();
        if (width >= height) {
            gilbert(path, 0, 0, width, 0, 0, height);
        } else {
            gilbert(path, 0, 0, 0, height, width, 0);
        }

        double[][] field = new double[width][height];
        for (int i = 0; i < path.size(); i++) {
            int[] point = path.get(i);
            field[point[0]][point[1]] = i;
        }

        return field;
    }

    // "gilbert2d" by Jakub Červený: Hilbert-like curve for rectangles of any size
    private static void gilbert(List<int[]> path, int x, int y, int ax, int ay, int bx, int by) {
        int w = Math.abs(ax + ay);
        int h = Math.abs(bx + by);
        int dax = Integer.signum(ax), day = Integer.signum(ay);
        int dbx = Integer.signum(bx), dby = Integer.signum(by);

        if (h == 1) {
            for (int i = 0; i < w; i++) {
                path.add(new int[]{x, y});
                x += dax;
                y += day;
            }
            return;
        }
        if (w == 1) {
            for (int i = 0; i < h; i++) {
                path.add(new int[]{x, y});
                x += dbx;
                y += dby;
            }
            return;
        }

        int ax2 = Math.floorDiv(ax, 2), ay2 = Math.floorDiv(ay, 2);
        int bx2 = Math.floorDiv(bx, 2), by2 = Math.floorDiv(by, 2);
        int w2 = Math.abs(ax2 + ay2);
        int h2 = Math.abs(bx2 + by2);

        if (2 * w > 3 * h) {
            if (w2 % 2 != 0 && w > 2) {
                ax2 += dax;
                ay2 += day;
            }
            gilbert(path, x, y, ax2, ay2, bx, by);
            gilbert(path, x + ax2, y + ay2, ax - ax2, ay - ay2, bx, by);
        } else {
            if (h2 % 2 != 0 && h > 2) {
                bx2 += dbx;
                by2 += dby;
            }
            gilbert(path, x, y, bx2, by2, ax2, ay2);
            gilbert(path, x + bx2, y + by2, ax, ay, bx - bx2, by - by2);
            gilbert(path, x + (ax - dax) + (bx2 - dbx), y + (ay - day) + (by2 - dby), -bx2, -by2, -(ax - ax2), -(ay - ay2));
        }
    }

}
