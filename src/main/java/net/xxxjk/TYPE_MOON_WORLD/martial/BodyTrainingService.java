package net.xxxjk.TYPE_MOON_WORLD.martial;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class BodyTrainingService {
   public static final int MAX_TOTAL_POINTS = 80;
   public static final int MAX_STAT_POINTS = 20;
   public static final int POINT_COST_STEP = 20;
   public static final double MAX_DAMAGE_REDUCTION = 0.8;
   public static final double MAX_STRENGTH_BONUS = 12.0;
   private static final ResourceLocation STRENGTH_ID = id("body_training_strength");
   private static final ResourceLocation SPEED_ID = id("body_training_speed");
   private static final ResourceLocation JUMP_ID = id("body_training_jump");
   private static final String SNAPSHOT_STORED = "Stored";

   private BodyTrainingService() {}

   public static int totalEarned(TypeMoonWorldModVariables.PlayerVariables vars) {
      return Mth.clamp(vars.body_training_points + allocatedPoints(vars), 0, MAX_TOTAL_POINTS);
   }

   public static int allocatedPoints(TypeMoonWorldModVariables.PlayerVariables vars) {
      return Mth.clamp(vars.body_strength, 0, MAX_STAT_POINTS)
         + Mth.clamp(vars.body_speed, 0, MAX_STAT_POINTS)
         + Mth.clamp(vars.body_resistance, 0, MAX_STAT_POINTS)
         + Mth.clamp(vars.body_technique, 0, MAX_STAT_POINTS);
   }

   public static int availablePointCapacity(TypeMoonWorldModVariables.PlayerVariables vars) {
      return Math.max(0, MAX_TOTAL_POINTS - allocatedPoints(vars));
   }

   public static int nextPointCost(TypeMoonWorldModVariables.PlayerVariables vars) {
      return pointCostForEarned(totalEarned(vars));
   }

   public static int pointCostForEarned(int earnedPoints) {
      int earned = Mth.clamp(earnedPoints, 0, MAX_TOTAL_POINTS);
      return earned >= MAX_TOTAL_POINTS ? 0 : (earned + 1) * POINT_COST_STEP;
   }

   public static void award(ServerPlayer player, int amount) {
      if (amount <= 0) return;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.servant_card_transformed || vars.master_card_active) return;
      int cost = nextPointCost(vars);
      if (cost <= 0) return;
      vars.body_training_xp += amount;
      boolean changed = false;
      while ((cost = nextPointCost(vars)) > 0 && vars.body_training_xp >= cost) {
         vars.body_training_xp -= cost;
         vars.body_training_points++;
         changed = true;
         player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.typemoonworld.body.point_gained"), true);
      }
      if (changed || player.tickCount % 20 == 0) vars.syncPlayerVariables(player);
   }

   public static boolean allocate(ServerPlayer player, String stat) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.body_training_points <= 0 || allocatedPoints(vars) >= MAX_TOTAL_POINTS) return false;
      boolean applied = switch (stat == null ? "" : stat) {
         case "strength" -> increment(() -> vars.body_strength, v -> vars.body_strength = v);
         case "speed" -> increment(() -> vars.body_speed, v -> vars.body_speed = v);
         case "resistance" -> increment(() -> vars.body_resistance, v -> vars.body_resistance = v);
         case "technique" -> increment(() -> vars.body_technique, v -> vars.body_technique = v);
         default -> false;
      };
      if (applied) {
         vars.body_training_points--;
         applyAttributes(player, vars);
         vars.syncPlayerVariables(player);
      }
      return applied;
   }

   private static boolean increment(IntGetter getter, IntSetter setter) {
      int current = getter.get();
      if (current >= MAX_STAT_POINTS) return false;
      setter.set(current + 1);
      return true;
   }

   public static double stagedPercent(int level) {
      int value = Mth.clamp(level, 0, MAX_STAT_POINTS);
      if (value <= 5) return value * 0.01;
      if (value <= 9) return 0.05 + (value - 5) * 0.02;
      return 0.18 + (value - 10) * 0.02;
   }

   public static double strengthBonus(int level) {
      int value = Mth.clamp(level, 0, MAX_STAT_POINTS);
      if (value <= 5) return value * 0.2;
      if (value <= 9) return 1.0 + (value - 5) * 0.4;
      return 3.6 + (value - 10) * ((MAX_STRENGTH_BONUS - 3.6) / 10.0);
   }

   public static double resistanceReduction(int level) {
      return Mth.clamp(stagedPercent(level), 0.0, MAX_DAMAGE_REDUCTION);
   }

   public static void clear(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (player == null || vars == null) return;
      clearValues(vars);
      applyAttributes(player, vars);
   }

   public static void stashForServantCard(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (player == null || vars == null) return;
      stashValues(vars);
      applyAttributes(player, vars);
   }

   public static boolean restoreFromServantCard(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (player == null || vars == null || !restoreValues(vars)) return false;
      applyAttributes(player, vars);
      return true;
   }

   static void stashValues(TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag saved = vars.servant_card_saved_body_training;
      if (saved == null || !saved.getBoolean(SNAPSHOT_STORED)) {
         vars.servant_card_saved_body_training = writeSnapshot(new BodyTrainingData(
            vars.body_training_xp, vars.body_training_points, vars.body_strength,
            vars.body_speed, vars.body_resistance, vars.body_technique
         ));
      }
      clearValues(vars);
   }

   static boolean restoreValues(TypeMoonWorldModVariables.PlayerVariables vars) {
      BodyTrainingData saved = readSnapshot(vars.servant_card_saved_body_training);
      if (saved == null) return false;
      vars.body_training_xp = saved.xp();
      vars.body_strength = saved.strength();
      vars.body_speed = saved.speed();
      vars.body_resistance = saved.resistance();
      vars.body_technique = saved.technique();
      vars.body_training_points = saved.points();
      vars.servant_card_saved_body_training = new CompoundTag();
      return true;
   }

   static CompoundTag writeSnapshot(BodyTrainingData values) {
      int strength = Mth.clamp(values.strength(), 0, MAX_STAT_POINTS);
      int speed = Mth.clamp(values.speed(), 0, MAX_STAT_POINTS);
      int resistance = Mth.clamp(values.resistance(), 0, MAX_STAT_POINTS);
      int technique = Mth.clamp(values.technique(), 0, MAX_STAT_POINTS);
      int allocated = strength + speed + resistance + technique;
      CompoundTag saved = new CompoundTag();
      saved.putBoolean(SNAPSHOT_STORED, true);
      saved.putInt("Xp", Math.max(0, values.xp()));
      saved.putInt("Points", Mth.clamp(values.points(), 0, Math.max(0, MAX_TOTAL_POINTS - allocated)));
      saved.putInt("Strength", strength);
      saved.putInt("Speed", speed);
      saved.putInt("Resistance", resistance);
      saved.putInt("Technique", technique);
      return saved;
   }

   static BodyTrainingData readSnapshot(CompoundTag saved) {
      if (saved == null || !saved.getBoolean(SNAPSHOT_STORED)) return null;
      return new BodyTrainingData(
         saved.getInt("Xp"), saved.getInt("Points"), saved.getInt("Strength"),
         saved.getInt("Speed"), saved.getInt("Resistance"), saved.getInt("Technique")
      ).normalized();
   }

   private static void clearValues(TypeMoonWorldModVariables.PlayerVariables vars) {
      vars.body_training_xp = 0;
      vars.body_training_points = 0;
      vars.body_strength = 0;
      vars.body_speed = 0;
      vars.body_resistance = 0;
      vars.body_technique = 0;
   }

   public static void applyAttributes(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      boolean cardTransformed = vars.servant_card_transformed || vars.master_card_active;
      int strength = cardTransformed ? 0 : vars.body_strength;
      int speed = cardTransformed ? 0 : vars.body_speed;
      int technique = cardTransformed ? 0 : vars.body_technique;
      update(player.getAttribute(Attributes.ATTACK_DAMAGE), STRENGTH_ID, strengthBonus(strength), AttributeModifier.Operation.ADD_VALUE);
      update(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, stagedPercent(speed), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      update(player.getAttribute(Attributes.JUMP_STRENGTH), JUMP_ID, stagedPercent(technique), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
   }

   private static void update(AttributeInstance attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
      if (attribute == null) return;
      AttributeModifier old = attribute.getModifier(id);
      if (old != null && Double.compare(old.amount(), amount) == 0 && old.operation() == operation) return;
      if (old != null) attribute.removeModifier(id);
      if (amount != 0.0) attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", path);
   }

   @FunctionalInterface private interface IntGetter { int get(); }
   @FunctionalInterface private interface IntSetter { void set(int value); }

   record BodyTrainingData(int xp, int points, int strength, int speed, int resistance, int technique) {
      BodyTrainingData normalized() {
         int normalizedStrength = Mth.clamp(this.strength, 0, MAX_STAT_POINTS);
         int normalizedSpeed = Mth.clamp(this.speed, 0, MAX_STAT_POINTS);
         int normalizedResistance = Mth.clamp(this.resistance, 0, MAX_STAT_POINTS);
         int normalizedTechnique = Mth.clamp(this.technique, 0, MAX_STAT_POINTS);
         int allocated = normalizedStrength + normalizedSpeed + normalizedResistance + normalizedTechnique;
         return new BodyTrainingData(
            Math.max(0, this.xp),
            Mth.clamp(this.points, 0, Math.max(0, MAX_TOTAL_POINTS - allocated)),
            normalizedStrength, normalizedSpeed, normalizedResistance, normalizedTechnique
         );
      }
   }
}
