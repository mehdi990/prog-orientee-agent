package issia23.agents;

import issia23.data.Product;
import issia23.data.ProductType;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.gui.AgentWindowed;
import jade.gui.SimpleWindow4Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Cette classe représente un agent distributeur dans une simulation JADE.
 * Le DistributorAgent gère les demandes de remplacement de produits en vérifiant
 * la disponibilité des produits dans son inventaire et en répondant aux requêtes
 * des autres agents.
 */

public class DistributorAgent extends AgentWindowed {
// Liste des produits disponibles pour le remplacement
    private List<Product> availableProducts;    // Délais de livraison estimés pour chaque produit
    private Map<String, Integer> productDeliveryTimes;

    protected void setup() {
        // Initialisation de l'interface utilisateur de l'agent
        this.window = new SimpleWindow4Agent(getLocalName(), this);
        window.setBackgroundTextColor(Color.CYAN);
        println("Hello, I am a distributor agent.");

        // Initialisation des produits disponibles
        availableProducts = new ArrayList<>();

        // Exemple d'ajout de produits à la liste
        availableProducts.add(new Product("Mouse", ProductType.Mouse));
        availableProducts.add(new Product("CoffeeMaker", ProductType.coffeeMaker));

        productDeliveryTimes = new HashMap<>();
        productDeliveryTimes.put("Mouse", 2);
        productDeliveryTimes.put("CoffeeMaker", 3);



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

                boolean productFound = false;
                // Vérification de la disponibilité du produit demandé
                for (Product product : availableProducts) {
                    if (product.getName().equals(content)) {
                        reply.setPerformative(ACLMessage.PROPOSE);
                        int deliveryTime = productDeliveryTimes.getOrDefault(content, 7);
                        String responseContent = "Product available: " + content +
                                ". Estimated delivery time: " + deliveryTime + " days.";
                        reply.setContent(responseContent);
                        productFound = true;
                        break;
                    }
                }
                if (!productFound) {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("Product not available: " + content);
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

}
