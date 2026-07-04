package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.UUID;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class ServantCardTransformManager {
   public static final String DEATH_RULE_KEY = "fate_card_death_release";
   private static final ResourceLocation MAX_HEALTH_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_max_health");
   private static final ResourceLocation ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_attack");
   private static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_speed");
   private static final ResourceLocation ARMOR_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_armor");

   private ServantCardTransformManager() {
   }

   public static boolean transform(ServerPlayer player, String servantId) {
      ServantDefinition definition = ServantDataRegistry.get(servantId);
      if (definition == null) {
         player.displayClientMessage(Component.literal("Unknown servant card: " + servantId), true);
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.servant_card_transformed) {
         release(player, false);
      }
      saveArmor(player, vars);
      vars.servant_card_transformed = true;
      vars.servant_card_id = servantId;
      vars.servant_card_master_uuid = "";
      vars.servant_card_max_mana = ServantCardManaService.maxManaFor(servantId);
      vars.servant_card_mana = vars.servant_card_max_mana;
      vars.servant_card_mana_regen = ServantCardManaService.regenPerSecondFor(servantId);
      vars.servant_card_jump_charges = 4;
      vars.servant_card_jump_recovery_ticks = 0;
      vars.servant_card_np_cooldown = 0;
      vars.servant_card_skill_cooldowns = "";
      vars.servant_card_transform_cooldown = 40;
      vars.servant_card_release_cooldown = 40;
      vars.servant_card_action_mode = 0;
      vars.servant_card_flying = false;
      vars.servant_card_flight_forward = 0.0;
      vars.servant_card_flight_strafe = 0.0;
      vars.servant_card_flight_vertical = 0.0;
      vars.servant_card_was_magus = vars.is_magus;
      vars.servant_card_was_magic_circuit_open = vars.is_magic_circuit_open;
      vars.is_magic_circuit_open = false;
      equipArmor(player, servantId);
      ServantCardLoadoutManager.saveAndEquip(player, vars, servantId);
      applyAttributes(player, definition.parameters());
      vars.syncPlayerVariables(player);
      player.displayClientMessage(Component.literal("Servant Card: " + definition.displayName()), true);
      return true;
   }

   public static boolean release(ServerPlayer player, boolean keepOneHp) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed) {
         return false;
      }
      removeAttributes(player);
      ServantCardFlightController.stop(player, vars, false);
      restoreArmor(player, vars);
      ServantCardLoadoutManager.restore(player, vars);
      vars.servant_card_transformed = false;
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
      player.displayClientMessage(Component.literal("Servant Card released"), true);
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
         return;
      }
      if (vars.servant_card_np_cooldown > 0) {
         vars.servant_card_np_cooldown--;
      }
      tickSkillCooldowns(vars);
      tickJumpRecovery(vars);
      ServantCardManaService.tick(player, vars);
      ServantCardFlightController.tick(player, vars);
   }

   public static boolean triggerAction(ServerPlayer player, int slot) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || slot < 0 || slot > 9) {
         return false;
      }
      SkillAction action = actionFor(vars.servant_card_id, slot, player.isCrouching());
      if (action == null) {
         player.displayClientMessage(Component.literal("Empty servant skill slot"), true);
         return false;
      }
      boolean np = slot == 9;
      int currentCooldown = np ? vars.servant_card_np_cooldown : getSkillCooldown(vars, slot);
      if (currentCooldown > 0) {
         player.displayClientMessage(Component.literal("Cooldown: " + (currentCooldown / 20.0F) + "s"), true);
         return false;
      }
      if (!ServantCardManaService.consume(player, vars, action.mpCost())) {
         player.displayClientMessage(Component.literal("Not enough MP"), true);
         return false;
      }
      if (!performAction(player, vars, action)) {
         return false;
      }
      if (np) {
         vars.servant_card_np_cooldown = action.cooldownTicks();
      } else {
         setSkillCooldown(vars, slot, action.cooldownTicks());
      }
      vars.syncPlayerVariables(player);
      player.displayClientMessage(Component.literal(action.displayName() + " activated"), true);
      return true;
   }

   public static boolean bindMaster(ServerPlayer servant, ServerPlayer master) {
      if (servant == master) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed) {
         return false;
      }
      vars.servant_card_master_uuid = master.getUUID().toString();
      vars.syncPlayerVariables(servant);
      servant.displayClientMessage(Component.literal("Master bound: " + master.getGameProfile().getName()), true);
      master.displayClientMessage(Component.literal("Servant bound: " + servant.getGameProfile().getName()), true);
      return true;
   }

   public static boolean bigJump(ServerPlayer player, boolean backward) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || vars.servant_card_jump_charges <= 0) {
         return false;
      }
      ServantDefinition definition = ServantDataRegistry.get(vars.servant_card_id);
      double scale = definition == null ? 1.0 : Mth.clamp(definition.parameters().movementSpeed() / 0.28, 0.8, 1.8);
      double yaw = Math.toRadians(player.getYRot());
      double dir = backward ? -1.0 : 1.0;
      double x = -Math.sin(yaw) * dir * 1.35 * scale;
      double z = Math.cos(yaw) * dir * 1.35 * scale;
      player.setDeltaMovement(player.getDeltaMovement().add(x, 0.58 * scale, z));
      player.hurtMarked = true;
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
      player.setHealth((float)Math.min(params.maxHealth(), Math.max(1.0, params.maxHealth())));
   }

   private static void removeAttributes(ServerPlayer player) {
      remove(player.getAttribute(Attributes.MAX_HEALTH), MAX_HEALTH_ID);
      remove(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_ID);
      remove(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID);
      remove(player.getAttribute(Attributes.ARMOR), ARMOR_ID);
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
      player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.getServantCardArmor(servantId, EquipmentSlot.CHEST)));
      player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.getServantCardArmor(servantId, EquipmentSlot.LEGS)));
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
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < 9; i++) {
         if (i > 0) {
            builder.append(',');
         }
         builder.append(i < cooldowns.length ? Math.max(0, cooldowns[i]) : 0);
      }
      return builder.toString();
   }

   private static SkillAction actionFor(String servantId, int slot, boolean crouching) {
      return switch (servantId == null ? "" : servantId) {
         case "artoria_pendragon" -> switch (slot) {
            case 0 -> new SkillAction("Mana Burst", "mana_burst", 35.0, 220);
            case 1 -> new SkillAction("Invisible Air Slash", "wind_slash", 26.0, 120);
            case 2 -> new SkillAction("Charisma", "charisma", 30.0, 360);
            case 3 -> new SkillAction("Lion Leap", "leap_combo", 24.0, 100);
            case 4 -> new SkillAction("Air Cleave", "air_cleave", 26.0, 140);
            default -> null;
         };
         case "cu_chulainn" -> switch (slot) {
            case 0 -> new SkillAction("Rune Cast", "rune_cast", 22.0, 120);
            case 1 -> new SkillAction("Spear Vault", "spear_vault", 24.0, 120);
            case 2 -> new SkillAction("Recast Stance", "recast", 24.0, 260);
            case 3 -> new SkillAction("Algiz", "algiz", 30.0, 300);
            case 4 -> new SkillAction("Berkana", "berkana", 34.0, 400);
            default -> null;
         };
         case "heracles" -> switch (slot) {
            case 0 -> new SkillAction("Roar", "roar", 18.0, 160);
            case 1 -> new SkillAction("Jump Attack", "jump_attack", 28.0, 140);
            case 2 -> new SkillAction("Charge", "charge", 26.0, 130);
            case 3 -> new SkillAction("Earth Rend", "earth_rend", 34.0, 260);
            case 4 -> new SkillAction("Slam", "slam", 30.0, 180);
            default -> null;
         };
         case "medea" -> switch (slot) {
            case 0 -> new SkillAction("Workshop", "workshop", 30.0, 600);
            case 1 -> new SkillAction("Craft Mystic Item", "craft_item", 22.0, 160);
            case 2 -> new SkillAction("Dragonfang Soldier", "summon_dragonfang", 42.0, 420);
            case 3 -> new SkillAction("Blink Volley", "blink_volley", 26.0, 120);
            case 4 -> new SkillAction("Barrier Nova", "barrier", 30.0, 240);
            case 5 -> new SkillAction("Hecate Bind", "bind", 32.0, 220);
            case 6 -> new SkillAction("Aerial Escape", "escape", 28.0, 180);
            case 7 -> new SkillAction("Thunderstorm", "thunder", 42.0, 320);
            default -> null;
         };
         case "medusa" -> switch (slot) {
            case 0 -> new SkillAction("Chain Snare", "snare", 22.0, 140);
            case 1 -> new SkillAction("Viper Rush", "viper_rush", 24.0, 120);
            case 2 -> new SkillAction("Serpent Step", "serpent_step", 20.0, 100);
            case 3 -> new SkillAction("Cybele", "cybele", 36.0, 420);
            case 4 -> new SkillAction("Monster Strength", "monster_strength", 28.0, 320);
            case 5 -> new SkillAction("Charm", "charm", 26.0, 220);
            case 6 -> new SkillAction("Bloodfort Field", "bloodfort_field", 38.0, 600);
            case 9 -> new SkillAction(crouching ? "Bloodfort NP" : "Bellerophon", crouching ? "bloodfort_np" : "bellerophon", 90.0, 1600);
            default -> null;
         };
         case "cursed_arm_hassan" -> switch (slot) {
            case 0 -> new SkillAction("Presence Concealment", "stealth", 18.0, 220);
            case 1 -> new SkillAction("Dirk Throw", "dirk_throw", 18.0, 80);
            case 2 -> new SkillAction("Shadow Step", "shadow_step", 24.0, 160);
            case 3 -> new SkillAction("Shadow Lunge", "shadow_lunge", 24.0, 130);
            case 4 -> new SkillAction("Self Modification", "self_mod", 28.0, 360);
            case 9 -> new SkillAction("Zabaniya", "zabaniya", 80.0, 1400);
            default -> null;
         };
         case "emiya_archer" -> switch (slot) {
            case 0 -> new SkillAction("Project Kanshou and Bakuya", "emiya_kb", 10.0, 20);
            case 1 -> new SkillAction("Project Overedge", "emiya_overedge", 22.0, 80);
            case 2 -> new SkillAction("Rho Aias", "rho_aias", 35.0, 300);
            case 3 -> new SkillAction("Pseudo Spiral Sword", "emiya_spiral", 22.0, 80);
            case 4 -> new SkillAction("Crimson Hound", "emiya_hound", 20.0, 80);
            case 5 -> new SkillAction("Borrowed NP", "emiya_borrowed", 46.0, 240);
            case 6 -> new SkillAction("Twin Flurry", "twin_flurry", 24.0, 120);
            case 7 -> new SkillAction("Broken Phantasm", "emiya_bp", 55.0, 520);
            case 8 -> new SkillAction("Projection Cycle", "emiya_cycle", 8.0, 20);
            case 9 -> new SkillAction("Unlimited Blade Works", "ubw", 110.0, 2400);
            default -> null;
         };
         case "sasaki_kojiro" -> switch (slot) {
            case 0 -> new SkillAction("Presence Concealment", "stealth", 18.0, 220);
            case 1 -> new SkillAction("Afterimage Slash", "afterimage", 24.0, 120);
            case 2 -> new SkillAction("Teleport Behind", "shadow_step", 30.0, 180);
            case 3 -> new SkillAction("Mind's Eye", "mind_eye", 24.0, 320);
            case 4 -> new SkillAction("Sweep Burst", "sweep", 24.0, 140);
            default -> null;
         };
         case "oda_nobunaga" -> switch (slot) {
            case 0 -> new SkillAction("Floating Matchlock", "matchlock", 18.0, 80);
            case 1 -> new SkillAction("Matchlock Volley", "volley", 26.0, 140);
            case 2 -> new SkillAction("Three Line Rotation", "fire_barrage", 32.0, 220);
            case 3 -> new SkillAction("Atsumori Step", "atsumori", 22.0, 100);
            case 4 -> new SkillAction("Anti-Mystery Spark", "anti_mystery", 32.0, 260);
            case 5 -> new SkillAction("Demon King Pressure", "maou", 42.0, 500);
            case 6 -> new SkillAction("Strategy", "strategy", 24.0, 360);
            case 7 -> new SkillAction("Ash Field", "ash_field", 38.0, 420);
            case 8 -> new SkillAction("Hasebe Repel", "hasebe_repel", 24.0, 160);
            case 9 -> new SkillAction(crouching ? "Hajun" : "Three Thousand Worlds", crouching ? "hajun" : "three_thousand", crouching ? 130.0 : 95.0, crouching ? 2800 : 1800);
            default -> null;
         };
         case "enkidu" -> switch (slot) {
            case 0 -> new SkillAction("Transfiguration", "transfiguration", 28.0, 320);
            case 1 -> new SkillAction("Presence Detection", "detection", 18.0, 160);
            case 2 -> new SkillAction("Perfect Form", "perfect_form", 38.0, 520);
            case 3 -> new SkillAction("Chains of Heaven", "chains", 32.0, 220);
            case 4 -> new SkillAction("Age of Babylon", "age_babylon", 34.0, 180);
            case 5 -> new SkillAction("Earth Wedge", "earth_wedge", 28.0, 160);
            case 6 -> new SkillAction("Clay Bulwark", "bulwark", 30.0, 320);
            case 7 -> new SkillAction("Stardust Step", "stardust", 20.0, 100);
            case 8 -> new SkillAction("Mega Age of Babylon", "mega_age", 50.0, 420);
            case 9 -> new SkillAction("Enuma Elish", "enuma_elish", 120.0, 2400);
            default -> null;
         };
         case "gawain" -> switch (slot) {
            case 0 -> new SkillAction("Belt of Berhillak", "belt", 28.0, 420);
            case 1 -> new SkillAction("Noon Guard", "noon_guard", 26.0, 320);
            case 2 -> new SkillAction("Gallatin Spark", "gallatin_spark", 28.0, 180);
            case 3 -> new SkillAction("Solar Rebuke", "solar_rebuke", 32.0, 220);
            case 4 -> new SkillAction("Radiant Field", "radiant_field", 36.0, 520);
            case 5 -> new SkillAction("Solar Combo", "solar_combo", 26.0, 130);
            default -> null;
         };
         case "li_shuwen" -> switch (slot) {
            case 0 -> new SkillAction("Circle Realm", "circle_realm", 22.0, 260);
            case 1 -> new SkillAction("Yin Yang Crossing", "yin_yang", 28.0, 160);
            case 2 -> new SkillAction("Shoulder Charge", "shoulder_charge", 26.0, 140);
            case 3 -> new SkillAction("Tremor Interrupt", "interrupt", 24.0, 160);
            case 4 -> new SkillAction("Counter", "counter", 26.0, 240);
            case 5 -> new SkillAction("Pursuit", "pursuit", 22.0, 120);
            case 9 -> new SkillAction("Wu Er Da", "wu_er_da", 95.0, 1600);
            default -> null;
         };
         case "paracelsus" -> switch (slot) {
            case 0 -> new SkillAction("Element Cycle", "element_cycle", 8.0, 20);
            case 1 -> new SkillAction("High Speed Chanting", "chant", 22.0, 280);
            case 2 -> new SkillAction("Elemental Spirit", "elemental_spirit", 30.0, 220);
            case 3 -> new SkillAction("Philosopher Stone", "stone", 36.0, 420);
            case 4 -> new SkillAction("Fire Magic", "fire", 24.0, 120);
            case 5 -> new SkillAction("Water Magic", "water", 24.0, 120);
            case 6 -> new SkillAction("Earth Magic", "earth", 24.0, 120);
            case 7 -> new SkillAction("Wind Magic", "wind", 24.0, 120);
            case 8 -> new SkillAction("Mixed Element Burst", "mixed_element", 42.0, 320);
            default -> null;
         };
         default -> null;
      };
   }

   private static boolean performAction(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, SkillAction action) {
      String id = action.effectId();
      switch (id) {
         case "mana_burst" -> {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 2, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1, false, true, true));
            player.getPersistentData().putInt("ServantCardManaBurstUntil", player.tickCount + 160);
         }
         case "charisma", "strategy" -> buffNearby(player, 10.0, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 220, 0, false, true, true));
         case "stealth", "circle_realm" -> {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, "circle_realm".equals(id) ? 140 : 220, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1, false, true, true));
            player.getPersistentData().putInt("ServantCardConcealmentUntil", player.tickCount + ("circle_realm".equals(id) ? 140 : 220));
         }
         case "berkana", "perfect_form", "stone" -> player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1, false, true, true));
         case "algiz", "barrier", "rho_aias", "bulwark", "noon_guard", "belt" -> player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 180, 1, false, true, true));
         case "monster_strength", "self_mod", "transfiguration", "maou", "chant" -> player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1, false, true, true));
         case "emiya_kb" -> equipPair(player, ModItems.GAN_JIANG.get(), ModItems.MO_YE.get());
         case "emiya_overedge" -> equipPair(player, ModItems.GAN_JIANG_OVEREDGE.get(), ModItems.MO_YE_OVEREDGE.get());
         case "emiya_spiral" -> player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.PSEUDO_SPIRAL_SWORD.get()));
         case "emiya_hound" -> player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.CRIMSON_HOUND.get()));
         case "emiya_borrowed" -> player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.EXCALIBUR_GALLATIN.get()));
         case "emiya_bp" -> {
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.PSEUDO_SPIRAL_SWORD.get()));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0, false, true, true));
         }
         case "emiya_cycle", "element_cycle" -> {
            vars.servant_card_action_mode = (vars.servant_card_action_mode + 1) % 4;
            cycleLoadout(player, vars);
         }
         case "ubw" -> {
            vars.has_unlimited_blade_works = true;
            vars.is_chanting_ubw = true;
            vars.ubw_chant_progress = 0;
            vars.ubw_chant_timer = 0;
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0, false, true, true));
         }
         default -> performBurstLikeAction(player, id);
      }
      return true;
   }

   private static void performBurstLikeAction(ServerPlayer player, String id) {
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      if (isDashAction(id)) {
         player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 1.35, 0.18, dir.z * 1.35));
         player.hurtMarked = true;
      }
      float damage = switch (id) {
         case "zabaniya", "wu_er_da" -> 90.0F;
         case "bellerophon", "bloodfort_np", "three_thousand", "hajun", "enuma_elish" -> 80.0F;
         case "earth_rend", "slam", "mega_age", "mixed_element", "thunder" -> 32.0F;
         default -> 18.0F;
      };
      double range = id.endsWith("_np") || "bellerophon".equals(id) || "three_thousand".equals(id) || "enuma_elish".equals(id) ? 12.0 : 5.0;
      hitForwardArc(player, dir, range, damage);
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, player.getX() + dir.x * 1.5, player.getY() + 1.0, player.getZ() + dir.z * 1.5, 6, 0.5, 0.3, 0.5, 0.0);
         level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F);
      }
   }

   private static boolean isDashAction(String id) {
      return id.contains("step") || id.contains("rush") || id.contains("leap") || id.contains("charge") || id.contains("pursuit") || id.contains("vault");
   }

   private static void hitForwardArc(ServerPlayer player, Vec3 dir, double range, float damage) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 origin = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
      AABB box = player.getBoundingBox().inflate(range, 3.0, range);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(origin);
         if (to.lengthSqr() > range * range || to.normalize().dot(dir) < 0.45) {
            continue;
         }
         target.invulnerableTime = 0;
         target.hurt(player.damageSources().playerAttack(player), damage);
         target.invulnerableTime = 0;
         target.push(dir.x * 0.75, 0.18, dir.z * 0.75);
         target.hurtMarked = true;
      }
   }

   private static void buffNearby(ServerPlayer player, double radius, MobEffectInstance effect) {
      player.addEffect(new MobEffectInstance(effect));
      if (player.level() instanceof ServerLevel level) {
         for (ServerPlayer other : level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(radius), p -> p != player)) {
            other.addEffect(new MobEffectInstance(effect));
         }
      }
   }

   private static void equipPair(ServerPlayer player, net.minecraft.world.item.Item main, net.minecraft.world.item.Item off) {
      ItemStack mainStack = new ItemStack(main);
      ItemStack offStack = new ItemStack(off);
      PlayerNoblePhantasmHelper.markUbwProjection(mainStack);
      PlayerNoblePhantasmHelper.markUbwProjection(offStack);
      player.setItemInHand(InteractionHand.MAIN_HAND, mainStack);
      player.setItemInHand(InteractionHand.OFF_HAND, offStack);
   }

   private static void cycleLoadout(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if ("paracelsus".equals(vars.servant_card_id)) {
         player.displayClientMessage(Component.literal("Element mode " + (vars.servant_card_action_mode + 1)), true);
         return;
      }
      ItemStack payload = switch (vars.servant_card_action_mode) {
         case 1 -> new ItemStack(ModItems.CRIMSON_HOUND.get());
         case 2 -> new ItemStack(ModItems.EXCALIBUR_GALLATIN.get());
         case 3 -> new ItemStack(ModItems.RHO_AIAS.get());
         default -> new ItemStack(ModItems.PSEUDO_SPIRAL_SWORD.get());
      };
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.NAMELESS_BOW.get()));
      player.setItemInHand(InteractionHand.OFF_HAND, payload);
   }

   private record SkillAction(String displayName, String effectId, double mpCost, int cooldownTicks) {
   }
}
