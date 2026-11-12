package com.tallerwebi.dominio.gasto;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.alerta.Alerta;
import com.tallerwebi.dominio.alerta.CalculadoraAlertas;
import com.tallerwebi.dominio.categoria.ServicioCategoria;
import com.tallerwebi.dominio.excepcion.GastoInvalidoExeption;
import com.tallerwebi.dominio.excepcion.GastoNoEncontrado;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
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
  static final int MESES_HACIA_ATRAS = 12;

  private final RepositorioGasto repositorioGasto;
  private final ServicioPresupuesto servicioPresupuesto;
  private final ServicioCategoria servicioCategoria;
  private final Clock reloj;
  private final CalculadoraAlertas calculadoraAlertas = new CalculadoraAlertas();

  @Autowired
  public GastoServicioImpl(
    RepositorioGasto repositorioGasto,
    ServicioPresupuesto servicioPresupuesto,
    ServicioCategoria servicioCategoria
  ) {
    this(repositorioGasto, servicioPresupuesto, servicioCategoria, Clock.systemDefaultZone());
  }

  // Permite fijar "hoy" en los tests
  public GastoServicioImpl(
    RepositorioGasto repositorioGasto,
    ServicioPresupuesto servicioPresupuesto,
    ServicioCategoria servicioCategoria,
    Clock reloj
  ) {
    this.repositorioGasto = repositorioGasto;
    this.servicioPresupuesto = servicioPresupuesto;
    this.servicioCategoria = servicioCategoria;
    this.reloj = reloj;
  }

  @Override
  public void registrarGasto(Gasto gasto, Usuario usuario) {
    validar(gasto);

    gasto.setUsuario(usuario);

    repositorioGasto.guardar(gasto);
  }

  @Override
  public void registrarGastos(List<Gasto> gastos, Usuario usuario) {
    // Primero se validan todos: así un gasto inválido no deja guardados a medias los anteriores.
    // Con varios, el error dice de cuál se trata
    for (Gasto gasto : gastos) {
      validar(gasto, gastos.size() == 1 ? "" : gasto.getDescripcion() + ": ");
    }
    for (Gasto gasto : gastos) {
      gasto.setUsuario(usuario);
      repositorioGasto.guardar(gasto);
    }
  }

  @Override
  public Alerta alertaDePresupuesto(List<Gasto> gastos, Usuario usuario) {
    YearMonth mesActual = YearMonth.now(reloj);
    double importeDelMes = gastos
      .stream()
      .filter(gasto -> YearMonth.from(gasto.getFecha()).equals(mesActual))
      .mapToDouble(Gasto::getImporte)
      .sum();
    if (importeDelMes <= 0) {
      return null;
    }
    // Los gastos del mes en curso cuentan como uno solo: lo que se suma de golpe al mes
    return alertaDePresupuesto(new Gasto(importeDelMes, mesActual.atDay(1), null), usuario);
  }

  @Override
  public Alerta alertaDeRepeticion(Gasto gasto, Usuario usuario) {
    // La alerta habla de "esta semana": un gasto cargado con fecha vieja no la dispara
    LocalDate inicioDeLaSemana = LocalDate
      .now(reloj)
      .minusDays(CalculadoraAlertas.DIAS_ALERTA_REPETICION - 1);
    if (gasto.getFecha().isBefore(inicioDeLaSemana)) {
      return null;
    }
    LocalDate desde = gasto.getFecha().minusDays(CalculadoraAlertas.DIAS_ALERTA_REPETICION - 1);
    List<Gasto> gastosDeLaSemana = repositorioGasto.obtenerGastosEntreFechas(
      usuario,
      desde,
      gasto.getFecha()
    );
    // "Chico" se mide contra el total del mes del gasto; sin total, contra lo asignado a categorías
    YearMonth mesDelGasto = YearMonth.from(gasto.getFecha());
    double presupuesto = servicioPresupuesto.presupuestoDelMes(usuario, mesDelGasto);
    if (presupuesto <= 0) {
      presupuesto = servicioPresupuesto.totalDelMes(usuario, mesDelGasto);
    }
    return calculadoraAlertas.alertaDeRepeticion(
      gasto,
      gastosDeLaSemana,
      presupuesto,
      servicioCategoria.marcasDe(usuario)
    );
  }

  @Override
  public Alerta alertaDePresupuesto(Gasto gasto, Usuario usuario) {
    YearMonth mes = YearMonth.from(gasto.getFecha());
    // Como las demás alertas, solo aplica al mes en curso: un mes que ya cerró no se avisa
    if (!mes.equals(YearMonth.now(reloj))) {
      return null;
    }
    // Ya guardado: el total del mes incluye el gasto nuevo
    double gastadoDelMes = sumarGastos(usuario, mes.atDay(1), mes.atEndOfMonth());
    // Contra el total del mes; si no hay, contra lo asignado a categorías
    double presupuesto = servicioPresupuesto.presupuestoDelMes(usuario, mes);
    if (presupuesto <= 0) {
      presupuesto = servicioPresupuesto.totalDelMes(usuario, mes);
    }
    return calculadoraAlertas.alertaDePresupuesto(gastadoDelMes, gasto.getImporte(), presupuesto);
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
    gasto.setCategoria(datos.getCategoria());

    repositorioGasto.modificar(gasto);
  }

  @Override
  public void eliminarGasto(Long id, Usuario usuario) {
    Gasto gasto = obtenerGasto(id, usuario);
    repositorioGasto.eliminar(gasto);
  }

  private boolean esDelUsuario(Gasto gasto, Usuario usuario) {
    return (
      usuario != null &&
      gasto.getUsuario() != null &&
      Objects.equals(gasto.getUsuario().getId(), usuario.getId())
    );
  }

  private void validar(Gasto gasto) {
    validar(gasto, "");
  }

  private void validar(Gasto gasto, String prefijoDelError) {
    if (gasto.getImporte() == null || gasto.getImporte() <= 0) {
      throw new GastoInvalidoExeption(prefijoDelError + "El importe debe ser mayor a cero");
    }

    LocalDate minima = obtenerFechaMinimaPermitida();
    LocalDate maxima = obtenerFechaMaximaPermitida();
    LocalDate fecha = gasto.getFecha();
    if (fecha == null || fecha.isBefore(minima) || fecha.isAfter(maxima)) {
      throw new GastoInvalidoExeption(
        prefijoDelError +
        "La fecha debe estar entre el " +
        minima.format(FORMATO_FECHA) +
        " y el " +
        maxima.format(FORMATO_FECHA)
      );
    }
  }

  @Override
  public List<Gasto> obtenerGastos(Usuario usuario) {
    return repositorioGasto.buscarGastosPorUsuario(usuario);
  }

  @Override
  public Double sumarGastos(Usuario usuario, LocalDate desde, LocalDate hasta) {
    return repositorioGasto
      .obtenerGastosEntreFechas(usuario, desde, hasta)
      .stream()
      .mapToDouble(Gasto::getImporte)
      .sum();
  }

  @Override
  public List<Gasto> obtenerGastosDelMesAnterior(Usuario usuario, YearMonth mesActual) {
    return repositorioGasto.obtenerGastosDelMes(usuario, mesActual.minusMonths(1));
  }

  @Override
  public List<Gasto> obtenerGastosDelMes(Usuario usuario, YearMonth mes) {
    return repositorioGasto.obtenerGastosDelMes(usuario, mes);
  }

  @Override
  public LocalDate obtenerFechaMinimaPermitida() {
    // Se pueden cargar gastos atrasados: hasta el mismo mes del año pasado
    return YearMonth.now(reloj).minusMonths(MESES_HACIA_ATRAS).atDay(1);
  }

  @Override
  public LocalDate obtenerFechaMaximaPermitida() {
    return YearMonth.now(reloj).atEndOfMonth();
  }
}
