# Afijos

[← Índice Mod](INDICE.md)

Síntomas que esta nota evita: con casco y botas "del Gigante" el jugador tiene la vida de una sola pieza; el arco certero dispara igual que uno común; una flecha que vuelve con su chunk pega el doble; "Espada de hierro Afilado".

## Los doce

| Afijo (`id`) | Sufijo | Para | Cómo |
|---|---|---|---|
| `afilado` | del Filo | cuerpo a cuerpo | `ATTACK_DAMAGE`, `ADD_VALUE` |
| `furioso` | de la Furia | cuerpo a cuerpo | `ATTACK_SPEED`, `ADD_MULTIPLIED_BASE` |
| `largo` | del Alcance | cuerpo a cuerpo | `ENTITY_INTERACTION_RANGE`, `ADD_VALUE` |
| `vampirico` | del Vampiro | cuerpo a cuerpo, a distancia | `Efectos.alPegar`: cura `daño × valor` |
| `ardiente` | de las Llamas | cuerpo a cuerpo, a distancia | `Efectos.alPegar` (prende al herido) / `alDisparar` (prende la flecha) |
| `certero` | del Halcón | a distancia | `Efectos.alDisparar`: daño base × (1 + valor) |
| `ligero` | del Viento | a distancia, armadura | `MOVEMENT_SPEED`, `ADD_MULTIPLIED_BASE` |
| `robusto` | del Gigante | armadura | `MAX_HEALTH`, `ADD_VALUE` |
| `reforzado` | de la Fortaleza | armadura | `ARMOR`, `ADD_VALUE` |
| `tenaz` | del Coloso | armadura | `ARMOR_TOUGHNESS`, `ADD_VALUE` |
| `firme` | de la Montaña | armadura | `KNOCKBACK_RESISTANCE`, `ADD_VALUE` |
| `afortunado` | de la Suerte | armadura | `LUCK`, `ADD_VALUE` |

Una legendaria necesita 4 afijos distintos: **cada tipo tiene que tener al menos 4** (a distancia tiene justo 4). Si se saca uno, la legendaria de ese tipo sale con menos.

- **El sufijo es un sustantivo ("del Vampiro") y no un adjetivo:** con adjetivos habría que concordar con el objeto ("Espada Afilada", "Arco Afilado", "Botas Afiladas"), y el nombre base es traducible: el mod no sabe en qué idioma ni con qué género lo ve el cliente.
- Los valores máximos están en `Configuracion.valoresMaximos`, por id. El fuego va en segundos; los porcentajes, como fracción.

## Atributos: el id lleva el grupo de slots

**El modificador se llama `mythicloot:<afijo>/<grupo>`** (`mythicloot:robusto/head`, `mythicloot:afilado/mainhand`). Vanilla aplica los `attribute_modifiers` de cada pieza equipada con su id, y un `AttributeInstance` guarda **un modificador por id**: si el casco y las botas usaran `mythicloot:robusto`, la segunda pieza reemplazaría a la primera y no sumarían. Es el mismo motivo por el que vanilla nombra la armadura `armor.helmet`, `armor.boots`…

- El grupo de la armadura sale de `EQUIPPABLE.slot()` con `EquipmentSlotGroup.bySlot`; las armas y los arcos usan `MAINHAND`.
- Las armas y armaduras de vanilla ya traen `attribute_modifiers` (el daño de la espada, la armadura del peto): se suman con `withModifierAdded`, nunca se reemplazan (ver [Botín](botin.md#qué-se-guarda-en-el-objeto)).
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
- **`ENTITY_LOAD` también corre cuando la flecha vuelve a cargarse con su chunk**, y el daño subido ya viene en su NBT: la marca `FLECHA_POTENCIADA` (attachment persistente) evita multiplicarlo dos veces.

## Agregar un afijo

1. Un valor en `AfijoDeBotin`: id (para siempre), sufijo, tipos, y atributo y operación o una descripción para el lore.
2. Su valor máximo en `Configuracion.valoresPorDefecto()` (si falta, `validar` rechaza la config).
3. Si no es un atributo, el efecto en `Efectos`.
4. Una fila en la tabla del README y un caso en los tests.

## Ver también

- [Botín: generar y soltar](botin.md)
- [Tests: botín](../tests/botin.md)
