package org.xet.experiments.builder.algorithm.height_map;

import org.xet.experiments.builder.algorithm.color.ImageColorEnum;

/**
 * Generator that only describes when every pixel falls; the heights are derived from that by
 * {@link HeightMapUtils#fromFallTime(double[][], boolean)}.
 */
public abstract class PatternGenerator extends GeneratorHeightMap {
    private final String name;
    private final boolean lowBuild;

    /** @param lowBuild true keeps the build as low as possible instead of shaping it after the pattern */
    protected PatternGenerator(String name, boolean lowBuild) {
        this.name = name;
        this.lowBuild = lowBuild;
    }

    /** Fall time of every pixel, lower = earlier. Only the order matters, not the scale. */
    protected abstract double[][] fallTime(int width, int height, ImageColorEnum[][] colors);

    @Override
    public FallPlan generate(int width, int height, ImageColorEnum[][] colors) {
        return HeightMapUtils.fromFallTime(fallTime(width, height, colors), lowBuild);
    }

    @Override
    public int[][] getHeightMap(int width, int height) {
        return generate(width, height, null).heightMap();
    }

    @Override
    public String getName() {
        return name;
    }
}
