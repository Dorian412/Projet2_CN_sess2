import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MinesweeperServer {

    private static ServerSocket sSocket;
    private static ExecutorService threadPool;

    public static void main(String[] args){

        try{

            if(args.length == 0){
                System.out.println("veuillez indiquez un nombre de thread");
                return;
            }

            if(Integer.parseInt(args[0]) < 0){
                System.out.println("vous n avez pas choisi un nombre de thread valide");
                return;
            }

            //recupération de du nombre de thread voulu et lancement du pool de thread
            int poolSize = Integer.parseInt(args[0]);
            sSocket = new ServerSocket(8014);
            threadPool = Executors.newFixedThreadPool(poolSize);
            
            System.out.println("Serveur lancé");

            //boucle d'acceptation de connexion
            while(true)
            {
                Socket cSocket = sSocket.accept();
                //System.out.println("Connexion acceptée");
                threadPool.execute(new ThreadWorker(cSocket));
            }

        }
        catch(IOException | NumberFormatException e)
        {
            System.err.println(e);
            threadPool.shutdown();
        }

    }
}