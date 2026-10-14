# Mythic Loot

Hostile mobs you kill can drop **weapons, bows, armor, shields and tools with a rarity and random affixes**: more damage, life steal, sharper arrows, more health, more armor… The higher the mob's level, the better the loot.

Made to be played with **Bloodmoon & Bosses** (mobs with levels) and **Hero's Forge** (stats bought with experience): Bloodmoon makes mobs stronger, Hero's Forge makes you stronger, and Mythic Loot keeps your gear improving past netherite with Sharpness V.

## When it drops

- **Only if you killed the mob** (or it died from a fall or fire after your hit, like vanilla loot).
- **The chance grows with the mob's level:** 2% plus 0.08% per level, up to 10%.
- **With Bloodmoon & Bosses:** the level is the mob's level, **champions have double the chance**, and **zone bosses always drop an epic item or better**, one in four with a **boss affix**. Mobs without a level (from spawners) drop nothing, so farms are unaffected.
- **Without Bloodmoon:** hostile mobs drop loot with a level based on distance from world spawn.
- Epic and legendary items drop **glowing** (visible through walls), with a sound and an on-screen message.

## Rarities

| Rarity | Color | Affixes | From level |
|---|---|---|---|
| Magic | blue | 1 | 1 |
| Rare | yellow | 2 | 20 |
| Epic | purple | 3 (+10%) | 50 |
| Legendary | gold | 4 (+25%), glowing | 80 |

## Item types

| Type | Items |
|---|---|
| Melee | sword, axe or spear; **from epic, also a mace** |
| Ranged | bow or crossbow |
| Armor | helmet, chestplate, leggings or boots |
| Trident | trident |
| Shield | shield (its affixes work in the **offhand**) |
| Tool | pickaxe or shovel |

- **The material depends on the level:** copper, then iron (chainmail for armor), iron, and diamond from level 60. **Netherite only on legendaries of level 80 or more.**
- They don't come enchanted: enchant them at the table like any other item. You can rename and repair them at the anvil without losing their affixes.

## Affixes

The name carries the main affix: **"Diamond Sword of the Vampire"**. The tooltip shows the rarity, the level and every affix.

| Affix | On | What it does | At max (level 100) |
|---|---|---|---|
| of the Edge | melee and tridents | Damage | +6 |
| of Fury | melee and tridents | Attack speed | +20% |
| of Reach | melee and tridents | Reach to mobs | +1 block |
| of the Vampire | weapons, bows and tridents | Heals part of the damage you deal | 8% |
| of Flames | weapons, bows and tridents | Sets targets (or arrows) on fire | 5 seconds |
| of the Hawk | bows, crossbows and tridents | Arrow and thrown trident damage | +40% |
| of the Wind | bows, armor and tools | Speed | +6% |
| of the Giant | armor and shields | Health | +2 hearts per piece |
| of the Fortress | armor and shields | Armor | +2 |
| of the Colossus | armor and shields | Armor toughness | +2 |
| of the Mountain | armor and shields | Knockback resistance | +10% |
| of Luck | armor and shields | Luck | +1 |
| of the Miner | tools | Mining speed, like Efficiency (stacks with it) | +12 |
| of the Horizon | tools | Reach to blocks | +1.5 blocks |
| of the Depths | tools | Underwater mining speed | +0.5 |

- **The value depends on the level:** a quarter of the max at level 1, the full value at level 100. Each item rolls between 75% and 100% of that, and epics and legendaries get 10% and 25% more.
- **Armor pieces stack:** four pieces "of the Giant" add their health together.

## Boss affixes

Each species of Bloodmoon & Bosses zone boss has its own affix, inspired by what that mob does. **One in four items a boss drops comes with it**, and it can't be found anywhere else.

| Boss | Affix | What it does | At max (level 100) |
|---|---|---|---|
| Zombie | of the Feast | Killing an enemy heals part of your max health | 10% |
| Skeleton | of the Volley | Chance for each arrow to fire an extra one | 30% |
| Vindicator | of the Executioner | Your hits ignore part of the enemy's armor | 30% |
| Witch | of the Brew | When hurt, chance to drink a brew like a witch (fire resistance, water breathing or instant health). Once every 20 seconds | 30% |
| Wither Skeleton | of Withering | Your hits inflict Wither II | 4 seconds |
| Piglin Brute | of the Brute | Take less melee damage | −20% |
| Blaze | of the Blaze | Take less fire and lava damage, and melee attackers catch fire | −50% |
| Enderman | of the Void | Chance to dodge an attack by teleporting up to 8 blocks. Once every 5 seconds | 20% |

Boss affixes work on any item type while it's equipped in its slot, and don't stack with themselves (the highest one counts).

## Merging at the anvil

Two matching items with affixes (two copper swords, two iron helmets…) combine at the anvil like in vanilla, and **the result keeps the affixes of both**:

- "Copper Sword of the Edge" + "Copper Sword of Fury" = a **rare** sword with damage and attack speed.
- **The rarity goes up with the number of affixes:** 2 rare, 3 epic, 4 legendary. If both have the same affix, the higher value stays.
- **Each affix gained or improved costs 4 levels**, on top of vanilla's repair or enchanting cost.

## Commands (op)

Commands are in Spanish:

| Command | What it does |
|---|---|
| `/mythicloot generar <level> [rarity] [type] [affix]` | Gives you an item with affixes of that level |
| `/mythicloot recargar` | Reloads the config |

## Configuration

Everything is configurable in `config/mythicloot.json`: drop chances, rarity weights and levels, how often each item type drops, each affix's max value, boss affixes per species, the anvil cost… It reloads with `/mythicloot recargar`, no restart needed. With **[Mod Menu](https://www.curseforge.com/minecraft/mc-mods/modmenu)** you can also edit it in-game.

## Languages

Chat messages follow each player's game language (**English or Spanish**). Item names and lore are translated on clients that have the mod; players without it see them in the config's language.

## Installation

All the logic runs on the server: items use vanilla components, so **a client without the mod sees them complete**. For singleplayer or LAN, install it on the client that opens the world.

Requires **Minecraft 26.3**, **Fabric Loader 0.19.5+**, **[Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)** and **Java 25**.

## Works well with

- **Bloodmoon & Bosses** (recommended): loot level is the mob's level, champions have double the chance, and zone bosses always drop an epic or better, sometimes with a boss affix.
- **Hero's Forge**: its life steal and ranged damage stack with the affixes, and its dodge rolls separately from "of the Void".
- **Soul Link**: no conflicts. With shared health, "of the Feast" and "of the Brew" heal the whole group.

License: MIT. Feel free to use it in modpacks.
