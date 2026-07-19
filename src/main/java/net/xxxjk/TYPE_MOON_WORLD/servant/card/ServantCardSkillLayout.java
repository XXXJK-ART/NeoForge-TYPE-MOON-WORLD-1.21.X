package net.xxxjk.TYPE_MOON_WORLD.servant.card;

public final class ServantCardSkillLayout {
   private ServantCardSkillLayout() {
   }

   public static ServantCardSkillAction actionFor(String servantId, int slot, boolean crouching) {
      return switch (servantId == null ? "" : servantId) {
         case "artoria_pendragon" -> switch (slot) {
            case -1 -> new ServantCardSkillAction("Knight Combo", "artoria_small_combo", 16.0, 30);
            case 0 -> new ServantCardSkillAction("Mana Burst", "mana_burst", 20.0, 1400);
            case 1 -> new ServantCardSkillAction("Strike Air: Hammer of the Wind King", "invisible_air_hammer", 60.0, 220);
            case 2 -> new ServantCardSkillAction("Charisma", "charisma", 30.0, 360);
            case 3 -> new ServantCardSkillAction("Invisible Air Release", "invisible_air_release", 28.0, 100);
            case 4 -> new ServantCardSkillAction("Instinct A", "artoria_instinct", 18.0, 240);
            case 5 -> new ServantCardSkillAction("Riding B", "artoria_riding", 20.0, 160);
            default -> null;
         };
         case "cu_chulainn" -> switch (slot) {
            case -1 -> new ServantCardSkillAction("Spear Thrust", "cu_crouch_thrust", 0.0, 18);
            case 0 -> new ServantCardSkillAction("Ansuz Rune", "cu_ansuz", 22.0, 120);
            case 1 -> new ServantCardSkillAction("Laguz Rune", "cu_laguz", 20.0, 160);
            case 2 -> new ServantCardSkillAction("Tiwaz Rune", "cu_tiwaz", 24.0, 260);
            case 3 -> new ServantCardSkillAction("Algiz Rune", "cu_algiz", 30.0, 300);
            case 4 -> new ServantCardSkillAction("Berkana Rune", "cu_berkana", 34.0, 400);
            case 5 -> new ServantCardSkillAction("Disengage", "cu_disengage", 18.0, 400);
            default -> null;
         };
         case "heracles" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("Great Leap", "heracles_big_jump", 18.0, 120);
            case 1 -> new ServantCardSkillAction("Ground Slam", "heracles_ground_slam", 32.0, 180);
            case 2 -> new ServantCardSkillAction("Roar", "heracles_roar", 18.0, 180);
            case 3 -> new ServantCardSkillAction("Valor A+", "heracles_valor", 20.0, 300);
            case 4 -> new ServantCardSkillAction("Mind's Eye (False) B", "heracles_mind_eye", 18.0, 240);
            case 5 -> new ServantCardSkillAction("Battle Continuation A", "heracles_battle_continuation", 35.0, 600);
            case 9 -> new ServantCardSkillAction("Twelve Labors", "god_hand_status", 0.0, 20);
            default -> null;
         };
         case "medea" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("Workshop", "workshop", 0.0, 80);
            case 1 -> new ServantCardSkillAction("Auto Minor Magic", "medea_minor_magic", 0.0, 20);
            case 2 -> new ServantCardSkillAction("Craft Mystic Item", "craft_item", 0.0, 80);
            case 3 -> new ServantCardSkillAction("Dragonfang Soldier", "summon_dragonfang", 0.0, 80);
            case 4 -> new ServantCardSkillAction("Blink Volley", "blink_volley", 26.0, 120);
            case 5 -> new ServantCardSkillAction("Barrier Nova", "barrier", 30.0, 240);
            case 6 -> new ServantCardSkillAction("Hecate Bind", "bind", 32.0, 220);
            case 7 -> new ServantCardSkillAction("Aerial Escape", "escape", 28.0, 180);
            case 8 -> new ServantCardSkillAction(crouching ? "Greater Hecate Beam" : "Thunderstorm", crouching ? "medea_beam" : "thunder", crouching ? 40.0 : 42.0, crouching ? 220 : 320);
            case 9 -> new ServantCardSkillAction("Rule Breaker", "rule_breaker", 80.0, 3600);
            default -> null;
         };
         case "medusa" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("Chain Snare", "snare", 22.0, 140);
            case 1 -> new ServantCardSkillAction("Viper Rush", "viper_rush", 24.0, 120);
            case 2 -> new ServantCardSkillAction("Serpent Step", "serpent_step", 20.0, 100);
            case 3 -> new ServantCardSkillAction("Mystic Eyes", "medusa_mystic_eyes", 12.0, 80);
            case 4 -> new ServantCardSkillAction("Bloodfort Andromeda", "bloodfort_field", 38.0, 600);
            case 5 -> new ServantCardSkillAction("Monstrous Strength B", "monster_strength", 28.0, 300);
            case 9 -> new ServantCardSkillAction("Bellerophon", "bellerophon", 90.0, 3600);
            default -> null;
         };
         case "cursed_arm_hassan" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("Presence Concealment", "stealth", 18.0, 220);
            case 1 -> new ServantCardSkillAction("Self Modification", "self_mod", 28.0, 360);
            case 2 -> new ServantCardSkillAction("Create Dirk", "hassan_dagger", 0.0, 40);
            case 3 -> new ServantCardSkillAction("Dirk Throw", "dirk_throw", 8.0, 40);
            case 4 -> new ServantCardSkillAction("Shadow Step", "shadow_step", 16.0, 120);
            case 5 -> new ServantCardSkillAction("Shadow Lunge", "shadow_lunge", 20.0, 180);
            case 9 -> new ServantCardSkillAction("Zabaniya", "zabaniya", 80.0, 1400);
            default -> null;
         };
         case "emiya_archer" -> switch (slot) {
            case -1 -> new ServantCardSkillAction("Twin Flurry", "twin_flurry", 18.0, 20);
            case 0 -> new ServantCardSkillAction("Project Kanshou and Bakuya", "emiya_kb", 10.0, 20);
            case 1 -> new ServantCardSkillAction("Rho Aias", "rho_aias", 200.0, 300);
            case 2 -> new ServantCardSkillAction("Pseudo Spiral Sword", "emiya_spiral", 120.0, 10);
            case 3 -> new ServantCardSkillAction("Crimson Hound", "emiya_hound", 100.0, 80);
            case 4 -> new ServantCardSkillAction("Projection Loadout Cycle", "emiya_cycle", 100.0, 20);
            case 5 -> new ServantCardSkillAction("Trace Opponent Weapon", "copy_weapon", 38.0, 360);
            case 6 -> new ServantCardSkillAction("Layered Projection", "emiya_layered_projection", 36.0, 240);
            case 9 -> new ServantCardSkillAction("Unlimited Blade Works", "ubw", 0.0, 3600);
            default -> null;
         };
         case "sasaki_kojiro" -> switch (slot) {
            case -1 -> new ServantCardSkillAction("Iaijutsu Step", "sasaki_crouch_combo", 10.0, 20);
            case 0 -> new ServantCardSkillAction("Presence Concealment", "stealth", 18.0, 220);
            case 1 -> new ServantCardSkillAction("Tsubame Gaeshi", "tsubame_gaeshi", 0.0, 300);
            case 2 -> new ServantCardSkillAction("Afterimage", "sasaki_afterimage", 15.0, 160);
            case 3 -> new ServantCardSkillAction("Mind's Eye (False)", "sasaki_mind_eye", 18.0, 240);
            case 4 -> new ServantCardSkillAction("Sweep", "sasaki_sweep", 12.0, 80);
            case 5 -> new ServantCardSkillAction("Transparency", "sasaki_transparency", 20.0, 300);
            default -> null;
         };
         case "oda_nobunaga" -> switch (slot) {
            case -1 -> new ServantCardSkillAction("Heshikiri Hasebe Short Thrust", "oda_hasebe_short_thrust", 10.0, 24);
            case 0 -> new ServantCardSkillAction("Floating Matchlock Refill", "oda_floating_refill", 20.0, 80);
            case 1 -> new ServantCardSkillAction("Three Line Rotation", "oda_three_line", 32.0, 220);
            case 2 -> new ServantCardSkillAction("Demon King", "maou", 42.0, 500);
            case 3 -> new ServantCardSkillAction("Encircling Matchlocks", "oda_encircle_matchlocks", 34.0, 180);
            case 4 -> new ServantCardSkillAction("Charged Matchlock", "oda_charged_matchlock", 55.0, 320);
            case 5 -> new ServantCardSkillAction("Crossfire Net", "oda_crossfire_net", 36.0, 260);
            case 6 -> new ServantCardSkillAction("Scorched Earth Formation", "oda_scorched_earth", 38.0, 420);
            case 7 -> new ServantCardSkillAction("Heshikiri Hasebe Fire Breakthrough", "oda_hasebe_breakthrough", 28.0, 160);
            case 8 -> new ServantCardSkillAction("Three Thousand Worlds", "three_thousand", 95.0, 3600);
            case 9 -> new ServantCardSkillAction("Dairokuten Maou Hajun", "hajun", 130.0, 3600);
            default -> null;
         };
         case "enkidu" -> switch (slot) {
            case -1 -> new ServantCardSkillAction("Morphing Melee Combo", "enkidu_morph_melee", 0.0, 18);
            case 0 -> new ServantCardSkillAction("Transfiguration", "transfiguration", 0.0, 0);
            case 1 -> new ServantCardSkillAction("Presence Detection", "detection", 18.0, 160);
            case 2 -> new ServantCardSkillAction("Chains of Heaven", "chains", 36.0, 260);
            case 3 -> new ServantCardSkillAction(crouching ? "Grand Age of Babylon" : "Age of Babylon", crouching ? "age_babylon_grand" : "age_babylon", crouching ? 80.0 : 34.0, crouching ? 520 : 180);
            case 4 -> new ServantCardSkillAction("Mega Age of Babylon", "mega_age", 90.0, 900);
            case 5 -> new ServantCardSkillAction("Clay Bulwark", "bulwark", 30.0, 320);
            case 6 -> new ServantCardSkillAction("Stardust Step", "stardust", 20.0, 100);
            case 7 -> new ServantCardSkillAction("Earth Spike Sprout", "earth_spike", 24.0, 160);
            case 8 -> new ServantCardSkillAction("Sky Spear Sweep", "sky_spear_sweep", 32.0, 220);
            case 9 -> new ServantCardSkillAction("Enuma Elish", "enuma_elish", 120.0, 3600);
            default -> null;
         };
         case "gilgamesh" -> switch (slot) {
            case -1 -> new ServantCardSkillAction("近战小技能", "gilgamesh_melee", 0.0, 18);
            case 0 -> new ServantCardSkillAction("王律键", "gilgamesh_key", 0.0, 0);
            case 1 -> new ServantCardSkillAction("天之锁", "gilgamesh_chains", 36.0, 260);
            case 2 -> new ServantCardSkillAction(crouching ? "大规模王之宝库" : "王之宝库", crouching ? "gilgamesh_grand_vault" : "gilgamesh_vault", crouching ? 80.0 : 34.0, crouching ? 520 : 180);
            case 3 -> new ServantCardSkillAction("环敌王之宝库", "gilgamesh_ring_vault", 55.0, 300);
            case 4 -> new ServantCardSkillAction("解毒的灵药", "gilgamesh_elixir", 45.0, 600);
            case 5 -> new ServantCardSkillAction("众神之盾", "gilgamesh_divine_shield", 30.0, 100);
            case 6 -> new ServantCardSkillAction("千里眼", "gilgamesh_clairvoyance", 18.0, 160);
            case 7 -> new ServantCardSkillAction("王者威仪", "gilgamesh_charisma", 40.0, 360);
            case 8 -> new ServantCardSkillAction("狂笑·超大规模王之宝库", "gilgamesh_laugh_vault", 240.0, 3600);
            case 9 -> new ServantCardSkillAction("伊伽莉玛 & 修尔夏伽那", "gilgamesh_cross_slash", 160.0, 1400);
            default -> null;
         };
         case "gawain" -> switch (slot) {
            case -1 -> new ServantCardSkillAction("Solar Combo", "solar_combo", 0.0, 30);
            case 0 -> new ServantCardSkillAction("Gallatin Spark", "gallatin_spark", 28.0, 180);
            case 1 -> new ServantCardSkillAction("Solar Rebuke", "solar_rebuke", 32.0, 220);
            case 2 -> new ServantCardSkillAction("Radiant Field", "radiant_field", 36.0, 520);
            case 3 -> new ServantCardSkillAction("Flame Tornado", "flame_tornado", 34.0, 260);
            case 4 -> new ServantCardSkillAction("Numeral of the Saint", "noon_guard", 24.0, 360);
            case 5 -> new ServantCardSkillAction("Charisma E", "gawain_charisma", 20.0, 300);
            default -> null;
         };
         case "li_shuwen" -> switch (slot) {
            case -1 -> new ServantCardSkillAction("Baji Step", "shoulder_charge", 0.0, 24);
            case 0 -> new ServantCardSkillAction("Circle Realm (Extreme)", "circle_realm", 16.0, 260);
            case 1 -> new ServantCardSkillAction("Yin Yang Crossing", "yin_yang", 28.0, 160);
            case 2 -> new ServantCardSkillAction("Counter", "counter", 26.0, 240);
            case 3 -> new ServantCardSkillAction("Tremor Interrupt", "interrupt", 24.0, 160);
            case 4 -> new ServantCardSkillAction("Pursuit", "pursuit", 16.0, 100);
            case 5 -> new ServantCardSkillAction("Fierce Tiger Climbs Mountain", "li_fierce_tiger", 26.0, 200);
            case 9 -> new ServantCardSkillAction("Wu Er Da", "wu_er_da", 0.0, 1200);
            default -> null;
         };
         case "paracelsus" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("Workshop", "paracelsus_workshop", 0.0, 80);
            case 1 -> new ServantCardSkillAction("Item Construction", "paracelsus_craft_stone", 0.0, 80);
            case 2 -> new ServantCardSkillAction("Elemental Spirit", "paracelsus_spirit_toggle", 0.0, 20);
            case 3 -> new ServantCardSkillAction("Return to Workshop", "paracelsus_workshop_teleport", 28.0, 180);
            case 4 -> new ServantCardSkillAction("Furnace of the Flame Emperor", "paracelsus_fire_furnace", 40.0, 600);
            case 5 -> new ServantCardSkillAction("Deep Sea Pressure", "paracelsus_water_pressure", 35.0, 500);
            case 6 -> new ServantCardSkillAction("Roar of the Mountains", "paracelsus_earth_roar", 45.0, 700);
            case 7 -> new ServantCardSkillAction("Cutting of the Firmament", "paracelsus_wind_cut", 30.0, 400);
            case 8 -> new ServantCardSkillAction("Elemental Guardian", "paracelsus_elemental_guardian", 0.0, 100);
            default -> null;
         };
         default -> null;
      };
   }
}

