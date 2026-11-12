package com.tallerwebi.dominio.simulacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.dominio.Usuario;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import java.time.YearMonth;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ServicioSimulacion {

  private final SessionFactory sessionFactory;
  private final ObjectMapper mapper = new ObjectMapper();

  public ServicioSimulacion(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  public EstadoSimulacion obtener(Usuario usuario, YearMonth base, YearMonth simulado) {
    SimulacionGuardada guardada = buscar(usuario, base, simulado);
    if (guardada == null) return EstadoSimulacion.vacio();
    try {
      return mapper.readValue(guardada.getEstado(), EstadoSimulacion.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("No se pudo leer la simulación guardada", e);
    }
  }

  public boolean existe(Usuario usuario, YearMonth base, YearMonth simulado) {
    return buscar(usuario, base, simulado) != null;
  }

  public List<YearMonth> mesesBaseGuardados(Usuario usuario) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "select distinct mesBase FROM SimulacionGuardada WHERE usuario = :usuario",
        String.class
      )
      .setParameter("usuario", usuario)
      .getResultList()
      .stream()
      .map(YearMonth::parse)
      .toList();
  }

  public List<YearMonth> mesesSimuladosGuardados(Usuario usuario, YearMonth base) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "select mesSimulado FROM SimulacionGuardada WHERE usuario = :usuario AND mesBase = :base",
        String.class
      )
      .setParameter("usuario", usuario)
      .setParameter("base", base.toString())
      .getResultList()
      .stream()
      .map(YearMonth::parse)
      .toList();
  }

  public void guardar(
    Usuario usuario,
    YearMonth base,
    YearMonth simulado,
    EstadoSimulacion estado
  ) {
    validar(estado);
    Usuario propietario = sessionFactory
      .getCurrentSession()
      .find(Usuario.class, usuario.getId(), LockModeType.PESSIMISTIC_WRITE);
    if (propietario == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    try {
      String json = mapper.writeValueAsString(estado);
      SimulacionGuardada guardada = buscar(propietario, base, simulado);
      if (guardada == null) {
        sessionFactory
          .getCurrentSession()
          .persist(new SimulacionGuardada(propietario, base, simulado, json));
      } else {
        guardada.setEstado(json);
      }
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("No se pudo guardar la simulación", e);
    }
  }

  private SimulacionGuardada buscar(Usuario usuario, YearMonth base, YearMonth simulado) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "FROM SimulacionGuardada WHERE usuario = :usuario AND mesBase = :base AND mesSimulado = :simulado",
        SimulacionGuardada.class
      )
      .setParameter("usuario", usuario)
      .setParameter("base", base.toString())
      .setParameter("simulado", simulado.toString())
      .uniqueResult();
  }

  private void validar(EstadoSimulacion estado) {
    if (
      estado == null ||
      estado.excluidos() == null ||
      estado.agregados() == null ||
      estado.excluidos().stream().anyMatch(id -> id == null || id <= 0)
    ) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de simulación inválido");
    }
    validarBorradores(estado);
    for (EstadoSimulacion.GastoSimulado gasto : estado.agregados()) {
      validarGasto(gasto);
    }
  }

  private void validarBorradores(EstadoSimulacion estado) {
    if (
      !textoValido(estado.descripcionExtra(), 80) ||
      !textoValido(estado.descripcionOrdinario(), 80) ||
      !textoValido(estado.montoExtra(), 40) ||
      !textoValido(estado.montoOrdinario(), 40)
    ) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Borrador de simulación inválido");
    }
  }

  private void validarGasto(EstadoSimulacion.GastoSimulado gasto) {
    if (
      gasto == null ||
      !textoValido(gasto.descripcion(), 80) ||
      gasto.descripcion().isBlank() ||
      !importeValido(gasto.importe()) ||
      !("ordinario".equals(gasto.tipo()) || "extraordinario".equals(gasto.tipo()))
    ) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gasto simulado inválido");
    }
  }

  private boolean importeValido(Double importe) {
    return importe != null && Double.isFinite(importe) && importe > 0;
  }

  private boolean textoValido(String texto, int maximo) {
    return texto != null && texto.length() <= maximo;
  }
}
