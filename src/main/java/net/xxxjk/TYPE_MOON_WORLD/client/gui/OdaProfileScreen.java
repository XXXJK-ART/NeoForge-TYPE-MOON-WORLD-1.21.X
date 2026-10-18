package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaCombatEvents;

/** The same reference is available from the card HUD and the NPC command screen. */
@EventBusSubscriber(modid = "typemoonworld", value = Dist.CLIENT)
public final class OdaProfileScreen extends Screen {
   private static final KeyMapping OPEN = new KeyMapping("key.typemoonworld.oda_profile", InputConstants.KEY_F8, "key.categories.typemoonworld");
   private final Screen parent;
   private final List<FormattedCharSequence> lines = new ArrayList<>();
   private int scroll;
   public OdaProfileScreen(Screen parent) { super(Component.translatable("gui.typemoonworld.oda_profile.title")); this.parent = parent; }
   @Override protected void init() {
      lines.clear();
      for (String section : List.of("parameters", "traits", "class_passives", "personal_passives", "active", "np")) {
         add(Component.translatable("gui.typemoonworld.oda_profile." + section).withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD));
         if (section.equals("active") || section.equals("np")) {
            int start = section.equals("active") ? -1 : 8, end = section.equals("active") ? 7 : 9;
            for (int slot = start; slot <= end; slot++) {
               var skill = OdaRules.skill(slot);
               add(Component.literal((slot == -1 ? "⇧ + LMB" : Integer.toString(slot)) + " · ")
                  .append(Component.translatable("skill.typemoonworld.servant_card." + skill.effect()))
                  .append(" · " + skill.mp() + " MP / " + skill.cooldown() + " ticks"));
               add(Component.translatable("gui.typemoonworld.oda_profile.effect." + slot));
            }
         } else add(Component.translatable("gui.typemoonworld.oda_profile." + section + ".body"));
         lines.add(FormattedCharSequence.EMPTY);
      }
   }
   private void add(Component text) { lines.addAll(font.split(text, Math.min(560, width - 48))); }
   @Override public void render(GuiGraphics gui, int mouseX, int mouseY, float partial) {
      gui.fill(0, 0, width, height, 0xFF161723);
      gui.drawCenteredString(font, title, width / 2, 12, 0xFFEBBE70);
      int x = Math.max(24, (width - 560) / 2);
      gui.enableScissor(x, 32, width - 24, height - 24);
      for (int i = 0; i < lines.size(); i++) gui.drawString(font, lines.get(i), x, 36 + i * 12 - scroll, 0xFFE5E5E5);
      gui.disableScissor();
      gui.drawCenteredString(font, Component.translatable("gui.typemoonworld.oda_profile.close"), width / 2, height - 16, 0xFFAAAAAA);
   }
   @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
      scroll = Math.clamp(scroll - (int)(vertical * 28), 0, Math.max(0, lines.size() * 12 - height + 64)); return true;
   }
   @Override public void onClose() { minecraft.setScreen(parent); }
   @Override public boolean isPauseScreen() { return false; }
   @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
      var mc = Minecraft.getInstance();
      while (OPEN.consumeClick()) if (mc.player != null && mc.screen == null && OdaCombatEvents.isOda(mc.player)) mc.setScreen(new OdaProfileScreen(null));
   }
   @SubscribeEvent public static void hint(RenderGuiEvent.Post event) {
      var mc = Minecraft.getInstance();
      if (mc.player != null && !mc.options.hideGui && mc.screen == null && OdaCombatEvents.isOda(mc.player))
         event.getGuiGraphics().drawString(mc.font, Component.translatable("gui.typemoonworld.oda_profile.hint", OPEN.getTranslatedKeyMessage()), 8, 8, 0xFFEBBE70);
   }
   @EventBusSubscriber(modid = "typemoonworld", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
   public static final class Registration {
      @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(OPEN); }
   }
}
