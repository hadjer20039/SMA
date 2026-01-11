import jade.core.Runtime; 
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;


/**
 * Classe principale pour le lancement de la simulation.
 * Configure l'environnement JADE, initialise le Main Container avec interface graphique,
 * et crée l'agent Atelier ainsi que des agents Robots selon les paramètres fournis.
 * @author Hadjer CHEDJARI EL MEUR et Etienne BOSSU
 * @version 1.0 (Janvier 2026)
 */
public class Main {

    /**
     * Point d'entrée du programme. 
     * @param args Arguments : nbRobots, lambda1, lambda2, lambda3. 
     */
    public static void main(String[] args) {

        // Paramètres par défaut (conforment aux consignes du sujet)
        String lambda1 = "1000";
        String lambda2 = "2000";
        String lambda3 = "500";
        int nbRobots = 3;

        // Lecture des arguments passés via les scripts de lancement
        try {
            if (args.length >= 1) {
                nbRobots = Integer.parseInt(args[0]);
            } 
            if (args.length >= 4) {
                lambda1 = args[1];
                lambda2 = args[2];
                lambda3 = args[3];
            }
        } catch (NumberFormatException e) {
            System.err.println("Erreur : Les arguments fournis ne sont pas des nombres valides.");
        }

        // Configuration et lancement de la plateforme JADE
        Runtime rt = Runtime.instance(); 
        Profile p = new ProfileImpl();
        p.setParameter(Profile.GUI, "true"); // Affiche l'interface de gestion JADE
        ContainerController cc = rt.createMainContainer(p);

        try {
            // Instanciation de l'agent Atelier avec ses paramètres temporels
            Object[] atelierArgs = new Object[]{lambda1, lambda2};
            AgentController ac = cc.createNewAgent("Atelier", "agents.AtelierAgent", atelierArgs);
            ac.start();
            
            // Création des agents Robots selon le nombre demandé
            for (int i = 1; i <= nbRobots; i++) {
                Object[] robotArgs = new Object[]{lambda3};
                AgentController rc = cc.createNewAgent("Robot" + i, "agents.RobotAgent", robotArgs);
                rc.start();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage des agents.");
            e.printStackTrace();
        }
    }
}