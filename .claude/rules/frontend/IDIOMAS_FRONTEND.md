---
globs: projects/frontend/**/*
---

# Idiomas — Frontend

Complementa a [IDIOMAS.md](../IDIOMAS.md), que manda sobre **qué** se traduce y
cómo se escribe. Acá está el **cómo** en la web. Librería: **i18next** con
**react-i18next**.

## Dónde viven los textos

**Cada feature es dueña de sus textos**, en su carpeta `language/`:

```
src/
├── features/
│   ├── auth/language/       es.json  en.json
│   └── usuarios/language/   es.json  en.json
├── shared/
│   └── language/            es.json  en.json     <- textos comunes
│       ├── i18n.ts          <- configuración única de i18next
│       ├── idiomas.ts       <- idiomas soportados y detección
│       ├── useIdioma.ts     <- leer y cambiar el idioma
│       └── index.ts
└── i18next.d.ts             <- tipos de las claves
```

- El **espacio de nombres** de i18next es la carpeta dueña: `auth`, `usuarios`,
  `shared`. Una feature nueva crea su `language/es.json` y `language/en.json`.
- `shared/language/` tiene lo que no es de una feature: layouts,
  `components/`, utilidades, errores del cliente HTTP y botones genéricos. Un
  texto no va ahí «porque lo usan dos pantallas» de una misma feature.
- Una feature **no usa las claves de otra**. Si dos necesitan lo mismo y no es
  de dominio, va a `shared`.
- `i18n.ts` **descubre** los archivos con
  `import.meta.glob('/src/**/language/{es,en}.json', { eager: true })`: `shared/`
  no importa código de ninguna feature, así que la regla de dependencia se
  mantiene.

### Al agregar una feature con textos

Además de sus dos JSON, se actualizan estos puntos:

1. `src/i18next.d.ts`: sus tipos (una prueba lo exige).
2. La expresión de espacios de nombres de
   `shared/language/sinTextoFijo.test.ts`: reconoce los mensajes de Zod.
3. `ClaveDeMenu` en `layouts/navegacion/navegacion.ts`, solo si el menú usa
   claves de la feature.

## Claves tipadas

`src/i18next.d.ts` declara los `es.json` como recursos (solo tipos). Una clave
mal escrita **rompe `pnpm build`**. Una prueba exige que cada carpeta
`language` esté declarada ahí.

## Uso

```tsx
const { t } = useTranslation('usuarios')
return <Button>{t('crear.boton')}</Button>
```

- Textos con datos: `t('roles.aviso', { correo })`. React ya escapa el
  resultado; **nunca** se muestra una traducción con `dangerouslySetInnerHTML`.
- Plurales: claves `_one` y `_other` con `count`.
- Con marcado en medio de una frase se usa `<Trans>`, no frases partidas.
- Los componentes genéricos de `components/` siguen recibiendo textos por props;
  sus valores por defecto salen de `shared`. La salida del CLI de shadcn que trae
  textos en inglés se edita (con un comentario que lo diga).
- Nunca se guarda un texto ya traducido en una constante de módulo, en un store
  ni en la caché: quedaría en el idioma anterior. Se guarda la **clave**.

## Validaciones (Zod)

Los mensajes de un schema son **claves** (`'auth:validacion.correoObligatorio'`),
no textos. Se traducen en un único punto al mostrarse, así un error visible
cambia de idioma con la pantalla. Ver
[FORMULARIOS_ZOD_TANSTACK_FRONTEND.md](FORMULARIOS_ZOD_TANSTACK_FRONTEND.md).

## Errores de la API

`ApiError.message` ya llega en el idioma pedido con `Accept-Language`: se muestra
tal cual y **no** se traduce en el cliente por código. Los errores propios del
cliente (`CLIENT_NETWORK`, `CLIENT_TIMEOUT`, `CLIENT_UNKNOWN`) sí salen de
`shared`.

## Cambiar de idioma

`useIdioma` es la única vía. Al cambiar: i18next, el atributo `lang` de
`<html>`, la preferencia en `localStorage` (es un dato sin sensibilidad), el
formato de fechas y la invalidación de la caché de TanStack Query, para que los
mensajes del servidor se pidan de nuevo.

Las fechas y números usan el idioma vigente (`es` y `en-US`); no se escribe
`'es'` fijo en ningún formateador.

## Control automático

- ESLint (`eslint-plugin-i18next`, `no-literal-string`) rechaza texto fijo en JSX,
  incluidos `aria-label`, `placeholder`, `title`, `alt` y `label`.
- Una prueba cubre lo que ESLint no ve: textos fijos en avisos (`toast*`),
  mensajes de Zod que no sean claves y voseo en los `es.json`.
- Otra prueba compara claves y marcadores entre idiomas.
- No se desactiva la regla en un archivo para «salir del paso»; una excepción
  puntual lleva el motivo en el comentario.

## Tests

`setupTests.ts` inicia i18next en `es`. Las pruebas de pantalla buscan el texto
en español, como lo ve la persona.
