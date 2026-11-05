package com.tallerwebi.dominio.presupuesto;

import com.tallerwebi.dominio.NombreDeMes;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.excepcion.PresupuestoInvalido;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * ServicioPresupuestoImpl
 */
@Service
@Transactional
public class ServicioPresupuestoImpl implements ServicioPresupuesto {

  private RepositorioPresupuesto repositorioPresupuesto;
  private final Clock reloj;

  @Autowired
  public ServicioPresupuestoImpl(RepositorioPresupuesto repositorioPresupuesto) {
    this(repositorioPresupuesto, Clock.systemDefaultZone());
  }

  // Permite fijar "hoy" en los tests
  public ServicioPresupuestoImpl(RepositorioPresupuesto repositorioPresupuesto, Clock reloj) {
    this.repositorioPresupuesto = repositorioPresupuesto;
    this.reloj = reloj;
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
    validarQueNoSuperaElPresupuestoMensual(usuario, mes, existente, monto);

    if (existente != null) {
      existente.setMonto(monto);
      repositorioPresupuesto.modificar(existente);
      return;
    }

    repositorioPresupuesto.guardar(new Presupuesto(usuario, categoria, mes, monto));
  }

  // Sin presupuesto en el perfil no hay tope; con él, las categorías no pueden sumar más
  private void validarQueNoSuperaElPresupuestoMensual(
    Usuario usuario,
    YearMonth mes,
    Presupuesto existente,
    Double monto
  ) {
    double presupuestoDelMes = presupuestoDelMes(usuario, mes);
    if (presupuestoDelMes <= 0) {
      return;
    }
    double anterior = existente == null ? 0.0 : existente.getMonto();
    double otras = totalDelMes(usuario, mes) - anterior;
    if (otras + monto > presupuestoDelMes) {
      throw new PresupuestoInvalido("El monto supera tu presupuesto mensual disponible");
    }
  }

  @Override
  public Presupuesto buscar(Usuario usuario, Categoria categoria, YearMonth mes) {
    return repositorioPresupuesto.buscar(usuario, categoria, mes);
  }

  @Override
  public List<Presupuesto> obtenerDelMes(Usuario usuario, YearMonth mes) {
    return repositorioPresupuesto.obtenerDelMes(usuario, mes);
  }

  @Override
  public Double totalDelMes(Usuario usuario, YearMonth mes) {
    double total = 0.0;
    for (Presupuesto presupuesto : obtenerDelMes(usuario, mes)) {
      total += presupuesto.getMonto();
    }
    return total;
  }

  @Override
  public Double presupuestoDelMes(Usuario usuario, YearMonth mes) {
    PresupuestoTotal total = repositorioPresupuesto.buscarTotal(usuario, mes);
    return total != null ? total.getMonto() : usuario.presupuestoSegunPerfil();
  }

  @Override
  public void definirPresupuestoDelMes(Usuario usuario, YearMonth mes, Double monto) {
    if (mes == null) {
      throw new PresupuestoInvalido("El mes es obligatorio");
    }
    if (monto == null || monto <= 0) {
      throw new PresupuestoInvalido("El monto debe ser mayor a cero");
    }
    if (monto < totalDelMes(usuario, mes)) {
      throw new PresupuestoInvalido(
        "El total no puede ser menor a lo que ya asignaste a tus categorías"
      );
    }

    PresupuestoTotal existente = repositorioPresupuesto.buscarTotal(usuario, mes);
    if (existente == null) {
      existente = new PresupuestoTotal(usuario, mes, monto);
    } else {
      existente.setMonto(monto);
    }
    repositorioPresupuesto.guardarTotal(existente);
  }

  @Override
  public void validarPresupuestoPorDefecto(Usuario usuario, Double nuevoPorDefecto) {
    Map<YearMonth, Double> asignadoPorMes = new TreeMap<>();
    for (Presupuesto presupuesto : repositorioPresupuesto.obtenerDelUsuario(usuario)) {
      asignadoPorMes.merge(presupuesto.getMes(), presupuesto.getMonto(), Double::sum);
    }
    // Los meses ya cerrados no se pueden reasignar, así que no frenan el cambio
    YearMonth mesActual = YearMonth.now(reloj);
    for (Map.Entry<YearMonth, Double> mes : asignadoPorMes.entrySet()) {
      if (mes.getKey().isBefore(mesActual)) {
        continue;
      }
      boolean usaElDefecto = repositorioPresupuesto.buscarTotal(usuario, mes.getKey()) == null;
      if (usaElDefecto && mes.getValue() > nuevoPorDefecto) {
        throw new PresupuestoInvalido(
          "El presupuesto por defecto no puede ser menor a lo que ya asignaste en " +
          NombreDeMes.de(mes.getKey())
        );
      }
    }
  }

  @Override
  public Double noAsignadoDelMes(Usuario usuario, YearMonth mes) {
    return presupuestoDelMes(usuario, mes) - totalDelMes(usuario, mes);
  }
}
