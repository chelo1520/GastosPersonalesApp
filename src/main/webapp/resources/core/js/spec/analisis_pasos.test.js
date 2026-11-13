import { secuenciar } from "../analisis_pasos.js";

describe("Pasos del modal de análisis", () => {
  it("marca los pasos de a uno, en orden, y al final avisa", () => {
    const programados = [];
    const marcados = [];
    let termino = false;

    secuenciar(3, {
      marcar: (paso) => marcados.push(paso),
      terminar: () => {
        termino = true;
      },
      intervalo: 100,
      programar: (fn, ms) => programados.push({ fn, ms }),
    });

    expect(programados.map((p) => p.ms)).toEqual([100, 200, 300, 800]);
    programados.forEach((p) => p.fn());

    expect(marcados).toEqual([1, 2, 3]);
    expect(termino).toBeTrue();
  });

  it("usa un intervalo de 600 ms por paso si no se indica otro", () => {
    const tiempos = [];

    secuenciar(2, { marcar: () => {}, terminar: () => {}, programar: (fn, ms) => tiempos.push(ms) });

    expect(tiempos).toEqual([600, 1200, 1700]);
  });
});
