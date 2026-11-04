package com.sharalyn.titrator;

import net.minecraft.tileentity.TileEntity;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class MessageSetFill implements IMessage {

    public int x, y, z, amount;

    public MessageSetFill() {}

    public MessageSetFill(int x, int y, int z, int a) {
        this.x = x;
        this.y = y;
        this.z = z;
        amount = a;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(amount);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        amount = buf.readInt();
    }

    public static class Handler implements IMessageHandler<MessageSetFill, IMessage> {

        @Override
        public IMessage onMessage(MessageSetFill msg, MessageContext ctx) {
            TileEntity te = ctx.getServerHandler().playerEntity.worldObj.getTileEntity(msg.x, msg.y, msg.z);
            if (te instanceof TileEntityTitrator) ((TileEntityTitrator) te).setFillAmount(msg.amount);
            return null;
        }
    }
}
