package com.mrbysco.slabmachines.tileentity.furnace.compat;

import com.mrbysco.slabmachines.packets.SlabPacketHandler;
import com.mrbysco.slabmachines.packets.SlabPacketUpdateFurnaceMessage;
import com.mrbysco.slabmachines.tileentity.furnace.TileFurnaceSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry.ItemStackHolder;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Map.Entry;

public class TileFastFurnaceSlab extends TileFurnaceSlab{
	public static final int INPUT = 0;
	public static final int FUEL = 1;
	public static final int OUTPUT = 2;
	
	protected ItemStack recipeKey = ItemStack.EMPTY;

	@ItemStackHolder(value = "minecraft:sponge", meta = 1)
	public static final ItemStack WET_SPONGE = ItemStack.EMPTY;

	public TileFastFurnaceSlab() {
		this.totalCookTime = 200;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound = super.writeToNBT(compound);
		compound.setInteger("BurnTime", this.furnaceBurnTime);
		compound.setInteger("CookTime", this.cookTime);
		compound.setInteger("CookTimeTotal", this.totalCookTime);
		return compound;
	}

	@Override
	public void update() {
		if (world.isRemote && isBurning()) {
			furnaceBurnTime--;
			return;
		} else if (world.isRemote) return;

		ItemStack fuel = ItemStack.EMPTY;
		boolean canSmelt = canSmelt();


		if (!this.isBurning() && !(fuel = furnaceItemStacks.get(FUEL)).isEmpty()) {
			if (canSmelt) burnFuel(fuel, false);
		}

		boolean wasBurning = isBurning();

		if (this.isBurning()) {
			furnaceBurnTime--;
			if (canSmelt) smelt();
			else cookTime = 0;
		}

		if (!this.isBurning() && !(fuel = furnaceItemStacks.get(FUEL)).isEmpty()) {
			if (canSmelt) 
				burnFuel(fuel, wasBurning);
		}
		
		if (wasBurning && isBurning())
		{
			NetworkRegistry.TargetPoint target = new NetworkRegistry.TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 64);
			SlabPacketHandler.INSTANCE.sendToAllAround(new SlabPacketUpdateFurnaceMessage(this), target);
		}
	}
	
	protected void smelt() {
		cookTime++;
		if (this.cookTime == this.totalCookTime) {
			this.cookTime = 0;
			this.totalCookTime = this.getCookTime(this.furnaceItemStacks.get(INPUT));
			this.smeltItem();
		}
	}

	protected void burnFuel(ItemStack fuel, boolean burnedThisTick) {
		currentItemBurnTime = (furnaceBurnTime = getItemBurnTime(fuel));
		if (this.isBurning()) {
			Item item = fuel.getItem();
			fuel.shrink(1);
			if (fuel.isEmpty()) furnaceItemStacks.set(FUEL, item.getContainerItem(fuel));
			if (!burnedThisTick)
			{
				NetworkRegistry.TargetPoint target = new NetworkRegistry.TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 64);
				SlabPacketHandler.INSTANCE.sendToAllAround(new SlabPacketUpdateFurnaceMessage(this), target);
			}
		}
	}

	protected boolean canSmelt() {
		ItemStack input = furnaceItemStacks.get(INPUT);
		if (input.isEmpty()) return false;

		ItemStack recipeOutput = ItemStack.EMPTY;
		if (recipeKey.isEmpty() || !OreDictionary.itemMatches(recipeKey, input, false)) {
			boolean matched = false;
			for (Entry<ItemStack, ItemStack> e : FurnaceRecipes.instance().getSmeltingList().entrySet()) {
				if (OreDictionary.itemMatches(e.getKey(), input, false)) {
					recipeKey = e.getKey();
					recipeOutput = e.getValue();
					matched = true;
					break;
				}
			}
			if (!matched) {
				recipeKey = ItemStack.EMPTY;
				return false;
			}
		}

		return recipeOutput.isEmpty() || ItemHandlerHelper.canItemStacksStack(furnaceItemStacks.get(OUTPUT), recipeOutput);
	}

	@Override
	public void smeltItem() {
		ItemStack input = this.furnaceItemStacks.get(INPUT);
		ItemStack recipeOutput = FurnaceRecipes.instance().getSmeltingList().get(recipeKey);
		ItemStack output = this.furnaceItemStacks.get(OUTPUT);

		if (output.isEmpty()) this.furnaceItemStacks.set(OUTPUT, recipeOutput.copy());
		else if (ItemHandlerHelper.canItemStacksStack(output, recipeOutput)) output.grow(recipeOutput.getCount());

		if (input.isItemEqual(WET_SPONGE) && this.furnaceItemStacks.get(FUEL).getItem() == Items.BUCKET) this.furnaceItemStacks.set(FUEL, new ItemStack(Items.WATER_BUCKET));

		input.shrink(1);
	}

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
		return oldState.getBlock() != newState.getBlock();
	}
}
