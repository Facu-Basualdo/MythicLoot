# Mythic Loot

<p align="center"><img src="src/main/resources/assets/botinmitico/icon.png" alt="Mythic Loot" width="256"></p>

Mod de Fabric para **Minecraft Java 26.3**: los mobs hostiles que matás pueden soltar **armas, arcos y armaduras con rareza y afijos** al azar. Más daño, robo de vida, flechas certeras, más vida, más armadura… Cuanto más nivel tiene el mob, mejor es lo que suelta.

Pensado para jugarse con **Bloodmoon & Bosses** (los mobs con niveles) y **Hero's Forge** (las estadísticas que se compran con experiencia): Bloodmoon hace más fuertes a los mobs, Hero's Forge te hace más fuerte a vos, y Mythic Loot hace que el equipo siga mejorando más allá de la netherita con Filo V.

> **Estado:** versión 0.1.0. Todo lo de abajo está hecho y lo cubren las pruebas automáticas. Falta probarlo a mano en un servidor junto con Bloodmoon & Bosses.

## Cómo funciona en el juego

### Cuándo se suelta

- **Sólo si lo mataste vos** (o lo remató la caída o el fuego después de tu golpe, como el loot de vanilla), y con `/gamerule mobDrops` prendida.
- **La chance crece con el nivel del mob:** 2 % más 0,08 % por nivel, hasta 10 %. Un mob de nivel 50 suelta algo una de cada 16 veces; uno de nivel 100, una de cada 10.
- **Con Bloodmoon & Bosses:**
  - el nivel es el del mob ("Zombi [Nv 57]");
  - **los campeones tienen el doble de chance**;
  - **los jefes de zona sueltan siempre un objeto épico o mejor**;
  - los mobs sin nivel (de spawners, huevos o comandos) no sueltan nada: las granjas no cambian.
- **Sin Bloodmoon:** sueltan los mobs hostiles, con un nivel por la distancia al spawn del mundo (1 cada 100 bloques, ×8 en el Nether, +20 en el Nether y +40 en el End).
- Las épicas y legendarias caen **brillando** (se ven a través de las paredes), con un sonido y un "¡Botín épico!" en pantalla.

### Rarezas

| Rareza | Color | Afijos | Desde el nivel | Peso |
|---|---|---|---|---|
| Mágica | azul | 1 | 1 | 60 |
| Rara | amarillo | 2 | 20 | 30 |
| Épica | violeta | 3 (+10 %) | 50 | 12 |
| Legendaria | dorado | 4 (+25 %) y brilla | 80 | 4 |

Entre las que ya pueden salir, se sortea por peso: en el nivel 100 sale una legendaria cada ~26 objetos.

### Qué objeto

- **Cuerpo a cuerpo** (espada o hacha), **a distancia** (arco o ballesta) o **armadura** (casco, peto, pantalones o botas).
- **El material depende del nivel:** cobre hasta el 19, hierro (cadena en armadura) hasta el 39, hierro hasta el 59, diamante desde el 60. **Netherita sólo las legendarias de nivel 80 o más.**
- No salen encantados: se encantan en la mesa como cualquier otro. Se pueden renombrar y reparar en el yunque sin perder los afijos.

### Afijos

El nombre lleva el afijo principal: **"Espada de diamante del Vampiro"**. El tooltip muestra la rareza, el nivel y cada afijo: los que son atributos los muestra Minecraft solo ("Cuando está en la mano principal: +4,5 de daño de ataque").

| Afijo | Nombre | En | Qué hace | Al máximo (nivel 100, antes de la rareza) |
|---|---|---|---|---|
| Afilado | del Filo | espadas y hachas | Daño | +6 |
| Furioso | de la Furia | espadas y hachas | Velocidad de ataque | +20 % |
| Largo | del Alcance | espadas y hachas | Alcance a mobs | +1 bloque |
| Vampírico | del Vampiro | armas y arcos | Te cura una parte del daño que hacés (también con flechas) | 8 % |
| Ardiente | de las Llamas | armas y arcos | Prende fuego al que golpeás, o las flechas | 5 segundos |
| Certero | del Halcón | arcos y ballestas | Daño de las flechas | +40 % |
| Ligero | del Viento | arcos y armaduras | Velocidad | +6 % |
| Robusto | del Gigante | armaduras | Vida | +2 corazones por pieza |
| Reforzado | de la Fortaleza | armaduras | Armadura | +2 |
| Tenaz | del Coloso | armaduras | Dureza de armadura | +2 |
| Firme | de la Montaña | armaduras | Resistencia al empuje | +10 % |
| Afortunado | de la Suerte | armaduras | Suerte | +1 |

- **El valor depende del nivel:** en el nivel 1 es un cuarto del máximo y llega entero en el 100. Cada objeto saca entre el 75 % y el 100 % de eso, y las épicas y legendarias rinden un 10 % y un 25 % más.
- **Las piezas de armadura se suman:** cuatro piezas "del Gigante" dan su vida juntas.
- El robo de vida no cura con el golpe que mata (como el de Hero's Forge).
- Certero y el daño a distancia de Hero's Forge se suman.

## Comandos (op)

| Comando | Qué hace |
|---|---|
| `/botinmitico generar <nivel> [rareza] [tipo]` | Te da un objeto con afijos de ese nivel. `rareza`: `magica`, `rara`, `epica`, `legendaria` (si no va, al azar); `tipo`: `cuerpo_a_cuerpo`, `a_distancia`, `armadura` |
| `/botinmitico recargar` | Vuelve a leer `config/botinmitico.json` sin reiniciar el servidor |

## Configuración

En el primer arranque se crea **`config/botinmitico.json`** con los valores por defecto. Editalo y usá `/botinmitico recargar`, o reiniciá el servidor.

- Si el archivo tiene un error (JSON mal escrito o un valor imposible), se queda la configuración que había, el archivo no se toca y el error aparece en el chat y en la consola.
- Si falta una clave se usa su valor por defecto, y al leer el archivo se reescribe completo. Una clave que no existe se ignora con un aviso en la consola.
- Los objetos que ya se soltaron no cambian: sus valores quedan guardados en cada uno.

| Clave | Por defecto | Qué hace |
|---|---|---|
| `nivelMaximo` | 100 | El nivel en que los afijos llegan a su máximo |
| `bloquesPorNivel`, `extraNether`, `extraEnd` | 100, 20, 40 | Sin Bloodmoon: cómo se calcula el nivel por la distancia |
| `chanceBase`, `chancePorNivel`, `chanceMaxima` | 0.02, 0.0008, 0.10 | La chance de soltar algo, de 0 a 1 |
| `multiplicadorCampeon` | 2.0 | Por cuánto se multiplica la chance de un campeón de Bloodmoon |
| `botinDeJefe`, `rarezaDeJefe` | `true`, `"epica"` | Si los jefes de zona sueltan siempre, y la rareza mínima |
| `rarezas` | ver arriba | Por rareza (`magica`, `rara`, `epica`, `legendaria`): `nivelMinimo` y `peso` |
| `pesosDeTipo` | 40, 20, 40 | Cuánto sale cada tipo: `cuerpo_a_cuerpo`, `a_distancia`, `armadura` |
| `nivelesPorMaterial` | 20 | Cada cuántos niveles mejora el material |
| `netheritaSoloLegendaria` | `true` | Si la netherita sólo sale en legendarias |
| `valoresMaximos` | ver la tabla de afijos | Lo que vale cada afijo en el nivel máximo: los porcentajes como fracción (`0.08` = 8 %), el fuego en segundos |

## Requisitos

| | Versión |
|---|---|
| Minecraft Java | 26.3 |
| [Fabric Loader](https://fabricmc.net/use/) | 0.19.5 o más nuevo |
| [Fabric API](https://modrinth.com/mod/fabric-api) | para 26.3 |
| Java | 25 |
| Bloodmoon & Bosses | opcional, recomendado |

## Instalación

Toda la lógica corre en el servidor.

- **Servidor dedicado:** copiá `botinmitico-<versión>.jar` y Fabric API a la carpeta `mods/` del servidor. Los jugadores no necesitan instalar nada.
- **Singleplayer o LAN:** instalalo en el cliente del que abre el mundo, junto con Fabric API.
- Anda junto con Bloodmoon & Bosses, Hero's Forge y Soul Link.

## Compilar

```bash
./gradlew build          # Windows: gradlew build
```

El jar queda en `build/libs/`. El proyecto pide Java 25 (`gradle/gradle-daemon-jvm.properties`): si Gradle no encuentra tu JDK 25, agregá a tu `~/.gradle/gradle.properties` (el global, no el del repo) la línea `org.gradle.java.installations.paths=<ruta a tu JDK 25>`.

## Probar

```bash
./gradlew runGameTest    # pruebas automáticas en un servidor sin ventana
```

Las pruebas verifican:

- que cada tipo y rareza salga con sus datos, la cantidad justa de afijos, sólo los que le sirven, los atributos en el objeto (sin perder el daño de vanilla del arma), el nombre con el sufijo y la rareza en el tooltip;
- la cuenta de los valores por nivel y rareza, el material por nivel (netherita sólo legendaria) y que un jefe nunca suelte menos de la rareza mínima;
- que dos piezas de armadura con el mismo afijo sumen;
- el vampírico y el ardiente al pegar, y el certero y el ardiente en las flechas (sin aplicarse dos veces);
- el sorteo con Bloodmoon (nivel del mob, sin nivel no suelta, campeones, jefes) y sin él (nivel por distancia);
- que sólo suelte si lo mató un jugador y si es hostil;
- la lectura de la config (valores por defecto, archivos parciales, valores inválidos).

Para probar a mano, mirá [claude/general/probar-en-el-juego.md](claude/general/probar-en-el-juego.md).

## Estructura

```
src/main/       el mod: genera los objetos, los suelta al morir el mob y aplica los afijos que no son atributos
src/gametest/   pruebas automáticas (Fabric GameTest)
claude/         documentación técnica para agentes de IA (invariantes, decisiones y bugs ya resueltos)
docs/           el plan del mod
```

Para entender por qué el código está hecho así, empezá por [CLAUDE.md](CLAUDE.md) y [claude/mod/INDICE.md](claude/mod/INDICE.md).

## Licencia

[MIT](LICENSE): podés usarlo, modificarlo y redistribuirlo (también en modpacks) siempre que mantengas el aviso de copyright.
