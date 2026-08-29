package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneProgram;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneReleaseMode;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneProgramKind;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneProgramService;
import net.xxxjk.TYPE_MOON_WORLD.network.RuneProgramMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Release configuration for a saved rune program. Slot composition stays in the editor. */
public final class RuneProgramConfigScreen extends Screen {
   private final Screen parent;
   private final RuneProgram program;

   public RuneProgramConfigScreen(Screen parent, RuneProgram program) {
      super(Component.translatable("gui.typemoonworld.rune.config"));
      this.parent = parent;
      this.program = program == null ? new RuneProgram() : program.copy();
      if (!RuneProgramService.matchesReleaseMode(this.program, this.program.releaseMode())) {
         this.program.setReleaseMode(defaultMode(this.program.kind()));
      }
   }

   @Override
   protected void init() {
      int panelX = Math.max(6, (width - Math.min(520, width - 12)) / 2);
      int panelW = Math.min(520, width - 12);
      int y = Math.max(12, (height - 250) / 2) + 52;
      addRenderableWidget(new NeonButton(panelX + 16, y, panelW - 32, 24,
         modeLabel(), b -> cycleMode(), GuiUtils.ARCANE_CYAN).setArcaneStyle(true));
      if (program.releaseMode() == RuneReleaseMode.BLOCK_TRAP) {
         addRenderableWidget(new NeonButton(panelX + 16, y + 32, panelW - 32, 20,
            configLabel(), b -> cycleConfig(), GuiUtils.ARCANE_GOLD).setArcaneStyle(true));
      }
      addRenderableWidget(new NeonButton(panelX + 16, y + 88, (panelW - 44) / 2, 22,
         Component.translatable("gui.typemoonworld.rune.editor"), b -> minecraft.setScreen(new RuneProgramEditorScreen(this, program)), GuiUtils.ARCANE_GOLD).setArcaneStyle(true));
      addRenderableWidget(new NeonButton(panelX + 28 + (panelW - 44) / 2, y + 88, (panelW - 44) / 2, 22,
         Component.translatable("gui.done"), b -> save(), GuiUtils.ARCANE_VALID).setArcaneStyle(true));
      addRenderableWidget(new NeonButton(panelX + 16, y + 118, panelW - 32, 22,
         Component.translatable("gui.cancel"), b -> onClose(), GuiUtils.ARCANE_BORDER).setArcaneStyle(true));
   }

   private Component configLabel() {
      var config = program.releaseConfig();
      if (program.releaseMode() == RuneReleaseMode.BLOCK_TRAP) {
         return Component.translatable("gui.typemoonworld.rune.trap_config", config.getDouble("radius"), config.getInt("delay"));
      }
      return Component.empty();
   }

   private void cycleConfig() {
      var config = program.releaseConfig();
      if (program.releaseMode() == RuneReleaseMode.BLOCK_TRAP) {
         double radius = config.contains("radius") ? config.getDouble("radius") : 4.0D;
         int delay = config.contains("delay") ? config.getInt("delay") : 0;
         radius = radius >= 16.0D ? 2.0D : radius + 2.0D;
         delay = delay >= 600 ? 0 : delay + 100;
         config.putDouble("radius", radius);
         config.putInt("delay", delay);
      }
      program.setReleaseConfig(config);
      clearWidgetsAndReinit();
   }

   private Component modeLabel() {
      String key = "gui.typemoonworld.rune.mode." + program.releaseMode().name().toLowerCase(java.util.Locale.ROOT);
      Component translated = Component.translatable(key);
      Component mode = translated.getString().equals(key) ? Component.literal(program.releaseMode().name()) : translated;
      return Component.translatable("gui.typemoonworld.rune.release_mode_select", mode);
   }

   private void cycleMode() {
      RuneReleaseMode[] modes = allowedModes();
      int current = java.util.Arrays.asList(modes).indexOf(program.releaseMode());
      program.setReleaseMode(modes[(current + 1 + modes.length) % modes.length]);
      clearWidgetsAndReinit();
   }

   private RuneReleaseMode[] allowedModes() {
      return switch (program.kind()) {
         case FULL_RELEASE -> new RuneReleaseMode[] {RuneReleaseMode.DIRECT_AIR, RuneReleaseMode.BLOCK_TRAP, RuneReleaseMode.RUNE_STONE};
         case ENCHANTMENT -> new RuneReleaseMode[] {RuneReleaseMode.WEAPON, RuneReleaseMode.TOOL, RuneReleaseMode.RUNE_STONE};
         case REINFORCEMENT -> new RuneReleaseMode[] {RuneReleaseMode.ARMOR, RuneReleaseMode.BODY, RuneReleaseMode.RUNE_STONE};
         case INSCRIPTION, INVALID -> new RuneReleaseMode[] {RuneReleaseMode.RUNE_STONE};
      };
   }

   private static RuneReleaseMode defaultMode(RuneProgramKind kind) {
      return switch (kind) {
         case FULL_RELEASE -> RuneReleaseMode.DIRECT_AIR;
         case ENCHANTMENT -> RuneReleaseMode.WEAPON;
         case REINFORCEMENT -> RuneReleaseMode.ARMOR;
         case INSCRIPTION, INVALID -> RuneReleaseMode.RUNE_STONE;
      };
   }

   private void clearWidgetsAndReinit() {
      clearWidgets();
      init();
   }

   private void save() {
      if (!program.validate().valid() || !RuneProgramService.matchesReleaseMode(program, program.releaseMode())) {
         if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable("message.typemoonworld.rune.save_failed"), true);
         }
         return;
      }
      // A newly-created editor copy is not persisted on the server yet.  Send
      // an empty anchor for creation; only an entry already present in the
      // client library may submit an update anchor.
      String persistedId = "";
      if (minecraft != null && minecraft.player != null) {
         TypeMoonWorldModVariables.PlayerVariables vars =
            minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (RuneProgramService.find(vars, program.uuid()) != null) persistedId = program.uuid().toString();
      }
      applyLocalProgramPreview();
      PacketDistributor.sendToServer(new RuneProgramMessage(RuneProgramMessage.UPSERT,
         program.serializeNBT(), persistedId), new CustomPacketPayload[0]);
      onClose();
   }

   private void applyLocalProgramPreview() {
      if (minecraft == null || minecraft.player == null) return;
      TypeMoonWorldModVariables.PlayerVariables vars =
         minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.ensureMagicSystemInitialized();
      for (int i = 0; i < vars.rune_programs.size(); i++) {
         RuneProgram current = vars.rune_programs.get(i);
         if (current != null && program.uuid().equals(current.uuid())) {
            vars.rune_programs.set(i, program.copy());
            return;
         }
      }
      if (vars.rune_programs.size() < RuneProgramService.MAX_PROGRAMS) vars.rune_programs.add(program.copy());
   }

   @Override
   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      GuiUtils.renderScreenBackdrop(graphics, width, height);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      renderBackground(graphics, mouseX, mouseY, partialTick);
      int panelW = Math.min(520, width - 12);
      int panelX = (width - panelW) / 2;
      int panelY = Math.max(12, (height - 250) / 2);
      GuiUtils.renderArcaneWindow(graphics, panelX, panelY, panelW, 250, GuiUtils.ARCANE_CYAN);
      graphics.drawCenteredString(font, title, width / 2, panelY + 10, GuiUtils.ARCANE_TEXT);
      graphics.drawCenteredString(font, Component.literal(program.displayName()), width / 2, panelY + 32, GuiUtils.ARCANE_TEXT_MUTED);
      graphics.drawCenteredString(font, Component.translatable("gui.typemoonworld.rune.config_hint"), width / 2, panelY + 125, GuiUtils.ARCANE_TEXT_MUTED);
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override public void onClose() { minecraft.setScreen(parent); }
   @Override public boolean isPauseScreen() { return false; }
}
