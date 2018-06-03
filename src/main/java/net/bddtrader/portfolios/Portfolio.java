package net.bddtrader.portfolios;

import net.bddtrader.tradingdata.PriceReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.bddtrader.portfolios.Trade.deposit;

/**
 * A Portfolio records the financial position of a client, as well as the history of their trades.
 */
public class Portfolio {

    private static Long INITIAL_DEPOSIT_IN_DOLLARS = 1000L;

    private final Long portfolioId;
    private final Long clientId;
    private final List<Trade> history = new CopyOnWriteArrayList<>();

    public Portfolio(Long portfolioId, Long clientId) {
        this.portfolioId = portfolioId;
        this.clientId = clientId;
        placeOrder(deposit(INITIAL_DEPOSIT_IN_DOLLARS).dollars());
    }

    public Long getPortfolioId() {
        return portfolioId;
    }

    public Long getClientId() {
        return clientId;
    }

    public double getCash() {
        return getCashPosition().orElse(Position.EMPTY_CASH_POSITION).getTotalValueInDollars();
    }

    private Long getCashInCents() {
        return getCashPosition().orElse(Position.EMPTY_CASH_POSITION).getTotalValueInCents();
    }

    public void placeOrder(Trade trade) {

        ensureSufficientFundsAreAvailableFor(trade);

        trade.cashTransation().ifPresent( history::add );

        history.add(trade);
    }

    public OrderPlacement placeOrderUsingPricesFrom(PriceReader priceReader) {
        return new OrderPlacement(priceReader);
    }

    public class OrderPlacement {
        private PriceReader priceReader;

        OrderPlacement(PriceReader priceReader) {
            this.priceReader = priceReader;
        }

        public void forTrade(Trade trade) {
            if (shouldFindMarketPriceFor(trade)) {
                trade = trade.atPrice(priceReader.getPriceFor(trade.getSecurityCode()));
            }

            placeOrder(trade);
        }
    }

    private boolean shouldFindMarketPriceFor(Trade trade) {
        return trade.getPriceInCents() == 0;
    }

    private void ensureSufficientFundsAreAvailableFor(Trade trade) {

        if (trade.getType() != TradeType.Buy) { return; }

        if (!hasSufficientFundsFor(trade)){
            throw new InsufficientFundsException("Insufficient funds: " + getCash() + " for purchase of " + trade.getTotalInCents() / 100);
        }
    }

    public boolean hasSufficientFundsFor(Trade trade) {
        return (trade.getType().direction() == TradeDirection.Increase) || ((getCashInCents() >= trade.getTotalInCents()));

    }

    public Map<String, Position> calculatePositionsUsing(PriceReader priceReader) {

        Positions positions = getPositions();

        positions.updateMarketPricesUsing(priceReader);

        return positions.getPositions();
    }

    public Double calculateProfitUsing(PriceReader priceReader) {

        Positions positions = getPositions();

        positions.updateMarketPricesUsing(priceReader);

        return positions.getPositions().values().stream()
                .filter(position -> (!position.getSecurityCode().equals(Trade.CASH_ACCOUNT)))
                .mapToDouble(Position::getProfit)
                .sum();
    }

    private Optional<Position> getCashPosition() {
        return Optional.ofNullable(getPositions().getPositions().get(Trade.CASH_ACCOUNT));
    }

    public List<Trade> getHistory() {
        return new ArrayList<>(history);
    }


    public Positions getPositions() {
        Positions positions = new Positions();

        for (Trade trade : history) {
            positions.apply(trade);
        }
        return positions;
    }
}
