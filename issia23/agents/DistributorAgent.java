package issia23.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.gui.AgentWindowed;
import jade.gui.SimpleWindow4Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.awt.*;
import java.util.*;

/**
 * Cette classe représente un agent distributeur dans une simulation JADE.
 * Le DistributorAgent gère les demandes de remplacement de produits en vérifiant
 * la disponibilité des produits dans son inventaire et en répondant aux requêtes
 * des autres agents.
 */

public class DistributorAgent extends AgentWindowed {
    // Ensemble des produits disponibles pour le remplacement
    private Set<String> availableProducts;
    // Délais de livraison estimés pour chaque produit
    private Map<String, Integer> productDeliveryTimes;

    protected void setup() {
        // Initialisation de l'interface utilisateur de l'agent
        this.window = new SimpleWindow4Agent(getLocalName(), this);
        window.setBackgroundTextColor(Color.CYAN);
        println("Hello, I am a distributor agent.");

        // Initialisation des produits disponibles et des délais de livraison
        availableProducts = new HashSet<>(Arrays.asList("Mouse", "Screen", "CoffeeMaker", "WashingMachine", "Dishwasher", "VacuumCleaner"));
        productDeliveryTimes = new HashMap<>();
        productDeliveryTimes.put("Mouse", 2); // Exemple : 2 jours pour livrer une souris
        productDeliveryTimes.put("Screen", 5); // Exemple : 5 jours pour livrer un écran
        productDeliveryTimes.put("CoffeeMaker", 3);
        productDeliveryTimes.put("Dishwasher", 4);
        productDeliveryTimes.put("WashingMachine", 6);



        // Ajout du comportement pour écouter et traiter les demandes de remplacement
        addBehaviour(new HandleReplacementRequest());
    }

    private class HandleReplacementRequest extends CyclicBehaviour {
        public void action() {
            // Définition du modèle de message pour filtrer les messages de type REQUEST
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // Récupération du contenu du message (nom du produit demandé)
                String content = msg.getContent();
                ACLMessage reply = msg.createReply();

                // Vérification de la disponibilité du produit demandé
                if (availableProducts.contains(content)) {
                    reply.setPerformative(ACLMessage.PROPOSE);
                    int deliveryTime = productDeliveryTimes.getOrDefault(content, 7); // Utilisation du délai par défaut si non spécifié
                    String responseContent = "Product available for replacement: " + content +
                            ". Estimated delivery time: " + deliveryTime + " days.";
                    reply.setContent(responseContent);
                } else {
                    // Envoi d'une réponse de refus si le produit n'est pas disponible
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("Product not available for replacement: " + content);
                }
                myAgent.send(reply);
            } else {
                // Si aucun message correspondant n'est reçu, bloquer le comportement
                block();
            }
        }
    }

}
