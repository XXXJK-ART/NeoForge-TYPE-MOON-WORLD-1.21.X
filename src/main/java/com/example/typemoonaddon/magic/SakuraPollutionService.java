package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.data.ImaginarySpaceData;
import com.example.typemoonaddon.data.PollutionData;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class SakuraPollutionService {
    private static final float BLACK_MUD_PROGRESS_PER_SECOND = 0.035F;
    private static final float SERVANT_KILL_PROGRESS = 0.25F;

    public static boolean isPolluted(LivingEntity target) {
        return target != null && progress(target) > 0.0F;
    }

    public static boolean isFullyCorrupted(LivingEntity target) {
        return target != null && target.getData(AddonAttachments.POLLUTION.get()).fullyCorrupted();
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
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        PollutionData pollution = target.getData(AddonAttachments.POLLUTION.get());
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

    private SakuraPollutionService() {
    }
}
