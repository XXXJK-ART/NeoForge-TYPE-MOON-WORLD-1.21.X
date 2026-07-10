package net.xxxjk.TYPE_MOON_WORLD.client.screens;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.xxxjk.TYPE_MOON_WORLD.client.ReplayUiSuppressor;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@EventBusSubscriber({Dist.CLIENT})
@SuppressWarnings("null")
public class Magic_display_Overlay {
    private static final ResourceLocation MANA_ICON = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/screens/mana.png");

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void eventHandler(RenderGuiEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui) return;
        if (ReplayUiSuppressor.shouldHideTypeMoonHud()) return;

        int h = event.getGuiGraphics().guiHeight();
        Player entity = minecraft.player;
        if (entity == null) return;
        TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (vars.servant_card_transformed) return;
        if (!vars.is_magus) return;

        try {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ZERO
            );
            RenderSystem.setShaderColor(1, 1, 1, 1);

            double currentMana = vars.player_mana;
            double maxMana = vars.player_max_mana;

            int barWidth = 120;
            int barHeight = 10;
            int barX = 10;
            int barY = h - 20;

            event.getGuiGraphics().fill(barX, barY, barX + barWidth, barY + barHeight, 0x80000000);

            int startColor = 0xFF00E5FF;
            int endColor = 0xFF2979FF;
            if (currentMana <= maxMana * 0.2) {
                startColor = 0xFFFF4000;
                endColor = 0xFFFF0000;
            } else if (currentMana > maxMana) {
                startColor = 0xFFFF00FF;
                endColor = 0xFF9D00FF;
            }

            if (maxMana > 0) {
                double ratio = Math.min(1.0, Math.max(0.0, currentMana / maxMana));
                int fillWidth = (int) (barWidth * ratio);
                event.getGuiGraphics().fillGradient(barX, barY, barX + fillWidth, barY + barHeight, startColor, endColor);
            }

            event.getGuiGraphics().fill(barX - 1, barY - 1, barX + barWidth + 1, barY, 0xFFFFFFFF);
            event.getGuiGraphics().fill(barX - 1, barY + barHeight, barX + barWidth + 1, barY + barHeight + 1, 0xFFFFFFFF);
            event.getGuiGraphics().fill(barX - 1, barY - 1, barX, barY + barHeight + 1, 0xFFFFFFFF);
            event.getGuiGraphics().fill(barX + barWidth, barY - 1, barX + barWidth + 1, barY + barHeight + 1, 0xFFFFFFFF);

            String manaText = (int) currentMana + " / " + (int) maxMana;
            int textWidth = minecraft.font.width(manaText);
            int textX = barX + (barWidth - textWidth) / 2;
            int textY = barY + (barHeight - 8) / 2 + 1;
            event.getGuiGraphics().drawString(minecraft.font, manaText, textX, textY, 0xFFFFFFFF, true);

            int iconX = barX - 4;
            int iconY = barY - 3;
            event.getGuiGraphics().blit(MANA_ICON, iconX, iconY, 0, 0, 16, 16, 16, 16);

            if (vars.is_magic_circuit_open) {
                net.minecraft.network.chat.MutableComponent magicName = Component.translatable("gui.typemoonworld.mode.none");
                int magicColor = 0xFF00FFFF;

                if (!vars.selected_magics.isEmpty() && vars.current_magic_index >= 0 && vars.current_magic_index < vars.selected_magics.size()) {
                    String magicId = vars.selected_magics.get(vars.current_magic_index);
                    String translationKey = "magic.typemoonworld." + magicId + ".name";
                    magicName = Component.translatable(translationKey);

                    if (magicId.startsWith("ruby")) {
                        magicColor = 0xFFFF0000;
                    } else if (magicId.startsWith("sapphire")) {
                        magicColor = 0xFF0088FF;
                    } else if (magicId.startsWith("emerald")) {
                        magicColor = 0xFF00FF00;
                    } else if (magicId.startsWith("topaz")) {
                        magicColor = 0xFFFFFF00;
                    } else if ("projection".equals(magicId) || "structural_analysis".equals(magicId)) {
                        magicColor = 0xFF00FFFF;
                    } else if ("broken_phantasm".equals(magicId)) {
                        magicColor = 0xFFFF4000;
                    } else if ("unlimited_blade_works".equals(magicId)) {
                        magicColor = 0xFFFF0000;
                    } else if ("sword_barrel_full_open".equals(magicId)) {
                        magicColor = 0xFFFF0000;
                        magicName = Component.translatable(
                                "gui.typemoonworld.overlay.mode_with_index",
                                Component.translatable(translationKey),
                                vars.sword_barrel_mode
                        );
                    } else if ("gravity_magic".equals(magicId)) {
                        magicColor = 0xFF8A7CFF;
                        String targetKey = vars.gravity_magic_target == 0
                                ? "gui.typemoonworld.overlay.gravity.target.self.short"
                                : "gui.typemoonworld.overlay.gravity.target.other.short";
                        String modeKey = switch (vars.gravity_magic_mode) {
                            case -2 -> "gui.typemoonworld.overlay.gravity.mode.ultra_light.short";
                            case -1 -> "gui.typemoonworld.overlay.gravity.mode.light.short";
                            case 1 -> "gui.typemoonworld.overlay.gravity.mode.heavy.short";
                            case 2 -> "gui.typemoonworld.overlay.gravity.mode.ultra_heavy.short";
                            default -> "gui.typemoonworld.overlay.gravity.mode.normal.short";
                        };
                        magicName = Component.translatable(
                                "gui.typemoonworld.overlay.gravity.format",
                                Component.translatable(translationKey),
                                Component.translatable(targetKey),
                                Component.translatable(modeKey)
                        );
                    } else if ("healing_magic".equals(magicId)) {
                        magicColor = 0xFFCCFFCC;
                        String targetKey = vars.healing_magic_target == 0
                                ? "gui.typemoonworld.overlay.healing.target.self.short"
                                : "gui.typemoonworld.overlay.healing.target.other.short";
                        magicName = Component.translatable(
                                "gui.typemoonworld.overlay.healing.format",
                                Component.translatable(translationKey),
                                Component.translatable(targetKey)
                        );
                    } else if ("magic_bullet".equals(magicId)) {
                        magicColor = 0xFFCCAAFF;
                    } else if ("suggestion_magic".equals(magicId)) {
                        magicColor = 0xFFFFAAFF;
                    } else if ("binding_magic".equals(magicId)) {
                        magicColor = 0xFFFFDD66;
                    } else if ("time_alter".equals(magicId)) {
                        magicColor = 0xFF66CCFF;
                        magicName = Component.translatable(
                                "gui.typemoonworld.overlay.time_alter.format",
                                Component.translatable(translationKey),
                                Component.translatable(vars.time_alter_mode == 0
                                        ? "gui.typemoonworld.overlay.time_alter.mode.accel.short"
                                        : "gui.typemoonworld.overlay.time_alter.mode.stagnate.short")
                        );
                    } else if ("spiritual_healing".equals(magicId)) {
                        magicColor = 0xFFF8F3E7;
                    } else if ("baptism_rite".equals(magicId)) {
                        magicColor = 0xFFFFD24A;
                    } else if ("fire_magic".equals(magicId)
                            || "water_magic".equals(magicId)
                            || "wind_magic".equals(magicId)
                            || "earth_magic".equals(magicId)) {
                        int mode = switch (magicId) {
                            case "fire_magic" -> vars.fire_magic_mode;
                            case "water_magic" -> vars.water_magic_mode;
                            case "wind_magic" -> vars.wind_magic_mode;
                            case "earth_magic" -> vars.earth_magic_mode;
                            default -> 0;
                        };
                        magicColor = switch (magicId) {
                            case "fire_magic" -> 0xFFFF6633;
                            case "water_magic" -> 0xFF66CCFF;
                            case "wind_magic" -> 0xFF99FFCC;
                            case "earth_magic" -> 0xFFCCAA66;
                            default -> 0xFFFFFFFF;
                        };
                        magicName = Component.translatable(
                                "gui.typemoonworld.overlay.element.format",
                                Component.translatable(translationKey),
                                Component.translatable(mode == 1
                                        ? "gui.typemoonworld.overlay.element.mode.utility.short"
                                        : "gui.typemoonworld.overlay.element.mode.attack.short")
                        );
                    } else if ("jewel_random_shoot".equals(magicId)) {
                        magicColor = 0xFFEAEAEA;
                    } else if ("jewel_machine_gun".equals(magicId)) {
                        magicColor = 0xFFFF00FF;
                    } else if ("reinforcement".equals(magicId)
                            || "reinforcement_self".equals(magicId)
                            || "reinforcement_other".equals(magicId)
                            || "reinforcement_item".equals(magicId)) {
                        magicColor = 0xFF00AA00;

                        String targetKey = switch (vars.reinforcement_target) {
                            case 0 -> "gui.typemoonworld.overlay.reinforcement.target.self.short";
                            case 1 -> "gui.typemoonworld.overlay.reinforcement.target.other.short";
                            case 2 -> "gui.typemoonworld.overlay.reinforcement.target.item.short";
                            case 3 -> "gui.typemoonworld.overlay.reinforcement.target.cancel.short";
                            default -> "gui.typemoonworld.overlay.reinforcement.unknown.short";
                        };
                        String partKey = switch (vars.reinforcement_mode) {
                            case 0 -> "gui.typemoonworld.overlay.reinforcement.part.body.short";
                            case 1 -> "gui.typemoonworld.overlay.reinforcement.part.arm.short";
                            case 2 -> "gui.typemoonworld.overlay.reinforcement.part.leg.short";
                            case 3 -> "gui.typemoonworld.overlay.reinforcement.part.eye.short";
                            default -> "gui.typemoonworld.overlay.reinforcement.unknown.short";
                        };
                        String cancelKey = switch (vars.reinforcement_mode) {
                            case 0 -> "gui.typemoonworld.overlay.reinforcement.cancel.self.short";
                            case 1 -> "gui.typemoonworld.overlay.reinforcement.cancel.other.short";
                            case 2 -> "gui.typemoonworld.overlay.reinforcement.cancel.item.short";
                            default -> "gui.typemoonworld.overlay.reinforcement.unknown.short";
                        };

                        Component targetShort = Component.translatable(targetKey);
                        Component partShort = Component.translatable(partKey);
                        Component cancelShort = Component.translatable(cancelKey);

                        if (vars.reinforcement_target == 3) {
                            magicName = Component.translatable(
                                    "gui.typemoonworld.overlay.reinforcement.format.cancel",
                                    targetShort,
                                    cancelShort
                            );
                        } else if (vars.reinforcement_target == 2) {
                            magicName = Component.translatable(
                                    "gui.typemoonworld.overlay.reinforcement.format.item",
                                    targetShort,
                                    vars.reinforcement_level
                            );
                        } else {
                            magicName = Component.translatable(
                                    "gui.typemoonworld.overlay.reinforcement.format.part",
                                    targetShort,
                                    partShort,
                                    vars.reinforcement_level
                            );
                        }
                    }
                }

                Component labelStr = Component.translatable("gui.typemoonworld.overlay.current_magic");
                int magicTextX = barX;
                int magicTextY = barY - 12;

                event.getGuiGraphics().drawString(minecraft.font, labelStr, magicTextX, magicTextY, 0xFFFFFFFF, true);
                event.getGuiGraphics().drawString(
                        minecraft.font,
                        magicName,
                        magicTextX + minecraft.font.width(labelStr),
                        magicTextY,
                        magicColor,
                        true
                );
            }
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }
}
