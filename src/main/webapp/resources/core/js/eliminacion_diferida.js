// Eliminar con "Deshacer" (rediseño v2): la acción real (ejecutar) se demora; mientras tanto se puede deshacer.
// confirmar() la adelanta (otro gasto eliminado, salir de la página). Sin DOM: el reloj se inyecta para los tests.
export const DEMORA_DESHACER = 4000;

export function eliminarConDeshacer({
  ejecutar,
  alDeshacer,
  demora = DEMORA_DESHACER,
  programar = setTimeout,
  cancelar = clearTimeout,
}) {
  let pendiente = true;
  let temporizador = null;

  function terminar() {
    pendiente = false;
    cancelar(temporizador);
  }

  function confirmar() {
    if (!pendiente) {
      return false;
    }
    terminar();
    ejecutar();
    return true;
  }

  function deshacer() {
    if (!pendiente) {
      return false;
    }
    terminar();
    alDeshacer();
    return true;
  }

  temporizador = programar(confirmar, demora);

  return {
    confirmar,
    deshacer,
    estaPendiente: () => pendiente,
  };
}
