package net.xxxjk.TYPE_MOON_WORLD.servant.jeanne;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;

/** Shared NPC and servant-card voice hooks. */
public final class JeanneAlterVoice {
   private static final String LAST_VOICE_TICK = "JeanneAlterVoiceLastTick";

   private JeanneAlterVoice() {}

   public static void summon(LivingEntity entity) {
      play(entity, ModSounds.JEANNE_ALTER_VOICE_SUMMON.get(), 0, true);
   }

   public static void attack(LivingEntity entity) {
      if (entity == null || entity.getRandom().nextFloat() > 0.48F) return;
      play(entity, ModSounds.JEANNE_ALTER_VOICE_ATTACK.get(), 70, false);
   }

   public static void skill(LivingEntity entity) {
      play(entity, ModSounds.JEANNE_ALTER_VOICE_SKILL.get(), 36, false);
   }

   public static void noblePhantasm(LivingEntity entity) {
      play(entity, ModSounds.JEANNE_ALTER_VOICE_NP.get(), 0, true);
   }

   public static void defeat(LivingEntity entity) {
      play(entity, ModSounds.JEANNE_ALTER_VOICE_DEFEAT.get(), 0, true);
   }

   public static void clear(LivingEntity entity) {
      if (entity != null) entity.getPersistentData().remove(LAST_VOICE_TICK);
   }

   private static void play(LivingEntity entity, SoundEvent sound, int cooldown, boolean forced) {
      if (entity == null || sound == null || !(entity.level() instanceof ServerLevel level)) return;
      long now = level.getGameTime();
      long last = entity.getPersistentData().getLong(LAST_VOICE_TICK);
      if (!forced && entity.getPersistentData().contains(LAST_VOICE_TICK) && now - last < cooldown) return;
      entity.getPersistentData().putLong(LAST_VOICE_TICK, now);
      level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.VOICE, 1.15F, 1.0F);
   }
}
