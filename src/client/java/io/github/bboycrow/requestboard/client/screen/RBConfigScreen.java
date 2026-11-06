package io.github.bboycrow.requestboard.client.screen;

import io.github.bboycrow.requestboard.config.RBConfig;
import io.github.bboycrow.requestboard.network.RBPayloads;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else
/*import net.minecraft.client.gui.GuiGraphics;*/

/** In-game editor for the server config, opened with /requestboard config (operators only). */
public class RBConfigScreen extends Screen {
	private static final int W = 340;
	private static final int ROW_H = 20;
	private static final int CONTROL_W = 96;
	private static final int PANEL_BG = 0xF01C140E;
	private static final int BORDER = 0xFF8A6A42;
	private static final int GOLD = 0xFFFFD45A;
	private static final int TEXT = 0xFFF2E3C0;
	private static final int SOFT = 0xFFB8B0A0;

	/** One line of the editor. */
	private sealed interface Row permits Header, Toggle, Number, Choice {
		String key();
	}

	private record Header(String key) implements Row {
	}

	private record Toggle(String key, Supplier<Boolean> get, Consumer<Boolean> set) implements Row {
	}

	private record Number(String key, Supplier<Integer> get, Consumer<Integer> set, int min, int max) implements Row {
	}

	private record Choice(String key, List<String> values, Function<String, Component> label, Supplier<String> get, Consumer<String> set) implements Row {
	}

	private final RBConfig config;
	private final List<Row> rows = new ArrayList<>();
	private int left;
	private int top;
	private int bottom;
	private int scroll;

	public RBConfigScreen(RBConfig config) {
		super(Component.translatable("requestboard.config.title"));
		this.config = config;
		this.buildRows();
	}

	private void buildRows() {
		RBConfig c = this.config;
		this.rows.add(new Header("general"));
		this.rows.add(new Number("maxTitleLength", () -> c.maxTitleLength, v -> c.maxTitleLength = v, 8, 128));
		this.rows.add(new Number("maxDetailsLength", () -> c.maxDetailsLength, v -> c.maxDetailsLength = v, 0, 2000));
		this.rows.add(new Number("maxActiveRequestsPerPlayer", () -> c.maxActiveRequestsPerPlayer, v -> c.maxActiveRequestsPerPlayer = v, 0, 9999));
		this.rows.add(new Number("maxObjectiveEntries", () -> c.maxObjectiveEntries, v -> c.maxObjectiveEntries = v, 1, 32));
		this.rows.add(new Number("maxStacksPerObjective", () -> c.maxStacksPerObjective, v -> c.maxStacksPerObjective = v, 0, 99999));
		this.rows.add(new Toggle("harvestRequiresObjective", () -> c.harvestRequiresObjective, v -> c.harvestRequiresObjective = v));
		this.rows.add(new Number("maxAcceptors", () -> c.maxAcceptors, v -> c.maxAcceptors = v, 0, 9999));
		this.rows.add(new Toggle("allowAcceptOwnRequest", () -> c.allowAcceptOwnRequest, v -> c.allowAcceptOwnRequest = v));
		this.rows.add(new Toggle("operatorsCanRemoveAny", () -> c.operatorsCanRemoveAny, v -> c.operatorsCanRemoveAny = v));
		this.rows.add(new Toggle("broadcastNewRequests", () -> c.broadcastNewRequests, v -> c.broadcastNewRequests = v));
		this.rows.add(new Toggle("playSounds", () -> c.playSounds, v -> c.playSounds = v));
		this.rows.add(new Choice("defaultStyle", List.of("MODERN", "FANTASY"),
			v -> Component.translatable("requestboard.style." + v.toLowerCase(java.util.Locale.ROOT)), () -> c.defaultStyle, v -> c.defaultStyle = v));

		this.rows.add(new Header("categories"));
		this.rows.add(new Toggle("categories.announcement", () -> c.categories.announcement, v -> c.categories.announcement = v));
		this.rows.add(new Toggle("categories.service", () -> c.categories.service, v -> c.categories.service = v));
		this.rows.add(new Toggle("categories.harvest", () -> c.categories.harvest, v -> c.categories.harvest = v));

		this.rows.add(new Header("rewards"));
		this.rows.add(new Toggle("rewards.enabled", () -> c.rewards.enabled, v -> c.rewards.enabled = v));
		this.rows.add(new Toggle("rewards.autoWithdraw", () -> c.rewards.autoWithdraw, v -> c.rewards.autoWithdraw = v));
		this.rows.add(new Toggle("rewards.autoDispatch", () -> c.rewards.autoDispatch, v -> c.rewards.autoDispatch = v));
		this.rows.add(new Choice("rewards.splitMode", List.of("EVEN", "PRORATA"),
			v -> Component.translatable("requestboard.config.split." + v.toLowerCase(java.util.Locale.ROOT)),
			() -> c.rewards.splitMode.name(), v -> c.rewards.splitMode = RBConfig.SplitMode.valueOf(v)));
		this.rows.add(new Choice("rewards.remainderMode", List.of("RANDOM", "FIRST_COME", "CREATOR"),
			v -> Component.translatable("requestboard.config.remainder." + v.toLowerCase(java.util.Locale.ROOT)),
			() -> c.rewards.remainderMode.name(), v -> c.rewards.remainderMode = RBConfig.RemainderMode.valueOf(v)));
		this.rows.add(new Number("rewards.maxRewardStacks", () -> c.rewards.maxRewardStacks, v -> c.rewards.maxRewardStacks = v, 1, 9));
		this.rows.add(new Toggle("rewards.textEnabled", () -> c.rewards.textEnabled, v -> c.rewards.textEnabled = v));
		this.rows.add(new Number("rewards.maxTextLength", () -> c.rewards.maxTextLength, v -> c.rewards.maxTextLength = v, 8, 200));

		this.rows.add(new Header("timeLimit"));
		this.rows.add(new Toggle("timeLimit.enabled", () -> c.timeLimit.enabled, v -> c.timeLimit.enabled = v));
		this.rows.add(new Number("timeLimit.maxMinutes", () -> c.timeLimit.maxMinutes, v -> c.timeLimit.maxMinutes = v, 0, 999999));

		this.rows.add(new Header("editing"));
		this.rows.add(new Toggle("editing.enabled", () -> c.editing.enabled, v -> c.editing.enabled = v));
		this.rows.add(new Toggle("editing.category", () -> c.editing.category, v -> c.editing.category = v));
		this.rows.add(new Toggle("editing.title", () -> c.editing.title, v -> c.editing.title = v));
		this.rows.add(new Toggle("editing.details", () -> c.editing.details, v -> c.editing.details = v));
		this.rows.add(new Toggle("editing.objectives", () -> c.editing.objectives, v -> c.editing.objectives = v));
		this.rows.add(new Toggle("editing.rewards", () -> c.editing.rewards, v -> c.editing.rewards = v));
		this.rows.add(new Toggle("editing.timeLimit", () -> c.editing.timeLimit, v -> c.editing.timeLimit = v));

		this.rows.add(new Header("contributions"));
		this.rows.add(new Toggle("contributions.enabled", () -> c.contributions.enabled, v -> c.contributions.enabled = v));
		this.rows.add(new Toggle("contributions.creatorStock", () -> c.contributions.creatorStock, v -> c.contributions.creatorStock = v));
		this.rows.add(new Toggle("contributions.creatorCanAdjust", () -> c.contributions.creatorCanAdjust, v -> c.contributions.creatorCanAdjust = v));
	}

	/** The config being edited (not the active one until saved). */
	public RBConfig editedConfig() {
		return this.config;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private int listTop() {
		return this.top + 34;
	}

	private int listBottom() {
		return this.bottom - 28;
	}

	private int maxScroll() {
		return Math.max(0, this.rows.size() * ROW_H - (this.listBottom() - this.listTop()));
	}

	@Override
	protected void init() {
		this.left = (this.width - W) / 2;
		this.top = 12;
		this.bottom = this.height - 12;
		this.scroll = Math.max(0, Math.min(this.scroll, this.maxScroll()));

		int controlX = this.left + W - 14 - CONTROL_W;
		for (int i = 0; i < this.rows.size(); i++) {
			int y = this.listTop() + i * ROW_H - this.scroll + 2;
			if (y < this.listTop() || y + 16 > this.listBottom()) continue;
			Row row = this.rows.get(i);
			switch (row) {
				case Toggle t -> this.addRenderableWidget(Button.builder(onOff(t.get().get()), b -> {
					t.set().accept(!t.get().get());
					b.setMessage(onOff(t.get().get()));
				}).bounds(controlX, y, CONTROL_W, 16).build());
				case Choice ch -> this.addRenderableWidget(Button.builder(ch.label().apply(ch.get().get()), b -> {
					int next = (ch.values().indexOf(ch.get().get()) + 1) % ch.values().size();
					ch.set().accept(ch.values().get(next));
					b.setMessage(ch.label().apply(ch.get().get()));
				}).bounds(controlX, y, CONTROL_W, 16).build());
				case Number n -> {
					EditBox box = new EditBox(this.font, controlX + 1, y, CONTROL_W - 2, 16, Component.translatable("requestboard.config." + n.key()));
					box.setMaxLength(6);
					box.setValue(Integer.toString(n.get().get()));
					box.setResponder(value -> {
						String digits = value.replaceAll("\\D", "");
						if (!digits.equals(value)) {
							box.setValue(digits);
							return;
						}
						if (!digits.isEmpty()) {
							n.set().accept(Math.max(n.min(), Math.min(n.max(), Integer.parseInt(digits))));
						}
					});
					this.addRenderableWidget(box);
				}
				case Header h -> {
				}
			}
		}

		int by = this.bottom - 22;
		this.addRenderableWidget(Button.builder(Component.translatable("requestboard.config.defaults"), b -> {
			RBConfig defaults = new RBConfig();
			this.config.categories = defaults.categories;
			this.config.rewards = defaults.rewards;
			this.config.timeLimit = defaults.timeLimit;
			this.config.editing = defaults.editing;
			this.config.contributions = defaults.contributions;
			this.config.maxTitleLength = defaults.maxTitleLength;
			this.config.maxDetailsLength = defaults.maxDetailsLength;
			this.config.maxActiveRequestsPerPlayer = defaults.maxActiveRequestsPerPlayer;
			this.config.maxObjectiveEntries = defaults.maxObjectiveEntries;
			this.config.maxStacksPerObjective = defaults.maxStacksPerObjective;
			this.config.harvestRequiresObjective = defaults.harvestRequiresObjective;
			this.config.maxAcceptors = defaults.maxAcceptors;
			this.config.allowAcceptOwnRequest = defaults.allowAcceptOwnRequest;
			this.config.operatorsCanRemoveAny = defaults.operatorsCanRemoveAny;
			this.config.broadcastNewRequests = defaults.broadcastNewRequests;
			this.config.playSounds = defaults.playSounds;
			this.config.defaultStyle = defaults.defaultStyle;
			this.rows.clear();
			this.buildRows();
			this.rebuildWidgets();
		}).bounds(this.left + 10, by, 96, 16).build());
		this.addRenderableWidget(Button.builder(Component.translatable("requestboard.action.cancel"), b -> this.onClose())
			.bounds(this.left + W - 10 - 96 - 4 - 96, by, 96, 16).build());
		this.addRenderableWidget(Button.builder(Component.translatable("requestboard.config.save").withStyle(ChatFormatting.GREEN), b -> {
			ClientPlayNetworking.send(new RBPayloads.SaveConfig(this.config.toJson()));
			this.onClose();
		}).bounds(this.left + W - 10 - 96, by, 96, 16).build());
	}

	private static Component onOff(boolean value) {
		return value
			? Component.translatable("requestboard.config.on").withStyle(ChatFormatting.GREEN)
			: Component.translatable("requestboard.config.off").withStyle(ChatFormatting.RED);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		int before = this.scroll;
		this.scroll = Math.max(0, Math.min(this.maxScroll(), this.scroll - (int) (scrollY * ROW_H)));
		if (before != this.scroll) {
			this.rebuildWidgets();
		}
		return true;
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
		g.fill(this.left, this.top, this.left + W, this.bottom, PANEL_BG);
		g.outline(this.left, this.top, W, this.bottom - this.top, BORDER);
		Component title = this.title.copy().withStyle(ChatFormatting.BOLD);
		g.text(this.font, title, this.left + (W - this.font.width(title)) / 2, this.top + 8, GOLD, true);
		g.fill(this.left + 8, this.listTop() - 3, this.left + W - 8, this.listTop() - 2, BORDER);
		g.fill(this.left + 8, this.listBottom() + 2, this.left + W - 8, this.listBottom() + 3, BORDER);

		@Nullable Row hovered = null;
		g.enableScissor(this.left, this.listTop(), this.left + W, this.listBottom());
		for (int i = 0; i < this.rows.size(); i++) {
			int y = this.listTop() + i * ROW_H - this.scroll;
			if (y + ROW_H < this.listTop() || y > this.listBottom()) continue;
			Row row = this.rows.get(i);
			Component label = Component.translatable(row instanceof Header ? "requestboard.config.section." + row.key() : "requestboard.config." + row.key());
			if (row instanceof Header) {
				g.text(this.font, label.copy().withStyle(ChatFormatting.BOLD), this.left + 12, y + 7, GOLD, true);
			} else {
				int labelW = W - 14 - CONTROL_W - 30;
				g.text(this.font, this.font.plainSubstrByWidth(label.getString(), labelW), this.left + 22, y + 6, TEXT, true);
				if (mouseX >= this.left + 12 && mouseX < this.left + 22 + labelW && mouseY >= y + 2 && mouseY < y + 18
					&& mouseY >= this.listTop() && mouseY < this.listBottom()) {
					hovered = row;
				}
			}
		}
		g.disableScissor();
		if (this.maxScroll() > 0) {
			int trackTop = this.listTop();
			int trackH = this.listBottom() - trackTop;
			int contentH = this.rows.size() * ROW_H;
			int thumbH = Math.max(16, trackH * trackH / contentH);
			int thumbY = trackTop + (trackH - thumbH) * this.scroll / this.maxScroll();
			g.fill(this.left + W - 6, trackTop, this.left + W - 3, trackTop + trackH, 0x60000000);
			g.fill(this.left + W - 6, thumbY, this.left + W - 3, thumbY + thumbH, 0xFFD8C08A);
		}
		if (hovered != null) {
			List<FormattedCharSequence> lines = new ArrayList<>(this.font.split(Component.translatable("requestboard.config." + hovered.key()).withStyle(ChatFormatting.YELLOW), 220));
			lines.addAll(this.font.split(Component.translatable("requestboard.config." + hovered.key() + ".desc").withStyle(ChatFormatting.GRAY), 220));
			g.setTooltipLines(this.font, lines, mouseX, mouseY);
		}
		Component hint = Component.translatable("requestboard.config.hint");
		g.text(this.font, this.font.plainSubstrByWidth(hint.getString(), W - 16), this.left + (W - Math.min(W - 16, this.font.width(hint))) / 2, this.top + 20, SOFT, false);
	}
}
