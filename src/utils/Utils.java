package utils;

import jade.core.Agent;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import java.util.Random;

public class Utils {
    private static Random rnd = new Random();

    public static AID getRandomRobot(Agent myAgent) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("robot-service"); 
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(myAgent, template);
            if (result.length > 0) {
                int index = rnd.nextInt(result.length);
                return result[index].getName();
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        return null;
    }
}