package com.example.moneyhelper.parser;

import android.content.Context;
import android.net.Uri;

import android.util.Log;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Парсер выписок Сбербанка в формате PDF
 * Использует библиотеку PdfBox-Android
 */
public class SberbankStatementParser {
    private static final String TAG = "SberbankParser";

    // Паттерны для парсинга
    private static final Pattern CATEGORY_PATTERN = Pattern.compile(
            "(Супермаркеты|Транспорт|Рестораны и кафе|Прочие расходы|Прочие операции|" +
                    "Перевод|Перевод СБП|Перевод на карту|Перевод с карты|Оплата по QR)"
    );

    private final Context context;
    private final SimpleDateFormat dateFormat;


    public SberbankStatementParser(Context context) {
        this.context = context;
        this.dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    }

    /**
     * Парсит PDF файл выписки
     */
    public List<Transaction> parseStatement(Uri pdfUri) throws Exception {
        List<Transaction> transactions = new ArrayList<>();

        try (InputStream inputStream = context.getContentResolver().openInputStream(pdfUri);
             PDDocument document = PDDocument.load(inputStream)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            Log.d(TAG, "Извлечен текст из PDF, длина: " + text.length());

            // Для отладки - выводим первые 2000 символов
            if (text.length() > 0) {
                Log.d(TAG, "Первые строки PDF:\n" + text.substring(0, Math.min(2000, text.length())));
            }

            transactions = parseTransactions(text);

            Log.d(TAG, "Распознано транзакций: " + transactions.size());

        } catch (Exception e) {
            Log.e(TAG, "Ошибка парсинга PDF", e);
            throw e;
        }

        return transactions;
    }

    /**
     * Парсит текст и извлекает транзакции
     */
    private List<Transaction> parseTransactions(String text) {
        List<Transaction> transactions = new ArrayList<>();
        String[] lines = text.split("\n");

        Log.d(TAG, "=== НАЧАЛО ПАРСИНГА ===");
        Log.d(TAG, "Всего строк в документе: " + lines.length);
        Log.d(TAG, "Формат: ДД.ММ.ГГГГ ЧЧ:ММ КОД КАТЕГОРИЯ СУММА ОСТАТОК");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.isEmpty()) continue;

            // Ищем строку с датой операции (начало транзакции)
            if (line.matches("\\d{2}\\.\\d{2}\\.\\d{4}\\s+\\d{2}:\\d{2}.*")) {
                Log.d(TAG, "\n--- НАЙДЕНА ТРАНЗАКЦИЯ (строка " + i + ") ---");
                Log.d(TAG, "Строка с датой: " + line);

                Transaction transaction = parseTransactionBlock(lines, i);
                if (transaction != null && !transaction.isIncome) {
                    transactions.add(transaction);
                    Log.d(TAG, "✓ Транзакция добавлена (расход)");
                    Log.d(TAG, String.format("  Сумма: %s, Категория: %s, Описание: %s",
                            transaction.amount, transaction.category, transaction.description));
                } else if (transaction != null && transaction.isIncome) {
                    // transactions.add(transaction);
                    // transaction.isIncome = true;
                   Log.d(TAG, "✗ Транзакция пропущена (доход): " + String.format("%s", transaction.amount));
                } else {
                    Log.d(TAG, "✗ Транзакция не распознана");
                }
            }
        }

        Log.d(TAG, "\n=== КОНЕЦ ПАРСИНГА ===");
        Log.d(TAG, "Распознано РАСХОДОВ: " + transactions.size());

        return transactions;
    }

    /**
     * Парсит блок транзакции (несколько строк)
     * Формат строки: ДД.ММ.ГГГГ ЧЧ:ММ КОД_АВТОРИЗАЦИИ КАТЕГОРИЯ СУММА ОСТАТОК
     */
    private Transaction parseTransactionBlock(String[] lines, int startIndex) {
        Transaction transaction = new Transaction();

        String mainLine = lines[startIndex].trim();
        Log.d(TAG, "Анализируем строку: " + mainLine);

        String transactionId = extractTransactionId(mainLine);
        if (transactionId != null) {
            transaction.id = transactionId;
            Log.d(TAG, "✓ Код транзакции: " + transactionId);
        }

        // 1. Парсим дату и время
        transaction.date = parseDate(mainLine);




        Log.d(TAG, "Дата: " + transaction.date);

        // 2. Извлекаем категорию из основной строки
        String category = findCategory(mainLine);
        if (category != null) {
            transaction.category = mapCategory(category);
            Log.d(TAG, "✓ Найдена категория: " + category + " -> " + transaction.category);
        }

        // 3. Извлекаем суммы (их может быть две: сумма операции и остаток)

        List<Integer> amounts = extractAllAmounts(mainLine);
        if (!amounts.isEmpty()) {
            // ВСЕГДА берем ПЕРВУЮ сумму - это сумма операции
            // Вторая сумма (если есть) - это остаток на счете
            transaction.amount = amounts.get(1);
            Log.d(TAG, "✓ Найдена сумма операции: " + transaction.amount);

            if (amounts.size() > 1) {
                Log.d(TAG, "  Остаток на счете: " + amounts.get(1));
            }

            // Определяем тип операции
            transaction.isIncome = mainLine.contains("+");
            Log.d(TAG, "Тип: " + (transaction.isIncome ? "Доход" : "Расход"));
        } else {
            Log.w(TAG, "✗ Сумма не найдена");
            return null;
        }

        // 4. Извлекаем описание из основной строки
        // Убираем дату, время, код, категорию, суммы
        String description = extractDescriptionFromMainLine(mainLine, category);
        if (description != null && !description.isEmpty()) {
            transaction.description = description;
            Log.d(TAG, "✓ Найдено описание из основной строки: " + description);
        }

        // 5. Если описания нет, ищем в следующих строках
        if (transaction.description == null || transaction.description.isEmpty()) {
            int endIndex = Math.min(startIndex + 5, lines.length);
            for (int i = startIndex + 1; i < endIndex; i++) {
                String line = lines[i].trim();
                if (!line.isEmpty() && !line.contains("Операция по карте")) {
                    String merchantName = extractMerchantName(line);
                    if (merchantName != null && !merchantName.isEmpty()) {
                        transaction.description = merchantName;
                        Log.d(TAG, "✓ Найдено описание в следующей строке: " + merchantName);
                        break;
                    }
                }
            }
        }

        // 6. Если категория не найдена, определяем по описанию
        if (transaction.category == null && transaction.description != null) {
            transaction.category = guessCategoryFromDescription(transaction.description);
            Log.d(TAG, "⚠ Категория определена по описанию: " + transaction.category);
        }

        // 7. Fallback значения
        if (transaction.category == null) {
            transaction.category = "Другое";
            Log.w(TAG, "⚠ Категория не найдена, установлена 'Другое'");
        }

        if (transaction.description == null || transaction.description.isEmpty()) {
            transaction.description = category != null ? category : "Без описания";
            Log.w(TAG, "⚠ Описание не найдено, используется категория или 'Без описания'");
        }

        return transaction;
    }

    /**
     * Парсит дату из строки
     */
    private Date parseDate(String line) {
        Pattern dateTimePattern = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})\\s+(\\d{2}:\\d{2})");
        Matcher matcher = dateTimePattern.matcher(line);

        if (matcher.find()) {
            String dateTimeStr = matcher.group(1) + " " + matcher.group(2);
            try {
                return dateFormat.parse(dateTimeStr);
            } catch (ParseException e) {
                Log.e(TAG, "Ошибка парсинга даты: " + dateTimeStr, e);
            }
        }

        return new Date();
    }

    /**
     * Извлекает код транзакции (авторизации) - 6 цифр после времени
     */
    private String extractTransactionId(String line) {
        // Паттерн: время (ЧЧ:ММ) затем пробелы и 6 цифр
        Pattern pattern = Pattern.compile("\\d{2}:\\d{2}\\s+(\\d{6})");
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Ищет категорию в строке
     */
    private String findCategory(String line) {
        Matcher matcher = CATEGORY_PATTERN.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Извлекает все суммы из строки (может быть несколько)
     * Формат: ДД.ММ.ГГГГ ЧЧ:ММ КОД КАТЕГОРИЯ СУММА_ОПЕРАЦИИ ОСТАТОК
     */
    private List<Integer> extractAllAmounts(String line) {
        List<Integer> amounts = new ArrayList<>();

        // 1. Очистка "мусора" (время и код авторизации), как было в оригинале
        String cleanedLine = line.replaceFirst("\\d{2}:\\d{2}\\s+\\d{6}\\s+", " ");

        // 2. Убираем ВСЕ пробелы, как ты и предложил
        // Пример: "Супермаркеты 349,97 36 975,65" -> "Супермаркеты349,9736975,65"
        String noSpacesLine = cleanedLine.replaceAll("\\s+", "");

        Log.d(TAG, "Строка без пробелов: " + noSpacesLine);

        // Паттерн:
        // ([^\\d])           -> Группа 1: Любой символ, КРОМЕ цифры (буква категории перед суммой)
        // ([+-]?\\d+[,.]\\d{2}) -> Группа 2: Сама сумма (опционально знак +/-, цифры, точка/запятая, ровно 2 цифры)
        Pattern pattern = Pattern.compile("([^\\d])([+-]?\\d+[,.]\\d{2})");
        Matcher matcher = pattern.matcher(noSpacesLine);

        while (matcher.find()) {
            // matcher.group(1) - это буква перед суммой (нам она нужна только для проверки паттерна)
            String amountStr = matcher.group(2); // Сама сумма, например "349,97" или "+109,00"

            try {
                // Заменяем запятую на точку для парсинга Java
                String parsableAmount = amountStr.replace(",", ".");

                // Double.parseDouble отлично понимает "+" и "-" в начале строки
                double amountP = Double.parseDouble(parsableAmount.replace(",", "."));
                int amount = (int) Math.round(amountP);

                // Проверка на адекватность суммы
                if (amount != 0 && Math.abs(amount) < 10000000) {
                    amounts.add(amount);
                    Log.d(TAG, "  Найдена сумма: " + amount);
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Ошибка парсинга суммы: " + amountStr, e);
            }
        }

        return amounts;
    }

    /**
     * Извлекает описание из основной строки
     * Убирает дату, время, код авторизации, категорию и суммы
     */
    private String extractDescriptionFromMainLine(String line, String category) {
        String cleaned = line;

        // Убираем дату и время
        cleaned = cleaned.replaceFirst("\\d{2}\\.\\d{2}\\.\\d{4}\\s+\\d{2}:\\d{2}", "");

        // Убираем код авторизации (6 цифр)
        cleaned = cleaned.replaceFirst("\\s*\\d{6}\\s*", " ");

        // Убираем категорию если она найдена
        if (category != null) {
            cleaned = cleaned.replace(category, "");
        }

        // Убираем все суммы (числа с точкой/запятой и 2 цифрами после)
        cleaned = cleaned.replaceAll("[+-]?\\s*\\d{1,3}(?:[,\\s]\\d{3})*[,.]\\d{2}", "");

        // Очищаем от лишних пробелов
        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        // Если что-то осталось и это не просто пробелы
        if (!cleaned.isEmpty() && cleaned.length() > 2) {
            return cleaned;
        }

        return null;
    }

    /**
     * Извлекает название места/продавца
     */
    private String extractMerchantName(String line) {
        // Пропускаем строки с датами
        if (line.matches(".*\\d{2}\\.\\d{2}\\.\\d{4}.*")) {
            return null;
        }

        // Пропускаем строки только с числами и суммами
        if (line.matches("^[\\d\\s.,+-]+$")) {
            return null;
        }

        // Убираем техническую информацию
        String cleaned = line
                .replaceAll("Операция по карте \\*+\\d+\\.?", "")
                .replaceAll("\\s+RUS\\.?$", "")
                .replaceAll("\\s+[A-Z]{3}\\s*$", "") // Коды валют
                .replaceAll("\\d{6,}", "") // Длинные числа (коды авторизации)
                .trim();

        // Убираем суммы денег из описания
        cleaned = cleaned.replaceAll("\\d+[,.]\\d{2}", "").trim();

        // Проверяем, что осталось что-то осмысленное
        if (cleaned.length() > 3 && !cleaned.matches("^[\\d\\s.,+-]+$")) {
            return cleaned;
        }

        return null;
    }

    /**
     * Определяет категорию по описанию места
     */
    private String guessCategoryFromDescription(String description) {
        if (description == null) return "Другое";

        String desc = description.toUpperCase();

        // Продукты
        if (desc.contains("MAGNIT") || desc.contains("PEREKRESTOK") ||
                desc.contains("PYATEROCHKA") || desc.contains("MONETKA") ||
                desc.contains("BRISTOL") || desc.contains("KRASNOE") ||
                desc.contains("BELOE") || desc.contains("POLYUSTORG")) {
            return "Продукты";
        }

        // Транспорт
        if (desc.contains("TRANSPORT") || desc.contains("TRAMVAI") ||
                desc.contains("МЕТРО") || desc.contains("ТАКСИ") ||
                desc.contains("YANDEX") && desc.contains("GO")) {
            return "Транспорт";
        }

        // Кафе и рестораны
        if (desc.contains("PAPA") || desc.contains("DZHONS") ||
                desc.contains("TURLOV") || desc.contains("SHAURMA") ||
                desc.contains("CAFE") || desc.contains("RESTAURANT")) {
            return "Кафе и рестораны";
        }

        // Яндекс сервисы
        if (desc.contains("YANDEX")) {
            if (desc.contains("PLUS")) return "Подписки";
            if (desc.contains("GO")) return "Транспорт";
            return "Другое";
        }

        // Переводы
        if (desc.contains("Перевод") || desc.contains("СБП")) {
            return "Переводы";
        }

        // ЖД билеты
        if (desc.contains("ж/д") || desc.contains("перевозок")) {
            return "Транспорт";
        }

        return "Другое";
    }

    /**
     * Маппинг категорий Сбербанка на категории приложения
     */
    private String mapCategory(String sberbankCategory) {
        switch (sberbankCategory) {
            case "Супермаркеты":
                return "Продукты";
            case "Транспорт":
                return "Транспорт";
            case "Рестораны и кафе":
                return "Кафе и рестораны";
            case "Прочие расходы":
            case "Прочие операции":
                return "Другое";
            case "Перевод":
            case "Оплата по QR":
            case "Перевод СБП":
            case "Перевод на карту":
            case "Перевод с карты":
                return "Переводы";
            default:
                return "Другое";
        }
    }

    /**
     * Класс для хранения данных транзакции
     */
    public static class Transaction {
        public Date date;
        public String id;
        public int amount;
        public String category;
        public String description;
        public boolean isIncome;

        @Override
        public String toString() {
            return String.format(Locale.getDefault(),
                    "Transaction{date=%s, amount=%s, category='%s', description='%s', isIncome=%b}",
                    date, amount, category, description, isIncome);
        }
    }
}