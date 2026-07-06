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
            default -> null;
         };
         case "cu_chulainn" -> switch (slot) {
            case -1 -> new ServantCardSkillAction("Spear Thrust", "cu_crouch_thrust", 0.0, 18);
            case 0 -> new ServantCardSkillAction("Ansuz Rune", "cu_ansuz", 22.0, 120);
            case 1 -> new ServantCardSkillAction("Laguz Rune", "cu_laguz", 20.0, 160);
            case 2 -> new ServantCardSkillAction("Tiwaz Rune", "cu_tiwaz", 24.0, 260);
            case 3 -> new ServantCardSkillAction("Algiz Rune", "cu_algiz", 30.0, 300);
            case 4 -> new ServantCardSkillAction("Berkana Rune", "cu_berkana", 34.0, 400);
            default -> null;
         };
         case "heracles" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("Great Leap", "heracles_big_jump", 18.0, 120);
            case 1 -> new ServantCardSkillAction("Ground Slam", "heracles_ground_slam", 32.0, 180);
            case 2 -> new ServantCardSkillAction("Roar", "heracles_roar", 18.0, 180);
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
            case 9 -> new ServantCardSkillAction("Bellerophon", "bellerophon", 90.0, 3600);
            default -> null;
         };
         case "cursed_arm_hassan" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("Presence Concealment", "stealth", 18.0, 220);
            case 1 -> new ServantCardSkillAction("Self Modification", "self_mod", 28.0, 360);
            case 2 -> new ServantCardSkillAction("Create Dirk", "hassan_dagger", 0.0, 40);
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
            case 1 -> new ServantCardSkillAction("Tsubame Gaeshi", "tsubame_gaeshi", 0.0, 1200);
            default -> null;
         };
         case "oda_nobunaga" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("Floating Matchlock", "matchlock", 18.0, 80);
            case 1 -> new ServantCardSkillAction("Matchlock Volley", "volley", 26.0, 140);
            case 2 -> new ServantCardSkillAction("Three Line Rotation", "fire_barrage", 32.0, 220);
            case 3 -> new ServantCardSkillAction("Atsumori Step", "atsumori", 22.0, 100);
            case 4 -> new ServantCardSkillAction("Anti-Mystery Spark", "anti_mystery", 32.0, 260);
            case 5 -> new ServantCardSkillAction("Demon King Pressure", "maou", 42.0, 500);
            case 6 -> new ServantCardSkillAction("Strategy", "strategy", 24.0, 360);
            case 7 -> new ServantCardSkillAction("Ash Field", "ash_field", 38.0, 420);
            case 8 -> new ServantCardSkillAction("Hasebe Repel", "hasebe_repel", 24.0, 160);
            case 9 -> new ServantCardSkillAction(crouching ? "Hajun" : "Three Thousand Worlds", crouching ? "hajun" : "three_thousand", crouching ? 130.0 : 95.0, 3600);
            default -> null;
         };
         case "enkidu" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("Transfiguration", "transfiguration", 28.0, 320);
            case 1 -> new ServantCardSkillAction("Presence Detection", "detection", 18.0, 160);
            case 2 -> new ServantCardSkillAction("Perfect Form", "perfect_form", 38.0, 520);
            case 3 -> new ServantCardSkillAction("Chains of Heaven", "chains", 32.0, 220);
            case 4 -> new ServantCardSkillAction("Age of Babylon", "age_babylon", 34.0, 180);
            case 5 -> new ServantCardSkillAction("Earth Wedge", "earth_wedge", 28.0, 160);
            case 6 -> new ServantCardSkillAction("Clay Bulwark", "bulwark", 30.0, 320);
            case 7 -> new ServantCardSkillAction("Stardust Step", "stardust", 20.0, 100);
            case 8 -> new ServantCardSkillAction("Mega Age of Babylon", "mega_age", 50.0, 420);
            case 9 -> new ServantCardSkillAction("Enuma Elish", "enuma_elish", 120.0, 3600);
            default -> null;
         };
         case "gawain" -> switch (slot) {
            case -1 -> new ServantCardSkillAction("Solar Combo", "solar_combo", 0.0, 30);
            case 0 -> new ServantCardSkillAction("Gallatin Spark", "gallatin_spark", 28.0, 180);
            case 1 -> new ServantCardSkillAction("Solar Rebuke", "solar_rebuke", 32.0, 220);
            case 2 -> new ServantCardSkillAction("Radiant Field", "radiant_field", 36.0, 520);
            case 3 -> new ServantCardSkillAction("Flame Tornado", "flame_tornado", 34.0, 260);
            default -> null;
         };
         case "li_shuwen" -> switch (slot) {
            case -1 -> new ServantCardSkillAction("Baji Step", "shoulder_charge", 0.0, 24);
            case 0 -> new ServantCardSkillAction("Circle Realm (Extreme)", "circle_realm", 16.0, 260);
            case 1 -> new ServantCardSkillAction("Yin Yang Crossing", "yin_yang", 28.0, 160);
            case 2 -> new ServantCardSkillAction("Counter", "counter", 26.0, 240);
            case 3 -> new ServantCardSkillAction("Tremor Interrupt", "interrupt", 24.0, 160);
            case 9 -> new ServantCardSkillAction("Wu Er Da", "wu_er_da", 0.0, 1200);
            default -> null;
         };
         case "paracelsus" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("Element Cycle", "element_cycle", 8.0, 20);
            case 1 -> new ServantCardSkillAction("High Speed Chanting", "chant", 22.0, 280);
            case 2 -> new ServantCardSkillAction("Elemental Spirit", "elemental_spirit", 30.0, 220);
            case 3 -> new ServantCardSkillAction("Philosopher Stone", "stone", 36.0, 420);
            case 4 -> new ServantCardSkillAction("Fire Magic", "fire", 24.0, 120);
            case 5 -> new ServantCardSkillAction("Water Magic", "water", 24.0, 120);
            case 6 -> new ServantCardSkillAction("Earth Magic", "earth", 24.0, 120);
            case 7 -> new ServantCardSkillAction("Wind Magic", "wind", 24.0, 120);
            case 8 -> new ServantCardSkillAction("Mixed Element Burst", "mixed_element", 42.0, 320);
            default -> null;
         };
         default -> null;
      };
   }
}

