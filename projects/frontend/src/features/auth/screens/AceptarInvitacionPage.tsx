import { useTokenDelFragmento } from '../api'
import { AceptarInvitacionScreen } from './AceptarInvitacionScreen'

export function AceptarInvitacionPage() {
  const token = useTokenDelFragmento('/accept-invitation')

  // * `key`: un token distinto empieza un formulario limpio, sin el error del anterior.
  return <AceptarInvitacionScreen key={token ?? 'sin-token'} token={token} />
}
