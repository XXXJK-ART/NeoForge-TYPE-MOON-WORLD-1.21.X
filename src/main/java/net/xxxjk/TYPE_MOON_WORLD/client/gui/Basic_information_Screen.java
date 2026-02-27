package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.Basic_information_Button_Message;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.procedures.Basic_information_back_player_self;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.BasicInformationMenu;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@SuppressWarnings({"null", "unused"})
public class Basic_information_Screen extends AbstractContainerScreen<BasicInformationMenu> {
    private static final HashMap<String, Object> guistate = BasicInformationMenu.guistate;
    private static final double STAT_EPSILON = 1.0E-4;
    private static final int STAT_ROW_HOVER_WIDTH = 220;

    private final Level world;
    private final int x;
    private final int y;
    private final int z;
    private final Player entity;

    private final List<StatTooltipArea> statTooltipAreas = new ArrayList<>();

    Button imagebutton_basic_attributes;
    Button imagebutton_magical_attributes;
    Button imagebutton_magical_properties;

    public Basic_information_Screen(BasicInformationMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.world = container.world;
        this.x = container.x;
        this.y = container.y;
        this.z = container.z;
        this.entity = container.entity;
        this.imageWidth = 360;
        this.imageHeight = 200;
    }

    private static final ResourceLocation texture = ResourceLocation.parse("typemoonworld:textures/screens/basic_information.png");

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        if (Basic_information_back_player_self.execute(entity) instanceof LivingEntity livingEntity) {
            this.renderEntityInInventoryFollowsAngle(guiGraphics, this.leftPos + 60, this.topPos + 170,
                    (float) Math.atan((this.leftPos + 60 - mouseX) / 40.0),
                    (float) Math.atan((this.topPos + 87 - mouseY) / 40.0),
                    livingEntity);
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        this.renderStatModifierTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;

        GuiUtils.renderBackground(guiGraphics, x, y, w, h);
        GuiUtils.renderTechFrame(guiGraphics, x + 10, y + 35, 100, 150, 0xFF00AAAA, 0xFF00FFFF);

        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int startX = 125;
        int startY = 40;
        int spacing = 15;

        statTooltipAreas.clear();
        TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);

        double currentMana = vars.player_mana;
        double maxMana = vars.player_max_mana;
        double baseCurrentMana = Math.min(currentMana, maxMana);
        List<ModifierReason> currentManaReasons = new ArrayList<>();
        if (currentMana > maxMana + STAT_EPSILON) {
            currentManaReasons.add(new ModifierReason(
                    Component.translatable("gui.typemoonworld.basic_info.mod.reason.mana_overload"),
                    currentMana - maxMana
            ));
        }
        drawStatWithModifiers(
                guiGraphics,
                Component.translatable("gui.typemoonworld.basic_info.current_mana"),
                baseCurrentMana,
                currentManaReasons,
                startX,
                startY,
                0,
                "gui.typemoonworld.basic_info.unit.mana",
                true
        );

        drawStatWithModifiers(
                guiGraphics,
                Component.translatable("gui.typemoonworld.basic_info.max_mana"),
                maxMana,
                List.of(),
                startX,
                startY + spacing,
                0,
                "gui.typemoonworld.basic_info.unit.mana",
                true
        );

        double baseRegen = vars.player_mana_egenerated_every_moment;
        double regenMultiplier = vars.current_mana_regen_multiplier <= 0.0 ? 1.0 : vars.current_mana_regen_multiplier;
        List<ModifierReason> regenReasons = new ArrayList<>();
        double leylineDelta = baseRegen * regenMultiplier - baseRegen;
        if (Math.abs(leylineDelta) > STAT_EPSILON) {
            regenReasons.add(new ModifierReason(
                    Component.translatable(
                            "gui.typemoonworld.basic_info.mod.reason.leyline",
                            String.format(Locale.ROOT, "%.2fx", regenMultiplier)
                    ),
                    leylineDelta
            ));
        }
        drawStatWithModifiers(
                guiGraphics,
                Component.translatable("gui.typemoonworld.basic_info.mana_regen"),
                baseRegen,
                regenReasons,
                startX,
                startY + spacing * 2,
                1,
                "gui.typemoonworld.basic_info.unit.mana_per_cycle",
                true
        );

        double baseIntervalTicks = vars.player_restore_magic_moment;
        double effectiveIntervalTicks = baseIntervalTicks;
        List<ModifierReason> intervalReasons = new ArrayList<>();
        if (vars.is_magic_circuit_open) {
            double halvedTicks = baseIntervalTicks / 2.0;
            double halvedDeltaSec = (halvedTicks - baseIntervalTicks) / 20.0;
            if (Math.abs(halvedDeltaSec) > STAT_EPSILON) {
                intervalReasons.add(new ModifierReason(
                        Component.translatable("gui.typemoonworld.basic_info.mod.reason.magic_circuit"),
                        halvedDeltaSec
                ));
            }
            effectiveIntervalTicks = halvedTicks;
            if (effectiveIntervalTicks < 1.0) {
                double beforeFloor = effectiveIntervalTicks;
                effectiveIntervalTicks = 1.0;
                double floorDeltaSec = (effectiveIntervalTicks - beforeFloor) / 20.0;
                if (Math.abs(floorDeltaSec) > STAT_EPSILON) {
                    intervalReasons.add(new ModifierReason(
                            Component.translatable("gui.typemoonworld.basic_info.mod.reason.interval_floor"),
                            floorDeltaSec
                    ));
                }
            }
        }
        drawStatWithModifiers(
                guiGraphics,
                Component.translatable("gui.typemoonworld.basic_info.regen_interval"),
                baseIntervalTicks / 20.0,
                intervalReasons,
                startX,
                startY + spacing * 3,
                2,
                "gui.typemoonworld.basic_info.unit.second",
                false
        );

        int attrY = startY + spacing * 5;

        Component baseLabel = Component.translatable("gui.typemoonworld.basic_info.base_attributes");
        guiGraphics.drawString(this.font, baseLabel, startX, attrY, 0xFF00E0E0, false);

        Component baseAttr = buildBaseAttributes(vars);
        guiGraphics.drawWordWrap(this.font, baseAttr, startX, attrY + spacing, 160, 0xFFCCCCCC);

        Component extraLabel = Component.translatable("gui.typemoonworld.basic_info.extra_attributes");
        guiGraphics.drawString(this.font, extraLabel, startX, attrY + spacing * 3, 0xFF00E0E0, false);

        Component extraAttr = buildExtraAttributes(vars);
        guiGraphics.drawWordWrap(this.font, extraAttr, startX, attrY + spacing * 4, 160, 0xFFCCCCCC);
    }

    private void drawStatWithModifiers(
            GuiGraphics gui,
            Component label,
            double baseValue,
            List<ModifierReason> reasons,
            int x,
            int y,
            int decimals,
            String unitKey,
            boolean positiveIsBeneficial
    ) {
        gui.drawString(this.font, label, x, y, 0xFFAAAAAA, false);

        double totalModifier = reasons.stream().mapToDouble(ModifierReason::delta).sum();
        String unitText = Component.translatable(unitKey).getString();
        String baseText = formatNumber(baseValue, decimals);
        String mainText = baseText + " " + unitText;

        int valueX = x + Math.max(62, this.font.width(label) + 6);
        gui.drawString(this.font, mainText, valueX, y, 0xFFFFFFFF, false);

        if (Math.abs(totalModifier) <= STAT_EPSILON) {
            return;
        }

        String modifierText = "(" + formatSignedNumber(totalModifier, decimals) + ")";
        boolean isBuff = positiveIsBeneficial ? totalModifier >= 0.0 : totalModifier <= 0.0;
        int modifierColor = isBuff ? 0xFF55FF55 : 0xFFFF7777;
        int modifierX = valueX + this.font.width(mainText) + 4;
        gui.drawString(this.font, modifierText, modifierX, y, modifierColor, false);

        statTooltipAreas.add(new StatTooltipArea(
                this.leftPos + x,
                this.topPos + y - 1,
                STAT_ROW_HOVER_WIDTH,
                this.font.lineHeight + 3,
                buildModifierTooltip(label, baseValue, totalModifier, decimals, unitKey, reasons, positiveIsBeneficial)
        ));
    }

    private List<Component> buildModifierTooltip(
            Component label,
            double baseValue,
            double totalModifier,
            int decimals,
            String unitKey,
            List<ModifierReason> reasons,
            boolean positiveIsBeneficial
    ) {
        List<Component> lines = new ArrayList<>();
        Component unit = Component.translatable(unitKey);
        String unitText = unit.getString();

        lines.add(label.copy().withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable(
                "gui.typemoonworld.basic_info.tooltip.base",
                formatNumber(baseValue, decimals),
                unit
        ).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(
                "gui.typemoonworld.basic_info.tooltip.total_modifier",
                formatSignedNumber(totalModifier, decimals),
                unit
        ).withStyle((positiveIsBeneficial ? totalModifier >= 0.0 : totalModifier <= 0.0) ? ChatFormatting.GREEN : ChatFormatting.RED));
        lines.add(Component.translatable(
                "gui.typemoonworld.basic_info.tooltip.effective",
                formatNumber(baseValue + totalModifier, decimals),
                unit
        ).withStyle(ChatFormatting.WHITE));

        if (!reasons.isEmpty()) {
            lines.add(Component.empty());
            lines.add(Component.translatable("gui.typemoonworld.basic_info.tooltip.details").withStyle(ChatFormatting.GOLD));
            for (ModifierReason reason : reasons) {
                String detail = String.format(
                        Locale.ROOT,
                        "- %s (%s %s)",
                        reason.label().getString(),
                        formatSignedNumber(reason.delta(), decimals),
                        unitText
                );
                lines.add(Component.literal(detail).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        return lines;
    }

    private void renderStatModifierTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (StatTooltipArea area : statTooltipAreas) {
            if (mouseX >= area.x() && mouseX < area.x() + area.width()
                    && mouseY >= area.y() && mouseY < area.y() + area.height()) {
                renderSimpleTooltip(guiGraphics, area.lines(), mouseX, mouseY);
                return;
            }
        }
    }

    private void renderSimpleTooltip(GuiGraphics guiGraphics, List<Component> lines, int mouseX, int mouseY) {
        if (lines.isEmpty()) {
            return;
        }

        int padding = 5;
        int lineHeight = 10;
        int tooltipWidth = 0;
        for (Component line : lines) {
            tooltipWidth = Math.max(tooltipWidth, this.font.width(line));
        }
        tooltipWidth += padding * 2;
        int tooltipHeight = (lines.size() * lineHeight) + padding * 2;

        int tooltipX = mouseX + 12;
        int tooltipY = mouseY - 12;
        if (tooltipX + tooltipWidth > this.width - 4) {
            tooltipX = this.width - tooltipWidth - 4;
        }
        if (tooltipX < 4) {
            tooltipX = 4;
        }
        if (tooltipY + tooltipHeight > this.height - 4) {
            tooltipY = this.height - tooltipHeight - 4;
        }
        if (tooltipY < 4) {
            tooltipY = 4;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 400);
        guiGraphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xF0100010);
        guiGraphics.renderOutline(tooltipX, tooltipY, tooltipWidth, tooltipHeight, 0xFF00AAAA);

        int textY = tooltipY + padding;
        for (Component line : lines) {
            guiGraphics.drawString(this.font, line, tooltipX + padding, textY, 0xFFFFFFFF, false);
            textY += lineHeight;
        }
        guiGraphics.pose().popPose();
    }

    private String formatNumber(double value, int decimals) {
        if (decimals <= 0) {
            return String.format(Locale.ROOT, "%d", Math.round(value));
        }
        return String.format(Locale.ROOT, "%1$." + decimals + "f", value);
    }

    private String formatSignedNumber(double value, int decimals) {
        if (decimals <= 0) {
            return String.format(Locale.ROOT, "%+d", Math.round(value));
        }
        return String.format(Locale.ROOT, "%1$+." + decimals + "f", value);
    }

    @Override
    public void init() {
        super.init();

        int btnY = this.topPos + 5;
        int btnWidth = 80;
        int btnHeight = 16;
        int startX = this.leftPos + this.imageWidth - (btnWidth * 3) - 10;

        imagebutton_basic_attributes = new NeonButton(startX, btnY, btnWidth, btnHeight, Component.translatable("gui.typemoonworld.tab.basic_attributes"), e -> {
            // Already here
        });
        this.addRenderableWidget(imagebutton_basic_attributes);

        imagebutton_magical_attributes = new NeonButton(startX + btnWidth + 2, btnY, btnWidth, btnHeight, Component.translatable("gui.typemoonworld.tab.body_modification"), e -> {
            PacketDistributor.sendToServer(new Basic_information_Button_Message(0, x, y, z));
            Basic_information_Button_Message.handleButtonAction(entity, 0, x, y, z);
        });
        this.addRenderableWidget(imagebutton_magical_attributes);

        imagebutton_magical_properties = new NeonButton(startX + (btnWidth + 2) * 2, btnY, btnWidth, btnHeight, Component.translatable("gui.typemoonworld.tab.magic_knowledge"), e -> {
            PacketDistributor.sendToServer(new Basic_information_Button_Message(1, x, y, z));
            Basic_information_Button_Message.handleButtonAction(entity, 1, x, y, z);
        });
        this.addRenderableWidget(imagebutton_magical_properties);
    }

    private Component buildBaseAttributes(TypeMoonWorldModVariables.PlayerVariables vars) {
        net.minecraft.network.chat.MutableComponent builder = Component.empty();
        boolean has = false;
        if (vars.player_magic_attributes_earth) {
            builder.append(Component.translatable("attribute.typemoonworld.earth"));
            has = true;
        }
        if (vars.player_magic_attributes_water) {
            if (has) builder.append(Component.literal(" / "));
            builder.append(Component.translatable("attribute.typemoonworld.water"));
            has = true;
        }
        if (vars.player_magic_attributes_fire) {
            if (has) builder.append(Component.literal(" / "));
            builder.append(Component.translatable("attribute.typemoonworld.fire"));
            has = true;
        }
        if (vars.player_magic_attributes_wind) {
            if (has) builder.append(Component.literal(" / "));
            builder.append(Component.translatable("attribute.typemoonworld.wind"));
            has = true;
        }
        if (vars.player_magic_attributes_ether) {
            if (has) builder.append(Component.literal(" / "));
            builder.append(Component.translatable("attribute.typemoonworld.ether"));
            has = true;
        }
        if (!has) {
            builder.append(Component.literal("/"));
        }
        return builder;
    }

    private Component buildExtraAttributes(TypeMoonWorldModVariables.PlayerVariables vars) {
        net.minecraft.network.chat.MutableComponent builder = Component.empty();
        boolean has = false;
        if (vars.player_magic_attributes_none) {
            builder.append(Component.translatable("attribute.typemoonworld.none"));
            has = true;
        }
        if (vars.player_magic_attributes_imaginary_number) {
            if (has) builder.append(Component.literal(" / "));
            builder.append(Component.translatable("attribute.typemoonworld.imaginary"));
            has = true;
        }
        if (vars.player_magic_attributes_sword) {
            if (has) builder.append(Component.literal(" / "));
            builder.append(Component.translatable("attribute.typemoonworld.sword"));
            has = true;
        }
        if (!has) {
            builder.append(Component.literal("/"));
        }
        return builder;
    }

    private void renderEntityInInventoryFollowsAngle(GuiGraphics guiGraphics, int x, int y,
                                                     float angleXComponent, float angleYComponent, LivingEntity entity) {
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf cameraOrientation = new Quaternionf().rotateX(angleYComponent * 20 * ((float) Math.PI / 180F));
        pose.mul(cameraOrientation);
        float f2 = entity.yBodyRot;
        float f3 = entity.getYRot();
        float f4 = entity.getXRot();
        float f5 = entity.yHeadRotO;
        float f6 = entity.yHeadRot;
        entity.yBodyRot = 180.0F + angleXComponent * 20.0F;
        entity.setYRot(180.0F + angleXComponent * 40.0F);
        entity.setXRot(-angleYComponent * 20.0F);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();
        InventoryScreen.renderEntityInInventory(guiGraphics, x, y, 55, new Vector3f(0, 0, 0), pose, cameraOrientation, entity);
        entity.yBodyRot = f2;
        entity.setYRot(f3);
        entity.setXRot(f4);
        entity.yHeadRotO = f5;
        entity.yHeadRot = f6;
    }

    public Level getWorld() {
        return world;
    }

    private record ModifierReason(Component label, double delta) {
    }

    private record StatTooltipArea(int x, int y, int width, int height, List<Component> lines) {
    }
}
