import { validarPasswordNueva } from "./login_funciones.js";
import { marcarRequisitos } from "./contrasena.js";

const btnRestablecerNode = document.getElementById("btn-restablecer");
const inputPasswordNode = document.getElementById("password");
const inputConfirmacionNode = document.getElementById("confirmacion");

function actualizar() {
  const password = inputPasswordNode.value;
  const confirmacion = inputConfirmacionNode.value;
  marcarRequisitos(password);
  inputConfirmacionNode.classList.toggle("is-invalid", confirmacion.length > 0 && password !== confirmacion);
  btnRestablecerNode.disabled = !validarPasswordNueva(password, confirmacion);
}

[inputPasswordNode, inputConfirmacionNode].forEach((input) => input.addEventListener("input", actualizar));
actualizar();
