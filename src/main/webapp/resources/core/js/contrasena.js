import { reglasDeContrasena } from "./login_funciones.js";

// Marca con check verde cada requisito de fragments/contrasena que la contraseña ya cumple
export function marcarRequisitos(password) {
  const reglas = reglasDeContrasena(password);
  document.querySelectorAll("#password-requisitos [data-regla]").forEach((item) => {
    item.classList.toggle("is-ok", reglas[item.dataset.regla]);
  });
}
