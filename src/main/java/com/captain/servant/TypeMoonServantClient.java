package com.captain.servant;

import com.captain.servant.client.SpiritualizationClient;
import com.captain.servant.client.TelepathyClient;
import com.captain.servant.client.KnightArmoryScreen;
import com.captain.servant.network.FateSupportNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = "typemoonworld", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class TypeMoonServantClient {
    private TypeMoonServantClient() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        FateSupportNetwork.setArmoryScreen(KnightArmoryScreen::open);
        NeoForge.EVENT_BUS.register(new SpiritualizationClient());
        NeoForge.EVENT_BUS.register(new TelepathyClient());
    }

}
