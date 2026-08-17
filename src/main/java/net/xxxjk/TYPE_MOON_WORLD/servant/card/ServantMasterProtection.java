package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GawainEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.IskandarEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LancelotBerserkerEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.IskandarMountEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MacedonianSoldierEntity;

/** Prevents incidental heavy-servant damage to that servant's contracted master. */
public final class ServantMasterProtection {
   private ServantMasterProtection() {
   }

   public static boolean isProtectedMaster(LivingEntity attacker, LivingEntity target) {
      if (!(target instanceof ServerPlayer master) || attacker == null) return false;
      if (attacker instanceof ServantEntity servant) {
         return isHeavyServant(servant) && servant.isBoundTo(master);
      }
      if (attacker instanceof IskandarMountEntity mount) {
         return mount.isBoundToMaster(master);
      }
      if (attacker instanceof MacedonianSoldierEntity soldier
         && soldier.level() instanceof net.minecraft.server.level.ServerLevel level) {
         if (soldier.getPersistentData().hasUUID("IonioiHetairoiOwner")) {
            Entity owner = level.getEntity(soldier.getPersistentData().getUUID("IonioiHetairoiOwner"));
            if (owner instanceof LivingEntity living && isProtectedMaster(living, target)) {
               return true;
            }
         }
         if (soldier.getPersistentData().hasUUID("ServantCardIskandarOwner")) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(
               soldier.getPersistentData().getUUID("ServantCardIskandarOwner"));
            return owner != null && isProtectedMaster(owner, target);
         }
      }
      if (!(attacker instanceof ServerPlayer servantPlayer)) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = servantPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || !isHeavyServantId(vars.servant_card_id)) return false;
      return MasterServantLinkService.getLinkedMaster(servantPlayer, vars) == master;
   }

   /** Covers routine melee, collision, stomp and projectile damage owned by a protected heavy servant. */
   public static boolean isProtectedMasterDamage(DamageSource source, LivingEntity target) {
      if (source == null || !(target instanceof ServerPlayer)) return false;
      if (isProtectedAttacker(source.getEntity(), target) || isProtectedAttacker(source.getDirectEntity(), target)) {
         return true;
      }
      Entity direct = source.getDirectEntity();
      return direct instanceof Projectile projectile && isProtectedAttacker(projectile.getOwner(), target);
   }

   private static boolean isProtectedAttacker(Entity candidate, LivingEntity target) {
      return candidate instanceof LivingEntity living && isProtectedMaster(living, target);
   }

   private static boolean isHeavyServant(ServantEntity servant) {
      return servant instanceof HeraclesEntity
         || servant instanceof GawainEntity
         || servant instanceof LancelotBerserkerEntity
         || servant instanceof IskandarEntity;
   }

   private static boolean isHeavyServantId(String servantId) {
      return "heracles".equals(servantId)
         || "gawain".equals(servantId)
         || "lancelot_berserker".equals(servantId)
         || "iskandar".equals(servantId);
   }
}
