package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.data.ImaginarySpaceData;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import com.example.typemoonaddon.magic.MatouSakuraMasterProfile;
import net.xxxjk.typemoonworld.api.AddonRegistrar;
import net.xxxjk.typemoonworld.api.ExecutionResult;
import net.xxxjk.typemoonworld.api.MagicAttributes;
import net.xxxjk.typemoonworld.api.MagicDefinitionData;
import net.xxxjk.typemoonworld.api.MagicOption;
import net.xxxjk.typemoonworld.api.MagicPresetHandler;
import net.xxxjk.typemoonworld.api.MagicRegistry;
import net.xxxjk.typemoonworld.api.MasterProfileContext;
import net.xxxjk.typemoonworld.api.MasterProfileData;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class SakuraTypeMoonIntegration {
    public static final ResourceLocation IMAGINARY_STORAGE = TypeMoonAddon.id("imaginary_absorption");
    public static final ResourceLocation IMAGINARY_ABSORPTION = TypeMoonAddon.id("imaginary_absorption_evolved");
    public static final ResourceLocation SHADOW_MATERIALIZATION = TypeMoonAddon.id("shadow_materialization");
    public static final ResourceLocation BLACK_MUD_CONTROL = TypeMoonAddon.id("black_mud_control");
    public static final ResourceLocation SUMMON_BLACK_MUD = TypeMoonAddon.id("summon_black_mud");
    public static final ResourceLocation SHADOW_BINDING = TypeMoonAddon.id("shadow_binding");
    public static final ResourceLocation SHADOW_TRANSFER = TypeMoonAddon.id("shadow_transfer");
    public static final ResourceLocation HEROIC_SPIRIT_DEVOURER = TypeMoonAddon.id("heroic_spirit_devourer");
    public static final ResourceLocation FORBIDDEN_MAGIC = TypeMoonAddon.id("forbidden_magic");
    public static final ResourceLocation SHADOW_ART = TypeMoonAddon.id("shadow_art");

    public static final ResourceLocation MATOU_SAKURA = TypeMoonAddon.id("matou_sakura");
    public static final ResourceLocation MATOU_SAKURA_ALTER = TypeMoonAddon.id("matou_sakura_alter");
    public static final ResourceLocation MATOU_SAKURA_FHA = TypeMoonAddon.id("matou_sakura_fha");

    public static final ResourceLocation MAGIC_BASICS_CATEGORY = TypeMoonAddon.id("basic");
    public static final ResourceLocation IMAGINARY_MAGIC_SCHOOL = TypeMoonAddon.id("imaginary_magic");

    public static final String MODE_PRESET_KEY = "imaginary_mode";
    public static final String SHADOW_COMMAND_PRESET_KEY = "shadow_command_mode";
    public static final String BLACK_MUD_SUMMON_PRESET_KEY = "black_mud_summon_mode";
    public static final String SHADOW_ART_MODE_PRESET_KEY = "shadow_art_mode";

    private static final List<ResourceLocation> IMAGINARY_REQUIREMENT = List.of(MagicAttributes.IMAGINARY_NUMBER);
    private static MagicRegistry magicRegistry;

    public static void register() {
        if (!TypeMoonWorldApi.isCompatible(TypeMoonWorldApi.API_VERSION)) {
            throw new IllegalStateException("Incompatible Type Moon World API version " + TypeMoonWorldApi.API_VERSION);
        }

        AddonRegistrar addon = TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID);
        magicRegistry = addon.magics();

        boolean mastersRegistered = registerMasterProfiles(addon);
        boolean definitionsRegistered = registerDefinitions(magicRegistry);
        boolean presetsRegistered = registerPresets(magicRegistry);
        boolean controlsRegistered = registerControls(addon);
        boolean executorsRegistered = registerExecutors(magicRegistry);

        if (mastersRegistered && definitionsRegistered && presetsRegistered && controlsRegistered && executorsRegistered) {
            TypeMoonAddon.LOGGER.info("Registered Sakura addon Type Moon integration IDs");
        } else {
            TypeMoonAddon.LOGGER.warn(
                    "Sakura addon Type Moon integration finished with duplicate or rejected registrations: masters={}, definitions={}, presets={}, controls={}, executors={}",
                    mastersRegistered,
                    definitionsRegistered,
                    presetsRegistered,
                    controlsRegistered,
                    executorsRegistered
            );
        }
    }

    private static boolean registerMasterProfiles(AddonRegistrar addon) {
        boolean stayNight = addon.masters().register(
                new MasterProfileData(
                        MATOU_SAKURA,
                        "item.typemoonworld.master_card_matou_sakura",
                        "sakura",
                        1_000.0D,
                        8.0D,
                        4
                ),
                context -> initializeMaster(context, MasterVariant.STAY_NIGHT)
        );
        boolean alter = addon.masters().register(
                new MasterProfileData(
                        MATOU_SAKURA_ALTER,
                        "item.typemoonworld.master_card_matou_sakura_alter",
                        "sakura",
                        1_000.0D,
                        8.0D,
                        16
                ),
                context -> initializeMaster(context, MasterVariant.ALTER)
        );
        boolean fha = addon.masters().register(
                new MasterProfileData(
                        MATOU_SAKURA_FHA,
                        "item.typemoonworld.master_card_matou_sakura_fha",
                        "sakura",
                        1_000.0D,
                        8.0D,
                        4
                ),
                context -> initializeMaster(context, MasterVariant.FHA)
        );
        return stayNight && alter && fha;
    }

    private static void initializeMaster(MasterProfileContext context, MasterVariant variant) {
        learn(context, IMAGINARY_STORAGE, 40.0D);
        grantMasterCardAttributes(context.player());
        if (variant == MasterVariant.FHA) {
            learn(context, IMAGINARY_ABSORPTION, 60.0D);
            learn(context, SHADOW_MATERIALIZATION, 40.0D);
            learn(context, BLACK_MUD_CONTROL, 40.0D);
            learn(context, SUMMON_BLACK_MUD, 40.0D);
            learn(context, SHADOW_BINDING, 40.0D);
            learn(context, SHADOW_TRANSFER, 40.0D);
            learn(context, HEROIC_SPIRIT_DEVOURER, 40.0D);
            learn(context, FORBIDDEN_MAGIC, 70.0D);
            learn(context, SHADOW_ART, 70.0D);
            context.player().getData(com.example.typemoonaddon.registry.AddonAttachments.IMAGINARY_SPACE.get()).unlockForbiddenMagic();
            context.player().getData(com.example.typemoonaddon.registry.AddonAttachments.IMAGINARY_SPACE.get()).unlockShadowArt();
            com.example.typemoonaddon.registry.AddonAttachments.sync(context.player(), com.example.typemoonaddon.registry.AddonAttachments.IMAGINARY_SPACE);
        }
        context.mana().add(context.profile().maximumMana());
    }

    private static void learn(MasterProfileContext context, ResourceLocation magicId, double proficiency) {
        context.magicKnowledge().learn(magicId);
        context.magicKnowledge().setProficiency(magicId, proficiency);
    }

    private static boolean registerDefinitions(MagicRegistry registry) {
        return registry.registerDefinition(definition(IMAGINARY_STORAGE, "magic.typemoonworld.imaginary_storage.name", 0.0D, 0))
                & registry.registerDefinition(definition(IMAGINARY_ABSORPTION, "magic.typemoonworld.imaginary_absorption_evolved.name", 0.0D, 0))
                & registry.registerDefinition(definition(SHADOW_MATERIALIZATION, "magic.typemoonworld.shadow_materialization.name", 0.0D, 0))
                & registry.registerDefinition(definition(BLACK_MUD_CONTROL, "magic.typemoonworld.black_mud_control.name", 0.0D, 0))
                & registry.registerDefinition(definition(SUMMON_BLACK_MUD, "magic.typemoonworld.summon_black_mud.name", 0.0D, 20))
                & registry.registerDefinition(definition(SHADOW_BINDING, "magic.typemoonworld.shadow_binding.name", 50.0D, 20))
                & registry.registerDefinition(definition(SHADOW_TRANSFER, "magic.typemoonworld.shadow_transfer.name", 0.0D, 0))
                & registry.registerDefinition(definition(HEROIC_SPIRIT_DEVOURER, "magic.typemoonworld.heroic_spirit_devourer.name", 0.0D, 40))
                & registry.registerDefinition(definition(FORBIDDEN_MAGIC, "magic.typemoonworld.forbidden_magic.name", 0.0D, 0))
                & registry.registerDefinition(definition(SHADOW_ART, "magic.typemoonworld.shadow_art.name", 0.0D, 0));
    }

    private static MagicDefinitionData definition(ResourceLocation id, String nameKey, double manaCost, int cooldownTicks) {
        return new MagicDefinitionData(
                id,
                nameKey,
                MAGIC_BASICS_CATEGORY,
                IMAGINARY_MAGIC_SCHOOL,
                manaCost,
                cooldownTicks,
                true,
                true,
                false,
                false,
                false,
                0,
                0,
                IMAGINARY_REQUIREMENT
        );
    }

    private static boolean registerPresets(MagicRegistry registry) {
        return registry.registerPreset(IMAGINARY_STORAGE, new EnumPreset(MODE_PRESET_KEY, "storage", List.of("storage", "protection")))
                & registry.registerPreset(IMAGINARY_ABSORPTION, new EnumPreset(MODE_PRESET_KEY, "storage", List.of("storage", "protection")))
                & registry.registerPreset(BLACK_MUD_CONTROL, new EnumPreset(SHADOW_COMMAND_PRESET_KEY, "free", List.of("free", "spread", "hold", "attack_around", "gather", "hunt", "dismiss")))
                & registry.registerPreset(SUMMON_BLACK_MUD, new EnumPreset(BLACK_MUD_SUMMON_PRESET_KEY, "release", List.of("release", "dismiss")))
                & registry.registerPreset(SHADOW_ART, new EnumPreset(SHADOW_ART_MODE_PRESET_KEY, "balanced", List.of("auto_defense", "auto_attack", "balanced")));
    }

    private static boolean registerControls(AddonRegistrar addon) {
        return addon.client().registerControl(IMAGINARY_STORAGE, enumOption(MODE_PRESET_KEY, "storage", "storage", "protection"))
                & addon.client().registerControl(IMAGINARY_ABSORPTION, enumOption(MODE_PRESET_KEY, "storage", "storage", "protection"))
                & addon.client().registerControl(BLACK_MUD_CONTROL, enumOption(SHADOW_COMMAND_PRESET_KEY, "free", "free", "spread", "hold", "attack_around", "gather", "hunt", "dismiss"))
                & addon.client().registerControl(SUMMON_BLACK_MUD, enumOption(BLACK_MUD_SUMMON_PRESET_KEY, "release", "release", "dismiss"))
                & addon.client().registerControl(SHADOW_ART, enumOption(SHADOW_ART_MODE_PRESET_KEY, "balanced", "auto_defense", "auto_attack", "balanced"));
    }

    private static MagicOption enumOption(String key, String defaultValue, String... values) {
        return new MagicOption(key, MagicOption.Kind.ENUM, defaultValue, 0, 0, List.of(values));
    }

    private static boolean registerExecutors(MagicRegistry registry) {
        return registry.registerExecutor(IMAGINARY_STORAGE, context -> castStorage(context.serverPlayer(), false))
                & registry.registerExecutor(IMAGINARY_ABSORPTION, context -> castStorage(context.serverPlayer(), true))
                & registry.registerExecutor(SHADOW_MATERIALIZATION, context -> castShadowMaterialization(context.serverPlayer()))
                & registry.registerExecutor(BLACK_MUD_CONTROL, context -> castBlackMudControl(context.serverPlayer(), context.preset()))
                & registry.registerExecutor(SUMMON_BLACK_MUD, context -> castSummonBlackMud(context.serverPlayer(), context.preset()))
                & registry.registerExecutor(SHADOW_BINDING, context -> castShadowBinding(context.serverPlayer()))
                & registry.registerExecutor(SHADOW_TRANSFER, context -> castShadowTransfer(context.serverPlayer()))
                & registry.registerExecutor(HEROIC_SPIRIT_DEVOURER, context -> castHeroicSpiritDevourer(context.serverPlayer()))
                & registry.registerExecutor(FORBIDDEN_MAGIC, context -> castForbiddenMagic(context.serverPlayer()))
                & registry.registerExecutor(SHADOW_ART, context -> castShadowArt(context.serverPlayer(), context.preset()));
    }

    private static ExecutionResult castStorage(ServerPlayer player, boolean absorption) {
        return player != null && SakuraImaginaryStorageService.cast(player, absorption)
                ? ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(0)
                : ExecutionResult.FAILED;
    }

    private static ExecutionResult castShadowMaterialization(ServerPlayer player) {
        return player != null && SakuraShadowMaterializationService.beginCharging(player)
                ? ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(0)
                : ExecutionResult.FAILED;
    }

    private static ExecutionResult castBlackMudControl(ServerPlayer player, CompoundTag preset) {
        if (player == null) {
            return ExecutionResult.FAILED;
        }
        ImaginarySpaceData.ShadowCommandMode mode = readShadowCommandMode(preset);
        return SakuraBlackMudControlService.selectMode(player, mode) && SakuraBlackMudControlService.cast(player)
                ? ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(0)
                : ExecutionResult.FAILED;
    }

    private static ExecutionResult castSummonBlackMud(ServerPlayer player, CompoundTag preset) {
        if (player == null) {
            return ExecutionResult.FAILED;
        }
        ImaginarySpaceData.BlackMudSummonMode mode = readBlackMudSummonMode(preset);
        return SakuraSummonBlackMudService.selectMode(player, mode) && SakuraSummonBlackMudService.cast(player)
                ? ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(0)
                : ExecutionResult.FAILED;
    }

    private static ExecutionResult castShadowBinding(ServerPlayer player) {
        return player != null && SakuraShadowBindingMagicService.cast(player)
                ? ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(0)
                : ExecutionResult.FAILED;
    }

    private static ExecutionResult castShadowTransfer(ServerPlayer player) {
        return player != null && SakuraShadowTransferService.cast(player)
                ? ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(0)
                : ExecutionResult.FAILED;
    }

    private static ExecutionResult castHeroicSpiritDevourer(ServerPlayer player) {
        return player != null && isHeroicSpiritDevourerLearned(player)
                && SakuraPollutionService.summonCorruptedServants(player) > 0
                ? ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(40)
                : ExecutionResult.FAILED;
    }

    private static ExecutionResult castForbiddenMagic(ServerPlayer player) {
        return player != null && SakuraForbiddenMagicService.cast(player)
                ? ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(0)
                : ExecutionResult.FAILED;
    }

    private static ExecutionResult castShadowArt(ServerPlayer player, CompoundTag preset) {
        if (player == null) {
            return ExecutionResult.FAILED;
        }
        ImaginarySpaceData.ShadowArtMode mode = readShadowArtMode(preset);
        return SakuraShadowArtService.selectMode(player, mode) && SakuraShadowArtService.cast(player)
                ? ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(0)
                : ExecutionResult.FAILED;
    }

    private static ImaginarySpaceData.ShadowCommandMode readShadowCommandMode(CompoundTag preset) {
        String value = preset == null ? "" : preset.getString(SHADOW_COMMAND_PRESET_KEY);
        for (ImaginarySpaceData.ShadowCommandMode mode : ImaginarySpaceData.ShadowCommandMode.values()) {
            if (mode.serializedName().equals(value)) {
                return mode;
            }
        }
        return ImaginarySpaceData.ShadowCommandMode.FREE;
    }

    private static ImaginarySpaceData.BlackMudSummonMode readBlackMudSummonMode(CompoundTag preset) {
        String value = preset == null ? "" : preset.getString(BLACK_MUD_SUMMON_PRESET_KEY);
        for (ImaginarySpaceData.BlackMudSummonMode mode : ImaginarySpaceData.BlackMudSummonMode.values()) {
            if (mode.serializedName().equals(value)) {
                return mode;
            }
        }
        return ImaginarySpaceData.BlackMudSummonMode.RELEASE;
    }

    private static ImaginarySpaceData.ShadowArtMode readShadowArtMode(CompoundTag preset) {
        String value = preset == null ? "" : preset.getString(SHADOW_ART_MODE_PRESET_KEY);
        for (ImaginarySpaceData.ShadowArtMode mode : ImaginarySpaceData.ShadowArtMode.values()) {
            if (mode.serializedName().equals(value)) {
                return mode;
            }
        }
        return ImaginarySpaceData.ShadowArtMode.BALANCED;
    }

    public static MagicRegistry registry() {
        if (magicRegistry == null) {
            magicRegistry = TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics();
        }
        return magicRegistry;
    }

    public static boolean learnAndGrantAttribute(ServerPlayer player) {
        return player != null && SakuraImaginaryStorageService.learnAndGrantAttribute(player);
    }

    public static boolean ensureImaginaryAttribute(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        boolean changed = !vars.is_magus || !vars.player_magic_attributes_imaginary_number;
        vars.is_magus = true;
        vars.player_magic_attributes_imaginary_number = true;
        if (changed) {
            vars.syncPlayerVariables(player);
        }
        return true;
    }

    public static boolean grantWaterAttribute(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        boolean changed = !vars.is_magus || !vars.player_magic_attributes_water;
        vars.is_magus = true;
        vars.player_magic_attributes_water = true;
        if (changed) {
            vars.syncPlayerVariables(player);
        }
        return true;
    }

    public static boolean grantMasterCardAttributes(ServerPlayer player) {
        return ensureImaginaryAttribute(player) && grantWaterAttribute(player);
    }

    public static boolean assimilateCrestWorm(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        ImaginarySpaceData data = player.getData(com.example.typemoonaddon.registry.AddonAttachments.IMAGINARY_SPACE.get());
        if (!data.learned() || data.crestWormAssimilated()) {
            return false;
        }
        boolean ready = ensureKnowledge(player, IMAGINARY_STORAGE)
                && ensureKnowledge(player, IMAGINARY_ABSORPTION)
                && ensureImaginaryAttribute(player)
                && grantWaterAttribute(player);
        return ready && data.assimilateCrestWorm();
    }

    public static boolean performHolyGrailRitual(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        ImaginarySpaceData data = player.getData(com.example.typemoonaddon.registry.AddonAttachments.IMAGINARY_SPACE.get());
        if (!data.learned() || data.grailWormAscended()) {
            return false;
        }
        boolean baseReady = data.crestWormAssimilated() || assimilateCrestWorm(player);
        boolean ready = baseReady
                && ensureImaginaryAttribute(player)
                && grantWaterAttribute(player);
        return ready && data.ascendGrailWorm();
    }

    public static boolean ensureGrailWormPower(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        ImaginarySpaceData data = player.getData(com.example.typemoonaddon.registry.AddonAttachments.IMAGINARY_SPACE.get());
        if (!data.grailWormAscended()) {
            return true;
        }
        if (data.grailErosionFull() && data.shadowArtState() == ImaginarySpaceData.ShadowArtState.LOCKED) {
            data.unlockShadowArt();
        }
        normalizeBlackSakuraLoadout(player, data);
        return ensureImaginaryAttribute(player) && grantWaterAttribute(player);
    }

    public static boolean isAlterBlackSakura(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        return MatouSakuraMasterProfile.variant(player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).master_card_id)
                == MatouSakuraMasterProfile.Variant.ALTER;
    }

    public static boolean normalizeBlackSakuraLoadout(ServerPlayer player, ImaginarySpaceData data) {
        if (player == null || data == null || !data.grailWormAscended() || !isAlterBlackSakura(player)) {
            return false;
        }
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        boolean full = data.grailErosionFull();
        boolean changed = false;
        if (full) {
            changed |= ensureKnowledge(player, IMAGINARY_ABSORPTION);
            changed |= ensureKnowledge(player, SHADOW_MATERIALIZATION);
            changed |= ensureKnowledge(player, BLACK_MUD_CONTROL);
            changed |= ensureKnowledge(player, SUMMON_BLACK_MUD);
            changed |= ensureKnowledge(player, SHADOW_BINDING);
            changed |= ensureKnowledge(player, SHADOW_TRANSFER);
            changed |= ensureKnowledge(player, HEROIC_SPIRIT_DEVOURER);
            changed |= ensureKnowledge(player, FORBIDDEN_MAGIC);
            changed |= ensureKnowledge(player, SHADOW_ART);
            changed |= vars.migrateMagicId(IMAGINARY_STORAGE.toString(), IMAGINARY_ABSORPTION.toString());
        } else {
            changed |= ensureKnowledge(player, IMAGINARY_STORAGE);
            changed |= vars.migrateMagicId(IMAGINARY_ABSORPTION.toString(), IMAGINARY_STORAGE.toString());
            changed |= vars.removeLearnedMagic(SHADOW_MATERIALIZATION.toString());
            changed |= vars.removeLearnedMagic(BLACK_MUD_CONTROL.toString());
            changed |= vars.removeLearnedMagic(SUMMON_BLACK_MUD.toString());
            changed |= vars.removeLearnedMagic(SHADOW_BINDING.toString());
            changed |= vars.removeLearnedMagic(SHADOW_TRANSFER.toString());
            changed |= vars.removeLearnedMagic(HEROIC_SPIRIT_DEVOURER.toString());
            changed |= vars.removeLearnedMagic(FORBIDDEN_MAGIC.toString());
            changed |= vars.removeLearnedMagic(SHADOW_ART.toString());
        }
        return changed;
    }

    public static boolean ensureForbiddenMagicKnowledge(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        player.getData(com.example.typemoonaddon.registry.AddonAttachments.IMAGINARY_SPACE.get()).unlockForbiddenMagic();
        return SakuraRuleBreakerDispelService.unlockForbiddenMagicKnowledge(player);
    }

    public static boolean ensureShadowArtKnowledge(ServerPlayer player) {
        return SakuraRuleBreakerDispelService.unlockShadowArtKnowledge(player);
    }

    private static boolean ensureKnowledge(ServerPlayer player, ResourceLocation id) {
        var knowledge = registry().knowledge(player);
        return knowledge.isLearned(id) || knowledge.learn(id);
    }

    public static boolean isLearned(ServerPlayer player) {
        return player != null
                && (SakuraImaginaryStorageService.isLearned(player) || SakuraImaginaryStorageService.isAbsorptionLearned(player))
                && hasImaginaryAttribute(player);
    }

    public static boolean isAbsorptionLearned(ServerPlayer player) {
        return player != null
                && SakuraImaginaryStorageService.isAbsorptionLearned(player)
                && hasImaginaryAttribute(player);
    }

    public static boolean isShadowMaterializationLearned(ServerPlayer player) {
        return player != null
                && player.getData(com.example.typemoonaddon.registry.AddonAttachments.IMAGINARY_SPACE.get()).grailWormAscended()
                && SakuraTypeMoonIntegration.registry().knowledge(player).isLearned(SHADOW_MATERIALIZATION)
                && hasImaginaryAttribute(player);
    }

    public static boolean isBlackMudControlLearned(ServerPlayer player) {
        return player != null
                && player.getData(com.example.typemoonaddon.registry.AddonAttachments.IMAGINARY_SPACE.get()).grailWormAscended()
                && SakuraTypeMoonIntegration.registry().knowledge(player).isLearned(BLACK_MUD_CONTROL)
                && hasImaginaryAttribute(player);
    }

    public static boolean isSummonBlackMudLearned(ServerPlayer player) {
        return player != null
                && player.getData(com.example.typemoonaddon.registry.AddonAttachments.IMAGINARY_SPACE.get()).grailWormAscended()
                && SakuraTypeMoonIntegration.registry().knowledge(player).isLearned(SUMMON_BLACK_MUD)
                && hasImaginaryAttribute(player);
    }

    public static boolean isShadowBindingLearned(ServerPlayer player) {
        return player != null
                && player.getData(com.example.typemoonaddon.registry.AddonAttachments.IMAGINARY_SPACE.get()).grailWormAscended()
                && SakuraTypeMoonIntegration.registry().knowledge(player).isLearned(SHADOW_BINDING)
                && hasImaginaryAttribute(player);
    }

    public static boolean isShadowTransferLearned(ServerPlayer player) {
        return player != null
                && player.getData(com.example.typemoonaddon.registry.AddonAttachments.IMAGINARY_SPACE.get()).grailWormAscended()
                && SakuraTypeMoonIntegration.registry().knowledge(player).isLearned(SHADOW_TRANSFER)
                && hasImaginaryAttribute(player);
    }

    public static boolean isShadowArtLearned(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        ImaginarySpaceData data = player.getData(com.example.typemoonaddon.registry.AddonAttachments.IMAGINARY_SPACE.get());
        return data.shadowArtUnlocked()
                && data.grailErosionFull()
                && SakuraTypeMoonIntegration.registry().knowledge(player).isLearned(SHADOW_ART)
                && hasImaginaryAttribute(player);
    }

    public static boolean isHeroicSpiritDevourerLearned(ServerPlayer player) {
        return player != null
                && player.getData(com.example.typemoonaddon.registry.AddonAttachments.IMAGINARY_SPACE.get()).grailWormAscended()
                && SakuraTypeMoonIntegration.registry().knowledge(player).isLearned(HEROIC_SPIRIT_DEVOURER)
                && hasImaginaryAttribute(player);
    }

    public static boolean isForbiddenMagicLearned(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        ImaginarySpaceData data = player.getData(com.example.typemoonaddon.registry.AddonAttachments.IMAGINARY_SPACE.get());
        return data.forbiddenMagicUnlocked()
                && !data.grailWormAscended()
                && data.crestWormAssimilated()
                && SakuraTypeMoonIntegration.registry().knowledge(player).isLearned(FORBIDDEN_MAGIC)
                && hasImaginaryAttribute(player);
    }

    public static float imaginaryDamage(ServerPlayer caster, LivingEntity target, float baseDamage) {
        if (caster == null || target == null || baseDamage <= 0.0F) {
            return 0.0F;
        }
        return caster.getData(com.example.typemoonaddon.registry.AddonAttachments.IMAGINARY_SPACE.get()).grailWormAscended()
                && ServantIdentityHelper.isServantLike(target)
                ? baseDamage * com.example.typemoonaddon.config.GameplayConfig.GRAIL_SERVANT_DAMAGE_MULTIPLIER
                : baseDamage;
    }

    public static double currentMana(LivingEntity entity) {
        return entity == null ? 0.0D : registry().mana(entity).current();
    }

    public static double restoreMana(LivingEntity entity, double amount) {
        if (entity == null || amount <= 0.0D) {
            return 0.0D;
        }
        var mana = registry().mana(entity);
        double restored = Math.min(amount, Math.max(0.0D, mana.maximum() - mana.current()));
        if (restored > 0.0D) {
            mana.add(restored);
        }
        return restored;
    }

    public static double transferMana(LivingEntity source, LivingEntity recipient, double maximumAmount) {
        if (source == null || recipient == null || source == recipient || maximumAmount <= 0.0D) {
            return 0.0D;
        }
        var sourceMana = registry().mana(source);
        var recipientMana = registry().mana(recipient);
        double amount = Math.min(
                maximumAmount,
                Math.min(sourceMana.current(), recipientMana.maximum() - recipientMana.current())
        );
        if (amount <= 0.0D || !sourceMana.tryConsume(amount)) {
            return 0.0D;
        }
        recipientMana.add(amount);
        return amount;
    }

    private static boolean hasImaginaryAttribute(ServerPlayer player) {
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        return vars.is_magus && vars.player_magic_attributes_imaginary_number;
    }

    public static boolean tryConsumeMana(ServerPlayer player, double amount) {
        return player != null && SakuraImaginaryStorageService.tryConsumeMana(player, amount);
    }

    public static void refundMana(ServerPlayer player, double amount) {
        if (player != null) {
            restoreMana(player, amount);
        }
    }

    public static void addProficiency(ServerPlayer player, double amount) {
        if (player != null) {
            SakuraImaginaryStorageService.addProficiency(player, amount);
        }
    }

    private enum MasterVariant {
        STAY_NIGHT,
        ALTER,
        FHA
    }

    private record EnumPreset(String key, String defaultValue, List<String> values) implements MagicPresetHandler {
        @Override
        public CompoundTag normalize(CompoundTag input) {
            CompoundTag normalized = new CompoundTag();
            String requested = input == null ? "" : input.getString(key);
            normalized.putString(key, values.contains(requested) ? requested : defaultValue);
            return normalized;
        }
    }

    private SakuraTypeMoonIntegration() {
    }
}
