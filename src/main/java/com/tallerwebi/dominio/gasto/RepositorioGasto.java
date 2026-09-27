package com.tallerwebi.dominio.gasto;

import java.time.LocalDate;
import java.util.List;

import com.tallerwebi.dominio.Usuario;

/**
 * RepositorioGasto
 */
public interface RepositorioGasto {

    void guardar(Gasto gasto);

    List<Gasto> BuscarGastosPorUsuario(Usuario usuario);

    List<Gasto> obtenerGastosEntreFechas( Usuario usuario, LocalDate desde, LocalDate hasta);
}
