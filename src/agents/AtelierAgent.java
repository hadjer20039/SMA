package agents;

import model.Product; 
import utils.Utils;  

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import java.io.IOException;
import java.util.Random;

public class AtelierAgent extends Agent {
    private long lambda1 = 1000;
    private long lambda2 = 2000;
    private int productCount = 0;
    private Random rnd = new Random();

    private int totalFailuresGlobal = 0;
    private int finishedProductsCount = 0;

    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            try {
                lambda1 = Long.parseLong((String) args[0]);
                lambda2 = Long.parseLong((String) args[1]);
            } catch (Exception e) {}
        }
        System.out.println("Atelier " + getLocalName() + " prêt.");
        addBehaviour(new GenerateProductBehaviour(this, getRandomTime()));
        addBehaviour(new ReceiveFinishedProductBehaviour());
    }

    private long getRandomTime() {
        return lambda1 + (long)(rnd.nextDouble() * (lambda2 - lambda1));
    }

    private class GenerateProductBehaviour extends WakerBehaviour {
        public GenerateProductBehaviour(Agent a, long timeout) { super(a, timeout); }
        protected void onWake() {
            Product p = new Product("P-" + (++productCount), 5, 3);
            AID randomRobot = Utils.getRandomRobot(myAgent);

            if (randomRobot != null) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(randomRobot);
                try {
                    msg.setContentObject(p);
                    Utils.totalMessages.incrementAndGet();
                    send(msg);
                    System.out.println("Atelier : Produit " + p.getId() + " envoyé à " + randomRobot.getLocalName());
                } catch (IOException e) { e.printStackTrace(); }
            }
            myAgent.addBehaviour(new GenerateProductBehaviour(myAgent, getRandomTime()));
        }
    }

    private class ReceiveFinishedProductBehaviour extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = receive();
            if (msg != null && msg.getPerformative() == ACLMessage.INFORM && msg.getSender().getLocalName().contains("Robot")) {
                try {
                    Product p = (Product) msg.getContentObject();
                    if(p.isFinished()) {
                        // MISE A JOUR DES STATS
                        finishedProductsCount++;
                        totalFailuresGlobal += p.getFailures();
                        double avgFailures = (finishedProductsCount > 0) ? (double) totalFailuresGlobal / finishedProductsCount : 0.0;
                        
                        long duration = System.currentTimeMillis() - p.getStartTime();
                        
                        // AFFICHAGE COMPLET
                        System.out.println("--------------------------------------------------");
                        System.out.println("--- PRODUIT FINI : " + p.getId() + " ---");
                        System.out.println("   > Reçu de          : " + msg.getSender().getLocalName());
                        System.out.println("   > Temps écoulé     : " + duration + " ms");
                        System.out.println("   > Échecs sur ce P  : " + p.getFailures());
                        System.out.println("   > Moyenne Échecs   : " + String.format("%.2f", avgFailures));
                        System.out.println("   > Total Messages   : " + Utils.totalMessages.get());
                        System.out.println("--------------------------------------------------");
                    }
                } catch (Exception e) { e.printStackTrace(); }
            } else {
                block();
            }
        }
    }
}