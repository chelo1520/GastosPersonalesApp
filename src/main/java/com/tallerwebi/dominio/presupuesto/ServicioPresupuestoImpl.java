package com.tallerwebi.dominio.presupuesto;

import com.tallerwebi.dominio.NombreDeMes;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.excepcion.PresupuestoInvalido;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
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

  private static final double PORCENTAJE_MAXIMO = 100.0;
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
    validarCategoriaYMes(categoria, mes);
    if (!esPositivoFinito(monto)) {
      throw new PresupuestoInvalido("El monto debe ser mayor a cero");
    }
    guardarAsignacion(usuario, categoria, mes, monto, null);
  }

  @Override
  public void definirPorcentaje(
    Usuario usuario,
    Categoria categoria,
    YearMonth mes,
    Double porcentaje
  ) {
    validarCategoriaYMes(categoria, mes);
    if (!esPositivoFinito(porcentaje) || porcentaje > PORCENTAJE_MAXIMO) {
      throw new PresupuestoInvalido("El porcentaje debe ser mayor a cero y como máximo 100");
    }
    double total = presupuestoDelMes(usuario, mes);
    if (total <= 0) {
      throw new PresupuestoInvalido("Definí primero un presupuesto total para este mes");
    }
    double monto = calcularPorcentaje(total, porcentaje);
    if (monto <= 0) {
      throw new PresupuestoInvalido("El porcentaje debe equivaler al menos a un centavo");
    }
    guardarAsignacion(usuario, categoria, mes, monto, porcentaje);
  }

  private boolean esPositivoFinito(Double valor) {
    return valor != null && Double.isFinite(valor) && valor > 0;
  }

  private void validarCategoriaYMes(Categoria categoria, YearMonth mes) {
    if (categoria == null) {
      throw new PresupuestoInvalido("Elegí una categoría");
    }
    if (mes == null) {
      throw new PresupuestoInvalido("El mes es obligatorio");
    }
  }

  private void guardarAsignacion(
    Usuario usuario,
    Categoria categoria,
    YearMonth mes,
    Double monto,
    Double porcentaje
  ) {
    Presupuesto existente = repositorioPresupuesto.buscar(usuario, categoria, mes);
    validarQueNoSuperaElPresupuestoMensual(usuario, mes, existente, monto);

    if (existente != null) {
      existente.setMonto(monto);
      existente.setPorcentaje(porcentaje);
      repositorioPresupuesto.modificar(existente);
      return;
    }

    Presupuesto nuevo = new Presupuesto(usuario, categoria, mes, monto);
    nuevo.setPorcentaje(porcentaje);
    repositorioPresupuesto.guardar(nuevo);
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
    double anterior = existente == null ? 0.0 : montoConTotal(existente, presupuestoDelMes);
    BigDecimal asignado = BigDecimal
      .valueOf(totalDelMes(usuario, mes))
      .subtract(BigDecimal.valueOf(anterior))
      .add(BigDecimal.valueOf(monto));
    if (asignado.compareTo(BigDecimal.valueOf(presupuestoDelMes)) > 0) {
      throw new PresupuestoInvalido("El monto supera tu presupuesto mensual disponible");
    }
  }

  @Override
  public Presupuesto buscar(Usuario usuario, Categoria categoria, YearMonth mes) {
    Presupuesto presupuesto = repositorioPresupuesto.buscar(usuario, categoria, mes);
    return presupuesto == null
      ? null
      : conMontoCalculado(presupuesto, presupuestoDelMes(usuario, mes));
  }

  @Override
  public List<Presupuesto> obtenerDelMes(Usuario usuario, YearMonth mes) {
    double total = presupuestoDelMes(usuario, mes);
    List<Presupuesto> resultado = new ArrayList<>();
    for (Presupuesto presupuesto : repositorioPresupuesto.obtenerDelMes(usuario, mes)) {
      resultado.add(conMontoCalculado(presupuesto, total));
    }
    return resultado;
  }

  private Presupuesto conMontoCalculado(Presupuesto presupuesto, double total) {
    if (presupuesto.getPorcentaje() == null) {
      return presupuesto;
    }
    Presupuesto calculado = new Presupuesto(
      presupuesto.getUsuario(),
      presupuesto.getCategoria(),
      presupuesto.getMes(),
      montoConTotal(presupuesto, total)
    );
    calculado.setId(presupuesto.getId());
    calculado.setPorcentaje(presupuesto.getPorcentaje());
    return calculado;
  }

  private double montoConTotal(Presupuesto presupuesto, double total) {
    return presupuesto.getPorcentaje() == null
      ? presupuesto.getMonto()
      : calcularPorcentaje(total, presupuesto.getPorcentaje());
  }

  private double calcularPorcentaje(double total, double porcentaje) {
    return BigDecimal
      .valueOf(total)
      .multiply(BigDecimal.valueOf(porcentaje))
      .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
      .doubleValue();
  }

  @Override
  public Double totalDelMes(Usuario usuario, YearMonth mes) {
    BigDecimal total = BigDecimal.ZERO;
    for (Presupuesto presupuesto : obtenerDelMes(usuario, mes)) {
      total = total.add(BigDecimal.valueOf(presupuesto.getMonto()));
    }
    return total.doubleValue();
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
    if (!esPositivoFinito(monto)) {
      throw new PresupuestoInvalido("El monto debe ser mayor a cero");
    }
    if (
      monto < totalConNuevoPresupuesto(repositorioPresupuesto.obtenerDelMes(usuario, mes), monto)
    ) {
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
    Map<YearMonth, List<Presupuesto>> asignadoPorMes = new TreeMap<>();
    for (Presupuesto presupuesto : repositorioPresupuesto.obtenerDelUsuario(usuario)) {
      asignadoPorMes
        .computeIfAbsent(presupuesto.getMes(), clave -> new ArrayList<>())
        .add(presupuesto);
    }
    // Los meses ya cerrados no se pueden reasignar, así que no frenan el cambio
    YearMonth mesActual = YearMonth.now(reloj);
    for (Map.Entry<YearMonth, List<Presupuesto>> mes : asignadoPorMes.entrySet()) {
      if (mes.getKey().isBefore(mesActual)) {
        continue;
      }
      boolean usaElDefecto = repositorioPresupuesto.buscarTotal(usuario, mes.getKey()) == null;
      if (
        usaElDefecto && totalConNuevoPresupuesto(mes.getValue(), nuevoPorDefecto) > nuevoPorDefecto
      ) {
        throw new PresupuestoInvalido(
          "El presupuesto por defecto no puede ser menor a lo que ya asignaste en " +
          NombreDeMes.de(mes.getKey())
        );
      }
    }
  }

  private double totalConNuevoPresupuesto(List<Presupuesto> presupuestos, double total) {
    BigDecimal asignado = BigDecimal.ZERO;
    for (Presupuesto presupuesto : presupuestos) {
      double monto = montoConTotal(presupuesto, total);
      if (presupuesto.getPorcentaje() != null && monto <= 0) {
        throw new PresupuestoInvalido("El total es demasiado bajo para el porcentaje asignado");
      }
      asignado = asignado.add(BigDecimal.valueOf(monto));
    }
    return asignado.doubleValue();
  }

  @Override
  public List<MesDePresupuesto> resumenDelAnio(Usuario usuario, int anio) {
    List<MesDePresupuesto> meses = new ArrayList<>();
    for (int numero = 1; numero <= 12; numero++) {
      YearMonth mes = YearMonth.of(anio, numero);
      PresupuestoTotal propio = repositorioPresupuesto.buscarTotal(usuario, mes);
      double total = propio != null ? propio.getMonto() : usuario.presupuestoSegunPerfil();
      meses.add(new MesDePresupuesto(mes, total, propio != null, totalDelMes(usuario, mes)));
    }
    return meses;
  }

  @Override
  public Double noAsignadoDelMes(Usuario usuario, YearMonth mes) {
    return presupuestoDelMes(usuario, mes) - totalDelMes(usuario, mes);
  }
}
