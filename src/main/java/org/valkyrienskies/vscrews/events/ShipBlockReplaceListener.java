package org.valkyrienskies.vscrews.events;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static org.valkyrienskies.mod.common.VSGameUtilsKt.getShipManagingPos;

@Mod.EventBusSubscriber
public class ShipBlockReplaceListener {
    /* ==========================================================
     * BLOCK REPLACEMENT (FIRE, LAVA, COMMANDS, FLUIDS, ETC.)
     * ========================================================== */
    @SubscribeEvent
    public static void onBlockReplace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getWorld() instanceof World)) return;
        World world = (World) event.getWorld();

        if (world.isClientSide) return;

        BlockPos pos = event.getPos();

        // Only care about ship blocks
        if (getShipManagingPos(world, pos) == null) return;

        Entity entity = event.getEntity();
        // Allow player placement of blocks (normal building)
        if (entity instanceof PlayerEntity) {
            return;
        }

        // EVERYTHING ELSE is denied (fire, lava, commands, explosions, etc.)
        event.setCanceled(true);
    }

}
