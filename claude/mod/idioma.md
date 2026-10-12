# Idioma de los textos

[← Índice Mod](INDICE.md)

**Hay dos clases de texto y se traducen distinto.** Si un objeto le aparece a un jugador con `mythicloot.lore.rareza` en vez de "Rareza: épica", el respaldo quedó vacío. Si un mensaje del chat le llega en otro idioma, se armó con el idioma de la config en vez del del jugador.

| Texto | Cómo | Por qué |
|---|---|---|
| Chat: el aviso de botín, los mensajes de `/mythicloot` | `Idioma.tr`: lo traduce el servidor en el idioma del jugador | Cada mensaje va a una persona; el servidor sabe su idioma (`clientInformation().language()`) |
| Nombre y lore de un objeto | `Idioma.guardado`: `Component.translatableWithFallback` con el texto en el idioma de la config como respaldo | Es el mismo objeto para todos, queda guardado y cambia de manos: no se puede traducir por jugador sin reescribir paquetes |

**Un cliente vanilla ve el respaldo; uno con el mod lo ve en su idioma.** Vanilla busca la clave en sus archivos de idioma y, si no la tiene, muestra `fallback`. El respaldo se arma una sola vez, al generar el objeto, con la opción `idioma` de la config (`en_us` por defecto). Cambiar `idioma` no cambia los objetos que ya existen.

**El nombre base del objeto ("Espada de hierro") es de vanilla y lo traduce siempre el cliente.** Con la config en inglés y un cliente vanilla en español se ve "Espada de hierro of the Vampire": el sufijo es el respaldo y el resto, del cliente. Para un servidor de habla hispana, poné `"idioma": "es_ar"`.

Los objetos generados antes de la 0.4.0 tienen el nombre y el lore como texto fijo en español: no se migran.

## Archivos

`assets/mythicloot/lang/en_us.json` y `es_ar.json`. Los lee `Idioma` con `getResourceAsStream` (para el chat y los respaldos), y los lee también el cliente que tiene el mod instalado (para los objetos). **Las plantillas usan el formato de vanilla**: `%s`, `%1$s` y `%%` para un porcentaje. `Idioma.tr` entiende lo mismo: las del lore las arma el cliente con su propio formateador, y un `%` suelto le rompería la línea.

Los números van como texto ya formateado (`Texto.numero`, en el idioma de la config): el cliente no sabe poner coma decimal. "Prende fuego 1 segundo" y "… 2,5 segundos" son dos claves (`.uno`, `.varios`).

| Clave | Qué es |
|---|---|
| `mythicloot.afijo.<id>.sufijo` | El sufijo del nombre ("del Vampiro", "of the Vampire"): va al final en los dos idiomas |
| `mythicloot.afijo.<id>.descripcion` | La línea del lore de los afijos que no son atributos |
| `mythicloot.rareza.<id>` | En el lore ("Rareza: épica"): en español, femenino |
| `mythicloot.aviso.<id>` | El aviso al soltarse ("¡Botín épico!"): en español, masculino. Por eso son dos claves y no una |

**Los ids no se traducen**: los de `Rareza`, `AfijoDeBotin` y `TipoDeObjeto` van en el `custom_data` de cada objeto, en la config y en los argumentos de `/mythicloot generar`.

`textosEnLosDosIdiomas` verifica que los dos archivos tengan las mismas claves, que cada afijo tenga sufijo y cada rareza nombre y aviso, y el `%%`. `generarCadaTipoYRareza` verifica que el lore quede guardado como traducible con respaldo.

Los errores de la config (`Configuracion.validar`) siguen en español.

## Ver también

- [Botín: generar y soltar](botin.md): dónde se arman el nombre y el lore.
- [Afijos](afijos.md)
