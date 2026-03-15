package net.xxxjk.TYPE_MOON_WORLD.magic.jewel;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.GanderProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.CarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemQuality;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.gravity.GemGravityFieldMagic;
import net.xxxjk.TYPE_MOON_WORLD.magic.nordic.MagicGander;
import net.xxxjk.TYPE_MOON_WORLD.magic.other.MagicGravity;
import net.xxxjk.TYPE_MOON_WORLD.magic.other.MagicGravityEffectHandler;
import net.xxxjk.TYPE_MOON_WORLD.magic.projection.StructureProjectionBuildHandler;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.joml.Vector3f;

public final class GemEngravingService {
   public static final String TAG_ENGRAVED_MAGIC = "TypeMoonGemEngravedMagic";
   private static final String TAG_ENGRAVED_AT = "TypeMoonGemEngravedAt";
   public static final String TAG_ENGRAVED_MANA_COST = "TypeMoonGemEngravedManaCost";
   public static final String TAG_ENGRAVED_CONTENT_KEY = "TypeMoonGemEngravedContentKey";
   public static final String TAG_REINFORCEMENT_PART = "TypeMoonGemReinforcementPart";
   public static final String TAG_REINFORCEMENT_LEVEL = "TypeMoonGemReinforcementLevel";
   public static final String TAG_PROJECTION_KIND = "TypeMoonGemProjectionKind";
   public static final String TAG_PROJECTION_ITEM = "TypeMoonGemProjectionItem";
   public static final String TAG_PROJECTION_ITEM_NAME = "TypeMoonGemProjectionItemName";
   public static final String TAG_PROJECTION_STRUCTURE_ID = "TypeMoonGemProjectionStructureId";
   public static final String TAG_PROJECTION_STRUCTURE_NAME = "TypeMoonGemProjectionStructureName";
   public static final String PROJECTION_KIND_ITEM = "item";
   public static final String PROJECTION_KIND_STRUCTURE = "structure";
   private static final String ADVANCED_JEWEL_MAGIC_ID = "jewel_magic_release";
   private static final int GRAVITY_MIN_DURATION_TICKS = 600;
   private static final int GRAVITY_MAX_DURATION_TICKS = 3600;
   private static final int GRAVITY_RESULT_MESSAGE_DELAY_TICKS = 12;
   private static final int GANDER_MAX_CHARGE_SECONDS = 5;
   private static final double GANDER_RELEASE_FORWARD_FROM_ANCHOR = 0.08;
   private static final DustParticleOptions GANDER_BLACK_DUST = new DustParticleOptions(new Vector3f(0.05F, 0.05F, 0.05F), 1.0F);
   private static final DustParticleOptions GANDER_RED_DUST = new DustParticleOptions(new Vector3f(0.95F, 0.08F, 0.12F), 1.1F);

   private GemEngravingService() {
   }

   public static boolean tryEngraveWithChisel(ServerPlayer player, InteractionHand chiselHand, ItemStack chiselStack, ItemStack offhandStack) {
      if (offhandStack.getItem() instanceof CarvedGemItem carvedGem) {
         if (!player.isShiftKeyDown()) {
            return false;
         } else if (offhandStack.getCount() != 1) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.gem.engrave.single_required"), true);
            return true;
         } else {
            TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
               TypeMoonWorldModVariables.PLAYER_VARIABLES
            );
            if (!vars.learned_magics.contains(ADVANCED_JEWEL_MAGIC_ID)) {
               player.displayClientMessage(Component.translatable("message.typemoonworld.gem.engrave.locked"), true);
               return true;
            } else {
               String selectedMagic = getSelectedMagic(vars);
               if (selectedMagic == null) {
                  player.displayClientMessage(Component.translatable("message.typemoonworld.gem.engrave.no_magic_selected"), true);
                  return true;
               } else if (!GemCompatibilityService.isWhitelistedMagic(selectedMagic)) {
                  player.displayClientMessage(Component.translatable("message.typemoonworld.gem.engrave.not_supported"), true);
                  return true;
               } else if (getEngravedMagicId(offhandStack) != null) {
                  player.displayClientMessage(Component.translatable("message.typemoonworld.gem.engrave.already"), true);
                  return true;
               } else {
                  GemQuality quality = carvedGem.getQuality();
                  GemType type = carvedGem.getType();
                  int chance = GemCompatibilityService.calculateEngraveSuccessChance(quality, type, selectedMagic, vars.proficiency_jewel_magic_release);
                  int roll = player.level().getRandom().nextInt(100) + 1;
                  boolean success = roll <= chance;
                  damageChisel(player, chiselHand, chiselStack);
                  if (success) {
                     if (offhandStack.getItem() != ModItems.getNormalizedCarvedGem(type)) {
                        ItemStack normalized = new ItemStack(ModItems.getNormalizedCarvedGem(type), offhandStack.getCount());
                        copyEngravingData(offhandStack, normalized);
                        player.setItemInHand(InteractionHand.OFF_HAND, normalized);
                        offhandStack = normalized;
                     }

                     setEngravedMagic(offhandStack, selectedMagic);
                     vars.proficiency_jewel_magic_release = Math.min(100.0, vars.proficiency_jewel_magic_release + 0.3);
                     vars.syncPlayerVariables(player);
                     player.displayClientMessage(
                        Component.translatable("message.typemoonworld.gem.engrave.success", getMagicName(selectedMagic)), true
                     );
                     player.level().playSound(null, player.blockPosition(), SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.PLAYERS, 0.8F, 1.2F);
                  } else {
                     offhandStack.shrink(1);
                     player.displayClientMessage(Component.translatable("message.typemoonworld.gem.engrave.failed"), true);
                     player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 1.0F);
                  }

                  return true;
               }
            }
         }
      } else {
         return false;
      }
   }

   public static GemEngravingService.CastResult tryCastEngravedMagic(ServerPlayer player, InteractionHand hand, ItemStack gemStack) {
      String magicId = getEngravedMagicId(gemStack);
      if (magicId == null) {
         return GemEngravingService.CastResult.NOT_ENGRAVED;
      } else {
         boolean success = switch (magicId) {
            case "projection" -> castProjection(player, gemStack);
            case "reinforcement" -> castReinforcement(player, gemStack);
            case "gravity_magic" -> castGravity(player, gemStack);
            case "gander" -> castGander(player, gemStack);
            default -> false;
         };
         if (!success) {
            return GemEngravingService.CastResult.FAILED;
         } else {
            consumeHeldGem(player, hand, gemStack);
            return GemEngravingService.CastResult.SUCCESS;
         }
      }
   }

   public static String getEngravedMagicId(ItemStack stack) {
      CustomData customData = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      if (customData == null) {
         return null;
      } else {
         CompoundTag tag = customData.copyTag();
         if (!tag.contains(TAG_ENGRAVED_MAGIC, 8)) {
            return null;
         } else {
            String id = tag.getString(TAG_ENGRAVED_MAGIC);
            return id.isEmpty() ? null : id;
         }
      }
   }

   public static void copyEngravingData(ItemStack from, ItemStack to) {
      CustomData customData = (CustomData)from.get(DataComponents.CUSTOM_DATA);
      if (customData == null) {
         to.remove(DataComponents.CUSTOM_DATA);
      } else {
         CompoundTag tag = customData.copyTag();
         tag.remove(TAG_ENGRAVED_AT);
         ensureContentKey(tag);
         to.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      }
   }

   public static void setEngravedMagic(ItemStack stack, String magicId) {
      CompoundTag tag = ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      tag.putString(TAG_ENGRAVED_MAGIC, magicId);
      tag.remove(TAG_ENGRAVED_AT);
      tag.remove(TAG_ENGRAVED_MANA_COST);
      tag.remove(TAG_ENGRAVED_CONTENT_KEY);
      tag.remove(TAG_REINFORCEMENT_PART);
      tag.remove(TAG_REINFORCEMENT_LEVEL);
      tag.remove(TAG_PROJECTION_KIND);
      tag.remove(TAG_PROJECTION_ITEM);
      tag.remove(TAG_PROJECTION_ITEM_NAME);
      tag.remove(TAG_PROJECTION_STRUCTURE_ID);
      tag.remove(TAG_PROJECTION_STRUCTURE_NAME);
      tag.putString(TAG_ENGRAVED_CONTENT_KEY, magicId == null ? "" : magicId);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
   }

   public static void setEngravedManaCost(ItemStack stack, double manaCost) {
      CompoundTag tag = ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      tag.putDouble(TAG_ENGRAVED_MANA_COST, Math.max(0.0, manaCost));
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
   }

   public static double getEngravedManaCost(ItemStack stack) {
      CustomData customData = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      if (customData == null) {
         return 0.0;
      } else {
         CompoundTag tag = customData.copyTag();
         return !tag.contains(TAG_ENGRAVED_MANA_COST) ? 0.0 : Math.max(0.0, tag.getDouble(TAG_ENGRAVED_MANA_COST));
      }
   }

   public static void setReinforcementConfig(ItemStack stack, int part, int level, double manaCost) {
      int clampedPart = Math.max(0, Math.min(3, part));
      int clampedLevel = Math.max(1, Math.min(5, level));
      CompoundTag tag = ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      tag.putInt(TAG_REINFORCEMENT_PART, clampedPart);
      tag.putInt(TAG_REINFORCEMENT_LEVEL, clampedLevel);
      tag.putDouble(TAG_ENGRAVED_MANA_COST, Math.max(0.0, manaCost));
      tag.putString(TAG_ENGRAVED_CONTENT_KEY, "reinforcement:" + clampedPart + ":" + clampedLevel);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
   }

   public static void setProjectionItemConfig(ItemStack stack, ItemStack selectedItem, Provider registries, double manaCost) {
      ItemStack sanitized = sanitizeProjectionTemplate(selectedItem);
      String itemId = BuiltInRegistries.ITEM.getKey(sanitized.getItem()).toString();
      int payloadHash = sanitized.save(registries).hashCode();
      CompoundTag tag = ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      tag.putString(TAG_PROJECTION_KIND, PROJECTION_KIND_ITEM);
      tag.put(TAG_PROJECTION_ITEM, sanitized.save(registries));
      tag.putString(TAG_PROJECTION_ITEM_NAME, sanitized.getHoverName().getString());
      tag.putDouble(TAG_ENGRAVED_MANA_COST, Math.max(0.0, manaCost));
      tag.putString(TAG_ENGRAVED_CONTENT_KEY, "projection:item:" + itemId + ":" + Integer.toHexString(payloadHash));
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
   }

   public static void setProjectionStructureConfig(ItemStack stack, String structureId, String structureName, double manaCost) {
      String normalizedStructureId = structureId == null ? "" : structureId;
      CompoundTag tag = ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      tag.putString(TAG_PROJECTION_KIND, PROJECTION_KIND_STRUCTURE);
      tag.putString(TAG_PROJECTION_STRUCTURE_ID, normalizedStructureId);
      tag.putString(TAG_PROJECTION_STRUCTURE_NAME, structureName == null ? "" : structureName);
      tag.putDouble(TAG_ENGRAVED_MANA_COST, Math.max(0.0, manaCost));
      tag.putString(TAG_ENGRAVED_CONTENT_KEY, "projection:structure:" + normalizedStructureId);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
   }

   public static boolean isProjectionStructure(ItemStack stack) {
      CustomData customData = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      return customData == null ? false : PROJECTION_KIND_STRUCTURE.equals(customData.copyTag().getString(TAG_PROJECTION_KIND));
   }

   public static int getReinforcementPart(ItemStack stack, int fallback) {
      CustomData customData = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      if (customData == null) {
         return fallback;
      } else {
         CompoundTag tag = customData.copyTag();
         return !tag.contains(TAG_REINFORCEMENT_PART) ? fallback : Math.max(0, Math.min(3, tag.getInt(TAG_REINFORCEMENT_PART)));
      }
   }

   public static int getReinforcementLevel(ItemStack stack, int fallback) {
      CustomData customData = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      if (customData == null) {
         return fallback;
      } else {
         CompoundTag tag = customData.copyTag();
         return !tag.contains(TAG_REINFORCEMENT_LEVEL) ? fallback : Math.max(1, Math.min(5, tag.getInt(TAG_REINFORCEMENT_LEVEL)));
      }
   }

   public static String getProjectionStructureId(ItemStack stack) {
      CustomData customData = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      return customData == null ? "" : customData.copyTag().getString(TAG_PROJECTION_STRUCTURE_ID);
   }

   public static String getProjectionStructureName(ItemStack stack) {
      CustomData customData = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      return customData == null ? "" : customData.copyTag().getString(TAG_PROJECTION_STRUCTURE_NAME);
   }

   public static String getProjectionItemName(ItemStack stack) {
      CustomData customData = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      return customData == null ? "" : customData.copyTag().getString(TAG_PROJECTION_ITEM_NAME);
   }

   public static ItemStack getProjectionItemTemplate(ItemStack stack, Provider registries) {
      CustomData customData = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      if (customData == null) {
         return ItemStack.EMPTY;
      } else {
         CompoundTag tag = customData.copyTag();
         return !tag.contains(TAG_PROJECTION_ITEM, 10)
            ? ItemStack.EMPTY
            : ItemStack.parseOptional(registries, tag.getCompound(TAG_PROJECTION_ITEM));
      }
   }

   public static List<Component> getEngravingDetailLines(ItemStack stack) {
      List<Component> lines = new ArrayList<>();
      String magicId = getEngravedMagicId(stack);
      if (magicId == null) {
         return lines;
      } else {
         lines.add(Component.translatable("tooltip.typemoonworld.gem.engraved.magic", getMagicName(magicId)));
         switch (magicId) {
            case "reinforcement":
               int part = getReinforcementPart(stack, 0);
               int level = getReinforcementLevel(stack, 1);
               lines.add(Component.translatable("tooltip.typemoonworld.gem.engraved.reinforcement", getReinforcementPartName(part), level));
               break;
            case "projection":
               if (isProjectionStructure(stack)) {
                  String name = getProjectionStructureName(stack);
                  lines.add(
                     Component.translatable(
                        "tooltip.typemoonworld.gem.engraved.projection.mode",
                        Component.translatable("tooltip.typemoonworld.gem.engraved.projection.mode.structure")
                     )
                  );
                  if (!name.isEmpty()) {
                     lines.add(Component.translatable("tooltip.typemoonworld.gem.engraved.projection.target", Component.literal(name)));
                  }
               } else {
                  String itemName = getProjectionItemName(stack);
                  lines.add(
                     Component.translatable(
                        "tooltip.typemoonworld.gem.engraved.projection.mode",
                        Component.translatable("tooltip.typemoonworld.gem.engraved.projection.mode.item")
                     )
                  );
                  if (!itemName.isEmpty()) {
                     lines.add(Component.translatable("tooltip.typemoonworld.gem.engraved.projection.target", Component.literal(itemName)));
                  }
               }
               break;
            case "gravity_magic":
               lines.add(Component.translatable("tooltip.typemoonworld.gem.engraved.gravity.self_cast_hint"));
         }

         double manaCost = getEngravedManaCost(stack);
         if (manaCost > 0.0) {
            lines.add(Component.translatable("tooltip.typemoonworld.gem.engraved.mana_cost", (int)Math.ceil(manaCost)));
         }

         return lines;
      }
   }

   public static double calculateReinforcementManaCost(int level) {
      return 20.0 * Math.max(1, Math.min(5, level));
   }

   public static Component getReinforcementPartName(int part) {
      return switch (Math.max(0, Math.min(3, part))) {
         case 1 -> Component.translatable("gui.typemoonworld.mode.hand");
         case 2 -> Component.translatable("gui.typemoonworld.mode.leg");
         case 3 -> Component.translatable("gui.typemoonworld.mode.eye");
         default -> Component.translatable("gui.typemoonworld.mode.body");
      };
   }

   public static ItemStack sanitizeProjectionTemplate(ItemStack source) {
      ItemStack projected = source.copy();
      projected.setCount(1);
      projected.remove(DataComponents.CONTAINER);
      projected.remove(DataComponents.BUNDLE_CONTENTS);
      projected.remove(DataComponents.BLOCK_ENTITY_DATA);
      return projected;
   }

   private static String getSelectedMagic(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars.selected_magics.isEmpty()) {
         return null;
      } else {
         return vars.current_magic_index >= 0 && vars.current_magic_index < vars.selected_magics.size()
            ? vars.selected_magics.get(vars.current_magic_index)
            : null;
      }
   }

   private static void ensureContentKey(CompoundTag tag) {
      if (tag != null && tag.contains(TAG_ENGRAVED_MAGIC, 8)) {
         String key = tag.getString(TAG_ENGRAVED_CONTENT_KEY);
         if (key.isEmpty()) {
            String magicId = tag.getString(TAG_ENGRAVED_MAGIC);
            if ("reinforcement".equals(magicId)) {
               int part = Math.max(0, Math.min(3, tag.getInt(TAG_REINFORCEMENT_PART)));
               int level = Math.max(1, Math.min(5, tag.getInt(TAG_REINFORCEMENT_LEVEL)));
               tag.putString(TAG_ENGRAVED_CONTENT_KEY, "reinforcement:" + part + ":" + level);
            } else if ("projection".equals(magicId)) {
               String kind = tag.getString(TAG_PROJECTION_KIND);
               if (PROJECTION_KIND_STRUCTURE.equals(kind)) {
                  tag.putString(TAG_ENGRAVED_CONTENT_KEY, "projection:structure:" + tag.getString(TAG_PROJECTION_STRUCTURE_ID));
               } else if (PROJECTION_KIND_ITEM.equals(kind)) {
                  tag.putString(TAG_ENGRAVED_CONTENT_KEY, "projection:item:" + tag.getString(TAG_PROJECTION_ITEM_NAME));
               } else {
                  tag.putString(TAG_ENGRAVED_CONTENT_KEY, "projection");
               }
            } else {
               tag.putString(TAG_ENGRAVED_CONTENT_KEY, magicId);
            }
         }
      }
   }

   public static Component getMagicName(String magicId) {
      return switch (magicId) {
         case "projection" -> Component.translatable("magic.typemoonworld.projection.name");
         case "reinforcement" -> Component.translatable("magic.typemoonworld.reinforcement.name");
         case "gravity_magic" -> Component.translatable("magic.typemoonworld.gravity_magic.name");
         case "gander" -> Component.translatable("magic.typemoonworld.gander.name");
         default -> Component.literal(magicId);
      };
   }

   private static void damageChisel(ServerPlayer player, InteractionHand chiselHand, ItemStack chiselStack) {
      EquipmentSlot slot = chiselHand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
      chiselStack.hurtAndBreak(1, player, slot);
   }

   private static void consumeHeldGem(ServerPlayer player, InteractionHand hand, ItemStack stack) {
      stack.shrink(1);
      if (stack.isEmpty()) {
         player.setItemInHand(hand, ItemStack.EMPTY);
      }
   }

   private static boolean castProjection(ServerPlayer player, ItemStack gemStack) {
      return isProjectionStructure(gemStack) ? castProjectionStructure(player, gemStack) : castProjectionItem(player, gemStack);
   }

   private static boolean castProjectionItem(ServerPlayer player, ItemStack gemStack) {
      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      ItemStack selected = getProjectionItemTemplate(gemStack, player.registryAccess());
      if (selected.isEmpty()) {
         selected = vars.projection_selected_item;
      }

      if (selected.isEmpty()) {
         return false;
      } else {
         ItemStack projected = sanitizeProjectionTemplate(selected);
         if (projected.isDamageableItem()) {
            projected.setDamageValue(Math.max(0, projected.getMaxDamage() - 1));
         }

         CompoundTag tag = ((CustomData)projected.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
         tag.putBoolean("is_projected", true);
         tag.putLong("projection_time", player.level().getGameTime());
         projected.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
         projected.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
         if (!player.addItem(projected.copy())) {
            player.drop(projected.copy(), false);
         }

         return true;
      }
   }

   private static boolean castProjectionStructure(ServerPlayer player, ItemStack gemStack) {
      String structureId = getProjectionStructureId(gemStack);
      if (structureId.isEmpty()) {
         return false;
      } else {
         BlockPos anchor = player.blockPosition().above();
         if (player.pick(32.0, 1.0F, false) instanceof BlockHitResult blockHit) {
            anchor = blockHit.getBlockPos().relative(blockHit.getDirection());
         }

         return StructureProjectionBuildHandler.startProjectionFromGem(player, structureId, anchor, 0);
      }
   }

   private static boolean castReinforcement(ServerPlayer player, ItemStack gemStack) {
      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      int part = getReinforcementPart(gemStack, vars.reinforcement_mode);
      int level = getReinforcementLevel(gemStack, 1);
      int amplifier = Math.max(0, level - 1);
      int duration = (600 + (int)Math.round(Math.max(0.0, Math.min(100.0, vars.proficiency_reinforcement)) * 10.0)) * level;
      switch (part) {
         case 1:
            player.addEffect(new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_STRENGTH, duration, amplifier, false, false, true));
            break;
         case 2:
            player.addEffect(new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_AGILITY, duration, amplifier, false, false, true));
            break;
         case 3:
            player.addEffect(new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_SIGHT, duration, amplifier, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, false, false));
            break;
         default:
            player.addEffect(new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_DEFENSE, duration, amplifier, false, false, true));
      }

      return true;
   }

   private static boolean castGravity(ServerPlayer player, ItemStack gemStack) {
      return GemGravityFieldMagic.throwGravityFieldProjectile(player, gemStack);
   }

   private static boolean castGander(ServerPlayer player, ItemStack gemStack) {
      GanderProjectileEntity projectile = new GanderProjectileEntity(player.level(), player);
      projectile.setNoGravity(true);
      projectile.setChargeSeconds(GANDER_MAX_CHARGE_SECONDS);
      projectile.setVisualScale(MagicGander.getVisualScaleForChargeSeconds(GANDER_MAX_CHARGE_SECONDS));
      ItemStack visualGem = gemStack.copy();
      visualGem.setCount(1);
      projectile.setItem(visualGem);
      Vec3 direction = player.getLookAngle().normalize();
      if (EntityUtils.getRayTraceTarget(player, 48.0) instanceof EntityHitResult entityHitResult) {
         Entity target = entityHitResult.getEntity();
         if (!EntityUtils.isImmunePlayerTarget(target)) {
            Vec3 targetPos = target.getEyePosition().subtract(player.getEyePosition());
            if (targetPos.lengthSqr() > 1.0E-6) {
               direction = targetPos.normalize();
            }
         }
      }

      Vec3 spawnPos = MagicGander.getChargeAnchor(player).add(direction.scale(GANDER_RELEASE_FORWARD_FROM_ANCHOR));
      projectile.setPos(spawnPos);
      projectile.shoot(direction.x, direction.y, direction.z, 3.8F, 0.0F);
      player.level().addFreshEntity(projectile);
      player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 0.7F, 1.35F);
      spawnGanderGemParticles(player, spawnPos);
      return true;
   }

   public static boolean castGravitySelfFromGem(ServerPlayer player, InteractionHand hand, int mode) {
      return castGravityFromGem(player, hand, 0, mode);
   }

   public static boolean castGravityFromGem(ServerPlayer player, InteractionHand hand, int targetMode, int mode) {
      if (player == null) {
         return false;
      } else if (mode < MagicGravity.MODE_ULTRA_LIGHT || mode > MagicGravity.MODE_ULTRA_HEAVY) {
         return false;
      } else if (targetMode != MagicGravity.MODE_NORMAL && targetMode != MagicGravity.MODE_HEAVY) {
         return false;
      } else {
         ItemStack heldStack = player.getItemInHand(hand);
         if (!heldStack.isEmpty() && getEngravedMagicId(heldStack) != null && "gravity_magic".equals(getEngravedMagicId(heldStack))) {
            LivingEntity target = resolveGravityTarget(player, targetMode);
            if (target == null) {
               player.displayClientMessage(Component.translatable("message.typemoonworld.magic.gravity.no_target"), true);
               return false;
            } else {
               TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
                  TypeMoonWorldModVariables.PLAYER_VARIABLES
               );
               int duration = calculateGravityDurationTicks(vars.proficiency_gravity_magic);
               Component targetComp = (Component)(target == player ? Component.translatable("gui.typemoonworld.mode.self") : target.getDisplayName());
               player.displayClientMessage(MagicGravity.getChantComponentForMode(mode), true);
               Component resultMessage;
               if (mode == MagicGravity.MODE_NORMAL) {
                  MagicGravityEffectHandler.clearGravityState(target);
                  resultMessage = Component.translatable("message.typemoonworld.magic.gravity.normalized", targetComp);
               } else {
                  long until = player.level().getGameTime() + duration;
                  MagicGravityEffectHandler.applyGravityState(target, mode, until);

                  String modeKey = switch (mode) {
                     case -2 -> "gui.typemoonworld.mode.gravity.ultra_light";
                     case -1 -> "gui.typemoonworld.mode.gravity.light";
                     default -> "gui.typemoonworld.mode.gravity.normal";
                     case 1 -> "gui.typemoonworld.mode.gravity.heavy";
                     case 2 -> "gui.typemoonworld.mode.gravity.ultra_heavy";
                  };
                  resultMessage = Component.translatable(
                     "message.typemoonworld.magic.gravity.applied", targetComp, Component.translatable(modeKey), duration / 20
                  );
               }

               queueGravityActionbarResult(player, resultMessage);
               vars.proficiency_gravity_magic = Math.min(100.0, vars.proficiency_gravity_magic + 0.2);
               vars.syncPlayerVariables(player);
               consumeHeldGem(player, hand, heldStack);
               return true;
            }
         } else {
            return false;
         }
      }
   }

   private static LivingEntity resolveGravityTarget(ServerPlayer player, int targetMode) {
      if (targetMode == MagicGravity.MODE_NORMAL) {
         return player;
      } else {
         return EntityUtils.getRayTraceTarget(player, 10.0) instanceof EntityHitResult entityHitResult
               && entityHitResult.getEntity() instanceof LivingEntity living
               && living.isAlive()
            ? living
            : null;
      }
   }

   private static int calculateGravityDurationTicks(double proficiency) {
      double clamped = Math.max(0.0, Math.min(100.0, proficiency));
      double ratio = clamped / 100.0;
      return GRAVITY_MIN_DURATION_TICKS + (int)Math.round((GRAVITY_MAX_DURATION_TICKS - GRAVITY_MIN_DURATION_TICKS) * ratio);
   }

   private static void queueGravityActionbarResult(ServerPlayer player, Component message) {
      TYPE_MOON_WORLD.queueServerWork(GRAVITY_RESULT_MESSAGE_DELAY_TICKS, () -> {
         if (!player.isRemoved()) {
            player.displayClientMessage(message, true);
         }
      });
   }

   private static void spawnGanderGemParticles(ServerPlayer player, Vec3 center) {
      if (player.level() instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(GANDER_BLACK_DUST, center.x, center.y, center.z, 8, 0.08, 0.08, 0.08, 0.0);
         serverLevel.sendParticles(GANDER_RED_DUST, center.x, center.y, center.z, 6, 0.07, 0.07, 0.07, 0.0);
      }
   }

   public static enum CastResult {
      NOT_ENGRAVED,
      SUCCESS,
      FAILED;
   }
}
