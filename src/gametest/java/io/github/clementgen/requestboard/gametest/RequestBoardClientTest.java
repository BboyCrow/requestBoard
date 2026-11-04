package io.github.clementgen.requestboard.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.clementgen.requestboard.RequestBoard;
import io.github.clementgen.requestboard.block.BoardRoof;
import io.github.clementgen.requestboard.block.BoardWood;
import io.github.clementgen.requestboard.block.RequestBoardBlock;
import io.github.clementgen.requestboard.client.screen.RequestBoardScreen;
import io.github.clementgen.requestboard.data.Acceptor;
import io.github.clementgen.requestboard.data.BoardRequest;
import io.github.clementgen.requestboard.data.Category;
import io.github.clementgen.requestboard.data.ObjectiveEntry;
import io.github.clementgen.requestboard.data.RequestStorage;
import io.github.clementgen.requestboard.network.RBPayloads;
import io.github.clementgen.requestboard.server.RequestManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * Drives the board like a player would and saves screenshots under run/screenshots.
 * Run with: ./gradlew runClientGameTest
 */
public class RequestBoardClientTest implements FabricClientGameTest {
	private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-00000000a1e7");
	private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-0000000000b0");
	private static final UUID CAM = UUID.fromString("00000000-0000-0000-0000-000000000ca7");

	@Override
	public void runTest(ClientGameTestContext ctx) {
		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			sp.getConnection().waitForChunksRender();
			ctx.runOnClient(mc -> {
				mc.options.guiScale().set(1);
				mc.resizeGui();
			});
			TestServerContext server = sp.getServer();
			server.runCommand("gamemode creative @a");
			server.runCommand("time set noon");
			server.runCommand("weather clear");

			// Place a few board variants in front of the player.
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				BlockPos base = p.blockPosition();
				p.teleportTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
				BoardWood[] woods = {BoardWood.OAK, BoardWood.SPRUCE, BoardWood.CHERRY, BoardWood.CRIMSON, BoardWood.BIRCH};
				BoardRoof[] roofs = {BoardRoof.STONE, BoardRoof.DEEPSLATE_BRICKS, BoardRoof.BRICKS, BoardRoof.BLACKSTONE, BoardRoof.SANDSTONE};
				for (int i = 0; i < woods.length; i++) {
					BlockPos pos = base.offset(-4 + i * 2, 0, 4);
					p.level().setBlockAndUpdate(pos, RequestBoard.REQUEST_BOARD.defaultBlockState()
						.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
						.setValue(RequestBoardBlock.WOOD, woods[i])
						.setValue(RequestBoardBlock.ROOF, roofs[i]));
				}
				p.getInventory().add(new ItemStack(Items.DIAMOND, 32));
				p.getInventory().add(new ItemStack(Items.GOLDEN_APPLE, 5));
				p.getInventory().add(new ItemStack(Items.IRON_PICKAXE));
			});
			server.runCommand("tp @a ~ ~ ~ 0 15");
			ctx.waitTicks(60);
			ctx.takeScreenshot("01_blocks");

			// Seed some requests from other (offline) players.
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				RequestStorage storage = RequestManager.storage(s);
				long now = System.currentTimeMillis();
				storage.add(new BoardRequest(UUID.randomUUID(), ALEX, "Alex_Mineur", Category.HARVEST, "Recherche minerai d'émeraude", "3 stacks, livraison à la base nord.",
					List.of(obj(Items.EMERALD_ORE, 192), obj(Items.DEEPSLATE_EMERALD_ORE, 20)), List.of(new ItemStack(Items.DIAMOND, 16)), true, now - 1000, 0, List.of()));
				storage.add(new BoardRequest(UUID.randomUUID(), BOB, "Bob_Builder", Category.SERVICE, "Dispo pour creuser un trou pour vous", "Je creuse ce que vous voulez, outils fournis.",
					List.of(), List.of(), false, now - 2000, now + 45_000, List.of(new Acceptor(CAM, "Cam", now))));
				storage.add(new BoardRequest(UUID.randomUUID(), CAM, "Cam", Category.ANNOUNCEMENT, "Fête au village samedi soir !", "Ramenez des gâteaux.",
					List.of(obj(Items.CAKE, 3)), List.of(new ItemStack(Items.FIREWORK_ROCKET, 64)), true, now - 3000, now + 3 * 86_400_000L,
					List.of(new Acceptor(p.getUUID(), p.getPlainTextName(), now), new Acceptor(ALEX, "Alex_Mineur", now))));
				storage.add(new BoardRequest(UUID.randomUUID(), BOB, "Bob_Builder", Category.HARVEST, "Besoin de bois de chêne", "",
					List.of(obj(Items.OAK_LOG, 640), obj(Items.OAK_PLANKS, 64), obj(Items.STICK, 32)), List.of(new ItemStack(Items.EMERALD, 10), new ItemStack(Items.BREAD, 20)), true, now - 4000, now + 5_400_000L, List.of()));
				RequestManager.broadcastRequests(s);
				RequestManager.openBoard(p);
			});
			ctx.waitForScreen(RequestBoardScreen.class);
			ctx.waitTicks(40);
			ctx.takeScreenshot("02_board");

			// Open the first card (newest = emerald request) and accept it.
			click(ctx, 50, 60);
			ctx.waitTicks(10);
			ctx.takeScreenshot("03_detail");
			ctx.clickScreenButton("requestboard.action.accept");
			ctx.waitTicks(10);
			ctx.takeScreenshot("04_detail_accepted");
			boolean accepted = server.computeOnServer(s -> RequestManager.storage(s).requests().stream()
				.anyMatch(r -> r.creator().equals(ALEX) && r.isAcceptedBy(player(s).getUUID())));
			check(accepted, "accept did not register on the server");
			ctx.getInput().pressKey(InputConstants.KEY_ESCAPE);
			ctx.waitTicks(5);

			// Category filter
			click(ctx, 85, 17);
			ctx.waitTicks(5);
			ctx.takeScreenshot("05_filter_announcement");

			// Create a request.
			ctx.clickScreenButton("requestboard.tab.create");
			ctx.waitTicks(5);
			ctx.takeScreenshot("06_create_empty");
			click(ctx, 80, 58);
			ctx.getInput().typeChars("Recherche diamants");
			click(ctx, 80, 85);
			ctx.getInput().typeChars("Pour une armure complete.");
			click(ctx, 295, 40);
			ctx.getInput().typeChars("90");
			ctx.clickScreenButton("requestboard.create.add_item");
			ctx.waitTicks(5);
			ctx.getInput().typeChars("diamond ore");
			ctx.waitTicks(5);
			ctx.takeScreenshot("07_picker");
			click(ctx, 40 + 22 + 9, 10 + 52 + 9);
			ctx.waitTicks(5);
			// Reward: click the diamonds (hotbar slot 0) in the inventory grid.
			click(ctx, 180 + 8, 140 + 3 * 16 + 2 + 8);
			// And one golden apple with a right click.
			clickButton(ctx, 180 + 16 + 8, 140 + 3 * 16 + 2 + 8, InputConstants.MOUSE_BUTTON_RIGHT);
			ctx.waitTicks(5);
			ctx.takeScreenshot("08_create_filled");
			ctx.clickScreenButton("requestboard.create.publish");
			ctx.waitTicks(20);
			ctx.takeScreenshot("09_board_after_create");

			UUID created = server.computeOnServer(s -> RequestManager.storage(s).requests().stream()
				.filter(r -> r.creator().equals(player(s).getUUID())).map(BoardRequest::id).findFirst().orElse(null));
			check(created != null, "request was not created");
			int diamondsLeft = server.computeOnServer(s -> player(s).getInventory().countItem(Items.DIAMOND));
			check(diamondsLeft == 0, "diamonds were not withdrawn: " + diamondsLeft);

			ctx.clickScreenButton("requestboard.tab.mine");
			ctx.waitTicks(10);
			ctx.takeScreenshot("10_mine");

			// Close with dispatch: 32 diamonds + 1 apple between 3 acceptors.
			int[] totals = server.computeOnServer(s -> {
				ServerPlayer p = player(s);
				RequestStorage storage = RequestManager.storage(s);
				BoardRequest r = storage.get(created);
				long now = System.currentTimeMillis();
				r.addAcceptor(new Acceptor(ALEX, "Alex_Mineur", now));
				r.addAcceptor(new Acceptor(BOB, "Bob_Builder", now + 1));
				r.addAcceptor(new Acceptor(CAM, "Cam", now + 2));
				RequestManager.handleAction(p, created, RBPayloads.Action.CLOSE);
				int diamonds = 0;
				int apples = 0;
				List<String> parts = new ArrayList<>();
				for (UUID id : List.of(ALEX, BOB, CAM)) {
					int d = 0;
					for (ItemStack stack : storage.takePending(id)) {
						if (stack.is(Items.DIAMOND)) d += stack.getCount();
						if (stack.is(Items.GOLDEN_APPLE)) apples += stack.getCount();
					}
					diamonds += d;
					parts.add(Integer.toString(d));
				}
				RequestBoard.LOGGER.info("[gametest] diamond split: {}", String.join("/", parts));
				return new int[] {diamonds, apples, storage.get(created) == null ? 1 : 0};
			});
			check(totals[0] == 32, "diamonds lost in dispatch: " + totals[0]);
			check(totals[1] == 1, "apple lost in dispatch: " + totals[1]);
			check(totals[2] == 1, "request not removed after close");
			ctx.waitTicks(10);
			ctx.takeScreenshot("11_mine_after_close");
			RequestBoard.LOGGER.info("[gametest] all checks passed");
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static ObjectiveEntry obj(Item item, int count) {
		return new ObjectiveEntry(BuiltInRegistries.ITEM.wrapAsHolder(item), count);
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

	/** Clicks at a position relative to the board panel's top-left corner (GUI units). */
	private static void click(ClientGameTestContext ctx, int x, int y) {
		clickButton(ctx, x, y, InputConstants.MOUSE_BUTTON_LEFT);
	}

	private static void clickButton(ClientGameTestContext ctx, int x, int y, int button) {
		double[] pos = ctx.computeOnClient(mc -> {
			int gw = mc.getWindow().getGuiScaledWidth();
			int gh = mc.getWindow().getGuiScaledHeight();
			int scale = mc.getWindow().getGuiScale();
			int left = (gw - 340) / 2;
			int top = Math.max(22, (gh - 240) / 2 + 10);
			return new double[] {(left + x) * scale + scale / 2.0, (top + y) * scale + scale / 2.0};
		});
		ctx.getInput().setCursorPos(pos[0], pos[1]);
		ctx.getInput().pressMouse(button);
	}
}
