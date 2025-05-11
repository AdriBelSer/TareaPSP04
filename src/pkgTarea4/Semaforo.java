package pkgTarea4;

public class Semaforo {

    private int lectores = 0;
    private boolean escritorActivo = false;

    public synchronized void comenzarLectura() throws InterruptedException {
        while (escritorActivo) {
            wait();
        }
        lectores++;
    }

    public synchronized void terminarLectura() {
        lectores--;
        if (lectores == 0) {
            notifyAll();
        }
    }

    public synchronized void comenzarEscritura() throws InterruptedException {
        while (escritorActivo || lectores > 0) {
            wait();
        }
        escritorActivo = true;
    }

    public synchronized void terminarEscritura() {
        escritorActivo = false;
        notifyAll();
    }
}
