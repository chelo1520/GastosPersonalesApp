package com.tallerwebi.dominio.presupuesto;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.excepcion.PresupuestoInvalido;
import jakarta.transaction.Transactional;
import java.time.YearMonth;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * ServicioPresupuestoImpl
 */
@Service
@Transactional
public class ServicioPresupuestoImpl implements ServicioPresupuesto {

  private RepositorioPresupuesto repositorioPresupuesto;

  @Autowired
  public ServicioPresupuestoImpl(RepositorioPresupuesto repositorioPresupuesto) {
    this.repositorioPresupuesto = repositorioPresupuesto;
  }

  @Override
  public void definir(Usuario usuario, Categoria categoria, YearMonth mes, Double monto) {
    if (categoria == null) {
      throw new PresupuestoInvalido("Elegí una categoría");
    }
    if (mes == null) {
      throw new PresupuestoInvalido("El mes es obligatorio");
    }
    if (monto == null || monto <= 0) {
      throw new PresupuestoInvalido("El monto debe ser mayor a cero");
    }

    Presupuesto existente = repositorioPresupuesto.buscar(usuario, categoria, mes);
    if (existente != null) {
      existente.setMonto(monto);
      repositorioPresupuesto.modificar(existente);
      return;
    }

    repositorioPresupuesto.guardar(new Presupuesto(usuario, categoria, mes, monto));
  }

  @Override
  public Presupuesto buscar(Usuario usuario, Categoria categoria, YearMonth mes) {
    return repositorioPresupuesto.buscar(usuario, categoria, mes);
  }

  @Override
  public List<Presupuesto> obtenerDelMes(Usuario usuario, YearMonth mes) {
    return repositorioPresupuesto.obtenerDelMes(usuario, mes);
  }
}
