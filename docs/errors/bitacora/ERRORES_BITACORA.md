# Errores — Bitácora (`BIT`)

## `ERR_BIT_01`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** la consulta de la bitácora pidió un rango con `desde` posterior a `hasta`.
- **Dónde se lanza:** `BitacoraQueryService.consultar()`
