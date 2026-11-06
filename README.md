# RequestBoard

A shared notice board for Minecraft servers (Fabric — Minecraft **1.21.1**, **1.21.11** and **26.3**). Players post **announcements**, **services** and **harvest requests**. Others browse them, accept them and get rewarded.

*Un tableau d'affichage partagé pour serveur Minecraft : chaque joueur publie ses annonces, services et demandes de récolte, et les autres les acceptent.*

See [CHANGELOG.md](CHANGELOG.md) for the release history.

## Features

- **Request Board block**: it takes the colour of the wood and stone used to craft it (12 woods × 16 stone roofs, all in their own creative tab). Boards facing the same way merge into one big board (2×2, 3×2…), and the pinned notes are laid out at random so a big board never looks repetitive.
- **Board**: request cards show the player head and name, a "your request" / "accepted" badge, the category, title, requested items with amounts, rewards, a live countdown and the list of players who accepted.
- **Filters**: by category and by player.
- **Detail view** on a parchment scroll, with Accept / Withdraw / Close / Delete. Several players can accept the same request, and everyone can see who accepted it.
- **New request page**: category, title, details, time limit, requested items picked from a searchable list (any amount, counted in stacks), and rewards clicked straight from your inventory.
- **My contracts**: your requests (close or delete in one click, with confirmation) and the contracts you accepted.
- **Edit your requests**: an *Edit* button in the request view changes the category, title, details, objective, reward and time limit (add, change or remove it). Each part can be locked in the config.
- **Contributions**: players who accepted a request say how many of each requested item they have (*My contribution*). The request shows what is pledged and what is still missing, and the card gets a progress bar. Informative only: nothing is taken from inventories. The creator can add what they already have in stock, and correct each participant's contribution (marked ✔).
- **Text rewards**: besides items, a request can promise a short written reward ("a house built for you").
- **Rewards**: optionally withdrawn when the request is posted. On close, they are split between the acceptors, evenly or in proportion to their contributions, and the remainder goes to a random player, the first to accept or back to the creator. When a request is deleted or expires, the rewards go back to the creator. Items owed to offline players are delivered at their next login.
- **Two looks**: *Modern* (sticky notes on a cork-like board) or *Adventure* (an old quest board with bounty posters and a dagger). Each player switches with the ✦ button next to the tabs; the server sets the default.
- **Advancements**: their own tab, from crafting a board to taking part in 100 completed contracts (see below).
- **Server-authoritative**: every action is validated server-side, and the server's config is synced to clients.

## Advancements

| Advancement | How to get it |
|---|---|
| Hear Ye, Hear Ye | Craft a request board |
| First Contract | Post your first request |
| Town Crier | Post 10 requests |
| Taskmaster (challenge) | Post 100 requests |
| Patron | Close a request whose reward is shared between 3 or more players |
| At Your Service | Accept another player's request |
| Mission Accomplished | Receive your share of a reward when a request is closed |
| Pillar of the Community (challenge) | Take part in 10 completed contracts |
| Village Legend (challenge) | Take part in 100 completed contracts |

Counters are kept per player in the world save, so players who were offline when a contract was closed get their advancements at their next login.

## Crafting

```
S S S     S = stone slab (sets the roof)
P B P     P = planks, F = fence (same wood, sets the board)
F   F     B = book and quill
```

## Configuration

`config/requestboard.json` is created on first launch. Each option is explained in the file's `_help` section. Operators can also change it in game with **`/requestboard config`**, an editor with a description of every setting, or reload the file with `/requestboard reload`. Both commands are for operators only.

| Option | Default | Meaning |
|---|---|---|
| `maxTitleLength` | 40 | Max title length (8–128) |
| `maxDetailsLength` | 300 | Max details length (0 hides the field) |
| `maxActiveRequestsPerPlayer` | 5 | Open requests per player (0 = unlimited) |
| `maxObjectiveEntries` | 5 | Different items per request |
| `maxStacksPerObjective` | 0 | Max stacks per requested item (0 = unlimited) |
| `harvestRequiresObjective` | true | Harvest requests must list an item |
| `maxAcceptors` | 0 | Players per request (0 = unlimited) |
| `allowAcceptOwnRequest` | false | Accept your own request |
| `operatorsCanRemoveAny` | true | Operators can delete any request |
| `broadcastNewRequests` | true | Announce new requests in every player's chat and in the server console |
| `playSounds` | true | Sounds on board events |
| `defaultStyle` | MODERN | Board look for players who haven't picked one: `MODERN` or `FANTASY` |
| `rewards.textEnabled` | true | Allow a short text reward |
| `rewards.maxTextLength` | 60 | Max length of the text reward (8–200) |
| `editing.enabled` | true | Let creators edit their requests |
| `editing.category/title/details/objectives/rewards/timeLimit` | true | Which parts can be edited (old item rewards go back to the creator when replaced) |
| `contributions.enabled` | true | Let participants declare what they have for a request |
| `contributions.creatorStock` | true | The creator can declare what they already have |
| `contributions.creatorCanAdjust` | true | The creator can correct participants' contributions |
| `categories.announcement/service/harvest` | true | Enable each category |
| `rewards.enabled` | true | Allow rewards |
| `rewards.autoWithdraw` | true | Take rewards from the creator's inventory when posting |
| `rewards.autoDispatch` | true | Split rewards between acceptors on close (needs `autoWithdraw`) |
| `rewards.splitMode` | EVEN | `EVEN` (equal shares) or `PRORATA` (in proportion to each participant's declared contribution) |
| `rewards.remainderMode` | RANDOM | `RANDOM`, `FIRST_COME` or `CREATOR` |
| `rewards.maxRewardStacks` | 9 | Reward stacks per request (1–9) |
| `timeLimit.enabled` | true | Allow time limits (expired requests disappear) |
| `timeLimit.maxMinutes` | 0 | Longest time limit (0 = no maximum) |

## Installation

Pick the jar matching your Minecraft version: `requestboard-<version>+1.21.1.jar`, `+1.21.11.jar` or `+26.3.jar`.
It needs Fabric Loader ≥ 0.20.1 and Fabric API, and must be installed on **the server and every client**.

On 1.21.1 there is no pale oak, so that board variant isn't available.

## Development

Requires JDK 25 to run Gradle (JDK 21 is fetched automatically for the 1.21.x builds).
The project uses [Stonecutter](https://stonecutter.kikugie.dev/) to build every Minecraft version from one source tree:
version-specific code sits in `//? if >=26.1 { ... }` comment blocks, and `src/` shows the active version (26.3 in git).

```bash
./gradlew build                          # jars for every version in versions/<mc>/build/libs/
./gradlew :1.21.1:runClient              # dev client for one version
./gradlew :26.3:runClientGameTest        # automated UI test (1.21.11 and 26.3), screenshots in versions/<mc>/build/run/clientGameTest
REQUESTBOARD_LANG=en_us ./gradlew :26.3:runClientGameTest   # same test with the game in English (default: fr_fr)
REQUESTBOARD_GALLERY=1 ./gradlew :26.3:runClientGameTest    # also takes the Modrinth gallery screenshots (1920x1080, English)
python tools/generate_assets.py          # regenerates models, textures and the per-version data in generated/<mc>/ (needs Pillow)
```

Check UI changes in **both English and French**: French strings are often longer.

To edit code for another version, run the Gradle task `"Set active project to 1.21.1"` (Stonecutter group), work, then switch back to 26.3 before committing.

Shared resources live in `src/main/resources`; data whose format changed between versions (recipes, loot tables, advancements, item models, blockstates) is generated per version in `generated/<mc>/`. Edit `tools/generate_assets.py`, not the generated files.

## License

MIT
