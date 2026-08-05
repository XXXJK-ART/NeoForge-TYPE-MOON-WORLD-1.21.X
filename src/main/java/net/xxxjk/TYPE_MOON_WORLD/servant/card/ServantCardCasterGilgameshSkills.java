package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.Comparator;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.RoyalCannonProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GilgameshSlateItem;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CasterGilgameshCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.GilgameshDivineShield;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.joml.Vector3f;

public final class ServantCardCasterGilgameshSkills {
   public static final String AMMO_TAG = CasterGilgameshCombatHelper.AMMO_TAG;
   public static final String FIRING_TAG = CasterGilgameshCombatHelper.FIRING_TAG;
   public static final int MAX_AMMO = CasterGilgameshCombatHelper.MAX_AMMO;
   public static final int STARTING_AMMO = CasterGilgameshCombatHelper.STARTING_AMMO;
   public static final int CANNON_SHOTS_PER_ROUND = CasterGilgameshCombatHelper.CANNON_SHOTS_PER_ROUND;
   public static final float WAND_DOMINION_MULTIPLIER = 1.20F;

   private static final String INITIALIZED_TAG = "ServantCardCasterGilgameshInitialized";
   private static final String LAST_CANNON_ROUND = "ServantCardCasterGilgameshLastCannonRound";
   private static final String LEADER_UNTIL = "ServantCardCasterGilgameshLeaderUntil";
   private static final String RETURN_UNTIL = "ServantCardCasterGilgameshReturnUntil";
   private static final String WORKSHOP_ACTIVE = "ServantCardCasterGilgameshWorkshopActive";
   private static final String WORKSHOP_X = "ServantCardCasterGilgameshWorkshopX";
   private static final String WORKSHOP_Y = "ServantCardCasterGilgameshWorkshopY";
   private static final String WORKSHOP_Z = "ServantCardCasterGilgameshWorkshopZ";
   private static final String LAST_HEAL = "ServantCardCasterGilgameshLastHeal";
   private static final String LAST_MP = "ServantCardCasterGilgameshLastMp";
   private static final String LAST_SHIELD = "ServantCardCasterGilgameshLastShield";
   private static final ResourceLocation LEADER_ATTACK_ID =
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_caster_gilgamesh_leader_attack");
   private static final ResourceLocation WORKSHOP_ARMOR_ID =
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_caster_gilgamesh_workshop_armor");
   private static final DustParticleOptions GOLD =
      new DustParticleOptions(new Vector3f(1.0F, 0.72F, 0.12F), 1.05F);

   private ServantCardCasterGilgameshSkills() {
   }

   public static void initialize(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data = player.getPersistentData();
      boolean changed = false;
      if (!data.getBoolean(INITIALIZED_TAG)) {
         data.putBoolean(INITIALIZED_TAG, true);
         data.putInt(AMMO_TAG, STARTING_AMMO);
         data.putBoolean(FIRING_TAG, false);
         data.putLong(LAST_CANNON_ROUND, -20L);
         changed = true;
      }
      if (!data.contains(AMMO_TAG)) {
         data.putInt(AMMO_TAG, STARTING_AMMO);
         changed = true;
      }
      if (!data.contains(FIRING_TAG)) {
         data.putBoolean(FIRING_TAG, false);
         changed = true;
      }
      if (syncRoyalCannonAmmo(vars, data)) {
         changed = true;
      }
      if (changed) vars.syncServantCardRuntime(player);
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!(player.level() instanceof ServerLevel level) || !"gilgamesh_caster".equals(vars.servant_card_id)) {
         clear(player);
         return;
      }
      CompoundTag data = player.getPersistentData();
      initialize(player, vars);
      boolean ammoChanged = false;
      if (player.tickCount % 20 == 0) {
         int oldAmmo = Math.max(0, data.getInt(AMMO_TAG));
         int newAmmo = Math.min(MAX_AMMO, oldAmmo + 1);
         if (newAmmo != oldAmmo) {
            data.putInt(AMMO_TAG, newAmmo);
            ammoChanged = syncRoyalCannonAmmo(vars, data);
         }
      }
      tickLeader(player, level, data);
      tickWorkshop(player, vars, level, data);
      GilgameshDivineShield.tick(player);
      syncDivineShieldCooldown(player, vars);
      if (data.getBoolean(FIRING_TAG) && level.getGameTime() >= data.getLong(LAST_CANNON_ROUND) + 20L) {
         if (fireCannonRound(player, vars, true)) {
            data.putLong(LAST_CANNON_ROUND, level.getGameTime());
         } else {
            data.putBoolean(FIRING_TAG, false);
            vars.syncServantCardRuntime(player);
         }
      }
      if (player.tickCount % 20 == 0) {
         vars.syncMana(player);
         if (ammoChanged) {
            vars.syncServantCardRuntime(player);
         }
      }
   }

   public static void clear(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.putBoolean(FIRING_TAG, false);
      data.remove(INITIALIZED_TAG);
      data.remove(WORKSHOP_ACTIVE);
      data.remove(LEADER_UNTIL);
      data.remove(RETURN_UNTIL);
      GilgameshDivineShield.clear(player);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.ATTACK_DAMAGE), LEADER_ATTACK_ID);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.ARMOR), WORKSHOP_ARMOR_ID);
      if (player.level() instanceof ServerLevel level) {
         for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(36.0), LivingEntity::isAlive)) {
            ServantCardSkillUtils.remove(living.getAttribute(Attributes.ATTACK_DAMAGE), LEADER_ATTACK_ID);
            ServantCardSkillUtils.remove(living.getAttribute(Attributes.ARMOR), WORKSHOP_ARMOR_ID);
         }
      }
   }

   private static void syncDivineShieldCooldown(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      String oldCooldowns = vars.servant_card_skill_cooldowns;
      String oldEnds = vars.servant_card_skill_cooldown_ends;
      ServantCardTransformManager.setSkillCooldownUntil(player, vars, 8, GilgameshDivineShield.cooldownUntil(player));
      if (!java.util.Objects.equals(oldCooldowns, vars.servant_card_skill_cooldowns)
         || !java.util.Objects.equals(oldEnds, vars.servant_card_skill_cooldown_ends)) {
         vars.syncPlayerVariables(player);
      }
   }

   public static boolean performSlateBasic(ServerPlayer player) {
      return fireSlateShots(player, player.isCrouching() ? GilgameshSlateItem.CROUCH_SHOT_COUNT : 1);
   }

   public static boolean performSlateVolley(ServerPlayer player) {
      return fireSlateShots(player, GilgameshSlateItem.CROUCH_SHOT_COUNT);
   }

   public static boolean performLeader(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      CompoundTag data = player.getPersistentData();
      long until = level.getGameTime() + 25L * 20L;
      data.putLong(LEADER_UNTIL, until);
      for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(30.0), e -> isAlly(player, e))) {
         ServantCardSkillUtils.addOrReplaceMultiplied(ally.getAttribute(Attributes.ATTACK_DAMAGE), LEADER_ATTACK_ID, 0.25);
      }
      level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 42, 2.2, 1.1, 2.2, 0.04);
      return true;
   }

   public static boolean performReturn(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      long until = level.getGameTime() + 20L * 20L;
      player.getPersistentData().putLong(RETURN_UNTIL, until);
      for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(20.0), e -> isAlly(player, e))) {
         ally.getPersistentData().putLong("CasterGilgameshCritBuffUntil", until);
      }
      level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 36, 1.7, 1.0, 1.7, 0.08);
      return true;
   }

   public static boolean performItemCreation(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      CompoundTag data = player.getPersistentData();
      long now = level.getGameTime();
      if (player.getHealth() <= player.getMaxHealth() * 0.60F && now >= data.getLong(LAST_HEAL) + 20L * 20L) {
         player.heal(player.getMaxHealth() * 0.40F);
         data.putLong(LAST_HEAL, now);
         level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.5, 0.6, 0.5, 0.04);
         return true;
      }
      if (vars.servant_card_mana <= vars.servant_card_max_mana * 0.50 && now >= data.getLong(LAST_MP) + 20L * 20L) {
         vars.servant_card_mana = Math.min(vars.servant_card_max_mana, vars.servant_card_mana + vars.servant_card_max_mana * 0.30);
         vars.syncMana(player);
         data.putLong(LAST_MP, now);
         level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 0.9, player.getZ(), 28, 0.5, 0.6, 0.5, 0.08);
         return true;
      }
      if (now >= data.getLong(LAST_SHIELD) + 20L * 20L) {
         player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 5 * 20, 4, false, true, true));
         data.putLong(LAST_SHIELD, now);
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.6, 0.7, 0.6, 0.04);
         return true;
      }
      player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.cooldown", "20.0"), true);
      return false;
   }

   public static boolean performWorkshop(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      CompoundTag data = player.getPersistentData();
      data.putBoolean(WORKSHOP_ACTIVE, true);
      data.putDouble(WORKSHOP_X, player.getX());
      data.putDouble(WORKSHOP_Y, player.getY());
      data.putDouble(WORKSHOP_Z, player.getZ());
      level.sendParticles(GOLD, player.getX(), player.getY() + 0.25, player.getZ(), 90, 3.5, 0.18, 3.5, 0.05);
      level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 0.9, player.getZ(), 44, 2.4, 0.9, 2.4, 0.08);
      return true;
   }

   public static boolean performCannonCalibration(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      return fireCannonRound(player, vars, false);
   }

   public static boolean toggleRoyalCannon(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data = player.getPersistentData();
      if (data.getBoolean(FIRING_TAG)) {
         data.putBoolean(FIRING_TAG, false);
         vars.syncServantCardRuntime(player);
         return true;
      }
      initialize(player, vars);
      if (!hasCannonResources(player, vars) || findCannonTarget(player) == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
         return false;
      }
      data.putBoolean(FIRING_TAG, true);
      data.putLong(LAST_CANNON_ROUND, -20L);
      vars.syncServantCardRuntime(player);
      return true;
   }

   static boolean hasCannonResources(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data = player.getPersistentData();
      return data.getInt(AMMO_TAG) >= CANNON_SHOTS_PER_ROUND && vars.servant_card_mana >= CANNON_SHOTS_PER_ROUND;
   }

   private static boolean fireSlateShots(ServerPlayer player, int shots) {
      if (!(player.level() instanceof ServerLevel level) || shots <= 0) return false;
      LivingEntity target = ServantCardSkillUtils.findAutomaticLookTarget(player, 32.0, 1.6);
      Vec3 look = player.getLookAngle().normalize();
      for (int i = 0; i < shots; i++) {
         Vec3 direction = look;
         if (shots > 1) {
            direction = look.add((player.getRandom().nextDouble() - 0.5) * 0.16,
               (player.getRandom().nextDouble() - 0.5) * 0.10,
               (player.getRandom().nextDouble() - 0.5) * 0.16).normalize();
         }
         Vec3 start = player.getEyePosition().add(direction.scale(0.7));
         float damage = GilgameshSlateItem.MIN_SHOT_DAMAGE
            + player.getRandom().nextInt((int)(GilgameshSlateItem.MAX_SHOT_DAMAGE - GilgameshSlateItem.MIN_SHOT_DAMAGE) + 1);
         RoyalCannonProjectileEntity shot = new RoyalCannonProjectileEntity(level, player, start, direction, damage);
         shot.setHomingTarget(target);
         level.addFreshEntity(shot);
         level.sendParticles(GOLD, start.x, start.y, start.z, 8, 0.08, 0.08, 0.08, 0.04);
      }
      return true;
   }

   private static boolean fireCannonRound(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, boolean sustained) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      initialize(player, vars);
      LivingEntity target = findCannonTarget(player);
      if (target == null || !hasCannonResources(player, vars)) return false;
      if (!ServantCardManaService.consume(player, vars, CANNON_SHOTS_PER_ROUND)) return false;
      CompoundTag data = player.getPersistentData();
      data.putInt(AMMO_TAG, Math.max(0, data.getInt(AMMO_TAG) - CANNON_SHOTS_PER_ROUND));
      if (syncRoyalCannonAmmo(vars, data)) {
         vars.syncServantCardRuntime(player);
      }
      CasterGilgameshCombatHelper.spawnRoyalCannonVolley(
         level, player, target, CANNON_SHOTS_PER_ROUND,
         CasterGilgameshCombatHelper.CANNON_DAMAGE_PER_SHOT,
         WAND_DOMINION_MULTIPLIER, true
      );
      if (!sustained) {
         data.putLong(LAST_CANNON_ROUND, level.getGameTime());
      }
      return true;
   }

   private static boolean syncRoyalCannonAmmo(TypeMoonWorldModVariables.PlayerVariables vars, CompoundTag data) {
      int ammo = Math.min(MAX_AMMO, Math.max(0, data.getInt(AMMO_TAG)));
      if (vars.servant_card_royal_cannon_ammo == ammo) return false;
      vars.servant_card_royal_cannon_ammo = ammo;
      return true;
   }

   private static LivingEntity findCannonTarget(ServerPlayer player) {
      LivingEntity lookTarget = ServantCardSkillUtils.findAutomaticLookTarget(player, 56.0, 2.2);
      if (lookTarget != null) return lookTarget;
      return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(40.0),
         e -> e.isAlive() && e != player && !player.isAlliedTo(e) && !ServantMasterTargeting.isContractMaster(player, e)
            && !EntityUtils.isImmunePlayerTarget(e))
         .stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
   }

   private static void tickLeader(ServerPlayer player, ServerLevel level, CompoundTag data) {
      long until = data.getLong(LEADER_UNTIL);
      if (until <= 0L || level.getGameTime() < until) return;
      data.remove(LEADER_UNTIL);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(36.0), LivingEntity::isAlive)) {
         ServantCardSkillUtils.remove(living.getAttribute(Attributes.ATTACK_DAMAGE), LEADER_ATTACK_ID);
      }
   }

   private static void tickWorkshop(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, ServerLevel level, CompoundTag data) {
      if (!data.getBoolean(WORKSHOP_ACTIVE) || player.tickCount % 10 != 0) return;
      Vec3 center = new Vec3(data.getDouble(WORKSHOP_X), data.getDouble(WORKSHOP_Y), data.getDouble(WORKSHOP_Z));
      boolean playerInside = player.position().distanceToSqr(center) <= 15.0 * 15.0;
      if (playerInside && vars.servant_card_mana < vars.servant_card_max_mana) {
         double bonusRegen = ServantCardManaService.regenPerSecondFor("gilgamesh_caster") * 0.30 / 2.0;
         vars.servant_card_mana = Math.min(vars.servant_card_max_mana, vars.servant_card_mana + bonusRegen);
      }
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(18.0), e -> e.isAlive() && isAlly(player, e))) {
         if (living.position().distanceToSqr(center) <= 15.0 * 15.0) {
            ServantCardSkillUtils.addOrReplaceMultiplied(living.getAttribute(Attributes.ARMOR), WORKSHOP_ARMOR_ID, 0.20);
            if (living instanceof ServerPlayer allyPlayer) {
               TypeMoonWorldModVariables.PlayerVariables allyVars = allyPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
               if (allyVars.servant_card_transformed && allyVars.servant_card_mana < allyVars.servant_card_max_mana) {
                  allyVars.servant_card_mana = Math.min(allyVars.servant_card_max_mana,
                     allyVars.servant_card_mana + ServantCardManaService.regenPerSecondFor(allyVars.servant_card_id) * 0.30 / 2.0);
               }
            }
         } else {
            ServantCardSkillUtils.remove(living.getAttribute(Attributes.ARMOR), WORKSHOP_ARMOR_ID);
         }
      }
   }

   private static boolean isAlly(ServerPlayer player, LivingEntity living) {
      return living == player || living.isAlliedTo(player);
   }
}
