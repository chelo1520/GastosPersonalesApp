/* global bootstrap */
// Confirmación con modal: los formularios con data-confirmar-texto se detienen y preguntan antes de enviarse.
// Necesita el fragmento fragments/confirmar :: modal en la página y Bootstrap cargado antes.
(function () {
  const modalEl = document.getElementById("modal-confirmar");
  if (!modalEl || typeof bootstrap === "undefined") {
    return;
  }
  const modal = new bootstrap.Modal(modalEl);
  const titulo = modalEl.querySelector(".app-confirmar-titulo");
  const texto = modalEl.querySelector(".app-confirmar-texto");
  const botonSi = modalEl.querySelector(".app-confirmar-si");
  let formulario = null;
  let disparador = null;

  document.addEventListener("submit", (evento) => {
    const form = evento.target;
    if (!(form instanceof HTMLFormElement) || !form.dataset.confirmarTexto) {
      return;
    }
    evento.preventDefault();
    formulario = form;
    disparador = document.activeElement;
    titulo.textContent = form.dataset.confirmarTitulo || "¿Confirmás?";
    texto.textContent = form.dataset.confirmarTexto;
    botonSi.textContent = form.dataset.confirmarBoton || "Confirmar";
    modal.show();
  });

  botonSi.addEventListener("click", () => {
    if (formulario) {
      // submit() no vuelve a disparar el evento submit: no se pregunta dos veces
      formulario.submit();
    }
  });

  // Al cancelar, el foco vuelve al botón que abrió la confirmación
  modalEl.addEventListener("hidden.bs.modal", () => {
    if (disparador && typeof disparador.focus === "function") {
      disparador.focus();
    }
    formulario = null;
  });
})();
