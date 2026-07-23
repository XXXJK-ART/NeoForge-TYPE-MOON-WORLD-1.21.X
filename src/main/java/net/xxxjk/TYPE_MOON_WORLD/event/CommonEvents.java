package net.xxxjk.TYPE_MOON_WORLD.event;

import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Added;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Expired;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Remove;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.advancement.TypeMoonAdvancementHelper;
import net.xxxjk.TYPE_MOON_WORLD.combat.OriginBulletHelper;
import net.xxxjk.TYPE_MOON_WORLD.effect.PetrifiedEffect;
import net.xxxjk.TYPE_MOON_WORLD.effect.SuggestionEffect;
import net.xxxjk.TYPE_MOON_WORLD.entity.CyanWindFieldEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.BrokenPhantasmProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.CrimsonHoundProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GaeBulgArmyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.PseudoSpiralSwordProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MerlinEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RhoAiasEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RyougiShikiEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.SwordBarrelProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ThompsonContenderItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.TempleStoneSwordAxeItem;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EmiyaArcherEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EnkiduCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EnkiduEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesGodHandHelper;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.MagicJewelMachineGun;
import net.xxxjk.TYPE_MOON_WORLD.magic.basic.MagicSuggestion;
import net.xxxjk.TYPE_MOON_WORLD.magic.nordic.MagicGander;
import net.xxxjk.TYPE_MOON_WORLD.magic.nordic.MagicGandrMachineGun;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardDefenseHandler;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTraitService;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.SowaExpertiseHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDefinitionLoader;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantSkillDefinitionLoader;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantNoblePhantasmDefinitionLoader;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiDefinitionLoader;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardDefinitionLoader;
import net.xxxjk.TYPE_MOON_WORLD.magic.data.MagicDefinitionLoader;
import net.xxxjk.TYPE_MOON_WORLD.network.DefinitionSnapshotService;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.utils.MerlinWorldEventLimiter;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SasakiKojiroCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantVoiceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CursedArmHassanEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CursedArmHassanCombatHelper;

@EventBusSubscriber(
   modid = "typemoonworld"
)
public class CommonEvents {
   private static final String GOD_HAND_REVIVE_LOCK_TAG = "GodHandReviveLockUntil";
   private static final String GOD_HAND_HIGH_DAMAGE_REVIVE_UNTIL_TAG = "GodHandHighDamageReviveUntil";
   private static final String BATTLE_CONTINUATION_RECOVERY_ACTIVE_TAG = "BattleContinuationRecoveryActive";
   private static final String BATTLE_CONTINUATION_LAST_HEAL_TICK_TAG = "BattleContinuationLastHealTick";
   private static final double BATTLE_CONTINUATION_TRIGGER_HEALTH_RATIO = 0.20;
   private static final double EMIYA_BATTLE_CONTINUATION_TRIGGER_HEALTH_RATIO = 0.50;
   private static final float BATTLE_CONTINUATION_HEAL_AMOUNT = 10.0F;
   private static final int BATTLE_CONTINUATION_HEAL_INTERVAL_TICKS = 20;
   private static final String EFFECT_RESISTANCE_REENTRY_TAG = "TypeMoonAdjustingHarmfulEffect";
   private static final Map<String, Set<UUID>> SUGGESTED_MOB_IDS_BY_DIMENSION = new ConcurrentHashMap<>();
   private static final Map<String, Set<UUID>> SERVANT_IDS_BY_DIMENSION = new ConcurrentHashMap<>();
   private static final Map<String, Set<UUID>> SHIKI_IDS_BY_DIMENSION = new ConcurrentHashMap<>();

   @SubscribeEvent
   public static void onPlayerTickPre(net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre event) {
      if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof ServerPlayer serverPlayer)) {
         return;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = serverPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.servant_card_transformed || vars.master_active) {
         ServantCardTransformManager.normalizeFood(serverPlayer);
      }
   }

   @SubscribeEvent
   public static void onServantCardFall(LivingFallEvent event) {
      if (!(event.getEntity() instanceof ServerPlayer player)) {
         return;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.servant_card_transformed && "gilgamesh".equals(vars.servant_card_id)) {
         player.fallDistance = 0.0F;
         event.setDistance(0.0F);
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onAddReloadListeners(AddReloadListenerEvent event) {
      event.addListener(new ServantDefinitionLoader());
      event.addListener(new ServantCardDefinitionLoader());
      event.addListener(new MagicDefinitionLoader());
      event.addListener(new ServantSkillDefinitionLoader());
      event.addListener(new ServantNoblePhantasmDefinitionLoader());
      event.addListener(new ServantAiDefinitionLoader());
   }

   @SubscribeEvent
   public static void onEntityJoin(EntityJoinLevelEvent event) {
      if (!event.getLevel().isClientSide) {
         if (event.getEntity() instanceof ServantEntity servant && event.getLevel() instanceof ServerLevel serverLevel) {
            trackServant(servant, serverLevel);
         }
         if (event.getEntity() instanceof RyougiShikiEntity shiki && event.getLevel() instanceof ServerLevel serverLevel) {
            trackShiki(shiki, serverLevel);
         }
         // Pass servantId from spawn eggs after entity creation.
         if (event.getEntity() instanceof Monster monster) {
            try {
               addTypeMoonTargetGoals(monster);
            } catch (Exception var3) {
            }
         }
         if (event.getEntity() instanceof net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity mage) {
            try {
               mage.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(mage,
                  net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity.class, true,
                  e -> e instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity s
                     && s.getDefinition() != null
                     && s.getDefinition().faction() == net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantFaction.BEAST));
            } catch (Exception ignored) {
            }
         }
         if (event.getEntity() instanceof net.xxxjk.TYPE_MOON_WORLD.entity.RyougiShikiEntity shiki) {
            try {
               shiki.targetSelector.addGoal(7, new NearestAttackableTargetGoal<>(shiki,
                  net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity.class, true,
                  e -> e instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity s
                     && s.getDefinition() != null
                     && s.getDefinition().faction() == net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantFaction.BEAST));
            } catch (Exception ignored) {
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLevelTick(Post event) {
      if (!event.getLevel().isClientSide) {
         if (event.getLevel() instanceof ServerLevel serverLevel) {
            // Sasaki Kojiro Presence Concealment D check.
            if (serverLevel.getGameTime() % 40L == 0L) {
               forEachTrackedServant(serverLevel, servant -> {
                  if (!servant.getPersistentData().getBoolean("StealthPassiveActive")) {
                     return;
                  }
                  CompoundTag data = servant.getPersistentData();
                  long currentTick = serverLevel.getGameTime();
                  long lastHurtTick = data.getLong("LastHurtTick");
                  boolean wasRecentlyHurt = servant.invulnerableTime > 10 || (currentTick - lastHurtTick) < 100;
                  if (servant.hasEffect(MobEffects.INVISIBILITY) && !wasRecentlyHurt) {
                     servant.removeEffect(MobEffects.INVISIBILITY);
                  }
               });
            }
            if (serverLevel.getGameTime() % 20L == 0L) {
               forEachTrackedServant(serverLevel, servant -> {
                  if (servant instanceof CursedArmHassanEntity hassan && hassan.hasEffect(MobEffects.INVISIBILITY)) {
                     CursedArmHassanCombatHelper.clearNonServantTargeting(hassan);
                  }
               });
            }
            if (serverLevel.getGameTime() % 10L == 0L) {
               tickSuggestedMobs(serverLevel);
            }
            if (serverLevel.getGameTime() % 200L == 0L) {
               List<ServerPlayer> players = serverLevel.players();
               if (!players.isEmpty()) {
                  if (!(serverLevel.random.nextFloat() >= 0.02F)) {
                     ServerPlayer ref = players.get(serverLevel.random.nextInt(players.size()));
                     BlockPos center = ref.blockPosition();
                     Holder<Biome> biome = serverLevel.getBiome(center);
                     if (biome.is(Biomes.CHERRY_GROVE) || biome.is(Biomes.MEADOW)) {
                        int radius = 20;
                        double checkR = 64.0;
                        AABB box = new AABB(
                           center.getX() - checkR,
                           center.getY() - 6.0,
                           center.getZ() - checkR,
                           center.getX() + checkR,
                           center.getY() + 6.0,
                           center.getZ() + checkR
                        );
                        List<MerlinEntity> merlins = serverLevel.getEntitiesOfClass(MerlinEntity.class, box, LivingEntity::isAlive);
                        if (merlins.isEmpty()) {
                           BlockPos spawnPos = null;

                           for (int i = 0; i < 20 && spawnPos == null; i++) {
                              int dx = serverLevel.random.nextInt(radius * 2 + 1) - radius;
                              int dz = serverLevel.random.nextInt(radius * 2 + 1) - radius;
                              BlockPos trial = center.offset(dx, 0, dz);
                              BlockPos top = serverLevel.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, trial);
                              if (serverLevel.getBlockState(top).isAir()) {
                                 BlockState below = serverLevel.getBlockState(top.below());
                                 if (!below.isAir()) {
                                    spawnPos = top;
                                 }
                              }
                           }

                           if (spawnPos != null) {
                              MerlinEntity merlin = ModEntities.MERLIN.get().create(serverLevel);
                              if (merlin != null) {
                                 merlin.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, ref.getYRot(), ref.getXRot());
                                 serverLevel.addFreshEntity(merlin);
                                 TypeMoonAdvancementHelper.grantNearby(
                                    serverLevel, Vec3.atCenterOf(spawnPos), 24.0, TypeMoonAdvancementHelper.WANDERER_OF_PARADISE
                                 );
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
      if (!event.getEntity().level().isClientSide) {
         Player player = event.getEntity();
         if (player instanceof ServerPlayer serverPlayer) {
            MagicJewelMachineGun.tick(serverPlayer);
            MagicGandrMachineGun.tick(serverPlayer);
            MagicGander.tick(serverPlayer);
            if (serverPlayer.tickCount % 20 == 0) {
               TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)serverPlayer.getData(
                  TypeMoonWorldModVariables.PLAYER_VARIABLES
               );
               TypeMoonAdvancementHelper.syncPassive(serverPlayer, vars);
            }
            TypeMoonWorldModVariables.PlayerVariables cardVars = (TypeMoonWorldModVariables.PlayerVariables)serverPlayer.getData(
               TypeMoonWorldModVariables.PLAYER_VARIABLES
            );
            ServantCardTransformManager.tick(serverPlayer, cardVars);
            MasterStateManager.tick(serverPlayer, cardVars);
            MasterServantLinkService.tick(serverPlayer, cardVars);
         }

         if (player.isSpectator()) {
            if (!player.getActiveEffects().isEmpty()) {
               player.removeAllEffects();
            }
         } else {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.getItem() instanceof TempleStoneSwordAxeItem) {
               boolean skipDebuff = player instanceof LivingEntity le
                  && le.level().getEntity(le.getId()) instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity servant
                  && servant.isExemptFromStoneAxeDebuff();
               if (!skipDebuff && player instanceof ServerPlayer serverPlayer) {
                  TypeMoonWorldModVariables.PlayerVariables vars = serverPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                  skipDebuff = vars.servant_card_transformed && "heracles".equals(vars.servant_card_id);
               }
               if (!skipDebuff) {
                  boolean hasStrength = player.hasEffect(MobEffects.DAMAGE_BOOST);
                  if (hasStrength) {
                     if (player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                     }

                     if (player.hasEffect(MobEffects.DIG_SLOWDOWN)) {
                        player.removeEffect(MobEffects.DIG_SLOWDOWN);
                     }
                  } else {
                     if (!player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                     }

                     if (!player.hasEffect(MobEffects.DIG_SLOWDOWN)) {
                        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 2));
                     }
                  }
               }
            }

            if (player instanceof ServerPlayer serverPlayer) {
               boolean isSleeping = player.isSleeping();
               CompoundTag tag = player.getPersistentData();
               boolean prevSleeping = tag.getBoolean("TypeMoonMerlinPrevSleeping");
               TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)serverPlayer.getData(
                  TypeMoonWorldModVariables.PLAYER_VARIABLES
               );
               int favor = vars.merlin_favor;
               if (isSleeping && !prevSleeping && serverPlayer.level() instanceof ServerLevel serverLevel) {
                  float chance = 0.15F;
                  if (serverLevel.random.nextFloat() < chance) {
                     if (favor <= -1 && MerlinWorldEventLimiter.tryConsume(serverLevel)) {
                        serverPlayer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 0));
                        serverPlayer.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 0));
                        Component msg = Component.translatable("entity.typemoonworld.merlin.speech.sleep_negative");
                        serverPlayer.displayClientMessage(msg, false);
                     } else if (favor >= 3 && MerlinWorldEventLimiter.tryConsume(serverLevel)) {
                        serverPlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 0));
                        serverPlayer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 0));
                        Component msg = Component.translatable("entity.typemoonworld.merlin.speech.sleep_positive");
                        serverPlayer.displayClientMessage(msg, false);
                     }
                  }
               }

               tag.putBoolean("TypeMoonMerlinPrevSleeping", isSleeping);
               if (serverPlayer.level() instanceof ServerLevel serverLevelx) {
                  if (favor < 0) {
                     if (serverLevelx.random.nextInt(2000) == 0 && MerlinWorldEventLimiter.tryConsume(serverLevelx)) {
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
                        Component msg = Component.translatable("entity.typemoonworld.merlin.speech.prank_negative");
                        serverPlayer.displayClientMessage(msg, false);
                     }
                  } else if (favor > 0 && favor < 3 && serverLevelx.random.nextInt(2600) == 0 && MerlinWorldEventLimiter.tryConsume(serverLevelx)) {
                     double baseY = player.getY() + 0.8 + player.level().random.nextDouble() * 0.8;
                     serverLevelx.sendParticles(ParticleTypes.CHERRY_LEAVES, player.getX(), baseY, player.getZ(), 24, 0.8, 0.6, 0.8, 0.02);
                     Component msg = Component.translatable("entity.typemoonworld.merlin.speech.prank_positive");
                     serverPlayer.displayClientMessage(msg, false);
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
      if (!event.getEntity().level().isClientSide) {
         if (EntityUtils.isSpectatorPlayer(event.getEntity())) {
            event.setCanceled(true);
         } else {
            if (net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderDamageTypes.isInfection(event.getSource())) {
               return;
            }
            if (event.getSource().getEntity() instanceof LivingEntity attackerWithPetrify
               && attackerWithPetrify.hasEffect(ModMobEffects.PETRIFIED)) {
               event.setCanceled(true);
               return;
            }
            if (event.getSource().getEntity() instanceof LivingEntity attackerWithBinding
               && blocksAttacks(attackerWithBinding)) {
               event.setCanceled(true);
               return;
            }
            if (event.getSource().getDirectEntity() instanceof LivingEntity directWithPetrify
               && directWithPetrify.hasEffect(ModMobEffects.PETRIFIED)) {
               event.setCanceled(true);
               return;
            }
            if (event.getSource().getDirectEntity() instanceof LivingEntity directWithBinding
               && blocksAttacks(directWithBinding)) {
               event.setCanceled(true);
               return;
            }
            Entity directEntity = event.getSource().getDirectEntity();
            if (directEntity instanceof Projectile projectile && projectile.getOwner() == event.getEntity()) {
               event.setCanceled(true);
            } else if (directEntity instanceof CyanWindFieldEntity windField && windField.getOwner() == event.getEntity()) {
               event.setCanceled(true);
            } else {
            handleContenderBulletDamage(event, directEntity);
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
               event.setAmount(ArtoriaPendragonCombatHelper.applyManaBurstOutgoing(attacker, event.getAmount()));
               event.setAmount(ServantCardTraitService.applyOutgoingDamage(attacker, event.getEntity(), event.getAmount()));
            }
            if (event.getEntity() instanceof ServerPlayer player) {
               TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
                  TypeMoonWorldModVariables.PLAYER_VARIABLES
               );
               if (vars.servant_card_transformed && "enkidu".equals(vars.servant_card_id)
                  && net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardEnkiduSkills.isEnumaElishActive(player)
                  && !net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderDamageTypes.bypassesEnkiduNoblePhantasm(event.getSource())) {
                  event.setCanceled(true);
                  event.setAmount(0.0F);
                  return;
               }
               if (vars.servant_card_transformed
                  && "paracelsus".equals(vars.servant_card_id)
                  && isParacelsusIgnoredDamage(event.getSource())) {
                  event.setCanceled(true);
                  event.setAmount(0.0F);
                  player.clearFire();
                  return;
               }
               if (vars.servant_card_transformed
                  && "oda_nobunaga".equals(vars.servant_card_id)
                  && isOdaNobunagaIgnoredDamage(event.getSource())) {
                  event.setCanceled(true);
                  event.setAmount(0.0F);
                  player.clearFire();
                  return;
               }
               if (!SowaExpertiseHelper.rollBypass(event.getSource()) && ServantCardDefenseHandler.handleIncomingDamage(player, vars, event)) {
                  return;
               }
               if (net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardGawainSkills.tryConsumeBeltGuts(player, vars, event)) {
                  return;
               }
               if (vars.servant_card_transformed && player.getHealth() - event.getAmount() <= 0.0F) {
                  MasterServantLinkService.onServantDeath(player, vars);
               }
               if (vars.servant_card_transformed
                  && vars.servant_card_death_release
                  && !ServantCardDefenseHandler.isSpecialNoblePhantasmDamage(event.getSource(), event.getAmount())
                  && player.getHealth() - event.getAmount() <= 0.0F) {
                  event.setCanceled(true);
                  event.setAmount(0.0F);
                  ServantCardTransformManager.release(player, true);
                  return;
               }
               if (vars.master_active && vars.master_revive_available && player.getHealth() - event.getAmount() <= 0.0F) {
                  event.setCanceled(true);
                  event.setAmount(0.0F);
                  MasterStateManager.tryRevive(player, vars);
                  return;
               }
               if (vars.master_active && player.getHealth() - event.getAmount() <= 0.0F) {
                  ServerPlayer servant = MasterServantLinkService.getLinkedServant(player, vars);
                  if (servant != null) {
                     MasterServantLinkService.breakLink(player, servant, true);
                  }
               }
            }
            if (event.getEntity() instanceof LivingEntity living) {
                  if (!OriginBulletHelper.isOriginBulletDamage(event.getSource()) && tryRedirectRhoAiasDamage(living, event)) {
                     return;
                  }
                  if (living instanceof EnkiduEntity enkidu && EnkiduCombatHelper.isEnumaElishActive(enkidu)
                     && !net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderDamageTypes.bypassesEnkiduNoblePhantasm(event.getSource())) {
                     event.setCanceled(true);
                     event.setAmount(0.0F);
                     if (enkidu.level() instanceof ServerLevel serverLevel && enkidu.tickCount % 6 == 0) {
                        serverLevel.sendParticles(
                           ParticleTypes.END_ROD,
                           enkidu.getX(), enkidu.getY() + enkidu.getBbHeight() * 0.55, enkidu.getZ(),
                           14, 0.32, 0.42, 0.32, 0.05
                        );
                        serverLevel.sendParticles(
                           ParticleTypes.HAPPY_VILLAGER,
                           enkidu.getX(), enkidu.getY() + enkidu.getBbHeight() * 0.5, enkidu.getZ(),
                           8, 0.24, 0.3, 0.24, 0.035
                        );
                     }
                     return;
                  }
                  if (living instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonEntity artoria
                     && ArtoriaPendragonCombatHelper.tryNegateMedeaSmallMagic(artoria, event.getSource(), event.getAmount())) {
                     event.setCanceled(true);
                     return;
                  }
                  if (living instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonEntity artoria
                     && directEntity instanceof Projectile projectile
                     && ArtoriaPendragonCombatHelper.tryDodgeProjectileWithInstinct(artoria, projectile)) {
                     event.setCanceled(true);
                     return;
                  }
                  event.setAmount(MagicResistanceHelper.applyMagicDamageReduction(living, event.getSource(), event.getAmount()));
                  event.setAmount(ArtoriaPendragonCombatHelper.applyAvalonDamageReduction(living, event.getSource(), event.getAmount()));
                  if (event.getAmount() <= 0.0F) {
                     event.setCanceled(true);
                     return;
                  }
               }
               if (event.getSource().is(DamageTypes.FALL)) {
                  LivingEntity mob = event.getEntity();
                  if (mob instanceof LivingEntity
                     && (mob.hasEffect(ModMobEffects.REINFORCEMENT_SELF_AGILITY) || mob.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_AGILITY))) {
                     event.setAmount(event.getAmount() * 0.5F);
                  }
               }

               if (event.getSource().getEntity() instanceof Player player) {
                  if (player.hasEffect(ModMobEffects.NINE_LIVES)) {
                     player.removeEffect(ModMobEffects.NINE_LIVES);

                     LivingEntity target = event.getEntity();
                     double baseDamageAttr = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                     float baseDamage = (float)baseDamageAttr;
                     triggerNineLives(player, target, baseDamage);
                  }

                  if (event.getEntity() instanceof LivingEntity && player.level() instanceof ServerLevel serverLevel) {
                     TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
                        TypeMoonWorldModVariables.PLAYER_VARIABLES
                     );
                     if (vars.merlin_favor >= 3 && serverLevel.random.nextFloat() < 0.05F && MerlinWorldEventLimiter.tryConsume(serverLevel)) {
                        int duration = 1200;
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0, true, true));
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0, true, true));
                        double radius = 24.0;
                        AABB box = new AABB(
                           player.getX() - radius,
                           player.getY() - 4.0,
                           player.getZ() - radius,
                           player.getX() + radius,
                           player.getY() + 4.0,
                           player.getZ() + radius
                        );
                        List<MerlinEntity> merlins = serverLevel.getEntitiesOfClass(MerlinEntity.class, box, LivingEntity::isAlive);
                        if (!merlins.isEmpty()) {
                           Component message = Component.translatable("entity.typemoonworld.merlin.speech.buff_support_high");

                           for (ServerPlayer sp : serverLevel.players()) {
                              if (sp.distanceToSqr(player) <= radius * radius) {
                                 sp.displayClientMessage(message, false);
                              }
                           }
                        }
                     }
                  }
               }

               if (event.getEntity() instanceof ServerPlayer damagedPlayer && damagedPlayer.level() instanceof ServerLevel serverLevelx) {
                  TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)damagedPlayer.getData(
                     TypeMoonWorldModVariables.PLAYER_VARIABLES
                  );
                  if (vars.merlin_favor >= 5) {
                     if (event.getSource().getEntity() instanceof Player attacker) {
                        TypeMoonWorldModVariables.PlayerVariables attackerVars = (TypeMoonWorldModVariables.PlayerVariables)attacker.getData(
                           TypeMoonWorldModVariables.PLAYER_VARIABLES
                        );
                        if (attackerVars.merlin_favor >= 3) {
                           return;
                        }
                     }

                     float maxHealth = damagedPlayer.getMaxHealth();
                     if (damagedPlayer.getHealth() <= maxHealth * 0.5F) {
                        if (serverLevelx.random.nextFloat() >= 0.15F) {
                           return;
                        }

                        if (MerlinWorldEventLimiter.tryConsume(serverLevelx)) {
                           double radius = 24.0;
                           AABB box = new AABB(
                              damagedPlayer.getX() - radius,
                              damagedPlayer.getY() - 4.0,
                              damagedPlayer.getZ() - radius,
                              damagedPlayer.getX() + radius,
                              damagedPlayer.getY() + 4.0,
                              damagedPlayer.getZ() + radius
                           );
                           List<MerlinEntity> merlins = serverLevelx.getEntitiesOfClass(MerlinEntity.class, box, LivingEntity::isAlive);
                           MerlinEntity helper = merlins.isEmpty() ? null : merlins.get(0);
                           if (helper == null) {
                              helper = ModEntities.MERLIN.get().create(serverLevelx);
                              if (helper != null) {
                                 double dx = (serverLevelx.random.nextDouble() - 0.5) * 2.0;
                                 double dz = (serverLevelx.random.nextDouble() - 0.5) * 2.0;
                                 helper.moveTo(
                                    damagedPlayer.getX() + dx,
                                    damagedPlayer.getY(),
                                    damagedPlayer.getZ() + dz,
                                    damagedPlayer.getYRot(),
                                    damagedPlayer.getXRot()
                                 );
                                 if (helper.getAttribute(Attributes.MAX_HEALTH) != null) {
                                    helper.getAttribute(Attributes.MAX_HEALTH).setBaseValue(300.0);
                                 }

                                 helper.setHealth(300.0F);
                                 CompoundTag htag = helper.getPersistentData();
                                 htag.putBoolean("TypeMoonHelperClone", true);
                                 htag.putString("TypeMoonHelperOwner", damagedPlayer.getUUID().toString());
                                 serverLevelx.addFreshEntity(helper);
                              }
                           }

                           if (helper != null) {
                              CompoundTag htag = helper.getPersistentData();
                              if (!htag.getBoolean("TypeMoonHelperClone")) {
                                 htag.putBoolean("TypeMoonSummonedFull", true);
                              }

                              AABB mobBox = new AABB(
                                 damagedPlayer.getX() - radius,
                                 damagedPlayer.getY() - 4.0,
                                 damagedPlayer.getZ() - radius,
                                 damagedPlayer.getX() + radius,
                                 damagedPlayer.getY() + 4.0,
                                 damagedPlayer.getZ() + radius
                              );

                              for (Monster mob : serverLevelx.getEntitiesOfClass(Monster.class, mobBox, m -> m.getTarget() == damagedPlayer)) {
                                 mob.setTarget(helper);
                              }

                              Component message = Component.translatable("entity.typemoonworld.merlin.speech.guard_summon");

                              for (ServerPlayer spx : serverLevelx.players()) {
                                 if (spx.distanceToSqr(damagedPlayer) <= radius * radius) {
                                    spx.displayClientMessage(message, false);
                                 }
                              }

                              TypeMoonAdvancementHelper.grant(damagedPlayer, TypeMoonAdvancementHelper.SEPARATE_TRUTH_FROM_FALSEHOOD);
                           }
                        }
                     }
                  }
               }

               if (event.getSource().getEntity() instanceof MerlinEntity merlin
                  && event.getEntity() instanceof Monster mob
                  && (mob.getTarget() == null || mob.getTarget() != merlin)) {
                  mob.setTarget(merlin);
               }
            }
         }
      }

      // ========== God Hand and battle continuation servant damage handling ==========
      if (event.getEntity() instanceof ServantEntity servant && !event.isCanceled()) {
         handleServantDamage(servant, event);
      }
   }

   @SubscribeEvent
   public static void onMobEffectRemoved(Remove event) {
      restorePetrifiedMobState(event.getEntity(), event.getEffect().value());
      clearBasicMagecraftEffectTags(event.getEntity(), event.getEffect().value());
   }

   @SubscribeEvent
   public static void onDefinitionSnapshotLogin(PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) DefinitionSnapshotService.send(player);
   }

   @SubscribeEvent
   public static void onDefinitionSnapshotReload(OnDatapackSyncEvent event) {
      event.getRelevantPlayers().forEach(DefinitionSnapshotService::send);
   }

   @SubscribeEvent
   public static void onMobEffectExpired(Expired event) {
      if (event.getEffectInstance() != null) {
         restorePetrifiedMobState(event.getEntity(), event.getEffectInstance().getEffect().value());
         clearBasicMagecraftEffectTags(event.getEntity(), event.getEffectInstance().getEffect().value());
      }
   }

   // ======================== 十二试炼 / 战斗续行 ========================

   private static boolean isMajorBrokenPhantasmExplosion(DamageSource source, float originalDamage) {
      if (!source.is(DamageTypeTags.IS_EXPLOSION) || originalDamage < 300.0F) {
         return false;
      }
      Entity direct = source.getDirectEntity();
      if (direct instanceof PseudoSpiralSwordProjectileEntity
         || direct instanceof CrimsonHoundProjectileEntity
         || direct instanceof BrokenPhantasmProjectileEntity) {
         return true;
      }
      if (direct instanceof SwordBarrelProjectileEntity swordBarrel) {
         return swordBarrel.isBrokenPhantasm();
      }
      return direct instanceof EmiyaArcherEntity;
   }

   private static boolean isGaeBulgArmyDamage(DamageSource source) {
      return source != null && source.getDirectEntity() instanceof GaeBulgArmyProjectileEntity;
   }

   private static boolean isHeraclesPoisonOrWitherSpecialAttack(ServantEntity servant, DamageSource source) {
      return HeraclesGodHandHelper.hasGodHand(servant)
         && source != null
         && (source.is(NeoForgeMod.POISON_DAMAGE) || source.is(DamageTypes.WITHER));
   }

   private static void handleServantDamage(ServantEntity servant, LivingIncomingDamageEvent event) {
      if (servant.level().isClientSide()) return;

      CompoundTag data = servant.getPersistentData();
      float damage = event.getAmount();
      float originalDamage = damage;
      long currentTick = servant.level().getGameTime();
      boolean majorBrokenPhantasmExplosion = isMajorBrokenPhantasmExplosion(event.getSource(), originalDamage);
      boolean artoriaExcalibur = isArtoriaExcaliburDamage(event.getSource());
      boolean gaeBulgArmy = isGaeBulgArmyDamage(event.getSource());
      boolean originBullet = OriginBulletHelper.isOriginBulletDamage(event.getSource());
      boolean antiHeraclesNoblePhantasm = HeraclesGodHandHelper.hasGodHand(servant) && (majorBrokenPhantasmExplosion || gaeBulgArmy);
      boolean heraclesPoisonOrWitherSpecialAttack = isHeraclesPoisonOrWitherSpecialAttack(servant, event.getSource());
      boolean enkiduWitherUndefendable = servant instanceof EnkiduEntity && EnkiduCombatHelper.isPerfectFormUndefendableDamage(event.getSource());
      boolean invisibleAirBypass = data.getLong(ArtoriaPendragonCombatHelper.TAG_INVISIBLE_AIR_DAMAGE_BYPASS_UNTIL) > currentTick;
      boolean inPlaceGodHandRevive = shouldUseInPlaceGodHandRevive(event.getSource(), originalDamage);
      boolean paleRiderInfection = net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderDamageTypes.isInfection(event.getSource());
      if (servant instanceof EnkiduEntity enkidu && EnkiduCombatHelper.isFireDamage(event.getSource())) {
         EnkiduCombatHelper.extinguishFire(enkidu);
         event.setCanceled(true);
         event.setAmount(0.0F);
         return;
      }
      if (inPlaceGodHandRevive) {
         data.putLong(GOD_HAND_HIGH_DAMAGE_REVIVE_UNTIL_TAG, currentTick + 2L);
      }

      // Record last hurt time for passive combat checks.
      data.putLong("LastHurtTick", currentTick);
      if (invisibleAirBypass && servant instanceof CursedArmHassanEntity hassan && CursedArmHassanCombatHelper.tryDodge(hassan, event.getSource())) {
         event.setCanceled(true);
         data.remove(ArtoriaPendragonCombatHelper.TAG_INVISIBLE_AIR_DAMAGE_BYPASS_UNTIL);
         return;
      }
      if (invisibleAirBypass) {
         data.remove(ArtoriaPendragonCombatHelper.TAG_INVISIBLE_AIR_DAMAGE_BYPASS_UNTIL);
      }
      if (!paleRiderInfection && !originBullet && !enkiduWitherUndefendable && ServantCombatSystem.isUntargetable(servant)) {
         event.setCanceled(true);
         return;
      }

      if (!originBullet && !artoriaExcalibur && !antiHeraclesNoblePhantasm && !heraclesPoisonOrWitherSpecialAttack && !enkiduWitherUndefendable && !invisibleAirBypass) {
         ServantCombatSystem.handleIncomingDamage(servant, event);
         if (event.isCanceled()) {
            return;
         }
      }
      damage = event.getAmount();
      if (servant instanceof EnkiduEntity enkidu) {
         event.setAmount(EnkiduCombatHelper.applyPerfectFormPassiveDamageReduction(enkidu, event));
         damage = event.getAmount();
      }
      if (servant instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.LiShuwenEntity liShuwen) {
         event.setAmount(net.xxxjk.TYPE_MOON_WORLD.servant.entity.LiShuwenCombatHelper.applyIncomingDamageModifiers(liShuwen, event.getSource(), event.getAmount()));
         damage = event.getAmount();
      }
      if (servant instanceof EnkiduEntity enkidu && EnkiduCombatHelper.tryClayBodyOnHeavyDamage(enkidu, event)) {
         return;
      }
      if (CuChulainnCombatHelper.isCuChulainn(servant)) {
         CuChulainnCombatHelper.markCombat(servant);
         if (data.getBoolean(CuChulainnCombatHelper.PROTECTION_FROM_ARROWS_TAG)
            && !CuChulainnCombatHelper.isMovementRestricted(servant)
            && !event.getSource().is(DamageTypeTags.IS_EXPLOSION)
            && event.getSource().getDirectEntity() instanceof Projectile projectile
            && projectile.getOwner() != servant) {
            if (servant.level() instanceof ServerLevel sl) {
               sl.sendParticles(ParticleTypes.END_ROD,
                  servant.getX(), servant.getY() + servant.getBbHeight() * 0.55, servant.getZ(),
                  8, 0.2, 0.25, 0.2, 0.02);
               sl.sendParticles(ParticleTypes.ENCHANT,
                  servant.getX(), servant.getY() + servant.getBbHeight() * 0.5, servant.getZ(),
                  12, 0.3, 0.4, 0.3, 0.03);
            }
            event.setCanceled(true);
            return;
         }

         float shield = data.getFloat(CuChulainnCombatHelper.ALGIZ_SHIELD_TAG);
         if (!originBullet && shield > 0.0F) {
            if (majorBrokenPhantasmExplosion) {
               float minimumDamage = originalDamage * 0.5F;
               float absorbable = Math.max(0.0F, damage - minimumDamage);
               float absorbed = Math.min(shield, absorbable);
               if (absorbed > 0.0F) {
                  event.setAmount(damage - absorbed);
                  damage = event.getAmount();
                  if (shield > absorbed) {
                     data.putFloat(CuChulainnCombatHelper.ALGIZ_SHIELD_TAG, shield - absorbed);
                  } else {
                     data.remove(CuChulainnCombatHelper.ALGIZ_SHIELD_TAG);
                  }
                  if (servant.level() instanceof ServerLevel sl) {
                     sl.sendParticles(ParticleTypes.WAX_OFF,
                        servant.getX(), servant.getY() + servant.getBbHeight() * 0.55, servant.getZ(),
                        16, 0.45, 0.45, 0.45, 0.05);
                     sl.sendParticles(ParticleTypes.FLAME,
                        servant.getX(), servant.getY() + servant.getBbHeight() * 0.5, servant.getZ(),
                        12, 0.35, 0.3, 0.35, 0.04);
                  }
               }
               if (damage <= 0.0F) {
                  event.setAmount(minimumDamage);
                  damage = event.getAmount();
               }
            } else {
            if (shield >= damage) {
               data.putFloat(CuChulainnCombatHelper.ALGIZ_SHIELD_TAG, shield - damage);
               if (servant.level() instanceof ServerLevel sl) {
                  sl.sendParticles(ParticleTypes.WAX_ON,
                     servant.getX(), servant.getY() + servant.getBbHeight() * 0.55, servant.getZ(),
                     10, 0.3, 0.4, 0.3, 0.02);
               }
               event.setCanceled(true);
               return;
            }

            event.setAmount(damage - shield);
            damage = event.getAmount();
            data.remove(CuChulainnCombatHelper.ALGIZ_SHIELD_TAG);
            if (servant.level() instanceof ServerLevel sl) {
               sl.sendParticles(ParticleTypes.WAX_OFF,
                  servant.getX(), servant.getY() + servant.getBbHeight() * 0.55, servant.getZ(),
                  12, 0.35, 0.4, 0.35, 0.03);
            }
            }
         }
      }
      if (SasakiKojiroCombatHelper.isSasakiKojiro(servant)
         && data.getBoolean(SasakiKojiroCombatHelper.MINDSEYE_ACTIVE_TAG)
         && (event.getSource().getEntity() != null || event.getSource().getDirectEntity() != null)) {
         SasakiKojiroCombatHelper.markCombat(servant);

         int durabilityLoss = SasakiKojiroCombatHelper.damageBladeFromIncomingAttack(servant, damage);
         if (durabilityLoss > 0 && servant.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CRIT,
               servant.getX(), servant.getY() + servant.getBbHeight() * 0.6, servant.getZ(),
               6, 0.2, 0.2, 0.2, 0.05);
            sl.playSound(null, servant.blockPosition(),
               SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.75F, 1.5F);
         }
      }

      // --- God Hand: immunity against low-rank damage ---
      if (data.getBoolean("GodHandActive")) {
         float threshold = data.getFloat("GodHandThreshold");
         if (!paleRiderInfection && !heraclesPoisonOrWitherSpecialAttack && !artoriaExcalibur && !majorBrokenPhantasmExplosion && !gaeBulgArmy && damage < threshold) {
            if (servant.level() instanceof ServerLevel sl) {
               sl.sendParticles(ParticleTypes.ENCHANT,
                  servant.getX(), servant.getY() + servant.getBbHeight() * 0.5, servant.getZ(),
                  8, 0.4, 0.4, 0.4, 0.1);
               sl.playSound(null, servant.blockPosition(),
                  SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.6F, 1.4F);
            }
            event.setCanceled(true);
            return;
         }

         // Adaptive resistance: repeated damage types are reduced over time.
         if (!paleRiderInfection && !heraclesPoisonOrWitherSpecialAttack && !artoriaExcalibur && !majorBrokenPhantasmExplosion && !gaeBulgArmy) {
            float reduction = data.getFloat("GodHandAdaptiveReduction");
            float maxReduction = data.getFloat("GodHandAdaptiveMax");
            float currentResistance = data.getFloat("GodHandCurrentResistance");
            if (currentResistance < maxReduction) {
               data.putFloat("GodHandCurrentResistance",
                  Math.min(currentResistance + reduction, maxReduction));
            }
            float resistanceNow = data.getFloat("GodHandCurrentResistance");
            if (resistanceNow > 0) {
               event.setAmount(damage * (1.0F - resistanceNow));
            }
         } else if (servant.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLASH,
               servant.getX(), servant.getY() + servant.getBbHeight() * 0.55, servant.getZ(),
               1, 0.0, 0.0, 0.0, 0.0);
            sl.sendParticles(ParticleTypes.FLAME,
               servant.getX(), servant.getY() + servant.getBbHeight() * 0.45, servant.getZ(),
               24, 0.55, 0.5, 0.55, 0.06);
         }
      }

      // --- 十二试炼：致死时满血复活（最多 11 次） ---
      // 斩断因果时跳过复活
      boolean causalSevered = data.getBoolean("CausalSevered");
      if (!causalSevered && data.getBoolean("GodHandActive") && servant.getHealth() - event.getAmount() <= 0) {
         int livesLeft = data.getInt("GodHandLives");
         if (livesLeft > 0) {
            event.setCanceled(true);
            if (inPlaceGodHandRevive) {
               reviveGodHandInPlace(servant, data, livesLeft - 1);
               return;
            }
            if (respawnHeraclesFromGodHand(servant, livesLeft - 1)) {
               return;
            }

            servant.setHealth(servant.getMaxHealth());
            servant.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 4, false, false, true));
            data.putInt("GodHandLives", livesLeft - 1);
            data.putLong(GOD_HAND_REVIVE_LOCK_TAG, servant.level().getGameTime() + 20L);
            return;
         }
      }

      // --- 战斗续行 A：致死时保留 1HP + 5s 无敌，5min CD ---
      // 斩断因果时跳过
      double battleContinuationRatio = servant instanceof EmiyaArcherEntity
         ? EMIYA_BATTLE_CONTINUATION_TRIGGER_HEALTH_RATIO
         : BATTLE_CONTINUATION_TRIGGER_HEALTH_RATIO;
      if (!causalSevered && data.getBoolean("BattleContinuationActive")
         && canTriggerBattleContinuation(data)
         && servant.getHealth() - event.getAmount() <= servant.getMaxHealth() * battleContinuationRatio) {
         int cd = data.getInt("BattleContinuationCooldown");
         if (cd <= 0) {
            event.setCanceled(true);
            event.setAmount(0.0F);
            servant.setHealth(Math.max(1.0F, servant.getMaxHealth() * (float)battleContinuationRatio));
            servant.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 3, false, false, true));
            data.putBoolean(BATTLE_CONTINUATION_RECOVERY_ACTIVE_TAG, true);
            data.remove(BATTLE_CONTINUATION_LAST_HEAL_TICK_TAG);
            data.putInt("BattleContinuationCooldown",
               data.getInt("BattleContinuationMaxCooldown"));

            if (servant.level() instanceof ServerLevel sl) {
               sl.sendParticles(ParticleTypes.CRIT,
                  servant.getX(), servant.getY() + 0.8, servant.getZ(),
                  15, 0.5, 0.5, 0.5, 0.4);
               sl.sendParticles(ParticleTypes.CLOUD,
                  servant.getX(), servant.getY() + 0.2, servant.getZ(),
                  10, 0.3, 0.3, 0.3, 0.05);
               sl.playSound(null, servant.blockPosition(),
                  SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 0.8F, 1.5F);
            }
         }
      }
   }

   // ======================== 从者冷却计时器 ========================

   private static boolean canTriggerBattleContinuation(CompoundTag data) {
      return !data.getBoolean("GodHandActive") || data.getInt("GodHandLives") <= 0;
   }

   @SubscribeEvent
   public static void onServantLevelTick(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
      if (event.getLevel().isClientSide()) return;
      if (event.getLevel() instanceof ServerLevel sl) {
         long gameTime = sl.getGameTime();
         if (gameTime % 5L != 0L) {
            return;
         }
         forEachTrackedServant(sl, servant -> {
            CompoundTag data = servant.getPersistentData();
            SasakiKojiroCombatHelper.repairBladeOutOfCombat(servant);
            CuChulainnCombatHelper.tickStatus(servant);
            if (data.getInt("BattleContinuationCooldown") > 0) {
               data.putInt("BattleContinuationCooldown", Math.max(0, data.getInt("BattleContinuationCooldown") - 5));
            }
            tickBattleContinuationRecovery(servant, data, sl);
         });
      }
   }

   private static void tickBattleContinuationRecovery(ServantEntity servant, CompoundTag data, ServerLevel level) {
      if (!data.getBoolean(BATTLE_CONTINUATION_RECOVERY_ACTIVE_TAG)) {
         return;
      }

      if (!servant.isAlive() || servant.getHealth() >= servant.getMaxHealth()) {
         data.remove(BATTLE_CONTINUATION_RECOVERY_ACTIVE_TAG);
         data.remove(BATTLE_CONTINUATION_LAST_HEAL_TICK_TAG);
         return;
      }

      long now = level.getGameTime();
      long lastHealTick = data.getLong(BATTLE_CONTINUATION_LAST_HEAL_TICK_TAG);
      if (lastHealTick > 0L && now - lastHealTick < BATTLE_CONTINUATION_HEAL_INTERVAL_TICKS) {
         return;
      }

      servant.heal(BATTLE_CONTINUATION_HEAL_AMOUNT);
      data.putLong(BATTLE_CONTINUATION_LAST_HEAL_TICK_TAG, now);
      level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
         servant.getX() + (servant.getRandom().nextDouble() - 0.5) * 0.6,
         servant.getY() + servant.getBbHeight(),
         servant.getZ() + (servant.getRandom().nextDouble() - 0.5) * 0.6,
         3, 0.03, 0.05, 0.03, 0.0);

      if (servant.getHealth() >= servant.getMaxHealth()) {
         data.remove(BATTLE_CONTINUATION_RECOVERY_ACTIVE_TAG);
         data.remove(BATTLE_CONTINUATION_LAST_HEAL_TICK_TAG);
      }
   }

   @SubscribeEvent
   public static void onLivingDeath(LivingDeathEvent event) {
      if (!event.getEntity().level().isClientSide) {
         if (event.getEntity() instanceof Player player) {
            OriginBulletHelper.clearPlayerSeal(player);
         }

         if (event.getEntity() instanceof ServerPlayer player) {
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            if (vars.servant_card_transformed) {
               ServantCardTransformManager.prepareVanishingEquipment(player, vars);
            }
            if (vars.master_active) {
               MasterStateManager.release(player);
            }
         }

         if (event.getEntity() instanceof ServantEntity servant) {
            CompoundTag data = servant.getPersistentData();
            boolean causalSevered = data.getBoolean("CausalSevered");
            if (!causalSevered && data.getBoolean("GodHandActive")) {
               int livesLeft = data.getInt("GodHandLives");
               if (livesLeft > 0) {
                  event.setCanceled(true);
                  if (shouldUseInPlaceGodHandRevive(event.getSource(), 0.0F)
                     || servant.level().getGameTime() <= data.getLong(GOD_HAND_HIGH_DAMAGE_REVIVE_UNTIL_TAG)) {
                     reviveGodHandInPlace(servant, data, livesLeft - 1);
                     return;
                  }
                  if (respawnHeraclesFromGodHand(servant, livesLeft - 1)) {
                     return;
                  }
               }
            }
         }

         ServantEntity servantKiller = null;
         if (event.getSource().getEntity() instanceof ServantEntity servant) {
            servantKiller = servant;
         } else if (event.getSource().getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof ServantEntity servantOwner) {
            servantKiller = servantOwner;
         }

         if (servantKiller != null && event.getEntity() instanceof LivingEntity defeatedLiving) {
            ServantVoiceHelper.tryPlayVictory(servantKiller, defeatedLiving);
         }

         if (event.getSource().getEntity() instanceof Player player
            && event.getEntity() instanceof Monster mob
            && mob.getTarget() instanceof MerlinEntity merlin
            && merlin.isAlive()
            && player.level() instanceof ServerLevel) {
            TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
               TypeMoonWorldModVariables.PLAYER_VARIABLES
            );
            if (vars.merlin_favor < 5) {
               vars.merlin_favor = Math.min(5, vars.merlin_favor + 1);
               vars.syncPlayerVariables(player);
            }
         }
         // 英灵死亡后清理 pendingServantId，防止下一个刷怪蛋继承错误ID
      }
   }

   @SubscribeEvent
   public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
      Entity e = event.getEntity();
      if (e instanceof ServantEntity && event.getLevel() instanceof ServerLevel serverLevel) {
         untrackServant(e, serverLevel);
      }
      if (e instanceof RyougiShikiEntity) {
         if (event.getLevel() instanceof ServerLevel serverLevel) {
            untrackShiki(e, serverLevel);
         }
         RemovalReason reason = e.getRemovalReason();
         if (reason != null) {
            if (reason == RemovalReason.KILLED
               || reason == RemovalReason.DISCARDED
               || reason == RemovalReason.UNLOADED_TO_CHUNK
               || reason == RemovalReason.UNLOADED_WITH_PLAYER
               || reason == RemovalReason.CHANGED_DIMENSION) {
               if (event.getLevel() instanceof ServerLevel serverLevel) {
                  speakNearby(serverLevel, e, "entity.typemoonworld.ryougi_shiki.speech.refused", 20.0);
               }

               if (e instanceof RyougiShikiEntity shiki) {
                  shiki.setPersistenceRequired();
                  shiki.invulnerableTime = 0;
               }
            }
         }
      }
   }

   private static boolean respawnHeraclesFromGodHand(ServantEntity servant, int remainingLives) {
      if (!(servant instanceof HeraclesEntity) || !(servant.level() instanceof ServerLevel serverLevel)) {
         return false;
      }

      CompoundTag snapshot = servant.saveWithoutId(new CompoundTag());
      snapshot.remove("UUID");
      HeraclesEntity replacement = new HeraclesEntity(ModEntities.HERACLES.get(), serverLevel);
      replacement.load(snapshot);
      replacement.moveTo(servant.getX(), servant.getY(), servant.getZ(), servant.getYRot(), servant.getXRot());
      replacement.setYBodyRot(servant.yBodyRot);
      replacement.setYHeadRot(servant.getYHeadRot());
      replacement.setDeltaMovement(Vec3.ZERO);
      replacement.setNoGravity(false);
      replacement.clearFire();
      replacement.setTarget(servant.getTarget());
      replacement.getPersistentData().merge(servant.getPersistentData().copy());
      replacement.getPersistentData().putInt("GodHandLives", remainingLives);
      replacement.getPersistentData().remove("CausalSevered");
      replacement.getPersistentData().putLong(GOD_HAND_REVIVE_LOCK_TAG, serverLevel.getGameTime() + 20L);
      replacement.removeEffect(ModMobEffects.PETRIFIED);
      replacement.getPersistentData().remove(PetrifiedEffect.TAG_PREV_NO_AI);
      replacement.setNoAi(false);
      replacement.setHealth(replacement.getMaxHealth());
      replacement.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 4, false, false, true));
      replacement.setPersistenceRequired();

      serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
         servant.getX(), servant.getY() + servant.getBbHeight() * 0.8, servant.getZ(),
         60, 0.8, 1.2, 0.8, 0.03);
      serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
         servant.getX(), servant.getY() + servant.getBbHeight() * 0.65, servant.getZ(),
         36, 0.9, 1.0, 0.9, 0.02);
      serverLevel.sendParticles(ParticleTypes.CLOUD,
         servant.getX(), servant.getY() + 0.3, servant.getZ(),
         45, 0.85, 0.35, 0.85, 0.04);
      serverLevel.sendParticles(ParticleTypes.POOF,
         servant.getX(), servant.getY() + servant.getBbHeight() * 0.5, servant.getZ(),
         24, 0.55, 0.75, 0.55, 0.03);
      serverLevel.playSound(null, servant.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 1.0F, 0.8F);

      LivingEntity oldTarget = servant.getTarget();
      servant.remove(RemovalReason.DISCARDED);
      serverLevel.addFreshEntity(replacement);
      if (oldTarget != null && oldTarget.isAlive()) {
         replacement.setTarget(oldTarget);
      }

      AABB retargetBox = replacement.getBoundingBox().inflate(96.0);
      for (net.minecraft.world.entity.Mob mob : serverLevel.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, retargetBox)) {
         if (mob == replacement) {
            continue;
         }
         if (mob.getTarget() == servant) {
            mob.setTarget(replacement);
         }
         if (mob.getLastHurtByMob() == servant) {
            mob.setLastHurtByMob(replacement);
         }
      }
      return true;
   }

   private static boolean shouldUseInPlaceGodHandRevive(DamageSource source, float originalDamage) {
      return isArtoriaExcaliburDamage(source) || originalDamage >= 100.0F;
   }

   private static void reviveGodHandInPlace(ServantEntity servant, CompoundTag data, int remainingLives) {
      data.putInt("GodHandLives", remainingLives);
      data.remove("CausalSevered");
      data.remove(GOD_HAND_REVIVE_LOCK_TAG);
      data.remove(GOD_HAND_HIGH_DAMAGE_REVIVE_UNTIL_TAG);
      servant.clearFire();
      servant.invulnerableTime = 0;
      servant.hurtTime = 0;
      servant.hurtDuration = 0;
      servant.setHealth(servant.getMaxHealth());
      servant.setDeltaMovement(servant.getDeltaMovement().multiply(0.55, 1.0, 0.55));
      servant.hurtMarked = true;

      if (servant.level() instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
            servant.getX(), servant.getY() + servant.getBbHeight() * 0.75, servant.getZ(),
            24, 0.6, 0.8, 0.6, 0.025);
         serverLevel.sendParticles(ParticleTypes.POOF,
            servant.getX(), servant.getY() + servant.getBbHeight() * 0.5, servant.getZ(),
            14, 0.45, 0.55, 0.45, 0.025);
         serverLevel.playSound(null, servant.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 0.75F, 0.78F);
      }
   }

   private static boolean isArtoriaExcaliburDamage(DamageSource source) {
      return source != null && source.getDirectEntity() instanceof ArtoriaExcaliburBeamEntity;
   }

   private static void restorePetrifiedMobState(LivingEntity entity, net.minecraft.world.effect.MobEffect effect) {
      if (effect != ModMobEffects.PETRIFIED.get()) {
         return;
      }
      if (entity instanceof net.minecraft.world.entity.Mob mob) {
         boolean previousNoAi = mob.getPersistentData().getBoolean(PetrifiedEffect.TAG_PREV_NO_AI);
         mob.setNoAi(previousNoAi);
         mob.getPersistentData().remove(PetrifiedEffect.TAG_PREV_NO_AI);
         mob.getNavigation().stop();
         mob.setTarget(null);
      }
   }

   @SubscribeEvent
   public static void onLivingDrops(LivingDropsEvent event) {
      if (event.getEntity() instanceof Player) {
         event.getDrops().removeIf(drop -> ServantCardTransformManager.shouldDeleteBoundDrop(drop.getItem()));
      }
   }

   private static boolean blocksAttacks(LivingEntity entity) {
      if (entity == null) {
         return false;
      }
      MobEffectInstance binding = entity.getEffect(ModMobEffects.BINDING);
      if (binding != null && (binding.getAmplifier() > 0 || entity.getPersistentData().getBoolean(net.xxxjk.TYPE_MOON_WORLD.effect.BindingEffect.TAG_FULL_BIND))) {
         return true;
      }
      MobEffectInstance suggestion = entity.getEffect(ModMobEffects.SUGGESTION);
      return suggestion != null && suggestion.getAmplifier() >= 3;
   }

   private static String dimensionKey(ServerLevel level) {
      return level.dimension().location().toString();
   }

   private static void trackServant(ServantEntity servant, ServerLevel level) {
      if (servant != null && level != null) {
         SERVANT_IDS_BY_DIMENSION.computeIfAbsent(dimensionKey(level), unused -> ConcurrentHashMap.newKeySet()).add(servant.getUUID());
      }
   }

   private static void trackShiki(RyougiShikiEntity shiki, ServerLevel level) {
      if (shiki != null && level != null) {
         SHIKI_IDS_BY_DIMENSION.computeIfAbsent(dimensionKey(level), unused -> ConcurrentHashMap.newKeySet()).add(shiki.getUUID());
      }
   }

   private static void untrackServant(Entity entity, ServerLevel level) {
      if (entity == null || level == null) {
         return;
      }
      Set<UUID> ids = SERVANT_IDS_BY_DIMENSION.get(dimensionKey(level));
      if (ids != null) {
         ids.remove(entity.getUUID());
      }
   }

   private static void untrackShiki(Entity entity, ServerLevel level) {
      if (entity == null || level == null) {
         return;
      }
      Set<UUID> ids = SHIKI_IDS_BY_DIMENSION.get(dimensionKey(level));
      if (ids != null) {
         ids.remove(entity.getUUID());
      }
   }

   private static boolean hasTrackedServants(net.minecraft.world.level.Level level) {
      if (!(level instanceof ServerLevel serverLevel)) {
         return false;
      }
      Set<UUID> ids = SERVANT_IDS_BY_DIMENSION.get(dimensionKey(serverLevel));
      return ids != null && !ids.isEmpty();
   }

   private static boolean hasTrackedShiki(net.minecraft.world.level.Level level) {
      if (!(level instanceof ServerLevel serverLevel)) {
         return false;
      }
      Set<UUID> ids = SHIKI_IDS_BY_DIMENSION.get(dimensionKey(serverLevel));
      return ids != null && !ids.isEmpty();
   }

   private static void addTypeMoonTargetGoals(Monster monster) {
      monster.targetSelector.addGoal(
         3,
         new NearestAttackableTargetGoal<RyougiShikiEntity>(monster, RyougiShikiEntity.class, 20, true, false, null) {
            @Override
            public boolean canUse() {
               return hasTrackedShiki(this.mob.level()) && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
               return hasTrackedShiki(this.mob.level()) && super.canContinueToUse();
            }
         }
      );
      monster.targetSelector.addGoal(
         4,
         new NearestAttackableTargetGoal<ServantEntity>(monster, ServantEntity.class, 20, true, false, null) {
            @Override
            public boolean canUse() {
               return hasTrackedServants(this.mob.level()) && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
               return hasTrackedServants(this.mob.level()) && super.canContinueToUse();
            }
         }
      );
   }

   private static void forEachTrackedServant(ServerLevel level, Consumer<ServantEntity> consumer) {
      Set<UUID> ids = SERVANT_IDS_BY_DIMENSION.get(dimensionKey(level));
      if (ids == null || ids.isEmpty()) {
         return;
      }
      Iterator<UUID> iterator = ids.iterator();
      while (iterator.hasNext()) {
         Entity entity = level.getEntity(iterator.next());
         if (!(entity instanceof ServantEntity servant) || !servant.isAlive()) {
            iterator.remove();
            continue;
         }
         consumer.accept(servant);
      }
   }

   private static void trackSuggestedMob(LivingEntity entity) {
      if (entity != null && entity.level() instanceof ServerLevel level) {
         String key = dimensionKey(level);
         SUGGESTED_MOB_IDS_BY_DIMENSION.computeIfAbsent(key, unused -> ConcurrentHashMap.newKeySet()).add(entity.getUUID());
      }
   }

   private static void untrackSuggestedMob(LivingEntity entity) {
      if (entity == null) {
         return;
      }
      if (entity.level() instanceof ServerLevel level) {
         Set<UUID> ids = SUGGESTED_MOB_IDS_BY_DIMENSION.get(dimensionKey(level));
         if (ids != null) {
            ids.remove(entity.getUUID());
         }
      } else {
         for (Set<UUID> ids : SUGGESTED_MOB_IDS_BY_DIMENSION.values()) {
            ids.remove(entity.getUUID());
         }
      }
   }

   private static void tickSuggestedMobs(ServerLevel level) {
      Set<UUID> ids = SUGGESTED_MOB_IDS_BY_DIMENSION.get(dimensionKey(level));
      if (ids == null || ids.isEmpty()) {
         return;
      }
      Iterator<UUID> iterator = ids.iterator();
      while (iterator.hasNext()) {
         Entity entity = level.getEntity(iterator.next());
         if (!(entity instanceof Mob mob) || !mob.isAlive() || !mob.hasEffect(ModMobEffects.SUGGESTION)) {
            iterator.remove();
            continue;
         }
         int command = mob.getPersistentData().getInt(SuggestionEffect.TAG_COMMAND);
         if (command == MagicSuggestion.COMMAND_STOP) {
            mob.getNavigation().stop();
            mob.setTarget(null);
         } else if (command == MagicSuggestion.COMMAND_FOLLOW && mob.getPersistentData().hasUUID(SuggestionEffect.TAG_CASTER)) {
            Entity caster = level.getEntity(mob.getPersistentData().getUUID(SuggestionEffect.TAG_CASTER));
            if (caster instanceof LivingEntity living && living.isAlive()) {
               mob.setTarget(null);
               if (mob.distanceToSqr(living) > 9.0) {
                  mob.getNavigation().moveTo(living, 1.0);
               }
            }
         } else if (command == MagicSuggestion.COMMAND_AWAY && mob.getPersistentData().hasUUID(SuggestionEffect.TAG_CASTER)) {
            Entity caster = level.getEntity(mob.getPersistentData().getUUID(SuggestionEffect.TAG_CASTER));
            if (caster != null) {
               Vec3 away = mob.position().subtract(caster.position());
               if (away.lengthSqr() < 1.0E-6) {
                  away = mob.getLookAngle();
               }
               Vec3 pos = mob.position().add(away.normalize().scale(8.0));
               mob.setTarget(null);
               mob.getNavigation().moveTo(pos.x, pos.y, pos.z, 1.15);
            }
         } else if (command == MagicSuggestion.COMMAND_CONFUSE) {
            Vec3 pos = mob.position().add((mob.getRandom().nextDouble() - 0.5) * 6.0, 0.0, (mob.getRandom().nextDouble() - 0.5) * 6.0);
            mob.setTarget(null);
            mob.getNavigation().moveTo(pos.x, pos.y, pos.z, 0.85);
         }
      }
   }

   private static void clearBasicMagecraftEffectTags(LivingEntity entity, net.minecraft.world.effect.MobEffect effect) {
      if (entity == null || effect == null) {
         return;
      }
      if (effect == ModMobEffects.BINDING.get()) {
         entity.getPersistentData().remove(net.xxxjk.TYPE_MOON_WORLD.effect.BindingEffect.TAG_FULL_BIND);
      } else if (effect == ModMobEffects.SUGGESTION.get()) {
         untrackSuggestedMob(entity);
         entity.getPersistentData().remove(SuggestionEffect.TAG_COMMAND);
         entity.getPersistentData().remove(SuggestionEffect.TAG_CASTER);
         entity.getPersistentData().remove(SuggestionEffect.TAG_ATTACK_TARGET);
      }
   }

   private static void speakNearby(ServerLevel level, Entity center, String key, double radius) {
      for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, center.getBoundingBox().inflate(radius))) {
         p.displayClientMessage(Component.translatable(key), false);
      }
   }

   private static boolean tryRedirectRhoAiasDamage(LivingEntity living, LivingIncomingDamageEvent event) {
      if (!(living.level() instanceof ServerLevel level) || event.getAmount() <= 0.0F) {
         return false;
      }

      for (RhoAiasEntity shield : level.getEntitiesOfClass(
         RhoAiasEntity.class,
         living.getBoundingBox().inflate(8.0),
         entity -> entity.isAlive() && entity.protects(living)
      )) {
         float incoming = event.getAmount();
         float absorbed = shield.absorb(incoming);
         float remaining = Math.max(0.0F, incoming - absorbed);
         level.sendParticles(
            ParticleTypes.END_ROD,
            living.getX(),
            living.getY() + living.getBbHeight() * 0.55,
            living.getZ(),
            8,
            0.25,
            0.25,
            0.25,
            0.01
         );
         level.sendParticles(
            ParticleTypes.ENCHANT,
            shield.getX(),
            shield.getY(),
            shield.getZ(),
            12,
            0.5,
            0.5,
            0.5,
            0.02
         );
         if (remaining <= 0.0F) {
            event.setCanceled(true);
            return true;
         }
         event.setAmount(remaining);
         return false;
      }
      return false;
   }

   private static void handleContenderBulletDamage(LivingIncomingDamageEvent event, Entity directEntity) {
      if (directEntity == null || !directEntity.getPersistentData().getBoolean(ThompsonContenderItem.CONTENDER_ARROW_TAG)) {
         return;
      }
      LivingEntity target = event.getEntity();
      boolean origin = directEntity.getPersistentData().getBoolean(ThompsonContenderItem.ORIGIN_ARROW_TAG);
      event.setAmount(Math.max(event.getAmount(), 20.0F));
      if (target.level() instanceof ServerLevel level) {
         level.sendParticles(origin ? ParticleTypes.SOUL : ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), origin ? 22 : 10, 0.2, 0.22, 0.2, 0.05);
         level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + target.getBbHeight() * 0.45, target.getZ(), origin ? 12 : 5, 0.16, 0.18, 0.16, 0.035);
         level.playSound(null, target.getX(), target.getY(), target.getZ(), origin ? SoundEvents.SOUL_ESCAPE.value() : SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, origin ? 0.8F : 0.65F, origin ? 0.8F : 1.25F);
      }
      if (!origin) {
         return;
      }
      if (target instanceof Player player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (vars.is_magic_circuit_open) {
            event.setAmount(event.getAmount() + (float)Math.max(0.0, vars.player_max_mana));
         }
         OriginBulletHelper.sealPlayerUntilDeath(player);
      }
      if (target.isAlive() && !OriginBulletHelper.isNpcSealed(target)) {
         OriginBulletHelper.sealNpc(target);
      }
      if (OriginBulletHelper.isServantTarget(target)) {
         float bonus = (float)Math.max(0.0, OriginBulletHelper.maxMpOf(target) / 10.0);
         if (bonus > 0.0F) {
            event.setAmount(event.getAmount() + bonus);
         }
      }
   }

   private static boolean isParacelsusIgnoredDamage(DamageSource source) {
      return source != null
         && (source.is(DamageTypeTags.IS_FIRE)
            || source.is(DamageTypes.LAVA)
            || source.is(DamageTypes.HOT_FLOOR)
            || source.is(DamageTypes.IN_FIRE)
            || source.is(DamageTypes.ON_FIRE)
            || source.is(DamageTypes.IN_WALL));
   }

   private static boolean isOdaNobunagaIgnoredDamage(DamageSource source) {
      return source != null
         && (source.is(DamageTypeTags.IS_FIRE)
            || source.is(DamageTypes.LAVA)
            || source.is(DamageTypes.HOT_FLOOR)
            || source.is(DamageTypes.IN_FIRE)
            || source.is(DamageTypes.ON_FIRE));
   }

   @SubscribeEvent
   public static void onEffectAdded(Added event) {
      if (EntityUtils.isSpectatorPlayer(event.getEntity())) {
         event.getEntity().removeAllEffects();
      } else {
         LivingEntity living = event.getEntity();
         MobEffectInstance effectInstance = event.getEffectInstance();
         if (living instanceof Mob && effectInstance != null && effectInstance.getEffect() == ModMobEffects.SUGGESTION) {
            trackSuggestedMob(living);
         }
         if (living != null && effectInstance != null && tryRedirectRhoAiasEffect(living, effectInstance)) {
            return;
         }
         if (living != null
            && effectInstance != null
            && effectInstance.getDuration() > 1
            && effectInstance.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL
            && !living.getPersistentData().getBoolean(EFFECT_RESISTANCE_REENTRY_TAG)) {
            int adjustedDuration = MagicResistanceHelper.applyDebuffResistance(living, effectInstance.getDuration());
            if (adjustedDuration < effectInstance.getDuration()) {
               living.getPersistentData().putBoolean(EFFECT_RESISTANCE_REENTRY_TAG, true);
               try {
                  living.removeEffect(effectInstance.getEffect());
                  living.addEffect(new MobEffectInstance(
                     effectInstance.getEffect(),
                     adjustedDuration,
                     effectInstance.getAmplifier(),
                     effectInstance.isAmbient(),
                     effectInstance.isVisible(),
                     effectInstance.showIcon()
                  ));
               } finally {
                  living.getPersistentData().remove(EFFECT_RESISTANCE_REENTRY_TAG);
               }
               return;
            }
         }
         if (event.getEffectInstance().getEffect() == ModMobEffects.NINE_LIVES
            && !event.getEntity().level().isClientSide
            && event.getEntity().level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 20; i++) {
               int delay = i * 2;
               TYPE_MOON_WORLD.queueServerWork(
                  delay,
                  () -> {
                     serverLevel.sendParticles(
                        ParticleTypes.CLOUD, event.getEntity().getX(), event.getEntity().getY() + 0.2, event.getEntity().getZ(), 40, 2.0, 0.2, 2.0, 0.4
                     );
                     serverLevel.sendParticles(
                        ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        event.getEntity().getX(),
                        event.getEntity().getY() + 0.2,
                        event.getEntity().getZ(),
                        10,
                        1.5,
                        0.5,
                        1.5,
                        0.2
                     );
                     if (serverLevel.random.nextFloat() < 0.3F) {
                        serverLevel.sendParticles(
                           ParticleTypes.POOF, event.getEntity().getX(), event.getEntity().getY() + 0.5, event.getEntity().getZ(), 15, 1.0, 0.2, 1.0, 0.3
                        );
                        serverLevel.sendParticles(
                           ParticleTypes.LARGE_SMOKE, event.getEntity().getX(), event.getEntity().getY() + 0.5, event.getEntity().getZ(), 5, 0.5, 0.2, 0.5, 0.2
                        );
                     }
                  }
               );
            }

            serverLevel.playSound(
               null, event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 0.8F
            );
            TYPE_MOON_WORLD.queueServerWork(
               10,
               () -> serverLevel.playSound(
                  null,
                  event.getEntity().getX(),
                  event.getEntity().getY(),
                  event.getEntity().getZ(),
                  SoundEvents.FIRE_EXTINGUISH,
                  SoundSource.PLAYERS,
                  1.0F,
                  0.6F
               )
            );
            TYPE_MOON_WORLD.queueServerWork(
               25,
               () -> serverLevel.playSound(
                  null,
                  event.getEntity().getX(),
                  event.getEntity().getY(),
                  event.getEntity().getZ(),
                  SoundEvents.GENERIC_EXTINGUISH_FIRE,
                  SoundSource.PLAYERS,
                  1.0F,
                  0.5F
               )
            );
         }
      }
   }

   private static boolean tryRedirectRhoAiasEffect(LivingEntity living, MobEffectInstance effectInstance) {
      if (!(living.level() instanceof ServerLevel level)
         || effectInstance.getEffect().value().getCategory() != net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
         return false;
      }

      for (RhoAiasEntity shield : level.getEntitiesOfClass(
         RhoAiasEntity.class,
         living.getBoundingBox().inflate(8.0),
         entity -> entity.isAlive() && entity.protects(living)
      )) {
         living.removeEffect(effectInstance.getEffect());
         shield.absorb(10.0F + effectInstance.getAmplifier() * 5.0F);
         level.sendParticles(
            ParticleTypes.WAX_ON,
            shield.getX(),
            shield.getY(),
            shield.getZ(),
            10,
            0.5,
            0.5,
            0.5,
            0.02
         );
         return true;
      }
      return false;
   }

   private static void triggerNineLives(Player player, LivingEntity target, float damageBase) {
      Vec3[] lastCenter = new Vec3[]{new Vec3(target.getX(), target.getY() + target.getBbHeight() / 2.0F, target.getZ())};

      for (int i = 1; i <= 8; i++) {
         int tickDelay = i * 2;
         TYPE_MOON_WORLD.queueServerWork(
            tickDelay,
            () -> {
               if (player.level() instanceof ServerLevel serverLevel) {
                  Vec3 viewDir = player.getLookAngle().normalize();
                  Vec3 upDir = new Vec3(0.0, 1.0, 0.0);
                  Vec3 rightDir = viewDir.cross(upDir).normalize();
                  if (rightDir.lengthSqr() < 0.01) {
                     rightDir = new Vec3(1.0, 0.0, 0.0);
                  }

                  Vec3 planeUp = rightDir.cross(viewDir).normalize();
                  float ts = Math.max(target.getBbWidth(), target.getBbHeight());
                  double angle = serverLevel.random.nextDouble() * 2.0 * Math.PI;
                  double dist = ts * 1.5 + 4.0;
                  double cxOff = (serverLevel.random.nextDouble() - 0.5) * target.getBbWidth() * 0.8;
                  double cyOff = (serverLevel.random.nextDouble() - 0.5) * target.getBbHeight() * 0.8;
                  Vec3 center = target.isAlive()
                     ? new Vec3(target.getX(), target.getY() + target.getBbHeight() / 2.0F, target.getZ()).add(rightDir.scale(cxOff)).add(planeUp.scale(cyOff))
                     : lastCenter[0];
                  lastCenter[0] = center;
                  serverLevel.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 10, 0.5, 0.5, 0.5, 0.5);
                  serverLevel.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z, 3, 0.3, 0.3, 0.3, 0.05);
                  player.level().playSound(null, center.x, center.y, center.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.5F);
                  Vec3 slashDir = rightDir.scale(Math.cos(angle)).add(planeUp.scale(Math.sin(angle))).normalize();
                  Vec3 startPos = center.add(slashDir.scale(-dist));
                  Vec3 endPos = center.add(slashDir.scale(dist));
                  RubyProjectileEntity slash = new RubyProjectileEntity(serverLevel, startPos.x, startPos.y, startPos.z);
                  slash.setItem(ItemStack.EMPTY);
                  slash.setGemType(99);
                  slash.setVisualScale(Math.max(0.5F, ts / 1.8F));
                  slash.setVisualEnd(endPos);
                  slash.setNoGravity(true);
                  slash.setDeltaMovement(Vec3.ZERO);
                  serverLevel.addFreshEntity(slash);
                  double radius = ts * 0.75 + 2.0;
                  AABB box = new AABB(center.x - radius, center.y - radius, center.z - radius, center.x + radius, center.y + radius, center.z + radius);

                  for (LivingEntity e : serverLevel.getEntitiesOfClass(
                     LivingEntity.class, box, ex -> ex.isAlive() && ex != player && !EntityUtils.isImmunePlayerTarget(ex)
                  )) {
                     e.invulnerableTime = 0;
                     e.hurt(player.damageSources().mobAttack(player), damageBase * 2.0F);
                     e.invulnerableTime = 0;
                  }
               }
            }
         );
      }

      TYPE_MOON_WORLD.queueServerWork(
         26,
         () -> {
            if (player.level() instanceof ServerLevel serverLevel) {
               Vec3 viewDir = player.getLookAngle().normalize();
               Vec3 upDir = new Vec3(0.0, 1.0, 0.0);
               Vec3 rightDir = viewDir.cross(upDir).normalize();
               if (rightDir.lengthSqr() < 0.01) {
                  rightDir = new Vec3(1.0, 0.0, 0.0);
               }

               Vec3 planeUp = rightDir.cross(viewDir).normalize();
               float ts = Math.max(target.getBbWidth(), target.getBbHeight());
               double angle = serverLevel.random.nextDouble() * 2.0 * Math.PI;
               double dist = ts * 1.5 + 4.0;
               Vec3 center = lastCenter[0];
               Vec3 slashDir = rightDir.scale(Math.cos(angle)).add(planeUp.scale(Math.sin(angle))).normalize();
               Vec3 startPos = center.add(slashDir.scale(-dist));
               Vec3 endPos = center.add(slashDir.scale(dist));
               RubyProjectileEntity slash = new RubyProjectileEntity(serverLevel, startPos.x, startPos.y, startPos.z);
               slash.setItem(ItemStack.EMPTY);
               slash.setGemType(99);
               slash.setVisualScale(Math.max(0.5F, ts / 1.8F));
               slash.setVisualEnd(endPos);
               slash.setNoGravity(true);
               slash.setDeltaMovement(Vec3.ZERO);
               serverLevel.addFreshEntity(slash);
               double radius = ts * 0.75 + 2.0;
               AABB box = new AABB(center.x - radius, center.y - radius, center.z - radius, center.x + radius, center.y + radius, center.z + radius);

               for (LivingEntity e : serverLevel.getEntitiesOfClass(
                  LivingEntity.class, box, ex -> ex.isAlive() && ex != player && !EntityUtils.isImmunePlayerTarget(ex)
               )) {
                  e.invulnerableTime = 0;
                  e.hurt(player.damageSources().mobAttack(player), damageBase * 5.0F);
                  e.invulnerableTime = 0;
               }
            }
         }
      );
   }
}
