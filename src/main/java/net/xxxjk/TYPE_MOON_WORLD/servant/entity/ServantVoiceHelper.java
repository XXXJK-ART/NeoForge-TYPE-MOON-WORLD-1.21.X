package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;

public final class ServantVoiceHelper {
   private static final String GLOBAL_VOICE_TICK_TAG = "TypeMoonVoiceGlobalTick";
   private static final String CATEGORY_VOICE_TICK_PREFIX = "TypeMoonVoice.";
   private static final int GLOBAL_VOICE_COOLDOWN = 40;
   private static final int ATTACK_VOICE_COOLDOWN = 90;
   private static final int ROAR_VOICE_COOLDOWN = 160;
   private static final int SPECIAL_VOICE_COOLDOWN = 200;
   private static final int VICTORY_VOICE_COOLDOWN = 320;
   private static final int FAIL_VOICE_COOLDOWN = 80;

   private ServantVoiceHelper() {
   }

   public static void tryPlayAttack(ServantEntity servant) {
      if (isSasakiKojiro(servant)) {
         if (servant.getRandom().nextFloat() > 0.45F) {
            return;
         }
         playVoice(servant, "attack", ATTACK_VOICE_COOLDOWN, 1.0F, 1.0F, ModSounds.SASAKI_KOJIRO_VOICE_ATTACK.get());
      } else if (isCuChulainn(servant)) {
         if (servant.getRandom().nextFloat() > 0.4F) {
            return;
         }
         playVoice(servant, "attack", ATTACK_VOICE_COOLDOWN, 1.05F, 1.0F, ModSounds.CU_CHULAINN_VOICE_ATTACK.get());
      }
   }

   public static void tryPlayRoar(ServantEntity servant) {
      if (!isHeracles(servant)) {
         return;
      }

      playVoice(servant, "roar", ROAR_VOICE_COOLDOWN, 1.2F, 0.95F, ModSounds.HERACLES_VOICE_ROAR.get());
   }

   public static void tryPlayTsurigameshi(ServantEntity servant) {
      if (!isSasakiKojiro(servant)) {
         return;
      }

      playVoice(servant, "tsurigameshi", SPECIAL_VOICE_COOLDOWN, 1.05F, 1.0F, ModSounds.SASAKI_KOJIRO_VOICE_TSURIGAMESHI.get());
   }

   public static void tryPlayVictory(ServantEntity servant, LivingEntity defeated) {
      if (defeated == null || defeated == servant || defeated.isAlliedTo(servant)) {
         return;
      }

      if (isSasakiKojiro(servant)) {
         playVoice(servant, "victory", VICTORY_VOICE_COOLDOWN, 1.0F, 1.0F, ModSounds.SASAKI_KOJIRO_VOICE_VICTORY.get());
      } else if (isHeracles(servant)) {
         playVoice(servant, "victory", VICTORY_VOICE_COOLDOWN, 1.15F, 0.92F, ModSounds.HERACLES_VOICE_VICTORY.get());
      } else if (isCuChulainn(servant)) {
         playVoice(servant, "victory", VICTORY_VOICE_COOLDOWN, 1.05F, 1.0F, ModSounds.CU_CHULAINN_VOICE_VICTORY.get());
      }
   }

   public static void tryPlayFail(ServantEntity servant) {
      if (isSasakiKojiro(servant)) {
         playVoice(servant, "fail", FAIL_VOICE_COOLDOWN, 1.0F, 0.96F, ModSounds.SASAKI_KOJIRO_VOICE_FAIL.get());
      } else if (isHeracles(servant)) {
         playVoice(servant, "fail", FAIL_VOICE_COOLDOWN, 1.15F, 0.9F, ModSounds.HERACLES_VOICE_FAIL.get());
      } else if (isCuChulainn(servant)) {
         playVoice(servant, "fail", FAIL_VOICE_COOLDOWN, 1.0F, 0.95F, ModSounds.CU_CHULAINN_VOICE_FAIL.get());
      }
   }

   public static void tryPlayGaeBolg(ServantEntity servant) {
      if (!isCuChulainn(servant)) {
         return;
      }

      playVoice(servant, "gae_bolg", SPECIAL_VOICE_COOLDOWN, 1.1F, 1.0F, ModSounds.CU_CHULAINN_VOICE_GAE_BOLG.get());
   }

   private static void playVoice(ServantEntity servant, String category, int cooldownTicks, float volume, float pitch, SoundEvent sound) {
      if (!(servant.level() instanceof ServerLevel serverLevel) || sound == null) {
         return;
      }

      CompoundTag data = servant.getPersistentData();
      long now = serverLevel.getGameTime();
      if (now - data.getLong(GLOBAL_VOICE_TICK_TAG) < GLOBAL_VOICE_COOLDOWN) {
         return;
      }

      String categoryTag = CATEGORY_VOICE_TICK_PREFIX + category;
      if (now - data.getLong(categoryTag) < cooldownTicks) {
         return;
      }

      data.putLong(GLOBAL_VOICE_TICK_TAG, now);
      data.putLong(categoryTag, now);
      float finalPitch = pitch + (servant.getRandom().nextFloat() - 0.5F) * 0.08F;
      serverLevel.playSound(null, servant.getX(), servant.getY(), servant.getZ(), sound, SoundSource.HOSTILE, volume, finalPitch);
   }

   private static boolean isSasakiKojiro(ServantEntity servant) {
      return servant != null && SasakiKojiroEntity.SERVANT_KEY.equals(servant.getServantId());
   }

   private static boolean isHeracles(ServantEntity servant) {
      return servant != null && HeraclesEntity.SERVANT_KEY.equals(servant.getServantId());
   }

   private static boolean isCuChulainn(ServantEntity servant) {
      return servant != null && CuChulainnEntity.SERVANT_KEY.equals(servant.getServantId());
   }
}
