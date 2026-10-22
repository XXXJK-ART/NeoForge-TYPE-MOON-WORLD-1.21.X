package net.xxxjk.TYPE_MOON_WORLD.servant.cu;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.*;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Shared physical spear handoff. The projectile retains its original ItemStack. */
public final class CuChulainnWeaponService {
   private static final String LOCK_TARGET="CuSharedLockTarget", USE="CuSharedUse", START="CuSharedUseStart", CROUCH="CuSharedUseCrouch";
   private static final Map<LivingEntity,Leap> LEAPS=new WeakHashMap<>();
   private static final Map<LivingEntity,Pursuit> PURSUITS=new WeakHashMap<>();
   private static final Map<LivingEntity,Long> THROWS=new WeakHashMap<>();
   private record Leap(long start,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,float damage,UUID target) { }
   private record Pursuit(long until,UUID target) { }
   private CuChulainnWeaponService() { }
   public static boolean busy(LivingEntity owner){return LEAPS.containsKey(owner)||PURSUITS.containsKey(owner)||THROWS.containsKey(owner);}
   public static void tick(LivingEntity owner) {
      locked(owner);
      Long throwAt=THROWS.get(owner);
      if(throwAt!=null && owner.level().getGameTime()>=throwAt){
         THROWS.remove(owner);
         if(owner.isAlive() && CuChulainnAbilityService.spearHand(owner)!=null)CuSpearLedger.launch(owner,false,24,null);
      }
      Pursuit pursuit=PURSUITS.get(owner);
      if(pursuit!=null && owner.level() instanceof ServerLevel level) {
         var e=level.getEntity(pursuit.target());
         if(!(e instanceof LivingEntity target) || !CuChulainnAbilityService.enemy(owner,target)
            || owner.level().getGameTime()>pursuit.until() || CuChulainnAbilityService.spearHand(owner)==null) PURSUITS.remove(owner);
         else if(owner.distanceTo(target)<=4 && owner.hasLineOfSight(target)) {thornHit(owner,target);PURSUITS.remove(owner);}
         else {owner.setDeltaMovement(target.position().subtract(owner.position()).normalize().scale(1.2));owner.hurtMarked=true;}
      }
      Leap leap=LEAPS.get(owner);
      if(leap!=null) {
         long age=owner.level().getGameTime()-leap.start();
         if(!owner.isAlive() || !leap.dimension().equals(owner.level().dimension()) || CuChulainnAbilityService.spearHand(owner)==null) LEAPS.remove(owner);
         else if(age>=20 || age>0 && (owner.verticalCollision || owner.getDeltaMovement().y<=0)) {
            LivingEntity target=owner.level() instanceof ServerLevel level && leap.target()!=null && level.getEntity(leap.target()) instanceof LivingEntity t ? t:null;
            CuChulainnVisualService.action(owner,11,16);CuSpearLedger.launch(owner,true,leap.damage(),target);LEAPS.remove(owner);
         }
      }
      var data=owner.getPersistentData();
      if(data.getBoolean("CuLeapFall")) {
         owner.fallDistance=0;
         if(owner.onGround() && !LEAPS.containsKey(owner)) data.remove("CuLeapFall");
      }
   }
   public static LivingEntity locked(LivingEntity owner) {
      var data=owner.getPersistentData();
      if(!data.hasUUID(LOCK_TARGET) || !(owner.level() instanceof ServerLevel level)) return null;
      var entity=level.getEntity(data.getUUID(LOCK_TARGET));
      if(owner.level().getGameTime()>=data.getLong(CuChulainnAbilityService.LOCK_END)
         || !level.dimension().location().toString().equals(data.getString("CuLockDimension"))
         || !(entity instanceof LivingEntity living) || !CuChulainnAbilityService.enemy(owner,living)) {
         data.remove(LOCK_TARGET);data.remove(CuChulainnAbilityService.LOCK_END);return null;
      }
      return (LivingEntity)entity;
   }
   public static void clear(LivingEntity owner) { var data=owner.getPersistentData();data.remove(LOCK_TARGET);data.remove(USE);data.remove("CuLeapFall");LEAPS.remove(owner);PURSUITS.remove(owner);THROWS.remove(owner); }
   public static boolean lock(LivingEntity owner,LivingEntity target) {
      if(!CuChulainnAbilityService.enemy(owner,target) || owner.distanceTo(target)>4 || !owner.hasLineOfSight(target)) return false;
      owner.getPersistentData().putUUID(LOCK_TARGET,target.getUUID());
      owner.getPersistentData().putString("CuLockDimension",owner.level().dimension().location().toString());
      owner.getPersistentData().putLong(CuChulainnAbilityService.LOCK_END,owner.level().getGameTime()+CuChulainnRules.LOCK_TICKS);
      return true;
   }
   public static boolean begin(ServerPlayer owner) {
      if(!CuChulainnAbilityService.canAct(owner) || CuChulainnAbilityService.active(owner) || CuChulainnAbilityService.spearHand(owner)==null || busy(owner))return false;
      var data=owner.getPersistentData();data.putBoolean(USE,true);data.putBoolean(CROUCH,owner.isShiftKeyDown());data.putLong(START,owner.level().getGameTime());return true;
   }
   public static void charge(ServerPlayer owner,int ticks) {
      int threshold=owner.getPersistentData().getBoolean(CROUCH)?40:20;
      if(owner.getPersistentData().getBoolean(USE) && ticks>=threshold && (ticks-threshold)%16==0)
         CuChulainnVisualService.action(owner,10,20);
   }
   public static boolean release(ServerPlayer owner,int ticks) {
      var data=owner.getPersistentData();if(!data.getBoolean(USE))return false;
      boolean crouch=data.getBoolean(CROUCH);data.remove(USE);
      if(!CuChulainnAbilityService.canAct(owner) || CuChulainnAbilityService.spearHand(owner)==null)return false;
      return switch(CuChulainnRules.input(crouch,ticks)) {
         case LOCK -> lock(owner,CuChulainnAbilityService.aimTarget(owner,4));
         case THROW -> ordinaryThrow(owner);
         case THORN -> noble(owner,false,20);
         case FLIGHT -> noble(owner,true,ticks);
      };
   }
   public static boolean release(ServerPlayer owner) {return release(owner,(int)(owner.level().getGameTime()-owner.getPersistentData().getLong(START)));}
   public static boolean ordinaryThrow(LivingEntity owner) {
      if(busy(owner) || CuChulainnAbilityService.cooldown(owner,12)>owner.level().getGameTime() || CuChulainnAbilityService.spearHand(owner)==null)return false;
      THROWS.put(owner,owner.level().getGameTime()+5);
      CuChulainnAbilityService.cooldown(owner,12,20);CuChulainnVisualService.action(owner,11,16);return true;
   }
   public static boolean noble(LivingEntity owner,boolean flight,int ticks) {
      long now=owner.level().getGameTime();
      if(!CuChulainnAbilityService.canAct(owner) || CuChulainnAbilityService.active(owner) || CuChulainnAbilityService.spearHand(owner)==null
         || busy(owner) || owner.getPersistentData().getLong(CuChulainnAbilityService.NP_END)>now)return false;
      LivingEntity target=locked(owner);
      if(flight) {
         if(!owner.onGround() || !owner.level().noCollision(owner,owner.getBoundingBox().move(0,.15,0)))return false;
         if(target==null || owner.distanceTo(target)>48 || !owner.hasLineOfSight(target))target=CuChulainnAbilityService.aimTarget(owner,48);
      } else {
         if(target==null)target=CuChulainnAbilityService.aimTarget(owner,4);
         if(!CuChulainnAbilityService.enemy(owner,target) || !CuChulainnRules.thornDistance(owner.distanceTo(target)) || !owner.hasLineOfSight(target))return false;
      }
      double cost=flight?CuChulainnRules.flightPayment(ticks,CuChulainnAbilityService.mana(owner)):20;
      if(cost<=0 || !CuChulainnAbilityService.pay(owner,cost,true))return false;
      int cooldown=flight?1200:600;CuChulainnAbilityService.npCooldown(owner,cooldown);
      UUID targetId=target==null?null:target.getUUID();float damage=CuChulainnRules.flightDamage(cost);
      NoblePhantasmCastService.begin(owner,"cu_gae_bolg",cooldown,t->CuChulainnAbilityService.npCooldown(owner,t),()->{LEAPS.remove(owner);PURSUITS.remove(owner);CuChulainnVisualService.action(owner,-100,0);});
      CuChulainnVisualService.action(owner,10,(int)(NoblePhantasmCastService.releaseAt(owner)-now)+2);
      Runnable effect=()->{
         if(!owner.isAlive() || CuChulainnAbilityService.spearHand(owner)==null) {NoblePhantasmCastService.failBeforeEffect(owner);return;}
         if(flight) {
            if(!owner.onGround() || !owner.level().noCollision(owner,owner.getBoundingBox().move(0,.15,0))) {
               NoblePhantasmCastService.failBeforeEffect(owner);return;
            }
            Vec3 move=owner.getDeltaMovement();owner.setDeltaMovement(move.x,1.2,move.z);owner.hurtMarked=true;
            owner.getPersistentData().putBoolean("CuLeapFall",true);
            CuChulainnVisualService.action(owner,10,20);
            LEAPS.put(owner,new Leap(owner.level().getGameTime(),owner.level().dimension(),damage,targetId));
         } else if(targetId!=null) PURSUITS.put(owner,new Pursuit(owner.level().getGameTime()+40,targetId));
      };
      if(!NoblePhantasmCastService.deferEffect(owner,effect))effect.run();return true;
   }
   private static void thornHit(LivingEntity owner,LivingEntity target) {
      if(ArtoriaPendragonCombatHelper.tryNegateCertainHitOrDeath(target,"gae_bolg") || ArtoriaPendragonCombatHelper.tryProtectWithAvalon(target))return;
      boolean death=!(target instanceof EmiyaArcherEntity) && !(target instanceof EnkiduEntity)
         && owner.getRandom().nextFloat()<CuChulainnCombatHelper.getDeathThornChance(target);
      var base=owner.damageSources().mobAttack(owner);
      if(net.xxxjk.TYPE_MOON_WORLD.servant.heracles.GodHandService.conceptHit(target,base,250,"cu_chulainn:gae_bolg",death))return;
      int lives=target.getPersistentData().getInt("GodHandLives");CuChulainnAbilityService.hit(owner,target,250,"cu_gae_bolg",false);
      if(death && target.isAlive() && !HeraclesGodHandHelper.consumedLifeAfterLethalHit(target,lives))
         CuChulainnAbilityService.hit(owner,target,Math.max(target.getMaxHealth()*2,500),"cu_death_thorn",false);
   }
}
