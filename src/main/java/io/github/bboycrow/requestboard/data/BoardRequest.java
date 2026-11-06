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
		Acceptor.CODEC.listOf().optionalFieldOf("acceptors", List.of()).forGetter(BoardRequest::acceptors),
		Codec.STRING.optionalFieldOf("reward_text", "").forGetter(BoardRequest::rewardText),
		ObjectiveEntry.CODEC.listOf().optionalFieldOf("own_stock", List.of()).forGetter(BoardRequest::ownStock)
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
	private final String rewardText;
	/** What the creator says they already have of each requested item. */
	private List<ObjectiveEntry> ownStock;

	public BoardRequest(UUID id, UUID creator, String creatorName, Category category, String title, String details,
			List<ObjectiveEntry> objectives, List<ItemStack> rewards, boolean rewardsEscrowed, long createdAt, long expiresAt,
			List<Acceptor> acceptors) {
		this(id, creator, creatorName, category, title, details, objectives, rewards, rewardsEscrowed, createdAt, expiresAt, acceptors, "");
	}

	public BoardRequest(UUID id, UUID creator, String creatorName, Category category, String title, String details,
			List<ObjectiveEntry> objectives, List<ItemStack> rewards, boolean rewardsEscrowed, long createdAt, long expiresAt,
			List<Acceptor> acceptors, String rewardText) {
		this(id, creator, creatorName, category, title, details, objectives, rewards, rewardsEscrowed, createdAt, expiresAt, acceptors, rewardText, List.of());
	}

	public BoardRequest(UUID id, UUID creator, String creatorName, Category category, String title, String details,
			List<ObjectiveEntry> objectives, List<ItemStack> rewards, boolean rewardsEscrowed, long createdAt, long expiresAt,
			List<Acceptor> acceptors, String rewardText, List<ObjectiveEntry> ownStock) {
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
		this.rewardText = rewardText == null ? "" : rewardText;
		this.ownStock = List.copyOf(ownStock);
	}

	/** Same request with new content; id, creator, dates and acceptors are kept. */
	public BoardRequest edited(Category category, String title, String details, List<ObjectiveEntry> objectives,
			List<ItemStack> rewards, boolean rewardsEscrowed, String rewardText, long expiresAt) {
		// Contributions only keep the items still asked for, capped at the new amounts.
		List<Acceptor> kept = new ArrayList<>();
		for (Acceptor acceptor : this.acceptors) {
			kept.add(acceptor.withContribution(capTo(objectives, acceptor.contribution()), acceptor.confirmed()));
		}
		return new BoardRequest(this.id, this.creator, this.creatorName, category, title, details, objectives, rewards,
			rewardsEscrowed, this.createdAt, expiresAt, kept, rewardText, capTo(objectives, this.ownStock));
	}

	/** Keeps only the requested items, each capped at the requested amount. */
	public static List<ObjectiveEntry> capTo(List<ObjectiveEntry> objectives, List<ObjectiveEntry> declared) {
		List<ObjectiveEntry> out = new ArrayList<>();
		for (ObjectiveEntry wanted : objectives) {
			int amount = 0;
			for (ObjectiveEntry entry : declared) {
				if (entry.item().value() == wanted.item().value()) amount = Math.max(amount, entry.count());
			}
			amount = Math.max(0, Math.min(amount, wanted.count()));
			if (amount > 0) out.add(new ObjectiveEntry(wanted.item(), amount));
		}
		return out;
	}

	public static int amountIn(List<ObjectiveEntry> entries, ObjectiveEntry wanted) {
		int total = 0;
		for (ObjectiveEntry entry : entries) {
			if (entry.item().value() == wanted.item().value()) total += entry.count();
		}
		return total;
	}

	/** Total for a requested item: the creator's stock plus what all acceptors declared. */
	public int contributedFor(ObjectiveEntry wanted) {
		int total = amountIn(this.ownStock, wanted);
		for (Acceptor acceptor : this.acceptors) total += acceptor.amountFor(wanted.item());
		return total;
	}

	/** Share of the objective covered by contributions, from 0 to 1. */
	public float progress() {
		long needed = 0;
		long covered = 0;
		for (ObjectiveEntry wanted : this.objectives) {
			needed += wanted.count();
			covered += Math.min(wanted.count(), this.contributedFor(wanted));
		}
		return needed == 0 ? 0 : (float) covered / needed;
	}

	public boolean hasContributions() {
		return !this.ownStock.isEmpty() || this.acceptors.stream().anyMatch(Acceptor::hasContribution);
	}

	public List<ObjectiveEntry> ownStock() {
		return this.ownStock;
	}

	public void setOwnStock(List<ObjectiveEntry> stock) {
		this.ownStock = List.copyOf(stock);
	}

	public void replaceAcceptor(Acceptor acceptor) {
		this.acceptors.replaceAll(a -> a.id().equals(acceptor.id()) ? acceptor : a);
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
		ByteBufCodecs.STRING_UTF8.encode(buf, r.rewardText);
		OBJECTIVES_STREAM.encode(buf, r.ownStock);
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
			ACCEPTORS_STREAM.decode(buf),
			ByteBufCodecs.STRING_UTF8.decode(buf),
			OBJECTIVES_STREAM.decode(buf)
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
	public String rewardText() { return this.rewardText; }

	public boolean hasAnyReward() {
		return !this.rewards.isEmpty() || !this.rewardText.isEmpty();
	}

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
