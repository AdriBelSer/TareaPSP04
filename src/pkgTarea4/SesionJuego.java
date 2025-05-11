package pkgTarea4;

import java.util.Random;

public class SesionJuego {
    private boolean autenticado = false;
    private String email;

    int intentosAdivina;
    int numeroSecreto;
    int marcadorUsuarioDados;
    int marcadorServidorDados;
    int rondaDados;
    int marcadorUsuarioPPT;
    int marcadorServidorPPT;
    int rondaPPT;

    public SesionJuego() {
        this.autenticado = false;
        this.email = null;

        this.intentosAdivina = 0;
        this.numeroSecreto = new Random().nextInt(100) + 1;
        this.marcadorUsuarioDados = 0;
        this.marcadorServidorDados = 0;
        this.rondaDados = 0;
        this.marcadorUsuarioPPT = 0;
        this.marcadorServidorPPT = 0;
        this.rondaPPT = 0;
    }

    public void autenticar(String email) {
        this.autenticado = true;
        this.email = email;
    }

    public boolean estaAutenticado() {
        return autenticado;
    }

    public void cerrarSesion() {
        this.autenticado = false;
        this.email = null;
    }
}
