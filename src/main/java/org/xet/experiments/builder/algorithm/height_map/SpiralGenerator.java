package org.xet.experiments.builder.algorithm.height_map;

import org.xet.experiments.builder.algorithm.color.ImageColorEnum;

/**
 * Waves that rotate while they spread.
 * galaxy: round wave with curved spiral arms, vortex: square rings that twist a bit more with every ring.
 */
public class SpiralGenerator extends PatternGenerator {
    private final boolean square;
    private final int arms;
    private final double armDelay;
    private final double twist;

    public SpiralGenerator(String name, boolean square, int arms, double armDelay, double twist) {
        super(name, false);
        this.square = square;
        this.arms = arms;
        this.armDelay = armDelay;
        this.twist = twist;
    }

    @Override
    protected double[][] fallTime(int width, int height, ImageColorEnum[][] colors) {
        return HeightMapUtils.fieldFromCenter(width, height, (dx, dy) -> {
            double radius = square ? Math.max(Math.abs(dx), Math.abs(dy)) : Math.hypot(dx, dy);
            double angle = (Math.atan2(dy, dx) + Math.PI) / (2 * Math.PI);
            double phase = angle * arms + radius * twist;
            return radius + (phase - Math.floor(phase)) * armDelay;
        });
    }
}
