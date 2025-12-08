# 📦 Модуль "Категории" - Документация

## 🎯 Обзор

Модуль для управления категориями расходов с поддержкой бюджетирования и статистики.

## 📁 Структура файлов

```
com.example.moneyhelper/
├── models/
│   └── Category.java              # Модель категории
├── services/
│   └── CategoryService.java       # Бизнес-логика работы с категориями
├── CategoryAdapter.java           # Адаптер для RecyclerView
└── CategoriesFragment.java        # UI фрагмент
```

## 🗄️ Структура данных

### Таблицы БД

**categories** - глобальные категории
```sql
id, name, icon
```

**user_categories** - категории пользователя
```sql
id, user_id, cat_id, name, fixed
```

**monthly_expenses** - расходы по категориям за месяц
```sql
id, user_cat_id, expenses, date_id
```

**predict** - прогнозы/бюджеты
```sql
id, user_cat_id, predict
```

### Модель Category

```java
public class Category {
    // Идентификаторы
    private long userCategoryId;  // ID из user_categories
    private long categoryId;      // ID из categories
    
    // Данные
    private String name;          // Название
    private String icon;          // Эмодзи иконка
    private boolean isFixed;      // Фиксированная категория
    
    // Расчеты
    private double currentExpense; // Текущие расходы
    private double budget;         // Бюджет/прогноз
    private int percentage;        // % от общих расходов
}
```

## 🔧 CategoryService API

### Основные методы

#### getAllCategories()
```java
List<Category> categories = categoryService.getAllCategories();
```
Получает все категории пользователя за текущий месяц с расходами и бюджетами.

#### getCategoriesForMonth(Date month)
```java
List<Category> categories = categoryService.getCategoriesForMonth(new Date());
```
Получает категории за конкретный месяц.

#### createCategory(String name, String icon, boolean isFixed)
```java
long categoryId = categoryService.createCategory("Продукты", "🛒", false);
```
Создает новую категорию. Возвращает ID или -1 при ошибке.

#### updateCategory(long userCategoryId, String name, String icon, boolean isFixed)
```java
boolean success = categoryService.updateCategory(1, "Еда", "🍕", false);
```
Обновляет существующую категорию.

#### deleteCategory(long userCategoryId)
```java
boolean success = categoryService.deleteCategory(1);
```
Удаляет категорию и все связанные данные (CASCADE).

#### getCategoryStats(Date month)
```java
CategoryService.CategoryStats stats = categoryService.getCategoryStats(new Date());

// Доступные поля:
stats.totalCategories    // Количество категорий
stats.totalExpense       // Общие расходы
stats.totalBudget        // Общий бюджет
stats.overBudgetCount    // Количество категорий с перерасходом

// Вычисляемые методы:
stats.getRemainingBudget()    // Остаток бюджета
stats.getBudgetFulfillment()  // % выполнения бюджета
```

## 🎨 UI Компоненты

### CategoriesFragment

**Основной функционал:**
- Список категорий с расходами
- Статистика по месяцу
- Добавление/редактирование/удаление
- Детальная информация по категории
- Автоматическое обновление

**Элементы UI:**
```xml
- categoriesRecyclerView  # Список категорий
- addButton               # Кнопка добавления
- progressBar             # Индикатор загрузки
- emptyTextView           # Текст для пустого состояния
- statsTextView           # Статистика
```

### CategoryAdapter

**Отображение:**
- Иконка категории (эмодзи)
- Название
- Текущие расходы
- Процент от общих расходов
- Бюджет (если задан)
- Разница (перерасход/экономия)
- ProgressBar выполнения бюджета

**Цветовая индикация:**
- 🟢 Зеленый: < 80% бюджета
- 🟠 Оранжевый: 80-100% бюджета
- 🔴 Красный: > 100% (перерасход)

**Обработка кликов:**
```java
categoryAdapter = new CategoryAdapter(categories, new CategoryClickListener() {
    @Override
    public void onCategoryClick(Category category) {
        // Короткий клик - детали
    }

    @Override
    public void onCategoryLongClick(Category category) {
        // Долгий клик - опции (редактировать/удалить)
    }
});
```

## 💡 Примеры использования

### Пример 1: Загрузка категорий

```java
// В Fragment
CategoryService categoryService = new CategoryService(getContext());

new Thread(() -> {
    List<Category> categories = categoryService.getAllCategories();
    
    getActivity().runOnUiThread(() -> {
        categoryAdapter.updateCategories(categories);
    });
}).start();
```

### Пример 2: Создание категории

```java
CategoryService categoryService = new CategoryService(getContext());

new Thread(() -> {
    long id = categoryService.createCategory("Здоровье", "💊", false);
    
    getActivity().runOnUiThread(() -> {
        if (id > 0) {
            Toast.makeText(context, "Категория создана", Toast.LENGTH_SHORT).show();
            loadCategories(); // Обновить список
        }
    });
}).start();
```

### Пример 3: Получение статистики

```java
CategoryService categoryService = new CategoryService(getContext());

new Thread(() -> {
    CategoryService.CategoryStats stats = 
        categoryService.getCategoryStats(new Date());
    
    getActivity().runOnUiThread(() -> {
        String text = String.format(
            "Категорий: %d\nРасходы: %.0f ₽\nБюджет: %.0f ₽",
            stats.totalCategories,
            stats.totalExpense,
            stats.totalBudget
        );
        statsTextView.setText(text);
    });
}).start();
```

### Пример 4: Работа с вычисляемыми полями

```java
Category category = categoryService.getCategoryById(1);

// Разница между расходами и бюджетом
double diff = category.getDifference();
if (diff > 0) {
    System.out.println("Перерасход: " + diff);
} else {
    System.out.println("Экономия: " + Math.abs(diff));
}

// Процент выполнения бюджета
int fulfillment = category.getBudgetFulfillment();
System.out.println("Выполнение: " + fulfillment + "%");

// Есть ли перерасход
if (category.isOverBudget()) {
    System.out.println("⚠️ Превышен бюджет!");
}

// Остаток бюджета
double remaining = category.getRemainingBudget();
System.out.println("Осталось: " + remaining);
```

## 🔄 SQL запросы

### Получение категорий с данными

```sql
SELECT 
    uc.id as user_cat_id,
    uc.cat_id,
    uc.name,
    c.icon,
    uc.fixed,
    COALESCE(SUM(me.expenses), 0) as current_expense,
    COALESCE(p.predict, 0) as budget
FROM user_categories uc
JOIN categories c ON uc.cat_id = c.id
LEFT JOIN monthly_expenses me ON me.user_cat_id = uc.id
LEFT JOIN dates d ON me.date_id = d.id AND d.date = '2025-12-01'
LEFT JOIN predict p ON p.user_cat_id = uc.id
WHERE uc.user_id = 1
GROUP BY uc.id
ORDER BY current_expense DESC;
```

### Создание категории

```sql
-- 1. Создать глобальную категорию
INSERT INTO categories (name, icon) VALUES ('Продукты', '🛒');

-- 2. Создать пользовательскую категорию
INSERT INTO user_categories (user_id, cat_id, name, fixed)
VALUES (1, 1, 'Продукты', 0);
```

## 📊 Формат данных

### Category JSON (для API/экспорта)

```json
{
  "userCategoryId": 1,
  "categoryId": 1,
  "name": "Продукты",
  "icon": "🛒",
  "isFixed": false,
  "currentExpense": 5000.0,
  "budget": 6000.0,
  "percentage": 35,
  "difference": -1000.0,
  "budgetFulfillment": 83
}
```

## ⚠️ Важные моменты

### 1. Многопоточность
Все операции с БД выполняются в фоновом потоке:
```java
new Thread(() -> {
    // Работа с БД
    getActivity().runOnUiThread(() -> {
        // Обновление UI
    });
}).start();
```

### 2. Обработка ошибок
```java
try {
    List<Category> categories = categoryService.getAllCategories();
} catch (Exception e) {
    Log.e(TAG, "Error loading categories", e);
    // Показать ошибку пользователю
}
```

### 3. Закрытие Cursor
```java
Cursor cursor = db.query(...);
try {
    // Работа с cursor
} finally {
    cursor.close(); // Всегда закрывайте!
}
```

### 4. CASCADE удаление
При удалении категории автоматически удаляются:
- Все расходы (monthly_expenses)
- Все прогнозы (predict)

## 🎨 Кастомизация

### Изменение цветов ProgressBar

В `CategoryAdapter.bind()`:
```java
if (progress > 100) {
    budgetProgressBar.setProgressTintList(
        ColorStateList.valueOf(Color.parseColor("#YOUR_COLOR")));
}
```

### Добавление новых иконок

```java
String[] icons = {"🛒", "🚗", "🏠", "💊", "🎮", "📱", "✈️", "🍕"};
```

### Изменение формата отображения

В `CategoryAdapter.bind()`:
```java
expenseTextView.setText(
    String.format(Locale.getDefault(), "%.2f ₽", amount)
);
```

## 🚀 Дальнейшие улучшения

- [ ] Сортировка категорий (по имени, расходам, проценту)
- [ ] Фильтрация (показать только с перерасходом)
- [ ] Графики и диаграммы
- [ ] Экспорт в Excel/CSV
- [ ] История изменений категории
- [ ] Подкатегории
- [ ] Теги и метки
- [ ] Уведомления о перерасходе

## 📞 Поддержка

При возникновении проблем проверьте:
1. Логи с тегом `CategoryService`
2. Структуру БД (правильные ли связи)
3. Права доступа к БД
4. Закрыты ли все Cursor'ы

---

**Модуль готов к использованию!** 🎉