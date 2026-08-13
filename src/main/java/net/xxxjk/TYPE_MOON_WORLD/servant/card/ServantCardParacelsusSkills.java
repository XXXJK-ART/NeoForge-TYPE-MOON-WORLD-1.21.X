package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaBeamEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaMagicBoltEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ProjectionCircuitEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RhoAiasEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.OpenParacelsusCraftScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.OpenParacelsusElementScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ParacelsusSpiritCannonEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.BattlefieldAreaService;
import org.joml.Vector3f;

import static net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillUtils.findLookTarget;

public final class ServantCardParacelsusSkills {
   private static final String WORKSHOP_ACTIVE_TAG = "ServantCardParacelsusWorkshopActive";
   private static final String WORKSHOP_X_TAG = "ServantCardParacelsusWorkshopX";
   private static final String WORKSHOP_Y_TAG = "ServantCardParacelsusWorkshopY";
   private static final String WORKSHOP_Z_TAG = "ServantCardParacelsusWorkshopZ";
   private static final String WORKSHOP_RADIUS_TAG = "ServantCardParacelsusWorkshopRadius";
   private static final String SPIRIT_ACTIVE_TAG = "ServantCardParacelsusSpiritActive";
   private static final String SPIRIT_LAST_TICK_TAG = "ServantCardParacelsusSpiritLastTick";
   private static final String SPIRIT_MODE_TAG = "ServantCardParacelsusSpiritMode";
   private static final String PHILOSOPHER_STONE_STOCK_TAG = "ServantCardParacelsusPhilosopherStoneStock";
   private static final String DIAMOND_SHIELD_STOCK_TAG = "ServantCardParacelsusDiamondShieldStock";
   private static final String LAST_STONE_USE_TAG = "ServantCardParacelsusLastStoneUse";
   private static final String LAST_NP_TAG = "ServantCardParacelsusLastNp";
   private static final double WORKSHOP_RADIUS = 15.0;
   private static final int PHILOSOPHER_STONE_MAX_CHARGES = 3;
   private static final int DIAMOND_SHIELD_MAX_CHARGES = 3;
   private static final int PHILOSOPHER_STONE_INVULN_TICKS = 60;
   private static final double SPIRIT_MP_COST = 4.5;
   private static final long SPIRIT_INTERVAL = 20L;
   private static final DustParticleOptions FIRE = new DustParticleOptions(new Vector3f(1.0F, 0.28F, 0.28F), 1.2F);
   private static final DustParticleOptions WATER = new DustParticleOptions(new Vector3f(0.28F, 0.55F, 1.0F), 1.2F);
   private static final DustParticleOptions EARTH = new DustParticleOptions(new Vector3f(0.35F, 0.95F, 0.35F), 1.2F);
   private static final DustParticleOptions WIND = new DustParticleOptions(new Vector3f(0.95F, 0.95F, 1.0F), 1.2F);
   private static final DustParticleOptions AETHER = new DustParticleOptions(new Vector3f(1.0F, 0.84F, 0.42F), 1.2F);

   private record BlockBackup(BlockPos pos, BlockState state) {
   }

   private ServantCardParacelsusSkills() {
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!vars.servant_card_transformed || !"paracelsus".equals(vars.servant_card_id)) {
         clear(player);
         return;
      }
      tickWorkshop(player, vars);
      tickElementalSpirits(player, vars);
   }

   public static void clear(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.remove(WORKSHOP_ACTIVE_TAG);
      data.remove(WORKSHOP_X_TAG);
      data.remove(WORKSHOP_Y_TAG);
      data.remove(WORKSHOP_Z_TAG);
      data.remove(WORKSHOP_RADIUS_TAG);
      data.remove(SPIRIT_ACTIVE_TAG);
      data.remove(SPIRIT_LAST_TICK_TAG);
      data.remove(SPIRIT_MODE_TAG);
      data.remove(PHILOSOPHER_STONE_STOCK_TAG);
      data.remove(DIAMOND_SHIELD_STOCK_TAG);
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_paracelsus_stone_stock = 0;
      vars.servant_card_paracelsus_diamond_shield_stock = 0;
   }

   public static boolean performParacelsusWorkshop(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return false;
      }
      if (!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.workshop_overworld_only"), true);
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      CompoundTag data = player.getPersistentData();
      if (data.getBoolean(WORKSHOP_ACTIVE_TAG)) {
         if (!isInsideWorkshop(player)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.paracelsus_workshop_need_inside"), true);
            return false;
         }
         BattlefieldAreaService.unregister(level, player.getUUID(), BattlefieldAreaService.Type.WORKSHOP);
         clearWorkshop(data);
         level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 0.4, player.getZ(), 30, 0.7, 0.2, 0.7, 0.035);
         level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.75F, 1.25F);
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.paracelsus_workshop_removed"), true);
         return true;
      }
      if (!ServantCardManaService.consume(player, vars, 30.0)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
         return false;
      }
      data.putBoolean(WORKSHOP_ACTIVE_TAG, true);
      data.putDouble(WORKSHOP_X_TAG, player.getX());
      data.putDouble(WORKSHOP_Y_TAG, player.getY());
      data.putDouble(WORKSHOP_Z_TAG, player.getZ());
      data.putDouble(WORKSHOP_RADIUS_TAG, WORKSHOP_RADIUS);
      BattlefieldAreaService.register(level, player.getUUID(), BattlefieldAreaService.Type.WORKSHOP,
         workshopCenter(data), WORKSHOP_RADIUS, Long.MAX_VALUE);
      VFXServerEffects.spawn(level, "servant_medea_workshop", player, 128.0);
      spawnWorkshopHighlight(level, workshopCenter(data), WORKSHOP_RADIUS, true);
      level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 0.95F);
      return true;
   }

   public static boolean performParacelsusCraftStone(ServerPlayer player) {
      syncParacelsusStocks(player, player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES));
      ModNetwork.sendToPlayer(
         player,
         new OpenParacelsusCraftScreenMessage(
            getPhilosopherStoneStock(player), getDiamondShieldStock(player), countLeylineMaps(player)
         )
      );
      return true;
   }

   public static boolean craftSelectedParacelsusItem(ServerPlayer player, int choice) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || !"paracelsus".equals(vars.servant_card_id)) {
         return false;
      }
      if (!hasWorkshop(player)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.paracelsus_no_workshop"), true);
         return false;
      }
      if (!isInsideWorkshop(player)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.paracelsus_need_workshop"), true);
         return false;
      }
      if (choice == 0 && getPhilosopherStoneStock(player) >= PHILOSOPHER_STONE_MAX_CHARGES) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.paracelsus_stone_full", PHILOSOPHER_STONE_MAX_CHARGES), true);
         return false;
      }
      if (choice == 1 && getDiamondShieldStock(player) >= DIAMOND_SHIELD_MAX_CHARGES) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.paracelsus_diamond_shield_full", DIAMOND_SHIELD_MAX_CHARGES), true);
         return false;
      }
      if (!ServantCardManaService.consume(player, vars, adjustedWorkshopCost(player, 30.0))) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
         return false;
      }
      Component made;
      if (choice == 0) {
         setPhilosopherStoneStock(player, getPhilosopherStoneStock(player) + 1);
         made = Component.translatable("hud.typemoonworld.servant_card.paracelsus_stone");
      } else if (choice == 1) {
         setDiamondShieldStock(player, getDiamondShieldStock(player) + 1);
         made = Component.translatable("hud.typemoonworld.servant_card.paracelsus_diamond_shield");
      } else if (choice == 2) {
         giveCraftedItem(player, new ItemStack(ModItems.LEYLINE_SURVEY_MAP.get()));
         made = Component.translatable("item.typemoonworld.leyline_survey_map");
      } else {
         return false;
      }
      syncParacelsusStocks(player, vars);
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(), 28, 0.45, 0.35, 0.45, 0.02);
         level.sendParticles(AETHER, player.getX(), player.getY() + 1.05, player.getZ(), 22, 0.35, 0.3, 0.35, 0.01);
         level.playSound(null, player.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.9F, 1.2F);
      }
      player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.paracelsus_item_crafted", made), true);
      return true;
   }

   private static int countLeylineMaps(ServerPlayer player) {
      int count = 0;
      if (player.getMainHandItem().is(ModItems.LEYLINE_SURVEY_MAP.get())) {
         count += player.getMainHandItem().getCount();
      }
      if (player.getOffhandItem().is(ModItems.LEYLINE_SURVEY_MAP.get())) {
         count += player.getOffhandItem().getCount();
      }
      for (ItemStack stack : player.getInventory().items) {
         if (stack.is(ModItems.LEYLINE_SURVEY_MAP.get())) {
            count += stack.getCount();
         }
      }
      return count;
   }

   private static void giveCraftedItem(ServerPlayer player, ItemStack stack) {
      if (stack.isEmpty()) {
         return;
      }
      if (player.getMainHandItem().isEmpty()) {
         player.setItemInHand(InteractionHand.MAIN_HAND, stack);
      } else if (player.getOffhandItem().isEmpty()) {
         player.setItemInHand(InteractionHand.OFF_HAND, stack);
      } else if (!player.getInventory().add(stack)) {
         player.drop(stack, false);
      }
   }

   public static boolean toggleParacelsusSpirits(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      boolean active = !data.getBoolean(SPIRIT_ACTIVE_TAG);
      data.putBoolean(SPIRIT_ACTIVE_TAG, active);
      if (!active) {
         data.remove(SPIRIT_LAST_TICK_TAG);
      }
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(active ? ParticleTypes.ENCHANT : ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 28, 0.55, 0.4, 0.55, 0.025);
         level.playSound(null, player.blockPosition(), active ? SoundEvents.ENCHANTMENT_TABLE_USE : SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, active ? 1.25F : 0.75F);
      }
      player.displayClientMessage(Component.translatable(active ? "message.typemoonworld.servant_card.paracelsus_spirit_on" : "message.typemoonworld.servant_card.paracelsus_spirit_off"), true);
      return true;
   }

   public static boolean performParacelsusWorkshopTeleport(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return false;
      }
      if (!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.workshop_overworld_only"), true);
         return false;
      }
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(WORKSHOP_ACTIVE_TAG)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.paracelsus_no_workshop"), true);
         return false;
      }
      Vec3 center = workshopCenter(data);
      level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 0.8, player.getZ(), 30, 0.45, 0.55, 0.45, 0.08);
      player.teleportTo(center.x, center.y + 0.2, center.z);
      player.hurtMarked = true;
      player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, false, true, true));
      level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 0.8, player.getZ(), 30, 0.45, 0.55, 0.45, 0.08);
      spawnWorkshopHighlight(level, center, Math.max(3.0, data.getDouble(WORKSHOP_RADIUS_TAG)), false);
      level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.85F, 1.1F);
      return true;
   }

   public static void performParacelsusFireFurnace(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 center = aimGroundPoint(player, level, 28.0);
      BlockPos centerPos = BlockPos.containing(center);
      level.playSound(null, centerPos, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.15F, 0.72F);
      level.playSound(null, centerPos, SoundEvents.LAVA_AMBIENT, SoundSource.PLAYERS, 1.0F, 0.85F);
      for (int i = 0; i < 100; i += 4) {
         int delay = i;
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            if (!level.isLoaded(centerPos)) {
               return;
            }
            level.sendParticles(ParticleTypes.LAVA, center.x, center.y + 0.18, center.z, 18, 5.0, 0.08, 5.0, 0.0);
            level.sendParticles(FIRE, center.x, center.y + 0.25, center.z, 32, 5.2, 0.12, 5.2, 0.015);
            if (delay % 20 == 0) {
               igniteFirePillars(player, level, center, 3 + level.getRandom().nextInt(3));
            }
         });
      }
      TYPE_MOON_WORLD.queueServerWork(100, () -> {
         if (!level.isLoaded(centerPos)) {
            return;
         }
         level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.8, center.z, 3, 1.8, 0.6, 1.8, 0.0);
         level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + 0.4, center.z, 55, 4.8, 0.7, 4.8, 0.08);
         level.playSound(null, centerPos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.1F, 0.95F);
         for (LivingEntity target : hostilesInRadius(level, player, center, 8.5, 4.0)) {
            Vec3 push = target.position().subtract(center).multiply(1.0, 0.0, 1.0);
            if (push.lengthSqr() < 1.0E-4) {
               push = player.getLookAngle().multiply(1.0, 0.0, 1.0);
            }
            push = push.normalize().scale(1.35);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.55, push.z));
            target.hurtMarked = true;
         }
      });
   }

   public static void performParacelsusWaterPressure(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 center = aimGroundPoint(player, level, 30.0).add(0.0, 2.2, 0.0);
      BlockPos centerPos = BlockPos.containing(center);
      level.playSound(null, centerPos, SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.1F, 0.75F);
      level.sendParticles(WATER, center.x, center.y, center.z, 90, 2.5, 2.5, 2.5, 0.03);
      for (int tick = 0; tick <= 120; tick += 10) {
         int delay = tick;
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            if (!level.isLoaded(centerPos)) {
               return;
            }
            level.sendParticles(ParticleTypes.BUBBLE, center.x, center.y, center.z, 36, 2.7, 2.2, 2.7, 0.04);
            level.sendParticles(WATER, center.x, center.y, center.z, 26, 2.9, 2.0, 2.9, 0.025);
            for (LivingEntity target : hostilesInRadius(level, player, center, 3.5, 3.5)) {
               target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3, false, true, true));
               target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, false, true, true));
               Vec3 pull = center.subtract(target.position().add(0.0, target.getBbHeight() * 0.5, 0.0)).scale(0.045);
               target.setDeltaMovement(target.getDeltaMovement().scale(0.35).add(pull.x, -0.08, pull.z));
               target.hurtMarked = true;
               if (delay % 20 == 0) {
                  target.invulnerableTime = 0;
                  target.hurt(player.damageSources().magic(), (float)applyWorkshopDamage(player, 30.0));
               }
            }
         });
      }
      TYPE_MOON_WORLD.queueServerWork(120, () -> {
         level.sendParticles(WATER, center.x, center.y, center.z, 120, 5.8, 0.35, 5.8, 0.13);
         level.sendParticles(ParticleTypes.SPLASH, center.x, center.y, center.z, 90, 5.5, 0.55, 5.5, 0.22);
         level.playSound(null, centerPos, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.3F, 0.7F);
         for (LivingEntity target : hostilesInRadius(level, player, center, 10.0, 4.0)) {
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().magic(), (float)applyWorkshopDamage(player, 50.0));
            Vec3 push = target.position().subtract(center).multiply(1.0, 0.0, 1.0);
            if (push.lengthSqr() > 1.0E-4) {
               push = push.normalize().scale(1.85);
               target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.42, push.z));
               target.hurtMarked = true;
            }
         }
      });
   }

   public static void performParacelsusEarthRoar(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 center = aimGroundPoint(player, level, 32.0);
      BlockPos centerPos = BlockPos.containing(center);
      level.playSound(null, centerPos, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.1F, 0.58F);
      level.sendParticles(EARTH, center.x, center.y + 0.15, center.z, 90, 6.5, 0.2, 6.5, 0.035);
      List<BlockBackup> backups = new ArrayList<>();
      int pillars = 3 + level.getRandom().nextInt(4);
      for (int i = 0; i < pillars; i++) {
         BlockPos base = randomGroundNear(level, center, 11.0);
         int height = 3 + level.getRandom().nextInt(4);
         int delay = i * 5;
         TYPE_MOON_WORLD.queueServerWork(delay, () -> raiseEarthPillar(player, level, base, height, backups));
      }
      int cracks = 3 + level.getRandom().nextInt(3);
      for (int i = 0; i < cracks; i++) {
         double angle = Math.PI * 2.0 * i / cracks + level.getRandom().nextDouble() * 0.7;
         int delay = 18 + i * 3;
         TYPE_MOON_WORLD.queueServerWork(delay, () -> carveEarthCrack(player, level, center, angle, backups));
      }
      for (int delay = 34; delay <= 78; delay += 5) {
         TYPE_MOON_WORLD.queueServerWork(delay, () -> dropRockImpacts(player, level, center));
      }
      TYPE_MOON_WORLD.queueServerWork(150, () -> restoreBackups(level, backups));
   }

   public static void performParacelsusWindCut(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 forward = player.getLookAngle().normalize();
      Vec3 horizontal = new Vec3(forward.x, 0.0, forward.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         horizontal = new Vec3(0.0, 0.0, 1.0);
      } else {
         horizontal = horizontal.normalize();
      }
      Vec3 side = new Vec3(-horizontal.z, 0.0, horizontal.x).normalize();
      Vec3 bladeForward = horizontal;
      Vec3 origin = player.getEyePosition().add(0.0, -0.25, 0.0);
      int blades = 3 + level.getRandom().nextInt(3);
      List<BlockBackup> backups = new ArrayList<>();
      level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.95F);
      for (int i = 0; i < blades; i++) {
         double sideOffset = (i - (blades - 1) * 0.5) * 0.75;
         int delay = i * 3;
         TYPE_MOON_WORLD.queueServerWork(delay, () -> releaseWindBlade(player, level, origin.add(side.scale(sideOffset)), bladeForward, side, backups));
      }
      TYPE_MOON_WORLD.queueServerWork(80, () -> restoreBackups(level, backups));
   }

   public static boolean usePhilosopherStone(ServerPlayer player) {
      long now = player.level().getGameTime();
      CompoundTag data = player.getPersistentData();
      if (now - data.getLong(LAST_STONE_USE_TAG) < 900L) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.cooldown", String.format(java.util.Locale.ROOT, "%.1f", (900L - (now - data.getLong(LAST_STONE_USE_TAG))) / 20.0F)), true);
         return false;
      }
      int stock = getPhilosopherStoneStock(player);
      if (stock <= 0) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.paracelsus_no_stone"), true);
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!ServantCardManaService.consume(player, vars, adjustedWorkshopCost(player, 20.0))) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
         return false;
      }
      data.putLong(LAST_STONE_USE_TAG, now);
      setPhilosopherStoneStock(player, stock - 1);
      syncParacelsusStocks(player, vars);
      player.heal(player.getMaxHealth() - player.getHealth());
      clearHarmfulEffects(player);
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, PHILOSOPHER_STONE_INVULN_TICKS, 1, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, PHILOSOPHER_STONE_INVULN_TICKS, 2, false, true, true));
      player.invulnerableTime = Math.max(player.invulnerableTime, PHILOSOPHER_STONE_INVULN_TICKS);
      if (player.level() instanceof ServerLevel level) {
         VFXServerEffects.spawn(level, "paracelsus_philosopher_stone", player, 128.0);
         level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + player.getBbHeight() * 0.8, player.getZ(), 18, 0.2, 0.3, 0.2, 0.02);
         level.sendParticles(AETHER, player.getX(), player.getY() + player.getBbHeight() * 0.8, player.getZ(), 26, 0.25, 0.35, 0.25, 0.01);
         level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9F, 1.3F);
      }
      player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.paracelsus_stone_used", getPhilosopherStoneStock(player)), true);
      return true;
   }

   public static boolean useDiamondShield(ServerPlayer player) {
      int stock = getDiamondShieldStock(player);
      if (stock <= 0) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.paracelsus_no_diamond_shield"), true);
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      setDiamondShieldStock(player, stock - 1);
      syncParacelsusStocks(player, vars);
      if (player.level() instanceof ServerLevel level) {
         RhoAiasEntity shield = new RhoAiasEntity(level, player, findLookTarget(player, 18.0, 2.0));
         shield.setPos(player.getX(), player.getY() + player.getBbHeight() * 0.55, player.getZ());
         level.addFreshEntity(shield);
         player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 1, false, true, true));
         player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 0, false, true, true));
         level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 36, 0.65, 0.5, 0.65, 0.03);
         level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.8F);
      }
      player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.paracelsus_diamond_shield_used", getDiamondShieldStock(player)), true);
      return true;
   }

   private static void tickPhilosopherStoneAutoUse(ServerPlayer player) {
      if (player.tickCount % 20 != 0 || player.getHealth() > player.getMaxHealth() * 0.5F) {
         return;
      }
      usePhilosopherStone(player);
   }

   public static boolean useElementalSword(ServerPlayer player, boolean advanced) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || !"paracelsus".equals(vars.servant_card_id)) {
         return false;
      }
      long now = player.level().getGameTime();
      int cooldown = advanced ? 3600 : 1800;
      CompoundTag data = player.getPersistentData();
      long last = data.getLong(LAST_NP_TAG + (advanced ? "Advanced" : "Normal"));
      if (now - last < cooldown) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.cooldown", String.format(java.util.Locale.ROOT, "%.1f", (cooldown - (now - last)) / 20.0F)), true);
         return false;
      }
      double cost = advanced ? 140.0 : 80.0;
      if (!ServantCardManaService.consume(player, vars, adjustedWorkshopCost(player, cost))) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
         return false;
      }
      data.putLong(LAST_NP_TAG + (advanced ? "Advanced" : "Normal"), now);
      releaseElementalSword(player, advanced);
      return true;
   }

   public static boolean hasWorkshop(ServerPlayer player) {
      return player.getPersistentData().getBoolean(WORKSHOP_ACTIVE_TAG);
   }

   public static int getPhilosopherStoneStock(ServerPlayer player) {
      return Mth.clamp(player.getPersistentData().getInt(PHILOSOPHER_STONE_STOCK_TAG), 0, PHILOSOPHER_STONE_MAX_CHARGES);
   }

   private static void setPhilosopherStoneStock(ServerPlayer player, int value) {
      player.getPersistentData().putInt(PHILOSOPHER_STONE_STOCK_TAG, Mth.clamp(value, 0, PHILOSOPHER_STONE_MAX_CHARGES));
   }

   public static int getDiamondShieldStock(ServerPlayer player) {
      return Mth.clamp(player.getPersistentData().getInt(DIAMOND_SHIELD_STOCK_TAG), 0, DIAMOND_SHIELD_MAX_CHARGES);
   }

   private static void setDiamondShieldStock(ServerPlayer player, int value) {
      player.getPersistentData().putInt(DIAMOND_SHIELD_STOCK_TAG, Mth.clamp(value, 0, DIAMOND_SHIELD_MAX_CHARGES));
   }

   private static void syncParacelsusStocks(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      vars.servant_card_paracelsus_stone_stock = getPhilosopherStoneStock(player);
      vars.servant_card_paracelsus_diamond_shield_stock = getDiamondShieldStock(player);
      vars.syncServantCardRuntime(player);
   }

   public static boolean openElementalGuardianScreen(ServerPlayer player) {
      ModNetwork.sendToPlayer(player, new OpenParacelsusElementScreenMessage());
      return true;
   }

   public static boolean deploySelectedElementalGuardian(ServerPlayer player, int element) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || !"paracelsus".equals(vars.servant_card_id) || !(player.level() instanceof ServerLevel level)) {
         return false;
      }
      if (!ServantCardManaService.consume(player, vars, adjustedWorkshopCost(player, 26.0))) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
         return false;
      }
      Vec3 pos = aimGroundPoint(player, level, 24.0).add(0.0, 1.4, 0.0);
      ParacelsusSpiritCannonEntity guardian = ParacelsusSpiritCannonEntity.summonGuardian(level, player, pos, element, 120 * 20);
      level.addFreshEntity(guardian);
      level.sendParticles(AETHER, pos.x, pos.y, pos.z, 32, 0.35, 0.35, 0.35, 0.02);
      level.sendParticles(ParticleTypes.ENCHANT, pos.x, pos.y, pos.z, 40, 0.45, 0.45, 0.45, 0.03);
      level.playSound(null, BlockPos.containing(pos), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.85F, 1.35F);
      vars.syncMana(player);
      return true;
   }

   private static void tickWorkshop(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(WORKSHOP_ACTIVE_TAG) || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 center = workshopCenter(data);
      double radius = data.getDouble(WORKSHOP_RADIUS_TAG);
      if (radius <= 0.0) {
         radius = WORKSHOP_RADIUS;
         data.putDouble(WORKSHOP_RADIUS_TAG, radius);
      }
      if (player.tickCount % 20 == 0) {
         spawnWorkshopHighlight(level, center, radius, false);
      }
      if (isInsideWorkshop(player) && player.tickCount % 10 == 0) {
         vars.servant_card_mana = Math.min(vars.servant_card_max_mana, vars.servant_card_mana + 5.5);
         vars.syncMana(player);
         player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, 0, false, false, false));
      }
   }

   private static void tickElementalSpirits(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(SPIRIT_ACTIVE_TAG) || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      long now = level.getGameTime();
      if (now % 6L == 0L) {
         spawnElementalSpiritAura(level, player, now);
      }
      if (now - data.getLong(SPIRIT_LAST_TICK_TAG) < SPIRIT_INTERVAL) {
         return;
      }
      data.putLong(SPIRIT_LAST_TICK_TAG, now);
      LivingEntity target = findLookTarget(player, 24.0, 2.2);
      if (target == null || !isValidTarget(player, target)) {
         target = findNearestHostile(level, player, 22.0);
      }
      if (target == null) {
         return;
      }
      double cost = adjustedWorkshopCost(player, SPIRIT_MP_COST);
      if (!ServantCardManaService.consume(player, vars, cost)) {
         data.putBoolean(SPIRIT_ACTIVE_TAG, false);
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.paracelsus_spirit_off"), true);
         return;
      }
      int mode = player.getRandom().nextInt(4);
      data.putInt(SPIRIT_MODE_TAG, mode);
      fireElementalSpirit(player, level, target, mode);
   }

   private static void fireElementalSpirit(ServerPlayer player, ServerLevel level, LivingEntity target, int mode) {
      Vec3 origin = player.position().add(0.0, player.getBbHeight() * 0.78, 0.0);
      Vec3 targetPos = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      Vec3 dir = targetPos.subtract(origin).normalize();
      Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
      if (side.lengthSqr() < 1.0E-4) {
         side = new Vec3(1.0, 0.0, 0.0);
      } else {
         side = side.normalize();
      }
      Vec3 spawn = origin.add(side.scale((mode - 1.5) * 0.32)).add(0.0, 0.18 * Math.sin(player.tickCount * 0.3 + mode), 0.0);
      if (mode == 3) {
         MedeaBeamEffectEntity beam = new MedeaBeamEffectEntity(level, player, spawn, targetPos, (float)applyWorkshopDamage(player, 12.0), 6);
         beam.setBreakBlocks(false);
         level.addFreshEntity(beam);
         level.playSound(null, BlockPos.containing(spawn), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 0.55F, 1.5F);
         return;
      }
      MedeaMagicBoltEntity bolt = new MedeaMagicBoltEntity(level, player);
      bolt.setMode(switch (mode) {
         case 0 -> MedeaMagicBoltEntity.Mode.FIRE_BOLT;
         case 1 -> MedeaMagicBoltEntity.Mode.FROST_BOLT;
         default -> MedeaMagicBoltEntity.Mode.SUPER_BOLT;
      });
      bolt.setMagicDamage((float)applyWorkshopDamage(player, mode == 2 ? 14.0 : 12.0));
      bolt.setPos(spawn.x, spawn.y, spawn.z);
      bolt.shoot(dir.x, dir.y, dir.z, 2.45F, 0.08F);
      level.addFreshEntity(bolt);
      level.playSound(null, BlockPos.containing(spawn), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.55F, 1.25F + mode * 0.08F);
   }

   private static void releaseElementalSword(ServerPlayer player, boolean advanced) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      PlayerNoblePhantasmHelper.finishServantCardVoiceSession(player, "paracelsus", ModSounds.PARACELSUS_VOICE_NP.get(), ModSounds.PARACELSUS_VOICE_NP_SHORT.get());
      ServantCardVoiceHelper.tryPlaySkill(player, "paracelsus_sword_np");
      float damage = advanced ? 1000.0F : 500.0F;
      double range = advanced ? 100.0 : 54.0;
      double halfAngle = advanced ? 22.5 : 18.0;
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 1, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0, false, true, true));
      Vec3 forward = PlayerNoblePhantasmHelper.horizontalLook(player);
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x).normalize();
      Vec3 origin = player.position().add(0.0, player.getBbHeight() * 0.7, 0.0);
      AABB search = player.getBoundingBox().inflate(range).expandTowards(forward.scale(range));
      double cos = Math.cos(Math.toRadians(halfAngle));
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, search, e -> e.isAlive() && e != player && isValidTarget(player, e))) {
         Vec3 to = living.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
         if (to.lengthSqr() < 1.0E-4 || to.lengthSqr() > range * range || to.normalize().dot(forward) < cos) {
            continue;
         }
         living.invulnerableTime = 0;
         living.hurt(player.damageSources().magic(), (float)applyWorkshopDamage(player, damage));
         living.invulnerableTime = 0;
         living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, advanced ? 300 : 180, 0, false, true, true));
         living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, advanced ? 300 : 180, 0, false, true, true));
         Vec3 impact = living.position().add(0.0, living.getBbHeight() * 0.45, 0.0);
         level.sendParticles(ParticleTypes.END_ROD, impact.x, impact.y, impact.z, 14, 0.35, 0.25, 0.35, 0.04);
      }
      VFXServerEffects.spawn(level, "paracelsus_noble_phantasm", player, advanced ? 2.5F : 1.5F);
      spawnNoblePhantasmFx(level, player, origin);
      breakElementalSwordTerrain(level, player, forward, side, range, advanced);
      level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.1F);
   }

   private static void spawnNoblePhantasmFx(ServerLevel level, ServerPlayer player, Vec3 center) {
      level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.2, center.z, 34, 0.45, 0.15, 0.45, 0.05);
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.4, center.z, 20, 0.35, 0.2, 0.35, 0.03);
      level.sendParticles(FIRE, center.x + 0.7, center.y, center.z, 10, 0.12, 0.12, 0.12, 0.01);
      level.sendParticles(WATER, center.x - 0.7, center.y, center.z, 10, 0.12, 0.12, 0.12, 0.01);
      level.sendParticles(EARTH, center.x, center.y, center.z + 0.7, 10, 0.12, 0.12, 0.12, 0.01);
      level.sendParticles(WIND, center.x, center.y, center.z - 0.7, 10, 0.12, 0.12, 0.12, 0.01);
      level.sendParticles(AETHER, center.x, center.y + 0.8, center.z, 20, 0.25, 0.2, 0.25, 0.02);
   }

   private static void breakElementalSwordTerrain(ServerLevel level, ServerPlayer player, Vec3 forward, Vec3 side, double range, boolean advanced) {
      int length = advanced ? 100 : 54;
      int maxBroken = advanced ? 220 : 110;
      int broken = 0;
      int vertical = advanced ? 4 : 3;
      BlockPos center = player.blockPosition();
      for (int step = 1; step <= length && broken < maxBroken; step++) {
         double fan = Math.max(1.0, step * Math.tan(Math.toRadians(advanced ? 22.5 : 18.0)));
         int sideRange = Mth.ceil(fan);
         Vec3 base = player.position().add(forward.scale(step));
         for (int sideStep = -sideRange; sideStep <= sideRange && broken < maxBroken; sideStep++) {
            for (int y = -1; y <= vertical && broken < maxBroken; y++) {
               Vec3 posVec = new Vec3(base.x, center.getY() + y + 0.5, base.z).add(side.scale(sideStep));
               BlockPos pos = BlockPos.containing(posVec);
               if (player.position().distanceToSqr(posVec) > range * range || !destroyFanBlock(level, pos, advanced)) {
                  continue;
               }
               broken++;
            }
         }
      }
   }

   private static boolean destroyFanBlock(ServerLevel level, BlockPos pos, boolean advanced) {
      var state = level.getBlockState(pos);
      float hardness = state.getDestroySpeed(level, pos);
      float cap = advanced ? 70.0F : 35.0F;
      if (state.isAir() || hardness < 0.0F || hardness >= cap || state.getFluidState().isSource()) {
         return false;
      }
      boolean removed = level.removeBlock(pos, false);
      if (removed) {
         level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.15, 0.15, 0.15, 0.02);
      }
      return removed;
   }

   private static Vec3 aimGroundPoint(ServerPlayer player, ServerLevel level, double range) {
      Vec3 eye = player.getEyePosition();
      Vec3 end = eye.add(player.getLookAngle().scale(range));
      HitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
      Vec3 target = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : end;
      BlockPos pos = BlockPos.containing(target);
      if (hit instanceof BlockHitResult blockHit && blockHit.getDirection() != Direction.UP) {
         pos = blockHit.getBlockPos().relative(blockHit.getDirection());
      }
      for (int y = Math.min(level.getMaxBuildHeight() - 2, pos.getY() + 4); y >= level.getMinBuildHeight() + 1; y--) {
         BlockPos check = new BlockPos(pos.getX(), y, pos.getZ());
         BlockState state = level.getBlockState(check);
         BlockState above = level.getBlockState(check.above());
         if (!state.isAir() && state.blocksMotion() && (above.isAir() || above.canBeReplaced())) {
            return Vec3.atCenterOf(check.above()).add(0.0, -0.5, 0.0);
         }
      }
      return target;
   }

   private static List<LivingEntity> hostilesInRadius(ServerLevel level, ServerPlayer player, Vec3 center, double horizontalRadius, double verticalRadius) {
      AABB area = new AABB(center.x - horizontalRadius, center.y - verticalRadius, center.z - horizontalRadius, center.x + horizontalRadius, center.y + verticalRadius, center.z + horizontalRadius);
      double radiusSqr = horizontalRadius * horizontalRadius;
      List<LivingEntity> result = new ArrayList<>();
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive() && e != player && isValidTarget(player, e))) {
         Vec3 flat = living.position().subtract(center).multiply(1.0, 0.0, 1.0);
         if (flat.lengthSqr() <= radiusSqr) {
            result.add(living);
         }
      }
      return result;
   }

   private static void scorchFireTerrain(ServerPlayer player, ServerLevel level, BlockPos center, int radius, List<BlockBackup> backups) {
      int changed = 0;
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 1, radius))) {
         if (changed >= 120) {
            break;
         }
         BlockPos immutable = pos.immutable();
         double dist = Math.sqrt(immutable.distToCenterSqr(center.getX() + 0.5, immutable.getY() + 0.5, center.getZ() + 0.5));
         if (dist > radius || level.getRandom().nextFloat() > 0.32F) {
            continue;
         }
         BlockState state = level.getBlockState(immutable);
         if (state.isAir() || state.hasBlockEntity() || state.is(Blocks.BEDROCK)) {
            continue;
         }
         float hardness = state.getDestroySpeed(level, immutable);
         if (hardness < 0.0F || hardness > 8.0F) {
            continue;
         }
         if (state.blocksMotion()) {
            BlockPos above = immutable.above();
            if (level.getBlockState(above).isAir() || level.getBlockState(above).canBeReplaced()) {
               backupAndSet(level, backups, above, Blocks.LAVA.defaultBlockState());
               changed++;
            }
            if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL)) {
               backupAndSet(level, backups, immutable, Blocks.MAGMA_BLOCK.defaultBlockState());
               changed++;
            } else if (hardness <= 2.2F) {
               backupAndSet(level, backups, immutable, Blocks.AIR.defaultBlockState());
               changed++;
            }
         } else if (hardness <= 2.0F) {
            backupAndSet(level, backups, immutable, Blocks.AIR.defaultBlockState());
            changed++;
         }
      }
   }

   private static void igniteFirePillars(ServerPlayer player, ServerLevel level, Vec3 center, int count) {
      for (int i = 0; i < count; i++) {
         double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
         double distance = 1.2 + level.getRandom().nextDouble() * 6.6;
         Vec3 pos = center.add(Math.cos(angle) * distance, 0.2, Math.sin(angle) * distance);
         level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 1.5, pos.z, 42, 0.35, 1.45, 0.35, 0.07);
         level.sendParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.45, pos.z, 14, 0.25, 0.18, 0.25, 0.0);
         level.playSound(null, BlockPos.containing(pos), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7F, 0.75F + level.getRandom().nextFloat() * 0.2F);
         for (LivingEntity target : hostilesInRadius(level, player, pos, 2.1, 3.4)) {
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().magic(), (float)applyWorkshopDamage(player, 80.0));
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 100));
            for (int burn = 20; burn <= 100; burn += 20) {
               TYPE_MOON_WORLD.queueServerWork(burn, () -> {
                  if (target.isAlive() && target.level() == level && target.getRemainingFireTicks() > 0) {
                     target.invulnerableTime = 0;
                     target.hurt(player.damageSources().magic(), (float)applyWorkshopDamage(player, 15.0));
                  }
               });
            }
         }
      }
   }

   private static List<BlockBackup> placeTemporaryWaterSphere(ServerLevel level, BlockPos center, int radius) {
      List<BlockBackup> placed = new ArrayList<>();
      int cap = 96;
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
         if (placed.size() >= cap) {
            break;
         }
         BlockPos immutable = pos.immutable();
         if (immutable.distSqr(center) > radius * radius + 1) {
            continue;
         }
         BlockState state = level.getBlockState(immutable);
         if ((state.isAir() || state.canBeReplaced()) && !state.hasBlockEntity()) {
            backupAndSet(level, placed, immutable, Blocks.WATER.defaultBlockState());
         }
      }
      return placed;
   }

   private static BlockPos randomGroundNear(ServerLevel level, Vec3 center, double radius) {
      double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
      double distance = level.getRandom().nextDouble() * radius;
      int x = Mth.floor(center.x + Math.cos(angle) * distance);
      int z = Mth.floor(center.z + Math.sin(angle) * distance);
      int startY = Mth.clamp(Mth.floor(center.y) + 5, level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 2);
      for (int y = startY; y >= level.getMinBuildHeight() + 1; y--) {
         BlockPos pos = new BlockPos(x, y, z);
         BlockState state = level.getBlockState(pos);
         if (!state.isAir() && state.blocksMotion()) {
            return pos.above();
         }
      }
      return BlockPos.containing(center);
   }

   private static void raiseEarthPillar(ServerPlayer player, ServerLevel level, BlockPos base, int height, List<BlockBackup> backups) {
      level.playSound(null, base, SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.2F, 0.55F);
      for (int y = 0; y < height; y++) {
         int delay = y * 2;
         int step = y;
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            BlockPos pos = base.above(step);
            if (level.getBlockState(pos).isAir() || level.getBlockState(pos).canBeReplaced() || canBreakSoftTerrain(level, pos, 5.0F)) {
               backupAndSet(level, backups, pos, (step % 2 == 0 ? Blocks.STONE : Blocks.COBBLESTONE).defaultBlockState());
               level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.35, pos.getZ() + 0.5, 8, 0.35, 0.15, 0.35, 0.04);
               for (LivingEntity target : hostilesInRadius(level, player, Vec3.atCenterOf(pos), 1.7, 2.2)) {
                  target.invulnerableTime = 0;
                  target.hurt(player.damageSources().magic(), (float)applyWorkshopDamage(player, 60.0));
                  target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.85, 0.0));
                  target.hurtMarked = true;
               }
            }
         });
      }
   }

   private static void dropRockImpacts(ServerPlayer player, ServerLevel level, Vec3 center) {
      Vec3 impact = center.add((level.getRandom().nextDouble() - 0.5) * 15.0, 0.3, (level.getRandom().nextDouble() - 0.5) * 15.0);
      BlockPos pos = BlockPos.containing(impact);
      level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.STONE.defaultBlockState()), impact.x, impact.y + 3.0, impact.z, 24, 0.6, 1.0, 0.6, 0.05);
      level.sendParticles(ParticleTypes.POOF, impact.x, impact.y + 0.3, impact.z, 18, 0.65, 0.22, 0.65, 0.08);
      level.playSound(null, pos, SoundEvents.DEEPSLATE_BREAK, SoundSource.PLAYERS, 0.75F, 0.65F);
      for (LivingEntity target : hostilesInRadius(level, player, impact, 2.4, 4.2)) {
         target.invulnerableTime = 0;
         target.hurt(player.damageSources().magic(), (float)applyWorkshopDamage(player, 40.0));
      }
   }

   private static void carveEarthCrack(ServerPlayer player, ServerLevel level, Vec3 center, double angle, List<BlockBackup> backups) {
      Vec3 dir = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
      for (int step = 1; step <= 12; step++) {
         Vec3 at = center.add(dir.scale(step));
         BlockPos ground = randomGroundAt(level, at);
         if (ground == null) {
            continue;
         }
         BlockPos open = ground.above();
         BlockState state = level.getBlockState(open);
         if (state.isAir() || state.canBeReplaced() || canBreakSoftTerrain(level, open, 6.0F)) {
            backupAndSet(level, backups, open, Blocks.AIR.defaultBlockState());
         }
         if (step % 2 == 0) {
            backupAndSet(level, backups, ground, Blocks.DEEPSLATE.defaultBlockState());
         }
         Vec3 crackPos = Vec3.atCenterOf(open);
         level.sendParticles(EARTH, crackPos.x, crackPos.y, crackPos.z, 9, 0.35, 0.1, 0.35, 0.02);
         for (LivingEntity target : hostilesInRadius(level, player, crackPos, 1.8, 2.0)) {
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().magic(), (float)applyWorkshopDamage(player, 80.0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 9, false, true, true));
         }
      }
   }

   private static BlockPos randomGroundAt(ServerLevel level, Vec3 pos) {
      int x = Mth.floor(pos.x);
      int z = Mth.floor(pos.z);
      int startY = Mth.clamp(Mth.floor(pos.y) + 4, level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 2);
      for (int y = startY; y >= level.getMinBuildHeight() + 1; y--) {
         BlockPos check = new BlockPos(x, y, z);
         BlockState state = level.getBlockState(check);
         if (!state.isAir() && state.blocksMotion()) {
            return check;
         }
      }
      return null;
   }

   private static void releaseWindBlade(ServerPlayer player, ServerLevel level, Vec3 origin, Vec3 forward, Vec3 side, List<BlockBackup> backups) {
      Set<Integer> damaged = new HashSet<>();
      int broken = 0;
      for (int step = 1; step <= 20; step++) {
         Vec3 center = origin.add(forward.scale(step));
         level.sendParticles(WIND, center.x, center.y, center.z, 5, 0.12, 0.12, 0.12, 0.01);
         level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
         for (int sideStep = -1; sideStep <= 1; sideStep++) {
            Vec3 cut = center.add(side.scale(sideStep));
            BlockPos base = BlockPos.containing(cut);
            for (int y = -1; y <= 1; y++) {
               if (broken >= 80) {
                  break;
               }
               BlockPos pos = base.offset(0, y, 0);
               if (destroyWindCutBlock(level, pos, backups)) {
                  broken++;
               }
            }
         }
         AABB area = new AABB(center.x - 1.6, center.y - 1.2, center.z - 1.6, center.x + 1.6, center.y + 1.8, center.z + 1.6);
         for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive() && e != player && isValidTarget(player, e))) {
            if (!damaged.add(target.getId())) {
               continue;
            }
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().magic(), (float)applyWorkshopDamage(player, 70.0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, 10, false, true, true));
            Vec3 pull = center.subtract(target.position()).multiply(0.08, 0.0, 0.08);
            target.setDeltaMovement(target.getDeltaMovement().scale(0.45).add(pull.x, 0.03, pull.z));
            target.hurtMarked = true;
            level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 12, 0.25, 0.25, 0.25, 0.04);
         }
      }
   }

   private static boolean destroyWindCutBlock(ServerLevel level, BlockPos pos, List<BlockBackup> backups) {
      BlockState state = level.getBlockState(pos);
      if (state.isAir() || state.hasBlockEntity() || state.is(Blocks.BEDROCK) || state.is(Blocks.END_PORTAL_FRAME)) {
         return false;
      }
      float hardness = state.getDestroySpeed(level, pos);
      if (hardness < 0.0F || hardness > 4.0F) {
         return false;
      }
      boolean soft = !state.blocksMotion() || hardness <= 1.5F || state.is(Blocks.OAK_LEAVES) || state.is(Blocks.SPRUCE_LEAVES) || state.is(Blocks.BIRCH_LEAVES) || state.is(Blocks.JUNGLE_LEAVES) || state.is(Blocks.ACACIA_LEAVES) || state.is(Blocks.DARK_OAK_LEAVES) || state.is(Blocks.MANGROVE_LEAVES) || state.is(Blocks.CHERRY_LEAVES);
      if (!soft && hardness > 2.5F) {
         return false;
      }
      backupAndSet(level, backups, pos, Blocks.AIR.defaultBlockState());
      level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.12, 0.12, 0.12, 0.02);
      return true;
   }

   private static void backupAndSet(ServerLevel level, List<BlockBackup> backups, BlockPos pos, BlockState state) {
      BlockPos immutable = pos.immutable();
      BlockState previous = level.getBlockState(immutable);
      if (previous.hasBlockEntity() || previous.is(Blocks.BEDROCK) || previous.is(Blocks.END_PORTAL_FRAME)) {
         return;
      }
      boolean alreadyBackedUp = false;
      for (BlockBackup backup : backups) {
         if (backup.pos().equals(immutable)) {
            alreadyBackedUp = true;
            break;
         }
      }
      if (!alreadyBackedUp) {
         backups.add(new BlockBackup(immutable, previous));
      }
      level.setBlock(immutable, state, 3);
   }

   private static void restoreBackups(ServerLevel level, List<BlockBackup> backups) {
      for (int i = backups.size() - 1; i >= 0; i--) {
         BlockBackup backup = backups.get(i);
         if (level.isLoaded(backup.pos()) && !level.getBlockState(backup.pos()).hasBlockEntity()) {
            level.setBlock(backup.pos(), backup.state(), 3);
         }
      }
      backups.clear();
   }

   private static boolean canBreakSoftTerrain(ServerLevel level, BlockPos pos, float cap) {
      BlockState state = level.getBlockState(pos);
      float hardness = state.getDestroySpeed(level, pos);
      return !state.isAir() && !state.hasBlockEntity() && !state.is(Blocks.BEDROCK) && !state.is(Blocks.END_PORTAL_FRAME) && hardness >= 0.0F && hardness <= cap;
   }

   private static LivingEntity findNearestHostile(ServerLevel level, ServerPlayer player, double radius) {
      AABB area = player.getBoundingBox().inflate(radius, 8.0, radius);
      LivingEntity best = null;
      double bestDistance = Double.MAX_VALUE;
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive() && e != player && isValidTarget(player, e))) {
         double distance = living.distanceToSqr(player);
         if (distance < bestDistance) {
            bestDistance = distance;
            best = living;
         }
      }
      return best;
   }

   private static boolean isValidTarget(ServerPlayer player, LivingEntity living) {
      if (living == null || living == player || player.isAlliedTo(living) || living.isAlliedTo(player) || EntityUtils.isImmunePlayerTarget(living)) {
         return false;
      }
      if (living instanceof Player targetPlayer) {
         return !EntityUtils.isImmunePlayerTarget(targetPlayer);
      }
      if (living instanceof NeutralMob neutral) {
         return neutral.isAngry();
      }
      return living instanceof Monster || (living instanceof Mob mob && mob.getTarget() == player);
   }

   private static void clearHarmfulEffects(ServerPlayer player) {
      List<MobEffectInstance> active = new ArrayList<>(player.getActiveEffects());
      for (MobEffectInstance effect : active) {
         if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            player.removeEffect(effect.getEffect());
         }
      }
   }

   private static boolean isInsideWorkshop(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(WORKSHOP_ACTIVE_TAG)) {
         return false;
      }
      Vec3 center = workshopCenter(data);
      double radius = data.getDouble(WORKSHOP_RADIUS_TAG);
      return center.distanceToSqr(player.position()) <= radius * radius;
   }

   private static Vec3 workshopCenter(CompoundTag data) {
      return new Vec3(data.getDouble(WORKSHOP_X_TAG), data.getDouble(WORKSHOP_Y_TAG), data.getDouble(WORKSHOP_Z_TAG));
   }

   private static void clearWorkshop(CompoundTag data) {
      data.remove(WORKSHOP_ACTIVE_TAG);
      data.remove(WORKSHOP_X_TAG);
      data.remove(WORKSHOP_Y_TAG);
      data.remove(WORKSHOP_Z_TAG);
      data.remove(WORKSHOP_RADIUS_TAG);
   }

   private static double adjustedWorkshopCost(ServerPlayer player, double baseCost) {
      return isInsideWorkshop(player) ? baseCost * 0.85 : baseCost;
   }

   private static double applyWorkshopDamage(ServerPlayer player, double baseDamage) {
      double halved = baseDamage * 0.5;
      double currentDamage = isInsideWorkshop(player) ? halved * 1.18 : halved;
      return net.xxxjk.TYPE_MOON_WORLD.servant.entity.ParacelsusBalanceRules.reduceDamage(currentDamage);
   }

   private static void spawnWorkshopHighlight(ServerLevel level, Vec3 center, double radius, boolean burst) {
      level.addFreshEntity(new ProjectionCircuitEffectEntity(level, center.x, center.y + 0.06, center.z, (float)(burst ? 1.0 : radius * 0.92), (float)radius, burst ? 0.32F : 0.18F, burst ? 34 : 26, 0x9FDFFF));
      int points = burst ? 96 : 64;
      for (int i = 0; i < points; i++) {
         double angle = Math.PI * 2.0 * i / points;
         double x = center.x + Math.cos(angle) * radius;
         double z = center.z + Math.sin(angle) * radius;
         level.sendParticles(ParticleTypes.ENCHANT, x, center.y + 0.16, z, 1, 0.0, 0.015, 0.0, 0.0);
         if (burst && i % 8 == 0) {
            level.sendParticles(AETHER, x, center.y + 0.18, z, 1, 0.02, 0.04, 0.02, 0.0);
         }
      }
   }

   private static void spawnElementalSpiritAura(ServerLevel level, ServerPlayer player, long now) {
      double baseX = player.getX();
      double baseY = player.getY() + player.getBbHeight() * 0.75;
      double baseZ = player.getZ();
      double orbit = 0.85;
      double phase = now * 0.18;
      ParticleOptions[] elements = new ParticleOptions[]{FIRE, WATER, EARTH, WIND};
      for (int i = 0; i < 4; i++) {
         double angle = phase + Math.PI * 2.0 * i / 4.0;
         double x = baseX + Math.cos(angle) * orbit;
         double z = baseZ + Math.sin(angle) * orbit;
         double y = baseY + 0.18 + Math.sin(phase + i) * 0.08;
         level.sendParticles(elements[i], x, y, z, 8, 0.07, 0.07, 0.07, 0.01);
         level.sendParticles(AETHER, x, y + 0.08, z, 4, 0.05, 0.05, 0.05, 0.005);
      }
      level.sendParticles(ParticleTypes.ENCHANT, baseX, baseY + 0.12, baseZ, 12, 0.28, 0.28, 0.28, 0.02);
   }
}
