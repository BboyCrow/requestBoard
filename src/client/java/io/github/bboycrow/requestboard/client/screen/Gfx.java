package io.github.bboycrow.requestboard.client.screen;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
//? if >=1.21.9 {
import net.minecraft.world.entity.player.PlayerSkin;
//?} else
/*import net.minecraft.client.resources.PlayerSkin;*/
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
*///?}
//? if >=1.21.6
import net.minecraft.client.renderer.RenderPipelines;

/**
 * The drawing calls the board screen needs, under one name on every supported Minecraft version.
 * Minecraft renamed or reshaped most of them between 1.21.1, 1.21.11 and 26.x.
 */
public final class Gfx {
	//? if >=26.1 {
	public final GuiGraphicsExtractor raw;

	public Gfx(GuiGraphicsExtractor raw) {
		this.raw = raw;
	}
	//?} else {
	/*public final GuiGraphics raw;

	public Gfx(GuiGraphics raw) {
		this.raw = raw;
	}
	*///?}

	/**
	 * Before 1.21.6 items are drawn in front of everything else (they sit at z=150), so they would show through
	 * the detail scroll and the item picker. The screen sets this while drawing what lies under an overlay.
	 */
	//? if >=1.21.6 {
	private static final boolean ITEMS_DRAWN_ON_TOP = false;
	//?} else
	/*private static final boolean ITEMS_DRAWN_ON_TOP = true;*/

	/**
	 * True when drawing order can't be relied on (before 1.21.6, text and items have their own depth),
	 * so anything under an overlay would show through it.
	 */
	public static final boolean LEGACY_LAYERING = ITEMS_DRAWN_ON_TOP;
	public boolean underOverlay;

	/** Tooltip to draw last (1.21.1 draws tooltips immediately, so they are deferred). */
	private @Nullable Runnable pendingTooltip;

	/** Draws a whole texture of size texW x texH, stretched to w x h. */
	public void blit(Identifier texture, int x, int y, int w, int h, int texW, int texH) {
		//? if >=1.21.6 {
		this.raw.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, w, h, texW, texH, texW, texH);
		//?} else {
		/*// Text and fills are batched and drawn later; flush them so this image lands on top, in drawing order.
		this.raw.flush();
		this.raw.blit(texture, x, y, w, h, 0.0F, 0.0F, texW, texH, texW, texH);
		*///?}
	}

	public void fill(int x0, int y0, int x1, int y1, int color) {
		this.raw.fill(x0, y0, x1, y1, color);
	}

	public void outline(int x, int y, int w, int h, int color) {
		//? if >=26.1 {
		this.raw.outline(x, y, w, h, color);
		//?} else
		/*this.raw.renderOutline(x, y, w, h, color);*/
	}

	public void text(Font font, @Nullable String text, int x, int y, int color, boolean shadow) {
		//? if >=26.1 {
		this.raw.text(font, text, x, y, color, shadow);
		//?} else
		/*this.raw.drawString(font, text, x, y, color, shadow);*/
	}

	public void text(Font font, Component text, int x, int y, int color, boolean shadow) {
		//? if >=26.1 {
		this.raw.text(font, text, x, y, color, shadow);
		//?} else
		/*this.raw.drawString(font, text, x, y, color, shadow);*/
	}

	public void text(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
		//? if >=26.1 {
		this.raw.text(font, text, x, y, color, shadow);
		//?} else
		/*this.raw.drawString(font, text, x, y, color, shadow);*/
	}

	public void textWithBackdrop(Font font, Component text, int x, int y, int width, int color) {
		//? if >=26.1 {
		this.raw.textWithBackdrop(font, text, x, y, width, color);
		//?} else
		/*this.raw.drawStringWithBackdrop(font, text, x, y, width, color);*/
	}

	public void textWithWordWrap(Font font, FormattedText text, int x, int y, int width, int color, boolean shadow) {
		//? if >=26.1 {
		this.raw.textWithWordWrap(font, text, x, y, width, color, shadow);
		//?} elif >=1.21.6 {
		/*this.raw.drawWordWrap(font, text, x, y, width, color, shadow);
		*///?} else
		/*this.raw.drawWordWrap(font, text, x, y, width, color);*/
	}

	public void item(ItemStack stack, int x, int y) {
		if (ITEMS_DRAWN_ON_TOP && this.underOverlay) return;
		//? if >=26.1 {
		this.raw.item(stack, x, y);
		//?} else
		/*this.raw.renderItem(stack, x, y);*/
	}

	public void itemDecorations(Font font, ItemStack stack, int x, int y) {
		if (ITEMS_DRAWN_ON_TOP && this.underOverlay) return;
		//? if >=26.1 {
		this.raw.itemDecorations(font, stack, x, y);
		//?} else
		/*this.raw.renderItemDecorations(font, stack, x, y);*/
	}

	public void itemDecorations(Font font, ItemStack stack, int x, int y, @Nullable String countText) {
		if (ITEMS_DRAWN_ON_TOP && this.underOverlay) return;
		//? if >=26.1 {
		this.raw.itemDecorations(font, stack, x, y, countText);
		//?} else
		/*this.raw.renderItemDecorations(font, stack, x, y, countText);*/
	}

	public void setTooltipForNextFrame(Font font, ItemStack stack, int x, int y) {
		//? if >=1.21.6 {
		this.raw.setTooltipForNextFrame(font, stack, x, y);
		//?} else
		/*this.pendingTooltip = () -> this.raw.renderTooltip(font, stack, x, y);*/
	}

	public void setComponentTooltipForNextFrame(Font font, List<Component> lines, int x, int y) {
		//? if >=1.21.6 {
		this.raw.setComponentTooltipForNextFrame(font, lines, x, y);
		//?} else
		/*this.pendingTooltip = () -> this.raw.renderComponentTooltip(font, lines, x, y);*/
	}

	public void enableScissor(int x0, int y0, int x1, int y1) {
		this.raw.enableScissor(x0, y0, x1, y1);
	}

	public void disableScissor() {
		this.raw.disableScissor();
	}

	public void face(PlayerSkin skin, int x, int y, int size) {
		//? if >=26.1 {
		PlayerFaceExtractor.extractRenderState(this.raw, skin, x, y, size);
		//?} else
		/*PlayerFaceRenderer.draw(this.raw, skin, x, y, size);*/
	}

	/** Draws anything that must end up on top (only needed before 1.21.6). */
	public void flush() {
		if (this.pendingTooltip != null) {
			this.pendingTooltip.run();
			this.pendingTooltip = null;
		}
	}
}
