import { calcularPresupuesto, formatearPesos } from "../presupuesto_funciones.js";

describe("Cálculo del presupuesto por defecto", () => {
  it("multiplica el ingreso por el porcentaje y redondea a centavos", () => {
    expect(calcularPresupuesto("1100000", "50")).toBe(550000);
    expect(calcularPresupuesto(1000.01, 33.33)).toBe(333.3);
  });

  it("devuelve 0 cuando falta el ingreso o el porcentaje, o no son números", () => {
    expect(calcularPresupuesto("", "100")).toBe(0);
    expect(calcularPresupuesto("1000", "")).toBe(0);
    expect(calcularPresupuesto("abc", "50")).toBe(0);
    expect(calcularPresupuesto("-5", "50")).toBe(0);
  });

  it("formatea en pesos argentinos con dos decimales", () => {
    expect(formatearPesos(550000)).toBe("550.000,00");
    expect(formatearPesos(1234567.5)).toBe("1.234.567,50");
    expect(formatearPesos(0)).toBe("0,00");
  });
});
