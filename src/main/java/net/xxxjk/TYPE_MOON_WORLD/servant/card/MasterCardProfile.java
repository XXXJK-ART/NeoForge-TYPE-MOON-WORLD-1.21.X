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
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicCircuitColorHelper;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.GemEngravingService;
import net.xxxjk.TYPE_MOON_WORLD.martial.BodyTrainingService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

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
      BodyTrainingService.clear(player, vars);
      resetToProfileState(vars);
      // MasterStateManager synchronizes immediately; apply the target attributes first so
      // a previous sword attribute cannot auto-awaken Unlimited Blade Works during a switch.
      applyAttributes(vars, profile.attributes());
      if (!MasterStateManager.activateProfile(player, profile.commandSpellStyle())) {
         return false;
      }

      vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.is_magus = true;
      vars.player_max_mana = profile.maxMana();
      vars.player_mana = profile.maxMana();
      vars.player_mana_egenerated_every_moment = profile.regenAmount();
      vars.player_restore_magic_moment = profile.regenIntervalTicks();
      profile.applyMagic(vars);
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
      return true;
   }

   public static boolean restoreOriginalState(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      String cardId = vars.master_card_id == null ? "" : vars.master_card_id;
      CompoundTag savedVariables = vars.master_card_saved_variables == null ? new CompoundTag() : vars.master_card_saved_variables.copy();
      CompoundTag savedInventory = vars.master_card_saved_inventory == null ? new CompoundTag() : vars.master_card_saved_inventory.copy();
      clearPlayerInventory(player);
      if (!savedVariables.isEmpty()) {
         vars.deserializeNBT(player.registryAccess(), savedVariables);
         BodyTrainingService.clear(player, vars);
      } else {
         vars.master_active = false;
         vars.master_card_active = false;
         vars.master_card_id = "";
         vars.master_card_saved_variables = new CompoundTag();
         vars.master_card_saved_inventory = new CompoundTag();
      }
      restoreInventory(player, savedInventory);
      give(player, createCardStack(cardId));
      return true;
   }

   private static Profile profile(String masterId) {
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
            learn(vars, "sword_barrel_full_open");
            vars.proficiency_sword_barrel_full_open = Math.max(vars.proficiency_sword_barrel_full_open, 10.0);
            addAnalyzedItem(vars, new ItemStack(Items.IRON_SWORD));
         }, player -> {
         });
         case "kotomine_kirei" -> new Profile(masterId, "kirei", 300.0, 5.0, 10, Attributes.NONE, vars -> {
            learnBajiquan(vars, 80.0);
            learn(vars, "baptism_rite");
            vars.proficiency_baptism_rite = Math.max(vars.proficiency_baptism_rite, 85.0);
            learn(vars, "spiritual_healing");
            vars.proficiency_spiritual_healing = Math.max(vars.proficiency_spiritual_healing, 75.0);
            learn(vars, "healing_magic");
            vars.proficiency_healing_magic = Math.max(vars.proficiency_healing_magic, 50.0);
         }, player -> player.displayClientMessage(Component.translatable("message.typemoonworld.master_card.black_key_placeholder"), false));
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
            give(player, new ItemStack(ModItems.MERCURY_SWORD.get()));
         });
         default -> null;
      };
   }

   private static void saveOriginalStateAndClearPlayer(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, String masterId) {
      vars.master_card_saved_variables = vars.serializeNBT(player.registryAccess());
      vars.master_card_saved_inventory = saveInventory(player);
      vars.master_card_active = true;
      vars.master_card_id = masterId == null ? "" : masterId;
      clearPlayerInventory(player);
   }

   private static void resetToProfileState(TypeMoonWorldModVariables.PlayerVariables vars) {
      vars.learned_magics.clear();
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
