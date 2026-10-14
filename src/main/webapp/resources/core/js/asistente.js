/* global Chart, bootstrap */
// Asistente ("Tu gasto real"): gráfico con puntos numerados, panel lateral y síntesis en segundo plano.
// Todo lo que se dibuja sale de la tabla de meses y del panel, ya renderizados por Thymeleaf.
(function () {
  const canvas = document.getElementById("grafico-gasto-real");
  const tabla = document.getElementById("tabla-meses");
  const toggle = document.getElementById("toggle-usd");
  const panelEl = document.getElementById("panel-asistente");

  // ---- Lecturas con ancla: se numeran en el panel, en el gráfico y en la tabla de categorías ----
  const lecturas = Array.from(document.querySelectorAll(".app-lectura")).filter(
    (el) => el.dataset.mes || el.dataset.categoria
  );
  const porMes = new Map();
  lecturas.forEach((el, i) => {
    const numero = i + 1;
    el.dataset.numero = String(numero);
    const badge = el.querySelector(".app-numero");
    if (badge) {
      badge.textContent = String(numero);
    }
    if (el.dataset.mes) {
      porMes.set(el.dataset.mes, { numero, el });
    }
    if (el.dataset.categoria) {
      const fila = document.querySelector('#tabla-categorias tr[data-categoria="' + CSS.escape(el.dataset.categoria) + '"]');
      if (fila) {
        fila.classList.add("is-con-lectura", "is-" + estiloDe(el));
        const nombre = fila.querySelector(".app-categoria-nombre");
        if (nombre) {
          const span = document.createElement("span");
          span.className = "app-numero";
          span.textContent = String(numero);
          span.setAttribute("aria-hidden", "true");
          nombre.prepend(span);
        }
      }
    }
  });

  function estiloDe(el) {
    return ["is-ok", "is-warn", "is-over"].find((c) => el.classList.contains(c)) || "is-ok";
  }

  const colores = { "is-ok": "#167a3e", "is-warn": "#c2500a", "is-over": "#b91c1c" };

  // ---- Panel lateral ----
  let panel = null;
  const esDesktop = () => window.matchMedia("(min-width: 992px)").matches;
  function elPanel() {
    if (!panel && panelEl && typeof bootstrap !== "undefined") {
      // En desktop sin fondo oscuro y con scroll: el gráfico sigue visible mientras se lee
      panel = new bootstrap.Offcanvas(panelEl, esDesktop() ? { backdrop: false, scroll: true } : {});
      panelEl.addEventListener("shown.bs.offcanvas", () => botonAbrir && botonAbrir.setAttribute("aria-expanded", "true"));
      panelEl.addEventListener("hidden.bs.offcanvas", () => botonAbrir && botonAbrir.setAttribute("aria-expanded", "false"));
    }
    return panel;
  }
  const botonAbrir = document.getElementById("abrir-panel");
  if (botonAbrir) {
    botonAbrir.addEventListener("click", () => elPanel() && elPanel().show());
  }

  function abrirLectura(el) {
    const p = elPanel();
    if (!p) {
      return;
    }
    p.show();
    const cuerpo = el.querySelector(".accordion-collapse");
    if (cuerpo && typeof bootstrap !== "undefined") {
      bootstrap.Collapse.getOrCreateInstance(cuerpo, { toggle: false }).show();
    }
    const boton = el.querySelector(".accordion-button");
    if (boton) {
      setTimeout(() => boton.focus(), 350);
    }
  }

  // ---- Síntesis en segundo plano: la página se arma con las reglas y acá se pide el párrafo ----
  const sintesis = document.querySelector(".app-sintesis");
  const parrafo = document.getElementById("veredicto-texto");
  const leerTodo = document.querySelector(".app-sintesis-leer");
  function revisarRecorte() {
    if (!parrafo || !leerTodo) {
      return;
    }
    const recortado = parrafo.classList.contains("is-recortada");
    leerTodo.hidden = recortado && parrafo.scrollHeight <= parrafo.clientHeight + 2;
  }
  if (leerTodo && parrafo) {
    leerTodo.addEventListener("click", () => {
      const abierto = parrafo.classList.toggle("is-recortada") === false;
      leerTodo.textContent = abierto ? "Ver menos" : "Leer todo";
      leerTodo.setAttribute("aria-expanded", String(abierto));
    });
  }
  if (sintesis && sintesis.dataset.sintesisUrl && parrafo) {
    fetch(sintesis.dataset.sintesisUrl, { headers: { Accept: "text/plain" } })
      .then((r) => (r.ok && r.status !== 204 ? r.text() : ""))
      .then((texto) => {
        if (texto && texto.trim()) {
          parrafo.textContent = texto.trim();
        }
      })
      .catch(() => {})
      .finally(() => {
        sintesis.classList.remove("is-escribiendo");
        const estado = sintesis.querySelector(".app-sintesis-estado");
        if (estado) {
          estado.remove();
        }
        revisarRecorte();
      });
  }
  if (panelEl) {
    panelEl.addEventListener("shown.bs.offcanvas", revisarRecorte);
  }

  // ---- "Ver las N categorías" ----
  const verTodas = document.getElementById("ver-todas-las-categorias");
  if (verTodas) {
    verTodas.addEventListener("click", () => {
      const tablaCat = document.getElementById("tabla-categorias");
      const abierta = tablaCat.classList.toggle("is-completa");
      verTodas.setAttribute("aria-expanded", String(abierta));
      verTodas.textContent = abierta ? "Ver menos" : verTodas.dataset.textoCerrado || verTodas.textContent;
      verTodas.dataset.textoCerrado = verTodas.dataset.textoCerrado || "Ver las " + tablaCat.querySelectorAll("tbody tr").length + " categorías";
    });
  }

  // ---- "Ver en la tabla": resalta la fila de la categoría ----
  document.querySelectorAll(".app-ver-en-tabla").forEach((boton) => {
    boton.addEventListener("click", () => {
      const fila = document.querySelector('#tabla-categorias tr[data-categoria="' + CSS.escape(boton.dataset.categoria) + '"]');
      if (!fila) {
        return;
      }
      if (fila.classList.contains("app-fila-extra") && verTodas && !document.getElementById("tabla-categorias").classList.contains("is-completa")) {
        verTodas.click();
      }
      if (!esDesktop() && elPanel()) {
        elPanel().hide();
      }
      resaltar(fila);
    });
  });

  function resaltar(el) {
    el.classList.add("is-resaltada");
    el.scrollIntoView({ behavior: "smooth", block: "center" });
    setTimeout(() => el.classList.remove("is-resaltada"), 3000);
  }

  // ---- Gráfico ----
  if (!canvas || !tabla || typeof Chart === "undefined") {
    return;
  }
  const filas = Array.from(tabla.querySelectorAll("tbody tr"));
  if (!filas.length) {
    return;
  }
  const labels = filas.map((tr) => tr.dataset.etiqueta);
  const mesesClave = filas.map((tr) => tr.dataset.mes);
  const nominal = filas.map((tr) => parseFloat(tr.dataset.nominal) || 0);
  const real = filas.map((tr) => parseFloat(tr.dataset.real) || 0);
  const usd = filas.map((tr) => parseFloat(tr.dataset.usd) || 0);
  const enCurso = filas.map((tr) => tr.dataset.enCurso === "true");

  const estilos = getComputedStyle(document.documentElement);
  const colorNominal = estilos.getPropertyValue("--app-cat-2").trim() || "#2e9183";
  const colorReal = estilos.getPropertyValue("--app-cat-1").trim() || "#0f4c47";
  const fmtArs = (n) => "$ " + Math.round(n).toLocaleString("es-AR");
  const fmtUsd = (n) => "USD " + Math.round(n).toLocaleString("es-AR");
  const segmentoEnCurso = (ctx) => (enCurso[ctx.p1DataIndex] ? [6, 4] : undefined);

  // Índice del mes → lectura numerada; el punto se dibuja sobre la línea "en pesos de hoy"
  const lecturaEnIndice = mesesClave.map((mes) => porMes.get(mes) || null);
  let resaltado = null;
  if (lecturaEnIndice.some(Boolean)) {
    const pista = document.getElementById("pista-grafico");
    if (pista) {
      pista.hidden = false;
    }
  }

  const puntosNumerados = {
    id: "puntosNumerados",
    afterDatasetsDraw(chart) {
      const meta = chart.getDatasetMeta(1).hidden ? chart.getDatasetMeta(0) : chart.getDatasetMeta(1);
      const ctx = chart.ctx;
      lecturaEnIndice.forEach((lectura, i) => {
        const punto = meta.data[i];
        if (!lectura || !punto) {
          return;
        }
        const color = colores[estiloDe(lectura.el)];
        ctx.save();
        if (resaltado === lectura.numero) {
          ctx.beginPath();
          ctx.arc(punto.x, punto.y, 17, 0, Math.PI * 2);
          ctx.strokeStyle = color;
          ctx.lineWidth = 2;
          ctx.stroke();
        }
        ctx.beginPath();
        ctx.arc(punto.x, punto.y, 11, 0, Math.PI * 2);
        ctx.fillStyle = color;
        ctx.fill();
        ctx.fillStyle = "#fff";
        ctx.font = "700 12px 'IBM Plex Sans', sans-serif";
        ctx.textAlign = "center";
        ctx.textBaseline = "middle";
        ctx.fillText(String(lectura.numero), punto.x, punto.y + 0.5);
        ctx.restore();
      });
    },
  };

  const grafico = new Chart(canvas, {
    type: "line",
    data: {
      labels,
      datasets: [
        { label: "Gastado", data: nominal, borderColor: colorNominal, backgroundColor: colorNominal, tension: 0.3, segment: { borderDash: segmentoEnCurso } },
        { label: "En pesos de hoy", data: real, borderColor: colorReal, backgroundColor: colorReal, tension: 0.3, segment: { borderDash: segmentoEnCurso } },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: "index", intersect: false },
      onHover(evento, elementos, chart) {
        const i = elementos.length ? elementos[0].index : -1;
        chart.canvas.style.cursor = i >= 0 && lecturaEnIndice[i] ? "pointer" : "default";
      },
      onClick(evento, elementos) {
        const i = elementos.length ? elementos[0].index : -1;
        if (i >= 0 && lecturaEnIndice[i]) {
          resaltado = lecturaEnIndice[i].numero;
          grafico.update();
          abrirLectura(lecturaEnIndice[i].el);
        }
      },
      plugins: {
        legend: { position: "bottom" },
        tooltip: {
          callbacks: {
            label: (ctx) => " " + ctx.dataset.label + ": " + (toggle && toggle.checked ? fmtUsd(ctx.parsed.y) : fmtArs(ctx.parsed.y)),
            footer: (items) => {
              const lectura = items.length ? lecturaEnIndice[items[0].dataIndex] : null;
              return lectura ? "Tocá para ver la lectura " + lectura.numero : "";
            },
          },
        },
      },
      scales: { y: { ticks: { callback: (v) => (toggle && toggle.checked ? fmtUsd(v) : fmtArs(v)) } } },
    },
    plugins: [puntosNumerados],
  });

  // "Ver en el gráfico": cierra el panel en mobile, resalta el punto y lleva al gráfico
  document.querySelectorAll(".app-ver-en-grafico").forEach((boton) => {
    const i = mesesClave.indexOf(boton.dataset.mes);
    if (i >= 0) {
      boton.textContent = "Ver " + labels[i].replace(" (en curso)", "") + " en el gráfico";
    }
    boton.addEventListener("click", () => {
      const lectura = porMes.get(boton.dataset.mes);
      resaltado = lectura ? lectura.numero : null;
      grafico.update();
      if (!esDesktop() && elPanel()) {
        elPanel().hide();
      }
      canvas.scrollIntoView({ behavior: "smooth", block: "center" });
    });
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
