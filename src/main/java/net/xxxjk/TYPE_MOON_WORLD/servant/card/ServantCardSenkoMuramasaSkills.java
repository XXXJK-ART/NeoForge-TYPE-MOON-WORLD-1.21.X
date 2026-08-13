package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.BattlefieldAreaService;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.TsumukariMuramasaItem;
import net.xxxjk.TYPE_MOON_WORLD.magic.MuramasaSlashHandler;
import net.xxxjk.TYPE_MOON_WORLD.network.OpenMuramasaForgeScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MuramasaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

public final class ServantCardSenkoMuramasaSkills {
   public static final String WORKSHOP_ACTIVE = "ServantCardMuramasaWorkshopActive";
   public static final String WORKSHOP_EXPIRES = "ServantCardMuramasaWorkshopExpires";
   public static final String TRIAL_UNTIL = "ServantCardMuramasaTrialUntil";
   public static final String KARMA_UNTIL = "ServantCardMuramasaKarmaUntil";
   public static final String FLAME_UNTIL = "ServantCardMuramasaFlameUntil";
   public static final String TEMPER_UNTIL = "ServantCardMuramasaTemperUntil";
   public static final String NP_USED = "ServantCardMuramasaNpUsed";
   public static final String NP_PENDING = "ServantCardMuramasaNpPending";
   private static final String ATTACK_ID = "typemoonworld:servant_card_muramasa_attack";
   private static final String SPEED_ID = "typemoonworld:servant_card_muramasa_speed";
   private static final double WORKSHOP_RADIUS = 25.0;

   private ServantCardSenkoMuramasaSkills() {
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!"senko_muramasa".equals(vars.servant_card_id)) {
         clear(player, vars);
         return;
      }
      CompoundTag data = player.getPersistentData();
      long now = player.level().getGameTime();
      if (data.getBoolean(WORKSHOP_ACTIVE) && now >= data.getLong(WORKSHOP_EXPIRES)) {
         endWorkshop(player);
      }
      refreshModifiers(player, now);
      if (now % 10L == 0L && data.getLong(FLAME_UNTIL) > now) {
         vars.servant_card_mana = Math.min(vars.servant_card_max_mana, vars.servant_card_mana + 1.0);
         vars.syncMana(player);
      }
      if (data.getBoolean(NP_PENDING)) {
         player.setDeltaMovement(Vec3.ZERO);
      }
   }

   public static void clear(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (player == null) return;
      endWorkshop(player);
      CompoundTag data = player.getPersistentData();
      for (String key : List.of(TRIAL_UNTIL, KARMA_UNTIL, FLAME_UNTIL, TEMPER_UNTIL, NP_PENDING)) {
         data.remove(key);
      }
      data.remove(NP_USED);
      removeModifier(player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE), ATTACK_ID);
      removeModifier(player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED), SPEED_ID);
      if (isProjectedMuramasaItem(player.getMainHandItem())) {
         player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      }
   }

   public static boolean isNpUsed(ServerPlayer player) {
      return player.getPersistentData().getBoolean(NP_USED);
   }

   public static boolean openForgeSelection(ServerPlayer player) {
      if (!isMuramasa(player)) return false;
      ModNetwork.sendToPlayer(player, new OpenMuramasaForgeScreenMessage());
      return true;
   }

   public static boolean forge(ServerPlayer player, int choice) {
      if (!isMuramasa(player) || choice < 0 || choice > 3) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!ServantCardManaService.consume(player, vars, 10.0)) return false;
      Item item = switch (choice) {
         case 0 -> ModItems.MURAMASA.get();
         case 1 -> ModItems.WAKIZASHI.get();
         case 2 -> ModItems.KATANA.get();
         default -> ModItems.NODACHI.get();
      };
      player.setItemInHand(InteractionHand.MAIN_HAND, MuramasaCombatHelper.projectedStack(item));
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 32, 0.4, 0.5, 0.4, 0.08);
         level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.9, player.getZ(), 16, 0.35, 0.35, 0.35, 0.04);
         level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
      }
      return true;
   }

   public static boolean performWorkshop(ServerPlayer player) {
      if (!isMuramasa(player) || !(player.level() instanceof ServerLevel level)) return false;
      if (player.getPersistentData().getBoolean(WORKSHOP_ACTIVE)) {
         endWorkshop(player);
         return true;
      }
      long expires = level.getGameTime() + 1200L;
      player.getPersistentData().putBoolean(WORKSHOP_ACTIVE, true);
      player.getPersistentData().putLong(WORKSHOP_EXPIRES, expires);
      BattlefieldAreaService.register(level, player.getUUID(), BattlefieldAreaService.Type.WORKSHOP,
         player.position(), WORKSHOP_RADIUS, expires);
      level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 0.1, player.getZ(), 80, WORKSHOP_RADIUS * 0.65, 0.1, WORKSHOP_RADIUS * 0.65, 0.03);
      level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 0.75F);
      return true;
   }

   public static boolean performTrial(ServerPlayer player) {
      player.getPersistentData().putLong(TRIAL_UNTIL, player.level().getGameTime() + 300L);
      return true;
   }

   public static boolean performKarma(ServerPlayer player) {
      player.getPersistentData().putLong(KARMA_UNTIL, player.level().getGameTime() + 200L);
      return true;
   }

   public static boolean performFlame(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      vars.servant_card_mana = Math.min(vars.servant_card_max_mana, vars.servant_card_mana + 100.0);
      vars.syncMana(player);
      player.getPersistentData().putLong(FLAME_UNTIL, player.level().getGameTime() + 300L);
      return true;
   }

   public static boolean performProjectionVolley(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      Vec3 look = player.getLookAngle().normalize();
      Vec3 aim = player.getEyePosition().add(look.scale(28.0));
      for (int i = 0; i < 5; i++) {
         Vec3 spawn = player.getEyePosition().add(look.scale(0.4))
            .add((player.getRandom().nextDouble() - 0.5) * 1.2, (player.getRandom().nextDouble() - 0.5) * 0.8, (player.getRandom().nextDouble() - 0.5) * 1.2);
         UBWProjectileEntity sword = new UBWProjectileEntity(level, player, MuramasaCombatHelper.projectedStack(Items.IRON_SWORD));
         sword.setPos(spawn.x, spawn.y, spawn.z);
         Vec3 direction = aim.add((player.getRandom().nextDouble() - 0.5) * 2.0, (player.getRandom().nextDouble() - 0.5) * 1.0,
            (player.getRandom().nextDouble() - 0.5) * 2.0).subtract(spawn).normalize();
         sword.setDeltaMovement(direction.scale(2.7));
         level.addFreshEntity(sword);
      }
      level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.5, 0.6, 0.5, 0.1);
      return true;
   }

   public static boolean performTemper(ServerPlayer player) {
      player.getPersistentData().putLong(TEMPER_UNTIL, player.level().getGameTime() + 240L);
      player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 240, 0, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 240, 0, false, true, true));
      return true;
   }

   public static boolean performKarmaSlash(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      LivingEntity target = ServantCardSkillUtils.findLookTarget(player, 12.0, 2.0);
      if (target == null) return false;
      removeBeneficialEffects(target);
      applyTrueDamage(player, target, 42.0F);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 8, 0.4, 0.6, 0.4, 0.0);
      return true;
   }

   public static boolean performSwordField(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      Vec3 center = player.position();
      level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.5, center.z, 80, 8.0, 0.8, 8.0, 0.08);
      for (int i = 0; i < 32; i++) {
         double angle = Math.PI * 2.0 * i / 32.0;
         Vec3 point = center.add(Math.cos(angle) * 8.0, 0.8, Math.sin(angle) * 8.0);
         level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 2, 0.1, 0.25, 0.1, 0.05);
      }
      TYPE_MOON_WORLD.queueServerWork(15, () -> {
         if (!player.isAlive() || player.level() != level) return;
         for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(8.0),
            e -> e != player && e.isAlive() && !player.isAlliedTo(e))) {
            applyTrueDamage(player, target, 28.0F);
            target.igniteForSeconds(3.0F);
         }
         level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1.0, player.getZ(), 120, 8.0, 1.2, 8.0, 0.08);
      });
      return true;
   }

   public static boolean performNoblePhantasm(ServerPlayer player) {
      if (!isMuramasa(player) || isNpUsed(player) || !(player.level() instanceof ServerLevel level)) return false;
      player.getPersistentData().putBoolean(NP_USED, true);
      player.getPersistentData().putBoolean(NP_PENDING, true);
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 220, 10, false, false, false));
      level.playSound(null, player.blockPosition(), ModSounds.SENKO_MURAMASA_VOICE_NP.get(), SoundSource.VOICE, 1.0F, 1.0F);
      expandField(level, player, 0);
      TYPE_MOON_WORLD.queueServerWork(200, () -> revealTsumukari(player));
      return true;
   }

   private static void expandField(ServerLevel level, ServerPlayer player, int radius) {
      if (!player.isAlive() || !player.getPersistentData().getBoolean(NP_PENDING)) return;
      double r = Math.min(25.0, radius);
      int points = Math.max(24, (int)(r * 5.0));
      for (int i = 0; i < points; i++) {
         double angle = Math.PI * 2.0 * i / points;
         double x = player.getX() + Math.cos(angle) * r;
         double z = player.getZ() + Math.sin(angle) * r;
         level.sendParticles(ParticleTypes.FLAME, x, player.getY() + 0.1, z, 1, 0.0, 0.08, 0.0, 0.02);
         level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, player.getY() + 0.35, z, 1, 0.0, 0.04, 0.0, 0.01);
      }
      level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 0.5, player.getZ(), 14, Math.max(0.2, r * 0.45), 0.5, Math.max(0.2, r * 0.45), 0.04);
      if (radius < 25) TYPE_MOON_WORLD.queueServerWork(8, () -> expandField(level, player, radius + 1));
   }

   private static void revealTsumukari(ServerPlayer player) {
      if (!player.isAlive() || !(player.level() instanceof ServerLevel level)) return;
      player.setItemInHand(InteractionHand.MAIN_HAND, MuramasaCombatHelper.projectedStack(ModItems.TSUMUKARI_MURAMASA.get()));
      level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.0, player.getZ(), 2, 0, 0, 0, 0);
      level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 120, 1.5, 1.2, 1.5, 0.15);
      level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1.0, player.getZ(), 180, 12.0, 3.0, 12.0, 0.12);
      replaceTemporaryRedField(level, player, 16, 12 * 20);
      player.getPersistentData().putBoolean(NP_PENDING, false);
      for (int i = 25; i >= 0; i--) {
         final int step = i;
         TYPE_MOON_WORLD.queueServerWork((25 - i) * 3, () -> {
            if (!player.isAlive()) return;
            double r = step;
            int points = Math.max(20, (int)(r * 4.0));
            for (int j = 0; j < points; j++) {
               double angle = Math.PI * 2.0 * j / points;
               level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX() + Math.cos(angle) * r, player.getY() + 0.15,
                  player.getZ() + Math.sin(angle) * r, 1, 0, 0.04, 0, 0.01);
            }
         });
      }
   }

   private static void replaceTemporaryRedField(ServerLevel level, ServerPlayer player, int radius, int restoreDelay) {
      Map<BlockPos, BlockState> originals = new HashMap<>();
      BlockPos center = player.blockPosition();
      int radiusSquared = radius * radius;
      int limit = 900;
      for (int dx = -radius; dx <= radius && originals.size() < limit; dx++) {
         for (int dz = -radius; dz <= radius && originals.size() < limit; dz++) {
            if (dx * dx + dz * dz > radiusSquared) {
               continue;
            }
            int x = center.getX() + dx;
            int z = center.getZ() + dz;
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.hasChunkAt(pos) || level.getBlockEntity(pos) != null) {
               continue;
            }
            BlockState original = level.getBlockState(pos);
            BlockState replacement = Blocks.RED_SANDSTONE.defaultBlockState();
            if (original.is(Blocks.BEDROCK) || original.equals(replacement) || original.getDestroySpeed(level, pos) < 0.0F) {
               continue;
            }
            if (level.setBlock(pos, replacement, 3)) {
               originals.put(pos.immutable(), original);
            }
         }
      }
      TYPE_MOON_WORLD.queueServerWork(restoreDelay, () -> {
         for (Map.Entry<BlockPos, BlockState> entry : originals.entrySet()) {
            if (level.hasChunkAt(entry.getKey()) && level.getBlockState(entry.getKey()).is(Blocks.RED_SANDSTONE)) {
               level.setBlock(entry.getKey(), entry.getValue(), 3);
            }
         }
      });
   }

   private static void refreshModifiers(ServerPlayer player, long now) {
      AttributeInstance attack = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
      AttributeInstance speed = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
      removeModifier(attack, ATTACK_ID);
      removeModifier(speed, SPEED_ID);
      if (attack != null && (now < player.getPersistentData().getLong(TRIAL_UNTIL) || now < player.getPersistentData().getLong(KARMA_UNTIL))) {
         double bonus = now < player.getPersistentData().getLong(KARMA_UNTIL) ? 0.50 : 0.30;
         attack.addTransientModifier(new AttributeModifier(net.minecraft.resources.ResourceLocation.parse(ATTACK_ID), bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      }
      if (speed != null && now < player.getPersistentData().getLong(TEMPER_UNTIL)) {
         speed.addTransientModifier(new AttributeModifier(net.minecraft.resources.ResourceLocation.parse(SPEED_ID), 0.30, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      }
   }

   private static void endWorkshop(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(WORKSHOP_ACTIVE)) return;
      if (player.level() instanceof ServerLevel level) BattlefieldAreaService.unregister(level, player.getUUID(), BattlefieldAreaService.Type.WORKSHOP);
      data.remove(WORKSHOP_ACTIVE);
      data.remove(WORKSHOP_EXPIRES);
   }

   private static void removeModifier(AttributeInstance attribute, String id) {
      if (attribute != null) attribute.removeModifier(net.minecraft.resources.ResourceLocation.parse(id));
   }

   private static boolean isMuramasa(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && "senko_muramasa".equals(vars.servant_card_id);
   }

   private static boolean isProjectedMuramasaItem(ItemStack stack) {
      if (stack == null || stack.isEmpty()) return false;
      CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
         net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
      return tag.getBoolean("is_projected") && (stack.is(ModItems.MURAMASA.get()) || stack.is(ModItems.TSUMUKARI_MURAMASA.get())
         || stack.is(ModItems.WAKIZASHI.get()) || stack.is(ModItems.KATANA.get()) || stack.is(ModItems.NODACHI.get()));
   }

   private static void removeBeneficialEffects(LivingEntity target) {
      for (MobEffectInstance effect : List.copyOf(target.getActiveEffects())) {
         if (effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.BENEFICIAL) target.removeEffect(effect.getEffect());
      }
   }

   private static void applyTrueDamage(ServerPlayer player, LivingEntity target, float amount) {
      if (!target.isAlive() || EntityUtils.isImmunePlayerTarget(target)) return;
      float before = target.getHealth();
      target.invulnerableTime = 0;
      target.hurt(player.damageSources().magic(), amount);
      target.invulnerableTime = 0;
      float expected = Math.max(0.0F, before - amount);
      if (target.isAlive() && target.getHealth() > expected) target.setHealth(expected);
   }
}
