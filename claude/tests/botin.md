# Tests de botín

[← Índice Tests](INDICE.md)

Síntomas que esta nota evita: un test de Bloodmoon deja el modo prendido y otro test en paralelo ve mobs sin nivel; un test de chance pasa a veces; el test de la armadura falla porque el jugador "no se puso" el casco.

Todo está en `MythicLootGameTest`.

## Qué cubre

| Test | Qué verifica |
|---|---|
| `generarCadaTipoYRareza` | Para los 3 tipos × 4 rarezas en nivel 60: los datos en `custom_data`, un afijo por cada uno de la rareza, todos sirven para el tipo, cada atributo suma su valor en `attribute_modifiers` (contra el ítem de vanilla), el arma conserva el daño de vanilla, el nombre termina en un sufijo y el lore arranca con "Rareza: …" |
| `valoresYMaterialesSegunElNivel` | La fórmula de `valor` en los bordes (máximo, legendaria × 1,25, mínimo, nivel de más) · cobre en el nivel 5 · en el 90, épica de diamante y legendaria de netherita con brillo · en el 10 sólo mágica · un jefe nunca da menos que la mínima, aunque su nivel no la dé |
| `elAfijoSeSumaAlAtributoDelObjeto` | Un peto de hierro con reforzado 0,5 tiene un solo modificador de armadura, con la del peto más 0,5 |
| `dosPiezasDeArmaduraSuman` | Casco y botas "del Gigante" de 4 suman 8 de vida en un zombi, dos ticks después |
| `vampiricoYArdienteAlPegar` | Una espada con vampírico 0,1 y ardiente cura 0,8 con un golpe de 8 y prende al gólem · una espada común no |
| `certeroYArdienteEnLasFlechas` | Una flecha de un arco con certero 0,5 aparece con 1,5 × el daño base y prendida · llamar de nuevo a `alDisparar` no lo aplica dos veces · con un arco común, el daño de vanilla |
| `sorteoConYSinBloodmoon` | Con Bloodmoon: el nivel del attachment, sin attachment no suelta ni con chance 1, chance 0 no suelta, el afijo lo hace campeón, el jefe suelta épica o mejor aunque la chance sea 0 · sin Bloodmoon: el nivel de la distancia, y el attachment de jefe no cuenta |
| `alMorirSueltaSiLoMatoUnJugador` | Con chance 1: un zombi muerto por un jugador suelta un objeto con afijos; muerto con `genericKill`, no; un gólem (no es `Enemy`) muerto por un jugador, no |
| `archivoDeConfiguracion` | Sin archivo, `POR_DEFECTO` y el archivo con rarezas y afijos · un archivo parcial se completa también dentro de los mapas y de una rareza · `chanceMaxima` 2, una `rarezaDeJefe` que no existe y un valor máximo 0 se rechazan sin pisar el archivo |

## Cosas que no son obvias

- **Bloodmoon no está en el classpath de los tests.** `AttachmentsDeBloodmoon` (entrypoint `main` del mod de test) registra `bloodmoonandbosses:nivel`, `jefe` y `afijo` con los codecs de Bloodmoon, y `NivelDelMob` los encuentra por id igual que encontraría los de verdad.
- **El modo Bloodmoon y la config son globales y los tests corren en paralelo:** se cambian y se restauran (en `finally`) **dentro del mismo método**, que corre entero en un tick. Si quedaran puestos entre ticks, otro test los vería.
- **`Caida.sortear` recibe la config:** los casos de chance 0 y 1 se arman con `conChance` y no tocan la global. Sólo `alMorir` lee la global, y por eso su test usa `MythicLoot.usar`.
- **Los afijos exactos se arman con `GeneradorDeBotin.armar`** y un `ObjetoConAfijos` hecho a mano: con `generar` saldrían al azar.
- **Los jugadores de prueba no tickean** (su conexión es un `EmbeddedChannel`), y vanilla aplica los atributos del equipo en el tick: la armadura se prueba con un zombi, que sí tickea, y `runAfterDelay(2)`.
- **Matá con `playerAttack(jugador)`** para que `getLastHurtByPlayer()` lo tenga. El jugador sale de `entrar()` (supervivencia), no de `makeMockServerPlayerInLevel`, que fuerza creativo.
- **Contá y borrá los ítems:** todos los mobs mueren en el mismo lugar, y el zombi suelta también su carne podrida. `contar` sólo cuenta los que tienen `ObjetoConAfijos`.

## Ver también

- [Botín: generar y soltar](../mod/botin.md)
- [Afijos](../mod/afijos.md)
