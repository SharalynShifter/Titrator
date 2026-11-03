package com.sharalyn.titrator;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;

public class TitratorConfigGui extends GuiConfig {

    public TitratorConfigGui(GuiScreen parent) {
        super(
            parent,
            getConfigElements(),
            Titrator.MODID,
            false,
            false,
            GuiConfig.getAbridgedConfigPath(TitratorConfig.config.toString()));
    }

    private static List<IConfigElement> getConfigElements() {
        return new ConfigElement(TitratorConfig.config.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements();
    }
}
