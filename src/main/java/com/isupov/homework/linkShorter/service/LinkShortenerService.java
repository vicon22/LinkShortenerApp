package com.isupov.homework.linkShorter.service;

// Основной класс приложения
import com.isupov.homework.linkShorter.models.Link;
import com.isupov.homework.linkShorter.models.User;

import java.awt.Desktop;
import java.net.URI;
import java.util.*;
import java.io.*;
import java.util.Scanner;

public class LinkShortenerService {

    // Параметры конфигурации
    private static final long DEFAULT_EXPIRY_TIME;
    private static final int DEFAULT_MAX_CLICKS;

    private static final Scanner scanner = new Scanner(System.in);

    static {
        long expiryTime;
        int maxClicks;

        Properties config = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            config.load(input);
            expiryTime = Long.parseLong(config.getProperty("DEFAULT_EXPIRY_TIME", "60")) * 60 * 1000;
            maxClicks = Integer.parseInt(config.getProperty("DEFAULT_MAX_CLICKS", "10"));
        } catch (IOException e) {
            System.out.println("Ошибка загрузки конфигурации! Установлены значения по умолчанию DEFAULT_EXPIRY_TIME - 60 минут. DEFAULT_MAX_CLICKS - 10");
            expiryTime = 60 * 60 * 1000;
            maxClicks = 10;
        }

        DEFAULT_EXPIRY_TIME = expiryTime;
        DEFAULT_MAX_CLICKS = maxClicks;
    }

    // Карта для хранения данных пользователей
    private final Map<String, User> users = new HashMap<>();

    // Генерация UUID для пользователя
    public String generateUuid() {
        return UUID.randomUUID().toString();
    }

    // Создание короткой ссылки
    public String createShortLink(String uuid, String originalUrl, int userMaxClicks, long userExpiryTime) {
        User user = users.computeIfAbsent(uuid, User::new);
        int maxClicks = Math.max(userMaxClicks, DEFAULT_MAX_CLICKS);
        long expiryTime = Math.min(userExpiryTime, DEFAULT_EXPIRY_TIME);

        String shortUrl = "clck.ru/" + UUID.randomUUID().toString().substring(0, 6);
        Link link = new Link(originalUrl, shortUrl, System.currentTimeMillis() + expiryTime, maxClicks);
        user.getLinks().put(shortUrl, link);
        return shortUrl;
    }

    // Переход по короткой ссылке
    public String accessShortLink(String shortUrl) {
        try {
            for (User user : users.values()) {
                Link link = user.getLinks().get(shortUrl);
                if (link != null) {
                    if (link.isExpired()) {
                        return "Ссылка истекла.";
                    }
                    if (link.isLimitReached()) {
                        return "Лимит переходов исчерпан.";
                    }
                    link.incrementClickCount();
                    Desktop.getDesktop().browse(new URI(link.getOriginalUrl()));
                    return "Открытие ссылки в браузере: " + link.getOriginalUrl();
                }
            }
        } catch (Exception e) {
            return "Ошибка при открытии ссылки: " + e.getMessage();
        }
        return "Ссылка не найдена.";
    }

    // Удаление старых ссылок
    public void cleanUpExpiredLinks() {
        for (User user : users.values()) {
            user.getLinks().values().removeIf(Link::isExpired);
        }
    }

    // Удаление ссылки пользователем
    public String deleteShortLink(String uuid, String shortUrl) {
        User user = users.get(uuid);
        if (user != null && user.getLinks().remove(shortUrl) != null) {
            return "Ссылка успешно удалена.";
        }
        return "Ссылка не найдена или вы не являетесь её владельцем.";
    }

    // Изменение лимита переходов
    public String updateClickLimit(String uuid, String shortUrl, int newLimit) {
        User user = users.get(uuid);
        if (user != null) {
            Link link = user.getLinks().get(shortUrl);
            if (link != null) {
                link.setMaxClicks(Math.max(newLimit, DEFAULT_MAX_CLICKS));
                return "Лимит переходов успешно обновлён.";
            }
        }
        return "Ссылка не найдена или вы не являетесь её владельцем.";
    }

    public void run() {

        System.out.println("Добро пожаловать в сервис сокращения ссылок!");

        String currentUserUuid = null;

        while (true) {
            if (currentUserUuid == null) {
                System.out.println("""
                        Введите команду:
                        1. Войти по UUID
                        2. Создать нового пользователя
                        3. Выйти
                        """);
                System.out.print("Ваш выбор: ");
                int userChoice = scanner.nextInt();
                scanner.nextLine();

                switch (userChoice) {
                    case 1:
                        System.out.print("Введите ваш UUID: ");
                        String uuid = scanner.nextLine();
                        if (users.containsKey(uuid)) {
                            currentUserUuid = uuid;
                            System.out.println("Успешный вход!");
                        } else {
                            System.out.println("Пользователь с таким UUID не найден.");
                        }
                        break;

                    case 2:
                        currentUserUuid = generateUuid();
                        users.put(currentUserUuid, new User(currentUserUuid));
                        System.out.println("Ваш новый UUID: " + currentUserUuid);
                        break;

                    case 3:
                        System.out.println("Спасибо за использование сервиса!");
                        scanner.close();
                        return;

                    default:
                        System.out.println("Неверная команда. Попробуйте снова.");
                }
            } else {
                System.out.println("""
                        Введите команду:
                        1. Создать ссылку
                        2. Перейти по ссылке
                        3. Удалить ссылку
                        4. Изменить лимит переходов
                        5. Выйти из аккаунта
                        6. Завершить программу
                        
                        """);
                System.out.print("Ваш выбор: ");
                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1:
                        System.out.print("Введите оригинальный URL: ");
                        String originalUrl = scanner.nextLine();
                        System.out.print("Введите максимальное количество переходов: ");
                        int maxClicks = scanner.nextInt();
                        System.out.print("Введите время жизни ссылки (в минутах): ");
                        long expiryTime = scanner.nextLong() * 60 * 1000;
                        scanner.nextLine();

                        String shortLink = createShortLink(currentUserUuid, originalUrl, maxClicks, expiryTime);
                        System.out.println("Короткая ссылка: " + shortLink);
                        break;

                    case 2:
                        System.out.print("Введите короткую ссылку: ");
                        String shortUrl = scanner.nextLine();
                        String result = accessShortLink(shortUrl);
                        System.out.println(result);
                        break;

                    case 3:
                        System.out.print("Введите короткую ссылку для удаления: ");
                        String linkToDelete = scanner.nextLine();
                        String deleteResult = deleteShortLink(currentUserUuid, linkToDelete);
                        System.out.println(deleteResult);
                        break;

                    case 4:
                        System.out.print("Введите короткую ссылку для изменения лимита: ");
                        String linkToUpdate = scanner.nextLine();
                        System.out.print("Введите новый лимит переходов: ");
                        int newLimit = scanner.nextInt();
                        scanner.nextLine();
                        String updateResult = updateClickLimit(currentUserUuid, linkToUpdate, newLimit);
                        System.out.println(updateResult);
                        break;

                    case 5:
                        System.out.println("Вы вышли из аккаунта.");
                        currentUserUuid = null;
                        break;

                    case 6:
                        System.out.println("Спасибо за использование сервиса!");
                        scanner.close();
                        return;

                    default:
                        System.out.println("Неверная команда. Попробуйте снова.");
                }
            }
        }
    }

    private String showAuthMenu() {
        String currentUserUuid = null;

        System.out.println("""
                        Введите команду:
                        1. Войти по UUID
                        2. Создать нового пользователя
                        3. Выйти
                        """);
        System.out.print("Ваш выбор: ");
        int userChoice = scanner.nextInt();
        scanner.nextLine();

        switch (userChoice) {
            case 1:
                System.out.print("Введите ваш UUID: ");
                String uuid = scanner.nextLine();
                if (users.containsKey(uuid)) {
                    currentUserUuid = uuid;
                    System.out.println("Успешный вход!");
                } else {
                    System.out.println("Пользователь с таким UUID не найден.");
                }
                break;

            case 2:
                currentUserUuid = generateUuid();
                users.put(currentUserUuid, new User(currentUserUuid));
                System.out.println("Ваш новый UUID: " + currentUserUuid);
                break;

            case 3:
                System.out.println("Спасибо за использование сервиса!");
                System.exit(0);
            default:
                System.out.println("Неверная команда. Попробуйте снова.");
        }

        return currentUserUuid;
    }
}
