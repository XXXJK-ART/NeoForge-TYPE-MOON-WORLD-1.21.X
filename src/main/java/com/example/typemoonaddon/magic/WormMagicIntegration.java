package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.network.AddonNetwork;
import com.example.typemoonaddon.network.OpenDetectionWormControlPayload;
import com.example.typemoonaddon.engravedworm.EngravedWormService;
import com.example.typemoonaddon.worm.WormEntity;
import com.example.typemoonaddon.worm.WormType;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.typemoonworld.api.AddonRegistrar;
import net.xxxjk.typemoonworld.api.ExecutionResult;
import net.xxxjk.typemoonworld.api.MagicCastContext;
import net.xxxjk.typemoonworld.api.MagicDefinitionData;
import net.xxxjk.typemoonworld.api.MagicOption;
import net.xxxjk.typemoonworld.api.MagicPresetHandler;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;

/** Public-API registrations for Matou-style worm magecraft. */
public final class WormMagicIntegration {
    public static final ResourceLocation WORM_MAGIC = TypeMoonAddon.id("worm_magic");
    public static final ResourceLocation WORM_CONTROL = TypeMoonAddon.id("worm_control");
    public static final ResourceLocation ENGRAVED_WORM_OPERATION = TypeMoonAddon.id("engraved_worm_operation");

    public static final String CONTROL_MODE = "worm_control_mode";
    public static final String OPERATION_MODE = "engraved_worm_operation_mode";
    private static final String COLLAPSE_UNTIL = "TypeMoonAddonEngravedWormCollapseUntil";
    private static final Set<String> CONTROL_MODES = Set.of("follow", "attack", "recall", "free");
    private static final Set<String> OPERATION_MODES = Set.of("collapse", "immobilize", "shed", "overdraw");

    private WormMagicIntegration() {
    }

    public static void register() {
        AddonRegistrar addon = TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID);
        boolean definitions = addon.magics().registerDefinition(knowledgeDefinition(
                        WORM_MAGIC, "magic.typemoonworld.worm_magic.name", 30))
                & addon.magics().registerDefinition(operationDefinition(
                        WORM_CONTROL, "magic.typemoonworld.worm_control.name", 40, 4.0D, 4))
                & addon.magics().registerDefinition(operationDefinition(
                        ENGRAVED_WORM_OPERATION, "magic.typemoonworld.engraved_worm_operation.name", 55, 8.0D, 10));
        boolean presets = addon.magics().registerPreset(WORM_CONTROL, enumPreset(CONTROL_MODE, "follow", CONTROL_MODES))
                & addon.magics().registerPreset(ENGRAVED_WORM_OPERATION, enumPreset(OPERATION_MODE, "collapse", OPERATION_MODES));
        boolean controls = addon.client().registerControl(WORM_CONTROL,
                new MagicOption(CONTROL_MODE, MagicOption.Kind.ENUM, "follow", 0, 0, List.of("follow", "attack", "recall", "free")))
                & addon.client().registerControl(ENGRAVED_WORM_OPERATION,
                new MagicOption(OPERATION_MODE, MagicOption.Kind.ENUM, "collapse", 0, 0, List.of("collapse", "immobilize", "shed", "overdraw")));
        boolean executors = addon.magics().registerExecutor(WORM_CONTROL, WormMagicIntegration::castWormControl)
                & addon.magics().registerExecutor(ENGRAVED_WORM_OPERATION, WormMagicIntegration::castEngravedWormOperation);
        if (!(definitions && presets && controls && executors)) {
            TypeMoonAddon.LOGGER.warn("Worm magic API registration contained duplicate or rejected entries");
        }
    }

    public static boolean isKnown(ServerPlayer player, ResourceLocation id) {
        return player != null && TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics().knowledge(player).isLearned(id);
    }

    public static boolean isCollapseActive(ServerPlayer player, long gameTime) {
        return player != null && player.getPersistentData().getLong(COLLAPSE_UNTIL) > gameTime;
    }

    public static void openDetectionControl(ServerPlayer player, WormEntity worm) {
        if (player == null || worm == null || worm.getOwnerId() == null || !player.getUUID().equals(worm.getOwnerId())
                || worm.getVariant() != WormType.DETECTION) {
            return;
        }
        AddonNetwork.sendToPlayer(player, new OpenDetectionWormControlPayload(
                worm.getId(),
                worm.getOwnerId(),
                worm.getDisplayName().getString(),
                worm.isManuallyControlled(),
                worm.isManuallyControlled(),
                worm.getGuPower(),
                List.of(
                        "follow",
                        "recall",
                        "free",
                        "attack"
                )
        ));
    }

    public static boolean applyDetectionControl(ServerPlayer player, int wormEntityId, int mode) {
        if (player == null || !(player.serverLevel().getEntity(wormEntityId) instanceof WormEntity worm)
                || worm.getOwnerId() == null
                || !player.getUUID().equals(worm.getOwnerId())
                || worm.getVariant() != WormType.DETECTION) {
            return false;
        }
        switch (Math.clamp(mode, 0, 3)) {
            case 0 -> {
                worm.setManuallyControlled(true);
                worm.setTarget(null);
            }
            case 1 -> {
                worm.setManuallyControlled(true);
                worm.setTarget(null);
                worm.setPos(player.getX(), player.getEyeY() - 0.3D, player.getZ());
            }
            case 2 -> {
                worm.setManuallyControlled(false);
                worm.setTarget(null);
            }
            case 3 -> {
                worm.setManuallyControlled(true);
                worm.setTarget(findTarget(player, 32.0D));
            }
            default -> {
            }
        }
        return true;
    }

    public static void tickCollapse(ServerPlayer player) {
        if (player == null || !isCollapseActive(player, player.level().getGameTime())) {
            return;
        }
        if (player.tickCount % 20 == 0) {
            player.hurt(player.damageSources().magic(), 8.0F);
        }
    }

    private static MagicDefinitionData knowledgeDefinition(ResourceLocation id, String key, int complexity) {
        return new MagicDefinitionData(id, key, TypeMoonAddon.id("special"), TypeMoonAddon.id("none"),
                complexity, Math.max(1, (complexity + 19) / 20), 0.0D, 0.0D, 0,
                true, false, false, false, true, true, 0, 0, null, 0.0D, List.of());
    }

    private static MagicDefinitionData operationDefinition(
            ResourceLocation id, String key, int complexity, double manaCost, int cooldown) {
        return new MagicDefinitionData(id, key, TypeMoonAddon.id("special"), TypeMoonAddon.id("none"),
                complexity, Math.max(1, (complexity + 19) / 20), manaCost, 0.0D, cooldown,
                true, true, false, true, false, true, 0, cooldown, WORM_MAGIC, 0.0D, List.of());
    }

    private static MagicPresetHandler enumPreset(String key, String defaultValue, Set<String> allowed) {
        return new EnumPreset(key, defaultValue, allowed);
    }

    private static ExecutionResult castWormControl(MagicCastContext context) {
        LivingEntity caster = context.caster();
        if (caster == null || !(caster.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return ExecutionResult.FAILED;
        }
        String mode = context.preset().getString(CONTROL_MODE);
        if (!CONTROL_MODES.contains(mode)) {
            return ExecutionResult.FAILED;
        }
        List<WormEntity> worms = level.getEntitiesOfClass(WormEntity.class,
                caster.getBoundingBox().inflate(64.0D), worm -> caster.getUUID().equals(worm.getOwnerId()));
        if (worms.isEmpty()) {
            return ExecutionResult.FAILED;
        }
        LivingEntity target = "attack".equals(mode) ? findTarget(caster, 32.0D) : null;
        for (WormEntity worm : worms) {
            switch (mode) {
                case "attack" -> {
                    worm.setManuallyControlled(true);
                    worm.setTarget(target);
                }
                case "recall" -> {
                    worm.setManuallyControlled(true);
                    worm.setTarget(null);
                    worm.setPos(caster.getX(), caster.getEyeY() - 0.3D, caster.getZ());
                }
                case "free" -> {
                    worm.setManuallyControlled(false);
                    worm.setTarget(null);
                }
                default -> {
                    worm.setManuallyControlled(false);
                    worm.setTarget(null);
                }
            }
        }
        return ExecutionResult.SUCCESS.withCost(4.0D).withCooldown(4);
    }

    private static ExecutionResult castEngravedWormOperation(MagicCastContext context) {
        ServerPlayer caster = context.serverPlayer();
        if (caster == null) {
            return castNpcEngravedWormOperation(context);
        }
        if (caster.level().isClientSide()) {
            return ExecutionResult.FAILED;
        }
        String mode = context.preset().getString(OPERATION_MODE);
        if (!OPERATION_MODES.contains(mode)) {
            return ExecutionResult.FAILED;
        }
        ServerPlayer host = "overdraw".equals(mode) ? caster : findPlayerTarget(caster, 32.0D);
        if (host == null || !EngravedWormService.hasWormOwnedBy(host, caster.getUUID())) {
            return ExecutionResult.FAILED;
        }
        return switch (mode) {
            case "collapse" -> collapse(caster, host);
            case "immobilize" -> immobilize(caster, host);
            case "shed" -> shed(caster, host);
            case "overdraw" -> overdraw(caster);
            default -> ExecutionResult.FAILED;
        };
    }

    private static ExecutionResult castNpcEngravedWormOperation(MagicCastContext context) {
        LivingEntity caster = context.caster();
        if (caster == null || !(caster.level() instanceof net.minecraft.server.level.ServerLevel)
                || !OPERATION_MODES.contains(context.preset().getString(OPERATION_MODE))) {
            return ExecutionResult.FAILED;
        }
        String mode = context.preset().getString(OPERATION_MODE);
        LivingEntity host = context.target() != null && context.target().isAlive()
                ? context.target()
                : findTarget(caster, 32.0D);
        return switch (mode) {
            case "collapse" -> {
                if (host == null) yield ExecutionResult.FAILED;
                host.getPersistentData().putLong(COLLAPSE_UNTIL, host.level().getGameTime() + 20L * 8L);
                yield ExecutionResult.SUCCESS.withCost(12.0D).withCooldown(10);
            }
            case "immobilize" -> {
                if (host == null) yield ExecutionResult.FAILED;
                for (LivingEntity entity : host.level().getEntitiesOfClass(LivingEntity.class,
                        host.getBoundingBox().inflate(4.0D), LivingEntity::isAlive)) {
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 8, 255, false, true, true), caster);
                    entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 8, 0, false, true, true), caster);
                    entity.setDeltaMovement(0.0D, entity.getDeltaMovement().y, 0.0D);
                }
                yield ExecutionResult.SUCCESS.withCost(16.0D).withCooldown(20);
            }
            case "overdraw" -> {
                if (caster.getHealth() <= 2.0F) yield ExecutionResult.FAILED;
                float healthCost = Math.min(caster.getHealth() - 1.0F, 6.0F);
                caster.hurt(caster.damageSources().magic(), healthCost);
                double gu = Math.max(51.0D, context.proficiency());
                TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics().mana(caster)
                        .add(Math.max(1.0D, gu * healthCost * 0.5D));
                yield ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(10);
            }
            case "shed" -> ExecutionResult.FAILED;
            default -> ExecutionResult.FAILED;
        };
    }

    private static ExecutionResult collapse(ServerPlayer caster, ServerPlayer host) {
        host.getPersistentData().putLong(COLLAPSE_UNTIL, host.level().getGameTime() + 20L * 8L);
        return ExecutionResult.SUCCESS.withCost(12.0D).withCooldown(10);
    }

    private static ExecutionResult immobilize(ServerPlayer caster, ServerPlayer host) {
        for (LivingEntity entity : host.serverLevel().getEntitiesOfClass(LivingEntity.class,
                host.getBoundingBox().inflate(4.0D), LivingEntity::isAlive)) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 8, 255, false, true, true), caster);
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 8, 0, false, true, true), caster);
            entity.setDeltaMovement(0.0D, entity.getDeltaMovement().y, 0.0D);
        }
        return ExecutionResult.SUCCESS.withCost(16.0D).withCooldown(20);
    }

    private static ExecutionResult shed(ServerPlayer caster, ServerPlayer host) {
        List<net.minecraft.world.item.ItemStack> removed = EngravedWormService.removeOwnedBy(host, caster.getUUID());
        if (removed.isEmpty()) {
            return ExecutionResult.FAILED;
        }
        for (net.minecraft.world.item.ItemStack stack : removed) {
            if (!caster.getInventory().add(stack)) {
                caster.drop(stack, false);
            }
        }
        return ExecutionResult.SUCCESS.withCost(8.0D).withCooldown(10);
    }

    private static ExecutionResult overdraw(ServerPlayer caster) {
        int gu = EngravedWormService.totalGuOwnedBy(caster, caster.getUUID());
        if (gu <= 0 || caster.getHealth() <= 2.0F) {
            return ExecutionResult.FAILED;
        }
        float healthCost = Math.min(caster.getHealth() - 1.0F, 6.0F);
        caster.hurt(caster.damageSources().magic(), healthCost);
        TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics().mana(caster).add(Math.max(1.0D, gu * healthCost * 0.5D));
        return ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(10);
    }

    private static ServerPlayer findPlayerTarget(ServerPlayer player, double range) {
        LivingEntity target = findTarget(player, range);
        return target instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    private static LivingEntity findTarget(LivingEntity caster, double range) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle().normalize();
        AABB search = caster.getBoundingBox().expandTowards(look.scale(range)).inflate(2.0D);
        return caster.level().getEntitiesOfClass(LivingEntity.class, search,
                        candidate -> candidate != caster
                                && candidate.isAlive()
                                && caster.hasLineOfSight(candidate)
                                && !isOwnedWorm(caster, candidate))
                .stream()
                .filter(candidate -> {
                    Vec3 offset = candidate.getEyePosition().subtract(eye);
                    double forward = offset.dot(look);
                    return forward >= 0.0D && forward <= range
                            && offset.subtract(look.scale(forward)).lengthSqr() <= 2.25D;
                })
                .min(Comparator.comparingDouble(caster::distanceToSqr))
                .orElse(null);
    }

    private static boolean isOwnedWorm(LivingEntity caster, LivingEntity candidate) {
        return candidate instanceof WormEntity worm && caster.getUUID().equals(worm.getOwnerId());
    }

    private record EnumPreset(String key, String defaultValue, Set<String> allowed) implements MagicPresetHandler {
        @Override
        public CompoundTag normalize(CompoundTag input) {
            CompoundTag normalized = new CompoundTag();
            String value = input == null ? defaultValue : input.getString(key);
            normalized.putString(key, allowed.contains(value) ? value : defaultValue);
            return normalized;
        }
    }
}
