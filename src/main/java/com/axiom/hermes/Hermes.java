package com.axiom.hermes;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

// todo Добавить защиту от SQL инъекций

// Examples: https://github.com/shopizer-ecommerce
/**
 * Профиль нагрузки примерно следующий:
 * 1. Дистрибьютор имеет до 10 пользователей
 * 2. Клиентов около 300 пользователей
 * 3. Каждый клиент делает еженедельный заказ по 120 позиций
 * 4. В каталоге около 500 позиций который ежедневно смотрит 300 пользователей
 */
@QuarkusMain
public class Hermes {
    public static void main(String[] args) {
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("Hermes: wholesale and distribution server");
        System.out.println("--------------------------------------------------------------------------");
        Quarkus.run(args);
    }
}
