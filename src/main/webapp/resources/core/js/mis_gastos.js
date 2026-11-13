import {
  aplicarFiltros,
  cantidadDeGastos,
  etiquetaDelFiltro,
  formatearPesos,
  formatearPesosSinDecimales,
} from "./mis_gastos_filtros.js";

// "Mis gastos" (rediseño v2): conecta el mapa de calor, el buscador, el orden y las categorías del panel
// con la lista. Todo se resuelve en el navegador sobre los gastos del mes que mandó el servidor.
const raiz = document.getElementById("mis-gastos");
const grupos = [...document.querySelectorAll("[data-grupo-dia]")];
const listaPorImporte = document.getElementById("lista-por-importe");

if (raiz && grupos.length) {
  const numeroDeMes = Number(raiz.dataset.mesNumero);
  const filas = [...document.querySelectorAll(".app-mg-fila")];
  const casillas = [...document.querySelectorAll(".app-mg-dia-casilla[data-dia]")];
  const categorias = [...document.querySelectorAll(".app-mg-categoria")];
  const botonesDeOrden = [...document.querySelectorAll("[data-orden]")];
  const buscador = document.getElementById("buscar-gasto");
  const pista = document.getElementById("calor-pista");
  const sinCoincidencias = document.getElementById("sin-coincidencias");
  const limpiar = document.getElementById("limpiar-filtros");

  // Un gasto por fila de la lista por día (la lista por importe repite los mismos)
  const gastos = grupos.flatMap((grupo) =>
    [...grupo.querySelectorAll(".app-mg-fila")].map((fila) => ({
      id: Number(fila.dataset.id),
      dia: Number(fila.dataset.dia),
      categoria: fila.dataset.categoria,
      importe: Number(fila.dataset.importe),
      descripcion: fila.dataset.descripcion,
    })),
  );

  const filtros = { dia: null, categoria: null, texto: "" };
  let orden = "fecha";

  function escribirTotal(nodo, importe) {
    nodo.querySelector("span").textContent = formatearPesos(importe);
  }

  function mostrar() {
    const resultado = aplicarFiltros(gastos, filtros);
    const porImporte = orden === "importe";

    filas.forEach((fila) => {
      fila.hidden = !resultado.ids.has(Number(fila.dataset.id));
    });
    grupos.forEach((grupo) => {
      const total = resultado.porDia[Number(grupo.dataset.grupoDia)];
      grupo.hidden = porImporte || total === undefined;
      if (total !== undefined) {
        escribirTotal(grupo.querySelector("[data-total-grupo]"), total);
      }
    });
    listaPorImporte.hidden = !porImporte || resultado.cantidad === 0;
    escribirTotal(listaPorImporte.querySelector("[data-total-grupo]"), resultado.total);
    sinCoincidencias.hidden = resultado.cantidad > 0;

    document.getElementById("filtro-etiqueta").textContent = etiquetaDelFiltro(filtros, numeroDeMes);
    escribirTotal(document.getElementById("filtro-total"), resultado.total);
    document.getElementById("filtro-cantidad").textContent = cantidadDeGastos(resultado.cantidad);
    limpiar.hidden = !resultado.hayFiltros;

    categorias.forEach((boton) => {
      const datos = resultado.porCategoria[boton.dataset.categoria];
      const elegida = filtros.categoria === boton.dataset.categoria;
      boton.hidden = !datos && !elegida;
      boton.classList.toggle("is-activa", elegida);
      boton.setAttribute("aria-pressed", String(elegida));
      boton.querySelector("[data-monto]").textContent = formatearPesosSinDecimales(datos ? datos.total : 0);
      boton.querySelector("[data-barra]").style.width = `${datos ? datos.ancho : 0}%`;
    });

    casillas.forEach((casilla) => {
      const elegida = filtros.dia === Number(casilla.dataset.dia);
      casilla.classList.toggle("is-elegida", elegida);
      casilla.setAttribute("aria-pressed", String(elegida));
    });
    pista.textContent = filtros.dia
      ? `Mostrando el ${filtros.dia}/${numeroDeMes} · tocá de nuevo para quitar`
      : "Tocá un día para ver sus gastos";

    botonesDeOrden.forEach((boton) => {
      const activo = boton.dataset.orden === orden;
      boton.classList.toggle("is-activo", activo);
      boton.setAttribute("aria-pressed", String(activo));
    });
  }

  casillas.forEach((casilla) => {
    casilla.addEventListener("click", () => {
      const dia = Number(casilla.dataset.dia);
      filtros.dia = filtros.dia === dia ? null : dia;
      mostrar();
    });
  });

  categorias.forEach((boton) => {
    boton.addEventListener("click", () => {
      const categoria = boton.dataset.categoria;
      filtros.categoria = filtros.categoria === categoria ? null : categoria;
      mostrar();
    });
  });

  botonesDeOrden.forEach((boton) => {
    boton.addEventListener("click", () => {
      orden = boton.dataset.orden;
      mostrar();
    });
  });

  buscador.addEventListener("input", () => {
    filtros.texto = buscador.value;
    mostrar();
  });

  limpiar.addEventListener("click", () => {
    filtros.dia = null;
    filtros.categoria = null;
    filtros.texto = "";
    buscador.value = "";
    mostrar();
  });

  mostrar();
}
