package com.qendolin.betterclouds;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.qendolin.betterclouds.clouds.Debug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class Commands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal(Main.MODID + ":profile")
                .then(argument("interval", IntegerArgumentType.integer(30))
                        .executes(context -> {
                            int interval = IntegerArgumentType.getInteger(context, "interval");
                            Main.debugChatMessage("profiling.enabled", interval);
                            Debug.profileInterval = interval;
                            return 1;
                        }))
                .then(literal("stop")
                        .executes(context -> {
                            Main.debugChatMessage("profiling.disabled");
                            Debug.profileInterval = 0;
                            return 1;
                        }))
        );
        /*dispatcher.register(literal(Main.MODID + ":frustum")
                .then(literal("capture")
                        .executes(context -> {
                            if (context.getSource().getEntity() instanceof LocalPlayer player) {
                                context.getSource().getPlayer().getClient().worldRenderer.captureFrustum();
                                return 1;
                            }
                            return 0;
                        }))
                .then(literal("release")
                        .executes(context -> {
                            context.getSource().getClient().worldRenderer.killFrustum();
                            return 1;
                        }))
                .then(literal("debugCulling")
                        .then(argument("enable", BoolArgumentType.bool())
                                .executes(context -> {
                                    Debug.frustumCulling = BoolArgumentType.getBool(context, "enable");
                                    return 1;
                                }))));*/
        dispatcher.register(literal(Main.MODID + ":generator")
                .then(literal("pause")
                        .executes(context -> {
                            Debug.generatorPause = true;
                            Main.debugChatMessage("generatorPaused");
                            return 1;
                        }))
                .then(literal("resume")
                        .executes(context -> {
                            Debug.generatorPause = false;
                            Main.debugChatMessage("generatorResumed");
                            return 1;
                        })));
        dispatcher.register(literal(Main.MODID + ":config")
                .then(literal("open").executes(context -> {
                    Minecraft client = Minecraft.getInstance();
                    // The chat screen will call setScreen(null) after the command handler
                    // which would override our call, so we delay it
                    client.execute(() -> client.setScreen(ConfigGUI.create(null)));
                    return 1;
                }))
                .then(literal("reload").executes(context -> {
                    Main.debugChatMessage("reloadingConfig");
                    Main.getConfigInstance().load();
                    Main.debugChatMessage("configReloaded");
                    return 1;
                }))
                .then(literal("gpuIncompatibleMessage")
                        .then(argument("enable", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean enable = BoolArgumentType.getBool(context, "enable");
                                    if (Main.getConfig().gpuIncompatibleMessageEnabled == enable) return 1;
                                    Main.getConfig().gpuIncompatibleMessageEnabled = enable;
                                    Main.getConfigInstance().save();
                                    Main.debugChatMessage("updatedPreferences");
                                    return 1;
                                }))));
    }
}
