package net.xxxjk.TYPE_MOON_WORLD.utils;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class MerlinWorldEventLimiter {
   public static final int DAILY_LIMIT = 5;
   private static final Map<MinecraftServer, MerlinWorldEventLimiter.BudgetState> BUDGETS = new WeakHashMap<>();

   private MerlinWorldEventLimiter() {
   }

   public static boolean tryConsume(ServerLevel level) {
      MinecraftServer server = level.getServer();
      if (server != null && server.overworld() != null) {
         MerlinWorldEventLimiter.BudgetState state = BUDGETS.computeIfAbsent(server, s -> new MerlinWorldEventLimiter.BudgetState());
         long day = server.overworld().getGameTime() / 24000L;
         if (state.day != day) {
            state.day = day;
            state.used = 0;
         }

         if (state.used >= 5) {
            return false;
         } else {
            state.used++;
            return true;
         }
      } else {
         return false;
      }
   }

   private static final class BudgetState {
      long day = Long.MIN_VALUE;
      int used = 0;
   }
}
