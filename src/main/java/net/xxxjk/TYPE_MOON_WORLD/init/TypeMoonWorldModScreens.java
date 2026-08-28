package net.xxxjk.TYPE_MOON_WORLD.init;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.Basic_information_Screen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.GemCarvingTableScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.Magical_attributes_Screen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.MagicResearchTableScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.MagicCopyingTableScreen;

@EventBusSubscriber(
   modid = "typemoonworld",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public class TypeMoonWorldModScreens {
   @SubscribeEvent
   public static void clientLoad(RegisterMenuScreensEvent event) {
      event.register(TypeMoonWorldModMenus.BASIC_INFORMATION.get(), Basic_information_Screen::new);
      event.register(TypeMoonWorldModMenus.MAGICAL_ATTRIBUTES.get(), Magical_attributes_Screen::new);
      event.register(TypeMoonWorldModMenus.GEM_CARVING_TABLE.get(), GemCarvingTableScreen::new);
      event.register(TypeMoonWorldModMenus.MAGIC_RESEARCH_TABLE.get(), MagicResearchTableScreen::new);
      event.register(TypeMoonWorldModMenus.MAGIC_COPYING_TABLE.get(), MagicCopyingTableScreen::new);
   }
}
