# AfkPlugin (Paper 1.21.11, Java 21)

UNTESTED: built without network/Paper access, so it has not been compiled. Build it
yourself and fix anything that doesn't compile — see "Known risks" below.

## Build
    ./gradlew build
Output: build/libs/AfkPlugin-2.0.0.jar -> drop into your server's /plugins folder.
Requires LuckPerms and PlayerKits2 installed (soft-dependency, so AfkPlugin will still
start without them, but ranks/kits won't work).

## First-time setup (as op, in-game)
1. `/afk wand` — gives a golden axe named "AFK Wand".
2. Left-click a block for corner 1, right-click a block for corner 2 of the AFK room.
3. `/afk setroom` — saves that region.
4. Stand where players should land, then `/afk setspawn`.
5. Place a red stained glass block just OUTSIDE the room (the "enter" pad) and another
   just INSIDE it (the "exit" pad). Any red stained glass block does this — it's not a
   specific marked block, so don't use red stained glass elsewhere for building.
6. `/afk reload` any time you edit config.yml by hand.

## Commands
/afk, /afk list, /afk wand, /afk setroom, /afk setspawn, /afk setip <ip>, /afk reload
/coins, /coins give <player> <amount>, /coins giveall <amount>, /coins take <player> <amount>
/coinshop
/baltop

## Permissions
afk.use, afk.list (default true) · afk.admin (op)
coins.use, coins.shop (default true) · coins.admin (op)

## Known risks (I could not test any of these)
1. LuckPerms group names: ranks dispatch `lp user <player> parent addtemp <group> 3d`
   from console. Confirm with `/lp group oskar+ info` etc. that these names resolve.
2. Kit command: defaults to `kit give {kit} {player}` run from console — set in
   config.yml under kits.give-command. Test in your console with a real kit name and
   fix the template if PlayerKits2 expects something else.
3. Whether PlayerKits2's console give bypasses permission checks. If kit delivery
   silently fails for players without /kit permission, this is likely why.

## Config (config.yml)
Everything is editable without recompiling: auto-AFK timeout, AFK block material,
room protection toggles, coin interval/sound, scoreboard IP/title, and the full
shop (ranks + kits, prices, durations, LuckPerms groups, kit names).

## Notes on what was built
- No kick — AFK is purely cosmetic/informational outside the room.
- "Came from" location is stored in memory only, so a restart while someone is
  AFK sends them to the world spawn when they step off, instead of back to where
  they were.
- Kills come from vanilla's PLAYER_KILLS statistic (permanent, player kills only).
- Coins only accrue while physically standing in the AFK room.
- Buying a rank you already have active (per this plugin's own ranks.yml) is blocked
  until it expires. Kits can be bought any number of times.
