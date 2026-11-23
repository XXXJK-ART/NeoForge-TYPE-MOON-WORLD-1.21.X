package net.xxxjk.TYPE_MOON_WORLD.gametest;

import com.captain.servant.compat.superbwarfare.SuperbWarfareLoader;
import com.captain.servant.lancelot.KnightArmoryEntry;
import com.captain.servant.lancelot.KnightArmoryService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LancelotBerserkerEntity;

/** Runs in both optional-mod configurations without linking any Warfare class when absent. */
@GameTestHolder("typemoonworld_lancelot_armory")
@PrefixGameTestTemplate(false)
public final class LancelotArmoryGameTests {
    @GameTest(template = "ancient_temple", timeoutTicks = 30)
    public static void menuAndWeaponCostsRespectOptionalIntegration(GameTestHelper helper) {
        var player = player(helper);
        int mask = KnightArmoryService.unlockedMask(player);
        helper.assertTrue(mask == (SuperbWarfareLoader.isAvailable() ? 63 : 1), "incorrect available summons");
        var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        double before = vars.servant_card_mana;
        var entry = SuperbWarfareLoader.isAvailable() ? KnightArmoryEntry.MINIGUN : KnightArmoryEntry.IRON_ROD;
        player.setHealth(player.getMaxHealth());
        KnightArmoryService.select(player, entry.networkId());
        helper.assertTrue(Math.abs(vars.servant_card_mana - (before - entry.manaCost())) < 0.01, "wrong summon cost");
        helper.assertTrue(player.getInventory().items.stream().anyMatch(stack -> KnightArmoryService.entryFor(stack) == entry), "weapon missing");
        if (entry == KnightArmoryEntry.MINIGUN) {
            helper.assertTrue(KnightArmoryService.cooldownUntil(player, entry) == helper.getLevel().getGameTime() + 3600,
                    "player minigun did not receive a three-minute cooldown");
        }
        double after = vars.servant_card_mana;
        KnightArmoryService.select(player, entry.networkId());
        KnightArmoryService.select(player, -1);
        helper.assertTrue(vars.servant_card_mana == after, "cooldown/invalid selection consumed MP");
        if (!SuperbWarfareLoader.isAvailable()) {
            KnightArmoryService.select(player, KnightArmoryEntry.A10.networkId());
            helper.assertTrue(vars.servant_card_mana == after && !player.isPassenger(), "absent integration enabled aircraft");
        }
        cleanup(player);
        helper.succeed();
    }

    @GameTest(template = "ancient_temple", timeoutTicks = 30)
    public static void aircraftExpiryDetonatesAndDoesNotUseSkillSeven(GameTestHelper helper) throws Exception {
        if (!SuperbWarfareLoader.isAvailable()) { helper.succeed(); return; }
        var player = player(helper);
        var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        int skillSeven = ServantCardTransformManager.getSkillCooldown(vars, 7);
        helper.assertTrue(KnightArmoryService.summonA10Skill(player), "aircraft failed to summon");
        Entity aircraft = player.getVehicle();
        helper.assertTrue(aircraft != null, "player did not mount aircraft");
        helper.assertTrue(aircraft.getPersistentData().getLong("FateSupportKnightVehicleExpires") == helper.getLevel().getGameTime() + 300,
                "player aircraft must crash after 15 seconds");
        helper.assertTrue(KnightArmoryService.cooldownUntil(player, KnightArmoryEntry.A10)
                == helper.getLevel().getGameTime() + 3600, "player aircraft did not receive a three-minute cooldown");
        helper.assertTrue(Math.abs(vars.servant_card_mana - 550) < 0.01, "wrong aircraft mana");
        helper.assertTrue(ServantCardTransformManager.getSkillCooldown(vars, 7) == skillSeven, "aircraft stole skill 7 cooldown");
        helper.assertTrue(!KnightArmoryService.summonA10Skill(player), "duplicate aircraft accepted");
        var victim = EntityType.ZOMBIE.create(helper.getLevel());
        victim.setPos(aircraft.getX() + 2, aircraft.getY(), aircraft.getZ());
        victim.setNoAi(true); victim.setNoGravity(true);
        victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); victim.setHealth(1000);
        helper.getLevel().addFreshEntity(victim);
        aircraft.getPersistentData().putLong("FateSupportKnightVehicleExpires", helper.getLevel().getGameTime());
        tickVehicle(helper, aircraft);
        helper.assertTrue(aircraft.isRemoved() && aircraft.getPersistentData().getBoolean("FateSupportKnightVehicleDetonated"), "expiry did not explode");
        helper.assertTrue(!player.isPassenger() && player.isAlive(), "pilot was not safely ejected");
        helper.assertTrue(!SuperbWarfareLoader.hasActiveSummonedA10(player), "aircraft ownership was not cleared");
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(victim.getHealth() < 1000, "aircraft produced no actual explosion damage");
            victim.discard(); cleanup(player); helper.succeed();
        });
    }

    @GameTest(template = "ancient_temple", timeoutTicks = 30)
    public static void aircraftCollisionAndInvalidOwnerCleanUp(GameTestHelper helper) throws Exception {
        if (!SuperbWarfareLoader.isAvailable()) { helper.succeed(); return; }
        var player = player(helper);
        helper.assertTrue(KnightArmoryService.summonA10Skill(player), "aircraft failed to summon");
        Entity aircraft = player.getVehicle();
        aircraft.tickCount = 20;
        aircraft.horizontalCollision = true;
        tickVehicle(helper, aircraft);
        helper.assertTrue(aircraft.isRemoved() && aircraft.getPersistentData().getBoolean("FateSupportKnightVehicleDetonated"), "impact did not explode");
        player.getPersistentData().remove("FateSupportKnightArmoryCooldown_a10");
        helper.assertTrue(KnightArmoryService.summonA10Skill(player), "second test aircraft failed");
        aircraft = player.getVehicle();
        player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servant_card_transformed = false;
        tickVehicle(helper, aircraft);
        helper.assertTrue(aircraft.isRemoved() && !aircraft.getPersistentData().getBoolean("FateSupportKnightVehicleDetonated"), "invalid owner left an active aircraft");
        cleanup(player);
        helper.succeed();
    }

    @GameTest(template = "ancient_temple", timeoutTicks = 30)
    public static void obstructedLaunchRefundsManaAndCooldown(GameTestHelper helper) {
        if (!SuperbWarfareLoader.isAvailable()) { helper.succeed(); return; }
        var player = player(helper);
        BlockPos obstacle = player.blockPosition().above(15);
        helper.getLevel().setBlockAndUpdate(obstacle, Blocks.STONE.defaultBlockState());
        helper.assertTrue(!KnightArmoryService.summonA10Skill(player), "aircraft spawned in a solid block");
        var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        helper.assertTrue(vars.servant_card_mana == 1000 && !player.isPassenger(), "failed launch consumed MP");
        helper.assertTrue(KnightArmoryService.cooldownUntil(player, KnightArmoryEntry.A10) == 0, "failed launch started cooldown");
        vars.servant_card_mana = 100;
        net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardUnlimitedMode.setEnabled(player, true);
        helper.assertTrue(!KnightArmoryService.summonA10Skill(player), "unlimited mode bypassed launch collision");
        helper.assertTrue(vars.servant_card_mana == 100, "failed zero-cost launch created free mana");
        helper.getLevel().setBlockAndUpdate(obstacle, Blocks.AIR.defaultBlockState());
        cleanup(player);
        helper.succeed();
    }

    @GameTest(template = "ancient_temple", timeoutTicks = 80)
    public static void npcSummonsAndFiresMinigunWithSharedCost(GameTestHelper helper) throws Exception {
        if (!SuperbWarfareLoader.isAvailable()) { helper.succeed(); return; }
        var level = helper.getLevel();
        var npc = ModEntities.LANCELOT_BERSERKER.get().create(level);
        var target = EntityType.ZOMBIE.create(level);
        helper.assertTrue(npc != null && target != null, "test entities missing");
        var pos = helper.absolutePos(new BlockPos(2, 4, 2));
        npc.setPos(pos.getX(), 200, pos.getZ());
        npc.setNoGravity(true); npc.tickCount = 100;
        // A target to the side catches shooting along the default +Z look
        // direction instead of actually aiming the ordinary gun projectile.
        target.setPos(pos.getX() - 10, 200, pos.getZ() + 2);
        target.setNoAi(true); target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); target.setHealth(1000);
        level.addFreshEntity(npc); level.addFreshEntity(target);
        npc.setHealth(npc.getMaxHealth());
        npc.setTarget(target); npc.setCurrentMp(100);
        callIntegration("tickLancelotNpc", new Class<?>[]{LancelotBerserkerEntity.class, ServerLevel.class}, npc, level);
        helper.assertTrue(npc.getCurrentMp() == 100
                && KnightArmoryService.entryFor(npc.getMainHandItem()) != KnightArmoryEntry.MINIGUN,
                "NPC used its minigun above half health");
        npc.setHealth(npc.getMaxHealth() * 0.5F);
        java.util.List<net.minecraft.world.phys.Vec3> directions = new java.util.ArrayList<>();
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> shotListener = event -> {
            if (event.getEntity() instanceof net.minecraft.world.entity.projectile.Projectile projectile
                    && projectile.getOwner() == npc) directions.add(projectile.getDeltaMovement().normalize());
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                net.neoforged.bus.api.EventPriority.LOWEST, false,
                net.neoforged.neoforge.event.entity.EntityJoinLevelEvent.class, shotListener);
        try {
            callIntegration("tickLancelotNpc", new Class<?>[]{LancelotBerserkerEntity.class, ServerLevel.class}, npc, level);
        } finally {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(shotListener);
        }
        var aim = target.getEyePosition().subtract(npc.getEyePosition()).normalize();
        helper.assertTrue(!directions.isEmpty() && directions.stream().allMatch(direction -> direction.dot(aim) > 0.99),
                "NPC minigun fired along its old facing instead of aiming at the side target");
        helper.assertTrue(KnightArmoryService.entryFor(npc.getMainHandItem()) == KnightArmoryEntry.MINIGUN, "NPC failed to select minigun");
        helper.assertTrue(Math.abs(npc.getCurrentMp() - 20) < 0.01, "NPC used different minigun cost");
        helper.assertTrue(KnightArmoryService.cooldownUntil(npc, KnightArmoryEntry.MINIGUN) == level.getGameTime() + 3600, "NPC used different cooldown");
        helper.runAfterDelay(35, () -> {
            helper.assertTrue(target.getHealth() < 1000, "NPC minigun did not damage its target");
            npc.discard(); target.discard(); helper.succeed();
        });
    }

    @GameTest(template = "ancient_temple", timeoutTicks = 160)
    public static void npcAircraftRamsWithoutFiringAndUsesFifteenSecondLifetime(GameTestHelper helper) throws Exception {
        if (!SuperbWarfareLoader.isAvailable()) { helper.succeed(); return; }
        var level = helper.getLevel();
        var npc = ModEntities.LANCELOT_BERSERKER.get().create(level);
        var target = EntityType.ZOMBIE.create(level);
        var pos = helper.absolutePos(new BlockPos(2, 4, 2));
        npc.setPos(pos.getX(), 200, pos.getZ()); npc.setNoAi(true); npc.setNoGravity(true);
        target.setPos(pos.getX(), 200, pos.getZ() + 80); target.setNoAi(true); target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); target.setHealth(1000);
        level.addFreshEntity(npc); level.addFreshEntity(target); npc.setTarget(target); npc.setCurrentMp(600);
        npc.setHealth(npc.getMaxHealth() * 0.5F);
        java.util.concurrent.atomic.AtomicInteger shots = new java.util.concurrent.atomic.AtomicInteger();
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> listener = event -> {
            var data = event.getEntity().getPersistentData();
            if (data.hasUUID("FateSupportKnightProjectileVehicle") && data.hasUUID("FateSupportKnightProjectileOwner")
                    && data.getUUID("FateSupportKnightProjectileOwner").equals(npc.getUUID())) shots.incrementAndGet();
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                net.neoforged.bus.api.EventPriority.LOWEST, false,
                net.neoforged.neoforge.event.entity.EntityJoinLevelEvent.class, listener);
        helper.assertTrue(SuperbWarfareLoader.tryDeployNpcA10(npc, target), "NPC aircraft deploy failed");
        Entity aircraft = npc.getVehicle();
        helper.assertTrue(Math.abs(npc.getCurrentMp() - 150) < 0.01, "NPC aircraft MP differs");
        helper.assertTrue(aircraft.getPersistentData().getLong("FateSupportKnightVehicleExpires") == level.getGameTime() + 300,
                "NPC aircraft duration differs");
        helper.assertTrue(KnightArmoryService.cooldownUntil(npc, KnightArmoryEntry.A10) == level.getGameTime() + 3600,
                "NPC aircraft cooldown differs");
        // Exercise both the ordinary trigger and named-weapon firing paths,
        // rather than only verifying that our AI chose not to call them.
        Class<?> aircraftType = Class.forName("com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity");
        helper.assertTrue(!(boolean) aircraftType.getMethod("canShoot", net.minecraft.world.entity.LivingEntity.class)
                .invoke(aircraft, npc), "ramming aircraft accepted a fire request");
        aircraftType.getMethod("vehicleShoot", net.minecraft.world.entity.LivingEntity.class, java.util.UUID.class,
                net.minecraft.world.phys.Vec3.class).invoke(aircraft, npc, target.getUUID(), target.getEyePosition());
        aircraftType.getMethod("vehicleShoot", net.minecraft.world.entity.LivingEntity.class, String.class,
                net.minecraft.world.phys.Vec3.class).invoke(aircraft, npc, "Cannon", target.getEyePosition());
        aircraftType.getMethod("vehicleShoot", net.minecraft.world.entity.LivingEntity.class, String.class, java.util.UUID.class,
                net.minecraft.world.phys.Vec3.class).invoke(aircraft, npc, "Cannon", target.getUUID(), target.getEyePosition());
        tickVehicle(helper, aircraft);
        var toward = target.getBoundingBox().getCenter().subtract(aircraft.position()).normalize();
        helper.assertTrue(aircraft.getDeltaMovement().normalize().dot(toward) > 0.95, "NPC aircraft orbited instead of ramming");
        helper.runAfterDelay(35, () -> {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);
            helper.assertTrue(shots.get() == 0, "ramming aircraft fired a weapon");
            helper.assertTrue(!aircraft.isRemoved(), "aircraft crashed before reaching its target");
        });
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(aircraft.isRemoved() && aircraft.getPersistentData().getBoolean("FateSupportKnightVehicleDetonated"),
                    "NPC aircraft did not complete its ramming attack");
            helper.assertTrue(target.getHealth() < 1000, "NPC ramming attack did not damage its target");
            npc.discard(); target.discard(); helper.succeed();
        });
    }

    @GameTest(template = "ancient_temple", timeoutTicks = 30)
    public static void aircraftDoesNotRamItsOwnDamageProxy(GameTestHelper helper) throws Exception {
        if (!SuperbWarfareLoader.isAvailable()) { helper.succeed(); return; }
        var player = player(helper);
        helper.assertTrue(KnightArmoryService.summonA10Skill(player), "aircraft failed to summon");
        Entity aircraft = player.getVehicle();
        Class<?> vehicleType = Class.forName("com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity");
        callIntegration("maintainDamageProxy", new Class<?>[]{vehicleType, ServerLevel.class}, aircraft, helper.getLevel());
        aircraft.tickCount = 20;
        tickVehicle(helper, aircraft);
        helper.assertTrue(!aircraft.isRemoved() && player.isPassenger(), "aircraft exploded against its own damage proxy");
        helper.assertTrue(!(boolean) vehicleType.getMethod("canShoot", net.minecraft.world.entity.LivingEntity.class)
                .invoke(aircraft, player), "player could fire a ramming aircraft");
        aircraft.discard(); cleanup(player); helper.succeed();
    }

    @GameTest(template = "ancient_temple", timeoutTicks = 30)
    public static void aircraftRammingAnEnemyDetonates(GameTestHelper helper) throws Exception {
        if (!SuperbWarfareLoader.isAvailable()) { helper.succeed(); return; }
        var player = player(helper);
        helper.assertTrue(KnightArmoryService.summonA10Skill(player), "aircraft failed to summon");
        Entity aircraft = player.getVehicle();
        var target = EntityType.ZOMBIE.create(helper.getLevel());
        target.setPos(aircraft.position()); target.setNoAi(true); target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); target.setHealth(1000);
        helper.getLevel().addFreshEntity(target);
        aircraft.tickCount = 20;
        tickVehicle(helper, aircraft);
        helper.assertTrue(aircraft.isRemoved() && aircraft.getPersistentData().getBoolean("FateSupportKnightVehicleDetonated"),
                "aircraft passed through its enemy instead of exploding on impact");
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(target.getHealth() < 1000, "ramming caused no explosion damage");
            target.discard(); cleanup(player); helper.succeed();
        });
    }

    private static ServerPlayer player(GameTestHelper helper) {
        // A GameTest mock has no negotiated mod channels. Avoid the login/data
        // sync path, while keeping a real ServerPlayer that can ride vehicles.
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "armory-test");
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), profile, cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(
                helper.getLevel().getServer(), connection, player, cookie) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { }
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,
                                       net.minecraft.network.PacketSendListener listener) { }
        };
        helper.getLevel().addNewPlayer(player);
        var pos = helper.absolutePos(new BlockPos(2, 4, 2));
        player.setPos(pos.getX(), 200, pos.getZ());
        helper.getLevel().getChunkAt(player.blockPosition());
        helper.assertTrue(ServantCardTransformManager.transform(player, "lancelot_berserker"), "card transform failed");
        var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        vars.servant_card_max_mana = 1000;
        vars.servant_card_mana = 1000;
        return player;
    }

    private static void cleanup(ServerPlayer player) {
        player.discard();
    }

    private static void tickVehicle(GameTestHelper helper, Entity vehicle) throws Exception {
        Class<?> vehicleType = Class.forName("com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity");
        callIntegration("tickVehicle", new Class<?>[]{vehicleType, ServerLevel.class}, vehicle, helper.getLevel());
    }

    private static void callIntegration(String method, Class<?>[] types, Object... args) throws Exception {
        Class<?> integration = Class.forName("com.captain.servant.compat.superbwarfare.SuperbWarfareIntegration");
        var operation = integration.getDeclaredMethod(method, types);
        operation.setAccessible(true);
        operation.invoke(integration.getConstructor().newInstance(), args);
    }
}
