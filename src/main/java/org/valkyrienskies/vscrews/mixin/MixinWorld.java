package org.valkyrienskies.vscrews.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

@Mixin(World.class)
public abstract class MixinWorld {

    @Inject(
            method = "getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/function/Predicate;)Ljava/util/List;",
            at = @At("HEAD"),
            cancellable = true
    )
    private <T extends Entity> void vscrews_fixInfiniteAABB_getEntitiesOfClass(
            Class<? extends T> clazz,
            AxisAlignedBB aabb,
            Predicate<? super T> predicate,
            CallbackInfoReturnable<List<T>> cir
    ) {
        if (isInfiniteAABB(aabb)) {
            cir.setReturnValue(Collections.emptyList());
        }
    }

    // ✔ ALSO VALID IN 1.16.5
    @Inject(
            method = "getEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/function/Predicate;)Ljava/util/List;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void vscrews_fixInfiniteAABB_getEntities(
            Entity entity,
            AxisAlignedBB aabb,
            Predicate<? super Entity> predicate,
            CallbackInfoReturnable<List<Entity>> cir
    ) {
        if (isInfiniteAABB(aabb)) {
            cir.setReturnValue(Collections.emptyList());
        }
    }

    private static boolean isInfiniteAABB(AxisAlignedBB aabb) {
        return Double.isInfinite(aabb.minX)
                || Double.isInfinite(aabb.minY)
                || Double.isInfinite(aabb.minZ)
                || Double.isInfinite(aabb.maxX)
                || Double.isInfinite(aabb.maxY)
                || Double.isInfinite(aabb.maxZ);
    }
}

