package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;

public final class ServantCardTransformManager {
   public static final String DEATH_RULE_KEY = "fate_card_death_release";
   private static final ResourceLocation MAX_HEALTH_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_max_health");
   private static final ResourceLocation ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_attack");
   private static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_speed");
   private static final ResourceLocation ARMOR_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_armor");
   private static final ResourceLocation TOUGHNESS_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_toughness");
   private static final ResourceLocation KNOCKBACK_RESISTANCE_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_knockback_resistance");
   private static final ResourceLocation JUMP_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_jump");
   private static final int SERVANT_CARD_NEUTRAL_FOOD = 17;
   private static final String SERVANT_CARD_TAG_PREFIX = "tmw_servant_card";

   private ServantCardTransformManager() {
   }

   public static boolean transform(ServerPlayer player, String servantId) {
      ServantDefinition definition = ServantDataRegistry.get(servantId);
      if (definition == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.unknown", servantId), true);
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.servant_card_transformed) {
         release(player, false);
      }
      clearServantRuntimeState(player, vars);
      saveArmor(player, vars);
      vars.servant_card_transformed = true;
      vars.servant_card_id = servantId;
      applyServantCardTags(player, servantId);
      vars.servant_card_master_uuid = "";
      vars.servant_card_max_mana = ServantCardManaService.maxManaFor(servantId);
      vars.servant_card_mana = vars.servant_card_max_mana;
      vars.servant_card_mana_regen = ServantCardManaService.regenPerSecondFor(servantId);
      vars.servant_card_jump_charges = 4;
      vars.servant_card_jump_recovery_ticks = 0;
      vars.servant_card_np_cooldown = 0;
      vars.servant_card_skill_cooldowns = "";
      if ("medea".equals(servantId)) {
         vars.servant_card_np_cooldown = 3600;
      }
      vars.servant_card_transform_cooldown = 40;
      vars.servant_card_release_cooldown = 40;
      vars.servant_card_action_mode = 0;
      if ("enkidu".equals(servantId)) {
         vars.servant_card_enkidu_transfiguration_points = "6,6,6,6,6";
      }
      vars.servant_card_medusa_mystic_eyes_active = false;
      vars.servant_card_hassan_cloak_broken = false;
      vars.servant_card_hassan_zabaniya_animation_until = 0;
      vars.servant_card_flying = false;
      vars.servant_card_flight_forward = 0.0;
      vars.servant_card_flight_strafe = 0.0;
      vars.servant_card_flight_vertical = 0.0;
      clearServantRuntimeState(player, vars);
      ServantCardTraitService.apply(player, definition);
      if ("heracles".equals(servantId)) {
         ServantCardHeraclesSkills.initializeHeraclesGodHand(player);
      }
      vars.servant_card_was_magus = vars.is_magus;
      vars.servant_card_was_magic_circuit_open = vars.is_magic_circuit_open;
      vars.is_magic_circuit_open = false;
      equipArmor(player, servantId);
      ServantCardLoadoutManager.saveAndEquip(player, vars, servantId);
      applyAttributes(player, definition.parameters());
      if ("enkidu".equals(servantId)) {
         ServantCardEnkiduSkills.applyCurrentTransfiguration(player);
      }
      vars.syncPlayerVariables(player);
      player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.transformed", definition.displayName()), true);
      return true;
   }

   public static boolean release(ServerPlayer player, boolean keepOneHp) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed) {
         return false;
      }
      removeAttributes(player);
      ServantCardFlightController.stop(player, vars, false);
      ServantCardDefenseHandler.clear(player);
      stopActiveNoblePhantasmVoices(player);
      restoreArmor(player, vars);
      ServantCardLoadoutManager.restore(player, vars);
      ServerPlayer linkedMaster = MasterServantLinkService.getLinkedMaster(player, vars);
      if (linkedMaster != null) {
         MasterServantLinkService.breakLink(linkedMaster, player, false);
      } else {
         MasterStateManager.clearServantSide(player, vars);
      }
      vars.servant_card_transformed = false;
      clearServantCardTags(player);
      vars.servant_card_id = "";
      vars.servant_card_master_uuid = "";
      vars.servant_card_mana = 0.0;
      vars.servant_card_max_mana = 0.0;
      vars.servant_card_mana_regen = 0.0;
      vars.servant_card_jump_charges = 0;
      vars.servant_card_jump_recovery_ticks = 0;
      vars.servant_card_skill_cooldowns = "";
      vars.servant_card_np_cooldown = 0;
      vars.servant_card_action_mode = 0;
      vars.servant_card_flying = false;
      vars.servant_card_flight_forward = 0.0;
      vars.servant_card_flight_strafe = 0.0;
      vars.servant_card_flight_vertical = 0.0;
      vars.servant_card_release_cooldown = 40;
      vars.servant_card_medusa_mystic_eyes_active = false;
      vars.servant_card_hassan_cloak_broken = false;
      vars.servant_card_hassan_zabaniya_animation_until = 0;
      clearServantRuntimeState(player, vars);
      ServantCardTraitService.clear(player);
      vars.is_magus = vars.servant_card_was_magus;
      vars.is_magic_circuit_open = vars.servant_card_was_magic_circuit_open;
      vars.servant_card_was_magus = false;
      vars.servant_card_was_magic_circuit_open = false;
      if (keepOneHp) {
         player.setHealth(Math.max(1.0F, Math.min(player.getMaxHealth(), 1.0F)));
      } else if (player.getHealth() > player.getMaxHealth()) {
         player.setHealth(player.getMaxHealth());
      }
      vars.syncPlayerVariables(player);
      player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.released"), true);
      return true;
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars.servant_card_transform_cooldown > 0) {
         vars.servant_card_transform_cooldown--;
      }
      if (vars.servant_card_release_cooldown > 0) {
         vars.servant_card_release_cooldown--;
      }
      if (!vars.servant_card_transformed) {
         if (hasServantCardTag(player)) {
            clearServantCardTags(player);
         }
         return;
      }
      ensureServantCardTags(player, vars.servant_card_id);
      normalizeFood(player);
      player.fallDistance = 0.0F;
      if (vars.servant_card_np_cooldown > 0) {
         vars.servant_card_np_cooldown--;
      }
      tickSkillCooldowns(vars);
      tickJumpRecovery(vars);
      ServantCardManaService.tick(player, vars);
      ServantCardFlightController.tick(player, vars);
      ServantCardDefenseHandler.tick(player, vars);
      ServantCardTraitService.tick(player);
      tickCurrentServant(player, vars);
   }

   private static void tickCurrentServant(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      switch (vars.servant_card_id) {
         case "artoria_pendragon" -> ServantCardArtoriaSkills.tick(player, vars);
         case "cu_chulainn" -> ServantCardCuChulainnSkills.tick(player, vars);
         case "heracles" -> ServantCardHeraclesSkills.tick(player, vars);
         case "gawain" -> ServantCardGawainSkills.tick(player, vars);
         case "medea" -> ServantCardMedeaSkills.tick(player, vars);
         case "paracelsus" -> ServantCardParacelsusSkills.tick(player, vars);
         case "oda_nobunaga" -> ServantCardOdaNobunagaSkills.tick(player, vars);
         case "medusa" -> ServantCardMedusaSkills.tick(player, vars);
         case "cursed_arm_hassan" -> ServantCardHassanSkills.tick(player, vars);
         case "li_shuwen" -> ServantCardLiShuwenSkills.tick(player, vars);
         case "enkidu" -> ServantCardEnkiduSkills.tick(player, vars);
         case "emiya_archer" -> {
            ServantCardEmiyaSkills.tickEmiyaContinuousProjection(player, vars);
            ServantCardEmiyaSkills.tickEmiyaUbwChantSwords(player, vars);
            ServantCardEmiyaSkills.tickEmiyaUbwSupport(player, vars);
         }
         default -> {
         }
      }
   }

   private static void clearServantRuntimeState(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      ServantCardEmiyaSkills.clearEmiyaLayeredProjection(player);
      ServantCardArtoriaSkills.clear(player);
      ServantCardCuChulainnSkills.clear(player);
      ServantCardHeraclesSkills.clear(player);
      ServantCardGawainSkills.clear(player);
      ServantCardMedeaSkills.clear(player, vars);
      ServantCardParacelsusSkills.clear(player);
      ServantCardOdaNobunagaSkills.clear(player);
      ServantCardMedusaSkills.clear(player);
      ServantCardLiShuwenSkills.clear(player);
      ServantCardEnkiduSkills.clear(player);
   }

   public static void normalizeFood(ServerPlayer player) {
      player.getFoodData().setFoodLevel(SERVANT_CARD_NEUTRAL_FOOD);
      player.getFoodData().setSaturation(0.0F);
      player.getFoodData().setExhaustion(0.0F);
   }

   private static void applyServantCardTags(ServerPlayer player, String servantId) {
      clearServantCardTags(player);
      player.addTag(SERVANT_CARD_TAG_PREFIX);
      String servantTag = servantCardTag(servantId);
      if (servantTag != null) {
         player.addTag(servantTag);
      }
   }

   private static void ensureServantCardTags(ServerPlayer player, String servantId) {
      String servantTag = servantCardTag(servantId);
      if (!player.getTags().contains(SERVANT_CARD_TAG_PREFIX) || servantTag == null || !player.getTags().contains(servantTag)) {
         applyServantCardTags(player, servantId);
      }
   }

   private static void clearServantCardTags(ServerPlayer player) {
      for (String tag : new java.util.ArrayList<>(player.getTags())) {
         if (tag.equals(SERVANT_CARD_TAG_PREFIX) || tag.startsWith(SERVANT_CARD_TAG_PREFIX + "_")) {
            player.removeTag(tag);
         }
      }
   }

   private static boolean hasServantCardTag(ServerPlayer player) {
      for (String tag : player.getTags()) {
         if (tag.equals(SERVANT_CARD_TAG_PREFIX) || tag.startsWith(SERVANT_CARD_TAG_PREFIX + "_")) {
            return true;
         }
      }
      return false;
   }

   private static String servantCardTag(String servantId) {
      return servantId == null || servantId.isBlank() ? null : SERVANT_CARD_TAG_PREFIX + "_" + servantId.trim().toLowerCase(java.util.Locale.ROOT);
   }

   public static boolean triggerAction(ServerPlayer player, int slot) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || slot < -1 || slot > 9) {
         return false;
      }
      ServantCardSkillAction action = actionFor(vars.servant_card_id, slot, player.isCrouching());
      if (action == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.empty_slot"), true);
         return false;
      }
      boolean np = slot == 9;
      if (np && "emiya_archer".equals(vars.servant_card_id) && PlayerNoblePhantasmHelper.hasOneShotProjectionNoblePhantasm(player)) {
         if (vars.servant_card_np_cooldown > 0) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.cooldown", String.format(java.util.Locale.ROOT, "%.1f", vars.servant_card_np_cooldown / 20.0F)), true);
            return false;
         }
         return PlayerNoblePhantasmHelper.useOneShotProjectionNoblePhantasm(player);
      }
      if ("ubw".equals(action.effectId())) {
         return ServantCardEmiyaSkills.performUbwAction(player, vars, action);
      }
      if ("hajun".equals(action.effectId())) {
         return ServantCardOdaNobunagaSkills.performOdaHajunAction(player, vars, action);
      }
      int cooldownSlot = slot < 0 ? 6 : slot;
      int currentCooldown = np ? vars.servant_card_np_cooldown : getSkillCooldown(vars, cooldownSlot);
      if (ServantCardArtoriaSkills.isWindAction(action)) {
         currentCooldown = Math.max(currentCooldown, getSkillCooldown(vars, ServantCardArtoriaSkills.WIND_HAMMER_SLOT));
         currentCooldown = Math.max(currentCooldown, getSkillCooldown(vars, ServantCardArtoriaSkills.WIND_RELEASE_SLOT));
      }
      if (ServantCardArtoriaSkills.isWindAction(action) && ServantCardArtoriaSkills.isWindLockedByExcalibur(player)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.artoria_excalibur_wind_locked"), true);
         return false;
      }
      if (currentCooldown > 0) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.cooldown", String.format(java.util.Locale.ROOT, "%.1f", currentCooldown / 20.0F)), true);
         return false;
      }
      if ("copy_weapon".equals(action.effectId()) && ServantCardEmiyaSkills.findCopyableWeaponTarget(player, 8.0, 1.6) == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.no_trace_weapon"), true);
         return false;
      }
      if ("tsubame_gaeshi".equals(action.effectId()) && !PlayerNoblePhantasmHelper.hasTsubameGaeshiTarget(player)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
         return false;
      }
      if ("zabaniya".equals(action.effectId()) && ServantCardHassanSkills.findZabaniyaTarget(player) == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
         return false;
      }
      if ("wu_er_da".equals(action.effectId()) && "li_shuwen".equals(vars.servant_card_id)) {
         if (!ServantCardLiShuwenSkills.performLiWuErDa(player, vars)) {
            return false;
         }
         vars.syncPlayerVariables(player);
         ServantCardVoiceHelper.tryPlaySkill(player, action.effectId());
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.skill_activated", Component.translatable(skillTranslationKey(action))), true);
         return true;
      }
      if ("bloodfort_field".equals(action.effectId()) && !ServantCardMedusaSkills.hasBloodfortTargets(player, ServantCardMedusaSkills.MEDUSA_BLOODFORT_RADIUS)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
         return false;
      }
      if ("escape".equals(action.effectId()) && "medea".equals(vars.servant_card_id) && !ServantCardMedeaSkills.hasMedeaWorkshop(player)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.medea_no_workshop"), true);
         return false;
      }
      if ("paracelsus_workshop_teleport".equals(action.effectId()) && "paracelsus".equals(vars.servant_card_id) && !ServantCardParacelsusSkills.hasWorkshop(player)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.paracelsus_no_workshop"), true);
         return false;
      }
      if (ServantCardActionPreconditions.requiresLookTarget(action.effectId())
         && ServantCardSkillUtils.findLookTarget(player, ServantCardActionPreconditions.targetRangeFor(action.effectId()), 1.8) == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
         return false;
      }
      double mpCost = ServantCardSkillCostRules.effectiveMpCost(vars, action);
      boolean paid = np ? ServantCardManaService.consumeNoblePhantasm(player, vars, mpCost) : ServantCardManaService.consume(player, vars, mpCost);
      if (!paid) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
         return false;
      }
      if (!performAction(player, vars, action)) {
         return false;
      }
      if (!"three_thousand".equals(action.effectId()) && !"hajun".equals(action.effectId()) && !"oda_charged_matchlock".equals(action.effectId())) {
         ServantCardVoiceHelper.tryPlaySkill(player, action.effectId());
      }
      int cooldownTicks = effectiveCooldownTicks(action, np);
      if (np) {
         vars.servant_card_np_cooldown = cooldownTicks;
      } else if (ServantCardArtoriaSkills.isWindAction(action)) {
         setSkillCooldown(vars, ServantCardArtoriaSkills.WIND_HAMMER_SLOT, cooldownTicks);
         setSkillCooldown(vars, ServantCardArtoriaSkills.WIND_RELEASE_SLOT, cooldownTicks);
      } else {
         setSkillCooldown(vars, cooldownSlot, cooldownTicks);
      }
      vars.syncPlayerVariables(player);
      player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.skill_activated", Component.translatable(skillTranslationKey(action))), true);
      return true;
   }

   private static void stopActiveNoblePhantasmVoices(ServerPlayer player) {
      PlayerNoblePhantasmHelper.finishServantCardVoiceSession(
         player,
         "artoria_pendragon",
         ModSounds.ARTORIA_VOICE_EXCALIBUR.get(),
         ModSounds.ARTORIA_VOICE_EXCALIBUR_SHORT.get()
      );
      PlayerNoblePhantasmHelper.finishServantCardVoiceSession(
         player,
         "gawain",
         ModSounds.GAWAIN_VOICE_NP.get(),
         ModSounds.GAWAIN_VOICE_GALLATIN_SHORT.get()
      );
      PlayerNoblePhantasmHelper.finishServantCardVoiceSession(player, "cu_chulainn", ModSounds.CU_CHULAINN_VOICE_GAE_BOLG.get(), null);
      PlayerNoblePhantasmHelper.finishServantCardVoiceSession(
         player,
         "emiya_archer",
         ModSounds.EMIYA_ARCHER_VOICE_UBW.get(),
         ModSounds.EMIYA_ARCHER_VOICE_UBW_SHORT.get()
      );
   }

   public static void handleHoldAction(ServerPlayer player, int slot, boolean pressed) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed) {
         return;
      }
      if ("emiya_archer".equals(vars.servant_card_id) && slot == 1) {
         if (pressed) {
            triggerAction(player, slot);
         } else {
            ServantCardEmiyaSkills.stopRhoAias(player);
         }
      } else if ("li_shuwen".equals(vars.servant_card_id) && slot == 2) {
         if (pressed) {
            triggerAction(player, slot);
         } else {
            player.getPersistentData().remove("ServantCardLiCounterUntil");
         }
      } else if ("oda_nobunaga".equals(vars.servant_card_id)) {
         if (slot == 4) {
            if (pressed) {
               triggerAction(player, slot);
            } else {
               ServantCardOdaNobunagaSkills.releaseOdaChargedMatchlock(player);
            }
         } else if (slot == 8) {
            if (pressed) {
               triggerAction(player, slot);
            } else {
               ServantCardOdaNobunagaSkills.markOdaThreeThousandRelease(player);
            }
         }
      }
   }

   public static boolean bindMaster(ServerPlayer servant, ServerPlayer master) {
      return MasterStateManager.bind(master, servant);
   }

   public static String skillTranslationKey(String servantId, int slot, boolean crouching) {
      ServantCardSkillAction action = actionFor(servantId, slot, crouching);
      return action == null ? "" : skillTranslationKey(action);
   }

   private static String skillTranslationKey(ServantCardSkillAction action) {
      return "skill.typemoonworld.servant_card." + action.effectId();
   }

   public static boolean bigJump(ServerPlayer player, float forwardInput, float strafeInput) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || vars.servant_card_jump_charges <= 0) {
         return false;
      }
      if (PlayerNoblePhantasmHelper.isChargingMovementLocked(player)) {
         return false;
      }
      ServantDefinition definition = ServantDataRegistry.get(vars.servant_card_id);
      double scale = definition == null ? 1.0 : Mth.clamp(definition.parameters().movementSpeed() / 0.28, 0.8, 1.8);
      double yaw = Math.toRadians(player.getYRot());
      Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      Vec3 horizontal = forward.scale(Mth.clamp(forwardInput, -1.0F, 1.0F)).add(right.scale(Mth.clamp(strafeInput, -1.0F, 1.0F)));
      if (horizontal.lengthSqr() > 1.0) {
         horizontal = horizontal.normalize();
      }
      double horizontalPower = horizontal.lengthSqr() < 0.0001 ? 0.0 : 1.35 * scale;
      double verticalPower = Mth.clamp(1.08 + (scale - 1.0) * 0.22, 1.05, 1.32);
      player.setDeltaMovement(horizontal.scale(horizontalPower).add(0.0, verticalPower, 0.0));
      player.hurtMarked = true;
      player.fallDistance = 0.0F;
      if ("heracles".equals(vars.servant_card_id)) {
         ServantCardHeraclesSkills.beginHeraclesAirborne(player, false);
      }
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, player.getX(), player.getY() + 0.15, player.getZ(), 18, 0.35, 0.08, 0.35, 0.08);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, player.getX(), player.getY() + 0.35, player.getZ(), 10, 0.25, 0.18, 0.25, 0.06);
         level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 0.75F, 1.45F);
      }
      vars.servant_card_jump_charges--;
      vars.servant_card_jump_recovery_ticks = vars.servant_card_jump_charges <= 0 ? 100 : 20;
      vars.syncPlayerVariables(player);
      return true;
   }

   private static void applyAttributes(ServerPlayer player, ServantParams params) {
      removeAttributes(player);
      addOrReplace(player.getAttribute(Attributes.MAX_HEALTH), MAX_HEALTH_ID, params.maxHealth() - player.getAttributeBaseValue(Attributes.MAX_HEALTH));
      addOrReplace(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_ID, params.attackDamage() - player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));
      addOrReplace(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, params.movementSpeed() - player.getAttributeBaseValue(Attributes.MOVEMENT_SPEED));
      addOrReplace(player.getAttribute(Attributes.ARMOR), ARMOR_ID, params.armor());
      addOrReplace(player.getAttribute(Attributes.ARMOR_TOUGHNESS), TOUGHNESS_ID, armorToughnessBonus(params));
      addOrReplace(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK_RESISTANCE_ID, knockbackResistanceBonus(params));
      addOrReplace(player.getAttribute(Attributes.JUMP_STRENGTH), JUMP_ID, jumpStrengthBonus(params));
      player.setHealth((float)Math.min(params.maxHealth(), Math.max(1.0, params.maxHealth())));
   }

   private static void removeAttributes(ServerPlayer player) {
      remove(player.getAttribute(Attributes.MAX_HEALTH), MAX_HEALTH_ID);
      remove(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_ID);
      remove(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID);
      remove(player.getAttribute(Attributes.ARMOR), ARMOR_ID);
      remove(player.getAttribute(Attributes.ARMOR_TOUGHNESS), TOUGHNESS_ID);
      remove(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK_RESISTANCE_ID);
      remove(player.getAttribute(Attributes.JUMP_STRENGTH), JUMP_ID);
   }

   private static double armorToughnessBonus(ServantParams params) {
      return 0.0;
   }

   private static double knockbackResistanceBonus(ServantParams params) {
      double endurance = Mth.clamp((params.armor() / 15.0), 0.0, 1.2);
      double strength = Mth.clamp((params.attackDamage() / 25.0), 0.0, 1.2);
      return Mth.clamp(0.18 + endurance * 0.36 + strength * 0.22, 0.0, 0.85);
   }

   private static double jumpStrengthBonus(ServantParams params) {
      double agility = Mth.clamp((params.movementSpeed() - 0.16) / 0.20, 0.0, 1.0);
      return 0.30 + agility * 0.12;
   }

   private static void addOrReplace(AttributeInstance attribute, ResourceLocation id, double value) {
      if (attribute == null) {
         return;
      }
      attribute.removeModifier(id);
      attribute.addPermanentModifier(new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE));
   }

   private static void remove(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null) {
         attribute.removeModifier(id);
      }
   }

   private static void saveArmor(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      HolderLookup.Provider lookup = player.registryAccess();
      CompoundTag tag = new CompoundTag();
      for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
         ItemStack stack = player.getItemBySlot(slot);
         if (!stack.isEmpty()) {
            tag.put(ServantCardRegistry.slotName(slot), stack.save(lookup));
         }
      }
      vars.servant_card_saved_armor = tag;
   }

   private static void restoreArmor(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      HolderLookup.Provider lookup = player.registryAccess();
      for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
         ItemStack stack = ItemStack.EMPTY;
         if (vars.servant_card_saved_armor.contains(ServantCardRegistry.slotName(slot))) {
            stack = ItemStack.parseOptional(lookup, vars.servant_card_saved_armor.getCompound(ServantCardRegistry.slotName(slot)));
         }
         player.setItemSlot(slot, stack);
      }
      vars.servant_card_saved_armor = new CompoundTag();
   }

   private static void equipArmor(ServerPlayer player, String servantId) {
      if ("medea".equals(servantId)) {
         player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.getServantCardArmor(servantId, EquipmentSlot.HEAD)));
         player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.getServantCardArmor(servantId, EquipmentSlot.CHEST)));
         player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
         return;
      }
      if (servantCardHasHeadArmor(servantId)) {
         player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.getServantCardArmor(servantId, EquipmentSlot.HEAD)));
      } else {
         player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
      }
      player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.getServantCardArmor(servantId, EquipmentSlot.CHEST)));
      if (servantCardHasLegArmor(servantId)) {
         player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.getServantCardArmor(servantId, EquipmentSlot.LEGS)));
      } else {
         player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
      }
   }

   private static boolean servantCardHasHeadArmor(String servantId) {
      return "medusa".equals(servantId) || "cursed_arm_hassan".equals(servantId);
   }

   private static boolean servantCardHasLegArmor(String servantId) {
      return !"medea".equals(servantId) && !"cursed_arm_hassan".equals(servantId);
   }

   private static void tickJumpRecovery(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars.servant_card_jump_charges >= 4) {
         return;
      }
      if (vars.servant_card_jump_recovery_ticks > 0) {
         vars.servant_card_jump_recovery_ticks--;
         return;
      }
      vars.servant_card_jump_charges = Math.min(4, vars.servant_card_jump_charges + 1);
      vars.servant_card_jump_recovery_ticks = vars.servant_card_jump_charges <= 0 ? 100 : 20;
   }

   private static void tickSkillCooldowns(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars.servant_card_skill_cooldowns == null || vars.servant_card_skill_cooldowns.isBlank()) {
         return;
      }
      int[] cooldowns = parseSkillCooldowns(vars);
      boolean changed = false;
      for (int i = 0; i < cooldowns.length; i++) {
         if (cooldowns[i] > 0) {
            cooldowns[i]--;
            changed = true;
         }
      }
      if (changed) {
         vars.servant_card_skill_cooldowns = serializeSkillCooldowns(cooldowns);
      }
   }

   public static int getSkillCooldown(TypeMoonWorldModVariables.PlayerVariables vars, int slot) {
      if (slot < 0 || slot >= 9) {
         return 0;
      }
      return parseSkillCooldowns(vars)[slot];
   }

   private static void setSkillCooldown(TypeMoonWorldModVariables.PlayerVariables vars, int slot, int ticks) {
      int[] cooldowns = parseSkillCooldowns(vars);
      if (slot >= 0 && slot < cooldowns.length) {
         cooldowns[slot] = Math.max(0, ticks);
         vars.servant_card_skill_cooldowns = serializeSkillCooldowns(cooldowns);
      }
   }

   private static int[] parseSkillCooldowns(TypeMoonWorldModVariables.PlayerVariables vars) {
      int[] result = new int[9];
      String raw = vars.servant_card_skill_cooldowns == null ? "" : vars.servant_card_skill_cooldowns;
      if (raw.isBlank()) {
         return result;
      }
      String[] parts = raw.split(",");
      for (int i = 0; i < result.length && i < parts.length; i++) {
         try {
            result[i] = Math.max(0, Integer.parseInt(parts[i]));
         } catch (NumberFormatException ignored) {
            result[i] = 0;
         }
      }
      return result;
   }

   private static String serializeSkillCooldowns(int[] cooldowns) {
      boolean any = false;
      for (int i = 0; i < 9; i++) {
         if (i < cooldowns.length && cooldowns[i] > 0) {
            any = true;
            break;
         }
      }
      if (!any) {
         return "";
      }
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < 9; i++) {
         if (i > 0) {
            builder.append(',');
         }
         builder.append(i < cooldowns.length ? Math.max(0, cooldowns[i]) : 0);
      }
      return builder.toString();
   }

   private static int effectiveCooldownTicks(ServantCardSkillAction action, boolean npSlot) {
      if (action == null) {
         return 0;
      }
      String id = action.effectId();
      int cooldown = action.cooldownTicks();
      if ("tsubame_gaeshi".equals(id) || "zabaniya".equals(id) || "wu_er_da".equals(id)) {
         return Math.max(cooldown, 1200);
      }
      return npSlot ? Math.max(cooldown, 3600) : cooldown;
   }

   private static ServantCardSkillAction actionFor(String servantId, int slot, boolean crouching) {
      return ServantCardSkillLayout.actionFor(servantId, slot, crouching);
   }

   private static boolean performAction(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, ServantCardSkillAction action) {
      String id = action.effectId();
      switch (id) {
         case "mana_burst" -> ServantCardArtoriaSkills.performManaBurst(player);
         case "charisma" -> ServantCardArtoriaSkills.performCharisma(player);
         case "strategy" -> ServantCardOdaNobunagaSkills.performOdaStrategy(player);
         case "stealth" -> ServantCardCommonSkills.performPresenceConcealment(player, id);
         case "circle_realm" -> ServantCardLiShuwenSkills.performCircleRealm(player);
         case "berkana" -> ServantCardCommonSkills.performBerkana(player);
         case "perfect_form" -> ServantCardEnkiduSkills.performEnkiduPerfectForm(player);
         case "rho_aias" -> ServantCardEmiyaSkills.spawnRhoAias(player);
         case "invisible_air_hammer" -> ServantCardArtoriaSkills.performInvisibleAirHammer(player, action.cooldownTicks());
         case "invisible_air_release" -> ServantCardArtoriaSkills.performInvisibleAirRelease(player, action.cooldownTicks());
         case "artoria_small_combo" -> ServantCardArtoriaSkills.performSmallCombo(player);
         case "tsubame_gaeshi" -> {
            if (!ServantCardSasakiKojiroSkills.performTsubameGaeshi(player)) {
               return false;
            }
         }
         case "cu_ansuz" -> ServantCardCuChulainnSkills.performAnsuzRune(player);
         case "cu_laguz" -> ServantCardCuChulainnSkills.performLaguzRune(player);
         case "cu_tiwaz" -> ServantCardCuChulainnSkills.performTiwazRune(player);
         case "cu_algiz" -> ServantCardCuChulainnSkills.performAlgizRune(player);
         case "cu_berkana" -> ServantCardCuChulainnSkills.performBerkanaRune(player);
         case "cu_crouch_thrust" -> ServantCardCuChulainnSkills.performCrouchThrust(player);
         case "heracles_big_jump" -> ServantCardHeraclesSkills.performBigJump(player);
         case "heracles_ground_slam" -> ServantCardHeraclesSkills.performGroundSlam(player, true);
         case "heracles_roar" -> ServantCardHeraclesSkills.performRoar(player);
         case "god_hand_status" -> ServantCardHeraclesSkills.showGodHandStatus(player);
         case "algiz" -> ServantCardGawainSkills.performGuard(player);
         case "barrier" -> ServantCardMedeaSkills.performMedeaBarrier(player);
         case "bulwark" -> ServantCardEnkiduSkills.performEnkiduBulwark(player);
         case "self_mod" -> ServantCardHassanSkills.performSelfModification(player);
         case "transfiguration" -> ServantCardEnkiduSkills.performEnkiduTransfiguration(player);
         case "maou" -> ServantCardOdaNobunagaSkills.performOdaMaou(player);
         case "workshop" -> {
            if (!ServantCardMedeaSkills.performMedeaWorkshop(player)) {
               return false;
            }
         }
         case "medea_minor_magic" -> {
            if (!ServantCardMedeaSkills.toggleMedeaMinorMagic(player)) {
               return false;
            }
         }
         case "craft_item" -> {
            if (!ServantCardMedeaSkills.performMedeaCraftItem(player)) {
               return false;
            }
         }
         case "summon_dragonfang" -> {
            if (!ServantCardMedeaSkills.performMedeaDragonfang(player)) {
               return false;
            }
         }
         case "blink_volley" -> ServantCardMedeaSkills.performMedeaBlinkVolley(player);
         case "bind" -> ServantCardMedeaSkills.performMedeaBind(player);
         case "rule_breaker" -> {
            if (!PlayerNoblePhantasmHelper.useRuleBreaker(player)) {
               return false;
            }
         }
         case "escape" -> {
            if (!ServantCardMedeaSkills.performMedeaEscape(player)) {
               return false;
            }
         }
         case "thunder" -> ServantCardMedeaSkills.performMedeaThunder(player);
         case "medea_beam" -> ServantCardMedeaSkills.performMedeaBeam(player);
         case "snare" -> ServantCardMedusaSkills.performSnare(player);
         case "viper_rush" -> ServantCardMedusaSkills.performViperRush(player);
         case "serpent_step" -> ServantCardMedusaSkills.performSerpentStep(player);
         case "medusa_mystic_eyes" -> ServantCardMedusaSkills.performMysticEyesToggle(player);
         case "bloodfort_field" -> ServantCardMedusaSkills.performBloodfort(player, false);
         case "bellerophon" -> ServantCardMedusaSkills.performBellerophon(player);
         case "hassan_dagger" -> ServantCardHassanSkills.giveDirk(player);
         case "zabaniya" -> {
            if (!ServantCardHassanSkills.performZabaniya(player)) {
               return false;
            }
         }
         case "emiya_kb" -> ServantCardEmiyaSkills.equipPair(player, ModItems.GAN_JIANG.get(), ModItems.MO_YE.get());
         case "emiya_spiral" -> player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.PSEUDO_SPIRAL_SWORD.get()));
         case "emiya_hound" -> player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.CRIMSON_HOUND.get()));
         case "copy_weapon" -> {
            if (!ServantCardEmiyaSkills.copyOpponentWeapon(player)) {
               return false;
            }
         }
         case "emiya_layered_projection" -> ServantCardEmiyaSkills.startLayeredProjection(player);
         case "emiya_cycle" -> ServantCardEmiyaSkills.cycleAction(player, vars);
         case "paracelsus_workshop" -> {
            if (!ServantCardParacelsusSkills.performParacelsusWorkshop(player)) {
               return false;
            }
         }
         case "paracelsus_craft_stone" -> {
            if (!ServantCardParacelsusSkills.performParacelsusCraftStone(player)) {
               return false;
            }
         }
         case "paracelsus_spirit_toggle" -> {
            if (!ServantCardParacelsusSkills.toggleParacelsusSpirits(player)) {
               return false;
            }
         }
         case "paracelsus_workshop_teleport" -> {
            if (!ServantCardParacelsusSkills.performParacelsusWorkshopTeleport(player)) {
               return false;
            }
         }
         case "paracelsus_elemental_guardian" -> {
            if (!ServantCardParacelsusSkills.openElementalGuardianScreen(player)) {
               return false;
            }
         }
         case "paracelsus_fire_furnace" -> ServantCardParacelsusSkills.performParacelsusFireFurnace(player);
         case "paracelsus_water_pressure" -> ServantCardParacelsusSkills.performParacelsusWaterPressure(player);
         case "paracelsus_earth_roar" -> ServantCardParacelsusSkills.performParacelsusEarthRoar(player);
         case "paracelsus_wind_cut" -> ServantCardParacelsusSkills.performParacelsusWindCut(player);
         case "ubw" -> ServantCardEmiyaSkills.startUbwChant(player, vars);
         case "oda_hasebe_short_thrust" -> ServantCardOdaNobunagaSkills.performOdaHasebeShortThrust(player);
         case "oda_floating_refill" -> ServantCardOdaNobunagaSkills.performOdaFloatingRefill(player);
         case "oda_three_line" -> ServantCardOdaNobunagaSkills.performOdaThreeLine(player);
         case "oda_encircle_matchlocks" -> {
            if (!ServantCardOdaNobunagaSkills.performOdaEncirclingMatchlocks(player)) {
               return false;
            }
         }
         case "oda_charged_matchlock" -> ServantCardOdaNobunagaSkills.startOdaChargedMatchlock(player);
         case "oda_crossfire_net" -> {
            if (!ServantCardOdaNobunagaSkills.performOdaCrossfireNet(player)) {
               return false;
            }
         }
         case "oda_scorched_earth" -> ServantCardOdaNobunagaSkills.performOdaScorchedEarth(player);
         case "oda_hasebe_breakthrough" -> ServantCardOdaNobunagaSkills.performOdaHasebeBreakthrough(player);
         case "matchlock" -> ServantCardOdaNobunagaSkills.performOdaMatchlock(player, 1, 13.0F);
         case "volley" -> ServantCardOdaNobunagaSkills.performOdaMatchlock(player, 8, 10.0F);
         case "fire_barrage" -> ServantCardOdaNobunagaSkills.performOdaFireBarrage(player);
         case "atsumori" -> ServantCardOdaNobunagaSkills.performOdaAtsumori(player);
         case "anti_mystery" -> ServantCardOdaNobunagaSkills.performOdaAntiMystery(player);
         case "ash_field" -> ServantCardOdaNobunagaSkills.performOdaAshField(player);
         case "hasebe_repel" -> ServantCardOdaNobunagaSkills.performOdaHasebeRepel(player);
         case "three_thousand" -> ServantCardOdaNobunagaSkills.startOdaThreeThousand(player);
         case "hajun" -> ServantCardOdaNobunagaSkills.startOdaHajun(player);
         case "detection" -> ServantCardEnkiduSkills.performEnkiduDetection(player);
         case "chains" -> ServantCardEnkiduSkills.performEnkiduChains(player);
         case "age_babylon" -> ServantCardEnkiduSkills.performEnkiduAgeOfBabylon(player, 12, 16.0F, false);
         case "age_babylon_grand" -> ServantCardEnkiduSkills.performEnkiduGrandAgeOfBabylon(player);
         case "earth_wedge" -> ServantCardEnkiduSkills.performEnkiduEarthWedge(player);
         case "stardust" -> ServantCardEnkiduSkills.performEnkiduStardust(player);
         case "mega_age" -> ServantCardEnkiduSkills.performEnkiduMegaAgeOfBabylon(player);
         case "earth_spike" -> ServantCardEnkiduSkills.performEnkiduEarthSpike(player);
         case "sky_spear_sweep" -> ServantCardEnkiduSkills.performEnkiduSkySpearSweep(player);
         case "enkidu_morph_melee" -> ServantCardEnkiduSkills.performEnkiduMorphMelee(player);
         case "enuma_elish" -> ServantCardEnkiduSkills.performEnkiduEnumaElish(player);
         case "gallatin_spark" -> ServantCardGawainSkills.performGawainGallatinSpark(player);
         case "solar_rebuke" -> ServantCardGawainSkills.performGawainSolarRebuke(player);
         case "radiant_field" -> ServantCardGawainSkills.performGawainRadiantField(player);
         case "flame_tornado" -> ServantCardGawainSkills.performGawainFlameTornado(player);
         case "solar_combo" -> ServantCardGawainSkills.performGawainSolarCombo(player);
         case "yin_yang" -> ServantCardLiShuwenSkills.performLiYinYang(player);
         case "shoulder_charge" -> ServantCardLiShuwenSkills.performLiShoulder(player);
         case "interrupt" -> ServantCardLiShuwenSkills.performLiInterrupt(player);
         case "counter" -> ServantCardLiShuwenSkills.performLiCounter(player);
         case "pursuit" -> ServantCardLiShuwenSkills.performLiPursuit(player);
         default -> ServantCardCommonSkills.performFallback(player, id);
      }
      return true;
   }

}
