package net.xxxjk.TYPE_MOON_WORLD.init;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.xxxjk.TYPE_MOON_WORLD.entity.MerlinEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RyougiShikiEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.StoneManEntity;

@EventBusSubscriber(
   modid = "typemoonworld",
   bus = Bus.MOD
)
public class ModEventBusEvents {
   @SubscribeEvent
   public static void registerAttributes(EntityAttributeCreationEvent event) {
      event.put(ModEntities.RYOUGI_SHIKI.get(), RyougiShikiEntity.createAttributes().build());
      event.put(ModEntities.MERLIN.get(), MerlinEntity.createAttributes().build());
      event.put(ModEntities.STONE_MAN.get(), StoneManEntity.createAttributes().build());
      event.put(ModEntities.MYSTIC_MAGICIAN.get(), MysticMagicianEntity.createAttributes().build());
   }
}
