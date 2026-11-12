import { validarCamposDeLogin } from "./login_funciones.js";
import { estadoMascota, salteaIntro } from "./login_mascota.js";

// Login v2 "intro + glass" (diseno/v2 del coord). Fases en data-fase de <main>:
// entrada -> intro (mascota sola) -> formulario (escena a la izquierda, panel crema a la derecha).
const loginNode = document.getElementById("login");
const panelNode = document.getElementById("login-panel");
const btnIntroNode = document.getElementById("btn-intro");
const btnLoginNode = document.getElementById("btn-login");
const inputEmailNode = document.getElementById("email");
const inputPasswordNode = document.getElementById("password");
const btnMostrarNode = document.getElementById("btn-mostrar");
const formNode = document.getElementById("form-login");
const errorNode = document.getElementById("login-error");
const mascotaNode = document.getElementById("mascota");
const fallbackNode = document.getElementById("mascota-fallback");
const globoNode = document.getElementById("login-globo");

const MENSAJE_FALTAN_DATOS = "Completá email y contraseña para entrar.";
const enfocables = panelNode.querySelectorAll("input, button, a");

const estado = {
  intro: true,
  email: inputEmailNode.value,
  foco: null,
  mostrando: false,
  error: formNode.dataset.error === "true",
  ingresando: false,
};
let pulso = 0;

function actualizarMascota() {
  const m = estadoMascota(estado);
  globoNode.textContent = m.globo;
  if (mascotaNode) {
    mascotaNode.setAttribute("mood", m.mood);
    mascotaNode.setAttribute("covering", m.covering);
    mascotaNode.setAttribute("look", m.look);
    mascotaNode.setAttribute("motion", m.motion);
  }
}

// Cada cambio de "pulse" hace saltar a la mascota: al entrar, al pasar al formulario, con cada tecla y al enviar
function saltar() {
  pulso += 1;
  if (mascotaNode) {
    mascotaNode.setAttribute("pulse", String(pulso));
  }
}

// Mientras está la intro el formulario no se alcanza con Tab
function formularioAlcanzable(alcanzable) {
  enfocables.forEach((nodo) => {
    if (alcanzable) {
      nodo.removeAttribute("tabindex");
    } else {
      nodo.setAttribute("tabindex", "-1");
    }
  });
  panelNode.setAttribute("aria-hidden", String(!alcanzable));
}

function mostrarFormulario() {
  if (!estado.intro) {
    return;
  }
  estado.intro = false;
  loginNode.dataset.fase = "formulario";
  formularioAlcanzable(true);
  document.getElementById("login-intro").setAttribute("aria-hidden", "true");
  saltar();
  actualizarMascota();
}

function mostrarError(mensaje) {
  errorNode.textContent = mensaje;
  errorNode.hidden = false;
}

function limpiarError() {
  estado.error = false;
  errorNode.hidden = true;
}

// Intro: el botón "Ingresar" o cualquier tecla pasan al formulario
btnIntroNode.addEventListener("click", mostrarFormulario);
window.addEventListener("keydown", (evento) => {
  if (estado.intro && !evento.metaKey && !evento.ctrlKey) {
    mostrarFormulario();
  }
});

function tecla() {
  estado.email = inputEmailNode.value;
  limpiarError();
  saltar();
  actualizarMascota();
}

[inputEmailNode, inputPasswordNode].forEach((input) => {
  input.addEventListener("input", tecla);
  input.addEventListener("focus", () => {
    // Si algo enfoca el formulario durante la intro (autocompletar, un test), se pasa al formulario
    mostrarFormulario();
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

// Si falta un dato se avisa sin ir al servidor; si están los dos, "Ingresando…" y se envía
formNode.addEventListener("submit", (evento) => {
  if (!validarCamposDeLogin(inputEmailNode.value, inputPasswordNode.value)) {
    evento.preventDefault();
    estado.error = true;
    mostrarError(MENSAJE_FALTAN_DATOS);
    saltar();
    actualizarMascota();
    return;
  }
  estado.ingresando = true;
  btnLoginNode.textContent = "Ingresando…";
  btnLoginNode.disabled = true;
  saltar();
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

const cajaMascotaNode = document.getElementById("mascota-caja");

function mostrarFallback() {
  (cajaMascotaNode || mascotaNode).remove();
  fallbackNode.hidden = false;
}

if (mascotaNode && (!hayWebGl() || !window.customElements.get("gastitos-mascot"))) {
  mostrarFallback();
} else if (mascotaNode) {
  // Si three no llegó en unos segundos (sin red), se muestra el logo
  setTimeout(() => {
    if (!mascotaNode.querySelector("canvas")) {
      mostrarFallback();
    }
  }, 6000);
}

// Arranque: con aviso, error o menos movimiento se va directo al formulario; si no, la intro a los 120 ms
const menosMovimiento = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
const hayAviso = estado.error || panelNode.querySelector(".login-aviso") !== null;
if (salteaIntro({ menosMovimiento, hayAviso })) {
  loginNode.classList.add("is-directo");
  mostrarFormulario();
  requestAnimationFrame(() => requestAnimationFrame(() => loginNode.classList.remove("is-directo")));
} else {
  formularioAlcanzable(false);
  actualizarMascota();
  setTimeout(() => {
    // Si ya se pasó al formulario (una tecla en los primeros milisegundos), no se vuelve a la intro
    if (estado.intro) {
      loginNode.dataset.fase = "intro";
      saltar();
    }
  }, 120);
}
