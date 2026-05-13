package org.valkyrienskies.vscrews.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Pseudo
@Mixin(
        targets = {
                "xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuGomuNoRocketProjectile",
                "xyz.pixelatedw.mineminenomi.entities.projectiles.GomuGomuNoRocketProjectile"
        },
        remap = false
)
public abstract class MixinGomuGomuNoRocketProjectile {

    @Inject(method = "onBlockImpactEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private void vscrews_fixShipRelativePull(BlockPos hit, CallbackInfo ci) {
        int life = vscrews_invokeInt(this, "getLife");
        int maxLife = vscrews_invokeInt(this, "getMaxLife");
        if (life >= maxLife) {
            ci.cancel();
            return;
        }

        LivingEntity entity = vscrews_invokeLiving(this, "getThrower");
        if (entity != null) {
            vscrews_tryRemoveReducedFall(entity);

            double dx = (hit.getX() + 0.5D) - entity.getX();
            double dy = (hit.getY() + 0.5D) - entity.getY();
            double dz = (hit.getZ() + 0.5D) - entity.getZ();

            vscrews_trySetDeltaMovement(entity, dx * 0.35D, 0.3D + dy * 0.35D, dz * 0.35D);
        }

        ci.cancel();
    }

    @Unique
    private static int vscrews_invokeInt(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            Object out = m.invoke(target);
            return out instanceof Number ? ((Number) out).intValue() : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    @Unique
    private static LivingEntity vscrews_invokeLiving(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            Object out = m.invoke(target);
            return out instanceof LivingEntity ? (LivingEntity) out : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Unique
    private static void vscrews_tryRemoveReducedFall(LivingEntity entity) {
        String[] effectHolderClasses = {
                "xyz.pixelatedw.mineminenomi.init.ModEffects",
                "xyz.pixelatedw.mineminenomi.init.ModEffect"
        };

        for (String holderClassName : effectHolderClasses) {
            try {
                Class<?> holderClass = Class.forName(holderClassName);
                Field reducedFallField = holderClass.getField("REDUCED_FALL");
                Object reducedFallObj = reducedFallField.get(null);

                Object effectObj = reducedFallObj;
                if (reducedFallObj != null) {
                    try {
                        Method getMethod = reducedFallObj.getClass().getMethod("get");
                        effectObj = getMethod.invoke(reducedFallObj);
                    } catch (NoSuchMethodException ignored) {
                        // Not a supplier-like object; use field value directly.
                    }
                }

                if (effectObj instanceof Effect) {
                    entity.removeEffect((Effect) effectObj);
                }
                return;
            } catch (Throwable ignored) {
                // Try next candidate holder class.
            }
        }
    }

    @Unique
    private static void vscrews_trySetDeltaMovement(LivingEntity entity, double x, double y, double z) {
        String[] helperClasses = {
                "xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper",
                "xyz.pixelatedw.mineminenomi.wypi.abilities.AbilityHelper"
        };

        for (String helperClassName : helperClasses) {
            try {
                Class<?> helper = Class.forName(helperClassName);
                Method setDelta = helper.getMethod("setDeltaMovement", net.minecraft.entity.Entity.class, double.class, double.class, double.class);
                setDelta.invoke(null, entity, x, y, z);
                return;
            } catch (Throwable ignored) {
                // Try next candidate helper class.
            }
        }
    }
}

