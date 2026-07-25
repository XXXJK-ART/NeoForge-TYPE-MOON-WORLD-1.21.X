package net.xxxjk.TYPE_MOON_WORLD.martial;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.xxxjk.TYPE_MOON_WORLD.entity.KendoMasterEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Keeps kendo sparring isolated to the dojo master and applies its failure rules. */
@EventBusSubscriber(modid = "typemoonworld")
public final class KendoEvents {
   private KendoEvents() {}

   @SubscribeEvent
   public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
      event.getEntity().getPersistentData().remove("TypeMoonKendoSparring");
      event.getEntity().getPersistentData().remove("TypeMoonKendoSparringMaster");
   }

   @SubscribeEvent
   public static void onDamage(LivingIncomingDamageEvent event) {
      if (event.getSource().getEntity() instanceof Player attacker
         && attacker.getPersistentData().getBoolean("TypeMoonKendoSparring")) {
         boolean allowed = attacker.getPersistentData().hasUUID("TypeMoonKendoSparringMaster")
            && event.getEntity().getUUID().equals(attacker.getPersistentData().getUUID("TypeMoonKendoSparringMaster"))
            && attacker instanceof net.minecraft.server.level.ServerPlayer serverPlayer
            && KendoCombatService.isMartialDamage(serverPlayer);
         if (!allowed) event.setCanceled(true);
      }

      if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer defender)
         || !defender.getPersistentData().getBoolean("TypeMoonKendoSparring")) return;
      boolean allowed = defender.getPersistentData().hasUUID("TypeMoonKendoSparringMaster")
         && event.getSource().getEntity() != null
         && event.getSource().getEntity().getUUID().equals(defender.getPersistentData().getUUID("TypeMoonKendoSparringMaster"));
      if (!allowed) {
         event.setCanceled(true);
         return;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = defender.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      String schoolId = defender.getPersistentData().getString("TypeMoonKendoSparringSchool");
      KendoSchool school = "tennen_rishin_ryu".equals(schoolId) ? KendoSchool.TENNEN : KendoSchool.HOKUSHIN;
      if (school.proficiency(vars) < school.preMasterCap() && defender.getHealth() - event.getAmount() <= 0.0F) {
         event.setCanceled(true);
         defender.setHealth(1.0F);
         if (defender.serverLevel().getEntity(defender.getPersistentData().getUUID("TypeMoonKendoSparringMaster")) instanceof KendoMasterEntity master)
            master.endDuel(defender, false);
      }
   }
}
