package net.xxxjk.TYPE_MOON_WORLD.servant.cu;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnEntity;

public final class CuChulainnCombatAi {
   private static final Map<LivingEntity,Melee> MELEE=new WeakHashMap<>();
   private static final class Melee {
      final long start; final UUID target; final ResourceKey<Level> dimension; final int action;
      boolean hit;
      Melee(CuChulainnEntity owner,LivingEntity target,int action) {
         start=owner.level().getGameTime();this.target=target.getUUID();dimension=owner.level().dimension();this.action=action;
      }
   }
   private CuChulainnCombatAi() { }
   public static boolean meleeActive(LivingEntity owner) {
      Melee melee=MELEE.get(owner);return melee!=null && owner.level().getGameTime()-melee.start<12;
   }
   public static void clear(LivingEntity owner) { MELEE.remove(owner); }
   /** One ordinary melee hit; extra spear turns are presentation only. */
   public static boolean basicAttack(CuChulainnEntity owner,LivingEntity target) {
      long now=owner.level().getGameTime();Melee previous=MELEE.get(owner);
      if(!CuChulainnAbilityService.canAct(owner) || CuChulainnAbilityService.active(owner) || CuChulainnWeaponService.busy(owner)
         || previous!=null && now-previous.start<20 || CuChulainnAbilityService.spearHand(owner)==null
         || !CuChulainnAbilityService.enemy(owner,target) || !owner.hasLineOfSight(target) || owner.distanceTo(target)>3)return false;
      int action=CuChulainnRules.NPC_DANCE+owner.getRandom().nextInt(3);
      owner.faceToward(target.position());owner.getNavigation().stop();
      MELEE.put(owner,new Melee(owner,target,action));
      CuChulainnVisualService.action(owner,action,12);CuChulainnVoiceService.attack(owner);return true;
   }
   public static void tickMelee(LivingEntity owner) {
      Melee melee=MELEE.get(owner);if(melee==null)return;
      long age=owner.level().getGameTime()-melee.start;
      if(age>=12){if(age>=20)MELEE.remove(owner);return;}
      if(!(owner instanceof CuChulainnEntity npc) || !(owner.level() instanceof ServerLevel level)
         || !melee.dimension.equals(owner.level().dimension()) || !CuChulainnAbilityService.canAct(owner)
         || CuChulainnAbilityService.spearHand(owner)==null) {
         MELEE.remove(owner);CuChulainnVisualService.action(owner,-100,0);return;
      }
      if(age>=5 && !melee.hit) {
         melee.hit=true;
         var entity=level.getEntity(melee.target);
         if(entity instanceof LivingEntity target && CuChulainnAbilityService.enemy(owner,target)
            && owner.distanceTo(target)<=3 && owner.hasLineOfSight(target))npc.doHurtTarget(target);
      }
   }
   public static void tick(CuChulainnEntity owner,LivingEntity target) {
      if(!CuChulainnAbilityService.canAct(owner) || CuChulainnAbilityService.active(owner)){owner.getNavigation().stop();return;}
      if(CuChulainnWeaponService.followupPending(owner)){owner.getNavigation().stop();return;}
      if(CuChulainnWeaponService.busy(owner)){owner.getNavigation().stop();return;}
      if(CuSpearLedger.outstanding(owner)) {
         owner.getNavigation().stop();
         if(CuChulainnWeaponService.npcRecallDue(owner))CuChulainnAbilityService.start(owner,7);
         return;
      }
      if(!CuChulainnAbilityService.enemy(owner,target))return;
      double range=owner.distanceTo(target);owner.faceToward(target.position());owner.getLookControl().setLookAt(target,30,30);
      if(owner.tickCount%10!=0)return;
      if(owner.getHealth()<owner.getMaxHealth()*.35 && CuChulainnAbilityService.start(owner,5))return;
      if(owner.getHealth()<owner.getMaxHealth()*.65 && CuChulainnAbilityService.start(owner,4))return;
      if(range<=4)CuChulainnWeaponService.lock(owner,target);
      if(owner.getHealth()<owner.getMaxHealth()*.6 && range<=5 && CuChulainnWeaponService.noble(owner,false,20))return;
      if(range>6 && owner.hasLineOfSight(target) && CuChulainnAbilityService.mana(owner)>=300 && CuChulainnWeaponService.noble(owner,true,40))return;
      if(range>=3 && range<=7 && owner.hasLineOfSight(target)
         && owner.level().getGameTime()>=owner.getPersistentData().getLong("CuNpcNextThrust") && CuChulainnAbilityService.start(owner,-1)) {
         owner.getPersistentData().putLong("CuNpcNextThrust",owner.level().getGameTime()+60);return;
      }
      if(range<=4 && CuChulainnAbilityService.start(owner,8))return;
      if(range<=5 && CuChulainnAbilityService.start(owner,9))return;
      if(range>5 && range<=CuChulainnRules.THROW_RANGE && CuChulainnWeaponService.ordinaryThrow(owner,target))return;
      if(owner.getPersistentData().getFloat(net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnCombatHelper.ALGIZ_SHIELD_TAG)<=0 && CuChulainnAbilityService.start(owner,3))return;
      if(range>5 && owner.hasLineOfSight(target) && CuChulainnAbilityService.start(owner,0))return;
      if(owner.getHealth()<owner.getMaxHealth()*.3 && range>5 && CuChulainnAbilityService.start(owner,6))return;
      if(owner.getPersistentData().getLong("CuShared:tiwaz")<=owner.level().getGameTime() && CuChulainnAbilityService.start(owner,2))return;
      if(range>8 && !owner.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED) && CuChulainnAbilityService.start(owner,1))return;
      if(range<=3 && basicAttack(owner,target))return;
      owner.getNavigation().moveTo(target,1.15);
   }
}
