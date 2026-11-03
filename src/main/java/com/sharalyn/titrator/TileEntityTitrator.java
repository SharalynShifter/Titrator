package com.sharalyn.titrator;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.fluids.IFluidHandler;

import cpw.mods.fml.common.Optional;

@Optional.InterfaceList({ @Optional.Interface(iface = "cofh.api.energy.IEnergyReceiver", modid = "CoFHAPI|energy"),
    @Optional.Interface(iface = "cofh.api.energy.IEnergyHandler", modid = "CoFHAPI|energy") })
public class TileEntityTitrator extends TileEntity
    implements IFluidHandler, ISidedInventory, cofh.api.energy.IEnergyReceiver {

    public FluidTank tank = new FluidTank(8000);
    private ItemStack[] inv = new ItemStack[2];
    private int fillAmount = 250;
    private static final int[] SLOTS_TOP_SIDES = new int[] { 0 };
    private static final int[] SLOTS_BOTTOM = new int[] { 1 };

    private static final int WATER_BOTTLE_MB = 250;
    private static final int CREOSOTE_BOTTLE_MB = 1000;

    private int energy = 0;
    private boolean requireRF = true;

    private int workProgress = 0;
    private int workTarget = 0;
    private boolean workIsFilling = true;
    private FluidStack workFluid = null;
    private ItemStack workOutput = null;
    private ItemStack workInputRef = null;

    private int getRfPerTick() {
        try {
            return TitratorConfig.rfPerTick;
        } catch (Exception e) {
            return 1;
        }
    }

    private int getRfCapacity() {
        try {
            return TitratorConfig.rfCapacity;
        } catch (Exception e) {
            return 5000;
        }
    }

    private int getRfMaxReceive() {
        try {
            return TitratorConfig.rfMaxReceive;
        } catch (Exception e) {
            return 100;
        }
    }

    private int getMbPerTick() {
        try {
            return TitratorConfig.mbPerTick;
        } catch (Exception e) {
            return 100;
        }
    }

    private boolean getRequireRFConfig() {
        try {
            return TitratorConfig.requireRF;
        } catch (Exception e) {
            return requireRF;
        }
    }

    public boolean getWorkIsFilling() {
        return workIsFilling;
    }

    public int getFillAmount() {
        return fillAmount;
    }

    public void setFillAmount(int v) {
        fillAmount = Math.max(1, Math.min(8000, v));
        markDirty();
        if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    private boolean isEmptyBottle(ItemStack s) {
        return s != null && s.getItem() == Items.glass_bottle;
    }

    private boolean isWaterBottle(ItemStack s) {
        return s != null && s.getItem() == Items.potionitem && s.getItemDamage() == 0;
    }

    private boolean isCreosoteBottle(ItemStack s) {
        if (s == null) return false;
        FluidStack fs = FluidContainerRegistry.getFluidForFilledItem(s);
        if (fs != null && fs.getFluid() != null
            && fs.getFluid()
                .getName()
                .toLowerCase()
                .contains("creosote"))
            return true;
        String unloc = s.getItem()
            .getUnlocalizedName()
            .toLowerCase();
        if (unloc.contains("creosote") && unloc.contains("bottle")) return true;
        if (s.hasDisplayName() && s.getDisplayName()
            .toLowerCase()
            .contains("creosote")) return true;
        if (unloc.contains("fluidcontainers") && s.getItemDamage() == 0) {
            if (s.getItem()
                .getUnlocalizedName()
                .toLowerCase()
                .contains("immersive")
                || s.getItem()
                    .getClass()
                    .getName()
                    .toLowerCase()
                    .contains("immersive"))
                return true;
        }
        return false;
    }

    private ItemStack findRealCreosoteBottle(FluidStack fluid) {
        try {
            net.minecraft.item.ItemStack stack = cpw.mods.fml.common.registry.GameRegistry
                .findItemStack("Railcraft", "bottle.creosote", 1);
            if (stack != null) return stack;
        } catch (Exception e) {}
        try {
            net.minecraft.item.ItemStack stack = cpw.mods.fml.common.registry.GameRegistry
                .findItemStack("Railcraft", "fluid.creosote.bottle", 1);
            if (stack != null) return stack;
        } catch (Exception e) {}
        try {
            net.minecraft.item.ItemStack stack = cpw.mods.fml.common.registry.GameRegistry
                .findItemStack("Railcraft", "bottle.creosote.oil", 1);
            if (stack != null) return stack;
        } catch (Exception e) {}
        try {
            net.minecraft.item.Item rcItem = cpw.mods.fml.common.registry.GameRegistry
                .findItem("Railcraft", "bottle.creosote");
            if (rcItem != null) return new ItemStack(rcItem, 1, 0);
        } catch (Exception e) {}
        try {
            net.minecraft.item.Item ieItem = cpw.mods.fml.common.registry.GameRegistry
                .findItem("ImmersiveEngineering", "fluidContainers");
            if (ieItem != null) return new ItemStack(ieItem, 1, 0);
        } catch (Exception e) {}
        try {
            net.minecraft.item.Item ieItem = cpw.mods.fml.common.registry.GameRegistry
                .findItem("immersiveengineering", "fluidContainers");
            if (ieItem != null) return new ItemStack(ieItem, 1, 0);
        } catch (Exception e) {}
        try {
            ItemStack fromRegistry = FluidContainerRegistry
                .fillFluidContainer(fluid, new ItemStack(Items.glass_bottle));
            if (fromRegistry != null) return fromRegistry;
        } catch (Exception e) {}
        return null;
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;
        if (inv[0] == null || inv[1] != null) {
            if (workTarget != 0) resetWork();
            return;
        }
        if (workTarget > 0) {
            if (workInputRef == null || inv[0] == null || !isSameItemType(workInputRef, inv[0])) {
                resetWork();
                return;
            }
            if (!canWorkWithEnergy()) return;
            consumeEnergy();
            int remaining = workTarget - workProgress;
            int toTransfer = Math.min(getMbPerTick(), remaining);
            if (workIsFilling) {
                if (tank.getFluidAmount() < toTransfer) {
                    resetWork();
                    return;
                }
                tank.drain(toTransfer, true);
            } else {
                FluidStack toFill = workFluid.copy();
                toFill.amount = toTransfer;
                if (tank.getFluidAmount() + toTransfer > tank.getCapacity()) {
                    resetWork();
                    return;
                }
                if (tank.getFluid() == null) tank.setFluid(toFill.copy());
                else tank.fill(toFill, true);
            }
            workProgress += toTransfer;
            if (workProgress >= workTarget) {
                inv[1] = workOutput.copy();
                inv[0].stackSize--;
                if (inv[0].stackSize <= 0) inv[0] = null;
                resetWork();
                markDirty();
                if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            } else {
                if (worldObj.getTotalWorldTime() % 5 == 0) {
                    markDirty();
                    worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                }
            }
            return;
        }
        ItemStack input = inv[0];
        ItemStack empty = input.copy();
        empty.stackSize = 1;
        FluidStack contained = getFluidFromContainer(input);
        if (contained != null) {
            if (tank.getFluid() != null && tank.getFluid()
                .getFluid() != contained.getFluid()) return;
            if (tank.getFluidAmount() + contained.amount > tank.getCapacity()) return;
            ItemStack emptyResult = getEmptyContainer(input);
            if (emptyResult != null) {
                workIsFilling = false;
                workTarget = contained.amount;
                workProgress = 0;
                workFluid = contained.copy();
                workOutput = emptyResult.copy();
                workInputRef = input.copy();
                workInputRef.stackSize = 1;
                markDirty();
                if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                return;
            }
        }
        if (tank.getFluid() != null && tank.getFluidAmount() >= fillAmount) {
            FluidStack toFill = new FluidStack(
                tank.getFluid()
                    .getFluid(),
                fillAmount);
            ItemStack filled = tryFillContainer(empty, toFill);
            if (filled != null) {
                workIsFilling = true;
                workTarget = fillAmount;
                workProgress = 0;
                workFluid = toFill.copy();
                workOutput = filled.copy();
                workInputRef = input.copy();
                workInputRef.stackSize = 1;
                markDirty();
                if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }
        }
    }

    private boolean isSameItemType(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getItem() != b.getItem()) return false;
        if (a.getItemDamage() != b.getItemDamage()) return false;
        return true;
    }

    private void resetWork() {
        workProgress = 0;
        workTarget = 0;
        workFluid = null;
        workOutput = null;
        workInputRef = null;
        workIsFilling = true;
    }

    public int getWorkProgress() {
        return workProgress;
    }

    public int getWorkTarget() {
        return workTarget;
    }

    public boolean isWorking() {
        return workTarget > 0;
    }

    public int getScaledProgress(int scale) {
        if (workTarget == 0) return 0;
        return workProgress * scale / workTarget;
    }

    public void setWorkProgress(int p) {
        workProgress = p;
    }

    public void setWorkTarget(int t) {
        workTarget = t;
    }

    private boolean canWorkWithEnergy() {
        if (!getRequireRFConfig()) {
            if (energy >= getRfPerTick()) return true;
            try {
                Class.forName("cofh.api.energy.IEnergyReceiver");
                return true;
            } catch (ClassNotFoundException e) {
                return true;
            }
        } else {
            return energy >= getRfPerTick();
        }
    }

    private void consumeEnergy() {
        if (energy >= getRfPerTick()) {
            energy -= getRfPerTick();
            if (energy < 0) energy = 0;
        }
    }

    @Optional.Method(modid = "CoFHAPI|energy")
    @Override
    public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
        int toReceive = Math.min(maxReceive, Math.min(getRfMaxReceive(), getRfCapacity() - energy));
        if (!simulate) {
            energy += toReceive;
            if (worldObj != null && worldObj.getTotalWorldTime() % 10 == 0)
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
        return toReceive;
    }

    @Optional.Method(modid = "CoFHAPI|energy")
    @Override
    public int getEnergyStored(ForgeDirection from) {
        return energy;
    }

    @Optional.Method(modid = "CoFHAPI|energy")
    @Override
    public int getMaxEnergyStored(ForgeDirection from) {
        return getRfCapacity();
    }

    @Optional.Method(modid = "CoFHAPI|energy")
    @Override
    public boolean canConnectEnergy(ForgeDirection from) {
        return true;
    }

    // No-args version kept for compatibility with some CoFH forks, NOT an override in 1.7.10
    @Optional.Method(modid = "CoFHAPI|energy")
    public int getMaxEnergyStored() {
        return getRfCapacity();
    }

    @Optional.Method(modid = "CoFHAPI|energy")
    public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {
        return 0;
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int e) {
        energy = Math.max(0, Math.min(getRfCapacity(), e));
    }

    public void setRequireRF(boolean req) {
        requireRF = req;
        try {
            TitratorConfig.requireRF = req;
        } catch (Exception ex) {}
    }

    public boolean getRequireRF() {
        return getRequireRFConfig();
    }

    private FluidStack getFluidFromContainer(ItemStack stack) {
        if (stack == null) return null;
        if (stack.getItem() instanceof IFluidContainerItem) {
            IFluidContainerItem item = (IFluidContainerItem) stack.getItem();
            return item.getFluid(stack);
        }
        if (isWaterBottle(stack)) {
            if (FluidRegistry.WATER != null) return new FluidStack(FluidRegistry.WATER, WATER_BOTTLE_MB);
        }
        if (isCreosoteBottle(stack)) {
            Fluid creosote = FluidRegistry.getFluid("creosote");
            if (creosote == null) creosote = FluidRegistry.getFluid("creosote oil");
            if (creosote == null) creosote = FluidRegistry.getFluid("creosote_oil");
            if (creosote != null) return new FluidStack(creosote, CREOSOTE_BOTTLE_MB);
        }
        return FluidContainerRegistry.getFluidForFilledItem(stack);
    }

    private ItemStack getEmptyContainer(ItemStack filled) {
        if (filled == null) return null;
        if (filled.getItem() instanceof IFluidContainerItem) {
            IFluidContainerItem item = (IFluidContainerItem) filled.getItem();
            ItemStack copy = filled.copy();
            copy.stackSize = 1;
            item.drain(copy, Integer.MAX_VALUE, true);
            if (item.getFluid(copy) != null) {
                NBTTagCompound tag = copy.getTagCompound();
                if (tag != null) {
                    tag.removeTag("Fluid");
                    if (tag.hasNoTags()) copy.setTagCompound(null);
                }
            }
            if (item.getFluid(copy) == null) return copy;
            return new ItemStack(Items.glass_bottle);
        }
        if (isWaterBottle(filled)) return new ItemStack(Items.glass_bottle);
        if (isCreosoteBottle(filled)) return new ItemStack(Items.glass_bottle);
        return FluidContainerRegistry.drainFluidContainer(filled);
    }

    private ItemStack tryFillContainer(ItemStack empty, FluidStack fluid) {
        if (empty == null || fluid == null) return null;
        if (isEmptyBottle(empty)) {
            if (fluid.getFluid() == FluidRegistry.WATER && fluid.amount == WATER_BOTTLE_MB)
                return new ItemStack(Items.potionitem, 1, 0);
            if (fluid.getFluid() != null && fluid.getFluid()
                .getName()
                .toLowerCase()
                .contains("creosote") && fluid.amount == CREOSOTE_BOTTLE_MB) {
                ItemStack realBottle = findRealCreosoteBottle(fluid);
                if (realBottle != null) return realBottle;
                ItemStack fromReg = FluidContainerRegistry.fillFluidContainer(fluid, empty);
                if (fromReg != null) return fromReg;
            }
            return null;
        }
        if (empty.getItem() instanceof IFluidContainerItem) {
            IFluidContainerItem cell = (IFluidContainerItem) empty.getItem();
            ItemStack copy = empty.copy();
            int sim = cell.fill(copy, fluid, false);
            if (sim == fillAmount) {
                cell.fill(copy, fluid, true);
                if (cell.getFluid(copy) == null) {
                    NBTTagCompound tag = copy.getTagCompound();
                    if (tag == null) tag = new NBTTagCompound();
                    NBTTagCompound fluidTag = new NBTTagCompound();
                    fluid.writeToNBT(fluidTag);
                    tag.setTag("Fluid", fluidTag);
                    copy.setTagCompound(tag);
                }
                return copy;
            }
        } else {
            if (fillAmount == 1000) {
                ItemStack filled = FluidContainerRegistry.fillFluidContainer(fluid, empty);
                if (filled != null) return filled;
            }
        }
        return null;
    }

    public boolean fillFromHeldItem(ItemStack held, EntityPlayer player) {
        if (held == null || worldObj == null) return false;
        // Handle IFluidContainerItem (portable tanks, etc.)
        if (held.getItem() instanceof IFluidContainerItem) {
            IFluidContainerItem cont = (IFluidContainerItem) held.getItem();
            FluidStack fluid = cont.getFluid(held);
            if (fluid != null) {
                // Empty portable tank INTO titrator
                if (tank.getFluid() != null && tank.getFluid()
                    .getFluid() != fluid.getFluid()) return false;
                int filled = tank.fill(fluid, false);
                if (filled >= fluid.amount) {
                    if (!worldObj.isRemote) {
                        tank.fill(fluid, true);
                        ItemStack copy = held.copy();
                        copy.stackSize = 1;
                        cont.drain(copy, fluid.amount, true);
                        if (cont.getFluid(copy) != null) {
                            NBTTagCompound tag = copy.getTagCompound();
                            if (tag != null) {
                                tag.removeTag("Fluid");
                                if (tag.hasNoTags()) copy.setTagCompound(null);
                            }
                        }
                        boolean isCreative = player.capabilities.isCreativeMode;
                        if (held.stackSize == 1 || isCreative)
                            player.inventory.setInventorySlotContents(player.inventory.currentItem, copy);
                        else {
                            held.stackSize--;
                            if (!player.inventory.addItemStackToInventory(copy))
                                player.dropPlayerItemWithRandomChoice(copy, false);
                        }
                        markDirty();
                        if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                    }
                    return true;
                }
            } else {
                // Fill EMPTY portable tank FROM titrator - THIS WAS MISSING
                if (tank.getFluid() == null || tank.getFluidAmount() <= 0) return false;
                if (tank.getFluidAmount() < fillAmount) return false;
                FluidStack tankFluid = new FluidStack(
                    tank.getFluid()
                        .getFluid(),
                    fillAmount);
                ItemStack single = held.copy();
                single.stackSize = 1;
                int sim = cont.fill(single, tankFluid, false);
                if (sim == fillAmount || sim == tankFluid.amount) {
                    ItemStack filled = single.copy();
                    cont.fill(filled, tankFluid, true);
                    if (cont.getFluid(filled) == null) {
                        NBTTagCompound tag = filled.getTagCompound();
                        if (tag == null) tag = new NBTTagCompound();
                        NBTTagCompound fluidTag = new NBTTagCompound();
                        tankFluid.writeToNBT(fluidTag);
                        tag.setTag("Fluid", fluidTag);
                        filled.setTagCompound(tag);
                    }
                    if (!worldObj.isRemote) {
                        tank.drain(fillAmount, true);
                        boolean isCreative = player.capabilities.isCreativeMode;
                        if (held.stackSize == 1 || isCreative)
                            player.inventory.setInventorySlotContents(player.inventory.currentItem, filled);
                        else {
                            held.stackSize--;
                            if (!player.inventory.addItemStackToInventory(filled))
                                player.dropPlayerItemWithRandomChoice(filled, false);
                        }
                        markDirty();
                        if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                    }
                    return true;
                }
                ItemStack filled = tryFillContainer(single, tankFluid);
                if (filled != null) {
                    if (!worldObj.isRemote) {
                        tank.drain(fillAmount, true);
                        boolean isCreative = player.capabilities.isCreativeMode;
                        if (held.stackSize == 1 || isCreative)
                            player.inventory.setInventorySlotContents(player.inventory.currentItem, filled);
                        else {
                            held.stackSize--;
                            if (!player.inventory.addItemStackToInventory(filled))
                                player.dropPlayerItemWithRandomChoice(filled, false);
                        }
                        markDirty();
                        if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                    }
                    return true;
                }
            }
        } else {
            // Vanilla buckets, bottles, etc.
            FluidStack contained = getFluidFromContainer(held);
            if (contained != null) {
                if (tank.getFluid() != null && tank.getFluid()
                    .getFluid() != contained.getFluid()) return false;
                if (tank.getFluidAmount() + contained.amount > tank.getCapacity()) return false;
                if (!worldObj.isRemote) {
                    ItemStack emptyResult = getEmptyContainer(held);
                    if (emptyResult != null) {
                        tank.fill(contained, true);
                        boolean isCreative = player.capabilities.isCreativeMode;
                        if (held.stackSize == 1 || isCreative)
                            player.inventory.setInventorySlotContents(player.inventory.currentItem, emptyResult);
                        else {
                            held.stackSize--;
                            if (!player.inventory.addItemStackToInventory(emptyResult))
                                player.dropPlayerItemWithRandomChoice(emptyResult, false);
                        }
                        markDirty();
                        if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                        return true;
                    }
                }
            }
            if (tank.getFluid() != null && tank.getFluidAmount() > 0) {
                FluidStack tankFluid = tank.getFluid()
                    .copy();
                ItemStack single = held.copy();
                single.stackSize = 1;
                int required = fillAmount;
                if (isEmptyBottle(single)) {
                    if (tankFluid.getFluid() == FluidRegistry.WATER) required = WATER_BOTTLE_MB;
                    else if (tankFluid.getFluid()
                        .getName()
                        .toLowerCase()
                        .contains("creosote")) required = CREOSOTE_BOTTLE_MB;
                }
                if (tank.getFluidAmount() >= required) {
                    FluidStack toFill = new FluidStack(tankFluid.getFluid(), required);
                    ItemStack filled = tryFillContainer(single, toFill);
                    if (filled != null) {
                        if (!worldObj.isRemote) {
                            tank.drain(required, true);
                            boolean isCreative = player.capabilities.isCreativeMode;
                            if (held.stackSize == 1 || isCreative)
                                player.inventory.setInventorySlotContents(player.inventory.currentItem, filled);
                            else {
                                held.stackSize--;
                                if (!player.inventory.addItemStackToInventory(filled))
                                    player.dropPlayerItemWithRandomChoice(filled, false);
                            }
                            markDirty();
                            if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public int fill(ForgeDirection from, FluidStack res, boolean doFill) {
        if (from == ForgeDirection.DOWN) return 0;
        return tank.fill(res, doFill);
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack res, boolean doDrain) {
        return null;
    }

    @Override
    public FluidStack drain(ForgeDirection from, int max, boolean doDrain) {
        return null;
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid f) {
        return from != ForgeDirection.DOWN;
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid f) {
        return false;
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        return new FluidTankInfo[] { tank.getInfo() };
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return side == 0 ? SLOTS_BOTTOM : SLOTS_TOP_SIDES;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return slot == 0 && side != 0;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return slot == 1 && side == 0;
    }

    @Override
    public int getSizeInventory() {
        return 2;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        return inv[i];
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack s) {
        inv[i] = s;
        markDirty();
    }

    @Override
    public ItemStack decrStackSize(int i, int n) {
        if (inv[i] == null) return null;
        if (inv[i].stackSize <= n) {
            ItemStack r = inv[i];
            inv[i] = null;
            return r;
        }
        ItemStack r = inv[i].splitStack(n);
        if (inv[i].stackSize == 0) inv[i] = null;
        return r;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int i) {
        ItemStack s = inv[i];
        inv[i] = null;
        return s;
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack s) {
        return i == 0;
    }

    @Override
    public String getInventoryName() {
        return "Titrator";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer p) {
        return true;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        tank.readFromNBT(tag);
        fillAmount = tag.getInteger("FillAmount");
        energy = tag.getInteger("Energy");
        workProgress = tag.getInteger("WorkProgress");
        workTarget = tag.getInteger("WorkTarget");
        workIsFilling = tag.getBoolean("WorkIsFilling");
        if (tag.hasKey("WorkFluid")) workFluid = FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("WorkFluid"));
        else workFluid = null;
        if (tag.hasKey("WorkOutput")) workOutput = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("WorkOutput"));
        else workOutput = null;
        if (tag.hasKey("WorkInputRef"))
            workInputRef = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("WorkInputRef"));
        else workInputRef = null;
        if (tag.hasKey("RequireRF")) requireRF = tag.getBoolean("RequireRF");
        else requireRF = true;
        NBTTagList list = tag.getTagList("Items", 10);
        inv = new ItemStack[2];
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound c = list.getCompoundTagAt(i);
            inv[c.getByte("Slot")] = ItemStack.loadItemStackFromNBT(c);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tank.writeToNBT(tag);
        tag.setInteger("FillAmount", fillAmount);
        tag.setInteger("Energy", energy);
        tag.setInteger("WorkProgress", workProgress);
        tag.setInteger("WorkTarget", workTarget);
        tag.setBoolean("WorkIsFilling", workIsFilling);
        if (workFluid != null) {
            NBTTagCompound fTag = new NBTTagCompound();
            workFluid.writeToNBT(fTag);
            tag.setTag("WorkFluid", fTag);
        }
        if (workOutput != null) {
            NBTTagCompound oTag = new NBTTagCompound();
            workOutput.writeToNBT(oTag);
            tag.setTag("WorkOutput", oTag);
        }
        if (workInputRef != null) {
            NBTTagCompound iTag = new NBTTagCompound();
            workInputRef.writeToNBT(iTag);
            tag.setTag("WorkInputRef", iTag);
        }
        tag.setBoolean("RequireRF", requireRF);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < 2; i++) if (inv[i] != null) {
            NBTTagCompound c = new NBTTagCompound();
            c.setByte("Slot", (byte) i);
            inv[i].writeToNBT(c);
            list.appendTag(c);
        }
        tag.setTag("Items", list);
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
    }
}
