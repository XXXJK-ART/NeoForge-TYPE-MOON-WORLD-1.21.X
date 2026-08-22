package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class MedeaReinforcementCharmItem extends Item {
   private static final ResourceLocation HEALTH_ID = id("medea_reinforcement_charm_health");
   private static final ResourceLocation SPEED_ID = id("medea_reinforcement_charm_speed");
   private static final ResourceLocation ARMOR_ID = id("medea_reinforcement_charm_armor");
   private static final ResourceLocation ATTACK_ID = id("medea_reinforcement_charm_attack");
   private static final double C_MAX_HEALTH = 300.0;
   private static final double C_MOVEMENT_SPEED = 0.28;
   private static final double C_ARMOR = 9.0;
   private static final double C_ATTACK_DAMAGE = 15.0;

   public MedeaReinforcementCharmItem(Properties properties) {
      super(properties);
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.add(Component.translatable("item.typemoonworld.medea_reinforcement_charm.desc").withStyle(ChatFormatting.LIGHT_PURPLE));
   }

   public static void tick(ServerPlayer player) {
      if (player == null || player.tickCount % 20 != 0) {
         return;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      boolean active = hasCharm(player);
      if (!active) {
         removeAll(player);
         return;
      }
      applyAtLeast(player, Attributes.MAX_HEALTH, HEALTH_ID, C_MAX_HEALTH);
      applyAtLeast(player, Attributes.MOVEMENT_SPEED, SPEED_ID, C_MOVEMENT_SPEED);
      applyAtLeast(player, Attributes.ARMOR, ARMOR_ID, C_ARMOR);
      applyAtLeast(player, Attributes.ATTACK_DAMAGE, ATTACK_ID, C_ATTACK_DAMAGE);
   }

   private static boolean hasCharm(ServerPlayer player) {
      if (player.getMainHandItem().is(ModItems.MEDEA_REINFORCEMENT_CHARM.get())
         || player.getOffhandItem().is(ModItems.MEDEA_REINFORCEMENT_CHARM.get())) {
         return true;
      }
      for (ItemStack stack : player.getInventory().items) {
         if (stack.is(ModItems.MEDEA_REINFORCEMENT_CHARM.get())) {
            return true;
         }
      }
      return false;
   }

   private static void applyAtLeast(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double targetValue) {
      AttributeInstance instance = player.getAttribute(attribute);
      if (instance == null) {
         return;
      }
      if (instance.getModifier(id) != null) {
         instance.removeModifier(id);
      }
      double missing = targetValue - instance.getValue();
      if (missing > 0.0) {
         instance.addTransientModifier(new AttributeModifier(id, missing, AttributeModifier.Operation.ADD_VALUE));
      }
   }

   private static void removeAll(ServerPlayer player) {
      remove(player.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID);
      remove(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID);
      remove(player.getAttribute(Attributes.ARMOR), ARMOR_ID);
      remove(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_ID);
      if (player.getHealth() > player.getMaxHealth()) {
         player.setHealth(player.getMaxHealth());
      }
   }

   private static void remove(AttributeInstance instance, ResourceLocation id) {
      if (instance != null && instance.getModifier(id) != null) {
         instance.removeModifier(id);
      }
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, path);
   }
}
