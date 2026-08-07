package com.example.typemoonaddon;

import com.example.typemoonaddon.registry.AddonItems;
import com.example.typemoonaddon.registry.AddonMenus;
import com.example.typemoonaddon.registry.AddonMobEffects;
import com.example.typemoonaddon.registry.AddonEntities;
import com.example.typemoonaddon.registry.AddonSounds;
import com.example.typemoonaddon.config.AddonCommandConfig;
import com.example.typemoonaddon.detection.DetectionAttachments;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceAttachments;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceConfig;
import com.example.typemoonaddon.kimaris.KimarisAttachments;
import com.example.typemoonaddon.magic.ImaginaryDisplacementAttachments;
import com.example.typemoonaddon.storage.StorageAttachments;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

public final class TypeMoonAddon {
    public static final String MOD_ID = "typemoonworld";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TypeMoonAddon(IEventBus modEventBus, ModContainer modContainer) {
        AddonItems.register(modEventBus);
        AddonMenus.register(modEventBus);
        AddonMobEffects.register(modEventBus);
        AddonEntities.register(modEventBus);
        AddonSounds.register(modEventBus);
        StorageAttachments.register(modEventBus);
        ImaginaryDisplacementAttachments.register(modEventBus);
        KimarisAttachments.register(modEventBus);
        DetectionAttachments.register(modEventBus);
        ImaginarySpaceAttachments.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, ImaginarySpaceConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.COMMON, AddonCommandConfig.SPEC, "typemoonworld-admin.toml");
        modEventBus.addListener(this::addCreativeTabContents);
        LOGGER.info("Integrated Type Moon addon systems initialized");
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(AddonItems.MYSTIC_CODE_FRAGMENT);
        }
    }
}
