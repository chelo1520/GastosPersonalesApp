import { validarCamposDeRegistro } from "./login_funciones.js";
import { marcarRequisitos } from "./contrasena.js";

const btnRegistrarmeNode = document.getElementById("btn-registrarme");
const inputEmailNode = document.getElementById("email");
const inputPasswordNode = document.getElementById("password");
const inputConfirmacionNode = document.getElementById("confirmacion");

function actualizar() {
  const password = inputPasswordNode.value;
  const confirmacion = inputConfirmacionNode.value;
  marcarRequisitos(password);
  // El aviso de "no coinciden" aparece recién cuando ya se escribió algo en la confirmación
  inputConfirmacionNode.classList.toggle("is-invalid", confirmacion.length > 0 && password !== confirmacion);
  btnRegistrarmeNode.disabled = !validarCamposDeRegistro(inputEmailNode.value, password, confirmacion);
}

[inputEmailNode, inputPasswordNode, inputConfirmacionNode].forEach((input) =>
  input.addEventListener("input", actualizar)
);
actualizar();
