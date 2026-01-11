package agents;

import model.Product; 
import utils.Utils;  

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import java.io.IOException;
import java.util.Random;

/**
 * Agent responsable de la gestion de l'atelier.
 * Il génère de nouveaux produits à intervalles réguliers et centralise
 * les statistiques de production une fois les produits terminés.
 * @author Hadjer CHEDJARI EL MEUR et Etienne BOSSU
 * @version 1.0 (Janvier 2026)
 */
public class AtelierAgent extends Agent {
    private long lambda1 = 1000;
    private long lambda2 = 2000;
    private int productCount = 0;
    private Random rnd = new Random();

    // Variables pour le suivi des performances globales
    private int totalFailuresGlobal = 0;
    private int finishedProductsCount = 0;

    protected void setup() {
        // Récupération des paramètres lambda1 et lambda2 passés au démarrage
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            try {
                lambda1 = Long.parseLong((String) args[0]);
                lambda2 = Long.parseLong((String) args[1]);
            } catch (Exception e) {
                // En cas d'erreur de parsing, on garde les valeurs par défaut
            }
        }
        
        System.out.println("Atelier " + getLocalName() + " prêt.");

        // Lancement de la génération de produits et de l'écoute des retours
        addBehaviour(new GenerateProductBehaviour(this, getRandomTime()));
        addBehaviour(new ReceiveFinishedProductBehaviour());
    }

    /**
     * Calcule un délai aléatoire entre lambda1 et lambda2 pour simuler l'arrivée des produits.
     */
    private long getRandomTime() {
        return lambda1 + (long)(rnd.nextDouble() * (lambda2 - lambda1));
    }

    /**
     * Comportement chargé de créer un nouveau produit et de l'envoyer à un robot.
     * Ce comportement se reprogramme lui-même pour simuler un flux continu.
     */
    private class GenerateProductBehaviour extends WakerBehaviour {
        public GenerateProductBehaviour(Agent a, long timeout) { 
            super(a, timeout); 
        }

        protected void onWake() {
            // Création d'un produit nécessitant 3 compétences parmi 5 possibles
            Product p = new Product("P-" + (++productCount), 5, 3);
            
            // Sélection d'un robot au hasard via le Directory Facilitator
            AID randomRobot = Utils.getRandomRobot(myAgent);

            if (randomRobot != null) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(randomRobot);
                try {
                    msg.setContentObject(p);
                    Utils.totalMessages.incrementAndGet();
                    send(msg);
                    System.out.println("Atelier : Produit " + p.getId() + " envoyé à " + randomRobot.getLocalName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Atelier : Aucun robot disponible pour le produit " + p.getId());
            }

            // Planification du prochain produit
            myAgent.addBehaviour(new GenerateProductBehaviour(myAgent, getRandomTime()));
        }
    }

    /**
     * Comportement cyclique qui attend le retour des produits finis par les robots.
     * Affiche un récapitulatif des statistiques à chaque réception.
     */
    private class ReceiveFinishedProductBehaviour extends CyclicBehaviour {
        public void action() {
            // On récupère uniquement les messages de type INFORM provenant des robots
            ACLMessage msg = receive();
            if (msg != null && msg.getPerformative() == ACLMessage.INFORM) {
                try {
                    Object content = msg.getContentObject();
                    if (content instanceof Product) {
                        Product p = (Product) content;
                        
                        if (p.isFinished()) {
                            // Mise à jour des compteurs pour les statistiques globales
                            finishedProductsCount++;
                            totalFailuresGlobal += p.getFailures();
                            double avgFailures = (finishedProductsCount > 0) ? (double) totalFailuresGlobal / finishedProductsCount : 0.0;
                            
                            long duration = System.currentTimeMillis() - p.getStartTime();
                            
                            // Affichage des résultats dans la console
                            System.out.println("--------------------------------------------------");
                            System.out.println("--- PRODUIT FINI : " + p.getId() + " ---");
                            System.out.println("   > Terminé par       : " + msg.getSender().getLocalName());
                            System.out.println("   > Temps de cycle    : " + duration + " ms");
                            System.out.println("   > Échecs sur ce P   : " + p.getFailures());
                            System.out.println("   > Moyenne d'échecs  : " + String.format("%.2f", avgFailures));
                            System.out.println("   > Messages réseau   : " + Utils.totalMessages.get());
                            System.out.println("--------------------------------------------------");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // Si aucun message, on met le comportement en attente pour libérer le CPU
                block();
            }
        }
    }
}