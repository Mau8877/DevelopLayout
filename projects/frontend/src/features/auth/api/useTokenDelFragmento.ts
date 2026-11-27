import { useLocation, useNavigate } from '@tanstack/react-router'
import { useEffect, useState } from 'react'

/** Rutas públicas cuya credencial llega en el enlace de un correo. */
type RutaConToken = '/reset-password' | '/accept-invitation'

/**
 * Toma el token del fragmento (`#token=`), lo deja solo en memoria y lo quita de
 * la URL reemplazando la entrada del historial.
 *
 * * El estado se ajusta durante el render: así también se toma un enlace nuevo
 * * pegado en la misma pestaña, que solo cambia el fragmento.
 */
export function useTokenDelFragmento(ruta: RutaConToken): string | null {
  const hash = useLocation({ select: (location) => location.hash })
  const navigate = useNavigate()
  const enLaUrl = new URLSearchParams(hash).get('token')
  const [token, setToken] = useState(enLaUrl)
  if (enLaUrl && enLaUrl !== token) setToken(enLaUrl)

  useEffect(() => {
    if (hash) void navigate({ to: ruta, replace: true })
  }, [hash, navigate, ruta])

  return token
}
