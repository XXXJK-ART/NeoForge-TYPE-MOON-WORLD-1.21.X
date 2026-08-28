package io.github.typemoonaddon;

import io.github.typemoonaddon.config.GameplayConfig;
import io.github.typemoonaddon.client.ClientModEvents;
import io.github.typemoonaddon.magic.TypeMoonIntegration;
import io.github.typemoonaddon.network.AddonNetwork;
import io.github.typemoonaddon.registry.ModAttachments;
import io.github.typemoonaddon.registry.ModBlocks;
import io.github.typemoonaddon.registry.ModEffects;
import io.github.typemoonaddon.registry.ModEntities;
import io.github.typemoonaddon.registry.ModFluids;
import io.github.typemoonaddon.registry.ModItems;
import io.github.typemoonaddon.registry.ModMenus;
import io.github.typemoonaddon.magic.VoidRingRegaliaService;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

@Mod(TypeMoonAddon.MOD_ID)
public final class TypeMoonAddon {
    public static final String MOD_ID = "typemoonaddon";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceKey<CreativeModeTab> TYPE_MOON_WORLD_TAB = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB,
        ResourceLocation.fromNamespaceAndPath("typemoonworld", "type_moon_world_tab")
    );
    private static final ResourceKey<CreativeModeTab> SERVANT_CARDS_TAB = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB,
        ResourceLocation.fromNamespaceAndPath("typemoonworld", "servant_cards_tab")
    );

    public TypeMoonAddon(IEventBus modEventBus, ModContainer modContainer) {
        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModAttachments.ATTACHMENTS.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);

        modEventBus.addListener(AddonNetwork::register);
        modEventBus.addListener(ModEntities::registerAttributes);
        modEventBus.addListener(this::commonSetup);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientModEvents.register(modEventBus);
        }
        modContainer.registerConfig(ModConfig.Type.SERVER, GameplayConfig.SPEC);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(SERVANT_CARDS_TAB)) {
            event.accept(VoidRingRegaliaService.create(event.getParameters().holders()));
        }
        if (event.getTabKey().equals(TYPE_MOON_WORLD_TAB)) {
            event.accept(ModItems.IMAGINARY_PRIMER.get());
            event.accept(ModItems.CREST_WORM.get());
            event.accept(ModItems.HOLY_GRAIL_FRAGMENT.get());
            event.accept(ModItems.SHADOW_FAMILIAR_SPAWN_EGG.get());
            event.accept(ModItems.BLACK_SHADOW_SPAWN_EGG.get());
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Type Moon World freezes public extension registries in its own
        // common-setup work queue. Register during event dispatch, before that
        // queue is drained, rather than enqueueing behind the freeze task.
        TypeMoonIntegration.register();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
