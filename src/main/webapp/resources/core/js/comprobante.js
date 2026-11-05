// Revisión de un comprobante: un gasto destildado se ve atenuado y deja de pedir sus campos
// obligatorios (no se guarda). No se deshabilita: así sus datos no se pierden si hay un error.
document.querySelectorAll(".app-gasto-propuesto").forEach((gasto) => {
  const tilde = gasto.querySelector(".app-incluir");
  const campos = gasto.querySelectorAll(".app-gasto-propuesto-campos [required]");
  function actualizar() {
    gasto.classList.toggle("is-excluido", !tilde.checked);
    campos.forEach((campo) => {
      campo.required = tilde.checked;
    });
  }
  tilde.addEventListener("change", actualizar);
  actualizar();
});
