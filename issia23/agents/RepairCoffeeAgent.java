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

/** Définition de la classe RepairCoffeeAgent, un type d'agent dans JADE spécialisé dans la réparation.*/
public class RepairCoffeeAgent extends AgentWindowed {

    /** Définition de la classe RepairCoffeeAgent, un type d'agent dans JADE spécialisé dans la réparation.*/
    List<ProductType> specialities;

    @Override
    public void setup() {
        // Configuration de l'interface utilisateur graphique pour cet agent.
        this.window = new SimpleWindow4Agent(getLocalName(), this);
        this.window.setBackgroundTextColor(Color.orange);
        println("hello, do you want coffee ?");

        // Génération aléatoire des spécialités de l'agent.
        Random hasard = new Random();
        specialities = new ArrayList<>();
        for (ProductType type : ProductType.values()) {
            if (hasard.nextBoolean()) specialities.add(type);
        }
        if (specialities.isEmpty()) specialities.add(ProductType.values()[hasard.nextInt(ProductType.values().length)]);
        println("I have these specialities : ");
        specialities.forEach(p -> println("\t" + p));

        // Enregistrement de l'agent dans les pages jaunes JADE.
        AgentServicesTools.register(this, "repair", "coffee");
        println("I'm just registered as a repair-coffee");

        // Ajout d'un comportement pour écouter et répondre aux demandes de réparation.
        addBehaviour(new HandleRepairRequestsBehaviour());
    }

    /** Méthode pour valider si un type de produit est géré par cet agent.*/
    private boolean isValidProductType(String productTypeString) {
        for (ProductType type : ProductType.values()) {
            if (type.name().equals(productTypeString)) {
                return true;
            }
        }
        return false;
    }

    /** Méthode pour calculer le coût estimé de la réparation en fonction du type de produit.*/
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

    /** Classe interne définissant le comportement pour gérer les demandes de réparation.*/
    private class HandleRepairRequestsBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            // Filtrage des messages pour les demandes de réparation.
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // Analyse de la demande de réparation.
                String requestedRepair = msg.getContent();
                ACLMessage reply = msg.createReply();

                try {
                    ProductType requestedType = ProductType.valueOf(requestedRepair);
                    if (isValidProductType(requestedRepair) && specialities.contains(requestedType)) {
                        // Si l'agent peut réparer le type de produit demandé.
                        reply.setPerformative(ACLMessage.PROPOSE);
                        LocalDate repairDate = LocalDate.now().plusDays(new Random().nextInt(3) + 1);
                        String responseContent = "Date de réparation proposée : " + repairDate;
                        responseContent += ". Coût estimé : " + calculateEstimatedCost(requestedType);
                        reply.setContent(responseContent);
                    } else {
                        // Refus si l'agent ne peut pas réparer ce type de produit.
                        reply.setPerformative(ACLMessage.REFUSE);
                    }
                } catch (IllegalArgumentException e) {
                    // Gestion d'une demande invalide.
                    reply.setPerformative(ACLMessage.REFUSE);
                }
                myAgent.send(reply);
            } else {
                // Blocage du comportement en l'absence de messages correspondants.
                block();
            }
        }
    }
}
