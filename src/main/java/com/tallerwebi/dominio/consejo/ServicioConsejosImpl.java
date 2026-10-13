package com.tallerwebi.dominio.consejo;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.asistente.CategoriaGastoReal;
import com.tallerwebi.dominio.asistente.EstadoGastoReal;
import com.tallerwebi.dominio.asistente.GastoReal;
import com.tallerwebi.dominio.asistente.ServicioGastoReal;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.excepcion.ConsejoNoEncontrado;
import com.tallerwebi.dominio.indicadores.ServicioIndicadores;
import com.tallerwebi.dominio.presupuesto.Presupuesto;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
import com.tallerwebi.dominio.resumen.ResumenCategoria;
import com.tallerwebi.dominio.resumen.ServicioResumen;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ServicioConsejosImpl: tres reglas, todas deterministas y sin IA. HU-15 solo redacta.
 */
@Service
@Transactional
public class ServicioConsejosImpl implements ServicioConsejos {

  // Un presupuesto sin cambios que perdió al menos esto de poder de compra merece aviso
  static final double UMBRAL_PRESUPUESTO_DESACTUALIZADO = 5.0;
  // Una categoría que sube más que la inflación por encima de esto, dos meses seguidos
  static final double UMBRAL_SUBA_REAL = 5.0;
  // Como mucho se muestran estos consejos por mes, los más importantes primero
  static final int MAXIMO_CONSEJOS = 4;
  private static final int MESES_HACIA_ATRAS = 24;
  private static final int MESES_PARA_LA_TENDENCIA = 3;
  private static final double REDONDEO_SUGERIDO = 1000.0;
  private static final Locale ARGENTINA = Locale.of("es", "AR");

  private final RepositorioConsejo repositorioConsejo;
  private final ServicioPresupuesto servicioPresupuesto;
  private final ServicioIndicadores servicioIndicadores;
  private final ServicioResumen servicioResumen;
  private final ServicioGastoReal servicioGastoReal;
  private final Clock reloj;

  @Autowired
  public ServicioConsejosImpl(
    RepositorioConsejo repositorioConsejo,
    ServicioPresupuesto servicioPresupuesto,
    ServicioIndicadores servicioIndicadores,
    ServicioResumen servicioResumen,
    ServicioGastoReal servicioGastoReal
  ) {
    this(
      repositorioConsejo,
      servicioPresupuesto,
      servicioIndicadores,
      servicioResumen,
      servicioGastoReal,
      Clock.systemDefaultZone()
    );
  }

  // Permite fijar "hoy" en los tests
  public ServicioConsejosImpl(
    RepositorioConsejo repositorioConsejo,
    ServicioPresupuesto servicioPresupuesto,
    ServicioIndicadores servicioIndicadores,
    ServicioResumen servicioResumen,
    ServicioGastoReal servicioGastoReal,
    Clock reloj
  ) {
    this.repositorioConsejo = repositorioConsejo;
    this.servicioPresupuesto = servicioPresupuesto;
    this.servicioIndicadores = servicioIndicadores;
    this.servicioResumen = servicioResumen;
    this.servicioGastoReal = servicioGastoReal;
    this.reloj = reloj;
  }

  @Override
  public List<Consejo> obtenerPendientes(Usuario usuario) {
    YearMonth mesActual = YearMonth.now(reloj);
    List<Consejo> guardados = repositorioConsejo.obtenerDelMes(usuario, mesActual);

    List<Consejo> calculados = new ArrayList<>();
    calculados.addAll(presupuestosDesactualizados(usuario, mesActual));
    calculados.addAll(categoriasQueSubenMasQueLaInflacion(usuario, mesActual));
    GastoReal gastoReal = servicioGastoReal.calcular(
      usuario,
      ServicioGastoReal.PERIODO_POR_DEFECTO
    );
    Consejo enAlza = gastoRealEnAlza(usuario, mesActual, gastoReal);
    if (enAlza != null) {
      calculados.add(enAlza);
    }

    // Un consejo ya guardado (por su tipo y categoría) conserva su estado; los nuevos se guardan
    List<Consejo> pendientes = new ArrayList<>();
    for (Consejo calculado : calculados) {
      Consejo guardado = buscarIgual(guardados, calculado);
      if (guardado == null) {
        repositorioConsejo.guardar(calculado);
        pendientes.add(calculado);
      } else if (guardado.getEstado() == EstadoConsejo.PENDIENTE) {
        guardado.setTexto(calculado.getTexto());
        guardado.setMontoSugerido(calculado.getMontoSugerido());
        repositorioConsejo.modificar(guardado);
        pendientes.add(guardado);
      }
    }
    pendientes.sort(Comparator.comparing(Consejo::getSeveridad).reversed());
    return pendientes.size() > MAXIMO_CONSEJOS
      ? pendientes.subList(0, MAXIMO_CONSEJOS)
      : pendientes;
  }

  @Override
  public void descartar(Long id, Usuario usuario) {
    Consejo consejo = consejoDelUsuario(id, usuario);
    consejo.setEstado(EstadoConsejo.DESCARTADO);
    repositorioConsejo.modificar(consejo);
  }

  @Override
  public void aplicar(Long id, Usuario usuario) {
    Consejo consejo = consejoDelUsuario(id, usuario);
    if (!consejo.isAplicable()) {
      throw new ConsejoNoEncontrado();
    }
    Categoria categoria = categoriaDelPresupuesto(
      usuario,
      consejo.getMes(),
      consejo.getCategoria()
    );
    servicioPresupuesto.definir(usuario, categoria, consejo.getMes(), consejo.getMontoSugerido());
    consejo.setEstado(EstadoConsejo.APLICADO);
    repositorioConsejo.modificar(consejo);
  }

  // Regla 1: el presupuesto de una categoría sigue igual desde hace meses y la inflación lo erosionó
  private List<Consejo> presupuestosDesactualizados(Usuario usuario, YearMonth mesActual) {
    List<Consejo> consejos = new ArrayList<>();
    for (Presupuesto presupuesto : servicioPresupuesto.obtenerDelMes(usuario, mesActual)) {
      YearMonth desde = desdeCuandoVale(usuario, presupuesto, mesActual);
      if (desde.equals(mesActual)) {
        continue;
      }
      double acumulada = inflacionAcumulada(desde.plusMonths(1), mesActual);
      if (acumulada < UMBRAL_PRESUPUESTO_DESACTUALIZADO) {
        continue;
      }
      double sugerido =
        Math.ceil((presupuesto.getMonto() * (1 + acumulada / 100)) / REDONDEO_SUGERIDO) *
        REDONDEO_SUGERIDO;
      String nombre = presupuesto.getCategoria().getNombre();
      String texto = String.format(
        ARGENTINA,
        "Tu presupuesto de %s ($ %,.0f) es el mismo desde %s y la inflación desde entonces fue %.1f %%. Para que compre lo mismo, llevalo a $ %,.0f.",
        nombre,
        presupuesto.getMonto(),
        nombreDeMes(desde),
        acumulada,
        sugerido
      );
      consejos.add(
        new Consejo(
          usuario,
          mesActual,
          TipoConsejo.PRESUPUESTO_DESACTUALIZADO,
          Severidad.ATENCION,
          nombre,
          texto,
          sugerido
        )
      );
    }
    return consejos;
  }

  // Mes más antiguo, hacia atrás y sin cortes, en que la categoría tiene este mismo monto
  private YearMonth desdeCuandoVale(Usuario usuario, Presupuesto presupuesto, YearMonth mesActual) {
    YearMonth desde = mesActual;
    for (int i = 0; i < MESES_HACIA_ATRAS; i++) {
      Presupuesto anterior = servicioPresupuesto.buscar(
        usuario,
        presupuesto.getCategoria(),
        desde.minusMonths(1)
      );
      if (anterior == null || !Objects.equals(anterior.getMonto(), presupuesto.getMonto())) {
        break;
      }
      desde = desde.minusMonths(1);
    }
    return desde;
  }

  private double inflacionAcumulada(YearMonth desde, YearMonth hasta) {
    double acumulada = 1.0;
    for (YearMonth mes = desde; !mes.isAfter(hasta); mes = mes.plusMonths(1)) {
      acumulada *= 1 + servicioIndicadores.inflacionMensual(mes).getPorcentaje() / 100;
    }
    return (acumulada - 1) * 100;
  }

  // Regla 2: una categoría subió más que la inflación en los dos últimos meses cerrados
  private List<Consejo> categoriasQueSubenMasQueLaInflacion(Usuario usuario, YearMonth mesActual) {
    YearMonth ultimo = mesActual.minusMonths(1);
    YearMonth anteultimo = ultimo.minusMonths(1);
    Map<String, CategoriaGastoReal> ultimoMes = variacionesPorCategoria(usuario, ultimo);
    Map<String, CategoriaGastoReal> anteultimoMes = variacionesPorCategoria(usuario, anteultimo);

    List<Consejo> consejos = new ArrayList<>();
    for (CategoriaGastoReal cat : ultimoMes.values()) {
      CategoriaGastoReal previa = anteultimoMes.get(cat.getNombre());
      if (!subeEnSerio(cat) || previa == null || !subeEnSerio(previa)) {
        continue;
      }
      String texto = String.format(
        ARGENTINA,
        "%s viene subiendo más que la inflación: %+.1f %% real en %s y %+.1f %% en %s. Vale la pena mirar qué cambió ahí.",
        cat.getNombre(),
        previa.getVariacionReal(),
        nombreDeMes(anteultimo),
        cat.getVariacionReal(),
        nombreDeMes(ultimo)
      );
      consejos.add(
        new Consejo(
          usuario,
          mesActual,
          TipoConsejo.CATEGORIA_SUBE_MAS_QUE_LA_INFLACION,
          Severidad.ALERTA,
          cat.getNombre(),
          texto,
          null
        )
      );
    }
    return consejos;
  }

  private boolean subeEnSerio(CategoriaGastoReal categoria) {
    return categoria.getVariacionReal() != null && categoria.getVariacionReal() > UMBRAL_SUBA_REAL;
  }

  // Variación real de cada categoría de un mes contra el mes anterior
  private Map<String, CategoriaGastoReal> variacionesPorCategoria(Usuario usuario, YearMonth mes) {
    Map<String, Double> actual = gastadoPorCategoria(usuario, mes);
    Map<String, Double> anterior = gastadoPorCategoria(usuario, mes.minusMonths(1));
    double ipc = servicioIndicadores.inflacionMensual(mes).getPorcentaje();
    Map<String, CategoriaGastoReal> variaciones = new LinkedHashMap<>();
    for (Map.Entry<String, Double> cat : actual.entrySet()) {
      variaciones.put(
        cat.getKey(),
        new CategoriaGastoReal(
          cat.getKey(),
          cat.getValue(),
          anterior.getOrDefault(cat.getKey(), 0.0),
          ipc
        )
      );
    }
    return variaciones;
  }

  private Map<String, Double> gastadoPorCategoria(Usuario usuario, YearMonth mes) {
    Map<String, Double> porCategoria = new LinkedHashMap<>();
    for (ResumenCategoria categoria : servicioResumen
      .obtenerResumen(usuario, mes)
      .getCategorias()) {
      if (categoria.getGastado() > 0) {
        porCategoria.put(categoria.getNombre(), categoria.getGastado());
      }
    }
    return porCategoria;
  }

  // Regla 3: el veredicto del período está en rojo
  private Consejo gastoRealEnAlza(Usuario usuario, YearMonth mesActual, GastoReal gastoReal) {
    if (
      gastoReal.getVeredicto() == null ||
      gastoReal.getVeredicto().getEstado() != EstadoGastoReal.ROJO
    ) {
      return null;
    }
    String texto = String.format(
      ARGENTINA,
      "En los últimos %d meses tus gastos crecieron %.1f %% por encima de la inflación. No es solo que todo está más caro: estás consumiendo más.",
      MESES_PARA_LA_TENDENCIA + MESES_PARA_LA_TENDENCIA,
      gastoReal.getVeredicto().getVariacionReal()
    );
    return new Consejo(
      usuario,
      mesActual,
      TipoConsejo.GASTO_REAL_EN_ALZA,
      Severidad.ATENCION,
      null,
      texto,
      null
    );
  }

  private Consejo buscarIgual(List<Consejo> guardados, Consejo calculado) {
    for (Consejo guardado : guardados) {
      if (
        guardado.getTipo() == calculado.getTipo() &&
        Objects.equals(guardado.getCategoria(), calculado.getCategoria())
      ) {
        return guardado;
      }
    }
    return null;
  }

  // Un consejo de otro usuario se trata igual que uno inexistente
  private Consejo consejoDelUsuario(Long id, Usuario usuario) {
    Consejo consejo = repositorioConsejo.buscarPorId(id);
    if (
      consejo == null ||
      usuario == null ||
      consejo.getUsuario() == null ||
      !Objects.equals(consejo.getUsuario().getId(), usuario.getId())
    ) {
      throw new ConsejoNoEncontrado();
    }
    return consejo;
  }

  private Categoria categoriaDelPresupuesto(Usuario usuario, YearMonth mes, String nombre) {
    for (Presupuesto presupuesto : servicioPresupuesto.obtenerDelMes(usuario, mes)) {
      if (presupuesto.getCategoria().getNombre().equals(nombre)) {
        return presupuesto.getCategoria();
      }
    }
    throw new ConsejoNoEncontrado();
  }

  private String nombreDeMes(YearMonth mes) {
    String nombre = mes.getMonth().getDisplayName(java.time.format.TextStyle.FULL, ARGENTINA);
    return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1) + " " + mes.getYear();
  }
}
