package org.valkyrienskies.vscrews.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.Locale;

@Pseudo
@Mixin(
        targets = {
                "xyz.pixelatedw.mineminenomi.entities.projectiles.beta.BetaBetaChainProjectile",
                "xyz.pixelatedw.mineminenomi.entities.projectiles.BetaBetaChainProjectile"
        },
        remap = false
)
public abstract class MixinBetaBetaChainProjectile {

    @Unique
    private static final Logger VSCREWS_LOG = LogManager.getLogger("VSCrews/BetaChain");

    @Inject(method = "onBlockImpactEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private void vscrews_fixShipRelativePull(BlockPos hit, CallbackInfo ci) {
        int life = vscrews_invokeInt(this, "getLife");
        int maxLife = vscrews_invokeInt(this, "getMaxLife");
        if (life >= maxLife) {
            ci.cancel();
            return;
        }

        LivingEntity entity = vscrews_invokeLiving(this, "getThrower");
        if (entity == null) {
            ci.cancel();
            return;
        }

        double hitBlockX = hit.getX() + 0.5D;
        double hitBlockY = hit.getY() + 0.5D;
        double hitBlockZ = hit.getZ() + 0.5D;

        double hitX = vscrews_invokeDouble(this, "getX", hitBlockX);
        double hitY = vscrews_invokeDouble(this, "getY", hitBlockY);
        double hitZ = vscrews_invokeDouble(this, "getZ", hitBlockZ);

        double dx = hitX - entity.getX();
        double dy = hitY - entity.getY();
        double dz = hitZ - entity.getZ();

        double pullX = dx * 0.35D;
        double pullY = 0.3D + dy * 0.35D;
        double pullZ = dz * 0.35D;

        VSCREWS_LOG.info(String.format(Locale.ROOT,
                "[BetaImpact] attachProj=(%.3f, %.3f, %.3f) attachBlock=(%.3f, %.3f, %.3f) player=(%.3f, %.3f, %.3f) pull=(%.3f, %.3f, %.3f) life=%d/%d dim=%s",
                hitX, hitY, hitZ,
                hitBlockX, hitBlockY, hitBlockZ,
                entity.getX(), entity.getY(), entity.getZ(),
                pullX, pullY, pullZ,
                life, maxLife,
                entity.level.dimension().location()));

        if (!vscrews_trySetDeltaMovement(entity, pullX, pullY, pullZ)) {
            entity.setDeltaMovement(pullX, pullY, pullZ);
            VSCREWS_LOG.warn("[BetaImpact] Fallback to Entity#setDeltaMovement because AbilityHelper reflection failed");
        }

        ci.cancel();
    }

    @Unique
    private static double vscrews_invokeDouble(Object target, String methodName, double fallback) {
        try {
            Method m = target.getClass().getMethod(methodName);
            Object out = m.invoke(target);
            return out instanceof Number ? ((Number) out).doubleValue() : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
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
    private static boolean vscrews_trySetDeltaMovement(LivingEntity entity, double x, double y, double z) {
        String[] helperClasses = {
                "xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper",
                "xyz.pixelatedw.mineminenomi.wypi.abilities.AbilityHelper"
        };

        for (String helperClassName : helperClasses) {
            try {
                Class<?> helper = Class.forName(helperClassName);
                Method setDelta = helper.getMethod("setDeltaMovement", Entity.class, double.class, double.class, double.class);
                setDelta.invoke(null, entity, x, y, z);
                return true;
            } catch (Throwable ignored) {
                // Try next candidate helper class.
            }
        }

        return false;
    }
}
