package net.xxxjk.TYPE_MOON_WORLD.servant.skill;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

/**
 * Shared MP gate for noble phantasms. Ordinary skills deliberately do not use
 * this service: they must not consume the reserve kept for an NP.
 */
public final class ServantNoblePhantasmResourceService {
   public static final String POWER_SCALE_TAG = "ServantNpPowerScale";
   public static final String OVERDRAFT_TAG = "ServantNpOverdraft";
   public static final String OVERDRAFT_WEAK_UNTIL_TAG = "ServantNpOverdraftWeakUntil";
   public static final String OVERDRAFT_COOLDOWN_MULTIPLIER_TAG = "ServantNpOverdraftCooldownMultiplier";
   public static final int OVERDRAFT_WEAK_TICKS = 10 * 20;

   private static final ResourceLocation OVERDRAFT_SPEED_ID =
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_np_overdraft_speed");
   private static final ResourceLocation OVERDRAFT_ATTACK_ID =
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_np_overdraft_attack");

   public record CastDecision(
      boolean allowed,
      boolean overdraft,
      double requiredCost,
      double availableMp,
      double powerScale,
      double actualMpCost
   ) {
      public static CastDecision denied(double cost, double available) {
         return new CastDecision(false, false, cost, available, 0.0, 0.0);
      }
   }

   private ServantNoblePhantasmResourceService() {
   }

   public static CastDecision evaluateNpcCast(ServantEntity caster, double requiredCost) {
      double cost = Math.max(0.0, requiredCost);
      double available = Math.max(0.0, caster == null ? 0.0 : caster.getCurrentMp());
      if (caster == null) {
         return CastDecision.denied(cost, available);
      }
      if (cost <= 0.0) {
         return new CastDecision(true, false, 0.0, available, 1.0, 0.0);
      }
      if (available + 1.0E-6 >= cost) {
         return new CastDecision(true, false, cost, available, 1.0, cost);
      }
      boolean canOverdraft = caster.getHealth() < caster.getMaxHealth() * 0.5F
         && available + 1.0E-6 < cost;
      return canOverdraft
         ? new CastDecision(true, true, cost, available, 1.0, available)
         : CastDecision.denied(cost, available);
   }

   public static boolean canAttemptNpcCast(ServantEntity caster, double requiredCost) {
      return evaluateNpcCast(caster, requiredCost).allowed()
         && !isOverdraftWeak(caster);
   }

   public static void commitNpcCast(ServantEntity caster, CastDecision decision) {
      if (caster == null || decision == null || !decision.allowed()) {
         return;
      }
      if (decision.overdraft()) {
         caster.setCurrentMp(0.0);
         caster.getPersistentData().putLong(
            OVERDRAFT_WEAK_UNTIL_TAG,
            caster.level().getGameTime() + OVERDRAFT_WEAK_TICKS
         );
      } else {
         caster.setCurrentMp(Math.max(0.0, caster.getCurrentMp() - decision.actualMpCost()));
         if (!isOverdraftWeak(caster)) {
            caster.getPersistentData().remove(OVERDRAFT_TAG);
            caster.getPersistentData().remove(OVERDRAFT_COOLDOWN_MULTIPLIER_TAG);
         }
      }
      caster.getPersistentData().putDouble(POWER_SCALE_TAG, 1.0);
      caster.getPersistentData().putBoolean(OVERDRAFT_TAG, decision.overdraft());
      caster.getPersistentData().putInt(
         OVERDRAFT_COOLDOWN_MULTIPLIER_TAG,
         decision.overdraft() ? 2 : 1
      );
   }

   public static double powerScale(ServantEntity caster) {
      if (caster == null) {
         return 1.0;
      }
      double scale = caster.getPersistentData().getDouble(POWER_SCALE_TAG);
      return Double.isFinite(scale) && scale > 0.0 ? Math.min(1.0, scale) : 1.0;
   }

   public static double scale(ServantEntity caster, double value) {
      return Math.max(0.0, value) * powerScale(caster);
   }

   public static int cooldownMultiplier(ServantEntity caster) {
      return caster != null && caster.getPersistentData().getBoolean(OVERDRAFT_TAG) ? 2 : 1;
   }

   public static int cooldownTicks(ServantEntity caster, int baseTicks) {
      long scaled = (long)Math.max(0, baseTicks) * cooldownMultiplier(caster);
      return (int)Math.min(Integer.MAX_VALUE, scaled);
   }

   public static int cooldownTicks(CastDecision decision, int baseTicks) {
      long scaled = (long)Math.max(0, baseTicks) * (decision != null && decision.overdraft() ? 2 : 1);
      return (int)Math.min(Integer.MAX_VALUE, scaled);
   }

   public static void storePowerScale(ServantEntity caster, double powerScale) {
      if (caster != null) {
         caster.getPersistentData().putDouble(POWER_SCALE_TAG,
            Math.max(0.2, Math.min(1.0, powerScale)));
      }
   }

   public static void clearPowerScale(ServantEntity caster) {
      if (caster != null) {
         caster.getPersistentData().remove(POWER_SCALE_TAG);
      }
   }

   public static boolean isOverdraftWeak(ServantEntity caster) {
      return caster != null
         && caster.getPersistentData().getLong(OVERDRAFT_WEAK_UNTIL_TAG) > caster.level().getGameTime();
   }

   public static void tick(ServantEntity caster) {
      if (caster == null || caster.level().isClientSide()) {
         return;
      }
      long now = caster.level().getGameTime();
      if (isOverdraftWeak(caster)) {
         updateModifier(caster.getAttribute(Attributes.MOVEMENT_SPEED), OVERDRAFT_SPEED_ID, -0.30);
         updateModifier(caster.getAttribute(Attributes.ATTACK_DAMAGE), OVERDRAFT_ATTACK_ID, -0.50);
         return;
      }
      removeModifier(caster.getAttribute(Attributes.MOVEMENT_SPEED), OVERDRAFT_SPEED_ID);
      removeModifier(caster.getAttribute(Attributes.ATTACK_DAMAGE), OVERDRAFT_ATTACK_ID);
      if (caster.getPersistentData().getLong(OVERDRAFT_WEAK_UNTIL_TAG) > 0L
         && caster.getPersistentData().getLong(OVERDRAFT_WEAK_UNTIL_TAG) <= now) {
         caster.getPersistentData().remove(OVERDRAFT_WEAK_UNTIL_TAG);
         caster.getPersistentData().remove(OVERDRAFT_TAG);
         caster.getPersistentData().remove(OVERDRAFT_COOLDOWN_MULTIPLIER_TAG);
      }
   }

   private static void updateModifier(AttributeInstance attribute, ResourceLocation id, double amount) {
      if (attribute == null) {
         return;
      }
      AttributeModifier existing = attribute.getModifier(id);
      if (existing != null && Math.abs(existing.amount() - amount) < 1.0E-6
         && existing.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
         return;
      }
      if (existing != null) {
         attribute.removeModifier(id);
      }
      attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
   }

   private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null && attribute.getModifier(id) != null) {
         attribute.removeModifier(id);
      }
   }
}
