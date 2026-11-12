import { validarCamposDeLogin } from "./login_funciones.js";
import { estadoMascota } from "./login_mascota.js";

const btnLoginNode = document.getElementById("btn-login");
const inputEmailNode = document.getElementById("email");
const inputPasswordNode = document.getElementById("password");
const btnMostrarNode = document.getElementById("btn-mostrar");
const formNode = document.getElementById("form-login");
const mascotaNode = document.getElementById("mascota");
const fallbackNode = document.getElementById("mascota-fallback");
const globoNode = document.getElementById("login-globo");

// El botón se habilita con "input" (no "keyup"): así también cuenta el autocompletar y el pegar
function habilitarIngresar() {
  btnLoginNode.disabled = !validarCamposDeLogin(inputEmailNode.value, inputPasswordNode.value);
}

// Estado del formulario que mira la mascota. El error del servidor dura hasta que se escribe algo
const estado = {
  email: inputEmailNode.value,
  foco: null,
  mostrando: false,
  error: formNode.dataset.error === "true",
};
let pulso = 0;

function actualizarMascota() {
  const m = estadoMascota(estado);
  globoNode.textContent = m.globo;
  if (mascotaNode) {
    mascotaNode.setAttribute("mood", m.mood);
    mascotaNode.setAttribute("covering", m.covering);
    mascotaNode.setAttribute("look", m.look);
  }
}

function tecla() {
  estado.email = inputEmailNode.value;
  estado.error = false;
  pulso += 1;
  if (mascotaNode) {
    mascotaNode.setAttribute("pulse", String(pulso));
  }
  habilitarIngresar();
  actualizarMascota();
}

[inputEmailNode, inputPasswordNode].forEach((input) => {
  input.addEventListener("input", tecla);
  input.addEventListener("focus", () => {
    estado.foco = input === inputEmailNode ? "email" : "password";
    actualizarMascota();
  });
  input.addEventListener("blur", () => {
    estado.foco = null;
    actualizarMascota();
  });
});

// Mostrar u ocultar la contraseña; con mousedown no se pierde el foco del campo
btnMostrarNode.addEventListener("mousedown", (evento) => evento.preventDefault());
btnMostrarNode.addEventListener("click", () => {
  estado.mostrando = !estado.mostrando;
  inputPasswordNode.type = estado.mostrando ? "text" : "password";
  btnMostrarNode.textContent = estado.mostrando ? "Ocultar" : "Mostrar";
  btnMostrarNode.setAttribute("aria-pressed", String(estado.mostrando));
  btnMostrarNode.setAttribute("aria-label", estado.mostrando ? "Ocultar la contraseña" : "Mostrar la contraseña");
  inputPasswordNode.focus();
  actualizarMascota();
});

// Sin WebGL (o si three no carga desde el CDN) queda el logo estático
function hayWebGl() {
  try {
    const canvas = document.createElement("canvas");
    return Boolean(canvas.getContext("webgl") || canvas.getContext("experimental-webgl"));
  } catch {
    return false;
  }
}

if (mascotaNode && (!hayWebGl() || !window.customElements.get("gastitos-mascot"))) {
  mascotaNode.remove();
  fallbackNode.hidden = false;
} else if (mascotaNode) {
  // Si three no llegó en unos segundos (sin red), se muestra el logo
  setTimeout(() => {
    if (!mascotaNode.querySelector("canvas")) {
      mascotaNode.remove();
      fallbackNode.hidden = false;
    }
  }, 6000);
}

habilitarIngresar();
actualizarMascota();
