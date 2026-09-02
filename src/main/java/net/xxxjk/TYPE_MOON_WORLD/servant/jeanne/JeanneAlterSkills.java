package net.xxxjk.TYPE_MOON_WORLD.servant.jeanne;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillUtils;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.typemoonworld.api.CardActionContext;
import net.xxxjk.typemoonworld.api.ExecutionResult;
import net.xxxjk.typemoonworld.api.ServantContext;

/** Server-authoritative Jeanne Alter actions kept independent of addon internals. */
public final class JeanneAlterSkills {
   private static final String ACTIVE = "JeanneAlterActive";
   private static final String AVENGER = "JeanneAlterAvengerB";
   private static final String OBLIVION = "JeanneAlterOblivionCorrectionA";
   private static final String REPLENISHMENT = "JeanneAlterSelfReplenishmentAPlus";
   private static final String GRUDGE = "JeanneAlterGrudgeStacks";
   private static final String DREAM_UNTIL = "JeanneAlterEphemeralDreamUntil";
   private static final String DREAM_READY = "JeanneAlterEphemeralDreamReadyAt";
   private static final String INFERNO_UNTIL = "JeanneAlterInfernoUntil";
   private static final String DEATH_LOCK_HEALTH = "JeanneAlterDeathLockHealth";
   private static final String DEATH_LOCK_UNTIL = "JeanneAlterDeathLockUntil";
   private static final String DEATH_LOCK_READY = "JeanneAlterDeathLockReadyAt";
   private static final int MAX_GRUDGE = 5;
   private static final int DEATH_LOCK_TICKS = 60 * 20;
   private static final int DEATH_LOCK_COOLDOWN = 20 * 60 * 20;
   private static final String FIRE_PILLAR_COOLDOWN = "JeanneAlterFirePillarUntil";
   private static final String CURSED_LANCE_COOLDOWN = "JeanneAlterCursedLanceUntil";
   private static final String SELF_MODIFICATION_COOLDOWN = "JeanneAlterSelfModificationUntil";
   private static final String DRAGON_WITCH_COOLDOWN = "JeanneAlterDragonWitchUntil";
   private static final String INFERNO_BODY_COOLDOWN = "JeanneAlterInfernoBodyUntil";
   private static final String EPHEMERAL_DREAM_COOLDOWN = "JeanneAlterEphemeralDreamUntil";
   private static final String NOBLE_PHANTASM_COOLDOWN = "JeanneAlterNpUntil";

   private JeanneAlterSkills() {}
   public static ExecutionResult avengerB(ServantContext context) {
      return initializePassive(context.caster(), AVENGER);
   }
   public static ExecutionResult oblivionCorrectionA(ServantContext context) {
      LivingEntity caster = context.caster();
      if (caster == null) return ExecutionResult.FAILED;
      CompoundTag data = caster.getPersistentData();
      data.putBoolean(OBLIVION, true);
      data.putBoolean("CritDamageBoostActive", true);
      data.putFloat("CritDamageMultiplier", 2.0F);
      data.putBoolean(ACTIVE, true);
      return ExecutionResult.SUCCESS;
   }
   public static ExecutionResult selfReplenishmentAPlus(ServantContext context) {
      return initializePassive(context.caster(), REPLENISHMENT);
   }

   /** Applies the same passive markers to a player transformed by the servant card. */
   public static void initializeCardPassives(ServerPlayer player) {
      if (player == null) return;
      CompoundTag data = player.getPersistentData();
      data.putBoolean(ACTIVE, true);
      data.putBoolean(AVENGER, true);
      data.putBoolean(OBLIVION, true);
      data.putBoolean(REPLENISHMENT, true);
      data.putBoolean("CritDamageBoostActive", true);
      data.putFloat("CritDamageMultiplier", 2.0F);
   }

   private static ExecutionResult initializePassive(LivingEntity caster, String marker) {
      if (caster == null) return ExecutionResult.FAILED;
      caster.getPersistentData().putBoolean(marker, true);
      caster.getPersistentData().putBoolean(ACTIVE, true);
      return ExecutionResult.SUCCESS;
   }
   public static ExecutionResult firePillar(CardActionContext c) {
      return targetAction(c, 72.0F, 22, 140, "jeanne_alter_fire_pillar", 3.5D);
   }

   public static ExecutionResult cursedLance(CardActionContext c) {
      return targetAction(c, 125.0F, 35, 240, "jeanne_alter_cursed_lance_impact", 5.0D);
   }

   public static ExecutionResult selfModification(CardActionContext c) {
      if (!canAffordCardAction(c.player(), 30.0D, false)) return ExecutionResult.FAILED;
      c.player().addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 1));
      playCardSkill(c.player(), "jeanne_alter_self_modification");
      return ExecutionResult.SUCCESS.withCost(30).withCooldown(500);
   }
   public static ExecutionResult dragonWitch(CardActionContext c) {
      if (!canAffordCardAction(c.player(), 42.0D, false)) return ExecutionResult.FAILED;
      for (LivingEntity ally : c.player().level().getEntitiesOfClass(LivingEntity.class,
            c.player().getBoundingBox().inflate(24.0D), e -> e != c.player() && e.isAlliedTo(c.player())))
         ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0));
      c.player().addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0));
      playCardSkill(c.player(), "jeanne_alter_dragon_witch");
      return ExecutionResult.SUCCESS.withCost(42).withCooldown(600);
   }
   public static ExecutionResult infernoBody(CardActionContext c) {
      if (!canAffordCardAction(c.player(), 45.0D, false)) return ExecutionResult.FAILED;
      c.player().addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 300, 0));
      c.player().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0));
      playCardSkill(c.player(), "jeanne_alter_inferno_body");
      return ExecutionResult.SUCCESS.withCost(45).withCooldown(600);
   }
   public static ExecutionResult ephemeralDream(CardActionContext c) {
      if (!canAffordCardAction(c.player(), 55.0D, false)) return ExecutionResult.FAILED;
      c.player().getPersistentData().putLong(DREAM_UNTIL, c.player().level().getGameTime() + 120L);
      c.player().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 4));
      c.player().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1));
      c.player().setHealth(Math.max(1.0F, c.player().getHealth() - c.player().getMaxHealth() * 0.15F));
      playCardSkill(c.player(), "jeanne_alter_ephemeral_dream");
      return ExecutionResult.SUCCESS.withCost(55).withCooldown(800);
   }
   public static ExecutionResult noblePhantasm(CardActionContext c) {
      ServerPlayer p = c.player();
      if (!canAffordCardAction(p, 220.0D, true)) return ExecutionResult.FAILED;
      if (!(p.level() instanceof ServerLevel level)) return ExecutionResult.FAILED;
      JeanneAlterVoice.noblePhantasm(p);
      VFXServerEffects.spawnReplayable(level, "jeanne_alter_np_invocation", p, 2.3F);
      for (LivingEntity target : p.level().getEntitiesOfClass(LivingEntity.class,
            p.getBoundingBox().inflate(18.0D), e -> e != p && !e.isAlliedTo(p))) {
         target.hurt(p.damageSources().indirectMagic(p, p), 80.0F);
         target.setRemainingFireTicks(160);
         VFXServerEffects.spawn(level, "jeanne_alter_np_execution_flames", target, 128.0D);
      }
      return ExecutionResult.SUCCESS.withCost(220).withCooldown(2400);
   }

   public static ExecutionResult combatFirePillar(ServantContext c) {
      if (shouldUseNoblePhantasm(c) || !canNpcCast(c, FIRE_PILLAR_COOLDOWN, 22.0D, 140, 2.5D, 24.0D)) {
         return ExecutionResult.NOT_HANDLED;
      }
      return castNpcArea(c, FIRE_PILLAR_COOLDOWN, 22.0D, 140, 28.0F, 3.5D, "jeanne_alter_fire_pillar");
   }

   public static ExecutionResult combatCursedLance(ServantContext c) {
      if (shouldUseNoblePhantasm(c) || !canNpcCast(c, CURSED_LANCE_COOLDOWN, 35.0D, 240, 5.0D, 36.0D)) {
         return ExecutionResult.NOT_HANDLED;
      }
      return castNpcArea(c, CURSED_LANCE_COOLDOWN, 35.0D, 240, 42.0F, 5.0D,
         "jeanne_alter_cursed_lance_impact");
   }

   public static ExecutionResult combatSelfModification(ServantContext c) {
      if (!(c.caster() instanceof ServantEntity servant) || c.target() == null
         || shouldUseNoblePhantasm(c)
         || c.distance() > 20.0D || servant.getCurrentMp() < 30.0D
         || servant.getPersistentData().getLong(SELF_MODIFICATION_COOLDOWN) > c.gameTick()
         || servant.getHealth() > servant.getMaxHealth() * 0.72F) return ExecutionResult.NOT_HANDLED;
      servant.getPersistentData().putLong(SELF_MODIFICATION_COOLDOWN, c.gameTick() + 500L);
      servant.setCurrentMp(Math.max(0.0D, servant.getCurrentMp() - 30.0D));
      servant.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 1));
      servant.triggerNamedActionAnimation("cast");
      playNpcSkill(servant, "jeanne_alter_self_modification");
      return ExecutionResult.SUCCESS;
   }

   public static ExecutionResult combatDragonWitch(ServantContext c) {
      if (!(c.caster() instanceof ServantEntity servant) || c.target() == null
         || shouldUseNoblePhantasm(c)
         || c.distance() > 24.0D || servant.getCurrentMp() < 42.0D
         || servant.getPersistentData().getLong(DRAGON_WITCH_COOLDOWN) > c.gameTick()
         || c.gameTick() % 160L != 0L) return ExecutionResult.NOT_HANDLED;
      servant.getPersistentData().putLong(DRAGON_WITCH_COOLDOWN, c.gameTick() + 600L);
      servant.setCurrentMp(Math.max(0.0D, servant.getCurrentMp() - 42.0D));
      servant.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0));
      for (LivingEntity ally : c.level().getEntitiesOfClass(LivingEntity.class,
         servant.getBoundingBox().inflate(24.0D), e -> e != servant && e.isAlliedTo(servant))) {
         ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0));
      }
      servant.triggerNamedActionAnimation("cast");
      playNpcSkill(servant, "jeanne_alter_dragon_witch");
      return ExecutionResult.SUCCESS;
   }

   public static ExecutionResult combatInfernoBody(ServantContext c) {
      if (!(c.caster() instanceof ServantEntity servant) || c.target() == null
         || shouldUseNoblePhantasm(c)
         || servant.getCurrentMp() < 45.0D
         || servant.getPersistentData().getLong(INFERNO_BODY_COOLDOWN) > c.gameTick()
         || servant.getHealth() > servant.getMaxHealth() * 0.58F) return ExecutionResult.NOT_HANDLED;
      servant.getPersistentData().putLong(INFERNO_BODY_COOLDOWN, c.gameTick() + 600L);
      servant.setCurrentMp(Math.max(0.0D, servant.getCurrentMp() - 45.0D));
      servant.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 300, 0));
      servant.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0));
      servant.triggerNamedActionAnimation("cast");
      playNpcSkill(servant, "jeanne_alter_inferno_body");
      return ExecutionResult.SUCCESS;
   }

   public static ExecutionResult combatEphemeralDream(ServantContext c) {
      if (!(c.caster() instanceof ServantEntity servant) || c.target() == null
         || shouldUseNoblePhantasm(c)
         || servant.getCurrentMp() < 55.0D
         || servant.getPersistentData().getLong(EPHEMERAL_DREAM_COOLDOWN) > c.gameTick()
         || c.gameTick() % 220L != 0L) return ExecutionResult.NOT_HANDLED;
      servant.getPersistentData().putLong(EPHEMERAL_DREAM_COOLDOWN, c.gameTick() + 800L);
      servant.setCurrentMp(Math.max(0.0D, servant.getCurrentMp() - 55.0D));
      servant.getPersistentData().putLong(DREAM_UNTIL, c.gameTick() + 120L);
      servant.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 4));
      servant.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1));
      servant.setHealth(Math.max(1.0F, servant.getHealth() - servant.getMaxHealth() * 0.15F));
      servant.triggerNamedActionAnimation("cast");
      playNpcSkill(servant, "jeanne_alter_ephemeral_dream");
      return ExecutionResult.SUCCESS;
   }

   public static ExecutionResult combatNoblePhantasm(ServantContext c) {
      if (!shouldUseNoblePhantasm(c)
         || !canNpcCast(c, NOBLE_PHANTASM_COOLDOWN, 220.0D, 2400, 0.0D, 32.0D)) {
         return ExecutionResult.NOT_HANDLED;
      }
      if (!(c.caster() instanceof ServantEntity servant) || !(c.level() instanceof ServerLevel level)) {
         return ExecutionResult.NOT_HANDLED;
      }
      servant.getPersistentData().putLong(NOBLE_PHANTASM_COOLDOWN, c.gameTick() + 2400L);
      servant.setCurrentMp(Math.max(0.0D, servant.getCurrentMp() - 220.0D));
      servant.getNavigation().stop();
      servant.faceToward(c.target().position());
      servant.triggerNamedActionAnimation("cast");
      JeanneAlterVoice.noblePhantasm(servant);
      VFXServerEffects.spawnReplayable(level, "jeanne_alter_np_invocation", servant, 2.3F);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
         servant.getBoundingBox().inflate(18.0D), e -> validTarget(servant, e))) {
         target.hurt(servant.damageSources().indirectMagic(servant, servant), 80.0F);
         target.setRemainingFireTicks(160);
         VFXServerEffects.spawn(level, "jeanne_alter_np_execution_flames", target, 128.0D);
      }
      return ExecutionResult.SUCCESS;
   }

   /** Shared event hooks for NPCs and transformed players. */
   @net.neoforged.bus.api.SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onIncomingDamage(LivingIncomingDamageEvent event) {
      LivingEntity victim = event.getEntity();
      if (victim.level().isClientSide()) return;
      DamageSource source = event.getSource();
      LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
      if (isJeanneAlter(victim)) {
         addGrudge(victim, event.getAmount());
         long now = victim.level().getGameTime();
         CompoundTag data = victim.getPersistentData();
         if (data.getLong(DEATH_LOCK_UNTIL) > now) {
            event.setCanceled(true);
            restoreDeathLockedHealth(victim, data);
            return;
         }
      }
      if (attacker != null && isJeanneAlter(attacker)) addGrudge(attacker, event.getAmount());
   }

   /** Prevents lethal post-mitigation damage before vanilla can enter the death path. */
   @net.neoforged.bus.api.SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onFinalDamage(LivingDamageEvent.Pre event) {
      LivingEntity victim = event.getEntity();
      if (victim.level().isClientSide() || !isJeanneAlter(victim)) return;
      CompoundTag data = victim.getPersistentData();
      long now = victim.level().getGameTime();
      if (data.getLong(DEATH_LOCK_UNTIL) > now) {
         event.setNewDamage(0.0F);
         restoreDeathLockedHealth(victim, data);
         return;
      }
      if (data.getLong(DEATH_LOCK_READY) > now) return;
      float effectiveHealth = victim.getHealth() + Math.max(0.0F, victim.getAbsorptionAmount());
      if (event.getNewDamage() + 1.0E-4F < effectiveHealth) return;
      activateDeathLock(victim, now, Math.max(1.0F, victim.getHealth()));
      event.setNewDamage(0.0F);
   }

   @net.neoforged.bus.api.SubscribeEvent
   public static void onDamageApplied(LivingDamageEvent.Post event) {
      LivingEntity victim = event.getEntity();
      if (victim.level().isClientSide()) return;
      LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity living ? living : null;
      if (attacker != null && isJeanneAlter(attacker)) {
         CompoundTag data = attacker.getPersistentData();
         long now = attacker.level().getGameTime();
         if (data.getLong(INFERNO_UNTIL) > now
               && data.getLong("JeanneAlterInfernoProcAt") <= now
               && attacker.getRandom().nextFloat() < 0.42F) {
            data.putLong("JeanneAlterInfernoProcAt", now + 12L);
            if (attacker.level() instanceof ServerLevel level) {
               VFXServerEffects.spawn(level, "jeanne_alter_inferno_pillar", victim, 96.0D);
               for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                     victim.getBoundingBox().inflate(2.8D), e -> validTarget(attacker, e))) {
                  target.hurt(attacker.damageSources().indirectMagic(attacker, attacker), 52.0F);
                  target.setRemainingFireTicks(100);
               }
            }
         }
      }
   }

   @net.neoforged.bus.api.SubscribeEvent
   public static void onTick(EntityTickEvent.Post event) {
      if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide()
            || !isJeanneAlter(entity)) return;
      CompoundTag data = entity.getPersistentData();
      long now = entity.level().getGameTime();
      if (data.getBoolean(REPLENISHMENT) && entity.tickCount % 20 == 0) {
         if (entity instanceof ServantEntity servant) {
            servant.setCurrentMp(Math.min(servant.getMaxMp(), servant.getCurrentMp() + 8.0D));
         } else if (entity instanceof ServerPlayer player) {
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            vars.servant_card_mana = Math.min(vars.servant_card_max_mana, vars.servant_card_mana + 8.0D);
            vars.syncMana(player);
         }
      }
      if (data.getLong(DEATH_LOCK_UNTIL) > now) {
         restoreDeathLockedHealth(entity, data);
      } else if (data.contains(DEATH_LOCK_UNTIL)) {
         data.remove(DEATH_LOCK_UNTIL);
         data.remove(DEATH_LOCK_HEALTH);
         entity.setHealth(1.0F);
      }
   }

   private static void activateDeathLock(LivingEntity victim, long now, float lockedHealth) {
      CompoundTag data = victim.getPersistentData();
      float health = Math.max(1.0F, Math.min(victim.getMaxHealth(), lockedHealth));
      data.putFloat(DEATH_LOCK_HEALTH, health);
      data.putLong(DEATH_LOCK_UNTIL, now + DEATH_LOCK_TICKS);
      data.putLong(DEATH_LOCK_READY, now + DEATH_LOCK_COOLDOWN);
      victim.setHealth(health);
      victim.invulnerableTime = Math.max(victim.invulnerableTime, 10);
   }

   private static void restoreDeathLockedHealth(LivingEntity victim, CompoundTag data) {
      float locked = Math.max(1.0F, Math.min(victim.getMaxHealth(), data.getFloat(DEATH_LOCK_HEALTH)));
      if (Math.abs(victim.getHealth() - locked) > 1.0E-4F) victim.setHealth(locked);
      victim.invulnerableTime = Math.max(victim.invulnerableTime, 1);
   }

   private static void addGrudge(LivingEntity entity, float damage) {
      if (damage <= 0.0F) return;
      CompoundTag data = entity.getPersistentData();
      data.putInt(GRUDGE, Math.min(MAX_GRUDGE, data.getInt(GRUDGE) + 1));
      if (entity instanceof ServantEntity servant) {
         servant.setCurrentMp(Math.min(servant.getMaxMp(), servant.getCurrentMp() + Math.min(8.0D, damage * 0.1D)));
      }
   }

   public static boolean isJeanneAlter(LivingEntity entity) {
      if (entity instanceof JeanneAlterEntity) return true;
      if (!(entity instanceof ServerPlayer player)) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && "jeanne_alter".equals(vars.servant_card_id);
   }

   public static boolean consumeGrudgeBurst(LivingEntity caster) {
      if (!isJeanneAlter(caster) || !(caster.level() instanceof ServerLevel level)) return false;
      CompoundTag data = caster.getPersistentData();
      int stacks = Math.min(MAX_GRUDGE, Math.max(0, data.getInt(GRUDGE)));
      if (stacks <= 0) return false;
      data.remove(GRUDGE);
      float damage = 50.0F + stacks * 20.0F;
      Vec3 center = caster.getBoundingBox().getCenter();
      VFXServerEffects.spawn(level, "jeanne_alter_grudge_burst", center);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
            caster.getBoundingBox().inflate(7.0D), e -> validTarget(caster, e))) {
         target.hurt(caster.damageSources().indirectMagic(caster, caster), damage);
         target.setRemainingFireTicks(100);
      }
      return true;
   }

   public static void clear(LivingEntity entity) {
      if (entity == null) return;
      CompoundTag data = entity.getPersistentData();
      for (String key : new String[]{ACTIVE, AVENGER, OBLIVION, REPLENISHMENT, GRUDGE,
            DREAM_UNTIL, DREAM_READY, INFERNO_UNTIL, DEATH_LOCK_HEALTH, DEATH_LOCK_UNTIL,
            DEATH_LOCK_READY, "JeanneAlterInfernoProcAt", "CritDamageBoostActive",
            "CritDamageMultiplier"}) data.remove(key);
   }

   public static boolean startFlagExplosion(ServerPlayer player) {
      if (player == null || !(player.level() instanceof ServerLevel level)) return false;
      LivingEntity target = ServantCardSkillUtils.findAutomaticLookTarget(player, 32.0D, 2.0D);
      if (target == null) return false;
      for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
            target.getBoundingBox().inflate(7.0D), e -> validTarget(player, e))) {
         victim.hurt(player.damageSources().indirectMagic(player, player), 50.0F);
         victim.setRemainingFireTicks(100);
      }
      VFXServerEffects.spawn(level, "jeanne_alter_flag_curse_finale", target, 96.0D);
      JeanneAlterVoice.skill(player);
      return true;
   }

   private static ExecutionResult targetAction(CardActionContext c, float damage, int cost, int cooldown,
                                                String effectId, double radius) {
      ServerPlayer p = c.player();
      if (!canAffordCardAction(p, cost, false)) return ExecutionResult.FAILED;
      LivingEntity target = ServantCardSkillUtils.findAutomaticLookTarget(p, 36.0D, 2.0D);
      if (target == null) return ExecutionResult.FAILED;
      if (!(p.level() instanceof ServerLevel level)) return ExecutionResult.FAILED;
      for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
         target.getBoundingBox().inflate(radius), e -> validTarget(p, e))) {
         victim.hurt(p.damageSources().indirectMagic(p, p), victim == target ? damage : damage * 0.45F);
         victim.setRemainingFireTicks(100);
      }
      JeanneAlterVoice.skill(p);
      VFXServerEffects.spawn(level, effectId, target, 128.0D);
      return ExecutionResult.SUCCESS.withCost(cost).withCooldown(cooldown);
   }

   private static ExecutionResult castNpcArea(ServantContext c, String cooldownTag, double cost, int cooldown,
                                               float damage, double radius, String effectId) {
      ServantEntity servant = (ServantEntity)c.caster();
      LivingEntity primary = c.target();
      ServerLevel level = (ServerLevel)c.level();
      servant.getPersistentData().putLong(cooldownTag, c.gameTick() + cooldown);
      servant.setCurrentMp(Math.max(0.0D, servant.getCurrentMp() - cost));
      servant.getNavigation().stop();
      servant.faceToward(primary.position());
      servant.triggerNamedActionAnimation("cast");
      JeanneAlterVoice.skill(servant);
      VFXServerEffects.spawn(level, effectId, primary, 128.0D);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
         primary.getBoundingBox().inflate(radius), e -> validTarget(servant, e))) {
         target.hurt(servant.damageSources().indirectMagic(servant, servant), target == primary ? damage : damage * 0.45F);
         target.setRemainingFireTicks(100);
      }
      return ExecutionResult.SUCCESS;
   }

   private static boolean canNpcCast(ServantContext c, String cooldownTag, double cost, int cooldown,
                                     double minimumRange, double maximumRange) {
      if (!(c.caster() instanceof ServantEntity servant) || !(c.level() instanceof ServerLevel)
         || c.target() == null || !validTarget(servant, c.target()) || !c.hasLineOfSight()
         || c.distance() < minimumRange || c.distance() > maximumRange || servant.getCurrentMp() < cost) {
         return false;
      }
      long until = servant.getPersistentData().getLong(cooldownTag);
      return until <= c.gameTick() && !servant.isHardCombatActionActive();
   }

   private static boolean shouldUseNoblePhantasm(ServantContext c) {
      if (!(c.caster() instanceof ServantEntity servant) || c.target() == null) return false;
      return servant.getHealth() <= servant.getMaxHealth() * 0.55F
         && servant.getCurrentMp() >= 220.0D
         && servant.getPersistentData().getLong(NOBLE_PHANTASM_COOLDOWN) <= c.gameTick();
   }

   private static boolean validTarget(LivingEntity caster, LivingEntity target) {
      return target != null && target.isAlive() && target != caster
         && !caster.isAlliedTo(target) && !target.isAlliedTo(caster);
   }

   private static void playCardSkill(ServerPlayer player, String effectId) {
      JeanneAlterVoice.skill(player);
      if (player.level() instanceof ServerLevel level) {
         VFXServerEffects.spawnReplayable(level, effectId, player, 1.2F);
      }
   }

   private static void playNpcSkill(ServantEntity servant, String effectId) {
      JeanneAlterVoice.skill(servant);
      if (servant.level() instanceof ServerLevel level) {
         VFXServerEffects.spawnReplayable(level, effectId, servant, 1.2F);
      }
   }

   private static boolean canAffordCardAction(ServerPlayer player, double cost, boolean noblePhantasm) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double required = noblePhantasm ? cost * MasterServantLinkService.noblePhantasmCostMultiplier(player, vars) : cost;
      if (Double.isFinite(required) && ServantCardManaService.availableForConsume(player, vars) + 1.0E-6 >= required) {
         return true;
      }
      player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
      return false;
   }
}
