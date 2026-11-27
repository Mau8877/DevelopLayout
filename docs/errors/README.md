# `docs/errors/` — Catálogo de códigos de error

Esta carpeta documenta, para humanos (soporte, quien consume la API desde
el frontend/mobile, o el propio equipo de backend meses después), qué
significa cada código de error (`ERR_{MODULO}_{NUMERO}`) que la API puede
devolver. El código en sí (la constante Java, `XxxErrorCodes`) le sirve al
`service` que lo lanza; esto de acá le sirve a la persona que lo recibe y
no quiere ponerse a leer código fuente para entender qué pasó.

El **proceso** de cuándo y cómo se documenta un código nuevo (obligatorio
en el mismo cambio que lo introduce) vive en
[GUIA_ERRORES_BACKEND.md](../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md).
Este `README.md` es solo el índice — no repite esas reglas.

## Cómo está organizada

Una carpeta por feature/módulo (`system/`, y a futuro `usuarios/`,
`pedidos/`, etc. — se crean recién cuando ese módulo tiene su primer
error real, no antes), y dentro un único archivo `ERRORES_{MODULO}.md` con
todos los códigos de ese módulo, cada uno en su propia sección para poder
enlazarlo directo:

```
docs/errors/
├── README.md                          <- este archivo, el índice
├── system/
│   └── ERRORES_SISTEMA.md
├── usuarios/
│   └── ERRORES_USUARIOS.md            (cuando exista el primer error de usuarios)
└── pedidos/
    └── ERRORES_PEDIDOS.md             (cuando exista el primer error de pedidos)
```

## Índice de errores

### Sistema (`SYS`)

- [`ERR_SYS_00`](system/ERRORES_SISTEMA.md#err_sys_00) — Error interno no controlado (500)
- [`ERR_SYS_01`](system/ERRORES_SISTEMA.md#err_sys_01) — Validación de Bean Validation fallida (400)
- [`ERR_SYS_02`](system/ERRORES_SISTEMA.md#err_sys_02) — Límite de requests por IP superado (429)
- [`ERR_SYS_03`](system/ERRORES_SISTEMA.md#err_sys_03) — Ruta inexistente (404)
- [`ERR_SYS_04`](system/ERRORES_SISTEMA.md#err_sys_04) — Método no permitido (405)
- [`ERR_SYS_05`](system/ERRORES_SISTEMA.md#err_sys_05) — Solicitud no compatible con el recurso (otro 4xx)

### Autenticación (`AUTH`)

- [`ERR_AUTH_01`](auth/ERRORES_AUTH.md#err_auth_01) — Credenciales inválidas (401)
- [`ERR_AUTH_02`](auth/ERRORES_AUTH.md#err_auth_02) — Cuenta bloqueada temporalmente por intentos fallidos (423)
- [`ERR_AUTH_03`](auth/ERRORES_AUTH.md#err_auth_03) — Usuario no activo (403)
- [`ERR_AUTH_04`](auth/ERRORES_AUTH.md#err_auth_04) — El refresh token ya no es válido (401)
- [`ERR_AUTH_05`](auth/ERRORES_AUTH.md#err_auth_05) — El enlace de recuperación no es válido (400)
- [`ERR_AUTH_06`](auth/ERRORES_AUTH.md#err_auth_06) — La contraseña nueva es igual a la actual (400)
- [`ERR_AUTH_07`](auth/ERRORES_AUTH.md#err_auth_07) — No autenticado (401)
- [`ERR_AUTH_08`](auth/ERRORES_AUTH.md#err_auth_08) — Acceso denegado o CSRF inválido (403)
- [`ERR_AUTH_09`](auth/ERRORES_AUTH.md#err_auth_09) — La sesión a cerrar no existe (404)
- [`ERR_AUTH_10`](auth/ERRORES_AUTH.md#err_auth_10) — La sesión actual no se cierra desde «Mis sesiones» (400)
- [`ERR_AUTH_11`](auth/ERRORES_AUTH.md#err_auth_11) — La invitación no es válida (400)

### Bitácora (`BIT`)

- [`ERR_BIT_01`](bitacora/ERRORES_BITACORA.md#err_bit_01) — Rango de fechas inválido (400)

### Usuarios (`US`)

- [`ERR_US_01`](usuarios/ERRORES_USUARIOS.md#err_us_01) — El usuario no existe (404)
- [`ERR_US_02`](usuarios/ERRORES_USUARIOS.md#err_us_02) — Ya existe una cuenta con ese correo (409)
- [`ERR_US_03`](usuarios/ERRORES_USUARIOS.md#err_us_03) — Rol inexistente (400)
- [`ERR_US_04`](usuarios/ERRORES_USUARIOS.md#err_us_04) — No podés desactivar tu propia cuenta (409)
- [`ERR_US_05`](usuarios/ERRORES_USUARIOS.md#err_us_05) — No corresponde reenviar la invitación (409)
- [`ERR_US_06`](usuarios/ERRORES_USUARIOS.md#err_us_06) — Demasiados reenvíos (409)

<!--
  A medida que se agregan módulos de negocio (usuarios, pedidos...),
  cada uno suma acá su propia sección "### Nombre (PREFIJO)" con la lista
  de sus códigos, igual que la sección de Sistema de arriba — ver
  GUIA_ERRORES_BACKEND.md para el detalle exacto de qué hacer al agregar
  un código nuevo.
-->
