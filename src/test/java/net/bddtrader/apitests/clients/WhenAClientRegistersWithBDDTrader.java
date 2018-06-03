package net.bddtrader.apitests.clients;

import net.bddtrader.clients.Client;
import net.bddtrader.clients.ClientController;
import net.bddtrader.clients.ClientDirectory;
import net.bddtrader.portfolios.PortfolioController;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class WhenAClientRegistersWithBDDTrader {

    PortfolioController portfolioController = mock(PortfolioController.class);
    ClientDirectory clientDirectory = new ClientDirectory();
    ClientController controller = new ClientController(clientDirectory, portfolioController);


    @Test
    public void aClientRegistersByProvidingANameAndAPassword() {

        // WHEN
        Client registeredClient = controller.register(Client.withFirstName("Sarah-Jane").andLastName("Smith"));

        // THEN
        assertThat(registeredClient).isEqualToComparingFieldByField(registeredClient);
    }

    @Test
    public void registeredClientsAreStoredInTheClientDirectory() {

        // WHEN
        Client registeredClient = controller.register(Client.withFirstName("Sarah-Jane").andLastName("Smith"));

        // THEN
        assertThat(clientDirectory.findClientById(1))
                .isPresent()
                .contains(registeredClient);
    }

    @Test
    public void registeredClientsCanBeRetrievedById() {

        // GIVEN
        Client sarahJane = controller.register(Client.withFirstName("Sarah-Jane").andLastName("Smith"));

        // WHEN
        Client foundClient = controller.findClientById(1L).getBody();

        // THEN
        assertThat(foundClient).isEqualTo(sarahJane);
    }

    @Test
    public void registeredClientsCanBeListed() {

        // GIVEN
        controller.register(Client.withFirstName("Sarah-Jane").andLastName("Smith"));
        controller.register(Client.withFirstName("Joe").andLastName("Smith"));

        // WHEN
        List<Client> foundClients = controller.findAll();

        // THEN
        assertThat(foundClients).hasSize(2);
    }

    @Test
    public void returns404WhenNoMatchingClientIsFound() {

        // GIVEN
        controller.register(Client.withFirstName("Sarah-Jane").andLastName("Smith"));

        // WHEN
        HttpStatus status = controller.findClientById(100L).getStatusCode();

        // THEN
        assertThat(status).isEqualTo(HttpStatus.NOT_FOUND);
    }

}
