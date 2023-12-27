package com.qendolin.betterclouds;

import com.qendolin.betterclouds.clouds.Debug;
import com.qendolin.betterclouds.compat.GLCompat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.opengl.GL32;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Mod(Main.MODID)
public class Main {
    public static final String MODID = "betterclouds";
    public static final boolean IS_DEV = FMLLoader.isProduction();
    public static final boolean IS_CLIENT = FMLLoader.getDist().isClient(); //todo test it   //FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    public static final NamedLogger LOGGER = new NamedLogger(LogManager.getLogger(MODID), !IS_DEV);

    public static GLCompat glCompat;

    private static final Config CONFIG = new Config();

    public Main() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onClientSetup);
        NeoForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CONFIG.GENERAL_SPEC, "betterclouds-v1.toml");
    }

    public static Config getConfig() {
        return CONFIG;
    }

    public static void initGlCompat() {
        try {
            glCompat = new GLCompat(IS_DEV);
        } catch (Exception e) {
//            Telemetry.INSTANCE.sendUnhandledException(e);
            throw e;
        }

        if (glCompat.isIncompatible()) {
            LOGGER.warn("Your GPU is not compatible with Better Clouds. Try updating your drivers?");
            LOGGER.info(" - Vendor:       {}", GL32.glGetString(GL32.GL_VENDOR));
            LOGGER.info(" - Renderer:     {}", GL32.glGetString(GL32.GL_RENDERER));
            LOGGER.info(" - GL Version:   {}", GL32.glGetString(GL32.GL_VERSION));
            LOGGER.info(" - GLSL Version: {}", GL32.glGetString(GL32.GL_SHADING_LANGUAGE_VERSION));
            LOGGER.info(" - Extensions:   {}", String.join(", ", glCompat.supportedCheckedExtensions));
            LOGGER.info(" - Functions:    {}", String.join(", ", glCompat.supportedCheckedFunctions));
        } else if (glCompat.isPartiallyIncompatible()) {
            LOGGER.warn("Your GPU is not fully compatible with Better Clouds.");
            for (String fallback : glCompat.usedFallbacks) {
                LOGGER.info("- Using {} fallback", fallback);
            }
        }
    }

    public static boolean isProfilingEnabled() {
        return Debug.profileInterval > 0;
    }

    public static void debugChatMessage(String id, Object... args) {
        debugChatMessage(Text.translatable(debugChatMessageKey(id), args));
    }

    public static void debugChatMessage(Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) return;
        client.inGameHud.getChatHud().addMessage(Text.literal("§e[§bBC§b§e]§r ").append(message));
    }

    public static String debugChatMessageKey(String id) {
        return MODID + ".message." + id;
    }

//    public static Version getVersion() {
//        return version;
//    }

    public void onClientSetup(FMLClientSetupEvent event) {
//        ClientLifecycleEvents.CLIENT_STARTED.register(client -> glCompat.enableDebugOutputSynchronous());
//        glCompat.enableDebugOutputSynchronous();

//        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(ShaderPresetLoader.INSTANCE);

        if (!IS_DEV) return;
        LOGGER.info("Initialized in dev mode, performance might vary");
    }

    @SubscribeEvent
    public void onClientLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (glCompat.isIncompatible()) {
            CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute(Main::sendGpuIncompatibleChatMessage);
        }
        if (glCompat.isIncompatible()) {
            CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(Main::sendGpuIncompatibleChatMessage);
        } else if (glCompat.isPartiallyIncompatible()) {
            CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(Main::sendGpuPartiallyIncompatibleChatMessage);
        }
    }

    public static void sendGpuIncompatibleChatMessage() {
        if (!Config.gpuIncompatibleMessageEnabled.get()) return;
        debugChatMessage(
                Text.translatable(debugChatMessageKey("gpuIncompatible"))
                        .append(Text.literal("\n - "))
                        .append(Text.translatable(debugChatMessageKey("disable"))
                                .styled(style -> style.withItalic(true).withUnderline(true).withColor(Formatting.GRAY)
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                                "/betterclouds:config gpuIncompatibleMessage false")))));
    }

    public static void sendGpuPartiallyIncompatibleChatMessage() {
        if (!Config.gpuIncompatibleMessageEnabled.get()) return;
        debugChatMessage(
                Text.translatable(debugChatMessageKey("gpuPartiallyIncompatible"))
                        .append(Text.literal("\n - "))
                        .append(Text.translatable(debugChatMessageKey("disable"))
                                .styled(style -> style.withItalic(true).withUnderline(true).withColor(Formatting.GRAY)
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                                "/betterclouds:config gpuIncompatibleMessage false")))));
    }

    public void addReloadListenerEvent(AddReloadListenerEvent e) {

    }

    @SubscribeEvent
    public void onRegisterCommandEvent(RegisterClientCommandsEvent event) {
        Commands.register(event.getDispatcher());
    }
}
