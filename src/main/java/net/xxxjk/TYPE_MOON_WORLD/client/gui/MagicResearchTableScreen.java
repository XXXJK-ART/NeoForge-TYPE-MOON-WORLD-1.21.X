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
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicResearchMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.MagicResearchTableMenu;

public class MagicResearchTableScreen extends AbstractContainerScreen<MagicResearchTableMenu> {
   private final List<String> magics=new ArrayList<>(); private int cursor;
   public MagicResearchTableScreen(MagicResearchTableMenu m,Inventory i,Component t){super(m,i,t);imageWidth=220;imageHeight=220;}
   @Override protected void init(){super.init();var v=minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);magics.addAll(v.learned_magics.stream().filter(MagicLearningStrategy::canResearch).sorted().toList());cursor=0;addRenderableWidget(Button.builder(Component.literal("<"),b->cycle(-1)).bounds(leftPos+75,topPos+40,20,18).build());addRenderableWidget(Button.builder(Component.literal(">"),b->cycle(1)).bounds(leftPos+180,topPos+40,20,18).build());addRenderableWidget(Button.builder(Component.translatable("gui.typemoonworld.magic_research_table.research"),b->{if(!magics.isEmpty())PacketDistributor.sendToServer(new MagicResearchMessage(magics.get(cursor)));}).bounds(leftPos+75,topPos+78,125,20).build());}
   private void cycle(int d){if(!magics.isEmpty())cursor=Math.floorMod(cursor+d,magics.size());}
   @Override protected void renderBg(GuiGraphics g,float p,int x,int y){g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xEE111820);g.renderOutline(leftPos,topPos,imageWidth,imageHeight,GuiUtils.ARCANE_CYAN);}
   @Override public void render(GuiGraphics g,int x,int y,float p){super.render(g,x,y,p);if(!magics.isEmpty()){String id=magics.get(cursor);var v=minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);double proficiency=MagicProficiencyService.get(v,id);g.drawString(font,Component.translatable("magic.typemoonworld."+id+".name"),leftPos+100,topPos+45,0xFFFFFF,false);g.drawString(font,"Proficiency: "+proficiency,leftPos+75,topPos+62,0xA9DCE8,false);g.drawString(font,"C:"+MagicLearningStrategy.complexity(id)+"  T:"+MagicLearningStrategy.researchTicks(id,proficiency)+"  MP:"+MagicLearningStrategy.researchManaCost(id,proficiency),leftPos+75,topPos+105,0xA9DCE8,false);}renderTooltip(g,x,y);}
}
