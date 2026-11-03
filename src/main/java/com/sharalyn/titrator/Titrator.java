package com.sharalyn.titrator;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;

@Mod(
    modid = Titrator.MODID,
    name = "Titrator",
    version = "1.2",
    dependencies = "after:CoFHAPI|energy;after:Railcraft;after:ImmersiveEngineering;after:ThermalFoundation;after:ThermalExpansion;after:ThermalDynamics",
    guiFactory = "com.sharalyn.titrator.TitratorGuiFactory")
public class Titrator {

    public static final String MODID = "titrator";
    public static Block blockTitrator;
    @Mod.Instance(MODID)
    public static Titrator instance;
    @SidedProxy(clientSide = "com.sharalyn.titrator.ClientProxy", serverSide = "com.sharalyn.titrator.CommonProxy")
    public static CommonProxy proxy;
    public static SimpleNetworkWrapper network;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        TitratorConfig.init(event.getSuggestedConfigurationFile());
        FMLCommonHandler.instance()
            .bus()
            .register(this);

        proxy.preInit(event);
        network = NetworkRegistry.INSTANCE.newSimpleChannel("titrator");
        network.registerMessage(MessageSetFill.Handler.class, MessageSetFill.class, 0, Side.SERVER);
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
        blockTitrator = new BlockTitrator().setBlockName("titrator")
            .setCreativeTab(CreativeTabs.tabRedstone);
        GameRegistry.registerBlock(blockTitrator, "titrator");
        GameRegistry.registerTileEntity(TileEntityTitrator.class, "tileTitrator");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.registerRenderers();
        // Recipe as requested:
        // B G B
        // G Bu G
        // P R P
        // B=bottle, G=glass pane, Bu=bucket, P=piston, R=redstone block OR machine frame (any tier)

        // Base recipe with Redstone Block - always available
        GameRegistry.addRecipe(
            new ItemStack(blockTitrator),
            "BGB",
            "GUG",
            "PRP",
            'B',
            Items.glass_bottle,
            'G',
            Blocks.glass_pane,
            'U',
            Items.bucket,
            'P',
            Blocks.piston,
            'R',
            Blocks.redstone_block);

        // Try to find all machine frame variants - same shape, frame instead of redstone block
        try {
            // Method 1: Direct ItemStack lookup
            String[] modIds = { "ThermalFoundation", "ThermalExpansion" };
            String[] names = { "Frame", "MachineFrame", "frame", "machineFrame", "FrameBasic" };
            for (String modId : modIds) {
                for (String name : names) {
                    ItemStack stack = GameRegistry.findItemStack(modId, name, 1);
                    if (stack != null) {
                        GameRegistry.addRecipe(
                            new ItemStack(blockTitrator),
                            "BGB",
                            "GUG",
                            "PRP",
                            'B',
                            Items.glass_bottle,
                            'G',
                            Blocks.glass_pane,
                            'U',
                            Items.bucket,
                            'P',
                            Blocks.piston,
                            'R',
                            stack);
                    }
                }
            }
            // Method 2: Block lookup -> ItemStack (Thermal Foundation Frame is a Block)
            for (String modId : modIds) {
                for (String name : names) {
                    Block b = GameRegistry.findBlock(modId, name);
                    if (b != null) {
                        for (int meta = 0; meta <= 5; meta++) {
                            ItemStack metaFrame = new ItemStack(b, 1, meta);
                            GameRegistry.addRecipe(
                                new ItemStack(blockTitrator),
                                "BGB",
                                "GUG",
                                "PRP",
                                'B',
                                Items.glass_bottle,
                                'G',
                                Blocks.glass_pane,
                                'U',
                                Items.bucket,
                                'P',
                                Blocks.piston,
                                'R',
                                metaFrame);
                        }
                        // Also wildcard
                        GameRegistry.addRecipe(
                            new ItemStack(blockTitrator),
                            "BGB",
                            "GUG",
                            "PRP",
                            'B',
                            Items.glass_bottle,
                            'G',
                            Blocks.glass_pane,
                            'U',
                            Items.bucket,
                            'P',
                            Blocks.piston,
                            'R',
                            new ItemStack(b, 1, 32767));
                    }
                }
            }
            // Method 3: Item lookup
            for (String modId : modIds) {
                for (String name : names) {
                    net.minecraft.item.Item it = GameRegistry.findItem(modId, name);
                    if (it != null) {
                        for (int meta = 0; meta <= 5; meta++) {
                            ItemStack metaFrame = new ItemStack(it, 1, meta);
                            GameRegistry.addRecipe(
                                new ItemStack(blockTitrator),
                                "BGB",
                                "GUG",
                                "PRP",
                                'B',
                                Items.glass_bottle,
                                'G',
                                Blocks.glass_pane,
                                'U',
                                Items.bucket,
                                'P',
                                Blocks.piston,
                                'R',
                                metaFrame);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.modID.equals(MODID)) {
            TitratorConfig.syncFromFile();
        }
    }
}
