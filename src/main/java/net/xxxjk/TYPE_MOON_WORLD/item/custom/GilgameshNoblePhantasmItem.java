package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.GilgameshNoblePhantasmRenderer;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshEaBeamEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshGateWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Marker item for treasures exposed from Gilgamesh's vault. */
public class GilgameshNoblePhantasmItem extends Item implements NoblePhantasmItem, GeoItem {
   private static final int BAB_ILU_LONG_PRESS_TICKS = 20;
   private static final int EA_TREE_TICKS = 72;
   private static final String[] VAULT_WEAPONS = {"durandal", "gram", "harpe", "vajra", "fangtian_huaji"};
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private final String modelId;

   public GilgameshNoblePhantasmItem(Properties properties, String modelId) {
      super(properties);
      this.modelId = modelId;
   }

   public String modelId() { return this.modelId; }

   @Override
   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
      if ("durandal".equals(this.modelId) && stack.getDamageValue() != 0) {
         stack.setDamageValue(0);
      }
      super.inventoryTick(stack, level, entity, slotId, isSelected);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (player.getCooldowns().isOnCooldown(this)) {
         return InteractionResultHolder.fail(stack);
      }
      if ("babili".equals(this.modelId) || "ea".equals(this.modelId)) {
         if ("ea".equals(this.modelId) && player instanceof ServerPlayer serverPlayer) {
            AbilityStats eaStats = stats();
            if (!consumeMana(serverPlayer, eaStats.mana())) {
               serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
               return InteractionResultHolder.fail(stack);
            }
         }
         player.startUsingItem(hand);
         if (player instanceof ServerPlayer serverPlayer && serverPlayer.level() instanceof ServerLevel serverLevel) {
         }
         return InteractionResultHolder.consume(stack);
      }
      if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.consume(stack);

      AbilityStats stats = stats();
      if (!consumeMana(serverPlayer, stats.mana())) {
         serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
         return InteractionResultHolder.fail(stack);
      }

      boolean cast = switch (this.modelId) {
         case "durandal", "gram", "harpe", "vajra", "fangtian_huaji" -> castWeapon(serverPlayer, this.modelId, stats.damage());
         default -> false;
      };
      if (!cast) {
         refundMana(serverPlayer, stats.mana());
         return InteractionResultHolder.fail(stack);
      }

      serverPlayer.getCooldowns().addCooldown(this, stats.cooldown());
      serverPlayer.swing(hand, true);
      return InteractionResultHolder.consume(stack);
   }

   @Override
   public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
      if (!(living instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) return;
      int heldTicks = getUseDuration(stack, living) - remainingUseDuration;
      if ("ea".equals(this.modelId) && heldTicks == 1 && findEaEntity(serverLevel, player) == null) {
         GilgameshEaBeamEntity controller = new GilgameshEaBeamEntity(serverLevel, player, player.getLookAngle());
         serverLevel.addFreshEntity(controller);
      }
      super.onUseTick(level, living, stack, remainingUseDuration);
   }

   @Override
   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return ("babili".equals(this.modelId) || "ea".equals(this.modelId)) ? 72000 : super.getUseDuration(stack, entity);
   }

   @Override
   public UseAnim getUseAnimation(ItemStack stack) {
      return ("babili".equals(this.modelId) || "ea".equals(this.modelId)) ? UseAnim.BOW : super.getUseAnimation(stack);
   }

   @Override
   public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
      if (!(living instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) return;
      int heldTicks = this.getUseDuration(stack, living) - timeLeft;
      if ("babili".equals(this.modelId)) {
         if (heldTicks < BAB_ILU_LONG_PRESS_TICKS) {
            AbilityStats gateStats = stats();
            if (!consumeMana(player, gateStats.mana())) {
               player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
               return;
            }
            if (castBabIlu(player)) player.getCooldowns().addCooldown(this, gateStats.cooldown());
            else refundMana(player, gateStats.mana());
         } else {
            Vec3 direction = player.getLookAngle().normalize();
            player.getCooldowns().addCooldown(this, 400);
            VFXServerEffects.spawn(serverLevel, "gilgamesh_ea_tree", player.position().add(0, player.getBbHeight() * 0.65, 0), 192.0);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.PLAYERS, 1.6F, 0.72F);
            TYPE_MOON_WORLD.queueServerWork(EA_TREE_TICKS, () -> {
               if (!player.isAlive() || player.level() != serverLevel) return;
               giveEaIfMissing(player);
               serverLevel.playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 1.5F, 0.62F);
            });
         }
         return;
      }
      GilgameshEaBeamEntity ea = findEaEntity(serverLevel, player);
      if (ea != null) {
         ea.requestRelease(heldTicks);
         player.getCooldowns().addCooldown(this, stats().cooldown());
         return;
      }
      return;
   }

   private boolean castBabIlu(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      Vec3 look = player.getLookAngle().normalize();
      Vec3 right = look.cross(new Vec3(0, 1, 0));
      if (right.lengthSqr() < 1.0E-4) right = new Vec3(1, 0, 0);
      right = right.normalize();
      Vec3 up = right.cross(look).normalize();
      LivingEntity target = findLookTarget(player, 32.0, 4.0);
      Vec3 targetPoint = target == null ? player.getEyePosition().add(look.scale(30.0)) : target.getEyePosition();

      for (int i = 0; i < 18; i++) {
         int row = i / 6;
         int column = i % 6;
         double horizontal = (column - 2.5) * 1.35;
         double vertical = 1.2 + row * 1.35;
         Vec3 start = player.position().add(0, player.getBbHeight() * 0.55, 0)
            .add(right.scale(horizontal)).add(up.scale(vertical)).subtract(look.scale(1.5 + row * 0.25));
         Vec3 aim = targetPoint.add(right.scale((level.random.nextDouble() - 0.5) * 1.1))
            .add(up.scale((level.random.nextDouble() - 0.5) * 0.8)).subtract(start).normalize();
         String weapon = VAULT_WEAPONS[i % VAULT_WEAPONS.length];
         GilgameshGateWeaponProjectileEntity projectile = new GilgameshGateWeaponProjectileEntity(level, player, start, aim, weapon, 18.0F);
         projectile.setLaunchDelay(24 + (i / 6) * 8);
         level.addFreshEntity(projectile);
      }
      level.playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 1.2F, 1.35F);
      return true;
   }

   public static void giveEaIfMissing(ServerPlayer player) {
      if (player.getOffhandItem().is(ModItems.GILGAMESH_EA.get())) return;
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack existing = player.getInventory().getItem(slot);
         if (!existing.is(ModItems.GILGAMESH_EA.get())) continue;
         if (player.getOffhandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.OFF_HAND, existing.copy());
            player.getInventory().setItem(slot, ItemStack.EMPTY);
         }
         return;
      }
      ItemStack ea = new ItemStack(ModItems.GILGAMESH_EA.get());
      if (player.getOffhandItem().isEmpty()) {
         player.setItemInHand(InteractionHand.OFF_HAND, ea);
      } else if (!player.getInventory().add(ea)) {
         player.drop(ea, false);
      }
   }

   private boolean castWeapon(ServerPlayer player, String weapon, float damage) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      Vec3 direction = EntityUtils.getAutoAimDirection(player, 48.0, 18.0);
      if (!"vajra".equals(weapon)) {
         scheduleCloseRangeStrike(player, weapon, direction);
         return true;
      }
      Vec3 start = player.getEyePosition().add(direction.scale(0.8));
      GilgameshGateWeaponProjectileEntity projectile = new GilgameshGateWeaponProjectileEntity(level, player, start, direction, weapon, damage);
      projectile.setEmpowered(true); projectile.setLaunchDelay(24); level.addFreshEntity(projectile);
      level.playSound(null, player.blockPosition(), launchSound(weapon), SoundSource.PLAYERS, 1.5F, launchPitch(weapon));
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, start.x, start.y, start.z, 48, 0.45, 0.45, 0.45, 0.18);
      return true;
   }

   private static void scheduleCloseRangeStrike(ServerPlayer player, String weapon, Vec3 direction) {
      ServerLevel level = (ServerLevel)player.level();
      VFXServerEffects.spawnOriented(level, "gilgamesh_" + weapon + "_strike", player.getEyePosition(), direction, 96.0);
      TYPE_MOON_WORLD.queueServerWork(5, () -> {
         if (!player.isAlive() || player.level() != level) return;
         Vec3 origin = player.getEyePosition();
         Vec3 forward = direction.normalize();
         if ("fangtian_huaji".equals(weapon)) {
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(10), e -> e.isAlive() && e != player && !e.isAlliedTo(player) && !EntityUtils.isImmunePlayerTarget(e))) {
               Vec3 delta = target.position().subtract(player.position()); double distance = delta.length();
               if (distance > 10 || delta.lengthSqr() < 1.0E-6 || forward.dot(delta.normalize()) < 0.5) continue;
               float amount = (float)(100.0 - 50.0 * distance / 10.0);
               target.invulnerableTime = 0; target.hurt(player.damageSources().mobAttack(player), amount); target.invulnerableTime = 0;
               Vec3 push = delta.normalize().scale(2.2); target.push(push.x, .5, push.z);
            }
            return;
         }
         Vec3 right = Math.abs(forward.y) > 0.9 ? new Vec3(1, 0, 0) : forward.cross(new Vec3(0, 1, 0)).normalize();
         Vec3 up = right.cross(forward).normalize();
         double length = switch (weapon) { case "gram" -> 12; case "durandal" -> 10; case "harpe" -> 8; default -> 10; };
         double halfWidth = switch (weapon) { case "gram" -> 3.5; case "durandal" -> 3; default -> 3; };
         double halfHeight = 2.0;
         AABB box = new AABB(origin, origin.add(forward.scale(length))).inflate(halfWidth, halfHeight, halfWidth);
         for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player && !e.isAlliedTo(player) && !EntityUtils.isImmunePlayerTarget(e))) {
            Vec3 rel = target.position().add(0, target.getBbHeight() * .5, 0).subtract(origin);
            if (rel.dot(forward) < 0 || rel.dot(forward) > length || Math.abs(rel.dot(right)) > halfWidth || Math.abs(rel.dot(up)) > halfHeight) continue;
            float amount = switch (weapon) {
               case "durandal" -> 120.0F;
               case "gram" -> ServantIdentityHelper.hasTrait(target, ServantTraitTag.DRAGON) ? 200.0F : 130.0F;
               case "harpe" -> ServantIdentityHelper.hasTrait(target, ServantTraitTag.UNDEAD) ? 120.0F : 80.0F;
               default -> 100.0F;
            };
            target.invulnerableTime = 0; target.hurt(player.damageSources().mobAttack(player), amount); target.invulnerableTime = 0;
            if ("durandal".equals(weapon)) { target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 80, 1)); target.hurt(player.damageSources().magic(), 30.0F); }
            if ("gram".equals(weapon)) target.igniteForSeconds(6.0F);
            if ("harpe".equals(weapon)) { target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 200, 0)); target.getPersistentData().putLong("HarpeCursedWoundUntil", level.getGameTime() + 200); }
         }
      });
   }

   private static GilgameshEaBeamEntity findEaEntity(ServerLevel level, ServerPlayer player) {
      return level.getEntitiesOfClass(GilgameshEaBeamEntity.class, player.getBoundingBox().inflate(2.0), e -> e.isAlive() && e.isOwnedBy(player)).stream().findFirst().orElse(null);
   }

   public static void castBabIluPublic(ServerPlayer player) { new GilgameshNoblePhantasmItem(new Item.Properties(), "babili").castBabIlu(player); }

   private static LivingEntity findLookTarget(ServerPlayer player, double range, double inflate) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle().normalize();
      Vec3 end = eye.add(look.scale(range));
      AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(inflate);
      EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
         entity -> entity instanceof LivingEntity living && living.isAlive() && entity != player
            && !player.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(entity), range * range);
      return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
   }

   private AbilityStats stats() {
      return switch (this.modelId) {
         case "babili" -> new AbilityStats(18.0, 160, 18.0F);
         case "ea" -> new AbilityStats(1000.0, 2400, 5000.0F);
         case "durandal" -> new AbilityStats(16.0, 240, 120.0F);
         case "gram" -> new AbilityStats(18.0, 280, 130.0F);
         case "harpe" -> new AbilityStats(14.0, 220, 80.0F);
         case "vajra" -> new AbilityStats(24.0, 360, 40.0F);
         case "fangtian_huaji" -> new AbilityStats(15.0, 200, 100.0F);
         default -> new AbilityStats(0.0, 20, 0.0F);
      };
   }

   private static boolean consumeMana(ServerPlayer player, double amount) {
      if (player.getAbilities().instabuild || amount <= 0.0) return true;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.servant_card_transformed) {
         return ServantCardManaService.consumeNoblePhantasm(player, vars, amount);
      }
      if (vars.player_mana < amount) return false;
      vars.player_mana -= amount;
      vars.syncMana(player);
      return true;
   }

   private static void refundMana(ServerPlayer player, double amount) {
      if (player.getAbilities().instabuild || amount <= 0.0) return;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.servant_card_transformed) {
         vars.servant_card_mana = Math.min(vars.servant_card_max_mana, vars.servant_card_mana + amount);
      } else {
         vars.player_mana = Math.min(vars.player_max_mana, vars.player_mana + amount);
      }
      vars.syncMana(player);
   }

   private static net.minecraft.sounds.SoundEvent launchSound(String weapon) {
      return switch (weapon) {
         case "gram" -> SoundEvents.FIRECHARGE_USE;
         case "vajra" -> SoundEvents.LIGHTNING_BOLT_THUNDER;
         case "fangtian_huaji" -> SoundEvents.TRIDENT_THROW.value();
         default -> SoundEvents.PLAYER_ATTACK_SWEEP;
      };
   }

   private static float launchPitch(String weapon) {
      return switch (weapon) {
         case "gram" -> 0.72F;
         case "vajra" -> 1.35F;
         case "harpe" -> 1.5F;
         default -> 1.0F;
      };
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.add(Component.translatable("item.typemoonworld.gilgamesh_" + localizationId() + ".desc").withStyle(ChatFormatting.GOLD));
   }

   private String localizationId() {
      return switch (this.modelId) {
         case "babili" -> "bab_ilu";
         case "fangtian_huaji" -> "fangtian_huaji";
         default -> this.modelId;
      };
   }

   private record AbilityStats(double mana, int cooldown, float damage) { }

   @Override public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private GilgameshNoblePhantasmRenderer renderer;
         @Override public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (renderer == null) renderer = new GilgameshNoblePhantasmRenderer();
            return renderer;
         }
      });
   }

   @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      if ("ea".equals(this.modelId)) controllers.add(new AnimationController<>(this, "ea_spin", 0, this::eaAnimation));
   }
   private PlayState eaAnimation(AnimationState<GilgameshNoblePhantasmItem> state) {
      Entity entity = state.getData(DataTickets.ENTITY);
      boolean active = entity instanceof LivingEntity living && GilgameshEaBeamEntity.isEaActiveFor(living);
      state.getController().setAnimation(RawAnimation.begin().thenLoop(active ? "xuanzhuan2" : "idle"));
      return PlayState.CONTINUE;
   }
   @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
