package net.xxxjk.TYPE_MOON_WORLD.init;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.xxxjk.TYPE_MOON_WORLD.client.gui.Basic_information_Screen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.Magical_attributes_Screen;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TypeMoonWorldModScreens {
    @SubscribeEvent
    public static void clientLoad(RegisterMenuScreensEvent event) {
        event.register(TypeMoonWorldModMenus.BASIC_INFORMATION.get(), Basic_information_Screen::new);
        event.register(TypeMoonWorldModMenus.MAGICAL_ATTRIBUTES.get(), Magical_attributes_Screen::new);
    }
}

