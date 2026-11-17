package com.Maul.lotmmi.network.packets.toClient;

import com.Maul.lotmmi.LotmMysticalItems;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record ArrodesReplyPacket(List<String> lines, int kind) implements CustomPacketPayload {

    public static final int KIND_ANSWER = 0;
    public static final int KIND_NOTE = 1;
    public static final int KIND_QUESTION = 2;
    public static final int KIND_DONE = 3;
    public static final int KIND_LOCKED = 4;

    private static final StreamCodec<ByteBuf, List<String>> STRINGS =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());

    public static final Type<ArrodesReplyPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "arrodes_reply"));

    public static final StreamCodec<FriendlyByteBuf, ArrodesReplyPacket> STREAM_CODEC = StreamCodec.composite(
            STRINGS, ArrodesReplyPacket::lines,
            ByteBufCodecs.VAR_INT, ArrodesReplyPacket::kind,
            ArrodesReplyPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ArrodesReplyPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> com.Maul.lotmmi.client.ClientPacketHandler.arrodesReply(packet));
    }
}
