package com.example.typemoonaddon;

import com.example.typemoonaddon.registry.AddonItems;
import com.example.typemoonaddon.registry.AddonMenus;
import com.example.typemoonaddon.registry.AddonMobEffects;
import com.example.typemoonaddon.registry.AddonEntities;
import com.example.typemoonaddon.registry.AddonBlocks;
import com.example.typemoonaddon.block.entity.AddonBlockEntities;
import com.example.typemoonaddon.registry.AddonFluids;
import com.example.typemoonaddon.registry.AddonAttachments;
import com.example.typemoonaddon.registry.AddonSounds;
import com.example.typemoonaddon.config.GameplayConfig;
import com.example.typemoonaddon.detection.DetectionAttachments;
import com.example.typemoonaddon.engravedworm.EngravedWormAttachments;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceAttachments;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceConfig;
import com.example.typemoonaddon.kimaris.KimarisAttachments;
import com.example.typemoonaddon.magic.WormMagicIntegration;
import com.example.typemoonaddon.magic.SummoningMagicIntegration;
import com.example.typemoonaddon.magic.BoundaryMagicIntegration;
import com.example.typemoonaddon.magic.MatouKariyaMasterIntegration;
import com.example.typemoonaddon.magic.ImaginaryDisplacementAttachments;
import com.example.typemoonaddon.storage.StorageAttachments;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.xxxjk.TYPE_MOON_WORLD.init.ModCreativeModeTabs;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import org.slf4j.Logger;

public final class TypeMoonAddon {
    /** Legacy compatibility namespace for the non-Sakura extensions retained by the core mod. */
    public static final String MOD_ID = "typemoonworld";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TypeMoonAddon(IEventBus modEventBus, ModContainer modContainer) {
        AddonFluids.register(modEventBus);
        AddonBlocks.register(modEventBus);
        AddonBlockEntities.register(modEventBus);
        AddonItems.register(modEventBus);
        AddonMenus.register(modEventBus);
        AddonMobEffects.register(modEventBus);
        AddonEntities.register(modEventBus);
        AddonSounds.register(modEventBus);
        AddonAttachments.register(modEventBus);
        StorageAttachments.register(modEventBus);
        ImaginaryDisplacementAttachments.register(modEventBus);
        KimarisAttachments.register(modEventBus);
        DetectionAttachments.register(modEventBus);
        EngravedWormAttachments.register(modEventBus);
        ImaginarySpaceAttachments.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, GameplayConfig.SPEC, "typemoonworld-sakura-gameplay.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, ImaginarySpaceConfig.SPEC, "typemoonworld-imaginary-space.toml");
        modEventBus.addListener(AddonEntities::registerAttributes);
        modEventBus.addListener(AddonEntities::registerSpawnPlacements);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreativeTabContents);
        LOGGER.info("Integrated Type Moon addon systems initialized");
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(AddonItems.MYSTIC_CODE_FRAGMENT);
            event.accept(AddonItems.IMAGINARY_PRIMER);
            event.accept(AddonItems.CREST_WORM);
            event.accept(AddonItems.WORM);
            event.accept(AddonItems.ENGRAVED_WORM);
            event.accept(AddonItems.HOLY_GRAIL_FRAGMENT);
            event.accept(AddonItems.VOID_RING_REGALIA);
            event.accept(AddonItems.MAGIC_PAGE_SPIRIT_SUMMONING);
            event.accept(AddonItems.MAGIC_PAGE_WRAITH_SERVITUDE);
            event.accept(AddonItems.MAGIC_PAGE_EVIL_SPIRIT_SUMMONING);
            event.accept(AddonItems.MAGIC_PAGE_WORM_MAGIC);
            event.accept(AddonItems.MAGIC_PAGE_WORM_CONTROL);
            event.accept(AddonItems.MAGIC_PAGE_ENGRAVED_WORM_OPERATION);
            event.accept(AddonItems.MAGIC_PAGE_BOUNDARY_ART);
            event.accept(AddonItems.MAGIC_PAGE_SENSING_BOUNDARY);
            event.accept(AddonItems.MAGIC_PAGE_WARNING_BOUNDARY);
            event.accept(AddonItems.MAGIC_PAGE_DEFENSE_BOUNDARY);
            event.accept(AddonItems.MAGIC_PAGE_SUGGESTION_BOUNDARY);
            event.accept(AddonItems.MAGIC_PAGE_ANTI_MAGIC_BOUNDARY);
            event.accept(AddonItems.MAGIC_PAGE_GUARD_BOUNDARY);
            event.accept(AddonItems.MAGIC_PAGE_INTERFERENCE_BOUNDARY);
        }
        if (event.getTab() == ModCreativeModeTabs.MAGIC_BOOKS_TAB.get()) {
            event.accept(AddonItems.MAGIC_BOOK_IMAGINARY_STORAGE);
            event.accept(AddonItems.MAGIC_PAGE_IMAGINARY_STORAGE);
            event.accept(AddonItems.MAGIC_BOOK_IMAGINARY_ABSORPTION);
            event.accept(AddonItems.MAGIC_PAGE_IMAGINARY_ABSORPTION);
            event.accept(AddonItems.MAGIC_BOOK_SPIRIT_SUMMONING);
            event.accept(AddonItems.MAGIC_BOOK_WRAITH_SERVITUDE);
            event.accept(AddonItems.MAGIC_BOOK_EVIL_SPIRIT_SUMMONING);
            event.accept(AddonItems.MAGIC_BOOK_WORM_MAGIC);
            event.accept(AddonItems.MAGIC_PAGE_WORM_MAGIC);
            event.accept(AddonItems.MAGIC_BOOK_WORM_CONTROL);
            event.accept(AddonItems.MAGIC_PAGE_WORM_CONTROL);
            event.accept(AddonItems.MAGIC_BOOK_ENGRAVED_WORM_OPERATION);
            event.accept(AddonItems.MAGIC_PAGE_ENGRAVED_WORM_OPERATION);
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
            // Creative tab contents can be rebuilt with a different feature/permission set;
            // an anchor item is not guaranteed to be present in every rebuild.
            event.accept(AddonItems.MANA_FURNACE.toStack(),
                CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
        if (event.getTab() == ModCreativeModeTabs.SPAWN_EGGS_TAB.get()) {
            event.accept(AddonItems.GILLES_DE_RAIS_CASTER_SPAWN_EGG);
            event.accept(AddonItems.GILLES_SEA_MONSTER_SPAWN_EGG);
            event.accept(AddonItems.GILLES_LARGE_SEA_MONSTER_SPAWN_EGG);
            event.accept(AddonItems.GILLES_HUGE_SEA_MONSTER_SPAWN_EGG);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        registerNonSakuraMagic();
    }

    /** Registers only legacy extensions that are still owned by the core jar. */
    public static void registerNonSakuraMagic() {
        WormMagicIntegration.register();
        SummoningMagicIntegration.register();
        BoundaryMagicIntegration.register();
        MatouKariyaMasterIntegration.register();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
