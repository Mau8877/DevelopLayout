# Idiomas — regla transversal

Aplica a **todo texto que ve una persona**: web, mobile, y las respuestas y
correos del backend. El detalle de cada capa está en
[IDIOMAS_FRONTEND.md](frontend/IDIOMAS_FRONTEND.md),
[IDIOMAS_MOBILE.md](mobile/IDIOMAS_MOBILE.md) e
[IDIOMAS_BACKEND.md](backend/IDIOMAS_BACKEND.md).

> **Estado:** obligatoria. La migración del código escrito antes de esta regla
> es [HU-INICIAL-Idiomas](../../docs/HistoriasDeUsuario/HU-INICIAL-Idiomas.md).

## Prohibido escribir texto fijo en el código

Todo texto visible sale de un archivo de idioma: etiquetas, botones, títulos,
`placeholder`, textos de accesibilidad (`aria-label`, `sr-only`, `tooltip`),
avisos, validaciones, errores, estados vacíos, correos y mensajes de la API.
**No hay excepciones por «es una sola palabra» o «es temporal».**

No es texto traducible:

- El **nombre del producto**: vive en una sola constante por proyecto (lo
  cambia `project:init`), nunca repetido en los archivos de idioma.
- Códigos y valores del contrato (`ERR_US_02`, `SUPER_ADMIN`, `ACTIVO`). Lo que
  se muestra en su lugar sí se traduce.
- Logs, comentarios, nombres de variables, tests y documentación: siguen en
  español y **nunca** pasan por los archivos de idioma.

## Idiomas

| Código | Idioma | Papel |
| --- | --- | --- |
| `es` | Español **neutro** | Idioma por defecto y fuente de las claves |
| `en` | Inglés **neutro** (ortografía de Estados Unidos) | |

Agregar otro idioma es agregar su archivo en cada carpeta `language` y sumarlo a
la lista de idiomas soportados. No exige tocar pantallas.

### Español neutro

- Trato de **tú**: «Inicia sesión», «Elige», «Puedes reenviarla». **Nunca**
  voseo («iniciá», «podés») ni «usted».
- Sin regionalismos ni modismos de ningún país («coger», «vale», «ahorita»,
  «ordenador»: se usa «computadora» o «dispositivo»).
- Vocabulario fijo: «correo electrónico» (o «correo»), «contraseña», «iniciar
  sesión», «cerrar sesión», «dispositivo».
- Mayúscula solo en la primera palabra de títulos y botones.
- Los errores dicen qué pasó y qué hacer, sin culpar a la persona.

### Inglés neutro

- Ortografía de Estados Unidos, sin jerga ni expresiones idiomáticas.
- Vocabulario fijo: *email*, *password*, *sign in*, *sign out*, *device*.
- *Sentence case* en títulos y botones.

## Cómo se elige el idioma

1. La elección guardada de la persona, si cambió el idioma alguna vez.
2. Si no, el idioma del navegador o del teléfono (`es-*` → `es`, `en-*` → `en`).
3. Si no es ninguno de los soportados, `es`.

La web y la app tienen un **selector de idioma**, también en las pantallas sin
sesión. La preferencia se guarda en el dispositivo (no en la cuenta).

Toda petición a la API lleva `Accept-Language` con el idioma vigente.

## Reglas de los archivos de idioma

- **Mismas claves y mismos marcadores** (`{nombre}`) en todos los idiomas. Una
  prueba falla si falta, sobra o difiere uno.
- Ningún valor vacío.
- Una frase completa por clave. **No se arman frases concatenando** trozos
  traducidos: el orden de las palabras cambia entre idiomas.
- Los datos van como marcadores, nunca pegados con `+` o interpolados fuera.
- Los plurales usan las variantes del sistema de cada capa, nunca `if (n === 1)`
  en la pantalla.
- Claves descriptivas en español y por significado (`usuarios.crear.titulo`),
  no por el texto que contienen.
- Fechas, horas y números se formatean con el idioma vigente.
