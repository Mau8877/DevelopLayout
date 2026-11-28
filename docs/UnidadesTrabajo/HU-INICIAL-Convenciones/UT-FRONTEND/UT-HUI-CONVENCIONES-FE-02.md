# UT-HUI-CONVENCIONES-FE-02 — Barriles en todas las carpetas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-CONVENCIONES-FE-02` |
| **Nombre** | Barriles en todas las carpetas |
| **Historia** | HU-INICIAL-Convenciones-FE |
| **Depende de** | 01 |

## Antes de implementar

ARQUITECTURA_FRONTEND (barriles).

## Solicitud de negocio

Importar siempre igual y poder reorganizar sin romper a nadie.

## Objetivo

`index.ts` en cada carpeta obligatoria, imports por barril y una prueba que lo exige.

## Alcance

- Crear los `index.ts` que faltan en las subcarpetas de `auth`, `bitacora` y `usuarios`, y en `utils/` y `shared/api/`; revisar que los existentes exporten todo.
- Ajustar los imports según la tabla de la regla. El barril raíz de cada feature sigue nombrando solo lo público.
- Archivos con efectos al importarse (como la configuración de dayjs) o con exportación por defecto se reexportan con nombre.
- Prueba `estructura.test.ts`: barriles presentes y completos, sin saltarse el barril de una feature, sin ciclos con el barril propio.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/`
- `projects/frontend/src/utils/index.ts`
- `projects/frontend/src/shared/api/index.ts`
- `projects/frontend/src/estructura.test.ts`

## Seguridad

Sin impacto de seguridad: solo cambia cómo se importa.

## Fuera de alcance (no tocar)

- `components/ui/`, `lib/`, `routes/` y `tests/`.
- Cambiar comportamiento.

## Resultado esperado

Toda carpeta obligatoria expone su contenido por su barril.

## Validación

1. La prueba falla al quitar un barril, dejar un archivo sin exportar o importar un archivo interno de otra feature.
2. `pnpm build` sin avisos de dependencias circulares; `pnpm lint` y `pnpm test` en verde.
