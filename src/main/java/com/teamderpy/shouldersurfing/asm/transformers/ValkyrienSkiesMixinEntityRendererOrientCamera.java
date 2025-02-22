package com.teamderpy.shouldersurfing.asm.transformers;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.teamderpy.shouldersurfing.asm.Mappings;
import com.teamderpy.shouldersurfing.asm.ShoulderTransformer;

public class ValkyrienSkiesMixinEntityRendererOrientCamera extends ShoulderTransformer
{
	@Override
	protected InsnList searchList(Mappings mappings, boolean obf)
	{
		InsnList searchList = new InsnList();
		searchList.add(new LdcInsnNode(15D));
		return searchList;
	}
	
	@Override
	protected void transform(Mappings mappings, boolean obf, MethodNode method, int offset)
	{
		// d3 = 15D;
		// ->
		// d3 = InjectionDelegation.ValkyrienSkiesMixinEntityRenderer_orientCamera_cameraDistance();
		
		AbstractInsnNode instruction = method.instructions.get(offset);
		method.instructions.set(instruction, new MethodInsnNode(INVOKESTATIC, "com/teamderpy/shouldersurfing/asm/InjectionDelegation", "ValkyrienSkiesMixinEntityRenderer_orientCamera_cameraDistance", "()D", false));
	}
	
	@Override
	public String getClassId()
	{
		return "ValkyrienSkiesMixinEntityRenderer";
	}
	
	@Override
	public String getMethodId()
	{
		return "ValkyrienSkiesMixinEntityRenderer#orientCamera";
	}
	
	@Override
	protected boolean hasMethodTransformer()
	{
		return true;
	}
	
	@Override
	protected boolean hasClassTransformer()
	{
		return false;
	}
}
