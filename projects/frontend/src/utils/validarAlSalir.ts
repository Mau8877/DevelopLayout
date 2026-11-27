interface CampoConValidacion {
  handleBlur: () => void
  validate: (causa: 'change') => unknown
}

/**
 * `onBlur` de un campo: lo marca como tocado y repite la validación de `onChange`.
 *
 * ! No declarar además un validador `onBlur` en el formulario: sus errores solo se
 * ! limpian al volver a salir del campo, y al escribir y pulsar Enter sin salir el
 * ! envío quedaba bloqueado por un error que ya no era cierto.
 */
export function validarAlSalir(campo: CampoConValidacion) {
  return () => {
    campo.handleBlur()
    void campo.validate('change')
  }
}
