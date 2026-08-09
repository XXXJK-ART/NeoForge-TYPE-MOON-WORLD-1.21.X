package net.xxxjk.TYPE_MOON_WORLD.servant.card;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.BlackKeyItem;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicCircuitColorHelper;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.GemEngravingService;
import net.xxxjk.TYPE_MOON_WORLD.martial.BodyTrainingService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveService;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.api.MasterProfileApiRegistry;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;
import net.xxxjk.typemoonworld.api.event.MasterProfileEvent;
import net.xxxjk.TYPE_MOON_WORLD.talent.TalentService;
import net.neoforged.neoforge.common.NeoForge;

public final class MasterCardProfile {
   private static final int KIRITSUGU_ORIGIN_BULLET_STOCK = 6;

   @SuppressWarnings("unchecked")
   private static final DeferredItem<Item>[] HIGH_GEMS = new DeferredItem[]{
      ModItems.CARVED_EMERALD_HIGH,
      ModItems.CARVED_RUBY_HIGH,
      ModItems.CARVED_SAPPHIRE_HIGH,
      ModItems.CARVED_TOPAZ_HIGH,
      ModItems.CARVED_WHITE_GEMSTONE_HIGH,
      ModItems.CARVED_CYAN_GEMSTONE_HIGH,
      ModItems.CARVED_BLACK_SHARD_HIGH
   };
   @SuppressWarnings("unchecked")
   private static final DeferredItem<Item>[] HIGH_FULL_GEMS = new DeferredItem[]{
      ModItems.CARVED_EMERALD_HIGH_FULL,
      ModItems.CARVED_RUBY_HIGH_FULL,
      ModItems.CARVED_SAPPHIRE_HIGH_FULL,
      ModItems.CARVED_TOPAZ_HIGH_FULL,
      ModItems.CARVED_WHITE_GEMSTONE_HIGH_FULL,
      ModItems.CARVED_CYAN_GEMSTONE_HIGH_FULL,
      ModItems.CARVED_BLACK_SHARD_HIGH_FULL
   };

   private MasterCardProfile() {
   }

   public static boolean apply(ServerPlayer player, String masterId, InteractionHand hand) {
      Profile profile = profile(masterId);
      if (profile == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.master_card.unknown"), true);
         return false;
      }
      ResourceLocation publicProfileId = masterId != null && masterId.indexOf(':') >= 0
         ? ResourceLocation.tryParse(masterId)
         : ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, masterId == null || masterId.isBlank() ? "unknown" : masterId);
      if (NeoForge.EVENT_BUS.post(new MasterProfileEvent.Pre(player, publicProfileId)).isCanceled()) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.servant_card_transformed) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.master.servant_cannot_master"), true);
         return false;
      }
      if (vars.master_card_active) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.master_card.already_active"), true);
         return false;
      }
      ItemStack held = player.getItemInHand(hand);
      if (!held.isEmpty()) {
         held.shrink(1);
      }
      saveOriginalStateAndClearPlayer(player, vars, profile.id());
      TalentService.suspendActiveEffects(player);
      PassiveService.suspendEffects(player);
      BodyTrainingService.clear(player, vars);
      resetToProfileState(vars);
      // MasterStateManager synchronizes immediately; apply the target attributes first so
      // a previous sword attribute cannot auto-awaken Unlimited Blade Works during a switch.
      applyAttributes(vars, profile.attributes());
      if (!MasterStateManager.activateProfile(player, profile.commandSpellStyle())) {
         return false;
      }
      applyMasterCardTags(player, profile.id());

      vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.is_magus = true;
      vars.player_max_mana = profile.maxMana();
      vars.player_mana = profile.maxMana();
      vars.player_mana_egenerated_every_moment = profile.regenAmount();
      vars.player_restore_magic_moment = profile.regenIntervalTicks();
      profile.applyMagic(vars);
      grantMasterDetection(vars);
      TYPE_MOON_WORLD.queueServerWork(2, () -> {
         TypeMoonWorldModVariables.PlayerVariables delayedVars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (delayedVars.master_active && delayedVars.master_card_active && profile.id().equals(delayedVars.master_card_id)) {
            profile.giveItems(player);
         }
      });
      vars.magic_circuit_color_rgb = MagicCircuitColorHelper.resolveColor(vars);
      vars.syncPlayerVariables(player);
      MasterVisualStateSync.broadcast(player, vars);

      player.displayClientMessage(
         Component.translatable("message.typemoonworld.master_card.activated", Component.translatable("item.typemoonworld.master_card_" + profile.id())),
         true
      );
      player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);
      if (player.level() instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.5, 0.7, 0.5, 0.04);
         serverLevel.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 32, 0.6, 0.7, 0.6, 0.08);
      }
      NeoForge.EVENT_BUS.post(new MasterProfileEvent.Post(player, publicProfileId));
      return true;
   }

   public static boolean restoreOriginalState(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      String cardId = vars.master_card_id == null ? "" : vars.master_card_id;
      CompoundTag savedVariables = vars.master_card_saved_variables == null ? new CompoundTag() : vars.master_card_saved_variables.copy();
      scrubContractState(savedVariables);
      CompoundTag savedInventory = vars.master_card_saved_inventory == null ? new CompoundTag() : vars.master_card_saved_inventory.copy();
      boolean hasSnapshot = !savedVariables.isEmpty() && savedInventory.contains("items", 9);
      if (hasSnapshot) {
         clearPlayerInventory(player);
         vars.deserializeNBT(player.registryAccess(), savedVariables);
         vars.master_servant_uuid = "";
         vars.master_servant_contract_id = "";
         vars.servant_card_master_uuid = "";
         vars.servant_card_contract_id = "";
         vars.servant_card_contract_state = MasterServantLinkService.SERVANT_CONTRACT_NATIVE;
         MasterServantLinkService.clearContractTags(player);
         MasterServantLinkService.clearSurvival(vars);
         BodyTrainingService.applyAttributes(player, vars);
         restoreInventory(player, savedInventory);
      } else {
         vars.master_active = false;
         vars.master_card_active = false;
         vars.master_card_id = "";
         vars.master_card_saved_variables = new CompoundTag();
         vars.master_card_saved_inventory = new CompoundTag();
         MasterServantLinkService.clearContractTags(player);
      }
      PassiveService.resumeEffects(player);
      TalentService.resumeActiveEffects(player);
      give(player, createCardStack(cardId));
      clearMasterCardTags(player);
      return true;
   }

   public static void ensureTags(ServerPlayer player, String masterId) {
      if (player == null || masterId == null || masterId.isBlank()) return;
      String type = "tmw_master_card_" + masterId.trim().toLowerCase(java.util.Locale.ROOT);
      if (!player.getTags().contains("tmw_master_card") || !player.getTags().contains(type)) {
         applyMasterCardTags(player, masterId);
      }
   }

   public static void clearMasterCardTags(ServerPlayer player) {
      if (player == null) return;
      for (String tag : new java.util.ArrayList<>(player.getTags())) {
         if (tag.equals("tmw_master_card") || tag.startsWith("tmw_master_card_")) player.removeTag(tag);
      }
   }

   private static void applyMasterCardTags(ServerPlayer player, String masterId) {
      clearMasterCardTags(player);
      player.addTag("tmw_master_card");
      player.addTag("tmw_master_card_" + masterId.trim().toLowerCase(java.util.Locale.ROOT));
   }

   private static Profile profile(String masterId) {
      ResourceLocation externalId = ResourceLocation.tryParse(masterId);
      MasterProfileApiRegistry.Entry external = MasterProfileApiRegistry.get(externalId);
      if (external != null) {
         var data = external.data();
         return new Profile(masterId, data.commandSpellStyle(), data.maximumMana(), data.regenerationAmount(),
            data.regenerationIntervalTicks(), Attributes.NONE, vars -> { }, player -> external.initializer().initialize(
               new net.xxxjk.typemoonworld.api.MasterProfileContext(
                  player, data,
                  TypeMoonWorldApi.addon(data.id().getNamespace()).magics().mana(player),
                  TypeMoonWorldApi.addon(data.id().getNamespace()).magics().knowledge(player),
                  TypeMoonWorldApi.master(player)
               )));
      }
      return switch (masterId) {
         case "tohsaka_rin" -> new Profile(masterId, "rin", 1000.0, 8.0, 4, Attributes.FIVE_ELEMENTS, vars -> {
            learnBajiquan(vars, 60.0);
            learnJewelSuite(vars, 85.0);
            learn(vars, "gander");
            vars.proficiency_gander = Math.max(vars.proficiency_gander, 80.0);
            learn(vars, "reinforcement");
            vars.proficiency_reinforcement = Math.max(vars.proficiency_reinforcement, 65.0);
            learn(vars, "gravity_magic");
            vars.proficiency_gravity_magic = Math.max(vars.proficiency_gravity_magic, 70.0);
         }, player -> {
            giveHighGemKit(player);
            for (int i = 0; i < 5; i++) {
               ItemStack stack = new ItemStack(ModItems.CARVED_RUBY_HIGH_FULL.get());
               GemEngravingService.setEngravedMagic(stack, "gravity_magic");
               GemEngravingService.setEngravedManaCost(stack, 80.0);
               give(player, stack);
            }
         });
         case "emiya_kiritsugu" -> new Profile(masterId, "kiritsugu", 280.0, 5.0, 12, Attributes.FIRE_EARTH, vars -> {
            learn(vars, "time_alter");
            vars.proficiency_time_alter = Math.max(vars.proficiency_time_alter, 70.0);
            learn(vars, "reinforcement");
            vars.proficiency_reinforcement = Math.max(vars.proficiency_reinforcement, 40.0);
         }, player -> {
            equipHotbarSlot(player, 0, new ItemStack(ModItems.THOMPSON_CONTENDER.get()));
            give(player, new ItemStack(ModItems.ORIGIN_BULLET.get(), KIRITSUGU_ORIGIN_BULLET_STOCK));
            give(player, new ItemStack(ModItems.BULLET.get(), 64));
            give(player, new ItemStack(ModItems.BULLET.get(), 64));
            give(player, new ItemStack(ModItems.BULLET.get(), 64));
         });
         case "emiya_shirou" -> new Profile(masterId, "shirou", 300.0, 2.0, 16, Attributes.SWORD, vars -> {
            learn(vars, "projection");
            vars.proficiency_projection = Math.max(vars.proficiency_projection, 75.0);
            learn(vars, "reinforcement");
            vars.proficiency_reinforcement = Math.max(vars.proficiency_reinforcement, 45.0);
            learn(vars, "structural_analysis");
            vars.proficiency_structural_analysis = Math.max(vars.proficiency_structural_analysis, 75.0);
            learn(vars, "unlimited_blade_works");
            vars.proficiency_unlimited_blade_works = Math.max(vars.proficiency_unlimited_blade_works, 50.0);
            vars.has_unlimited_blade_works = true;
            learn(vars, "ubw_sword_control");
            learn(vars, "sword_barrel_full_open");
            vars.proficiency_sword_barrel_full_open = Math.max(vars.proficiency_sword_barrel_full_open, 10.0);
            addAnalyzedItem(vars, new ItemStack(Items.IRON_SWORD));
         }, player -> {
         });
         case "kotomine_kirei" -> new Profile(masterId, "kirei", 300.0, 5.0, 10, Attributes.NONE, vars -> {
            learnBajiquan(vars, 80.0);
            learn(vars, "black_key_fire_engraving");
            vars.magic_proficiencies.put("black_key_fire_engraving", 80.0);
            learn(vars, "baptism_rite");
            vars.proficiency_baptism_rite = Math.max(vars.proficiency_baptism_rite, 85.0);
            learn(vars, "spiritual_healing");
            vars.proficiency_spiritual_healing = Math.max(vars.proficiency_spiritual_healing, 75.0);
            learn(vars, "healing_magic");
            vars.proficiency_healing_magic = Math.max(vars.proficiency_healing_magic, 50.0);
         }, player -> {
            for (int i = 0; i < 5; i++) {
               ItemStack blackKeys = new ItemStack(ModItems.BLACK_KEY.get(), 3);
               BlackKeyItem.setExpanded(blackKeys, true);
               give(player, blackKeys);
            }
         });
         case "luvia" -> new Profile(masterId, "luvia", 1000.0, 9.0, 6, Attributes.EARTH, vars -> {
            learnJewelSuite(vars, 85.0);
            learn(vars, "jewel_machine_gun");
            vars.proficiency_jewel_magic_release = Math.max(vars.proficiency_jewel_magic_release, 80.0);
            learn(vars, "gander");
            vars.proficiency_gander = Math.max(vars.proficiency_gander, 60.0);
            learn(vars, "reinforcement");
            vars.proficiency_reinforcement = Math.max(vars.proficiency_reinforcement, 65.0);
         }, MasterCardProfile::giveHighGemKit);
         case "elsa_saijo" -> new Profile(masterId, "elsa_saijo", 350.0, 6.0, 11, Attributes.WIND, vars -> {
            learn(vars, "fire_magic");
            vars.proficiency_fire_magic = Math.max(vars.proficiency_fire_magic, 65.0);
            learn(vars, "wind_magic");
            vars.proficiency_wind_magic = Math.max(vars.proficiency_wind_magic, 60.0);
            learn(vars, "water_magic");
            vars.proficiency_water_magic = Math.max(vars.proficiency_water_magic, 55.0);
         }, player -> {
         });
         case "waver" -> new Profile(masterId, "waver", 120.0, 3.0, 18, Attributes.EARTH, vars -> {
            learn(vars, "reinforcement");
            vars.proficiency_reinforcement = Math.max(vars.proficiency_reinforcement, 35.0);
            learn(vars, "structural_analysis");
            vars.proficiency_structural_analysis = Math.max(vars.proficiency_structural_analysis, 55.0);
            learn(vars, "suggestion_magic");
            vars.proficiency_suggestion_magic = Math.max(vars.proficiency_suggestion_magic, 25.0);
         }, player -> {
         });
         case "tohsaka_tokiomi" -> new Profile(masterId, "tokiomi", 750.0, 7.0, 8, Attributes.FIRE, vars -> {
            learnJewelSuite(vars, 85.0);
            learn(vars, "fire_magic");
            vars.proficiency_fire_magic = Math.max(vars.proficiency_fire_magic, 75.0);
         }, player -> {
            giveHighGemKit(player);
            give(player, new ItemStack(ModItems.RUBY_STAFF.get()));
         });
         default -> null;
      };
   }

   private static void saveOriginalStateAndClearPlayer(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, String masterId) {
      if (vars.master_active && !vars.master_servant_uuid.isBlank()) {
         MasterServantLinkService.onMasterLost(player, vars);
      }
      vars.master_card_saved_variables = vars.serializeNBT(player.registryAccess());
      scrubContractState(vars.master_card_saved_variables);
      vars.master_card_saved_inventory = saveInventory(player);
      vars.master_card_active = true;
      vars.master_card_id = masterId == null ? "" : masterId;
      applyMasterCardTags(player, vars.master_card_id);
      vars.master_servant_uuid = "";
      vars.master_servant_contract_id = "";
      vars.servant_card_master_uuid = "";
      vars.servant_card_contract_id = "";
      vars.servant_card_contract_state = MasterServantLinkService.SERVANT_CONTRACT_NATIVE;
      MasterServantLinkService.clearSurvival(vars);
      clearPlayerInventory(player);
   }

   private static void scrubContractState(CompoundTag tag) {
      tag.remove("master_servant_uuid");
      tag.remove("servant_card_master_uuid");
      tag.remove("master_servant_contract_id");
      tag.remove("servant_card_contract_id");
      tag.remove("servant_card_contract_state");
      tag.remove("master_servant_link_partner_uuid");
      tag.remove("master_servant_link_partner_hp");
      tag.remove("master_servant_link_partner_max_hp");
      tag.remove("master_servant_link_partner_mana");
      tag.remove("master_servant_link_partner_max_mana");
      tag.remove("master_servant_link_state");
      tag.remove("master_servant_link_decay");
      tag.remove("master_servant_link_drawing_mana");
      tag.remove("master_servant_independent_ticks");
      tag.remove("master_servant_survival_state");
      tag.remove("master_servant_survival_ticks");
      tag.remove("master_servant_master_position_valid");
      tag.remove("master_servant_master_position_online");
      tag.remove("master_servant_master_dimension");
      tag.remove("master_servant_master_x");
      tag.remove("master_servant_master_y");
      tag.remove("master_servant_master_z");
   }

   private static void resetToProfileState(TypeMoonWorldModVariables.PlayerVariables vars) {
      vars.learned_magics.clear();
      vars.magic_proficiencies.clear();
      vars.selected_magics.clear();
      vars.selected_magic_runtime_slot_indices.clear();
      vars.selected_magic_display_names.clear();
      vars.current_magic_index = 0;
      vars.active_wheel_index = 0;
      vars.clearAllWheelSlots();
      vars.crest_entries.clear();
      vars.crest_practice_count.clear();
      vars.mysticEyesInventory.setStackInSlot(0, ItemStack.EMPTY);
      vars.magicCrestInventory.setStackInSlot(0, ItemStack.EMPTY);
      vars.analyzed_items.clear();
      vars.projection_selected_item = ItemStack.EMPTY;
      vars.analyzed_structures.clear();
      vars.projection_selected_structure_id = "";
      vars.has_unlimited_blade_works = false;
      vars.proficiency_structural_analysis = 0.0;
      vars.proficiency_projection = 0.0;
      vars.proficiency_reinforcement = 0.0;
      vars.proficiency_jewel_magic_shoot = 0.0;
      vars.proficiency_jewel_magic_release = 0.0;
      vars.proficiency_unlimited_blade_works = 0.0;
      vars.proficiency_sword_barrel_full_open = 0.0;
      vars.proficiency_gravity_magic = 0.0;
      vars.proficiency_gander = 0.0;
      vars.proficiency_healing_magic = 0.0;
      vars.proficiency_magic_bullet = 0.0;
      vars.proficiency_suggestion_magic = 0.0;
      vars.proficiency_binding_magic = 0.0;
      vars.proficiency_fire_magic = 0.0;
      vars.proficiency_water_magic = 0.0;
      vars.proficiency_wind_magic = 0.0;
      vars.proficiency_earth_magic = 0.0;
      vars.proficiency_time_alter = 0.0;
      vars.proficiency_spiritual_healing = 0.0;
      vars.proficiency_baptism_rite = 0.0;
      vars.bajiquan_learned = false;
      vars.bajiquan_proficiency = 0.0;
      vars.bajiquan_tiger_unlocked = false;
      vars.bajiquan_circle_realm_cooldown_until = 0L;
      vars.ganryu_learned = false;
      vars.ganryu_proficiency = 0.0;
      vars.ganryu_tsubame_unlocked = false;
      vars.martial_ukemi_learned = false;
      vars.sword_barrel_mode = 0;
      vars.gandr_machine_gun_mode = 0;
      vars.jewel_magic_mode = 0;
      vars.gravity_magic_target = 0;
      vars.gravity_magic_mode = 0;
      vars.healing_magic_target = 0;
      vars.fire_magic_mode = 0;
      vars.water_magic_mode = 0;
      vars.wind_magic_mode = 0;
      vars.earth_magic_mode = 0;
      vars.time_alter_mode = 0;
      vars.reinforcement_mode = 0;
      vars.reinforcement_target = 0;
      vars.reinforcement_level = 1;
   }

   private static CompoundTag saveInventory(ServerPlayer player) {
      CompoundTag tag = new CompoundTag();
      ListTag items = new ListTag();
      player.getInventory().save(items);
      tag.put("items", items);
      return tag;
   }

   private static void restoreInventory(ServerPlayer player, CompoundTag tag) {
      if (tag != null && tag.contains("items", 9)) {
         player.getInventory().load(tag.getList("items", 10));
      }
      player.getInventory().setChanged();
   }

   private static void clearPlayerInventory(ServerPlayer player) {
      player.containerMenu.setCarried(ItemStack.EMPTY);
      player.getInventory().clearContent();
      player.getInventory().setChanged();
   }

   private static void applyAttributes(TypeMoonWorldModVariables.PlayerVariables vars, Attributes attributes) {
      vars.player_magic_attributes_earth = false;
      vars.player_magic_attributes_water = false;
      vars.player_magic_attributes_fire = false;
      vars.player_magic_attributes_wind = false;
      vars.player_magic_attributes_ether = false;
      vars.player_magic_attributes_none = false;
      vars.player_magic_attributes_imaginary_number = false;
      vars.player_magic_attributes_sword = false;
      switch (attributes) {
         case FIVE_ELEMENTS -> {
            vars.player_magic_attributes_earth = true;
            vars.player_magic_attributes_water = true;
            vars.player_magic_attributes_fire = true;
            vars.player_magic_attributes_wind = true;
            vars.player_magic_attributes_ether = true;
         }
         case FIRE_EARTH -> {
            vars.player_magic_attributes_fire = true;
            vars.player_magic_attributes_earth = true;
         }
         case SWORD -> vars.player_magic_attributes_sword = true;
         case EARTH -> vars.player_magic_attributes_earth = true;
         case WIND -> vars.player_magic_attributes_wind = true;
         case FIRE -> vars.player_magic_attributes_fire = true;
      }
   }

   private static void learnJewelSuite(TypeMoonWorldModVariables.PlayerVariables vars, double proficiency) {
      learn(vars, "jewel_magic_shoot");
      learn(vars, "jewel_random_shoot");
      learn(vars, "jewel_magic_release");
      vars.proficiency_jewel_magic_shoot = Math.max(vars.proficiency_jewel_magic_shoot, proficiency);
      vars.proficiency_jewel_magic_release = Math.max(vars.proficiency_jewel_magic_release, proficiency);
   }

   private static void learn(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      if (!vars.learned_magics.contains(magicId)) {
         vars.learned_magics.add(magicId);
      }
   }

   private static void grantMasterDetection(TypeMoonWorldModVariables.PlayerVariables vars) {
      learn(vars, "detection");
      MagicProficiencyService.set(vars, "detection", Math.max(MagicProficiencyService.get(vars, "detection"), 90.0));
   }

   private static void learnBajiquan(TypeMoonWorldModVariables.PlayerVariables vars, double proficiency) {
      vars.bajiquan_learned = true;
      vars.bajiquan_proficiency = Math.max(vars.bajiquan_proficiency, proficiency);
      learn(vars, "bajiquan");
   }

   private static void addAnalyzedItem(TypeMoonWorldModVariables.PlayerVariables vars, ItemStack stack) {
      for (ItemStack existing : vars.analyzed_items) {
         if (ItemStack.isSameItemSameComponents(existing, stack)) {
            return;
         }
      }
      vars.analyzed_items.add(stack.copy());
      vars.projection_selected_item = stack.copy();
   }

   private static void giveHighGemKit(ServerPlayer player) {
      for (DeferredItem<Item> fullGem : HIGH_FULL_GEMS) {
         give(player, new ItemStack(fullGem.get(), 10));
      }
      for (DeferredItem<Item> emptyGem : HIGH_GEMS) {
         give(player, new ItemStack(emptyGem.get(), 64));
      }
   }

   private static void give(ServerPlayer player, ItemStack stack) {
      if (stack.isEmpty()) {
         return;
      }
      if (!player.getInventory().add(stack)) {
         player.drop(stack, false);
      }
   }

   private static void equipHotbarSlot(ServerPlayer player, int slot, ItemStack stack) {
      int clampedSlot = Math.max(0, Math.min(8, slot));
      ItemStack existing = player.getInventory().getItem(clampedSlot);
      if (!existing.isEmpty()) {
         give(player, existing.copy());
      }
      player.getInventory().setItem(clampedSlot, stack.copy());
      player.getInventory().selected = clampedSlot;
      player.getInventory().setChanged();
   }

   private static ItemStack createCardStack(String masterId) {
      if (MasterProfileApiRegistry.get(ResourceLocation.tryParse(masterId)) != null) {
         return net.xxxjk.TYPE_MOON_WORLD.item.custom.MasterCardItem.create(ModItems.MASTER_CARD_GENERIC.get(), masterId);
      }
      Item item = switch (masterId == null ? "" : masterId) {
         case "tohsaka_rin" -> ModItems.MASTER_CARD_TOHSAKA_RIN.get();
         case "emiya_kiritsugu" -> ModItems.MASTER_CARD_EMIYA_KIRITSUGU.get();
         case "emiya_shirou" -> ModItems.MASTER_CARD_EMIYA_SHIROU.get();
         case "kotomine_kirei" -> ModItems.MASTER_CARD_KOTOMINE_KIREI.get();
         case "luvia" -> ModItems.MASTER_CARD_LUVIA.get();
         case "elsa_saijo" -> ModItems.MASTER_CARD_ELSA_SAIJO.get();
         case "waver" -> ModItems.MASTER_CARD_WAVER.get();
         case "tohsaka_tokiomi" -> ModItems.MASTER_CARD_TOHSAKA_TOKIOMI.get();
         default -> null;
      };
      return item == null ? ItemStack.EMPTY : new ItemStack(item);
   }

   private enum Attributes {
      FIVE_ELEMENTS,
      FIRE_EARTH,
      SWORD,
      EARTH,
      WIND,
      FIRE,
      NONE
   }

   private record Profile(
      String id,
      String commandSpellStyle,
      double maxMana,
      double regenAmount,
      int regenIntervalTicks,
      Attributes attributes,
      ProfileMagicApplier magicApplier,
      ProfileItemGiver itemGiver
   ) {
      void applyMagic(TypeMoonWorldModVariables.PlayerVariables vars) {
         this.magicApplier.apply(vars);
      }

      void giveItems(ServerPlayer player) {
         this.itemGiver.give(player);
      }
   }

   @FunctionalInterface
   private interface ProfileMagicApplier {
      void apply(TypeMoonWorldModVariables.PlayerVariables vars);
   }

   @FunctionalInterface
   private interface ProfileItemGiver {
      void give(ServerPlayer player);
   }
}
