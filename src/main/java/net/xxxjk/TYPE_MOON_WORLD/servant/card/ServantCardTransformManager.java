package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.chain.service.EnumaChainService;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.martial.BodyTrainingService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.GilgameshDivineShield;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.talent.TalentService;
import net.neoforged.neoforge.common.NeoForge;
import net.xxxjk.typemoonworld.api.event.ServantTransformEvent;
import net.xxxjk.typemoonworld.api.event.ServantActionEvent;
import net.xxxjk.typemoonworld.api.ExecutionResult;
import net.xxxjk.typemoonworld.api.ServantContext;

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
   private static final String BOUND_ITEM_TAG = "ServantCardBound";
   private static final String BOUND_WEAPON_TAG = "ServantCardWeapon";
   private static final String EMIYA_WEAPON_EXEMPT_TAG = "ServantCardEmiyaWeaponExempt";
   static final int SERVANT_CARD_DEFAULT_JUMP_CHARGES = ServantCardJumpRecoveryRules.DEFAULT_JUMP_CHARGES;

   private ServantCardTransformManager() {
   }

   public static boolean transform(ServerPlayer player, String servantId) {
      ServantDefinition definition = ServantDataRegistry.get(servantId);
      if (definition == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.unknown", servantId), true);
         return false;
      }
      ResourceLocation publicId = publicServantId(servantId);
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.master_card_active) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.master_card_conflict"), true);
         return false;
      }
      if (NeoForge.EVENT_BUS.post(new ServantTransformEvent.Pre(player, publicId)).isCanceled()) return false;
      if (vars.servant_card_transformed) {
         release(player, false);
      }
      if (vars.master_active) {
         MasterStateManager.release(player);
      }
      clearServantRuntimeState(player, vars);
      player.getPersistentData().remove("MasterLossForcedDeath");
      player.getPersistentData().remove("MasterLossDecayDamage");
      saveArmor(player, vars);
      saveFood(player, vars);
      vars.servant_card_transformed = true;
      vars.servant_card_id = servantId;
      TalentService.suspendActiveEffects(player);
      PassiveService.suspendEffects(player);
      BodyTrainingService.stashForServantCard(player, vars);
      applyServantCardTags(player, servantId);
      vars.servant_card_master_uuid = "";
      vars.servant_card_contract_id = "";
      vars.servant_card_contract_state = MasterServantLinkService.SERVANT_CONTRACT_NATIVE;
      MasterServantLinkService.clearMasterPosition(vars);
      MasterServantLinkService.clearSurvival(vars);
      vars.servant_card_last_combat_tick = player.level().getGameTime();
      vars.servant_card_max_mana = ServantCardManaService.maxManaFor(servantId);
      vars.servant_card_mana = vars.servant_card_max_mana;
      vars.servant_card_mana_regen = ServantCardManaService.regenPerSecondFor(servantId);
      vars.servant_card_jump_charges = SERVANT_CARD_DEFAULT_JUMP_CHARGES;
      vars.servant_card_jump_recovery_ticks = 0;
      vars.servant_card_jump_recovery_end = 0L;
      vars.servant_card_np_cooldown = 0;
      vars.servant_card_np_cooldown_end = 0L;
      vars.servant_card_skill_cooldowns = "";
      vars.servant_card_skill_cooldown_ends = "";
      vars.servant_card_gilles_spellbook_mana = 0.0;
      vars.servant_card_gilles_spellbook_max_mana = 0.0;
      if ("medea".equals(servantId)) {
         vars.servant_card_np_cooldown = 3600;
      }
      vars.servant_card_transform_cooldown = 40;
      vars.servant_card_release_cooldown = 40;
      vars.servant_card_action_mode = 0;
      if ("enkidu".equals(servantId)) {
         vars.servant_card_enkidu_transfiguration_points = "6,6,6,6,6";
      }
      if ("gilgamesh".equals(servantId)) {
         ServantCardGilgameshSkills.reset(player);
      }
      if ("gilgamesh_caster".equals(servantId)) {
         ServantCardCasterGilgameshSkills.initialize(player, vars);
      }
      if ("zhao_yun_rider".equals(servantId)) {
         ServantCardZhaoYunSkills.initialize(player);
      }
      if ("okita_souji_saber".equals(servantId)) {
         ServantCardOkitaSoujiSaberSkills.initialize(player);
      }
      if ("gilles_de_rais_caster".equals(servantId)) {
         ServantCardGillesDeRaisSkills.initialize(player);
      }
      vars.servant_card_medusa_mystic_eyes_active = false;
      vars.servant_card_hassan_cloak_broken = false;
      vars.servant_card_hassan_zabaniya_animation_until = 0;
      vars.servant_card_flying = false;
      vars.servant_card_flight_mode = 0;
      vars.servant_card_high_flight_until = 0L;
      vars.servant_card_high_flight_cooldown_until = 0L;
      vars.servant_card_oda_flight_ticks = 100;
      vars.servant_card_oda_flight_cooldown_until = 0L;
      vars.servant_card_oda_flight_recharge_at = 0L;
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
      if ("pale_rider".equals(servantId)) ServantCardPaleRiderSkills.initialize(player, vars);
      if ("hundred_faces_hassan".equals(servantId)) ServantCardHundredFacesHassanSkills.initialize(player, vars);
      if ("diarmuid_ua_duibhne".equals(servantId)) ServantCardDiarmuidSkills.initialize(player);
      if ("lancelot_berserker".equals(servantId)) ServantCardLancelotBerserkerSkills.initialize(player, vars);
      if ("iskandar".equals(servantId)) ServantCardIskandarSkills.initialize(player);
      if ("shadow_hassan".equals(servantId)) ServantCardShadowHassanSkills.initialize(player);
      if ("fanatic_assassin".equals(servantId)) ServantCardFanaticAssassinSkills.initialize(player);
      applyAttributes(player, definition.parameters(), servantId);
      if ("arash".equals(servantId)) ServantCardArashSkills.initialize(player);
      if ("enkidu".equals(servantId)) {
         ServantCardEnkiduSkills.applyCurrentTransfiguration(player);
      }
      vars.syncPlayerVariables(player);
      player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.transformed", definition.displayName()), true);
      NeoForge.EVENT_BUS.post(new ServantTransformEvent.Post(player, publicId));
      return true;
   }

   public static boolean release(ServerPlayer player, boolean keepOneHp) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed) {
         return false;
      }
      ResourceLocation releasedId = publicServantId(vars.servant_card_id);
      removeAttributes(player);
      ServantCardFlightController.stop(player, vars, false);
      ServantCardDefenseHandler.clear(player);
      stopActiveNoblePhantasmVoices(player);
      if ("ushiwakamaru_rider".equals(vars.servant_card_id)) ServantCardUshiwakamaruSkills.clear(player, vars);
      restoreArmor(player, vars);
      if ("pale_rider".equals(vars.servant_card_id)) ServantCardPaleRiderSkills.clear(player);
      if ("hundred_faces_hassan".equals(vars.servant_card_id)) ServantCardHundredFacesHassanSkills.clear(player);
      if ("diarmuid_ua_duibhne".equals(vars.servant_card_id)) ServantCardDiarmuidSkills.clear(player);
      if ("lancelot_berserker".equals(vars.servant_card_id)) ServantCardLancelotBerserkerSkills.clear(player, vars);
      if ("iskandar".equals(vars.servant_card_id)) ServantCardIskandarSkills.clear(player);
      if ("shadow_hassan".equals(vars.servant_card_id)) ServantCardShadowHassanSkills.clear(player);
      if ("fanatic_assassin".equals(vars.servant_card_id)) ServantCardFanaticAssassinSkills.clear(player);
      if ("senko_muramasa".equals(vars.servant_card_id)) ServantCardSenkoMuramasaSkills.clear(player, vars);
      if ("zhao_yun_rider".equals(vars.servant_card_id)) ServantCardZhaoYunSkills.clear(player);
      if ("gilgamesh_caster".equals(vars.servant_card_id)) ServantCardCasterGilgameshSkills.clear(player);
      if ("okita_souji_saber".equals(vars.servant_card_id)) ServantCardOkitaSoujiSaberSkills.clear(player);
      if ("gilles_de_rais_caster".equals(vars.servant_card_id)) ServantCardGillesDeRaisSkills.clear(player);
      ServantCardLoadoutManager.restore(player, vars);
      MasterServantLinkService.onServantLost(player, vars);
      vars.servant_card_transformed = false;
      BodyTrainingService.restoreFromServantCard(player, vars);
      clearServantCardTags(player);
      vars.servant_card_id = "";
      vars.servant_card_master_uuid = "";
      vars.servant_card_contract_id = "";
      vars.servant_card_contract_state = MasterServantLinkService.SERVANT_CONTRACT_NATIVE;
      vars.servant_card_mana = 0.0;
      vars.servant_card_max_mana = 0.0;
      vars.servant_card_mana_regen = 0.0;
      vars.servant_card_jump_charges = 0;
      vars.servant_card_jump_recovery_ticks = 0;
      vars.servant_card_jump_recovery_end = 0L;
      vars.servant_card_skill_cooldowns = "";
      vars.servant_card_skill_cooldown_ends = "";
      vars.servant_card_np_cooldown = 0;
      vars.servant_card_np_cooldown_end = 0L;
      vars.servant_card_gilles_spellbook_mana = 0.0;
      vars.servant_card_gilles_spellbook_max_mana = 0.0;
      vars.servant_card_action_mode = 0;
      vars.servant_card_flying = false;
      vars.servant_card_flight_mode = 0;
      vars.servant_card_high_flight_until = 0L;
      vars.servant_card_high_flight_cooldown_until = 0L;
      vars.servant_card_oda_flight_ticks = 100;
      vars.servant_card_oda_flight_cooldown_until = 0L;
      vars.servant_card_oda_flight_recharge_at = 0L;
      vars.servant_card_flight_forward = 0.0;
      vars.servant_card_flight_strafe = 0.0;
      vars.servant_card_flight_vertical = 0.0;
      vars.servant_card_release_cooldown = 40;
      vars.servant_card_medusa_mystic_eyes_active = false;
      vars.servant_card_hassan_cloak_broken = false;
      vars.servant_card_hassan_zabaniya_animation_until = 0;
      clearServantRuntimeState(player, vars);
      ServantCardTraitService.clear(player);
      PassiveService.resumeEffects(player);
      TalentService.resumeActiveEffects(player);
      vars.is_magus = vars.servant_card_was_magus;
      vars.is_magic_circuit_open = vars.servant_card_was_magic_circuit_open;
      vars.servant_card_was_magus = false;
      vars.servant_card_was_magic_circuit_open = false;
      restoreFood(player, vars);
      MasterServantLinkService.clearSurvival(vars);
      vars.servant_card_last_combat_tick = Long.MIN_VALUE;
      if (keepOneHp) {
         player.setHealth(Math.max(1.0F, Math.min(player.getMaxHealth(), 1.0F)));
      } else if (player.getHealth() > player.getMaxHealth()) {
         player.setHealth(player.getMaxHealth());
      }
      vars.syncPlayerVariables(player);
      player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.released"), true);
      NeoForge.EVENT_BUS.post(new ServantTransformEvent.End(player, releasedId));
      return true;
   }

   private static ResourceLocation publicServantId(String servantId) {
      ResourceLocation parsed = servantId != null && servantId.indexOf(':') >= 0 ? ResourceLocation.tryParse(servantId) : null;
      return parsed != null ? parsed : ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID,
         servantId == null || servantId.isBlank() ? "unknown" : servantId);
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars.servant_card_transform_cooldown > 0) {
         vars.servant_card_transform_cooldown--;
      }
      if (vars.servant_card_release_cooldown > 0) {
         vars.servant_card_release_cooldown--;
      }
      if (!vars.servant_card_transformed) {
         boolean bodyTrainingRestored = BodyTrainingService.restoreFromServantCard(player, vars);
         if (hasServantCardTag(player)) {
            clearServantCardTags(player);
         }
         if (bodyTrainingRestored) vars.syncPlayerVariables(player);
         return;
      }
      ensureServantCardTags(player, vars.servant_card_id);
      normalizeFood(player);
      player.fallDistance = 0.0F;
      boolean timersChanged = tickNoblePhantasmCooldown(player, vars);
      timersChanged |= tickSkillCooldowns(player, vars);
      timersChanged |= tickJumpRecovery(player, vars);
      ServantCardManaService.tick(player, vars);
      ServantCardHealthService.tick(player, vars);
      ServantCardFlightController.tick(player, vars);
      ServantCardDefenseHandler.tick(player, vars);
      ServantCardTraitService.tick(player);
      tickCurrentServant(player, vars);
      if (timersChanged && player.tickCount % 5 == 0) {
         vars.syncServantCardRuntime(player);
      } else if (player.tickCount % 40 == 0) {
         vars.syncServantCardRuntime(player);
      }
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
         case "sasaki_kojiro" -> ServantCardSasakiKojiroSkills.tick(player);
         case "cursed_arm_hassan" -> ServantCardHassanSkills.tick(player, vars);
         case "shadow_hassan" -> ServantCardShadowHassanSkills.tick(player, vars);
         case "fanatic_assassin" -> ServantCardFanaticAssassinSkills.tick(player, vars);
         case "li_shuwen" -> ServantCardLiShuwenSkills.tick(player, vars);
         case "pale_rider" -> ServantCardPaleRiderSkills.tick(player, vars);
         case "hundred_faces_hassan" -> ServantCardHundredFacesHassanSkills.tick(player, vars);
         case "diarmuid_ua_duibhne" -> ServantCardDiarmuidSkills.tick(player, vars);
         case "lancelot_berserker" -> ServantCardLancelotBerserkerSkills.tick(player, vars);
         case "iskandar" -> ServantCardIskandarSkills.tick(player, vars);
         case "enkidu" -> ServantCardEnkiduSkills.tick(player, vars);
         case "gilgamesh" -> ServantCardGilgameshSkills.tick(player, vars);
         case "gilgamesh_caster" -> ServantCardCasterGilgameshSkills.tick(player, vars);
         case "gilles_de_rais_caster" -> ServantCardGillesDeRaisSkills.tick(player, vars);
         case "emiya_archer" -> {
            ServantCardEmiyaSkills.tickEmiyaContinuousProjection(player, vars);
            ServantCardEmiyaSkills.tickEmiyaUbwChantSwords(player, vars);
            ServantCardEmiyaSkills.tickEmiyaUbwSupport(player, vars);
            ServantCardEmiyaSkills.tickEmiyaEquipmentAndCounter(player, vars);
         }
         case "ushiwakamaru_rider" -> ServantCardUshiwakamaruSkills.tick(player, vars);
         case "arash" -> ServantCardArashSkills.tick(player, vars);
         case "nightingale" -> ServantCardNightingaleSkills.tick(player, vars);
         case "zhao_yun_rider" -> ServantCardZhaoYunSkills.tick(player, vars);
         case "senko_muramasa" -> ServantCardSenkoMuramasaSkills.tick(player, vars);
         case "baobhan_sith" -> ServantCardBaobhanSithSkills.tick(player, vars);
         case "okita_souji_saber" -> ServantCardOkitaSoujiSaberSkills.tick(player, vars);
         default -> {
         }
      }
   }

   static void clearServantRuntimeState(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      ServantCardEmiyaSkills.clear(player);
      ServantCardArtoriaSkills.clear(player);
      ServantCardCuChulainnSkills.clear(player);
      ServantCardHeraclesSkills.clear(player);
      ServantCardGawainSkills.clear(player);
      ServantCardMedeaSkills.clear(player, vars);
      ServantCardParacelsusSkills.clear(player);
      ServantCardOdaNobunagaSkills.clear(player);
      ServantCardMedusaSkills.clear(player);
      ServantCardLiShuwenSkills.clear(player);
      ServantCardPaleRiderSkills.clear(player);
      ServantCardHundredFacesHassanSkills.clear(player);
      ServantCardDiarmuidSkills.clear(player);
      ServantCardLancelotBerserkerSkills.clear(player, vars);
      ServantCardIskandarSkills.clear(player);
      ServantCardShadowHassanSkills.clear(player);
      ServantCardFanaticAssassinSkills.clear(player);
      ServantCardEnkiduSkills.clear(player);
      ServantCardGilgameshSkills.clear(player);
      ServantCardCasterGilgameshSkills.clear(player);
      ServantCardGillesDeRaisSkills.clear(player);
      ServantCardArashSkills.clear(player);
      ServantCardNightingaleSkills.clear(player, false);
      ServantCardZhaoYunSkills.clear(player);
      ServantCardSenkoMuramasaSkills.clear(player, vars);
      ServantCardBaobhanSithSkills.clear(player);
      ServantCardOkitaSoujiSaberSkills.clear(player);
   }

   public static void normalizeFood(ServerPlayer player) {
      var food = player.getFoodData();
      if (food.getFoodLevel() != SERVANT_CARD_NEUTRAL_FOOD) {
         food.setFoodLevel(SERVANT_CARD_NEUTRAL_FOOD);
      }
      if (food.getSaturationLevel() != 0.0F) {
         food.setSaturation(0.0F);
      }
      if (food.getExhaustionLevel() != 0.0F) {
         food.setExhaustion(0.0F);
      }
   }

   private static void saveFood(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      var food = player.getFoodData();
      vars.servant_card_food_snapshot_valid = true;
      vars.servant_card_saved_food_level = food.getFoodLevel();
      vars.servant_card_saved_saturation = food.getSaturationLevel();
      vars.servant_card_saved_exhaustion = food.getExhaustionLevel();
   }

   private static void restoreFood(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!vars.servant_card_food_snapshot_valid) return;
      var food = player.getFoodData();
      food.setFoodLevel(vars.servant_card_saved_food_level);
      food.setSaturation(vars.servant_card_saved_saturation);
      food.setExhaustion(vars.servant_card_saved_exhaustion);
      vars.servant_card_food_snapshot_valid = false;
   }

   public static void prepareVanishingEquipment(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      var enchantments = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
      var vanishing = enchantments.getOrThrow(Enchantments.VANISHING_CURSE);
      for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
         ItemStack stack = player.getItemBySlot(slot);
         if (!stack.isEmpty()) stack.enchant(vanishing, 1);
      }
      boolean emiyaWeaponsSurvive = "emiya_archer".equals(vars.servant_card_id);
      if (!emiyaWeaponsSurvive) {
         for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty()) stack.enchant(vanishing, 1);
         }
      }
      for (ItemStack stack : player.getInventory().items) {
         if (isBoundItem(stack) && !isEmiyaWeaponExempt(stack)) {
            stack.enchant(vanishing, 1);
         } else if (!emiyaWeaponsSurvive && PlayerNoblePhantasmHelper.isUbwProjection(stack)) {
            stack.enchant(vanishing, 1);
         }
      }
   }

   public static ItemStack markGeneratedItem(ItemStack stack, boolean weapon, boolean emiyaWeaponExempt) {
      if (stack.isEmpty()) {
         return stack;
      }
      CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
      CompoundTag tag = customData == null ? new CompoundTag() : customData.copyTag();
      tag.putBoolean(BOUND_ITEM_TAG, true);
      tag.putBoolean(BOUND_WEAPON_TAG, weapon);
      if (emiyaWeaponExempt) {
         tag.putBoolean(EMIYA_WEAPON_EXEMPT_TAG, true);
      } else {
         tag.remove(EMIYA_WEAPON_EXEMPT_TAG);
      }
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      return stack;
   }

   public static boolean shouldDeleteBoundDrop(ItemStack stack) {
      return isBoundItem(stack) && !isEmiyaWeaponExempt(stack);
   }

   private static boolean isBoundItem(ItemStack stack) {
      CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
      return customData != null && customData.copyTag().getBoolean(BOUND_ITEM_TAG);
   }

   private static boolean isEmiyaWeaponExempt(ItemStack stack) {
      CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
      if (customData == null) {
         return false;
      }
      CompoundTag tag = customData.copyTag();
      return tag.getBoolean(BOUND_WEAPON_TAG) && tag.getBoolean(EMIYA_WEAPON_EXEMPT_TAG);
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
      if (EnumaChainService.isCasting(player)) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || slot < -1 || slot > 9) {
         return false;
      }
      if (ServantCardArashSkills.isPlayerChanting(player)) {
         return false;
      }
      if (ServantCardNightingaleSkills.isCasting(player)) {
         return false;
      }
      if (ServantMasterCarryService.isCarryingMaster(player)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.master_carry.skill_blocked"), true);
         return false;
      }
      if (player.getPersistentData().getLong("TypeMoonCombatSuppressedUntil") > player.level().getGameTime()) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.skill_suppressed"), true);
         return false;
      }
      String externalActionId = net.xxxjk.TYPE_MOON_WORLD.api.CardActionRegistry.actionIdForSlot(vars.servant_card_id, slot);
      ResourceLocation parsedActionId = ResourceLocation.tryParse(externalActionId);
      ServantContext externalContext = new ServantContext(player, null, vars.servant_card_id, player.level(), 0.0, true, player.level().getGameTime());
      boolean noblePhantasmAction = isNoblePhantasmAction(vars.servant_card_id, slot);
      if (parsedActionId != null && NeoForge.EVENT_BUS.post(new ServantActionEvent.Pre(
         noblePhantasmAction ? ServantActionEvent.Kind.NOBLE_PHANTASM : ServantActionEvent.Kind.SKILL, parsedActionId, externalContext)).isCanceled()) return false;
      net.xxxjk.typemoonworld.api.ExecutionResult external = net.xxxjk.TYPE_MOON_WORLD.api.CardActionRegistry.executeSlot(
         player, vars.servant_card_id, slot, player.isCrouching(), player.level().getGameTime()
      );
      if (external.handled()) {
         if (external.success() && external.resourceCost() > 0.0) {
            if (!ServantCardManaService.consume(player, vars, external.resourceCost())) return false;
         }
         if (parsedActionId != null) NeoForge.EVENT_BUS.post(new ServantActionEvent.Post(
            noblePhantasmAction ? ServantActionEvent.Kind.NOBLE_PHANTASM : ServantActionEvent.Kind.SKILL, parsedActionId, externalContext, external));
         return external.success();
      }
      ServantCardSkillAction action = actionFor(vars.servant_card_id, slot, player.isCrouching());
      if (action == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.empty_slot"), true);
         return false;
      }
      if ("muramasa_forge".equals(action.effectId())) {
         return ServantCardSenkoMuramasaSkills.openForgeSelection(player);
      }
      boolean npSlot = usesSharedNoblePhantasmCooldown(vars.servant_card_id, slot);
      boolean np = noblePhantasmAction;
      boolean unlimited = ServantCardUnlimitedMode.isEnabled(player);
      if (np && "emiya_archer".equals(vars.servant_card_id) && PlayerNoblePhantasmHelper.hasOneShotProjectionNoblePhantasm(player)) {
         if (!unlimited && vars.servant_card_np_cooldown > 0) {
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
      if ("iskandar_ionioi_hetairoi".equals(action.effectId()) && ServantCardIskandarSkills.isIonioiActiveOrInside(player)) {
         return ServantCardIskandarSkills.performIonioiHetairoi(player, vars);
      }
      if ("iskandar_gordius_wheel".equals(action.effectId()) && ServantCardIskandarSkills.hasActiveGordiusWheel(player)) {
         return ServantCardIskandarSkills.performGordiusWheel(player);
      }
      if ("lancelot_aroundight".equals(action.effectId()) && ServantCardLancelotBerserkerSkills.isAroundightActive(player)) {
         if (ServantCardLancelotBerserkerSkills.performAroundight(player, vars)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.skill_activated", Component.translatable(skillTranslationKey(action))), true);
            vars.syncPlayerVariables(player);
         }
         return true;
      }
      if ("gilgamesh_key".equals(action.effectId())) {
         return ServantCardGilgameshSkills.performKey(player);
      }
      if ("gilgamesh_melee".equals(action.effectId())) {
         return ServantCardGilgameshSkills.performMelee(player);
      }
      if ("pale_rider".equals(vars.servant_card_id)
         && ((slot == 1 && ServantCardPaleRiderSkills.isPossessing(player))
            || (slot == 8 && ServantCardPaleRiderSkills.isUnderworldActive(player))
            || (slot == 9 && ServantCardPaleRiderSkills.isCalamityActive(player)))) {
         return performAction(player, vars, action);
      }
      if (ServantCardGilgameshSkills.isVaultAction(action.effectId()) && !ServantCardGilgameshSkills.hasKey(player)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.gilgamesh_key_required"), true);
         return false;
      }
      if ("gilgamesh_divine_shield".equals(action.effectId())) {
         if (GilgameshDivineShield.isActive(player)) {
            if (ServantCardGilgameshSkills.performDivineShield(player, vars)) {
               player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.skill_activated", Component.translatable(skillTranslationKey(action))), true);
            }
            return true;
         }
         int shieldCooldown = GilgameshDivineShield.cooldownRemaining(player);
         if (shieldCooldown > 0) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.cooldown", String.format(java.util.Locale.ROOT, "%.1f", shieldCooldown / 20.0F)), true);
            return false;
         }
      }
      int cooldownSlot = slot < 0 ? 6 : slot;
      int currentCooldown = unlimited ? 0 : npSlot ? vars.servant_card_np_cooldown : getSkillCooldown(vars, cooldownSlot);
      if (!unlimited && "zhao_yun_summon_hakuryu".equals(action.effectId())) {
         currentCooldown = Math.max(currentCooldown, ServantCardZhaoYunSkills.summonCooldownRemaining(player));
      }
      if (!unlimited && ServantCardArtoriaSkills.isWindAction(action)) {
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
      if ("copy_weapon".equals(action.effectId())
         && ServantCardEmiyaSkills.findCopyableWeaponTarget(player, 20.0, 1.6) == null
         && ServantCardEmiyaSkills.findCrossSlashCopyTarget(player, 40.0) == null) {
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
      if ("ushiwakamaru_moonlit_step".equals(action.effectId()) && !ServantCardUshiwakamaruSkills.hasMoonlitTarget(player)
         || "ushiwakamaru_usumidori".equals(action.effectId()) && !ServantCardUshiwakamaruSkills.hasUsumidoriTarget(player)
         || "ushiwakamaru_eight_boat".equals(action.effectId()) && !ServantCardUshiwakamaruSkills.hasEightBoatTarget(player)
         || "ushiwakamaru_spider_slayer".equals(action.effectId()) && !ServantCardUshiwakamaruSkills.hasSpiderSlayerTargets(player)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
         return false;
      }
      if ("ushiwakamaru_eagle_drop".equals(action.effectId()) && player.onGround()) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.must_be_airborne"), true);
         return false;
      }
      if (ServantCardActionPreconditions.requiresLookTarget(action.effectId())
         && isInvalidLookTarget(player, action.effectId())) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
         return false;
      }
      if ("shadow_hassan_meditative_sensitivity".equals(action.effectId())
         && !ServantCardShadowHassanSkills.hasMeditativeSensitivityTarget(player)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
         return false;
      }
      if (("arash_arrow_rain".equals(action.effectId()) || "arash_energy_small".equals(action.effectId())
         || "arash_energy_large".equals(action.effectId())) && !ServantCardArashSkills.hasRequiredBow(player)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.arash_bow_required"), true);
         return false;
      }
      if ("arash_stella".equals(action.effectId())) {
         return ServantCardArashSkills.performStellaAction(player, vars, action);
      }
      if ("nightingale_pledge".equals(action.effectId())) {
         return ServantCardNightingaleSkills.performNoblePhantasmAction(player, vars, action);
      }
      if ("baobhan_sith_fetch_failnaught".equals(action.effectId())) {
         return ServantCardBaobhanSithSkills.openFetchFailnaughtMap(player);
      }
      double mpCost = ServantCardSkillCostRules.effectiveMpCost(vars, action);
      boolean zhaoYunBreakthroughDiscount =
         "zhao_yun_rider".equals(vars.servant_card_id)
            && ServantCardZhaoYunSkills.hasSevenInSevenOutDiscount(player)
            && ServantCardZhaoYunSkills.isMeleeSmallSkill(action.effectId());
      boolean ubwFreeLayeredProjection =
         "emiya_archer".equals(vars.servant_card_id)
            && "emiya_layered_projection".equals(action.effectId())
            && vars.is_in_ubw;
      if (zhaoYunBreakthroughDiscount) {
         mpCost = 0.0;
      }
      if (ubwFreeLayeredProjection) {
         mpCost = 0.0;
      }
      ServantCardManaService.ManaSnapshot manaBeforeAction = ServantCardManaService.snapshot(player, vars);
      boolean paid = np ? ServantCardManaService.consumeNoblePhantasm(player, vars, mpCost) : ServantCardManaService.consume(player, vars, mpCost);
      if (!paid) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
         return false;
      }
      if (!performAction(player, vars, action)) {
         ServantCardManaService.restore(player, vars, manaBeforeAction);
         return false;
      }
      if (!"three_thousand".equals(action.effectId()) && !"hajun".equals(action.effectId()) && !"oda_charged_matchlock".equals(action.effectId())) {
         ServantCardVoiceHelper.tryPlaySkill(player, action.effectId());
      }
      int cooldownTicks = effectiveCooldownTicks(action, npSlot);
      if (zhaoYunBreakthroughDiscount) {
         cooldownTicks = Math.max(1, (cooldownTicks + 1) / 2);
      }
      if (ubwFreeLayeredProjection) {
         cooldownTicks = 0;
      }
      if (npSlot) {
         setNoblePhantasmCooldown(player, vars, cooldownTicks);
      } else if (ServantCardArtoriaSkills.isWindAction(action)) {
         setSkillCooldown(player, vars, ServantCardArtoriaSkills.WIND_HAMMER_SLOT, cooldownTicks);
         setSkillCooldown(player, vars, ServantCardArtoriaSkills.WIND_RELEASE_SLOT, cooldownTicks);
      } else {
         setSkillCooldown(player, vars, cooldownSlot, cooldownTicks);
      }
      vars.syncPlayerVariables(player);
      player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.skill_activated", Component.translatable(skillTranslationKey(action))), true);
      return true;
   }

   private static boolean isInvalidLookTarget(ServerPlayer player, String actionId) {
      var target = ServantCardSkillUtils.findLookTarget(player,
         ServantCardActionPreconditions.targetRangeFor(actionId), 1.8);
      if (target == null) {
         return true;
      }
      // Rule Breaker is explicitly allowed to target Medea's contract master so it
      // can terminate the contract. Other targeted actions keep the automatic
      // contract-master exclusion.
      boolean mayTargetContractMaster = "rule_breaker".equals(actionId)
         && "medea".equals(player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servant_card_id);
      return !mayTargetContractMaster && ServantMasterTargeting.isContractMaster(player, target);
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
      PlayerNoblePhantasmHelper.finishServantCardVoiceSession(
         player, "nightingale", ModSounds.NIGHTINGALE_VOICE_NP.get(), null);
      PlayerNoblePhantasmHelper.finishServantCardVoiceSession(
         player, "okita_souji_saber", ModSounds.OKITA_SOUJI_SABER_VOICE_NP.get(), null);
   }

   public static void handleHoldAction(ServerPlayer player, int slot, boolean pressed) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || ServantMasterCarryService.isCarryingMaster(player)) {
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
      } else if ("arash".equals(vars.servant_card_id) && slot == 9) {
         if (pressed) {
            triggerAction(player, slot);
         } else {
            ServantCardArashSkills.requestPlayerStellaRelease(player);
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
      String externalKey = net.xxxjk.TYPE_MOON_WORLD.api.CardActionRegistry.translationKey(servantId, slot);
      if (!externalKey.isBlank()) {
         return externalKey;
      }
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
      if ("shadow_hassan".equals(vars.servant_card_id) && !ServantCardShadowHassanSkills.canAttack(player)) return false;
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
      vars.servant_card_jump_recovery_ticks = ServantCardJumpRecoveryRules.recoveryTicksFor(vars.servant_card_jump_charges);
      vars.servant_card_jump_recovery_end = 0L;
      vars.syncPlayerVariables(player);
      return true;
   }

   private static void applyAttributes(ServerPlayer player, ServantParams params, String servantId) {
      removeAttributes(player);
      addOrReplace(player.getAttribute(Attributes.MAX_HEALTH), MAX_HEALTH_ID, params.maxHealth() - player.getAttributeBaseValue(Attributes.MAX_HEALTH));
      addOrReplace(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_ID, params.attackDamage() - player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));
      addOrReplace(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, params.movementSpeed() - player.getAttributeBaseValue(Attributes.MOVEMENT_SPEED));
      addOrReplace(player.getAttribute(Attributes.ARMOR), ARMOR_ID, params.armor());
      addOrReplace(player.getAttribute(Attributes.ARMOR_TOUGHNESS), TOUGHNESS_ID, armorToughnessBonus(params));
      double knockbackResistance = "heracles".equals(servantId)
         ? Math.max(0.0, 1.0 - player.getAttributeBaseValue(Attributes.KNOCKBACK_RESISTANCE))
         : knockbackResistanceBonus(params);
      addOrReplace(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK_RESISTANCE_ID, knockbackResistance);
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
      if ("pale_rider".equals(servantId)) {
         for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) player.setItemSlot(slot, ItemStack.EMPTY);
         }
         return;
      }
      if ("shadow_hassan".equals(servantId)) {
         player.setItemSlot(EquipmentSlot.HEAD, generatedArmor(servantId, EquipmentSlot.HEAD));
         player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
         player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
         player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
         return;
      }
      if ("fanatic_assassin".equals(servantId)) {
         player.setItemSlot(EquipmentSlot.HEAD, generatedArmor(servantId, EquipmentSlot.HEAD));
         player.setItemSlot(EquipmentSlot.CHEST, generatedArmor(servantId, EquipmentSlot.CHEST));
         player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
         player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
         return;
      }
      if ("medea".equals(servantId)) {
         player.setItemSlot(EquipmentSlot.HEAD, markGeneratedItem(new ItemStack(ModItems.getServantCardArmor(servantId, EquipmentSlot.HEAD)), false, false));
         player.setItemSlot(EquipmentSlot.CHEST, markGeneratedItem(new ItemStack(ModItems.getServantCardArmor(servantId, EquipmentSlot.CHEST)), false, false));
         player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
         return;
      }
      if ("diarmuid_ua_duibhne".equals(servantId)) {
         player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
         player.setItemSlot(EquipmentSlot.CHEST, generatedArmor(servantId, EquipmentSlot.CHEST));
         player.setItemSlot(EquipmentSlot.LEGS, generatedArmor(servantId, EquipmentSlot.LEGS));
         player.setItemSlot(EquipmentSlot.FEET, generatedArmor(servantId, EquipmentSlot.FEET));
         return;
      }
      if ("lancelot_berserker".equals(servantId)) {
         player.setItemSlot(EquipmentSlot.HEAD, generatedArmor(servantId, EquipmentSlot.HEAD));
         player.setItemSlot(EquipmentSlot.CHEST, generatedArmor(servantId, EquipmentSlot.CHEST));
         player.setItemSlot(EquipmentSlot.LEGS, generatedArmor(servantId, EquipmentSlot.LEGS));
         player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
         return;
      }
      if ("iskandar".equals(servantId)) {
         player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
         player.setItemSlot(EquipmentSlot.CHEST, generatedArmor(servantId, EquipmentSlot.CHEST));
         player.setItemSlot(EquipmentSlot.LEGS, generatedArmor(servantId, EquipmentSlot.LEGS));
         player.setItemSlot(EquipmentSlot.FEET, generatedArmor(servantId, EquipmentSlot.FEET));
         return;
      }
      if ("gilles_de_rais_caster".equals(servantId)) {
         player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
         player.setItemSlot(EquipmentSlot.CHEST, addonArmor("typemoonworld:cursed_armor_render"));
         player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
         player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
         return;
      }
      if (servantCardHasHeadArmor(servantId)) {
         player.setItemSlot(EquipmentSlot.HEAD, generatedArmor(servantId, EquipmentSlot.HEAD));
      } else {
         player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
      }
      player.setItemSlot(EquipmentSlot.CHEST, generatedArmor(servantId, EquipmentSlot.CHEST));
      if (servantCardHasLegArmor(servantId)) {
         player.setItemSlot(EquipmentSlot.LEGS, generatedArmor(servantId, EquipmentSlot.LEGS));
      } else {
         player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
      }
   }

   private static boolean servantCardHasHeadArmor(String servantId) {
      return "artoria_pendragon".equals(servantId)
         || "sasaki_kojiro".equals(servantId)
         || "enkidu".equals(servantId)
         || "medusa".equals(servantId)
         || "cursed_arm_hassan".equals(servantId)
         || "li_shuwen".equals(servantId)
         || "oda_nobunaga".equals(servantId)
         || "paracelsus".equals(servantId)
         || "ushiwakamaru_rider".equals(servantId)
         || "zhao_yun_rider".equals(servantId)
         || "gilgamesh_caster".equals(servantId)
         || "baobhan_sith".equals(servantId)
         || "hundred_faces_hassan".equals(servantId);

   }

   private static ItemStack generatedArmor(String servantId, EquipmentSlot slot) {
      ItemStack stack = new ItemStack(ModItems.getServantCardArmor(servantId, slot));
      if (servantId != null && servantId.indexOf(':') >= 0 && stack.getItem() instanceof net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantCardArmorItem) {
         net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantCardArmorItem.create(stack, servantId);
      }
      return markGeneratedItem(stack, false, false);
   }

   private static ItemStack addonArmor(String id) {
      ResourceLocation key = ResourceLocation.tryParse(id);
      if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
         return ItemStack.EMPTY;
      }
      return markGeneratedItem(new ItemStack(BuiltInRegistries.ITEM.get(key)), false, false);
   }

   private static boolean servantCardHasLegArmor(String servantId) {
      return !"medea".equals(servantId) && !"cursed_arm_hassan".equals(servantId)
         && !"fanatic_assassin".equals(servantId) && !"hundred_faces_hassan".equals(servantId);
   }

   private static boolean tickJumpRecovery(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      boolean eightBoatActive = ServantCardUshiwakamaruSkills.isEightBoatActive(player);
      int maxCharges = ServantCardUshiwakamaruRules.jumpLimit(eightBoatActive);
      long now = player.level().getGameTime();
      ServantCardJumpRecoveryRules.State normalized = ServantCardJumpRecoveryRules.normalize(
         vars.servant_card_jump_charges,
         vars.servant_card_jump_recovery_ticks,
         vars.servant_card_jump_recovery_end,
         eightBoatActive,
         now
      );
      boolean changed = normalized.changed();
      vars.servant_card_jump_charges = normalized.charges();
      vars.servant_card_jump_recovery_ticks = normalized.recoveryTicks();
      vars.servant_card_jump_recovery_end = normalized.recoveryEnd();
      if (vars.servant_card_jump_charges >= maxCharges) {
         return changed;
      }
      if (!hasJumpRecoverySupport(player)) {
         if (vars.servant_card_jump_recovery_end > 0L) {
            vars.servant_card_jump_recovery_ticks = ServantCardJumpRecoveryRules.clampTicks(
               remainingTicks(now, vars.servant_card_jump_recovery_end));
            vars.servant_card_jump_recovery_end = 0L;
            changed = true;
         } else if (vars.servant_card_jump_recovery_ticks <= 0) {
            vars.servant_card_jump_recovery_ticks = ServantCardJumpRecoveryRules.recoveryTicksFor(vars.servant_card_jump_charges);
            changed = true;
         }
         return changed;
      }
      if (vars.servant_card_jump_recovery_end <= 0L && vars.servant_card_jump_recovery_ticks > 0) {
         vars.servant_card_jump_recovery_ticks = ServantCardJumpRecoveryRules.clampTicks(vars.servant_card_jump_recovery_ticks);
         vars.servant_card_jump_recovery_end = now + vars.servant_card_jump_recovery_ticks;
         changed = true;
      }
      vars.servant_card_jump_recovery_ticks = remainingTicks(now, vars.servant_card_jump_recovery_end);
      if (vars.servant_card_jump_recovery_ticks > ServantCardJumpRecoveryRules.EMPTY_RECOVERY_TICKS) {
         vars.servant_card_jump_recovery_ticks = ServantCardJumpRecoveryRules.recoveryTicksFor(vars.servant_card_jump_charges);
         vars.servant_card_jump_recovery_end = now + vars.servant_card_jump_recovery_ticks;
         changed = true;
      }
      if (vars.servant_card_jump_recovery_ticks > 0) {
         return true;
      }
      vars.servant_card_jump_charges = Math.min(maxCharges, vars.servant_card_jump_charges + 1);
      vars.servant_card_jump_recovery_ticks = ServantCardJumpRecoveryRules.recoveryTicksFor(vars.servant_card_jump_charges);
      vars.servant_card_jump_recovery_end = vars.servant_card_jump_charges >= maxCharges ? 0L : now + vars.servant_card_jump_recovery_ticks;
      return true;
   }

   private static boolean hasJumpRecoverySupport(ServerPlayer player) {
      if (player.onGround()) {
         return true;
      }
      if (player.getVehicle() != null && player.getVehicle().onGround()) {
         return true;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if ("oda_nobunaga".equals(vars.servant_card_id)
         && !vars.servant_card_flying
         && ServantCardFlightController.hasGroundWithin(player, 2)) {
         return true;
      }
      return ServantCardUshiwakamaruRules.canRecoverJump(
         ServantCardUshiwakamaruSkills.isEightBoatActive(player), false);
   }

   private static boolean tickSkillCooldowns(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      long now = player.level().getGameTime();
      long[] ends = parseSkillCooldownEnds(vars);
      int[] legacy = parseSkillCooldowns(vars);
      boolean changed = false;
      for (int i = 0; i < ends.length; i++) {
         if (ends[i] <= 0L && legacy[i] > 0) {
            ends[i] = now + legacy[i];
            changed = true;
         }
      }
      int[] remaining = new int[10];
      for (int i = 0; i < remaining.length; i++) {
         remaining[i] = remainingTicks(now, ends[i]);
         if (remaining[i] == 0) {
            ends[i] = 0L;
         }
      }
      String serialized = serializeSkillCooldowns(remaining);
      String serializedEnds = serializeSkillCooldownEnds(ends);
      changed |= !serialized.equals(vars.servant_card_skill_cooldowns) || !serializedEnds.equals(vars.servant_card_skill_cooldown_ends);
      vars.servant_card_skill_cooldowns = serialized;
      vars.servant_card_skill_cooldown_ends = serializedEnds;
      return changed;
   }

   public static int getSkillCooldown(TypeMoonWorldModVariables.PlayerVariables vars, int slot) {
      if (slot < 0 || slot >= 10) {
         return 0;
      }
      return parseSkillCooldowns(vars)[slot];
   }

   private static void setSkillCooldown(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, int slot, int ticks) {
      if (ServantCardUnlimitedMode.isEnabled(player)) {
         return;
      }
      int[] cooldowns = parseSkillCooldowns(vars);
      long[] ends = parseSkillCooldownEnds(vars);
      if (slot >= 0 && slot < cooldowns.length) {
         cooldowns[slot] = Math.max(0, ticks);
         ends[slot] = ticks > 0 ? player.level().getGameTime() + ticks : 0L;
         vars.servant_card_skill_cooldowns = serializeSkillCooldowns(cooldowns);
         vars.servant_card_skill_cooldown_ends = serializeSkillCooldownEnds(ends);
      }
   }

   /** Mirrors an absolute cooldown owned by a stateful skill into the card HUD. */
   public static void setSkillCooldownUntil(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, int slot, long until) {
      setSkillCooldown(player, vars, slot, remainingTicks(player.level().getGameTime(), until));
   }

   private static boolean tickNoblePhantasmCooldown(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      long now = player.level().getGameTime();
      if (vars.servant_card_np_cooldown_end <= 0L && vars.servant_card_np_cooldown > 0) {
         vars.servant_card_np_cooldown_end = now + vars.servant_card_np_cooldown;
      }
      int remaining = remainingTicks(now, vars.servant_card_np_cooldown_end);
      boolean changed = remaining != vars.servant_card_np_cooldown;
      vars.servant_card_np_cooldown = remaining;
      if (remaining == 0) {
         vars.servant_card_np_cooldown_end = 0L;
      }
      return changed;
   }

   public static void setNoblePhantasmCooldown(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, int ticks) {
      if (ServantCardUnlimitedMode.isEnabled(player)) {
         vars.servant_card_np_cooldown = 0;
         vars.servant_card_np_cooldown_end = 0L;
         return;
      }
      vars.servant_card_np_cooldown = Math.max(0, ticks);
      vars.servant_card_np_cooldown_end = ticks > 0 ? player.level().getGameTime() + ticks : 0L;
   }

   private static int remainingTicks(long now, long end) {
      return end <= now ? 0 : (int)Math.min(Integer.MAX_VALUE, end - now);
   }

   private static long[] parseSkillCooldownEnds(TypeMoonWorldModVariables.PlayerVariables vars) {
      long[] result = new long[10];
      String raw = vars.servant_card_skill_cooldown_ends == null ? "" : vars.servant_card_skill_cooldown_ends;
      if (raw.isBlank()) {
         return result;
      }
      String[] parts = raw.split(",");
      for (int i = 0; i < result.length && i < parts.length; i++) {
         try {
            result[i] = Math.max(0L, Long.parseLong(parts[i]));
         } catch (NumberFormatException ignored) {
            result[i] = 0L;
         }
      }
      return result;
   }

   private static String serializeSkillCooldownEnds(long[] ends) {
      boolean any = false;
      for (long end : ends) {
         if (end > 0L) {
            any = true;
            break;
         }
      }
      if (!any) {
         return "";
      }
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < 10; i++) {
         if (i > 0) builder.append(',');
         builder.append(i < ends.length ? Math.max(0L, ends[i]) : 0L);
      }
      return builder.toString();
   }

   private static int[] parseSkillCooldowns(TypeMoonWorldModVariables.PlayerVariables vars) {
      int[] result = new int[10];
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
      for (int i = 0; i < 10; i++) {
         if (i < cooldowns.length && cooldowns[i] > 0) {
            any = true;
            break;
         }
      }
      if (!any) {
         return "";
      }
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < 10; i++) {
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
      if ("zhao_yun_changbanpo".equals(id) || "iskandar_ionioi_hetairoi".equals(id)) {
         return action.cooldownTicks();
      }
      if ("baobhan_sith_fetch_failnaught".equals(id)) {
         return action.cooldownTicks();
      }
      if ("gilles_uncontrolled_huge_sea_monster".equals(id)) {
         return action.cooldownTicks();
      }
      int cooldown = action.cooldownTicks();
      if ("zabaniya".equals(id) || "wu_er_da".equals(id)) {
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
         case "okita_shukuchi" -> {
            if (!ServantCardOkitaSoujiSaberSkills.performShukuchi(player)) return false;
         }
         case "okita_ichimonji" -> {
            if (!ServantCardOkitaSoujiSaberSkills.performIchimonji(player)) return false;
         }
         case "okita_kaifuu" -> {
            if (!ServantCardOkitaSoujiSaberSkills.performKaifuu(player)) return false;
         }
         case "okita_mind_eye" -> {
            if (!ServantCardOkitaSoujiSaberSkills.performMindEye(player)) return false;
         }
         case "okita_oath_haori" -> {
            if (!ServantCardOkitaSoujiSaberSkills.performHaoriRush(player)) return false;
         }
         case "okita_feigned_retreat" -> {
            if (!ServantCardOkitaSoujiSaberSkills.performFeignedRetreat(player)) return false;
         }
         case "okita_stance_break" -> {
            if (!ServantCardOkitaSoujiSaberSkills.performStanceBreak(player)) return false;
         }
         case "okita_shinsengumi_command" -> {
            if (!ServantCardOkitaSoujiSaberSkills.performCommand(player)) return false;
         }
         case "okita_mumyoudan_zuki" -> {
            if (ServantCardOkitaSoujiSaberSkills.isWeak(player)) {
               player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.okita_weak_np_blocked"), true);
               return false;
            }
            if (!ServantCardOkitaSoujiSaberSkills.performMumyoudanZuki(player)) return false;
         }
         case "okita_flag_of_sincerity" -> {
            if (!ServantCardOkitaSoujiSaberSkills.performFlagOfSincerity(player)) return false;
         }
         case "gilles_summon_small_sea_monster" -> {
            if (!ServantCardGillesDeRaisSkills.summonSmallSeaMonsters(player)) return false;
         }
         case "gilles_summon_large_sea_monster" -> {
            if (!ServantCardGillesDeRaisSkills.summonLargeSeaMonster(player)) return false;
         }
         case "gilles_abyssal_gaze" -> {
            if (!ServantCardGillesDeRaisSkills.performAbyssalGaze(player)) return false;
         }
         case "gilles_life_absorb" -> {
            if (!ServantCardGillesDeRaisSkills.performLifeAbsorb(player)) return false;
         }
         case "gilles_sea_monster_command" -> {
            if (!ServantCardGillesDeRaisSkills.performCommand(player)) return false;
         }
         case "gilles_pollution_ink_fog" -> {
            if (!ServantCardGillesDeRaisSkills.performPollutionInkFog(player)) return false;
         }
         case "gilles_prelati_shroud" -> {
            if (!ServantCardGillesDeRaisSkills.performPrelatiShroud(player)) return false;
         }
         case "gilles_profane_growth" -> {
            if (!ServantCardGillesDeRaisSkills.performProfaneGrowth(player)) return false;
         }
         case "gilles_evil_god_praise" -> {
            if (!ServantCardGillesDeRaisSkills.performEvilGodPraise(player)) return false;
         }
         case "gilles_uncontrolled_huge_sea_monster" -> {
            if (!ServantCardGillesDeRaisSkills.summonHugeSeaMonster(player)) return false;
         }
         case "nightingale_steel_nursing" -> {
            if (!ServantCardNightingaleSkills.performSteelNursing(player)) return false;
         }
         case "nightingale_angel_cry" -> {
            if (!ServantCardNightingaleSkills.performAngelCry(player)) return false;
         }
         case "arash_arrow_rain" -> {
            if (!ServantCardArashSkills.performArrowRain(player)) return false;
         }
         case "arash_energy_small" -> {
            if (!ServantCardArashSkills.performSmallEnergyArrow(player)) return false;
         }
         case "arash_energy_large" -> {
            if (!ServantCardArashSkills.performLargeEnergyArrow(player)) return false;
         }
         case "arash_arrow_creation" -> {
            if (!ServantCardArashSkills.performArrowCreation(player)) return false;
         }
         case "mana_burst" -> ServantCardArtoriaSkills.performManaBurst(player);
         case "charisma" -> ServantCardArtoriaSkills.performCharisma(player);
         case "strategy" -> ServantCardOdaNobunagaSkills.performOdaStrategy(player);
         case "stealth" -> ServantCardCommonSkills.performPresenceConcealment(player, vars.servant_card_id);
         case "circle_realm" -> ServantCardLiShuwenSkills.performCircleRealm(player);
         case "berkana" -> ServantCardCommonSkills.performBerkana(player);
         case "perfect_form" -> ServantCardEnkiduSkills.performEnkiduPerfectForm(player);
         case "rho_aias" -> ServantCardEmiyaSkills.spawnRhoAias(player);
         case "invisible_air_hammer" -> ServantCardArtoriaSkills.performInvisibleAirHammer(player, action.cooldownTicks());
         case "invisible_air_release" -> ServantCardArtoriaSkills.performInvisibleAirRelease(player, action.cooldownTicks());
         case "artoria_small_combo" -> ServantCardArtoriaSkills.performSmallCombo(player);
         case "artoria_instinct" -> ServantCardArtoriaSkills.performInstinct(player);
         case "artoria_riding" -> ServantCardArtoriaSkills.performRiding(player);
         case "mana_burst_beam" -> ServantCardArtoriaSkills.performManaBurstBeam(player);
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
         case "cu_disengage" -> ServantCardCuChulainnSkills.performDisengage(player);
         case "heracles_big_jump" -> ServantCardHeraclesSkills.performBigJump(player);
         case "heracles_ground_slam" -> ServantCardHeraclesSkills.performGroundSlam(player, true);
         case "heracles_roar" -> ServantCardHeraclesSkills.performRoar(player);
         case "heracles_valor" -> ServantCardHeraclesSkills.performValor(player);
         case "heracles_mind_eye" -> ServantCardHeraclesSkills.performMindEye(player);
         case "heracles_battle_continuation" -> ServantCardHeraclesSkills.performBattleContinuation(player);
         case "god_hand_status" -> ServantCardHeraclesSkills.showGodHandStatus(player);
         case "algiz" -> ServantCardGawainSkills.performGuard(player);
         case "barrier" -> ServantCardMedeaSkills.performMedeaBarrier(player);
         case "bulwark" -> ServantCardEnkiduSkills.performEnkiduBulwark(player);
         case "self_mod" -> ServantCardHassanSkills.performSelfModification(player);
         case "dirk_throw" -> ServantCardHassanSkills.performDirkThrow(player);
         case "shadow_step" -> ServantCardHassanSkills.performShadowStep(player);
         case "shadow_lunge" -> ServantCardHassanSkills.performShadowLunge(player);
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
         case "monster_strength" -> ServantCardMedusaSkills.performMedusaMonsterStrength(player);
         case "viper_rush" -> ServantCardMedusaSkills.performViperRush(player);
         case "serpent_step" -> ServantCardMedusaSkills.performSerpentStep(player);
         case "medusa_mystic_eyes" -> ServantCardMedusaSkills.performMysticEyesToggle(player);
         case "bloodfort_field" -> ServantCardMedusaSkills.performBloodfort(player, false);
         case "bellerophon" -> ServantCardMedusaSkills.performBellerophon(player);
         case "sasaki_afterimage" -> ServantCardSasakiKojiroSkills.performAfterimage(player);
         case "sasaki_mind_eye" -> ServantCardSasakiKojiroSkills.performMindEye(player);
         case "sasaki_sweep" -> ServantCardSasakiKojiroSkills.performSweep(player);
         case "sasaki_transparency" -> ServantCardSasakiKojiroSkills.performTransparency(player);
         case "hassan_dagger" -> ServantCardHassanSkills.giveDirk(player);
         case "zabaniya" -> {
            if (!ServantCardHassanSkills.performZabaniya(player)) {
               return false;
            }
         }
         case "emiya_kb" -> ServantCardEmiyaSkills.equipPair(player, ModItems.GAN_JIANG.get(), ModItems.MO_YE.get());
         case "emiya_spiral" -> ServantCardEmiyaSkills.equipUniqueAmmo(player, ModItems.PSEUDO_SPIRAL_SWORD.get());
         case "emiya_hound" -> ServantCardEmiyaSkills.equipUniqueAmmo(player, ModItems.CRIMSON_HOUND.get());
         case "copy_weapon" -> {
            if (!ServantCardEmiyaSkills.copyOpponentWeapon(player)) {
               return false;
            }
         }
         case "emiya_layered_projection" -> ServantCardEmiyaSkills.startLayeredProjection(player);
         case "emiya_spherical_projection" -> {
            if (!ServantCardEmiyaSkills.performSphericalProjection(player)) return false;
         }
         case "emiya_auto_counter" -> ServantCardEmiyaSkills.startAutoCounter(player);
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
         case "gilgamesh_melee" -> ServantCardGilgameshSkills.performMelee(player);
         case "gilgamesh_chains" -> ServantCardGilgameshSkills.performChains(player);
         case "gilgamesh_vault" -> ServantCardGilgameshSkills.performVault(player, false);
         case "gilgamesh_grand_vault" -> ServantCardGilgameshSkills.performVault(player, true);
         case "gilgamesh_ring_vault" -> ServantCardGilgameshSkills.performRingVault(player);
         case "gilgamesh_elixir" -> ServantCardGilgameshSkills.performElixir(player);
         case "gilgamesh_divine_shield" -> { if (!ServantCardGilgameshSkills.performDivineShield(player, vars)) return false; }
         case "gilgamesh_clairvoyance" -> ServantCardGilgameshSkills.performClairvoyance(player);
         case "gilgamesh_charisma" -> ServantCardGilgameshSkills.performCharisma(player);
         case "gilgamesh_laugh_vault" -> ServantCardGilgameshSkills.performLaughVault(player);
         case "gilgamesh_cross_slash" -> ServantCardGilgameshSkills.performCrossSlash(player);
         case "caster_gilgamesh_slate_volley" -> {
            if (!ServantCardCasterGilgameshSkills.performSlateVolley(player)) return false;
         }
         case "caster_gilgamesh_leader" -> {
            if (!ServantCardCasterGilgameshSkills.performLeader(player)) return false;
         }
         case "caster_gilgamesh_return" -> {
            if (!ServantCardCasterGilgameshSkills.performReturn(player)) return false;
         }
         case "caster_gilgamesh_item_creation" -> {
            if (!ServantCardCasterGilgameshSkills.performItemCreation(player, vars)) return false;
         }
         case "caster_gilgamesh_workshop" -> {
            if (!ServantCardCasterGilgameshSkills.performWorkshop(player)) return false;
         }
         case "caster_gilgamesh_cannon_calibration" -> {
            if (!ServantCardCasterGilgameshSkills.performCannonCalibration(player, vars)) return false;
         }
         case "caster_gilgamesh_royal_cannon" -> {
            if (!ServantCardCasterGilgameshSkills.toggleRoyalCannon(player, vars)) return false;
         }
         case "gallatin_spark" -> ServantCardGawainSkills.performGawainGallatinSpark(player);
         case "solar_rebuke" -> ServantCardGawainSkills.performGawainSolarRebuke(player);
         case "radiant_field" -> ServantCardGawainSkills.performGawainRadiantField(player);
         case "flame_tornado" -> ServantCardGawainSkills.performGawainFlameTornado(player);
         case "solar_combo" -> ServantCardGawainSkills.performGawainSolarCombo(player);
         case "noon_guard" -> ServantCardGawainSkills.performGuard(player);
         case "gawain_charisma" -> ServantCardGawainSkills.performCharisma(player);
         case "yin_yang" -> ServantCardLiShuwenSkills.performLiYinYang(player);
         case "shoulder_charge" -> ServantCardLiShuwenSkills.performLiShoulder(player);
         case "interrupt" -> ServantCardLiShuwenSkills.performLiInterrupt(player);
         case "counter" -> ServantCardLiShuwenSkills.performLiCounter(player);
         case "pursuit" -> ServantCardLiShuwenSkills.performLiPursuit(player);
         case "li_fierce_tiger" -> ServantCardLiShuwenSkills.performLiFierceTiger(player);
         case "li_baji_combo" -> ServantCardLiShuwenSkills.performLiBajiCombo(player);
         case "li_high_jump" -> ServantCardLiShuwenSkills.performLiHighJump(player);
         case "li_fa_jin" -> ServantCardLiShuwenSkills.performLiFaJin(player);
         case "pale_rider_spawn" -> { if (!ServantCardPaleRiderSkills.spawnMenu(player, player.isCrouching())) return false; }
         case "pale_rider_possession" -> { if (!ServantCardPaleRiderSkills.openPossession(player)) return false; }
         case "pale_rider_command" -> { if (!ServantCardPaleRiderSkills.openCommand(player)) return false; }
         case "pale_rider_stealth" -> ServantCardPaleRiderSkills.togglePerfectConcealment(player);
         case "pale_rider_transfer" -> { if (!ServantCardPaleRiderSkills.transfer(player)) return false; }
         case "pale_rider_plague_rush" -> { if (!ServantCardPaleRiderSkills.plagueRush(player)) return false; }
         case "pale_rider_ash_step" -> { if (!ServantCardPaleRiderSkills.ashStep(player)) return false; }
         case "pale_rider_death_pulse" -> { if (!ServantCardPaleRiderSkills.deathPulse(player)) return false; }
         case "pale_rider_underworld" -> { if (!ServantCardPaleRiderSkills.toggleUnderworld(player)) return false; }
         case "pale_rider_calamity" -> { if (!ServantCardPaleRiderSkills.toggleCalamity(player)) return false; }
         case "hundred_faces_summon" -> { if (!ServantCardHundredFacesHassanSkills.summonOrOpenMenu(player, false)) return false; }
         case "hundred_faces_summon_menu" -> { if (!ServantCardHundredFacesHassanSkills.summonOrOpenMenu(player, true)) return false; }
         case "hundred_faces_command" -> { if (!ServantCardHundredFacesHassanSkills.openGlobalCommand(player)) return false; }
         case "hundred_faces_single_command" -> { if (!ServantCardHundredFacesHassanSkills.openSingleCommand(player)) return false; }
         case "hundred_faces_switch" -> { if (!ServantCardHundredFacesHassanSkills.openSwitch(player)) return false; }
         case "hundred_faces_concealment" -> { if (!ServantCardHundredFacesHassanSkills.performPresenceConcealment(player)) return false; }
         case "diarmuid_twin_spear_combo" -> ServantCardDiarmuidSkills.performTwinSpearCombo(player);
         case "diarmuid_red_rose_focus" -> ServantCardDiarmuidSkills.performRedRoseFocus(player);
         case "diarmuid_yellow_rose_focus" -> ServantCardDiarmuidSkills.performYellowRoseFocus(player);
         case "diarmuid_knight_strategy" -> ServantCardDiarmuidSkills.performKnightStrategy(player);
         case "diarmuid_mana_burst_jump" -> ServantCardDiarmuidSkills.performManaBurstJump(player);
         case "diarmuid_flower_step" -> ServantCardDiarmuidSkills.performFlowerStep(player);
         case "diarmuid_disengage" -> ServantCardDiarmuidSkills.performDisengage(player);
         case "diarmuid_dual_wield" -> ServantCardDiarmuidSkills.performDualWieldToggle(player);
         case "lancelot_maul_combo" -> ServantCardLancelotBerserkerSkills.performMaulCombo(player);
         case "lancelot_feral_rush" -> ServantCardLancelotBerserkerSkills.performFeralRush(player);
         case "lancelot_ground_slam" -> ServantCardLancelotBerserkerSkills.performGroundSlam(player);
         case "lancelot_hunt_step" -> ServantCardLancelotBerserkerSkills.performHuntStep(player);
         case "lancelot_mana_reversal" -> ServantCardLancelotBerserkerSkills.performManaReversal(player);
         case "lancelot_berserk_roar" -> ServantCardLancelotBerserkerSkills.performBerserkRoar(player);
         case "lancelot_knight_of_owner" -> { if (!ServantCardLancelotBerserkerSkills.performKnightOfOwner(player)) return false; }
         case "lancelot_aroundight" -> { if (!ServantCardLancelotBerserkerSkills.performAroundight(player, vars)) return false; }
         case "iskandar_bucephalus" -> { if (!ServantCardIskandarSkills.performBucephalus(player)) return false; }
         case "iskandar_royal_sword_assault" -> { if (!ServantCardIskandarSkills.performRoyalSwordAssault(player)) return false; }
         case "iskandar_conqueror_order" -> { if (!ServantCardIskandarSkills.performConquerorOrder(player)) return false; }
         case "iskandar_thunder_call" -> { if (!ServantCardIskandarSkills.performThunderCall(player)) return false; }
         case "iskandar_battlefield_stride" -> { if (!ServantCardIskandarSkills.performBattlefieldStride(player)) return false; }
         case "iskandar_vanguard_summon" -> { if (!ServantCardIskandarSkills.performVanguardSummon(player)) return false; }
         case "iskandar_kingly_war_cry" -> { if (!ServantCardIskandarSkills.performKinglyWarCry(player)) return false; }
         case "iskandar_charge" -> { if (!ServantCardIskandarSkills.performCharge(player)) return false; }
         case "iskandar_gordius_wheel" -> { if (!ServantCardIskandarSkills.performGordiusWheel(player)) return false; }
         case "iskandar_ionioi_hetairoi" -> { if (!ServantCardIskandarSkills.performIonioiHetairoi(player, vars)) return false; }
         case "shadow_hassan_concealment" -> { if (!ServantCardShadowHassanSkills.toggleConcealment(player)) return false; }
         case "shadow_hassan_lantern" -> { if (!ServantCardShadowHassanSkills.performShadowLantern(player, vars)) return false; }
         case "shadow_hassan_wandering" -> { if (!ServantCardShadowHassanSkills.performShadowWandering(player)) return false; }
         case "shadow_hassan_ambush" -> { if (!ServantCardShadowHassanSkills.performShadowAmbush(player)) return false; }
         case "shadow_hassan_bind" -> { if (!ServantCardShadowHassanSkills.performShadowBind(player)) return false; }
         case "shadow_hassan_flurry" -> { if (!ServantCardShadowHassanSkills.performShadowFlurry(player)) return false; }
         case "shadow_hassan_retreat" -> { if (!ServantCardShadowHassanSkills.performShadowRetreat(player)) return false; }
         case "shadow_hassan_slash" -> { if (!ServantCardShadowHassanSkills.performSlash(player)) return false; }
         case "shadow_hassan_meditative_sensitivity" -> { if (!ServantCardShadowHassanSkills.performMeditativeSensitivity(player)) return false; }
         case "fanatic_concealment" -> ServantCardFanaticAssassinSkills.performConcealment(player);
         case "fanatic_heartbeat" -> { if (!ServantCardFanaticAssassinSkills.performHeartbeat(player)) return false; }
         case "fanatic_marrow" -> { if (!ServantCardFanaticAssassinSkills.performMarrow(player)) return false; }
         case "fanatic_hair" -> { if (!ServantCardFanaticAssassinSkills.performHair(player)) return false; }
         case "fanatic_temperature" -> ServantCardFanaticAssassinSkills.performTemperature(player);
         case "fanatic_nerves" -> ServantCardFanaticAssassinSkills.performNerves(player);
         case "fanatic_computer" -> { if (!ServantCardFanaticAssassinSkills.performComputer(player)) return false; }
         case "fanatic_toxin" -> ServantCardFanaticAssassinSkills.performToxin(player);
         case "fanatic_jinn" -> { if (!ServantCardFanaticAssassinSkills.performJinn(player)) return false; }
         case "ushiwakamaru_tengu_strategy" -> ServantCardUshiwakamaruSkills.performTenguStrategy(player);
         case "ushiwakamaru_charisma" -> ServantCardUshiwakamaruSkills.performCharisma(player);
         case "ushiwakamaru_moonlit_step" -> ServantCardUshiwakamaruSkills.performMoonlitStep(player);
         case "ushiwakamaru_sweeping_thrust" -> ServantCardUshiwakamaruSkills.performSweepingThrust(player);
         case "ushiwakamaru_eagle_drop" -> ServantCardUshiwakamaruSkills.performEagleDrop(player);
         case "ushiwakamaru_six_secret" -> ServantCardUshiwakamaruSkills.performSixSecret(player);
         case "ushiwakamaru_usumidori" -> ServantCardUshiwakamaruSkills.performUsumidori(player);
         case "ushiwakamaru_benkei" -> ServantCardUshiwakamaruSkills.performBenkei(player);
         case "ushiwakamaru_spider_slayer" -> { if (!ServantCardUshiwakamaruSkills.performSpiderSlayer(player)) return false; }
         case "ushiwakamaru_eight_boat" -> { if (!ServantCardUshiwakamaruSkills.performEightBoat(player, vars)) return false; }
         case "zhao_yun_spear_combo" -> { if (!ServantCardZhaoYunSkills.performBasicSpearCombo(player)) return false; }
         case "zhao_yun_summon_hakuryu" -> { if (!ServantCardZhaoYunSkills.summonSkillHakuryu(player)) return false; }
         case "zhao_yun_spear_breakthrough" -> { if (!ServantCardZhaoYunSkills.performSpearBreakthrough(player)) return false; }
         case "zhao_yun_rescue" -> { if (!ServantCardZhaoYunSkills.performRescue(player)) return false; }
         case "zhao_yun_dragon_sweep" -> { if (!ServantCardZhaoYunSkills.performDragonSweep(player)) return false; }
         case "zhao_yun_mounted_rush" -> { if (!ServantCardZhaoYunSkills.performMountedRush(player)) return false; }
         case "zhao_yun_dragon_flash" -> { if (!ServantCardZhaoYunSkills.performDragonFlash(player)) return false; }
         case "zhao_yun_seven_probe" -> { if (!ServantCardZhaoYunSkills.performSevenProbe(player)) return false; }
         case "zhao_yun_hakuryu_trample" -> { if (!ServantCardZhaoYunSkills.performHakuryuTrample(player)) return false; }
         case "zhao_yun_qinggang" -> { if (!ServantCardZhaoYunSkills.performQinggang(player)) return false; }
         case "zhao_yun_changbanpo" -> { if (!ServantCardZhaoYunSkills.performChangbanpo(player)) return false; }
         case "muramasa_workshop" -> { if (!ServantCardSenkoMuramasaSkills.performWorkshop(player)) return false; }
         case "muramasa_trial_slash" -> { if (!ServantCardSenkoMuramasaSkills.performTrial(player)) return false; }
         case "muramasa_karma_eye" -> { if (!ServantCardSenkoMuramasaSkills.performKarma(player)) return false; }
         case "muramasa_flame" -> { if (!ServantCardSenkoMuramasaSkills.performFlame(player, vars)) return false; }
         case "muramasa_projection_volley" -> { if (!ServantCardSenkoMuramasaSkills.performProjectionVolley(player)) return false; }
         case "muramasa_temper" -> { if (!ServantCardSenkoMuramasaSkills.performTemper(player)) return false; }
         case "muramasa_karma_slash" -> { if (!ServantCardSenkoMuramasaSkills.performKarmaSlash(player)) return false; }
         case "muramasa_sword_field" -> { if (!ServantCardSenkoMuramasaSkills.performSwordField(player)) return false; }
         case "muramasa_no_gen_kensai" -> { if (!ServantCardSenkoMuramasaSkills.performNoblePhantasm(player)) return false; }
         case "baobhan_sith_curse_panel" -> { if (!ServantCardBaobhanSithSkills.openCursePanel(player)) return false; }
         case "baobhan_sith_blood_spike" -> { if (!ServantCardBaobhanSithSkills.performBloodSpike(player)) return false; }
         case "baobhan_sith_blood_thorns" -> { if (!ServantCardBaobhanSithSkills.performBloodThorns(player)) return false; }
         case "baobhan_sith_curse_volley" -> { if (!ServantCardBaobhanSithSkills.performCurseVolley(player)) return false; }
         case "baobhan_sith_fingertip_dance" -> { if (!ServantCardBaobhanSithSkills.performFingertipDance(player)) return false; }
         case "baobhan_sith_night_feast" -> { if (!ServantCardBaobhanSithSkills.performNightFeast(player)) return false; }
         case "baobhan_sith_grimalkin" -> { if (!ServantCardBaobhanSithSkills.performGrimalkin(player)) return false; }
         case "baobhan_sith_blessed_successor" -> { if (!ServantCardBaobhanSithSkills.performBlessedSuccessor(player)) return false; }
         case "baobhan_sith_fairy_vampirism" -> { if (!ServantCardBaobhanSithSkills.performFairyVampirism(player)) return false; }
         case "baobhan_sith_fetch_failnaught" -> { if (!ServantCardBaobhanSithSkills.performFetchFailnaught(player)) return false; }
         default -> ServantCardCommonSkills.performFallback(player, id);
      }
      return true;
   }

   static boolean isNoblePhantasmAction(String servantId, int slot) {
      return isUshiwakamaruNoblePhantasmSlot(servantId, slot)
         || "zhao_yun_rider".equals(servantId) && (slot == 8 || slot == 9)
         || "okita_souji_saber".equals(servantId) && (slot == 8 || slot == 9)
         || slot == 9 && !"gilgamesh".equals(servantId) && !"gilgamesh_caster".equals(servantId);
   }

   static boolean isUshiwakamaruNoblePhantasmSlot(String servantId, int slot) {
      return "ushiwakamaru_rider".equals(servantId) && slot >= 5 && slot <= 9;
   }

   static boolean usesSharedNoblePhantasmCooldown(String servantId, int slot) {
      return slot == 9 && !"gilgamesh".equals(servantId) && !"gilgamesh_caster".equals(servantId)
         && !"okita_souji_saber".equals(servantId)
         && !isUshiwakamaruNoblePhantasmSlot(servantId, slot);
   }

}
