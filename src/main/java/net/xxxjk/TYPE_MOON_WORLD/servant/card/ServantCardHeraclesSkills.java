package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantTrueSweepService;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantSprintCollisionHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.heracles.GodHandService;
import net.xxxjk.TYPE_MOON_WORLD.servant.heracles.HeraclesAbilityService;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactService;

/** Card adapter: shared services own revival, movement and all nine actions. */
public final class ServantCardHeraclesSkills {
   private ServantCardHeraclesSkills() {}
   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!"heracles".equals(vars.servant_card_id)) return;
      initializeHeraclesGodHand(player);
      if (syncGodHandLives(player,vars)) vars.syncServantCardRuntime(player);
      if (!HeraclesAbilityService.busy(player) && player.level() instanceof ServerLevel level)
         ServantSprintCollisionHelper.tryPlayerSprintCollision(player,level,player.getPersistentData(),"ServantCardHeraclesLastSprintCollisionBreak",false,10,1.25,.26,32,45);
   }
   public static void clear(ServerPlayer player) {
      HeraclesAbilityService.cancel(player);GodHandService.clear(player);
      var d=player.getPersistentData();
      for(String key:new java.util.ArrayList<>(d.getAllKeys())) if(key.startsWith("Heracles")||key.startsWith("ServantCardHeracles")) d.remove(key);
   }
   public static void initializeHeraclesGodHand(ServerPlayer player) { GodHandService.initialize(player); }
   private static boolean syncGodHandLives(ServerPlayer player,TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data=player.getPersistentData();
      int lives=Mth.clamp(data.getInt("GodHandLives") + 1,1,12);
      if(vars.servant_card_heracles_god_hand_lives==lives)return false;
      vars.servant_card_heracles_god_hand_lives=lives;return true;
   }
   public static void beginHeraclesAirborne(ServerPlayer player,boolean forced) { }
   public static void performBigJump(ServerPlayer player) { HeraclesAbilityService.start(player,0); }
   public static void performGroundSlam(ServerPlayer player,boolean heavy) { HeraclesAbilityService.start(player,1); }
   public static void showGodHandStatus(ServerPlayer player) { HeraclesAbilityService.status(player); }
   public static void performBasicSweep(ServerPlayer player) { ServantTrueSweepService.triggerPlayerAttack(player,player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES)); }
   public static void triggerHeraclesAttackImpact(ServerPlayer player,LivingEntity target) {
      var vars=player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if(!vars.servant_card_transformed||!"heracles".equals(vars.servant_card_id)||!(player.level() instanceof ServerLevel level))return;
      TerrainImpactService.impactGroundLayer(level,player,target.position(),TerrainImpactProfile.of(TerrainImpactProfile.Tier.SMALL),TerrainImpactService.Permission.PLAYER);
      level.sendParticles(ParticleTypes.CRIT,target.getX(),target.getY()+.5,target.getZ(),10,.45,.35,.45,.12);
   }
   public static void triggerHeraclesBlockAttack(ServerPlayer player,BlockPos pos) {
      if(player.level() instanceof ServerLevel level)TerrainImpactService.impactWallTunnel(level,player,player.position(),player.getLookAngle(),TerrainImpactProfile.of(TerrainImpactProfile.Tier.SMALL),3,3,(int)Math.ceil(player.getBbHeight()));
   }
}
