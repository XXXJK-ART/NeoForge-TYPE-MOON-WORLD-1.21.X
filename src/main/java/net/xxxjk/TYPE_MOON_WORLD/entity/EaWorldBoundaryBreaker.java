package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.ChantHandler;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.UBWInstanceManager;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardOdaNobunagaSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EmiyaArcherCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EmiyaArcherEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaEntity;
import net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions;

/** Server-only boundary collapse used by anti-world attacks such as EA. */
public final class EaWorldBoundaryBreaker {
   private EaWorldBoundaryBreaker() { }

   /**
    * Collapses the containing UBW/Hajun field and returns the caster after its
    * dimension transition. The returned object may be a replacement NPC entity.
    */
   public static LivingEntity breakFor(LivingEntity caster, ServerLevel source) {
      if (caster == null || source == null) return caster;
      if (UBWInstanceManager.isUbwDimension(source)) {
         collapseUbw(source);
      } else if (ModDimensions.isHajunDimension(source.dimension().location())) {
         collapseHajun(source);
      } else {
         return caster;
      }
      return findLiving(source.getServer(), caster.getUUID());
   }

   private static void collapseUbw(ServerLevel source) {
      UUID ownerId = UBWInstanceManager.getOwnerId(source.dimension());
      Entity owner = ownerId == null ? null : source.getEntity(ownerId);
      if (owner instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (vars.is_in_ubw) {
            ChantHandler.returnFromUBW(player, vars);
            return;
         }
      }
      if (owner instanceof EmiyaArcherEntity archer) {
         if (EmiyaArcherCombatHelper.breakUbwForEa(archer)) return;
      }
      // Older worlds did not persist the instance-owner map. Recover by state.
      for (ServerPlayer player : new ArrayList<>(source.players())) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (vars.is_in_ubw) {
            ChantHandler.returnFromUBW(player, vars);
            return;
         }
      }
      for (Entity candidate : snapshotEntities(source)) {
         if (candidate instanceof EmiyaArcherEntity archer && EmiyaArcherCombatHelper.breakUbwForEa(archer)) return;
      }
   }

   private static void collapseHajun(ServerLevel source) {
      for (ServerPlayer player : new ArrayList<>(source.players())) {
         if (ServantCardOdaNobunagaSkills.returnFromOdaHajunField(player)) return;
      }
      for (Entity candidate : snapshotEntities(source)) {
         if (candidate instanceof OdaNobunagaEntity oda && OdaNobunagaCombatHelper.breakHajunForEa(oda)) return;
      }
   }

   private static List<Entity> snapshotEntities(ServerLevel level) {
      List<Entity> entities = new ArrayList<>();
      level.getEntities().getAll().forEach(entities::add);
      return entities;
   }

   private static LivingEntity findLiving(net.minecraft.server.MinecraftServer server, UUID id) {
      if (server == null || id == null) return null;
      ServerPlayer player = server.getPlayerList().getPlayer(id);
      if (player != null) return player;
      for (ServerLevel level : server.getAllLevels()) {
         Entity entity = level.getEntity(id);
         if (entity instanceof LivingEntity living) return living;
      }
      return null;
   }
}
