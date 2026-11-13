import {
  aplicarFiltros,
  cantidadDeGastos,
  coincide,
  etiquetaDelFiltro,
  formatearPesos,
  formatearPesosSinDecimales,
  normalizar,
} from "../mis_gastos_filtros.js";

describe("Filtros de Mis gastos", () => {
  const sinFiltros = { dia: null, categoria: null, texto: "" };
  const gastos = [
    { id: 1, dia: 12, categoria: "Café y snacks", importe: 2500, descripcion: "Café con medialunas" },
    { id: 2, dia: 12, categoria: "Alimentación", importe: 46200, descripcion: "Compra semanal" },
    { id: 3, dia: 10, categoria: "Café y snacks", importe: 3000, descripcion: "Cortado" },
    { id: 4, dia: 3, categoria: "Transporte", importe: 15000, descripcion: "Carga SUBE" },
  ];

  it("busca sin importar tildes ni mayúsculas", () => {
    expect(normalizar("  CAFÉ ")).toBe("cafe");

    expect(coincide(gastos[0], { ...sinFiltros, texto: "cafe" })).toBeTrue();
  });

  it("el texto también encuentra por categoría", () => {
    expect(coincide(gastos[2], { ...sinFiltros, texto: "snacks" })).toBeTrue();

    expect(coincide(gastos[3], { ...sinFiltros, texto: "snacks" })).toBeFalse();
  });

  it("sin filtros muestra todo el mes", () => {
    const resultado = aplicarFiltros(gastos, sinFiltros);

    expect(resultado.cantidad).toBe(4);
    expect(resultado.total).toBe(66700);
    expect(resultado.hayFiltros).toBeFalse();
    expect(resultado.porDia).toEqual({ 12: 48700, 10: 3000, 3: 15000 });
  });

  it("filtra por día y recalcula el total", () => {
    const resultado = aplicarFiltros(gastos, { ...sinFiltros, dia: 12 });

    expect([...resultado.ids]).toEqual([1, 2]);
    expect(resultado.total).toBe(48700);
    expect(resultado.hayFiltros).toBeTrue();
  });

  it("filtra por categoría pero el panel sigue mostrando todas las categorías", () => {
    const resultado = aplicarFiltros(gastos, { ...sinFiltros, categoria: "Café y snacks" });

    expect([...resultado.ids]).toEqual([1, 3]);
    expect(resultado.total).toBe(5500);
    expect(Object.keys(resultado.porCategoria).length).toBe(3);
  });

  it("las barras del panel son relativas a la categoría de más gasto y respetan el día", () => {
    const resultado = aplicarFiltros(gastos, { ...sinFiltros, dia: 12 });

    expect(resultado.porCategoria["Alimentación"]).toEqual({ total: 46200, ancho: 100 });
    expect(resultado.porCategoria["Café y snacks"]).toEqual({ total: 2500, ancho: 5 });
    expect(resultado.porCategoria.Transporte).toBeUndefined();
  });

  it("combina día, categoría y texto", () => {
    const resultado = aplicarFiltros(gastos, { dia: 12, categoria: "Café y snacks", texto: "medialunas" });

    expect([...resultado.ids]).toEqual([1]);
  });

  it("arma la etiqueta del panel según los filtros", () => {
    expect(etiquetaDelFiltro(sinFiltros, 11)).toBe("Total del mes");

    expect(etiquetaDelFiltro({ dia: 12, categoria: "Café y snacks", texto: " cafe " }, 11)).toBe(
      "Filtrado: Café y snacks · 12/11 · “cafe”",
    );
  });

  it("formatea importes y cantidades en castellano", () => {
    expect(formatearPesos(48700)).toBe("48.700,00");
    expect(formatearPesosSinDecimales(48700.4)).toBe("48.700");
    expect(cantidadDeGastos(1)).toBe("1 gasto");
    expect(cantidadDeGastos(3)).toBe("3 gastos");
  });
});
