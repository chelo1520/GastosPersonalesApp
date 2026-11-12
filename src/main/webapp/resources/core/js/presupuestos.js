/* global bootstrap */
(function () {
  const tabla = document.getElementById("tabla-presupuestos");
  if (!tabla) {
    return;
  }

  // Lápiz: muestra el campo del monto en la misma fila; cancelar vuelve a como estaba
  function editar(celda, activo) {
    const ver = celda.querySelector(".app-presupuesto-ver");
    const form = celda.querySelector(".app-presupuesto-editar");
    if (!ver || !form) {
      return;
    }
    ver.hidden = activo;
    form.hidden = !activo;
    const monto = form.querySelector("input[name='valor']");
    if (activo) {
      monto.focus();
      monto.select();
      equivalencia(form);
    } else {
      monto.value = monto.defaultValue;
      form.querySelector("select[name='tipo']").value =
        form.querySelector("select[name='tipo'] option[selected]")?.value || "pesos";
    }
  }

  // Modal de asignación: monto y porcentaje ligados, atajos y lo que queda sin asignar.
  // Si el modal no está en la página, el lápiz abre el formulario de la fila como antes
  const modalEl = document.getElementById("modal-presupuesto");
  const modal = modalEl ? armarModal(modalEl) : null;

  function armarModal(el) {
    const totalDelMes = Number(tabla.dataset.total) || 0;
    const sinAsignar = Number(tabla.dataset.sinAsignar) || 0;
    const formato = new Intl.NumberFormat("es-AR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    const titulo = el.querySelector("#modal-presupuesto-titulo");
    const categoriaId = el.querySelector("#modal-categoria-id");
    const tipo = el.querySelector("#modal-tipo");
    const valor = el.querySelector("#modal-valor");
    const monto = el.querySelector("#modal-monto");
    const porcentaje = el.querySelector("#modal-porcentaje");
    const queda = el.querySelector("#modal-queda");
    const error = el.querySelector("#modal-error");
    const form = el.querySelector("form");
    // Cuánto ya tiene esta categoría: al reemplazarlo, vuelve a quedar disponible
    let asignadoActual = 0;
    let ultimoEditado = "pesos";

    function redondear(numero) {
      return Math.round(numero * 100) / 100;
    }
    function actualizarQueda() {
      const montoNumero = Number(monto.value);
      if (!(totalDelMes > 0) || !(montoNumero > 0)) {
        queda.textContent = "";
        queda.classList.remove("is-neg");
        return;
      }
      const restante = redondear(sinAsignar + asignadoActual - montoNumero);
      queda.classList.toggle("is-neg", restante < 0);
      queda.textContent =
        restante < 0
          ? "Supera tu presupuesto del mes por $ " + formato.format(-restante)
          : "Te quedan $ " + formato.format(restante) + " sin asignar";
    }
    function desdePorcentaje() {
      const pct = Number(porcentaje.value);
      monto.value = pct > 0 && totalDelMes > 0 ? redondear((totalDelMes * pct) / 100) : "";
      ultimoEditado = "porcentaje";
      actualizarQueda();
    }
    function desdeMonto() {
      const montoNumero = Number(monto.value);
      porcentaje.value =
        montoNumero > 0 && totalDelMes > 0 ? redondear((montoNumero / totalDelMes) * 100) : "";
      ultimoEditado = "pesos";
      actualizarQueda();
    }
    porcentaje.addEventListener("input", desdePorcentaje);
    monto.addEventListener("input", desdeMonto);
    el.querySelectorAll(".app-atajo").forEach((atajo) =>
      atajo.addEventListener("click", () => {
        porcentaje.value = atajo.dataset.porcentaje;
        desdePorcentaje();
        monto.focus();
      }),
    );
    // Se guarda en la modalidad que se tocó al final: porcentaje si se eligió un %, pesos si se escribió un monto
    form.addEventListener("submit", () => {
      tipo.value = ultimoEditado;
      valor.value = ultimoEditado === "porcentaje" ? porcentaje.value : monto.value;
    });

    function mostrar() {
      if (typeof bootstrap !== "undefined") {
        bootstrap.Modal.getOrCreateInstance(el).show();
      } else {
        el.classList.add("show");
        el.style.display = "block";
        el.removeAttribute("aria-hidden");
      }
    }

    return {
      abrir(fila, mensajeDeError) {
        titulo.textContent = "Asignar presupuesto a " + (fila.dataset.nombre || "la categoría");
        categoriaId.value = fila.dataset.categoriaId || "";
        asignadoActual = Number(fila.dataset.presupuesto) || 0;
        const pctActual = fila.dataset.porcentaje;
        if (pctActual) {
          porcentaje.value = pctActual;
          desdePorcentaje();
        } else {
          monto.value = asignadoActual > 0 ? asignadoActual : "";
          desdeMonto();
        }
        error.hidden = !mensajeDeError;
        error.textContent = mensajeDeError || "";
        mostrar();
        (pctActual ? porcentaje : monto).focus();
      },
    };
  }

  tabla.addEventListener("click", (evento) => {
    const lapiz = evento.target.closest(".app-presupuesto-lapiz");
    if (lapiz) {
      if (modal) {
        modal.abrir(lapiz.closest("tr"));
      } else {
        editar(lapiz.closest("td"), true);
      }
      return;
    }
    const cancelar = evento.target.closest(".app-presupuesto-cancelar");
    if (cancelar) {
      editar(cancelar.closest("td"), false);
    }
  });

  // Mientras se escribe: un porcentaje muestra a cuántos pesos equivale, y un monto a qué % del total
  const total = Number(tabla.dataset.total) || 0;
  const pesos = new Intl.NumberFormat("es-AR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  function equivalencia(form) {
    const salida = form.querySelector(".app-presupuesto-equivale");
    if (!salida) {
      return;
    }
    const tipo = form.querySelector("select[name='tipo']").value;
    const valor = Number(form.querySelector("input[name='valor']").value);
    if (!(valor > 0) || !(total > 0)) {
      salida.textContent = "";
    } else if (tipo === "porcentaje") {
      salida.textContent = "= $ " + pesos.format(Math.round(total * valor) / 100);
    } else {
      salida.textContent = "= " + pesos.format(Math.round((valor / total) * 10000) / 100) + " % del total";
    }
  }
  tabla.addEventListener("input", (evento) => {
    const form = evento.target.closest(".app-presupuesto-editar");
    if (form) {
      equivalencia(form);
    }
  });

  tabla.addEventListener("keydown", (evento) => {
    if (evento.key === "Escape" && evento.target.closest(".app-presupuesto-editar")) {
      editar(evento.target.closest("td"), false);
    }
  });

  // Si el servidor rechazó un valor, la fila vuelve con su formulario abierto: se reabre en el modal
  // con el mismo error (Bootstrap se carga después de este script, por eso se espera al DOM)
  if (modal) {
    document.addEventListener("DOMContentLoaded", () => {
      const abierto = tabla.querySelector(".app-presupuesto-editar:not([hidden])");
      const alerta = document.querySelector(".alert-danger[role='alert']:not(#modal-error)");
      if (abierto && alerta) {
        modal.abrir(abierto.closest("tr"), alerta.textContent.trim());
      }
    });
  }

  // Ocultar las categorías sin presupuesto; la preferencia se recuerda en este navegador
  const ocultar = document.getElementById("ocultar-sin-presupuesto");
  const todoOculto = document.getElementById("presupuestos-todo-oculto");
  const CLAVE = "gastitos.presupuestos.ocultarSinPresupuesto";

  function aplicarFiltro() {
    const filas = Array.from(tabla.querySelectorAll("tbody tr"));
    let visibles = 0;
    filas.forEach((tr) => {
      const oculta = ocultar.checked && tr.classList.contains("app-presupuesto-sin-definir");
      tr.hidden = oculta;
      if (!oculta) {
        visibles++;
      }
    });
    if (todoOculto) {
      todoOculto.hidden = visibles > 0;
    }
  }

  if (ocultar) {
    try {
      ocultar.checked = localStorage.getItem(CLAVE) === "1";
    } catch {
      // Sin almacenamiento disponible: arranca mostrando todas
    }
    ocultar.addEventListener("change", () => {
      try {
        localStorage.setItem(CLAVE, ocultar.checked ? "1" : "0");
      } catch {
        // No se pudo recordar; el filtro igual se aplica
      }
      aplicarFiltro();
    });
    aplicarFiltro();
  }
})();
