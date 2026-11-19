package com.tallerwebi.dominio.comprobante;

/** El almacén de comprobantes no está configurado o falló al guardar un archivo. */
public class AlmacenNoDisponible extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public AlmacenNoDisponible(String mensaje) {
    super(mensaje);
  }

  public AlmacenNoDisponible(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
