package org.valkyrienskies.vscrews.events;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.valkyrienskies.core.api.Ship;
import org.valkyrienskies.vscrews.VSCrewsMod;
import org.valkyrienskies.vscrews.crew.Crew;
import org.valkyrienskies.vscrews.crew.CrewManager;
import net.minecraft.tileentity.TileEntity;
import org.valkyrienskies.vscrews.helm.HelmOwnership;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.valkyrienskies.mod.common.VSGameUtilsKt.getShipManagingPos;

@Mod.EventBusSubscriber
public class HelmPlaceListener {

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        // Only players
       if (!(event.getEntity() instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) event.getEntity();

        // Server only
        if (!(event.getWorld() instanceof net.minecraft.world.server.ServerWorld)) return;
        net.minecraft.world.server.ServerWorld world =
                (net.minecraft.world.server.ServerWorld) event.getWorld();

        BlockPos pos = event.getPos();

        // TileEntity must exist
        TileEntity te = world.getBlockEntity(pos);
        if (te == null) return;

        // Must be a helm (source of truth)
        if (!HelmOwnership.isHelm(te)) return;

        // Resolve crew (member, not just owner)
        Crew crew = CrewManager.findCrewByMember(player.getUUID());

        /* ==========================================================
         * ASSIGN OWNERSHIP
         * ========================================================== */
        HelmOwnership.setOwnership(
                te,
                player.getUUID(),
                crew
        );
    }
}
