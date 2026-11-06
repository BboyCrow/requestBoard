package io.github.bboycrow.requestboard.server;

import io.github.bboycrow.requestboard.Compat;
import io.github.bboycrow.requestboard.RequestBoard;
import io.github.bboycrow.requestboard.config.RBConfig;
import io.github.bboycrow.requestboard.data.Acceptor;
import io.github.bboycrow.requestboard.data.BoardRequest;
import io.github.bboycrow.requestboard.data.Category;
import io.github.bboycrow.requestboard.data.ObjectiveEntry;
import io.github.bboycrow.requestboard.data.RequestStorage;
import io.github.bboycrow.requestboard.network.RBPayloads;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/** Server-side rules of the board: every change goes through here. */
public final class RequestManager {
	private static final int MAIN_INVENTORY_SIZE = Inventory.INVENTORY_SIZE;
	private static int tickCounter;

	private RequestManager() {
	}

	public static RequestStorage storage(MinecraftServer server) {
		return Compat.storage(server);
	}

	private static RBConfig config() {
		return RBConfig.get();
	}

	// ---- Sync ----

	public static void sendConfig(ServerPlayer player) {
		ServerPlayNetworking.send(player, new RBPayloads.SyncConfig(config().toJson()));
	}

	public static void sendRequests(ServerPlayer player) {
		List<BoardRequest> list = new ArrayList<>(storage(Compat.server(player)).requests());
		ServerPlayNetworking.send(player, new RBPayloads.SyncRequests(list, System.currentTimeMillis()));
	}

	public static void broadcastRequests(MinecraftServer server) {
		List<BoardRequest> list = new ArrayList<>(storage(server).requests());
		RBPayloads.SyncRequests payload = new RBPayloads.SyncRequests(list, System.currentTimeMillis());
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (ServerPlayNetworking.canSend(player, RBPayloads.SyncRequests.TYPE)) {
				ServerPlayNetworking.send(player, payload);
			}
		}
	}

	public static void broadcastConfig(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (ServerPlayNetworking.canSend(player, RBPayloads.SyncConfig.TYPE)) {
				sendConfig(player);
			}
		}
	}

	public static void openBoard(ServerPlayer player) {
		if (!ServerPlayNetworking.canSend(player, RBPayloads.OpenBoard.TYPE)) {
			player.sendSystemMessage(Component.literal("RequestBoard must be installed on the client to use the board.").withStyle(ChatFormatting.RED));
			return;
		}
		sendConfig(player);
		sendRequests(player);
		ServerPlayNetworking.send(player, RBPayloads.OpenBoard.INSTANCE);
	}

	public static void onJoin(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, RBPayloads.SyncConfig.TYPE)) {
			sendConfig(player);
			sendRequests(player);
		}
		BoardAdvancements.sync(player, storage(Compat.server(player)).stats(player.getUUID()));
		List<ItemStack> pending = storage(Compat.server(player)).takePending(player.getUUID());
		if (!pending.isEmpty()) {
			int total = 0;
			for (ItemStack stack : pending) {
				total += stack.getCount();
				give(player, stack);
			}
			player.sendSystemMessage(Component.translatable("requestboard.msg.pending_delivered", total).withStyle(ChatFormatting.GOLD));
		}
	}

	// ---- Creation and editing ----

	public static void create(ServerPlayer player, RBPayloads.CreateRequest packet) {
		Component error = packet.editId().isPresent() ? tryEdit(player, packet, packet.editId().get()) : tryCreate(player, packet);
		if (error != null) {
			ServerPlayNetworking.send(player, new RBPayloads.CreateResult(false, error));
		}
	}

	/** Content of a request, checked against the config. */
	private record Content(Category category, String title, String details, List<ObjectiveEntry> objectives, String rewardText) {
	}

	/** Validates the text parts of a request; returns an error, or null with the result in {@code out[0]}. */
	private static @Nullable Component validateContent(RBPayloads.CreateRequest packet, Content[] out) {
		RBConfig cfg = config();
		Category category = packet.category();
		if (!cfg.isCategoryEnabled(category)) {
			return Component.translatable("requestboard.error.category_disabled");
		}
		String title = sanitize(packet.title());
		if (title.isEmpty()) {
			return Component.translatable("requestboard.error.title_empty");
		}
		if (title.length() > cfg.maxTitleLength) {
			return Component.translatable("requestboard.error.title_too_long", cfg.maxTitleLength);
		}
		String details = packet.details().strip();
		if (details.length() > cfg.maxDetailsLength) {
			return Component.translatable("requestboard.error.details_too_long", cfg.maxDetailsLength);
		}
		String rewardText = sanitize(packet.rewardText());
		if (!rewardText.isEmpty() && !cfg.rewards.textEnabled) {
			return Component.translatable("requestboard.error.text_reward_disabled");
		}
		if (rewardText.length() > cfg.rewards.maxTextLength) {
			return Component.translatable("requestboard.error.text_reward_too_long", cfg.rewards.maxTextLength);
		}

		// Objectives: merge duplicates, validate amounts.
		Map<Holder<Item>, Integer> merged = new LinkedHashMap<>();
		for (ObjectiveEntry entry : packet.objectives()) {
			if (entry.item().value() == Items.AIR || entry.count() <= 0) {
				return Component.translatable("requestboard.error.invalid_objective");
			}
			merged.merge(entry.item(), entry.count(), Integer::sum);
		}
		if (merged.size() > cfg.maxObjectiveEntries) {
			return Component.translatable("requestboard.error.too_many_objectives", cfg.maxObjectiveEntries);
		}
		List<ObjectiveEntry> objectives = new ArrayList<>();
		for (Map.Entry<Holder<Item>, Integer> e : merged.entrySet()) {
			ObjectiveEntry entry = new ObjectiveEntry(e.getKey(), e.getValue());
			int max = cfg.maxCountFor(entry.maxStackSize());
			if (entry.count() > max) {
				return Component.translatable("requestboard.error.objective_too_large", entry.displayStack().getHoverName(), max);
			}
			objectives.add(entry);
		}
		if (category == Category.HARVEST && cfg.harvestRequiresObjective && objectives.isEmpty()) {
			return Component.translatable("requestboard.error.harvest_needs_objective");
		}
		out[0] = new Content(category, title, details, objectives, rewardText);
		return null;
	}

	/** Checks the reward picks against the player's inventory; returns an error, or null with the picks in {@code out}. */
	private static @Nullable Component validatePicks(ServerPlayer player, List<RBPayloads.RewardPick> rawPicks, Map<Integer, Integer> out) {
		RBConfig cfg = config();
		for (RBPayloads.RewardPick pick : rawPicks) {
			if (pick.slot() < 0 || pick.slot() >= MAIN_INVENTORY_SIZE || pick.count() <= 0) {
				return Component.translatable("requestboard.error.invalid_reward");
			}
			out.merge(pick.slot(), pick.count(), Integer::sum);
		}
		if (!out.isEmpty() && !cfg.rewards.enabled) {
			return Component.translatable("requestboard.error.rewards_disabled");
		}
		if (out.size() > cfg.rewards.maxRewardStacks) {
			return Component.translatable("requestboard.error.too_many_rewards", cfg.rewards.maxRewardStacks);
		}
		Inventory inventory = player.getInventory();
		for (Map.Entry<Integer, Integer> pick : out.entrySet()) {
			ItemStack inSlot = inventory.getItem(pick.getKey());
			if (inSlot.isEmpty() || inSlot.getCount() < pick.getValue()) {
				return Component.translatable("requestboard.error.reward_missing");
			}
		}
		return null;
	}

	/** Withdraws (or just copies, if withdrawal is off) the validated picks. */
	private static List<ItemStack> takePicks(ServerPlayer player, Map<Integer, Integer> picks, boolean withdraw) {
		Inventory inventory = player.getInventory();
		List<ItemStack> rewards = new ArrayList<>();
		for (Map.Entry<Integer, Integer> pick : picks.entrySet()) {
			ItemStack taken = withdraw
				? inventory.removeItem(pick.getKey(), pick.getValue())
				: inventory.getItem(pick.getKey()).copyWithCount(pick.getValue());
			if (!taken.isEmpty()) {
				rewards.add(taken);
			}
		}
		if (withdraw && !rewards.isEmpty()) {
			inventory.setChanged();
		}
		return rewards;
	}

	private static @Nullable Component tryCreate(ServerPlayer player, RBPayloads.CreateRequest packet) {
		RBConfig cfg = config();
		MinecraftServer server = Compat.server(player);
		RequestStorage storage = storage(server);

		Content[] content = new Content[1];
		Component error = validateContent(packet, content);
		if (error != null) {
			return error;
		}
		Content c = content[0];
		if (cfg.maxActiveRequestsPerPlayer > 0) {
			long active = storage.requests().stream().filter(r -> r.creator().equals(player.getUUID())).count();
			if (active >= cfg.maxActiveRequestsPerPlayer) {
				return Component.translatable("requestboard.error.too_many_requests", cfg.maxActiveRequestsPerPlayer);
			}
		}

		// Time limit.
		long now = System.currentTimeMillis();
		long expiresAt = 0;
		if (packet.durationMinutes() > 0) {
			if (!cfg.timeLimit.enabled) {
				return Component.translatable("requestboard.error.time_limit_disabled");
			}
			if (cfg.timeLimit.maxMinutes > 0 && packet.durationMinutes() > cfg.timeLimit.maxMinutes) {
				return Component.translatable("requestboard.error.time_limit_too_long", cfg.timeLimit.maxMinutes);
			}
			expiresAt = now + packet.durationMinutes() * 60_000L;
		}

		Map<Integer, Integer> picks = new LinkedHashMap<>();
		error = validatePicks(player, packet.rewards(), picks);
		if (error != null) {
			return error;
		}
		boolean withdraw = cfg.rewards.autoWithdraw;
		List<ItemStack> rewards = takePicks(player, picks, withdraw);

		BoardRequest request = new BoardRequest(
			UUID.randomUUID(), player.getUUID(), Compat.name(player), c.category(), c.title(), c.details(),
			c.objectives(), rewards, withdraw && !rewards.isEmpty(), now, expiresAt, List.of(), c.rewardText()
		);
		storage.add(request);
		BoardAdvancements.sync(player, storage.addPosted(player.getUUID()));

		ServerPlayNetworking.send(player, new RBPayloads.CreateResult(true, Component.translatable("requestboard.msg.created", c.title())));
		playSound(player, SoundEvents.BOOK_PUT);
		if (cfg.broadcastNewRequests) {
			Component announce = Component.translatable("requestboard.msg.broadcast_new", Compat.name(player),
				Component.translatable(c.category().translationKey()), c.title()).withStyle(ChatFormatting.GOLD);
			// Everyone, the creator included, plus the server console.
			for (ServerPlayer other : server.getPlayerList().getPlayers()) {
				other.sendSystemMessage(announce);
			}
			server.sendSystemMessage(announce);
		}
		broadcastRequests(server);
		return null;
	}

	private static @Nullable Component tryEdit(ServerPlayer player, RBPayloads.CreateRequest packet, UUID id) {
		RBConfig cfg = config();
		MinecraftServer server = Compat.server(player);
		RequestStorage storage = storage(server);
		BoardRequest old = storage.get(id);
		if (old == null) {
			return Component.translatable("requestboard.error.not_found");
		}
		if (!old.creator().equals(player.getUUID())) {
			return Component.translatable("requestboard.error.not_creator");
		}
		if (!cfg.editing.enabled) {
			return Component.translatable("requestboard.error.editing_disabled");
		}

		// Parts that may not be edited keep their old value, whatever the client sent.
		RBConfig.Editing can = cfg.editing;
		RBPayloads.CreateRequest merged = new RBPayloads.CreateRequest(
			packet.editId(),
			can.category ? packet.category() : old.category(),
			can.title ? packet.title() : old.title(),
			can.details ? packet.details() : old.details(),
			can.objectives ? packet.objectives() : old.objectives(),
			packet.rewards(),
			can.rewards && packet.replaceRewards(),
			can.rewards ? packet.rewardText() : old.rewardText(),
			packet.durationMinutes()
		);
		Content[] content = new Content[1];
		Component error = validateContent(merged, content);
		if (error != null) {
			return error;
		}
		Content c = content[0];

		List<ItemStack> rewards = old.rewards();
		boolean escrowed = old.rewardsEscrowed();
		if (merged.replaceRewards()) {
			Map<Integer, Integer> picks = new LinkedHashMap<>();
			error = validatePicks(player, merged.rewards(), picks);
			if (error != null) {
				return error;
			}
			// Take the new rewards first (the slots were checked), then give the old ones back.
			boolean withdraw = cfg.rewards.autoWithdraw;
			rewards = takePicks(player, picks, withdraw);
			returnRewards(server, old, storage);
			escrowed = withdraw && !rewards.isEmpty();
		}

		// Time limit: -1 keeps it, 0 removes it, more restarts it from now.
		long expiresAt = old.expiresAt();
		int minutes = merged.durationMinutes();
		if (can.timeLimit && minutes >= 0) {
			if (minutes == 0) {
				expiresAt = 0;
			} else {
				if (!cfg.timeLimit.enabled) {
					return Component.translatable("requestboard.error.time_limit_disabled");
				}
				if (cfg.timeLimit.maxMinutes > 0 && minutes > cfg.timeLimit.maxMinutes) {
					return Component.translatable("requestboard.error.time_limit_too_long", cfg.timeLimit.maxMinutes);
				}
				expiresAt = System.currentTimeMillis() + minutes * 60_000L;
			}
		}

		BoardRequest updated = old.edited(c.category(), c.title(), c.details(), c.objectives(), rewards, escrowed, c.rewardText(), expiresAt);
		storage.add(updated);
		ServerPlayNetworking.send(player, new RBPayloads.CreateResult(true, Component.translatable("requestboard.msg.edited", c.title())));
		playSound(player, SoundEvents.BOOK_PAGE_TURN);
		for (Acceptor acceptor : updated.acceptors()) {
			notify(server, acceptor.id(), Component.translatable("requestboard.msg.acceptor_edited", updated.creatorName(), c.title())
				.withStyle(ChatFormatting.YELLOW));
		}
		broadcastRequests(server);
		return null;
	}

	// ---- Contributions ----

	public static void updateContribution(ServerPlayer player, UUID requestId, UUID target, List<ObjectiveEntry> declared) {
		RBConfig cfg = config();
		MinecraftServer server = Compat.server(player);
		RequestStorage storage = storage(server);
		BoardRequest request = storage.get(requestId);
		if (request == null) {
			error(player, "requestboard.error.not_found");
			return;
		}
		if (!cfg.contributions.enabled) {
			error(player, "requestboard.error.contributions_disabled");
			return;
		}
		List<ObjectiveEntry> amounts = BoardRequest.capTo(request.objectives(), declared);
		boolean isCreator = request.creator().equals(player.getUUID());

		if (isCreator && target.equals(player.getUUID())) {
			// The creator's own stock.
			if (!cfg.contributions.creatorStock) {
				error(player, "requestboard.error.creator_stock_disabled");
				return;
			}
			request.setOwnStock(amounts);
		} else if (isCreator) {
			// The creator corrects a participant.
			if (!cfg.contributions.creatorCanAdjust) {
				error(player, "requestboard.error.creator_adjust_disabled");
				return;
			}
			Acceptor acceptor = request.acceptors().stream().filter(a -> a.id().equals(target)).findFirst().orElse(null);
			if (acceptor == null) {
				error(player, "requestboard.error.not_found");
				return;
			}
			request.replaceAcceptor(acceptor.withContribution(amounts, true));
			notify(server, acceptor.id(), Component.translatable("requestboard.msg.contribution_adjusted",
				Compat.name(player), request.title()).withStyle(ChatFormatting.AQUA));
		} else {
			// A participant's own contribution.
			if (!target.equals(player.getUUID())) {
				error(player, "requestboard.error.not_creator");
				return;
			}
			Acceptor acceptor = request.acceptors().stream().filter(a -> a.id().equals(player.getUUID())).findFirst().orElse(null);
			if (acceptor == null) {
				error(player, "requestboard.error.not_accepted");
				return;
			}
			request.replaceAcceptor(acceptor.withContribution(amounts, false));
			int percent = Math.round(request.progress() * 100);
			notify(server, request.creator(), Component.translatable("requestboard.msg.creator_contribution",
				Compat.name(player), request.title(), percent).withStyle(ChatFormatting.AQUA));
		}
		storage.setDirty();
		player.sendSystemMessage(Component.translatable("requestboard.msg.contribution_saved", request.title()).withStyle(ChatFormatting.GREEN));
		playSound(player, SoundEvents.BOOK_PAGE_TURN);
		broadcastRequests(server);
	}

	// ---- Config editor (operators) ----

	public static void openConfigEditor(ServerPlayer player) {
		if (!ServerPlayNetworking.canSend(player, RBPayloads.OpenConfig.TYPE)) {
			player.sendSystemMessage(Component.literal("RequestBoard must be installed on the client to use the config editor.").withStyle(ChatFormatting.RED));
			return;
		}
		ServerPlayNetworking.send(player, new RBPayloads.OpenConfig(config().toJson()));
	}

	public static void saveConfig(ServerPlayer player, String json) {
		if (!Compat.isOperator(player)) {
			RequestBoard.LOGGER.warn("{} tried to change the RequestBoard config without being an operator", Compat.name(player));
			error(player, "requestboard.error.not_operator");
			return;
		}
		RBConfig parsed;
		try {
			parsed = RBConfig.fromJson(json);
		} catch (RuntimeException e) {
			error(player, "requestboard.error.invalid_config");
			return;
		}
		RBConfig.replaceAndSave(parsed);
		MinecraftServer server = Compat.server(player);
		broadcastConfig(server);
		Component done = Component.translatable("requestboard.msg.config_saved", Compat.name(player)).withStyle(ChatFormatting.GREEN);
		player.sendSystemMessage(done);
		server.sendSystemMessage(done);
	}

	private static String sanitize(String text) {
		return text.replace('\n', ' ').replace('\r', ' ').strip();
	}

	// ---- Actions ----

	public static void handleAction(ServerPlayer player, UUID requestId, RBPayloads.Action action) {
		MinecraftServer server = Compat.server(player);
		RequestStorage storage = storage(server);
		BoardRequest request = storage.get(requestId);
		if (request == null) {
			player.sendSystemMessage(Component.translatable("requestboard.error.not_found").withStyle(ChatFormatting.RED));
			sendRequests(player);
			return;
		}
		switch (action) {
			case ACCEPT -> accept(player, request, storage);
			case LEAVE -> leave(player, request, storage);
			case CLOSE -> close(player, request, storage);
			case DELETE -> delete(player, request, storage);
		}
		broadcastRequests(server);
	}

	private static void accept(ServerPlayer player, BoardRequest request, RequestStorage storage) {
		RBConfig cfg = config();
		boolean own = request.creator().equals(player.getUUID());
		if (own && !cfg.allowAcceptOwnRequest) {
			error(player, "requestboard.error.accept_own");
			return;
		}
		if (request.isAcceptedBy(player.getUUID())) {
			error(player, "requestboard.error.already_accepted");
			return;
		}
		if (cfg.maxAcceptors > 0 && request.acceptors().size() >= cfg.maxAcceptors) {
			error(player, "requestboard.error.full");
			return;
		}
		request.addAcceptor(new Acceptor(player.getUUID(), Compat.name(player), System.currentTimeMillis()));
		storage.setDirty();
		if (!own) {
			BoardAdvancements.grant(player, BoardAdvancements.ACCEPT_REQUEST);
		}
		player.sendSystemMessage(Component.translatable("requestboard.msg.accepted", request.title()).withStyle(ChatFormatting.GREEN));
		playSound(player, SoundEvents.VILLAGER_WORK_CARTOGRAPHER);
		notify(Compat.server(player), request.creator(),
			Component.translatable("requestboard.msg.creator_accepted", Compat.name(player), request.title()).withStyle(ChatFormatting.GREEN));
	}

	private static void leave(ServerPlayer player, BoardRequest request, RequestStorage storage) {
		if (!request.removeAcceptor(player.getUUID())) {
			error(player, "requestboard.error.not_accepted");
			return;
		}
		storage.setDirty();
		player.sendSystemMessage(Component.translatable("requestboard.msg.left", request.title()).withStyle(ChatFormatting.YELLOW));
		notify(Compat.server(player), request.creator(),
			Component.translatable("requestboard.msg.creator_left", Compat.name(player), request.title()).withStyle(ChatFormatting.YELLOW));
	}

	private static void close(ServerPlayer player, BoardRequest request, RequestStorage storage) {
		if (!request.creator().equals(player.getUUID())) {
			error(player, "requestboard.error.not_creator");
			return;
		}
		MinecraftServer server = Compat.server(player);
		storage.remove(request.id());
		Set<UUID> rewarded = dispatchRewards(server, request, storage);
		boolean dispatched = !rewarded.isEmpty();
		player.sendSystemMessage(Component.translatable(dispatched ? "requestboard.msg.closed_dispatched" : "requestboard.msg.closed", request.title()).withStyle(ChatFormatting.GREEN));
		playSound(player, SoundEvents.EXPERIENCE_ORB_PICKUP);
		if (rewarded.size() >= 3) {
			BoardAdvancements.grant(player, BoardAdvancements.PATRON);
		}
		for (Acceptor acceptor : request.acceptors()) {
			boolean gotReward = rewarded.contains(acceptor.id());
			RequestStorage.PlayerStats stats = storage.addCompleted(acceptor.id(), gotReward);
			ServerPlayer online = server.getPlayerList().getPlayer(acceptor.id());
			if (online != null) {
				BoardAdvancements.sync(online, stats);
			}
			notify(server, acceptor.id(), Component.translatable(gotReward ? "requestboard.msg.acceptor_closed_rewarded" : "requestboard.msg.acceptor_closed",
				request.creatorName(), request.title()).withStyle(ChatFormatting.GREEN));
		}
	}

	private static void delete(ServerPlayer player, BoardRequest request, RequestStorage storage) {
		boolean isCreator = request.creator().equals(player.getUUID());
		boolean isOp = config().operatorsCanRemoveAny && Compat.isOperator(player);
		if (!isCreator && !isOp) {
			error(player, "requestboard.error.not_creator");
			return;
		}
		MinecraftServer server = Compat.server(player);
		storage.remove(request.id());
		returnRewards(server, request, storage);
		player.sendSystemMessage(Component.translatable("requestboard.msg.deleted", request.title()).withStyle(ChatFormatting.YELLOW));
		if (!isCreator) {
			notify(server, request.creator(), Component.translatable("requestboard.msg.deleted_by_op", request.title()).withStyle(ChatFormatting.YELLOW));
		}
		for (Acceptor acceptor : request.acceptors()) {
			notify(server, acceptor.id(), Component.translatable("requestboard.msg.acceptor_deleted", request.title()).withStyle(ChatFormatting.YELLOW));
		}
	}

	// ---- Expiry ----

	public static void tick(MinecraftServer server) {
		if (++tickCounter % 20 != 0) {
			return;
		}
		RequestStorage storage = storage(server);
		long now = System.currentTimeMillis();
		List<BoardRequest> expired = storage.requests().stream().filter(r -> r.hasTimeLimit() && r.expiresAt() <= now).toList();
		if (expired.isEmpty()) {
			return;
		}
		for (BoardRequest request : expired) {
			storage.remove(request.id());
			returnRewards(server, request, storage);
			notify(server, request.creator(), Component.translatable("requestboard.msg.expired", request.title()).withStyle(ChatFormatting.GRAY));
			for (Acceptor acceptor : request.acceptors()) {
				notify(server, acceptor.id(), Component.translatable("requestboard.msg.expired", request.title()).withStyle(ChatFormatting.GRAY));
			}
		}
		broadcastRequests(server);
	}

	// ---- Rewards ----

	private static void returnRewards(MinecraftServer server, BoardRequest request, RequestStorage storage) {
		if (request.rewardsEscrowed()) {
			deliver(server, storage, request.creator(), request.rewards());
		}
	}

	/** Splits escrowed rewards between acceptors. Returns the players who received something. */
	private static Set<UUID> dispatchRewards(MinecraftServer server, BoardRequest request, RequestStorage storage) {
		if (!request.rewardsEscrowed() || request.rewards().isEmpty()) {
			return Set.of();
		}
		RBConfig cfg = config();
		List<Acceptor> acceptors = new ArrayList<>(request.acceptors());
		if (!cfg.effectiveAutoDispatch() || acceptors.isEmpty()) {
			returnRewards(server, request, storage);
			return Set.of();
		}
		acceptors.sort(Comparator.comparingLong(Acceptor::acceptedAt));

		// Pool identical items (same item + components) so the split is per item type.
		List<ItemStack> pools = new ArrayList<>();
		List<Integer> totals = new ArrayList<>();
		for (ItemStack stack : request.rewards()) {
			int index = -1;
			for (int i = 0; i < pools.size(); i++) {
				if (ItemStack.isSameItemSameComponents(pools.get(i), stack)) {
					index = i;
					break;
				}
			}
			if (index < 0) {
				pools.add(stack.copyWithCount(1));
				totals.add(stack.getCount());
			} else {
				totals.set(index, totals.get(index) + stack.getCount());
			}
		}

		// Weight of each acceptor: 1 each (even split), or what they declared (pro rata).
		int k = acceptors.size();
		long[] weights = new long[k];
		java.util.Arrays.fill(weights, 1);
		if (cfg.rewards.splitMode == RBConfig.SplitMode.PRORATA && cfg.contributions.enabled) {
			long[] declared = new long[k];
			long sum = 0;
			for (int i = 0; i < k; i++) {
				for (ObjectiveEntry wanted : request.objectives()) {
					declared[i] += Math.min(acceptors.get(i).amountFor(wanted.item()), wanted.count());
				}
				sum += declared[i];
			}
			if (sum > 0) {
				weights = declared;
			}
		}
		long totalWeight = 0;
		List<Integer> eligible = new ArrayList<>();
		for (int i = 0; i < k; i++) {
			totalWeight += weights[i];
			if (weights[i] > 0) eligible.add(i);
		}

		Map<UUID, List<ItemStack>> shares = new LinkedHashMap<>();
		List<ItemStack> backToCreator = new ArrayList<>();
		for (int p = 0; p < pools.size(); p++) {
			ItemStack proto = pools.get(p);
			int total = totals.get(p);
			int[] amounts = new int[k];
			int given = 0;
			for (int i = 0; i < k; i++) {
				amounts[i] = (int) (total * weights[i] / totalWeight);
				given += amounts[i];
			}
			// Items that can't be split evenly (always fewer than the number of eligible players).
			int remainder = total - given;
			if (remainder > 0) {
				switch (cfg.rewards.remainderMode) {
					case FIRST_COME -> {
						for (int i = 0; i < remainder; i++) amounts[eligible.get(i % eligible.size())]++;
					}
					case RANDOM -> {
						List<Integer> order = new ArrayList<>(eligible);
						Collections.shuffle(order);
						for (int i = 0; i < remainder; i++) amounts[order.get(i % order.size())]++;
					}
					case CREATOR -> splitIntoStacks(proto, remainder, backToCreator);
				}
			}
			for (int i = 0; i < k; i++) {
				if (amounts[i] > 0) {
					splitIntoStacks(proto, amounts[i], shares.computeIfAbsent(acceptors.get(i).id(), id -> new ArrayList<>()));
				}
			}
		}
		for (Map.Entry<UUID, List<ItemStack>> share : shares.entrySet()) {
			deliver(server, storage, share.getKey(), share.getValue());
		}
		deliver(server, storage, request.creator(), backToCreator);
		return shares.keySet();
	}

	private static void splitIntoStacks(ItemStack proto, int amount, List<ItemStack> out) {
		int max = Math.max(1, proto.getMaxStackSize());
		while (amount > 0) {
			int n = Math.min(max, amount);
			out.add(proto.copyWithCount(n));
			amount -= n;
		}
	}

	/** Gives items to an online player, or keeps them until the player logs in. */
	private static void deliver(MinecraftServer server, RequestStorage storage, UUID playerId, List<ItemStack> items) {
		if (items.isEmpty()) return;
		ServerPlayer player = server.getPlayerList().getPlayer(playerId);
		if (player != null) {
			for (ItemStack stack : items) {
				give(player, stack.copy());
			}
		} else {
			storage.addPending(playerId, items.stream().map(ItemStack::copy).toList());
		}
	}

	private static void give(ServerPlayer player, ItemStack stack) {
		Compat.give(player, stack);
	}

	// ---- Messages ----

	private static void error(ServerPlayer player, String key) {
		player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
	}

	private static void notify(MinecraftServer server, UUID playerId, Component message) {
		ServerPlayer target = server.getPlayerList().getPlayer(playerId);
		if (target != null) {
			target.sendSystemMessage(message);
			playSound(target, SoundEvents.BOOK_PAGE_TURN);
		}
	}

	private static void playSound(ServerPlayer player, SoundEvent sound) {
		if (config().playSounds) {
			player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.7F, 1.0F);
		}
	}
}
