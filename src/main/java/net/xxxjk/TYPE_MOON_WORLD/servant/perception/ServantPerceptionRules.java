package net.xxxjk.TYPE_MOON_WORLD.servant.perception;

import java.util.Collection;
import java.util.Locale;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

public final class ServantPerceptionRules {
   public static final int BRIGHT_TICKS = 20;
   public static final int DARK_TICKS = 20;
   public static final int SCAN_INTERVAL = 10;
   public static final double BASE_RADIUS = 10.0;

   private ServantPerceptionRules() { }

   public enum RelationColor {
      PLAYER(255, 220, 32), HOSTILE(255, 48, 48), FRIENDLY(48, 128, 255), NEUTRAL(245, 245, 245);
      private final int red, green, blue;
      RelationColor(int red, int green, int blue) { this.red = red; this.green = green; this.blue = blue; }
      public int red() { return red; }
      public int green() { return green; }
      public int blue() { return blue; }
   }

   public static boolean isPerceivableTarget(Entity entity) {
      return entity != null && entity.isAlive() && !(entity instanceof Animal) && !(entity instanceof Villager);
   }

   public static RelationColor color(Entity observer, Entity target) {
      if (target instanceof Player) return RelationColor.PLAYER;
      if (observer != null && (observer.isAlliedTo(target) || target.isAlliedTo(observer))) return RelationColor.FRIENDLY;
      if (target instanceof Monster || target instanceof Mob mob && mob.getTarget() == observer) return RelationColor.HOSTILE;
      return RelationColor.NEUTRAL;
   }

   public static double radius(Collection<String> skills) {
      return radius(skills, highestRank(skills, "presence_concealment_") > 0);
   }

   public static double radius(Collection<String> skills, boolean concealmentActive) {
      int concealment = highestRank(skills, "presence_concealment_");
      if (concealmentActive && concealment == 6) return 0.0;
      if (concealmentActive && concealment > 0) return new double[]{0.0, 9.0, 8.0, 6.0, 4.0, 3.0}[concealment];
      double baseRadius = concealment > 0 ? new double[]{0.0, 9.0, 8.0, 6.0, 4.0, 3.0}[concealment] : BASE_RADIUS;
      if (!concealmentActive) baseRadius = BASE_RADIUS;
      int observer = Math.max(highestRank(skills, "instinct_"), Math.max(highestRank(skills, "true_mind_eye_"),
         Math.max(highestRank(skills, "false_mind_eye_"), Math.max(highestRank(skills, "mindseye_"), highestRank(skills, "clairvoyance_")))));
      return baseRadius * observerMultiplier(observer);
   }

   public static double observerMultiplier(int rank) {
      return switch (Math.max(0, Math.min(6, rank))) {
         case 1 -> 1.2;
         case 2 -> 1.4;
         case 3 -> 1.6;
         case 4 -> 1.8;
         case 5, 6 -> 2.0;
         default -> 1.0;
      };
   }

   public static int highestRank(Collection<String> skills, String prefix) {
      if (skills == null || prefix == null) return 0;
      int rank = 0;
      for (String skill : skills) {
         if (skill == null) continue;
         String key = skill.substring(skill.indexOf(':') + 1).toLowerCase(Locale.ROOT);
         int start = key.startsWith(prefix) ? 0 : key.indexOf("_" + prefix);
         if (start >= 0) rank = Math.max(rank, rank(key.substring(start + (start == 0 ? 0 : 1) + prefix.length())));
      }
      return rank;
   }

   private static int rank(String value) {
      if (value.startsWith("ex")) return 6;
      if (value.isEmpty()) return 0;
      return switch (value.charAt(0)) { case 'e' -> 1; case 'd' -> 2; case 'c' -> 3; case 'b' -> 4; case 'a' -> 5; default -> 0; };
   }
}
