package com.isupov.homework.linkShorter.service;

import com.isupov.homework.linkShorter.models.Link;
import com.isupov.homework.linkShorter.models.User;

import java.awt.Desktop;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

public class LinkShortenerService {

    private static final int DEFAULT_EXPIRY_TIME;
    private static final int DEFAULT_MAX_CLICKS;
    private final Map<String, User> users = new HashMap<>();
    private final Scanner scanner = new Scanner(System.in);
    private String currentUserUuid;

    static {
        int expiryTime;
        int maxClicks;

        Properties config = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            config.load(input);
            expiryTime = Integer.parseInt(config.getProperty("DEFAULT_EXPIRY_TIME", "60"));
            maxClicks = Integer.parseInt(config.getProperty("DEFAULT_MAX_CLICKS", "10"));
        } catch (IOException e) {
            System.out.println("Ошибка загрузки конфигурации! Установлены значения по умолчанию DEFAULT_EXPIRY_TIME - 60 минут. DEFAULT_MAX_CLICKS - 10");
            expiryTime = 60;
            maxClicks = 10;
        }

        DEFAULT_EXPIRY_TIME = expiryTime;
        DEFAULT_MAX_CLICKS = maxClicks;
    }

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

    public String deleteShortLink(String uuid, String shortUrl) {
        User user = users.get(uuid);
        if (user != null && user.getLinks().remove(shortUrl) != null) {
            return "Ссылка успешно удалена.";
        }
        return "Ссылка не найдена или вы не являетесь её владельцем.";
    }

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
        startCleanupTask();

        while (true) {
            if (currentUserUuid == null) {
                showAuthMenu();
            } else {
                showMainMenu();
            }
        }
    }

    private void showAuthMenu() {

        System.out.println("""
                        Введите команду:
                        1. Войти по UUID
                        2. Создать нового пользователя
                        3. Выйти
                        """);
        System.out.print("Ваш выбор: ");
        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                login();
                break;
            case "2":
                register();
                break;
            case "3":
                exit();
            default:
                System.out.println("Неверная команда. Попробуйте снова.");
        }
    }

    private void showMainMenu() {
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
        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                createShortLink();
                break;
            case "2":
                accessShortLink();
                break;
            case "3":
                deleteLink();
                break;
            case "4":
                changeLimit();
                break;
            case "5":
                System.out.println("Вы вышли из аккаунта.");
                currentUserUuid = null;
                break;
            case "6":
                exit();
            default:
                System.out.println("Неверная команда. Попробуйте снова.");
        }
    }

    private void login() {
        System.out.print("Введите ваш UUID: ");
        String uuid = scanner.nextLine();
        if (users.containsKey(uuid)) {
            currentUserUuid = uuid;
            System.out.println("Успешный вход!");
        } else {
            System.out.println("Пользователь с таким UUID не найден.");
        }
    }

    private void register() {
        currentUserUuid = UUID.randomUUID().toString();;
        users.put(currentUserUuid, new User(currentUserUuid));
        System.out.println("Ваш новый UUID: " + currentUserUuid);
    }

    private void exit() {
        System.out.println("До свидания!");
        System.exit(0);
    }

    private void changeLimit() {
        System.out.print("Введите короткую ссылку для изменения лимита: ");
        String linkToUpdate = scanner.nextLine();
        int newLimit = getIntFromTerminal("новый лимит переходов");
        String updateResult = updateClickLimit(currentUserUuid, linkToUpdate, newLimit);
        System.out.println(updateResult);
    }

    private void deleteLink() {
        System.out.print("Введите короткую ссылку для удаления: ");
        String linkToDelete = scanner.nextLine();
        String deleteResult = deleteShortLink(currentUserUuid, linkToDelete);
        System.out.println(deleteResult);
    }

    private void accessShortLink() {
        System.out.print("Введите короткую ссылку: ");
        String shortUrl = scanner.nextLine();
        String result = accessShortLink(shortUrl);
        System.out.println(result);
    }

    private void createShortLink() {
        System.out.print("Введите оригинальный URL: ");
        String originalUrl = scanner.nextLine();
        int maxClicks = getIntFromTerminal("максимальное количество переходов");
        int expiryTimeMinutes = getIntFromTerminal("время жизни ссылки (в минутах)");

        String shortLink = createShortLink(currentUserUuid, originalUrl, maxClicks, expiryTimeMinutes);
        System.out.println("Короткая ссылка: " + shortLink);
    }

    public String createShortLink(String uuid, String originalUrl, int userMaxClicks, int userExpiryTimeMinutes) {
        User user = users.computeIfAbsent(uuid, User::new);
        int maxClicks = Math.max(userMaxClicks, DEFAULT_MAX_CLICKS);
        long expiryTimeMinutes = Math.min(userExpiryTimeMinutes, DEFAULT_EXPIRY_TIME);

        String shortUrl = "clck.ru/" + UUID.randomUUID().toString().substring(0, 6);
        Link link = new Link(originalUrl, shortUrl, System.currentTimeMillis() + expiryTimeMinutes * 60 * 1000, maxClicks);
        user.getLinks().put(shortUrl, link);
        return shortUrl;
    }

    private int getIntFromTerminal(String variableName) {
        System.out.printf("Введите %s: ", variableName);
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine());
            } catch (Exception e) {
                System.out.printf("Некорректный ввод! Введите %s еще раз: ", variableName);
            }
        }
    }

    // Запуск фоновой очистки
    public void startCleanupTask() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // Интервал очистки: 1 минута
                    cleanUpExpiredLinks();
                } catch (InterruptedException e) {
                    System.out.println("Фоновая очистка была прервана: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    // Автоматическое удаление старых ссылок
    public void cleanUpExpiredLinks() {
        for (User user : users.values()) {
            Iterator<Map.Entry<String, Link>> iterator = user.getLinks().entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Link> entry = iterator.next();
                if (entry.getValue().isExpired()) {
                    System.out.printf("Ссылка [%s] истекла. Удаление!%n", entry.getValue().getShortUrl());
                    iterator.remove();
                }
            }
        }
    }
}
