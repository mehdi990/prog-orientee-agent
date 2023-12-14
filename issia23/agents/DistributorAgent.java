package issia23.agents;


import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.gui.AgentWindowed;
import jade.gui.SimpleWindow4Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.awt.*;

/**
 * Cette classe représente un agent distributeur dans une simulation JADE.
 * Le DistributorAgent gère les demandes de remplacement de produits en vérifiant
 * la disponibilité des produits dans son inventaire et en répondant aux requêtes
 * des autres agents.
 */


public class DistributorAgent extends AgentWindowed {
    // Liste des produits disponibles que le distributeur peut offrir en remplacement
    private String[] availableProducts;

    /**
     * Configuration initiale de l'agent DistributorAgent.
     * Initialise la liste des produits disponibles et ajoute le comportement
     * pour écouter les demandes de remplacement.
     */
    protected void setup() {
        // Initialisation du distributeur avec un assortiment de produits
        availableProducts = new String[]{"Mouse", "Screen", "CoffeeMaker", "WashingMachine", "Dishwasher", "VacuumCleaner"};

        // Initialisation de la fenêtre GUI
        this.window = new SimpleWindow4Agent(getLocalName(), this);
        window.setBackgroundTextColor(Color.CYAN);
        println("Hello, I am a distributor agent.");


        // Ajouter un comportement pour écouter les demandes de remplacement
        addBehaviour(new HandleReplacementRequest());
    }

    /**
     * Comportement cyclique pour gérer les demandes de remplacement de produits.
     * Écoute les messages de type REQUEST et répond en fonction de la disponibilité
     * des produits demandés.
     */
    private class HandleReplacementRequest extends CyclicBehaviour {
        public void action() {
            // Définir le modèle de message pour les demandes de remplacement
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // Traitement de la demande de remplacement
                String content = msg.getContent();
                // Vérifier si le produit demandé est disponible
                boolean isAvailable = checkProductAvailability(content);

                ACLMessage reply = msg.createReply();

                if (isAvailable) {
                    // Envoyer une réponse positive si le produit est disponible
                    reply.setPerformative(ACLMessage.CONFIRM);
                    reply.setContent("Product available for replacement: " + content);
                } else {
                    // Envoyer une réponse négative si le produit n'est pas disponible
                    reply.setPerformative(ACLMessage.DISCONFIRM);
                    reply.setContent("Product not available for replacement: " + content);
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private boolean checkProductAvailability(String productName) {
        for (String product : availableProducts) {
            if (product.equalsIgnoreCase(productName)) {
                return true;
            }
        }
        return false;
    }
}
