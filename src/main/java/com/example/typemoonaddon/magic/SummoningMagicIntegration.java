package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.entity.EvilSpiritEntity;
import com.example.typemoonaddon.entity.SummonedSpiritEntity;
import com.example.typemoonaddon.entity.WraithEntity;
import com.example.typemoonaddon.registry.AddonEntities;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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

/** Server-authoritative summoning and command modes for spirit magic. */
public final class SummoningMagicIntegration {
    public static final ResourceLocation PRELUDE = TypeMoonAddon.id("spirit_summoning");
    public static final ResourceLocation WRAITH_SERVITUDE = TypeMoonAddon.id("wraith_servitude");
    public static final ResourceLocation EVIL_SPIRIT_SUMMONING = TypeMoonAddon.id("evil_spirit_summoning");
    private static final String MODE = "summoning_mode";
    private static final String SUMMON_SIZE = "summon_size";
    private static final Set<String> MODES = Set.of("follow", "wander", "attack", "dismiss");
    private static final Set<String> SUMMON_SIZES = Set.of("large", "small");

    private SummoningMagicIntegration() {
    }

    public static void register() {
        AddonRegistrar addon = TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID);
        boolean definitions = addon.magics().registerDefinition(definition(PRELUDE, 25, false, false, false, null))
                & addon.magics().registerDefinition(definition(WRAITH_SERVITUDE, 25, true, true, true, PRELUDE))
                & addon.magics().registerDefinition(definition(EVIL_SPIRIT_SUMMONING, 35, true, true, true, PRELUDE));
        boolean presets = addon.magics().registerPreset(WRAITH_SERVITUDE, preset())
                & addon.magics().registerPreset(EVIL_SPIRIT_SUMMONING, evilSpiritPreset());
        boolean controls = addon.client().registerControl(WRAITH_SERVITUDE,
                new MagicOption(MODE, MagicOption.Kind.ENUM, "follow", 0, 0, List.copyOf(MODES)))
                & addon.client().registerControl(EVIL_SPIRIT_SUMMONING,
                new MagicOption(SUMMON_SIZE, MagicOption.Kind.ENUM, "large", 0, 0, List.copyOf(SUMMON_SIZES)));
        boolean executors = addon.magics().registerExecutor(WRAITH_SERVITUDE, SummoningMagicIntegration::castWraith)
                & addon.magics().registerExecutor(EVIL_SPIRIT_SUMMONING, SummoningMagicIntegration::castEvilSpirit);
        if (!(definitions && presets && controls && executors)) {
            TypeMoonAddon.LOGGER.warn("Spirit summoning registration contained duplicate or rejected entries");
        }
    }

    public static void tick(ServerPlayer player) {
        if (player == null || player.tickCount % 300 != 0) {
            return;
        }
        double proficiency = TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics().knowledge(player)
                .proficiency(WRAITH_SERVITUDE);
        double cost = Math.max(1.0D, 10.0D - Math.max(0.0D, Math.min(100.0D, proficiency)) * 0.09D);
        List<WraithEntity> spirits = player.serverLevel().getEntitiesOfClass(WraithEntity.class,
                player.getBoundingBox().inflate(64.0D), spirit -> player.getUUID().equals(spirit.getOwnerId()));
        for (WraithEntity spirit : spirits) {
            if (!TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics().mana(player).tryConsume(cost)) {
                spirit.discard();
            }
        }
    }

    private static ExecutionResult castWraith(MagicCastContext context) {
        LivingEntity caster = context.caster();
        if (caster == null || !(caster.level() instanceof net.minecraft.server.level.ServerLevel level)
                || !hasPrelude(caster)) {
            return ExecutionResult.FAILED;
        }
        String mode = normalizeMode(context.preset());
        List<SummonedSpiritEntity> spirits = controllableSpirits(caster);
        for (SummonedSpiritEntity spirit : spirits) {
            if (spirit.getOwnerId() == null) {
                spirit.setOwner(caster);
            }
        }
        if (spirits.isEmpty()) {
            if (caster instanceof ServerPlayer player) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.typemoonworld.wraith_servitude.no_target"), true);
            }
            return ExecutionResult.FAILED;
        }
        int cap = Math.max(1, Math.min(12, 1 + (int)(context.proficiency() / 10.0D)));
        if ("dismiss".equals(mode)) {
            spirits.forEach(LivingEntity::discard);
            return ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(10);
        }
        if (spirits.size() > cap) {
            spirits = spirits.subList(0, cap);
        }
        applyCommand(spirits, caster, mode, context.target());
        return ExecutionResult.SUCCESS.withCost(25.0D).withCooldown(12);
    }

    private static ExecutionResult castEvilSpirit(MagicCastContext context) {
        LivingEntity caster = context.caster();
        if (caster == null || !(caster.level() instanceof net.minecraft.server.level.ServerLevel level)
                || !hasPrelude(caster)) {
            return ExecutionResult.FAILED;
        }
        List<EvilSpiritEntity> spirits = ownedEvilSpirits(caster);
        if (spirits.size() < 3) {
            String size = normalizeSummonSize(context.preset());
            EvilSpiritEntity spirit = ("small".equals(size)
                    ? AddonEntities.EVIL_SPIRIT_SMALL.get()
                    : AddonEntities.EVIL_SPIRIT.get()).create(level);
            if (spirit == null) return ExecutionResult.FAILED;
            spirit.setOwner(caster);
            spirit.configure(context.proficiency());
            spirit.setCommandMode(SummonedSpiritEntity.MODE_ATTACK);
            Vec3 spawn = caster.position().add(caster.getLookAngle().scale(2.0D)).add(0.0D, 0.8D, 0.0D);
            spirit.moveTo(spawn.x, spawn.y, spawn.z, caster.getYRot(), 0.0F);
            level.addFreshEntity(spirit);
        }
        return ExecutionResult.SUCCESS.withCost(80.0D).withCooldown(20);
    }

    private static boolean hasPrelude(LivingEntity caster) {
        return TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics().knowledge(caster).isLearned(PRELUDE);
    }

    private static String normalizeMode(CompoundTag preset) {
        String mode = preset == null ? "follow" : preset.getString(MODE);
        return MODES.contains(mode) ? mode : "follow";
    }

    private static String normalizeSummonSize(CompoundTag preset) {
        String size = preset == null ? "large" : preset.getString(SUMMON_SIZE);
        return SUMMON_SIZES.contains(size) ? size : "large";
    }

    private static int modeToCommand(String mode) {
        return switch (mode) {
            case "wander" -> SummonedSpiritEntity.MODE_WANDER;
            case "attack" -> SummonedSpiritEntity.MODE_ATTACK;
            default -> SummonedSpiritEntity.MODE_FOLLOW;
        };
    }

    private static void applyCommand(List<? extends SummonedSpiritEntity> spirits, LivingEntity player,
                                     String mode, LivingEntity target) {
        int command = modeToCommand(mode);
        for (SummonedSpiritEntity spirit : spirits) {
            spirit.setCommandMode(command);
            if (command == SummonedSpiritEntity.MODE_ATTACK) {
                spirit.setTarget(target != null && target.isAlive() && !spirit.isAlliedTo(target)
                        ? target : findTarget(player));
            }
        }
    }

    private static LivingEntity findTarget(LivingEntity player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB box = player.getBoundingBox().expandTowards(look.scale(32.0D)).inflate(2.0D);
        return player.level().getEntitiesOfClass(LivingEntity.class, box,
                candidate -> candidate != player && candidate.isAlive() && player.hasLineOfSight(candidate)
                        && !player.isAlliedTo(candidate)).stream()
                .min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
    }

    private static List<SummonedSpiritEntity> controllableSpirits(LivingEntity player) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return List.of();
        }
        return level.getEntitiesOfClass(SummonedSpiritEntity.class,
                player.getBoundingBox().inflate(64.0D), spirit ->
                        (spirit instanceof WraithEntity || spirit instanceof EvilSpiritEntity)
                                && (spirit.getOwnerId() == null || player.getUUID().equals(spirit.getOwnerId())));
    }

    private static List<EvilSpiritEntity> ownedEvilSpirits(LivingEntity player) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return List.of();
        }
        return level.getEntitiesOfClass(EvilSpiritEntity.class,
                player.getBoundingBox().inflate(64.0D), spirit -> player.getUUID().equals(spirit.getOwnerId()));
    }

    private static MagicDefinitionData definition(ResourceLocation id, int complexity, boolean selectable,
                                                  boolean crestAllowed, boolean copyable, ResourceLocation prerequisite) {
        return new MagicDefinitionData(id, "magic.typemoonworld." + id.getPath() + ".name",
                TypeMoonAddon.id("special"), TypeMoonAddon.id("none"), complexity, 1, 0.0D, 0.0D, 10,
                true, selectable, crestAllowed, false, !selectable, copyable, 0, 20, prerequisite,
                0.0D, List.of());
    }

    private static MagicPresetHandler preset() {
        return new MagicPresetHandler() {
            @Override
            public CompoundTag normalize(CompoundTag input) {
                CompoundTag output = new CompoundTag();
                String mode = input == null ? "follow" : input.getString(MODE);
                output.putString(MODE, MODES.contains(mode) ? mode : "follow");
                return output;
            }
        };
    }

    private static MagicPresetHandler evilSpiritPreset() {
        return new MagicPresetHandler() {
            @Override
            public CompoundTag normalize(CompoundTag input) {
                CompoundTag output = new CompoundTag();
                String size = input == null ? "large" : input.getString(SUMMON_SIZE);
                output.putString(SUMMON_SIZE, SUMMON_SIZES.contains(size) ? size : "large");
                return output;
            }
        };
    }
}
