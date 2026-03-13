package net.xxxjk.TYPE_MOON_WORLD.event;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
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
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Added;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.CyanWindFieldEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MerlinEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RyougiShikiEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.TempleStoneSwordAxeItem;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.MagicJewelMachineGun;
import net.xxxjk.TYPE_MOON_WORLD.magic.nordic.MagicGander;
import net.xxxjk.TYPE_MOON_WORLD.magic.nordic.MagicGandrMachineGun;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.utils.MerlinWorldEventLimiter;

@EventBusSubscriber(
   modid = "typemoonworld"
)
public class CommonEvents {
   @SubscribeEvent
   public static void onEntityJoin(EntityJoinLevelEvent event) {
      if (!event.getLevel().isClientSide) {
         if (event.getEntity() instanceof Monster monster) {
            try {
               monster.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(monster, RyougiShikiEntity.class, true));
            } catch (Exception var3) {
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLevelTick(Post event) {
      if (!event.getLevel().isClientSide) {
         if (event.getLevel() instanceof ServerLevel serverLevel) {
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
         }

         if (player.isSpectator()) {
            if (!player.getActiveEffects().isEmpty()) {
               player.removeAllEffects();
            }
         } else {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.getItem() instanceof TempleStoneSwordAxeItem) {
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
            Entity directEntity = event.getSource().getDirectEntity();
            if (directEntity instanceof Projectile projectile && projectile.getOwner() == event.getEntity()) {
               event.setCanceled(true);
            } else if (directEntity instanceof CyanWindFieldEntity windField && windField.getOwner() == event.getEntity()) {
               event.setCanceled(true);
            } else {
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
                     if (player.level() instanceof ServerLevel var22) {
                        ;
                     }

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

               if (event.getEntity() instanceof Player damagedPlayer && damagedPlayer.level() instanceof ServerLevel serverLevelx) {
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
                                    helper.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
                                 }

                                 helper.setHealth(200.0F);
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
   }

   @SubscribeEvent
   public static void onLivingDeath(LivingDeathEvent event) {
      if (!event.getEntity().level().isClientSide) {
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
      }
   }

   @SubscribeEvent
   public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
      Entity e = event.getEntity();
      if (e instanceof RyougiShikiEntity) {
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

   private static void speakNearby(ServerLevel level, Entity center, String key, double radius) {
      for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, center.getBoundingBox().inflate(radius))) {
         p.displayClientMessage(Component.translatable(key), false);
      }
   }

   @SubscribeEvent
   public static void onEffectAdded(Added event) {
      if (EntityUtils.isSpectatorPlayer(event.getEntity())) {
         event.getEntity().removeAllEffects();
      } else {
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
