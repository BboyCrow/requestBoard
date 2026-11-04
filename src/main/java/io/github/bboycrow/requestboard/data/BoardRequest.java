package io.github.bboycrow.requestboard.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/** A request posted on the board. Only the acceptor list changes after creation. */
public final class BoardRequest {
	public static final Codec<BoardRequest> CODEC = RecordCodecBuilder.create(i -> i.group(
		UUIDUtil.CODEC.fieldOf("id").forGetter(BoardRequest::id),
		UUIDUtil.CODEC.fieldOf("creator").forGetter(BoardRequest::creator),
		Codec.STRING.fieldOf("creator_name").forGetter(BoardRequest::creatorName),
		Category.CODEC.fieldOf("category").forGetter(BoardRequest::category),
		Codec.STRING.fieldOf("title").forGetter(BoardRequest::title),
		Codec.STRING.optionalFieldOf("details", "").forGetter(BoardRequest::details),
		ObjectiveEntry.CODEC.listOf().optionalFieldOf("objectives", List.of()).forGetter(BoardRequest::objectives),
		ItemStack.CODEC.listOf().optionalFieldOf("rewards", List.of()).forGetter(BoardRequest::rewards),
		Codec.BOOL.optionalFieldOf("rewards_escrowed", false).forGetter(BoardRequest::rewardsEscrowed),
		Codec.LONG.fieldOf("created_at").forGetter(BoardRequest::createdAt),
		Codec.LONG.optionalFieldOf("expires_at", 0L).forGetter(BoardRequest::expiresAt),
		Acceptor.CODEC.listOf().optionalFieldOf("acceptors", List.of()).forGetter(BoardRequest::acceptors)
	).apply(i, BoardRequest::new));

	private static final StreamCodec<RegistryFriendlyByteBuf, List<ObjectiveEntry>> OBJECTIVES_STREAM = ObjectiveEntry.STREAM_CODEC.apply(ByteBufCodecs.list());
	private static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> STACKS_STREAM = ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list());
	private static final StreamCodec<RegistryFriendlyByteBuf, List<Acceptor>> ACCEPTORS_STREAM = Acceptor.STREAM_CODEC.apply(ByteBufCodecs.list());

	public static final StreamCodec<RegistryFriendlyByteBuf, BoardRequest> STREAM_CODEC = StreamCodec.of(BoardRequest::write, BoardRequest::read);

	private final UUID id;
	private final UUID creator;
	private final String creatorName;
	private final Category category;
	private final String title;
	private final String details;
	private final List<ObjectiveEntry> objectives;
	private final List<ItemStack> rewards;
	private final boolean rewardsEscrowed;
	private final long createdAt;
	private final long expiresAt;
	private final List<Acceptor> acceptors;

	public BoardRequest(UUID id, UUID creator, String creatorName, Category category, String title, String details,
			List<ObjectiveEntry> objectives, List<ItemStack> rewards, boolean rewardsEscrowed, long createdAt, long expiresAt,
			List<Acceptor> acceptors) {
		this.id = id;
		this.creator = creator;
		this.creatorName = creatorName;
		this.category = category;
		this.title = title;
		this.details = details;
		this.objectives = List.copyOf(objectives);
		this.rewards = rewards.stream().map(ItemStack::copy).toList();
		this.rewardsEscrowed = rewardsEscrowed;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
		this.acceptors = new ArrayList<>(acceptors);
	}

	private static void write(RegistryFriendlyByteBuf buf, BoardRequest r) {
		UUIDUtil.STREAM_CODEC.encode(buf, r.id);
		UUIDUtil.STREAM_CODEC.encode(buf, r.creator);
		ByteBufCodecs.STRING_UTF8.encode(buf, r.creatorName);
		Category.STREAM_CODEC.encode(buf, r.category);
		ByteBufCodecs.STRING_UTF8.encode(buf, r.title);
		ByteBufCodecs.STRING_UTF8.encode(buf, r.details);
		OBJECTIVES_STREAM.encode(buf, r.objectives);
		STACKS_STREAM.encode(buf, r.rewards);
		buf.writeBoolean(r.rewardsEscrowed);
		buf.writeLong(r.createdAt);
		buf.writeLong(r.expiresAt);
		ACCEPTORS_STREAM.encode(buf, r.acceptors);
	}

	private static BoardRequest read(RegistryFriendlyByteBuf buf) {
		return new BoardRequest(
			UUIDUtil.STREAM_CODEC.decode(buf),
			UUIDUtil.STREAM_CODEC.decode(buf),
			ByteBufCodecs.STRING_UTF8.decode(buf),
			Category.STREAM_CODEC.decode(buf),
			ByteBufCodecs.STRING_UTF8.decode(buf),
			ByteBufCodecs.STRING_UTF8.decode(buf),
			OBJECTIVES_STREAM.decode(buf),
			STACKS_STREAM.decode(buf),
			buf.readBoolean(),
			buf.readLong(),
			buf.readLong(),
			ACCEPTORS_STREAM.decode(buf)
		);
	}

	public UUID id() { return this.id; }
	public UUID creator() { return this.creator; }
	public String creatorName() { return this.creatorName; }
	public Category category() { return this.category; }
	public String title() { return this.title; }
	public String details() { return this.details; }
	public List<ObjectiveEntry> objectives() { return this.objectives; }
	public List<ItemStack> rewards() { return this.rewards; }
	public boolean rewardsEscrowed() { return this.rewardsEscrowed; }
	public long createdAt() { return this.createdAt; }
	public long expiresAt() { return this.expiresAt; }
	public List<Acceptor> acceptors() { return this.acceptors; }

	public boolean hasTimeLimit() {
		return this.expiresAt > 0;
	}

	public boolean isAcceptedBy(UUID player) {
		return this.acceptors.stream().anyMatch(a -> a.id().equals(player));
	}

	public boolean removeAcceptor(UUID player) {
		return this.acceptors.removeIf(a -> a.id().equals(player));
	}

	public void addAcceptor(Acceptor acceptor) {
		this.acceptors.add(acceptor);
	}
}
