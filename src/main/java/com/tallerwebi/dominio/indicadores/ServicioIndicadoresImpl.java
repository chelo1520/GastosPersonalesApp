package com.tallerwebi.dominio.indicadores;

import com.tallerwebi.dominio.excepcion.IndicadoresNoDisponibles;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * ServicioIndicadoresImpl: trae las series una vez por día y responde desde memoria.
 * Si la API falla, sigue respondiendo con lo último cargado y lo señala en datosActualizados().
 */
@Service
public class ServicioIndicadoresImpl implements ServicioIndicadores {

  private final ClienteIndicadores cliente;
  private final Clock reloj;

  private final NavigableMap<LocalDate, CotizacionDolar> dolarPorFecha = new TreeMap<>();
  private final NavigableMap<YearMonth, InflacionMensual> inflacionPorMes = new TreeMap<>();
  private CotizacionDolar cotizacionDeHoy;
  private LocalDate diaDeLaCache;
  private LocalDateTime momentoUltimaCarga;
  private boolean ultimaCargaFallo;

  @Autowired
  public ServicioIndicadoresImpl(ClienteIndicadores cliente) {
    this(cliente, Clock.systemDefaultZone());
  }

  // Permite fijar "hoy" en los tests
  public ServicioIndicadoresImpl(ClienteIndicadores cliente, Clock reloj) {
    this.cliente = cliente;
    this.reloj = reloj;
  }

  @Override
  public synchronized CotizacionDolar cotizacionDolar(LocalDate fecha) {
    actualizarSiHaceFalta();
    Map.Entry<LocalDate, CotizacionDolar> cotizacion = dolarPorFecha.floorEntry(fecha);
    if (cotizacion == null) {
      throw new IndicadoresNoDisponibles("No hay cotización del dólar para " + fecha);
    }
    return cotizacion.getValue();
  }

  @Override
  public synchronized CotizacionDolar dolarDeHoy() {
    actualizarSiHaceFalta();
    if (cotizacionDeHoy != null) {
      return cotizacionDeHoy;
    }
    return cotizacionDolar(LocalDate.now(reloj));
  }

  @Override
  public synchronized InflacionMensual inflacionMensual(YearMonth mes) {
    actualizarSiHaceFalta();
    InflacionMensual publicada = inflacionPorMes.get(mes);
    if (publicada != null) {
      return publicada;
    }
    Map.Entry<YearMonth, InflacionMensual> ultima = inflacionPorMes.floorEntry(mes);
    if (ultima == null) {
      throw new IndicadoresNoDisponibles("No hay dato de inflación para " + mes);
    }
    return new InflacionMensual(mes, ultima.getValue().getPorcentaje(), true);
  }

  @Override
  public synchronized LocalDateTime ultimaActualizacion() {
    return momentoUltimaCarga;
  }

  @Override
  public synchronized boolean datosActualizados() {
    return momentoUltimaCarga != null && !ultimaCargaFallo;
  }

  // Una carga por día. Si falla y hay datos de una carga anterior, se siguen usando
  private void actualizarSiHaceFalta() {
    LocalDate hoy = LocalDate.now(reloj);
    if (hoy.equals(diaDeLaCache)) {
      return;
    }
    try {
      cargar();
      diaDeLaCache = hoy;
      momentoUltimaCarga = LocalDateTime.now(reloj);
      ultimaCargaFallo = false;
    } catch (RuntimeException e) {
      ultimaCargaFallo = true;
      if (dolarPorFecha.isEmpty() || inflacionPorMes.isEmpty()) {
        throw new IndicadoresNoDisponibles("La API de indicadores no responde", e);
      }
    }
  }

  private void cargar() {
    NavigableMap<LocalDate, CotizacionDolar> dolares = new TreeMap<>();
    for (CotizacionDolar cotizacion : cliente.obtenerDolarOficialHistorico()) {
      dolares.put(cotizacion.getFecha(), cotizacion);
    }
    NavigableMap<YearMonth, InflacionMensual> inflacion = new TreeMap<>();
    for (InflacionMensual mensual : cliente.obtenerInflacionMensual()) {
      inflacion.put(mensual.getMes(), mensual);
    }
    if (dolares.isEmpty() || inflacion.isEmpty()) {
      throw new IndicadoresNoDisponibles("La API de indicadores devolvió series vacías");
    }
    CotizacionDolar hoy = cliente.obtenerDolarOficialDeHoy();

    dolarPorFecha.clear();
    dolarPorFecha.putAll(dolares);
    inflacionPorMes.clear();
    inflacionPorMes.putAll(inflacion);
    cotizacionDeHoy = hoy;
  }
}
