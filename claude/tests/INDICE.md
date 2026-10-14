# Índice — Tests (`src/gametest`)

Fabric GameTest API · `gradlew runGameTest` levanta un servidor sin ventana, corre los tests y sale con error si alguno falla.

[← Hub](../../CLAUDE.md)

| Nota | De qué trata |
|---|---|
| [Botín](botin.md) | Bloodmoon no está en los tests: el mod de test registra sus attachments con los mismos ids (`AttachmentsDeBloodmoon`) y el modo se fuerza con `usarBloodmoon` dentro del método, restaurando en `finally` · la config global se cambia con `MythicLoot.usar` en el mismo método · `sortear` recibe la config: los casos de chance no tocan la global · los afijos exactos se arman con `GeneradorDeBotin.armar` · la vida de la armadura se prueba con un zombi (los jugadores de prueba no tickean, y los atributos de equipo se aplican en el tick) · el log cuenta un test más que los nuestros (es de Fabric) |
| [Pantalla de configuración](pantalla-de-configuracion.md) | `ConfiguracionClienteTest` en `runClientGameTest`, con `setScreen` (Mod Menu no está en las pruebas) · el cliente de prueba arranca en inglés: el test elige `es_ar` · los campos están dentro del contenedor de `ScrollableLayout` · `CycleButton.onPress` con un `KeyEvent`, nunca null · la config del cliente de prueba se borra antes de cada corrida |
