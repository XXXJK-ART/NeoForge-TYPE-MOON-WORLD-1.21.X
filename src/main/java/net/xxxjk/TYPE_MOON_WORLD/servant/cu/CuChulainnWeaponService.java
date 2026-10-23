package net.xxxjk.TYPE_MOON_WORLD.servant.cu;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.*;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardUnlimitedMode;
import java.util.*;

/** Shared physical spear handoff. The projectile retains its original ItemStack. */
public final class CuChulainnWeaponService {
   private static final String LOCK_TARGET="CuSharedLockTarget", USE="CuSharedUse", START="CuSharedUseStart", CROUCH="CuSharedUseCrouch";
   private static final Map<LivingEntity,Leap> LEAPS=new WeakHashMap<>();
   private static final Map<LivingEntity,Pursuit> PURSUITS=new WeakHashMap<>();
   private static final Map<LivingEntity,Throw> THROWS=new WeakHashMap<>();
   private static final Map<LivingEntity,Followup> FOLLOWUPS=new WeakHashMap<>();
   private static final Map<LivingEntity,Thrust> THRUSTS=new WeakHashMap<>();
   private static final Map<LivingEntity,Long> CATCHES=new WeakHashMap<>();
   private record Throw(long releaseAt,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,UUID target,boolean army,float damage,Vec3 aim) { }
   private record Followup(long until,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,UUID target) { }
   private record Leap(long start,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,float damage,UUID target,Vec3 aim) { }
   private record Pursuit(long until,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,UUID target) { }
   private static final class Thrust {
      final long start;final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;final UUID target;boolean hit;
      Thrust(LivingEntity owner,UUID target){start=owner.level().getGameTime();dimension=owner.level().dimension();this.target=target;}
   }
   private CuChulainnWeaponService() { }
   public static boolean busy(LivingEntity owner){return LEAPS.containsKey(owner)||PURSUITS.containsKey(owner)||THROWS.containsKey(owner)||THRUSTS.containsKey(owner)||owner instanceof CuChulainnEntity && CATCHES.getOrDefault(owner,0L)>owner.level().getGameTime()||CuChulainnCombatAi.meleeActive(owner);}
   public static void caught(LivingEntity owner) {
      if(!owner.isAlive() || CuChulainnAbilityService.spearHand(owner)==null)return;
      CATCHES.put(owner,owner.level().getGameTime()+12);CuChulainnVisualService.action(owner,CuChulainnRules.RECALL_CATCH,12);
      if(owner instanceof ServantEntity npc)npc.getNavigation().stop();
      owner.level().playSound(null,owner.blockPosition(),SoundEvents.TRIDENT_RETURN,SoundSource.PLAYERS,.75F,1.25F);
   }
   public static boolean followupPending(LivingEntity owner){return FOLLOWUPS.containsKey(owner);}
   public static boolean npcRecallDue(LivingEntity owner) {
      var entry=CuSpearLedger.entry(owner);
      return owner instanceof CuChulainnEntity && entry!=null && entry.entity!=null && CuSpearLedger.recallable(owner)
         && owner.level() instanceof ServerLevel level && level.getEntity(entry.entity) instanceof net.xxxjk.TYPE_MOON_WORLD.entity.CuSpearEntity spear
         && spear.readyForNpcRecall();
   }
   public static void tick(LivingEntity owner) {
      if(CATCHES.getOrDefault(owner,0L)<=owner.level().getGameTime())CATCHES.remove(owner);
      var lock=locked(owner);
      if(lock!=null && owner instanceof ServerPlayer && owner.level().getGameTime()%20==0 && !owner.isUsingItem() && !NoblePhantasmCastService.chanting(owner)) {
         feedback(owner,"lock_status",lock.getDisplayName(),(owner.getPersistentData().getLong(CuChulainnAbilityService.LOCK_END)-owner.level().getGameTime()+19)/20);
      }
      if(owner instanceof ServerPlayer player && owner.getPersistentData().getBoolean(USE)
         && (!player.isUsingItem() || CuChulainnAbilityService.spearHand(owner)==null)) {
         owner.getPersistentData().remove(USE);CuChulainnVisualService.action(owner,-100,0);
      }
      if(NoblePhantasmCastService.chanting(owner) && "cu_gae_bolg".equals(owner.getPersistentData().getString("TypeMoonNpCastAction")))
         CuChulainnVisualService.charge(owner,1);
      Throw throwing=THROWS.get(owner);
      if(throwing!=null && (!throwing.dimension().equals(owner.level().dimension()) || (!throwing.army() && !CuChulainnAbilityService.canAct(owner))
         || !owner.isAlive() || owner.isRemoved() || CuChulainnAbilityService.spearHand(owner)==null)) {
         THROWS.remove(owner);CuChulainnVisualService.action(owner,-100,0);
      } else if(throwing!=null && owner.level().getGameTime()>=throwing.releaseAt()){
         THROWS.remove(owner);
         LivingEntity target=owner.level() instanceof ServerLevel level && throwing.target()!=null && level.getEntity(throwing.target()) instanceof LivingEntity t?t:null;
         if(throwing.army()) {
            if(!CuChulainnAbilityService.enemy(owner,target))target=null;
            CuSpearLedger.launch(owner,true,throwing.damage(),target,throwing.aim());
         } else if(throwing.target()==null || reachable(owner,target,CuChulainnRules.THROW_RANGE))CuSpearLedger.launch(owner,false,24,target);
         else CuChulainnVisualService.action(owner,-100,0);
      }
      Followup followup=FOLLOWUPS.get(owner);
      if(followup!=null && owner.level() instanceof ServerLevel level) {
         var entity=level.getEntity(followup.target());
         if(!(owner instanceof CuChulainnEntity) || !owner.isAlive() || owner.isRemoved() || !followup.dimension().equals(level.dimension())
            || level.getGameTime()>followup.until() || !(entity instanceof LivingEntity target) || !CuChulainnAbilityService.enemy(owner,target)
            || CuSpearLedger.outstanding(owner))FOLLOWUPS.remove(owner);
         else if(ordinaryThrow(owner,target))FOLLOWUPS.remove(owner);
      }
      Pursuit pursuit=PURSUITS.get(owner);
      if(pursuit!=null && owner.level() instanceof ServerLevel level) {
         var e=level.getEntity(pursuit.target());
         if(!(e instanceof LivingEntity target) || !CuChulainnAbilityService.enemy(owner,target)
            || !pursuit.dimension().equals(owner.level().dimension()) || !owner.isAlive()
            || owner.level().getGameTime()>pursuit.until() || CuChulainnAbilityService.spearHand(owner)==null) {
            PURSUITS.remove(owner);CuChulainnVisualService.action(owner,-100,0);
         } else if(owner.distanceTo(target)<=4 && owner.hasLineOfSight(target)) {
            if(owner instanceof CuChulainnEntity npc)npc.faceToward(target.position());
            THRUSTS.put(owner,new Thrust(owner,target.getUUID()));PURSUITS.remove(owner);
            CuChulainnVisualService.action(owner,CuChulainnRules.THORN_STRIKE,CuChulainnRules.THORN_DURATION);
            Vec3 forward=target.position().subtract(owner.position()).multiply(1,0,1).normalize();
            owner.setDeltaMovement(forward.scale(.75).add(0,owner.getDeltaMovement().y,0));owner.hurtMarked=true;
         }
         else {owner.setDeltaMovement(target.position().subtract(owner.position()).normalize().scale(1.2));owner.hurtMarked=true;}
      }
      Thrust thrust=THRUSTS.get(owner);
      if(thrust!=null && owner.level() instanceof ServerLevel level) {
         long age=level.getGameTime()-thrust.start;
         if(!owner.isAlive() || owner.isRemoved() || !thrust.dimension.equals(level.dimension()) || CuChulainnAbilityService.spearHand(owner)==null) {
            THRUSTS.remove(owner);CuChulainnVisualService.action(owner,-100,0);
         } else {
            if(age>=CuChulainnRules.THORN_HIT_TICK && !thrust.hit) {
               thrust.hit=true;
               var selected=level.getEntity(thrust.target);
               if(selected instanceof LivingEntity target && reachable(owner,target,CuChulainnRules.THORN_RANGE)) {
                  CuChulainnVisualService.thornImpact(owner,target);thornHit(owner,target);
               }
            }
            if(age>=CuChulainnRules.THORN_DURATION)THRUSTS.remove(owner);
         }
      }
      Leap leap=LEAPS.get(owner);
      if(leap!=null) {
         long age=owner.level().getGameTime()-leap.start();
         if(!owner.isAlive() || owner.isRemoved() || !leap.dimension().equals(owner.level().dimension()) || CuChulainnAbilityService.spearHand(owner)==null) {
            LEAPS.remove(owner);CuChulainnVisualService.action(owner,-100,0);
         }
         else if(age>=8) {
            // Takeoff commits the throw: a moved/dead target or later crowd control
            // cannot silently delete the paid spear before its five-frame release.
            CuChulainnVisualService.action(owner,11,16);
            THROWS.put(owner,new Throw(owner.level().getGameTime()+5,leap.dimension(),leap.target(),true,leap.damage(),leap.aim()));LEAPS.remove(owner);
         }
      }
      var data=owner.getPersistentData();
      if(data.getBoolean("CuLeapFall")) {
         owner.fallDistance=0;
         if(owner.onGround() && !LEAPS.containsKey(owner) && !THROWS.containsKey(owner)) data.remove("CuLeapFall");
      }
   }
   public static LivingEntity locked(LivingEntity owner) {
      var data=owner.getPersistentData();
      if(!data.hasUUID(LOCK_TARGET) || !(owner.level() instanceof ServerLevel level)) return null;
      var entity=level.getEntity(data.getUUID(LOCK_TARGET));
      if(owner.level().getGameTime()>=data.getLong(CuChulainnAbilityService.LOCK_END)
         || !level.dimension().location().toString().equals(data.getString("CuLockDimension"))
         || !(entity instanceof LivingEntity living) || !CuChulainnAbilityService.enemy(owner,living)) {
         data.remove(LOCK_TARGET);data.remove(CuChulainnAbilityService.LOCK_END);
         feedback(owner,"lock_lost");return null;
      }
      return (LivingEntity)entity;
   }
   public static void clear(LivingEntity owner) { var data=owner.getPersistentData();data.remove(LOCK_TARGET);data.remove(USE);data.remove("CuLeapFall");LEAPS.remove(owner);PURSUITS.remove(owner);THROWS.remove(owner);FOLLOWUPS.remove(owner);THRUSTS.remove(owner);CATCHES.remove(owner);CuChulainnCombatAi.clear(owner); }
   public static boolean lock(LivingEntity owner,LivingEntity target) {
      if(!CuChulainnAbilityService.enemy(owner,target) || owner.distanceTo(target)>4 || !owner.hasLineOfSight(target)) {
         feedback(owner,"lock_failed");return false;
      }
      owner.getPersistentData().putUUID(LOCK_TARGET,target.getUUID());
      owner.getPersistentData().putString("CuLockDimension",owner.level().dimension().location().toString());
      owner.getPersistentData().putLong(CuChulainnAbilityService.LOCK_END,owner.level().getGameTime()+CuChulainnRules.LOCK_TICKS);
      feedback(owner,"locked",target.getDisplayName());
      if(owner instanceof ServerPlayer player)player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP,SoundSource.PLAYERS,.45F,1.5F);
      CuChulainnVisualService.lock(owner,target);
      return true;
   }
   private static void feedback(LivingEntity owner,String key,Object... args) {
      if(owner instanceof ServerPlayer player)player.displayClientMessage(Component.translatable("message.typemoonworld.cu."+key,args),true);
   }
   public static boolean begin(ServerPlayer owner) {
      if(!CuChulainnAbilityService.canAct(owner) || CuChulainnAbilityService.active(owner) || CuChulainnAbilityService.spearHand(owner)==null || busy(owner))return false;
      var data=owner.getPersistentData();data.putBoolean(USE,true);data.putBoolean(CROUCH,owner.isShiftKeyDown());data.putLong(START,owner.level().getGameTime());return true;
   }
   public static void charge(ServerPlayer owner,int ticks) {
      int threshold=owner.getPersistentData().getBoolean(CROUCH)?40:20;
      if(!owner.getPersistentData().getBoolean(USE))return;
      if(ticks>=threshold) {
         var data=owner.getPersistentData();
         if(data.getInt("CuAction")!=10 || data.getLong("CuActionUntil")<=owner.level().getGameTime())CuChulainnVisualService.action(owner,10,72000);
         CuChulainnVisualService.charge(owner,Math.min(1,ticks/(data.getBoolean(CROUCH)?70.0:20)));
      }
   }
   public static boolean release(ServerPlayer owner,int ticks) {
      var data=owner.getPersistentData();if(!data.getBoolean(USE))return false;
      boolean crouch=data.getBoolean(CROUCH);data.remove(USE);
      CuChulainnVisualService.action(owner,-100,0);
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
      return ordinaryThrow(owner,null);
   }
   public static boolean ordinaryThrow(LivingEntity owner,LivingEntity target) {
      if(!(owner.level() instanceof ServerLevel) || !CuChulainnAbilityService.canAct(owner)
         || CuChulainnAbilityService.active(owner) || busy(owner) || CuChulainnAbilityService.cooldown(owner,12)>owner.level().getGameTime()
         || CuChulainnAbilityService.spearHand(owner)==null || target!=null && !reachable(owner,target,CuChulainnRules.THROW_RANGE))return false;
      if(owner instanceof CuChulainnEntity npc && target!=null)npc.faceToward(target.position());
      THROWS.put(owner,new Throw(owner.level().getGameTime()+5,owner.level().dimension(),target==null?null:target.getUUID(),false,24,null));
      CuChulainnVoiceService.attack(owner);
      CuChulainnAbilityService.cooldown(owner,12,20);CuChulainnVisualService.action(owner,11,16);return true;
   }
   public static boolean noble(LivingEntity owner,boolean flight,int ticks) {
      long now=owner.level().getGameTime();
      if(!CuChulainnAbilityService.canAct(owner) || CuChulainnAbilityService.active(owner) || CuChulainnAbilityService.spearHand(owner)==null
         || busy(owner))return false;
      if(!ServantCardUnlimitedMode.isCooldownDisabled(owner) && owner.getPersistentData().getLong(CuChulainnAbilityService.NP_END)>now) {
         feedback(owner,"np_cooldown");return false;
      }
      LivingEntity target=locked(owner);
      if(flight) {
         if(!canLeap(owner) || !owner.onGround() && owner.level().noCollision(owner,owner.getBoundingBox().move(0,-.6,0))) {
            feedback(owner,"flight_failed");return false;
         }
      }
      double range=flight?CuChulainnRules.THROW_RANGE:CuChulainnRules.THORN_RANGE;
      if(target==null) {
         var candidates=CuChulainnAbilityService.aimedTargets(owner,range,flight?1.25:.35);
         if(!candidates.isEmpty())target=candidates.get(owner.getRandom().nextInt(candidates.size()));
      }
      String resultKey=flight?"flight_failed":"thorn_failed";
      if(!reachable(owner,target,range)){feedback(owner,resultKey);return false;}
      double cost=flight?CuChulainnRules.flightPayment(ticks,CuChulainnAbilityService.mana(owner)):20;
      if(cost<=0 || !CuChulainnAbilityService.pay(owner,cost,true)){feedback(owner,resultKey);return false;}
      int cooldown=flight?1200:600;CuChulainnAbilityService.npCooldown(owner,cooldown);
      UUID targetId=target==null?null:target.getUUID();float damage=CuChulainnRules.flightDamage(cost);
      NoblePhantasmCastService.begin(owner,"cu_gae_bolg",cooldown,t->CuChulainnAbilityService.npCooldown(owner,t),()->{
         LEAPS.remove(owner);PURSUITS.remove(owner);CuChulainnVisualService.action(owner,-100,0);feedback(owner,resultKey);
         if(flight && owner instanceof CuChulainnEntity && NoblePhantasmCastService.interruptedByDamage(owner) && owner.isAlive())
            FOLLOWUPS.put(owner,new Followup(owner.level().getGameTime()+40,owner.level().dimension(),targetId));
      });
      CuChulainnVisualService.action(owner,10,(int)(NoblePhantasmCastService.releaseAt(owner)-now)+2);
      Runnable effect=()->{
         if(!owner.isAlive() || CuChulainnAbilityService.spearHand(owner)==null) {NoblePhantasmCastService.failBeforeEffect(owner);return;}
         var selected=((ServerLevel)owner.level()).getEntity(targetId);
         if(!(selected instanceof LivingEntity living) || !reachable(owner,living,range)) {
            NoblePhantasmCastService.failBeforeEffect(owner);return;
         }
         if(flight) {
            if(!canLeap(owner)) {
               NoblePhantasmCastService.failBeforeEffect(owner);return;
            }
            Vec3 move=owner.getDeltaMovement();owner.setDeltaMovement(move.x,1.2,move.z);owner.hurtMarked=true;
            owner.getPersistentData().putBoolean("CuLeapFall",true);
            CuChulainnVisualService.action(owner,10,20);
            LEAPS.put(owner,new Leap(owner.level().getGameTime(),owner.level().dimension(),damage,targetId,living.getBoundingBox().getCenter()));
         } else if(targetId!=null) {
            PURSUITS.put(owner,new Pursuit(owner.level().getGameTime()+40,owner.level().dimension(),targetId));
            CuChulainnVisualService.action(owner,CuChulainnRules.THORN_STRIKE,40);
         }
         feedback(owner,flight?"flight_success":"thorn_success");
      };
      if(!NoblePhantasmCastService.deferEffect(owner,effect))effect.run();return true;
   }
   private static boolean reachable(LivingEntity owner,LivingEntity target,double range) {
      return CuChulainnAbilityService.enemy(owner,target) && owner.distanceToSqr(target)<=range*range && owner.hasLineOfSight(target);
   }
   private static boolean canLeap(LivingEntity owner) {
      // A small step, knockback or a jump during the chant must not cancel the throw.
      return !owner.isInWaterOrBubble() && !owner.isInLava()
         && owner.level().noCollision(owner,owner.getBoundingBox().expandTowards(0,.25,0));
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
