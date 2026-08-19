package net.xxxjk.TYPE_MOON_WORLD.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class NightVisionEffectSource {
   public static final String PLAYER_CLAIRVOYANCE = "TypeMoonPassiveClairvoyanceNightVision";
   public static final String NPC_CLAIRVOYANCE = "TypeMoonNpcPassiveClairvoyanceNightVision";
   public static final String REINFORCEMENT_SELF_SIGHT = "TypeMoonReinforcementSelfSightNightVision";
   public static final String REINFORCEMENT_OTHER_SIGHT = "TypeMoonReinforcementOtherSightNightVision";
   public static final String NPC_REINFORCEMENT_SIGHT = "TypeMoonNpcReinforcementSightNightVision";
   public static final String GILGAMESH_CARD = "TypeMoonGilgameshCardNightVision";

   private NightVisionEffectSource() {
   }

   public static void addHidden(LivingEntity entity, int duration, String tag) {
      if (entity == null || tag == null || tag.isBlank()) return;
      entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, false, false));
      mark(entity, tag);
   }

   public static void mark(LivingEntity entity, String tag) {
      if (entity == null || tag == null || tag.isBlank()) return;
      entity.getPersistentData().putBoolean(tag, true);
   }

   public static void clearHiddenIfTagged(LivingEntity entity, String tag, int maxManagedDuration) {
      if (entity == null || tag == null || tag.isBlank()) return;
      CompoundTag data = entity.getPersistentData();
      if (!data.getBoolean(tag)) return;
      MobEffectInstance current = entity.getEffect(MobEffects.NIGHT_VISION);
      if (isHiddenManagedNightVision(current, maxManagedDuration)) {
         entity.removeEffect(MobEffects.NIGHT_VISION);
      }
      data.remove(tag);
   }

   public static void clearHiddenIfAnyTagged(LivingEntity entity, int maxManagedDuration, String... tags) {
      if (tags == null) return;
      for (String tag : tags) {
         clearHiddenIfTagged(entity, tag, maxManagedDuration);
      }
   }

   private static boolean isHiddenManagedNightVision(MobEffectInstance effect, int maxManagedDuration) {
      return effect != null
         && !effect.isVisible()
         && !effect.showIcon()
         && effect.getDuration() <= Math.max(1, maxManagedDuration);
   }
}
