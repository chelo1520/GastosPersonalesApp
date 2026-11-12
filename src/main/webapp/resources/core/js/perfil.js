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

// Tabla anual: el lápiz muestra el campo del total en la misma fila; cancelar o Escape lo cierran
const tablaAnualNode = document.getElementById("tabla-anual");

function editarMes(celda, activo) {
  const ver = celda.querySelector(".app-perfil-mes-ver");
  const form = celda.querySelector(".app-perfil-mes-editar");
  if (!ver || !form) {
    return;
  }
  ver.hidden = activo;
  form.hidden = !activo;
  const monto = form.querySelector("input[name='monto']");
  if (activo) {
    monto.focus();
    monto.select();
  } else {
    monto.value = monto.defaultValue;
  }
}

if (tablaAnualNode) {
  tablaAnualNode.addEventListener("click", (evento) => {
    const lapiz = evento.target.closest(".app-perfil-mes-lapiz");
    if (lapiz) {
      editarMes(lapiz.closest("td"), true);
      return;
    }
    const cancelar = evento.target.closest(".app-perfil-mes-cancelar");
    if (cancelar) {
      editarMes(cancelar.closest("td"), false);
    }
  });
  tablaAnualNode.addEventListener("keydown", (evento) => {
    if (evento.key === "Escape" && evento.target.closest(".app-perfil-mes-editar")) {
      editarMes(evento.target.closest("td"), false);
    }
  });
}
