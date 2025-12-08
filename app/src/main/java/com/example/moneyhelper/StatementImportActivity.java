package com.example.moneyhelper;
import com.example.moneyhelper.service.StatementImportService;
import android.os.Bundle;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class StatementImportActivity extends AppCompatActivity {

    private Button btnSelectFile;
    private Button btnImport;
    private TextView tvFileName;
    private TextView tvStatus;
    private ProgressBar progressBar;

    private Uri selectedFileUri;
    private StatementImportService importService;

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedFileUri = uri;
                    tvFileName.setText(getFileName(uri));
                    btnImport.setEnabled(true);
                    tvStatus.setText("Файл выбран. Нажмите 'Импортировать' для начала.");
                }
            }
    );

    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openFilePicker();
                } else {
                    Toast.makeText(this, "Необходимо разрешение для чтения файлов",
                            Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statement_import);

        initViews();
        initService();
        setupListeners();
    }

    private void initViews() {
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnImport = findViewById(R.id.btnImport);
        tvFileName = findViewById(R.id.tvFileName);
        tvStatus = findViewById(R.id.tvStatus);
        progressBar = findViewById(R.id.progressBar);

        btnImport.setEnabled(false);
        progressBar.setVisibility(ProgressBar.GONE);
    }

    private void initService() {
        importService = new StatementImportService(this);
    }

    private void setupListeners() {
        btnSelectFile.setOnClickListener(v -> checkPermissionAndOpenFilePicker());

        btnImport.setOnClickListener(v -> {
            if (selectedFileUri != null) {
                startImport();
            }
        });
    }

    /**
     * Проверяет разрешения и открывает файловый менеджер
     */
    private void checkPermissionAndOpenFilePicker() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ не требует разрешения для чтения через URI
            openFilePicker();
        } else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            openFilePicker();
        } else {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    /**
     * Открывает файловый менеджер для выбора PDF
     */
    private void openFilePicker() {
        filePickerLauncher.launch("application/pdf");
    }

    /**
     * Запускает процесс импорта
     */
    private void startImport() {
        setImportingState(true);

        // Выполняем импорт в фоновом потоке
        new Thread(() -> {
            StatementImportService.ImportResult result =
                    importService.importStatement(selectedFileUri);

            // Возвращаемся в UI поток для обновления интерфейса
            runOnUiThread(() -> {
                setImportingState(false);
                showImportResult(result);
            });
        }).start();
    }

    /**
     * Устанавливает состояние UI во время импорта
     */
    private void setImportingState(boolean isImporting) {
        btnSelectFile.setEnabled(!isImporting);
        btnImport.setEnabled(!isImporting);
        progressBar.setVisibility(isImporting ? ProgressBar.VISIBLE : ProgressBar.GONE);

        if (isImporting) {
            tvStatus.setText("Импорт в процессе...");
        }
    }

    /**
     * Показывает результат импорта
     */
    private void showImportResult(StatementImportService.ImportResult result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(result.isSuccess() ? "Импорт завершен" : "Ошибка импорта");
        builder.setMessage(result.getMessage());

        if (result.isSuccess()) {
            builder.setPositiveButton("OK", (dialog, which) -> {
                // Можно вернуться на главный экран или обновить данные
                finish();
            });

            builder.setNeutralButton("Импортировать еще", (dialog, which) -> {
                resetForm();
            });
        } else {
            builder.setPositiveButton("OK", null);
        }

        builder.show();

        tvStatus.setText(result.isSuccess() ?
                "Импорт успешно завершен!" :
                "Ошибка: " + result.error);
    }

    /**
     * Сбрасывает форму для нового импорта
     */
    private void resetForm() {
        selectedFileUri = null;
        tvFileName.setText("Файл не выбран");
        tvStatus.setText("Выберите PDF файл с выпиской Сбербанка");
        btnImport.setEnabled(false);
    }

    /**
     * Получает имя файла из URI
     */
    private String getFileName(Uri uri) {
        String path = uri.getPath();
        if (path != null) {
            int index = path.lastIndexOf('/');
            if (index != -1) {
                return path.substring(index + 1);
            }
        }
        return "Неизвестный файл";
    }


}