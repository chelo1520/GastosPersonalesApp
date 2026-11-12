import { readFileSync } from "node:fs";
import { JSDOM } from "jsdom";

const codigo = readFileSync(new URL("../simulacion.js", import.meta.url), "utf8");

describe("Guardado de simulación", () => {
  let dom;
  let documento;
  let periodos;
  let fetch;

  beforeEach(() => {
    dom = new JSDOM(`
      <form id="sim-periodos" data-mes-actual="2026-11">
        <select id="mes-base" name="mesBase"><option value="anterior" selected>Anterior</option><option value="actual">Actual</option></select>
        <select name="mesSimulado"><option value="2026-10" selected>Pasado</option><option value="2026-11">Actual</option></select>
      </form>
      <div id="simulacion" data-presupuesto="100" data-guardar-url="/guardar">
        <ul id="sim-lista"><li class="app-sim-item" data-id="12" data-importe="50"><input class="app-sim-check" type="checkbox" checked></li></ul>
        <form class="app-sim-form" data-tipo="ordinario"><input id="sim-ordinario-descripcion" class="app-sim-input-descripcion"><input id="sim-ordinario-monto" class="app-sim-input-monto"></form>
        <form class="app-sim-form" data-tipo="extraordinario"><input id="sim-extra-descripcion" class="app-sim-input-descripcion"><input id="sim-extra-monto" class="app-sim-input-monto"></form>
        <span id="sim-total"></span><span id="sim-presupuesto"></span>
        <div id="sim-diferencia"><span id="sim-diferencia-label"></span><span id="sim-diferencia-valor"></span></div>
        <div id="sim-barra"><div id="sim-barra-fill"></div></div><p id="sim-sugerencia"></p>
        <p id="sim-guardado"></p><button id="sim-reintentar" hidden></button>
      </div>
    `, { runScripts: "outside-only", url: "http://localhost/" });
    documento = dom.window.document;
    periodos = documento.getElementById("sim-periodos");
    fetch = jasmine.createSpy("fetch").and.resolveTo({ ok: true });
    dom.window.fetch = fetch;
    dom.window.eval(codigo);
  });

  afterEach(() => dom.window.close());

  it("guarda los cambios y los borradores", async () => {
    const check = documento.querySelector(".app-sim-check");
    check.checked = false;
    check.dispatchEvent(new dom.window.Event("change", { bubbles: true }));
    documento.getElementById("sim-extra-descripcion").value = "Borrador";
    documento.getElementById("sim-extra-descripcion").dispatchEvent(new dom.window.Event("input", { bubbles: true }));
    await new Promise((resolve) => setTimeout(resolve, 0));
    const estado = JSON.parse(fetch.calls.mostRecent().args[1].body);

    expect(estado.excluidos).toEqual([12]);
    expect(estado.descripcionExtra).toBe("Borrador");
    expect(documento.getElementById("sim-guardado").textContent).toBe("Cambios guardados");
  });

  it("guarda los gastos agregados sin convertirlos en gastos reales", async () => {
    const form = documento.querySelector("[data-tipo=extraordinario]");
    documento.getElementById("sim-extra-descripcion").value = "Regalo";
    documento.getElementById("sim-extra-monto").value = "300";
    form.dispatchEvent(new dom.window.Event("submit", { bubbles: true, cancelable: true }));
    await new Promise((resolve) => setTimeout(resolve, 0));
    const estado = JSON.parse(fetch.calls.mostRecent().args[1].body);

    expect(estado.agregados).toEqual([{ descripcion: "Regalo", importe: 300, tipo: "extraordinario", incluido: true }]);
    expect(estado.descripcionExtra).toBe("");
  });

  it("espera al guardado antes de cambiar de meses", async () => {
    let resolver;
    fetch.and.returnValue(new Promise((resolve) => { resolver = resolve; }));
    const enviar = spyOn(periodos, "submit");
    documento.querySelector(".app-sim-check").dispatchEvent(new dom.window.Event("change", { bubbles: true }));
    periodos.dispatchEvent(new dom.window.Event("submit", { bubbles: true, cancelable: true }));

    expect(enviar).not.toHaveBeenCalled();
    resolver({ ok: true });
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(enviar).toHaveBeenCalledWith();
  });

  it("cambia al mes actual al elegir otra base desde una simulación pasada", () => {
    const enviar = spyOn(periodos, "requestSubmit");
    const base = documento.getElementById("mes-base");
    base.value = "actual";
    base.dispatchEvent(new dom.window.Event("change", { bubbles: true }));

    expect(periodos.querySelector("[name=mesSimulado]").value).toBe("2026-11");
    expect(enviar).toHaveBeenCalledWith();
  });

  it("informa errores, impide cambiar de meses y permite reintentar", async () => {
    fetch.and.resolveTo({ ok: false, status: 500 });
    spyOn(dom.window.console, "error");
    const enviar = spyOn(periodos, "submit");
    documento.querySelector(".app-sim-check").dispatchEvent(new dom.window.Event("change", { bubbles: true }));
    periodos.dispatchEvent(new dom.window.Event("submit", { bubbles: true, cancelable: true }));
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(enviar).not.toHaveBeenCalled();
    expect(documento.getElementById("sim-reintentar").hidden).toBeFalse();
    fetch.and.resolveTo({ ok: true });
    documento.getElementById("sim-reintentar").click();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(documento.getElementById("sim-guardado").textContent).toBe("Cambios guardados");
  });
});
