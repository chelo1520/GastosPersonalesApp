package com.tallerwebi.dominio.consejo;

import jakarta.persistence.AttributeConverter;

/**
 * Guarda un enum por su nombre, como texto plano. A diferencia de @Enumerated, no genera ni un
 * ENUM de MySQL ni una restricción CHECK con la lista de valores: se pueden agregar tipos de
 * consejo sin migrar la columna (hbm2ddl update no altera columnas ni restricciones existentes).
 */
abstract class ConvertidorDeEnum<E extends Enum<E>> implements AttributeConverter<E, String> {

  private final Class<E> tipo;

  protected ConvertidorDeEnum(Class<E> tipo) {
    this.tipo = tipo;
  }

  @Override
  public String convertToDatabaseColumn(E valor) {
    return valor == null ? null : valor.name();
  }

  @Override
  public E convertToEntityAttribute(String nombre) {
    return nombre == null ? null : Enum.valueOf(tipo, nombre);
  }
}
