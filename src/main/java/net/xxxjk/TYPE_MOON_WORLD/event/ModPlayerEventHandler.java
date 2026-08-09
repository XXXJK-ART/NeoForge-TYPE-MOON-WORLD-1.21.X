package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.AvalonItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.BizenNagamitsuItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ThompsonContenderItem;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardZhaoYunSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper;

@EventBusSubscriber(
   modid = "typemoonworld"
)
public class ModPlayerEventHandler {
   private static boolean isModItem(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
         return id != null && "typemoonworld".equals(id.getNamespace());
      }
   }

   private static boolean checkMagus(Player player) {
      if (player.level().isClientSide()) {
         return true;
      } else if (player.isSpectator()) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.spectator_no_mod_item"), true);
         return false;
      } else {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (!vars.is_magus) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.not_magus_interaction"), true);
            return false;
         } else {
            return true;
         }
      }
   }

   private static boolean isPetrified(Player player) {
      return player.hasEffect(ModMobEffects.PETRIFIED);
   }

   @SubscribeEvent
   public static void onRightClickItem(RightClickItem event) {
      if (!event.getLevel().isClientSide()) {
         if (isPetrified(event.getEntity())) {
            event.setCanceled(true);
            return;
         }
         if (handleThompsonContenderRightClick(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            return;
         }
         if (handleZhaoYunSpearRightClick(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            return;
         }
         if (isModItem(event.getItemStack()) && !checkMagus(event.getEntity())) {
            event.setCanceled(true);
         }
         if (event.getEntity() instanceof ServerPlayer player) {
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            if (vars.servant_card_transformed && "oda_nobunaga".equals(vars.servant_card_id) && !player.isCrouching()) {
               net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardOdaNobunagaSkills.fireHeshikiriPrimary(player);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onRightClickBlock(RightClickBlock event) {
      if (!event.getLevel().isClientSide()) {
         if (isPetrified(event.getEntity())) {
            event.setCanceled(true);
            return;
         }
         if (handleThompsonContenderRightClick(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            return;
         }
         if (handleZhaoYunSpearRightClick(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            return;
         }
         if (event.getEntity() instanceof ServerPlayer player) {
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            if (vars.servant_card_transformed && "heracles".equals(vars.servant_card_id)) {
               net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHeraclesSkills.triggerHeraclesBlockAttack(player, event.getPos());
               event.setCanceled(true);
               return;
            }
         }
         if (isModItem(event.getItemStack()) && !checkMagus(event.getEntity())) {
            event.setCanceled(true);
         }
      }
   }

   @SubscribeEvent
   public static void onEntityInteract(EntityInteract event) {
      if (!event.getLevel().isClientSide()) {
         if (isPetrified(event.getEntity())) {
            event.setCanceled(true);
            return;
         }
         if (handleThompsonContenderRightClick(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            return;
         }
         if (handleZhaoYunSpearRightClick(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            return;
         }
         if (isModItem(event.getItemStack()) && !checkMagus(event.getEntity())) {
            event.setCanceled(true);
            return;
         }
         if (handleAvalonArtoriaCardActivation(event.getEntity(), event.getHand(), event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            return;
         }
      }
   }

   @SubscribeEvent
   public static void onLeftClickBlock(LeftClickBlock event) {
      if (!event.getLevel().isClientSide()) {
         if (isPetrified(event.getEntity())) {
            event.setCanceled(true);
            return;
         }
         if (isModItem(event.getItemStack()) && !checkMagus(event.getEntity())) {
            event.setCanceled(true);
         }
      }
   }

   @SubscribeEvent
   public static void onAttackEntity(AttackEntityEvent event) {
      if (event.getEntity().level().isClientSide()) {
         return;
      }
      if (isPetrified(event.getEntity())) {
         event.setCanceled(true);
         return;
      }
      if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof LivingEntity target) {
         ItemStack stack = player.getMainHandItem();
         if (stack.getItem() instanceof BizenNagamitsuItem && PlayerNoblePhantasmHelper.triggerTsubameOnHit(player, stack, target)) {
            event.setCanceled(true);
            return;
         }
         triggerArtoriaManaBurstTerrainBreak(player, target);
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterProtection.isProtectedMaster(player, target)) {
            event.setCanceled(true);
            return;
         }
         if (vars.servant_card_transformed && "shadow_hassan".equals(vars.servant_card_id)) {
            if (!net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardShadowHassanSkills.canAttack(player)) {
               event.setCanceled(true);
               return;
            }
            net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardShadowHassanSkills.revealForAttack(player);
         }
         if (vars.servant_card_transformed && "li_shuwen".equals(vars.servant_card_id)) {
            net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardLiShuwenSkills.revealCircleRealm(player);
         }
         if (vars.servant_card_transformed && "oda_nobunaga".equals(vars.servant_card_id) && !player.isCrouching()) {
            net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardOdaNobunagaSkills.fireHeshikiriPrimary(player);
         }
         if (vars.servant_card_transformed) {
            net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardVoiceHelper.tryPlayAttack(player);
            if ("zhao_yun_rider".equals(vars.servant_card_id)) {
               ServantCardZhaoYunSkills.markCombatActivity(player);
               net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardZhaoYunSkills.onZhaoYunAttack(player, target);
            }
         }
         net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHeraclesSkills.triggerHeraclesAttackImpact(player, target);
      }
   }

   private static boolean handleThompsonContenderRightClick(Player player, InteractionHand hand) {
      return player instanceof ServerPlayer serverPlayer && ThompsonContenderItem.handleServerRightClick(serverPlayer, hand);
   }

   /**
    * The transformed Zhao Yun player can use the Yajiao spear's ordinary
    * right-click as skill-slot 1.  Routing through the card manager keeps the
    * normal MP, cooldown, HUD feedback and skill implementation in one place.
    */
   private static boolean handleZhaoYunSpearRightClick(Player player, InteractionHand hand) {
      if (!(player instanceof ServerPlayer serverPlayer)
         || hand != InteractionHand.MAIN_HAND
         || !player.getItemInHand(hand).is(ModItems.YAJIAO_QIANG.get())) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars =
         player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed
         || !"zhao_yun_rider".equals(vars.servant_card_id)) {
         return false;
      }
      // This is Zhao Yun's ordinary weapon thrust, not a skill-slot action:
      // it deliberately bypasses the mana-cost pipeline and only keeps its
      // own 0.5-second action cooldown.
      return ServantCardZhaoYunSkills.performNormalSpearThrust(serverPlayer);
   }

   private static boolean handleAvalonArtoriaCardActivation(Player player, InteractionHand hand, Entity target) {
      if (!(player instanceof ServerPlayer serverPlayer)
         || player.isCrouching()
         || !(target instanceof ServerPlayer targetPlayer)) {
         return false;
      }
      ItemStack stack = player.getItemInHand(hand);
      if (!stack.is(ModItems.AVALON.get())) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables targetVars = targetPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!targetVars.servant_card_transformed || !"artoria_pendragon".equals(targetVars.servant_card_id)) {
         return false;
      }
      AvalonItem.activateFor(serverPlayer, stack, targetPlayer);
      player.displayClientMessage(Component.translatable("item.typemoonworld.avalon.active"), true);
      return true;
   }

   private static void triggerArtoriaManaBurstTerrainBreak(ServerPlayer player, LivingEntity target) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed
         || !"artoria_pendragon".equals(vars.servant_card_id)
         || !ArtoriaPendragonCombatHelper.isManaBurstActive(player)
         || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      int broken = 0;
      int limit = 18;
      BlockPos center = target.blockPosition();
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 2, 1))) {
         if (broken >= limit) {
            break;
         }
         BlockState state = level.getBlockState(pos);
         float hardness = state.getDestroySpeed(level, pos);
         if (state.isAir()
            || state.is(Blocks.BEDROCK)
            || hardness < 0.0F
            || hardness > 18.0F
            || state.getExplosionResistance(level, pos, null) >= 1200.0F) {
            continue;
         }
         if (level.removeBlock(pos, false)) {
            broken++;
            if (broken % 3 == 0) {
               level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.18, 0.18, 0.18, 0.025);
            }
         }
      }
      if (broken > 0) {
         level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + target.getBbHeight() * 0.45, target.getZ(), 10, 0.7, 0.45, 0.7, 0.05);
         level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.28F, 1.65F);
      }
   }
}
