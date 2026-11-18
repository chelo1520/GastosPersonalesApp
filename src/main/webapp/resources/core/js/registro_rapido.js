/* global bootstrap */
// Registro rápido del resumen (rediseño v2). El modal lo abre el botón "+ Registrar gasto" (Bootstrap);
// acá se reabre solo cuando el servidor lo pide (data-abrir: "Guardar y cargar otro" o un error) y,
// si además llegó el pop-up de presupuesto, se muestra primero el pop-up y el modal al cerrarlo.
const modal = document.getElementById("modal-registro");
const formulario = document.getElementById("form-registro");
const importe = document.getElementById("registro-importe");
const alertaPresupuesto = document.getElementById("modal-alerta-presupuesto");

// Al abrir, el cursor va al importe: se puede cargar todo con el teclado y Enter guarda
modal.addEventListener("shown.bs.modal", () => {
  importe.focus();
  importe.select();
});

// Un solo envío aunque se apriete dos veces (no se deshabilitan los botones: perderían name="otro")
let enviando = false;
formulario.addEventListener("submit", (evento) => {
  if (enviando) {
    evento.preventDefault();
    return;
  }
  enviando = true;
});

function abrirRegistro() {
  bootstrap.Modal.getOrCreateInstance(modal).show();
}

if (alertaPresupuesto) {
  if (modal.dataset.abrir === "true") {
    alertaPresupuesto.addEventListener("hidden.bs.modal", abrirRegistro, { once: true });
  }
  bootstrap.Modal.getOrCreateInstance(alertaPresupuesto).show();
} else if (modal.dataset.abrir === "true") {
  abrirRegistro();
}
