package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.gasto.Gasto;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Un gasto en la lista "Últimos movimientos" del resumen: con la inicial y el color de su categoría
 * y la fecha relativa a hoy ("Hoy", "Ayer" o "12 nov").
 */
public class MovimientoReciente {

  private static final DateTimeFormatter DIA_Y_MES = DateTimeFormatter.ofPattern(
    "d MMM",
    Locale.of("es", "AR")
  );

  private final Gasto gasto;
  private final LocalDate hoy;

  public MovimientoReciente(Gasto gasto, LocalDate hoy) {
    this.gasto = gasto;
    this.hoy = hoy;
  }

  public Long getId() {
    return gasto.getId();
  }

  public String getDescripcion() {
    return gasto.getDescripcion();
  }

  public Double getImporte() {
    return gasto.getImporte();
  }

  public String getCategoria() {
    return gasto.getCategoria() == null ? "Sin categoría" : gasto.getCategoria().getNombre();
  }

  public String getInicial() {
    String nombre = getCategoria();
    return nombre.isBlank() ? "?" : nombre.substring(0, 1).toUpperCase(Locale.ROOT);
  }

  public String getColor() {
    return ColoresDeCategoria.de(
      gasto.getCategoria() == null ? null : gasto.getCategoria().getNombre()
    );
  }

  public String getCuando() {
    LocalDate fecha = gasto.getFecha();
    if (fecha.equals(hoy)) {
      return "Hoy";
    }
    if (fecha.equals(hoy.minusDays(1))) {
      return "Ayer";
    }
    return fecha.format(DIA_Y_MES).replace(".", "");
  }
}
