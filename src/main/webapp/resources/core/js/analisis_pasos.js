// Secuencia de pasos del modal de análisis: marca uno por uno y al final avisa. Puro, sin DOM.
// programar(fn, ms) permite inyectar setTimeout en los tests.

export function secuenciar(cantidad, { marcar, terminar, intervalo = 600, programar = setTimeout }) {
  for (let paso = 1; paso <= cantidad; paso++) {
    programar(() => marcar(paso), intervalo * paso);
  }
  programar(terminar, intervalo * cantidad + 500);
}
