package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicLearningStrategy;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicCopyMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.MagicCopyingTableMenu;

public class MagicCopyingTableScreen extends AbstractContainerScreen<MagicCopyingTableMenu>{private final List<String> magics=new ArrayList<>();private int cursor;public MagicCopyingTableScreen(MagicCopyingTableMenu m,Inventory i,Component t){super(m,i,t);imageWidth=220;imageHeight=220;}@Override protected void init(){super.init();var v=minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);magics.addAll(v.learned_magics.stream().filter(MagicLearningStrategy::canCopy).sorted().toList());addRenderableWidget(Button.builder(Component.literal("<"),b->cycle(-1)).bounds(leftPos+75,topPos+40,20,18).build());addRenderableWidget(Button.builder(Component.literal(">"),b->cycle(1)).bounds(leftPos+180,topPos+40,20,18).build());addRenderableWidget(Button.builder(Component.translatable("gui.typemoonworld.magic_copying_table.copy"),b->{if(!magics.isEmpty())PacketDistributor.sendToServer(new MagicCopyMessage(magics.get(cursor)));}).bounds(leftPos+75,topPos+78,125,20).build());}private void cycle(int d){if(!magics.isEmpty())cursor=Math.floorMod(cursor+d,magics.size());}@Override protected void renderBg(GuiGraphics g,float p,int x,int y){g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xEE111820);g.renderOutline(leftPos,topPos,imageWidth,imageHeight,GuiUtils.ARCANE_CYAN);}@Override public void render(GuiGraphics g,int x,int y,float p){super.render(g,x,y,p);if(!magics.isEmpty())g.drawString(font,Component.translatable("magic.typemoonworld."+magics.get(cursor)+".name"),leftPos+100,topPos+45,0xFFFFFF,false);renderTooltip(g,x,y);}}
