package Tests;


import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.moneyhelper.R;
import com.example.moneyhelper.parser.SberbankStatementParser;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

import java.util.List;

/**
 * Activity для тестирования и отладки парсера
 * Использовать временно для проверки работы парсера
 */
public class ParserTestActivity extends AppCompatActivity {

    private static final String TAG = "ParserTestActivity";

    private TextView tvResults;
    private Button btnSelectFile;
    private SberbankStatementParser parser;

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    testParser(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parser_test);
        PDFBoxResourceLoader.init(getApplicationContext());

        tvResults = findViewById(R.id.tvResults);
        btnSelectFile = findViewById(R.id.btnSelectFile);

        parser = new SberbankStatementParser(this);


        btnSelectFile.setOnClickListener(v ->
                filePickerLauncher.launch("application/pdf")
        );
    }

    private void testParser(Uri pdfUri) {
        tvResults.setText("Парсинг...");

        new Thread(() -> {
            try {
                List<SberbankStatementParser.Transaction> transactions =
                        parser.parseStatement(pdfUri);

                StringBuilder result = new StringBuilder();
                result.append("=== РЕЗУЛЬТАТЫ ПАРСИНГА ===\n\n");
                result.append("Всего транзакций: ").append(transactions.size()).append("\n\n");

                int count = 0;
                for (SberbankStatementParser.Transaction t : transactions) {
                    count++;
                    result.append("--- Транзакция #").append(count).append(" ---\n");
                    result.append("Дата: ").append(t.date).append("\n");
                    result.append("Сумма: ").append(String.format("%s", t.amount)).append(" руб.\n");
                    result.append("Категория: ").append(t.category != null ? t.category : "НЕ НАЙДЕНА").append("\n");
                    result.append("Описание: ").append(t.description != null ? t.description : "НЕ НАЙДЕНО").append("\n");
                    result.append("Тип: ").append(t.isIncome ? "Доход" : "Расход").append("\n");
                    result.append("\n");
                }

                runOnUiThread(() -> tvResults.setText(result.toString()));

            } catch (Exception e) {
                Log.e(TAG, "Ошибка парсинга", e);
                runOnUiThread(() ->
                        tvResults.setText("Ошибка: " + e.getMessage() + "\n\n" +
                                "Смотрите логи с тегом: SberbankParser")
                );
            }
        }).start();
    }
}