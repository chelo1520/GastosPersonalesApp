package com.tallerwebi.punta_a_punta;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ReiniciarDB {

  private static final String E2E_DATABASE_NAME = "tallerwebi_e2e";

  public static void limpiarBaseDeDatos() {
    try {
      String dbHost = System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "localhost";
      String dbPort = System.getenv("DB_PORT") != null ? System.getenv("DB_PORT") : "3306";
      String dbName = System.getenv("DB_NAME");
      if (!E2E_DATABASE_NAME.equals(dbName)) {
        throw new IllegalStateException(
          "Las pruebas E2E solo pueden limpiar la base dedicada '" +
          E2E_DATABASE_NAME +
          "'. Configurá DB_NAME antes de ejecutar las pruebas."
        );
      }

      String dbUser = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "user";
      String dbPassword = System.getenv("DB_PASSWORD") != null
        ? System.getenv("DB_PASSWORD")
        : "user";

      String sqlCommands =
        "DELETE FROM Gasto;\n" +
        "DELETE FROM Usuario;\n" +
        "ALTER TABLE Usuario AUTO_INCREMENT = 1;\n" +
        "INSERT INTO Usuario(id, email, password, rol, activo) VALUES(null, 'test@unlam.edu.ar', 'test', 'ADMIN', true);";

      Process process = new ProcessBuilder(
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
      )
        .redirectErrorStream(true)
        .start();
      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      int exitCode = process.waitFor();

      if (exitCode == 0) {
        System.out.println("Base de datos limpiada exitosamente");
      } else {
        throw new IllegalStateException(
          "Error al limpiar la base de datos. Exit code: " + exitCode + ". " + output
        );
      }
    } catch (IOException e) {
      throw new IllegalStateException("Error ejecutando Docker para limpiar la base de datos", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Se interrumpió la limpieza de la base de datos", e);
    }
  }
}
