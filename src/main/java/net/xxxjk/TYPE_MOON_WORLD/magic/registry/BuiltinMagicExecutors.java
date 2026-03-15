package net.xxxjk.TYPE_MOON_WORLD.magic.registry;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicRegistry;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.magic.broken_phantasm.MagicBrokenPhantasm;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.MagicJewelMachineGun;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.ruby.MagicRubyThrow;
import net.xxxjk.TYPE_MOON_WORLD.magic.nordic.MagicGander;
import net.xxxjk.TYPE_MOON_WORLD.magic.nordic.MagicGandrMachineGun;
import net.xxxjk.TYPE_MOON_WORLD.magic.other.MagicGravity;
import net.xxxjk.TYPE_MOON_WORLD.magic.projection.MagicProjection;
import net.xxxjk.TYPE_MOON_WORLD.magic.projection.MagicStructuralAnalysis;
import net.xxxjk.TYPE_MOON_WORLD.magic.reinforcement.MagicReinforcementEventHandler;
import net.xxxjk.TYPE_MOON_WORLD.magic.reinforcement.MagicReinforcementItem;
import net.xxxjk.TYPE_MOON_WORLD.magic.reinforcement.MagicReinforcementOther;
import net.xxxjk.TYPE_MOON_WORLD.magic.reinforcement.MagicReinforcementSelf;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.MagicSwordBarrelFullOpen;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.MagicUnlimitedBladeWorks;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class BuiltinMagicExecutors {
   private static final String PROVIDER = "typemoonworld_core";

   private BuiltinMagicExecutors() {
   }

   public static void registerBuiltin(IMagicRegistry registry) {
      registry.register("reinforcement", BuiltinMagicExecutors::executeReinforcement, "typemoonworld_core");
      registry.register("reinforcement_self", ctx -> executeSimple(ctx, MagicReinforcementSelf::execute), "typemoonworld_core");
      registry.register("reinforcement_other", ctx -> executeSimple(ctx, MagicReinforcementOther::execute), "typemoonworld_core");
      registry.register("reinforcement_item", ctx -> executeSimple(ctx, MagicReinforcementItem::execute), "typemoonworld_core");
      registry.register("jewel_magic_shoot", BuiltinMagicExecutors::executeKnowledgeOnly, "typemoonworld_core");
      registry.register("jewel_magic_release", BuiltinMagicExecutors::executeKnowledgeOnly, "typemoonworld_core");
      registry.register("jewel_random_shoot", ctx -> {
         boolean ok = MagicRubyThrow.executeRandom(ctx.entity(), 0);
         return ok ? MagicExecutionResult.SUCCESS : MagicExecutionResult.FAILED;
      }, "typemoonworld_core");
      registry.register("projection", ctx -> executeSimple(ctx, MagicProjection::execute), "typemoonworld_core");
      registry.register("structural_analysis", ctx -> executeSimple(ctx, MagicStructuralAnalysis::execute), "typemoonworld_core");
      registry.register("broken_phantasm", ctx -> executeSimple(ctx, MagicBrokenPhantasm::execute), "typemoonworld_core");
      registry.register("unlimited_blade_works", ctx -> executeSimple(ctx, MagicUnlimitedBladeWorks::execute), "typemoonworld_core");
      registry.register("sword_barrel_full_open", ctx -> executeSimple(ctx, MagicSwordBarrelFullOpen::execute), "typemoonworld_core");
      registry.register("gravity_magic", ctx -> executeSimple(ctx, MagicGravity::execute), "typemoonworld_core");
      registry.register("gander", ctx -> toResult(MagicGander.execute(ctx.entity())), "typemoonworld_core");
      registry.register("jewel_machine_gun", ctx -> toResult(MagicJewelMachineGun.execute(ctx.entity())), "typemoonworld_core");
      registry.register("gandr_machine_gun", ctx -> toResult(MagicGandrMachineGun.execute(ctx.entity())), "typemoonworld_core");
   }

   private static MagicExecutionResult executeKnowledgeOnly(MagicExecutionContext context) {
      Player player = context.asPlayer();
      if (player != null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.knowledge_only"), true);
      }

      return MagicExecutionResult.FAILED;
   }

   private static MagicExecutionResult executeReinforcement(MagicExecutionContext context) {
      Entity entity = context.entity();
      TypeMoonWorldModVariables.PlayerVariables vars = context.vars();
      switch (vars.reinforcement_target) {
         case 0:
            MagicReinforcementSelf.execute(entity);
            break;
         case 1:
            MagicReinforcementOther.execute(entity);
            break;
         case 2:
            MagicReinforcementItem.execute(entity);
            break;
         case 3:
            if (entity instanceof Player player) {
               handleReinforcementCancel(player, vars.reinforcement_mode);
            }
      }

      return MagicExecutionResult.SUCCESS;
   }

   private static void handleReinforcementCancel(Player player, int cancelType) {
      if (cancelType == 0) {
         TypeMoonWorldModVariables.ReinforcementData data = (TypeMoonWorldModVariables.ReinforcementData)player.getData(
            TypeMoonWorldModVariables.REINFORCEMENT_DATA
         );
         if (player.getUUID().equals(data.casterUUID)) {
            player.removeEffect(ModMobEffects.REINFORCEMENT_SELF_STRENGTH);
            player.removeEffect(ModMobEffects.REINFORCEMENT_SELF_DEFENSE);
            player.removeEffect(ModMobEffects.REINFORCEMENT_SELF_AGILITY);
            player.removeEffect(ModMobEffects.REINFORCEMENT_SELF_SIGHT);
            player.removeEffect(MobEffects.NIGHT_VISION);
            data.casterUUID = null;
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.cancel.self.success"), true);
         } else {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.cancel.other.not_yours"), true);
         }
      } else if (cancelType == 1) {
         if (player instanceof ServerPlayer serverPlayer) {
            HitResult hitResult = EntityUtils.getRayTraceTarget(serverPlayer, 10.0);
            if (hitResult.getType() == Type.ENTITY) {
               if (((EntityHitResult)hitResult).getEntity() instanceof LivingEntity livingTarget) {
                  TypeMoonWorldModVariables.ReinforcementData data = (TypeMoonWorldModVariables.ReinforcementData)livingTarget.getData(
                     TypeMoonWorldModVariables.REINFORCEMENT_DATA
                  );
                  if (player.getUUID().equals(data.casterUUID)) {
                     livingTarget.removeEffect(ModMobEffects.REINFORCEMENT_OTHER_STRENGTH);
                     livingTarget.removeEffect(ModMobEffects.REINFORCEMENT_OTHER_DEFENSE);
                     livingTarget.removeEffect(ModMobEffects.REINFORCEMENT_OTHER_AGILITY);
                     livingTarget.removeEffect(ModMobEffects.REINFORCEMENT_OTHER_SIGHT);
                     livingTarget.removeEffect(MobEffects.NIGHT_VISION);
                     data.casterUUID = null;
                     player.displayClientMessage(
                        Component.translatable("message.typemoonworld.magic.reinforcement.cancel.other.success", livingTarget.getDisplayName()),
                        true
                     );
                  } else {
                     player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.cancel.other.not_yours"), true);
                  }
               }
            } else {
               player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.no_target"), true);
            }
         }
      } else {
         if (cancelType == 2) {
            ItemStack stack = player.getMainHandItem();
            if (!stack.isEmpty() && stack.has(DataComponents.CUSTOM_DATA)) {
               CompoundTag tag = ((CustomData)stack.get(DataComponents.CUSTOM_DATA)).copyTag();
               if (tag.getBoolean("Reinforced")) {
                  if (tag.hasUUID("CasterUUID") && player.getUUID().equals(tag.getUUID("CasterUUID"))) {
                     MagicReinforcementEventHandler.removeReinforcement(player, stack, tag);
                     player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.cancel.item.success"), true);
                  } else {
                     player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.cancel.other.not_yours"), true);
                  }
               } else {
                  player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.cancel.item.not_reinforced"), true);
               }
            } else if (!stack.isEmpty()) {
               player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.cancel.item.not_reinforced"), true);
            }
         }
      }
   }

   private static MagicExecutionResult executeSimple(MagicExecutionContext context, BuiltinMagicExecutors.EntityMagicAction action) {
      action.apply(context.entity());
      return MagicExecutionResult.SUCCESS;
   }

   private static MagicExecutionResult toResult(boolean success) {
      return success ? MagicExecutionResult.SUCCESS : MagicExecutionResult.FAILED;
   }

   private interface EntityMagicAction {
      void apply(Entity var1);
   }
}
