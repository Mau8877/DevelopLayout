---
globs: projects/frontend/**/*
---

# Formularios (Zod + TanStack Form) — Frontend

## Un solo schema, dos usos

El schema de Zod de un recurso vive en `schemas/` dentro de la feature
(`src/features/{feature}/schemas/{recurso}Schema.ts`) y se reutiliza para:

1. Validar el formulario en el cliente, vía `@tanstack/react-form`.
2. Tipar y validar el payload que se manda a la API (el `Request` DTO del
   lado del frontend, inferido del schema con `z.infer<typeof schema>`).

**No se duplica** la validación en dos lugares (un schema para el form y un
`type CrearUsuarioRequest = {...}` escrito a mano aparte) — el segundo se
deriva del primero:

```ts
// src/features/usuarios/schemas/crearUsuarioSchema.ts
import { z } from 'zod'

export const crearUsuarioSchema = z.object({
  email: z
    .string()
    .trim()
    .min(1, 'usuarios:validacion.correoObligatorio')
    .max(255, 'usuarios:validacion.correoLargo')
    .pipe(z.email('usuarios:validacion.correoInvalido')),
  roles: z.array(z.string()).min(1, 'usuarios:validacion.rolObligatorio'),
})

export type CrearUsuarioRequest = z.infer<typeof crearUsuarioSchema>
```

Si el contrato ya está en `shared/api/schema.d.ts`, el tipo del request se
comprueba contra él. El límite de 72 de una contraseña se mide en **bytes**, como
en el backend (BCrypt), no en caracteres.

Si el schema de creación y el de edición difieren (ej. edición no pide
password), se definen como schemas separados pero uno puede derivar del otro
con `.partial()`/`.omit()`/`.extend()` en vez de copiar los campos a mano.

## Mensajes: claves de idioma

El proyecto es multi-idioma (ver [IDIOMAS_FRONTEND.md](IDIOMAS_FRONTEND.md)). El
mensaje de cada regla es una **clave**, nunca una frase:

```ts
z.string().min(1, 'auth:validacion.correoObligatorio')
```

Los errores se traducen al mostrarse y van **inline, debajo del campo
correspondiente**, no en un aviso genérico que no indica qué campo falló.

## Integración con TanStack Form

Las versiones instaladas (`@tanstack/react-form` ^1.33, `zod` ^4.4) soportan
pasar el schema de Zod **directamente** como validador (Zod 4 implementa el
protocolo Standard Schema, que TanStack Form v1 consume nativamente, sin
paquete adapter intermedio):

```tsx
const form = useForm({
  defaultValues: { email: '', roles: [] as string[] },
  validators: {
    onChange: crearUsuarioSchema,
  },
  onSubmit: async ({ value }) => {
    // value ya viene tipado como CrearUsuarioRequest
  },
})
```

> Este patrón (schema directo, sin adapter) corresponde a las versiones
> exactas instaladas hoy en `package.json`. Si en algún momento se
> actualiza `@tanstack/react-form` o `zod` a un major distinto, **verificar
> primero** en el changelog de esa versión si el soporte de Standard Schema
> sigue funcionando igual antes de asumir que este ejemplo sigue vigente —
> es una integración que evoluciona rápido entre versiones de ambas
> librerías.

## Validar al salir de un campo

**No se declara un validador `onBlur` además de `onChange`.** Sus errores solo se
limpian al volver a salir del campo: tras pasar del correo a la contraseña quedaba
anotado «la contraseña es obligatoria», y al escribirla y pulsar Enter sin salir
del campo el envío se bloqueaba. Para mostrar errores al salir, el campo usa
`onBlur={validarAlSalir(field)}` (`utils/formularios/validarAlSalir.ts`), que repite la
validación de `onChange`. Todo formulario tiene un test que **envía con Enter**,
no solo con clic. El mensaje de un campo se obtiene con `primerError`.

Se prefiere `validators.onChange` (valida mientras el usuario escribe) sobre
solo validar en `onSubmit`, para que el error aparezca inline apenas el
campo es inválido, no recién al intentar enviar.
