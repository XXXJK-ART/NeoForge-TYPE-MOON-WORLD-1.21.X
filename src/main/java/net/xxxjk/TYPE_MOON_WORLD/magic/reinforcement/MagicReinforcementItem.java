package net.xxxjk.TYPE_MOON_WORLD.magic.reinforcement;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemAttributeModifiers.Builder;
import net.minecraft.world.item.component.ItemAttributeModifiers.Entry;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NoblePhantasmItem;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;

public class MagicReinforcementItem {
   public static void execute(Entity entity) {
      if (entity instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         ItemStack heldItem = player.getMainHandItem();
         if (heldItem.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.no_item"), true);
         } else {
            boolean isNoblePhantasm = heldItem.getItem() instanceof NoblePhantasmItem;
            boolean isStandardReinforceable = isNoblePhantasm
               || heldItem.isDamageableItem()
               || heldItem.getItem() instanceof TieredItem
               || heldItem.getItem() instanceof ArmorItem
               || heldItem.getItem() instanceof BowItem;
            int maxLevel = Math.min(5, 1 + (int)(vars.proficiency_reinforcement / 20.0));
            int requestLevel = vars.reinforcement_level == 0 ? 1 : vars.reinforcement_level;
            int level = Math.min(requestLevel, maxLevel);
            boolean wouldChange = false;
            if (!isStandardReinforceable) {
               wouldChange = true;
            } else {
               if (heldItem.isDamaged()) {
                  wouldChange = true;
               }

               Registry<Enchantment> registry = player.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
               Holder<Enchantment> primaryEnchantment = getPrimaryEnchantmentForItem(heldItem, registry, isNoblePhantasm);
               Holder<Enchantment> secondaryEnchantment = getSecondaryNoblePhantasmEnchantment(heldItem, registry, isNoblePhantasm, primaryEnchantment);
               if (primaryEnchantment != null) {
                  int currentLevel = EnchantmentHelper.getEnchantmentsForCrafting(heldItem).getLevel(primaryEnchantment);
                  if (level > currentLevel) {
                     wouldChange = true;
                  }
               }

               if (secondaryEnchantment != null) {
                  int currentLevel = EnchantmentHelper.getEnchantmentsForCrafting(heldItem).getLevel(secondaryEnchantment);
                  if (level > currentLevel) {
                     wouldChange = true;
                  }
               }
            }

            if (!wouldChange) {
               player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.item.already_better"), true);
            } else {
               double baseSuccess = 1.0 - level * 0.1;
               double proficiencyBonus = vars.proficiency_reinforcement / 100.0 * 0.4;
               double successChance = Math.min(1.0, baseSuccess + proficiencyBonus);
               double failChancePercent = 5.0 * level - vars.proficiency_reinforcement / 2.0;
               if (failChancePercent < 0.0) {
                  failChancePercent = 0.0;
               }

               failChancePercent = Math.min(50.0, failChancePercent);
               if (Math.random() * 100.0 < failChancePercent) {
                  player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.failed"), true);
                  player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 1.0F);
                  vars.proficiency_reinforcement = Math.min(100.0, vars.proficiency_reinforcement + 0.2);
                  vars.syncPlayerVariables(player);
               } else {
                  double cost = 50.0 * level;
                  if (ManaHelper.consumeManaOrHealth(player, cost)) {
                     boolean didSomething = false;
                     CompoundTag tag = ((CustomData)heldItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
                     tag.putBoolean("Reinforced", true);
                     tag.putInt("ReinforcedLevel", level);
                     tag.putLong("ReinforcementTime", player.level().getGameTime());
                     tag.putInt("ReinforcementExpiry", 600);
                     tag.putUUID("CasterUUID", player.getUUID());
                     boolean hadForcedGlint = Boolean.TRUE.equals(heldItem.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
                     if (!hadForcedGlint) {
                        heldItem.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                        tag.putBoolean("ReinforcementInjectedGlint", true);
                     }

                     if (!isStandardReinforceable) {
                        double addedDamage = 3.0;
                        if (level == 2) {
                           addedDamage = 4.0;
                        } else if (level == 3) {
                           addedDamage = 5.0;
                        } else if (level == 4) {
                           addedDamage = 5.5;
                        } else if (level >= 5) {
                           addedDamage = 6.0;
                        }

                        Builder builder = ItemAttributeModifiers.builder();
                        ItemAttributeModifiers existingModifiers = (ItemAttributeModifiers)heldItem.get(DataComponents.ATTRIBUTE_MODIFIERS);
                        if (existingModifiers != null) {
                           for (Entry entry : existingModifiers.modifiers()) {
                              builder.add(entry.attribute(), entry.modifier(), entry.slot());
                           }
                        }

                        builder.add(
                           Attributes.ATTACK_DAMAGE,
                           new AttributeModifier(
                              ResourceLocation.fromNamespaceAndPath("typemoonworld", "reinforcement_item_damage"), addedDamage, Operation.ADD_VALUE
                           ),
                           EquipmentSlotGroup.MAINHAND
                        );
                        heldItem.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
                        tag.putBoolean("ReinforcementTemporary", true);
                        tag.putInt("ReinforcementHitsLeft", 3);
                        didSomething = true;
                     } else {
                        Registry<Enchantment> registryx = player.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
                        Holder<Enchantment> primaryEnchantmentx = getPrimaryEnchantmentForItem(heldItem, registryx, isNoblePhantasm);
                        Holder<Enchantment> secondaryEnchantmentx = getSecondaryNoblePhantasmEnchantment(
                           heldItem, registryx, isNoblePhantasm, primaryEnchantmentx
                        );
                        if (applyEnchantmentUpgrade(heldItem, tag, primaryEnchantmentx, level, "ReinforcedEnchantment", "ReinforcedEnchantmentLevel")) {
                           didSomething = true;
                        }

                        if (applyEnchantmentUpgrade(
                           heldItem, tag, secondaryEnchantmentx, level, "ReinforcedEnchantmentExtra", "ReinforcedEnchantmentExtraLevel"
                        )) {
                           didSomething = true;
                        }

                        if (heldItem.isDamaged()) {
                           heldItem.setDamageValue(Math.max(0, heldItem.getDamageValue() - level * 50));
                           didSomething = true;
                        }
                     }

                     heldItem.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                     if (didSomething) {
                        player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.item.temporary_success"), true);
                        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
                        if (player.level() instanceof ServerLevel serverLevel) {
                           serverLevel.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 15, 0.5, 0.5, 0.5, 0.1);
                        }
                     } else {
                        player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.item.no_effect"), true);
                     }

                     vars.proficiency_reinforcement = Math.min(100.0, vars.proficiency_reinforcement + 0.5);
                     vars.syncPlayerVariables(player);
                  }
               }
            }
         }
      }
   }

   private static boolean applyEnchantmentUpgrade(
      ItemStack stack, CompoundTag tag, Holder<Enchantment> enchantment, int level, String idTagKey, String deltaTagKey
   ) {
      if (enchantment == null) {
         return false;
      } else {
         int currentLevel = EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(enchantment);
         if (level <= currentLevel) {
            return false;
         } else {
            enchantment.unwrapKey().ifPresent(key -> tag.putString(idTagKey, key.location().toString()));
            tag.putInt(deltaTagKey, level - currentLevel);
            EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(enchantment, level));
            return true;
         }
      }
   }

   private static Holder<Enchantment> getPrimaryEnchantmentForItem(ItemStack stack, Registry<Enchantment> registry, boolean isNoblePhantasm) {
      return (Holder<Enchantment>)(isNoblePhantasm ? registry.getHolderOrThrow(Enchantments.UNBREAKING) : getTypeBasedEnchantmentForItem(stack, registry));
   }

   private static Holder<Enchantment> getSecondaryNoblePhantasmEnchantment(
      ItemStack stack, Registry<Enchantment> registry, boolean isNoblePhantasm, Holder<Enchantment> primary
   ) {
      if (!isNoblePhantasm) {
         return null;
      } else {
         Holder<Enchantment> typeEnchantment = getTypeBasedEnchantmentForItem(stack, registry);
         return typeEnchantment != null && (primary == null || !typeEnchantment.equals(primary)) ? typeEnchantment : null;
      }
   }

   private static Holder<Enchantment> getTypeBasedEnchantmentForItem(ItemStack stack, Registry<Enchantment> registry) {
      if (stack.getItem() instanceof SwordItem) {
         return registry.getHolderOrThrow(Enchantments.SHARPNESS);
      } else if (stack.getItem() instanceof ArmorItem) {
         return registry.getHolderOrThrow(Enchantments.PROTECTION);
      } else if (stack.getItem() instanceof DiggerItem) {
         return registry.getHolderOrThrow(Enchantments.EFFICIENCY);
      } else if (stack.getItem() instanceof BowItem) {
         return registry.getHolderOrThrow(Enchantments.POWER);
      } else if (stack.getItem() instanceof CrossbowItem) {
         return registry.getHolderOrThrow(Enchantments.QUICK_CHARGE);
      } else if (stack.getItem() instanceof TridentItem) {
         return registry.getHolderOrThrow(Enchantments.IMPALING);
      } else if (stack.getItem() instanceof MaceItem) {
         return registry.getHolderOrThrow(Enchantments.DENSITY);
      } else {
         return stack.getItem() instanceof ShieldItem ? registry.getHolderOrThrow(Enchantments.UNBREAKING) : null;
      }
   }
}
