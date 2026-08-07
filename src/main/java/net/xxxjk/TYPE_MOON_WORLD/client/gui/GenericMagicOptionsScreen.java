package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicWheelSlotEditMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.typemoonworld.api.MagicOption;

/** Generic controls for addon magic presets. The server normalizes the submitted NBT. */
public final class GenericMagicOptionsScreen extends Screen {
   private final Screen parent;
   private final ResourceLocation magicId;
   private final List<MagicOption> options;
   private final Map<String, String> values = new LinkedHashMap<>();
   private final Map<String, EditBox> edits = new LinkedHashMap<>();
   private TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry;

   public GenericMagicOptionsScreen(Screen parent, ResourceLocation magicId, List<MagicOption> options) {
      super(Component.translatable("gui.typemoonworld.magic_options", magicId.toString()));
      this.parent = parent; this.magicId = magicId; this.options = List.copyOf(options);
   }

   @Override protected void init() {
      if (minecraft == null || minecraft.player == null) return;
      var vars = minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      this.entry = vars.getCurrentRuntimeWheelEntry();
      CompoundTag preset = entry == null || entry.presetPayload == null ? new CompoundTag() : entry.presetPayload;
      int y = Math.max(34, height / 2 - options.size() * 13);
      for (MagicOption option : options) {
         String initial = read(option, preset); values.put(option.key(), initial);
         if (option.kind() == MagicOption.Kind.INTEGER || option.kind() == MagicOption.Kind.ITEM) {
            EditBox edit = new EditBox(font, width / 2, y, 150, 20, Component.literal(option.key()));
            edit.setValue(initial); edit.setMaxLength(128); addRenderableWidget(edit); edits.put(option.key(), edit);
         } else {
            addRenderableWidget(new NeonButton(width / 2, y, 150, 20, label(option, initial), button -> cycle(option, button), MagicUiColors.colorFor(magicId.getPath(), false))
               .setArcaneStyle(true)
               .setCompactStyle(true));
         }
         y += 26;
      }
      addRenderableWidget(new NeonButton(width / 2 - 76, y + 6, 72, 20, Component.translatable("gui.done"), button -> save(), GuiUtils.ARCANE_VALID)
         .setArcaneStyle(true)
         .setCompactStyle(true));
      addRenderableWidget(new NeonButton(width / 2 + 4, y + 6, 72, 20, Component.translatable("gui.cancel"), button -> onClose(), GuiUtils.ARCANE_DANGER)
         .setArcaneStyle(true)
         .setCompactStyle(true));
   }

   private String read(MagicOption option, CompoundTag tag) {
      if (!tag.contains(option.key())) return option.defaultValue();
      return switch (option.kind()) {
         case BOOLEAN -> Boolean.toString(tag.getBoolean(option.key()));
         case INTEGER -> Integer.toString(tag.getInt(option.key()));
         default -> tag.getString(option.key());
      };
   }
   private Component label(MagicOption option, String value) { return Component.translatable("magic.option." + magicId.getNamespace() + "." + option.key()).append(": " + value); }
   private void cycle(MagicOption option, Button button) {
      String old = values.getOrDefault(option.key(), option.defaultValue()); String next;
      if (option.kind() == MagicOption.Kind.BOOLEAN) next = Boolean.toString(!Boolean.parseBoolean(old));
      else {
         List<String> choices = option.values().isEmpty() ? List.of("self", "target") : option.values();
         int index = Math.max(-1, choices.indexOf(old)); next = choices.get((index + 1) % choices.size());
      }
      values.put(option.key(), next); button.setMessage(label(option, next));
   }
   private void save() {
      if (entry == null || !magicId.toString().equals(entry.magicId)) { onClose(); return; }
      CompoundTag tag = entry.presetPayload == null ? new CompoundTag() : entry.presetPayload.copy();
      for (MagicOption option : options) {
         String value = edits.containsKey(option.key()) ? edits.get(option.key()).getValue() : values.getOrDefault(option.key(), option.defaultValue());
         switch (option.kind()) {
            case BOOLEAN -> tag.putBoolean(option.key(), Boolean.parseBoolean(value));
            case INTEGER -> { try { tag.putInt(option.key(), Math.max(option.min(), Math.min(option.max(), Integer.parseInt(value)))); } catch (NumberFormatException ignored) { tag.putInt(option.key(), option.min()); } }
            default -> tag.putString(option.key(), value == null ? "" : value.substring(0, Math.min(128, value.length())));
         }
      }
      PacketDistributor.sendToServer(new MagicWheelSlotEditMessage(MagicWheelSlotEditMessage.ACTION_SET, entry.wheelIndex, entry.slotIndex, -1,
         entry.sourceType, entry.magicId, tag, entry.crestEntryId, entry.displayNameCache), new CustomPacketPayload[0]);
      onClose();
   }
   @Override public void onClose() { if (minecraft != null) minecraft.setScreen(null); }
   @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      GuiUtils.renderScreenBackdrop(graphics, this.width, this.height);
      int formY = Math.max(34, this.height / 2 - this.options.size() * 13);
      int panelX = this.width / 2 - 176;
      int panelY = Math.max(18, formY - 32);
      int panelH = Math.min(this.height - panelY - 12, Math.max(92, this.options.size() * 26 + 68));
      int accent = MagicUiColors.colorFor(this.magicId.getPath(), false);
      GuiUtils.renderArcaneWindow(graphics, panelX, panelY, 352, panelH, accent);
      GuiUtils.renderArcanePanel(graphics, panelX + 10, panelY + 32, 332, Math.max(44, panelH - 44), accent);
   }
   @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);
      int formY = Math.max(34, this.height / 2 - this.options.size() * 13);
      graphics.drawCenteredString(font, title, width / 2, Math.max(26, formY - 23), GuiUtils.ARCANE_TEXT);
      int y = formY;
      for (MagicOption option : options) {
         graphics.drawString(font, Component.literal(option.key()), width / 2 - 158, y + 6, GuiUtils.ARCANE_TEXT_MUTED, false);
         y += 26;
      }
   }
}
