// Qué dice y cómo se siente la mascota del login según el estado del formulario. Puro, sin DOM.
// Frases y reglas del handoff del rediseño v2 (diseno/v2 del coord, "Texto del globo según el estado").

export function emailParece(email = "") {
  return /\S+@\S+\.\S+/.test(email);
}

// estado: { email, foco: "email" | "password" | null, mostrando: boolean, error: boolean }
export function estadoMascota(estado) {
  const emailOk = emailParece(estado.email);
  const tapandose = estado.foco === "password" && !estado.mostrando;
  let globo = "¡Hola! ¿Cómo viene el mes?";
  if (estado.error) {
    globo = "Me faltan datos…";
  } else if (tapandose) {
    globo = "No miro, prometido.";
  } else if (estado.foco === "password") {
    globo = "Uy, la estoy viendo…";
  } else if (estado.foco === "email") {
    globo = emailOk ? "¡Ese mail me suena!" : "Anotando…";
  }
  return {
    globo,
    mood: estado.error ? "worried" : emailOk ? "happy" : "neutral",
    covering: String(tapandose),
    look: estado.foco ? "form" : "cursor",
  };
}
