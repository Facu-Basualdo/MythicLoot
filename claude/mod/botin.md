# Botín: generar y soltar

[← Índice Mod](INDICE.md)

Síntomas que esta nota evita: una granja de spawners llena de legendarias; un mob que se mata con lava suelta botín; la espada con afijos pierde su daño de vanilla; un jefe de nivel 30 suelta una mágica; el mod revienta al arrancar porque Bloodmoon todavía no registró sus attachments.

## Cuándo se suelta (`Caida`)

**`Caida.alMorir` corre en `ServerLivingEntityEvents.AFTER_DEATH`**, y sale sin hacer nada si:

| Condición | Por qué |
|---|---|
| No es un `Mob` o no es del servidor | — |
| Sin Bloodmoon, no es `Enemy` | Sin niveles, "hostil" es lo único que separa un zombi de una vaca. Con Bloodmoon decide él quién tiene nivel (también piglins, que no son `Enemy`) |
| `getLastHurtByPlayer()` es null | Es la misma memoria que usa vanilla para el loot "killed_by_player": vale si lo remató la caída o el fuego después de un golpe, no si murió solo en una granja |
| `GameRules.MOB_DROPS` apagada | Como el resto del loot |

`Caida.sortear(mob, config, random)` decide la rareza, o null si no suelta. Está separado para probarlo sin matar nada:

1. **Sin nivel, nada** (`NivelDelMob.nivel` null).
2. **Jefe de zona** (con `botinDeJefe`): suelta siempre, con `rarezaDeJefe` (épica) como mínima.
3. Si no, la chance `min(chanceBase + chancePorNivel × nivel, chanceMaxima)`, por `multiplicadorCampeon` si es campeón.

El objeto cae con `spawnAtLocation`. **Épicas y legendarias quedan brillando** (`setGlowingTag`, el contorno se ve a través de las paredes), con sonido y un "¡Botín épico!" en la barra de acción del que lo mató.

## El nivel (`NivelDelMob`)

| | Nivel | Jefe / campeón |
|---|---|---|
| Con Bloodmoon (`isModLoaded("bloodmoonandbosses")`) | El attachment `bloodmoonandbosses:nivel` del mob. **Sin él, null: no suelta** | `bloodmoonandbosses:jefe` / `bloodmoonandbosses:afijo` |
| Sin Bloodmoon | `porDistancia`: 1 cada `bloquesPorNivel` desde el spawn, Nether ×8 + `extraNether`, End desde (0, 0) + `extraEnd` (la cuenta de Bloodmoon por defecto, sin días ni campeones) | Nunca |

- **Con Bloodmoon, los mobs de spawners, huevos y comandos no tienen nivel a propósito** (para que las granjas no cambien), y por eso tampoco sueltan botín con afijos. Sin Bloodmoon no se puede distinguir: un mob de spawner cuenta, con el nivel bajo del lugar.
- **No se depende de las clases de Bloodmoon:** `AttachmentRegistryImpl.get(Identifier)` busca sus attachments por id. Los ids no pueden cambiar nunca (están en el NBT de los mobs de todos los mundos), así que son un contrato más firme que un nombre de clase. `AttachmentRegistryImpl` es de la implementación de Fabric API: si un día cambia, el síntoma es un error de compilación acá.
- **Se buscan en cada uso y no al iniciar:** Fabric no garantiza el orden en que inicializa los mods, y Bloodmoon podría registrarlos después. Es un get de un mapa.
- `NivelDelMob.usarBloodmoon` se decide en `onInitialize`; los tests lo fuerzan.

## Generar (`GeneradorDeBotin`)

`generar(config, nivel, rareza, tipo, random)` → `armar(stack, ObjetoConAfijos, principal)`. Los atributos de armadura van en el grupo del slot de su `equippable`; todo lo demás, en la mano principal. `rearmar` cambia los afijos de un objeto que ya los tenía (ver [yunque](yunque.md)).

**Rareza** (`elegirRareza`): por peso entre las que ya salen con ese nivel (`rarezas.<id>.nivelMinimo`); con una mínima, sólo las iguales o mejores. **Si ninguna tan buena sale todavía, da la mínima igual**: el jefe la asegura. La mágica tiene que salir desde el nivel 1 (`validar`), o un mob de nivel bajo no tendría qué soltar.

**Material** (`base`): `nivel / nivelesPorMaterial`, tope 4.

| Índice | Nivel (por defecto) | Armas | Armadura |
|---|---|---|---|
| 0 | 1–19 | cobre | cobre |
| 1 | 20–39 | hierro | cadena |
| 2 | 40–59 | hierro | hierro |
| 3 | 60–79 | diamante | diamante |
| 4 | 80+ | netherita **sólo si es legendaria** (`netheritaSoloLegendaria`), si no diamante | igual |

A distancia es arco (2 de 3) o ballesta, en cualquier nivel.

**Afijos:** tantos como `rareza.afijos()` (1 a 4), sin repetir, entre los que `sirvenPara` el tipo. El valor es `AfijoDeBotin.valor(maximo, nivel, nivelMaximo, azar, rareza)` = `maximo × (0,25 + 0,75 × nivel / nivelMaximo) × (0,75 + 0,25 × azar) × bonificación` (épica 1,1, legendaria 1,25), redondeado a 3 decimales (el fuego, a segundos enteros). El primero que sale da el nombre.

## Qué se guarda en el objeto

Todo en **componentes de vanilla**: un cliente sin el mod lo ve entero, y el objeto viaja a cofres, al yunque o a otro jugador.

| Componente | Qué lleva |
|---|---|
| `custom_data` | Bajo la clave `mythicloot`: `{nivel, rareza, afijos: {id: valor}}` (`ObjetoConAfijos`). `CustomData.update` no pisa las claves de otros mods |
| `attribute_modifiers` | **Los de vanilla más los de los afijos** (sumados al modificador de vanilla del mismo atributo, o agregados con `withModifierAdded`): si se pusiera uno nuevo, la espada perdería su daño y el peto su armadura |
| `item_name` | El nombre traducible de vanilla + el sufijo, del color de la rareza. En `item_name` y no `custom_name`: no sale en cursiva ni como "renombrado", y un yunque igual lo puede renombrar |
| `lore` | "Rareza: épica", "Nivel 57" y una línea por cada afijo que no es atributo (los atributos los muestra vanilla) |
| `enchantment_glint_override` | Sólo en las legendarias |

- **`ObjetoConAfijos.de` ignora un id que no existe** (un afijo sacado en una versión nueva) y devuelve null si la rareza no se reconoce: el objeto queda como uno común con sus atributos.
- Los objetos no salen encantados: se pueden encantar en la mesa como cualquier otro.

## Config

`config/mythicloot.json`, leído como en Bloodmoon (`ArchivoDeConfiguracion`): se mezcla sobre `POR_DEFECTO` clave por clave (también dentro de los mapas), se valida y se reescribe completo; un archivo con error se rechaza sin tocarlo y sigue la config anterior. `/mythicloot recargar` lo vuelve a leer. **Los mapas (`rarezas`, `pesosDeTipo`, `valoresMaximos`) van por id**: una clave mal escrita es un `warn` y se ignora.

## Ver también

- [Afijos](afijos.md)
- [Juntar en el yunque](yunque.md)
- [Tests: botín](../tests/botin.md)
- [Probar en el juego](../general/probar-en-el-juego.md)
- [Idioma](idioma.md): el nombre y el lore se guardan traducibles, con respaldo en el idioma de la config.
