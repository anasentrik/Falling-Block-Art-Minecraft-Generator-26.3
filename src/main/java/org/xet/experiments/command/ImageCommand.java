package org.xet.experiments.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xet.experiments.builder.algorithm.BuilderImage;
import org.xet.experiments.builder.algorithm.Effects;
import org.xet.experiments.builder.algorithm.ImageGetter;
import org.xet.experiments.builder.algorithm.height_map.RegistryGeneratorsHeightMap;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;

public class ImageCommand {
    private static final Logger LOGGER = LogManager.getLogger(ImageCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection selection) {
        dispatcher.register(Commands.literal("image")
                .then(argument("width", IntegerArgumentType.integer())
                        .then(argument("height", IntegerArgumentType.integer())
                                .then(argument("typeHeightMap", StringArgumentType.word())
                                        .suggests(new AttributeTypeHeightSuggestionProvider())
                                        .then(argument("file", StringArgumentType.word())
                                                .suggests(new AttributeFileSuggestionProvider())
                                                .executes(ImageCommand::run)
                                                .then(argument("effects", StringArgumentType.word())
                                                        .suggests(new EffectSuggestionProvider())
                                                        .executes(ImageCommand::run)
                                                )
                                        )
                                )
                        )
                )
                .then(Commands.literal("collapse")
                        .executes(ImageCommand::collapse)
                        .then(argument("effects", StringArgumentType.word())
                                .suggests(new EffectSuggestionProvider())
                                .executes(ImageCommand::collapse)
                        )
                )
        );
    }

    private static int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        Vec3 pos = player.position();
        ServerLevel serverLevel = player.level();
        CommandSourceStack source = context.getSource();

        LOGGER.info("Player {} is running the image generation command at position {}",
                   player.getName().getString(), pos);

        try {
            BuilderImage builderImage = new BuilderImage(context);

            source.sendSystemMessage(Component.literal("§eStarting image generation..."));

            builderImage.buildAsync(pos, serverLevel, source)
                .exceptionally(throwable -> {
                    LOGGER.error("Critical error while running the image command", throwable);
                    source.sendSystemMessage(Component.literal("§cCritical error: " + throwable.getMessage()));
                    return null;
                });

            ItemStack stack = new ItemStack(Items.DIAMOND);
            player.addItem(stack);

            source.sendSystemMessage(Component.literal("§aCommand started successfully! Generation is running in the background."));

        } catch (Exception e) {
            LOGGER.error("Error while creating BuilderImage", e);
            source.sendSystemMessage(Component.literal("§cError during initialization: " + e.getMessage()));
            return 0;
        }

        return 1;
    }

    private static int collapse(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        try {
            String text = BuilderImage.readEffectsArgument(context);
            BuilderImage.collapse(source, text == null ? null : Effects.parse(text));
        } catch (Exception e) {
            LOGGER.error("Error during collapse", e);
            source.sendSystemMessage(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }

        return 1;
    }

    static class AttributeTypeHeightSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
        private final String[] names = RegistryGeneratorsHeightMap.getAvailableNames();

        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
            for (String name : names) {
                builder.suggest(name);
            }
            return builder.buildFuture();
        }
    }

    /** Suggests the effect names, also after a '+' (burst+jitter). */
    static class EffectSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
            String typed = builder.getRemaining();
            String prefix = typed.substring(0, typed.lastIndexOf('+') + 1);
            for (String name : Effects.NAMES) {
                builder.suggest(prefix + name);
            }
            return builder.buildFuture();
        }
    }

    static class AttributeFileSuggestionProvider implements SuggestionProvider<CommandSourceStack> {

        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
            for (String file : ImageGetter.getAvailableImages()) {
                builder.suggest(file);
            }

            return builder.buildFuture();
        }
    }

}
