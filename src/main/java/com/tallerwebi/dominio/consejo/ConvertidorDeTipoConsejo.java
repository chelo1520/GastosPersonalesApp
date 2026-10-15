package com.tallerwebi.dominio.consejo;

/** TipoConsejo como texto en la base; ver ConvertidorDeEnum. Sin @Converter a propósito: el escaneo de Spring (soporte hibernate5) no es compatible con Hibernate 6 para registrarlos; se declara en el campo. */
public class ConvertidorDeTipoConsejo extends ConvertidorDeEnum<TipoConsejo> {

  public ConvertidorDeTipoConsejo() {
    super(TipoConsejo.class);
  }
}
