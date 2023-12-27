package issia23.agents;

import issia23.data.Product;
import issia23.data.ProductType;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
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
    private List<Product> availableProducts;
    // Délai de livraison standard pour tous les produits (en jours)
    private static final int STANDARD_DELIVERY_TIME = 1;

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

        // Enregistrement dans le Directory Facilitator (DF)
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("product-distribution");
        sd.setName(getLocalName() + "-product-distribution");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (Exception e) {
            e.printStackTrace();
        }

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
                        // Utiliser la constante STANDARD_DELIVERY_TIME pour le délai de livraison
                        String responseContent = "Product available: " + content +
                                ". Estimated delivery time: " + STANDARD_DELIVERY_TIME + " days.";
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

    @Override
    public void takeDown() {
        // Déréférencement du Directory Facilitator (DF)
        try {
            DFService.deregister(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        println("Distributor-agent " + getAID().getName() + " terminating.");
    }

}
