# Mythic Loot — mod de Minecraft

**El nombre visible es "Mythic Loot", pero el id es y sigue siendo `botinmitico`** (paquete, comando, config, la clave del `custom_data` y los ids de los modificadores). No lo renombres: va guardado en cada objeto que ya se soltó, y un id nuevo los dejaría sin afijos.

Mod de Fabric para **Minecraft Java 26.3**: los mobs hostiles que mata un jugador pueden soltar armas, arcos y armaduras con **rareza** (mágica, rara, épica, legendaria) y de 1 a 4 **afijos** al azar (más daño, robo de vida, flechas certeras, más vida…), mejores cuanto más nivel tiene el mob. Con **Bloodmoon & Bosses** el nivel es el del mob y los jefes de zona sueltan siempre uno; sin él, sale de la distancia al spawn. Java 25, Gradle 9.7 + Fabric Loom 1.18, Fabric Loader 0.19.5, Fabric API 0.162.0+26.3. Toda la lógica corre en el servidor: los objetos usan componentes de vanilla y un cliente sin el mod los ve completos.

> **Estado:** versión 0.1.0, con los gametests en verde. Falta probarlo a mano en un servidor con Bloodmoon & Bosses (ver [docs/plan.md](docs/plan.md)).

## Cómo obtener contexto

1. Ubicá el **área** en la tabla de abajo.
2. Abrí su `INDICE.md` y leé las descripciones: muchas veces la respuesta ya está ahí.
3. Abrí **sólo** la nota que corresponde. No leas un área entera.

Si no sabés en qué nota está algo, buscá por contenido antes de abrir archivos:

```bash
grep -ril "<palabra clave>" claude/
```

El mismo tema en distintas áreas usa el mismo nombre de archivo (`claude/mod/botin.md` ↔ `claude/tests/botin.md`), así que el grep devuelve el clúster completo.

## Áreas

| Área | Carpeta | Stack | Índice |
|---|---|---|---|
| Mod | `src/main/` | Java 25 · Fabric API (eventos de entidades y de daño, attachments, comandos) · un accessor mixin · nombres oficiales de Mojang | [claude/mod/INDICE.md](claude/mod/INDICE.md) |
| Tests | `src/gametest/` | Fabric GameTest API · servidor sin ventana | [claude/tests/INDICE.md](claude/tests/INDICE.md) |

## Notas transversales

| Nota | De qué trata |
|---|---|
| [Entorno Java y Gradle](claude/general/entorno-java-gradle.md) | `~/.gradle/gradle.properties` fuerza JDK 17 (React Native) y gana sobre el proyecto · Java 25 se fija con `gradle/gradle-daemon-jvm.properties` + `org.gradle.java.installations.paths` en el `gradle.properties` **global** · sin eso Loom falla con "requires JVM runtime version 25" |
| [Minecraft 26.x sin ofuscar](claude/general/minecraft-26-sin-ofuscar.md) | Desde 26.1 se usan los nombres de Mojang (Yarn está muerto) · verificá firmas con `javap` sobre el jar de `.gradle/loom-cache` · las constantes de entidades están en `EntityTypes` |
| [Probar en el juego](claude/general/probar-en-el-juego.md) | `runServer` necesita `eula=true` y `online-mode=false` en `run/` · `/botinmitico generar <nivel> [rareza] [tipo]` da un objeto sin matar nada · probá con un cliente vanilla |

## Features que cruzan áreas

| Feature | Mod | Tests |
|---|---|---|
| Generar objetos (rareza, material por nivel, afijos, nombre y lore) y cuándo se sueltan (chance, jefes, campeones, Bloodmoon o distancia) | [botin](claude/mod/botin.md) | [botin](claude/tests/botin.md) |
| Afijos: atributos de vanilla y los que aplica el mod (vampírico, ardiente, certero) | [afijos](claude/mod/afijos.md) | [botin](claude/tests/botin.md) |

## Reglas de workflow

- **Después de cada feature corré `/docs`** para actualizar las notas según el diff, y `node scripts/verificar-docs.mjs` tiene que dar OK.
- **Si la feature se nota en el juego, actualizá el `README.md`**: es lo que lee quien instala el mod, y `/docs` sólo toca `claude/`.
- **Antes de dar algo por terminado corré `gradlew runGameTest`**: compilar no alcanza, los eventos y el mixin recién se aplican en runtime.
- **Con cada cambio que se pueda notar en el juego, subí `version` en `gradle.properties`**: minor para una feature nueva, patch para un arreglo.
- **No commitees ni pushees salvo que te lo pidan.**
- Mensajes, comentarios y docs en **español rioplatense** (vos), como el resto del proyecto.

## Convenciones de código

- Paquete `com.botinmitico`; mixins en `com.botinmitico.mixin`, registrados en `src/main/resources/botinmitico.mixins.json`.
- **Métodos y accessors agregados por mixins llevan prefijo `botinmitico$`** para no chocar con otros mods.
- **Ningún número de balance escrito a mano en el código:** van en `Configuracion`.
- **Los ids de `AfijoDeBotin` y `Rareza` no se renombran nunca:** van en el `custom_data` de los objetos. El nombre visible (`sufijo`, `nombre`) sí se puede cambiar.
- **No se depende de las clases de Bloodmoon & Bosses:** se leen sus attachments por id (ver [botin](claude/mod/botin.md)).
- **Textos al jugador en castellano armados en el servidor**, con `Component.literal`; el nombre base del objeto es el traducible de vanilla.
- Indentación con tabs, imports agrupados: java/terceros → `net.fabricmc` → `net.minecraft`.
- Comentarios sólo para el porqué, no para parafrasear.

## Qué NO va en el grafo

Planes, investigaciones, auditorías y borradores van en `docs/` y se leen a demanda; `claude/` es sólo contexto permanente. El plan del mod está en [docs/plan.md](docs/plan.md).
