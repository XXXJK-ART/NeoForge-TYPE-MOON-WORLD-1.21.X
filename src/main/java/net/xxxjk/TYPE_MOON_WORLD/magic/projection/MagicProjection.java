package net.xxxjk.TYPE_MOON_WORLD.magic.projection;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NoblePhantasmItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;

public class MagicProjection {
   public static void execute(Entity entity) {
      if (entity instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         boolean crestProjectionCast = vars.isCurrentSelectionFromCrest("projection");
         boolean swordAttributeActive = vars.player_magic_attributes_sword && !crestProjectionCast;
         ItemStack target = vars.projection_selected_item == null ? ItemStack.EMPTY : vars.projection_selected_item;
         if (isRhoAiasProjectionTarget(target)) {
            tryProjectRhoAias(player, vars, target, swordAttributeActive, !crestProjectionCast, true);
            return;
         }
         InteractionHand handToUse = findAvailableHand(player);
         if (handToUse != null) {
            if (!target.isEmpty() && target.has(DataComponents.CUSTOM_DATA)) {
               CompoundTag custom = ((CustomData)target.get(DataComponents.CUSTOM_DATA)).copyTag();
               ResourceLocation executorId = ResourceLocation.tryParse(custom.getString("tmw_projection_executor"));
               if (executorId != null && net.xxxjk.TYPE_MOON_WORLD.api.ExtensionApiRegistry.hasProjectionItem(executorId)
                     && net.xxxjk.TYPE_MOON_WORLD.api.ExtensionApiRegistry.projectionItem(executorId,
                        new net.xxxjk.typemoonworld.api.ProjectionItemContext(player, (net.minecraft.server.level.ServerLevel)player.level(), player.blockPosition(), target, vars.player_mana))) {
                  vars.syncPlayerVariables(player);
                  return;
               }
            }
            if (target.isEmpty()) {
               player.displayClientMessage(Component.translatable("message.typemoonworld.projection.no_target"), true);
               return;
            }

            if (!isAnalyzedItem(vars, target)) {
               player.displayClientMessage(Component.translatable("message.typemoonworld.projection.requires_analysis"), true);
               return;
            }

            if (MagicStructuralAnalysis.isProjectionBanned(target)) {
               player.displayClientMessage(
                  Component.translatable(MagicStructuralAnalysis.isBedrock(target)
                     ? "message.typemoonworld.projection.cannot_project_bedrock"
                     : "message.typemoonworld.projection.cannot_project_divine"),
                  true
               );
               return;
            }

            double cost = applyProjectionMagicDiscount(player, calculateCost(target, swordAttributeActive, vars.proficiency_projection));
            if (ManaHelper.consumeOneTimeMagicCost(player, cost)) {
               if (!crestProjectionCast) {
                  net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService.add(vars, "projection", 0.2);
               }

               ItemStack projected = createProjectedItem(target, swordAttributeActive, player.level().getGameTime());
               player.setItemInHand(handToUse, projected);
               if (vars.has_unlimited_blade_works) {
                  PlayerNoblePhantasmHelper.tryCompleteProjectedKanshouBakuyaPair(player, projected, handToUse);
               }

               vars.syncPlayerVariables(player);
               grantAdvancement(player, "trace_on");
               player.displayClientMessage(Component.translatable("message.typemoonworld.trace_on"), true);
            }
         } else {
            player.displayClientMessage(Component.translatable("message.typemoonworld.projection.hands_full"), true);
         }
      }
   }

   public static boolean tryDirectProjectFromAnalysis(
      ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, ItemStack analyzedTarget, boolean swordAttributeActive
   ) {
      if (player == null || vars == null || analyzedTarget.isEmpty()
         || MagicStructuralAnalysis.isProjectionBanned(analyzedTarget)
         || !isAnalyzedItem(vars, analyzedTarget)) {
         return false;
      } else {
         if (isRhoAiasProjectionTarget(analyzedTarget)) {
            return tryProjectRhoAias(player, vars, analyzedTarget, swordAttributeActive, false, false);
         }
         InteractionHand handToUse = findAvailableHand(player);
         if (handToUse == null) {
            return false;
         } else {
            ItemStack projected = createProjectedItem(analyzedTarget, swordAttributeActive, player.level().getGameTime());
            player.setItemInHand(handToUse, projected);
            if (vars.has_unlimited_blade_works) {
               PlayerNoblePhantasmHelper.tryCompleteProjectedKanshouBakuyaPair(player, projected, handToUse);
            }
            vars.syncPlayerVariables(player);
            grantAdvancement(player, "trace_on");
            player.displayClientMessage(Component.translatable("message.typemoonworld.trace_on"), true);
            return true;
         }
      }
   }

   private static boolean tryProjectRhoAias(
      ServerPlayer player,
      TypeMoonWorldModVariables.PlayerVariables vars,
      ItemStack target,
      boolean swordAttributeActive,
      boolean addProjectionProficiency,
      boolean consumeMana
   ) {
      if (target.isEmpty()) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.projection.no_target"), true);
         return false;
      }
      if (!vars.has_unlimited_blade_works) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.projection.rho_aias_requires_ubw"), true);
         return false;
      }
      if (!isAnalyzedItem(vars, target)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.projection.requires_analysis"), true);
         return false;
      }
      if (MagicStructuralAnalysis.isProjectionBanned(target)) {
         player.displayClientMessage(
            Component.translatable(MagicStructuralAnalysis.isBedrock(target)
               ? "message.typemoonworld.projection.cannot_project_bedrock"
               : "message.typemoonworld.projection.cannot_project_divine"),
            true
         );
         return false;
      }
      if (consumeMana) {
         double cost = applyProjectionMagicDiscount(player, calculateCost(target, swordAttributeActive, vars.proficiency_projection));
         if (!ManaHelper.consumeOneTimeMagicCost(player, cost)) {
            return false;
         }
      }
      if (!RhoAiasProjectionHelper.spawn(player)) {
         return false;
      }
      if (addProjectionProficiency) {
         net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService.add(vars, "projection", 0.2);
      }
      vars.syncPlayerVariables(player);
      grantAdvancement(player, "trace_on");
      player.displayClientMessage(Component.translatable("message.typemoonworld.trace_on"), true);
      return true;
   }

   private static boolean isRhoAiasProjectionTarget(ItemStack stack) {
      return stack != null && stack.is(ModItems.RHO_AIAS.get());
   }

   public static double calculateCost(ItemStack stack, boolean hasSwordAttribute, double proficiency) {
      double baseCost = 10.0;
      if (stack.getItem() instanceof NoblePhantasmItem) {
         baseCost = 1000.0;
      } else {
         Rarity rarity = stack.getRarity();
         if (rarity == Rarity.UNCOMMON) {
            baseCost = 50.0;
         } else if (rarity == Rarity.RARE) {
            baseCost = 100.0;
         } else if (rarity == Rarity.EPIC) {
            baseCost = 300.0;
         }
      }

      int enchantCount = ((ItemEnchantments)stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)).size();
      if (enchantCount > 0) {
         baseCost *= 1.0 + enchantCount * 0.2;
      }

      if (stack.getMaxDamage() > 0) {
         baseCost *= 1.0 + stack.getMaxDamage() / 1000.0;
      }

      if (stack.getItem() instanceof BlockItem blockItem) {
         BlockState state = blockItem.getBlock().defaultBlockState();
         float hardness = state.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
         if (hardness > 0.0F) {
            baseCost += hardness * 5.0F;
         }
      }

      ItemAttributeModifiers modifiers = (ItemAttributeModifiers)stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
      double damage = modifiers.compute(0.0, EquipmentSlot.MAINHAND);
      if (damage > 0.0) {
         baseCost += damage * 5.0;
      }

      if (hasSwordAttribute) {
         boolean isSword = stack.getItem() instanceof SwordItem;
         if (isSword) {
            baseCost *= 0.1;
         } else {
            baseCost *= 0.5;
         }
      }

      return baseCost * (1.0 - proficiency * 0.005);
   }

   private static double applyProjectionMagicDiscount(ServerPlayer player, double cost) {
      if (player == null || cost <= 0.0 || !player.getPersistentData().getBoolean("ProjectionMagicActive")) {
         return cost;
      }
      float discount = player.getPersistentData().getFloat("ProjectionMagicSwordDiscount");
      if (discount <= 0.0F) {
         return cost;
      }
      return cost * Math.max(0.0F, Math.min(1.0F, discount));
   }

   private static boolean isNoblePhantasm(ItemStack stack) {
      return stack.getItem() instanceof NoblePhantasmItem;
   }

   private static void grantAdvancement(ServerPlayer player, String idPath) {
      if (player.getServer() != null) {
         ResourceLocation id = ResourceLocation.fromNamespaceAndPath("typemoonworld", idPath);
         AdvancementHolder holder = player.getServer().getAdvancements().get(id);
         if (holder != null) {
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);

            for (String criterion : progress.getRemainingCriteria()) {
               player.getAdvancements().award(holder, criterion);
            }
         }
      }
   }

   private static ItemStack findProjectionTargetLikeAnalysis(ServerPlayer player) {
      ItemStack heldItem = player.getMainHandItem();
      if (!heldItem.isEmpty()) {
         return heldItem;
      } else {
         HitResult hitResult = rayTrace(player, 5.0);
         if (hitResult.getType() == Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult)hitResult;
            BlockState state = player.level().getBlockState(blockHit.getBlockPos());
            return state.getBlock().asItem().getDefaultInstance();
         } else {
            if (hitResult.getType() == Type.ENTITY) {
               EntityHitResult entityHit = (EntityHitResult)hitResult;
               Entity hitEntity = entityHit.getEntity();
               if (hitEntity instanceof ItemEntity itemEntity) {
                  return itemEntity.getItem();
               }

               if (hitEntity instanceof LivingEntity livingEntity) {
                  ItemStack mainHand = livingEntity.getMainHandItem();
                  if (!mainHand.isEmpty()) {
                     return mainHand;
                  }

                  ItemStack offHand = livingEntity.getOffhandItem();
                  if (!offHand.isEmpty()) {
                     return offHand;
                  }
               }
            }

            return ItemStack.EMPTY;
         }
      }
   }

   private static ItemStack sanitizeProjectionTarget(ItemStack original) {
      ItemStack sanitized = original.copy();
      sanitized.setCount(1);
      sanitized.remove(DataComponents.CONTAINER);
      sanitized.remove(DataComponents.BUNDLE_CONTENTS);
      sanitized.remove(DataComponents.BLOCK_ENTITY_DATA);
      if (sanitized.has(DataComponents.CUSTOM_DATA)) {
         CustomData customData = (CustomData)sanitized.get(DataComponents.CUSTOM_DATA);
         if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("BlockEntityTag")) {
               CompoundTag blockEntityTag = tag.getCompound("BlockEntityTag");
               if (blockEntityTag.contains("Items")) {
                  blockEntityTag.remove("Items");
                  if (blockEntityTag.isEmpty()) {
                     tag.remove("BlockEntityTag");
                  } else {
                     tag.put("BlockEntityTag", blockEntityTag);
                  }
               }
            }

            if (tag.isEmpty()) {
               sanitized.remove(DataComponents.CUSTOM_DATA);
            } else {
               sanitized.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
         }
      }

      return sanitized;
   }

   private static InteractionHand findAvailableHand(ServerPlayer player) {
      if (player.getMainHandItem().isEmpty()) {
         return InteractionHand.MAIN_HAND;
      } else {
         return player.getOffhandItem().isEmpty() ? InteractionHand.OFF_HAND : null;
      }
   }

   private static ItemStack createProjectedItem(ItemStack target, boolean swordAttributeActive, long gameTime) {
      ItemStack projected = target.copy();
      projected.setCount(1);
      projected.remove(DataComponents.CONTAINER);
      projected.remove(DataComponents.BUNDLE_CONTENTS);
      projected.remove(DataComponents.BLOCK_ENTITY_DATA);
      if (projected.isDamageableItem()) {
         if (swordAttributeActive) {
            boolean isSword = projected.getItem() instanceof SwordItem;
            int maxDmg = projected.getMaxDamage();
            if (isSword) {
               projected.setDamageValue((int)(maxDmg * 0.333));
            } else {
               projected.setDamageValue((int)(maxDmg * 0.9));
            }
         } else {
            projected.setDamageValue(projected.getMaxDamage() - 1);
         }
      }

      CompoundTag tag = new CompoundTag();
      tag.putBoolean("is_projected", true);
      if (swordAttributeActive) {
         tag.putBoolean("is_infinite_projection", true);
      }

      tag.putLong("projection_time", gameTime);
      CustomData existingData = (CustomData)projected.get(DataComponents.CUSTOM_DATA);
      if (existingData != null) {
         CompoundTag existingTag = existingData.copyTag();
         existingTag.merge(tag);
         projected.set(DataComponents.CUSTOM_DATA, CustomData.of(existingTag));
      } else {
         projected.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      }

      projected.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
      return projected;
   }

   private static boolean isAnalyzedItem(TypeMoonWorldModVariables.PlayerVariables vars, ItemStack stack) {
      if (vars == null || stack == null || stack.isEmpty()) {
         return false;
      }

      for (ItemStack analyzed : vars.analyzed_items) {
         if (ItemStack.isSameItemSameComponents(analyzed, stack)) {
            return true;
         }
      }

      return false;
   }

   private static HitResult rayTrace(ServerPlayer player, double range) {
      float partialTicks = 1.0F;
      HitResult blockHit = player.pick(range, partialTicks, false);
      Vec3 eyePos = player.getEyePosition(partialTicks);
      double distToBlock = blockHit.getType() != Type.MISS ? blockHit.getLocation().distanceTo(eyePos) : range;
      Vec3 lookDir = player.getViewVector(partialTicks);
      Vec3 endPos = eyePos.add(lookDir.x * range, lookDir.y * range, lookDir.z * range);
      AABB searchBox = player.getBoundingBox().expandTowards(lookDir.scale(range)).inflate(1.0);
      EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
         player, eyePos, endPos, searchBox, e -> !e.isSpectator() && e.isPickable(), distToBlock * distToBlock
      );
      return (HitResult)(entityHit != null ? entityHit : blockHit);
   }
}
