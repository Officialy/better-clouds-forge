package com.qendolin.betterclouds;

import com.qendolin.betterclouds.clouds.Debug;
import com.qendolin.betterclouds.compat.GLCompat;
import com.qendolin.betterclouds.compat.GsonConfigInstanceBuilderDuck;
import com.qendolin.betterclouds.compat.Telemetry;
import dev.isxander.yacl3.config.GsonConfigInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL32;

import java.nio.file.Path;

@Mod(Main.MODID)
public class Main {
    public static final String MODID = "betterclouds";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static final boolean IS_DEV = FMLLoader.isProduction(); //todo test

    public static GLCompat glCompat;

    private static final GsonConfigInstance<Config> CONFIG;

    static {
        if (FMLLoader.getDist().equals(Dist.CLIENT)) {
            GsonConfigInstance.Builder<Config> builder = GsonConfigInstance
                .createBuilder(Config.class)
                .setPath(Path.of("config/betterclouds-v1.json"));

            if (builder instanceof GsonConfigInstanceBuilderDuck) {
                //noinspection unchecked
                GsonConfigInstanceBuilderDuck<Config> duck = (GsonConfigInstanceBuilderDuck<Config>) builder;
                builder = duck.betterclouds$appendGsonBuilder(b -> b
                    .setLenient().setPrettyPrinting()
                    .registerTypeAdapter(Config.class, Config.INSTANCE_CREATOR)
                    .registerTypeAdapter(Config.ShaderConfigPreset.class, Config.ShaderConfigPreset.INSTANCE_CREATOR));
            }
            CONFIG = builder.build();
        } else {
            CONFIG = null;
        }
    }

    public static void initGlCompat() {
        glCompat = new GLCompat(IS_DEV);
        if (glCompat.isIncompatible()) {
            LOGGER.warn("Your GPU is not compatible with Better Clouds. OpenGL 4.3 is required!");
            LOGGER.info(" - Vendor:       {}", GL32.glGetString(GL32.GL_VENDOR));
            LOGGER.info(" - Renderer:     {}", GL32.glGetString(GL32.GL_RENDERER));
            LOGGER.info(" - GL Version:   {}", GL32.glGetString(GL32.GL_VERSION));
            LOGGER.info(" - GLSL Version: {}", GL32.glGetString(GL32.GL_SHADING_LANGUAGE_VERSION));
            LOGGER.info(" - Extensions:   {}", String.join(", ", glCompat.supportedCheckedExtensions));
            LOGGER.info(" - Functions:    {}", String.join(", ", glCompat.supportedCheckedFunctions));
        }
        if (getConfig().lastTelemetryVersion < Telemetry.VERSION && Telemetry.INSTANCE != null) {
            Telemetry.INSTANCE.sendSystemInfo()
                .whenComplete((success, throwable) -> {
                    Minecraft client = Minecraft.getInstance();
                    if (success && client != null) {
                        client.execute(() -> {
                            getConfig().lastTelemetryVersion = Telemetry.VERSION;
                            CONFIG.save();
                        });
                    }
                });
        }
    }

    public static Config getConfig() {
        return CONFIG.getConfig();
    }

    public static boolean isProfilingEnabled() {
        return Debug.profileInterval > 0;
    }

    public static void debugChatMessage(String id, Object... args) {
        debugChatMessage(Component.translatable(debugChatMessageKey(id), args));
    }

    public static void debugChatMessage(Component message) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        client.gui.getChat().addMessage(Component.literal("§e[§bBC§b§e]§r ").append(message));
    }

    public static String debugChatMessageKey(String id) {
        return MODID + ".message." + id;
    }

    static GsonConfigInstance<Config> getConfigInstance() {
        return CONFIG;
    }

    public void onInitializeClient() {
        if (CONFIG == null)
            throw new IllegalStateException("CONFIG is null!");
        CONFIG.load();

//        ClientLifecycleEvents.CLIENT_STARTED.register(client -> glCompat.enableDebugOutputSynchronous());

//        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
//            if (!glCompat.isIncompatible()) return;
//            CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS)
//                .execute(() -> client.execute(Main::sendGpuIncompatibleChatMessage));
//        });

//        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
//            .registerReloadListener(ShaderPresetLoader.INSTANCE);

//        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> Commands.register(dispatcher));

        if (!IS_DEV) return;
        LOGGER.info("Initialized in dev mode, performance might vary");
    }

    public static void sendGpuIncompatibleChatMessage() {
        if (!getConfig().gpuIncompatibleMessageEnabled) return;
        debugChatMessage(
            Component.translatable(debugChatMessageKey("gpuIncompatible"))
                .append(Component.literal("\n - "))
                    .append(Component.translatable(debugChatMessageKey("gpuIncompatible.disable"))
                    .withStyle(style -> style.withItalic(true).withUnderlined(true).withColor(ChatFormatting.GRAY)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                            "/betterclouds:config gpuIncompatibleMessage false")))));
    }

}
