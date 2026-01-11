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

public class RobotAgent extends Agent {
    private HashMap<String, Double> skills = new HashMap<>();
    private List<Product> productQueue = new ArrayList<>();
    private boolean isWorking = false;
    private long lambda3 = 3000;
    
    protected void setup() {
        Object[] args = getArguments();
        if(args != null && args.length > 0) {
             try { lambda3 = Long.parseLong((String)args[0]); } catch(Exception e){}
        }

        int totalSkillsAvailable = 5; 
        for(int i=0; i<3; i++) {
            int skillId = (int)(Math.random() * totalSkillsAvailable);
            skills.put(String.valueOf(skillId), 0.5 + (Math.random() * 0.49));
        }
        
        System.out.println("Robot " + getLocalName() + " prêt. Skills: " + skills);

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("robot-service");
        sd.setName("robot-service");
        dfd.addServices(sd);
        
        for (String skill : skills.keySet()) {
            ServiceDescription sdSkill = new ServiceDescription();
            sdSkill.setType("skill-" + skill);
            sdSkill.setName("robot-service");
            dfd.addServices(sdSkill);
        }

        try { DFService.register(this, dfd); } catch (FIPAException e) { e.printStackTrace(); }

        addBehaviour(new ReceiveNewProductBehaviour());
        addBehaviour(new ResponderBehaviour());
        addBehaviour(new WorkerBehaviour());
    }

    public double calculateMakespan() {
        double totalTime = 0;
        for (Product p : productQueue) {
            String skill = p.getNextMissingSkill();
            if (skill != null && skills.containsKey(skill)) {
                double prob = skills.get(skill);
                double E = (1.0 - prob) / prob;
                totalTime += lambda3 * (1.0 + E);
            } else {
                totalTime += lambda3; 
            }
        }
        return totalTime;
    }

    private class ReceiveNewProductBehaviour extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM); 
            ACLMessage msg = receive(mt);
            if (msg != null) {
                try {
                    Object content = msg.getContentObject();
                    if (content instanceof Product) {
                        myAgent.addBehaviour(new ManagerBehaviour((Product) content));
                    }
                } catch (Exception e) { e.printStackTrace(); }
            } else { block(); }
        }
    }

    private class ManagerBehaviour extends OneShotBehaviour {
        private Product product;
        public ManagerBehaviour(Product p) { this.product = p; }

        public void action() {
            String neededSkill = product.getNextMissingSkill();
            
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
                     
                     myAgent.addBehaviour(new CollectProposalsBehaviour(result.length, product, cfp.getConversationId()));
                } else {
                    System.out.println("!!! [Manager " + getLocalName() + "] Personne pour skill " + neededSkill + ".");
                }
            } catch (FIPAException e) { e.printStackTrace(); }
        }
    }
    
    private class CollectProposalsBehaviour extends WakerBehaviour {
        private Product product;
        private String conversationId;
        
        public CollectProposalsBehaviour(int expected, Product p, String convId) {
            super(RobotAgent.this, 1000);
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
             
             if (bestProposal != null) {
                 ACLMessage accept = bestProposal.createReply();
                 accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                 try { accept.setContentObject(product); } catch(Exception e){}
                 
                 Utils.totalMessages.incrementAndGet(); 
                 myAgent.send(accept);
             }
        }
    }

    private class ResponderBehaviour extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.or(
                MessageTemplate.MatchPerformative(ACLMessage.CFP),
                MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)
            );
            ACLMessage msg = receive(mt);
            
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.CFP) {
                    double duration = calculateMakespan();
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(duration));
                    
                    Utils.totalMessages.incrementAndGet(); 
                    send(reply);
                } 
                else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    try {
                        Product p = (Product) msg.getContentObject();
                        productQueue.add(p);
                        System.out.println("VVV [Worker " + getLocalName() + "] J'ai gagné " + p.getId() + " (File: " + productQueue.size() + ")");
                    } catch (Exception e) {}
                }
            } else { block(); }
        }
    }

    private class WorkerBehaviour extends TickerBehaviour {
        public WorkerBehaviour() { super(RobotAgent.this, 200); } 

        protected void onTick() {
            if (!isWorking && !productQueue.isEmpty()) {
                Product currentProduct = productQueue.remove(0); 
                String skillToApply = currentProduct.getNextMissingSkill();
                
                if (skills.containsKey(skillToApply)) {
                    isWorking = true;
                    
                    myAgent.addBehaviour(new WakerBehaviour(myAgent, lambda3) {
                        protected void onWake() {
                            double prob = skills.get(skillToApply);
                            
                            if (Math.random() < prob) {
                                currentProduct.setSkillDone(skillToApply);
                                System.out.println("*** [Worker " + getLocalName() + "] SUCCES skill " + skillToApply + " sur P-" + currentProduct.getId());
                                myAgent.addBehaviour(new ManagerBehaviour(currentProduct));
                            } else {
                                currentProduct.addFailure();
                                System.out.println("!!! [Worker " + getLocalName() + "] ECHEC skill " + skillToApply + " sur P-" + currentProduct.getId() + " -> Retour file");
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