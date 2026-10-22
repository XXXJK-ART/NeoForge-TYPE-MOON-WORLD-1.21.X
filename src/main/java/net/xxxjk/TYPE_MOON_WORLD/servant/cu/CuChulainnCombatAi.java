package net.xxxjk.TYPE_MOON_WORLD.servant.cu;

import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnEntity;

public final class CuChulainnCombatAi {
   private CuChulainnCombatAi() { }
   public static void tick(CuChulainnEntity owner,LivingEntity target) {
      if(!CuChulainnAbilityService.canAct(owner) || CuChulainnAbilityService.active(owner))return;
      if(CuChulainnWeaponService.busy(owner)){owner.getNavigation().stop();return;}
      if(CuSpearLedger.outstanding(owner)) {CuChulainnAbilityService.start(owner,7);return;}
      if(!CuChulainnAbilityService.enemy(owner,target))return;
      double range=owner.distanceTo(target);owner.getLookControl().setLookAt(target,30,30);
      if(owner.tickCount%10!=0)return;
      if(owner.getHealth()<owner.getMaxHealth()*.35 && CuChulainnAbilityService.start(owner,5))return;
      if(owner.getHealth()<owner.getMaxHealth()*.65 && CuChulainnAbilityService.start(owner,4))return;
      if(range<=4)CuChulainnWeaponService.lock(owner,target);
      if(owner.getHealth()<owner.getMaxHealth()*.6 && range<=5 && CuChulainnWeaponService.noble(owner,false,20))return;
      if(range>6 && owner.hasLineOfSight(target) && CuChulainnAbilityService.mana(owner)>=300 && CuChulainnWeaponService.noble(owner,true,40))return;
      if(range<=4 && CuChulainnAbilityService.start(owner,8))return;
      if(range<=5 && CuChulainnAbilityService.start(owner,9))return;
      if(owner.getPersistentData().getFloat(net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnCombatHelper.ALGIZ_SHIELD_TAG)<=0 && CuChulainnAbilityService.start(owner,3))return;
      if(range>5 && owner.hasLineOfSight(target) && CuChulainnAbilityService.start(owner,0))return;
      if(owner.getHealth()<owner.getMaxHealth()*.3 && range>5 && CuChulainnAbilityService.start(owner,6))return;
      if(owner.getPersistentData().getLong("CuShared:tiwaz")<=owner.level().getGameTime() && CuChulainnAbilityService.start(owner,2))return;
      if(range>8 && !owner.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED) && CuChulainnAbilityService.start(owner,1))return;
      if(range<3 && owner.tickCount%20==0 && CuChulainnAbilityService.spearHand(owner)!=null)owner.doHurtTarget(target);
      owner.getNavigation().moveTo(target,1.15);
   }
}
