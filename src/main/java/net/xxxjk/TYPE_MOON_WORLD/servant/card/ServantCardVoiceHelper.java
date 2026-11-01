package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class ServantCardVoiceHelper {
   private static final String LAST_VOICE_TICK = "ServantCardPlayerVoiceLastTick";
   private static final String OKITA_COMBAT_ACTIVITY = "ServantCardOkitaLastCombatVoiceActivity";

   private ServantCardVoiceHelper() {
   }

   public static void tryPlayAttack(ServerPlayer player) {
      if(net.xxxjk.TYPE_MOON_WORLD.servant.cu.CuChulainnAbilityService.isOwner(player)) {
         net.xxxjk.TYPE_MOON_WORLD.servant.cu.CuChulainnVoiceService.attack(player);return;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (isOkita(vars) && tryPlayOkitaBattleStart(player, vars)) return;
      if (!vars.servant_card_transformed || player.getRandom().nextFloat() > 0.42F) {
         return;
      }
      play(player, switch (vars.servant_card_id) {
         case "heracles" -> ModSounds.HERACLES_VOICE_ROAR.get();
         case "sasaki_kojiro" -> ModSounds.SASAKI_KOJIRO_VOICE_ATTACK.get();
         case "cu_chulainn" -> ModSounds.CU_CHULAINN_VOICE_ATTACK.get();
         case "medea" -> ModSounds.MEDEA_VOICE_ATTACK.get();
         case "medusa" -> ModSounds.MEDUSA_VOICE_ATTACK.get();
         case "cursed_arm_hassan" -> ModSounds.CURSED_ARM_HASSAN_VOICE_ATTACK.get();
         case "shadow_hassan" -> ModSounds.SHADOW_HASSAN_VOICE_ATTACK.get();
         case "fanatic_assassin" -> ModSounds.FANATIC_ASSASSIN_VOICE_ATTACK.get();
         case "emiya_archer" -> ModSounds.EMIYA_ARCHER_VOICE_ATTACK.get();
         case "artoria_pendragon" -> ModSounds.ARTORIA_VOICE_ATTACK.get();
         case "oda_nobunaga" -> ModSounds.ODA_NOBUNAGA_VOICE_ATTACK.get();
         case "okita_souji_saber" -> ModSounds.OKITA_SOUJI_SABER_VOICE_ATTACK.get();
         case "enkidu" -> ModSounds.ENKIDU_VOICE_ATTACK.get();
         case "gilgamesh" -> ModSounds.GILGAMESH_VOICE_ATTACK.get();
         case "gawain" -> ModSounds.GAWAIN_VOICE_ATTACK.get();
         case "li_shuwen" -> ModSounds.LI_SHUWEN_VOICE_ATTACK.get();
         case "paracelsus" -> ModSounds.PARACELSUS_VOICE_ATTACK.get();
         case "ushiwakamaru_rider" -> ModSounds.USHIWAKAMARU_RIDER_VOICE_ATTACK.get();
         case "arash" -> ModSounds.ARASH_VOICE_ATTACK.get();
         case "nightingale" -> ModSounds.NIGHTINGALE_VOICE_ATTACK.get();
         case "senko_muramasa" -> ModSounds.SENKO_MURAMASA_VOICE_ATTACK.get();
         case "diarmuid_ua_duibhne" -> ModSounds.DIARMUID_UA_DUIBHNE_VOICE_ATTACK.get();
         case "lancelot_berserker" -> ModSounds.LANCELOT_BERSERKER_VOICE_ATTACK.get();
         case "baobhan_sith" -> ModSounds.BAOBHAN_SITH_VOICE_ATTACK.get();
         case "jeanne_alter" -> ModSounds.JEANNE_ALTER_VOICE_ATTACK.get();
         default -> null;
      }, 70);
   }

   public static void tryPlaySkill(ServerPlayer player, String effectId) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || effectId == null) {
         return;
      }
      if (isOkita(vars) && !"okita_mumyoudan_zuki".equals(effectId)
         && !"okita_flag_of_sincerity".equals(effectId)) tryPlayOkitaBattleStart(player, vars);
      SoundEvent sound = usesUshiwakamaruNoblePhantasmVoice(effectId)
         ? ModSounds.USHIWAKAMARU_RIDER_VOICE_NP.get()
         : switch (effectId) {
         case "god_hand_status", "heracles_big_jump", "heracles_ground_slam", "heracles_shoulder",
            "heracles_uppercut", "heracles_sweep", "heracles_throw", "heracles_stomp", "heracles_flurry", "heracles_pounce"
            -> ModSounds.HERACLES_VOICE_ROAR.get();
         case "sasaki_crouch_cutback", "sasaki_vertical_slash", "sasaki_downward_slash", "sasaki_upward_slash",
            "sasaki_horizontal_slash", "sasaki_thrust", "sasaki_spinning_slash", "sasaki_counter_stance"
            -> ModSounds.SASAKI_KOJIRO_VOICE_ATTACK.get();
         case "zabaniya" -> ModSounds.CURSED_ARM_HASSAN_VOICE_ZABANIYA_SHORT.get();
         case "shadow_hassan_meditative_sensitivity" -> ModSounds.SHADOW_HASSAN_VOICE_MEDITATIVE_SENSITIVITY.get();
         case "fanatic_heartbeat" -> ModSounds.FANATIC_ASSASSIN_VOICE_HEARTBEAT.get();
         case "fanatic_marrow" -> ModSounds.FANATIC_ASSASSIN_VOICE_MARROW.get();
         case "fanatic_hair" -> ModSounds.FANATIC_ASSASSIN_VOICE_HAIR.get();
         case "fanatic_temperature", "fanatic_computer" -> ModSounds.FANATIC_ASSASSIN_VOICE_COMPUTER_TEMPERATURE.get();
         case "fanatic_nerves" -> ModSounds.FANATIC_ASSASSIN_VOICE_NERVES.get();
         case "bellerophon" -> ModSounds.MEDUSA_VOICE_BELLEROPHON.get();
         case "rho_aias" -> ModSounds.EMIYA_ARCHER_VOICE_RHO_AIAS.get();
         case "emiya_spiral" -> ModSounds.EMIYA_ARCHER_VOICE_SPIRAL.get();
         case "copy_weapon", "emiya_kb", "emiya_hound", "emiya_layered_projection" -> ModSounds.EMIYA_ARCHER_VOICE_PROJECTION.get();
         case "artoria_invisible_air_toggle", "artoria_wind_defense",
            "invisible_air_hammer", "invisible_air_release" -> ModSounds.ARTORIA_VOICE_INVISIBLE_AIR.get();
         case "mana_burst", "artoria_small_combo", "artoria_knight_triple_slash",
            "artoria_intercept_thrust", "artoria_spinning_slash", "artoria_upward_slash",
            "artoria_guard_counter", "artoria_knight_heavy_slash" -> ModSounds.ARTORIA_VOICE_ATTACK.get();
         case "enuma_elish" -> ModSounds.ENKIDU_VOICE_NP.get();
         case "gilgamesh_cross_slash" -> ModSounds.GILGAMESH_VOICE_ATTACK.get();
         case "caster_gilgamesh_cannon_calibration", "caster_gilgamesh_royal_cannon" -> ModSounds.CASTER_GILGAMESH_VOICE_NP.get();
         case "caster_gilgamesh_leader", "caster_gilgamesh_return", "caster_gilgamesh_workshop", "caster_gilgamesh_item_creation" -> ModSounds.CASTER_GILGAMESH_VOICE_SHOT.get();
         case "gallatin_spark" -> ModSounds.GAWAIN_VOICE_GALLATIN_SHORT.get();
         case "flame_tornado" -> ModSounds.GAWAIN_VOICE_FIRE_ATTACK.get();
         case "wu_er_da" -> ModSounds.LI_SHUWEN_VOICE_WU_ER_DA_SHORT.get();
         case "rule_breaker" -> ModSounds.MEDEA_VOICE_RULE_BREAKER_SHORT.get();
         case "paracelsus_sword_np" -> ModSounds.PARACELSUS_VOICE_NP.get();
         case "zhao_yun_changbanpo" -> ModSounds.ZHAO_YUN_VOICE_NP.get();
         case "oda_hasebe_short_thrust", "oda_floating_refill", "oda_three_line",
            "oda_encircle_matchlocks", "oda_charged_matchlock", "oda_crossfire_net",
            "oda_scorched_earth", "oda_hasebe_breakthrough", "maou" -> ModSounds.ODA_NOBUNAGA_VOICE_SKILL.get();
         case "okita_mumyoudan_zuki" -> ModSounds.OKITA_VOICE_NP_SHORT.get();
         case "okita_flag_of_sincerity" -> ModSounds.OKITA_SOUJI_SABER_VOICE_FLAG.get();
         case "okita_ichimonji", "okita_kaifuu", "okita_thrust", "okita_downward_slash", "okita_stance_break"
            -> ModSounds.OKITA_SOUJI_SABER_VOICE_ATTACK.get();
         case "arash_arrow_rain", "arash_energy_small", "arash_energy_large" -> ModSounds.ARASH_VOICE_ATTACK.get();
         case "paracelsus_craft_stone", "paracelsus_spirit_toggle", "paracelsus_workshop_teleport" -> ModSounds.PARACELSUS_VOICE_SPELL.get();
         case "muramasa_no_gen_kensai" -> ModSounds.SENKO_MURAMASA_VOICE_NP.get();
         case "muramasa_tsumukari" -> ModSounds.SENKO_MURAMASA_VOICE_TSUMUKARI.get();
         case "muramasa_forge", "muramasa_workshop", "muramasa_trial_slash", "muramasa_karma_eye",
            "muramasa_flame", "muramasa_projection_volley", "muramasa_temper", "muramasa_karma_slash",
            "muramasa_sword_field" -> ModSounds.SENKO_MURAMASA_VOICE_ATTACK.get();
         case "diarmuid_twin_spear_combo", "diarmuid_red_rose_focus", "diarmuid_yellow_rose_focus",
            "diarmuid_knight_strategy", "diarmuid_mana_burst_jump", "diarmuid_flower_step", "diarmuid_disengage"
            -> ModSounds.DIARMUID_UA_DUIBHNE_VOICE_ATTACK.get();
         case "lancelot_aroundight" -> ModSounds.LANCELOT_BERSERKER_VOICE_NP.get();
         case "lancelot_berserk_roar" -> ModSounds.LANCELOT_BERSERKER_VOICE_ROAR.get();
         case "lancelot_maul_combo", "lancelot_feral_rush", "lancelot_ground_slam", "lancelot_hunt_step",
            "lancelot_mana_reversal", "lancelot_knight_of_owner" -> ModSounds.LANCELOT_BERSERKER_VOICE_ATTACK.get();
         case "baobhan_sith_fetch_failnaught" -> ModSounds.BAOBHAN_SITH_VOICE_NP.get();
         case "baobhan_sith_blood_spike", "baobhan_sith_blood_thorns", "baobhan_sith_curse_volley",
            "baobhan_sith_fingertip_dance", "baobhan_sith_night_feast", "baobhan_sith_grimalkin",
            "baobhan_sith_blessed_successor", "baobhan_sith_fairy_vampirism" -> ModSounds.BAOBHAN_SITH_VOICE_ATTACK.get();
         case "jeanne_alter_le_grondement_du_haine" -> ModSounds.JEANNE_ALTER_VOICE_NP.get();
         case "jeanne_alter_fire_pillar", "jeanne_alter_cursed_lance", "jeanne_alter_self_modification_ex",
            "jeanne_alter_dragon_witch_ex", "jeanne_alter_inferno_body", "jeanne_alter_ephemeral_dream_a"
            -> ModSounds.JEANNE_ALTER_VOICE_SKILL.get();
         default -> null;
      };
      if (sound == ModSounds.ODA_NOBUNAGA_VOICE_SKILL.get()) {
         if (player.getRandom().nextFloat() > 0.4F) return;
         play(player, sound, 200);
      } else {
         play(player, sound, 40);
      }
   }

   public static void tryPlayVictory(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (isOkita(vars)) play(player, ModSounds.OKITA_SOUJI_SABER_VOICE_VICTORY.get(), 160);
   }

   public static void tryPlayFail(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (isOkita(vars)) play(player, ModSounds.OKITA_SOUJI_SABER_VOICE_FAIL.get(), 80);
   }

   private static boolean tryPlayOkitaBattleStart(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      long now = player.level().getGameTime();
      var data = player.getPersistentData();
      long last = data.getLong(OKITA_COMBAT_ACTIVITY);
      boolean newCombat = !data.contains(OKITA_COMBAT_ACTIVITY) || now - last >= 200L;
      data.putLong(OKITA_COMBAT_ACTIVITY, now);
      return newCombat && play(player, ModSounds.OKITA_SOUJI_SABER_VOICE_BATTLE_START.get(), 40);
   }

   private static boolean isOkita(TypeMoonWorldModVariables.PlayerVariables vars) {
      return vars.servant_card_transformed && "okita_souji_saber".equals(vars.servant_card_id);
   }

   static boolean usesUshiwakamaruNoblePhantasmVoice(String effectId) {
      return "ushiwakamaru_eight_boat".equals(effectId);
   }

   private static boolean play(ServerPlayer player, SoundEvent sound, int cooldownTicks) {
      if (sound == null || !(player.level() instanceof ServerLevel level)) {
         return false;
      }
      if (net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.suppressLegacyVoice(player, sound)) return false;
      boolean diarmuid = ServantCardDiarmuidSkills.isActiveCard(player);
      if (diarmuid && net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidVoiceService.locked(player)) return false;
      long now = level.getGameTime();
      long last = player.getPersistentData().getLong(LAST_VOICE_TICK);
      if (player.getPersistentData().contains(LAST_VOICE_TICK) && now - last < cooldownTicks) {
         return false;
      }
      player.getPersistentData().putLong(LAST_VOICE_TICK, now);
      if (diarmuid) net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidVoiceService.lock(player, 22);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.VOICE, 1.0F, 1.0F);
      return true;
   }
}
