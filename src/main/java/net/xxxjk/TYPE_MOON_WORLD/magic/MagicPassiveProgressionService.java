package net.xxxjk.TYPE_MOON_WORLD.magic;

import java.util.HashSet;
import java.util.Set;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicDisplayMetadata;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveRank;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveService;
import net.xxxjk.TYPE_MOON_WORLD.talent.TalentService;

public final class MagicPassiveProgressionService {
   private static final int BASE_THRESHOLD = 290;
   private static final int FIRST_ROLL = 300;
   private static final int MAX_THRESHOLD = 10000;

   private MagicPassiveProgressionService() {
   }

   public static void rebase(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars == null) return;
      vars.magic_passive_last_threshold = currentBand(totalMagicProficiency(vars));
   }

   public static void onNaturalGain(TypeMoonWorldModVariables.PlayerVariables vars, String changedMagicId) {
      if (vars == null || changedMagicId == null || TalentService.isTalent(changedMagicId) || MagicDisplayMetadata.isMartialMagic(changedMagicId)) {
         return;
      }
      PassiveRank current = PassiveService.rank(vars, PassiveService.HIGH_SPEED_INCANTATION);
      if (current == PassiveRank.A) {
         rebase(vars);
         return;
      }
      double total = totalMagicProficiency(vars);
      int band = currentBand(total);
      int last = Math.max(BASE_THRESHOLD, vars.magic_passive_last_threshold);
      if (band < FIRST_ROLL || band <= last) return;
      for (int threshold = Math.max(FIRST_ROLL, last + 10); threshold <= band; threshold += 10) {
         current = PassiveService.rank(vars, PassiveService.HIGH_SPEED_INCANTATION);
         if (current == PassiveRank.A) {
            break;
         }
         double chance = chancePercent(total) / 100.0;
         if (Math.random() < chance) {
            vars.passive_ranks.put(PassiveService.HIGH_SPEED_INCANTATION, current == null ? PassiveRank.E : current.next());
         }
      }
      vars.magic_passive_last_threshold = band;
   }

   public static double chancePercent(double totalMagicProficiency) {
      return Math.max(0.0, Math.min(100.0, totalMagicProficiency / 10.0));
   }

   public static double totalMagicProficiency(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars == null) return 0.0;
      Set<String> ids = new HashSet<>();
      ids.addAll(vars.learned_magics);
      ids.addAll(vars.magic_proficiencies.keySet());
      ids.add("magic_analysis");
      ids.add("structural_analysis");
      ids.add("projection");
      ids.add("reinforcement");
      ids.add("jewel_magic_shoot");
      ids.add("jewel_magic_release");
      ids.add("unlimited_blade_works");
      ids.add("sword_barrel_full_open");
      ids.add("gravity_magic");
      ids.add("gander");
      ids.add("healing_magic");
      ids.add("magic_bullet");
      ids.add("suggestion_magic");
      ids.add("binding_magic");
      ids.add("fire_magic");
      ids.add("water_magic");
      ids.add("wind_magic");
      ids.add("earth_magic");
      ids.add("time_alter");
      ids.add("spiritual_healing");
      ids.add("baptism_rite");
      double total = 0.0;
      Set<String> normalized = new HashSet<>();
      for (String id : ids) {
         String canonical = MagicLearningStrategy.normalizeDisplayId(id);
         if (canonical == null || canonical.isBlank()
            || TalentService.isTalent(canonical)
            || MagicDisplayMetadata.isMartialMagic(canonical)
            || !normalized.add(canonical)) {
            continue;
         }
         total += MagicProficiencyService.getRaw(vars, canonical);
      }
      return total;
   }

   private static int currentBand(double total) {
      if (total < FIRST_ROLL) return BASE_THRESHOLD;
      return Math.min(MAX_THRESHOLD, (int)Math.floor(total / 10.0) * 10);
   }
}
