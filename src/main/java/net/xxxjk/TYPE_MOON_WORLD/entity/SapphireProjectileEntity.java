package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class SapphireProjectileEntity extends ThrowableItemProjectile {
   public final List<Vec3> tracePos = new LinkedList<>();

   public SapphireProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
   }

   public SapphireProjectileEntity(Level level, LivingEntity shooter) {
      super(ModEntities.SAPPHIRE_PROJECTILE.get(), shooter, level);
      this.setPos(EntityUtils.getRightHandCastAnchor(shooter));
   }

   public SapphireProjectileEntity(Level level, double x, double y, double z) {
      super(ModEntities.SAPPHIRE_PROJECTILE.get(), x, y, z, level);
   }

   protected Item getDefaultItem() {
      return ModItems.CARVED_SAPPHIRE_FULL.get();
   }

   public void tick() {
      if (!this.level().isClientSide) {
         Vec3 start = this.position();
         Vec3 end = start.add(this.getDeltaMovement());
         HitResult raytrace = this.level().clip(new ClipContext(start, end, Block.COLLIDER, Fluid.SOURCE_ONLY, this));
         if (raytrace.getType() == Type.BLOCK) {
            this.onHit(raytrace);
            if (this.isRemoved()) {
               return;
            }
         }
      } else {
         Vec3 pos = this.position();
         boolean addPos = true;
         if (this.tracePos.size() > 0) {
            Vec3 lastPos = this.tracePos.get(this.tracePos.size() - 1);
            addPos = pos.distanceToSqr(lastPos) >= 0.01;
         }

         if (addPos) {
            this.tracePos.add(pos);
            if (this.tracePos.size() > 560) {
               this.tracePos.remove(0);
            }
         }
      }

      super.tick();
   }

   protected void onHit(HitResult result) {
      super.onHit(result);
      if (!this.level().isClientSide) {
         Level level = this.level();
         BlockPos center = this.blockPosition();
         float multiplier = 1.0F;
         ItemStack stack = this.getItem();
         if (stack.getItem() instanceof FullManaCarvedGemItem gemItem) {
            multiplier = gemItem.getQuality().getEffectMultiplier();
         }

         int radius = Math.round(3.0F * multiplier);
         List<BlockPos> placedBlocks = new ArrayList<>();
         RandomSource random = level.getRandom();
         Entity owner = this.getOwner();

         for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
               for (int z = -radius; z <= radius; z++) {
                  if (x * x + y * y + z * z <= radius * radius) {
                     BlockPos pos = center.offset(x, y, z);
                     if ((owner == null || !(pos.distSqr(owner.blockPosition()) <= 4.0)) && level.getBlockState(pos).isAir()) {
                        int roll = random.nextInt(100);
                        BlockState iceState;
                        if (roll < 60) {
                           iceState = Blocks.ICE.defaultBlockState();
                        } else if (roll < 90) {
                           iceState = Blocks.PACKED_ICE.defaultBlockState();
                        } else {
                           iceState = Blocks.BLUE_ICE.defaultBlockState();
                        }

                        if (level.setBlock(pos, iceState, 3)) {
                           placedBlocks.add(pos);
                        }
                     }
                  }
               }
            }
         }

         if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(), this.getZ(), 50, 3.0, 3.0, 3.0, 0.1);
         }

         AABB aabb = new AABB(center).inflate(radius);

         for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, aabb)) {
            if (entity != owner && !EntityUtils.isImmunePlayerTarget(entity)) {
               entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
               if (owner instanceof LivingEntity livingOwner) {
                  EntityUtils.triggerSwarmAnger(level, livingOwner, entity);
                  if (!(livingOwner instanceof Player ownerPlayer && ownerPlayer.isCreative()) && entity instanceof Mob mob) {
                     mob.setTarget(livingOwner);
                  }
               }
            }
         }

         if (!placedBlocks.isEmpty()) {
            for (BlockPos pos : placedBlocks) {
               int duration = 100 + random.nextInt(60);
               TYPE_MOON_WORLD.queueServerWork(duration, () -> {
                  BlockState state = level.getBlockState(pos);
                  if (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)) {
                     level.destroyBlock(pos, false);
                  }
               });
            }
         }

         this.discard();
      }
   }
}
