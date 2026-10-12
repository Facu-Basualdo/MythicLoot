# Plan — Mod "Mythic Loot" (botín con afijos)

## Contexto

Con Bloodmoon & Bosses los mobs llegan al nivel 100 y con Hero's Forge el jugador sube con experiencia, pero el equipo deja de mejorar: desde la zona de nivel 50, la tabla de balance de Hero's Forge dice siempre "netherita con Filo V y Protección IV". Este mod suma un tercer eje de progresión: armas, arcos y armaduras con rareza y afijos al azar, mejores cuanto más nivel tiene el mob.

Mismas reglas que los otros mods: todo en el servidor (componentes de vanilla, sin ítems propios), config en JSON, gametests, docs en `claude/`.

## Decisiones

- **Sin ítems propios:** objetos de vanilla con `attribute_modifiers`, `custom_data`, `item_name`, `lore`. Un cliente vanilla los ve completos y no hace falta el mod del lado del cliente.
- **Afijos con atributo cuando se puede** (vanilla los aplica y los muestra); los demás (vampírico, ardiente, certero) por eventos de Fabric. Certero en `ENTITY_LOAD` de la flecha, con el arco de `getWeaponItem()`: sin mixins de daño.
- **Sufijos y no adjetivos** ("del Vampiro"): el nombre base es traducible y no hay que concordar género.
- **Con Bloodmoon, el nivel del mob; sin él, la distancia.** Sin depender de sus clases: sus attachments por id (`AttachmentRegistryImpl.get`). Con Bloodmoon, los mobs sin nivel (spawners, huevos) no sueltan: las granjas no cambian.
- **Rarezas en los mismos niveles que el botín raro de Bloodmoon** (20, 50, 80). Netherita sólo en legendarias.
- **Jefes de zona: siempre**, épica o mejor.

## Fases

1. **0.1.0 — hecha:** generar (rareza, material, afijos, nombre, lore), soltar al morir, los 12 afijos, la config, `/mythicloot generar` y `recargar`, y los gametests.

## Pendiente

- **Probarlo a mano** en un servidor con Bloodmoon & Bosses (y Hero's Forge y Soul Link): los gametests simulan los attachments de Bloodmoon, no lo cargan de verdad.
- Balance: con los valores por defecto, ¿una armadura completa "del Gigante" más los 15 corazones de Hero's Forge vuelven trivial la zona de nivel 100?
- Ideas para más adelante: afijos de herramientas (minado, fortuna), conjuntos con bonus, un "nivel requerido" para equipar.
