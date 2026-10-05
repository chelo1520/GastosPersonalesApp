package com.tallerwebi.dominio.gasto;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.GastoInvalidoExeption;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * GastoServicioImpl
 */
@Service
@Transactional
public class GastoServicioImpl implements GastoServicio {

  private RepositorioGasto repositorioGasto;

  public GastoServicioImpl(RepositorioGasto repositorioGasto) {
    this.repositorioGasto = repositorioGasto;
  }

  @Override
  public void registrarGasto(Gasto gasto, Usuario usuario) {
    if (gasto.getImporte() <= 0) {
      throw new GastoInvalidoExeption("El importe debe ser mayor a cero");
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
  public List<Gasto> obtenerGastosDelMesAnterior(Usuario usuario, YearMonth mesActual) {
    return repositorioGasto.obtenerGastosDelMes(usuario, mesActual.minusMonths(1));
  }
}
