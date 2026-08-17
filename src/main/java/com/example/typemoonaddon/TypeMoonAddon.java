package com.example.typemoonaddon;

import com.example.typemoonaddon.registry.AddonItems;
import com.example.typemoonaddon.registry.AddonMenus;
import com.example.typemoonaddon.registry.AddonMobEffects;
import com.example.typemoonaddon.registry.AddonEntities;
import com.example.typemoonaddon.registry.AddonBlocks;
import com.example.typemoonaddon.registry.AddonFluids;
import com.example.typemoonaddon.registry.AddonAttachments;
import com.example.typemoonaddon.registry.AddonSounds;
import com.example.typemoonaddon.config.GameplayConfig;
import com.example.typemoonaddon.detection.DetectionAttachments;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceAttachments;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceConfig;
import com.example.typemoonaddon.kimaris.KimarisAttachments;
import com.example.typemoonaddon.magic.SakuraTypeMoonIntegration;
import com.example.typemoonaddon.magic.ImaginaryDisplacementAttachments;
import com.example.typemoonaddon.storage.StorageAttachments;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.xxxjk.TYPE_MOON_WORLD.init.ModCreativeModeTabs;
import org.slf4j.Logger;

public final class TypeMoonAddon {
    public static final String MOD_ID = "typemoonworld";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TypeMoonAddon(IEventBus modEventBus, ModContainer modContainer) {
        AddonFluids.register(modEventBus);
        AddonBlocks.register(modEventBus);
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
        ImaginarySpaceAttachments.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, GameplayConfig.SPEC, "typemoonworld-sakura-gameplay.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, ImaginarySpaceConfig.SPEC, "typemoonworld-imaginary-space.toml");
        modEventBus.addListener(AddonEntities::registerAttributes);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreativeTabContents);
        LOGGER.info("Integrated Type Moon addon systems initialized");
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(AddonItems.MYSTIC_CODE_FRAGMENT);
            event.accept(AddonItems.IMAGINARY_PRIMER);
            event.accept(AddonItems.CREST_WORM);
            event.accept(AddonItems.HOLY_GRAIL_FRAGMENT);
            event.accept(AddonItems.VOID_RING_REGALIA);
        }
        if (event.getTab() == ModCreativeModeTabs.MAGIC_BOOKS_TAB.get()) {
            event.accept(AddonItems.MAGIC_BOOK_IMAGINARY_STORAGE);
            event.accept(AddonItems.MAGIC_PAGE_IMAGINARY_STORAGE);
            event.accept(AddonItems.MAGIC_BOOK_IMAGINARY_ABSORPTION);
            event.accept(AddonItems.MAGIC_PAGE_IMAGINARY_ABSORPTION);
        }
        if (event.getTab() == ModCreativeModeTabs.SERVANT_CARDS_TAB.get()) {
            event.accept(AddonItems.MASTER_CARD_MATOU_SAKURA);
            event.accept(AddonItems.MASTER_CARD_MATOU_SAKURA_ALTER);
            event.accept(AddonItems.MASTER_CARD_MATOU_SAKURA_FHA);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        SakuraTypeMoonIntegration.register();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
