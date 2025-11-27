package org.dev.powermarket.domain.enums;


import lombok.Getter;

@Getter
public enum DashboardOrderStatus {
    AWAITING_ACCEPTANCE("На рассмотрении"),
    ACCEPTED("В процессе согласования"),
    IN_PROGRESS("В работе"),
    COMPLETED("Выполнен"),
    CANCELLED("Отклонён");

    private final String label;

    DashboardOrderStatus(String label) {
        this.label = label;
    }

}