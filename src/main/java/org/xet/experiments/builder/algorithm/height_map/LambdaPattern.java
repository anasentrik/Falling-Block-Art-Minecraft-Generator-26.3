package org.xet.experiments.builder.algorithm.height_map;

import org.xet.experiments.builder.algorithm.color.ImageColorEnum;

/** A pattern whose fall times are computed by a function, see {@link Patterns}. */
public class LambdaPattern extends PatternGenerator {
    @FunctionalInterface
    public interface FallTimeFunction {
        double[][] apply(int width, int height, ImageColorEnum[][] colors);
    }

    private final FallTimeFunction function;

    public LambdaPattern(String name, boolean lowBuild, FallTimeFunction function) {
        super(name, lowBuild);
        this.function = function;
    }

    @Override
    protected double[][] fallTime(int width, int height, ImageColorEnum[][] colors) {
        return function.apply(width, height, colors);
    }
}
