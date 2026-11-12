(function () {
  const root = document.getElementById("simulacion");
  if (!root) return;

  const lista = document.getElementById("sim-lista");
  const periodos = document.getElementById("sim-periodos");
  const presupuesto = parseFloat(root.dataset.presupuesto) || 0;
  const estadoGuardado = document.getElementById("sim-guardado");
  const reintentar = document.getElementById("sim-reintentar");
  let version = 0;
  let versionGuardada = 0;
  let guardando = null;

  function estadoActual() {
    const items = Array.from(lista.querySelectorAll(".app-sim-item"));
    const campo = (id) => document.getElementById(id).value;
    return {
      excluidos: items.filter((item) => item.dataset.id && !item.querySelector(".app-sim-check").checked)
        .map((item) => Number(item.dataset.id)),
      agregados: items.filter((item) => item.dataset.tipo).map((item) => ({
        descripcion: item.dataset.descripcion,
        importe: Number(item.dataset.importe),
        tipo: item.dataset.tipo,
        incluido: item.querySelector(".app-sim-check").checked,
      })),
      descripcionExtra: campo("sim-extra-descripcion"),
      montoExtra: campo("sim-extra-monto"),
      descripcionOrdinario: campo("sim-ordinario-descripcion"),
      montoOrdinario: campo("sim-ordinario-monto"),
    };
  }

  function guardar() {
    if (guardando) return guardando;
    guardando = (async () => {
      try {
        while (versionGuardada < version) {
          const versionEnviada = version;
          estadoGuardado.textContent = "Guardando cambios…";
          const respuesta = await fetch(root.dataset.guardarUrl, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(estadoActual()),
          });
          if (!respuesta.ok) throw new Error("HTTP " + respuesta.status);
          versionGuardada = versionEnviada;
        }
        estadoGuardado.textContent = "Cambios guardados";
        reintentar.hidden = true;
        return true;
      } catch (error) {
        console.error("No se pudo guardar la simulación", error);
        estadoGuardado.textContent = "No se pudieron guardar los cambios. Reintentá antes de salir.";
        reintentar.hidden = false;
        return false;
      } finally {
        guardando = null;
      }
    })();
    return guardando;
  }

  function cambio() {
    version += 1;
    guardar();
  }

  reintentar.addEventListener("click", guardar);
  window.addEventListener("beforeunload", (evento) => {
    if (versionGuardada < version) {
      evento.preventDefault();
      evento.returnValue = "";
    }
  });
  document.addEventListener("click", async (evento) => {
    const enlace = evento.target.closest("a[href]");
    if (!enlace || evento.defaultPrevented || evento.button !== 0 || evento.ctrlKey ||
      evento.metaKey || evento.shiftKey || evento.altKey || enlace.target || enlace.hasAttribute("download") ||
      versionGuardada === version) return;
    evento.preventDefault();
    if (await guardar()) window.location.assign(enlace.href);
  });

  if (periodos) {
    periodos.addEventListener("submit", async (evento) => {
      if (versionGuardada === version) return;
      evento.preventDefault();
      if (await guardar()) periodos.submit();
      else periodos.reset();
    });
    periodos.addEventListener("change", (evento) => {
      if (evento.target.name === "mesBase") {
        const simulado = periodos.querySelector("[name=mesSimulado]");
        if (simulado.value < periodos.dataset.mesActual) simulado.value = periodos.dataset.mesActual;
      }
      periodos.requestSubmit();
    });
  }

  const fmt = (n) => "$ " + Math.round(n).toLocaleString("es-AR");

  const el = {
    total: document.getElementById("sim-total"),
    presupuesto: document.getElementById("sim-presupuesto"),
    diferencia: document.getElementById("sim-diferencia"),
    label: document.getElementById("sim-diferencia-label"),
    valor: document.getElementById("sim-diferencia-valor"),
    barra: document.getElementById("sim-barra"),
    fill: document.getElementById("sim-barra-fill"),
    sugerencia: document.getElementById("sim-sugerencia"),
  };

  function recalcular() {
    const items = lista ? Array.from(lista.querySelectorAll(".app-sim-item")) : [];
    let total = 0;
    let mayorExtra = null;

    items.forEach((li) => {
      const marcado = li.querySelector(".app-sim-check").checked;
      if (!marcado) return;
      const importe = parseFloat(li.dataset.importe) || 0;
      total += importe;
      if (li.dataset.extraordinario === "true" && (!mayorExtra || importe > mayorExtra.importe)) {
        mayorExtra = { importe, descripcion: li.dataset.descripcion };
      }
    });

    const diff = total - presupuesto;
    // Sin presupuesto, cualquier gasto ya lo excede: barra llena
    const pct = presupuesto > 0 ? (total / presupuesto) * 100 : (total > 0 ? 100 : 0);

    el.total.textContent = fmt(total);
    el.presupuesto.textContent = fmt(presupuesto);
    el.diferencia.classList.toggle("is-ok", diff <= 0);
    // Acentos como escapes Unicode: el JS se sirve sin charset y el navegador los rompe
    el.label.textContent = diff > 0 ? "Te pasar\u00edas por" : "Te sobrar\u00edan";
    el.valor.textContent = fmt(Math.abs(diff));
    el.fill.style.width = Math.min(pct, 100) + "%";
    el.barra.setAttribute("aria-valuenow", Math.min(Math.round(pct), 100));

    if (mayorExtra && presupuesto > 0) {
      const sin = total - mayorExtra.importe;
      const pctSin = Math.round((sin / presupuesto) * 100);
      const nombre = "\u00ab" + mayorExtra.descripcion + "\u00bb";
      el.sugerencia.textContent = "Desmarcando " + nombre + " quedar\u00edas en " + fmt(sin) + " (" + pctSin + " %).";
      el.sugerencia.hidden = false;
    } else {
      el.sugerencia.hidden = true;
    }
  }

  const TIPOS = {
    extraordinario: { tag: "Extraordinario", categoria: "Extraordinario" },
    ordinario: { tag: "Nuevo", categoria: "Ordinario" },
  };

  function agregarGasto(descripcion, monto, tipo) {
    const li = document.createElement("li");
    li.className = "app-sim-item";
    li.dataset.importe = monto;
    li.dataset.descripcion = descripcion;
    li.dataset.extraordinario = String(tipo === "extraordinario");
    li.dataset.tipo = tipo;

    const label = document.createElement("label");
    label.className = "app-sim-item-label";

    const check = document.createElement("input");
    check.type = "checkbox";
    check.className = "app-sim-check";
    check.checked = true;

    const desc = document.createElement("span");
    desc.className = "app-sim-descripcion";
    const texto = document.createElement("span");
    texto.textContent = descripcion;
    const tag = document.createElement("span");
    tag.className = "app-sim-tag";
    tag.textContent = TIPOS[tipo].tag;
    desc.append(texto, tag);

    label.append(check, desc);

    const cat = document.createElement("span");
    cat.className = "app-sim-categoria";
    cat.textContent = TIPOS[tipo].categoria;

    const imp = document.createElement("span");
    imp.className = "app-sim-importe";
    imp.textContent = Math.round(monto).toLocaleString("es-AR");

    li.append(label, cat, imp);
    lista.appendChild(li);

    // Si el mes base estaba vacío, el aviso deja de aplicar con el primer gasto agregado
    const vacio = document.getElementById("sim-vacio");
    if (vacio) vacio.hidden = true;
  }

  if (lista) {
    lista.addEventListener("change", (e) => {
      if (e.target.classList.contains("app-sim-check")) {
        recalcular();
        cambio();
      }
    });
  }

  document.querySelectorAll(".app-sim-form").forEach((form) => {
    form.addEventListener("submit", (e) => {
      e.preventDefault();
      const descInput = form.querySelector(".app-sim-input-descripcion");
      const montoInput = form.querySelector(".app-sim-input-monto");
      const descripcion = descInput.value.trim();
      const monto = parseFloat(montoInput.value);
      if (!descripcion || !(monto > 0) || !lista) return;

      agregarGasto(descripcion, monto, form.dataset.tipo);
      descInput.value = "";
      montoInput.value = "";
      descInput.focus();
      recalcular();
      cambio();
    });
    form.addEventListener("input", cambio);
  });

  recalcular();
})();