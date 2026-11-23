package net.xxxjk.TYPE_MOON_WORLD.client;

import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantBaseAction;

/** Distinguishes a tap from sustained input without changing the actual attack cadence. */
public final class ServantBaseAttackInput {
   private int action = ServantBaseAction.NONE;
   private int previousWeapon = ServantBaseAction.NONE;
   private long lastSent;
   private long lastAttack = Long.MIN_VALUE;

   public int attack(boolean emptyHand, long now) {
      int weapon = emptyHand ? ServantBaseAction.FIST : ServantBaseAction.SWORD;
      boolean repeated = previousWeapon == weapon && now >= lastAttack && now - lastAttack <= 10;
      previousWeapon = weapon;
      lastAttack = now;
      int next = repeated ? combo(weapon) : weapon;
      if (action == next && ServantBaseAction.isContinuous(action)) return ServantBaseAction.NONE;
      action = next;
      lastSent = now;
      return action;
   }

   public int tick(boolean held, boolean allowed, long now) {
      if (!held || !allowed || now - lastAttack > 10) {
         boolean continuous = ServantBaseAction.isContinuous(action);
         action = ServantBaseAction.NONE;
         if (!allowed) { previousWeapon = ServantBaseAction.NONE; lastAttack = Long.MIN_VALUE; }
         return continuous ? ServantBaseAction.STOP : ServantBaseAction.NONE;
      }
      if (action == ServantBaseAction.NONE) return ServantBaseAction.NONE;
      if (ServantBaseAction.isContinuous(action) && now - lastSent >= 5) {
         lastSent = now;
         return action;
      }
      return ServantBaseAction.NONE;
   }

   public boolean isContinuous() { return ServantBaseAction.isContinuous(action); }

   private static int combo(int weapon) {
      return weapon == ServantBaseAction.FIST ? ServantBaseAction.FIST_COMBO : ServantBaseAction.SWORD_COMBO;
   }
}
