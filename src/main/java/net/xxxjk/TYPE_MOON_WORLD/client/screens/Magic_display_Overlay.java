package net.xxxjk.TYPE_MOON_WORLD.client.screens;

import com.example.typemoonaddon.data.ImaginarySpaceData;
import com.example.typemoonaddon.registry.AddonAttachments;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.xxxjk.TYPE_MOON_WORLD.client.ReplayUiSuppressor;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.GuiUtils;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.MagicUiColors;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.talent.TalentService;

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
        int guiWidth = event.getGuiGraphics().guiWidth();
        Player entity = minecraft.player;
        if (entity == null) return;
        TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (vars.servant_card_transformed) return;
        String selectedId = vars.current_magic_index >= 0 && vars.current_magic_index < vars.selected_magics.size()
                ? vars.selected_magics.get(vars.current_magic_index) : "";
        boolean selectedTalent = TalentService.isTalent(selectedId) && TalentService.owns(vars, selectedId);
        if (!vars.is_magus && !selectedTalent) return;

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

            if (vars.is_magus) {
                GuiUtils.renderHudPanel(event.getGuiGraphics(), barX - 3, barY - 3, barWidth + 6, barHeight + 6, GuiUtils.ARCANE_CYAN);
                event.getGuiGraphics().fill(barX, barY, barX + barWidth, barY + barHeight, GuiUtils.ARCANE_BACKGROUND);

                int startColor = 0xFF008F99;
                int endColor = 0xFF205EAA;
                if (currentMana <= maxMana * 0.2) {
                    startColor = 0xFFB33600;
                    endColor = 0xFFB00000;
                } else if (currentMana > maxMana) {
                    startColor = 0xFFB000B0;
                    endColor = 0xFF7200B8;
                }

                if (maxMana > 0) {
                    double ratio = Math.min(1.0, Math.max(0.0, currentMana / maxMana));
                    int fillWidth = (int) (barWidth * ratio);
                    event.getGuiGraphics().fillGradient(barX, barY, barX + fillWidth, barY + barHeight, startColor, endColor);
                }

                event.getGuiGraphics().renderOutline(barX - 1, barY - 1, barWidth + 2, barHeight + 2, GuiUtils.ARCANE_BORDER);

                String manaText = (int) currentMana + " / " + (int) maxMana;
                int textWidth = minecraft.font.width(manaText);
                int textX = barX + (barWidth - textWidth) / 2;
                int textY = barY + (barHeight - 8) / 2 + 1;
                event.getGuiGraphics().drawString(minecraft.font, manaText, textX, textY, 0xFFFFFFFF, true);

                int iconX = barX - 4;
                int iconY = barY - 3;
                event.getGuiGraphics().blit(MANA_ICON, iconX, iconY, 0, 0, 16, 16, 16, 16);
            }

            if (selectedTalent || vars.is_magus && vars.is_magic_circuit_open) {
                MutableComponent magicName = Component.translatable("gui.typemoonworld.mode.none");
                int magicColor = 0xFF00FFFF;

                if (!vars.selected_magics.isEmpty() && vars.current_magic_index >= 0 && vars.current_magic_index < vars.selected_magics.size()) {
                    String magicId = vars.selected_magics.get(vars.current_magic_index);
                    MutableComponent baseMagicName = resolveMagicDisplayName(vars, vars.current_magic_index, magicId);
                    magicName = baseMagicName.copy();

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
                                baseMagicName,
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
                                baseMagicName,
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
                                baseMagicName,
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
                                baseMagicName,
                                Component.translatable(vars.time_alter_mode == 0
                                        ? "gui.typemoonworld.overlay.time_alter.mode.accel.short"
                                        : "gui.typemoonworld.overlay.time_alter.mode.stagnate.short")
                        );
                    } else if ("spiritual_healing".equals(magicId)) {
                        magicColor = 0xFFF8F3E7;
                    } else if ("baptism_rite".equals(magicId)) {
                        magicColor = 0xFFFFD24A;
                    } else if (PlayerMagicSelectionService.isElementalMagic(magicId)) {
                        int mode = PlayerMagicSelectionService.getElementMode(vars, magicId);
                        magicColor = switch (magicId) {
                            case "fire_magic", "flame_array" -> 0xFFFF6633;
                            case "water_magic", "azure_water_array" -> 0xFF66CCFF;
                            case "wind_magic", "gale_wind_array" -> 0xFF99FFCC;
                            case "earth_magic", "rock_earth_array" -> 0xFFCCAA66;
                            default -> 0xFFFFFFFF;
                        };
                        magicName = Component.translatable(
                                "gui.typemoonworld.overlay.element.format",
                                baseMagicName,
                                Component.translatable(mode == 1
                                        ? "gui.typemoonworld.overlay.element.mode.utility.short"
                                        : "gui.typemoonworld.overlay.element.mode.attack.short")
                        );
                    } else if (isImaginaryStorageMagic(magicId)) {
                        magicColor = 0xFF66DDAA;
                        String mode = currentImaginaryMode(entity, vars, magicId);
                        magicName = Component.translatable(
                                "gui.typemoonworld.overlay.imaginary.format",
                                baseMagicName,
                                Component.translatable(imaginaryModeLabelKey(magicId, mode))
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

                    magicColor = MagicUiColors.colorFor(magicId, vars.isCurrentSelectionFromCrest(magicId));
                }

                Component labelStr = Component.translatable(TalentService.isTalent(selectedId)
                        ? "gui.typemoonworld.overlay.current_talent"
                        : "gui.typemoonworld.overlay.current_magic");
                int magicTextX = barX;
                int magicTextY = vars.is_magus ? barY - 12 : h - 20;
                int maxNameWidth = Math.max(24, guiWidth - magicTextX - minecraft.font.width(labelStr) - 14);
                String clippedName = minecraft.font.plainSubstrByWidth(magicName.getString(), maxNameWidth);
                Component displayMagicName = Component.literal(clippedName);
                int magicPanelWidth = Math.min(guiWidth - magicTextX - 6, minecraft.font.width(labelStr) + minecraft.font.width(displayMagicName) + 8);
                GuiUtils.renderHudPanel(event.getGuiGraphics(), magicTextX - 3, magicTextY - 2, magicPanelWidth, 12, magicColor);

                event.getGuiGraphics().drawString(minecraft.font, labelStr, magicTextX, magicTextY, GuiUtils.ARCANE_TEXT_MUTED, true);
                event.getGuiGraphics().drawString(
                        minecraft.font,
                        displayMagicName,
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

    private static boolean isImaginaryStorageMagic(String magicId) {
        String path = magicPath(magicId);
        return "imaginary_absorption".equals(path) || "imaginary_absorption_evolved".equals(path);
    }

    /** Uses the same display-name precedence as the radial selector. */
    private static MutableComponent resolveMagicDisplayName(TypeMoonWorldModVariables.PlayerVariables vars, int index, String magicId) {
        if (vars != null && index >= 0 && index < vars.selected_magic_display_names.size()) {
            String cached = vars.selected_magic_display_names.get(index);
            if (cached != null && !cached.isBlank() && !cached.equals(magicId) && !cached.contains(".typemoonworld.")) {
                return Component.literal(cached);
            }
        }

        String path = magicPath(magicId);
        String selectedKey = "key.typemoonworld.magic." + path + ".selected";
        MutableComponent selected = Component.translatable(selectedKey);
        if (!selected.getString().equals(selectedKey)) return selected;

        String nameKey = "magic.typemoonworld." + path + ".name";
        MutableComponent name = Component.translatable(nameKey);
        if (!name.getString().equals(nameKey)) return name;

        String shortKey = "key.typemoonworld.magic." + path + ".short";
        MutableComponent shortName = Component.translatable(shortKey);
        if (!shortName.getString().equals(shortKey)) return shortName;
        return Component.literal(path.replace('_', ' '));
    }

    private static String currentImaginaryMode(Player player, TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
        TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = vars.getCurrentRuntimeWheelEntry();
        if (entry != null && sameMagicPath(entry.magicId, magicId) && entry.presetPayload != null && entry.presetPayload.contains("imaginary_mode")) {
            return entry.presetPayload.getString("imaginary_mode");
        }

        ImaginarySpaceData data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        return data.magicMode() == ImaginarySpaceData.MagicMode.PROTECTION ? "protection" : "storage";
    }

    private static String imaginaryModeLabelKey(String magicId, String mode) {
        if ("protection".equals(mode)) {
            return "gui.typemoonworld.overlay.imaginary.mode.protection.short";
        }
        return "imaginary_absorption_evolved".equals(magicPath(magicId))
                ? "gui.typemoonworld.overlay.imaginary.mode.absorption.short"
                : "gui.typemoonworld.overlay.imaginary.mode.storage.short";
    }

    private static boolean sameMagicPath(String left, String right) {
        return magicPath(left).equals(magicPath(right));
    }

    private static String magicPath(String magicId) {
        if (magicId == null) {
            return "";
        }
        int split = magicId.indexOf(':');
        return split >= 0 ? magicId.substring(split + 1) : magicId;
    }
}
