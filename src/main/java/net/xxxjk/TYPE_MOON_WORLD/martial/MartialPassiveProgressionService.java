package net.xxxjk.TYPE_MOON_WORLD.martial;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveRank;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveService;

public final class MartialPassiveProgressionService {
   public static final int FIRST_THRESHOLD = 150;

   private MartialPassiveProgressionService() {
   }

   public static double total(TypeMoonWorldModVariables.PlayerVariables vars) {
      return vars.bajiquan_proficiency + vars.ganryu_proficiency + vars.hokushin_proficiency + vars.tennen_proficiency;
   }

   public static double highest(TypeMoonWorldModVariables.PlayerVariables vars) {
      return Math.max(Math.max(vars.bajiquan_proficiency, vars.ganryu_proficiency), Math.max(vars.hokushin_proficiency, vars.tennen_proficiency));
   }

   public static double chancePercent(double total, double highest) {
      return Math.max(0.0, Math.min(100.0, (total + highest) / 10.0));
   }

   public static void afterNaturalGain(ServerPlayer player, double totalBefore) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double totalAfter = total(vars);
      if (totalAfter <= totalBefore || totalAfter < FIRST_THRESHOLD) return;
      int reached = (int)Math.floor(totalAfter / 10.0) * 10;
      int next = Math.max(FIRST_THRESHOLD, vars.martial_passive_last_threshold + 10);
      boolean changed = false;
      while (next <= reached) {
         vars.martial_passive_last_threshold = next;
         PassiveRank current = PassiveService.rank(vars, PassiveService.MIND_EYE_TRUE);
         if (current != PassiveRank.A && player.getRandom().nextDouble() * 100.0 < chancePercent(totalAfter, highest(vars))) {
            PassiveRank promoted = current == null ? PassiveRank.E : current.next();
            vars.passive_ranks.put(PassiveService.MIND_EYE_TRUE, promoted);
            player.displayClientMessage(Component.translatable("message.typemoonworld.passive.mind_eye_true.awakened", promoted.name()), false);
            changed = true;
         }
         next += 10;
      }
      if (changed) vars.syncPlayerVariables(player);
   }
}
