# Tests de botín

[← Índice Tests](INDICE.md)

Síntomas que esta nota evita: un test de Bloodmoon deja el modo prendido y otro test en paralelo ve mobs sin nivel; un test de chance pasa a veces; el test de la armadura falla porque el jugador "no se puso" el casco.

Todo está en `MythicLootGameTest`.

## Qué cubre

| Test | Qué verifica |
|---|---|
| `generarCadaTipoYRareza` | Para los 6 tipos × 4 rarezas en nivel 60: los datos en `custom_data`, un afijo por cada uno de la rareza, todos sirven para el tipo, cada atributo suma su valor en `attribute_modifiers` (contra el ítem de vanilla), el arma y el tridente conservan el daño de vanilla, el nombre termina en un sufijo y el lore arranca con "Rareza: …" |
| `valoresYMaterialesSegunElNivel` | La fórmula de `valor` en los bordes (máximo, legendaria × 1,25, mínimo, nivel de más) · cobre en el nivel 5 · en el 90, épica de diamante y legendaria de netherita con brillo · en el 10 sólo mágica · un jefe nunca da menos que la mínima, aunque su nivel no la dé |
| `lanzasMazasTridentesEscudosYHerramientas` | En 200 tiradas de nivel 60: una mágica nunca es maza, salen lanzas de diamante, y una épica a veces es maza · herramienta de cobre en el nivel 5 · tridente · los modificadores del escudo van todos en `OFFHAND` |
| `elEscudoValeEnLaManoSecundaria` | Un escudo "del Gigante" de 4 da 4 de vida a un zombi en la mano secundaria y nada en la principal, dos ticks después |
| `tridenteLanzado` | Un `ThrownTrident` de verdad que vuela hacia un gólem (con resistencia al fuego, para medir sólo el golpe): certero 0,5 pega 12, el vampírico cura 1,2 y el gólem queda prendido. Con `succeedWhen`, porque el golpe llega en un tick posterior |
| `elAfijoDeJefeVaAparte` | Una épica de armadura con vacío tiene 4 afijos, el nombre del vacío y una línea roja · en el yunque, una legendaria de 4 gana el de jefe y sigue legendaria · con dos de jefe distintos queda el de la izquierda, y la rareza cuenta sólo los comunes |
| `sorteoDelAfijoDeJefe` | Con Bloodmoon y `conAfijosDeJefe(1)`: un zombi que no es jefe no trae; jefe zombi → festín, jefa bruja → brebaje; con chance 0, ninguno |
| `festinYMarchitoAlPegarYAlMatar` | Casco del Festín 0,1: matar un zombi cura 2 · espada del Marchitamiento: el gólem queda con Wither II · un casco en la mochila o en la mano no cuenta |
| `verdugoIgnoraLaArmadura` | Por el mixin de verdad: dos zombis con pechera de diamante; el golpe sin verdugo entra reducido, con verdugo 1 entra entero (10) |
| `brutoLlamaradaBrebajeYVacio` | Llamando a cada efecto directo (ver abajo): bruto 0,5 y llamarada 0,5 bajan a la mitad el golpe y el fuego, no la caída · brebaje 1 cura 4 y la llamarada prende al zombi; el segundo golpe no cura (espera) · el vacío no esquiva una caída, sí el golpe de un mob (y el jugador se movió), y el siguiente no (espera) |
| `lluviaDeFlechas` | Arco de la Lluvia 1: al agregar una flecha aparecen dos, y la copia es `CREATIVE_ONLY` |
| `elAfijoSeSumaAlAtributoDelObjeto` | Un peto de hierro con reforzado 0,5 tiene un solo modificador de armadura, con la del peto más 0,5 |
| `dosPiezasDeArmaduraSuman` | Casco y botas "del Gigante" de 4 suman 8 de vida en un zombi, dos ticks después |
| `vampiricoYArdienteAlPegar` | Una espada con vampírico 0,1 y ardiente cura 0,8 con un golpe de 8 y prende al gólem · una espada común no |
| `certeroYArdienteEnLasFlechas` | Una flecha de un arco con certero 0,5 aparece con 1,5 × el daño base y prendida · llamar de nuevo a `alDisparar` no lo aplica dos veces · con un arco común, el daño de vanilla |
| `sorteoConYSinBloodmoon` | Con Bloodmoon: el nivel del attachment, sin attachment no suelta ni con chance 1, chance 0 no suelta, el afijo lo hace campeón, el jefe suelta épica o mejor aunque la chance sea 0 · sin Bloodmoon: el nivel de la distancia, y el attachment de jefe no cuenta |
| `alMorirSueltaSiLoMatoUnJugador` | Con chance 1: un zombi muerto por un jugador suelta un objeto con afijos; muerto con `genericKill`, no; un gólem (no es `Enemy`) muerto por un jugador, no |
| `juntarEnElYunque` | Con un `AnvilMenu` de verdad: dos espadas sanas, "del Filo" y "de la Furia", dan una rara con los dos afijos, sin el filo sumado dos veces, con el nombre de la izquierda y costo `costoPorAfijoUnido` · gastada, 2 de reparar más el afijo · un afijo repetido peor no da resultado; uno mejor se queda el valor alto sin subir la rareza · un casco legendario de 4 no gana un quinto, pero sí mejora uno, con la armadura del casco bien sumada · espada + hacha no se juntan |
| `archivoDeConfiguracion` | Sin archivo, `POR_DEFECTO` y el archivo con rarezas y afijos · un archivo parcial se completa también dentro de los mapas y de una rareza · `porMob` acepta especies nuevas, `""` saca una y el resto queda por defecto · un afijo común en `porMob`, `afijosDeJefe.chance` 2, `chanceMaxima` 2, una `rarezaDeJefe` o `rarezaMinimaDeMaza` que no existe y un valor máximo 0 se rechazan sin pisar el archivo |

## Cosas que no son obvias

- **Bloodmoon no está en el classpath de los tests.** `AttachmentsDeBloodmoon` (entrypoint `main` del mod de test) registra `bloodmoonandbosses:nivel`, `jefe` y `afijo` con los codecs de Bloodmoon, y `NivelDelMob` los encuentra por id igual que encontraría los de verdad.
- **El modo Bloodmoon y la config son globales y los tests corren en paralelo:** se cambian y se restauran (en `finally`) **dentro del mismo método**, que corre entero en un tick. Si quedaran puestos entre ticks, otro test los vería.
- **`Caida.sortear` recibe la config:** los casos de chance 0 y 1 se arman con `conChance` y no tocan la global. Sólo `alMorir` lee la global, y por eso su test usa `MythicLoot.usar`.
- **Los afijos exactos se arman con `GeneradorDeBotin.armar`** y un `ObjetoConAfijos` hecho a mano: con `generar` saldrían al azar.
- **Los jugadores de prueba no tickean** (su conexión es un `EmbeddedChannel`), y vanilla aplica los atributos del equipo en el tick: la armadura se prueba con un zombi, que sí tickea, y `runAfterDelay(2)`.
- **Matá con `playerAttack(jugador)`** para que `getLastHurtByPlayer()` lo tenga. El jugador sale de `entrar()` (supervivencia), no de `makeMockServerPlayerInLevel`, que fuerza creativo.
- **El yunque se prueba con `new AnvilMenu(0, inventario, ContainerLevelAccess.NULL)`:** cada `getSlot(i).set(...)` dispara `createResult`, como cuando el jugador pone los objetos. Copiá los stacks antes de ponerlos, que el menú se queda con ellos.
- **Los jugadores de prueba son invulnerables:** `ServerPlayer.isInvulnerableTo` da true mientras `connection.hasClientLoaded()` sea false, y con un `EmbeddedChannel` nunca carga. Los efectos que se disparan al recibir daño (bruto, llamarada, brebaje, vacío) se prueban llamando a `EfectosDeJefe` directo; el mixin se prueba con verdugo, que actúa sobre el zombi.
- **Los jugadores de prueba entran en (0, 0, 0)**, fuera de la estructura: para el salto del vacío hay que llevarlos adentro (`teleportTo`), o `randomTeleport` no encuentra suelo.
- **Contá y borrá los ítems:** todos los mobs mueren en el mismo lugar, y el zombi suelta también su carne podrida. `contar` sólo cuenta los que tienen `ObjetoConAfijos`.

## Ver también

- [Botín: generar y soltar](../mod/botin.md)
- [Afijos](../mod/afijos.md)
- [Juntar en el yunque](../mod/yunque.md)
