package net.xxxjk.TYPE_MOON_WORLD.magic.jewel;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.cyan.MagicCyanThrow;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.emerald.MagicEmeraldUse;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.emerald.MagicEmeraldWinterRiver;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.ruby.MagicRubyThrow;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.sapphire.MagicSapphireThrow;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.sapphire.MagicSapphireWinterFrost;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.topaz.MagicTopazReinforcement;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.topaz.MagicTopazThrow;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

import java.util.Comparator;
import java.util.List;

public final class GemUseService {
    private static final String ADVANCED_JEWEL_MAGIC_ID = "jewel_magic_release";
    private static final float UPWARD_TRIGGER_PITCH = -65.0F;

    private GemUseService() {
    }

    public static ItemStack useFilledGem(Level level, Player player, InteractionHand hand, ItemStack heldStack, GemType type) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return heldStack;
        }

        GemEngravingService.CastResult engravedCast = GemEngravingService.tryCastEngravedMagic(serverPlayer, hand, heldStack);
        if (engravedCast != GemEngravingService.CastResult.NOT_ENGRAVED) {
            return serverPlayer.getItemInHand(hand);
        }

        if (type == GemType.WHITE_GEMSTONE) {
            useWhiteGem(serverPlayer, hand, heldStack);
            return serverPlayer.getItemInHand(hand);
        }
        if (type == GemType.BLACK_SHARD) {
            useBlackShardGem(serverPlayer, hand, heldStack);
            return serverPlayer.getItemInHand(hand);
        }

        double qualityMultiplier = resolveQualityMultiplier(heldStack);
        boolean upwardRelease = isUpwardRelease(serverPlayer);

        int beforeCount = countFullGemCount(serverPlayer, type);
        castBasicByType(serverPlayer, type);
        int afterCount = countFullGemCount(serverPlayer, type);
        if (afterCount < beforeCount) {
            handleComboAfterThrow(serverPlayer, type, qualityMultiplier, upwardRelease);
        }
        return serverPlayer.getItemInHand(hand);
    }

    private static void castBasicByType(ServerPlayer player, GemType type) {
        switch (type) {
            case RUBY -> MagicRubyThrow.execute(player);
            case SAPPHIRE -> MagicSapphireThrow.execute(player);
            case EMERALD -> MagicEmeraldUse.execute(player);
            case TOPAZ -> MagicTopazThrow.execute(player);
            case CYAN -> MagicCyanThrow.execute(player);
            case WHITE_GEMSTONE -> {
                // handled separately
            }
            case BLACK_SHARD -> {
                // handled separately
            }
        }
    }

    private static void useWhiteGem(ServerPlayer player, InteractionHand hand, ItemStack heldStack) {
        ItemStack projectileStack = heldStack.copy();
        projectileStack.setCount(1);

        RubyProjectileEntity projectile = new RubyProjectileEntity(player.level(), player);
        projectile.setGemType(5); // white
        projectile.setItem(projectileStack);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, MagicConstants.RUBY_THROW_VELOCITY, MagicConstants.RUBY_THROW_INACCURACY);
        player.level().addFreshEntity(projectile);

        heldStack.shrink(1);
        if (heldStack.isEmpty()) {
            player.setItemInHand(hand, ItemStack.EMPTY);
        }
    }

    private static void useBlackShardGem(ServerPlayer player, InteractionHand hand, ItemStack heldStack) {
        ItemStack projectileStack = heldStack.copy();
        projectileStack.setCount(1);

        CompoundTag tag = projectileStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putFloat("ExplosionPowerMultiplier", resolveBlackShardExplosionMultiplier(projectileStack));
        projectileStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        RubyProjectileEntity projectile = new RubyProjectileEntity(player.level(), player);
        projectile.setGemType(6); // black shard
        projectile.setItem(projectileStack);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, MagicConstants.RUBY_THROW_VELOCITY, MagicConstants.RUBY_THROW_INACCURACY);
        player.level().addFreshEntity(projectile);

        heldStack.shrink(1);
        if (heldStack.isEmpty()) {
            player.setItemInHand(hand, ItemStack.EMPTY);
        }
    }

    private static float resolveBlackShardExplosionMultiplier(ItemStack stack) {
        if (!(stack.getItem() instanceof FullManaCarvedGemItem fullGem)) {
            return 1.0F;
        }

        double manaScale = fullGem.getManaAmount(stack) / 100.0D;
        double qualityScale = Math.max(0.1D, fullGem.getQuality().getEffectMultiplier());
        double adjusted = manaScale / qualityScale;
        return (float) Math.max(0.25D, Math.min(4.0D, adjusted));
    }

    private static int countFullGemCount(ServerPlayer player, GemType type) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof FullManaCarvedGemItem fullGemItem && fullGemItem.getType() == type) {
                count += stack.getCount();
            }
        }

        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof FullManaCarvedGemItem fullGemItem && fullGemItem.getType() == type) {
            count += offhand.getCount();
        }
        return count;
    }

    private static void handleComboAfterThrow(ServerPlayer player, GemType type, double qualityMultiplier, boolean upwardRelease) {
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (!vars.learned_magics.contains(ADVANCED_JEWEL_MAGIC_ID)) {
            GemComboTracker.reset(player);
            return;
        }

        GemComboTracker.ComboState state = GemComboTracker.recordUse(player, type, qualityMultiplier, upwardRelease);
        if (!state.triggered()) {
            return;
        }

        float avgMultiplier = (float) Math.max(0.4D, state.averageMultiplier());
        switch (type) {
            case EMERALD -> {
                MagicEmeraldWinterRiver.executeFromCombo(player, avgMultiplier);
            }
            case SAPPHIRE -> {
                MagicSapphireWinterFrost.executeFromCombo(player, avgMultiplier);
            }
            case TOPAZ -> {
                MagicTopazReinforcement.executeFromCombo(player, avgMultiplier);
            }
            case CYAN -> {
                markLatestCyanProjectileAsTornado(player, avgMultiplier);
            }
            case WHITE_GEMSTONE -> {
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0));
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.6, 0.6, 0.6, 0.02);
                }
            }
            case RUBY -> {
                // Ruby has no separate advanced trigger.
            }
            case BLACK_SHARD -> {
                // Black shard has no separate advanced trigger.
            }
        }
    }

    private static boolean markLatestCyanProjectileAsTornado(ServerPlayer player, double avgMultiplier) {
        List<RubyProjectileEntity> projectiles = player.level().getEntitiesOfClass(
                RubyProjectileEntity.class,
                player.getBoundingBox().inflate(48.0D),
                p -> p.isAlive() && p.getOwner() == player && p.getGemType() == 4 && p.tickCount <= 8
        );
        if (projectiles.isEmpty()) {
            return false;
        }

        RubyProjectileEntity target = projectiles.stream()
                .min(Comparator
                        .comparingInt((RubyProjectileEntity p) -> p.tickCount)
                        .thenComparingDouble(p -> p.distanceToSqr(player)))
                .orElse(null);
        if (target == null) {
            return false;
        }

        ItemStack projectileStack = target.getItem().copy();
        if (projectileStack.isEmpty()) {
            return false;
        }

        CompoundTag tag = projectileStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.getBoolean("IsCyanTornado")) {
            return false;
        }

        float radius = (float) (MagicConstants.CYAN_WIND_RADIUS * 1.5D * avgMultiplier);
        int duration = (int) (MagicConstants.CYAN_WIND_DURATION * (1.0D + 0.5D * avgMultiplier));
        tag.putBoolean("IsCyanTornado", true);
        tag.putFloat("CyanRadius", radius);
        tag.putInt("CyanDuration", duration);
        projectileStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        target.setItem(projectileStack);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY(), target.getZ(), 12, 0.4, 0.4, 0.4, 0.02);
        }
        return true;
    }

    private static boolean isUpwardRelease(ServerPlayer player) {
        return player.getXRot() <= UPWARD_TRIGGER_PITCH;
    }

    private static double resolveQualityMultiplier(ItemStack stack) {
        if (stack.getItem() instanceof FullManaCarvedGemItem fullGem) {
            return fullGem.getQuality().getEffectMultiplier();
        }
        return 1.0D;
    }
}
