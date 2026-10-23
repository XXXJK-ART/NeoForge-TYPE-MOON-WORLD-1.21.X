package net.xxxjk.TYPE_MOON_WORLD.servant.cu;

import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.CuRuneFireballEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModParticles;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.*;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

/** Server-owned actions, resources and status for both incarnations. */
public final class CuChulainnAbilityService {
   public static final String LAST_DAMAGE = "CuSharedLastDamage";
   public static final String NP_END = "CuSharedNpEnd";
   public static final String LOCK = "CuSharedLock", LOCK_END = "CuSharedLockEnd";
   private static final Map<LivingEntity,Cast> CASTS = new WeakHashMap<>();
   private static final String PREFIX="CuShared:";
   private static final class Cast {
      final int slot; final long start; final ResourceKey<Level> dimension;
      int processed=-1; UUID target; final Vec3 origin, forward;
      Cast(LivingEntity owner,int slot) {
         this.slot=slot; start=owner.level().getGameTime(); dimension=owner.level().dimension();
         origin=owner.getEyePosition().add(owner.getLookAngle().scale(1.2)); forward=owner.getLookAngle().multiply(1,0,1).normalize();
      }
   }
   private CuChulainnAbilityService() { }
   public static boolean isOwner(LivingEntity owner) {
      var definition=ServantIdentityHelper.definitionOf(owner);
      return definition!=null && "cu_chulainn".equals(definition.id());
   }
   public static boolean active(LivingEntity owner) { return CASTS.containsKey(owner); }
   public static InteractionHand spearHand(LivingEntity owner) {
      if (CuSpearLedger.outstanding(owner)) return null;
      for(var hand:InteractionHand.values()) if(owner.getItemInHand(hand).is(ModItems.GAE_BULG.get())) return hand;
      return null;
   }
   public static boolean canAct(LivingEntity owner) {
      return owner.isAlive() && !owner.isRemoved() && !ServantCombatSystem.cannotAct(owner)
         && !ServantCombatSystem.skillsSuppressed(owner) && !NoblePhantasmCastService.chanting(owner)
         && !owner.isPassenger() && !owner.isSleeping()
         && !net.xxxjk.TYPE_MOON_WORLD.chain.service.BindingService.isBound(owner.getUUID());
   }
   public static boolean enemy(LivingEntity owner,LivingEntity target) {
      return target!=null && target!=owner && target.isAlive() && target.level()==owner.level()
         && !owner.isAlliedTo(target) && !target.isAlliedTo(owner)
         && !ServantMasterTargeting.isContractMaster(owner,target) && !EntityUtils.isImmunePlayerTarget(target);
   }
   public static double mana(LivingEntity owner) {
      return owner instanceof ServantEntity npc ? npc.getCurrentMp()
         : owner instanceof ServerPlayer p ? p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().transformed()
            ? ServantCardManaService.availableForConsume(p,p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES))
            : p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).magicState().mana() : 0;
   }
   public static boolean pay(LivingEntity owner,double amount,boolean np) {
      if(amount<=0) return true;
      if(mana(owner)+1e-6<amount) return false;
      if(owner instanceof ServerPlayer p) return np ? net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper.consumeStrict(p,amount)
         : ServantCardManaService.consume(p,p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES),amount);
      if(owner instanceof ServantEntity npc) {
         npc.setCurrentMp(npc.getCurrentMp()-amount);
         if(np) NoblePhantasmCastService.noteNpcPayment(npc,amount);
         return true;
      }
      return false;
   }
   public static long cooldown(LivingEntity owner,int slot) {
      return ServantCardUnlimitedMode.isCooldownDisabled(owner) ? 0L : owner.getPersistentData().getLong(PREFIX+"cd:"+slot);
   }
   public static void cooldown(LivingEntity owner,int slot,int ticks) {
      String key=PREFIX+"cd:"+slot;
      if(ServantCardUnlimitedMode.isCooldownDisabled(owner)) {
         owner.getPersistentData().remove(key);
         if(owner instanceof ServerPlayer p && slot>=0 && slot<=9)
            ServantCardTransformManager.setSkillCooldownUntil(p,p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES),slot,owner.level().getGameTime());
         return;
      }
      owner.getPersistentData().putLong(key,owner.level().getGameTime()+ticks);
      if(owner instanceof ServerPlayer p && slot>=0 && slot<=9)
         ServantCardTransformManager.setSkillCooldownUntil(p,p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES),slot,owner.level().getGameTime()+ticks);
   }
   public static void npCooldown(LivingEntity owner,int ticks) {
      if(ServantCardUnlimitedMode.isCooldownDisabled(owner)) {
         owner.getPersistentData().remove(NP_END);
         if(owner instanceof ServerPlayer p) ServantCardTransformManager.setNoblePhantasmCooldown(p,p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES),0);
         return;
      }
      owner.getPersistentData().putLong(NP_END,owner.level().getGameTime()+ticks);
      if(owner instanceof ServerPlayer p) ServantCardTransformManager.setNoblePhantasmCooldown(p,p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES),ticks);
   }
   public static void clearCooldowns(ServerPlayer player) {
      var data=player.getPersistentData();
      data.remove(NP_END);
      for(int slot=-1;slot<=12;slot++) data.remove(PREFIX+"cd:"+slot);
   }
   public static boolean start(LivingEntity owner,int slot) {
      var spec=CuChulainnRules.skill(slot);
      if(slot==6 && owner.level() instanceof ServerLevel && isOwner(owner) && owner.isAlive()
         && owner.getPersistentData().getFloat(CuChulainnCombatHelper.RUNE_BARRIER_HP_TAG)>0) {
         clearBarrier(owner);
         Cast cast=CASTS.get(owner);if(cast!=null && cast.slot==6)CASTS.remove(owner);
         CuChulainnVisualService.action(owner,-100,0);
         return true;
      }
      if(spec==null || !(owner.level() instanceof ServerLevel) || !isOwner(owner) || !canAct(owner)
         || active(owner) || CuChulainnWeaponService.busy(owner) || cooldown(owner,slot)>owner.level().getGameTime()
         || spec.spear() && spearHand(owner)==null || slot==7 && !CuSpearLedger.recallable(owner)) return false;
      if(!pay(owner,spec.mana(),false)) return false;
      cooldown(owner,slot,spec.cooldown());
      Cast cast=new Cast(owner,slot); CASTS.put(owner,cast);
      int visual=owner instanceof CuChulainnEntity && owner.getRandom().nextBoolean()
         ? slot==8?CuChulainnRules.NPC_SWEEP:slot==9?CuChulainnRules.NPC_FLURRY:slot:slot;
      CuChulainnVisualService.action(owner,visual,spec.duration());
      if(slot>=0 && slot<=4 || slot==6)CuChulainnVisualService.runeCast(owner,slot);
      else if(slot==-1 || slot==8 || slot==9)CuChulainnVoiceService.attack(owner);
      var data=owner.getPersistentData(); long now=owner.level().getGameTime();
      switch(slot) {
         case -1 -> { owner.setDeltaMovement(owner.getDeltaMovement().add(cast.forward.scale(1.18)).add(0,.12,0)); owner.hurtMarked=true; }
         case 1 -> {
            owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,220,2));
            owner.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE,220,0));
            owner.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,400,0));
            owner.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,160,0));
         }
         case 2 -> {
            data.putLong(PREFIX+"tiwaz",now+300);
            modifier(owner,Attributes.ATTACK_DAMAGE,"attack",.30);
            modifier(owner,Attributes.MOVEMENT_SPEED,"speed",.22);
            modifier(owner,Attributes.ARMOR,"armor",.20);
         }
         case 3 -> data.putFloat(CuChulainnCombatHelper.ALGIZ_SHIELD_TAG,100);
         case 4 -> {
            DisengageService.cleanse(owner); owner.heal(10);
            data.putInt(PREFIX+"heals",9); data.putLong(PREFIX+"nextHeal",now+20);
            owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION,120,1));
         }
         case 5 -> DisengageService.apply(owner,"C");
         case 6 -> {
            data.putFloat(CuChulainnCombatHelper.RUNE_BARRIER_HP_TAG,2000);
            data.putLong(CuChulainnCombatHelper.RUNE_BARRIER_UNTIL_TAG,now+300);
            data.putDouble(CuChulainnCombatHelper.RUNE_BARRIER_X_TAG,owner.getX());
            data.putDouble(CuChulainnCombatHelper.RUNE_BARRIER_Y_TAG,owner.getY());
            data.putDouble(CuChulainnCombatHelper.RUNE_BARRIER_Z_TAG,owner.getZ());
            CuChulainnVisualService.barrier(owner,300,false);
         }
         case 7 -> CuSpearLedger.recall(owner);
         default -> { }
      }
      return true;
   }
   private static void modifier(LivingEntity owner,net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr,String name,double amount) {
      ServantCardSkillUtils.addOrReplaceMultiplied(owner.getAttribute(attr),ResourceLocation.fromNamespaceAndPath("typemoonworld","cu_shared_"+name),amount);
   }
   public static void tick(LivingEntity owner) {
      DisengageService.tick(owner);
      CuSpearLedger.deliverPending(owner);
      CuChulainnWeaponService.tick(owner);
      CuChulainnCombatAi.tickMelee(owner);
      if(!isOwner(owner)) { if(active(owner)) clear(owner); return; }
      var data=owner.getPersistentData(); long now=owner.level().getGameTime();
      data.putBoolean(CuChulainnCombatHelper.PROTECTION_FROM_ARROWS_TAG,true);
      if(owner instanceof ServerPlayer p && now%20==0) {
         var vars=p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         for(int slot=0;slot<=9;slot++)if(cooldown(owner,slot)>now) ServantCardTransformManager.setSkillCooldownUntil(p,vars,slot,cooldown(owner,slot));
         if(data.getLong(NP_END)>now) ServantCardTransformManager.setNoblePhantasmCooldown(p,vars,(int)Math.min(Integer.MAX_VALUE,data.getLong(NP_END)-now));
         vars.syncServantCardRuntime(p);
      }
      if(data.contains(LAST_DAMAGE) && now-data.getLong(LAST_DAMAGE)>=CuChulainnRules.RECOVERY_TICKS) {
         owner.setHealth(owner.getMaxHealth()); DisengageService.cleanse(owner);
         if(owner instanceof ServantEntity npc) npc.setCurrentMp(npc.getMaxMp());
         else if(owner instanceof ServerPlayer p) { var vars=p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES); vars.servantCardState().setMana(vars.servantCardState().maxMana()); vars.syncMana(p); }
         data.remove(LAST_DAMAGE);
      }
      if(data.contains(PREFIX+"tiwaz") && now>=data.getLong(PREFIX+"tiwaz")) clearTiwaz(owner);
      int heals=data.getInt(PREFIX+"heals");
      if(heals>0 && now>=data.getLong(PREFIX+"nextHeal")) {
         owner.heal(6); data.putInt(PREFIX+"heals",heals-1); data.putLong(PREFIX+"nextHeal",now+20);
      }
      if(data.getFloat(CuChulainnCombatHelper.RUNE_BARRIER_HP_TAG)>0) {
         if(now>=data.getLong(CuChulainnCombatHelper.RUNE_BARRIER_UNTIL_TAG)) clearBarrier(owner);
         else {
            owner.teleportTo(data.getDouble(CuChulainnCombatHelper.RUNE_BARRIER_X_TAG),data.getDouble(CuChulainnCombatHelper.RUNE_BARRIER_Y_TAG),data.getDouble(CuChulainnCombatHelper.RUNE_BARRIER_Z_TAG));
            owner.setDeltaMovement(Vec3.ZERO); owner.hurtMarked=true;
            if(owner instanceof ServantEntity npc) npc.getNavigation().stop();
            if(now%4==0) CuChulainnVisualService.edges(owner);
            if(now%20==0) CuChulainnVisualService.barrier(owner,(int)(data.getLong(CuChulainnCombatHelper.RUNE_BARRIER_UNTIL_TAG)-now),false);
         }
      }
      Cast cast=CASTS.get(owner); if(cast==null) return;
      if(!owner.isAlive() || !cast.dimension.equals(owner.level().dimension()) || ServantCombatSystem.cannotAct(owner)
         || ServantCombatSystem.skillsSuppressed(owner)) { CASTS.remove(owner); return; }
      int age=(int)(now-cast.start);
      if(cast.slot>=0 && cast.slot<=4 || cast.slot==6)CuChulainnVisualService.runePulse(owner,cast.slot,age);
      for(int frame=cast.processed+1;frame<=Math.min(age,CuChulainnRules.skill(cast.slot).duration());frame++) {
         if(cast.slot==0) {
            for(int i=0;i<5;i++) {
               Vec3 rune=runePosition(cast,i);
               if(frame==i*2) ((ServerLevel)owner.level()).sendParticles(ModParticles.ANSUZ_RUNE.get(),rune.x,rune.y,rune.z,1,0,0,0,0);
               if(frame>=i*2 && frame<12+i*2 && frame%2==0) ((ServerLevel)owner.level()).sendParticles(ParticleTypes.FLAME,rune.x,rune.y,rune.z,8,.16,.16,.16,.015);
               if(frame==12+i*2) fire(owner,rune);
            }
         } else if(cast.slot==-1 && frame==3) {
            if(spearHand(owner)!=null)hitArc(owner,cast.forward,4.2,24,3,false);
         } else if(cast.slot==8 && (frame==6 || frame==12)) {
            if(spearHand(owner)!=null) hitArc(owner,cast.forward,4,12,3,true);
         } else if(cast.slot==9 && (frame==4 || frame==8 || frame==12)) {
            if(frame==4) { var targets=targets(owner,cast.forward,5,true); if(!targets.isEmpty()) cast.target=targets.getFirst().getUUID(); }
            var target=cast.target==null ? null : ((ServerLevel)owner.level()).getEntity(cast.target);
            if(target instanceof LivingEntity living && enemy(owner,living) && owner.distanceTo(living)<=5 && owner.hasLineOfSight(living) && spearHand(owner)!=null) hit(owner,living,10,"cu_flurry",false);
         }
      }
      cast.processed=age;
      if(age>=CuChulainnRules.skill(cast.slot).duration()) CASTS.remove(owner);
   }
   private static Vec3 runePosition(Cast cast,int index) {
      double angle=Math.PI*index/4; Vec3 side=new Vec3(-cast.forward.z,0,cast.forward.x);
      return cast.origin.add(side.scale(Math.cos(angle)*1.6)).add(0,Math.sin(angle)*1.6-.4,0);
   }
   private static void fire(LivingEntity owner,Vec3 from) {
      var ball=ModEntities.CU_RUNE_FIREBALL.get().create(owner.level()); if(ball==null) return;
      ball.setOwner(owner); ball.setPos(from);
      LivingEntity target=CuChulainnWeaponService.locked(owner);
      if(!enemy(owner,target) || owner.distanceTo(target)>24 || !owner.hasLineOfSight(target)) target=aimTarget(owner,24);
      Vec3 direction=target==null ? owner.getLookAngle() : target.getBoundingBox().getCenter().subtract(from).normalize();
      ball.setDeltaMovement(direction); ((ServerLevel)owner.level()).addFreshEntity(ball);
   }
   public static LivingEntity aimTarget(LivingEntity owner,double range) {
      return targets(owner,owner.getLookAngle(),range,true).stream().min(Comparator.comparingDouble(e -> {
         Vec3 to=e.getBoundingBox().getCenter().subtract(owner.getEyePosition()).normalize(); return 1-to.dot(owner.getLookAngle());
      })).orElse(null);
   }
   public static List<LivingEntity> targets(LivingEntity owner,Vec3 forward,double range,boolean cone) {
      return owner.level().getEntitiesOfClass(LivingEntity.class,owner.getBoundingBox().inflate(range),e -> enemy(owner,e)
         && owner.distanceToSqr(e)<=range*range && owner.hasLineOfSight(e)
         && (!cone || e.position().subtract(owner.position()).normalize().dot(forward)>.65))
         .stream().sorted(Comparator.comparingDouble(owner::distanceToSqr)).toList();
   }
   public static List<LivingEntity> aimedTargets(LivingEntity owner,double range,double tolerance) {
      Vec3 start=owner.getEyePosition(), end=start.add(owner.getLookAngle().scale(range));
      AABB search=owner.getBoundingBox().expandTowards(end.subtract(start)).inflate(tolerance);
      return owner.level().getEntitiesOfClass(LivingEntity.class,search,e -> enemy(owner,e)
         && owner.distanceToSqr(e)<=range*range && owner.hasLineOfSight(e)
         && e.getBoundingBox().inflate(tolerance).clip(start,end).isPresent())
         .stream().sorted(Comparator.comparingDouble(owner::distanceToSqr)).toList();
   }
   private static void hitArc(LivingEntity owner,Vec3 direction,double range,float damage,int maximum,boolean sweep) {
      targets(owner,direction,range,!sweep).stream().limit(maximum).forEach(t -> hit(owner,t,damage,sweep?"cu_sweep":"cu_thrust",false));
      if(owner.level() instanceof ServerLevel level) level.sendParticles(ParticleTypes.SWEEP_ATTACK,owner.getX(),owner.getY()+1,owner.getZ(),1,0,0,0,0);
   }
   public static boolean hit(LivingEntity owner,LivingEntity target,float damage,String skill,boolean magic) {
      if(!enemy(owner,target)) return false;
      DamageSource base=magic ? owner.damageSources().indirectMagic(owner,owner)
         : owner instanceof net.minecraft.world.entity.player.Player p ? owner.damageSources().playerAttack(p) : owner.damageSources().mobAttack(owner);
      var source=new CombatDamageSource(base,AttackOrigin.skill("cu_chulainn",skill));
      DamageSource resolved=skill.equals("cu_gae_bolg") || skill.equals("cu_death_thorn")
         ? NoblePhantasmDamageClassifier.withNoblePhantasm(source,"cu_gae_bolg_thorn"):source;
      int previous=target.invulnerableTime; target.invulnerableTime=0;
      try { return target.hurt(resolved,damage); } finally { target.invulnerableTime=previous; }
   }
   public static void actualDamage(LivingEntity owner) {
      if(isOwner(owner)) owner.getPersistentData().putLong(LAST_DAMAGE,owner.level().getGameTime());
   }
   public static void clearBarrier(LivingEntity owner) {
      owner.getPersistentData().remove(CuChulainnCombatHelper.RUNE_BARRIER_HP_TAG);
      owner.getPersistentData().remove(CuChulainnCombatHelper.RUNE_BARRIER_UNTIL_TAG);
      CuChulainnVisualService.barrier(owner,0,false);
   }
   public static float absorb(LivingEntity owner,float amount) {
      var data=owner.getPersistentData();float remainder=amount;
      if(data.getLong(CuChulainnCombatHelper.RUNE_BARRIER_UNTIL_TAG)>owner.level().getGameTime()){
         float barrier=data.getFloat(CuChulainnCombatHelper.RUNE_BARRIER_HP_TAG);
         if(barrier>0){barrierHit(owner);float used=Math.min(barrier,remainder);remainder-=used;
            if(barrier<=used)clearBarrier(owner);else data.putFloat(CuChulainnCombatHelper.RUNE_BARRIER_HP_TAG,barrier-used);}
      }
      float shield=data.getFloat(CuChulainnCombatHelper.ALGIZ_SHIELD_TAG);
      if(shield>0){float used=Math.min(shield,remainder);remainder-=used;
         if(shield<=used)data.remove(CuChulainnCombatHelper.ALGIZ_SHIELD_TAG);else data.putFloat(CuChulainnCombatHelper.ALGIZ_SHIELD_TAG,shield-used);}
      return remainder;
   }
   public static void barrierHit(LivingEntity owner) { CuChulainnVisualService.barrier(owner,Math.max(0,(int)(owner.getPersistentData().getLong(CuChulainnCombatHelper.RUNE_BARRIER_UNTIL_TAG)-owner.level().getGameTime())),true); }
   private static void clearTiwaz(LivingEntity owner) {
      for(var entry:Map.of(Attributes.ATTACK_DAMAGE,"attack",Attributes.MOVEMENT_SPEED,"speed",Attributes.ARMOR,"armor").entrySet()) {
         var attr=owner.getAttribute(entry.getKey()); if(attr!=null) attr.removeModifier(ResourceLocation.fromNamespaceAndPath("typemoonworld","cu_shared_"+entry.getValue()));
      }
      owner.getPersistentData().remove(PREFIX+"tiwaz");
   }
   public static void clear(LivingEntity owner) {
      CuChulainnVisualService.voice(owner,CuChulainnRules.STOP_CHANT_VOICE,0);
      CuChulainnVisualService.action(owner,-100,0);
      CASTS.remove(owner); clearTiwaz(owner); clearBarrier(owner); DisengageService.clear(owner);
      var data=owner.getPersistentData(); data.remove(PREFIX+"heals"); data.remove(LAST_DAMAGE);
      data.remove(CuChulainnCombatHelper.ALGIZ_SHIELD_TAG); data.remove(CuChulainnCombatHelper.PROTECTION_FROM_ARROWS_TAG);
      CuChulainnWeaponService.clear(owner); CuSpearLedger.releaseForm(owner);
   }
}
