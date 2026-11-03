package com.sharalyn.titrator;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;

public class GuiTitrator extends GuiContainer {

    TileEntityTitrator tile;
    GuiTextField amountField;

    private int getRfCapacity() {
        try {
            return TitratorConfig.rfCapacity;
        } catch (Exception e) {
            return 5000;
        }
    }

    private int getRfPerTick() {
        try {
            return TitratorConfig.rfPerTick;
        } catch (Exception e) {
            return 1;
        }
    }

    private int getMbPerTick() {
        try {
            return TitratorConfig.mbPerTick;
        } catch (Exception e) {
            return 100;
        }
    }

    public GuiTitrator(InventoryPlayer inv, TileEntityTitrator te) {
        super(new ContainerTitrator(inv, te));
        tile = te;
        xSize = 176;
        ySize = 176;
    }

    @Override
    public void initGui() {
        super.initGui();
        guiLeft = (width - xSize) / 2;
        guiTop = (height - ySize) / 2;
        // Fill field - moved right, no overlap
        amountField = new GuiTextField(fontRendererObj, guiLeft + 110, guiTop + 24, 38, 12);
        amountField.setText(String.valueOf(tile.getFillAmount()));
        amountField.setMaxStringLength(4);
        amountField.setFocused(false);
        amountField.setTextColor(0xFFFFFF);
        amountField.setDisabledTextColour(0x555555);
        amountField.setEnableBackgroundDrawing(false);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        String title = "Titrator";
        fontRendererObj.drawString(title, (xSize - fontRendererObj.getStringWidth(title)) / 2, 6, 0x404040);
        fontRendererObj.drawString("Inventory", 8, 84, 0x404040);

        fontRendererObj.drawString("Fill:", 110, 14, 0x404040);
        fontRendererObj.drawString("mB", 150, 26, 0x606060);

        int relMx = mx - guiLeft;
        int relMy = my - guiTop;

        if (relMx >= 10 && relMx <= 28 && relMy >= 18 && relMy <= 70) {
            List<String> tip = new ArrayList<String>();
            if (tile.tank.getFluid() != null && tile.tank.getFluidAmount() > 0) {
                FluidStack fs = tile.tank.getFluid();
                tip.add(
                    EnumChatFormatting.WHITE + fs.getFluid()
                        .getLocalizedName());
                tip.add(EnumChatFormatting.GRAY + "" + fs.amount + " mB / 8000 mB");
                tip.add(EnumChatFormatting.DARK_GRAY + "" + (fs.amount * 100 / 8000) + "% full");
            } else {
                tip.add(EnumChatFormatting.GRAY + "Empty");
                tip.add(EnumChatFormatting.DARK_GRAY + "0 / 8000 mB");
            }
            func_146283_a(tip, relMx, relMy);
        }
        // RF bar - thinner (4px) and shorter, below Fill, tooltip only, no RF label
        if (relMx >= 154 && relMx <= 164 && relMy >= 38 && relMy <= 68) {
            List<String> tip = new ArrayList<String>();
            tip.add(
                EnumChatFormatting.RED + "Energy: "
                    + EnumChatFormatting.WHITE
                    + tile.getEnergy()
                    + " / "
                    + getRfCapacity()
                    + " RF");
            tip.add(EnumChatFormatting.GRAY + "Usage: " + getRfPerTick() + " RF/t");
            tip.add(EnumChatFormatting.GRAY + "Speed: " + getMbPerTick() + " mB/t");
            tip.add(
                tile.getRequireRF() ? EnumChatFormatting.RED + "Requires RF"
                    : EnumChatFormatting.GREEN + "Optional RF");
            if (tile.isWorking()) {
                tip.add(
                    EnumChatFormatting.YELLOW + "Working: "
                        + tile.getWorkProgress()
                        + " / "
                        + tile.getWorkTarget()
                        + " mB");
            } else {
                tip.add(EnumChatFormatting.DARK_GRAY + "Idle");
            }
            func_146283_a(tip, relMx, relMy);
        }
        if (relMx >= 78 && relMx <= 98 && relMy >= 36 && relMy <= 60) {
            if (tile.isWorking()) {
                List<String> tip = new ArrayList<String>();
                String dir = tile.getWorkIsFilling() ? "Tank -> Bottle" : "Bottle -> Tank";
                tip.add(EnumChatFormatting.WHITE + dir);
                tip.add(EnumChatFormatting.GRAY + "" + tile.getWorkProgress() + " / " + tile.getWorkTarget() + " mB");
                tip.add(
                    EnumChatFormatting.GREEN + ""
                        + (tile.getWorkTarget() > 0 ? tile.getWorkProgress() * 100 / tile.getWorkTarget() : 0)
                        + "%");
                func_146283_a(tip, relMx, relMy);
            }
        }
        if (relMx >= 110 && relMx <= 148 && relMy >= 24 && relMy <= 36) {
            List<String> tip = new ArrayList<String>();
            tip.add(EnumChatFormatting.WHITE + "Fill amount per bottle");
            tip.add(EnumChatFormatting.GRAY + "Click to edit");
            tip.add(EnumChatFormatting.GRAY + "Right-click for 250/1000 preset");
            tip.add(EnumChatFormatting.DARK_GRAY + "Range: 1-8000 mB");
            tip.add(EnumChatFormatting.YELLOW + "Water: 250 mB");
            tip.add(EnumChatFormatting.GOLD + "Creosote: 1000 mB");
            func_146283_a(tip, relMx, relMy);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float pt, int mx, int my) {
        GL11.glColor4f(1, 1, 1, 1);
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFFC6C6C6);
        drawRect(guiLeft, guiTop, guiLeft + xSize - 1, guiTop + 1, 0xFFFFFFFF);
        drawRect(guiLeft, guiTop, guiLeft + 1, guiTop + ySize - 1, 0xFFFFFFFF);
        drawRect(guiLeft + xSize - 1, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF555555);
        drawRect(guiLeft, guiTop + ySize - 1, guiLeft + xSize, guiTop + ySize, 0xFF555555);
        drawRect(guiLeft + 1, guiTop + 1, guiLeft + xSize - 1, guiTop + 2, 0xFF8B8B8B);
        drawRect(guiLeft + 1, guiTop + 2, guiLeft + 2, guiTop + ySize - 1, 0xFF8B8B8B);

        int tankLeft = guiLeft + 10;
        int tankRight = guiLeft + 28;
        int tankTop = guiTop + 18;
        int tankBottom = guiTop + 70;
        int tankHeight = tankBottom - tankTop;

        drawRect(tankLeft - 2, tankTop - 2, tankRight + 2, tankBottom + 2, 0xFF373737);
        drawRect(tankLeft - 1, tankTop - 1, tankRight + 1, tankBottom + 1, 0xFFFFFFFF);
        drawRect(tankLeft, tankTop, tankRight, tankBottom, 0xFF000000);
        drawRect(tankLeft + 1, tankTop + 1, tankRight - 1, tankBottom - 1, 0xFF8B8B8B);

        if (tile.tank.getFluid() != null && tile.tank.getFluidAmount() > 0) {
            FluidStack fs = tile.tank.getFluid();
            Fluid fluid = fs.getFluid();
            IIcon icon = fluid.getStillIcon();
            if (icon == null) icon = fluid.getIcon();
            if (icon != null) {
                mc.getTextureManager()
                    .bindTexture(TextureMap.locationBlocksTexture);
                int col = fluid.getColor(fs);
                float r = (col >> 16 & 0xFF) / 255f;
                float g = (col >> 8 & 0xFF) / 255f;
                float b = (col & 0xFF) / 255f;
                GL11.glColor4f(r, g, b, 1.0f);
                int fluidH = fs.amount * (tankHeight - 2) / 8000;
                int w = tankRight - tankLeft - 2;
                int remaining = fluidH;
                int yPos = tankBottom - 1;
                while (remaining > 0) {
                    int drawH = Math.min(16, remaining);
                    yPos -= drawH;
                    this.drawTexturedModelRectFromIcon(tankLeft + 1, yPos, icon, w, drawH);
                    remaining -= drawH;
                }
                GL11.glColor4f(1, 1, 1, 1);
            } else {
                int h = fs.amount * (tankHeight - 2) / 8000;
                if (h > 0) drawRect(tankLeft + 1, tankBottom - 1 - h, tankRight - 1, tankBottom - 1, 0xFF2D7FFF);
            }
        } else {
            drawRect(tankLeft + 1, tankTop + 1, tankRight - 1, tankBottom - 1, 0xFF6B6B6B);
        }

        for (int i = 0; i <= 4; i++) {
            int markY = tankBottom - 1 - (i * (tankHeight - 2) / 4);
            drawRect(tankRight, markY, tankRight + 6, markY + 1, 0xFF000000);
        }

        // RF bar - THINNER (6px wide) and SHORTER (30px tall), sitting below Fill field
        // Fill is at y=24, so RF bar starts at y=38 below it
        int rfTop = guiTop + 38;
        int rfBottom = guiTop + 68; // 30px tall, not full tank height
        int rfLeft = guiLeft + 156;
        int rfRight = guiLeft + 162; // 6px wide, thinner
        int rfHeight = rfBottom - rfTop;
        drawRect(rfLeft - 1, rfTop - 1, rfRight + 1, rfBottom + 1, 0xFF373737);
        drawRect(rfLeft, rfTop, rfRight, rfBottom, 0xFF000000);
        drawRect(rfLeft + 1, rfTop + 1, rfRight - 1, rfBottom - 1, 0xFF3A3A3A);
        if (tile.getEnergy() > 0) {
            int rfH = tile.getEnergy() * (rfHeight - 2) / getRfCapacity();
            if (rfH > 0) {
                for (int y = 0; y < rfH; y++) {
                    int r = 255;
                    int g = (int) ((float) y / rfH * 60);
                    int b = 0;
                    int color = (0xFF << 24) | (r << 16) | (g << 8) | b;
                    if (y % 4 == 0) color = 0xFFFF5555;
                    drawRect(rfLeft + 1, rfBottom - 1 - y - 1, rfRight - 1, rfBottom - 1 - y, color);
                }
            }
        }

        // Fill field background - black box like in your screenshot
        drawRect(guiLeft + 110, guiTop + 24, guiLeft + 150, guiTop + 36, 0xFF000000);
        // Slight inset
        drawRect(guiLeft + 111, guiTop + 25, guiLeft + 149, guiTop + 35, 0xFF000000);

        for (Object o : inventorySlots.inventorySlots) {
            Slot s = (Slot) o;
            int sx = guiLeft + s.xDisplayPosition - 1;
            int sy = guiTop + s.yDisplayPosition - 1;
            drawRect(sx, sy, sx + 18, sy + 18, 0xFFFFFFFF);
            drawRect(sx, sy, sx + 18, sy + 1, 0xFF373737);
            drawRect(sx, sy, sx + 1, sy + 18, 0xFF373737);
            drawRect(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
            if (!s.getHasStack()) {
                if (s.slotNumber == 0) {
                    drawRect(sx + 7, sy + 4, sx + 11, sy + 13, 0x668B8B8B);
                    drawRect(sx + 8, sy + 2, sx + 10, sy + 4, 0x668B8B8B);
                } else if (s.slotNumber == 1) {
                    drawRect(sx + 6, sy + 5, sx + 12, sy + 12, 0x66444444);
                }
            }
        }

        int arrowX = guiLeft + 88;
        int arrowTop = guiTop + 38;
        int arrowH = 20;
        drawRect(arrowX - 2, arrowTop, arrowX + 2, arrowTop + arrowH, 0xFF555555);
        drawRect(arrowX - 1, arrowTop, arrowX + 1, arrowTop + arrowH, 0xFF373737);

        if (tile.isWorking()) {
            boolean filling = tile.getWorkIsFilling();
            int prog = tile.getScaledProgress(arrowH);
            int progColorBright = filling ? 0xFF55FF55 : 0xFF55AAFF;
            int progColor = filling ? 0xFF00AA00 : 0xFF2244CC;
            if (filling) {
                if (prog > 0) {
                    drawRect(arrowX - 1, arrowTop, arrowX + 1, arrowTop + prog, progColorBright);
                    int headY = arrowTop + Math.min(prog, arrowH - 5);
                    if (prog >= arrowH - 5) {
                        drawRect(arrowX - 6, headY, arrowX + 6, headY + 1, progColor);
                        drawRect(arrowX - 5, headY + 1, arrowX + 5, headY + 2, progColor);
                        drawRect(arrowX - 4, headY + 2, arrowX + 4, headY + 3, progColor);
                        drawRect(arrowX - 3, headY + 3, arrowX + 3, headY + 4, progColor);
                        drawRect(arrowX - 1, headY + 4, arrowX + 1, headY + 5, progColor);
                    }
                }
                if (prog < arrowH) {
                    drawRect(arrowX - 6, arrowTop + arrowH - 5, arrowX + 6, arrowTop + arrowH - 4, 0xFF000000);
                    drawRect(arrowX - 5, arrowTop + arrowH - 4, arrowX + 5, arrowTop + arrowH - 3, 0xFF000000);
                    drawRect(arrowX - 4, arrowTop + arrowH - 3, arrowX + 4, arrowTop + arrowH - 2, 0xFF000000);
                    drawRect(arrowX - 3, arrowTop + arrowH - 2, arrowX + 3, arrowTop + arrowH - 1, 0xFF000000);
                    drawRect(arrowX - 1, arrowTop + arrowH - 1, arrowX + 1, arrowTop + arrowH, 0xFF000000);
                }
            } else {
                if (prog > 0) {
                    drawRect(arrowX - 1, arrowTop + arrowH - prog, arrowX + 1, arrowTop + arrowH, progColorBright);
                    int headY = arrowTop + arrowH - Math.min(prog, arrowH - 5) - 5;
                    if (prog >= arrowH - 5) {
                        drawRect(arrowX - 1, headY, arrowX + 1, headY + 1, progColor);
                        drawRect(arrowX - 3, headY + 1, arrowX + 3, headY + 2, progColor);
                        drawRect(arrowX - 4, headY + 2, arrowX + 4, headY + 3, progColor);
                        drawRect(arrowX - 5, headY + 3, arrowX + 5, headY + 4, progColor);
                        drawRect(arrowX - 6, headY + 4, arrowX + 6, headY + 5, progColor);
                    }
                }
                if (prog < arrowH) {
                    drawRect(arrowX - 1, arrowTop, arrowX + 1, arrowTop + 1, 0xFF000000);
                    drawRect(arrowX - 3, arrowTop + 1, arrowX + 3, arrowTop + 2, 0xFF000000);
                    drawRect(arrowX - 4, arrowTop + 2, arrowX + 4, arrowTop + 3, 0xFF000000);
                    drawRect(arrowX - 5, arrowTop + 3, arrowX + 5, arrowTop + 4, 0xFF000000);
                    drawRect(arrowX - 6, arrowTop + 4, arrowX + 6, arrowTop + 5, 0xFF000000);
                }
            }
        } else {
            drawRect(arrowX - 1, arrowTop, arrowX + 1, arrowTop + 15, 0xFF000000);
            drawRect(arrowX - 5, arrowTop + 15, arrowX + 5, arrowTop + 16, 0xFF000000);
            drawRect(arrowX - 4, arrowTop + 16, arrowX + 4, arrowTop + 17, 0xFF000000);
            drawRect(arrowX - 3, arrowTop + 17, arrowX + 3, arrowTop + 18, 0xFF000000);
            drawRect(arrowX - 2, arrowTop + 18, arrowX + 2, arrowTop + 19, 0xFF000000);
            drawRect(arrowX - 1, arrowTop + 19, arrowX + 1, arrowTop + 20, 0xFF000000);
        }
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        super.drawScreen(mx, my, pt);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glDisable(GL11.GL_LIGHTING);
        if (amountField.isFocused()) {
            amountField.drawTextBox();
        } else {
            // Centered display when not editing - looks cleaner
            String txt = amountField.getText();
            int w = fontRendererObj.getStringWidth(txt);
            int cx = amountField.xPosition + (amountField.width - w) / 2;
            // Slight vertical centering inside 12px box
            fontRendererObj.drawString(txt, cx, amountField.yPosition + 2, 0xFFFFFF);
        }
        GL11.glEnable(GL11.GL_LIGHTING);
    }

    @Override
    protected void keyTyped(char c, int key) {
        if (amountField.textboxKeyTyped(c, key)) {
            try {
                int v = Integer.parseInt(amountField.getText());
                if (v >= 1 && v <= 8000) {
                    Titrator.network.sendToServer(new MessageSetFill(tile.xCoord, tile.yCoord, tile.zCoord, v));
                }
            } catch (NumberFormatException e) {}
        } else super.keyTyped(c, key);
    }

    @Override
    public void mouseClicked(int mx, int my, int btn) {
        super.mouseClicked(mx, my, btn);
        amountField.mouseClicked(mx, my, btn);
        if (btn == 1 && mx >= guiLeft + 110 && mx <= guiLeft + 150 && my >= guiTop + 24 && my <= guiTop + 36) {
            int preset = tile.getFillAmount() == 250 ? 1000 : 250;
            amountField.setText(String.valueOf(preset));
            Titrator.network.sendToServer(new MessageSetFill(tile.xCoord, tile.yCoord, tile.zCoord, preset));
        }
    }
}
