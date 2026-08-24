package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.config.GameplayConfig;
import com.example.typemoonaddon.data.PollutionData;
import com.example.typemoonaddon.network.VoidAbsorptionLinkPayload;
import com.example.typemoonaddon.registry.AddonAttachments;
import com.example.typemoonaddon.registry.AddonMobEffects;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Two-stage incantation and multi-target Imaginary Absorption unlocked by Rule Breaker. */
public final class SakuraForbiddenMagicService {
    private static final int CHANT_TICKS = 40;
    private static final int CONNECTION_TICKS = 40;
    private static final int DAMAGE_INTERVAL_TICKS = 20;
    private static final int AFTEREFFECT_TICKS = 200;
    private static final double REQUIRED_MANA = 900.0D;
    private static final double RANGE = 50.0D;
    private static final double MIN_CONE_DOT = 0.5D;
    private static final Map<UUID, Session> SESSIONS = new LinkedHashMap<>();
    private static final Map<UUID, AuxiliaryTargetLock> AUXILIARY_LOCKS = new LinkedHashMap<>();

    public static boolean cast(ServerPlayer caster) {
        if (!validCaster(caster) || SESSIONS.containsKey(caster.getUUID())) {
            if (caster != null) {
                notify(caster, "message.typemoonworld.forbidden_magic.already_casting");
            }
            return false;
        }
        if (currentMana(caster) < REQUIRED_MANA) {
            notify(caster, "message.typemoonworld.forbidden_magic.insufficient_mana");
            return false;
        }
        long now = caster.serverLevel().getGameTime();
        SESSIONS.put(caster.getUUID(), new Session(caster.getUUID(), caster.level().dimension(), now + CHANT_TICKS));
        notify(caster, "message.typemoonworld.forbidden_magic.chant_first");
        SakuraParticleService.send(
                caster.serverLevel(), caster, ParticleTypes.SQUID_INK,
                caster.getX(), caster.getY() + 1.0D, caster.getZ(),
                18, 0.5D, 0.8D, 0.5D, 0.02D
        );
        caster.serverLevel().playSound(
                null, caster.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 0.8F, 0.55F
        );
        return true;
    }

    public static void tick(MinecraftServer server) {
        Iterator<Session> iterator = SESSIONS.values().iterator();
        while (iterator.hasNext()) {
            Session session = iterator.next();
            ServerPlayer caster = server.getPlayerList().getPlayer(session.casterId);
            if (!validSessionCaster(caster, session)) {
                release(server, session);
                iterator.remove();
                continue;
            }

            long now = caster.serverLevel().getGameTime();
            if (!session.active) {
                tickChant(caster, session, now);
                if (now < session.chantFinishTick) {
                    continue;
                }
                if (!activate(caster, session, now)) {
                    release(server, session);
                    iterator.remove();
                    continue;
                }
            }

            maintainConnections(server, caster, session, now);
            if (now >= session.connectionFinishTick) {
                release(server, session);
                iterator.remove();
                notify(caster, "message.typemoonworld.forbidden_magic.finished");
            }
        }
    }

    public static void playerUnavailable(ServerPlayer player) {
        cancelCasting(player);
    }

    public static void playerChangedDimension(ServerPlayer player) {
        cancelCasting(player);
    }

    public static void serverStopping(MinecraftServer server) {
        for (Session session : SESSIONS.values()) {
            release(server, session);
        }
        SESSIONS.clear();
        AUXILIARY_LOCKS.clear();
    }

    private static void cancelCasting(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session != null) {
            release(player.server, session);
        }
    }

    private static void tickChant(ServerPlayer caster, Session session, long now) {
        if (!session.secondLineShown && now >= session.chantFinishTick - CHANT_TICKS / 2L) {
            session.secondLineShown = true;
            notify(caster, "message.typemoonworld.forbidden_magic.chant_second");
            caster.serverLevel().playSound(
                    null, caster.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.PLAYERS, 0.9F, 0.4F
            );
        }
        if (now % 2L == 0L) {
            SakuraParticleService.send(
                    caster.serverLevel(), caster, ParticleTypes.SQUID_INK,
                    caster.getX(), caster.getY() + 1.0D, caster.getZ(),
                    6, 0.65D, 0.9D, 0.65D, 0.01D
            );
        }
    }

    private static boolean activate(ServerPlayer caster, Session session, long now) {
        if (currentMana(caster) < REQUIRED_MANA) {
            notify(caster, "message.typemoonworld.forbidden_magic.insufficient_mana");
            return false;
        }
        session.active = true;
        session.connectionFinishTick = now + CONNECTION_TICKS;
        session.nextDamageTick = now + DAMAGE_INTERVAL_TICKS;
        drainAllMana(caster);
        caster.addEffect(new MobEffectInstance(MobEffects.CONFUSION, AFTEREFFECT_TICKS, 0, false, true, true));

        Vec3 origin = caster.getEyePosition();
        Vec3 look = caster.getLookAngle().normalize();
        AABB search = new AABB(origin, origin).inflate(RANGE);
        for (LivingEntity target : caster.serverLevel().getEntitiesOfClass(
                LivingEntity.class,
                search,
                candidate -> validTarget(caster, candidate, origin, look)
        )) {
            UUID bindingId = UUID.randomUUID();
            boolean locked = target instanceof EnderDragon || beginAuxiliaryBinding(bindingId, target);
            if (!locked) {
                continue;
            }
            session.targets.put(target.getUUID(), new TargetBinding(bindingId, !(target instanceof EnderDragon)));
            maintainForbiddenAbsorption(caster, target);
        }

        SakuraParticleService.send(
                caster.serverLevel(), caster, ParticleTypes.SQUID_INK,
                caster.getX(), caster.getY() + 1.0D, caster.getZ(),
                80, 2.0D, 1.5D, 2.0D, 0.08D
        );
        caster.serverLevel().playSound(
                null, caster.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK,
                SoundSource.PLAYERS, 1.0F, 0.45F
        );
        notify(caster, "message.typemoonworld.forbidden_magic.activated", session.targets.size());
        return true;
    }

    private static void maintainConnections(MinecraftServer server, ServerPlayer caster, Session session, long now) {
        boolean damagePulse = now >= session.nextDamageTick;
        if (damagePulse) {
            session.nextDamageTick += DAMAGE_INTERVAL_TICKS;
        }
        Iterator<Map.Entry<UUID, TargetBinding>> iterator = session.targets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TargetBinding> entry = iterator.next();
            LivingEntity target = living(server, session.dimension, entry.getKey());
            TargetBinding binding = entry.getValue();
            if (target == null) {
                endAuxiliaryBinding(server, binding.bindingId);
                iterator.remove();
                continue;
            }
            if (binding.locked && !maintainAuxiliaryBinding(binding.bindingId, target)) {
                endAuxiliaryBinding(server, binding.bindingId);
                iterator.remove();
                continue;
            }
            maintainForbiddenAbsorption(caster, target);
            if (damagePulse) {
                pulseForbiddenAbsorption(caster, target);
            }
        }
    }

    private static void release(MinecraftServer server, Session session) {
        for (TargetBinding binding : session.targets.values()) {
            if (binding.locked) {
                endAuxiliaryBinding(server, binding.bindingId);
            }
        }
        session.targets.clear();
    }

    private static boolean beginAuxiliaryBinding(UUID bindingId, LivingEntity target) {
        if (target.getServer() == null) {
            return false;
        }
        AuxiliaryTargetLock lock = AUXILIARY_LOCKS.computeIfAbsent(target.getUUID(), ignored -> new AuxiliaryTargetLock(
                target.getUUID(),
                target.level().dimension(),
                target instanceof Mob mob && mob.isNoAi()
        ));
        lock.bindingIds.add(bindingId);
        return maintainAuxiliaryBinding(bindingId, target);
    }

    private static boolean maintainAuxiliaryBinding(UUID bindingId, LivingEntity target) {
        AuxiliaryTargetLock lock = AUXILIARY_LOCKS.get(target.getUUID());
        if (lock == null || !lock.bindingIds.contains(bindingId)) {
            return false;
        }
        if (target instanceof Mob mob) {
            mob.setNoAi(true);
        }
        target.forceAddEffect(new MobEffectInstance(AddonMobEffects.SHADOW_BINDING_SLOWNESS, 10, 5, false, true, true), null);
        target.setDeltaMovement(target.getDeltaMovement().multiply(0.15D, 0.4D, 0.15D));
        target.hurtMarked = true;
        return true;
    }

    private static void endAuxiliaryBinding(MinecraftServer server, UUID bindingId) {
        Iterator<AuxiliaryTargetLock> iterator = AUXILIARY_LOCKS.values().iterator();
        while (iterator.hasNext()) {
            AuxiliaryTargetLock lock = iterator.next();
            if (!lock.bindingIds.remove(bindingId) || !lock.bindingIds.isEmpty()) {
                continue;
            }
            iterator.remove();
            LivingEntity target = living(server, lock.dimension, lock.targetId);
            if (target != null) {
                restoreAuxiliaryTarget(target, lock.targetWasNoAi);
            }
        }
    }

    private static void restoreAuxiliaryTarget(LivingEntity target, boolean wasNoAi) {
        target.removeEffect(AddonMobEffects.SHADOW_BINDING_SLOWNESS);
        if (target instanceof Mob mob) {
            mob.setNoAi(SakuraShadowBindingService.isBound(target) || wasNoAi);
        }
    }

    private static void maintainForbiddenAbsorption(ServerPlayer caster, LivingEntity target) {
        if (!(target instanceof EnderDragon)) {
            target.addEffect(new MobEffectInstance(AddonMobEffects.BANISHMENT, 25, 0, false, true, true), caster);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 2, false, true, true), caster);
            SakuraPollutionService.expose(target, caster, true);
        }
        syncVoidAbsorptionLink(caster, target);
    }

    private static void pulseForbiddenAbsorption(ServerPlayer caster, LivingEntity target) {
        float damage = GameplayConfig.SHADOW_DAMAGE_PER_SECOND;
        if (!(target instanceof EnderDragon) && SakuraShadowBindingService.absorbDirectDamage(target, caster, damage)) {
            return;
        }
        target.hurt(caster.damageSources().indirectMagic(caster, caster), damage);
    }

    private static void syncVoidAbsorptionLink(Entity source, LivingEntity target) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                source,
                new VoidAbsorptionLinkPayload(source.getId(), target.getId(), 3, SakuraParticleService.palette(source))
        );
    }

    private static boolean validCaster(ServerPlayer caster) {
        return caster != null
                && caster.isAlive()
                && !caster.isSpectator()
                && SakuraTypeMoonIntegration.isForbiddenMagicLearned(caster);
    }

    private static boolean validSessionCaster(ServerPlayer caster, Session session) {
        return validCaster(caster) && caster.level().dimension().equals(session.dimension);
    }

    private static boolean validTarget(ServerPlayer caster, LivingEntity target, Vec3 origin, Vec3 look) {
        if (target == caster
                || !target.isAlive()
                || target.isRemoved()
                || areShadowFactionAllies(caster, target)) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        Vec3 offset = target.getBoundingBox().getCenter().subtract(origin);
        double distanceSqr = offset.lengthSqr();
        return distanceSqr <= RANGE * RANGE
                && distanceSqr > 1.0E-8D
                && look.dot(offset.normalize()) >= MIN_CONE_DOT;
    }

    private static boolean areShadowFactionAllies(ServerPlayer caster, LivingEntity target) {
        if (target.isAlliedTo(caster) || caster.isAlliedTo(target)) {
            return true;
        }
        PollutionData pollution = target.getExistingDataOrNull(AddonAttachments.POLLUTION.get());
        return pollution != null && caster.getUUID().equals(pollution.controllerId());
    }

    private static LivingEntity living(MinecraftServer server, ResourceKey<Level> dimension, UUID targetId) {
        ServerLevel level = server.getLevel(dimension);
        Entity entity = level == null ? null : level.getEntity(targetId);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private static double currentMana(ServerPlayer player) {
        return SakuraTypeMoonIntegration.registry().mana(player).current();
    }

    private static void drainAllMana(ServerPlayer player) {
        double current = currentMana(player);
        if (current > 0.0D) {
            SakuraTypeMoonIntegration.registry().mana(player).tryConsume(current);
        }
    }

    private static void notify(ServerPlayer player, String key, Object... arguments) {
        player.displayClientMessage(Component.translatable(key, arguments), true);
    }

    private static final class Session {
        private final UUID casterId;
        private final ResourceKey<Level> dimension;
        private final long chantFinishTick;
        private final Map<UUID, TargetBinding> targets = new LinkedHashMap<>();
        private boolean secondLineShown;
        private boolean active;
        private long connectionFinishTick;
        private long nextDamageTick;

        private Session(UUID casterId, ResourceKey<Level> dimension, long chantFinishTick) {
            this.casterId = casterId;
            this.dimension = dimension;
            this.chantFinishTick = chantFinishTick;
        }
    }

    private static final class AuxiliaryTargetLock {
        private final UUID targetId;
        private final ResourceKey<Level> dimension;
        private final boolean targetWasNoAi;
        private final Set<UUID> bindingIds = new HashSet<>();

        private AuxiliaryTargetLock(UUID targetId, ResourceKey<Level> dimension, boolean targetWasNoAi) {
            this.targetId = targetId;
            this.dimension = dimension;
            this.targetWasNoAi = targetWasNoAi;
        }
    }

    private record TargetBinding(UUID bindingId, boolean locked) {
    }

    private SakuraForbiddenMagicService() {
    }
}
