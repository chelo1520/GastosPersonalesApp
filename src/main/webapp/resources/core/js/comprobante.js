// Revisión de un comprobante con varios gastos: un gasto destildado no se valida ni se guarda, y la
// suma de los elegidos se compara contra el total del comprobante mientras se edita.
(function () {
  const form = document.getElementById("form-comprobante");
  if (!form) {
    return;
  }
  const totalInput = document.getElementById("total-comprobante");
  const suma = document.getElementById("suma-elegida");
  const aviso = document.getElementById("aviso-suma");
  const total = totalInput ? parseFloat(totalInput.value) : NaN;
  // Igual que ConfirmacionComprobante.TOLERANCIA_TOTAL
  const TOLERANCIA = 1;
  const formato = new Intl.NumberFormat("es-AR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  form.querySelectorAll(".app-gasto-propuesto-campos [required]").forEach((campo) => {
    campo.dataset.requerido = "true";
  });

  function actualizar() {
    let elegida = 0;
    form.querySelectorAll(".app-gasto-propuesto").forEach((gasto) => {
      const tilde = gasto.querySelector(".app-incluir");
      const incluido = !tilde || tilde.checked;
      gasto.classList.toggle("is-excluido", !incluido);
      // Lo excluido no tiene que frenar el envío por campos requeridos. No se deshabilita: así sus
      // datos viajan igual y no se pierden si la revisión vuelve con un error
      gasto.querySelectorAll("[data-requerido]").forEach((campo) => {
        campo.required = incluido;
      });
      const importe = parseFloat(gasto.querySelector(".app-importe").value);
      if (incluido && !isNaN(importe)) {
        elegida += importe;
      }
    });
    if (suma) {
      suma.textContent = "$ " + formato.format(elegida);
    }
    if (aviso && !isNaN(total)) {
      aviso.hidden = Math.abs(elegida - total) < TOLERANCIA;
    }
  }

  form.addEventListener("input", actualizar);
  form.addEventListener("change", actualizar);
  actualizar();
})();
