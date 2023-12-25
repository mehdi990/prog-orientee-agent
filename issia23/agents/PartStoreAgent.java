package issia23.agents;

import issia23.data.Part;
import jade.core.AgentServicesTools;
import jade.core.behaviours.CyclicBehaviour;
import jade.gui.AgentWindowed;
import jade.gui.SimpleWindow4Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.List;

import java.awt.Color;
import java.util.Random;


/** class that represents a part-Store agent.
 * It is declared in the service repair-partstore.
 * It an infitine number of specific part wih a pecific cost ( up to 30% more than the standard price)
 * @author emmanueladam
 * */
public class PartStoreAgent extends AgentWindowed {
    List<Part> parts;

    /** Méthode setup appelée lors de l'initialisation de l'agent.*/
    @Override
    public void setup() {
        // Configuration de l'interface utilisateur graphique pour cet agent.
        this.window = new SimpleWindow4Agent(getLocalName(), this);
        this.window.setBackgroundTextColor(Color.LIGHT_GRAY);
        AgentServicesTools.register(this, "repair", "partstore");
        println("hello, I'm just registered as a parts-store");
        println("do you want some special parts ?");

        // Génération aléatoire des pièces disponibles dans le magasin.
        Random hasard = new Random();
        parts = new ArrayList<>();
        var existingParts = Part.getListParts();
        for (Part p : existingParts) {
            if (hasard.nextBoolean()) {
                parts.add(new Part(p.getName(), p.getType(), p.getStandardPrice() * (1 + Math.random() * 0.3)));
            }
        }
        if (parts.isEmpty()) {
            parts.add(existingParts.get(hasard.nextInt(existingParts.size())));
        }
        println("here are the parts I sell : ");
        parts.forEach(p -> println("\t" + p));

        // Enregistrement de l'agent dans les pages jaunes JADE.
        AgentServicesTools.register(this, "repair", "partstore");

        // Ajout d'un comportement pour écouter et répondre aux demandes de pièces.
        addBehaviour(new HandlePartRequestsBehaviour());
    }



    /** Classe interne définissant le comportement pour gérer les demandes de pièces.*/
    private class HandlePartRequestsBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            // Filtrage des messages pour les demandes de pièces.
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // Analyse de la demande de pièce.
                String requestedPart = msg.getContent();
                ACLMessage reply = msg.createReply();

                // Vérification de la disponibilité de la pièce demandée.
                boolean partAvailable = parts.stream().anyMatch(p -> p.getName().equals(requestedPart));
                if (partAvailable) {
                    // Si la pièce est disponible.
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent("Part available: " + requestedPart);
                } else {
                    // Refus si la pièce n'est pas disponible.
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("Part not available: " + requestedPart);
                }
                myAgent.send(reply);
            } else {
                //Blocage du comportement en l'absence de messages correspondants.
                block();
            }
        }
    }
}