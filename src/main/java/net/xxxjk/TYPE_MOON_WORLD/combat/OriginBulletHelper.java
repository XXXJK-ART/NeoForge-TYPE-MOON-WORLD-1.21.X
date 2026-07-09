package net.xxxjk.TYPE_MOON_WORLD.combat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.entity.ContenderBulletEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public final class OriginBulletHelper {
   public static final String NPC_SKILL_SEAL_TAG = "TypeMoonOriginBulletSkillSeal";

   private OriginBulletHelper() {
   }

   public static boolean isOriginBulletDamage(DamageSource source) {
      return source != null
         && source.getDirectEntity() instanceof ContenderBulletEntity bullet
         && bullet.isOriginBullet();
   }

   public static boolean isSealed(Player player) {
      if (player == null) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.origin_bullet_sealed;
   }

   public static void sealPlayerUntilDeath(Player player) {
      if (player == null) {
         return;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.origin_bullet_sealed = true;
      vars.is_magic_circuit_open = false;
      vars.magic_circuit_open_timer = 0.0;
      vars.player_mana = 0.0;
      vars.syncPlayerVariables(player);
   }

   public static void clearPlayerSeal(Player player) {
      if (player == null) {
         return;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.origin_bullet_sealed) {
         vars.origin_bullet_sealed = false;
         vars.syncPlayerVariables(player);
      }
   }

   public static void sealNpc(LivingEntity living) {
      if (living instanceof MysticMagicianEntity) {
         living.getPersistentData().putBoolean(NPC_SKILL_SEAL_TAG, true);
      }
   }

   public static boolean isNpcSealed(LivingEntity living) {
      return living instanceof MysticMagicianEntity && living.getPersistentData().getBoolean(NPC_SKILL_SEAL_TAG);
   }

   public static boolean isServantTarget(LivingEntity living) {
      if (living instanceof ServantEntity) {
         return true;
      }
      if (living instanceof Player player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return vars.servant_card_transformed;
      }
      return false;
   }

   public static double maxMpOf(LivingEntity living) {
      if (living instanceof ServantEntity servant) {
         return servant.getMaxMp();
      }
      if (living instanceof Player player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return vars.servant_card_transformed ? vars.servant_card_max_mana : 0.0;
      }
      return 0.0;
   }
}
