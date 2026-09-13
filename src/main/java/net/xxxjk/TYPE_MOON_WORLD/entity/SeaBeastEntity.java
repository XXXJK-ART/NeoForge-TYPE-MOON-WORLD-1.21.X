package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.NightKinEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Shares night-kin combat, specialization and resistance; keeps its own health migration. */
public class SeaBeastEntity extends NightKinEntity implements GeoEntity {
   private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
   public SeaBeastEntity(EntityType<? extends Monster> type,Level level){super(type,level);inheritBodyScale(1);}
   public static AttributeSupplier.Builder createAttributes(){return attributes(300,24,16,.36);}
   @Override protected boolean receivesGeneratedName(){return false;}
   @Override protected void registerGoals(){super.registerGoals();goalSelector.addGoal(0,new FloatGoal(this));}
   @Override protected void ensureCurrentStageAttributes(){migrateStageAttributes("SeaBeastStatsV1",300,getPersistentData().getBoolean("NightKinSpeed")?.36*1.3:.36);}

   @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
   @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){
      c.add(new AnimationController<>(this,"locomotion",5,s->s.setAndContinue(RawAnimation.begin().thenLoop(s.isMoving()?"walk":"idle"))));
   }
   public static boolean checkSpawn(EntityType<SeaBeastEntity> t,net.minecraft.world.level.ServerLevelAccessor l,MobSpawnType st,net.minecraft.core.BlockPos p,net.minecraft.util.RandomSource r){
      if(l.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL || !l.getBiome(p).is(net.minecraft.tags.BiomeTags.IS_BEACH)
         || !Mob.checkMobSpawnRules(t,l,st,p,r))return false;
      // A beach biome can extend inland. Require actual sea water within twelve blocks.
      for(net.minecraft.core.BlockPos q:net.minecraft.core.BlockPos.betweenClosed(p.offset(-12,-2,-12),p.offset(12,1,12)))
         if(l.getFluidState(q).is(net.minecraft.tags.FluidTags.WATER))return true;
      return false;
   }
}