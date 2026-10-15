package com.tallerwebi.dominio.comprobante;

/** La imagen no contiene datos suficientes para proponer un gasto. */
public class ComprobanteNoLegible extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ComprobanteNoLegible() {
    super(
      "No pudimos leer los datos del comprobante. Probá con otra imagen o cargá el gasto a mano."
    );
  }

  public ComprobanteNoLegible(Throwable causa) {
    super(
      "No pudimos leer los datos del comprobante. Probá con otra imagen o cargá el gasto a mano.",
      causa
    );
  }
}
