package com.tallerwebi.dominio.comprobante;

/** El servicio externo de lectura no está disponible o no está configurado. */
public class LectorComprobanteNoDisponible extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public LectorComprobanteNoDisponible() {
    super(
      "La lectura de comprobantes no está disponible por el momento. Podés cargar el gasto a mano."
    );
  }

  public LectorComprobanteNoDisponible(Throwable causa) {
    super(
      "La lectura de comprobantes no está disponible por el momento. Podés cargar el gasto a mano.",
      causa
    );
  }
}
