package net.xxxjk.TYPE_MOON_WORLD.advancement;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class TypeMoonAdvancementHelper {
   public static final String MAGIC_APPRENTICE = "magic_apprentice";
   public static final String PRESET_MAGIC = "preset_magic";
   public static final String OUT = "out";
   public static final String SPARKLING = "sparkling";
   public static final String ALL_GEMS = "all_gems";
   public static final String I_AM_THE_BONE_OF_MY_SWORD = "i_am_the_bone_of_my_sword";
   public static final String EX_CALIBUR = "ex_calibur";
   public static final String SWORD_IN_THE_STONE = "sword_in_the_stone";
   public static final String BERSERKER_IS_THE_STRONGEST = "berserker_is_the_strongest";
   public static final String WANDERER_OF_PARADISE = "wanderer_of_paradise";
   public static final String FLOWER_MAGICIAN = "flower_magician";
   public static final String SEPARATE_TRUTH_FROM_FALSEHOOD = "separate_truth_from_falsehood";
   public static final String EYES_OF_DEATH_PERCEPTION = "eyes_of_death_perception";

   private TypeMoonAdvancementHelper() {
   }

   public static void grant(ServerPlayer player, String idPath) {
      if (player == null || idPath == null || idPath.isEmpty() || player.getServer() == null) {
         return;
      }

      AdvancementHolder holder = player.getServer().getAdvancements().get(ResourceLocation.fromNamespaceAndPath("typemoonworld", idPath));
      if (holder == null) {
         return;
      }

      AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
      for (String criterion : progress.getRemainingCriteria()) {
         player.getAdvancements().award(holder, criterion);
      }
   }

   public static void grantNearby(ServerLevel level, Vec3 center, double radius, String idPath) {
      if (level == null || center == null || radius <= 0.0 || idPath == null || idPath.isEmpty()) {
         return;
      }

      double radiusSqr = radius * radius;
      for (ServerPlayer player : level.players()) {
         if (player.distanceToSqr(center) <= radiusSqr) {
            grant(player, idPath);
         }
      }
   }

   public static void syncPassive(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (player == null || vars == null) {
         return;
      }

      if (!vars.learned_magics.isEmpty()) {
         grant(player, MAGIC_APPRENTICE);
      }

      if (vars.player_magic_attributes_sword) {
         grant(player, I_AM_THE_BONE_OF_MY_SWORD);
      }

      if (vars.merlin_favor >= 5) {
         grant(player, FLOWER_MAGICIAN);
      }
   }

   public static void grantOutIfEligible(ServerPlayer player, LivingEntity target, int mode) {
      if (player != null && target == player && mode > 0 && player.isFallFlying()) {
         grant(player, OUT);
      }
   }
}
