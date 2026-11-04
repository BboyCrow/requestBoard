package io.github.bboycrow.requestboard.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Acceptor(UUID id, String name, long acceptedAt) {
	public static final Codec<Acceptor> CODEC = RecordCodecBuilder.create(i -> i.group(
		UUIDUtil.CODEC.fieldOf("id").forGetter(Acceptor::id),
		Codec.STRING.fieldOf("name").forGetter(Acceptor::name),
		Codec.LONG.fieldOf("accepted_at").forGetter(Acceptor::acceptedAt)
	).apply(i, Acceptor::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, Acceptor> STREAM_CODEC = StreamCodec.composite(
		UUIDUtil.STREAM_CODEC, Acceptor::id,
		ByteBufCodecs.STRING_UTF8, Acceptor::name,
		ByteBufCodecs.VAR_LONG, Acceptor::acceptedAt,
		Acceptor::new
	);
}
