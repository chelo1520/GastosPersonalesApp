// Revisión de un comprobante, estilo revisión de código: cada gasto es una línea con aceptar o
// descartar; al decidir se achica y se abre el siguiente sin revisar. El tilde "incluir" sigue siendo
// lo que viaja al servidor: aceptar lo marca y descartar lo desmarca. Sin JavaScript queda todo abierto.
(function () {
  const form = document.getElementById("form-comprobante");
  if (!form) {
    return;
  }
  const gastos = Array.from(form.querySelectorAll(".app-gasto-propuesto"));
  const progreso = document.getElementById("revision-progreso");
  const aceptadosNode = document.getElementById("revision-aceptados");
  const descartadosNode = document.getElementById("revision-descartados");
  const pendientesNode = document.getElementById("revision-pendientes");
  const confirmar = document.getElementById("btn-confirmar");
  const abrirTodo = form.dataset.abrirTodo === "true";

  function tilde(gasto) {
    return gasto.querySelector(".app-incluir");
  }
  function estado(gasto) {
    return gasto.dataset.estado || "pendiente";
  }

  // Los campos obligatorios solo se exigen si el gasto se va a guardar
  function actualizarRequeridos(gasto) {
    const incluido = tilde(gasto).checked;
    gasto.classList.toggle("is-excluido", !incluido);
    gasto.querySelectorAll(".app-gasto-propuesto-campos [required], .app-gasto-propuesto-campos [data-requerido]").forEach((campo) => {
      campo.dataset.requerido = "";
      campo.required = incluido;
    });
  }

  // La línea resume lo que se va a guardar: fecha, importe y categoría elegida
  function actualizarResumen(gasto) {
    const fecha = gasto.querySelector("input[type='date']");
    const importe = gasto.querySelector("input[type='number']");
    const categoria = gasto.querySelector("select");
    const nombre = gasto.querySelector(".app-gasto-propuesto-nombre");
    const descripcion = gasto.querySelector("input[type='text']");
    if (fecha && fecha.value) {
      const [, mes, dia] = fecha.value.split("-");
      gasto.querySelector(".app-gasto-propuesto-fecha").textContent = dia + "/" + mes;
    }
    if (importe) {
      const numero = Number(importe.value);
      gasto.querySelector(".app-gasto-propuesto-importe").textContent =
        numero > 0 ? "$ " + numero.toLocaleString("es-AR", { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : "$ —";
    }
    if (categoria) {
      const elegida = categoria.options[categoria.selectedIndex];
      gasto.querySelector(".app-gasto-propuesto-categoria").textContent =
        categoria.value && elegida ? elegida.textContent : "sin categoría";
    }
    if (descripcion && nombre && descripcion.value.trim()) {
      nombre.textContent = descripcion.value.trim();
    }
  }

  function abrir(gasto, abierto) {
    gasto.classList.toggle("is-cerrado", !abierto);
    gasto.querySelector(".app-gasto-propuesto-resumen").setAttribute("aria-expanded", String(abierto));
  }

  function actualizarProgreso() {
    const aceptados = gastos.filter((g) => estado(g) === "aceptado").length;
    const descartados = gastos.filter((g) => estado(g) === "descartado").length;
    const pendientes = gastos.length - aceptados - descartados;
    const aGuardar = gastos.filter((g) => tilde(g).checked).length;
    aceptadosNode.textContent = String(aceptados);
    descartadosNode.textContent = String(descartados);
    pendientesNode.textContent = String(pendientes);
    confirmar.textContent = aGuardar === 1 ? "Guardar 1 gasto" : "Guardar " + aGuardar + " gastos";
  }

  // Después de decidir, el siguiente sin revisar se abre y queda a la vista
  function siguientePendiente(desde) {
    const resto = gastos.slice(gastos.indexOf(desde) + 1).concat(gastos.slice(0, gastos.indexOf(desde)));
    const siguiente = resto.find((g) => estado(g) === "pendiente");
    if (siguiente) {
      abrir(siguiente, true);
      siguiente.scrollIntoView({ block: "nearest", behavior: "smooth" });
      siguiente.querySelector(".app-gasto-propuesto-aceptar").focus();
    } else {
      confirmar.focus();
    }
  }

  function decidir(gasto, nuevoEstado) {
    const incluir = nuevoEstado === "aceptado";
    tilde(gasto).checked = incluir;
    actualizarRequeridos(gasto);
    // No se acepta un gasto con campos inválidos: se queda abierto mostrando el error del navegador
    if (incluir) {
      const campos = Array.from(gasto.querySelectorAll(".app-gasto-propuesto-campos input, .app-gasto-propuesto-campos select"));
      const invalido = campos.find((campo) => !campo.checkValidity());
      if (invalido) {
        abrir(gasto, true);
        invalido.reportValidity();
        return;
      }
    }
    gasto.dataset.estado = nuevoEstado;
    gasto.classList.toggle("is-aceptado", incluir);
    gasto.classList.toggle("is-descartado", !incluir);
    actualizarResumen(gasto);
    abrir(gasto, false);
    actualizarProgreso();
    siguientePendiente(gasto);
  }

  gastos.forEach((gasto) => {
    gasto.querySelector(".app-gasto-propuesto-aceptar").addEventListener("click", () => decidir(gasto, "aceptado"));
    gasto.querySelector(".app-gasto-propuesto-descartar").addEventListener("click", () => decidir(gasto, "descartado"));
    // La línea abre y cierra los campos para corregir algo, sin cambiar la decisión
    gasto.querySelector(".app-gasto-propuesto-resumen").addEventListener("click", () => {
      abrir(gasto, gasto.classList.contains("is-cerrado"));
    });
    gasto.querySelectorAll(".app-gasto-propuesto-campos input, .app-gasto-propuesto-campos select").forEach((campo) =>
      campo.addEventListener("input", () => actualizarResumen(gasto)),
    );
    // El tilde sigue funcionando a mano, como siempre
    tilde(gasto).addEventListener("change", () => {
      gasto.dataset.estado = tilde(gasto).checked ? "aceptado" : "descartado";
      gasto.classList.toggle("is-aceptado", tilde(gasto).checked);
      gasto.classList.toggle("is-descartado", !tilde(gasto).checked);
      actualizarRequeridos(gasto);
      actualizarProgreso();
    });
    actualizarRequeridos(gasto);
    actualizarResumen(gasto);
  });

  // Al entrar: solo el primero abierto (o todos, si el servidor devolvió un error)
  gastos.forEach((gasto, indice) => abrir(gasto, abrirTodo || indice === 0));
  progreso.hidden = false;
  actualizarProgreso();
})();
