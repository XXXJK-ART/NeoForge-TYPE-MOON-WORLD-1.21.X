package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHassanSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillUtils;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillLayout;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.concealment.ConcealmentRank;
import net.xxxjk.TYPE_MOON_WORLD.servant.concealment.ServantConcealment;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.heracles.GodHandService;

@GameTestHolder("typemoonworld_hassan")
@PrefixGameTestTemplate(false)
public final class CursedArmHassanGameTests {
   private static ServantEntity servant(GameTestHelper h, String id, int x) {
      var entity = BuiltinServantEntityFactory.create(h.getLevel(), ResourceLocation.fromNamespaceAndPath("typemoonworld", id));
      entity.setPos(h.absolutePos(new BlockPos(x, 4, 4)).getCenter());
      entity.setNoAi(true); entity.setNoGravity(true);
      h.getLevel().addFreshEntity(entity);
      entity.setHealth(entity.getMaxHealth());
      return entity;
   }

   private static <T extends Mob> T mob(GameTestHelper h, EntityType<T> type, int x) {
      T entity = type.create(h.getLevel());
      entity.setPos(h.absolutePos(new BlockPos(x, 4, 6)).getCenter());
      entity.setNoAi(true); entity.setNoGravity(true);
      h.getLevel().addFreshEntity(entity);
      return entity;
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void extractionDefersDeathAndCopiesCannotKillTwice(GameTestHelper h) {
      var caster = (CursedArmHassanEntity) servant(h, "cursed_arm_hassan", 2);
      var cow = mob(h, EntityType.COW, 4);
      float health = cow.getHealth();
      h.assertTrue(HassanZabaniyaService.extract(caster, cow), "organic extraction failed");
      h.assertTrue(cow.isAlive() && cow.getHealth() == health, "extraction killed or damaged the target");
      var heart = caster.getOffhandItem(); var duplicate = heart.copy();
      h.assertTrue(heart.is(ModItems.ZABANIYA_HEART.get()), "heart was not held in the left hand");
      h.assertTrue(HassanZabaniyaService.data(heart).getLong(HassanZabaniyaService.EXPIRES)
         == h.getLevel().getServer().overworld().getGameTime() + 1200, "lifetime is not 60 seconds");
      h.assertTrue(!HassanZabaniyaService.canExtract(cow), "second extraction replaced an active link");
      h.assertTrue(HassanZabaniyaService.crush(caster, heart, InteractionHand.OFF_HAND) && !cow.isAlive(), "crush did not kill");
      h.assertTrue(!HassanZabaniyaService.crush(caster, duplicate, InteractionHand.OFF_HAND), "copied heart resolved twice");
      caster.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void transferredOffhandHeartCanBeCrushedByAnotherPlayer(GameTestHelper h) {
      var caster = (CursedArmHassanEntity) servant(h, "cursed_arm_hassan", 2);
      var cow = mob(h, EntityType.COW, 4);
      var holder = h.makeMockServerPlayerInLevel();
      h.assertTrue(HassanZabaniyaService.extract(caster, cow), "heart extraction failed");
      var heart = caster.getOffhandItem();
      h.assertTrue(caster.getMainHandItem().isEmpty() && heart.is(ModItems.ZABANIYA_HEART.get()), "core was not placed in left hand");
      caster.setItemInHand(InteractionHand.OFF_HAND, net.minecraft.world.item.ItemStack.EMPTY);
      holder.setItemInHand(InteractionHand.OFF_HAND, heart);
      ModItems.ZABANIYA_HEART.get().use(h.getLevel(), holder, InteractionHand.OFF_HAND);
      h.assertTrue(!cow.isAlive() && holder.getOffhandItem().isEmpty(), "a recipient could not crush the transferred heart");
      caster.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void quickStabHitsNearbyTargetWithoutMana(GameTestHelper h) {
      var caster = (CursedArmHassanEntity) servant(h, "cursed_arm_hassan", 2);
      var cow = mob(h, EntityType.COW, 4);
      cow.setPos(caster.getX() + 1.8, caster.getY(), caster.getZ());
      caster.setCurrentMp(0.0);
      float health = cow.getHealth();
      CursedArmHassanCombatHelper.tick(caster, ServantAiContext.forEntity(caster, cow, h.getLevel().getGameTime()));
      h.assertTrue(cow.getHealth() < health, "slot 1 quick stab did not hit its close target");
      caster.discard(); cow.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void singleDaggerSkillSpawnsTenDamageProjectile(GameTestHelper h) {
      var player = h.makeMockServerPlayerInLevel();
      BlockPos feet = h.absolutePos(new BlockPos(3, 4, 4));
      player.teleportTo(feet.getX() + 0.5, feet.getY() + 50.0, feet.getZ() + 0.5);
      player.setNoGravity(true);
      player.setYRot(0.0F);
      player.setXRot(0.0F);
      var cow = mob(h, EntityType.COW, 4);
      cow.setPos(player.getX(), player.getY(), player.getZ() + 4.0);
      player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
         cow.position().add(0.0, cow.getBbHeight() * 0.5, 0.0));
      h.assertTrue(ServantCardSkillUtils.findLookTarget(player, 9.0, 1.8) == cow, "single dagger target was not selected");
      h.assertTrue(player.hasLineOfSight(cow), "single dagger target is blocked");
      player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.DIRK_SMALL_KNIFE.get()));
      h.assertTrue(ServantCardHassanSkills.performKnifeFeint(player), "single dagger throw failed");
      var projectiles = h.getLevel().getEntitiesOfClass(net.xxxjk.TYPE_MOON_WORLD.entity.DirkProjectileEntity.class,
         player.getBoundingBox().inflate(20.0), projectile -> projectile.getOwner() == player);
      h.assertTrue(projectiles.size() == 1 && projectiles.getFirst().getDamage() == 10.0F, "single dagger was not a ten-damage projectile");
      h.assertTrue(player.getOffhandItem().getCount() == 1 && player.getOffhandItem().getDamageValue() == 1,
         "single dagger did not consume one durability point");
      projectiles.forEach(net.minecraft.world.entity.Entity::discard);
      player.discard(); cow.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void tripleDaggerSkillConsumesThreeDurabilityPoints(GameTestHelper h) {
      var player = h.makeMockServerPlayerInLevel();
      BlockPos feet = h.absolutePos(new BlockPos(3, 4, 4));
      player.teleportTo(feet.getX() + 0.5, feet.getY() + 50.0, feet.getZ() + 0.5);
      player.setNoGravity(true);
      player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.DIRK_SMALL_KNIFE.get()));
      ServantCardHassanSkills.performDirkThrow(player);
      var projectiles = h.getLevel().getEntitiesOfClass(net.xxxjk.TYPE_MOON_WORLD.entity.DirkProjectileEntity.class,
         player.getBoundingBox().inflate(20.0), projectile -> projectile.getOwner() == player);
      h.assertTrue(projectiles.size() == 3, "triple dagger skill did not spawn three projectiles");
      h.assertTrue(player.getOffhandItem().getCount() == 1 && player.getOffhandItem().getDamageValue() == 3,
         "triple dagger skill did not consume three durability points");
      projectiles.forEach(net.minecraft.world.entity.Entity::discard);
      player.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void rightHandItemsAreMovedOrStoredInsteadOfDeleted(GameTestHelper h) {
      var player = h.makeMockServerPlayerInLevel();
      h.assertTrue(ServantCardTransformManager.transform(player, "cursed_arm_hassan"), "transform failed");
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
      player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
      ServantCardHassanSkills.tick(player, vars);
      h.assertTrue(player.getMainHandItem().isEmpty() && player.getOffhandItem().is(Items.IRON_SWORD), "main-hand item was not moved to offhand");
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
      player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE_SWORD));
      ServantCardHassanSkills.tick(player, vars);
      h.assertTrue(player.getMainHandItem().isEmpty() && player.getOffhandItem().is(Items.STONE_SWORD)
         && player.getInventory().countItem(Items.DIAMOND_SWORD) == 1, "occupied offhand caused main-hand item loss");
      ServantCardTransformManager.release(player, false);
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void anatomicalFiltersMechanicalCoreAndEnkiduClay(GameTestHelper h) {
      var caster = (CursedArmHassanEntity) servant(h, "cursed_arm_hassan", 2);
      var skeleton = mob(h, EntityType.SKELETON, 4);
      var golem = mob(h, EntityType.IRON_GOLEM, 6);
      h.assertTrue(HassanZabaniyaService.coreOf(skeleton) == HassanZabaniyaService.Core.NONE, "skeleton has a heart");
      float health = skeleton.getHealth();
      HassanZabaniyaService.resolve(caster, skeleton, 55, true);
      h.assertTrue(skeleton.getHealth() == health, "heartless target received fallback damage");
      h.assertTrue(HassanZabaniyaService.extract(caster, golem) && caster.getOffhandItem().is(ModItems.ZABANIYA_MECHANICAL_CORE.get()), "golem core missing");
      HassanZabaniyaService.crush(caster, caster.getOffhandItem(), InteractionHand.OFF_HAND);
      h.assertTrue(!golem.isAlive(), "mechanical core did not kill golem");
      var enkidu = servant(h, "enkidu", 8);
      h.assertTrue(HassanZabaniyaService.extract(caster, enkidu), "clay extraction failed");
      h.assertTrue(caster.getOffhandItem().is(ModItems.ZABANIYA_DIVINE_CLAY.get()), "Enkidu yielded a heart");
      h.assertTrue(!HassanZabaniyaService.crush(caster, caster.getOffhandItem(), InteractionHand.OFF_HAND) && enkidu.isAlive(), "clay was lethal");
      caster.discard(); skeleton.discard(); enkidu.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void heraclesLosesExactlyOneLifeAndCannotBeExtractedAgain(GameTestHelper h) {
      var caster = (CursedArmHassanEntity) servant(h, "cursed_arm_hassan", 2);
      var heracles = servant(h, "heracles", 5);
      GodHandService.initialize(heracles);
      int lives = GodHandService.read(heracles).lives();
      h.assertTrue(HassanZabaniyaService.extract(caster, heracles), "Heracles first heart rejected");
      var heart = caster.getOffhandItem(); var duplicate = heart.copy();
      h.assertTrue(GodHandService.read(heracles).lives() == lives, "extraction consumed a life");
      HassanZabaniyaService.crush(caster, heart, InteractionHand.OFF_HAND);
      h.assertTrue(heracles.isAlive() && GodHandService.read(heracles).lives() == lives - 1, "crush did not cost exactly one life");
      h.assertTrue(!HassanZabaniyaService.canExtract(heracles), "Heracles heart extracted twice");
      HassanZabaniyaService.crush(caster, duplicate, InteractionHand.OFF_HAND);
      h.assertTrue(GodHandService.read(heracles).lives() == lives - 1, "duplicate core cost another life");
      caster.discard(); heracles.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void expiredCoreCannotKillAndLinkCanBeReplaced(GameTestHelper h) {
      var caster = (CursedArmHassanEntity) servant(h, "cursed_arm_hassan", 2);
      var cow = mob(h, EntityType.COW, 4);
      HassanZabaniyaService.extract(caster, cow);
      var heart = caster.getOffhandItem(); var data = HassanZabaniyaService.data(heart);
      data.putLong(HassanZabaniyaService.EXPIRES, 0); heart.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
      cow.getPersistentData().putLong(HassanZabaniyaService.EXPIRES, 0);
      h.assertTrue(!HassanZabaniyaService.crush(caster, heart, InteractionHand.OFF_HAND) && cow.isAlive() && heart.isEmpty(), "expired core killed");
      h.assertTrue(HassanZabaniyaService.canExtract(cow), "expired link permanently blocked extraction");
      caster.discard(); cow.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 90)
   public static void npcRetreatsAndCrushesAfterOneToThreeSeconds(GameTestHelper h) {
      var caster = (CursedArmHassanEntity) servant(h, "cursed_arm_hassan", 2);
      var cow = mob(h, EntityType.COW, 5);
      HassanZabaniyaService.extract(caster, cow);
      long now = h.getLevel().getServer().overworld().getGameTime();
      long delay = caster.getPersistentData().getLong("HassanZabaniyaCrushAt") - now;
      h.assertTrue(delay >= 20 && delay <= 60, "NPC crush delay outside 1-3 seconds");
      h.runAfterDelay(15, () -> h.assertTrue(cow.isAlive(), "NPC crushed too early"));
      h.runAfterDelay(65, () -> {
         h.assertTrue(!cow.isAlive() && caster.getOffhandItem().isEmpty(), "NPC did not crush after delay");
         caster.discard(); h.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void ordinaryThrowUsesTheRightClickProjectileWithoutMana(GameTestHelper h) {
      var caster = (CursedArmHassanEntity) servant(h, "cursed_arm_hassan", 2);
      var cow = mob(h, EntityType.COW, 14);
      caster.setCurrentMp(0);
      caster.getPersistentData().putInt(CursedArmHassanEntity.TAG_DIRK_AMMO, 1);
      CursedArmHassanCombatHelper.tick(caster, ServantAiContext.forEntity(caster, cow, h.getLevel().getGameTime()));
      var projectiles = h.getLevel().getEntitiesOfClass(net.xxxjk.TYPE_MOON_WORLD.entity.DirkProjectileEntity.class,
         caster.getBoundingBox().inflate(2), e -> e.getOwner() == caster);
      h.assertTrue(projectiles.size() == 1 && projectiles.getFirst().getDamage() == 10, "ordinary 10-damage dirk was not thrown");
      h.assertTrue(Math.abs(projectiles.getFirst().getDeltaMovement().length() - 1.9) < 0.001 && caster.getCurrentMp() == 0,
         "ordinary throw differs from right click or spent mana");
      h.assertTrue(caster.getOffhandItem().getCount() == 1 && caster.getOffhandItem().getDamageValue() == 0
         && caster.getPersistentData().getInt(CursedArmHassanEntity.TAG_DIRK_AMMO) == 16,
         "NPC did not immediately replenish its exhausted dagger without dropping it");
      projectiles.forEach(projectile -> projectile.discard()); caster.discard(); cow.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void actualServantKillsGrantPassiveEvolutionOnly(GameTestHelper h) {
      var caster = (CursedArmHassanEntity) servant(h, "cursed_arm_hassan", 2);
      caster.setHealth(1);
      CursedArmHassanCombatHelper.tick(caster, ServantAiContext.forEntity(caster, null, h.getLevel().getGameTime()));
      h.assertTrue(caster.getSelfModificationLevel() == 0, "low health granted evolution");
      var cow = mob(h, EntityType.COW, 4);
      HassanZabaniyaService.extract(caster, cow); HassanZabaniyaService.crush(caster, caster.getOffhandItem(), InteractionHand.OFF_HAND);
      h.runAfterDelay(2, () -> {
         h.assertTrue(caster.getSelfModificationLevel() == 0, "ordinary kill granted evolution");
         for (int i = 0; i < 4; i++) {
            var victim = servant(h, "medea", 6 + i);
            h.assertTrue(HassanZabaniyaService.extract(caster, victim), "servant extraction failed");
            HassanZabaniyaService.crush(caster, caster.getOffhandItem(), InteractionHand.OFF_HAND);
         }
      });
      h.runAfterDelay(5, () -> {
         h.assertTrue(caster.getSelfModificationLevel() == 3, "servant kills did not grant capped stages");
         h.assertTrue("hassan_quick_stab".equals(ServantCardSkillLayout.actionFor("cursed_arm_hassan", 1, false).effectId()), "slot 1 quick stab is missing");
         caster.discard(); h.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void cardAndNpcShareToggleRankAndEvolutionAttributes(GameTestHelper h) {
      var player = h.makeMockServerPlayerInLevel();
      h.assertTrue(ServantCardTransformManager.transform(player, "cursed_arm_hassan"), "transform failed");
      var caster = (CursedArmHassanEntity) servant(h, "cursed_arm_hassan", 3);
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servantCardState().setMana(100);
      h.assertTrue(ServantCardTransformManager.triggerAction(player, 0), "toggle on failed");
      h.assertTrue(HassanPresenceService.enabled(player) && ServantConcealment.rankOf(player) == ConcealmentRank.A_PLUS
         && ConcealmentRank.A_PLUS.maxParticles() == 1, "A+ shared profile missing");
      h.assertTrue(ServantCardTransformManager.triggerAction(player, 0) && !HassanPresenceService.enabled(player), "toggle off failed");
      h.assertTrue(vars.servantCardState().mana() == 82, "toggle off charged mana");
      h.assertTrue(HassanPresenceService.set(caster, true), "NPC toggle on failed");
      ServantConcealment.breakConcealment(caster);
      h.assertTrue(!HassanPresenceService.enabled(caster) && !HassanPresenceService.set(caster, true), "break cooldown bypassed");
      double playerHealth = player.getMaxHealth(), npcHealth = caster.getMaxHealth();
      double playerAttack = player.getAttributeValue(Attributes.ATTACK_DAMAGE), npcAttack = caster.getAttributeValue(Attributes.ATTACK_DAMAGE);
      caster.evolveAfterServantKill(); ServantCardHassanSkills.evolveAfterServantKill(player);
      h.assertTrue(Math.abs(player.getMaxHealth() - playerHealth - (caster.getMaxHealth() - npcHealth)) < 0.01, "evolution health differs");
      h.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_DAMAGE) - playerAttack
         - (caster.getAttributeValue(Attributes.ATTACK_DAMAGE) - npcAttack)) < 0.01, "evolution attack differs");
      caster.discard(); ServantCardTransformManager.release(player, false); h.succeed();
   }
}
