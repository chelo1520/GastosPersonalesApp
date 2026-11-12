// Qué dice y cómo se siente la mascota del login según el estado de la pantalla. Puro, sin DOM.
// Frases y reglas del handoff "Login v2 (intro + glass)" del rediseño (diseno/v2 del coord).

export function emailParece(email = "") {
  return /\S+@\S+\.\S+/.test(email);
}

// estado: { intro, email, foco: "email" | "password" | null, mostrando, error, ingresando }
export function estadoMascota(estado) {
  const emailOk = emailParece(estado.email);
  const tapandose = !estado.intro && estado.foco === "password" && !estado.mostrando;
  let globo = "¿Cómo viene el mes?";
  if (estado.intro) {
    globo = "¡Hola! Yo te cuido los pesos.";
  } else if (estado.ingresando) {
    globo = "¡Vamos adentro!";
  } else if (estado.error) {
    globo = "Me faltan datos…";
  } else if (tapandose) {
    globo = "No miro, prometido.";
  } else if (estado.foco === "password") {
    globo = "Uy, la estoy viendo…";
  } else if (estado.foco === "email") {
    globo = emailOk ? "¡Ese mail me suena!" : "Anotando…";
  }
  let mood = "neutral";
  if (estado.error) {
    mood = "worried";
  } else if (estado.ingresando || emailOk || estado.intro) {
    mood = "happy";
  }
  return {
    globo,
    mood,
    covering: String(tapandose),
    look: !estado.intro && estado.foco ? "form" : "cursor",
    motion: estado.intro ? "animado" : "sobrio",
  };
}

// La intro se saltea si el sistema pide menos movimiento o si se vuelve al login con un aviso o un error
// (cuenta creada, sesión cerrada, contraseña incorrecta): ahí lo que importa es el formulario.
export function salteaIntro({ menosMovimiento, hayAviso }) {
  return Boolean(menosMovimiento || hayAviso);
}
