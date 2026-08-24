package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.entity.BoundaryMarkEntity;
import com.example.typemoonaddon.registry.AddonEntities;
import com.example.typemoonaddon.network.AddonNetwork;
import com.example.typemoonaddon.network.OpenBoundaryImmunityPayload;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.xxxjk.typemoonworld.api.AddonRegistrar;
import net.xxxjk.typemoonworld.api.ExecutionResult;
import net.xxxjk.typemoonworld.api.MagicCastContext;
import net.xxxjk.typemoonworld.api.MagicDefinitionData;
import net.xxxjk.typemoonworld.api.MagicOption;
import net.xxxjk.typemoonworld.api.MagicPresetHandler;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;

/** Core server implementation for the seven configurable boundary magecrafts. */
public final class BoundaryMagicIntegration {
    public static final ResourceLocation BOUNDARY_ART = TypeMoonAddon.id("boundary_art");
    public static final String SIDE = "boundary_side";
    public static final String POWER = "boundary_power";
    public static final String BLACKLIST = "boundary_blacklist";
    public static final String BLACKLIST_UUID = "boundary_blacklist_uuid";
    public static final String COMPLEXITY = "boundary_complexity";
    public static final List<ResourceLocation> BOUNDARY_MAGIC_IDS = List.of(
            TypeMoonAddon.id("sensing_boundary"),
            TypeMoonAddon.id("warning_boundary"),
            TypeMoonAddon.id("defense_boundary"),
            TypeMoonAddon.id("suggestion_boundary"),
            TypeMoonAddon.id("anti_magic_boundary"),
            TypeMoonAddon.id("guard_boundary"),
            TypeMoonAddon.id("interference_boundary")
    );
    private static final Set<String> BLACKLISTS = Set.of("none", "all_players", "specific_player", "non_players",
            "all_living", "specific_entity", "hostile", "all_aggro_targets");
    private static final List<String> TYPES = List.of("sensing_boundary", "warning_boundary", "defense_boundary",
            "suggestion_boundary", "anti_magic_boundary", "guard_boundary", "interference_boundary");
    private static final double BASE_MANA_PER_SECOND = 0.25D;

    private BoundaryMagicIntegration() {
    }

    public static void register() {
        AddonRegistrar addon = TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID);
        boolean definitions = addon.magics().registerDefinition(definition(BOUNDARY_ART, 20, false, null));
        for (String type : TYPES) {
            ResourceLocation id = TypeMoonAddon.id(type);
            definitions &= addon.magics().registerDefinition(definition(id, 30, true, BOUNDARY_ART));
        }
        boolean presets = true;
        boolean controls = true;
        boolean executors = true;
        for (String type : TYPES) {
            ResourceLocation id = TypeMoonAddon.id(type);
            presets &= addon.magics().registerPreset(id, new BoundaryPreset());
            controls &= addon.client().registerControl(id, new MagicOption(SIDE, MagicOption.Kind.INTEGER,
                    "10", 10, 100, List.of()));
            controls &= addon.client().registerControl(id, new MagicOption(POWER, MagicOption.Kind.ENUM,
                    "1", 0, 0, List.of("1", "2", "3", "4", "5")));
            controls &= addon.client().registerControl(id, new MagicOption(BLACKLIST, MagicOption.Kind.ENUM,
                    "none", 0, 0, List.copyOf(BLACKLISTS)));
            controls &= addon.client().registerControl(id, new MagicOption(COMPLEXITY, MagicOption.Kind.INTEGER,
                    "10", 1, 100, List.of()));
            executors &= addon.magics().registerExecutor(id, BoundaryMagicIntegration::castBoundary);
        }
        if (!(definitions && presets && controls && executors)) {
            TypeMoonAddon.LOGGER.warn("Boundary magic API registration contained duplicate or rejected entries");
        }
    }

    public static void tickMark(BoundaryMarkEntity mark) {
        if (!mark.isActive() || !(mark.level() instanceof ServerLevel level)) {
            return;
        }
        if (mark.tickCount % 20 != 0) {
            return;
        }
        double ratio = Math.max(0.0D, mark.getRemainingRatio());
        if (ratio <= 0.0D) {
            deactivateBoundary(level, mark.getBoundaryId());
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(mark.getAuthorId());
        if (owner == null || !owner.isAlive() || !owner.level().dimension().equals(level.dimension())) {
            deactivateBoundary(level, mark.getBoundaryId());
            return;
        }
        double volume = scaledSide(mark) * scaledSide(mark) * scaledSide(mark);
        double cost = BASE_MANA_PER_SECOND * volume * scaledPower(mark) / 1000.0D;
        if (!ManaFurnaceService.trySupply(owner, cost)
                && !TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics().mana(owner)
                .tryConsume(Math.max(0.1D, cost))) {
            deactivateBoundary(level, mark.getBoundaryId());
            return;
        }
        if ("defense_boundary".equals(mark.getBoundaryType())) {
            mark.setBarrierHealth(Math.min(1000.0F, mark.getBarrierHealth() + 2.0F * scaledPower(mark)));
        }
        applyBoundaryEffect(mark, owner, level);
    }

    public static boolean absorbDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel level)) return false;
        List<BoundaryMarkEntity> marks = marksContaining(level, target, "defense_boundary");
        for (BoundaryMarkEntity mark : marks) {
            if (!isAffected(mark, target)) continue;
            float barrier = mark.getBarrierHealth();
                if (barrier <= 0.0F) continue;
                float absorbed = Math.min(barrier, event.getAmount());
                mark.setBarrierHealth(barrier - absorbed);
                event.setAmount(event.getAmount() - absorbed);
                if (event.getAmount() <= 0.0F) return true;
        }
        return event.getAmount() <= 0.0F;
    }

    private static ExecutionResult castBoundary(MagicCastContext context) {
        ServerPlayer player = context.serverPlayer();
        if (player == null || !hasKnowledge(player, BOUNDARY_ART)) return ExecutionResult.FAILED;
        String rawMagicId = context.magicId();
        int namespaceSeparator = rawMagicId == null ? -1 : rawMagicId.indexOf(':');
        String type = namespaceSeparator >= 0 ? rawMagicId.substring(namespaceSeparator + 1) : rawMagicId;
        CompoundTag preset = new BoundaryPreset().normalize(context.preset());
        int side = preset.getInt(SIDE);
        int power = preset.getInt(POWER);
        int complexity = preset.getInt(COMPLEXITY);
        String blacklist = preset.getString(BLACKLIST);
        String blacklistUuid = preset.getString(BLACKLIST_UUID);
        int required = (int) Math.ceil((side * (double) side * side * power) / 1000.0D);
        EntityHitResult entityHit = entityHit(player);
        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity livingTarget) {
            openBoundaryImmunity(player, livingTarget);
            return ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(5);
        }
        BoundaryMarkEntity existing = findOwnedBoundary(player, type);
        if (existing != null && !player.isShiftKeyDown()) {
            deactivateBoundary(player.serverLevel(), existing.getBoundaryId());
            player.displayClientMessage(Component.translatable("message.typemoonworld.boundary.deactivated",
                    Component.translatable("magic.typemoonworld." + type + ".name")), true);
            return ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(10);
        }
        HitResult rawHit = player.pick(64.0D, 0.0F, false);
        if (!(rawHit instanceof BlockHitResult hit) || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.boundary.no_surface"), true);
            return ExecutionResult.FAILED;
        }
        BlockPos placement = hit.getBlockPos();
        if (!player.serverLevel().getEntitiesOfClass(BoundaryMarkEntity.class,
                new AABB(placement).inflate(0.25D), BoundaryMarkEntity::isActive).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.boundary.mark_occupied"), true);
            return ExecutionResult.FAILED;
        }
        if (player.isShiftKeyDown()) {
            BoundaryMarkEntity carved = BoundaryMarkEntity.unbound(player.serverLevel(), player.getUUID(), placement,
                    hit.getDirection());
            carved.setComplexity(complexity);
            player.serverLevel().addFreshEntity(carved);
            player.displayClientMessage(Component.translatable("message.typemoonworld.boundary.mark_carved", complexity), true);
            return ExecutionResult.SUCCESS.withCost(1.0D).withCooldown(5);
        }
        List<BoundaryMarkEntity> available = availableMarks(player.serverLevel(), player.getUUID(), placement, required);
        if (available.size() < required) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.boundary.insufficient_marks", available.size(), required), true);
            return ExecutionResult.FAILED;
        }
        UUID boundaryId = UUID.randomUUID();
        UUID blacklistId = blacklistUuid.isEmpty() ? null : UUID.fromString(blacklistUuid);
        for (int index = 0; index < required; index++) {
            BoundaryMarkEntity mark = available.get(index);
            mark.setBoundaryId(boundaryId);
            mark.setBoundaryType(type);
            mark.setSide(side);
            mark.setPower(power);
            mark.setComplexity(complexity);
            mark.setRequiredMarks(required);
            mark.setRemainingMarks(required);
            mark.setOccupied(true);
            mark.setController(index == 0);
            mark.setBlacklist(blacklist);
            mark.setBlacklistUuid(blacklistId);
        }
        player.displayClientMessage(Component.translatable("message.typemoonworld.boundary.activated",
                Component.translatable("magic.typemoonworld." + type + ".name"), required), true);
        return ExecutionResult.SUCCESS.withCost(Math.max(10.0D, required * 0.05D)).withCooldown(20);
    }

    private static void applyBoundaryEffect(BoundaryMarkEntity mark, ServerPlayer owner, ServerLevel level) {
        AABB box = box(mark);
        double powerScale = Math.max(0.25D, mark.getRemainingRatio());
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity.isAlive() && isAffected(mark, entity));
        for (LivingEntity entity : entities) {
            switch (mark.getBoundaryType()) {
                case "sensing_boundary" -> entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false, true));
                case "warning_boundary" -> entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, true, true));
                case "suggestion_boundary" -> entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 30, 0, true, false, true));
                case "anti_magic_boundary" -> {
                    if (entity instanceof ServerPlayer target) {
                        var targetMana = TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics().mana(target);
                        targetMana.tryConsume(Math.min(1.0D * scaledPower(mark) * powerScale, targetMana.current()));
                    }
                }
                case "guard_boundary" -> {
                    if (entity != owner && entity.distanceToSqr(mark) < 16.0D * powerScale) entity.hurt(level.damageSources().magic(), scaledPower(mark));
                }
                case "interference_boundary" -> {
                    int amplifier = Math.max(0, scaledPower(mark) - 1);
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, amplifier, true, false, true));
                    entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30, amplifier, true, false, true));
                }
                default -> {
                }
            }
        }
    }

    private static boolean isAffected(BoundaryMarkEntity mark, LivingEntity entity) {
        if (entity.getUUID().equals(mark.getAuthorId())) return false;
        if (isImmuneTo(mark.getBoundaryType(), entity)) return false;
        return switch (readBlacklist(mark)) {
            case "all_players" -> !(entity instanceof Player);
            case "specific_player" -> !(entity instanceof Player && uuidMatches(entity.getUUID(), mark.getBlacklistUuid()));
            case "non_players" -> entity instanceof Player;
            case "all_living" -> false;
            case "specific_entity" -> !uuidMatches(entity.getUUID(), mark.getBlacklistUuid());
            case "hostile" -> !(entity instanceof Monster);
            case "all_aggro_targets" -> !isAggroTarget(entity);
            default -> true;
        };
    }

    private static String readBlacklist(BoundaryMarkEntity mark) {
        String blacklist = mark.getBlacklist();
        return BLACKLISTS.contains(blacklist) ? blacklist : "none";
    }

    private static AABB box(BoundaryMarkEntity mark) {
        double half = scaledSide(mark) / 2.0D;
        return new AABB(mark.getX() - half, mark.getY() - half, mark.getZ() - half,
                mark.getX() + half, mark.getY() + half, mark.getZ() + half);
    }

    private static List<BoundaryMarkEntity> marksContaining(ServerLevel level, LivingEntity target, String type) {
        return level.getEntitiesOfClass(BoundaryMarkEntity.class, target.getBoundingBox().inflate(100.0D),
                mark -> mark.isActive() && type.equals(mark.getBoundaryType()) && box(mark).contains(target.position()));
    }

    private static BoundaryMarkEntity findOwnedBoundary(ServerPlayer player, String type) {
        return player.serverLevel().getEntitiesOfClass(BoundaryMarkEntity.class, player.getBoundingBox().inflate(128.0D),
                mark -> mark.isActive() && player.getUUID().equals(mark.getAuthorId()) && type.equals(mark.getBoundaryType()))
                .stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
    }

    public static void markDestroyed(BoundaryMarkEntity mark) {
        if (mark == null || !mark.isBound()) {
            return;
        }
        if (!(mark.level() instanceof ServerLevel level)) {
            return;
        }
        UUID boundaryId = mark.getBoundaryId();
        List<BoundaryMarkEntity> remaining = level.getEntitiesOfClass(BoundaryMarkEntity.class,
                mark.getBoundingBox().inflate(512.0D), candidate -> candidate.isActive()
                        && boundaryId.equals(candidate.getBoundaryId()));
        remaining.remove(mark);
        int count = remaining.size();
        for (BoundaryMarkEntity candidate : remaining) {
            candidate.setRemainingMarks(count);
            candidate.setController(false);
        }
        if (count > 0) {
            remaining.get(0).setController(true);
        } else {
            mark.deactivate();
        }
    }

    private static List<BoundaryMarkEntity> availableMarks(ServerLevel level, UUID authorId,
                                                            BlockPos placement, int required) {
        return level.getEntitiesOfClass(BoundaryMarkEntity.class, new AABB(placement).inflate(128.0D), mark ->
                        mark.isActive() && authorId.equals(mark.getAuthorId()) && !mark.isBound())
                .stream()
                .sorted(Comparator.comparingDouble(mark -> mark.distanceToSqr(Vec3.atCenterOf(placement))))
                .limit(required)
                .toList();
    }

    private static void deactivateBoundary(ServerLevel level, UUID boundaryId) {
        if (boundaryId == null) {
            return;
        }
        for (BoundaryMarkEntity mark : level.getEntitiesOfClass(BoundaryMarkEntity.class,
                new AABB(BlockPos.ZERO).inflate(30_000_000.0D), candidate ->
                        boundaryId.equals(candidate.getBoundaryId()))) {
            mark.deactivate();
        }
    }

    public static void openBoundaryImmunity(ServerPlayer player, LivingEntity target) {
        if (player == null || target == null || target.level() != player.level()) {
            return;
        }
        AddonNetwork.sendToPlayer(player, new OpenBoundaryImmunityPayload(
                target.getId(),
                target.getDisplayName().getString(),
                boundaryImmunity(target)
        ));
    }

    public static List<String> boundaryImmunity(LivingEntity entity) {
        if (entity == null) {
            return List.of();
        }
        ListTag list = entity.getPersistentData().getList("TypeMoonBoundaryImmunity", Tag.TAG_STRING);
        List<String> result = new ArrayList<>(list.size());
        for (int index = 0; index < list.size(); index++) {
            result.add(list.getString(index));
        }
        return result;
    }

    public static void setBoundaryImmunity(LivingEntity entity, List<String> ids) {
        if (entity == null) {
            return;
        }
        ListTag list = new ListTag();
        if (ids != null) {
            for (String id : ids) {
                if (id != null && BOUNDARY_MAGIC_IDS.stream().anyMatch(boundary -> boundary.toString().equals(id))) {
                    list.add(net.minecraft.nbt.StringTag.valueOf(id));
                }
            }
        }
        entity.getPersistentData().put("TypeMoonBoundaryImmunity", list);
    }

    public static List<ResourceLocation> boundaryMagicIds() {
        return BOUNDARY_MAGIC_IDS;
    }

    public static boolean isBoundaryMagicId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return false;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(rawId);
        String path = parsed == null ? rawId : parsed.getPath();
        return BOUNDARY_MAGIC_IDS.stream().anyMatch(id -> id.getPath().equals(path));
    }

    private static boolean hasKnowledge(ServerPlayer player, ResourceLocation id) {
        return TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics().knowledge(player).isLearned(id);
    }

    private static boolean uuidMatches(UUID a, UUID b) {
        return a != null && a.equals(b);
    }

    private static boolean isImmuneTo(String boundaryType, LivingEntity entity) {
        if (boundaryType == null || entity == null) {
            return false;
        }
        return boundaryImmunity(entity).contains(TypeMoonAddon.id(boundaryType).toString());
    }

    private static EntityHitResult entityHit(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(64.0D));
        AABB box = player.getBoundingBox().expandTowards(look.scale(64.0D)).inflate(1.0D);
        return ProjectileUtil.getEntityHitResult(player.level(), player, eye, end, box,
                entity -> entity instanceof LivingEntity living && living.isAlive() && entity != player);
    }

    private static boolean isAggroTarget(LivingEntity entity) {
        return entity instanceof Mob mob && mob.getTarget() != null
                || entity instanceof NeutralMob neutralMob && neutralMob.isAngry();
    }

    private static double scaledSide(BoundaryMarkEntity mark) {
        return Math.max(1.0D, mark.getSide() * Math.max(0.25D, mark.getRemainingRatio()));
    }

    private static int scaledPower(BoundaryMarkEntity mark) {
        return Math.max(1, (int) Math.round(mark.getPower() * Math.max(0.25D, mark.getRemainingRatio())));
    }

    private static MagicDefinitionData definition(ResourceLocation id, int complexity, boolean selectable,
                                                  ResourceLocation prerequisite) {
        return new MagicDefinitionData(id, "magic.typemoonworld." + id.getPath() + ".name",
                TypeMoonAddon.id("special"), TypeMoonAddon.id("none"), complexity, 1, 0.0D, 0.0D, 10,
                true, selectable, true, false, !selectable, true, 0, 20, prerequisite, 0.0D, List.of());
    }

    private static final class BoundaryPreset implements MagicPresetHandler {
        @Override
        public CompoundTag normalize(CompoundTag input) {
            CompoundTag output = input == null ? new CompoundTag() : input.copy();
            output.putInt(SIDE, Math.max(10, Math.min(100, output.contains(SIDE) ? output.getInt(SIDE) : 10)));
            output.putInt(POWER, Math.max(1, Math.min(5, output.contains(POWER) ? output.getInt(POWER) : 1)));
            output.putInt(COMPLEXITY, Math.max(1, Math.min(100,
                    output.contains(COMPLEXITY) ? output.getInt(COMPLEXITY) : 10)));
            String blacklist = output.contains(BLACKLIST) ? output.getString(BLACKLIST) : "none";
            output.putString(BLACKLIST, BLACKLISTS.contains(blacklist) ? blacklist : "none");
            if (!"specific_player".equals(output.getString(BLACKLIST))
                    && !"specific_entity".equals(output.getString(BLACKLIST))) {
                output.remove(BLACKLIST_UUID);
            } else if (output.contains(BLACKLIST_UUID)) {
                try {
                    UUID.fromString(output.getString(BLACKLIST_UUID));
                } catch (IllegalArgumentException ex) {
                    output.remove(BLACKLIST_UUID);
                }
            }
            return output;
        }
    }
}
