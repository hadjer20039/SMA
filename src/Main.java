import jade.core.Runtime; 
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

public class Main {
    public static void main(String[] args) {

        //Valeurs par défaut
        String lambda1 = "1000";
        String lambda2 = "2000";
        String lambda3 = "500";
        int nbRobots = 3;

        // Lecture des arguments
        if (args.length >= 1) {
            nbRobots = Integer.parseInt(args[0]);
        } 
        if (args.length >= 4) {
            lambda1 = args[1];
            lambda2 = args[2];
            lambda3 = args[3];
        }

        // Initialisation du conteneur JADE
        Runtime rt = Runtime.instance(); 
        Profile p = new ProfileImpl();
        p.setParameter(Profile.GUI, "true");
        ContainerController cc = rt.createMainContainer(p);

        // Création et démarrage des agents
        try {
            //Donner lambda 1 et 2 a l'atelier 
            Object[] atelierArgs = new Object[]{lambda1, lambda2};
            AgentController ac = cc.createNewAgent("Atelier", "agents.AtelierAgent", atelierArgs);
            ac.start();
            
            //Lancer les robots avec lambda 3
            for (int i = 1; i <= nbRobots; i++) {
                Object[] robotArgs = new Object[]{lambda3};
                AgentController rc = cc.createNewAgent("Robot" + i, "agents.RobotAgent", robotArgs);
                rc.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}