import { readFileSync } from "node:fs";
import { JSDOM } from "jsdom";

const codigo = readFileSync(new URL("../comprobante.js", import.meta.url), "utf8");

function gasto(i, importe) {
  return `
    <fieldset class="app-gasto-propuesto">
      <legend><label><input type="checkbox" class="app-incluir" name="gastos[${i}].incluir" checked>
        <span class="app-gasto-propuesto-nombre">Gasto ${i}</span></label></legend>
      <div class="app-gasto-propuesto-linea">
        <button type="button" class="app-gasto-propuesto-resumen" aria-expanded="true">
          <span class="app-gasto-propuesto-datos">
            <span class="app-gasto-propuesto-fecha"></span>
            <span class="app-gasto-propuesto-importe"></span>
            <span class="app-gasto-propuesto-categoria"></span>
          </span>
        </button>
        <span class="app-gasto-propuesto-acciones">
          <button type="button" class="app-gasto-propuesto-aceptar">✓</button>
          <button type="button" class="app-gasto-propuesto-descartar">✗</button>
        </span>
      </div>
      <div class="app-gasto-propuesto-campos">
        <input type="number" name="gastos[${i}].importe" value="${importe}" required>
        <input type="date" name="gastos[${i}].fecha" value="2026-11-05" required>
        <select name="gastos[${i}].categoriaId" required>
          <option value="">Seleccioná</option><option value="3" selected>Delivery</option>
        </select>
        <input type="text" name="gastos[${i}].descripcion" value="Gasto ${i}" required>
      </div>
    </fieldset>`;
}

describe("Revisión compacta del comprobante", () => {
  let dom;
  let documento;

  function armar(error = false) {
    dom = new JSDOM(`
      <form id="form-comprobante" ${error ? "data-abrir-todo=\"true\"" : ""}>
        <div id="revision-progreso" hidden>
          <strong id="revision-aceptados">0</strong><strong id="revision-descartados">0</strong><strong id="revision-pendientes">0</strong>
        </div>
        ${gasto(0, "1200")}${gasto(1, "")}${gasto(2, "300")}
        <button type="submit" id="btn-confirmar">Confirmar y guardar</button>
      </form>
    `, { runScripts: "outside-only", url: "http://localhost/" });
    dom.window.HTMLElement.prototype.scrollIntoView = () => {};
    dom.window.eval(codigo);
    documento = dom.window.document;
  }

  afterEach(() => dom.window.close());

  function items() {
    return Array.from(documento.querySelectorAll(".app-gasto-propuesto"));
  }
  function nombres() {
    return items().map((g) => g.querySelector(".app-gasto-propuesto-nombre").textContent);
  }

  it("al entrar deja abierto solo el primero y muestra el progreso", () => {
    armar();

    expect(items().map((g) => g.classList.contains("is-cerrado"))).toEqual([false, true, true]);
    expect(documento.getElementById("revision-progreso").hidden).toBeFalse();
    expect(documento.getElementById("revision-pendientes").textContent).toBe("3");
    expect(documento.getElementById("btn-confirmar").textContent).toBe("Guardar 3 gastos");
    expect(items()[0].querySelector(".app-gasto-propuesto-importe").textContent).toBe("$ 1.200,00");
    expect(items()[0].querySelector(".app-gasto-propuesto-categoria").textContent).toBe("Delivery");
  });

  it("aceptar marca el tilde, achica el gasto, lo manda al final y abre el siguiente sin revisar", () => {
    armar();
    items()[0].querySelector(".app-gasto-propuesto-aceptar").click();

    expect(nombres()).toEqual(["Gasto 1", "Gasto 2", "Gasto 0"]);
    const aceptado = items()[2];

    expect(aceptado.classList.contains("is-cerrado")).toBeTrue();
    expect(aceptado.classList.contains("is-aceptado")).toBeTrue();
    expect(aceptado.querySelector(".app-incluir").checked).toBeTrue();
    expect(aceptado.querySelector(".app-incluir").name).toBe("gastos[0].incluir");
    expect(items()[0].classList.contains("is-cerrado")).toBeFalse();
    expect(documento.getElementById("revision-aceptados").textContent).toBe("1");
    expect(documento.getElementById("revision-pendientes").textContent).toBe("2");
  });

  it("descartar desmarca el tilde, deja de exigir los campos y sigue con el próximo", () => {
    armar();
    items()[0].querySelector(".app-gasto-propuesto-aceptar").click();
    // Gasto 1 quedó primero; al descartarlo pasa al final, detrás del aceptado
    items()[0].querySelector(".app-gasto-propuesto-descartar").click();

    expect(nombres()).toEqual(["Gasto 2", "Gasto 0", "Gasto 1"]);
    const descartado = items()[2];

    expect(descartado.classList.contains("is-descartado")).toBeTrue();
    expect(descartado.querySelector(".app-incluir").checked).toBeFalse();
    expect(descartado.querySelector("input[type='number']").required).toBeFalse();
    expect(items()[0].classList.contains("is-cerrado")).toBeFalse();
    expect(documento.getElementById("revision-descartados").textContent).toBe("1");
    expect(documento.getElementById("btn-confirmar").textContent).toBe("Guardar 2 gastos");
  });

  it("no acepta un gasto con un campo inválido: lo deja abierto", () => {
    armar();
    const segundo = items()[1];
    segundo.querySelector(".app-gasto-propuesto-resumen").click();
    segundo.querySelector(".app-gasto-propuesto-aceptar").click();

    expect(segundo.classList.contains("is-cerrado")).toBeFalse();
    expect(segundo.dataset.estado).toBeUndefined();
    expect(documento.getElementById("revision-aceptados").textContent).toBe("0");
  });

  it("la línea reabre un gasto decidido para corregirlo y la línea refleja lo corregido", () => {
    armar();
    const primero = items()[0];
    primero.querySelector(".app-gasto-propuesto-aceptar").click();
    primero.querySelector(".app-gasto-propuesto-resumen").click();

    expect(primero.classList.contains("is-cerrado")).toBeFalse();
    expect(items()[2]).toBe(primero);

    const importe = primero.querySelector("input[type='number']");
    importe.value = "999";
    importe.dispatchEvent(new dom.window.Event("input", { bubbles: true }));

    expect(primero.querySelector(".app-gasto-propuesto-importe").textContent).toBe("$ 999,00");
    expect(primero.classList.contains("is-aceptado")).toBeTrue();
  });

  it("con un error del servidor deja todos los gastos abiertos", () => {
    armar(true);

    expect(items().every((g) => !g.classList.contains("is-cerrado"))).toBeTrue();
  });
});
