# BingoBackpack

A Fabric mod that extends [Yet Another Bingo](https://gitlab.com/horrificdev/bingo) with additional features.

## Features

- **Team Backpacks** - Shared inventory for bingo teams via `/backpack`
- **Bingo Items** - Special collectible items that drop from mobs with various effects
- **Troll Effects** - Fun effects via `/trolls enable|disable|toggle|status`
- **Extended Bingo API** - Full access to game state, card manipulation, and scoring

## Installation

⚠️ **Important:** This mod requires a custom version of Yet Another Bingo!

1. Download **both** JAR files from the [Releases](../../releases):
   - `bingobackpack-*.jar` - This mod
   - `bingo-*-all.jar` - Custom Yet Another Bingo

2. Place both JARs in your server's `mods/` folder

3. Also install [Fabric API](https://modrinth.com/mod/fabric-api)

## Building from Source

```bash
# Clone with submodules
git clone --recursive https://github.com/YOUR_REPO/bingoBackpack.git

# Build everything
./build.sh

# Output files will be in out/
```

## Commands

- `/backpack` - Open team backpack
- `/backpack items on|off` - Toggle bingo item drops
- `/backpack items give <item> [player]` - Give a bingo item
- `/backpack items list` - List all bingo items
- `/trolls enable|disable|toggle|status` - Manage troll effects
