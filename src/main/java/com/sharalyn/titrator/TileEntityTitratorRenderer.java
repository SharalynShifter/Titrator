package com.sharalyn.titrator;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;

public class TileEntityTitratorRenderer extends TileEntitySpecialRenderer {

    private static final float WINDOW_Y_BOTTOM = 6f / 16f;
    private static final float WINDOW_Y_TOP = 12f / 16f;
    private static final float WINDOW_HEIGHT = WINDOW_Y_TOP - WINDOW_Y_BOTTOM;

    // Fixed: normal side window at 5-7 (left), mirrored at 8-10 (right) - matches textures
    private static final float WIN_X0 = 5f / 16f;
    private static final float WIN_X1 = 8f / 16f; // 5,6,7 = 3px inside window for normal side
    private static final float WIN_X0_MIR = 8f / 16f;
    private static final float WIN_X1_MIR = 11f / 16f; // 8,9,10 = 3px inside window for mirrored side

    @Override
    public void renderTileEntityAt(TileEntity tileEntity, double x, double y, double z, float partialTicks) {
        if (!(tileEntity instanceof TileEntityTitrator)) return;
        TileEntityTitrator te = (TileEntityTitrator) tileEntity;
        if (te.tank == null || te.tank.getFluid() == null || te.tank.getFluidAmount() <= 0) return;

        FluidStack fluidStack = te.tank.getFluid();
        Fluid fluid = fluidStack.getFluid();
        if (fluid == null) return;

        IIcon fluidIcon = fluid.getIcon(fluidStack);
        if (fluidIcon == null) fluidIcon = fluid.getStillIcon();
        if (fluidIcon == null) return;

        int meta = te.getWorldObj()
            .getBlockMetadata(te.xCoord, te.yCoord, te.zCoord);
        int frontSide = meta;
        if (frontSide < 2 || frontSide > 5) frontSide = 3;

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

        float fillRatio = (float) te.tank.getFluidAmount() / (float) te.tank.getCapacity();
        fillRatio = Math.max(0f, Math.min(1f, fillRatio));

        float fluidYBottom = WINDOW_Y_BOTTOM;
        float fluidYTop = WINDOW_Y_BOTTOM + (WINDOW_HEIGHT * fillRatio);
        if (fluidYTop <= fluidYBottom + 0.001f) return;

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        this.bindTexture(net.minecraft.client.renderer.texture.TextureMap.locationBlocksTexture);
        Tessellator tess = Tessellator.instance;

        renderFluidOnSide(tess, fluidIcon, rightSide, fluidYBottom, fluidYTop, false);
        renderFluidOnSide(tess, fluidIcon, leftSide, fluidYBottom, fluidYTop, true);

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private void renderFluidOnSide(Tessellator tess, IIcon icon, int side, float yBottom, float yTop,
        boolean isMirrored) {
        float offset = 0.002f;
        float h0 = isMirrored ? WIN_X0_MIR : WIN_X0;
        float h1 = isMirrored ? WIN_X1_MIR : WIN_X1;

        // Fix UV flip - East (5) and North (2) are flipped in vanilla RenderBlocks
        // After testing: front East/West works = North/South sides (2,3) are OK, so flip East+North
        if (side == 5 || side == 2) {
            float flippedH0 = 1f - h1;
            float flippedH1 = 1f - h0;
            h0 = flippedH0;
            h1 = flippedH1;
        }

        if (side == 5) {
            drawQuad(tess, icon, 1f + offset, 1f + offset, yBottom, yTop, h0, h1, true);
        } else if (side == 4) {
            drawQuad(tess, icon, 0f - offset, 0f - offset, yBottom, yTop, h0, h1, true);
        } else if (side == 3) {
            drawQuad(tess, icon, h0, h1, yBottom, yTop, 1f + offset, 1f + offset, false);
        } else if (side == 2) {
            drawQuad(tess, icon, h0, h1, yBottom, yTop, 0f - offset, 0f - offset, false);
        }
    }

    private void drawQuad(Tessellator tess, IIcon icon, float x0, float x1, float y0, float y1, float z0, float z1,
        boolean isEastWest) {
        tess.startDrawingQuads();
        tess.setNormal(0, 1, 0);
        tess.setBrightness(0xF000F0);

        float u0 = icon.getMinU();
        float u1 = icon.getMaxU();
        float v0 = icon.getMinV();
        float v1 = icon.getMaxV();

        if (isEastWest) {
            tess.addVertexWithUV(x0, y1, z1, u1, v0);
            tess.addVertexWithUV(x0, y1, z0, u0, v0);
            tess.addVertexWithUV(x0, y0, z0, u0, v1);
            tess.addVertexWithUV(x0, y0, z1, u1, v1);
        } else {
            tess.addVertexWithUV(x1, y1, z0, u1, v0);
            tess.addVertexWithUV(x0, y1, z0, u0, v0);
            tess.addVertexWithUV(x0, y0, z0, u0, v1);
            tess.addVertexWithUV(x1, y0, z0, u1, v1);
        }
        tess.draw();
    }
}
