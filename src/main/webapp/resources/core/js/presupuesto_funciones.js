// Cuentas del presupuesto que se muestran en vivo (perfil y presupuestos). Puro, sin DOM.

// ingreso × porcentaje / 100, redondeado a centavos. Con datos vacíos o inválidos devuelve 0
export function calcularPresupuesto(ingreso, porcentaje) {
  const ingresoNumero = Number(ingreso);
  const porcentajeNumero = Number(porcentaje);
  if (!(ingresoNumero > 0) || !(porcentajeNumero > 0)) {
    return 0;
  }
  return Math.round(ingresoNumero * porcentajeNumero) / 100;
}

// Formato es-AR con dos decimales: 1234567.5 → "1.234.567,50"
export function formatearPesos(monto) {
  return new Intl.NumberFormat("es-AR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(monto);
}
