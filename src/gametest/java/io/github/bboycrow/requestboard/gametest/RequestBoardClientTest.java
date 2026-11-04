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
import io.github.bboycrow.requestboard.network.RBPayloads;
import io.github.bboycrow.requestboard.server.RequestManager;
import java.util.List;
import java.util.UUID;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen;
//?} else
/*import net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;*/
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
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
 * Drives the board like a player would and saves screenshots under build/run/clientGameTest/screenshots.
 * Run with: ./gradlew runClientGameTest
 */
public class RequestBoardClientTest implements FabricClientGameTest {
	private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-00000000a1e7");
	private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-0000000000b0");
	private static final UUID CAM = UUID.fromString("00000000-0000-0000-0000-000000000ca7");
	private static final UUID DANA = UUID.fromString("00000000-0000-0000-0000-00000000da7a");

	@Override
	public void runTest(ClientGameTestContext ctx) {
		ctx.runOnClient(mc -> {
			mc.options.languageCode = "fr_fr";
			mc.getLanguageManager().setSelected("fr_fr");
			mc.reloadResourcePacks();
		});
		ctx.waitTicks(100);
		ctx.runOnClient(mc -> mc.getWindow().setWindowed(1280, 720));
		ctx.waitTicks(10);

		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			//? if >=26.1 {
			sp.getConnection().waitForChunksRender();
			//?} else
			/*sp.getClientWorld().waitForChunksRender();*/
			ctx.runOnClient(mc -> {
				mc.options.guiScale().set(2);
				//? if >=26.1 {
				mc.resizeGui();
				//?} else
				/*mc.resizeDisplay();*/
				BoardStyle.choose(BoardStyle.MODERN);
			});
			TestServerContext server = sp.getServer();
			server.runCommand("gamemode creative @a");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule show_advancement_messages false");

			// ---- Blocks: single variants, a 2x2 board and a 3x2 board.
			BlockPos base = server.computeOnServer(s -> {
				ServerPlayer p = player(s);
				BlockPos b = p.blockPosition();
				ServerLevel level = p.level();
				place(level, b.offset(-7, 0, 6), BoardWood.SPRUCE, BoardRoof.DEEPSLATE_BRICKS);
				place(level, b.offset(-5, 0, 6), BoardWood.CHERRY, BoardRoof.BRICKS);
				place(level, b.offset(-3, 0, 6), BoardWood.CRIMSON, BoardRoof.BLACKSTONE);
				for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) place(level, b.offset(x - 1, y, 6), BoardWood.OAK, BoardRoof.STONE);
				for (int x = 0; x < 3; x++) for (int y = 0; y < 2; y++) place(level, b.offset(x + 3, y, 6), BoardWood.DARK_OAK, BoardRoof.MOSSY_COBBLESTONE);
				p.getInventory().add(new ItemStack(Items.DIAMOND, 32));
				p.getInventory().add(new ItemStack(Items.GOLDEN_APPLE, 5));
				return b;
			});
			server.runCommand("tp @a " + base.getX() + ".5 " + base.getY() + " " + base.getZ() + ".5 0 10");
			ctx.waitTicks(60);
			ctx.takeScreenshot("01_blocs");
			server.runOnServer(s -> { ServerPlayer p = player(s); p.getAbilities().flying = true; p.onUpdateAbilities(); });
			server.runCommand("tp @a " + base.getX() + ".5 " + (base.getY() + 4) + " " + (base.getZ() + 3) + ".5 0 50");
			ctx.waitTicks(30);
			ctx.takeScreenshot("02_toits_vus_du_dessus");
			server.runCommand("tp @a " + (base.getX() + 1) + ".5 " + (base.getY() + 1) + " " + (base.getZ() + 3) + ".5 0 5");
			ctx.waitTicks(20);
			ctx.takeScreenshot("02b_notes_aleatoires");
			server.runCommand("tp @a " + base.getX() + ".5 " + base.getY() + " " + base.getZ() + ".5 0 10");

			// ---- Creative tab with every variant
			ctx.getInput().pressKey(options -> options.keyInventory);
			ctx.waitForScreen(CreativeModeInventoryScreen.class);
			//? if >=26.1 {
			ctx.runOnClient(mc -> ((FabricCreativeModeInventoryScreen) mc.gui.screen()).setSelectedTab(RequestBoard.TAB));
			//?} else
			/*ctx.runOnClient(mc -> ((FabricCreativeInventoryScreen) mc.screen).setSelectedItemGroup(RequestBoard.TAB));*/
			ctx.waitTicks(10);
			ctx.takeScreenshot("03_onglet_creatif");
			ctx.getInput().pressKey(InputConstants.KEY_ESCAPE);
			ctx.waitTicks(5);

			// ---- Ten requests
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				RequestStorage storage = RequestManager.storage(s);
				long now = System.currentTimeMillis();
				UUID me = p.getUUID();
				String meName = p.getPlainTextName();
				add(storage, ALEX, "Alex_Mineur", Category.HARVEST, "Recherche minerai d'émeraude", "3 stacks, livraison à la base nord.",
					List.of(obj(Items.EMERALD_ORE, 192)), List.of(new ItemStack(Items.DIAMOND, 16)), now - 1000, 0, List.of());
				add(storage, BOB, "Bob_Builder", Category.SERVICE, "Dispo pour creuser un trou pour vous", "Je creuse ce que vous voulez, outils fournis.",
					List.of(), List.of(), now - 2000, now + 45_000, List.of(new Acceptor(CAM, "Cam", now)));
				add(storage, CAM, "Cam", Category.ANNOUNCEMENT, "Fête au village samedi soir !", "Ramenez des gâteaux.",
					List.of(obj(Items.CAKE, 3)), List.of(new ItemStack(Items.FIREWORK_ROCKET, 64)), now - 3000, now + 3 * 86_400_000L,
					List.of(new Acceptor(me, meName, now), new Acceptor(ALEX, "Alex_Mineur", now)));
				add(storage, BOB, "Bob_Builder", Category.HARVEST, "Besoin de bois de chêne", "",
					List.of(obj(Items.OAK_LOG, 640), obj(Items.OAK_PLANKS, 64), obj(Items.STICK, 32)),
					List.of(new ItemStack(Items.EMERALD, 10), new ItemStack(Items.BREAD, 20)), now - 4000, now + 5_400_000L, List.of());
				add(storage, DANA, "Dana_Lune", Category.SERVICE, "Escorte jusqu'à la forteresse du Nether", "Je connais le chemin, prévoyez de quoi manger.",
					List.of(), List.of(new ItemStack(Items.GOLD_INGOT, 24)), now - 5000, 0, List.of(new Acceptor(BOB, "Bob_Builder", now)));
				add(storage, me, meName, Category.HARVEST, "Récoltes pour le grand festin", "Pour le banquet de la mairie.",
					List.of(obj(Items.WHEAT, 128), obj(Items.CARROT, 64), obj(Items.POTATO, 64), obj(Items.PUMPKIN, 16), obj(Items.MELON_SLICE, 64)),
					List.of(new ItemStack(Items.EXPERIENCE_BOTTLE, 32)), now - 6000, now + 7_200_000L, List.of(new Acceptor(DANA, "Dana_Lune", now)));
				add(storage, ALEX, "Alex_Mineur", Category.ANNOUNCEMENT, "Ouverture de ma boutique au spawn", "Outils enchantés, échanges contre émeraudes.",
					List.of(), List.of(), now - 7000, 0, List.of());
				add(storage, CAM, "Cam", Category.HARVEST, "Plumes et cuir pour des livres", "",
					List.of(obj(Items.FEATHER, 40), obj(Items.LEATHER, 30)), List.of(new ItemStack(Items.BOOK, 5)), now - 8000, now + 1_800_000L, List.of());
				add(storage, DANA, "Dana_Lune", Category.SERVICE, "Je répare vos outils", "",
					List.of(), List.of(), now - 9000, 0, List.of(new Acceptor(ALEX, "Alex_Mineur", now), new Acceptor(CAM, "Cam", now)));
				add(storage, BOB, "Bob_Builder", Category.HARVEST, "Sable pour la verrerie", "",
					List.of(obj(Items.SAND, 1280)), List.of(new ItemStack(Items.IRON_INGOT, 32)), now - 10000, now + 600_000L, List.of());
				RequestManager.broadcastRequests(s);
				RequestManager.openBoard(p);
			});
			ctx.waitForScreen(RequestBoardScreen.class);
			ctx.waitTicks(40);
			ctx.takeScreenshot("04_moderne_10_requetes");
			ctx.getInput().setCursorPos(640, 400);
			ctx.getInput().scroll(-10);
			ctx.waitTicks(5);
			ctx.takeScreenshot("05_moderne_10_requetes_defile");
			ctx.getInput().scroll(10);
			ctx.waitTicks(2);
			click(ctx, 50, 60);
			ctx.waitTicks(5);
			ctx.takeScreenshot("06_moderne_detail");
			ctx.clickScreenButton("requestboard.action.accept");
			ctx.waitTicks(5);
			ctx.getInput().pressKey(InputConstants.KEY_ESCAPE);
			ctx.waitTicks(5);

			// ---- Switch to the adventure style with the button next to the tabs.
			click(ctx, 340 - 20 + 9, -20 + 9);
			ctx.waitTicks(5);
			check(ctx.computeOnClient(mc -> BoardStyle.current()) == BoardStyle.FANTASY, "style button did not switch");
			ctx.takeScreenshot("07_aventure_10_requetes");
			click(ctx, 50, 60);
			ctx.waitTicks(5);
			ctx.takeScreenshot("08_aventure_detail");
			ctx.getInput().pressKey(InputConstants.KEY_ESCAPE);
			ctx.waitTicks(5);
			ctx.clickScreenButton("requestboard.tab.mine");
			ctx.waitTicks(5);
			ctx.takeScreenshot("09_aventure_mes_contrats");

			// ---- Create page (fields drawn as ink on paper)
			ctx.clickScreenButton("requestboard.tab.create");
			ctx.waitTicks(5);
			click(ctx, 80, 60);
			ctx.getInput().typeChars("Recherche diamants");
			click(ctx, 80, 85);
			ctx.getInput().typeChars("Pour une armure complète.");
			ctx.clickScreenButton("requestboard.create.add_item");
			ctx.waitTicks(5);
			ctx.getInput().typeChars("diamant");
			ctx.waitTicks(5);
			click(ctx, 40 + 22 + 9, 10 + 52 + 9);
			ctx.waitTicks(5);
			click(ctx, 180 + 8, 140 + 3 * 16 + 2 + 8);
			clickButton(ctx, 180 + 16 + 8, 140 + 3 * 16 + 2 + 8, InputConstants.MOUSE_BUTTON_RIGHT);
			ctx.waitTicks(5);
			ctx.takeScreenshot("10_aventure_creation");

			click(ctx, 340 - 20 + 9, -20 + 9);
			ctx.waitTicks(5);
			ctx.takeScreenshot("11_moderne_creation");
			ctx.clickScreenButton("requestboard.create.publish");
			ctx.waitTicks(20);

			UUID created = server.computeOnServer(s -> RequestManager.storage(s).requests().stream()
				.filter(r -> r.creator().equals(player(s).getUUID()) && r.title().equals("Recherche diamants"))
				.map(BoardRequest::id).findFirst().orElse(null));
			check(created != null, "request was not created");
			int diamondsLeft = server.computeOnServer(s -> player(s).getInventory().countItem(Items.DIAMOND));
			check(diamondsLeft == 0, "diamonds were not withdrawn: " + diamondsLeft);
			ctx.clickScreenButton("requestboard.tab.mine");
			ctx.waitTicks(5);
			ctx.takeScreenshot("12_moderne_mes_contrats");

			// ---- Close with dispatch: 32 diamonds + 1 apple between 3 acceptors.
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
				for (UUID id : List.of(ALEX, BOB, CAM)) {
					for (ItemStack stack : storage.takePending(id)) {
						if (stack.is(Items.DIAMOND)) diamonds += stack.getCount();
						if (stack.is(Items.GOLDEN_APPLE)) apples += stack.getCount();
					}
				}
				return new int[] {diamonds, apples, storage.get(created) == null ? 1 : 0};
			});
			check(totals[0] == 32, "diamonds lost in dispatch: " + totals[0]);
			check(totals[1] == 1, "apple lost in dispatch: " + totals[1]);
			check(totals[2] == 1, "request not removed after close");

			// ---- Connections update when a board is broken.
			boolean split = server.computeOnServer(s -> {
				ServerLevel level = player(s).level();
				level.removeBlock(base.offset(4, 1, 6), false);
				BlockState left = level.getBlockState(base.offset(3, 1, 6));
				BlockState below = level.getBlockState(base.offset(4, 0, 6));
				return !left.getValue(RequestBoardBlock.RIGHT) && !below.getValue(RequestBoardBlock.UP) && below.getValue(RequestBoardBlock.LEFT);
			});
			check(split, "board connections did not update after breaking a block");
			// ---- Advancements
			for (String name : List.of("first_request", "accept_request", "patron")) {
				boolean done = server.computeOnServer(s -> {
					var holder = s.getAdvancements().get(RequestBoard.id(name));
					return holder != null && player(s).getAdvancements().getOrStartProgress(holder).isDone();
				});
				check(done, "advancement not granted: " + name);
			}
			ctx.setScreen(() -> null);
			ctx.waitTicks(5);
			ctx.runOnClient(mc -> {
				var advancements = mc.getConnection().getAdvancements();
				advancements.setSelectedTab(advancements.get(RequestBoard.id("root")), false);
				//? if >=26.1 {
				mc.gui.setScreen(new net.minecraft.client.gui.screens.advancements.AdvancementsScreen(advancements));
				//?} else
				/*mc.setScreen(new net.minecraft.client.gui.screens.advancements.AdvancementsScreen(advancements));*/
			});
			ctx.waitTicks(10);
			ctx.getInput().setCursorPos(480, 198);
			ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
			ctx.getInput().setCursorPos(1100, 650);
			ctx.waitTicks(20);
			ctx.takeScreenshot("13_progres");
			ctx.setScreen(() -> null);

			ctx.runOnClient(mc -> BoardStyle.choose(BoardStyle.MODERN));
			RequestBoard.LOGGER.info("[gametest] all checks passed");
		}
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
