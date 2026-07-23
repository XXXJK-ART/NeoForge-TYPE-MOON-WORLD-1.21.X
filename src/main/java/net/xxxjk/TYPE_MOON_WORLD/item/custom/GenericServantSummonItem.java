package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.typemoonworld.api.event.ServantSummonEvent;
import net.neoforged.neoforge.common.NeoForge;

/** A single registry item that carries the servant id in CUSTOM_DATA. */
public final class GenericServantSummonItem extends Item {
   public static final String SERVANT_ID_TAG = "tmw_servant_id";

   public GenericServantSummonItem(Properties properties) { super(properties.stacksTo(1)); }

   public static ItemStack create(Item item, String servantId) {
      ItemStack stack = new ItemStack(item);
      CompoundTag tag = new CompoundTag();
      tag.putString(SERVANT_ID_TAG, servantId == null ? "" : servantId);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      return stack;
   }

   public static String servantId(ItemStack stack) {
      CustomData data = stack == null ? null : stack.get(DataComponents.CUSTOM_DATA);
      return data == null ? "" : data.copyTag().getString(SERVANT_ID_TAG);
   }

   @Override public Component getName(ItemStack stack) {
      ResourceLocation id = ResourceLocation.tryParse(servantId(stack));
      return id == null ? super.getName(stack) : Component.translatable("item." + id.getNamespace() + ".servant_summon." + id.getPath());
   }

   @Override public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
      ItemStack stack = context.getItemInHand();
      ResourceLocation id = ResourceLocation.tryParse(servantId(stack));
      if (id == null || !id.getNamespace().matches("[a-z][a-z0-9_]{1,63}")) return InteractionResult.FAIL;
      BlockPos clicked = context.getClickedPos();
      Direction face = context.getClickedFace();
      BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(face);
      ServantSummonEvent.Pre pre = NeoForge.EVENT_BUS.post(new ServantSummonEvent.Pre(server, id, pos));
      if (pre.isCanceled()) return InteractionResult.FAIL;
      var entity = ModEntities.GENERIC_SERVANT.get().create(server);
      if (entity == null) return InteractionResult.FAIL;
      entity.setServantId(id.toString());
      entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.01, context.getRotation(), 0.0F);
      if (!server.addFreshEntity(entity)) return InteractionResult.FAIL;
      NeoForge.EVENT_BUS.post(new ServantSummonEvent.Post(server, id, pos, entity));
      if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) stack.shrink(1);
      return InteractionResult.CONSUME;
   }
}
