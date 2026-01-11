package utils;

import jade.core.Agent;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Classe utilitaire regroupant des fonctions partagées par les agents.
 * Elle gère notamment les statistiques globales et la recherche de services.
 * @author Hadjer CHEDJARI EL MEUR et Etienne BOSSU
 * @version 1.0 (Janvier 2026)
 */
public class Utils {
    private static Random rnd = new Random();
    
    /** Compteur global de messages. */
    public static AtomicInteger totalMessages = new AtomicInteger(0);
    
    /**
     * Recherche un robot dans l'annuaire (DF) offrant le service "robot-service".
     * @param myAgent L'agent qui effectue la recherche.
     * @return L'identifiant (AID) d'un robot choisi au hasard parmi les résultats.
     */
    public static AID getRandomRobot(Agent myAgent) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("robot-service"); 
        template.addServices(sd);

        try {
            // Consultation de l'annuaire du Directory Facilitator
            DFAgentDescription[] result = DFService.search(myAgent, template);
            if (result.length > 0) {
                // Sélection aléatoire pour assurer une distribution de charge initiale
                int index = rnd.nextInt(result.length);
                return result[index].getName();
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        return null;
    }
}