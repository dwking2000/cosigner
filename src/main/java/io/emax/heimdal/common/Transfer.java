package io.emax.heimdal.common;

import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;

/**
 * POJO for representing account transfers
 */
public class Transfer {
    public final @NotNull String currency;
    public final @NotNull String to;
    public final @NotNull String from;
    public final @NotNull String amount;
    public final Long counter;

    public Transfer(@NotNull String to,
                    @NotNull String from,
                    @NotNull String amount,
                    @NotNull String currency,
                    Long counter) {
        this.to = to;
        this.from = from;
        this.amount = amount;
        this.currency = currency;
        this.counter = counter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Transfer transfer = (Transfer) o;

        if (!currency.equals(transfer.currency)) return false;
        if (!to.equals(transfer.to)) return false;
        if (!from.equals(transfer.from)) return false;
        if (!amount.equals(transfer.amount)) return false;
        return !(counter != null ? !counter.equals(transfer.counter) : transfer.counter != null);

    }

    @Override
    public int hashCode() {
        int result = currency.hashCode();
        result = 31 * result + to.hashCode();
        result = 31 * result + from.hashCode();
        result = 31 * result + amount.hashCode();
        result = 31 * result + (counter != null ? counter.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return (new Gson()).toJson(this);
    }
}
