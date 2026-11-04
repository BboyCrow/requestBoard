package io.github.bboycrow.requestboard.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.bboycrow.requestboard.RequestBoard;
import io.github.bboycrow.requestboard.block.BoardRoof;
import io.github.bboycrow.requestboard.block.BoardWood;
import io.github.bboycrow.requestboard.block.RequestBoardBlock;
import io.github.bboycrow.requestboard.client.BoardStyle;
import io.github.bboycrow.requestboard.client.screen.RequestBoardScreen;
import io.github.bboycrow.requestboard.data.Acceptor;
import io.github.bboycrow.requestboard.data.BoardRequest;
import io.github.bboycrow.requestboard.data.Category;
import io.github.bboycrow.requestboard.data.ObjectiveEntry;
import io.github.bboycrow.requestboard.data.RequestStorage;
import io.github.bboycrow.requestboard.server.RequestManager;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Takes the screenshots used on the Modrinth page. Only runs when REQUESTBOARD_GALLERY=1 is set:
 * REQUESTBOARD_GALLERY=1 ./gradlew :26.3:runClientGameTest
 */
public class GalleryShots implements FabricClientGameTest {
	private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-00000000a1e7");
	private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-0000000000b0");
	private static final UUID CAM = UUID.fromString("00000000-0000-0000-0000-000000000ca7");
	private static final UUID DANA = UUID.fromString("00000000-0000-0000-0000-00000000da7a");

	@Override
	public void runTest(ClientGameTestContext ctx) {
		if (!"1".equals(System.getenv("REQUESTBOARD_GALLERY"))) {
			return;
		}
		//? if >=26.1 {
		ctx.runOnClient(mc -> {
			mc.options.languageCode = "en_us";
			mc.getLanguageManager().setSelected("en_us");
			mc.reloadResourcePacks();
		});
		ctx.waitTicks(100);
		ctx.runOnClient(mc -> mc.getWindow().setWindowed(1920, 1080));
		ctx.waitTicks(10);
		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			sp.getConnection().waitForChunksRender();
			ctx.runOnClient(mc -> {
				mc.options.guiScale().set(3);
				mc.resizeGui();
				BoardStyle.choose(BoardStyle.MODERN);
			});
			TestServerContext server = sp.getServer();
			for (String cmd : List.of("gamemode creative @a", "time set 1000", "weather clear", "gamerule show_advancement_messages false",
					"gamerule advance_time false", "gamerule advance_weather false", "gamerule spawn_mobs false")) {
				server.runCommand(cmd);
			}
			BlockPos base = server.computeOnServer(s -> player(s).blockPosition());
			int x = base.getX();
			int y = base.getY();
			int z = base.getZ();

			// ---- A small market square
			cmd(server, "fill %d %d %d %d %d %d minecraft:grass_block", x - 16, y - 1, z - 6, x + 16, y - 1, z + 20);
			cmd(server, "fill %d %d %d %d %d %d minecraft:air", x - 16, y, z - 6, x + 16, y + 12, z + 20);
			cmd(server, "fill %d %d %d %d %d %d minecraft:dirt_path", x - 1, y - 1, z - 6, x + 1, y - 1, z + 7);
			cmd(server, "fill %d %d %d %d %d %d minecraft:dirt_path", x - 12, y - 1, z + 6, x + 12, y - 1, z + 7);
			cmd(server, "fill %d %d %d %d %d %d minecraft:cobblestone", x - 3, y - 1, z + 8, x + 3, y - 1, z + 10);
			server.runOnServer(s -> {
				ServerLevel level = player(s).level();
				for (int bx = -1; bx <= 1; bx++) for (int by = 0; by <= 1; by++) {
					place(level, new BlockPos(x + bx, y + by, z + 9), BoardWood.SPRUCE, BoardRoof.DEEPSLATE_BRICKS);
				}
				place(level, new BlockPos(x - 5, y, z + 9), BoardWood.CHERRY, BoardRoof.BRICKS);
				place(level, new BlockPos(x + 5, y, z + 9), BoardWood.BIRCH, BoardRoof.SANDSTONE);
			});
			for (int side : new int[] {-3, 3}) {
				cmd(server, "setblock %d %d %d minecraft:spruce_fence", x + side, y, z + 8);
				cmd(server, "setblock %d %d %d minecraft:lantern", x + side, y + 1, z + 8);
			}
			cmd(server, "setblock %d %d %d minecraft:barrel[facing=up]", x - 7, y, z + 9);
			cmd(server, "setblock %d %d %d minecraft:barrel[facing=up]", x - 7, y + 1, z + 9);
			cmd(server, "setblock %d %d %d minecraft:hay_block", x + 7, y, z + 9);
			cmd(server, "setblock %d %d %d minecraft:composter", x + 7, y, z + 10);
			cmd(server, "setblock %d %d %d minecraft:potted_red_tulip", x - 2, y, z + 8);
			cmd(server, "setblock %d %d %d minecraft:potted_dandelion", x + 2, y, z + 8);
			for (int[] t : new int[][] {{-11, 14}, {10, 15}, {-14, 4}, {13, 3}}) {
				cmd(server, "place feature minecraft:oak %d %d %d", x + t[0], y, z + t[1]);
			}
			for (int i = 0; i < 26; i++) {
				int fx = x + (i * 7 % 29) - 14;
				int fz = z + (i * 11 % 24) - 4;
				if (Math.abs(fx - x) <= 2 || (fz >= z + 5 && fz <= z + 11)) continue;
				cmd(server, "setblock %d %d %d minecraft:%s", fx, y, fz, i % 3 == 0 ? "poppy" : i % 3 == 1 ? "dandelion" : "short_grass");
			}
			// Variant line-up, further back: two rows of six, the back row on a stone step
			cmd(server, "fill %d %d %d %d %d %d minecraft:stone_bricks", x - 7, y, z - 6, x + 7, y, z - 5);
			server.runOnServer(s -> {
				ServerLevel level = player(s).level();
				BoardWood[] woods = BoardWood.values();
				BoardRoof[] roofs = BoardRoof.values();
				for (int i = 0; i < woods.length; i++) {
					boolean back = i >= 6;
					int col = back ? i - 6 : i;
					BlockPos pos = back ? new BlockPos(x - 5 + col * 2, y + 1, z - 5) : new BlockPos(x - 5 + col * 2, y, z - 8);
					place(level, pos, woods[i], roofs[(i * 5) % roofs.length]);
				}
			});

			ctx.runOnClient(mc -> {
				if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
			});
			tp(server, x + 0.5, y + 0.6, z + 2.8, 0, 6);
			shot(ctx, 60, "01_market_square");
			tp(server, x + 0.5, y + 0.2, z + 5.6, 0, 10);
			shot(ctx, 30, "02_big_board_closeup");
			tp(server, x + 0.5, y + 1.4, z - 12.2, 0, 12);
			shot(ctx, 30, "03_wood_and_stone_variants");
						tp(server, x - 4.5, y + 0.4, z + 4.0, -30, 4);
			shot(ctx, 40, "04_market_side_view");
			ctx.runOnClient(mc -> {
				if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
			});

			// ---- Board screens
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				p.getInventory().add(new ItemStack(Items.DIAMOND, 48));
				p.getInventory().add(new ItemStack(Items.GOLDEN_APPLE, 6));
				p.getInventory().add(new ItemStack(Items.EMERALD, 30));
				seed(s);
				RequestManager.broadcastRequests(s);
				for (String id : List.of("root", "first_request", "accept_request", "requests_10", "mission_accomplished", "patron")) {
					var holder = s.getAdvancements().get(RequestBoard.id(id));
					if (holder != null) p.getAdvancements().award(holder, "done");
				}
			});
			tp(server, x + 0.5, y, z + 5.5, 0, 10);
			ctx.waitTicks(10);
			server.runOnServer(s -> RequestManager.openBoard(player(s)));
			ctx.waitForScreen(RequestBoardScreen.class);
			ctx.getInput().setCursorPos(1880, 1040);
			shot(ctx, 40, "05_board_modern");
			click(ctx, 330, -11);
			ctx.getInput().setCursorPos(1880, 1040);
			shot(ctx, 10, "06_board_adventure");
			click(ctx, 250, 130);
			shot(ctx, 10, "07_request_details");
			ctx.getInput().pressKey(InputConstants.KEY_ESCAPE);
			ctx.waitTicks(5);
			ctx.clickScreenButton("requestboard.tab.mine");
			shot(ctx, 10, "08_my_contracts");

			ctx.clickScreenButton("requestboard.tab.create");
			ctx.waitTicks(5);
			click(ctx, 80, 60);
			ctx.getInput().typeChars("Looking for diamonds");
			click(ctx, 80, 85);
			ctx.getInput().typeChars("For a full set of armour. Bring them to the north base!");
			click(ctx, 295, 40);
			ctx.getInput().typeChars("120");
			ctx.clickScreenButton("requestboard.create.add_item");
			ctx.waitTicks(5);
			ctx.getInput().typeChars("diamond");
			ctx.waitTicks(5);
			shot(ctx, 5, "09_item_picker");
			click(ctx, 40 + 22 + 9 + 18 * 2, 10 + 52 + 9);
			ctx.waitTicks(5);
			click(ctx, 180 + 16 * 2 + 8, 140 + 3 * 16 + 2 + 8);
			click(ctx, 180 + 8, 140 + 3 * 16 + 2 + 8);
			click(ctx, 330, -11);
			ctx.getInput().setCursorPos(1880, 1040);
			shot(ctx, 10, "10_new_request");
			ctx.setScreen(() -> null);
			ctx.waitTicks(5);

			ctx.runOnClient(mc -> {
				var advancements = mc.getConnection().getAdvancements();
				advancements.setSelectedTab(advancements.get(RequestBoard.id("root")), false);
				mc.gui.setScreen(new net.minecraft.client.gui.screens.advancements.AdvancementsScreen(advancements));
				mc.gui.toastManager().clear();
			});
			ctx.waitTicks(10);
			int[] tab = ctx.computeOnClient(mc -> new int[] {mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), mc.getWindow().getGuiScale()});
			ctx.getInput().setCursorPos(((tab[0] - 252) / 2 + 13) * tab[2], ((tab[1] - 140) / 2 - 14) * tab[2]);
			ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
			ctx.getInput().setCursorPos(1880, 1040);
			shot(ctx, 20, "11_advancements");
			ctx.setScreen(() -> null);

			ctx.getInput().pressKey(options -> options.keyInventory);
			ctx.waitTicks(5);
			ctx.runOnClient(mc -> ((net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen) mc.gui.screen()).setSelectedTab(RequestBoard.TAB));
			ctx.getInput().setCursorPos(1880, 1040);
			shot(ctx, 10, "12_creative_tab");
			ctx.setScreen(() -> null);
			ctx.runOnClient(mc -> BoardStyle.choose(BoardStyle.MODERN));
		}
		//?}
	}

	private static void shot(ClientGameTestContext ctx, int waitTicks, String name) {
		ctx.waitTicks(waitTicks);
		//? if >=26.1
		ctx.runOnClient(mc -> mc.gui.toastManager().clear());
		ctx.waitTicks(2);
		ctx.takeScreenshot("gallery_" + name);
	}

	private static void cmd(TestServerContext server, String format, Object... args) {
		server.runCommand(String.format(java.util.Locale.ROOT, format, args));
	}

	private static void tp(TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand(String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
	}

	private static void seed(MinecraftServer s) {
		ServerPlayer p = player(s);
		RequestStorage storage = RequestManager.storage(s);
		long now = System.currentTimeMillis();
		UUID me = p.getUUID();
		String meName = p.getPlainTextName();
		add(storage, ALEX, "Alex_Miner", Category.HARVEST, "Looking for emerald ore", "3 stacks please, delivery to the north base.",
			List.of(obj(Items.EMERALD_ORE, 192)), List.of(new ItemStack(Items.DIAMOND, 16)), now - 1000, 0, List.of(new Acceptor(DANA, "Dana_Moon", now)));
		add(storage, BOB, "Bob_Builder", Category.SERVICE, "Available to dig a hole for you", "Any size, tools provided.",
			List.of(), List.of(), now - 2000, now + 45_000, List.of(new Acceptor(CAM, "Cam", now)));
		add(storage, CAM, "Cam", Category.ANNOUNCEMENT, "Village party on Saturday night!", "Bring cakes and fireworks.",
			List.of(obj(Items.CAKE, 3)), List.of(new ItemStack(Items.FIREWORK_ROCKET, 64)), now - 3000, now + 3 * 86_400_000L,
			List.of(new Acceptor(me, meName, now), new Acceptor(ALEX, "Alex_Miner", now)));
		add(storage, BOB, "Bob_Builder", Category.HARVEST, "Need oak logs for the bridge", "",
			List.of(obj(Items.OAK_LOG, 640), obj(Items.OAK_PLANKS, 64), obj(Items.STICK, 32)),
			List.of(new ItemStack(Items.EMERALD, 10), new ItemStack(Items.BREAD, 20)), now - 4000, now + 5_400_000L, List.of());
		add(storage, DANA, "Dana_Moon", Category.SERVICE, "Escort to the Nether fortress", "I know the way, bring food.",
			List.of(), List.of(new ItemStack(Items.GOLD_INGOT, 24)), now - 5000, 0, List.of(new Acceptor(BOB, "Bob_Builder", now)));
		add(storage, me, meName, Category.HARVEST, "Crops for the town feast", "For the banquet at the town hall.",
			List.of(obj(Items.WHEAT, 128), obj(Items.CARROT, 64), obj(Items.POTATO, 64), obj(Items.PUMPKIN, 16), obj(Items.MELON_SLICE, 64)),
			List.of(new ItemStack(Items.EXPERIENCE_BOTTLE, 32)), now - 6000, now + 7_200_000L, List.of(new Acceptor(DANA, "Dana_Moon", now)));
		add(storage, ALEX, "Alex_Miner", Category.ANNOUNCEMENT, "My shop opens at spawn", "Enchanted tools, trades for emeralds.",
			List.of(), List.of(), now - 7000, 0, List.of());
		add(storage, CAM, "Cam", Category.HARVEST, "Feathers and leather for books", "",
			List.of(obj(Items.FEATHER, 40), obj(Items.LEATHER, 30)), List.of(new ItemStack(Items.BOOK, 5)), now - 8000, now + 1_800_000L, List.of());
	}

	private static void place(ServerLevel level, BlockPos pos, BoardWood wood, BoardRoof roof) {
		BlockState state = RequestBoard.REQUEST_BOARD.defaultBlockState()
			.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
			.setValue(RequestBoardBlock.WOOD, wood)
			.setValue(RequestBoardBlock.ROOF, roof);
		level.setBlockAndUpdate(pos, RequestBoardBlock.connect(state, level, pos));
	}

	private static void add(RequestStorage storage, UUID creator, String name, Category category, String title, String details,
			List<ObjectiveEntry> objectives, List<ItemStack> rewards, long createdAt, long expiresAt, List<Acceptor> acceptors) {
		storage.add(new BoardRequest(UUID.randomUUID(), creator, name, category, title, details, objectives, rewards, !rewards.isEmpty(),
			createdAt, expiresAt, acceptors));
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static ObjectiveEntry obj(Item item, int count) {
		return new ObjectiveEntry(BuiltInRegistries.ITEM.wrapAsHolder(item), count);
	}

	private static void click(ClientGameTestContext ctx, int x, int y) {
		double[] pos = ctx.computeOnClient(mc -> {
			int gw = mc.getWindow().getGuiScaledWidth();
			int gh = mc.getWindow().getGuiScaledHeight();
			int scale = mc.getWindow().getGuiScale();
			int left = (gw - 340) / 2;
			int top = Math.max(22, (gh - 240) / 2 + 10);
			return new double[] {(left + x) * scale + scale / 2.0, (top + y) * scale + scale / 2.0};
		});
		ctx.getInput().setCursorPos(pos[0], pos[1]);
		ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
	}
}
