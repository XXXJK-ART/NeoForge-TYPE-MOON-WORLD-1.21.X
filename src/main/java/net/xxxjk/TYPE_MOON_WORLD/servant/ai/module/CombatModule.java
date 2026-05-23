package net.xxxjk.TYPE_MOON_WORLD.servant.ai.module;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.CombatDisposition;

import java.util.List;

public final class CombatModule implements ServantAiModule {
   private static final double SLAM_RADIUS = 4.0;
   private static final double SLAM_DAMAGE_MULTIPLIER = 1.5;
   private static final int ROAR_COOLDOWN = 300;
   private static final int SLAM_COOLDOWN = 120;
   private static final int SWEEP_COOLDOWN = 100;
   private static final int CHARGE_COOLDOWN = 160;
   private static final ResourceLocation VALOR_RES = ResourceLocation.fromNamespaceAndPath(
      "typemoonworld", "frenzy_atk_boost");
   private static final ResourceLocation FRENZY_SPEED_RES = ResourceLocation.fromNamespaceAndPath(
      "typemoonworld", "frenzy_speed_boost");

   @Override
   public void tick(ServantEntity entity, ServantAiContext context) {
      LivingEntity target = context.target();
      if (target == null || target.isDeadOrDying()) {
         entity.setTarget(null);
         return;
      }

      CompoundTag data = entity.getPersistentData();
      double healthRatio = entity.getHealth() / entity.getMaxHealth();
      CombatDisposition combatStyle = entity.getCombatDisposition();

      // 非狂化型：低血量撤退
      if (combatStyle != CombatDisposition.FRENZIED && healthRatio < 0.25) {
         entity.getNavigation().stop();
         entity.setTarget(null);
         return;
      }

      // 动画中冻结
      if (entity.isRoaring() || entity.isSlamming() || entity.isPerformingAction()) {
         return;
      }

      int tick = (int) context.gameTick();
      double distance = entity.distanceTo(target);

      // ——— 1. 首次吼叫（推开 + 视觉） ———
      if (!data.getBoolean("HasRoared")) {
         data.putBoolean("HasRoared", true);
         data.putInt("LastRoarTick", tick);
         entity.triggerRoarAnimation();
         performRoar(entity);
         return;
      }

      // ——— 2. 低血量吼叫（<50%）———
      if (healthRatio < 0.5 && !data.getBoolean("LowHealthRoared")) {
         if (tick - data.getInt("LastRoarTick") >= ROAR_COOLDOWN) {
            data.putBoolean("LowHealthRoared", true);
            data.putInt("LastRoarTick", tick);
            entity.triggerRoarAnimation();
            performRoar(entity);
            return;
         }
      }

      // ——— 3. 跳跃攻击：目标在上方 3-8格 ———
      double dy = target.getY() - entity.getY();
      double distSqr = entity.distanceToSqr(target);
      if (dy > 3.0 && dy < 8.0 && distSqr < 100.0 && entity.onGround()
            && entity.getRandom().nextInt(100) < 25) {
         entity.triggerJumpAttackAnimation();
         // 垂直 + 水平冲向目标
         entity.setDeltaMovement(entity.getDeltaMovement().add(0.0, 1.2, 0.0));
         Vec3 dir = target.position().subtract(entity.position()).normalize().scale(0.5);
         entity.setDeltaMovement(entity.getDeltaMovement().add(dir.x, 0.0, dir.z));
         // 着陆时触发AOE（模拟着陆砸地）
         performGroundSlam(entity);
         return;
      }

      // ——— 4. 冲刺攻击：距离 6-15格 ———
      if (distance >= 6.0 && distance <= 15.0) {
         int lastCharge = data.getInt("LastChargeTick");
         if (tick - lastCharge >= CHARGE_COOLDOWN && entity.getRandom().nextInt(100) < 30) {
            data.putInt("LastChargeTick", tick);
            entity.triggerChargeAnimation();
            // 面朝目标快速冲刺
            Vec3 chargeDir = target.position().subtract(entity.position()).normalize().scale(1.5);
            entity.setDeltaMovement(chargeDir.x, entity.getDeltaMovement().y, chargeDir.z);
            // 冲刺粒子尾迹
            if (entity.level() instanceof ServerLevel sl) {
               sl.sendParticles(ParticleTypes.CLOUD,
                  entity.getX(), entity.getY() + 0.5, entity.getZ(),
                  25, 0.3, 0.3, 0.3, 0.15);
            }
            // 冲刺路径上破坏方块（模拟冲锋）
            BlockPos entityPos = entity.blockPosition();
            for (int i = 0; i < 5; i++) {
               BlockPos breakPos = entityPos.relative(entity.getDirection(), i);
               BlockState state = entity.level().getBlockState(breakPos);
               float hardness = state.getDestroySpeed(entity.level(), breakPos);
               if (!state.isAir() && hardness >= 0 && hardness < 50
                     && !state.is(Blocks.BEDROCK)) {
                  entity.level().removeBlock(breakPos, false);
               }
            }
            // 冲刺结束后对目标造成伤害
            if (distance <= 3.5) {
               entity.doHurtTarget(target);
               applyDivinityDamage(entity, target, data);
               applyMadEnhancementDamage(entity, target, data);
            }
            return;
         }
      }

      // ——— 5. 横扫攻击：近距离 + 周围2+敌人 ———
      if (distance <= 4.0) {
         AABB sweepBox = entity.getBoundingBox().inflate(3.0);
         List<LivingEntity> nearby = entity.level().getEntitiesOfClass(
            LivingEntity.class, sweepBox,
            e -> e != entity && e.isAlive() && !e.isAlliedTo(entity));
         int lastSweep = data.getInt("LastSweepTick");
         if (nearby.size() >= 2 && tick - lastSweep >= SWEEP_COOLDOWN
               && entity.getRandom().nextInt(100) < 35) {
            data.putInt("LastSweepTick", tick);
            entity.triggerSweepAnimation();
            performSweep(entity);
            return;
         }
      }

      // ——— 6. 砸地：近距离 + CD ———
      if (distance <= SLAM_RADIUS && !entity.isRoaring() && !entity.isSlamming()) {
         int lastSlam = data.getInt("LastSlamTick");
         if (tick - lastSlam >= SLAM_COOLDOWN && entity.getRandom().nextInt(100) < 30) {
            data.putInt("LastSlamTick", tick);
            entity.triggerGroundSlam();
            performGroundSlam(entity);
            return;
         }
      }

      // ——— 7. 接近 + 近战 ———
      double speed = distance > 8 ? 1.4 : (distance > 4 ? 1.2 : 1.0);
      if (distance > 3.5) {
         entity.getNavigation().moveTo(target, speed);
      } else {
         entity.doHurtTarget(target);
         applyDivinityDamage(entity, target, data);
         applyMadEnhancementDamage(entity, target, data);
         // 近战攻击粒子
         if (entity.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
               target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
               1, 0.0, 0.0, 0.0, 0.0);
            sl.sendParticles(ParticleTypes.CRIT,
               target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
               5, 0.3, 0.3, 0.3, 0.15);
         }
      }

      // ——— 8. 狂暴：低血量永久 ATK+5 + 移速+0.05 ———
      if (healthRatio < 0.3 && combatStyle == CombatDisposition.FRENZIED) {
         AttributeInstance atk = entity.getAttribute(Attributes.ATTACK_DAMAGE);
         if (atk != null && atk.getModifier(VALOR_RES) == null) {
            atk.addPermanentModifier(new AttributeModifier(
               VALOR_RES, 5.0, AttributeModifier.Operation.ADD_VALUE));
         }
         AttributeInstance spd = entity.getAttribute(Attributes.MOVEMENT_SPEED);
         if (spd != null && spd.getModifier(FRENZY_SPEED_RES) == null) {
            spd.addPermanentModifier(new AttributeModifier(
               FRENZY_SPEED_RES, 0.05, AttributeModifier.Operation.ADD_VALUE));
         }
      }
   }

   /**
    * 神性 A：每次近战额外施加 25 点魔法伤害
    */
   private void applyDivinityDamage(ServantEntity entity, LivingEntity target, CompoundTag data) {
      if (data.getBoolean("DivinityActive")) {
         float divDmg = data.getFloat("DivinityFlatDamage");
         if (divDmg > 0) {
            target.hurt(entity.damageSources().magic(), divDmg);
         }
      }
   }

   /**
    * 狂化 B：额外造成基础攻击力 15% 的伤害
    */
   private void applyMadEnhancementDamage(ServantEntity entity, LivingEntity target, CompoundTag data) {
      if (data.getBoolean("MadEnhancementActive")) {
         float bonusRatio = data.getFloat("MadEnhancementDamageBonus");
         if (bonusRatio > 0) {
            AttributeInstance atk = entity.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atk != null) {
               float extraDmg = (float) (atk.getValue() * bonusRatio);
               target.hurt(entity.damageSources().mobAttack(entity), extraDmg);
            }
         }
      }
   }

   private void performRoar(ServantEntity entity) {
      if (!(entity.level() instanceof ServerLevel)) return;
      AABB box = entity.getBoundingBox().inflate(6.0);
      List<LivingEntity> nearby = entity.level().getEntitiesOfClass(
         LivingEntity.class, box, e -> e != entity && e.isAlive());
      for (LivingEntity le : nearby) {
         double dx = le.getX() - entity.getX();
         double dz = le.getZ() - entity.getZ();
         double dist = Math.sqrt(dx * dx + dz * dz);
         if (dist > 0) {
            double strength = 0.8 * (1.0 - dist / 6.0);
            le.push(dx / dist * strength, 0.3, dz / dist * strength);
            le.hurtMarked = true;
         }
      }
   }

   private void performGroundSlam(ServantEntity entity) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      AttributeInstance atkAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      double baseAtk = atkAttr != null ? atkAttr.getValue() : 5.0;
      double slamDamage = baseAtk * SLAM_DAMAGE_MULTIPLIER;
      DamageSource src = entity.damageSources().mobAttack(entity);
      AABB box = entity.getBoundingBox().inflate(SLAM_RADIUS);
      List<LivingEntity> nearby = entity.level().getEntitiesOfClass(
         LivingEntity.class, box, e -> e != entity && e.isAlive());
      for (LivingEntity le : nearby) {
         le.hurt(src, (float) slamDamage);
         double dx = le.getX() - entity.getX();
         double dz = le.getZ() - entity.getZ();
         double dist = Math.sqrt(dx * dx + dz * dz);
         if (dist > 0) {
            double kb = 1.2 * (1.0 - dist / SLAM_RADIUS);
            le.push(dx / dist * kb, 0.4, dz / dist * kb);
            le.hurtMarked = true;
         }
      }
      sl.sendParticles(ParticleTypes.EXPLOSION,
         entity.getX(), entity.getY() + 0.5, entity.getZ(),
         5, 2.0, 0.5, 2.0, 0.0);
      // 砸地地形破坏：3格半径内破坏软方块
      BlockPos center = entity.blockPosition();
      int radius = 3;
      for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-radius, 0, -radius),
            center.offset(radius, 1, radius))) {
         BlockState state = sl.getBlockState(pos);
         float hardness = state.getDestroySpeed(sl, pos);
         if (!state.isAir() && hardness >= 0 && hardness < 50
               && !state.is(Blocks.BEDROCK)) {
            sl.removeBlock(pos, false);
         }
      }
   }

   /**
    * 横扫攻击：扇形范围伤害 + 地形破坏
    */
   private void performSweep(ServantEntity entity) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      Vec3 look = entity.getLookAngle();
      Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
      AttributeInstance atkAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      double baseAtk = atkAttr != null ? atkAttr.getValue() : 5.0;
      double sweepDamage = baseAtk * 1.2;
      DamageSource src = entity.damageSources().mobAttack(entity);

      // 扇形3格范围，前方180度
      for (double r = 0.5; r <= 3.0; r += 0.5) {
         for (double theta = -Math.PI / 2; theta <= Math.PI / 2; theta += Math.PI / 8) {
            double x = look.x * Math.cos(theta) - look.z * Math.sin(theta);
            double z = look.x * Math.sin(theta) + look.z * Math.cos(theta);
            Vec3 offset = new Vec3(x, 0, z).normalize().scale(r);
            BlockPos pos = BlockPos.containing(center.add(offset));
            BlockState state = sl.getBlockState(pos);
            if (!state.isAir() && (state.getDestroySpeed(sl, pos) >= 0
                  || state.getFluidState().isSource())) {
               sl.removeBlock(pos, false);
            }
         }
      }

      // 扇形范围伤害
      AABB sweepBox = entity.getBoundingBox().inflate(3.0).move(look.scale(1.5));
      List<LivingEntity> nearby = sl.getEntitiesOfClass(
         LivingEntity.class, sweepBox,
         e -> e != entity && e.isAlive());
      for (LivingEntity le : nearby) {
         le.hurt(src, (float) sweepDamage);
         double dx = le.getX() - entity.getX();
         double dz = le.getZ() - entity.getZ();
         double dist = Math.sqrt(dx * dx + dz * dz);
         if (dist > 0) {
            double kb = 0.8 * (1.0 - dist / 3.0);
            le.push(dx / dist * kb, 0.3, dz / dist * kb);
            le.hurtMarked = true;
         }
      }

      // 横扫粒子特效
      sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
         entity.getX() + look.x * 1.5,
         entity.getY() + entity.getBbHeight() * 0.5,
         entity.getZ() + look.z * 1.5,
         3, 0.0, 0.0, 0.0, 0.0);
   }
}
