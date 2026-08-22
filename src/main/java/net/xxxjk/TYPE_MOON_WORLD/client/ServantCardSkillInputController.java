package net.xxxjk.TYPE_MOON_WORLD.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import net.xxxjk.TYPE_MOON_WORLD.network.ServantCardActionMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ServantCardHoldActionMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;

/** Runtime-only selection and direct-input handling for transformed servant-card players. */
public final class ServantCardSkillInputController {
   private static final boolean[] SLOT_DOWN = new boolean[ServantCardKeybindConfig.SLOT_COUNT];
   private static String selectedServantId = "";
   private static int selectedSlot = -1;
   private static boolean selectedHoldDown;

   private ServantCardSkillInputController() {
   }

   public static boolean handleScroll(Player player, TypeMoonWorldModVariables.PlayerVariables vars, double scrollDelta) {
      if (player == null || vars == null || !vars.servant_card_transformed || !TypeMoonWorldModKeyMappings.MAGIC_MODE_SWITCH.isDown()) {
         return false;
      }
      ensureSelection(vars);
      int next = findOccupiedSlot(vars, selectedSlot, scrollDelta > 0.0 ? 1 : -1);
      if (next < 0) {
         return false;
      }
      if (next != selectedSlot && selectedHoldDown) {
         PacketDistributor.sendToServer(new ServantCardHoldActionMessage(selectedSlot, false), new CustomPacketPayload[0]);
         selectedHoldDown = false;
      }
      selectedSlot = next;
      return true;
   }

   public static void tick(Player player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (player == null || vars == null || !vars.servant_card_transformed) {
         clear();
         return;
      }
      ensureSelection(vars);
      long window = Minecraft.getInstance().getWindow().getWindow();
      boolean anyDirectSkillDown = false;
      for (int slot = 0; slot < ServantCardKeybindConfig.SLOT_COUNT; slot++) {
         int key = ServantCardKeybindConfig.keyFor(vars.servant_card_id, slot);
         boolean down = ServantCardKeybindConfig.isKeyDown(window, key);
         anyDirectSkillDown |= down;
         boolean hold = isHoldSkill(vars.servant_card_id, slot);
         if (hold) {
            if (down != SLOT_DOWN[slot]) {
               SLOT_DOWN[slot] = down;
               if (down) {
                  sendSkillPressed(vars.servant_card_id, slot);
               } else if (isForcedHoldSkill(vars.servant_card_id, slot)) {
                  PacketDistributor.sendToServer(new ServantCardHoldActionMessage(slot, false), new CustomPacketPayload[0]);
               }
            }
         } else {
            if (down && !SLOT_DOWN[slot]) {
               PacketDistributor.sendToServer(new ServantCardActionMessage(slot), new CustomPacketPayload[0]);
            }
            SLOT_DOWN[slot] = down;
         }
      }

      if (selectedSlot >= 0 && TypeMoonWorldModKeyMappings.BASIC_INFORMATION_GUI.consumeClick() && !anyDirectSkillDown) {
         if (isForcedHoldSkill(vars.servant_card_id, selectedSlot)) {
            PacketDistributor.sendToServer(new ServantCardHoldActionMessage(selectedSlot, true), new CustomPacketPayload[0]);
            selectedHoldDown = true;
         } else {
            PacketDistributor.sendToServer(new ServantCardActionMessage(selectedSlot), new CustomPacketPayload[0]);
         }
      }
      if (selectedHoldDown && !TypeMoonWorldModKeyMappings.BASIC_INFORMATION_GUI.isDown()) {
         PacketDistributor.sendToServer(new ServantCardHoldActionMessage(selectedSlot, false), new CustomPacketPayload[0]);
         selectedHoldDown = false;
      }
   }

   public static int selectedSlot(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars == null || !vars.servant_card_transformed) {
         return -1;
      }
      ensureSelection(vars);
      return selectedSlot;
   }

   public static boolean isForcedHoldSkill(String servantId, int slot) {
      return ("emiya_archer".equals(servantId) && slot == 1)
         || ("li_shuwen".equals(servantId) && slot == 2)
         || ("arash".equals(servantId) && slot == 9)
         || ("oda_nobunaga".equals(servantId) && (slot == 4 || slot == 8));
   }

   public static boolean isHoldSkill(String servantId, int slot) {
      // The existing server hold handler has release behavior only for these special skills.
      return isForcedHoldSkill(servantId, slot);
   }

   public static void clear() {
      for (int slot = 0; slot < SLOT_DOWN.length; slot++) {
         SLOT_DOWN[slot] = false;
      }
      selectedServantId = "";
      selectedSlot = -1;
      selectedHoldDown = false;
   }

   private static void ensureSelection(TypeMoonWorldModVariables.PlayerVariables vars) {
      String servantId = vars.servant_card_id == null ? "" : vars.servant_card_id;
      if (!servantId.equals(selectedServantId)) {
         if (selectedHoldDown && selectedSlot >= 0) {
            PacketDistributor.sendToServer(new ServantCardHoldActionMessage(selectedSlot, false), new CustomPacketPayload[0]);
         }
         selectedServantId = servantId;
         selectedSlot = firstOccupiedSlot(vars);
         selectedHoldDown = false;
         for (int slot = 0; slot < SLOT_DOWN.length; slot++) {
            SLOT_DOWN[slot] = false;
         }
      } else if (!isOccupied(vars, selectedSlot)) {
         if (selectedHoldDown && selectedSlot >= 0) {
            PacketDistributor.sendToServer(new ServantCardHoldActionMessage(selectedSlot, false), new CustomPacketPayload[0]);
            selectedHoldDown = false;
         }
         selectedSlot = firstOccupiedSlot(vars);
      }
   }

   private static int firstOccupiedSlot(TypeMoonWorldModVariables.PlayerVariables vars) {
      for (int slot = 0; slot < ServantCardKeybindConfig.SLOT_COUNT; slot++) {
         if (isOccupied(vars, slot)) {
            return slot;
         }
      }
      return -1;
   }

   private static int findOccupiedSlot(TypeMoonWorldModVariables.PlayerVariables vars, int currentSlot, int direction) {
      for (int offset = 1; offset <= ServantCardKeybindConfig.SLOT_COUNT; offset++) {
         int slot = Math.floorMod(currentSlot + direction * offset, ServantCardKeybindConfig.SLOT_COUNT);
         if (isOccupied(vars, slot)) {
            return slot;
         }
      }
      return -1;
   }

   private static boolean isOccupied(TypeMoonWorldModVariables.PlayerVariables vars, int slot) {
      return slot >= 0 && slot < ServantCardKeybindConfig.SLOT_COUNT
         && !ServantCardTransformManager.skillTranslationKey(vars.servant_card_id, slot, false).isBlank();
   }

   private static void sendSkillPressed(String servantId, int slot) {
      if (isForcedHoldSkill(servantId, slot)) {
         PacketDistributor.sendToServer(new ServantCardHoldActionMessage(slot, true), new CustomPacketPayload[0]);
      } else {
         PacketDistributor.sendToServer(new ServantCardActionMessage(slot), new CustomPacketPayload[0]);
      }
   }
}
