package net.xxxjk.TYPE_MOON_WORLD.init;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.client.PaleRiderClientState;
import net.xxxjk.TYPE_MOON_WORLD.client.ReplayUiSuppressor;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.MagicModeSwitcherScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.MagicRadialMenuScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.MagicWheelSwitchScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.MasterCommandSpellScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.ProjectionPresetScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.projection.StructuralAnalysisSelectionClient;
import net.xxxjk.TYPE_MOON_WORLD.client.projection.StructuralProjectionPlacementClient;
import net.xxxjk.TYPE_MOON_WORLD.network.Basic_information_gui_Message;
import net.xxxjk.TYPE_MOON_WORLD.network.BajiquanInputMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.GanryuInputMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.HakuryuRideMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.KendoInputMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.CastMagicMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.CycleMagicMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.Lose_health_regain_mana_Message;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicCircuitSwitchMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ManaBurstInputMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicModeSwitchMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.MysticEyesToggleMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ServantCardActionMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ServantCardBasicAttackMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ServantCardFlightMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ServantCardHoldActionMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ServantCardJumpMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.SwitchMagicWheelMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ThompsonContenderUseMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.network.PaleRiderPossessionInputMessage;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.martial.BajiquanCombatService;
import net.xxxjk.TYPE_MOON_WORLD.martial.GanryuCombatService;
import net.xxxjk.TYPE_MOON_WORLD.martial.KendoCombatService;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.talent.TalentService;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public class TypeMoonWorldModKeyMappings {
   public static final String KEY_CATEGORY = "key.categories.typemoonworld";
   public static final KeyMapping MAGIC_CIRCUIT_SWITCH = new KeyMapping("key.typemoonworld.magic_circuit_switch", 66, "key.categories.typemoonworld");
   public static final KeyMapping CAST_MAGIC = new KeyMapping("key.typemoonworld.cast_magic", 67, "key.categories.typemoonworld");
   public static final KeyMapping LOSE_HEALTH_REGAIN_MANA = new KeyMapping("key.typemoonworld.lose_health_regain_mana", 88, "key.categories.typemoonworld");
   public static final KeyMapping BASIC_INFORMATION_GUI = new KeyMapping("key.typemoonworld.basic_information_gui", 82, "key.categories.typemoonworld");
   public static final KeyMapping MYSTIC_EYES_ACTIVATE = new KeyMapping("key.typemoonworld.mystic_eyes_activate", 86, "key.categories.typemoonworld");
   public static final KeyMapping OPEN_PROJECTION_PRESET = new KeyMapping("key.typemoonworld.open_projection_preset", 258, "key.categories.typemoonworld");
   public static final KeyMapping CYCLE_MAGIC = new KeyMapping("key.typemoonworld.cycle_magic", 90, "key.categories.typemoonworld");
   public static final KeyMapping MAGIC_MODE_SWITCH = new KeyMapping("key.typemoonworld.magic_mode_switch", 341, "key.categories.typemoonworld");
   public static final KeyMapping MAGIC_WHEEL_SWITCH = new KeyMapping("key.typemoonworld.magic_wheel_switch", 342, "key.categories.typemoonworld");
   public static final KeyMapping[] SERVANT_CARD_SKILL_KEYS = new KeyMapping[]{
      new KeyMapping("key.typemoonworld.servant_card.slot0", GLFW.GLFW_KEY_KP_0, KEY_CATEGORY),
      new KeyMapping("key.typemoonworld.servant_card.slot1", GLFW.GLFW_KEY_KP_1, KEY_CATEGORY),
      new KeyMapping("key.typemoonworld.servant_card.slot2", GLFW.GLFW_KEY_KP_2, KEY_CATEGORY),
      new KeyMapping("key.typemoonworld.servant_card.slot3", GLFW.GLFW_KEY_KP_3, KEY_CATEGORY),
      new KeyMapping("key.typemoonworld.servant_card.slot4", GLFW.GLFW_KEY_KP_4, KEY_CATEGORY),
      new KeyMapping("key.typemoonworld.servant_card.slot5", GLFW.GLFW_KEY_KP_5, KEY_CATEGORY),
      new KeyMapping("key.typemoonworld.servant_card.slot6", GLFW.GLFW_KEY_KP_6, KEY_CATEGORY),
      new KeyMapping("key.typemoonworld.servant_card.slot7", GLFW.GLFW_KEY_KP_7, KEY_CATEGORY),
      new KeyMapping("key.typemoonworld.servant_card.slot8", GLFW.GLFW_KEY_KP_8, KEY_CATEGORY),
      new KeyMapping("key.typemoonworld.servant_card.slot9", GLFW.GLFW_KEY_KP_9, KEY_CATEGORY)
   };

   @SubscribeEvent
   public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
      event.register(MAGIC_CIRCUIT_SWITCH);
      event.register(CAST_MAGIC);
      event.register(LOSE_HEALTH_REGAIN_MANA);
      event.register(BASIC_INFORMATION_GUI);
      event.register(MYSTIC_EYES_ACTIVATE);
      event.register(OPEN_PROJECTION_PRESET);
      event.register(CYCLE_MAGIC);
      event.register(MAGIC_MODE_SWITCH);
      event.register(MAGIC_WHEEL_SWITCH);
      for (KeyMapping mapping : SERVANT_CARD_SKILL_KEYS) {
         event.register(mapping);
      }
   }

   @EventBusSubscriber({Dist.CLIENT})
   public static class KeyEventListener {
      private static final int RIGHT_ARM_CAST_POSE_TICKS = 6;
      private static final int MACHINE_GUN_POSE_WARMUP_TICKS = 30;
      private static final int MACHINE_GUN_POSE_STOP_NO_COOLDOWN_TICKS = 8;
      private static final long MACHINE_GUN_HOLD_RELEASE_STOP_MS = 250L;
      private static boolean isTabDown = false;
      private static boolean isCycleMagicDown = false;
      private static boolean isModeSwitchDown = false;
      private static boolean isWheelSwitchDown = false;
      private static final boolean[] numpadWheelDown = new boolean[10];
      private static final boolean[] servantCardHoldDown = new boolean[10];
      private static boolean servantJumpDown = false;
      private static boolean bajiquanJumpDown = false;
      private static boolean bajiquanCrouchDown = false;
      private static boolean ganryuJumpDown = false;
      private static boolean ganryuCrouchDown = false;
      private static boolean ganryuUseDown = false;
      private static boolean kendoJumpDown = false;
      private static boolean kendoCrouchDown = false;
      private static boolean kendoUseDown = false;
      private static long servantLastJumpTapMs = 0L;
      private static int servantFlightInputSendDelay = 0;
      private static int servantFlightInputKeepaliveChecks = 0;
      private static float lastServantFlightForward = Float.NaN;
      private static float lastServantFlightStrafe = Float.NaN;
      private static float lastServantFlightVertical = Float.NaN;
      private static int manaBurstInputSendDelay = 0;
      private static int manaBurstInputKeepaliveChecks = 0;
      private static float lastManaBurstForward = Float.NaN;
      private static float lastManaBurstStrafe = Float.NaN;
      private static boolean lastManaBurstJump = false;
      private static boolean lastManaBurstSneak = false;
      private static int paleRiderInputSendDelay = 0;
      private static int paleRiderInputKeepaliveChecks = 0;
      private static float lastPaleRiderForward = Float.NaN;
      private static float lastPaleRiderStrafe = Float.NaN;
      private static float lastPaleRiderVertical = Float.NaN;
      private static float lastPaleRiderYaw = Float.NaN;
      private static float lastPaleRiderPitch = Float.NaN;
      private static long castPressStartMs = -1L;
      private static boolean castLongTriggered = false;
      private static boolean machineGunCastKeyDown = false;
      private static long machineGunCastPressStartMs = -1L;
      private static boolean machineGunReleaseStopArmed = false;
      private static boolean ganderCastKeyDown = false;
      private static long ganderCastPressStartMs = -1L;
      private static int rightArmCastPoseTicks = 0;
      private static boolean machineGunPoseLatched = false;
      private static int machineGunPoseWarmupTicks = 0;
      private static int machineGunPoseNoCooldownTicks = 0;
      private static HumanoidArm localCastingArm = HumanoidArm.RIGHT;
      private static int lastProjectionCrestWheel = Integer.MIN_VALUE;
      private static int lastProjectionCrestRuntimeIndex = Integer.MIN_VALUE;
      private static int lastProjectionCrestSlot = Integer.MIN_VALUE;
      private static String lastProjectionCrestEntryId = "";
      private static int lastProjectionCrestPayloadHash = 0;

      @SubscribeEvent
      public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
         clearClientInputState();
      }

      @SubscribeEvent
      public static void onInteractionKey(InteractionKeyMappingTriggered event) {
         Minecraft minecraft = Minecraft.getInstance();
         Player player = minecraft.player;
         if (player == null || minecraft.screen != null) {
            return;
         }
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (event.isUseItem()) {
            if (player.getVehicle() instanceof net.xxxjk.TYPE_MOON_WORLD.entity.ZhaoYunHakuryuEntity mount
               && !isHakuryuMountedUseReserved(player, vars)) {
               PacketDistributor.sendToServer(new HakuryuRideMessage(mount.getId()), new CustomPacketPayload[0]);
               event.setCanceled(true);
               event.setSwingHand(true);
               return;
            }
            if (minecraft.hitResult instanceof EntityHitResult entityHit
               && entityHit.getEntity() instanceof net.xxxjk.TYPE_MOON_WORLD.entity.ZhaoYunHakuryuEntity mount) {
               PacketDistributor.sendToServer(new HakuryuRideMessage(mount.getId()), new CustomPacketPayload[0]);
               event.setCanceled(true);
               event.setSwingHand(true);
               return;
            }
         }
         if (isClientGanryuActive(player, vars)) {
            boolean blockTarget = minecraft.hitResult != null && minecraft.hitResult.getType() == HitResult.Type.BLOCK;
            if (event.isAttack() && !blockTarget) {
               PacketDistributor.sendToServer(new GanryuInputMessage(GanryuCombatService.INPUT_A, player.isCrouching(), minecraft.options.keyJump.isDown()));
               event.setCanceled(true);
               event.setSwingHand(true);
               return;
            }
            if (event.isUseItem()) {
               event.setCanceled(true);
               event.setSwingHand(true);
               return;
            }
         }
         if (isClientKendoActive(player, vars)) {
            boolean blockTarget = minecraft.hitResult != null && minecraft.hitResult.getType() == HitResult.Type.BLOCK;
            int school = "hokushin_ittoryu".equals(PlayerMagicSelectionService.getCurrentMagicId(vars)) ? 0 : 1;
            if (event.isAttack() && !blockTarget) {
               PacketDistributor.sendToServer(new KendoInputMessage(school, KendoCombatService.INPUT_A, player.isCrouching(), minecraft.options.keyJump.isDown()));
               event.setCanceled(true); event.setSwingHand(true); return;
            }
            if (event.isUseItem()) {
               PacketDistributor.sendToServer(new KendoInputMessage(school, KendoCombatService.INPUT_B_START, player.isCrouching(), minecraft.options.keyJump.isDown()));
               event.setCanceled(true); event.setSwingHand(true); return;
            }
         }
         if (isClientBajiquanActive(player, vars)) {
            boolean blockTarget = minecraft.hitResult != null && minecraft.hitResult.getType() == HitResult.Type.BLOCK;
            if (event.isAttack() && !blockTarget) {
               PacketDistributor.sendToServer(new BajiquanInputMessage(BajiquanCombatService.INPUT_A, player.isCrouching(), minecraft.options.keyJump.isDown()));
               event.setCanceled(true);
               event.setSwingHand(true);
               return;
            }
            if (event.isUseItem()) {
               PacketDistributor.sendToServer(new BajiquanInputMessage(BajiquanCombatService.INPUT_B, player.isCrouching(), minecraft.options.keyJump.isDown()));
               event.setCanceled(true);
               event.setSwingHand(true);
               return;
            }
         }
         if (event.isUseItem() && isClientDiarmuidDualWieldActive(player, vars)) {
            PacketDistributor.sendToServer(new ServantCardBasicAttackMessage(true), new CustomPacketPayload[0]);
            player.swing(InteractionHand.MAIN_HAND);
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
         }
         if (event.isUseItem() && player.isCrouching() && vars.servant_card_transformed && "gilgamesh".equals(vars.servant_card_id)) {
            PacketDistributor.sendToServer(new ServantCardBasicAttackMessage(true), new CustomPacketPayload[0]);
            event.setCanceled(true);
            event.setSwingHand(true);
            return;
         }
         if (event.isUseItem() && player.getMainHandItem().is(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.THOMPSON_CONTENDER.get())) {
            PacketDistributor.sendToServer(new ThompsonContenderUseMessage(), new CustomPacketPayload[0]);
            event.setCanceled(true);
            event.setSwingHand(true);
            return;
         }
         if (!event.isAttack()) {
            return;
         }
         if (isClientDiarmuidDualWieldActive(player, vars)
            && (minecraft.hitResult == null || minecraft.hitResult.getType() != HitResult.Type.BLOCK)) {
            PacketDistributor.sendToServer(new ServantCardBasicAttackMessage(false), new CustomPacketPayload[0]);
            player.swing(InteractionHand.OFF_HAND);
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
         }
         if (player.getMainHandItem().is(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.TEMPLE_STONE_SWORD_AXE.get())) {
            PacketDistributor.sendToServer(new ServantCardBasicAttackMessage(false), new CustomPacketPayload[0]);
            event.setCanceled(true);
            event.setSwingHand(true);
            return;
         }
         if (player.isCrouching() && vars.servant_card_transformed && supportsCrouchAttack(vars.servant_card_id)) {
            PacketDistributor.sendToServer(new ServantCardActionMessage(-1), new CustomPacketPayload[0]);
            event.setCanceled(true);
            event.setSwingHand(true);
            return;
         }
         if (vars.servant_card_transformed
            && "oda_nobunaga".equals(vars.servant_card_id)
            && net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardOdaNobunagaSkills.isHoldingHeshikiriClient(player)) {
            PacketDistributor.sendToServer(new ServantCardBasicAttackMessage(false), new CustomPacketPayload[0]);
         }
      }

      @SubscribeEvent
      public static void onPaleRiderMovementInput(MovementInputUpdateEvent event) {
         if (!(event.getEntity() instanceof Player player) || !player.isPassenger()) return;
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (vars.servant_card_transformed && "pale_rider".equals(vars.servant_card_id)
            && (PaleRiderClientState.possessing
               || player.getVehicle() instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ApocalypseHorseEntity)) {
            // Shift descends while possessing and must not dismount the domain horse.
            event.getInput().shiftKeyDown = false;
         }
      }

      @SubscribeEvent
      public static void onMouseScroll(MouseScrollingEvent event) {
         if (Minecraft.getInstance().screen == null) {
            double scrollDelta = event.getScrollDeltaY();
            if (scrollDelta == 0.0) {
               return;
            }

            if (StructuralProjectionPlacementClient.handleScroll(scrollDelta)) {
               event.setCanceled(true);
               return;
            }

            if (TypeMoonWorldModKeyMappings.MAGIC_MODE_SWITCH.isDown()) {
               Player player = Minecraft.getInstance().player;
               if (player != null) {
                  TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
                     TypeMoonWorldModVariables.PLAYER_VARIABLES
                  );
                  if (vars.is_magus && vars.is_magic_circuit_open) {
                     if (isCurrentSelectionFromCrest(vars)) {
                        event.setCanceled(true);
                        return;
                     }

                     boolean forward = scrollDelta > 0.0;
                     PacketDistributor.sendToServer(new MagicModeSwitchMessage(1, forward ? 1 : -1), new CustomPacketPayload[0]);
                     event.setCanceled(true);
                     return;
                  }
               }
            }

            if (TypeMoonWorldModKeyMappings.CYCLE_MAGIC.isDown()) {
               Player player = Minecraft.getInstance().player;
               if (player != null) {
                  TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
                     TypeMoonWorldModVariables.PLAYER_VARIABLES
                  );
                  if ((vars.is_magus && vars.is_magic_circuit_open) || TalentService.hasAny(vars)) {
                     PacketDistributor.sendToServer(new CycleMagicMessage(scrollDelta > 0.0), new CustomPacketPayload[0]);
                     event.setCanceled(true);
                  }
               }
            }
         }
      }

      @SubscribeEvent
      public static void onClientTick(Post event) {
         if (rightArmCastPoseTicks > 0) {
            rightArmCastPoseTicks--;
         }
         if (Minecraft.getInstance().screen != null) {
            clearClientInputState();
            return;
         }

         if (Minecraft.getInstance().screen == null) {
            Player player = Minecraft.getInstance().player;
            if (player == null) {
               clearClientInputState();
               return;
            }

            TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
               TypeMoonWorldModVariables.PLAYER_VARIABLES
            );
            boolean jumpDownNow = Minecraft.getInstance().options.keyJump.isDown();
            boolean crouchDownNow = Minecraft.getInstance().options.keyShift.isDown();
            boolean useDownNow = Minecraft.getInstance().options.keyUse.isDown();
            boolean ganryuActive = isClientGanryuActive(player, vars);
            boolean kendoActive = isClientKendoActive(player, vars);
            if (ganryuActive && crouchDownNow && !ganryuCrouchDown) {
               PacketDistributor.sendToServer(new GanryuInputMessage(GanryuCombatService.INPUT_DOWN, true, false));
            }
            if (ganryuActive && jumpDownNow && !ganryuJumpDown) {
               PacketDistributor.sendToServer(new GanryuInputMessage(GanryuCombatService.INPUT_JUMP, player.isCrouching(), true));
            }
            if (ganryuActive && useDownNow && !ganryuUseDown) {
               PacketDistributor.sendToServer(new GanryuInputMessage(GanryuCombatService.INPUT_B_START, player.isCrouching(), jumpDownNow));
            } else if ((!ganryuActive || !useDownNow) && ganryuUseDown) {
               PacketDistributor.sendToServer(new GanryuInputMessage(GanryuCombatService.INPUT_B_END, false, false));
            }
            ganryuJumpDown = ganryuActive && jumpDownNow;
            ganryuCrouchDown = ganryuActive && crouchDownNow;
            ganryuUseDown = ganryuActive && useDownNow;
            int kendoSchool = vars != null && "hokushin_ittoryu".equals(PlayerMagicSelectionService.getCurrentMagicId(vars)) ? 0 : 1;
            if (kendoActive && crouchDownNow && !kendoCrouchDown) PacketDistributor.sendToServer(new KendoInputMessage(kendoSchool, KendoCombatService.INPUT_DOWN, true, false));
            if (kendoActive && jumpDownNow && !kendoJumpDown) PacketDistributor.sendToServer(new KendoInputMessage(kendoSchool, KendoCombatService.INPUT_JUMP, player.isCrouching(), true));
            if (kendoActive && useDownNow && !kendoUseDown) PacketDistributor.sendToServer(new KendoInputMessage(kendoSchool, KendoCombatService.INPUT_B_START, player.isCrouching(), jumpDownNow));
            else if ((!kendoActive || !useDownNow) && kendoUseDown) PacketDistributor.sendToServer(new KendoInputMessage(kendoSchool, KendoCombatService.INPUT_B_END, false, false));
            kendoJumpDown = kendoActive && jumpDownNow;
            kendoCrouchDown = kendoActive && crouchDownNow;
            kendoUseDown = kendoActive && useDownNow;
            if (isClientBajiquanActive(player, vars) && crouchDownNow && !bajiquanCrouchDown) {
               PacketDistributor.sendToServer(new BajiquanInputMessage(BajiquanCombatService.INPUT_DOWN, true, false));
            }
            if (isClientBajiquanActive(player, vars) && jumpDownNow && !bajiquanJumpDown) {
               PacketDistributor.sendToServer(new BajiquanInputMessage(BajiquanCombatService.INPUT_JUMP, player.isCrouching(), true));
            }
            bajiquanJumpDown = jumpDownNow;
            bajiquanCrouchDown = crouchDownNow;
            localCastingArm = resolveLocalCastingArm(player);
            syncProjectionSelectionFromCurrentCrestPreset(player, vars);
            updateMachineGunFiringPose(vars);
            syncManaBurstInput(player, vars);
            StructuralProjectionPlacementClient.cancelIfInvalid(vars);
            boolean suppressScreens = ReplayUiSuppressor.shouldSuppressTypeMoonScreens();
            if (vars.master_active) {
               handleMasterControls(suppressScreens);
               if (Minecraft.getInstance().screen != null) {
                  return;
               }
            }
            if (vars.servant_card_transformed) {
               handleServantCardControls(vars);
               return;
            }
            clearServantCardInputState();
            if (TypeMoonWorldModKeyMappings.MAGIC_MODE_SWITCH.isDown()) {
               if (!isModeSwitchDown) {
                  if (vars.is_magus
                     && vars.is_magic_circuit_open
                     && !vars.selected_magics.isEmpty()
                     && vars.current_magic_index >= 0
                     && vars.current_magic_index < vars.selected_magics.size()) {
                     String currentMagic = vars.selected_magics.get(vars.current_magic_index);
                     if (vars.isCurrentSelectionFromCrest(currentMagic)) {
                        player.displayClientMessage(Component.translatable("message.typemoonworld.crest.preset_runtime_locked"), true);
                     } else if ("sword_barrel_full_open".equals(currentMagic)) {
                        if (!suppressScreens && Minecraft.getInstance().screen == null) {
                           Minecraft.getInstance().setScreen(new MagicModeSwitcherScreen(vars.sword_barrel_mode));
                           isModeSwitchDown = true;
                        }
                     } else if (!"reinforcement".equals(currentMagic)
                        && !"reinforcement_self".equals(currentMagic)
                        && !"reinforcement_other".equals(currentMagic)
                        && !"reinforcement_item".equals(currentMagic)) {
                        if ("gravity_magic".equals(currentMagic)) {
                           if (!suppressScreens && Minecraft.getInstance().screen == null) {
                              Minecraft.getInstance().setScreen(new MagicModeSwitcherScreen(vars.gravity_magic_mode));
                              isModeSwitchDown = true;
                           }
                        } else if ("gandr_machine_gun".equals(currentMagic)) {
                           PacketDistributor.sendToServer(new MagicModeSwitchMessage(1, 1), new CustomPacketPayload[0]);
                           isModeSwitchDown = true;
                        } else if ("healing_magic".equals(currentMagic)) {
                           PacketDistributor.sendToServer(new MagicModeSwitchMessage(8, -1), new CustomPacketPayload[0]);
                           isModeSwitchDown = true;
                        } else if (net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService.isElementalMagic(currentMagic)) {
                           PacketDistributor.sendToServer(new MagicModeSwitchMessage(9, -1), new CustomPacketPayload[0]);
                           isModeSwitchDown = true;
                        } else if ("time_alter".equals(currentMagic)) {
                           if (vars.proficiency_time_alter >= 80.0) {
                              Minecraft.getInstance().setScreen(new net.xxxjk.TYPE_MOON_WORLD.client.gui.TimeAlterMultiplierScreen(vars.time_alter_multiplier));
                           } else {
                              PacketDistributor.sendToServer(new MagicModeSwitchMessage(10, -1), new CustomPacketPayload[0]);
                           }
                           isModeSwitchDown = true;
                        } else if ("mana_burst".equals(currentMagic)) {
                           if (!suppressScreens && Minecraft.getInstance().screen == null) {
                              Minecraft.getInstance().setScreen(new MagicModeSwitcherScreen(0));
                              isModeSwitchDown = true;
                           }
                        }
                     } else if (!suppressScreens && Minecraft.getInstance().screen == null) {
                        Minecraft.getInstance().setScreen(new MagicModeSwitcherScreen(vars.reinforcement_mode));
                        isModeSwitchDown = true;
                     }
                  }

                  isModeSwitchDown = true;
               }
            } else {
               isModeSwitchDown = false;
            }

            if (TypeMoonWorldModKeyMappings.MAGIC_WHEEL_SWITCH.isDown()) {
               if (!isWheelSwitchDown) {
                  isWheelSwitchDown = true;
                  if (!suppressScreens
                     && ((vars.is_magus && vars.is_magic_circuit_open) || TalentService.hasAny(vars))
                     && !(Minecraft.getInstance().screen instanceof MagicWheelSwitchScreen)) {
                     Minecraft.getInstance().setScreen(new MagicWheelSwitchScreen(vars.active_wheel_index));
                  }
               }
            } else {
               isWheelSwitchDown = false;
            }

            handleNumpadWheelQuickSwitch(vars);
            if (TypeMoonWorldModKeyMappings.CYCLE_MAGIC.isDown()) {
               if (!isCycleMagicDown) {
                  isCycleMagicDown = true;
                  if (((vars.is_magus && vars.is_magic_circuit_open) || TalentService.hasAny(vars))
                     && !vars.selected_magics.isEmpty()
                     && !suppressScreens
                     && !(Minecraft.getInstance().screen instanceof MagicRadialMenuScreen)) {
                     Minecraft.getInstance()
                        .setScreen(
                           new MagicRadialMenuScreen(
                              vars.selected_magics, vars.selected_magic_display_names, buildRadialSourceFlags(vars), buildRadialCrestPresetHints(vars, player)
                           )
                        );
                  }
               }
            } else {
               isCycleMagicDown = false;
            }

            if (TypeMoonWorldModKeyMappings.MAGIC_CIRCUIT_SWITCH.consumeClick() && vars.is_magus) {
               PacketDistributor.sendToServer(new MagicCircuitSwitchMessage(0, 0), new CustomPacketPayload[0]);
               MagicCircuitSwitchMessage.pressAction(player, 0, 0);
            }

            handleCastKey(player, vars);
            if (TypeMoonWorldModKeyMappings.LOSE_HEALTH_REGAIN_MANA.consumeClick()) {
               PacketDistributor.sendToServer(new Lose_health_regain_mana_Message(0, 0), new CustomPacketPayload[0]);
               Lose_health_regain_mana_Message.pressAction(player, 0, 0);
            }

            if (TypeMoonWorldModKeyMappings.BASIC_INFORMATION_GUI.consumeClick() && !suppressScreens) {
               PacketDistributor.sendToServer(new Basic_information_gui_Message(0, 0), new CustomPacketPayload[0]);
               Basic_information_gui_Message.pressAction(player, 0, 0);
            }

            if (TypeMoonWorldModKeyMappings.MYSTIC_EYES_ACTIVATE.consumeClick() && vars.is_magus) {
               PacketDistributor.sendToServer(new MysticEyesToggleMessage(0), new CustomPacketPayload[0]);
               MysticEyesToggleMessage.pressAction(player, 0);
            }

            if (TypeMoonWorldModKeyMappings.OPEN_PROJECTION_PRESET.isDown()) {
               if (!isTabDown) {
                  isTabDown = true;
                  if (!suppressScreens && vars.is_magus && vars.is_magic_circuit_open && !vars.selected_magics.isEmpty()) {
                     int index = vars.current_magic_index;
                     if (index >= 0 && index < vars.selected_magics.size()) {
                        String magicId = vars.selected_magics.get(index);
                        if ("projection".equals(magicId)
                           || "structural_analysis".equals(magicId)
                           || "unlimited_blade_works".equals(magicId)
                           || "broken_phantasm".equals(magicId)) {
                           if (vars.isCurrentSelectionFromCrest(magicId)) {
                              player.displayClientMessage(Component.translatable("message.typemoonworld.crest.preset_runtime_locked"), true);
                           } else {
                              Minecraft.getInstance().setScreen(new ProjectionPresetScreen(player));
                           }
                        }
                     }
                  }
               }
            } else {
               isTabDown = false;
            }
         }
      }

      private static void handleCastKey(Player player, TypeMoonWorldModVariables.PlayerVariables vars) {
         String currentSelection = PlayerMagicSelectionService.getCurrentMagicId(vars);
         if (TalentService.isTalent(currentSelection) && TalentService.owns(vars, currentSelection)) {
            if (TypeMoonWorldModKeyMappings.CAST_MAGIC.consumeClick()) triggerCast(player, 0, 0);
            castPressStartMs = -1L;
            castLongTriggered = false;
            machineGunCastKeyDown = false;
            ganderCastKeyDown = false;
            return;
         }
         if (isClientBajiquanActive(player, vars)) {
            if (TypeMoonWorldModKeyMappings.CAST_MAGIC.consumeClick()) {
               PacketDistributor.sendToServer(new BajiquanInputMessage(BajiquanCombatService.INPUT_CIRCLE_REALM, false, false));
            }
            return;
         }
         boolean selectionActive = StructuralAnalysisSelectionClient.isActive();
         boolean structuralSelected = isStructuralAnalysisSelected(vars);
         boolean projectionStructureSelected = isStructureProjectionSelected(vars);
         if (projectionStructureSelected) {
            if (TypeMoonWorldModKeyMappings.CAST_MAGIC.consumeClick() && vars.is_magus) {
               if (!StructuralProjectionPlacementClient.isActive()) {
                  StructuralProjectionPlacementClient.startPreview();
               } else if (!StructuralProjectionPlacementClient.isLocked()) {
                  StructuralProjectionPlacementClient.confirmPlacement();
               } else {
                  StructuralProjectionPlacementClient.startProjection();
               }
            }

            machineGunCastKeyDown = false;
            machineGunCastPressStartMs = -1L;
            machineGunReleaseStopArmed = false;
            machineGunPoseLatched = false;
            machineGunPoseWarmupTicks = 0;
            machineGunPoseNoCooldownTicks = 0;
            ganderCastKeyDown = false;
            ganderCastPressStartMs = -1L;
            castPressStartMs = -1L;
            castLongTriggered = false;
         } else if (!selectionActive && !structuralSelected) {
            if (isGanderSelected(vars)) {
               long now = System.currentTimeMillis();
               if (isFireInputDown(vars)) {
                  if (!ganderCastKeyDown && vars.is_magus && EntityUtils.hasAnyEmptyHand(player)) {
                     ganderCastKeyDown = true;
                     ganderCastPressStartMs = now;
                     triggerCast(player, 1, 0);
                  }
               } else {
                  if (ganderCastKeyDown && vars.is_magus) {
                     long holdMs = ganderCastPressStartMs < 0L ? 0L : now - ganderCastPressStartMs;
                     triggerCast(player, 2, (int)Math.min(2147483647L, holdMs));
                  }

                  ganderCastKeyDown = false;
                  ganderCastPressStartMs = -1L;
               }

               machineGunCastKeyDown = false;
               machineGunCastPressStartMs = -1L;
               machineGunReleaseStopArmed = false;
            } else if (isJewelMachineGunSelected(vars)) {
               long now = System.currentTimeMillis();
               boolean fireDown = isFireInputDown(vars);
               if (fireDown && !machineGunCastKeyDown) {
                  machineGunCastPressStartMs = now;
                  if (vars.is_magus && EntityUtils.hasAnyEmptyHand(player)) {
                     boolean likelyStopping = machineGunPoseLatched || vars.magic_cooldown > 0.0;
                     machineGunPoseLatched = !likelyStopping;
                     machineGunPoseWarmupTicks = machineGunPoseLatched ? 30 : 0;
                     machineGunPoseNoCooldownTicks = 0;
                     machineGunReleaseStopArmed = !likelyStopping;
                     triggerCast(player, 0, 0);
                  } else {
                     machineGunReleaseStopArmed = false;
                  }
               } else if (!fireDown && machineGunCastKeyDown) {
                  long holdMs = machineGunCastPressStartMs < 0L ? 0L : now - machineGunCastPressStartMs;
                  if (vars.is_magus && machineGunReleaseStopArmed && holdMs >= 250L) {
                     machineGunPoseLatched = false;
                     machineGunPoseWarmupTicks = 0;
                     machineGunPoseNoCooldownTicks = 0;
                     triggerCast(player, 0, (int)Math.min(2147483647L, holdMs));
                  }

                  machineGunCastPressStartMs = -1L;
                  machineGunReleaseStopArmed = false;
               }

               machineGunCastKeyDown = fireDown;
               ganderCastKeyDown = false;
               ganderCastPressStartMs = -1L;
            } else {
               machineGunCastKeyDown = false;
               machineGunCastPressStartMs = -1L;
               machineGunReleaseStopArmed = false;
               machineGunPoseLatched = false;
               machineGunPoseWarmupTicks = 0;
               machineGunPoseNoCooldownTicks = 0;
               ganderCastKeyDown = false;
               ganderCastPressStartMs = -1L;
               if (TypeMoonWorldModKeyMappings.CAST_MAGIC.consumeClick() && vars.is_magus) {
                  triggerRightArmCastPose(vars);
                  triggerCast(player, 0, 0);
               }
            }

            castPressStartMs = -1L;
            castLongTriggered = false;
         } else {
            machineGunCastKeyDown = false;
            machineGunCastPressStartMs = -1L;
            machineGunReleaseStopArmed = false;
            machineGunPoseLatched = false;
            machineGunPoseWarmupTicks = 0;
            machineGunPoseNoCooldownTicks = 0;
            ganderCastKeyDown = false;
            ganderCastPressStartMs = -1L;
            long now = System.currentTimeMillis();
            if (TypeMoonWorldModKeyMappings.CAST_MAGIC.isDown()) {
               if (castPressStartMs < 0L) {
                  castPressStartMs = now;
               }

               if (!selectionActive && structuralSelected && !castLongTriggered && now - castPressStartMs >= 1000L) {
                  StructuralAnalysisSelectionClient.startSelectionFromCrosshair();
                  castLongTriggered = true;
               }
            } else {
               if (castPressStartMs >= 0L) {
                  if (!castLongTriggered) {
                     if (StructuralAnalysisSelectionClient.isActive()) {
                        StructuralAnalysisSelectionClient.confirmWithCrosshair();
                     } else if (vars.is_magus) {
                        triggerCast(player, 0, 0);
                     }
                  }

                  castPressStartMs = -1L;
                  castLongTriggered = false;
               }
            }
         }
      }

      private static void handleNumpadWheelQuickSwitch(TypeMoonWorldModVariables.PlayerVariables vars) {
         if ((vars.is_magus && vars.is_magic_circuit_open) || TalentService.hasAny(vars)) {
            if (Minecraft.getInstance().screen == null) {
               long window = Minecraft.getInstance().getWindow().getWindow();
               int[] keys = new int[]{320, 321, 322, 323, 324, 325, 326, 327, 328, 329};

               for (int wheel = 0; wheel < keys.length; wheel++) {
                  boolean down = GLFW.glfwGetKey(window, keys[wheel]) == 1;
                  if (down && !numpadWheelDown[wheel]) {
                     PacketDistributor.sendToServer(new SwitchMagicWheelMessage(wheel), new CustomPacketPayload[0]);
                  }

                  numpadWheelDown[wheel] = down;
               }
            }
         } else {
            for (int i = 0; i < numpadWheelDown.length; i++) {
               numpadWheelDown[i] = false;
            }
         }
      }

      private static void handleServantCardControls(TypeMoonWorldModVariables.PlayerVariables vars) {
         if (Minecraft.getInstance().screen != null) {
            clearServantCardInputState();
            return;
         }
         long window = Minecraft.getInstance().getWindow().getWindow();
         if ("pale_rider".equals(vars.servant_card_id) && Minecraft.getInstance().player != null && PaleRiderClientState.possessing) {
            float forward = (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == 1 ? 1.0F : 0.0F) + (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == 1 ? -1.0F : 0.0F);
            float strafe = (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == 1 ? 1.0F : 0.0F) + (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == 1 ? -1.0F : 0.0F);
            boolean descend = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == 1 || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == 1;
            float vertical = (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == 1 ? 1.0F : 0.0F) + (descend ? -1.0F : 0.0F);
            float yaw = Minecraft.getInstance().player.getYRot();
            float pitch = Minecraft.getInstance().player.getXRot();
            if (paleRiderInputSendDelay > 0) {
               paleRiderInputSendDelay--;
            } else {
               boolean changed = forward != lastPaleRiderForward || strafe != lastPaleRiderStrafe || vertical != lastPaleRiderVertical
                  || Float.isNaN(lastPaleRiderYaw) || Math.abs(Mth.degreesDifference(lastPaleRiderYaw, yaw)) >= 2.0F
                  || Float.isNaN(lastPaleRiderPitch) || Math.abs(pitch - lastPaleRiderPitch) >= 2.0F;
               boolean moving = forward != 0.0F || strafe != 0.0F || vertical != 0.0F;
               int keepaliveChecks = moving ? 2 : 10;
               if (changed || ++paleRiderInputKeepaliveChecks >= keepaliveChecks) {
                  PacketDistributor.sendToServer(new PaleRiderPossessionInputMessage(forward, strafe, vertical, yaw, pitch), new CustomPacketPayload[0]);
                  lastPaleRiderForward = forward;
                  lastPaleRiderStrafe = strafe;
                  lastPaleRiderVertical = vertical;
                  lastPaleRiderYaw = yaw;
                  lastPaleRiderPitch = pitch;
                  paleRiderInputKeepaliveChecks = 0;
               }
               paleRiderInputSendDelay = 2;
            }
         } else {
            clearPaleRiderInputState();
         }
         for (int slot = 0; slot < TypeMoonWorldModKeyMappings.SERVANT_CARD_SKILL_KEYS.length; slot++) {
            if (isHoldServantCardSkill(vars, slot)) {
               boolean down = TypeMoonWorldModKeyMappings.SERVANT_CARD_SKILL_KEYS[slot].isDown();
               if (down != servantCardHoldDown[slot]) {
                  servantCardHoldDown[slot] = down;
                  PacketDistributor.sendToServer(new ServantCardHoldActionMessage(slot, down), new CustomPacketPayload[0]);
               }
            } else {
               servantCardHoldDown[slot] = false;
               while (TypeMoonWorldModKeyMappings.SERVANT_CARD_SKILL_KEYS[slot].consumeClick()) {
                  PacketDistributor.sendToServer(new ServantCardActionMessage(slot), new CustomPacketPayload[0]);
               }
            }
         }

         boolean jumpDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == 1;
         boolean sneakDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == 1 || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == 1;
         boolean backDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == 1;
         if (jumpDown && !servantJumpDown) {
            long now = System.currentTimeMillis();
            boolean flightServant = "medea".equals(vars.servant_card_id) || "oda_nobunaga".equals(vars.servant_card_id)
               || "enkidu".equals(vars.servant_card_id) || "gilgamesh".equals(vars.servant_card_id)
               || "gilgamesh_caster".equals(vars.servant_card_id);
            if (flightServant && now - servantLastJumpTapMs <= 280L && !sneakDown && !backDown) {
               PacketDistributor.sendToServer(new ServantCardFlightMessage(true, 0.0F, 0.0F, 0.0F), new CustomPacketPayload[0]);
               servantLastJumpTapMs = 0L;
            } else {
               servantLastJumpTapMs = now;
               if (sneakDown) {
                  float forward = (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == 1 ? 1.0F : 0.0F) + (backDown ? -1.0F : 0.0F);
                  float strafe = (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == 1 ? 1.0F : 0.0F) + (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == 1 ? -1.0F : 0.0F);
                  PacketDistributor.sendToServer(new ServantCardJumpMessage(forward, strafe), new CustomPacketPayload[0]);
               }
            }
         }
         if (vars.servant_card_flying && servantFlightInputSendDelay-- <= 0) {
            float forward = (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == 1 ? 1.0F : 0.0F) + (backDown ? -1.0F : 0.0F);
            float strafe = (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == 1 ? 1.0F : 0.0F) + (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == 1 ? -1.0F : 0.0F);
            float vertical = (jumpDown ? 1.0F : 0.0F) + (sneakDown ? -1.0F : 0.0F);
            boolean changed = forward != lastServantFlightForward || strafe != lastServantFlightStrafe || vertical != lastServantFlightVertical;
            if (changed || ++servantFlightInputKeepaliveChecks >= 10) {
               PacketDistributor.sendToServer(new ServantCardFlightMessage(false, forward, strafe, vertical), new CustomPacketPayload[0]);
               lastServantFlightForward = forward;
               lastServantFlightStrafe = strafe;
               lastServantFlightVertical = vertical;
               servantFlightInputKeepaliveChecks = 0;
               servantFlightInputSendDelay = 1;
            } else {
               servantFlightInputSendDelay = 1;
            }
         } else if (!vars.servant_card_flying) {
            servantFlightInputSendDelay = 0;
            servantFlightInputKeepaliveChecks = 0;
            lastServantFlightForward = Float.NaN;
            lastServantFlightStrafe = Float.NaN;
            lastServantFlightVertical = Float.NaN;
         }
         servantJumpDown = jumpDown;
      }

      public static void clearClientInputState() {
         clearServantCardInputState();
         clearManaBurstInputState();
         bajiquanJumpDown = false;
         bajiquanCrouchDown = false;
         ganryuJumpDown = false;
         ganryuCrouchDown = false;
         ganryuUseDown = false;
         kendoJumpDown = false;
         kendoCrouchDown = false;
         kendoUseDown = false;
      }

      private static void clearServantCardInputState() {
         servantJumpDown = false;
         servantLastJumpTapMs = 0L;
         servantFlightInputSendDelay = 0;
         servantFlightInputKeepaliveChecks = 0;
         lastServantFlightForward = Float.NaN;
         lastServantFlightStrafe = Float.NaN;
         lastServantFlightVertical = Float.NaN;
         clearPaleRiderInputState();
         for (int slot = 0; slot < servantCardHoldDown.length; slot++) {
            servantCardHoldDown[slot] = false;
         }
      }

      private static void clearPaleRiderInputState() {
         paleRiderInputSendDelay = 0;
         paleRiderInputKeepaliveChecks = 0;
         lastPaleRiderForward = Float.NaN;
         lastPaleRiderStrafe = Float.NaN;
         lastPaleRiderVertical = Float.NaN;
         lastPaleRiderYaw = Float.NaN;
         lastPaleRiderPitch = Float.NaN;
      }

      private static boolean isHoldServantCardSkill(TypeMoonWorldModVariables.PlayerVariables vars, int slot) {
         return ("emiya_archer".equals(vars.servant_card_id) && slot == 1)
            || ("li_shuwen".equals(vars.servant_card_id) && slot == 2)
            || ("arash".equals(vars.servant_card_id) && slot == 9)
            || ("oda_nobunaga".equals(vars.servant_card_id) && (slot == 4 || slot == 8));
      }

      private static void handleMasterControls(boolean suppressScreens) {
         if (Minecraft.getInstance().screen != null) {
            return;
         }
         while (TypeMoonWorldModKeyMappings.SERVANT_CARD_SKILL_KEYS[0].consumeClick()) {
            if (!suppressScreens) {
               Minecraft.getInstance().setScreen(new MasterCommandSpellScreen());
            }
         }
      }

      private static boolean supportsCrouchAttack(String servantId) {
         return "emiya_archer".equals(servantId)
            || "sasaki_kojiro".equals(servantId)
            || "cu_chulainn".equals(servantId)
            || "oda_nobunaga".equals(servantId)
            || "enkidu".equals(servantId)
            || "gilgamesh".equals(servantId)
            || "diarmuid_ua_duibhne".equals(servantId);
      }

      private static boolean isClientDiarmuidDualWieldActive(Player player, TypeMoonWorldModVariables.PlayerVariables vars) {
         return vars.servant_card_transformed
            && "diarmuid_ua_duibhne".equals(vars.servant_card_id)
            && vars.servant_card_action_mode == 1
            && player.getMainHandItem().getItem() instanceof net.xxxjk.TYPE_MOON_WORLD.item.custom.DiarmuidSpearItem
            && player.getOffhandItem().getItem() instanceof net.xxxjk.TYPE_MOON_WORLD.item.custom.DiarmuidSpearItem;
      }

      private static void triggerCast(Player player, int eventType, int pressedMs) {
         PacketDistributor.sendToServer(new CastMagicMessage(eventType, pressedMs), new CustomPacketPayload[0]);
         CastMagicMessage.pressAction(player, eventType, pressedMs);
      }

      private static void triggerRightArmCastPose(TypeMoonWorldModVariables.PlayerVariables vars) {
         int poseTicks = getTapCastPoseTicks(vars);
         if (poseTicks > 0) {
            rightArmCastPoseTicks = Math.max(rightArmCastPoseTicks, poseTicks);
         }
      }

      private static void updateMachineGunFiringPose(TypeMoonWorldModVariables.PlayerVariables vars) {
         if (!isJewelMachineGunSelected(vars)) {
            machineGunPoseLatched = false;
            machineGunPoseWarmupTicks = 0;
            machineGunPoseNoCooldownTicks = 0;
         } else if (machineGunPoseLatched) {
            if (machineGunPoseWarmupTicks > 0) {
               machineGunPoseWarmupTicks--;
               machineGunPoseNoCooldownTicks = 0;
            } else if (vars.magic_cooldown > 0.0) {
               machineGunPoseNoCooldownTicks = 0;
            } else {
               machineGunPoseNoCooldownTicks++;
               if (machineGunPoseNoCooldownTicks > 8) {
                  machineGunPoseLatched = false;
                  machineGunPoseWarmupTicks = 0;
                  machineGunPoseNoCooldownTicks = 0;
               }
            }
         }
      }

      private static void syncManaBurstInput(Player player, TypeMoonWorldModVariables.PlayerVariables vars) {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.options == null
            || player == null
            || vars == null
            || vars.servant_card_transformed
            || vars.master_card_active
            || !canSendManaBurstInput(vars)) {
            clearManaBurstInputState();
            return;
         }
         if (manaBurstInputSendDelay > 0) {
            manaBurstInputSendDelay--;
            return;
         }

         float forward = (minecraft.options.keyUp.isDown() ? 1.0F : 0.0F) + (minecraft.options.keyDown.isDown() ? -1.0F : 0.0F);
         float strafe = (minecraft.options.keyLeft.isDown() ? -1.0F : 0.0F) + (minecraft.options.keyRight.isDown() ? 1.0F : 0.0F);
         boolean jump = minecraft.options.keyJump.isDown();
         boolean sneak = minecraft.options.keyShift.isDown();
         if (forward != 0.0F || strafe != 0.0F || jump || sneak) {
            boolean changed = forward != lastManaBurstForward || strafe != lastManaBurstStrafe
               || jump != lastManaBurstJump || sneak != lastManaBurstSneak;
            if (changed || ++manaBurstInputKeepaliveChecks >= 3) {
               PacketDistributor.sendToServer(new ManaBurstInputMessage(forward, strafe, jump, sneak), new CustomPacketPayload[0]);
               lastManaBurstForward = forward;
               lastManaBurstStrafe = strafe;
               lastManaBurstJump = jump;
               lastManaBurstSneak = sneak;
               manaBurstInputKeepaliveChecks = 0;
            }
            manaBurstInputSendDelay = 1;
         } else {
            clearManaBurstInputState();
            manaBurstInputSendDelay = 2;
         }
      }

      private static void clearManaBurstInputState() {
         manaBurstInputSendDelay = 0;
         manaBurstInputKeepaliveChecks = 0;
         lastManaBurstForward = Float.NaN;
         lastManaBurstStrafe = Float.NaN;
         lastManaBurstJump = false;
         lastManaBurstSneak = false;
      }

      private static boolean canSendManaBurstInput(TypeMoonWorldModVariables.PlayerVariables vars) {
         return vars.learned_magics.contains("mana_burst") || vars.selected_magics.contains("mana_burst");
      }

      private static int getTapCastPoseTicks(TypeMoonWorldModVariables.PlayerVariables vars) {
         if (!vars.is_magus || !vars.is_magic_circuit_open) {
            return 0;
         } else if (vars.selected_magics.isEmpty()) {
            return 0;
         } else {
            int index = vars.current_magic_index;
            if (index >= 0 && index < vars.selected_magics.size()) {
               String magicId = vars.selected_magics.get(index);

               return switch (magicId) {
                  case "jewel_random_shoot", "broken_phantasm" -> 6;
                  default -> 0;
               };
            } else {
               return 0;
            }
         }
      }

      private static boolean isFireInputDown(TypeMoonWorldModVariables.PlayerVariables vars) {
         if (TypeMoonWorldModKeyMappings.CAST_MAGIC.isDown()) {
            return true;
         } else if (!isGunLikeSpellSelected(vars)) {
            return false;
         } else {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft.options != null && minecraft.options.keyAttack.isDown();
         }
      }

      private static boolean isGunLikeSpellSelected(TypeMoonWorldModVariables.PlayerVariables vars) {
         return isGanderSelected(vars) || isJewelMachineGunSelected(vars);
      }

      private static boolean isClientBajiquanActive(Player player, TypeMoonWorldModVariables.PlayerVariables vars) {
         return player != null && vars != null && vars.bajiquan_learned && vars.is_magic_circuit_open && !vars.servant_card_transformed
            && player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()
            && BajiquanCombatService.MAGIC_ID.equals(net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService.getCurrentMagicId(vars));
      }

      private static boolean isHakuryuMountedUseReserved(Player player, TypeMoonWorldModVariables.PlayerVariables vars) {
         return vars != null
            && vars.servant_card_transformed
            && "zhao_yun_rider".equals(vars.servant_card_id)
            && player.getMainHandItem().is(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.YAJIAO_QIANG.get());
      }

      private static boolean isClientGanryuActive(Player player, TypeMoonWorldModVariables.PlayerVariables vars) {
         if (player == null || vars == null || !vars.ganryu_learned || !vars.is_magic_circuit_open || vars.servant_card_transformed
            || !GanryuCombatService.MAGIC_ID.equals(net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService.getCurrentMagicId(vars))) return false;
         ItemStack main = player.getMainHandItem();
         ItemStack off = player.getOffhandItem();
         return (GanryuCombatService.isAllowedBlade(main) || GanryuCombatService.isAllowedBlade(off))
            && (main.isEmpty() || GanryuCombatService.isAllowedBlade(main))
            && (off.isEmpty() || GanryuCombatService.isAllowedBlade(off));
      }

      private static boolean isClientKendoActive(Player player, TypeMoonWorldModVariables.PlayerVariables vars) {
         if (player == null || vars == null || !vars.is_magic_circuit_open || vars.servant_card_transformed) return false;
         String id = PlayerMagicSelectionService.getCurrentMagicId(vars);
         boolean learned = ("hokushin_ittoryu".equals(id) && vars.hokushin_learned)
            || ("tennen_rishin_ryu".equals(id) && vars.tennen_learned);
         ItemStack main = player.getMainHandItem();
         ItemStack off = player.getOffhandItem();
         return learned && (GanryuCombatService.isAllowedBlade(main) || GanryuCombatService.isAllowedBlade(off))
            && (main.isEmpty() || GanryuCombatService.isAllowedBlade(main))
            && (off.isEmpty() || GanryuCombatService.isAllowedBlade(off));
      }

      private static HumanoidArm resolveLocalCastingArm(Player player) {
         HumanoidArm arm = EntityUtils.resolveEmptyCastingArm(player);
         return arm == null ? player.getMainArm() : arm;
      }

      private static List<Boolean> buildRadialSourceFlags(TypeMoonWorldModVariables.PlayerVariables vars) {
         List<Boolean> flags = new ArrayList<>(vars.selected_magics.size());

         for (int i = 0; i < vars.selected_magics.size(); i++) {
            TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = getRuntimeWheelEntry(vars, i);
            flags.add(entry != null && "crest".equals(entry.sourceType));
         }

         return flags;
      }

      private static List<String> buildRadialCrestPresetHints(TypeMoonWorldModVariables.PlayerVariables vars, Player player) {
         List<String> hints = new ArrayList<>(vars.selected_magics.size());

         for (int i = 0; i < vars.selected_magics.size(); i++) {
            TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = getRuntimeWheelEntry(vars, i);
            if (entry != null && "crest".equals(entry.sourceType)) {
               hints.add(buildCrestPresetHintForEntry(entry, vars, player));
            } else {
               hints.add("");
            }
         }

         return hints;
      }

      private static TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry getRuntimeWheelEntry(
         TypeMoonWorldModVariables.PlayerVariables vars, int runtimeIndex
      ) {
         if (runtimeIndex >= 0 && runtimeIndex < vars.selected_magic_runtime_slot_indices.size()) {
            int slot = vars.selected_magic_runtime_slot_indices.get(runtimeIndex);
            return vars.getWheelSlotEntry(vars.active_wheel_index, slot);
         } else {
            return null;
         }
      }

      private static String buildCrestPresetHintForEntry(
         TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry, TypeMoonWorldModVariables.PlayerVariables vars, Player player
      ) {
         if (entry != null && entry.magicId != null && !entry.magicId.isEmpty()) {
            CompoundTag payload = entry.presetPayload == null ? new CompoundTag() : entry.presetPayload;
            if (payload.isEmpty()) {
               return "";
            } else {
               String var4 = entry.magicId;

               return switch (var4) {
                  case "reinforcement" -> buildReinforcementHint(payload);
                  case "gravity_magic" -> buildGravityHint(payload);
                  case "gandr_machine_gun" -> buildGandrMachineGunHint(payload);
                  case "projection" -> buildProjectionHint(payload, vars, player);
                  case "mana_burst" -> buildManaBurstHint(payload);
                  default -> "";
               };
            }
         } else {
            return "";
         }
      }

      private static String buildReinforcementHint(CompoundTag payload) {
         int target = payload.contains("reinforcement_target") ? payload.getInt("reinforcement_target") : 0;
         int mode = payload.contains("reinforcement_mode") ? payload.getInt("reinforcement_mode") : 0;
         int level = payload.contains("reinforcement_level") ? payload.getInt("reinforcement_level") : 1;

         String targetKey = switch (target) {
            case 1 -> "gui.typemoonworld.overlay.reinforcement.target.other.short";
            case 2 -> "gui.typemoonworld.overlay.reinforcement.target.item.short";
            case 3 -> "gui.typemoonworld.overlay.reinforcement.target.cancel.short";
            default -> "gui.typemoonworld.overlay.reinforcement.target.self.short";
         };
         if (target == 3) {
            String cancelKey = switch (mode) {
               case 1 -> "gui.typemoonworld.overlay.reinforcement.cancel.other.short";
               case 2 -> "gui.typemoonworld.overlay.reinforcement.cancel.item.short";
               default -> "gui.typemoonworld.overlay.reinforcement.cancel.self.short";
            };
            return Component.translatable(targetKey).getString() + "/" + Component.translatable(cancelKey).getString();
         } else if (target == 2) {
            return Component.translatable(targetKey).getString() + " L" + Math.max(1, Math.min(5, level));
         } else {
            String partKey = switch (mode) {
               case 1 -> "gui.typemoonworld.overlay.reinforcement.part.arm.short";
               case 2 -> "gui.typemoonworld.overlay.reinforcement.part.leg.short";
               case 3 -> "gui.typemoonworld.overlay.reinforcement.part.eye.short";
               default -> "gui.typemoonworld.overlay.reinforcement.part.body.short";
            };
            return Component.translatable(targetKey).getString() + "/" + Component.translatable(partKey).getString() + " L" + Math.max(1, Math.min(5, level));
         }
      }

      private static String buildGravityHint(CompoundTag payload) {
         int target = payload.contains("gravity_target") ? payload.getInt("gravity_target") : 0;
         int mode = payload.contains("gravity_mode") ? payload.getInt("gravity_mode") : 0;
         String targetKey = target == 0 ? "gui.typemoonworld.overlay.gravity.target.self.short" : "gui.typemoonworld.overlay.gravity.target.other.short";

         String modeKey = switch (mode) {
            case -2 -> "gui.typemoonworld.overlay.gravity.mode.ultra_light.short";
            case -1 -> "gui.typemoonworld.overlay.gravity.mode.light.short";
            default -> "gui.typemoonworld.overlay.gravity.mode.normal.short";
            case 1 -> "gui.typemoonworld.overlay.gravity.mode.heavy.short";
            case 2 -> "gui.typemoonworld.overlay.gravity.mode.ultra_heavy.short";
         };
         return Component.translatable(targetKey).getString() + "/" + Component.translatable(modeKey).getString();
      }

      private static String buildGandrMachineGunHint(CompoundTag payload) {
         int mode = payload.contains("gandr_machine_gun_mode") ? payload.getInt("gandr_machine_gun_mode") : 0;
         String modeKey = mode == 1 ? "gui.typemoonworld.overlay.gandr.mode.barrage.short" : "gui.typemoonworld.overlay.gandr.mode.rapid.short";
         return Component.translatable(modeKey).getString();
      }

      private static String buildManaBurstHint(CompoundTag payload) {
         int mode = payload.contains("mana_burst_mode") ? Math.max(0, Math.min(2, payload.getInt("mana_burst_mode"))) : 1;
         int level = payload.contains("mana_burst_level") ? Math.max(1, Math.min(5, payload.getInt("mana_burst_level"))) : 1;
         String modeKey = switch (mode) {
            case 0 -> "gui.typemoonworld.mode.mana_burst.weapon";
            case 2 -> "gui.typemoonworld.mode.mana_burst.direct";
            default -> "gui.typemoonworld.mode.mana_burst.body";
         };
         return Component.translatable(modeKey).getString() + " L" + level;
      }

      private static String buildProjectionHint(CompoundTag payload, TypeMoonWorldModVariables.PlayerVariables vars, Player player) {
         if (payload.getBoolean("projection_lock_empty")) {
            return "EMPTY";
         } else if (!payload.contains("projection_structure_id")) {
            if (payload.contains("projection_item", 10) && player != null) {
               ItemStack projectionItem = ItemStack.parse(player.registryAccess(), payload.getCompound("projection_item")).orElse(ItemStack.EMPTY);
               String mode = Component.translatable("gui.typemoonworld.gem_carving_table.projection_mode.item").getString();
               return !projectionItem.isEmpty() ? mode + ":" + projectionItem.getHoverName().getString() : mode;
            } else {
               return "";
            }
         } else {
            String structureId = payload.getString("projection_structure_id");
            TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure = vars.getStructureById(structureId);
            String structureName = structure != null && structure.name != null && !structure.name.isEmpty() ? structure.name : structureId;
            return Component.translatable("gui.typemoonworld.gem_carving_table.projection_mode.structure").getString() + ":" + structureName;
         }
      }

      private static boolean isStructuralAnalysisSelected(TypeMoonWorldModVariables.PlayerVariables vars) {
         if (!vars.is_magus || !vars.is_magic_circuit_open) {
            return false;
         } else if (vars.selected_magics.isEmpty()) {
            return false;
         } else {
            int index = vars.current_magic_index;
            return index >= 0 && index < vars.selected_magics.size() ? "structural_analysis".equals(vars.selected_magics.get(index)) : false;
         }
      }

      private static boolean isStructureProjectionSelected(TypeMoonWorldModVariables.PlayerVariables vars) {
         if (!vars.is_magus || !vars.is_magic_circuit_open) {
            return false;
         } else if (vars.selected_magics.isEmpty()) {
            return false;
         } else {
            int index = vars.current_magic_index;
            if (index < 0 || index >= vars.selected_magics.size()) {
               return false;
            } else if (!"projection".equals(vars.selected_magics.get(index))) {
               return false;
            } else {
               if (vars.isCurrentSelectionFromCrest("projection")) {
                  TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = getRuntimeWheelEntry(vars, index);
                  if (entry != null) {
                     CompoundTag payload = TypeMoonWorldModVariables.PlayerVariables.normalizeProjectionPresetPayload(entry.presetPayload);
                     return payload.contains("projection_structure_id") && !payload.getString("projection_structure_id").isEmpty();
                  }
               }

               return vars.projection_selected_structure_id != null && !vars.projection_selected_structure_id.isEmpty();
            }
         }
      }

      private static void syncProjectionSelectionFromCurrentCrestPreset(Player player, TypeMoonWorldModVariables.PlayerVariables vars) {
         if (player != null && vars != null) {
            if (vars.is_magus && vars.is_magic_circuit_open && !vars.selected_magics.isEmpty()) {
               int runtimeIndex = vars.current_magic_index;
               if (runtimeIndex >= 0 && runtimeIndex < vars.selected_magics.size()) {
                  if ("projection".equals(vars.selected_magics.get(runtimeIndex))) {
                     if (vars.isCurrentSelectionFromCrest("projection")) {
                        TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = getRuntimeWheelEntry(vars, runtimeIndex);
                        if (entry != null) {
                           CompoundTag rawPayload = entry.presetPayload == null ? new CompoundTag() : entry.presetPayload;
                           int payloadHash = rawPayload.hashCode();
                           if (lastProjectionCrestWheel == vars.active_wheel_index
                              && lastProjectionCrestRuntimeIndex == runtimeIndex
                              && lastProjectionCrestSlot == entry.slotIndex
                              && lastProjectionCrestPayloadHash == payloadHash
                              && lastProjectionCrestEntryId.equals(entry.crestEntryId == null ? "" : entry.crestEntryId)) {
                              return;
                           }

                           CompoundTag payload = TypeMoonWorldModVariables.PlayerVariables.normalizeProjectionPresetPayload(entry.presetPayload);
                           if (payload.getBoolean("projection_lock_empty")) {
                              vars.projection_selected_structure_id = "";
                              vars.projection_selected_item = ItemStack.EMPTY;
                           } else if (payload.contains("projection_structure_id")) {
                              vars.projection_selected_structure_id = payload.getString("projection_structure_id");
                              vars.projection_selected_item = ItemStack.EMPTY;
                           } else {
                              if (payload.contains("projection_item", 10)) {
                                 ItemStack parsed = ItemStack.parse(player.registryAccess(), payload.getCompound("projection_item")).orElse(ItemStack.EMPTY);
                                 vars.projection_selected_item = parsed;
                                 vars.projection_selected_structure_id = "";
                              }
                           }
                           lastProjectionCrestWheel = vars.active_wheel_index;
                           lastProjectionCrestRuntimeIndex = runtimeIndex;
                           lastProjectionCrestSlot = entry.slotIndex;
                           lastProjectionCrestPayloadHash = payloadHash;
                           lastProjectionCrestEntryId = entry.crestEntryId == null ? "" : entry.crestEntryId;
                           return;
                        }
                     }
                  }
               }
            }
         }
         resetProjectionCrestCache();
      }

      private static void resetProjectionCrestCache() {
         lastProjectionCrestWheel = Integer.MIN_VALUE;
         lastProjectionCrestRuntimeIndex = Integer.MIN_VALUE;
         lastProjectionCrestSlot = Integer.MIN_VALUE;
         lastProjectionCrestEntryId = "";
         lastProjectionCrestPayloadHash = 0;
      }

      private static boolean isJewelMachineGunSelected(TypeMoonWorldModVariables.PlayerVariables vars) {
         if (!vars.is_magus || !vars.is_magic_circuit_open) {
            return false;
         } else if (vars.selected_magics.isEmpty()) {
            return false;
         } else {
            int index = vars.current_magic_index;
            if (index >= 0 && index < vars.selected_magics.size()) {
               String magicId = vars.selected_magics.get(index);
               return "jewel_machine_gun".equals(magicId) || "gandr_machine_gun".equals(magicId);
            } else {
               return false;
            }
         }
      }

      private static boolean isGanderSelected(TypeMoonWorldModVariables.PlayerVariables vars) {
         if (!vars.is_magus || !vars.is_magic_circuit_open) {
            return false;
         } else if (vars.selected_magics.isEmpty()) {
            return false;
         } else {
            int index = vars.current_magic_index;
            return index >= 0 && index < vars.selected_magics.size() ? "gander".equals(vars.selected_magics.get(index)) : false;
         }
      }

      private static boolean isCurrentSelectionFromCrest(TypeMoonWorldModVariables.PlayerVariables vars) {
         if (!vars.is_magus || !vars.is_magic_circuit_open) {
            return false;
         } else if (vars.selected_magics.isEmpty()) {
            return false;
         } else {
            int index = vars.current_magic_index;
            return index >= 0 && index < vars.selected_magics.size() ? vars.isCurrentSelectionFromCrest(vars.selected_magics.get(index)) : false;
         }
      }

      public static boolean isLocalGanderCharging() {
         return ganderCastKeyDown;
      }

      public static boolean isLocalGandrMachineGunCasting() {
         if (!machineGunCastKeyDown) {
            return false;
         } else {
            Player player = Minecraft.getInstance().player;
            if (player == null) {
               return false;
            } else {
               TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
                  TypeMoonWorldModVariables.PLAYER_VARIABLES
               );
               if (vars.selected_magics.isEmpty()) {
                  return false;
               } else {
                  int index = vars.current_magic_index;
                  return index >= 0 && index < vars.selected_magics.size() ? "gandr_machine_gun".equals(vars.selected_magics.get(index)) : false;
               }
            }
         }
      }

      public static boolean isLocalTapCastPoseActive() {
         return rightArmCastPoseTicks > 0;
      }

      public static boolean isLocalMachineGunFiringPoseActive() {
         return machineGunPoseLatched;
      }

      public static HumanoidArm getLocalCastingArm() {
         return localCastingArm;
      }
   }
}
