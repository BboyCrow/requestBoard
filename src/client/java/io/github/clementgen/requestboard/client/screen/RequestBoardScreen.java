package io.github.clementgen.requestboard.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.clementgen.requestboard.Compat;
import io.github.clementgen.requestboard.RequestBoard;
import io.github.clementgen.requestboard.client.BoardStyle;
import io.github.clementgen.requestboard.client.ClientBoardState;
import io.github.clementgen.requestboard.config.RBConfig;
import io.github.clementgen.requestboard.data.Acceptor;
import io.github.clementgen.requestboard.data.BoardRequest;
import io.github.clementgen.requestboard.data.Category;
import io.github.clementgen.requestboard.data.ObjectiveEntry;
import io.github.clementgen.requestboard.network.RBPayloads;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.IntConsumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.world.item.component.ResolvableProfile;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.DefaultPlayerSkin;
*///?}
import org.jetbrains.annotations.Nullable;

/** The notice board: list of requests, creation page and "my contracts". */
public class RequestBoardScreen extends Screen {
	private static final Identifier PARCHMENT_TEX = RequestBoard.id("textures/gui/parchment.png");
	private static final Identifier PARCHMENT_LARGE_TEX = RequestBoard.id("textures/gui/parchment_large.png");

	private static final int W = 340;
	private static final int H = 240;
	private static final int PW = 260;
	private static final int PH = 220;
	private static final int LW = 328;
	private static final int LH = 228;

	private static final int CARD_H = 62;
	private static final int CARD_GAP = 6;
	private static final int ROW_H = 28;
	private static final int OBJ_ROWS = 5;
	private static final int PICKER_COLS = 12;
	private static final int PICKER_ROWS = 7;

	private static final int INK = 0xFF3B2A14;
	private static final int INK_SOFT = 0xFF6B5434;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int CREAM = 0xFFF2E3C0;
	private static final int GOLD = 0xFFFFD45A;
	private static final int GRAY = 0xFFB8B0A0;
	private static final int GREEN = 0xFF7CD67C;
	private static final int RED = 0xFFE86A5A;
	private static final int DARK_GREEN = 0xFF2E6B2E;
	private static final int DARK_RED = 0xFF9A2A20;
	private static final int CARD_BG = 0xD8201610;
	private static final int CARD_BG_HOVER = 0xE8302418;
	private static final int CARD_BORDER = 0xFF8A6A42;
	private static final int FIELD_BG = 0xFFF7EDD3;
	private static final int FIELD_BORDER = 0xFF9A7B4F;
	private static final int HINT = 0xFF9A8460;

	private enum Tab { BOARD, CREATE, MINE }

	private record ClickZone(int x, int y, int w, int h, IntConsumer action) {
		boolean contains(double mx, double my) {
			return mx >= this.x && mx < this.x + this.w && my >= this.y && my < this.y + this.h;
		}
	}

	private static final class ObjectiveDraft {
		final Holder<Item> item;
		String count;

		ObjectiveDraft(Holder<Item> item, String count) {
			this.item = item;
			this.count = count;
		}
	}

	private static @Nullable List<ItemStack> allItems;

	private final List<ClickZone> zones = new ArrayList<>();
	private final List<AbstractWidget> parchmentFields = new ArrayList<>();
	private final Map<AbstractWidget, Component> fieldHints = new HashMap<>();
	//? if >=26.1
	private final Map<UUID, ResolvableProfile> profiles = new HashMap<>();

	private Tab tab = Tab.BOARD;
	private int left;
	private int top;

	// Board / my contracts
	private @Nullable Category filterCategory;
	private @Nullable String filterPlayer;
	private double boardScroll;
	private double mineScroll;
	private @Nullable UUID selectedId;
	private @Nullable String pendingConfirm;
	private long pendingConfirmUntil;
	private @Nullable Component boardMessage;
	private long boardMessageUntil;

	// Creation draft (kept while the screen is open)
	private Category draftCategory;
	private String draftTitle = "";
	private String draftDetails = "";
	private String draftDuration = "";
	private final List<ObjectiveDraft> draftObjectives = new ArrayList<>();
	private final LinkedHashMap<Integer, Integer> draftRewards = new LinkedHashMap<>();
	private int objectiveScroll;
	private @Nullable Component createMessage;
	private boolean createMessageIsError;
	private boolean waitingForServer;

	// Item picker
	private boolean pickerOpen;
	private String pickerSearch = "";
	private double pickerScroll;
	private List<ItemStack> pickerResults = List.of();

	public RequestBoardScreen() {
		super(Component.translatable("requestboard.title"));
		this.draftCategory = firstEnabledCategory();
	}

	private static RBConfig cfg() {
		return RBConfig.get();
	}

	private static Category firstEnabledCategory() {
		for (Category c : Category.values()) {
			if (cfg().isCategoryEnabled(c)) return c;
		}
		return Category.HARVEST;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ------------------------------------------------------------------ server callbacks

	public void onRequestsUpdated() {
		if (this.selectedId != null && ClientBoardState.find(this.selectedId) == null) {
			this.selectedId = null;
		}
		if (this.filterPlayer != null && ClientBoardState.requests().stream().noneMatch(r -> r.creatorName().equals(this.filterPlayer))) {
			this.filterPlayer = null;
		}
		this.rebuildWidgets();
	}

	public void onConfigUpdated() {
		if (!cfg().isCategoryEnabled(this.draftCategory)) {
			this.draftCategory = firstEnabledCategory();
		}
		this.rebuildWidgets();
	}

	public void onCreateResult(boolean success, Component message) {
		this.waitingForServer = false;
		if (success) {
			this.draftTitle = "";
			this.draftDetails = "";
			this.draftDuration = "";
			this.draftObjectives.clear();
			this.draftRewards.clear();
			this.objectiveScroll = 0;
			this.createMessage = null;
			this.boardMessage = message;
			this.boardMessageUntil = System.currentTimeMillis() + 4000;
			this.tab = Tab.BOARD;
		} else {
			this.createMessage = message;
			this.createMessageIsError = true;
		}
		this.rebuildWidgets();
	}

	// ------------------------------------------------------------------ widgets

	@Override
	protected void init() {
		this.left = (this.width - W) / 2;
		this.top = Math.max(22, (this.height - H) / 2 + 10);

		this.parchmentFields.clear();
		this.fieldHints.clear();
		int tx = this.left + 2;
		for (Tab t : Tab.values()) {
			Button button = this.addRenderableWidget(Button.builder(tabLabel(t), b -> this.switchTab(t))
				.bounds(tx, this.top - 20, 104, 18).build());
			button.active = this.tab != t;
			tx += 106;
		}
		this.addRenderableWidget(Button.builder(Component.literal("✦"), b -> {
			BoardStyle.choose(BoardStyle.current().next());
			this.rebuildWidgets();
		}).bounds(this.left + W - 20, this.top - 20, 18, 18)
			.tooltip(Tooltip.create(Component.translatable("requestboard.style.button", Component.translatable(BoardStyle.current().translationKey()))))
			.build());

		if (this.selectedId != null && ClientBoardState.find(this.selectedId) != null && this.tab != Tab.CREATE) {
			this.initDetailButtons(ClientBoardState.find(this.selectedId));
			return;
		}
		switch (this.tab) {
			case BOARD -> this.initBoard();
			case CREATE -> this.initCreate();
			case MINE -> {
			}
		}
	}

	private Component tabLabel(Tab t) {
		return switch (t) {
			case BOARD -> Component.translatable("requestboard.tab.board");
			case CREATE -> Component.translatable("requestboard.tab.create");
			case MINE -> Component.translatable("requestboard.tab.mine");
		};
	}

	private void switchTab(Tab t) {
		this.tab = t;
		this.selectedId = null;
		this.pickerOpen = false;
		this.rebuildWidgets();
	}

	private void initBoard() {
		this.addRenderableWidget(Button.builder(categoryFilterLabel(), b -> {
			this.filterCategory = nextCategory(this.filterCategory);
			this.boardScroll = 0;
			b.setMessage(categoryFilterLabel());
		}).bounds(this.left + 10, this.top + 9, 150, 16).build());
		this.addRenderableWidget(Button.builder(playerFilterLabel(), b -> {
			this.filterPlayer = nextPlayer(this.filterPlayer);
			this.boardScroll = 0;
			b.setMessage(playerFilterLabel());
		}).bounds(this.left + 164, this.top + 9, 150, 16).build());
	}

	private Component categoryFilterLabel() {
		Component value = this.filterCategory == null
			? Component.translatable("requestboard.filter.all")
			: Component.translatable(this.filterCategory.translationKey());
		return Component.translatable("requestboard.filter.category", value);
	}

	private Component playerFilterLabel() {
		Component value = this.filterPlayer == null ? Component.translatable("requestboard.filter.all") : Component.literal(this.filterPlayer);
		return Component.translatable("requestboard.filter.player", value);
	}

	private @Nullable Category nextCategory(@Nullable Category current) {
		List<Category> options = new ArrayList<>();
		options.add(null);
		for (Category c : Category.values()) {
			if (cfg().isCategoryEnabled(c)) options.add(c);
		}
		int i = options.indexOf(current);
		return options.get((i + 1) % options.size());
	}

	private @Nullable String nextPlayer(@Nullable String current) {
		List<String> options = new ArrayList<>();
		options.add(null);
		TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		for (BoardRequest r : ClientBoardState.requests()) names.add(r.creatorName());
		options.addAll(names);
		int i = options.indexOf(current);
		return options.get((i + 1) % options.size());
	}

	private void initDetailButtons(BoardRequest request) {
		int px = this.left + (W - PW) / 2;
		int py = this.top + (H - PH) / 2;
		UUID me = this.myId();
		boolean isCreator = request.creator().equals(me);
		boolean accepted = request.isAcceptedBy(me);
		boolean isOp = this.minecraft.player != null && Compat.isOperator(this.minecraft.player);

		List<Button> buttons = new ArrayList<>();
		if (accepted) {
			buttons.add(Button.builder(Component.translatable("requestboard.action.leave"),
				b -> this.sendAction(request, RBPayloads.Action.LEAVE)).width(70).build());
		} else if (!isCreator || cfg().allowAcceptOwnRequest) {
			Button accept = Button.builder(Component.translatable("requestboard.action.accept"),
				b -> this.sendAction(request, RBPayloads.Action.ACCEPT)).width(70).build();
			accept.active = cfg().maxAcceptors <= 0 || request.acceptors().size() < cfg().maxAcceptors;
			buttons.add(accept);
		}
		if (isCreator) {
			buttons.add(confirmButton(request, RBPayloads.Action.CLOSE, "requestboard.action.close"));
		}
		if (isCreator || (isOp && cfg().operatorsCanRemoveAny)) {
			buttons.add(confirmButton(request, RBPayloads.Action.DELETE, "requestboard.action.delete"));
		}
		buttons.add(Button.builder(Component.translatable("requestboard.action.back"), b -> this.closeDetail()).width(56).build());

		int total = buttons.stream().mapToInt(Button::getWidth).sum() + 4 * (buttons.size() - 1);
		int bx = px + (PW - total) / 2;
		for (Button button : buttons) {
			button.setPosition(bx, py + PH - 34);
			button.setHeight(16);
			this.addRenderableWidget(button);
			bx += button.getWidth() + 4;
		}
	}

	private Button confirmButton(BoardRequest request, RBPayloads.Action action, String key) {
		String confirmKey = action.name() + request.id();
		boolean armed = confirmKey.equals(this.pendingConfirm) && System.currentTimeMillis() < this.pendingConfirmUntil;
		Component label = armed ? Component.translatable("requestboard.action.confirm").withStyle(ChatFormatting.RED) : Component.translatable(key);
		return Button.builder(label, b -> {
			if (confirmKey.equals(this.pendingConfirm) && System.currentTimeMillis() < this.pendingConfirmUntil) {
				this.pendingConfirm = null;
				this.sendAction(request, action);
			} else {
				this.pendingConfirm = confirmKey;
				this.pendingConfirmUntil = System.currentTimeMillis() + 3000;
				this.rebuildWidgets();
			}
		}).width(62).build();
	}

	private void initCreate() {
		RBConfig cfg = cfg();
		if (this.pickerOpen) {
			int px = this.left + (W - PW) / 2;
			int py = this.top + (H - PH) / 2;
			EditBox search = this.field(px + 20, py + 30, PW - 40, 16, Component.translatable("requestboard.picker.search"));
			this.hint(search, Component.translatable("requestboard.picker.search"));
			search.setValue(this.pickerSearch);
			search.setResponder(value -> {
				this.pickerSearch = value;
				this.pickerScroll = 0;
				this.refreshPicker();
			});
			this.addRenderableWidget(search);
			this.setInitialFocus(search);
			this.addRenderableWidget(Button.builder(Component.translatable("requestboard.action.back"), b -> {
				this.pickerOpen = false;
				this.rebuildWidgets();
			}).bounds(px + (PW - 60) / 2, py + PH - 34, 60, 16).build());
			this.refreshPicker();
			return;
		}

		int x = this.left + 20;
		this.addRenderableWidget(Button.builder(Component.translatable("requestboard.create.category", Component.translatable(this.draftCategory.translationKey())), b -> {
			Category next = this.draftCategory;
			do {
				next = Category.values()[(next.ordinal() + 1) % Category.values().length];
			} while (!cfg.isCategoryEnabled(next));
			this.draftCategory = next;
			this.rebuildWidgets();
		}).bounds(x, this.top + 32, 140, 16).build());

		if (cfg.timeLimit.enabled) {
			EditBox duration = this.field(this.left + 270, this.top + 32, 50, 16, Component.translatable("requestboard.create.duration"));
			duration.setMaxLength(6);
			this.hint(duration, Component.translatable("requestboard.create.duration_hint"));
			duration.setValue(this.draftDuration);
			duration.setResponder(value -> {
				String digits = value.replaceAll("\\D", "");
				if (!digits.equals(value)) {
					duration.setValue(digits);
					return;
				}
				this.draftDuration = digits;
			});
			this.addRenderableWidget(duration);
		}

		EditBox title = this.field(x, this.top + 52, 300, 16, Component.translatable("requestboard.create.title"));
		title.setMaxLength(cfg.maxTitleLength);
		this.hint(title, Component.translatable("requestboard.create.title_hint", cfg.maxTitleLength));
		title.setValue(this.draftTitle);
		title.setResponder(value -> this.draftTitle = value);
		this.addRenderableWidget(title);

		if (cfg.maxDetailsLength > 0) {
			//? if >=1.21.6 {
			MultiLineEditBox details = MultiLineEditBox.builder()
				.setX(x)
				.setY(this.top + 72)
				.setShowBackground(false)
				.setShowDecorations(false)
				.setTextColor(INK)
				.setTextShadow(false)
				.setCursorColor(INK)
				.build(this.font, 300, 34, Component.translatable("requestboard.create.details"));
			this.parchmentFields.add(details);
			this.fieldHints.put(details, Component.translatable("requestboard.create.details_hint"));
			//?} else {
			/*MultiLineEditBox details = new MultiLineEditBox(this.font, x, this.top + 72, 300, 34,
				Component.translatable("requestboard.create.details_hint"), Component.translatable("requestboard.create.details"));
			*///?}
			details.setCharacterLimit(cfg.maxDetailsLength);
			details.setValue(this.draftDetails);
			details.setValueListener(value -> this.draftDetails = value);
			this.addRenderableWidget(details);
		}

		Button add = Button.builder(Component.translatable("requestboard.create.add_item"), b -> {
			this.pickerOpen = true;
			this.pickerScroll = 0;
			this.rebuildWidgets();
		}).bounds(this.left + 112, this.top + 110, 58, 14).build();
		add.active = this.draftObjectives.size() < cfg.maxObjectiveEntries;
		this.addRenderableWidget(add);

		this.objectiveScroll = Math.max(0, Math.min(this.objectiveScroll, Math.max(0, this.draftObjectives.size() - OBJ_ROWS)));
		for (int i = 0; i < OBJ_ROWS && i + this.objectiveScroll < this.draftObjectives.size(); i++) {
			ObjectiveDraft draft = this.draftObjectives.get(i + this.objectiveScroll);
			EditBox count = this.field(x + 20, this.top + 126 + i * 18, 44, 16, Component.translatable("requestboard.create.amount"));
			count.setMaxLength(7);
			count.setValue(draft.count);
			count.setResponder(value -> {
				String digits = value.replaceAll("\\D", "");
				if (!digits.equals(value)) {
					count.setValue(digits);
					return;
				}
				draft.count = digits;
			});
			this.addRenderableWidget(count);
		}

		Button publish = Button.builder(Component.translatable("requestboard.create.publish"), b -> this.submit())
			.bounds(this.left + 256, this.top + 208, 64, 16).build();
		publish.active = !this.waitingForServer;
		this.addRenderableWidget(publish);
	}

	/**
	 * A text field drawn as ink on light paper (frame painted by {@link #drawFields}).
	 * Before 1.21.6 text always has a shadow, which blurs dark ink, so the vanilla dark field is kept there.
	 */
	private EditBox field(int x, int y, int w, int h, Component narration) {
		//? if >=1.21.6 {
		EditBox box = new EditBox(this.font, x + 4, y + (h - 8) / 2, w - 8, 9, narration);
		box.setBordered(false);
		box.setTextShadow(false);
		box.setTextColor(INK);
		box.setTextColorUneditable(INK_SOFT);
		this.parchmentFields.add(box);
		//?} else
		/*EditBox box = new EditBox(this.font, x, y, w, h, narration);*/
		return box;
	}

	private void hint(EditBox box, Component hint) {
		//? if >=1.21.6 {
		this.fieldHints.put(box, hint);
		//?} else
		/*box.setHint(hint.copy().withStyle(ChatFormatting.DARK_GRAY));*/
	}

	private void drawFields(Gfx g) {
		for (AbstractWidget widget : this.parchmentFields) {
			boolean multi = widget instanceof MultiLineEditBox;
			int x = multi ? widget.getX() : widget.getX() - 4;
			int y = multi ? widget.getY() : widget.getY() - 4;
			int w = multi ? widget.getWidth() : widget.getWidth() + 8;
			int h = multi ? widget.getHeight() : widget.getHeight() + 7;
			g.fill(x, y, x + w, y + h, FIELD_BG);
			g.outline(x, y, w, h, widget.isFocused() ? INK : FIELD_BORDER);
			Component hint = this.fieldHints.get(widget);
			boolean empty = widget instanceof EditBox box ? box.getValue().isEmpty()
				: widget instanceof MultiLineEditBox area && area.getValue().isEmpty();
			if (hint != null && empty && !widget.isFocused()) {
				g.text(this.font, this.truncate(hint, w - 8), x + 4, y + (multi ? 4 : (h - 8) / 2), HINT, false);
			}
			if (multi && cfg().maxDetailsLength > 0) {
				String counter = this.draftDetails.length() + "/" + cfg().maxDetailsLength;
				g.text(this.font, counter, x + w - this.font.width(counter) - 2, y + h - 9, INK_SOFT, false);
			}
		}
	}

	private void refreshPicker() {
		if (allItems == null) {
			allItems = BuiltInRegistries.ITEM.stream().filter(item -> item != Items.AIR).map(ItemStack::new).toList();
		}
		String query = this.pickerSearch.strip().toLowerCase(Locale.ROOT);
		if (query.isEmpty()) {
			this.pickerResults = allItems;
			return;
		}
		List<ItemStack> results = new ArrayList<>();
		for (ItemStack stack : allItems) {
			String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
			String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
			if (name.contains(query) || id.contains(query)) results.add(stack);
		}
		this.pickerResults = results;
	}

	private void pickItem(ItemStack stack) {
		Holder<Item> holder = Compat.holder(stack);
		for (ObjectiveDraft draft : this.draftObjectives) {
			if (draft.item.value() == holder.value()) {
				this.pickerOpen = false;
				this.rebuildWidgets();
				return;
			}
		}
		this.draftObjectives.add(new ObjectiveDraft(holder, Integer.toString(stack.getMaxStackSize())));
		this.objectiveScroll = Math.max(0, this.draftObjectives.size() - OBJ_ROWS);
		this.pickerOpen = false;
		this.rebuildWidgets();
	}

	private void submit() {
		RBConfig cfg = cfg();
		if (this.draftTitle.isBlank()) {
			this.setCreateError(Component.translatable("requestboard.error.title_empty"));
			return;
		}
		List<ObjectiveEntry> objectives = new ArrayList<>();
		for (ObjectiveDraft draft : this.draftObjectives) {
			int count = parse(draft.count);
			if (count <= 0) {
				this.setCreateError(Component.translatable("requestboard.error.invalid_objective"));
				return;
			}
			objectives.add(new ObjectiveEntry(draft.item, count));
		}
		if (this.draftCategory == Category.HARVEST && cfg.harvestRequiresObjective && objectives.isEmpty()) {
			this.setCreateError(Component.translatable("requestboard.error.harvest_needs_objective"));
			return;
		}
		List<RBPayloads.RewardPick> rewards = new ArrayList<>();
		this.draftRewards.forEach((slot, count) -> rewards.add(new RBPayloads.RewardPick(slot, count)));
		int minutes = cfg.timeLimit.enabled ? Math.max(0, parse(this.draftDuration)) : 0;
		this.waitingForServer = true;
		this.createMessage = null;
		ClientPlayNetworking.send(new RBPayloads.CreateRequest(this.draftCategory, this.draftTitle.strip(), this.draftDetails.strip(), objectives, rewards, minutes));
		this.rebuildWidgets();
	}

	private void setCreateError(Component message) {
		this.createMessage = message;
		this.createMessageIsError = true;
	}

	private static int parse(String text) {
		try {
			return text.isEmpty() ? 0 : Integer.parseInt(text);
		} catch (NumberFormatException e) {
			return Integer.MAX_VALUE;
		}
	}

	private void sendAction(BoardRequest request, RBPayloads.Action action) {
		ClientPlayNetworking.send(new RBPayloads.RequestAction(request.id(), action));
		if (action == RBPayloads.Action.CLOSE || action == RBPayloads.Action.DELETE) {
			this.selectedId = null;
		}
		this.rebuildWidgets();
	}

	private void openDetail(BoardRequest request) {
		this.selectedId = request.id();
		this.pendingConfirm = null;
		this.rebuildWidgets();
	}

	private void closeDetail() {
		this.selectedId = null;
		this.pendingConfirm = null;
		this.rebuildWidgets();
	}

	@Override
	public void tick() {
		super.tick();
		if (this.pendingConfirm != null && System.currentTimeMillis() >= this.pendingConfirmUntil) {
			this.pendingConfirm = null;
			this.rebuildWidgets();
		}
	}

	// ------------------------------------------------------------------ input

	//? if >=1.21.9 {
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		return this.handleClick(event.x(), event.y(), event.button());
	}
	//?} else {
	/*@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}
		return this.handleClick(mouseX, mouseY, button);
	}
	*///?}

	private boolean handleClick(double mx, double my, int button) {
		for (int i = this.zones.size() - 1; i >= 0; i--) {
			ClickZone zone = this.zones.get(i);
			if (zone.contains(mx, my)) {
				this.playClick();
				zone.action().accept(button);
				return true;
			}
		}
		int px = this.left + (W - PW) / 2;
		int py = this.top + (H - PH) / 2;
		boolean outsideParchment = mx < px || mx >= px + PW || my < py || my >= py + PH;
		if (this.selectedId != null && this.tab != Tab.CREATE && outsideParchment && my >= this.top) {
			this.closeDetail();
			return true;
		}
		if (this.pickerOpen && this.tab == Tab.CREATE && outsideParchment && my >= this.top) {
			this.pickerOpen = false;
			this.rebuildWidgets();
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (super.mouseScrolled(x, y, scrollX, scrollY)) {
			return true;
		}
		double step = scrollY * 20;
		if (this.tab == Tab.CREATE && this.pickerOpen) {
			this.pickerScroll = Math.max(0, this.pickerScroll - scrollY);
			return true;
		}
		if (this.selectedId != null) {
			return false;
		}
		switch (this.tab) {
			case BOARD -> this.boardScroll = Math.max(0, this.boardScroll - step);
			case MINE -> this.mineScroll = Math.max(0, this.mineScroll - step);
			case CREATE -> {
				if (x < this.left + 175 && y > this.top + 120) {
					int before = this.objectiveScroll;
					this.objectiveScroll = Math.max(0, Math.min(this.objectiveScroll - (int) Math.signum(scrollY), Math.max(0, this.draftObjectives.size() - OBJ_ROWS)));
					if (before != this.objectiveScroll) this.rebuildWidgets();
				}
			}
		}
		return true;
	}

	//? if >=1.21.9 {
	@Override
	public boolean keyPressed(KeyEvent event) {
		return this.handleEscape(event.key()) || super.keyPressed(event);
	}
	//?} else {
	/*@Override
	public boolean keyPressed(int key, int scanCode, int modifiers) {
		return this.handleEscape(key) || super.keyPressed(key, scanCode, modifiers);
	}
	*///?}

	private boolean handleEscape(int key) {
		if (key == InputConstants.KEY_ESCAPE) {
			if (this.pickerOpen) {
				this.pickerOpen = false;
				this.rebuildWidgets();
				return true;
			}
			if (this.selectedId != null) {
				this.closeDetail();
				return true;
			}
		}
		return false;
	}

	private void playClick() {
		this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	// ------------------------------------------------------------------ rendering

	//? if >=26.1 {
	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		this.extractTransparentBackground(graphics);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		this.renderContent(new Gfx(graphics), mouseX, mouseY);
		super.extractRenderState(graphics, mouseX, mouseY, a);
	}
	//?} elif >=1.21.6 {
	/*@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float a) {
		this.renderTransparentBackground(graphics);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float a) {
		this.renderContent(new Gfx(graphics), mouseX, mouseY);
		super.render(graphics, mouseX, mouseY, a);
	}
	*///?} else {
	/*private @Nullable Gfx frame;

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float a) {
		this.renderTransparentBackground(graphics);
		this.frame = new Gfx(graphics);
		this.renderContent(this.frame, mouseX, mouseY);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float a) {
		super.render(graphics, mouseX, mouseY, a);
		if (this.frame != null) this.frame.flush();
	}
	*///?}

	private void renderContent(Gfx g, int mouseX, int mouseY) {
		this.zones.clear();
		BoardRequest selected = this.selectedId == null ? null : ClientBoardState.find(this.selectedId);
		boolean overlay = (selected != null && this.tab != Tab.CREATE) || (this.pickerOpen && this.tab == Tab.CREATE);
		int mx = overlay ? -1 : mouseX;
		int my = overlay ? -1 : mouseY;

		g.underOverlay = overlay;
		if (overlay && Gfx.LEGACY_LAYERING) {
			// Old versions would let the content below show through the scroll: draw only the board.
			this.drawBoardBackground(g);
		} else {
			switch (this.tab) {
				case BOARD -> this.renderBoard(g, mx, my, !overlay);
				case CREATE -> this.renderCreate(g, mx, my, !overlay);
				case MINE -> this.renderMine(g, mx, my, !overlay);
			}
		}

		g.underOverlay = false;
		if (selected != null && this.tab != Tab.CREATE) {
			this.renderDetail(g, selected, mouseX, mouseY);
		} else if (this.pickerOpen && this.tab == Tab.CREATE) {
			this.renderPicker(g, mouseX, mouseY);
		}
	}

	// ---- Board tab

	private List<BoardRequest> filteredRequests() {
		List<BoardRequest> list = new ArrayList<>();
		for (BoardRequest r : ClientBoardState.requests()) {
			if (this.filterCategory != null && r.category() != this.filterCategory) continue;
			if (this.filterPlayer != null && !r.creatorName().equals(this.filterPlayer)) continue;
			list.add(r);
		}
		list.sort(Comparator.comparingLong(BoardRequest::createdAt).reversed());
		return list;
	}

	private void renderBoard(Gfx g, int mouseX, int mouseY, boolean interactive) {
		this.drawBoardBackground(g);

		int x0 = this.left + 10;
		int y0 = this.top + 30;
		int x1 = this.left + W - 10;
		int y1 = this.top + H - 9;
		List<BoardRequest> requests = this.filteredRequests();

		if (this.boardMessage != null && System.currentTimeMillis() < this.boardMessageUntil) {
			g.textWithBackdrop(this.font, this.boardMessage, this.left + (W - this.font.width(this.boardMessage)) / 2, y1 - 12, this.font.width(this.boardMessage), GREEN);
		}

		if (requests.isEmpty()) {
			Component empty = Component.translatable(ClientBoardState.requests().isEmpty() ? "requestboard.board.empty" : "requestboard.board.no_match");
			int w = this.font.width(empty);
			int ex = this.left + (W - w) / 2;
			g.fill(ex - 6, this.top + H / 2 - 4, ex + w + 6, this.top + H / 2 + 12, 0xF0201610);
			g.text(this.font, empty, ex, this.top + H / 2, CREAM, false);
			return;
		}

		int cardW = (x1 - x0 - CARD_GAP) / 2;
		int rows = (requests.size() + 1) / 2;
		int contentH = rows * (CARD_H + CARD_GAP) - CARD_GAP;
		int maxScroll = Math.max(0, contentH - (y1 - y0));
		this.boardScroll = Math.min(this.boardScroll, maxScroll);
		int scroll = (int) this.boardScroll;

		g.enableScissor(x0, y0, x1, y1);
		for (int i = 0; i < requests.size(); i++) {
			int cx = x0 + (i % 2) * (cardW + CARD_GAP);
			int cy = y0 + (i / 2) * (CARD_H + CARD_GAP) - scroll;
			if (cy + CARD_H < y0 || cy > y1) continue;
			BoardRequest request = requests.get(i);
			boolean visible = interactive && mouseY >= y0 && mouseY < y1;
			this.renderCard(g, request, cx, cy, cardW, CARD_H, visible ? mouseX : -1, visible ? mouseY : -1);
			if (interactive) {
				int zy = Math.max(cy, y0);
				int zh = Math.min(cy + CARD_H, y1) - zy;
				if (zh > 0) this.zones.add(new ClickZone(cx, zy, cardW, zh, button -> this.openDetail(request)));
			}
		}
		g.disableScissor();
		this.renderScrollbar(g, x1 + 2, y0, y1, scroll, maxScroll, contentH);
	}

	private void drawBoardBackground(Gfx g) {
		BoardStyle style = BoardStyle.current();
		g.blit(style.background, this.left, this.top, W, H, style.textureWidth, style.textureHeight);
	}

	private void renderScrollbar(Gfx g, int x, int y0, int y1, int scroll, int maxScroll, int contentH) {
		if (maxScroll <= 0) return;
		int trackH = y1 - y0;
		int thumbH = Math.max(12, trackH * trackH / contentH);
		int thumbY = y0 + (trackH - thumbH) * scroll / maxScroll;
		g.fill(x, y0, x + 3, y1, 0x60000000);
		g.fill(x, thumbY, x + 3, thumbY + thumbH, 0xFFD8C08A);
	}

	private void renderCard(Gfx g, BoardRequest r, int x, int y, int w, int h, int mouseX, int mouseY) {
		boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
		g.fill(x, y, x + w, y + h, hovered ? CARD_BG_HOVER : CARD_BG);
		g.outline(x, y, w, h, hovered ? GOLD : CARD_BORDER);
		g.fill(x + 1, y + 1, x + 3, y + h - 1, r.category().color());

		UUID me = this.myId();
		this.head(g, r.creator(), x + 6, y + 5, 16);
		Component badge = null;
		int badgeColor = GOLD;
		if (r.creator().equals(me)) {
			badge = Component.translatable("requestboard.badge.yours");
		} else if (r.isAcceptedBy(me)) {
			badge = Component.translatable("requestboard.badge.accepted");
			badgeColor = GREEN;
		}
		int badgeW = badge == null ? 0 : this.font.width(badge) + 4;
		g.text(this.font, this.ellipsis(r.creatorName(), w - 30 - badgeW), x + 26, y + 5, WHITE, true);
		if (badge != null) {
			g.text(this.font, badge, x + w - badgeW, y + 5, badgeColor, true);
		}

		g.text(this.font, Component.translatable(r.category().translationKey()), x + 26, y + 15, r.category().color(), true);
		if (r.hasTimeLimit()) {
			Component time = this.timeLeft(r);
			g.text(this.font, time, x + w - this.font.width(time) - 4, y + 15, this.timeColor(r), true);
		}

		g.text(this.font, this.ellipsis(r.title(), w - 10), x + 6, y + 26, CREAM, true);

		// Objectives on the left, rewards on the right.
		int iconY = y + 34;
		int half = (w - 12) / 2;
		int maxObj = Math.max(1, half / 18);
		for (int i = 0; i < Math.min(maxObj, r.objectives().size()); i++) {
			ObjectiveEntry entry = r.objectives().get(i);
			boolean more = i == maxObj - 1 && r.objectives().size() > maxObj;
			this.objectiveIcon(g, entry, x + 6 + i * 18, iconY, mouseX, mouseY, more ? r.objectives().size() - maxObj : 0);
		}
		if (!r.rewards().isEmpty()) {
			int maxRew = Math.max(1, (half - 10) / 18);
			int shown = Math.min(maxRew, r.rewards().size());
			int rx = x + w - 6 - shown * 18;
			g.text(this.font, "+", rx - 8, iconY + 4, GOLD, true);
			for (int i = 0; i < shown; i++) {
				this.rewardIcon(g, r.rewards().get(i), rx + i * 18, iconY, mouseX, mouseY);
			}
		}

		Component acceptedBy = Component.translatable("requestboard.accepted_by", this.acceptorNames(r));
		g.text(this.font, this.truncate(acceptedBy, w - 10), x + 6, y + h - 10, GRAY, true);
	}

	// ---- My contracts tab

	private void renderMine(Gfx g, int mouseX, int mouseY, boolean interactive) {
		this.drawBoardBackground(g);
		UUID me = this.myId();
		List<BoardRequest> mine = new ArrayList<>();
		List<BoardRequest> accepted = new ArrayList<>();
		for (BoardRequest r : ClientBoardState.requests()) {
			if (r.creator().equals(me)) mine.add(r);
			else if (r.isAcceptedBy(me)) accepted.add(r);
		}
		mine.sort(Comparator.comparingLong(BoardRequest::createdAt).reversed());
		accepted.sort(Comparator.comparingLong(BoardRequest::createdAt).reversed());

		int x0 = this.left + 10;
		int x1 = this.left + W - 10;
		int y0 = this.top + 10;
		int y1 = this.top + H - 9;
		int contentH = 2 * 16 + Math.max(1, mine.size()) * ROW_H + Math.max(1, accepted.size()) * ROW_H + 8;
		int maxScroll = Math.max(0, contentH - (y1 - y0));
		this.mineScroll = Math.min(this.mineScroll, maxScroll);
		int y = y0 - (int) this.mineScroll;
		boolean inside = interactive && mouseY >= y0 && mouseY < y1;
		int mx = inside ? mouseX : -1;
		int my = inside ? mouseY : -1;

		g.enableScissor(x0, y0, x1, y1);
		y = this.sectionHeader(g, Component.translatable("requestboard.mine.created", mine.size()), x0, y);
		if (mine.isEmpty()) {
			y = this.emptyRow(g, Component.translatable("requestboard.mine.none_created"), x0, x1, y);
		}
		for (BoardRequest r : mine) {
			this.renderMineRow(g, r, true, x0, y, x1 - x0, mx, my, interactive, y0, y1);
			y += ROW_H;
		}
		y += 8;
		y = this.sectionHeader(g, Component.translatable("requestboard.mine.accepted", accepted.size()), x0, y);
		if (accepted.isEmpty()) {
			y = this.emptyRow(g, Component.translatable("requestboard.mine.none_accepted"), x0, x1, y);
		}
		for (BoardRequest r : accepted) {
			this.renderMineRow(g, r, false, x0, y, x1 - x0, mx, my, interactive, y0, y1);
			y += ROW_H;
		}
		g.disableScissor();
		this.renderScrollbar(g, x1 + 2, y0, y1, (int) this.mineScroll, maxScroll, contentH);
	}

	private int sectionHeader(Gfx g, Component text, int x, int y) {
		int w = this.font.width(text) + 10;
		g.fill(x, y + 1, x + w, y + 13, 0xF0201610);
		g.outline(x, y + 1, w, 12, CARD_BORDER);
		g.text(this.font, text, x + 5, y + 3, GOLD, false);
		return y + 16;
	}

	private int emptyRow(Gfx g, Component text, int x0, int x1, int y) {
		g.fill(x0, y, x1, y + ROW_H - 4, CARD_BG);
		g.text(this.font, text, x0 + 8, y + 8, GRAY, true);
		return y + ROW_H;
	}

	private void renderMineRow(Gfx g, BoardRequest r, boolean created, int x, int y, int w, int mouseX, int mouseY,
			boolean interactive, int clipTop, int clipBottom) {
		int h = ROW_H - 4;
		g.fill(x, y, x + w, y + h, CARD_BG);
		g.outline(x, y, w, h, CARD_BORDER);
		g.fill(x + 1, y + 1, x + 3, y + h - 1, r.category().color());
		this.head(g, r.creator(), x + 6, y + 4, 16);

		int buttonsW = created ? 3 * 40 : 2 * 40;
		int textW = w - 30 - buttonsW - 4;
		g.text(this.font, this.ellipsis(r.title(), textW), x + 26, y + 3, CREAM, true);
		MutableComponent info = Component.translatable(r.category().translationKey()).withColor(r.category().color());
		if (created) {
			info.append(Component.literal(" · ").withColor(GRAY))
				.append(Component.translatable("requestboard.mine.participants", r.acceptors().size()).withColor(GRAY));
		} else {
			info.append(Component.literal(" · ").withColor(GRAY))
				.append(Component.translatable("requestboard.by", r.creatorName()).withColor(GRAY));
		}
		if (r.hasTimeLimit()) {
			info.append(Component.literal(" · ").withColor(GRAY)).append(this.timeLeft(r).copy().withColor(this.timeColor(r)));
		}
		g.text(this.font, truncate(info, textW), x + 26, y + 13, WHITE, true);

		boolean rowVisible = y + h > clipTop && y < clipBottom;
		int bx = x + w - buttonsW - 2;
		int by = y + 5;
		this.miniButton(g, Component.translatable("requestboard.action.view"), bx, by, 38, 14, 0xFFD8C08A, mouseX, mouseY,
			interactive && rowVisible, b -> this.openDetail(r));
		bx += 40;
		if (created) {
			this.miniConfirmButton(g, r, RBPayloads.Action.CLOSE, "requestboard.action.close_short", bx, by, GREEN, mouseX, mouseY, interactive && rowVisible);
			bx += 40;
			this.miniConfirmButton(g, r, RBPayloads.Action.DELETE, "requestboard.action.delete_short", bx, by, RED, mouseX, mouseY, interactive && rowVisible);
		} else {
			this.miniButton(g, Component.translatable("requestboard.action.leave_short"), bx, by, 38, 14, RED, mouseX, mouseY,
				interactive && rowVisible, b -> this.sendAction(r, RBPayloads.Action.LEAVE));
		}
	}

	private FormattedCharSequence truncate(Component text, int width) {
		if (this.font.width(text) <= width) {
			return text.getVisualOrderText();
		}
		FormattedText cut = this.font.substrByWidth(text, width - this.font.width("…"));
		return net.minecraft.locale.Language.getInstance().getVisualOrder(FormattedText.composite(cut, FormattedText.of("…")));
	}

	private String ellipsis(String text, int width) {
		if (this.font.width(text) <= width) return text;
		return this.font.plainSubstrByWidth(text, width - this.font.width("…")) + "…";
	}

	private void miniConfirmButton(Gfx g, BoardRequest r, RBPayloads.Action action, String key, int x, int y, int color,
			int mouseX, int mouseY, boolean interactive) {
		String confirmKey = action.name() + r.id();
		boolean armed = confirmKey.equals(this.pendingConfirm) && System.currentTimeMillis() < this.pendingConfirmUntil;
		Component label = armed ? Component.translatable("requestboard.action.confirm") : Component.translatable(key);
		this.miniButton(g, label, x, y, 38, 14, armed ? GOLD : color, mouseX, mouseY, interactive, b -> {
			if (confirmKey.equals(this.pendingConfirm) && System.currentTimeMillis() < this.pendingConfirmUntil) {
				this.pendingConfirm = null;
				this.sendAction(r, action);
			} else {
				this.pendingConfirm = confirmKey;
				this.pendingConfirmUntil = System.currentTimeMillis() + 3000;
			}
		});
	}

	private void miniButton(Gfx g, Component label, int x, int y, int w, int h, int color, int mouseX, int mouseY,
			boolean interactive, IntConsumer action) {
		boolean hovered = interactive && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
		g.fill(x, y, x + w, y + h, hovered ? 0xFF6A4A2C : 0xFF3E2A18);
		g.outline(x, y, w, h, color);
		int tw = this.font.width(label);
		g.text(this.font, label, x + (w - tw) / 2, y + (h - 8) / 2, color, false);
		if (interactive) {
			this.zones.add(new ClickZone(x, y, w, h, action));
		}
	}

	// ---- Detail overlay

	private void renderDetail(Gfx g, BoardRequest r, int mouseX, int mouseY) {
		g.fill(this.left, this.top, this.left + W, this.top + H, 0xA0000000);
		int px = this.left + (W - PW) / 2;
		int py = this.top + (H - PH) / 2;
		g.blit(PARCHMENT_TEX, px, py, PW, PH, PW, PH);

		int x = px + 20;
		int w = PW - 40;
		int y = py + 18;

		List<FormattedCharSequence> titleLines = this.font.split(Component.literal(r.title()).withStyle(ChatFormatting.BOLD), w);
		for (int i = 0; i < Math.min(2, titleLines.size()); i++) {
			FormattedCharSequence line = titleLines.get(i);
			g.text(this.font, line, px + (PW - this.font.width(line)) / 2, y, INK, false);
			y += 10;
		}
		y += 2;

		this.head(g, r.creator(), x, y, 12);
		Component by = Component.translatable("requestboard.by", r.creatorName());
		g.text(this.font, by, x + 16, y + 2, INK_SOFT, false);
		Component cat = Component.translatable(r.category().translationKey());
		g.text(this.font, cat, x + 20 + this.font.width(by), y + 2, darker(r.category().color()), false);
		if (r.hasTimeLimit()) {
			Component time = this.timeLeft(r);
			g.text(this.font, time, x + w - this.font.width(time), y + 2, r.expiresAt() - ClientBoardState.now() < 60_000 ? DARK_RED : INK_SOFT, false);
		}
		y += 15;
		g.fill(x, y, x + w, y + 1, 0x603B2A14);
		y += 4;

		if (!r.details().isEmpty()) {
			List<FormattedCharSequence> lines = this.font.split(Component.literal(r.details()), w);
			int maxLines = 5;
			for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
				g.text(this.font, lines.get(i), x, y, INK, false);
				y += 9;
			}
			if (lines.size() > maxLines) {
				g.text(this.font, "…", x + w - 6, y - 9, INK, false);
			}
			y += 3;
		}

		g.text(this.font, Component.translatable("requestboard.objective").withStyle(ChatFormatting.BOLD), x, y, INK, false);
		y += 10;
		if (r.objectives().isEmpty()) {
			g.text(this.font, Component.translatable("requestboard.none"), x + 2, y + 2, INK_SOFT, false);
			y += 12;
		} else {
			int perRow = w / 20;
			for (int i = 0; i < r.objectives().size(); i++) {
				this.objectiveIcon(g, r.objectives().get(i), x + (i % perRow) * 20, y + (i / perRow) * 18, mouseX, mouseY, 0);
			}
			y += ((r.objectives().size() - 1) / perRow + 1) * 18;
		}

		g.text(this.font, Component.translatable("requestboard.reward").withStyle(ChatFormatting.BOLD), x, y, INK, false);
		if (!r.rewards().isEmpty() && !r.rewardsEscrowed()) {
			Component hand = Component.translatable("requestboard.reward.by_hand");
			g.text(this.font, hand, x + w - this.font.width(hand), y, INK_SOFT, false);
		}
		y += 10;
		if (r.rewards().isEmpty()) {
			g.text(this.font, Component.translatable("requestboard.none"), x + 2, y + 2, INK_SOFT, false);
			y += 12;
		} else {
			for (int i = 0; i < r.rewards().size(); i++) {
				this.rewardIcon(g, r.rewards().get(i), x + i * 20, y, mouseX, mouseY);
			}
			y += 18;
		}

		Component accepted = Component.translatable("requestboard.accepted_by", this.acceptorNames(r));
		List<FormattedCharSequence> acceptedLines = this.font.split(accepted, w);
		for (int i = 0; i < Math.min(2, acceptedLines.size()); i++) {
			if (y + 9 > py + PH - 36) break;
			g.text(this.font, acceptedLines.get(i), x, y, INK, false);
			y += 9;
		}
	}

	// ---- Create tab

	private void renderCreate(Gfx g, int mouseX, int mouseY, boolean interactive) {
		RBConfig cfg = cfg();
		this.drawBoardBackground(g);
		int px = this.left + 6;
		int py = this.top + 6;
		g.blit(PARCHMENT_LARGE_TEX, px, py, LW, LH, LW, LH);
		if (!this.pickerOpen) this.drawFields(g);

		Component header = this.waitingForServer
			? Component.translatable("requestboard.create.sending")
			: this.createMessage != null ? this.createMessage : Component.translatable("requestboard.create.header").withStyle(ChatFormatting.BOLD);
		int headerColor = this.createMessage != null && !this.waitingForServer ? (this.createMessageIsError ? DARK_RED : DARK_GREEN) : INK;
		FormattedCharSequence headerLine = this.truncate(header, LW - 40);
		g.text(this.font, headerLine, this.left + (W - this.font.width(headerLine)) / 2, this.top + 20, headerColor, false);

		if (cfg.timeLimit.enabled) {
			Component label = Component.translatable("requestboard.create.duration");
			g.text(this.font, label, this.left + 266 - this.font.width(label), this.top + 36, INK, false);
		}

		int x = this.left + 20;
		// Objectives column
		g.text(this.font, Component.translatable("requestboard.objective").withStyle(ChatFormatting.BOLD), x, this.top + 113, INK, false);
		if (this.draftObjectives.isEmpty()) {
			g.text(this.font, Component.translatable("requestboard.create.no_objective"), x, this.top + 131, INK_SOFT, false);
		}
		for (int i = 0; i < OBJ_ROWS && i + this.objectiveScroll < this.draftObjectives.size(); i++) {
			int index = i + this.objectiveScroll;
			ObjectiveDraft draft = this.draftObjectives.get(index);
			int ry = this.top + 126 + i * 18;
			ItemStack stack = new ItemStack(draft.item);
			g.item(stack, x, ry);
			if (interactive && mouseX >= x && mouseX < x + 16 && mouseY >= ry && mouseY < ry + 16) {
				g.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
			}
			int count = parse(draft.count);
			int max = cfg.maxCountFor(stack.getMaxStackSize());
			int color = count <= 0 || count > max ? DARK_RED : INK_SOFT;
			String stacks = count > 0 && count != Integer.MAX_VALUE ? stackText(count, stack.getMaxStackSize()) : "";
			g.text(this.font, this.font.plainSubstrByWidth(stacks, 66), x + 68, ry + 4, color, false);
			this.miniButton(g, Component.literal("✕"), x + 136, ry + 1, 14, 14, DARK_RED, mouseX, mouseY, interactive, b -> {
				this.draftObjectives.remove(index);
				this.rebuildWidgets();
			});
		}
		if (this.draftObjectives.size() > OBJ_ROWS) {
			String pos = (this.objectiveScroll + 1) + "-" + Math.min(this.draftObjectives.size(), this.objectiveScroll + OBJ_ROWS) + "/" + this.draftObjectives.size();
			g.text(this.font, pos, x + 60 - this.font.width(pos), this.top + 113, INK_SOFT, false);
		}

		// Rewards column
		int rx = this.left + 180;
		g.text(this.font, Component.translatable("requestboard.reward").withStyle(ChatFormatting.BOLD), rx, this.top + 113, INK, false);
		if (!cfg.rewards.enabled) {
			g.textWithWordWrap(this.font, Component.translatable("requestboard.create.rewards_disabled"), rx, this.top + 128, 140, INK_SOFT, false);
			return;
		}
		String counter = "(" + this.draftRewards.size() + "/" + cfg.rewards.maxRewardStacks + ")";
		int labelW = this.font.width(Component.translatable("requestboard.reward").withStyle(ChatFormatting.BOLD));
		g.text(this.font, counter, rx + labelW + 4, this.top + 113, INK_SOFT, false);

		var inventory = this.minecraft.player == null ? null : this.minecraft.player.getInventory();
		if (inventory == null) return;
		this.draftRewards.entrySet().removeIf(e -> inventory.getItem(e.getKey()).isEmpty());
		this.draftRewards.replaceAll((slot, count) -> Math.min(count, inventory.getItem(slot).getCount()));

		// Picked rewards row
		int i = 0;
		int pickY = this.top + 122;
		for (Map.Entry<Integer, Integer> pick : new ArrayList<>(this.draftRewards.entrySet())) {
			int ix = rx + i * 16;
			g.fill(ix, pickY, ix + 16, pickY + 16, 0x40FFD45A);
			ItemStack stack = inventory.getItem(pick.getKey()).copyWithCount(pick.getValue());
			g.item(stack, ix, pickY);
			g.itemDecorations(this.font, stack, ix, pickY);
			if (interactive) {
				int slot = pick.getKey();
				if (mouseX >= ix && mouseX < ix + 16 && mouseY >= pickY && mouseY < pickY + 16) {
					g.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
				}
				this.zones.add(new ClickZone(ix, pickY, 16, 16, button -> {
					if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
						int left = this.draftRewards.getOrDefault(slot, 0) - 1;
						if (left <= 0) this.draftRewards.remove(slot);
						else this.draftRewards.put(slot, left);
					} else {
						this.draftRewards.remove(slot);
					}
				}));
			}
			i++;
		}
		if (this.draftRewards.isEmpty()) {
			g.text(this.font, Component.translatable("requestboard.create.reward_hint"), rx, pickY + 4, INK_SOFT, false);
		}

		// Inventory grid: main inventory then hotbar, like vanilla.
		int gridY = this.top + 140;
		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 9; col++) {
				int slot = row < 3 ? 9 + row * 9 + col : col;
				int sx = rx + col * 16;
				int sy = gridY + row * 16 + (row == 3 ? 2 : 0);
				g.fill(sx, sy, sx + 16, sy + 16, 0x50000000);
				g.fill(sx + 1, sy + 1, sx + 15, sy + 15, 0x30FFFFFF);
				ItemStack inSlot = inventory.getItem(slot);
				if (inSlot.isEmpty()) continue;
				int remaining = inSlot.getCount() - this.draftRewards.getOrDefault(slot, 0);
				ItemStack shown = inSlot.copyWithCount(Math.max(1, remaining));
				g.item(shown, sx, sy);
				if (remaining <= 0) {
					g.fill(sx, sy, sx + 16, sy + 16, 0xA0D8C8A0);
				} else {
					g.itemDecorations(this.font, shown, sx, sy);
				}
				boolean hovered = interactive && mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16;
				if (hovered) {
					g.fill(sx, sy, sx + 16, sy + 16, 0x60FFFFFF);
					g.setTooltipForNextFrame(this.font, inSlot, mouseX, mouseY);
				}
				if (interactive && remaining > 0) {
					this.zones.add(new ClickZone(sx, sy, 16, 16, button -> this.addReward(slot, button == InputConstants.MOUSE_BUTTON_RIGHT ? 1 : remaining)));
				}
			}
		}
		if (!cfg.rewards.autoWithdraw) {
			g.text(this.font, Component.translatable("requestboard.create.not_withdrawn"), rx, gridY + 68, INK_SOFT, false);
		}
	}

	private void addReward(int slot, int amount) {
		if (!this.draftRewards.containsKey(slot) && this.draftRewards.size() >= cfg().rewards.maxRewardStacks) {
			this.setCreateError(Component.translatable("requestboard.error.too_many_rewards", cfg().rewards.maxRewardStacks));
			return;
		}
		this.draftRewards.merge(slot, amount, Integer::sum);
		if (this.createMessageIsError) this.createMessage = null;
	}

	// ---- Item picker

	private void renderPicker(Gfx g, int mouseX, int mouseY) {
		g.fill(this.left, this.top, this.left + W, this.top + H, 0xA0000000);
		int px = this.left + (W - PW) / 2;
		int py = this.top + (H - PH) / 2;
		g.blit(PARCHMENT_TEX, px, py, PW, PH, PW, PH);
		Component title = Component.translatable("requestboard.picker.title").withStyle(ChatFormatting.BOLD);
		g.text(this.font, title, px + (PW - this.font.width(title)) / 2, py + 18, INK, false);
		this.drawFields(g);

		int gx = px + (PW - PICKER_COLS * 18) / 2;
		int gy = py + 52;
		int rows = (this.pickerResults.size() + PICKER_COLS - 1) / PICKER_COLS;
		int maxScroll = Math.max(0, rows - PICKER_ROWS);
		this.pickerScroll = Math.min(this.pickerScroll, maxScroll);
		int firstRow = (int) this.pickerScroll;
		if (this.pickerResults.isEmpty()) {
			Component none = Component.translatable("requestboard.picker.none");
			g.text(this.font, none, px + (PW - this.font.width(none)) / 2, gy + 50, INK_SOFT, false);
		}
		for (int row = 0; row < PICKER_ROWS; row++) {
			for (int col = 0; col < PICKER_COLS; col++) {
				int index = (firstRow + row) * PICKER_COLS + col;
				if (index >= this.pickerResults.size()) break;
				ItemStack stack = this.pickerResults.get(index);
				int sx = gx + col * 18;
				int sy = gy + row * 18;
				boolean hovered = mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18;
				g.fill(sx, sy, sx + 18, sy + 18, hovered ? 0x60FFFFFF : 0x20000000);
				g.item(stack, sx + 1, sy + 1);
				if (hovered) g.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
				this.zones.add(new ClickZone(sx, sy, 18, 18, button -> this.pickItem(stack)));
			}
		}
		if (maxScroll > 0) {
			this.renderScrollbar(g, gx + PICKER_COLS * 18 + 3, gy, gy + PICKER_ROWS * 18, firstRow, maxScroll, rows);
		}
	}

	// ------------------------------------------------------------------ helpers

	private UUID myId() {
		return this.minecraft.player == null ? new UUID(0, 0) : this.minecraft.player.getUUID();
	}

	private void head(Gfx g, UUID playerId, int x, int y, int size) {
		PlayerInfo info = this.minecraft.getConnection() == null ? null : this.minecraft.getConnection().getPlayerInfo(playerId);
		if (info != null) {
			g.face(info.getSkin(), x, y, size);
		} else {
			//? if >=26.1 {
			PlayerFaceExtractor.extractRenderState(g.raw, this.profiles.computeIfAbsent(playerId, ResolvableProfile::createUnresolved), x, y, size);
			//?} else
			/*g.face(DefaultPlayerSkin.get(playerId), x, y, size);*/
		}
	}

	private void objectiveIcon(Gfx g, ObjectiveEntry entry, int x, int y, int mouseX, int mouseY, int moreCount) {
		ItemStack stack = entry.displayStack();
		g.item(stack, x, y);
		g.itemDecorations(this.font, stack, x, y, moreCount > 0 ? "+" + (moreCount + 1) : shortCount(entry.count()));
		if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
			List<Component> lines = new ArrayList<>();
			lines.add(stack.getHoverName());
			lines.add(Component.literal(entry.count() + " · " + stackText(entry.count(), stack.getMaxStackSize())).withStyle(ChatFormatting.GRAY));
			g.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
		}
	}

	private void rewardIcon(Gfx g, ItemStack stack, int x, int y, int mouseX, int mouseY) {
		g.item(stack, x, y);
		g.itemDecorations(this.font, stack, x, y);
		if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
			g.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
		}
	}

	private Component acceptorNames(BoardRequest r) {
		if (r.acceptors().isEmpty()) {
			return Component.translatable("requestboard.nobody");
		}
		List<String> names = r.acceptors().stream().map(Acceptor::name).toList();
		return Component.literal(String.join(", ", names));
	}

	private Component timeLeft(BoardRequest r) {
		long ms = Math.max(0, r.expiresAt() - ClientBoardState.now());
		long total = ms / 1000;
		long days = total / 86400;
		long hours = (total % 86400) / 3600;
		long minutes = (total % 3600) / 60;
		long seconds = total % 60;
		if (days > 0) {
			return Component.translatable("requestboard.time.days", days, hours);
		}
		return Component.literal(hours > 0
			? String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
			: String.format(Locale.ROOT, "%02d:%02d", minutes, seconds));
	}

	private int timeColor(BoardRequest r) {
		long ms = r.expiresAt() - ClientBoardState.now();
		return ms < 60_000 ? RED : ms < 600_000 ? GOLD : GRAY;
	}

	private static String shortCount(int count) {
		if (count < 1000) return Integer.toString(count);
		if (count < 10_000) return String.format(Locale.ROOT, "%.1fk", count / 1000.0);
		return (count / 1000) + "k";
	}

	private static String stackText(int count, int maxStack) {
		if (maxStack <= 1) {
			return Component.translatable("requestboard.items", count).getString();
		}
		int stacks = count / maxStack;
		int rest = count % maxStack;
		if (stacks == 0) return Component.translatable("requestboard.items", rest).getString();
		String key = stacks == 1 ? "requestboard.stack" : "requestboard.stacks";
		if (rest == 0) return Component.translatable(key, stacks).getString();
		return Component.translatable(key + "_rest", stacks, rest).getString();
	}

	private static int darker(int color) {
		int r = (color >> 16 & 0xFF) * 6 / 10;
		int g = (color >> 8 & 0xFF) * 6 / 10;
		int b = (color & 0xFF) * 6 / 10;
		return 0xFF000000 | r << 16 | g << 8 | b;
	}
}
