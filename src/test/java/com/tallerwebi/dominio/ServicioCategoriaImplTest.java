package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.categoria.CategoriaRepositorio;
import com.tallerwebi.dominio.categoria.MarcasHormiga;
import com.tallerwebi.dominio.categoria.PreferenciaCategoria;
import com.tallerwebi.dominio.categoria.RepositorioPreferenciaCategoria;
import com.tallerwebi.dominio.categoria.ServicioCategoria;
import com.tallerwebi.dominio.categoria.ServicioCategoriaImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ServicioCategoriaImplTest {

  private CategoriaRepositorio categoriaRepositorio;
  private RepositorioPreferenciaCategoria repositorioPreferencia;
  private ServicioCategoria servicio;
  private Usuario usuario;
  private Categoria delivery;

  @BeforeEach
  void init() {
    categoriaRepositorio = mock(CategoriaRepositorio.class);
    repositorioPreferencia = mock(RepositorioPreferenciaCategoria.class);
    servicio = new ServicioCategoriaImpl(categoriaRepositorio, repositorioPreferencia);
    usuario = new Usuario();
    delivery = new Categoria();
    delivery.setId(7L);
    delivery.setEsencial(false);
    when(categoriaRepositorio.buscarPorId(7L)).thenReturn(delivery);
  }

  @Test
  void debeGuardarUnaPreferenciaEsencialCuandoElUsuarioQuitaUnaCategoriaDeHormiga() {
    servicio.quitarDeHormiga(usuario, 7L);

    ArgumentCaptor<PreferenciaCategoria> guardada = ArgumentCaptor.forClass(
      PreferenciaCategoria.class
    );
    verify(repositorioPreferencia).guardar(guardada.capture());
    assertTrue(guardada.getValue().getEsencial());
    assertTrue(guardada.getValue().getUsuario() == usuario);
    assertTrue(guardada.getValue().getCategoria() == delivery);
    // La categoría del maestro no se toca: sigue siendo prescindible para los demás
    assertFalse(delivery.isEsencial());
    verify(categoriaRepositorio, never()).modificar(any());
  }

  @Test
  void debeActualizarLaPreferenciaCuandoYaExiste() {
    PreferenciaCategoria existente = new PreferenciaCategoria(usuario, delivery, true);
    when(repositorioPreferencia.buscar(usuario, delivery)).thenReturn(existente);

    servicio.marcarComoHormiga(usuario, 7L);

    assertFalse(existente.getEsencial());
    verify(repositorioPreferencia).modificar(existente);
    verify(repositorioPreferencia, never()).guardar(any());
  }

  @Test
  void noDebeGuardarNadaParaUnaCategoriaFija() {
    Categoria salud = new Categoria();
    salud.setId(3L);
    salud.setFija(true);
    when(categoriaRepositorio.buscarPorId(3L)).thenReturn(salud);

    servicio.marcarComoHormiga(usuario, 3L);

    verify(repositorioPreferencia, never()).guardar(any());
    verify(repositorioPreferencia, never()).modificar(any());
  }

  @Test
  void noDebeGuardarNadaParaUnaCategoriaInexistente() {
    servicio.quitarDeHormiga(usuario, 99L);

    verify(repositorioPreferencia, never()).guardar(any());
  }

  @Test
  void lasMarcasDelUsuarioSalenDeSusPreferencias() {
    when(repositorioPreferencia.obtenerDelUsuario(usuario))
      .thenReturn(List.of(new PreferenciaCategoria(usuario, delivery, true)));

    MarcasHormiga marcas = servicio.marcasDe(usuario);

    assertFalse(marcas.esPrescindible(delivery));
    assertTrue(MarcasHormiga.porDefecto().esPrescindible(delivery));
  }
}
