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
    private static final Set<String> MODES = Set.of("follow", "wander", "attack", "dismiss");

    private SummoningMagicIntegration() {
    }

    public static void register() {
        AddonRegistrar addon = TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID);
        boolean definitions = addon.magics().registerDefinition(definition(PRELUDE, 25, false, false, false, null))
                & addon.magics().registerDefinition(definition(WRAITH_SERVITUDE, 25, true, true, true, PRELUDE))
                & addon.magics().registerDefinition(definition(EVIL_SPIRIT_SUMMONING, 35, true, true, true, PRELUDE));
        boolean presets = addon.magics().registerPreset(WRAITH_SERVITUDE, preset())
                & addon.magics().registerPreset(EVIL_SPIRIT_SUMMONING, preset());
        boolean controls = addon.client().registerControl(WRAITH_SERVITUDE,
                new MagicOption(MODE, MagicOption.Kind.ENUM, "follow", 0, 0, List.copyOf(MODES)))
                & addon.client().registerControl(EVIL_SPIRIT_SUMMONING,
                new MagicOption(MODE, MagicOption.Kind.ENUM, "follow", 0, 0, List.copyOf(MODES)));
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
        ServerPlayer player = context.serverPlayer();
        if (player == null || !hasPrelude(player)) {
            return ExecutionResult.FAILED;
        }
        String mode = normalizeMode(context.preset());
        List<WraithEntity> spirits = ownedWraiths(player);
        int cap = Math.max(1, Math.min(12, 1 + (int)(context.proficiency() / 10.0D)));
        if ("dismiss".equals(mode)) {
            spirits.forEach(LivingEntity::discard);
            return ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(10);
        }
        if (spirits.size() < cap) {
            WraithEntity spirit = AddonEntities.WRAITH.get().create(player.serverLevel());
            if (spirit == null) return ExecutionResult.FAILED;
            spirit.setOwner(player);
            spirit.setCommandMode(modeToCommand(mode));
            Vec3 spawn = player.position().add(player.getLookAngle().scale(1.5D)).add(0.0D, 0.6D, 0.0D);
            spirit.moveTo(spawn.x, spawn.y, spawn.z, player.getYRot(), 0.0F);
            player.serverLevel().addFreshEntity(spirit);
            spirits = ownedWraiths(player);
        }
        applyCommand(spirits, player, mode, context.target());
        return ExecutionResult.SUCCESS.withCost(25.0D).withCooldown(12);
    }

    private static ExecutionResult castEvilSpirit(MagicCastContext context) {
        ServerPlayer player = context.serverPlayer();
        if (player == null || !hasPrelude(player)) {
            return ExecutionResult.FAILED;
        }
        String mode = normalizeMode(context.preset());
        List<EvilSpiritEntity> spirits = ownedEvilSpirits(player);
        if ("dismiss".equals(mode)) {
            spirits.forEach(LivingEntity::discard);
            return ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(10);
        }
        if (spirits.size() < 3) {
            EvilSpiritEntity spirit = AddonEntities.EVIL_SPIRIT.get().create(player.serverLevel());
            if (spirit == null) return ExecutionResult.FAILED;
            spirit.setOwner(player);
            spirit.configure(context.proficiency());
            spirit.setCommandMode(modeToCommand(mode));
            Vec3 spawn = player.position().add(player.getLookAngle().scale(2.0D)).add(0.0D, 0.8D, 0.0D);
            spirit.moveTo(spawn.x, spawn.y, spawn.z, player.getYRot(), 0.0F);
            player.serverLevel().addFreshEntity(spirit);
            spirits = ownedEvilSpirits(player);
        }
        applyCommand(spirits, player, mode, context.target());
        return ExecutionResult.SUCCESS.withCost(80.0D).withCooldown(20);
    }

    private static boolean hasPrelude(ServerPlayer player) {
        return TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics().knowledge(player).isLearned(PRELUDE);
    }

    private static String normalizeMode(CompoundTag preset) {
        String mode = preset == null ? "follow" : preset.getString(MODE);
        return MODES.contains(mode) ? mode : "follow";
    }

    private static int modeToCommand(String mode) {
        return switch (mode) {
            case "wander" -> SummonedSpiritEntity.MODE_WANDER;
            case "attack" -> SummonedSpiritEntity.MODE_ATTACK;
            default -> SummonedSpiritEntity.MODE_FOLLOW;
        };
    }

    private static void applyCommand(List<? extends SummonedSpiritEntity> spirits, ServerPlayer player,
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

    private static LivingEntity findTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB box = player.getBoundingBox().expandTowards(look.scale(32.0D)).inflate(2.0D);
        return player.level().getEntitiesOfClass(LivingEntity.class, box,
                candidate -> candidate != player && candidate.isAlive() && player.hasLineOfSight(candidate)
                        && !player.isAlliedTo(candidate)).stream()
                .min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
    }

    private static List<WraithEntity> ownedWraiths(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(WraithEntity.class,
                player.getBoundingBox().inflate(64.0D), spirit -> player.getUUID().equals(spirit.getOwnerId()));
    }

    private static List<EvilSpiritEntity> ownedEvilSpirits(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(EvilSpiritEntity.class,
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
}
