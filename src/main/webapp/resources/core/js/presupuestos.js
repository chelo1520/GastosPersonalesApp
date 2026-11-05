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
    const monto = form.querySelector("input[name='monto']");
    if (activo) {
      monto.focus();
      monto.select();
    } else {
      monto.value = monto.defaultValue;
    }
  }

  tabla.addEventListener("click", (evento) => {
    const lapiz = evento.target.closest(".app-presupuesto-lapiz");
    if (lapiz) {
      editar(lapiz.closest("td"), true);
      return;
    }
    const cancelar = evento.target.closest(".app-presupuesto-cancelar");
    if (cancelar) {
      editar(cancelar.closest("td"), false);
    }
  });

  tabla.addEventListener("keydown", (evento) => {
    if (evento.key === "Escape" && evento.target.closest(".app-presupuesto-editar")) {
      editar(evento.target.closest("td"), false);
    }
  });

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
