package net.xxxjk.typemoonworld.smoke;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

@Mod(value = "typemoonworld_li_shuwen_card_smoke", dist = Dist.CLIENT)
public final class LiShuwenCardSmoke {
   private boolean opened;
   private int ticks;
   private boolean done;

   public LiShuwenCardSmoke() { NeoForge.EVENT_BUS.register(this); }

   @SubscribeEvent
   public void tick(ClientTickEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (done || mc.getOverlay() != null) return;
      try {
         if (mc.screen instanceof AccessibilityOnboardingScreen) {
            mc.options.onboardingAccessibilityFinished();
            mc.setScreen(new TitleScreen());
         }
         if (mc.screen instanceof TitleScreen && !opened) {
            opened = true;
            mc.options.pauseOnLostFocus = false;
            mc.options.guiScale().set(2);
            mc.getWindow().setWindowed(960, 540);
            mc.reloadResourcePacks().whenComplete((unused, failure) -> mc.execute(() -> {
               try {
                  if (failure != null) throw new IllegalStateException("Resource reload failed", failure);
                  checkResources(mc);
                  mc.setScreen(new CardScreen(new ItemStack(ModItems.SERVANT_CARD_LI_SHUWEN.get())));
               } catch (Throwable error) {
                  done = true;
                  TYPE_MOON_WORLD.LOGGER.error("LI_SHUWEN_CARD_SMOKE_FAIL", error);
                  mc.stop();
               }
            }));
            return;
         }
         if (!opened || !(mc.screen instanceof CardScreen)) return;
         if (++ticks == 45) {
            File output = new File(mc.gameDirectory, "qa");
            output.mkdirs();
            Screenshot.grab(output, "li-shuwen-card.png", mc.getMainRenderTarget(), ignored -> { });
            TYPE_MOON_WORLD.LOGGER.info("LI_SHUWEN_CARD_SMOKE_PASS screenshot=qa/screenshots/li-shuwen-card.png");
            done = true;
            mc.stop();
         }
      } catch (Throwable error) {
         done = true;
         TYPE_MOON_WORLD.LOGGER.error("LI_SHUWEN_CARD_SMOKE_FAIL", error);
         mc.stop();
      }
   }

   private static void checkResources(Minecraft mc) {
      require(mc.getResourceManager().getResource(id("models/item/servant_card_li_shuwen.json")).isPresent(), "card model missing");
      require(mc.getResourceManager().getResource(id("textures/item/card_faces_3d/servant/li_shuwen_card.png")).isPresent(), "card front missing");
      require(mc.getResourceManager().getResource(id("textures/item/servant_card_backs/assassin.png")).isPresent(), "assassin back missing");
      require(mc.getResourceManager().getResource(id("textures/item/card_edge.png")).isPresent(), "card edge missing");
      require(mc.getResourceManager().getResource(id("textures/models/armor/servant_card_li_shuwen_head.png")).isPresent(), "head overlay missing");
      BufferedImage front = readImage(mc, id("textures/item/card_faces_3d/servant/li_shuwen_card.png"));
      require(front.getWidth() == 292 && front.getHeight() == 500, "card front dimensions changed: " + front.getWidth() + "x" + front.getHeight());
      require(mc.getItemRenderer().getModel(new ItemStack(ModItems.SERVANT_CARD_LI_SHUWEN.get()), null, null, 0)
         != mc.getModelManager().getMissingModel(), "card baked model missing");
   }

   private static BufferedImage readImage(Minecraft mc, ResourceLocation id) {
      try (InputStream input = mc.getResourceManager().getResourceOrThrow(id).open()) {
         BufferedImage image = ImageIO.read(input);
         require(image != null, "unable to decode " + id);
         return image;
      } catch (Exception error) {
         throw new IllegalStateException("unable to read " + id, error);
      }
   }

   private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, path); }
   private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }

   private static final class CardScreen extends Screen {
      private final ItemStack card;

      private CardScreen(ItemStack card) {
         super(Component.literal("Li Shuwen Card Smoke"));
         this.card = card;
      }

      @Override
      public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         renderBackground(graphics, mouseX, mouseY, partialTick);
         graphics.drawCenteredString(font, title, width / 2, 18, 0xFFFFFF);
         graphics.pose().pushPose();
         graphics.pose().translate(width / 2.0F, height / 2.0F + 18.0F, 0.0F);
         graphics.pose().scale(8.0F, 8.0F, 8.0F);
         graphics.renderItem(card, -8, -8);
         graphics.pose().popPose();
      }
   }
}
