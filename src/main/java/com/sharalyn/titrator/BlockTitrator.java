package com.sharalyn.titrator;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockTitrator extends BlockContainer {

    private IIcon iconTop, iconBottom, iconSide, iconSideMirrored, iconBack, iconFront, iconFrontActive;

    public BlockTitrator() {
        super(Material.iron);
        setBlockName("titrator");
        setHardness(3.0F);
        setStepSound(soundTypeMetal);
    }

    @Override
    public void registerBlockIcons(IIconRegister reg) {
        // assets/titrator/textures/blocks/
        iconTop = reg.registerIcon("titrator:titrator_top");
        iconBottom = reg.registerIcon("titrator:titrator_bottom");
        iconSide = reg.registerIcon("titrator:titrator_side");
        iconSideMirrored = reg.registerIcon("titrator:titrator_side_mirrored");
        iconBack = reg.registerIcon("titrator:titrator_back");
        iconFront = reg.registerIcon("titrator:titrator_front");
        iconFrontActive = reg.registerIcon("titrator:titrator_front_active");
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        // Inventory / item render
        if (side == 0) return iconBottom;
        if (side == 1) return iconTop;
        if (side == 3) return iconFront; // show front in inventory
        if (side == 2) return iconBack; // back in inventory
        return iconSide;
    }

    @Override
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        int meta = world.getBlockMetadata(x, y, z);
        TileEntity te = world.getTileEntity(x, y, z);
        boolean active = false;
        if (te instanceof TileEntityTitrator) {
            active = ((TileEntityTitrator) te).isWorking();
        }
        if (side == 0) return iconBottom;
        if (side == 1) return iconTop;

        int frontSide = meta;
        if (frontSide < 2 || frontSide > 5) frontSide = 3; // default south

        // back = opposite of front
        int backSide = 3;
        if (frontSide == 2) backSide = 3;
        else if (frontSide == 3) backSide = 2;
        else if (frontSide == 4) backSide = 5;
        else if (frontSide == 5) backSide = 4;

        if (side == frontSide) {
            return active ? iconFrontActive : iconFront;
        }
        if (side == backSide) {
            return iconBack; // clean like bottom, as you suggested
        }

        // left / right logic to mirror side textures
        // front north (2): right=east(5), left=west(4)
        // front south (3): right=west(4), left=east(5)
        // front west (4): right=north(2), left=south(3)
        // front east (5): right=south(3), left=north(2)
        int rightSide = 5, leftSide = 4;
        if (frontSide == 2) {
            rightSide = 5;
            leftSide = 4;
        } else if (frontSide == 3) {
            rightSide = 4;
            leftSide = 5;
        } else if (frontSide == 4) {
            rightSide = 2;
            leftSide = 3;
        } else if (frontSide == 5) {
            rightSide = 3;
            leftSide = 2;
        }

        if (side == rightSide) {
            return iconSide; // right side = current look
        }
        if (side == leftSide) {
            return iconSideMirrored; // left side = mirrored
        }

        return iconSide;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        int l = MathHelper.floor_double((double) (placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        int meta = 0;
        if (l == 0) meta = 2; // north
        if (l == 1) meta = 5; // east
        if (l == 2) meta = 3; // south
        if (l == 3) meta = 4; // west
        world.setBlockMetadataWithNotify(x, y, z, meta, 2);
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEntityTitrator) {
            if (((TileEntityTitrator) te).isWorking()) {
                return 7; // little LED glow when working - lab-y
            }
        }
        return 0;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityTitrator();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy,
        float hz) {
        if (world.isRemote) return true;
        TileEntityTitrator te = (TileEntityTitrator) world.getTileEntity(x, y, z);
        net.minecraft.item.ItemStack held = player.getHeldItem();
        // Right-click with fluid container to fill tank (vanilla behavior)
        // If sneaking, always open GUI
        if (held != null && !player.isSneaking()) {
            if (te.fillFromHeldItem(held, player)) {
                return true;
            }
        }
        player.openGui(Titrator.instance, 0, world, x, y, z);
        return true;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return true;
    }

    @Override
    public int getRenderType() {
        return 0;
    }
}
