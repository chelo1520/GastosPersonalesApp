package com.tallerwebi.dominio.gasto;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.GastoInvalidoExeption;

/**
 * GastoServicioImpl
 */
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

}
