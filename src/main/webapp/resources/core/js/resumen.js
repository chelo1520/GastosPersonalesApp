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
