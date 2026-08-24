package com.example.typemoonaddon.worm;

import com.example.typemoonaddon.registry.AddonEntities;
import com.example.typemoonaddon.registry.AddonItems;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;

public final class WormCaptureService {
    private WormCaptureService() {
    }

    public static boolean canCapture(ServerPlayer player, LivingEntity target) {
        if (player == null || target == null || !target.isAlive()) {
            return false;
        }
        if (target instanceof WormEntity worm) {
            if (player.getUUID().equals(worm.getOwnerId())) {
                return true;
            }
            return worm.getOwnerId() == null && isControlledForCapture(worm);
        }
        WormType type = typeOf(target);
        return type != null && isControlledForCapture(target);
    }

    @Nullable
    public static ItemStack capture(ServerPlayer player, LivingEntity target) {
        if (!canCapture(player, target)) {
            return ItemStack.EMPTY;
        }
        WormType type = typeOf(target) == null ? WormType.SILVERFISH : typeOf(target);
        int gu = defaultGu(type, target);
        ItemStack stack = WormStackData.create(AddonItems.WORM.get(), type, gu, player.getUUID());
        WormStackData.setCaptureEligible(stack, true);
        if (target instanceof WormEntity worm && worm.getOwnerId() != null) {
            WormStackData.set(stack, worm.getVariant(), worm.getGuPower(), worm.getOwnerId());
        }
        target.discard();
        return stack;
    }

    public static boolean release(ServerLevel level, ServerPlayer player, ItemStack stack, double x, double y, double z) {
        if (stack.isEmpty()) {
            return false;
        }
        WormType type = WormStackData.type(stack);
        int gu = WormStackData.gu(stack);
        UUID owner = player == null ? WormStackData.owner(stack) : player.getUUID();
        WormEntity worm = AddonEntities.WORM.get().create(level);
        if (worm == null) {
            return false;
        }
        worm.setPos(x, y, z);
        worm.setVariant(type);
        worm.setGuPower(gu);
        worm.setOwnerId(owner);
        worm.setYRot(player == null ? level.random.nextFloat() * 360.0F : player.getYRot());
        worm.setXRot(player == null ? 0.0F : player.getXRot());
        if (player != null) {
            worm.setDeltaMovement(player.getLookAngle().scale(0.18D));
        }
        level.addFreshEntity(worm);
        return true;
    }

    @Nullable
    public static WormType typeOf(LivingEntity target) {
        if (target instanceof Silverfish) {
            return WormType.SILVERFISH;
        }
        if (target instanceof Endermite) {
            return WormType.ENDERMITE;
        }
        if (target instanceof Bee bee) {
            return bee.getPersistentData().getBoolean("WormBlade") ? WormType.WINGED : null;
        }
        if (target instanceof WormEntity worm) {
            return worm.getVariant();
        }
        return null;
    }

    private static int defaultGu(WormType type, LivingEntity target) {
        if (target instanceof WormEntity worm) {
            return worm.getGuPower();
        }
        return type.defaultGu();
    }

    public static boolean isControlledForCapture(LivingEntity target) {
        return target != null
                && (target.hasEffect(ModMobEffects.BINDING)
                || target.hasEffect(ModMobEffects.SUGGESTION));
    }
}
