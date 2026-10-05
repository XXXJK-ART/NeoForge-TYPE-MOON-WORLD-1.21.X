package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.entity.EvilSpiritEntity;
import com.example.typemoonaddon.entity.SummonedSpiritEntity;
import com.example.typemoonaddon.entity.WraithEntity;
import com.example.typemoonaddon.registry.AddonEntities;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.typemoonworld.api.AddonRegistrar;
import net.xxxjk.typemoonworld.api.ExecutionResult;
import net.xxxjk.typemoonworld.api.MagicCastContext;
import net.xxxjk.typemoonworld.api.MagicDefinitionData;
import net.xxxjk.typemoonworld.api.MagicOption;
import net.xxxjk.typemoonworld.api.MagicPresetHandler;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;

/** Reset implementation for the existing spirit magic IDs. */
public final class SummoningMagicIntegration {
    public static final ResourceLocation PRELUDE = TypeMoonAddon.id("spirit_summoning");
    public static final ResourceLocation WRAITH_SERVITUDE = TypeMoonAddon.id("wraith_servitude");
    public static final ResourceLocation EVIL_SPIRIT_SUMMONING = TypeMoonAddon.id("evil_spirit_summoning");
    private static final String MODE = "summoning_mode";
    private static final String COUNT = "summon_count";
    private static final String COMMAND = "command_action";
    private static final String SUMMON_SIZE = "summon_size";
    private static final String WRAITH_MODE_TAG = "TypeMoonWraithServitudeMode";
    private static final String EVIL_MODE_TAG = "TypeMoonEvilSpiritSummoningMode";
    private static final Set<String> MODES = Set.of("summon", "command");
    private static final Set<String> LEGACY_COMMANDS = Set.of("follow", "wander", "attack", "dismiss");
    private static final Set<String> SUMMON_SIZES = Set.of("large", "small");
    private static final int MAX_SEARCH = 1024;

    private SummoningMagicIntegration() { }

    public static void register() {
        AddonRegistrar addon = TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID);
        boolean definitions = addon.magics().registerDefinition(definition(PRELUDE, 25, false, false, false, null))
                & addon.magics().registerDefinition(definition(WRAITH_SERVITUDE, 25, true, true, true, PRELUDE))
                & addon.magics().registerDefinition(definition(EVIL_SPIRIT_SUMMONING, 35, true, true, true, PRELUDE));
        boolean presets = addon.magics().registerPreset(WRAITH_SERVITUDE, wraithPreset())
                & addon.magics().registerPreset(EVIL_SPIRIT_SUMMONING, evilPreset());
        boolean controls = addon.client().registerControl(WRAITH_SERVITUDE,
                new MagicOption(MODE, MagicOption.Kind.ENUM, "summon", 0, 0, List.of("summon", "command")))
                & addon.client().registerControl(WRAITH_SERVITUDE,
                new MagicOption(COUNT, MagicOption.Kind.INTEGER, "1", 1, 10, List.of()))
                & addon.client().registerControl(WRAITH_SERVITUDE,
                new MagicOption(COMMAND, MagicOption.Kind.ENUM, "follow", 0, 0, List.of("follow", "stay", "attack", "dismiss")))
                & addon.client().registerControl(EVIL_SPIRIT_SUMMONING,
                new MagicOption(MODE, MagicOption.Kind.ENUM, "summon", 0, 0, List.of("summon", "command")))
                & addon.client().registerControl(EVIL_SPIRIT_SUMMONING,
                new MagicOption(SUMMON_SIZE, MagicOption.Kind.ENUM, "large", 0, 0, List.copyOf(SUMMON_SIZES)));
        // The command selector is shared by both command-capable magic IDs.
        controls &= addon.client().registerControl(EVIL_SPIRIT_SUMMONING,
                new MagicOption(COMMAND, MagicOption.Kind.ENUM, "follow", 0, 0, List.of("follow", "stay", "attack", "dismiss")));
        boolean executors = addon.magics().registerExecutor(WRAITH_SERVITUDE, SummoningMagicIntegration::castWraith)
                & addon.magics().registerExecutor(EVIL_SPIRIT_SUMMONING, SummoningMagicIntegration::castEvilSpirit);
        if (!(definitions && presets && controls && executors)) {
            TypeMoonAddon.LOGGER.warn("Spirit magic reset registration contained a duplicate or rejected entry");
        }
    }

    public static void tick(ServerPlayer player) {
        if (player == null || player.tickCount % 300 != 0) return;
        double proficiency = proficiency(player, WRAITH_SERVITUDE);
        double cost = Math.max(1.0D, 10.0D - proficiency * 0.09D);
        for (WraithEntity spirit : ownedWraiths(player)) {
            if (!TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics().mana(player).tryConsume(cost)) spirit.discard();
        }
    }

    private static ExecutionResult castWraith(MagicCastContext context) {
        if (!(context.caster() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)
                || !hasPrelude(player)) return ExecutionResult.FAILED;
        if ("command".equals(mode(player, context.preset(), WRAITH_MODE_TAG))) return commandWraith(player, context.target(), command(context.preset()));
        int max = maxWraithCount(context.proficiency());
        int requested = count(context.preset(), max);
        int spawnCount = Math.min(requested, Math.max(0, max - ownedWraiths(player).size()));
        if (spawnCount <= 0) { notify(player, "message.typemoonworld.wraith_servitude.limit", max); return ExecutionResult.FAILED; }
        if (!mana(player, 5.0D)) return ExecutionResult.FAILED;
        for (int i = 0; i < spawnCount; i++) {
            WraithEntity spirit = AddonEntities.WRAITH.get().create(level);
            if (spirit == null) continue;
            spirit.setOwner(player);
            Vec3 spawn = player.position().add(player.getLookAngle().scale(1.5D));
            spirit.moveTo(spawn.x + (i % 3 - 1) * 0.8D, spawn.y + 0.8D + (i / 3) * 0.35D,
                    spawn.z + (i / 3) * 0.6D, player.getYRot(), 0.0F);
            level.addFreshEntity(spirit);
        }
        notify(player, "message.typemoonworld.wraith_servitude.summoned", spawnCount);
        return ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(10);
    }

    private static ExecutionResult commandWraith(ServerPlayer player, LivingEntity target, String action) {
        List<WraithEntity> spirits = ownedWraiths(player);
        if (spirits.isEmpty()) { notify(player, "message.typemoonworld.wraith_servitude.no_target"); return ExecutionResult.FAILED; }
        if ("dismiss".equals(action)) spirits.forEach(LivingEntity::discard);
        else if (target instanceof WraithEntity owned && player.getUUID().equals(owned.getOwnerId())) applyCommand(List.of(owned), target, action);
        else if (target != null && target.isAlive() && !(target instanceof WraithEntity) && !(target instanceof EvilSpiritEntity)) applyPriority(spirits, target, action);
        else applyCommand(spirits, null, action);
        notify(player, "message.typemoonworld.wraith_servitude.commanded", spirits.size());
        return ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(6);
    }

    private static ExecutionResult castEvilSpirit(MagicCastContext context) {
        if (!(context.caster() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)
                || !hasPrelude(player)) return ExecutionResult.FAILED;
        if ("command".equals(mode(player, context.preset(), EVIL_MODE_TAG))) return commandEvil(player, context.target(), command(context.preset()));
        if (ownedEvilSpirits(player).size() >= 3) { notify(player, "message.typemoonworld.evil_spirit_summoning.limit"); return ExecutionResult.FAILED; }
        if (!mana(player, 80.0D)) return ExecutionResult.FAILED;
        EvilSpiritEntity spirit = ("small".equals(normalizeSize(context.preset())) ? AddonEntities.EVIL_SPIRIT_SMALL : AddonEntities.EVIL_SPIRIT).get().create(level);
        if (spirit == null) return ExecutionResult.FAILED;
        spirit.setOwner(player);
        spirit.configure(context.proficiency());
        spirit.setCommandMode(SummonedSpiritEntity.MODE_FOLLOW);
        Vec3 spawn = player.position().add(player.getLookAngle().scale(2.0D)).add(0.0D, 0.8D, 0.0D);
        spirit.moveTo(spawn.x, spawn.y, spawn.z, player.getYRot(), 0.0F);
        level.addFreshEntity(spirit);
        notify(player, "message.typemoonworld.evil_spirit_summoning.summoned");
        return ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(20);
    }

    private static ExecutionResult commandEvil(ServerPlayer player, LivingEntity target, String action) {
        List<EvilSpiritEntity> spirits = ownedEvilSpirits(player);
        if (spirits.isEmpty()) { notify(player, "message.typemoonworld.evil_spirit_summoning.no_target"); return ExecutionResult.FAILED; }
        if ("dismiss".equals(action)) {
            if (!mana(player, 20.0D)) return ExecutionResult.FAILED;
            spirits.forEach(LivingEntity::discard);
            return ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(8);
        }
        if (target instanceof WraithEntity) { notify(player, "message.typemoonworld.evil_spirit_summoning.cannot_command"); return ExecutionResult.FAILED; }
        if (!mana(player, target instanceof EvilSpiritEntity ? 2.0D : 20.0D)) return ExecutionResult.FAILED;
        if (target instanceof EvilSpiritEntity owned && player.getUUID().equals(owned.getOwnerId())) applyCommand(List.of(owned), target, action);
        else if (target != null && target.isAlive() && !(target instanceof EvilSpiritEntity)) applyPriority(spirits, target, action);
        else { notify(player, "message.typemoonworld.evil_spirit_summoning.cannot_command"); return ExecutionResult.FAILED; }
        return ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(8);
    }

    private static void applyCommand(List<? extends SummonedSpiritEntity> spirits, LivingEntity target, String action) {
        for (SummonedSpiritEntity spirit : spirits) {
            spirit.setCommandMode("attack".equals(action) ? SummonedSpiritEntity.MODE_ATTACK : "stay".equals(action) ? SummonedSpiritEntity.MODE_WANDER : SummonedSpiritEntity.MODE_FOLLOW);
            if (target != null && target.isAlive()) spirit.setTarget(target);
        }
    }

    private static void applyPriority(List<? extends SummonedSpiritEntity> spirits, LivingEntity target, String action) {
        for (SummonedSpiritEntity spirit : spirits) {
            spirit.setPriorityTarget(target);
            spirit.setCommandMode("attack".equals(action) ? SummonedSpiritEntity.MODE_ATTACK : SummonedSpiritEntity.MODE_WANDER);
        }
    }

    private static boolean hasPrelude(LivingEntity caster) {
        return TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics().knowledge(caster).isLearned(PRELUDE);
    }

    public static boolean toggleMode(ServerPlayer player, String magicId) {
        if (player == null) return false;
        String tag = "wraith_servitude".equals(magicId) ? WRAITH_MODE_TAG : EVIL_MODE_TAG;
        String next = "command".equals(player.getPersistentData().getString(tag)) ? "summon" : "command";
        player.getPersistentData().putString(tag, next);
        notify(player, "message.typemoonworld.summoning.mode_changed", Component.translatable("magic.option.typemoonworld.summoning_mode." + next));
        return true;
    }

    private static String mode(ServerPlayer player, CompoundTag preset, String tag) {
        String persistent = player.getPersistentData().getString(tag);
        if (MODES.contains(persistent)) return persistent;
        String value = preset == null ? "summon" : preset.getString(MODE);
        return LEGACY_COMMANDS.contains(value) || "command".equals(value) ? "command" : "summon";
    }

    private static int maxWraithCount(double proficiency) {
        return Math.max(1, Math.min(10, 1 + (int)Math.floor(Math.max(0.0D, Math.min(100.0D, proficiency)) / 10.0D)));
    }

    private static int count(CompoundTag preset, int max) {
        int value = preset == null ? 1 : preset.getInt(COUNT);
        return Math.max(1, Math.min(max, value <= 0 ? 1 : value));
    }

    private static String normalizeSize(CompoundTag preset) {
        String size = preset == null ? "large" : preset.getString(SUMMON_SIZE);
        return SUMMON_SIZES.contains(size) ? size : "large";
    }

    private static String command(CompoundTag preset) {
        String value = preset == null ? "follow" : preset.getString(COMMAND);
        if (LEGACY_COMMANDS.contains(value)) return "attack".equals(value) ? "attack" : "dismiss".equals(value) ? "dismiss" : "follow";
        return Set.of("follow", "stay", "attack", "dismiss").contains(value) ? value : "follow";
    }

    private static double proficiency(LivingEntity entity, ResourceLocation id) {
        return TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics().knowledge(entity).proficiency(id);
    }

    private static boolean mana(ServerPlayer player, double amount) {
        if (TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics().mana(player).tryConsume(amount)) return true;
        notify(player, "message.typemoonworld.magic.not_enough_mana");
        return false;
    }

    private static List<WraithEntity> ownedWraiths(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(WraithEntity.class, player.getBoundingBox().inflate(MAX_SEARCH), s -> player.getUUID().equals(s.getOwnerId()));
    }

    private static List<EvilSpiritEntity> ownedEvilSpirits(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(EvilSpiritEntity.class, player.getBoundingBox().inflate(MAX_SEARCH), s -> player.getUUID().equals(s.getOwnerId()));
    }

    private static void notify(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args), true);
    }

    private static MagicDefinitionData definition(ResourceLocation id, int complexity, boolean selectable,
                                                   boolean crestAllowed, boolean copyable, ResourceLocation prerequisite) {
        return new MagicDefinitionData(id, "magic.typemoonworld." + id.getPath() + ".name", TypeMoonAddon.id("special"), TypeMoonAddon.id("none"), complexity, 1, 0.0D, 0.0D, 10, true, selectable, crestAllowed, false, !selectable, copyable, 0, 20, prerequisite, 0.0D, List.of());
    }

    private static MagicPresetHandler wraithPreset() {
        return new MagicPresetHandler() {
            @Override public CompoundTag normalize(CompoundTag input) {
                CompoundTag output = new CompoundTag();
                String mode = input == null ? "summon" : input.getString(MODE);
                output.putString(MODE, MODES.contains(mode) || LEGACY_COMMANDS.contains(mode) ? mode : "summon");
                int count = input == null ? 1 : input.getInt(COUNT);
                output.putInt(COUNT, Math.max(1, Math.min(10, count <= 0 ? 1 : count)));
                output.putString(COMMAND, command(input));
                return output;
            }
        };
    }

    private static MagicPresetHandler evilPreset() {
        return new MagicPresetHandler() {
            @Override public CompoundTag normalize(CompoundTag input) {
                CompoundTag output = new CompoundTag();
                String mode = input == null ? "summon" : input.getString(MODE);
                output.putString(MODE, MODES.contains(mode) || LEGACY_COMMANDS.contains(mode) ? mode : "summon");
                output.putString(SUMMON_SIZE, normalizeSize(input));
                output.putString(COMMAND, command(input));
                return output;
            }
        };
    }
}
