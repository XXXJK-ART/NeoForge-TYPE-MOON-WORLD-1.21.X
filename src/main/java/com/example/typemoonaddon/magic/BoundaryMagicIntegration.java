package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.entity.BoundaryMarkEntity;
import com.example.typemoonaddon.network.AddonNetwork;
import com.example.typemoonaddon.network.BoundaryImpactVisualPayload;
import com.example.typemoonaddon.network.OpenBoundaryImmunityPayload;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
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
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicLearningProgressService;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicLearningStrategy;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.passive.AdvancedPassiveService;
import net.xxxjk.TYPE_MOON_WORLD.servant.concealment.ServantConcealment;

/** Core server implementation for boundary inscriptions and configurable boundary magecrafts. */
public final class BoundaryMagicIntegration {
    public static final ResourceLocation BOUNDARY_ART = TypeMoonAddon.id("boundary_art");
    public static final String SIDE = "boundary_side";
    public static final String POWER = "boundary_power";
    public static final String BLACKLIST = "boundary_blacklist";
    public static final String BLACKLIST_UUID = "boundary_blacklist_uuid";
    public static final String COMPLEXITY = "boundary_complexity";
    public static final String INTERFERENCE_EFFECT = "boundary_interference_effect";
    public static final String SHAPE = "boundary_shape";
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
    private static final Set<String> INTERFERENCE_EFFECTS = Set.of("nausea", "blindness", "darkness");
    private static final Set<String> SHAPES = Set.of("sphere", "hemisphere");
    private static final List<String> TYPES = List.of("sensing_boundary", "warning_boundary", "defense_boundary",
            "suggestion_boundary", "anti_magic_boundary", "guard_boundary", "interference_boundary");
    private static final double BASE_MANA_PER_SECOND = 0.25D;
    private static final String TAG_LAST_DEFENSE_BLOCKED_TICK = "TypeMoonBoundaryDefenseBlockedTick";
    private static final String TAG_LAST_DEFENSE_IMPACT_TICK = "TypeMoonBoundaryDefenseImpactTick";
    private static final Map<UUID, Set<UUID>> PRESENCE_MEMORY = new HashMap<>();

    private BoundaryMagicIntegration() {
    }

    public static void register() {
        AddonRegistrar addon = TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID);
        boolean definitions = addon.magics().registerDefinition(definition(BOUNDARY_ART, 20, true, null));
        for (String type : TYPES) {
            ResourceLocation id = TypeMoonAddon.id(type);
            definitions &= addon.magics().registerDefinition(definition(id, 30, true, BOUNDARY_ART));
        }
        boolean presets = true;
        boolean controls = true;
        boolean executors = addon.magics().registerExecutor(BOUNDARY_ART, BoundaryMagicIntegration::castBoundaryArt);
        for (String type : TYPES) {
            ResourceLocation id = TypeMoonAddon.id(type);
            presets &= addon.magics().registerPreset(id, new BoundaryPreset());
            controls &= addon.client().registerControl(id, new MagicOption(SIDE, MagicOption.Kind.INTEGER,
                    "10", 10, 100, List.of()));
            controls &= addon.client().registerControl(id, new MagicOption(POWER, MagicOption.Kind.ENUM,
                    "1", 0, 0, List.of("1", "2", "3", "4", "5")));
            controls &= addon.client().registerControl(id, new MagicOption(BLACKLIST, MagicOption.Kind.ENUM,
                    "none", 0, 0, List.copyOf(BLACKLISTS)));
            controls &= addon.client().registerControl(id, new MagicOption(SHAPE, MagicOption.Kind.ENUM,
                    "sphere", 0, 0, List.of("sphere", "hemisphere")));
            if ("interference_boundary".equals(type)) {
                controls &= addon.client().registerControl(id, new MagicOption(INTERFERENCE_EFFECT,
                        MagicOption.Kind.ENUM, "nausea", 0, 0, List.of("nausea", "blindness", "darkness")));
            }
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
        double volume = shapeVolume(mark);
        double cost = BASE_MANA_PER_SECOND * volume * scaledPower(mark) / 1000.0D;
        if ("guard_boundary".equals(mark.getBoundaryType()) && !hasGuardTargets(mark, level)) {
            cost = 0.0D;
        }
        if ("defense_boundary".equals(mark.getBoundaryType()) && mark.getBarrierHealth() < 1000.0F) {
            cost += (1000.0F - mark.getBarrierHealth()) * 0.0005D;
        }
        if (cost > 0.0D && !ManaFurnaceService.trySupply(owner, cost)
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

    public static void enforceDefenseMovement(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide() || !(entity.level() instanceof ServerLevel level)
                || entity.isSpectator()) {
            return;
        }
        Vec3 previous = new Vec3(entity.xo, entity.yo, entity.zo);
        Vec3 current = entity.position();
        if (previous.distanceToSqr(current) < 1.0E-6D) {
            return;
        }
        for (BoundaryMarkEntity mark : activeBoundariesNear(level, current, 128.0D, "defense_boundary")) {
            if (!isAffected(mark, entity) || !segmentCrossesBoundary(mark, previous, current)) {
                continue;
            }
            entity.teleportTo(previous.x, previous.y, previous.z);
            entity.setDeltaMovement(0.0D, Math.min(0.0D, entity.getDeltaMovement().y), 0.0D);
            entity.hurtMarked = true;
            playDefenseImpact(level, mark, previous.add(current).scale(0.5D));
            if (entity instanceof ServerPlayer player) {
                notifyDefenseBlocked(player);
            }
            return;
        }
    }

    public static boolean blocksDefenseInteraction(ServerPlayer player, Vec3 target) {
        if (player == null || target == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        Vec3 start = player.getEyePosition();
        Vec3 middle = start.add(target).scale(0.5D);
        double radius = Math.max(128.0D, start.distanceTo(target) + 8.0D);
        for (BoundaryMarkEntity mark : activeBoundariesNear(level, middle, radius, "defense_boundary")) {
            if (!isAffected(mark, player) || !segmentCrossesBoundary(mark, start, target)) {
                continue;
            }
            notifyDefenseBlocked(player);
            return true;
        }
        return false;
    }

    public static double adjustNaturalManaRegen(LivingEntity entity, double regen) {
        if (entity == null || regen <= 0.0D || !(entity.level() instanceof ServerLevel level)) {
            return regen;
        }
        double multiplier = 1.0D;
        for (BoundaryMarkEntity mark : marksContaining(level, entity, "anti_magic_boundary")) {
            if (isAffected(mark, entity)) {
                multiplier = Math.min(multiplier, Math.max(0.0D, 1.0D - scaledPower(mark) * 0.2D));
            }
        }
        return regen * multiplier;
    }

    public static boolean tryAnalyzeTargetedBoundaryMark(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        EntityHitResult hit = boundaryMarkHit(player);
        if (hit == null || !(hit.getEntity() instanceof BoundaryMarkEntity mark) || !mark.isActive()) {
            return false;
        }
        analyzeBoundaryMark(player, mark);
        return true;
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
        String blacklist = preset.getString(BLACKLIST);
        String blacklistUuid = preset.getString(BLACKLIST_UUID);
        String shape = preset.getString(SHAPE);
        int required = (int) Math.ceil(shapeVolumeUnits(side, shape) * power / 1000.0D);
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
            mark.setRequiredMarks(required);
            mark.setRemainingMarks(required);
            mark.setOccupied(true);
            mark.setController(index == 0);
            mark.setBlacklist(blacklist);
            mark.setBlacklistUuid(blacklistId);
            mark.getPersistentData().putString(SHAPE, shape);
            if ("interference_boundary".equals(type)) {
                String effect = preset.getString(INTERFERENCE_EFFECT);
                mark.getPersistentData().putString(INTERFERENCE_EFFECT,
                        INTERFERENCE_EFFECTS.contains(effect) ? effect : "nausea");
            } else {
                mark.getPersistentData().remove(INTERFERENCE_EFFECT);
            }
        }
        player.displayClientMessage(Component.translatable("message.typemoonworld.boundary.activated",
                Component.translatable("magic.typemoonworld." + type + ".name"), required), true);
        return ExecutionResult.SUCCESS.withCost(Math.max(10.0D, required * 0.05D)).withCooldown(20);
    }

    private static ExecutionResult castBoundaryArt(MagicCastContext context) {
        ServerPlayer player = context.serverPlayer();
        if (player == null || !hasKnowledge(player, BOUNDARY_ART)) {
            return ExecutionResult.FAILED;
        }
        EntityHitResult entityHit = entityHit(player);
        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity livingTarget) {
            openBoundaryImmunity(player, livingTarget);
            return ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(5);
        }
        HitResult rawHit = player.pick(64.0D, 0.0F, false);
        if (!(rawHit instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.boundary.no_surface"), true);
            return ExecutionResult.FAILED;
        }
        BlockPos placement = hit.getBlockPos();
        if (!player.serverLevel().getEntitiesOfClass(BoundaryMarkEntity.class,
                new AABB(placement).inflate(0.25D), BoundaryMarkEntity::isActive).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.boundary.mark_occupied"), true);
            return ExecutionResult.FAILED;
        }
        int complexity = carvedComplexity(player);
        BoundaryMarkEntity carved = BoundaryMarkEntity.unbound(player.serverLevel(), player.getUUID(), placement,
                hit.getDirection());
        carved.setComplexity(complexity);
        player.serverLevel().addFreshEntity(carved);
        player.displayClientMessage(Component.translatable("message.typemoonworld.boundary.mark_carved", complexity), true);
        return ExecutionResult.SUCCESS.withCost(1.0D).withCooldown(5);
    }

    private static void applyBoundaryEffect(BoundaryMarkEntity mark, ServerPlayer owner, ServerLevel level) {
        AABB box = box(mark);
        double powerScale = Math.max(0.25D, mark.getRemainingRatio());
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity.isAlive() && containsBoundary(mark, entity.position()) && isAffected(mark, entity));
        updatePresenceReports(mark, owner, level, entities);
        for (LivingEntity entity : entities) {
            switch (mark.getBoundaryType()) {
                case "sensing_boundary" -> {
                    if (!ServantConcealment.isFullyConcealed(entity)) {
                        entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false, true));
                    }
                }
                case "warning_boundary" -> entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, true, true));
                case "suggestion_boundary" -> urgeMobAway(mark, entity);
                case "anti_magic_boundary" -> {
                }
                case "guard_boundary" -> {
                    if (entity != owner) {
                        float damage = (float) (0.75D + scaledPower(mark) * 0.45D * powerScale);
                        entity.hurt(level.damageSources().magic(), damage);
                        level.sendParticles(ParticleTypes.ENCHANTED_HIT, entity.getX(), entity.getY(0.55D),
                                entity.getZ(), 8, 0.25D, 0.35D, 0.25D, 0.02D);
                    }
                }
                case "interference_boundary" -> {
                    int amplifier = Math.max(0, scaledPower(mark) / 2 - 1);
                    entity.addEffect(new MobEffectInstance(interferenceEffect(mark), 45, amplifier, true, false, true));
                }
                default -> {
                }
            }
        }
    }

    private static void updatePresenceReports(BoundaryMarkEntity mark, ServerPlayer owner, ServerLevel level,
                                              List<LivingEntity> entities) {
        UUID boundaryId = mark.getBoundaryId();
        if (boundaryId == null) {
            return;
        }
        String type = mark.getBoundaryType();
        if (!"sensing_boundary".equals(type) && !"warning_boundary".equals(type)) {
            PRESENCE_MEMORY.remove(boundaryId);
            return;
        }
        Set<UUID> current = new HashSet<>();
        for (LivingEntity entity : entities) {
            if ("sensing_boundary".equals(type) && ServantConcealment.isFullyConcealed(entity)) {
                continue;
            }
            current.add(entity.getUUID());
        }
        Set<UUID> previous = PRESENCE_MEMORY.getOrDefault(boundaryId, Set.of());
        for (LivingEntity entity : entities) {
            if (!current.contains(entity.getUUID()) || previous.contains(entity.getUUID())) {
                continue;
            }
            if ("sensing_boundary".equals(type)) {
                owner.sendSystemMessage(Component.translatable("message.typemoonworld.boundary.sensing_enter",
                        entity.getDisplayName(), mark.blockPosition().getX(), mark.blockPosition().getY(),
                        mark.blockPosition().getZ()));
                owner.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.55F, 1.35F);
            } else {
                owner.sendSystemMessage(Component.translatable("message.typemoonworld.boundary.warning_enter",
                        entity.getDisplayName(), mark.blockPosition().getX(), mark.blockPosition().getY(),
                        mark.blockPosition().getZ()).withStyle(ChatFormatting.RED));
                level.playSound(null, entity.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK,
                        SoundSource.HOSTILE, 1.0F + scaledPower(mark) * 0.35F, 0.65F);
            }
        }
        if ("sensing_boundary".equals(type)) {
            for (UUID id : previous) {
                if (current.contains(id)) {
                    continue;
                }
                Entity leaving = level.getEntity(id);
                Component name = leaving == null ? Component.literal(id.toString()) : leaving.getDisplayName();
                owner.sendSystemMessage(Component.translatable("message.typemoonworld.boundary.sensing_leave",
                        name, mark.blockPosition().getX(), mark.blockPosition().getY(), mark.blockPosition().getZ()));
                owner.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.45F, 0.75F);
            }
        }
        PRESENCE_MEMORY.put(boundaryId, current);
    }

    private static void urgeMobAway(BoundaryMarkEntity mark, LivingEntity entity) {
        if (!(entity instanceof Mob mob)) {
            return;
        }
        mob.setTarget(null);
        Vec3 center = box(mark).getCenter();
        Vec3 away = mob.position().subtract(center);
        if (away.lengthSqr() < 1.0E-4D) {
            away = new Vec3(mob.getRandom().nextDouble() - 0.5D, 0.0D,
                    mob.getRandom().nextDouble() - 0.5D);
        }
        away = away.normalize();
        double distance = scaledSide(mark) * 0.5D + 6.0D + scaledPower(mark) * 2.0D;
        Vec3 target = center.add(away.scale(distance));
        mob.getNavigation().moveTo(target.x, mob.getY(), target.z, 1.0D + scaledPower(mark) * 0.05D);
    }

    private static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> interferenceEffect(BoundaryMarkEntity mark) {
        return switch (readInterferenceEffect(mark)) {
            case "blindness" -> MobEffects.BLINDNESS;
            case "darkness" -> MobEffects.DARKNESS;
            default -> MobEffects.CONFUSION;
        };
    }

    private static String readInterferenceEffect(BoundaryMarkEntity mark) {
        String value = mark.getPersistentData().getString(INTERFERENCE_EFFECT);
        return INTERFERENCE_EFFECTS.contains(value) ? value : "nausea";
    }

    private static boolean hasGuardTargets(BoundaryMarkEntity mark, ServerLevel level) {
        return !level.getEntitiesOfClass(LivingEntity.class, box(mark),
                entity -> entity.isAlive() && containsBoundary(mark, entity.position()) && isAffected(mark, entity)).isEmpty();
    }

    private static boolean isAffected(BoundaryMarkEntity mark, LivingEntity entity) {
        if (entity.getUUID().equals(mark.getAuthorId())) return false;
        if (isImmuneTo(mark.getBoundaryType(), entity)) return false;
        return switch (readBlacklist(mark)) {
            case "all_players" -> entity instanceof Player;
            case "specific_player" -> entity instanceof Player && uuidMatches(entity.getUUID(), mark.getBlacklistUuid());
            case "non_players" -> !(entity instanceof Player);
            case "all_living" -> true;
            case "specific_entity" -> uuidMatches(entity.getUUID(), mark.getBlacklistUuid());
            case "hostile" -> entity instanceof Monster;
            case "all_aggro_targets" -> isAggroTarget(entity);
            default -> false;
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

    private static boolean containsBoundary(BoundaryMarkEntity mark, Vec3 point) {
        Vec3 center = boundaryCenter(mark);
        double radius = scaledSide(mark) / 2.0D;
        double dx = point.x - center.x;
        double dy = point.y - center.y;
        double dz = point.z - center.z;
        if (dx * dx + dy * dy + dz * dz > radius * radius) {
            return false;
        }
        return !"hemisphere".equals(readShape(mark)) || dy >= 0.0D;
    }

    private static Vec3 boundaryCenter(BoundaryMarkEntity mark) {
        return new Vec3(mark.getX(), mark.getY(), mark.getZ());
    }

    private static String readShape(BoundaryMarkEntity mark) {
        String value = mark.getPersistentData().getString(SHAPE);
        return SHAPES.contains(value) ? value : "sphere";
    }

    private static double shapeVolume(BoundaryMarkEntity mark) {
        return shapeVolumeUnits(scaledSide(mark), readShape(mark));
    }

    private static double shapeVolumeUnits(double side, String shape) {
        double sphere = Math.PI / 6.0D * side * side * side;
        return "hemisphere".equals(shape) ? sphere * 0.5D : sphere;
    }

    private static List<BoundaryMarkEntity> marksContaining(ServerLevel level, LivingEntity target, String type) {
        return level.getEntitiesOfClass(BoundaryMarkEntity.class, target.getBoundingBox().inflate(100.0D),
                mark -> mark.isActive() && type.equals(mark.getBoundaryType()) && containsBoundary(mark, target.position()));
    }

    private static List<BoundaryMarkEntity> activeBoundariesNear(ServerLevel level, Vec3 center, double radius, String type) {
        return level.getEntitiesOfClass(BoundaryMarkEntity.class,
                new AABB(center, center).inflate(Math.max(8.0D, radius)),
                mark -> mark.isActive() && type.equals(mark.getBoundaryType()));
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
        PRESENCE_MEMORY.remove(boundaryId);
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

    private static int carvedComplexity(ServerPlayer player) {
        double proficiency = TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics().knowledge(player)
                .proficiency(BOUNDARY_ART);
        return Math.max(1, Math.min(100, (int) Math.floor(proficiency)));
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

    private static EntityHitResult boundaryMarkHit(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(64.0D));
        AABB box = player.getBoundingBox().expandTowards(look.scale(64.0D)).inflate(1.0D);
        return ProjectileUtil.getEntityHitResult(player.level(), player, eye, end, box,
                entity -> entity instanceof BoundaryMarkEntity mark && mark.isActive());
    }

    private static void analyzeBoundaryMark(ServerPlayer player, BoundaryMarkEntity mark) {
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        double analysis = AdvancedPassiveService.effectiveMagicAnalysisProficiency(vars);
        int complexity = mark.getComplexity();
        boolean owner = player.getUUID().equals(mark.getAuthorId());
        boolean knowsBoundaryArt = MagicLearningStrategy.isLearned(vars, "boundary_art");
        String targetMagic = knowsBoundaryArt && mark.isBound() ? mark.getBoundaryType() : "boundary_art";
        if (knowsBoundaryArt && !mark.isBound()) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.boundary.analysis_no_bound_magic"), true);
            MagicProficiencyService.add(vars, "magic_analysis", 0.05D);
            vars.syncPlayerVariables(player);
            return;
        }
        double revealChance = Mth.clamp((analysis + 50.0D - complexity) / 100.0D, 0.05D, 0.95D);
        double learnChance = Mth.clamp((analysis + 25.0D - complexity) / 120.0D, 0.02D, 0.85D);
        boolean revealed = owner || player.getRandom().nextDouble() <= revealChance;
        if (revealed) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.boundary.analysis_identified",
                    Component.translatable("magic.typemoonworld." + targetMagic + ".name"), complexity), false);
        } else {
            player.displayClientMessage(Component.translatable("message.typemoonworld.boundary.analysis_unknown", complexity), true);
        }
        if (MagicLearningStrategy.isLearned(vars, targetMagic)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.boundary.analysis_already_known",
                    Component.translatable("magic.typemoonworld." + targetMagic + ".name")), true);
            MagicProficiencyService.add(vars, "magic_analysis", 0.08D);
            vars.syncPlayerVariables(player);
            return;
        }
        if (!MagicLearningStrategy.learningRequirementsMet(vars, targetMagic)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.boundary.analysis_requirements"), true);
            MagicLearningProgressService.addWithLearningCheck(player, targetMagic, Math.max(5.0D, analysis * 2.0D));
            MagicProficiencyService.add(vars, "magic_analysis", 0.1D);
            vars.syncPlayerVariables(player);
            return;
        }
        if (player.getRandom().nextDouble() <= learnChance) {
            boolean learned = TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics().knowledge(player)
                    .learn(TypeMoonAddon.id(targetMagic));
            if (learned) {
                player.displayClientMessage(Component.translatable("message.typemoonworld.boundary.analysis_learned",
                        Component.translatable("magic.typemoonworld." + targetMagic + ".name")), false);
            }
        } else {
            MagicLearningProgressService.addWithLearningCheck(player, targetMagic,
                    MagicLearningProgressService.maxProgress(targetMagic) * Math.max(0.02D, revealChance * 0.08D));
            player.displayClientMessage(Component.translatable("message.typemoonworld.boundary.analysis_failed",
                    Component.translatable("magic.typemoonworld." + targetMagic + ".name")), true);
        }
        MagicProficiencyService.add(vars, "magic_analysis", Math.max(0.1D, 0.8D - complexity / 150.0D));
        vars.syncPlayerVariables(player);
    }

    private static boolean segmentCrossesBoundary(BoundaryMarkEntity mark, Vec3 start, Vec3 end) {
        boolean startInside = containsBoundary(mark, start);
        boolean endInside = containsBoundary(mark, end);
        if (startInside != endInside) {
            return true;
        }
        if (startInside || endInside) {
            return false;
        }
        Vec3 delta = end.subtract(start);
        for (int step = 1; step < 16; step++) {
            double fraction = step / 16.0D;
            if (containsBoundary(mark, start.add(delta.scale(fraction)))) {
                return true;
            }
        }
        return false;
    }

    private static void notifyDefenseBlocked(ServerPlayer player) {
        long now = player.level().getGameTime();
        long last = player.getPersistentData().getLong(TAG_LAST_DEFENSE_BLOCKED_TICK);
        if (last + 20L <= now) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.boundary.defense_blocked"), true);
            player.getPersistentData().putLong(TAG_LAST_DEFENSE_BLOCKED_TICK, now);
        }
    }

    private static void playDefenseImpact(ServerLevel level, BoundaryMarkEntity mark, Vec3 impact) {
        if (level == null || mark == null || impact == null) {
            return;
        }
        long now = level.getGameTime();
        long last = mark.getPersistentData().getLong(TAG_LAST_DEFENSE_IMPACT_TICK);
        if (last + 4L > now) {
            return;
        }
        mark.getPersistentData().putLong(TAG_LAST_DEFENSE_IMPACT_TICK, now);
        AddonNetwork.sendNear(level, impact.x, impact.y, impact.z, 24.0D,
                new BoundaryImpactVisualPayload(impact.x, impact.y, impact.z, boundaryColor(mark.getBoundaryType())));
    }

    private static int boundaryColor(String type) {
        return switch (type) {
            case "warning_boundary" -> 0xFFE0B14C;
            case "defense_boundary" -> 0xFF65B8FF;
            case "suggestion_boundary" -> 0xFFB96BFF;
            case "anti_magic_boundary" -> 0xFF5C7BFF;
            case "guard_boundary" -> 0xFFF06B6B;
            case "interference_boundary" -> 0xFF78F08A;
            default -> 0xFFF0F0FF;
        };
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
                TypeMoonAddon.id("boundary"), TypeMoonAddon.id("boundary"), complexity, 1, 0.0D, 0.0D, 10,
                true, selectable, true, false, !selectable, true, 0, 20, prerequisite, 0.0D, List.of());
    }

    private static final class BoundaryPreset implements MagicPresetHandler {
        @Override
        public CompoundTag normalize(CompoundTag input) {
            CompoundTag output = input == null ? new CompoundTag() : input.copy();
            output.putInt(SIDE, Math.max(10, Math.min(100, output.contains(SIDE) ? output.getInt(SIDE) : 10)));
            output.putInt(POWER, Math.max(1, Math.min(5, output.contains(POWER) ? output.getInt(POWER) : 1)));
            String shape = output.contains(SHAPE) ? output.getString(SHAPE) : "sphere";
            output.putString(SHAPE, SHAPES.contains(shape) ? shape : "sphere");
            output.putInt(COMPLEXITY, Math.max(1, Math.min(100,
                    output.contains(COMPLEXITY) ? output.getInt(COMPLEXITY) : 10)));
            String interferenceEffect = output.contains(INTERFERENCE_EFFECT)
                    ? output.getString(INTERFERENCE_EFFECT) : "nausea";
            output.putString(INTERFERENCE_EFFECT, INTERFERENCE_EFFECTS.contains(interferenceEffect)
                    ? interferenceEffect : "nausea");
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
