import js from "@eslint/js";
import globals from "globals";
import jasmine from "eslint-plugin-jasmine";

export default [
  // Mascota 3D del handoff de diseño: código de terceros, no se lintea
  { ignores: ["mascot3d.js"] },
  js.configs.recommended,
  {
    files: ["**/*.js"],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: "module",
      globals: {
        ...globals.browser,
        ...globals.jasmine,
      },
    },
    plugins: {
      jasmine,
    },
    rules: {
      "indent": ["error", 2],
      "linebreak-style": ["error", "unix"],
      "quotes": ["error", "double"],
      "semi": ["error", "always"],
      "no-unused-vars": "warn",
    },
  },
  {
    files: ["spec/**/*.js"],
    rules: {
      ...jasmine.configs.recommended.rules,
    },
  },
];
