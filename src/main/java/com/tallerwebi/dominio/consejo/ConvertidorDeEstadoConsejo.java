package com.tallerwebi.dominio.consejo;

/** EstadoConsejo como texto en la base; ver ConvertidorDeEnum. Sin @Converter a propósito: el escaneo de Spring (soporte hibernate5) no es compatible con Hibernate 6 para registrarlos; se declara en el campo. */
public class ConvertidorDeEstadoConsejo extends ConvertidorDeEnum<EstadoConsejo> {

  public ConvertidorDeEstadoConsejo() {
    super(EstadoConsejo.class);
  }
}
