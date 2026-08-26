package io.github.typemoonaddon.magic;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.compat.TypeMoonContractBridge;
import io.github.typemoonaddon.compat.TypeMoonServantBridge;
import io.github.typemoonaddon.data.ImaginarySpaceData.CorruptedServantRecord;
import io.github.typemoonaddon.data.ImaginarySpaceData.ShadowCommandMode;
import io.github.typemoonaddon.data.PollutionData;
import io.github.typemoonaddon.entity.BlackShadowEntity;
import io.github.typemoonaddon.entity.ShadowFamiliarEntity;
import io.github.typemoonaddon.network.OpenDevourerSelectionPayload;
import io.github.typemoonaddon.registry.ModAttachments;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;

/** Owns persistent servant pollution, conversion, allegiance and corrupted-servant recall. */
public final class PollutionService {
    public static final float CORRUPTION_PRIORITY_HEALTH_THRESHOLD = 100.0F;

    private static final String LAST_EROSION_CONTROLLER_TAG = "TypeMoonAddonLastErosionController";
    private static final String LAST_EROSION_DAMAGE_TICK_TAG = "TypeMoonAddonLastErosionDamageTick";
    private static final long EROSION_KILL_CREDIT_TICKS = 20L * 15L;
    private static final float PASSIVE_PROGRESS_PER_TICK = 1.0F / (5.0F * 60.0F * 20.0F);
    private static final float SHADOW_CONTINUOUS_MULTIPLIER = 1.5F;
    private static final float BLACK_MUD_CONTINUOUS_MULTIPLIER = 5.0F;
    private static final float EMIYA_RECOVERY_PER_TICK = 1.0F / (35.0F * 20.0F);
    private static final float EMIYA_DAMAGE_PER_SECOND = 1.0F;
    private static final Set<String> IMMUNE_SERVANTS = Set.of("gilgamesh", "pale_rider", "enkidu");
    private static final String EMIYA = "emiya_archer";
    private static final Map<UUID, ManaSnapshot> CORRUPTED_MANA_CEILINGS = new HashMap<>();

    public static void beginManaSuppression(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }
        if (!isFullyCorrupted(entity)) {
            CORRUPTED_MANA_CEILINGS.remove(entity.getUUID());
            return;
        }

        ManaSnapshot ceiling = CORRUPTED_MANA_CEILINGS.computeIfAbsent(
            entity.getUUID(),
            ignored -> manaSnapshot(entity)
        );
        suppressManaRecovery(entity, ceiling);
    }

    public static void finishManaSuppression(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }
        ManaSnapshot ceiling = CORRUPTED_MANA_CEILINGS.get(entity.getUUID());
        if (!isFullyCorrupted(entity)) {
            CORRUPTED_MANA_CEILINGS.remove(entity.getUUID());
            return;
        }

        if (ceiling == null) {
            CORRUPTED_MANA_CEILINGS.put(
                entity.getUUID(),
                manaSnapshot(entity)
            );
            return;
        }
        suppressManaRecovery(entity, ceiling);
        supplyManaFromController(entity);
    }

    public static void clearManaSuppression(Entity entity) {
        if (entity != null) {
            CORRUPTED_MANA_CEILINGS.remove(entity.getUUID());
        }
    }

    public static void serverStopping() {
        CORRUPTED_MANA_CEILINGS.clear();
    }

    private static void suppressManaRecovery(
        LivingEntity entity,
        ManaSnapshot ceiling
    ) {
        var mana = TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics().mana(entity);
        double excess = mana.current() - ceiling.playerMana();
        if (excess > 0.0D) {
            mana.tryConsume(excess);
        }
        CORRUPTED_MANA_CEILINGS.put(entity.getUUID(), manaSnapshot(entity));
    }

    private static void supplyManaFromController(LivingEntity entity) {
        PollutionData pollution = entity.getExistingDataOrNull(ModAttachments.POLLUTION.get());
        ServerPlayer controller = pollution == null
            ? null
            : controller(entity.getServer(), pollution.controllerId());
        if (controller == null || controller == entity || !controller.isAlive()) {
            return;
        }

        var registry = TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics();
        var sourceMana = registry.mana(controller);
        var targetMana = registry.mana(entity);
        double transferred = Math.min(0.5D, Math.max(0.0D, targetMana.maximum() - targetMana.current()));
        if (transferred <= 0.0D || !sourceMana.tryConsume(transferred)) {
            return;
        }
        targetMana.add(transferred);
        CORRUPTED_MANA_CEILINGS.put(entity.getUUID(), manaSnapshot(entity));
    }

    private static ManaSnapshot manaSnapshot(LivingEntity entity) {
        double current = TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics().mana(entity).current();
        return new ManaSnapshot(current, current, current);
    }

    public static boolean expose(
        LivingEntity target,
        @Nullable Entity source,
        boolean continuous
    ) {
        ServerPlayer controller = controllerOf(source);
        if (controller == null) {
            return false;
        }
        return exposeWithMultiplier(
            target,
            controller,
            continuous,
            continuous ? SHADOW_CONTINUOUS_MULTIPLIER : 1.0F,
            target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
        );
    }

    public static boolean expose(
        LivingEntity target,
        @Nullable ServerPlayer controller,
        boolean continuous,
        Vec3 contactPoint
    ) {
        return exposeWithMultiplier(
            target,
            controller,
            continuous,
            continuous ? BLACK_MUD_CONTINUOUS_MULTIPLIER : 1.0F,
            contactPoint
        );
    }

    private static boolean exposeWithMultiplier(
        LivingEntity target,
        @Nullable ServerPlayer controller,
        boolean continuous,
        float continuousMultiplier,
        Vec3 contactPoint
    ) {
        if (controller == null
            || !controller.getData(ModAttachments.IMAGINARY_SPACE.get()).grailWormAscended()
            || target.level().isClientSide()
            || !TypeMoonServantBridge.isServantLike(target)
            || isImmune(target)) {
            return false;
        }

        PollutionData data = target.getData(ModAttachments.POLLUTION.get());
        float relativeHeight = target.getBbHeight() <= 0.0F
            ? 0.5F
            : (float)((contactPoint.y - target.getY()) / target.getBbHeight());
        boolean changed = data.begin(controller.getUUID(), relativeHeight);
        changed |= data.expose(target.level().getGameTime(), continuous, continuousMultiplier);
        if (isEmiya(target)) {
            changed |= data.capBelowFull();
        }
        if (changed) {
            target.syncData(ModAttachments.POLLUTION.get());
        }
        return true;
    }

    public static void tickEntity(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }
        PollutionData data = entity.getExistingDataOrNull(ModAttachments.POLLUTION.get());
        if (data == null || data.progress() <= 0.0F) {
            return;
        }
        if (entity instanceof BlackShadowEntity) {
            spawnParticles(entity, data);
            return;
        }
        if (isImmune(entity)) {
            data.clearForImmunity();
            entity.syncData(ModAttachments.POLLUTION.get());
            return;
        }

        if (data.fullyCorrupted()) {
            if (discardSupersededCopy(entity, data)) {
                return;
            }
            maintainCorruptedControl(entity, data);
            if (!isServantCardUser(entity)) {
                spawnParticles(entity, data);
            }
            return;
        }

        boolean changed;
        if (isEmiya(entity)) {
            changed = data.reduceProgress(EMIYA_RECOVERY_PER_TICK);
            changed |= data.capBelowFull();
            if (entity.tickCount % 20 == 0) {
                entity.hurt(entity.damageSources().magic(), EMIYA_DAMAGE_PER_SECOND);
            }
        } else {
            float rate = PASSIVE_PROGRESS_PER_TICK * data.exposureMultiplier(entity.level().getGameTime());
            changed = data.addProgress(rate);
            if (data.progress() >= 1.0F) {
                complete(entity, data);
                changed = true;
            }
        }

        spawnParticles(entity, data);
        if (changed && (entity.tickCount % 5 == 0 || data.progress() <= 0.0F || data.fullyCorrupted())) {
            entity.syncData(ModAttachments.POLLUTION.get());
        }
    }

    public static boolean shouldBlockDamage(LivingEntity victim, @Nullable Entity attacker) {
        if (areShadowFactionAllies(victim, attacker)) {
            return true;
        }
        if (attacker instanceof LivingEntity livingAttacker) {
            PollutionData attackerData = livingAttacker.getExistingDataOrNull(ModAttachments.POLLUTION.get());
            return attackerData != null
                && attackerData.fullyCorrupted()
                && attackerData.controllerId() != null
                && attackerData.controllerId().equals(victim.getUUID());
        }
        return false;
    }

    public static boolean cannotTargetAlly(LivingEntity mob, @Nullable LivingEntity proposedTarget) {
        if (proposedTarget == null) {
            return false;
        }
        if (areShadowFactionAllies(mob, proposedTarget)) {
            return true;
        }
        PollutionData data = mob.getExistingDataOrNull(ModAttachments.POLLUTION.get());
        return data != null
            && data.fullyCorrupted()
            && data.controllerId() != null
            && data.controllerId().equals(proposedTarget.getUUID());
    }

    public static void rememberServantDamage(LivingEntity entity, DamageSource source) {
        if (entity.level().isClientSide() || !TypeMoonServantBridge.isServantOrCardUser(entity)) {
            return;
        }
        ServerPlayer controller = controllerOf(source.getEntity());
        if (controller == null) {
            controller = controllerOf(source.getDirectEntity());
        }
        if (controller == null) {
            return;
        }
        CompoundTag persistentData = entity.getPersistentData();
        persistentData.putUUID(LAST_EROSION_CONTROLLER_TAG, controller.getUUID());
        persistentData.putLong(LAST_EROSION_DAMAGE_TICK_TAG, entity.level().getGameTime());
    }

    public static void entityDied(LivingEntity entity, DamageSource source) {
        Entity directKiller = source.getDirectEntity();
        Entity killingEntity = source.getEntity();
        boolean servant = TypeMoonServantBridge.isServantOrCardUser(entity);
        BlackShadowEntity killingShadow = directKiller instanceof BlackShadowEntity shadow
            ? shadow
            : killingEntity instanceof BlackShadowEntity shadow ? shadow : null;
        if (servant && killingShadow != null) {
            addBlackShadowPollution(killingShadow, io.github.typemoonaddon.config.GameplayConfig.BLACK_SHADOW_POLLUTION_PER_SERVANT_KILL);
        }
        PollutionData data = entity.getExistingDataOrNull(ModAttachments.POLLUTION.get());
        if (servant && (data == null || !data.fullyCorrupted())) {
            String attribution = "damage_source";
            ServerPlayer killer = controllerOf(killingEntity);
            if (killer == null) {
                attribution = "direct_entity";
                killer = controllerOf(directKiller);
            }
            if (killer == null) {
                attribution = "last_hurt_mob";
                killer = controllerOf(entity.getLastHurtByMob());
            }
            if (killer == null) {
                attribution = "recent_damage";
                killer = recentErosionController(entity);
            }
            if (killer != null) {
                GrailErosionService.record(killer, entity, attribution);
            } else {
                TypeMoonAddon.LOGGER.info(
                    "Grail erosion outcome victim_type={} servant_id={} source_entity={} direct_entity={} accepted=false reason=controller_unresolved",
                    BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()),
                    TypeMoonServantBridge.servantId(entity),
                    killingEntity == null ? "none" : BuiltInRegistries.ENTITY_TYPE.getKey(killingEntity.getType()),
                    directKiller == null ? "none" : BuiltInRegistries.ENTITY_TYPE.getKey(directKiller.getType())
                );
            }
        }
        clearRecentErosionController(entity);
        if (data == null || !data.fullyCorrupted() || data.controllerId() == null || data.rosterId() == null) {
            return;
        }
        MinecraftServer server = entity.getServer();
        ServerPlayer owner = server == null ? null : server.getPlayerList().getPlayer(data.controllerId());
        if (owner != null) {
            owner.getData(ModAttachments.IMAGINARY_SPACE.get()).clearCorruptedServantActive(
                data.rosterId(),
                entity.getUUID()
            );
        }
    }

    public static int summonCorruptedServants(ServerPlayer owner) {
        if (!owner.getData(ModAttachments.IMAGINARY_SPACE.get()).grailWormAscended()) {
            return 0;
        }
        var ownerData = owner.getData(ModAttachments.IMAGINARY_SPACE.get());
        return summonCorruptedServants(
            owner,
            ownerData.selectedCorruptedServants(),
            owner,
            null,
            true
        );
    }

    public static int summonRandomCorruptedServants(
        ServerPlayer owner,
        BlackShadowEntity shadow,
        @Nullable LivingEntity combatTarget
    ) {
        List<CorruptedServantRecord> candidates = new ArrayList<>(
            owner.getData(ModAttachments.IMAGINARY_SPACE.get()).corruptedServants()
        );
        candidates.removeIf(record -> !canRecall(owner, record));
        if (candidates.isEmpty()) {
            return 0;
        }
        int count = Math.min(candidates.size(), 1 + owner.getRandom().nextInt(2));
        Set<UUID> selected = new HashSet<>();
        while (selected.size() < count && !candidates.isEmpty()) {
            CorruptedServantRecord record = candidates.remove(owner.getRandom().nextInt(candidates.size()));
            selected.add(record.rosterId());
        }
        return summonCorruptedServants(owner, selected, shadow, combatTarget, false);
    }

    public static void openDevourerSelection(ServerPlayer owner) {
        var ownerData = owner.getData(ModAttachments.IMAGINARY_SPACE.get());
        Set<UUID> selected = ownerData.selectedCorruptedServants();
        List<OpenDevourerSelectionPayload.Entry> entries = new ArrayList<>();
        for (CorruptedServantRecord record : ownerData.corruptedServants()) {
            LivingEntity active = findControlledEntity(owner, record);
            CompoundTag snapshot = record.entitySnapshot();
            entries.add(new OpenDevourerSelectionPayload.Entry(
                record.rosterId(),
                corruptedDisplayName(snapshot, active),
                selected.contains(record.rosterId()),
                active != null && active.isAlive(),
                active instanceof ServerPlayer || "minecraft:player".equals(snapshot.getString("id"))
            ));
        }
        PacketDistributor.sendToPlayer(owner, new OpenDevourerSelectionPayload(entries));
    }

    private static int summonCorruptedServants(
        ServerPlayer owner,
        Set<UUID> rosterIds,
        LivingEntity anchor,
        @Nullable LivingEntity combatTarget,
        boolean notifyOwner
    ) {
        if (!(anchor.level() instanceof ServerLevel anchorLevel)) {
            return 0;
        }
        int summoned = 0;
        var ownerData = owner.getData(ModAttachments.IMAGINARY_SPACE.get());
        int index = 0;
        for (CorruptedServantRecord record : ownerData.corruptedServants()) {
            if (!rosterIds.contains(record.rosterId())) {
                continue;
            }
            LivingEntity active = findControlledEntity(owner, record);
            if (active != null) {
                ownerData.setCorruptedServantActive(record.rosterId(), active.getUUID());
                recall(anchor, active, index++);
                assignCombatTarget(owner, active, combatTarget);
                summoned++;
                continue;
            }

            CompoundTag tag = record.entitySnapshot();
            tag.remove("UUID");
            tag.remove("Pos");
            tag.remove("Motion");
            tag.remove("Rotation");
            tag.remove("Passengers");
            Optional<Entity> created = EntityType.create(tag, anchorLevel);
            if (created.isEmpty() || !(created.get() instanceof LivingEntity living)) {
                continue;
            }
            living.moveTo(summonPosition(anchor, index++));
            if (living instanceof Mob mob) {
                mob.setPersistenceRequired();
                mob.setHealth(mob.getMaxHealth());
                mob.setTarget(null);
            }
            PollutionData pollution = living.getData(ModAttachments.POLLUTION.get());
            pollution.restoreComplete(owner.getUUID(), record.rosterId());
            if (!anchorLevel.addFreshEntity(living)) {
                continue;
            }
            TypeMoonContractBridge.transferTo(owner, living);
            ownerData.setCorruptedServantActive(record.rosterId(), living.getUUID());
            living.syncData(ModAttachments.POLLUTION.get());
            ImaginaryShadowService.playDissolutionVisual(anchorLevel, living, anchor);
            assignCombatTarget(owner, living, combatTarget);
            summoned++;
        }

        if (notifyOwner) {
            owner.displayClientMessage(Component.translatable(
                summoned > 0
                    ? "message.typemoonaddon.heroic_spirit_devourer.summoned"
                    : "message.typemoonaddon.heroic_spirit_devourer.empty",
                summoned
            ), true);
        }
        return summoned;
    }

    public static int dismissCorruptedServants(ServerPlayer owner) {
        java.util.List<Mob> entities = new java.util.ArrayList<>();
        var ownerData = owner.getData(ModAttachments.IMAGINARY_SPACE.get());
        for (ServerLevel level : owner.server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof Mob mob)) {
                    continue;
                }
                PollutionData data = mob.getExistingDataOrNull(ModAttachments.POLLUTION.get());
                if (data == null
                    || !data.fullyCorrupted()
                    || !owner.getUUID().equals(data.controllerId())
                    || data.rosterId() == null) {
                    continue;
                }
                entities.add(mob);
            }
        }
        for (Mob mob : entities) {
            PollutionData data = mob.getData(ModAttachments.POLLUTION.get());
            ownerData.setCorruptedServantActive(data.rosterId(), null);
            mob.discard();
        }
        return entities.size();
    }

    public static int killCorruptedServantsForDispel(ServerPlayer owner) {
        List<LivingEntity> entities = new ArrayList<>();
        var ownerData = owner.getData(ModAttachments.IMAGINARY_SPACE.get());
        for (ServerLevel level : owner.server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof LivingEntity living) || living == owner || !living.isAlive()) {
                    continue;
                }
                PollutionData data = living.getExistingDataOrNull(ModAttachments.POLLUTION.get());
                if (data != null
                    && data.fullyCorrupted()
                    && owner.getUUID().equals(data.controllerId())) {
                    entities.add(living);
                }
            }
        }
        for (LivingEntity entity : entities) {
            PollutionData data = entity.getData(ModAttachments.POLLUTION.get());
            if (data.rosterId() != null) {
                ownerData.setCorruptedServantActive(data.rosterId(), null);
            }
            if (ImaginaryShadowService.dissolveAndKill(owner, entity)) {
                data.clearForDispel();
                entity.syncData(ModAttachments.POLLUTION.get());
            }
        }
        return entities.size();
    }

    public static boolean hasControlledCombatant(ServerPlayer owner) {
        for (Entity entity : owner.serverLevel().getAllEntities()) {
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                continue;
            }
            PollutionData data = mob.getExistingDataOrNull(ModAttachments.POLLUTION.get());
            if (data != null
                && data.fullyCorrupted()
                && owner.getUUID().equals(data.controllerId())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isImmune(LivingEntity entity) {
        String id = normalizedServantId(entity);
        return id != null && IMMUNE_SERVANTS.contains(id);
    }

    public static boolean canBecomeFullyCorrupted(LivingEntity entity) {
        if (!TypeMoonServantBridge.isServantLike(entity) || isImmune(entity) || isEmiya(entity)) {
            return false;
        }
        PollutionData data = entity.getExistingDataOrNull(ModAttachments.POLLUTION.get());
        return data == null || !data.fullyCorrupted();
    }

    public static boolean isUnpollutableServantOrCardUser(@Nullable LivingEntity entity) {
        return entity != null
            && TypeMoonServantBridge.isServantOrCardUser(entity)
            && !isFullyCorrupted(entity)
            && !canBecomeFullyCorrupted(entity);
    }

    public static boolean shouldPrioritizeCorruption(@Nullable LivingEntity target) {
        return target != null
            && target.isAlive()
            && target.getHealth() < CORRUPTION_PRIORITY_HEALTH_THRESHOLD
            && canBecomeFullyCorrupted(target);
    }

    public static float limitDamageForCorruptionPriority(LivingEntity target, float requestedDamage) {
        if (requestedDamage <= 0.0F || !canBecomeFullyCorrupted(target)) {
            return Math.max(0.0F, requestedDamage);
        }
        float protectedHealth = Math.nextDown(CORRUPTION_PRIORITY_HEALTH_THRESHOLD);
        return Math.min(requestedDamage, Math.max(0.0F, target.getHealth() - protectedHealth));
    }

    public static boolean isPolluted(LivingEntity entity) {
        PollutionData data = entity.getExistingDataOrNull(ModAttachments.POLLUTION.get());
        return data != null && data.progress() > 0.0F;
    }

    public static boolean isPollutedBy(LivingEntity target, BlackShadowEntity shadow) {
        PollutionData data = target.getExistingDataOrNull(ModAttachments.POLLUTION.get());
        LivingEntity owner = shadow.getOwner();
        return data != null && data.progress() > 0.0F && owner != null
            && owner.getUUID().equals(data.controllerId());
    }

    public static void addBlackShadowPollution(BlackShadowEntity shadow, float amount) {
        if (shadow.level().isClientSide() || amount <= 0.0F) {
            return;
        }
        PollutionData data = shadow.getData(ModAttachments.POLLUTION.get());
        if (data.addProgress(amount)) {
            shadow.syncData(ModAttachments.POLLUTION.get());
        }
    }

    public static boolean isFullyCorrupted(@Nullable Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        PollutionData data = living.getExistingDataOrNull(ModAttachments.POLLUTION.get());
        return data != null && data.fullyCorrupted();
    }

    public static boolean isShadowFactionMember(@Nullable Entity entity) {
        return entity instanceof BlackShadowEntity
            || entity instanceof ShadowFamiliarEntity
            || isFullyCorrupted(entity);
    }

    public static boolean areShadowFactionAllies(@Nullable Entity first, @Nullable Entity second) {
        return first != null
            && second != null
            && isShadowFactionMember(first)
            && isShadowFactionMember(second);
    }

    public static float progress(LivingEntity entity) {
        PollutionData data = entity.getExistingDataOrNull(ModAttachments.POLLUTION.get());
        return data == null ? 0.0F : data.progress();
    }

    public static boolean blocksCommandSpell(@Nullable LivingEntity entity) {
        return entity != null && isPolluted(entity);
    }

    private static void complete(LivingEntity entity, PollutionData data) {
        UUID rosterId = null;
        ServerPlayer controller = controller(entity.getServer(), data.controllerId());
        if (controller != null) {
            TypeMoonContractBridge.transferTo(controller, entity);
        }
        if (controller != null && (entity instanceof Mob || entity instanceof ServerPlayer)) {
            CompoundTag snapshot = entity.saveWithoutId(new CompoundTag());
            var typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            snapshot.putString("id", typeId.toString());
            snapshot.putString("TypeMoonAddonDisplayName", entity.getDisplayName().getString());
            snapshot.remove("UUID");
            rosterId = UUID.randomUUID();
            controller.getData(ModAttachments.IMAGINARY_SPACE.get()).addCorruptedServant(
                rosterId,
                snapshot,
                entity.getUUID()
            );
        }
        data.complete(rosterId);
        if (controller != null) {
            GrailErosionService.record(controller, entity, "full_pollution");
        }
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.setPersistenceRequired();
        }
        if (entity.level() instanceof ServerLevel level) {
            ImaginaryShadowService.playDissolutionVisual(level, entity, controller == null ? entity : controller);
        }
    }

    private static boolean discardSupersededCopy(LivingEntity entity, PollutionData data) {
        if (!(entity instanceof Mob) || data.rosterId() == null) {
            return false;
        }
        ServerPlayer owner = controller(entity.getServer(), data.controllerId());
        if (owner == null) {
            return false;
        }
        UUID activeId = owner.getData(ModAttachments.IMAGINARY_SPACE.get()).activeCorruptedServant(data.rosterId());
        if (entity.getUUID().equals(activeId)) {
            return false;
        }
        entity.discard();
        return true;
    }

    private static void maintainCorruptedControl(LivingEntity entity, PollutionData data) {
        ServerPlayer owner = controller(entity.getServer(), data.controllerId());
        if (owner == null) {
            if (entity instanceof Mob mob) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
            return;
        }
        if (entity instanceof Mob mob && mob.getTarget() == owner) {
            mob.setTarget(null);
        }

        ShadowCommandMode mode = owner.getData(ModAttachments.IMAGINARY_SPACE.get()).activeShadowCommandMode();
        if (mode == ShadowCommandMode.HOLD) {
            entity.setDeltaMovement(Vec3.ZERO);
            if (entity instanceof Mob mob) {
                mob.getNavigation().stop();
            }
            return;
        }
        if (mode == ShadowCommandMode.GATHER) {
            if (owner.level() == entity.level()) {
                moveToward(entity, owner.position(), 1.15D);
            }
            return;
        }
        if (mode == ShadowCommandMode.HUNT && entity instanceof Mob mob) {
            LivingEntity huntTarget = BlackMudHuntService.targetForOwner(owner);
            if (huntTarget != null
                && huntTarget.level() == entity.level()
                && validControlledTarget(owner, data, huntTarget)) {
                mob.setTarget(huntTarget);
                mob.getNavigation().moveTo(huntTarget, 1.15D);
            }
            return;
        }
        if (mode == ShadowCommandMode.SPREAD
            && owner.level() == entity.level()
            && entity.distanceToSqr(owner) < 18.0D * 18.0D) {
            Vec3 away = entity.position().subtract(owner.position());
            if (away.lengthSqr() < 0.01D) {
                double angle = (entity.getUUID().hashCode() & 1023) * Math.PI * 2.0D / 1024.0D;
                away = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            }
            moveToward(entity, entity.position().add(away.normalize().scale(20.0D)), 1.0D);
        }
        if (entity instanceof Mob mob
            && owner.getData(ModAttachments.IMAGINARY_SPACE.get()).shadowAttackAround()
            && (mob.getTarget() == null || !validControlledTarget(owner, data, mob.getTarget()))
            && entity.tickCount % 20 == 0) {
            LivingEntity target = entity.level().getEntitiesOfClass(
                LivingEntity.class,
                new AABB(entity.blockPosition()).inflate(24.0D),
                candidate -> validControlledTarget(owner, data, candidate)
            ).stream().min(java.util.Comparator.comparingDouble(entity::distanceToSqr)).orElse(null);
            mob.setTarget(target);
        }
    }

    private static boolean validControlledTarget(
        ServerPlayer owner,
        @Nullable PollutionData controllerData,
        @Nullable LivingEntity candidate
    ) {
        if (candidate == null
            || candidate == owner
            || !candidate.isAlive()
            || candidate.isInvulnerable()
            || owner.isAlliedTo(candidate)
            || candidate.isAlliedTo(owner)
            || candidate instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        PollutionData candidateData = candidate.getExistingDataOrNull(ModAttachments.POLLUTION.get());
        UUID controllerId = controllerData == null ? owner.getUUID() : controllerData.controllerId();
        return candidateData == null
            || !candidateData.fullyCorrupted()
            || !java.util.Objects.equals(controllerId, candidateData.controllerId());
    }

    private static void moveToward(LivingEntity entity, Vec3 destination, double speed) {
        if (entity instanceof Mob mob) {
            if (entity.tickCount % 10 == 0) {
                mob.getNavigation().moveTo(destination.x, destination.y, destination.z, speed);
            }
            return;
        }
        Vec3 direction = destination.subtract(entity.position());
        if (direction.lengthSqr() > 9.0D) {
            entity.setDeltaMovement(direction.normalize().scale(0.22D));
            entity.hurtMarked = true;
        }
    }

    private static void spawnParticles(LivingEntity entity, PollutionData data) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        float progress = data.progress();
        int interval = Math.max(2, 12 - Mth.floor(progress * 10.0F));
        if (entity.tickCount % interval != 0) {
            return;
        }
        double origin = Mth.lerp(progress, data.originHeight(), 0.5F);
        int groups = Math.max(1, (int)Math.ceil((1 + Mth.floor(progress * 10.0F)) / 10.0D));
        ServerPlayer controller = controller(level.getServer(), data.controllerId());
        byte palette = controller == null
            ? GrailParticleService.BLACK
            : GrailParticleService.palette(controller);
        GrailParticleService.send(
            level,
            palette,
            ParticleTypes.SQUID_INK,
            entity.getX(),
            entity.getY() + entity.getBbHeight() * origin,
            entity.getZ(),
            groups * 10,
            entity.getBbWidth() * (0.1D + 0.35D * progress),
            entity.getBbHeight() * 0.45D * progress,
            entity.getBbWidth() * (0.1D + 0.35D * progress),
            0.01D
        );
    }

    @Nullable
    private static ServerPlayer controllerOf(@Nullable Entity source) {
        if (source instanceof ServerPlayer player
            && player.getData(ModAttachments.IMAGINARY_SPACE.get()).grailWormAscended()) {
            return player;
        }
        if (source instanceof ShadowFamiliarEntity familiar) {
            LivingEntity owner = familiar.getOwner();
            if (owner instanceof ServerPlayer player
                && player.getData(ModAttachments.IMAGINARY_SPACE.get()).grailWormAscended()) {
                return player;
            }
        }
        if (source instanceof io.github.typemoonaddon.shadowlogic.entity.ShadowArtRibbonEntity ribbon
            && ribbon.ownerId() != null
            && ribbon.level() instanceof ServerLevel level) {
            Entity owner = level.getEntity(ribbon.ownerId());
            if (owner == null) {
                owner = level.getServer().getPlayerList().getPlayer(ribbon.ownerId());
            }
            return controllerOf(owner);
        }
        if (source instanceof Projectile projectile && projectile.getOwner() != source) {
            ServerPlayer controller = controllerOf(projectile.getOwner());
            if (controller != null) {
                return controller;
            }
        }
        if (source instanceof BlackShadowEntity shadow) {
            LivingEntity owner = shadow.getOwner();
            if (owner instanceof ServerPlayer player
                && player.getData(ModAttachments.IMAGINARY_SPACE.get()).grailWormAscended()) {
                return player;
            }
        }
        if (source instanceof LivingEntity living) {
            PollutionData pollution = living.getExistingDataOrNull(ModAttachments.POLLUTION.get());
            if (pollution != null && pollution.fullyCorrupted()) {
                return controller(living.getServer(), pollution.controllerId());
            }
        }
        return null;
    }

    @Nullable
    private static ServerPlayer recentErosionController(LivingEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        if (!persistentData.hasUUID(LAST_EROSION_CONTROLLER_TAG)
            || !persistentData.contains(LAST_EROSION_DAMAGE_TICK_TAG)) {
            return null;
        }
        long elapsed = entity.level().getGameTime() - persistentData.getLong(LAST_EROSION_DAMAGE_TICK_TAG);
        if (elapsed < 0L || elapsed > EROSION_KILL_CREDIT_TICKS || entity.getServer() == null) {
            return null;
        }
        ServerPlayer controller = entity.getServer().getPlayerList().getPlayer(
            persistentData.getUUID(LAST_EROSION_CONTROLLER_TAG)
        );
        return controller != null
            && controller.getData(ModAttachments.IMAGINARY_SPACE.get()).grailWormAscended()
            ? controller
            : null;
    }

    private static void clearRecentErosionController(LivingEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        persistentData.remove(LAST_EROSION_CONTROLLER_TAG);
        persistentData.remove(LAST_EROSION_DAMAGE_TICK_TAG);
    }

    public static boolean isEmiya(LivingEntity entity) {
        return EMIYA.equals(normalizedServantId(entity));
    }

    public static boolean isBlackShadowAbsorptionTarget(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        String id = normalizedServantId(entity);
        return "enkidu".equals(id) || "pale_rider".equals(id) || "gilgamesh".equals(id);
    }

    @Nullable
    private static String normalizedServantId(LivingEntity entity) {
        String id = TypeMoonServantBridge.servantId(entity);
        if (id == null) {
            return null;
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        int separator = normalized.indexOf(':');
        return separator >= 0 ? normalized.substring(separator + 1) : normalized;
    }

    @Nullable
    private static ServerPlayer controller(@Nullable MinecraftServer server, @Nullable UUID controllerId) {
        return server == null || controllerId == null ? null : server.getPlayerList().getPlayer(controllerId);
    }

    @Nullable
    private static Entity findEntity(@Nullable MinecraftServer server, @Nullable UUID entityId) {
        if (server == null || entityId == null) {
            return null;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(entityId);
        if (player != null) {
            return player;
        }
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(entityId);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    private static boolean isControlledBy(ServerPlayer owner, LivingEntity entity, UUID rosterId) {
        PollutionData data = entity.getExistingDataOrNull(ModAttachments.POLLUTION.get());
        return data != null
            && data.fullyCorrupted()
            && owner.getUUID().equals(data.controllerId())
            && rosterId.equals(data.rosterId());
    }

    private static boolean canRecall(ServerPlayer owner, CorruptedServantRecord record) {
        LivingEntity active = findControlledEntity(owner, record);
        if (active != null) {
            return true;
        }
        return !"minecraft:player".equals(record.entitySnapshot().getString("id"));
    }

    @Nullable
    private static LivingEntity findControlledEntity(ServerPlayer owner, CorruptedServantRecord record) {
        Entity active = findEntity(owner.getServer(), record.activeEntityId());
        if (active instanceof LivingEntity living
            && living.isAlive()
            && isControlledBy(owner, living, record.rosterId())) {
            return living;
        }
        for (ServerLevel level : owner.server.getAllLevels()) {
            for (Entity candidate : level.getAllEntities()) {
                if (candidate instanceof LivingEntity living
                    && living.isAlive()
                    && isControlledBy(owner, living, record.rosterId())) {
                    return living;
                }
            }
        }
        return null;
    }

    private static boolean isServantCardUser(LivingEntity entity) {
        return entity instanceof ServerPlayer player
            && TypeMoonWorldApi.servantForm(player).transformed();
    }

    private static void assignCombatTarget(
        ServerPlayer owner,
        LivingEntity entity,
        @Nullable LivingEntity target
    ) {
        PollutionData data = entity.getExistingDataOrNull(ModAttachments.POLLUTION.get());
        if (entity instanceof Mob mob && validControlledTarget(owner, data, target)) {
            mob.setTarget(target);
        }
    }

    private static String corruptedDisplayName(CompoundTag snapshot, @Nullable Entity active) {
        if (active != null) {
            return active.getDisplayName().getString();
        }
        String storedName = snapshot.getString("TypeMoonAddonDisplayName");
        if (!storedName.isBlank()) {
            return storedName;
        }
        ResourceLocation typeId = ResourceLocation.tryParse(snapshot.getString("id"));
        return typeId == null
            ? snapshot.getString("id")
            : BuiltInRegistries.ENTITY_TYPE.getOptional(typeId)
                .map(type -> type.getDescription().getString())
                .orElse(typeId.toString());
    }

    private static void recall(LivingEntity anchor, LivingEntity entity, int index) {
        Vec3 position = summonPosition(anchor, index);
        ServerLevel anchorLevel = (ServerLevel)anchor.level();
        if (entity.level() == anchorLevel) {
            entity.teleportTo(position.x, position.y, position.z);
        } else {
            entity.teleportTo(
                anchorLevel,
                position.x,
                position.y,
                position.z,
                Set.of(),
                entity.getYRot(),
                entity.getXRot()
            );
        }
        ImaginaryShadowService.playDissolutionVisual(anchorLevel, entity, anchor);
    }

    private static Vec3 summonPosition(LivingEntity anchor, int index) {
        double angle = anchor.getYRot() * Math.PI / 180.0D + Math.PI + index * 1.35D;
        return anchor.position().add(Math.sin(angle) * 2.5D, 0.1D, -Math.cos(angle) * 2.5D);
    }

    private PollutionService() {
    }

    private record ManaSnapshot(double playerMana, double servantCardMana, double servantEntityMana) {
    }
}
