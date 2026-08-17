package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.data.ImaginarySpaceData;
import com.example.typemoonaddon.entity.SakuraBlackShadowEntity;
import com.example.typemoonaddon.entity.SakuraShadowArtRibbonEntity;
import com.example.typemoonaddon.registry.AddonAttachments;
import com.example.typemoonaddon.registry.AddonEntities;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class SakuraShadowArtService {
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, TypeMoonAddon.id("shadow_art"));
    private static final int RIBBON_COUNT = 10;
    private static final double ROOT_RADIUS = 1.15D;
    private static final double IDLE_RADIUS = 2.6D;
    private static final double MAX_LENGTH = 24.0D;
    private static final double ATTACK_RADIUS = 18.0D;
    private static final double PROJECTILE_SCAN_RADIUS = 12.0D;
    private static final double PROJECTILE_HIT_RADIUS_SQR = 2.0D * 2.0D;
    private static final int ATTACK_COOLDOWN_TICKS = 20;
    private static final int STRIKE_TICKS = 6;
    private static final int RETRACT_TICKS = 12;
    private static final int TARGET_SCAN_TICKS = 10;
    private static final int HOLD_RIBBON_COUNT = 3;
    private static final int LIFT_TICKS = 16;
    private static final float ATTACK_DAMAGE = 8.0F;
    private static final double MANA_PER_RIBBON_SECOND = 1.0D;
    private static final DustParticleOptions SHADOW_RED_DUST = new DustParticleOptions(new Vector3f(0.48F, 0.015F, 0.025F), 0.75F);
    private static final Map<HoldKey, Integer> TARGET_HOLD_TICKS = new HashMap<>();
    private static final Map<HoldKey, Vec3> TARGET_LIFT_ORIGINS = new HashMap<>();
    private static final Map<UUID, Long> CANCEL_CONFIRM_UNTIL = new HashMap<>();

    public static boolean cast(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.isSpectator() || !SakuraTypeMoonIntegration.isShadowArtLearned(player)) {
            return false;
        }
        ImaginarySpaceData data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        if (!data.shadowArtUnlocked()) {
            data.unlockShadowArt();
        }
        if (data.shadowArtActive()) {
            long now = player.serverLevel().getGameTime();
            if (CANCEL_CONFIRM_UNTIL.getOrDefault(player.getUUID(), 0L) >= now) {
                CANCEL_CONFIRM_UNTIL.remove(player.getUUID());
                return deactivate(player, false);
            }
            CANCEL_CONFIRM_UNTIL.put(player.getUUID(), now + 100L);
            player.displayClientMessage(Component.translatable("message.typemoonworld.shadow_art.confirm_cancel"), true);
            return true;
        }
        if (!data.activateShadowArt()) {
            return false;
        }
        for (int index = 0; index < RIBBON_COUNT; index++) {
            if (data.shadowArtRespawnTicks(index) == 0) {
                spawnRibbon(player, index, SakuraShadowArtRibbonEntity.REGROW);
            }
        }
        data.finishShadowArtActivation();
        AddonAttachments.sync(player, AddonAttachments.IMAGINARY_SPACE);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 1.0F, 0.55F);
        return true;
    }

    public static boolean selectMode(ServerPlayer player, ImaginarySpaceData.ShadowArtMode mode) {
        if (player == null || !SakuraTypeMoonIntegration.isShadowArtLearned(player)) {
            return false;
        }
        ImaginarySpaceData data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        if (!data.shadowArtUnlocked()) {
            data.unlockShadowArt();
        }
        data.setShadowArtMode(mode);
        AddonAttachments.sync(player, AddonAttachments.IMAGINARY_SPACE);
        player.displayClientMessage(Component.translatable("message.typemoonworld.shadow_art.mode." + data.shadowArtMode().serializedName()), true);
        return true;
    }

    public static boolean deactivate(ServerPlayer player, boolean dispelled) {
        ImaginarySpaceData data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        boolean changed = dispelled ? data.dispelShadowArt() : data.deactivateShadowArt();
        if (!changed && !dispelled) {
            return false;
        }
        for (SakuraShadowArtRibbonEntity ribbon : ribbons(player)) {
            ribbon.setAction(SakuraShadowArtRibbonEntity.RETRACT, null);
        }
        if (dispelled) {
            ribbons(player).forEach(Entity::discard);
        }
        CANCEL_CONFIRM_UNTIL.remove(player.getUUID());
        clearOwnerState(player.getUUID());
        AddonAttachments.sync(player, AddonAttachments.IMAGINARY_SPACE);
        return true;
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickPlayer(player);
        }
    }

    private static void tickPlayer(ServerPlayer player) {
        ImaginarySpaceData data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        if (!data.shadowArtActive()) {
            return;
        }
        if (!player.isAlive() || player.isSpectator() || !SakuraTypeMoonIntegration.isShadowArtLearned(player)) {
            deactivate(player, false);
            return;
        }
        List<SakuraShadowArtRibbonEntity> owned = ribbons(player);
        if (data.shadowArtState() == ImaginarySpaceData.ShadowArtState.DEACTIVATING) {
            if (owned.isEmpty()) {
                data.finishShadowArtDeactivation();
                AddonAttachments.sync(player, AddonAttachments.IMAGINARY_SPACE);
            }
            return;
        }
        boolean[] present = new boolean[RIBBON_COUNT];
        for (SakuraShadowArtRibbonEntity ribbon : owned) {
            int index = Math.clamp(ribbon.ribbonIndex(), 0, RIBBON_COUNT - 1);
            if (present[index]) {
                ribbon.discard();
            } else {
                present[index] = true;
            }
        }
        for (int index = 0; index < present.length; index++) {
            if (present[index]) {
                continue;
            }
            if (data.tickShadowArtRibbonRecovery(index) || data.shadowArtRespawnTicks(index) == 0) {
                spawnRibbon(player, index, SakuraShadowArtRibbonEntity.REGROW);
            }
        }
        if (player.tickCount % 20 == 0) {
            double cost = owned.size() * MANA_PER_RIBBON_SECOND;
            if (cost > 0.0D && !SakuraTypeMoonIntegration.tryConsumeMana(player, cost)) {
                deactivate(player, false);
                return;
            }
        }
        if (player.tickCount % 4 == 0) {
            spawnShadowCircleParticles(player);
        }
        if (defenseAllowed(data.shadowArtMode()) && player.tickCount % 2 == 0) {
            interceptThreat(player, owned);
        }
        if (attackAllowed(data.shadowArtMode()) && player.tickCount % TARGET_SCAN_TICKS == 0) {
            assignAttack(player, owned);
        }
        maintainPiercedTargets(player, owned);
    }

    public static void tickRibbon(SakuraShadowArtRibbonEntity ribbon) {
        LivingEntity owner = owner(ribbon);
        if (owner == null || owner instanceof ServerPlayer player && !player.getData(AddonAttachments.IMAGINARY_SPACE.get()).shadowArtActive()) {
            ribbon.discard();
            return;
        }
        int index = Math.clamp(ribbon.ribbonIndex(), 0, RIBBON_COUNT - 1);
        double angle = Math.PI * 2.0D * index / RIBBON_COUNT;
        Vec3 root = owner.position().add(Math.cos(angle) * ROOT_RADIUS, 0.12D, Math.sin(angle) * ROOT_RADIUS);
        if (ribbon.action() == SakuraShadowArtRibbonEntity.RETRACT) {
            ribbon.setPos(ribbon.position().lerp(root, 0.42D));
            if (ribbon.tickCount - ribbon.actionStart() >= RETRACT_TICKS) {
                ribbon.discard();
            }
            return;
        }
        Entity rawTarget = ribbon.targetEntityId() < 0 ? null : ribbon.level().getEntity(ribbon.targetEntityId());
        if (isTargetAction(ribbon.action()) && rawTarget instanceof LivingEntity target && legalRibbonTarget(owner, target, ribbon.action())) {
            Vec3 endpoint = piercePoint(target, index);
            Vec3 delta = endpoint.subtract(root);
            if (delta.lengthSqr() > square(MAX_LENGTH)) {
                ribbon.setAction(SakuraShadowArtRibbonEntity.IDLE, null);
                return;
            }
            double progress = ribbon.action() == SakuraShadowArtRibbonEntity.ATTACK ? smoothStep(Math.clamp((ribbon.tickCount - ribbon.actionStart()) / (double) STRIKE_TICKS, 0.0D, 1.0D)) : 1.0D;
            ribbon.setPos(root.add(delta.scale(progress)));
            if (progress >= 1.0D && ribbon.action() == SakuraShadowArtRibbonEntity.ATTACK) {
                boolean hit = target.hurt(shadowArtDamage(owner, ribbon), ATTACK_DAMAGE);
                ribbon.setAction(hit ? SakuraShadowArtRibbonEntity.PIERCED : SakuraShadowArtRibbonEntity.IDLE, hit ? target : null);
            }
            return;
        }
        double time = owner.tickCount;
        double seed = index * 1.731D;
        double radius = IDLE_RADIUS + index % 4 * 0.58D;
        double x = Math.sin(time * (0.021D + index * 0.0007D) + seed) * radius + Math.sin(time * 0.009D + seed * 2.3D) * 0.75D;
        double z = Math.sin(time * (0.017D + index * 0.0005D) + seed * 1.41D) * radius + Math.cos(time * 0.011D + seed * 0.77D) * 0.65D;
        double y = 1.15D + Math.sin(time * 0.026D + seed * 1.8D) * 0.8D + Math.sin(time * 0.007D + seed) * 0.35D;
        ribbon.setPos(root.add(x, y, z));
        if (ribbon.action() != SakuraShadowArtRibbonEntity.REGROW || ribbon.tickCount - ribbon.actionStart() > 12) {
            ribbon.setAction(SakuraShadowArtRibbonEntity.IDLE, null);
        }
    }

    public static void ribbonBroken(SakuraShadowArtRibbonEntity ribbon) {
        LivingEntity owner = owner(ribbon);
        if (owner instanceof ServerPlayer player) {
            player.getData(AddonAttachments.IMAGINARY_SPACE.get()).breakShadowArtRibbon(ribbon.ribbonIndex());
            AddonAttachments.sync(player, AddonAttachments.IMAGINARY_SPACE);
        } else if (owner instanceof SakuraBlackShadowEntity shadow) {
            shadow.breakShadowArtRibbon(ribbon.ribbonIndex());
        }
        if (ribbon.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SQUID_INK, ribbon.getX(), ribbon.getY(), ribbon.getZ(), 10, 0.2D, 0.2D, 0.2D, 0.02D);
        }
        ribbon.discard();
    }

    private static void assignAttack(ServerPlayer owner, List<SakuraShadowArtRibbonEntity> ribbons) {
        int limit = owner.getData(AddonAttachments.IMAGINARY_SPACE.get()).shadowArtMode() == ImaginarySpaceData.ShadowArtMode.BALANCED ? 5 : 10;
        List<SakuraShadowArtRibbonEntity> available = ribbons.stream()
                .filter(ribbon -> ribbon.ribbonIndex() < limit && ribbon.action() == SakuraShadowArtRibbonEntity.IDLE && ribbon.tickCount - ribbon.actionStart() >= ATTACK_COOLDOWN_TICKS)
                .sorted(Comparator.comparingInt(SakuraShadowArtRibbonEntity::ribbonIndex))
                .toList();
        if (available.isEmpty()) {
            return;
        }
        List<LivingEntity> targets = findTargets(owner, Math.max(1, available.size() / 2));
        if (targets.isEmpty()) {
            return;
        }
        int pairedRibbonCount = Math.min(available.size(), targets.size() * 2);
        for (int index = 0; index < available.size(); index++) {
            int targetIndex = index < pairedRibbonCount ? index / 2 : index % targets.size();
            available.get(index).setAction(SakuraShadowArtRibbonEntity.ATTACK, targets.get(targetIndex));
        }
    }

    private static List<LivingEntity> findTargets(ServerPlayer owner, int limit) {
        LinkedHashSet<LivingEntity> ordered = new LinkedHashSet<>();
        LivingEntity retaliation = owner.getLastHurtByMob();
        if (legalTarget(owner, retaliation) && owner.distanceToSqr(retaliation) <= square(ATTACK_RADIUS)) {
            ordered.add(retaliation);
        }
        List<LivingEntity> candidates = owner.serverLevel().getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(ATTACK_RADIUS), target -> legalTarget(owner, target) && owner.distanceToSqr(target) <= square(ATTACK_RADIUS));
        candidates.stream().filter(target -> target instanceof Mob mob && mob.getTarget() == owner).sorted(Comparator.comparingDouble(owner::distanceToSqr)).forEach(ordered::add);
        LivingEntity attacked = owner.getLastHurtMob();
        if (legalTarget(owner, attacked) && owner.distanceToSqr(attacked) <= square(ATTACK_RADIUS)) {
            ordered.add(attacked);
        }
        candidates.stream().sorted(Comparator.comparingDouble(owner::distanceToSqr)).forEach(ordered::add);
        return ordered.stream().limit(limit).toList();
    }

    private static void interceptThreat(ServerPlayer owner, List<SakuraShadowArtRibbonEntity> ribbons) {
        int firstDefense = owner.getData(AddonAttachments.IMAGINARY_SPACE.get()).shadowArtMode() == ImaginarySpaceData.ShadowArtMode.BALANCED ? 5 : 0;
        SakuraShadowArtRibbonEntity defender = ribbons.stream().filter(ribbon -> ribbon.ribbonIndex() >= firstDefense && !isHoldingAction(ribbon.action())).findFirst().orElse(null);
        if (defender == null) {
            return;
        }
        for (Projectile projectile : owner.serverLevel().getEntitiesOfClass(Projectile.class, owner.getBoundingBox().inflate(PROJECTILE_SCAN_RADIUS), projectile -> projectile.isAlive() && projectile.getOwner() != owner)) {
            Vec3 velocity = projectile.getDeltaMovement();
            if (velocity.lengthSqr() < 0.0025D) {
                continue;
            }
            Vec3 toOwner = owner.getEyePosition().subtract(projectile.position());
            double along = toOwner.dot(velocity) / velocity.lengthSqr();
            if (along < 0.0D || along > PROJECTILE_SCAN_RADIUS || projectile.position().add(velocity.scale(along)).distanceToSqr(owner.getEyePosition()) > PROJECTILE_HIT_RADIUS_SQR) {
                continue;
            }
            defender.setAction(SakuraShadowArtRibbonEntity.DEFEND, projectile);
            defender.setPos(projectile.position());
            projectile.discard();
            return;
        }
    }

    private static void maintainPiercedTargets(LivingEntity owner, List<SakuraShadowArtRibbonEntity> ribbons) {
        Map<LivingEntity, Integer> counts = new HashMap<>();
        for (SakuraShadowArtRibbonEntity ribbon : ribbons) {
            if (isHoldingAction(ribbon.action()) && ribbon.targetEntityId() >= 0 && ribbon.level().getEntity(ribbon.targetEntityId()) instanceof LivingEntity target && legalRibbonTarget(owner, target, ribbon.action())) {
                counts.merge(target, 1, Integer::sum);
            }
        }
        long now = owner.level().getGameTime();
        for (Map.Entry<LivingEntity, Integer> entry : counts.entrySet()) {
            LivingEntity target = entry.getKey();
            if (entry.getValue() < HOLD_RIBBON_COUNT) {
                continue;
            }
            HoldKey key = new HoldKey(owner.getUUID(), target.getUUID());
            int ticks = TARGET_HOLD_TICKS.merge(key, 1, Integer::sum);
            Vec3 origin = TARGET_LIFT_ORIGINS.computeIfAbsent(key, ignored -> target.position());
            double progress = smoothStep(Math.clamp(ticks / (double) LIFT_TICKS, 0.0D, 1.0D));
            Vec3 destination = origin.add(0.0D, 2.4D, 0.0D);
            target.teleportTo(origin.lerp(destination, progress).x, origin.lerp(destination, progress).y, origin.lerp(destination, progress).z);
            target.setDeltaMovement(Vec3.ZERO);
            target.fallDistance = 0.0F;
            target.hurtMarked = true;
            for (SakuraShadowArtRibbonEntity ribbon : ribbons) {
                if (ribbon.targetEntityId() == target.getId()) {
                    ribbon.setAction(ticks >= LIFT_TICKS ? SakuraShadowArtRibbonEntity.PINNED : SakuraShadowArtRibbonEntity.LIFT, target);
                }
            }
        }
    }

    private static void spawnShadowCircleParticles(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return;
        }
        for (int index = 0; index < 6; index++) {
            Vec3 position = randomShadowCirclePosition(owner);
            level.sendParticles(ParticleTypes.SQUID_INK, position.x, position.y, position.z, 1, 0.02D, 0.025D, 0.02D, 0.008D);
        }
        for (int index = 0; index < 2; index++) {
            Vec3 position = randomShadowCirclePosition(owner);
            level.sendParticles(SHADOW_RED_DUST, position.x, position.y, position.z, 1, 0.015D, 0.02D, 0.015D, 0.0D);
        }
    }

    private static Vec3 randomShadowCirclePosition(LivingEntity owner) {
        double radius = Math.sqrt(owner.getRandom().nextDouble()) * IDLE_RADIUS;
        double angle = owner.getRandom().nextDouble() * Math.PI * 2.0D;
        return owner.position().add(Math.cos(angle) * radius, 0.08D, Math.sin(angle) * radius);
    }

    private static void spawnRibbon(LivingEntity owner, int index, byte action) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return;
        }
        SakuraShadowArtRibbonEntity ribbon = AddonEntities.SHADOW_ART_RIBBON.get().create(level);
        if (ribbon == null) {
            return;
        }
        ribbon.initialize(owner, index);
        ribbon.setPos(owner.position());
        ribbon.setAction(action, null);
        level.addFreshEntity(ribbon);
    }

    private static List<SakuraShadowArtRibbonEntity> ribbons(LivingEntity owner) {
        return ((ServerLevel) owner.level()).getEntitiesOfClass(SakuraShadowArtRibbonEntity.class, new AABB(owner.blockPosition()).inflate(MAX_LENGTH + 8.0D), ribbon -> owner.getUUID().equals(ribbon.ownerId()));
    }

    @Nullable
    private static LivingEntity owner(SakuraShadowArtRibbonEntity ribbon) {
        UUID id = ribbon.ownerId();
        if (id == null || !(ribbon.level() instanceof ServerLevel level)) {
            return null;
        }
        Entity entity = level.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private static boolean legalTarget(ServerPlayer owner, @Nullable LivingEntity target) {
        return target != null && target.level() == owner.level() && SakuraMagicTargeting.validHostileTarget(owner, target);
    }

    private static boolean legalRibbonTarget(LivingEntity owner, @Nullable LivingEntity target, byte action) {
        double maximumLength = action == SakuraShadowArtRibbonEntity.ATTACK ? ATTACK_RADIUS : MAX_LENGTH;
        return target != null && target.level() == owner.level() && SakuraMagicTargeting.validHostileTarget(owner, target) && owner.distanceToSqr(target) <= square(maximumLength);
    }

    private static Vec3 piercePoint(LivingEntity target, int ribbonIndex) {
        double width = Math.max(0.25D, target.getBbWidth() * 0.42D);
        double angle = Math.PI * 2.0D * ribbonIndex / RIBBON_COUNT + (ribbonIndex % 2) * 0.31D;
        double yFraction = switch (ribbonIndex % 5) {
            case 0 -> 0.78D;
            case 1 -> 0.64D;
            case 2 -> 0.52D;
            case 3 -> 0.38D;
            default -> 0.24D;
        };
        return target.position().add(Math.cos(angle) * width, target.getBbHeight() * yFraction, Math.sin(angle) * width);
    }

    private static boolean isTargetAction(byte action) {
        return action == SakuraShadowArtRibbonEntity.ATTACK || isHoldingAction(action);
    }

    public static boolean isHoldingAction(byte action) {
        return action == SakuraShadowArtRibbonEntity.PIERCED || action == SakuraShadowArtRibbonEntity.LIFT || action == SakuraShadowArtRibbonEntity.THROW || action == SakuraShadowArtRibbonEntity.PINNED;
    }

    private static boolean attackAllowed(ImaginarySpaceData.ShadowArtMode mode) {
        return mode != ImaginarySpaceData.ShadowArtMode.AUTO_DEFENSE;
    }

    private static boolean defenseAllowed(ImaginarySpaceData.ShadowArtMode mode) {
        return mode != ImaginarySpaceData.ShadowArtMode.AUTO_ATTACK;
    }

    private static double smoothStep(double value) {
        double clamped = Math.clamp(value, 0.0D, 1.0D);
        return clamped * clamped * (3.0D - 2.0D * clamped);
    }

    private static double square(double value) {
        return value * value;
    }

    private static DamageSource shadowArtDamage(LivingEntity owner, SakuraShadowArtRibbonEntity ribbon) {
        ServerLevel level = (ServerLevel) owner.level();
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE_TYPE), ribbon, owner);
    }

    public static void playerUnavailable(ServerPlayer player) {
        CANCEL_CONFIRM_UNTIL.remove(player.getUUID());
        if (player.getData(AddonAttachments.IMAGINARY_SPACE.get()).shadowArtActive()) {
            deactivate(player, false);
            ribbons(player).forEach(Entity::discard);
            player.getData(AddonAttachments.IMAGINARY_SPACE.get()).finishShadowArtDeactivation();
            AddonAttachments.sync(player, AddonAttachments.IMAGINARY_SPACE);
        }
    }

    public static void serverStopping() {
        TARGET_HOLD_TICKS.clear();
        TARGET_LIFT_ORIGINS.clear();
        CANCEL_CONFIRM_UNTIL.clear();
    }

    private static void clearOwnerState(UUID ownerId) {
        Set<HoldKey> owned = new HashSet<>();
        TARGET_HOLD_TICKS.keySet().stream().filter(key -> key.ownerId().equals(ownerId)).forEach(owned::add);
        owned.forEach(key -> {
            TARGET_HOLD_TICKS.remove(key);
            TARGET_LIFT_ORIGINS.remove(key);
        });
    }

    private record HoldKey(UUID ownerId, UUID targetId) {
    }

    private SakuraShadowArtService() {
    }
}
