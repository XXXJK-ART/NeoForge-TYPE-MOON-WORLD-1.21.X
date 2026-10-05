package net.xxxjk.typemoonworld.api;

import net.minecraft.world.entity.LivingEntity;


/** Optional magic adapters supplied by the addon at runtime. */
public final class AddonMagicHooks {
   public interface Provider {
      double adjustNaturalManaRegen(LivingEntity entity, double value);
      boolean swapNpc(LivingEntity caster, LivingEntity target);
      int bladeManaCost();
      double bladeSpeed();
      boolean castNpcBlade(LivingEntity caster, LivingEntity target, double proficiency);
      boolean castNpcDetection(LivingEntity caster, LivingEntity target, double proficiency);
      boolean castNpcImaginarySpace(LivingEntity caster, LivingEntity target);
   }

   private static final Provider EMPTY = new Provider() {
      public double adjustNaturalManaRegen(LivingEntity entity, double value) { return value; }
      public boolean swapNpc(LivingEntity caster, LivingEntity target) { return false; }
      public int bladeManaCost() { return 0; }
      public double bladeSpeed() { return 1.0D; }
      public boolean castNpcBlade(LivingEntity caster, LivingEntity target, double proficiency) { return false; }
      public boolean castNpcDetection(LivingEntity caster, LivingEntity target, double proficiency) { return false; }
      public boolean castNpcImaginarySpace(LivingEntity caster, LivingEntity target) { return false; }
   };
   private static volatile Provider provider = EMPTY;

   private AddonMagicHooks() { }
   public static void register(Provider value) { provider = value == null ? EMPTY : value; }
   public static double adjustNaturalManaRegen(LivingEntity entity, double value) { return provider.adjustNaturalManaRegen(entity, value); }
   public static boolean swapNpc(LivingEntity caster, LivingEntity target) { return provider.swapNpc(caster, target); }
   public static int bladeManaCost() { return provider.bladeManaCost(); }
   public static double bladeSpeed() { return provider.bladeSpeed(); }
   public static boolean castNpcBlade(LivingEntity caster, LivingEntity target, double proficiency) { return provider.castNpcBlade(caster, target, proficiency); }
   public static boolean castNpcDetection(LivingEntity caster, LivingEntity target, double proficiency) { return provider.castNpcDetection(caster, target, proficiency); }
   public static boolean castNpcImaginarySpace(LivingEntity caster, LivingEntity target) { return provider.castNpcImaginarySpace(caster, target); }
}
