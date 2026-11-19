package com.tallerwebi.dominio.comprobante;

/** El comprobante no existe, es de otro usuario o su archivo no está disponible. */
public class ComprobanteNoEncontrado extends RuntimeException {

  private static final long serialVersionUID = 1L;
  private static final String MENSAJE = "El archivo de este comprobante no está disponible.";

  public ComprobanteNoEncontrado() {
    super(MENSAJE);
  }

  public ComprobanteNoEncontrado(Throwable causa) {
    super(MENSAJE, causa);
  }
}
