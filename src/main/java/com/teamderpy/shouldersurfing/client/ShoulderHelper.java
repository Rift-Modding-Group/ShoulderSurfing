package com.teamderpy.shouldersurfing.client;

import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.teamderpy.shouldersurfing.config.Config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ShoulderHelper
{
	private static final Predicate<Entity> ENTITY_IS_PICKABLE = Predicates.and(EntitySelectors.NOT_SPECTATING, entity -> entity != null && entity.canBeCollidedWith());
	private static final ResourceLocation PULL_PROPERTY = new ResourceLocation("pull");
	private static final ResourceLocation THROWING_PROPERTY = new ResourceLocation("throwing");
	private static final ResourceLocation CHARGED_PROPERTY = new ResourceLocation("charged");
	
	public static ShoulderLook shoulderSurfingLook(Entity entity, float partialTicks, double distanceSq)
	{
		Vec3d cameraOffset = ShoulderHelper.calcCameraOffset(ShoulderRenderer.getInstance().getCameraDistance(), entity.rotationYaw, entity.rotationPitch);
		Vec3d headOffset = ShoulderHelper.calcRayTraceHeadOffset(cameraOffset);
		Vec3d cameraPos = entity.getPositionEyes(partialTicks).add(cameraOffset);
		Vec3d viewVector = entity.getLook(partialTicks);
		double length = headOffset.lengthVector(); //1.9 compatibility
		length *= length;
		
		if(Config.CLIENT.limitPlayerReach() && length < distanceSq)
		{
			distanceSq -= length;
		}
		
		double distance = Math.sqrt(distanceSq) + cameraOffset.distanceTo(headOffset);
		Vec3d traceEnd = cameraPos.add(viewVector.scale(distance));
		return new ShoulderLook(cameraPos, traceEnd, headOffset);
	}
	
	public static class ShoulderLook
	{
		private final Vec3d cameraPos;
		private final Vec3d traceEndPos;
		private final Vec3d headOffset;
		
		public ShoulderLook(Vec3d cameraPos, Vec3d traceEndPos, Vec3d headOffset)
		{
			this.cameraPos = cameraPos;
			this.traceEndPos = traceEndPos;
			this.headOffset = headOffset;
		}
		
		public Vec3d cameraPos()
		{
			return this.cameraPos;
		}
		
		public Vec3d traceEndPos()
		{
			return this.traceEndPos;
		}
		
		public Vec3d headOffset()
		{
			return this.headOffset;
		}
	}
	
	public static Vec3d calcCameraOffset(double distance, float yaw, float pitch)
	{
		return new Vec3d(Config.CLIENT.getOffsetX(), Config.CLIENT.getOffsetY(), -Config.CLIENT.getOffsetZ())
				.rotatePitch((float) Math.toRadians(-pitch))
				.rotateYaw((float) Math.toRadians(-yaw))
				.normalize()
				.scale(distance);
	}
	
	public static Vec3d calcRayTraceHeadOffset(Vec3d cameraOffset)
	{
		Vec3d view = Minecraft.getMinecraft().getRenderViewEntity().getLookVec();
		return ShoulderHelper.calcPlaneWithLineIntersection(Vec3d.ZERO, view, cameraOffset, view);
	}
	
	public static Vec3d calcPlaneWithLineIntersection(Vec3d planePoint, Vec3d planeNormal, Vec3d linePoint, Vec3d lineNormal)
	{
		double distance = (planeNormal.dotProduct(planePoint) - planeNormal.dotProduct(linePoint)) / planeNormal.dotProduct(lineNormal);
		return linePoint.add(lineNormal.scale(distance));
	}
	
	public static RayTraceResult traceBlocksAndEntities(Entity entity, PlayerControllerMP gameMode, double playerReachOverride, boolean stopOnFluid, float partialTick, boolean traceEntities, boolean shoulderSurfing)
	{
		double playerReach = Math.max(gameMode.getBlockReachDistance(), playerReachOverride);
		RayTraceResult blockHit = traceBlocks(entity, stopOnFluid, playerReach, partialTick, shoulderSurfing);
		
		if(!traceEntities)
		{
			return blockHit;
		}
		
		Vec3d eyePosition = entity.getPositionEyes(partialTick);
		
		if(gameMode.extendedReach())
		{
			playerReach = Math.max(playerReach, gameMode.getCurrentGameType().isCreative() ? 6.0D : 3.0D);
		}
		
		if(blockHit != null)
		{
			playerReach = blockHit.hitVec.distanceTo(eyePosition);
		}
		
		RayTraceResult entityHit = traceEntities(entity, playerReach, partialTick, shoulderSurfing);
		
		if(entityHit != null)
		{
			double distance = eyePosition.distanceTo(entityHit.hitVec);
			
			if(distance < playerReach || blockHit == null)
			{
				return entityHit;
			}
		}
		
		return blockHit;
	}
	
	public static RayTraceResult traceEntities(Entity cameraEntity, double playerReach, float partialTick, boolean shoulderSurfing)
	{
		double playerReachSq = playerReach * playerReach;
		Vec3d viewVector = cameraEntity.getLook(1.0F)
			.scale(playerReach);
		Vec3d eyePosition = cameraEntity.getPositionEyes(partialTick);
		double searchDistance = Math.min(64, playerReach);
		AxisAlignedBB aabb = cameraEntity.getEntityBoundingBox()
			.expand(viewVector.x * searchDistance, viewVector.y * searchDistance, viewVector.z * searchDistance)
			.grow(1.0D, 1.0D, 1.0D);
		Vec3d from;
		Vec3d to;
		
		if(shoulderSurfing)
		{
			ShoulderLook look = ShoulderHelper.shoulderSurfingLook(cameraEntity, partialTick, playerReachSq);
			from = eyePosition.add(look.headOffset());
			to = look.traceEndPos();
			aabb = aabb.offset(look.cameraPos().subtract(eyePosition));
		}
		else
		{
			from = eyePosition;
			to = from.add(viewVector);
		}
		
		List<Entity> entities = Minecraft.getMinecraft().world.getEntitiesInAABBexcluding(cameraEntity, aabb, ENTITY_IS_PICKABLE);
		Vec3d entityHitVec = null;
		Entity entityResult = null;
		double minEntityReachSq = playerReachSq;
		
		for(Entity entity : entities)
		{
			AxisAlignedBB axisalignedbb = entity.getEntityBoundingBox().grow(entity.getCollisionBorderSize());
			RayTraceResult raytraceresult = axisalignedbb.calculateIntercept(from, to);
			
			if(axisalignedbb.contains(eyePosition))
			{
				if(minEntityReachSq >= 0.0D)
				{
					entityResult = entity;
					entityHitVec = raytraceresult == null ? eyePosition : raytraceresult.hitVec;
					minEntityReachSq = 0.0D;
				}
			}
			else if(raytraceresult != null)
			{
				double distanceSq = eyePosition.squareDistanceTo(raytraceresult.hitVec);
				
				if(distanceSq < minEntityReachSq || minEntityReachSq == 0.0D)
				{
					if(entity == cameraEntity.getRidingEntity() && !entity.canRiderInteract())
					{
						if(minEntityReachSq == 0.0D)
						{
							entityResult = entity;
							entityHitVec = raytraceresult.hitVec;
						}
					}
					else
					{
						entityResult = entity;
						entityHitVec = raytraceresult.hitVec;
						minEntityReachSq = distanceSq;
					}
				}
			}
		}
		
		if(entityResult == null)
		{
			return null;
		}
		
		return new RayTraceResult(entityResult, entityHitVec);
	}
	
	public static RayTraceResult traceBlocks(Entity entity, boolean stopOnFluid, double distance, float partialTick, boolean shoulderSurfing)
	{
		Vec3d eyePosition = entity.getPositionEyes(partialTick);
		
		if(shoulderSurfing)
		{
			ShoulderLook look = ShoulderHelper.shoulderSurfingLook(entity, partialTick, distance * distance);
			Vec3d from = eyePosition.add(look.headOffset());
			Vec3d to = look.traceEndPos();
			return entity.world.rayTraceBlocks(from, to, stopOnFluid, true, true);
		}
		else
		{
			Vec3d from = eyePosition;
			Vec3d view = entity.getLook(partialTick);
			Vec3d to = from.add(view.scale(distance));
			return entity.world.rayTraceBlocks(from, to, stopOnFluid, true, true);
		}
	}
	
	public static boolean isHoldingSpecialItem()
	{
		final EntityPlayerSP player = Minecraft.getMinecraft().player;
		
		if(player != null)
		{
			List<String> overrides = Config.CLIENT.getAdaptiveCrosshairItems();
			ItemStack stack = player.getActiveItemStack();
			
			if(stack != null)
			{
				Item current = stack.getItem();
				
				if(current.getPropertyGetter(PULL_PROPERTY) != null || current.getPropertyGetter(THROWING_PROPERTY) != null)
				{
					return true;
				}
				else if(overrides.contains(current.getRegistryName().toString()))
				{
					return true;
				}
			}
			
			for(ItemStack item : player.getHeldEquipment())
			{
				if(item != null)
				{
					if(item.getItem().getPropertyGetter(CHARGED_PROPERTY) != null)
					{
						return true;
					}
					else if(overrides.contains(item.getItem().getRegistryName().toString()))
					{
						return true;
					}
				}
			}
		}
		
		return false;
	}
}
