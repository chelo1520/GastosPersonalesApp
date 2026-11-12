package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.NombreDeMes;
import com.tallerwebi.dominio.presupuesto.MesDePresupuesto;
import java.time.YearMonth;

/**
 * Una fila de la tabla anual de presupuestos del perfil, lista para la vista.
 */
public class MesDelAnio {

  private final MesDePresupuesto datos;
  private final YearMonth mesActual;

  public MesDelAnio(MesDePresupuesto datos, YearMonth mesActual) {
    this.datos = datos;
    this.mesActual = mesActual;
  }

  public String getMes() {
    return datos.mes().toString();
  }

  public String getNombre() {
    return NombreDeMes.sinAnio(datos.mes());
  }

  public double getTotal() {
    return datos.total();
  }

  public boolean isPropio() {
    return datos.propio();
  }

  public double getAsignado() {
    return datos.asignado();
  }

  public double getSinAsignar() {
    return datos.sinAsignar();
  }

  public boolean isEnCurso() {
    return datos.mes().equals(mesActual);
  }

  public boolean isPasado() {
    return datos.mes().isBefore(mesActual);
  }
}
