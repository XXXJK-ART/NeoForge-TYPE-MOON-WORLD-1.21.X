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
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.minecraft.ChatFormatting;
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
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneProgramKind;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneEffectDispatcher;

/** Hooks the selected rune program into vanilla interaction events. */
@EventBusSubscriber(modid = "typemoonworld")
public final class RuneReleaseEvents {
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
      // Inscription mode is a normal right-click action. Keep crouch as an
      // explicit modifier for weapon/armor/tool engraving so those media do
      // not steal vanilla item use actions.
      boolean engravingRequest = selected != null && selected.releaseMode() != RuneReleaseMode.DIRECT_AIR
         && selected.releaseMode() != RuneReleaseMode.BLOCK_TRAP && selected.releaseMode() != RuneReleaseMode.BODY
         && mediumMatches(held, selected.releaseMode())
         && (selected.releaseMode() == RuneReleaseMode.RUNE_STONE || player.isCrouching())
         && inscription(held) == null;
      if (engravingRequest) {
         inscribe(player, held, selected);
         event.setCanceled(true); event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS); return;
      }
      RuneInscriptionData stored = inscription(held);
      if (stored != null && (stored.medium() == RuneReleaseMode.RUNE_STONE || stored.medium() == RuneReleaseMode.TOOL)) {
         if (triggerInscribed(player, held, stored.medium(), player)) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
         }
      }
   }

   @SubscribeEvent
   public static void onRightClickBlock(RightClickBlock event) {
      if (!(event.getEntity() instanceof ServerPlayer player)) return;
      RuneProgram program = current(player);
      if (program != null && program.releaseMode() == RuneReleaseMode.BLOCK_TRAP) {
         net.minecraft.core.BlockPos placePos = event.getPos().relative(event.getFace());
         if (!event.getLevel().getBlockState(placePos).canBeReplaced()) return;
         if (event.getLevel().setBlock(placePos, ModBlocks.RUNE_INSCRIPTION.get().defaultBlockState(), 3)) {
            if (event.getLevel().getBlockEntity(placePos) instanceof RuneBlockEntity runeBlock) runeBlock.configure(player, program);
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
         }
      } else {
         ItemStack held = event.getItemStack();
         RuneInscriptionData stored = inscription(held);
         if (program != null && program.releaseMode() == RuneReleaseMode.RUNE_STONE
            && mediumMatches(held, RuneReleaseMode.RUNE_STONE) && stored == null) {
            inscribe(player, held, program);
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
         } else if (stored != null && (stored.medium() == RuneReleaseMode.RUNE_STONE || stored.medium() == RuneReleaseMode.TOOL)
            && triggerInscribed(player, held, stored.medium(), player)) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
         }
      }
   }

   @SubscribeEvent
   public static void onLeftClickBlock(LeftClickBlock event) {
      if (!(event.getEntity() instanceof ServerPlayer player)) return;
      triggerInscribed(player, event.getItemStack(), RuneReleaseMode.TOOL, player);
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (!(event.getEntity() instanceof ServerPlayer player)) return;
      if (player.tickCount % 20 == 0) {
         for (ItemStack armor : player.getArmorSlots()) {
            RuneInscriptionData data = inscription(armor);
            if (data != null && data.medium() == RuneReleaseMode.ARMOR) {
               RuneProgram reinforcement = RuneProgram.fromNBT(data.snapshot());
               if (reinforcement.kind() == RuneProgramKind.REINFORCEMENT) {
                  RuneEffectDispatcher.applyReinforcement(player, reinforcement);
                  RuneEffectDispatcher.emitProgramGlyphs(player, reinforcement);
               }
            }
         }
      }
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
      return !stack.isEmpty() && !(stack.getItem() instanceof net.minecraft.world.item.ArmorItem)
         && !(stack.getItem() instanceof net.minecraft.world.item.TieredItem)
         && !(stack.getItem() instanceof net.minecraft.world.item.SwordItem);
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

   @SubscribeEvent
   public static void onItemTooltip(ItemTooltipEvent event) {
      RuneInscriptionData data = inscription(event.getItemStack());
      if (data == null) {
         var component = event.getItemStack().get(ModDataComponents.RUNE_INSCRIPTION.get());
         if (component != null) data = RuneInscriptionData.fromComponent(component);
      }
      if (data == null || data.snapshot().isEmpty()) return;
      RuneProgram program = RuneProgram.fromNBT(data.snapshot());
      event.getToolTip().add(net.minecraft.network.chat.Component.translatable(
         "tooltip.typemoonworld.rune.inscribed", program.displayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
      event.getToolTip().add(net.minecraft.network.chat.Component.translatable(
         "tooltip.typemoonworld.rune.uses", data.remainingUses()).withStyle(data.remainingUses() > 0 ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
   }

   private static void inscribe(ServerPlayer player, ItemStack stack, RuneProgram program) {
      stack.set(ModDataComponents.RUNE_INSCRIPTION.get(), new RuneInscriptionData(
         program.uuid().toString(), program.serializeNBT(), 1, program.releaseMode(), "ready").toComponent());
      player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.typemoonworld.rune.inscribed"), true);
   }

   private static boolean triggerInscribed(ServerPlayer player, ItemStack stack, RuneReleaseMode mode, LivingEntity target) {
      RuneInscriptionData data = inscription(stack);
      if (data == null || data.medium() != mode) return false;
      RuneProgram program = RuneProgram.fromNBT(data.snapshot());
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double manaBefore = vars.player_mana;
      if (!RuneReleaseService.triggerProgram(player, program, mode, stack, target)) {
         vars.player_mana = manaBefore;
         vars.syncMana(player);
         return false;
      }
      int remaining = data.remainingUses() - 1;
      stack.set(ModDataComponents.RUNE_INSCRIPTION.get(), new RuneInscriptionData(data.programId(), data.snapshot(), remaining, data.medium(), remaining <= 0 ? "spent" : "ready").toComponent());
      return true;
   }
}
