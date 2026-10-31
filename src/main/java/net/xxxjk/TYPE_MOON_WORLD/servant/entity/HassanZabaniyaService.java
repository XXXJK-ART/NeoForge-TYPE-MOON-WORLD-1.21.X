package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.AttackOrigin;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.CombatDamageSource;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.heracles.GodHandService;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.joml.Vector3f;

/** Shared extraction, binding and delayed death for the NPC and servant card. */
public final class HassanZabaniyaService {
   public static final int LIFETIME = 1200;
   public static final String TOKEN = "HassanZabaniyaToken";
   public static final String EXPIRES = "HassanZabaniyaExpires";
   private static final String TARGET = "Target";
   private static final String CASTER = "Caster";
   private static final String IDENTITY = "Identity";
   private static final String CRUSH_AT = "HassanZabaniyaCrushAt";
   private static final String HERACLES_USED = "HassanZabaniyaHeartCrushed";
   private static final TagKey<EntityType<?>> HEARTLESS = tag("zabaniya_heartless");
   private static final TagKey<EntityType<?>> MECHANICAL = tag("zabaniya_mechanical_core");
   private static final TagKey<EntityType<?>> ORGANIC = tag("zabaniya_organic_heart");
   private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.65F, 0.015F, 0.025F), 1.2F);

   public enum Core { NONE, HEART, MECHANICAL, CLAY }

   private HassanZabaniyaService() { }

   private static TagKey<EntityType<?>> tag(String path) {
      return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, path));
   }

   public static boolean isExtractedItem(ItemStack stack) {
      return stack.getItem() instanceof net.xxxjk.TYPE_MOON_WORLD.item.custom.ZabaniyaCoreItem;
   }

   public static CompoundTag data(ItemStack stack) {
      return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
   }

   private static String identity(LivingEntity entity) {
      var definition = ServantIdentityHelper.definitionOf(entity);
      return definition == null ? "" : definition.id();
   }

   public static Core coreOf(LivingEntity target) {
      if (target == null || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target)) return Core.NONE;
      if ("enkidu".equals(identity(target))) return Core.CLAY;
      if (target.getType().is(HEARTLESS)) return Core.NONE;
      if (target.getType().is(MECHANICAL) || target.getPersistentData().getBoolean("TypeMoonMechanicalCore")) return Core.MECHANICAL;
      if (target.getType().is(ORGANIC)) return Core.HEART;
      if (target.getType().is(EntityTypeTags.UNDEAD) || target instanceof Slime
         || java.util.Set.of("pale_rider").contains(identity(target))) return Core.NONE;
      return Core.HEART;
   }

   public static boolean canExtract(LivingEntity target) {
      return coreOf(target) != Core.NONE && !("heracles".equals(identity(target)) && target.getPersistentData().getBoolean(HERACLES_USED))
         && !GodHandService.immune(target, "cursed_arm_hassan:zabaniya")
         && target.getPersistentData().getLong(EXPIRES) <= time(target);
   }

   private static long time(LivingEntity entity) {
      return entity.level().getServer().overworld().getGameTime();
   }

   public static boolean resolve(LivingEntity caster, LivingEntity target, float failureDamage, boolean canExtractRoll) {
      if (!(caster.level() instanceof ServerLevel level) || !EntityUtils.isValidCombatTarget(caster, target)
         || target.level() != level || CursedArmHassanCombatHelper.isForbiddenTarget(target)
         || ServantMasterTargeting.isContractMaster(caster, target)) return false;
      if (!canExtract(target)) {
         message(caster, "no_core", target.getDisplayName());
         return false;
      }
      if (target.getPersistentData().getBoolean("CausalSevered")) return false;
      long now = level.getGameTime();
      if (target.isInvulnerableTo(source(caster))
         || target.getPersistentData().getLong(net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem.TAG_INVULN_UNTIL) > now
         || target.getPersistentData().getLong("ServantCardCombatInvulnUntil") > now
         || net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatDefenseService.isDodgeActive(target, now)) return false;
      boolean succeeded = canExtractRoll && caster.getRandom().nextFloat() < 0.6F;
      if (succeeded && ArtoriaPendragonCombatHelper.tryNegateCertainHitOrDeath(target, "zabaniya", NoblePhantasmCastService.attackToken(caster))) return false;
      if (!succeeded) {
         if (coreOf(target) == Core.CLAY) return false;
         DamageSource source = source(caster);
         if (!GodHandService.conceptHit(target, source, failureDamage, "cursed_arm_hassan:zabaniya", false)
            && target.hurt(source, failureDamage)) CursedArmHassanCombatHelper.applyZabaniyaCurse(target);
         return false;
      }
      return extract(caster, target);
   }

   public static boolean extract(LivingEntity caster, LivingEntity target) {
      if (!(caster.level() instanceof ServerLevel) || caster.level() != target.level() || !canExtract(target)
         || !EntityUtils.isValidCombatTarget(caster, target) || CursedArmHassanCombatHelper.isForbiddenTarget(target)
         || ServantMasterTargeting.isContractMaster(caster, target)) return false;
      Core core = coreOf(target);
      ItemStack stack = new ItemStack(switch (core) {
         case MECHANICAL -> ModItems.ZABANIYA_MECHANICAL_CORE.get();
         case CLAY -> ModItems.ZABANIYA_DIVINE_CLAY.get();
         default -> ModItems.ZABANIYA_HEART.get();
      });
      UUID token = UUID.randomUUID();
      long expiry = time(caster) + LIFETIME;
      CompoundTag itemData = new CompoundTag();
      itemData.putUUID(TARGET, target.getUUID());
      itemData.putUUID(CASTER, caster.getUUID());
      itemData.putUUID(TOKEN, token);
      itemData.putString(IDENTITY, identity(target));
      itemData.putString("TargetName", target.getDisplayName().getString());
      itemData.putLong(EXPIRES, expiry);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(itemData));
      target.getPersistentData().putUUID(TOKEN, token);
      target.getPersistentData().putLong(EXPIRES, expiry);
      if (caster instanceof ServerPlayer player) {
         ItemStack held = player.getMainHandItem();
         if (!held.isEmpty()) storeOrDrop(player, held.copy());
         player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
         ItemStack offhand = player.getOffhandItem();
         if (!offhand.isEmpty()) storeOrDrop(player, offhand.copy());
         player.setItemInHand(InteractionHand.OFF_HAND, stack);
         message(player, core == Core.CLAY ? "clay_extracted" : "extracted", target.getDisplayName(), 60);
      } else if (caster instanceof CursedArmHassanEntity npc) {
         if (!npc.getMainHandItem().isEmpty()) npc.spawnAtLocation(npc.getMainHandItem().copy());
         if (!npc.getOffhandItem().isEmpty()) npc.spawnAtLocation(npc.getOffhandItem().copy());
         npc.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
         npc.setItemInHand(InteractionHand.OFF_HAND, stack);
         npc.getPersistentData().putLong(CRUSH_AT, time(npc) + npc.getRandom().nextIntBetweenInclusive(20, 60));
      }
      if (core != Core.CLAY && target instanceof ServerPlayer player) message(player, "heart_taken", 60);
      return true;
   }

   public static boolean tickNpc(CursedArmHassanEntity npc) {
      if (!npc.isAlive() || npc.isRemoved()) return false;
      ItemStack stack = npc.getOffhandItem();
      if (!isExtractedItem(stack)) return false;
      if (expire(stack, npc)) return false;
      CompoundTag binding = data(stack);
      LivingEntity target = find(npc, binding.hasUUID(TARGET) ? binding.getUUID(TARGET) : null);
      npc.getNavigation().stop();
      if (net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem.cannotAct(npc)
         || npc.hasEffect(net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects.PETRIFIED)) return true;
      if (target != null && target.isAlive()) {
         npc.getLookControl().setLookAt(target, 60.0F, 60.0F);
         npc.faceToward(target.position());
         npc.getMoveControl().strafe(-0.75F, 0.0F);
      }
      if (time(npc) >= npc.getPersistentData().getLong(CRUSH_AT)) {
         if (stack.is(ModItems.ZABANIYA_DIVINE_CLAY.get())) {
            npc.spawnAtLocation(stack.copy());
            stack.setCount(0);
         }
         else crush(npc, stack, InteractionHand.OFF_HAND);
         npc.getPersistentData().remove(CRUSH_AT);
      }
      return true;
   }

   public static boolean expire(ItemStack stack, LivingEntity holder) {
      CompoundTag binding = data(stack);
      if (!binding.contains(EXPIRES) || binding.getLong(EXPIRES) <= time(holder)) {
         stack.setCount(0);
         message(holder, "expired");
         return true;
      }
      return false;
   }

   public static boolean crush(LivingEntity holder, ItemStack stack, InteractionHand hand) {
      if (holder.level().isClientSide() || !holder.isAlive() || !isExtractedItem(stack) || stack.is(ModItems.ZABANIYA_DIVINE_CLAY.get())) return false;
      if (expire(stack, holder)) return false;
      CompoundTag binding = data(stack);
      LivingEntity target = find(holder, binding.hasUUID(TARGET) ? binding.getUUID(TARGET) : null);
      boolean valid = target != null && target.isAlive() && binding.hasUUID(TOKEN)
         && target.getPersistentData().hasUUID(TOKEN) && binding.getUUID(TOKEN).equals(target.getPersistentData().getUUID(TOKEN))
         && binding.getString(IDENTITY).equals(identity(target)) && !EntityUtils.isImmunePlayerTarget(target);
      boolean mechanical = stack.is(ModItems.ZABANIYA_MECHANICAL_CORE.get());
      stack.setCount(0);
      spawnCrushFx(holder, hand, mechanical);
      if (valid && !target.getPersistentData().getBoolean("CausalSevered")) {
         spawnTargetCrushFx(target, mechanical);
      }
      if (!valid || (target != null && target.getPersistentData().getBoolean("CausalSevered"))) {
         message(holder, "link_lost");
         return false;
      }
      target.getPersistentData().remove(TOKEN);
      target.getPersistentData().remove(EXPIRES);
      LivingEntity caster = find(holder, binding.hasUUID(CASTER) ? binding.getUUID(CASTER) : null);
      if (caster == null) caster = holder;
      if (!EntityUtils.isValidCombatTarget(caster, target) || ServantMasterTargeting.isContractMaster(caster, target)) return false;
      crushTarget(caster, target);
      return true;
   }

   private static void crushTarget(LivingEntity caster, LivingEntity target) {
      if (net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterDamageRouter.special(target, Float.MAX_VALUE,
         defender -> crushTarget(caster, defender))) return;
      DamageSource source = source(caster);
      target.setLastHurtByMob(caster);
      if (caster instanceof Player player) target.setLastHurtByPlayer(player);
      if (HeraclesGodHandHelper.hasGodHand(target)) target.getPersistentData().putBoolean(HERACLES_USED, true);
      if (!GodHandService.conceptHit(target, source, 0, "cursed_arm_hassan:zabaniya", true)) {
         target.setHealth(0.0F);
         target.die(source);
      }
   }

   private static DamageSource source(LivingEntity caster) {
      return new CombatDamageSource(caster instanceof Player player ? caster.damageSources().playerAttack(player)
         : caster.damageSources().mobAttack(caster), AttackOrigin.skill("cursed_arm_hassan", "zabaniya"));
   }

   private static LivingEntity find(LivingEntity holder, UUID uuid) {
      if (uuid == null) return null;
      for (ServerLevel level : holder.level().getServer().getAllLevels()) {
         Entity entity = level.getEntity(uuid);
         if (entity instanceof LivingEntity living) return living;
      }
      return null;
   }

   private static void spawnCrushFx(LivingEntity holder, InteractionHand hand, boolean mechanical) {
      if (!(holder.level() instanceof ServerLevel level)) return;
      Vec3 pos = holder instanceof Player player ? EntityUtils.getHandCastAnchor(player, hand) : EntityUtils.getLeftHandCastAnchor(holder);
      level.sendParticles(mechanical ? ParticleTypes.ELECTRIC_SPARK : BLOOD, pos.x, pos.y, pos.z,
         mechanical ? 80 : 110, 0.42, 0.48, 0.42, 0.12);
      level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,
         new ItemStack(mechanical ? ModItems.ZABANIYA_MECHANICAL_CORE.get() : ModItems.ZABANIYA_HEART.get())),
         pos.x, pos.y, pos.z, 36, 0.24, 0.26, 0.24, 0.14);
      level.playSound(null, pos.x, pos.y, pos.z, mechanical ? SoundEvents.IRON_GOLEM_HURT : SoundEvents.SLIME_SQUISH,
         SoundSource.PLAYERS, 1.0F, 0.65F);
      holder.swing(hand, true);
   }

   private static void spawnTargetCrushFx(LivingEntity target, boolean mechanical) {
      if (!(target.level() instanceof ServerLevel level)) return;
      Vec3 pos = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      level.sendParticles(mechanical ? ParticleTypes.ELECTRIC_SPARK : BLOOD, pos.x, pos.y, pos.z,
         mechanical ? 70 : 120, 0.48, 0.65, 0.48, 0.13);
      if (!mechanical) {
         level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, pos.x, pos.y, pos.z, 24,
            0.35, 0.45, 0.35, 0.1);
      }
   }

   private static void storeOrDrop(ServerPlayer player, ItemStack stack) {
      if (!stack.isEmpty() && !player.getInventory().add(stack)) player.drop(stack, false);
   }

   private static void message(LivingEntity entity, String key, Object... args) {
      if (entity instanceof ServerPlayer player) player.displayClientMessage(Component.translatable("message.typemoonworld.hassan." + key, args), true);
   }
}
