package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.xxxjk.TYPE_MOON_WORLD.servant.concealment.ServantConcealment;

/** Player-card toggle for the shared A-rank presence-concealment rules. */
public final class HundredFacesPresenceService {
   private static final String ENABLED = "HundredFacesPresenceEnabled";
   private static final String OWNS_INVISIBILITY = "HundredFacesPresenceOwnsInvisibility";

   private HundredFacesPresenceService() { }

   public static boolean enabled(ServerPlayer player) {
      return player.getPersistentData().getBoolean(ENABLED);
   }

   public static boolean toggle(ServerPlayer player) {
      if (!ServantCardHundredFacesHassanSkills.isActiveCard(player)) return false;
      if (enabled(player)) {
         set(player, false);
         return true;
      }
      if (!ServantConcealment.canActivatePresenceConcealment(player)) return false;
      set(player, true);
      return true;
   }

   public static void set(ServerPlayer player, boolean enabled) {
      var data = player.getPersistentData();
      data.putBoolean(ENABLED, enabled);
      if (!enabled) {
         if (data.getBoolean(OWNS_INVISIBILITY)) player.removeEffect(MobEffects.INVISIBILITY);
         data.remove(OWNS_INVISIBILITY);
         return;
      }
      applyOwnedInvisibility(player);
   }

   public static void tick(ServerPlayer player) {
      if (!enabled(player)) return;
      if (!ServantConcealment.canActivatePresenceConcealment(player)) {
         set(player, false);
         return;
      }
      var effect = player.getEffect(MobEffects.INVISIBILITY);
      if (effect == null) {
         applyOwnedInvisibility(player);
      } else if (player.getPersistentData().getBoolean(OWNS_INVISIBILITY) && effect.getDuration() < 40) {
         player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
      }
   }

   private static void applyOwnedInvisibility(ServerPlayer player) {
      if (player.hasEffect(MobEffects.INVISIBILITY)) {
         player.getPersistentData().putBoolean(OWNS_INVISIBILITY, false);
         return;
      }
      player.getPersistentData().putBoolean(OWNS_INVISIBILITY, true);
      player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
   }
}
