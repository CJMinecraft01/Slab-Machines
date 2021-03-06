package com.mrbysco.slabmachines.gui.compat.tcon;

import com.mrbysco.slabmachines.tileentity.compat.tinkers.TileStencilTableSlab;
import com.mrbysco.slabmachines.utils.SlabUtil;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.ObjectUtils;
import slimeknights.tconstruct.tools.common.inventory.ContainerStencilTable;
import slimeknights.tconstruct.tools.common.tileentity.TileStencilTable;

public class ContainerStencilTableSlab extends ContainerStencilTable{

	public ContainerStencilTableSlab(InventoryPlayer playerInventory, TileStencilTable tile) {
		super(playerInventory, tile);
	}
	
	@Override
	public <TE extends TileEntity> TE detectTE(Class<TE> clazz) {
	    return ObjectUtils.firstNonNull(SlabUtil.detectSlabTileentity(this.world, this.pos, clazz),
	    								detectChest(this.world, this.pos.north(), clazz),
	                                    detectChest(this.world, this.pos.east(), clazz),
	                                    detectChest(this.world, this.pos.south(), clazz),
	                                    detectChest(this.world, this.pos.west(), clazz));
	}

	private <TE extends TileEntity> TE detectChest(World world, BlockPos pos, Class<TE> clazz) {
		TileEntity te = this.world.getTileEntity(pos);
	    
	    if(te != null && clazz.isAssignableFrom(te.getClass())) {
	      return (TE) te;
	    }
	    
	    if(this.tile instanceof TileStencilTableSlab)
	    {
	    	TileStencilTableSlab slabTile = (TileStencilTableSlab)this.tile;
	    	TileEntity te2 = SlabUtil.detectLeveledSlab(world, pos, clazz, slabTile.getHalf());
		    if(te2 != null && clazz.isAssignableFrom(te2.getClass())) {
		    	return (TE) te2;
		    }
	    }
	    
	    return null;
	}
}
