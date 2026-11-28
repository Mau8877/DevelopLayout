---
globs: projects/mobile/**/*
---

# Componentización — Mobile

**Todo widget reutilizable se construye genérico y en `lib/shared/widgets/`
desde la primera vez que se necesita.** Las pantallas no repiten bloques de UI
ni construyen patrones visuales "inline" que otra feature tendría que volver a
escribir. `features/{feature}/widgets/` es solo para widgets que conocen el
dominio de esa feature.

Esta regla reemplaza el criterio anterior de "se mueve a `shared/` cuando una
segunda feature lo necesita": para UI, un widget genérico nace en `shared/widgets/`.

## Dónde vive cada widget

1. **¿Ya existe?** Buscar en `lib/shared/widgets/` y en el tema
   (`core/theme/`). Si existe, se usa; si le falta una variante, se extiende en
   vez de crear uno paralelo.
2. **¿Conoce el dominio de una feature?** (usa modelos, providers o textos de
   negocio de esa feature) → `lib/features/{feature}/widgets/`. Solo en este caso.
3. **Si es genérico** → `lib/shared/widgets/`, desde el primer uso (no se espera
   a un segundo consumidor).

Un widget de feature que otra feature necesita se importa de esa feature solo si
la relación de negocio lo justifica (ver
[ARQUITECTURA_MOBILE.md](../ARQUITECTURA_MOBILE.md#regla-de-dependencia)); si no
tenía dominio, se baja a `shared/widgets/`.

## Qué se componentiza siempre

Ya existen en `shared/widgets/`: `AppTextField` (con `obscureText` también apaga
el autocorrector y las sugerencias del teclado), `AppButton`, `EstadoError`,
`pedirConfirmacion`, `mostrarError` (el `SnackBar` de error) y `SelectorIdioma`.

- Campo de texto con label, error y estilo del tema (sobre `TextFormField`, ver
  [FORMULARIOS_MOBILE.md](FORMULARIOS_MOBILE.md)).
- Botón primario/secundario con estado de carga.
- Estados de un `AsyncValue`: cargando, vacío, error con "Reintentar" (ver
  [ERRORES_LOGGING_MOBILE.md](ERRORES_LOGGING_MOBILE.md)).
- Diálogo de confirmación, encabezado de pantalla, tarjeta/ítem de lista,
  lista paginada.
- Cualquier bloque que aparezca **dos o más veces**.

Si el widget que hace falta no existe, la UT que lo necesita lo crea en
`shared/widgets/`; no lo deja inline.

## Cómo se escribe un widget genérico

- **Sin dominio ni datos:** no importa de `features/`, no lee providers de
  feature, no llama a `services/`. Recibe datos y callbacks por parámetros.
- **Textos por parámetro**, con defaults genéricos solo si son universales
  ("Cancelar", "Reintentar").
- **Tema, no valores sueltos:** colores y tipografía desde `Theme.of(context)`/
  `AppColors`; sin `Color(0x...)` ni `TextStyle` hardcodeados dentro del widget.
- **Un widget es una clase**, no un método `_buildX()` de otra clase: las clases
  permiten `const` y reconstrucciones acotadas. Constructor `const` siempre que se pueda.
- **Accesibilidad:** `Semantics`/`tooltip` en íconos sin texto, áreas táctiles
  de al menos 48×48, contraste del tema respetado.
- **Archivo:** `snake_case.dart`, un widget público por archivo.
- **Tests:** solo si el widget tiene lógica no trivial (ver
  [TESTING_MOBILE.md](TESTING_MOBILE.md)).

## Pantallas delgadas

Una pantalla de `screens/` observa un provider y compone widgets. Si supera
~150 líneas de `build` o repite un bloque, se extrae a widgets.

## Límites

No se crea una "librería de widgets" por adelantado: se crean cuando una UT los
necesita, y genéricos desde el primer día. Un widget de un solo uso con muchos
parámetros de configuración es peor que código claro.
