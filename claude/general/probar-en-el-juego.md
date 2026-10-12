# Probar en el juego

[← Hub](../../CLAUDE.md)

Los gametests ([tests: botín](../tests/botin.md)) cubren la lógica, pero no muestran cómo se ve el tooltip en el cliente, ni si una legendaria de nivel 90 se siente como premio o como regalo. Para eso hace falta un servidor y un cliente.

## Pasos

1. `gradlew runServer` una vez. Crea `run/` y se cierra pidiendo la EULA.
2. En `run/eula.txt` poné `eula=true` (es aceptar la EULA de Mojang: lo hace la persona, no el agente).
3. En `run/server.properties` poné **`online-mode=false`**: los clientes de desarrollo no tienen cuenta autenticada y sin esto el servidor los rechaza.
4. `gradlew runServer` de nuevo, y `op Player…` en la consola para poder usar `/mythicloot`.
5. `gradlew runClient` → *Multijugador → Conexión directa → `localhost`*.

Para probarlo con Bloodmoon & Bosses, copiá su jar (`BloodmoonAndBosses/build/libs/bloodmoonandbosses-<versión>.jar`) a `run/mods/` antes de arrancar el servidor.

**Después de cambiar código hay que reiniciar el cliente y el servidor:** no hay hot reload.

## Comandos

| Comando | Para qué |
|---|---|
| `/mythicloot generar <nivel> [rareza] [tipo]` | Te da un objeto sin matar nada. `rareza`: `magica`, `rara`, `epica`, `legendaria`; `tipo`: `cuerpo_a_cuerpo`, `a_distancia`, `armadura` |
| `/mythicloot recargar` | Vuelve a leer `run/config/mythicloot.json` |
| `/bloodmoon invocar zombie 60` (con Bloodmoon) | Un mob con nivel para matarlo |
| `/bloodmoon evento jefe` (con Bloodmoon) | Un jefe de zona, que suelta siempre |

Para ver soltar sin esperar la chance, poné `"chanceBase": 1` y `"chanceMaxima": 1` en la config y `/mythicloot recargar`.

## Qué mirar

- El tooltip: nombre del color de la rareza con su sufijo, "Rareza: …", "Nivel …", las líneas azules de los afijos que no son atributos y, abajo, los atributos de vanilla con los del afijo sumados.
- Ponerse dos piezas "del Gigante": los corazones tienen que sumar las dos.
- Una espada "del Vampiro": pegarle a un mob con poca vida tiene que curar; "de las Llamas", prenderlo.
- Un arco "del Halcón": comparar cuántas flechas pide un zombi contra un arco común.
- Matar un mob con lava o con `/kill`: no suelta nada. Con Bloodmoon, uno de un spawner o de un huevo tampoco.
- Una épica o legendaria cae brillando, con sonido y "¡Botín épico!" en la barra de acción.
- Renombrar un objeto en el yunque: los afijos siguen andando.
- **Con un cliente vanilla sin el mod:** todo lo anterior tiene que verse igual.

## Ver también

- [Entorno Java y Gradle](entorno-java-gradle.md)
- [Botín: generar y soltar](../mod/botin.md)
