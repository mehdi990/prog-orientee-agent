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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**class related to the user, owner of products to repair
 * @author emmanueladam
 * */
public class UserAgent extends GuiAgent {
    private Map<AID, LocalDate> receivedProposals = new HashMap<>();
    private boolean isCollectingProposals = true;
    private static final int SOME_SEVERITY_THRESHOLD = 5;

    /**list of products to repair*/
    List<Product> products;
    /**general skill of repairing*/
    int skill;
    /**gui window*/
    UserAgentWindow window;
    @Override
    public void setup() {
        this.window = new UserAgentWindow(getLocalName(), this);
        window.setButtonActivated(true);
        Random hasard = new Random();
        skill = hasard.nextInt(5);
        println("hello, I have a skill = " + skill);

        products = new ArrayList<>();
        int nbTypeOfProducts = ProductType.values().length;
        int nbPoductsByType = Product.NB_PRODS / nbTypeOfProducts;
        var existingProducts = Product.getListProducts();
        for (int i = 0; i < nbTypeOfProducts; i++) {
            if (hasard.nextBoolean()) {
                Product product = existingProducts.get(hasard.nextInt(nbPoductsByType) + (i * nbPoductsByType));
                product.setRepairState(determineProductState(product)); // Définir l'état de réparabilité
                products.add(product);
            }
        }
        if (products.isEmpty()) {
            Product product = existingProducts.get(hasard.nextInt(nbPoductsByType * nbTypeOfProducts));
            product.setRepairState(determineProductState(product));
            products.add(product);
        }
        window.addProductsToCombo(products);
        println("Here are my objects : ");
        products.forEach(p -> println("\t" + p));

        addBehaviour(new SendRepairRequestBehaviour());
        addBehaviour(new HandleCFPResponsesBehaviour());

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


    private class SendRepairRequestBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            // Trouver les agents café de réparation
            List<AID> repairCoffees = findRepairCoffeeAgents();

            // Créer et envoyer un message CFP aux cafés de réparation
            sendCFPToRepairCoffees(repairCoffees);
        }

        private List<AID> findRepairCoffeeAgents() {
            List<AID> agents = new ArrayList<>();
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("repair-coffee");
            template.addServices(sd);

            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                for (DFAgentDescription agentDesc : result) {
                    agents.add(agentDesc.getName());
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
            return agents;
        }

        private void sendCFPToRepairCoffees(List<AID> repairCoffees) {
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            for (AID aid : repairCoffees) {
                cfp.addReceiver(aid);
            }
            cfp.setProtocol(InteractionProtocol.FIPA_CONTRACT_NET);
            cfp.setContent("Besoin de réparer mon ordinateur portable"); // Définir en fonction du produit
            send(cfp);
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
                try {
                    LocalDate proposedDate = LocalDate.parse(msg.getContent());
                    receivedProposals.put(msg.getSender(), proposedDate);
                } catch (DateTimeParseException e) {
                    e.printStackTrace();
                }
            } else {
                block();
            }
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


    private boolean isProductRepairable(Product product) {
        return product.isRepairable();
    }

    private class PostSelectionBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            // Vérifiez d'abord si la liste 'products' n'est pas vide
            if (products.isEmpty()) {
                println("Aucun produit à traiter.");
                return;
            }

            // Supposons que vous voulez traiter le premier produit de la liste
            Product selectedProduct = products.get(0);

            // Vérifiez si le produit sélectionné est réparable
            if (isProductRepairable(selectedProduct)) {
                // Si réparable, demandez une pièce de réparation
                requestRepairPart(selectedProduct);
            } else {
                // Si non réparable, recherchez un nouveau produit
                searchForNewProduct(selectedProduct.getType());
            }
        }
    }



    private void requestRepairPart(Product product) {
        List<AID> partStores = findPartStores(); // Trouver les PartStoreAgent

        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
        for (AID store : partStores) {
            cfp.addReceiver(store);
        }
        cfp.setContent("Demande de pièce pour : " + product.getName());
        cfp.setProtocol(InteractionProtocol.FIPA_CONTRACT_NET);
        send(cfp);
    }

    private List<AID> findPartStores() {
        List<AID> partStores = new ArrayList<>();
        // Logique pour trouver les PartStoreAgent dans le DF (Directory Facilitator)
        return partStores;
    }

    private List<AID> findDistributors() {
        List<AID> distributors = new ArrayList<>();
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("distributor-service");  // Assurez-vous que ce type correspond à celui enregistré par les DistributorAgent
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
        }
        cfp.setContent("Recherche de nouveau produit de type : " + type);
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
            //TODO: Up to you to omplete the project....
        }
    }

    public void println(String s){window.println(s);}

    @Override
    public void takeDown(){println("bye !!!");}
}
