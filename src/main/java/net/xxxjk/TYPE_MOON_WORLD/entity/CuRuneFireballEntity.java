package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.cu.CuChulainnAbilityService;

public final class CuRuneFireballEntity extends ThrowableItemProjectile {
   public CuRuneFireballEntity(EntityType<? extends ThrowableItemProjectile> type,Level level){super(type,level);setNoGravity(true);}
   @Override protected net.minecraft.world.item.Item getDefaultItem(){return net.minecraft.world.item.Items.FIRE_CHARGE;}
   @Override protected boolean canHitEntity(net.minecraft.world.entity.Entity entity){return getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && CuChulainnAbilityService.enemy(owner,living) && super.canHitEntity(entity);}
   @Override protected void onHitBlock(net.minecraft.world.phys.BlockHitResult result){super.onHitBlock(result);if(!level().isClientSide())discard();}
   public void setOwner(LivingEntity owner){super.setOwner(owner);}
   @Override public void tick(){
      var velocity=getDeltaMovement();super.tick();if(!isRemoved())setDeltaMovement(velocity);
      if(level().isClientSide())return;
      if(tickCount>=24 || !(getOwner() instanceof LivingEntity owner) || !owner.isAlive() || owner.level()!=level()){discard();return;}
      if(level() instanceof net.minecraft.server.level.ServerLevel server)server.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,getX(),getY(),getZ(),2,.05,.05,.05,.01);
   }
   @Override protected void onHitEntity(EntityHitResult result){
      if(!level().isClientSide() && result.getEntity() instanceof LivingEntity target && getOwner() instanceof LivingEntity owner && CuChulainnAbilityService.enemy(owner,target)) {
         var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("typemoonworld","cu_rune_fire"));
         var source=new net.xxxjk.TYPE_MOON_WORLD.servant.combat.CombatDamageSource(new net.minecraft.world.damagesource.DamageSource(registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(key),this,owner),net.xxxjk.TYPE_MOON_WORLD.servant.combat.AttackOrigin.skill("cu_chulainn","cu_ansuz"));
         int previous=target.invulnerableTime;target.invulnerableTime=0;
         try{target.hurt(net.xxxjk.typemoonworld.api.MagecraftDamageSource.of(source,"rune_origin",net.xxxjk.typemoonworld.api.MagicComplexity.TWO_VERSE,owner),10);}finally{target.invulnerableTime=previous;}
         if(!net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper.blocksHarmfulMagicEffect(target,net.xxxjk.typemoonworld.api.MagicComplexity.TWO_VERSE)&&!target.fireImmune()&&!target.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE))target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(),100));
      }
      discard();
   }
}
