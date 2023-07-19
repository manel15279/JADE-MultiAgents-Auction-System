import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;


public class Auctioner extends Agent{

    private AID[] buyers;
    private String product;
    private Integer initialPrice;
    private Integer reservePrice;

    @Override
    protected void setup() {
        System.out.println("Le vendeur commence l'enchère !");

        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            product = (String) args[0];
            initialPrice = Integer.parseInt((String) args[1]);
            reservePrice = Integer.parseInt((String) args[2]);
            System.out.println("Produit en vente : " + product + " au prix de " + initialPrice + " dollars.");

            addBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    //récuperer les agents acheteurs
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("Buyer");
                    template.addServices(sd);

                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);

                        buyers = new AID[result.length];
                        for (int i = 0; i < result.length; i++) {
                            buyers[i] = result[i].getName();
                        }
                    } catch (FIPAException e) {
                        e.printStackTrace();
                    }

                    myAgent.addBehaviour(new Performer());
                }
            });

        } else {
            System.out.println("Aucun produit en vente !");
            doDelete();
        }
    }

    private class Performer extends Behaviour {
        private int step = 0;
        private Map<AID, Integer> offers = new HashMap<>();
        private int num = 0; //compter le nbr d'acheteur a qui l'agent a envoyer un CFP
        private MessageTemplate mt;
        private AID maxBuyer = null;
        private int maxOffer = 0;
        private int noOffers = 0;

        @Override
        public void action() {
            switch (step) {
                case 0:
                    // l'agent envoie un Call for Proposal à tous les acheteurs, sauf à l'acheteur ayant fait l'offre la plus élevée précédemment.
                    offers = new HashMap<>();
                    num = 0;
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

                    for (int i = 0; i < buyers.length; i++) {
                        if (maxBuyer == null || (maxBuyer != null && buyers[i].compareTo(maxBuyer) != 0)) {
                            cfp.addReceiver(buyers[i]);
                            num++;
                        }
                    }
                    // set actualproductprice
                    if (maxBuyer != null) {
                        cfp.setContent(product + ";" + maxOffer);
                    } else {
                        cfp.setContent(product + ";" + initialPrice);
                    }

                    cfp.setConversationId("Enchere");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis());

                    myAgent.send(cfp);

                    mt = MessageTemplate.and(
                            MessageTemplate.MatchConversationId("Enchere"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));

                    step = 1;
                    break;

                case 1:
                    // l'agent recoit les réponses des acheteurs et les traite (PROPOSE/REFUSE)
                    ACLMessage reply = myAgent.receive(mt);

                    if (reply != null) {
                        switch (reply.getPerformative()) {
                            case ACLMessage.PROPOSE:
                                offers.put(reply.getSender(), Integer.parseInt(reply.getContent()));
                                System.out.println(reply.getSender().getName() + " offre " + reply.getContent() + " dollars.");
                                noOffers = 0;
                                break;

                            case ACLMessage.REFUSE:
                                offers.put(reply.getSender(), null);
                                noOffers++;
                                break;
                        }

                        if (offers.size() == num) {
                            step = 2;
                        }

                    } else {
                        block();
                    }
                    break;

                case 2:
                    //l'agent détermine l'acheteur ayant fait l'offre la plus élevée et envoie des message ACCEPT_PROPOSAL et REJECT_PROPOSAL.
                    Iterator<Map.Entry<AID, Integer>> iterator = offers.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<AID, Integer> item = iterator.next();
                        if (item.getValue() != null && maxOffer < item.getValue()) {
                            maxBuyer = item.getKey();
                            maxOffer = item.getValue();
                        }
                    }

                    if (maxBuyer != null) {
                        System.out.println("L offre la plus élevée proposée : " + maxOffer + " dollars par " + maxBuyer.getName());
                    } else {
                        System.out.println("Pas d'offres reçues !");
                    }

                    ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    accept.addReceiver(maxBuyer);
                    accept.setContent(product + ";" + maxOffer);
                    accept.setConversationId("Enchère");
                    accept.setReplyWith("Offre acceptée" + System.currentTimeMillis());
                    myAgent.send(accept);


                    offers.keySet().stream()
                            .filter(aid -> aid != maxBuyer && offers.get(aid) != null)
                            .forEach(aid -> {
                                ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                                reject.addReceiver(maxBuyer);
                                reject.setContent(product + ";" + offers.get(aid));
                                reject.setConversationId("Enchère");
                                reject.setReplyWith("Offre rejetée" + System.currentTimeMillis());
                                myAgent.send(reject);
                            });

                    step = 3;
                    break;

                case 3:
                    //l'agent vérifie le nbr de REFUSE reçus. Si nbr = 3, l'enchère se termine. Sinon, il revient à l'étape 0 pour une nouvelle itération.
                    System.out.println("Est ce que quelqu un propose plus ?");

                    if (noOffers != 0) {
                        System.out.println(maxOffer + " dollars " + noOffers + " fois.");
                    }

                    if (noOffers == 3) {
                        step = 4;
                    } else {
                        step = 0;
                    }
                    break;

                case 4:
                    // comparer l'offre la plus élevée au prix de réserve
                    if(maxOffer > reservePrice)
                        System.out.println("Prix de réserve atteint, produit vendu au client " + maxBuyer.getName() + " à " + maxOffer + " dollars.");
                    else
                        System.out.println("Prix de réserve non-atteint.");

                    step = 5;
                    break;
            }
        }

        @Override
        public boolean done() {
            return (step == 5);
        }
    }
}