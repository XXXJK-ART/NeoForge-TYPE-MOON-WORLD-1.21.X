package io.github.typemoonaddon.magic;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.client.TypeMoonClientModeBridge;
import io.github.typemoonaddon.compat.TypeMoonAttributeBridge;
import io.github.typemoonaddon.compat.TypeMoonServantBridge;
import io.github.typemoonaddon.config.GameplayConfig;
import io.github.typemoonaddon.data.ImaginarySpaceData.MagicMode;
import io.github.typemoonaddon.data.ImaginarySpaceData.BlackMudSummonMode;
import io.github.typemoonaddon.data.ImaginarySpaceData.ShadowCommandMode;
import io.github.typemoonaddon.data.ImaginarySpaceData.ShadowArtMode;
import io.github.typemoonaddon.registry.ModAttachments;
import io.github.typemoonaddon.shadowlogic.magic.ShadowArtService;
import io.github.typemoonaddon.shadowlogic.magic.ShadowBindingMagicService;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.xxxjk.typemoonworld.api.AddonRegistrar;
import net.xxxjk.typemoonworld.api.ExecutionResult;
import net.xxxjk.typemoonworld.api.MagicDefinitionData;
import net.xxxjk.typemoonworld.api.MagicOption;
import net.xxxjk.typemoonworld.api.MagicPresetHandler;
import net.xxxjk.typemoonworld.api.MagicRegistry;
import net.xxxjk.typemoonworld.api.MagicAttributes;
import net.xxxjk.typemoonworld.api.MagicAvailabilityProvider;
import net.xxxjk.typemoonworld.api.MasterProfileData;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public final class TypeMoonIntegration {
    private static final List<ResourceLocation> IMAGINARY_REQUIREMENT = List.of(MagicAttributes.IMAGINARY_NUMBER);
    /**
     * The path stays unchanged so existing learned-magic and proficiency data
     * remain valid. Only the player-facing name changed to Imaginary Storage.
     */
    public static final ResourceLocation IMAGINARY_STORAGE = TypeMoonAddon.id("imaginary_absorption");
    private static final ResourceLocation LEGACY_IMAGINARY_SHADOW = TypeMoonAddon.id("imaginary_shadow");
    public static final ResourceLocation IMAGINARY_ABSORPTION = TypeMoonAddon.id("imaginary_absorption_evolved");
    public static final ResourceLocation SHADOW_MATERIALIZATION = TypeMoonAddon.id("shadow_materialization");
    public static final ResourceLocation BLACK_MUD_CONTROL = TypeMoonAddon.id("black_mud_control");
    public static final ResourceLocation SUMMON_BLACK_MUD = TypeMoonAddon.id("summon_black_mud");
    public static final ResourceLocation SHADOW_BINDING = TypeMoonAddon.id("shadow_binding");
    public static final ResourceLocation SHADOW_TRANSFER = TypeMoonAddon.id("shadow_transfer");
    public static final ResourceLocation HEROIC_SPIRIT_DEVOURER = TypeMoonAddon.id("heroic_spirit_devourer");
    public static final ResourceLocation FORBIDDEN_MAGIC = TypeMoonAddon.id("forbidden_magic");
    public static final ResourceLocation SHADOW_ART = TypeMoonAddon.id("shadow_art");
    public static final ResourceLocation MAGIC_BASICS_CATEGORY = ResourceLocation.fromNamespaceAndPath("typemoonworld", "basic");
    public static final ResourceLocation IMAGINARY_MAGIC_SCHOOL = TypeMoonAddon.id("imaginary_magic");
    public static final String MODE_PRESET_KEY = "imaginary_mode";
    public static final String SHADOW_COMMAND_PRESET_KEY = "shadow_command_mode";
    public static final String BLACK_MUD_SUMMON_PRESET_KEY = "black_mud_summon_mode";
    public static final String SHADOW_ART_MODE_PRESET_KEY = "shadow_art_mode";

    private static MagicRegistry magicRegistry;

    public static void register() {
        if (!TypeMoonWorldApi.isCompatible(TypeMoonWorldApi.API_VERSION)) {
            throw new IllegalStateException("Incompatible Type Moon World API version " + TypeMoonWorldApi.API_VERSION);
        }

        AddonRegistrar addon = TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID);
        TypeMoonAttributeBridge.register();
        addon.registerMagicAvailability(new MagicAvailabilityProvider() {
            private final java.util.Set<ResourceLocation> ids = java.util.Set.of(
                IMAGINARY_STORAGE, IMAGINARY_ABSORPTION, SHADOW_MATERIALIZATION, BLACK_MUD_CONTROL,
                SUMMON_BLACK_MUD, SHADOW_BINDING, SHADOW_TRANSFER, HEROIC_SPIRIT_DEVOURER,
                FORBIDDEN_MAGIC, SHADOW_ART
            );
            @Override public java.util.Set<ResourceLocation> magicIds() { return ids; }
            @Override public boolean isAvailable(LivingEntity entity, ResourceLocation magicId) {
                return TypeMoonIntegration.isAvailable(entity, magicId);
            }
        });
        magicRegistry = addon.magics();
        boolean matouSakuraProfileRegistered = addon.masters().register(
            new MasterProfileData(
                MatouSakuraMasterProfile.ID,
                "item.typemoonaddon.master_card_matou_sakura",
                "sakura",
                1_000.0D,
                8.0D,
                4
            ),
            context -> MatouSakuraMasterProfile.initialize(context.player(), MatouSakuraMasterProfile.Variant.STAY_NIGHT)
        );
        boolean matouSakuraAlterProfileRegistered = addon.masters().register(
            new MasterProfileData(
                MatouSakuraMasterProfile.ALTER_ID,
                "item.typemoonaddon.master_card_matou_sakura_alter",
                "sakura",
                1_000.0D,
                8.0D,
                16
            ),
            context -> MatouSakuraMasterProfile.initialize(context.player(), MatouSakuraMasterProfile.Variant.ALTER)
        );
        boolean matouSakuraFhaProfileRegistered = addon.masters().register(
            new MasterProfileData(
                MatouSakuraMasterProfile.FHA_ID,
                "item.typemoonaddon.master_card_matou_sakura_fha",
                "sakura",
                1_000.0D,
                8.0D,
                4
            ),
            context -> MatouSakuraMasterProfile.initialize(context.player(), MatouSakuraMasterProfile.Variant.FHA)
        );

        MagicDefinitionData storageDefinition = new MagicDefinitionData(
            IMAGINARY_STORAGE,
            "magic.typemoonaddon.imaginary_storage.name",
            MAGIC_BASICS_CATEGORY,
            IMAGINARY_MAGIC_SCHOOL,
            0.0D,
            0,
            true,
            true,
            false,
            false,
            false,
            0,
            0,
            IMAGINARY_REQUIREMENT
        );
        MagicDefinitionData absorptionDefinition = new MagicDefinitionData(
            IMAGINARY_ABSORPTION,
            "magic.typemoonaddon.imaginary_absorption_evolved.name",
            MAGIC_BASICS_CATEGORY,
            IMAGINARY_MAGIC_SCHOOL,
            0.0D,
            0,
            true,
            true,
            false,
            false,
            false,
            0,
            0,
            IMAGINARY_REQUIREMENT
        );
        MagicDefinitionData materializationDefinition = new MagicDefinitionData(
            SHADOW_MATERIALIZATION,
            "magic.typemoonaddon.shadow_materialization.name",
            MAGIC_BASICS_CATEGORY,
            IMAGINARY_MAGIC_SCHOOL,
            0.0D,
            0,
            true,
            true,
            false,
            false,
            false,
            0,
            0,
            IMAGINARY_REQUIREMENT
        );
        MagicDefinitionData blackMudControlDefinition = new MagicDefinitionData(
            BLACK_MUD_CONTROL,
            "magic.typemoonaddon.black_mud_control.name",
            MAGIC_BASICS_CATEGORY,
            IMAGINARY_MAGIC_SCHOOL,
            0.0D,
            0,
            true,
            true,
            false,
            false,
            false,
            0,
            0,
            IMAGINARY_REQUIREMENT
        );
        MagicDefinitionData summonBlackMudDefinition = new MagicDefinitionData(
            SUMMON_BLACK_MUD,
            "magic.typemoonaddon.summon_black_mud.name",
            MAGIC_BASICS_CATEGORY,
            IMAGINARY_MAGIC_SCHOOL,
            0.0D,
            SummonBlackMudService.COOLDOWN_TICKS,
            true,
            true,
            false,
            false,
            false,
            0,
            0,
            IMAGINARY_REQUIREMENT
        );
        MagicDefinitionData shadowBindingDefinition = new MagicDefinitionData(
            SHADOW_BINDING,
            "magic.typemoonaddon.shadow_binding.name",
            MAGIC_BASICS_CATEGORY,
            IMAGINARY_MAGIC_SCHOOL,
            ShadowBindingMagicService.MANA_COST,
            ShadowBindingMagicService.COOLDOWN_TICKS,
            true,
            true,
            false,
            false,
            false,
            0,
            0,
            IMAGINARY_REQUIREMENT
        );
        MagicDefinitionData shadowTransferDefinition = new MagicDefinitionData(
            SHADOW_TRANSFER,
            "magic.typemoonaddon.shadow_transfer.name",
            MAGIC_BASICS_CATEGORY,
            IMAGINARY_MAGIC_SCHOOL,
            0.0D,
            0,
            true,
            true,
            false,
            false,
            false,
            0,
            0,
            IMAGINARY_REQUIREMENT
        );
        MagicDefinitionData heroicSpiritDevourerDefinition = new MagicDefinitionData(
            HEROIC_SPIRIT_DEVOURER,
            "magic.typemoonaddon.heroic_spirit_devourer.name",
            MAGIC_BASICS_CATEGORY,
            IMAGINARY_MAGIC_SCHOOL,
            0.0D,
            40,
            true,
            true,
            false,
            false,
            false,
            0,
            0,
            IMAGINARY_REQUIREMENT
        );
        MagicDefinitionData forbiddenMagicDefinition = new MagicDefinitionData(
            FORBIDDEN_MAGIC,
            "magic.typemoonaddon.forbidden_magic.name",
            MAGIC_BASICS_CATEGORY,
            IMAGINARY_MAGIC_SCHOOL,
            0.0D,
            0,
            true,
            true,
            false,
            false,
            false,
            0,
            0,
            IMAGINARY_REQUIREMENT
        );
        MagicDefinitionData shadowArtDefinition = new MagicDefinitionData(
            SHADOW_ART, "magic.typemoonaddon.shadow_art.name", MAGIC_BASICS_CATEGORY, IMAGINARY_MAGIC_SCHOOL,
            0.0D, 0, true, true, false, false, false, 0, 0, IMAGINARY_REQUIREMENT
        );

        boolean storageDefinitionRegistered = magicRegistry.registerDefinition(storageDefinition);
        boolean absorptionDefinitionRegistered = magicRegistry.registerDefinition(absorptionDefinition);
        boolean materializationDefinitionRegistered = magicRegistry.registerDefinition(materializationDefinition);
        boolean blackMudControlDefinitionRegistered = magicRegistry.registerDefinition(blackMudControlDefinition);
        boolean summonBlackMudDefinitionRegistered = magicRegistry.registerDefinition(summonBlackMudDefinition);
        boolean shadowBindingDefinitionRegistered = magicRegistry.registerDefinition(shadowBindingDefinition);
        boolean shadowTransferDefinitionRegistered = magicRegistry.registerDefinition(shadowTransferDefinition);
        boolean heroicSpiritDevourerDefinitionRegistered = magicRegistry.registerDefinition(heroicSpiritDevourerDefinition);
        boolean forbiddenMagicDefinitionRegistered = magicRegistry.registerDefinition(forbiddenMagicDefinition);
        boolean shadowArtDefinitionRegistered = magicRegistry.registerDefinition(shadowArtDefinition);
        boolean presetRegistered = magicRegistry.registerPreset(IMAGINARY_STORAGE, new ImaginaryStoragePreset());
        boolean absorptionPresetRegistered = magicRegistry.registerPreset(IMAGINARY_ABSORPTION, new ImaginaryStoragePreset());
        boolean blackMudControlPresetRegistered = magicRegistry.registerPreset(BLACK_MUD_CONTROL, new BlackMudControlPreset());
        boolean summonBlackMudPresetRegistered = magicRegistry.registerPreset(SUMMON_BLACK_MUD, new BlackMudSummonPreset());
        boolean shadowArtPresetRegistered = magicRegistry.registerPreset(SHADOW_ART, new ShadowArtPreset());
        boolean controlRegistered = addon.client().registerControl(
            IMAGINARY_STORAGE,
            new MagicOption(
                MODE_PRESET_KEY,
                MagicOption.Kind.ENUM,
                MagicMode.STORAGE.serializedName(),
                0,
                0,
                List.of(MagicMode.STORAGE.serializedName(), MagicMode.PROTECTION.serializedName())
            )
        );
        boolean absorptionControlRegistered = addon.client().registerControl(
            IMAGINARY_ABSORPTION,
            new MagicOption(
                MODE_PRESET_KEY,
                MagicOption.Kind.ENUM,
                MagicMode.STORAGE.serializedName(),
                0,
                0,
                List.of(MagicMode.STORAGE.serializedName(), MagicMode.PROTECTION.serializedName())
            )
        );
        boolean blackMudControlRegistered = addon.client().registerControl(
            BLACK_MUD_CONTROL,
            new MagicOption(
                SHADOW_COMMAND_PRESET_KEY,
                MagicOption.Kind.ENUM,
                ShadowCommandMode.FREE.serializedName(),
                0,
                0,
                List.of(
                    ShadowCommandMode.FREE.serializedName(),
                    ShadowCommandMode.SPREAD.serializedName(),
                    ShadowCommandMode.HOLD.serializedName(),
                    ShadowCommandMode.GATHER.serializedName(),
                    ShadowCommandMode.HUNT.serializedName(),
                    ShadowCommandMode.DISMISS.serializedName()
                )
            )
        );
        boolean summonBlackMudControlRegistered = addon.client().registerControl(
            SUMMON_BLACK_MUD,
            new MagicOption(
                BLACK_MUD_SUMMON_PRESET_KEY,
                MagicOption.Kind.ENUM,
                BlackMudSummonMode.RELEASE.serializedName(),
                0,
                0,
                List.of(
                    BlackMudSummonMode.RELEASE.serializedName(),
                    BlackMudSummonMode.DISMISS.serializedName()
                )
            )
        );
        boolean shadowArtControlRegistered = addon.client().registerControl(
            SHADOW_ART,
            new MagicOption(
                SHADOW_ART_MODE_PRESET_KEY, MagicOption.Kind.ENUM, ShadowArtMode.BALANCED.serializedName(), 0, 0,
                List.of(ShadowArtMode.AUTO_DEFENSE.serializedName(), ShadowArtMode.AUTO_ATTACK.serializedName(), ShadowArtMode.BALANCED.serializedName())
            )
        );
        boolean customOptionsRegistered = FMLEnvironment.dist != Dist.CLIENT
            || TypeMoonClientModeBridge.registerOptions(addon.client());
        boolean executorRegistered = magicRegistry.registerExecutor(IMAGINARY_STORAGE, context -> {
            ServerPlayer player = context.serverPlayer();
            if (player == null || HolyGrailService.blocksAction(player) || !isLegacyStorageLearned(player)) {
                return ExecutionResult.FAILED;
            }
            player.getData(ModAttachments.IMAGINARY_SPACE.get()).setMagicMode(
                MagicMode.fromSerialized(context.preset().getString(MODE_PRESET_KEY))
            );
            return ImaginaryStorageService.castFromTypeMoonWorld(player)
                ? ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(0)
                : ExecutionResult.FAILED;
        });
        boolean absorptionExecutorRegistered = magicRegistry.registerExecutor(IMAGINARY_ABSORPTION, context -> {
            ServerPlayer player = context.serverPlayer();
            if (player != null) {
                player.getData(ModAttachments.IMAGINARY_SPACE.get()).setMagicMode(
                    MagicMode.fromSerialized(context.preset().getString(MODE_PRESET_KEY))
                );
            }
            return player != null && !HolyGrailService.blocksAction(player)
                && isAbsorptionLearned(player) && ImaginaryStorageService.castFromAbsorption(player)
                ? ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(0)
                : ExecutionResult.FAILED;
        });
        boolean materializationExecutorRegistered = magicRegistry.registerExecutor(SHADOW_MATERIALIZATION, context -> {
            ServerPlayer player = context.serverPlayer();
            return player != null && !HolyGrailService.blocksAction(player)
                && isShadowMaterializationLearned(player) && ShadowMaterializationService.beginCharging(player)
                ? ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(0)
                : ExecutionResult.FAILED;
        });
        boolean blackMudControlExecutorRegistered = magicRegistry.registerExecutor(BLACK_MUD_CONTROL, context -> {
            ServerPlayer player = context.serverPlayer();
            if (player != null) {
                player.getData(ModAttachments.IMAGINARY_SPACE.get()).setSelectedShadowCommandMode(
                    ShadowCommandMode.fromSerialized(context.preset().getString(SHADOW_COMMAND_PRESET_KEY))
                );
            }
            return player != null && BlackMudControlService.cast(player)
                ? ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(0)
                : ExecutionResult.FAILED;
        });
        boolean summonBlackMudExecutorRegistered = magicRegistry.registerExecutor(SUMMON_BLACK_MUD, context -> {
            ServerPlayer player = context.serverPlayer();
            if (player != null) {
                player.getData(ModAttachments.IMAGINARY_SPACE.get()).setBlackMudSummonMode(
                    BlackMudSummonMode.fromSerialized(context.preset().getString(BLACK_MUD_SUMMON_PRESET_KEY))
                );
            }
            return player != null && SummonBlackMudService.cast(player)
                ? ExecutionResult.SUCCESS
                    .withCost(0.0D)
                    .withCooldown(SummonBlackMudService.COOLDOWN_TICKS)
                : ExecutionResult.FAILED;
        });
        boolean shadowBindingExecutorRegistered = magicRegistry.registerExecutor(SHADOW_BINDING, context -> {
            ServerPlayer player = context.serverPlayer();
            return player != null && ShadowBindingMagicService.cast(player)
                ? ExecutionResult.SUCCESS
                    .withCost(ShadowBindingMagicService.MANA_COST)
                    .withCooldown(ShadowBindingMagicService.COOLDOWN_TICKS)
                : ExecutionResult.FAILED;
        });
        boolean shadowTransferExecutorRegistered = magicRegistry.registerExecutor(SHADOW_TRANSFER, context -> {
            ServerPlayer player = context.serverPlayer();
            return player != null && ShadowTransferService.openSelection(player)
                ? ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(0)
                : ExecutionResult.FAILED;
        });
        boolean heroicSpiritDevourerExecutorRegistered = magicRegistry.registerExecutor(HEROIC_SPIRIT_DEVOURER, context -> {
            ServerPlayer player = context.serverPlayer();
            return player != null && isHeroicSpiritDevourerLearned(player)
                && PollutionService.summonCorruptedServants(player) > 0
                ? ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(40)
                : ExecutionResult.FAILED;
        });
        boolean forbiddenMagicExecutorRegistered = magicRegistry.registerExecutor(FORBIDDEN_MAGIC, context -> {
            ServerPlayer player = context.serverPlayer();
            return player != null && isForbiddenMagicLearned(player) && ForbiddenMagicService.cast(player)
                ? ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(0)
                : ExecutionResult.FAILED;
        });
        boolean shadowArtExecutorRegistered = magicRegistry.registerExecutor(SHADOW_ART, context -> {
            ServerPlayer player = context.serverPlayer();
            if (player == null || !isShadowArtLearned(player)) return ExecutionResult.FAILED;
            ShadowArtMode mode = ShadowArtMode.fromSerialized(context.preset().getString(SHADOW_ART_MODE_PRESET_KEY));
            ShadowArtService.selectMode(player, mode);
            return ShadowArtService.cast(player) ? ExecutionResult.SUCCESS.withCost(0.0D).withCooldown(0) : ExecutionResult.FAILED;
        });

        if (!matouSakuraProfileRegistered || !matouSakuraAlterProfileRegistered || !matouSakuraFhaProfileRegistered
            || !storageDefinitionRegistered || !absorptionDefinitionRegistered
            || !materializationDefinitionRegistered || !blackMudControlDefinitionRegistered
            || !summonBlackMudDefinitionRegistered || !shadowBindingDefinitionRegistered
            || !shadowTransferDefinitionRegistered || !heroicSpiritDevourerDefinitionRegistered
            || !forbiddenMagicDefinitionRegistered || !shadowArtDefinitionRegistered
            || !presetRegistered || !absorptionPresetRegistered || !blackMudControlPresetRegistered
            || !summonBlackMudPresetRegistered || !shadowArtPresetRegistered
            || !controlRegistered || !absorptionControlRegistered || !blackMudControlRegistered
            || !summonBlackMudControlRegistered || !shadowArtControlRegistered
            || !customOptionsRegistered || !executorRegistered || !absorptionExecutorRegistered
            || !materializationExecutorRegistered || !blackMudControlExecutorRegistered
            || !summonBlackMudExecutorRegistered || !shadowBindingExecutorRegistered
            || !shadowTransferExecutorRegistered || !heroicSpiritDevourerExecutorRegistered
            || !forbiddenMagicExecutorRegistered || !shadowArtExecutorRegistered) {
            throw new IllegalStateException(String.format(
                "Failed to register Type Moon World extension (sakuraMaster=%s, storageDefinition=%s, absorptionDefinition=%s, materializationDefinition=%s, blackMudControlDefinition=%s, summonBlackMudDefinition=%s, shadowBindingDefinition=%s, transferDefinition=%s, devourerDefinition=%s, forbiddenDefinition=%s, shadowArtDefinition=%s, storagePreset=%s, absorptionPreset=%s, blackMudPreset=%s, summonBlackMudPreset=%s, shadowArtPreset=%s, storageControl=%s, absorptionControl=%s, blackMudControl=%s, summonBlackMudControl=%s, shadowArtControl=%s, customOptions=%s, storageExecutor=%s, absorptionExecutor=%s, materializationExecutor=%s, blackMudControlExecutor=%s, summonBlackMudExecutor=%s, shadowBindingExecutor=%s, transferExecutor=%s, devourerExecutor=%s, forbiddenExecutor=%s, shadowArtExecutor=%s)",
                matouSakuraProfileRegistered,
                storageDefinitionRegistered,
                absorptionDefinitionRegistered,
                materializationDefinitionRegistered,
                blackMudControlDefinitionRegistered,
                summonBlackMudDefinitionRegistered,
                shadowBindingDefinitionRegistered,
                shadowTransferDefinitionRegistered,
                heroicSpiritDevourerDefinitionRegistered,
                forbiddenMagicDefinitionRegistered,
                shadowArtDefinitionRegistered,
                presetRegistered,
                absorptionPresetRegistered,
                blackMudControlPresetRegistered,
                summonBlackMudPresetRegistered,
                shadowArtPresetRegistered,
                controlRegistered,
                absorptionControlRegistered,
                blackMudControlRegistered,
                summonBlackMudControlRegistered,
                shadowArtControlRegistered,
                customOptionsRegistered,
                executorRegistered,
                absorptionExecutorRegistered,
                materializationExecutorRegistered,
                blackMudControlExecutorRegistered,
                summonBlackMudExecutorRegistered,
                shadowBindingExecutorRegistered,
                shadowTransferExecutorRegistered,
                heroicSpiritDevourerExecutorRegistered,
                forbiddenMagicExecutorRegistered,
                shadowArtExecutorRegistered
            ));
        }

        TypeMoonAddon.LOGGER.info(
            "Registered Type Moon World magic extension: {} (category={}, school={})",
            IMAGINARY_STORAGE,
            MAGIC_BASICS_CATEGORY,
            IMAGINARY_MAGIC_SCHOOL
        );
    }

    public static boolean learnAndGrantAttribute(ServerPlayer player) {
        var knowledge = registry().knowledge(player);
        if (!knowledge.isLearned(IMAGINARY_STORAGE) && !knowledge.learn(IMAGINARY_STORAGE)) {
            return false;
        }
        return ensureImaginaryAttribute(player);
    }

    public static boolean ensureImaginaryAttribute(ServerPlayer player) {
        boolean upgraded = player.getData(ModAttachments.IMAGINARY_SPACE.get()).crestWormAssimilated();
        var knowledge = registry().knowledge(player);
        boolean knowledgeReady = upgraded
            ? ensureAbsorptionKnowledge(player)
            : knowledge.isLearned(IMAGINARY_STORAGE) || knowledge.learn(IMAGINARY_STORAGE);
        boolean ready = knowledgeReady
            && TypeMoonAttributeBridge.grantImaginaryNumber(player)
            && (!upgraded || TypeMoonAttributeBridge.grantWater(player));
        if (ready) {
            removeObsoleteMagicWheelEntries(player, upgraded);
        }
        return ready;
    }

    public static boolean ensureAbsorptionKnowledge(ServerPlayer player) {
        var knowledge = registry().knowledge(player);
        return knowledge.isLearned(IMAGINARY_ABSORPTION) || knowledge.learn(IMAGINARY_ABSORPTION);
    }

    public static boolean assimilateCrestWorm(ServerPlayer player) {
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        if (!data.learned() || data.crestWormAssimilated()) {
            return false;
        }
        var knowledge = registry().knowledge(player);
        if (!knowledge.isLearned(IMAGINARY_STORAGE) || !ensureAbsorptionKnowledge(player)) {
            return false;
        }
        if (!TypeMoonAttributeBridge.grantWater(player)) {
            return false;
        }
        knowledge.setProficiency(
            IMAGINARY_ABSORPTION,
            Math.max(knowledge.proficiency(IMAGINARY_STORAGE), knowledge.proficiency(LEGACY_IMAGINARY_SHADOW))
        );
        if (!data.assimilateCrestWorm()) {
            return false;
        }
        removeObsoleteMagicWheelEntries(player, true);
        return true;
    }

    public static boolean performHolyGrailRitual(ServerPlayer player) {
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        if (!data.learned() || data.grailWormAscended()) {
            return false;
        }
        var knowledge = registry().knowledge(player);
        if (!knowledge.isLearned(IMAGINARY_STORAGE)
            || !ensureAbsorptionKnowledge(player)
            || !ensureShadowMaterializationKnowledge(player)
            || !ensureBlackMudControlKnowledge(player)
            || !ensureSummonBlackMudKnowledge(player)
            || !ensureShadowBindingKnowledge(player)
            || !ensureShadowTransferKnowledge(player)
            || !ensureHeroicSpiritDevourerKnowledge(player)
            || !TypeMoonAttributeBridge.grantImaginaryNumber(player)) {
            return false;
        }

        knowledge.setProficiency(
            IMAGINARY_ABSORPTION,
            Math.max(knowledge.proficiency(IMAGINARY_STORAGE), knowledge.proficiency(LEGACY_IMAGINARY_SHADOW))
        );
        if (!data.crestWormAssimilated() && !data.assimilateCrestWorm()) {
            return false;
        }
        boolean ascended = data.ascendGrailWorm();
        if (data.crestWormAssimilated()) {
            removeObsoleteMagicWheelEntries(player, true);
        }
        return ascended;
    }

    public static boolean ensureGrailWormPower(ServerPlayer player) {
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        if (!data.grailWormAscended()) {
            return true;
        }
        if (data.grailErosionFull() && data.shadowArtState() == io.github.typemoonaddon.data.ImaginarySpaceData.ShadowArtState.LOCKED) {
            data.unlockShadowArt();
        }
        return ensureShadowMaterializationKnowledge(player)
            && ensureBlackMudControlKnowledge(player)
            && ensureSummonBlackMudKnowledge(player)
            && ensureShadowBindingKnowledge(player)
            && ensureShadowTransferKnowledge(player)
            && ensureHeroicSpiritDevourerKnowledge(player)
            && (!data.shadowArtUnlocked() || ensureShadowArtKnowledge(player))
            && (!data.grailErosionFull() || TypeMoonAttributeBridge.grantGrailWormPower(player));
    }

    public static boolean ensureShadowMaterializationKnowledge(ServerPlayer player) {
        var knowledge = registry().knowledge(player);
        return knowledge.isLearned(SHADOW_MATERIALIZATION) || knowledge.learn(SHADOW_MATERIALIZATION);
    }

    public static boolean ensureBlackMudControlKnowledge(ServerPlayer player) {
        var knowledge = registry().knowledge(player);
        return knowledge.isLearned(BLACK_MUD_CONTROL) || knowledge.learn(BLACK_MUD_CONTROL);
    }

    public static boolean ensureSummonBlackMudKnowledge(ServerPlayer player) {
        var knowledge = registry().knowledge(player);
        return knowledge.isLearned(SUMMON_BLACK_MUD) || knowledge.learn(SUMMON_BLACK_MUD);
    }

    public static boolean ensureShadowBindingKnowledge(ServerPlayer player) {
        var knowledge = registry().knowledge(player);
        return knowledge.isLearned(SHADOW_BINDING) || knowledge.learn(SHADOW_BINDING);
    }

    public static boolean ensureShadowTransferKnowledge(ServerPlayer player) {
        var knowledge = registry().knowledge(player);
        return knowledge.isLearned(SHADOW_TRANSFER) || knowledge.learn(SHADOW_TRANSFER);
    }

    public static boolean ensureHeroicSpiritDevourerKnowledge(ServerPlayer player) {
        var knowledge = registry().knowledge(player);
        return knowledge.isLearned(HEROIC_SPIRIT_DEVOURER) || knowledge.learn(HEROIC_SPIRIT_DEVOURER);
    }

    public static boolean ensureForbiddenMagicKnowledge(ServerPlayer player) {
        return RuleBreakerDispelService.unlockForbiddenMagicKnowledge(player);
    }

    public static boolean ensureShadowArtKnowledge(ServerPlayer player) {
        return RuleBreakerDispelService.unlockShadowArtKnowledge(player);
    }

    public static boolean isShadowArtLearned(ServerPlayer player) {
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        return data.shadowArtUnlocked() && data.grailErosionFull() && registry().knowledge(player).isLearned(SHADOW_ART)
            ;
    }

    public static float imaginaryDamage(ServerPlayer caster, LivingEntity target, float baseDamage) {
        if (baseDamage <= 0.0F) {
            return 0.0F;
        }
        return caster.getData(ModAttachments.IMAGINARY_SPACE.get()).grailWormAscended()
            && TypeMoonServantBridge.isServantLike(target)
            ? baseDamage * GameplayConfig.GRAIL_SERVANT_DAMAGE_MULTIPLIER
            : baseDamage;
    }

    public static boolean isServantLike(LivingEntity target) {
        return TypeMoonServantBridge.isServantLike(target);
    }

    public static float imaginaryIncomingDamage(DamageSource source, LivingEntity target, float baseDamage) {
        if (!(source.getEntity() instanceof ServerPlayer caster) || !isAddonImaginaryDamage(source)) {
            return baseDamage;
        }
        return imaginaryDamage(caster, target, baseDamage);
    }

    private static boolean isAddonImaginaryDamage(DamageSource source) {
        return source.typeHolder().unwrapKey().map(key -> {
            ResourceLocation id = key.location();
            return TypeMoonAddon.MOD_ID.equals(id.getNamespace())
                && ("imaginary_absorption".equals(id.getPath()) || "shadow_banishment".equals(id.getPath()));
        }).orElse(false);
    }

    public static boolean isLearned(ServerPlayer player) {
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        ResourceLocation activeMagic = data.crestWormAssimilated() ? IMAGINARY_ABSORPTION : IMAGINARY_STORAGE;
        return data.learned()
            && registry().knowledge(player).isLearned(activeMagic)
            ;
    }

    public static boolean isLegacyStorageLearned(ServerPlayer player) {
        return isLearned(player)
            && !player.getData(ModAttachments.IMAGINARY_SPACE.get()).crestWormAssimilated()
            && registry().knowledge(player).isLearned(IMAGINARY_STORAGE);
    }

    public static boolean isAbsorptionLearned(ServerPlayer player) {
        return isLearned(player)
            && player.getData(ModAttachments.IMAGINARY_SPACE.get()).crestWormAssimilated()
            && registry().knowledge(player).isLearned(IMAGINARY_ABSORPTION);
    }

    public static boolean isShadowMaterializationLearned(ServerPlayer player) {
        return player.getData(ModAttachments.IMAGINARY_SPACE.get()).grailWormAscended()
            && player.getData(ModAttachments.IMAGINARY_SPACE.get()).grailErosionFull()
            && registry().knowledge(player).isLearned(SHADOW_MATERIALIZATION)
            ;
    }

    public static boolean isBlackMudControlLearned(ServerPlayer player) {
        return player.getData(ModAttachments.IMAGINARY_SPACE.get()).grailWormAscended()
            && player.getData(ModAttachments.IMAGINARY_SPACE.get()).grailErosionFull()
            && registry().knowledge(player).isLearned(BLACK_MUD_CONTROL)
            ;
    }

    public static boolean isSummonBlackMudLearned(ServerPlayer player) {
        return player.getData(ModAttachments.IMAGINARY_SPACE.get()).grailWormAscended()
            && player.getData(ModAttachments.IMAGINARY_SPACE.get()).grailErosionFull()
            && registry().knowledge(player).isLearned(SUMMON_BLACK_MUD)
            ;
    }

    public static boolean isShadowBindingLearned(ServerPlayer player) {
        return player.getData(ModAttachments.IMAGINARY_SPACE.get()).grailWormAscended()
            && player.getData(ModAttachments.IMAGINARY_SPACE.get()).grailErosionFull()
            && registry().knowledge(player).isLearned(SHADOW_BINDING)
            ;
    }

    public static boolean isShadowTransferLearned(ServerPlayer player) {
        return player.getData(ModAttachments.IMAGINARY_SPACE.get()).grailWormAscended()
            && player.getData(ModAttachments.IMAGINARY_SPACE.get()).grailErosionFull()
            && registry().knowledge(player).isLearned(SHADOW_TRANSFER)
            ;
    }

    public static boolean isHeroicSpiritDevourerLearned(ServerPlayer player) {
        return player.getData(ModAttachments.IMAGINARY_SPACE.get()).grailWormAscended()
            && player.getData(ModAttachments.IMAGINARY_SPACE.get()).grailErosionFull()
            && registry().knowledge(player).isLearned(HEROIC_SPIRIT_DEVOURER)
            ;
    }

    public static boolean isForbiddenMagicLearned(ServerPlayer player) {
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        return data.forbiddenMagicUnlocked()
            && !data.grailWormAscended()
            && data.crestWormAssimilated()
            && registry().knowledge(player).isLearned(FORBIDDEN_MAGIC)
            ;
    }

    public static boolean canUseShadowEffect(ServerPlayer player) {
        return isAbsorptionLearned(player);
    }

    private static boolean isAvailable(LivingEntity entity, ResourceLocation magicId) {
        if (entity == null || magicId == null) return false;
        var data = entity.getData(ModAttachments.IMAGINARY_SPACE.get());
        var knowledge = registry().knowledge(entity);
        if (IMAGINARY_STORAGE.equals(magicId)) {
            return data.learned() && !data.crestWormAssimilated() && knowledge.isLearned(IMAGINARY_STORAGE);
        }
        if (IMAGINARY_ABSORPTION.equals(magicId)) {
            return data.learned() && data.crestWormAssimilated() && knowledge.isLearned(IMAGINARY_ABSORPTION);
        }
        if (FORBIDDEN_MAGIC.equals(magicId)) {
            return data.forbiddenMagicUnlocked() && !data.grailWormAscended()
                && data.crestWormAssimilated() && knowledge.isLearned(FORBIDDEN_MAGIC);
        }
        if (!data.grailWormAscended() || !data.grailErosionFull() || !knowledge.isLearned(magicId)) return false;
        return !SHADOW_ART.equals(magicId) || data.shadowArtUnlocked();
    }

    public static double proficiency(LivingEntity entity) {
        if (entity instanceof net.minecraft.world.entity.player.Player player
            && player.getData(ModAttachments.IMAGINARY_SPACE.get()).crestWormAssimilated()) {
            return registry().knowledge(entity).proficiency(IMAGINARY_ABSORPTION);
        }
        return registry().knowledge(entity).proficiency(IMAGINARY_STORAGE);
    }

    public static void addProficiency(ServerPlayer player, double amount) {
        if (amount > 0.0D && isLearned(player)) {
            ResourceLocation activeMagic = player.getData(ModAttachments.IMAGINARY_SPACE.get()).crestWormAssimilated()
                ? IMAGINARY_ABSORPTION
                : IMAGINARY_STORAGE;
            registry().knowledge(player).addProficiency(activeMagic, amount);
        }
    }

    public static boolean tryConsumeMana(ServerPlayer player, double amount) {
        if (amount <= 0.0D) return true;
        boolean consumed = registry().mana(player).tryConsume(amount);
        if (consumed) SpiritualDamageService.recordMagicCost(player, amount);
        return consumed;
    }

    public static double currentMana(LivingEntity entity) {
        return registry().mana(entity).current();
    }

    public static void refundMana(ServerPlayer player, double amount) {
        restoreMana(player, amount);
    }

    public static double restoreMana(LivingEntity entity, double amount) {
        if (amount <= 0.0D) {
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
        if (source == recipient || maximumAmount <= 0.0D) {
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

    public static double drainMana(LivingEntity source, double maximumAmount) {
        if (maximumAmount <= 0.0D) {
            return 0.0D;
        }
        var mana = registry().mana(source);
        double amount = Math.min(maximumAmount, Math.max(0.0D, mana.current()));
        return amount > 0.0D && mana.tryConsume(amount) ? amount : 0.0D;
    }

    public static double drainAllMana(LivingEntity source) {
        return drainMana(source, Double.MAX_VALUE);
    }

    private static void removeObsoleteMagicWheelEntries(ServerPlayer player, boolean removeStorage) {
        // The public API owns wheel state. Legacy entries remain inert because every
        // executor validates the addon's authoritative progression attachment.
    }

    public static boolean isTransformedServant(ServerPlayer player) {
        return TypeMoonWorldApi.servantForm(player).transformed();
    }

    public static void verifyRegistration() {
        verifyWheelSpell(IMAGINARY_STORAGE, "Imaginary Storage");
        verifyWheelSpell(IMAGINARY_ABSORPTION, "Imaginary Absorption");
        verifyWheelSpell(SHADOW_ART, "Shadow Art");
        verifyWheelSpell(SHADOW_MATERIALIZATION, "Shadow Materialization");
        verifyWheelSpell(BLACK_MUD_CONTROL, "Black Mud Control");
        verifyWheelSpell(SUMMON_BLACK_MUD, "Summon Black Mud");
        verifyWheelSpell(SHADOW_BINDING, "Shadow Binding");
        verifyWheelSpell(SHADOW_TRANSFER, "Shadow Transfer");
        verifyWheelSpell(HEROIC_SPIRIT_DEVOURER, "Heroic Spirit Devourer");
        verifyWheelSpell(FORBIDDEN_MAGIC, "Forbidden Magic");
        TypeMoonAttributeBridge.verifyCompatibility();
        TypeMoonServantBridge.verifyCompatibility();
        TypeMoonAddon.LOGGER.info(
            "Verified Type Moon World magic definitions after data loading: {}, {}",
            IMAGINARY_STORAGE,
            SHADOW_ART
        );
    }

    private static void verifyWheelSpell(ResourceLocation id, String name) {
        MagicDefinitionData definition = registry().definition(id)
            .orElseThrow(() -> new IllegalStateException("Type Moon World lost the " + name + " magic definition during loading"));
        if (!MAGIC_BASICS_CATEGORY.equals(definition.category())
            || !IMAGINARY_MAGIC_SCHOOL.equals(definition.school())
            || definition.knowledgeOnly()
            || !definition.wheelSelectable()) {
            throw new IllegalStateException(name + " is not registered as an equipable Magic Basics spell");
        }
    }

    private static MagicRegistry registry() {
        if (magicRegistry == null) {
            magicRegistry = TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics();
        }
        return magicRegistry;
    }

    private static final class ImaginaryStoragePreset implements MagicPresetHandler {
        @Override
        public CompoundTag normalize(CompoundTag input) {
            CompoundTag normalized = new CompoundTag();
            String requested = input == null ? "" : input.getString(MODE_PRESET_KEY);
            normalized.putString(MODE_PRESET_KEY, MagicMode.fromSerialized(requested).serializedName());
            return normalized;
        }
    }

    private static final class BlackMudControlPreset implements MagicPresetHandler {
        @Override
        public CompoundTag normalize(CompoundTag input) {
            CompoundTag normalized = new CompoundTag();
            String requested = input == null ? "" : input.getString(SHADOW_COMMAND_PRESET_KEY);
            ShadowCommandMode mode = ShadowCommandMode.fromSerialized(requested);
            if (mode == ShadowCommandMode.ATTACK_AROUND) {
                mode = ShadowCommandMode.FREE;
            }
            normalized.putString(
                SHADOW_COMMAND_PRESET_KEY,
                mode.serializedName()
            );
            return normalized;
        }
    }

    private static final class BlackMudSummonPreset implements MagicPresetHandler {
        @Override
        public CompoundTag normalize(CompoundTag input) {
            CompoundTag normalized = new CompoundTag();
            String requested = input == null ? "" : input.getString(BLACK_MUD_SUMMON_PRESET_KEY);
            normalized.putString(
                BLACK_MUD_SUMMON_PRESET_KEY,
                BlackMudSummonMode.fromSerialized(requested).serializedName()
            );
            return normalized;
        }
    }

    private static final class ShadowArtPreset implements MagicPresetHandler {
        @Override public CompoundTag normalize(CompoundTag input) {
            CompoundTag normalized = new CompoundTag();
            normalized.putString(SHADOW_ART_MODE_PRESET_KEY, ShadowArtMode.fromSerialized(input == null ? "" : input.getString(SHADOW_ART_MODE_PRESET_KEY)).serializedName());
            return normalized;
        }
    }

    private TypeMoonIntegration() {
    }
}
