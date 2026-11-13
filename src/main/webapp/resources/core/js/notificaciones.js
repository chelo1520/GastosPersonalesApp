// Notificaciones emergentes del resumen (alertas de gastos hormiga): aparecen cada vez que el servidor manda
// la alerta, como antes el banner, pero se van solas. Con el mouse encima esperan; la ✕ las cierra antes.
export const DURACION_NOTIFICACION = 6000;

function cerrar(notificacion) {
  if (notificacion.classList.contains("is-saliendo")) {
    return;
  }
  notificacion.classList.add("is-saliendo");
  setTimeout(() => notificacion.remove(), 300);
}

document.querySelectorAll(".app-notificacion").forEach((notificacion) => {
  let temporizador = setTimeout(() => cerrar(notificacion), DURACION_NOTIFICACION);
  notificacion.addEventListener("mouseenter", () => clearTimeout(temporizador));
  notificacion.addEventListener("mouseleave", () => {
    temporizador = setTimeout(() => cerrar(notificacion), DURACION_NOTIFICACION / 2);
  });
  notificacion.querySelector(".app-notificacion-cerrar").addEventListener("click", () => {
    clearTimeout(temporizador);
    cerrar(notificacion);
  });
});
