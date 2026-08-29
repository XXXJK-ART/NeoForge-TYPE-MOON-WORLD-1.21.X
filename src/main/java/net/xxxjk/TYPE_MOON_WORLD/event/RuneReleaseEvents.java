package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
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
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneDefinition;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RunePosition;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneProgramCostService;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneProgramExecutor;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneRegistry;

/** Hooks the selected rune program into vanilla interaction events. */
@EventBusSubscriber(modid = "typemoonworld")
public final class RuneReleaseEvents {
   private static final String PROJECTILE_RUNE = "tmwRuneProjectile";
   private static final String PROJECTILE_TARGET = "tmwRuneTrackingTarget";
   private static final String ARMOR_RING_TICK = "tmwRuneArmorRingTick";
   private RuneReleaseEvents() { }

   @SubscribeEvent
   public static void onProjectileJoin(EntityJoinLevelEvent event) {
      if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Projectile projectile)
         || !(projectile.getOwner() instanceof ServerPlayer player)) return;
      ItemStack inscription = projectileMedium(player);
      RuneInscriptionData data = inscription(inscription);
      if (data == null || data.medium() != RuneReleaseMode.WEAPON) return;
      RuneProgram program = RuneProgram.fromNBT(data.snapshot());
      if (!payProjectileCost(player, program)) return;
      var tag = projectile.getPersistentData();
      tag.putBoolean(PROJECTILE_RUNE, true);
      tag.put("tmwRuneProgram", program.serializeNBT());
      tag.putBoolean("tmwRuneTracking", hasTracking(program));
      tag.putBoolean("tmwRuneSureHit", hasSureHit(program));
      tag.putInt("tmwRuneTrackingStrength", trackingStrength(program));
      projectile.setNoGravity(true);
   }

   @SubscribeEvent
   public static void onProjectileTick(EntityTickEvent.Post event) {
      if (!(event.getEntity() instanceof Projectile projectile) || projectile.level().isClientSide()
         || !projectile.getPersistentData().getBoolean(PROJECTILE_RUNE)
         || !projectile.getPersistentData().getBoolean("tmwRuneTracking")) return;
      if (!(projectile.getOwner() instanceof LivingEntity owner)) return;
      if (projectile.tickCount % 3 != 0) return;
      LivingEntity target = cachedProjectileTarget(projectile);
      if (target == null || !target.isAlive() || target.distanceToSqr(projectile) > 1024.0D) {
         target = nearestProjectileTarget(projectile, owner, projectile.getPersistentData().getBoolean("tmwRuneSureHit"));
         if (target != null) projectile.getPersistentData().putUUID(PROJECTILE_TARGET, target.getUUID());
      }
      if (target == null) return;
      Vec3 velocity = projectile.getDeltaMovement();
      double speed = Math.max(0.25D, velocity.length());
      Vec3 desired = target.getEyePosition().subtract(projectile.position()).normalize();
      int strength = Math.max(1, projectile.getPersistentData().getInt("tmwRuneTrackingStrength"));
      double blend = Math.min(0.72D, 0.10D + strength * 0.08D);
      projectile.setDeltaMovement(velocity.normalize().scale(speed).scale(1.0D - blend).add(desired.scale(speed * blend)));
      projectile.hurtMarked = true;
   }

   @SubscribeEvent
   public static void onProjectileImpact(ProjectileImpactEvent event) {
      Projectile projectile = event.getProjectile();
      if (projectile.level().isClientSide() || !projectile.getPersistentData().getBoolean(PROJECTILE_RUNE)) return;
      if (projectile.getPersistentData().getBoolean("tmwRuneSureHit")
         && !(event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult)) {
         if (projectile.getOwner() instanceof LivingEntity owner) {
            LivingEntity target = nearestProjectileTarget(projectile, owner, true);
            if (target != null) {
               Vec3 velocity = target.getEyePosition().subtract(projectile.position()).normalize().scale(Math.max(0.35D, projectile.getDeltaMovement().length()));
               projectile.setDeltaMovement(velocity);
               event.setCanceled(true);
               return;
            }
         }
      }
      if (event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult hit
         && hit.getEntity() instanceof LivingEntity target
         && projectile.getOwner() instanceof ServerPlayer player) {
         CompoundTag snapshot = projectile.getPersistentData().getCompound("tmwRuneProgram");
         if (!snapshot.isEmpty()) RuneProgramExecutor.applyProjectileEffects(player, target, RuneProgram.fromNBT(snapshot));
      }
   }

   @SubscribeEvent
   public static void onAttack(AttackEntityEvent event) {
      if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof LivingEntity target)) return;
      ItemStack held = player.getMainHandItem();
      triggerInscribed(player, held, RuneReleaseMode.WEAPON, target);
   }

   @SubscribeEvent
   public static void onArmorRuneTriggered(LivingIncomingDamageEvent event) {
      if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) return;
      long now = player.level().getGameTime();
      if (now - player.getPersistentData().getLong(ARMOR_RING_TICK) < 20L) return;
      boolean triggered = false;
      for (ItemStack armor : player.getArmorSlots()) {
         RuneInscriptionData data = inscription(armor);
         if (data == null || data.medium() != RuneReleaseMode.ARMOR) continue;
         RuneProgram reinforcement = RuneProgram.fromNBT(data.snapshot());
         if (reinforcement.kind() != RuneProgramKind.REINFORCEMENT) continue;
         RuneEffectDispatcher.applyReinforcement(player, reinforcement);
         RuneEffectDispatcher.emitProgramGlyphs(player, reinforcement);
         triggered = true;
      }
      if (triggered) player.getPersistentData().putLong(ARMOR_RING_TICK, now);
   }

   private static ItemStack projectileMedium(ServerPlayer player) {
      ItemStack main = player.getMainHandItem();
      if (mediumMatches(main, RuneReleaseMode.WEAPON)) return main;
      ItemStack off = player.getOffhandItem();
      return mediumMatches(off, RuneReleaseMode.WEAPON) ? off : ItemStack.EMPTY;
   }

   private static boolean payProjectileCost(ServerPlayer player, RuneProgram program) {
      long now = player.level().getGameTime();
      if (player.getPersistentData().getLong("tmwRuneProjectileCostTick") == now) return true;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double cost = RuneProgramCostService.calculate(program);
      if (!vars.is_magic_circuit_open || vars.player_mana < cost) {
         player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.typemoonworld.magic.insufficient_mana"), true);
         return false;
      }
      vars.player_mana = Math.max(0.0D, vars.player_mana - cost);
      player.getPersistentData().putLong("tmwRuneProjectileCostTick", now);
      vars.syncMana(player);
      return true;
   }

   private static boolean hasTracking(RuneProgram program) {
      return trackingStrength(program) > 0;
   }

   private static boolean hasSureHit(RuneProgram program) {
      int strength = trackingStrength(program);
      return strength >= 2 || program.sequence().stream().anyMatch(id -> "sowilo".equals(id) || "tiwaz".equals(id));
   }

   private static int trackingStrength(RuneProgram program) {
      int strength = 0;
      for (int i = 0; i < program.sequence().size(); i++) {
         RuneDefinition definition = RuneRegistry.get(program.sequence().get(i));
         RunePosition role = i < program.sequencePositions().size() ? program.sequencePositions().get(i) : RunePosition.EFFECT;
         if (definition == null) continue;
         String semantic = definition.semantic(role);
         if ("accuracy".equals(semantic) || "focus".equals(semantic) || "luck".equals(semantic) || "control".equals(semantic) || "link".equals(semantic)) strength++;
      }
      return Math.min(8, strength);
   }

   private static LivingEntity nearestProjectileTarget(Projectile projectile, LivingEntity owner, boolean sureHit) {
      double radius = sureHit ? 32.0D : 24.0D;
      Vec3 origin = projectile.position();
      Vec3 direction = projectile.getDeltaMovement().lengthSqr() > 1.0E-6 ? projectile.getDeltaMovement().normalize() : owner.getLookAngle();
      return projectile.level().getEntitiesOfClass(LivingEntity.class, new AABB(origin, origin).inflate(radius), entity -> {
         if (!entity.isAlive() || entity == owner || entity.isSpectator()) return false;
         Vec3 to = entity.getEyePosition().subtract(origin);
         return sureHit || direction.dot(to.normalize()) > 0.35D;
      }).stream().min(java.util.Comparator.comparingDouble(entity -> entity.distanceToSqr(projectile))).orElse(null);
   }

   private static LivingEntity cachedProjectileTarget(Projectile projectile) {
      if (!(projectile.level() instanceof net.minecraft.server.level.ServerLevel level)
         || !projectile.getPersistentData().hasUUID(PROJECTILE_TARGET)) return null;
      Entity entity = level.getEntity(projectile.getPersistentData().getUUID(PROJECTILE_TARGET));
      return entity instanceof LivingEntity living ? living : null;
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
         && selected.releaseMode() == RuneReleaseMode.RUNE_STONE
         && inscription(held) == null;
      if (engravingRequest) {
         inscribe(player, held, selected);
         event.setCanceled(true); event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS); return;
      }
      RuneInscriptionData stored = inscription(held);
      if (stored != null && (stored.medium() == RuneReleaseMode.RUNE_STONE || stored.medium() == RuneReleaseMode.TOOL)) {
         if (triggerInscribed(player, held, stored.medium(), stored.medium() == RuneReleaseMode.TOOL ? null : player)) {
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
            && triggerInscribed(player, held, stored.medium(), stored.medium() == RuneReleaseMode.TOOL ? null : player)) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
         }
      }
   }

   @SubscribeEvent
   public static void onLeftClickBlock(LeftClickBlock event) {
      if (!(event.getEntity() instanceof ServerPlayer player)) return;
      triggerInscribed(player, event.getItemStack(), RuneReleaseMode.TOOL, null);
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
                  if (player.getRandom().nextInt(8) == 0) RuneEffectDispatcher.emitPassiveGlyph(player, reinforcement);
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

   /** Handles the shared C release key: engrave the held medium before normal casting. */
   public static boolean tryInscriptionKey(ServerPlayer player) {
      if (player == null) return false;
      RuneProgram selected = current(player);
      if (selected == null || selected.releaseMode() == RuneReleaseMode.DIRECT_AIR
         || selected.releaseMode() == RuneReleaseMode.BLOCK_TRAP || selected.releaseMode() == RuneReleaseMode.BODY
         || selected.releaseMode() == RuneReleaseMode.RUNE_STONE) return false;
      ItemStack held = player.getMainHandItem();
      return mediumMatches(held, selected.releaseMode()) && inscription(held) == null && inscribeAndReport(player, held, selected);
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
         case WEAPON -> stack.getItem() instanceof ProjectileWeaponItem || stack.getItem() instanceof ProjectileItem
            || stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem || stack.getItem() instanceof TridentItem
            || stack.getAttributeModifiers().modifiers().stream().anyMatch(e -> e.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
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

   private static boolean inscribeAndReport(ServerPlayer player, ItemStack stack, RuneProgram program) {
      inscribe(player, stack, program);
      return true;
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
      if (data.medium() == RuneReleaseMode.RUNE_STONE) {
         int remaining = data.remainingUses() - 1;
         stack.set(ModDataComponents.RUNE_INSCRIPTION.get(), new RuneInscriptionData(data.programId(), data.snapshot(), remaining, data.medium(), remaining <= 0 ? "spent" : "ready").toComponent());
      }
      return true;
   }
}
