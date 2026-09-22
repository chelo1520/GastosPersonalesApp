package com.tallerwebi.dominio;

import java.util.List;

import com.tallerwebi.dominio.gasto.Gasto;

/**
 * RepositorioGasto
 */
public interface RepositorioGasto {

    void guardar(Gasto gasto);

    List<Gasto> BuscarGastosPorUsuario(Usuario usuario);

}
