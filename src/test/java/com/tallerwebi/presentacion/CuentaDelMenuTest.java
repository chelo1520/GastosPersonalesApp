package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tallerwebi.dominio.Usuario;
import org.junit.jupiter.api.Test;

public class CuentaDelMenuTest {

  @Test
  public void elBotonMuestraSoloElNombreYAlDesplegarNombreYApellidoConLasDosIniciales() {
    CuentaDelMenu cuenta = CuentaDelMenu.de(usuario(" Ana ", " Pérez ", "ana@unlam.edu.ar"));

    assertEquals("Ana", cuenta.getNombre());
    assertEquals("Ana Pérez", cuenta.getNombreCompleto());
    assertEquals("AP", cuenta.getIniciales());
    assertEquals("ana@unlam.edu.ar", cuenta.getEmail());
  }

  @Test
  public void soloConNombreUsaElNombreEnTodosLadosYUnaInicial() {
    CuentaDelMenu cuenta = CuentaDelMenu.de(usuario("Luis", null, "luis@unlam.edu.ar"));

    assertEquals("Luis", cuenta.getNombre());
    assertEquals("Luis", cuenta.getNombreCompleto());
    assertEquals("L", cuenta.getIniciales());
  }

  @Test
  public void sinNombreUsaElEmailAunqueTengaApellido() {
    CuentaDelMenu cuenta = CuentaDelMenu.de(usuario("  ", "Pérez", "test@unlam.edu.ar"));

    assertEquals("test@unlam.edu.ar", cuenta.getNombre());
    assertEquals("test@unlam.edu.ar", cuenta.getNombreCompleto());
    assertEquals("T", cuenta.getIniciales());
  }

  @Test
  public void sinNingunDatoMuestraUnSignoDePregunta() {
    CuentaDelMenu cuenta = CuentaDelMenu.de(usuario(null, null, null));

    assertEquals("", cuenta.getNombre());
    assertEquals("?", cuenta.getIniciales());
  }

  private Usuario usuario(String nombre, String apellido, String email) {
    Usuario usuario = new Usuario();
    usuario.setNombre(nombre);
    usuario.setApellido(apellido);
    usuario.setEmail(email);
    return usuario;
  }
}
