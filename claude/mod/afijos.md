# Afijos

[← Índice Mod](INDICE.md)

Síntomas que esta nota evita: con casco y botas "del Gigante" el jugador tiene la vida de una sola pieza; el arco certero dispara igual que uno común; el tridente certero lanzado pega lo mismo que uno común; el escudo "del Gigante" no da vida; una flecha que vuelve con su chunk pega el doble; "Espada de hierro Afilado".

## Los quince

| Afijo (`id`) | Sufijo | Para | Cómo |
|---|---|---|---|
| `afilado` | del Filo | cuerpo a cuerpo, tridente | `ATTACK_DAMAGE`, `ADD_VALUE` |
| `furioso` | de la Furia | cuerpo a cuerpo, tridente | `ATTACK_SPEED`, `ADD_MULTIPLIED_BASE` |
| `largo` | del Alcance | cuerpo a cuerpo, tridente | `ENTITY_INTERACTION_RANGE`, `ADD_VALUE` |
| `vampirico` | del Vampiro | cuerpo a cuerpo, a distancia, tridente | `Efectos.alPegar`: cura `daño × valor` |
| `ardiente` | de las Llamas | cuerpo a cuerpo, a distancia, tridente | `Efectos.alPegar` (prende al herido, también con el tridente lanzado) / `alDisparar` (prende la flecha) |
| `certero` | del Halcón | a distancia, tridente | Flechas: `Efectos.alDisparar`, daño base × (1 + valor). Tridente lanzado: `TridenteMixin` |
| `ligero` | del Viento | a distancia, armadura, herramienta | `MOVEMENT_SPEED`, `ADD_MULTIPLIED_BASE` |
| `robusto` | del Gigante | armadura, escudo | `MAX_HEALTH`, `ADD_VALUE` |
| `reforzado` | de la Fortaleza | armadura, escudo | `ARMOR`, `ADD_VALUE` |
| `tenaz` | del Coloso | armadura, escudo | `ARMOR_TOUGHNESS`, `ADD_VALUE` |
| `firme` | de la Montaña | armadura, escudo | `KNOCKBACK_RESISTANCE`, `ADD_VALUE` |
| `afortunado` | de la Suerte | armadura, escudo | `LUCK`, `ADD_VALUE` |
| `eficiente` | del Minero | herramienta | `MINING_EFFICIENCY`, `ADD_VALUE` (el atributo de Eficiencia: sólo con la herramienta correcta) |
| `amplio` | del Horizonte | herramienta | `BLOCK_INTERACTION_RANGE`, `ADD_VALUE` |
| `submarino` | de las Profundidades | herramienta | `SUBMERGED_MINING_SPEED`, `ADD_VALUE` (base 0,2) |

Una legendaria necesita 4 afijos distintos: **cada tipo tiene que tener al menos 4** (a distancia y herramienta tienen justo 4). Si se saca uno, la legendaria de ese tipo sale con menos.

- **El sufijo es un sustantivo ("del Vampiro") y no un adjetivo:** con adjetivos habría que concordar con el objeto ("Espada Afilada", "Arco Afilado", "Botas Afiladas"), y el nombre base es traducible: el mod no sabe en qué idioma ni con qué género lo ve el cliente.
- Los valores máximos están en `Configuracion.valoresMaximos`, por id. El fuego va en segundos; los porcentajes, como fracción.

## Atributos: sumados al de vanilla, o con el grupo de slots en el id

**Si el objeto ya trae un modificador del mismo atributo, operación y grupo de slots** (la armadura del peto, el daño de la espada, la dureza del diamante), `GeneradorDeBotin.sumar` le suma el valor del afijo y conserva el id de vanilla: con un modificador aparte el tooltip mostraba dos líneas ("Armadura: +4" y "Armadura: +0.5"). En el daño del arma eso además lo deja en la línea verde de vanilla, que ya incluye el daño base del jugador. El valor del afijo solo sigue en `custom_data`.

**Si no, el modificador se llama `mythicloot:<afijo>/<grupo>`** (`mythicloot:robusto/head`, `mythicloot:afilado/mainhand`). Vanilla aplica los `attribute_modifiers` de cada pieza equipada con su id, y un `AttributeInstance` guarda **un modificador por id**: si el casco y las botas usaran `mythicloot:robusto`, la segunda pieza reemplazaría a la primera y no sumarían. Es el mismo motivo por el que vanilla nombra la armadura `armor.helmet`, `armor.boots`…

- El grupo de la armadura sale de `EQUIPPABLE.slot()` con `EquipmentSlotGroup.bySlot`; **el escudo (`BLOCKS_ATTACKS`) usa `OFFHAND`**, que es donde se lleva (con `MAINHAND` sus afijos no valdrían nunca); las armas, los arcos, los tridentes y las herramientas, `MAINHAND`.
- Las armas y armaduras de vanilla ya traen `attribute_modifiers` (el daño de la espada, la armadura del peto): se suman (al modificador existente o con `withModifierAdded`), nunca se reemplazan (ver [Botín](botin.md#qué-se-guarda-en-el-objeto)).
- Al equipar, vanilla aplica los modificadores en el tick siguiente (`collectEquipmentChanges`): el test espera dos ticks.

## Los que aplica el mod (`Efectos`)

**Al pegar (`ServerLivingEntityEvents.AFTER_DAMAGE`):** si `fuente.getEntity()` es un `ServerPlayer` distinto del herido y el golpe no se bloqueó, se busca el arma del golpe:

| Golpe | Arma |
|---|---|
| Cuerpo a cuerpo (`getDirectEntity() == jugador`) | `getMainHandItem()` |
| Flecha (`getDirectEntity()` es `AbstractArrow`) | `flecha.getWeaponItem()`: el arco con el que se tiró, aunque el jugador ya lo haya cambiado de mano |

- **Vampírico** cura `daño × valor` (el daño real, después de la armadura del herido). `AFTER_DAMAGE` no corre si el golpe mató: el golpe final no cura.
- **Ardiente cuerpo a cuerpo** prende al herido `valor` segundos. A distancia no se hace acá: la flecha ya va prendida.

**Al disparar (`ServerEntityEvents.ENTITY_LOAD` de un `AbstractArrow` cuyo dueño es un `ServerPlayer`):**

- **Certero** multiplica el daño base (`setBaseDamage`). **26.3 no tiene `getBaseDamage`:** lo lee `FlechaAccessor` (`@Accessor("baseDamage")`, el único mixin). Al pegar, vanilla hace `ceil(velocidad × modifyDamage(baseDamage))`: Poder se suma a la base ya multiplicada por certero, y todo se multiplica por la velocidad, así que certero rinde más con el arco tensado del todo, como la flecha común. Con Hero's Forge, su daño a distancia multiplica el daño final y se suman los dos.
- **Ardiente** prende la flecha (`igniteForSeconds`): una flecha prendida prende lo que toca, como con Llama.
- **El tridente lanzado no pasa por acá** (`alDisparar` lo saltea): ver [El tridente lanzado](#el-tridente-lanzado).
- **`ENTITY_LOAD` también corre cuando la flecha vuelve a cargarse con su chunk**, y el daño subido ya viene en su NBT: la marca `FLECHA_POTENCIADA` (attachment persistente) evita multiplicarlo dos veces.

## El tridente lanzado

`ThrownTrident` es un `AbstractArrow`, pero **`onHitEntity` no usa el daño base**: pega un `8.0f` fijo (más `EnchantmentHelper.modifyDamage`, Empalamiento) y no llama al de la flecha, así que **tampoco prende al blanco** aunque el tridente vaya prendido (verificado con `javap` en 26.3).

- **Certero:** `TridenteMixin` (`@ModifyConstant` sobre el `8.0f` de `onHitEntity`) llama a `Efectos.danioDelTridente`, que lo multiplica por (1 + certero) del `getWeaponItem()` del tridente (el que se tiró).
- **Ardiente:** `alPegar` prende al herido si el golpe vino directo del jugador **o** de un `ThrownTrident`.
- **Vampírico:** como con las flechas: `getDirectEntity()` es el tridente y `getWeaponItem()` el objeto con los afijos.
- Con el tridente en la mano valen los afijos de cuerpo a cuerpo, como cualquier arma.

## Afijos de jefe

En `EfectosDeJefe`. Ocho (`festin`, `lluvia`, `verdugo`, `brebaje`, `marchito`, `bruto`, `llamarada`, `vacio`), con el segundo constructor de `AfijoDeBotin`: **sin tipos** (no salen en el sorteo de `generar`, y `sirvePara` da false), sin atributo, con descripción para el lore (en rojo), y `deJefe()` true. Los suelta sólo el jefe de zona de esa especie: ver [Botín](botin.md#afijos-de-jefe).

**Valen equipados, no en el arma del golpe:** `EfectosDeJefe.valor(entidad, afijo)` recorre los `EquipmentSlot` y toma el **máximo** (no la suma) de los objetos que están en su lugar (`lugarDe`: la armadura en el slot de su `equippable`, el escudo en `OFFHAND`, todo lo demás en `MAINHAND`). Así un casco "del Verdugo" sirve, y un casco en la mano o en la mochila no.

| Afijo | Dónde | Cómo |
|---|---|---|
| `festin` | `AFTER_DEATH` | Si el muerto es `Enemy` y `fuente.getEntity()` es un jugador: cura `vidaMáxima × valor` |
| `lluvia` | `Efectos.alDisparar` → `EfectosDeJefe.alDisparar` | Con chance `valor`, un `Arrow` nuevo con la velocidad levemente desviada, el daño base de la flecha (ya con certero), el crítico y el fuego; `pickup` `CREATIVE_ONLY`. La copia lleva `FLECHA_POTENCIADA` **antes** de `addFreshEntity`: si no, su `ENTITY_LOAD` volvería a tirar la lluvia. Si el arco no tiene afijos pero el jugador tiene lluvia (por un casco), igual se marca y se tira |
| `verdugo` | `DanioMixin`, `@WrapOperation` sobre `CombatRules.getDamageAfterAbsorb` en `getDamageAfterArmorAbsorb` | Si `fuente.getEntity()` es un jugador, la armadura del herido × (1 − valor). La dureza no cambia |
| `brebaje` | `AFTER_DAMAGE` del herido | Chance `valor`, con espera (`ULTIMO_BREBAJE`, attachment no persistente con el `gameTime`). Resistencia al fuego si se quema, respiración bajo el agua, si no curación instantánea I. **La instantánea se aplica con `applyInstantaneousEffect`**: con `addEffect` recién cura en el tick siguiente |
| `marchito` | `AFTER_DAMAGE` del atacante | Wither (`nivelDeMarchito`) por `valor` segundos (enteros, como el fuego: `enSegundos()`) |
| `bruto` | `DanioMixin`, `@ModifyVariable` al principio de `hurtServer` | Golpe cuerpo a cuerpo (`cuerpoACuerpo`: la entidad que causa es la directa y viva, ni proyectil, ni explosión, ni magia) × (1 − valor) |
| `llamarada` | El mismo `@ModifyVariable` + `AFTER_DAMAGE` | `DamageTypeTags.IS_FIRE` × (1 − valor), y prende `segundosDeLlamarada` al que le pegó cuerpo a cuerpo |
| `vacio` | `ALLOW_DAMAGE` | Ataques de algo (`getEntity()` no null, no `BYPASSES_INVULNERABILITY`), chance `valor`, con espera (`ULTIMO_TELETRANSPORTE`). Salta como la fruta de chorus (16 intentos, `randomTeleport` con `CONSUMABLE_DOES_NOT_TELEPORT_TO`); **sólo cancela el golpe si el salto salió** |

- **Fabric API no tiene un evento para cambiar el daño** (sólo `ALLOW_DAMAGE` para cancelarlo): por eso el mixin, con el mismo enganche que el daño de Hero's Forge. Los dos `@ModifyVariable` se encadenan.
- Todos los efectos son sólo de jugadores (`ServerPlayer`).
- Los números que no son el valor del afijo (nivel del Wither, duración y espera del brebaje, fuego de la llamarada, espera y distancia del vacío) están en `Configuracion.AfijosDeJefe`.

## Agregar un afijo

1. Un valor en `AfijoDeBotin`: id (para siempre), sufijo, tipos, y atributo y operación o una descripción para el lore. Uno de jefe usa el constructor de dos argumentos y va también en `afijosPorMob` (`Configuracion`).
2. Su valor máximo en `Configuracion.valoresPorDefecto()` (si falta, `validar` rechaza la config).
3. Si no es un atributo, el efecto en `Efectos`.
4. Una fila en la tabla del README y un caso en los tests.

## Ver también

- [Botín: generar y soltar](botin.md)
- [Tests: botín](../tests/botin.md)
- [Idioma](idioma.md): los sufijos y las descripciones están en los archivos de idioma.
