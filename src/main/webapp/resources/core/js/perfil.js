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
