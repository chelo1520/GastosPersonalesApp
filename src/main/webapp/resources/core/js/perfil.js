import { validarPasswordNueva } from "./login_funciones.js";
import { marcarRequisitos } from "./contrasena.js";
import { calcularPresupuesto, formatearPesos } from "./presupuesto_funciones.js";

// Presupuesto por defecto: el monto (ingreso × %) se recalcula mientras se escribe
const inputIngresoNode = document.getElementById("ingresoMensual");
const inputPorcentajeNode = document.getElementById("porcentaje");
const presupuestoCalculadoNode = document.getElementById("perfil-presupuesto-calculado");

function recalcularPresupuesto() {
  presupuestoCalculadoNode.textContent = formatearPesos(
    calcularPresupuesto(inputIngresoNode.value, inputPorcentajeNode.value),
  );
}

// Los links "Definir presupuesto" llegan con #presupuesto: el desplegable se abre solo
const desplegablePresupuestoNode = document.getElementById("guia-destino-perfil");
if (desplegablePresupuestoNode && window.location.hash === "#presupuesto") {
  desplegablePresupuestoNode.open = true;
}

if (inputIngresoNode && inputPorcentajeNode && presupuestoCalculadoNode) {
  [inputIngresoNode, inputPorcentajeNode].forEach((input) =>
    input.addEventListener("input", recalcularPresupuesto),
  );
}

// Cambiar contraseña: check verde por requisito y el botón se habilita con todo completo
const btnCambiarNode = document.getElementById("btn-cambiar-contrasena");
const inputActualNode = document.getElementById("actual");
const inputPasswordNode = document.getElementById("password");
const inputConfirmacionNode = document.getElementById("confirmacion");

function actualizar() {
  const password = inputPasswordNode.value;
  const confirmacion = inputConfirmacionNode.value;
  marcarRequisitos(password);
  inputConfirmacionNode.classList.toggle("is-invalid", confirmacion.length > 0 && password !== confirmacion);
  btnCambiarNode.disabled = inputActualNode.value.length === 0 || !validarPasswordNueva(password, confirmacion);
}

if (btnCambiarNode) {
  [inputActualNode, inputPasswordNode, inputConfirmacionNode].forEach((input) =>
    input.addEventListener("input", actualizar),
  );
  actualizar();
}

// Tabla anual: el lápiz abre el modal con el mes, su total actual y el valor por defecto del perfil
const tablaAnualNode = document.getElementById("tabla-anual");
const modalMesNode = document.getElementById("modal-mes");

if (tablaAnualNode && modalMesNode) {
  const titulo = modalMesNode.querySelector("#modal-mes-titulo");
  const mesValor = modalMesNode.querySelector("#modal-mes-valor");
  const monto = modalMesNode.querySelector("#modal-mes-monto");
  const ayuda = modalMesNode.querySelector("#modal-mes-ayuda");
  const error = modalMesNode.querySelector("#modal-mes-error");
  const porDefecto = Number(tablaAnualNode.dataset.porDefecto) || 0;

  function mostrarModal() {
    if (typeof window.bootstrap !== "undefined") {
      window.bootstrap.Modal.getOrCreateInstance(modalMesNode).show();
    } else {
      modalMesNode.classList.add("show");
      modalMesNode.style.display = "block";
      modalMesNode.removeAttribute("aria-hidden");
    }
  }

  function abrirMes(fila, mensajeDeError) {
    titulo.textContent = "Presupuesto de " + (fila.dataset.nombre || "este mes");
    mesValor.value = fila.dataset.mes || "";
    monto.value = fila.dataset.total || "";
    ayuda.textContent =
      fila.dataset.propio === "true"
        ? "Este mes tiene un total propio. Si lo cambiás, reemplaza al anterior."
        : porDefecto > 0
          ? "Hoy usa el valor por defecto. Al guardar, este mes pasa a tener un total propio."
          : "Sin presupuesto por defecto: este será el total del mes.";
    error.hidden = !mensajeDeError;
    error.textContent = mensajeDeError || "";
    mostrarModal();
    monto.focus();
    monto.select();
  }

  tablaAnualNode.addEventListener("click", (evento) => {
    const lapiz = evento.target.closest(".app-perfil-mes-lapiz");
    if (lapiz) {
      abrirMes(lapiz.closest("tr"));
    }
  });

  // Si el servidor rechazó un total, se reabre el modal de ese mes con el mensaje
  const mesConError = tablaAnualNode.dataset.errorMes;
  const alertaMes = document.getElementById("error-mes");
  if (mesConError && alertaMes) {
    const fila = tablaAnualNode.querySelector(`tr[data-mes="${mesConError}"]`);
    if (fila) {
      alertaMes.hidden = true;
      window.addEventListener("DOMContentLoaded", () => abrirMes(fila, alertaMes.textContent.trim()));
    }
  }
}
