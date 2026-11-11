import { readFileSync } from "node:fs";
import { JSDOM } from "jsdom";

const codigo = readFileSync(new URL("../presupuestos.js", import.meta.url), "utf8");

describe("Edición de presupuestos", () => {
  let dom;

  beforeEach(() => {
    dom = new JSDOM(`
      <table id="tabla-presupuestos"><tbody><tr><td>
        <div class="app-presupuesto-ver"><button class="app-presupuesto-lapiz">Editar</button></div>
        <form class="app-presupuesto-editar" hidden>
          <select name="tipo"><option value="pesos">Pesos</option><option value="porcentaje" selected>Porcentaje</option></select>
          <input name="valor" value="25">
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
});
