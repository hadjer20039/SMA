package agents;

import model.Product;
import utils.Utils; 

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.*;

/**
 * Agent représentant un robot de l'atelier.
 * Il possède des compétences spécifiques et gère sa propre file de production.
 * Il participe au protocole de négociation pour accepter de nouveaux produits.
 * @author Hadjer CHEDJARI EL MEUR et Etienne BOSSU
 */
public class RobotAgent extends Agent {
    private HashMap<String, Double> skills = new HashMap<>();
    private List<Product> productQueue = new ArrayList<>();
    private boolean isWorking = false;
    private long lambda3 = 3000;
    
    protected void setup() {
        // Récupération des paramètres (lambda3)
        Object[] args = getArguments();
        if(args != null && args.length > 0) {
             try { lambda3 = Long.parseLong((String)args[0]); } catch(Exception e){}
        }

        // Initialisation aléatoire des compétences (3 compétences sur 5 possibles)
        int totalSkillsAvailable = 5; 
        for(int i=0; i<3; i++) {
            int skillId = (int)(Math.random() * totalSkillsAvailable);
            skills.put(String.valueOf(skillId), 0.5 + (Math.random() * 0.49));
        }
        
        System.out.println("Robot " + getLocalName() + " prêt. Compétences: " + skills);

        // Enregistrement au Directory Facilitator (DF)
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        
        // Service général de robot
        ServiceDescription sd = new ServiceDescription();
        sd.setType("robot-service");
        sd.setName("robot-service");
        dfd.addServices(sd);
        
        // Publication d'un service par compétence possédée
        for (String skill : skills.keySet()) {
            ServiceDescription sdSkill = new ServiceDescription();
            sdSkill.setType("skill-" + skill);
            sdSkill.setName("robot-service");
            dfd.addServices(sdSkill);
        }

        try { 
            DFService.register(this, dfd); 
        } catch (FIPAException e) { 
            e.printStackTrace(); 
        }

        // Ajout des comportements de base
        addBehaviour(new ReceiveNewProductBehaviour());
        addBehaviour(new ResponderBehaviour());
        addBehaviour(new WorkerBehaviour());
    }

    /**
     * Calcule le temps de traitement estimé pour vider la file d'attente actuelle.
     * Utilise l'espérance pour intégrer la probabilité d'échec.
     * @return le makespan calculé
     */
    public double calculateMakespan() {
        double totalTime = 0;
        for (Product p : productQueue) {
            String skill = p.getNextMissingSkill();
            if (skill != null && skills.containsKey(skill)) {
                double prob = skills.get(skill);
                // Formule de l'espérance pour le nombre d'essais : E = (1-p)/p
                double E = (1.0 - prob) / prob;
                totalTime += lambda3 * (1.0 + E);
            } else {
                totalTime += lambda3; 
            }
        }
        return totalTime;
    }

    /**
     * Écoute l'arrivée de nouveaux produits envoyés par l'Atelier.
     */
    private class ReceiveNewProductBehaviour extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM); 
            ACLMessage msg = receive(mt);
            if (msg != null) {
                try {
                    Object content = msg.getContentObject();
                    if (content instanceof Product) {
                        // Dès qu'un produit arrive, on cherche à le traiter ou le déléguer
                        myAgent.addBehaviour(new ManagerBehaviour((Product) content));
                    }
                } catch (Exception e) { e.printStackTrace(); }
            } else { block(); }
        }
    }

    /**
     * Gère l'acheminement d'un produit : soit il est fini, soit on lance une enchère
     * pour trouver un robot capable de réaliser la prochaine étape.
     */
    private class ManagerBehaviour extends OneShotBehaviour {
        private Product product;
        public ManagerBehaviour(Product p) { this.product = p; }

        public void action() {
            String neededSkill = product.getNextMissingSkill();
            
            // Si le produit est terminé, on le renvoie à l'Atelier
            if (neededSkill == null) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(new jade.core.AID("Atelier", jade.core.AID.ISLOCALNAME));
                try { 
                    msg.setContentObject(product); 
                    Utils.totalMessages.incrementAndGet(); 
                    send(msg); 
                } catch(Exception e){}
                return;
            }

            // Sinon, on cherche des robots compétents pour la suite
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("skill-" + neededSkill);
            template.addServices(sd);
            
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                if (result.length > 0) {
                     ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                     for (DFAgentDescription agent : result) cfp.addReceiver(agent.getName());
                     cfp.setContent("skill-" + neededSkill);
                     cfp.setConversationId("nego-" + product.getId() + "-" + System.currentTimeMillis());
                     
                     Utils.totalMessages.incrementAndGet(); 
                     send(cfp);
                     
                     // On attend les propositions des autres robots
                     myAgent.addBehaviour(new CollectProposalsBehaviour(result.length, product, cfp.getConversationId()));
                } else {
                    System.out.println("!!! [Manager " + getLocalName() + "] Aucun robot trouvé pour le skill " + neededSkill);
                }
            } catch (FIPAException e) { e.printStackTrace(); }
        }
    }
    
    /**
     * Collecte les offres des robots et sélectionne la meilleure (le plus petit makespan).
     */
    private class CollectProposalsBehaviour extends WakerBehaviour {
        private Product product;
        private String conversationId;
        
        public CollectProposalsBehaviour(int expected, Product p, String convId) {
            super(RobotAgent.this, 1000); // Temps d'attente des réponses : 1 seconde
            this.product = p;
            this.conversationId = convId;
        }
        
        protected void onWake() {
             MessageTemplate mt = MessageTemplate.and(
                 MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                 MessageTemplate.MatchConversationId(conversationId)
             );
             
             ACLMessage bestProposal = null;
             double bestTime = Double.MAX_VALUE;
             
             ACLMessage msg = myAgent.receive(mt);
             while(msg != null) {
                 try {
                    double time = Double.parseDouble(msg.getContent());
                    if (time < bestTime) {
                        bestTime = time;
                        bestProposal = msg;
                    }
                 } catch(Exception e){}
                 msg = myAgent.receive(mt);
             }
             
             // On accepte la proposition la plus avantageuse
             if (bestProposal != null) {
                 ACLMessage accept = bestProposal.createReply();
                 accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                 try { accept.setContentObject(product); } catch(Exception e){}
                 
                 Utils.totalMessages.incrementAndGet(); 
                 myAgent.send(accept);
             }
        }
    }

    /**
     * Répond aux appels d'offres (CFP) et gère l'acceptation finale des tâches.
     */
    private class ResponderBehaviour extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.or(
                MessageTemplate.MatchPerformative(ACLMessage.CFP),
                MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)
            );
            ACLMessage msg = receive(mt);
            
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.CFP) {
                    // On propose notre temps de traitement actuel
                    double duration = calculateMakespan();
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(duration));
                    
                    Utils.totalMessages.incrementAndGet(); 
                    send(reply);
                } 
                else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    // On a gagné l'enchère, on ajoute le produit à notre file
                    try {
                        Product p = (Product) msg.getContentObject();
                        productQueue.add(p);
                        System.out.println("V [Worker " + getLocalName() + "] Produit reçu : " + p.getId() + " (File: " + productQueue.size() + ")");
                    } catch (Exception e) {}
                }
            } else { block(); }
        }
    }

    /**
     * Simule le travail effectif du robot sur les produits de sa file.
     */
    private class WorkerBehaviour extends TickerBehaviour {
        public WorkerBehaviour() { super(RobotAgent.this, 200); } 

        protected void onTick() {
            // Si le robot n'est pas déjà occupé et que la file n'est pas vide
            if (!isWorking && !productQueue.isEmpty()) {
                Product currentProduct = productQueue.remove(0); 
                String skillToApply = currentProduct.getNextMissingSkill();
                
                if (skills.containsKey(skillToApply)) {
                    isWorking = true;
                    
                    // On simule la durée de fabrication lambda3
                    myAgent.addBehaviour(new WakerBehaviour(myAgent, lambda3) {
                        protected void onWake() {
                            double prob = skills.get(skillToApply);
                            
                            // Loi de Bernoulli pour déterminer le succès ou l'échec
                            if (Math.random() < prob) {
                                currentProduct.setSkillDone(skillToApply);
                                System.out.println("*** [Worker " + getLocalName() + "] SUCCÈS sur " + currentProduct.getId());
                                // Une fois fini, on repasse par le Manager pour la suite
                                myAgent.addBehaviour(new ManagerBehaviour(currentProduct));
                            } else {
                                currentProduct.addFailure();
                                System.out.println("!!! [Worker " + getLocalName() + "] ÉCHEC sur " + currentProduct.getId() + " -> Retour file");
                                // En cas d'échec, le produit est remis en tête de file
                                productQueue.add(0, currentProduct);
                            }
                            isWorking = false;
                        }
                    });
                }
            }
        }
    }
}