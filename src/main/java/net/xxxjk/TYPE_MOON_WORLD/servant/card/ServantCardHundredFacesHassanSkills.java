package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.DirkProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.HundredFacesOpenScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.HundredFacesStateMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HundredFacesHassanEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HundredFacesHassanPersonaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.hundredfaces.HundredFacesHassanRules;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class ServantCardHundredFacesHassanSkills {
   public static final String COMMAND_TAG = "HundredFacesCardCommand";
   public static final String ATTACK_ENABLED_TAG = "HundredFacesCardAttackEnabled";
   public static final String FORCE_CONCEALMENT_TAG = "HundredFacesCardForceConcealment";
   public static final String PERSONAL_COMMAND_TAG = "HundredFacesCardPersonalCommand";
   public static final String PERSONAL_ATTACK_TAG = "HundredFacesCardPersonalAttack";
   public static final String PERSONAL_CONCEALMENT_TAG = "HundredFacesCardPersonalConcealment";
   private static final String GLOBAL_ATTACK_ENABLED_TAG = "HundredFacesCardGlobalAttackEnabled";
   private static final String GLOBAL_CONCEALMENT_TAG = "HundredFacesCardGlobalConcealment";
   public static final String GLOBAL_COMMAND_TAG = "HundredFacesCardGlobalCommand";
   public static final String PERSONA_MODE_TAG = "HundredFacesPersonaMode";
   private static final String RETREAT_UNTIL_TAG = "HundredFacesCardRetreatUntil";
   private static final ResourceLocation RETREAT_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "hundred_faces_retreat_speed");
   private static final String LAST_STATE_COUNT_TAG = "HundredFacesCardLastStateCount";
   private static final String BODY_TRANSFER_ACTIVE_TAG = "HundredFacesCardBodyTransferActive";
   private static final ResourceLocation SPLIT_HEALTH_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "hundred_faces_split_health");
   private static final ResourceLocation SPLIT_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "hundred_faces_split_attack");
   private static final ResourceLocation SPLIT_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "hundred_faces_split_armor");
   private static final double MAIN_BASE_MANA_REGEN = HundredFacesHassanRules.MAIN_BASE_MANA / 180.0;
   private static final double MAIN_MIN_MANA_REGEN = HundredFacesHassanRules.MAIN_MIN_MANA / 300.0;
   public static final int COMMAND_FREE = 0;
   public static final int COMMAND_HOLD = 1;
   public static final int COMMAND_FOLLOW = 2;
   public static final int COMMAND_RECALL = 10;
   public static final int COMMAND_SCATTER = 11;
   public static final int COMMAND_ATTACK_TOGGLE = 12;
   public static final int COMMAND_CONCEALMENT_TOGGLE = 13;
   public static final int COMMAND_CLEAR_PERSONAL = 14;
   public static final int COMMAND_MODE_SCOUT = 20;
   public static final int COMMAND_MODE_ASSASSINATION = 21;
   public static final int COMMAND_MODE_THROWING = 22;
   public static final int COMMAND_MODE_POISON = 23;
   public static final int COMMAND_MODE_INDUCE = 24;
   public static final int COMMAND_MODE_SUPPORT = 25;
   private static final Map<ServerPlayer, HundredFacesStateMessage> LAST_SENT_STATES = new WeakHashMap<>();
   private static final Map<UUID, PendingSummon> PENDING_SUMMONS = new HashMap<>();

   private static final class PendingSummon {
      final ServerLevel level;
      final UUID target;
      final List<Vec3> positions;
      int nextIndex;
      long nextBatchTick;

      PendingSummon(ServerLevel level, LivingEntity target, List<Vec3> positions, long now) {
         this.level = level;
         this.target = target == null ? null : target.getUUID();
         this.positions = List.copyOf(positions);
         this.nextBatchTick = now;
      }

      int remaining() { return this.positions.size() - this.nextIndex; }
   }

   private ServantCardHundredFacesHassanSkills() {
   }

   public static void initialize(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data = player.getPersistentData();
      data.putBoolean(GLOBAL_ATTACK_ENABLED_TAG, true);
      data.putBoolean(GLOBAL_CONCEALMENT_TAG, false);
      data.putInt(GLOBAL_COMMAND_TAG, COMMAND_FOLLOW);
      data.putInt(PERSONA_MODE_TAG, HundredFacesHassanEntity.PersonaMode.ASSASSINATION.ordinal());
      data.putInt(LAST_STATE_COUNT_TAG, 0);
      data.putInt(HundredFacesHassanRules.TAG_TOTAL_SPLIT_COUNT, 0);
      LAST_SENT_STATES.remove(player);
      applyBodySplitAttributes(player, vars);
      sendState(player, true);
   }

   public static void clear(ServerPlayer player) {
      LAST_SENT_STATES.remove(player);
      cancelPendingSummon(player, true);
      HundredFacesPresenceService.set(player, false);
      AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (speed != null) speed.removeModifier(RETREAT_SPEED_ID);
      if (player.getServer() != null) {
         discardOwnedPersonas(player);
      }
      CompoundTag data = player.getPersistentData();
      data.remove(GLOBAL_ATTACK_ENABLED_TAG);
      data.remove(GLOBAL_CONCEALMENT_TAG);
      data.remove(GLOBAL_COMMAND_TAG);
      data.remove(PERSONA_MODE_TAG);
      data.remove(RETREAT_UNTIL_TAG);
      data.remove(LAST_STATE_COUNT_TAG);
      data.remove(HundredFacesHassanRules.TAG_TOTAL_SPLIT_COUNT);
      removeBodySplitAttributes(player);
      sendIfSupported(player, new HundredFacesStateMessage(0, true));
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!isActiveCard(player)) return;
      ServantCardConcealmentHelper.tick(player);
      HundredFacesPresenceService.tick(player);
      tickBattleRetreat(player);
      tickPendingSummon(player, vars);
      ServantCardHassanSkills.tick(player, vars);
      applyBodySplitAttributes(player, vars);
      if (player.tickCount % 20 == 0) {
         if (player.level() instanceof ServerLevel level) rescaleOwnedPersonas(level, player.getUUID());
         sendState(player, false);
      }
   }

   public static boolean summonOrOpenMenu(ServerPlayer player, boolean menu) {
      if (!isActiveCard(player)) return false;
      if (menu) {
         sendIfSupported(player, new HundredFacesOpenScreenMessage(0, List.of()));
         return true;
      }
      return summon(player, 1);
   }

   public static boolean summon(ServerPlayer player, int requested) {
      if (!isActiveCard(player) || !(player.level() instanceof ServerLevel level)) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      int alreadySplit = totalSplitCount(player);
      if (PENDING_SUMMONS.containsKey(player.getUUID())) return false;
      int count = HundredFacesHassanRules.affordableTotalSummonCount(vars.servant_card_mana, alreadySplit, requested);
      if (count <= 0) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.hundred_faces.no_capacity_or_mp"), true);
         return false;
      }
      LivingEntity inheritedTarget = ServantCardSkillUtils.findAutomaticLookTarget(player, 32.0, 2.0);
      List<Vec3> positions = findSafeSpawnPositions(player, level, count);
      if (positions.isEmpty()) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.hundred_faces.no_safe_position"), true);
         return false;
      }
      int payable = positions.size();
      if (!ServantCardManaService.consume(player, vars, payable * HundredFacesHassanRules.MP_PER_PERSONA)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
         return false;
      }
      if (payable >= 20) {
         level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.HUNDRED_FACES_HASSAN_VOICE_NP.get(),
            SoundSource.PLAYERS, 1.15F, 0.98F + (player.getRandom().nextFloat() - 0.5F) * 0.06F);
      }
      PENDING_SUMMONS.put(player.getUUID(), new PendingSummon(level, inheritedTarget, positions, level.getGameTime()));
      return true;
   }

   public static boolean openGlobalCommand(ServerPlayer player) {
      if (!isActiveCard(player)) return false;
      sendIfSupported(player, new HundredFacesOpenScreenMessage(1, List.of()));
      return true;
   }

   public static boolean openSingleCommand(ServerPlayer player) {
      if (!isActiveCard(player) || !(player.level() instanceof ServerLevel level)) return false;
      HundredFacesHassanPersonaEntity persona = findLookedAtOwnedPersona(player, level);
      if (persona == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.hundred_faces.no_persona_target"), true);
         return false;
      }
      sendIfSupported(player, new HundredFacesOpenScreenMessage(2, List.of(targetInfo(persona))));
      return true;
   }

   public static boolean openSingleCommand(ServerPlayer player, HundredFacesHassanPersonaEntity persona) {
      if (!isActiveCard(player) || persona == null || !player.getUUID().equals(persona.getOwnerUuid())) return false;
      sendIfSupported(player, new HundredFacesOpenScreenMessage(2, List.of(targetInfo(persona))));
      return true;
   }

   public static boolean openSwitch(ServerPlayer player) {
      if (!isActiveCard(player) || !(player.level() instanceof ServerLevel level)) return false;
      List<HundredFacesOpenScreenMessage.Target> targets = ownedPersonas(level, player.getUUID(), true).stream()
         .sorted(Comparator.comparingDouble(player::distanceToSqr))
         .map(ServantCardHundredFacesHassanSkills::targetInfo)
         .toList();
      sendIfSupported(player, new HundredFacesOpenScreenMessage(3, targets));
      return true;
   }

   public static void setGlobalCommand(ServerPlayer player, int command) {
      setGlobalCommand(player, command, -1);
   }

   public static void setGlobalCommand(ServerPlayer player, int command, int limit) {
      if (ServantMasterCarryService.isCarryingMaster(player)) return;
      if (!isActiveCard(player) || !(player.level() instanceof ServerLevel level)) return;
      boolean all = limit <= 0;
      List<HundredFacesHassanPersonaEntity> personas = scopedCommandPersonas(level, player, limit);
      if (isPersonaModeCommand(command)) {
         HundredFacesHassanEntity.PersonaMode mode = modeForCommand(command);
         if (all) player.getPersistentData().putInt(PERSONA_MODE_TAG, mode.ordinal());
         for (HundredFacesHassanPersonaEntity persona : personas) persona.setPersonaMode(mode);
      } else if (command == COMMAND_ATTACK_TOGGLE) {
         boolean enabled = all || personas.isEmpty() ? !globalAttackEnabled(player) : !personaAttackEnabled(personas.get(0));
         if (all) player.getPersistentData().putBoolean(GLOBAL_ATTACK_ENABLED_TAG, enabled);
         for (HundredFacesHassanPersonaEntity persona : personas) {
            if (!all || !hasPersonalAttackOverride(persona)) setPersonaAttackEnabled(persona, enabled, !all);
         }
         player.displayClientMessage(Component.translatable(enabled
            ? "message.typemoonworld.hundred_faces.attack_on"
            : "message.typemoonworld.hundred_faces.attack_off"), true);
      } else if (command == COMMAND_CONCEALMENT_TOGGLE) {
         boolean enabled = all || personas.isEmpty() ? !globalConcealmentEnabled(player) : !personaForceConcealment(personas.get(0));
         if (all) player.getPersistentData().putBoolean(GLOBAL_CONCEALMENT_TAG, enabled);
         for (HundredFacesHassanPersonaEntity persona : personas) {
            if (!all || !hasPersonalConcealmentOverride(persona)) setPersonaForceConcealment(persona, enabled, !all);
         }
         player.displayClientMessage(Component.translatable(enabled
            ? "message.typemoonworld.hundred_faces.concealment_on"
            : "message.typemoonworld.hundred_faces.concealment_off"), true);
      } else if (command == COMMAND_RECALL) {
         if (all) player.getPersistentData().putInt(GLOBAL_COMMAND_TAG, COMMAND_FOLLOW);
         for (HundredFacesHassanPersonaEntity persona : personas) {
            if (all && hasPersonalCommandOverride(persona)) continue;
            setPersonaCommand(persona, COMMAND_FOLLOW, !all);
            if (persona.distanceToSqr(player) > 48.0 * 48.0) teleportPersonaNearOwner(persona, player);
         }
      } else if (command == COMMAND_SCATTER) {
         if (all) player.getPersistentData().putInt(GLOBAL_COMMAND_TAG, COMMAND_FREE);
         for (HundredFacesHassanPersonaEntity persona : personas) {
            if (all && hasPersonalCommandOverride(persona)) continue;
            setPersonaCommand(persona, COMMAND_SCATTER, !all);
         }
      } else if (command == COMMAND_FREE) {
         if (all) player.getPersistentData().putInt(GLOBAL_COMMAND_TAG, COMMAND_FREE);
         for (HundredFacesHassanPersonaEntity persona : personas) {
            if (!all || !hasPersonalCommandOverride(persona)) setPersonaCommand(persona, COMMAND_FREE, !all);
         }
      } else if (command == COMMAND_CLEAR_PERSONAL) {
         int globalCommand = globalMovementCommand(player);
         boolean attack = globalAttackEnabled(player);
         boolean concealment = globalConcealmentEnabled(player);
         for (HundredFacesHassanPersonaEntity persona : personas) {
            setPersonaCommand(persona, globalCommand, false);
            setPersonaAttackEnabled(persona, attack, false);
            setPersonaForceConcealment(persona, concealment, false);
         }
         player.displayClientMessage(Component.translatable("message.typemoonworld.hundred_faces.personal_cleared"), true);
      }
      sendState(player, true);
   }

   public static void setPersonaCommand(ServerPlayer player, int entityId, int command) {
      if (ServantMasterCarryService.isCarryingMaster(player)) return;
      if (!isActiveCard(player) || !(player.level() instanceof ServerLevel level)) return;
      Entity entity = level.getEntity(entityId);
      if (!(entity instanceof HundredFacesHassanPersonaEntity persona) || !player.getUUID().equals(persona.getOwnerUuid())) return;
      if (isPersonaModeCommand(command)) {
         persona.setPersonaMode(modeForCommand(command));
      } else if (command == COMMAND_ATTACK_TOGGLE) setPersonaAttackEnabled(persona, !personaAttackEnabled(persona), true);
      else if (command == COMMAND_CONCEALMENT_TOGGLE) setPersonaForceConcealment(persona, !personaForceConcealment(persona), true);
      else setPersonaCommand(persona, sanitizeMovementCommand(command), true);
      sendState(player, true);
   }

   public static boolean switchTo(ServerPlayer player, int entityId) {
      if (!isActiveCard(player) || !(player.level() instanceof ServerLevel level)) return false;
      Entity entity = level.getEntity(entityId);
      if (!(entity instanceof HundredFacesHassanPersonaEntity target) || !target.isAlive() || !player.getUUID().equals(target.getOwnerUuid())) return false;
      Vec3 old = player.position();
      float oldYaw = player.getYRot();
      float oldPitch = player.getXRot();
      HundredFacesHassanEntity.PersonaMode mode = target.getPersonaMode();
      int command = personaCommand(target);
      boolean attack = personaAttackEnabled(target);
      boolean forceConcealment = personaForceConcealment(target);
      boolean personalCommand = hasPersonalCommandOverride(target);
      boolean personalAttack = hasPersonalAttackOverride(target);
      boolean personalConcealment = hasPersonalConcealmentOverride(target);
      Vec3 targetPos = target.position();
      float targetYaw = target.getYRot();
      float targetPitch = target.getXRot();
      HundredFacesHassanPersonaEntity replacement = ModEntities.HUNDRED_FACES_HASSAN_PERSONA.get().create(level);
      if (replacement == null) return false;
      replacement.moveTo(old.x, old.y, old.z, oldYaw, oldPitch);
      replacement.initialize(player, null, mode, Math.max(1, totalSplitCount(player)));
      setPersonaCommand(replacement, command, personalCommand);
      setPersonaAttackEnabled(replacement, attack, personalAttack);
      setPersonaForceConcealment(replacement, forceConcealment, personalConcealment);
      if (!level.addFreshEntity(replacement)) {
         replacement.discard();
         return false;
      }
      target.discard();
      level.sendParticles(ParticleTypes.SQUID_INK, old.x, old.y + 0.9, old.z, 18, 0.35, 0.5, 0.35, 0.02);
      level.sendParticles(ParticleTypes.SMOKE, targetPos.x, targetPos.y + 0.9, targetPos.z, 18, 0.35, 0.5, 0.35, 0.03);
      level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.75F, 1.4F);
      player.teleportTo(targetPos.x, targetPos.y, targetPos.z);
      player.setYRot(targetYaw);
      player.setXRot(targetPitch);
      ServantCardConcealmentHelper.apply(player, 50);
      sendState(player, true);
      return true;
   }

   public static boolean tryTransferBodyOnLethalDamage(ServerPlayer player) {
      if (!isActiveCard(player) || player.getServer() == null) return false;
      CompoundTag data = player.getPersistentData();
      if (data.getBoolean(BODY_TRANSFER_ACTIVE_TAG)) return false;
      data.putBoolean(BODY_TRANSFER_ACTIVE_TAG, true);
      try {
         HundredFacesHassanPersonaEntity vessel = null;
         for (ServerLevel level : player.getServer().getAllLevels()) {
            for (HundredFacesHassanPersonaEntity persona : ownedPersonas(level, player.getUUID(), true)) {
               if (vessel == null
                  || (persona.level() == player.level() && vessel.level() != player.level())
                  || persona.getHealth() > vessel.getHealth()) {
                  vessel = persona;
               }
            }
         }
         if (vessel == null || !(vessel.level() instanceof ServerLevel targetLevel)) return false;

         ServerLevel oldLevel = player.serverLevel();
         Vec3 old = player.position();
         Vec3 targetPos = vessel.position();
         float yaw = vessel.getYRot();
         float pitch = vessel.getXRot();
         vessel.discard();

         player.teleportTo(targetLevel, targetPos.x, targetPos.y, targetPos.z, yaw, pitch);
         player.setHealth(Math.max(1.0F, Math.min(player.getMaxHealth(), player.getMaxHealth() * 0.35F)));
         player.clearFire();
         player.invulnerableTime = 40;
         ServantCardConcealmentHelper.apply(player, 50);

         targetLevel.sendParticles(ParticleTypes.SOUL, targetPos.x, targetPos.y + 0.6, targetPos.z,
            24, 0.45, 0.7, 0.45, 0.04);
         targetLevel.sendParticles(ParticleTypes.POOF, targetPos.x, targetPos.y + 0.5, targetPos.z,
            18, 0.45, 0.45, 0.45, 0.04);
         targetLevel.playSound(null, BlockPos.containing(targetPos), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.3F);
         oldLevel.sendParticles(ParticleTypes.SQUID_INK, old.x, old.y + 0.8, old.z,
            24, 0.45, 0.55, 0.45, 0.03);
         player.displayClientMessage(Component.translatable("message.typemoonworld.hundred_faces.body_transfer"), true);
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         applyBodySplitAttributes(player, vars);
         sendState(player, true);
         return true;
      } finally {
         data.remove(BODY_TRANSFER_ACTIVE_TAG);
      }
   }

   public static boolean performPresenceConcealment(ServerPlayer player) {
      return HundredFacesPresenceService.toggle(player);
   }

   public static boolean createDirk(ServerPlayer player) {
      if (!isActiveCard(player)) return false;
      ItemStack mainhand = player.getMainHandItem();
      ItemStack offhand = player.getOffhandItem();
      if (offhand.is(ModItems.DIRK_SMALL_KNIFE.get())) {
         offhand.setDamageValue(0);
         return true;
      }
      if (mainhand.is(ModItems.DIRK_SMALL_KNIFE.get())) {
         mainhand.setDamageValue(0);
         return true;
      }
      if (!HundredFacesHassanRules.shouldGenerateDirk(mainhand.isEmpty(), offhand.isEmpty())) return false;
      ItemStack dirk = new ItemStack(ModItems.DIRK_SMALL_KNIFE.get());
      ServantCardTransformManager.markGeneratedItem(dirk, true, false);
      player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, dirk);
      return true;
   }

   public static boolean performDirkVolley(ServerPlayer player) {
      if (!isActiveCard(player) || !hasUsableDirks(player, 3)) return false;
      net.xxxjk.TYPE_MOON_WORLD.servant.concealment.ServantConcealment.breakConcealment(player);
      ServantCardHassanSkills.performDirkThrow(player);
      return true;
   }

   public static boolean performKnifeFeint(ServerPlayer player) {
      LivingEntity target = ServantCardSkillUtils.findLookTarget(player, 9.0, 1.8);
      if (!isValidTarget(player, target) || player.distanceTo(target) < 2.5 || !player.hasLineOfSight(target)
         || !hasUsableDirks(player, 1)) return false;
      DirkProjectileEntity projectile = new DirkProjectileEntity(player.level(), player);
      projectile.setDamage(10.0F);
      projectile.setMaxDistance(18.0);
      projectile.setOnHitEffects(35, 35);
      projectile.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
      Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0).subtract(projectile.position()).normalize();
      projectile.shoot(aim.x, aim.y, aim.z, 2.2F, 0.0F);
      player.level().addFreshEntity(projectile);
      player.getOffhandItem().hurtAndBreak(1, player, EquipmentSlot.OFFHAND);
      net.xxxjk.TYPE_MOON_WORLD.servant.concealment.ServantConcealment.breakConcealment(player);
      return true;
   }

   public static boolean performShadowStep(ServerPlayer player) {
      LivingEntity target = ServantCardSkillUtils.findLookTarget(player, 8.0, 1.8);
      if (!isValidTarget(player, target) || player.distanceTo(target) < 3.0 || !player.hasLineOfSight(target)) return false;
      Vec3 destination = findSafeBackstabPosition(player, target);
      if (destination == null) return false;
      Vec3 old = player.position();
      float oldYaw = player.getYRot();
      float oldPitch = player.getXRot();
      player.teleportTo(destination.x, destination.y, destination.z);
      player.setDeltaMovement(Vec3.ZERO);
      player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.position());
      if (!dealPhysicalDamage(player, target, 1.45F, "shadow_step")) {
         player.teleportTo(old.x, old.y, old.z);
         player.setYRot(oldYaw);
         player.setXRot(oldPitch);
         return false;
      }
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true, true));
      net.xxxjk.TYPE_MOON_WORLD.servant.concealment.ServantConcealment.breakConcealment(player);
      return true;
   }

   public static boolean performShadowLunge(ServerPlayer player) {
      LivingEntity target = ServantCardSkillUtils.findLookTarget(player, 6.5, 1.8);
      if (!isValidTarget(player, target) || player.distanceTo(target) < 2.0 || !player.hasLineOfSight(target)) return false;
      Vec3 dir = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
      if (dir.lengthSqr() < 1.0E-4) return false;
      dir = dir.normalize();
      Vec3 destination = findLungeDestination(player, target, dir, 2.4);
      if (destination == null) return false;
      if (!dealPhysicalDamage(player, target, 1.05F, "shadow_lunge")) return false;
      player.teleportTo(destination.x, destination.y, destination.z);
      player.setDeltaMovement(dir.scale(1.65).add(0.0, 0.18, 0.0));
      player.hurtMarked = true;
      target.push(dir.x * 0.55, 0.18, dir.z * 0.55);
      target.hurtMarked = true;
      net.xxxjk.TYPE_MOON_WORLD.servant.concealment.ServantConcealment.breakConcealment(player);
      return true;
   }

   public static boolean performBattleRetreat(ServerPlayer player) {
      if (!isActiveCard(player)) return false;
      cleanseHarmfulEffects(player);
      player.getPersistentData().putLong(RETREAT_UNTIL_TAG,
         player.level().getGameTime() + HundredFacesHassanRules.RETREAT_DURATION_TICKS);
      AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (speed != null) {
         AttributeModifier existing = speed.getModifier(RETREAT_SPEED_ID);
         if (existing != null) speed.removeModifier(RETREAT_SPEED_ID);
         speed.addTransientModifier(new AttributeModifier(RETREAT_SPEED_ID, 0.4,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
      }
      return true;
   }

   public static boolean hasActiveAttack(HundredFacesHassanPersonaEntity persona) {
      return personaAttackEnabled(persona);
   }

   public static int personaCommand(HundredFacesHassanPersonaEntity persona) {
      return sanitizeMovementCommand(persona.getPersistentData().getInt(COMMAND_TAG));
   }

   public static boolean personaAttackEnabled(HundredFacesHassanPersonaEntity persona) {
      CompoundTag data = persona.getPersistentData();
      return !data.contains(ATTACK_ENABLED_TAG) || data.getBoolean(ATTACK_ENABLED_TAG);
   }

   public static boolean personaForceConcealment(HundredFacesHassanPersonaEntity persona) {
      return persona.getPersistentData().getBoolean(FORCE_CONCEALMENT_TAG);
   }

   public static boolean isActiveCard(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && "hundred_faces_hassan".equals(vars.servant_card_id);
   }

   private static boolean isPersonaModeCommand(int command) {
      return command >= COMMAND_MODE_SCOUT && command <= COMMAND_MODE_SUPPORT;
   }

   private static HundredFacesHassanEntity.PersonaMode modeForCommand(int command) {
      return command == COMMAND_MODE_THROWING ? HundredFacesHassanEntity.PersonaMode.THROWING
         : HundredFacesHassanEntity.PersonaMode.ASSASSINATION;
   }

   public static boolean isOwnedBy(HundredFacesHassanPersonaEntity persona, ServerPlayer player) {
      return persona != null && player != null && player.getUUID().equals(persona.getOwnerUuid());
   }

   public static void notifyPersonaDeath(HundredFacesHassanPersonaEntity persona, ServerPlayer player) {
      if (!isActiveCard(player)) return;
      player.displayClientMessage(Component.translatable("message.typemoonworld.hundred_faces.persona_lost",
         persona.blockPosition().getX(), persona.blockPosition().getY(), persona.blockPosition().getZ()), true);
      sendState(player, true);
   }

   public static void sendState(ServerPlayer player, boolean force) {
      if (!isActiveCard(player)) return;
      int count = 0;
      if (player.getServer() != null) {
         for (ServerLevel level : player.getServer().getAllLevels()) count += countOwnedPersonas(level, player.getUUID());
      }
      HundredFacesStateMessage state = new HundredFacesStateMessage(count, globalAttackEnabled(player));
      if (force || !state.equals(LAST_SENT_STATES.get(player)) || player.tickCount % 100 == 0) {
         sendIfSupported(player, state);
         LAST_SENT_STATES.put(player, state);
      }
      player.getPersistentData().putInt(LAST_STATE_COUNT_TAG, count);
   }

   public static int countOwnedPersonas(ServerLevel level, UUID ownerUuid) {
      return ownedPersonas(level, ownerUuid, true).size();
   }

   public static int totalSplitCount(ServerPlayer player) {
      return Math.max(0, Math.min(HundredFacesHassanRules.MAX_PERSONAS,
         player.getPersistentData().getInt(HundredFacesHassanRules.TAG_TOTAL_SPLIT_COUNT)));
   }

   public static void sendIfSupported(ServerPlayer player, CustomPacketPayload payload) {
      ModNetwork.sendToPlayer(player, payload);
   }

   private static void tickPendingSummon(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      PendingSummon pending = PENDING_SUMMONS.get(player.getUUID());
      if (pending == null) return;
      if (!isActiveCard(player) || !player.isAlive() || player.level() != pending.level) {
         cancelPendingSummon(player, true);
         return;
      }
      long now = pending.level.getGameTime();
      if (now < pending.nextBatchTick) return;
      Entity trackedTarget = pending.target == null ? null : pending.level.getEntity(pending.target);
      LivingEntity target = trackedTarget instanceof LivingEntity living && living.isAlive() ? living : null;
      int spawned = 0;
      int attempted = 0;
      int splitCount = totalSplitCount(player);
      float yaw = player.getYRot();
      HundredFacesHassanEntity.PersonaMode mode = selectedPersonaMode(player);
      while (attempted < HundredFacesHassanRules.MAX_SUMMON_BATCH && pending.nextIndex < pending.positions.size()) {
         Vec3 pos = pending.positions.get(pending.nextIndex++);
         attempted++;
         HundredFacesHassanPersonaEntity persona = ModEntities.HUNDRED_FACES_HASSAN_PERSONA.get().create(pending.level);
         if (persona == null) {
            refundPersonaMp(player, vars, 1);
            continue;
         }
         persona.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
         persona.initialize(player, target, mode, splitCount + spawned + 1);
         setPersonaCommand(persona, globalMovementCommand(player), false);
         setPersonaAttackEnabled(persona, globalAttackEnabled(player), false);
         setPersonaForceConcealment(persona, globalConcealmentEnabled(player), false);
         if (!pending.level.addFreshEntity(persona)) {
            persona.discard();
            refundPersonaMp(player, vars, 1);
            continue;
         }
         spawned++;
         pending.level.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y + 0.9, pos.z, 10, 0.35, 0.55, 0.35, 0.02);
      }
      if (spawned > 0) {
         addTotalSplitCount(player, spawned);
         pending.level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.8, 0.8, 0.8, 0.03);
         rescaleOwnedPersonas(pending.level, player.getUUID());
         sendState(player, true);
      }
      if (pending.remaining() <= 0) {
         PENDING_SUMMONS.remove(player.getUUID());
         player.displayClientMessage(Component.translatable("message.typemoonworld.hundred_faces.summoned", pending.positions.size()), true);
      } else {
         pending.nextBatchTick = now + 2;
      }
   }

   private static List<Vec3> findSafeSpawnPositions(ServerPlayer player, ServerLevel level, int requested) {
      List<Vec3> positions = new ArrayList<>(requested);
      Vec3 forward = horizontal(player.getLookAngle());
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      for (int index = 0; index < requested; index++) {
         Vec3 found = null;
         for (int attempt = 0; attempt < 32 && found == null; attempt++) {
            int ring = attempt / 8;
            double angle = index * 2.399963229728653 + attempt * 0.7853981633974483;
            double radius = 1.8 + ring * 1.25;
            Vec3 offset = forward.scale(Math.cos(angle) * radius).add(right.scale(Math.sin(angle) * radius));
            BlockPos base = BlockPos.containing(player.getX() + offset.x, player.getY(), player.getZ() + offset.z);
            for (int dy = -2; dy <= 2 && found == null; dy++) {
               BlockPos feet = base.offset(0, dy, 0);
               BlockPos below = feet.below();
               if (!level.getWorldBorder().isWithinBounds(feet)
                  || !level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)
                  || !level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                  || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) continue;
               Vec3 candidate = new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
               AABB box = player.getBoundingBox().move(candidate.subtract(player.position()));
               if (!level.noCollision(player, box) || positions.stream().anyMatch(previous -> box.intersects(
                  player.getBoundingBox().move(previous.subtract(player.position()))))) continue;
               found = candidate;
            }
         }
         if (found == null) break;
         positions.add(found);
      }
      return positions;
   }

   public static void cancelPendingSummon(ServerPlayer player, boolean refund) {
      PendingSummon pending = PENDING_SUMMONS.remove(player.getUUID());
      if (refund && pending != null && pending.remaining() > 0) {
         refundPersonaMp(player, player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES), pending.remaining());
      }
   }

   public static void cleanupOwnedPersonas(ServerPlayer player) {
      cancelPendingSummon(player, true);
      HundredFacesPresenceService.set(player, false);
      AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (speed != null) speed.removeModifier(RETREAT_SPEED_ID);
      discardOwnedPersonas(player);
   }

   private static void refundPersonaMp(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, int count) {
      double refund = Math.max(0, count) * HundredFacesHassanRules.MP_PER_PERSONA;
      if (refund <= 0 || vars.servant_card_max_mana <= 0) return;
      vars.servant_card_mana = Math.min(vars.servant_card_max_mana, vars.servant_card_mana + refund);
      vars.syncMana(player);
   }

   private static List<HundredFacesHassanPersonaEntity> ownedPersonas(ServerLevel level, UUID ownerUuid, boolean aliveOnly) {
      List<HundredFacesHassanPersonaEntity> personas = new ArrayList<>();
      for (Entity entity : level.getEntities().getAll()) {
         if (entity instanceof HundredFacesHassanPersonaEntity persona
            && ownerUuid.equals(persona.getOwnerUuid())
            && (!aliveOnly || persona.isAlive())) {
            personas.add(persona);
         }
      }
      return personas;
   }

   private static List<HundredFacesHassanPersonaEntity> scopedCommandPersonas(ServerLevel level, ServerPlayer player, int limit) {
      List<HundredFacesHassanPersonaEntity> personas = ownedPersonas(level, player.getUUID(), true);
      personas.sort(Comparator.comparingDouble(player::distanceToSqr));
      if (limit <= 0 || personas.size() <= limit) return personas;
      return new ArrayList<>(personas.subList(0, Math.max(0, limit)));
   }

   private static void rescaleOwnedPersonas(ServerLevel level, UUID ownerUuid) {
      List<HundredFacesHassanPersonaEntity> personas = ownedPersonas(level, ownerUuid, true);
      ServerPlayer owner = level.getServer() == null ? null : level.getServer().getPlayerList().getPlayer(ownerUuid);
      int splitCount = owner == null ? Math.max(1, personas.size()) : Math.max(1, totalSplitCount(owner));
      for (HundredFacesHassanPersonaEntity persona : personas) {
         if (owner != null) persona.syncOwnerCombatAttributes(owner);
         persona.applyPersonaAttributes(splitCount, false);
      }
   }

   private static boolean hasUsableDirks(ServerPlayer player, int count) {
      ItemStack stack = player.getOffhandItem();
      return stack.is(ModItems.DIRK_SMALL_KNIFE.get()) && stack.getMaxDamage() - stack.getDamageValue() >= count;
   }

   private static boolean isValidTarget(ServerPlayer player, LivingEntity target) {
      return target != null && target.isAlive() && target != player
         && !EntityUtils.isImmunePlayerTarget(target)
         && !net.xxxjk.TYPE_MOON_WORLD.servant.entity.CursedArmHassanCombatHelper.isForbiddenTarget(target)
         && !ServantMasterTargeting.isContractMaster(player, target)
         && !player.isAlliedTo(target) && !target.isAlliedTo(player);
   }

   private static boolean dealPhysicalDamage(ServerPlayer player, LivingEntity target, float scale, String action) {
      double attack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
      return target.hurt(new net.xxxjk.TYPE_MOON_WORLD.servant.combat.CombatDamageSource(
         player.damageSources().playerAttack(player),
         net.xxxjk.TYPE_MOON_WORLD.servant.combat.AttackOrigin.skill("hundred_faces_hassan", action)),
         (float)(attack * scale));
   }

   private static Vec3 findSafeBackstabPosition(ServerPlayer player, LivingEntity target) {
      if (!(player.level() instanceof ServerLevel level)) return null;
      Vec3 facing = target.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (facing.lengthSqr() < 1.0E-4) return null;
      Vec3 behind = target.position().subtract(facing.normalize().scale(1.35));
      BlockPos anchor = BlockPos.containing(behind.x, target.getY(), behind.z);
      for (int dy = -2; dy <= 3; dy++) {
         BlockPos feet = anchor.offset(0, dy, 0);
         BlockPos below = feet.below();
         if (!level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)
            || !level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
            || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
            || !level.getWorldBorder().isWithinBounds(feet)) continue;
         Vec3 candidate = new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
         if (!level.noCollision(player, player.getBoundingBox().move(candidate.subtract(player.position())))) continue;
         Vec3 eye = candidate.add(0.0, player.getBbHeight() * 0.55, 0.0);
         if (level.clip(new ClipContext(player.getEyePosition(), eye, ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) continue;
         return candidate;
      }
      return null;
   }

   private static Vec3 findLungeDestination(ServerPlayer player, LivingEntity target, Vec3 direction, double distance) {
      if (!(player.level() instanceof ServerLevel level)) return null;
      Vec3 last = player.position();
      for (double travel = 0.2; travel <= distance + 1.0E-6; travel += 0.2) {
         Vec3 candidate = player.position().add(direction.scale(Math.min(travel, distance)));
         AABB box = player.getBoundingBox().move(candidate.subtract(player.position()));
         if (box.intersects(target.getBoundingBox())) return last;
         if (!level.noCollision(player, box)) return travel <= 0.2 ? null : last;
         last = candidate;
      }
      return last;
   }

   private static void cleanseHarmfulEffects(LivingEntity entity) {
      for (MobEffectInstance effect : List.copyOf(entity.getActiveEffects())) {
         if (effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
            entity.removeEffect(effect.getEffect());
         }
      }
   }

   private static void tickBattleRetreat(ServerPlayer player) {
      long until = player.getPersistentData().getLong(RETREAT_UNTIL_TAG);
      AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (until > player.level().getGameTime()) {
         if (speed != null && speed.getModifier(RETREAT_SPEED_ID) == null) {
            speed.addTransientModifier(new AttributeModifier(RETREAT_SPEED_ID, 0.4,
               AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
         }
      } else if (speed != null && speed.getModifier(RETREAT_SPEED_ID) != null) {
         speed.removeModifier(RETREAT_SPEED_ID);
      }
   }

   private static HundredFacesHassanEntity.PersonaMode selectedPersonaMode(ServerPlayer player) {
      int ordinal = player.getPersistentData().getInt(PERSONA_MODE_TAG);
      HundredFacesHassanEntity.PersonaMode[] modes = HundredFacesHassanEntity.PersonaMode.values();
      return ordinal >= 0 && ordinal < modes.length && modes[ordinal] == HundredFacesHassanEntity.PersonaMode.THROWING
         ? HundredFacesHassanEntity.PersonaMode.THROWING : HundredFacesHassanEntity.PersonaMode.ASSASSINATION;
   }

   public static void setPersonaMode(ServerPlayer player, int ordinal) {
      if (!isActiveCard(player)) return;
      HundredFacesHassanEntity.PersonaMode[] modes = HundredFacesHassanEntity.PersonaMode.values();
      if (ordinal < 0 || ordinal >= modes.length) return;
      HundredFacesHassanEntity.PersonaMode mode = modes[ordinal] == HundredFacesHassanEntity.PersonaMode.THROWING
         ? HundredFacesHassanEntity.PersonaMode.THROWING : HundredFacesHassanEntity.PersonaMode.ASSASSINATION;
      player.getPersistentData().putInt(PERSONA_MODE_TAG, mode.ordinal());
      if (player.level() instanceof ServerLevel level) {
         for (HundredFacesHassanPersonaEntity persona : ownedPersonas(level, player.getUUID(), true)) {
            persona.setPersonaMode(mode);
         }
      }
   }

   private static void discardOwnedPersonas(ServerPlayer player) {
      if (player.getServer() == null) return;
      for (ServerLevel level : player.getServer().getAllLevels()) {
         for (HundredFacesHassanPersonaEntity persona : ownedPersonas(level, player.getUUID(), false)) persona.discard();
      }
   }

   private static int addTotalSplitCount(ServerPlayer player, int amount) {
      int total = Math.max(0, Math.min(HundredFacesHassanRules.MAX_PERSONAS, totalSplitCount(player) + Math.max(0, amount)));
      player.getPersistentData().putInt(HundredFacesHassanRules.TAG_TOTAL_SPLIT_COUNT, total);
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      applyBodySplitAttributes(player, vars);
      return total;
   }

   private static void applyBodySplitAttributes(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      int splitCount = totalSplitCount(player);
      addOrReplaceValue(player.getAttribute(Attributes.MAX_HEALTH), SPLIT_HEALTH_ID,
         HundredFacesHassanRules.mainHealthForSplitCount(splitCount) - HundredFacesHassanRules.MAIN_BASE_HEALTH);
      addOrReplaceValue(player.getAttribute(Attributes.ATTACK_DAMAGE), SPLIT_ATTACK_ID,
         HundredFacesHassanRules.mainAttackDamageForSplitCount(splitCount) - HundredFacesHassanRules.MAIN_BASE_ATTACK_DAMAGE);
      addOrReplaceValue(player.getAttribute(Attributes.ARMOR), SPLIT_ARMOR_ID,
         HundredFacesHassanRules.mainArmorForSplitCount(splitCount) - HundredFacesHassanRules.MAIN_BASE_ARMOR);
      if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
      double maxMana = HundredFacesHassanRules.mainManaForSplitCount(splitCount);
      double regen = MAIN_BASE_MANA_REGEN - (MAIN_BASE_MANA_REGEN - MAIN_MIN_MANA_REGEN)
         * Math.max(0.0, Math.min(1.0, splitCount / (double)HundredFacesHassanRules.MAX_PERSONAS));
      boolean manaChanged = Math.abs(vars.servant_card_max_mana - maxMana) > 1.0E-6
         || Math.abs(vars.servant_card_mana_regen - regen) > 1.0E-6
         || vars.servant_card_mana > maxMana;
      vars.servant_card_max_mana = maxMana;
      vars.servant_card_mana_regen = regen;
      vars.servant_card_mana = Math.min(vars.servant_card_mana, maxMana);
      if (manaChanged) vars.syncMana(player);
   }

   private static void removeBodySplitAttributes(ServerPlayer player) {
      remove(player.getAttribute(Attributes.MAX_HEALTH), SPLIT_HEALTH_ID);
      remove(player.getAttribute(Attributes.ATTACK_DAMAGE), SPLIT_ATTACK_ID);
      remove(player.getAttribute(Attributes.ARMOR), SPLIT_ARMOR_ID);
   }

   private static void addOrReplaceValue(AttributeInstance attribute, ResourceLocation id, double value) {
      if (attribute == null) return;
      AttributeModifier existing = attribute.getModifier(id);
      if (existing != null && existing.operation() == AttributeModifier.Operation.ADD_VALUE
         && Math.abs(existing.amount() - value) < 1.0E-6) {
         return;
      }
      if (existing != null) attribute.removeModifier(id);
      if (Math.abs(value) > 1.0E-6) {
         attribute.addTransientModifier(new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE));
      }
   }

   private static void remove(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null) attribute.removeModifier(id);
   }

   private static HundredFacesOpenScreenMessage.Target targetInfo(HundredFacesHassanPersonaEntity persona) {
      return new HundredFacesOpenScreenMessage.Target(persona.getId(),
         persona.blockPosition().getX(), persona.blockPosition().getY(), persona.blockPosition().getZ(),
         persona.getDisplayName().getString(), persona.getHealth(), persona.getMaxHealth(),
         (float)persona.getAttributeValue(Attributes.ARMOR), persona.getPersonaMode().name(),
         personaCommand(persona), personaAttackEnabled(persona));
   }

   private static void setPersonaCommand(HundredFacesHassanPersonaEntity persona, int command, boolean personal) {
      persona.getPersistentData().putInt(COMMAND_TAG, sanitizeMovementCommand(command));
      persona.getPersistentData().putBoolean(PERSONAL_COMMAND_TAG, personal);
      if (command == COMMAND_SCATTER && persona.level() instanceof ServerLevel level) {
         ServerPlayer owner = persona.getOwnerPlayer(level);
         if (owner != null) {
            Vec3 away = persona.position().subtract(owner.position()).multiply(1.0, 0.0, 1.0);
            if (away.lengthSqr() < 1.0E-4) {
               double angle = (persona.getId() & 15) * Math.PI / 8.0;
               away = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
            }
            Vec3 destination = persona.position().add(away.normalize().scale(8.0 + persona.getRandom().nextDouble() * 8.0));
            persona.setScatterDestination(destination);
         }
      } else if (command == COMMAND_HOLD) {
         persona.getNavigation().stop();
         persona.setTarget(null);
      } else {
         persona.getPersistentData().remove("HundredFacesPersonaScatterX");
         persona.getPersistentData().remove("HundredFacesPersonaScatterY");
         persona.getPersistentData().remove("HundredFacesPersonaScatterZ");
      }
   }

   private static void setPersonaAttackEnabled(HundredFacesHassanPersonaEntity persona, boolean enabled, boolean personal) {
      persona.getPersistentData().putBoolean(ATTACK_ENABLED_TAG, enabled);
      persona.getPersistentData().putBoolean(PERSONAL_ATTACK_TAG, personal);
      if (!enabled) persona.setTarget(null);
   }

   private static void setPersonaForceConcealment(HundredFacesHassanPersonaEntity persona, boolean enabled, boolean personal) {
      persona.getPersistentData().putBoolean(FORCE_CONCEALMENT_TAG, enabled);
      persona.getPersistentData().putBoolean(PERSONAL_CONCEALMENT_TAG, personal);
   }

   private static boolean hasPersonalCommandOverride(HundredFacesHassanPersonaEntity persona) {
      return persona.getPersistentData().getBoolean(PERSONAL_COMMAND_TAG);
   }

   private static boolean hasPersonalAttackOverride(HundredFacesHassanPersonaEntity persona) {
      return persona.getPersistentData().getBoolean(PERSONAL_ATTACK_TAG);
   }

   private static boolean hasPersonalConcealmentOverride(HundredFacesHassanPersonaEntity persona) {
      return persona.getPersistentData().getBoolean(PERSONAL_CONCEALMENT_TAG);
   }

   private static boolean globalAttackEnabled(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      return !data.contains(GLOBAL_ATTACK_ENABLED_TAG) || data.getBoolean(GLOBAL_ATTACK_ENABLED_TAG);
   }

   private static boolean globalConcealmentEnabled(ServerPlayer player) {
      return player.getPersistentData().getBoolean(GLOBAL_CONCEALMENT_TAG);
   }

   private static int globalMovementCommand(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      return data.contains(GLOBAL_COMMAND_TAG) ? sanitizeMovementCommand(data.getInt(GLOBAL_COMMAND_TAG)) : COMMAND_FOLLOW;
   }

   private static int sanitizeMovementCommand(int command) {
      return command == COMMAND_HOLD || command == COMMAND_FOLLOW || command == COMMAND_SCATTER
         ? command : COMMAND_FREE;
   }

   private static HundredFacesHassanPersonaEntity findLookedAtOwnedPersona(ServerPlayer player, ServerLevel level) {
      HitResult hit = player.pick(12.0, 0.0F, false);
      if (hit instanceof EntityHitResult entityHit
         && entityHit.getEntity() instanceof HundredFacesHassanPersonaEntity persona
         && player.getUUID().equals(persona.getOwnerUuid())) {
         return persona;
      }
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle().normalize();
      AABB box = player.getBoundingBox().expandTowards(look.scale(12.0)).inflate(1.25);
      HundredFacesHassanPersonaEntity best = null;
      double bestScore = 0.82;
      for (HundredFacesHassanPersonaEntity persona : level.getEntitiesOfClass(HundredFacesHassanPersonaEntity.class, box,
         p -> p.isAlive() && player.getUUID().equals(p.getOwnerUuid()))) {
         Vec3 to = persona.position().add(0.0, persona.getBbHeight() * 0.5, 0.0).subtract(eye);
         double distance = to.length();
         if (distance <= 0.01 || distance > 12.0) continue;
         double score = look.dot(to.normalize());
         if (score > bestScore) {
            bestScore = score;
            best = persona;
         }
      }
      return best;
   }

   private static void teleportPersonaNearOwner(HundredFacesHassanPersonaEntity persona, ServerPlayer owner) {
      Vec3 forward = horizontal(owner.getLookAngle());
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      Vec3 pos = safeSpawnPosition(owner, right.scale((persona.getId() & 1) == 0 ? 1.8 : -1.8).subtract(forward.scale(1.2)));
      persona.teleportTo(pos.x, pos.y, pos.z);
      persona.getNavigation().stop();
   }

   private static Vec3 safeSpawnPosition(LivingEntity owner, Vec3 offset) {
      Vec3 preferred = owner.position().add(offset.x, 0.0, offset.z);
      if (owner.level().noCollision(owner, owner.getBoundingBox().move(preferred.subtract(owner.position())))) return preferred;
      BlockPos base = owner.blockPosition();
      for (int dx = -2; dx <= 2; dx++) {
         for (int dz = -2; dz <= 2; dz++) {
            Vec3 candidate = new Vec3(base.getX() + dx + 0.5, owner.getY(), base.getZ() + dz + 0.5);
            if (owner.level().noCollision(owner, owner.getBoundingBox().move(candidate.subtract(owner.position())))) return candidate;
         }
      }
      return owner.position();
   }

   private static Vec3 horizontal(Vec3 look) {
      Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
      return horizontal.lengthSqr() < 1.0E-5 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
   }
}
