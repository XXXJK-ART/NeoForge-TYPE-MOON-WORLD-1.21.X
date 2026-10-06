package com.example.typemoonaddon;

import com.example.typemoonaddon.registry.AddonItems;
import com.example.typemoonaddon.registry.AddonMenus;
import com.example.typemoonaddon.registry.AddonMobEffects;
import com.example.typemoonaddon.registry.AddonEntities;
import com.example.typemoonaddon.registry.AddonBlocks;
import com.example.typemoonaddon.block.entity.AddonBlockEntities;
import com.example.typemoonaddon.registry.AddonSounds;
import com.example.typemoonaddon.detection.DetectionAttachments;
import com.example.typemoonaddon.detection.DetectionService;
import com.example.typemoonaddon.airflow_blade.AirflowBladeService;
import com.example.typemoonaddon.engravedworm.EngravedWormAttachments;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceAttachments;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceService;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceConfig;
import com.example.typemoonaddon.kimaris.KimarisAttachments;
import com.example.typemoonaddon.magic.WormMagicIntegration;
import com.example.typemoonaddon.magic.SummoningMagicIntegration;
import com.example.typemoonaddon.magic.BoundaryMagicIntegration;
import com.example.typemoonaddon.magic.EntityDisplacementService;
import com.example.typemoonaddon.magic.MatouKariyaMasterIntegration;
import com.example.typemoonaddon.magic.ImaginaryDisplacementAttachments;
import com.example.typemoonaddon.storage.StorageAttachments;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.xxxjk.TYPE_MOON_WORLD.init.ModCreativeModeTabs;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import org.slf4j.Logger;
import com.example.typemoonaddon.event.AddonPlayerTickServices;
import net.xxxjk.typemoonworld.api.AddonManaSource;
import net.xxxjk.typemoonworld.api.AddonMagicHooks;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.typemoonworld.api.AddonEntrypoint;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class TypeMoonAddon implements AddonEntrypoint {
    /** Compatibility namespace for extensions retained by the core mod. */
    public static final String MOD_ID = "typemoonworld";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TypeMoonAddon() {
    }

    @Override
    public void initialize(IEventBus modEventBus, ModContainer modContainer) {
        AddonBlocks.register(modEventBus);
        AddonBlockEntities.register(modEventBus);
        AddonItems.register(modEventBus);
        AddonMenus.register(modEventBus);
        AddonMobEffects.register(modEventBus);
        AddonEntities.register(modEventBus);
        AddonSounds.register(modEventBus);
        StorageAttachments.register(modEventBus);
        ImaginaryDisplacementAttachments.register(modEventBus);
        KimarisAttachments.register(modEventBus);
        DetectionAttachments.register(modEventBus);
        EngravedWormAttachments.register(modEventBus);
        ImaginarySpaceAttachments.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, ImaginarySpaceConfig.SPEC, "typemoonworld-imaginary-space.toml");
        modEventBus.addListener(AddonEntities::registerAttributes);
        modEventBus.addListener(AddonEntities::registerSpawnPlacements);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreativeTabContents);
        AddonPlayerTickServices.register();
        AddonManaSource.register(com.example.typemoonaddon.magic.ManaFurnaceService::hasInfiniteSupply);
        AddonMagicHooks.register(new AddonMagicHooks.Provider() {
            public double adjustNaturalManaRegen(net.minecraft.world.entity.LivingEntity entity, double value) {
                return BoundaryMagicIntegration.adjustNaturalManaRegen(entity, value);
            }
            public boolean swapNpc(net.minecraft.world.entity.LivingEntity caster, net.minecraft.world.entity.LivingEntity target) {
                return caster instanceof MysticMagicianEntity mage && EntityDisplacementService.swapNpc(mage, target);
            }
            public int bladeManaCost() { return (int) AirflowBladeService.BLADE_MANA_COST; }
            public double bladeSpeed() { return AirflowBladeService.BLADE_SPEED; }
            public boolean castNpcBlade(net.minecraft.world.entity.LivingEntity caster, net.minecraft.world.entity.LivingEntity target, double proficiency) {
                return AirflowBladeService.castNpcBlade(caster, target, proficiency);
            }
            public boolean castNpcDetection(net.minecraft.world.entity.LivingEntity caster, net.minecraft.world.entity.LivingEntity target, double proficiency) {
                return DetectionService.castNpcDetection(caster, target, proficiency);
            }
            public boolean castNpcImaginarySpace(net.minecraft.world.entity.LivingEntity caster, net.minecraft.world.entity.LivingEntity target) {
                return ImaginarySpaceService.castNpcCreature(caster, target).status() == ImaginarySpaceService.CastStatus.SUCCESS;
            }
        });
        LOGGER.info("Integrated Type Moon addon systems initialized");
    }

    @Override
    public void registerPayloads(PayloadRegistrar registrar) {
        com.example.typemoonaddon.network.AddonNetwork.registerPayloads(registrar);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == ModCreativeModeTabs.MAGIC_BOOKS_TAB.get()) {
            event.accept(AddonItems.MAGIC_BOOK_SPIRIT_SUMMONING);
            event.accept(AddonItems.MAGIC_BOOK_WRAITH_SERVITUDE);
            event.accept(AddonItems.MAGIC_BOOK_EVIL_SPIRIT_SUMMONING);
            event.accept(AddonItems.MAGIC_BOOK_WORM_MAGIC);
            event.accept(AddonItems.MAGIC_PAGE_WORM_MAGIC);
            event.accept(AddonItems.MAGIC_BOOK_WORM_CONTROL);
            event.accept(AddonItems.MAGIC_PAGE_WORM_CONTROL);
            event.accept(AddonItems.MAGIC_BOOK_ENGRAVED_WORM_OPERATION);
            event.accept(AddonItems.MAGIC_PAGE_ENGRAVED_WORM_OPERATION);
            event.accept(AddonItems.MAGIC_BOOK_SANCTIFICATION);
            event.accept(AddonItems.MAGIC_PAGE_SANCTIFICATION);
            event.accept(AddonItems.MAGIC_BOOK_BOUNDARY_ART);
            event.accept(AddonItems.MAGIC_PAGE_BOUNDARY_ART);
            event.accept(AddonItems.MAGIC_BOOK_SENSING_BOUNDARY);
            event.accept(AddonItems.MAGIC_PAGE_SENSING_BOUNDARY);
            event.accept(AddonItems.MAGIC_BOOK_WARNING_BOUNDARY);
            event.accept(AddonItems.MAGIC_PAGE_WARNING_BOUNDARY);
            event.accept(AddonItems.MAGIC_BOOK_DEFENSE_BOUNDARY);
            event.accept(AddonItems.MAGIC_PAGE_DEFENSE_BOUNDARY);
            event.accept(AddonItems.MAGIC_BOOK_SUGGESTION_BOUNDARY);
            event.accept(AddonItems.MAGIC_PAGE_SUGGESTION_BOUNDARY);
            event.accept(AddonItems.MAGIC_BOOK_ANTI_MAGIC_BOUNDARY);
            event.accept(AddonItems.MAGIC_PAGE_ANTI_MAGIC_BOUNDARY);
            event.accept(AddonItems.MAGIC_BOOK_GUARD_BOUNDARY);
            event.accept(AddonItems.MAGIC_PAGE_GUARD_BOUNDARY);
            event.accept(AddonItems.MAGIC_BOOK_INTERFERENCE_BOUNDARY);
            event.accept(AddonItems.MAGIC_PAGE_INTERFERENCE_BOUNDARY);
        }
        if (event.getTab() == ModCreativeModeTabs.TYPE_MOON_WORLD_TAB.get()) {
            CreativeModeTab.TabVisibility visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
            event.accept(AddonItems.MANA_FURNACE.toStack(), visibility);

            // Keep the worm magecraft materials together immediately after the origin bullet.
            ItemStack anchor = ModItems.ORIGIN_BULLET.toStack();
            event.insertAfter(anchor, AddonItems.SILVERFISH_WORM.toStack(), visibility);
            anchor = AddonItems.SILVERFISH_WORM.toStack();
            event.insertAfter(anchor, AddonItems.ENDERMITE_WORM.toStack(), visibility);
            anchor = AddonItems.ENDERMITE_WORM.toStack();
            event.insertAfter(anchor, AddonItems.WINGED_WORM.toStack(), visibility);
            anchor = AddonItems.WINGED_WORM.toStack();
            event.insertAfter(anchor, AddonItems.FIREPROOF_WORM.toStack(), visibility);
            anchor = AddonItems.FIREPROOF_WORM.toStack();
            event.insertAfter(anchor, AddonItems.DETECTION_WORM.toStack(), visibility);
            anchor = AddonItems.DETECTION_WORM.toStack();
            event.insertAfter(anchor, AddonItems.ENGRAVED_WORM.toStack(), visibility);
        }
        if (event.getTab() == ModCreativeModeTabs.SPAWN_EGGS_TAB.get()) {
            event.accept(AddonItems.GILLES_DE_RAIS_CASTER_SPAWN_EGG);
            event.accept(AddonItems.GILLES_SEA_MONSTER_SPAWN_EGG);
            event.accept(AddonItems.GILLES_LARGE_SEA_MONSTER_SPAWN_EGG);
            event.accept(AddonItems.GILLES_HUGE_SEA_MONSTER_SPAWN_EGG);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        registerLegacyMagic();
    }

    /** Registers extensions that are still owned by the core jar. */
    public static void registerLegacyMagic() {
        WormMagicIntegration.register();
        SummoningMagicIntegration.register();
        BoundaryMagicIntegration.register();
        MatouKariyaMasterIntegration.register();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
