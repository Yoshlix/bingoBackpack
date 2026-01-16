# Bingo Items System

Dieses System ermöglicht es, spezielle Items zum Bingo-Spiel hinzuzufügen.

## Features

- **Paper-Items**: Alle Bingo-Items erscheinen als Papier mit custom Name und Lore
- **Rechtsklick zum Einlösen**: Items werden durch Rechtsklick aktiviert
- **Raritäten-System**: 5 Stufen mit unterschiedlichen Drop-Chancen und Effektstärken
- **Mob-Drops**: Items können beim Töten von Mobs droppen
- **Bingo-Reihen**: Items können bei Bingo-Reihen-Vervollständigung gegeben werden

## Raritäten

| Rarity    | Farbe  | Base Drop-Chance | Effekt-Multiplikator | Dauer-Multiplikator |
|-----------|--------|------------------|----------------------|---------------------|
| COMMON    | Weiß   | 15%              | 1.0x                 | 1.0x                |
| UNCOMMON  | Grün   | 8%               | 1.5x                 | 1.25x               |
| RARE      | Blau   | 4%               | 2.0x                 | 1.5x                |
| EPIC      | Lila   | 1.5%             | 3.0x                 | 2.0x                |
| LEGENDARY | Gold   | 0.5%             | 5.0x                 | 3.0x                |

## Neues Item erstellen

### 1. Datei erstellen

Kopiere `_ItemTemplate.java` aus dem `items` Ordner und benenne sie um:

```
src/main/java/de/yoshlix/bingobackpack/item/items/MeinNeuesItem.java
```

### 2. Klasse anpassen

```java
public class MeinNeuesItem extends BingoItem {
    
    @Override
    public String getId() {
        return "mein_neues_item"; // Unique ID
    }
    
    @Override
    public String getName() {
        return "Mein Neues Item"; // Anzeigename
    }
    
    @Override
    public String getDescription() {
        return "Beschreibung was es tut.";
    }
    
    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE; // Wähle Rarity
    }
    
    @Override
    public boolean onUse(ServerPlayer player) {
        // Deine Logik hier!
        player.sendSystemMessage(Component.literal("§aEs funktioniert!"));
        return true; // true = Item wird verbraucht
    }
}
```

### 3. Item registrieren

In `BingoItemRegistry.java`, füge in der `init()` Methode hinzu:

```java
register(new MeinNeuesItem());
```

## Nützliche Methoden in `onUse()`

### Potion Effects
```java
player.addEffect(new MobEffectInstance(
    MobEffects.SPEED,           // Effekt-Typ
    getDurationTicks(30),       // Dauer (30 Sekunden * Rarity-Bonus)
    (int)(getEffectStrength()), // Stärke basierend auf Rarity
    false, true, true
));
```

### Heilen
```java
float healAmount = (float)(4.0 * getEffectStrength()); // 2 Herzen * Rarity
player.heal(healAmount);
```

### Teleportieren
```java
player.teleportTo(x, y, z);
```

### Nachrichten
```java
player.sendSystemMessage(Component.literal("§aGrüner Text!"));
```

### Bedingungen prüfen
```java
if (player.getHealth() >= player.getMaxHealth()) {
    player.sendSystemMessage(Component.literal("§cDu hast volle Leben!"));
    return false; // Item nicht verbrauchen
}
```

## Items bei Bingo-Reihen geben

Um einem Spieler ein Item zu geben (z.B. bei Bingo-Reihen):

```java
// Bestimmtes Item geben
BingoItemRegistry.getById("speed_boost")
    .ifPresent(item -> BingoItemManager.getInstance().giveItem(player, item));

// Zufälliges Item einer bestimmten Rarity
BingoItemManager.getInstance().giveRandomItem(player, ItemRarity.RARE);

// Komplett zufälliges Item
BingoItemManager.getInstance().giveRandomItem(player);
```

## Konfiguration

Der BingoItemManager bietet Konfigurationsmöglichkeiten:

```java
// Drop-Chance global anpassen (z.B. verdoppeln)
BingoItemManager.getInstance().setGlobalDropChanceMultiplier(2.0);

// Drops komplett deaktivieren
BingoItemManager.getInstance().setDropsEnabled(false);
```

## Beispiel-Items

Im `items` Ordner findest du folgende Beispiele:

- `ExampleSpeedBoostItem` - Speed Boost mit Potion Effect
- `ExampleHealingItem` - Heilung mit Bedingungsprüfung
- `ExampleNightVisionItem` - Einfacher Effekt
- `ExampleExtraHeartsItem` - Permanente Attribut-Änderung (Epic)
- `ExampleTeleportHomeItem` - Teleport, nur aus Bingo-Reihen (Legendary)

## Dateistruktur

```
src/main/java/de/yoshlix/bingobackpack/item/
├── ItemRarity.java          # Enum für Raritäten
├── BingoItem.java           # Abstrakte Basisklasse
├── BingoItemRegistry.java   # Registrierung aller Items
├── BingoItemManager.java    # Manager für Usage/Drops
└── items/
    ├── _ItemTemplate.java   # Template zum Kopieren
    ├── ExampleSpeedBoostItem.java
    ├── ExampleHealingItem.java
    ├── ExampleNightVisionItem.java
    ├── ExampleExtraHeartsItem.java
    └── ExampleTeleportHomeItem.java
```
