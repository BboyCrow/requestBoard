package io.github.bboycrow.requestboard.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

/**
 * A player who accepted a request. {@code contribution} is what they say they have gathered for each
 * requested item. It is only informative: nothing is taken from their inventory.
 */
public record Acceptor(UUID id, String name, long acceptedAt, List<ObjectiveEntry> contribution, boolean confirmed) {
	public static final Codec<Acceptor> CODEC = RecordCodecBuilder.create(i -> i.group(
		UUIDUtil.CODEC.fieldOf("id").forGetter(Acceptor::id),
		Codec.STRING.fieldOf("name").forGetter(Acceptor::name),
		Codec.LONG.fieldOf("accepted_at").forGetter(Acceptor::acceptedAt),
		ObjectiveEntry.CODEC.listOf().optionalFieldOf("contribution", List.of()).forGetter(Acceptor::contribution),
		Codec.BOOL.optionalFieldOf("confirmed", false).forGetter(Acceptor::confirmed)
	).apply(i, Acceptor::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, Acceptor> STREAM_CODEC = StreamCodec.composite(
		UUIDUtil.STREAM_CODEC, Acceptor::id,
		ByteBufCodecs.STRING_UTF8, Acceptor::name,
		ByteBufCodecs.VAR_LONG, Acceptor::acceptedAt,
		ObjectiveEntry.STREAM_CODEC.apply(ByteBufCodecs.list(64)), Acceptor::contribution,
		ByteBufCodecs.BOOL, Acceptor::confirmed,
		Acceptor::new
	);

	public Acceptor {
		contribution = List.copyOf(contribution);
	}

	public Acceptor(UUID id, String name, long acceptedAt) {
		this(id, name, acceptedAt, List.of(), false);
	}

	/** New amounts; {@code confirmed} is true when the request's creator set them. */
	public Acceptor withContribution(List<ObjectiveEntry> contribution, boolean confirmed) {
		return new Acceptor(this.id, this.name, this.acceptedAt, contribution, confirmed);
	}

	/** Amount this acceptor declared for an item (0 if none). */
	public int amountFor(Holder<Item> item) {
		int total = 0;
		for (ObjectiveEntry entry : this.contribution) {
			if (entry.item().value() == item.value()) total += entry.count();
		}
		return total;
	}

	public boolean hasContribution() {
		return this.contribution.stream().anyMatch(e -> e.count() > 0);
	}
}
