# Juntar en el yunque

[← Índice Mod](INDICE.md)

Síntomas que esta nota evita: juntar "Espada de cobre del Filo" con "Espada de cobre de la Furia" da una espada sólo del Filo; dos espadas sanas y sin encantar no dan resultado; el daño del afijo queda sumado dos veces; el mixin explota al cargar `AnvilMenu`.

## Qué hace (`Yunque`)

Vanilla copia el objeto de la izquierda y le suma la durabilidad y los encantamientos del de la derecha: los componentes de la derecha (`custom_data`, `attribute_modifiers`, `lore`) se pierden. `Yunque.unir(resultado, derecha)` le pone al resultado los afijos de los dos si la derecha es un objeto del mod **del mismo ítem** (un libro o un material de reparación no cuentan).

`juntar(izquierda, derecha)`, decidido con el usuario el 2026-10-12:

| Regla | Por qué |
|---|---|
| Los afijos de los dos; uno repetido se queda con el valor **más alto** (no se suman) | Sumar dejaría farmear un afijo sin techo juntando copias |
| Tope: los de la legendaria (4). Si se pasa, quedan los de la izquierda y los primeros de la derecha (orden del enum) | — |
| La rareza sube a `Rareza.conAfijos(cantidad)` y nunca baja de la de ninguno de los dos | "Rareza" es cuántos afijos trae: una mágica con dos sería un objeto que no puede salir |
| El nivel es el más alto | — |
| Los valores **no** se recalculan con la bonificación de la rareza nueva | Ya vienen con la de su rareza original; recalcular pediría el azar que no se guarda |
| El nombre es el del de la izquierda, pintado del color de la rareza nueva | El sufijo del principal no se guarda aparte. Si se cambiara el texto, vanilla lo vería distinto del `itemName` que manda el cliente y le pondría un `custom_name` |

Devuelve cuántos afijos **ganó o mejoró** el de la izquierda. Con 0 no toca nada: queda lo de vanilla (y si tampoco hay reparación ni encantamientos, no hay resultado).

`GeneradorDeBotin.rearmar` **vuelve los `attribute_modifiers` a los del prototipo** (`stack.getPrototype()`) antes de sumar los afijos: si sumara sobre los del resultado, los de la izquierda quedarían dos veces.

## El mixin (`YunqueMixin`)

`@Inject` en `AnvilMenu.createResult`, en la **primera lectura de `itemName`** (`GETFIELD`, ordinal 0): ahí ya terminó lo de durabilidad y encantamientos (y los `return` de "no se puede juntar", como espada + hacha) y todavía no empezó el renombre.

- **El costo es la variable local que después decide si hay resultado** (`if (costo <= 0) resultado = EMPTY`). Se le suma `ganados × costoPorAfijoUnido` con un `LocalIntRef` de MixinExtras: por eso dos objetos sanos y sin encantar dan resultado. Vanilla después le suma el `repair_cost` acumulado de los dos y aplica el "¡Demasiado caro!" de 40 como siempre.
- **El jar de 26.3 no trae tabla de variables locales:** los `@Local` van por `ordinal` y tipo. `ItemStack` 0 = izquierda, 1 = resultado (la copia), 2 = derecha; `int` 0 = costo. Si una versión nueva cambia el método, el síntoma es que `juntarEnElYunque` falla o que el mixin no aplica al cargar (`defaultRequire: 1`). Verificá con `javap -c` (ver [minecraft-26-sin-ofuscar](../general/minecraft-26-sin-ofuscar.md)).
- Todo es del servidor: el cliente sin el mod ve el resultado y el costo que le sincroniza el menú.

## Afijo de jefe

**No cuenta para el tope de 4 ni para la rareza** (`Yunque.comunes`): una legendaria de 4 todavía puede ganar uno. **Hay uno solo por objeto:** si los dos traen uno distinto, queda el de la izquierda; si traen el mismo, el valor más alto. Si no, juntando objetos se apilarían los efectos de varios jefes. Ganarlo cuenta como un afijo ganado (cobra `costoPorAfijoUnido`).

## Ver también

- [Botín: generar y soltar](botin.md)
- [Tests: botín](../tests/botin.md)
