package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshCrossSlashEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshGateWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ChainsOfHeavenBindingEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GilgameshNoblePhantasmItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.EnkiduDetectionHighlightMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.OpenGilgameshVaultScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.GilgameshDivineShield;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceRank;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EnkiduCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

public final class ServantCardGilgameshSkills {
   private static final String KEY = "ServantCardGilgameshKey";
   private static final String MASK = "ServantCardGilgameshVaultMask";
   private static final String GENERATED = "ServantCardGilgameshGenerated";
   private static final String SINGLE_COOLDOWN = "ServantCardGilgameshSingleVaultCooldown";
   private static final String MELEE_COOLDOWN = "ServantCardGilgameshMeleeCooldown";
   private static final String CHAIN_TARGET = "ServantCardGilgameshChainTarget";
   private static final String CHAIN_UNTIL = "ServantCardGilgameshChainUntil";
   private static final String CHARISMA_UNTIL = "ServantCardGilgameshCharismaUntil";
   private static final ResourceLocation CHARISMA_KNOCKBACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "gilgamesh_charisma_knockback");
   private static final String[] WEAPONS = {"gae_bulg", "pseudo_spiral_sword", "fangtian_huaji", "gram", "durandal", "vajra", "harpe"};

   private ServantCardGilgameshSkills() {}

   public static void reset(ServerPlayer player) {
      player.getPersistentData().remove(KEY);
      GilgameshDivineShield.clear(player);
      player.getPersistentData().putInt(MASK, 0);
      player.getPersistentData().remove(SINGLE_COOLDOWN);
      player.getPersistentData().remove(MELEE_COOLDOWN);
      player.getPersistentData().remove(CHAIN_TARGET);
      player.getPersistentData().remove(CHAIN_UNTIL);
      player.getPersistentData().remove(CHARISMA_UNTIL);
      player.getPersistentData().remove("ClairvoyanceActive");
   }

   public static void clear(ServerPlayer player) {
      if (player.level() instanceof ServerLevel level) {
         for (GilgameshCrossSlashEntity slash : level.getEntitiesOfClass(GilgameshCrossSlashEntity.class,
            player.getBoundingBox().inflate(GilgameshCrossSlashEntity.SLASH_LENGTH + 64.0), e -> e.isOwnedBy(player))) {
            slash.discard();
         }
      }
      player.getPersistentData().remove(KEY);
      GilgameshDivineShield.clear(player);
      player.removeEffect(MobEffects.NIGHT_VISION);
      MagicResistanceHelper.setMagicResistance(player, MagicResistanceRank.NONE, 0.0F, 0.0F);
      player.getPersistentData().remove("ClairvoyanceActive");
      if (player.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null) player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).removeModifier(CHARISMA_KNOCKBACK_ID);
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) if (isGenerated(player.getInventory().getItem(i))) player.getInventory().setItem(i, ItemStack.EMPTY);
      if (isGenerated(player.getMainHandItem())) player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      if (isGenerated(player.getOffhandItem())) player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!vars.servant_card_transformed || !"gilgamesh".equals(vars.servant_card_id)) return;
      MobEffectInstance nightVision = player.getEffect(MobEffects.NIGHT_VISION);
      if (nightVision == null || nightVision.getDuration() < 220) player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false, false));
      MagicResistanceHelper.setMagicResistance(player, MagicResistanceRank.A, MagicResistanceHelper.damageReductionForRank(MagicResistanceRank.A), 0.75F);
      long now = player.level().getGameTime();
      GilgameshDivineShield.tick(player);
      syncDivineShieldCooldown(player, vars);
      if (now >= player.getPersistentData().getLong(CHARISMA_UNTIL) && player.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null) {
         player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).removeModifier(CHARISMA_KNOCKBACK_ID);
      }
      tickChainPursuit(player, now);
   }

   public static void syncDivineShieldCooldown(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      syncDivineShieldCooldown(player, vars, divineShieldSlot(vars.servant_card_id));
   }

   public static void syncDivineShieldCooldown(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, int slot) {
      String oldCooldowns = vars.servant_card_skill_cooldowns;
      String oldEnds = vars.servant_card_skill_cooldown_ends;
      ServantCardTransformManager.setSkillCooldownUntil(player, vars, slot, GilgameshDivineShield.cooldownUntil(player));
      if (!java.util.Objects.equals(oldCooldowns, vars.servant_card_skill_cooldowns)
         || !java.util.Objects.equals(oldEnds, vars.servant_card_skill_cooldown_ends)) {
         vars.syncPlayerVariables(player);
      }
   }

   private static int divineShieldSlot(String servantId) {
      return "gilgamesh_caster".equals(servantId) ? 8 : 5;
   }

   public static boolean isVaultAction(String id) {
      return id != null && (id.startsWith("gilgamesh_") && !id.equals("gilgamesh_key") && !id.equals("gilgamesh_melee") && !id.equals("gilgamesh_elixir") && !id.equals("gilgamesh_divine_shield") && !id.equals("gilgamesh_clairvoyance") && !id.equals("gilgamesh_charisma"));
   }

   public static boolean hasKey(ServerPlayer player) {
      if (player.getMainHandItem().is(ModItems.GILGAMESH_BAB_ILU.get()) || player.getOffhandItem().is(ModItems.GILGAMESH_BAB_ILU.get())) return true;
      for (ItemStack stack : player.getInventory().items) if (stack.is(ModItems.GILGAMESH_BAB_ILU.get())) return true;
      return false;
   }

   public static boolean performKey(ServerPlayer player) {
      if (!hasKey(player)) {
         ItemStack key = markGilgameshGenerated(ServantCardTransformManager.markGeneratedItem(new ItemStack(ModItems.GILGAMESH_BAB_ILU.get()), true, false));
         player.setItemInHand(InteractionHand.MAIN_HAND, key);
         player.getPersistentData().putBoolean(KEY, true);
      }
      player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.typemoonworld.servant_card.gilgamesh_key_ready"), true);
      PacketDistributor.sendToPlayer(player, new OpenGilgameshVaultScreenMessage(player.getPersistentData().getInt(MASK)), new CustomPacketPayload[0]);
      return true;
   }

   public static boolean selectTreasure(ServerPlayer player, int index) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || !isGilgameshVaultUser(vars.servant_card_id) || !hasKey(player) || index < 0 || index >= 7) return false;
      int mask = player.getPersistentData().getInt(MASK);
      if ((mask & (1 << index)) != 0) return false;
      ItemStack treasure = markGilgameshGenerated(ServantCardTransformManager.markGeneratedItem(treasureFor(index), true, false));
      ItemStack main = player.getMainHandItem();
      if (main.is(ModItems.GILGAMESH_BAB_ILU.get()) && !isGenerated(main)) {
         if (!player.getInventory().add(main.copy())) return false;
      } else if (!main.isEmpty() && !player.getInventory().add(main.copy())) {
         return false;
      }
      player.setItemInHand(InteractionHand.MAIN_HAND, treasure);
      player.getPersistentData().putInt(MASK, mask | (1 << index));
      player.inventoryMenu.broadcastChanges();
      return true;
   }

   private static boolean isGilgameshVaultUser(String servantId) {
      return "gilgamesh".equals(servantId) || "gilgamesh_caster".equals(servantId);
   }

   public static ItemStack treasureFor(int index) {
      return switch (index) {
         case 0 -> new ItemStack(ModItems.GILGAMESH_GAE_BULG.get());
         case 1 -> new ItemStack(ModItems.GILGAMESH_SPIRAL_SWORD.get());
         case 2 -> new ItemStack(ModItems.GILGAMESH_FANGTIAN_HUAJI.get());
         case 3 -> new ItemStack(ModItems.GILGAMESH_GRAM.get());
         case 4 -> new ItemStack(ModItems.GILGAMESH_DURANDAL.get());
         case 5 -> new ItemStack(ModItems.GILGAMESH_VAJRA.get());
         case 6 -> new ItemStack(ModItems.GILGAMESH_HARPE.get());
         default -> ItemStack.EMPTY;
      };
   }

   private static ItemStack markGilgameshGenerated(ItemStack stack) {
      CustomData data = stack.get(DataComponents.CUSTOM_DATA);
      CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
      tag.putBoolean(GENERATED, true);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      return stack;
   }

   private static boolean isGenerated(ItemStack stack) {
      CustomData data = stack.get(DataComponents.CUSTOM_DATA);
      return data != null && data.copyTag().getBoolean(GENERATED);
   }

   public static boolean performMelee(ServerPlayer player) {
      long now = player.level().getGameTime();
      if (!ServantCardUnlimitedMode.isEnabled(player) && now < player.getPersistentData().getLong(MELEE_COOLDOWN)) return false;
      if (!ServantCardUnlimitedMode.isEnabled(player)) player.getPersistentData().putLong(MELEE_COOLDOWN, now + 18L);
      hitForwardArc(player, player.getLookAngle(), 4.5, 28.0F);
      ServantCardVoiceHelper.tryPlayAttack(player);
      return true;
   }

   public static void performChains(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return;
      LivingEntity target = ServantCardSkillUtils.findLookTarget(player, 30.0, 3.0);
      if (target == null) return;
      int duration = 120;
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 6, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 2, false, true, true));
      level.addFreshEntity(new ChainsOfHeavenBindingEntity(level, player, target, duration + 6, true));
      player.getPersistentData().putString(CHAIN_TARGET, target.getUUID().toString());
      player.getPersistentData().putLong(CHAIN_UNTIL, level.getGameTime() + duration);
   }

   public static void performVault(ServerPlayer player, boolean grand) {
      castProjectiles(player, grand ? 48 : 18, grand ? 42 : 26, grand ? 42.0F : 22.0F);
   }

   public static boolean performSingleVault(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      long now = player.level().getGameTime();
      if (!hasKey(player) || !ServantCardUnlimitedMode.isEnabled(player) && now < player.getPersistentData().getLong(SINGLE_COOLDOWN)) return false;
      if (!ServantCardManaService.consume(player, vars, 12.0)) return false;
      if (!ServantCardUnlimitedMode.isEnabled(player)) player.getPersistentData().putLong(SINGLE_COOLDOWN, now + 80L);
      if (!(player.level() instanceof ServerLevel level)) return false;
      Vec3 direction = EntityUtils.getAutoAimDirection(player, 48.0, 18.0);
      GilgameshGateWeaponProjectileEntity p = new GilgameshGateWeaponProjectileEntity(level, player, player.getEyePosition().add(direction.scale(.8)), direction, WEAPONS[player.getRandom().nextInt(WEAPONS.length)], 24.0F);
      p.setLaunchDelay(8); level.addFreshEntity(p); return true;
   }

   public static void performRingVault(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return;
      LivingEntity target = ServantCardSkillUtils.findLookTarget(player, 30.0, 3.0);
      if (target == null) return;
      Vec3 center = target.position().add(0.0, target.getBbHeight() * .5, 0.0);
      int count = 36;
      double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));
      for (int i = 0; i < count; i++) {
         double y = 1.0 - 2.0 * (i + .5) / count;
         double radius = Math.sqrt(Math.max(0.0, 1.0 - y * y));
         double angle = i * goldenAngle;
         Vec3 start = center.add(Math.cos(angle) * radius * 8.0, y * 8.0, Math.sin(angle) * radius * 8.0);
         Vec3 aim = center.subtract(start).normalize();
         GilgameshGateWeaponProjectileEntity p = new GilgameshGateWeaponProjectileEntity(level, player, start, aim, WEAPONS[i % WEAPONS.length], 35.0F);
         p.setHomingTarget(target);
         p.setEffectStride(3);
         p.setLaunchDelay(16); level.addFreshEntity(p);
      }
   }

   public static void performElixir(ServerPlayer player) {
      ServantCardSkillUtils.clearHarmfulEffects(player);
      player.clearFire();
   }

   public static boolean performDivineShield(ServerPlayer player) {
      if (GilgameshDivineShield.isActive(player)) {
         return GilgameshDivineShield.deactivate(player);
      }
      return GilgameshDivineShield.activate(player);
   }

   public static boolean performDivineShield(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      boolean result = performDivineShield(player);
      if (result) {
         syncDivineShieldCooldown(player, vars);
      }
      return result;
   }

   public static void performClairvoyance(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return;
      List<Integer> ids = new ArrayList<>();
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         player.getBoundingBox().inflate(100),
         e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e) && !player.isAlliedTo(e) && !e.isAlliedTo(player)
      )) {
         living.removeEffect(MobEffects.INVISIBILITY);
         living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false, false));
         ids.add(living.getId());
      }
      PacketDistributor.sendToPlayer(player, new EnkiduDetectionHighlightMessage(ids, 200), new CustomPacketPayload[0]);
   }

   public static void performCharisma(ServerPlayer player) {
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 360, 1, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 360, 1, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 360, 0, false, true, true));
      AttributeInstance knockback = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
      if (knockback != null) {
         knockback.removeModifier(CHARISMA_KNOCKBACK_ID);
         knockback.addTransientModifier(new AttributeModifier(CHARISMA_KNOCKBACK_ID, 0.35, AttributeModifier.Operation.ADD_VALUE));
      }
      player.getPersistentData().putLong(CHARISMA_UNTIL, player.level().getGameTime() + 360L);
      if (player.level() instanceof ServerLevel level) {
         for (ServerPlayer ally : level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(16), p -> p != player && !p.isSpectator())) {
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 240, 0, false, true, true));
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 240, 0, false, true, true));
         }
      }
   }

   public static void performLaughVault(ServerPlayer player) {
      if (player.level() instanceof ServerLevel level) level.playSound(null, player.blockPosition(), ModSounds.GILGAMESH_VOICE_MONGREL.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
      castProjectiles(player, 48, 128.0, 48.0F);
      for (int round = 1; round < 4; round++) {
         int delay = round * 16;
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            if (player.isAlive() && vars.servant_card_transformed && "gilgamesh".equals(vars.servant_card_id) && hasKey(player)) {
               castProjectiles(player, 48, 128.0, 48.0F);
            }
         });
      }
   }

   public static void performCrossSlash(ServerPlayer player) {
      if (player.level() instanceof ServerLevel level) GilgameshCrossSlashEntity.spawnPair(level, player, player.getLookAngle(), player);
   }

   private static void castProjectiles(ServerPlayer player, int count, double spread, float damage) {
      if (!(player.level() instanceof ServerLevel level)) return;
      Vec3 look = player.getLookAngle().normalize();
      Vec3 right = look.cross(new Vec3(0, 1, 0)); if (right.lengthSqr() < 0.01) right = new Vec3(1, 0, 0); right = right.normalize();
      Vec3 up = right.cross(look).normalize(); LivingEntity target = ServantCardSkillUtils.findLookTarget(player, 40, 3);
      Vec3 point = target == null ? player.getEyePosition().add(look.scale(30)) : target.getEyePosition();
      int effectStride = count >= 48 ? 4 : count >= 18 ? 2 : 1;
      for (int i = 0; i < count; i++) { double x = (level.random.nextDouble() - .5) * spread; double y = (level.random.nextDouble() - .5) * spread * .5; Vec3 start = player.getEyePosition().add(right.scale(x)).add(up.scale(y)); Vec3 aim = point.subtract(start).normalize(); GilgameshGateWeaponProjectileEntity p = new GilgameshGateWeaponProjectileEntity(level, player, start, aim, WEAPONS[i % WEAPONS.length], damage); p.setEffectStride(effectStride); p.setLaunchDelay(8 + i % 8 * 3); level.addFreshEntity(p); }
      level.playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 1.2F, 1.25F);
   }

   private static void hitForwardArc(ServerPlayer player, Vec3 dir, double range, float damage) { ServantCardSkillUtils.hitForwardArc(player, dir, range, damage); }

   private static void tickChainPursuit(ServerPlayer player, long now) {
      if (!(player.level() instanceof ServerLevel level) || now >= player.getPersistentData().getLong(CHAIN_UNTIL) || now % 20 != 0) return;
      String raw = player.getPersistentData().getString(CHAIN_TARGET);
      try {
         net.minecraft.world.entity.Entity entity = level.getEntity(java.util.UUID.fromString(raw));
         if (!(entity instanceof LivingEntity target) || !target.isAlive()) return;
         if (ServantMasterTargeting.isContractMaster(player, target)) {
            player.getPersistentData().remove(CHAIN_TARGET);
            return;
         }
         target.invulnerableTime = 0; target.hurt(player.damageSources().magic(), 8.0F); target.invulnerableTime = 0;
         for (int i = 0; i < 3; i++) {
            Vec3 start = player.getEyePosition().add((i - 1) * 1.4, 1.0 + i * .4, 0);
            GilgameshGateWeaponProjectileEntity p = new GilgameshGateWeaponProjectileEntity(level, player, start, target.getEyePosition().subtract(start).normalize(), WEAPONS[(player.tickCount + i) % WEAPONS.length], 18.0F);
            p.setLaunchDelay(4 + i * 3); p.setHomingTarget(target); level.addFreshEntity(p);
         }
      } catch (IllegalArgumentException ignored) { }
   }

}
