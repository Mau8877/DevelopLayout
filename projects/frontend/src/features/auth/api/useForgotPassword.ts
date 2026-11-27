import { useMutation } from '@tanstack/react-query'
import { apiClient } from '@/store'
import type { ForgotPasswordRequest } from '../schemas/forgotPasswordSchema'

/** Pide el enlace. El backend responde igual exista o no la cuenta. */
export function useForgotPassword() {
  return useMutation({
    mutationFn: async (request: ForgotPasswordRequest) => {
      await apiClient.post('/auth/password/forgot', request)
    },
  })
}
