import { readFileSync } from "node:fs";
import { JSDOM } from "jsdom";

const codigo = readFileSync(new URL("../presupuestos.js", import.meta.url), "utf8");

describe("Edición de presupuestos", () => {
  let dom;

  beforeEach(() => {
    dom = new JSDOM(`
      <table id="tabla-presupuestos" data-total="400000"><tbody><tr><td>
        <div class="app-presupuesto-ver"><button class="app-presupuesto-lapiz">Editar</button></div>
        <form class="app-presupuesto-editar" hidden>
          <select name="tipo"><option value="pesos">Pesos</option><option value="porcentaje" selected>Porcentaje</option></select>
          <input name="valor" value="25">
          <small class="app-presupuesto-equivale"></small>
          <button type="button" class="app-presupuesto-cancelar">Cancelar</button>
        </form>
      </td></tr></tbody></table>
    `, { runScripts: "outside-only", url: "http://localhost/" });
    dom.window.eval(codigo);
  });

  afterEach(() => dom.window.close());

  it("abre el editor y al cancelar restaura el valor y la modalidad guardados", () => {
    const documento = dom.window.document;
    const formulario = documento.querySelector("form");
    documento.querySelector(".app-presupuesto-lapiz").click();

    expect(formulario.hidden).toBeFalse();
    expect(documento.activeElement.name).toBe("valor");
    formulario.querySelector("input").value = "200";
    formulario.querySelector("select").value = "pesos";
    formulario.querySelector(".app-presupuesto-cancelar").click();

    expect(formulario.hidden).toBeTrue();
    expect(formulario.querySelector("input").value).toBe("25");
    expect(formulario.querySelector("select").value).toBe("porcentaje");
    expect(documento.querySelector(".app-presupuesto-ver").hidden).toBeFalse();
  });

  it("muestra en vivo a cuántos pesos equivale un porcentaje y a qué porcentaje equivale un monto", () => {
    const documento = dom.window.document;
    const formulario = documento.querySelector("form");
    const equivale = formulario.querySelector(".app-presupuesto-equivale");
    documento.querySelector(".app-presupuesto-lapiz").click();

    expect(equivale.textContent).toBe("= $ 100.000,00");

    formulario.querySelector("select").value = "pesos";
    formulario.querySelector("input").value = "50000";
    formulario.querySelector("input").dispatchEvent(new dom.window.Event("input", { bubbles: true }));

    expect(equivale.textContent).toBe("= 12,50 % del total");

    formulario.querySelector("input").value = "";
    formulario.querySelector("input").dispatchEvent(new dom.window.Event("input", { bubbles: true }));

    expect(equivale.textContent).toBe("");
  });
});

describe("Modal de asignación por categoría", () => {
  let dom;
  let documento;

  beforeEach(() => {
    dom = new JSDOM(`
      <div class="alert alert-danger" role="alert" hidden></div>
      <table id="tabla-presupuestos" data-total="400000" data-sin-asignar="100000" data-mes-nombre="Noviembre 2026"><tbody>
        <tr data-categoria-id="7" data-nombre="Delivery" data-presupuesto="40000" data-porcentaje="10"><td>
          <div class="app-presupuesto-ver"><button class="app-presupuesto-lapiz">Editar</button></div>
          <form class="app-presupuesto-editar" hidden>
            <select name="tipo"><option value="pesos">Pesos</option><option value="porcentaje" selected>Porcentaje</option></select>
            <input name="valor" value="10">
          </form>
        </td></tr>
      </tbody></table>
      <div class="modal" id="modal-presupuesto" aria-hidden="true">
        <form id="modal-presupuesto-form">
          <input type="hidden" name="categoriaId" id="modal-categoria-id">
          <input type="hidden" name="tipo" id="modal-tipo" value="pesos">
          <input type="hidden" name="valor" id="modal-valor">
          <h2 id="modal-presupuesto-titulo"></h2>
          <p id="modal-error" hidden></p>
          <input type="number" id="modal-monto">
          <input type="number" id="modal-porcentaje">
          <button type="button" class="app-atajo" data-porcentaje="20">20 %</button>
          <p id="modal-queda"></p>
          <button type="submit" id="modal-guardar">Guardar</button>
        </form>
      </div>
    `, { runScripts: "outside-only", url: "http://localhost/" });
    dom.window.eval(codigo);
    documento = dom.window.document;
  });

  afterEach(() => dom.window.close());

  function escribir(input, valor) {
    input.value = valor;
    input.dispatchEvent(new dom.window.Event("input", { bubbles: true }));
  }

  it("el lápiz abre el modal precargado con la categoría y su porcentaje actual", () => {
    documento.querySelector(".app-presupuesto-lapiz").click();

    const modal = documento.getElementById("modal-presupuesto");
    expect(modal.classList.contains("show")).toBeTrue();
    expect(documento.getElementById("modal-presupuesto-titulo").textContent).toBe("Asignar presupuesto a Delivery");
    expect(documento.getElementById("modal-categoria-id").value).toBe("7");
    expect(documento.getElementById("modal-porcentaje").value).toBe("10");
    expect(documento.getElementById("modal-monto").value).toBe("40000");
    expect(documento.getElementById("modal-queda").textContent).toBe("Te quedan $ 100.000,00 sin asignar");
    expect(documento.querySelector(".app-presupuesto-editar").hidden).toBeTrue();
  });

  it("liga monto y porcentaje en los dos sentidos y avisa cuando se pasa del total", () => {
    documento.querySelector(".app-presupuesto-lapiz").click();
    const monto = documento.getElementById("modal-monto");
    const porcentaje = documento.getElementById("modal-porcentaje");

    escribir(porcentaje, "25");
    expect(monto.value).toBe("100000");

    escribir(monto, "50000");
    expect(porcentaje.value).toBe("12.5");
    expect(documento.getElementById("modal-queda").textContent).toBe("Te quedan $ 90.000,00 sin asignar");

    escribir(monto, "200000");
    const queda = documento.getElementById("modal-queda");
    expect(queda.textContent).toBe("Supera tu presupuesto del mes por $ 60.000,00");
    expect(queda.classList.contains("is-neg")).toBeTrue();
  });

  it("un atajo completa los dos campos y al guardar manda la modalidad que se tocó al final", () => {
    documento.querySelector(".app-presupuesto-lapiz").click();
    const form = documento.getElementById("modal-presupuesto-form");

    documento.querySelector(".app-atajo").click();
    expect(documento.getElementById("modal-porcentaje").value).toBe("20");
    expect(documento.getElementById("modal-monto").value).toBe("80000");
    form.dispatchEvent(new dom.window.Event("submit", { cancelable: true }));
    expect(documento.getElementById("modal-tipo").value).toBe("porcentaje");
    expect(documento.getElementById("modal-valor").value).toBe("20");

    escribir(documento.getElementById("modal-monto"), "33000");
    form.dispatchEvent(new dom.window.Event("submit", { cancelable: true }));
    expect(documento.getElementById("modal-tipo").value).toBe("pesos");
    expect(documento.getElementById("modal-valor").value).toBe("33000");
  });
});
