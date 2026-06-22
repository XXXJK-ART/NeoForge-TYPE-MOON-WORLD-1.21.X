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
      } else if (isMedea(servant)) {
         if (servant.getRandom().nextFloat() > 0.4F) {
            return;
         }
         playVoice(servant, "attack", ATTACK_VOICE_COOLDOWN, 1.0F, 1.0F, ModSounds.MEDEA_VOICE_ATTACK.get());
      } else if (isMedusa(servant)) {
         if (servant.getRandom().nextFloat() > 0.4F) {
            return;
         }
         playVoice(servant, "attack", ATTACK_VOICE_COOLDOWN, 1.0F, 0.98F, ModSounds.MEDUSA_VOICE_ATTACK.get());
      } else if (isCursedArmHassan(servant)) {
         if (servant.getRandom().nextFloat() > 0.45F) {
            return;
         }
         playVoice(servant, "attack", ATTACK_VOICE_COOLDOWN, 1.0F, 0.95F, ModSounds.CURSED_ARM_HASSAN_VOICE_ATTACK.get());
      } else if (isEmiya(servant)) {
         if (servant.getRandom().nextFloat() > 0.45F) {
            return;
         }
         playVoice(servant, "attack", ATTACK_VOICE_COOLDOWN, 1.0F, 1.0F, ModSounds.EMIYA_ARCHER_VOICE_ATTACK.get());
      } else if (isArtoria(servant)) {
         if (servant.getRandom().nextFloat() > 0.45F) {
            return;
         }
         playVoice(servant, "attack", ATTACK_VOICE_COOLDOWN, 1.0F, 1.0F, ModSounds.ARTORIA_VOICE_ATTACK.get());
      } else if (isOdaNobunaga(servant)) {
         if (servant.getRandom().nextFloat() > 0.45F) {
            return;
         }
         playVoice(servant, "attack", ATTACK_VOICE_COOLDOWN, 1.0F, 1.0F, ModSounds.ODA_NOBUNAGA_VOICE_ATTACK.get());
      } else if (isEnkidu(servant)) {
         if (servant.getRandom().nextFloat() > 0.45F) {
            return;
         }
         playVoice(servant, "attack", ATTACK_VOICE_COOLDOWN, 1.0F, 1.05F, ModSounds.ENKIDU_VOICE_ATTACK.get());
      } else if (isGawain(servant)) {
         if (servant.getRandom().nextFloat() > 0.45F) {
            return;
         }
         playVoice(servant, "attack", ATTACK_VOICE_COOLDOWN, 1.05F, 1.0F, ModSounds.GAWAIN_VOICE_ATTACK.get());
      } else if (isLiShuwen(servant)) {
         if (servant.getRandom().nextFloat() > 0.45F) {
            return;
         }
         playVoice(servant, "attack", ATTACK_VOICE_COOLDOWN, 1.0F, 0.98F, ModSounds.LI_SHUWEN_VOICE_ATTACK.get());
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
      } else if (isMedea(servant)) {
         playVoice(servant, "victory", VICTORY_VOICE_COOLDOWN, 1.0F, 1.0F, ModSounds.MEDEA_VOICE_VICTORY.get());
      } else if (isMedusa(servant)) {
         playVoice(servant, "victory", VICTORY_VOICE_COOLDOWN, 1.0F, 0.98F, ModSounds.MEDUSA_VOICE_VICTORY.get());
      } else if (isCursedArmHassan(servant)) {
         playVoice(servant, "victory", VICTORY_VOICE_COOLDOWN, 1.0F, 0.95F, ModSounds.CURSED_ARM_HASSAN_VOICE_VICTORY.get());
      } else if (isEmiya(servant)) {
         playVoice(servant, "victory", VICTORY_VOICE_COOLDOWN, 1.0F, 1.0F, ModSounds.EMIYA_ARCHER_VOICE_VICTORY.get());
      } else if (isArtoria(servant)) {
         playVoice(servant, "victory", VICTORY_VOICE_COOLDOWN, 1.0F, 1.0F, ModSounds.ARTORIA_VOICE_VICTORY.get());
      } else if (isOdaNobunaga(servant)) {
         playVoice(servant, "victory", VICTORY_VOICE_COOLDOWN, 1.0F, 1.0F, ModSounds.ODA_NOBUNAGA_VOICE_VICTORY.get());
      } else if (isEnkidu(servant)) {
         playVoice(servant, "victory", VICTORY_VOICE_COOLDOWN, 1.0F, 1.05F, ModSounds.ENKIDU_VOICE_VICTORY.get());
      } else if (isGawain(servant)) {
         playVoice(servant, "victory", VICTORY_VOICE_COOLDOWN, 1.05F, 1.0F, ModSounds.GAWAIN_VOICE_VICTORY.get());
      } else if (isLiShuwen(servant)) {
         playVoice(servant, "victory", VICTORY_VOICE_COOLDOWN, 1.0F, 0.96F, ModSounds.LI_SHUWEN_VOICE_VICTORY.get());
      }
   }

   public static void tryPlayFail(ServantEntity servant) {
      if (isSasakiKojiro(servant)) {
         playVoice(servant, "fail", FAIL_VOICE_COOLDOWN, 1.0F, 0.96F, ModSounds.SASAKI_KOJIRO_VOICE_FAIL.get());
      } else if (isHeracles(servant)) {
         playVoice(servant, "fail", FAIL_VOICE_COOLDOWN, 1.15F, 0.9F, ModSounds.HERACLES_VOICE_FAIL.get());
      } else if (isCuChulainn(servant)) {
         playVoice(servant, "fail", FAIL_VOICE_COOLDOWN, 1.0F, 0.95F, ModSounds.CU_CHULAINN_VOICE_FAIL.get());
      } else if (isMedea(servant)) {
         playVoice(servant, "fail", FAIL_VOICE_COOLDOWN, 1.0F, 0.95F, ModSounds.MEDEA_VOICE_FAIL.get());
      } else if (isMedusa(servant)) {
         playVoice(servant, "fail", FAIL_VOICE_COOLDOWN, 1.0F, 0.95F, ModSounds.MEDUSA_VOICE_FAIL.get());
      } else if (isCursedArmHassan(servant)) {
         playVoice(servant, "fail", FAIL_VOICE_COOLDOWN, 1.0F, 0.92F, ModSounds.CURSED_ARM_HASSAN_VOICE_FAIL.get());
      } else if (isEmiya(servant)) {
         playVoice(servant, "fail", FAIL_VOICE_COOLDOWN, 1.0F, 1.0F, ModSounds.EMIYA_ARCHER_VOICE_FAIL.get());
      } else if (isArtoria(servant)) {
         playVoice(servant, "fail", FAIL_VOICE_COOLDOWN, 1.0F, 1.0F, ModSounds.ARTORIA_VOICE_FAIL.get());
      } else if (isOdaNobunaga(servant)) {
         playVoice(servant, "fail", FAIL_VOICE_COOLDOWN, 1.0F, 1.0F, ModSounds.ODA_NOBUNAGA_VOICE_FAIL.get());
      } else if (isEnkidu(servant)) {
         playVoice(servant, "fail", FAIL_VOICE_COOLDOWN, 1.0F, 1.0F, ModSounds.ENKIDU_VOICE_FAIL.get());
      } else if (isGawain(servant)) {
         playVoice(servant, "fail", FAIL_VOICE_COOLDOWN, 1.05F, 0.98F, ModSounds.GAWAIN_VOICE_FAIL.get());
      } else if (isLiShuwen(servant)) {
         playVoice(servant, "fail", FAIL_VOICE_COOLDOWN, 1.0F, 0.94F, ModSounds.LI_SHUWEN_VOICE_FAIL.get());
      }
   }

   public static void tryPlayGaeBolg(ServantEntity servant) {
      if (!isCuChulainn(servant)) {
         return;
      }

      playVoice(servant, "gae_bolg", SPECIAL_VOICE_COOLDOWN, 1.1F, 1.0F, ModSounds.CU_CHULAINN_VOICE_GAE_BOLG.get());
   }

   public static void tryPlaySpell(ServantEntity servant) {
      if (!isMedea(servant)) {
         return;
      }

      playVoice(servant, "spell", SPECIAL_VOICE_COOLDOWN, 1.0F, 1.0F, ModSounds.MEDEA_VOICE_SPELL.get());
   }

   public static void tryPlayRuleBreaker(ServantEntity servant) {
      if (!isMedea(servant)) {
         return;
      }

      playVoice(servant, "rule_breaker", SPECIAL_VOICE_COOLDOWN, 1.0F, 1.0F, ModSounds.MEDEA_VOICE_RULE_BREAKER.get());
   }

   public static void tryPlayBellerophon(ServantEntity servant) {
      if (!isMedusa(servant)) {
         return;
      }

      playVoice(servant, "bellerophon", SPECIAL_VOICE_COOLDOWN, 1.0F, 0.98F, ModSounds.MEDUSA_VOICE_BELLEROPHON.get());
   }

   public static void tryPlayZabaniya(ServantEntity servant) {
      if (!isCursedArmHassan(servant)) {
         return;
      }

      playVoice(servant, "zabaniya", SPECIAL_VOICE_COOLDOWN, 1.0F, 0.95F, ModSounds.CURSED_ARM_HASSAN_VOICE_ZABANIYA.get());
   }

   public static void tryPlayProjection(ServantEntity servant) {
      if (!isEmiya(servant)) {
         return;
      }

      playVoice(servant, "projection", SPECIAL_VOICE_COOLDOWN, 1.0F, 1.0F, ModSounds.EMIYA_ARCHER_VOICE_PROJECTION.get());
   }

   public static void tryPlayEmiyaTwinThrow(ServantEntity servant) {
      if (!isEmiya(servant)) {
         return;
      }

      playVoice(servant, "twin_throw", SPECIAL_VOICE_COOLDOWN, 1.0F, 1.0F, ModSounds.EMIYA_ARCHER_VOICE_TWIN_THROW.get());
   }

   public static void tryPlayEmiyaSpiral(ServantEntity servant) {
      if (!isEmiya(servant)) {
         return;
      }

      playVoice(servant, "spiral", SPECIAL_VOICE_COOLDOWN, 1.0F, 1.0F, ModSounds.EMIYA_ARCHER_VOICE_SPIRAL.get());
   }

   public static void tryPlayEmiyaRhoAias(ServantEntity servant) {
      if (!isEmiya(servant)) {
         return;
      }

      playVoice(servant, "rho_aias", SPECIAL_VOICE_COOLDOWN, 1.0F, 1.0F, ModSounds.EMIYA_ARCHER_VOICE_RHO_AIAS.get());
   }

   public static void tryPlayEmiyaUbw(ServantEntity servant) {
      if (!isEmiya(servant)) {
         return;
      }

      playVoiceForced(servant, "ubw", 1.1F, 1.0F, ModSounds.EMIYA_ARCHER_VOICE_UBW.get());
   }

   public static void tryPlayArtoriaInvisibleAir(ServantEntity servant) {
      if (!isArtoria(servant)) {
         return;
      }

      playVoice(servant, "invisible_air", SPECIAL_VOICE_COOLDOWN, 1.05F, 1.0F, ModSounds.ARTORIA_VOICE_INVISIBLE_AIR.get());
   }

   public static void tryPlayArtoriaExcaliburRelease(ServantEntity servant) {
      if (!isArtoria(servant)) {
         return;
      }

      playVoice(servant, "excalibur_release", SPECIAL_VOICE_COOLDOWN, 1.1F, 1.0F, ModSounds.ARTORIA_VOICE_EXCALIBUR_RELEASE.get());
   }

   public static void tryPlayArtoriaExcalibur(ServantEntity servant) {
      if (!isArtoria(servant)) {
         return;
      }

      playVoiceForced(servant, "excalibur", 1.2F, 1.0F, ModSounds.ARTORIA_VOICE_EXCALIBUR.get());
   }

   public static void tryPlayOdaNobunagaNp(ServantEntity servant) {
      if (!isOdaNobunaga(servant)) {
         return;
      }

      playVoiceForced(servant, "oda_np", 1.1F, 1.0F, ModSounds.ODA_NOBUNAGA_VOICE_NP.get());
   }

   public static void tryPlayOdaNobunagaHajun(ServantEntity servant) {
      if (!isOdaNobunaga(servant)) {
         return;
      }

      playVoiceForced(servant, "oda_hajun", 1.2F, 0.95F, ModSounds.ODA_NOBUNAGA_VOICE_HAJUN.get());
   }

   public static void tryPlayEnkiduNp(ServantEntity servant) {
      if (!isEnkidu(servant)) {
         return;
      }

      playVoiceForced(servant, "enkidu_np", 1.2F, 1.0F, ModSounds.ENKIDU_VOICE_NP.get());
   }

   public static void tryPlayGawainFireAttack(ServantEntity servant) {
      if (!isGawain(servant)) {
         return;
      }

      playVoice(servant, "gawain_fire_attack", SPECIAL_VOICE_COOLDOWN, 1.05F, 1.0F, ModSounds.GAWAIN_VOICE_FIRE_ATTACK.get());
   }

   public static void tryPlayGawainNp(ServantEntity servant) {
      if (!isGawain(servant)) {
         return;
      }

      playVoiceForced(servant, "gawain_np", 1.25F, 1.0F, ModSounds.GAWAIN_VOICE_NP.get());
   }

   public static void tryPlayLiShuwenNp(ServantEntity servant) {
      if (!isLiShuwen(servant)) {
         return;
      }

      playVoiceForced(servant, "li_shuwen_np", 1.05F, 0.96F, ModSounds.LI_SHUWEN_VOICE_NP.get());
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

   private static void playVoiceForced(ServantEntity servant, String category, float volume, float pitch, SoundEvent sound) {
      if (!(servant.level() instanceof ServerLevel serverLevel) || sound == null) {
         return;
      }

      CompoundTag data = servant.getPersistentData();
      long now = serverLevel.getGameTime();
      data.putLong(GLOBAL_VOICE_TICK_TAG, now);
      data.putLong(CATEGORY_VOICE_TICK_PREFIX + category, now);
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

   private static boolean isMedea(ServantEntity servant) {
      return servant != null && MedeaEntity.SERVANT_KEY.equals(servant.getServantId());
   }

   private static boolean isMedusa(ServantEntity servant) {
      return servant != null && MedusaEntity.SERVANT_KEY.equals(servant.getServantId());
   }

   private static boolean isCursedArmHassan(ServantEntity servant) {
      return servant != null && CursedArmHassanEntity.SERVANT_KEY.equals(servant.getServantId());
   }

   private static boolean isEmiya(ServantEntity servant) {
      return servant != null && EmiyaArcherEntity.SERVANT_KEY.equals(servant.getServantId());
   }

   private static boolean isArtoria(ServantEntity servant) {
      return servant != null && ArtoriaPendragonEntity.SERVANT_KEY.equals(servant.getServantId());
   }

   private static boolean isOdaNobunaga(ServantEntity servant) {
      return servant != null && OdaNobunagaEntity.SERVANT_KEY.equals(servant.getServantId());
   }

   private static boolean isEnkidu(ServantEntity servant) {
      return servant != null && EnkiduEntity.SERVANT_KEY.equals(servant.getServantId());
   }

   private static boolean isGawain(ServantEntity servant) {
      return servant != null && GawainEntity.SERVANT_KEY.equals(servant.getServantId());
   }

   private static boolean isLiShuwen(ServantEntity servant) {
      return servant != null && LiShuwenEntity.SERVANT_KEY.equals(servant.getServantId());
   }
}
