package com.captain.servant.spiritual;

import com.captain.servant.compat.superbwarfare.SuperbWarfareLoader;
import com.captain.servant.item.MessageStoneItem;
import com.captain.servant.item.ModItems;
import com.captain.servant.lancelot.KnightArmoryEntry;
import com.captain.servant.lancelot.KnightArmoryService;
import com.captain.servant.voice.VoiceCommunicationService;
import com.captain.servant.emiya.EmiyaSupportService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardEmiyaSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatDefenseService;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;

/** Exercises real registries, payloads and injected main-mod behavior on a dedicated server. */
@GameTestHolder("typemoonworld_fatesupport")
@PrefixGameTestTemplate(false)
public final class FateSupportGameTests {
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void optionalBridgesMatchInstalledMods(GameTestHelper h) {
        h.assertTrue(SuperbWarfareLoader.isAvailable() == ModList.get().isLoaded("superbwarfare"), "vehicle bridge failed to load or loaded without its mod");
        h.assertTrue(VoiceCommunicationService.isAvailable() == ModList.get().isLoaded("voicechat"), "voice availability mismatch");
        h.assertTrue(ModItems.MESSAGE_STONE.get() instanceof MessageStoneItem, "message stone was not registered");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void stoneUseRequiresVoicechatAndBindsMutually(GameTestHelper h) {
        ServerPlayer p = servant(h, "emiya_archer");
        ItemStack left = new ItemStack(ModItems.MESSAGE_STONE.get());
        ItemStack right = new ItemStack(ModItems.MESSAGE_STONE.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, left);
        p.setItemInHand(InteractionHand.OFF_HAND, right);
        left.getItem().use(p.level(), p, InteractionHand.MAIN_HAND);
        if (VoiceCommunicationService.isAvailable()) {
            var a = MessageStoneItem.binding(left);
            var b = MessageStoneItem.binding(right);
            h.assertTrue(a != null && b != null && a.partnerId().equals(b.stoneId())
                    && b.partnerId().equals(a.stoneId()) && a.pairId().equals(b.pairId()), "stones were not mutually paired");
            left.getItem().use(p.level(), p, InteractionHand.MAIN_HAND);
        }
        h.assertTrue(MessageStoneItem.binding(left) == null && MessageStoneItem.binding(right) == null,
                "disabled use or unbinding left active bindings");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void armoryValidatesSelectionAndSpiritualization(GameTestHelper h) {
        ServerPlayer p = servant(h, "lancelot_berserker");
        var v = vars(p);
        KnightArmoryService.select(p, -1);
        h.assertTrue(v.servant_card_mana == 1000.0, "invalid selection consumed mana");
        KnightArmoryService.select(p, KnightArmoryEntry.IRON_ROD.networkId());
        h.assertTrue(v.servant_card_mana == 990.0 && p.getInventory().countItem(
                net.xxxjk.TYPE_MOON_WORLD.item.ModItems.LANCELOT_IRON_ROD.get()) == 1, "base armory failed without optional guns");
        p.getPersistentData().putInt("FateSupportSpiritualizationPhase", SpiritualizationService.PHASE_ACTIVE);
        KnightArmoryService.select(p, KnightArmoryEntry.IRON_ROD.networkId());
        h.assertTrue(v.servant_card_mana == 990.0, "restricted selection consumed mana");
        h.assertTrue(!ServantCardTransformManager.triggerAction(p, 6), "injected action guard did not block spiritualized player");
        h.assertTrue(!ServantCombatDefenseService.requestDodge(p, -1, 0), "current core allowed dodge during spiritualization");
        h.assertTrue(!ServantCombatDefenseService.setGuarding(p, true), "current core allowed guard during spiritualization");
        p.getPersistentData().remove("FateSupportSpiritualizationPhase");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void spiritualizationTransitionsAndRestoresPhysics(GameTestHelper h) {
        ServerPlayer p = servant(h, "emiya_archer");
        h.assertTrue(SpiritualizationService.toggle(p), "entry refused");
        h.assertTrue(vars(p).servant_card_mana == 995.0, "entry mana cost incorrect");
        p.getPersistentData().putLong("FateSupportSpiritualizationPhaseUntil", p.level().getGameTime());
        SpiritualizationService.tickPlayer(p);
        h.assertTrue(SpiritualizationService.isRestricted(p) && p.isInvisible(), "entry did not activate restrictions and invisibility");
        h.assertTrue(SpiritualizationService.toggle(p), "exit refused");
        p.getPersistentData().putLong("FateSupportSpiritualizationPhaseUntil", p.level().getGameTime());
        SpiritualizationService.tickPlayer(p);
        h.assertTrue(!SpiritualizationService.isRestricted(p) && !p.isInvisible() && !p.noPhysics, "exit did not restore player state");
        h.assertTrue(!SpiritualizationService.toggle(p) && vars(p).servant_card_mana == 995.0, "cooldown allowed another entry or consumed mana");
        p.setGameMode(GameType.SPECTATOR);
        p.noPhysics = true;
        SpiritualizationService.tickPlayer(p);
        h.assertTrue(p.noPhysics && !SpiritualizationService.toggle(p), "spirit cleanup broke spectator movement or allowed spectator entry");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void spiritualizationPreservesExistingInvisibility(GameTestHelper h) {
        ServerPlayer p = servant(h, "emiya_archer");
        p.setInvisible(true);
        SpiritualizationService.toggle(p);
        p.getPersistentData().putLong("FateSupportSpiritualizationPhaseUntil", p.level().getGameTime());
        SpiritualizationService.tickPlayer(p);
        SpiritualizationService.toggle(p);
        p.getPersistentData().putLong("FateSupportSpiritualizationExitReveal", p.level().getGameTime());
        SpiritualizationService.tickPlayer(p);
        p.getPersistentData().putLong("FateSupportSpiritualizationPhaseUntil", p.level().getGameTime());
        SpiritualizationService.tickPlayer(p);
        h.assertTrue(p.isInvisible() && !p.noPhysics, "exiting spirit form removed earlier invisibility or retained noPhysics");
        SpiritualizationService.clearOnDisconnect(p);
        h.assertTrue(p.isInvisible(), "ordinary logout cleanup removed unrelated invisibility");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void leylineFallbackSuppressesDeathAndDrainsMana(GameTestHelper h) {
        ServerPlayer p = servant(h, "artoria_pendragon");
        bindLeyline(h, p);
        var v = vars(p);
        v.master_servant_survival_state = MasterServantLinkService.SURVIVAL_FORCED_DEATH;
        v.master_servant_survival_ticks = 1;
        MasterServantLinkService.tick(p, v);
        h.assertTrue(p.isAlive() && v.master_servant_survival_ticks == 0, "fallback did not replace master-loss death countdown");
        ServantCardManaService.tick(p, v);
        h.assertTrue(Math.abs(v.servant_card_mana - 999.75) < 1.0E-6 && v.servant_card_mana_regen == -5.0,
                "fallback regenerated mana instead of draining it");
        h.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
        MasterServantLinkService.tick(p, v);
        h.assertTrue(!p.getPersistentData().getBoolean("FateSupportArtificialLeylinePrimaryContract"), "destroyed leyline retained fallback protection");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void independentActionRunsBeforeLeylineFallback(GameTestHelper h) {
        ServerPlayer p = servant(h, "emiya_archer");
        bindLeyline(h, p);
        var v = vars(p);
        v.master_servant_survival_state = MasterServantLinkService.SURVIVAL_INDEPENDENT;
        v.master_servant_survival_ticks = 2;
        MasterServantLinkService.tick(p, v);
        h.assertTrue(MasterServantLinkService.SURVIVAL_INDEPENDENT.equals(v.master_servant_survival_state)
                && v.master_servant_survival_ticks == 1, "fallback skipped independent action");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void emiyaLoadoutRejectsLegacyForbiddenCopiesAndHonorsMana(GameTestHelper h) {
        ServerPlayer p = servant(h, "emiya_archer");
        var v = vars(p);
        p.getRandom().setSeed(42L);
        v.servant_card_mana = 0.0;
        // Old saves may contain copies that the new rules no longer allow.
        v.servant_card_emiya_copied_noble_phantasms = "typemoonworld:excalibur,typemoonworld:gilgamesh_ea,typemoonworld:gilgamesh_bab_ilu";
        for (int i = 0; i < 20; i++) {
            ServantCardEmiyaSkills.cycleLoadout(p, v);
            ItemStack ammo = p.getOffhandItem();
            h.assertTrue(p.getMainHandItem().is(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.NAMELESS_BOW.get()), "loadout did not equip the bow");
            h.assertTrue(ammo.is(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.CRIMSON_HOUND.get())
                    || ammo.is(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.PSEUDO_SPIRAL_SWORD.get()), "forbidden or unaffordable copy entered the loadout");
        }
        h.assertTrue(EmiyaSupportService.isExcaliburArrowExplosion(p,
                new ItemStack(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.EXCALIBUR.get())), "Emiya Excalibur arrow rule did not activate");
        v.servant_card_transformed = false;
        h.assertTrue(!EmiyaSupportService.isExcaliburArrowExplosion(p,
                new ItemStack(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.EXCALIBUR.get())), "released servant card kept Emiya-only rules");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void privateVoiceRoutesFollowContractValidity(GameTestHelper h) {
        ServerPlayer master = servant(h, "emiya_archer");
        ServerPlayer servant = servant(h, "artoria_pendragon");
        vars(master).servant_card_transformed = false;
        vars(master).master_active = true;
        vars(master).master_command_spells = 3;
        vars(master).master_servant_uuid = servant.getUUID().toString();
        vars(servant).servant_card_master_uuid = master.getUUID().toString();
        var magic = TypeMoonWorldApi.addon("fatesupport").magics();
        magic.knowledge(master).learn(VoiceCommunicationService.MESSAGE_MAGIC_ID);
        h.assertTrue(VoiceCommunicationService.toggleMessageMagic(master) == VoiceCommunicationService.isAvailable(), "message magic ignored voicechat availability");
        if (VoiceCommunicationService.isAvailable()) {
            VoiceCommunicationService service = new VoiceCommunicationService();
            service.onServerTick(new net.neoforged.neoforge.event.tick.ServerTickEvent.Post(() -> true, h.getLevel().getServer()));
            var route = VoiceCommunicationService.privateVoiceRoute(master.getUUID());
            h.assertTrue(route != null && route.targetId().equals(servant.getUUID()), "master-to-servant route missing");
            h.assertTrue(VoiceCommunicationService.privateVoiceRoute(servant.getUUID()).targetId().equals(master.getUUID()), "servant-to-master route missing");
            vars(master).master_command_spells = 0;
            service.onServerTick(new net.neoforged.neoforge.event.tick.ServerTickEvent.Post(() -> true, h.getLevel().getServer()));
            h.assertTrue(VoiceCommunicationService.privateVoiceRoute(master.getUUID()) == null, "expired contract retained private routing");
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void a10SummonRequiresWarfareAndHasOwnershipAndCooldown(GameTestHelper h) {
        ServerPlayer p = servant(h, "lancelot_berserker");
        boolean summoned = KnightArmoryService.summonA10Skill(p);
        h.assertTrue(summoned == SuperbWarfareLoader.isAvailable(), "A-10 availability mismatch");
        if (summoned) {
            h.assertTrue(vars(p).servant_card_mana == 550.0 && SuperbWarfareLoader.countOwnedVehicles(p) == 1,
                    "A-10 mana cost or vehicle ownership incorrect");
            h.assertTrue(p.isPassenger() && p.getVehicle().getPersistentData().getBoolean(SuperbWarfareLoader.VEHICLE_A10_TAG), "player was not mounted on the owned A-10");
            h.assertTrue(!KnightArmoryService.summonA10Skill(p) && vars(p).servant_card_mana == 550.0, "duplicate A-10 summon ignored cooldown");
            var vehicle = p.getVehicle();
            p.stopRiding();
            vehicle.discard();
        } else {
            h.assertTrue(vars(p).servant_card_mana == 1000.0, "missing warfare consumed A-10 mana");
        }
        h.succeed();
    }

    private static ServerPlayer servant(GameTestHelper h, String id) {
        // The vanilla helper skips NeoForge channel negotiation, which breaks other mods' login sync.
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "fatesupport-test");
        var information = net.minecraft.server.level.ClientInformation.createDefault();
        ServerPlayer p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, information);
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        var cookie = new net.minecraft.server.network.CommonListenerCookie(profile, 0, information, false,
                net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
        p.setGameMode(GameType.SURVIVAL);
        p.setNoGravity(true);
        p.setPos(h.absolutePos(new BlockPos(4, 3, 4)).getBottomCenter());
        var v = vars(p);
        v.servant_card_transformed = true;
        v.servant_card_id = id;
        v.servant_card_max_mana = 1000.0;
        v.servant_card_mana = 1000.0;
        v.master_active = false;
        return p;
    }

    private static void bindLeyline(GameTestHelper h, ServerPlayer p) {
        BlockPos relative = new BlockPos(2, 2, 2);
        h.setBlock(relative, ModBlocks.ARTIFICIAL_LEYLINE_BLOCK.get());
        BlockPos pos = h.absolutePos(relative);
        CompoundTag data = new CompoundTag();
        data.putUUID("Owner", p.getUUID());
        h.getLevel().getBlockEntity(pos).loadWithComponents(data, h.getLevel().registryAccess());
        var v = vars(p);
        v.servant_card_artificial_leyline_bonus_active = true;
        v.servant_card_artificial_leyline_dimension = p.level().dimension().location().toString();
        v.servant_card_artificial_leyline_x = pos.getX();
        v.servant_card_artificial_leyline_y = pos.getY();
        v.servant_card_artificial_leyline_z = pos.getZ();
        p.getPersistentData().putBoolean("FateSupportArtificialLeylinePrimaryContract", true);
    }

    private static TypeMoonWorldModVariables.PlayerVariables vars(ServerPlayer p) {
        return p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
    }
}
