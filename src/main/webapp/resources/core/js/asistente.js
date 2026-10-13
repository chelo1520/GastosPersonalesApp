/* global Chart */
(function () {
  const canvas = document.getElementById("grafico-gasto-real");
  const tabla = document.getElementById("tabla-meses");
  const toggle = document.getElementById("toggle-usd");
  if (!canvas || !tabla || typeof Chart === "undefined") {
    return;
  }

  // Los datos salen de la tabla (ya renderizada por Thymeleaf)
  const filas = Array.from(tabla.querySelectorAll("tbody tr"));
  if (!filas.length) {
    return;
  }

  const labels = filas.map((tr) => tr.dataset.etiqueta);
  const nominal = filas.map((tr) => parseFloat(tr.dataset.nominal) || 0);
  const real = filas.map((tr) => parseFloat(tr.dataset.real) || 0);
  const usd = filas.map((tr) => parseFloat(tr.dataset.usd) || 0);
  // El mes en curso va punteado: todavía no terminó
  const enCurso = filas.map((tr) => tr.dataset.enCurso === "true");

  const estilos = getComputedStyle(document.documentElement);
  const colorNominal = estilos.getPropertyValue("--app-cat-2").trim() || "#2e9183";
  const colorReal = estilos.getPropertyValue("--app-cat-1").trim() || "#0f4c47";

  const fmtArs = (n) => "$ " + Math.round(n).toLocaleString("es-AR");
  const fmtUsd = (n) => "USD " + Math.round(n).toLocaleString("es-AR");

  const segmentoEnCurso = (ctx) => (enCurso[ctx.p1DataIndex] ? [6, 4] : undefined);

  const grafico = new Chart(canvas, {
    type: "line",
    data: {
      labels,
      datasets: [
        {
          label: "Gastado",
          data: nominal,
          borderColor: colorNominal,
          backgroundColor: colorNominal,
          tension: 0.3,
          segment: { borderDash: segmentoEnCurso },
        },
        {
          label: "En pesos de hoy",
          data: real,
          borderColor: colorReal,
          backgroundColor: colorReal,
          tension: 0.3,
          segment: { borderDash: segmentoEnCurso },
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: "index", intersect: false },
      plugins: {
        legend: { position: "bottom" },
        tooltip: {
          callbacks: {
            label: (ctx) => " " + ctx.dataset.label + ": " + (toggle && toggle.checked ? fmtUsd(ctx.parsed.y) : fmtArs(ctx.parsed.y)),
          },
        },
      },
      scales: {
        y: {
          ticks: { callback: (v) => (toggle && toggle.checked ? fmtUsd(v) : fmtArs(v)) },
        },
      },
    },
  });

  if (toggle) {
    toggle.addEventListener("change", () => {
      const verUsd = toggle.checked;
      tabla.classList.toggle("is-usd", verUsd);
      // En dólares la brecha de la inflación no aplica: queda una sola línea
      grafico.data.datasets[0].data = verUsd ? usd : nominal;
      grafico.data.datasets[0].label = verUsd ? "Gastado en USD" : "Gastado";
      grafico.data.datasets[1].hidden = verUsd;
      grafico.update();
    });
  }
})();
