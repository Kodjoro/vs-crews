package org.valkyrienskies.vscrews.events;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.valkyrienskies.vscrews.VSCrewsMod;
import org.valkyrienskies.vscrews.crew.Crew;
import org.valkyrienskies.vscrews.crew.CrewManager;
import net.minecraft.tileentity.TileEntity;
import org.valkyrienskies.vscrews.helm.HelmOwnership;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class HelmPlaceListener {

    private static final Set<ResourceLocation> HELM_ITEM_IDS = new HashSet<>(Arrays.asList(
            new ResourceLocation("vs_eureka", "acacia_ship_helm"),
            new ResourceLocation("vs_eureka", "oak_ship_helm"),
            new ResourceLocation("vs_eureka", "birch_ship_helm"),
            new ResourceLocation("vs_eureka", "jungle_ship_helm"),
            new ResourceLocation("vs_eureka", "dark_oak_ship_helm"),
            new ResourceLocation("vs_eureka", "crimson_ship_helm"),
            new ResourceLocation("vs_eureka", "warped_ship_helm")
    ));

    @SubscribeEvent
    public void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        VSCrewsMod.LOGGER.info("[HelmPlaceListener] Event triggered");

        // Entity check
        if (!(event.getEntity() instanceof PlayerEntity)) {
            VSCrewsMod.LOGGER.info("[HelmPlaceListener] Not a PlayerEntity: {}", event.getEntity());
            return;
        }
        PlayerEntity player = (PlayerEntity) event.getEntity();
        VSCrewsMod.LOGGER.info("[HelmPlaceListener] Player detected: {}", player.getName().getString());

        // World check
        if (!(event.getWorld() instanceof net.minecraft.world.server.ServerWorld)) {
            VSCrewsMod.LOGGER.info("[HelmPlaceListener] Not a ServerWorld");
            return;
        }
        net.minecraft.world.server.ServerWorld world =
                (net.minecraft.world.server.ServerWorld) event.getWorld();
        VSCrewsMod.LOGGER.info("[HelmPlaceListener] ServerWorld confirmed");

        // Item retrieval
        ItemStack used = player.getMainHandItem();
        if (used.isEmpty()) {
            VSCrewsMod.LOGGER.info("[HelmPlaceListener] Player main hand is empty");
            return;
        }
        VSCrewsMod.LOGGER.info("[HelmPlaceListener] Main hand item: {}", used.getItem().getRegistryName());

        // RegistryName check
        ResourceLocation id = used.getItem().getRegistryName();
        if (id == null) {
            VSCrewsMod.LOGGER.warn("[HelmPlaceListener] Item registry name is null");
            return;
        }
        VSCrewsMod.LOGGER.info("[HelmPlaceListener] RegistryName found: {}", id);

        // Helm ID set check
        if (!HELM_ITEM_IDS.contains(id)) {
            VSCrewsMod.LOGGER.info("[HelmPlaceListener] Item ID not in HELM_ITEM_IDS: {}", id);
            return;
        }
        VSCrewsMod.LOGGER.info("[HelmPlaceListener] Item is a valid helm: {}", id);

        // BlockEntity check
        TileEntity te = world.getBlockEntity(event.getPos());
        if (te == null) {
            VSCrewsMod.LOGGER.info("[HelmPlaceListener] No TileEntity at pos: {}", event.getPos());
            return;
        }
        VSCrewsMod.LOGGER.info("[HelmPlaceListener] TileEntity found at pos: {}", event.getPos());

        // Crew lookup
        Crew crew = CrewManager.findCrewByOwner(player.getUUID());
        if (crew != null) {
            VSCrewsMod.LOGGER.info("[HelmPlaceListener] Crew found for player: {}", crew.getName());
        } else {
            VSCrewsMod.LOGGER.info("[HelmPlaceListener] No crew found for player: {}", player.getName().getString());
        }

        HelmOwnership.setOwnership(te, player.getUUID(), crew);
        VSCrewsMod.LOGGER.info("[HelmPlaceListener] Ownership applied to TileEntity");

        VSCrewsMod.LOGGER.info("[HelmPlaceListener] SUCCESS: Helm placed by {} at {}. Crew: {}",
                player.getName().getString(),
                event.getPos(),
                crew != null ? crew.getName() : "none");
    }
}
