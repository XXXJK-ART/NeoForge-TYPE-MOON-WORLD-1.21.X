package net.xxxjk.TYPE_MOON_WORLD.servant.ai.module;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
   private static final int SLASH_COOLDOWN = 60;
   private static final int TELEPORT_COOLDOWN = 120;
   private static final int STOMP_COOLDOWN = 80;
   private static final int UPPERCUT_COOLDOWN = 50;
   private static final int H_SWING_COOLDOWN = 50;
   private static final int BLOCK_BREAK_COOLDOWN = 15;
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

      // ——— 0. 被方块挡住：挥砍砸开前方路径 ———
      if (entity.getNavigation().isInProgress() || distance > 3.5) {
         int lastBreak = data.getInt("LastBlockBreakTick");
         if (tick - lastBreak >= BLOCK_BREAK_COOLDOWN) {
            boolean stuck = isBlockedForward(entity, target);
            if (stuck) {
               data.putInt("LastBlockBreakTick", tick);
               breakForwardBlocks(entity);
            }
         }
      }

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
               entity.triggerAttackSwing();
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

      // ——— 6. 斩击：近距离 + 周围1-2敌人 ———
      if (distance <= 4.0) {
         AABB slashBox = entity.getBoundingBox().inflate(3.0);
         List<LivingEntity> nearSlash = entity.level().getEntitiesOfClass(
            LivingEntity.class, slashBox,
            e -> e != entity && e.isAlive() && !e.isAlliedTo(entity));
         int lastSlash = data.getInt("LastSlashTick");
         if (nearSlash.size() >= 1 && nearSlash.size() <= 2 && tick - lastSlash >= SLASH_COOLDOWN
               && entity.getRandom().nextInt(100) < 40) {
            data.putInt("LastSlashTick", tick);
            entity.triggerSlashAnimation();
            performSlash(entity, target);
            return;
         }
      }

      // ——— 7. 瞬移到敌人身后：距离 4-12格 ———
      if (distance >= 4.0 && distance <= 12.0) {
         int lastTeleport = data.getInt("LastTeleportTick");
         if (tick - lastTeleport >= TELEPORT_COOLDOWN && entity.getRandom().nextInt(100) < 25) {
            data.putInt("LastTeleportTick", tick);
            entity.triggerTeleportAnimation();
            performTeleportBehind(entity, target);
            return;
         }
      }

      // ——— 8. 跺脚：近距离 ———
      if (distance <= 3.0) {
         int lastStomp = data.getInt("LastStompTick");
         if (tick - lastStomp >= STOMP_COOLDOWN && entity.getRandom().nextInt(100) < 30) {
            data.putInt("LastStompTick", tick);
            entity.triggerStompAnimation();
            performStomp(entity);
            return;
         }
      }

      // ——— 9. 砸地：近距离 + CD ———
      if (distance <= SLAM_RADIUS && !entity.isRoaring() && !entity.isSlamming()) {
         int lastSlam = data.getInt("LastSlamTick");
         if (tick - lastSlam >= SLAM_COOLDOWN && entity.getRandom().nextInt(100) < 30) {
            data.putInt("LastSlamTick", tick);
            entity.triggerGroundSlam();
            performGroundSlam(entity);
            return;
         }
      }

      // ——— 10. 上勾拳：近距离随机 ———
      if (distance <= 3.5) {
         int lastUppercut = data.getInt("LastUppercutTick");
         if (tick - lastUppercut >= UPPERCUT_COOLDOWN && entity.getRandom().nextInt(100) < 35) {
            data.putInt("LastUppercutTick", tick);
            entity.triggerUppercutAnimation();
            performUppercut(entity, target);
            return;
         }
      }

      // ——— 11. 横挥：近距离随机 ———
      if (distance <= 3.5) {
         int lastHSwing = data.getInt("LastHSwingTick");
         if (tick - lastHSwing >= H_SWING_COOLDOWN && entity.getRandom().nextInt(100) < 35) {
            data.putInt("LastHSwingTick", tick);
            entity.triggerHorizontalSwingAnimation();
            performHorizontalSwing(entity, target);
            return;
         }
      }

      // ——— 7. 接近 + 近战 ———
      double speed = distance > 8 ? 1.4 : (distance > 4 ? 1.2 : 1.0);
      if (distance > 3.5) {
         entity.getNavigation().moveTo(target, speed);
      } else {
         entity.doHurtTarget(target);
         entity.triggerAttackSwing();
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

   /**
    * 斩击：对前方单体造成高倍率伤害
    */
   private void performSlash(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      AttributeInstance atkAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      double baseAtk = atkAttr != null ? atkAttr.getValue() : 5.0;
      double slashDamage = baseAtk * 1.8;
      DamageSource src = entity.damageSources().mobAttack(entity);

      // 朝目标方向进行180度扇形检测
      Vec3 look = entity.getLookAngle();
      Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
      AABB slashBox = entity.getBoundingBox().inflate(3.0).move(look.scale(1.5));
      List<LivingEntity> near = sl.getEntitiesOfClass(
         LivingEntity.class, slashBox,
         e -> e != entity && e.isAlive());
      for (LivingEntity le : near) {
         le.hurt(src, (float) slashDamage);
         double dx = le.getX() - entity.getX();
         double dz = le.getZ() - entity.getZ();
         double dist = Math.sqrt(dx * dx + dz * dz);
         if (dist > 0) {
            double kb = 1.0 * (1.0 - dist / 3.0);
            le.push(dx / dist * kb, 0.2, dz / dist * kb);
            le.hurtMarked = true;
         }
      }

      // 斩击轨迹粒子
      for (double r = 0.5; r <= 3.0; r += 0.5) {
         for (double theta = -Math.PI / 2; theta <= Math.PI / 2; theta += Math.PI / 6) {
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

      sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
         entity.getX() + look.x * 2.0,
         entity.getY() + entity.getBbHeight() * 0.6,
         entity.getZ() + look.z * 2.0,
         2, 0.0, 0.0, 0.0, 0.0);
      sl.sendParticles(ParticleTypes.CRIT,
         target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
         10, 0.3, 0.3, 0.3, 0.2);
   }

   /**
    * 瞬移到敌人身后：短距离传送 + 立即攻击
    */
   private void performTeleportBehind(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;

      // 计算目标背后位置
      Vec3 targetLook = target.getLookAngle();
      Vec3 behindPos = target.position().add(targetLook.scale(-2.0));

      // 确保位置安全（不卡在方块里）
      BlockPos safePos = BlockPos.containing(behindPos);
      if (!entity.level().getBlockState(safePos).canBeReplaced()) {
         safePos = safePos.above();
      }
      if (!entity.level().getBlockState(safePos).canBeReplaced()) {
         safePos = entity.blockPosition(); // 回到原位
      }

      // 瞬移
      entity.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);

      // 到达粒子
      sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
         entity.getX(), entity.getY() + 0.5, entity.getZ(),
         15, 0.3, 0.5, 0.3, 0.05);
      sl.sendParticles(ParticleTypes.LARGE_SMOKE,
         entity.getX(), entity.getY() + 1.0, entity.getZ(),
         8, 0.2, 0.3, 0.2, 0.03);

      // 立即攻击
      AttributeInstance atkAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      double baseAtk = atkAttr != null ? atkAttr.getValue() : 5.0;
      double tpDamage = baseAtk * 1.5;
      DamageSource src = entity.damageSources().mobAttack(entity);
      target.hurt(src, (float) tpDamage);
      target.knockback(0.6, entity.getX() - target.getX(), entity.getZ() - target.getZ());
      target.hurtMarked = true;

      // 攻击粒子
      sl.sendParticles(ParticleTypes.CRIT,
         target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
         8, 0.3, 0.3, 0.3, 0.15);
      sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
         SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.2F, 0.7F);
   }

   /**
    * 跺脚：小范围AOE伤害 + 击飞 + 地面碎裂
    */
   private void performStomp(ServantEntity entity) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      AttributeInstance atkAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      double baseAtk = atkAttr != null ? atkAttr.getValue() : 5.0;
      double stompDamage = baseAtk * 1.2;
      DamageSource src = entity.damageSources().mobAttack(entity);

      // 2.5格半径AOE伤害 + 击飞
      double stompRadius = 2.5;
      AABB box = entity.getBoundingBox().inflate(stompRadius);
      List<LivingEntity> nearby = sl.getEntitiesOfClass(
         LivingEntity.class, box, e -> e != entity && e.isAlive());
      for (LivingEntity le : nearby) {
         le.hurt(src, (float) stompDamage);
         double dx = le.getX() - entity.getX();
         double dz = le.getZ() - entity.getZ();
         double dist = Math.sqrt(dx * dx + dz * dz);
         if (dist > 0) {
            double kb = 1.0 * (1.0 - dist / stompRadius);
            le.push(dx / dist * kb, 0.5, dz / dist * kb); // 较高击飞
            le.hurtMarked = true;
         }
      }

      // 地面碎裂：脚下方块部分被破坏
      BlockPos center = entity.blockPosition();
      for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-2, 0, -2),
            center.offset(2, 0, 2))) {
         BlockState state = sl.getBlockState(pos);
         float hardness = state.getDestroySpeed(sl, pos);
         if (!state.isAir() && hardness >= 0 && hardness < 30
               && !state.is(Blocks.BEDROCK)) {
            sl.removeBlock(pos, false);
         }
      }

      // 同心圆碎裂扩散
      for (int i = 0; i < 8; i++) {
         double angle = (Math.PI * 2) * i / 8.0;
         BlockPos ringPos = center.offset(
            (int) Math.round(Math.cos(angle) * 2.5), 0,
            (int) Math.round(Math.sin(angle) * 2.5));
         BlockState state = sl.getBlockState(ringPos);
         float hardness = state.getDestroySpeed(sl, ringPos);
         if (!state.isAir() && hardness >= 0 && hardness < 30
               && !state.is(Blocks.BEDROCK)) {
            sl.removeBlock(ringPos, false);
         }
      }

      // 跺脚冲击波粒子
      sl.sendParticles(ParticleTypes.CLOUD,
         entity.getX(), entity.getY() + 0.1, entity.getZ(),
         20, 1.0, 0.2, 1.0, 0.2);
      sl.sendParticles(ParticleTypes.EXPLOSION,
         entity.getX(), entity.getY() + 0.3, entity.getZ(),
         3, 1.0, 0.3, 1.0, 0.0);
   }

   /**
    * 判断前方是否被可破坏方块阻挡
    */
   private boolean isBlockedForward(ServantEntity entity, LivingEntity target) {
      Vec3 toTarget = target.position().subtract(entity.position());
      Vec3 dir = toTarget.normalize();
      BlockPos eyePos = entity.blockPosition().above();
      // 检查前方1-2格、视线高度和上方一格
      for (int i = 1; i <= 2; i++) {
         BlockPos check = eyePos.offset((int) Math.round(dir.x * i), 0, (int) Math.round(dir.z * i));
         BlockState state = entity.level().getBlockState(check);
         BlockState stateAbove = entity.level().getBlockState(check.above());
         float hard = state.getDestroySpeed(entity.level(), check);
         // 当前格或上方格被实心方块挡住，且可破坏
         if ((!state.isAir() && hard >= 0 && hard < 50 && !state.is(Blocks.BEDROCK))
               || (!stateAbove.isAir() && stateAbove.getDestroySpeed(entity.level(), check.above()) >= 0
               && stateAbove.getDestroySpeed(entity.level(), check.above()) < 50
               && !stateAbove.is(Blocks.BEDROCK))) {
            return true;
         }
      }
      return false;
   }

   /**
    * 砸开前方1-3格的可破坏方块 + 挥砍粒子音效
    */
   private void breakForwardBlocks(ServantEntity entity) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      LivingEntity target = entity.getTarget();
      if (target == null) return;
      Vec3 dir = target.position().subtract(entity.position()).normalize();
      BlockPos center = entity.blockPosition().above();
      int broken = 0;
      // 前方3格、中心±1格宽度
      for (int d = 1; d <= 3; d++) {
         for (int w = -1; w <= 1; w++) {
            Vec3 perp = new Vec3(-dir.z, 0, dir.x).scale(w);
            BlockPos pos = center.offset(
               (int) Math.round(dir.x * d + perp.x),
               0,
               (int) Math.round(dir.z * d + perp.z));
            BlockState state = sl.getBlockState(pos);
            float hard = state.getDestroySpeed(sl, pos);
            if (!state.isAir() && hard >= 0 && hard < 50 && !state.is(Blocks.BEDROCK)) {
               sl.removeBlock(pos, false);
               broken++;
            }
         }
      }
      if (broken > 0) {
         // 挥砍粒子
         sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
            entity.getX() + dir.x * 2.0,
            entity.getY() + entity.getBbHeight() * 0.5,
            entity.getZ() + dir.z * 2.0,
            2, 0.0, 0.0, 0.0, 0.0);
         sl.sendParticles(ParticleTypes.CLOUD,
            entity.getX() + dir.x * 1.5,
            entity.getY() + entity.getBbHeight() * 0.5,
            entity.getZ() + dir.z * 1.5,
            8, 0.2, 0.2, 0.2, 0.08);
         sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2F, 0.8F);
      }
   }

   /**
    * 上勾拳：向右上举起，往左下方挥去，单体高伤害
    */
   private void performUppercut(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      AttributeInstance atkAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      double baseAtk = atkAttr != null ? atkAttr.getValue() : 5.0;
      double dmg = baseAtk * 1.4;
      DamageSource src = entity.damageSources().mobAttack(entity);
      target.hurt(src, (float) dmg);
      target.knockback(0.8, entity.getX() - target.getX(), entity.getZ() - target.getZ());
      target.hurtMarked = true;
      sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
         target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
         2, 0.0, 0.0, 0.0, 0.0);
      sl.sendParticles(ParticleTypes.CRIT,
         target.getX(), target.getY() + target.getBbHeight() * 0.7, target.getZ(),
         8, 0.3, 0.4, 0.3, 0.15);
   }

   /**
    * 横挥：右臂横在胸前向右挥，近距离扇形伤害
    */
   private void performHorizontalSwing(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      AttributeInstance atkAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      double baseAtk = atkAttr != null ? atkAttr.getValue() : 5.0;
      double dmg = baseAtk * 1.2;
      DamageSource src = entity.damageSources().mobAttack(entity);
      Vec3 look = entity.getLookAngle();
      AABB swingBox = entity.getBoundingBox().inflate(2.5).move(look.scale(1.0));
      List<LivingEntity> near = sl.getEntitiesOfClass(
         LivingEntity.class, swingBox,
         e -> e != entity && e.isAlive());
      for (LivingEntity le : near) {
         le.hurt(src, (float) dmg);
         double dx = le.getX() - entity.getX();
         double dz = le.getZ() - entity.getZ();
         double dist = Math.sqrt(dx * dx + dz * dz);
         if (dist > 0) {
            double kb = 0.7 * (1.0 - dist / 2.5);
            le.push(dx / dist * kb, 0.2, dz / dist * kb);
            le.hurtMarked = true;
         }
      }
      sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
         entity.getX() + look.x * 2.0,
         entity.getY() + entity.getBbHeight() * 0.55,
         entity.getZ() + look.z * 2.0,
         2, 0.0, 0.0, 0.0, 0.0);
      sl.sendParticles(ParticleTypes.CLOUD,
         entity.getX() + look.x * 1.5,
         entity.getY() + entity.getBbHeight() * 0.5,
         entity.getZ() + look.z * 1.5,
         6, 0.2, 0.2, 0.2, 0.05);
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
