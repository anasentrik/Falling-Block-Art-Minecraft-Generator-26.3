package org.xet.experiments.builder.algorithm.height_map;

import org.xet.experiments.builder.algorithm.color.ImageColorEnum;

public abstract class GeneratorHeightMap {
    public abstract int[][] getHeightMap(int width, int height);

    /**
     * Heights plus the order in which the pixels fall. By default the blocks fall from the lowest to the highest.
     * Image-aware generators use the block chosen for every pixel ({@code colors}).
     */
    public FallPlan generate(int width, int height, ImageColorEnum[][] colors) {
        return FallPlan.byHeight(getHeightMap(width, height));
    }

    public abstract String getName();
}
