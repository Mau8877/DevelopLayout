import { useLocation, useNavigate } from '@tanstack/react-router'
import { useEffect, useState } from 'react'
import { ResetPasswordScreen } from './ResetPasswordScreen'

function tokenDelFragmento(hash: string): string | null {
  return new URLSearchParams(hash).get('token')
}

/**
 * Toma el token del fragmento (`#token=`), lo deja solo en memoria y lo quita de
 * la URL reemplazando la entrada del historial.
 *
 * * El estado se ajusta durante el render: así también se toma un enlace nuevo
 * * pegado en la misma pestaña, que solo cambia el fragmento.
 */
export function ResetPasswordPage() {
  const hash = useLocation({ select: (location) => location.hash })
  const navigate = useNavigate()
  const enLaUrl = tokenDelFragmento(hash)
  const [token, setToken] = useState(enLaUrl)
  if (enLaUrl && enLaUrl !== token) setToken(enLaUrl)

  useEffect(() => {
    if (hash) void navigate({ to: '/reset-password', replace: true })
  }, [hash, navigate])

  // * `key`: un token distinto empieza un formulario limpio, sin el error del anterior.
  return <ResetPasswordScreen key={token ?? 'sin-token'} token={token} />
}
