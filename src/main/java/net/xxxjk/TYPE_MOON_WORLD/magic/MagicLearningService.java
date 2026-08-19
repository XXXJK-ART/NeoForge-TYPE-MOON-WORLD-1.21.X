package net.xxxjk.TYPE_MOON_WORLD.magic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.talent.TalentService;

public final class MagicLearningService {
   private MagicLearningService() {}

   public static boolean learnFromMaterial(ServerPlayer player, String id, double random) {
      if (TalentService.isTalent(id)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.talent.acquisition_restricted"), true);
         return false;
      }
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!MagicLearningStrategy.materialAllowed(vars, id)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.learning_restricted"), true);
         return false;
      }
      if (vars.learned_magics.contains(id)) return false;
      double chance = MagicLearningStrategy.learningChance(id, MagicProficiencyService.get(vars, "magic_analysis"));
      boolean success = random < chance;
      if (success) {
         grantFromMaterial(player, id);
      } else {
         player.displayClientMessage(Component.translatable("message.typemoonworld.scroll.learn_failed"), true);
         player.playNotifySound(SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
      }
      return success;
   }

   /** Grants another entry from a multi-magic teaching item after its shared roll succeeds. */
   public static boolean grantFromMaterial(ServerPlayer player, String id) {
      if (TalentService.isTalent(id)) return false;
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!MagicLearningStrategy.materialAllowed(vars, id) || vars.learned_magics.contains(id)) return false;
      vars.learned_magics.add(id);
      applyImmediateGrantBonuses(vars, id);
      awardAnalysisKnowledge(vars, id);
      if (MagicLearningStrategy.canAnalyze(id)) unlockAnalysisChance(player, vars);
      player.displayClientMessage(Component.translatable("message.typemoonworld.magic.learned", Component.translatable("magic.typemoonworld." + id + ".name")), true);
      player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
      vars.syncPlayerVariables(player);
      return true;
   }

   public static void unlockAnalysisChance(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars.learned_magics.contains("magic_analysis")) return;
      int count = vars.learned_magics.size();
      double chance = Math.min(1.0, count * 0.20);
      if (player.getRandom().nextDouble() < chance) {
         vars.learned_magics.add("magic_analysis");
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.analysis_unlocked"), true);
      }
   }

   public static void awardAnalysisKnowledge(TypeMoonWorldModVariables.PlayerVariables vars, String learnedId) {
      if ("magic_analysis".equals(learnedId)) return;
      int verses = MagicLearningStrategy.verses(learnedId);
      double current = MagicProficiencyService.get(vars, "magic_analysis");
      if (verses == 1 && current >= 20.0 || verses == 2 && current >= 30.0 || verses == 3 && current >= 50.0) return;
      MagicProficiencyService.add(vars, "magic_analysis", Math.max(0.5, MagicLearningStrategy.complexity(learnedId) / 12.0));
   }

   public static void applyImmediateGrantBonuses(TypeMoonWorldModVariables.PlayerVariables vars, String learnedId) {
      if ("theology".equals(learnedId)) {
         MagicProficiencyService.set(vars, "theology", 100.0);
      }
   }
}
