package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.ArrayList;
import java.util.Comparator;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.network.HundredFacesOpenScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.HundredFacesStateMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HundredFacesHassanEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HundredFacesHassanPersonaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.hundredfaces.HundredFacesHassanRules;

public final class ServantCardHundredFacesHassanSkills {
   public static final String COMMAND_TAG = "HundredFacesCardCommand";
   public static final String ATTACK_ENABLED_TAG = "HundredFacesCardAttackEnabled";
   private static final String GLOBAL_ATTACK_ENABLED_TAG = "HundredFacesCardGlobalAttackEnabled";
   private static final String LAST_STATE_COUNT_TAG = "HundredFacesCardLastStateCount";
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
   private static final Map<ServerPlayer, HundredFacesStateMessage> LAST_SENT_STATES = new WeakHashMap<>();

   private ServantCardHundredFacesHassanSkills() {
   }

   public static void initialize(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data = player.getPersistentData();
      data.putBoolean(GLOBAL_ATTACK_ENABLED_TAG, true);
      data.putInt(LAST_STATE_COUNT_TAG, 0);
      data.putInt(HundredFacesHassanRules.TAG_TOTAL_SPLIT_COUNT, 0);
      LAST_SENT_STATES.remove(player);
      applyBodySplitAttributes(player, vars);
      sendState(player, true);
   }

   public static void clear(ServerPlayer player) {
      LAST_SENT_STATES.remove(player);
      if (player.getServer() != null) {
         for (ServerLevel level : player.getServer().getAllLevels()) {
            for (HundredFacesHassanPersonaEntity persona : ownedPersonas(level, player.getUUID(), false)) {
               persona.discard();
            }
         }
      }
      CompoundTag data = player.getPersistentData();
      data.remove(GLOBAL_ATTACK_ENABLED_TAG);
      data.remove(LAST_STATE_COUNT_TAG);
      data.remove(HundredFacesHassanRules.TAG_TOTAL_SPLIT_COUNT);
      removeBodySplitAttributes(player);
      player.removeEffect(MobEffects.INVISIBILITY);
      sendIfSupported(player, new HundredFacesStateMessage(0, true));
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!isActiveCard(player)) return;
      ServantCardConcealmentHelper.tick(player);
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
      int live = countOwnedPersonas(level, player.getUUID());
      int alreadySplit = totalSplitCount(player);
      int capacity = Math.max(0, HundredFacesHassanRules.MAX_PERSONAS - alreadySplit);
      int mpLimited = (int)Math.floor(Math.max(0.0, vars.servant_card_mana) / HundredFacesHassanRules.MP_PER_PERSONA);
      int count = Math.max(0, Math.min(Math.max(1, requested), Math.min(capacity, mpLimited)));
      if (count <= 0) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.hundred_faces.no_capacity_or_mp"), true);
         return false;
      }
      if (!ServantCardManaService.consume(player, vars, count * HundredFacesHassanRules.MP_PER_PERSONA)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
         return false;
      }
      LivingEntity inheritedTarget = ServantCardSkillUtils.findAutomaticLookTarget(player, 32.0, 2.0);
      for (int start = 0; start < count; start += HundredFacesHassanRules.MAX_SUMMON_BATCH) {
         int batch = Math.min(HundredFacesHassanRules.MAX_SUMMON_BATCH, count - start);
         int offset = start;
         TYPE_MOON_WORLD.queueServerWork(offset / HundredFacesHassanRules.MAX_SUMMON_BATCH * 2, () -> spawnBatch(player, inheritedTarget, batch));
      }
      player.displayClientMessage(Component.translatable("message.typemoonworld.hundred_faces.summoned", count), true);
      applyBodySplitAttributes(player, vars);
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
      if (!isActiveCard(player) || !(player.level() instanceof ServerLevel level)) return;
      List<HundredFacesHassanPersonaEntity> personas = ownedPersonas(level, player.getUUID(), true);
      if (command == COMMAND_ATTACK_TOGGLE) {
         boolean enabled = !globalAttackEnabled(player);
         player.getPersistentData().putBoolean(GLOBAL_ATTACK_ENABLED_TAG, enabled);
         for (HundredFacesHassanPersonaEntity persona : personas) setPersonaAttackEnabled(persona, enabled);
         player.displayClientMessage(Component.translatable(enabled
            ? "message.typemoonworld.hundred_faces.attack_on"
            : "message.typemoonworld.hundred_faces.attack_off"), true);
      } else if (command == COMMAND_RECALL) {
         for (HundredFacesHassanPersonaEntity persona : personas) {
            setPersonaCommand(persona, COMMAND_FOLLOW);
            if (persona.distanceToSqr(player) > 48.0 * 48.0) teleportPersonaNearOwner(persona, player);
         }
      } else if (command == COMMAND_SCATTER) {
         for (HundredFacesHassanPersonaEntity persona : personas) {
            setPersonaCommand(persona, COMMAND_FREE);
            Vec3 delta = persona.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
            if (delta.lengthSqr() < 1.0E-4) {
               delta = new Vec3(persona.getRandom().nextDouble() - 0.5, 0.0, persona.getRandom().nextDouble() - 0.5);
            }
            Vec3 destination = persona.position().add(delta.normalize().scale(8.0 + persona.getRandom().nextDouble() * 8.0));
            persona.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.05);
         }
      }
      sendState(player, true);
   }

   public static void setPersonaCommand(ServerPlayer player, int entityId, int command) {
      if (!isActiveCard(player) || !(player.level() instanceof ServerLevel level)) return;
      Entity entity = level.getEntity(entityId);
      if (!(entity instanceof HundredFacesHassanPersonaEntity persona) || !player.getUUID().equals(persona.getOwnerUuid())) return;
      if (command == COMMAND_ATTACK_TOGGLE) setPersonaAttackEnabled(persona, !personaAttackEnabled(persona));
      else setPersonaCommand(persona, sanitizeMovementCommand(command));
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
      Vec3 targetPos = target.position();
      float targetYaw = target.getYRot();
      float targetPitch = target.getXRot();
      target.discard();

      HundredFacesHassanPersonaEntity replacement = ModEntities.HUNDRED_FACES_HASSAN_PERSONA.get().create(level);
      if (replacement != null) {
         replacement.moveTo(old.x, old.y, old.z, oldYaw, oldPitch);
         replacement.initialize(player, null, mode, Math.max(1, totalSplitCount(player)));
         setPersonaCommand(replacement, command);
         setPersonaAttackEnabled(replacement, attack);
         level.addFreshEntity(replacement);
      }
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
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 3, false, false, true));
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
   }

   public static boolean performPresenceConcealment(ServerPlayer player) {
      if (!isActiveCard(player)) return false;
      ServantCardConcealmentHelper.apply(player, 160);
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 0, false, true, true));
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

   public static boolean isActiveCard(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && "hundred_faces_hassan".equals(vars.servant_card_id);
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
      if (NetworkRegistry.hasChannel(player.connection, payload.type().id())) {
         PacketDistributor.sendToPlayer(player, payload);
      }
   }

   private static void spawnBatch(ServerPlayer player, LivingEntity inheritedTarget, int count) {
      if (!isActiveCard(player) || !(player.level() instanceof ServerLevel level)) return;
      int live = countOwnedPersonas(level, player.getUUID());
      int alreadySplit = totalSplitCount(player);
      Vec3 forward = horizontal(player.getLookAngle());
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      int spawned = 0;
      for (int i = 0; i < count && alreadySplit + spawned < HundredFacesHassanRules.MAX_PERSONAS; i++) {
         HundredFacesHassanPersonaEntity persona = ModEntities.HUNDRED_FACES_HASSAN_PERSONA.get().create(level);
         if (persona == null) continue;
         int index = live + spawned;
         double angle = Math.PI * 2.0 * index / Math.max(6.0, live + count);
         Vec3 offset = forward.scale(Math.cos(angle) * 1.7).add(right.scale(Math.sin(angle) * 1.7));
         Vec3 pos = safeSpawnPosition(player, offset);
         float yaw = (float)Math.toDegrees(Math.atan2(-forward.x, forward.z));
         persona.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
         persona.initialize(player, inheritedTarget, HundredFacesHassanEntity.PersonaMode.SCOUT, alreadySplit + spawned + 1);
         setPersonaCommand(persona, COMMAND_FOLLOW);
         setPersonaAttackEnabled(persona, globalAttackEnabled(player));
         if (level.addFreshEntity(persona)) {
            spawned++;
            level.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y + 0.9, pos.z, 10, 0.35, 0.55, 0.35, 0.02);
         }
      }
      if (spawned > 0) {
         addTotalSplitCount(player, spawned);
         level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.8, 0.8, 0.8, 0.03);
         rescaleOwnedPersonas(level, player.getUUID());
         sendState(player, true);
      }
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

   private static void rescaleOwnedPersonas(ServerLevel level, UUID ownerUuid) {
      List<HundredFacesHassanPersonaEntity> personas = ownedPersonas(level, ownerUuid, true);
      ServerPlayer owner = level.getServer() == null ? null : level.getServer().getPlayerList().getPlayer(ownerUuid);
      int splitCount = owner == null ? Math.max(1, personas.size()) : Math.max(1, totalSplitCount(owner));
      for (HundredFacesHassanPersonaEntity persona : personas) {
         if (owner != null) persona.syncOwnerCombatAttributes(owner);
         persona.applyPersonaAttributes(splitCount, false);
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

   private static void setPersonaCommand(HundredFacesHassanPersonaEntity persona, int command) {
      persona.getPersistentData().putInt(COMMAND_TAG, sanitizeMovementCommand(command));
      if (command == COMMAND_HOLD) {
         persona.getNavigation().stop();
         persona.setTarget(null);
      }
   }

   private static void setPersonaAttackEnabled(HundredFacesHassanPersonaEntity persona, boolean enabled) {
      persona.getPersistentData().putBoolean(ATTACK_ENABLED_TAG, enabled);
      if (!enabled) persona.setTarget(null);
   }

   private static boolean globalAttackEnabled(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      return !data.contains(GLOBAL_ATTACK_ENABLED_TAG) || data.getBoolean(GLOBAL_ATTACK_ENABLED_TAG);
   }

   private static int sanitizeMovementCommand(int command) {
      return command == COMMAND_HOLD || command == COMMAND_FOLLOW ? command : COMMAND_FREE;
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
