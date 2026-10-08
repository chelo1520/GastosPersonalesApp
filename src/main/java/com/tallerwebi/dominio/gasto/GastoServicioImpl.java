package com.tallerwebi.dominio.gasto;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.GastoInvalidoExeption;
import com.tallerwebi.dominio.excepcion.GastoNoEncontrado;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
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
    validar(gasto);

    gasto.setUsuario(usuario);

    repositorioGasto.guardar(gasto);
  }

  @Override
  public Gasto obtenerGasto(Long id, Usuario usuario) {
    Gasto gasto = id == null ? null : repositorioGasto.buscarPorId(id);
    // Un gasto de otro usuario se trata igual que uno inexistente
    if (gasto == null || !esDelUsuario(gasto, usuario)) {
      throw new GastoNoEncontrado();
    }
    return gasto;
  }

  @Override
  public void modificarGasto(Long id, Gasto datos, Usuario usuario) {
    Gasto gasto = obtenerGasto(id, usuario);
    validar(datos);

    gasto.setImporte(datos.getImporte());
    gasto.setFecha(datos.getFecha());
    gasto.setDescripcion(datos.getDescripcion());

    repositorioGasto.modificar(gasto);
  }

  private boolean esDelUsuario(Gasto gasto, Usuario usuario) {
    return (
      usuario != null &&
      gasto.getUsuario() != null &&
      Objects.equals(gasto.getUsuario().getId(), usuario.getId())
    );
  }

  private void validar(Gasto gasto) {
    if (gasto.getImporte() == null || gasto.getImporte() <= 0) {
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
  public List<Gasto> obtenerGastosDelMesAnterior(Usuario usuario, YearMonth mesActual) {
    return repositorioGasto.obtenerGastosDelMes(usuario, mesActual.minusMonths(1));
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
