/* global bootstrap */
// Registro rápido (rediseño v2), en el resumen y en Mis gastos. El modal lo abre "+ Registrar gasto"
// (Bootstrap); acá se reabre solo cuando el servidor lo pide (data-abrir: "Guardar y cargar otro" o un
// error) y, si además llegó el pop-up de presupuesto, se muestra primero el pop-up y el modal al cerrarlo.
const modal = document.getElementById("modal-registro");
const formulario = document.getElementById("form-registro");
const importe = document.getElementById("registro-importe");
const comprobante = document.getElementById("form-comprobante-rapido");
const archivo = document.getElementById("registro-archivo");
const analizando = document.getElementById("registro-analizando");
const alertaPresupuesto = document.getElementById("modal-alerta-presupuesto");

// Al abrir, el cursor va al importe: se puede cargar todo con el teclado y Enter guarda
modal.addEventListener("shown.bs.modal", () => {
  importe.focus();
  importe.select();
});

// Un solo envío aunque se apriete dos veces (no se deshabilitan los botones ni el archivo:
// un campo deshabilitado no viaja en el formulario)
function unSoloEnvio(form, alEnviar) {
  let enviando = false;
  form.addEventListener("submit", (evento) => {
    if (enviando) {
      evento.preventDefault();
      return;
    }
    enviando = true;
    alEnviar();
  });
}

unSoloEnvio(formulario, () => {});

// Comprobante: elegir el archivo ya lo envía; la lectura tarda unos segundos, se avisa en el modal
comprobante.classList.add("js-registro");
unSoloEnvio(comprobante, () => {
  comprobante.classList.add("is-enviando");
  analizando.hidden = false;
});
archivo.addEventListener("change", () => {
  if (archivo.files.length > 0) {
    comprobante.requestSubmit();
  }
});

// Si se vuelve con "Atrás" desde la revisión, el navegador restaura la página tal cual quedó
// ("Leyendo…" y los envíos bloqueados): se recarga para que el modal arranque de cero
window.addEventListener("pageshow", (evento) => {
  if (evento.persisted) {
    window.location.reload();
  }
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
