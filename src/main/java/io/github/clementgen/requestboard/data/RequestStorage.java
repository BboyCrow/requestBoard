package io.github.clementgen.requestboard.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.clementgen.requestboard.RequestBoard;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/** World-wide storage of board requests and of items waiting for offline players. */
public class RequestStorage extends SavedData {
	private record Pending(UUID player, List<ItemStack> items) {
		static final Codec<Pending> CODEC = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.fieldOf("player").forGetter(Pending::player),
			ItemStack.CODEC.listOf().fieldOf("items").forGetter(Pending::items)
		).apply(i, Pending::new));
	}

	/** Per-player counters used by the advancements. */
	public record PlayerStats(UUID player, int posted, int completed, boolean rewarded) {
		static final Codec<PlayerStats> CODEC = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.fieldOf("player").forGetter(PlayerStats::player),
			Codec.INT.optionalFieldOf("posted", 0).forGetter(PlayerStats::posted),
			Codec.INT.optionalFieldOf("completed", 0).forGetter(PlayerStats::completed),
			Codec.BOOL.optionalFieldOf("rewarded", false).forGetter(PlayerStats::rewarded)
		).apply(i, PlayerStats::new));
	}

	public static final Codec<RequestStorage> CODEC = RecordCodecBuilder.create(i -> i.group(
		BoardRequest.CODEC.listOf().optionalFieldOf("requests", List.of()).forGetter(s -> List.copyOf(s.requests.values())),
		Pending.CODEC.listOf().optionalFieldOf("pending_items", List.of()).forGetter(RequestStorage::pendingList),
		PlayerStats.CODEC.listOf().optionalFieldOf("player_stats", List.of()).forGetter(s -> List.copyOf(s.stats.values()))
	).apply(i, RequestStorage::new));

	public static final SavedDataType<RequestStorage> TYPE = new SavedDataType<>(
		Identifier.fromNamespaceAndPath(RequestBoard.MOD_ID, "requests"), RequestStorage::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	private final Map<UUID, BoardRequest> requests = new LinkedHashMap<>();
	private final Map<UUID, List<ItemStack>> pendingItems = new HashMap<>();
	private final Map<UUID, PlayerStats> stats = new HashMap<>();

	public RequestStorage() {
	}

	private RequestStorage(List<BoardRequest> requests, List<Pending> pending, List<PlayerStats> stats) {
		this(requests, pending);
		for (PlayerStats stat : stats) {
			this.stats.put(stat.player(), stat);
		}
	}

	private RequestStorage(List<BoardRequest> requests, List<Pending> pending) {
		for (BoardRequest request : requests) {
			this.requests.put(request.id(), request);
		}
		for (Pending p : pending) {
			this.pendingItems.computeIfAbsent(p.player(), k -> new ArrayList<>()).addAll(p.items().stream().map(ItemStack::copy).toList());
		}
	}

	private List<Pending> pendingList() {
		return this.pendingItems.entrySet().stream()
			.filter(e -> !e.getValue().isEmpty())
			.map(e -> new Pending(e.getKey(), List.copyOf(e.getValue())))
			.toList();
	}

	public PlayerStats stats(UUID player) {
		return this.stats.getOrDefault(player, new PlayerStats(player, 0, 0, false));
	}

	public PlayerStats addPosted(UUID player) {
		PlayerStats old = this.stats(player);
		return this.putStats(new PlayerStats(player, old.posted() + 1, old.completed(), old.rewarded()));
	}

	public PlayerStats addCompleted(UUID player, boolean rewarded) {
		PlayerStats old = this.stats(player);
		return this.putStats(new PlayerStats(player, old.posted(), old.completed() + 1, old.rewarded() || rewarded));
	}

	private PlayerStats putStats(PlayerStats stats) {
		this.stats.put(stats.player(), stats);
		this.setDirty();
		return stats;
	}

	public Collection<BoardRequest> requests() {
		return this.requests.values();
	}

	public @Nullable BoardRequest get(UUID id) {
		return this.requests.get(id);
	}

	public void add(BoardRequest request) {
		this.requests.put(request.id(), request);
		this.setDirty();
	}

	public @Nullable BoardRequest remove(UUID id) {
		BoardRequest removed = this.requests.remove(id);
		if (removed != null) {
			this.setDirty();
		}
		return removed;
	}

	public void addPending(UUID player, List<ItemStack> items) {
		if (items.isEmpty()) return;
		this.pendingItems.computeIfAbsent(player, k -> new ArrayList<>()).addAll(items);
		this.setDirty();
	}

	public List<ItemStack> takePending(UUID player) {
		List<ItemStack> items = this.pendingItems.remove(player);
		if (items == null) return List.of();
		this.setDirty();
		return items;
	}
}
