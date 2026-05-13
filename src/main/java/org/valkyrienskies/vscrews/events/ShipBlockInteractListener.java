package org.valkyrienskies.vscrews.events;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.valkyrienskies.core.api.Ship;
import org.valkyrienskies.vscrews.VSCrewsConfig;
import org.valkyrienskies.vscrews.crew.Crew;
import org.valkyrienskies.vscrews.crew.CrewManager;
import org.valkyrienskies.vscrews.helm.HelmOwnership;

import java.util.UUID;

import static org.valkyrienskies.mod.common.VSGameUtilsKt.getShipManagingPos;
import static org.valkyrienskies.vscrews.crew.CrewManager.findHelmOwner;

@Mod.EventBusSubscriber
public class ShipBlockInteractListener {

    /* ==========================================================
     * INTERACTIONS (RIGHT CLICK) — CREW / HELM PLACER RULES
     * ========================================================== */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        PlayerEntity player = event.getPlayer();
        World world = player.level;

        if (world.isClientSide) return;

        BlockPos pos = event.getPos();

        Ship ship = getShipManagingPos(world, pos);
        if (ship == null) return;

        UUID playerId = player.getUUID();
        UUID helmOwner = findHelmOwner(world, pos);
        Crew playerCrew = CrewManager.findCrewByMember(playerId);
        Crew shipCrew = helmOwner != null
                ? CrewManager.findCrewByShip(ship)
                : null;

        /* ==========================================================
         * PERMISSION LOGIC
         * ========================================================== */

        if (VSCrewsConfig.HELM_WITHOUT_CREW_USABLE_BY_EVERYONE.get()) {
            return;
        }

        if (helmOwner != null && helmOwner.equals(playerId)) {
            return;
        }

        if (shipCrew != null && shipCrew.ownsShip(ship)) {
            if (playerCrew != null && playerCrew.equals(shipCrew)) {
                return;
            }
            deny(event);
            return;
        }

        deny(event);
    }


    /* ==========================================================
     * HELPERS
     * ========================================================== */
    private static void deny(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(ActionResultType.FAIL);
    }
}
