package net.xxxjk.TYPE_MOON_WORLD.magic.jewel;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemQuality;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.cyan.MagicCyanThrow;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.emerald.MagicEmeraldUse;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.emerald.MagicEmeraldWinterRiver;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.gravity.GemGravityFieldMagic;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.ruby.MagicRubyThrow;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.sapphire.MagicSapphireThrow;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.sapphire.MagicSapphireWinterFrost;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.topaz.MagicTopazReinforcement;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.topaz.MagicTopazThrow;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

import java.util.Comparator;
import java.util.List;

public final class GemUseService {
    private static final String BASIC_JEWEL_MAGIC_ID = "jewel_magic_shoot";
    private static final String ADVANCED_JEWEL_MAGIC_ID = "jewel_magic_release";

    private GemUseService() {
    }

    public static ItemStack useFilledGem(Level level, Player player, InteractionHand hand, ItemStack heldStack, GemType type) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return heldStack;
        }

        if (!isBasicMagicUnlocked(serverPlayer)) {
            serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.typemoonworld.gem.throw.locked"), true);
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
        if (tryCastAdvancedOnlyWithHighQualityGem(serverPlayer, hand, heldStack, type, qualityMultiplier)) {
            return serverPlayer.getItemInHand(hand);
        }

        int beforeCount = countFullGemCount(serverPlayer, type);
        int beforeHighCount = countFullGemCountByQuality(serverPlayer, type, GemQuality.HIGH);
        castBasicByType(serverPlayer, type);
        int afterCount = countFullGemCount(serverPlayer, type);
        int afterHighCount = countFullGemCountByQuality(serverPlayer, type, GemQuality.HIGH);
        if (afterCount < beforeCount) {
            triggerAdvancedOnHighQuality(serverPlayer, type, qualityMultiplier, afterHighCount < beforeHighCount);
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
        double manaAmount = 100.0D;
        if (heldStack.getItem() instanceof FullManaCarvedGemItem fullGem) {
            manaAmount = Math.max(1.0D, fullGem.getManaAmount(heldStack));
        }
        double powerNormalized = normalizeWhiteGemPower(manaAmount);
        double radius = 6.0D + 8.0D * powerNormalized;
        float baseDamage = (float) (1.0D + 2.0D * powerNormalized);
        double baseHorizontalKnockback = 1.2D + 2.4D * powerNormalized;
        double baseVerticalKnockback = 0.25D + 0.55D * powerNormalized;

        if (player.level() instanceof ServerLevel serverLevel) {
            spawnWhiteShockwaveParticles(serverLevel, player, radius, powerNormalized);
        }

        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius, 3.5D + powerNormalized * 1.5D, radius),
                target -> isWhiteShockwaveTarget(player, target)
        );
        for (LivingEntity target : targets) {
            double dx = target.getX() - player.getX();
            double dz = target.getZ() - player.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance < 1.0E-4D) {
                double randomAngle = player.getRandom().nextDouble() * (Math.PI * 2.0D);
                dx = Math.cos(randomAngle);
                dz = Math.sin(randomAngle);
                distance = 1.0D;
            }

            double nx = dx / distance;
            double nz = dz / distance;
            double falloff = 1.0D - Math.min(1.0D, distance / radius);
            float damage = (float) Math.max(0.5D, baseDamage * (0.5D + 0.5D * falloff));
            target.hurt(player.damageSources().indirectMagic(player, player), damage);

            double horizontal = baseHorizontalKnockback * (0.35D + 0.65D * falloff);
            double vertical = baseVerticalKnockback * (0.45D + 0.55D * falloff);
            target.push(nx * horizontal, vertical, nz * horizontal);
        }

        heldStack.shrink(1);
        if (heldStack.isEmpty()) {
            player.setItemInHand(hand, ItemStack.EMPTY);
        }
    }

    private static boolean isWhiteShockwaveTarget(ServerPlayer player, LivingEntity target) {
        if (!target.isAlive() || target == player) {
            return false;
        }
        if (player.isAlliedTo(target) || target.isAlliedTo(player)) {
            return false;
        }
        if (target instanceof Player other && (other.isSpectator() || other.isCreative())) {
            return false;
        }
        if (target instanceof TamableAnimal tamable && tamable.isOwnedBy(player)) {
            return false;
        }
        return true;
    }

    private static double normalizeWhiteGemPower(double manaAmount) {
        // White gem full mana currently ranges around 50~200. Normalize for consistent scaling.
        double normalized = (manaAmount - 50.0D) / 150.0D;
        return Math.max(0.0D, Math.min(1.0D, normalized));
    }

    private static void spawnWhiteShockwaveParticles(ServerLevel level, ServerPlayer player, double radius, double powerNormalized) {
        double cx = player.getX();
        double cy = player.getY() + 1.0D;
        double cz = player.getZ();
        int burstLevel = (int) Math.round(1.0D + powerNormalized * 2.0D);

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, cx, cy, cz, 2 + burstLevel, 0.25D, 0.25D, 0.25D, 0.0D);
        level.sendParticles(ParticleTypes.FLASH, cx, cy, cz, 10 + 8 * burstLevel, 0.5D, 0.3D, 0.5D, 0.0D);
        level.sendParticles(ParticleTypes.END_ROD, cx, cy + 0.1D, cz, 420 + 260 * burstLevel, radius * 0.42D, 0.9D, radius * 0.42D, 0.25D);
        level.sendParticles(ParticleTypes.CLOUD, cx, cy - 0.2D, cz, 560 + 360 * burstLevel, radius * 0.5D, 0.35D, radius * 0.5D, 0.18D);
        level.sendParticles(ParticleTypes.POOF, cx, cy, cz, 520 + 320 * burstLevel, radius * 0.56D, 0.4D, radius * 0.56D, 0.12D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy, cz, 240 + 160 * burstLevel, radius * 0.45D, 0.55D, radius * 0.45D, 0.22D);
        level.sendParticles(ParticleTypes.WAX_ON, cx, cy + 0.2D, cz, 180 + 120 * burstLevel, radius * 0.52D, 0.7D, radius * 0.52D, 0.09D);

        int rings = 4;
        for (int r = 1; r <= rings; r++) {
            double ringRadius = radius * (r / (double) rings);
            int points = 72 + r * 24;
            double yOffset = 0.08D + (r % 2 == 0 ? 0.18D : 0.03D);

            for (int i = 0; i < points; i++) {
                double angle = (Math.PI * 2.0D * i) / points;
                double px = cx + Math.cos(angle) * ringRadius;
                double pz = cz + Math.sin(angle) * ringRadius;
                level.sendParticles(ParticleTypes.END_ROD, px, cy + yOffset, pz, 2, 0.04D, 0.02D, 0.04D, 0.06D);
                level.sendParticles(ParticleTypes.CLOUD, px, cy + yOffset - 0.08D, pz, 1, 0.03D, 0.01D, 0.03D, 0.02D);
            }
        }
    }

    private static void useBlackShardGem(ServerPlayer player, InteractionHand hand, ItemStack heldStack) {
        if (!GemGravityFieldMagic.throwBlackShardGravityProjectile(player, heldStack)) {
            return;
        }

        heldStack.shrink(1);
        if (heldStack.isEmpty()) {
            player.setItemInHand(hand, ItemStack.EMPTY);
        }
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

    private static int countFullGemCountByQuality(ServerPlayer player, GemType type, GemQuality quality) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof FullManaCarvedGemItem fullGemItem
                    && fullGemItem.getType() == type
                    && fullGemItem.getQuality() == quality) {
                count += stack.getCount();
            }
        }

        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof FullManaCarvedGemItem fullGemItem
                && fullGemItem.getType() == type
                && fullGemItem.getQuality() == quality) {
            count += offhand.getCount();
        }
        return count;
    }

    private static void triggerAdvancedOnHighQuality(ServerPlayer player, GemType type, double qualityMultiplier, boolean highQualityGem) {
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (!vars.learned_magics.contains(ADVANCED_JEWEL_MAGIC_ID) || !highQualityGem) {
            return;
        }

        float avgMultiplier = (float) Math.max(0.4D, qualityMultiplier);
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
                // Ruby keeps previous behavior and does not use high-quality auto advanced.
            }
            case BLACK_SHARD -> {
                // Black shard has no separate advanced trigger.
            }
        }
    }

    private static boolean tryCastAdvancedOnlyWithHighQualityGem(ServerPlayer player, InteractionHand hand, ItemStack heldStack, GemType type, double qualityMultiplier) {
        if (!supportsAdvancedOnly(type) || !isAdvancedMagicUnlocked(player) || !isHighQualityGem(heldStack, type)) {
            return false;
        }

        ItemStack consumedGem = consumeGemFromHand(player, hand, heldStack);
        if (consumedGem.isEmpty()) {
            return false;
        }

        float avgMultiplier = (float) Math.max(0.4D, qualityMultiplier);
        switch (type) {
            case EMERALD -> MagicEmeraldWinterRiver.executeFromCombo(player, avgMultiplier);
            case SAPPHIRE -> MagicSapphireWinterFrost.executeFromCombo(player, avgMultiplier);
            case TOPAZ -> MagicTopazReinforcement.executeFromCombo(player, avgMultiplier);
            case CYAN -> throwCyanTornadoProjectile(player, consumedGem, avgMultiplier);
            default -> {
                return false;
            }
        }
        return true;
    }

    private static boolean supportsAdvancedOnly(GemType type) {
        return type == GemType.EMERALD
                || type == GemType.SAPPHIRE
                || type == GemType.TOPAZ
                || type == GemType.CYAN;
    }

    private static boolean isAdvancedMagicUnlocked(ServerPlayer player) {
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        return vars.learned_magics.contains(ADVANCED_JEWEL_MAGIC_ID);
    }

    private static boolean isBasicMagicUnlocked(ServerPlayer player) {
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        return vars.learned_magics.contains(BASIC_JEWEL_MAGIC_ID);
    }

    private static boolean isHighQualityGem(ItemStack stack, GemType type) {
        if (!(stack.getItem() instanceof FullManaCarvedGemItem fullGemItem)) {
            return false;
        }
        return fullGemItem.getType() == type && fullGemItem.getQuality() == GemQuality.HIGH;
    }

    private static ItemStack consumeGemFromHand(ServerPlayer player, InteractionHand hand, ItemStack heldStack) {
        if (heldStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack consumed = heldStack.copy();
        consumed.setCount(1);
        heldStack.shrink(1);
        if (heldStack.isEmpty()) {
            player.setItemInHand(hand, ItemStack.EMPTY);
        }
        return consumed;
    }

    private static void throwCyanTornadoProjectile(ServerPlayer player, ItemStack consumedGem, float avgMultiplier) {
        ItemStack projectileStack = consumedGem.copy();
        projectileStack.setCount(1);

        CompoundTag tag = projectileStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        float radius = (float) (MagicConstants.CYAN_WIND_RADIUS * 1.5D * avgMultiplier);
        int duration = (int) (MagicConstants.CYAN_WIND_DURATION * (1.0D + 0.5D * avgMultiplier));
        tag.putBoolean("IsCyanTornado", true);
        tag.putFloat("CyanRadius", radius);
        tag.putInt("CyanDuration", duration);
        projectileStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        RubyProjectileEntity projectile = new RubyProjectileEntity(player.level(), player);
        projectile.setGemType(4);
        projectile.setItem(projectileStack);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, MagicConstants.RUBY_THROW_VELOCITY, MagicConstants.RUBY_THROW_INACCURACY);
        player.level().addFreshEntity(projectile);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, projectile.getX(), projectile.getY(), projectile.getZ(), 12, 0.4, 0.4, 0.4, 0.02);
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

    private static double resolveQualityMultiplier(ItemStack stack) {
        if (stack.getItem() instanceof FullManaCarvedGemItem fullGem) {
            return fullGem.getQuality().getEffectMultiplier();
        }
        return 1.0D;
    }

}
