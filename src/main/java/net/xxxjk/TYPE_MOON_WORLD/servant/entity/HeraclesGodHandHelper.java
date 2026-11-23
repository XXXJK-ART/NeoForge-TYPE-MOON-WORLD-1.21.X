package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.heracles.GodHandService;

/** Compatibility adapter. Shared state and rules live in GodHandService. */
public final class HeraclesGodHandHelper {
   private HeraclesGodHandHelper() {}
   public static boolean hasGodHand(LivingEntity target) {
      if (target == null || !target.getPersistentData().getBoolean("GodHandActive")) return false;
      var definition=ServantIdentityHelper.definitionOf(target);
      return target instanceof HeraclesEntity || definition!=null && "heracles".equals(definition.id());
   }
   public static float applyAntiHeraclesNoblePhantasmSpecialAttack(LivingEntity target, float damage) { return damage; }
   public static boolean isAdaptedToCybele(LivingEntity target) { return GodHandService.immune(target,"medusa:cybele"); }
   public static boolean isAdaptedToZabaniya(LivingEntity target) { return GodHandService.immune(target,"cursed_arm_hassan:zabaniya"); }
   public static boolean consumeLifeForCybele(LivingEntity target) { return GodHandService.special(target,"medusa:cybele"); }
   public static boolean consumeLifeForZabaniya(LivingEntity target) { return GodHandService.special(target,"cursed_arm_hassan:zabaniya"); }
   public static boolean consumedLifeAfterLethalHit(LivingEntity target,int livesBeforeHit) {
      return hasGodHand(target) && livesBeforeHit>0 && target.getPersistentData().getInt("GodHandLives")==livesBeforeHit-1;
   }
   public static void applyAdaptedSlow(LivingEntity target,int ticks) { }
}
