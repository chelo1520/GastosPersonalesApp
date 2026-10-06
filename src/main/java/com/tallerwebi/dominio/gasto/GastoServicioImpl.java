package com.tallerwebi.dominio.gasto;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.GastoInvalidoExeption;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * GastoServicioImpl
 */
@Service
@Transactional
public class GastoServicioImpl implements GastoServicio {

  private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private RepositorioGasto repositorioGasto;
  private Clock reloj;

  @Autowired
  public GastoServicioImpl(RepositorioGasto repositorioGasto) {
    this(repositorioGasto, Clock.systemDefaultZone());
  }

  // Permite fijar "hoy" en los tests
  public GastoServicioImpl(RepositorioGasto repositorioGasto, Clock reloj) {
    this.repositorioGasto = repositorioGasto;
    this.reloj = reloj;
  }

  @Override
  public void registrarGasto(Gasto gasto, Usuario usuario) {
    if (gasto.getImporte() <= 0) {
      throw new GastoInvalidoExeption("El importe debe ser mayor a cero");
    }

    LocalDate minima = obtenerFechaMinimaPermitida();
    LocalDate maxima = obtenerFechaMaximaPermitida();
    LocalDate fecha = gasto.getFecha();
    if (fecha == null || fecha.isBefore(minima) || fecha.isAfter(maxima)) {
      throw new GastoInvalidoExeption(
        "La fecha debe estar dentro del mes actual (entre el " +
        minima.format(FORMATO_FECHA) +
        " y el " +
        maxima.format(FORMATO_FECHA) +
        ")"
      );
    }

    gasto.setUsuario(usuario);

    repositorioGasto.guardar(gasto);
  }

  @Override
  public List<Gasto> obtenerGastos(Usuario usuario) {
    return repositorioGasto.BuscarGastosPorUsuario(usuario);
  }

  @Override
  public Double sumarGastos(Usuario usuario, LocalDate desde, LocalDate hasta) {
    List<Gasto> gastos = repositorioGasto.obtenerGastosEntreFechas(usuario, desde, hasta);

    Double total = 0.0;

    for (Gasto gasto : gastos) {
      total += gasto.getImporte();
    }

    return total;
  }

  @Override
  public LocalDate obtenerFechaMinimaPermitida() {
    return YearMonth.now(reloj).atDay(1);
  }

  @Override
  public LocalDate obtenerFechaMaximaPermitida() {
    return YearMonth.now(reloj).atEndOfMonth();
  }
}
