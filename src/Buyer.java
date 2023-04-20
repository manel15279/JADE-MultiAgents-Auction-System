import java.util.concurrent.ThreadLocalRandom;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class Buyer extends Agent{

    private int budget;

    @Override
    protected void setup() {

        setRandPrice();
        addBehaviour(new Requests());

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Buyer");
        sd.setName("Enchère");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        System.out.println(getAID().getName() + " veut acheter un produit et il a un budget de " + budget + " dollars.");
    }

    private void setRandPrice() {
        int min = 1000;
        int max = Integer.MAX_VALUE;

        budget = ThreadLocalRandom.current().nextInt(min, max);
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        System.out.println("L'acheteur " + getAID().getName() + " a fini.");
    }


    private class Requests extends Behaviour {

        private String product;
        private Integer actualProductPrice;

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive();

            if (msg != null) {
                parseContent(msg.getContent());

                ACLMessage reply = msg.createReply();
                int buyer;

                if (actualProductPrice < budget / 4) {
                    buyer = (int) (actualProductPrice + actualProductPrice * ((float) ThreadLocalRandom.current().nextInt(5, 10) / 10));
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(buyer));
                } else {
                    reply.setPerformative(ACLMessage.REFUSE);
                }

                myAgent.send(reply);
            } else {
                block();
            }
        }

        private void parseContent(String content) {
            String[] split = content.split("\\|\\|");

            product = split[0];
            actualProductPrice = Integer.parseInt(split[1]);
        }

        @Override
        public boolean done() {
            return false;
        }
    }

}