package net.xxxjk.TYPE_MOON_WORLD.magic.jewel;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;

public final class GemComboTracker {
   private static final String KEY_LAST_TYPE = "TypeMoonGemComboLastType";
   private static final String KEY_LAST_TICK = "TypeMoonGemComboLastTick";
   private static final String KEY_CHAIN_COUNT = "TypeMoonGemComboChainCount";
   private static final String KEY_CHAIN_MULTIPLIER_SUM = "TypeMoonGemComboMultiplierSum";
   private static final int WINDOW_TICKS = 40;

   private GemComboTracker() {
   }

   public static GemComboTracker.ComboState recordUse(ServerPlayer player, GemType gemType, double qualityMultiplier, boolean upwardRelease) {
      CompoundTag tag = player.getPersistentData();
      String lastType = tag.getString("TypeMoonGemComboLastType");
      long lastTick = tag.getLong("TypeMoonGemComboLastTick");
      int chain = tag.getInt("TypeMoonGemComboChainCount");
      double sumMultiplier = tag.contains("TypeMoonGemComboMultiplierSum") ? tag.getDouble("TypeMoonGemComboMultiplierSum") : 0.0;
      long now = player.level().getGameTime();
      boolean requireUpward = requiresUpwardRelease(gemType);
      if (requireUpward && !upwardRelease) {
         reset(player);
         return new GemComboTracker.ComboState(0, getThreshold(gemType), false, 0.0);
      } else {
         if (gemType.name().equals(lastType) && now - lastTick <= 40L) {
            chain++;
            sumMultiplier += qualityMultiplier;
         } else {
            chain = 1;
            sumMultiplier = qualityMultiplier;
         }

         int threshold = getThreshold(gemType);
         boolean triggered = chain >= threshold;
         if (triggered) {
            double avgMultiplier = chain <= 0 ? 1.0 : sumMultiplier / chain;
            reset(player);
            return new GemComboTracker.ComboState(0, threshold, true, avgMultiplier);
         } else {
            tag.putString("TypeMoonGemComboLastType", gemType.name());
            tag.putLong("TypeMoonGemComboLastTick", now);
            tag.putInt("TypeMoonGemComboChainCount", chain);
            tag.putDouble("TypeMoonGemComboMultiplierSum", sumMultiplier);
            return new GemComboTracker.ComboState(chain, threshold, false, 0.0);
         }
      }
   }

   public static void reset(ServerPlayer player) {
      CompoundTag tag = player.getPersistentData();
      tag.remove("TypeMoonGemComboLastType");
      tag.remove("TypeMoonGemComboLastTick");
      tag.remove("TypeMoonGemComboChainCount");
      tag.remove("TypeMoonGemComboMultiplierSum");
   }

   private static boolean requiresUpwardRelease(GemType gemType) {
      return gemType == GemType.EMERALD || gemType == GemType.SAPPHIRE || gemType == GemType.TOPAZ;
   }

   public static int getThreshold(GemType gemType) {
      return switch (gemType) {
         case EMERALD -> 2;
         case RUBY, SAPPHIRE, TOPAZ, WHITE_GEMSTONE, CYAN, BLACK_SHARD -> 3;
      };
   }

   public record ComboState(int chainCount, int threshold, boolean triggered, double averageMultiplier) {
   }
}
