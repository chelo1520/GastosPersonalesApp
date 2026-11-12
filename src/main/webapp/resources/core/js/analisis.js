/* global bootstrap */
import { secuenciar } from "./analisis_pasos.js";

// "Analizar mis gastos": abre el modal con la mascota, tilda los pasos y después carga el asistente.
// El botón es un link al asistente: sin JavaScript (o sin Bootstrap) va directo.
const boton = document.getElementById("analizar-gastos");
const modalNode = document.getElementById("modal-analisis");

function hayWebGl() {
  try {
    const canvas = document.createElement("canvas");
    return Boolean(canvas.getContext("webgl") || canvas.getContext("experimental-webgl"));
  } catch {
    return false;
  }
}

if (boton && modalNode && typeof bootstrap !== "undefined") {
  const mascota = document.getElementById("analisis-mascota");
  const fallback = document.getElementById("analisis-fallback");
  const pasos = Array.from(modalNode.querySelectorAll(".app-analisis-paso"));
  const destino = modalNode.dataset.asistente || boton.getAttribute("href");
  let pulso = 0;
  let corriendo = false;

  if (mascota && (!hayWebGl() || !window.customElements.get("gastitos-mascot"))) {
    mascota.remove();
    fallback.hidden = false;
  }

  boton.addEventListener("click", (evento) => {
    evento.preventDefault();
    if (corriendo) {
      return;
    }
    corriendo = true;
    pasos.forEach((paso) => paso.classList.remove("is-listo", "is-activo"));
    pasos[0].classList.add("is-activo");
    bootstrap.Modal.getOrCreateInstance(modalNode).show();
    secuenciar(pasos.length, {
      marcar: (numero) => {
        const paso = pasos[numero - 1];
        paso.classList.remove("is-activo");
        paso.classList.add("is-listo");
        if (pasos[numero]) {
          pasos[numero].classList.add("is-activo");
        }
        // La mascota salta en cada paso, como en el prototipo
        if (mascota && mascota.isConnected) {
          pulso += 1;
          mascota.setAttribute("pulse", String(pulso));
        }
      },
      terminar: () => {
        window.location.href = destino;
      },
    });
  });

  // Si se cierra antes de terminar, el redireccionamiento igual llega: se avisa con el link visible
  modalNode.addEventListener("hidden.bs.modal", () => {
    corriendo = false;
  });
}
