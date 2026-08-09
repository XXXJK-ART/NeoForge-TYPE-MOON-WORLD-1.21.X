package net.xxxjk.TYPE_MOON_WORLD.magic.basic;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
      applyAttributes(player, mode, level);
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
            }
         }
         for (double d = 1.0; d <= range; d += 1.5) {
            Vec3 p = eye.add(look.scale(d));
            serverLevel.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 4, 0.15, 0.15, 0.15, 0.02);
         }
         serverLevel.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.45F);
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
      if (mode == 2 && !player.onGround()) {
         Vec3 motion = player.getDeltaMovement();
         if (player.isShiftKeyDown()) {
            player.setDeltaMovement(motion.x, motion.y - 0.08, motion.z);
         } else if (motion.y < -0.08) {
            player.setDeltaMovement(motion.x, motion.y * 0.72, motion.z);
         }
         player.hurtMarked = true;
      }
   }

   public static void clear(ServerPlayer player) {
      if (player == null) return;
      player.getPersistentData().remove(MODE_TAG);
      player.getPersistentData().remove(LEVEL_TAG);
      player.getPersistentData().remove(NEXT_PAY_TAG);
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
}
