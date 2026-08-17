package com.example.typemoonaddon.servant;

import com.example.typemoonaddon.entity.GillesDeRaisEntity;
import com.example.typemoonaddon.entity.GillesPollutionZoneService;
import com.example.typemoonaddon.entity.HugeSeaMonsterEntity;
import com.example.typemoonaddon.entity.SeaMonsterEntity;
import com.example.typemoonaddon.registry.AddonEntities;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantVoiceHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.jetbrains.annotations.Nullable;

public final class GillesDeRaisCombatHelper {
    public static final double BOOK_MAX_MANA = 2000.0;
    public static final String ACTION_SUMMON_SMALL = "gilles_summon_small_sea_monster";
    public static final String ACTION_SUMMON_LARGE = "gilles_summon_large_sea_monster";
    public static final String ACTION_ABYSSAL_GAZE = "gilles_abyssal_gaze";
    public static final String ACTION_LIFE_ABSORB = "gilles_life_absorb";
    public static final String ACTION_SUMMON_HUGE = "gilles_uncontrolled_huge_sea_monster";
    private static final double PANIC_RANGE = 7.0;
    private static final double MIN_SAFE_RANGE = 18.0;
    private static final double IDEAL_RANGE = 28.0;
    private static final double MAX_COMMAND_RANGE = 40.0;
    private static final String TAG_BOOK_MANA = "GillesSpellbookMana";
    private static final String TAG_BOOK_INITIALIZED = "GillesSpellbookManaInitialized";
    private static final String TAG_LAST_BOOK_REGEN = "GillesSpellbookLastRegen";
    private static final String TAG_LAST_SMALL_SUMMON = "GillesLastSmallSeaMonsterSummon";
    private static final String TAG_LAST_LARGE_SUMMON = "GillesLastLargeSeaMonsterSummon";
    private static final String TAG_LAST_HUGE_SUMMON = "GillesLastHugeSeaMonsterSummon";
    private static final String TAG_LAST_GAZE = "GillesLastAbyssGaze";
    private static final String TAG_LAST_ABSORB = "GillesLastLifeAbsorb";
    private static final String TAG_HUGE_UUID = "GillesHugeSeaMonsterUuid";
    private static final String TAG_LAST_HELPER_TICK = "GillesLastHelperTick";
    private static final String TAG_LAST_RETREAT = "GillesLastRetreat";

    private GillesDeRaisCombatHelper() {
    }

    public static void tick(GillesDeRaisEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        CompoundTag data = entity.getPersistentData();
        long now = level.getGameTime();
        if (data.getLong(TAG_LAST_HELPER_TICK) == now) {
            return;
        }
        data.putLong(TAG_LAST_HELPER_TICK, now);
        GillesPollutionZoneService.tick(level);
        initializeBookMana(data);
        if (!entity.hasUsableSpellbook()) {
            if (entity.hasLostSpellbook()) {
                data.putDouble(TAG_BOOK_MANA, 0.0);
            }
            return;
        }
        regenerateBookMana(data, now);

        LivingEntity target = selectStrategicTarget(entity);

        if (entity.getHealth() <= entity.getMaxHealth() * 0.60F) {
            entity.unlockGiantSeaMonster();
        }

        if (target == null) {
            return;
        }

        runSummonerStateMachine(entity, target, data, now);
    }

    public static double getBookMana(GillesDeRaisEntity entity) {
        if (!entity.hasUsableSpellbook()) {
            return 0.0;
        }
        initializeBookMana(entity.getPersistentData());
        return entity.getPersistentData().getDouble(TAG_BOOK_MANA);
    }

    public static void addBookMana(GillesDeRaisEntity entity, double amount) {
        if (!entity.hasUsableSpellbook()) {
            return;
        }
        CompoundTag data = entity.getPersistentData();
        initializeBookMana(data);
        data.putDouble(TAG_BOOK_MANA, Math.min(BOOK_MAX_MANA, data.getDouble(TAG_BOOK_MANA) + Math.max(0.0, amount)));
    }

    public static net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult executeCombatAction(
            net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantCombatActionContext context) {
        if (!(context.caster() instanceof GillesDeRaisEntity entity) || !(entity.level() instanceof ServerLevel)) {
            return net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult.NOT_HANDLED;
        }
        LivingEntity target = context.target();
        if (!entity.hasUsableSpellbook() || !isValidTarget(entity, target)) {
            return net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult.NOT_HANDLED;
        }
        initializeBookMana(entity.getPersistentData());
        long now = context.gameTick();
        boolean success = switch (context.actionId()) {
            case ACTION_SUMMON_SMALL -> trySummonSmall(entity, target, entity.getPersistentData(), now, 0);
            case ACTION_SUMMON_LARGE -> trySummonLarge(entity, target, entity.getPersistentData(), now, 0, true);
            case ACTION_ABYSSAL_GAZE -> tryAbyssGaze(entity, target, entity.getPersistentData(), now);
            case ACTION_LIFE_ABSORB -> tryLifeAbsorb(entity, target, entity.getPersistentData(), now);
            case ACTION_SUMMON_HUGE -> trySummonHuge(entity, target, entity.getPersistentData(), now);
            default -> false;
        };
        return success
                ? net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult.SUCCESS
                : net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult.NOT_HANDLED;
    }

    private static void initializeBookMana(CompoundTag data) {
        if (!data.getBoolean(TAG_BOOK_INITIALIZED)) {
            data.putBoolean(TAG_BOOK_INITIALIZED, true);
            data.putDouble(TAG_BOOK_MANA, BOOK_MAX_MANA);
        }
    }

    private static void regenerateBookMana(CompoundTag data, long now) {
        long last = data.getLong(TAG_LAST_BOOK_REGEN);
        if (last <= 0L) {
            data.putLong(TAG_LAST_BOOK_REGEN, now);
            return;
        }
        long elapsedSeconds = (now - last) / 20L;
        if (elapsedSeconds <= 0L) {
            return;
        }
        double restored = elapsedSeconds * 5.0;
        data.putDouble(TAG_BOOK_MANA, Math.min(BOOK_MAX_MANA, data.getDouble(TAG_BOOK_MANA) + restored));
        data.putLong(TAG_LAST_BOOK_REGEN, last + elapsedSeconds * 20L);
    }

    private static boolean spendBookMana(CompoundTag data, double amount) {
        double current = data.getDouble(TAG_BOOK_MANA);
        if (current + 1.0E-6 < amount) {
            return false;
        }
        data.putDouble(TAG_BOOK_MANA, current - amount);
        return true;
    }

    private static void runSummonerStateMachine(GillesDeRaisEntity entity, LivingEntity target, CompoundTag data, long now) {
        entity.setTarget(target);
        entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.45, 0.0));

        double distance = Math.sqrt(entity.distanceToSqr(target));
        int nearbyEnemies = countNearbyEnemies(entity, 14.0);
        int smallCount = countSeaMonsters(entity, false);
        int largeCount = countSeaMonsters(entity, true);
        boolean phaseTwo = entity.isGiantSeaMonsterUnlocked();
        boolean panic = distance <= PANIC_RANGE || nearbyEnemies >= 3 || entity.getHealth() <= entity.getMaxHealth() * 0.35F;

        updateSummonerMovement(entity, target, distance, panic, now, data);

        if (phaseTwo && trySummonHuge(entity, target, data, now)) {
            return;
        }

        if (panic) {
            if (trySummonSmall(entity, target, data, now, 20)) {
                return;
            }
            if (tryAbyssGaze(entity, target, data, now)) {
                return;
            }
        }

        int desiredSmall = phaseTwo ? 20 : 10;
        int desiredLarge = phaseTwo ? 3 : 1;
        if (panic) {
            desiredSmall += 8;
            desiredLarge += 1;
        }

        if (smallCount < desiredSmall && trySummonSmall(entity, target, data, now, phaseTwo ? 32 : 40)) {
            return;
        }
        if ((nearbyEnemies >= 2 || target.getHealth() > 80.0F || smallCount >= 8)
                && largeCount < desiredLarge
                && trySummonLarge(entity, target, data, now, phaseTwo ? 90 : 120, false)) {
            return;
        }
        if (getBookMana(entity) <= BOOK_MAX_MANA * 0.45 && tryLifeAbsorb(entity, target, data, now)) {
            return;
        }
        tryAbyssGaze(entity, target, data, now);
    }

    private static void updateSummonerMovement(GillesDeRaisEntity entity, LivingEntity target, double distance,
                                               boolean panic, long now, CompoundTag data) {
        if (distance < MIN_SAFE_RANGE || panic) {
            keepDistance(entity, target, panic ? 13.0 : 9.0, now, data);
            return;
        }
        if (distance > MAX_COMMAND_RANGE && entity.hasLineOfSight(target)) {
            Vec3 toward = target.position().subtract(entity.position());
            if (toward.horizontalDistanceSqr() > 1.0E-4) {
                toward = toward.normalize().scale(Math.min(10.0, distance - IDEAL_RANGE));
                entity.getNavigation().moveTo(entity.getX() + toward.x, entity.getY(), entity.getZ() + toward.z, 0.9);
            }
            return;
        }
        if (distance >= MIN_SAFE_RANGE && distance <= MAX_COMMAND_RANGE) {
            entity.getNavigation().stop();
        }
    }

    private static boolean tryAbyssGaze(GillesDeRaisEntity entity, LivingEntity target, CompoundTag data, long now) {
        if (now - data.getLong(TAG_LAST_GAZE) < 400L || entity.distanceToSqr(target) > 24.0 * 24.0
                || !entity.hasLineOfSight(target) || entity.getCurrentMp() < 10.0
                || ServantCombatSystem.skillsSuppressed(entity) || ServantCombatSystem.cannotAct(entity)) {
            return false;
        }
        data.putLong(TAG_LAST_GAZE, now);
        entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 10.0));
        entity.faceToward(target.position());
        entity.triggerNamedActionAnimation("gaze");
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1, false, true, true));
        if (entity.getRandom().nextFloat() < 0.30F) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 8, false, true, true));
        }
        if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                    30, 0.45, 0.55, 0.45, 0.04);
            level.playSound(null, target.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 0.9F, 1.25F);
        }
        ServantVoiceHelper.tryPlayGillesGaze(entity);
        return true;
    }

    private static boolean tryLifeAbsorb(GillesDeRaisEntity entity, LivingEntity target, CompoundTag data, long now) {
        if (data.getDouble(TAG_BOOK_MANA) >= BOOK_MAX_MANA || now - data.getLong(TAG_LAST_ABSORB) < 80L
                || entity.distanceToSqr(target) > 12.0 * 12.0 || !entity.hasLineOfSight(target)) {
            return false;
        }
        data.putLong(TAG_LAST_ABSORB, now);
        float before = target.getHealth();
        target.invulnerableTime = 0;
        target.hurt(entity.damageSources().magic(), 8.0F);
        float drained = Math.max(0.0F, before - target.getHealth());
        if (drained > 0.0F) {
            data.putDouble(TAG_BOOK_MANA, Math.min(BOOK_MAX_MANA, data.getDouble(TAG_BOOK_MANA) + drained));
        }
        if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY() + 0.7, target.getZ(),
                    16, 0.35, 0.45, 0.35, 0.04);
        }
        return drained > 0.0F;
    }

    private static boolean trySummonSmall(GillesDeRaisEntity entity, LivingEntity target, CompoundTag data, long now, int cooldownTicks) {
        if (now - data.getLong(TAG_LAST_SMALL_SUMMON) < cooldownTicks || countSeaMonsters(entity, false) >= 50) {
            return false;
        }
        if (!spendBookMana(data, 50.0)) {
            return false;
        }
        data.putLong(TAG_LAST_SMALL_SUMMON, now);
        spawnSeaMonster(entity, target, false);
        return true;
    }

    private static boolean trySummonLarge(GillesDeRaisEntity entity, LivingEntity target, CompoundTag data, long now,
                                          int cooldownTicks, boolean force) {
        if (now - data.getLong(TAG_LAST_LARGE_SUMMON) < cooldownTicks || countSeaMonsters(entity, true) >= 5) {
            return false;
        }
        if (force || entity.distanceToSqr(target) < 8.0 * 8.0 || countSeaMonsters(entity, false) >= 8 || target.getHealth() > 120.0F) {
            if (!spendBookMana(data, 200.0)) {
                return false;
            }
            data.putLong(TAG_LAST_LARGE_SUMMON, now);
            spawnSeaMonster(entity, target, true);
            return true;
        }
        return false;
    }

    private static boolean trySummonHuge(GillesDeRaisEntity entity, LivingEntity target, CompoundTag data, long now) {
        if (!entity.isGiantSeaMonsterUnlocked() || now - data.getLong(TAG_LAST_HUGE_SUMMON) < 1200L || hasActiveHugeSeaMonster(entity, data)) {
            return false;
        }
        if (!spendBookMana(data, 2000.0)) {
            return false;
        }
        HugeSeaMonsterEntity huge = AddonEntities.GILLES_HUGE_SEA_MONSTER.get().create((ServerLevel) entity.level());
        if (huge == null) {
            data.putDouble(TAG_BOOK_MANA, Math.min(BOOK_MAX_MANA, data.getDouble(TAG_BOOK_MANA) + 2000.0));
            return false;
        }
        Vec3 offset = target.position().subtract(entity.position());
        if (offset.horizontalDistanceSqr() < 1.0E-4) {
            offset = entity.getLookAngle();
        }
        offset = offset.normalize().scale(8.0);
        huge.moveTo(entity.getX() + offset.x, entity.getY(), entity.getZ() + offset.z, entity.getYRot(), 0.0F);
        huge.setSource(entity);
        huge.setTarget(target);
        ((ServerLevel) entity.level()).addFreshEntity(huge);
        if (entity.isPassenger()) {
            entity.stopRiding();
        }
        entity.startRiding(huge, true);
        entity.getNavigation().stop();
        entity.setTarget(null);
        data.putUUID(TAG_HUGE_UUID, huge.getUUID());
        data.putLong(TAG_LAST_HUGE_SUMMON, now);
        entity.triggerNamedActionAnimation("np");
        ServantVoiceHelper.tryPlayGillesNp(entity);
        ((ServerLevel) entity.level()).sendParticles(ParticleTypes.SQUID_INK, huge.getX(), huge.getY() + 2.0, huge.getZ(),
                120, 3.0, 1.4, 3.0, 0.08);
        return true;
    }

    private static void spawnSeaMonster(GillesDeRaisEntity entity, LivingEntity target, boolean large) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        SeaMonsterEntity seaMonster = AddonEntities.GILLES_SEA_MONSTER.get().create(level);
        if (seaMonster == null) {
            return;
        }
        Vec3 dir = target.position().subtract(entity.position());
        if (dir.horizontalDistanceSqr() < 1.0E-4) {
            dir = entity.getLookAngle();
        }
        Vec3 side = new Vec3(-dir.z, 0.0, dir.x).normalize().scale((entity.getRandom().nextDouble() - 0.5) * 4.0);
        Vec3 spawn = entity.position().add(dir.normalize().scale(2.5)).add(side);
        seaMonster.moveTo(spawn.x, entity.getY(), spawn.z, entity.getYRot(), 0.0F);
        seaMonster.setController(entity);
        seaMonster.setLarge(large);
        seaMonster.setTarget(target);
        level.addFreshEntity(seaMonster);
        level.playSound(null, entity.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, large ? 1.3F : 0.9F, large ? 0.65F : 0.85F);
        level.sendParticles(ParticleTypes.SQUID_INK, seaMonster.getX(), seaMonster.getY() + 0.7, seaMonster.getZ(),
                large ? 30 : 14, 0.6, 0.5, 0.6, 0.05);
        ServantVoiceHelper.tryPlayGillesSummon(entity);
    }

    private static int countSeaMonsters(GillesDeRaisEntity entity, boolean large) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return 0;
        }
        UUID uuid = entity.getUUID();
        return level.getEntitiesOfClass(SeaMonsterEntity.class, entity.getBoundingBox().inflate(96.0),
                seaMonster -> seaMonster.isAlive() && seaMonster.isLarge() == large && uuid.equals(seaMonster.getControllerUuid())).size();
    }

    private static boolean hasActiveHugeSeaMonster(GillesDeRaisEntity entity, CompoundTag data) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return false;
        }
        if (data.hasUUID(TAG_HUGE_UUID)) {
            Entity existing = level.getEntity(data.getUUID(TAG_HUGE_UUID));
            if (existing instanceof HugeSeaMonsterEntity huge && huge.isAlive()) {
                return true;
            }
        }
        UUID uuid = entity.getUUID();
        return !level.getEntitiesOfClass(HugeSeaMonsterEntity.class, entity.getBoundingBox().inflate(160.0),
                huge -> huge.isAlive() && uuid.equals(huge.getSourceUuid())).isEmpty();
    }

    private static void keepDistance(GillesDeRaisEntity entity, LivingEntity target, double retreatBlocks, long now, CompoundTag data) {
        if (now - data.getLong(TAG_LAST_RETREAT) < 10L) {
            return;
        }
        data.putLong(TAG_LAST_RETREAT, now);
        Vec3 away = entity.position().subtract(target.position());
        if (away.horizontalDistanceSqr() < 1.0E-4) {
            away = entity.getLookAngle().reverse();
        }
        away = away.normalize().scale(retreatBlocks);
        entity.getNavigation().moveTo(entity.getX() + away.x, entity.getY(), entity.getZ() + away.z, 1.2);
    }

    @Nullable
    private static LivingEntity selectStrategicTarget(GillesDeRaisEntity entity) {
        LivingEntity current = entity.getTarget();
        if (isValidTarget(entity, current)) {
            return current;
        }
        LivingEntity revenge = entity.getLastHurtByMob();
        if (isValidTarget(entity, revenge)) {
            entity.setTarget(revenge);
            return revenge;
        }
        LivingEntity masterThreat = findMasterThreat(entity);
        if (isValidTarget(entity, masterThreat)) {
            entity.setTarget(masterThreat);
            return masterThreat;
        }
        LivingEntity nearby = findNearbyTarget(entity, 36.0);
        if (nearby != null) {
            entity.setTarget(nearby);
        }
        return nearby;
    }

    @Nullable
    private static LivingEntity findNearbyTarget(GillesDeRaisEntity entity, double radius) {
        AABB box = entity.getBoundingBox().inflate(radius);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : entity.level().getEntitiesOfClass(LivingEntity.class, box, candidate -> isValidTarget(entity, candidate))) {
            double distance = entity.distanceToSqr(candidate);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    @Nullable
    private static LivingEntity findMasterThreat(GillesDeRaisEntity entity) {
        LivingEntity master = entity.getEntityMaster();
        if (master == null || master.level() != entity.level() || master.distanceToSqr(entity) > 64.0 * 64.0) {
            return null;
        }
        LivingEntity attacker = master.getLastHurtByMob();
        if (isValidTarget(entity, attacker)) {
            return attacker;
        }
        return master instanceof net.minecraft.world.entity.Mob mob && isValidTarget(entity, mob.getTarget())
                ? mob.getTarget()
                : null;
    }

    private static int countNearbyEnemies(GillesDeRaisEntity entity, double radius) {
        return entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(radius),
                candidate -> isValidTarget(entity, candidate)).size();
    }

    private static boolean isValidTarget(GillesDeRaisEntity entity, @Nullable LivingEntity target) {
        return target != null && target != entity && target.isAlive() && !target.isAlliedTo(entity)
                && !EntityUtils.isImmunePlayerTarget(target);
    }
}
