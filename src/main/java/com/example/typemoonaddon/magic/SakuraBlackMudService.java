package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.registry.AddonBlocks;
import com.example.typemoonaddon.registry.AddonAttachments;
import com.example.typemoonaddon.registry.AddonFluids;
import com.example.typemoonaddon.registry.AddonMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;

public final class SakuraBlackMudService {
    public static final int CORRUPTION_STAGES = 3;

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 5L != 0L) {
            return;
        }
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity living && living.isAlive() && isOnOrInBlackMud(living)) {
                ServerPlayer owner = SakuraSummonBlackMudService.ownerAt(level, living.blockPosition());
                if (owner != null && owner.getUUID().equals(living.getUUID())) {
                    continue;
                }
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 5, false, true, true));
                living.addEffect(new MobEffectInstance(AddonMobEffects.BLACK_MUD_CORRUPTION, 80, 0, false, true, true));
                if (SakuraShadowBindingService.isBound(living)) {
                    living.setDeltaMovement(0.0D, living.getDeltaMovement().y * 0.2D, 0.0D);
                    living.hurtMarked = true;
                }
                if (owner != null) {
                    SakuraPollutionService.expose(living, owner, true);
                }
                if (level.getGameTime() % 20L == 0L && !(living instanceof ServerPlayer player && player.isCreative())) {
                    living.hurt(level.damageSources().magic(), 2.0F);
                }
            }
        }
    }

    public static boolean isOnOrInBlackMud(LivingEntity living) {
        BlockPos feet = living.blockPosition();
        return living.level().getBlockState(feet).is(AddonBlocks.BLACK_MUD.get())
                || living.level().getBlockState(feet.below()).is(AddonBlocks.BLACK_MUD.get());
    }

    public static boolean isImmune(LivingEntity living) {
        return false;
    }

    public static boolean canWalkOnSurface(LivingEntity living, FluidState fluidState) {
        if (living == null || fluidState == null || (!fluidState.is(AddonFluids.BLACK_MUD.get()) && !fluidState.is(AddonFluids.FLOWING_BLACK_MUD.get()))) {
            return false;
        }
        if (living instanceof ServerPlayer player && player.getData(AddonAttachments.IMAGINARY_SPACE.get()).grailWormAscended()) {
            return true;
        }
        return SakuraPollutionService.isFullyCorrupted(living);
    }

    private SakuraBlackMudService() {
    }
}
