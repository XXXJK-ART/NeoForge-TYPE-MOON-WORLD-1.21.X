package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.entity.RhoAiasEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWInterceptorSwordEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ChainsOfHeavenBindingEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.DragonfangSoldierEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.EnkiduEarthWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaMagicBoltEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockBulletEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RedSkeletonHajunEntity;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModParticles;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.ChantHandler;
import net.xxxjk.TYPE_MOON_WORLD.network.EnkiduDetectionHighlightMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.OpenEnkiduTransfigurationScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatFormulas;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CursedArmHassanCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesGodHandHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.DeferredTerrainDestruction;

import static net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillUtils.*;

public final class ServantCardEnkiduSkills {
   private static final String BOUND_UNTIL = "ServantCardEnkiduBoundUntil";
   private static final String BOUND_OWNER = "ServantCardEnkiduBoundOwner";
   private static final String BOUND_X = "ServantCardEnkiduBoundX";
   private static final String BOUND_Y = "ServantCardEnkiduBoundY";
   private static final String BOUND_Z = "ServantCardEnkiduBoundZ";
   private static final String BOUND_PREV_NO_AI = "ServantCardEnkiduBoundPrevNoAi";
   private static final String TAG_LAST_PASSIVE_RESOURCE = "ServantCardEnkiduLastPassiveResource";
   private static final String TAG_LAST_PROJECTILE_INTERCEPT = "ServantCardEnkiduLastProjectileIntercept";
   private static final String ENUMA_ACTIVE = "ServantCardEnkiduEnumaActive";
   private static final String ENUMA_RELEASE_TICK = "ServantCardEnkiduEnumaReleaseTick";
   private static final String ENUMA_FINISH_TICK = "ServantCardEnkiduEnumaFinishTick";
   private static final String ENUMA_STAGE = "ServantCardEnkiduEnumaStage";
   private static final String ENUMA_STAGE_START = "ServantCardEnkiduEnumaStageStart";
   private static final String ENUMA_PREV_INVULNERABLE = "ServantCardEnkiduEnumaPrevInvulnerable";
   private static final String ENUMA_INVISIBLE = "ServantCardEnkiduEnumaInvisible";
   private static final String ENUMA_PREV_INVISIBLE = "ServantCardEnkiduEnumaPrevInvisible";
   private static final String ENUMA_LAST_FLIGHT_FX = "ServantCardEnkiduEnumaLastFlightFx";
   private static final String ENUMA_START_X = "ServantCardEnkiduEnumaStartX";
   private static final String ENUMA_START_Y = "ServantCardEnkiduEnumaStartY";
   private static final String ENUMA_START_Z = "ServantCardEnkiduEnumaStartZ";
   private static final ResourceLocation TRANSFIG_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_enkidu_transfig_attack");
   private static final ResourceLocation TRANSFIG_HEALTH_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_enkidu_transfig_health");
   private static final ResourceLocation TRANSFIG_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_enkidu_transfig_speed");
   private static final ResourceLocation TRANSFIG_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_enkidu_transfig_armor");
   private static final ResourceLocation TRANSFIG_TOUGHNESS_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_enkidu_transfig_toughness");
   private static final ResourceLocation TRANSFIG_JUMP_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_enkidu_transfig_jump");
   private static final ResourceLocation TRANSFIG_LUCK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_enkidu_transfig_luck");
   private static final Map<UUID, Set<UUID>> BOUND_TARGETS_BY_OWNER = new HashMap<>();
   private static final int ENUMA_WINDUP = 10 * 20;
   private static final int ENUMA_RELEASE_VISUAL = 5 * 20;
   private static final double ENUMA_GROUND_EXPLOSION_RADIUS = 60.0;
   private static final ItemStack[] AGE_WEAPONS = new ItemStack[]{
      new ItemStack(Items.IRON_SWORD), new ItemStack(Items.IRON_AXE), new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.TRIDENT),
      new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.DIAMOND_AXE), new ItemStack(Items.NETHERITE_SWORD), new ItemStack(Items.BOW), new ItemStack(Items.CROSSBOW)
   };

   private ServantCardEnkiduSkills() {
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!vars.servant_card_transformed || !"enkidu".equals(vars.servant_card_id) || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      tickBoundTargets(player, level);
      tickEnumaElishFlightState(player, level);
      if (player.isOnFire()) {
         player.clearFire();
      }
      tickPerfectFormPassive(player, vars, level);
      tickPassivePresence(player, level);
      interceptHostileProjectiles(player, level, 10.0);
   }

   public static void clear(ServerPlayer player) {
      clearTrackedBoundTargets(player);
      clearActiveEnumaState(player);
      clearTransfigurationAttributes(player);
   }

   public static void performEnkiduTransfiguration(ServerPlayer player) {
      PacketDistributor.sendToPlayer(player, new OpenEnkiduTransfigurationScreenMessage(), new CustomPacketPayload[0]);
   }

   public static void setTransfigurationPoints(ServerPlayer player, int[] requested) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || !"enkidu".equals(vars.servant_card_id)) {
         return;
      }
      int[] normalized = normalizeTransfigurationPoints(requested);
      vars.servant_card_enkidu_transfiguration_points = serializeTransfigurationPoints(normalized);
      applyTransfigurationAttributes(player, vars, normalized);
      vars.syncPlayerVariables(player);
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 42, 0.75, 0.65, 0.75, 0.06);
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 22, 0.45, 0.4, 0.45, 0.04);
         level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 1.1F);
      }
   }

   private static int[] normalizeTransfigurationPoints(int[] requested) {
      int[] points = new int[]{6, 6, 6, 6, 6};
      if (requested != null) {
         for (int i = 0; i < points.length && i < requested.length; i++) {
            points[i] = Mth.clamp(requested[i], 0, 30);
         }
      }
      int total = Arrays.stream(points).sum();
      while (total > 30) {
         for (int i = points.length - 1; i >= 0 && total > 30; i--) {
            if (points[i] > 0) {
               points[i]--;
               total--;
            }
         }
      }
      int index = 0;
      while (total < 30) {
         points[index++ % points.length]++;
         total++;
      }
      return points;
   }

   public static void applyCurrentTransfiguration(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      int[] points = parseTransfigurationPoints(vars);
      int[] normalized = normalizeTransfigurationPoints(points);
      vars.servant_card_enkidu_transfiguration_points = serializeTransfigurationPoints(normalized);
      applyTransfigurationAttributes(player, vars, normalized);
      vars.syncPlayerVariables(player);
   }

   public static void adjustTransfigurationPoint(ServerPlayer player, int stat, int delta) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || !"enkidu".equals(vars.servant_card_id) || stat < 0 || stat >= 5 || delta == 0) {
         return;
      }
      int[] points = parseTransfigurationPoints(vars);
      int next = Mth.clamp(points[stat] + (delta > 0 ? 1 : -1), 0, 30);
      if (next == points[stat]) {
         return;
      }
      int total = 0;
      for (int i = 0; i < points.length; i++) {
         total += i == stat ? next : points[i];
      }
      if (total > 30) {
         return;
      }
      points[stat] = next;
      vars.servant_card_enkidu_transfiguration_points = serializeTransfigurationPoints(points);
      applyTransfigurationAttributes(player, vars, points);
      vars.syncPlayerVariables(player);
   }

   public static void clearTransfigurationAttributes(ServerPlayer player) {
      removeModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), TRANSFIG_ATTACK_ID);
      removeModifier(player.getAttribute(Attributes.MAX_HEALTH), TRANSFIG_HEALTH_ID);
      removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), TRANSFIG_SPEED_ID);
      removeModifier(player.getAttribute(Attributes.ARMOR), TRANSFIG_ARMOR_ID);
      removeModifier(player.getAttribute(Attributes.ARMOR_TOUGHNESS), TRANSFIG_TOUGHNESS_ID);
      removeModifier(player.getAttribute(Attributes.JUMP_STRENGTH), TRANSFIG_JUMP_ID);
      removeModifier(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), TRANSFIG_LUCK_ID);
   }

   private static void applyTransfigurationAttributes(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, int[] points) {
      clearTransfigurationAttributes(player);
      int strength = points.length > 0 ? points[0] : 6;
      int endurance = points.length > 1 ? points[1] : 6;
      int agility = points.length > 2 ? points[2] : 6;
      int mana = points.length > 3 ? points[3] : 6;
      int luck = points.length > 4 ? points[4] : 6;
      ServantDefinition definition = ServantDataRegistry.get("enkidu");
      ServantParams base = definition != null ? definition.parameters() : ServantParams.of("B", false, "B", false, "B", false, "B", false, "B", false);
      double targetHealth = maxHealthFor(endurance);
      double targetAttack = attackFor(strength);
      double targetArmor = armorFor(endurance);
      double targetSpeed = speedFor(agility);
      addModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), TRANSFIG_ATTACK_ID, targetAttack - base.attackDamage(), AttributeModifier.Operation.ADD_VALUE);
      addModifier(player.getAttribute(Attributes.MAX_HEALTH), TRANSFIG_HEALTH_ID, targetHealth - base.maxHealth(), AttributeModifier.Operation.ADD_VALUE);
      addModifier(player.getAttribute(Attributes.ARMOR), TRANSFIG_ARMOR_ID, targetArmor - base.armor(), AttributeModifier.Operation.ADD_VALUE);
      addModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), TRANSFIG_SPEED_ID, targetSpeed - base.movementSpeed(), AttributeModifier.Operation.ADD_VALUE);
      addModifier(player.getAttribute(Attributes.ARMOR_TOUGHNESS), TRANSFIG_TOUGHNESS_ID, armorToughnessFor(targetArmor, targetAttack) - armorToughnessFor(base.armor(), base.attackDamage()), AttributeModifier.Operation.ADD_VALUE);
      addModifier(player.getAttribute(Attributes.JUMP_STRENGTH), TRANSFIG_JUMP_ID, jumpStrengthFor(targetSpeed) - jumpStrengthFor(base.movementSpeed()), AttributeModifier.Operation.ADD_VALUE);
      addModifier(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), TRANSFIG_LUCK_ID, Mth.clamp((coefficientFor(luck) - effectiveLuckCoefficient(base)) * 0.004, -0.16, 0.28), AttributeModifier.Operation.ADD_VALUE);
      double ratio = vars.servant_card_max_mana <= 0.0 ? 1.0 : vars.servant_card_mana / vars.servant_card_max_mana;
      vars.servant_card_max_mana = Math.max(50.0, manaPoolFor(mana));
      vars.servant_card_mana = Math.min(vars.servant_card_max_mana, Math.max(0.0, vars.servant_card_max_mana * ratio));
      if (player.getHealth() > player.getMaxHealth()) {
         player.setHealth(player.getMaxHealth());
      }
   }

   private static int coefficientFor(int points) {
      if (points < 3) {
         return 5;
      }
      if (points == 3) {
         return 10;
      }
      if (points == 4) {
         return 20;
      }
      if (points == 5) {
         return 30;
      }
      if (points == 6) {
         return 40;
      }
      if (points <= 8) {
         return 50;
      }
      return 100;
   }

   private static double maxHealthFor(int points) {
      return coefficientFor(points) * 10.0;
   }

   private static double attackFor(int points) {
      return coefficientFor(points) * 0.5;
   }

   private static double speedFor(int points) {
      return 0.16 + coefficientFor(points) * 0.004;
   }

   private static double armorFor(int points) {
      return coefficientFor(points) * 0.3;
   }

   private static double manaPoolFor(int points) {
      return coefficientFor(points) * 20.0;
   }

   private static double armorToughnessFor(double armor, double attack) {
      double endurance = Mth.clamp(armor / 15.0, 0.0, 1.2);
      double strength = Mth.clamp(attack / 25.0, 0.0, 1.2);
      return 2.0 + endurance * 5.5 + strength * 2.0;
   }

   private static double jumpStrengthFor(double speed) {
      double agility = Mth.clamp((speed - 0.16) / 0.20, 0.0, 1.0);
      return 0.30 + agility * 0.12;
   }

   private static int effectiveLuckCoefficient(ServantParams params) {
      return params.luckPlus() ? params.luck().plusCoefficient() : params.luck().coefficient();
   }

   private static void addModifier(AttributeInstance attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
      if (attribute == null) {
         return;
      }
      attribute.removeModifier(id);
      if (Math.abs(amount) > 1.0E-6) {
         attribute.addPermanentModifier(new AttributeModifier(id, amount, operation));
      }
   }

   private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null) {
         attribute.removeModifier(id);
      }
   }

   public static int[] parseTransfigurationPoints(TypeMoonWorldModVariables.PlayerVariables vars) {
      int[] points = new int[]{6, 6, 6, 6, 6};
      String raw = vars.servant_card_enkidu_transfiguration_points == null ? "" : vars.servant_card_enkidu_transfiguration_points;
      String[] parts = raw.split(",");
      for (int i = 0; i < points.length && i < parts.length; i++) {
         try {
            points[i] = Mth.clamp(Integer.parseInt(parts[i]), 0, 30);
         } catch (NumberFormatException ignored) {
            points[i] = 6;
         }
      }
      return points;
   }

   private static String serializeTransfigurationPoints(int[] points) {
      return points[0] + "," + points[1] + "," + points[2] + "," + points[3] + "," + points[4];
   }

   public static void performEnkiduPerfectForm(ServerPlayer player) {
      ServantCardSkillUtils.clearHarmfulEffects(player);
      player.heal(Math.max(18.0F, player.getMaxHealth() * 0.28F));
      player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 220, 2, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 140, 1, false, true, true));
      player.clearFire();
      if (player.level() instanceof ServerLevel level) {
         VFXServerEffects.spawn(level, "servant_enkidu_perfect_form", player, 128.0);
         level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 55, 0.9, 0.75, 0.9, 0.08);
         level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 0.9, player.getZ(), 24, 0.45, 0.55, 0.45, 0.04);
         level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.85F, 1.45F);
      }
   }

   public static void performEnkiduBulwark(ServerPlayer player) {
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 220, 2, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 220, 1, false, true, true));
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      interceptHostileProjectiles(player, level, 12.0);
      growPlayerPlantBulwark(player, level, 9, 11, 130);
   }

   public static void performEnkiduDetection(ServerPlayer player) {
      if (player.level() instanceof ServerLevel level) {
         List<Integer> ids = new ArrayList<>();
         for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(100.0), e -> {
            if (!e.isAlive() || e == player) return false;
            if (e instanceof ServerPlayer other) return !other.isSpectator();
            return !EntityUtils.isImmunePlayerTarget(e);
         })) {
            living.removeEffect(MobEffects.INVISIBILITY);
            ids.add(living.getId());
         }
         PacketDistributor.sendToPlayer(player, new EnkiduDetectionHighlightMessage(ids, 200), new CustomPacketPayload[0]);
         VFXServerEffects.spawnReplayable(level, "servant_enkidu_presence_detection", player, 0.65F);
         level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 32, 1.2, 0.55, 1.2, 0.04);
      }
   }

   public static void performEnkiduChains(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 eye = player.getEyePosition();
      Vec3 center = eye.add(player.getLookAngle().scale(22.0));
      List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(7.0, 4.0, 7.0), e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e));
      if (targets.isEmpty()) {
         LivingEntity lookTarget = findLookTarget(player, 30.0, 3.0);
         if (lookTarget != null) {
            targets = List.of(lookTarget);
         }
      }
      for (LivingEntity target : targets) {
         boolean divine = hasTrait(target, ServantTraitTag.DIVINE) || hasTrait(target, ServantTraitTag.CELESTIAL);
         int duration = divine ? 160 : 90;
         bindTarget(player, level, target, duration, divine);
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, divine ? 6 : 3, false, true, true));
         target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, divine ? 2 : 0, false, true, true));
         level.addFreshEntity(new ChainsOfHeavenBindingEntity(level, player, target, duration + 6, divine));
         VFXServerEffects.spawnReplayable(level, "servant_enkidu_chain_of_heaven", target, Math.max(1.2F, duration / 20.0F));
      }
      level.playSound(null, player.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.25F, 0.8F);
      TYPE_MOON_WORLD.queueServerWork(10, () -> performBoundPursuit(player));
   }

   public static void performEnkiduAgeOfBabylon(ServerPlayer player, int count, float damage, boolean mega) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      LivingEntity target = findLookTarget(player, mega ? 42.0 : 32.0, 2.2);
      VFXServerEffects.spawnReplayable(level, "servant_enkidu_age_of_babylon", player, mega ? 4.5F : 2.2F);
      spawnAgeOfBabylonVolley(player, level, target, count, 0, damage, mega ? 3.05F : 2.8F, true);
   }

   public static void performEnkiduGrandAgeOfBabylon(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      LivingEntity target = findLookTarget(player, 42.0, 2.5);
      VFXServerEffects.spawnReplayable(level, "servant_enkidu_age_of_babylon", player, 4.5F);
      spawnAgeOfBabylonVolley(player, level, target, 105, 0, 20.0F, 2.8F, true);
   }

   public static void performEnkiduMegaAgeOfBabylon(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      LivingEntity target = findLookTarget(player, 46.0, 2.8);
      VFXServerEffects.spawnReplayable(level, "servant_enkidu_age_of_babylon", player, 5.0F);
      for (int batch = 0; batch < 10; batch++) {
         int batchIndex = batch;
         TYPE_MOON_WORLD.queueServerWork(batch * 8 + 1, () -> {
            if (player.isAlive() && player.level() instanceof ServerLevel serverLevel) {
               LivingEntity liveTarget = target != null && target.isAlive() ? target : findLookTarget(player, 46.0, 2.8);
               spawnAgeOfBabylonVolley(player, serverLevel, liveTarget, 72, batchIndex, 22.0F, 3.05F, true);
            }
         });
      }
   }

   public static void performEnkiduEarthWedge(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 look = PlayerNoblePhantasmHelper.horizontalLook(player);
      for (double dist = 2.0; dist <= 10.0; dist += 1.4) {
         Vec3 pos = player.position().add(look.scale(dist));
         EnkiduEarthWeaponProjectileEntity weapon = EnkiduEarthWeaponProjectileEntity.weapon(level, player, new ItemStack(Items.TRIDENT), 18.0F, null, 0.0F, true);
         weapon.setPos(pos.x, pos.y - 0.6, pos.z);
         weapon.setDeltaMovement(0.0, 1.25, 0.0);
         level.addFreshEntity(weapon);
      }
      hitForwardArc(player, look, 10.0, 18.0F);
   }

   public static void performEnkiduStardust(ServerPlayer player) {
      if (player.level() instanceof ServerLevel level) {
         Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
         player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 2.2, 0.35, dir.z * 2.2));
         player.hurtMarked = true;
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.8, player.getZ(), 24, 0.35, 0.3, 0.35, 0.08);
      }
   }

   public static void performEnkiduEnumaElish(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      CompoundTag data = player.getPersistentData();
      long now = level.getGameTime();
      data.putBoolean(ENUMA_ACTIVE, true);
      data.putLong(ENUMA_RELEASE_TICK, now + ENUMA_WINDUP);
      data.putLong(ENUMA_FINISH_TICK, now + ENUMA_WINDUP + ENUMA_RELEASE_VISUAL);
      data.putInt(ENUMA_STAGE, 0);
      data.putLong(ENUMA_STAGE_START, now);
      data.putBoolean(ENUMA_PREV_INVULNERABLE, player.isInvulnerable());
      data.putBoolean(ENUMA_PREV_INVISIBLE, player.isInvisible());
      data.remove(ENUMA_INVISIBLE);
      data.putLong(ENUMA_LAST_FLIGHT_FX, 0L);
      data.putDouble(ENUMA_START_X, player.getX());
      data.putDouble(ENUMA_START_Y, player.getY());
      data.putDouble(ENUMA_START_Z, player.getZ());
      player.setNoGravity(true);
      player.fallDistance = 0.0F;
      VFXServerEffects.spawn(level, "servant_enkidu_enuma_elish", player, 192.0);
      spawnEnumaWindupFx(level, player.position());
      level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.6F, 0.85F);
   }

   public static void performEnkiduEarthSpike(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      LivingEntity target = findLookTarget(player, 24.0, 2.2);
      Vec3 center = target == null ? player.position().add(PlayerNoblePhantasmHelper.horizontalLook(player).scale(8.0)) : target.position();
      spawnPlantPatch(level, center, 3, 95, true);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.x, center.y + 0.12, center.z, 26, 0.75, 0.08, 0.75, 0.05);
      level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.35, center.z, 18, 0.5, 0.18, 0.5, 0.08);
      level.playSound(null, BlockPos.containing(center), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.15F, 0.85F);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(3.6, 2.4, 3.6), e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         living.invulnerableTime = 0;
         living.hurt(player.damageSources().playerAttack(player), 28.0F);
         living.invulnerableTime = 0;
         living.setDeltaMovement(living.getDeltaMovement().add(0.0, 0.42, 0.0));
         living.hurtMarked = true;
      }
   }

   public static void performEnkiduSkySpearSweep(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 forward = PlayerNoblePhantasmHelper.horizontalLook(player);
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
      Vec3 origin = player.position().add(0.0, player.getBbHeight() * 0.55, 0.0);
      AABB area = player.getBoundingBox().inflate(7.0, 1.6, 7.0).move(forward.scale(2.5));
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         Vec3 rel = living.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
         if (rel.lengthSqr() > 1.0E-4 && rel.normalize().dot(forward) < 0.15) {
            continue;
         }
         living.invulnerableTime = 0;
         living.hurt(player.damageSources().playerAttack(player), 38.0F);
         living.invulnerableTime = 0;
         living.push(forward.x * 0.7, 0.18, forward.z * 0.7);
         living.hurtMarked = true;
      }
      for (int i = -5; i <= 5; i++) {
         Vec3 fx = origin.add(forward.scale(2.2 + Math.abs(i) * 0.18)).add(side.scale(i * 0.58));
         level.sendParticles(ParticleTypes.END_ROD, fx.x, fx.y, fx.z, 6, 0.08, 0.08, 0.08, 0.035);
         level.sendParticles(ParticleTypes.SWEEP_ATTACK, fx.x, fx.y, fx.z, 1, 0.0, 0.0, 0.0, 0.0);
      }
      spawnPlantPatch(level, player.position().add(forward.scale(3.2)), 3, 75, true);
      level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.1F, 0.92F);
   }

   public static void performEnkiduMorphMelee(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      int variant = player.getRandom().nextInt(5);
      Vec3 forward = PlayerNoblePhantasmHelper.horizontalLook(player);
      LivingEntity target = findLookTarget(player, 5.5, 1.8);
      if (target == null) {
         hitForwardArc(player, forward, 4.5, 18.0F);
         level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + forward.x * 1.6, player.getY() + 1.0, player.getZ() + forward.z * 1.6, 2, 0.0, 0.0, 0.0, 0.0);
         return;
      }
      Vec3 hit = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      switch (variant) {
         case 0 -> {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 50, 0, false, true, true));
            damageAndMove(player, target, 24.0F, forward, 0.12, 0.08);
            level.sendParticles(ParticleTypes.CRIT, hit.x, hit.y, hit.z, 18, 0.2, 0.24, 0.2, 0.1);
         }
         case 1 -> {
            damageAndMove(player, target, 26.0F, forward, 0.35, 0.08);
            hitForwardArc(player, forward, 4.2, 12.0F);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, hit.x, hit.y, hit.z, 3, 0.0, 0.0, 0.0, 0.0);
         }
         case 2 -> {
            damageAndMove(player, target, 23.0F, forward, 0.05, 0.52);
            spawnPlantPatch(level, target.position(), 2, 65, true);
         }
         case 3 -> {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 55, 1, false, true, true));
            damageAndMove(player, target, 20.0F, player.position().subtract(target.position()), 0.22, 0.06);
            spawnPlantPatch(level, target.position(), 3, 70, true);
         }
         default -> {
            boolean divine = hasTrait(target, ServantTraitTag.DIVINE);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, divine ? 80 : 45, divine ? 1 : 0, false, true, true));
            damageAndMove(player, target, divine ? 31.0F : 24.0F, player.position().subtract(target.position()), 0.28, 0.08);
            VFXServerEffects.spawn(level, "servant_enkidu_chain_of_heaven", target, 96.0);
         }
      }
      level.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.9F, 1.35F);
   }

   private static void tickEnumaElishFlightState(ServerPlayer player, ServerLevel level) {
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(ENUMA_ACTIVE)) {
         return;
      }
      long now = level.getGameTime();
      if (!player.isAlive()) {
         clearEnumaState(player);
         return;
      }
      long release = data.getLong(ENUMA_RELEASE_TICK);
      if (now < release) {
         player.setNoGravity(true);
         player.fallDistance = 0.0F;
         double progress = 1.0 - (double)(release - now) / Math.max(1.0, ENUMA_WINDUP);
         Vec3 anchor = new Vec3(data.getDouble(ENUMA_START_X), data.getDouble(ENUMA_START_Y) + 13.0 + progress * 7.0, data.getDouble(ENUMA_START_Z));
         Vec3 hover = anchor.subtract(player.position());
         player.setDeltaMovement(player.getDeltaMovement().scale(0.45).add(
            Mth.clamp(hover.x * 0.04, -0.16, 0.16),
            Mth.clamp(hover.y * 0.07, 0.03, 0.34),
            Mth.clamp(hover.z * 0.04, -0.16, 0.16)
         ));
         player.hurtMarked = true;
         if (now % 4L == 0L) {
            emitEnumaDrillFx(level, player, player.position().add(0.0, 12.0, 0.0), now, false);
         }
         return;
      }
      if (player.isInvulnerable() && data.contains(ENUMA_PREV_INVULNERABLE)) {
         player.setInvulnerable(data.getBoolean(ENUMA_PREV_INVULNERABLE));
      }
      if (data.getInt(ENUMA_STAGE) <= 1) {
         activateEnumaInvisibility(player);
      }
      player.setNoGravity(true);
      player.fallDistance = 0.0F;
      Vec3 dir = player.getLookAngle().lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : player.getLookAngle().normalize();
      player.setDeltaMovement(dir.scale(data.getInt(ENUMA_STAGE) <= 1 ? 3.05 : 3.45));
      player.hurtMarked = true;
      maybeSpawnEnumaFlightFx(level, player, now);
      emitEnumaDrillFx(level, player, player.position().add(dir.scale(4.0)), now, true);
      LivingEntity hit = findFlightHit(player, level);
      boolean timedOut = now >= data.getLong(ENUMA_FINISH_TICK);
      if (player.horizontalCollision || hit != null || timedOut) {
         if (data.getInt(ENUMA_STAGE) <= 1) {
            Vec3 impact = hit == null ? player.position() : hit.position().add(0.0, hit.getBbHeight() * 0.45, 0.0);
            restoreEnumaInvisibility(player);
            applyEnumaSmallExplosion(player, level, impact, hit);
            data.putInt(ENUMA_STAGE, 2);
            data.putLong(ENUMA_STAGE_START, now);
            Vec3 boosted = dir.add(0.0, -0.18, 0.0).normalize().scale(3.65);
            player.setDeltaMovement(boosted);
            player.hurtMarked = true;
         } else {
            Vec3 impact = hit == null ? player.position() : hit.position().add(0.0, hit.getBbHeight() * 0.45, 0.0);
            restoreEnumaInvisibility(player);
            applyEnumaGroundExplosion(player, level, impact, hit);
            clearEnumaState(player);
         }
      }
   }

   private static LivingEntity findFlightHit(ServerPlayer player, ServerLevel level) {
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(1.6), e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         return living;
      }
      return null;
   }

   public static void clearActiveEnumaState(ServerPlayer player) {
      clearEnumaState(player);
   }

   public static boolean isEnumaElishActive(ServerPlayer player) {
      return player != null && player.getPersistentData().getBoolean(ENUMA_ACTIVE);
   }

   private static void clearEnumaState(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      if (data.contains(ENUMA_PREV_INVULNERABLE)) {
         player.setInvulnerable(data.getBoolean(ENUMA_PREV_INVULNERABLE));
      }
      restoreEnumaInvisibility(player);
      player.setNoGravity(false);
      player.fallDistance = 0.0F;
      data.remove(ENUMA_ACTIVE);
      data.remove(ENUMA_RELEASE_TICK);
      data.remove(ENUMA_FINISH_TICK);
      data.remove(ENUMA_STAGE);
      data.remove(ENUMA_STAGE_START);
      data.remove(ENUMA_PREV_INVULNERABLE);
      data.remove(ENUMA_INVISIBLE);
      data.remove(ENUMA_PREV_INVISIBLE);
      data.remove(ENUMA_LAST_FLIGHT_FX);
      data.remove(ENUMA_START_X);
      data.remove(ENUMA_START_Y);
      data.remove(ENUMA_START_Z);
   }

   private static void activateEnumaInvisibility(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(ENUMA_INVISIBLE)) {
         if (!data.contains(ENUMA_PREV_INVISIBLE)) {
            data.putBoolean(ENUMA_PREV_INVISIBLE, player.isInvisible());
         }
         data.putBoolean(ENUMA_INVISIBLE, true);
         player.setInvisible(true);
      }
   }

   private static void restoreEnumaInvisibility(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      if (data.getBoolean(ENUMA_INVISIBLE)) {
         player.setInvisible(data.getBoolean(ENUMA_PREV_INVISIBLE));
         data.remove(ENUMA_INVISIBLE);
      }
   }

   private static void spawnEnumaWindupFx(ServerLevel level, Vec3 origin) {
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, origin.x, origin.y + 0.15, origin.z, 220, 12.0, 0.16, 12.0, 0.1);
      level.sendParticles(ParticleTypes.END_ROD, origin.x, origin.y + 10.0, origin.z, 220, 2.0, 9.0, 2.0, 0.2);
      level.sendParticles(ParticleTypes.ENCHANTED_HIT, origin.x, origin.y + 3.0, origin.z, 180, 10.0, 3.0, 10.0, 0.16);
      level.sendParticles(ParticleTypes.FLASH, origin.x, origin.y + 1.0, origin.z, 3, 0.16, 0.16, 0.16, 0.0);
   }

   private static void maybeSpawnEnumaFlightFx(ServerLevel level, ServerPlayer player, long now) {
      CompoundTag data = player.getPersistentData();
      if (now - data.getLong(ENUMA_LAST_FLIGHT_FX) < 18L) {
         return;
      }
      data.putLong(ENUMA_LAST_FLIGHT_FX, now);
      VFXServerEffects.spawn(level, "servant_enkidu_enuma_elish_flight", player, 192.0);
      Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.55, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 28, 0.55, 0.55, 0.55, 0.12);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.x, center.y, center.z, 18, 0.42, 0.42, 0.42, 0.08);
   }

   private static void emitEnumaDrillFx(ServerLevel level, ServerPlayer player, Vec3 targetPoint, long now, boolean release) {
      Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
      Vec3 dir = targetPoint.subtract(center);
      if (dir.lengthSqr() < 1.0E-4) {
         dir = player.getLookAngle();
      }
      dir = dir.normalize();
      Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
      if (side.lengthSqr() < 1.0E-4) {
         side = new Vec3(1.0, 0.0, 0.0);
      }
      side = side.normalize();
      Vec3 up = side.cross(dir).normalize();
      int points = release ? 18 : 10;
      double radius = release ? 1.05 : 0.65;
      for (int i = 0; i < points; i++) {
         double angle = now * 0.48 + i * Math.PI * 2.0 / points;
         double along = i * (release ? 0.32 : 0.22);
         Vec3 ring = side.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
         Vec3 pos = center.add(dir.scale(along)).add(ring);
         level.sendParticles(i % 3 == 0 ? ParticleTypes.END_ROD : ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.01);
      }
      if (now % 3L == 0L) {
         level.sendParticles(ParticleTypes.ENCHANTED_HIT, center.x, center.y, center.z, release ? 16 : 8, 0.65, 0.65, 0.65, 0.08);
      }
   }

   private static void applyEnumaSmallExplosion(ServerPlayer player, ServerLevel level, Vec3 impact, LivingEntity directTarget) {
      VFXServerEffects.spawn(level, "servant_enkidu_enuma_elish_impact", impact, 192.0);
      level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y + 0.35, impact.z, 8, 1.2, 0.55, 1.2, 0.02);
      level.sendParticles(ParticleTypes.FLASH, impact.x, impact.y + 0.25, impact.z, 1, 0.08, 0.08, 0.08, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, impact.x, impact.y, impact.z, 80, 1.7, 1.0, 1.7, 0.16);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, impact.x, impact.y + 0.2, impact.z, 38, 1.5, 0.75, 1.5, 0.1);
      level.playSound(null, BlockPos.containing(impact), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0F, 1.45F);
      if (directTarget != null) {
         applyNoDefenseDamageOverTicks(player, directTarget, 4000.0F, 20);
      }
      applyEnumaAreaDamage(player, level, impact, 7.0, 500.0F, directTarget);
      breakEnumaImpactTerrain(level, impact, 6.0);
   }

   private static void applyEnumaGroundExplosion(ServerPlayer player, ServerLevel level, Vec3 impact, LivingEntity directTarget) {
      double radius = ENUMA_GROUND_EXPLOSION_RADIUS;
      VFXServerEffects.spawn(level, "servant_enkidu_enuma_elish_ground_impact", impact, 192.0);
      VFXServerEffects.spawn(level, "servant_enkidu_enuma_elish_aftermath", impact, 192.0);
      level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impact.x, impact.y, impact.z, 10, 1.2, 0.8, 1.2, 0.0);
      level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y + 1.2, impact.z, 56, radius * 0.34, radius * 0.22, radius * 0.34, 0.045);
      level.sendParticles(ParticleTypes.FLASH, impact.x, impact.y + 0.4, impact.z, 5, 0.4, 0.25, 0.4, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, impact.x, impact.y, impact.z, 520, radius * 0.55, radius * 0.5, radius * 0.55, 0.34);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, impact.x, impact.y + 0.35, impact.z, 320, radius * 0.5, radius * 0.34, radius * 0.5, 0.24);
      level.sendParticles(ParticleTypes.ENCHANTED_HIT, impact.x, impact.y + 0.45, impact.z, 280, radius * 0.48, radius * 0.42, radius * 0.48, 0.28);
      level.sendParticles(ParticleTypes.CLOUD, impact.x, impact.y + 0.05, impact.z, 260, radius * 0.58, radius * 0.26, radius * 0.58, 0.18);
      level.playSound(null, BlockPos.containing(impact), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 5.0F, 0.82F);
      applyEnumaAreaDamage(player, level, impact, radius, 500.0F, directTarget);
      breakEnumaImpactTerrainInWaves(level, impact, radius);
   }

   private static void applyEnumaAreaDamage(ServerPlayer player, ServerLevel level, Vec3 impact, double radius, float damage, LivingEntity directTarget) {
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(impact, impact).inflate(radius), e -> e != player && e.isAlive() && !e.isAlliedTo(player) && !EntityUtils.isImmunePlayerTarget(e))) {
         double distance = Math.max(1.0, living.position().distanceTo(impact));
         if (distance <= radius) {
            float scaled = (float)(damage * Math.max(0.2, 1.0 - distance / (radius + 4.0)));
            if (living == directTarget) {
               scaled = Math.max(scaled, damage * 0.85F);
            }
            applyNoDefenseDamage(player, living, scaled);
         }
      }
   }

   private static void breakEnumaImpactTerrain(ServerLevel level, Vec3 impact, double radius) {
      BlockPos center = BlockPos.containing(impact);
      double radiusSqr = radius * radius;
      double guaranteedCore = Math.min(radius * 0.34, 18.0);
      double guaranteedCoreSqr = guaranteedCore * guaranteedCore;
      DeferredTerrainDestruction.queueSphere(level, impact, radius, 24, (serverLevel, pos, ignoredDistanceSqr, currentRadius, origin) -> {
         double dx = pos.getX() - center.getX() + 0.5 - (impact.x - center.getX());
         double dy = pos.getY() - center.getY() + 0.5 - (impact.y - center.getY());
         double dz = pos.getZ() - center.getZ() + 0.5 - (impact.z - center.getZ());
         double distSqr = dx * dx + dy * dy + dz * dz;
         if (distSqr > radiusSqr) {
            return false;
         }
         double edge = Math.sqrt(distSqr) / Math.max(1.0, radius);
         boolean innerCore = distSqr <= guaranteedCoreSqr;
         double noiseThreshold = radius > 40.0 ? 0.24 + edge * 0.18 : 0.1;
         return (innerCore || blockNoise(serverLevel, pos) >= noiseThreshold) && canEnumaBreakBlock(serverLevel, pos);
      }, (serverLevel, pos, removed) -> {
         if ((removed & 63) == 0) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0.0);
         }
      });
   }

   private static void breakEnumaImpactTerrainInWaves(ServerLevel level, Vec3 impact, double radius) {
      int waveCount = Math.max(1, Mth.ceil(radius / 2.0));
      double waveStep = radius / waveCount;
      for (int wave = 1; wave <= waveCount; wave++) {
         final int waveIndex = wave;
         TYPE_MOON_WORLD.queueServerWork(waveIndex * 2, () -> {
            double previousRadius = Math.max(0.0, (waveIndex - 1) * waveStep);
            double currentRadius = waveIndex * waveStep;
            DeferredTerrainDestruction.queueShell(level, impact, currentRadius, previousRadius, 48,
               (serverLevel, pos, distanceSqr, current, origin) -> canEnumaBreakBlock(serverLevel, pos),
               (serverLevel, pos, removed) -> {
                  if ((removed & 127) == 0) {
                     serverLevel.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.24, 0.24, 0.24, 0.0);
                  }
               });
            if (waveIndex % 3 == 0) {
               level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2F, 0.72F);
            }
         });
      }
   }

   private static boolean canEnumaBreakBlock(ServerLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      float hardness = state.getDestroySpeed(level, pos);
      return !state.isAir()
         && !state.hasBlockEntity()
         && hardness >= 0.0F
         && hardness <= 80.0F
         && state.getExplosionResistance(level, pos, null) < 1200.0F;
   }

   private static double blockNoise(ServerLevel level, BlockPos pos) {
      long seed = level.getSeed();
      long mixed = seed ^ (long)pos.getX() * 341873128712L ^ (long)pos.getY() * 132897987541L ^ (long)pos.getZ() * 42317861L;
      mixed ^= mixed >>> 33;
      mixed *= 0xff51afd7ed558ccdL;
      mixed ^= mixed >>> 33;
      mixed *= 0xc4ceb9fe1a85ec53L;
      mixed ^= mixed >>> 33;
      return (mixed & 0xFFFFFFL) / (double)0x1000000L;
   }

   private static void applyNoDefenseDamage(ServerPlayer player, LivingEntity target, float amount) {
      target.removeEffect(MobEffects.DAMAGE_RESISTANCE);
      target.removeEffect(MobEffects.ABSORPTION);
      target.setAbsorptionAmount(0.0F);
      target.invulnerableTime = 0;
      float before = target.getHealth();
      target.hurt(player.damageSources().magic(), amount);
      target.invulnerableTime = 0;
      float expected = before - amount;
      if (target.isAlive() && target.getHealth() > expected) {
         target.setHealth(Math.max(0.0F, expected));
         if (target.getHealth() <= 0.0F) {
            target.die(player.damageSources().magic());
         }
      }
   }

   private static void applyNoDefenseDamageOverTicks(ServerPlayer player, LivingEntity target, float totalAmount, int ticks) {
      int duration = Math.max(1, ticks);
      float perTick = totalAmount / duration;
      for (int delay = 0; delay < duration; delay++) {
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            if (player.isAlive() && target.isAlive()) {
               applyNoDefenseDamage(player, target, perTick);
            }
         });
      }
   }

   private static void tickPerfectFormPassive(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, ServerLevel level) {
      if (player.tickCount % 20 != 0 || !isOnEarth(player, level)) {
         return;
      }
      vars.servant_card_mana = Math.min(vars.servant_card_max_mana, vars.servant_card_mana + 6.0);
      if (player.getHealth() < player.getMaxHealth()) {
         player.heal(6.0F);
      }
      ServantDefinition definition = ServantDataRegistry.get(vars.servant_card_id);
      ServantParams params = definition == null ? null : definition.parameters();
      CompoundTag data = player.getPersistentData();
      data.putDouble("TypeMoonCombatStamina", Math.min(ServantCombatFormulas.staminaMax(params), data.getDouble("TypeMoonCombatStamina") + ServantCombatFormulas.staminaRegenPerSecond(params)));
      data.putDouble("TypeMoonCombatPoise", Math.min(ServantCombatFormulas.poiseMax(params), data.getDouble("TypeMoonCombatPoise") + ServantCombatFormulas.poiseRegenPerSecond(params)));
      if (player.tickCount % 40 == 0) {
         level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 0.35, player.getZ(), 8, 0.28, 0.16, 0.28, 0.035);
         vars.syncPlayerVariables(player);
      }
   }

   private static boolean isOnEarth(ServerPlayer player, ServerLevel level) {
      if (!player.onGround()) {
         return false;
      }
      BlockPos below = player.blockPosition().below();
      BlockState state = level.getBlockState(below);
      return !state.isAir() && state.isSolidRender(level, below);
   }

   private static void tickPassivePresence(ServerPlayer player, ServerLevel level) {
      if ((player.tickCount + Math.floorMod(player.getUUID().hashCode(), 20)) % 20 != 0) {
         return;
      }
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(50.0), e -> e != player && e.isAlive() && !EntityUtils.isImmunePlayerTarget(e))) {
         if (living.hasEffect(MobEffects.INVISIBILITY)) {
            living.removeEffect(MobEffects.INVISIBILITY);
         }
      }
   }

   private static void interceptHostileProjectiles(ServerPlayer player, ServerLevel level, double radius) {
      CompoundTag data = player.getPersistentData();
      if (player.tickCount - data.getInt(TAG_LAST_PROJECTILE_INTERCEPT) < 4) {
         return;
      }
      data.putInt(TAG_LAST_PROJECTILE_INTERCEPT, player.tickCount);
      List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(radius), p -> p.isAlive() && p.getOwner() != player);
      List<OdaMatchlockBulletEntity> bullets = level.getEntitiesOfClass(OdaMatchlockBulletEntity.class, player.getBoundingBox().inflate(radius), b -> b.isAlive() && b.getOwner() != player);
      if (projectiles.isEmpty() && bullets.isEmpty()) {
         return;
      }
      int spawned = 0;
      for (Projectile projectile : projectiles) {
         if (spawned++ >= 8) {
            break;
         }
         spawnInterceptor(player, level, projectile.position(), projectile);
      }
      for (OdaMatchlockBulletEntity bullet : bullets) {
         if (spawned++ >= 12) {
            break;
         }
         spawnInterceptor(player, level, bullet.position(), bullet);
      }
   }

   private static void spawnInterceptor(ServerPlayer player, ServerLevel level, Vec3 targetPos, Entity target) {
      Vec3 spawn = player.position().add(0.0, player.getBbHeight() * 0.65, 0.0);
      EnkiduEarthWeaponProjectileEntity interceptor = EnkiduEarthWeaponProjectileEntity.weapon(level, player, new ItemStack(Items.TRIDENT), 10.0F, null, 0.0F, false);
      interceptor.setPos(spawn.x, spawn.y, spawn.z);
      Vec3 aim = targetPos.subtract(spawn);
      interceptor.setDeltaMovement(aim.lengthSqr() > 1.0E-4 ? aim.normalize().scale(4.2) : new Vec3(0.0, 1.2, 0.0));
      interceptor.alignToMotion();
      level.addFreshEntity(interceptor);
   }

   private static void bindTarget(ServerPlayer player, ServerLevel level, LivingEntity target, int duration, boolean divine) {
      CompoundTag data = target.getPersistentData();
      unregisterBoundTarget(data.hasUUID(BOUND_OWNER) ? data.getUUID(BOUND_OWNER) : null, target.getUUID());
      long until = level.getGameTime() + duration;
      data.putLong(BOUND_UNTIL, until);
      data.putUUID(BOUND_OWNER, player.getUUID());
      data.putDouble(BOUND_X, target.getX());
      data.putDouble(BOUND_Y, target.getY());
      data.putDouble(BOUND_Z, target.getZ());
      if (target instanceof Mob mob) {
         data.putBoolean(BOUND_PREV_NO_AI, mob.isNoAi());
         mob.getNavigation().stop();
         mob.setNoAi(true);
      }
      target.setDeltaMovement(Vec3.ZERO);
      registerBoundTarget(player.getUUID(), target.getUUID());
      TYPE_MOON_WORLD.queueServerWork(duration + 2, () -> restoreBoundTarget(target, player.getUUID()));
   }

   private static void tickBoundTargets(ServerPlayer player, ServerLevel level) {
      long now = level.getGameTime();
      Set<UUID> targetIds = BOUND_TARGETS_BY_OWNER.get(player.getUUID());
      if (targetIds == null || targetIds.isEmpty()) {
         return;
      }
      Iterator<UUID> iterator = targetIds.iterator();
      while (iterator.hasNext()) {
         UUID targetId = iterator.next();
         Entity entity = level.getEntity(targetId);
         if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
            iterator.remove();
            continue;
         }
         CompoundTag data = target.getPersistentData();
         if (!data.hasUUID(BOUND_OWNER) || !player.getUUID().equals(data.getUUID(BOUND_OWNER))) {
            iterator.remove();
            continue;
         }
         if (data.getLong(BOUND_UNTIL) <= now) {
            restoreBoundTarget(target, player.getUUID(), false);
            iterator.remove();
            continue;
         }
         target.setDeltaMovement(Vec3.ZERO);
         target.hurtMarked = true;
         target.setPos(data.getDouble(BOUND_X), data.getDouble(BOUND_Y), data.getDouble(BOUND_Z));
         if (target instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
            mob.setNoAi(true);
         }
      }
      if (targetIds.isEmpty()) {
         BOUND_TARGETS_BY_OWNER.remove(player.getUUID());
      }
   }

   private static void restoreBoundTarget(LivingEntity target, UUID owner) {
      restoreBoundTarget(target, owner, true);
   }

   private static void restoreBoundTarget(LivingEntity target, UUID owner, boolean unregister) {
      if (target == null) {
         return;
      }
      UUID targetId = target.getUUID();
      CompoundTag data = target.getPersistentData();
      if (!data.hasUUID(BOUND_OWNER) || !owner.equals(data.getUUID(BOUND_OWNER))) {
         if (unregister) {
            unregisterBoundTarget(owner, targetId);
         }
         return;
      }
      if (target instanceof Mob mob) {
         mob.setNoAi(data.getBoolean(BOUND_PREV_NO_AI));
      }
      data.remove(BOUND_OWNER);
      data.remove(BOUND_UNTIL);
      data.remove(BOUND_PREV_NO_AI);
      data.remove(BOUND_X);
      data.remove(BOUND_Y);
      data.remove(BOUND_Z);
      if (unregister) {
         unregisterBoundTarget(owner, targetId);
      }
   }

   private static void performBoundPursuit(ServerPlayer player) {
      if (!player.isAlive() || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      for (LivingEntity target : getTrackedBoundTargets(player, level)) {
         spawnAgeOfBabylonAroundTarget(player, level, target, 24, 0, 20.0F, 2.9F);
      }
   }

   private static void registerBoundTarget(UUID owner, UUID target) {
      if (owner == null || target == null) {
         return;
      }
      BOUND_TARGETS_BY_OWNER.computeIfAbsent(owner, ignored -> new LinkedHashSet<>()).add(target);
   }

   private static void unregisterBoundTarget(UUID owner, UUID target) {
      if (owner == null || target == null) {
         return;
      }
      Set<UUID> targets = BOUND_TARGETS_BY_OWNER.get(owner);
      if (targets == null) {
         return;
      }
      targets.remove(target);
      if (targets.isEmpty()) {
         BOUND_TARGETS_BY_OWNER.remove(owner);
      }
   }

   private static List<LivingEntity> getTrackedBoundTargets(ServerPlayer player, ServerLevel level) {
      Set<UUID> targetIds = BOUND_TARGETS_BY_OWNER.get(player.getUUID());
      if (targetIds == null || targetIds.isEmpty()) {
         return List.of();
      }
      long now = level.getGameTime();
      List<LivingEntity> targets = new ArrayList<>();
      Iterator<UUID> iterator = targetIds.iterator();
      while (iterator.hasNext()) {
         UUID targetId = iterator.next();
         Entity entity = level.getEntity(targetId);
         if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
            iterator.remove();
            continue;
         }
         CompoundTag data = target.getPersistentData();
         if (!data.hasUUID(BOUND_OWNER) || !player.getUUID().equals(data.getUUID(BOUND_OWNER)) || data.getLong(BOUND_UNTIL) <= now) {
            iterator.remove();
            continue;
         }
         targets.add(target);
      }
      if (targetIds.isEmpty()) {
         BOUND_TARGETS_BY_OWNER.remove(player.getUUID());
      }
      return targets;
   }

   private static void clearTrackedBoundTargets(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         BOUND_TARGETS_BY_OWNER.remove(player.getUUID());
         return;
      }
      Set<UUID> targetIds = BOUND_TARGETS_BY_OWNER.remove(player.getUUID());
      if (targetIds == null || targetIds.isEmpty()) {
         return;
      }
      for (UUID targetId : targetIds) {
         Entity entity = level.getEntity(targetId);
         if (entity instanceof LivingEntity target) {
            restoreBoundTarget(target, player.getUUID());
         }
      }
   }

   private static void spawnAgeOfBabylonVolley(ServerPlayer player, ServerLevel level, LivingEntity target, int count, int batch, float damage, float speed, boolean volley) {
      Vec3 targetCenter = target == null ? player.getEyePosition().add(player.getLookAngle().scale(28.0)) : target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      Vec3 origin = player.position().add(0.0, 0.8, 0.0);
      Vec3 toTarget = targetCenter.subtract(origin);
      Vec3 forward = new Vec3(toTarget.x, 0.0, toTarget.z);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = PlayerNoblePhantasmHelper.horizontalLook(player);
      }
      forward = forward.normalize();
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
      Vec3 volleyCenter = player.position().subtract(forward.scale(5.5 + Math.min(18.0, batch * 1.1)));
      int columns = volley ? Math.max(8, (int)Math.ceil(Math.sqrt(count * 1.35))) : 1;
      int rows = volley ? Math.max(1, (int)Math.ceil((double)count / columns)) : 1;
      double sideSpacing = count >= 80 ? 3.8 : 3.15;
      double rowSpacing = count >= 80 ? 3.35 : 2.75;
      for (int i = 0; i < count; i++) {
         int row = i / columns;
         int col = i % columns;
         double forwardOffset = -(row - (rows - 1) * 0.42) * rowSpacing - player.getRandom().nextDouble() * 1.45;
         double sideOffset = (col - (columns - 1) * 0.5) * sideSpacing + (player.getRandom().nextDouble() - 0.5) * 1.45;
         Vec3 spawn = groundSpawn(level, volleyCenter.add(forward.scale(forwardOffset)).add(side.scale(sideOffset)));
         Vec3 aimPoint = targetCenter.add(side.scale((player.getRandom().nextDouble() - 0.5) * 7.0)).add(forward.scale((player.getRandom().nextDouble() - 0.5) * 5.0)).add(0.0, (player.getRandom().nextDouble() - 0.5) * 2.1, 0.0);
         ItemStack stack = AGE_WEAPONS[(i + batch) % AGE_WEAPONS.length].copy();
         float finalDamage = target != null && hasTrait(target, ServantTraitTag.DIVINE) ? damage + 8.0F : damage;
         spawnAgeGate(level, spawn, true);
         int delay = 6 + ((batch * count + i) % 4);
         TYPE_MOON_WORLD.queueServerWork(delay, () -> spawnAgeProjectile(player, level, target, stack, finalDamage, spawn, aimPoint, speed, true));
      }
      level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.PLAYERS, 1.2F, 1.35F);
   }

   private static void spawnAgeOfBabylonAroundTarget(ServerPlayer player, ServerLevel level, LivingEntity target, int count, int batch, float damage, float speed) {
      Vec3 targetCenter = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      for (int i = 0; i < count; i++) {
         double angle = (Math.PI * 2.0 * i) / Math.max(1, count) + player.getRandom().nextDouble() * 0.28;
         double radius = 7.5 + player.getRandom().nextDouble() * 7.0;
         Vec3 spawn = groundSpawn(level, target.position().add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius));
         ItemStack stack = AGE_WEAPONS[(i + batch) % AGE_WEAPONS.length].copy();
         spawnAgeGate(level, spawn, true);
         int delay = 6 + ((batch * count + i) % 5);
         TYPE_MOON_WORLD.queueServerWork(delay, () -> spawnAgeProjectile(player, level, target, stack, damage, spawn, targetCenter, speed, true));
      }
   }

   private static void spawnAgeProjectile(ServerPlayer player, ServerLevel level, LivingEntity target, ItemStack stack, float damage, Vec3 spawn, Vec3 aimPoint, float speed, boolean volley) {
      if (!player.isAlive()) {
         return;
      }
      EnkiduEarthWeaponProjectileEntity projectile;
      if (stack.is(Items.BOW) || stack.is(Items.CROSSBOW)) {
         projectile = EnkiduEarthWeaponProjectileEntity.bow(level, player, stack, damage, target, stack.is(Items.CROSSBOW));
      } else {
         projectile = EnkiduEarthWeaponProjectileEntity.weapon(level, player, stack, damage, target, volley ? 0.22F : 0.16F, true);
      }
      projectile.setPos(spawn.x, spawn.y, spawn.z);
      Vec3 aim = aimPoint.subtract(spawn);
      projectile.setDeltaMovement(aim.lengthSqr() > 1.0E-4 ? aim.normalize().scale(speed + player.getRandom().nextDouble() * 0.7) : new Vec3(0.0, 0.15, 0.0));
      projectile.alignToMotion();
      level.addFreshEntity(projectile);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, spawn.x, spawn.y + 0.25, spawn.z, 6, 0.2, 0.18, 0.2, 0.05);
   }

   private static Vec3 groundSpawn(ServerLevel level, Vec3 approximate) {
      BlockPos column = BlockPos.containing(approximate.x, approximate.y, approximate.z);
      BlockPos surface = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
      while (surface.getY() > level.getMinBuildHeight() && level.getBlockState(surface.below()).isAir()) {
         surface = surface.below();
      }
      return Vec3.atBottomCenterOf(surface).add(0.0, 0.08, 0.0);
   }

   private static void spawnAgeGate(ServerLevel level, Vec3 pos, boolean volley) {
      VFXServerEffects.spawn(level, "servant_enkidu_age_of_babylon_gate", pos, volley ? 160.0 : 96.0);
      level.sendParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y + 0.04, pos.z, volley ? 18 : 10, volley ? 0.9 : 0.45, 0.02, volley ? 0.9 : 0.45, 0.03);
      level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 0.45, pos.z, volley ? 9 : 5, 0.18, 0.18, 0.18, 0.04);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y + 0.22, pos.z, volley ? 6 : 3, 0.12, 0.12, 0.12, 0.035);
   }

   private static void growPlayerPlantBulwark(ServerPlayer player, ServerLevel level, int radius, int height, int duration) {
      Vec3 forward = PlayerNoblePhantasmHelper.horizontalLook(player);
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
      BlockPos base = findSurface(level, BlockPos.containing(player.position().add(forward.scale(3.0))));
      List<BlockPos> placed = new ArrayList<>();
      for (int y = 0; y <= height; y++) {
         int layer = y;
         TYPE_MOON_WORLD.queueServerWork(layer * 2, () -> {
            double vertical = (double)layer / Math.max(1.0, height);
            double layerRadius = radius * Math.sqrt(Math.max(0.0, 1.0 - vertical * vertical * 0.82));
            int r = Math.max(1, (int)Math.ceil(layerRadius));
            for (int sx = -r; sx <= r; sx++) {
               for (int depth = -1; depth <= r; depth++) {
                  double normalized = (sx * sx) / Math.max(1.0, layerRadius * layerRadius) + (depth * depth) / Math.max(1.0, radius * radius);
                  if (normalized > 1.05) {
                     continue;
                  }
                  Vec3 offset = side.scale(sx).add(forward.scale(depth));
                  BlockPos pos = BlockPos.containing(base.getX() + 0.5 + offset.x, base.getY() + layer, base.getZ() + 0.5 + offset.z);
                  if (!canGrowTemporaryPlant(level, pos)) {
                     continue;
                  }
                  level.setBlock(pos, layer < 2 || normalized < 0.34 ? Blocks.OAK_LOG.defaultBlockState() : Blocks.OAK_LEAVES.defaultBlockState(), 3);
                  net.xxxjk.TYPE_MOON_WORLD.servant.entity.EnkiduTemporaryPlantHelper.register(level, pos, level.getGameTime() + duration);
                  placed.add(pos.immutable());
               }
            }
            Vec3 layerCenter = Vec3.atCenterOf(base.above(layer));
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, layerCenter.x, layerCenter.y, layerCenter.z, 14, radius * 0.35, 0.18, radius * 0.35, 0.05);
            level.playSound(null, base, layer == 0 ? SoundEvents.ROOTED_DIRT_BREAK : SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 0.95F, 0.72F + layer * 0.06F);
         });
      }
      TYPE_MOON_WORLD.queueServerWork(duration, () -> clearTemporaryPlants(level, placed));
   }

   private static void spawnPlantPatch(ServerLevel level, Vec3 center, int radius, int duration, boolean lush) {
      BlockPos base = findSurface(level, BlockPos.containing(center));
      List<BlockPos> placed = new ArrayList<>();
      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            BlockPos pos = findSurface(level, base.offset(dx, 0, dz)).above();
            if (!canGrowTemporaryPlant(level, pos)) {
               continue;
            }
            level.setBlock(pos, lush && (Math.abs(dx) + Math.abs(dz)) % 4 == 0 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.FERN.defaultBlockState(), 3);
            net.xxxjk.TYPE_MOON_WORLD.servant.entity.EnkiduTemporaryPlantHelper.register(level, pos, level.getGameTime() + duration);
            placed.add(pos.immutable());
         }
      }
      Vec3 fx = Vec3.atCenterOf(base);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, fx.x, fx.y + 0.45, fx.z, lush ? 28 : 16, radius * 0.45, 0.18, radius * 0.45, 0.04);
      TYPE_MOON_WORLD.queueServerWork(duration, () -> clearTemporaryPlants(level, placed));
   }

   private static BlockPos findSurface(ServerLevel level, BlockPos start) {
      BlockPos.MutableBlockPos pos = start.mutable();
      while (pos.getY() > level.getMinBuildHeight() && level.getBlockState(pos).isAir()) {
         pos.move(Direction.DOWN);
      }
      while (pos.getY() < level.getMaxBuildHeight() - 1 && !level.getBlockState(pos.above()).isAir()) {
         pos.move(Direction.UP);
      }
      return pos.immutable();
   }

   private static boolean canGrowTemporaryPlant(ServerLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return state.isAir() || state.canBeReplaced();
   }

   private static void clearTemporaryPlants(ServerLevel level, List<BlockPos> positions) {
      for (BlockPos pos : positions) {
         if (net.xxxjk.TYPE_MOON_WORLD.servant.entity.EnkiduTemporaryPlantHelper.unregister(level, pos)
            && net.xxxjk.TYPE_MOON_WORLD.servant.entity.EnkiduTemporaryPlantHelper.isTemporaryPlantState(level.getBlockState(pos))) {
            level.removeBlock(pos, false);
         }
      }
   }

   private static void damageAndMove(ServerPlayer player, LivingEntity target, float damage, Vec3 direction, double horizontal, double vertical) {
      Vec3 dir = new Vec3(direction.x, 0.0, direction.z);
      if (dir.lengthSqr() < 1.0E-4) {
         dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      }
      dir = dir.normalize();
      target.invulnerableTime = 0;
      target.hurt(player.damageSources().playerAttack(player), damage);
      target.invulnerableTime = 0;
      target.push(dir.x * horizontal, vertical, dir.z * horizontal);
      target.hurtMarked = true;
   }

}

