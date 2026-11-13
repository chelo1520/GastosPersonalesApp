// Filtros de "Mis gastos" (rediseño v2): por día, por categoría y por texto, sobre los gastos del mes.
// Puro, sin DOM. gasto: { id, dia, categoria, importe, descripcion }
// filtros: { dia: número | null, categoria: string | null, texto: string }

const PESOS = new Intl.NumberFormat("es-AR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const PESOS_SIN_DECIMALES = new Intl.NumberFormat("es-AR", { maximumFractionDigits: 0 });

// Sin tildes y en minúscula: "cafe" encuentra "Café"
export function normalizar(texto = "") {
  return texto.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().trim();
}

export function coincide(gasto, filtros, { conCategoria = true } = {}) {
  if (filtros.dia && gasto.dia !== filtros.dia) {
    return false;
  }
  if (conCategoria && filtros.categoria && gasto.categoria !== filtros.categoria) {
    return false;
  }
  const buscado = normalizar(filtros.texto);
  return !buscado || normalizar(gasto.descripcion).includes(buscado) || normalizar(gasto.categoria).includes(buscado);
}

function sumar(gastos) {
  return gastos.reduce((total, gasto) => total + gasto.importe, 0);
}

// "Total del mes" sin filtros; si no, "Filtrado: Café y snacks · 12/11 · “cafe”"
export function etiquetaDelFiltro(filtros, numeroDeMes) {
  const texto = (filtros.texto || "").trim();
  const partes = [filtros.categoria, filtros.dia ? `${filtros.dia}/${numeroDeMes}` : null, texto ? `“${texto}”` : null];
  const activas = partes.filter(Boolean);
  return activas.length ? `Filtrado: ${activas.join(" · ")}` : "Total del mes";
}

export function cantidadDeGastos(cantidad) {
  return cantidad === 1 ? "1 gasto" : `${cantidad} gastos`;
}

export function formatearPesos(importe) {
  return PESOS.format(importe);
}

export function formatearPesosSinDecimales(importe) {
  return PESOS_SIN_DECIMALES.format(importe);
}

// Lo que muestra la pantalla con estos filtros. Las categorías del panel no se filtran por categoría
// (así se puede pasar de una a otra), pero sí por día y texto; la barra es relativa a la de más gasto.
export function aplicarFiltros(gastos, filtros) {
  const visibles = gastos.filter((g) => coincide(g, filtros));
  const base = gastos.filter((g) => coincide(g, filtros, { conCategoria: false }));

  const porDia = {};
  visibles.forEach((g) => {
    porDia[g.dia] = (porDia[g.dia] || 0) + g.importe;
  });

  const totales = {};
  base.forEach((g) => {
    totales[g.categoria] = (totales[g.categoria] || 0) + g.importe;
  });
  const maximo = Math.max(0, ...Object.values(totales));
  const porCategoria = {};
  Object.entries(totales).forEach(([categoria, total]) => {
    porCategoria[categoria] = { total, ancho: maximo > 0 ? Math.round((total * 100) / maximo) : 0 };
  });

  return {
    ids: new Set(visibles.map((g) => g.id)),
    total: sumar(visibles),
    cantidad: visibles.length,
    porDia,
    porCategoria,
    hayFiltros: Boolean(filtros.dia || filtros.categoria || normalizar(filtros.texto)),
  };
}
