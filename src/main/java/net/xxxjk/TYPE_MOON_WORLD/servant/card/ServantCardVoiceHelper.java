package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class ServantCardVoiceHelper {
   private static final String LAST_VOICE_TICK = "ServantCardPlayerVoiceLastTick";

   private ServantCardVoiceHelper() {
   }

   public static void tryPlayAttack(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || player.getRandom().nextFloat() > 0.42F) {
         return;
      }
      play(player, switch (vars.servant_card_id) {
         case "sasaki_kojiro" -> ModSounds.SASAKI_KOJIRO_VOICE_ATTACK.get();
         case "cu_chulainn" -> ModSounds.CU_CHULAINN_VOICE_ATTACK.get();
         case "medea" -> ModSounds.MEDEA_VOICE_ATTACK.get();
         case "medusa" -> ModSounds.MEDUSA_VOICE_ATTACK.get();
         case "cursed_arm_hassan" -> ModSounds.CURSED_ARM_HASSAN_VOICE_ATTACK.get();
         case "emiya_archer" -> ModSounds.EMIYA_ARCHER_VOICE_ATTACK.get();
         case "artoria_pendragon" -> ModSounds.ARTORIA_VOICE_ATTACK.get();
         case "oda_nobunaga" -> ModSounds.ODA_NOBUNAGA_VOICE_ATTACK.get();
         case "enkidu" -> ModSounds.ENKIDU_VOICE_ATTACK.get();
         case "gawain" -> ModSounds.GAWAIN_VOICE_ATTACK.get();
         case "li_shuwen" -> ModSounds.LI_SHUWEN_VOICE_ATTACK.get();
         case "paracelsus" -> ModSounds.PARACELSUS_VOICE_ATTACK.get();
         default -> null;
      }, 70);
   }

   public static void tryPlaySkill(ServerPlayer player, String effectId) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || effectId == null) {
         return;
      }
      SoundEvent sound = switch (effectId) {
         case "zabaniya" -> ModSounds.CURSED_ARM_HASSAN_VOICE_ZABANIYA_SHORT.get();
         case "bellerophon" -> ModSounds.MEDUSA_VOICE_BELLEROPHON.get();
         case "rho_aias" -> ModSounds.EMIYA_ARCHER_VOICE_RHO_AIAS.get();
         case "emiya_spiral" -> ModSounds.EMIYA_ARCHER_VOICE_SPIRAL.get();
         case "copy_weapon", "emiya_kb", "emiya_hound", "emiya_layered_projection" -> ModSounds.EMIYA_ARCHER_VOICE_PROJECTION.get();
         case "invisible_air_hammer", "invisible_air_release" -> ModSounds.ARTORIA_VOICE_INVISIBLE_AIR.get();
         case "enuma_elish" -> ModSounds.ENKIDU_VOICE_NP.get();
         case "gallatin_spark" -> ModSounds.GAWAIN_VOICE_GALLATIN_SHORT.get();
         case "flame_tornado" -> ModSounds.GAWAIN_VOICE_FIRE_ATTACK.get();
         case "wu_er_da" -> ModSounds.LI_SHUWEN_VOICE_WU_ER_DA_SHORT.get();
         case "rule_breaker" -> ModSounds.MEDEA_VOICE_RULE_BREAKER_SHORT.get();
         case "paracelsus_sword_np" -> ModSounds.PARACELSUS_VOICE_NP.get();
         case "paracelsus_craft_stone", "paracelsus_spirit_toggle", "paracelsus_workshop_teleport" -> ModSounds.PARACELSUS_VOICE_SPELL.get();
         default -> null;
      };
      play(player, sound, 40);
   }

   private static void play(ServerPlayer player, SoundEvent sound, int cooldownTicks) {
      if (sound == null || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      long now = level.getGameTime();
      long last = player.getPersistentData().getLong(LAST_VOICE_TICK);
      if (now - last < cooldownTicks) {
         return;
      }
      player.getPersistentData().putLong(LAST_VOICE_TICK, now);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.VOICE, 1.0F, 1.0F);
   }
}
