package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ReglasDeContrasenaTest {

  @Test
  public void debeAceptarUnaContrasenaConLargoMayusculaMinusculaYCaracterEspecial() {
    assertTrue(ReglasDeContrasena.cumple("Gasto1!"));
    assertTrue(ReglasDeContrasena.cumple("Ab.cde"));
  }

  @Test
  public void debeAceptarLetrasConTildeYEnie() {
    assertTrue(ReglasDeContrasena.cumple("Ñandú_2026"));
  }

  @Test
  public void debeRechazarUnaContrasenaDeMenosDe6Caracteres() {
    assertFalse(ReglasDeContrasena.cumple("Ab1!"));
  }

  @Test
  public void debeRechazarUnaContrasenaSinMayuscula() {
    assertFalse(ReglasDeContrasena.cumple("gastos1!"));
  }

  @Test
  public void debeRechazarUnaContrasenaSinMinuscula() {
    assertFalse(ReglasDeContrasena.cumple("GASTOS1!"));
  }

  @Test
  public void debeRechazarUnaContrasenaSinCaracterEspecial() {
    assertFalse(ReglasDeContrasena.cumple("Gastos123"));
  }

  @Test
  public void noDebeContarElEspacioComoCaracterEspecial() {
    assertFalse(ReglasDeContrasena.cumple("Gastos 123"));
  }

  @Test
  public void debeRechazarUnaContrasenaVaciaONula() {
    assertFalse(ReglasDeContrasena.cumple(""));
    assertFalse(ReglasDeContrasena.cumple(null));
  }
}
