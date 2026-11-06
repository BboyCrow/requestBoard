# Changelog

All notable changes to RequestBoard are listed here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).
Each release ships one jar per Minecraft version: `requestboard-<version>+<minecraft>.jar`.

## [1.1.0] - 2026-11-06

### Added
- **Edit a request.** The creator gets an *Edit* button in the request view to change the category, title, details, objective, reward and time limit (add, change or remove it). When item rewards are replaced, the old ones go back to the creator. Participants are told in chat.
- **Text rewards.** The *Reward* section has *Items* and *Text* tabs: a request can promise a short written reward ("a house built for you") alongside or instead of items. It is shown on the card and in the request view, and handed over in person.
- **Contributions.** Players who accepted a request can say how many of each requested item they have (*My contribution*). The request view shows what is pledged and what is missing for each item (with a per-player breakdown on hover), and the card gets a progress bar. Informative only: nothing is taken from inventories.
- **Creator's stock and corrections.** The creator can declare what they already have (counted in the progress) and correct each participant's contribution; corrections are marked ✔ and the player is told.
- **Pro rata reward split.** Rewards can be shared in proportion to each participant's declared contribution instead of equally.
- **In-game settings editor.** `/requestboard config` opens an editor for every setting, with a description on hover. Changes are saved on the server and sent to all players.
- New settings: `rewards.textEnabled`, `rewards.maxTextLength`, `rewards.splitMode`, `editing.*` (one switch per editable part and a global one), `contributions.enabled`, `contributions.creatorStock`, `contributions.creatorCanAdjust`.

### Changed
- New requests are now also announced to their creator and written to the server console.
- The request view scrolls when its content is long (many items, long details or many participants), with the header and buttons kept in place.
- Lists of more than 4 requested items scroll in the creation page and the contribution panel, with a position counter.
- Request cards have more breathing room; the "Accepted by" line no longer touches the bottom edge.
- When editing a request that offers no items yet, the *Items* tab shows the inventory right away.

### Fixed
- On Minecraft 1.21.1, text-area character counters no longer overlap the tabs and notes below them.
- `/requestboard` (and its `reload` and `config` subcommands) is confirmed to be operators-only; the server also re-checks operator rights when settings are saved.

## [1.0.0] - 2026-11-04

### Added
- **Request Board block**, coloured by the wood and stone used to craft it (12 woods × 16 stone roofs, 11 woods on 1.21.1), with its own creative tab. Boards facing the same way merge into one big board, with randomly pinned notes.
- **Board screen** with three categories (announcement, service, harvest), filters by category and player, a parchment request view, a creation page with a searchable item picker and inventory rewards, and a *My contracts* page.
- **Rewards** withdrawn when posting and split between participants when the request is closed, with a configurable rule for leftovers. Items owed to offline players are delivered at their next login.
- Optional **time limit** with a live countdown.
- **Two looks**: *Modern* (sticky notes) and *Adventure* (old quest board), chosen per player.
- **Nine advancements** with custom icons.
- JSON config (`config/requestboard.json`) synced from the server, reloadable with `/requestboard reload`.
- English and French translations.
- Builds for Minecraft 1.21.1, 1.21.11 and 26.3 (Fabric).

[1.1.0]: https://github.com/BboyCrow/requestBoard/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/BboyCrow/requestBoard/releases/tag/v1.0.0
