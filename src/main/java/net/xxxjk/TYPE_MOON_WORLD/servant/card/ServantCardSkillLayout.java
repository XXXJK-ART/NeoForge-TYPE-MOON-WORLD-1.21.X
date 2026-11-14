package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderCombatRules;

public final class ServantCardSkillLayout {
   private ServantCardSkillLayout() {
   }

   public static ServantCardSkillAction actionFor(String servantId, int slot, boolean crouching) {
      return switch (servantId == null ? "" : servantId) {
         case "artoria_pendragon" -> switch (slot) {
            case -1 -> new ServantCardSkillAction("Knight Combo", "artoria_small_combo", 16.0, 30);
            case 0 -> new ServantCardSkillAction("Mana Burst (Toggle)", "mana_burst",
               net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules.BURST_START_MP, 0);
            case 1 -> new ServantCardSkillAction("Strike Air: Hammer of the Wind King", "invisible_air_hammer", 60.0,
               net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules.WIND_HAMMER_COOLDOWN);
            case 2 -> new ServantCardSkillAction("Knight Triple Slash", net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules.KNIGHT_TRIPLE_SLASH,
               net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules.TRIPLE_SLASH_MP, net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules.TRIPLE_SLASH_COOLDOWN);
            case 3 -> new ServantCardSkillAction("Invisible Air Release", "invisible_air_release", 28.0,
               net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules.WIND_RELEASE_COOLDOWN);
            case 4 -> new ServantCardSkillAction("Intercepting Thrust", net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules.INTERCEPT_THRUST,
               net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules.INTERCEPT_THRUST_MP, net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules.INTERCEPT_THRUST_COOLDOWN);
            case 5 -> new ServantCardSkillAction("Turning Slash", net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules.SPINNING_SLASH,
               net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules.SPINNING_SLASH_MP, net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules.SPINNING_SLASH_COOLDOWN);
            case 6 -> new ServantCardSkillAction("Invisible Air: Wind Defense", net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules.WIND_DEFENSE,
               net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules.WIND_DEFENSE_MP,
               net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules.WIND_DEFENSE_COOLDOWN);
            case 7 -> new ServantCardSkillAction("Rising Slash", net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules.UPWARD_SLASH,
               net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules.UPWARD_SLASH_MP, net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules.UPWARD_SLASH_COOLDOWN);
            case 8 -> new ServantCardSkillAction("Guard Counter", net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules.GUARD_COUNTER,
               net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules.GUARD_COUNTER_MP, net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules.GUARD_COUNTER_COOLDOWN);
            case 9 -> new ServantCardSkillAction("Invisible Air", net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules.WIND_BARRIER_TOGGLE, 0.0, 0);
            default -> null;
         };
         case "okita_souji_saber" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("Shukuchi B", "okita_shukuchi", 0.0, 10);
            case 1 -> new ServantCardSkillAction("Tennen Rishin-ryu: Ichimonji", "okita_ichimonji", 8.0, 60);
            case 2 -> new ServantCardSkillAction("Tennen Rishin-ryu: Kaifu", "okita_kaifuu", 10.0, 90);
            case 3 -> new ServantCardSkillAction("Tennen Rishin-ryu: Thrust", "okita_thrust", 5.0, 16);
            case 4 -> new ServantCardSkillAction("Tennen Rishin-ryu: Downward Slash", "okita_downward_slash", 8.0, 60);
            case 5 -> new ServantCardSkillAction("Coughing Feint", "okita_feigned_retreat", 8.0, 140);
            case 6 -> new ServantCardSkillAction("Stance-Breaking Thrust", "okita_stance_break", 14.0, 180);
            case 7 -> new ServantCardSkillAction("Shinsengumi Command", "okita_shinsengumi_command", 6.0, 80);
            case 8 -> new ServantCardSkillAction("Mumyoudan-zuki", "okita_mumyoudan_zuki", 20.0, 3600);
            case 9 -> new ServantCardSkillAction("Flag of Sincerity", "okita_flag_of_sincerity", 100.0, 2400);
            default -> null;
         };
         case "jeanne_alter" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("Execution Flame", "jeanne_alter_fire_pillar", 22.0, 140);
            case 1 -> new ServantCardSkillAction("Cursed Lance Array", "jeanne_alter_cursed_lance", 35.0, 240);
            case 2 -> new ServantCardSkillAction("Self-Modification EX", "jeanne_alter_self_modification_ex", 30.0, 500);
            case 3 -> new ServantCardSkillAction("Dragon Witch EX", "jeanne_alter_dragon_witch_ex", 42.0, 600);
            case 4 -> new ServantCardSkillAction("Inferno Body", "jeanne_alter_inferno_body", 45.0, 600);
            case 5 -> new ServantCardSkillAction("Ephemeral Dream A", "jeanne_alter_ephemeral_dream_a", 55.0, 800);
            case 9 -> new ServantCardSkillAction("Le Grondement Du Haine", "jeanne_alter_le_grondement_du_haine", 220.0, 2400);
            default -> null;
         };
         case "gilles_de_rais_caster" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("Summon Small Sea Monster", "gilles_summon_small_sea_monster", 0.0, 80);
            case 1 -> new ServantCardSkillAction("Summon Large Sea Monster", "gilles_summon_large_sea_monster", 0.0, 240);
            case 2 -> new ServantCardSkillAction("Abyssal Gaze", "gilles_abyssal_gaze", 10.0, 400);
            case 3 -> new ServantCardSkillAction("Life Absorption", "gilles_life_absorb", 0.0, 80);
            case 4 -> new ServantCardSkillAction("Sea Monster Command", "gilles_sea_monster_command", 0.0, 20);
            case 5 -> new ServantCardSkillAction("Pollution Ink Fog", "gilles_pollution_ink_fog", 0.0, 360);
            case 6 -> new ServantCardSkillAction("Prelati Shroud", "gilles_prelati_shroud", 0.0, 600);
            case 7 -> new ServantCardSkillAction("Profane Growth", "gilles_profane_growth", 0.0, 900);
            case 8 -> new ServantCardSkillAction("Evil God Praise", "gilles_evil_god_praise", 40.0, 800);
            case 9 -> new ServantCardSkillAction("Prelati's Spellbook: Huge Sea Monster", "gilles_uncontrolled_huge_sea_monster", 0.0, 1200);
            default -> null;
         };
         case "cu_chulainn" -> {
            var spec=net.xxxjk.TYPE_MOON_WORLD.servant.cu.CuChulainnRules.skill(slot);
            yield spec==null?null:new ServantCardSkillAction(spec.id(),spec.id(),spec.mana(),spec.cooldown());
         }
         case "heracles" -> {
            var spec = net.xxxjk.TYPE_MOON_WORLD.servant.heracles.HeraclesAbilityService.spec(slot);
            yield spec == null ? null : new ServantCardSkillAction(spec.name(), spec.id(), spec.mana(), spec.cooldown());
         }
         case "medea" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("Workshop", "workshop", 0.0, 80);
            case 1 -> new ServantCardSkillAction("Auto Minor Magic", "medea_minor_magic", 0.0, 20);
            case 2 -> new ServantCardSkillAction("Craft Mystic Item", "craft_item", 0.0, 80);
            case 3 -> new ServantCardSkillAction("Dragonfang Soldier", "summon_dragonfang", 0.0, 80);
            case 4 -> new ServantCardSkillAction("Blink Volley", "blink_volley", 26.0, 120);
            case 5 -> new ServantCardSkillAction("Barrier Nova", "barrier", 30.0, 240);
            case 6 -> new ServantCardSkillAction("Hecate Bind", "bind", 32.0, 220);
            case 7 -> new ServantCardSkillAction("Aerial Escape", "escape", 120.0, 400);
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
            case 0 -> new ServantCardSkillAction("Presence Concealment A+", "hassan_concealment", 18.0, 0);
            case 1 -> new ServantCardSkillAction("Quick Stab", "hassan_quick_stab", 0.0, 30);
            case 2 -> new ServantCardSkillAction("Create Dirk", "hassan_dagger", 0.0, 40);
            case 3 -> new ServantCardSkillAction("Throwing (Dagger) B", "dirk_throw", 8.0, 40);
            case 4 -> new ServantCardSkillAction("Shadow Ambush", "shadow_step", 10.0, 120);
            case 5 -> new ServantCardSkillAction("Shadow Lunge", "shadow_lunge", 7.0, 85);
            case 6 -> new ServantCardSkillAction("Single Dirk Throw", "knife_feint", 5.0, 70);
            case 7 -> new ServantCardSkillAction("Elbow Strike", "elbow_strike", 6.0, 80);
            case 8 -> new ServantCardSkillAction("Trip Sweep", "trip_sweep", 8.0, 160);
            case 9 -> new ServantCardSkillAction("Zabaniya", "zabaniya", 80.0, 1400);
            default -> null;
         };
         case "fanatic_assassin" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("Presence Concealment A-", "fanatic_concealment", 15.0, 300);
            case 1 -> new ServantCardSkillAction("Delusional Heartbeat", "fanatic_heartbeat", 50.0, 600);
            case 2 -> new ServantCardSkillAction("Delusional Marrow", "fanatic_marrow", 40.0, 600);
            case 3 -> new ServantCardSkillAction("Delusional Hair", "fanatic_hair", 30.0, 600);
            case 4 -> new ServantCardSkillAction("Delusional Temperature", "fanatic_temperature", 30.0, 600);
            case 5 -> new ServantCardSkillAction("Delusional Nerves", "fanatic_nerves", 20.0, 600);
            case 6 -> new ServantCardSkillAction("Delusional Computer", "fanatic_computer", 40.0, 600);
            case 7 -> new ServantCardSkillAction("Delusional Toxin", "fanatic_toxin", 30.0, 600);
            case 8 -> new ServantCardSkillAction("Delusional Jinn", "fanatic_jinn", 60.0, 600);
            default -> null;
         };
         case "shadow_hassan" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("气息遮断 EX", "shadow_hassan_concealment",
               ServantCardShadowHassanSkills.ACTIVE_CONCEALMENT_MANA_COST,
               ServantCardShadowHassanSkills.ACTIVE_CONCEALMENT_COOLDOWN_TICKS);
            case 1 -> new ServantCardSkillAction("影灯笼 A", "shadow_hassan_lantern", 0.0, 0);
            case 2 -> new ServantCardSkillAction("幽弋 A", "shadow_hassan_wandering", 15.0, 100);
            case 3 -> new ServantCardSkillAction("Shadow Ambush", "shadow_hassan_ambush", 20.0, 120);
            case 4 -> new ServantCardSkillAction("Shadow Bind", "shadow_hassan_bind", 18.0, 160);
            case 5 -> new ServantCardSkillAction("Shadow Blade Flurry", "shadow_hassan_flurry", 24.0, 180);
            case 6 -> new ServantCardSkillAction("Shadow Retreat", "shadow_hassan_retreat", 12.0, 140);
            case 7 -> new ServantCardSkillAction("Slash", "shadow_hassan_slash", 30.0, 300);
            case 8 -> new ServantCardSkillAction("影中附身", "shadow_hassan_possession", 0.0, 0);
            case 9 -> new ServantCardSkillAction("Zabaniya: Meditative Sensitivity", "shadow_hassan_meditative_sensitivity", 0.0, 0);
            default -> null;
         };
         case "emiya_archer" -> switch (slot) {
            case -1 -> new ServantCardSkillAction("Twin Flurry", "twin_flurry", 18.0, 20);
            case 0 -> new ServantCardSkillAction("Project Kanshou and Bakuya", "emiya_kb", 10.0, 20);
            case 1 -> new ServantCardSkillAction("Rho Aias", "rho_aias", 200.0, 300);
            case 2 -> new ServantCardSkillAction("Pseudo Spiral Sword", "emiya_spiral", 500.0, 10);
            case 3 -> new ServantCardSkillAction("Crimson Hound", "emiya_hound", 300.0, 80);
            case 4 -> new ServantCardSkillAction("Projection Loadout Cycle", "emiya_cycle", 300.0, 20);
            case 5 -> new ServantCardSkillAction("Trace Opponent Weapon", "copy_weapon", 38.0, 360);
            case 6 -> new ServantCardSkillAction("Layered Projection", "emiya_layered_projection", 36.0, 240);
            case 7 -> new ServantCardSkillAction("Spherical Layered Projection", "emiya_spherical_projection", 45.0, 240);
            case 8 -> new ServantCardSkillAction("Unlimited Blade Counter", "emiya_auto_counter", 60.0, 600);
            case 9 -> new ServantCardSkillAction("Unlimited Blade Works", "ubw", 0.0, 3600);
            default -> null;
         };
         case "sasaki_kojiro" -> switch (slot) {
            case -1 -> new ServantCardSkillAction("Crouching Cutback", "sasaki_crouch_cutback", 8.0, 20);
            case 0 -> new ServantCardSkillAction("Presence Concealment D", "sasaki_presence_concealment", 18.0, 0);
            case 1 -> new ServantCardSkillAction("Ichimonji: Vertical Slash", "sasaki_vertical_slash", 12.0, 80);
            case 2 -> new ServantCardSkillAction("Falling Swallow", "sasaki_downward_slash", 8.0, 60);
            case 3 -> new ServantCardSkillAction("Returning Swallow", "sasaki_upward_slash", 10.0, 80);
            case 4 -> new ServantCardSkillAction("Ichimonji: Horizontal Slash", "sasaki_horizontal_slash", 12.0, 80);
            case 5 -> new ServantCardSkillAction("Transparency B+", "sasaki_transparency", 20.0, 600);
            case 6 -> new ServantCardSkillAction("Swallow-Piercing Thrust", "sasaki_thrust", 14.0, 100);
            case 7 -> new ServantCardSkillAction("Turning Formation Breaker", "sasaki_spinning_slash", 16.0, 140);
            case 8 -> new ServantCardSkillAction("Ganryu Stance Counter", "sasaki_counter_stance", 18.0, 180);
            case 9 -> new ServantCardSkillAction("Tsubame Gaeshi", "sasaki_tsubame_gaeshi", 20.0, 600);
            default -> null;
         };
         case "oda_nobunaga" -> {
            var skill = net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaRules.skill(slot);
            yield skill == null ? null : new ServantCardSkillAction(skill.name(), skill.effect(), skill.mp(), skill.cooldown());
         }
         case "enkidu" -> switch (slot) {
            case -1 -> new ServantCardSkillAction("Morphing Melee Combo", "enkidu_morph_melee", 0.0, 18);
            case 0 -> new ServantCardSkillAction("Transfiguration", "transfiguration", 0.0, 0);
            case 1 -> new ServantCardSkillAction("Presence Detection", "detection", 18.0, 160);
            // Enkidu exposes one NP only; the chain barrage is an Enuma sub-stage.
            case 2 -> null;
            case 3 -> new ServantCardSkillAction(crouching ? "Grand Age of Babylon" : "Age of Babylon", crouching ? "age_babylon_grand" : "age_babylon", crouching ? 80.0 : 34.0, crouching ? 520 : 180);
            case 4 -> new ServantCardSkillAction("Mega Age of Babylon", "mega_age", 90.0, 900);
            case 5 -> new ServantCardSkillAction("Clay Bulwark", "bulwark", 30.0, 320);
            case 6 -> new ServantCardSkillAction("Stardust Step", "stardust", 20.0, 100);
            case 7 -> new ServantCardSkillAction("Earth Spike Sprout", "earth_spike", 24.0, 160);
            case 8 -> new ServantCardSkillAction("Sky Spear Sweep", "sky_spear_sweep", 32.0, 220);
            case 9 -> new ServantCardSkillAction("Enuma Elish", "enuma_elish", 120.0, 3600);
            default -> null;
         };
         case "gilgamesh_caster" -> switch (slot) {
            case -1 -> new ServantCardSkillAction("石板射击", "caster_gilgamesh_slate_volley", 0.0, 18);
            case 0 -> new ServantCardSkillAction("王律键", "gilgamesh_key", 0.0, 0);
            case 1 -> new ServantCardSkillAction("石板连射", "caster_gilgamesh_slate_volley", 0.0, 24);
            case 2 -> new ServantCardSkillAction("领袖气质 A+", "caster_gilgamesh_leader", 20.0, 35 * 20);
            case 3 -> new ServantCardSkillAction("王的回归 A", "caster_gilgamesh_return", 15.0, 30 * 20);
            case 4 -> new ServantCardSkillAction("道具作成（伪）", "caster_gilgamesh_item_creation", 0.0, 0);
            case 5 -> new ServantCardSkillAction("阵地建造 A", "caster_gilgamesh_workshop", 0.0, 80);
            case 6 -> new ServantCardSkillAction("王之号炮：单轮校准", "caster_gilgamesh_cannon_calibration", 0.0, 20);
            case 7 -> new ServantCardSkillAction("千里眼", "gilgamesh_clairvoyance", 18.0, 160);
            case 8 -> new ServantCardSkillAction("众神之盾", "gilgamesh_divine_shield", 30.0, 0);
            case 9 -> new ServantCardSkillAction("王之号炮", "caster_gilgamesh_royal_cannon", 0.0, 0);
            default -> null;
         };
         case "gilgamesh" -> switch (slot) {
            case -1 -> new ServantCardSkillAction("近战小技能", "gilgamesh_melee", 0.0, 18);
            case 0 -> new ServantCardSkillAction("王律键", "gilgamesh_key", 0.0, 0);
            case 1 -> new ServantCardSkillAction("天之锁", "gilgamesh_chains", 36.0, 260);
            case 2 -> new ServantCardSkillAction(crouching ? "大规模王之宝库" : "王之宝库", crouching ? "gilgamesh_grand_vault" : "gilgamesh_vault", crouching ? 80.0 : 34.0, crouching ? 200 : 100);
            case 3 -> new ServantCardSkillAction("环敌王之宝库", "gilgamesh_ring_vault", 55.0, 300);
            case 4 -> new ServantCardSkillAction("解毒的灵药", "gilgamesh_elixir", 45.0, 600);
            case 5 -> new ServantCardSkillAction("众神之盾", "gilgamesh_divine_shield", 30.0, 0);
            case 6 -> new ServantCardSkillAction("千里眼", "gilgamesh_clairvoyance", 18.0, 160);
            case 7 -> new ServantCardSkillAction("王者威仪", "gilgamesh_charisma", 40.0, 360);
            case 8 -> new ServantCardSkillAction("大规模王之宝库", "gilgamesh_laugh_vault", 80.0, 600);
            case 9 -> new ServantCardSkillAction("伊伽莉玛 & 修尔夏伽那", "gilgamesh_cross_slash", 300.0, 120 * 20);
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
            case 6 -> new ServantCardSkillAction("Bajiquan Combination", "li_baji_combo", 20.0, 100);
            case 7 -> new ServantCardSkillAction("Bajiquan High Jump", "li_high_jump", 8.0, 80);
            case 8 -> new ServantCardSkillAction("Fa Jin", "li_fa_jin", 24.0, 140);
            case 9 -> new ServantCardSkillAction("Wu Er Da", "wu_er_da", 0.0, 1200);
            default -> null;
         };
         case "paracelsus" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("Workshop", "paracelsus_workshop", 0.0, 80);
            case 1 -> new ServantCardSkillAction("Item Construction", "paracelsus_craft_stone", 0.0, 80);
            case 2 -> new ServantCardSkillAction("Elemental Spirit", "paracelsus_spirit_toggle", 0.0, 20);
            case 3 -> new ServantCardSkillAction("Return to Workshop", "paracelsus_workshop_teleport", 120.0, 400);
            case 4 -> new ServantCardSkillAction("Furnace of the Flame Emperor", "paracelsus_fire_furnace", 40.0, 600);
            case 5 -> new ServantCardSkillAction("Deep Sea Pressure", "paracelsus_water_pressure", 35.0, 500);
            case 6 -> new ServantCardSkillAction("Roar of the Mountains", "paracelsus_earth_roar", 45.0, 700);
            case 7 -> new ServantCardSkillAction("Cutting of the Firmament", "paracelsus_wind_cut", 30.0, 400);
            case 8 -> new ServantCardSkillAction("Elemental Guardian", "paracelsus_elemental_guardian", 0.0, 100);
            default -> null;
         };
         case "pale_rider" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("Plague Spawn", "pale_rider_spawn", 10.0, 40);
            case 1 -> new ServantCardSkillAction("Possession", "pale_rider_possession", 18.0, 80);
            case 2 -> new ServantCardSkillAction("Command", "pale_rider_command", 0.0, 20);
            case 3 -> new ServantCardSkillAction("Perfect Concealment", "pale_rider_stealth", 12.0, 100);
            case 4 -> new ServantCardSkillAction("Ash Transfer", "pale_rider_transfer", 20.0, 100);
            case 5 -> new ServantCardSkillAction("Plague Rush", "pale_rider_plague_rush", PaleRiderCombatRules.PLAGUE_RUSH_MP_COST, PaleRiderCombatRules.PLAGUE_RUSH_COOLDOWN_TICKS);
            case 6 -> new ServantCardSkillAction("Ash Step", "pale_rider_ash_step", PaleRiderCombatRules.ASH_STEP_MP_COST, PaleRiderCombatRules.ASH_STEP_COOLDOWN_TICKS);
            case 7 -> new ServantCardSkillAction("Death Pulse", "pale_rider_death_pulse", PaleRiderCombatRules.DEATH_PULSE_MP_COST, PaleRiderCombatRules.DEATH_PULSE_COOLDOWN_TICKS);
            case 8 -> new ServantCardSkillAction("Guidance of the Underworld", "pale_rider_underworld", PaleRiderCombatRules.UNDERWORLD_CAST_MP_COST, PaleRiderCombatRules.UNDERWORLD_COOLDOWN_TICKS);
            case 9 -> new ServantCardSkillAction("Sword, Famine, Death and Beast", "pale_rider_calamity", PaleRiderCombatRules.CALAMITY_CAST_MP_COST, PaleRiderCombatRules.CALAMITY_COOLDOWN_TICKS);
            default -> null;
         };
         case "hundred_faces_hassan" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("Knife Feint", "hundred_faces_knife_feint", 5.0, 75);
            case 1 -> new ServantCardSkillAction(crouching ? "Persona Direct Command" : "Persona Command",
               crouching ? "hundred_faces_single_command" : "hundred_faces_command", 0.0, 20);
            case 2 -> new ServantCardSkillAction("Persona Dirk", "hundred_faces_dagger", 0.0, 40);
            case 3 -> new ServantCardSkillAction("Persona Switch", "hundred_faces_switch", 0.0, 0);
            case 4 -> new ServantCardSkillAction("Presence Concealment A", "hundred_faces_concealment", 16.0, 110);
            case 5 -> new ServantCardSkillAction("Three Dirk Throw", "hundred_faces_dirk_throw", 8.0, 40);
            case 6 -> new ServantCardSkillAction("Shadow Step Backstab", "hundred_faces_shadow_step", 10.0, 120);
            case 7 -> new ServantCardSkillAction("Shadow Lunge", "hundred_faces_shadow_lunge", 7.0, 90);
            case 8 -> new ServantCardSkillAction("Battle Retreat B", "hundred_faces_battle_retreat", 10.0, 400);
            case 9 -> new ServantCardSkillAction(crouching ? "Zabaniya: Quantity Select" : "Zabaniya: One Face",
               crouching ? "hundred_faces_summon_menu" : "hundred_faces_summon", 0.0, crouching ? 20 : 30);
            default -> null;
         };
         case "diarmuid_ua_duibhne" -> {
            var action = slot == 7 && crouching ? net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidAction.CROSS_GUARD
               : net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidAction.bySlot(slot);
            yield action == null || action.basic() ? null : new ServantCardSkillAction(action.path(), action.skillId(), action.mana(), action.cooldown());
         }
         case "lancelot_berserker" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("Mad Enhancement Combo", "lancelot_maul_combo", 8.0, 80);
            case 1 -> new ServantCardSkillAction("Feral Rush", "lancelot_feral_rush", 12.0, 140);
            case 2 -> new ServantCardSkillAction("Ground Slam", "lancelot_ground_slam", 16.0, 200);
            case 3 -> new ServantCardSkillAction("Hunt Step", "lancelot_hunt_step", 8.0, 100);
            case 4 -> new ServantCardSkillAction("Mana Reversal", "lancelot_mana_reversal", 15.0, 360);
            case 5 -> new ServantCardSkillAction("Mad Roar", "lancelot_berserk_roar", 14.0, 320);
            case 8 -> new ServantCardSkillAction("Knight of Owner", "lancelot_knight_of_owner", 10.0, 40);
            case 9 -> new ServantCardSkillAction("Aroundight", "lancelot_aroundight", 50.0, 1200);
            default -> null;
         };
         case "iskandar" -> switch (slot) {
            case -1 -> new ServantCardSkillAction("Royal Sword Assault", "iskandar_royal_sword_assault", 0.0, 18);
            case 0 -> new ServantCardSkillAction("Bucephalus", "iskandar_bucephalus", 20.0, 40);
            case 1 -> new ServantCardSkillAction("Royal Sword Assault", "iskandar_royal_sword_assault", 10.0, 80);
            case 2 -> new ServantCardSkillAction("Conqueror Order", "iskandar_conqueror_order", 14.0, 180);
            case 3 -> new ServantCardSkillAction("Thunder Call", "iskandar_thunder_call", 18.0, 220);
            case 4 -> new ServantCardSkillAction("Battlefield Stride", "iskandar_battlefield_stride", 12.0, 120);
            case 5 -> new ServantCardSkillAction("Vanguard Breakthrough", "iskandar_vanguard_summon", 24.0, 300);
            case 6 -> new ServantCardSkillAction("Kingly War Cry", "iskandar_kingly_war_cry", 16.0, 260);
            case 7 -> new ServantCardSkillAction("Charge", "iskandar_charge", 20.0, 40);
            case 8 -> new ServantCardSkillAction("Gordius Wheel", "iskandar_gordius_wheel", 50.0, 400);
            case 9 -> new ServantCardSkillAction("Ionioi Hetairoi", "iskandar_ionioi_hetairoi", 60.0, 1800);
            default -> null;
         };
         case "baobhan_sith" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("Curse Panel", "baobhan_sith_curse_panel", 0.0, 0);
            case 1 -> new ServantCardSkillAction("Blood Spike", "baobhan_sith_blood_spike", 6.0, 120);
            case 2 -> new ServantCardSkillAction("Blood Thorns", "baobhan_sith_blood_thorns", 12.0, 220);
            case 3 -> new ServantCardSkillAction("Fetch Volley", "baobhan_sith_curse_volley", 14.0, 180);
            case 4 -> new ServantCardSkillAction("Dance of the Fingertips A+", "baobhan_sith_fingertip_dance", 10.0, 300);
            case 5 -> new ServantCardSkillAction("Invitation to the Night Feast B", "baobhan_sith_night_feast", 15.0, 400);
            case 6 -> new ServantCardSkillAction("Grimalkin A", "baobhan_sith_grimalkin", 15.0, 600);
            case 7 -> new ServantCardSkillAction("Blessed Successor EX", "baobhan_sith_blessed_successor", 25.0, 700);
            case 8 -> new ServantCardSkillAction("Fairy Vampirism A", "baobhan_sith_fairy_vampirism", 10.0, 500);
            case 9 -> new ServantCardSkillAction("Fetch Failnaught", "baobhan_sith_fetch_failnaught", 35.0, 300);
            default -> null;
         };
         case "ushiwakamaru_rider" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("Tengu Strategy", "ushiwakamaru_tengu_strategy", 25.0, 400);
            case 1 -> new ServantCardSkillAction("Charisma", "ushiwakamaru_charisma", 15.0, 600);
            case 2 -> new ServantCardSkillAction("Moonlit Step", "ushiwakamaru_moonlit_step", 6.0, 140);
            case 3 -> new ServantCardSkillAction("Sweeping Thrust", "ushiwakamaru_sweeping_thrust", 8.0, 120);
            case 4 -> new ServantCardSkillAction("Kurama Style: Eagle Drop", "ushiwakamaru_eagle_drop", 15.0, 200);
            case 5 -> new ServantCardSkillAction("Jizaiten Eye: Six Secret Insight", "ushiwakamaru_six_secret", 50.0, 600);
            case 6 -> new ServantCardSkillAction("Usumidori: Heaven-Blade Step", "ushiwakamaru_usumidori", 50.0, 600);
            case 7 -> new ServantCardSkillAction("Benkei: Immovable Ground", "ushiwakamaru_benkei", 100.0, 600);
            case 8 -> new ServantCardSkillAction("Houmaru: Spider Slayer", "ushiwakamaru_spider_slayer", 50.0, 600);
            case 9 -> new ServantCardSkillAction("Dan-no-ura: Eight-Boat Leap", "ushiwakamaru_eight_boat", 225.0, 1200);
            default -> null;
         };
         case "arash" -> switch (slot) {
            case 0 -> new ServantCardSkillAction(crouching ? "Grand Arrow Rain" : "Arrow Rain", "arash_arrow_rain", 8.0, 160);
            case 1 -> new ServantCardSkillAction("Charged Arrow", "arash_energy_small", 8.0, 80);
            case 2 -> new ServantCardSkillAction("Heavy Charged Arrow", "arash_energy_large", 20.0, 240);
            case 3 -> new ServantCardSkillAction("Arrow Construction", "arash_arrow_creation", 1.0, 0);
            case 4 -> new ServantCardSkillAction("Emergency Retreat", "arash_retreat", 18.0, 140);
            case 5 -> new ServantCardSkillAction("Close-Range Knockback", "arash_melee_knockback", 12.0, 90);
            case 9 -> new ServantCardSkillAction("Stella", "arash_stella", 100.0, 3600);
            default -> null;
         };
         case "nightingale" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("Nursing of Steel A", "nightingale_steel_nursing", 15.0, 200);
            case 1 -> new ServantCardSkillAction("Angel's Cry EX", "nightingale_angel_cry", 10.0, 300);
            case 9 -> new ServantCardSkillAction("Nightingale Pledge", "nightingale_pledge", 50.0, 600);
            default -> null;
         };
         case "zhao_yun_rider" -> switch (slot) {
            case -1 -> new ServantCardSkillAction("Dragon Gall Spear Art", "zhao_yun_spear_combo", 0.0, 20);
            case 0 -> new ServantCardSkillAction("Summon Hakuryu", "zhao_yun_summon_hakuryu", 20.0, 0);
            case 1 -> new ServantCardSkillAction("涯角枪·破阵", "zhao_yun_spear_breakthrough", 16.0, 120);
            case 2 -> new ServantCardSkillAction("单骑救主 A", "zhao_yun_rescue", 30.0, 400);
            case 3 -> new ServantCardSkillAction("龙回首·横扫", "zhao_yun_dragon_sweep", 20.0, 160);
            case 4 -> new ServantCardSkillAction("白龙突进", "zhao_yun_mounted_rush", 18.0, 160);
            case 5 -> new ServantCardSkillAction("龙胆闪", "zhao_yun_dragon_flash", 22.0, 200);
            case 6 -> new ServantCardSkillAction("七探蛇盘", "zhao_yun_seven_probe", 28.0, 280);
            case 7 -> new ServantCardSkillAction("白龙踏阵", "zhao_yun_hakuryu_trample", 26.0, 320);
            case 8 -> new ServantCardSkillAction("青釭剑 C", "zhao_yun_qinggang", 50.0, 400);
            case 9 -> new ServantCardSkillAction("长坂坡一骑驱 EX", "zhao_yun_changbanpo", 150.0, 1200);
            default -> null;
         };
         case "senko_muramasa" -> switch (slot) {
            case 0 -> new ServantCardSkillAction("锻造", "muramasa_forge", 0.0, 0);
            case 1 -> new ServantCardSkillAction("阵地建造", "muramasa_workshop", 30.0, 1200);
            case 2 -> crouching
               ? new ServantCardSkillAction("muramasa_downward_slash", "muramasa_downward_slash", 8.0, 60)
               : new ServantCardSkillAction("试斩 B+", "muramasa_trial_slash", 10.0, 500);
            case 3 -> new ServantCardSkillAction("业之瞳 A", "muramasa_karma_eye", 15.0, 600);
            case 4 -> new ServantCardSkillAction("焰 EX", "muramasa_flame", 0.0, 600);
            case 5 -> crouching
               ? new ServantCardSkillAction("muramasa_thrust", "muramasa_thrust", 14.0, 100)
               : new ServantCardSkillAction("投影连刃", "muramasa_projection_volley", 25.0, 400);
            case 6 -> new ServantCardSkillAction("锻火淬身", "muramasa_temper", 20.0, 600);
            case 7 -> new ServantCardSkillAction("宿业断斩", "muramasa_karma_slash", 35.0, 500);
            case 8 -> crouching
               ? new ServantCardSkillAction("muramasa_spinning_slash", "muramasa_spinning_slash", 16.0, 140)
               : new ServantCardSkillAction("刀剑荒野·试制", "muramasa_sword_field", 40.0, 600);
            case 9 -> new ServantCardSkillAction("无元剑制", "muramasa_no_gen_kensai", 50.0, 3600);
            default -> null;
         };
         default -> null;
      };
   }
}

