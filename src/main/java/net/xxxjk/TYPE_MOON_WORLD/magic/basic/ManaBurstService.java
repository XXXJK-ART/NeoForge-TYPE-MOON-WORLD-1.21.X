package net.xxxjk.TYPE_MOON_WORLD.magic.basic;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService;
import net.xxxjk.TYPE_MOON_WORLD.magic.WheelCastingModifierService;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;

public final class ManaBurstService {
   public static final String MAGIC_ID = "mana_burst";
   private static final String MODE_TAG = "TypeMoonManaBurstMode";
   private static final String LEVEL_TAG = "TypeMoonManaBurstLevel";
   private static final String NEXT_PAY_TAG = "TypeMoonManaBurstNextPay";
   private static final String INPUT_FORWARD_TAG = "TypeMoonManaBurstInputForward";
   private static final String INPUT_STRAFE_TAG = "TypeMoonManaBurstInputStrafe";
   private static final String INPUT_JUMP_TAG = "TypeMoonManaBurstInputJump";
   private static final String INPUT_SNEAK_TAG = "TypeMoonManaBurstInputSneak";
   private static final String INPUT_TICK_TAG = "TypeMoonManaBurstInputTick";
   private static final String JET_TICKS_TAG = "TypeMoonManaBurstJetTicks";
   private static final ResourceLocation DAMAGE_ID = ResourceLocation.fromNamespaceAndPath("typemoonworld", "mana_burst_damage");
   private static final ResourceLocation ATTACK_SPEED_ID = ResourceLocation.fromNamespaceAndPath("typemoonworld", "mana_burst_attack_speed");
   private static final ResourceLocation MOVE_ID = ResourceLocation.fromNamespaceAndPath("typemoonworld", "mana_burst_move");
   private static final ResourceLocation JUMP_ID = ResourceLocation.fromNamespaceAndPath("typemoonworld", "mana_burst_jump");
   private static final double[] UPKEEP = {15, 25, 35, 45, 55};
   private static final double[] WEAPON_DAMAGE = {6, 12, 18, 24, 30};
   private static final double[] WEAPON_SPEED = {0.15, 0.30, 0.45, 0.60, 0.75};
   private static final double[] BODY_DAMAGE = {5, 10, 15, 20, 25};
   private static final double[] BODY_MOVE = {0.15, 0.30, 0.45, 0.60, 0.75};
   private static final double[] BODY_JUMP = {0.15, 0.30, 0.45, 0.60, 0.75};
   private static final double[] DIRECT_COST = {150, 250, 350, 450, 550};
   private static final float[] DIRECT_DAMAGE = {40, 80, 120, 160, 200};
   private static final double[] DIRECT_RANGE = {20, 28, 36, 44, 52};
   private static final double[] JET_ACCEL = {0.03, 0.045, 0.06, 0.075, 0.09};
   private static final double[] JET_CAP = {0.38, 0.45, 0.53, 0.60, 0.68};
   private static final double[] JET_GROUND_LIFT = {0.32, 0.36, 0.40, 0.44, 0.48};
   private static final double[] JET_AIR_LIFT = {0.050, 0.060, 0.070, 0.080, 0.090};
   private static final double[] JET_UP_CAP = {0.38, 0.43, 0.48, 0.53, 0.58};
   private static final Vec3 JUMP_EXHAUST_DIRECTION = new Vec3(0.0, -1.0, 0.0);
   private static final Vec3 SNEAK_EXHAUST_DIRECTION = new Vec3(0.0, 1.0, 0.0);

   private ManaBurstService() {
   }

   public static MagicExecutionResult execute(net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext ctx) {
      ServerPlayer player = ctx.asServerPlayer();
      if (player == null) return MagicExecutionResult.FAILED;
      CompoundTag payload = currentPayload(ctx.vars());
      int mode = Math.max(0, Math.min(2, payload.getInt("mana_burst_mode")));
      int level = Math.max(1, Math.min(5, payload.contains("mana_burst_level") ? payload.getInt("mana_burst_level") : 1));
      int maxLevel = unlockedLevel(MagicProficiencyService.get(ctx.vars(), MAGIC_ID));
      if (level > maxLevel) level = maxLevel;
      if (mode == 2) {
         return direct(player, ctx.vars(), level);
      }
      return toggle(player, ctx.vars(), mode + 1, level);
   }

   private static CompoundTag currentPayload(TypeMoonWorldModVariables.PlayerVariables vars) {
      var entry = vars == null ? null : vars.getCurrentRuntimeWheelEntry();
      return entry != null && entry.presetPayload != null ? entry.presetPayload : new CompoundTag();
   }

   private static MagicExecutionResult toggle(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, int mode, int level) {
      CompoundTag data = player.getPersistentData();
      if (data.getInt(MODE_TAG) == mode) {
         clear(player);
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.mana_burst.off"), true);
         return MagicExecutionResult.SUCCESS;
      }
      clear(player);
      if (!ManaHelper.consumeManaStrict(player, WheelCastingModifierService.adjustManaCost(player, UPKEEP[level - 1]), false)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
         return MagicExecutionResult.FAILED;
      }
      data.putInt(MODE_TAG, mode);
      data.putInt(LEVEL_TAG, level);
      data.putLong(NEXT_PAY_TAG, player.level().getGameTime() + 20L);
      data.putInt(JET_TICKS_TAG, maxJetTicks(level));
      applyAttributes(player, mode, level);
      spawnToggleFx(player, mode, level);
      MagicProficiencyService.add(vars, MAGIC_ID, 0.25);
      vars.syncPlayerVariables(player);
      player.displayClientMessage(Component.translatable("message.typemoonworld.magic.mana_burst.on", level), true);
      return MagicExecutionResult.SUCCESS;
   }

   private static MagicExecutionResult direct(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, int level) {
      if (!ManaHelper.consumeManaStrict(player, WheelCastingModifierService.adjustManaCost(player, DIRECT_COST[level - 1]), false)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
         return MagicExecutionResult.FAILED;
      }
      if (player.level() instanceof ServerLevel serverLevel) {
         Vec3 eye = player.getEyePosition();
         Vec3 look = player.getLookAngle().normalize();
         double range = DIRECT_RANGE[level - 1];
         Vec3 end = eye.add(look.scale(range));
         Set<Integer> hit = new HashSet<>();
         for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(1.4), e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
            Vec3 rel = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(eye);
            double along = rel.dot(look);
            if (along < 0.0 || along > range || !hit.add(target.getId())) continue;
            double side = rel.subtract(look.scale(along)).length();
            if (side <= 1.1) {
               target.invulnerableTime = 0;
               target.hurt(player.damageSources().magic(), DIRECT_DAMAGE[level - 1]);
               target.invulnerableTime = 0;
               spawnDirectImpactFx(serverLevel, target.position().add(0.0, target.getBbHeight() * 0.5, 0.0), level);
            }
         }
         spawnDirectBeamFx(serverLevel, eye, look, range, level);
         destroyDirectBeamBlocks(serverLevel, player, eye, look, range, level);
         serverLevel.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.15F, 1.45F);
         serverLevel.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.95F, 0.85F + level * 0.05F);
      }
      player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), 40);
      MagicProficiencyService.add(vars, MAGIC_ID, 0.35);
      vars.syncPlayerVariables(player);
      return MagicExecutionResult.SUCCESS;
   }

   public static void tick(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      int mode = data.getInt(MODE_TAG);
      if (mode <= 0) return;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!player.isAlive() || vars.servant_card_transformed || vars.master_card_active) {
         clear(player);
         return;
      }
      int level = Math.max(1, Math.min(5, data.getInt(LEVEL_TAG)));
      applyAttributes(player, mode, level);
      long now = player.level().getGameTime();
      if (data.getLong(NEXT_PAY_TAG) <= now) {
         if (!ManaHelper.consumeManaStrict(player, WheelCastingModifierService.adjustManaCost(player, UPKEEP[level - 1]), false)) {
            clear(player);
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.mana_burst.no_mana"), true);
            return;
         }
         data.putLong(NEXT_PAY_TAG, now + 20L);
      }
      if (now % 10L == 0L) {
         spawnSustainFx(player, mode, level);
      }
      if (mode == 2) {
         applyBodyJetMovement(player, data, level, now);
      }
   }

   public static void setInput(ServerPlayer player, float forward, float strafe, boolean jump, boolean sneak) {
      if (player == null) {
         return;
      }
      CompoundTag data = player.getPersistentData();
      data.putFloat(INPUT_FORWARD_TAG, clampInput(forward));
      data.putFloat(INPUT_STRAFE_TAG, clampInput(strafe));
      data.putBoolean(INPUT_JUMP_TAG, jump);
      data.putBoolean(INPUT_SNEAK_TAG, sneak);
      data.putLong(INPUT_TICK_TAG, player.level().getGameTime());
   }

   public static void primeExternalJetMovement(ServerPlayer player, int level) {
      if (player == null) {
         return;
      }
      player.getPersistentData().putInt(JET_TICKS_TAG, maxJetTicks(level));
   }

   public static void tickExternalJetMovement(ServerPlayer player, int level) {
      if (player == null || !player.isAlive()) {
         return;
      }
      int clampedLevel = Math.max(1, Math.min(5, level));
      applyBodyJetMovement(player, player.getPersistentData(), clampedLevel, player.level().getGameTime());
   }

   public static void clearExternalJetMovement(ServerPlayer player) {
      if (player == null) {
         return;
      }
      CompoundTag data = player.getPersistentData();
      data.remove(INPUT_FORWARD_TAG);
      data.remove(INPUT_STRAFE_TAG);
      data.remove(INPUT_JUMP_TAG);
      data.remove(INPUT_SNEAK_TAG);
      data.remove(INPUT_TICK_TAG);
      data.remove(JET_TICKS_TAG);
   }

   public static void clear(ServerPlayer player) {
      if (player == null) return;
      player.getPersistentData().remove(MODE_TAG);
      player.getPersistentData().remove(LEVEL_TAG);
      player.getPersistentData().remove(NEXT_PAY_TAG);
      player.getPersistentData().remove(INPUT_FORWARD_TAG);
      player.getPersistentData().remove(INPUT_STRAFE_TAG);
      player.getPersistentData().remove(INPUT_JUMP_TAG);
      player.getPersistentData().remove(INPUT_SNEAK_TAG);
      player.getPersistentData().remove(INPUT_TICK_TAG);
      player.getPersistentData().remove(JET_TICKS_TAG);
      remove(player.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID);
      remove(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_ID);
      remove(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVE_ID);
      remove(player.getAttribute(Attributes.JUMP_STRENGTH), JUMP_ID);
   }

   private static void applyAttributes(ServerPlayer player, int mode, int level) {
      if (mode == 1) {
         update(player.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID, WEAPON_DAMAGE[level - 1], AttributeModifier.Operation.ADD_VALUE);
         update(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_ID, WEAPON_SPEED[level - 1], AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
         remove(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVE_ID);
         remove(player.getAttribute(Attributes.JUMP_STRENGTH), JUMP_ID);
      } else if (mode == 2) {
         update(player.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID, BODY_DAMAGE[level - 1], AttributeModifier.Operation.ADD_VALUE);
         update(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVE_ID, BODY_MOVE[level - 1], AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
         update(player.getAttribute(Attributes.JUMP_STRENGTH), JUMP_ID, BODY_JUMP[level - 1], AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
         remove(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_ID);
      }
   }

   private static int unlockedLevel(double proficiency) {
      return Math.max(1, Math.min(5, 1 + (int)Math.floor(proficiency / 20.0)));
   }

   private static void update(AttributeInstance attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
      if (attribute == null) return;
      AttributeModifier current = attribute.getModifier(id);
      if (current != null) attribute.removeModifier(id);
      if (amount != 0.0) attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
   }

   private static void remove(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null && attribute.getModifier(id) != null) attribute.removeModifier(id);
   }

   private static float clampInput(float input) {
      if (!Float.isFinite(input)) {
         return 0.0F;
      }
      return Math.max(-1.0F, Math.min(1.0F, input));
   }

   private static int maxJetTicks(int level) {
      return 6 + Math.max(1, Math.min(5, level)) * 2;
   }

   private static void applyBodyJetMovement(ServerPlayer player, CompoundTag data, int level, long now) {
      boolean grounded = player.onGround();
      if (grounded) {
         data.putInt(JET_TICKS_TAG, maxJetTicks(level));
      }
      boolean freshInput = now - data.getLong(INPUT_TICK_TAG) <= 6L;
      float forwardInput = freshInput ? data.getFloat(INPUT_FORWARD_TAG) : 0.0F;
      float strafeInput = freshInput ? data.getFloat(INPUT_STRAFE_TAG) : 0.0F;
      boolean jump = freshInput && data.getBoolean(INPUT_JUMP_TAG);
      boolean sneak = freshInput && data.getBoolean(INPUT_SNEAK_TAG);
      Vec3 motion = player.getDeltaMovement();
      Vec3 next = motion;

      Vec3 forward = player.getLookAngle();
      forward = new Vec3(forward.x, 0.0, forward.z);
      if (forward.lengthSqr() < 1.0E-6) {
         forward = Vec3.directionFromRotation(0.0F, player.getYRot());
         forward = new Vec3(forward.x, 0.0, forward.z);
      }
      forward = forward.normalize();
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      Vec3 desired = forward.scale(forwardInput).add(right.scale(strafeInput));
      if (desired.lengthSqr() > 1.0E-4) {
         desired = desired.normalize();
         double accel = JET_ACCEL[level - 1] * (player.onGround() ? 1.35 : 1.0);
         next = next.add(desired.scale(accel));
         next = clampHorizontal(next, JET_CAP[level - 1]);
      }

      if (sneak) {
         next = new Vec3(next.x, next.y - (0.08 + level * 0.018), next.z);
      } else if (jump) {
         int jetTicks = Math.max(0, data.getInt(JET_TICKS_TAG));
         if (jetTicks > 0) {
            double lift = grounded ? JET_GROUND_LIFT[level - 1] : JET_AIR_LIFT[level - 1];
            double minUp = grounded ? JET_GROUND_LIFT[level - 1] : next.y + lift;
            double maxUp = JET_UP_CAP[level - 1];
            next = new Vec3(next.x, Math.min(maxUp, Math.max(minUp, next.y + lift)), next.z);
            data.putInt(JET_TICKS_TAG, jetTicks - 1);
         } else if (next.y < -0.18) {
            next = new Vec3(next.x, next.y * 0.72, next.z);
         }
      }

      if (!next.equals(motion)) {
         player.setDeltaMovement(next);
         player.hurtMarked = true;
      }
      if ((desired.lengthSqr() > 1.0E-4 || jump || sneak) && player.level() instanceof ServerLevel serverLevel && now % 2L == 0L) {
         spawnJetFx(serverLevel, player, desired, jump, sneak, level);
      }
   }

   private static Vec3 clampHorizontal(Vec3 motion, double cap) {
      double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
      if (horizontal <= cap || horizontal <= 1.0E-6) {
         return motion;
      }
      double scale = cap / horizontal;
      return new Vec3(motion.x * scale, motion.y, motion.z * scale);
   }

   private static void spawnToggleFx(ServerPlayer player, int mode, int level) {
      if (player.level() instanceof ServerLevel serverLevel) {
         double y = player.getY() + player.getBbHeight() * 0.55;
         serverLevel.sendParticles(ParticleTypes.FLASH, player.getX(), y, player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
         serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), y, player.getZ(), 34 + level * 8, 0.55, 0.65, 0.55, 0.12);
         serverLevel.sendParticles(mode == 1 ? ParticleTypes.CRIT : ParticleTypes.END_ROD, player.getX(), y, player.getZ(), 18 + level * 5, 0.45, 0.55, 0.45, 0.08);
      }
      player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.85F, 1.0F + level * 0.08F);
   }

   private static void spawnSustainFx(ServerPlayer player, int mode, int level) {
      if (player.level() instanceof ServerLevel serverLevel) {
         double y = player.getY() + 0.25 + player.getBbHeight() * 0.42;
         serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), y, player.getZ(), 3 + level, 0.28, 0.38, 0.28, 0.02);
         if (mode == 1) {
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), y, player.getZ(), 2 + level, 0.25, 0.25, 0.25, 0.02);
         } else {
            serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.15, player.getZ(), 2 + level, 0.22, 0.08, 0.22, 0.018);
         }
      }
   }

   private static void spawnJetFx(ServerLevel level, ServerPlayer player, Vec3 desired, boolean jump, boolean sneak, int levelRank) {
      Vec3 base = player.position().add(0.0, 0.18, 0.0);
      boolean emitted = false;
      if (desired.lengthSqr() > 1.0E-4) {
         Vec3 horizontalExhaust = desired.normalize().scale(-1.0);
         spawnJetStream(level, base, horizontalExhaust, levelRank, false);
         emitted = true;
      }
      if (jump) {
         spawnJetStream(level, player.position().add(0.0, 0.12, 0.0), JUMP_EXHAUST_DIRECTION, levelRank, true);
         emitted = true;
      } else if (sneak) {
         spawnJetStream(level, player.position().add(0.0, player.getBbHeight() * 0.72, 0.0), SNEAK_EXHAUST_DIRECTION, levelRank, false);
         emitted = true;
      }
      if (!emitted) {
         spawnJetStream(level, base, player.getLookAngle().multiply(-1.0, 0.0, -1.0), levelRank, false);
      }
   }

   private static void spawnJetStream(ServerLevel level, Vec3 origin, Vec3 exhaustDirection, int levelRank, boolean intense) {
      Vec3 direction = exhaustDirection.lengthSqr() > 1.0E-4 ? exhaustDirection.normalize() : JUMP_EXHAUST_DIRECTION;
      int steps = intense ? 3 : 2;
      for (int i = 0; i < steps; i++) {
         double distance = 0.16 + i * 0.22;
         Vec3 p = origin.add(direction.scale(distance));
         double spread = Math.max(0.035, 0.12 - i * 0.018);
         level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 2 + levelRank, spread, spread * 0.7, spread, 0.04);
         if (i == steps - 1) {
            level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 1 + levelRank / 2, spread * 0.9, spread * 0.6, spread * 0.9, 0.026);
         }
         if (intense && i == 0) {
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 0.03, p.z, 1 + levelRank / 2, spread * 0.55, spread * 0.55, spread * 0.55, 0.03);
         }
      }
   }

   private static void spawnDirectBeamFx(ServerLevel level, Vec3 eye, Vec3 look, double range, int levelRank) {
      level.sendParticles(ParticleTypes.FLASH, eye.x + look.x * 0.8, eye.y + look.y * 0.8, eye.z + look.z * 0.8, 1, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.EXPLOSION, eye.x + look.x * 1.1, eye.y + look.y * 1.1, eye.z + look.z * 1.1, 2, 0.12, 0.12, 0.12, 0.0);
      for (double d = 0.8; d <= range; d += 0.75) {
         Vec3 p = eye.add(look.scale(d));
         double spread = 0.08 + levelRank * 0.025;
         level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 5 + levelRank, spread, spread, spread, 0.035);
         if (((int)(d * 10.0)) % 15 == 0) {
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 3 + levelRank, spread * 1.4, spread * 1.4, spread * 1.4, 0.035);
            level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 2 + levelRank / 2, spread * 1.8, spread * 1.2, spread * 1.8, 0.018);
         }
      }
      Vec3 end = eye.add(look.scale(range));
      spawnDirectImpactFx(level, end, levelRank);
   }

   private static void spawnDirectImpactFx(ServerLevel level, Vec3 pos, int levelRank) {
      level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 2 + levelRank / 2, 0.25, 0.25, 0.25, 0.0);
      level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y, pos.z, 14 + levelRank * 5, 0.45, 0.35, 0.45, 0.05);
      level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 18 + levelRank * 6, 0.32, 0.32, 0.32, 0.12);
   }

   private static void destroyDirectBeamBlocks(ServerLevel level, ServerPlayer player, Vec3 eye, Vec3 look, double range, int levelRank) {
      int maxBreak = 10 + levelRank * 8;
      double radius = 0.45 + levelRank * 0.12;
      float maxHardness = 0.9F + levelRank * 0.65F;
      Set<BlockPos> seen = new HashSet<>();
      int broken = 0;
      int r = (int)Math.ceil(radius);
      for (double d = 1.0; d <= range && broken < maxBreak; d += 0.65) {
         Vec3 center = eye.add(look.scale(d));
         BlockPos base = BlockPos.containing(center);
         for (BlockPos pos : BlockPos.betweenClosed(base.offset(-r, -r, -r), base.offset(r, r, r))) {
            if (broken >= maxBreak) {
               return;
            }
            BlockPos immutable = pos.immutable();
            if (seen.add(immutable) && center.distanceToSqr(Vec3.atCenterOf(immutable)) <= radius * radius && destroyDirectBlock(level, player, immutable, maxHardness)) {
               broken++;
            }
         }
      }
   }

   private static boolean destroyDirectBlock(ServerLevel level, ServerPlayer player, BlockPos pos, float maxHardness) {
      BlockState state = level.getBlockState(pos);
      float hardness = state.getDestroySpeed(level, pos);
      if (state.isAir()
         || state.is(Blocks.BEDROCK)
         || state.hasBlockEntity()
         || hardness < 0.0F
         || hardness > maxHardness
         || state.getExplosionResistance(level, pos, null) >= 1200.0F) {
         return false;
      }
      boolean destroyed = level.destroyBlock(pos, false, player);
      if (destroyed) {
         level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, 0.16, 0.16, 0.16, 0.03);
         level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.12, 0.12, 0.12, 0.035);
      }
      return destroyed;
   }
}
