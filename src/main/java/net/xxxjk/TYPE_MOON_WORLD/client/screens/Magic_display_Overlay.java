package net.xxxjk.TYPE_MOON_WORLD.client.screens;

import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;

import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;

import net.minecraft.network.chat.Component;

@EventBusSubscriber({Dist.CLIENT})
@SuppressWarnings("null")
public class Magic_display_Overlay {
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void eventHandler(RenderGuiEvent.Pre event) {
        int h = event.getGuiGraphics().guiHeight();
        Player entity = Minecraft.getInstance().player;
        if (entity == null) return;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        // Fetch Mana Data
        TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        
        if (!vars.is_magus) return; // Do not render if not awakened
        
        double currentMana = vars.player_mana;
        double maxMana = vars.player_max_mana;
        
        // --- Mana Bar Configuration ---
        int barWidth = 120;
        int barHeight = 10;
        int barX = 10; // Reset X to 10
        int barY = h - 20;
        
        // 1. Draw Background (Dark, semi-transparent)
        event.getGuiGraphics().fill(barX, barY, barX + barWidth, barY + barHeight, 0x80000000);
        
        // 2. Draw Progress (Dynamic Color based on status)
        int startColor = 0xFF00E5FF; // Default Cyan
        int endColor = 0xFF2979FF;   // Default Blue
        
        if (currentMana <= maxMana * 0.2) {
             // Low Mana (Red/Orange Warning)
             startColor = 0xFFFF4000;
             endColor = 0xFFFF0000;
        } else if (currentMana > maxMana) {
             // Overload (Purple/Magenta)
             startColor = 0xFFFF00FF;
             endColor = 0xFF9D00FF;
        }

        if (maxMana > 0) {
            double ratio = Math.min(1.0, Math.max(0.0, currentMana / maxMana));
            int fillWidth = (int)(barWidth * ratio);
            event.getGuiGraphics().fillGradient(barX, barY, barX + fillWidth, barY + barHeight, startColor, endColor);
        }
        
        // 3. Draw Border/Outline
        // Top
        event.getGuiGraphics().fill(barX - 1, barY - 1, barX + barWidth + 1, barY, 0xFFFFFFFF);
        // Bottom
        event.getGuiGraphics().fill(barX - 1, barY + barHeight, barX + barWidth + 1, barY + barHeight + 1, 0xFFFFFFFF);
        // Left
        event.getGuiGraphics().fill(barX - 1, barY - 1, barX, barY + barHeight + 1, 0xFFFFFFFF);
        // Right
        event.getGuiGraphics().fill(barX + barWidth, barY - 1, barX + barWidth + 1, barY + barHeight + 1, 0xFFFFFFFF);

        // 4. Draw Mana Text (Centered on bar)
        String manaText = (int)currentMana + " / " + (int)maxMana;
        int textWidth = Minecraft.getInstance().font.width(manaText);
        int textX = barX + (barWidth - textWidth) / 2;
        int textY = barY + (barHeight - 8) / 2 + 1; // +1 for centering adjustment
        event.getGuiGraphics().drawString(Minecraft.getInstance().font, manaText, textX, textY, 0xFFFFFFFF, true);

        // 5. Draw Icon/Original UI Texture (Overlapping, scaled up 2x, drawn LAST to be ON TOP)
        // Icon size 16x16 (originally 8x8), drawn at barX - 4, barY - 3 to overlap
        int iconX = barX - 4;
        int iconY = barY - 3; 
        
        event.getGuiGraphics().blit(ResourceLocation.parse("typemoonworld:textures/screens/mana.png"), iconX, iconY, 0, 0, 16, 16, 16, 16);
        

        // --- Magic Name Display ---
        if (vars.is_magic_circuit_open) {
            net.minecraft.network.chat.MutableComponent magicName = Component.literal("None");
            int magicColor = 0xFF00FFFF; // Default Cyan

            if (!vars.selected_magics.isEmpty() && vars.current_magic_index >= 0 && vars.current_magic_index < vars.selected_magics.size()) {
                String magicId = vars.selected_magics.get(vars.current_magic_index);
                
                // Use translation keys instead of hardcoded strings
                String translationKey = "magic.typemoonworld." + magicId + ".name";
                
                // Fallback logic for legacy keys or direct ID mapping if needed, 
                // but generally we should use the lang file.
                // Assuming lang file entries exist or will be created.
                // For now, let's map known IDs to specific translation keys to be safe, 
                // or just use the generic pattern which is cleaner.
                
                magicName = Component.translatable(translationKey);
                
                if (magicId.startsWith("ruby")) {
                    magicColor = 0xFFFF0000; // Red
                } else if (magicId.startsWith("sapphire")) {
                    magicColor = 0xFF0088FF; // Blue
                } else if (magicId.startsWith("emerald")) {
                    magicColor = 0xFF00FF00; // Green
                } else if (magicId.startsWith("topaz")) {
                    magicColor = 0xFFFFFF00; // Yellow
                } else if ("projection".equals(magicId) || "structural_analysis".equals(magicId)) {
                    magicColor = 0xFF00FFFF; // Cyan
                } else if ("broken_phantasm".equals(magicId)) {
                    magicColor = 0xFFFF4000; // Orange Red
                } else if ("unlimited_blade_works".equals(magicId)) {
                    magicColor = 0xFFFF0000; // Red
                } else if ("sword_barrel_full_open".equals(magicId)) {
                    magicColor = 0xFFFF0000; // Red
                    // Append Mode info
                    magicName = Component.translatable(translationKey).append(" [Mode: " + vars.sword_barrel_mode + "]");
                }
            }
            
            Component labelStr = Component.translatable("gui.typemoonworld.overlay.current_magic");
            // Draw slightly above the bar
            // Icon overlaps bar, text should be above bar
            int magicTextX = barX; 
            int magicTextY = barY - 12;
            
            event.getGuiGraphics().drawString(Minecraft.getInstance().font, labelStr, magicTextX, magicTextY, 0xFFFFFFFF, true);
            event.getGuiGraphics().drawString(Minecraft.getInstance().font, magicName, magicTextX + Minecraft.getInstance().font.width(labelStr), magicTextY, magicColor, true);
        }

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }
}
