package net.xxxjk.TYPE_MOON_WORLD.init;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.xxxjk.TYPE_MOON_WORLD.entity.MerlinEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedusaPegasusEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RyougiShikiEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.StoneManEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.DragonfangSoldierEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

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
      event.put(ModEntities.HERACLES.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.SASAKI_KOJIRO.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.CU_CHULAINN.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.MEDEA.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.MEDUSA.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.CURSED_ARM_HASSAN.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.MEDUSA_PEGASUS.get(), MedusaPegasusEntity.createAttributes().build());
      event.put(ModEntities.DRAGONFANG_SOLDIER.get(), DragonfangSoldierEntity.createAttributes().build());
   }
}
