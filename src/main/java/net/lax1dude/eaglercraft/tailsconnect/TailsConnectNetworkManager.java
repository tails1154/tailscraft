package net.lax1dude.eaglercraft.tailsconnect;

import net.minecraft.network.SingleplayerNetworkManager;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.util.text.ITextComponent;

/** Decodes the guest's addressed packets with the normal integrated-server client codec. */
public final class TailsConnectNetworkManager extends SingleplayerNetworkManager {
    public TailsConnectNetworkManager() {
        super(TailsConnectClient.SERVER);
        isPlayerChannelOpen = true;
    }

    @Override public void connect() { }
    @Override public boolean isLocalChannel() { return false; }

    @Override public void sendPacket(Packet packet) {
        if (!isPlayerChannelOpen) return;
        try {
            temporaryBuffer.clear();
            temporaryBuffer.writeVarIntToBuffer(packetState.getPacketId(EnumPacketDirection.SERVERBOUND, packet));
            packet.writePacketData(temporaryBuffer);
            byte[] bytes = new byte[temporaryBuffer.writerIndex()];
            temporaryBuffer.getBytes(0, bytes);
            TailsConnectClient.sendGame(bytes);
        } catch (Exception ex) {
            closeChannel(new net.minecraft.util.text.TextComponentString("Could not send game packet"));
        }
    }

    @Override public void closeChannel(ITextComponent reason) {
        if (!isPlayerChannelOpen) return;
        isPlayerChannelOpen = false;
        clearRecieveQueue();
        TailsConnectClient.reset();
        doClientDisconnect(reason);
    }
}
