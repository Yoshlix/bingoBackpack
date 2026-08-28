# 🎒 BingoBackpack

A Minecraft Fabric mod that extends [Yet Another Bingo](https://gitlab.com/horrificdev/bingo) with team backpacks, 38 unique collectible items, and chaos-inducing gameplay mechanics.

## ✨ Features

### 🎒 Team Backpacks
Shared inventory system for bingo teams accessible via `/backpack`. All team members can store and retrieve items from a persistent shared storage.

### 🎁 Bingo Items (38 Unique Items)
Special collectible items that drop from mobs with game-changing effects. Items are categorized by rarity (COMMON, UNCOMMON, RARE, EPIC, LEGENDARY) and can be found in-game or spawned via commands.

### 🔧 Extended Bingo API
Full programmatic access to game state, card manipulation, scoring, and team management.

---

## 🎁 Bingo Items Overview

### 🎯 Bingo Field Manipulation
Directly modify the bingo card and field states:

- **Complete Random Bingo Field** (RARE) - Instantly completes a random uncompleted field
- **Complete Chosen Bingo Field** (RARE) - Choose which field to complete
- **Reroll Random Field** (UNCOMMON) - Rerolls a random field to a new objective
- **Reroll Chosen Field** (UNCOMMON) - Choose which field to reroll
- **Reset Field Progress** (COMMON) - Resets progress on a random enemy team field

### ✈️ Teleportation
Move instantly across the world:

- **Random Teleport** (COMMON) - Teleports to random coordinates within 10,000 blocks
- **Biome Teleport (Random)** (UNCOMMON) - Teleports to a random biome
- **Biome Teleport (Choice)** (RARE) - Choose your destination biome
- **Nether Teleport** (UNCOMMON) - Instant trip to the Nether
- **End Teleport** (RARE) - Direct portal to The End

### ⚔️ PvP & Sabotage
Mess with your opponents:

- **Kill Enemy Team** (LEGENDARY) - Eliminates all players from a random enemy team
- **Kill Random Player** (EPIC) - Kills one random player from any team
- **Swap Location (Random)** (UNCOMMON) - Swaps positions with a random player
- **Swap Location (Choice)** (RARE) - Choose who to swap with
- **Inventory Swap** (EPIC) - Swaps inventories with a random player
- **Item Swap** (RARE) - Swaps a random item from your inventory with a random player
- **Delete Enemy Items** (LEGENDARY) - Deletes random items from enemy team inventories

### 💨 Buffs & Effects
Powerful status effects to boost your performance:

- **Speed Boost (1 Min)** (COMMON) - Speed II + Haste I for 60 seconds
- **Speed Boost (5 Min)** (UNCOMMON) - Speed II + Haste II for 5 minutes
- **Speed Boost (15 Min)** (RARE) - Speed III + Haste III for 15 minutes
- **Flight (1 Min)** (RARE) - Creative flight for 60 seconds
- **Flight (5 Min)** (EPIC) - Creative flight for 5 minutes
- **Flight (15 Min)** (LEGENDARY) - Creative flight for 15 minutes

### 🎮 Game Control
Manipulate the game rules themselves:

- **Shuffle Bingo Card** (LEGENDARY) - Completely reshuffles the entire bingo card for all teams
- **Timeout Player** (RARE) - Prevents a chosen player from moving for 30 seconds
- **Timeout Team** (EPIC) - Freezes an entire enemy team for 30 seconds

### 🧭 Utility
Helpful tools for navigation and protection:

- **Bingo Radar** (UNCOMMON) - Shows direction and distance to nearest incomplete bingo objective
- **Team Shield** (EPIC) - Protects your team from PvP items for 5 minutes
- **Structure Finder** (RARE) - Locates the nearest structure (Village, Temple, Mansion, etc.)
- **Time Watch** (UNCOMMON) - Shows how much time has passed in the bingo game

### 🎲 Gambling & Chaos
High-risk, high-reward items with unpredictable outcomes:

- **Münzwurf des Schicksals** (LEGENDARY) - 50/50 chance: completes a field for you OR a random enemy team
- **Schrödingers Kiste** (RARE) - Spawns a chest with random loot: empty, dirt, diamonds, or rare bingo items
- **Instant Furnace** (UNCOMMON) - Instantly smelts all smeltable items in your inventory
- **Mob-Pheromone** (RARE) - Increases mob spawns in 30 block radius for 2 minutes
- **Tunnel-Bohrer** (EPIC) - Instantly digs a 3×3 tunnel, 30 blocks deep in your look direction

### 🌟 Special Items
Unique mechanics that don't fit other categories:

- **Levitation Dart** (UNCOMMON) - Shoots a projectile that gives enemies Levitation for 10 seconds
- **Lockdown** (LEGENDARY) - Prevents all players from using `/spawn` for 5 minutes
- **Wildcard** (EPIC) - Can be used as any other bingo item (one-time transformation)

### 💻 Real-Life Chaos (client-side)
Items that reach the target's actual computer. These need the mod installed **client-side** and are **off by default** — enable `pcPranksEnabled` in the config. The heart item is in-game and always available.

- **Glasherz / Weak Hearts** (EPIC) - Caps a random enemy's health at 3 hearts for 2 minutes (in-game, no client mod needed)
- **Blauer Bildschirm des Schicksals / Fake Shutdown** (EPIC) - Shows a dismissible fake "shutting down" overlay on a random enemy's screen
- **Bewerbungshilfe / Indeed** (EPIC) - Opens indeed.com in a random enemy's browser
- **Kopfstand / Monitor Flip** (EPIC) - Rotates a random enemy's displays 180° for 2 minutes, then reverts (**Windows only**)

---

## 📦 Installation

⚠️ **Important:** This mod requires a custom version of Yet Another Bingo!

**Requires Minecraft 26.2 and Java 25.**

1. Download **both** JAR files from the [Releases](../../releases):
   - `bingobackpack-*.jar` - This mod
   - `bingo-*+mc26.2.jar` - Custom Yet Another Bingo version (2.13.0-custom)

2. Place both JARs in your server's `mods/` folder

3. Install [Fabric API](https://modrinth.com/mod/fabric-api) (required dependency)

4. Start your server

---

## 🔨 Building from Source

```bash
# Clone with submodules (includes custom Bingo fork)
git clone --recursive https://github.com/YOUR_REPO/bingoBackpack.git
cd bingoBackpack

# Build everything (mod + dependencies)
./build.sh

# Output files will be in out/
# - bingobackpack-*.jar
# - bingo-*+mc26.2.jar
# - discord-service-*-all.jar
```

Requires a JDK that can build for Java 25.

---

## 🎯 How It Works

### Item Drops
When a player kills a mob, there's a configurable chance to drop a random bingo item (based on rarity weights). Items are NBT-tagged paper items with custom names, lore, and effects.

### Item Rarity
- **COMMON** (⚪) - Frequently dropped, basic effects
- **UNCOMMON** (🟢) - Moderate rarity, useful utilities
- **RARE** (🔵) - Rare drops, powerful effects
- **EPIC** (🟣) - Very rare, game-changing abilities
- **LEGENDARY** (🟡) - Extremely rare, ultimate power

### Team System
The mod integrates with Yet Another Bingo's team system. Team backpacks are persistent and survive server restarts. Many items affect entire teams or target enemy teams.

---

## 🛠️ For Developers

### Adding New Items

1. Create a new class extending `BingoItem`:
```java
public class MyNewItem extends BingoItem {
    public MyNewItem() {
        super("my_new_item", ItemRarity.RARE, true); // droppable
    }
    
    @Override
    protected boolean use(ServerPlayer player, ItemStack stack) {
        // Your item logic here
        return true; // Consumed successfully
    }
}
```

2. Register in `BingoItemRegistry.java`:
```java
register(new MyNewItem());
```

3. The item will automatically:
   - Be registered in the item system
   - Be added to the droppable pool (if marked droppable)
   - Work with `/backpack items give my_new_item`
   - Appear in `/backpack items list`

### API Access
```java
// Get BingoApi instance
BingoApi api = BingoBackpack.getBingoApi();

// Access teams, game state, objectives, etc.
api.getTeams();
api.getPlayerTeam(player);
api.completeGoal(team, goal);
```

---

## 📝 License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

---

## 🙏 Credits

- Built on [Fabric](https://fabricmc.net/)
- Extends [Yet Another Bingo](https://gitlab.com/horrificdev/bingo) by HorrificDev
- Developed for Minecraft 26.2 (Yet Another Bingo 2.13.0)
