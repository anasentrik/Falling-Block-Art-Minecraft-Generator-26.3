package org.xet.experiments.builder.algorithm.height_map;

import org.xet.experiments.builder.algorithm.color.ImageColorEnum;

/**
 * A single wave that starts in the centre and spreads outwards (or starts at the edges and closes in).
 * ripple / ripple_in: round wave, square / frame: square rings, x_cross: diagonals first, plus: centre lines first.
 */
public class RippleGenerator extends PatternGenerator {
    public enum Shape { CIRCLE, SQUARE, X_CROSS, PLUS }

    private final Shape shape;
    private final boolean inward;

    public RippleGenerator(String name, Shape shape, boolean inward) {
        super(name, false);
        this.shape = shape;
        this.inward = inward;
    }

    @Override
    protected double[][] fallTime(int width, int height, ImageColorEnum[][] colors) {
        double[][] field = HeightMapUtils.fieldFromCenter(width, height, (dx, dy) -> switch (shape) {
            case CIRCLE -> Math.hypot(dx, dy);
            case SQUARE -> Math.max(Math.abs(dx), Math.abs(dy));
            case X_CROSS -> Math.min(Math.abs(dx - dy), Math.abs(dx + dy));
            case PLUS -> Math.min(Math.abs(dx), Math.abs(dy));
        });

        if (inward) {
            double max = 0;
            for (double[] column : field) {
                for (double value : column) {
                    max = Math.max(max, value);
                }
            }
            for (double[] column : field) {
                for (int y = 0; y < column.length; y++) {
                    column[y] = max - column[y];
                }
            }
        }

        return field;
    }
}
