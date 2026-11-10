document.addEventListener("DOMContentLoaded", () => {
  const bienvenida = document.getElementById("guia-bienvenida");
  if (bienvenida) {
    bienvenida.addEventListener("cancel", (evento) => {
      evento.preventDefault();
      bienvenida.querySelector("form[data-guia-pausar]").requestSubmit();
    });
    bienvenida.showModal();
    return;
  }

  const guia = document.getElementById("guia-pasos");
  if (!guia) {
    return;
  }

  const pasos = {
    1: { destino: "#guia-destino-perfil", url: guia.dataset.perfil, foco: "#ingresoMensual" },
    2: {
      destino: "#guia-destino-categorias",
      url: guia.dataset.presupuestos,
      foco: "tr:not([hidden]) .app-presupuesto-lapiz",
    },
    3: {
      destino: "#guia-destino-gasto, #guia-destino-comprobante, #guia-destino-lista",
      url: guia.dataset.registrar,
      foco: "#importe",
    },
    4: { destino: "#guia-destino-resumen", url: guia.dataset.resumen },
    6: { destino: "#guia-destino-simulacion", url: guia.dataset.simulacion },
    7: { destino: "#guia-destino-asistente", url: guia.dataset.asistente },
  };
  const paso = pasos[guia.dataset.paso];
  if (!paso) {
    return;
  }
  const destino = document.querySelector(paso.destino);
  if (!destino) {
    window.location.replace(paso.url);
    return;
  }

  document.body.classList.add("app-guia-activa");
  destino.classList.add("app-guia-destacado");
  destino.scrollIntoView({ block: "start" });
  const campo = paso.foco && destino.querySelector(paso.foco);
  if (campo) {
    campo.focus({ preventScroll: true });
  }
});
