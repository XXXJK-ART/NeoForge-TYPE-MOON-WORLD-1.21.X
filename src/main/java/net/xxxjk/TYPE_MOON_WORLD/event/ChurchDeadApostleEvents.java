package net.xxxjk.TYPE_MOON_WORLD.event;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.entity.BlackKeyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.HumanNpcEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.NpcScaleHelper;
import net.xxxjk.TYPE_MOON_WORLD.entity.church.ChurchExecutorEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.church.StigmaBearer;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.DeadApostleEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.LivingDeadEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.NightKinEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.BlackKeyItem;
import net.xxxjk.TYPE_MOON_WORLD.magic.church.BaptismRiteEventHandler;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@EventBusSubscriber(modid = "typemoonworld")
public final class ChurchDeadApostleEvents {
   public static final ResourceKey<DamageType> STIGMA_DELAYED = ResourceKey.create(
      Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("typemoonworld", "stigma_delayed"));
   private static final Map<UUID, ArrayDeque<PendingDamage>> STIGMA_DAMAGE = new HashMap<>();

   private ChurchDeadApostleEvents() {}

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onIncomingDamage(LivingIncomingDamageEvent event) {
      LivingEntity target = event.getEntity();
      LivingEntity attacker = ownerOf(event.getSource());
      if (attacker != null && ((DeadApostleEntity.isDeadApostle(attacker) && DeadApostleEntity.isDeadApostle(target))
         || (attacker instanceof ChurchExecutorEntity && target instanceof ChurchExecutorEntity))) {
         event.setCanceled(true);
         event.setAmount(0.0F);
         return;
      }
      if (attacker != null && event.getSource().getDirectEntity() == attacker
         && (event.getSource().is(DamageTypes.PLAYER_ATTACK) || event.getSource().is(DamageTypes.MOB_ATTACK))) {
         ItemStack held = attacker.getMainHandItem();
         if (held.getItem() instanceof BlackKeyItem) {
            float damage = BlackKeyItem.meleeDamage(held);
            if (BlackKeyProjectileEntity.isUndead(target)) damage *= 2.0F;
            event.setAmount(damage);
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onFinalDamage(LivingDamageEvent.Pre event) {
      LivingEntity target = event.getEntity();
      float damage = event.getNewDamage();
      if (target instanceof ChurchExecutorEntity && event.getSource().getDirectEntity() instanceof Projectile
         && net.xxxjk.TYPE_MOON_WORLD.combat.ChurchDeadApostleRules.cassockBlocks(damage)) {
         event.setNewDamage(0.0F);
         return;
      }
      if (damage > 0.0F) {
         if (target instanceof ChurchExecutorEntity executor && executor.isBaptismChanting()) {
            executor.interruptBaptismChant();
         } else if (target instanceof ServerPlayer player && BaptismRiteEventHandler.isChanting(player)) {
            BaptismRiteEventHandler.interrupt(player);
         }
      }
      if (!hasStigma(target) || !net.xxxjk.TYPE_MOON_WORLD.combat.ChurchDeadApostleRules.shouldStigmaDelay(
         damage, isStigmaExcluded(event.getSource()))) return;
      long due = target.level().getGameTime() + 20L;
      STIGMA_DAMAGE.computeIfAbsent(target.getUUID(), ignored -> new ArrayDeque<>())
         .addLast(new PendingDamage(due, damage, event.getSource().getDirectEntity(), event.getSource().getEntity()));
      event.setNewDamage(0.0F);
   }

   @SubscribeEvent
   public static void onEntityTick(EntityTickEvent.Post event) {
      if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide) return;
      ArrayDeque<PendingDamage> queue = STIGMA_DAMAGE.get(living.getUUID());
      if (queue == null) return;
      long now = living.level().getGameTime();
      while (!queue.isEmpty() && queue.peekFirst().dueTick <= now) {
         PendingDamage pending = queue.removeFirst();
         if (!living.isAlive()) continue;
         var holder = living.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(STIGMA_DELAYED);
         DamageSource source = pending.direct == null ? new DamageSource(holder)
            : pending.owner == null ? new DamageSource(holder, pending.direct)
            : new DamageSource(holder, pending.direct, pending.owner);
         living.invulnerableTime = 0;
         living.hurt(source, pending.amount);
      }
      if (queue.isEmpty()) STIGMA_DAMAGE.remove(living.getUUID());
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onLivingDeath(LivingDeathEvent event) {
      if (event.isCanceled() || !(event.getEntity().level() instanceof ServerLevel level)) return;
      LivingEntity victim = event.getEntity();
      LivingEntity attacker = ownerOf(event.getSource());
      if (!(attacker instanceof LivingDeadEntity || attacker instanceof NightKinEntity)) return;
      if (!(victim instanceof Villager || victim instanceof HumanNpcEntity) || victim instanceof ChurchExecutorEntity && attacker == victim) return;
      if (level.random.nextDouble() >= 0.10) return;

      var stage = net.xxxjk.TYPE_MOON_WORLD.combat.ChurchDeadApostleRules.conversionStage(level.random.nextDouble());
      EntityType<? extends DeadApostleEntity> type = switch (stage) {
         case THE_DEAD -> ModEntities.THE_DEAD.get();
         case GHOUL -> ModEntities.GHOUL.get();
         case LIVING_DEAD -> ModEntities.LIVING_DEAD.get();
         case NIGHT_KIN -> ModEntities.NIGHT_KIN.get();
      };
      DeadApostleEntity replacement = type.create(level);
      if (replacement == null) return;
      replacement.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), victim.getXRot());
      replacement.finalizeSpawn(level, level.getCurrentDifficultyAt(victim.blockPosition()), MobSpawnType.CONVERSION, null);
      if (replacement.getAttribute(Attributes.SCALE) != null && victim.getAttribute(Attributes.SCALE) != null) {
         replacement.inheritBodyScale(victim.getAttributeValue(Attributes.SCALE));
      }
      Boolean female = identifiableFemale(victim);
      if (replacement instanceof LivingDeadEntity || replacement instanceof NightKinEntity) {
         if (victim.hasCustomName()) replacement.inheritName(victim.getCustomName(), victim.isCustomNameVisible(), female);
         else {
            // finalizeSpawn assigns an independent spawn name first; replace it so a known source gender is retained.
            replacement.setCustomName(null);
            replacement.initializeGeneratedName(female);
         }
      } else {
         replacement.setCustomName(null);
         replacement.setCustomNameVisible(false);
      }
      level.addFreshEntity(replacement);
   }

   @SubscribeEvent
   public static void onDrops(LivingDropsEvent event) {
      if (!(event.getEntity() instanceof ChurchExecutorEntity executor)
         || executor.getWeaponType() != ChurchExecutorEntity.WEAPON_BLACK_KEY
         || executor.getRandom().nextFloat() >= 0.20F) return;
      ItemStack drop = executor.getMainHandItem().is(ModItems.BLACK_KEY.get())
         ? executor.getMainHandItem().copy() : new ItemStack(ModItems.BLACK_KEY.get());
      event.getDrops().add(new ItemEntity(executor.level(), executor.getX(), executor.getY(), executor.getZ(), drop));
   }

   private static boolean hasStigma(LivingEntity entity) {
      if (entity instanceof StigmaBearer bearer && bearer.hasStigma()) return true;
      if (entity instanceof ServerPlayer player) {
         return player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).learned_magics.contains("stigma");
      }
      return false;
   }

   private static boolean isStigmaExcluded(DamageSource source) {
      return source.is(STIGMA_DELAYED) || source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC_KILL);
   }

   private static LivingEntity ownerOf(DamageSource source) {
      if (source.getEntity() instanceof LivingEntity living) return living;
      if (source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity living) return living;
      return null;
   }

   private static Boolean identifiableFemale(LivingEntity entity) {
      if (entity instanceof ChurchExecutorEntity executor) return executor.isFemale();
      if (entity instanceof MysticMagicianEntity magician) return MysticMagicianEntity.isFemaleVariant(magician.getSkinVariant());
      return null;
   }

   private record PendingDamage(long dueTick, float amount, Entity direct, Entity owner) {}
}
