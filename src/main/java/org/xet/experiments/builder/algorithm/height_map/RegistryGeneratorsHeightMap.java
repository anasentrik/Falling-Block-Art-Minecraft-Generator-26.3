package org.xet.experiments.builder.algorithm.height_map;

import java.util.ArrayList;
import java.util.List;

public class RegistryGeneratorsHeightMap {
    private static final List<GeneratorHeightMap> generators = new ArrayList<>();

    static GeneratorHeightMap V1 = registerGenerator(new GeneratorV1());
    static GeneratorHeightMap V2 = registerGenerator(new GeneratorV2());
    static GeneratorHeightMap V3 = registerGenerator(new GeneratorV3());
    static GeneratorHeightMap V4 = registerGenerator(new GeneratorV4());
    static GeneratorHeightMap V5 = registerGenerator(new GeneratorGame());
    static GeneratorHeightMap V6 = registerGenerator(new WaveNoiseGenerator());
    // same kind as wave_noise, fast; the old one stays as the reference
    static GeneratorHeightMap WAVE_NOISE_FAST = registerGenerator(new WaveNoiseFastGenerator("wave_noise_fast", 0.15));
    static GeneratorHeightMap WAVE_NOISE_BIG = registerGenerator(new WaveNoiseFastGenerator("wave_noise_big", 0.07));
    static GeneratorHeightMap WAVE_NOISE_FINE = registerGenerator(new WaveNoiseFastGenerator("wave_noise_fine", 0.30));

    // waves from the centre / edges
    static GeneratorHeightMap RIPPLE = registerGenerator(new RippleGenerator("ripple", RippleGenerator.Shape.CIRCLE, false));
    static GeneratorHeightMap RIPPLE_IN = registerGenerator(new RippleGenerator("ripple_in", RippleGenerator.Shape.CIRCLE, true));
    static GeneratorHeightMap SQUARE = registerGenerator(new RippleGenerator("square", RippleGenerator.Shape.SQUARE, false));
    static GeneratorHeightMap FRAME = registerGenerator(new RippleGenerator("frame", RippleGenerator.Shape.SQUARE, true));
    static GeneratorHeightMap X_CROSS = registerGenerator(new RippleGenerator("x_cross", RippleGenerator.Shape.X_CROSS, false));
    static GeneratorHeightMap PLUS = registerGenerator(new RippleGenerator("plus", RippleGenerator.Shape.PLUS, false));

    // several waves at once
    static GeneratorHeightMap RAINDROPS = registerGenerator(new MultiSeedGenerator("raindrops", MultiSeedGenerator.Mode.RAINDROPS));
    static GeneratorHeightMap CORNERS = registerGenerator(new MultiSeedGenerator("corners", MultiSeedGenerator.Mode.CORNERS));
    static GeneratorHeightMap TILES = registerGenerator(new MultiSeedGenerator("tiles", MultiSeedGenerator.Mode.TILES));

    // rotating waves
    static GeneratorHeightMap GALAXY = registerGenerator(new SpiralGenerator("galaxy", false, 3, 12, -0.06));
    static GeneratorHeightMap VORTEX = registerGenerator(new SpiralGenerator("vortex", true, 1, 10, 0.05));

    // shaped waves
    static GeneratorHeightMap HEART = registerGenerator(new ShapeWaveGenerator("heart", ShapeWaveGenerator.heart()));
    static GeneratorHeightMap STAR = registerGenerator(new ShapeWaveGenerator("star", ShapeWaveGenerator.star(5, 0.45)));
    static GeneratorHeightMap HEXAGON = registerGenerator(new ShapeWaveGenerator("hexagon", ShapeWaveGenerator.star(3, 1)));
    static GeneratorHeightMap FLOWER = registerGenerator(new ShapeWaveGenerator("flower", ShapeWaveGenerator.flower(6)));

    // sweeps
    static GeneratorHeightMap SINE = registerGenerator(new SweepGenerator("sine", false));
    static GeneratorHeightMap CURTAIN = registerGenerator(new SweepGenerator("curtain", true));

    // branching / paths
    static GeneratorHeightMap LIGHTNING = registerGenerator(new LightningGenerator());
    static GeneratorHeightMap MAZE = registerGenerator(new MazeGenerator("maze", false));
    static GeneratorHeightMap DISSOLVE = registerGenerator(new MazeGenerator("dissolve", true));
    static GeneratorHeightMap HILBERT = registerGenerator(new HilbertGenerator());

    // image-aware
    static GeneratorHeightMap OUTLINE = registerGenerator(new OutlineGenerator());
    static GeneratorHeightMap DARK_FIRST = registerGenerator(new LambdaPattern("dark_first", true, (w, h, c) -> Patterns.brightness(w, h, c, true)));
    static GeneratorHeightMap LIGHT_FIRST = registerGenerator(new LambdaPattern("light_first", true, (w, h, c) -> Patterns.brightness(w, h, c, false)));
    static GeneratorHeightMap HUE = registerGenerator(new LambdaPattern("hue", true, Patterns::hue));
    static GeneratorHeightMap EDGES = registerGenerator(new LambdaPattern("edges", true, Patterns::edges));
    static GeneratorHeightMap ISLANDS = registerGenerator(new LambdaPattern("islands", true, Patterns::islands));

    // more shapes
    static GeneratorHeightMap TRIANGLE = registerGenerator(new ShapeWaveGenerator("triangle", ShapeWaveGenerator.regular(3)));
    static GeneratorHeightMap PENTAGON = registerGenerator(new ShapeWaveGenerator("pentagon", ShapeWaveGenerator.regular(5)));
    static GeneratorHeightMap OCTAGON = registerGenerator(new ShapeWaveGenerator("octagon", ShapeWaveGenerator.regular(8)));
    static GeneratorHeightMap GEAR = registerGenerator(new ShapeWaveGenerator("gear", ShapeWaveGenerator.gear(12)));
    static GeneratorHeightMap CROSS = registerGenerator(new ShapeWaveGenerator("cross", ShapeWaveGenerator.cross()));
    static GeneratorHeightMap BUTTERFLY = registerGenerator(new ShapeWaveGenerator("butterfly", ShapeWaveGenerator.butterfly()));
    static GeneratorHeightMap CLOVER = registerGenerator(new ShapeWaveGenerator("clover", ShapeWaveGenerator.rose(4, 0.2)));
    static GeneratorHeightMap DAISY = registerGenerator(new ShapeWaveGenerator("daisy", ShapeWaveGenerator.rose(12, 0.55)));

    // more waves
    static GeneratorHeightMap RADAR = registerGenerator(new LambdaPattern("radar", false, (w, h, c) -> Patterns.radar(w, h, 1)));
    static GeneratorHeightMap PINWHEEL = registerGenerator(new LambdaPattern("pinwheel", false, (w, h, c) -> Patterns.radar(w, h, 4)));
    static GeneratorHeightMap ELLIPSE = registerGenerator(new LambdaPattern("ellipse", false, (w, h, c) -> Patterns.focus(w, h, false)));
    static GeneratorHeightMap HYPERBOLA = registerGenerator(new LambdaPattern("hyperbola", false, (w, h, c) -> Patterns.focus(w, h, true)));
    static GeneratorHeightMap CONTOUR = registerGenerator(new LambdaPattern("contour", false, (w, h, c) -> Patterns.contour(w, h)));
    static GeneratorHeightMap VORONOI = registerGenerator(new LambdaPattern("voronoi", false, (w, h, c) -> Patterns.voronoi(w, h)));
    static GeneratorHeightMap STORM = registerGenerator(new LambdaPattern("storm", false, (w, h, c) -> Patterns.storm(w, h)));
    static GeneratorHeightMap CROSSFIRE = registerGenerator(new LambdaPattern("crossfire", false, (w, h, c) -> Patterns.crossfire(w, h)));
    static GeneratorHeightMap OCEAN = registerGenerator(new LambdaPattern("ocean", false, (w, h, c) -> Patterns.ocean(w, h)));
    static GeneratorHeightMap CHEVRON = registerGenerator(new LambdaPattern("chevron", false, (w, h, c) -> Patterns.chevron(w, h)));
    static GeneratorHeightMap SUNFLOWER = registerGenerator(new LambdaPattern("sunflower", true, (w, h, c) -> Patterns.sunflower(w, h)));

    // rows, columns, rings, paths
    static GeneratorHeightMap BLINDS = registerGenerator(new LambdaPattern("blinds", true, (w, h, c) -> Patterns.blinds(w, h)));
    static GeneratorHeightMap CHECKER = registerGenerator(new LambdaPattern("checker", true, (w, h, c) -> Patterns.checker(w, h)));
    static GeneratorHeightMap WEAVE = registerGenerator(new LambdaPattern("weave", true, (w, h, c) -> Patterns.weave(w, h)));
    static GeneratorHeightMap PULSE = registerGenerator(new LambdaPattern("pulse", true, (w, h, c) -> Patterns.pulse(w, h)));
    static GeneratorHeightMap SPIRAL = registerGenerator(new LambdaPattern("spiral", true, (w, h, c) -> Patterns.spiral(w, h, true)));
    static GeneratorHeightMap SPIRAL_OUT = registerGenerator(new LambdaPattern("spiral_out", true, (w, h, c) -> Patterns.spiral(w, h, false)));
    static GeneratorHeightMap GLITCH = registerGenerator(new LambdaPattern("glitch", true, (w, h, c) -> Patterns.glitch(w, h)));

    // noise and interference
    static GeneratorHeightMap PLASMA = registerGenerator(new LambdaPattern("plasma", true, (w, h, c) -> Patterns.plasma(w, h)));
    static GeneratorHeightMap MOIRE = registerGenerator(new LambdaPattern("moire", true, (w, h, c) -> Patterns.moire(w, h)));
    static GeneratorHeightMap INKDROP = registerGenerator(new LambdaPattern("inkdrop", false, (w, h, c) -> Patterns.inkdrop(w, h)));
    static GeneratorHeightMap KALEIDOSCOPE = registerGenerator(new LambdaPattern("kaleidoscope", false, (w, h, c) -> Patterns.kaleidoscope(w, h)));
    static GeneratorHeightMap MATRIX = registerGenerator(new LambdaPattern("matrix", true, (w, h, c) -> Patterns.matrix(w, h)));

    // growth and fractals
    static GeneratorHeightMap MOLD = registerGenerator(new LambdaPattern("mold", true, (w, h, c) -> Patterns.mold(w, h)));
    static GeneratorHeightMap SERPENT = registerGenerator(new LambdaPattern("serpent", true, (w, h, c) -> Patterns.serpent(w, h)));
    static GeneratorHeightMap SIERPINSKI = registerGenerator(new LambdaPattern("sierpinski", true, (w, h, c) -> Patterns.sierpinski(w, h)));
    static GeneratorHeightMap CARPET = registerGenerator(new LambdaPattern("carpet", true, (w, h, c) -> Patterns.carpet(w, h)));
    static GeneratorHeightMap RULE30 = registerGenerator(new LambdaPattern("rule30", true, (w, h, c) -> Patterns.rule30(w, h)));

    public static GeneratorHeightMap getGeneratorByName(String name) {
        for (GeneratorHeightMap gen : generators) {
            if (gen.getName().equals(name)) {
                return gen;
            }
        }

        throw new IllegalArgumentException("Unknown generator name: " + name);
    }

    public static String[] getAvailableNames() {
        List<String> names = new ArrayList<>();
        for (GeneratorHeightMap gen : generators) {
            names.add(gen.getName());
        }
        return names.toArray(new String[0]);
    }

    private static <T extends GeneratorHeightMap> T registerGenerator(T entry) {
        RegistryGeneratorsHeightMap.generators.add(entry);
        return entry;
    }
}
