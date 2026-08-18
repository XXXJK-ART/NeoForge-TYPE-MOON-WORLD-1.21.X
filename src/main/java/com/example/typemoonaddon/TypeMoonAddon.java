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
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.xxxjk.TYPE_MOON_WORLD.init.ModCreativeModeTabs;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
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
        if (event.getTab() == ModCreativeModeTabs.TYPE_MOON_WORLD_TAB.get()) {
            event.insertAfter(ModItems.KIKU_ICHIMONJI_NORIMUNE.toStack(), AddonItems.PRELATIS_SPELLBOOK.toStack(),
                CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
        if (event.getTab() == ModCreativeModeTabs.SERVANT_CARDS_TAB.get()) {
            ItemStack afterMasterCards = new ItemStack(ModItems.MASTER_CARD_LEFF_LAYNOR_FLAUROS.get());
            ItemStack sakura = AddonItems.MASTER_CARD_MATOU_SAKURA.toStack();
            ItemStack sakuraAlter = AddonItems.MASTER_CARD_MATOU_SAKURA_ALTER.toStack();
            ItemStack sakuraFha = AddonItems.MASTER_CARD_MATOU_SAKURA_FHA.toStack();
            event.insertAfter(afterMasterCards, sakura, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(sakura, sakuraAlter, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(sakuraAlter, sakuraFha, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(AddonItems.CURSED_ARMOR_RENDER);
        }
        if (event.getTab() == ModCreativeModeTabs.SPAWN_EGGS_TAB.get() || event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(AddonItems.GILLES_DE_RAIS_CASTER_SPAWN_EGG);
            event.accept(AddonItems.GILLES_SEA_MONSTER_SPAWN_EGG);
            event.accept(AddonItems.GILLES_LARGE_SEA_MONSTER_SPAWN_EGG);
            event.accept(AddonItems.GILLES_HUGE_SEA_MONSTER_SPAWN_EGG);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        SakuraTypeMoonIntegration.register();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
