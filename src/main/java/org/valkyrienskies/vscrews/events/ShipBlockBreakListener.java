package org.valkyrienskies.vscrews.events;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.valkyrienskies.core.api.Ship;
import org.valkyrienskies.vscrews.VSCrewsConfig;
import org.valkyrienskies.vscrews.crew.Crew;
import org.valkyrienskies.vscrews.crew.CrewManager;

import java.util.UUID;

import static org.valkyrienskies.mod.common.VSGameUtilsKt.getShipManagingPos;
import static org.valkyrienskies.vscrews.crew.CrewManager.findHelmOwner;

@Mod.EventBusSubscriber
public class ShipBlockBreakListener {

    /* ==========================================================
     * PLAYER BREAKING (LEFT CLICK)
     * ========================================================== */
    @SubscribeEvent
    public static void onPlayerBreak(BlockEvent.BreakEvent event) {
        PlayerEntity player = event.getPlayer();
        World world = player.level;

        if (world.isClientSide) return;

        BlockPos pos = event.getPos();
        Ship ship = getShipManagingPos(world, pos);
        if (ship == null) return;

        UUID playerId = player.getUUID();

        // Who owns the ship (via helm)
        UUID helmOwner = findHelmOwner(world, pos);

        // Crew that owns this ship
        Crew shipCrew = CrewManager.findCrewByShip(ship);

        // Crew of the player
        Crew playerCrew = CrewManager.findCrewByMember(playerId);

        /* ==================================================
         * PERMISSION LOGIC
         * ================================================== */

        // Public ships
        if (VSCrewsConfig.HELM_WITHOUT_CREW_USABLE_BY_EVERYONE.get()) {
            return;
        }

        // Helm owner can always break
        if (helmOwner != null && helmOwner.equals(playerId)) {
            return;
        }

        // Crew-owned ship: only that crew may break
        if (shipCrew != null) {
            if (playerCrew != null && playerCrew.equals(shipCrew)) {
                return;
            }
            event.setCanceled(true);
            return;
        }

        // Default: deny
        event.setCanceled(true);
    }

    /* ==========================================================
     * EXPLOSIONS (SAFETY NET)
     * ========================================================== */
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        World world = event.getWorld();
        if (world.isClientSide) return;

        event.getAffectedBlocks().removeIf(pos ->
                getShipManagingPos(world, pos) != null
        );
    }
}
