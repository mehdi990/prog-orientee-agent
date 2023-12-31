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
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.awt.Color;

/** Définition de la classe RepairCoffeeAgent, un type d'agent dans JADE spécialisé dans la réparation.*/
public class RepairCoffeeAgent extends AgentWindowed {

    /** Définition de la classe RepairCoffeeAgent, un type d'agent dans JADE spécialisé dans la réparation.*/
    public static List<ProductType> specialities;
    private static final int TIME_COST = 3; // Time cost in days for repair coffee


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
        println("Specialities initialized: " + specialities);

        // Enregistrement de l'agent dans les pages jaunes JADE.
        AgentServicesTools.register(this, "repair", "coffee");
        println("Registered as repair-coffee agent");

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

    //public boolean isProductRepairable(ProductType type, String state) {
        // Liste des états réparables
        //List<String> repairableStates = Arrays.asList("réparable", "légèrement endommagé", "facilement réparable");

        // Vérifie si le type de produit est dans les spécialités et si son état est réparable
       // return specialities.contains(type) && repairableStates.contains(state);
    //}

    public boolean isProductRepairable(ProductType type) {
        println("Checking if I can repair: " + type);
        println("My specialities are: " + specialities);
        return specialities.contains(type);
    }



    /** Classe interne définissant le comportement pour gérer les demandes de réparation.*/
    private class HandleRepairRequestsBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            // Filtrage des messages pour les demandes de réparation.
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                println("Received repair request: " + msg.getContent());
                // Analyse de la demande de réparation.
                String requestedRepair = msg.getContent();
                ACLMessage reply = msg.createReply();

                try {
                    // Déclaration des variables en dehors du bloc if
                    ProductType requestedType;
                    String productState;
                    // Extraction du type et de l'état du produit depuis le contenu du message
                    String[] contentParts = requestedRepair.split("-");
                    // Checking if the requested repair is a valid product type
                    if (contentParts.length >= 2) {
                         requestedType = ProductType.valueOf(contentParts[0]);
                         productState = contentParts[1];
                        System.out.println("Requested repair type: " + requestedType);
                    } else {
                        // Gérer le cas où le message n'est pas dans le format attendu
                        throw new IllegalArgumentException("Message format incorrect: " + requestedRepair);
                    }

                    // Utilisation des variables requestedType et productState
                    RepairCoffeeAgent repairAgent = (RepairCoffeeAgent) myAgent;
                    if (repairAgent.isProductRepairable(requestedType)) {
                        // Si l'agent peut réparer le type de produit demandé.
                        reply.setPerformative(ACLMessage.PROPOSE);
                        int additionalDays = 1 + new Random().nextInt(3); // Génère un nombre entre 1 et 3
                        LocalDate repairDate = LocalDate.now().plusDays(additionalDays);
                        String responseContent = "Proposed repair date: " + repairDate +
                                ". Estimated cost: " + ((RepairCoffeeAgent) myAgent).calculateEstimatedCost(requestedType) +
                                ". Time cost: "+ ((RepairCoffeeAgent) myAgent).TIME_COST + " days.";
                        reply.setContent(responseContent);
                    } else {
                        // Refus si l'agent ne peut pas réparer ce type de produit.
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent("Cannot repair type: " + requestedType);
                    }
                } catch (IllegalArgumentException e) {
                    // Gestion d'une demande invalide.
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("Invalid repair request: " + requestedRepair);
                }
                // Sending the reply
                myAgent.send(reply);
                System.out.println("Sent reply to " + msg.getSender() + ": " + reply.getContent());
            } else {
                // Blocage du comportement en l'absence de messages correspondants.
                block();
            }
        }
    }

}
