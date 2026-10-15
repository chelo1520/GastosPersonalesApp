package com.tallerwebi.dominio.comprobante;

import java.util.List;

/** Extrae datos de un comprobante (imagen JPG/PNG o PDF) sin persistir el archivo. */
@FunctionalInterface
public interface LectorComprobante {
  String TIPO_PDF = "application/pdf";

  /** tipoArchivo es el media type detectado: image/jpeg, image/png o application/pdf. */
  DatosComprobante leer(byte[] archivo, String tipoArchivo, List<String> categorias);
}
