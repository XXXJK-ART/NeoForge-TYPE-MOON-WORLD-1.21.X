package net.xxxjk.TYPE_MOON_WORLD.magic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.magic.player.MercurySwordMagicAmplifier;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.passive.AdvancedPassiveService;

public final class WheelCastingModifierService {
   private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();

   private WheelCastingModifierService() {
   }

   public static Scope begin(ServerPlayer player, String magicId) {
      Scope scope = new Scope(player, magicId);
      CURRENT.set(scope);
      return scope;
   }

   public static double adjustManaCost(Player player, double baseCost) {
      if (baseCost <= 0.0 || !(player instanceof ServerPlayer serverPlayer)) return baseCost;
      Scope scope = CURRENT.get();
      if (scope == null || scope.player != serverPlayer) return baseCost;
      TypeMoonWorldModVariables.PlayerVariables vars = serverPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return Math.max(0.0, baseCost * AdvancedPassiveService.manaMultiplier(vars) * MercurySwordMagicAmplifier.manaCostMultiplier(serverPlayer));
   }

   public static int adjustChantTicks(Player player, int baseTicks) {
      if (baseTicks <= 1 || player == null) return baseTicks;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return Math.max(1, (int)Math.ceil(baseTicks * AdvancedPassiveService.chantMultiplier(vars)));
   }

   public static final class Scope implements AutoCloseable {
      private final ServerPlayer player;
      private final String magicId;
      private boolean closed;

      private Scope(ServerPlayer player, String magicId) {
         this.player = player;
         this.magicId = magicId;
      }

      public String magicId() {
         return this.magicId;
      }

      @Override
      public void close() {
         if (!this.closed) {
            this.closed = true;
            CURRENT.remove();
         }
      }
   }
}
