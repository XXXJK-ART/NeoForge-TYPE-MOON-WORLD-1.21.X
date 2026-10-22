package net.xxxjk.TYPE_MOON_WORLD.servant.cu;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/** Compact server-side VFX hooks. Client animation layers may consume the action tag. */
public final class CuChulainnVisualService {
   private CuChulainnVisualService() { }
   public static void action(LivingEntity owner,int action,int duration) {
      owner.getPersistentData().putInt("CuAction",action);
      owner.getPersistentData().putLong("CuActionUntil",owner.level().getGameTime()+duration);
      var hand=CuChulainnAbilityService.spearHand(owner);
      var arm=hand==net.minecraft.world.InteractionHand.OFF_HAND?owner.getMainArm().getOpposite():owner.getMainArm();
      owner.getPersistentData().putLong("CuActionStart",owner.level().getGameTime());
      owner.getPersistentData().putBoolean("CuActionLeft",arm==net.minecraft.world.entity.HumanoidArm.LEFT);
      send(owner,new net.xxxjk.TYPE_MOON_WORLD.network.CuChulainnVisualMessage(owner.getUUID(),action,duration,owner.position(),false,owner.level().getGameTime(),arm==net.minecraft.world.entity.HumanoidArm.LEFT));
   }
   public static void barrier(LivingEntity owner,int ticks,boolean flash) {
      send(owner,new net.xxxjk.TYPE_MOON_WORLD.network.CuChulainnVisualMessage(owner.getUUID(),100,ticks,owner.position(),flash,owner.level().getGameTime(),false));
      if(flash && owner.level() instanceof ServerLevel level) level.sendParticles(ParticleTypes.WAX_ON,owner.getX(),owner.getY()+1,owner.getZ(),6,.7,.8,.7,.01);
   }
   public static void edges(LivingEntity owner) {
      if(!(owner.level() instanceof ServerLevel level))return;
      double x=owner.getX(),y=owner.getY(),z=owner.getZ();
      double half=CuChulainnRules.BARRIER_HALF_WIDTH,height=CuChulainnRules.BARRIER_HEIGHT;
      for(int i=0;i<=6;i++) {
         double t=i/6.0,off=-half+2*half*t;
         for(double edgeY:new double[]{y,y+height})for(int sign:new int[]{-1,1}) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,x+off,edgeY,z+sign*half,1,0,0,0,0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,x+sign*half,edgeY,z+off,1,0,0,0,0);
         }
         for(int a:new int[]{-1,1})for(int b:new int[]{-1,1})level.sendParticles(ParticleTypes.ELECTRIC_SPARK,x+a*half,y+height*t,z+b*half,1,0,0,0,0);
      }
   }
   private static void send(LivingEntity owner,net.xxxjk.TYPE_MOON_WORLD.network.CuChulainnVisualMessage message){
      if(!(owner.level() instanceof ServerLevel level))return;
      for(var player:level.players())if(player.distanceToSqr(owner)<=128*128)net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork.sendToPlayer(player,message);
   }
   public static void syncTo(LivingEntity owner,net.minecraft.server.level.ServerPlayer viewer) {
      var data=owner.getPersistentData();long now=owner.level().getGameTime();
      long start=data.getLong("CuActionStart"),end=data.getLong("CuActionUntil");
      if(end>now)net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork.sendToPlayer(viewer,new net.xxxjk.TYPE_MOON_WORLD.network.CuChulainnVisualMessage(owner.getUUID(),data.getInt("CuAction"),(int)(end-start),owner.position(),false,start,data.getBoolean("CuActionLeft")));
      long barrier=data.getLong(net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnCombatHelper.RUNE_BARRIER_UNTIL_TAG);
      if(barrier>now)net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork.sendToPlayer(viewer,new net.xxxjk.TYPE_MOON_WORLD.network.CuChulainnVisualMessage(owner.getUUID(),100,(int)(barrier-now),owner.position(),false,now,false));
   }
}
