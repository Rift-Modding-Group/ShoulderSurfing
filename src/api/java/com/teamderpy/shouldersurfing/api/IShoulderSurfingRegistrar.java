package com.teamderpy.shouldersurfing.api;

import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import com.teamderpy.shouldersurfing.api.callback.IAdaptiveItemCallback;

import net.minecraft.item.ItemStack;

public interface IShoulderSurfingRegistrar
{
	IShoulderSurfingRegistrar registerAdaptiveItemCallback(IAdaptiveItemCallback adaptiveItemCallback);
	
	default IShoulderSurfingRegistrar registerAdaptiveItemCallback(Predicate<ItemStack> predicate)
	{
		return this.registerAdaptiveItemCallback((minecraft, entity) ->
		{
			return StreamSupport.stream(entity.getHeldEquipment().spliterator(), false)
					.anyMatch(stack -> stack != null && predicate.test(stack));
		});
	}
}
