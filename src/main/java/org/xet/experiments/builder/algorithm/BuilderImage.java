package org.xet.experiments.builder.algorithm;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xet.experiments.builder.algorithm.color.ColorMap;
import org.xet.experiments.builder.algorithm.color.ImageColorEnum;
import org.xet.experiments.builder.algorithm.height_map.FallPlan;
import org.xet.experiments.builder.algorithm.height_map.GeneratorHeightMap;
import org.xet.experiments.builder.algorithm.height_map.HeightMapUtils;
import org.xet.experiments.builder.algorithm.height_map.RegistryGeneratorsHeightMap;
import org.xet.experiments.builder.data.ColorHeightMaps;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;

public class BuilderImage {
    private static final Logger LOGGER = LogManager.getLogger(BuilderImage.class);
    private static final int GENERATION_TIMEOUT_SECONDS = 30;
    private static final int MAX_ATTEMPTS = 5;
    private static final long RETRY_BUDGET_NANOS = 8_000_000_000L;

    private ColorHeightMaps fallingBlocks;
    private final String fileName;
    private final int width;
    private final int height;
    private final String generatorName;
    private final Effects effects;

    private static final List<BlockPlacementTask> activeTasks = new ArrayList<>();
    private static boolean tickListenerRegistered = false;

    // --- COLLAPSE: stores the block positions of each player's last build ---
    private static final Map<UUID, LastBuild> lastBuilds = new ConcurrentHashMap<>();
    private static final List<CollapseTask> activeCollapseTasks = new ArrayList<>();

    public BuilderImage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        this.fileName = context.getArgument("file", String.class);
        this.width = context.getArgument("width", Integer.class);
        this.height = context.getArgument("height", Integer.class);
        this.generatorName = context.getArgument("typeHeightMap", String.class);
        try {
            this.effects = Effects.parse(readEffectsArgument(context));
        } catch (IllegalArgumentException e) {
            throw new SimpleCommandExceptionType(Component.literal(e.getMessage())).create();
        }

        LOGGER.info("Creating BuilderImage: file={}, size={}x{}, generator={}, effects={}", fileName, width, height, generatorName, effects);
    }

    /** The optional "effects" argument of the command, null if it was not given. */
    public static String readEffectsArgument(CommandContext<CommandSourceStack> context) {
        try {
            return context.getArgument("effects", String.class);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public CompletableFuture<Void> buildAsync(Vec3 pos, ServerLevel serverLevel, CommandSourceStack source) {
        LOGGER.info("Starting asynchronous image generation...");
        // the top block of a column sits at pos.y + height + 1
        int maxAllowedHeight = Math.min(HeightMapUtils.MAX_BUILD_Y, serverLevel.getMaxY()) - (int) Math.floor(pos.y) - 1;

        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("Generation started...");
                // random generators differ in height from try to try, so a build that is too tall gets a few new tries
                long start = System.nanoTime();
                ColorHeightMaps result = getImageFallingBlocks(fileName, width, height, generatorName);
                int maxHeight = HeightMapUtils.maxHeight(result.heightMap());
                for (int attempt = 2; attempt <= MAX_ATTEMPTS && maxHeight > maxAllowedHeight
                        && System.nanoTime() - start < RETRY_BUDGET_NANOS; attempt++) {
                    LOGGER.info("Build of {} blocks is too tall, trying again (attempt {})", maxHeight + 1, attempt);
                    result = getImageFallingBlocks(fileName, width, height, generatorName);
                    maxHeight = HeightMapUtils.maxHeight(result.heightMap());
                }
                if (maxHeight > maxAllowedHeight) {
                    throw new IllegalStateException("'" + generatorName + "' needs " + (maxHeight + 1)
                            + " blocks of height but only " + (maxAllowedHeight + 1)
                            + " fit here (max Y " + Math.min(HeightMapUtils.MAX_BUILD_Y, serverLevel.getMaxY())
                            + "). Use a smaller image, stand lower or pick another generator.");
                }
                LOGGER.info("Generation finished, max height {}", maxHeight);
                return result;
            } catch (CommandSyntaxException e) {
                LOGGER.error("Error during generation: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        }, Executors.newSingleThreadExecutor())
        .orTimeout(GENERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // world changes must happen on the server thread
        .thenAcceptAsync(result -> {
            this.fallingBlocks = result;
            source.sendSystemMessage(Component.literal("§aGeneration finished! Placing blocks..."));
            startGradualBlockPlacement(pos, serverLevel, source);
        }, source.getServer())
        .exceptionally(throwable -> {
            Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
                    ? throwable.getCause() : throwable;
            if (cause instanceof TimeoutException) {
                LOGGER.error("Generation exceeded the timeout of {} seconds", GENERATION_TIMEOUT_SECONDS);
                source.sendSystemMessage(Component.literal("§cError: generation exceeded the timeout of " + GENERATION_TIMEOUT_SECONDS + " seconds"));
            } else {
                LOGGER.error("Error during generation", throwable);
                source.sendSystemMessage(Component.literal("§cError during generation: " + cause.getMessage()));
            }
            return null;
        });
    }

    private void startGradualBlockPlacement(Vec3 pos, ServerLevel serverLevel, CommandSourceStack source) {
        if (!tickListenerRegistered) {
            registerTickListener();
        }

        List<BlockPlacement> blocks = prepareBlockPlacements(pos, fallingBlocks);

        BlockPlacementTask task = new BlockPlacementTask(blocks, serverLevel, source, effects.rate());
        activeTasks.add(task);

        // --- COLLAPSE: remember the positions of this build so it can be collapsed later,
        // in the order the generator wants them to fall (a block never comes before the one it hangs on) ---
        if (source.getPlayer() != null) {
            List<BlockPlacement> fallOrder = new ArrayList<>(blocks);
            fallOrder.sort(Comparator.<BlockPlacement>comparingDouble(bp -> bp.fallOrder)
                    .thenComparingInt(bp -> bp.pos.getY()));
            List<BlockPos> positions = new ArrayList<>();
            for (BlockPlacement bp : fallOrder) {
                positions.add(bp.pos);
            }
            Vec3 center = new Vec3(Math.floor(pos.x) + fallingBlocks.width() / 2.0, pos.y, Math.floor(pos.z) + fallingBlocks.height() / 2.0);
            lastBuilds.put(source.getPlayer().getUUID(), new LastBuild(positions, center, effects));
        }

        source.sendSystemMessage(Component.literal("§ePlacing " + blocks.size() + " blocks"
                + (effects == Effects.NONE ? "..." : " (effects: " + effects + ")...")));
    }

    /**
     * Called from the "/image collapse" command.
     * Makes the blocks of this player's last build fall (falling block)
     * if there is no block directly below them.
     */
    public static void collapse(CommandSourceStack source, Effects override) throws CommandSyntaxException {
        if (source.getPlayer() == null) {
            throw new SimpleCommandExceptionType(Component.literal("This command can only be used by a player.")).create();
        }

        UUID uuid = source.getPlayer().getUUID();
        LastBuild build = lastBuilds.get(uuid);
        List<BlockPos> positions = build == null ? null : build.positions();

        if (positions == null || positions.isEmpty()) {
            throw new SimpleCommandExceptionType(Component.literal("There is no previous /image build to collapse.")).create();
        }

        if (!tickListenerRegistered) {
            registerTickListener();
        }

        ServerLevel serverLevel = source.getPlayer().level();
        // an effect given to /image collapse replaces the motion and rhythm chosen when the image was built
        Effects effects = override == null ? build.effects() : build.effects().withCollapseOf(override);
        CollapseTask task = new CollapseTask(positions, serverLevel, source, build.center(), effects);
        activeCollapseTasks.add(task);

        source.sendSystemMessage(Component.literal("§eStarting collapse of " + positions.size() + " blocks"
                + (effects == Effects.NONE ? "..." : " (effects: " + effects + ")...")));
    }

    private static void registerTickListener() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            activeTasks.removeIf(task -> {
                if (task.isCompleted()) {
                    return true;
                }

                task.processNextBatch();
                return false;
            });

            activeCollapseTasks.removeIf(task -> {
                if (task.isCompleted()) {
                    return true;
                }

                task.processNextBatch();
                return false;
            });
        });
        tickListenerRegistered = true;
    }

    private List<BlockPlacement> prepareBlockPlacements(Vec3 pos, ColorHeightMaps image) {
        List<BlockPlacement> blocks = new ArrayList<>();

        for (int i = 0; i < image.width(); i++) {
            for (int j = 0; j < image.height(); j++) {
                int x = (int) pos.x + i;
                int y = (int) pos.y + image.heightMap()[i][j];
                int z = (int) pos.z + j;
                double order = image.fallOrder()[i][j];

                if (image.heightMap()[i][j] == 0) {
                    blocks.add(new BlockPlacement(
                        new BlockPos(x, y, z),
                        Blocks.TORCH.defaultBlockState(),
                        order
                    ));
                }

                y += 1;
                blocks.add(new BlockPlacement(
                    new BlockPos(x, y, z),
                    getBlockWithColor(image.colorMap()[i][j]),
                    order
                ));

                addWallTorchIfNeeded(blocks, image, i, j, x, y, z, order);
            }
        }

        if (effects.assemble()) {
            // pattern order: a block always comes after the block it hangs on, so nothing is placed in mid air
            blocks.sort(Comparator.<BlockPlacement>comparingDouble(a -> a.fallOrder).thenComparingInt(a -> a.pos.getY()));
        } else {
            blocks.sort(Comparator.comparingInt(a -> a.pos.getY()));
        }

        return blocks;
    }

    private void addWallTorchIfNeeded(List<BlockPlacement> blocks, ColorHeightMaps image, int i, int j, int x, int y, int z, double order) {
        if (i + 1 < image.width() && image.heightMap()[i + 1][j] - 1 == image.heightMap()[i][j]) {
            BlockState state = Blocks.WALL_TORCH.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
            blocks.add(new BlockPlacement(new BlockPos(x + 1, y, z), state, order));
        }
        if (i - 1 >= 0 && image.heightMap()[i - 1][j] - 1 == image.heightMap()[i][j]) {
            BlockState state = Blocks.WALL_TORCH.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
            blocks.add(new BlockPlacement(new BlockPos(x - 1, y, z), state, order));
        }
        if (j + 1 < image.height() && image.heightMap()[i][j + 1] - 1 == image.heightMap()[i][j]) {
            BlockState state = Blocks.WALL_TORCH.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
            blocks.add(new BlockPlacement(new BlockPos(x, y, z + 1), state, order));
        }
        if (j - 1 >= 0 && image.heightMap()[i][j - 1] - 1 == image.heightMap()[i][j]) {
            BlockState state = Blocks.WALL_TORCH.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
            blocks.add(new BlockPlacement(new BlockPos(x, y, z - 1), state, order));
        }
    }

    private static BlockState getBlockWithColor(ImageColorEnum color) {
        return color.getBlockState();
    }

    private ColorHeightMaps getImageFallingBlocks(String fileName, int width, int height, String generatorName) throws CommandSyntaxException {
        BufferedImage image;
        try {
            image = ImageGetter.getImage(fileName);
        } catch (IOException e) {
            LOGGER.error("Error loading image: {}", e.getMessage());
            Component textError = Component.literal(e.getMessage());
            throw new SimpleCommandExceptionType(textError).create();
        }

        ColorMap colorMap = new ColorMap(image);
        ImageColorEnum[][] colorStrings = colorMap.getColorMap(width, height);

        GeneratorHeightMap generator = RegistryGeneratorsHeightMap.getGeneratorByName(generatorName);
        FallPlan plan = generator.generate(width, height, colorStrings);
        if (effects.jitter()) {
            plan = plan.jittered(new Random(), 0.05);
        }

        return new ColorHeightMaps(width, height, colorStrings, plan.heightMap(), plan.fallOrder());
    }

    private static class BlockPlacement {
        final BlockPos pos;
        final BlockState state;
        final double fallOrder;

        BlockPlacement(BlockPos pos, BlockState state, double fallOrder) {
            this.pos = pos;
            this.state = state;
            this.fallOrder = fallOrder;
        }
    }

    private static class BlockPlacementTask {
        private final List<BlockPlacement> blocks;
        private final ServerLevel serverLevel;
        private final CommandSourceStack source;
        private int currentIndex = 0;
        private final int totalBlocks;
        private int lastReportedPercent = 0;
        private final Effects.Rate rate;
        private int tick = 0;

        BlockPlacementTask(List<BlockPlacement> blocks, ServerLevel serverLevel, CommandSourceStack source, Effects.Rate rate) {
            this.rate = rate;
            this.blocks = blocks;
            this.serverLevel = serverLevel;
            this.source = source;
            this.totalBlocks = blocks.size();
        }

        void processNextBatch() {
            int processed = 0;
            int limit = rate.blocksPerTick(tick++, currentIndex, totalBlocks);

            while (currentIndex < blocks.size() && processed < limit) {
                BlockPlacement placement = blocks.get(currentIndex);
                serverLevel.setBlock(placement.pos, placement.state, 3);
                currentIndex++;
                processed++;
            }

            int currentPercent = (currentIndex * 100) / totalBlocks;
            if (currentPercent >= lastReportedPercent + 25) {
                lastReportedPercent = currentPercent;
                source.sendSystemMessage(Component.literal("§ePlacement progress: " + currentPercent + "%"));
            }

            if (isCompleted()) {
                source.sendSystemMessage(Component.literal("§aBlock placement finished!"));
            }
        }

        boolean isCompleted() {
            return currentIndex >= blocks.size();
        }
    }

    /**
     * Task that drops the blocks of a build gradually (to avoid lag).
     * A block is only dropped (turned into a FallingBlockEntity) if the space below it is empty/air.
     */
    private static class CollapseTask {
        private final List<BlockPos> positions;
        private final ServerLevel serverLevel;
        private final CommandSourceStack source;
        private int currentIndex = 0;
        private final int totalBlocks;
        private int lastReportedPercent = 0;
        private final Effects effects;
        private final Vec3 center;
        private final Random random = new Random();
        // direction all blocks lean to for the "tilt" effect
        private final Vec3 tilt;
        private int tick = 0;

        CollapseTask(List<BlockPos> positions, ServerLevel serverLevel, CommandSourceStack source, Vec3 center, Effects effects) {
            this.center = center;
            this.effects = effects;
            double angle = random.nextInt(4) * Math.PI / 2;
            this.tilt = new Vec3(Math.cos(angle) * 0.14, 0, Math.sin(angle) * 0.14);
            this.positions = positions;
            this.serverLevel = serverLevel;
            this.source = source;
            this.totalBlocks = positions.size();
        }

        void processNextBatch() {
            int processed = 0;
            int limit = effects.rate().blocksPerTick(tick++, currentIndex, totalBlocks);

            while (currentIndex < positions.size() && processed < limit) {
                BlockPos pos = positions.get(currentIndex);
                // positions that are already gone (popped torches) don't count towards the limit
                if (dropIfUnsupported(pos)) {
                    processed++;
                }
                currentIndex++;
            }

            int currentPercent = (currentIndex * 100) / totalBlocks;
            if (currentPercent >= lastReportedPercent + 25) {
                lastReportedPercent = currentPercent;
                source.sendSystemMessage(Component.literal("§eCollapse progress: " + currentPercent + "%"));
            }

            if (isCompleted()) {
                source.sendSystemMessage(Component.literal("§aCollapse finished!"));
            }
        }

        boolean isCompleted() {
            return currentIndex >= positions.size();
        }

        /**
         * Removes the torch a column stands on, or, if the block at this position has nothing (air) below it,
         * turns it into a FallingBlockEntity so it falls naturally like sand.
         *
         * @return true if something was removed or dropped
         */
        private boolean dropIfUnsupported(BlockPos pos) {
            BlockState state = serverLevel.getBlockState(pos);

            if (state.isAir()) {
                return false;
            }
            if (state.is(Blocks.BEDROCK)) {
                return false;
            }
            // torch on the ground under a starting column: remove it so the column (and everything hanging on it) falls
            if (state.is(Blocks.TORCH)) {
                serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                return true;
            }

            BlockPos below = pos.below();
            if (!serverLevel.getBlockState(below).isAir()) {
                return false; // still supported, no need to fall
            }

            // FallingBlockEntity.fall replaces the block with air and spawns the falling entity
            FallingBlockEntity falling = FallingBlockEntity.fall(serverLevel, pos, state);
            if (effects.motion() != Effects.Motion.NONE) {
                // blocks that land badly after being pushed vanish instead of dropping thousands of items
                falling.disableDrop();
                falling.setDeltaMovement(startVelocity(pos));
                falling.needsSync = true;
            }
            return true;
        }

        private Vec3 startVelocity(BlockPos pos) {
            double dx = pos.getX() + 0.5 - center.x;
            double dz = pos.getZ() + 0.5 - center.z;
            double distance = Math.hypot(dx, dz);
            double ux;
            double uz;
            if (distance < 1e-3) {
                double angle = random.nextDouble() * 2 * Math.PI;
                ux = Math.cos(angle);
                uz = Math.sin(angle);
            } else {
                ux = dx / distance;
                uz = dz / distance;
            }

            return switch (effects.motion()) {
                case BURST -> {
                    double speed = 0.25 + random.nextDouble() * 0.35;
                    yield new Vec3(ux * speed, 0.35 + random.nextDouble() * 0.25, uz * speed);
                }
                case SWIRL -> new Vec3(-uz * 0.30 + ux * 0.08, 0.15 + random.nextDouble() * 0.10, ux * 0.30 + uz * 0.08);
                case SCATTER -> new Vec3((random.nextDouble() - 0.5) * 0.4, 0.05 + random.nextDouble() * 0.15, (random.nextDouble() - 0.5) * 0.4);
                case TILT -> new Vec3(tilt.x + (random.nextDouble() - 0.5) * 0.03, 0, tilt.z + (random.nextDouble() - 0.5) * 0.03);
                case NONE -> Vec3.ZERO;
            };
        }
    }

    /** What /image collapse needs to know about the last build of a player. */
    private record LastBuild(List<BlockPos> positions, Vec3 center, Effects effects) {
    }
}
