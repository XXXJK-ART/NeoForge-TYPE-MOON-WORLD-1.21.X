package com.captain.servant;

import com.captain.servant.command.FateSupportCommands;
import com.captain.servant.combat.AttackFeedbackService;
import com.captain.servant.compat.superbwarfare.SuperbWarfareLoader;
import com.captain.servant.item.ModItems;
import com.captain.servant.leyline.ArtificialLeylineFallbackService;
import com.captain.servant.network.FateSupportNetwork;
import com.captain.servant.spiritual.SpiritualizationService;
import com.captain.servant.voice.VoiceCommunicationService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.xxxjk.typemoonworld.api.AddonEntrypoint;

/** Initializes the former Fate Support addon inside the main mod jar. */
public final class FateSupportIntegration implements AddonEntrypoint {
    @Override
    public void initialize(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreativeTabContents);

        NeoForge.EVENT_BUS.register(new VoiceCommunicationService());
        NeoForge.EVENT_BUS.register(new SpiritualizationService());
        NeoForge.EVENT_BUS.register(new AttackFeedbackService());
        NeoForge.EVENT_BUS.register(new ArtificialLeylineFallbackService());
        NeoForge.EVENT_BUS.register(new FateSupportCommands());
        SuperbWarfareLoader.initialize();
    }

    @Override
    public void registerPayloads(PayloadRegistrar registrar) {
        FateSupportNetwork.register(registrar);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        TypeMoonServant.LOGGER.info("Fate Support content integrated into Type Moon World");
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        ResourceLocation tabId = ResourceLocation.fromNamespaceAndPath("typemoonworld", "type_moon_world_tab");
        if (!event.getTabKey().location().equals(tabId)) {
            return;
        }

        ResourceLocation anchorId = ResourceLocation.fromNamespaceAndPath("typemoonworld", "excalibur_gallatin");
        ItemStack messageStone = new ItemStack(ModItems.MESSAGE_STONE.get());
        if (BuiltInRegistries.ITEM.containsKey(anchorId)) {
            Item anchor = BuiltInRegistries.ITEM.get(anchorId);
            event.insertAfter(new ItemStack(anchor), messageStone,
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        } else {
            event.accept(messageStone, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}
