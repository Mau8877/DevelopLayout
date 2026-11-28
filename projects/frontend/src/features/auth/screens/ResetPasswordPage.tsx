import { useTokenDelFragmento } from '../api'
import { ResetPasswordScreen } from './ResetPasswordScreen'

export function ResetPasswordPage() {
  const token = useTokenDelFragmento('/reset-password')

  // * `key`: un token distinto empieza un formulario limpio, sin el error del anterior.
  return <ResetPasswordScreen key={token ?? 'sin-token'} token={token} />
}
