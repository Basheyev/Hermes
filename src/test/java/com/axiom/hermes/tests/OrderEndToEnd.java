package com.axiom.hermes.tests;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderEndToEnd {
    @Test
    @Order(1)
    public void saleEndToEnd() {
        // todo добавить тесты на остатки, бронь и доступно для покупки
        //  1) Сохранить текущие остатки доступные для продажи
        //  2) Создать заказ
        //  3) Добавть позицию заказа
        //  4) Установить статус - подтвердить заказ
        //  5) Проверить доступные остатки (должно быть)
        //  6) Отгрузить заказ
        //  7) Сверить остатки
    }
}
