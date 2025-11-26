package org.dev.powermarket.domain.enums;

public enum RentalRequestStatus {
    PENDING,           // Ожидает ответа арендодателя
    IN_CONTRACT,       // В статусе договора (обе стороны обсуждают детали)
    CONFIRMED,         // Обе стороны подтвердили, готово к аренде
    IN_RENT,           // В аренде (активная аренда)
    COMPLETED,         // Аренда завершена
    REJECTED,          // Отклонено арендодателем
    CANCELLED          // Отменено
}
