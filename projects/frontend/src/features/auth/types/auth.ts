import type { components } from '@/shared/api/schema'

/**
 * Usuario de la sesión actual (respuesta de GET /auth/me y de POST /auth/login).
 *
 * * Sale del contrato OpenAPI generado (`pnpm openapi:types`), no se escribe a mano.
 * * `Required`: springdoc marca todos los campos de respuesta como opcionales porque el
 * * DTO no declara cuáles son obligatorios; el backend siempre los devuelve.
 */
export type UsuarioSesion = Required<components['schemas']['UsuarioSesionResponse']>
