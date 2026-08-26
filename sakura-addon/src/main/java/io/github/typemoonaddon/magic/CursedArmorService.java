package io.github.typemoonaddon.magic;

import io.github.typemoonaddon.compat.TypeMoonServantBridge;
import io.github.typemoonaddon.config.GameplayConfig;
import io.github.typemoonaddon.data.ImaginarySpaceData.CursedArmorState;
import io.github.typemoonaddon.registry.ModAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

/** Server-authoritative lifecycle and damage rules for the Grail's cursed armor. */
public final class CursedArmorService {
    public static boolean beginFormation(ServerPlayer player) {
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        long now = player.server.overworld().getGameTime();
        if (!data.grailErosionFull() || !data.beginCursedArmorFormation(now)) {
            return false;
        }
        clearArmorSlots(player);
        sync(player);
        player.serverLevel().playSound(
            null,
            player.blockPosition(),
            SoundEvents.SCULK_SHRIEKER_SHRIEK,
            SoundSource.PLAYERS,
            0.8F,
            0.55F
        );
        return true;
    }

    public static boolean beginDissolution(ServerPlayer player) {
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        if (!data.beginCursedArmorDissolution(player.server.overworld().getGameTime())) {
            return false;
        }
        sync(player);
        player.serverLevel().playSound(
            null,
            player.blockPosition(),
            SoundEvents.FIRE_EXTINGUISH,
            SoundSource.PLAYERS,
            0.9F,
            0.65F
        );
        return true;
    }

    public static void tick(ServerPlayer player) {
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        if (!data.cursedArmorPresent()) {
            return;
        }
        clearArmorSlots(player);
        long now = player.server.overworld().getGameTime();
        long elapsed = Math.max(0L, now - data.cursedArmorStageStartTick());
        boolean changed = false;
        if (data.cursedArmorState() == CursedArmorState.FORMING) {
            spawnFormationParticles(player, elapsed);
            if (elapsed >= GameplayConfig.CURSED_ARMOR_TRANSITION_TICKS) {
                changed = data.activateCursedArmor();
            }
        } else if (data.cursedArmorState() == CursedArmorState.DISSOLVING) {
            spawnDissolutionParticles(player, elapsed);
            if (elapsed >= GameplayConfig.CURSED_ARMOR_TRANSITION_TICKS) {
                changed = data.finishCursedArmorDissolution();
            }
        }
        if (changed) {
            sync(player);
        }
    }

    public static void sync(ServerPlayer player) {
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        player.getData(ModAttachments.CURSED_ARMOR_VIEW.get()).set(
            data.cursedArmorState(),
            data.cursedArmorStageStartTick()
        );
        player.syncData(ModAttachments.IMAGINARY_SPACE.get());
        player.syncData(ModAttachments.CURSED_ARMOR_VIEW.get());
    }

    public static float reduceDamage(ServerPlayer player, DamageSource source, float amount) {
        enforceArmorSlots(player);
        if (amount <= 0.0F || source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return amount;
        }
        float visibility = protectionProgress(player);
        if (visibility <= 0.0F) {
            return amount;
        }
        LivingEntity attacker = rootLivingAttacker(source);
        float multiplier = attacker != null && TypeMoonServantBridge.isServantOrCardUser(attacker)
            ? GameplayConfig.CURSED_ARMOR_SERVANT_MULTIPLIER
            : 1.0F;
        float effectiveArmor = GameplayConfig.CURSED_ARMOR_POINTS * multiplier * visibility;
        return CombatRules.getDamageAfterAbsorb(player, amount, source, effectiveArmor, 0.0F);
    }

    public static void enforceArmorSlots(ServerPlayer player) {
        if (player.getData(ModAttachments.IMAGINARY_SPACE.get()).cursedArmorPresent()) {
            clearArmorSlots(player);
        }
    }

    public static float protectionProgress(ServerPlayer player) {
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        long elapsed = Math.max(0L, player.level().getGameTime() - data.cursedArmorStageStartTick());
        float transition = Mth.clamp(
            elapsed / (float)GameplayConfig.CURSED_ARMOR_TRANSITION_TICKS,
            0.0F,
            1.0F
        );
        return switch (data.cursedArmorState()) {
            case FORMING -> transition;
            case ACTIVE -> 1.0F;
            case DISSOLVING -> 1.0F - transition;
            default -> 0.0F;
        };
    }

    private static void clearArmorSlots(ServerPlayer player) {
        player.setItemSlot(EquipmentSlot.HEAD, net.minecraft.world.item.ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.CHEST, net.minecraft.world.item.ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.LEGS, net.minecraft.world.item.ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.FEET, net.minecraft.world.item.ItemStack.EMPTY);
    }

    private static LivingEntity rootLivingAttacker(DamageSource source) {
        Entity sourceEntity = source.getEntity();
        if (sourceEntity instanceof LivingEntity living) {
            return living;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner) {
            return owner;
        }
        return direct instanceof LivingEntity living ? living : null;
    }

    private static void spawnFormationParticles(ServerPlayer player, long elapsed) {
        if (elapsed % 2L != 0L) {
            return;
        }
        ServerLevel level = player.serverLevel();
        float progress = Mth.clamp(elapsed / (float)GameplayConfig.CURSED_ARMOR_TRANSITION_TICKS, 0.0F, 1.0F);
        double y = player.getY() + player.getBbHeight() * (1.0D - progress);
        level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), y, player.getZ(), 7, 0.32D, 0.16D, 0.32D, 0.015D);
        if (elapsed % 6L == 0L) {
            level.sendParticles(ParticleTypes.DRIPPING_OBSIDIAN_TEAR, player.getX(), y + 0.2D, player.getZ(), 3, 0.28D, 0.08D, 0.28D, 0.0D);
        }
    }

    private static void spawnDissolutionParticles(ServerPlayer player, long elapsed) {
        if (elapsed % 3L != 0L) {
            return;
        }
        ServerLevel level = player.serverLevel();
        double y = player.getY() + level.random.nextDouble() * player.getBbHeight();
        level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), y, player.getZ(), 3, 0.4D, 0.12D, 0.4D, 0.02D);
        level.sendParticles(ParticleTypes.CRIMSON_SPORE, player.getX(), y, player.getZ(), 2, 0.3D, 0.1D, 0.3D, 0.005D);
    }

    private CursedArmorService() {
    }
}
