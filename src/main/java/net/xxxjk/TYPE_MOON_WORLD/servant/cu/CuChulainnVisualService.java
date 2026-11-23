package net.xxxjk.TYPE_MOON_WORLD.servant.cu;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.xxxjk.TYPE_MOON_WORLD.init.ModParticles;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;

/** Compact server-side VFX hooks. Client animation layers may consume the action tag. */
public final class CuChulainnVisualService {
   private static final DustParticleOptions RED=new DustParticleOptions(new Vector3f(1,.025F,.075F),1.0F);
   private CuChulainnVisualService() { }
   public static void runeCast(LivingEntity owner,int slot) {
      if(!(owner.level() instanceof ServerLevel level))return;
      var rune=switch(slot){
         case 0 -> ModParticles.ANSUZ_RUNE.get();case 1 -> ModParticles.LAGUZ_RUNE.get();
         case 2 -> ModParticles.TIWAZ_RUNE.get();case 3 -> ModParticles.ALGIZ_RUNE.get();
         case 4 -> ModParticles.BERKANA_RUNE.get();default -> ModParticles.RUNE_BARRIER.get();
      };
      var forward=owner.getLookAngle().multiply(1,0,1).normalize();
      var side=new net.minecraft.world.phys.Vec3(-forward.z,0,forward.x);
      var center=owner.position().add(0,1.25,0).add(forward.scale(.85));
      // Ansuz already has five firing sigils; other runes only need three at cast start.
      if(slot!=0) {
         level.sendParticles(rune,center.x,center.y,center.z,1,0,0,0,0);
         for(int i=-1;i<=1;i+=2){var p=center.add(side.scale(i*.55)).add(0,-.28,0);level.sendParticles(rune,p.x,p.y,p.z,1,0,0,0,0);}
      }
      runeElements(level,center,slot,3);
      level.playSound(null,owner.blockPosition(),SoundEvents.EVOKER_PREPARE_SUMMON,SoundSource.PLAYERS,.5F,1.6F);
      level.playSound(null,owner.blockPosition(),SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.PLAYERS,.8F,slot==0?1.15F:1.4F);
   }
   public static void runePulse(LivingEntity owner,int slot,int age) {
      if(!(owner.level() instanceof ServerLevel level) || age%3!=0)return;
      var element=runeElement(slot);
      var forward=owner.getLookAngle().multiply(1,0,1).normalize();
      var side=new net.minecraft.world.phys.Vec3(-forward.z,0,forward.x);
      var center=owner.position().add(0,1.2,0).add(forward.scale(.95));
      for(int i=0;i<12;i++) {
         double angle=i*Math.PI/6+age*.09,radius=1.05+.12*Math.sin(age*.3);
         var p=center.add(side.scale(Math.cos(angle)*radius)).add(0,Math.sin(angle)*radius,0);
         level.sendParticles(element,p.x,p.y,p.z,1,0,0,0,0);
      }
      runeElements(level,center,slot,1);
   }
   private static net.minecraft.core.particles.SimpleParticleType runeElement(int slot) {
      return switch(slot) {
         case 0 -> ParticleTypes.FLAME;case 1 -> ParticleTypes.SPLASH;case 2 -> ParticleTypes.CRIT;
         case 3 -> ParticleTypes.END_ROD;case 4 -> ParticleTypes.HAPPY_VILLAGER;default -> ParticleTypes.ELECTRIC_SPARK;
      };
   }
   private static void runeElements(ServerLevel level,net.minecraft.world.phys.Vec3 center,int slot,int density) {
      var accent=switch(slot) {
         case 0 -> ParticleTypes.SMOKE;case 1 -> ParticleTypes.FALLING_WATER;case 2 -> ParticleTypes.ELECTRIC_SPARK;
         case 3 -> ParticleTypes.WAX_ON;case 4 -> ParticleTypes.COMPOSTER;default -> ParticleTypes.END_ROD;
      };
      level.sendParticles(runeElement(slot),center.x,center.y,center.z,16*density,.6,.65,.4,.035);
      level.sendParticles(accent,center.x,center.y,center.z,6*density,.55,.6,.35,.025);
   }
   public static void thornImpact(LivingEntity owner,LivingEntity target) {
      if(!(owner.level() instanceof ServerLevel level))return;
      var from=owner.getEyePosition();var end=target.getBoundingBox().getCenter();
      for(int i=0;i<=24;i++) {
         var p=from.lerp(end,i/24.0);level.sendParticles(RED,p.x,p.y,p.z,2,.045,.045,.045,0);
      }
      level.sendParticles(ParticleTypes.CRIT,end.x,end.y,end.z,36,.35,.45,.35,.18);
      level.sendParticles(RED,end.x,end.y,end.z,48,.45,.55,.45,.05);
      level.playSound(null,owner.blockPosition(),SoundEvents.TRIDENT_THROW.value(),SoundSource.PLAYERS,1.1F,.85F);
   }
   public static void voice(LivingEntity owner,int action,int ticks) {
      send(owner,new net.xxxjk.TYPE_MOON_WORLD.network.CuChulainnVisualMessage(owner.getUUID(),action,ticks,owner.position(),false,owner.level().getGameTime(),false));
   }
   public static void charge(LivingEntity owner,double power) {
      if(owner.level() instanceof ServerLevel level && level.getGameTime()%30==0)
         level.playSound(null,owner.blockPosition(),SoundEvents.AMETHYST_BLOCK_RESONATE,SoundSource.PLAYERS,.35F,(float)(.7+power*.45));
   }
   public static void lock(LivingEntity owner,LivingEntity target) {
      if(!(owner instanceof net.minecraft.server.level.ServerPlayer player) || !(owner.level() instanceof ServerLevel level))return;
      for(int i=0;i<12;i++) {
         double angle=i*Math.PI/6;
         level.sendParticles(player,ParticleTypes.ENCHANTED_HIT,true,target.getX()+Math.cos(angle)*.55,target.getY()+target.getBbHeight()*.65,target.getZ()+Math.sin(angle)*.55,1,0,0,0,0);
      }
   }
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
