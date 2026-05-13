package org.valkyrienskies.vscrews;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.ModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.valkyrienskies.vscrews.commands.VSCrewCommand;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.valkyrienskies.vscrews.crew.CrewManager;

@Mod("vs-crews")
public class VSCrewsMod
{
    public static final Logger LOGGER = LogManager.getLogger();

    public VSCrewsMod() {
        // Register config as SERVER to avoid client-side mismatches and use per-world serverconfig
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, VSCrewsConfig.COMMON_SPEC);

        // Register commands
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onWorldLoad);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLogin);

        LOGGER.info("[VS-Crews] Registered listeners, commands, and config");
    }

    private void onRegisterCommands(final RegisterCommandsEvent event) {
        VSCrewCommand.register(event.getDispatcher());
        LOGGER.info("[VS-Crews] Registered commands");
    }

    private void onWorldLoad(final WorldEvent.Load event) {
        if (event.getWorld() instanceof ServerWorld) {
            ServerWorld sw = (ServerWorld) event.getWorld();
            CrewManager.init(sw);
            LOGGER.info("[VS-Crews] Loaded crew data for world {}", sw.dimension().location());
        }
    }

    private void onPlayerLogin(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity sp = (ServerPlayerEntity) event.getPlayer();
            CrewManager.cacheName(sp.getUUID(), sp.getGameProfile().getName());
            LOGGER.debug("[VS-Crews] Cached player name: {} -> {}", sp.getUUID(), sp.getGameProfile().getName());
        }
    }
}
