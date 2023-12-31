package issia23.agents;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames.InteractionProtocol;
import jade.domain.DFService;
import jade.lang.acl.ACLMessage;
import issia23.data.Product;
import issia23.data.ProductType;
import issia23.gui.UserAgentWindow;
import jade.core.AgentServicesTools;
import jade.gui.GuiAgent;
import jade.gui.GuiEvent;
import jade.lang.acl.MessageTemplate;

import javax.swing.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**class related to the user, owner of products to repair
 * @author emmanueladam
 * */
public class UserAgent extends GuiAgent {
    // Map pour stocker les propositions reçues de différents agents avec leurs dates.
    private Map<AID, LocalDate> receivedProposals = new HashMap<>();

    // Flag pour déterminer si l'agent est toujours en train de collecter des propositions.
    private boolean isCollectingProposals = true;
    // Seuil de gravité pour déterminer si un produit est réparable ou non.
    private static final int SOME_SEVERITY_THRESHOLD = 5;



    /**list of products to repair*/
    List<Product> products;
    /**general skill of repairing*/
    int skill;
    /**gui window*/
    UserAgentWindow window;

    /** Méthode setup appelée lors de l'initialisation de l'agent.*/
    @Override
    public void setup() {
        // Configuration de l'interface utilisateur.
        this.window = new UserAgentWindow(getLocalName(), this);
        window.setButtonActivated(true);
        Random hasard = new Random();// Détermination aléatoire de la compétence de réparation.
        skill = hasard.nextInt(5);
        println("hello, I have a skill = " + skill);

        // Génération aléatoire de la liste des produits à réparer.
        products = new ArrayList<>();
        int nbTypeOfProducts = ProductType.values().length;
        int nbPoductsByType = Product.NB_PRODS / nbTypeOfProducts;
        var existingProducts = Product.getListProducts();
        for (int i = 0; i < nbTypeOfProducts; i++) {
            if (hasard.nextBoolean()) {
                Product product = existingProducts.get(hasard.nextInt(nbPoductsByType) + (i * nbPoductsByType));
                // Définir l'état de réparabilité.
                product.setRepairState(determineProductState(product));
                products.add(product);
            }
        }
        if (products.isEmpty()) {
            Product product = existingProducts.get(hasard.nextInt(nbPoductsByType * nbTypeOfProducts));
            product.setRepairState(determineProductState(product));
            products.add(product);
        }
        println("Generated " + products.size() + " products for repair. Here are my objects: ");
        products.forEach(p -> println("\t" + p));

        // Ajout des produits à l'interface utilisateur et affichage dans la console.
        window.addProductsToCombo(products);

        // Ajout de différents comportements à cet agent.
        addBehaviour(new SendRepairRequestBehaviour());
        addBehaviour(new HandleCFPResponsesBehaviour());
        /** Ajout d'un comportement pour gérer la décision après la sélection.*/
        addBehaviour(new WakerBehaviour(this, 10000) {
            @Override
            protected void onWake() {
                isCollectingProposals = false;
                selectBestProposal();
            }
        });

        // Ajout d'un comportement pour traiter la décision post-sélection
        addBehaviour(new PostSelectionBehaviour());
    }

    private List<AID> findRepairCoffeeAgents() {
        List<AID> agents = new ArrayList<>();
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("repair");
        sd.setName("coffee");
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            for (DFAgentDescription agentDesc : result) {
                agents.add(agentDesc.getName());
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        return agents;
    }

    private String createMessageContentFromProduct(Product product) {
        return product.getType().name() + "-" + determineProductState(product);
    }

    private class SendRepairRequestBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            // Trouver les agents café de réparation
            List<AID> repairCoffees = findRepairCoffeeAgents();
            Product selectedProduct = window.getSelectedProduct();

            // Vérifier si un produit a été sélectionné
            if (selectedProduct != null) {
                // Préparer le contenu du message
                String messageContent = createMessageContentFromProduct(selectedProduct);
                // Créer et envoyer un message CFP aux cafés de réparation pour le produit sélectionné
                sendCFPToRepairCoffees(repairCoffees, messageContent);
            } else {
                println("Aucun produit sélectionné pour la réparation.");
            }
        }


    }


    private class HandleCFPResponsesBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            if (!isCollectingProposals) {
                block();
                return;
            }

            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String content = msg.getContent();
                try {
                    String dateString = extractDate(content);
                    LocalDate proposedDate = LocalDate.parse(dateString);
                    receivedProposals.put(msg.getSender(), proposedDate);
                } catch (DateTimeParseException e) {
                    e.printStackTrace();
                }
            } else {
                block();
            }
        }
        private String extractDate(String content) {
            String[] parts = content.split("\\.");
            for (String part : parts) {
                if (part.contains("Proposed repair date")) {
                    return part.split(":")[1].trim();
                }
            }
            throw new DateTimeParseException("Date not found in content", content, 0);
        }
    }




    private void selectBestProposal() {
        AID bestAgent = null;
        LocalDate earliestDate = LocalDate.MAX;

        for (Map.Entry<AID, LocalDate> entry : receivedProposals.entrySet()) {
            if (entry.getValue().isBefore(earliestDate)) {
                earliestDate = entry.getValue();
                bestAgent = entry.getKey();
            }
        }

        if (bestAgent != null) {
            sendAcceptance(bestAgent);
        }
    }

    private void sendAcceptance(AID bestAgent) {
        ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
        accept.addReceiver(bestAgent);
        send(accept);
    }

    private String determineProductState(Product product) {
        // Détermination de l'état de réparabilité en fonction du type de produit
        switch (product.getType()) {
            case Mouse:
                // Exemple: Les souris sont généralement faciles à réparer
                return "facilement réparable";
            case screen:
                // Les écrans peuvent être difficiles à réparer en fonction des dommages
                return "difficilement réparable";
            case coffeeMaker:
                // Les cafetières peuvent avoir des problèmes mineurs
                return "légèrement endommagé";
            case washingMachine:
                // Les machines à laver peuvent être obsolètes ou compliquées à réparer
                return "obsolète";
            case dishwasher:
                // Lave-vaisselle : potentiellement compliqué à réparer
                return "fortement endommagé";
            case vacuumCleaner:
                // Les aspirateurs peuvent généralement être réparés facilement
                return "facilement réparable";
            default:
                // Pour tout autre type non spécifié
                return "état inconnu";
        }
    }



    private class PostSelectionBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            Product selectedProduct = selectProductForAction();

            if (selectedProduct != null) {
                if (receivedProposals.containsKey(selectedProduct)) {
                    println("Selected product for repair: " + selectedProduct.getName());
                    requestRepairPart(selectedProduct);
                } else {
                    println("Selected product for replacement: " + selectedProduct.getName());
                    searchForNewProduct(selectedProduct.getType());
                }
            } else {
                println("No products available for action.");
            }
        }

        private Product selectProductForAction() {
            return products.stream()
                    .filter(p -> receivedProposals.containsKey(p))
                    .min(Comparator.comparing(p -> receivedProposals.get(p)))
                    .orElse(null);
        }
    }


    /** Demande une pièce de réparation aux magasins de pièces détachées.*/
    private void requestRepairPart(Product product) {
        List<AID> partStores = findPartStores();
        if (!partStores.isEmpty()) {
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            for (AID store : partStores) {
                cfp.addReceiver(store);
                println("Sending CFP to Part Store: " + store.getName());
            }
            cfp.setContent(product.getName());
            cfp.setProtocol(InteractionProtocol.FIPA_CONTRACT_NET);
            send(cfp);
        } else {
            println("No Part Store Agents found.");
        }
    }


    private List<AID> findPartStores() {
        List<AID> partStores = new ArrayList<>();
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("part-store");
        sd.setName("part-store");
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            for (DFAgentDescription agentDesc : result) {
                partStores.add(agentDesc.getName());
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        return partStores;
    }

    private List<AID> findDistributors() {
        List<AID> distributors = new ArrayList<>();
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("product-distribution");
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            for (DFAgentDescription agentDesc : result) {
                distributors.add(agentDesc.getName());
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        return distributors;
    }


    private void searchForNewProduct(ProductType type) {
        List<AID> distributors = findDistributors();

        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
        for (AID distributor : distributors) {
            cfp.addReceiver(distributor);
            println("Sending CFP to Distributor: " + distributor.getName());
        }
        cfp.setContent(type.toString());
        cfp.setProtocol(InteractionProtocol.FIPA_CONTRACT_NET);
        send(cfp);
    }


    /**the window sends an evt to the agent*/
    @Override
    public void onGuiEvent(GuiEvent evt)
    {
        //if it is the OK button
        if(evt.getType()==UserAgentWindow.OK_EVENT)
        {
            //search about repair coffee
            var coffees = AgentServicesTools.searchAgents(this, "repair", "coffee");
            println("-".repeat(30));
            for(AID aid:coffees)
                println("found this repair coffee : " + aid.getLocalName());
            println("-".repeat(30));

            // Obtenez le produit sélectionné par l'utilisateur
            Product selectedProduct = (Product) window.getSelectedProduct();
            if (selectedProduct != null) {
                // Envoyer un appel CFP pour le produit sélectionné
                sendCFPForSelectedProduct(selectedProduct);
            } else {
                println("No product selected.");
            }

        }
    }

    private void sendCFPToRepairCoffees(List<AID> repairCoffees, String messageContent) {
        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
        for (AID aid : repairCoffees) {
            cfp.addReceiver(aid);
        }
        cfp.setProtocol(InteractionProtocol.FIPA_CONTRACT_NET);
        cfp.setContent(messageContent);
        send(cfp);
    }

    // Méthode pour envoyer un CFP pour le produit sélectionné
    private void sendCFPForSelectedProduct(Product selectedProduct) {
        List<AID> repairCoffees = findRepairCoffeeAgents();
        // Inclure à la fois le type de produit et son état dans le contenu du message
        String messageContent = selectedProduct.getType().name() + "-" + determineProductState(selectedProduct);
        println("Sending CFP with content: " + messageContent);
        sendCFPToRepairCoffees(repairCoffees, messageContent);
    }

    public void println(String s){window.println(s);}

    @Override
    public void takeDown(){println("bye !!!");}
}
