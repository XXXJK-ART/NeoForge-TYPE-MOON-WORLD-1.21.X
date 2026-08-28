package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneProgram;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneReleaseMode;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneReleaseService;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneProgramService;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.RuneBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModDataComponents;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneInscriptionData;

/** Hooks the selected rune program into vanilla interaction events. */
@EventBusSubscriber(modid = "typemoonworld")
public final class RuneReleaseEvents {
   private static final String LAST_TRIGGER = "tmwRuneReleaseTick";
   private RuneReleaseEvents() { }

   @SubscribeEvent
   public static void onAttack(AttackEntityEvent event) {
      if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof LivingEntity target)) return;
      ItemStack held = player.getMainHandItem();
      triggerInscribed(player, held, RuneReleaseMode.WEAPON, target);
   }

   @SubscribeEvent
   public static void onRightClickItem(RightClickItem event) {
      if (!(event.getEntity() instanceof ServerPlayer player)) return;
      ItemStack held = event.getItemStack();
      RuneProgram selected = current(player);
      if (player.isCrouching() && selected != null && selected.releaseMode() != RuneReleaseMode.DIRECT_AIR
         && selected.releaseMode() != RuneReleaseMode.BLOCK_TRAP && selected.releaseMode() != RuneReleaseMode.BODY
         && mediumMatches(held, selected.releaseMode())) {
         int uses = selected.releaseMode() == RuneReleaseMode.RUNE_STONE ? (held.is(Items.SMOOTH_STONE) ? 3 : held.is(Items.STONE) ? 2 : 1) : 20;
         held.set(ModDataComponents.RUNE_INSCRIPTION.get(), new RuneInscriptionData(selected.uuid().toString(), selected.serializeNBT(), uses, selected.releaseMode(), "ready").toComponent());
         player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.typemoonworld.rune.inscribed"), true);
         event.setCanceled(true); event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS); return;
      }
      RuneInscriptionData stored = inscription(held);
      if (stored != null && (stored.medium() == RuneReleaseMode.RUNE_STONE || stored.medium() == RuneReleaseMode.TOOL)) {
         triggerInscribed(player, held, stored.medium(), player);
      }
   }

   @SubscribeEvent
   public static void onRightClickBlock(RightClickBlock event) {
      if (!(event.getEntity() instanceof ServerPlayer player)) return;
      RuneProgram program = current(player);
      if (program != null && program.releaseMode() == RuneReleaseMode.BLOCK_TRAP) {
         net.minecraft.core.BlockPos placePos = event.getPos().relative(event.getFace());
         if (!event.getLevel().getBlockState(placePos).canBeReplaced()) return;
         event.getLevel().setBlock(placePos, ModBlocks.RUNE_INSCRIPTION.get().defaultBlockState(), 3);
         if (event.getLevel().getBlockEntity(placePos) instanceof RuneBlockEntity runeBlock) runeBlock.configure(player, program);
         event.setCanceled(true);
         event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
      } else {
         triggerInscribed(player, event.getItemStack(), RuneReleaseMode.TOOL, player);
      }
   }

   @SubscribeEvent
   public static void onLeftClickBlock(LeftClickBlock event) {
      if (!(event.getEntity() instanceof ServerPlayer player)) return;
      triggerInscribed(player, event.getItemStack(), RuneReleaseMode.TOOL, player);
   }

   @SubscribeEvent
   public static void onDamage(LivingIncomingDamageEvent event) {
      if (!(event.getEntity() instanceof ServerPlayer player)) return;
      long now = player.level().getGameTime();
      if (now - player.getPersistentData().getLong(LAST_TRIGGER) < 10L) return;
      for (ItemStack armor : player.getArmorSlots()) {
         if (triggerInscribed(player, armor, RuneReleaseMode.ARMOR, player)) { player.getPersistentData().putLong(LAST_TRIGGER, now); break; }
      }
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (!(event.getEntity() instanceof ServerPlayer player)) return;
      RuneProgram program = current(player);
      CompoundTag data = player.getPersistentData();
      if (program == null || program.releaseMode() != RuneReleaseMode.BODY || !player.isCrouching() || player.getDeltaMovement().horizontalDistanceSqr() > 0.0025D) {
         data.remove("tmwRuneMeditation"); return;
      }
      int meditation = data.getInt("tmwRuneMeditation") + 1; data.putInt("tmwRuneMeditation", meditation);
      long now = player.level().getGameTime();
      if (meditation >= 60 && now - data.getLong("tmwRuneBodyLast") >= 200L) {
         if (RuneReleaseService.trigger(player, RuneReleaseMode.BODY, ItemStack.EMPTY, player)) data.putLong("tmwRuneBodyLast", now);
         data.putInt("tmwRuneMeditation", 0);
      }
   }

   private static RuneProgram current(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return RuneProgramService.find(vars, PlayerMagicSelectionService.getCurrentMagicId(vars));
   }

   private static boolean isRuneStone(ItemStack stack) {
      return !stack.isEmpty() && (stack.is(Items.STONE) || stack.is(Items.COBBLESTONE) || stack.is(Items.SMOOTH_STONE));
   }

   private static boolean isTool(ItemStack stack) {
      if (stack.isEmpty()) return false;
      String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
      return path.endsWith("pickaxe") || path.endsWith("axe") || path.endsWith("shovel") || path.endsWith("hoe") || path.endsWith("shears");
   }

   private static boolean mediumMatches(ItemStack stack, RuneReleaseMode mode) {
      if (stack == null || stack.isEmpty()) return false;
      return switch (mode) {
         case RUNE_STONE -> isRuneStone(stack);
         case WEAPON -> stack.getAttributeModifiers().modifiers().stream().anyMatch(e -> e.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
         case ARMOR -> stack.getItem() instanceof net.minecraft.world.item.ArmorItem;
         case TOOL -> isTool(stack);
         default -> false;
      };
   }

   private static RuneInscriptionData inscription(ItemStack stack) {
      if (stack == null || stack.isEmpty()) return null;
      var component = stack.get(ModDataComponents.RUNE_INSCRIPTION.get());
      if (component == null) return null;
      RuneInscriptionData data = RuneInscriptionData.fromComponent(component);
      return data.remainingUses() > 0 && "ready".equals(data.state()) ? data : null;
   }

   private static boolean triggerInscribed(ServerPlayer player, ItemStack stack, RuneReleaseMode mode, LivingEntity target) {
      RuneInscriptionData data = inscription(stack);
      if (data == null || data.medium() != mode) return false;
      RuneProgram program = RuneProgram.fromNBT(data.snapshot());
      if (!RuneReleaseService.triggerProgram(player, program, mode, stack, target)) return false;
      int remaining = data.remainingUses() - 1;
      stack.set(ModDataComponents.RUNE_INSCRIPTION.get(), new RuneInscriptionData(data.programId(), data.snapshot(), remaining, data.medium(), remaining <= 0 ? "spent" : "ready").toComponent());
      return true;
   }
}
