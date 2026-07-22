package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.PaleRiderCommandMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.PaleRiderSelectMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.PaleRiderSpawnModeMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.PaleRiderOpenScreenMessage;

public final class PaleRiderScreen extends Screen {
   private final int kind;
   private final List<PaleRiderOpenScreenMessage.Target> targets;

   public PaleRiderScreen(int kind, List<PaleRiderOpenScreenMessage.Target> targets) {
      super(Component.translatable(kind == 2 ? "screen.typemoonworld.pale_rider.possession" : kind == 3 ? "screen.typemoonworld.pale_rider.command" : "screen.typemoonworld.pale_rider.spawn"));
      this.kind = kind;
      this.targets = targets == null ? List.of() : targets;
   }

   @Override protected void init() {
      int width = 210;
      int left = (this.width - width) / 2;
      if (kind == 0 || kind == 1) {
         addRenderableWidget(Button.builder(Component.translatable(kind == 1 ? "screen.typemoonworld.pale_rider.single_rat" : "screen.typemoonworld.pale_rider.rat_swarm"), b -> selectSpawn(kind == 1 ? 2 : 0)).bounds(left, 40, width, 20).build());
         addRenderableWidget(Button.builder(Component.translatable(kind == 1 ? "screen.typemoonworld.pale_rider.single_crow" : "screen.typemoonworld.pale_rider.crow"), b -> selectSpawn(kind == 1 ? 3 : 1)).bounds(left, 65, width, 20).build());
         if (kind == 0) {
            addRenderableWidget(Button.builder(Component.translatable("screen.typemoonworld.pale_rider.single_rat"), b -> selectSpawn(2)).bounds(left, 90, width, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("screen.typemoonworld.pale_rider.single_crow"), b -> selectSpawn(3)).bounds(left, 115, width, 20).build());
         }
      } else if (kind == 2) {
         int mapLeft = (this.width - 240) / 2;
         int mapTop = 42;
         int mapWidth = 240;
         int mapHeight = Math.max(100, Math.min(160, this.height - 82));
         int minX = targets.stream().mapToInt(PaleRiderOpenScreenMessage.Target::x).min().orElse(0);
         int maxX = targets.stream().mapToInt(PaleRiderOpenScreenMessage.Target::x).max().orElse(minX + 1);
         int minZ = targets.stream().mapToInt(PaleRiderOpenScreenMessage.Target::z).min().orElse(0);
         int maxZ = targets.stream().mapToInt(PaleRiderOpenScreenMessage.Target::z).max().orElse(minZ + 1);
         int spanX = Math.max(1, maxX - minX);
         int spanZ = Math.max(1, maxZ - minZ);
         for (PaleRiderOpenScreenMessage.Target target : targets) {
            int px = mapLeft + 6 + (int)((target.x() - minX) / (double)spanX * (mapWidth - 28));
            int py = mapTop + 6 + (int)((target.z() - minZ) / (double)spanZ * (mapHeight - 28));
            Button marker = Button.builder(Component.literal(Integer.toString(target.entityId())), b -> selectEntity(target.entityId()))
               .bounds(px, py, 22, 18).tooltip(Tooltip.create(Component.literal(target.name() + "  X:" + target.x() + " Z:" + target.z()))).build();
            addRenderableWidget(marker);
         }
      } else {
         String[] labels = {"free", "hold", "attack", "gather"};
         for (int i = 0; i < labels.length; i++) {
            final int command = i;
            addRenderableWidget(Button.builder(Component.translatable("screen.typemoonworld.pale_rider.command." + labels[i]), b -> selectCommand(command)).bounds(left, 40 + i * 25, width, 20).build());
         }
      }
   }

   @Override public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(gui, mouseX, mouseY, partialTick);
      if (kind == 2) {
         int left = (this.width - 240) / 2;
         int top = 42;
         int height = Math.max(100, Math.min(160, this.height - 82));
         gui.fill(left, top, left + 240, top + height, 0xD0101315);
         gui.renderOutline(left, top, 240, height, 0xFF8E969D);
      }
      gui.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFE5E7EA);
      super.render(gui, mouseX, mouseY, partialTick);
   }

   private void selectSpawn(int mode) {
      PacketDistributor.sendToServer(new PaleRiderSpawnModeMessage(mode), new CustomPacketPayload[0]);
      Minecraft.getInstance().setScreen(null);
   }

   private void selectEntity(int id) {
      PacketDistributor.sendToServer(new PaleRiderSelectMessage(id), new CustomPacketPayload[0]);
      Minecraft.getInstance().setScreen(null);
   }

   private void selectCommand(int command) {
      PacketDistributor.sendToServer(new PaleRiderCommandMessage(command), new CustomPacketPayload[0]);
      Minecraft.getInstance().setScreen(null);
   }
}
