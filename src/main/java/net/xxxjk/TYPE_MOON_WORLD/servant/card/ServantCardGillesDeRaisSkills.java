package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import com.example.typemoonaddon.entity.HugeSeaMonsterEntity;
import com.example.typemoonaddon.entity.SeaMonsterEntity;
import com.example.typemoonaddon.registry.AddonEntities;
import com.example.typemoonaddon.registry.AddonItems;
import com.example.typemoonaddon.registry.AddonSounds;
import com.example.typemoonaddon.servant.GillesDeRaisCombatHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class ServantCardGillesDeRaisSkills {
   public static final String SERVANT_ID = "gilles_de_rais_caster";
   private static final String TAG_BOOK_MANA = "GillesCardSpellbookMana";
   private static final String TAG_LAST_REGEN = "GillesCardSpellbookLastRegen";
   private static final String TAG_SHROUD_UNTIL = "GillesCardShroudUntil";
   public static final String TAG_HUGE_RIDE_UUID = "GillesCardHugeSeaMonsterRideUuid";
   public static final String TAG_MASTER_LOSS_DEFERRED = "GillesCardMasterLossDeferredUntilHugeDeath";
   private static final double BOOK_MAX_MANA = 2000.0;
   private static final double BOOK_REGEN_PER_SECOND = 20.0;
   private static final int SMALL_COUNT = 3;
   private static final int SHROUD_DURATION = 160;
   private static final int GROWTH_DURATION = 400;

   private ServantCardGillesDeRaisSkills() {
   }

   public static void initialize(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.putDouble(TAG_BOOK_MANA, BOOK_MAX_MANA);
      data.putLong(TAG_LAST_REGEN, player.level().getGameTime());
      data.remove(TAG_SHROUD_UNTIL);
      data.remove(TAG_HUGE_RIDE_UUID);
      data.remove(TAG_MASTER_LOSS_DEFERRED);
      updateSyncedBookMana(player);
      syncRuntime(player);
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data = player.getPersistentData();
      long now = player.level().getGameTime();
      if (!data.contains(TAG_BOOK_MANA)) {
         initialize(player);
      }
      if (data.getLong(TAG_SHROUD_UNTIL) <= now) {
         data.remove(TAG_SHROUD_UNTIL);
      }
      if (!hasUsableSpellbook(player)) {
         data.putLong(TAG_LAST_REGEN, now);
         if (player.tickCount % 20 == 0) {
            updateSyncedBookMana(player);
            syncRuntime(player);
         }
         return;
      }
      long last = data.getLong(TAG_LAST_REGEN);
      if (last <= 0L || last > now) {
         last = now;
      }
      long elapsed = now - last;
      if (elapsed > 0L) {
         double regen = elapsed * BOOK_REGEN_PER_SECOND / 20.0;
         data.putDouble(TAG_BOOK_MANA, Math.min(BOOK_MAX_MANA, data.getDouble(TAG_BOOK_MANA) + regen));
         data.putLong(TAG_LAST_REGEN, now);
      }
      if (data.getLong(TAG_SHROUD_UNTIL) > now && (player.tickCount & 7) == 0) {
         LivingEntity attacker = player.getLastHurtByMob();
         if (isEnemy(player, attacker) && player.distanceToSqr(attacker) <= 48.0 * 48.0) {
            commandSeaMonsters(player, attacker);
         }
      }
      if (player.tickCount % 20 == 0) {
         updateSyncedBookMana(player);
         syncRuntime(player);
      }
   }

   public static void clear(ServerPlayer player) {
      cleanupSummons(player);
      player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
      player.removeEffect(MobEffects.ABSORPTION);
      CompoundTag data = player.getPersistentData();
      data.remove(TAG_BOOK_MANA);
      data.remove(TAG_LAST_REGEN);
      data.remove(TAG_SHROUD_UNTIL);
      data.remove(TAG_HUGE_RIDE_UUID);
      data.remove(TAG_MASTER_LOSS_DEFERRED);
      updateSyncedBookMana(player);
   }

   public static boolean summonSmallSeaMonsters(ServerPlayer player) {
      if (!requireUsableSpellbook(player)) {
         return false;
      }
      if (!(player.level() instanceof ServerLevel level) || !consumeBookMana(player, 50.0)) {
         return false;
      }
      LivingEntity target = findLookTarget(player, 28.0, 2.4);
      int summoned = 0;
      for (int i = 0; i < SMALL_COUNT; i++) {
         SeaMonsterEntity seaMonster = AddonEntities.GILLES_SEA_MONSTER.get().create(level);
         if (seaMonster == null) {
            continue;
         }
         Vec3 pos = findSummonPosition(level, player, i, SMALL_COUNT, 2.4);
         seaMonster.moveTo(pos.x, pos.y, pos.z, player.getYRot(), 0.0F);
         seaMonster.setController(player);
         seaMonster.setLarge(false);
         if (target != null) {
            seaMonster.setTarget(target);
         }
         if (level.addFreshEntity(seaMonster)) {
            summoned++;
         }
      }
      if (summoned <= 0) {
         addBookMana(player, 50.0);
         return false;
      }
      playSummonEffects(player, 0.9F);
      return true;
   }

   public static boolean summonLargeSeaMonster(ServerPlayer player) {
      if (!requireUsableSpellbook(player)) {
         return false;
      }
      if (!(player.level() instanceof ServerLevel level) || !consumeBookMana(player, 200.0)) {
         return false;
      }
      LivingEntity target = findLookTarget(player, 32.0, 2.6);
      SeaMonsterEntity seaMonster = AddonEntities.GILLES_SEA_MONSTER.get().create(level);
      if (seaMonster == null) {
         addBookMana(player, 200.0);
         return false;
      }
      Vec3 pos = findSummonPosition(level, player, 0, 1, 3.3);
      seaMonster.moveTo(pos.x, pos.y, pos.z, player.getYRot(), 0.0F);
      seaMonster.setController(player);
      seaMonster.setLarge(true);
      if (target != null) {
         seaMonster.setTarget(target);
      }
      if (!level.addFreshEntity(seaMonster)) {
         addBookMana(player, 200.0);
         return false;
      }
      playSummonEffects(player, 0.7F);
      return true;
   }

   public static boolean performAbyssalGaze(ServerPlayer player) {
      if (!requireUsableSpellbook(player)) {
         return false;
      }
      LivingEntity target = findLookTarget(player, 24.0, 2.0);
      if (target == null) {
         return false;
      }
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1, false, true, true));
      if (player.getRandom().nextFloat() < 0.45F) {
         target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, true, true));
      }
      target.hurt(player.damageSources().magic(), 6.0F);
      if (player.level() instanceof ServerLevel level) {
         ServantCardSkillUtils.spawnLineParticles(level, player.getEyePosition(), target.position().add(0.0, target.getBbHeight() * 0.5, 0.0), ParticleTypes.SQUID_INK);
         level.playSound(null, player.blockPosition(), AddonSounds.GILLES_VOICE_GAZE.get(), SoundSource.PLAYERS, 1.0F, 0.95F);
         level.playSound(null, target.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 0.9F, 0.85F);
      }
      commandSeaMonsters(player, target);
      return true;
   }

   public static boolean performLifeAbsorb(ServerPlayer player) {
      if (!requireUsableSpellbook(player)) {
         return false;
      }
      LivingEntity target = findLookTarget(player, 12.0, 1.8);
      if (target == null) {
         target = findNearestEnemy(player, 8.0);
      }
      if (target == null) {
         return false;
      }
      float amount = 8.0F;
      float before = target.getHealth();
      target.invulnerableTime = 0;
      boolean hit = target.hurt(player.damageSources().magic(), amount);
      target.invulnerableTime = 0;
      if (!hit) {
         return false;
      }
      player.heal(4.0F);
      addBookMana(player, Math.max(0.0F, before - target.getHealth()));
      if (player.level() instanceof ServerLevel level) {
         ServantCardSkillUtils.spawnLineParticles(level, target.position().add(0.0, target.getBbHeight() * 0.5, 0.0), player.position().add(0.0, 1.0, 0.0), ParticleTypes.WITCH);
         level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.75F, 0.75F);
      }
      return true;
   }

   public static boolean performCommand(ServerPlayer player) {
      if (!requireUsableSpellbook(player)) {
         return false;
      }
      LivingEntity target = findLookTarget(player, 32.0, 2.8);
      int commanded = commandSeaMonsters(player, target);
      if (commanded <= 0) {
         return false;
      }
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1.0, player.getZ(), 28, 0.9, 0.35, 0.9, 0.025);
         level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.8F, 0.6F);
      }
      return true;
   }

   public static boolean performPollutionInkFog(ServerPlayer player) {
      if (!requireUsableSpellbook(player)) {
         return false;
      }
      if (!(player.level() instanceof ServerLevel level) || !consumeBookMana(player, 120.0)) {
         return false;
      }
      Vec3 center = findLookLocation(player, 24.0);
      if (center == null) {
         addBookMana(player, 120.0);
         return false;
      }
      GillesDeRaisCombatHelper.addPollutionZone(level.dimension(), center, 5.0, 120, 5.0F, player.getUUID(), null);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(5.0), entity -> isEnemy(player, entity))) {
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, false, true, true));
      }
      level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y + 0.35, center.z, 64, 1.6, 0.35, 1.6, 0.04);
      level.playSound(null, BlockPos.containing(center), SoundEvents.SQUID_SQUIRT, SoundSource.PLAYERS, 1.0F, 0.65F);
      return true;
   }

   public static boolean performPrelatiShroud(ServerPlayer player) {
      if (!requireUsableSpellbook(player)) {
         return false;
      }
      if (!(player.level() instanceof ServerLevel level) || !consumeBookMana(player, 180.0)) {
         return false;
      }
      long until = level.getGameTime() + SHROUD_DURATION;
      player.getPersistentData().putLong(TAG_SHROUD_UNTIL, until);
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, SHROUD_DURATION, 1, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, SHROUD_DURATION, 1, false, true, true));
      LivingEntity attacker = player.getLastHurtByMob();
      if (isEnemy(player, attacker)) {
         commandSeaMonsters(player, attacker);
      }
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 0.9, player.getZ(), 50, 0.75, 0.7, 0.75, 0.06);
      level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 0.7F);
      return true;
   }

   public static boolean performProfaneGrowth(ServerPlayer player) {
      if (!requireUsableSpellbook(player)) {
         return false;
      }
      if (!(player.level() instanceof ServerLevel level) || !consumeBookMana(player, 300.0)) {
         return false;
      }
      int affected = 0;
      for (SeaMonsterEntity seaMonster : ownedSeaMonsters(level, player, 64.0)) {
         seaMonster.heal(seaMonster.isLarge() ? 90.0F : 30.0F);
         seaMonster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, GROWTH_DURATION, seaMonster.isLarge() ? 1 : 0, false, true, true));
         seaMonster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, GROWTH_DURATION, 0, false, true, true));
         LivingEntity target = findLookTarget(player, 32.0, 2.6);
         if (target != null) {
            seaMonster.setTarget(target);
         }
         affected++;
      }
      for (HugeSeaMonsterEntity hugeSeaMonster : ownedHugeSeaMonsters(level, player, 96.0)) {
         hugeSeaMonster.heal(250.0F);
         hugeSeaMonster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, GROWTH_DURATION, 0, false, true, true));
         affected++;
      }
      if (affected <= 0) {
         addBookMana(player, 300.0);
         return false;
      }
      level.sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 0.8, player.getZ(), 60, 1.6, 0.8, 1.6, 0.04);
      level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 0.7F, 1.45F);
      return true;
   }

   public static boolean performEvilGodPraise(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return false;
      }
      int affected = 0;
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(8.0), entity -> isEnemy(player, entity))) {
         Vec3 away = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
         if (away.lengthSqr() < 1.0E-4) {
            away = player.getLookAngle().multiply(-1.0, 0.0, -1.0);
         }
         away = away.normalize();
         target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, true, true));
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2, false, true, true));
         target.push(away.x * 1.2, 0.22, away.z * 1.2);
         target.hurtMarked = true;
         target.hurt(player.damageSources().magic(), 10.0F);
         affected++;
      }
      if (affected <= 0) {
         return false;
      }
      level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1.0, player.getZ(), 90, 2.0, 0.9, 2.0, 0.05);
      level.playSound(null, player.blockPosition(), AddonSounds.GILLES_VOICE_FAIL.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
      return true;
   }

   public static boolean summonHugeSeaMonster(ServerPlayer player) {
      if (!requireUsableSpellbook(player)) {
         return false;
      }
      if (!(player.level() instanceof ServerLevel level) || !consumeBookMana(player, BOOK_MAX_MANA)) {
         return false;
      }
      HugeSeaMonsterEntity hugeSeaMonster = AddonEntities.GILLES_HUGE_SEA_MONSTER.get().create(level);
      if (hugeSeaMonster == null) {
         addBookMana(player, BOOK_MAX_MANA);
         return false;
      }
      LivingEntity target = findLookTarget(player, 48.0, 3.5);
      Vec3 pos = target == null ? findSummonPosition(level, player, 0, 1, 8.0) : target.position().add(target.getLookAngle().multiply(-7.0, 0.0, -7.0));
      pos = safePositionNear(level, hugeSeaMonster, pos);
      hugeSeaMonster.moveTo(pos.x, pos.y, pos.z, player.getYRot(), 0.0F);
      hugeSeaMonster.setSource(player);
      if (target != null) {
         hugeSeaMonster.setTarget(target);
      }
      if (!level.addFreshEntity(hugeSeaMonster)) {
         addBookMana(player, BOOK_MAX_MANA);
         return false;
      }
      if (player.isPassenger()) {
         player.stopRiding();
      }
      if (!player.startRiding(hugeSeaMonster, true)) {
         hugeSeaMonster.discard();
         addBookMana(player, BOOK_MAX_MANA);
         return false;
      }
      player.getPersistentData().putUUID(TAG_HUGE_RIDE_UUID, hugeSeaMonster.getUUID());
      level.playSound(null, player.blockPosition(), AddonSounds.GILLES_VOICE_NP.get(), SoundSource.PLAYERS, 1.35F, 0.85F);
      level.playSound(null, hugeSeaMonster.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.2F, 0.55F);
      level.sendParticles(ParticleTypes.DRAGON_BREATH, hugeSeaMonster.getX(), hugeSeaMonster.getY() + 1.0, hugeSeaMonster.getZ(), 160, 4.0, 2.0, 4.0, 0.08);
      return true;
   }

   public static boolean isGillesCardPlayer(ServerPlayer player) {
      net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PlayerVariables vars =
         player.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && SERVANT_ID.equals(vars.servant_card_id);
   }

   public static boolean hasLivingHugeSeaMonster(ServerPlayer player) {
      if (!isGillesCardPlayer(player) || !(player.level() instanceof ServerLevel level)) {
         return false;
      }
      CompoundTag data = player.getPersistentData();
      if (data.hasUUID(TAG_HUGE_RIDE_UUID)) {
         net.minecraft.world.entity.Entity entity = level.getEntity(data.getUUID(TAG_HUGE_RIDE_UUID));
         if (entity instanceof HugeSeaMonsterEntity huge
            && huge.isAlive() && !huge.isRemoved()
            && player.getUUID().equals(huge.getSourceUuid())) {
            return true;
         }
      }
      return player.getVehicle() instanceof HugeSeaMonsterEntity huge
         && huge.isAlive() && !huge.isRemoved()
         && player.getUUID().equals(huge.getSourceUuid());
   }

   public static boolean hasMasterLossDeferral(ServerPlayer player) {
      return isGillesCardPlayer(player) && player.getPersistentData().getBoolean(TAG_MASTER_LOSS_DEFERRED);
   }

   public static void deferMasterLossUntilHugeDeath(ServerPlayer player) {
      if (!isGillesCardPlayer(player) || !hasLivingHugeSeaMonster(player)) {
         return;
      }
      player.getPersistentData().putBoolean(TAG_MASTER_LOSS_DEFERRED, true);
   }

   public static void clearMasterLossDeferral(ServerPlayer player) {
      if (player != null) {
         player.getPersistentData().remove(TAG_MASTER_LOSS_DEFERRED);
      }
   }

   public static void onHugeSeaMonsterDeath(HugeSeaMonsterEntity hugeSeaMonster) {
      if (!(hugeSeaMonster.level() instanceof ServerLevel level) || hugeSeaMonster.getSourceUuid() == null) {
         return;
      }
      net.minecraft.world.entity.Entity source = level.getEntity(hugeSeaMonster.getSourceUuid());
      if (!(source instanceof ServerPlayer player) || !isGillesCardPlayer(player)) {
         return;
      }
      CompoundTag data = player.getPersistentData();
      if (data.hasUUID(TAG_HUGE_RIDE_UUID)
         && hugeSeaMonster.getUUID().equals(data.getUUID(TAG_HUGE_RIDE_UUID))) {
         data.remove(TAG_HUGE_RIDE_UUID);
      }
      if (data.getBoolean(TAG_MASTER_LOSS_DEFERRED)) {
         data.remove(TAG_MASTER_LOSS_DEFERRED);
         MasterServantLinkService.forceMasterlessDeathAfterHugeSeaMonster(player);
      }
   }

   private static boolean consumeBookMana(ServerPlayer player, double amount) {
      if (!requireUsableSpellbook(player)) {
         return false;
      }
      CompoundTag data = player.getPersistentData();
      if (!data.contains(TAG_BOOK_MANA)) {
         initialize(player);
      }
      double mana = data.getDouble(TAG_BOOK_MANA);
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double totalAvailable = mana + Math.max(0.0, vars.servant_card_mana);
      if (totalAvailable + 1.0E-6 < amount) {
         player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.typemoonworld.servant_card.gilles_not_enough_book_mana"), true);
         updateSyncedBookMana(player);
         syncRuntime(player);
         return false;
      }
      if (mana + 1.0E-6 < amount) {
         double refill = amount - mana;
         if (ServantCardManaService.consumeOwnMana(player, vars, refill)) {
            mana += refill;
            data.putDouble(TAG_BOOK_MANA, mana);
         }
      }
      data.putDouble(TAG_BOOK_MANA, Math.max(0.0, mana - amount));
      updateSyncedBookMana(player);
      syncRuntime(player);
      return true;
   }

   private static void addBookMana(ServerPlayer player, double amount) {
      CompoundTag data = player.getPersistentData();
      if (!data.contains(TAG_BOOK_MANA)) {
         initialize(player);
      }
      data.putDouble(TAG_BOOK_MANA, Math.min(BOOK_MAX_MANA, data.getDouble(TAG_BOOK_MANA) + Math.max(0.0, amount)));
      updateSyncedBookMana(player);
      syncRuntime(player);
   }

   private static boolean requireUsableSpellbook(ServerPlayer player) {
      if (hasUsableSpellbook(player)) {
         return true;
      }
      player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.typemoonworld.servant_card.gilles_spellbook_required"), true);
      updateSyncedBookMana(player);
      syncRuntime(player);
      return false;
   }

   private static boolean hasUsableSpellbook(ServerPlayer player) {
      return player.getMainHandItem().is(AddonItems.PRELATIS_SPELLBOOK.get())
         || player.getOffhandItem().is(AddonItems.PRELATIS_SPELLBOOK.get());
   }

   private static void updateSyncedBookMana(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      CompoundTag data = player.getPersistentData();
      vars.servant_card_gilles_spellbook_mana = data.contains(TAG_BOOK_MANA) ? Math.max(0.0, data.getDouble(TAG_BOOK_MANA)) : 0.0;
      vars.servant_card_gilles_spellbook_max_mana = isGillesCardPlayer(player) ? BOOK_MAX_MANA : 0.0;
   }

   private static void syncRuntime(ServerPlayer player) {
      player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).syncServantCardRuntime(player);
   }

   private static int commandSeaMonsters(ServerPlayer player, LivingEntity target) {
      if (!(player.level() instanceof ServerLevel level)) {
         return 0;
      }
      int commanded = 0;
      for (SeaMonsterEntity seaMonster : ownedSeaMonsters(level, player, 128.0)) {
         seaMonster.setTarget(isEnemy(player, target) ? target : null);
         commanded++;
      }
      for (HugeSeaMonsterEntity hugeSeaMonster : ownedHugeSeaMonsters(level, player, 160.0)) {
         hugeSeaMonster.setTarget(isEnemy(player, target) ? target : null);
         commanded++;
      }
      return commanded;
   }

   private static List<SeaMonsterEntity> ownedSeaMonsters(ServerLevel level, ServerPlayer player, double radius) {
      UUID owner = player.getUUID();
      return new ArrayList<>(level.getEntitiesOfClass(SeaMonsterEntity.class, player.getBoundingBox().inflate(radius),
         seaMonster -> seaMonster.isAlive() && owner.equals(seaMonster.getControllerUuid())));
   }

   private static List<HugeSeaMonsterEntity> ownedHugeSeaMonsters(ServerLevel level, ServerPlayer player, double radius) {
      UUID owner = player.getUUID();
      return new ArrayList<>(level.getEntitiesOfClass(HugeSeaMonsterEntity.class, player.getBoundingBox().inflate(radius),
         hugeSeaMonster -> hugeSeaMonster.isAlive() && owner.equals(hugeSeaMonster.getSourceUuid())));
   }

   private static void cleanupSummons(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      for (SeaMonsterEntity seaMonster : ownedSeaMonsters(level, player, 192.0)) {
         seaMonster.discard();
      }
      for (HugeSeaMonsterEntity hugeSeaMonster : ownedHugeSeaMonsters(level, player, 256.0)) {
         if (player.getVehicle() == hugeSeaMonster) {
            player.stopRiding();
         }
         hugeSeaMonster.discard();
      }
      player.getPersistentData().remove(TAG_HUGE_RIDE_UUID);
      player.getPersistentData().remove(TAG_MASTER_LOSS_DEFERRED);
   }

   private static LivingEntity findLookTarget(ServerPlayer player, double range, double inflate) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle().normalize();
      AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(inflate);
      LivingEntity best = null;
      double bestScore = 0.74;
      for (LivingEntity living : player.level().getEntitiesOfClass(LivingEntity.class, box, entity -> isEnemy(player, entity) && player.hasLineOfSight(entity))) {
         Vec3 to = living.position().add(0.0, living.getBbHeight() * 0.5, 0.0).subtract(eye);
         double distance = to.length();
         if (distance <= 0.01 || distance > range) {
            continue;
         }
         double score = look.dot(to.normalize());
         if (score > bestScore) {
            bestScore = score;
            best = living;
         }
      }
      return best;
   }

   private static LivingEntity findNearestEnemy(ServerPlayer player, double radius) {
      if (!(player.level() instanceof ServerLevel level)) {
         return null;
      }
      LivingEntity best = null;
      double bestDistance = radius * radius + 1.0;
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius), entity -> isEnemy(player, entity))) {
         double distance = player.distanceToSqr(living);
         if (distance < bestDistance) {
            bestDistance = distance;
            best = living;
         }
      }
      return best;
   }

   private static boolean isEnemy(ServerPlayer player, LivingEntity entity) {
      return entity != null && entity != player && entity.isAlive()
         && !player.isAlliedTo(entity) && !entity.isAlliedTo(player)
         && !ServantMasterTargeting.isContractMaster(player, entity)
         && !EntityUtils.isImmunePlayerTarget(entity);
   }

   private static Vec3 findLookLocation(ServerPlayer player, double range) {
      LivingEntity target = findLookTarget(player, range, 2.4);
      if (target != null) {
         return target.position();
      }
      if (!(player.level() instanceof ServerLevel level)) {
         return null;
      }
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle().normalize();
      HitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
      if (hit.getType() == HitResult.Type.BLOCK) {
         BlockHitResult blockHit = (BlockHitResult) hit;
         return Vec3.atBottomCenterOf(blockHit.getBlockPos().relative(blockHit.getDirection()));
      }
      return eye.add(look.scale(Math.min(range, 12.0)));
   }

   private static Vec3 findSummonPosition(ServerLevel level, LivingEntity owner, int index, int total, double baseRadius) {
      double angle = Math.PI * 2.0 * index / Math.max(1, total);
      double radius = baseRadius + (index % 2) * 0.9;
      Vec3 base = owner.position().add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
      return safePositionNear(level, owner, base);
   }

   private static Vec3 safePositionNear(ServerLevel level, LivingEntity entity, Vec3 base) {
      for (int y = 2; y >= -4; y--) {
         Vec3 candidate = new Vec3(base.x, base.y + y, base.z);
         AABB moved = entity.getBoundingBox().move(candidate.subtract(entity.position()));
         BlockPos below = BlockPos.containing(candidate.x, candidate.y - 0.08, candidate.z);
         BlockState belowState = level.getBlockState(below);
         if (level.getWorldBorder().isWithinBounds(BlockPos.containing(candidate))
            && level.noCollision(entity, moved)
            && belowState.isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) {
            return candidate;
         }
      }
      return base;
   }

   private static void playSummonEffects(ServerPlayer player, float pitch) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 0.9, player.getZ(), 48, 1.0, 0.5, 1.0, 0.04);
      level.playSound(null, player.blockPosition(), AddonSounds.GILLES_VOICE_SUMMON.get(), SoundSource.PLAYERS, 1.0F, pitch);
      level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 0.9F, 0.55F);
   }
}
