import { readFileSync } from "node:fs";
import { JSDOM } from "jsdom";

const codigo = readFileSync(new URL("../guia.js", import.meta.url), "utf8");

describe("Guía de bienvenida", () => {
  let dom;

  afterEach(() => {
    dom.window.close();
  });

  function iniciar(contenido) {
    dom = new JSDOM(contenido, { runScripts: "outside-only", url: "http://localhost/gastitos/perfil" });
    dom.window.HTMLElement.prototype.scrollIntoView = () => {};
    dom.window.eval(codigo);
    dom.window.document.dispatchEvent(new dom.window.Event("DOMContentLoaded"));
    return dom.window.document;
  }

  it("resalta el presupuesto y enfoca el ingreso", () => {
    const documento = iniciar(`
      <section id="guia-pasos" data-paso="1" data-perfil="/gastitos/perfil"></section>
      <section id="guia-destino-perfil"><input id="ingresoMensual"></section>
    `);

    expect(documento.body.classList.contains("app-guia-activa")).toBeTrue();
    expect(documento.getElementById("guia-destino-perfil").classList.contains("app-guia-destacado")).toBeTrue();
    expect(documento.activeElement.id).toBe("ingresoMensual");
  });

  it("enfoca el lápiz de una categoría visible", () => {
    const documento = iniciar(`
      <section id="guia-pasos" data-paso="2" data-presupuestos="/gastitos/presupuestos"></section>
      <section id="guia-destino-categorias"><table><tbody>
        <tr hidden><td><button class="app-presupuesto-lapiz"></button></td></tr>
        <tr><td><button class="app-presupuesto-lapiz" id="editar"></button></td></tr>
      </tbody></table></section>
    `);

    expect(documento.activeElement.id).toBe("editar");
  });

  for (const [paso, destino, url] of [
    ["6", "simulacion", "/gastitos/simulacion"],
    ["7", "asistente", "/gastitos/asistente"],
  ]) {
    it(`resalta ${destino} sin enfocar controles`, () => {
      const documento = iniciar(`
        <section id="guia-pasos" data-paso="${paso}" data-${destino}="${url}"></section>
        <section id="guia-destino-${destino}"><button>Acción opcional</button></section>
      `);

      expect(documento.body.classList.contains("app-guia-activa")).toBeTrue();
      expect(documento.getElementById(`guia-destino-${destino}`).classList.contains("app-guia-destacado")).toBeTrue();
      expect(documento.activeElement).toBe(documento.body);
    });
  }
});
