import { emailParece, estadoMascota } from "../login_mascota.js";

describe("Mascota del login", () => {
  const base = { email: "", foco: null, mostrando: false, error: false };

  it("saluda y mira al cursor cuando nadie está escribiendo", () => {
    expect(estadoMascota(base)).toEqual({
      globo: "¡Hola! ¿Cómo viene el mes?",
      mood: "neutral",
      covering: "false",
      look: "cursor",
    });
  });

  it("anota mientras se escribe el email y se alegra cuando parece un mail", () => {
    expect(estadoMascota({ ...base, foco: "email", email: "med" }).globo).toBe("Anotando…");

    const conMail = estadoMascota({ ...base, foco: "email", email: "medio@gastitos.demo" });

    expect(conMail.globo).toBe("¡Ese mail me suena!");
    expect(conMail.mood).toBe("happy");
    expect(conMail.look).toBe("form");
  });

  it("se tapa los ojos con la contraseña oculta y espía si se muestra", () => {
    const oculta = estadoMascota({ ...base, foco: "password" });

    expect(oculta.globo).toBe("No miro, prometido.");
    expect(oculta.covering).toBe("true");

    const visible = estadoMascota({ ...base, foco: "password", mostrando: true });

    expect(visible.globo).toBe("Uy, la estoy viendo…");
    expect(visible.covering).toBe("false");
  });

  it("se preocupa con un error aunque el email sea válido", () => {
    const conError = estadoMascota({ ...base, email: "medio@gastitos.demo", error: true });

    expect(conError.globo).toBe("Me faltan datos…");
    expect(conError.mood).toBe("worried");
  });

  it("reconoce un email con forma de mail", () => {
    expect(emailParece("a@b.co")).toBeTrue();
    expect(emailParece("sin-arroba")).toBeFalse();
    expect(emailParece("")).toBeFalse();
  });
});
