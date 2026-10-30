package org.xet.experiments.builder.algorithm;

import java.util.List;

/**
 * Extra options for /image that work with every generator. Several can be combined with '+', e.g. {@code burst+jitter}.
 *
 * @param jitter   random offset on the fall times, a tidy pattern becomes a bit ragged
 * @param assemble the image is built in the order of the pattern instead of from the bottom up
 * @param motion   how the blocks move when they start to fall
 * @param rate     how many blocks per tick are processed over time
 */
public record Effects(boolean jitter, boolean assemble, Motion motion, Rate rate) {
    public static final Effects NONE = new Effects(false, false, Motion.NONE, Rate.NORMAL);

    public static final List<String> NAMES = List.of(
            "jitter", "assemble", "burst", "swirl", "scatter", "tilt", "slow", "avalanche", "pulse");

    public enum Motion {
        NONE,
        /** thrown outwards from the centre, then fall in an arc */
        BURST,
        /** pushed around the centre, then fall in a spiral */
        SWIRL,
        /** a random push in every direction (dust) */
        SCATTER,
        /** all lean the same way (dominoes) */
        TILT
    }

    public enum Rate {
        /** 15 blocks per tick */
        NORMAL,
        /** 4 blocks per tick */
        SLOW,
        /** starts slowly and speeds up to 25 blocks per tick */
        AVALANCHE,
        /** swings between 3 and 25 blocks per tick */
        PULSE;

        public int blocksPerTick(int tick, int index, int total) {
            return switch (this) {
                case NORMAL -> 15;
                case SLOW -> 4;
                case AVALANCHE -> {
                    double progress = total == 0 ? 1 : index / (double) total;
                    yield 3 + (int) (22 * progress * progress);
                }
                case PULSE -> 3 + (int) (22 * (0.5 + 0.5 * Math.sin(tick * 2 * Math.PI / 40)));
            };
        }
    }

    /** @throws IllegalArgumentException with a readable message if a name is unknown */
    public static Effects parse(String text) {
        if (text == null || text.isBlank() || text.equalsIgnoreCase("none")) {
            return NONE;
        }

        boolean jitter = false;
        boolean assemble = false;
        Motion motion = Motion.NONE;
        Rate rate = Rate.NORMAL;
        for (String token : text.toLowerCase().split("\\+")) {
            switch (token) {
                case "none" -> {
                }
                case "jitter" -> jitter = true;
                case "assemble" -> assemble = true;
                case "burst" -> motion = Motion.BURST;
                case "swirl" -> motion = Motion.SWIRL;
                case "scatter" -> motion = Motion.SCATTER;
                case "tilt" -> motion = Motion.TILT;
                case "slow" -> rate = Rate.SLOW;
                case "avalanche" -> rate = Rate.AVALANCHE;
                case "pulse" -> rate = Rate.PULSE;
                default -> throw new IllegalArgumentException("Unknown effect '" + token + "'. Available: "
                        + String.join(", ", NAMES) + " (combine with +, e.g. burst+jitter)");
            }
        }
        return new Effects(jitter, assemble, motion, rate);
    }

    /** These effects, but with the collapse part (motion and rate) of another one. */
    public Effects withCollapseOf(Effects other) {
        return new Effects(jitter, assemble, other.motion, other.rate);
    }

    @Override
    public String toString() {
        List<String> names = new java.util.ArrayList<>();
        if (jitter) names.add("jitter");
        if (assemble) names.add("assemble");
        if (motion != Motion.NONE) names.add(motion.name().toLowerCase());
        if (rate != Rate.NORMAL) names.add(rate.name().toLowerCase());
        return names.isEmpty() ? "none" : String.join("+", names);
    }
}
