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
import net.xxxjk.TYPE_MOON_WORLD.entity.GaeBulgProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModParticles;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SasakiKojiroCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSpecialization;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.CombatDisposition;

import java.util.List;

public final class CombatModule implements ServantAiModule {
   private static final double SLAM_RADIUS = 4.0;
   private static final double SLAM_DAMAGE_MULTIPLIER = 1.5;
   private static final int BASIC_ATTACK_COOLDOWN = 12;
   private static final int ASSASSIN_BASIC_ATTACK_COOLDOWN = 6;
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
   private static final int CU_RECAST_MP_COST = 25;
   private static final int CU_RUNE_MP_COST = 20;
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

      SasakiKojiroCombatHelper.markCombat(entity);
      CuChulainnCombatHelper.markCombat(entity);

      CompoundTag data = entity.getPersistentData();
      double healthRatio = entity.getHealth() / entity.getMaxHealth();
      CombatDisposition combatStyle = entity.getCombatDisposition();
      var behaviorProfile = context.behaviorProfile();
      int tick = (int) context.gameTick();
      ServantSpecialization specialization = context.definition().specialization();
      boolean canBreakForwardBlocks = specialization.hasCombatAction("break_forward_blocks");
      boolean canRoar = specialization.hasCombatAction("roar");
      boolean canLowHealthRoar = specialization.hasCombatAction("low_health_roar");
      boolean canJumpAttack = specialization.hasCombatAction("jump_attack");
      boolean canCharge = specialization.hasCombatAction("charge");
      boolean canSweep = specialization.hasCombatAction("sweep");
      boolean canSlash = specialization.hasCombatAction("slash");
      boolean canTeleportBehind = specialization.hasCombatAction("teleport_behind");
      boolean canStomp = specialization.hasCombatAction("stomp");
      boolean canUppercut = specialization.hasCombatAction("uppercut");
      boolean canHorizontalSwing = specialization.hasCombatAction("horizontal_swing");
      boolean canGroundSlam = specialization.hasCombatAction("slam");
      boolean canAssassinCombo = specialization.hasCombatAction("combo");
      boolean canTsurigameshi = specialization.hasCombatAction("tsurigameshi");
      boolean canRuneCast = specialization.hasCombatAction("rune_cast");
      boolean canGaeBolg = specialization.hasCombatAction("gae_bolg");
      boolean canGaeBolgArmy = specialization.hasCombatAction("gae_bolg_army");
      boolean canRecastStance = specialization.hasCombatAction("recast_stance");
      double retreatThreshold = Math.max(0.08, behaviorProfile.applyCombatDisposition(combatStyle));
      double aggressionRange = Math.max(16.0, behaviorProfile.aggressionRange());
      if (CuChulainnCombatHelper.isLaguzActive(entity)) {
         aggressionRange *= 2.0;
      }
      double attackCommitDistance = Math.max(3.0, behaviorProfile.attackCommitDistance());
      double skillChanceScale = Math.max(0.75, Math.min(1.35, behaviorProfile.skillUsageFrequency() / 0.65));
      boolean hasLineOfSight = entity.getSensing().hasLineOfSight(target);
      boolean gaeBolgWindingUp = CuChulainnCombatHelper.isGaeBolgWindingUp(entity);

      entity.getLookControl().setLookAt(target, 30.0F, 30.0F);

      if (entity.distanceToSqr(target) > aggressionRange * aggressionRange * 1.5) {
         entity.setTarget(null);
         entity.getNavigation().stop();
         return;
      }

      // 非狂化型：低血量撤退
      if (combatStyle != CombatDisposition.FRENZIED && healthRatio < retreatThreshold) {
         // 决死一战检测
         int retreatStartTick = data.getInt("RetreatStartTick");
         if (retreatStartTick == 0) {
            data.putInt("RetreatStartTick", tick);
            retreatStartTick = tick;
         }

         // 条件1：逃跑时间超过10秒 (200 ticks)
         boolean timeout = (tick - retreatStartTick) >= 200;

         // 条件2：周围3格内有3+敌人
         AABB dangerZone = entity.getBoundingBox().inflate(3.0);
         int enemyCount = entity.level().getEntitiesOfClass(
            LivingEntity.class, dangerZone,
            e -> e != entity && e.isAlive() && !e.isAlliedTo(entity)
         ).size();
         boolean surrounded = enemyCount >= 3;

         // 决死一战：清除逃跑计时器，获得临时buff
         if (timeout || surrounded) {
            data.remove("RetreatStartTick");
            data.putBoolean("LastStandActive", true);
            // 施加力量I 10秒 (200 ticks)
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
               net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 200, 0, false, true));
            // 施加抗性I 10秒
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
               net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 200, 0, false, true));
            // 粒子效果
            if (entity.level() instanceof ServerLevel sl) {
               sl.sendParticles(ParticleTypes.ENCHANTED_HIT,
                  entity.getX(), entity.getY() + 1, entity.getZ(),
                  30, 0.5, 0.5, 0.5, 0.1);
            }
            // 不执行逃跑，继续正常战斗
         } else {
            performRetreatFootwork(entity, target, combatStyle);
            return;
         }
      }

      // 动画中冻结
      if (entity.isRoaring() || entity.isSlamming() || entity.isHardCombatActionActive()) {
         return;
      }

      if (!gaeBolgWindingUp && canRecastStance && healthRatio < 0.3 && CuChulainnCombatHelper.canUseRecastStance(entity)
            && entity.getCurrentMp() >= CU_RECAST_MP_COST) {
         performRecastStance(entity);
         return;
      }

      double distance = entity.distanceTo(target);

      // ——— 0. 被方块挡住：挥砍砸开前方路径（仅Berserker） ———
      if (canBreakForwardBlocks && (entity.getNavigation().isInProgress() || distance > 3.5)) {
         int lastBreak = data.getInt("LastBlockBreakTick");
         if (tick - lastBreak >= BLOCK_BREAK_COOLDOWN) {
            boolean stuck = isBlockedForward(entity, target);
            if (stuck) {
               data.putInt("LastBlockBreakTick", tick);
               breakForwardBlocks(entity);
            }
         }
      }

      // ——— 1. 首次吼叫（推开 + 视觉）（Berserker专属） ———
      if (canRoar && !data.getBoolean("HasRoared")) {
         data.putBoolean("HasRoared", true);
         data.putInt("LastRoarTick", tick);
         entity.triggerRoarAnimation();
         performRoar(entity);
         return;
      }

      // ——— 2. 低血量吼叫（<50%）（Berserker专属）———
      if (canLowHealthRoar && healthRatio < 0.5 && !data.getBoolean("LowHealthRoared")) {
         if (tick - data.getInt("LastRoarTick") >= ROAR_COOLDOWN) {
            data.putBoolean("LowHealthRoared", true);
            data.putInt("LastRoarTick", tick);
            entity.triggerRoarAnimation();
            performRoar(entity);
            
         }
      }

      // ——— 3. 跳跃攻击：目标在上方 3-8格（Berserker专属） ———
      double dy = target.getY() - entity.getY();
      double distSqr = entity.distanceToSqr(target);
      if (canJumpAttack && dy > 3.0 && dy < 8.0 && distSqr < 100.0 && entity.onGround()
            && passesSkillChance(entity, 25, skillChanceScale)) {
         entity.triggerJumpAttackAnimation();
         // 垂直 + 水平冲向目标
         entity.getNavigation().stop();
         entity.jumpFromGround();
         Vec3 dir = target.position().subtract(entity.position());
         Vec3 horizontal = new Vec3(dir.x, 0.0, dir.z);
         if (horizontal.lengthSqr() > 1.0E-4) {
            horizontal = horizontal.normalize().scale(0.6);
         } else {
            horizontal = Vec3.ZERO;
         }
         Vec3 motion = entity.getDeltaMovement();
         entity.setDeltaMovement(motion.x + horizontal.x, Math.max(motion.y + 0.55, 1.25), motion.z + horizontal.z);
         entity.hasImpulse = true;
         // 着陆时触发AOE（模拟着陆砸地）
         performGroundSlam(entity);
         return;
      }

      // ——— 4. 冲刺攻击：距离 6-15格（Berserker专属） ———
      if (canCharge && distance >= 6.0 && distance <= 15.0) {
         int lastCharge = data.getInt("LastChargeTick");
         if (tick - lastCharge >= CHARGE_COOLDOWN && passesSkillChance(entity, 30, skillChanceScale)) {
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
            
         }
      }

      // ——— 5. 横扫攻击：近距离 + 周围2+敌人 ———
      if (canSweep && distance <= 4.0) {
         AABB sweepBox = entity.getBoundingBox().inflate(3.0);
         List<LivingEntity> nearby = entity.level().getEntitiesOfClass(
            LivingEntity.class, sweepBox,
            e -> e != entity && e.isAlive() && !e.isAlliedTo(entity));
         int lastSweep = data.getInt("LastSweepTick");
         if (nearby.size() >= 2 && tick - lastSweep >= SWEEP_COOLDOWN
               && passesSkillChance(entity, 35, skillChanceScale)) {
            data.putInt("LastSweepTick", tick);
            entity.triggerSweepAnimation();
            performSweep(entity);
            return;
         }
      }

      // ——— 6. 斩击：近距离 + 周围1-2敌人 ———
      if (canSlash && distance <= 4.0) {
         AABB slashBox = entity.getBoundingBox().inflate(3.0);
         List<LivingEntity> nearSlash = entity.level().getEntitiesOfClass(
            LivingEntity.class, slashBox,
            e -> e != entity && e.isAlive() && !e.isAlliedTo(entity));
         int lastSlash = data.getInt("LastSlashTick");
         if (nearSlash.size() >= 1 && nearSlash.size() <= 2 && tick - lastSlash >= SLASH_COOLDOWN
               && passesSkillChance(entity, 40, skillChanceScale)) {
            data.putInt("LastSlashTick", tick);
            entity.triggerSlashAnimation();
            performSlash(entity, target);
            return;
         }
      }

      // ——— 7. 瞬移到敌人身后：距离 4-12格 ———
      if (canTeleportBehind && distance >= 4.0 && distance <= 12.0) {
         int lastTeleport = data.getInt("LastTeleportTick");
         if (tick - lastTeleport >= TELEPORT_COOLDOWN && (!hasLineOfSight || passesSkillChance(entity, 25, skillChanceScale))) {
            data.putInt("LastTeleportTick", tick);
            entity.triggerTeleportAnimation();
            performTeleportBehind(entity, target);
            return;
         }
      }

      // ——— 8. 跺脚：近距离（Berserker专属） ———
      if (canStomp && distance <= 3.0) {
         int lastStomp = data.getInt("LastStompTick");
         if (tick - lastStomp >= STOMP_COOLDOWN && passesSkillChance(entity, 30, skillChanceScale)) {
            data.putInt("LastStompTick", tick);
            entity.triggerStompAnimation();
            performStomp(entity);
            return;
         }
      }

      // ——— 9. 砸地：近距离 + CD（Berserker专属） ———
      if (canGroundSlam && distance <= SLAM_RADIUS && !entity.isRoaring() && !entity.isSlamming()) {
         int lastSlam = data.getInt("LastSlamTick");
         if (tick - lastSlam >= SLAM_COOLDOWN && passesSkillChance(entity, 30, skillChanceScale)) {
            data.putInt("LastSlamTick", tick);
            entity.triggerGroundSlam();
            performGroundSlam(entity);
            return;
         }
      }

      // ——— 燕返（Assassin专属）：目标HP<40%，100固定真伤 + 概率斩杀 ———
      if (canTsurigameshi && distance < 4.0 && !SasakiKojiroCombatHelper.isBladeBroken(entity)) {
         int lastTsurigameshi = data.getInt("LastTsurigameshiTick");
         if (tick - lastTsurigameshi >= 600 && entity.getCurrentMp() >= 30) {
            data.putInt("LastTsurigameshiTick", tick);
            entity.setCurrentMp(entity.getCurrentMp() - 30);
            entity.triggerTsurigameshiAnimation();
            performTsurigameshi(entity, target);
            return;
         }
      }

      // ——— Assassin连击：近距离快速3连击 ———
      if (canAssassinCombo && distance < 3.0) {
         int lastCombo = data.getInt("LastComboTick");
         if (tick - lastCombo >= 20 && passesSkillChance(entity, 50, skillChanceScale * 1.2)) {
            data.putInt("LastComboTick", tick);
            performAssassinCombo(entity, target);
            return;
         }
      }

      // ——— 10. 上勾拳：近距离随机 ———
      if (!gaeBolgWindingUp && canRuneCast && entity.getCurrentMp() >= CU_RUNE_MP_COST && CuChulainnCombatHelper.canCastRune(entity)) {
         if (tryUseCuRune(entity, target, healthRatio, distance, hasLineOfSight)) {
            return;
         }
      }

      if (!gaeBolgWindingUp && canGaeBolgArmy && entity.getCurrentMp() >= 300 && CuChulainnCombatHelper.canUseArmyGaeBolg(entity)) {
         AABB armyBox = entity.getBoundingBox().inflate(8.0);
         int groupSize = entity.level().getEntitiesOfClass(
            LivingEntity.class, armyBox, e -> e != entity && e.isAlive() && !e.isAlliedTo(entity)
         ).size();
         boolean desperate = healthRatio <= 0.5;
         boolean favorableWindow = distance >= 5.0 || groupSize >= 2 || desperate;
         int armyChance = desperate ? 100 : (groupSize >= 3 ? 55 : 35);
         if (favorableWindow && passesSkillChance(entity, armyChance, desperate ? skillChanceScale * 1.25 : skillChanceScale)) {
            performGaeBolgArmy(entity, target);
            return;
         }
      }

      if (!gaeBolgWindingUp && canGaeBolg && entity.getCurrentMp() >= 10 && CuChulainnCombatHelper.canUseSingleGaeBolg(entity)) {
         if (distance <= 12.0 && (distance <= 3.0 || passesSkillChance(entity, 28, skillChanceScale))) {
            performGaeBolg(entity, target, distance <= 3.0);
            return;
         }
      }

      if (canUppercut && distance <= 3.5) {
         int lastUppercut = data.getInt("LastUppercutTick");
         if (tick - lastUppercut >= UPPERCUT_COOLDOWN && passesSkillChance(entity, 35, skillChanceScale)) {
            data.putInt("LastUppercutTick", tick);
            entity.triggerUppercutAnimation();
            performUppercut(entity, target);
            return;
         }
      }

      // ——— 11. 横挥：近距离随机 ———
      if (canHorizontalSwing && distance <= 3.5) {
         int lastHSwing = data.getInt("LastHSwingTick");
         if (tick - lastHSwing >= H_SWING_COOLDOWN && passesSkillChance(entity, 35, skillChanceScale)) {
            data.putInt("LastHSwingTick", tick);
            entity.triggerHorizontalSwingAnimation();
            performHorizontalSwing(entity, target);
            return;
         }
      }

      // ——— 7. 接近 + 近战 ———
      
      if (!hasLineOfSight) {
         entity.getNavigation().moveTo(target, distance > 8.0 ? 1.3 : 1.1);
      } else if (distance > Math.max(3.5, attackCommitDistance)) {
         double speed = distance > 10.0 ? 1.35 : (distance > 6.0 ? 1.2 : 1.0);
         if (combatStyle == CombatDisposition.CAUTIOUS && distance < attackCommitDistance + 1.5) {
            speed = 0.9;
         }
         entity.getNavigation().moveTo(target, speed);
      } else {
         int basicAttackCooldown = canAssassinCombo ? ASSASSIN_BASIC_ATTACK_COOLDOWN : BASIC_ATTACK_COOLDOWN;
         int lastBasicAttack = data.getInt("LastBasicAttackTick");
         if (distance > 2.4 || tick - lastBasicAttack < basicAttackCooldown) {
            performCombatFootwork(entity, target, combatStyle, distance);
            return;
         }
         data.putInt("LastBasicAttackTick", tick);
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
   private boolean passesSkillChance(ServantEntity entity, int basePercent, double scale) {
      int effectivePercent = (int)Math.round(basePercent * scale);
      effectivePercent = Math.max(5, Math.min(95, effectivePercent));
      return entity.getRandom().nextInt(100) < effectivePercent;
   }

   private void performRetreatFootwork(ServantEntity entity, LivingEntity target, CombatDisposition combatStyle) {
      Vec3 away = entity.position().subtract(target.position());
      if (away.lengthSqr() < 1.0E-4) {
         away = new Vec3(entity.getRandom().nextDouble() - 0.5, 0.0, entity.getRandom().nextDouble() - 0.5);
      }

      Vec3 retreatPos = entity.position().add(away.normalize().scale(combatStyle == CombatDisposition.CAUTIOUS ? 6.0 : 4.0));
      entity.getNavigation().moveTo(retreatPos.x, retreatPos.y, retreatPos.z, combatStyle == CombatDisposition.CAUTIOUS ? 1.25 : 1.05);
      if (entity.getRandom().nextInt(4) == 0) {
         float side = entity.getRandom().nextBoolean() ? 0.5F : -0.5F;
         entity.getMoveControl().strafe(-0.6F, side);
      }
      entity.getLookControl().setLookAt(target, 30.0F, 30.0F);
   }

   private void performCombatFootwork(ServantEntity entity, LivingEntity target, CombatDisposition combatStyle, double distance) {
      entity.getNavigation().stop();
      float side = entity.getRandom().nextBoolean() ? 0.45F : -0.45F;
      float forward = switch (combatStyle) {
         case CAUTIOUS -> distance < 2.4 ? -0.35F : -0.10F;
         case FRENZIED -> 0.35F;
         default -> distance > 2.7 ? 0.10F : 0.0F;
      };
      float sideScale = combatStyle == CombatDisposition.FRENZIED ? 0.20F : side;
      entity.getMoveControl().strafe(forward, sideScale);
      entity.getLookControl().setLookAt(target, 30.0F, 30.0F);
   }

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
    * 燕返（Tsurigameshi）：必杀一击特效增强版
    */
   private void performTsurigameshi(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      if (SasakiKojiroCombatHelper.isSasakiKojiro(entity)) {
         float hitChance = SasakiKojiroCombatHelper.getTsurigameshiHitChance(entity);
         if (hitChance <= 0.0F) {
            return;
         }

         if (entity.getRandom().nextFloat() > hitChance) {
            // 未命中：少量挥空粒子 + 弱攻击音效
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
               target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
               4, 0.25, 0.25, 0.25, 0.02);
            sl.sendParticles(ParticleTypes.CLOUD,
               target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
               8, 0.2, 0.2, 0.2, 0.04);
            sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
               SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.HOSTILE, 1.0F, 1.15F);
            return;
         }

         target.invulnerableTime = 0;
         target.hurt(entity.damageSources().mobAttack(entity), 300.0F);
         target.invulnerableTime = 0;

         // === 燕返命中：多重斩击轨迹 + 灵子散逸 ===
         Vec3 entityPos = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
         Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
         Vec3 slashDir = targetPos.subtract(entityPos).normalize();

         // 1. 弧形斩击轨迹粒子（从下往上三道弧线）
         for (int arc = 0; arc < 3; arc++) {
            double arcOffset = (arc - 1) * 0.4;
            for (double t = 0.0; t <= 1.0; t += 0.1) {
               Vec3 pos = entityPos.lerp(targetPos, t);
               double wave = Math.sin(t * Math.PI) * 0.3 * (arc + 1);
               Vec3 perp = new Vec3(-slashDir.z, 0, slashDir.x);
               pos = pos.add(perp.scale(wave + arcOffset));
               sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                  pos.x, pos.y, pos.z,
                  2, 0.0, 0.0, 0.0, 0.0);
            }
         }

         // 2. 目标处爆发：灵魂火焰 + 末影碎裂
         sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
            target.getX(), target.getY() + target.getBbHeight() * 0.3, target.getZ(),
            25, 0.5, 0.6, 0.5, 0.08);
         sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
            target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
            15, 0.4, 0.5, 0.4, 0.05);
         sl.sendParticles(ParticleTypes.SOUL,
            target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
            12, 0.3, 0.4, 0.3, 0.04);

         // 3. 斩击闪光（中心扩散）
         sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
            target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
            8, 0.3, 0.3, 0.3, 0.15);

         // 4. 实体侧烟雾残留（瞬移残影感）
         sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
            entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
            20, 0.3, 0.5, 0.3, 0.04);

         // 5. 击飞效果
         double dx = target.getX() - entity.getX();
         double dz = target.getZ() - entity.getZ();
         double dist = Math.sqrt(dx * dx + dz * dz);
         if (dist > 0) {
            target.push(dx / dist * 0.8, 0.4, dz / dist * 0.8);
            target.hurtMarked = true;
         }

         // 6. 音效叠加
         sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.8F, 0.5F);
         sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.ENDER_EYE_DEATH, SoundSource.HOSTILE, 1.2F, 0.7F);
         sl.playSound(null, target.getX(), target.getY(), target.getZ(),
            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.5F, 0.6F);
         return;
      }

      // 100点固定真伤
      target.hurt(entity.damageSources().generic(), 100.0F);

      double hpRatio = target.getHealth() / target.getMaxHealth();
      net.minecraft.util.RandomSource random = entity.getRandom();

      // 即死判定（HP<10%，30%概率）
      if (hpRatio < 0.10 && random.nextInt(100) < 30) {
         target.setHealth(0);
         target.die(entity.damageSources().mobAttack(entity));
      }
      // 半血斩杀判定（HP<30%，50%概率，额外造成剩余HP一半的伤害）
      else if (hpRatio < 0.30 && random.nextInt(100) < 50) {
         float bonusDmg = target.getHealth() / 2.0F;
         target.hurt(entity.damageSources().generic(), bonusDmg);
      }

      // 通用燕返特效
      Vec3 entityPos = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
      Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
      Vec3 slashDir = targetPos.subtract(entityPos).normalize();

      // 弧形斩击轨迹
      for (double t = 0.0; t <= 1.0; t += 0.15) {
         Vec3 pos = entityPos.lerp(targetPos, t);
         double wave = Math.sin(t * Math.PI) * 0.2;
         Vec3 perp = new Vec3(-slashDir.z, 0, slashDir.x);
         pos = pos.add(perp.scale(wave));
         sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
            pos.x, pos.y, pos.z,
            1, 0.0, 0.0, 0.0, 0.0);
      }

      sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
         target.getX(), target.getY() + target.getBbHeight() * 0.3, target.getZ(),
         15, 0.4, 0.5, 0.4, 0.06);
      sl.sendParticles(ParticleTypes.CRIT,
         target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
         20, 0.4, 0.4, 0.4, 0.2);
      sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
         entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
         15, 0.3, 0.3, 0.3, 0.08);

      sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
         SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.5F, 0.6F);
      sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
         SoundEvents.ENDER_EYE_DEATH, SoundSource.HOSTILE, 1.0F, 0.8F);
   }

   /**
    * Assassin连击：快速3连击
    */
   private void performAssassinCombo(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;

      entity.triggerAttackSwing();

      for (int i = 0; i < 3; i++) {
         final int idx = i;
         net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.queueServerWork(idx * 4, () -> {
            if (target.isAlive() && entity.isAlive()) {
               entity.doHurtTarget(target);
            }
         });
      }

      // 连击粒子
      sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
         target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
         3, 0.3, 0.3, 0.3, 0.1);
      sl.sendParticles(ParticleTypes.CRIT,
         target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
         8, 0.3, 0.3, 0.3, 0.15);
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

   private boolean tryUseCuRune(ServantEntity entity, LivingEntity target, double healthRatio, double distance, boolean hasLineOfSight) {
      CuChulainnCombatHelper.RuneType activeRune = CuChulainnCombatHelper.getActiveRune(entity);
      if (healthRatio < 0.65 && entity.getPersistentData().getFloat(CuChulainnCombatHelper.ALGIZ_SHIELD_TAG) <= 0.0F
            && activeRune != CuChulainnCombatHelper.RuneType.ALGIZ) {
         performAlgizRune(entity);
         return true;
      }

      if (distance > 7.0 && hasLineOfSight && target != null && activeRune != CuChulainnCombatHelper.RuneType.ANSUZ) {
         performAnsuzRune(entity, target);
         return true;
      }

      if (distance <= 4.5 && activeRune != CuChulainnCombatHelper.RuneType.TIWAZ) {
         performTiwazRune(entity);
         return true;
      }

      if ((!hasLineOfSight || activeRune == CuChulainnCombatHelper.RuneType.NONE)
            && activeRune != CuChulainnCombatHelper.RuneType.LAGUZ) {
         performLaguzRune(entity);
         return true;
      }

      return false;
   }

   private void performRecastStance(ServantEntity entity) {
      entity.setCurrentMp(entity.getCurrentMp() - CU_RECAST_MP_COST);
      entity.triggerRuneCastAnimation();
      entity.setHealth(entity.getMaxHealth());
      java.util.List<net.minecraft.world.effect.MobEffectInstance> active = java.util.List.copyOf(entity.getActiveEffects());
      for (net.minecraft.world.effect.MobEffectInstance effect : active) {
         if (effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
            entity.removeEffect(effect.getEffect());
         }
      }
      CuChulainnCombatHelper.markRecastUsed(entity);
      if (entity.level() instanceof ServerLevel sl) {
         spawnRuneParticles(sl, entity, ModParticles.TIWAZ_RUNE.get(), 18, 0.45, 0.02);
         sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
            entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
            18, 0.4, 0.6, 0.4, 0.05);
      }
   }

   private void performLaguzRune(ServantEntity entity) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      entity.setCurrentMp(entity.getCurrentMp() - CU_RUNE_MP_COST);
      entity.triggerRuneCastAnimation();
      CuChulainnCombatHelper.markRuneCast(entity);
      CuChulainnCombatHelper.setRune(entity, CuChulainnCombatHelper.RuneType.LAGUZ, 200);
      spawnRuneParticles(sl, entity, ModParticles.LAGUZ_RUNE.get(), 16, 0.65, 0.01);
      sl.sendParticles(ParticleTypes.ENCHANT,
         entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(),
         20, 0.6, 0.8, 0.6, 0.04);
   }

   private void performTiwazRune(ServantEntity entity) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      entity.setCurrentMp(entity.getCurrentMp() - CU_RUNE_MP_COST);
      entity.triggerRuneCastAnimation();
      CuChulainnCombatHelper.markRuneCast(entity);
      CuChulainnCombatHelper.setRune(entity, CuChulainnCombatHelper.RuneType.TIWAZ, 300);
      spawnRuneParticles(sl, entity, ModParticles.TIWAZ_RUNE.get(), 18, 0.45, 0.02);
      sl.sendParticles(ParticleTypes.END_ROD,
         entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(),
         14, 0.45, 0.7, 0.45, 0.03);
   }

   private void performAlgizRune(ServantEntity entity) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      entity.setCurrentMp(entity.getCurrentMp() - CU_RUNE_MP_COST);
      entity.triggerRuneCastAnimation();
      CuChulainnCombatHelper.markRuneCast(entity);
      CuChulainnCombatHelper.setRune(entity, CuChulainnCombatHelper.RuneType.ALGIZ, 160);
      spawnRuneParticles(sl, entity, ModParticles.ALGIZ_RUNE.get(), 16, 0.55, 0.01);
      sl.sendParticles(ParticleTypes.WAX_ON,
         entity.getX(), entity.getY() + entity.getBbHeight() * 0.55, entity.getZ(),
         18, 0.5, 0.8, 0.5, 0.03);
   }

   private void performAnsuzRune(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      entity.setCurrentMp(entity.getCurrentMp() - CU_RUNE_MP_COST);
      entity.triggerRuneCastAnimation();
      CuChulainnCombatHelper.markRuneCast(entity);
      CuChulainnCombatHelper.setRune(entity, CuChulainnCombatHelper.RuneType.ANSUZ, 40);
      spawnRuneParticles(sl, entity, ModParticles.ANSUZ_RUNE.get(), 18, 0.45, 0.02);
      Vec3 start = entity.position().add(0.0, entity.getBbHeight() * 0.65, 0.0);
      Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
      for (double t = 0.0; t <= 1.0; t += 0.1) {
         Vec3 pos = start.lerp(end, t);
         sl.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 1, 0.03, 0.03, 0.03, 0.0);
      }
      net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.queueServerWork(8, () -> {
         if (!(entity.level() instanceof ServerLevel serverLevel) || !entity.isAlive()) {
            return;
         }
         Vec3 impact = target.isAlive()
            ? target.position().add(0.0, target.getBbHeight() * 0.5, 0.0)
            : end;
         AABB fireBox = new AABB(impact, impact).inflate(2.5);
         for (LivingEntity living : serverLevel.getEntitiesOfClass(LivingEntity.class, fireBox, e -> e != entity && e.isAlive() && !e.isAlliedTo(entity))) {
            living.hurt(entity.damageSources().magic(), 80.0F);
            living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), 100));
         }
         serverLevel.sendParticles(ParticleTypes.FLAME, impact.x, impact.y, impact.z, 24, 0.8, 0.5, 0.8, 0.05);
         serverLevel.sendParticles(ParticleTypes.SMOKE, impact.x, impact.y, impact.z, 16, 0.7, 0.35, 0.7, 0.03);
         serverLevel.playSound(null, impact.x, impact.y, impact.z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.1F, 0.8F);
      });
   }

   private void performGaeBolg(ServantEntity entity, LivingEntity target, boolean meleeMode) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      entity.setCurrentMp(entity.getCurrentMp() - 10.0);
      CuChulainnCombatHelper.markSingleGaeBolg(entity);
      CuChulainnCombatHelper.startGaeBolgWindup(entity, CuChulainnCombatHelper.GAE_BOLG_WINDUP_TICKS);
      entity.triggerGaeBolgThrowAnimation(CuChulainnCombatHelper.GAE_BOLG_WINDUP_TICKS);
      Vec3 fallbackAim = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
      net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.queueServerWork(CuChulainnCombatHelper.GAE_BOLG_WINDUP_TICKS, () -> {
         releaseSingleGaeBolg(entity, target, meleeMode, fallbackAim);
      });
   }

   private void performGaeBolgArmy(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      entity.setCurrentMp(entity.getCurrentMp() - 300.0);
      CuChulainnCombatHelper.markArmyGaeBolg(entity);
      CuChulainnCombatHelper.startGaeBolgWindup(entity, CuChulainnCombatHelper.GAE_BOLG_WINDUP_TICKS);
      entity.triggerGaeBolgThrowAnimation(CuChulainnCombatHelper.GAE_BOLG_WINDUP_TICKS);
      Vec3 fallbackAim = target.position().add(0.0, target.getBbHeight() * 0.3, 0.0);
      net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.queueServerWork(CuChulainnCombatHelper.GAE_BOLG_WINDUP_TICKS, () -> {
         releaseArmyGaeBolg(entity, target, fallbackAim);
      });
   }

   private void releaseSingleGaeBolg(ServantEntity entity, LivingEntity target, boolean preferMelee, Vec3 fallbackAim) {
      CuChulainnCombatHelper.clearGaeBolgWindup(entity);
      if (!(entity.level() instanceof ServerLevel sl) || !entity.isAlive()) {
         return;
      }

      LivingEntity resolvedTarget = target != null && target.isAlive() ? target : null;
      if (resolvedTarget == null) {
         return;
      }

      if (preferMelee && entity.distanceTo(resolvedTarget) <= 3.5) {
         applyFixedNoArmorDamage(entity, resolvedTarget, 250.0F);
         if (resolvedTarget.isAlive() && entity.getRandom().nextFloat() < CuChulainnCombatHelper.getDeathThornChance(resolvedTarget)) {
            resolvedTarget.invulnerableTime = 0;
            resolvedTarget.hurt(entity.damageSources().genericKill(), Float.MAX_VALUE);
            if (resolvedTarget.isAlive()) {
               resolvedTarget.setHealth(0.0F);
               resolvedTarget.die(entity.damageSources().genericKill());
            }
         }
         sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
            resolvedTarget.getX(), resolvedTarget.getY() + resolvedTarget.getBbHeight() * 0.5, resolvedTarget.getZ(),
            4, 0.0, 0.0, 0.0, 0.0);
         sl.sendParticles(ParticleTypes.CRIT,
            resolvedTarget.getX(), resolvedTarget.getY() + resolvedTarget.getBbHeight() * 0.55, resolvedTarget.getZ(),
            16, 0.25, 0.25, 0.25, 0.12);
         return;
      }

      GaeBulgProjectileEntity projectile = new GaeBulgProjectileEntity(sl, entity);
      projectile.setMode(GaeBulgProjectileEntity.Mode.SINGLE);
      projectile.setTrackedTarget(resolvedTarget);
      projectile.setPos(entity.getX(), entity.getY() + entity.getBbHeight() * 0.65, entity.getZ());
      Vec3 aim = resolvedTarget.position().add(0.0, resolvedTarget.getBbHeight() * 0.45, 0.0);
      if (fallbackAim != null && !resolvedTarget.isAlive()) {
         aim = fallbackAim;
      }
      Vec3 toTarget = aim.subtract(projectile.position()).normalize();
      projectile.shoot(toTarget.x, toTarget.y + 0.06, toTarget.z, 2.4F, 0.0F);
      sl.addFreshEntity(projectile);
   }

   private void releaseArmyGaeBolg(ServantEntity entity, LivingEntity target, Vec3 fallbackAim) {
      CuChulainnCombatHelper.clearGaeBolgWindup(entity);
      if (!(entity.level() instanceof ServerLevel sl) || !entity.isAlive()) {
         return;
      }

      LivingEntity resolvedTarget = target != null && target.isAlive() ? target : null;
      if (resolvedTarget == null && fallbackAim == null) {
         return;
      }

      GaeBulgProjectileEntity projectile = new GaeBulgProjectileEntity(sl, entity);
      projectile.setMode(GaeBulgProjectileEntity.Mode.ARMY);
      if (resolvedTarget != null) {
         projectile.setTrackedTarget(resolvedTarget);
      }
      projectile.setPos(entity.getX(), entity.getY() + entity.getBbHeight() * 0.65, entity.getZ());
      Vec3 aim = resolvedTarget != null
         ? resolvedTarget.position().add(0.0, resolvedTarget.getBbHeight() * 0.3, 0.0)
         : fallbackAim;
      Vec3 toTarget = aim.subtract(projectile.position()).normalize();
      projectile.shoot(toTarget.x, toTarget.y + 0.14, toTarget.z, 2.0F, 0.0F);
      sl.addFreshEntity(projectile);
   }

   private void applyFixedNoArmorDamage(ServantEntity attacker, LivingEntity target, float damage) {
      float before = target.getHealth();
      target.invulnerableTime = 0;
      target.hurt(attacker.damageSources().mobAttack(attacker), damage);
      target.invulnerableTime = 0;
      float desired = Math.max(0.0F, before - damage);
      if (target.getHealth() > desired) {
         target.setHealth(desired);
      }
   }

   private void spawnRuneParticles(ServerLevel level, ServantEntity entity, net.minecraft.core.particles.SimpleParticleType particle, int count, double spread, double speed) {
      level.sendParticles(
         particle,
         entity.getX(),
         entity.getY() + entity.getBbHeight() * 0.72,
         entity.getZ(),
         1,
         0.0,
         0.0,
         0.0,
         0.0
      );
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
