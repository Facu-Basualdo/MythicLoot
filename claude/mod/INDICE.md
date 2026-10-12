# Índice — Mod (`src/main`)

Java 25 · Fabric API 0.162.0+26.3 (eventos de entidades y de daño, attachments, comandos) · Minecraft 26.3 (nombres de Mojang).

[← Hub](../../CLAUDE.md)

## Protocolo antes de tocar el código

1. Verificá la firma real de lo que uses de vanilla con `javap` (ver [minecraft-26-sin-ofuscar](../general/minecraft-26-sin-ofuscar.md)) en vez de confiar en tutoriales o en memoria.
2. Corré `gradlew runGameTest`.

## Notas

| Nota | De qué trata |
|---|---|
| [Botín: generar y soltar](botin.md) | `AFTER_DEATH` con `getLastHurtByPlayer()` (la memoria de "killed_by_player") y `MOB_DROPS` · con Bloodmoon sólo sueltan los mobs **con** nivel (spawners y huevos no: las granjas no cambian) y cualquier tipo que él nivele; sin él, sólo `Enemy`, con el nivel de la distancia al spawn · los attachments de Bloodmoon se leen por id con `AttachmentRegistryImpl.get`, cada vez (orden de inicialización) · el jefe suelta siempre, con `rarezaDeJefe` como mínima aunque su nivel todavía no la dé · material cada `nivelesPorMaterial`, netherita sólo legendaria · todo va en componentes de vanilla: `custom_data` (clave `botinmitico`), `attribute_modifiers` sumado a los de vanilla, `item_name` y `lore` |
| [Afijos](afijos.md) | 12 afijos; los que tienen atributo los aplica vanilla al equipar, los otros `Efectos` · **el id del modificador lleva el grupo de slots** (`botinmitico:robusto/head`): con el mismo id, dos piezas se pisan y no suman · vampírico y ardiente en `AFTER_DAMAGE`, con el arma de la mano o el arco de la flecha (`getWeaponItem()`) · certero en `ENTITY_LOAD` de la flecha, con `FlechaAccessor` porque 26.3 no tiene `getBaseDamage`, y la marca `FLECHA_POTENCIADA` para no aplicarlo dos veces · el sufijo es un sustantivo ("del Vampiro") para no concordar género |
