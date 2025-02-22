package com.teamderpy.shouldersurfing.event;

import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import com.teamderpy.shouldersurfing.client.KeyHandler;
import com.teamderpy.shouldersurfing.client.ShoulderInstance;
import com.teamderpy.shouldersurfing.client.ShoulderRenderer;
import com.teamderpy.shouldersurfing.config.Config;
import com.teamderpy.shouldersurfing.config.Perspective;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

@SideOnly(Side.CLIENT)
public class ClientEventHandler
{
	@SubscribeEvent
	public void clientTickEvent(ClientTickEvent event)
	{
		if(event.phase.equals(Phase.START)) ShoulderInstance.getInstance().tick();
		if (event.phase == TickEvent.Phase.END && Minecraft.getMinecraft().world != null && Minecraft.getMinecraft().currentScreen == null) {
			boolean rightButtonDown = Mouse.isButtonDown(1);
			if (!rightButtonDown && !ShoulderInstance.getInstance().canUseMountAim() && Minecraft.getMinecraft().getRenderViewEntity().isRiding()) {
				ShoulderInstance.getInstance().setCanUseMountAim(true);
			}
			if (!Minecraft.getMinecraft().getRenderViewEntity().isRiding()) ShoulderInstance.getInstance().setCanUseMountAim(false);
		}
	}
	
	@SubscribeEvent
	public void preRenderLivingEntityEvent(RenderLivingEvent.Pre<?> event)
	{
		if(event.isCancelable() && event.getEntity().equals(Minecraft.getMinecraft().getRenderViewEntity()) && Minecraft.getMinecraft().currentScreen == null && ShoulderRenderer.getInstance().skipEntityRendering())
		{
			event.setCanceled(true);
		}
	}
	
	@SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
	public void preRenderGameOverlayEvent(RenderGameOverlayEvent.Pre event)
	{
		boolean doRender = Config.CLIENT.getCrosshairVisibility(Perspective.current()).doRender(Minecraft.getMinecraft().objectMouseOver, ShoulderInstance.getInstance().isAiming());
		
		if(event.getType().equals(RenderGameOverlayEvent.ElementType.CROSSHAIRS))
		{
			if(doRender)
			{
				ShoulderRenderer.getInstance().offsetCrosshair(event.getResolution(), event.getPartialTicks());
			}
			else
			{
				event.setCanceled(true);
			}
		}
		//Using BOSSHEALTH to pop matrix because when CROSSHAIRS is cancelled it will not fire RenderGameOverlayEvent#Post and cause a stack overflow
		else if(doRender && event.getType().equals(RenderGameOverlayEvent.ElementType.BOSSHEALTH))
		{
			ShoulderRenderer.getInstance().clearCrosshairOffset();
		}
	}
	
	@SubscribeEvent
	public void renderWorldLast(RenderWorldLastEvent event)
	{
		ShoulderRenderer.getInstance().updateDynamicRaytrace(event.getPartialTicks());
	}
	
	@SubscribeEvent
	public void keyInputEvent(InputEvent event)
	{
		KeyHandler.onInput();
	}
	
	@SubscribeEvent
	public void onConfigChanged(OnConfigChangedEvent event)
	{
		Config.CLIENT.sync();
	}
	
	@SubscribeEvent
	public void onGuiClosed(GuiOpenEvent event)
	{
		if(event.getGui() == null)
		{
			Keyboard.enableRepeatEvents(true);
		}
	}
}
