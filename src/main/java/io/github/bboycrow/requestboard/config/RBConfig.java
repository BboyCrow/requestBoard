package io.github.bboycrow.requestboard.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import io.github.bboycrow.requestboard.RequestBoard;
import io.github.bboycrow.requestboard.data.Category;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Mod configuration, stored as JSON in {@code config/requestboard.json}.
 * The server's copy is authoritative and is synced to clients on join and on reload.
 */
public class RBConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	public enum RemainderMode { RANDOM, FIRST_COME, CREATOR }

	/** How withdrawn rewards are shared on close. */
	public enum SplitMode { EVEN, PRORATA }

	public static class Categories {
		public boolean announcement = true;
		public boolean service = true;
		public boolean harvest = true;
	}

	public static class Rewards {
		public boolean enabled = true;
		public boolean autoWithdraw = true;
		public boolean autoDispatch = true;
		public RemainderMode remainderMode = RemainderMode.RANDOM;
		public SplitMode splitMode = SplitMode.EVEN;
		public int maxRewardStacks = 9;
		public boolean textEnabled = true;
		public int maxTextLength = 60;
	}

	/** What the creator of a request may change afterwards. */
	public static class Editing {
		public boolean enabled = true;
		public boolean category = true;
		public boolean title = true;
		public boolean details = true;
		public boolean objectives = true;
		public boolean rewards = true;
		public boolean timeLimit = true;
	}

	/** Players who accepted a request can say how much of each item they have (informative only). */
	public static class Contributions {
		public boolean enabled = true;
		public boolean creatorStock = true;
		public boolean creatorCanAdjust = true;
	}

	public static class TimeLimit {
		public boolean enabled = true;
		public int maxMinutes = 0;
	}

	/** Human-readable help written into the file; ignored when read. */
	public Map<String, String> _help = help();

	public int maxTitleLength = 40;
	public int maxDetailsLength = 300;
	public int maxActiveRequestsPerPlayer = 5;
	public int maxObjectiveEntries = 5;
	public int maxStacksPerObjective = 0;
	public boolean harvestRequiresObjective = true;
	public int maxAcceptors = 0;
	public boolean allowAcceptOwnRequest = false;
	public boolean operatorsCanRemoveAny = true;
	public boolean broadcastNewRequests = true;
	public boolean playSounds = true;
	public String defaultStyle = "MODERN";
	public Categories categories = new Categories();
	public Rewards rewards = new Rewards();
	public TimeLimit timeLimit = new TimeLimit();
	public Editing editing = new Editing();
	public Contributions contributions = new Contributions();

	private static RBConfig instance = new RBConfig();

	public static RBConfig get() {
		return instance;
	}

	/** Replaces the active config (used client-side when the server syncs its config). */
	public static void set(RBConfig config) {
		instance = config.sanitized();
	}

	/** Replaces the active config and writes it to disk (server side, from the in-game editor). */
	public static RBConfig replaceAndSave(RBConfig config) {
		instance = config.sanitized();
		instance.save();
		return instance;
	}

	/** A detached copy, safe to edit without touching the active config. */
	public RBConfig copy() {
		return fromJson(this.toJson());
	}

	public boolean canEditAnything() {
		return this.editing.enabled && (this.editing.category || this.editing.title || this.editing.details
			|| this.editing.objectives || this.editing.rewards || this.editing.timeLimit);
	}

	public boolean textRewardsEnabled() {
		return this.rewards.textEnabled;
	}

	public boolean isCategoryEnabled(Category category) {
		return switch (category) {
			case ANNOUNCEMENT -> this.categories.announcement;
			case SERVICE -> this.categories.service;
			case HARVEST -> this.categories.harvest;
		};
	}

	/** Rewards are only dispatched automatically when they were withdrawn at creation. */
	public boolean effectiveAutoDispatch() {
		return this.rewards.enabled && this.rewards.autoWithdraw && this.rewards.autoDispatch;
	}

	/** Max amount for one requested item, or {@link Integer#MAX_VALUE} when unlimited. */
	public int maxCountFor(int maxStackSize) {
		if (this.maxStacksPerObjective <= 0) {
			return 999_999;
		}
		return this.maxStacksPerObjective * Math.max(1, maxStackSize);
	}

	public String toJson() {
		return GSON.toJson(this);
	}

	public static RBConfig fromJson(String json) {
		RBConfig config = GSON.fromJson(json, RBConfig.class);
		return config == null ? new RBConfig() : config.sanitized();
	}

	private RBConfig sanitized() {
		if (this.categories == null) this.categories = new Categories();
		if (this.rewards == null) this.rewards = new Rewards();
		if (this.rewards.remainderMode == null) this.rewards.remainderMode = RemainderMode.RANDOM;
		if (this.rewards.splitMode == null) this.rewards.splitMode = SplitMode.EVEN;
		if (this.timeLimit == null) this.timeLimit = new TimeLimit();
		if (this.editing == null) this.editing = new Editing();
		if (this.contributions == null) this.contributions = new Contributions();
		if (this.defaultStyle == null) this.defaultStyle = "MODERN";
		this.maxTitleLength = clamp(this.maxTitleLength, 8, 128);
		this.maxDetailsLength = clamp(this.maxDetailsLength, 0, 2000);
		this.maxActiveRequestsPerPlayer = Math.max(0, this.maxActiveRequestsPerPlayer);
		this.maxObjectiveEntries = clamp(this.maxObjectiveEntries, 1, 32);
		this.maxStacksPerObjective = Math.max(0, this.maxStacksPerObjective);
		this.maxAcceptors = Math.max(0, this.maxAcceptors);
		this.rewards.maxRewardStacks = clamp(this.rewards.maxRewardStacks, 1, 9);
		this.timeLimit.maxMinutes = Math.max(0, this.timeLimit.maxMinutes);
		this.rewards.maxTextLength = clamp(this.rewards.maxTextLength, 8, 200);
		this.defaultStyle = this.defaultStyle.trim().toUpperCase(java.util.Locale.ROOT);
		if (!"MODERN".equals(this.defaultStyle) && !"FANTASY".equals(this.defaultStyle)) this.defaultStyle = "MODERN";
		if (!this.categories.announcement && !this.categories.service && !this.categories.harvest) {
			this.categories.harvest = true;
		}
		this._help = help();
		return this;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	public static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(RequestBoard.MOD_ID + ".json");
	}

	/** Loads the config from disk (creating it with defaults if missing) and rewrites it with any new keys. */
	public static RBConfig load() {
		Path path = path();
		RBConfig config = new RBConfig();
		if (Files.exists(path)) {
			try {
				config = fromJson(Files.readString(path, StandardCharsets.UTF_8));
			} catch (IOException | JsonParseException e) {
				RequestBoard.LOGGER.error("Could not read {}, using defaults", path, e);
				config = new RBConfig();
			}
		}
		instance = config.sanitized();
		instance.save();
		return instance;
	}

	public void save() {
		try {
			Files.createDirectories(path().getParent());
			Files.writeString(path(), this.toJson(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			RequestBoard.LOGGER.error("Could not write {}", path(), e);
		}
	}

	private static Map<String, String> help() {
		Map<String, String> help = new LinkedHashMap<>();
		help.put("maxTitleLength", "Max characters in a request title (8-128).");
		help.put("maxDetailsLength", "Max characters in the details text (0 disables details).");
		help.put("maxActiveRequestsPerPlayer", "Open requests a player can have at once (0 = unlimited).");
		help.put("maxObjectiveEntries", "Different items a request can ask for (1-32).");
		help.put("maxStacksPerObjective", "Max stacks per requested item (0 = unlimited).");
		help.put("harvestRequiresObjective", "Harvest requests must list at least one item.");
		help.put("maxAcceptors", "Players who can accept the same request (0 = unlimited).");
		help.put("allowAcceptOwnRequest", "Let a player accept their own request.");
		help.put("operatorsCanRemoveAny", "Operators can delete any request (rewards go back to the creator).");
		help.put("broadcastNewRequests", "Announce new requests in chat to every player.");
		help.put("playSounds", "Play a sound on board events.");
		help.put("defaultStyle", "Board look for players who have not picked one: MODERN (sticky notes) or FANTASY (old quest board).");
		help.put("categories", "Enable or disable each category (at least one stays enabled).");
		help.put("rewards.enabled", "Allow rewards (items from the creator's inventory only).");
		help.put("rewards.autoWithdraw", "Take reward items from the creator's inventory when the request is posted. If false, rewards are only shown and handed over manually.");
		help.put("rewards.autoDispatch", "When the creator closes the request, split the withdrawn rewards evenly between everyone who accepted it. Ignored if autoWithdraw is false. If false, rewards return to the creator.");
		help.put("rewards.splitMode", "EVEN: equal shares. PRORATA: shares in proportion to what each participant declared in their contribution (participants who declared nothing get nothing; equal shares if nobody declared anything).");
		help.put("rewards.remainderMode", "Who gets the leftover items when the split is uneven: RANDOM, FIRST_COME (earliest acceptors) or CREATOR (back to the creator).");
		help.put("rewards.maxRewardStacks", "Max reward stacks per request (1-9).");
		help.put("rewards.textEnabled", "Allow a short text reward (e.g. \"a free house build\"), shown on the request. Not handed out automatically.");
		help.put("rewards.maxTextLength", "Max characters of the text reward (8-200).");
		help.put("editing.enabled", "Let the creator edit their request after posting it (Edit button in the request view).");
		help.put("editing.category / title / details / objectives / rewards / timeLimit", "Which parts of a request can be edited. Changing rewards returns the old withdrawn items and takes the new ones. The time limit can be changed, added or removed.");
		help.put("contributions.creatorStock", "The creator can say how many of each item they already have; it counts in the progress.");
		help.put("contributions.creatorCanAdjust", "The creator can correct each participant's contribution (marked as confirmed).");
		help.put("contributions.enabled", "Players who accepted a request can say how many of each requested item they have, so everyone sees what is still missing. Informative only: nothing is taken from their inventory.");
		help.put("timeLimit.enabled", "Allow requests with a time limit; they disappear when it runs out and rewards go back to the creator.");
		help.put("timeLimit.maxMinutes", "Longest allowed time limit in minutes (0 = no maximum).");
		return help;
	}
}
