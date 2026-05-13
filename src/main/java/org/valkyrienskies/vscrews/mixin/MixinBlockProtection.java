package org.valkyrienskies.vscrews.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.api.protection.BlockProtectionRule;

import static org.valkyrienskies.mod.common.VSGameUtilsKt.getShipManagingPos;

@Mixin(value = BlockProtectionRule.class, remap = false)
public abstract class MixinBlockProtection {

    @Inject(
            method = "check",
            at = @At("HEAD"),
            cancellable = true
    )
    private void vscrews_blockMMNMOnShips(
            World world,
            BlockPos pos,
            BlockState state,
            CallbackInfoReturnable<Boolean> cir
    ) {
        // Only enforce on server
        if (world.isClientSide) {
            return;
        }

        if (getShipManagingPos(world, pos) == null) {
            return;
        }
        // Hard deny MMNM ability placement
        cir.setReturnValue(false);
    }
}
