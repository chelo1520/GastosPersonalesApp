package com.tallerwebi.punta_a_punta;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ReiniciarDB {

  public static void limpiarBaseDeDatos() {
    try {
      String dbHost = System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "localhost";
      String dbPort = System.getenv("DB_PORT") != null ? System.getenv("DB_PORT") : "3306";
      String dbName = System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "tallerwebi";
      String dbUser = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "user";
      String dbPassword = System.getenv("DB_PASSWORD") != null
        ? System.getenv("DB_PASSWORD")
        : "user";

      // Primero los gastos: tienen una FK a Usuario y bloquearían el borrado de usuarios
      String sqlCommands =
        "DELETE FROM Gasto;\n" +
        "ALTER TABLE Gasto AUTO_INCREMENT = 1;\n" +
        "DELETE FROM Usuario;\n" +
        "ALTER TABLE Usuario AUTO_INCREMENT = 1;\n" +
        "INSERT INTO Usuario(id, email, password, rol, activo) VALUES(null, 'test@unlam.edu.ar', 'test', 'ADMIN', true);";

      // Se invoca docker directamente (sin /bin/bash ni cmd) para que funcione en cualquier SO
      List<String> comando = List.of(
        "docker",
        "exec",
        "tallerwebi-mysql",
        "mysql",
        "-h",
        dbHost,
        "-P",
        dbPort,
        "-u",
        dbUser,
        "-p" + dbPassword,
        dbName,
        "-e",
        sqlCommands
      );

      Process process = new ProcessBuilder(comando).redirectErrorStream(true).start();
      String salida = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      int exitCode = process.waitFor();

      if (exitCode == 0) {
        System.out.println("Base de datos limpiada exitosamente");
      } else {
        System.err.println("Error al limpiar la base de datos. Exit code: " + exitCode);
        System.err.println(salida);
      }
    } catch (IOException | InterruptedException e) {
      System.err.println("Error ejecutando script de limpieza: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
