package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
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
         ServantSprintCollisionHelper.tryPlayerSprintCollision(player,level,player.getPersistentData(),"ServantCardHeraclesLastSprintCollisionBreak",false,10,1.25,.26,32,45,true);
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
   public static boolean triggerHeraclesBlockAttack(ServerPlayer player,BlockPos pos) {
      if(HeraclesAbilityService.busy(player)||!(player.level() instanceof ServerLevel level))return false;
      BlockState clicked=level.getBlockState(pos);
      if(clicked.isAir()||clicked.getCollisionShape(level,pos).isEmpty()||level.getBlockEntity(pos)!=null
         ||clicked.getDestroySpeed(level,pos)<0||clicked.getDestroySpeed(level,pos)>35
         ||!isInForwardBreakLane(player,pos))return false;
      var profile=new TerrainImpactProfile(TerrainImpactProfile.Tier.HEAVY,3.0,35,48,80);
      return TerrainImpactService.impactWallTunnel(level,player,player.position(),player.getLookAngle(),profile,3,3,(int)Math.ceil(player.getBbHeight()));
   }
   private static boolean isInForwardBreakLane(ServerPlayer player,BlockPos pos) {
      Vec3 forward=player.getLookAngle().multiply(1,0,1);
      if(forward.lengthSqr()<1.0E-4)return false;
      forward=forward.normalize();
      Vec3 offset=Vec3.atCenterOf(pos).subtract(player.position());
      double along=offset.dot(forward);
      double across=offset.subtract(forward.scale(along)).horizontalDistance();
      return along>=0&&along<=3&&across<=1.5&&offset.y>=-.1&&offset.y<player.getBbHeight();
   }
}
