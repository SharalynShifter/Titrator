package com.sharalyn.titrator;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import cpw.mods.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer p, World w, int x, int y, int z) {
        return new ContainerTitrator(p.inventory, (TileEntityTitrator) w.getTileEntity(x, y, z));
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer p, World w, int x, int y, int z) {
        return new GuiTitrator(p.inventory, (TileEntityTitrator) w.getTileEntity(x, y, z));
    }
}
