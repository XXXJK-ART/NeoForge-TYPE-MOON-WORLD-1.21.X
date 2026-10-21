package net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;

/** The seven-second yellow-spear callout has one shared lock for cards, NPCs and standalone spears. */
public final class DiarmuidVoiceService {
   public static final String YELLOW_VOICE_READY = "DiarmuidYellowVoiceReady";
   public static final String VOICE_LOCK = "DiarmuidVoiceLockUntil";
   public static final int YELLOW_VOICE_COOLDOWN = 200;
   public static final int YELLOW_VOICE_DURATION = 142;
   private DiarmuidVoiceService() { }

   public static boolean yellowReady(LivingEntity owner) {
      return owner.level().getGameTime() >= owner.getPersistentData().getLong(YELLOW_VOICE_READY);
   }
   public static boolean locked(LivingEntity owner) {
      return owner.level().getGameTime() < owner.getPersistentData().getLong(VOICE_LOCK);
   }
   public static void lock(LivingEntity owner, int ticks) {
      owner.getPersistentData().putLong(VOICE_LOCK, owner.level().getGameTime() + ticks);
   }
   public static boolean yellowHit(LivingEntity owner) {
      if (!(owner.level() instanceof ServerLevel level) || !yellowReady(owner) || locked(owner)) return false;
      long now = level.getGameTime();
      owner.getPersistentData().putLong(YELLOW_VOICE_READY, now + YELLOW_VOICE_COOLDOWN);
      lock(owner, YELLOW_VOICE_DURATION);
      owner.getPersistentData().putLong("TypeMoonVoiceLockUntil", now + YELLOW_VOICE_DURATION);
      level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), ModSounds.DIARMUID_UA_DUIBHNE_VOICE_NP.get(), SoundSource.VOICE, 1.05F, 1.0F);
      return true;
   }
}
