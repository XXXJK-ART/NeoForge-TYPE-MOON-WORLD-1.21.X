package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.data.ImaginarySpaceData;
import com.example.typemoonaddon.data.PollutionData;
import com.example.typemoonaddon.entity.SakuraBlackShadowEntity;
import com.example.typemoonaddon.entity.SakuraShadowArtRibbonEntity;
import com.example.typemoonaddon.entity.SakuraShadowFamiliarEntity;
import com.example.typemoonaddon.registry.AddonAttachments;
import com.example.typemoonaddon.registry.AddonMobEffects;
import com.example.typemoonaddon.network.AddonNetwork;
import com.example.typemoonaddon.network.OpenDevourerSelectionPayload;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;

public final class SakuraPollutionService {
    private static final float BLACK_MUD_PROGRESS_PER_SECOND = 0.035F;
    private static final float SERVANT_KILL_PROGRESS = 0.25F;
    public static final float CORRUPTION_PRIORITY_HEALTH_THRESHOLD = 100.0F;
    private static final String LAST_EROSION_CONTROLLER_TAG = "TypeMoonAddonLastErosionController";
    private static final String LAST_EROSION_DAMAGE_TICK_TAG = "TypeMoonAddonLastErosionDamageTick";
    private static final long EROSION_KILL_CREDIT_TICKS = 20L * 15L;

    public static boolean isPolluted(LivingEntity target) {
        return target != null && progress(target) > 0.0F;
    }

    public static boolean isFullyCorrupted(LivingEntity target) {
        return target != null && target.getData(AddonAttachments.POLLUTION.get()).fullyCorrupted();
    }

    public static boolean canBecomeFullyCorrupted(LivingEntity target) {
        return target != null
                && !isServantLike(target)
                && !isFullyCorrupted(target)
                && !SakuraBlackMudService.isImmune(target);
    }

    public static boolean isUnpollutableServantOrCardUser(LivingEntity target) {
        return target != null
                && isServantLike(target)
                && !isFullyCorrupted(target)
                && !canBecomeFullyCorrupted(target);
    }

    public static boolean shouldPrioritizeCorruption(LivingEntity target) {
        return target != null
                && target.isAlive()
                && target.getHealth() < CORRUPTION_PRIORITY_HEALTH_THRESHOLD
                && canBecomeFullyCorrupted(target);
    }

    public static float limitDamageForCorruptionPriority(LivingEntity target, float requestedDamage) {
        if (target == null || requestedDamage <= 0.0F || !canBecomeFullyCorrupted(target)) {
            return Math.max(0.0F, requestedDamage);
        }
        float protectedHealth = Math.nextDown(CORRUPTION_PRIORITY_HEALTH_THRESHOLD);
        return Math.min(requestedDamage, Math.max(0.0F, target.getHealth() - protectedHealth));
    }

    public static boolean blocksCommandSpell(LivingEntity target) {
        return isFullyCorrupted(target);
    }

    public static float progress(LivingEntity target) {
        if (target == null) {
            return 0.0F;
        }
        return Math.clamp(target.getData(AddonAttachments.POLLUTION.get()).progress(), 0.0F, 1.0F);
    }

    public static void expose(LivingEntity target, ServerPlayer controller, boolean continuous) {
        if (target == null || controller == null || target == controller || target.level().isClientSide()) {
            return;
        }
        PollutionData data = target.getData(AddonAttachments.POLLUTION.get());
        if (data.fullyCorrupted()) {
            return;
        }
        data.begin(controller.getUUID(), 0.5F);
        boolean changed = continuous
                ? data.addProgress(BLACK_MUD_PROGRESS_PER_SECOND / 4.0F)
                : data.addProgress(SERVANT_KILL_PROGRESS);
        if (data.progress() >= 1.0F) {
            data.complete(UUID.randomUUID());
            if (isServantLike(target)) {
                SakuraGrailErosionService.record(controller, target, "full_pollution");
            }
            changed = true;
        }
        if (changed) {
            AddonAttachments.sync(target, AddonAttachments.POLLUTION);
        }
        if (data.progress() > 0.0F) {
            int amplifier = data.fullyCorrupted() ? 2 : 0;
            target.addEffect(new MobEffectInstance(AddonMobEffects.BLACK_MUD_CORRUPTION, 120, amplifier, false, true, true));
        }
    }

    public static void entityDied(LivingEntity target) {
        entityDied(target, null);
    }

    public static void entityDied(LivingEntity target, DamageSource source) {
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        PollutionData pollution = target.getData(AddonAttachments.POLLUTION.get());
        if (isServantLike(target) && !pollution.fullyCorrupted()) {
            recordServantDeath(target, source);
        }
        clearRecentErosionController(target);
        if (!pollution.fullyCorrupted() || pollution.controllerId() == null || pollution.rosterId() == null) {
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(pollution.controllerId());
        if (owner == null) {
            return;
        }
        ImaginarySpaceData ownerData = owner.getData(AddonAttachments.IMAGINARY_SPACE.get());
        CompoundTag snapshot = new CompoundTag();
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        snapshot.putString("id", entityId.toString());
        snapshot.putString("CustomName", target.getDisplayName().getString());
        if (ownerData.addCorruptedServant(pollution.rosterId(), snapshot, null)) {
            AddonAttachments.sync(owner, AddonAttachments.IMAGINARY_SPACE);
            owner.displayClientMessage(Component.translatable("message.typemoonworld.heroic_spirit_devourer.selection_saved", ownerData.corruptedServants().size()), true);
        }
    }

    public static void rememberServantDamage(LivingEntity target, DamageSource source) {
        if (target == null || source == null || target.level().isClientSide() || !isServantLike(target)) {
            return;
        }
        ServerPlayer controller = controllerOf(source.getEntity());
        if (controller == null) {
            controller = controllerOf(source.getDirectEntity());
        }
        if (controller == null) {
            return;
        }
        CompoundTag persistentData = target.getPersistentData();
        persistentData.putUUID(LAST_EROSION_CONTROLLER_TAG, controller.getUUID());
        persistentData.putLong(LAST_EROSION_DAMAGE_TICK_TAG, target.level().getGameTime());
    }

    public static int summonCorruptedServants(ServerPlayer owner) {
        if (owner == null || !(owner.level() instanceof ServerLevel level)) {
            return 0;
        }
        ImaginarySpaceData data = owner.getData(AddonAttachments.IMAGINARY_SPACE.get());
        List<ImaginarySpaceData.CorruptedServantRecord> candidates = new ArrayList<>();
        for (ImaginarySpaceData.CorruptedServantRecord record : data.corruptedServants()) {
            if (data.selectedCorruptedServants().isEmpty() || data.selectedCorruptedServants().contains(record.rosterId())) {
                candidates.add(record);
            }
        }
        int summoned = 0;
        for (ImaginarySpaceData.CorruptedServantRecord record : candidates) {
            if (record.activeEntityId() != null && level.getEntity(record.activeEntityId()) instanceof LivingEntity active && active.isAlive()) {
                summoned++;
                continue;
            }
            Entity entity = createEntity(level, record.entitySnapshot());
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            Vec3 offset = new Vec3((summoned % 3 - 1) * 1.8D, 0.0D, 2.0D + summoned / 3.0D);
            living.moveTo(owner.position().add(offset));
            living.getData(AddonAttachments.POLLUTION.get()).restoreComplete(owner.getUUID(), record.rosterId());
            level.addFreshEntity(living);
            data.setCorruptedServantActive(record.rosterId(), living.getUUID());
            summoned++;
        }
        if (summoned > 0) {
            AddonAttachments.sync(owner, AddonAttachments.IMAGINARY_SPACE);
            owner.displayClientMessage(Component.translatable("message.typemoonworld.heroic_spirit_devourer.summoned", summoned), true);
        } else {
            owner.displayClientMessage(Component.translatable("message.typemoonworld.heroic_spirit_devourer.empty"), true);
        }
        return summoned;
    }

    public static void openDevourerSelection(ServerPlayer owner) {
        if (owner == null) {
            return;
        }
        ImaginarySpaceData data = owner.getData(AddonAttachments.IMAGINARY_SPACE.get());
        List<OpenDevourerSelectionPayload.Entry> entries = new ArrayList<>();
        for (ImaginarySpaceData.CorruptedServantRecord record : data.corruptedServants()) {
            CompoundTag snapshot = record.entitySnapshot();
            String displayName = snapshot.getString("CustomName");
            if (displayName.isBlank()) {
                displayName = snapshot.getString("id");
            }
            boolean active = record.activeEntityId() != null
                    && owner.serverLevel().getEntity(record.activeEntityId()) instanceof LivingEntity living
                    && living.isAlive();
            entries.add(new OpenDevourerSelectionPayload.Entry(
                    record.rosterId(),
                    displayName,
                    data.selectedCorruptedServants().contains(record.rosterId()),
                    active,
                    false
            ));
        }
        AddonNetwork.sendToPlayer(owner, new OpenDevourerSelectionPayload(entries));
    }

    public static void setDevourerSelection(ServerPlayer owner, List<UUID> rosterIds) {
        if (owner == null || rosterIds == null) {
            return;
        }
        ImaginarySpaceData data = owner.getData(AddonAttachments.IMAGINARY_SPACE.get());
        if (data.setSelectedCorruptedServants(new HashSet<>(rosterIds))) {
            AddonAttachments.sync(owner, AddonAttachments.IMAGINARY_SPACE);
        }
        owner.displayClientMessage(Component.translatable(
                "message.typemoonworld.heroic_spirit_devourer.selection_saved",
                data.selectedCorruptedServants().size()
        ), true);
    }

    private static Entity createEntity(ServerLevel level, CompoundTag snapshot) {
        ResourceLocation id = ResourceLocation.tryParse(snapshot.getString("id"));
        if (id == null) {
            return null;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        return type == EntityType.PLAYER ? null : type.create(level);
    }

    private static void recordServantDeath(LivingEntity target, DamageSource source) {
        Entity killingEntity = source == null ? null : source.getEntity();
        Entity directKiller = source == null ? null : source.getDirectEntity();
        String attribution = "damage_source";
        ServerPlayer killer = controllerOf(killingEntity);
        if (killer == null) {
            attribution = "direct_entity";
            killer = controllerOf(directKiller);
        }
        if (killer == null) {
            attribution = "last_hurt_mob";
            killer = controllerOf(target.getLastHurtByMob());
        }
        if (killer == null) {
            attribution = "recent_damage";
            killer = recentErosionController(target);
        }
        if (killer != null) {
            SakuraGrailErosionService.record(killer, target, attribution);
            return;
        }
        TypeMoonAddon.LOGGER.info(
                "Grail erosion outcome victim_type={} servant_id={} source_entity={} direct_entity={} accepted=false reason=controller_unresolved",
                BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()),
                servantId(target),
                killingEntity == null ? "none" : BuiltInRegistries.ENTITY_TYPE.getKey(killingEntity.getType()),
                directKiller == null ? "none" : BuiltInRegistries.ENTITY_TYPE.getKey(directKiller.getType())
        );
    }

    private static boolean isServantLike(LivingEntity target) {
        return ServantIdentityHelper.isServantLike(target);
    }

    private static String servantId(LivingEntity target) {
        var definition = ServantIdentityHelper.definitionOf(target);
        return definition == null ? "unknown" : definition.id();
    }

    private static ServerPlayer recentErosionController(LivingEntity target) {
        CompoundTag persistentData = target.getPersistentData();
        if (!persistentData.hasUUID(LAST_EROSION_CONTROLLER_TAG)
                || !persistentData.contains(LAST_EROSION_DAMAGE_TICK_TAG)) {
            return null;
        }
        long elapsed = target.level().getGameTime() - persistentData.getLong(LAST_EROSION_DAMAGE_TICK_TAG);
        if (elapsed < 0L || elapsed > EROSION_KILL_CREDIT_TICKS || target.getServer() == null) {
            return null;
        }
        ServerPlayer controller = target.getServer().getPlayerList().getPlayer(
                persistentData.getUUID(LAST_EROSION_CONTROLLER_TAG)
        );
        return controller != null
                && controller.getData(AddonAttachments.IMAGINARY_SPACE.get()).grailWormAscended()
                ? controller
                : null;
    }

    private static void clearRecentErosionController(LivingEntity target) {
        CompoundTag persistentData = target.getPersistentData();
        persistentData.remove(LAST_EROSION_CONTROLLER_TAG);
        persistentData.remove(LAST_EROSION_DAMAGE_TICK_TAG);
    }

    private static ServerPlayer controllerOf(Entity source) {
        if (source instanceof ServerPlayer player
                && player.getData(AddonAttachments.IMAGINARY_SPACE.get()).grailWormAscended()) {
            return player;
        }
        if (source instanceof SakuraShadowFamiliarEntity familiar) {
            return controllerOf(familiar.getOwner());
        }
        if (source instanceof SakuraBlackShadowEntity shadow) {
            return controllerOf(shadow.getOwner());
        }
        if (source instanceof SakuraShadowArtRibbonEntity ribbon && ribbon.ownerId() != null) {
            MinecraftServer server = source.getServer();
            if (server != null) {
                ServerPlayer player = server.getPlayerList().getPlayer(ribbon.ownerId());
                if (player != null && player.getData(AddonAttachments.IMAGINARY_SPACE.get()).grailWormAscended()) {
                    return player;
                }
                for (ServerLevel level : server.getAllLevels()) {
                    Entity owner = level.getEntity(ribbon.ownerId());
                    ServerPlayer controller = controllerOf(owner);
                    if (controller != null) {
                        return controller;
                    }
                }
            }
        }
        if (source instanceof Projectile projectile && projectile.getOwner() != source) {
            ServerPlayer controller = controllerOf(projectile.getOwner());
            if (controller != null) {
                return controller;
            }
        }
        if (source instanceof LivingEntity living) {
            PollutionData pollution = living.getExistingDataOrNull(AddonAttachments.POLLUTION.get());
            if (pollution != null && pollution.fullyCorrupted() && living.getServer() != null) {
                return living.getServer().getPlayerList().getPlayer(pollution.controllerId());
            }
        }
        return null;
    }

    private SakuraPollutionService() {
    }
}
