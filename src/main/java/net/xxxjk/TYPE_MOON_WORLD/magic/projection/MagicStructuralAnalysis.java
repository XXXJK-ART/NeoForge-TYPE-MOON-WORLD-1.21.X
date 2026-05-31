package net.xxxjk.TYPE_MOON_WORLD.magic.projection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.AvalonItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NoblePhantasmItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.TempleStoneSwordAxeItem;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;

public class MagicStructuralAnalysis {
   private static final double STRUCTURE_COST_NORMAL_FACTOR = 0.1;
   private static final double STRUCTURE_COST_SWORD_FACTOR = 0.05;

   public static void execute(Entity entity) {
      if (entity instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         boolean crestAnalysisCast = vars.isCurrentSelectionFromCrest("structural_analysis");
         boolean swordAttributeActive = vars.player_magic_attributes_sword && !crestAnalysisCast;
         ItemStack heldItem = player.getMainHandItem();
         ItemStack targetItem = ItemStack.EMPTY;
         if (!heldItem.isEmpty()) {
            targetItem = heldItem;
         } else {
            HitResult hitResult = rayTrace(player, 5.0);
            if (hitResult.getType() == Type.BLOCK) {
               BlockHitResult blockHit = (BlockHitResult)hitResult;
               BlockState state = player.level().getBlockState(blockHit.getBlockPos());
               targetItem = state.getBlock().asItem().getDefaultInstance();
            } else if (hitResult.getType() == Type.ENTITY) {
               EntityHitResult entityHit = (EntityHitResult)hitResult;
               if (entityHit.getEntity() instanceof ItemEntity itemEntity) {
                  targetItem = itemEntity.getItem();
               } else if (entityHit.getEntity() instanceof LivingEntity livingEntity) {
                  ItemStack mainHand = livingEntity.getMainHandItem();
                  ItemStack offHand = livingEntity.getOffhandItem();
                  if (!mainHand.isEmpty()) {
                     targetItem = mainHand;
                  } else if (!offHand.isEmpty()) {
                     targetItem = offHand;
                  }
               }
            }
         }

         if (targetItem.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.structural_analysis.no_target"), true);
         } else {
            analyzeItem(player, vars, targetItem, swordAttributeActive, crestAnalysisCast);
         }
      }
   }

   private static void analyzeItem(
      ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, ItemStack target, boolean swordAttributeActive, boolean crestAnalysisCast
   ) {
      if (target.getItem() instanceof AvalonItem) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.structural_analysis.cannot_analyze_divine"), true);
      } else {
         boolean isTempleStone = target.getItem() instanceof TempleStoneSwordAxeItem;
         boolean isProjected = false;
         if (target.has(DataComponents.CUSTOM_DATA)) {
            CustomData cd = (CustomData)target.get(DataComponents.CUSTOM_DATA);
            if (cd != null) {
               CompoundTag tag = cd.copyTag();
               isProjected = tag.contains("is_projected") || tag.contains("projection_time");
            }
         }

         if (isProjected && !isTempleStone) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.projection.cannot_analyze_projected"), true);
         } else if (isProjected) {
            double specialCost = calculateCost(target, swordAttributeActive, vars.proficiency_structural_analysis);
            if (!consumeAnalysisManaOrFail(player, vars, specialCost)) {
               player.displayClientMessage(Component.translatable("message.typemoonworld.structural_analysis.failed"), true);
            } else {
               player.displayClientMessage(Component.translatable("message.typemoonworld.structural_analysis.trigger_on_set"), true);
               player.addEffect(new MobEffectInstance(ModMobEffects.NINE_LIVES, 600, 0));
               player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 2));
               player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1));
            }
         } else {
            ItemStack toSave = target.copy();
            toSave.setCount(1);
            if (toSave.has(DataComponents.CONTAINER)) {
               toSave.remove(DataComponents.CONTAINER);
            }

            if (toSave.has(DataComponents.BUNDLE_CONTENTS)) {
               toSave.remove(DataComponents.BUNDLE_CONTENTS);
            }

            if (toSave.has(DataComponents.BLOCK_ENTITY_DATA)) {
               toSave.remove(DataComponents.BLOCK_ENTITY_DATA);
            }

            if (toSave.has(DataComponents.CUSTOM_DATA)) {
               CustomData cd = (CustomData)toSave.get(DataComponents.CUSTOM_DATA);
               if (cd != null) {
                  CompoundTag customTag = cd.copyTag();
                  if (customTag.contains("BlockEntityTag")) {
                     CompoundTag bet = customTag.getCompound("BlockEntityTag");
                     if (bet.contains("Items")) {
                        bet.remove("Items");
                        if (bet.isEmpty()) {
                           customTag.remove("BlockEntityTag");
                        } else {
                           customTag.put("BlockEntityTag", bet);
                        }

                        if (customTag.isEmpty()) {
                           toSave.remove(DataComponents.CUSTOM_DATA);
                        } else {
                           toSave.set(DataComponents.CUSTOM_DATA, CustomData.of(customTag));
                        }
                     }
                  }
               }
            }

            boolean known = false;

            for (ItemStack s : vars.analyzed_items) {
               if (ItemStack.isSameItemSameComponents(s, toSave)) {
                  known = true;
                  break;
               }
            }

            if (known) {
               player.displayClientMessage(Component.translatable("message.typemoonworld.projection.already_analyzed"), true);
            } else {
               double cost = calculateCost(toSave, swordAttributeActive, vars.proficiency_structural_analysis);
               double successRate = 0.5 + vars.proficiency_structural_analysis * 0.005;
               if (swordAttributeActive && toSave.getItem() instanceof SwordItem) {
                  successRate = 1.0;
               }

               if (successRate > 1.0) {
                  successRate = 1.0;
               }

               boolean success = player.getRandom().nextDouble() < successRate;
               if (success) {
                  if (!consumeAnalysisManaOrFail(player, vars, cost)) {
                     player.displayClientMessage(Component.translatable("message.typemoonworld.structural_analysis.failed"), true);
                  } else {
                     vars.analyzed_items.add(toSave);
                     if (!crestAnalysisCast) {
                        vars.proficiency_structural_analysis = Math.min(100.0, vars.proficiency_structural_analysis + 0.5);
                     }

                     vars.syncPlayerVariables(player);
                     player.displayClientMessage(Component.translatable("message.typemoonworld.projection.analysis_complete", (int)cost), true);
                     boolean ubwDirectProjection = vars.has_unlimited_blade_works
                        && (vars.projection_selected_item == null || vars.projection_selected_item.isEmpty())
                        && (vars.projection_selected_structure_id == null || vars.projection_selected_structure_id.isEmpty());
                     if (ubwDirectProjection) {
                        MagicProjection.tryDirectProjectFromAnalysis(player, vars, toSave, swordAttributeActive);
                     }
                  }
               } else {
                  double failCost = cost * 0.3;
                  consumeAnalysisManaOrFail(player, vars, failCost);
                  if (!crestAnalysisCast) {
                     vars.proficiency_structural_analysis = Math.min(100.0, vars.proficiency_structural_analysis + 0.1);
                  }

                  vars.syncPlayerVariables(player);
                  player.displayClientMessage(Component.translatable("message.typemoonworld.structural_analysis.failed"), true);
               }
            }
         }
      }
   }

   public static boolean consumeAnalysisManaOrFail(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, double cost) {
      return ManaHelper.consumeOneTimeMagicCost(player, cost);
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

   public static double calculateCost(ItemStack stack, boolean hasSwordAttribute) {
      return calculateCost(stack, hasSwordAttribute, 0.0);
   }

   public static double calculateStructureCost(ItemStack stack, boolean hasSwordAttribute) {
      double factor = hasSwordAttribute ? 0.05 : 0.1;
      return calculateCost(stack, hasSwordAttribute) * factor;
   }
}
