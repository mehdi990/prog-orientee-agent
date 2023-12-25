package issia23.agents;

import issia23.data.ProductType;
import jade.core.AgentServicesTools;
import jade.gui.AgentWindowed;
import jade.gui.SimpleWindow4Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.Color;

public class RepairCoffeeAgent extends AgentWindowed {
    List<ProductType> specialities;

    @Override
    public void setup() {
        this.window = new SimpleWindow4Agent(getLocalName(), this);
        this.window.setBackgroundTextColor(Color.orange);
        println("hello, do you want coffee ?");
        Random hasard = new Random();

        specialities = new ArrayList<>();
        for (ProductType type : ProductType.values()) {
            if (hasard.nextBoolean()) specialities.add(type);
        }
        if (specialities.isEmpty()) specialities.add(ProductType.values()[hasard.nextInt(ProductType.values().length)]);
        println("I have these specialities : ");
        specialities.forEach(p -> println("\t" + p));

        AgentServicesTools.register(this, "repair", "coffee");
        println("I'm just registered as a repair-coffee");

        addBehaviour(new HandleRepairRequestsBehaviour());
    }

    private boolean isValidProductType(String productTypeString) {
        for (ProductType type : ProductType.values()) {
            if (type.name().equals(productTypeString)) {
                return true;
            }
        }
        return false;
    }

    private String calculateEstimatedCost(ProductType type) {
        double cost;
        switch (type) {
            case Mouse:
                cost = 10.0;
                break;
            case coffeeMaker:
                cost = 15.0;
                break;
            case vacuumCleaner:
                cost = 20.0;
                break;
            default:
                cost = 25.0;
        }
        return String.format("%.2f€", cost);
    }

    private class HandleRepairRequestsBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String requestedRepair = msg.getContent();
                ACLMessage reply = msg.createReply();

                try {
                    ProductType requestedType = ProductType.valueOf(requestedRepair);
                    if (isValidProductType(requestedRepair) && specialities.contains(requestedType)) {
                        reply.setPerformative(ACLMessage.PROPOSE);
                        LocalDate repairDate = LocalDate.now().plusDays(new Random().nextInt(3) + 1);
                        String responseContent = "Date de réparation proposée : " + repairDate;
                        responseContent += ". Coût estimé : " + calculateEstimatedCost(requestedType);
                        reply.setContent(responseContent);
                    } else {
                        reply.setPerformative(ACLMessage.REFUSE);
                    }
                } catch (IllegalArgumentException e) {
                    reply.setPerformative(ACLMessage.REFUSE);
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }
}
