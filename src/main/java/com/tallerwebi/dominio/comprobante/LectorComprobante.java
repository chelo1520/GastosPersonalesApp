package com.tallerwebi.dominio.comprobante;

import java.util.List;

/** Extrae datos de una imagen de comprobante sin persistir la imagen. */
@FunctionalInterface
public interface LectorComprobante {
  DatosComprobante leer(byte[] imagen, String tipoImagen, List<String> categorias);
}
