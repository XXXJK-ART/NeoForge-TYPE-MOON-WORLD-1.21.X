package net.xxxjk.TYPE_MOON_WORLD.magic.church;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.BlackKeyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.church.ChurchExecutorEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.BlackKeyItem;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicLearningStrategy;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;

public final class BlackKeyMiracleService {
   public static final String THEOLOGY = "theology";
   public static final String BLACK_KEY_MAKING = "black_key_making";
   public static final String IRON_ARMOR_ACTION = "iron_armor_action";
   public static final String CREMATION_RITE = "cremation_rite";
   private static final String IRON_ARMOR_ACTIVE = "TypeMoonIronArmorActionActive";
   private static final String CREMATION_ACTIVE = "TypeMoonCremationRiteActive";
   private static final double IRON_ARMOR_HIT_MANA = 14.0;
   private static final double CREMATION_HIT_MANA = 12.0;

   private BlackKeyMiracleService() {
   }

   public static MagicExecutionResult executeKnowledgeOnly(MagicExecutionContext context) {
      Player player = context.asPlayer();
      if (player != null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.knowledge_only"), true);
      }
      return MagicExecutionResult.FAILED;
   }

   public static MagicExecutionResult toggleIronArmorAction(MagicExecutionContext context) {
      return toggle(context.entity(), context.vars(), IRON_ARMOR_ACTIVE, IRON_ARMOR_ACTION,
         "message.typemoonworld.magic.iron_armor_action.enabled",
         "message.typemoonworld.magic.iron_armor_action.disabled");
   }

   public static MagicExecutionResult castCremationRite(MagicExecutionContext context) {
      if (context.entity() instanceof Player player && player.isShiftKeyDown()) {
         return toggle(context.entity(), context.vars(), CREMATION_ACTIVE, CREMATION_RITE,
            "message.typemoonworld.magic.cremation_rite.enabled",
            "message.typemoonworld.magic.cremation_rite.disabled");
      }
      return detonateCremationRite(context.entity(), context.vars())
         ? MagicExecutionResult.SUCCESS
         : MagicExecutionResult.FAILED;
   }

   public static void onProjectileHit(BlackKeyProjectileEntity projectile, LivingEntity target, ItemStack carried) {
      Entity owner = projectile.getOwner();
      if (!(owner instanceof LivingEntity caster) || target == null || carried == null || !BlackKeyItem.isExpanded(carried)) return;
      if (tryTriggerIronArmorAction(caster)) {
         double proficiency = proficiency(caster, IRON_ARMOR_ACTION);
         float bonus = (float)(6.0 + proficiency * 0.16);
         if (BlackKeyProjectileEntity.isUndead(target)) bonus += 6.0F;
         target.invulnerableTime = 0;
         target.hurt(projectile.damageSources().thrown(projectile, owner), bonus);
         Vec3 away = target.position().subtract(caster.position());
         target.knockback(0.7 + proficiency * 0.01, -away.x, -away.z);
         addProficiency(caster, IRON_ARMOR_ACTION, 0.18);
      }
      if (tryTriggerCremationRite(caster)) {
         double proficiency = proficiency(caster, CREMATION_RITE);
         int duration = 80 + (int)Math.round(proficiency * 2.0);
         int amplifier = 0;
         MobEffectInstance existing = target.getEffect(ModMobEffects.CREMATION_RITE);
         if (existing != null) {
            amplifier = Math.min(7, existing.getAmplifier() + 1);
            duration = Math.max(duration, existing.getDuration() + 40);
         }
         target.igniteForSeconds(3.0F + (float)(proficiency * 0.04F));
         target.addEffect(new MobEffectInstance(ModMobEffects.CREMATION_RITE, duration, amplifier, false, true, true));
         addProficiency(caster, CREMATION_RITE, 0.16);
      }
   }

   public static boolean tryTriggerIronArmorAction(Entity owner) {
      if (!(owner instanceof LivingEntity caster) || !isIronArmorActive(caster)) return false;
      if (caster instanceof ServerPlayer player) {
         return ManaHelper.consumeManaStrict(player, IRON_ARMOR_HIT_MANA, false);
      }
      return caster instanceof ChurchExecutorEntity executor && executor.hasIronArmorAction();
   }

   public static boolean tryTriggerCremationRite(Entity owner) {
      if (!(owner instanceof LivingEntity caster) || !isCremationRiteActive(caster)) return false;
      if (caster instanceof ServerPlayer player) {
         return ManaHelper.consumeManaStrict(player, CREMATION_HIT_MANA, false);
      }
      return caster instanceof ChurchExecutorEntity executor && executor.hasCremationRite();
   }

   public static int maxBrokenBlocks(Entity owner) {
      if (!(owner instanceof LivingEntity caster) || !isIronArmorActive(caster)) {
         return net.xxxjk.TYPE_MOON_WORLD.combat.ChurchDeadApostleRules.BLACK_KEY_MAX_BROKEN_BLOCKS;
      }
      double proficiency = proficiency(caster, IRON_ARMOR_ACTION);
      return 3 + Mth.floor(proficiency / 14.0);
   }

   public static int blockBreakRadius(Entity owner) {
      if (!(owner instanceof LivingEntity caster) || !isIronArmorActive(caster)) return 0;
      return proficiency(caster, IRON_ARMOR_ACTION) >= 70.0 ? 2 : 1;
   }

   public static boolean canBreakBlock(Level level, BlockPos pos, BlockState state, Entity owner) {
      if (state == null || state.isAir()) return false;
      float hardness = state.getDestroySpeed(level, pos);
      boolean protectedBlock = state.is(Blocks.BEDROCK) || state.is(Blocks.END_PORTAL_FRAME);
      if (protectedBlock || state.hasBlockEntity()) return false;
      if (!(owner instanceof LivingEntity caster) || !isIronArmorActive(caster)) {
         return net.xxxjk.TYPE_MOON_WORLD.combat.ChurchDeadApostleRules.blackKeyCanBreakBlock(hardness, state.hasBlockEntity(), protectedBlock);
      }
      return hardness >= 0.0F && hardness <= 3.5F + (float)(proficiency(caster, IRON_ARMOR_ACTION) * 0.045F);
   }

   public static boolean isIronArmorActive(LivingEntity caster) {
      if (caster instanceof ChurchExecutorEntity executor) return executor.hasIronArmorAction();
      return caster != null && caster.getPersistentData().getBoolean(IRON_ARMOR_ACTIVE);
   }

   public static boolean isCremationRiteActive(LivingEntity caster) {
      if (caster instanceof ChurchExecutorEntity executor) return executor.hasCremationRite();
      return caster != null && caster.getPersistentData().getBoolean(CREMATION_ACTIVE);
   }

   private static MagicExecutionResult toggle(Entity entity, TypeMoonWorldModVariables.PlayerVariables vars, String tag, String magicId,
                                             String enabledKey, String disabledKey) {
      if (!(entity instanceof LivingEntity living) || vars == null || !MagicLearningStrategy.isLearned(vars, magicId)) {
         return MagicExecutionResult.FAILED;
      }
      boolean enabled = !living.getPersistentData().getBoolean(tag);
      living.getPersistentData().putBoolean(tag, enabled);
      if (entity instanceof Player player) {
         player.displayClientMessage(Component.translatable(enabled ? enabledKey : disabledKey), true);
      }
      vars.syncPlayerVariables(entity);
      return MagicExecutionResult.SUCCESS;
   }

   private static boolean detonateCremationRite(Entity entity, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!(entity instanceof LivingEntity caster) || !(caster.level() instanceof ServerLevel level) || vars == null) return false;
      double proficiency = MagicProficiencyService.get(vars, CREMATION_RITE);
      double manaCost = 18.0 + proficiency * 0.15;
      if (caster instanceof ServerPlayer player && !ManaHelper.consumeManaStrict(player, manaCost, false)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
         return false;
      }
      double scanRadius = 16.0 + proficiency * 0.16;
      int detonated = 0;
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(scanRadius),
         living -> living.isAlive() && living.hasEffect(ModMobEffects.CREMATION_RITE))) {
         MobEffectInstance effect = target.getEffect(ModMobEffects.CREMATION_RITE);
         if (effect == null) continue;
         int layers = effect.getAmplifier() + 1;
         float radius = (float)Math.min(6.0, 1.2 + layers * 0.35 + proficiency * 0.012);
         float damage = (float)(6.0 + layers * 2.5 + proficiency * 0.12);
         target.invulnerableTime = 0;
         target.hurt(target.damageSources().magic(), damage);
         level.explode(caster, target.getX(), target.getY(0.45), target.getZ(), radius, false, Level.ExplosionInteraction.NONE);
         target.removeEffect(ModMobEffects.CREMATION_RITE);
         detonated++;
      }
      if (detonated > 0) {
         addProficiency(caster, CREMATION_RITE, Math.min(2.0, detonated * 0.25));
         level.playSound(null, caster.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.85F, 0.9F);
         return true;
      }
      if (entity instanceof Player player) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.cremation_rite.no_targets"), true);
      }
      return false;
   }

   private static double proficiency(LivingEntity caster, String magicId) {
      if (caster instanceof ChurchExecutorEntity executor) {
         return executor.getMiracleProficiency(magicId);
      }
      TypeMoonWorldModVariables.PlayerVariables vars = caster.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return MagicProficiencyService.get(vars, magicId);
   }

   private static void addProficiency(LivingEntity caster, String magicId, double amount) {
      if (caster instanceof ServerPlayer player) {
         MagicProficiencyService.add(player, magicId, amount);
      }
   }
}
