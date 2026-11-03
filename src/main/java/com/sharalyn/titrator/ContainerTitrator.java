package com.sharalyn.titrator;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ContainerTitrator extends Container {

    public TileEntityTitrator tile;
    private int lastFill, lastAmount, lastFluidId, lastProgress, lastTarget, lastEnergy;

    public ContainerTitrator(InventoryPlayer playerInv, TileEntityTitrator te) {
        tile = te;
        addSlotToContainer(new Slot(te, 0, 80, 20) {

            @Override
            public boolean isItemValid(ItemStack s) {
                return true;
            }
        });
        addSlotToContainer(new Slot(te, 1, 80, 60) {

            @Override
            public boolean isItemValid(ItemStack s) {
                return false;
            }
        });
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlotToContainer(new Slot(playerInv, c + r * 9 + 9, 8 + c * 18, 94 + r * 18));
        for (int c = 0; c < 9; c++) addSlotToContainer(new Slot(playerInv, c, 8 + c * 18, 152));
    }

    @Override
    public void addCraftingToCrafters(ICrafting c) {
        super.addCraftingToCrafters(c);
        c.sendProgressBarUpdate(this, 0, tile.getFillAmount());
        c.sendProgressBarUpdate(this, 1, tile.tank.getFluidAmount());
        int id = tile.tank.getFluid() != null ? FluidRegistry.getFluidID(
            tile.tank.getFluid()
                .getFluid())
            : 0;
        c.sendProgressBarUpdate(this, 2, id);
        c.sendProgressBarUpdate(this, 3, tile.getWorkProgress());
        c.sendProgressBarUpdate(this, 4, tile.getWorkTarget());
        c.sendProgressBarUpdate(this, 5, tile.getEnergy());
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (Object o : crafters) {
            ICrafting c = (ICrafting) o;
            if (lastFill != tile.getFillAmount()) c.sendProgressBarUpdate(this, 0, tile.getFillAmount());
            if (lastAmount != tile.tank.getFluidAmount()) c.sendProgressBarUpdate(this, 1, tile.tank.getFluidAmount());
            int id = tile.tank.getFluid() != null ? FluidRegistry.getFluidID(
                tile.tank.getFluid()
                    .getFluid())
                : 0;
            if (lastFluidId != id) c.sendProgressBarUpdate(this, 2, id);
            if (lastProgress != tile.getWorkProgress()) c.sendProgressBarUpdate(this, 3, tile.getWorkProgress());
            if (lastTarget != tile.getWorkTarget()) c.sendProgressBarUpdate(this, 4, tile.getWorkTarget());
            if (lastEnergy != tile.getEnergy()) c.sendProgressBarUpdate(this, 5, tile.getEnergy());
        }
        lastFill = tile.getFillAmount();
        lastAmount = tile.tank.getFluidAmount();
        lastFluidId = tile.tank.getFluid() != null ? FluidRegistry.getFluidID(
            tile.tank.getFluid()
                .getFluid())
            : 0;
        lastProgress = tile.getWorkProgress();
        lastTarget = tile.getWorkTarget();
        lastEnergy = tile.getEnergy();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int data) {
        if (id == 0) tile.setFillAmount(data);
        if (id == 1) {
            if (tile.tank.getFluid() != null) tile.tank.getFluid().amount = data;
        }
        if (id == 2) {
            if (data == 0) tile.tank.setFluid(null);
            else {
                int amt = tile.tank.getFluid() != null ? tile.tank.getFluidAmount() : lastAmount;
                if (amt == 0) amt = lastAmount;
                tile.tank.setFluid(new FluidStack(FluidRegistry.getFluid(data), amt));
            }
        }
        if (id == 3) tile.setWorkProgress(data);
        if (id == 4) tile.setWorkTarget(data);
        if (id == 5) tile.setEnergy(data);
    }

    @Override
    public boolean canInteractWith(EntityPlayer p) {
        return tile.isUseableByPlayer(p);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer p, int idx) {
        ItemStack stack = null;
        Slot slot = (Slot) inventorySlots.get(idx);
        if (slot != null && slot.getHasStack()) {
            ItemStack s = slot.getStack();
            stack = s.copy();
            if (idx < 2) {
                if (!mergeItemStack(s, 2, 38, true)) return null;
            } else if (!mergeItemStack(s, 0, 1, false)) return null;
            if (s.stackSize == 0) slot.putStack(null);
            else slot.onSlotChanged();
        }
        return stack;
    }
}
