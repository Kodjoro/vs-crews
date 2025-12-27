package org.valkyrienskies.vscrews.events;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.valkyrienskies.vscrews.VSCrewsConfig;
import org.valkyrienskies.vscrews.VSCrewsMod;
import org.valkyrienskies.vscrews.crew.Crew;
import org.valkyrienskies.vscrews.crew.CrewManager;
import net.minecraft.tileentity.TileEntity;
import org.valkyrienskies.vscrews.helm.HelmOwnership;

import java.util.UUID;

public class InteractListener {

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        PlayerEntity player = event.getPlayer();
        if (player == null || player.level == null || player.level.isClientSide) return;

        BlockPos pos = event.getPos();
        TileEntity entity = player.level.getBlockEntity(pos);
        if (entity == null) return;

        UUID owner = HelmOwnership.getOwner(entity);
        String crewName = HelmOwnership.getCrewName(entity);
        if (owner == null) return;

        // If crewName is set but no such crew exists, treat as no-crew
        Crew crewResolved = crewName != null ? CrewManager.getCrew(crewName) : null;
        if (crewName == null || crewResolved == null) {
            if (VSCrewsConfig.HELM_WITHOUT_CREW_USABLE_BY_EVERYONE.get()) {
                player.displayClientMessage(new StringTextComponent("Let's set sail"), true);
                return;
            } else {
                boolean isPlacer = owner.equals(player.getUUID());
                if (isPlacer) {
                    player.displayClientMessage(new StringTextComponent("Let's set sail"), true);
                    return;
                } else {
                    player.displayClientMessage(new StringTextComponent("You can't use this helm. Only the placer can."), true);
                    event.setCanceled(true);
                    event.setCancellationResult(ActionResultType.FAIL);
                    return;
                }
            }
        }

        boolean isMember = crewResolved.isMember(player.getUUID());
        if (!isMember) {
            player.displayClientMessage(new StringTextComponent("You can't use this helm."), true);
            event.setCanceled(true);
            event.setCancellationResult(ActionResultType.FAIL);
            VSCrewsMod.LOGGER.info("Denied helm interaction for player={} at pos={} crew={} ownerUUID={}", player.getName().getString(), pos, crewName, owner);
        } else {
            player.displayClientMessage(new StringTextComponent("Let's set sail " + crewName), true);
            VSCrewsMod.LOGGER.info("Allowed helm interaction for player={} at pos={} crew={} ownerUUID={}", player.getName().getString(), pos, crewName, owner);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        PlayerEntity player = event.getPlayer();
        if (player == null || player.level == null || player.level.isClientSide) return;

        BlockPos pos = event.getPos();
        TileEntity entity = player.level.getBlockEntity(pos);
        if (entity == null) return;

        UUID owner = HelmOwnership.getOwner(entity);
        String crewName = HelmOwnership.getCrewName(entity);
        if (owner == null) return;

        Crew crewResolved = crewName != null ? CrewManager.getCrew(crewName) : null;
        if (crewName == null || crewResolved == null) {
            if (VSCrewsConfig.ALLOW_NON_CREW_BREAK_HELM.get()) {
                return;
            } else {
                if (!owner.equals(player.getUUID())) {
                    player.displayClientMessage(new StringTextComponent("You can't break this helm. Only the owner can."), true);
                    event.setCanceled(true);
                    return;
                }
            }
            return;
        }

        boolean isMember = crewResolved.isMember(player.getUUID());
        if (!isMember) {
            if (VSCrewsConfig.ALLOW_NON_CREW_BREAK_HELM.get()) {
                return;
            }
            player.displayClientMessage(new StringTextComponent("You can't break this helm."), true);
            event.setCanceled(true);
            VSCrewsMod.LOGGER.info("Denied helm break for player={} at pos={} crew={} ownerUUID={}", player.getName().getString(), pos, crewName, owner);
        } else {
            VSCrewsMod.LOGGER.info("Allowed helm break for player={} at pos={} crew={} ownerUUID={}", player.getName().getString(), pos, crewName, owner);
        }
    }
}
