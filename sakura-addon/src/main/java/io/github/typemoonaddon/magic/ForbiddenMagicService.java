package io.github.typemoonaddon.magic;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Two-stage incantation and multi-target Imaginary Absorption unlocked by Rule Breaker. */
public final class ForbiddenMagicService {
    private static final int CHANT_TICKS = 40;
    private static final int CONNECTION_TICKS = 40;
    private static final int DAMAGE_INTERVAL_TICKS = 20;
    private static final int AFTEREFFECT_TICKS = 200;
    private static final double REQUIRED_MANA = 900.0D;
    private static final double RANGE = 50.0D;
    private static final double MIN_CONE_DOT = 0.5D;
    private static final Map<UUID, Session> SESSIONS = new LinkedHashMap<>();

    public static boolean cast(ServerPlayer caster) {
        if (!validCaster(caster) || SESSIONS.containsKey(caster.getUUID())) {
            if (caster != null) {
                notify(caster, "message.typemoonaddon.forbidden_magic.already_casting");
            }
            return false;
        }
        if (TypeMoonIntegration.currentMana(caster) < REQUIRED_MANA) {
            notify(caster, "message.typemoonaddon.forbidden_magic.insufficient_mana");
            return false;
        }
        long now = caster.serverLevel().getGameTime();
        SESSIONS.put(caster.getUUID(), new Session(
            caster.getUUID(),
            caster.level().dimension(),
            now + CHANT_TICKS
        ));
        notify(caster, "message.typemoonaddon.forbidden_magic.chant_first");
        GrailParticleService.send(
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
        tickRecoveryPenalties(server);
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
                notify(caster, "message.typemoonaddon.forbidden_magic.finished");
            }
        }
    }

    public static void playerUnavailable(ServerPlayer player) {
        cancelCasting(player);
        clearRecoveryPenalty(player);
    }

    public static void playerChangedDimension(ServerPlayer player) {
        cancelCasting(player);
    }

    private static void cancelCasting(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session != null) {
            release(player.server, session);
        }
    }

    public static void serverStopping(MinecraftServer server) {
        for (Session session : SESSIONS.values()) {
            release(server, session);
        }
        SESSIONS.clear();
    }

    private static void tickChant(ServerPlayer caster, Session session, long now) {
        if (!session.secondLineShown && now >= session.chantFinishTick - CHANT_TICKS / 2L) {
            session.secondLineShown = true;
            notify(caster, "message.typemoonaddon.forbidden_magic.chant_second");
            caster.serverLevel().playSound(
                null, caster.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 0.9F, 0.4F
            );
        }
        if (now % 2L == 0L) {
            GrailParticleService.send(
                caster.serverLevel(), caster, ParticleTypes.SQUID_INK,
                caster.getX(), caster.getY() + 1.0D, caster.getZ(),
                6, 0.65D, 0.9D, 0.65D, 0.01D
            );
        }
    }

    private static boolean activate(ServerPlayer caster, Session session, long now) {
        if (TypeMoonIntegration.currentMana(caster) < REQUIRED_MANA) {
            notify(caster, "message.typemoonaddon.forbidden_magic.insufficient_mana");
            return false;
        }
        session.active = true;
        session.connectionFinishTick = now + CONNECTION_TICKS;
        session.nextDamageTick = now + DAMAGE_INTERVAL_TICKS;
        TypeMoonIntegration.drainAllMana(caster);
        applyAftereffect(caster, now);

        Vec3 origin = caster.getEyePosition();
        Vec3 look = caster.getLookAngle().normalize();
        AABB search = new AABB(origin, origin).inflate(RANGE);
        for (LivingEntity target : caster.serverLevel().getEntitiesOfClass(
            LivingEntity.class,
            search,
            candidate -> validTarget(caster, candidate, origin, look)
        )) {
            UUID bindingId = UUID.randomUUID();
            boolean locked = target instanceof EnderDragon
                || ImaginaryShadowService.beginAuxiliaryBinding(bindingId, target);
            if (!locked) {
                continue;
            }
            session.targets.put(target.getUUID(), new TargetBinding(bindingId, !(target instanceof EnderDragon)));
            ImaginaryShadowService.maintainForbiddenAbsorption(caster, target);
        }

        GrailParticleService.send(
            caster.serverLevel(), caster, ParticleTypes.SQUID_INK,
            caster.getX(), caster.getY() + 1.0D, caster.getZ(),
            80, 2.0D, 1.5D, 2.0D, 0.08D
        );
        caster.serverLevel().playSound(
            null, caster.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK,
            SoundSource.PLAYERS, 1.0F, 0.45F
        );
        notify(caster, "message.typemoonaddon.forbidden_magic.activated", session.targets.size());
        return true;
    }

    private static void applyAftereffect(ServerPlayer caster, long now) {
        caster.addEffect(new MobEffectInstance(MobEffects.CONFUSION, AFTEREFFECT_TICKS, 0, false, true, true));
    }

    private static void tickRecoveryPenalties(MinecraftServer server) {
    }

    private static void clearRecoveryPenalty(ServerPlayer player) {
    }

    private static void maintainConnections(
        MinecraftServer server,
        ServerPlayer caster,
        Session session,
        long now
    ) {
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
                iterator.remove();
                continue;
            }
            if (binding.locked && !ImaginaryShadowService.maintainAuxiliaryBinding(binding.bindingId, target)) {
                ImaginaryShadowService.endAuxiliaryBinding(binding.bindingId, target);
                iterator.remove();
                continue;
            }
            ImaginaryShadowService.maintainForbiddenAbsorption(caster, target);
            if (damagePulse) {
                ImaginaryShadowService.pulseForbiddenAbsorption(caster, target);
            }
        }
    }

    private static void release(MinecraftServer server, Session session) {
        for (Map.Entry<UUID, TargetBinding> entry : session.targets.entrySet()) {
            TargetBinding binding = entry.getValue();
            if (!binding.locked) {
                continue;
            }
            LivingEntity target = living(server, session.dimension, entry.getKey());
            if (target != null) {
                ImaginaryShadowService.endAuxiliaryBinding(binding.bindingId, target);
            }
        }
        session.targets.clear();
    }

    private static boolean validCaster(ServerPlayer caster) {
        return caster != null
            && caster.isAlive()
            && !caster.isSpectator()
            && !HolyGrailService.blocksAction(caster)
            && TypeMoonIntegration.isForbiddenMagicLearned(caster);
    }

    private static boolean validSessionCaster(ServerPlayer caster, Session session) {
        return validCaster(caster) && caster.level().dimension().equals(session.dimension);
    }

    private static boolean validTarget(ServerPlayer caster, LivingEntity target, Vec3 origin, Vec3 look) {
        if (target == caster
            || !target.isAlive()
            || target.isRemoved()
            || PollutionService.areShadowFactionAllies(caster, target)) {
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

    private static LivingEntity living(MinecraftServer server, ResourceKey<Level> dimension, UUID targetId) {
        ServerLevel level = server.getLevel(dimension);
        Entity entity = level == null ? null : level.getEntity(targetId);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
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

    private record TargetBinding(UUID bindingId, boolean locked) {
    }

    private ForbiddenMagicService() {
    }
}
