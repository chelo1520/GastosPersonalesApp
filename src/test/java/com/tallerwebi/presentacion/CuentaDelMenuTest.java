package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tallerwebi.dominio.Usuario;
import org.junit.jupiter.api.Test;

public class CuentaDelMenuTest {

  @Test
  public void conNombreYApellidoMuestraSoloElNombreYSuInicial() {
    CuentaDelMenu cuenta = CuentaDelMenu.de(usuario(" Ana ", "Pérez", "ana@unlam.edu.ar"));

    assertEquals("Ana", cuenta.getNombre());
    assertEquals("A", cuenta.getInicial());
    assertEquals("ana@unlam.edu.ar", cuenta.getEmail());
  }

  @Test
  public void soloConNombreTambienMuestraElNombre() {
    CuentaDelMenu cuenta = CuentaDelMenu.de(usuario("Luis", null, "luis@unlam.edu.ar"));

    assertEquals("Luis", cuenta.getNombre());
    assertEquals("L", cuenta.getInicial());
  }

  @Test
  public void sinNombreUsaElEmailAunqueTengaApellido() {
    CuentaDelMenu cuenta = CuentaDelMenu.de(usuario("  ", "Pérez", "test@unlam.edu.ar"));

    assertEquals("test@unlam.edu.ar", cuenta.getNombre());
    assertEquals("T", cuenta.getInicial());
  }

  @Test
  public void sinNingunDatoMuestraUnSignoDePregunta() {
    CuentaDelMenu cuenta = CuentaDelMenu.de(usuario(null, null, null));

    assertEquals("", cuenta.getNombre());
    assertEquals("?", cuenta.getInicial());
  }

  private Usuario usuario(String nombre, String apellido, String email) {
    Usuario usuario = new Usuario();
    usuario.setNombre(nombre);
    usuario.setApellido(apellido);
    usuario.setEmail(email);
    return usuario;
  }
}
