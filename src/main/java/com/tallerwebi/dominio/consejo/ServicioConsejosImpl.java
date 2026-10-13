package com.tallerwebi.dominio.consejo;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.asistente.CategoriaGastoReal;
import com.tallerwebi.dominio.asistente.GastoReal;
import com.tallerwebi.dominio.asistente.ServicioAsistente;
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
 * ServicioConsejosImpl: las lecturas del período (HU-16) más las reglas con acción, todo
 * determinista y sin IA. HU-15 solo redacta.
 */
@Service
@Transactional
public class ServicioConsejosImpl implements ServicioConsejos {

  // Un presupuesto sin cambios que perdió al menos esto de poder de compra merece aviso
  static final double UMBRAL_PRESUPUESTO_DESACTUALIZADO = 5.0;
  // Una categoría que sube más que la inflación por encima de esto, dos meses seguidos
  static final double UMBRAL_SUBA_REAL = 5.0;
  // Como mucho se muestran estas lecturas además del veredicto, las más importantes primero
  static final int MAXIMO_LECTURAS = 3;
  // Y una sola propuesta por vez: el usuario la aplica o la descarta y recién entonces aparece la siguiente
  static final int MAXIMO_PROPUESTAS = 1;
  private static final int MESES_HACIA_ATRAS = 24;
  private static final double REDONDEO_SUGERIDO = 1000.0;
  private static final Locale ARGENTINA = Locale.of("es", "AR");

  private final RepositorioConsejo repositorioConsejo;
  private final ServicioPresupuesto servicioPresupuesto;
  private final ServicioIndicadores servicioIndicadores;
  private final ServicioResumen servicioResumen;
  private final ServicioGastoReal servicioGastoReal;
  private final ServicioAsistente servicioAsistente;
  private final Clock reloj;
  private final LecturasDelPeriodo lecturas = new LecturasDelPeriodo();

  @Autowired
  public ServicioConsejosImpl(
    RepositorioConsejo repositorioConsejo,
    ServicioPresupuesto servicioPresupuesto,
    ServicioIndicadores servicioIndicadores,
    ServicioResumen servicioResumen,
    ServicioGastoReal servicioGastoReal,
    ServicioAsistente servicioAsistente
  ) {
    this(
      repositorioConsejo,
      servicioPresupuesto,
      servicioIndicadores,
      servicioResumen,
      servicioGastoReal,
      servicioAsistente,
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
    ServicioAsistente servicioAsistente,
    Clock reloj
  ) {
    this.repositorioConsejo = repositorioConsejo;
    this.servicioPresupuesto = servicioPresupuesto;
    this.servicioIndicadores = servicioIndicadores;
    this.servicioResumen = servicioResumen;
    this.servicioGastoReal = servicioGastoReal;
    this.servicioAsistente = servicioAsistente;
    this.reloj = reloj;
  }

  @Override
  public List<Consejo> obtenerPendientes(Usuario usuario) {
    return obtenerPendientes(
      usuario,
      servicioGastoReal.calcular(usuario, ServicioGastoReal.PERIODO_POR_DEFECTO)
    );
  }

  @Override
  public List<Consejo> obtenerPendientes(Usuario usuario, GastoReal gastoReal) {
    YearMonth mesActual = YearMonth.now(reloj);
    List<Consejo> guardados = repositorioConsejo.obtenerDelMes(usuario, mesActual);

    List<Consejo> calculados = new ArrayList<>(lecturas.calcular(usuario, mesActual, gastoReal));
    calculados.addAll(categoriasQueSubenMasQueLaInflacion(usuario, mesActual));
    calculados.addAll(presupuestosDesactualizados(usuario, mesActual));

    List<Consejo> pendientes = conservarEstado(calculados, guardados);
    List<Consejo> paraMostrar = recortar(pendientes);

    // Si cambiaron los hechos, la síntesis guardada ya no vale: vuelve el texto de las reglas
    Consejo veredicto = veredictoDe(paraMostrar);
    if (
      veredicto != null &&
      veredicto.getTextoRedactado() != null &&
      !firmaDelPeriodo(paraMostrar).equals(veredicto.getFirmaHechos())
    ) {
      veredicto.redactar(null, null);
      repositorioConsejo.modificar(veredicto);
    }
    return paraMostrar;
  }

  // Una sola síntesis del período, escrita por la IA sobre estos hechos y guardada en el veredicto;
  // se vuelve a pedir solo cuando cambian los hechos. Las demás tarjetas muestran el texto de las reglas
  @Override
  public String sintetizar(Usuario usuario, GastoReal gastoReal) {
    List<Consejo> paraMostrar = obtenerPendientes(usuario, gastoReal);
    Consejo veredicto = veredictoDe(paraMostrar);
    if (veredicto == null) {
      return null;
    }
    if (veredicto.getTextoRedactado() == null) {
      String sintesis = servicioAsistente.sintetizar(paraMostrar, gastoReal);
      if (sintesis != null && !sintesis.isBlank()) {
        veredicto.redactar(sintesis.trim(), firmaDelPeriodo(paraMostrar));
        repositorioConsejo.modificar(veredicto);
      }
    }
    return veredicto.getTextoRedactado();
  }

  @Override
  public boolean faltaLaSintesis(List<Consejo> consejos) {
    Consejo veredicto = veredictoDe(consejos);
    return (
      veredicto != null && veredicto.getTextoRedactado() == null && servicioAsistente.disponible()
    );
  }

  private static Consejo veredictoDe(List<Consejo> consejos) {
    return !consejos.isEmpty() && consejos.get(0).isVeredicto() ? consejos.get(0) : null;
  }

  private static String firmaDelPeriodo(List<Consejo> consejos) {
    StringBuilder hechos = new StringBuilder();
    for (Consejo consejo : consejos) {
      hechos.append(consejo.firmaDeLosHechos()).append('|');
    }
    return Integer.toHexString(hechos.toString().hashCode());
  }

  // Un consejo ya guardado (por su tipo y categoría) conserva su estado; los nuevos se guardan
  private List<Consejo> conservarEstado(List<Consejo> calculados, List<Consejo> guardados) {
    List<Consejo> pendientes = new ArrayList<>();
    for (Consejo calculado : calculados) {
      Consejo guardado = buscarIgual(guardados, calculado);
      if (guardado == null) {
        repositorioConsejo.guardar(calculado);
        pendientes.add(calculado);
      } else if (guardado.getEstado() == EstadoConsejo.PENDIENTE) {
        guardado.setTexto(calculado.getTexto());
        guardado.setMontoSugerido(calculado.getMontoSugerido());
        if (!guardado.isVeredicto()) {
          // Solo el veredicto lleva síntesis; una redacción vieja por tarjeta ya no se muestra
          guardado.redactar(null, null);
        }
        repositorioConsejo.modificar(guardado);
        pendientes.add(guardado);
      }
    }
    return pendientes;
  }

  // El veredicto siempre va y no cuenta para el tope; después las lecturas y por último las propuestas
  private static List<Consejo> recortar(List<Consejo> pendientes) {
    Consejo veredicto = null;
    List<Consejo> lecturas = new ArrayList<>();
    List<Consejo> propuestas = new ArrayList<>();
    for (Consejo pendiente : pendientes) {
      if (pendiente.isVeredicto()) {
        veredicto = pendiente;
      } else if (pendiente.isLectura()) {
        lecturas.add(pendiente);
      } else {
        propuestas.add(pendiente);
      }
    }
    List<Consejo> paraMostrar = new ArrayList<>();
    if (veredicto != null) {
      paraMostrar.add(veredicto);
    }
    paraMostrar.addAll(losMasImportantes(lecturas, MAXIMO_LECTURAS));
    paraMostrar.addAll(losMasImportantes(propuestas, MAXIMO_PROPUESTAS));
    return paraMostrar;
  }

  private static List<Consejo> losMasImportantes(List<Consejo> consejos, int maximo) {
    consejos.sort(Comparator.comparing(Consejo::getSeveridad).reversed());
    return consejos.size() > maximo ? new ArrayList<>(consejos.subList(0, maximo)) : consejos;
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
