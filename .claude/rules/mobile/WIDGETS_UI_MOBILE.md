---
globs: projects/mobile/**/*
---

# Widgets / UI — Mobile

## Compartidos vs. de una sola feature

El criterio completo (árbol de decisión, qué se componentiza siempre, cómo se
escribe un widget genérico) está en
[COMPONENTIZACION_MOBILE.md](COMPONENTIZACION_MOBILE.md). En resumen:

- Un widget que **conoce el dominio** de una feature vive en
  `lib/features/{feature}/widgets/` (subcarpeta que se agrega cuando hace falta).
- Un widget **genérico** nace directo en `lib/shared/widgets/`, sin esperar a un
  segundo consumidor.

## Tema (`ThemeData`)

`lib/core/theme/app_theme.dart` ya define `AppColors`/`AppTheme.light`/`AppTheme.dark`
y `lib/main.dart` los referencia vía `MaterialApp(theme:, darkTheme:)` — pero
con una **paleta neutra de placeholder**, no la identidad de marca real del
proyecto. Mismo criterio que
[TAILWIND_STYLES_FRONTEND.md](../frontend/TAILWIND_STYLES_FRONTEND.md) del
lado web (que define la misma estructura de tokens con valores neutros):
hasta que exista la paleta final del proyecto, no se inventan colores de
marca acá — cuando se defina, se reemplazan los `Color(0x...)` de
`AppColors` por los reales, en el mismo cambio que actualiza `index.css`
del lado web (ambos son espejo manual de la misma paleta, ver el
comentario en `app_theme.dart`).
