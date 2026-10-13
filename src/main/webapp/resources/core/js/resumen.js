/* global Chart */
(function () {
  const canvas = document.getElementById("grafico-categorias");
  const tabla = document.getElementById("tabla-categorias");
  if (!canvas || !tabla || typeof Chart === "undefined") {
    return;
  }

  const filas = Array.from(tabla.querySelectorAll("tbody tr"));
  if (!filas.length) {
    return;
  }

  // Los datos salen de la tabla (ya renderizada por Thymeleaf)
  const labels = filas.map((tr) => tr.dataset.nombre);
  const valores = filas.map((tr) => parseFloat(tr.dataset.gastado) || 0);

  // El color de cada porción es el mismo que el del punto de la tabla
  const colores = filas.map((tr) => {
    const dot = tr.querySelector(".app-cat-dot");
    return dot ? getComputedStyle(dot).backgroundColor : "#9e9a92";
  });

  const fmt = (n) => "$ " + Math.round(n).toLocaleString("es-AR");

  new Chart(canvas, {
    type: "pie",
    data: {
      labels,
      datasets: [
        {
          data: valores,
          backgroundColor: colores,
          borderWidth: 0,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: true,
      plugins: {
        legend: { display: false }, // la tabla ya funciona como leyenda
        tooltip: {
          callbacks: {
            label: (ctx) => " " + ctx.label + ": " + fmt(ctx.parsed),
          },
        },
      },
    },
  });
})();

// Historial: barras apiladas por mes (hormiga abajo, el resto arriba) y una marca con el presupuesto
// de cada mes. Es una marca por barra y no una línea que los una: el presupuesto es un valor por mes
(function () {
  const canvas = document.getElementById("grafico-historial");
  const tabla = document.getElementById("tabla-historial");
  if (!canvas || !tabla || typeof Chart === "undefined") {
    return;
  }

  const filas = Array.from(tabla.querySelectorAll("tbody tr"));
  if (!filas.length) {
    return;
  }

  const numero = (valor) => parseFloat(valor) || 0;
  const meses = filas.map((tr) => ({
    etiqueta: tr.dataset.etiqueta,
    hormiga: numero(tr.dataset.hormiga),
    resto: numero(tr.dataset.resto),
    presupuesto: numero(tr.dataset.presupuesto),
    enCurso: tr.dataset.enCurso === "true",
  }));

  const VERDE = "#00907e";
  const NARANJA = "#c2500a";
  const TINTA = "#1b1b18";
  const SUPERFICIE = "#ffffff";

  // El mes en curso va más claro: todavía no cerró y su total va a seguir subiendo
  const color = (base) => meses.map((m) => (m.enCurso ? base + "73" : base));

  const fmt = (n) => "$ " + Math.round(n).toLocaleString("es-AR");
  const fmtEje = (n) =>
    "$ " + n.toLocaleString("es-AR", { notation: "compact", maximumFractionDigits: 1 });

  new Chart(canvas, {
    data: {
      labels: meses.map((m) => (m.enCurso ? [m.etiqueta, "(en curso)"] : m.etiqueta)),
      datasets: [
        {
          type: "bar",
          label: "Gastos hormiga",
          data: meses.map((m) => m.hormiga),
          backgroundColor: color(NARANJA),
          stack: "gastado",
          order: 1,
          maxBarThickness: 48,
          borderRadius: 0,
        },
        {
          type: "bar",
          label: "Resto de los gastos",
          data: meses.map((m) => m.resto),
          backgroundColor: color(VERDE),
          stack: "gastado",
          order: 1,
          maxBarThickness: 48,
          // 2px de separación con el segmento hormiga y la punta redondeada
          borderColor: SUPERFICIE,
          borderWidth: { bottom: 2 },
          borderSkipped: false,
          borderRadius: { topLeft: 4, topRight: 4 },
        },
        {
          type: "line",
          label: "Presupuesto del mes",
          data: meses.map((m) => m.presupuesto),
          showLine: false,
          pointStyle: "line",
          pointRadius: 18,
          pointHoverRadius: 18,
          pointBorderWidth: 3,
          pointHoverBorderWidth: 3,
          borderColor: TINTA,
          backgroundColor: TINTA,
          order: 0,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: "index", intersect: false },
      scales: {
        x: {
          stacked: true,
          grid: { display: false },
          ticks: { color: "#5c5b55" },
        },
        y: {
          stacked: true,
          beginAtZero: true,
          border: { display: false },
          grid: { color: "#ece9e2" },
          ticks: { color: "#5c5b55", callback: (valor) => fmtEje(valor), maxTicksLimit: 5 },
        },
      },
      plugins: {
        legend: {
          position: "bottom",
          labels: { usePointStyle: true, color: "#1b1b18", boxHeight: 8 },
        },
        tooltip: {
          callbacks: {
            title: (items) => {
              const m = meses[items[0].dataIndex];
              return m.etiqueta + (m.enCurso ? " (en curso)" : "");
            },
            label: (ctx) => " " + ctx.dataset.label + ": " + fmt(ctx.parsed.y),
            footer: (items) => {
              const m = meses[items[0].dataIndex];
              const gastado = m.hormiga + m.resto;
              const usado = m.presupuesto > 0 ? Math.round((gastado / m.presupuesto) * 100) : 0;
              return "Gastado: " + fmt(gastado) + " (" + usado + " % del presupuesto)";
            },
          },
        },
      },
    },
  });
})();
