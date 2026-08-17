package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveService;

public class SelfGeasScrollItem extends Item {
   public static final String CONTRACT_CLOSE_UNTIL_TAG = "TypeMoonSelfGeasCircuitClosedUntil";
   private static final String TAG_CONTRACT_ID = "ContractId";
   private static final String TAG_CREATOR_UUID = "CreatorUuid";
   private static final String TAG_CREATOR_NAME = "CreatorName";
   private static final String TAG_FORCE = "Force";
   private static final String TAG_SIGNER_UUID = "SignerUuid";
   private static final String TAG_SIGNER_NAME = "SignerName";
   private static final String TAG_TARGET_UUID = "TargetUuid";
   private static final String TAG_TARGET_NAME = "TargetName";

   public SelfGeasScrollItem(Properties properties) {
      super(properties);
   }

   public static ItemStack create(ServerPlayer creator, int force) {
      ItemStack stack = new ItemStack(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.SELF_GEAS_SCROLL.get());
      CompoundTag tag = new CompoundTag();
      tag.putUUID(TAG_CONTRACT_ID, UUID.randomUUID());
      tag.putInt(TAG_FORCE, Math.max(0, Math.min(100, force)));
      if (creator != null) {
         tag.putUUID(TAG_CREATOR_UUID, creator.getUUID());
         tag.putString(TAG_CREATOR_NAME, creator.getGameProfile().getName());
      }
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      return stack;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
         sign(stack, serverPlayer, true);
      }
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
   }

   @Override
   public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
      if (!entity.level().isClientSide() && entity instanceof ServerPlayer player) {
         sign(stack, player, false);
         return true;
      }
      return super.onEntitySwing(stack, entity);
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      CompoundTag tag = data(stack);
      tooltip.add(Component.translatable("tooltip.typemoonworld.self_geas_scroll.force", tag.getInt(TAG_FORCE)).withStyle(ChatFormatting.DARK_RED));
      tooltip.add(Component.translatable("tooltip.typemoonworld.self_geas_scroll.signer", nameOrUnset(tag, TAG_SIGNER_NAME)).withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("tooltip.typemoonworld.self_geas_scroll.target", nameOrUnset(tag, TAG_TARGET_NAME)).withStyle(ChatFormatting.GRAY));
      String creator = tag.getString(TAG_CREATOR_NAME);
      if (!creator.isEmpty()) {
         tooltip.add(Component.translatable("tooltip.typemoonworld.self_geas_scroll.creator", creator).withStyle(ChatFormatting.DARK_GRAY));
      }
   }

   public static boolean tryApplyContract(LivingIncomingDamageEvent event, ServerPlayer attacker, LivingEntity victim) {
      if (attacker == null || victim == null || event == null || attacker == victim) {
         return false;
      }
      ItemStack contract = findMatchingContract(attacker, victim);
      if (contract.isEmpty()) {
         return false;
      }
      CompoundTag tag = data(contract);
      int force = Math.max(0, Math.min(100, tag.getInt(TAG_FORCE)));
      TypeMoonWorldModVariables.PlayerVariables vars = attacker.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double gap = MagicProficiencyService.get(vars, "contract_magecraft") - force;
      if (gap > 75.0) {
         attacker.displayClientMessage(Component.translatable("message.typemoonworld.self_geas_scroll.resisted"), true);
         return false;
      }

      int debuffs = 3;
      float damageRatio = 0.75F;
      double closeChance = 1.0;
      int closeTicks = 20 * 60 * 5;
      if (gap > 50.0) {
         debuffs = 1;
         damageRatio = 0.25F;
         closeChance = 0.4;
         closeTicks = 20 * 60;
      } else if (gap > 25.0) {
         debuffs = 2;
         damageRatio = 0.50F;
         closeChance = 0.7;
         closeTicks = 20 * 60 * 3;
      }

      applyDebuffs(attacker, debuffs);
      attacker.hurt(attacker.damageSources().magic(), attacker.getMaxHealth() * damageRatio);
      if (!PassiveService.has(vars, PassiveService.HIGH_SPEED_DIVINE_WORDS) && attacker.getRandom().nextDouble() < closeChance) {
         vars.is_magic_circuit_open = false;
         vars.magic_circuit_open_timer = 0.0;
         attacker.getPersistentData().putLong(CONTRACT_CLOSE_UNTIL_TAG, attacker.level().getGameTime() + closeTicks);
         vars.syncMana(attacker);
      }
      attacker.displayClientMessage(Component.translatable("message.typemoonworld.self_geas_scroll.triggered"), true);
      return true;
   }

   private static void sign(ItemStack stack, ServerPlayer player, boolean signer) {
      CompoundTag tag = data(stack);
      ensureContractId(tag);
      if (!tag.contains(TAG_FORCE)) {
         tag.putInt(TAG_FORCE, 100);
      }
      if (signer) {
         tag.putUUID(TAG_SIGNER_UUID, player.getUUID());
         tag.putString(TAG_SIGNER_NAME, player.getGameProfile().getName());
         player.displayClientMessage(Component.translatable("message.typemoonworld.self_geas_scroll.signed.signer"), true);
      } else {
         tag.putUUID(TAG_TARGET_UUID, player.getUUID());
         tag.putString(TAG_TARGET_NAME, player.getGameProfile().getName());
         player.displayClientMessage(Component.translatable("message.typemoonworld.self_geas_scroll.signed.target"), true);
      }
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
   }

   private static ItemStack findMatchingContract(ServerPlayer attacker, LivingEntity victim) {
      UUID attackerId = attacker.getUUID();
      UUID victimId = victim.getUUID();
      ItemStack fromAttacker = findMatchingContractIn(attacker, attackerId, victimId);
      if (!fromAttacker.isEmpty()) {
         return fromAttacker;
      }
      return victim instanceof Player player ? findMatchingContractIn(player, attackerId, victimId) : ItemStack.EMPTY;
   }

   private static ItemStack findMatchingContractIn(Player player, UUID signer, UUID target) {
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack stack = player.getInventory().getItem(i);
         if (isMatchingContract(stack, signer, target)) {
            return stack;
         }
      }
      return ItemStack.EMPTY;
   }

   private static boolean isMatchingContract(ItemStack stack, UUID signer, UUID target) {
      if (stack.isEmpty() || !(stack.getItem() instanceof SelfGeasScrollItem)) {
         return false;
      }
      CompoundTag tag = data(stack);
      return tag.hasUUID(TAG_SIGNER_UUID) && tag.hasUUID(TAG_TARGET_UUID)
         && tag.getUUID(TAG_SIGNER_UUID).equals(signer)
         && tag.getUUID(TAG_TARGET_UUID).equals(target);
   }

   private static void applyDebuffs(ServerPlayer player, int count) {
      List<MobEffectInstance> effects = new ArrayList<>();
      effects.add(new MobEffectInstance(MobEffects.CONFUSION, 20 * 60, 0, false, true, true));
      effects.add(new MobEffectInstance(MobEffects.DARKNESS, 20 * 60, 0, false, true, true));
      effects.add(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 60, 0, false, true, true));
      while (effects.size() > count) {
         effects.remove(player.getRandom().nextInt(effects.size()));
      }
      for (MobEffectInstance effect : effects) {
         player.addEffect(effect);
      }
   }

   private static void ensureContractId(CompoundTag tag) {
      if (!tag.hasUUID(TAG_CONTRACT_ID)) {
         tag.putUUID(TAG_CONTRACT_ID, UUID.randomUUID());
      }
   }

   private static CompoundTag data(ItemStack stack) {
      CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
      return customData.copyTag();
   }

   private static Component nameOrUnset(CompoundTag tag, String key) {
      String value = tag.getString(key);
      return value.isEmpty() ? Component.translatable("tooltip.typemoonworld.self_geas_scroll.unset") : Component.literal(value);
   }
}
